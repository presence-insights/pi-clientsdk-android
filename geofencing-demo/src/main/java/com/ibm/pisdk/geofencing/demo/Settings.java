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
    public static String server;
    public static boolean ssl;
    public static String applicationId;
    public static String tenant;

    public static void load(Context c) {
        SharedPreferences settings = c.getSharedPreferences("login_data", Context.MODE_PRIVATE);
        server = settings.getString("server", null);
        ssl = settings.getBoolean("ssl", false);
        applicationId = settings.getString("applicationId", null);
        tenant = settings.getString("tenant", null);
    }

    public static void store(Context c) {
        SharedPreferences settings = c.getSharedPreferences("login_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("server", server);
        editor.putBoolean("ssl", ssl);
        editor.putString("applicationId", applicationId);
        editor.putString("tenant", tenant);
        // Commit the edits!
        editor.commit();
    }
}