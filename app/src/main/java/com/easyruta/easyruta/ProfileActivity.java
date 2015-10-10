package com.easyruta.easyruta;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by dcoellar on 10/10/15.
 */
public class ProfileActivity extends AppCompatActivity {

    private Activity activity;
    private ImageView photo;
    private Bitmap imageBitmap;
    private ParseObject transportista;
    private EditText nombre;
    private EditText ci_ruc;
    private EditText telefono;
    private EditText placa;
    private EditText marca;
    private EditText modelo;
    private EditText anio;
    private EditText color;
    private EditText cubicaje;
    private EditText extension;
    private Switch refrigeracion;

    LinearLayout currentTipo;
    TextView currentTipoText;
    ImageView currentTipoImage;
    String tipo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);
        activity = this;

        //ParseObject transportista = new ParseObject("Transportista");
        //transportista.put("photo",);

        photo = (ImageView)findViewById(R.id.profile_photo);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

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
        cubicaje = (EditText)findViewById(R.id.profile_cubicaje);
        cubicaje.setRawInputType(Configuration.KEYBOARD_QWERTY);
        extension = (EditText)findViewById(R.id.profile_extension);
        extension.setRawInputType(Configuration.KEYBOARD_QWERTY);
        refrigeracion = (Switch)findViewById(R.id.profile_refrigerado);

        findViewById(R.id.profile_tipo_furgon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipo = "furgon";
                setTipo(view);
            }
        });
        findViewById(R.id.profile_tipo_plataforma).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipo = "plataforma";
                setTipo(view);
            }
        });
        findViewById(R.id.profile_tipo_cama_baja).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipo = "cama baja";
                setTipo(view);
            }
        });
        findViewById(R.id.profile_tipo_banera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipo = "banera";
                setTipo(view);
            }
        });
        findViewById(R.id.profile_tipo_tanquero).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipo = "tanquero";
                setTipo(view);
            }
        });
        findViewById(R.id.profile_tipo_ninera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipo = "ninera";
                setTipo(view);
            }
        });

        loadProfile();

    }

    private void setTipo(View view){
        if (currentTipo != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                currentTipo.setBackground(getDrawable(R.drawable.tipo_background));
            }else{
                currentTipo.setBackgroundResource(R.drawable.tipo_background);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                currentTipoText.setTextColor(getColor(R.color.white));
            }else{
                currentTipoText.setTextColor(getResources().getColor(R.color.white));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                currentTipoImage.setImageDrawable(getDrawable(getTipoResource(currentTipoText.getText().toString(),true)));
            }else{
                currentTipoImage.setImageDrawable(getResources().getDrawable(getTipoResource(currentTipoText.getText().toString(),true)));
            }

            if (currentTipoText.getText().toString().equalsIgnoreCase("furgon")){
                findViewById(R.id.profile_furgon_container).setVisibility(View.GONE);
            }
            if (currentTipoText.getText().toString().equalsIgnoreCase("plataforma")){
                findViewById(R.id.profile_plataforma_container).setVisibility(View.GONE);
            }
        }

        currentTipo = (LinearLayout)view;
        currentTipoImage = (ImageView)((ViewGroup)view).getChildAt(0);
        currentTipoText = (TextView)((ViewGroup)view).getChildAt(1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            currentTipo.setBackground(getDrawable(R.drawable.tipo_background_selected));
        }else{
            currentTipo.setBackgroundResource(R.drawable.tipo_background_selected);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            currentTipoText.setTextColor(getColor(R.color.yellow));
        }else{
            currentTipoText.setTextColor(getResources().getColor(R.color.orange));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            currentTipoImage.setImageDrawable(getDrawable(getTipoResource(currentTipoText.getText().toString(),false)));
        }else{
            currentTipoImage.setImageDrawable(getResources().getDrawable(getTipoResource(currentTipoText.getText().toString(), false)));
        }

        if (currentTipoText.getText().toString().equalsIgnoreCase("furgon")){
            findViewById(R.id.profile_furgon_container).setVisibility(View.VISIBLE);
        }
        if (currentTipoText.getText().toString().equalsIgnoreCase("plataforma")){
            findViewById(R.id.profile_plataforma_container).setVisibility(View.VISIBLE);
        }
    }

    private int getTipoResource(String tipo,boolean white){
        if (tipo.equalsIgnoreCase("furgon")){
            if (white){ return R.drawable.tipo_furgon_white; }else{ return R.drawable.tipo_furgon_yellow; }
        }
        if (tipo.equalsIgnoreCase("plataforma")){
            if (white){ return R.drawable.tipo_plataforma_white; }else{ return R.drawable.tipo_plataforma_yellow; }
        }
        if (tipo.equalsIgnoreCase("cama baja")){
            if (white){ return R.drawable.tipo_furgon_white; }else{ return R.drawable.tipo_furgon_yellow; }
        }
        if (tipo.equalsIgnoreCase("banera")){
            if (white){ return R.drawable.tipo_banera_white; }else{ return R.drawable.tipo_banera_yellow; }
        }
        if (tipo.equalsIgnoreCase("tanquero")){
            if (white){ return R.drawable.tipo_tanquero_white; }else{ return R.drawable.tipo_tanquero_yellow; }
        }
        if (tipo.equalsIgnoreCase("ninera")){
            if (white){ return R.drawable.tipo_jaula_white; }else{ return R.drawable.tipo_jaula_yellow; }
        }
        return 0;
    }

    private void loadProfile(){
        transportista = ((EasyRutaApplication)getApplication()).getTransportista();
        nombre.setText(transportista.getString("Nombre"));
        ci_ruc.setText(transportista.getString("Cedula"));
        telefono.setText(transportista.getString("Telefono"));
        placa.setText(transportista.getString("Placa"));
        marca.setText(transportista.getString("Marca"));
        modelo.setText(transportista.getString("Modelo"));
        anio.setText(String.valueOf(transportista.getNumber("Anio")));
        color.setText(transportista.getString("Color"));
        NumberFormat formatter = new DecimalFormat("#0.00");
        if (transportista.getNumber("CubicajeMinimo") != null){
            cubicaje.setText(formatter.format(transportista.getNumber("CubicajeMinimo")));
        }
        if (transportista.getNumber("ExtensionMinima") != null) {
            extension.setText(formatter.format(transportista.getNumber("ExtensionMinima")));
        }
        refrigeracion.setChecked(transportista.getBoolean("Refrigerado"));
        tipo = transportista.getString("TipoTransporte");
        if (tipo != null){
            if (tipo.equalsIgnoreCase("furgon")){
                setTipo(findViewById(R.id.profile_tipo_furgon));
            }else if (tipo.equalsIgnoreCase("plataforma")){
                setTipo(findViewById(R.id.profile_tipo_plataforma));
            }else if (tipo.equalsIgnoreCase("cama baja")){
                setTipo(findViewById(R.id.profile_tipo_cama_baja));
            }else if (tipo.equalsIgnoreCase("banera")){
                setTipo(findViewById(R.id.profile_tipo_banera));
            }else if (tipo.equalsIgnoreCase("tanquero")){
                setTipo(findViewById(R.id.profile_tipo_tanquero));
            }else if (tipo.equalsIgnoreCase("ninera")){
                setTipo(findViewById(R.id.profile_tipo_ninera));
            }
        }else{
            setTipo(findViewById(R.id.profile_tipo_furgon));
        }

        ParseFile file = transportista.getParseFile("photo");
        Picasso.with(this).load(file.getUrl()).into(photo);
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

            if (imageBitmap != null){
                saveFile();
            }else{
                saveProfile(null);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveFile(){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] data = stream.toByteArray();

        final ParseFile file = new ParseFile("transportista_image_" + transportista.getObjectId(), data);
        file.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null){
                    saveProfile(file);
                }else{
                    Log.e("ERROR:",e.getMessage());
                    //TODO -  Let the user know
                }
            }
        });
    }

    private void saveProfile(ParseFile file){
        transportista.put("Nombre",nombre.getText().toString());
        transportista.put("Cedula",ci_ruc.getText().toString());
        transportista.put("Telefono", telefono.getText().toString());
        transportista.put("Placa",placa.getText().toString());
        transportista.put("Marca",marca.getText().toString());
        transportista.put("Modelo", modelo.getText().toString());
        if (anio.getText().toString().length() > 0){
            transportista.put("Anio", Integer.parseInt(anio.getText().toString()));
        }else{
            transportista.remove("Anio");
        }
        transportista.put("Color", color.getText().toString());
        if (file!=null){
            transportista.put("photo", file);
        }
        transportista.put("TipoTransporte", tipo);
        if (cubicaje.getText().toString().length() > 0){
            transportista.put("CubicajeMinimo", Double.parseDouble(cubicaje.getText().toString()));
        }else{
            transportista.remove("CubicajeMinimo");
        }
        if (extension.getText().toString().length() > 0){
            transportista.put("ExtensionMinima", Double.parseDouble(extension.getText().toString()));
        }else{
            transportista.remove("ExtensionMinima");
        }
        transportista.put("Refrigerado", refrigeracion.isChecked());

        transportista.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null){

                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }else{
                    Log.e("ERROR:",e.getMessage());
                    //TODO -  Let the user know
                }
            }
        });
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            photo.setImageBitmap(imageBitmap);
        }
    }
}
