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

import java.io.Serializable;

/**
 * Instances of this class represent geofences.
 */
public class PIGeofence implements Serializable {
    private String mCode = "";
    private String mName;
    private String mDescription;
    private double mLatitude;
    private double mLongitude;
    private double mRadius;

    public PIGeofence() {
    }

    public PIGeofence(String code, String name, String description, double latitude, double longitude, double radius) {
        this.mCode = code;
        this.mName = name;
        this.mDescription = description;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mRadius = radius;
    }

    public String getCode() {
        return mCode;
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public double getRadius() {
        return mRadius;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + mName + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PIGeofence that = (PIGeofence) o;
        return mCode.equals(that.mCode);
    }

    @Override
    public int hashCode() {
        return mCode.hashCode();
    }
}
