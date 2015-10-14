/**
 * Copyright (c) 2015 IBM Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.pisdk;

import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;

/**
 * This class manages regions for Presence Insights. It keeps track of the number of overall regions monitored.
 * It handles starting and stopping the monitoring of beacon regions and the monitoring and ranging of UUID regions.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class RegionManager {
    // TAG
    private final String TAG = RegionManager.class.getSimpleName();
    // handle to the BeaconManager
    private BeaconManager mBeaconManager;
    // regions used to range for beacons
    private Region mUuidRegion;
    // regions used to get enter/exit region events
    private ArrayList<Region> mBeaconRegions = new ArrayList<Region>();
    // maximum number of regions to monitor at one time
    private final int maxRegions = 19;

    public RegionManager(BeaconManager manager) {
        Log.d(TAG, "initializing region manager with maxRegions: " + maxRegions);
        mBeaconManager = manager;
    }

    public void add(String uuid) {
        Log.d(TAG, "adding uuid region: " + uuid);
        Region uuidRegion = new Region(uuid, Identifier.parse(uuid), null, null);
        handleAddUuidRegion(uuidRegion);
    }

    // creates a beacon region based off of beacon object
    public void add(Beacon beacon) {
        // create region from beacon object and then send to handle beacon region
        Log.d(TAG, "adding beacon region for beacon: " + beacon.toString());
        String uniqueId = beacon.getId2().toString() + beacon.getId3().toString();
        Region beaconRegion = new Region(uniqueId, beacon.getId1(), beacon.getId2(), beacon.getId3());
        handleAddBeaconRegion(beaconRegion);
    }

    public void remove(Region region) {
        Log.d(TAG, "removing region: " + region.toString());
        if (region.getId1() != null && region.getId2() != null && region.getId3() != null) {
            // remove beacon region
            try {
                mBeaconManager.stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mBeaconRegions.remove(region);
        } else {
            Log.e(TAG, "region was not removed. Did not match beacon region.");
        }
    }

    public void removeUuidRegion(Region region) {
        Log.d(TAG, "removing region: " + region.toString());
        if (region.getId1() != null && region.getId2() == null && region.getId3() == null) {
            // remove beacon region
            try {
                mBeaconManager.stopMonitoringBeaconsInRegion(region);
                mBeaconManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mUuidRegion = null;
        } else {
            Log.e(TAG, "region was not removed. Did not match uuid region.");
        }
    }

    private void handleAddUuidRegion(Region region) {
        Log.d(TAG, "entered handle add uuid region method");
        // if no region set.. start ranging and monitoring
        if (mUuidRegion == null) {
            mUuidRegion = region;
        } else { // else stop ranging and monitoring for previous region and start for new region
            try {
                mBeaconManager.stopMonitoringBeaconsInRegion(mUuidRegion);
                mBeaconManager.stopRangingBeaconsInRegion(mUuidRegion);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mUuidRegion = region;
        }
        // after handling assignment start it up!
        try {
            mBeaconManager.startMonitoringBeaconsInRegion(mUuidRegion);
            mBeaconManager.startRangingBeaconsInRegion(mUuidRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void handleAddBeaconRegion(Region region) {
        Log.d(TAG, "entered handle add beacon region method");
        // check to see if beacon region exists in list already
        if (mBeaconRegions.contains(region)) {
            Log.d(TAG, "region already contained in list");
            mBeaconRegions.remove(region);
            mBeaconRegions.add(region);
        } else if (mBeaconRegions.size() == maxRegions) {
            // stop monitoring and remove last object
            Log.d(TAG, "reached max regions to monitor, removing oldest region and adding new region");
            Region removeRegion = mBeaconRegions.get(mBeaconRegions.size() - 1);
            mBeaconRegions.remove(removeRegion);
            mBeaconRegions.add(region);
            try {
                mBeaconManager.stopMonitoringBeaconsInRegion(removeRegion);
                mBeaconManager.startMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "adding region to list");
            mBeaconRegions.add(region);
            try {
                mBeaconManager.startMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
