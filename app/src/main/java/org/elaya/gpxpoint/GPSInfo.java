package org.elaya.gpxpoint;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main window that displays the GPS information
 *
 */

public class GPSInfo extends Activity implements LocationListener{

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
     * Initialize GUI
     *
     * @param savedInstanceState  Saved information (not used)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpxinfo);
        valueLon = (TextView)findViewById(R.id.valueLon);
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
                GPSInfo.this.toggleDisplayGPS();
            }
        });
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            runtimePermission();
        } else {
            setupStatusListener();
        }
    }

    /**
     * For API >23, check for runtime permission.
     *
     */
    @RequiresApi(23)
    private void runtimePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            setupStatusListener();
        }

    }

    /**
     * Event call back when requestPermission is executed
     *
     * @param requestCode   Request code
     * @param permissions   Information over permission that are granted or refused.
     * @param grantResults  Information over grant result.
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupStatusListener();
        } else {
            displayWarning(R.string.locationAuth);
        }

    }

    /**
     *  Initialize the GPS status listener for api level<24
     * This listener checks when GPS started and when there is a GPS Fix
     *
     * @throws SecurityException When the app has not runtime rights to use the location service.
     */
    @RequiresApi(24)
    private void initGNSS() throws SecurityException{
        GnssStatus.Callback lListener=new GnssStatus.Callback(){
            public void onFirstFix(int pTtffMillis) {
                gpsFixed();
            }

            public void onStarted()
            {
                gpsFixed();
            }
        };
        locationManager.registerGnssStatusCallback(lListener);
    }


    /**
     * Initialize the GPS status listener.
     * Depending on the api level, the GNSS listener is used (API>=24) or else the GPSStatus is used
     * This listener checks when GPS started and when there is a GPS Fix
     */
    private void setupStatusListener()
    {

        try {
            if (Build.VERSION.SDK_INT >= 24) {
                initGNSS();
            } else {
                initGPSStatus();
            }
        } catch(SecurityException e){
            toast(e.getMessage());
        }
    }

    /**
     * Initialize the GPS status listener for api level<24
     * This listener checks when GPS started and when there is a GPS Fix
     *
     * @throws SecurityException When the app has not runtime rights to use the location service.
     */
    @SuppressWarnings( "deprecation" )
    private void initGPSStatus() throws SecurityException{
        GpsStatus.Listener lListener=new GpsStatus.Listener(){

            public void onGpsStatusChanged(int pEvent){
                switch(pEvent){
                    case GpsStatus.GPS_EVENT_STARTED:
                        gpsFixingStarted();
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        gpsFixed();
                        break;
                    default:
                        //nothing
                }
            }

        } ;
        locationManager.addGpsStatusListener(lListener);

    }

    /**
     * Display play a toast message
     *
     * @param pText Text to display
     */
    private void toast(String pText)
    {
        Toast lToast = Toast.makeText(getApplicationContext(),pText , Toast.LENGTH_LONG);
        lToast.show();
    }

    /**
     * Display a toast by String resource id
     *
     * @param pText Resource ID message
     */
    private void toast(int pText)
    {
        toast(getResources().getString(pText));
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
    public void copyCoordinates(@SuppressWarnings("unused") View pView)
    {
        if(lastLocation != null) {
            copyToClipboard(R.string.locationClip,String.valueOf(lastLocation.getLatitude())+" "+String.valueOf(lastLocation.getLongitude()));
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
        if(! "".equals(otherText)){
            copyToClipboard(R.string.otherClip,otherText);
            toast(R.string.otherCopied);
        }
    }

    /**
     * Fill the GPS display with the last known location
     * TODO Check Exception handling callers
     */
    private void latestLocation() throws SecurityException
    {
        Location lLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lLocation != null) {
            setLocation(lLocation);
        }
    }

    /**
     * Called for starting the GPS
     * * TODO Check Exception handling callers
     */

    private void startGPS() throws SecurityException
    {
        gpsData.setVisibility(View.VISIBLE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

        } catch(SecurityException e){
            displayWarning(R.string.locationAuth);
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

    private void gpsFixingStarted()
    {
        gpsFixLabel.setVisibility(View.VISIBLE);
    }

    private void gpsFixed()
    {
        gpsFixLabel.setVisibility(View.GONE);
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
     * Open a new activity that displays help information.
     * This page is activated by clikking the "?"  symbol.
     *
     * @param pView Not used
     */
    public void openHelp(View pView){
        try {
            Intent lHelpIntent = new Intent(this, HelpActivity.class);
            startActivity(lHelpIntent);
        }catch(Exception e){
            toast(e.getMessage());
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

