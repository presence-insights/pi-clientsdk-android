# Presence Insights SDK for Android

## Intro

&nbsp;&nbsp;&nbsp;&nbsp;
This library contains classes that are useful for interfacing with Presence Insights.

## Features/Overview

* **BLE beacon sensor** - monitor regions, range for beacons, and send beacon notification messages to PI


* **Management Config REST** - make calls to the management config server to retrieve information about your organization


* **Device Registration** - easily registers a smartphone or tablet with your organization.


## Getting Started

### Adding the library to your project

1. download library at <where can they download the library?>

2. add library file to /libs

3. add dependencies to gradle

    dependencies {
        compile 'org.altbeacon:android-beacon-library:2.1.4'
        compile (name:'presence-insights-v1.1', ext:'aar')
    }

### Setting up PIAPIAdapter

    PIAPIAdapter mAdapter = new PIAPIAdapter(context, "username", "password", "https://www.url.com", "TenantCode", "OrgCode");
                                             
### Making a call to the API

    mAdapter.getOrg(new PIAPICompletionHandler() {
        @Override
        public void onComplete(PIAPIResult result) {
            Log.i(TAG, result.getResultAsString());
        }
    });

Note: `PIAPICompletionHandler` is the callback interface for any asynchronous calls to the API.
    
### Setting up PIBeaconSensor

    PIBeaconSensor mBeaconSensor = new PIBeaconSensor(context, mAdapter);

    
### Starting and stopping the beacon sensor

    mBeaconSensor.start()
    
    mBeaconSensor.stop()
    
It's that easy!

### Setting the beacon advertisement layout
We use AltBeacon's Android library for monitoring and ranging for beacons.  

    // adding beacon layout for Estimote Beacons
    mBeaconSensor.addBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
    
From AltBeacon's [Github](https://github.com/AltBeacon/android-beacon-library), "IMPORTANT: By default, this library will only detect beacons meeting the AltBeacon specification."


## Classes and Interfaces

Have a look at the javadocs generated for this library, available in the `javadoc/` folder and launching `index.html`.
