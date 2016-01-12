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

import android.util.Log;

/**
 * Encapsulates the error result of a failed query.
 */
public class PIRequestError {
    /**
     * The HTTP status code of the qsery.
     */
    private final int statusCode;
    /**
     * The exception for this error, if any.
     */
    private final Exception exception;
    /**
     * The error message, if any.
     */
    private final String message;

    /**
     * Initialize this error result with the specified status code, optional exception and error message.
     * @param statusCode the HTTP status code from the HTTP response.
     * @param exception an optional exception that was raised while executing the reqquest.
     * @param message the error message.
     */
    public PIRequestError(int statusCode, Exception exception, String message) {
        this.statusCode = statusCode;
        this.exception = exception;
        this.message = message;
    }

    /**
     * Return the status code from the HTTP response.
     * A value of -1 or less indicates that the request did not go through and therefore no status code could be captured.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the exception that occurred while executing the request, if any.
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Return the error message.
     */
    public String getMessage() {
        return message;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("status code: ").append(statusCode);
        if (message != null) sb.append(", message: ").append(message);
        if (exception != null) sb.append(", exception:\n").append(Log.getStackTraceString(exception));
        return sb.toString();
    }
}
