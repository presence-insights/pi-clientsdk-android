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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This receiver will instantiate a beacon sensor on boot up of the device.
 * If the sensor was running before the user restarted the phone, it will pick up where it left off
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class onBootComplete extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PIBeaconSensor sensor = PIBeaconSensor.getInstance(context, null);
    }
}
