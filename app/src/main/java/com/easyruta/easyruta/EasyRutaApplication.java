package com.easyruta.easyruta;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pubnub.api.Pubnub;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dcoellar on 9/21/15.
 */
public class EasyRutaApplication extends Application  {

    private ParseUser user;
    private ParseObject transportista;
    private Pubnub pubnub;

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "LRW3NBrk3JYLeAkXrpTF2TV0bDPn5HQTndrao8my", "r0lEQ4CUuYsOUQcqyRQXGjScxXn1Bbq3V6OfA3ly");

        pubnub = new Pubnub("pub-c-ecec5777-242f-4a3e-8689-9b272441bb11", "sub-c-5327f6bc-60c6-11e5-b0b1-0619f8945a4f");
    }

    public ParseUser getUser() {
        return user;
    }

    public void setUser(ParseUser user) {
        this.user = user;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Transportista");
        query.whereEqualTo("user", user);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject transportista, ParseException e) {
                if (e == null && transportista != null) {
                    setTransportista(transportista);

                    ParseQuery<ParseObject> pendienteQuery = ParseQuery.getQuery("Pedido");
                    pendienteQuery.whereEqualTo("Estado", getString(R.string.status_parse_pendiente_confirmacion));
                    ParseQuery<ParseObject> activoQuery = ParseQuery.getQuery("Pedido");
                    activoQuery.whereEqualTo("Estado", getString(R.string.status_parse_activo));
                    ParseQuery<ParseObject> encursoQuery = ParseQuery.getQuery("Pedido");
                    encursoQuery.whereEqualTo("Estado", getString(R.string.status_parse_encurso));
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
                        Log.e("ERROR", "Transportista not found.");
                    }
                }
            }
        });
    }

    public void afterSetUser() {
        SharedPreferences prefs = this.getSharedPreferences("easyruta", MODE_PRIVATE);
        if (prefs.contains("pedido")){
            if (prefs.getString("estado","").equalsIgnoreCase(getString(R.string.status_parse_pendiente_confirmacion))){
                Intent intent = new Intent(this, PedidoPendiente.class);
                intent.putExtra(PedidoPendiente.PARAM_ID, prefs.getString("pedido",""));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }else{
                Intent intent = new Intent(this, PedidoActivo.class);
                intent.putExtra(PedidoPendiente.PARAM_ID, prefs.getString("pedido",""));
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

    public Pubnub getPubnub() {
        return pubnub;
    }
}
