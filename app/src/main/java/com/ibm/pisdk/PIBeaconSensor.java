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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;

/**
 * This class wraps the AltBeacon library's BeaconConsumer, and provides a simple interface to handle
 * the communication to the PIBeaconSensorService.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIBeaconSensor {
    private final String TAG = PIBeaconSensor.class.getSimpleName();

    private static final String INTENT_PARAMETER_ADAPTER = "adapter";
    private static final String INTENT_PARAMETER_COMMAND = "command";
    private static final String INTENT_PARAMETER_DEVICE_ID = "device_id";
    private static final String INTENT_PARAMETER_BEACON_LAYOUT = "beacon_layout";
    private static final String INTENT_PARAMETER_SEND_INTERVAL = "send_interval";

    public static final String INTENT_RECEIVER_BEACON_COLLECTION = "intent_receiver_beacon_collection";
    public static final String INTENT_RECEIVER_REGION_ENTER = "intent_receiver_region_enter";
    public static final String INTENT_RECEIVER_REGION_EXIT = "intent_receiver_region_exit";

    public static final String INTENT_EXTRA_BEACONS_IN_RANGE = "com.ibm.pi.android.beacons_in_range";
    public static final String INTENT_EXTRA_ENTER_REGION = "com.ibm.pi.android.enter_region";
    public static final String INTENT_EXTRA_EXIT_REGION = "com.ibm.pi.android.exit_region";

    private BluetoothAdapter mBluetoothAdapter;
    private final Context mContext;
    private final PIAPIAdapter mAdapter;
    private final String mDeviceId;

    private String mState;
    private static final String STARTED = "started";
    private static final String STOPPED = "stopped";

    /**
     * This interface provides a callback for beacons within range of the device.
     */
    public interface BeaconsInRangeListener {
        /**
         * Provides a collection of beacons within range
         *
         * @param beacons collection of Class Beacon.
         */
        void beaconsInRange(ArrayList<Beacon> beacons);
    }

    private BeaconsInRangeListener mBeaconsInRangeListener;

    public void setBeaconsInRangeListener(BeaconsInRangeListener listener) {
        mBeaconsInRangeListener = listener;
    }

    /**
     * This interface provides region event callbacks.
     */
    public interface RegionEventListener {
        /**
         * The device has entered a region
         *
         * @param region
         */
        void didEnterRegion(Region region);

        /**
         * The device has exited a region
         *
         * @param region
         */
        void didExitRegion(Region region);
    }

    private RegionEventListener mRegionEventListener;

    public void setRegionEventListener(RegionEventListener listener) {
        mRegionEventListener = listener;
    }

    private static PIBeaconSensor sInstance;

    public static PIBeaconSensor getInstance(Context context, PIAPIAdapter adapter) {
        if (sInstance == null) {
            //Always pass in the Application Context
            sInstance = new PIBeaconSensor(context.getApplicationContext(), adapter);
        }

        return sInstance;
    }

    /**
     * Default constructor
     *
     * @param context Activity context
     * @param adapter to handle sending of the beacon notification message
     * @see com.ibm.pisdk.PIAPIAdapter
     */
    private PIBeaconSensor(Context context, PIAPIAdapter adapter) {
        this.mContext = context;
        this.mAdapter = adapter;
        mState = STOPPED;

        // set listeners to null
        mBeaconsInRangeListener = null;
        mRegionEventListener = null;

        // get Device ID
        PIDeviceID deviceID = new PIDeviceID(context);
        mDeviceId = deviceID.getMacAddress();

        try {

            // If BLE isn't supported on the device we cannot proceed.
            if (!checkSupportBLE()) {
                throw new Exception("Device does not support BLE");
            }

            // Make sure to have reference to the bluetooth adapter, otherwise - retrieve it from the system.
            initBluetoothAdapter();

            // Make sure that BLE is on.
            if (!isBLEOn()) {
                // If BLE is off, turned it on
                if(!enableBLE()) {
                    PILogger.e(TAG, "Failed to start Bluetooth on this device.");
                }
            }

        } catch (Exception e){
            PILogger.e(TAG, "Failed to create PIBeaconSensorService: " + e.getMessage());
        }
    }

    /**
     * Start sensing for beacons.
     */
    public void start() {
        mState = STARTED;

        // Register to receive messages.
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_RECEIVER_BEACON_COLLECTION));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_RECEIVER_REGION_ENTER));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter(INTENT_RECEIVER_REGION_EXIT));

        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_ADAPTER, mAdapter);
        intent.putExtra(INTENT_PARAMETER_DEVICE_ID, mDeviceId);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "START_SCANNING");
        mContext.startService(intent);
    }

    /**
     * Stop sensing for beacons.
     */
    public void stop() {
        mState = STOPPED;

        // Unregister receiver.
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);

        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_ADAPTER, mAdapter);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "STOP_SCANNING");
        mContext.startService(intent);
    }

    /**
     * Sets the interval in which the device reports its location.
     *
     * @param sendInterval send interval in ms
     */
    public void setSendInterval(long sendInterval) {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_SEND_INTERVAL, sendInterval);
        mContext.startService(intent);
    }

    /**
     * Adds a new beacon advertisement layout.  By default, the AltBeacon library will only detect
     * beacons meeting the AltBeacon specification.  Please see AltBeacon's BeaconParser#setBeaconLayout
     * for a solid explanation of BLE advertisements.
     *
     * MUST BE CALLED BEFORE start()!
     *
     * @param beaconLayout the layout of the BLE advertisement
     */
    public void addBeaconLayout(String beaconLayout) {
        if (mState.equals(STOPPED)) {
            Intent intent = new Intent(mContext, PIBeaconSensorService.class);
            intent.putExtra(INTENT_PARAMETER_BEACON_LAYOUT, beaconLayout);
            mContext.startService(intent);
        } else {
            PILogger.e(TAG, "Cannot set beacon layout while service is running.");
        }
    }

    // Local broadcast receiver to handle callback
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                PILogger.d(TAG, "local broadcast receiver with callback");
                // beacons in range
                if (INTENT_RECEIVER_BEACON_COLLECTION.equals(intent.getAction())) {
                    if (mBeaconsInRangeListener != null) {
                        // Get extra data included in the Intent
                        ArrayList<Beacon> beacons = intent.getParcelableArrayListExtra(INTENT_EXTRA_BEACONS_IN_RANGE);
                        mBeaconsInRangeListener.beaconsInRange(beacons);
                    }
                }
                // region entered
                else if (INTENT_RECEIVER_REGION_ENTER.equals(intent.getAction())) {
                    PILogger.d(TAG, "local broadcast receiver with region enter");
                    if (mRegionEventListener != null) {
                        Region enterRegion = (Region) intent.getExtras().get(INTENT_EXTRA_ENTER_REGION);
                        mRegionEventListener.didEnterRegion(enterRegion);
                    }
                }
                // region exited
                else if (INTENT_RECEIVER_REGION_EXIT.equals(intent.getAction())) {
                    if (mRegionEventListener != null) {
                        Region exitRegion = (Region) intent.getExtras().get(INTENT_EXTRA_EXIT_REGION);
                        mRegionEventListener.didExitRegion(exitRegion);
                    }
                }
                // incorrect action received
                else {
                    PILogger.e(TAG, "incorrect action received, action received: " + intent.getAction());
                }
        }
    };

    // confirm if the device supports BLE, if not it can't be used for detecting beacons
    private  boolean checkSupportBLE(){
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            PILogger.e(TAG, "BLE is not supported for this device");
            return false;
        }
        return true;
    }

    // get the bluetooth adapter
    private void initBluetoothAdapter() throws Exception{
        if(mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if(mBluetoothAdapter == null)
                throw new Exception("Failed to get bluetooth adapter");
        }
    }

    // check to see if BLE is on
    private boolean isBLEOn(){
        return mBluetoothAdapter.isEnabled();
    }


    // enable bluetooth in case it's off (admin permission)
    private boolean enableBLE(){
        boolean response = true;
        if (!mBluetoothAdapter.isEnabled()) {
            response = false;
            mBluetoothAdapter.enable();
        }
        return response;
    }


}
