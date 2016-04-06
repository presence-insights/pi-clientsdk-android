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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import org.apache.log4j.Logger;

import java.util.Locale;

/**
 * Receives location change events and determines whether they are significant changes, that is,
 * whether the new location is outside the current bounding box.
 */
public class LocationUpdateReceiver extends BroadcastReceiver {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(LocationUpdateReceiver.class.getSimpleName());
    private Context context;
    private Location referenceLocation = null;
    private double maxDistance;
    private Settings settings;
    private ServiceConfig config;

    public LocationUpdateReceiver() {
    }

    public LocationUpdateReceiver(PIGeofencingManager geofencingService) {
        this();
        this.context = geofencingService.context;
        this.settings = geofencingService.settings;
        this.maxDistance = geofencingService.maxDistance;
        this.referenceLocation = GeofencingUtils.retrieveReferenceLocation(settings);
        this.config = new ServiceConfig().fromGeofencingManager(geofencingService);
        //log.debug(String.format("GeofenceManager() config=%s, settings=%s", config, settings));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean shouldProcess = intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED) != null;
        if (shouldProcess) {
            this.config = new ServiceConfig().fromIntent(intent);
            this.context = context;
            this.maxDistance = config.maxDistance;
            this.settings = new Settings(context);
            this.referenceLocation = GeofencingUtils.retrieveReferenceLocation(settings);
            Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
            //log.debug(String.format("onReceive() config=%s, settings=%s", config, settings));
            onLocationChanged(location, false);
        }
    }

    /**
     * Determine whether a significant location change occurred, based on the provided location.
     * @param location the location to compare with the reference location stored in the {@link Settings}.
     * @param force whether to force a reload of the fence in the bounding box regardless the current location,
     *  which is needed after a sync of the fences with the server.
     */
    void onLocationChanged(Location location, boolean force) {
        double d = maxDistance + 1d;
        if (referenceLocation != null) {
            d = referenceLocation.distanceTo(location);
        }
        //log.debug(String.format("onLocationChanged(location=%s; d=%,.0f)", location, d));
        if ((d > maxDistance) || force) {
            log.debug(String.format(Locale.US, "onLocationChanged() detected significant location change, distance to ref = %,.0f m, new location = %s", d, location));
            Intent intent = new Intent(context, SignificantLocationChangeService.class);
            intent.setPackage(context.getPackageName());
            config.newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            config.toIntent(intent);
            context.startService(intent);
        }
    }

}
