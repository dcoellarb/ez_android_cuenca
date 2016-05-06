package com.easyruta.easyruta.viewmodel;

import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.utils.DataService;
import com.easyruta.easyruta.utils.RedirectService;
import com.easyruta.easyruta.viewcontroller.LoginActivity;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

/**
 * Created by dcoellar on 11/30/15.
 */
public class LoginViewModel {

    EasyRutaApplication application;
    LoginActivity activity;
    RedirectService redirectService;
    DataService dataService;

    public LoginViewModel(LoginActivity activity){

        this.activity = activity;
        this.application = (EasyRutaApplication) activity.getApplication();
        this.redirectService = this.application.getRedirectService();
        this.dataService = this.application.getDataService();

    }

    public void Login(String login, String password){
        dataService.login(login, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (e == null){
                    if (user != null) {
                        redirectService.redirect(application, activity);
                    } else {
                        Log.e("ERROR", "Error singing up user is null");
                        activity.showError("Usuario y/o clave incorrectos.");
                    }
                } else {
                    Log.e("ERROR", "Error singing up:" + e.getMessage());
                    if (e.getCode() == 101){
                        activity.showError("Usuario y/o clave incorrectos.");
                    } else {
                        activity.showError(activity.getString(R.string.error_common));
                    }
                }
            }
        });
    }
}
