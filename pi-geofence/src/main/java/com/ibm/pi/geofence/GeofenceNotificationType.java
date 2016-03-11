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

/**
 * The possible types of geofence triggers
 */
enum GeofenceNotificationType {
    /**
     * Geofence entry.
     */
    IN("enter"),
    /**
     * Geofence exit.
     */
    OUT("exit");

    private final String op;

    GeofenceNotificationType(String op) {
        this.op = op;
    }

    public String operation() {
        return op;
    }

    public static GeofenceNotificationType fromString(String source) {
        if (source == null) {
            return null;
        }
        String s = source.trim().toLowerCase();
        return IN.operation().equals(s) ? IN : (OUT.operation().equals(s) ? OUT : null);
    }
}
