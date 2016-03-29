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

package com.ibm.pi.core.doctypes;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Simple class to encapsulate the Beacon documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIBeacon {
    private static final String JSON_CODE = "@code";
    private static final String JSON_NAME = "name";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_MAJOR = "major";
    private static final String JSON_MINOR = "minor";
    private static final String JSON_PROXIMITY_UUID = "proximityUUID";
    private static final String JSON_THRESHOLD = "threshold";

    // required
    private String code;
    private String name;
    private String proximityUUID;
    private String major;
    private String minor;
    private double threshold;
    private double x;
    private double y;

    // optional
    private String description;

    public PIBeacon(JSONObject beaconObj) {
        JSONObject geometry = (JSONObject)beaconObj.get("geometry");
        JSONObject properties = (JSONObject)beaconObj.get("properties");

        code = (String) properties.get(JSON_CODE);
        name = (String) properties.get(JSON_NAME);
        description = properties.get(JSON_DESCRIPTION) != null ? (String)properties.get(JSON_DESCRIPTION) : "";
        proximityUUID = (String) properties.get(JSON_PROXIMITY_UUID);
        major = (String) properties.get(JSON_MAJOR);
        minor = (String) properties.get(JSON_MINOR);
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

    public String getProximityUUID() {
        return proximityUUID;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
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
