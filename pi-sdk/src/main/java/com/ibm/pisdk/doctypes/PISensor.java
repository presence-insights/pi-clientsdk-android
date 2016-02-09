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
    private double threshold;
    private double x;
    private double y;

    // optional
    private String description;

    public PISensor(JSONObject sensorObj) {
        JSONObject geometry = (JSONObject)sensorObj.get("geometry");
        JSONObject properties = (JSONObject)sensorObj.get("properties");

        code = (String) properties.get(JSON_CODE);
        name = (String) properties.get(JSON_NAME);
        description = properties.get(JSON_DESCRIPTION) != null ? (String)properties.get(JSON_DESCRIPTION) : "";
        threshold = objToDouble(properties.get(JSON_THRESHOLD));

        JSONArray coordinates = (JSONArray) geometry.get("coordinates");
        x = objToDouble(coordinates.get(0));
        y = objToDouble(coordinates.get(1));
    }

    private double objToDouble(Object obj) {
        double returnVal = 0.0;
        if(obj instanceof Double) {
            returnVal = (Double) obj;
        } else if(obj instanceof Long) {
            returnVal = ((Long) obj).doubleValue();
        }
        return returnVal;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getThreshold() {
        return threshold;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getDescription() {
        return description;
    }
}
