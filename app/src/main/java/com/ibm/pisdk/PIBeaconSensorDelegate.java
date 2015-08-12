//
// IBM Confidential
// OCO Source Materials
// 5725-U96 © Copyright IBM Corp. 2015
// The source code for this program is not published or otherwise
// divested of its trade secrets, irrespective of what has
// been deposited with the U.S. Copyright Office.
//
package com.ibm.pisdk;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;

/**
 * This interface provides the users of the SDK callbacks regarding the beacon sensor.
 *
 * @author Ciaran Hannigan (cehannig@us.ibm.com)
 */
public interface PIBeaconSensorDelegate {

    /**
     * Provides a collection of beacons within range
     *
     * @param beacons collection of Class Beacon.
     */
    void beaconsInRange(ArrayList<Beacon> beacons);
}
