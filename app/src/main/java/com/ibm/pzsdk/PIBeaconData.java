package com.ibm.pzsdk;

import com.ibm.json.java.JSONObject;

import org.altbeacon.beacon.Beacon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class encapsulates all the data required to send to the PI Beacon Connector.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIBeaconData {

    /**
     * universally unique identifier for the beacon
     */
    private String proximityUUID;
    /**
     * unique identifier within the proximity UUID space
     */
    private int major;
    /**
     * unique identifier within the major space
     */
    private int minor;
    /**
     * in iBeacon speak, this means distance in meters
     */
    private double accuracy;
    /**
     * the beacon's signal strength
     */
    private int rssi;
    /**
     * the unique identifier of the device that detected the beacon, in this case an android device
     */
    private String deviceDescriptor;
    /**
     * time when the beacon was discovered in yyyy-MM-dd format
     */
    private String detectedTime;
    /**
     * string representing the range of a beacon (immediate, near, far)
     */
    private String proximity;

    /**
     *
     * @return proximity UUID
     */
    public String getProximityUUID() {
        return proximityUUID;
    }

    /**
     *
     * @param proximityUUID universally unique identifier for the beacon
     */
    public void setProximityUUID(String proximityUUID) {
        this.proximityUUID = proximityUUID;
    }

    /**
     *
     * @return unique identifier within the proximity UUID space
     */
    public int getMajor() {
        return major;
    }

    /**
     *
     * @param major unique identifier within the proximity UUID space
     */
    public void setMajor(int major) {
        this.major = major;
    }

    /**
     *
     * @return unique identifier within the major space
     */
    public int getMinor() {
        return minor;
    }

    /**
     *
     * @param minor unique identifier within the major space
     */
    public void setMinor(int minor) {
        this.minor = minor;
    }

    /**
     *
     * @return accuracy of beacon
     */
    public double getAccuracy() {
        return accuracy;
    }

    /**
     *
     * @param accuracy of the beacon
     */
    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    /**
     *
     * @return the beacon's signal strength
     */
    public int getRssi() {
        return rssi;
    }

    /**
     *
     * @param rssi the beacon's signal strength
     */
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    /**
     *
     * @return the unique identifier of the device that detected the beacon, in this case an android device
     */
    public String getDeviceDescriptor() {
        return deviceDescriptor;
    }

    /**
     *
     * @param descriptor the unique identifier of the device that detected the beacon, in this case an android device
     */
    public void setDeviceDescriptor(String descriptor) {
        deviceDescriptor = descriptor;
    }

    /**
     *
     * @return time when the beacon was discovered in yyyy-MM-dd format
     */
    public String getDetectedTime() {
        return detectedTime;
    }

    /**
     *
     * @param detectedTime time when the beacon was discovered in yyyy-MM-dd format
     */
    public void setDetectedTime(String detectedTime) {
        this.detectedTime = detectedTime;
    }

    /**
     *
     * @return range of a beacon (immediate, near, far)
     */
    public String getProximity() {
        return proximity;
    }

    /**
     *
     * @param proximity range of a beacon (immediate, near, far)
     */
    public void setProximity(String proximity) {
        this.proximity = proximity;
    }

    /**
     *
     * @param beacon AltBeacon beacon
     */
    public PIBeaconData(Beacon beacon) {
        this.proximityUUID = beacon.getId1().toUuidString();
        this.major = beacon.getId2().toInt();
        this.minor = beacon.getId3().toInt();
        this.rssi = beacon.getRssi();
        this.accuracy = beacon.getDistance();
        this.detectedTime = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        this.proximity = getProximityFromBeacon(beacon);
    }

    /**
     *
     * @param uuid universally unique identifier for the beacon
     * @param major unique identifier for beacon within uuid space
     * @param minor unique identifier for beacon within major space
     */
    public PIBeaconData(String uuid, int major, int minor) {
        this.proximityUUID = uuid;
        this.major = major;
        this.minor = minor;
    }

    /**
     * Returns a string representation of the beacons range based on it's distance.
     *
     * @param beacon AltBeacon beacon
     * @return string representing the range of a beacon (immediate, near, far)
     */
    private String getProximityFromBeacon(Beacon beacon) {
        String proximity;
        double distance = beacon.getDistance();
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

    /**
     * Helper method to provide the class as a JSON Object
     *
     * @return JSON Object of the beacon's data
     */
    public JSONObject getBeaconAsJson() {
        JSONObject beacon = new JSONObject();
        beacon.put("proximityUUID", proximityUUID);
        beacon.put("major", major);
        beacon.put("minor", minor);
        beacon.put("rssi", rssi);
        beacon.put("descriptor", deviceDescriptor);
        beacon.put("detectedTime", detectedTime);
        beacon.put("proximity", proximity);
        return beacon;
    }

}
