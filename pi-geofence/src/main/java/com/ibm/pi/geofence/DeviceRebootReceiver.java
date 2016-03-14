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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.apache.log4j.Logger;

/**
 * This receiver is notified upon device reboot and handles the re-registration of the
 * monitored geofences, since a device reboot removes all geofences from the device.
 */
public class DeviceRebootReceiver extends BroadcastReceiver {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(DeviceRebootReceiver.class);

    public DeviceRebootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            log.debug("onReceive() processing device reboot event, context=" + context + ", package=" + context.getPackageName());
            ServiceConfig config = new ServiceConfig();
            config.packageName = context.getPackageName();
            Intent newIntent = new Intent(context, SignificantLocationChangeService.class);
            newIntent.putExtra(ServiceConfig.EXTRA_REBOOT_EVENT_FLAG, true);
            config.toIntent(newIntent);
            context.startService(newIntent);
            //config.toIntent(
        } catch(Exception e) {
            log.error("error in onReceive()", e);
        }
    }
}
