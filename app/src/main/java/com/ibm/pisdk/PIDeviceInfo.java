/**
 * Copyright (c) 2015 IBM Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.ibm.pisdk;

import android.content.Context;

/**
 * This class provides the Presence Insights' implementation for device descriptor.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIDeviceInfo extends DeviceInfo {

    /**
     * Activity context
     */
    private Context mContext;

    /**
     *
     * @param context Activity Context
     */
    public PIDeviceInfo(Context context) {
        super();
        mContext = context;
        setDescriptor();
    }

    @Override
    protected void setDescriptor() {
        PIDeviceID device = new PIDeviceID(mContext);
        setDescriptor(device.getMacAddress());
    }

}
