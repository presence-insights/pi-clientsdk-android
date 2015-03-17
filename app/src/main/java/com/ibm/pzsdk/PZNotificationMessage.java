package com.ibm.pzsdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by natalies on 21/09/2014.
 */
class PZNotificationMessage {

    static final String JSON_NotificationMessage_SensorNotificationMessages = "snm";

    LinkedList<PZBeaconData> beacons;
    String deviceId;

    public PZNotificationMessage() {
    }


    public PZNotificationMessage(String deviceId, LinkedList<PZBeaconData> beacons) {
        this.beacons = beacons;
        this.deviceId = deviceId;
    }

    public LinkedList<PZBeaconData> getBeacons() {
        return beacons;
    }

    public void setBeacons(LinkedList<PZBeaconData> beacons) {
        this.beacons = beacons;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    private List<PZBeaconData> sortBeacons(
            LinkedList<PZBeaconData> beacons) {
        Collections.sort(beacons, new Comparator<PZBeaconData>() {

            @Override
            public int compare(PZBeaconData lhs, PZBeaconData rhs) {
                return (lhs.getAvgRssi() > rhs.getAvgRssi()) ? -1 : (lhs.getAvgRssi() == rhs.getAvgRssi() ? 0 : 1);


            }
        });
        return beacons;
    }

    private PZBeaconData getClosestBeacon(LinkedList<PZBeaconData> beacons) {
        double maxAvgRSSI = -128.0;
        PZBeaconData closestBeacon=null;
        if (beacons != null) {
            for (PZBeaconData beacon : beacons) {
                if (beacon.getAvgRssi() > maxAvgRSSI) {
                    maxAvgRSSI = beacon.getAvgRssi();
                    closestBeacon = beacon;
                }
            }
        }
        return closestBeacon;
    }


    /* {"snm":[
   {"entityDescriptor":"omri-phone",
    "dataType":"beacon",
    "sensorId":911,
    "notificationType":1,
    "data":{"minor":911,
            "rssi":-26.8,
            "major":0,
            "accuracy":0.04466835921509631,
            "proximityUUID":"bb1edb27-b863-45b0-a68a-fd91af6fbd64",
            "proximity":"IMMEDIATE"},
    "notificationTimestamp":1410775743777,
    "sensorTimestamp":1410775743777},

   {"entityDescriptor":"omri-phone",
     "dataType":"beacon",
     "sensorId":911,
     "notificationType":1,
     "data":{"minor":915,
             "rssi":-34.4,
             "major":0,
             "accuracy":0.1273503081016662,
             "proximityUUID":"bb1edb27-b863-45b0-a68a-fd91af6fbd64",
             "proximity":"IMMEDIATE"},
   "notificationTimestamp":1410775743777,
   "sensorTimestamp":1410775743777}]}
   */

    public JSONObject createNotificationMessage()
    {
        PZBeaconData closestBeacon=getClosestBeacon(beacons);
        List<PZBeaconData> sortedBeacons =sortBeacons(beacons);
        int closestBeaconMinor = closestBeacon.getMinor();
        long now=System.currentTimeMillis();
        JSONObject notificationMessage = new JSONObject();
        try {
            JSONArray data = new JSONArray();
            for (PZBeaconData bd: sortedBeacons) {

                JSONObject jo = new JSONObject();// add the closest beacon first

                jo.put("sensorId", closestBeaconMinor); // the external sensor id in the notification msg is the major code of the nearest beacon
                jo.put("sensorTimestamp", now);
                jo.put("notificationTimestamp", now);
                jo.put("notificationType", bd.getState());
                jo.put("entityDescriptor", deviceId);
                jo.put("dataType", "beacon");


                JSONObject element = new JSONObject();
                element.put("rssi", bd.getAvgRssi());
                element.put("accuracy", bd.getAccuracy());
                element.put("proximity", bd.getProximity());
                element.put("proximityUUID", bd.getUuid());
                element.put("major", bd.getMajor());
                element.put("minor", bd.getMinor());
                jo.put("data",element);

                data.put(jo);
            }

            notificationMessage.put(JSON_NotificationMessage_SensorNotificationMessages, data);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return notificationMessage;
    }
}
