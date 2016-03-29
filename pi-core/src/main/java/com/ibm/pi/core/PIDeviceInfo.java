/**
 * Copyright (c) 2015 IBM Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.pi.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.ibm.json.java.JSONObject;

/**
 * This class encapsulates what is required by the PI management server for device registration.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIDeviceInfo {
    static final String TAG = PIDeviceInfo.class.getSimpleName();
    static final String JSON_REGISTRATION_TYPE = "registrationType";
    static final String JSON_DEVICE_DESCRIPTOR = "descriptor";
    static final String JSON_NAME = "name";
    static final String JSON_DATA = "data";
    static final String JSON_UNENCRYPTED_DATA = "unencryptedData";
    static final String JSON_REGISTERED = "registered";
    static final String JSON_BLACKLIST = "blacklist";

    private String mName;
    private String mDeviceDescriptor;
    private String mRegistrationType;
    private JSONObject mData;
    private JSONObject mUnencryptedData;
    private boolean mRegistered = false;
    private boolean mBlacklisted = false;

    /**
     * Constructor for anonymous devices. This constructor will use ANDROID_ID as the
     * descriptor for the device.
     *
     * @param context
     */
    public PIDeviceInfo(Context context) {
        mDeviceDescriptor = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        updateDeviceDescriptor(context);
    }

    /**
     * Constructor for anonymous devices with custom descriptors.
     *
     * @param context
     * @param deviceDescriptor device descriptor used to uniquely identify the device
     */
    public PIDeviceInfo(Context context, String deviceDescriptor) {
        mDeviceDescriptor = deviceDescriptor;
        updateDeviceDescriptor(context);
    }

    /**
     * Constructor for devices you plan on registering. This constructor will use ANDROID_ID as the
     * descriptor for the device.
     *
     * @param context
     * @param name name of device
     * @param registrationType device registration type
     */
    public PIDeviceInfo(Context context, String name, String registrationType) {
        mName = name;
        mRegistrationType = registrationType;
        mRegistered = true;
        mDeviceDescriptor = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        updateDeviceDescriptor(context);
    }

    /**
     * Constructor, for devices you plan on registering, which allows you to specify your own
     * unique descriptor for this device.
     *
     * @param context
     * @param name name of device
     * @param registrationType device registration type
     * @param deviceDescriptor device descriptor used to uniquely identify the device
     */
    public PIDeviceInfo(Context context, String name, String registrationType, String deviceDescriptor) {
        mName = name;
        mRegistrationType = registrationType;
        mRegistered = true;
        mDeviceDescriptor = deviceDescriptor;

        updateDeviceDescriptor(context);
    }

    private void updateDeviceDescriptor(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PI_SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PI_SHARED_PREFS_DESCRIPTOR_KEY, mDeviceDescriptor);
        editor.apply();
    }

    /**
     *
     * @param data metadata pertaining to the device where encryption is required
     */
    public void setData(JSONObject data) {
        this.mData = data;
    }

    /**
     *
     * @param unencryptedData metadata pertaining to the device where encryption is not required
     */
    public void setUnencryptedData(JSONObject unencryptedData) {
        this.mUnencryptedData = unencryptedData;
    }

    /**
     *
     * @param blacklisted if the device is blacklisted. if true, analytics will ignore this device.
     */
    public void setBlacklisted(boolean blacklisted) {
        this.mBlacklisted = blacklisted;
    }

    /**
     * Helper method to provide the class as a JSON Object
     *
     * @return DeviceInfo class members as a JSON Object
     */
    protected JSONObject toJSON() {
        return addToJson(new JSONObject());
    }

    /**
     * Helper method to add the device info to an existing JSON Object
     *
     * @param payload an existing JSON Object
     * @return JSON Object with the device information added
     */
    protected JSONObject addToJson(JSONObject payload) {
        // this is to ensure we do not overwrite the device descriptor when we are updating a document
        if (payload.get(JSON_DEVICE_DESCRIPTOR) == null) {
            payload.put(JSON_DEVICE_DESCRIPTOR, mDeviceDescriptor);
        }
        if (mName != null) {
            payload.put(JSON_NAME, mName);
        }
        if (mRegistrationType != null) {
            payload.put(JSON_REGISTRATION_TYPE, mRegistrationType);
        }
        if (mData != null) {
            payload.put(JSON_DATA, mData);
        }
        if (mUnencryptedData != null) {
            payload.put(JSON_UNENCRYPTED_DATA, mUnencryptedData);
        }
        payload.put(JSON_REGISTERED, mRegistered);
        payload.put(JSON_BLACKLIST, mBlacklisted);

        return payload;
    }

    // remove once we handle Task 97443
    protected String getDescriptor() {
        return mDeviceDescriptor;
    }

    protected void setRegistered(boolean registered) {
        mRegistered = registered;
    }
}
