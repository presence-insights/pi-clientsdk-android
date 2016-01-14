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

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * .
 */
class GeofenceManager implements LocationListener {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = GeofenceManager.class.getSimpleName();
    /**
     * Mapping of registered geofences to their uuids.
     */
    private final Map<String, PIGeofence> fenceMap = new LinkedHashMap<String, PIGeofence>();
    private final PIGeofencingService service;
    private Location referenceLocation = null;
    private double distanceThreshold;

    GeofenceManager(PIGeofencingService service) {
        this.service = service;
        distanceThreshold = 500d;
    }

    /**
     * Update a geofence with the latest update from the server.
     * @param fence the geofence to update.
     */
    public void updateGeofence(PIGeofence fence) {
        PIGeofence g = getGeofence(fence.getUuid());
        if (g == null) {
            g = fence;
            synchronized (fenceMap) {
                fenceMap.put(g.getUuid(), g);
            }
        }
        // update the last entry date if it was updated
        String time = fence.getLastEnterDateAndTime();
        if (time != null) {
            g.setLastEnterDateAndTime(time);
        }
        // update the last exit date if it was updated
        time = fence.getLastExitDateAndTime();
        if (time != null) {
            g.setLastExitDateAndTime(time);
        }
    }

    /**
     * Get a geofence from the cache.
     * @param uuid the uuid of the fence to get.
     */
    public PIGeofence getGeofence(String uuid) {
        synchronized (fenceMap) {
            return fenceMap.get(uuid);
        }
    }

    public void addFences(Collection<PIGeofence> fences) {
        synchronized (fenceMap) {
            for (PIGeofence fence : fences) {
                fenceMap.put(fence.getUuid(), fence);
            }
        }
    }

    public void clearFences() {
        synchronized (fenceMap) {
            List<PIGeofence> list = getFences();
            if ((list != null) && !list.isEmpty()) {
                service.removeGeofences(list);
                fenceMap.clear();
            }
        }
    }

    /**
     * Get all fences in the cache.
     */
    List<PIGeofence> getFences() {
        synchronized (fenceMap) {
            return new ArrayList<PIGeofence>(fenceMap.values());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double d = distanceThreshold + 1d;
        if (referenceLocation != null) d = referenceLocation.distanceTo(location);
        if (d > distanceThreshold) {
            Log.v(LOG_TAG, "onLocationChanged(location=" + location + ")");
            clearFences();
            // where clause to find all geofences whose center's distance to the new location is < distanceThreshold
            String where = createWhereClause(location.getLatitude(), location.getLongitude(), distanceThreshold);
            List<PIGeofence> list = PIGeofence.find(PIGeofence.class, where);
            if ((list != null) && !list.isEmpty()) {
                TreeMap<Float, PIGeofence> map = new TreeMap<Float, PIGeofence>();
                for (PIGeofence g : list) {
                    Location l = new Location(LocationManager.NETWORK_PROVIDER);
                    l.setLatitude(g.getLatitude());
                    l.setLongitude(g.getLongitude());
                    map.put(l.distanceTo(location), g);
                }
                int count = 0;
                for (PIGeofence g : map.values()) {
                    fenceMap.put(g.getUuid(), g);
                    count++;
                    if (count >= 100) break;
                }
                service.addGeofences(getFences());
            }
            referenceLocation = location;
        }
    }

    /**
     * Calculate the coordinates of a point located at the specified distance on the specified bearing.
     * @param latitude the source point latitude.
     * @param longitude the source point longitude.
     * @param distance the distance to the source.
     * @param bearing direction in which to go for the target point, expressed in degrees.
     * @return a computed {@link LatLng} instance.
     */
    private LatLng calculateDerivedPosition(double latitude, double longitude, double distance, double bearing) {
        double latitudeRadians = Math.toRadians(latitude);
        double longitudeRadians = Math.toRadians(longitude);
        double EarthRadius = 6371000; // meters
        double angularDistance = distance / EarthRadius;
        double trueCourse = Math.toRadians(bearing);
        double lat = Math.asin(Math.sin(latitudeRadians) * Math.cos(angularDistance) + Math.cos(latitudeRadians) * Math.sin(angularDistance) * Math.cos(trueCourse));
        double dlon = Math.atan2(Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latitudeRadians), Math.cos(angularDistance) - Math.sin(latitudeRadians) * Math.sin(lat));
        double lon = ((longitudeRadians + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;
        return new LatLng(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    /**
     * Create a where clause to find the geofences closest to the specified location and in the specified range.
     */
    private String createWhereClause(double latitude, double longitude, double radius) {
        LatLng pos1 = calculateDerivedPosition(latitude, longitude, radius, 0);
        LatLng pos2 = calculateDerivedPosition(latitude, longitude, radius, 90);
        LatLng pos3 = calculateDerivedPosition(latitude, longitude, radius, 180);
        LatLng pos4 = calculateDerivedPosition(latitude, longitude, radius, 270);
        return String.format(Locale.US, "latitude < %f AND longitude < %f AND latitude > %f AND longitude > %f", pos1.latitude, pos2.longitude, pos3.latitude, pos4.longitude);
    }
}
