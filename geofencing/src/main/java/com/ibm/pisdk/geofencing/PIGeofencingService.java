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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.ibm.pisdk.doctypes.PIOrg;
import com.ibm.pisdk.geofencing.rest.HttpMethod;
import com.ibm.pisdk.geofencing.rest.PIHttpService;
import com.ibm.pisdk.geofencing.rest.PIJSONPayloadRequest;
import com.ibm.pisdk.geofencing.rest.PIRequest;
import com.ibm.pisdk.geofencing.rest.PIRequestCallback;
import com.ibm.pisdk.geofencing.rest.PIRequestError;
import com.ibm.pisdk.geofencing.rest.PISimpleRequest;

import org.apache.log4j.Logger;
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
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(PIGeofencingService.class);
    static final String INTENT_ID = "com.ibm.pisdk.geofencing.PIGeofencingService";
    /**
     * Part of a request path pointing to the geofence connector.
     */
    static final String GEOFENCE_CONNECTOR_PATH = "conn-geofence/v1";
    /**
     * Part of a request path pointing to the pi conifg connector.
     */
    static final String CONFIG_CONNECTOR_PATH = "pi-config/v2";
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
    final DelegatingGeofenceCallback geofenceCallback;
    /**
     * An internal cache of the "add geofences" requests, so the geofence transition service knows which callback to invoke.
     */
    static final Map<String, PIGeofenceCallback> callbackMap = new ConcurrentHashMap<>();
    /**
     * Pending intent used to register a set of geofences.
     */
    private PendingIntent pendingIntent;
    /**
     * Provides uniquely identifying information for the device.
     */
    private final GeofencingDeviceInfo deviceInfo;
    /**
     * Whether geofence events are posted to the PI geofence connector.
     */
    private boolean sendingGeofenceEvents = true;

    /**
     * Initialize this service.
     * @param context the Android application context.
     * @param baseURL base URL of the PI server.
     * @param tenantCode PI tenant code.
     * @param orgCode PI org code.
     * @param username PI username.
     * @param password PI password.
     * @param maxDistance distance threshold for sigificant location changes.
     * Defines the bounding box for the monitored geofences: square box with a {@code maxDistance} side centered on the current location.
     */
    public PIGeofencingService(Context context, String baseURL, String tenantCode, String orgCode, String username, String password, int maxDistance) {
        this(null, context, baseURL, tenantCode, orgCode, username, password, maxDistance);
    }

    /**
     * Initialize this service.
     * @param geofenceCallback callback invoked a geofence is triggered.
     * @param context the Android application context.
     * @param baseURL base URL of the PI server.
     * @param tenantCode PI tenant code.
     * @param orgCode PI org code.
     * @param username PI username.
     * @param password PI password.
     * @param maxDistance distance threshold for sigificant location changes.
     * Defines the bounding box for the monitored geofences: square box with a {@code maxDistance} side centered on the current location.
     */
    public PIGeofencingService(PIGeofenceCallback geofenceCallback, Context context, String baseURL, String tenantCode, String orgCode, String username, String password, int maxDistance) {
        LoggingConfiguration.configure(context);
        this.httpService = new PIHttpService(context, baseURL, tenantCode, orgCode, username, password);
        this.geofenceCallback = new DelegatingGeofenceCallback(this, geofenceCallback);
        callbackMap.put(INTENT_ID, this.geofenceCallback);
        this.context = context;
        this.deviceInfo = new GeofencingDeviceInfo(context);
        int n = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        log.debug("google play service availability = " + getGoogleAvailabilityAsText(n));
        geofenceManager = new GeofenceManager(this, maxDistance);
        GoogleLocationAPICallback serviceCallback = new GoogleLocationAPICallback(this);
        googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).addConnectionCallbacks(serviceCallback).addOnConnectionFailedListener(serviceCallback).build();
        log.debug("initGms() connecting to google play services ...");
        googleApiClient.connect();
    }

    /**
     * Provide an optional callback to receive notifications of geofence entry and exit events.
     * @param geofenceCallback the callback to set.
     */
    public void setGeofenceCallabck(PIGeofenceCallback geofenceCallback) {
        this.geofenceCallback.setDelegate(geofenceCallback);
    }

    /**
     * Send a notification to the PI geofence connector as an HTTP request.
     * @param fences the geofences for which to send a notification.
     * @param type the type of geofence notification: either {@link GeofenceNotificationType#IN IN} or {@link GeofenceNotificationType#OUT OUT}.
     */
    void postGeofenceEvent(final List<PIGeofence> fences, final GeofenceNotificationType type) {
        if (httpService.getTenantCode() == null) {
            log.warn("cannot send geofence notification because the tenant code is undefined");
        } else if (httpService.getOrgCode() == null) {
            log.warn("cannot send geofence notification because the org code is undefined");
        } else if (sendingGeofenceEvents) {
            PIRequestCallback<Void> callback = new PIRequestCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.debug("sucessfully notified connector for geofences " + fences);
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error notifying connector for geofences " + fences + " : " + error.toString());
                }
            };
            JSONObject payload = GeofencingJSONUtils.toJSONGeofenceEvent(fences, type, deviceInfo.getDescriptor());
            PIRequest request = new PISimpleRequest(callback, HttpMethod.POST, payload.toString());
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s", GEOFENCE_CONNECTOR_PATH, httpService.getTenantCode(), httpService.getOrgCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            httpService.executeRequest(request);
        }
    }

    /**
     * Create a new org with the specified parameters.
     * @param name the name of the org.
     * @param description a description of the org.
     * @param publicKey an optional public key used to encrypt data sent to the org.
     * @param useAsnewOrg whether to replace the current org with the created one in this service.
     * @param orgCallback an optional callback to receive a notification on the creation request
     * and perform actions accordingly.
     */
    public void createOrg(final String name, String description, String publicKey, final boolean useAsnewOrg, final PIRequestCallback<PIOrg> orgCallback) {
        if (httpService.getTenantCode() == null) {
            log.warn(String.format("cannot create the org '%s' because the tenant code is undefined", name));
        } else {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        com.ibm.json.java.JSONObject jsonObject = com.ibm.json.java.JSONObject.parse(result.toString());
                        PIOrg piOrg = new PIOrg(jsonObject);
                        log.debug(String.format("sucessfully created org '%s' with orgCode = %s", piOrg.getName(), piOrg.getCode()));
                        if (useAsnewOrg) httpService.setOrgCode(piOrg.getCode());
                        if (orgCallback != null) {
                            orgCallback.onSuccess(piOrg);
                        }
                    } catch(Exception e) {
                        String message = String.format("error parsing new org with name '%s'", name);
                        log.error(message, e);
                        if (orgCallback != null) {
                            orgCallback.onError(new PIRequestError(-1, e, message));
                        }
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error creating org " + name + " : " + error.toString());
                    if (orgCallback != null) {
                        orgCallback.onError(error);
                    }
                }
            };
            JSONObject json = GeofencingJSONUtils.toJSONPostOrg(name, description, publicKey);
            PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, HttpMethod.POST, json.toString());
            request.setPath(String.format(Locale.US, "%s/tenants/%s/orgs", CONFIG_CONNECTOR_PATH, httpService.getTenantCode()));
            request.setBasicAuthRequired(true);
            httpService.executeRequest(request);
        }
    }

    /**
     * Register the specified geofences with the PI server.
     * @param fences the geofences to register.
     */
    public void registerGeofences(final List<PIGeofence> fences, final PIRequestCallback<PIGeofence> userCallback) {
        log.debug("registerGeofences(" + fences + ")");
        if ((httpService.getTenantCode() != null) && (httpService.getOrgCode() != null)) {
            for (PIGeofence fence: fences) {
                registerGeofence(fence, userCallback);
            }
        }
    }

    /**
     * Register the specified single geofence with the PI server.
     * @param fence the geofence to register.
     */
    public void registerGeofence(final PIGeofence fence, final PIRequestCallback<PIGeofence> userCallback) {
        log.debug("registerGeofence(" + fence + ")");
        if ((httpService.getTenantCode() != null) && (httpService.getOrgCode() != null)) {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    log.debug("sucessfully posted geofence " + fence);
                    if (userCallback != null) {
                        try {
                            userCallback.onSuccess(GeofencingJSONUtils.parsePostGeofenceResponse(result));
                        } catch(Exception e) {
                            userCallback.onError(new PIRequestError(-1, e, "error parsing response for registration of fence " + fence));
                        }
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error posting geofence " + fence + " : " + error.toString());
                    if (userCallback != null) {
                        userCallback.onError(error);
                    }
                }
            };
            JSONObject payload = GeofencingJSONUtils.toJSONGeofence(fence);
            PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, HttpMethod.POST, payload.toString());
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s/geofences", CONFIG_CONNECTOR_PATH, httpService.getTenantCode(), httpService.getOrgCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            httpService.executeRequest(request);
        }
    }

    /**
     * Unregister the specified geofences from the PI server.
     * @param fences the geofences to unregister.
     */
    public void deleteGeofences(final List<PIGeofence> fences) {
        log.debug("deleteGeofences(" + fences + ")");
        if ((httpService.getTenantCode() != null) && (httpService.getOrgCode() != null)) {
            for (PIGeofence fence: fences) {
                deleteGeofence(fence);
            }
        }
    }

    /**
     * Unregister the specified single geofence from the PI server.
     * @param fence the geofence to unregister.
     */
    public void deleteGeofence(final PIGeofence fence) {
        log.debug("deleteGeofence(" + fence + ")");
        if ((httpService.getTenantCode() != null) && (httpService.getOrgCode() != null)) {
            PIRequestCallback<Void> callback = new PIRequestCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.debug("sucessfully deleted geofence " + fence);
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error deleting geofence " + fence + " : " + error.toString());
                }
            };
            PIRequest<Void> request = new PISimpleRequest(callback, HttpMethod.DELETE, null);
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s/geofences/%s",
                CONFIG_CONNECTOR_PATH, httpService.getTenantCode(), httpService.getOrgCode(), fence.getCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            httpService.executeRequest(request);
        }
    }

    /**
     * Add the specified geofences to the monitored geofences.
     * @param geofences the geofences to add.
     */
    public void monitorGeofences(List<PIGeofence> geofences) {
        log.debug("monitorGeofences(" + geofences + ")");
        geofences = geofenceManager.filterFromPrefs(geofences);
        if (!geofences.isEmpty()) {
            List<Geofence> list = new ArrayList<>(geofences.size());
            for (PIGeofence geofence : geofences) {
                list.add(new Geofence.Builder().setRequestId(geofence.getCode())
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
                    log.debug("add geofence request status " + status);
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
    public void unmonitorGeofences(List<PIGeofence> geofences) {
        log.debug("unmonitorGeofences(" + geofences + ")");
        geofences = geofenceManager.filterFromPrefs(geofences);
        if (!geofences.isEmpty()) {
            List<String> uuidsToRemove = new ArrayList<>(geofences.size());
            for (PIGeofence g : geofences) {
                uuidsToRemove.add(g.getCode());
            }
            LocationServices.GeofencingApi.removeGeofences(googleApiClient, uuidsToRemove);
            geofenceManager.removeFencesFromPrefs(geofences);
            geofenceCallback.onGeofencesUnmonitored(geofences);
        }
    }

    /**
     * Determine whether geofence events are posted to the PI geofence connector.
     * @return {@code true} if geofence events are posted, {@code false} otherwise.
     */
    public boolean isSendingGeofenceEvents() {
        return sendingGeofenceEvents;
    }

    /**
     * Specify whether geofence events should be posted to the PI geofence connector.
     * By default, events are posted, i.e. {@link #isSendingGeofenceEvents()} will return {@code true}.
     * @param sendingGeofenceEvents {@code true}  to enable geofence events posting, {@code false} otherwise.
     */
    public void setSendingGeofenceEvents(boolean sendingGeofenceEvents) {
        this.sendingGeofenceEvents = sendingGeofenceEvents;
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
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling monitorGeofences() and unmonitorGeofences()
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
            log.debug("loadGeofences() loading geofences from the server");
            loadGeofencesFromServer(true);
        } else {
            log.debug("loadGeofences() found geofences in local database");
            setInitialLocation();
        }
    }

    /**
     * Query the geofences from the server, based on the current anchor.
     */
    private void loadGeofencesFromServer(final boolean initialRequest) {
        if ((httpService.getTenantCode() != null) && (httpService.getOrgCode() != null)) {
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
                        log.debug("loadGeofences() got " + list.getGeofences().size() + " geofences");
                    } catch (Exception e) {
                        PIRequestError error = new PIRequestError(-1, e, "error while parsing JSON");
                        log.debug(error.toString());
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.debug(error.toString());
                }
            };
            PIJSONPayloadRequest request = new PIJSONPayloadRequest(cb, HttpMethod.GET, null);
            request.setPath(String.format("%s/tenants/%s/orgs/%s/geofences", CONFIG_CONNECTOR_PATH, httpService.getTenantCode(), httpService.getOrgCode()));
            httpService.executeRequest(request);
        }
    }

    /**
     * Set the initial location upon starting the app and trigger the registration of geofences, if any, around this location.
     */
    private void setInitialLocation() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Location last = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                log.debug("setInitialLocation() last location = " + last);
                if (last != null) {
                    geofenceManager.onLocationChanged(last, true);
                }
            }
        };
        new Thread(r).start();
    }

    /**
     * Converts a Google play services availability code into a displayable string. Used for debugging and tracing purposes.
     * @param availabilityCode the google api connection result to convert.
     * @return a readable string.
     */
    private String getGoogleAvailabilityAsText(int availabilityCode) {
        switch(availabilityCode) {
            case ConnectionResult.SUCCESS: return "SUCCESS";
            case ConnectionResult.SERVICE_MISSING: return "SERVICE_MISSING";
            case ConnectionResult.SERVICE_UPDATING: return "SERVICE_UPDATING";
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED: return "SERVICE_VERSION_UPDATE_REQUIRED";
            case ConnectionResult.SERVICE_DISABLED: return "SERVICE_DISABLED";
            case ConnectionResult.SERVICE_INVALID: return "SERVICE_INVALID";
            default: return "undefined";
        }
    }
}
