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

package com.ibm.pisdk.geofencing.rest;

import org.json.JSONObject;

/**
 * A request thay receives a JSON payload in the response.
 */
public class PIJSONPayloadRequest extends PIRequest<JSONObject> {
    /**
     * Initialize this request with the specified callback, "GET" request method and null body.
     * @param callback the callback instance to which the request results will be dispatched asynchronously.
     */
    public PIJSONPayloadRequest(PIRequestCallback<JSONObject> callback) {
        this(callback, HttpMethod.GET, null);
    }

    /**
     * Initialize this request with the specified callback.
     * @param callback the callback instance to which the request results will be dispatched asynchronously.
     * @param method the HTTP request method to use.
     * @param payload the payload to send along with the request (for PUT and POST requests).
     */
    public PIJSONPayloadRequest(PIRequestCallback<JSONObject> callback, HttpMethod method, String payload) {
        super(callback, method, payload);
    }

    @Override
    JSONObject resultFromResponse(byte[] source) throws Exception {
        String s = new String(source, Utils.UTF_8);
        return new JSONObject(s);
    }
}
