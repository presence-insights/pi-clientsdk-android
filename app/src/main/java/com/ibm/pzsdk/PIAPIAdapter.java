package com.ibm.pzsdk;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import org.apache.http.HttpStatus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by natalies on 22/09/2014.
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

    static final int READ_TIMEOUT_IN_MILLISECONDS = 7000; /* milliseconds */
    static final int CONNECTION_TIMEOUT_IN_MILLISECONDS = 7000; /* milliseconds */

    private Context mContext;
    private String mServerURL;
    private String mConnectorURL;
    private String mTenantCode;
    private String mOrgCode;

    public PIAPIAdapter(Context context, String hostname, String tenantCode, String orgCode){
        mContext = context;
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

    /*
        Public Management GET methods
     */

    /**
     *
     * @param completionHandler - callback for APIs asynchronous calls. - callback for APIs asynchronous calls.
     */
    public void getTenants(PIAPICompletionHandler completionHandler) {
        String tenants = String.format("%s/tenants", mServerURL);
        try {
            URL url = new URL(tenants);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param completionHandler - callback for APIs asynchronous calls. - callback for APIs asynchronous calls.
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
     *
     * @param completionHandler - callback for APIs asynchronous calls. - callback for APIs asynchronous calls.
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
     *
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param deviceCode - unique identifier for the device.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param zoneCode - unique identifier for the zone.
     * @param completionHandler - callback for APIs asynchronous calls.
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

    public void getBeacons(PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }
    public void getBeacons(String siteCode, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }

    /**
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param completionHandler - callback for APIs asynchronous calls.
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
    public void getBeaconsAdjacentTo(String beaconCode, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }

    /**
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param beaconCode - unique identifier for the beacon.
     * @param completionHandler - callback for APIs asynchronous calls.
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
    public void getBeaconById(String proximityUUID, String major, String minor, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }

    /**
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param sensorCode - unique identifier for the sensor.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param siteCode - unique identifier for the site.
     * @param floorCode - unique identifier for the floor.
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param completionHandler - callback for APIs asynchronous calls.
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
     *
     * @param device - object with all the necessary information to register the device.
     * @param completionHandler - callback for APIs asynchronous calls.
     */
    public void registerDevice(final PIDeviceInfo device, final PIAPICompletionHandler completionHandler) {
        final String registerDevice = String.format("%s/tenants/%s/orgs/%s/devices", mServerURL, mTenantCode, mOrgCode);
        try {
            URL url = new URL(registerDevice);
            POST(url, device.toJSON(), new PIAPICompletionHandler() {
                @Override
                public void onComplete(PIAPIResult postResult) {

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
                                            payload = JSONObject.parse((String)getResult.getResult());
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
    // TODO Handle unregistered Device flow

    /**
     *
     * @param device - object with all the necessary information to unregister the device.
     * @param completionHandler - callback for APIs asynchronous calls.
     */
    public void unregisterDevice(final PIDeviceInfo device, final PIAPICompletionHandler completionHandler) {
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
                        JSONObject devicePayload = null;
                        try {
                            payload = JSONObject.parse((String) getResult.getResult());
                            devices = (JSONArray) payload.get("rows");
                            devicePayload = (JSONObject) devices.get(0);
                            devicePayload.put(DeviceInfo.JSON_REGISTERED, false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // call PUT
                        String unregisterDevice = String.format("%s/tenants/%s/orgs/%s/devices/%s", mServerURL, mTenantCode, mOrgCode, devicePayload.get("@code"));
                        URL putUrl = null;
                        try {
                            putUrl = new URL(unregisterDevice);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        PUT(putUrl, devicePayload, completionHandler);

                    } else {
                        completionHandler.onComplete(getResult);
                    }
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /*
        Beacon Connector Methods
     */

    /**
     *
     * @param payload - TODO
     * @param completionHandler - callback for APIs asynchronous calls.
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
                    StringBuilder sb = new StringBuilder();
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                        }
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                }
                else {
                    try {
                        Exception exception = new Exception("Response code error: " + responseCode);
                        result.setException(exception);
                        result.setResponseCode(responseCode);
                        throw exception;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                }
                else {
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

                // if all goes well, sometimes empty, return payload
                if(responseCode == HttpStatus.SC_OK ||
                        responseCode == HttpStatus.SC_NO_CONTENT ||
                        responseCode == HttpStatus.SC_CREATED) {
                    StringBuilder sb = new StringBuilder();
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                        }
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                }
                else if (responseCode == HttpStatus.SC_INTERNAL_SERVER_ERROR ||
                        responseCode == HttpStatus.SC_CONFLICT) {
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
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                } else {
                    try {
                        Exception exception = new Exception("Response code error: " + responseCode);
                        result.setException(exception);
                        result.setResponseCode(responseCode);
                        throw exception;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

                // if all goes well, return payload
                if(responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_NO_CONTENT) {
                    StringBuilder sb = new StringBuilder();
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                        }
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    result.setHeader(connection.getHeaderFields());
                    result.setResult(sb.toString());
                    result.setResponseCode(responseCode);
                    return result;
                }
                else {
                    try {
                        Exception exception = new Exception("Response code error: " + responseCode);
                        result.setException(exception);
                        result.setResponseCode(responseCode);
                        throw exception;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return result;
            }

            protected void onPostExecute(PIAPIResult result) {
                completionHandler.onComplete(result);
            }

        }.execute(url, payload, completionHandler);
    }
}



