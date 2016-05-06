package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.os.Bundle;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;

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

        ((EasyRutaApplication)getApplication()).getRedirectService().redirect(((EasyRutaApplication)getApplication()),this);
    }

}
