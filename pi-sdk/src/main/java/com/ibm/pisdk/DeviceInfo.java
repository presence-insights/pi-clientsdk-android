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

package com.ibm.pisdk;

import com.ibm.json.java.JSONObject;

/**
 * This abstract class encapsulates what is required by the PI management server for device registration.
 * The only method abstracted here is getDescriptor().  This allows for developers to use this class and
 * provide their own process for generating the device descriptor.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public abstract class DeviceInfo {
    /**
     * constant for internal devices
     */
    static final public String DEVICE_INTERNAL = "Internal";
    /**
     * constant for external devices
     */
    static final public String DEVICE_EXTERNAL = "External";

    static final String JSON_REGISTRATION_TYPE = "registrationType";
    static final String JSON_DEVICE_DESCRIPTOR = "descriptor";
    static final String JSON_NAME = "name";
    static final String JSON_DATA = "data";
    static final String JSON_UNENCRYPTED_DATA = "unencryptedData";
    static final String JSON_REGISTERED = "registered";
    static final String JSON_BLACKLIST = "blacklist";

    private String mName;
    private String mDescriptor;
    private String mType;
    private JSONObject mData;
    private JSONObject mUnencryptedData;
    private boolean mRegistered;
    private boolean mBlacklisted;

    /**
     *
     * @return name of device
     */
    public String getName() {
        return mName;
    }

    /**
     *
     * @param name name of device
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * abstract method for setting the device descriptor
     */
    protected abstract void setDescriptor();

    /**
     *
     * @param descriptor device descriptor used to uniquely identify the device
     */
    public void setDescriptor(String descriptor) {
        mDescriptor = descriptor;
    }

    /**
     *
     * @return device descriptor
     */
    public String getDescriptor() {
        return mDescriptor;
    }

    /**
     *
     * @return device type
     */
    public String getType() {
        return mType;
    }

    /**
     *
     * @param type device type
     */
    public void setType(String type) {
        this.mType = type;
    }

    /**
     *
     * @return metadata pertaining to the device
     */
    public JSONObject getData() {
        return mData;
    }

    /**
     *
     * @param data metadata pertaining to the device
     */
    public void setData(JSONObject data) {
        this.mData = data;
    }

    /**
     *
     * @return if the device is registered
     */
    public boolean isRegistered() {
        return mRegistered;
    }

    /**
     *
     * @return if the device is blacklisted
     */
    public boolean isBlacklisted() {
        return mBlacklisted;
    }

    /**
     *
     * @param blacklisted if the device is blacklisted. if true, analytics will ignore this device.
     */
    public void setBlacklisted(boolean blacklisted) {
        this.mBlacklisted = blacklisted;
    }

    /**
     *
     * @param registered if the device is registered
     */
    public void setRegistered(boolean registered) {
        this.mRegistered = registered;
    }

    /**
     *
     * @return metadata pertaining to the device
     */
    public JSONObject getUnencryptedData() {
        return mUnencryptedData;
    }

    /**
     *
     * @param unencryptedData metadata where encryption is not required
     */
    public void setUnencryptedData(JSONObject unencryptedData) {
        this.mUnencryptedData = unencryptedData;
    }

    /**
     * Default constructor
     */
    public DeviceInfo() {
        mRegistered = false;
    }

    /**
     * Simple constructor
     *
     * @param name name of device
     * @param type device type
     */
    public DeviceInfo(String name, String type) {
        mName = name;
        mType = type;
        mRegistered = false;
        mBlacklisted = false;
        // call extended classes setDescriptor to generate the descriptor for the device
        setDescriptor();
    }

    /**
     * Helper method to provide the class as a JSON Object
     *
     * @return DeviceInfo class members as a JSON Object
     */
    public JSONObject toJSON() {
        return addToJson(new JSONObject());
    }

    /**
     * Helper method to add the device info to an existing JSON Object
     *
     * @param payload an existing JSON Object
     * @return JSON Object with the device information added
     */
    public JSONObject addToJson(JSONObject payload) {
        // this is to ensure we do not overwrite the device descriptor when we are updating a document
        if (payload.get(JSON_DEVICE_DESCRIPTOR) == null) {
            payload.put(JSON_DEVICE_DESCRIPTOR, mDescriptor);
        }
        if (mName != null) {
            payload.put(JSON_NAME, mName);
        }
        if (mType != null) {
            payload.put(JSON_REGISTRATION_TYPE, mType);
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
}
