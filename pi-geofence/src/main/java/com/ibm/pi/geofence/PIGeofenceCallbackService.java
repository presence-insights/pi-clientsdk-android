package com.ibm.pi.geofence;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * This is the superclass for all user-provided callback services.
 */
public abstract class PIGeofenceCallbackService extends IntentService implements PIGeofenceCallback {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(PIGeofenceCallbackService.class.getSimpleName());
    private Context context;

    public PIGeofenceCallbackService() {
        super("PIGeofenceCallbackService");
    }

    public PIGeofenceCallbackService(String name) {
        super(name);
    }

    public Context getContext() {
        return context;
    }

    /**
     * Dispatches the message encapsulated in the Intent to the appropriate PIGeofenceCallback method.
     * @param intent .
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        ServiceConfig config = new ServiceConfig().fromIntent(intent);
        log.debug("onHandleIntent() config=" + config);
        context = config.createContext(this);
        switch(config.eventType) {
            case ENTER:
                onGeofencesEnter(config.geofences);
                break;
            case EXIT:
                onGeofencesExit(config.geofences);
                break;
            case MONITOR:
                onGeofencesMonitored(config.geofences);
                break;
            case UNMONITOR:
                onGeofencesUnmonitored(config.geofences);
                break;
            case SERVER_SYNC:
                PIGeofenceList list = new PIGeofenceList(config.geofences);
                list.deletedGeofenceCodes = config.deletedGeofences;
                onGeofencesSync(list);
                break;
        }
    }

    @Override
    public void onGeofencesMonitored(List<PIGeofence> geofences) {
    }

    @Override
    public void onGeofencesUnmonitored(List<PIGeofence> geofences) {
    }
}
