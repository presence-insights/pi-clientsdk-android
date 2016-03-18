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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Receives location change events and determines whether they are significant changes, that is,
 * whether the new location is outside the current bounding box.
 */
public class GeofenceManager extends BroadcastReceiver {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(GeofenceManager.class.getSimpleName());
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final String GEOFENCES_PREF_KEY = "com.ibm.pi.geofences";
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final Set<String> GEOFENCES_PREF_DEFAULT = Collections.emptySet();
    /**
     * Settings key for the last reference location.
     */
    private static final String REFERENCE_LOCATION_LAT = "com.ibm.pi.ref_lat";
    private static final String REFERENCE_LOCATION_LNG = "com.ibm.pi.ref_lng";
    private Context context;
    private Location referenceLocation = null;
    private double maxDistance;
    private Settings settings;
    private ServiceConfig config;

    public GeofenceManager() {
    }

    public GeofenceManager(PIGeofencingService geofencingService) {
        this();
        this.context = geofencingService.context;
        this.settings = geofencingService.settings;
        this.maxDistance = geofencingService.maxDistance;
        this.referenceLocation = retrieveReferenceLocation(settings);
        this.config = new ServiceConfig().fromGeofencingService(geofencingService);
        //log.debug(String.format("GeofenceManager() config=%s, settings=%s", config, settings));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean shouldProcess = intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED) != null;
        if (shouldProcess) {
            this.config = new ServiceConfig().fromIntent(intent);
            this.context = context;
            this.maxDistance = config.maxDistance;
            this.settings = new Settings(context);
            this.referenceLocation = retrieveReferenceLocation(settings);
            Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
            //log.debug(String.format("onReceive() config=%s, settings=%s", config, settings));
            onLocationChanged(location, false);
        }
    }

    void onLocationChanged(Location location, boolean force) {
        double d = maxDistance + 1d;
        if (referenceLocation != null) {
            d = referenceLocation.distanceTo(location);
        }
        //log.debug(String.format("onLocationChanged(location=%s; d=%,.0f)", location, d));
        if ((d > maxDistance) || force) {
            log.debug(String.format(Locale.US, "onLocationChanged() detected significant location change, distance to ref = %,.0f m, new location = %s", d, location));
            Intent intent = new Intent(context, SignificantLocationChangeService.class);
            config.newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            config.toIntent(intent);
            context.startService(intent);
        }
    }

    static Location retrieveReferenceLocation(Settings settings) {
        double lat = settings.getDouble(REFERENCE_LOCATION_LAT, -1d);
        double lng = settings.getDouble(REFERENCE_LOCATION_LNG, -1d);
        Location location = new Location(LocationManager.NETWORK_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }

    static void storeReferenceLocation(Settings settings, Location location) {
        settings.putDouble(REFERENCE_LOCATION_LAT, location.getLatitude());
        settings.putDouble(REFERENCE_LOCATION_LNG, location.getLongitude());
    }

    /**
     * Extract the codes of all monitored geofences from the settings.
     */
    static Collection<String> geofenceCodesFromPrefs(Settings settings) {
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
        return geofencesFromCodes(geofenceCodesFromPrefs(settings));
    }

    static void updateGeofences(Settings settings, Collection<PIGeofence> fences) {
        if (fences != null) {
            settings.putStrings(GEOFENCES_PREF_KEY, geofencesToCodes(fences));
        }
    }

    static List<PIGeofence> geofencesFromCodes(Collection<String> geofenceCodes) {
        int size = geofenceCodes.size(); // in case size() has a non-constant cost
        List<PIGeofence> geofences = new ArrayList<>(size);
        // build the "code in (?, ..., ?)" where clause
        StringBuilder where = new StringBuilder("code in (");
        for (int count=0; count<size; count++) {
            if (count > 0) {
                where.append(", ");
            }
            where.append('?');
        }
        where.append(')');
        geofences.addAll(PIGeofence.find(PIGeofence.class, where.toString(), geofenceCodes.toArray(new String[size])));
        return geofences;
    }

    static PIGeofence geofenceFromCode(String geofenceCode) {
        List<PIGeofence> list = PIGeofence.find(PIGeofence.class, "code = ?", geofenceCode);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    static List<String> geofencesToCodes(Collection<PIGeofence> geofences) {
        List<String> geofenceCodes = new ArrayList<>(geofences.size());
        for (PIGeofence fence: geofences) {
            geofenceCodes.add(fence.getCode());
        }
        return geofenceCodes;
    }

    static int deleteGeofences(Collection<String> geofenceCodes) {
        int size = geofenceCodes.size(); // in case size() has a non-constant cost
        // build the "code in (?, ..., ?)" where clause
        StringBuilder where = new StringBuilder("code in (");
        for (int count = 0; count < size; count++) {
            if (count > 0) {
                where.append(", ");
            }
            where.append('?');
        }
        where.append(')');
        return PIGeofence.deleteAll(PIGeofence.class, where.toString(), geofenceCodes.toArray(new String[size]));
    }

    static byte[] loadResourceBytes(String name) {
        try {
            InputStream is = GeofenceManager.class.getClassLoader().getResourceAsStream(name);
            byte[] buffer = new byte[2048];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.debug(String.format("error while trying to read resource %s", name), e);
        }
        return null;
    }
}
