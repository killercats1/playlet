#!/usr/bin/env bash
# Builds the Playlet Mobile APK without Gradle or the Android SDK manager.
# Needs: a JDK, and the Ubuntu/Debian packages aapt, dalvik-exchange, zipalign and apksigner
#   (sudo apt-get install aapt dalvik-exchange zipalign apksigner).
# The Android 11 (API 30) framework jar is downloaded once into build/.
# Signing: uses $KEYSTORE (password $KEYSTORE_PASS, alias $KEY_ALIAS) or creates build/debug.keystore.
# Android only installs an update over an existing install when it is signed with the same key, so keep
# the keystore if you want to update without uninstalling.
set -euo pipefail
cd "$(dirname "$0")"

OUT=build
ANDROID_JAR="${ANDROID_JAR:-$OUT/android.jar}"
KEYSTORE="${KEYSTORE:-$OUT/debug.keystore}"
KEYSTORE_PASS="${KEYSTORE_PASS:-android}"
KEY_ALIAS="${KEY_ALIAS:-playlet}"
VERSION=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' AndroidManifest.xml)

mkdir -p "$OUT"
if [ ! -f "$ANDROID_JAR" ]; then
    echo "Downloading android.jar (API 30)"
    curl -fsSL -o "$ANDROID_JAR" https://raw.githubusercontent.com/Sable/android-platforms/master/android-30/android.jar
fi

rm -rf "$OUT/gen" "$OUT/classes" "$OUT"/*.apk "$OUT/classes.dex"
mkdir -p "$OUT/gen" "$OUT/classes"

echo "Packaging resources"
aapt package -f -m -J "$OUT/gen" -M AndroidManifest.xml -S res -I "$ANDROID_JAR" -F "$OUT/app.unsigned.apk"

echo "Compiling"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -bootclasspath "$ANDROID_JAR" \
    -d "$OUT/classes" $(find src "$OUT/gen" -name '*.java')

echo "Dexing"
dalvik-exchange --dex --output="$OUT/classes.dex" "$OUT/classes"
(cd "$OUT" && aapt add app.unsigned.apk classes.dex > /dev/null)

echo "Aligning and signing"
zipalign -f 4 "$OUT/app.unsigned.apk" "$OUT/app.aligned.apk"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
        -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Playlet Mobile" > /dev/null
fi
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KEYSTORE_PASS" --ks-key-alias "$KEY_ALIAS" \
    --out "$OUT/playlet-mobile-$VERSION.apk" "$OUT/app.aligned.apk"
apksigner verify "$OUT/playlet-mobile-$VERSION.apk"
rm -f "$OUT/app.unsigned.apk" "$OUT/app.aligned.apk"
echo "Built $OUT/playlet-mobile-$VERSION.apk"
