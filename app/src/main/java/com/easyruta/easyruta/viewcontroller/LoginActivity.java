package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.easyruta.easyruta.R;
import com.easyruta.easyruta.viewmodel.LoginViewModel;

/**
 * Created by dcoellar on 9/21/15.
 */
public class LoginActivity extends Activity{

    LoginViewModel viewModel;

    EditText login;
    EditText password;
    LinearLayout btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = this;
        setContentView(R.layout.activity_login);
        viewModel = new LoginViewModel(this);

        login = (EditText)findViewById(R.id.login_user);
        password = (EditText)findViewById(R.id.login_password);
        btnLogin = (LinearLayout)findViewById(R.id.login_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.Login(login.getText().toString(), password.getText().toString());
            }
        });
    }
}
