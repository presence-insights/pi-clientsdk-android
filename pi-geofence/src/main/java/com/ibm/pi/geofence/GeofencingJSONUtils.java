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

package com.ibm.pi.geofence;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utility methods to parse one or more geofences in geojson format.
 */
class GeofencingJSONUtils {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(PIGeofencingManager.class.getSimpleName());
    /**
     * Date format used to convert dates from/to UTC format such as "2015-08-24T09:00:00-05:00".
     */
    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /**
     * Date format used to convert dates from/to UTC format such as "2015-08-24T09:00:00.123Z".
     */
    private static final String TMP_DESC = UUID.randomUUID().toString();

    /**
     * Parse a list of geofences.
     * @param json json object representing the list of fences.
     * @return a list of {@link PIGeofence} instances.
     * @throws Exception if a parsing error occurs.
     */
    static GeofenceList parseGeofences(JSONObject json) throws Exception {
        List<PersistentGeofence> result = new ArrayList<>();
        JSONArray features = json.getJSONArray("features");
        for (int i=0; i<features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            result.add(parseGeofence(feature));
        }
        if (json.has("properties")) {
            JSONObject properties = json.getJSONObject("properties");
            List<String> deletedCodes = null;
            if (properties.has("deleted")) {
                JSONArray deletedCodesJson = properties.getJSONArray("deleted");
                deletedCodes = new ArrayList<>(deletedCodesJson.length());
                for (int i=0; i<deletedCodesJson.length(); i++) {
                    deletedCodes.add(deletedCodesJson.getString(i));
                }
                if (!deletedCodes.isEmpty()) {
                    int n = GeofencingUtils.deleteGeofences(deletedCodes);
                    log.debug(String.format("deleted %d geofences from local DB", n));
                }
            }
            int totalGeofences = properties.has("totalFeatures") ? properties.getInt("totalFeatures") : -1;
            String lastSyncDate = properties.has("lastSyncDate") ? properties.getString("lastSyncDate") : null;
            return new GeofenceList(result, totalGeofences, lastSyncDate, deletedCodes);
        }
        return new GeofenceList(result);
    }

    /**
     * Parse a single geofence.
     * @param feature json object representing the fence.
     * @return a {@link PIGeofence} instance.
     * @throws Exception if a parsing error occurs.
     */
    static PersistentGeofence parseGeofence(JSONObject feature) throws Exception {
        JSONObject props = feature.getJSONObject("properties");
        String code = props.has("code") ? props.getString("code") : (props.has("@code") ? props.getString("@code") : null);
        JSONObject updatedJSON = props.getJSONObject("@updated");
        long updated = updatedJSON.getLong("timestamp");
        JSONObject createdJSON = props.getJSONObject("@created");
        long created = createdJSON.getLong("timestamp");
        String name = props.has("name") ? props.getString("name") : null;
        String description = props.has("description") ? props.getString("description") : null;
        double radius = props.has("radius") ? props.getDouble("radius") : -1d;
        JSONObject geometry = feature.getJSONObject("geometry");
        JSONArray coord = geometry.getJSONArray("coordinates");
        double lng = coord.getDouble(0);
        double lat = coord.getDouble(1);
        PersistentGeofence geofence = GeofencingUtils.geofenceFromCode(code);
        if (geofence == null) {
            // if not in local DB create a new one
            geofence = new PersistentGeofence(code, name, description, lat, lng, radius);
        } else {
            // update existing geofence
            geofence.setName(name);
            geofence.setDescription(description);
            geofence.setLatitude(lat);
            geofence.setLongitude(lng);
            geofence.setRadius(radius);
        }
        geofence.setCreatedTimestamp(created);
        geofence.setUpdatedTimestamp(updated);
        return geofence;
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
    static JSONObject toJSONGeofence(PIGeofencingManager service, PersistentGeofence fence, boolean isUpdate) {
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
            if (isUpdate) {
                JSONObject updated = new JSONObject();
                properties.put("@updated", updated);
                updated.put("by", service.httpService.getUsername());
                updated.put("timestamp", fence.getUpdatedTimestamp());
            }
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

    /*
    {
      "notifications": [
        {
          "data": {
            "crossingType": "enter",
            "fenceCode": "34c824e9-b5ba-4032-b41e-a81e17fedc89"
          },
          "detectedTime": "2016-02-10T08:54:08+01:00",
          "descriptor": "343487045bbab8e9"
        }
      ],
      "sdkVersion": "1.0.1"
    }
    */
    static JSONObject toJSONGeofenceEvent(List<PIGeofence> fences, GeofenceNotificationType type, String deviceID, String sdkVersion) {
        JSONObject json = new JSONObject();
        try {
            json.put("sdkVersion", sdkVersion == null ? "" : sdkVersion);
            JSONArray notifications = new JSONArray();
            json.put("notifications", notifications);
            String date = formatDate(new Date());
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
            json.put("descriptor", TMP_DESC);
            //json.put("descriptor", deviceID);
            json.put("detectedTime", date);
            JSONObject data = new JSONObject();
            json.put("data", data);
            data.put("geofenceCode", fence.getCode());
            data.put("geofenceName", fence.getName());
            data.put("crossingType", type.operation());
        } catch(JSONException e) {
            log.error("exception generating json for geofence list", e);
        }
        return json;
    }

    static PersistentGeofence updateGeofenceTimestampsFromJSON(PersistentGeofence geofence, JSONObject json) {
        try {
            JSONObject properties = json.getJSONObject("properties");
            JSONObject created = properties.getJSONObject("@created");
            long createdTImestamp = created.getLong("timestamp");
            geofence.setCreatedTimestamp(createdTImestamp);
            long updatedTimestamp = 0L;
            if (properties.has("@updated")) {
                JSONObject updated = properties.getJSONObject("@updated");
                updatedTimestamp = updated.getLong("timestamp");
            }
            if (updatedTimestamp <= 0d) {
                updatedTimestamp = createdTImestamp;
            }
            geofence.setUpdatedTimestamp(updatedTimestamp);
        } catch(JSONException e) {
            log.error("exception geofence timestamps for " + json, e);
        }
        return geofence;
    }

    static String formatDate(Date date) {
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }
}
