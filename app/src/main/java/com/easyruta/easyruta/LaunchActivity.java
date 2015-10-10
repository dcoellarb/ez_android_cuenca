package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.parse.ParseUser;

/**
 * Created by dcoellar on 10/10/15.
 */
public class LaunchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launch);
    }

    @Override
    protected void onStart(){
        super.onStart();
        if (ParseUser.getCurrentUser() == null){
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }else{
            if (((EasyRutaApplication) getApplication()).getUser() == null){
                ((EasyRutaApplication) getApplication()).setUser(ParseUser.getCurrentUser());
            }else{
                ((EasyRutaApplication) getApplication()).afterSetUser();
            }
        }
    }
}
