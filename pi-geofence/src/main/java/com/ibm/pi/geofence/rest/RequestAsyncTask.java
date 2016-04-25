/**
 * Copyright (c) 2015, 2016 IBM Corporation. All rights reserved.
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
    private final PIRequest<T> mRequest;
    /**
     * The service which executes the request.
     */
    private final PIHttpService mService;
    /**
     * The result to return if the query succeeds.
     */
    private T mResult = null;
    //private AERequestResult<T> result = null;
    /**
     * The error to return if the query fails.
     */
    private PIRequestError mError = null;
    int mStatusCode = -1;
    int mNbTries = 0;
    boolean mReauthenticationRequired = false;

    RequestAsyncTask(PIHttpService service, PIRequest<T> request) {
        this.mRequest = request;
        this.mService = service;
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean done = false;
        CookieHandler tmpManager = CookieHandler.getDefault();
        try {
            CookieHandler.setDefault(mService.getCookieManager());
            StringBuilder sb = new StringBuilder();
            String query = mRequest.buildQuery(mService);
            sb.append(query);
            URL url = new URL(sb.toString());
            while (!done && (mNbTries < MAX_TRIES)) {
                mNbTries++;
                log.debug("request attempt #" + mNbTries);
                done = sendRequest(url);
            }
        } catch (ConnectException | SocketTimeoutException e) {
            log.debug("detected loss of connectivity with the server", e);
        } catch (Exception e) {
            mError = new PIRequestError(mStatusCode, e, e.getMessage());
        } finally {
            CookieHandler.setDefault(tmpManager);
        }
        return null;
    }

    private boolean sendRequest(URL url) throws Exception {
        HttpURLConnection connection = mService.handleConnection((HttpURLConnection) url.openConnection());
        connection.setRequestProperty(Utils.HTTP_HEADER_ACCEPT_LANGUAGE, Locale.getDefault().toString());
        mService.setUserAgentHeader(connection);
        log.debug("HTTP method = " + mRequest.getMethod() + ", request url = " + connection.getURL() + ", payload = " + mRequest.getPayload());
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        if (mRequest.isBasicAuthRequired()) mService.setAuthHeader(connection);
        HttpMethod method = mRequest.getMethod();
        connection.setRequestMethod(method.name());
        Utils.logRequestHeaders(connection);
        try {
            if ((method == HttpMethod.POST) || (method == HttpMethod.PUT)) {
                String payload = mRequest.getPayload();
                log.debug(mRequest.getMethod() + " method request url = " + connection.getURL() + ", payload = " + payload);
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
            mStatusCode = connection.getResponseCode();
        } catch (ConnectException | SocketTimeoutException e) {
            if (mNbTries >= MAX_TRIES) {
                log.error("code " + mStatusCode + ", exception: ", e);
            }
            return false;
        } catch (IOException e) {
            // workaround for issue described at http://stackoverflow.com/q/17121213
            mStatusCode = connection.getResponseCode();
            if (mStatusCode != 401) {
                log.error("code " + mStatusCode + ", exception: ", e);
            }
        } finally {
            Utils.logResponseHeaders(connection);
        }
        if (mStatusCode >= 400) {
            mService.logErrorBody(connection);
            // if version is >= JellyBean and an authentication challenge is issued, then handle re-authentication and resend the request
            if ((mStatusCode == 401) && !mReauthenticationRequired) {
                log.debug("re-authenticating due to auth challenge...");
                mReauthenticationRequired = true;
                return false;
            } else {
                mError = new PIRequestError(mStatusCode, null, "query failure");
            }
        }
        if (mError == null) {
            byte[] body = Utils.readBytes(connection);
            if (Utils.isTextResponseBody(connection)) {
                String bodyStr = new String(body, Utils.UTF_8);
                log.debug("doInBackground() response body = " + bodyStr);
            }
            mResult = mRequest.resultFromResponse(body);
        }
        log.debug(String.format("status code for " + mRequest.getMethod() + " request = %d, url = %s", mStatusCode, url));
        return true;
    }

    @Override
    protected void onPostExecute(Void obj) {
        PIRequestCallback<T> callback = mRequest.getCallback();
        if (mError != null) {
            callback.onError(mError);
        } else if (mResult != null) {
            callback.onSuccess(mResult);
        }
    }
}
