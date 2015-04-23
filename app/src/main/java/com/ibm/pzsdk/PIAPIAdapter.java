package com.ibm.pzsdk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

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

    private URL serverURL;
    private URL connectorURL;

    public PIAPIAdapter(URL serverURL, URL connectorURL){
        this.serverURL = serverURL;
        this.connectorURL = connectorURL;
    }

    public URL getServerURL(){
        return serverURL;
    }
    public URL getConnectorURL(){
        return connectorURL;
    }

    @Override
    public String toString() {
        return "PZAPIAdapter{" +
                "serverURL=" + serverURL +
                "connectorURL=" + connectorURL +
                '}';
    }

    class Result{
        Object result;
        String responseCode;
        Exception exception;
        Result(Object result, String responseCode, Exception e){this.result = result; this.responseCode = responseCode; this.exception = e;}
    }

    /*
        Public Management GET methods
     */
    public void getTenants(PIAPICompletionHandler completionHandler) {
        String tenants = String.format("%s/tenants", this.serverURL);
        try {
            URL url = new URL(tenants);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getTenant(String tenantCode, PIAPICompletionHandler completionHandler) {
        String tenant = String.format("%s/tenants/%s", this.serverURL, tenantCode);
        try {
            URL url = new URL(tenant);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getOrgs(String tenantCode, PIAPICompletionHandler completionHandler) {
        String orgs = String.format("%s/tenants/%s/orgs", this.serverURL, tenantCode);
        try {
            URL url = new URL(orgs);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getOrg(String tenantCode, String orgCode, PIAPICompletionHandler completionHandler) {
        String org = String.format("%s/tenants/%s/orgs/%s", this.serverURL, tenantCode, orgCode);
        try {
            URL url = new URL(org);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getSites(String tenantCode, String orgCode, PIAPICompletionHandler completionHandler) {
        String sites = String.format("%s/tenants/%s/orgs/%s/sites", this.serverURL, tenantCode, orgCode);
        try {
            URL url = new URL(sites);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getSite(String tenantCode, String orgCode, String siteCode, PIAPICompletionHandler completionHandler) {
        String site = String.format("%s/tenants/%s/orgs/%s/sites/%s", this.serverURL, tenantCode, orgCode, siteCode);
        try {
            URL url = new URL(site);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getFloors(String tenantCode, String orgCode, String siteCode, PIAPICompletionHandler completionHandler) {
        String floors = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors", this.serverURL, tenantCode, orgCode, siteCode);
        try {
            URL url = new URL(floors);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getFloor(String tenantCode, String orgCode, String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String floor = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s", this.serverURL, tenantCode, orgCode, siteCode, floorCode);
        try {
            URL url = new URL(floor);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getDevices(String tenantCode, String orgCode, PIAPICompletionHandler completionHandler) {
        String devices = String.format("%s/tenants/%s/orgs/%s/devices", this.serverURL, tenantCode, orgCode);
        try {
            URL url = new URL(devices);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getDevice(String tenantCode, String orgCode, String deviceCode, PIAPICompletionHandler completionHandler) {
        String device = String.format("%s/tenants/%s/orgs/%s/devices/%s", this.serverURL, tenantCode, orgCode, deviceCode);
        try {
            URL url = new URL(device);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getZones(String tenantCode, String orgCode, String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String zones = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/zones", this.serverURL, tenantCode, orgCode, siteCode, floorCode);
        try {
            URL url = new URL(zones);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getZone(String tenantCode, String orgCode, String siteCode, String floorCode, String zoneCode, PIAPICompletionHandler completionHandler) {
        String zone = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/zones/%s", this.serverURL, tenantCode, orgCode, siteCode, floorCode, zoneCode);
        try {
            URL url = new URL(zone);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getBeacons(String tenant, String orgCode, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }
    public void getBeacons(String tenantCode, String orgCode, String siteCode, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }
    public void getBeacons(String tenantCode, String orgCode, String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String beacons = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/beacons", this.serverURL, tenantCode, orgCode, siteCode, floorCode);
        try {
            URL url = new URL(beacons);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getBeaconsAdjacentTo(String tenantCode, String orgCode, String beaconCode, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }
    public void getBeacon(String tenantCode, String orgCode, String siteCode, String floorCode, String beaconCode, PIAPICompletionHandler completionHandler) {
        String beacon = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/beacons/%s", this.serverURL, tenantCode, orgCode, siteCode, floorCode, beaconCode);
        try {
            URL url = new URL(beacon);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getBeaconById(String tenantCode, String orgCode, String proximityUUID, String major, String minor, PIAPICompletionHandler completionHandler) {
        // TODO for next iteration, if we want this functionality
    }

    public void getSensors(String tenantCode, String orgCode, String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String sensors = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/sensors", this.serverURL, tenantCode, orgCode, siteCode, floorCode);
        try {
            URL url = new URL(sensors);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    public void getSensor(String tenantCode, String orgCode, String siteCode, String floorCode, String sensorCode, PIAPICompletionHandler completionHandler) {
        String sensor = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/sensors/%s", this.serverURL, tenantCode, orgCode, siteCode, floorCode, sensorCode);
        try {
            URL url = new URL(sensor);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getFloorMap(String tenantCode, String orgCode, String siteCode, String floorCode, PIAPICompletionHandler completionHandler) {
        String map = String.format("%s/tenants/%s/orgs/%s/sites/%s/floors/%s/map", this.serverURL, tenantCode, orgCode, siteCode, floorCode);
        try {
            URL url = new URL(map);
            GET_IMAGE(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void getProximityUUIDs(String tenantCode, String orgCode, PIAPICompletionHandler completionHandler) {
        String proximityUUIDs = String.format("%s/tenants/%s/orgs/%s/views/proximityUUID", this.serverURL, tenantCode, orgCode);
        try {
            URL url = new URL(proximityUUIDs);
            GET(url, completionHandler);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /*
        Beacon Connector Methods
     */
    public void sendBeaconNotificationMessage(String tenantCode, String orgCode, JSONObject payload, PIAPICompletionHandler completionHandler) {
        String bnm = String.format("%s/tenants/%s/orgs/%s", this.connectorURL, tenantCode, orgCode);
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
        new AsyncTask<Object, Void, String>(){
            private Exception exceptionToBeThrown;
            private PIAPICompletionHandler completionHandler;
            private URL url;
            private int responseCode = 0;
            private HttpURLConnection connection = null;
            @Override
            protected String doInBackground(Object... params) {
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
                    return sb.toString();
                }
                else {
                    try {
                        throw new Exception("Response code error: "+ responseCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return "";
            }

            protected void onPostExecute(String result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(url, completionHandler);
    }
    private void GET_IMAGE(URL url, PIAPICompletionHandler completionHandler) {
        new AsyncTask<Object, Void, Bitmap>(){
            Exception exceptionToBeThrown;
            PIAPICompletionHandler completionHandler;
            URL url;
            int responseCode = 0;
            HttpURLConnection connection = null;
            @Override
            protected Bitmap doInBackground(Object... params) {
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
                    e.printStackTrace();
                }

                // if all goes well, return payload
                if(responseCode == HttpStatus.SC_OK) {
                    try {
                        return BitmapFactory.decodeStream(connection.getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        throw new Exception("Response code error: "+ responseCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            protected void onPostExecute(String result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(url, completionHandler);
    }
    private void POST(URL url, JSONObject payload, PIAPICompletionHandler completionHandler) {
        new AsyncTask<Object, Void, String>(){
            private Exception exceptionToBeThrown;
            private PIAPICompletionHandler completionHandler;
            private URL url;
            private JSONObject payload;
            private int responseCode = 0;
            private HttpURLConnection connection = null;
            @Override
            protected String doInBackground(Object... params) {
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
                    return sb.toString();
                }
                else {
                    try {
                        throw new Exception("Response code error: "+ responseCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return "";
            }

            protected void onPostExecute(String result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(url, payload, completionHandler);
    }
}



