package com.ibm.pzsdk;

import com.ibm.json.java.JSONObject;

import org.altbeacon.beacon.Beacon;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hannigan on 4/22/15.
 */
public class PIBeaconData {
    public String getProximityUUID() {
        return proximityUUID;
    }

    public void setProximityUUID(String proximityUUID) {
        this.proximityUUID = proximityUUID;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getDetectedTime() {
        return detectedTime;
    }

    public void setDetectedTime(String detectedTime) {
        this.detectedTime = detectedTime;
    }

    public String getProximity() {
        return proximity;
    }

    public void setProximity(String proximity) {
        this.proximity = proximity;
    }

    private String proximityUUID;
    private int major;
    private int minor;
    private int accuracy;
    private int rssi;
    private String descriptor;
    private String detectedTime;
    private String proximity;

    public PIBeaconData(Beacon b) {
        this.proximityUUID = b.getId1().toUuidString();
        this.major = b.getId2().toInt();
        this.minor = b.getId3().toInt();
        this.rssi = b.getRssi();
        this.descriptor = b.getBluetoothAddress();
        this.detectedTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        this.proximity = getProximityFromBeacon(b);
    }

    public PIBeaconData(String uuid, int major, int minor) {
        this.proximityUUID = uuid;
        this.major = major;
        this.minor = minor;
    }

    private String getProximityFromBeacon(Beacon b) {
        String proximity = "";
        double distance = b.getDistance();
        if (distance <= 0.5) {
            proximity = "immediate";
        } else if (distance <= 10.0) {
            proximity = "near";
        } else if (distance > 10.0) {
            proximity = "far";
        } else {
            proximity = "unknown";
        }
        return proximity;
    }

    public JSONObject getBeaconAsJson() {
        JSONObject beacon = new JSONObject();
        beacon.put("proximityUUID", proximityUUID);
        beacon.put("major", major);
        beacon.put("minor", minor);
        beacon.put("accuracy", accuracy);
        beacon.put("rssi", rssi);
        beacon.put("descriptor", descriptor);
        beacon.put("detectedTime", detectedTime);
        beacon.put("proximity", proximity);
        return beacon;
    }

}
