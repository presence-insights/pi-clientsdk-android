#!/bin/bash

echo "moving app-debug.aar to test app as presence-insights-v1.1.aar"

cp ./pi-sdk/build/outputs/aar/pi-sdk-debug.aar ../pi-clientsdk-android-test/app/libs/presence-insights.aar
echo "completed move"
