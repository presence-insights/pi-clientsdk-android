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

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 *
 */
public class PIGeofenceCallbackServiceConnection<T extends PIGeofenceCallbackService> implements ServiceConnection {
    private T service;
    private boolean connected;
    private PIGeofenceCallback callback;

    public PIGeofenceCallbackServiceConnection() {
    }

    public PIGeofenceCallbackServiceConnection(PIGeofenceCallback callback) {
        this.callback = callback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onServiceConnected(ComponentName name, IBinder service) {
        PIGeofenceCallbackService.LocalBinder binder = (PIGeofenceCallbackService.LocalBinder) service;
        this.service = (T) binder.getService();
        this.connected = true;
        if (callback != null) {
            this.service.addCallback(callback);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (callback != null) {
            this.service.removeCallback(callback);
        }
        this.service = null;
        this.connected = false;
    }

    public boolean isConnected() {
        return connected;
    }

    public T getService() {
        return service;
    }
}
