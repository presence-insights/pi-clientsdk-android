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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.ibm.pi.geofence.rest.HttpMethod;
import com.ibm.pi.geofence.rest.PIHttpService;
import com.ibm.pi.geofence.rest.PIJSONPayloadRequest;
import com.ibm.pi.geofence.rest.PIRequest;
import com.ibm.pi.geofence.rest.PIRequestCallback;
import com.ibm.pi.geofence.rest.PIRequestError;
import com.ibm.pisdk.geofencing.BuildConfig;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Provides an API to load geofences from, and send entry/exit to, the server.
 */
public class PIGeofencingManager {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(PIGeofencingManager.class.getSimpleName());
    static final String INTENT_ID = "PIGeofencingService";
    /**
     * Part of a request path pointing to the geofence connector.
     */
    static final String GEOFENCE_CONNECTOR_PATH = "conn-geofence/v1";
    /**
     * Part of a request path pointing to the pi conifg connector.
     */
    static final String CONFIG_CONNECTOR_PATH = "pi-config/v2";
    /**
     * Mode indicating this geofence manager is executed by the user-provided app.
     */
    static final int MODE_APP = 1;
    /**
     * Mode indicating this geofence manager is executed by the geofence transition service.
     */
    static final int MODE_GEOFENCE_EVENT = 2;
    /**
     * Mode indicating this geofence manager is executed by the significant location change service.
     */
    static final int MODE_MONITORING_REQUEST = 3;
    /**
     * Mode indicating this geofence manager is executed by the reboot handler service.
     */
    static final int MODE_REBOOT = 4;
    /**
     * The restful service which connects to and communicates with the Adaptive Experience server.
     */
    final PIHttpService mHttpService;
    /**
     * The Google API client.
     */
    GoogleApiClient mGoogleApiClient;
    /**
     * The Android application context.
     */
    Context mContext;
    /**
     * Handles the geofences that are currently monitored.
     */
    final int mMaxDistance;
    /**
     * Pending intent used to register a set of geofences.
     */
    private PendingIntent mPendingIntent;
    /**
     * Provides uniquely identifying information for the device.
     */
    private final String mDeviceDescriptor;
    /**
     * The settings of the application.
     */
    Settings mSettings;
    /**
     * The execution mode for this gefoence manager;
     * one of {@link #MODE_APP}, {@link #MODE_GEOFENCE_EVENT}, {@link #MODE_MONITORING_REQUEST} or {@link #MODE_REBOOT}.
     */
    final int mMode;
    /**
     * A callback that immplements the Google API connection callback interfaces.
     */
    GoogleLocationAPICallback mGoogleAPICallback;
    /**
     * The minimum delay between two synchronizations with the server.
     */
    int mIntervalBetweenDowloads = 24;

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
    public PIGeofencingManager(Context context, String baseURL, String tenantCode, String orgCode, String username, String password, int maxDistance) {
        this(null, MODE_APP, context, baseURL, tenantCode, orgCode, username, password, maxDistance);
    }

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
    PIGeofencingManager(Settings settings, int mode, Context context, String baseURL, String tenantCode, String orgCode, String username, String password, int maxDistance) {
        log.debug("pi-geofence version " + BuildConfig.VERSION_NAME);
        this.mMode = mode;
        this.mMaxDistance = maxDistance;
        this.mHttpService = new PIHttpService(context, baseURL, tenantCode, orgCode, username, password);
        this.mContext = context;
        this.mSettings = (settings != null) ? settings : new Settings(context);
        log.debug("PIGeofencingService() settings = " + this.mSettings);
        this.mIntervalBetweenDowloads = this.mSettings.getInt(ServiceConfig.SERVER_SYNC_MIN_DELAY_HOURS, 24);
        this.mDeviceDescriptor = retrieveDeviceDescriptor();
        int n = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        log.debug("google play service availability = " + getGoogleAvailabilityAsText(n));
        if (this.mMode == MODE_APP) {
            //initSettingsData();
            updateSettings();
        }
        connectGoogleAPI();
    }

