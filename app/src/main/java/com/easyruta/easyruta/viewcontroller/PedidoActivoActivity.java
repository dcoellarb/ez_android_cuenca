package com.easyruta.easyruta.viewcontroller;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.utils.utils;
import com.easyruta.easyruta.viewmodel.PedidoActivoViewModel;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.pubnub.api.Pubnub;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by dcoellar on 9/26/15.
 */
public class PedidoActivoActivity extends Activity {

    public static String PARAM_ID = "id";
    private Activity activity;
    private PedidoActivoViewModel viewModel;
    private ImageView navImageView;
    private Button iniciar;
    private Button finalizar;
    private LinearLayout cancelar;
    private String contactoNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_pedido_activo);
        viewModel = new PedidoActivoViewModel(this);
        viewModel.pubNubSuscriptions();
        loadPedido();
    }

    private void loadPedido() {
        ImageView phoneImageView = (ImageView) findViewById(R.id.pedido_call);
        phoneImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        String[] permissions = new String[]{Manifest.permission.CALL_PHONE};
                        requestPermissions(permissions,0);
                    } else {
                        makeCall();
                    }
                }else{
                    makeCall();
                }
            }
        });

        navImageView = (ImageView)findViewById(R.id.pedido_navigate);
        navImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                        requestPermissions(permissions, 1);
                    } else {
                        callMap();
                    }
                }else{
                    callMap();
                }
            }
        });

        iniciar = (Button) findViewById(R.id.pedido_iniciar);
        finalizar = (Button) findViewById(R.id.pedido_finalizar);
        iniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.iniciarPedido();
            }
        });
        finalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.finalizarPedido();
            }
        });
    }

    public void setPedido(ParseObject pedido){
        ImageView empresaImage = (ImageView)findViewById(R.id.pedido_company_image);
        TextView contacto = (TextView)findViewById(R.id.pedido_contacto);
        try{
            ParseObject empresa = pedido.getParseObject("proveedorCarga").fetchIfNeeded();
            contacto.setText(empresa.getString("contacto"));

            /*
            Picasso.with(this.getBaseContext())
                    .load(empresa.getString("imageUrl"))
                    .placeholder(R.drawable.account)
                    .into(empresaImage);
            */

            contactoNumber = empresa.getString("telefono");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView viaje = (TextView)findViewById(R.id.pedido_viaje);
        String origen  = pedido.getString("ciudadOrigen");
        String destino  = pedido.getString("ciudadDestino");
        viaje.setText(origen + " - " + destino);

        TextView direccion_origen = (TextView)findViewById(R.id.pedido_direccion_origen);
        direccion_origen.setText(pedido.getString("direccionOrigen"));
        TextView direccion_destino = (TextView)findViewById(R.id.pedido_direccion_destino);
        direccion_destino.setText(pedido.getString("direccionDestino"));

        TextView producto = (TextView)findViewById(R.id.pedido_producto);
        producto.setText(pedido.getString("producto"));

        findViewById(R.id.pedido_valor).setVisibility(View.GONE);
        findViewById(R.id.pedido_comision).setVisibility(View.GONE);
        findViewById(R.id.pedido_comision_text).setVisibility(View.GONE);
        NumberFormat formatter = new DecimalFormat("#0.00");
        TextView precio = (TextView)findViewById(R.id.pedido_valor);
        if (pedido.getBoolean("donacion")){
            precio.setVisibility(View.VISIBLE);
            precio.setText("DONACION");
        } else if(pedido.getParseObject("transportista") == null) {
            precio.setVisibility(View.VISIBLE);
            precio.setText("$" + formatter.format(pedido.getNumber("valor")));
            TextView comision = (TextView)findViewById(R.id.pedido_comision);
            comision.setVisibility(View.VISIBLE);
            comision.setText("$" + formatter.format(pedido.getNumber("comision")));
            findViewById(R.id.pedido_comision_text).setVisibility(View.VISIBLE);
        }


        TextView peso = (TextView)findViewById(R.id.pedido_peso);
        peso.setText(utils.formatPeso(pedido.getString("peso")));

        TextView carga = (TextView)findViewById(R.id.pedido_carga);
        carga.setText(utils.formatDate(pedido.getDate("horaCarga")));
        TextView entrega = (TextView)findViewById(R.id.pedido_entrega);
        if (pedido.getDate("horaEntrega") != null){
            entrega.setText(utils.formatDate(pedido.getDate("horaEntrega")));
        }else{
            findViewById(R.id.pedido_entrega_text).setVisibility(View.GONE);
            entrega.setVisibility(View.GONE);
        }
    }

    public void toggleEstado(){
        iniciar.setVisibility(View.GONE);
        finalizar.setVisibility(View.VISIBLE);
        navImageView.setVisibility(View.VISIBLE);
    }

    public void closeActivity(){
        Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnubService().getPubnub();
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
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (viewModel.getGpsTracker().isGPSEnabled && viewModel.getGpsTracker().isGPSTrackingEnabled && viewModel.getGpsTracker().isNetworkEnabled){
                String uri = "geo:"+ viewModel.getGpsTracker().getLatitude() + "," + viewModel.getGpsTracker().getLongitude();
                startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
            }else{
                Toast toast = new Toast(activity);
                toast.setText(R.string.no_gps_message);
                toast.show();
            }
        }
    }
}
