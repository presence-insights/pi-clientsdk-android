/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.pisdk.geofencing;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.log4j.Level;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * .
 */
public class LoggingConfiguration {
    /**
     * Whether configuration already happened.
     */
    private static boolean configured = false;
    private static String logfile = null;

    /**
     * Configure log4j.
     * @param context used to retrieve the root file dir of the app for internal storage.
     */
    public synchronized static void configure(Context context) {
        if (!configured) {
            configured = true;
            final LogConfigurator configurator = new LogConfigurator();
            configurator.setUseFileAppender(true);

            //logfile = new File(context.getFilesDir(), File.separator + "pi-sdk.log").getAbsolutePath();
            logfile = new File(Environment.getExternalStorageDirectory(), File.separator + "pi-sdk.log").getAbsolutePath();
            configurator.setFileName(logfile);
            Log.v("LoggingConfiguration", "configure() log file=" + logfile);
            configurator.setImmediateFlush(true);
            configurator.setMaxFileSize(1024 * 1024);
            configurator.setMaxBackupSize(0);
            configurator.setUseLogCatAppender(true);
            configurator.setRootLevel(Level.DEBUG);
            configurator.setLevel("com.ibm.pisdk", Level.DEBUG);
            configurator.setLevel("com.orm", Level.WARN);
            configurator.configure();
        }
    }

    public static String getLogFile() {
        return logfile;
    }
}
