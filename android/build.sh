#!/usr/bin/env bash
# Build the Controller APK with the plain SDK tools. No Gradle, no libraries.
# Needs: JDK 17+ and Android SDK (platforms;android-35, build-tools;35.0.0).
# Output: android/build/controller.apk
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-$HOME/Android/Sdk}
BT=$SDK/build-tools/35.0.0
JAR=$SDK/platforms/android-35/android.jar
if [ -z "${JAVA_HOME:-}" ]; then JAVA_HOME=$(ls -d "$HOME"/.local/jdk/jdk-17* 2>/dev/null | head -1); fi
export PATH=$JAVA_HOME/bin:$PATH
KS=${CONTROLLER_KEYSTORE:-$HOME/.android/controller.keystore}  # keep this file: updates need the same key
VERSION=$(( ($(date +%s) - 1767225600) / 60 ))  # minutes since 2026-01-01: every build is newer than the last

rm -rf build && mkdir -p build/gen build/obj build/dex

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml \
  --java build/gen --min-sdk-version 28 --target-sdk-version 35 \
  --version-code "$VERSION" --version-name "1.$VERSION" build/res.zip

javac -source 8 -target 8 -Xlint:-options -encoding UTF-8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" -d build/obj \
  $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 28 --lib "$JAR" --output build/dex $(find build/obj -name '*.class')

cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk

if [ ! -f "$KS" ]; then
  mkdir -p "$(dirname "$KS")"
  keytool -genkeypair -keystore "$KS" -storepass controller -keypass controller -alias controller \
    -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=Controller" >/dev/null
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:controller --key-pass pass:controller \
  --out build/controller.apk build/aligned.apk
ls -l build/controller.apk
