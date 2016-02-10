package com.ibm.pisdk.geofencing.rest;

/**
 * A request that does not receive a JSON result result.
 */
public class PISimpleRequest extends PIRequest<Void> {
    /**
     * Initialize this request with the specified identifier and callback.
     * @param callback the callback instance to which the request results will be dispatched asynchronously.
     * @param method the HTTP request method to use.
     */
    public PISimpleRequest(PIRequestCallback<Void> callback, HttpMethod method, String payload) {
        super(callback, method, null);
    }

    @Override
    Void resultFromResponse(byte[] source) throws Exception {
        return null;
    }
}
