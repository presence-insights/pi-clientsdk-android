Presence Insights SDK for Android
========================

This library contains classes that allows for easy integration with IBM [Presence Insights](https://console.ng.bluemix.net/catalog/presence-insights/) service available on [Bluemix](https://console.ng.bluemix.net/).

This SDK supports Android 4.3+.

Features
--------

* **BLE beacon sensor** - monitor regions, range for beacons, and send beacon notification messages to PI

* **Management Config REST** - make calls to the management config server to retrieve information about your organization

* **Device Registration** - easily registers a smartphone or tablet with your organization.

Getting Started
---------------

### Adding the library to your project

#### JCenter/MavenCentral

**This is the recommended approach for retrieving the library.**

*   [JCenter](http://jcenter.bintray.com/com/ibm/pi/pi-sdk/)
*   [MavenCentral](https://repo1.maven.org/maven2/com/ibm/pi/pi-sdk/)

In the project `build.gradle`,

    allprojects {
        repositories {
            // add for Maven Central repo
            mavenCentral()
            // add for JCenter repo
            jcenter()
        }
    }

In the module `build.gradle`,

    dependencies {
        compile 'com.ibm.pi:pi-sdk:1.3.0'
    }

#### Manually

[Download](http://presenceinsights.ibmcloud.com/pidocs/mobileapps/mobile_android/) or build the Presence Insights SDK library from GitHub. See [Building the SDK](#building-the-sdk)

1. Add library file to `/libs` directory. If the directory doesn't exist, create it.

2. Add the following dependencies to the modules `build.gradle` file:

        ```
        dependencies {
            compile 'org.altbeacon:android-beacon-library:2.7'
            compile (name:'presence-insights-1.3.0', ext:'aar')
        }
        ```

3. You will also have to add the `flatDir` attribute to the project `build.gradle` file:

        ```
        allprojects {
            repositories {
                flatDir {
                    dirs 'libs'
                }
            }
        }
        ```

4. Add the `altbeacon` library to the dependencies. This is a requirement if you are taking this manual approach. Edit the modules `build.gradle` file and add:

        compile 'org.altbeacon:android-beacon-library:2.7'

Sync your gradle project. You should now have access to all of the Presence Insights APIs!

### Building the SDK

You can build the Presence Insights SDK straight from Git. This is useful if you ever have any need to build a custom version of the SDK.

In the terminal, type:
 
    ```
    git clone git@github.ibm.com:PresenceInsights/pi-clientsdk-android.git
    cd pi-clientsdk-android
    chmod +x build-android.sh
    sh build-android.sh
    ```
    
**Tip:** At this point, you may need to configure your development environment. Just follow the error messages. Some common issues include forgetting to create an `ANDROID_HOME` environment variable, or not having the correct android SDK downloaded. 

After the build finishes, you will find the library file (.aar) under `./pi-sdk/build/outputs/aar/`.

Using the SDK
-------------

There are two classes that do most of the heavy lifting: PIAPIAdapter and PIBeaconSensor.

####The PIAPIAdapter <a name="pi_adapter"></a>

The first thing you need to do is initialize an adapter.

```
PIAPIAdapter mAdapter = new PIAPIAdapter(context, <username>, <password>, "https://presenceinsights.ibmcloud.com", <tenant>, <org>);
```

You can obtain your tenant, org, username, and password from the Presence Insights dashboard on Bluemix. We do not store your username and password, it is up to the developer using this library to properly secure the credentials.

You are now able to query all sorts of useful information from Presence Insights!

* How about presenting a floor map to your customers?

```
mAdapter.getFloorMap(<site code>, <floor code>, new PIAPICompletionHandler() {
    @Override
    public void onComplete(PIAPIResult result) {
        if (result.getResponseCode() == HttpURLConnection.HTTP_OK) {
            mMap.setImageBitmap((Bitmap)result.getResult());
        } else {
            Log.e(TAG, result.getResponseCode() + ": " + result.getResult());
        }
    }
});
```

* You want to get a list of all the beacons on that floor and display their location on the map you just retrieved?

```
piAdapter.getBeacons(<site code>, <floor code>, new PIAPICompletionHandler() {
    @Override
    public void onComplete(PIAPIResult result) {
        if (result.getResponseCode() == HttpURLConnection.HTTP_OK) {
            ArrayList<PIBeacon> beacons = (ArrayList<PIBeacon>) result.getResult();
            // use the x and y coords from each beacon obj to place them on the map.
        } else {
            Log.e(TAG, result.getResponseCode() + ": " + result.getResult());
        }
    }
});
```

Don't forget to add the Internet permission to your manifest file!

```
<uses-permission android:name="android.permission.INTERNET" />
```

####The PI Beacon Sensor

After you initialize the adapter, you can initialize a Beacon Sensor.

```
PIBeaconSensor mBeaconSensor = PIBeaconSensor.getInstance(context, mAdapter);
```

Before starting the beacon sensor you will need to add the beacon layout to tell the sensor how to read the BLE advertisement from your beacons.

```
// adding beacon layout for iBeacons
mBeaconSensor.addBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
```

*Note:* From AltBeacon's [Github](https://github.com/AltBeacon/android-beacon-library), "**IMPORTANT:** By default, this library will only detect beacons meeting the AltBeacon specification."

Now we can start the beacon sensor.

```
mBeaconSensor.start()
```

And that's really all there is to getting the app to start sending the device location back to your Presence Insights instance.

The SDK by default sends information about the beacons around you every 5 seconds. This can be adjusted (time in ms).

```
mBeaconSensor.setSendInterval(10000); // send every 10 seconds
```

To stop beacon sensing,

```
mBeaconSensor.stop()
```

## Listening for monitoring and ranging callbacks

We have three callbacks that you can tie into when using the `PIBeaconSensor`.

    beaconsInRange(ArrayList<Beacon> beacons)
    didEnterRegion(Region region)
    didExitRegion(Region region)

Set up a listener, and you are good to go.

    mBeaconSensor.setDidEnterRegionListener(this);

## Device Registration

When registering a device, you have the option to use our default device descriptor, or your own.

    // will create a registered device using the default descriptor
    PIDeviceInfo mDeviceInfo = new PIDeviceInfo(context, "My Nexus 5", "External");

    // will create a registered device using the custom descriptor
    PIDeviceInfo mDeviceInfo = new PIDeviceInfo(context, "My Nexus 5", "External", "my_custom_unique_id");

If the user chooses not to register their device, we offer a way to register the device anonymously.
    
    // will create an anonymous device using the default descriptor
    PIDeviceInfo mDeviceInfo = new PIDeviceInfo(context);

    // will create an anonymous device using the custom descriptor
    PIDeviceInfo mDeviceInfo = new PIDeviceInfo(context, "my_custom_unique_id");

Adding personal data to PIDeviceInfo object (optional).

    JSONObject data = new JSONObject();
    data.put("cellphone", "919-555-5555");
    data.put("email", "android@us.ibm.com");
    mDeviceInfo.setData(data);

Adding non personal data to PIDeviceInfo object (optional). For example, linking push notification ID from another service to a device.

    JSONObject data = new JSONObject();
    data.put("pushID", "PIisAwesome");
    mDeviceInfo.setUnencryptedData(data);

Blacklisting a device. For example, to ignore an employees device.

    mDeviceInfo.setBlacklisted(true);

Finally, once the PIDeviceInfo object is ready to register, make the call to registerDevice from the PIAPIAdapter. Instantiate mAdapter as seen in section above, ['Setting up PIAPIAdapter'](#pi_adapter)

    mAdapter.registerDevice(mDeviceInfo, new PIAPICompletionHandler() {
        @Override
        public void onComplete(PIAPIResult result) {
            // if result.getResponseCode() is OK or CREATED
            PIDevice device = (PIDevice) result.getResult();
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
        
*    How can I send location events when the application is in the background or not open?

    1. There is a background process we create that uses scanPeriod (how long you scan) and betweenScanPeriod (how long to wait before waking up and scanning BLE). You should set these to values that make sense for your application. Consider the fact that scanning for BLE devices drains the battery and reduces a users privacy. 
    2. If a user force closes the app, Android will stop these background services. If a user restarts their phone, these background processes will not be running. Consider solutions such as signage around your venue to encourage users to open the application. 

