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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.ibm.pisdk.geofencing.LoggingConfiguration;
import com.ibm.pisdk.geofencing.PIGeofence;
import com.ibm.pisdk.geofencing.PIGeofencingService;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles a Google map where a number of geofences are highlighted as semi-transparent red circles.
 * As the current locations moves within a geofence, its circle becomes green and a marker with a
 * text label displays the name of the place.
 */
public class MapsActivity extends FragmentActivity {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(MapsActivity.class);
    /**
     * Color for fences active on the map.
     */
    private static final int ACTIVE_FENCE_COLOR = 0x6080E080;
    /**
     * Color for fences active on the map.
     */
    private static final int INACTIVE_FENCE_COLOR = 0x40E08080;
    /**
     * Normal mode.
     */
    private static final int MODE_NORMAL = 1;
    /**
     * In this mode, we are defining the location of a geofence.
     */
    private static final int MODE_EDIT = 2;
    /**
     * Might be null if Google Play services APK is not available.
     */
    GoogleMap googleMap;
    /**
     * Marker on the map for the user's current position.
     */
    private Marker currentMarker = null;
    Location currentLocation = null;
    float currentZoom = -1f;
    /**
     *
     */
    private final GeofenceManager geofenceManager = new GeofenceManager();
    /**
     * Mapping of geofence uuids to the corresponding objects (marker, circle etc.) displayed on the map.
     */
    Map<String, GeofenceInfo> geofenceInfoMap = new HashMap<>();
    /**
     * Reference to the geofencing service used in this demo.
     */
    PIGeofencingService service;
    /**
     * Whether the db has already been deleted once.
     */
    private static boolean dbDeleted = false;
    /**
     *
     */
    int mapMode = MODE_NORMAL;
    /**
     * Whether to send Slack and local notifications upon geofence events.
     */
    boolean trackingEnabled = true;
    /**
     * Positions a marker always at the center of the map while following zoom and pan actions.
     * This isn't great, because events are only sent after the transition/movement has ended, never during the transition,
     * thus the marker appears to stutter.
     */
    final GoogleMap.OnCameraChangeListener cameraListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            MarkerOptions options = new MarkerOptions().position(cameraPosition.target);
            if (currentMarker != null) {
                currentMarker.remove();
            }
            currentMarker = googleMap.addMarker(options);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentLocation != null) {
            outState.putDoubleArray("currentLocation", new double[] {currentLocation.getLatitude(), currentLocation.getLongitude()});
        }
        if (googleMap != null) {
            outState.putFloat("zoom", googleMap.getCameraPosition().zoom);
        }
        log.debug("onSaveInstanceState()");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            double[] loc = savedInstanceState.getDoubleArray("currentLocation");
            if (loc != null) {
                currentLocation = new Location(LocationManager.NETWORK_PROVIDER);
                currentLocation.setLatitude(loc[0]);
                currentLocation.setLongitude(loc[1]);
                currentLocation.setTime(System.currentTimeMillis());
            }
            currentZoom = savedInstanceState.getFloat("zoom", -1f);
            log.debug(String.format("restored currentLocation=%s; currentZoom=%f", currentLocation, currentZoom));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LoggingConfiguration.configure(this);
        log.debug("***************************************************************************************");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        trackingEnabled = prefs.getBoolean("tracking.enabled", true);
        log.debug("in onCreate() tracking is " + (trackingEnabled ? "enabled" : "disabled"));
        final Button btn = (Button) findViewById(R.id.addFenceButton);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapMode == MODE_NORMAL) {
                    mapMode = MODE_EDIT;
                    btn.setCompoundDrawablesRelativeWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
                    LatLng pos = googleMap.getCameraPosition().target;
                    refreshCurrentLocation(pos.latitude, pos.longitude);
                    googleMap.setOnCameraChangeListener(cameraListener);
                } else if (mapMode == MODE_EDIT) {
                    mapMode = MODE_NORMAL;
                    btn.setCompoundDrawablesRelativeWithIntrinsicBounds(android.R.drawable.ic_input_add, 0, 0, 0);
                    googleMap.setOnCameraChangeListener(null);
                    EditGeofenceDialog dialog = new EditGeofenceDialog();
                    dialog.customInit(MapsActivity.this, EditGeofenceDialog.MODE_NEW, null);
                    dialog.show(getFragmentManager(), "geofences");
                }
            }
        });
        if (savedInstanceState != null) {
            double[] loc = savedInstanceState.getDoubleArray("currentLocation");
            if (loc != null) {
                currentLocation = new Location(LocationManager.NETWORK_PROVIDER);
                currentLocation.setLatitude(loc[0]);
                currentLocation.setLongitude(loc[1]);
                currentLocation.setTime(System.currentTimeMillis());
            }
            currentZoom = savedInstanceState.getFloat("zoom", -1f);
            log.debug(String.format("restored currentLocation=%s; currentZoom=%f", currentLocation, currentZoom));
        }
        log.debug("onCreate() : init of geofencing service");
        /*
        if (!dbDeleted) {
            dbDeleted = true;
            DemoUtils.deleteGeofenceDB(this);
            String json = null;
            try {
                json = new String(DemoUtils.loadResourceBytes("com/ibm/pisdk/geofencing/small_sample_fences.geojson"));
                PIGeofenceList geofenceList = GeofencingJSONParser.parseGeofences(new JSONObject(json));
                Log.d(LOG_TAG, "loaded " + geofenceList.getGeofences().size() + " geofences");
                PIGeofence.saveInTx(geofenceList.getGeofences());
            } catch (Exception e) {
                Log.d(LOG_TAG, String.format("error while parsing JSON: %s", json), e);
            }
        }
        */
        service = new PIGeofencingService(new MyGeofenceCallback(this), this, "http://starterapp.mybluemix.net", "xf504jy", "bj6s0rw5", "a6su7f", "8xdr5vfh");
        service.setSendingGeofenceEvents(false);
        try {
            startSimulation(geofenceManager.getFences());
        } catch(Exception e) {
            log.error("error in startSimulation()", e);
        }
    }

    /**
     * Start the simulation of a user walking between the geofences.
     */
    void startSimulation(final List<PIGeofence> fences) {
        for (PIGeofence g : fences) {
            geofenceManager.updateGeofence(g);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setUpMapIfNeeded();
                    for (PIGeofence g : fences) {
                        refreshGeofenceInfo(g, false);
                    }
                } catch(Exception e) {
                    log.error("error in startSimulation()", e);
                }
            }
        });
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly installed) and the map has not already been instantiated.
     */
    void setUpMapIfNeeded() {
        if (googleMap == null) {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            log.debug("setUpMapIfNeeded() : googleMap = " + googleMap);
            if (googleMap != null) {
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                if (currentLocation == null)  currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                initGeofences();
                // receive updates for th ecurrent location
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10f, new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        currentLocation = location;
                        refreshCurrentLocation();
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) { }
                    @Override
                    public void onProviderEnabled(String provider) { }
                    @Override
                    public void onProviderDisabled(String provider) { }
                 });
                LatLng latlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                List<PIGeofence> fences = geofenceManager.getFences();
                Map<String, Object> map = (fences != null) && !fences.isEmpty() ? DemoUtils.computeBounds(fences, latlng) : DemoUtils.computeBounds(latlng, 0.0005, 0.0005);
                log.debug("setUpMapIfNeeded() : bounds map = " + map + ", fences = " + fences);
                final LatLngBounds bounds = (LatLngBounds) map.get("bounds");
                final LatLng loc = (LatLng) map.get("center");
                // set the map center and zoom level/bounds once it is loaded
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentZoom >= 0f) {
                                    log.debug("setUpMapIfNeeded() setting zoom");
                                    googleMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
                                } else {
                                    log.debug("setUpMapIfNeeded() setting bounds");
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                                }
                                refreshCurrentLocation(loc.latitude, loc.longitude);
                            }
                        });
                    }
                });
                // respond to taps on the fences labels
                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if (mapMode == MODE_EDIT) {
                            return true;
                        }
                        GeofenceInfo info = getGeofenceInfoForMarker(marker);
                        log.debug(String.format("onMarkerClick(marker=%s) info=%s", marker, info));
                        EditGeofenceDialog dialog = new EditGeofenceDialog();
                        dialog.customInit(MapsActivity.this, EditGeofenceDialog.MODE_UPDATE_DELETE, info);
                        dialog.show(getFragmentManager(), "geofences");
                        return true;
                    }
                });
            }
        }
    }

    /**
     * Update the marker for the device's current location.
     */
    void refreshCurrentLocation() {
        refreshCurrentLocation(currentLocation.getLatitude(), currentLocation.getLongitude());
    }

    /**
     * Update the marker for the device's current location.
     */
    private void refreshCurrentLocation(final double latitude, final double longitude) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LatLng pos = new LatLng(latitude, longitude);
                if (currentMarker == null) {
                    currentMarker = googleMap.addMarker(new MarkerOptions().position(pos).title("HappyUser"));
                } else {
                    currentMarker.setPosition(pos);
                }
                updateCurrentMarker();
            }
        });
    }

    /**
     * Update the marker for the current location, based on whether the location is inside a geofence or not.
     */
    void updateCurrentMarker() {
        if (currentMarker != null) {
            BitmapDescriptor btDesc = BitmapDescriptorFactory.defaultMarker(geofenceManager.hasActiveFence() ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_RED);
            currentMarker.setIcon(btDesc);
        }
    }

    /**
     * Update the circle and label or a geofence depending on its active state.
     *
     * @param fence  the fence to update.
     * @param active whether the fence is active (current location is within the fence).
     */
    void refreshGeofenceInfo(PIGeofence fence, boolean active) {
        String uuid = fence.getUuid();
        if (geofenceManager.getGeofence(uuid) == null) {
            geofenceManager.updateGeofence(fence);
        }
        if (active) {
            geofenceManager.addActiveFence(uuid);
        } else {
            geofenceManager.removeActiveFence(uuid);
        }
        GeofenceInfo info = geofenceInfoMap.get(uuid);
        if (info == null) {
            info = new GeofenceInfo();
            info.uuid = uuid;
            info.name = fence.getName();
            geofenceInfoMap.put(uuid, info);
        }
        info.active = active;
        int color = active ? ACTIVE_FENCE_COLOR : INACTIVE_FENCE_COLOR;
        LatLng pos = new LatLng(fence.getLatitude(), fence.getLongitude());
        if (info.circle != null) {
            info.circle.setFillColor(color);
        } else {
            Circle c = googleMap.addCircle(new CircleOptions().center(pos).radius(fence.getRadius()).fillColor(color).strokeWidth(0f));
            c.setVisible(true);
            info.circle = c;
        }
        BitmapDescriptor btDesc = BitmapDescriptorFactory.fromBitmap(getOrCreateBitmap(fence, info));
        if (info.marker != null) {
            info.marker.setIcon(btDesc);
        } else {
            info.marker = googleMap.addMarker(new MarkerOptions().position(pos).icon(btDesc));
        }
        info.name = fence.getName();
    }

    void removeGeofence(PIGeofence fence) {
        String uuid = fence.getUuid();
        GeofenceInfo info = geofenceInfoMap.get(uuid);
        if (info != null) {
            info.marker.remove();
            info.circle.remove();
            geofenceInfoMap.remove(uuid);
            geofenceManager.removeGeofence(fence);
        }
    }

    /**
     * Create a bitmap inside which the name of the geofence is drawn.
     * There is no other way to put text in a marker, as the Google map API does not provide any method for this.
     * @param fence the fence whose name to retrieve.
     * @return the generated bitmap with the geofence name inside.
     */
    Bitmap getOrCreateBitmap(PIGeofence fence, GeofenceInfo info) {
        String key = "geofence.icon." + fence.getUuid();
        Bitmap bitmap = GenericCache.getInstance().get(key);
        if ((bitmap == null) || !fence.getName().equals(info.name)) {
            IconGenerator gen = new IconGenerator(this);
            gen.setColor(0x80C0C0FF);
            bitmap = gen.makeIcon(fence.getName());
            // cache the bitmap for later reuse instead of re-generating it
            GenericCache.getInstance().put(key, bitmap);
        }
        return bitmap;
    }

    public GeofenceManager getGeofenceManager() {
        return geofenceManager;
    }

    GeofenceInfo getGeofenceInfoForMarker(Marker marker) {
        for (Map.Entry<String, GeofenceInfo> entry: geofenceInfoMap.entrySet()) {
            GeofenceInfo info = entry.getValue();
            if (marker.equals(info.marker)) {
                return info;
            }
        }
        return null;
    }

    void initGeofences() {
        List<PIGeofence> fences = PIGeofence.listAll(PIGeofence.class);
        geofenceManager.addFences(fences);
        for (PIGeofence g: fences) {
            boolean active = false;
            if (currentLocation != null) {
                Location loc = new Location(LocationManager.NETWORK_PROVIDER);
                loc.setLatitude(g.getLatitude());
                loc.setLongitude(g.getLongitude());
                loc.setTime(System.currentTimeMillis());
                active = loc.distanceTo(currentLocation) <= g.getRadius();
            }
            refreshGeofenceInfo(g, active);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tracking:
                trackingEnabled = !trackingEnabled;
                log.debug("tracking is now " + (trackingEnabled ? "enabled" : "disabled"));
                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("tracking.enabled", trackingEnabled);
                editor.apply();
                item.setIcon(trackingEnabled ? android.R.drawable.presence_video_online : android.R.drawable.presence_video_busy);
                log.debug(String.format("onOptionsItemSelected() tracking is now %s", trackingEnabled ? "enabled" : "disabled"));
                break;
            case R.id.action_mail_log:
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("message/rfc822");
                    //intent.setType("text/plain");
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
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                } catch(Exception e) {
                    log.debug(e.getMessage(), e);
                }
                break;
        }
       return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.action_tracking);
        item.setIcon(trackingEnabled ? android.R.drawable.presence_video_online : android.R.drawable.presence_video_busy);
        return true;
    }

    /**
     * Basic data structure holding the objects displayed on the map for an active geofence.
     */
    static class GeofenceInfo {
        Circle circle;
        Marker marker;
        boolean active = false;
        String uuid;
        String name;
    }
}
