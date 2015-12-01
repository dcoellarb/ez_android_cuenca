package com.easyruta.easyruta.viewmodel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.utils.DataService;
import com.easyruta.easyruta.utils.GPSTracker;
import com.easyruta.easyruta.utils.PubnubService;
import com.easyruta.easyruta.viewcontroller.PedidoActivoActivity;
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
public class PedidoActivoViewModel {

    EasyRutaApplication application;
    PedidoActivoActivity activity;
    PubnubService pubnubService;
    DataService dataService;
    ParseObject pedido;
    private GPSTracker gpsTracker;

    public Boolean isTransportistaIndependiente;

    public PedidoActivoViewModel(PedidoActivoActivity activity){

        this.activity = activity;
        this.application = (EasyRutaApplication) activity.getApplication();
        this.dataService = this.application.getDataService();
        this.pubnubService = this.application.getPubnubService();

        isTransportistaIndependiente = dataService.getProveedor() == null;

    }

    public void getPedido(final String id){
        dataService.getPedido(id, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    pedido = object;
                    activity.setPedido(object);
                    if (object.getString("Estado").equalsIgnoreCase(dataService.EN_CURSO)) {
                        gpsTracker = new GPSTracker(activity);
                        activity.toggleEstado();
                    }
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
        if (pedido.get("Estado").toString().equalsIgnoreCase(dataService.EN_CURSO)){
            estado = dataService.CANCELADO;
        }
        dataService.cancelarPedido(pedido, estado, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    dataService.getTransportista().increment("PedidosCancelados", 1);
                    dataService.getTransportista().saveInBackground();

                    JSONObject json = new JSONObject();
                    try {
                        json.put("id",pedido.getObjectId().toString());
                        json.put("uuid", pubnubService.getUuid());
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    pubnubService.getPubnub().publish(pubnubService.PEDIDO_CANCELADO_TRANSPORTISTA, pedido.getObjectId().toString(), new Callback() {
                    });

                    activity.closeActivity();
                } else {
                    Log.e("ERROR", e.getMessage());
                }
            }
        });
    }

    public void iniciarPedido(){
        dataService.iniciarPedido(pedido, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    if (e == null) {
                        dataService.getPedido(pedido.getObjectId().toString(), new GetCallback<ParseObject>() {
                            @Override
                            public void done(ParseObject object, ParseException e) {
                                pedido = object;
                            }
                        });

                        JSONObject json = new JSONObject();
                        try {
                            json.put("id",pedido.getObjectId().toString());
                            json.put("uuid", pubnubService.getUuid());
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        application.getPubnubService().getPubnub().publish(pubnubService.PEDIDO_INICIADO,json, new Callback() {
                        });
                        gpsTracker = new GPSTracker(activity);
                        activity.toggleEstado();
                    }
                }
            }
        });
    }

    public void finalizarPedido(){
        dataService.finalizarPedido(pedido, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    if (e == null) {
                        dataService.getTransportista().increment("PedidosCompletados", 1);
                        dataService.getTransportista().saveInBackground();

                        JSONObject json = new JSONObject();
                        try {
                            json.put("id",pedido.getObjectId().toString());
                            json.put("uuid", pubnubService.getUuid());
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        application.getPubnubService().getPubnub().publish(pubnubService.PEDIDO_COMPLETADO, pedido.getObjectId().toString(), new Callback() {
                        });

                        gpsTracker.stopUsingGPS();

                        activity.closeActivity();
                    }
                }
            }
        });
    }

    public GPSTracker getGpsTracker() {
        return gpsTracker;
    }

    public void pubNubSuscriptions() {
        try {
            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_INICIADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id")) && !isTransportistaIndependiente) {
                            if (!((JSONObject)message).getString("uuid").equalsIgnoreCase(pubnubService.getUuid())) {
                                gpsTracker = new GPSTracker(activity);
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        activity.toggleEstado();
                                    }
                                });
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread (new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Pedido cancelado")
                                            .setMessage("El cliente ha cancelado este pedido.")
                                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    activity.closeActivity();
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
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO_CONFIRMADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread (new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Pedido cancelado")
                                            .setMessage("El cliente ha cancelado este pedido.")
                                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    activity.closeActivity();
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
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO_CONFIRMADO, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread (new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(activity)
                                            .setTitle("Pedido cancelado")
                                            .setMessage("El cliente ha cancelado este pedido.")
                                            .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    activity.closeActivity();
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
                    Log.e("ERROR", error.getErrorString());
                }
            });

            pubnubService.getPubnub().subscribe(pubnubService.PEDIDO_CANCELADO_CONFIRMADO_PROVEEDOR, new Callback() {
                public void successCallback(String channel, Object message) {
                    try {
                        if (pedido.getObjectId().toString().equalsIgnoreCase(((JSONObject)message).getString("id"))) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    activity.closeActivity();
                                }
                            });
                        }
                    }catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ERROR", error.getErrorString());
                }
            });

        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

}
