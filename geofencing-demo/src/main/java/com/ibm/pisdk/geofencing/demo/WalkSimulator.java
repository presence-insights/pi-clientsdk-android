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

package com.ibm.pisdk.geofencing.demo;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationServices;
import com.ibm.pisdk.geofencing.PIGeofence;
import com.ibm.pisdk.geofencing.PIGeofencingService;

import java.util.List;

/**
 * Simulates a user walking between the provided geofences.
 * <p>the route is calculated as a fixed number of steps between two consecutive geofences in the list,
 * with a fixed wait time between two consecutive steps. At each step, a mock location is generated
 * and set as the current location.
 */
public class WalkSimulator implements Runnable {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = WalkSimulator.class.getSimpleName();
    private final List<PIGeofence> geofences;
    private final int nbSteps;
    private final long timeBetweenSteps;
    private final Callback callback;
    private final PIGeofencingService service;

    /**
     * Initialize this walk simulator.
     * @param geofences the list of geofences that determine the route.
     * @param nbSteps number of steps between two consecutive geofences.
     * @param timeBetweenSteps time to wait after each step.
     * @param callback A callback notified at each new step.
     */
    WalkSimulator(PIGeofencingService service, final List<PIGeofence> geofences, final int nbSteps, final long timeBetweenSteps, final Callback callback) {
        this.geofences = geofences;
        this.nbSteps = nbSteps;
        this.timeBetweenSteps = timeBetweenSteps;
        this.callback = callback;
        this.service = service;
    }

    @Override
    public void run() {
        try {
            Log.v(LOG_TAG, "starting WalkSimulator with number of steps between geofences = " + nbSteps + " and wait betweens steps = " + timeBetweenSteps + " ms");
            LocationServices.FusedLocationApi.setMockMode(service.getGoogleApiClient(), true);
            for (int i = 1; i < geofences.size(); i++) {
                PIGeofence g1 = geofences.get(i - 1);
                PIGeofence g2 = geofences.get(i);
                double stepLat = (g2.getLatitude() - g1.getLatitude()) / nbSteps;
                double stepLng = (g2.getLongitude() - g1.getLongitude()) / nbSteps;
                for (int j = 0; j < nbSteps; j++) {
                    double x = g1.getLatitude() + j * stepLat;
                    double y = g1.getLongitude() + j * stepLng;
                    Location loc = DemoUtils.createLocation(x, y);
                    LocationServices.FusedLocationApi.setMockLocation(service.getGoogleApiClient(), loc);
                    Thread.sleep(timeBetweenSteps);
                }
            }
            // do the last step
            Thread.sleep(timeBetweenSteps);
            Log.v(LOG_TAG, "end of walk!!!");
            callback.onSimulationEnded();
        } catch (Exception e) {
            Log.e(LOG_TAG, "", e);
        }
    }

    /**
     * Callback provided in the constructor to notify when a new location is computed.
     */
    public interface Callback {
        /**
         * Called when the simulation ends.
         */
        void onSimulationEnded();
    }
}
