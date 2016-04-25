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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.log4j.Logger;

/**
 * Callback implementation for feedback on the Google API connection state.
 */
class GoogleLocationAPICallback implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(GoogleLocationAPICallback.class.getSimpleName());
    private final PIGeofencingManager mGeofencingService;
    private PendingIntent mPendingIntent = null;

    public GoogleLocationAPICallback(PIGeofencingManager geofencingService) {
        this.mGeofencingService = geofencingService;
    }

    @Override
    public void onConnected(Bundle bundle) {
        log.debug("connected to google API");
        if (mGeofencingService.mMode == PIGeofencingManager.MODE_APP) {
            mGeofencingService.loadGeofences();
        }
        if (mGeofencingService.mMode != PIGeofencingManager.MODE_REBOOT) {
            // register a location change listener
            /*
            // doesn't work due to android issue https://code.google.com/p/android/issues/detail?id=197296
                final LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setInterval(10000L).setFastestInterval(10000L);
                LocationServices.FusedLocationApi.requestLocationUpdates(geofencingService.googleApiClient, locationRequest, getPendingIntent());
            */
            log.debug("registering location listener service");
            LocationManager lm = (LocationManager) mGeofencingService.mContext.getSystemService(Context.LOCATION_SERVICE);
            PendingIntent pi = getPendingIntent();
            String[] providers = { LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER };
            for (String provider: providers) {
                try {
                    lm.requestLocationUpdates(provider, 10_000L, 50f, pi);
                } catch(SecurityException e) {
                    log.debug("missing permission to request locations from " + provider +  " provider");
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        log.debug("connection to google API suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        log.debug("failed to connect to google API: " + connectionResult);
    }

    /**
     * Get a pending intent for the specified callback.
     * @return a <code>PendingIntent</code> instance.
     */
    private PendingIntent getPendingIntent() {
        if (mPendingIntent == null) {
            Class<?> clazz = LocationUpdateReceiver.class;
            //Class<?> clazz = FusedGeofenceManager.class;
            Intent intent = new Intent(mGeofencingService.mContext, clazz);
            //intent.setPackage(geofencingService.context.getPackageName());
            new ServiceConfig().fromGeofencingManager(mGeofencingService).toIntent(intent);
            intent.setClass(mGeofencingService.mContext, clazz);
            mPendingIntent = PendingIntent.getBroadcast(mGeofencingService.mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mPendingIntent;
    }
}
