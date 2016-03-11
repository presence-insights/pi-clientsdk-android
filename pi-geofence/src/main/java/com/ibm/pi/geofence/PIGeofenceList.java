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

import java.util.List;

/**
 * {"type":"FeatureCollection","features":[],"properties":{"pageNumber":1,"pageSize":10,"totalFeatures":0}}
 */
public class PIGeofenceList {
    private final List<PIGeofence> geofences;
    private int pageNumber;
    private int pageSize;
    private int totalGeofences;

    public PIGeofenceList(final List<PIGeofence> geofences) {
        this.geofences = geofences;
    }

    public PIGeofenceList(final List<PIGeofence> geofences, int pageNumber, int pageSize, int totalGeofences) {
        this.geofences = geofences;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalGeofences = totalGeofences;
    }

    public List<PIGeofence> getGeofences() {
        return geofences;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalGeofences() {
        return totalGeofences;
    }
}
