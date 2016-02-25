package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.utils.utils;
import com.easyruta.easyruta.viewmodel.PedidoPendienteViewModel;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.pubnub.api.Pubnub;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

/**
 * Created by dcoellar on 9/26/15.
 */
public class PedidoPendienteActivity extends Activity {

    public static String PARAM_ID = "id";

    private Activity activity;
    private PedidoPendienteViewModel viewModel;
    private ProgressBar progressBar;
    private TextView counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_pedido_pendiente);
        viewModel = new PedidoPendienteViewModel(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        viewModel.pubNubSuscriptions();
        loadPedido();
    }

    private void loadPedido(){
        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        String id = prefs.getString("pedido", "");
        viewModel.getPedido(id);
    }

    public void setPedido(ParseObject pedido){
        TextView viaje = (TextView)findViewById(R.id.pedido_viaje);
        try{
            String origen  = pedido.getParseObject("CiudadOrigen").fetchIfNeeded().getString("Nombre");
            String destino  = pedido.getParseObject("CiudadDestino").fetchIfNeeded().getString("Nombre");
            viaje.setText(origen + " - " + destino);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        NumberFormat formatter = new DecimalFormat("#0.00");

        TextView precio = (TextView)findViewById(R.id.pedido_valor);
        precio.setText("$" + formatter.format(pedido.getNumber("Valor")));

        TextView peso = (TextView)findViewById(R.id.pedido_peso);
        peso.setText("Peso desde:" + String.valueOf(pedido.getNumber("PesoDesde")) + " hasta " + String.valueOf(pedido.getNumber("PesoHasta")) + " Tn");

        TextView carga = (TextView)findViewById(R.id.pedido_carga);
        carga.setText("Carga: " + utils.formatDate(pedido.getDate("HoraCarga")));
        TextView entrega = (TextView)findViewById(R.id.pedido_entrega);
        entrega.setText("Entrega: " + utils.formatDate(pedido.getDate("HoraEntrega")));

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

        LinearLayout cancelar = (LinearLayout)findViewById(R.id.pedido_pendiente_cancelar);
        if (viewModel.isTransportistaIndependiente) {
            cancelar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    cancelPedido();
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

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(pedido.getDate("HoraSeleccion"));
        calEnd.add(Calendar.MINUTE, 30);
        Calendar calCurrent = Calendar.getInstance();
        long progress = calEnd.getTimeInMillis() - calCurrent.getTimeInMillis();

        progressBar = (ProgressBar) findViewById(R.id.pending_progress);
        counter = (TextView) findViewById(R.id.pending_counter);
        new CountDownTimer(progress, 1000) {

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
                cancelPedido();
            }
        }.start();


    }

    private void cancelPedido(){
        viewModel.cancelarPedido();
    }

    public void cancelActivity(){
        SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta",MODE_PRIVATE).edit();
        prefs.remove("pedido");
        prefs.remove("estado");
        prefs.commit();

        Pubnub pubnub = ((EasyRutaApplication)getApplication()).getPubnubService().getPubnub();
        pubnub.unsubscribeAll();

        Intent i = new Intent(getBaseContext(), MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
