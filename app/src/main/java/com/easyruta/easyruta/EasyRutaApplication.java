package com.easyruta.easyruta;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.easyruta.easyruta.utils.DataService;
import com.easyruta.easyruta.utils.PubnubService;
import com.easyruta.easyruta.utils.RedirectService;

import io.fabric.sdk.android.Fabric;

/**
 * Created by dcoellar on 9/21/15.
 */
public class EasyRutaApplication extends Application  {

    private Application application;
    private DataService dataService;
    private PubnubService pubnubService;
    private RedirectService redirectService;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        application = this;
        dataService = new DataService(this);
        pubnubService = new PubnubService();

    }

    public DataService getDataService() {
        return dataService;
    }

    public PubnubService getPubnubService() {
        return pubnubService;
    }

    public RedirectService getRedirectService() { return redirectService; }


}
