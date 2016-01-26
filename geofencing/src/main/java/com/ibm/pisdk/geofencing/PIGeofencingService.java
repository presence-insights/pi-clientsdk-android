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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.ibm.pisdk.PIDeviceID;
import com.ibm.pisdk.PIDeviceIDFactory;
import com.ibm.pisdk.geofencing.rest.PIHttpService;
import com.ibm.pisdk.geofencing.rest.PIJSONPayloadRequest;
import com.ibm.pisdk.geofencing.rest.PIRequestCallback;
import com.ibm.pisdk.geofencing.rest.PIRequestError;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides an API to load geofences from, and send entry/exit to, the server.
 */
public class PIGeofencingService {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = PIGeofencingService.class.getSimpleName();
    static final String INTENT_ID = "com.ibm.pisdk.geofencing.PIGeofencingService";
    static final String GEOFENCE_CONNECTOR_PATH = "conn-geofence/v1";
    /**
     * The restful service which connects to and communicates with the Adaptive Experience server.
     */
    final PIHttpService httpService;
    /**
     * The Google API client.
     */
    private GoogleApiClient googleApiClient;
    /**
     * The Android application context.
     */
    Context context;
    /**
     * Handles the geofences that are currently monitored.
     */
    final GeofenceManager geofenceManager;
    /**
     * The callback used to notify the app of geofence events.
     */
    final PIGeofenceCallback geofenceCallback;
    /**
     * An internal cache of the "add geofences" requests, so the geofence transition service knows which callback to invoke.
     */
    static final Map<String, PIGeofenceCallback> callbackMap = new ConcurrentHashMap<>();
    private PendingIntent pendingIntent = null;
    /**
     * Detects the app's background or foreground state and emits notifications accordingly.
     */
    private final ForegroundBackgroundHandler appStateHandler;
    /**
     * The current session information.
     */
    private Session session;
    /**
     * Provides uniquely identifying information for the device.
     */
    private final PIDeviceID deviceID;

