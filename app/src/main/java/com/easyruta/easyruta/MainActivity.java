package com.easyruta.easyruta;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LayoutInflater inflater;
    ListView pedidosList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        inflater = getLayoutInflater();

        pedidosList  = (ListView)findViewById(R.id.pedidos_list);
        loadPedidos();

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        /* Subscribe to the demo_tutorial channel */
        try {
            ((EasyRutaApplication)getApplication()).getPubnub().subscribe("new_pedido", new Callback() {
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
        query.whereEqualTo("Estado", "Pendiente");
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
        if (id == R.id.action_settings) {
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
            TextView text = (TextView)convertView.findViewById(R.id.pendido_viaje);

            try{
                String origen  = pedidos.get(i).getParseObject("CiudadOrigen").fetchIfNeeded().getString("Nombre");
                String destino  = pedidos.get(i).getParseObject("CiudadDestino").fetchIfNeeded().getString("Nombre");
                text.setText(origen + " - " + destino);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            final LinearLayout detail = (LinearLayout)convertView.findViewById(R.id.pedido_detail);
            TextView tomar = (TextView)detail.findViewById(R.id.pedido_tomar);
            tomar.setTag(pedidos.get(i));

            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (detail.getVisibility() == View.GONE) {
                        detail.setVisibility(View.VISIBLE);
                    } else {
                        detail.setVisibility(View.GONE);
                    }
                }
            });

            tomar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final ParseObject transportista = ((EasyRutaApplication)getApplication()).getTransportista();

                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
                    query.getInBackground(((ParseObject) view.getTag()).getObjectId().toString(), new GetCallback<ParseObject>() {
                        public void done(ParseObject pedido, ParseException e) {
                            if (e == null) {
                                //TODO - check if status is still pending

                                pedido.put("Estado", "PendienteConfirmacion");
                                pedido.put("Transportista", transportista);
                                pedido.saveInBackground();

                                //TODO - add callback to update ui and save pedido in app

                                Toast toast = Toast.makeText(getBaseContext(), transportista.getObjectId().toString(), Toast.LENGTH_LONG);
                                toast.show();
                            }
                        }
                    });
                }
            });

            return convertView;
        }
    }
}
