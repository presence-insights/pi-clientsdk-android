/**
 * Copyright (c) 2015 IBM Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.pi.core;

import android.util.Log;

/**
 * This class manages logging for Presence Insights. Enable debug mode to flood LogCat with useful
 * information regarding the classes within.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PILogger {
    static boolean inDebugMode = false;

    /**
     * Send a DEBUG log message.
     *
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    static public int d(String tag, String msg) {
        return inDebugMode ? Log.d(tag, msg) : 0;
    }

    /**
     * Send an ERROR log message.
     *
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    static public int e(String tag, String msg) {
        return inDebugMode ? Log.e(tag, msg) : 0;
    }

    /**
     * Enables/Disables logging.
     *
     * @param enable enable or disable logging.
     */
    static public void enableDebugMode(boolean enable) {
        inDebugMode = enable;
    }
}
