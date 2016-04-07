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

import android.content.Intent;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class PIGeofenceEvent {
    /**
     * The name of the action used as filter for geofence events delivered as intents.
     */
    public static final String ACTION_GEOFENCE_EVENT = "com.ibm.pi.geofence.action.GEOFENCE_EVENT";
    private static final String GEOFENCES_KEY = "com.ibm.pi.geofence.geofences";
    private static final String DELETED_GEOFENCES_KEY = "com.ibm.pi.geofence.deleted_geofences";
    private static final String EVENT_TYPE_KEY = "com.ibm.pi.geofence.event_type";

    /**
     * The possible types of geofence events.
     */
    public enum Type {
        /**
         * Event type indicating entry within one or more geofences.
         */
        ENTER("enter"),
        /**
         * Event type indicating exit from one or more geofences.
         */
        EXIT("exit"),
        /**
         * Event type indicating results from a server synchronization request.
         */
        SERVER_SYNC("server_sync");

        private String op;
        Type(String op) {
            this.op = op;
        }

        public String operation() {
            return op;
        }
    }
    private final Type eventType;
    private final List<PIGeofence> geofences;
    private final List<String> deletedGeofenceCodes;

    private PIGeofenceEvent(final Type eventType, List<PIGeofence> geofences, final List<String> deletedGeofenceCodes) {
        this.eventType = eventType;
        this.geofences = geofences;
        this.deletedGeofenceCodes = deletedGeofenceCodes;
    }

    /**
     * Get the list of the codes of the geofences that were deleted since the last synchronization.
     * @return a list of codes for geofences that were deleted, or {@code null} if no geofence was deleted or
     * {@code getEventType() != PIGeofenceEvent.Type.SERVER_SYNC}.
     */
    public List<String> getDeletedGeofenceCodes() {
        return deletedGeofenceCodes;
    }

    /**
     * Get the type of this event.
     * @return one of the elements in the {@link Type} enum.
     */
    public Type getEventType() {
        return eventType;
    }

    /**
     * Get the geofences this event relates to. The meaning of the result should can interpreted in two ways:
     * <ul>
     *     <li>if {@link #getEventType()} returns {@link Type#ENTER} or {@link Type#EXIT} then these are gefoences that were entered or exited</li>
     *     <li>if {@link #getEventType()} returns {@link Type#SERVER_SYNC}, then these are the gefoences that were created or updated
     *     since the last server synchronization</li>
     * </ul>
     * @return a list of {@link PIGeofence} objects, possibly {@code null}.
     */
    public List<PIGeofence> getGeofences() {
        return geofences;
    }

    /**
     * Extract intelligible geofence data from the specified intent.
     * @param intent the intent delivered to an optional user-defined broadcast receiver.
     * @return a {@code PIGeofenceEvent} object populated form the intent.
     */
    public static PIGeofenceEvent fromIntent(Intent intent) {
        String typeStr = intent.getStringExtra(EVENT_TYPE_KEY);
        Type eventType = null;
        try {
            eventType = Type.valueOf(typeStr);
        } catch(Exception ignore) {
        }
        List<PIGeofence> geofences = null;
        String[] codes = intent.getStringArrayExtra(GEOFENCES_KEY);
        if (codes != null) {
            List<PersistentGeofence> pgList = GeofencingUtils.geofencesFromCodes(codes);
            geofences = PersistentGeofence.toPIGeofences(pgList);
        }
        List<String> deletedCodes = null;
        codes = intent.getStringArrayExtra(DELETED_GEOFENCES_KEY);
        if (codes != null) {
            deletedCodes = Arrays.asList(codes);
        }
        return new PIGeofenceEvent(eventType, geofences, deletedCodes);
    }

    static void toIntent(Intent intent, final Type eventType, List<PersistentGeofence> geofences, final List<String> deletedGeofenceCodes) {
        intent.putExtra(EVENT_TYPE_KEY, eventType.name());
        if (geofences != null) {
            String[] geofenceCodes = new String[geofences.size()];
            for (int i=0; i<geofences.size(); i++) {
                geofenceCodes[i] = geofences.get(i).getCode();
            }
            intent.putExtra(GEOFENCES_KEY, geofenceCodes);
        }
        if (deletedGeofenceCodes != null) {
            intent.putExtra(DELETED_GEOFENCES_KEY, deletedGeofenceCodes.toArray(new String[deletedGeofenceCodes.size()]));
        }
    }
}
