package com.easyruta.easyruta.viewmodel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.utils.DataService;
import com.easyruta.easyruta.utils.PubnubService;
import com.easyruta.easyruta.viewcontroller.PedidoActivoActivity;
import com.easyruta.easyruta.viewcontroller.PedidoPendienteActivity;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dcoellar on 11/30/15.
 */
public class PedidoPendienteViewModel {

    EasyRutaApplication application;
    PedidoPendienteActivity activity;
    PubnubService pubnubService;
    DataService dataService;
    ParseObject pedido;
    ParseObject transportista;
    ParseObject proveedor;

    public Boolean isTransportistaIndependiente;

    public PedidoPendienteViewModel(PedidoPendienteActivity activity){

        this.activity = activity;
        this.application = (EasyRutaApplication) activity.getApplication();
        this.dataService = this.application.getDataService();
        this.pubnubService = this.application.getPubnubService();
        transportista = dataService.getTransportista();
        proveedor = dataService.getProveedor();
        isTransportistaIndependiente = proveedor == null;
    }

    public void getPedido(final String id){
        dataService.getPedido(id, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    pedido = object;
                    activity.setPedido(object);
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
    }

    public void cancelarPedido(){
        String estado = dataService.PENDIENTE;
        dataService.cancelarPedido(pedido, estado, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("id",pedido.getObjectId().toString());
                        json.put("uuid", pubnubService.getUuid());
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    pubnubService.getPubnub().publish(pubnubService.PEDIDO_CANCELADO_TRANSPORTISTA, json, new Callback() {
                    });

                    activity.cancelActivity();
                } else {
                    Log.e("ERROR", e.getMessage());
                }
            }
        });
    }

    public void pubNubSuscriptions(){
        try {
            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CONFIRMADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))){
                            SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", activity.MODE_PRIVATE).edit();
                            prefs.putString("estado", dataService.ACTIVO);
                            prefs.commit();

                            pubnubService.getPubnub().unsubscribeAll();

                            Intent intent = new Intent(activity, PedidoActivoActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            activity.startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_RECHAZADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Pedido no aceptado")
                                            .setMessage("El cliente no ha aceptado este pedido.")
                                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    activity.cancelActivity();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try{
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Pedido cancelado")
                                            .setMessage("El cliente ha cancelado este pedido.")
                                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    activity.cancelActivity();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO_PROVOEEDOR, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Pedido cancelado")
                                            .setMessage("El proveedor ha cancelado este pedido.")
                                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    activity.cancelActivity();
                                                }
                                            })
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .show();
                                }
                            });

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR",error.getErrorString());
                }
            });

        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }
}
