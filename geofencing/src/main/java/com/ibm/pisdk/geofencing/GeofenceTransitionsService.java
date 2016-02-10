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

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

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
    private static final Logger log = Logger.getLogger(PIGeofencingService.class);

    public GeofenceTransitionsService() {
        super("PIGeofencingService");
    }

    public GeofenceTransitionsService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        log.debug("in onHandleIntent()");
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {
            log.error(String.format(
                "Error code %d, triggering fences = %s, trigering location = %s", event.getErrorCode(), event.getTriggeringGeofences(), event.getTriggeringLocation()));
            return;
        }
        // Get the transition type.
        int transition = event.getGeofenceTransition();
        // Test that the reported transition was of interest.
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = event.getTriggeringGeofences();
            log.debug("geofence transition: " + triggeringGeofences);
            PIGeofenceCallback callback = PIGeofencingService.callbackMap.get(intent.getStringExtra(PIGeofencingService.INTENT_ID));
            List<PIGeofence> geofences = new ArrayList<>(triggeringGeofences.size());
            for (Geofence g : triggeringGeofences) {
                String code = g.getRequestId();
                List<PIGeofence> list = PIGeofence.find(PIGeofence.class, "code = ?", code);
                if (!list.isEmpty()) geofences.add(list.get(0));
            }
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                callback.onGeofencesEnter(geofences);
            } else {
                callback.onGeofencesExit(geofences);
            }
        } else {
            log.error("invalid transition type: " + transition);
        }
    }
}
