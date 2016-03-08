package com.ibm.pisdk.geofencing;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.ibm.pisdk.geofencing.rest.PIHttpService;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains configuration information on the geofencing service that can be transmitted accross processes via {@code Intent}s.
 */
class ServiceConfig implements Serializable {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(ServiceConfig.class);
    //static final String GEOFENCING_SERVICE_CONFIG = "geofencing_service_config";
    private static final String PREFIX = "com.ibm.pi.sdk.";
    static final String EXTRA_LOCATION_UPDATE = PREFIX + "extra.location_update";
    static final String EXTRA_SERVER_URL = PREFIX + "extra.server";
    static final String EXTRA_TENANT_CODE = PREFIX + "extra.tenant";
    static final String EXTRA_ORG_CODE = PREFIX + "extra.org";
    static final String EXTRA_USERNAME = PREFIX + "extra.username";
    static final String EXTRA_PASSWORD = PREFIX + "extra.password";
    static final String EXTRA_MAX_DISTANCE = PREFIX + "extra.max_distance";
    static final String EXTRA_PACKAGE_NAME = PREFIX + "extra.package";
    static final String EXTRA_CALLBACK_SERVICE_NAME = PREFIX + "extra.callback_service";
    static final String EXTRA_GEOFENCES = PREFIX + "extra.geofences";
    static final String EXTRA_EVENT_TYPE = PREFIX + "extra.event_type";

    enum EventType {
        ENTER,
        EXIT,
        MONITOR,
        UNMONITOR
    }

    String serverUrl;
    String tenantCode;
    String orgCode;
    String username;
    String password;
    double maxDistance;
    String packageName;
    String callbackServiceName;
    List<PIGeofence> geofences;
    EventType eventType;

    Context createContext(Service service) {
        Context context = null;
        try {
            context = service.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE);
            log.debug(String.format("successfully created context for %s : %s", packageName, context));
        } catch(Exception e) {
            log.error(String.format("error creating context for %s", packageName), e);
        }
        return context;
    }

    ServiceConfig fromGeofencingService(PIGeofencingService service) {
        PIHttpService httpService = service.httpService;
        serverUrl = httpService.getServerURL();
        tenantCode = httpService.getTenantCode();
        orgCode = httpService.getOrgCode();
        username = httpService.getUsername();
        password = httpService.getPassword();
        maxDistance = service.maxDistance;
        packageName = service.context.getPackageName();
        if (service.callbackServiceClass != null) {
            callbackServiceName = service.callbackServiceClass.getName();
        }
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
            .append(']').toString();
    }

    ServiceConfig fromIntent(Intent intent) {
        serverUrl = intent.getStringExtra(EXTRA_SERVER_URL);
        tenantCode = intent.getStringExtra(EXTRA_TENANT_CODE);
        orgCode = intent.getStringExtra(EXTRA_ORG_CODE);
        username = intent.getStringExtra(EXTRA_USERNAME);
        password = intent.getStringExtra(EXTRA_PASSWORD);
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        callbackServiceName = intent.getStringExtra(EXTRA_CALLBACK_SERVICE_NAME);
        maxDistance = intent.getDoubleExtra(EXTRA_MAX_DISTANCE, 10_000d);
        String s = intent.getStringExtra(EXTRA_GEOFENCES);
        if (s != null) {
            String[] codes = s.split("\\|");
            if ((codes != null) && (codes.length > 0)) {
                geofences = new ArrayList<>(codes.length);
                for (String code: codes) {
                    List<PIGeofence> list = PIGeofence.find(PIGeofence.class, "code = ?", code);
                    if (!list.isEmpty()) {
                        geofences.add(list.get(0));
                    }
                }
            }
        }
        s = intent.getStringExtra(EXTRA_EVENT_TYPE);
        if (s != null) {
            try {
                eventType = EventType.valueOf(s);
            } catch(Exception ignore) {
            }
        }
        return this;
    }

    ServiceConfig toIntent(Intent intent) {
        intent.putExtra(EXTRA_SERVER_URL, serverUrl);
        intent.putExtra(EXTRA_TENANT_CODE, tenantCode);
        intent.putExtra(EXTRA_ORG_CODE, orgCode);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, password);
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(EXTRA_CALLBACK_SERVICE_NAME, callbackServiceName);
        intent.putExtra(EXTRA_MAX_DISTANCE, maxDistance);
        if (geofences != null) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (PIGeofence fence: geofences) {
                if (count > 0) {
                    sb.append('|');
                }
                sb.append(fence.getCode());
                count++;
            }
            intent.putExtra(EXTRA_GEOFENCES, sb.toString());
        }
        if( eventType != null) {
            intent.putExtra(EXTRA_EVENT_TYPE, eventType.name());
        }
        return this;
    }
}