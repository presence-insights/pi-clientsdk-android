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
import android.location.Location;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

import org.apache.log4j.Logger;

import java.util.Locale;

/**
 * This service handles location updates emitted by the Google play services Location API.
 * Unfortunately, it doesn't work yet because of this Adroid issue: https://code.google.com/p/android/issues/detail?id=197296
 */
public class FusedGeofenceManager extends IntentService {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(FusedGeofenceManager.class.getSimpleName());
    /**
     * Settings key for the last reference location.
     */
    private Context context;
    private Location referenceLocation = null;
    private double maxDistance;
    private ServiceConfig config;

    public FusedGeofenceManager() {
        this(FusedGeofenceManager.class.getSimpleName());
    }

    public FusedGeofenceManager(String name) {
        super(name);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        //boolean shouldProcess = intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED) != null;
        intent.setExtrasClassLoader(LocationResult.class.getClassLoader());
        boolean shouldProcess = LocationResult.hasResult(intent);
        if (shouldProcess) {
            this.config = new ServiceConfig().fromIntent(intent);
            this.context = config.createContext(this);
            this.maxDistance = config.maxDistance;
            Settings settings = new Settings(context);
            this.referenceLocation = GeofenceManager.retrieveReferenceLocation(settings);
            //Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
            Location location = LocationResult.extractResult(intent).getLastLocation();
            //log.debug(String.format("onReceive() config=%s, settings=%s", config, settings));
            onLocationChanged(location);
        }
    }

    void onLocationChanged(Location location) {
        double d = maxDistance + 1d;
        if (referenceLocation != null) {
            d = referenceLocation.distanceTo(location);
        }
        //log.debug(String.format("onLocationChanged(location=%s; d=%,.0f)", location, d));
        // if signficant location change, handle the new region (bounded box) to monitor
        if (d > maxDistance) {
            log.debug(String.format(Locale.US, "onLocationChanged() detected significant location change, distance to ref = %,.0f m, new location = %s", d, location));
            Intent intent = new Intent(context, SignificantLocationChangeService.class);
            config.newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            config.toIntent(intent);
            context.startService(intent);
        }
    }
}
