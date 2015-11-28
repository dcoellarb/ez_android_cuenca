package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

/**
 * Created by dcoellar on 9/21/15.
 */
public class LoginActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = this;

        setContentView(R.layout.activity_login);

        final EditText login = (EditText)findViewById(R.id.login_user);
        final EditText password = (EditText)findViewById(R.id.login_password);
        LinearLayout btnLogin = (LinearLayout)findViewById(R.id.login_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.logInInBackground(login.getText().toString(), password.getText().toString(), new LogInCallback() {
                    public void done(ParseUser user, ParseException e) {
                        if (user != null) {
                            ((EasyRutaApplication) getApplication()).getRedirectService().redirectByUser(((EasyRutaApplication) getApplication()),activity);
                        } else {
                            Log.e("ERROR", "Error singing up:" + e.getMessage());
                        }
                    }
                });
            }
        });
    }
}
