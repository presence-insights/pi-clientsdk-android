# Presence Insights SDK for Android

This library contains classes that are useful for interfacing with Presence Insights. This SDK supports Android 4.3+.

## Features

* **BLE beacon sensor** - monitor regions, range for beacons, and send beacon notification messages to PI

* **Management Config REST** - make calls to the management config server to retrieve information about your organization

* **Device Registration** - easily registers a smartphone or tablet with your organization.

## Adding the library to your project

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

## Setting up PIAPIAdapter <a name="pi_adapter"></a>

        PIAPIAdapter mAdapter = new PIAPIAdapter(context, "username", "password", "https://www.url.com", "TenantCode", "OrgCode");

Note: We do not store your username and password, it is up to the developer using this library to properly secure the credentials.

## Making a call to the API

        mAdapter.getOrg(new PIAPICompletionHandler() {
            @Override
            public void onComplete(PIAPIResult result) {
                PIOrg myOrg = (PIOrg) result.getResult();
            }
        });

Note: `PIAPICompletionHandler` is the callback interface for all asynchronous calls to the API. Please refer to the javadocs for how to cast the result.

## Setting up PIBeaconSensor

        PIBeaconSensor mBeaconSensor = new PIBeaconSensor(context, mAdapter);

## Setting the beacon advertisement layout <a name="beacon_layout"></a>

We use AltBeacon's Android library for monitoring and ranging for beacons.

        // adding beacon layout for iBeacons
        mBeaconSensor.addBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");

From AltBeacon's [Github](https://github.com/AltBeacon/android-beacon-library), "IMPORTANT: By default, this library will only detect beacons meeting the AltBeacon specification."

## Starting and stopping the beacon sensor

        mBeaconSensor.start()
        mBeaconSensor.stop()

It's that easy!

## Listening for monitoring and ranging callbacks

We have three callbacks that you can tie into when using the `PIBeaconSensor`.

        beaconsInRange(ArrayList<Beacon> beacons)
        didEnterRegion(Region region)
        didExitRegion(Region region)

Set up a listener, and you are good to go.

        mBeaconSensor.setDidEnterRegionListener(this);

## Device Registration

1.  Create a PIDeviceInfo Object and set your values

        PIDeviceInfo mDeviceInfo = new PIDeviceInfo(context);
        mDeviceInfo.setName("My Nexus 5");
        mDeviceInfo.setType("External");
        mDeviceInfo.setRegistered(true);

2.  Adding personal data to PIDeviceInfo object (optional)

        JSONObject data = new JSONObject();
        data.put("cellphone", "919-555-5555");
        data.put("email", "android@us.ibm.com");

        mDeviceInfo.setData(data);

3.  Adding non personal data to PIDeviceInfo object (optional). For example, linking push notification ID from another service to a device.

        JSONObject data = new JSONObject();
        data.put("pushID", "PIisAwesome");

        mDeviceInfo.setUnencryptedData(data);

4.  Blacklisting a device. For example, to ignore an employees device.

        mDeviceInfo.setBlacklisted(true);

5.  Make the call to registerDevice from the PIAPIAdapter. Instantiate mAdapter as seen in section above, ['Setting up PIAPIAdapter'](#pi_adapter)

        mAdapter.registerDevice(mDeviceInfo, new PIAPICompletionHandler() {
            @Override
            public void onComplete(PIAPIResult result) {
                if (result.getResponseCode() == HttpStatus.SC_OK || result.getResponseCode() == HttpStatus.SC_CREATED) {
                    PIDevice device = new PIDevice(result.getResultAsJson());
                } else {
                    Log.e(TAG, result.getResultAsString());
                }
            }
        });

## Troubleshooting

*   First things first, if things are not working, enable debugging to see all the inner workings in LogCat.

        PILogger.enableDebugMode(true);

*   I started the beacon sensor, but it is not picking up any beacons. There are several reasons why this may be happening.
    1.  The beacons are not configured correctly in the PI UI. Ensure that the Proximity UUID is set correctly. We retrieve that to create a region to range for beacons.
    2.  The beacon layout has not been set on the PIBeaconSensor. Please see the section on [beacon advertisement layout](#beacon_layout).
    3.  The codes (username, password, tenant, org) used in creating the PIAPIAdapter may have been entered incorrectly.

*   I set up the callback for `beaconsInRange(ArrayList<Beacon>)`, but it is not being called. Make sure you set the listener for the callback you want.

        mBeaconSensor.setBeaconsInRangeListener(this);

