package com.ibm.pzsdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by hannigan on 3/26/2015.
 */
public class PIBeaconSensor {
    private String TAG = PIBeaconSensor.class.getSimpleName();

    private static final String INTENT_PARAMETER_ADAPTER = "adapter";
    private static final String INTENT_PARAMETER_COMMAND = "command";
    private static final String INTENT_PARAMETER_DELEGATE = "delegate";
    private static final String INTENT_PARAMETER_BEACON_LAYOUT = "beacon_layout";
    private static final String INTENT_PARAMETER_SEND_INTERVAL = "send_interval";
    private static final String INTENT_PARAMETER_TENANT = "tenant";
    private static final String INTENT_PARAMETER_ORG = "org";

    private BluetoothAdapter mBluetoothAdapter;

    private PIAPIAdapter mAdapter;
    private final String mTenant;
    private final String mOrg;

    public long mSendInterval = 5000;

    private Context mContext;

    /**
     *
     * @param context
     * @param adapter
     * @param tenant
     * @param org
     */
    public PIBeaconSensor(Context context, PIAPIAdapter adapter, String tenant, String org) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mTenant = tenant;
        this.mOrg = org;

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
                enableBLE();
            }

        } catch (Exception e){
            Log.d(TAG, "Failed to create PZBeaconSensorService: " + e.getMessage());
        }

    }

    /**
     * Start sensing for beacons.
     */
    public void start() {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_ADAPTER, mAdapter);
        intent.putExtra(INTENT_PARAMETER_TENANT, mTenant);
        intent.putExtra(INTENT_PARAMETER_ORG, mOrg);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "START_SCANNING");
        mContext.startService(intent);
    }

    /**
     * Stop sensing for beacons.
     */
    public void stop() {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_ADAPTER, mAdapter);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "STOP_SCANNING");
        mContext.startService(intent);
    }

    /**
     * Sets the interval in which the device reports its location.
     *
     * @param sendInterval - send interval in ms
     */
    // TODO make sure this is working

    public void setSendInterval(long sendInterval) {
        mSendInterval = sendInterval;
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_SEND_INTERVAL, mSendInterval);
        mContext.startService(intent);
    }

    // must be called before starting the beacon sensor service

    /**
     *
     *
     * @param layout
     */
    public void addBeaconLayout(String layout) {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_BEACON_LAYOUT, layout);
        mContext.startService(intent);
    }

    private  boolean checkSupportBLE(){
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "ble_not_supported");
            Toast.makeText(mContext, "ble_not_supported", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void initBluetoothAdapter()throws Exception{
        if(mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if(mBluetoothAdapter == null)
                throw new Exception("Failed to get bluetooth adapter");
        }
    }

    private boolean isBLEOn(){
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * enable bluetooth in case it's off(admin permission)
     */
    private boolean enableBLE(){
        boolean response=true;
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            response=false;
            mBluetoothAdapter.enable();
        }
        return response;
    }
}
