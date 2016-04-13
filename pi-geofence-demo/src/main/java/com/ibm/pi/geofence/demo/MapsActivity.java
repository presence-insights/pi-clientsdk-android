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
import android.content.IntentFilter;
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
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.ibm.pi.core.doctypes.PIOrg;
import com.ibm.pi.geofence.LoggingConfiguration;
import com.ibm.pi.geofence.PIGeofence;
import com.ibm.pi.geofence.PIGeofenceEvent;
import com.ibm.pi.geofence.PIGeofencingManager;
import com.ibm.pi.geofence.Settings;
import com.ibm.pi.geofence.rest.PIRequestCallback;
import com.ibm.pi.geofence.rest.PIRequestError;
import com.ibm.pisdk.geofencing.demo.R;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles a Google map where a number of geofences are highlighted as semi-transparent red circles.
 * As the current locations moves within a geofence, its circle becomes green and a marker with a
 * text label displays the name of the place.
 */
public class MapsActivity extends FragmentActivity {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(MapsActivity.class.getSimpleName());
    //private static Logger log;
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
     * Settings key for the tracking enabled flag.
     */
    static final String TRACKING_ENABLED_KEY = "tracking.enabled";
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
    GeofenceInfo editedInfo = null;
    Settings settings;
    /**
     *
     */
    private final GeofenceHolder geofenceHolder = new GeofenceHolder();
    /**
     * Mapping of geofence uuids to the corresponding objects (marker, circle etc.) displayed on the map.
     */
    Map<String, GeofenceInfo> geofenceInfoMap = new HashMap<>();
    /**
     * Reference to the geofencing service used in this demo.
     */
    PIGeofencingManager manager;
    CustomPIHttpService customHttpService;
    /**
     * Whether the db has already been deleted once.
     */
    private static boolean dbDeleted = false;
    Button addFenceButton = null;
    /**
     * UI mode: MODE_NORMAL or MODE_EDIT when positioning a geofence.
     */
    int mapMode = MODE_NORMAL;
    private ImageView mapCrossHair;
    /**
     * Whether to send Slack and local notifications upon geofence events.
     */
    boolean trackingEnabled = true;
    private MyGeofenceReceiver receiver;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentLocation != null) {
            outState.putDoubleArray("currentLocation", new double[] {currentLocation.getLatitude(), currentLocation.getLongitude()});
        }
        if (googleMap != null) {
            outState.putFloat("zoom", googleMap.getCameraPosition().zoom);
        }
        //log.debug("onSaveInstanceState()");
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
            //log.debug(String.format("restored currentLocation=%s; currentZoom=%f", currentLocation, currentZoom));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (receiver == null) {
            receiver = new MyGeofenceReceiver(this);
        }
        IntentFilter filter = new IntentFilter(PIGeofenceEvent.ACTION_GEOFENCE_EVENT);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log.debug("***************************************************************************************");
        super.onCreate(savedInstanceState);
        String pwd = "8xdr5vfh";
        setContentView(R.layout.maps_activity);
        mapCrossHair = (ImageView) findViewById(R.id.map_cross_hair);
        log.debug("in onCreate() tracking is " + (trackingEnabled ? "enabled" : "disabled"));
        addFenceButton = (Button) findViewById(R.id.addFenceButton);
        addFenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode();
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
        }
        */
        settings = new Settings(this);
        settings.putString("orgCode", "6x07ykw").commit();
        String orgCode = settings.getString("orgCode", null);
        log.debug(String.format("found orgCode = %s from settings", orgCode));
        //manager = PIGeofencingManager.newInstance(MyCallbackService.class, this, "http://pi-outdoor-proxy.mybluemix.net", "xf504jy", orgCode, "a6su7f", "8xdr5vfh", 10_000);
        manager = new PIGeofencingManager(this, "http://pi-outdoor-proxy.mybluemix.net", "xf504jy", orgCode, "a6su7f", pwd, 10_000);
        manager.setMinHoursBetweenServerSyncs(1);
        DemoUtils.updateSettingsIfNeeded(settings);
        trackingEnabled = settings.getBoolean(TRACKING_ENABLED_KEY, true);
        /*
        // testing the loading from a zip resource
        manager.loadGeofencesFromResource("com/ibm/pisdk/geofencing/geofence_2016-03-18_14_38_04.zip", new PIRequestCallback<List<PIGeofence>>() {
            @Override
            public void onSuccess(List<PIGeofence> result) {
                log.debug("successfully loaded geofences from resource: " + result);
            }

            @Override
            public void onError(PIRequestError error) {
                log.debug("error loading geofences from resource: " + error);
            }
        });
        */
        customHttpService = new CustomPIHttpService(manager, this, "http://pi-outdoor-proxy.mybluemix.net", "xf504jy", orgCode, "a6su7f", pwd);
        if (orgCode == null) {
            //String orgName = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            String orgName = "android-" + UUID.randomUUID().toString();
            log.debug("org name = " + orgName);
            customHttpService.createOrg(orgName, "org for id " + orgName, null, true, new PIRequestCallback<PIOrg>() {
                @Override
                public void onSuccess(PIOrg result) {
                    String orgCode = result.getCode();
                    updateTitle(orgCode);
                    settings.putString("orgCode", orgCode).commit();
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error creating org: " + error);
                }
            });
        } else {
            updateTitle(orgCode);
        }
        try {
            startSimulation(geofenceHolder.getFences());
        } catch(Exception e) {
            log.error("error in startSimulation()", e);
        }
    }

    /**
     * Update the title in the action bar with the specified org code.
     */
    private void updateTitle(String orgCode) {
        try {
            String s = getString(R.string.title_activity_maps);
            getActionBar().setTitle(String.format("%s (org: %s)", s, orgCode));
        } catch(Exception e) {
            log.error(String.format("error setting title bar with org=%s", orgCode), e);
        }
    }

    /**
     * Start the simulation of a user walking between the geofences.
     */
    void startSimulation(final List<PIGeofence> fences) {
        for (PIGeofence g : fences) {
            geofenceHolder.updateGeofence(g);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setUpMapIfNeeded();
                    for (PIGeofence g : fences) {
                        refreshGeofenceInfo(g, false);
                    }
                } catch (Exception e) {
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
                try {
                    if (currentLocation == null)  currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch(SecurityException e) {
                    log.debug("missing permission to request get last location from NETWORK_PROVIDER");
                }
                initGeofences();
                // receive updates for th ecurrent location
                try {
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
                } catch(SecurityException e) {
                    log.debug("missing permission to request locations from NETWORK_PROVIDER");
                }
                // set the map center and zoom level/bounds once it is loaded
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<PIGeofence> fences = geofenceHolder.getFences();
                                LatLng latlng = currentLocation != null
                                    ? new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())
                                    : googleMap.getCameraPosition().target;
                                Map<String, Object> map = (fences != null) && !fences.isEmpty() ? DemoUtils.computeBounds(fences, latlng) : DemoUtils.computeBounds(latlng, 0.0005, 0.0005);
                                log.debug("setUpMapIfNeeded() : bounds map = " + map + ", fences = " + fences);
                                LatLngBounds bounds = (LatLngBounds) map.get("bounds");
                                LatLng loc = (LatLng) map.get("center");
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
                        if (mapMode != MODE_EDIT) {
                            GeofenceInfo info = getGeofenceInfoForMarker(marker);
                            log.debug(String.format("onMarkerClick(marker=%s) info=%s", marker, info));
                            EditGeofenceDialog dialog = new EditGeofenceDialog();
                            dialog.customInit(MapsActivity.this, EditGeofenceDialog.MODE_UPDATE_DELETE, info);
                            dialog.show(getFragmentManager(), "geofences");
                        }
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
            BitmapDescriptor btDesc = BitmapDescriptorFactory.defaultMarker(geofenceHolder.hasActiveFence() ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_RED);
            currentMarker.setIcon(btDesc);
        }
    }

    /**
     * Update the circle and label of a geofence depending on its active state.
     * @param fence  the fence to update.
     * @param active whether the fence is active (current location is within the fence).
     */
    void refreshGeofenceInfo(PIGeofence fence, boolean active) {
        String uuid = fence.getCode();
        if (geofenceHolder.getGeofence(uuid) == null) {
            geofenceHolder.updateGeofence(fence);
        }
        if (active) {
            geofenceHolder.addActiveFence(uuid);
        } else {
            geofenceHolder.removeActiveFence(uuid);
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
        String uuid = fence.getCode();
        GeofenceInfo info = geofenceInfoMap.get(uuid);
        if (info != null) {
            info.marker.remove();
            info.circle.remove();
            geofenceInfoMap.remove(uuid);
            geofenceHolder.removeGeofence(fence);
        }
    }

    /**
     * Create a bitmap inside which the name of the geofence is drawn.
     * There is no other way to put text in a marker, as the Google map API does not provide any method for this.
     * @param fence the fence whose name to retrieve.
     * @return the generated bitmap with the geofence name inside.
     */
    Bitmap getOrCreateBitmap(PIGeofence fence, GeofenceInfo info) {
        String key = "geofence.icon." + fence.getCode();
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

    public GeofenceHolder getGeofenceHolder() {
        return geofenceHolder;
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
        try {
            final List<PIGeofence> fences = DemoUtils.getAllGeofencesFromDB();
            log.debug("initGeofences() " + (fences == null ? 0 : fences.size()) + " fences in local DB");
            geofenceHolder.clearFences();
            geofenceHolder.addFences(fences);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
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
                    } catch(Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private final AtomicBoolean simulationStarted = new AtomicBoolean(false);

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tracking:
                trackingEnabled = !trackingEnabled;
                log.debug("tracking is now " + (trackingEnabled ? "enabled" : "disabled"));
                settings.putBoolean(TRACKING_ENABLED_KEY, trackingEnabled).commit();
                item.setIcon(trackingEnabled ? R.mipmap.on : R.mipmap.off);
                log.debug(String.format("onOptionsItemSelected() tracking is now %s", trackingEnabled ? "enabled" : "disabled"));
                break;
            case R.id.action_mail_log:
                DemoUtils.sendLogByMail(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.action_tracking);
        item.setIcon(trackingEnabled ? R.mipmap.on : R.mipmap.off);
        return true;
    }

    void switchMode() {
        if (mapMode == MODE_NORMAL) {
            mapMode = MODE_EDIT;
            mapCrossHair.setAlpha(1.0f);
            addFenceButton.setCompoundDrawablesRelativeWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
            if (currentMarker != null) {
                currentMarker.remove();
                currentMarker = null;
            }
        } else if (mapMode == MODE_EDIT) {
            mapMode = MODE_NORMAL;
            mapCrossHair.setAlpha(0.0f);
            addFenceButton.setCompoundDrawablesRelativeWithIntrinsicBounds(android.R.drawable.ic_input_add, 0, 0, 0);
            googleMap.setOnCameraChangeListener(null);
            EditGeofenceDialog dialog = new EditGeofenceDialog();
            dialog.customInit(MapsActivity.this, editedInfo == null ? EditGeofenceDialog.MODE_NEW : EditGeofenceDialog.MODE_UPDATE_DELETE, editedInfo);
            dialog.show(getFragmentManager(), "geofences");
        }
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
