package com.easyruta.easyruta.viewcontroller;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.easyruta.easyruta.utils.DataService;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Arrays;

/**
 * Created by dcoellar on 10/10/15.
 */
public class ProfileActivity extends AppCompatActivity {

    private Activity activity;

    private DataService dataService;

    private ParseObject transportista;
    private EditText nombre;
    private EditText ci_ruc;
    private EditText telefono;
    private EditText placa;
    private EditText marca;
    private EditText modelo;
    private EditText anio;
    private EditText color;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);
        this.activity = this;
        this.dataService = ((EasyRutaApplication)getApplication()).getDataService();

        nombre = (EditText)findViewById(R.id.profile_name);
        ci_ruc = (EditText)findViewById(R.id.profile_ci_ruc);
        telefono = (EditText)findViewById(R.id.profile_phone);
        telefono.setRawInputType(Configuration.KEYBOARD_QWERTY);
        placa = (EditText)findViewById(R.id.profile_plate);
        marca = (EditText)findViewById(R.id.profile_brand);
        modelo = (EditText)findViewById(R.id.profile_model);
        anio = (EditText)findViewById(R.id.profile_year);
        anio.setRawInputType(Configuration.KEYBOARD_QWERTY);
        color = (EditText)findViewById(R.id.profile_color);
        findViewById(R.id.cerrar_session).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser.getCurrentUser().logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        dataService.setUser(null);
                        dataService.setTransportista(null);
                        dataService.setPedido(null);
                        dataService.setPedido(null);

                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                    }
                });
            }
        });
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfile();
            }
        });
        this.spinner = (Spinner) findViewById(R.id.profile_tipoCamion);

        loadProfile();

    }

    private void loadProfile(){
        transportista = ((EasyRutaApplication)getApplication()).getDataService().getTransportista();
        nombre.setText(transportista.getString("nombre"));
        ci_ruc.setText(transportista.getString("cedula"));
        telefono.setText(transportista.getString("telefono"));
        placa.setText(transportista.getString("placa"));
        marca.setText(transportista.getString("marca"));
        modelo.setText(transportista.getString("modelo"));
        if (transportista.getNumber("anio") != null) {
            anio.setText(String.valueOf(transportista.getNumber("anio")));
        }
        color.setText(transportista.getString("color"));
        spinner.setSelection(Arrays.asList(getResources().getStringArray(R.array.tipoCamion_values_array)).indexOf(transportista.getString("tipoCamion")));





        //TODO add combo box
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_profile_save) {
            saveProfile();
            return true;
        }

        if (id == android.R.id.home){
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveProfile(){
        transportista.put("nombre",nombre.getText().toString());
        transportista.put("cedula",ci_ruc.getText().toString());
        transportista.put("telefono", telefono.getText().toString());
        transportista.put("placa",placa.getText().toString());
        transportista.put("marca",marca.getText().toString());
        transportista.put("modelo", modelo.getText().toString());
        if (anio.getText().toString().length() > 0){
            transportista.put("anio", Integer.parseInt(anio.getText().toString()));
        }else{
            transportista.remove("anio");
        }
        transportista.put("color", color.getText().toString());
        transportista.put("tipoCamion", getResources().getStringArray(R.array.tipoCamion_values_array)[spinner.getSelectedItemPosition()]);


        transportista.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null){

                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }else{
                    Log.e("ERROR:",e.getMessage());
                    Toast toast = Toast.makeText(getBaseContext(), "Ooops!!! Estamos teniendo problemas con nuestros servidores, por favor intente mas tarde o contacte a soporte.", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }
}
