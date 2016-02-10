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

package com.ibm.pisdk.geofencing.demo;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private final SharedPreferences prefs;

    public Settings(Context context) {
        prefs = context.getSharedPreferences("pi-sdk-demo", Context.MODE_PRIVATE);
    }

    public String getString(String key, String defValue) {
        return prefs.getString(key, defValue);
    }

    public Settings putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
        return this;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    public Settings putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
        return this;
    }
}