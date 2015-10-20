package com.ibm.pisdk;

import android.util.Log;

/**
 * Created by hannigan on 10/14/15.
 */
public class PILogger {
    static boolean inDebugMode = false;

    static public int d(String tag, String msg) {
        return inDebugMode ? Log.d(tag, msg) : 0;
    }

    static public int e(String tag, String msg) {
        return inDebugMode ? Log.e(tag, msg) : 0;
    }

    static public void enableDebugMode(boolean enable) {
        inDebugMode = enable;
    }
}
