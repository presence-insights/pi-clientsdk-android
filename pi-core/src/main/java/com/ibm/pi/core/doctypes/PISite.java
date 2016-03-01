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

import com.ibm.json.java.JSONObject;

/**
 * Simple class to encapsulate the Site documents important attributes.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PISite {
    private static final String JSON_CODE = "@code";
    private static final String JSON_NAME = "name";
    private static final String JSON_TIMEZONE = "timeZone";
    private static final String JSON_STREET = "street";
    private static final String JSON_CITY = "city";
    private static final String JSON_STATE = "state";
    private static final String JSON_ZIP = "zip";
    private static final String JSON_COUNTRY = "country";

    // required
    private String code;
    private String name;
    private String timeZone;

    // optional
    private String street;
    private String city;
    private String state;
    private String zip;
    private String country;

    public PISite(JSONObject siteObj) {
        code = (String) siteObj.get(JSON_CODE);
        name = (String) siteObj.get(JSON_NAME);
        timeZone = (String) siteObj.get(JSON_TIMEZONE);

        street = (String) siteObj.get(JSON_STREET);
        city = (String) siteObj.get(JSON_CITY);
        state = (String) siteObj.get(JSON_STATE);
        zip = (String) siteObj.get(JSON_ZIP);
        country = (String) siteObj.get(JSON_COUNTRY);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

    public String getCountry() {
        return country;
    }
}
