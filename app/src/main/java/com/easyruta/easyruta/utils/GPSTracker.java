package com.easyruta.easyruta.utils;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.easyruta.easyruta.EasyRutaApplication;
import com.easyruta.easyruta.R;
import com.parse.ParseObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Create this Class from tutorial :
 * http://www.androidhive.info/2012/07/android-gps-location-manager-tutorial
 *
 * For Geocoder read this : http://stackoverflow.com/questions/472313/android-reverse-geocoding-getfromlocation
 *
 */
public class GPSTracker extends Service implements LocationListener {

    private ParseObject pedido;
    private PubnubService pubnubService;

    // Get Class Name
    private static String TAG = GPSTracker.class.getName();

    private final Context mContext;

    // flag for GPS Status
    public boolean isGPSEnabled = false;

    // flag for network status
    public boolean isNetworkEnabled = false;

    // flag for GPS Tracking is enabled
    public boolean isGPSTrackingEnabled = false;

    Location location;
    double latitude;
    double longitude;

    // How many Geocoder should return our GPSTracker
    int geocoderMaxResults = 1;

    // The minimum distance to change updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 250; // 100 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 30 * 1; // 30 segundos

    // Declaring a Location Manager
    protected LocationManager locationManager;

    // Store LocationManager.GPS_PROVIDER or LocationManager.NETWORK_PROVIDER information
    private String provider_info;

    public GPSTracker(Context context, ParseObject pedido) {
        Log.d("TEST DANIEL","Create new tracker");
        this.pedido = pedido;
        this.mContext = context;
        this.pubnubService = ((EasyRutaApplication)context.getApplicationContext()).getPubnubService();
        getLocation();
    }

    /**
     * Try to get my current location by GPS or Network Provider
     */
    public void getLocation() {

        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            //getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Try to get location if you GPS Service is enabled
            if (isGPSEnabled) {
                this.isGPSTrackingEnabled = true;
                provider_info = LocationManager.GPS_PROVIDER;
                if (provider_info != null){
                    getLaskKnoenLocation();
                }
            }

            if (location == null && isNetworkEnabled) { // Try to get location if you Network Service is enabled
                this.isGPSTrackingEnabled = true;
                provider_info = LocationManager.NETWORK_PROVIDER;
                if (provider_info != null){
                    getLaskKnoenLocation();
                }
            }

            updateGPSCoordinates();
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            Log.e(TAG, "Impossible to connect to LocationManager", e);
        }
    }

    private void getLaskKnoenLocation() {
        try {
            locationManager.requestLocationUpdates(provider_info, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(provider_info);
            }
        }catch (SecurityException e){
            Log.e("ERROR","Security exception " + e.getMessage());
        }
    }

    /**
     * Update GPSTracker latitude and longitude
     */
    public void updateGPSCoordinates() {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    /**
     * GPSTracker latitude getter and setter
     * @return latitude
     */
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        return latitude;
    }

    /**
     * GPSTracker longitude getter and setter
     * @return
     */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        return longitude;
    }

    /**
     * GPSTracker isGPSTrackingEnabled getter.
     * Check GPS/wifi is enabled
     */
    public boolean getIsGPSTrackingEnabled() {
        return this.isGPSTrackingEnabled;
    }

    /**
     * Stop using GPS listener
     * Calling this method will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(GPSTracker.this);
            }catch (SecurityException e){
                Log.e("ERROR","Security exception " + e.getMessage());
            }
        }
    }

    /**
     * Function to show settings alert dialog
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        //Setting Dialog Title
        alertDialog.setTitle(R.string.GPSAlertDialogTitle);

        //Setting Dialog Message
        alertDialog.setMessage(R.string.GPSAlertDialogMessage);

        //On Pressing Setting button
        alertDialog.setPositiveButton(R.string.action_settings, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        //On pressing cancel button
        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    /**
     * Get list of address by latitude and longitude
     * @return null or List<Address>
     */
    public List<Address> getGeocoderAddress(Context context) {
        if (location != null) {

            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);

            try {
                /**
                 * Geocoder.getFromLocation - Returns an array of Addresses
                 * that are known to describe the area immediately surrounding the given latitude and longitude.
                 */
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, this.geocoderMaxResults);

                return addresses;
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Impossible to connect to Geocoder", e);
            }
        }

        return null;
    }

    /**
     * Try to get AddressLine
     * @return null or addressLine
     */
    public String getAddressLine(Context context) {
        List<Address> addresses = getGeocoderAddress(context);

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String addressLine = address.getAddressLine(0);

            return addressLine;
        } else {
            return null;
        }
    }

    /**
     * Try to get Locality
     * @return null or locality
     */
    public String getLocality(Context context) {
        List<Address> addresses = getGeocoderAddress(context);

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String locality = address.getLocality();

            return locality;
        }
        else {
            return null;
        }
    }

    /**
     * Try to get Postal Code
     * @return null or postalCode
     */
    public String getPostalCode(Context context) {
        List<Address> addresses = getGeocoderAddress(context);

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String postalCode = address.getPostalCode();

            return postalCode;
        } else {
            return null;
        }
    }

    /**
     * Try to get CountryName
     * @return null or postalCode
     */
    public String getCountryName(Context context) {
        List<Address> addresses = getGeocoderAddress(context);
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String countryName = address.getCountryName();

            return countryName;
        } else {
            return null;
        }
    }

    @Override
    public void onLocationChanged(final Location location) {
        Log.d("TEST DANIEL", "new location:" + location.getLatitude() + " - " + location.getLongitude());
        this.pedido.add("locations", String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude()));
        this.pedido.saveInBackground();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("TEST DANIEL","onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("TEST DANIEL","onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("TEST DANIEL","onProviderDisabled");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
