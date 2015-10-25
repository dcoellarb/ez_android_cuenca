package com.easyruta.easyruta;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.parse.GetCallback;
import com.parse.LogOutCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pubnub.api.Pubnub;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Created by dcoellar on 9/21/15.
 */
public class EasyRutaApplication extends Application  {

    private ParseUser user;
    private ParseObject transportista;
    private Pubnub pubnub;
    Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        application = this;

        Parse.enableLocalDatastore(this);
        Parse.initialize(this, Constants.PARSE_APPLICATION_ID, Constants.PARSE_CLIENT_KEY);

        pubnub = new Pubnub(Constants.PUBNUB_PUB_KEY, Constants.PUBNUB_SUB_KEY);
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
                        Intent intent = new Intent(this, PedidoPendiente.class);
                        intent.putExtra(PedidoPendiente.PARAM_ID, prefs.getString("pedido", ""));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(this, PedidoActivo.class);
                        intent.putExtra(PedidoPendiente.PARAM_ID, prefs.getString("pedido", ""));
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

    public Pubnub getPubnub() {
        return pubnub;
    }
}
