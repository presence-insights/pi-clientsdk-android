package com.ibm.pzsdk;

/**
 *
 */
public interface PIAPICompletionHandler {

    /**
     *
     * @param result - result of asynchronous call from API.
     */
    public void onComplete(PIAPIResult result);
}
