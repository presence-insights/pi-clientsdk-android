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

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * .
 */
class GeofenceManager implements LocationListener {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(GeofenceManager.class);
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final String GEOFENCES_PREF_KEY = "com.ibm.pisdk.geofencing.geofences";
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final Set<String> GEOFENCES_PREF_DEFAULT = Collections.emptySet();
    /**
     * Mapping of registered geofences to their uuids.
     */
    private final Map<String, PIGeofence> fenceMap = new LinkedHashMap<>();
    private final PIGeofencingService service;
    private Location referenceLocation = null;
    private double maxDistance;

    GeofenceManager(PIGeofencingService service, int maxDistance) {
        this.service = service;
        this.maxDistance = maxDistance;
    }

    public void addFences(Collection<PIGeofence> fences) {
        synchronized (fenceMap) {
            for (PIGeofence fence : fences) {
                fenceMap.put(fence.getCode(), fence);
            }
        }
    }

    public void clearFences() {
        synchronized (fenceMap) {
            List<PIGeofence> list = getFences();
            if ((list != null) && !list.isEmpty()) {
                service.unmonitorGeofences(list);
                fenceMap.clear();
            }
        }
    }

    /**
     * Get all fences in the cache.
     */
    List<PIGeofence> getFences() {
        synchronized (fenceMap) {
            return new ArrayList<>(fenceMap.values());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double d = maxDistance + 1d;
        log.debug("onLocationChanged(location=" + location + ") new location");
        if (referenceLocation != null) d = referenceLocation.distanceTo(location);
        if (d > maxDistance) {
            log.debug("onLocationChanged(location=" + location + ")");
            clearFences();
            // where clause to find all geofences whose center's distance to the new location is < maxDistance
            String where = createWhereClause(location.getLatitude(), location.getLongitude(), maxDistance /2);
            List<PIGeofence> list = PIGeofence.find(PIGeofence.class, where);
            if ((list != null) && !list.isEmpty()) {
                TreeMap<Float, PIGeofence> map = new TreeMap<>();
                for (PIGeofence g : list) {
                    Location l = new Location(LocationManager.NETWORK_PROVIDER);
                    l.setLatitude(g.getLatitude());
                    l.setLongitude(g.getLongitude());
                    map.put(l.distanceTo(location), g);
                }
                int count = 0;
                for (PIGeofence g : map.values()) {
                    fenceMap.put(g.getCode(), g);
                    count++;
                    if (count >= 100) break;
                }
                service.monitorGeofences(getFences());
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
        double EarthRadius = 6371000d; // meters
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
        LatLng pos1 = calculateDerivedPosition(latitude, longitude, radius, 0d);
        LatLng pos2 = calculateDerivedPosition(latitude, longitude, radius, 90d);
        LatLng pos3 = calculateDerivedPosition(latitude, longitude, radius, 180d);
        LatLng pos4 = calculateDerivedPosition(latitude, longitude, radius, 270d);
        return String.format(Locale.US, "latitude < %f AND longitude < %f AND latitude > %f AND longitude > %f", pos1.latitude, pos2.longitude, pos3.latitude, pos4.longitude);
    }

    List<PIGeofence> filterFromPrefs(List<PIGeofence> fences) {
        List<PIGeofence> filtered = new ArrayList<>();
        Set<String> uuids = PreferenceManager.getDefaultSharedPreferences(service.context).getStringSet(GEOFENCES_PREF_KEY, GEOFENCES_PREF_DEFAULT);
        if (uuids == null) {
            uuids = GEOFENCES_PREF_DEFAULT;
        }
        for (PIGeofence fence: fences) {
            if (!uuids.contains(fence.getCode())) {
                filtered.add(fence);
            }
        }
        return filtered;
    }

    void addFencesToPrefs(List<PIGeofence> fences) {
        updatePrefs(fences, false);
    }

    void removeFencesFromPrefs(List<PIGeofence> fences) {
        updatePrefs(fences, true);
    }

    private void updatePrefs(List<PIGeofence> fences, boolean remove) {
        Set<String> uuids = getUuidsFromPrefs();
        for (PIGeofence fence: fences) {
            if (remove) {
                uuids.remove(fence.getCode());
            } else {
                uuids.add(fence.getCode());
            }
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service.context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(GEOFENCES_PREF_KEY, uuids);
        editor.apply();
    }

    private Set<String> getUuidsFromPrefs() {
        Set<String> uuids = PreferenceManager.getDefaultSharedPreferences(service.context).getStringSet(GEOFENCES_PREF_KEY, GEOFENCES_PREF_DEFAULT);
        if ((uuids == null) || (uuids == GEOFENCES_PREF_DEFAULT)) {
            uuids = new HashSet<>();
        }
        return uuids;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }
}
