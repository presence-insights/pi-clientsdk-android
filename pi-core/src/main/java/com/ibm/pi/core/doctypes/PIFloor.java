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

import android.graphics.Point;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Simple class to encapsulate the Floor documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIFloor {
    private static final String JSON_CODE = "@code";
    private static final String JSON_NAME = "name";
    private static final String JSON_Z = "z";

    // required
    private String code;
    private String name;
    private long z;
    private Point barriers;

    public PIFloor(JSONObject floorObj) {
        JSONObject geometry = (JSONObject)floorObj.get("geometry");
        JSONObject properties = (JSONObject)floorObj.get("properties");

        code = (String) properties.get(JSON_CODE);
        name = (String) properties.get(JSON_NAME);
        z = (Long) properties.get(JSON_Z);

        JSONArray coordinates = (JSONArray) geometry.get("coordinates");
        barriers = new Point(((Long) coordinates.get(0)).intValue(),
                             ((Long) coordinates.get(1)).intValue());
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getZ() {
        return z;
    }

    // Currently not implemented. This is a place holder for future work.
    public Point getBarriers() {
        return barriers;
    }
}
