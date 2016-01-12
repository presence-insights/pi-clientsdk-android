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

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * .
 */
class SignInTask extends AsyncTask<Void, Void, Void> {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = SignInTask.class.getSimpleName();
    final String userName;
    final String password;
    final PIRequestCallback<Void> callback;
    final PIHttpService service;
    private PIRequestError error = null;

    SignInTask(PIHttpService service, final String userName, final String password, final PIRequestCallback<Void> callback) {
        this.service = service;
        this.userName = userName;
        this.password = password;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        int statusCode = -1;
        try {
            statusCode = authenticateSync();
            if ((statusCode >= 400) && (statusCode != 404)) {
                error = new PIRequestError(statusCode, null, Utils.localize("authFailure", statusCode));
                callback.onError(error);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "error authenticating: " + e.getClass().getName() + " " + Log.getStackTraceString(e));
            error = new PIRequestError(statusCode, e, Utils.localize("authFailure", statusCode));
        }
        return null;
    }

    /**
     * Perform basic auth synchronously.
     * This method should always be called from an <code>AsyncTask</code>.
     * @return the HTTP response code for the authentication request.
     * @throws Exception if any error occurs.
     */
    int authenticateSync() throws Exception {
        int statusCode = -1;
        CookieHandler tmpManager = CookieHandler.getDefault();
        try {
            URL url = new URL(service.getBasicAuthenticationURI().toString());
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
            CookieHandler.setDefault(manager);
            HttpURLConnection connection = service.handleConnection((HttpURLConnection) url.openConnection());
            Log.d(LOG_TAG, "authenticateSync() url = " + connection.getURL());
            connection.setInstanceFollowRedirects(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            service.setAuthHeader(connection);
            service.setUserAgentHeader(connection);
            try {
                statusCode = connection.getResponseCode();
            } catch (IOException e) {
                statusCode = connection.getResponseCode();
            }
            Utils.logResponseHeaders(connection);
        } finally {
            CookieHandler.setDefault(tmpManager);
        }
        return statusCode;
    }

    @Override
    protected void onPostExecute(Void obj) {
        if (error != null) {
            callback.onError(error);
        } else {
            callback.onSuccess(null);
        }
    }
}
