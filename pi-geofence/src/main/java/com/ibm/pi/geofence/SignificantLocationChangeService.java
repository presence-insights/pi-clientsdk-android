/**
 * Copyright (c) 2015, 2016 IBM Corporation. All rights reserved.
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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * This service handles significant location changes.
 * <p>A new bounding box is computed based on the new location and the maxDistance.
 * Geofences in the new bounding box are registered monitoring on the device,
 * whereas registered geofences that no longer fit in the bounding box are unregistered,
 * all within the 100 geofences iimit.
 */
public class SignificantLocationChangeService extends IntentService {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(SignificantLocationChangeService.class.getSimpleName());
    private PIGeofencingManager mGeofencingService;
    private Settings mSettings;
    private ServiceConfig mConfig;

    public SignificantLocationChangeService() {
        super(SignificantLocationChangeService.class.getSimpleName());
    }

    public SignificantLocationChangeService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean locationUpdate = intent.getExtras().get(ServiceConfig.LOCATION_UPDATE_FLAG) != null;
        if (locationUpdate) {
            mConfig = new ServiceConfig().fromIntent(intent);
            log.debug("onHandleIntent() config=" + mConfig);
            if (mGeofencingService == null) {
                Context ctx = mConfig.createContext(this);
                this.mSettings = new Settings(ctx);
                this.mGeofencingService = new PIGeofencingManager(mSettings, PIGeofencingManager.MODE_MONITORING_REQUEST, ctx,
                    mConfig.mServerUrl, mConfig.mTenantCode, mConfig.mOrgCode, mConfig.mUsername, mConfig.mPassword, (int) mConfig.mMaxDistance);
                log.debug("onHandleIntent() settings=" + mSettings);
            }
            mConfig.populateFromSettings(mSettings);
            Location location = new Location(LocationManager.NETWORK_PROVIDER);
            location.setLatitude(mConfig.mNewLocation.latitude);
            location.setLongitude(mConfig.mNewLocation.longitude);
            processNewLocation(location);
        } else if (intent.getBooleanExtra(ServiceConfig.REBOOT_EVENT_FLAG, false)) {
            try {
                ServiceConfig config = new ServiceConfig().fromIntent(intent);
                log.debug("onHandleIntent(reboot_event) config=" + config);
                Context context = config.createContext(this);
                Settings settings = new Settings(context);
                config.populateFromSettings(settings);
                PIGeofencingManager geofencingService = new PIGeofencingManager(settings, PIGeofencingManager.MODE_REBOOT, context,
                    config.mServerUrl, config.mTenantCode, config.mOrgCode, config.mUsername, config.mPassword, (int) config.mMaxDistance);
                List<PersistentGeofence> geofences = GeofencingUtils.extractGeofences(settings);
                geofencingService.monitorGeofences(geofences);
            } catch(Exception e) {
                log.error("error handling post-reboot remonitoring", e);
            }
        }
    }

    /**
     * Computes a new bounding box based on the specified location and the {@code maxDistance} value.
     * Retrieves from the local DB the first 100 geofences nearest to the location and registers them if needed for monitoring.
     * @param location he center of the new bounding box.
     */
    void processNewLocation(Location location) {
        // where clause to find all geofences whose distance to the new location is < maxDistance
        String where = createWhereClause(location.getLatitude(), location.getLongitude(), mConfig.mMaxDistance / 2);
        List<PersistentGeofence> bboxFences = PersistentGeofence.find(PersistentGeofence.class, where);
        log.debug(String.format("where clause=%s, found fences: %s", where, bboxFences));
        TreeMap<Float, PersistentGeofence> map = new TreeMap<>();
        if ((bboxFences != null) && !bboxFences.isEmpty()) {
            for (PersistentGeofence g : bboxFences) {
                Location l = new Location(LocationManager.NETWORK_PROVIDER);
                l.setLatitude(g.getLatitude());
                l.setLongitude(g.getLongitude());
                float distance = l.distanceTo(location);
                map.put(distance, g);
            }
        }
        int count = 0;
        bboxFences = new ArrayList<>(map.size());
        for (PersistentGeofence g : map.values()) {
            bboxFences.add(g);
            count++;
            if (count >= 100) break;
        }
        List<PersistentGeofence> monitoredFences = GeofencingUtils.extractGeofences(mSettings);
        List<PersistentGeofence> toAdd = new ArrayList<>();
        List<PersistentGeofence> toRemove = new ArrayList<>();
        for (PersistentGeofence fence: bboxFences) {
            if (!monitoredFences.contains(fence)) {
                toAdd.add(fence);
            }
        }
        for (PersistentGeofence fence: monitoredFences) {
            if (!bboxFences.contains(fence)) {
                toRemove.add(fence);
            }
        }
        mGeofencingService.unmonitorGeofences(toRemove);
        mGeofencingService.monitorGeofences(toAdd);
        GeofencingUtils.updateGeofences(mSettings, bboxFences);
        GeofencingUtils.storeReferenceLocation(mSettings, location);
        log.debug("committing settings=" + mSettings);
        mSettings.commit();
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
        return String.format(Locale.US, "m_latitude < %f AND m_longitude < %f AND m_latitude > %f AND m_longitude > %f", pos1.latitude, pos2.longitude, pos3.latitude, pos4.longitude);
    }
}