    /**
     * Initialize this service.
     * @param httpService The restful service which connects to and communicates with the Adaptive Experience server.
     * @param context the Android application context.
     * @param geofenceCallback callback invoked a geofence is triggered.
     */
    public PIGeofencingService(PIHttpService httpService, Context context, PIGeofenceCallback geofenceCallback) {
        this.httpService = httpService;
        this.appStateHandler = new ForegroundBackgroundHandler(context);
        session = new Session(System.currentTimeMillis());
        this.appStateHandler.registerAppStateCallbacks(new ForegroundBackgroundHandler.AppStateCallbacks() {
            @Override
            public void onAppInForeground() {
                // store the session start date and send notif of previous session, if any
                Log.v(LOG_TAG, "application went to foreground!!!");
                if (session != null) {
                    //postMobileSessionData(session, null);
                }
                session = new Session(System.currentTimeMillis());
            }

            @Override
            public void onAppInBackground() {
                // store the session end date and/or duration
                Log.v(LOG_TAG, "application went to background!!!");
                if (session != null) {
                    session.duration = (System.currentTimeMillis() - session.start) / 1_000L;
                }
            }
        });

        this.geofenceCallback = new DelegatingGeofenceCallback(this, geofenceCallback);
        callbackMap.put(INTENT_ID, geofenceCallback);
        this.context = context;
        this.deviceID = PIDeviceIDFactory.newInstance(context);
        int n = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        String s = "undefined";
        if (n == ConnectionResult.SUCCESS) s = "SUCCESS";
        else if (n == ConnectionResult.SERVICE_MISSING) s = "SERVICE_MISSING";
        else if (n == ConnectionResult.SERVICE_UPDATING) s = "SERVICE_UPDATING";
        else if (n == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) s = "SERVICE_VERSION_UPDATE_REQUIRED";
        else if (n == ConnectionResult.SERVICE_DISABLED) s = "SERVICE_DISABLED";
        else if (n == ConnectionResult.SERVICE_INVALID) s = "SERVICE_INVALID";
        Log.v(LOG_TAG, "google play service availability = " + s);
        geofenceManager = new GeofenceManager(this);
        GoogleLocationAPICallback serviceCallback = new GoogleLocationAPICallback(this);
        googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).addConnectionCallbacks(serviceCallback).addOnConnectionFailedListener(serviceCallback).build();
        Log.v(LOG_TAG, "initGms() connecting to google play services ...");
        googleApiClient.connect();
    }

    /**
     * Send a notification to the geofence connector as an HTTP request.
     * @param fences the geofences for which to send a notification.
     * @param type the type of geofence notification: either {@link GeofenceNotificationType#IN IN} or {@link GeofenceNotificationType#OUT OUT}.
     */
    void sendGeofenceNotification(final List<PIGeofence> fences, final GeofenceNotificationType type) {
        PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                Log.v(LOG_TAG, "sucessfully notified connector for geofences " + fences);
            }

            @Override
            public void onError(PIRequestError error) {
                Log.e(LOG_TAG, "error notifyiing connector for geofences " + fences + " : " + error.toString());
            }
        };
        JSONObject payload = GeofencingJSONUtils.toJSON(fences, type, deviceID.getHardwareId());
        PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, "POST", payload.toString());
        String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s",
            GEOFENCE_CONNECTOR_PATH, httpService.getTenant(), httpService.getOrg());
        request.setPath(path);
        request.setBasicAuthRequired(true);
        httpService.executeRequest(request);
    }

    /**
     * Add the specified geofences to the monitored geofences.
     * @param geofences the geofences to add.
     */
    public void addGeofences(List<PIGeofence> geofences) {
        Log.v(LOG_TAG, "addGeofences(" + geofences + ")");
        geofences = geofenceManager.filterFromPrefs(geofences);
        if (!geofences.isEmpty()) {
            List<Geofence> list = new ArrayList<>(geofences.size());
            for (PIGeofence geofence : geofences) {
                list.add(new Geofence.Builder().setRequestId(geofence.getUuid())
                    .setCircularRegion(geofence.getLatitude(), geofence.getLongitude(), (float) geofence.getRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(100)
                    .setLoiteringDelay(100)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
                );
            }
            GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(list).build();
            // used to keep track of the add request
            PendingIntent pi = getPendingIntent(INTENT_ID);
            LocationServices.GeofencingApi.addGeofences(googleApiClient, request, pi).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    Log.v(LOG_TAG, "add geofence request status " + status);
                }
            });
            geofenceManager.addFencesToPrefs(geofences);
            geofenceCallback.onGeofencesMonitored(geofences);
        }
    }

    /**
     * Remove the specified geofences from the monitored geofences.
     * @param geofences the geofences to remove.
     */
    public void removeGeofences(List<PIGeofence> geofences) {
        Log.v(LOG_TAG, "removeGeofences(" + geofences + ")");
        geofences = geofenceManager.filterFromPrefs(geofences);
        if (!geofences.isEmpty()) {
            List<String> uuidsToRemove = new ArrayList<>(geofences.size());
            for (PIGeofence g : geofences) {
                uuidsToRemove.add(g.getUuid());
            }
            LocationServices.GeofencingApi.removeGeofences(googleApiClient, uuidsToRemove);
            geofenceManager.removeFencesFromPrefs(geofences);
            geofenceCallback.onGeofencesUnmonitored(geofences);
        }
    }

    /**
     * Get a pending intent for the specified callback.
     * @param geofenceCallbackUuid the uuid of an internally mapped callback.
     * @return a <code>PendingIntent</code> instance.
     */
    private PendingIntent getPendingIntent(String geofenceCallbackUuid) {
        if (pendingIntent == null) {
            Intent intent = new Intent(context, GeofenceTransitionsService.class);
            intent.putExtra(INTENT_ID, geofenceCallbackUuid);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeofences() and removeGeofences()
            pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pendingIntent;
    }

    /**
     * Get the google API client used by this service.
     */
    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    /**
     * Load geofences from the local database if they are present, or from the server if not.
     */
    void loadGeofences() {
        Iterator<PIGeofence> it = PIGeofence.findAll(PIGeofence.class);
        if ((httpService.getServerURL() != null) && !it.hasNext()) {
            Log.v(LOG_TAG, "loadGeofences() loading geofences from the server");
            loadGeofencesFromServer(true);
        } else {
            Log.v(LOG_TAG, "loadGeofences() found geofences in local database");
            setInitialLocation();
        }
    }

    /**
     * Query the geofences from the server, based on the current anchor.
     */
    private void loadGeofencesFromServer(final boolean initialRequest) {
        PIRequestCallback<JSONObject> cb = new PIRequestCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    PIGeofenceList list = GeofencingJSONUtils.parseGeofences(result);
                    List<PIGeofence> geofences = list.getGeofences();
                    if (!geofences.isEmpty()) {
                        PIGeofence.saveInTx(geofences);
                        geofenceManager.addFences(geofences);
                        setInitialLocation();
                    } else if (initialRequest) {
                        loadGeofencesFromServer(false);
                    }
                    Log.v(LOG_TAG, "loadGeofences() got " + list.getGeofences().size() + " geofences");
                } catch (Exception e) {
                    PIRequestError error = new PIRequestError(-1, e, "error while parsing JSON");
                    Log.v(LOG_TAG, error.toString());
                }
            }

            @Override
            public void onError(PIRequestError error) {
                Log.v(LOG_TAG, error.toString());
            }
        };
        PIJSONPayloadRequest request = new PIJSONPayloadRequest(cb);
        request.setPath("geofences");
        httpService.executeRequest(request);
    }

    private void setInitialLocation() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Location last = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                Log.d(LOG_TAG, "setInitialLocation() last location = " + last);
                if (last != null) {
                    geofenceManager.onLocationChanged(last);
                }
            }
        };
        new Thread(r).start();
    }

    /**
     * Holds the mobile session information.
     */
    static class Session {
        /**
         * start timestamp.
         */
        long start;
        /**
         * duration in seconds.
         */
        long duration;

        Session(long start) {
            this.start = start;
        }

        /**
         * Convert this session into an in-app eevnt.
         * @return an {@link AEInAppEvent} instance.
         */
        /*
        AEInAppEvent toInAppEvent() {
          AEInAppEvent event = new AEInAppEvent("mobileSession");
          event.setDate(new Date());
          event.setAttribute("startDate", new Date(start));
          event.setAttribute("duration", duration);
          return event;
        }
        */
    }
}
