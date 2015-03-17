package com.ibm.pzsdk;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import com.ibm.json.java.JSONArray;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by natalies on 22/09/2014.
 */
public class PZAPIAdapter implements Serializable{

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
    static final String REGISTER_ENTITY_RELATIVE_URL = "/RegistrationGatewayREST/registration/entity";
    static final String RETRIEVE_ENTITY_RELATIVE_URL = "/QueriesGatewayREST/Admin/entity";
    static final String RETRIEVE_LOCATION_RELATIVE_URL = "/QueriesGatewayREST/RT/location/full/entity";
    static final String RETRIEVE_MAP_RELATIVE_URL = "/QueriesGatewayREST/Admin/mapForRootZoneAndZValue";
    static final String RETRIEVE_PROXIMITY_UUIDS_RELATIVE_URL = "/QueriesGatewayREST/Admin/proximityUUIDs";
    static final String RETRIEVE_SENSORS_IDS_ADJACENT_TO_SENSOR_RELATIVE_URL = "/QueriesGatewayREST/Admin/sensorsIdsAdjacentToSensor";
    static final String SEND_NOTIFICATION_MESSAGE_RELATIVE_URL = "/UpdatesGatewayREST/beacon/json/notificationMessage";

    private URL serverURL;

    public PZAPIAdapter(URL serverURL){
        this.serverURL = serverURL;
    }

    public URL getServerURL(){
        return serverURL;
    }

    @Override
    public String toString() {
        return "PZAPIAdapter{" +
                "serverURL=" + serverURL +
                '}';
    }

    /****************************************************   Public functions ***********************************************************/
    public boolean isEntityRegisteredSync(String entityId) throws Exception {
        JSONObject retrievedEntity;
        try {
            retrievedEntity = retrieveEntitySync(entityId);
        } catch (Exception e){
            throw new Exception("Failed checking if entity " + entityId +" is registered: "+ e.getMessage());
        }
        boolean result;
        try {
            result = compareEntityIdToRetrievedEntity(entityId, retrievedEntity);
        } catch (Exception e) {
            throw new Exception("Failed checking if entity " + entityId + "is registered: " + e.getMessage());
        }
        return result;
    }

