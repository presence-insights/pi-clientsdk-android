package com.ibm.pzsdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by natalies on 22/09/2014.
 */
public class PZBeaconSensor extends BroadcastReceiver {

    private static final String INTENT_PARAMETER_ADAPTER = "adapter";
    private static final String INTENT_PARAMETER_COMMAND = "command";
    private static final String INTENT_PARAMETER_DELEGATE = "delegate";

    private static final String INTERNAL_TAG = "PZBeaconSensor";

    private Activity activity;
    private PZAPIAdapter pzAdapter;
    private PZBeaconSensorDelegate delegate;

    public PZBeaconSensor() {}

    public PZBeaconSensor(PZAPIAdapter adapter, Activity activity, PZBeaconSensorDelegate delegate) {
        this.pzAdapter = adapter;
        this.activity = activity;
        this.delegate = delegate;
    }

    public void stop() throws Exception{
        if (activity == null || pzAdapter == null){
            throw new Exception("Parameters activity or pzAdapter have not been initialized");
        }
        Intent intent = new Intent(this.activity, PZBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "STOP_SCANNING");
        intent.putExtra(INTENT_PARAMETER_ADAPTER, this.pzAdapter);
        this.activity.startService(intent);
    }

    public void start() throws Exception {
        if (activity == null || pzAdapter == null){
            throw new Exception("Parameters activity or pzAdapter have not been initialized");
        }
        Intent intent = new Intent(this.activity, PZBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "START_SCANNING");
        intent.putExtra(INTENT_PARAMETER_ADAPTER, this.pzAdapter);
        intent.putExtra(INTENT_PARAMETER_DELEGATE, this.delegate);
        this.activity.startService(intent);

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent PZBeaconSensorIntent = new Intent(context, PZBeaconSensorService.class);
        context.startService(PZBeaconSensorIntent);
    }
}