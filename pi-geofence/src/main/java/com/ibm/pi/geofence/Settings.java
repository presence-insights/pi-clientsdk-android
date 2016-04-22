/**
 * Copyright (c) 2015, 2016 IBM Corporation. All rights reserved.
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
import android.util.Log;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

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
     * Name of the password-based encryption algorithm.
     */
    private static final String ALGO = "PBEWITHSHA256AND256BITAES-CBC-BC";
    /**
     * Length of the salt.
     */
    private static final int SALT_LENGTH = 20;
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
    private SecretKey secretKey;
    private boolean keyError = false;
    private final String pwd;
    private final boolean encryptionEnabled;

    /*
    // for debugging purposes only
    static {
        try {
            for (Provider provider : Security.getProviders()) {
                log.debug("Provider: " + provider.getName() + " version: " + provider.getVersion());
                for (Provider.Service service : provider.getServices()) {
                    log.debug(String.format("  Type : %-30s  Algorithm: %-30s\n", service.getType(), service.getAlgorithm()));
                }
            }
        } catch(Exception e) {
            log.error("error display PBE algorithms: ", e);
        }
    }
    */

    /**
     * Initialize wih the specified app context.
     * @param context used to compute the file name from the app package name.
     */
    public Settings(Context context) {
        this(context, null);
    }

    /**
     * Initialize wih the specified app context.
     * @param context used to compute the file name from the app package name.
     */
    Settings(Context context, String pwd) {
        this.encryptionEnabled = pwd != null;
        this.pwd = pwd;
        String name;
        try {
            // encode the app's package name to Base64 in a filename-safe way
            // prefix with "." to have a hidden file
            name = "." + encode(context.getPackageName());
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

    public Settings clear() {
        properties.clear();
        return this;
    }

    public Set<String> getPropertyNames() {
        return properties.stringPropertyNames();
    }

    public Settings remove(String key) {
        if (key != null) {
            properties.remove(key);
        }
        return this;
    }

    /**
     * Actually persist the changes to the file.
     */
    public void commit() {
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
                InputStream is = new BufferedInputStream(new FileInputStream(file));
                if (encryptionEnabled) {
                    byte[] salt = new byte[SALT_LENGTH];
                    int count = 0;
                    while (count < SALT_LENGTH) {
                        int n = is.read(salt);
                        if (n <= 0) {
                            throw new EOFException(String.format("eof reading salt, could only read %d out of %d bytes", count, SALT_LENGTH));
                        }
                        count += n;
                    }
                    PBEParameterSpec params = new PBEParameterSpec(salt, 1000);
                    Cipher cipher = Cipher.getInstance(ALGO);
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), params);
                    is = new CipherInputStream(is, cipher);
                }
                properties.load(is);
                is.close();
                //log.debug("successfully loaded properties from " + file);
            }
        } catch(Exception e) {
            //log.error(String.format("error loading properties from %s", file), e);
            log.error(String.format("error loading properties from %s : %s", file, Log.getStackTraceString(e)));
            clear();
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
            if (encryptionEnabled) {
                byte[] salt = new byte[SALT_LENGTH];
                new SecureRandom().nextBytes(salt);
                os.write(salt);
                os.flush();
                PBEParameterSpec params = new PBEParameterSpec(salt, 1000);
                Cipher cipher = Cipher.getInstance(ALGO);
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), params);
                os = new CipherOutputStream(os, cipher);
            }
            properties.store(os, null);
            os.close();
            //log.debug("successfully stored properties to " + file);
        } catch(Exception e) {
            //log.error(String.format("error storing properties to %s", file), e);
            log.error(String.format("error storing properties to %s : %s", file, Log.getStackTraceString(e)));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        Properties props = new Properties();
        Enumeration names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (!name.toLowerCase().contains("password")) {
                props.put(name, properties.getProperty(name));
            }
        }
        return String.format("%s[filename=%s, properties=%s]", getClass().getSimpleName(), fileName, props);
    }

    private SecretKey getSecretKey() {
        if (!keyError && (secretKey == null)) {
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
                secretKey = skf.generateSecret(new PBEKeySpec(pwd.toCharArray()));
            } catch(Exception e) {
                keyError = true;
                log.error("error getting secret key: " + Log.getStackTraceString(e));
            }
        }
        return secretKey;
    }

    private static String encode(String source) throws Exception {
        return Base64.encodeToString(source.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private static String decode(String source) throws Exception {
        return source == null ? null : new String(Base64.decode(source, Base64.NO_PADDING | Base64.URL_SAFE), "UTF-8");
    }
}