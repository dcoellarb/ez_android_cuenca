package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.utils.utils;
import com.easyruta.easyruta.viewmodel.MainViewModel;
import com.parse.ParseAnalytics;
import com.parse.ParseObject;
import com.pubnub.api.Pubnub;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private MainViewModel viewModel;
    private LayoutInflater inflater;
    private ListView pedidosList;
    private SwipeRefreshLayout swipeLayout;
    private PedidosAdapter pedidosAdapter;
    private Activity activity;
    private Number saldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inflater = getLayoutInflater();
        activity = this;
        viewModel = new MainViewModel(this);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeToRefresh);
        swipeLayout.setOnRefreshListener(this);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }

    @Override
    public void onRefresh() {
        // TODO temporal solution
        // getSaldo();
    }

    @Override
    protected void onResume(){
        super.onResume();

        viewModel.pubNubSuscriptions();
        viewModel.refreshTransportista();
    }

    public void toogleDisponible(){
        if (viewModel.isTransportistaIndependiente){
            findViewById(R.id.transportista_home).setVisibility(View.VISIBLE);
            findViewById(R.id.transportista_proveedor_home).setVisibility(View.GONE);
            this.viewModel.getSaldo();

            pedidosList = (ListView) findViewById(R.id.pedidos_list);
            pedidosAdapter = new PedidosAdapter();
            pedidosList.setAdapter(pedidosAdapter);
            this.viewModel.getPedidosPendientes();

        }else{
            findViewById(R.id.transportista_home).setVisibility(View.GONE);
            findViewById(R.id.transportista_proveedor_home).setVisibility(View.VISIBLE);
            //TODO tomar en cuenta cuando se implmente Despachador
            /*
            if (viewModel.getTransportista().get("Estado").toString().equalsIgnoreCase("no disponible")) {
                findViewById(R.id.waiting_bar).setVisibility(View.GONE);
                ((TextView)findViewById(R.id.waiting_message)).setText("Tu estado es NO Disponible, no puedes recibir pedidos en este momento.");
                findViewById(R.id.waiting_button).setVisibility(View.VISIBLE);
                findViewById(R.id.waiting_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewModel.MarcarDisponible();
                    }
                });
            }
            */
        }
    }

    public void updateSaldo(ParseObject object){
        saldo = object.getNumber("saldo");
        if (saldo == null){
            saldo = 0;
        }
        TextView saldoView = (TextView) findViewById(R.id.saldo);
        NumberFormat formatter = new DecimalFormat("#0.00");
        saldoView.setText("$" + formatter.format(saldo));
    }

    public void updatePedidos(List<ParseObject> pedidos){
        if ( pedidos.size() >0 ){
            findViewById(R.id.pedidos_placeholder).setVisibility(View.GONE);
            swipeLayout.setRefreshing(false);
            pedidosAdapter.updatePedidos(pedidos);
        } else {
            findViewById(R.id.pedidos_placeholder).setVisibility(View.VISIBLE);
        }
    }

    public void moveToPedidoActivo(ParseObject pedido) {
        Intent intent = new Intent(activity, PedidoActivoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
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
            Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnubService().getPubnub();
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
            String origen = pedidos.get(i).getString("ciudadOrigen").substring(0, 1).toUpperCase() + pedidos.get(i).getString("ciudadOrigen").substring(1).toLowerCase();

            String destino = pedidos.get(i).getString("ciudadDestino").substring(0, 1).toUpperCase() + pedidos.get(i).getString("ciudadDestino").substring(1).toLowerCase();
            viaje.setText(origen + " - " + destino);

            TextView cantidad = (TextView)convertView.findViewById(R.id.pedido_cantidad);
            cantidad.setText(utils.formatPeso(pedidos.get(i).getString("peso")));

            NumberFormat formatter = new DecimalFormat("#0.00");
            TextView precio = (TextView)convertView.findViewById(R.id.pedido_valor);
            if (pedidos.get(i).getBoolean("donacion")){
                convertView.findViewById(R.id.pedido_decorator).setBackground(getResources().getDrawable(R.drawable.donacion_background));
                precio.setText("DONACION");

                convertView.findViewById(R.id.pedido_comision).setVisibility(View.GONE);
                convertView.findViewById(R.id.pedido_comision_text).setVisibility(View.GONE);
            }else{
                convertView.findViewById(R.id.pedido_decorator).setBackgroundColor(getResources().getColor(R.color.red));
                precio.setText("$" + formatter.format(pedidos.get(i).getNumber("valor")));

                TextView comision = (TextView)convertView.findViewById(R.id.pedido_comision);
                comision.setText("$" + formatter.format(pedidos.get(i).getNumber("comision")));
            }

            TextView carga = (TextView)convertView.findViewById(R.id.pedido_carga);
            carga.setText("Carga: " + utils.formatDate(pedidos.get(i).getDate("horaCarga")));
            TextView entrega = (TextView)convertView.findViewById(R.id.pedido_entrega);
            entrega.setText("Entrega: " + utils.formatDate(pedidos.get(i).getDate("horaEntrega")));

            final LinearLayout header = (LinearLayout)convertView.findViewById(R.id.pedido_header);
            header.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LinearLayout detail = (LinearLayout)((FrameLayout)header.getParent()).findViewById(R.id.pedido_detail);
                    if (detail.getVisibility() == View.GONE) {
                        detail.setVisibility(View.VISIBLE);
                        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.expand);
                        animation.setDuration(500);
                        detail.setAnimation(animation);
                        detail.animate();
                        animation.start();
                    } else {
                        detail.setVisibility(View.GONE);
                    }
                }
            });

            LinearLayout tomar = (LinearLayout)convertView.findViewById(R.id.pedido_tomar);
            tomar.setTag(pedidos.get(i));
            tomar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    viewModel.tomarPedido((ParseObject) view.getTag());
                }
            });
            Number saldo = viewModel.getTransportista().getNumber("saldo");
            if (saldo == null) {
                saldo = 0;
            }
            if (pedidos.get(i).getBoolean("donacion") == false && saldo.doubleValue() < pedidos.get(i).getNumber("comision").doubleValue()){
                tomar.setVisibility(View.GONE);
            }
            return convertView;
        }

        public void updatePedidos(List<ParseObject> pedidos) {
            this.pedidos.clear();
            this.pedidos.addAll(pedidos);
            this.notifyDataSetChanged();
        }
    }
}
