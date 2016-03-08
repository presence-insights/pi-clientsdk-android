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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

/**
 * .
 */
//public class GeofenceManager extends IntentService {
public class GeofenceManager extends BroadcastReceiver {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(GeofenceManager.class);
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final String GEOFENCES_PREF_KEY = "com.ibm.pi.geofences";
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final Set<String> GEOFENCES_PREF_DEFAULT = Collections.emptySet();
    /**
     * .
     */
    private static final String REFERENCE_LOCATION_LAT = "com.ibm.pi.ref_lat";
    private static final String REFERENCE_LOCATION_LNG = "com.ibm.pi.ref_lng";
    private PIGeofencingService geofencingService;
    private Location referenceLocation = null;
    private double maxDistance;
    private Settings settings;
    private ServiceConfig config;

    public GeofenceManager() {
        //this(GeofenceManager.class.getName());
    }

    /*
    public GeofenceManager(String name) {
        super(name);
    }
    */

    public GeofenceManager(PIGeofencingService geofencingService) {
        this();
        this.geofencingService = geofencingService;
        this.settings = geofencingService.settings;
        this.maxDistance = geofencingService.maxDistance;
        this.referenceLocation = retrieveReferenceLocation();
        this.config = new ServiceConfig().fromGeofencingService(geofencingService);
        log.debug(String.format("config=%s, settings=%s", config, settings));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean shouldProcess = intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED) != null;
        if (shouldProcess) {
            if (geofencingService == null) {
                this.config = new ServiceConfig().fromIntent(intent);
                log.debug("onReceive() config=" + config);
                this.maxDistance = config.maxDistance;
                log.debug("onReceive() settings=" + settings);
                this.geofencingService = new PIGeofencingService(PIGeofencingService.MODE_SERVICE, null, null, context,
                    config.serverUrl, config.tenantCode, config.orgCode, config.username, config.password, (int) config.maxDistance);
                this.settings = geofencingService.settings;
                this.referenceLocation = retrieveReferenceLocation();
            }
            Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
            onLocationChanged(location, false);
        } else {
            //log.debug("no location result");
        }
    }

    /*
    @Override
    protected void onHandleIntent(Intent intent) {
        boolean shouldProcess = LocationResult.hasResult(intent) || (intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED) != null);
        if (shouldProcess) {
            if (geofencingService == null) {
                this.config = new ServiceConfig().fromIntent(intent);
                log.debug("onHandleIntent() config=" + config);
                Context ctx = config.createContext(this);
                //Context ctx = getApplicationContext();
                this.maxDistance = config.maxDistance;
                log.debug("onHandleIntent() settings=" + settings);
                this.geofencingService = new PIGeofencingService(PIGeofencingService.MODE_SERVICE, null, null, ctx,
                    config.serverUrl, config.tenantCode, config.orgCode, config.username, config.password, (int) config.maxDistance);
                this.settings = geofencingService.settings;
                this.referenceLocation = retrieveReferenceLocation();
            }
            // doesn't work due to android issue https://code.google.com/p/android/issues/detail?id=197296
            //LocationResult result = LocationResult.extractResult(intent);
            //Location location = result.getLastLocation();
            Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
            onLocationChanged(location, false);
        } else {
            //log.debug("no location result");
        }
    }
    */

    void onLocationChanged(Location location, boolean initial) {
        double d = maxDistance + 1d;
        log.debug("onLocationChanged(location=" + location + ") new location");
        if (referenceLocation != null) {
            d = referenceLocation.distanceTo(location);
        }
        if (d > maxDistance) {
            // where clause to find all geofences whose center's distance to the new location is < maxDistance
            String where = createWhereClause(location.getLatitude(), location.getLongitude(), maxDistance /2);
            List<PIGeofence> bboxFences = PIGeofence.find(PIGeofence.class, where);
            log.debug(String.format("where clause=%s, found fences: %s", where, bboxFences));
            TreeMap<Float, PIGeofence> map = new TreeMap<>();
            if ((bboxFences != null) && !bboxFences.isEmpty()) {
                for (PIGeofence g : bboxFences) {
                    Location l = new Location(LocationManager.NETWORK_PROVIDER);
                    l.setLatitude(g.getLatitude());
                    l.setLongitude(g.getLongitude());
                    float distance = l.distanceTo(location);
                    map.put(distance, g);
                }
            }
            int count = 0;
            bboxFences = new ArrayList<>(map.size());
            for (PIGeofence g : map.values()) {
                bboxFences.add(g);
                count++;
                if (count >= 100) break;
            }
            List<PIGeofence> monitoredFences = extractGeofences(settings);
            List<PIGeofence> toAdd = new ArrayList<>();
            List<PIGeofence> toRemove = new ArrayList<>();
            for (PIGeofence fence: bboxFences) {
                if (!monitoredFences.contains(fence)) {
                    toAdd.add(fence);
                }
            }
            for (PIGeofence fence: monitoredFences) {
                if (!bboxFences.contains(fence)) {
                    toRemove.add(fence);
                }
            }
            geofencingService.unmonitorGeofences(toRemove);
            geofencingService.monitorGeofences(toAdd);
            updateGeofences(settings, bboxFences);
            referenceLocation = location;
            storeReferenceLocation(referenceLocation);
            log.debug("committing settings=" + settings);
            settings.commit();
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

    private Location retrieveReferenceLocation() {
        double lat = settings.getDouble(REFERENCE_LOCATION_LAT, -1d);
        double lng = settings.getDouble(REFERENCE_LOCATION_LNG, -1d);
        Location location = new Location(LocationManager.NETWORK_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }

    private void storeReferenceLocation(Location location) {
        settings.putDouble(REFERENCE_LOCATION_LAT, location.getLatitude());
        settings.putDouble(REFERENCE_LOCATION_LNG, location.getLongitude());
    }

    /**
     * Extract the codes of all monitored geofences from the settings.
     */
    static Collection<String> getUuidsFromPrefs(Settings settings) {
        Collection<String> uuids = settings.getStrings(GEOFENCES_PREF_KEY, GEOFENCES_PREF_DEFAULT);
        if ((uuids == null) || (uuids == GEOFENCES_PREF_DEFAULT)) {
            uuids = new HashSet<>();
        }
        return uuids;
    }

    /**
     * Extract the monitored geofences from the settings.
     */
    static List<PIGeofence> extractGeofences(Settings settings) {
        Collection<String> geofenceCodes = getUuidsFromPrefs(settings);
        List<PIGeofence> geofences = new ArrayList<>(geofenceCodes.size());
        for (String code: geofenceCodes) {
            List<PIGeofence> list = PIGeofence.find(PIGeofence.class, "code = ?", code);
            if (!list.isEmpty()) {
                geofences.add(list.get(0));
            }
        }
        return geofences;
    }

    static void updateGeofences(Settings settings, Collection<PIGeofence> fences) {
        if (fences != null) {
            List<String> fenceCodes = new ArrayList<>(fences.size());
            for (PIGeofence fence: fences) {
                fenceCodes.add(fence.getCode());
            }
            settings.putStrings(GEOFENCES_PREF_KEY, fenceCodes);
        }
    }

    /*
    @Override
    public boolean equals(Object o) {
        if (o != null) {
            return o.getClass() == GeofenceManager.class;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    */
}
