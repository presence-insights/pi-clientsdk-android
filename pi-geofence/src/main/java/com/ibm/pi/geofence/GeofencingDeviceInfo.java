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

import android.content.Context;

import com.ibm.pi.core.Constants;
import com.ibm.pi.core.PIDeviceInfo;

/**
 * This extension of {@link PIDeviceInfo} serves solely to make the descriptor accessible
 */
class GeofencingDeviceInfo extends PIDeviceInfo {
    // shared prefs or settings
    /**
     * The name of the shared preferences file where the descriptor is stored.
     */
    static final String PI_SHARED_PREF = Constants.PI_SHARED_PREFS;
    /**
     * The name of the descritpor key in the sghared preferences (also used in the settings).
     */
    static final String PI_DESCRIPTOR_KEY = Constants.PI_SHARED_PREFS_DESCRIPTOR_KEY;

    GeofencingDeviceInfo(Context context) {
        super(context);
    }

    @Override
    protected String getDescriptor() {
        return super.getDescriptor();
    }
}
