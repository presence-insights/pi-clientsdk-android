# Presence Insights SDK for Android

![This content is in draft state](https://img.shields.io/badge/documentation-draft-lightgrey.svg)

## Intro

This library contains classes that are useful for interfacing with Presence Insights.

## Features/Overview

* **BLE beacon sensor** - monitor regions, range for beacons, and send beacon notification messages to PI


* **Management Config REST** - make calls to the management config server to retrieve information about your organization


* **Device Registration** - easily registers a smartphone or tablet with your organization.


## Getting Started

### Adding the library to your project

1. Download or build the Presence Insights SDK library. See [Building the SDK](#building-the-sdk)

2. Add the library to your project. You can do this using Android Studio, or manually:

  **Using Android Studio:**

  Do not use Android Studio to import the project until we are able to publish the SDK on Maven.
  
  **Manually:**

  1. Add library file to `/libs` directory. 

  2. Add the following dependencies to `app/build.gradle` file:

        dependencies {
            compile 'org.altbeacon:android-beacon-library:2.1.4'
            compile (name:'presence-insights-v1.1', ext:'aar')
        }

  3. You will also have to add teh `flatDir` attribute to the top level `build.gradle` file:

        allprojects {
            repositories {
                jcenter()
                flatDir {
                    dirs 'libs'
                }
            }
        }

3. Add the `altbeacon` library to the dependencies. We are working on resolving this issue. Edit the `/app/build.gradle` file and add:
    compile 'org.altbeacon:android-beacon-library:2.1.4' to the `dependencies` object.

Sync your gradle project. You should now have access to all of the Presence Insights APIs!

## Building the SDK
You can build the Presence Insights SDK straight from git. This is useful if you ever have any need to build a custom version of the SDK.

1. In the terminal, type:
 
```
git clone git@github.ibm.com:PresenceInsights/pi-clientsdk-android.git
cd pi-clientsdk-android
chmod +x build-android.sh
// Wait a while for gradle services to download
```
    
**Tip** At this point, you may need to configure your development environment. Just follow the error messages. Some common issues include forgetting to create an `ANDROID_HOME` environment variable, or not having the correct android SDK downloaded. 

At this point, you should have a `/pi-android-sdk/` folder that contains a **.aar**, Android Library file.

### Setting up PIAPIAdapter

    PIAPIAdapter mAdapter = new PIAPIAdapter(context, "username", "password", "https://www.url.com", "TenantCode", "OrgCode");

Note: We do not store your username and password, it is up to the developer using this library to properly secure the credentials.

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

## Downloads

[Download the Android SDK](/pidocs/sdk/pi-android-sdk.zip)
