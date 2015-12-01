package com.easyruta.easyruta.viewmodel;

import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
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
                        redirectService.redirectByUser(application, activity);
                    } else {
                        Log.e("ERROR", "Error singing up user is null");
                    }
                } else {
                    Log.e("ERROR", "Error singing up:" + e.getMessage());
                }
            }
        });
    }
}
