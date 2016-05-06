package com.easyruta.easyruta.utils;

import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.utils.dataService.DataServiceFilter;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by dcoellar on 11/27/15.
 */

public class DataService {

    //Statics
    public static String PENDIENTE = "Pendiente";
    public static String ACTIVO = "Activo";
    public static String EN_CURSO = "EnCurso";
    public static String FINALIZADO = "Finalizado";
    public static String CANCELADO = "Cancelado";

    //Variables
    private DataService dataService;
    private ParseUser user;
    private ParseObject transportista;
    private ParseObject proveedor;
    private ParseObject pedido;

    public DataService(EasyRutaApplication application){

        dataService = this;

        Parse.setLogLevel(Log.VERBOSE);
        Parse.enableLocalDatastore(application);
        Parse.initialize(new Parse.Configuration.Builder(application)
                .applicationId(com.easyruta.easyruta.Constants.PARSE_APPLICATION_ID)
                .clientKey(null)
                .server(com.easyruta.easyruta.Constants.PARSE_SERVER_URL)
                .build()
        );
    }

    private ParseQuery<ParseObject> buildFilter(ParseQuery<ParseObject> query, DataServiceFilter filter) {
        switch (filter.getOperator()){
            case "=":
                query.whereEqualTo(filter.getField(), filter.getValue());
                break;
            case ">":
                query.whereGreaterThan(filter.getField(), filter.getValue());
                break;
            case ">=":
                query.whereGreaterThanOrEqualTo(filter.getField(), filter.getValue());
                break;
            case "<":
                query.whereLessThan(filter.getField(), filter.getValue());
                break;
            case "<=":
                query.whereLessThanOrEqualTo(filter.getField(), filter.getValue());
                break;
            case "!=":
                if (filter.getValue() == null){
                    query.whereExists(filter.getField());
                } else {
                    query.whereNotEqualTo(filter.getField(), filter.getValue());
                }
                break;
            case "containedIn":
                query.whereContainedIn(filter.getField(), (Collection<?>) filter.getValue());
                break;
            case "contains":
                query.whereContains(filter.getField(), filter.getValue().toString());
                break;
            case "containsAll":
                query.whereContainsAll(filter.getField(), (Collection<?>) filter.getValue());
                break;
            case "startsWith":
                query.whereStartsWith(filter.getField(), filter.getValue().toString());
                break;
        }
        return query;
    }
    /*
    private ParseQuery<ParseObject> buildQuery(EasyrutaDataCollections collection, DataServiceQueryParameters params){
        ParseQuery<ParseObject> query;
        if (params != null){
            if (params.getFilters() != null){

            }
        }
        return query;
    }
    */

    //Getter and Setters
    public ParseUser getUser() {
        if (user == null){
            user = ParseUser.getCurrentUser();
        }
        return user;
    }
    public void setUser(ParseUser user) {
        this.user = user;
    }

    //Getter and Setters
    public ParseObject getTransportista() {
        return transportista;
    }
    public void setTransportista(ParseObject object) {
         transportista = object;
    }
    public ParseObject getProveedor() {
        return proveedor;
    }
    public void setProveedor(ParseObject proveedor) {
        this.proveedor = proveedor;
    }
    public ParseObject getPedido() {
        return pedido;
    }
    public void setPedido(ParseObject pedido) {
        this.pedido = pedido;
    }

    //Public Methods
    public void getUserTransportista(GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Chofer");
        query.include("transportista");
        query.whereEqualTo("user", this.user);
        String id = this.user.getObjectId();
        Log.d("TEST",id);
        query.getFirstInBackground(callback);
    }
    public void getTransportistaCurrentPedido(GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> activoQuery = ParseQuery.getQuery("Pedido");
        activoQuery.whereEqualTo("estado", dataService.ACTIVO);
        ParseQuery<ParseObject> encursoQuery = ParseQuery.getQuery("Pedido");
        encursoQuery.whereEqualTo("estado", dataService.EN_CURSO);
        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(activoQuery);
        queries.add(encursoQuery);

        ParseQuery<ParseObject> query = ParseQuery.or(queries);
        query.getFirstInBackground(callback);
    }
    public void login(String user, String password, LogInCallback callback){
        ParseUser.logInInBackground(user, password, callback);
    }
    public void getPedidoPendientes(ParseObject transportista,FindCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("estado", dataService.PENDIENTE);
        query.whereEqualTo("tipoCamion", transportista.getString("tipoCamion"));
        query.orderByDescending("createdAt");
        query.findInBackground(callback);
    }
    public void getTransportistaSaldo(ParseObject chofer,GetCallback<ParseObject> callback){
        ParseQuery query = new ParseQuery("Chofer");
        query.whereEqualTo("objectId", chofer.getObjectId());
        query.getFirstInBackground(callback);
    }

    public void getPedido(String id, GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("objectId", id);
        query.include("proveedorCarga");
        query.getFirstInBackground(callback);
    }
    public void tomarPedido(String id,final ParseObject chofer, final SaveCallback callback){
        getPedido(id, new GetCallback<ParseObject>() {
            @Override
            public void done(final ParseObject pedido, ParseException e) {
                ParseACL acl = new ParseACL(pedido.getParseObject("proveedorCarga").getParseUser("user"));
                acl.setReadAccess(ParseUser.getCurrentUser(),true);
                acl.setWriteAccess(ParseUser.getCurrentUser(),true);
                pedido.setACL(acl);
                pedido.put("horaAsignacion", Calendar.getInstance().getTime());
                pedido.put("estado", dataService.ACTIVO);
                pedido.put("chofer", chofer);
                pedido.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        ParseObject transportista = dataService.getTransportista();
                        transportista.put("estado", "EnViaje");
                        if (pedido.getBoolean("donacion") == false){
                            Number saldo = transportista.getNumber("saldo");
                            if (saldo == null) {
                                saldo = 0;
                            }
                            transportista.put("saldo", saldo.doubleValue() - pedido.getNumber("comision").doubleValue());
                        }
                        transportista.saveInBackground(callback);
                    }
                });

            }
        });
    }
    public void iniciarPedido(ParseObject pedido, SaveCallback callback){
        pedido.put("horaInicio", new Date());
        pedido.put("estado", dataService.EN_CURSO);
        pedido.saveInBackground(callback);
    }
    public void finalizarPedido(ParseObject pedido, final SaveCallback callback){
        pedido.put("horaFinalizacion", new Date());
        pedido.put("estado", dataService.FINALIZADO);
        pedido.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                ParseObject transportista = dataService.getTransportista();
                transportista.put("estado", "Disponible");
                transportista.saveInBackground(callback);
            }
        });
    }
}
