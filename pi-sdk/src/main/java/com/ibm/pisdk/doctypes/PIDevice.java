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
import com.ibm.pisdk.PILogger;

/**
 * Simple class to encapsulate the Device documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIDevice {

    private static final String JSON_CODE = "@code";
    private static final String JSON_NAME = "name";
    private static final String JSON_REGISTERED = "registered";
    private static final String JSON_DESCRIPTOR = "descriptor";
    private static final String JSON_DESCRIPTOR_TYPE = "@descriptorType";
    private static final String JSON_REGISTRATION_TYPE = "registrationType";
    private static final String JSON_DATA = "data";
    private static final String JSON_UNENCRYPTED_DATA = "unencryptedData";
    private static final String JSON_BLACKLIST = "blacklist";

    private String code = "";
    private String name = "";
    private String descriptor = "";
    private String descriptorType = "";
    private String registrationType = "";
    private JSONObject data = new JSONObject();
    private JSONObject unencryptedData = new JSONObject();
    private boolean registered = false;
    private boolean blacklisted = false;

    // raw JSON object
    private JSONObject rawData;

    public PIDevice(JSONObject deviceObj) {
        code = (String) deviceObj.get(JSON_CODE);
        descriptor = (String) deviceObj.get(JSON_DESCRIPTOR);
        registered = (Boolean) deviceObj.get(JSON_REGISTERED);

        if (registered) {
            name = (String) deviceObj.get(JSON_NAME);
            descriptorType = (String) deviceObj.get(JSON_DESCRIPTOR_TYPE);
            registrationType = (String) deviceObj.get(JSON_REGISTRATION_TYPE);
            data = (JSONObject) deviceObj.get(JSON_DATA);
            unencryptedData = (JSONObject) deviceObj.get(JSON_UNENCRYPTED_DATA);
            blacklisted = (Boolean) deviceObj.get(JSON_BLACKLIST);
        }

        rawData = deviceObj;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getDescriptorType() {
        return descriptorType;
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public JSONObject getData() {
        return data;
    }

    public JSONObject getUnencryptedData() {
        return unencryptedData;
    }

    public boolean isRegistered() {
        return registered;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public JSONObject getRawData() {
        return rawData;
    }
}
