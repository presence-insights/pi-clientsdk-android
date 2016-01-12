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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Locale;

/**
 * Executes a request asynchronously.
 * @param <T> the type of result returned by the request.
 */
class RequestAsyncTask<T> extends AsyncTask<Void, Void, Void> {
    /**
     * Log tag for this class.
     */
    private static final String LOG_TAG = RequestAsyncTask.class.getSimpleName();
    /**
     * The request to execute.
     */
    private final PIRequest<T> request;
    /**
     * The service which executes the request.
     */
    private final PIHttpService service;
    /**
     * The result to return if the query succeeds.
     */
    private T result = null;
    //private AERequestResult<T> result = null;
    /**
     * The error to return if the query fails.
     */
    private PIRequestError error = null;

    RequestAsyncTask(PIHttpService service, PIRequest<T> request) {
        this.request = request;
        this.service = service;
    }

    @Override
    protected Void doInBackground(Void... params) {
        int statusCode = -1;
        boolean reauthenticationRequired = false;
        boolean done = false;
        CookieHandler tmpManager = CookieHandler.getDefault();
        try {
            CookieHandler.setDefault(service.getCookieManager());
            StringBuilder sb = new StringBuilder();
            String query = request.buildQuery(service);
            sb.append(query);
            URL url = new URL(sb.toString());
            while (!done) {
                HttpURLConnection connection = service.handleConnection((HttpURLConnection) url.openConnection());
                connection.setRequestProperty(Utils.HTTP_HEADER_ACCEPT_LANGUAGE, Locale.getDefault().toString());
                service.setUserAgentHeader(connection);
                Log.d(LOG_TAG, "HTTP method = " + request.getMethod() + ", request url = " + connection.getURL() + ", payload = " + request.getPayload());
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(30_000);
                if (request.isBasicAuthRequired()) service.setAuthHeader(connection);
                String method = request.getMethod().trim().toUpperCase();
                connection.setRequestMethod(method);
                Utils.logRequestHeaders(connection);
                if ("POST".equals(method) || "PUT".equals(method)) {
                    String payload = request.getPayload();
                    Log.d(LOG_TAG, request.getMethod() + " method request url = " + connection.getURL() + ", payload = " + payload);
                    if (payload != null && (payload.length() > 0)) {
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "application/json");
                        Writer writer = new OutputStreamWriter(connection.getOutputStream());
                        writer.write(payload);
                        writer.close();
                    }
                }
                // workaround for issue described at http://stackoverflow.com/q/17121213
                try {
                    statusCode = connection.getResponseCode();
                } catch (ConnectException | SocketTimeoutException e) {
                    // handle loss of connectivity at higher level
                    throw e;
                } catch (IOException e) {
                    statusCode = connection.getResponseCode();
                    if (statusCode != 401) {
                        Log.e(LOG_TAG, "code " + statusCode + ", exception: ", e);
                    }
                } finally {
                    Utils.logResponseHeaders(connection);
                }
                if (statusCode != 200) {
                    service.logErrorBody(connection);
                    // if version is >= JellyBean and an authentication challenge is issued, then handle re-authentication and resend the request
                    if ((statusCode == 401) && !reauthenticationRequired) {
                        Log.d(LOG_TAG, "re-authenticating due to auth challenge...");
                        reauthenticationRequired = true;
                        continue;
                    } else {
                        error = new PIRequestError(statusCode, null, "query failure");
                    }
                }
                done = true;
                if (error == null) {
                    byte[] body = Utils.readBytes(connection);
                    if (Utils.isTextResponseBody(connection)) {
                        String bodyStr = new String(body, Utils.UTF_8);
                        Log.d(LOG_TAG, "doInBackground() response body = " + bodyStr);
                    }
                    result = request.resultFromResponse(body);
                }
                Log.d(LOG_TAG, String.format("status code for " + request.getMethod() + " request = %d, url = %s", statusCode, url));
                // signal that network and server are on
                service.connectivityHandler.onActiveNetwork();
            }
        } catch (ConnectException | SocketTimeoutException e) {
            Log.v(LOG_TAG, "detected loss of connectivity with the server", e);
            if (service.connectivityHandler.isEnabled()) {
                service.connectivityHandler.onInactiveNetwork();
                service.connectivityHandler.persistRequest(request);
            }
        } catch (Exception e) {
            error = new PIRequestError(statusCode, e, e.getMessage());
        } finally {
            CookieHandler.setDefault(tmpManager);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void obj) {
        PIRequestCallback<T> callback = request.getCallback();
        if (error != null) {
            callback.onError(error);
        } else if (result != null) {
            callback.onSuccess(result);
        }
    }
}
