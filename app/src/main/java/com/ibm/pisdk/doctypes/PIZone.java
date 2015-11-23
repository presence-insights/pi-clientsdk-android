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

import android.graphics.Point;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import java.util.ArrayList;

/**
 * Simple class to encapsulate the Zone documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIZone {
    private static final String JSON_CODE = "@code";
    private static final String JSON_NAME = "name";
    private static final String JSON_TAGS = "tags";

    // required
    private String code;
    private String name;
    private ArrayList<ArrayList<Point>> polygons;

    // optional
    private ArrayList<String> tags;

    public PIZone(JSONObject zoneObj) {
        JSONObject geometry = (JSONObject)zoneObj.get("geometry");
        JSONObject properties = (JSONObject)zoneObj.get("properties");

        code = (String) properties.get(JSON_CODE);
        name = (String) properties.get(JSON_NAME);
        polygons = getPolygonsFromJson((JSONArray) geometry.get("coordinates"));

        tags = getTagsFromJson(properties);
    }

    private ArrayList<ArrayList<Point>> getPolygonsFromJson(JSONArray coordinates) {
        ArrayList<ArrayList<Point>> polygons = new ArrayList<ArrayList<Point>>();

        for (int i = 0; i < coordinates.size(); i++) {
            JSONArray polygon = (JSONArray) coordinates.get(i);
            ArrayList<Point> points = new ArrayList<Point>();
            for (int j = 0; j < polygon.size(); j++) {
                JSONArray point = (JSONArray)polygon.get(j);
                points.add(new Point(((Long)point.get(0)).intValue(), ((Long)point.get(1)).intValue()));
            }
            polygons.add(points);
        }

        return polygons;
    }

    private ArrayList<String> getTagsFromJson(JSONObject properties) {
        ArrayList<String> tags = new ArrayList<String>();
        JSONArray tempTags = (JSONArray) properties.get(JSON_TAGS);

        for (int i = 0; i < tempTags.size(); i++) {
            tags.add((String) tempTags.get(i));
        }

        return tags;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ArrayList<ArrayList<Point>> getPolygons() {
        return polygons;
    }

    public ArrayList<String> getTags() {
        return tags;
    }
}
