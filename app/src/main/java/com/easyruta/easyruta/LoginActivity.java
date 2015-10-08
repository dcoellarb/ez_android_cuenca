package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

/**
 * Created by dcoellar on 9/21/15.
 */
public class LoginActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = this;

        ParseUser user = ((EasyRutaApplication)getApplication()).getUser();
        ParseObject transportista = ((EasyRutaApplication)getApplication()).getTransportista();
        if (user != null && user.getUsername() != null) {
            if (transportista != null && transportista.getObjectId() != null) {
                Intent intent = new Intent(activity, MainActivity.class);
                startActivity(intent);
            }else {
                SetTransportisa(user);
            }
        }

        setContentView(R.layout.activity_login);

        final EditText login = (EditText)findViewById(R.id.login_user);
        final EditText password = (EditText)findViewById(R.id.login_password);
        Button btnLogin = (Button)findViewById(R.id.login_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.logInInBackground(login.getText().toString(), password.getText().toString(), new LogInCallback() {
                    public void done(ParseUser user, ParseException e) {
                        if (user != null) {
                            Log.d("ERROR", "User found:" + user.getObjectId());
                            ((EasyRutaApplication) getApplication()).setUser(user);
                            SetTransportisa(user);
                        } else {
                            Log.e("ERROR", "Error singing up:" + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    private void SetTransportisa(ParseUser user){
        final Activity activity = this;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Transportista");
        query.whereEqualTo("user", user);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> transportista, ParseException e) {
                if (e == null && transportista.size() > 0) {
                    Log.d("ERROR", "Transportista found found:" + transportista.get(0).getObjectId());
                    ((EasyRutaApplication)getApplication()).setTransportista(transportista.get(0));
                    Intent intent = new Intent(activity, MainActivity.class);
                    startActivity(intent);
                } else {
                    Log.e("ERROR", "Error: " + e.getMessage());
                }
            }
        });
    }
}
