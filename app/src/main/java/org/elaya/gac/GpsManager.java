package org.elaya.gac;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import static org.elaya.gac.GPSInfo.ST_OK;
import static org.elaya.gac.GPSInfo.ST_TEMP_UNAVAILABLE;
import static org.elaya.gac.GPSInfo.ST_UNAVAILABLE;

abstract class GpsManager implements LocationListener {

    private LocationManager locationManager;
    private BroadcastReceiver gpsStatus;
    private Activity activity;

    protected abstract void setStatus(int pStatus);


    GpsManager(Activity pActivity)
    {
        activity=pActivity;
        locationManager =  (LocationManager) pActivity.getSystemService(Context.LOCATION_SERVICE);
        setupProviderChangeListener();
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    private void setupProviderChangeListener(){

        gpsStatus = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String lAction=intent.getAction();
                if(lAction != null) {
                    if (lAction.matches("android.location.PROVIDERS_CHANGED")) {
                        Log.d("GPS Debug", "Boardcast");
                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            setStatus(ST_OK);
                        } else {
                            setStatus(GPSInfo.ST_DISABLED);
                        }
                    }
                }
            }
        };

    }


    public void enableStatusListener()
    {
        activity.registerReceiver(gpsStatus, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    public void disableStatusListener()
    {
        activity.unregisterReceiver(gpsStatus);
    }


    public abstract void onLocationChanged(Location location);



    @Override
    public void onProviderDisabled(String pProvider) {
        if(LocationManager.GPS_PROVIDER.equals(pProvider)) {
            setStatus(GPSInfo.ST_DISABLED);
        }
    }

    @Override
    public void onProviderEnabled(String pProvider) {
        if(LocationManager.GPS_PROVIDER.equals(pProvider)) {
            setStatus(ST_OK);
        }
    }

    @Override
    public void onStatusChanged(String pProvider, int pStatus, Bundle pExtras) {
        if(LocationManager.GPS_PROVIDER.equals(pProvider)) {
            switch(pStatus){

                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    setStatus(ST_TEMP_UNAVAILABLE);
                    break;

                case LocationProvider.OUT_OF_SERVICE:
                    setStatus(ST_UNAVAILABLE);
                    break;

                case LocationProvider.AVAILABLE:
                    setStatus(ST_OK);
                    break;
                default:
                    //do nothing
            }
        }
    }

    public void disableUpdates()
    {
        locationManager.removeUpdates(this);
    }

    public boolean enableUpdates()
    {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            return false;
        }catch(SecurityException e){
            return true;
        }

    }
}
