package com.ibm.pzsdk;

/**
 * This interface provides a callback method for the PIAPIAdapter's asynchronous calls.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public interface PIAPICompletionHandler {

    /**
     * Provides the results of the API call.
     *
     * @param result result of asynchronous call from API.
     */
    void onComplete(PIAPIResult result);
}
