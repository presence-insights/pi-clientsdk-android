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

package com.ibm.pi.geofence;

import android.os.Environment;
import android.util.Log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;

import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * This class configures Log4j logging and provide factory methods for creating loggers
 * while ensuring the configuration is properly done.
 * <p>Example usage:
 * <pre>
 *     // with a logger name:
 *     private static Logger logWithName = LoggingConfiguration.getLogger(MyClass.class.getSimpleName());
 *     // with a class:
 *     private static Logger logWithClass = LoggingConfiguration.getLogger(MyClass.class);
 * </pre>
 */
public class LoggingConfiguration {
    /**
     * Whether configuration already happened.
     */
    private static boolean configured = false;
    /**
     * Path to the log file on the file system.
     */
    private static String logfile = null;

    /**
     * Configure log4j.
     */
    private synchronized static void configure() {
        if (!configured) {
            configured = true;
            final LogConfigurator configurator = new LogConfigurator();
            configurator.setUseFileAppender(true);

            logfile = new File(Environment.getExternalStorageDirectory(), File.separator + "pi-sdk.log").getAbsolutePath();
            configurator.setFileName(logfile);
            Log.v("LoggingConfiguration", "configure() log file=" + logfile);
            configurator.setImmediateFlush(true);
            configurator.setMaxFileSize(1024 * 1024);
            configurator.setMaxBackupSize(0);
            configurator.setFilePattern("%d [%-5p][%c.%M(%L)] %m%n");
            configurator.setUseLogCatAppender(true);
            configurator.setRootLevel(Level.DEBUG);
            configurator.setLevel("com.ibm.pisdk", Level.DEBUG);
            configurator.setLevel("com.orm", Level.WARN);
            configurator.configure();
        }
    }

    /**
     * Get te path to the log file.
     * @return the log file path on the device's file system.
     */
    public static String getLogFile() {
        return logfile;
    }

    /**
     * Create a logger for the specified class.
     * @param clazz the class from which the logger name is computed.
     * @return a {@link Logger} instance.
     */
    public static Logger getLogger(Class<?> clazz) {
        configure();
        return Logger.getLogger(clazz);
    }

    /**
     * Create a logger with the specified name.
     * @param loggerName the logger name.
     * @return a {@link Logger} instance.
     */
    public static Logger getLogger(String loggerName) {
        configure();
        return Logger.getLogger(loggerName);
    }
}
