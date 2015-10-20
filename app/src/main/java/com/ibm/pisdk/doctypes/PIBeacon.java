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

package com.ibm.pisdk.doctypes;

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
    private static final String JSON_X = "x";
    private static final String JSON_Y = "y";
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
    private long threshold;
    private long x;
    private long y;

    // optional
    private String description;

    public PIBeacon(JSONObject beaconObj) {
        code = (String) beaconObj.get(JSON_CODE);
        name = (String) beaconObj.get(JSON_NAME);
        proximityUUID = (String) beaconObj.get(JSON_PROXIMITY_UUID);
        major = (String) beaconObj.get(JSON_MAJOR);
        minor = (String) beaconObj.get(JSON_MINOR);
        threshold = (Long) beaconObj.get(JSON_THRESHOLD);
        x = (Long) beaconObj.get(JSON_X);
        y = (Long) beaconObj.get(JSON_Y);

        description = beaconObj.get(JSON_DESCRIPTION) != null ? (String)beaconObj.get(JSON_DESCRIPTION) : "";
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
