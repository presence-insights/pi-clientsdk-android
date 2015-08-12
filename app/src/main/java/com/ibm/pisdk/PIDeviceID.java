//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
package com.ibm.pisdk;

import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * This class collects useful information about the current Android device.  PI uses it to collect
 * the MAC Address for uniquely identifying a device.
 *
 * @author Ryan Berger and MobileFirst team
 */
public class PIDeviceID {
    private final String platform = "ANDROID"; //$NON-NLS-1$
    private String uuid;
    private String macAddress;
    private String hardwareId;
    private String model;
    private String platformVersion;
    private String name;
    private Context context;


    private static final String TAG = PIDeviceID.class.getCanonicalName();

    /**
     * Default Constructor
     */
    PIDeviceID(Context context) {
        //Set the device model
        this.context = context;
        hardwareId = calculateHardwareId();
        platformVersion = "" + Build.VERSION.SDK_INT; //$NON-NLS-1$
        model = Build.MANUFACTURER + "-" + Build.MODEL; //$NON-NLS-1$
        name = platform + " " + platformVersion + " " + model;
        String manufacturer = Build.MANUFACTURER;
        if (model.startsWith(manufacturer)) {
            name = model;
        } else {
            name = manufacturer + " " + model;
        }
    }

    /**
     * Calculates the uuid for this device using the MAC address and the Android ID.  It retrieves the
     * MAC address from the WifiManager.
     *
     * @return uuid for device
     */
    private String calculateHardwareId() {
        String resultId;
        try {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                WifiManager wfManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiinfo = wfManager.getConnectionInfo();
                macAddress = wifiinfo.getMacAddress();
            } else {
                Log.d(TAG, "Cannot access WIFI service for a more robust device id"); //$NON-NLS-1$
            }
            String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

            if (macAddress != null) {
                uuid += macAddress;
            }
            // Use a hashed UUID not exposing the device ANDROID_ID/Mac Address
            resultId = UUID.nameUUIDFromBytes(uuid.getBytes()).toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to object in device ID in DefaultDeviceIdProvider", e); //$NON-NLS-1$
            resultId = "UNKNOWN_ID"; //$NON-NLS-1$
        }
        return resultId;
    }

    /**
     * Generate a Device UUID based on ANDROID_ID and Device MAC. The addition
     * of MAC address is to ensure uniqueness. There are reported cases of
     * non-unique ANDROID_ID in Froyo devices are of no concern.
     *
     * @return The device identifier.
     */
    public String getHardwareId() {
        return hardwareId;
    }

    /**
     *
     * @return The device model.
     */
    public String getModel() {
        return model;
    }

    /**
     *
     * @return the device name ("platformName Version Model")
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return the platform of the device (API level)
     */
    public String getPlatform() {
        return platform;
    }

    /**
     *
     * @return The device platform version (API level)
     */
    public String getPlatformVersion() {
        return platformVersion;
    }

    /**
     *
     * @return unique identifier of device
     */
    public String getUuid() {
        return uuid;
    }

    /**
     *
     * @param uuid the uuid to set
     */
    void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     *
     * @return the MAC address of the device
     */
    public String getMacAddress() {
        return macAddress;
    }
}
