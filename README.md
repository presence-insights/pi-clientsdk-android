# Presence Insights SDK for Android

This library contains classes that are useful for interfacing with Presence Insights. This SDK supports Android 4.3+.

## Features

* **BLE beacon sensor** - monitor regions, range for beacons, and send beacon notification messages to PI

* **Management Config REST** - make calls to the management config server to retrieve information about your organization

* **Device Registration** - easily registers a smartphone or tablet with your organization.

## Adding the library to your project

1. Create `/libs` directory if it does not already exist.

2. Add .aar file to `/libs` directory.

3. Add dependencies to module `build.gradle`.  Need to include the altbeacon library, for now.  We are working on resolving this issue.

        dependencies {
            compile 'org.altbeacon:android-beacon-library:2.1.4'
            compile (name:'presence-insights-v1.1', ext:'aar')
        }
    
4. Add `flatDir` to project `build.gradle`.

        allprojects {
            repositories {
            
            ...     
            
                flatDir {
                    dirs 'libs'
                }
            }
        }

5. Add Internet and Bluetooth permissions to your manifest file

        <uses-permission android:name="android.permission.INTERNET"/>
        <uses-permission android:name="android.permission.BLUETOOTH"/>

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

## Javadocs

Jump over to the <a href="/pidocs/mobile/android/javadoc/" target="_blank">Javadocs</a> for more information.

## Downloads

<a href="/pidocs/sdk/pi-android-sdk.zip" target="_blank">Download the Android SDK</a>
