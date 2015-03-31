package com.ibm.pzsdk;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.*;

/**
 * Created by natalies on 20/11/2014.
 */
public class PZBeaconSensorService extends Service {
    private PIAPIAdapter pzAdapter;
    private Set<String> proximityUUIDs;
    private PZBeaconSensorDelegate delegate = null;
    private static final String INTENT_PARAMETER_ADAPTER = "adapter";
    private static final String INTENT_PARAMETER_COMMAND = "command";
    private static final String INTENT_PARAMETER_DELEGATE = "delegate";
    private static final String SHARD_PREFERENCES_NAME = "PZBeaconSensorService";
    private static final String SHARD_PREFERENCES_UUIDS_LIST = "UUIDsList";
    private static final String SHARD_PREFERENCES_PZ_ADAPTER_URL = "PZAdapterUrl";
    private static final long SCAN_PERIOD = 2000;
    private static final long STOP_PERIOD = 3000;
    private static final long WAIT_BETWEEN_SCAN_PERIOD = SCAN_PERIOD + STOP_PERIOD;
    private static final long WAIT_BEFORE_FIRST_SCAN_PERIOD = 7000;
    private String deviceId;

    private Handler mHandler = new Handler();
    private Handler aHandler = new Handler();
    private Handler cHandler = new Handler();
    private Map<Integer, PZBeaconData> detectedRelevantBeacons = new HashMap<Integer, PZBeaconData>();// minor, beaconData
    private Map<Integer, PZBeaconData> currentCycleRelevantBeacons = new HashMap<Integer, PZBeaconData>();// minor, beaconData
    private Map<Integer, PZBeaconData> previousCycleRelevantBeacons = new HashMap<Integer, PZBeaconData>();// minor, beaconData
    private BluetoothAdapter bluetoothAdapter = null;

private static final String INTERNAL_TAG = "PZBeaconSensorService";
    @Override
    public void onCreate() {
        Toast.makeText(this, "service created", Toast.LENGTH_SHORT).show();
        Log.d(INTERNAL_TAG, "************************   service created    ************************\n ");

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

            // Get device unique descriptor
            deviceId = getDeviceDescriptor();

            // Restore preferences
            restorePreferences();

            // Start the scan and send notifications process
            startScanProcess();

        } catch (Exception e){
            Log.d(INTERNAL_TAG, "Failed to create PZBeaconSensorService: "+ e.getMessage());
        }

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d(INTERNAL_TAG, "************************   service starting    ************************\n ");
        String command = new String();

        if (intent!=null && null != intent.getExtras()){
            pzAdapter = (PIAPIAdapter)intent.getExtras().get(INTENT_PARAMETER_ADAPTER);
            command = intent.getExtras().getString(INTENT_PARAMETER_COMMAND);
            try {
                delegate = (PZBeaconSensorDelegate) intent.getExtras().get(INTENT_PARAMETER_DELEGATE);
            } catch (Exception e){
                delegate = null;
            }
        }

        Log.d(INTERNAL_TAG, "Command = "+command+"; "+pzAdapter);
//        try {
//            if (command.equals("START_SCANNING")){
//                    proximityUUIDs.addAll(pzAdapter.retrieveProximityUUIDsSync());
//
//            } else if (command.equals("STOP_SCANNING")){
//                proximityUUIDs.removeAll(pzAdapter.retrieveProximityUUIDsSync());
//            }
//        } catch (Exception e) {
//            Log.d(INTERNAL_TAG, "Failed to init proximity UUIDs: " + e.getMessage());
//        }

        if (proximityUUIDs.isEmpty()) {
            Log.d(INTERNAL_TAG, "Proximity UUIDs list is empty - no need of scanning --> stop service");
            stopSelf();
        }

        //Save preferences
        savePreferences();

        // If we get killed, after returning from here, restart
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d(INTERNAL_TAG, "************************   service done    *****************************\n ");

        // Stop all scanning processes
        aHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        cHandler.removeCallbacksAndMessages(null);
        bluetoothAdapter.stopLeScan(mLeScanCallback);

        //Save preferences
        savePreferences();

