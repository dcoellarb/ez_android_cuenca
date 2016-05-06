package com.easyruta.easyruta.viewmodel;

import android.util.Log;
import android.widget.Toast;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.utils.DataService;
import com.easyruta.easyruta.utils.PubnubService;
import com.easyruta.easyruta.viewcontroller.MainActivity;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
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
        dataService.getPedidoPendientes(transportista, new FindCallback<ParseObject>() {
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

    public void tomarPedido(final ParseObject pedido) {
        dataService.tomarPedido(pedido.getObjectId(), transportista, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    application.getPubnubService().getPubnub().publish(pubnubService.PEDIDO_ASIGNADO, pedido.getObjectId(), new Callback() {});
                    updatePedido(pedido);
                } else {
                    Log.e("ERROR:",e.getMessage());
                    Toast toast = Toast.makeText(activity.getBaseContext(), "Ooops!!! Estamos teniendo problemas con nuestros servidores, por favor intente mas tarde o contacte a soporte.", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

    }

    public void getPedido(String id, GetCallback<ParseObject> callback){
        dataService.getPedido(id, callback);
    }

    private void updatePedido(ParseObject pedido){
        dataService.setPedido(pedido);

        pubnubService.getPubnub().unsubscribe(pubnubService.PEDIDO_CREADO);
        pubnubService.getPubnub().unsubscribeAll();

        activity.moveToPedidoActivo(pedido);
    }

    public void pubNubSuscriptions(){
        Pubnub pubnub = pubnubService.getPubnub();

        JSONObject jso = new JSONObject();
        try {
            jso.put("type","transportista");
            jso.put("email",dataService.getUser().getEmail());
            jso.put("name", dataService.getTransportista().get("Nombre"));
            pubnub.setState(pubnubService.PEDIDO_CREADO, dataService.getUser().getObjectId(), jso, new Callback() {
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
                Log.d("TEST DANIEL", "subribing");
                pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CREADO, new Callback() {
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
            }

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_ASIGNADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    if (isTransportistaIndependiente){
                        getPedidosPendientes();
                    }else{
                        try {
                            getPedido(((JSONObject) message).getString("id"), new GetCallback<ParseObject>() {
                                @Override
                                public void done(ParseObject object, ParseException e) {
                                    if (e == null) {

                                        if (object.getParseObject("transportista") != null && object.getParseObject("transportista").getObjectId().toString().equalsIgnoreCase(proveedor.getObjectId().toString())
                                                && object.getParseObject("chofer") != null && object.getParseObject("chofer").getObjectId().toString().equalsIgnoreCase(transportista.getObjectId())){
                                            updatePedido(object);
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
                //TODO Tomar en cuenta cuando se implemente despachador
                /*
                pubnubService.getPubnub().subscribe(pubnubService.TRANSPORTISTA_HABILITADO, new Callback() {
                    public void successCallback(String channel, Object message) {
                        refreshTransportista();
                    }

                    public void errorCallback(String channel, PubnubError error) {
                        Log.e("ERROR", error.getErrorString());
                    }
                });
                */
            }


        } catch (PubnubException e) {
            e.printStackTrace();
        }

    }

    //TODO tomar en cuenta cuando se tenga despachador
    /*
    public void MarcarDisponible(){
        transportista.put("estado", "disponible");
        transportista.put("horaDisponible", new Date());
        transportista.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {

                    application.getPubnubService().getPubnub().publish(pubnubService.TRANSPORTISTA_HABILITADO, transportista.getObjectId().toString(), new Callback() {
                    });

                    activity.toogleDisponible();
                } else {
                    Log.e("ERROR", e.getMessage());
                    Toast toast = new Toast(activity);
                    toast.setText("No se pudo actualizar su estado en este momento, por favor intente mas tarde");
                    toast.show();
                }
            }
        });
    }
    */
    public void refreshTransportista(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Chofer");
        query.whereEqualTo("objectId",transportista.getObjectId());
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null){
                    dataService.setTransportista(object);
                    activity.toogleDisponible();
                }
            }
        });
    }

    public ParseObject getTransportista() {
        return transportista;
    }
}
