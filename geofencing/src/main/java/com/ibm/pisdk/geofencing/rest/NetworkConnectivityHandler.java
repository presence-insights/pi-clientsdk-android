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

package com.ibm.pisdk.geofencing.rest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.orm.SugarRecord;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * This class handles notifications of loss of connectivity to the server.
 * There are two scenarios for a loss of connectivity:
 * <ul>
 *   <li>the network is down on the device side</li>
 *   <li>the server is down or cannot be otherwise reached</li>
 * </ul>
 * Notifications of connectivity lost or back on are send by calling the {@link #onInactiveNetwork()} and {@link #onActiveNetwork()} methods.
 * This is done from two sources:
 * <ul>
 *   <li>the async task that executes HTTP requests: class {@link com.ibm.pisdk.geofencing.rest.RequestAsyncTask}</li>
 *   <li>from a timer task which checks periodically whether the network is connected, via the {@link ConnectivityManager} API</li>
 * </ul>
 */
class NetworkConnectivityHandler {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(NetworkConnectivityHandler.class);
    private static final Pattern AMP_PATTERN = Pattern.compile("&");
    private static final Pattern EQUAL_PATTERN = Pattern.compile("=");
    private final Context context;
    private final PIHttpService httpService;
    private AtomicBoolean networkActive = new AtomicBoolean(true);
    private Timer timer;
    private boolean enabled;

    NetworkConnectivityHandler(Context context, PIHttpService httpService, boolean enabled) {
        this.context = context;
        this.httpService = httpService;
        this.enabled = enabled;
        if (enabled) startTimer();
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the connectivity is currently active.
     */
    boolean isNetworkActive() {
        return networkActive.get();
    }

    void startTimer() {
        timer = new Timer("NetworkConnectivity Timer", true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (checkNetworkAvailable(context)) {
                    onActiveNetwork();
                } else {
                    onInactiveNetwork();
                }
            }
        };
        // check every 10 seconds.
        timer.schedule(task, 10_000L, 10_000L);
    }

    void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * Check whether any network connectivity is available, using the Android {@link ConnectivityManager} API.
     */
    private boolean checkNetworkAvailable(Context androidContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) androidContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null) && activeNetworkInfo.isConnected();
    }

    /**
     * Called upon detection that network or server connectivity is available.
     */
    void onActiveNetwork() {
        if (networkActive.compareAndSet(false, true)) {
            log.debug("onActiveNetwork() network connectivity back on");
            synchronized (this) {
                httpService.executor = Executors.newSingleThreadExecutor();
                Iterator<PersistentRequest> persisted = PersistentRequest.findAll(PersistentRequest.class);
                int count = 0;
                while (persisted.hasNext()) {
                    PIRequest<?> request = persisted.next().toRequest();
                    httpService.executeRequest(request);
                    count++;
                }
                PersistentRequest.deleteAll(PersistentRequest.class);
                log.debug("onActiveNetwork() restored " + count + " requests");
            }
        }
    }

    /**
     * Called upon detection that network or server connectivity is unavailable.
     */
    void onInactiveNetwork() {
        if (networkActive.compareAndSet(true, false)) {
            synchronized (this) {
                log.debug("detected loss of network connectivity");
                List<Runnable> pendingTasks = httpService.executor.shutdownNow();
                if (!pendingTasks.isEmpty()) {
                    log.debug("onInctiveNetwork() persisting " + pendingTasks.size() + " requests");
                    List<PersistentRequest> requests = new ArrayList<>();
                    for (Runnable task : pendingTasks) {
                        PIRequest<?> request = ((PIHttpService.RequestTask) task).request;
                        requests.add(new PersistentRequest(request));
                    }
                    PersistentRequest.saveInTx(requests);
                }
            }
        }
    }

    /**
     * Persist a single HTTP request that either timed out or was queued before loss of ocnnectivity.
     */
    void persistRequest(PIRequest<?> request) {
        synchronized (this) {
            log.debug("persistRequest() presisting " + request);
            new PersistentRequest(request).save();
        }
    }

    /**
     * This class is a persistable wrapper for the HTTP requests of type {@link PIRequest}.
     * Instances of this class are intented for potentially permanent storage in an Sqlite database.
     */
    public static class PersistentRequest extends SugarRecord<PersistentRequest> {
        /**
         * Additional arbitrary parameters added to the query, in URL-encoded format.
         */
        String parameters;
        /**
         * Path portion of the URL, added to the tenant and application parts.
         */
        String path = null;
        /**
         * HTTP request method to use.
         */
        String method = "GET";
        /**
         * The payload to send along with the request (for PUT and POST requests).
         */
        String payload;

        public PersistentRequest(PIRequest<?> request) {
            this.method = request.getMethod().name();
            this.payload = request.getPayload();
            this.path = request.getPath();
            try {
                // serialize query params to string format (URL-encoded)
                this.parameters = request.addParams(new StringBuilder()).toString();
            } catch (Exception e) {
                log.error("error converting query parameters", e);
            }
        }

        /**
         * Deserialize query params from string format.
         */
        List<QueryParameter> parseQueryParameters() {
            List<QueryParameter> list = new ArrayList<>();
            if ((parameters != null) && !parameters.isEmpty()) {
                log.debug("parseQueryParameters() params = " + parameters);
                String s = parameters.startsWith("?") ? parameters.substring(1) : parameters;
                String[] params = AMP_PATTERN.split(s);
                for (String param : params) {
                    String[] comp = EQUAL_PATTERN.split(param);
                    log.debug("parseQueryParameters() parsing param = " + Arrays.asList(comp));
                    list.add(new QueryParameter(Utils.urlDeccode(comp[0]), Utils.urlDeccode(comp[1])));
                }
            }
            return list;
        }

        /**
         * Create and populate (i.e. deserialize) an HTTP request from its persisted format.
         */
        PIRequest<?> toRequest() {
            PIRequestCallback<JSONObject> cb = null;
            /*
            switch(requestType) {
            case PIRequest.USER_CONTEXT:
                  cb = new DefaultUserContextRequestCallback(null);
                  break;

            case PIRequest.REMOTE_VARIABLE:
                  cb = new DefaultRemoteVariablesRequestCallback(null);
                  break;
            default:
                  break;
            }
            PIRequest<?> request = new PIJSONPayloadRequest(cb, method, payload);
            request.setPath(path);
            request.parameters.addAll(parseQueryParameters());
            return request;
            */
            return null;
        }
    }
}
