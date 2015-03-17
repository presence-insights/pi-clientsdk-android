package com.ibm.pzsdk;

import java.io.Serializable;
import java.util.LinkedList;


public interface PZBeaconSensorDelegate extends Serializable {
    public void updatePZLocation(LinkedList<PZBeaconData> beacons);
}