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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import java.util.Random;

/**
 *
 */
public class TestUtils {
  /**
   * A global PRNG.
   */
  private static final Random RAND = new Random(System.nanoTime());
  public static final String SERVER_URL = "http://192.168.1.10:3000";
  public static final String TENANT_ID = "lolo";
  public static final String APP_ID = "c96491c12c8c8b703168a50ba07947d3";
  public static final String API_KEY = "1061b86f-ef75-44b6-b86f-f9b2fb7fe715";
  public static final Context CONTEXT =  InstrumentationRegistry.getContext();
  public static final Context TARGET_CONTEXT =  InstrumentationRegistry.getTargetContext();

  /**
   * Get the name of the method that called this one.
   * @return the name of the invoking method as a string.
  */
  public static String getCurrentClassAndMethod() {
    StackTraceElement[] elements = new Exception().getStackTrace();
    if (elements.length < 2) {
      return "could not find current class/method name";
    }
    String s = elements[1].getClassName();
    int idx = s.lastIndexOf('.');
    return new StringBuilder(idx >= 0 ? s.substring(idx + 1) : s).append('.').append(elements[1].getMethodName()).append("()").toString();
  }

  /**
   * Generate a random string of the specified length.
   * @param length the length of the string to generate.
   * @return A string with {@code length} random uppercase alphabetic characters.
   */
  public static String generateRandomString(int length) {
    StringBuilder randomString = new StringBuilder();
    for (int i=0; i<length; i++) randomString.append((char) ('A' + RAND.nextInt(26)));
    return randomString.toString();
  }

  /*
  public static AEService createService(PIGeofenceCallback callback) {
    return callback == null
      ? new AEService(SERVER_URL, TENANT_ID, APP_ID, API_KEY, TARGET_CONTEXT)
      : new AEService(SERVER_URL, TENANT_ID, APP_ID, API_KEY, TARGET_CONTEXT, callback);
  }
  */
}
