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
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.ibm.pisdk.geofencing.PIGeofence;
import com.ibm.pisdk.geofencing.PIGeofencingService;
import com.ibm.pisdk.geofencing.rest.PIHttpService;

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
     * Log tag for this class.
     */
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();
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
     *
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
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "in onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);
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
        Log.v(LOG_TAG, "onCreate() : init of geofencing service");
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
        PIHttpService httpService = new PIHttpService(this, "http://192.168.1.10:3000", "lolo4j", "a94e5011c7d14a51bf41237a22bc27a0", "lolo4j");
        service = new PIGeofencingService(httpService, this, new MyGeofenceCallback(this));
    }

    /**
     * Start the simulation of a user walking between the geofences.
     */
    void startSimulation(List<PIGeofence> fences) {
        for (PIGeofence g : fences) {
            geofenceManager.updateGeofence(g);
        }
        // register a location listener to handle the user's position on the map
        final LocationRequest locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            .setInterval(60000L)
            .setFastestInterval(1000L);
        final List<PIGeofence> geofences = fences;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUpMapIfNeeded();
                for (PIGeofence g : geofences) {
                    refreshGeofenceInfo(g, false);
                }
                LocationServices.FusedLocationApi.requestLocationUpdates(service.getGoogleApiClient(), locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        //Log.v(LOG_TAG, "onLocationChanged() : location=" + location);
                        refreshCurrentLocation(location.getLatitude(), location.getLongitude());
                    }
                });
            }
        });
        /*
        Log.v(LOG_TAG, "startSimulation() : starting WalkSimulator");
        // start the simulated user walk between the geofences locations
        //WalkSimulator sim = new WalkSimulator(fences, 10, 1900L, new WalkSimulator.Callback() {
        WalkSimulator sim = new WalkSimulator(service, fences, 33, 570L, new WalkSimulator.Callback() {
            @Override
            public void onSimulationEnded() {
                Log.v(LOG_TAG, "in onSimulationEnded()");
            }
        });
        new Thread(sim, "WalkSimulator").start();
        */
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly installed) and the map has not already been instantiated.
     */
    void setUpMapIfNeeded() {
        if (googleMap == null) {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            Log.v(LOG_TAG, "setUpMapIfNeeded() : googleMap = " + googleMap);
            if (googleMap != null) {
                List<PIGeofence> fences = geofenceManager.getFences();
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                //Location currentLoc = LocationServices.FusedLocationApi.getLastLocation(service.getGoogleApiClient());
                Location currentLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if ((fences != null) && !fences.isEmpty()) {
                    Map<String, Object> map = DemoUtils.computeBounds(fences, new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()));
                    LatLngBounds bounds = (LatLngBounds) map.get("bounds");
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                    LatLng loc = (LatLng) map.get("center");
                    refreshCurrentLocation(loc.latitude, loc.longitude);
                }
                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if (mapMode == MODE_EDIT) {
                            return true;
                        }
                        GeofenceInfo info = getGeofenceInfoForMarker(marker);
                        Log.v(LOG_TAG, String.format("onMarkerClick(marker=%s) info=%s", marker, info));
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
    private void refreshCurrentLocation(final double latitude, final double longitude) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentMarker == null) {
                    currentMarker = googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("HappyUser"));
                } else {
                    currentMarker.setPosition(new LatLng(latitude, longitude));
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
        /*
        if (active) {
            // set a marker with the name/label associated with the geofence
            BitmapDescriptor btDesc = BitmapDescriptorFactory.fromBitmap(getOrCreateBitmap(fence));
            info.marker = googleMap.addMarker(new MarkerOptions().position(pos).icon(btDesc));
        } else if (info.marker != null) {
            info.marker.remove();
            info.marker = null;
        }
        */
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
