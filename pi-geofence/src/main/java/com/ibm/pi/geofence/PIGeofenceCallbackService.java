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

package com.ibm.pi.geofence;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This is the superclass for all user-provided callback services.
 */
public abstract class PIGeofenceCallbackService extends IntentService implements PIGeofenceCallback {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(PIGeofenceCallbackService.class.getSimpleName());
    private Context context;
    /**
     * Binder given to clients.
      */
    private final IBinder binder = new LocalBinder();
    private transient List<PIGeofenceCallback> callbacks = new CopyOnWriteArrayList<>();

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
        List<PIGeofence> geofences = null;
        switch(config.eventType) {
            case ENTER:
                geofences = PersistentGeofence.toPIGeofences(config.geofences);
                for (PIGeofenceCallback callback: callbacks) {
                    callback.onGeofencesEnter(geofences);
                }
                onGeofencesEnter(geofences);
                break;
            case EXIT:
                geofences = PersistentGeofence.toPIGeofences(config.geofences);
                for (PIGeofenceCallback callback: callbacks) {
                    callback.onGeofencesExit(geofences);
                }
                onGeofencesExit(geofences);
                break;
            case SERVER_SYNC:
                geofences = PersistentGeofence.toPIGeofences(config.geofences);
                for (PIGeofenceCallback callback: callbacks) {
                    callback.onGeofencesSync(geofences, config.deletedGeofences);
                }
                onGeofencesSync(geofences, config.deletedGeofences);
                break;
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        PIGeofenceCallbackService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PIGeofenceCallbackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    void addCallback(PIGeofenceCallback callback) {
        if (callback != null) {
            callbacks.add(callback);
        }
    }

    void removeCallback(PIGeofenceCallback callback) {
        if (callback != null) {
            callbacks.remove(callback);
        }
    }
}
