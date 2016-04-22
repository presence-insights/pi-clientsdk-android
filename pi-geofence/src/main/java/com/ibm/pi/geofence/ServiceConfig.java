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
    static final String LOCATION_UPDATE_FLAG =        PREFIX + "location_update";
    static final String SERVER_URL =                  PREFIX + "server";
    static final String TENANT_CODE =                 PREFIX + "tenant";
    static final String ORG_CODE =                    PREFIX + "org";
    static final String USERNAME =                    PREFIX + "username";
    static final String PASSWORD =                    PREFIX + "password";
    static final String MAX_DISTANCE =                PREFIX + "max_distance";
    static final String PACKAGE_NAME =                PREFIX + "package";
    static final String GEOFENCES =                   PREFIX + "geofences";
    static final String DELETED_GEOFENCES =           PREFIX + "deleted_geofences";
    static final String EVENT_TYPE =                  PREFIX + "event_type";
    static final String LATITUDE =                    PREFIX + "latitude";
    static final String LONGITUDE =                   PREFIX + "longitude";
    static final String REBOOT_EVENT_FLAG =           PREFIX + "reboot_event";
    static final String LAST_SYNC_DATE =              PREFIX + "last_sync_date";
    static final String SERVER_SYNC_LOCAL_TIMESTAMP = PREFIX + "server_sync_local_timestamp";
    static final String SERVER_SYNC_MIN_DELAY_HOURS = PREFIX + "server_sync_min_delay_hours";

    String mServerUrl;
    String mTenantCode;
    String mOrgCode;
    String mUsername;
    String mPassword;
    double mMaxDistance;
    String mPackageName;
    List<PersistentGeofence> mGeofences;
    PIGeofenceEvent.Type mEventType;
    LatLng mNewLocation;
    List<String> mDeletedGeofences;

    /**
     * Create an application context based on the app's package name.
     */
    Context createContext(Service service) {
        Context context = null;
        try {
            context = service.createPackageContext(mPackageName, Context.CONTEXT_INCLUDE_CODE);
            //log.debug(String.format("successfully created context for %s : %s", packageName, context));
        } catch(Exception e) {
            log.error(String.format("error creating context for %s", mPackageName), e);
        }
        return context;
    }

    /**
     * Set the values of the fields in this class from extras stored in the specified geofencing service.
     */
    ServiceConfig fromGeofencingManager(PIGeofencingManager service) {
        PIHttpService httpService = service.mHttpService;
        mServerUrl = httpService.getServerURL();
        mTenantCode = httpService.getTenantCode();
        mOrgCode = httpService.getOrgCode();
        mUsername = httpService.getUsername();
        mPassword = httpService.getPassword();
        mMaxDistance = service.mMaxDistance;
        mPackageName = service.mContext.getPackageName();
        debugCheck();
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[')
            .append("serverUrl=").append(mServerUrl)
            .append(", tenantCode=").append(mTenantCode)
            .append(", orgCode=").append(mOrgCode)
            .append(", username=").append(mUsername)
            .append(", passord=********") // never print the password!
            .append(", maxDistance=").append(mMaxDistance)
            .append(", packageName=").append(mPackageName)
            .append(", geofences=").append(mGeofences)
            .append(", eventType=").append(mEventType)
            .append(", newLocation=").append(mNewLocation)
            .append(']').toString();
    }

    /**
     * Set the values of the fields in this class from extras stored in the specified intent.
     */
    ServiceConfig fromIntent(Intent intent) {
        mServerUrl = intent.getStringExtra(SERVER_URL);
        mTenantCode = intent.getStringExtra(TENANT_CODE);
        mOrgCode = intent.getStringExtra(ORG_CODE);
        mUsername = intent.getStringExtra(USERNAME);
        mPassword = intent.getStringExtra(PASSWORD);
        mPackageName = intent.getStringExtra(PACKAGE_NAME);
        mMaxDistance = intent.getDoubleExtra(MAX_DISTANCE, 10_000d);
        if (intent.getBooleanExtra(LOCATION_UPDATE_FLAG, false)) {
            mNewLocation = new LatLng(intent.getDoubleExtra(LATITUDE, 0d), intent.getDoubleExtra(LONGITUDE, 0d));
        }
        String s = intent.getStringExtra(GEOFENCES);
        if (s != null) {
            String[] codes = s.split("\\|");
            if ((codes != null) && (codes.length > 0)) {
                mGeofences = GeofencingUtils.geofencesFromCodes(Arrays.asList(codes));
            }
        }
        s = intent.getStringExtra(DELETED_GEOFENCES);
        if (s != null) {
            String[] codes = s.split("\\|");
            if ((codes != null) && (codes.length > 0)) {
                mDeletedGeofences = Arrays.asList(codes);
            }
        }
        s = intent.getStringExtra(EVENT_TYPE);
        if (s != null) {
            try {
                mEventType = PIGeofenceEvent.Type.valueOf(s);
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
        intent.putExtra(SERVER_URL, mServerUrl);
        intent.putExtra(TENANT_CODE, mTenantCode);
        intent.putExtra(ORG_CODE, mOrgCode);
        intent.putExtra(USERNAME, mUsername);
        intent.putExtra(PASSWORD, mPassword);
        intent.putExtra(PACKAGE_NAME, mPackageName);
        intent.putExtra(MAX_DISTANCE, mMaxDistance);
        if (mNewLocation != null) {
            intent.putExtra(LOCATION_UPDATE_FLAG, true);
            intent.putExtra(LATITUDE, mNewLocation.latitude);
            intent.putExtra(LONGITUDE, mNewLocation.longitude);
        }
        if (mGeofences != null) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (PersistentGeofence fence: mGeofences) {
                if (count > 0) {
                    sb.append('|');
                }
                sb.append(fence.getCode());
                count++;
            }
            intent.putExtra(GEOFENCES, sb.toString());
        }
        if (mDeletedGeofences != null) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String code: mDeletedGeofences) {
                if (count > 0) {
                    sb.append('|');
                }
                sb.append(code);
                count++;
            }
            intent.putExtra(DELETED_GEOFENCES, sb.toString());
        }
        if( mEventType != null) {
            intent.putExtra(EVENT_TYPE, mEventType.name());
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
        mServerUrl = settings.getString(SERVER_URL, null);
        mTenantCode = settings.getString(TENANT_CODE, null);
        mOrgCode = settings.getString(ORG_CODE, null);
        mUsername = settings.getString(USERNAME, null);
        mPassword = settings.getString(PASSWORD, null);
    }
}
