# Presence Insights SDK for Android

## Getting Started
#### Setting up PIAPIAdapter

    PIAPIAdapter mAdapter = new PIAPIAdapter(context, "https://www.url.com", "TenantCode", "OrgCode");
                                             
#### Making a call to the API

    mAdapter.getOrg(new PIAPICompletionHandler() {
        @Override
        public void onComplete(PIAPIResult result) {
            Log.i(TAG, result.getResult());
        }
    });

Note: `PIAPICompletionHandler` is the callback interface for any asynchronous calls to the API.
    
#### Setting up PIBeaconSensor

    PIBeaconSensor mBeaconSensor = new PIBeaconSensor(context, mAdapter);

    
#### Starting and stopping the beacon sensor

    mBeaconSensor.start()
    
    mBeaconSensor.stop()
    
It's that easy!

#### Setting the beacon advertisement layout
We use AltBeacon's Android library for monitoring and ranging for beacons.  

    // adding beacon layout for Estimote Beacons
    mBeaconSensor.addBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
    
From AltBeacon's [Github](https://github.com/AltBeacon/android-beacon-library), "IMPORTANT: By default, this library will only detect beacons meeting the AltBeacon specification."



## Classes and Interfaces

Have a look at the javadocs generated for this library, available in the `javadoc/` folder and launching `index.html`.
