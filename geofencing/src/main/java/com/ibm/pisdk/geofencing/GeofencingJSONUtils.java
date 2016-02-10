/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.pisdk.geofencing;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility methods to parse one or more geofences in geojson format.
 */
public class GeofencingJSONUtils {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(PIGeofencingService.class);
    /**
     * Date format used to convert dates from/to UTC format such as "2015-08-24T09:00:00-05:00".
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";

    /**
     * Parse a list of geofences.
     * @param json json object representing the list of fences.
     * @return a list of {@link PIGeofence} instances.
     * @throws Exception if a parsing error occurs.
     */
    public static PIGeofenceList parseGeofences(JSONObject json) throws Exception {
        List<PIGeofence> result = new ArrayList<>();
        JSONObject geojson = json.getJSONObject("geojson");
        JSONArray features = geojson.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            result.add(parseGeofence(feature));
        }
        JSONObject properties = null;
        if (json.has("properties")) {
            properties = json.getJSONObject("properties");
            int pageNumber = properties.has("page") ? properties.getInt("page") : -1;
            int pageSize = properties.has("pageSize") ? properties.getInt("pageSize") : -1;
            int totalGeofences = properties.has("totalFeatures") ? properties.getInt("totalFeatures") : -1;
            return new PIGeofenceList(result, pageNumber, pageSize, totalGeofences);
        }
        return new PIGeofenceList(result);
    }

    /**
     * Parse a single geofence.
     * @param feature json object representing the fence.
     * @return a {@link PIGeofence} instance.
     * @throws Exception if a parsing error occurs.
     */
    static PIGeofence parseGeofence(JSONObject feature) throws Exception {
        JSONObject geometry = feature.getJSONObject("geometry");
        JSONArray coord = geometry.getJSONArray("coordinates");
        double lng = coord.getDouble(0);
        double lat = coord.getDouble(1);
        JSONObject props = feature.getJSONObject("properties");
        String code = props.has("code") ? props.getString("code") : null;
        String name = props.has("name") ? props.getString("name") : null;
        String description = props.has("description") ? props.getString("description") : null;
        double radius = props.has("radius") ? props.getDouble("radius") : -1d;
        return  new PIGeofence(code, name, description, lat, lng, radius);
    }

    /**
     * Parse a single geofence.
     * @param feature json object representing the fence.
     * @return a {@link PIGeofence} instance.
     * @throws Exception if a parsing error occurs.
     */
    static PIGeofence parsePostGeofenceResponse(JSONObject feature) throws Exception {
        JSONObject geometry = feature.getJSONObject("geometry");
        JSONArray coord = geometry.getJSONArray("coordinates");
        double lng = coord.getDouble(0);
        double lat = coord.getDouble(1);
        JSONObject props = feature.getJSONObject("properties");
        String code = props.has("@code") ? props.getString("@code") : null;
        String name = props.has("name") ? props.getString("name") : null;
        String description = props.has("description") ? props.getString("description") : null;
        double radius = props.has("radius") ? props.getDouble("radius") : -1d;
        return  new PIGeofence(code, name, description, lat, lng, radius);
    }

    /*
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [ 10.1, 12.4 ]
      },
      "properties": {
        "name": "France lab",
        "description": "string",
        "radius": 200
      }
    }
    */
    static JSONObject toJSONGeofence(PIGeofence fence) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "Feature");
            JSONObject geometry = new JSONObject();
            json.put("geometry", geometry);
            geometry.put("type", "Point");
            JSONArray coord = new JSONArray();
            geometry.put("coordinates", coord);
            coord.put(fence.getLongitude());
            coord.put(fence.getLatitude());
            JSONObject properties = new JSONObject();
            json.put("properties", properties);
            properties.put("name", fence.getName());
            properties.put("description", fence.getDescription() == null ? "" : fence.getDescription());
            properties.put("radius", fence.getRadius());
        } catch(JSONException e) {
            log.error("exception generating json for geofence " + fence, e);
        }
        return json;
    }

    /*
    {
      "name": "string",
      "registrationTypes": [ "Internal" ],
      "description": "string",
      "publicKey": "-----BEGIN PUBLIC KEY-----blabhblahblah=-----END PUBLIC KEY-----"
    }
    */
    static JSONObject toJSONPostOrg(String name, String description, String publicKey) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name).put("description", description).put("publicKey", publicKey);
            JSONArray regTypes = new JSONArray();
            json.put("registrationTypes", regTypes.put("Internal"));
        } catch(JSONException e) {
            log.error("exception generating json for org " + name, e);
        }
        return json;
    }

    /*
    {
        "name": "string",
        "registrationTypes": ["Internal"],
        "description": "string",
        "publicKey": "-----BEGIN PUBLIC KEY-----blahblahblah=-----END PUBLIC KEY-----",
        "@docType": "org",
        "@updated": {
            "by": "0f90fsy",
            "timestamp": 1453976376453
        },
        "@created": {
            "by": "0f90fsy",
            "timestamp": 1453976376453
        },
        "@tenant": "xf504jy",
        "@code": "ob6ppdp",
        "@sites": "http://localhost:3000/pi-config/v2/tenants/xf504jy/orgs/ob6ppdp/sites",
        "@devices": "http://localhost:3000/pi-config/v2/tenants/xf504jy/orgs/ob6ppdp/devices",
        "@geofences": "http://localhost:3000/pi-config/v2/tenants/xf504jy/orgs/ob6ppdp/geofences"
    }
    */
    static String parsePostOrgResponse(JSONObject json) {
        try {
        return json.getString("@code");
        } catch(Exception e) {
            log.error("exception parsing json for org: ", e);
        }
        return null;
    }

    static JSONObject toJSONGeofenceEvent(List<PIGeofence> fences, GeofenceNotificationType type, String deviceID) {
        JSONObject json = new JSONObject();
        try {
            JSONArray notifications = new JSONArray();
            json.put("notifications", notifications);
            String date = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            for (PIGeofence fence: fences) {
                notifications.put(toJSONGeofenceEvent(fence, deviceID, date, type));
            }
        } catch(JSONException e) {
            log.error("exception generating json for geofence list", e);
        }
        return json;
    }

    static JSONObject toJSONGeofenceEvent(PIGeofence fence, String deviceID, String date, GeofenceNotificationType type) {
        JSONObject json = new JSONObject();
        try {
            json.put("descriptor", deviceID);
            json.put("detectedTime", date);
            JSONObject data = new JSONObject();
            json.put("data", data);
            data.put("fenceCode", fence.getCode());
            data.put("crossingType", type.operation());
        } catch(JSONException e) {
            log.error("exception generating json for geofence list", e);
        }
        return json;
    }
}
