#!/bin/bash

echo "moving app-debug.aar to test app as presence-insights-v1.1.aar"

cp ./app/build/outputs/aar/app-debug.aar ../pi-clientsdk-android-test/app/libs/presence-insights-v1.1.aar
echo "completed move"
