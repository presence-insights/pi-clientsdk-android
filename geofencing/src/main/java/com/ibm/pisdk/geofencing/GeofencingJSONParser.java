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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods to parse one or more geofences in geojson format.
 */
public class GeofencingJSONParser {
    /**
     * Parse a list of geofences.
     * @param json json object representing the list of fences.
     * @return a list of {@link PIGeofence} instances.
     * @throws Exception if a parsing error occurs.
     */
    public static PIGeofenceList parseGeofences(JSONObject json) throws Exception {
        List<PIGeofence> result = new ArrayList<PIGeofence>();
        int anchor = json.getInt("anchor");
        JSONObject geojson = json.getJSONObject("geojson");
        JSONArray features = geojson.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            result.add(parseGeofence(feature));
        }
        return new PIGeofenceList(result, anchor);
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
        String uuid = props.has("uuid") ? props.getString("uuid") : null;
        String name = props.has("name") ? props.getString("name") : null;
        double radius = props.has("radius") ? props.getDouble("radius") : -1d;
        String msgIn = props.has("messageIn") ? props.getString("messageIn") : null;
        String msgOut = props.has("messageOut") ? props.getString("messageOut") : null;
        String entryDate = props.has("entryDate") ? props.getString("entryDate") : null;
        PIGeofence g = new PIGeofence(uuid, name, lat, lng, radius, msgIn, msgOut, entryDate);
        if (props.has("lastEnterDateAndTime")) g.setLastEnterDateAndTime(props.getString("lastEnterDateAndTime"));
        if (props.has("lastExitDateAndTime")) g.setLastExitDateAndTime(props.getString("lastExitDateAndTime"));
        return g;
    }

    /**
     * Parse a single geofence visit returned in repsonse to a in/out notification request.
     * <p>It has the following JSON format:
     * <pre>
     * {
     *   "action": "in or out",
     *   "timestamp": "2015-07-31T07:10:50.155Z"
     * }</pre>
     * @param visits json object representing the fence.
     * @return a {@link PIGeofence} instance.
     * @throws Exception if a parsing error occurs.
     */
    static PIGeofence parseGeofenceVisitRecord(JSONObject visits) throws Exception {
    /*
    String appId = visitRecord.getString("application");
    String user = visitRecord.getString("user");
    String lastUpdate = visitRecord.getString("lastUpdate");
    */
        JSONObject visit = visits.getJSONObject("visits");
        GeofenceNotificationType action = GeofenceNotificationType.fromString(visit.getString("action"));
        String timestamp = visit.getString("timestamp");
        PIGeofence g = new PIGeofence(null, null, -1d, -1d, -1d, null, null, null);
        switch (action) {
            case IN:
                g.setLastEnterDateAndTime(timestamp);
                break;
            case OUT:
                g.setLastExitDateAndTime(timestamp);
                break;
        }
        return g;
    }
}
