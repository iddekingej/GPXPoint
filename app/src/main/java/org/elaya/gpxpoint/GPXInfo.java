package org.elaya.gpxpoint;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import android.content.SharedPreferences;
import android.location.GpsStatus;
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


public class GPXInfo extends Activity implements LocationListener , GpsStatus.Listener {

    private TextView valueLon;
    private TextView valueLat;
    private TextView valueAltitude;
    private TextView valueSpeed;
    private TextView valueAccuracy;
    private TextView valueNumSatellites;
    private TextView gpsFixLabel;
    private TextView warningText;
    private Switch displayGPS;
    private LinearLayout gpsData;
    private RadioButton unitMeter;
    private SharedPreferences settings;
    private Location lastLocation=null;
    private String   otherText;

    private static final double METER_TO_FOOT = 3.2808399;
    private static final double METER_TO_MPH  = 3.6/1.609344;
    private static final double METER_TO_KMH  = 3.6;
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
        warningText   = (TextView) findViewById(R.id.warningText);
        displayGPS    = (Switch) findViewById(R.id.displayGPS);
        unitMeter     = (RadioButton) findViewById(R.id.unitMeter);
        gpsFixLabel    = (TextView) findViewById(R.id.gpsFixLabel);
        RadioButton unitFoot   = (RadioButton) findViewById(R.id.unitFoot);
        gpsData.setVisibility(View.GONE);
        gpsFixLabel.setVisibility(View.GONE);

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
        locationManager.addGpsStatusListener(this);
    }

    /**
     * Display a toast by String resource id
     *
     * @param pText Resource ID message
     */
    private void toast(int pText)
    {
        Toast lToast = Toast.makeText(getApplicationContext(),getResources().getString(pText) , Toast.LENGTH_LONG);
        lToast.show();
    }


    /**
     * Copy text to clipboard
     *
     * @param pLabel  Descriptive label (Is ID of string resource)
     * @param pText   Text to copy to clipboard
     */

    private void copyToClipboard(int pLabel,String pText)
    {
        ClipboardManager lClipboard=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData lData=ClipData.newPlainText(getResources().getString(pLabel),pText);
        lClipboard.setPrimaryClip(lData);
    }


    /**
     * Copy coordinates to clipboard
     *
     * @param pView  unused. User clicked on this view
     */
    public void copyCoordinates(View pView)
    {
        if(lastLocation != null) {
            copyToClipboard(R.string.locationClip,String.valueOf(lastLocation.getLongitude())+" "+String.valueOf(lastLocation.getLatitude()));
            toast(R.string.coordinatesCopied);
        }
    }

    /**
     * Copy coordinates to clipboard
     *
     * @param pView  unused. User clicked on this view
     */

    public void copyOther(View pView)
    {
        if(otherText != ""){
            copyToClipboard(R.string.otherClip,otherText);
            toast(R.string.otherCopied);;
        }
    }

    /**
     * Fill the GPS display with the last known location
     */
    private void latestLocation()
    {
        Location lLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lLocation != null) {
            setLocation(lLocation);
        }
    }

    /**
     * Called for starting the GPS
     */

    private void startGPS()
    {
        gpsData.setVisibility(View.VISIBLE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

        } catch (Exception e) {
            Toast lToast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            lToast.show();
        }
    }

    /**
     * Called when a user changes unit type (meters to foots).
     * The settings is saved and the display is refreshed.
     *
     * @param pRadio Radio button pressed. Not used
     */
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

    private String makeText(int pLabel,String pValue){
        return getResources().getString(pLabel)+" : "+pValue+" \n";
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
            lSpeed    = getResources().getString(R.string.valueKmh,pLocation.getSpeed()* METER_TO_KMH);
        } else {
            lAltitude=getResources().getString(R.string.valueFoot,pLocation.getAltitude() * METER_TO_FOOT);
            lAccuracy=getResources().getString(R.string.valueFoot,pLocation.getAccuracy() * METER_TO_FOOT);
            lSpeed    = getResources().getString(R.string.valueMph,pLocation.getSpeed() * METER_TO_MPH);
        }
        valueAltitude.setText(lAltitude);
        valueSpeed.setText(lSpeed);
        valueAccuracy.setText(lAccuracy);
        lastLocation=pLocation;
        otherText=makeText(R.string.Speed,lSpeed)
                 +makeText(R.string.altitude,lAltitude)
                 +makeText(R.string.accuracy,lAccuracy)
                 +makeText(R.string.numSatellites,valueNumSatellites.getText().toString());
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
     * Checking for GPS status change. (from the GPSStatus.Listener interface)
     * Used for displaying gps fix message
     *
     * @param pEvent Type of change event (Used to determine if there is a gps fix)
     */
    @Override
    public void onGpsStatusChanged(int pEvent)
    {
        switch(pEvent){
            case GpsStatus.GPS_EVENT_STARTED:
                gpsFixLabel.setVisibility(View.VISIBLE);
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                gpsFixLabel.setVisibility(View.GONE);
                break;
            default:
                //nothing
        }

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