        super.onDestroy();
    }

    private void restorePreferences()throws Exception{
        try {
            SharedPreferences settings = getSharedPreferences(SHARD_PREFERENCES_NAME, 0);
            proximityUUIDs = settings.getStringSet(SHARD_PREFERENCES_UUIDS_LIST, new HashSet<String>());
            String pzAdapterURL = settings.getString(SHARD_PREFERENCES_PZ_ADAPTER_URL, null);
//            if (pzAdapterURL != null) {
//                pzAdapter = new PZAPIAdapter(new URL(pzAdapterURL));
//            }
        } catch (Exception e) {
            throw new Exception("Un expected Exception while restoring Preferences");
        }


    }
    private void savePreferences(){
        SharedPreferences settings = getSharedPreferences(SHARD_PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(SHARD_PREFERENCES_UUIDS_LIST); // workaround for an android bug that resets the shard preferences for StringSet
        editor.putStringSet(SHARD_PREFERENCES_UUIDS_LIST, proximityUUIDs);
        editor.putString(SHARD_PREFERENCES_PZ_ADAPTER_URL, pzAdapter.getServerURL().toString());
        editor.apply();
    }
    private String getDeviceDescriptor() throws Exception {
        return getBluetoothMacAddress();
    }

    private String getBluetoothMacAddress() { return bluetoothAdapter.getAddress(); }

    private  boolean checkSupportBLE(){
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(INTERNAL_TAG, "ble_not_supported");
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void initBluetoothAdapter()throws Exception{
        if(bluetoothAdapter == null) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if(bluetoothAdapter == null)
                throw new Exception("Failed to get bluetooth adapter");
        }
    }

    private boolean isBLEOn(){
        return bluetoothAdapter.isEnabled();
    }

    /**
     * enable bluetooth in case it's off(admin permission)
     */
    private boolean enableBLE(){
        boolean response=true;
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            response=false;
            bluetoothAdapter.enable();
        }
        return response;
    }

    private void startScanProcess() {
        aHandler.postDelayed(recursiveScanAndStop, WAIT_BEFORE_FIRST_SCAN_PERIOD);
    }

    Runnable recursiveScanAndStop= new Runnable(){

        @Override
        public void run() {
            if (isBLEOn()) {
                scanStopAndUpdatePZ();
            } else {
                enableBLE();
            }
            aHandler.postDelayed(recursiveScanAndStop, WAIT_BETWEEN_SCAN_PERIOD);
        }
    };

    LinkedList<PZBeaconData> beacons;
    // set state by PZ expected protocol and send notification
    private void  updatePZ(){
        beacons = new LinkedList<PZBeaconData>();
        for (Integer minor : currentCycleRelevantBeacons.keySet()){
            PZBeaconData beaconData = currentCycleRelevantBeacons.get(minor);
            if (previousCycleRelevantBeacons.containsKey(minor)){ // beacon was heard in this cycle and also in the previous cycle
                // 0 - Old beacons (just update for new rssi)
                beaconData.setState(0);
            }
            else { // beacon was heard in this cycle but not in the previous cycle
                // 1 - New revealed beacons
                beaconData.setState(1);
            }
            beacons.add(beaconData);
        }

        for (Integer minor : previousCycleRelevantBeacons.keySet()){
            if (!currentCycleRelevantBeacons.containsKey(minor)){ // beacon was heard in the previous cycle but not in this cycle
                // -1 - Add exit ( = Unknown) beacons
                PZBeaconData beaconData = previousCycleRelevantBeacons.get(minor);
                beaconData.setState(-1);
                beaconData.setRssi(0);
                beaconData.setAccuracy(-1);
                beaconData.setProximity("UNKNOWN");
                beacons.add(beaconData);
            }
        }

        previousCycleRelevantBeacons = currentCycleRelevantBeacons;
        currentCycleRelevantBeacons = new HashMap<Integer, PZBeaconData>();

        // There is nothing to send to server
        if (beacons.isEmpty()) {
            return;
        }

        PIAPICompletionHandler completionHandler = (new PIAPICompletionHandler() {
                    @Override
                    public void onComplete(Object result, Exception e) {
                        if (e != null)
                            Log.d(INTERNAL_TAG, "notification massage sent to server with error\n " + e.getMessage());
                        else{
                            if (delegate != null) {
                                delegate.updatePZLocation(beacons);
                            }
                        }
            }
            });
//            if (pzAdapter != null) {
//                pzAdapter.sendNotificationMessageAsync(new PZNotificationMessage(deviceId, beacons), completionHandler);
//            } else {
//                Log.d(INTERNAL_TAG, "PZAdapter have not been initialized\n ");
//            }
    }
    private void scanStopAndUpdatePZ() {
        // stop scanning after SCAN_PERIOD and than update Presence Insights
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(mLeScanCallback);
                updatePZ();
            }
        }, SCAN_PERIOD);

        // start scanning
        bluetoothAdapter.startLeScan(mLeScanCallback);
    }
    // callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanData) {
                    cHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean patternFound = false;
                            int major = 0;
                            int minor = 0;
                            int txPower = 0;

                            String proximityUuid = "00000000-0000-0000-0000-000000000000";
                            int startByte = 2;
                            while (startByte <= 5) {
                                if (((int) scanData[startByte + 2] & 0xff) == 0x02 &&
                                        ((int) scanData[startByte + 3] & 0xff) == 0x15) {
                                    // yes!  This is an iBeacon
                                    patternFound = true;
                                    break;
                                } else if (((int) scanData[startByte] & 0xff) == 0x2d &&
                                        ((int) scanData[startByte + 1] & 0xff) == 0x24 &&
                                        ((int) scanData[startByte + 2] & 0xff) == 0xbf &&
                                        ((int) scanData[startByte + 3] & 0xff) == 0x16) {
                                    major = 0;
                                    minor = 0;
                                    proximityUuid = "00000000-0000-0000-0000-000000000000";

                                } else if (((int) scanData[startByte] & 0xff) == 0xad &&
                                        ((int) scanData[startByte + 1] & 0xff) == 0x77 &&
                                        ((int) scanData[startByte + 2] & 0xff) == 0x00 &&
                                        ((int) scanData[startByte + 3] & 0xff) == 0xc6) {

                                    major = 0;
                                    minor = 0;
                                    proximityUuid = "00000000-0000-0000-0000-000000000000";

                                }
                                startByte++;
                            }
                            if (patternFound == false) {
                                // This is not an iBeacon
                                return;
                            }
                            major = (scanData[startByte + 20] & 0xff) * 0x100 + (scanData[startByte + 21] & 0xff);
                            minor = (scanData[startByte + 22] & 0xff) * 0x100 + (scanData[startByte + 23] & 0xff);
                            txPower = (int) scanData[startByte + 24];
                            byte[] proximityUuidBytes = new byte[16];
                            System.arraycopy(scanData, startByte + 4, proximityUuidBytes, 0, 16);
                            String hexString = bytesToHex(proximityUuidBytes);
                            StringBuilder sb = new StringBuilder();
                            sb.append(hexString.substring(0, 8));
                            sb.append("-");
                            sb.append(hexString.substring(8, 12));
                            sb.append("-");
                            sb.append(hexString.substring(12, 16));
                            sb.append("-");
                            sb.append(hexString.substring(16, 20));
                            sb.append("-");
                            sb.append(hexString.substring(20, 32));
                            proximityUuid = sb.toString();

                            // Continue to forward to the server only if its the UUIDs we are listening on.
                            if (!proximityUUIDs.contains(proximityUuid)) {
                                return;
                            }


                            PZBeaconData beaconData = detectedRelevantBeacons.get(minor);
                            if (beaconData == null) {
                                beaconData = new PZBeaconData(proximityUuid, major, minor);
                                detectedRelevantBeacons.put(minor, beaconData);
                            }
                            currentCycleRelevantBeacons.put(minor, beaconData);
                            beaconData.setRssi(rssi);
                            double avgRSSI = beaconData.getAvgRssi();
                            double accuracy = calculateAccuracy(txPower, avgRSSI);
                            String proximity = calculateProximity(accuracy);

                            beaconData.setAccuracy(accuracy);
                            beaconData.setProximity(proximity);
                        }


                    });
                }
            };

    final private static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0;
        }
        double ratio = txPower - rssi;
        double ratioLinear = Math.pow(10, ratio / 10);
        return Math.sqrt(ratioLinear);
    }

    protected static String calculateProximity(double accuracy) {
        if (accuracy < 0) {
            return "UNKNOWN";
        }

        if (accuracy < 0.5) {
            return "IMMEDIATE";
        }
        if (accuracy <= 4.0) {
            return "NEAR";
        }
        return "FAR";

    }
}
