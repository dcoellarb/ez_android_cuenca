package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.easyruta.easyruta.Constants;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.viewmodel.LoginViewModel;

/**
 * Created by dcoellar on 9/21/15.
 */
public class LoginActivity extends Activity{

    LoginViewModel viewModel;

    EditText login;
    EditText password;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = this;
        Bundle b = getIntent().getExtras();
        if (b != null){
            String error = b.getString("error");
            if (error != null) {
                showError(error);
            }
        }

        setContentView(R.layout.activity_login);
        viewModel = new LoginViewModel(this);

        login = (EditText)findViewById(R.id.login_user);
        password = (EditText)findViewById(R.id.login_password);
        btnLogin = (Button)findViewById(R.id.login_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.Login(login.getText().toString(), password.getText().toString());
            }
        });

        findViewById(R.id.easyruta_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.EZ_URL));
                startActivity(browserIntent);
            }
        });
    }

    public void showError(String error){
        if (!error.isEmpty()){
            Toast toast = Toast.makeText(this, error, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
}
