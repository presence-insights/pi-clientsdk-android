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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;

public class PIBeaconSensorService extends Service implements BeaconConsumer {
    private static final String TAG = PIBeaconSensorService.class.getSimpleName();

    private static final String INTENT_PARAMETER_ADAPTER = "adapter";
    private static final String INTENT_PARAMETER_COMMAND = "command";
    private static final String INTENT_PARAMETER_DEVICE_ID = "device_id";
    private static final String INTENT_PARAMETER_BEACON_LAYOUT = "beacon_layout";
    private static final String INTENT_PARAMETER_SEND_INTERVAL = "send_interval";

    private Context mContext;
    private PIAPIAdapter mPiApiAdapter;
    private BeaconManager mBeaconManager;
    private RegionManager mRegionManager;
    private String mDeviceId;

    private volatile long sendInterval = 5000;
    private long lastSendTime = 0;
    private long currentTime = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command;
        Bundle extras = null;
        if (intent != null) {
            extras = intent.getExtras();
        }

        // lazily instantiate beacon manager
        if (mBeaconManager == null) {
            mBeaconManager = BeaconManager.getInstanceForApplication(this);
        }
        // and region manager
        if (mRegionManager == null) {
            mRegionManager = new RegionManager(mBeaconManager);
        }

        // check passed in intent for commands sent from Beacon Sensor wrapper class
        if (extras != null) {
            if (extras.get(INTENT_PARAMETER_ADAPTER) != null) {
                mPiApiAdapter = (PIAPIAdapter) extras.get(INTENT_PARAMETER_ADAPTER);
            }
            if (!extras.getString(INTENT_PARAMETER_DEVICE_ID, "").equals("")) {
                mDeviceId = extras.getString(INTENT_PARAMETER_DEVICE_ID);
            }
            if (extras.getLong(INTENT_PARAMETER_SEND_INTERVAL, -1) > 0) {
                sendInterval = extras.getLong(INTENT_PARAMETER_SEND_INTERVAL);
                PILogger.d(TAG, "updating send interval to: " + sendInterval);
            }
            if (!extras.getString(INTENT_PARAMETER_BEACON_LAYOUT, "").equals("")) {
                String beaconLayout = intent.getStringExtra(INTENT_PARAMETER_BEACON_LAYOUT);
                PILogger.d(TAG, "adding new beacon layout: " + beaconLayout);
                mBeaconManager.getBeaconParsers().add(new BeaconParser()
                        .setBeaconLayout(beaconLayout));
            }
            if (!extras.getString(INTENT_PARAMETER_COMMAND, "").equals("")) {
                command = extras.getString(INTENT_PARAMETER_COMMAND);
                if (command.equals("START_SCANNING")){
                    PILogger.d(TAG, "Service has started scanning for beacons");
                    mBeaconManager.bind(this);
                } else if (command.equals("STOP_SCANNING")){
                    PILogger.d(TAG, "Service has stopped scanning for beacons");
                    mBeaconManager.unbind(this);
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                PILogger.d(TAG, "entered region: " + region);

                // send enter region event to listener callback
                Intent intent = new Intent(PIBeaconSensor.INTENT_RECEIVER_REGION_ENTER);
                intent.putExtra(PIBeaconSensor.INTENT_EXTRA_ENTER_REGION, region);

                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }

            @Override
            public void didExitRegion(Region region) {
                PILogger.d(TAG, "exited region: " + region);

                // send exit region event to listener callback
                Intent intent = new Intent(PIBeaconSensor.INTENT_RECEIVER_REGION_EXIT);
                intent.putExtra(PIBeaconSensor.INTENT_EXTRA_EXIT_REGION, region);

                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                // not used
            }
        });

        mBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon b : beacons) {
                        mRegionManager.add(b);
                    }
                    currentTime = System.currentTimeMillis();
                    if (currentTime - lastSendTime > sendInterval) {
                        lastSendTime = currentTime;
                        sendBeaconNotification(beacons);
                    }
                }
            }
        });

        mPiApiAdapter.getProximityUUIDs(new PIAPICompletionHandler() {
            @Override
            public void onComplete(PIAPIResult result) {
                if (result.getResponseCode() == 200) {
                    ArrayList<String> uuids = (ArrayList<String>)result.getResult();
                    if (uuids.size() > 0) {
                        for (Object uuid : uuids.toArray()) {
                            // this is temporary
                            // with only one uuid per org assumption in RegionManager
                            // we will only range in the last uuid in the list
                            mRegionManager.add((String) uuid);
                        }
                    } else {
                        PILogger.e(TAG, "Call to Management server returned an empty array of proximity UUIDs");
                    }
                } else {
                    // default estimote uuid
                    PILogger.e(TAG, result.toString());
                }
            }
        });
    }

    private void sendBeaconNotification(Collection<Beacon> beacons) {
        JSONObject payload = buildBeaconPayload(beacons);
        PILogger.d(TAG, "sending beacon notification message");
        mPiApiAdapter.sendBeaconNotificationMessage(payload, new PIAPICompletionHandler() {
            @Override
            public void onComplete(PIAPIResult result) {
                if (result.getResponseCode() >= HttpStatus.SC_BAD_REQUEST) {
                    PILogger.e(TAG, result.toString());
                }
            }
        });

        // send beacons in range event to listener callback
        Intent intent = new Intent(PIBeaconSensor.INTENT_RECEIVER_BEACON_COLLECTION);
        intent.putParcelableArrayListExtra(PIBeaconSensor.INTENT_EXTRA_BEACONS_IN_RANGE, new ArrayList<Beacon>(beacons));

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private JSONObject buildBeaconPayload(Collection<Beacon> beacons) {
        long detectedTime = System.currentTimeMillis();
        JSONObject payload = new JSONObject();
        JSONArray beaconArray = new JSONArray();

        for (Beacon b : beacons) {
            PIBeaconData data = new PIBeaconData(b);
            data.setDetectedTime(detectedTime);
            data.setDeviceDescriptor(mDeviceId);
            beaconArray.add(data.getBeaconAsJson());
        }
        payload.put("bnm", beaconArray);

        return payload;
    }

    @Override
    public void onDestroy() {
        mBeaconManager.unbind(this);
        super.onDestroy();
    }
}
