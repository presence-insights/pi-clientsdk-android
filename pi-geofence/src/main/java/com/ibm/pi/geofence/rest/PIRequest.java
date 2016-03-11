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

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract super class common to all requests.
 */
public abstract class PIRequest<C> {
    /**
     * The callback associated with the request.
     */
    protected final PIRequestCallback<C> callback;
    /**
     * Additional arbitrary parameters that can be added to the query.
     */
    final List<QueryParameter> parameters = new ArrayList<>();
    /**
     * Path to the content item(s) retrieved by path.
     */
    String path = null;
    /**
     * HTTP request method to use.
     */
    HttpMethod method = HttpMethod.GET;
    /**
     * The payload to send along with the request (for PUT and POST requests).
     */
    String payload;
    /**
     * Whether basic authentication is required for this request.
     */
    boolean basicAuthRequired = true;

    /**
     * Initialize this request with the specified identifier and callback.
     * @param callback the callback instance to which the request results will be dispatched asynchronously.
     * @param method the HTTP request method to use.
     * @param payload the payload to send along with the request (for PUT and POST requests).
     */
    public PIRequest(PIRequestCallback<C> callback, HttpMethod method, String payload) {
        this.callback = callback;
        this.method = method;
        this.payload = payload;
    }

    /**
     * Get the <code>path</code> of the REST request.
     * @return the path string.
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the <code>path</code> of the REST request.
     * @param path the path string.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Build the query part of the HTTP request.
     * @param service the service performing the request.
     * @return the query part of the HTTP request as a string.
     * @throws Exception if any error occurs while building the query.
     */
    String buildQuery(PIHttpService service) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(service.getBaseQueryURI());
        if (!Utils.isNullOrBlank(path)) sb.append('/').append(path);
        addParams(sb);
        return sb.toString();
    }

    /**
     * Parse the raw response received from the server into an objet of the type handled by this request.
     * @param source the raw bytes of the server response.
     * @return an object of the type handled by this request.
     * @throws Exception if any error occurs while parse the JSON.
     */
    abstract C resultFromResponse(byte[] source) throws Exception;

    /**
     * Get the callback associated with this request.
     * @return an instance of an implementation of {@link PIRequestCallback}.
     */
    public PIRequestCallback<C> getCallback() {
        return callback;
    }

    /**
     * Get the HTTP request method to use.
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Get the payload to send along with the request (for PUT and POST requests).
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Encode the specified key and value so they can be used as a request parameter.
     * @param key the parmeter key.
     * @param value the parmeter value.
     * @return the encoded parmeter in form key=value.
     * @throws Exception if any error occurs.
     */
    String encodeParam(String key, Object value) throws Exception {
        StringBuilder sb = new StringBuilder(Utils.urlEncode(key));
        sb.append('=').append(value == null ? "" : Utils.urlEncode(value.toString()));
        return sb.toString();
    }

    /**
     * Add the specified arbitrary request parameter.
     * @param name the name of the parameter to add.
     * @param value the parameter value.
     */
    public void addParameter(String name, String value) {
        parameters.add(new QueryParameter(name, value));
    }

    /**
     * Add the parmaters ti the query url.
     * @param query the query to add the parameters to.
     * @return the full url with the query parmeters.
     * @throws Exception if any error occurs.
     */
    StringBuilder addParams(StringBuilder query) throws Exception {
        int count = 0;
        for (QueryParameter param : parameters) {
            query.append(count > 0 ? '&' : '?').append(encodeParam(param.name, param.value));
            count++;
        }
        return query;
    }

    /**
     * Determine whether basic authentication is required for this request.
     * @return {@code true} if basic auth is needed, {@code false} otherwise.
     */
    public boolean isBasicAuthRequired() {
        return basicAuthRequired;
    }

    /**
     * Specify whether basic authentication is required for this request.
     * @param basicAuthRequired  {@code true} if basic auth is needed, {@code false} otherwise.
     */
    public void setBasicAuthRequired(boolean basicAuthRequired) {
        this.basicAuthRequired = basicAuthRequired;
    }
}
