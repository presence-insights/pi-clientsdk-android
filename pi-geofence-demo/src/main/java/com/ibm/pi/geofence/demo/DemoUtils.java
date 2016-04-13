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

package com.ibm.pi.geofence.demo;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MenuItem;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ibm.pi.geofence.LoggingConfiguration;
import com.ibm.pi.geofence.PIGeofence;
import com.ibm.pi.geofence.PIGeofencingManager;
import com.ibm.pi.geofence.Settings;
import com.ibm.pisdk.geofencing.demo.R;
import com.orm.SugarDb;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility methods used in the geofencing demo.
 */
public class DemoUtils {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(DemoUtils.class.getSimpleName());
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
                log.error("error closing reader", e);
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
        log.debug("deleteGeofenceDB() geofence db name = '" + dbName + "'");
        File dbFile = context.getDatabasePath(dbName);
        boolean result = dbFile.delete();
        log.debug("deleteGeofenceDB() db file = '" + dbFile + "', result of delete = " + result);
    }

    /**
     * Compute the center of the provided geofences, along with the bounds of a box their centers fit in.
     * @param geofences the list of geofences from which to perform the computations.
     */
    static Map<String, Object> computeCenterLocationAndBounds(List<PIGeofence> geofences) {
        log.debug(String.format("computeCenterLocationAndBounds(geofences=%s)", geofences));
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
     * Find a box that fits all specified geofences and with the specified center.
     * @param geofences the list of geofences from which to perform the computations.
     */
    static Map<String, Object> computeBounds(List<PIGeofence> geofences, LatLng centerLocation) {
        log.debug(String.format("computeBounds(geofences=%s, centerLocation=%s)", geofences, centerLocation));
        if (centerLocation == null) return computeCenterLocationAndBounds(geofences);
        Map<String, Object> map = new HashMap<>();
        double maxLat = 0d;
        double maxLng = 0d;
        for (PIGeofence geofence : geofences) {
            double diff = Math.abs(centerLocation.latitude - geofence.getLatitude());
            if (diff > maxLat) {
                maxLat = diff;
            }
            diff = Math.abs(centerLocation.longitude - geofence.getLongitude());
            if (diff > maxLng) {
                maxLng = diff;
            }
        }
        log.debug(String.format("computeBounds() maxLat=%.6f, maxLng=%.6f", maxLat, maxLng));
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
        log.debug(String.format("computeBounds(geofences=%s, latDelta=%.6f, lngDelta=%.6f)", centerLocation, latDelta, lngDelta));
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
            log.debug(String.format("error while trying to read resource %s", name), e);
        }
        return null;
    }

    /**
     * Zip the specified file to a new file suffixed with '.zip'.
     * @param path the path of the file to zip.
     * @return the path to the zipped file, or {@code null} if an I/O error occurred.
     */
    static String zipFile(String path) {
        String zipPath = path + ".zip";
        File inputFile = new File(path);
        File zipFile = new File(zipPath);
        ZipOutputStream zos = null;
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(inputFile));
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            ZipEntry entry = new ZipEntry(inputFile.getName());
            zos.putNextEntry(entry);
            byte[] buffer = new byte[2048];
            int n;
            while ((n = is.read(buffer)) > 0) {
                zos.write(buffer, 0, n);
            }
            zos.finish();
            return zipPath;
        } catch(Exception e) {
            log.debug(String.format("to zip '%s' to '%s'", path, zipPath), e);
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch(IOException ignore) {
            }
        }
        return null;
    }

    static List<PIGeofence> getAllGeofencesFromDB() {
        List<PIGeofence> result = new ArrayList<>();
        try {
            Class<?> pgClass = Class.forName("com.ibm.pi.geofence.PersistentGeofence");
            Method listAllMethod = pgClass.getMethod("listAll", Class.class);
            listAllMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) listAllMethod.invoke(null, pgClass);
            Method toPIGeofenceMethod = pgClass.getDeclaredMethod("toPIGeofence");
            toPIGeofenceMethod.setAccessible(true);
            for (Object o: list) {
                result.add((PIGeofence) toPIGeofenceMethod.invoke(o));
            }
        } catch(Exception e) {
            log.debug("error getting list of geofences from DB: ", e);
        }
        return result;
    }

    static void loadGeofences(PIGeofencingManager manager) {
        try {
            Method m = PIGeofencingManager.class.getDeclaredMethod("loadGeofences");
            m.setAccessible(true);
            m.invoke(manager);
        } catch(Exception e) {
            log.debug("error getting list of geofences: ", e);
        }
    }

    static String extractSettingsData(Context context) {
        try {
            Method m = PIGeofencingManager.class.getDeclaredMethod("extractSettingsData", Context.class);
            m.setAccessible(true);
            return (String) m.invoke(null, context);
        } catch(Exception e) {
            log.debug("error extracting data: ", e);
        }
        return null;
    }

    static void updateSettingsIfNeeded(Settings settings) {
        Set<String> names = settings.getPropertyNames();
        String oldPrefix = "com.ibm.pi.sdk.extra.";
        String newPrefix = "com.ibm.pi.sdk.";
        int nbUpdates = 0;
        for (String name: names) {
            if (name.startsWith(oldPrefix)) {
                String newName = newPrefix + name.substring(oldPrefix.length());
                if (settings.getString(newName, null) == null) {
                    String value = settings.getString(name, null);
                    if (value != null) {
                        settings.putString(newName, value);
                    }
                }
                settings.remove(name);
                nbUpdates++;
            }
        }
        if (nbUpdates > 0) {
            settings.commit();
        }
    }

    static void sendLogByMail(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            //intent.setData(Uri.parse("mailto:")); // only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"LAURENTC@fr.ibm.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "PI sdk log - " + new java.util.Date());
            // zip the log file and send the zip as attachment
            String path = DemoUtils.zipFile(LoggingConfiguration.getLogFile());
            File file = new File(path);
            intent.putExtra(Intent.EXTRA_TEXT, "See attached log file '" + file.getName() + "'");
            Uri uri = Uri.parse(file.toURI().toString());
            log.debug("log file uril = " + uri);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            }
        } catch(Exception e) {
            log.debug(e.getMessage(), e);
        }
    }

    private final static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    public static String toHexString(byte[] bytes) {
        char[] result = new char[2 * bytes.length];
        for (int i=0; i<bytes.length; i++) {
            int a = (bytes[i] & 0xF0) >>> 4;
            int b = bytes[i] & 0x0F;
            result[2*i] = HEX_DIGITS[a];
            result[2*i+1] = HEX_DIGITS[b];
        }
        return new String(result);
    }

    public static byte[] fromHexString(String hexString) {
        char[] chars = hexString.toCharArray();
        byte[] result = new byte[chars.length/2];
        for (int i=0; i<result.length; i++) {
            char c = chars[2*i];
            int a = ((Character.isDigit(c) ?  c - '0' : c - 'A' + 10) << 4) & 0xF0;
            c = chars[2*i+1];
            int b = (Character.isDigit(c) ?  c - '0' : c - 'A' + 10) & 0x0F;
            result[i] = (byte) ((a | b) & 0xFF);
        }
        return result;
    }
}
