package com.ibm.pzsdk;

import com.ibm.json.java.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hannigan on 4/27/15.
 */
public abstract class DeviceInfo {
    static final public String DEVICE_INTERNAL = "Internal";
    static final public String DEVICE_EXTERNAL = "External";

    static final String JSON_REGISTRATION_TYPE = "registrationType";
    static final String JSON_DEVICE_DESCRIPTOR = "descriptor";
    static final String JSON_NAME = "name";
    static final String JSON_DATA = "data";
    static final String JSON_REGISTERED = "registered";

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    protected abstract void setDescriptor();

    public void setDescriptor(String descriptor) {
        mDescriptor = descriptor;
    }

    public String getDescriptor() {
        return mDescriptor;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public JSONObject getData() {
        return mData;
    }

    public void setData(JSONObject data) {
        this.mData = data;
    }

    public boolean isRegistered() {
        return mRegistered;
    }

    public void setRegistered(boolean registered) {
        this.mRegistered = registered;

    }

    private String mName;
    private String mDescriptor;
    private String mType;
    private JSONObject mData;
    private boolean mRegistered;

    public DeviceInfo() {
        mRegistered = false;
    }

    public DeviceInfo(String name, String type) {
        mName = name;
        mType = type;
        mRegistered = false;
        mDescriptor = getDescriptor();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put(JSON_NAME, mName);
        json.put(JSON_DEVICE_DESCRIPTOR, mDescriptor);
        json.put(JSON_REGISTRATION_TYPE, mType);
        json.put(JSON_REGISTERED, mRegistered);
        if (mData != null) {
            json.put(JSON_DATA, mData);
        }

        return json;
    }

    public JSONObject addToJson(JSONObject payload) {
        payload.put(JSON_DEVICE_DESCRIPTOR, mDescriptor);
        payload.put(JSON_NAME, mName);
        payload.put(JSON_REGISTRATION_TYPE, mType);
        payload.put(JSON_REGISTERED, mRegistered);
        if (mData != null) {
            payload.put(JSON_DATA, mData);
        }

        return payload;
    }
}
