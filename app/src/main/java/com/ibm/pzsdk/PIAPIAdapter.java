//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
package com.ibm.pzsdk;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import org.apache.http.HttpStatus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class provides an interface with the Presence Insights APIs.
 *
 * All methods return the API result as a String, unless otherwise specified.  I'm looking at you
 * getFloorMap.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public class PIAPIAdapter implements Serializable {
    private static final String TAG = PIAPIAdapter.class.getSimpleName();

    static final String JSON_Zone_Id = "id";
    static final String JSON_Zone_Type = "type";
    static final String JSON_Zone_Name = "name";
    static final String JSON_Zone_Description = "description";
    static final String JSON_Zone_TimeZone = "timeZone";

    static final String JSON_Entity_Id 			= "id";
    static final String JSON_Entity_Type 		= "type";
    static final String JSON_Entity_Name 		= "name";
    static final String JSON_Entity_Description = "description";
    static final String JSON_Entity_Email 		= "email";
    static final String JSON_Entity_Loyalty 	= "loyalty";
    static final String JSON_Entity_Phone 		= "phone";
    static final String JSON_Entity_OptedIn 	= "optedIn";
    static final String JSON_Entity_Descriptors	= "descriptors";

    static final String JSON_EntityFullLocation_Zones = "zones";
    static final String JSON_EntityFullLocation_ZoneTags = "tags";
    static final String JSON_EntityFullLocation_ZonePersonalTag = "personalTag";
    static final String JSON_EntityFullLocation_X = "x";
    static final String JSON_EntityFullLocation_Y = "y";
    static final String JSON_EntityFullLocation_Z = "z";
    static final String JSON_EntityFullLocation_Timestamp = "timestamp";

    static final private int READ_TIMEOUT_IN_MILLISECONDS = 7000; /* milliseconds */
    static final private int CONNECTION_TIMEOUT_IN_MILLISECONDS = 7000; /* milliseconds */

    // mContext will be nice to have if we want to do any kind of UI actions for them.  For example,
    // we could provide them the option to throw up a progress indicator while the async tasks are running.
    private final transient Context mContext;
    private final String mServerURL;
    private final String mConnectorURL;
    private final String mTenantCode;
    private final String mOrgCode;

    private final String mBasicAuth;


    /**
     * Constructor
     *
     * @param context Activity context
     * @param username username for tenant
     * @param password password for tenant
     * @param hostname url
     * @param tenantCode unique identifier for the tenant
     * @param orgCode unique identifier for the organization
     */
    public PIAPIAdapter(Context context, String username, String password, String hostname, String tenantCode, String orgCode){
        mContext = context;
        mBasicAuth = generateBasicAuth(username, password);
        mServerURL = hostname + context.getString(R.string.management_server_path);
        mConnectorURL = hostname + context.getString(R.string.beacon_connector_path);
        mTenantCode = tenantCode;
        mOrgCode = orgCode;
    }

    @Override
    public String toString() {
        return "PZAPIAdapter{" +
                "serverURL=" + mServerURL +
                "connectorURL=" + mConnectorURL +
                '}';
    }

    /**
     * Retrieves the specified tenant document
     *
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getTenant(PIAPICompletionHandler completionHandler) {
        String tenant = String.format("%s/tenants/%s", mServerURL, mTenantCode);
        try {
            URL url = new URL(tenant);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all the orgs of a tenant.  The tenant supplied in the PIAPIAdapter constructor.
     *
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getOrgs(PIAPICompletionHandler completionHandler) {
        String orgs = String.format("%s/tenants/%s/orgs", mServerURL, mTenantCode);
        try {
            URL url = new URL(orgs);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves an org within a tenant.
     *
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getOrg(PIAPICompletionHandler completionHandler) {
        String org = String.format("%s/tenants/%s/orgs/%s", mServerURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(org);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all the sites of an organization.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getSites(PIAPICompletionHandler completionHandler) {
        String sites = String.format("%s/tenants/%s/orgs/%s/sites", mServerURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(sites);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a site of an organization.
     *
     * @param siteCode unique identifier for the site.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getSite(String siteCode, PIAPICompletionHandler completionHandler) {
        String site = String.format("%s/tenants/%s/orgs/%s/sites/%s", mServerURL, mTenantCode, mOrgCode, siteCode);
        try {
            URL url = new URL(site);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all the floors of a site.
     *
     * @param siteCode unique identifier for the site.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getFloors(String siteCode, PIAPICompletionHandler completionHandler) {
        String floors = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors", mServerURL, mTenantCode, mOrgCode, siteCode);
        try {
            URL url = new URL(floors);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a floor of a site.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getFloor(String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String floor = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode);
        try {
            URL url = new URL(floor);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all devices of an organization.
     *
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getDevices(PIAPICompletionHandler completionHandler) {
        String devices = String.format("%s/tenants/%s/orgs/%s/devices", mServerURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(devices);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a device within an organization.
     *
     * @param deviceCode unique identifier for the device.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getDevice(String deviceCode, PIAPICompletionHandler completionHandler) {
        String device = String.format("%s/tenants/%s/orgs/%s/devices/%s", mServerURL, mTenantCode, mOrgCode, deviceCode);
        try {
            URL url = new URL(device);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all the zones on a floor.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getZones(String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String zones = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/zones", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode);
        try {
            URL url = new URL(zones);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a zone on a floor.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param zoneCode unique identifier for the zone.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getZone(String siteCode, String floorCode, String zoneCode, PIAPICompletionHandler completionHandler) {
        String zone = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/zones/%s", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode, zoneCode);
        try {
            URL url = new URL(zone);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all the beacons on a floor
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getBeacons(String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String beacons = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/beacons", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode);
        try {
            URL url = new URL(beacons);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a beacon on a floor.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param beaconCode unique identifier for the beacon.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getBeacon(String siteCode, String floorCode, String beaconCode, PIAPICompletionHandler completionHandler) {
        String beacon = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/beacons/%s", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode, beaconCode);
        try {
            URL url = new URL(beacon);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all the sensors on a floor.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getSensors(String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String sensors = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/sensors", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode);
        try {
            URL url = new URL(sensors);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a sensor on a floor.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param sensorCode unique identifier for the sensor.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getSensor(String siteCode, String floorCode, String sensorCode, PIAPICompletionHandler completionHandler) {
        String sensor = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/sensors/%s", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode, sensorCode);
        try {
            URL url = new URL(sensor);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the map image of a floor.  Returned as a Bitmap.
     *
     * @param siteCode unique identifier for the site.
     * @param floorCode unique identifier for the floor.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getFloorMap(String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String map = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/map", mServerURL, mTenantCode, mOrgCode, siteCode, floorCode);
        try {
            URL url = new URL(map);
            GET_IMAGE(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a list of proximity UUIDs from an organization.  Used for monitoring and ranging beacons in PIBeaconSensor.
     *
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void getProximityUUIDs(PIAPICompletionHandler completionHandler) {
        String proximityUUIDs = String.format("%s/tenants/%s/orgs/%s/views/proximityUUID", mServerURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(proximityUUIDs);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers a device within an organization. If it already exists, this will still go through and update
     * the document.
     *
     * @param device object with all the necessary information to register the device.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void registerDevice(final DeviceInfo device, final PIAPICompletionHandler completionHandler) {
        final String registerDevice = String.format("%s/tenants/%s/orgs/%s/devices", mServerURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(registerDevice);
            POST(url, device.toJSON(), new PIAPICompletionHandler() {
                @Override
                public void onComplete(PIAPIResult postResult) {
                    Log.i(TAG, "");
                    if (postResult.getResponseCode() == HttpStatus.SC_CONFLICT) {
                        // call GET
                        try {
                            final URL deviceLocation = new URL(postResult.getHeader().get("Location").get(0));
                            GET(deviceLocation, new PIAPICompletionHandler() {
                                @Override
                                public void onComplete(PIAPIResult getResult) {
                                    if (getResult.getResponseCode() == HttpStatus.SC_OK) {
                                        // build put payload
                                        JSONObject payload = null;
                                        try {
                                            payload = JSONObject.parse((String) getResult.getResult());
                                            device.addToJson(payload);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        // call PUT
                                        PUT(deviceLocation, payload, completionHandler);

                                    } else {
                                        completionHandler.onComplete(getResult);
                                    }
                                }
                            });
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    } else {
                        completionHandler.onComplete(postResult);
                    }
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a device within an organization.
     *
     * @param device object with all the necessary information to update the device.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void updateDevice(final DeviceInfo device, final PIAPICompletionHandler completionHandler) {
        String getDeviceObj = String.format("%s/tenants/%s/orgs/%s/devices?descriptor=%s", mServerURL, mTenantCode, mOrgCode, device.getDescriptor());
        try {
            URL url = new URL(getDeviceObj);
            GET(url, new PIAPICompletionHandler() {
                @Override
                public void onComplete(PIAPIResult getResult) {
                    if (getResult.getResponseCode() == HttpStatus.SC_OK) {
                        // build put payload
                        JSONObject payload;
                        JSONArray devices;
                        JSONObject devicePayload;
                        URL putUrl = null;
                        String deviceCode = "";

                        payload = getResult.getResultAsJson();
                        devices = (JSONArray) payload.get("rows");

                        if (devices.size() > 0) {
                            devicePayload = (JSONObject) devices.get(0);
                            devicePayload.put(DeviceInfo.JSON_REGISTERED, device.isRegistered());
							devicePayload.put(DeviceInfo.JSON_NAME, device.getName());
							devicePayload.put(DeviceInfo.JSON_DATA, device.getData());

                            // call PUT
                            if (devicePayload.get("@code") != null) {
                                deviceCode = (String) devicePayload.get("@code");
                            }
                            String unregisterDevice = String.format("%s/tenants/%s/orgs/%s/devices/%s", mServerURL, mTenantCode, mOrgCode, deviceCode);
                            try {
                                putUrl = new URL(unregisterDevice);
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            PUT(putUrl, devicePayload, completionHandler);
                        } else {
                            getResult.setResponseCode(404);
                            getResult.setResult("Device \"" + device.getName() + "\" does not exist");
                            completionHandler.onComplete(getResult);
                        }
                    } else {
                        completionHandler.onComplete(getResult);
                    }
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unregisters a device within an organization.
     *
     * @param device object with all the necessary information to unregister the device.
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void unregisterDevice(final DeviceInfo device, final PIAPICompletionHandler completionHandler) {
        device.setRegistered(false);
        updateDevice(device, completionHandler);
    }

    /**
     * Sends a beacon notification message to the beacon connector to report the device's location.
     *
     * @param payload a combination of PIBeaconData and the device descriptor
     * @param completionHandler callback for APIs asynchronous calls.
     */
    public void sendBeaconNotificationMessage(JSONObject payload, PIAPICompletionHandler completionHandler) {
        String bnm = String.format("%s/tenants/%s/orgs/%s", mConnectorURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(bnm);
            POST(url, payload, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /*
        REST Helpers
     */
    private String generateBasicAuth(String user, String pass) {
        String toEncode = String.format("%s:%s", user, pass);
        return "Basic " + Base64.encodeToString(toEncode.getBytes(), 0, toEncode.length(), Base64.DEFAULT);
    }

    private PIAPIResult cannotReachServer(PIAPIResult result) {
        result.setResponseCode(0);
        result.setResult("Cannot reach the server.");
        return result;
    }

    private void GET(URL url, PIAPICompletionHandler completionHandler) {
        new AsyncTask<Object, Void, PIAPIResult>(){
            private Exception exceptionToBeThrown;
            private PIAPICompletionHandler completionHandler;
            private URL url;
            private int responseCode = 0;
            private HttpURLConnection connection = null;
            private PIAPIResult result = new PIAPIResult();
            @Override
            protected PIAPIResult doInBackground(Object... params) {
                url = (URL) params[0];
                completionHandler = (PIAPICompletionHandler) params[1];
                // attempt GET from url
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", mBasicAuth);
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.connect();
                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    result.setException(e);
                    e.printStackTrace();
                }

                // build result object
                if (responseCode != 0) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br;
                    String line;
                    try {
                        if (responseCode >= HttpStatus.SC_OK && responseCode < HttpStatus.SC_BAD_REQUEST) {
                            br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        } else {
                            br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        }
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                        }
                        br.close();
                    } catch (IOException e) {
                        result.setException(e);
                        e.printStackTrace();
                    }
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                } else {
                    cannotReachServer(result);
                }

                return result;
            }

            protected void onPostExecute(PIAPIResult result) {
                completionHandler.onComplete(result);
            }

        }.execute(url, completionHandler);
    }
    private void GET_IMAGE(URL url, PIAPICompletionHandler completionHandler) {
        new AsyncTask<Object, Void, PIAPIResult>(){
            Exception exceptionToBeThrown;
            PIAPICompletionHandler completionHandler;
            URL url;
            int responseCode = 0;
            HttpURLConnection connection = null;
            private PIAPIResult result = new PIAPIResult();

            @Override
            protected PIAPIResult doInBackground(Object... params) {
                url = (URL) params[0];
                completionHandler = (PIAPICompletionHandler) params[1];

                // attempt GET from url
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", mBasicAuth);
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.connect();
                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    result.setException(e);
                    e.printStackTrace();
                }

                // if all goes well, return payload
                if(responseCode == HttpStatus.SC_OK) {
                    try {
                        result.setHeader(connection.getHeaderFields());
                        result.setResult(BitmapFactory.decodeStream(connection.getInputStream()));
                        result.setResponseCode(responseCode);
                        return result;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (responseCode != 0) {
                    try {
                        StringBuilder sb = new StringBuilder();
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                                sb.append("\n");
                            }
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Exception exception = new Exception("Response code error: " + responseCode);
                        result.setException(exception);
                        result.setResult(sb.toString());
                        result.setResponseCode(responseCode);
                        throw exception;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    cannotReachServer(result);
                }
                return result;
            }

            protected void onPostExecute(PIAPIResult result) {
                completionHandler.onComplete(result);
            }

        }.execute(url, completionHandler);
    }
    private void POST(URL url, JSONObject payload, PIAPICompletionHandler completionHandler) {
        new AsyncTask<Object, Void, PIAPIResult>(){
            private Exception exceptionToBeThrown;
            private PIAPICompletionHandler completionHandler;
            private URL url;
            private JSONObject payload;
            private int responseCode = 0;
            private HttpURLConnection connection = null;
            private PIAPIResult result = new PIAPIResult();

            @Override
            protected PIAPIResult doInBackground(Object... params) {
                url = (URL) params[0];
                payload = (JSONObject) params[1];
                completionHandler = (PIAPICompletionHandler) params[2];

                // attempt POST to url
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", mBasicAuth);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.connect();

                    // send payload
                    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                    out.write(payload.toString());
                    out.close();

                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    result.setException(e);
                    e.printStackTrace();
                }

                // build result object
                if (responseCode != 0) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br;
                    String line;
                    try {
                        if (responseCode >= HttpStatus.SC_OK && responseCode < HttpStatus.SC_BAD_REQUEST) {
                            br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        } else {
                            br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        }
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                        }
                        br.close();
                    } catch (IOException e) {
                        result.setException(e);
                        e.printStackTrace();
                    }
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                } else {
                    cannotReachServer(result);
                }

                return result;
            }

            protected void onPostExecute(PIAPIResult result) {
                completionHandler.onComplete(result);
            }

        }.execute(url, payload, completionHandler);
    }
    private void PUT(URL url, JSONObject payload, PIAPICompletionHandler completionHandler) {
        new AsyncTask<Object, Void, PIAPIResult>(){
            private Exception exceptionToBeThrown;
            private PIAPICompletionHandler completionHandler;
            private URL url;
            private JSONObject payload;
            private int responseCode = 0;
            private HttpURLConnection connection = null;
            private PIAPIResult result = new PIAPIResult();

            @Override
            protected PIAPIResult doInBackground(Object... params) {
                url = (URL) params[0];
                payload = (JSONObject) params[1];
                completionHandler = (PIAPICompletionHandler) params[2];

                // attempt POST to url
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
                    connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Authorization", mBasicAuth);
                    connection.setRequestMethod("PUT");
                    connection.setDoOutput(true);
                    connection.connect();

                    // send payload
                    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                    out.write(payload.toString());
                    out.close();

                    responseCode = connection.getResponseCode();
                } catch (IOException e) {
                    result.setException(e);
                    e.printStackTrace();
                }

                // build result object
                if (responseCode != 0) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br;
                    String line;
                    try {
                        if (responseCode >= HttpStatus.SC_OK && responseCode < HttpStatus.SC_BAD_REQUEST) {
                            br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        } else {
                            br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        }
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                        }
                        br.close();
                    } catch (IOException e) {
                        result.setException(e);
                        e.printStackTrace();
                    }
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                } else {
                    cannotReachServer(result);
                }
                return result;
            }

            protected void onPostExecute(PIAPIResult result) {
                completionHandler.onComplete(result);
            }

        }.execute(url, payload, completionHandler);
    }
}
