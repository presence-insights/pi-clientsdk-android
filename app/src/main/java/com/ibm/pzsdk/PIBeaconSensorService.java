package com.ibm.pzsdk;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class PIBeaconSensorService extends Service implements BeaconConsumer {
    private String TAG = PIBeaconSensorService.class.getSimpleName();

    private PIAPIAdapter mPiApiAdapter;
    private BeaconManager mBeaconManager;
    private Region demoEstimoteRegion = new Region("b9407f30-f5f8-466e-aff9-25556b57fe6d", null, null, null);
    private Set<String> proximityUUIDs = null;
    private PZBeaconSensorDelegate delegate = null;

    private static final String INTENT_PARAMETER_ADAPTER = "adapter";
    private static final String INTENT_PARAMETER_COMMAND = "command";
    private static final String INTENT_PARAMETER_DELEGATE = "delegate";
    private static final String INTENT_PARAMETER_BEACON_LAYOUT = "beacon_layout";
    private static final String INTENT_PARAMETER_SEND_INTERVAL = "send_interval";
    private static final String INTENT_PARAMETER_TENANT = "tenant";
    private static final String INTENT_PARAMETER_ORG = "org";

    private long sendInterval = 5000;
    private long lastSendTime = 0;
    private long currentTime = 0;

    private String mTenant;
    private String mOrg;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // methods
    public PIBeaconSensorService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = "";
        Bundle extras = intent.getExtras();

        // lazily instantiate beacon manager
        if (mBeaconManager == null) {
            mBeaconManager = BeaconManager.getInstanceForApplication(this);
        }

        // check passed in intent for commands sent from Beacon Sensor wrapper class
        if (extras != null) {
            if (extras.get(INTENT_PARAMETER_ADAPTER) != null) {
                mPiApiAdapter = (PIAPIAdapter) extras.get(INTENT_PARAMETER_ADAPTER);
            }
            if (extras.get(INTENT_PARAMETER_TENANT) != null) {
                mTenant = extras.getString(INTENT_PARAMETER_TENANT);
            }
            if (extras.get(INTENT_PARAMETER_ORG) != null) {
                mOrg = extras.getString(INTENT_PARAMETER_ORG);
            }
            if (extras.get(INTENT_PARAMETER_DELEGATE) != null) {
                delegate = (PZBeaconSensorDelegate) intent.getExtras().get(INTENT_PARAMETER_DELEGATE);
            } else {
                delegate = null;
            }
            if (extras.getLong(INTENT_PARAMETER_SEND_INTERVAL, -1) > 0) {
                sendInterval = extras.getLong(INTENT_PARAMETER_SEND_INTERVAL);
            }
            if (!extras.getString(INTENT_PARAMETER_BEACON_LAYOUT, "").equals("")) {
                mBeaconManager.getBeaconParsers().add(new BeaconParser()
                        .setBeaconLayout(intent.getStringExtra(INTENT_PARAMETER_BEACON_LAYOUT)));
            }
            if (!extras.getString(INTENT_PARAMETER_COMMAND, "").equals("")) {
                command = extras.getString(INTENT_PARAMETER_COMMAND);
                if (command.equals("START_SCANNING")){
                    // get proximityUUIDs, then, in completion callback, bind to beacon manager
                    mBeaconManager.bind(this);
                } else if (command.equals("STOP_SCANNING")){
                    // unbind from beacon manager and clear proximityUUIDs list
                    mBeaconManager.unbind(this);
                }
            }
        }

        // If we get killed, after returning from here, restart
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                log("did enter region: " + region.toString());
            }

            @Override
            public void didExitRegion(Region region) {
                log("did exit region: " + region.toString());
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: " + state);
            }
        });

        mBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    currentTime = System.currentTimeMillis();
                    if (currentTime - lastSendTime > sendInterval) {
                        lastSendTime = currentTime;
                        sendBeaconNotification(beacons);
                    }
                }
            }
        });

        if (proximityUUIDs == null || proximityUUIDs.isEmpty()) {
            mPiApiAdapter.getProximityUUIDs(mTenant, mOrg, new PIAPICompletionHandler() {
                @Override
                public void onComplete(Object result, Exception e) {
                    JSONArray uuids = null;
                    try {
                        uuids = JSONArray.parse((String) result);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (uuids == null) {
                        startMonitoringAndRangingBeaconsInRegion(demoEstimoteRegion);
                    } else {
                        startMonitoringAndRangingBeaconsInRegion(new Region((String) uuids.toArray()[0], null, null, null));
                    }
                }
            });
        } else {
            startMonitoringAndRangingBeaconsInRegion(new Region(proximityUUIDs.iterator().next(), null, null, null));
        }
    }

    private void startMonitoringAndRangingBeaconsInRegion(Region region) {
        try {
            mBeaconManager.startMonitoringBeaconsInRegion(region);
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException re) {    }
    }

    private void sendBeaconNotification(Collection<Beacon> beacons) {
        log("sending beacon notification with this collection of beacons:");
        JSONObject payload = buildBeaconPayload(beacons);
        mPiApiAdapter.sendBeaconNotificationMessage(mTenant, mOrg, payload, new PIAPICompletionHandler() {
            @Override
            public void onComplete(Object result, Exception e) {
                log("completed sending beacon notification message");
            }
        });
    }

    private JSONObject buildBeaconPayload(Collection<Beacon> beacons) {
        JSONObject payload = new JSONObject();
        JSONArray beaconArray = new JSONArray();

        for (Beacon b : beacons) {
            PIBeaconData data = new PIBeaconData(b);
            beaconArray.add(data.getBeaconAsJson());
        }
        payload.put("bnm", beaconArray);

        log(payload.toString());

        return payload;
    }

    @Override
    public void onDestroy() {
        mBeaconManager.unbind(this);
        super.onDestroy();
    }

    private void log(String msg) {
        Log.i(TAG, msg);
    }
}
