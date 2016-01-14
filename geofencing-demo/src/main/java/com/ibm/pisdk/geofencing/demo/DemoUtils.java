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

package com.ibm.pisdk.geofencing.demo;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ibm.pisdk.geofencing.PIGeofence;
import com.orm.SugarDb;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Utility methods used in the geofencing demo.
 */
public class DemoUtils {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = DemoUtils.class.getSimpleName();
    private static Random rand = new Random(System.nanoTime());

    /**
     * Read text from the psecified reader.
     * @param reader the source to read from.
     * @return a String witht he content of the reader.
     * @throws Exception if any I/O error occurs.
     */
    static String readText(Reader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = null;
        try {
            r = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
            String s;
            while ((s = r.readLine()) != null) {
                sb.append(s).append('\n');
            }
        } finally {
            try {
                if (r != null) r.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error closing reader", e);
            }
        }
        return sb.toString();
    }

    /**
     * Create a {@link Location} object from the speified latitude and longitude.
     * @param latitude the latitude for the location to create.
     * @param longitude the longitude for the location to create.
     * @return a {@link Location} object.
     */
    public static Location createLocation(double latitude, double longitude) {
        Location location = new Location(LocationManager.NETWORK_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(10f);
        location.setTime(System.currentTimeMillis());
        location.setAltitude(0d);
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return location;
    }

    /**
     * Randomize the specified list.
     * @param list the list to randomize.
     * @param <E> the type of elements in the list.
     * @return a new list with all the elements of the input but in a random order.
     */
    public static <E> List<E> randomize(List<E> list) {
        List<E> tmp = new ArrayList<>(list);
        List<E> result = new ArrayList<>(list.size());
        while (!tmp.isEmpty()) {
            int n = rand.nextInt(tmp.size());
            result.add(tmp.remove(n));
        }
        return result;
    }

    static void deleteGeofenceDB(Context context) {
        SugarDb sugarDB = new SugarDb(context);
        String dbName = sugarDB.getDatabaseName();
        Log.v(LOG_TAG, "deleteGeofenceDB() geofence db name = '" + dbName + "'");
        File dbFile = context.getDatabasePath(dbName);
        boolean result = dbFile.delete();
        Log.v(LOG_TAG, "deleteGeofenceDB() db file = '" + dbFile + "', result of delete = " + result);
    }

    static void loadLocallyStoredGeofences() {
    }

    /**
     * Compute the center of the provided geofences, along with the bounds of a box their centers fit in.
     * @param geofences the list of geofences from which to perform the computations.
     */
    static Map<String, Object> computeCenterLocationAndBounds(List<PIGeofence> geofences) {
        Map<String, Object> map = new HashMap<>();
        double minLat = Double.MAX_VALUE;
        double maxLat = 0d;
        double minLng = Double.MAX_VALUE;
        double maxLng = 0d;
        for (PIGeofence geofence : geofences) {
            if (geofence.getLatitude() < minLat) minLat = geofence.getLatitude();
            if (geofence.getLatitude() > maxLat) maxLat = geofence.getLatitude();
            if (geofence.getLongitude() < minLng) minLng = geofence.getLongitude();
            if (geofence.getLongitude() > maxLng) maxLng = geofence.getLongitude();
        }
        map.put("center", new LatLng(minLat + (maxLat - minLat) / 2d, minLng + (maxLng - minLng) / 2d));
        map.put("bounds", new LatLngBounds(new LatLng(minLat, minLng), new LatLng(maxLat, maxLng)));
        return map;
    }

    /**
     * Compute the center of the provided geofences, along with the bounds of a box their centers fit in.
     * @param geofences the list of geofences from which to perform the computations.
     */
    static Map<String, Object> computeBounds(List<PIGeofence> geofences, LatLng centerLocation) {
        if (centerLocation == null) return computeCenterLocationAndBounds(geofences);
        Map<String, Object> map = new HashMap<>();
        double maxLat = 0d;
        double maxLng = 0d;
        //centerLocation.
        for (PIGeofence geofence : geofences) {
            double diff = Math.abs(centerLocation.latitude - geofence.getLatitude());
            if (diff > maxLat) maxLat = diff;
            diff = Math.abs(centerLocation.longitude - geofence.getLongitude());
            if (diff > maxLng) maxLng = diff;
        }
        LatLngBounds bounds = new LatLngBounds(new LatLng(centerLocation.latitude - maxLat, centerLocation.longitude - maxLng),
            new LatLng(centerLocation.latitude + maxLat, centerLocation.longitude + maxLng));
        map.put("center", centerLocation);
        map.put("bounds", bounds);
        return map;
    }

    /**
     * Compute the center of the provided geofences, along with the bounds of a box their centers fit in.
     * @param centerLocation .
     */
    static Map<String, Object> computeBounds(LatLng centerLocation, double latDelta, double lngDelta) {
        Map<String, Object> map = new HashMap<>();
        LatLngBounds bounds = new LatLngBounds(new LatLng(centerLocation.latitude - latDelta, centerLocation.longitude - lngDelta),
            new LatLng(centerLocation.latitude + latDelta, centerLocation.longitude + lngDelta));
        map.put("center", centerLocation);
        map.put("bounds", bounds);
        return map;
    }

    static byte[] loadResourceBytes(String name) {
        try {
            InputStream is = DemoUtils.class.getClassLoader().getResourceAsStream(name);
            byte[] buffer = new byte[2048];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            Log.d(LOG_TAG, String.format("error while trying to read resource %s", name), e);
        }
        return null;
    }
}
