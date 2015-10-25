package com.easyruta.easyruta;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 * Created by dcoellar on 9/26/15.
 */
public class PedidoActivo extends Activity {

    public static String PARAM_ID = "id";
    private GPSTracker gpsTracker;
    private Activity activity;
    private ImageView navImageView;
    private LinearLayout iniciar;
    private LinearLayout finalizar;
    private String contactoNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        setContentView(R.layout.activity_pedido_activo);

        pubNubSuscriptions();

        loadPedido();
    }

    private void pubNubSuscriptions() {
        try {
            ((EasyRutaApplication) getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_cancelado), new Callback() {
                public void successCallback(String channel, Object message) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Pedido cancelado")
                            .setMessage("El cliente ha cancelado este pedido.")
                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    closeActivity();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
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
        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        final String id = prefs.getString("pedido", "");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("objectId", id);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    setPedido(object);
                    if (object.getString("Estado").equalsIgnoreCase(getString(R.string.status_parse_encurso))) {
                        toggleEstado();
                    }
                } else {
                    Log.e("ERROR", "Could not get pedido");
                    if (e != null) {
                        Log.e("ERROR", e.getMessage());
                    } else {
                        Log.e("ERROR", "result is null for id:" + id);
                    }
                }
            }
        });

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

                            pedido.put("HoraInicio", new Date());
                            pedido.put("Estado", getString(R.string.status_parse_encurso));
                            pedido.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        application.getPubnub().publish(getString(R.string.status_pedido_iniciado), id, new Callback() {});

                                        toggleEstado();
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

                            pedido.put("HoraFinalizacion", new Date());
                            pedido.put("Estado", getString(R.string.status_parse_finalizado));
                            pedido.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {

                                        ((EasyRutaApplication)getApplication()).getTransportista().increment("PedidosCompletados", 1);
                                        ((EasyRutaApplication)getApplication()).getTransportista().saveInBackground();

                                        application.getPubnub().publish(getString(R.string.status_pedido_finalizado), id, new Callback() {
                                        });

                                        gpsTracker.stopUsingGPS();

                                        closeActivity();
                                    }
                                }
                            });
                        }
                    }
                });
            }

        });


        findViewById(R.id.pedido_cancelar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                cancelPedido(true);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.confirmation_cancelar_pedido)
                        .setPositiveButton("Si", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

    }

    private void setPedido(ParseObject pedido){

        ImageView empresaImage = (ImageView)findViewById(R.id.pedido_company_image);
        TextView contacto = (TextView)findViewById(R.id.pedido_contacto);
        try{
            ParseObject empresa = pedido.getParseObject("empresa").fetchIfNeeded();
            contacto.setText(empresa.getString("PersonaContacto"));

            Picasso.with(this.getBaseContext())
                    .load(empresa.getString("ImageUrl"))
                    .placeholder(R.drawable.account)
                    .into(empresaImage);

            contactoNumber = empresa.getString("Telefono");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView viaje = (TextView)findViewById(R.id.pedido_viaje);
        try{
            String origen  = pedido.getParseObject("CiudadOrigen").fetchIfNeeded().getString("Nombre");
            String destino  = pedido.getParseObject("CiudadDestino").fetchIfNeeded().getString("Nombre");
            viaje.setText(origen + " - " + destino);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView direccion = (TextView)findViewById(R.id.pedido_direccion);
        direccion.setText(pedido.getString("DireccionDestino"));

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

    private void toggleEstado(){
        iniciar.setVisibility(View.GONE);
        finalizar.setVisibility(View.VISIBLE);
        navImageView.setVisibility(View.VISIBLE);

        gpsTracker = new GPSTracker(activity);
    }

    private void cancelPedido(final Boolean broadcast){
        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        final String id = prefs.getString("pedido", "");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("objectId", id);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    object.put("Estado", getString(R.string.status_parse_pendiente));
                    object.remove("Transportista");
                    object.add("TransportistasBloqueados", ((EasyRutaApplication)getApplication()).getTransportista().getObjectId());
                    object.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {

                            ((EasyRutaApplication)getApplication()).getTransportista().increment("PedidosCancelados", 1);
                            ((EasyRutaApplication)getApplication()).getTransportista().saveInBackground();

                            if (broadcast) {
                                ((EasyRutaApplication) activity.getApplication()).getPubnub().publish(getString(R.string.status_pedido_cancelado_transportista), id, new Callback() {
                                });
                            }

                            closeActivity();;
                        }
                    });
                } else {
                    Log.e("ERROR", "Could not get pedido:" + id);
                    if (e != null) {
                        Log.e("ERROR", e.getMessage());
                    } else {
                        Log.e("ERROR", "result is null for id:" + id);
                    }
                }
            }
        });
    }

    private void closeActivity(){
        SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", MODE_PRIVATE).edit();
        prefs.remove("pedido");
        prefs.remove("estado");
        prefs.commit();

        Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnub();
        pubnub.unsubscribeAll();

        Intent i = new Intent(getBaseContext(), MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
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
            callIntent.setData(Uri.parse("tel:" + contactoNumber));
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
