#!/bin/bash

echo "running gradle clean"
./gradlew clean
echo "running gradle build"
./gradlew build
echo "changing file name from app-release to presence-insights.aar"
cp ./app/build/outputs/aar/app-release.aar ./app/build/outputs/aar/presence-insights.aar

echo "================================="
echo "all done!"
echo "outputs can be found in ./app/build/docs/ and ./app/build/outputs/aar/ respectively"
echo "================================="

echo "building zip file for static docs"
sh ./compile-android-zip.sh
