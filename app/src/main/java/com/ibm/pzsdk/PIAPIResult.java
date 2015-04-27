package com.ibm.pzsdk;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by hannigan on 4/24/15.
 */
public class PIAPIResult implements Serializable {

    /**
     * payload returned from API call
     */
    Object result;
    /**
     * HTTP header
     */
    Map<String, List<String>> header;
    /**
     * HTTP response code
     */
    int responseCode;
    /**
     * Exception raised during API call
     */
    Exception exception;

    /**
     * Default constructor
     */
    PIAPIResult() {}

    /**
     *
     * @param result
     * @param responseCode
     * @param e
     */
    PIAPIResult(Object result, int responseCode, Exception e){this.result = result; this.responseCode = responseCode; this.exception = e;}

    /**
     *
     * @return
     */
    public Object getResult() {
        return result;
    }

    /**
     *
     * @param result
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     *
     * @return
     */
    public Map<String, List<String>> getHeader() {
        return header;
    }

    /**
     *
     * @param header
     */
    public void setHeader(Map<String, List<String>> header) {
        this.header = header;
    }

    /**
     *
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     *
     * @param responseCode
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     *
     * @return
     */
    public Exception getException() {
        return exception;
    }

    /**
     *
     * @param exception
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }
}
