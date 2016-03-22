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

package com.ibm.pi.geofence.rest;

import android.os.AsyncTask;

import com.ibm.pi.geofence.LoggingConfiguration;

import org.apache.log4j.Logger;

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
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(RequestAsyncTask.class.getSimpleName());
    /**
     * Maximum number of connection attemps.
     */
    private static final int MAX_TRIES = 3;
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
    int statusCode = -1;
    int nbTries = 0;
    boolean reauthenticationRequired = false;

    RequestAsyncTask(PIHttpService service, PIRequest<T> request) {
        this.request = request;
        this.service = service;
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean done = false;
        CookieHandler tmpManager = CookieHandler.getDefault();
        try {
            CookieHandler.setDefault(service.getCookieManager());
            StringBuilder sb = new StringBuilder();
            String query = request.buildQuery(service);
            sb.append(query);
            URL url = new URL(sb.toString());
            while (!done && (nbTries < MAX_TRIES)) {
                nbTries++;
                log.debug("request attempt #" + nbTries);
                done = sendRequest(url);
            }
        } catch (ConnectException | SocketTimeoutException e) {
            log.debug("detected loss of connectivity with the server", e);
        } catch (Exception e) {
            error = new PIRequestError(statusCode, e, e.getMessage());
        } finally {
            CookieHandler.setDefault(tmpManager);
        }
        return null;
    }

    private boolean sendRequest(URL url) throws Exception {
        HttpURLConnection connection = service.handleConnection((HttpURLConnection) url.openConnection());
        connection.setRequestProperty(Utils.HTTP_HEADER_ACCEPT_LANGUAGE, Locale.getDefault().toString());
        service.setUserAgentHeader(connection);
        log.debug("HTTP method = " + request.getMethod() + ", request url = " + connection.getURL() + ", payload = " + request.getPayload());
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        if (request.isBasicAuthRequired()) service.setAuthHeader(connection);
        HttpMethod method = request.getMethod();
        connection.setRequestMethod(method.name());
        Utils.logRequestHeaders(connection);
        try {
            if ((method == HttpMethod.POST) || (method == HttpMethod.PUT)) {
                String payload = request.getPayload();
                log.debug(request.getMethod() + " method request url = " + connection.getURL() + ", payload = " + payload);
                if (payload != null && (payload.length() > 0)) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    Writer writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(payload);
                    writer.close();
                }
            }
        } catch (ConnectException | SocketTimeoutException e) {
            return false;
        }
        try {
            statusCode = connection.getResponseCode();
        } catch (ConnectException | SocketTimeoutException e) {
            if (nbTries >= MAX_TRIES) {
                log.error("code " + statusCode + ", exception: ", e);
            }
            return false;
        } catch (IOException e) {
            // workaround for issue described at http://stackoverflow.com/q/17121213
            statusCode = connection.getResponseCode();
            if (statusCode != 401) {
                log.error("code " + statusCode + ", exception: ", e);
            }
        } finally {
            Utils.logResponseHeaders(connection);
        }
        if (statusCode >= 400) {
            service.logErrorBody(connection);
            // if version is >= JellyBean and an authentication challenge is issued, then handle re-authentication and resend the request
            if ((statusCode == 401) && !reauthenticationRequired) {
                log.debug("re-authenticating due to auth challenge...");
                reauthenticationRequired = true;
                return false;
            } else {
                error = new PIRequestError(statusCode, null, "query failure");
            }
        }
        if (error == null) {
            byte[] body = Utils.readBytes(connection);
            if (Utils.isTextResponseBody(connection)) {
                String bodyStr = new String(body, Utils.UTF_8);
                log.debug("doInBackground() response body = " + bodyStr);
            }
            result = request.resultFromResponse(body);
        }
        log.debug(String.format("status code for " + request.getMethod() + " request = %d, url = %s", statusCode, url));
        return true;
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
