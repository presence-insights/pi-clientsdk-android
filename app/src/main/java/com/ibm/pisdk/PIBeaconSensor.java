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
import android.util.Log;
import android.widget.Toast;

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
    private PIBeaconSensorDelegate mDelegate;

    private String mState;
    private static final String STARTED = "started";
    private static final String STOPPED = "stopped";

    /**
     * Default constructor
     *
     * @param context Activity context
     * @param adapter to handle sending of the beacon notification message
     * @see com.ibm.pisdk.PIAPIAdapter
     */
    public PIBeaconSensor(Context context, PIAPIAdapter adapter) {
        this.mContext = context;
        this.mAdapter = adapter;
        mState = STOPPED;

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
                    Log.d(TAG, "Failed to start Bluetooth on this device.");
                }
            }

        } catch (Exception e){
            Log.d(TAG, "Failed to create PIBeaconSensorService: " + e.getMessage());
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
            Toast.makeText(mContext, "Cannot set beacon layout while service is running.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot set beacon layout while service is running.");
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDelegate != null) {
                // beacons in range
                if (INTENT_RECEIVER_BEACON_COLLECTION.equals(intent.getAction())) {
                    // Get extra data included in the Intent
                    ArrayList<Beacon> beacons = intent.getParcelableArrayListExtra(INTENT_EXTRA_BEACONS_IN_RANGE);
                    mDelegate.beaconsInRange(beacons);
                }
                // region entered
                else if (INTENT_RECEIVER_REGION_ENTER.equals(intent.getAction())) {
                    Region enterRegion = (Region) intent.getExtras().get(INTENT_EXTRA_ENTER_REGION);
                    mDelegate.didEnterRegion(enterRegion);
                }
                // region exited
                else if (INTENT_RECEIVER_REGION_EXIT.equals(intent.getAction())) {
                    Region exitRegion = (Region) intent.getExtras().get(INTENT_EXTRA_EXIT_REGION);
                    mDelegate.didExitRegion(exitRegion);
                }
                // incorrect action received
                else {
                    Log.e(TAG, "incorrect action received, action received: " + intent.getAction());
                }
            }
        }
    };

    public void setBeaconsInRangeListener(PIBeaconSensorDelegate listener) {
        try {
            mDelegate = listener;
        } catch (ClassCastException e) {
            throw new ClassCastException(listener.toString()
                    + " must implement PIBeaconSensorDelegate");
        }
    }

    // confirm if the device supports BLE, if not it can't be used for detecting beacons
    private  boolean checkSupportBLE(){
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "ble_not_supported");
            Toast.makeText(mContext, "ble_not_supported", Toast.LENGTH_SHORT).show();
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
