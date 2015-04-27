package com.ibm.pzsdk;

import com.ibm.json.java.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by hannigan on 4/24/15.
 */
public class PIDeviceInfo extends DeviceInfo {

    @Override
    public String getDescriptor() {
        return "PI_TEST_DESCRIPTOR";
    }
}
