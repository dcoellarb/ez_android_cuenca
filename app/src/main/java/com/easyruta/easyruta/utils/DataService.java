package com.easyruta.easyruta.utils;

import android.util.Log;

import com.easyruta.easyruta.Constants;
import com.easyruta.easyruta.EasyRutaApplication;
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
import java.util.Date;
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
    public static String FINALIZADO = "Finalizado";
    public static String CANCELADO = "Cancelado";
    public static String CANCELADO_CLIENTE = "CanceladoCliente";

    //Variables
    private DataService dataService;
    private ParseUser user;
    private ParseObject transportista;
    private ParseObject proveedor;

    public DataService(EasyRutaApplication application){

        dataService = this;

        Parse.setLogLevel(Log.VERBOSE);
        Parse.enableLocalDatastore(application);
        Parse.initialize(application, Constants.PARSE_APPLICATION_ID, Constants.PARSE_CLIENT_KEY);
        //ParseInstallation.getCurrentInstallation().saveInBackground();

    }

    //Getter and Setters
    public ParseUser getUser() {
        if (user == null){
            user = ParseUser.getCurrentUser();
        }
        return user;
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

    //Public Methods
    public void getUserTransportista(GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Transportista");
        query.include("proveedor");
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
        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(pendienteQuery);
        queries.add(activoQuery);
        queries.add(encursoQuery);

        ParseQuery<ParseObject> query = ParseQuery.or(queries);
        query.getFirstInBackground(callback);
    }
    public void login(String user, String password, LogInCallback callback){
        ParseUser.logInInBackground(user, password, callback);
    }
    public void getPedidoPendientes(ParseObject transportista,FindCallback<ParseObject> callback){
        if (transportista.getString("TipoTransporte").equalsIgnoreCase("furgon") || transportista.getString("TipoTransporte").equalsIgnoreCase("plataforma")){
            //Or for furgon or plataformar
            ParseQuery<ParseObject> queryTipo = ParseQuery.getQuery("Pedido");
            queryTipo.whereEqualTo("TipoTransporte", transportista.getString("TipoTransporte"));
            ParseQuery<ParseObject> queryTipo1 = ParseQuery.getQuery("Pedido");
            queryTipo1.whereEqualTo("TipoTransporte", "furgon_plataforma");

            //Create list for or
            List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
            queries.add(queryTipo);
            queries.add(queryTipo1);

            //Combine query
            ParseQuery<ParseObject> query = ParseQuery.or(queries);
            //Regular query filters
            query.whereEqualTo("Estado", dataService.PENDIENTE);
            query.whereNotEqualTo("TransportistasBloqueados", transportista.getObjectId());
            query.whereNotEqualTo("TransportistasCancelados", transportista.getObjectId());
            //Run query
            query.orderByDescending("createdAt");
            Log.d("TEST","getting or");
            query.findInBackground(callback);
        }else{
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
            //Regular query filters
            query.whereEqualTo("Estado", dataService.PENDIENTE);
            //query.whereLessThanOrEqualTo("Comision", transportista.get("Saldo"));
            query.whereNotEqualTo("TransportistasBloqueados", transportista.getObjectId());
            query.whereNotEqualTo("TransportistasCancelados", transportista.getObjectId());
            //Filter type
            query.whereEqualTo("TipoTransporte", transportista.getString("TipoTransporte"));
            //Run query
            query.orderByDescending("createdAt");
            Log.d("TEST", "getting normal");
            query.findInBackground(callback);
        }
    }
    public void getTransportistaSaldo(ParseObject transportista,GetCallback<ParseObject> callback){
        ParseQuery query = new ParseQuery("Transportista");
        query.whereEqualTo("objectId", transportista.getObjectId());
        query.getFirstInBackground(callback);
    }

    public void getPedido(String id, GetCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Pedido");
        query.whereEqualTo("objectId", id);
        query.include("empresa");
        query.getFirstInBackground(callback);
    }
    public void cancelarPedido(final ParseObject pedido, final String estado, final SaveCallback callback){

        if (estado == dataService.PENDIENTE){
            ParseACL acl = new ParseACL(pedido.getParseObject("empresa").getParseUser("user"));
            acl.setRoleReadAccess("transportistaIndependiente", true);
            acl.setRoleWriteAccess("transportistaIndependiente", true);
            acl.setRoleReadAccess("transportista", true);
            acl.setRoleWriteAccess("transportista",true);
            acl.setRoleReadAccess("proveedor", true);
            acl.setRoleWriteAccess("proveedor",true);
            pedido.setACL(acl);
        }

        pedido.put("HoraSeleccion", Calendar.getInstance().getTime());
        pedido.put("Estado", estado);
        pedido.put("Transportista", transportista);
        pedido.saveInBackground(callback);

    }
    public void tomarPedido(String id,final ParseObject transportista, final SaveCallback callback){
        getPedido(id, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject pedido, ParseException e) {

                ParseACL acl = new ParseACL(pedido.getParseObject("empresa").getParseUser("user"));
                acl.setReadAccess(ParseUser.getCurrentUser(),true);
                acl.setWriteAccess(ParseUser.getCurrentUser(),true);
                pedido.setACL(acl);
                pedido.put("HoraSeleccion", Calendar.getInstance().getTime());
                pedido.put("Estado", dataService.PENDIENTE_CONFIRMACION);
                pedido.put("Transportista", transportista);
                pedido.saveInBackground(callback);
            }
        });
    }
    public void iniciarPedido(ParseObject pedido, SaveCallback callback){
        pedido.put("HoraInicio", new Date());
        pedido.put("Estado", dataService.EN_CURSO);
        pedido.saveInBackground(callback);
    }
    public void finalizarPedido(ParseObject pedido, SaveCallback callback){
        pedido.put("HoraFinalizacion", new Date());
        pedido.put("Estado", dataService.FINALIZADO);
        pedido.saveInBackground(callback);
    }
}
