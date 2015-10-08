package com.easyruta.easyruta;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

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
public class PedidoActivo extends Activity {

    public static String PARAM_ID = "id";
    private String id;
    private GPSTracker gpsTracker;
    private Activity activity;
    private ImageView navImageView;
    private LinearLayout iniciar;
    private LinearLayout finalizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        Intent intent = getIntent();
        id = intent.getStringExtra(PARAM_ID);
        setContentView(R.layout.activity_pedido_activo);

        pubNubSuscriptions();

        loadPedido();
    }

    private void pubNubSuscriptions() {
        try {
            ((EasyRutaApplication) getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_cancelado), new Callback() {
                public void successCallback(String channel, Object message) {
                    //TODO - show message popup

                    //TODO - go back to previous screen
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    private void loadPedido() {

        ImageView phoneImageView = (ImageView) findViewById(R.id.pedido_call);
        phoneImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = new String[]{Manifest.permission.CALL_PHONE};
                    requestPermissions(permissions,0);
                }else{
                    makeCall();
                }
            }
        });

        navImageView = (ImageView)findViewById(R.id.pedido_navigate);
        navImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                    requestPermissions(permissions,1);
                }else{
                    callMap();
                }
            }
        });

        iniciar = (LinearLayout)findViewById(R.id.pedido_iniciar);
        iniciar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
                query.getInBackground(id, new GetCallback<ParseObject>() {

                    public void done(final ParseObject pedido, ParseException e) {
                        if (e == null) {
                            final EasyRutaApplication application = (EasyRutaApplication) getApplication();

                            pedido.put("Estado", getString(R.string.status_parse_encurso));
                            pedido.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        application.getPubnub().publish(getString(R.string.status_pedido_iniciado), id, new Callback() {});

                                        iniciar.setVisibility(View.GONE);
                                        finalizar.setVisibility(View.VISIBLE);
                                        navImageView.setVisibility(View.VISIBLE);
                                        gpsTracker = new GPSTracker(activity);
                                    }
                                }
                            });
                        }
                    }
                });
            }

        });

        finalizar = (LinearLayout)findViewById(R.id.pedido_finalizar);
        finalizar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
                query.getInBackground(id, new GetCallback<ParseObject>() {

                    public void done(final ParseObject pedido, ParseException e) {
                        if (e == null) {
                            final EasyRutaApplication application = (EasyRutaApplication) getApplication();

                            pedido.put("Estado", getString(R.string.status_parse_finalizado));
                            pedido.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        application.getPubnub().publish(getString(R.string.status_pedido_finalizado), id, new Callback() {
                                        });

                                        gpsTracker.stopUsingGPS();

                                        Intent i = new Intent(getBaseContext(), MainActivity.class);
                                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(i);
                                    }
                                }
                            });
                        }
                    }
                });
            }

        });

        LinearLayout cancelar = (LinearLayout)findViewById(R.id.pedido_cancelar);
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

                                        gpsTracker.stopUsingGPS();

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            makeCall();
        }
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callMap();
        }
    }

    private void makeCall(){
        if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            //TODO - get phone from parse
            callIntent.setData(Uri.parse("tel:0377778888"));
            startActivity(callIntent);
        }
    }

    private void callMap(){
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            if (gpsTracker.isGPSEnabled && gpsTracker.isGPSTrackingEnabled && gpsTracker.isNetworkEnabled){
                String uri = "geo:"+ gpsTracker.getLatitude() + "," + gpsTracker.getLongitude();
                startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
            }else{
                Toast toast = new Toast(activity);
                toast.setText(R.string.no_gps_message);
                toast.show();
            }
        }
    }
}