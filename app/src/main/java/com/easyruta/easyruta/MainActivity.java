package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LayoutInflater inflater;
    private ListView pedidosList;
    private PedidosAdapter pedidosAdapter;
    private Activity activity;
    private Number saldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        activity = this;

        pubNubSuscriptions();

        inflater = getLayoutInflater();

        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }

    private void pubNubSuscriptions(){
        Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnub();
        try {
            pubnub.subscribe(getString(R.string.status_pedido_new_pedidos), new Callback() {
                public void successCallback(String channel, Object message) {
                    Log.d("TEST DANIEL", "pubnub new pedido");

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadPedidos();
                        }
                    });

                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });
            pubnub.subscribe(getString(R.string.status_pedido_tomado), new Callback() {
                public void successCallback(String channel, Object message) {
                    //loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnub.subscribe(getString(R.string.status_pedido_rechazado), new Callback() {
                public void successCallback(String channel, Object message) {
                    loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnub.subscribe(getString(R.string.status_pedido_cancelado_transportista), new Callback() {
                public void successCallback(String channel, Object message) {
                    //loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnub.subscribe(getString(R.string.status_pedido_cancelado), new Callback() {
                public void successCallback(String channel, Object message) {
                    //loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });
        } catch (PubnubException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume(){
        super.onResume();

        updateSaldo();
    }

    private void updateSaldo(){
        ParseQuery query = new ParseQuery("Transportista");
        query.whereEqualTo("objectId",((EasyRutaApplication)getApplication()).getTransportista().getObjectId());
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    saldo = object.getNumber("Saldo");
                    TextView saldoView = (TextView) findViewById(R.id.saldo);
                    NumberFormat formatter = new DecimalFormat("#0.00");
                    saldoView.setText("$" + formatter.format(saldo));

                    pedidosList = (ListView) findViewById(R.id.pedidos_list);
                    pedidosAdapter = new PedidosAdapter();
                    pedidosList.setAdapter(pedidosAdapter);
                    loadPedidos();
                } else {
                    Log.e("ERROR", "Could not get transportista");
                    if (e != null) {
                        Log.e("ERROR", e.getMessage());
                    } else {
                        Log.e("ERROR", "result is null for id:" + ((EasyRutaApplication)getApplication()).getTransportista().getObjectId());
                    }
                }
            }
        });
    }

    private void loadPedidos(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("Estado", getString(R.string.status_parse_pendiente));
        query.whereLessThanOrEqualTo("Comision", saldo);
        query.whereNotEqualTo("TransportistasBloqueados", ((EasyRutaApplication) getApplication()).getTransportista().getObjectId());
        query.orderByDescending("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> pedidos, ParseException e) {
                if (e == null) {
                    pedidosAdapter.updatePedidos(pedidos);
                } else {
                    Log.d("ERROR", "Error: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_profile) {
            Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnub();
            pubnub.unsubscribeAll();

            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class PedidosAdapter extends BaseAdapter {

        private List<ParseObject> pedidos = new ArrayList<>();

        @Override
        public int getCount() {
            return pedidos.size();
        }

        @Override
        public Object getItem(int i) {
            return pedidos.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            convertView = inflater.inflate(R.layout.pendidos_item,parent,false);

            TextView viaje = (TextView)convertView.findViewById(R.id.pedido_viaje);
            try{
                String origen  = pedidos.get(i).getParseObject("CiudadOrigen").fetchIfNeeded().getString("Nombre");
                String destino  = pedidos.get(i).getParseObject("CiudadDestino").fetchIfNeeded().getString("Nombre");
                viaje.setText(origen + " - " + destino);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            TextView cantidad = (TextView)convertView.findViewById(R.id.pedido_cantidad);
            if (pedidos.get(i).getString("TipoUnidad").equalsIgnoreCase("peso")){
                cantidad.setText("Peso desde:" + String.valueOf(pedidos.get(i).getNumber("PesoDesde")) + " hasta " + String.valueOf(pedidos.get(i).getNumber("PesoHasta")) + " Tn");
            }else{
                cantidad.setText(pedidos.get(i).getNumber("Unidades") + " " + pedidos.get(i).getString("Producto"));
            }

            NumberFormat formatter = new DecimalFormat("#0.00");
            TextView precio = (TextView)convertView.findViewById(R.id.pedido_valor);
            precio.setText("$" + formatter.format(pedidos.get(i).getNumber("Valor")));
            TextView peso = (TextView)convertView.findViewById(R.id.pedido_peso);
            peso.setText("Peso: " + pedidos.get(i).getNumber("PesoDesde") + " a " + pedidos.get(i).getNumber("PesoHasta") + " Tl.");
            TextView carga = (TextView)convertView.findViewById(R.id.pedido_carga);
            carga.setText("Carga: " + formatDate(pedidos.get(i).getDate("HoraCarga")));
            TextView entrega = (TextView)convertView.findViewById(R.id.pedido_entrega);
            entrega.setText("Entrega: " + formatDate(pedidos.get(i).getDate("HoraEntrega")));
            TextView extra = (TextView)convertView.findViewById(R.id.pedido_extra);
            if (pedidos.get(i).getString("TipoTransporte").equalsIgnoreCase("furgon")){
                extra.setText("Cubicaje Minimo:" + pedidos.get(i).getNumber("CubicajeMin") + " m3");
            }else if (pedidos.get(i).getString("TipoTransporte").equalsIgnoreCase("plataforma")) {
                extra.setText("Extension Minima:" + pedidos.get(i).getNumber("ExtensionMin") + " pies");
            }else{
                extra.setVisibility(View.GONE);
            }
            TextView refrigeracion = (TextView)convertView.findViewById(R.id.pedido_refrigeracion);
            if (pedidos.get(i).getString("TipoTransporte").equalsIgnoreCase("furgon")) {
                if (pedidos.get(i).getBoolean("CajaRefrigerada")){
                    refrigeracion.setText("Refrigeracion: Si");
                }else{
                    refrigeracion.setText("Refrigeracion: No");
                }
            }else{
                refrigeracion.setVisibility(View.GONE);
            }
            TextView comision = (TextView)convertView.findViewById(R.id.pedido_comision);
            comision.setText("$" + formatter.format(pedidos.get(i).getNumber("Comision")));


            final LinearLayout header = (LinearLayout)convertView.findViewById(R.id.pedido_header);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LinearLayout detail = (LinearLayout)((FrameLayout)header.getParent()).findViewById(R.id.pedido_detail);
                    LinearLayout separator = (LinearLayout)((FrameLayout)header.getParent()).findViewById(R.id.pedido_separator);

                    if (detail.getVisibility() == View.GONE) {
                        detail.setVisibility(View.VISIBLE);
                        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.expand);
                        animation.setDuration(500);
                        detail.setAnimation(animation);
                        detail.animate();
                        animation.start();
                        separator.setVisibility(View.VISIBLE);
                    } else {
                        detail.setVisibility(View.GONE);
                        separator.setVisibility(View.GONE);
                    }
                }
            });

            LinearLayout tomar = (LinearLayout)convertView.findViewById(R.id.pedido_tomar);
            tomar.setTag(pedidos.get(i));
            tomar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final ParseObject transportista = ((EasyRutaApplication)getApplication()).getTransportista();
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
                    query.getInBackground(((ParseObject) view.getTag()).getObjectId().toString(), new GetCallback<ParseObject>() {
                        public void done(final ParseObject pedido, ParseException e) {
                            if (e == null) {
                                //TODO - check if status is still pending
                                final EasyRutaApplication application = (EasyRutaApplication)getApplication();

                                pedido.put("HoraSeleccion", Calendar.getInstance().getTime());
                                pedido.put("Estado", getString(R.string.status_parse_pendiente_confirmacion));
                                pedido.put("Transportista", transportista);
                                pedido.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            application.getPubnub().publish(getString(R.string.status_pedido_tomado), pedido.getObjectId().toString(), new Callback() {
                                            });

                                            SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", MODE_PRIVATE).edit();
                                            prefs.putString("pedido", pedido.getObjectId());
                                            prefs.putString("estado", getString(R.string.status_parse_pendiente_confirmacion));
                                            prefs.commit();

                                            Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnub();
                                            pubnub.unsubscribeAll();

                                            Intent intent = new Intent(activity, PedidoPendiente.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);

                                        } else {
                                            Toast toast = Toast.makeText(getBaseContext(), transportista.getObjectId().toString(), Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    }
                                });

                                //TODO - add callback to update ui and save pedido in app
                            }
                        }
                    });
                }
            });

            return convertView;
        }

        public void updatePedidos(List<ParseObject> pedidos) {
            this.pedidos.clear();
            this.pedidos.addAll(pedidos);
            this.notifyDataSetChanged();
        }
    }

    public static String formatDate(Date date){
        String result = "";

        String day = "";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) && calendar.get(Calendar.DATE) == today.get(Calendar.DATE)){
            day = "Hoy";
        }else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == yesterday.get(Calendar.MONTH) && calendar.get(Calendar.DATE) == yesterday.get(Calendar.DATE)){
            day = "Ayer";
        }else if (calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == tomorrow.get(Calendar.MONTH) && calendar.get(Calendar.DATE) == tomorrow.get(Calendar.DATE)) {
            day = "Manana";
        }else{
            day = monthName(calendar.get(Calendar.MONTH)) + " " + calendar.get(Calendar.DATE);
        }
        result = day + " a las " + formatTime(calendar.get(Calendar.HOUR)) + ":" + formatTime(calendar.get(Calendar.MINUTE)) + formatAMPM(calendar.get(Calendar.AM_PM));
        return result;
    }

    private static String monthName(int month){
        String name = "";
        switch (month){
            case 1:
                name = "Enero";
                break;
            case 2:
                name = "Febrero";
                break;
            case 3:
                name = "Marzo";
                break;
            case 4:
                name = "Abril";
                break;
            case 5:
                name = "Mayo";
                break;
            case 6:
                name = "Junio";
                break;
            case 7:
                name = "Julio";
                break;
            case 8:
                name = "Agosto";
                break;
            case 9:
                name = "Septiembre";
                break;
            case 0:
                name = "Octubre";
                break;
            case 11:
                name = "Noviembre";
                break;
            case 12:
                name = "Dicienmbre";
                break;
        }
        return name;
    }

    private static String formatTime(int time){
        String result = String.valueOf(time);
        if (time < 10){
            result = "0" + String.valueOf(time);
        }
        return result;
    }

    private static String formatAMPM(int ampm){
        String result = "pm";
        if (ampm == 1){
            result = "am";
        }
        return result;
    }
}
