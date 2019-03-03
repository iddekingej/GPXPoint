package org.elaya.gac;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;

public abstract class GPSStatusListener implements GPSStatusCallback {
    private LocationManager locationManager;

    GPSStatusListener(Context pContext, LocationManager pLocationManager){
        GpsStatus.Listener lListener=new GpsStatus.Listener(){

            public void onGpsStatusChanged(int pEvent){
                switch(pEvent){
                    case GpsStatus.GPS_EVENT_STARTED:
                        startFix();
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        gpsFixed();
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        try {
                            GpsStatus lStatus = locationManager.getGpsStatus(null);
                            numberOfSatellites(getFixedSatellites(lStatus.getSatellites()));
                        }catch(SecurityException e){
                            //Do nothing from now
                        }
                        break;
                    default:
                        //nothing
                }
            }

        };

        locationManager=pLocationManager;

        if(pContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            locationManager.addGpsStatusListener(lListener);
        }


    }




    /**
     * GPSStatusCallback returns a list of satellites. GetFixedSatellites counts
     * how many of those satellites are used in the fix.
     * This function is only used when the depreciated GpsStatus is used (<API 23)
     *
     * @param pSatellites  List of satellites used in this function
     * @return    number of satellites in pSatellites
     */

    private int getFixedSatellites(Iterable<GpsSatellite> pSatellites)
    {
        int lFixed=0;

        for(GpsSatellite lSatellite : pSatellites){
            if(lSatellite.usedInFix()){
                lFixed++;
            }
        }
        return lFixed;
    }

    public abstract void startFix() ;


    public abstract void gpsFixed() ;

    public abstract void numberOfSatellites(int pNumber) ;



}
