package com.ibm.pi.geofence;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.ibm.pi.geofence.rest.PIHttpService;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Contains configuration information on the geofencing service that can be transmitted accross processes via {@code Intent}s.
 */
class ServiceConfig implements Serializable {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(ServiceConfig.class.getSimpleName());
    //static final String GEOFENCING_SERVICE_CONFIG = "geofencing_service_config";
    private static final String PREFIX = "com.ibm.pi.sdk.";
    static final String EXTRA_LOCATION_UPDATE_FLAG =  PREFIX + "extra.location_update";
    static final String EXTRA_SERVER_URL =            PREFIX + "extra.server";
    static final String EXTRA_TENANT_CODE =           PREFIX + "extra.tenant";
    static final String EXTRA_ORG_CODE =              PREFIX + "extra.org";
    static final String EXTRA_USERNAME =              PREFIX + "extra.username";
    static final String EXTRA_PASSWORD =              PREFIX + "extra.password";
    static final String EXTRA_MAX_DISTANCE =          PREFIX + "extra.max_distance";
    static final String EXTRA_PACKAGE_NAME =          PREFIX + "extra.package";
    static final String EXTRA_CALLBACK_SERVICE_NAME = PREFIX + "extra.callback_service";
    static final String EXTRA_GEOFENCES =             PREFIX + "extra.geofences";
    static final String EXTRA_DELETED_GEOFENCES =     PREFIX + "extra.deleted_geofences";
    static final String EXTRA_EVENT_TYPE =            PREFIX + "extra.event_type";
    static final String EXTRA_LATITUDE =              PREFIX + "extra.latitude";
    static final String EXTRA_LONGITUDE =             PREFIX + "extra.longitude";
    static final String EXTRA_REBOOT_EVENT_FLAG =     PREFIX + "extra.reboot_event";
    static final String EXTRA_LAST_SYNC_DATE =        PREFIX + "extra.last_sync_date";

    enum EventType {
        ENTER,
        EXIT,
        SERVER_SYNC
    }

    String serverUrl;
    String tenantCode;
    String orgCode;
    String username;
    String password;
    double maxDistance;
    String packageName;
    String callbackServiceName;
    List<PersistentGeofence> geofences;
    EventType eventType;
    LatLng newLocation;
    List<String> deletedGeofences;

