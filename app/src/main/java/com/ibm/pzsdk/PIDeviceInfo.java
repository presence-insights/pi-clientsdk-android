package com.ibm.pzsdk;

import android.content.Context;

/**
 * Created by hannigan on 4/24/15.
 */
public class PIDeviceInfo extends DeviceInfo {

    private Context mContext;

    public PIDeviceInfo(Context context) {
        super();
        mContext = context;
        setDescriptor();
    }

    @Override
    protected void setDescriptor() {
        PIDeviceID device = new PIDeviceID(mContext);
        setDescriptor(device.getMacAddress());
    }

}