    public void isEntityRegisteredAsync(String entityId, PZAPICompletionHandler completionHandler){
        new AsyncTask<Object,Void,JSONObject>(){
            private Exception exceptionToBeThrown;
            private String entityId;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected JSONObject doInBackground(Object... obj) {
                entityId = (String)obj[0];
                completionHandler = (PZAPICompletionHandler)obj[1];
                JSONObject retrievedEntity = null;
                try {
                    retrievedEntity = _retrieveEntity(entityId);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return retrievedEntity;
            }

            protected void onPostExecute(JSONObject retrievedEntity) {
                boolean result;
                if (exceptionToBeThrown != null){
                    completionHandler.onComplete(null, exceptionToBeThrown);
                    return;
                }

                try {
                    result = compareEntityIdToRetrievedEntity(entityId, retrievedEntity);
                } catch (Exception e) {
                    exceptionToBeThrown = new Exception("Failed checking if entity " + entityId + "is registered: " + e.getMessage());
                    completionHandler.onComplete(null, exceptionToBeThrown);
                    return;
                }
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(entityId, completionHandler);

    }

    public void registerEntitySync(String entityId, String name, String type, String description, String email, String loyalty, String phone, boolean optedIn)throws Exception{
        JSONObject jo;
        try {
            jo = createEntity(entityId, name, type, description, email, loyalty, phone, optedIn);
        } catch (Exception e){
            throw new Exception("Failed registering entity " + entityId +" : "+ e.getMessage());
        }
        AsyncTask<JSONObject,Void,Exception> localTask = new AsyncTask<JSONObject,Void,Exception>(){

            @Override
            protected Exception doInBackground(JSONObject... jo) {
                try{
                    _registerEntity(jo[0]);
                } catch (Exception e){
                    return e;
                }
                return null;
            }
        };
        localTask.execute(jo);
        Exception e = localTask.get();
        if (e != null){
            throw e;
        }
        return;
    }
    public void registerEntityAsync(String entityId, String name, String type, String description, String email, String loyalty, String phone, boolean optedIn, PZAPICompletionHandler completionHandler) throws Exception {
        JSONObject jo;
        try {
            jo = createEntity(entityId, name, type, description, email, loyalty, phone, optedIn);
        } catch (Exception e){
            throw new Exception("Failed registering entity " + entityId +" : "+ e.getMessage());
        }

        new AsyncTask<Object,Void,Void>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Void doInBackground(Object... obj) {
                completionHandler = (PZAPICompletionHandler)obj[1];
                try{
                    _registerEntity((JSONObject)obj[0]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return null;
            }
            protected void onPostExecute(Void result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(jo, completionHandler);
    }

    public void unregisterEntitySync(String entityId)throws Exception{
        AsyncTask<String,Void,Exception> localTask = new AsyncTask<String,Void,Exception>(){

            @Override
            protected Exception doInBackground(String... entities) {
                try{
                    _unregisterEntity(entities[0]);
                } catch (Exception e){
                    return e;
                }
                return null;
            }
        };
        localTask.execute(entityId);
        Exception e = localTask.get();
        if (e != null){
            throw e;
        }

        return;
    }
    public void unregisterEntityAsync(String entityId, PZAPICompletionHandler completionHandler){
        new AsyncTask<Object,Void,Void>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Void doInBackground(Object... params) {
                completionHandler = (PZAPICompletionHandler) params[1];
                try{
                    _unregisterEntity((String)params[0]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return null;
            }
            protected void onPostExecute(Void result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(entityId, completionHandler);

    }
    public void retrieveEntityAsync(String entityId, PZAPICompletionHandler completionHandler){
        // Each entityId can have more than one descriptor. EntityId is always also descriptor.
        retrieveEntityByDescriptorAsync(entityId, completionHandler);
    }
    public JSONObject retrieveEntitySync(String entityId) throws Exception {
        // Each entityId can have more than one descriptor. EntityId is always also descriptor.
        return retrieveEntityByDescriptorSync(entityId);
    }
    // checks if the device is registered (as an entity descriptor) for the given entityId
    public boolean isDeviceRegisteredForEntitySync(String entityId) throws Exception {
        String descriptor;
        try{
            descriptor = getDeviceDescriptor();
        } catch (Exception e){
            throw new Exception("Failed checking if device is registered for entity " + entityId + ": " + e.getMessage());
        }
        JSONObject retrievedEntity;
        try {
            retrievedEntity = retrieveEntitySync(descriptor);
        } catch (Exception e){
            throw new Exception("Failed checking if device is registered for entity " + entityId + ": Failed to retrieve entityId: " + e.getMessage());
        }
        if (retrievedEntity != null){
            boolean result;
            try {
                result = compareEntityIdToRetrievedEntity(entityId, retrievedEntity);
            } catch (Exception e) {
                throw new Exception("Failed checking if device is registered for entity " + entityId + ": " + e.getMessage());
            }
            return result;
        } else {
            throw new Exception("Failed checking if device is registered for entity "+entityId);
        }
    }

    public void isDeviceRegisteredForEntityAsync(String entityId, PZAPICompletionHandler completionHandler) {
        new AsyncTask<Object,Void,JSONObject>(){
            private Exception exceptionToBeThrown;
            private String entityId;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected JSONObject doInBackground(Object... obj) {
                entityId = (String)obj[0];
                completionHandler = (PZAPICompletionHandler)obj[1];
                String descriptor;
                try{
                    descriptor = getDeviceDescriptor();
                } catch (Exception e){
                    exceptionToBeThrown =  new Exception("Failed checking if device is registered for entity " + entityId + ": " + e.getMessage());
                    return null;
                }
                JSONObject retrievedEntity = null;
                try{
                    retrievedEntity  = _retrieveEntity(descriptor);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }

                return retrievedEntity;
            }

            protected void onPostExecute(JSONObject retrievedEntity) {
                boolean result = false;
                if(exceptionToBeThrown != null){
                    completionHandler.onComplete(null, exceptionToBeThrown);
                    return;
                }
                try {
                    result = compareEntityIdToRetrievedEntity(entityId, retrievedEntity);
                } catch (Exception e) {
                    exceptionToBeThrown =  new Exception("Failed checking if device is registered for entity " + entityId + ": " + e.getMessage());
                }
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(entityId, completionHandler);
    }

    // register the device (as an entity descriptor) for the given entityId
    public void registerDeviceForEntitySync(String entityId) throws Exception {
        AsyncTask<String, Void, Exception> localTask = new AsyncTask<String, Void, Exception>() {

            @Override
            protected Exception doInBackground(String... params) {
                try {
                    _registerDeviceForEntity(params[0]);
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        };
        localTask.execute(entityId);
        Exception e = localTask.get();
        if ( e!= null){
            throw e;
        }
    }
    public void registerDeviceForEntityAsync(String entityId, PZAPICompletionHandler completionHandler){

        new AsyncTask<Object,Void,Void>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Void doInBackground(Object... params) {
                completionHandler = (PZAPICompletionHandler) params[1];
                try{
                    _registerDeviceForEntity((String)params[0]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(entityId, completionHandler);
    }

    // unregister the device (no matter what entity it is registered for)
    public void unregisterDeviceSync() throws Exception{
        AsyncTask<Void,Void,Exception> localTask = new AsyncTask<Void,Void,Exception>(){

            @Override
            protected Exception doInBackground(Void... params) {
                try{
                    _unregisterDevice();
                } catch (Exception e){
                    return e;
                }
                return null;
            }
        };

        localTask.execute();
        Exception e = localTask.get();
        if (e != null){
            throw e;
        }
    }
    // unregister the device (no matter what entity it is registered for)
    public void unregisterDeviceAsync(PZAPICompletionHandler completionHandler){

        new AsyncTask<Object,Void,Void>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Void doInBackground(Object... params) {
                completionHandler = (PZAPICompletionHandler) params[0];
                try{
                    _unregisterDevice();
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(completionHandler);
    }

    public JSONObject retrieveLocationSync(String entityId) throws Exception {
        AsyncTask<String,Void,Result> localTask = new AsyncTask<String,Void,Result>(){

            @Override
            protected Result doInBackground(String... entityIds) {
                JSONObject location;
                try{
                    location = _retrieveLocation(entityIds[0]);
                } catch (Exception e){
                    return new Result(null, e);
                }
                return new Result(location, null);
            }
        };
        localTask.execute(entityId);
        Result result = localTask.get();
        if (result.exception != null) {
            throw result.exception;
        }
        return (JSONObject)result.result;
    }
    public void retrieveLocationAsync(String entityId,PZAPICompletionHandler completionHandler){
        new AsyncTask<Object,Void,JSONObject>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected JSONObject doInBackground(Object... obj) {
                JSONObject location = null;
                completionHandler = (PZAPICompletionHandler)obj[1];
                try{
                    location = _retrieveLocation((String) obj[0]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return location;
            }

            protected void onPostExecute(JSONObject result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(entityId, completionHandler);
    }

    public Bitmap retrieveMapSync(int rootZoneId, int floor) throws Exception {
        AsyncTask<Integer,Void,Result> localTask = new AsyncTask<Integer,Void,Result>(){

            @Override
            protected Result doInBackground(Integer... params) {
                Bitmap bmp;
                try {
                    bmp = _retrieveMap(params[0], params[1]);
                } catch (Exception e){
                    return new Result(null, e);
                }
                return new Result(bmp, null);
            }
        };
        localTask.execute(rootZoneId, floor);
        Result result = localTask.get();
        if (result.exception != null) {
            throw result.exception;
        }
        return (Bitmap)result.result;
    }

    public void retrieveMapAsync(int rootZoneId, int floor, PZAPICompletionHandler completionHandler){

        new AsyncTask<Object,Void,Bitmap>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Bitmap doInBackground(Object... params) {
                Bitmap bmp = null;
                completionHandler = (PZAPICompletionHandler) params[2];
                try{
                    bmp = _retrieveMap((Integer) params[0], (Integer) params[1]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return bmp;
            }

            protected void onPostExecute(Bitmap result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(rootZoneId, floor, completionHandler);
    }
    public Bitmap retrieveMapSync(JSONObject location) throws Exception{
        AsyncTask<JSONObject,Void,Result> localTask = new AsyncTask<JSONObject,Void,Result>(){

            @Override
            protected Result doInBackground(JSONObject... locations) {
                Bitmap bmp;
                try{
                    bmp = _retrieveMap(locations[0]);
                } catch (Exception e){
                    return new Result(null,e);
                }
                return new Result(bmp,null);
            }
        };

        localTask.execute(location);
        Result result = localTask.get();
        if (result.exception != null) {
            throw result.exception;
        }
        return (Bitmap)result.result;
    }
    public void retrieveMapAsync(JSONObject location, PZAPICompletionHandler completionHandler){
        new AsyncTask<Object,Void,Bitmap>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Bitmap doInBackground(Object... obj) {
                Bitmap bmp = null;
                completionHandler = (PZAPICompletionHandler) obj[1];
                try {
                    bmp = _retrieveMap((JSONObject) obj[0]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return bmp;
            }

            protected void onPostExecute(Bitmap result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(location, completionHandler);
    }
    /***************************************   Private functions  - Must be called from AsyncTask **************************************/
    private boolean _isEntityRegistered(String entityId) throws Exception {
        JSONObject retrievedEntity;
        try {
            retrievedEntity = _retrieveEntity(entityId);
        } catch (Exception e){
            throw new Exception("Failed checking if entity " + entityId +" is registered: "+ e.getMessage());
        }
        boolean result;
        try {
            result = compareEntityIdToRetrievedEntity(entityId, retrievedEntity);
        } catch (Exception e) {
            throw new Exception("Failed checking if entity " + entityId + "is registered: " + e.getMessage());
        }
        return result;
    }

    private void _registerEntity(JSONObject toSendJSON) throws Exception {
        // Cannot register an already registered entity.
        String entityId = toSendJSON.getString(JSON_Entity_Id);
        boolean isEntityRegistered;
        try{
            isEntityRegistered = _isEntityRegistered(entityId);
        } catch (Exception e){
            throw new Exception("Failed registering entity " + entityId + ": Failed checking if entity is registered: " + e.getMessage());
        }
        if (isEntityRegistered) {
            throw new Exception("Failed registering entity " + entityId + ": Entity is already registered");
        }
        URL url;
        try {
            url = new URL(serverURL + REGISTER_ENTITY_RELATIVE_URL);
        } catch (Exception e) {
            throw new Exception("Failed registering entity " + entityId + " : Failed creating URL");
        }
        try{
            sendPut(url,toSendJSON);
        } catch (Exception e) {
            throw new Exception("Failed registering entity " + entityId + ": " + e.getMessage());
        }
    }

    private void _unregisterEntity(String entityId) throws Exception {
        // Cannot unregister an non-registered entity.
        boolean isEntityRegistered;
        try {
            isEntityRegistered = _isEntityRegistered(entityId);
        } catch (Exception e) {
            throw new Exception("Failed unregistering entity: " + entityId + ": Failed checking if entity is registered: "+ e.getMessage());
        }
        if (!isEntityRegistered){
            throw new Exception("Failed unregistering entity: " + entityId + ": Entity is not registered");
        }
        URL url;
        try {
            url = new URL(serverURL + REGISTER_ENTITY_RELATIVE_URL + "/" + entityId);
        } catch (Exception e) {
            throw new Exception("Failed unregistering entity: " + entityId + ": Failed creating URL");
        }
        try{
            sendDelete(url);
        } catch (Exception e) {
            throw new Exception("Failed unregistering entity: " + entityId + ": " + e.getMessage());
        }
    }

    private JSONObject _retrieveEntity(String entityId) throws Exception{
        // Each entityId can have more than one descriptor. EntityId is always also descriptor.
        return _retrieveEntityByDescriptor(entityId);
    }
    private boolean _isDeviceRegisteredForEntity(String entityId) throws Exception {
        String descriptor;
        try{
            descriptor = getDeviceDescriptor();
        } catch (Exception e){
            throw new Exception("Failed checking if device is registered for entity " + entityId + ": " + e.getMessage());
        }
        JSONObject retrievedEntity;
        try {
            retrievedEntity  = _retrieveEntity(descriptor);
        } catch (Exception e){
            throw new Exception("Failed checking if device is registered for entity " + entityId + ": Failed retrieving entity by descriptor: " + e.getMessage());
        }
        if (retrievedEntity != null){
            boolean equal;
            try {
                equal = compareEntityIdToRetrievedEntity(entityId, retrievedEntity);
            } catch (Exception e) {
                throw new Exception("Failed checking if device is registered for entity " + entityId + ": " + e.getMessage());
            }
            return equal;
        } else {
            throw new Exception("Failed checking if device is registered for entity "+entityId);
        }
    }
    private boolean compareEntityIdToRetrievedEntity(String entityId, JSONObject retrievedEntity ) throws Exception{
        String retrievedEntityId;

        retrievedEntityId = getEntityId(retrievedEntity);
        if ("".equals(retrievedEntityId)){
            return false;
        }
        if (retrievedEntityId.equals(entityId))
            return true;
        throw new Exception("Unknown reason - entityId - "+ entityId + " and retrievedEntity = "+ retrievedEntity.toString());
    }

    private void _registerDeviceForEntity(String entityId) throws Exception {
        // Cannot register the device for the entity if the entity is not registered.
        boolean isEntityRegistered;
        try{
            isEntityRegistered = _isEntityRegistered(entityId);
        } catch (Exception e){
            throw new Exception("Failed registering device for entity " + entityId + ": Failed checking if entity is registered: " + e.getMessage());
        }
        if (!isEntityRegistered) {
                throw new Exception("Failed registering device for entity " + entityId + ": Entity is not registered");
        }
        boolean isDeviceRegisteredForEntity;
        try{
            isDeviceRegisteredForEntity = _isDeviceRegisteredForEntity(entityId);
        } catch (Exception e){
            throw new Exception("Failed registering device for entity " + entityId + ": Failed checking if the device is registered for entity: " + e.getMessage());
        }
        // If the device is already registered for the entity, do nothing.
        if (isDeviceRegisteredForEntity){
            return;
        }

        // Make sure the device is unregistered.
        try {
            _unregisterDevice();
        } catch (Exception e){
            throw new Exception("Failed registering device for entity " + entityId + ": Failed unregistering the device" + e.getMessage());
        }

        // Register the device for the entity by registering the device identifier as a descriptor for the entity.
        String descriptor;
        try{
            descriptor = getDeviceDescriptor();
        } catch (Exception e){
            throw new Exception("Failed registering device for entity: " + e.getMessage());
        }

        URL url;
        try {
            url = new URL(serverURL + REGISTER_ENTITY_RELATIVE_URL + "/" + entityId + "/descriptor/" + descriptor);
        } catch (Exception e) {
            throw new Exception("Failed registering device for entity "+entityId+": Failed creating URL");
        }
        try{
            sendPost(url);
        }
        catch(Exception e){
            throw new Exception("Failed registering device for entity " + entityId + ": " + e.getMessage());
        }
    }

    private String getEntityId(JSONObject entity) throws Exception {
        Object entityIdObj;
        try {
            entityIdObj = entity.get(JSON_Entity_Id);
        } catch (JSONException e) {
            throw new Exception("Missing parameter '" + JSON_Entity_Id + "'");
        }
        if ((entityIdObj == null) || !(entityIdObj instanceof String )) {
            throw new Exception("Missing parameter '" + JSON_Entity_Id + "' or it is not a string.");
        }

        return (String)entityIdObj;
    }

    // unregister the device (no matter what entity it is registered for)
    private void _unregisterDevice() throws Exception {
        // Get the entity which the device is registered. If the entity is empty then it means the device is not registered.
        String descriptor;
         try{
             descriptor = getDeviceDescriptor();
         } catch (Exception e){
             throw new Exception("Failed unregistering the device: " + e.getMessage());
        }
        JSONObject retrievedEntity;
        try {
            retrievedEntity = _retrieveEntity(descriptor);
        } catch (Exception e){
            throw new Exception("Failed unregistering the device: Failed retrieving entity by descriptor: " + e.getMessage());
        }
        String entityId;
        try {
            entityId = getEntityId(retrievedEntity);
        } catch (Exception e){
            throw new Exception("Failed unregistering the device: " + e.getMessage());
        }

        if ("".equals(entityId)){
            return; // The device is not registered
        }
        // Unregister the device by unregistering the device identifier as a descriptor for the entity.
        URL url;
        try {
            url = new URL(serverURL + REGISTER_ENTITY_RELATIVE_URL + "/" + entityId + "/descriptor/" + descriptor);
        } catch (Exception e) {
            throw new Exception("Failed unregistering the device: " + entityId + ": Failed creating URL");
        }
        try{
            sendDelete(url);
        } catch (Exception e) {
            throw new Exception("Failed unregistering the device: " + entityId + ": " + e.getMessage());
        }
    }

    private JSONObject _retrieveLocation(String entityId) throws Exception {
        URL url;
        JSONObject location;
        try {
            url = new URL(serverURL + RETRIEVE_LOCATION_RELATIVE_URL + "/" + entityId);
        } catch (Exception e) {
            throw new Exception("Failed retrieving location for entity " + entityId + ": Failed creating URL");
        }
        String locationStr;
        try{
            locationStr = sendGet(url);
        } catch (Exception e){
            throw new Exception("Failed retrieving location for entity " + entityId + ": " + e.getMessage());
        }
        try{
            location = new JSONObject(locationStr);
        } catch (Exception e) {
            throw new Exception("Failed retrieving location for entity " + entityId + ": Failed parsing JSON data: " + locationStr);
        }

        return location;
    }

    private Bitmap _retrieveMap(int rootZoneId, int floor) throws Exception {
        URL url;
        Bitmap bmp;
        try {
            url = new URL(serverURL + RETRIEVE_MAP_RELATIVE_URL + "/" + rootZoneId + "/" + floor);
        } catch (Exception e) {
            throw new Exception("Failed retrieving map for root-zone " +rootZoneId + " and floor " + floor + ": Failed creating URL");
        }
        try{
                bmp = sendGetImage(url);
        }
        catch (Exception e){
            throw new Exception("Failed retrieving map for root-zone " +rootZoneId + " and floor " + floor + ": \n" + e.getMessage());
        }
        return bmp;
    }

    private Bitmap _retrieveMap(JSONObject location)throws Exception{
        Object rootZoneIdObj;
        Object zonesObj;
        try {
            zonesObj = location.get(JSON_EntityFullLocation_Zones);

        } catch (JSONException e) {
            throw new Exception("Failed retrieving map: Missing parameter '" + JSON_EntityFullLocation_Zones + "'");
        }
        if ((zonesObj)== null || !(zonesObj instanceof org.json.JSONArray)){
            throw new Exception("Failed retrieving map: Missing parameter '" + JSON_EntityFullLocation_Zones + "' or it is not an org.json.JSONArray.");
        }
        int rootZoneId = -1;
        for (int i = 0; i < ((org.json.JSONArray)zonesObj).length(); i++){
            Object zoneObj = ((org.json.JSONArray) zonesObj).get(i);
            JSONObject zone = (JSONObject)zoneObj;
            Object zoneTypeObject;
            try {
                zoneTypeObject = zone.get(JSON_Zone_Type);
            } catch (JSONException e) {
                throw new Exception("Failed retrieving map: Missing parameter '" + JSON_Zone_Type + "' for root-zone");
            }
            if ((zoneTypeObject == null) || !(zoneTypeObject instanceof String)){
                throw new Exception("Failed retrieving map: Missing parameter '" + JSON_Zone_Type + "' for root-zone or it is not a String.");
            }
            String zoneType = (String)zoneTypeObject;
            if ("root-zone".equals(zoneType)){
                try {
                    rootZoneIdObj = zone.get(JSON_Zone_Id);
                } catch (JSONException e) {
                    throw new Exception("Failed retrieving map: Missing parameter '" + JSON_Zone_Id + "'");
                }
                if ((rootZoneIdObj == null) || !(rootZoneIdObj instanceof Number)){
                    throw new Exception("Failed retrieving map: Missing parameter '" + JSON_Zone_Id + "' or it is not a Number.");
                }
                rootZoneId = ((Number)rootZoneIdObj).intValue();
                break;
            }
        }
        if (rootZoneId == -1){
            throw new Exception("Failed retrieving map: Could not find the root-zone in the location");
        }
        Object floorObj;
        try {
            floorObj = location.get(JSON_EntityFullLocation_Z);
        } catch (JSONException e) {
            throw new Exception("Failed retrieving map: Missing parameter '" + JSON_EntityFullLocation_Z + "'");
        }
        if ((floorObj == null) || !(floorObj instanceof Number )) {
            throw new Exception("Failed retrieving map: Missing parameter '" + JSON_EntityFullLocation_Z + "' or it is not a Number.");
        }

        int floor = ((Number)floorObj).intValue();

        return  _retrieveMap(rootZoneId, floor);
    }
    /****************************************************   Internal functions ********************************************************/

    Set<String> retrieveProximityUUIDsSync() throws Exception {
        AsyncTask<Void,Void,Result> localTask = new AsyncTask<Void,Void,Result>(){

            @Override
            protected Result doInBackground(Void... voids) {
                Set<String> proximityUUIDs;
                try{
                    proximityUUIDs = _retrieveProximityUUIDs();
                } catch (Exception e){
                    return new Result(null, e);
                }
                return new Result(proximityUUIDs,null);
            }
        };
        localTask.execute();
        Result result = localTask.get();
        if (result.exception != null) {
            throw result.exception;
        }
        return (Set<String>)result.result;
    }
    private Set<String> _retrieveProximityUUIDs() throws Exception{
        Set<String> proximityUUIDs = new HashSet<String>();
        URL url;
        try {
            url = new URL(serverURL + RETRIEVE_PROXIMITY_UUIDS_RELATIVE_URL);
        } catch (Exception e) {
            throw new Exception("\nFailed retrieving proximity UUIDs: Failed creating URL\n");
        }
        JSONArray data;
        try {
            data = sendGetJSONArray(url);
        }
        catch (Exception e){
            throw new Exception("\nFailed retrieving proximity UUIDs: \n" + e.getMessage());
        }
        for (Object obj : data) {
            if ((obj == null) || !(obj instanceof String )) {
                throw new Exception("\nFailed retrieving proximity UUIDs: Failed converting JSON data\n");
            }
           proximityUUIDs.add((String)obj);
        }
        return proximityUUIDs;
    }

    List<Integer> retrieveSensorsIdsAdjacentToSensorSync(int sensorId) throws Exception {
        AsyncTask<Integer,Void,Result> localTask = new AsyncTask<Integer,Void,Result>(){
            @Override
            protected Result doInBackground(Integer... sensorIds) {
                List<Integer> sensorsIdsAdjacentToSensor;
                try {
                    sensorsIdsAdjacentToSensor = _retrieveSensorsIdsAdjacentToSensor(sensorIds[0]);
                } catch (Exception e){
                    return new Result(null, e);
                }
                return new Result(sensorsIdsAdjacentToSensor, null);
            }
        };

        localTask.execute(sensorId);
        Result result = localTask.get();
        if (result.exception != null) {
            throw result.exception;
        }
        return (List<Integer>)result.result;
    }
    private List<Integer> _retrieveSensorsIdsAdjacentToSensor(int sensorId) throws Exception{
        List<Integer> sensorsIdsAdjacentToSensor = new LinkedList<Integer>();
        URL url;
        try{
            url = new URL(serverURL + RETRIEVE_SENSORS_IDS_ADJACENT_TO_SENSOR_RELATIVE_URL+"/"+sensorId);
        } catch (Exception e) {
            throw new Exception("\nFailed retrieving sensors ids adjacent to sensor "+sensorId+": Failed creating URL");
        }
        JSONArray data;
        try {
            data = sendGetJSONArray(url);
        }
        catch (Exception e){
            throw new Exception("\nFailed retrieving sensors ids adjacent to sensor "+sensorId+": \n" + e.getMessage());
        }
        for (Object obj : data) {
            if ((obj == null) || !(obj instanceof Number)){
                throw new Exception("\nFailed retrieving sensors ids adjacent to sensor "+sensorId+": Failed converting JSON data");
            }
            sensorsIdsAdjacentToSensor.add(((Number)obj).intValue());
        }
        return sensorsIdsAdjacentToSensor;
    }

    void sendNotificationMessageAsync(PZNotificationMessage notificationMessage, PZAPICompletionHandler completionHandler){
        new AsyncTask<Object,Void,Void>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected Void doInBackground(Object... obj) {
                completionHandler = (PZAPICompletionHandler)obj[1];
                try {
                    _sendNotificationMessage((PZNotificationMessage)obj[0]);
                }
                catch(Exception e){
                    exceptionToBeThrown = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(notificationMessage, completionHandler);
    }
    private void _sendNotificationMessage(PZNotificationMessage notificationMessage) throws Exception {
        JSONObject toSendJSON = notificationMessage.createNotificationMessage();
        URL url;
        try {
            url = new URL(serverURL + SEND_NOTIFICATION_MESSAGE_RELATIVE_URL);
        } catch (Exception e) {
            throw new Exception("\nFailed sending the notification message: Failed creating URL");
        }
        try {
            sendPut(url,toSendJSON);
        } catch (IOException e) {
            throw new Exception("\nFailed sending the notification message: \n"+e.getMessage());
        }
    }
    /**************************************   bluetooth adapter MAC address as descriptor *********************************************/
    private String getDeviceDescriptor() throws Exception {
        return getBluetoothMacAddress();
    }

    private static String getBluetoothMacAddress() throws Exception {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // if device does not support Bluetooth
        if(bluetoothAdapter==null){
            throw new Exception("Device does not support bluetooth");
        }

        return bluetoothAdapter.getAddress();
    }

    private void retrieveEntityByDescriptorAsync(String descriptor, PZAPICompletionHandler completionHandler){
        new AsyncTask<Object,Void,JSONObject>(){
            private Exception exceptionToBeThrown;
            private PZAPICompletionHandler completionHandler;
            @Override
            protected JSONObject doInBackground(Object... obj) {
                JSONObject entity = null;
                completionHandler = (PZAPICompletionHandler)obj[1];
                try{
                    entity = _retrieveEntityByDescriptor((String) obj[0]);
                } catch (Exception e){
                    exceptionToBeThrown = e;
                }
                return entity;
            }

            protected void onPostExecute(JSONObject result) {
                completionHandler.onComplete(result, exceptionToBeThrown);
            }

        }.execute(descriptor, completionHandler);
    }

    private JSONObject retrieveEntityByDescriptorSync(String descriptor) throws Exception {
        AsyncTask<String,Void,Result> localTask = new AsyncTask<String,Void,Result>(){

            @Override
            protected Result doInBackground(String... descriptors) {
                JSONObject entity;
                try{
                    entity = _retrieveEntityByDescriptor(descriptors[0]);
                } catch (Exception e){
                    return new Result(null, e);
                }

                return new Result(entity, null);
            }
        };
        localTask.execute(descriptor);
        Result result = localTask.get();
        if (result.exception != null) {
            throw result.exception;
        }
        return (JSONObject)result.result;
    }

    private JSONObject _retrieveEntityByDescriptor(String descriptor) throws Exception {
        JSONObject entity;
        URL url;
        try {
            url = new URL(serverURL + RETRIEVE_ENTITY_RELATIVE_URL + "/" + descriptor);
        }catch (Exception e) {
            throw new Exception("Failed retrieving entity " + descriptor +": Failed creating URL");
        }
        String entityStr;
        try {
            entityStr = sendGet(url);
        } catch (Exception e){
            throw new Exception("Failed retrieving entity " + descriptor +": " + e.getMessage());
        }
        try{
            entity = new JSONObject(entityStr);
        } catch (Exception e) {
            throw new Exception("Failed retrieving entity " + descriptor +": Failed converting JSON data:" + entityStr);
        }
        return entity;
    }

    private JSONObject createEntity(String entityId, String name, String type, String description, String email, String loyalty, String phone, boolean optedIn) throws Exception {
        JSONObject jo = new JSONObject();
        try{
            jo.put(JSON_Entity_Id, entityId);
            jo.put(JSON_Entity_Name, name);
            jo.put(JSON_Entity_Type, type);
            jo.put(JSON_Entity_Description, description);
            jo.put(JSON_Entity_Email, email);
            jo.put(JSON_Entity_Loyalty, loyalty);
            jo.put(JSON_Entity_Phone, phone);
            jo.put(JSON_Entity_OptedIn, optedIn);
        } catch (JSONException e) {
            throw new Exception("Failed converting entity to JSON: "+ e.getMessage());
        }
        return jo;
    }

    class Result{
        Object result;
        Exception exception;
        Result(Object result, Exception e){this.result = result; this.exception = e;}
    }
    /******************************************************   REST functions **********************************************************/
    private void sendDelete(URL url) throws Exception {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
            connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
            connection.setRequestMethod("DELETE");
            // Starts the query
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode != HttpStatus.SC_OK) {
                throw new Exception("Response code error: "+ responseCode);
            }
    }

    private Bitmap sendGetImage(URL url) throws IOException {
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setDoInput(true);
         connection.connect();
         InputStream input = connection.getInputStream();

         return BitmapFactory.decodeStream(input);
 }
    private JSONArray sendGetJSONArray(URL url) throws Exception{
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
            connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            // Starts the query
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpStatus.SC_OK) {
                InputStream is = connection.getInputStream();
                try {
                    return JSONArray.parse(is);
                }
                catch (Exception e){
                   throw new  Exception("Failed parsing JSONArray data");
                }
            }
            else {
                throw new Exception("Response code error"+ responseCode);
            }
    }
    private String sendGet(URL url) throws Exception {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
            connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            // Starts the query
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpStatus.SC_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                br.close();
                return sb.toString();
            }
            else {
                throw new Exception("Response code error"+ responseCode);
            }
    }

    private void sendPost(URL url) throws Exception {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
            connection.setConnectTimeout(CONNECTION_TIMEOUT_IN_MILLISECONDS);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            // Starts the query
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpStatus.SC_OK) {
                throw new Exception("ResponseCode Error"+ responseCode);
            }
    }

    private void sendPut(URL url, JSONObject toSendJSON) throws IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpPut put = new HttpPut(url.toString());
            put.setHeader(HTTP.CONTENT_TYPE, "application/json");
            put.setEntity(new ByteArrayEntity(toSendJSON.toString().getBytes("UTF8")));
            httpClient.execute(put,responseHandler);
    }

}
