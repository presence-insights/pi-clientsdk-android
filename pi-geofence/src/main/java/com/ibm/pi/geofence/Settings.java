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

import android.content.Context;
import android.os.Environment;
import android.util.Base64;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * A container for app properties and settings that are persistent accross app uninstall and device reboots.
 * <p>It is backed by a file located on the device external storage, including emulated external storage for devices without an SD card.
 */
public class Settings {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(Settings.class.getSimpleName());
    /**
     * Fallback name used if the file name for the settings cannot be computed
     */
    private static final String PROPS_PATH = ".pi-sdk-props";
    /**
     * The value separator for multi-valued properties, e.g. list of fnece codes.
     */
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("|", Pattern.LITERAL);
    /**
     * The name of the backing file. It is computed by encoding the app's package name in Bas64 (without padding).
     */
    private final String fileName;
    /**
     * Contains the properties. To persist any change, {@link #commit()} must be called.
     */
    private final Properties properties = new Properties();
    /**
     * Used for synchronization of baking file access.
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Initialize wih the specified app context.
     * @param context used to compute the file name from the app package name.
     */
    public Settings(Context context) {
        String name;
        try {
            // encode the app's package name to Base64 in a filename-safe way
            // prefix with "." to have a hidden file
            name = "." + Base64.encodeToString(context.getPackageName().getBytes("UTF-8"), Base64.NO_PADDING|Base64.URL_SAFE);
        } catch(Exception e) {
            name = PROPS_PATH;
        }
        this.fileName = name;
        loadProperties();
    }

    public String getString(String key, String defValue) {
        return properties.getProperty(key, defValue);
    }

    public Settings putString(String key, String value) {
        properties.setProperty(key, value);
        return this;
    }

    public Collection<String> getStrings(String key, Collection<String> defValue) {
        String s = getString(key, null);
        if (s != null) {
            String[] values = SEPARATOR_PATTERN.split(s);
            if (values != null) {
                return new ArrayList<>(Arrays.asList(values));
            }
        }
        return defValue;
    }

    public Settings putStrings(String key, Collection<String> value) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String s: value) {
            if (count > 0) {
                sb.append(SEPARATOR_PATTERN.pattern());
            }
            sb.append(s);
            count++;
        }
        properties.setProperty(key, sb.toString());
        return this;
    }

    public int getInt(String key, int defValue) {
        return Integer.valueOf(getString(key, Integer.toString(defValue)));
    }

    public Settings putInt(String key, int value) {
        return putString(key, Integer.toString(value));
    }

    public long getLong(String key, long defValue) {
        return Long.valueOf(getString(key, Long.toString(defValue)));
    }

    public Settings putLong(String key, long value) {
        return putString(key, Long.toString(value));
    }

    public double getDouble(String key, double defValue) {
        return Double.valueOf(getString(key, Double.toString(defValue)));
    }

    public Settings putDouble(String key, double value) {
        return putString(key, Double.toString(value));
    }

    public boolean getBoolean(String key, boolean defValue) {
        return Boolean.valueOf(getString(key, Boolean.toString(defValue)));
    }

    public Settings putBoolean(String key, boolean value) {
        return putString(key, Boolean.toString(value));
    }

    /**
     * Actually persist the changes to the file.
     */
    public void commit() {
        /*
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                storeProperties();
                return null;
            }
        }.execute();
        */
        storeProperties();
    }

    /**
     * Load the properties from a file.
     */
    private void loadProperties() {
        File file = null;
        lock.lock();
        try {
            file = new File(Environment.getExternalStorageDirectory(), File.separator + fileName);
            //log.debug("loading properties from " + file);
            if (file.exists()) {
                properties.load(new BufferedInputStream(new FileInputStream(file)));
                //log.debug("successfully loaded properties from " + file + " : " + properties);
            }
        } catch(Exception e) {
            log.error(String.format("error loading properties from %s", file), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Store the properties to file.
     */
    private void storeProperties() {
        File file = new File(Environment.getExternalStorageDirectory(), File.separator + fileName);
        lock.lock();
        try {
            //log.debug("storing properties to " + file);
            OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            properties.store(os, null);
            os.close();
        } catch(Exception e) {
            log.error(String.format("error storing properties to %s", file), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return String.format("%s[filename=%s, properties=%s]", getClass().getSimpleName(), fileName, properties);
    }
}