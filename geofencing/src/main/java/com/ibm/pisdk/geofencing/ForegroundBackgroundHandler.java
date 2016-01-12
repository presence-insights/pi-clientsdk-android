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

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

class ForegroundBackgroundHandler implements ComponentCallbacks2 {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = ForegroundBackgroundHandler.class.getSimpleName();
    /**
     * The application context of the app using the SDK.
     */
    private final Context appContext;
    /**
     * The last computed state maintained by this handler.
     */
    private boolean inBackground = false;
    /**
     * A list of registered callbacks top notify of app state changes.
     */
    private final List<AppStateCallbacks> callbacks = new CopyOnWriteArrayList<>();
    /**
     * Timer that runs the periodic app state checks.
     */
    private final Timer timer;

    ForegroundBackgroundHandler(Context appContext) {
        this.appContext = appContext;
        this.appContext.registerComponentCallbacks(this);
        this.timer = new Timer("AppStateTimer", true);
        this.timer.schedule(new DetectForegroundTask(), 0L, 3000L);
    }

    /**
     * Get the last computed state maintained by this handler.
     * @return {@code true} if the app is currently marked as being in the background, {@code false} otherwise.
     */
    synchronized boolean isInBackground() {
        return inBackground;
    }

    synchronized void setInBackground(boolean inBackground) {
        this.inBackground = inBackground;
    }

    /**
     * Register the specified callbacks instance.
     */
    void registerAppStateCallbacks(AppStateCallbacks callback) {
        if (callback != null) {
            callbacks.add(callback);
        }
    }

    /**
     * un-register the specified callbacks instance.
     */
    void unregisterAppStateCallbacks(AppStateCallbacks callback) {
        if (callback != null) {
            callbacks.remove(callback);
        }
    }

    /**
     * Search the running processes for the current app and check whether it is in the foreground or background
     * @return {@code true} when the app is currently in the foreground, {@code false} otherwise.
     */
    boolean checkInForeground() {
        ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        String pkgName = appContext.getPackageName();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if ((process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) && pkgName.equals(process.processName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTrimMemory(int level) {
        if ((level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) && !isInBackground()) {
            fireNotifications(true);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    /**
     * Runs periodic checks on the app state, and emits notifications accordingly.
     */
    private class DetectForegroundTask extends TimerTask {
        @Override
        public void run() {
            boolean b = checkInForeground();
            if (b && isInBackground()) {
                fireNotifications(false);
            } else if (!b && !isInBackground()) {
                fireNotifications(true);
            }
        }
    }

    /**
     * Interface for the callbacks to register for the classes that wish to be notified of the app state changes.
     */
    interface AppStateCallbacks {
        /**
         * Called when the app goes back to the foreground.
         */
        void onAppInForeground();

        /**
         * Called when the app goes to the background.
         */
        void onAppInBackground();
    }

    /**
     * Notify the registered callbacks of an app state change.
     * @param inBackground {@code true} when the app went to the background, {@code false} when it went to the foreground.
     */
    private void fireNotifications(boolean inBackground) {
        if (inBackground) {
            setInBackground(true);
            for (AppStateCallbacks callback : callbacks) {
                callback.onAppInBackground();
            }
        } else {
            setInBackground(false);
            for (AppStateCallbacks callback : callbacks) {
                callback.onAppInForeground();
            }
        }
    }
}
