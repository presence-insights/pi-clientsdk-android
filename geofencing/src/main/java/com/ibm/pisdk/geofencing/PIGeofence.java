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

package com.ibm.pisdk.geofencing;

import com.orm.SugarRecord;

/**
 * Instances of this class represent geofences.
 */
public class PIGeofence extends SugarRecord<PIGeofence> {
    private String uuid;
    private String name;
    private double latitude;
    private double longitude;
    private double radius;
    private String messageIn;
    private String messageOut;
    private String entryDate;
    private String lastEnterDateAndTime;
    private String lastExitDateAndTime;

    public PIGeofence() {
    }

    public PIGeofence(String uuid, String name, double latitude, double longitude, double radius, String messageIn, String messageOut, String entryDate) {
        this.uuid = uuid;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.messageIn = messageIn;
        this.messageOut = messageOut;
        this.entryDate = entryDate;
    }

    public String getUuid() {
        return uuid;
    }

    void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getMessageIn() {
        return messageIn;
    }

    public String getMessageOut() {
        return messageOut;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public String getLastEnterDateAndTime() {
        return lastEnterDateAndTime;
    }

    public void setLastEnterDateAndTime(String lastEnterDateAndTime) {
        this.lastEnterDateAndTime = lastEnterDateAndTime;
    }

    public String getLastExitDateAndTime() {
        return lastExitDateAndTime;
    }

    public void setLastExitDateAndTime(String lastExitDateAndTime) {
        this.lastExitDateAndTime = lastExitDateAndTime;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
