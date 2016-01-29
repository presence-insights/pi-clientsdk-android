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

import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.log4j.Logger;

/**
 * Callback implementation for feedback on the Google API connection state.
 */
class GoogleLocationAPICallback implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /**
     * Logger for this class.
     */
    private static final Logger log = Logger.getLogger(PIGeofencingService.class);
    private final PIGeofencingService service;

    public GoogleLocationAPICallback(PIGeofencingService service) {
        this.service = service;
    }

    @Override
    public void onConnected(Bundle bundle) {
        log.debug("connected to google API");
        // register a location change listener
        final LocationRequest locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            //.setPriority(LocationRequest.PRIORITY_LOW_POWER)
            .setInterval(10000L)
            .setFastestInterval(10000L);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                LocationServices.FusedLocationApi.requestLocationUpdates(service.getGoogleApiClient(), locationRequest, service.geofenceManager);
            }
        };
        new Thread(r).start();
        service.loadGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {
        log.debug("connection to google API suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        log.debug("failed to connect to google API: " + connectionResult);
    }
}
