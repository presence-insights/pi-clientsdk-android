package com.ibm.pisdk.doctypes;

import com.ibm.json.java.JSONObject;

/**
 * Simple class to encapsulate the Sensor documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PISensor {
    private static final String JSON_CODE = "@code";
    private static final String JSON_NAME = "name";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_X = "x";
    private static final String JSON_Y = "y";
    private static final String JSON_THRESHOLD = "threshold";

    // required
    private String code;
    private String name;
    private long threshold;
    private long x;
    private long y;

    // optional
    private String description;

    public PISensor(JSONObject sensorObj) {
        code = (String) sensorObj.get(JSON_CODE);
        name = (String) sensorObj.get(JSON_NAME);
        threshold = (Long) sensorObj.get(JSON_THRESHOLD);
        x = (Long) sensorObj.get(JSON_X);
        y = (Long) sensorObj.get(JSON_Y);

        description = sensorObj.get(JSON_DESCRIPTION) != null ? (String)sensorObj.get(JSON_DESCRIPTION) : "";
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getThreshold() {
        return threshold;
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    public String getDescription() {
        return description;
    }
}
