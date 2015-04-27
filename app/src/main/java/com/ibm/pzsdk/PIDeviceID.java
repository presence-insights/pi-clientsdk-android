package com.ibm.pzsdk;

import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by bergerr on 4/24/2015.
 */
public class PIDeviceID {
    private final String platform = "ANDROID"; //$NON-NLS-1$
    private String uuid;
    private String hardwareId;
    private String model;
    private String platformVersion;
    private String name;
    private Context context;


    private static final String TAG = PIDeviceID.class.getCanonicalName();

    /**
     *
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

    private String calculateHardwareId() {
        String resultId = null;
        try {
            String macAddr = null;
            PackageManager packageManager = context.getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                WifiManager wfManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiinfo = wfManager.getConnectionInfo();
                macAddr = wifiinfo.getMacAddress();
            } else {
                Log.d(TAG, "Cannot access WIFI service for a more robust device id"); //$NON-NLS-1$
            }
            String uuid = Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

            if (macAddr != null) {
                uuid += macAddr;
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
     * @return The device model.
     */
    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public String getPlatform() {
        return platform;
    }

    /**
     * @return The device platform version.
     */
    public String getPlatformVersion() {
        return platformVersion;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid
     *            the uuid to set
     */
    void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
