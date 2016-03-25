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