    /**
     * Create an application context based on the app's package name.
     */
    Context createContext(Service service) {
        Context context = null;
        try {
            context = service.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE);
            //log.debug(String.format("successfully created context for %s : %s", packageName, context));
        } catch(Exception e) {
            log.error(String.format("error creating context for %s", packageName), e);
        }
        return context;
    }

    /**
     * Attempt to load the class of the user-specified callback service,
     * so the service can be invoked as:
     * <pre>
     * ServiceConfig config = ...;
     * Context context = ...;
     * Class<? extends PIGeofenceCallbackService> clazz =
     *   config.loadCallbackServiceClass(context);
     * Intent intent = new Intent(context, clazz);
     * ...
     * context.startService(intent)
     * </pre>
     * @param context the context whose class loader loads the desired class.
     * @return a class that extends {@link PIGeofenceCallbackService}.
     */
    @SuppressWarnings("unchecked")
    Class<? extends PIGeofenceCallbackService> loadCallbackServiceClass(Context context) {
        Class<? extends PIGeofenceCallbackService> clazz = null;
        if (callbackServiceName != null) {
            try {
                ClassLoader cl = context.getClassLoader();
                clazz = (Class<? extends PIGeofenceCallbackService>) Class.forName(callbackServiceName, true, cl);
            } catch(Exception e) {
                log.error(String.format("exeption loading callback service class '%s'", callbackServiceName), e);
            } catch(Error e) {
                log.error(String.format("error loading callback service class '%s'", callbackServiceName), e);
                throw e;
            }
        }
        return clazz;
    }

    /**
     * Set the values of the fields in this class from extras stored in the specified geofencing service.
     */
    ServiceConfig fromGeofencingManager(PIGeofencingManager service) {
        PIHttpService httpService = service.httpService;
        serverUrl = httpService.getServerURL();
        tenantCode = httpService.getTenantCode();
        orgCode = httpService.getOrgCode();
        username = httpService.getUsername();
        password = httpService.getPassword();
        maxDistance = service.maxDistance;
        packageName = service.context.getPackageName();
        if (service.callbackServiceName != null) {
            callbackServiceName = service.callbackServiceName;
        }
        debugCheck();
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[')
            .append("serverUrl=").append(serverUrl)
            .append(", tenantCode=").append(tenantCode)
            .append(", orgCode=").append(orgCode)
            .append(", username=").append(username)
            .append(", passord=********") // never print the password!
            .append(", maxDistance=").append(maxDistance)
            .append(", packageName=").append(packageName)
            .append(", callbackServiceName=").append(callbackServiceName)
            .append(", geofences=").append(geofences)
            .append(", eventType=").append(eventType)
            .append(", newLocation=").append(newLocation)
            .append(']').toString();
    }

    /**
     * Set the values of the fields in this class from extras stored in the specified intent.
     */
    ServiceConfig fromIntent(Intent intent) {
        serverUrl = intent.getStringExtra(EXTRA_SERVER_URL);
        tenantCode = intent.getStringExtra(EXTRA_TENANT_CODE);
        orgCode = intent.getStringExtra(EXTRA_ORG_CODE);
        username = intent.getStringExtra(EXTRA_USERNAME);
        password = intent.getStringExtra(EXTRA_PASSWORD);
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        callbackServiceName = intent.getStringExtra(EXTRA_CALLBACK_SERVICE_NAME);
        maxDistance = intent.getDoubleExtra(EXTRA_MAX_DISTANCE, 10_000d);
        if (intent.getBooleanExtra(EXTRA_LOCATION_UPDATE_FLAG, false)) {
            newLocation = new LatLng(intent.getDoubleExtra(EXTRA_LATITUDE, 0d), intent.getDoubleExtra(EXTRA_LONGITUDE, 0d));
        }
        String s = intent.getStringExtra(EXTRA_GEOFENCES);
        if (s != null) {
            String[] codes = s.split("\\|");
            if ((codes != null) && (codes.length > 0)) {
                geofences = GeofencingUtils.geofencesFromCodes(Arrays.asList(codes));
            }
        }
        s = intent.getStringExtra(EXTRA_DELETED_GEOFENCES);
        if (s != null) {
            String[] codes = s.split("\\|");
            if ((codes != null) && (codes.length > 0)) {
                deletedGeofences = Arrays.asList(codes);
            }
        }
        s = intent.getStringExtra(EXTRA_EVENT_TYPE);
        if (s != null) {
            try {
                eventType = EventType.valueOf(s);
            } catch(Exception ignore) {
            }
        }
        debugCheck();
        return this;
    }

    /**
     * Converts the fields in this class to simple types and adds them as extras to the specified intent.
     */
    ServiceConfig toIntent(Intent intent) {
        debugCheck();
        intent.putExtra(EXTRA_SERVER_URL, serverUrl);
        intent.putExtra(EXTRA_TENANT_CODE, tenantCode);
        intent.putExtra(EXTRA_ORG_CODE, orgCode);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, password);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(EXTRA_CALLBACK_SERVICE_NAME, callbackServiceName);
        intent.putExtra(EXTRA_MAX_DISTANCE, maxDistance);
        if (newLocation != null) {
            intent.putExtra(EXTRA_LOCATION_UPDATE_FLAG, true);
            intent.putExtra(EXTRA_LATITUDE, newLocation.latitude);
            intent.putExtra(EXTRA_LONGITUDE, newLocation.longitude);
        }
        if (geofences != null) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (PersistentGeofence fence: geofences) {
                if (count > 0) {
                    sb.append('|');
                }
                sb.append(fence.getCode());
                count++;
            }
            intent.putExtra(EXTRA_GEOFENCES, sb.toString());
        }
        if (deletedGeofences != null) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String code: deletedGeofences) {
                if (count > 0) {
                    sb.append('|');
                }
                sb.append(code);
                count++;
            }
            intent.putExtra(EXTRA_DELETED_GEOFENCES, sb.toString());
        }
        if( eventType != null) {
            intent.putExtra(EXTRA_EVENT_TYPE, eventType.name());
        }
        return this;
    }

    /**
     * Used for debugging purposes only.
     */
    private void debugCheck() {
        /*
        if (callbackServiceName == null) {
            log.debug("toIntent() service is null, call stack:" + Log.getStackTraceString(new Exception()));
        }
        */
    }

    void populateFromSettings(Settings settings) {
        serverUrl = settings.getString(EXTRA_SERVER_URL, null);
        tenantCode = settings.getString(EXTRA_TENANT_CODE, null);
        orgCode = settings.getString(EXTRA_ORG_CODE, null);
        username = settings.getString(EXTRA_USERNAME, null);
        password = settings.getString(EXTRA_PASSWORD, null);
        callbackServiceName = settings.getString(EXTRA_CALLBACK_SERVICE_NAME, null);
    }
}
