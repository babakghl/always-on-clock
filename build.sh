#!/bin/bash
# Manual Android build: aapt2 -> javac -> d8 -> zipalign -> apksigner
set -e
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk17}"
export PATH=$JAVA_HOME/bin:$PATH
SDK="${ANDROID_SDK:-${ANDROID_HOME:-$HOME/android-sdk}}"
BT=$SDK/build-tools/34.0.0
AJAR=$SDK/platforms/android-34/android.jar

rm -rf build
mkdir -p build/gen build/classes build/dex dist

echo "== aapt2 compile =="
$BT/aapt2 compile --dir src/main/res -o build/res.zip

echo "== aapt2 link =="
$BT/aapt2 link -o build/app-unsigned.apk \
    -I "$AJAR" \
    --manifest src/main/AndroidManifest.xml \
    --java build/gen \
    build/res.zip

echo "== javac =="
javac -encoding UTF-8 -source 8 -target 8 -cp "$AJAR" -d build/classes \
    $(find src/main/java build/gen -name '*.java')

echo "== jar + d8 =="
(cd build/classes && jar cf ../classes.jar .)
$BT/d8 --lib "$AJAR" --output build/dex build/classes.jar

echo "== package =="
cp build/app-unsigned.apk build/app-dex.apk
(cd build/dex && jar uf ../app-dex.apk classes.dex)
$BT/zipalign -f 4 build/app-dex.apk build/app-aligned.apk

echo "== keystore =="
KEYSTORE=~/workspace/aod-clock/debug.keystore
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" \
        -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 \
        -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

echo "== sign =="
$BT/apksigner sign --ks "$KEYSTORE" \
    --ks-pass pass:android --key-pass pass:android \
    --out dist/AlwaysOnClock.apk build/app-aligned.apk

echo "== verify =="
$BT/apksigner verify --print-certs dist/AlwaysOnClock.apk | head -3
ls -la dist/AlwaysOnClock.apk
echo BUILD_OK
