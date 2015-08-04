#!/bin/bash

LIBLOC="./app/build/outputs/aar/presence-insights.aar"
READMELOC="../static-docs/app/docs/mobile_android.md"
JAVADOCSLOC="./app/build/docs/javadoc"
DESTFOLDER="pi-android-sdk"

echo "================================="
echo "building zip for Android PI SDK!"
echo "will include the following,"
echo "	- aar file"
echo "	- README"
echo "	- javadocs"
echo "================================="
echo ""
echo "moving files into folder to be zipped."
mkdir ./$DESTFOLDER
cp $LIBLOC $DESTFOLDER
cp $READMELOC $DESTFOLDER
mv ./$DESTFOLDER/mobile_android.md ./$DESTFOLDER/README.md
cp -R $JAVADOCSLOC $DESTFOLDER 

echo "zipping the file!"
zip -r pi-android-sdk.zip $DESTFOLDER

rm -r zip/
echo "all done."

