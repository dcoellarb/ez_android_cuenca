package com.easyruta.easyruta.viewmodel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.utils.DataService;
import com.easyruta.easyruta.utils.PubnubService;
import com.easyruta.easyruta.viewcontroller.MainActivity;
import com.easyruta.easyruta.viewcontroller.PedidoActivoActivity;
import com.easyruta.easyruta.viewcontroller.PedidoPendienteActivity;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by dcoellar on 11/30/15.
 */
public class MainViewModel {

    EasyRutaApplication application;
    MainActivity activity;
    DataService dataService;
    PubnubService pubnubService;
    ParseObject transportista;
    ParseObject proveedor;

    public Boolean isTransportistaIndependiente;

    public MainViewModel(MainActivity activity){

        this.activity = activity;
        this.application = (EasyRutaApplication) activity.getApplication();
        this.dataService = this.application.getDataService();
        this.pubnubService = this.application.getPubnubService();
        transportista = dataService.getTransportista();
        proveedor = dataService.getProveedor();
        isTransportistaIndependiente = proveedor == null;

    }

    public void getPedidosPendientes(){
        dataService.getPedidoPendientes(transportista,new FindCallback<ParseObject>() {
            public void done(List<ParseObject> pedidos, ParseException e) {
                if (e == null) {
                    activity.updatePedidos(pedidos);
                } else {
                    Log.d("ERROR", "Error: " + e.getMessage());
                }
            }
        });
    }

    public void getSaldo(){
        dataService.getTransportistaSaldo(transportista, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    activity.updateSaldo(object);
                } else {
                    Log.e("ERROR", "Could not get transportista");
                    if (e != null) {
                        Log.e("ERROR", e.getMessage());
                    } else {
                        Log.e("ERROR", "result is null for id:" + transportista.getObjectId());
                    }
                }
            }
        });
    }

    public void tomarPedido(final String id) {
        dataService.tomarPedido(id, transportista, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    application.getPubnubService().getPubnub().publish(pubnubService.PEDIDO_TOMADO, id, new Callback() {
                    });

                    moveToPedidoPendiente(id);
                } else {
                    Toast toast = Toast.makeText(activity.getBaseContext(), transportista.getObjectId().toString(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

    }

    public void getPedido(String id, GetCallback<ParseObject> callback){
        dataService.getPedido(id, callback);
    }

    public void pubNubSuscriptions(){
        Pubnub pubnub = pubnubService.getPubnub();

        JSONObject jso = new JSONObject();
        try {
            jso.put("type","transportista");
            jso.put("email",dataService.getUser().getEmail());
            jso.put("name", dataService.getTransportista().get("Nombre"));
            pubnub.setState(pubnubService.NEW_PEDIDOS, dataService.getUser().getObjectId(), jso, new Callback() {
                public void successCallback(String channel, Object response) {
                }

                public void errorCallback(String channel, PubnubError error) {
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (isTransportistaIndependiente) {
                pubnubService.getPubnub().subscribe(pubnubService.NEW_PEDIDOS, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_RECHAZADO, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_RECHAZADO_PROVEEDOR, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO_TRANSPORTISTA, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO_PROVOEEDOR, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_TIMEOUT, new Callback() {
                    public void successCallback(String channel, Object message) {
                        getPedidosPendientes();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
            }

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_TOMADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    if (isTransportistaIndependiente){
                        getPedidosPendientes();
                    }else{
                        try {
                            getPedido(((JSONObject) message).getString("id"), new GetCallback<ParseObject>() {
                                @Override
                                public void done(ParseObject object, ParseException e) {
                                    if (e == null) {
                                        if (object.getParseObject("Proveedor").getObjectId().toString().equalsIgnoreCase(proveedor.getObjectId().toString())
                                                && object.getParseObject("Transportista").getObjectId().toString().equalsIgnoreCase(transportista.getObjectId())){
                                            moveToPedidoPendiente(object.getObjectId().toString());
                                        }
                                    } else {
                                        Log.e("ERROR", e.getMessage());
                                    }
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

            if (!isTransportistaIndependiente){
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CONFIRMADO_PROVEEDOR, new Callback() {
                    public void successCallback(String channel, Object message) {
                        try {
                            getPedido(((JSONObject) message).getString("id"), new GetCallback<ParseObject>() {
                                @Override
                                public void done(ParseObject object, ParseException e) {
                                    if (e == null) {
                                        if (object.getParseObject("Proveedor").getObjectId().toString().equalsIgnoreCase(proveedor.getObjectId().toString())
                                                && object.getParseObject("Transportista").getObjectId().toString().equalsIgnoreCase(transportista.getObjectId())){
                                            moveToPedidoActivo(object.getObjectId().toString());
                                        }
                                    } else {
                                        Log.e("ERROR", e.getMessage());
                                    }
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
            }


        } catch (PubnubException e) {
            e.printStackTrace();
        }

    }

    private void moveToPedidoPendiente(String id){
        SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", activity.MODE_PRIVATE).edit();
        prefs.putString("pedido", id);
        prefs.putString("estado", dataService.PENDIENTE_CONFIRMACION);
        prefs.commit();

        pubnubService.getPubnub().unsubscribe(pubnubService.NEW_PEDIDOS);
        pubnubService.getPubnub().unsubscribeAll();

        Intent intent = new Intent(activity, PedidoPendienteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    private void moveToPedidoActivo(String id){
        SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", activity.MODE_PRIVATE).edit();
        prefs.putString("pedido", id);
        prefs.putString("estado", dataService.ACTIVO);
        prefs.commit();

        pubnubService.getPubnub().unsubscribe(pubnubService.NEW_PEDIDOS);
        pubnubService.getPubnub().unsubscribeAll();

        Intent intent = new Intent(activity, PedidoActivoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
