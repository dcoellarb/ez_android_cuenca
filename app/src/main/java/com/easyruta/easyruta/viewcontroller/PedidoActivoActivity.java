package com.easyruta.easyruta.viewcontroller;

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
import android.view.View;
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
import com.squareup.picasso.Picasso;

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
    private LinearLayout iniciar;
    private LinearLayout finalizar;
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
        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        final String id = prefs.getString("pedido", "");

        viewModel.getPedido(id);

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
                    requestPermissions(permissions, 1);
                } else {
                    callMap();
                }
            }
        });

        iniciar = (LinearLayout) findViewById(R.id.pedido_iniciar);
        finalizar = (LinearLayout) findViewById(R.id.pedido_finalizar);
        cancelar = (LinearLayout)findViewById(R.id.pedido_cancelar);
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
        if (viewModel.isTransportistaIndependiente) {
            cancelar.setOnClickListener(new View.OnClickListener() {
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
        }else{
            cancelar.setVisibility(View.GONE);
        }

    }

    public void setPedido(ParseObject pedido){
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

        TextView direccion_origen = (TextView)findViewById(R.id.pedido_direccion_origen);
        direccion_origen.setText(pedido.getString("DireccionOrigen"));
        TextView direccion_destino = (TextView)findViewById(R.id.pedido_direccion_destino);
        direccion_destino.setText(pedido.getString("DireccionDestino"));

        TextView producto = (TextView)findViewById(R.id.pedido_producto);
        producto.setText(pedido.getString("Producto"));

        NumberFormat formatter = new DecimalFormat("#0.00");

        TextView precio = (TextView)findViewById(R.id.pedido_valor);
        precio.setText("$" + formatter.format(pedido.getNumber("Valor")));

        TextView peso = (TextView)findViewById(R.id.pedido_peso);
        if (pedido.getString("TipoUnidad").equalsIgnoreCase("peso")){
            peso.setText("Peso desde:" + String.valueOf(pedido.getNumber("PesoDesde")) + " hasta " + String.valueOf(pedido.getNumber("PesoHasta")) + " Tn");
        }else{
            peso.setText(pedido.getNumber("Unidades") + " unidades");
        }
        TextView carga = (TextView)findViewById(R.id.pedido_carga);
        carga.setText("Carga: " + utils.formatDate(pedido.getDate("HoraCarga")));
        TextView entrega = (TextView)findViewById(R.id.pedido_entrega);
        entrega.setText("Entrega: " + utils.formatDate(pedido.getDate("HoraEntrega")));

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

    public void toggleEstado(){
        iniciar.setVisibility(View.GONE);
        finalizar.setVisibility(View.VISIBLE);
        navImageView.setVisibility(View.VISIBLE);
    }

    public void cancelPedido(final Boolean broadcast){
        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        final String id = prefs.getString("pedido", "");

        viewModel.cancelarPedido();
    }

    public void closeActivity(){
        SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", MODE_PRIVATE).edit();
        prefs.remove("pedido");
        prefs.remove("estado");
        prefs.commit();

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
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
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
