package org.elaya.gac;

interface GPSStatusCallback {
    void startFix();
    void gpsFixed();
    void numberOfSatellites(int pNumber);
}
