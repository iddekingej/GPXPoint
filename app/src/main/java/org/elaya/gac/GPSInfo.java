package org.elaya.gac;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.elaya.gac.databinding.ActivityGpxinfoBinding;

import java.util.Date;

/**
 * Main window that displays the GPS information
 *
 */

public class GPSInfo extends AppCompatActivity {



    private Location lastLocation=null;
    private SharedPreferences settings;
    private String   otherText;
    private ActivityGpxinfoBinding binding;
    private GpsManager gpsManager;

    private static final double METER_TO_FOOT = 3.2808399;
    private static final double METER_TO_MPH  = 3.6/1.609344;
    private static final double METER_TO_KMH  = 3.6;
    private static final String PREF_MAIN     = "main";
    private static final String PREF_S_UNIT_METER = "unit_meter";


    /**
     * No warning message is displayed
     */
    public static final int ST_OK=0;


    /**
     * Status displays a message that the GPS is disabled
     */

    public  static final int ST_DISABLED=2;

    /**
     * Status displays a message that the GPS is unavailable
     */

    public  static final int ST_UNAVAILABLE=3;

    /**
     * Status displays a message that the GPS is temp unavailable
     */

    public  static final int ST_TEMP_UNAVAILABLE=4;

    /**
     * Initialize GUI
     *
     * @param savedInstanceState  Saved information (not used)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpxinfo);


        binding=DataBindingUtil.setContentView(this,R.layout.activity_gpxinfo );

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        binding.gpsData.setVisibility(View.GONE);
        binding.gpsFixLabel.setVisibility(View.GONE);

        showNotActual(false);
        displayPermissionWarning(false);

        settings = getSharedPreferences(PREF_MAIN, 0);

        boolean lUnitMeter = settings.getBoolean(PREF_S_UNIT_METER, true);

        if(lUnitMeter) {
            binding.unitMeter.toggle();
        } else {
            binding. unitFoot.toggle();
        }


        hideWarning();

        final GPSInfo lThis=this;

        gpsManager=new GpsManager(this) {
            @Override
            protected void setStatus(int pStatus) {
                lThis.setStatus(pStatus);
            }

            @Override
            public void onLocationChanged(Location pLocation) {
                setLocation(pLocation);
            }
        };




        binding.displayGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleDisplayGPS();
            }
        });
        if (Build.VERSION.SDK_INT >= 23) {
            runtimePermission();
        } else {
            setupStatusListener();
        }
        enableStatusListener();
    }

    private void gotoMaps()
    {
        if(lastLocation == null){
            toast(R.string.location_unknown);
            return;
        }
        String lLocation=String.valueOf(lastLocation.getLatitude())+","+String.valueOf(lastLocation.getLongitude());
        Uri lLocationUri=Uri.parse("geo:"+lLocation+"?q="+lLocation);
        Intent lMap=new Intent(Intent.ACTION_VIEW,lLocationUri);
        lMap.setPackage("com.google.android.apps.maps");
        if(lMap.resolveActivity(getPackageManager())!= null){
            startActivity(lMap);
        } else {
            toast(R.string.googleMapNotFound);
        }
    }

    private void shareData()
    {
        if(lastLocation==null){
            toast(R.string.location_unknown);
            return;
        }
        Intent lShareIntent=new Intent();
        lShareIntent.setAction(Intent.ACTION_SEND);

        String lSendText;

            lSendText =
                    getResources().getString(R.string.lat) + ":" + String.valueOf(lastLocation.getLatitude())
                            + "\n" + getResources().getString(R.string.lon) + ":" + String.valueOf(lastLocation.getLongitude())
                            + "\n\n" + otherText;

        lShareIntent.putExtra(Intent.EXTRA_TEXT,lSendText);
        lShareIntent.setType("text/plain");
        startActivity(lShareIntent);
    }

    /**
     * Open a new activity that displays help information.
     * This page is activated by clicking the "?"  symbol.
     *
     */
    private void openHelp(){
        try {
            Intent lHelpIntent = new Intent(this, HelpActivity.class);
            startActivity(lHelpIntent);
        }catch(Exception e){
            toast(e.getMessage());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem pItem) {
        switch (pItem.getItemId()) {
            case R.id.map:
                gotoMaps();
                break;

            case R.id.share:
                shareData();
                break;

            case R.id.help:
                openHelp();
                break;

            default:
                return super.onOptionsItemSelected(pItem);
        }
        return true;
    }

    //Todo can also call gpsManager don't neet gateway

    private void enableStatusListener()
    {
        gpsManager.enableStatusListener();
    }

    private void disableStatusListener()
    {
        gpsManager.disableStatusListener();
    }

    private void displayPermissionWarning(boolean pFlag)
    {
        if(pFlag) {
            binding.permissionWarning.setVisibility(View.VISIBLE);
        }else {
            binding.permissionWarning.setVisibility(View.GONE);
        }
    }

    private void setStatus(int pCode)
    {
        switch(pCode){
            case ST_OK:
                hideWarning();
                break;

            case ST_DISABLED:
                displayWarning(R.string.warningGPSDisabled);
                break;

            case ST_UNAVAILABLE:
                displayWarning(R.string.warningGPSUnavailable);
                break;

            case ST_TEMP_UNAVAILABLE:
                displayWarning(R.string.warningGPSTempUnavailable);
                break;

        }
    }




    private boolean hasNoRuntimeGPSPermission()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * For API >23, check for runtime permission.
     *
     */

    private void runtimePermission(){
        if(hasNoRuntimeGPSPermission()){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            setupStatusListener();
        }

    }


    public void askPermission(View pButton){
        runtimePermission();
    }

    /**
     * Event call back when requestPermission is executed
     *
     * @param requestCode   Request code
     * @param permissions   Information over permission that are granted or refused.
     * @param grantResults  Information over grant result.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupStatusListener();
            displayPermissionWarning(false);

        } else {
            displayPermissionWarning(true);
        }

    }

    /**
     *  Initialize the GPS status listener for api level<24
     * This listener checks when GPS started and when there is a GPS Fix
     *
     * @throws SecurityException When the app has not runtime rights to use the location service.
     */
    @RequiresApi(24)
    private void initGNSS() {
        GnssStatus.Callback lListener=new GnssStatus.Callback(){
            public void onFirstFix(int pTtffMillis) {
                gpsFixed();
            }
            public void onSatelliteStatusChanged(GnssStatus pStatus){
                    numberOfSatellites(pStatus.getSatelliteCount());
            }
            public void onStarted()
            {
                gpsFixed();
            }
        };
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            gpsManager.getLocationManager().registerGnssStatusCallback(lListener);
        }
    }

