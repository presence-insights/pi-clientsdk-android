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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PIBeaconSensorService extends Service implements BeaconConsumer {
    private static final String TAG = PIBeaconSensorService.class.getSimpleName();

    private Context mContext;
    private SharedPreferences mPrefs;
    private BackgroundPowerSaver mBackgroundPowerSaver;
    private PIAPIAdapter mPiApiAdapter;
    private BeaconManager mBeaconManager;
    private RegionManager mRegionManager;

    private volatile long mSendInterval = 5000l;
    private volatile long mBackgroundScanPeriod = 1100l;
    private volatile long mBackgroundBetweenScanPeriod = 60000l;
    private long mLastSendTime = 0;
    private long mCurrentTime = 0;
    private String mDeviceDescriptor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PILogger.d(TAG, "intent: " + intent);
        PILogger.d(TAG, "flags: " + flags);
        PILogger.d(TAG, "startId: " + startId);

        if (mBackgroundPowerSaver == null) {
            // set up for background ranging and monitoring
            mBackgroundPowerSaver = new BackgroundPowerSaver(this.getApplicationContext());
        }
        if (mBeaconManager == null) {
            mBeaconManager = BeaconManager.getInstanceForApplication(this);

            // set some default values
            mBeaconManager.setBackgroundScanPeriod(mBackgroundScanPeriod);
            mBeaconManager.setBackgroundBetweenScanPeriod(mBackgroundBetweenScanPeriod);

            mRegionManager = new RegionManager(mBeaconManager);
        }

        if (intent != null) {
            handleCommands(intent);
        } else {
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mPrefs = this.getSharedPreferences(getResources().getString(R.string.pi_shared_pref), Context.MODE_PRIVATE);

        setupDescriptor();
        PILogger.enableDebugMode(true);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(PIDeviceInfo.PI_SHARED_PREF_DESCRIPTOR_KEY)) {
                        mDeviceDescriptor = sharedPreferences.getString(key, "");
                    }
                }
            };

    private void setupDescriptor() {
        mDeviceDescriptor = mPrefs.getString(PIDeviceInfo.PI_SHARED_PREF_DESCRIPTOR_KEY, "");
        if ("".equals(mDeviceDescriptor)) {
            String android_id = Settings.Secure.getString(mContext.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            mDeviceDescriptor = android_id;

            mPrefs.edit().putString(PIDeviceInfo.PI_SHARED_PREF_DESCRIPTOR_KEY, android_id).apply();
        }

        mPrefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private void handleCommands(Intent intent) {
        Bundle extras = intent.getExtras();

        // check passed in intent for commands sent from Beacon Sensor wrapper class
        if (extras != null) {
            if (extras.containsKey(PIBeaconSensor.ADAPTER_KEY)) {
                mPiApiAdapter = (PIAPIAdapter) extras.get(PIBeaconSensor.ADAPTER_KEY);
            }
            if (extras.containsKey(PIBeaconSensor.SEND_INTERVAL_KEY)) {
                PILogger.d(TAG, "updating send interval to: " + mSendInterval);
                mSendInterval = extras.getLong(PIBeaconSensor.SEND_INTERVAL_KEY);
            }
            if (extras.containsKey(PIBeaconSensor.BEACON_LAYOUT_KEY)) {
                String beaconLayout = intent.getStringExtra(PIBeaconSensor.BEACON_LAYOUT_KEY);
                PILogger.d(TAG, "new beacon layout: " + beaconLayout);
                PILogger.d(TAG, "stored layout: " + mPrefs.getString(PIBeaconSensor.BEACON_LAYOUT_KEY, ""));
                List<BeaconParser> parsers =  mBeaconManager.getBeaconParsers();
                if (!mBeaconManager.isBound(this)) {
                    PILogger.d(TAG, "adding new beacon layout: " + beaconLayout);
                    mBeaconManager.getBeaconParsers().add(new BeaconParser()
                            .setBeaconLayout(beaconLayout));
                }
            }
            if (extras.containsKey(PIBeaconSensor.BACKGROUND_SCAN_PERIOD_KEY)) {
                PILogger.d(TAG, "updating background scan period to: " + mBackgroundScanPeriod);
                mBackgroundScanPeriod = extras.getLong(PIBeaconSensor.BACKGROUND_SCAN_PERIOD_KEY);
                mBeaconManager.setBackgroundScanPeriod(mBackgroundScanPeriod);
            }
            if (extras.containsKey(PIBeaconSensor.BACKGROUND_BETWEEN_SCAN_PERIOD_KEY)) {
                PILogger.d(TAG, "updating background between scan period to: " + mBackgroundBetweenScanPeriod);
                mBackgroundBetweenScanPeriod = extras.getLong(PIBeaconSensor.BACKGROUND_BETWEEN_SCAN_PERIOD_KEY);
                mBeaconManager.setBackgroundBetweenScanPeriod(mBackgroundBetweenScanPeriod);
            }
            if (extras.containsKey(PIBeaconSensor.START_IN_BACKGROUND_KEY)) {
                PILogger.d(TAG, "service started up in the background, starting sensor in background mode");
                mBeaconManager.setBackgroundMode(true);
            }
        }

        if (intent.getAction() != null) {
            String action = intent.getAction();
            if (action.equals(PIBeaconSensor.INTENT_ACTION_START)){
                PILogger.d(TAG, "Service has started scanning for beacons");
                mBeaconManager.bind(this);
            } else if (action.equals(PIBeaconSensor.INTENT_ACTION_STOP)){
                PILogger.d(TAG, "Service has stopped scanning for beacons");
                mBeaconManager.unbind(this);
                stopSelf();
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                PILogger.d(TAG, "entered region: " + region);
                mRegionManager.handleEnterRegion(region);

                // send enter region event to listener callback
                Intent intent = new Intent(PIBeaconSensor.INTENT_RECEIVER_REGION_ENTER);
                intent.putExtra(PIBeaconSensor.INTENT_EXTRA_ENTER_REGION, region);

                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
            }

            @Override
            public void didExitRegion(Region region) {
                PILogger.d(TAG, "exited region: " + region);
                mRegionManager.handleExitRegion(region);

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
                    mCurrentTime = System.currentTimeMillis();
                    if (mCurrentTime - mLastSendTime > mSendInterval) {
                        mLastSendTime = mCurrentTime;
                        sendBeaconNotification(beacons);
                    }
                }
            }
        });

        if (mPrefs.contains(PIBeaconSensor.UUID_KEY)) {
            mRegionManager.add(mPrefs.getString(PIBeaconSensor.UUID_KEY, ""));
        } else {
            mPiApiAdapter.getProximityUUIDs(new PIAPICompletionHandler() {
                @Override
                public void onComplete(PIAPIResult result) {
                    if (result.getResponseCode() == 200) {
                        ArrayList<String> uuids = (ArrayList<String>) result.getResult();
                        if (uuids.size() > 0) {
                            String uuid = (String) uuids.get(0);
                            mRegionManager.add(uuid);
                            mPrefs.edit().putString(PIBeaconSensor.UUID_KEY, uuid).apply();
                        } else {
                            PILogger.e(TAG, "Call to Management server returned an empty array of proximity UUIDs");
                        }
                    } else {
                        PILogger.e(TAG, result.toString());
                    }
                }
            });
        }
    }

    private void sendBeaconNotification(Collection<Beacon> beacons) {
        PILogger.d(TAG, "sending beacon notification message");

        JSONObject payload = buildBeaconPayload(beacons);
        mPiApiAdapter.sendBeaconNotificationMessage(payload, new PIAPICompletionHandler() {
            @Override
            public void onComplete(PIAPIResult result) {
                if (result.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
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

        // build payload with nearest beacon only
        Beacon nearestBeacon = beacons.iterator().next();
        for (Beacon b : beacons) {
            if (b.getDistance() < nearestBeacon.getDistance()) {
                nearestBeacon = b;
            }
        }

        PIBeaconData data = new PIBeaconData(nearestBeacon);
        data.setDetectedTime(detectedTime);
        data.setDeviceDescriptor(mDeviceDescriptor);
        beaconArray.add(data.getBeaconAsJson());

        payload.put("bnm", beaconArray);

        return payload;
    }

    @Override
    public void onDestroy() {
        mBeaconManager.unbind(this);
        super.onDestroy();
    }
}
