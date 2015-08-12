//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
package com.ibm.pisdk;

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
