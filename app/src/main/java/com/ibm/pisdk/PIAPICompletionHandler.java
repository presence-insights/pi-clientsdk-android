//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
package com.ibm.pisdk;

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
