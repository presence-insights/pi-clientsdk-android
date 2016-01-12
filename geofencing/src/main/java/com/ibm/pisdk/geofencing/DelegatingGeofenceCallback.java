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

import java.util.List;

/**
 *
 */
class DelegatingGeofenceCallback implements PIGeofenceCallback {
    private final PIGeofenceCallback delegate;
    private final PIGeofencingService service;

    DelegatingGeofenceCallback(final PIGeofencingService service, final PIGeofenceCallback delegate) {
        this.service = service;
        this.delegate = delegate;
    }

    @Override
    public void onGeofencesEnter(List<PIGeofence> geofences) {
        //service.notifyGeofences(geofences, GeofenceNotificationType.IN);
        if (delegate != null) {
            delegate.onGeofencesEnter(geofences);
        }
    }

    @Override
    public void onGeofencesExit(List<PIGeofence> geofences) {
        //service.notifyGeofences(geofences, GeofenceNotificationType.OUT);
        if (delegate != null) {
            delegate.onGeofencesExit(geofences);
        }
    }

    @Override
    public void onGeofencesMonitored(List<PIGeofence> geofences) {
        if (delegate != null) {
            delegate.onGeofencesMonitored(geofences);
        }
    }

    @Override
    public void onGeofencesUnmonitored(List<PIGeofence> geofences) {
        if (delegate != null) {
            delegate.onGeofencesUnmonitored(geofences);
        }
    }
}
