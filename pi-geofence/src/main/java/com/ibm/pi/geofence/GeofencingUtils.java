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

import android.location.Location;
import android.location.LocationManager;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A collection of utility methods for manipulating geofences and locations.
 */
class GeofencingUtils {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(GeofencingUtils.class.getSimpleName());
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final String GEOFENCES_PREF_KEY = "com.ibm.pi.geofences";
    /**
     * Key for the shared preferences that stores the uuids of the registered fences.
     */
    private static final Set<String> GEOFENCES_PREF_DEFAULT = Collections.emptySet();
    /**
     * Settings keys for the last reference location.
     */
    private static final String REFERENCE_LOCATION_LAT = "com.ibm.pi.ref_lat";
    private static final String REFERENCE_LOCATION_LNG = "com.ibm.pi.ref_lng";

    private GeofencingUtils() {
    }

    /**
     * Retrieve the reference location provided in the {@link Settings}.
     * @param settings the settings from which to retirive the reference location.
     * @return a {@link Location} object.
     */
    static Location retrieveReferenceLocation(Settings settings) {
        double lat = settings.getDouble(REFERENCE_LOCATION_LAT, -1d);
        double lng = settings.getDouble(REFERENCE_LOCATION_LNG, -1d);
        Location location = new Location(LocationManager.NETWORK_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }

    /**
     * Store the reference location into the {@link Settings}.
     */
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
    static List<PersistentGeofence> extractGeofences(Settings settings) {
        return geofencesFromCodes(geofenceCodesFromPrefs(settings));
    }

    static void updateGeofences(Settings settings, Collection<PersistentGeofence> fences) {
        if (fences != null) {
            settings.putStrings(GEOFENCES_PREF_KEY, geofencesToCodes(fences));
        }
    }

    /**
     * Get a set of geofoences from their codes, via a lookup in the local DB.
     * @param geofenceCodes the codes of the geofenes to retrieve.
     * @return a list of {@link PIGeofence} objects.
     */
    static List<PersistentGeofence> geofencesFromCodes(Collection<String> geofenceCodes) {
        int size = geofenceCodes.size(); // in case size() has a non-constant cost
        List<PersistentGeofence> geofences = new ArrayList<>(size);
        // build the "code in (?, ..., ?)" where clause
        StringBuilder where = new StringBuilder("code in (");
        for (int count=0; count<size; count++) {
            if (count > 0) {
                where.append(", ");
            }
            where.append('?');
        }
        where.append(')');
        geofences.addAll(PersistentGeofence.find(PersistentGeofence.class, where.toString(), geofenceCodes.toArray(new String[size])));
        return geofences;
    }

    static PersistentGeofence geofenceFromCode(String geofenceCode) {
        List<PersistentGeofence> list = PersistentGeofence.find(PersistentGeofence.class, "code = ?", geofenceCode);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    static List<String> geofencesToCodes(Collection<PersistentGeofence> geofences) {
        List<String> geofenceCodes = new ArrayList<>(geofences.size());
        for (PersistentGeofence fence: geofences) {
            geofenceCodes.add(fence.getCode());
        }
        return geofenceCodes;
    }

    /**
     * Delete the specified geofences from the local DB.
     * @param geofenceCodes the codes of the geofences to delete.
     * @return the number of actually deleted geofences.
     */
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
        return PersistentGeofence.deleteAll(PersistentGeofence.class, where.toString(), geofenceCodes.toArray(new String[size]));
    }

    /**
     * Load a resource from its path in the classpath.
     * @param name the path to the resource, must be accessible from the classpath root.
     * @return the resource content as a byte array, or {code null} if the resource could not be loaded.
     */
    static byte[] loadResourceBytes(String name) {
        try {
            InputStream is = LocationUpdateReceiver.class.getClassLoader().getResourceAsStream(name);
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
