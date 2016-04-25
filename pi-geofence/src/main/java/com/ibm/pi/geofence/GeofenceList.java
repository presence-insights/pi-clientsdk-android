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

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a list of geofences a returned by the PI server upon a get or sync request.
 */
class GeofenceList {
    private final List<PersistentGeofence> mGeofences;
    int mTotalGeofences;
    long mLastSyncTimestamp;
    List<String> mDeletedGeofenceCodes;

    GeofenceList(final List<PersistentGeofence> geofences) {
        this.mGeofences = (geofences == null) ? Collections.<PersistentGeofence>emptyList() : geofences;
        this.mDeletedGeofenceCodes = Collections.emptyList();
    }

    GeofenceList(final List<PersistentGeofence> geofences, int totalGeofences, long lastSyncTimestamp, List<String> deletedGeofenceCodes) {
        this.mGeofences = (geofences == null) ? Collections.<PersistentGeofence>emptyList() : geofences;
        this.mTotalGeofences = totalGeofences;
        this.mLastSyncTimestamp = lastSyncTimestamp;
        this.mDeletedGeofenceCodes = (deletedGeofenceCodes == null) ? Collections.<String>emptyList() : deletedGeofenceCodes;
    }

    /**
     * Get the list of geofences that were added or updated since the last sync and loaded from the PI server.
     * @return a list of {@link PIGeofence} objects, possibly empty.
     */
    List<PersistentGeofence> getGeofences() {
        return mGeofences;
    }

    int getTotalGeofences() {
        return mTotalGeofences;
    }

    long getLastSyncTimestamp() {
        return mLastSyncTimestamp;
    }

    /**
     * Get the list of codes for the geofences that were deleted since the last sync.
     * @return a list of geofence codes, possibly empty.
     */
    List<String> getDeletedGeofenceCodes() {
        return mDeletedGeofenceCodes;
    }
}
