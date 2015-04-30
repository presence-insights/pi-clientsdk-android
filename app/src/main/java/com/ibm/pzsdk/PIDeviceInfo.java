package com.ibm.pzsdk;

import android.content.Context;

/**
 * This class provides the Presence Insights' implementation for device descriptor.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIDeviceInfo extends DeviceInfo {

    /**
     * Activity context
     */
    private Context mContext;

    /**
     *
     * @param context Activity Context
     */
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
