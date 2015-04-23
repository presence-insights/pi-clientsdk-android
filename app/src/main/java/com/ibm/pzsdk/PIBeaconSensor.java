package com.ibm.pzsdk;

import android.content.Context;
import android.content.Intent;

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

    private PIAPIAdapter mAdapter;
    private final String mTenant;
    private final String mOrg;

    public long sendInterval = 2000;

    private Context mContext;

    public PIBeaconSensor(Context context, PIAPIAdapter adapter, String tenant, String org) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mTenant = tenant;
        this.mOrg = org;
    }

    public void start() {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_ADAPTER, mAdapter);
        intent.putExtra(INTENT_PARAMETER_TENANT, mTenant);
        intent.putExtra(INTENT_PARAMETER_ORG, mOrg);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "START_SCANNING");
        mContext.startService(intent);
    }
    public void stop() {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_ADAPTER, mAdapter);
        intent.putExtra(INTENT_PARAMETER_COMMAND, "STOP_SCANNING");
        mContext.startService(intent);
    }

    public void setSendInterval(long sendInterval) {
        this.sendInterval = sendInterval;
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_SEND_INTERVAL, this.sendInterval);
        mContext.startService(intent);
    }

    // must be called before starting the beacon sensor service
    public void addBeaconLayout(String layout) {
        Intent intent = new Intent(mContext, PIBeaconSensorService.class);
        intent.putExtra(INTENT_PARAMETER_BEACON_LAYOUT, layout);
        mContext.startService(intent);
    }



}
