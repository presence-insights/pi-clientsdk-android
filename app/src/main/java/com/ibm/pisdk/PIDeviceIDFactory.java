package com.ibm.pisdk;

import android.content.Context;

/**
 * Factory class for creating instances of {@link PIDeviceID}.
 */
public class PIDeviceIDFactory {
    /**
     * Instantiation is not permitted.
     */
    private PIDeviceIDFactory() {
    }

    /**
     * Create a new {@link PIDeviceID}.
     * @param context teh Android context used to retrieve device information.
     * @return a newly created {@link PIDeviceID}.
     */
    public static PIDeviceID newInstance(Context context) {
        return new PIDeviceID(context);
    }
}
