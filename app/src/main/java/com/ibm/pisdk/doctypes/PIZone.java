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

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import java.util.ArrayList;

/**
 * Simple class to encapsulate the Zone documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIZone {
    private static final String JSON_NAME = "name";
    private static final String JSON_X = "x";
    private static final String JSON_Y = "y";
    private static final String JSON_WIDTH = "width";
    private static final String JSON_HEIGHT = "height";
    private static final String JSON_TAGS = "tags";

    // required
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;

    // optional
    private ArrayList<String> tags;

    public PIZone(JSONObject zoneObj) {
        name = (String) zoneObj.get(JSON_NAME);
        x = (Integer) zoneObj.get(JSON_X);
        y = (Integer) zoneObj.get(JSON_Y);
        width = (Integer) zoneObj.get(JSON_WIDTH);
        height = (Integer) zoneObj.get(JSON_HEIGHT);

        tags = new ArrayList<String>();
        JSONArray tempTags = (JSONArray) zoneObj.get(JSON_TAGS);

        for (int i = 0; i < tempTags.size(); i++) {
            tags.add((String) tempTags.get(i));
        }
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ArrayList<String> getTags() {
        return tags;
    }
}
