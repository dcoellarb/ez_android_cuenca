package com.easyruta.easyruta.utils;

import com.easyruta.easyruta.Constants;
import com.easyruta.easyruta.EasyRutaApplication;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dcoellar on 11/27/15.
 */

public class DataService {

    //Statics
    public static String PENDIENTE = "Pendiente";
    public static String PENDIENTE_CONFIRMACION = "PendienteConfirmacion";
    public static String PENDIENTE_CONFIRMACION_PROVEEDOR = "PendienteConfirmacionProveedor";
    public static String ACTIVO = "Activo";
    public static String EN_CURSO = "EnCurso";
    public static String COMPLETADO = "Completado";
    public static String CANCELADO = "Cancelado";
    public static String CANCELADO_CLIENTE = "CanceladoCliente";

    //Variables
    private ParseUser user;
    private DataService dataService;

    public DataService(EasyRutaApplication application){

        dataService = this;

        Parse.enableLocalDatastore(application);
        Parse.initialize(application, Constants.PARSE_APPLICATION_ID, Constants.PARSE_CLIENT_KEY);

    }

    //Getter and Setters
    public ParseUser getUser() {
        if (user == null){
            user = ParseUser.getCurrentUser();
        }
        return user;
    }

    //Public Methods
    public void getUserTransportista(GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Transportista");
        query.whereEqualTo("user", this.user);
        query.getFirstInBackground(callback);
    }
    public void getTransportistaCurrentPedido(GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> pendienteQuery = ParseQuery.getQuery("Pedido");
        pendienteQuery.whereEqualTo("Estado", dataService.PENDIENTE_CONFIRMACION);
        ParseQuery<ParseObject> activoQuery = ParseQuery.getQuery("Pedido");
        activoQuery.whereEqualTo("Estado", dataService.ACTIVO);
        ParseQuery<ParseObject> encursoQuery = ParseQuery.getQuery("Pedido");
        encursoQuery.whereEqualTo("Estado", dataService.EN_CURSO);
        List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
        queries.add(pendienteQuery);
        queries.add(activoQuery);
        queries.add(encursoQuery);

        ParseQuery<ParseObject> query = ParseQuery.or(queries);
        query.getFirstInBackground(callback);
    }

    /*
    public void setUser(ParseUser user) {
        this.user = user;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Transportista");
        query.whereEqualTo("user", user);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject transportista, ParseException e) {
                if (e == null && transportista != null) {
                    setTransportista(transportista);

                    ParseQuery<ParseObject> pendienteQuery = ParseQuery.getQuery("Pedido");
                    pendienteQuery.whereEqualTo("Estado", dataService.PENDIENTE_CONFIRMACION);
                    ParseQuery<ParseObject> activoQuery = ParseQuery.getQuery("Pedido");
                    activoQuery.whereEqualTo("Estado", dataService.ACTIVO);
                    ParseQuery<ParseObject> encursoQuery = ParseQuery.getQuery("Pedido");
                    encursoQuery.whereEqualTo("Estado", dataService.EN_CURSO);
                    List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
                    queries.add(pendienteQuery);
                    queries.add(activoQuery);
                    queries.add(encursoQuery);

                    ParseQuery<ParseObject> query = ParseQuery.or(queries);
                    query.whereEqualTo("Transportista", transportista);
                    query.getFirstInBackground(new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject object, ParseException e) {
                            if (e == null && object != null){
                                SharedPreferences.Editor prefs = getSharedPreferences("easyruta", MODE_PRIVATE).edit();
                                prefs.putString("pedido",object.getObjectId());
                                prefs.putString("estado",object.getString("Estado"));
                                prefs.commit();
                            }
                            afterSetUser();
                        }
                    });

                } else {
                    if (e != null){
                        Log.e("ERROR", "Error: " + e.getMessage());
                    }else{
                        Log.e("ERROR", "Transportista could not be retrive.");
                    }

                    ParseUser.getCurrentUser().logOutInBackground(new LogOutCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e("ERROR", "Error: " + e.getMessage());
                            }

                            Intent intent = new Intent(application, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    });
                }
            }
        });
    }


    public void afterSetUser() {
        pubnub = new com.pubnub.api.Pubnub(Constants.PUBNUB_PUB_KEY, Constants.PUBNUB_SUB_KEY);
        String uuid = this.user.getObjectId();
        pubnub.setUUID(uuid);

        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        if (prefs.contains("pedido")){
            ParseQuery query = new ParseQuery("Pedido");
            try {
                ParseObject pedido = query.get(prefs.getString("pedido", ""));
                if (pedido != null &&
                        (pedido.getString("Estado").equalsIgnoreCase(getString(R.string.status_parse_pendiente_confirmacion))
                                || pedido.getString("Estado").equalsIgnoreCase(getString(R.string.status_parse_activo))
                                || pedido.getString("Estado").equalsIgnoreCase(getString(R.string.status_parse_encurso))
                        )) {

                    if (prefs.getString("estado", "").equalsIgnoreCase(getString(R.string.status_parse_pendiente_confirmacion))) {
                        Intent intent = new Intent(this, PedidoPendienteActivity.class);
                        intent.putExtra(PedidoPendienteActivity.PARAM_ID, prefs.getString("pedido", ""));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(this, PedidoActivoActivity.class);
                        intent.putExtra(PedidoPendienteActivity.PARAM_ID, prefs.getString("pedido", ""));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                } else {
                    SharedPreferences.Editor prefsEdit = this.getSharedPreferences("easyruta", MODE_PRIVATE).edit();
                    prefsEdit.remove("pedido");
                    prefsEdit.remove("estado");
                    prefsEdit.commit();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }catch (ParseException e){
                Log.e("ERROR", e.getMessage());

                if (e.getCode()==101){
                    SharedPreferences.Editor prefsEdit = this.getSharedPreferences("easyruta", MODE_PRIVATE).edit();
                    prefsEdit.remove("pedido");
                    prefsEdit.remove("estado");
                    prefsEdit.commit();
                }

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }else{
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
    public ParseObject getTransportista() {
        return transportista;
    }
    public void setTransportista(ParseObject transportista) {
        this.transportista = transportista;
    }
    */
}
