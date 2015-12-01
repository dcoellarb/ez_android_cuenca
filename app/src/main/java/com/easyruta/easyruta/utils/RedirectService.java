package com.easyruta.easyruta.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.viewcontroller.LoginActivity;
import com.easyruta.easyruta.viewcontroller.MainActivity;
import com.easyruta.easyruta.viewcontroller.PedidoActivoActivity;
import com.easyruta.easyruta.viewcontroller.PedidoPendienteActivity;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;

import java.io.Serializable;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by dcoellar on 11/27/15.
 */
public class RedirectService {

    public enum Activities {
        LOGIN, MAIN, PEDIDO_PENDIENTE, PEDIDO_ACTIVO
    }

    protected Activity activity;
    protected EasyRutaApplication application;
    protected DataService dataService;

    public void redirectByUser(EasyRutaApplication param_application,Activity param_activity){
        this.activity = param_activity;
        this.application = param_application;
        this.dataService = application.getDataService();

        rx.Observable observable = rx.Observable.concat(redirectCheckUserLogged(),redirectCheckUserTransportista(),redirectCheckUserCurrentPedido())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        Subscription subscription = observable.subscribe(new Observer<Serializable>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e("ERROR", e.getMessage());
            }

            @Override
            public void onNext(Serializable serializable) {
                if (serializable.toString().equalsIgnoreCase(Activities.LOGIN.toString())){
                    if (!activity.getClass().getName().equalsIgnoreCase("LoginActivity")){
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                    }
                }
                if (serializable.toString().equalsIgnoreCase(Activities.MAIN.toString())){
                    if (!activity.getClass().getName().equalsIgnoreCase("MainActivity")){
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                    }
                }
                if (serializable.toString().equalsIgnoreCase(Activities.PEDIDO_PENDIENTE.toString())){
                    if (!activity.getClass().getName().equalsIgnoreCase("PeidoPendienteActivity")){
                        Intent intent = new Intent(activity, PedidoPendienteActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                    }
                }
                if (serializable.toString().equalsIgnoreCase(Activities.PEDIDO_ACTIVO.toString())){
                    if (!activity.getClass().getName().equalsIgnoreCase("PedidoActivoActivity")){
                        Intent intent = new Intent(activity, PedidoActivoActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                    }
                }
            }
        });
    }

    private rx.Observable<String> redirectCheckUserLogged(){
        return rx.Observable.create(new rx.Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (application.getDataService().getUser() == null){
                    subscriber.onNext(Activities.LOGIN.toString());
                }else{
                    subscriber.onNext("");
                    subscriber.onCompleted();
                }
            }
        });
    }

    private rx.Observable<String> redirectCheckUserTransportista(){
        return rx.Observable.create(new rx.Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                if (application.getDataService().getTransportista() == null){
                    dataService.getUserTransportista(new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject object, ParseException e) {
                            if (e==null){
                                dataService.setTransportista(object);
                                if (object.getParseObject("proveedor") != null){
                                    dataService.setProveedor(object.getParseObject("proveedor"));
                                }
                                subscriber.onNext("");
                                subscriber.onCompleted();
                            }else{
                                Log.e("ERROR",e.getMessage());
                            }
                        }
                    });
                }else{
                    subscriber.onNext("");
                    subscriber.onCompleted();
                }
            }
        });
    }

    private rx.Observable<String> redirectCheckUserCurrentPedido(){
        return rx.Observable.create(new rx.Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                application.getDataService().getTransportistaCurrentPedido(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject object, ParseException e) {
                        String activity_name = Activities.MAIN.toString();
                        if (e == null){
                            if (object != null) {
                                SharedPreferences.Editor prefs = activity.getSharedPreferences("easyruta", activity.MODE_PRIVATE).edit();
                                prefs.putString("pedido", object.getObjectId().toString());
                                prefs.putString("estado", dataService.PENDIENTE_CONFIRMACION);
                                prefs.commit();

                                if (object.get("Estado").toString().equalsIgnoreCase(application.getDataService().PENDIENTE_CONFIRMACION)){
                                    activity_name = Activities.PEDIDO_PENDIENTE.toString();
                                }
                                if (object.get("Estado").toString().equalsIgnoreCase(application.getDataService().ACTIVO)
                                        || object.get("Estado").toString().equalsIgnoreCase(application.getDataService().EN_CURSO)){
                                    activity_name = Activities.PEDIDO_ACTIVO.toString();
                                }
                            }
                        } else {
                            Log.e("ERROR", e.getMessage());
                        }
                        subscriber.onNext(activity_name);
                        subscriber.onCompleted();
                    }
                });
            }
        });
    }

}
