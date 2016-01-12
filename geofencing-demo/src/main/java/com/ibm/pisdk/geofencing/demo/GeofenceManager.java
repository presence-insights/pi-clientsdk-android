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

import com.ibm.pisdk.geofencing.PIGeofence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * .
 */
public class GeofenceManager {
    /**
     * Mapping of registered geofences to their uuids.
     */
    private final Map<String, PIGeofence> fenceMap = new LinkedHashMap<String, PIGeofence>();
    /**
     * Mapping of currently active geofences to their uuids.
     */
    private final Set<String> activeSet = new HashSet<String>();

    /**
     * Update a geofence with the latest update from the server.
     * @param fence the geofence to update.
     */
    public void updateGeofence(PIGeofence fence) {
        PIGeofence g = getGeofence(fence.getUuid());
        if (g == null) {
            g = fence;
            synchronized (fenceMap) {
                fenceMap.put(g.getUuid(), g);
            }
        }
        // update the last entry date if it was updated
        String time = fence.getLastEnterDateAndTime();
        if (time != null) {
            g.setLastEnterDateAndTime(time);
        }
        // update the last exit date if it was updated
        time = fence.getLastExitDateAndTime();
        if (time != null) {
            g.setLastExitDateAndTime(time);
        }
    }

    /**
     * Remove the spêcified geofence.
     * @param fence the geofence to remove.
     */
    public void removeGeofence(PIGeofence fence) {
        PIGeofence g = null;
        synchronized (fenceMap) {
            g = fenceMap.remove(fence.getUuid());
        }
        if (g != null) {
            activeSet.remove(g);
        }
    }

    /**
     * Get a geofence from the cache.
     * @param uuid the uuid of the fence to get.
     */
    public PIGeofence getGeofence(String uuid) {
        synchronized (fenceMap) {
            return fenceMap.get(uuid);
        }
    }

    public void addFences(Collection<PIGeofence> fences) {
        synchronized (fenceMap) {
            for (PIGeofence fence : fences) {
                fenceMap.put(fence.getUuid(), fence);
            }
        }
    }

    /**
     * Get all fences in the cache.
     */
    public List<PIGeofence> getFences() {
        synchronized (fenceMap) {
            return new ArrayList<PIGeofence>(fenceMap.values());
        }
    }

    /**
     * Add the specified fence to the active list.
     * @param uuid the uuid of the activated fence.
     * @return <code>true</code> if the fence was added, <code>false</code> if there is no fence with this uuid.
     */
    public boolean addActiveFence(String uuid) {
        synchronized (activeSet) {
            return activeSet.add(uuid);
        }
    }

    /**
     * Remove the specified fence from the active list.
     * @param uuid the uuid of the deactivated fence.
     * @return <code>true</code> if the fence was removed, <code>false</code> if the fence was not in the active list.
     */
    public boolean removeActiveFence(String uuid) {
        synchronized (activeSet) {
            return activeSet.remove(uuid);
        }
    }

    /**
     * Remove the specified fence from the active list.
     * @return <code>true</code> there is at least one active fence, <code>false</code> otherwise.
     */
    public boolean hasActiveFence() {
        synchronized (activeSet) {
            return !activeSet.isEmpty();
        }
    }
}
