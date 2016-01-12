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

import android.util.Log;

import com.ibm.pisdk.geofencing.PIGeofence;
import com.ibm.pisdk.geofencing.PIGeofenceCallback;

import java.util.List;

/**
 * This callback manages updates to the map based on geofence events.
 */
public class MyGeofenceCallback implements PIGeofenceCallback {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = MyGeofenceCallback.class.getSimpleName();
    private static final String SLACK_CHANNEL = "#geo-spam";
    //#private static final String SLACK_CHANNEL = "@lolo4j";
    private final MapsActivity activity;
    private final SlackHTTPService slackService;

    public MyGeofenceCallback(final MapsActivity activity) {
        this.activity = activity;
        this.slackService = new SlackHTTPService(activity);
    }

    @Override
    public void onGeofencesMonitored(final List<PIGeofence> geofences) {
        Log.v(LOG_TAG, "onGeofencesMonitored() geofences = " + geofences);
        new Thread(new Runnable() {
            @Override
            public void run() {
                activity.startSimulation(geofences);
            }
        }).start();
    }

    @Override
    public void onGeofencesUnmonitored(List<PIGeofence> geofences) {
        Log.v(LOG_TAG, "onGeofencesUnmonitored() geofences = " + geofences);
    }

    @Override
    public void onGeofencesEnter(final List<PIGeofence> geofences) {
        Log.v(LOG_TAG, "entering geofence(s) " + geofences);
        updateUI(geofences, true);
        slackService.postGeofenceMessages(geofences, "enter", SLACK_CHANNEL);
    }

    @Override
    public void onGeofencesExit(final List<PIGeofence> geofences) {
        Log.v(LOG_TAG, "exiting geofence(s) " + geofences);
        updateUI(geofences, false);
        slackService.postGeofenceMessages(geofences, "exit", SLACK_CHANNEL);
    }

    void updateUI(final List<PIGeofence> geofences, final boolean isEntry) {
        for (final PIGeofence geofence : geofences) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PIGeofence g = activity.getGeofenceManager().getGeofence(geofence.getUuid());
                    Log.v(LOG_TAG, "Geofence " + (isEntry ? "entry" : "exit") + " for " + g + " (uuid=" + geofence + ")");
                    if (g != null) {
                        activity.refreshGeofenceInfo(g, isEntry);
                        activity.updateCurrentMarker();
                    }
                }
            });
        }
    }
}
