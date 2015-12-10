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
import com.parse.ParseException;
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
        getSaldo();
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
            getSaldo();
        }else{
            findViewById(R.id.transportista_home).setVisibility(View.GONE);
            findViewById(R.id.transportista_proveedor_home).setVisibility(View.VISIBLE);
            if (viewModel.getTransportista().get("Estado").toString().equalsIgnoreCase("disponible")) {
                findViewById(R.id.waiting_bar).setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.waiting_message)).setText("Esperando pedido...");
                findViewById(R.id.waiting_button).setVisibility(View.GONE);
            }
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
        }
    }

    private void getSaldo(){
        viewModel.getSaldo();
    }

    public void updateSaldo(ParseObject object){
        saldo = object.getNumber("Saldo");
        TextView saldoView = (TextView) findViewById(R.id.saldo);
        NumberFormat formatter = new DecimalFormat("#0.00");
        saldoView.setText("$" + formatter.format(saldo));

        pedidosList = (ListView) findViewById(R.id.pedidos_list);
        pedidosAdapter = new PedidosAdapter();
        pedidosList.setAdapter(pedidosAdapter);
        loadPedidos();

    }

    private void loadPedidos(){
        viewModel.getPedidosPendientes();
    }

    public void updatePedidos(List<ParseObject> pedidos){
        swipeLayout.setRefreshing(false);
        pedidosAdapter.updatePedidos(pedidos);
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
                cantidad.setText(pedidos.get(i).getNumber("Unidades") + " unidades");
            }

            NumberFormat formatter = new DecimalFormat("#0.00");
            TextView precio = (TextView)convertView.findViewById(R.id.pedido_valor);
            precio.setText("$" + formatter.format(pedidos.get(i).getNumber("Valor")));
            TextView carga = (TextView)convertView.findViewById(R.id.pedido_carga);
            carga.setText("Carga: " + utils.formatDate(pedidos.get(i).getDate("HoraCarga")));
            TextView entrega = (TextView)convertView.findViewById(R.id.pedido_entrega);
            entrega.setText("Entrega: " + utils.formatDate(pedidos.get(i).getDate("HoraEntrega")));
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
                    viewModel.tomarPedido(((ParseObject) view.getTag()).getObjectId().toString());
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
}
