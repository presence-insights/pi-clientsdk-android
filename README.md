# Presence Insights SDK for Android

## Getting Started
#### Setting up PIAPIAdapter

    PIAPIAdapter mAdapter = new PIAPIAdapter(new URL("https://www.url.com/pi-config/v1"),
                                             new URL("https://www.url.com/conn-beacon"),
                                             "TenantCode",
                                             "OrgCode");
                                             
#### Making a call to the API

    mAdapter.getOrg(new PIAPICompletionHandler() {
        @Override
        public void onComplete(PIAPIResult result) {
            // result.getResult();
            }
        }
    );

Note: `PIAPICompletionHandler` is the callback interface for any asynchronous calls to the API.
    
#### Setting up PIBeaconSensor

    PIBeaconSensor mBeaconSensor = new PIBeaconSensor(getActivity(), mAdapter);
    
Note: `getActivity()` retrieves the current activity context.
    
#### Starting and stopping the beacon sensor

    mBeaconSensor.start()
    
    mBeaconSensor.stop()
    
It's that easy!

#### Setting the beacon advertisement layout
We use AltBeacon's Android library for monitoring and ranging for beacons.  

    // adding beacon layout for Estimote Beacons
    mBeaconSensor.addBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
    
From their [Github](https://github.com/AltBeacon/android-beacon-library): IMPORTANT: By default, this library will only detect beacons meeting the AltBeacon specification.



## Classes
#### PIAPIAdapter

#### PIBeaconSensor

#### PIAPIResult

#### PIBeaconData

#### PIDeviceInfo

#### PIAPICompletionHandler

#### DeviceInfo

