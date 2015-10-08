package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

/**
 * Created by dcoellar on 9/26/15.
 */
public class PedidoPendiente extends Activity {

    public static String PARAM_ID = "id";
    private Activity activity;
    private ProgressBar progressBar;
    private TextView counter;
    private String id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Intent intent = getIntent();
        id = intent.getStringExtra(PARAM_ID);
        setContentView(R.layout.activity_pedido_pendiente);

        pubNubSuscriptions();

        loadPedido();
    }

    private void pubNubSuscriptions(){
        try {
            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_rechazado), new Callback() {
                public void successCallback(String channel, Object message) {
                    //TODO - show message popup

                    //TODO - go back to previous screen
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_confirmado), new Callback() {
                public void successCallback(String channel, Object message) {
                    Intent intent = new Intent(activity, PedidoActivo.class);
                    intent.putExtra(PedidoPendiente.PARAM_ID,id);
                    startActivity(intent);

                    //TODO - Manage back it can't go to list of pedidos
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_cancelado), new Callback() {
                public void successCallback(String channel, Object message) {
                    //TODO - show message popup

                    //TODO - go back to previous screen
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    private void loadPedido(){
        progressBar = (ProgressBar) findViewById(R.id.pending_progress);
        counter = (TextView) findViewById(R.id.pending_counter);
        new CountDownTimer(1800000, 1000) {

            public void onTick(long millisUntilFinished) {
                int progress = 1800000 - (int)millisUntilFinished;
                progressBar.setSecondaryProgress(progress);

                int seconds = (int) (millisUntilFinished / 1000) % 60 ;
                String secodsString = String.valueOf(seconds);
                if (seconds < 10)
                    secodsString = "0" + String.valueOf(seconds);
                int minutes = (int) ((millisUntilFinished / (1000*60)) % 60);
                String minutesString = String.valueOf(minutes);
                if (minutes < 10)
                    minutesString = "0" + String.valueOf(minutes);
                counter.setText(minutesString + ":" + secodsString);

            }

            public void onFinish() {
                //TODO - tell user and go back to list
            }
        }.start();

        LinearLayout cancelar = (LinearLayout)findViewById(R.id.pedido_pendiente_cancelar);
        cancelar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
                query.getInBackground(id, new GetCallback<ParseObject>() {
                    public void done(final ParseObject pedido, ParseException e) {
                        if (e == null) {
                            final EasyRutaApplication application = (EasyRutaApplication) getApplication();

                            //TODO - Add transportista id to exeption list
                            pedido.put("Estado", getString(R.string.status_parse_cancelado_transportista));
                            pedido.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        //TODO - Update efectivity rate of transportista

                                        application.getPubnub().publish(getString(R.string.status_pedido_cancelado_transportista), id, new Callback() {
                                        });

                                        //TODO -  go back to list of pedidos;
                                    }
                                }
                            });
                        }
                    }
                });
            }

        });
    }

}
