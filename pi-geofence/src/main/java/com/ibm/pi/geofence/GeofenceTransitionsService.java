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
            PIGeofenceCallback callback = PIGeofencingManager.callbackMap.get(intent.getStringExtra(PIGeofencingManager.INTENT_ID));
            PIGeofencingManager service = null;
            Context ctx = null;
            Settings settings = null;
            Class<? extends PIGeofenceCallbackService> clazz = null;
            if (callback == null) {
                ctx = config.createContext(this);
                settings = new Settings(ctx);
                config.populateFromSettings(settings);
            } else {
                service = ((DelegatingGeofenceCallback) callback).service;
                ctx = service.context;
                settings = service.settings;
            }
            // happens when the app is off
            if (config.callbackServiceName != null) {
                clazz = config.loadCallbackServiceClass(ctx);
            }
            if (callback == null) {
                service = PIGeofencingManager.newInstance(settings, PIGeofencingManager.MODE_GEOFENCE_EVENT, clazz, ctx,
                    config.serverUrl, config.tenantCode, config.orgCode, config.username, config.password, (int) config.maxDistance);
                callback = service.geofenceCallback;
            }
            config.populateFromSettings(service.settings);
            List<PersistentGeofence> geofences = new ArrayList<>(triggeringGeofences.size());
            for (Geofence g : triggeringGeofences) {
                String code = g.getRequestId();
                PersistentGeofence geofence = GeofencingUtils.geofenceFromCode(code);
                if (geofence != null) {
                    geofences.add(geofence);
                }
            }
            log.debug(String.format("callback = %s, clazz=%s, triggered geofences = %s", callback, clazz, geofences));
            if (callback != null) {
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    callback.onGeofencesEnter(PersistentGeofence.toPIGeofences(geofences));
                } else {
                    callback.onGeofencesExit(PersistentGeofence.toPIGeofences(geofences));
                }
            }
            if (clazz != null) {
                try {
                    Intent callbackIntent = new Intent(ctx, clazz);
                    config.geofences = geofences;
                    config.eventType = (transition == Geofence.GEOFENCE_TRANSITION_ENTER) ? ServiceConfig.EventType.ENTER : ServiceConfig.EventType.EXIT;
                    config.toIntent(callbackIntent);
                    log.debug(String.format("sending config=%s", config));
                    ctx.startService(callbackIntent);
                } catch(Exception e) {
                    log.error(String.format("error starting callback service '%s'", config.callbackServiceName), e);
                }
            }
        } else {
            log.error("invalid transition type: " + transition);
        }
    }
}
