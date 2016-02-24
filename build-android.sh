#!/bin/bash

echo "running gradle clean"
./gradlew clean
echo "running gradle build"
./gradlew build
echo "changing file name from pi-sdk-release to presence-insights.aar"
cp ./pi-sdk/build/outputs/aar/pi-sdk-release.aar ./pi-sdk/build/outputs/aar/presence-insights.aar

echo "================================="
echo "all done!"
echo "outputs can be found in ./pi-sdk/build/docs/ and ./pi-sdk/build/outputs/aar/ respectively"
echo "================================="
