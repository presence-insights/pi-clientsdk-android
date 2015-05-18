//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
package com.ibm.pzsdk;

import android.util.Log;

import com.ibm.json.java.JSONObject;

import org.altbeacon.beacon.Beacon;

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
    private String major;
    /**
     * unique identifier within the major space
     */
    private String minor;
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
     * time when the beacon was discovered in ms
     */
    private long detectedTime;
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
    public String getMajor() {
        return major;
    }

    /**
     *
     * @param major unique identifier within the proximity UUID space
     */
    public void setMajor(String major) {
        this.major = major;
    }

    /**
     *
     * @return unique identifier within the major space
     */
    public String getMinor() {
        return minor;
    }

    /**
     *
     * @param minor unique identifier within the major space
     */
    public void setMinor(String minor) {
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
    public void setAccuracy(double accuracy) {
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
     * @return time when the beacon was discovered in ms
     */
    public long getDetectedTime() {
        return detectedTime;
    }

    /**
     *
     * @param detectedTime time when the beacon was discovered in ms
     */
    public void setDetectedTime(long detectedTime) {
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
        this.major = beacon.getId2().toString();
        this.minor = beacon.getId3().toString();
        this.rssi = beacon.getRssi();
        this.accuracy = beacon.getDistance();
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
        this.major = Integer.toString(major);
        this.minor = Integer.toString(minor);
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
		try {
			beaconValidator(this);
		} catch (Exception e) {
			Log.e("ERROR", e.toString());
			e.printStackTrace();
		}

		JSONObject returnObj = new JSONObject();

        JSONObject beaconData = new JSONObject();
        beaconData.put("proximityUUID", proximityUUID);
        beaconData.put("major", major);
        beaconData.put("minor", minor);
        beaconData.put("accuracy", accuracy);
        beaconData.put("rssi", rssi);
        beaconData.put("proximity", proximity);

        returnObj.put("data", beaconData);
        returnObj.put("descriptor", deviceDescriptor);
        returnObj.put("detectedTime", detectedTime);
        return returnObj;
    }

	/**
	 * Simple test to see if a beacon is valid
	 * Cannot check RSSI or accuracy since they cannot be null
	 *
	 * @param beacon
	 * @throws Exception
	 */
	private void beaconValidator(PIBeaconData beacon) throws Exception {
		// DetectedTime, Accuracy, and RSSI (long, double, int) cannot be null
		if ((beacon.proximityUUID == null)
				|| (beacon.major == null)
				|| (beacon.minor == null)
				|| (beacon.proximity == null)
				|| (beacon.deviceDescriptor == null)
				) {
			throw new Exception("PIBeacon is not valid");
		}
	}

}