    /**
     * When the number of satellites changes, this function is called.
     * The number is updated in the GUI.
     *
     * @param pNum Number of satellites used in the fix.
     */
    private void numberOfSatellites(int pNum){
        binding.valueNumSatellites.setText(String.valueOf(pNum));
    }

    /**
     * Initialize the GPS status listener.
     * Depending on the api level, the GNSS listener is used (API>=24) or else the GPSStatusCallback is used
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

    /**
     * Initialize the GPS status listener for api level<24
     * This listener checks when GPS started and when there is a GPS Fix
     *
     * @throws SecurityException When the app has not runtime rights to use the location service.
     */
    private void initGPSStatus() {
        GpsStatus.Listener lListener=new GpsStatus.Listener(){

            public void onGpsStatusChanged(int pEvent){
                switch(pEvent){
                    case GpsStatus.GPS_EVENT_STARTED:
                        gpsFixingStarted();
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        gpsFixed();
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        try {
                            GpsStatus lStatus = gpsManager.getLocationManager().getGpsStatus(null);
                            numberOfSatellites(getFixedSatellites(lStatus.getSatellites()));
                        }catch(SecurityException e){
                            //Do nothing from now
                        }
                        break;
                    default:
                        //nothing
                }
            }

        } ;
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            gpsManager.getLocationManager().addGpsStatusListener(lListener);
        }

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
        if(lClipboard != null) {
            ClipData lData = ClipData.newPlainText(getResources().getString(pLabel), pText);
            lClipboard.setPrimaryClip(lData);
        }
    }


    /**
     * Copy coordinates to clipboard
     *
     * @param pView  unused. User clicked on this view
     */
    public void copyCoordinates(@SuppressWarnings("unused") View pView)
    {
        if(lastLocation != null) {
            copyToClipboard(R.string.locationClip,getLocationText());
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


    private String getLocationText()
    {
        return String.valueOf(lastLocation.getLatitude())+" "+String.valueOf(lastLocation.getLongitude());
    }

     /**
     * Called for starting the GPS
     * * TODO Check Exception handling callers
     */

    private void startGPS() {
        binding.gpsData.setVisibility(View.VISIBLE);
        try{
            if (gpsManager.enableUpdates()){
                displayPermissionWarning(true);
            }
        } catch (Throwable e) {
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
        lEditor.putBoolean(PREF_S_UNIT_METER,binding.unitMeter.isChecked());
        lEditor.apply();
        if(lastLocation != null){
            setLocation(lastLocation);
        }
    }

    private void showNotActual(boolean pFlag)
    {
        if(pFlag){
            String lText=getResources().getString(R.string.not_current)+DateFormat.getTimeFormat(this).format(new Date());
            binding.notCurrent.setText(lText);
            binding.notCurrent.setVisibility(View.VISIBLE);
        } else {
            binding.notCurrent.setVisibility(View.GONE);
        }
    }

    /**
     * When "displayGPS" is toggled on or of this method is called
     * when displaying GPS is turned of, the GPS information is hidden en LocationUpdates are stopped
     * when turned on the LocationListener is turned on and receives location updates
     */
    private void toggleDisplayGPS() {
        if (binding.displayGPS.isChecked()) {
            showNotActual(false);
            startGPS();
        } else {
            if(lastLocation != null){
                showNotActual(true);
            } else {
                showNotActual(false);
                binding.gpsData.setVisibility(View.GONE);
                binding.gpsFixLabel.setVisibility(View.GONE);
            }
            gpsManager.disableUpdates();

        }
    }

    /**
     * Displays a warning message on top of the screen (with red background)
     *
     * @param pWarning Warning message to display
     */
    private void displayWarning(int pWarning)
    {
        binding.warningText.setText(getResources().getString(pWarning));
        binding.warningText.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the warning messages
     */

    private void hideWarning()
    {
        binding.warningText.setVisibility(View.GONE);
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
        binding.valueLon.setText(String.valueOf(pLocation.getLongitude()));
        binding.valueLat.setText(String.valueOf(pLocation.getLatitude()));
        String lAltitude;
        String lAccuracy;
        String lSpeed;
        if(binding.unitMeter.isChecked()){
            lAltitude = getResources().getString(R.string.valueMeter,pLocation.getAltitude());
            lAccuracy = getResources().getString(R.string.valueMeter,pLocation.getAccuracy());
            lSpeed    = getResources().getString(R.string.valueKmh,pLocation.getSpeed()* METER_TO_KMH);
        } else {
            lAltitude=getResources().getString(R.string.valueFoot,pLocation.getAltitude() * METER_TO_FOOT);
            lAccuracy=getResources().getString(R.string.valueFoot,pLocation.getAccuracy() * METER_TO_FOOT);
            lSpeed    = getResources().getString(R.string.valueMph,pLocation.getSpeed() * METER_TO_MPH);
        }
        binding.valueAltitude.setText(lAltitude);
        binding.valueSpeed.setText(lSpeed);
        binding.valueAccuracy.setText(lAccuracy);
        lastLocation=pLocation;
        otherText=makeText(R.string.Speed,lSpeed)
                 +makeText(R.string.altitude,lAltitude)
                 +makeText(R.string.accuracy,lAccuracy)
                 +makeText(R.string.numSatellites,binding.valueNumSatellites.getText().toString());
    }

    /**
     * Called when GPS tries to get a fix on the satellites
     */
    private void gpsFixingStarted()
    {
        binding.valueNumSatellites.setText("-");
        binding.gpsFixLabel.setVisibility(View.VISIBLE);
    }

    private void gpsFixed()
    {
        binding.gpsFixLabel.setVisibility(View.GONE);
    }



    /**
     * Set the toolbar option menu
     *
     * @param pMenu Toolbar menu
     * @return If handled
     */
    @Override
    public boolean onCreateOptionsMenu(Menu pMenu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, pMenu);
        return super.onCreateOptionsMenu(pMenu);
    }


    /**
     * Disable GPS when the activity pauses
     */

    @Override
    protected void onPause()
    {
        super.onPause();
        gpsManager.disableUpdates();
        disableStatusListener();
    }

    /**
     * When necessary start GPS when activity is resumed
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        enableStatusListener();
        if(Build.VERSION.SDK_INT >= 23) {
            if(hasNoRuntimeGPSPermission()){
                displayPermissionWarning(true);
            }
        }

        if (gpsManager.getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setStatus(ST_OK);
        } else {
            setStatus(ST_DISABLED);
        }

        if (binding.displayGPS.isChecked()){

            startGPS();
        }
    }
}

