package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
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
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LayoutInflater inflater;
    ListView pedidosList;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        inflater = getLayoutInflater();

        pedidosList  = (ListView)findViewById(R.id.pedidos_list);

        loadPedidos();

        pubNubSuscriptions();

        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }

    private void pubNubSuscriptions(){
        try {
            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_new_pedidos), new Callback() {
                public void successCallback(String channel, Object message) {
                    loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_tomado), new Callback() {
                public void successCallback(String channel, Object message) {
                    loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_rechazado), new Callback() {
                public void successCallback(String channel, Object message) {
                    loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_cancelado_transportista), new Callback() {
                public void successCallback(String channel, Object message) {
                    loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            ((EasyRutaApplication)getApplication()).getPubnub().subscribe(getString(R.string.status_pedido_cancelado), new Callback() {
                public void successCallback(String channel, Object message) {
                    loadPedidos();
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });
        } catch (PubnubException e) {
            e.printStackTrace();
        }

    }

    private void loadPedidos(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("Estado", getString(R.string.status_parse_pendiente));
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> pedidos, ParseException e) {
                if (e == null) {
                    pedidosList.setAdapter(new PedidosAdapter(pedidos));
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class PedidosAdapter extends BaseAdapter {

        private List<ParseObject> pedidos;

        public PedidosAdapter(List<ParseObject> pedidos){
            this.pedidos = pedidos;
        }

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

            TextView text = (TextView)convertView.findViewById(R.id.pedido_viaje);
            try{
                String origen  = pedidos.get(i).getParseObject("CiudadOrigen").fetchIfNeeded().getString("Nombre");
                String destino  = pedidos.get(i).getParseObject("CiudadDestino").fetchIfNeeded().getString("Nombre");
                text.setText(origen + " - " + destino);
            } catch (ParseException e) {
                e.printStackTrace();
            }

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

                                pedido.put("Estado", getString(R.string.status_parse_pendiente_confirmacion));
                                pedido.put("Transportista", transportista);
                                pedido.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            application.getPubnub().publish(getString(R.string.status_pedido_tomado), pedido.getObjectId().toString(), new Callback() {
                                            });

                                            Intent intent = new Intent(activity, PedidoPendiente.class);
                                            intent.putExtra(PedidoPendiente.PARAM_ID,pedido.getObjectId());
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
    }
}
