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

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Instances of this class represent geofences.
 */
public class PersistentGeofence extends SugarRecord implements Serializable {
    @Unique
    private String mCode = "";
    private String mName;
    private String mDescription;
    private double mLatitude;
    private double mLongitude;
    private double mRadius;
    private long mCreatedTimestamp;
    private long mUpdatedTimestamp;

    public PersistentGeofence() {
    }

    PersistentGeofence(String code, String name, String description, double latitude, double longitude, double radius) {
        this.mCode = code;
        this.mName = name;
        this.mDescription = description;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mRadius = radius;
    }

    String getCode() {
        return mCode;
    }

    void setCode(String code) {
        this.mCode = code;
    }

    String getName() {
        return mName;
    }

    void setName(String name) {
        this.mName = name;
    }

    String getDescription() {
        return mDescription;
    }

    void setDescription(String description) {
        this.mDescription = description;
    }

    double getLatitude() {
        return mLatitude;
    }

    void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    double getLongitude() {
        return mLongitude;
    }

    void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    double getRadius() {
        return mRadius;
    }

    void setRadius(double radius) {
        this.mRadius = radius;
    }

    long getCreatedTimestamp() {
        return mCreatedTimestamp;
    }

    void setCreatedTimestamp(long createdTimestamp) {
        this.mCreatedTimestamp = createdTimestamp;
    }

    long getUpdatedTimestamp() {
        return mUpdatedTimestamp;
    }

    void setUpdatedTimestamp(long updatedTimestamp) {
        this.mUpdatedTimestamp = updatedTimestamp;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + mName + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistentGeofence that = (PersistentGeofence) o;
        return mCode.equals(that.mCode);
    }

    @Override
    public int hashCode() {
        return mCode.hashCode();
    }

    PIGeofence toPIGeofence() {
        return new PIGeofence(mCode, mName, mDescription, mLatitude, mLongitude, mRadius);
    }

    static PersistentGeofence fromPIGeofence(PIGeofence piGeofence) {
        return new PersistentGeofence(piGeofence.getCode(), piGeofence.getName(), piGeofence.getDescription(),
            piGeofence.getLatitude(), piGeofence.getLongitude(), piGeofence.getRadius());
    }

    static List<PIGeofence> toPIGeofences(List<PersistentGeofence> list) {
        List<PIGeofence> result = new ArrayList<>(list.size());
        for (PersistentGeofence pg: list) {
            result.add(pg.toPIGeofence());
        }
        return result;
    }
}
