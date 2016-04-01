package com.ibm.pi.geofence.demo;

import android.content.Context;

import com.ibm.pi.geofence.LoggingConfiguration;
import com.ibm.pi.geofence.PIGeofence;
import com.ibm.pi.geofence.PIGeofencingManager;
import com.ibm.pi.geofence.rest.HttpMethod;
import com.ibm.pi.geofence.rest.PIHttpService;
import com.ibm.pi.geofence.rest.PIJSONPayloadRequest;
import com.ibm.pi.geofence.rest.PIRequest;
import com.ibm.pi.geofence.rest.PIRequestCallback;
import com.ibm.pi.geofence.rest.PIRequestError;
import com.ibm.pi.core.doctypes.PIOrg;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class CustomPIHttpService extends PIHttpService {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggingConfiguration.getLogger(CustomPIHttpService.class.getSimpleName());
    /**
     * Part of a request path pointing to the pi conifg connector.
     */
    static final String CONFIG_CONNECTOR_PATH = "pi-config/v2";
    private PIGeofencingManager manager;

    public CustomPIHttpService(PIGeofencingManager manager, Context context, String serverURL, String tenantCode, String orgCode, String username, String password) {
        super(context, serverURL, tenantCode, orgCode, username, password);
        this.manager = manager;
    }

    /**
     * Create a new org with the specified parameters.
     * @param name the name of the org.
     * @param description a description of the org.
     * @param publicKey an optional public key used to encrypt data sent to the org.
     * @param useAsnewOrg whether to replace the current org with the created one in this service.
     * @param orgCallback an optional callback to receive a notification on the creation request
     * and perform actions accordingly.
     */
    public void createOrg(final String name, String description, String publicKey, final boolean useAsnewOrg, final PIRequestCallback<PIOrg> orgCallback) {
        if (getTenantCode() == null) {
            log.warn(String.format("cannot create the org '%s' because the tenant code is undefined", name));
        } else {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    try {
                        com.ibm.json.java.JSONObject jsonObject = com.ibm.json.java.JSONObject.parse(result.toString());
                        PIOrg piOrg = new PIOrg(jsonObject);
                        log.debug(String.format("sucessfully created org '%s' with orgCode = %s", piOrg.getName(), piOrg.getCode()));
                        if (useAsnewOrg) setOrgCode(piOrg.getCode());
                        if (orgCallback != null) {
                            orgCallback.onSuccess(piOrg);
                        }
                    } catch(Exception e) {
                        String message = String.format("error parsing new org with name '%s'", name);
                        log.error(message, e);
                        if (orgCallback != null) {
                            orgCallback.onError(new PIRequestError(-1, e, message));
                        }
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error creating org " + name + " : " + error.toString());
                    if (orgCallback != null) {
                        orgCallback.onError(error);
                    }
                }
            };
            JSONObject json = toJSONPostOrg(name, description, publicKey);
            PIRequest<JSONObject> request = new PIJSONPayloadRequest(callback, HttpMethod.POST, json.toString());
            request.setPath(String.format(Locale.US, "%s/tenants/%s/orgs", CONFIG_CONNECTOR_PATH, getTenantCode()));
            request.setBasicAuthRequired(true);
            executeRequest(request);
        }
    }

    /**
     * Register the specified single geofence with the PI server.
     * @param fence the geofence to register.
     */
    public void addGeofence(final PIGeofence fence, final PIRequestCallback<PIGeofence> userCallback) {
        log.debug("addGeofence(" + fence + ")");
        if ((getTenantCode() != null) && (getOrgCode() != null)) {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    log.debug("sucessfully posted geofence " + fence);
                    PIGeofence updated = null;
                    try {
                        updated = parseGeofence(result);
                    } catch(Exception e) {
                        log.error("error parsing JSON response: ", e);
                    }
                    if (updated != null) {
                        manager.loadGeofences();
                        //updated.save();
                        //setInitialLocation();
                    }
                    if (userCallback != null) {
                        try {
                            userCallback.onSuccess(updated);
                        } catch(Exception e) {
                            userCallback.onError(new PIRequestError(-1, e, "error parsing response for registration of fence " + fence));
                        }
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error posting geofence " + fence + " : " + error.toString());
                    if (userCallback != null) {
                        userCallback.onError(error);
                    }
                }
            };
            JSONObject payload = toJSONGeofence(fence, false);
            PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, HttpMethod.POST, payload.toString());
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s/geofences", CONFIG_CONNECTOR_PATH, getTenantCode(), getOrgCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            executeRequest(request);
        }
    }

    /**
     * Register the specified single geofence with the PI server.
     * @param fence the geofence to register.
     */
    public void updateGeofence(final PIGeofence fence, final PIRequestCallback<PIGeofence> userCallback) {
        log.debug("addGeofence(" + fence + ")");
        if ((getTenantCode() != null) && (getOrgCode() != null)) {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    log.debug("sucessfully updated geofence " + fence);
                    PIGeofence updated = null;
                    try {
                        updated = parseGeofence(result);
                    } catch(Exception e) {
                        log.error("error parsing JSON response: ", e);
                    }
                    if (updated != null) {
                        manager.loadGeofences();
                        //updated.save();
                        //setInitialLocation();
                    }
                    if (userCallback != null) {
                        try {
                            userCallback.onSuccess(updated);
                        } catch(Exception e) {
                            userCallback.onError(new PIRequestError(-1, e, "error parsing response for registration of fence " + fence));
                        }
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error putting geofence " + fence + " : " + error.toString());
                    if (userCallback != null) {
                        userCallback.onError(error);
                    }
                }
            };
            JSONObject payload = toJSONGeofence(fence, true);
            PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, HttpMethod.PUT, payload.toString());
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s/geofences/%s",
                CONFIG_CONNECTOR_PATH, getTenantCode(), getOrgCode(), fence.getCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            executeRequest(request);
        }
    }

    /**
     * Unregister the specified single geofence from the PI server.
     * @param fence the geofence to unregister.
     */
    public void removeGeofence(final PIGeofence fence, final PIRequestCallback<PIGeofence> userCallback) {
        log.debug("removeGeofence(" + fence + ")");
        if ((getTenantCode() != null) && (getOrgCode() != null)) {
            PIRequestCallback<JSONObject> callback = new PIRequestCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject result) {
                    log.debug("sucessfully deleted geofence " + fence);
                    manager.loadGeofences();
                    //setInitialLocation();
                    if (userCallback != null) {
                        try {
                            userCallback.onSuccess(fence);
                        } catch(Exception e) {
                            userCallback.onError(new PIRequestError(-1, e, "error parsing response for deletion of fence " + fence));
                        }
                    }
                }

                @Override
                public void onError(PIRequestError error) {
                    log.error("error deleting geofence " + fence + " : " + error.toString());
                    if (userCallback != null) {
                        userCallback.onError(error);
                    }
                }
            };
            PIJSONPayloadRequest request = new PIJSONPayloadRequest(callback, HttpMethod.DELETE, null);
            String path = String.format(Locale.US, "%s/tenants/%s/orgs/%s/geofences/%s",
                CONFIG_CONNECTOR_PATH, getTenantCode(), getOrgCode(), fence.getCode());
            request.setPath(path);
            request.setBasicAuthRequired(true);
            executeRequest(request);
        }
    }

    JSONObject toJSONGeofence(PIGeofence fence, boolean isUpdate) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "Feature");
            JSONObject geometry = new JSONObject();
            json.put("geometry", geometry);
            geometry.put("type", "Point");
            JSONArray coord = new JSONArray();
            geometry.put("coordinates", coord);
            coord.put(fence.getLongitude());
            coord.put(fence.getLatitude());
            JSONObject properties = new JSONObject();
            json.put("properties", properties);
            properties.put("name", fence.getName());
            properties.put("description", fence.getDescription() == null ? "" : fence.getDescription());
            properties.put("radius", fence.getRadius());
            if (isUpdate) {
                JSONObject updated = new JSONObject();
                properties.put("@updated", updated);
                updated.put("by", getUsername());
                updated.put("timestamp", new Date().getTime());
            }
        } catch(JSONException e) {
            log.error("exception generating json for geofence " + fence, e);
        }
        return json;
    }

    /**
     * Parse a single geofence.
     * @param feature json object representing the fence.
     * @return a {@link PIGeofence} instance.
     * @throws Exception if a parsing error occurs.
     */
    static PIGeofence parseGeofence(JSONObject feature) throws Exception {
        JSONObject props = feature.getJSONObject("properties");
        String code = props.has("code") ? props.getString("code") : (props.has("@code") ? props.getString("@code") : null);
        JSONObject updatedJSON = props.getJSONObject("@updated");
        long updated = updatedJSON.getLong("timestamp");
        JSONObject createdJSON = props.getJSONObject("@created");
        long created = createdJSON.getLong("timestamp");
        String name = props.has("name") ? props.getString("name") : null;
        String description = props.has("description") ? props.getString("description") : null;
        double radius = props.has("radius") ? props.getDouble("radius") : -1d;
        JSONObject geometry = feature.getJSONObject("geometry");
        JSONArray coord = geometry.getJSONArray("coordinates");
        double lng = coord.getDouble(0);
        double lat = coord.getDouble(1);
        PIGeofence geofence = new PIGeofence(code, name, description, lat, lng, radius);
        return geofence;
    }

    static JSONObject toJSONPostOrg(String name, String description, String publicKey) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name).put("description", description).put("publicKey", publicKey);
            JSONArray regTypes = new JSONArray();
            json.put("registrationTypes", regTypes.put("Internal"));
        } catch(JSONException e) {
            log.error("exception generating json for org " + name, e);
        }
        return json;
    }
}
