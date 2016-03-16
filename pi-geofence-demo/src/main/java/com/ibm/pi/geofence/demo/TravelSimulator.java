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

package com.ibm.pi.geofence.demo;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.ibm.pi.geofence.LoggingConfiguration;
import com.ibm.pi.geofence.PIGeofence;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simulates a user travelling between the provided geofences.
 * <p>the route is calculated as a fixed number of steps between two consecutive geofences in the list,
 * with a fixed wait time between two consecutive steps. At each step, a mock location is generated
 * and set as the current location.
 */
public class TravelSimulator implements Runnable {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(TravelSimulator.class.getSimpleName());
    private static final String TEST_LOCATION_PROVIDER = "pi-sdk-demo-provider";
    private final List<PIGeofence> geofences;
    private final int nbSteps;
    private final long timeBetweenSteps;
    private final Callback callback;
    //private LocationManager lm;
    private final Context context;
    /**
     * The Google API client.
     */
    private GoogleApiClient googleApiClient;

    /**
     * Initialize this walk simulator.
     * @param geofences        the list of geofences that determine the route.
     * @param nbSteps          number of steps between two consecutive geofences.
     * @param timeBetweenSteps time to wait after each step in millis.
     * @param callback         A callback notified at each new step.
     */
    TravelSimulator(Context context, List<PIGeofence> geofences, int nbSteps, long timeBetweenSteps, Callback callback) {
        this.context = context;
        this.geofences = geofences;
        this.nbSteps = nbSteps;
        this.timeBetweenSteps = timeBetweenSteps;
        this.callback = callback;
        log.debug(String.format("init of TravelSimulator with geofences=%s, nbSteps=%d, timeBetweenSteps=%d", geofences, nbSteps, timeBetweenSteps));
        /*
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm.getProvider(TEST_LOCATION_PROVIDER) == null) {
            lm.addTestProvider(TEST_LOCATION_PROVIDER, true, false, true, true, false, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_MEDIUM);
        }
        */
    }

    @Override
    public void run() {
        Looper.prepare();
        toast("starting simulated travel");

        try {
            //lm.setTestProviderEnabled(TEST_LOCATION_PROVIDER, true);
            connectGoogleAPI();
            LocationServices.FusedLocationApi.setMockMode(googleApiClient, true);
            log.debug(String.format("starting TravelSimulator with geofences=%s, nbSteps=%d, timeBetweenSteps=%d", geofences, nbSteps, timeBetweenSteps));
            for (int i = 1; i < geofences.size(); i++) {
                PIGeofence g1 = geofences.get(i - 1);
                PIGeofence g2 = geofences.get(i);
                log.debug(String.format("travelling from %s to %s", g1, g2));
                double stepLat = (g2.getLatitude() - g1.getLatitude()) / nbSteps;
                double stepLng = (g2.getLongitude() - g1.getLongitude()) / nbSteps;
                for (int j = 0; j < nbSteps; j++) {
                    double x = g1.getLatitude() + j * stepLat;
                    double y = g1.getLongitude() + j * stepLng;
                    Location loc = DemoUtils.createLocation(x, y);
                    LocationServices.FusedLocationApi.setMockLocation(googleApiClient, loc);
                    //lm.setTestProviderLocation(TEST_LOCATION_PROVIDER, loc);
                    Thread.sleep(timeBetweenSteps);
                }
            }
            // do the last step
            Thread.sleep(timeBetweenSteps);
            PIGeofence g = geofences.get(geofences.size() - 1);
            Location loc = DemoUtils.createLocation(g.getLatitude(), g.getLongitude());
            LocationServices.FusedLocationApi.setMockLocation(googleApiClient, loc);
            log.debug("end of walk!!!");
            if (callback != null) {
                callback.onSimulationEnded();
            }
        } catch (Exception e) {
            log.debug("", e);
        } finally {
            //lm.setTestProviderEnabled(TEST_LOCATION_PROVIDER, false);
            LocationServices.FusedLocationApi.setMockMode(googleApiClient, false);
            toast("simulated travel ended");
        }
    }

    private void connectGoogleAPI() {
        googleApiClient = new GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            //.addConnectionCallbacks(googleAPICallback)
            //.addOnConnectionFailedListener(googleAPICallback)
            .build();
        log.debug("initGms() connecting to google play services ...");
        try {
            // can't run blockingConnect() on the UI thread
            ConnectionResult result = googleApiClient.blockingConnect(60_000L, TimeUnit.MILLISECONDS);
            log.debug(String.format("google api connection %s, result=%s", (result.isSuccess() ? "success" : "error"), result));
        } catch(Exception e) {
            log.error("error while attempting connection to google api", e);
        }
    }

    private void toast(String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
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