    /**
     * Connect the Google API client. The synchronous / asynchronous mode depends on the {@link #mMode} of this geofence manager.
     */
    private void connectGoogleAPI() {
        if ((mContext != null) && (mMode != MODE_GEOFENCE_EVENT)) {
            mGoogleAPICallback = new GoogleLocationAPICallback(this);
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mGoogleAPICallback)
                .addOnConnectionFailedListener(mGoogleAPICallback)
                .build();
            log.debug("initGms() connecting to google play services ...");
            if ((mMode == MODE_MONITORING_REQUEST) || (mMode == MODE_REBOOT)) {
                try {
                    // can't run blockingConnect() on the UI thread
                    ConnectionResult result = new AsyncTask<Void, Void, ConnectionResult>() {
                        @Override
                        protected ConnectionResult doInBackground(Void... params) {
                            return mGoogleApiClient.blockingConnect(60_000L, TimeUnit.MILLISECONDS);
                        }
                    }.execute().get();
                    log.debug(String.format("google api connection %s, result=%s", (result.isSuccess() ? "success" : "error"), result));
                } catch(Exception e) {
                    log.error("error while attempting connection to google api", e);
                }
            } else if (mMode == MODE_APP) {
                mGoogleApiClient.connect();
            }
        }
    }

    /**
     * Send a notification to the PI geofence connector as an HTTP request.
     * @param fences the geofences for which to send a notification.
     * @param type the type of geofence notification: either {@link PIGeofenceEvent.Type#ENTER ENTER} or {@link PIGeofenceEvent.Type##EXIT EXIT}.
     */
    void postGeofenceEvent(final List<PersistentGeofence> fences, final PIGeofenceEvent.Type type) {
        if (mHttpService.getTenantCode() == null) {
            log.warn("cannot send geofence notification because the tenant code is undefined");
        } else if (mHttpService.getOrgCode() == null) {
            log.warn("cannot send geofence notification because the org code is undefined");
        } else {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    log.debug("sucessfully notified connector for geofences " + fences);
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error notifying connector for geofences " + fences + " : " + error.toString());
                }
            };
            JSONObject payload = GeofencingJSONUtils.toJSONGeofenceEvent(fences, type, mDeviceDescriptor, BuildConfig.VERSION_NAME);
            PIRequest<JSONObject> request = new PIJSONPayloadRequest(callback, HttpMethod.POST, payload.toString());
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s", GEOFENCE_CONNECTOR_PATH, mHttpService.getTenantCode(), mHttpService.getOrgCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            mHttpService.executeRequest(request);
        }
    }

    /**
     * Add the specified geofences to the monitored geofences.
     * @param geofences the geofences to add.
     */
    void monitorGeofences(List<PersistentGeofence> geofences) {
        if (!geofences.isEmpty()) {
            log.debug("monitorGeofences(" + geofences + ")");
            List<Geofence> list = new ArrayList<>(geofences.size());
            List<Geofence> noTriggerList = new ArrayList<>(geofences.size());
            Location last = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            for (PersistentGeofence geofence : geofences) {
                Geofence fence = new Geofence.Builder().setRequestId(geofence.getCode())
                    .setCircularRegion(geofence.getLatitude(), geofence.getLongitude(), (float) geofence.getRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(10_000)
                    .setLoiteringDelay(300000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

                Location location = new Location(LocationManager.NETWORK_PROVIDER);
                location.setLatitude(geofence.getLatitude());
                location.setLongitude(geofence.getLongitude());
                if ((mMode != MODE_REBOOT) || (last == null) || (location.distanceTo(last) > geofence.getRadius())) {
                    list.add(fence);
                } else {
                    // if already in geofence, do not trigger upon registration.
                    noTriggerList.add(fence);
                }
            }
            registerFencesForMonitoring(list, GeofencingRequest.INITIAL_TRIGGER_ENTER);
            registerFencesForMonitoring(noTriggerList, 0);
        }
    }

    private void registerFencesForMonitoring(List<Geofence> fences, int initialTrigger) {
        if (!fences.isEmpty()) {
            GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(initialTrigger)
                .addGeofences(fences).build();
            PendingIntent pi = getPendingIntent(INTENT_ID);
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, request, pi).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    log.debug("add geofence request status " + status);
                }
            });
        }
    }

    /**
     * Remove the specified geofences from the monitored geofences.
     * @param geofences the geofences to remove.
     */
    void unmonitorGeofences(List<PersistentGeofence> geofences) {
        log.debug("unmonitorGeofences(" + geofences + ")");
        if (!geofences.isEmpty()) {
            List<String> uuidsToRemove = new ArrayList<>(geofences.size());
            for (PersistentGeofence g : geofences) {
                uuidsToRemove.add(g.getCode());
            }
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, uuidsToRemove);
        }
    }

    /**
     * Get the minimum delay in hours between two synchronizations with the server.
     * When not already set, the default value is 24 hours.
     * @return the minimum number of hours between two server synchronizations.
     */
    public int getIntervalBetweenDowloads() {
        return mIntervalBetweenDowloads;
    }

    /**
     * Set the minimum delay in hours between two synchronizations with the server.
     * If (the specified value is less than 1, then this method has no effect.
     * @param intervalBetweenDowloads the minimum number of hours between two server synchronizations.
     */
    public void setIntervalBetweenDowloads(int intervalBetweenDowloads) {
        if (intervalBetweenDowloads >= 1) {
            this.mIntervalBetweenDowloads = intervalBetweenDowloads;
            updateSettings();
        }
    }

    /**
     * Load a set of geofences from a reosurce file.
     * @param resource the path to the resource to load the geofences from.
     */
    public void loadGeofencesFromResource(final String resource) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            private GeofenceList geofenceList;
            private PIRequestError error;

            @Override
            protected Void doInBackground(Void... params) {
                ZipInputStream zis = null;
                try {
                    InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
                    zis = new ZipInputStream(is);
                    ZipEntry entry;
                    Map<String, PersistentGeofence> allGeofences = new HashMap<>();
                    long maxSyncTimestamp = 0L;
                    while ((entry = zis.getNextEntry()) != null) {
                        byte[] bytes = GeofencingUtils.loadBytes(zis);
                        if (bytes != null) {
                            int fileSize = bytes.length;
                            JSONObject json = new JSONObject(new String(bytes, "UTF-8"));
                            bytes = null; // the byte[] may be large, we make sure it can be GC-ed ASAP
                            GeofenceList list = GeofencingJSONUtils.parseGeofences(json);
                            List<PersistentGeofence> geofences = list.getGeofences();
                            if ((geofences != null) && !geofences.isEmpty()) {
                                PersistentGeofence.saveInTx(geofences);
                                log.debug(String.format(Locale.US, "loaded %,d geofences from resource '[%s]/%s' (%,d bytes)",
                                    geofences.size(), resource, entry.getName(), fileSize));
                            }

                            if (list.getLastSyncTimestamp() >= 0L) {
                                if (list.getLastSyncTimestamp() > maxSyncTimestamp) {
                                    maxSyncTimestamp = list.getLastSyncTimestamp();
                                }
                            }
                            for (PersistentGeofence pg: list.getGeofences()) {
                                allGeofences.put(pg.getCode(), pg);
                            }
                        }
                        else {
                            log.debug(String.format("the zip entry [%s]/%s is empty", resource, entry.getName()));
                        }
                    }
                    if (maxSyncTimestamp >= 0L) {
                        mSettings.putLong(ServiceConfig.LAST_SYNC_TIMESTAMP, maxSyncTimestamp).commit();
                    }
                    geofenceList = new GeofenceList(new ArrayList<>(allGeofences.values()));
                    log.debug(String.format(Locale.US, "loaded %,d geofences from resource '[%s]', maxSyncDate=%s",
                        allGeofences.size(), resource, maxSyncTimestamp));
                } catch(Exception e) {
                    error = new PIRequestError(-1, e, String.format("error loading resource '%s'", resource));
                } finally {
                    try {
                        zis.close();
                    } catch(Exception e) {
                        log.error(String.format("error closing zip input stream for resource %s", resource), e);
                        if (error == null) {
                            error = new PIRequestError(-1, e, String.format("error loading resource '%s'", resource));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (error != null) {
                    log.error(String.format("error loading resource %s : %s", resource, error));
                } else {
                    try {
                        Intent broadcastIntent = new Intent(PIGeofenceEvent.ACTION_GEOFENCE_EVENT);
                        broadcastIntent.setPackage(mContext.getPackageName());
                        PIGeofenceEvent.toIntent(broadcastIntent, PIGeofenceEvent.Type.SERVER_SYNC, geofenceList.getGeofences(), null);
                        mContext.sendBroadcast(broadcastIntent);
                    } catch(Exception e) {
                        log.error("error sending broadcast event", e);
                    }
                }
            }
        };
        task.execute();
    }

    /**
     * Get the path to the log file.
     * @return the full path to the log file on the file system.
     */
    public static String getLogFilePath() {
        return LoggingConfiguration.getLogFile();
    }

    /**
     * Get a pending intent for the specified callback.
     * @param geofenceCallbackUuid the uuid of an internally mapped callback.
     * @return a <code>PendingIntent</code> instance.
     */
    private PendingIntent getPendingIntent(String geofenceCallbackUuid) {
        if (mPendingIntent == null) {
            Intent intent = new Intent(mContext, GeofenceTransitionsService.class);
            //intent.setPackage(context.getPackageName());
            ServiceConfig config = new ServiceConfig().fromGeofencingManager(this);
            config.populateFromSettings(mSettings);
            config.toIntent(intent);
            intent.putExtra(INTENT_ID, geofenceCallbackUuid);
            intent.setClass(mContext, GeofenceTransitionsService.class);
            mPendingIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mPendingIntent;
    }

    /**
     * Load geofences from the local database if they are present, or from the server if not.
     */
    void loadGeofences() {
        if (mHttpService.getServerURL() != null) {
            log.debug("loadGeofences() loading geofences from the server");
            loadGeofencesFromServer();
        } else {
            log.debug("loadGeofences() found geofences in local database");
            setInitialLocation();
        }
    }

    /**
     * Query the geofences from the server, based on the current anchor.
     */
    private void loadGeofencesFromServer() {
        if (PersistentGeofence.count(PersistentGeofence.class) <= 0) {
            loadGeofencesFromServer(-1L);
        } else {
            long now = System.currentTimeMillis();
            long lastTimeStamp = mSettings.getLong(ServiceConfig.SERVER_SYNC_LOCAL_TIMESTAMP, -1L);
            if ((lastTimeStamp < 0L) || (now - lastTimeStamp >= mIntervalBetweenDowloads * 3600L * 1000L)) {
                loadGeofencesFromServer(mSettings.getLong(ServiceConfig.LAST_SYNC_TIMESTAMP, -1L));
            }
        }
    }

    /**
     * Query the geofences from the server, based on the current last sync date.
     */
    private void loadGeofencesFromServer(long lastSyncTimestamp) {
        if ((mHttpService.getTenantCode() != null) && (mHttpService.getOrgCode() != null)) {
            PIRequestCallback<JSONObject> cb = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        GeofenceList list = GeofencingJSONUtils.parseGeofences(result);
                        if (list.getLastSyncTimestamp() >= 0L) {
                            mSettings.putLong(ServiceConfig.LAST_SYNC_TIMESTAMP, list.getLastSyncTimestamp()).commit();
                        }
                        List<PersistentGeofence> geofences = list.getGeofences();
                        if (!geofences.isEmpty()) {
                            PersistentGeofence.saveInTx(geofences);
                            setInitialLocation();
                        }
                        if (!geofences.isEmpty() || !list.getDeletedGeofenceCodes().isEmpty()) {
                            Intent broadcastIntent = new Intent(PIGeofenceEvent.ACTION_GEOFENCE_EVENT);
                            broadcastIntent.setPackage(mContext.getPackageName());
                            PIGeofenceEvent.toIntent(broadcastIntent, PIGeofenceEvent.Type.SERVER_SYNC, geofences, list.getDeletedGeofenceCodes());
                            //LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                            mContext.sendBroadcast(broadcastIntent);
                        }
                        mSettings.putLong(ServiceConfig.SERVER_SYNC_LOCAL_TIMESTAMP, System.currentTimeMillis()).commit();
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
            request.setPath(String.format("%s/tenants/%s/orgs/%s/geofences", CONFIG_CONNECTOR_PATH, mHttpService.getTenantCode(), mHttpService.getOrgCode()));
            request.addParameter("paginate", "false");
            if (lastSyncTimestamp >= 0L) {
                request.addParameter("updatedAfter", Long.toString(lastSyncTimestamp));
            }
            mHttpService.executeRequest(request);
        }
    }

    /**
     * Set the initial location upon starting the app and trigger the registration of geofences, if any, around this location.
     */
    private void setInitialLocation() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Location last = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                log.debug("setInitialLocation() last location = " + last);
                if (last != null) {
                    new LocationUpdateReceiver(PIGeofencingManager.this).onLocationChanged(last, true);
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

    private void updateSettings() {
        mSettings.putString(ServiceConfig.SERVER_URL, mHttpService.getServerURL())
            .putString(ServiceConfig.TENANT_CODE, mHttpService.getTenantCode())
            .putString(ServiceConfig.ORG_CODE, mHttpService.getOrgCode())
            .putString(ServiceConfig.USERNAME, mHttpService.getUsername())
            .putString(ServiceConfig.PASSWORD, mHttpService.getPassword())
            .putInt(ServiceConfig.MAX_DISTANCE, mMaxDistance)
            .putInt(ServiceConfig.SERVER_SYNC_MIN_DELAY_HOURS, mIntervalBetweenDowloads)
            .commit();
    }

    /**
     * Retrieve the last used device descritpor, if any. If none exists, one is created from the {@link com.ibm.pi.core.PIDeviceInfo PIDeviceInfo} API.
     * @return the device descriptor.
     */
    String retrieveDeviceDescriptor() {
        String result = mSettings.getString(GeofencingDeviceInfo.PI_DESCRIPTOR_KEY, null);
        if (result == null) {
            SharedPreferences prefs = mContext.getSharedPreferences(GeofencingDeviceInfo.PI_SHARED_PREF, Context.MODE_PRIVATE);
            result = prefs.getString(GeofencingDeviceInfo.PI_DESCRIPTOR_KEY, null);
            if (result == null) {
                GeofencingDeviceInfo info = new GeofencingDeviceInfo(mContext);
                result = info.getDescriptor();
                prefs.edit().putString(GeofencingDeviceInfo.PI_DESCRIPTOR_KEY, result).apply();
            }
            mSettings.putString(GeofencingDeviceInfo.PI_DESCRIPTOR_KEY, result).commit();
        }
        return result;
    }
}
