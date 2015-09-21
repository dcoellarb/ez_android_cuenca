package com.easyruta.easyruta;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;

/**
 * Created by dcoellar on 9/21/15.
 */
public class EasyRutaApplication extends Application  {

    private ParseUser user;
    private ParseObject transportista;

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(this, "LRW3NBrk3JYLeAkXrpTF2TV0bDPn5HQTndrao8my", "r0lEQ4CUuYsOUQcqyRQXGjScxXn1Bbq3V6OfA3ly");

        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        // Optionally enable public read access.
        // defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
    }

    public ParseUser getUser() {
        return user;
    }

    public void setUser(ParseUser user) {
        this.user = user;
    }

    public ParseObject getTransportista() {
        return transportista;
    }

    public void setTransportista(ParseObject transportista) {
        this.transportista = transportista;
    }
}
