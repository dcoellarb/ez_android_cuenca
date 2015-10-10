package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.text.DecimalFormat;
import java.text.NumberFormat;

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
                    SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta",MODE_PRIVATE).edit();
                    prefs.putString("estado",getString(R.string.status_parse_activo));
                    prefs.commit();

                    Intent intent = new Intent(activity, PedidoActivo.class);
                    intent.putExtra(PedidoPendiente.PARAM_ID,id);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
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

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("objectId", id);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null){
                    setPedido(object);
                }else{
                    Log.e("ERROR","Could not get pedido");
                    if (e != null){
                        Log.e("ERROR",e.getMessage());
                    }else{
                        Log.e("ERROR","result is null for id:" + id);
                    }
                }
            }
        });

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

    private void setPedido(ParseObject pedido){
        TextView viaje = (TextView)findViewById(R.id.pedido_viaje);
        try{
            String origen  = pedido.getParseObject("CiudadOrigen").fetchIfNeeded().getString("Nombre");
            String destino  = pedido.getParseObject("CiudadDestino").fetchIfNeeded().getString("Nombre");
            viaje.setText(origen + " - " + destino);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView producto = (TextView)findViewById(R.id.pedido_producto);
        producto.setText(pedido.getString("Producto"));

        NumberFormat formatter = new DecimalFormat("#0.00");

        TextView precio = (TextView)findViewById(R.id.pedido_valor);
        precio.setText("$" + formatter.format(pedido.getNumber("Valor")));

        TextView peso = (TextView)findViewById(R.id.pedido_peso);
        peso.setText("Peso: " + pedido.getNumber("PesoDesde") + " a " + pedido.getNumber("PesoHasta") + " Tl.");
        TextView carga = (TextView)findViewById(R.id.pedido_carga);
        carga.setText("Carga: " + MainActivity.formatDate(pedido.getDate("HoraCarga")));
        TextView entrega = (TextView)findViewById(R.id.pedido_entrega);
        entrega.setText("Entrega: " + MainActivity.formatDate(pedido.getDate("HoraEntrega")));

        TextView extra = (TextView)findViewById(R.id.pedido_extra);
        if (pedido.getString("TipoTransporte").equalsIgnoreCase("furgon")){
            extra.setText("Cubicaje Minimo:" + pedido.getNumber("CubicajeMin") + " m3");
        }else if (pedido.getString("TipoTransporte").equalsIgnoreCase("plataforma")) {
            extra.setText("Extension Minima:" + pedido.getNumber("ExtensionMin") + " pies");
        }else{
            extra.setVisibility(View.GONE);
        }
        TextView refrigeracion = (TextView)findViewById(R.id.pedido_refrigeracion);
        if (pedido.getString("TipoTransporte").equalsIgnoreCase("furgon")) {
            if (pedido.getBoolean("CajaRefrigerada")){
                refrigeracion.setText("Refrigeracion: Si");
            }else{
                refrigeracion.setText("Refrigeracion: No");
            }
        }else{
            refrigeracion.setVisibility(View.GONE);
        }
        TextView comision = (TextView)findViewById(R.id.pedido_comision);
        comision.setText("$" + formatter.format(pedido.getNumber("Comision")));

    }
}
