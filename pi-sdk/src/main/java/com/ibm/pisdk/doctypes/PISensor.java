package com.ibm.pisdk.doctypes;

import com.ibm.json.java.JSONArray;
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
    private static final String JSON_THRESHOLD = "threshold";

    // required
    private String code;
    private String name;
    private long threshold;
    private long x;
    private long y;

    // optional
    private String description;

    // raw JSON object
    private JSONObject rawData;

    public PISensor(JSONObject sensorObj) {
        JSONObject geometry = (JSONObject)sensorObj.get("geometry");
        JSONObject properties = (JSONObject)sensorObj.get("properties");

        code = (String) properties.get(JSON_CODE);
        name = (String) properties.get(JSON_NAME);
        description = properties.get(JSON_DESCRIPTION) != null ? (String)properties.get(JSON_DESCRIPTION) : "";
        threshold = (Long) properties.get(JSON_THRESHOLD);

        JSONArray coordinates = (JSONArray) geometry.get("coordinates");
        x = (Long) coordinates.get(0);
        y = (Long) coordinates.get(1);

        rawData = sensorObj;
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

    public JSONObject getRawData() {
        return rawData;
    }
}
