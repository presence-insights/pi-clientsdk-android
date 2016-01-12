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

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PIGeofencingServiceTest {
  private static final String LOG_TAG = PIGeofencingServiceTest.class.getSimpleName();

  @Before
  public void setUp() throws Exception {
  }

  /**
   * Test that creating a service with an invalid api key results in an authentication failure.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testAuthenticationFailure() throws Exception {
    /*
    AEService service = new AEService(TestUtils.SERVER_URL, TestUtils.TENANT_ID, TestUtils.APP_ID, TestUtils.generateRandomString(32), TestUtils.CONTEXT);
    TestRequestCallback<AEUserContext> callback = new TestRequestCallback<>();
    service.requestUserContext(callback);
    callback.awaitResult();
    assertNull(callback.result);
    assertNotNull(callback.error);
    assertEquals(401, callback.error.getStatusCode());
    */
  }

  /**
   * Test that creating a service with a valid api key results in a successful user context request.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testGetUserContextSuccess() throws Exception {
    /*
    AEService service = TestUtils.createService(null);
    TestRequestCallback<AEUserContext> callback = new TestRequestCallback<>();
    service.requestUserContext(callback);
    callback.awaitResult();
    assertNull(callback.error);
    assertNotNull(callback.result);
    assertTrue(callback.result instanceof AEUserContext);
    */
  }

  /**
   * Test that context updates are performed properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testUpdateUserContextSuccess() throws Exception {
    /*
    AEService service = TestUtils.createService(null);
    // populate a context with mobile data and post an update
    AEUserContext userContext = new AEUserContext();
    service.populateMobileContext(userContext);
    TestRequestCallback<List<AERemoteVariable>> updateCallback = new TestRequestCallback<>();
    service.postUserContextUpdate(userContext, updateCallback);
    updateCallback.awaitResult();
    // request a context from the server and test it has the same mobile data
    TestRequestCallback<AEUserContext> callback = new TestRequestCallback<>();
    service.requestUserContext(callback);
    callback.awaitResult();
    assertNull(callback.error);
    assertNotNull(callback.result);
    assertTrue(callback.result instanceof AEUserContext);
    AEUserContext newContext = callback.result;
    assertEquals(userContext.getDeviceMake(), newContext.getDeviceMake());
    assertEquals(userContext.getDeviceModel(), newContext.getDeviceModel());
    assertEquals(userContext.getOperatingSystem(), newContext.getOperatingSystem());
    assertEquals(userContext.getOperatingSystemVersion(), newContext.getOperatingSystemVersion());
    assertEquals(userContext.getLastKnownPosition(), newContext.getLastKnownPosition());
    */
  }
}
