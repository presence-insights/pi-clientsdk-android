package com.ibm.pisdk;

import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;

/**
 * Created by hannigan on 10/6/15.
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
        mBeaconManager = manager;
    }

    public void add(String uuid) {
        Region uuidRegion = new Region(uuid, Identifier.parse(uuid), null, null);
        handleAddUuidRegion(uuidRegion);
    }

    // creates a beacon region based off of beacon object
    public void add(Beacon beacon) {
        // create region from beacon object and then send to handle beacon region
        String uniqueId = beacon.getId2().toString() + beacon.getId3().toString();
        Region beaconRegion = new Region(uniqueId, beacon.getId1(), beacon.getId2(), beacon.getId3());
        handleAddBeaconRegion(beaconRegion);
    }

    public void remove(Region region) {
        if (region.getId1() != null && region.getId2() == null && region.getId3() == null) {
            // remove uuid region
            try {
                mBeaconManager.stopMonitoringBeaconsInRegion(region);
                mBeaconManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mUuidRegion = null;
        } else if (region.getId1() != null && region.getId2() != null && region.getId3() != null) {
            // remove beacon region
            try {
                mBeaconManager.stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mBeaconRegions.remove(region);
        } else {
            Log.e(TAG, "region was not removed. Did not match either a uuid region or beacon region.");
        }
    }

    private void handleAddUuidRegion(Region region) {
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
        // check to see if beacon region exists in list already
        if (mBeaconRegions.contains(region)) {
            mBeaconRegions.remove(region);
            mBeaconRegions.add(region);
        } else if (mBeaconRegions.size() == maxRegions) {
            // stop monitoring and remove last object
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
            mBeaconRegions.add(region);
            try {
                mBeaconManager.startMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
