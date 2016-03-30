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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.ibm.pi.geofence.PIGeofence;
import com.ibm.pi.geofence.PIGeofenceCallback;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This callback manages updates to the map based on geofence events.
 */
public class MyGeofenceCallback implements PIGeofenceCallback {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(MyGeofenceCallback.class);
    private static final String SLACK_CHANNEL = "#geo-spam";
    private static final AtomicInteger notifId = new AtomicInteger(0);
    //#private static final String SLACK_CHANNEL = "@lolo4j";
    private final MapsActivity activity;
    private final SlackHTTPService slackService;

    public MyGeofenceCallback(final MapsActivity activity) {
        this.activity = activity;
        this.slackService = new SlackHTTPService(activity);
    }

    public void onGeofencesMonitored(final List<PIGeofence> geofences) {
        log.debug("onGeofencesMonitored() geofences = " + geofences);
        activity.initGeofences();
        updateUI(geofences, false);
    }

    public void onGeofencesUnmonitored(List<PIGeofence> geofences) {
        log.debug("onGeofencesUnmonitored() geofences = " + geofences);
        activity.initGeofences();
        updateUI(geofences, false);
    }

    @Override
    public void onGeofencesEnter(final List<PIGeofence> geofences) {
        log.debug("entering geofence(s) " + geofences);
        if (activity.trackingEnabled) {
            updateUI(geofences, true);
            sendNotification(geofences, "enter");
            slackService.postGeofenceMessages(geofences, "enter", SLACK_CHANNEL);
        }
    }

    @Override
    public void onGeofencesExit(final List<PIGeofence> geofences) {
        log.debug("exiting geofence(s) " + geofences);
        if (activity.trackingEnabled) {
            updateUI(geofences, false);
            sendNotification(geofences, "exit");
            slackService.postGeofenceMessages(geofences, "exit", SLACK_CHANNEL);
        }
    }

    @Override
    public void onGeofencesSync(List<PIGeofence> geofences, List<String> deletedGeofenceCodes) {
    }

    void updateUI(final List<PIGeofence> geofences, final boolean isEntry) {
        for (final PIGeofence geofence : geofences) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PIGeofence g = activity.getGeofenceHolder().getGeofence(geofence.getCode());
                    log.debug("Geofence " + (isEntry ? "entry" : "exit") + " for " + g + " (uuid=" + geofence + ")");
                    if (g != null) {
                        activity.refreshGeofenceInfo(g, isEntry);
                        activity.updateCurrentMarker();
                    }
                }
            });
        }
    }

    private void sendNotification(List<PIGeofence> fences, String type) {
        String title = "Geofence: " + type;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (PIGeofence g: fences) {
            if (count > 0) sb.append('\n');
            sb.append(String.format("%s : lat=%.6f; lng=%.6f; radius=%.0f m", g.getName(), g.getLatitude(), g.getLongitude(), g.getRadius()));
            count++;
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(activity)
            .setAutoCancel(true)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title).setContentText(sb.toString());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(activity, MapsActivity.class);

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MapsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(notifId.incrementAndGet(), mBuilder.build());
    }
}
