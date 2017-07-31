package org.elaya.gpxpoint;

import android.app.Activity;
import android.content.Context;

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class GPXInfo extends Activity implements LocationListener {

    private TextView valueLon;
    private TextView valueLat;
    private TextView valueAltitude;
    private TextView valueSpeed;
    private TextView valueAccuracy;
    private TextView valueNumSatellites;
    private Switch displayGPS;
    private LinearLayout gpsData;
    private TextView warningText;
    private RadioButton unitMeter;
    private SharedPreferences settings;

    private static final double METER_TO_FOOT = 3.2808399;
    private static final double METER_TO_MILE = 1609.344;
    private static final double METER_TO_KM   = 3600;
    private static final String PREF_MAIN     = "main";
    private static final String PREF_S_UNIT_METER = "unitmeter";

    private LocationManager locationManager;

    /**
     * Setup window/activity
     * @param savedInstanceState Saved state , not used
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpxinfo);
        valueLon = (TextView) findViewById(R.id.valueLon);
        valueLat = (TextView) findViewById(R.id.valueLat);
        valueAltitude = (TextView) findViewById(R.id.valueAltitude);
        valueSpeed = (TextView) findViewById(R.id.valueSpeed);
        valueAccuracy = (TextView) findViewById(R.id.valueAccuracy);
        gpsData = (LinearLayout) findViewById(R.id.gpsData);
        valueNumSatellites = (TextView) findViewById(R.id.valueNumSatellites);
        warningText = (TextView) findViewById(R.id.warningText);
        displayGPS = (Switch) findViewById(R.id.displayGPS);
        unitMeter  = (RadioButton) findViewById(R.id.unitMeter);
        RadioButton unitFoot   = (RadioButton) findViewById(R.id.unitFoot);
        gpsData.setVisibility(View.GONE);

        settings = getSharedPreferences(PREF_MAIN, 0);

        boolean lUnitMeter = settings.getBoolean(PREF_S_UNIT_METER, true);

        if(lUnitMeter) {
            unitMeter.toggle();
        } else {
            unitFoot.toggle();
        }

        hideWarning();

        displayGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                GPXInfo.this.toggleDisplayGPS();
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void latestLocation()
    {
        Location lLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lLocation != null) {
            setLocation(lLocation);
        }
    }

    private void startGPS()
    {
        gpsData.setVisibility(View.VISIBLE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            latestLocation();
        } catch (Exception e) {
            Toast lToast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            lToast.show();
        }
    }

    public void toggleUnits(View pRadio)
    {
        SharedPreferences.Editor lEditor=settings.edit();
        lEditor.putBoolean(PREF_S_UNIT_METER,unitMeter.isChecked());
        lEditor.apply();
        latestLocation();
    }

    /**
     * When "displayGPS" is toggled on or of this method is called
     * when displaying GPS is turned of, the GPS information is hidden en LocationUpdates are stopped
     * when turned on the LocationListener is turned on and receives location updates
     */
    private void toggleDisplayGPS() {
        if (displayGPS.isChecked()) {
            startGPS();
        } else {
            gpsData.setVisibility(View.GONE);
            locationManager.removeUpdates(this);
        }
    }

    /**
     * Displays a warning message on top of the screen (with red background)
     *
     * @param pWarning Warning message to display
     */
    private void displayWarning(int pWarning)
    {
        warningText.setText(getResources().getString(pWarning));
        warningText.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the warning messages
     */

    private void hideWarning()
    {
        warningText.setVisibility(View.GONE);
    }

    /**
     * Displays information from the Location object
     *
     * @param pLocation Information about this location object is displayed
     */
    private void setLocation(@NonNull Location pLocation)
    {
        valueLon.setText(String.valueOf(pLocation.getLongitude()));
        valueLat.setText(String.valueOf(pLocation.getLatitude()));
        String lAltitude;
        String lAccuracy;
        String lSpeed;
        if(unitMeter.isChecked()){
            lAltitude = getResources().getString(R.string.valueMeter,pLocation.getAltitude());
            lAccuracy = getResources().getString(R.string.valueMeter,pLocation.getAccuracy());
            lSpeed    = getResources().getString(R.string.valueKmh,pLocation.getAccuracy() / METER_TO_KM);
        } else {
            lAltitude=getResources().getString(R.string.valueFoot,pLocation.getAltitude() * METER_TO_FOOT);
            lAccuracy=getResources().getString(R.string.valueFoot,pLocation.getAccuracy() * METER_TO_FOOT);
            lSpeed    = getResources().getString(R.string.valueMph,pLocation.getAccuracy() / METER_TO_MILE);
        }
        valueAltitude.setText(lAltitude);
        valueSpeed.setText(lSpeed);
        valueAccuracy.setText(lAccuracy);
    }

    /**
     * When  the location is changed this callback function is called.
     *
     * @param pLocation Current location
     */
    @Override
    public void onLocationChanged(@NonNull Location pLocation)
    {
        setLocation(pLocation);
    }

    /**
     * When status of the GPS is changed.
     * When the  GPS is unavailable , a message is displayed.
     * Also the number of satellites
     *
     * @param pProvider   The status is change for this type of provider
     * @param pStatus     Status (Availability)
     * @param pExtra      Extra information(used for getting the number of satellites) .
     */
    @Override
    public void onStatusChanged(String pProvider,int pStatus,Bundle pExtra)
    {
        if(LocationManager.GPS_PROVIDER.equals(pProvider)) {
            switch(pStatus){
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    displayWarning(R.string.warningGPSTempUnavailable);
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    displayWarning(R.string.warningGPSUnavailable);
                    break;
                case LocationProvider.AVAILABLE:
                    hideWarning();
                    break;
                default:
                    //do nothing
            }
        }
        int lNumSatellites=pExtra.getInt("satellites",-1);
        if(lNumSatellites>0) {
            valueNumSatellites.setText(String.valueOf(lNumSatellites));
        } else {
            valueNumSatellites.setText("-");
        }

    }

    /**
     * When the GPS is disabled this callback is called. In the method a warning is displayed that the
     * GPS is disabled
     *
     * @param pProvider Which location provider is disabled
     */
    @Override
    public void onProviderDisabled(String pProvider)
    {
        if(LocationManager.GPS_PROVIDER.equals(pProvider)) {
            displayWarning(R.string.warningGPSDisabled);
        }
    }

    /**
     * When the GPS is enabled this callback is called. In this method the warning that the GPS is disabled is hidden
     *
     * @param pProvider Which location provider is enabled.
     */
    @Override
    public void onProviderEnabled(String pProvider) {
        if(LocationManager.GPS_PROVIDER.equals(pProvider)) {
            hideWarning();
        }

    }

    /**
     * Disable GPS when the activity pauses
     */

    @Override
    protected void onPause()
    {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    /**
     * When necessary start GPS when activity is resumed
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        if (displayGPS.isChecked()) {
            startGPS();
        }
    }
}

