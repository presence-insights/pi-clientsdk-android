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

package com.ibm.pisdk.geofencing.demo;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.ibm.pisdk.geofencing.PIGeofence;
import com.ibm.pisdk.geofencing.rest.HttpMethod;
import com.ibm.pisdk.geofencing.rest.PIHttpService;
import com.ibm.pisdk.geofencing.rest.PIJSONPayloadRequest;
import com.ibm.pisdk.geofencing.rest.PIRequestCallback;
import com.ibm.pisdk.geofencing.rest.PIRequestError;

import org.json.JSONObject;

import java.net.URL;
import java.util.List;

/**
 * A service that sends messages to a Slack channel for each geofence enter/exit event.
 */
public class SlackHTTPService extends PIHttpService {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = SlackHTTPService.class.getSimpleName();

    public SlackHTTPService(Context context) {
        super(context, "https://cloudplatform.slack.com", null, null, null, null);
    }

    @Override
    protected Uri getBaseQueryURI() throws Exception {
        URL url = new URL(getServerURL());
        int port = url.getPort();
        String portStr = (port < 0) ? "" : ":" + port;
        Uri.Builder builder = new Uri.Builder().scheme(url.getProtocol()).encodedAuthority(url.getHost() + portStr);
        return builder.build();
    }

    /**
     * Send a single slack message for the specified geofences.
     * @param geofences the geofences to send a message about
     * @param type the type of geofence event: 'enter' or 'exit'.
     * @param channel the slack channel to send the message to.
     */
    public void postGeofenceMessages(List<PIGeofence> geofences, String type, String channel) {
        Log.d(LOG_TAG, "postGeofenceMessages() type=" + type + ", nbFences=" + geofences.size() + ", channel=" + channel);
        PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                Log.d(LOG_TAG, "slack request successful");
            }

            @Override
            public void onError(PIRequestError error) {
                Log.e(LOG_TAG, "slack request error: " + error);
            }
        };
        PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, HttpMethod.POST, null);
        request.setPath("api/chat.postMessage");
        request.addParameter("token", "xoxb-16699261284-N2bQJgPCbgghzhPb0efFJhuw");
        request.addParameter("channel", channel);
        StringBuilder sb = new StringBuilder(":android: ").append(type).append(": ");
        int count = 0;
        for (PIGeofence fence : geofences) {
            if (count > 0) sb.append(", ");
            sb.append('\'').append(fence.getName()).append('\'');
            count++;
        }
        request.addParameter("text", sb.toString());
        executeRequest(request);
    }
}
