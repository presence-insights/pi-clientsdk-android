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
 * Simple class to encapsulate the Org documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIOrg {

    private static final String JSON_NAME = "name";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_REGISTRATION_TYPES = "registrationTypes";

    String name;
    String description;
    ArrayList<String> registrationTypes;

    public PIOrg(JSONObject orgObj) {
        name = (String) orgObj.get(JSON_NAME);
        description = orgObj.get(JSON_DESCRIPTION) != null ? (String)orgObj.get(JSON_DESCRIPTION) : "";

        registrationTypes = new ArrayList<String>();
        JSONArray tempTypes = (JSONArray) orgObj.get(JSON_REGISTRATION_TYPES);

        for (int i = 0; i < tempTypes.size(); i++) {
            registrationTypes.add((String) tempTypes.get(i));
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<String> getRegistrationTypes() {
        return registrationTypes;
    }
}
