#!/bin/bash

echo "running gradle clean"
./gradlew clean
echo "running gradle build"
./gradlew build
echo "generating javadocs"
./gradlew generateReleaseJavadoc
echo "changing file name from app-release to presence-insights-v1.1.aar"
cp ./app/build/outputs/aar/app-release.aar ./app/build/outputs/aar/presence-insights-v1.1.aar
echo "================================="
echo "all done!"
echo "outputs can be found in ./app/build/docs/ and ./app/build/outputs/aar/ respectively"
echo "================================="
