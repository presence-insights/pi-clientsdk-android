//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
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
     *
     * @return the payload as an Object
     */
    public Object getResult() {
        return result;
    }

    /**
     *
     * @return the payload casted to a String
     */
    public String getResultAsString() {
        return (String) result;
    }

    /**
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
    public JSONObject getResultAsJson() {
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
