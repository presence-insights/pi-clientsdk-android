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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * This Android service receives events from the Geofencing API and invokes the AEX geofence callback accordingly.
 */
public class GeofenceTransitionsService extends IntentService {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(GeofenceTransitionsService.class.getSimpleName());

    public GeofenceTransitionsService() {
        super(GeofenceTransitionsService.class.getName());
    }

    public GeofenceTransitionsService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            handleIntent(intent);
        } catch(Exception e) {
            log.error("exception occurred in handleIntent(): ", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleIntent(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        int transition = event.getGeofenceTransition();
        ServiceConfig config = new ServiceConfig().fromIntent(intent);
        log.debug(String.format("in onHandleIntent() transition=%d, event=%s, config=%s", transition, event, config));
        if (event.hasError()) {
            log.error(String.format("Error code %d, triggering fences = %s, trigering location = %s", event.getErrorCode(), event.getTriggeringGeofences(), event.getTriggeringLocation()));
            return;
        }
        if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER) || (transition == Geofence.GEOFENCE_TRANSITION_EXIT)) {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = event.getTriggeringGeofences();
            log.debug("geofence transition: " + triggeringGeofences);
            Context ctx = config.createContext(this);
            Settings settings = new Settings(ctx);
            config.populateFromSettings(settings);
            PIGeofencingManager manager = new PIGeofencingManager(settings, PIGeofencingManager.MODE_GEOFENCE_EVENT, ctx,
                config.mServerUrl, config.mTenantCode, config.mOrgCode, config.mUsername, config.mPassword, (int) config.mMaxDistance);
            config.populateFromSettings(manager.mSettings);
            List<PersistentGeofence> geofences = new ArrayList<>(triggeringGeofences.size());
            for (Geofence g : triggeringGeofences) {
                String code = g.getRequestId();
                PersistentGeofence geofence = GeofencingUtils.geofenceFromCode(code);
                if (geofence != null) {
                    geofences.add(geofence);
                }
            }
            log.debug(String.format("triggered geofences = %s", geofences));
            PIGeofenceEvent.Type eventType = (transition == Geofence.GEOFENCE_TRANSITION_ENTER) ? PIGeofenceEvent.Type.ENTER : PIGeofenceEvent.Type.EXIT;
            manager.postGeofenceEvent(geofences, eventType);
            try {
                Intent broadcastIntent = new Intent(PIGeofenceEvent.ACTION_GEOFENCE_EVENT);
                broadcastIntent.setPackage(ctx.getPackageName());
                PIGeofenceEvent.toIntent(broadcastIntent, eventType, geofences, null);
                log.debug(String.format("sending config=%s", config));
                ctx.sendBroadcast(broadcastIntent);
            } catch(Exception e) {
                log.error("error sending broadcast event", e);
            }
        } else {
            log.error("invalid transition type: " + transition);
        }
    }
}
