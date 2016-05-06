package com.easyruta.easyruta.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.utils.exceptions.EzException;
import com.easyruta.easyruta.utils.exceptions.EzExceptions;
import com.easyruta.easyruta.viewcontroller.LoginActivity;
import com.easyruta.easyruta.viewcontroller.MainActivity;
import com.easyruta.easyruta.viewcontroller.PedidoActivoActivity;
import com.parse.GetCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
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

    public void redirect(EasyRutaApplication param_application,Activity param_activity){
        this.activity = param_activity;
        this.application = param_application;
        this.dataService = application.getDataService();

        rx.Observable observable = rx.Observable.concat(redirectCheckUserLogged(),redirectCheckUserTransportista(),redirectCheckUserCurrentPedido())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        this.redirectCheckUserLogged()
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean v) {
                        return redirectCheckUserTransportista();
                    }
                }).flatMap(new Func1<Boolean, Observable<Boolean>>() {
                     @Override
                    public Observable<Boolean> call(Boolean v) {
                        return redirectCheckUserCurrentPedido();
                    }
                }).subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Log.d("TEST","finish");
                    }

                    @Override
                    public void onError(Throwable e) {
                        EzException ex = (EzException)e;
                        String errorText = application.getString(R.string.error_common);
                        if (ex != null){
                            switch (ex.getType()) {
                                case USER_IS_NOT_CHOFER:
                                    errorText = "Esta applicacion es unicamente para conductores, estamos trabajando en tu app, contactanos por mas informacion";
                                    break;
                                case USER_NOT_LOGGED:
                                    errorText = "";
                                    //if (!activity.getClass().getName().equalsIgnoreCase("LoginActivity")
                                    //        && !activity.getClass().getName().equalsIgnoreCase("LaunchActivity")) {
                                    //    errorText = "Su session a expirado por favor vuelva a ingresar";
                                    //}
                                    break;
                            }
                        }
                        redirectToLogin(errorText);
                    }

                    @Override
                    public void onNext(Boolean activo) {
                        if (activo) {
                            redirectToPedidoActivo();
                        } else {
                            redirectToMain();
                        }
                    }
                });
    }

    private rx.Observable<Boolean> redirectCheckUserLogged(){
        return rx.Observable.create(new rx.Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if (application.getDataService().getUser() == null){
                    subscriber.onError(new EzException(EzExceptions.USER_NOT_LOGGED));
                }else{
                    subscriber.onNext(true);
                }
            }
        });
    }

    private rx.Observable<Boolean> redirectCheckUserTransportista(){
        return rx.Observable.create(new rx.Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                if (application.getDataService().getTransportista() == null){
                    dataService.getUserTransportista(new GetCallback<ParseObject>() {
                        @Override
                        public void done(ParseObject object, ParseException e) {
                            if (e != null) {
                                Log.e("ERROR",e.getMessage());
                                if (e.getCode() == 101) {
                                    subscriber.onError(new EzException(EzExceptions.USER_IS_NOT_CHOFER));
                                } else {
                                    subscriber.onError(e);
                                }
                            } else {
                                dataService.setTransportista(object);
                                dataService.setProveedor(object.getParseObject("transportista"));
                                subscriber.onNext(true);
                            }
                        }
                    });
                } else {
                    subscriber.onNext(true);
                }
            }
        });
    }

    private rx.Observable<Boolean> redirectCheckUserCurrentPedido(){
        return rx.Observable.create(new rx.Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                application.getDataService().getTransportistaCurrentPedido(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject object, ParseException e) {
                        if (e != null){
                            Log.e("ERROR",e.getMessage());
                            if (e.getCode() == 101) {
                                subscriber.onNext(false);
                            } else {
                                subscriber.onError(e);
                            }
                        } else {
                            dataService.setPedido(object);
                            if (object != null){
                                subscriber.onNext(true);
                            } else {
                                subscriber.onNext(false);
                            }
                            subscriber.onCompleted();
                        }
                    }
                });
            }
        });
    }

    private void redirectToLogin(final String errorMessage) {
        ParseUser.getCurrentUser().logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                if (!activity.getClass().getName().equalsIgnoreCase("LoginActivity")){
                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    Bundle b = new Bundle();
                    b.putString("error", errorMessage);
                    intent.putExtras(b);
                    activity.startActivity(intent);
                } else {
                    ((LoginActivity)activity).showError(errorMessage);
                }
            }
        });
    }

    private void redirectToMain() {
        if (!activity.getClass().getName().equalsIgnoreCase("MainActivity")){
            Intent intent = new Intent(activity, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }

    private void redirectToPedidoActivo() {
        if (!activity.getClass().getName().equalsIgnoreCase("PedidoActivoActivity")){
            Intent intent = new Intent(activity, PedidoActivoActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        }
    }

}
