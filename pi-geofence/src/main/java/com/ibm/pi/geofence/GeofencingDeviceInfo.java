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

import com.ibm.pisdk.PIDeviceInfo;

/**
 *
 */
public class GeofencingDeviceInfo extends PIDeviceInfo {
    GeofencingDeviceInfo(Context context) {
        super(context);
    }

    @Override
    protected String getDescriptor() {
        return super.getDescriptor();
    }
}
