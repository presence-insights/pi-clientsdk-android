package com.ibm.pi.geofence.demo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.ibm.pi.geofence.LoggingConfiguration;
import com.ibm.pi.geofence.PIGeofence;
import com.ibm.pi.geofence.PIGeofenceCallbackService;
import com.ibm.pi.geofence.Settings;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class MyCallbackService extends PIGeofenceCallbackService {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(MyCallbackService.class.getSimpleName());
    private static final String SLACK_CHANNEL = "#geo-spam";
    private static final AtomicInteger notifId = new AtomicInteger(0);
    //#private static final String SLACK_CHANNEL = "@lolo4j";
    private final SlackHTTPService slackService;
    private Settings settings;

    public MyCallbackService() {
        this("callback service");
    }

    public MyCallbackService(String name) {
        super(name);
        this.slackService = new SlackHTTPService(null);
    }

    @Override
    public void onGeofencesEnter(final List<PIGeofence> geofences) {
        log.debug("entering geofence(s) " + geofences);
        if (getSettings().getBoolean(MapsActivity.TRACKING_ENABLED_KEY, true)) {
            //updateUI(geofences, true);
            sendNotification(geofences, "enter");
            slackService.postGeofenceMessages(geofences, "enter", SLACK_CHANNEL);
        }
    }

    @Override
    public void onGeofencesExit(final List<PIGeofence> geofences) {
        log.debug("exiting geofence(s) " + geofences);
        if (getSettings().getBoolean(MapsActivity.TRACKING_ENABLED_KEY, true)) {
            //updateUI(geofences, false);
            sendNotification(geofences, "exit");
            slackService.postGeofenceMessages(geofences, "exit", SLACK_CHANNEL);
        }
    }

    /*
    void updateUI(final List<PIGeofence> geofences, final boolean isEntry) {
        for (final PIGeofence geofence : geofences) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PIGeofence g = context.getGeofenceManager().getGeofence(geofence.getCode());
                    log.debug("Geofence " + (isEntry ? "entry" : "exit") + " for " + g + " (uuid=" + geofence + ")");
                    if (g != null) {
                        context.refreshGeofenceInfo(g, isEntry);
                        context.updateCurrentMarker();
                    }
                }
            });
        }
    }
    */

    private void sendNotification(List<PIGeofence> fences, String type) {
        Context context = getContext();
        String title = "Geofence: " + type;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (PIGeofence g: fences) {
            if (count > 0) sb.append('\n');
            sb.append(String.format("%s : lat=%.6f; lng=%.6f; radius=%.0f m", g.getName(), g.getLatitude(), g.getLongitude(), g.getRadius()));
            count++;
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setAutoCancel(true)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title).setContentText(sb.toString());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MapsActivity.class);

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MapsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(notifId.incrementAndGet(), mBuilder.build());
    }

    public Settings getSettings() {
        if (settings == null) {
            settings = new Settings(getContext());
        }
        return settings;
    }
}
