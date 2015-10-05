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

import android.graphics.Bitmap;

import com.ibm.json.java.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class provides a simple encapsulation of what is returned by the PIAPIAdapter, with a couple
 * helper methods for casting the results from the API calls.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIAPIResult implements Serializable {

    /**
     * payload returned from API call
     */
    private Object result;
    /**
     * HTTP header
     */
    private Map<String, List<String>> header;
    /**
     * HTTP response code
     */
    private int responseCode;
    /**
     * Exception raised during API call
     */
    private Exception exception;

    /**
     * Default constructor
     */
    PIAPIResult() {}

    /**
     * Constructor for simple result creation.
     *
     * @param result payload returned from API call
     * @param responseCode HTTP response code
     */
    PIAPIResult(Object result, int responseCode){this.result = result; this.responseCode = responseCode;}

    /**
     * Cast the return of this method with the expected {@link com.ibm.pisdk.doctypes doctype}.
     * If you are retrieving a list, it will be of type ArrayList of type doctype.
     *
     * @return the payload as an Object
     */
    public Object getResult() {
        return result;
    }

    /**
     * Use this method to get the error message associated with the response code
     *
     * @return the payload casted to a String
     */
    public String getResultAsString() {
        return (String) result;
    }

    /**
     * Use this method when calling {@link PIAPIAdapter#getFloorMap(String, String, PIAPICompletionHandler)  getFloorMap}
     *
     * @return the payload as Bitmap
     */
    public Bitmap getResultAsBitmap() {
        return (Bitmap) result;
    }

    /**
     *
     * @return the payload as a JSON Object
     */
    protected JSONObject getResultAsJson() {
        try {
            return JSONObject.parse((String)result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param result payload returned from API call
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     *
     * @return HTTP header
     */
    public Map<String, List<String>> getHeader() {
        return header;
    }

    /**
     *
     * @param header HTTP Header
     */
    public void setHeader(Map<String, List<String>> header) {
        this.header = header;
    }

    /**
     *
     * @return HTTP response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     *
     * @param responseCode HTTP response code
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     *
     * @return exception thrown during API call
     */
    public Exception getException() {
        return exception;
    }

    /**
     *
     * @param exception exception thrown during API call
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }
}
