/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
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

package com.ibm.pisdk.geofencing.test;

import android.util.Log;

import com.ibm.pi.geofence.rest.PIRequestCallback;
import com.ibm.pi.geofence.rest.PIRequestError;


public class TestRequestCallback<T> implements PIRequestCallback<T> {
  /**
   * Log tag for this class.
   */
  private static final String LOG_TAG = TestRequestCallback.class.getSimpleName();
  private boolean resultReceived = false;
  public T result;
  public PIRequestError error;

  @Override
  public void onError(PIRequestError error) {
    this.error = error;
    notifyResultReceived();
  }

  @Override
  public void onSuccess(T result) {
    this.result = result;
    notifyResultReceived();
  }

  private void notifyResultReceived() {
    synchronized(this) {
      resultReceived = true;
      notifyAll();
    }
  }

  public void awaitResult() {
    synchronized(this) {
      while (!resultReceived) {
        try {
          wait(10L);
        } catch(InterruptedException e) {
          Log.e(LOG_TAG, "error while awaiting results", e);
        }
      }
    }
  }
}
