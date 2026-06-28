#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="$ROOT_DIR/.android-sdk"
TMP_DIR="$ROOT_DIR/.tmp"
CMDLINE_TOOLS_ZIP="$TMP_DIR/commandlinetools-mac.zip"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"

# The current Android command line tools need Java 17+. The app build itself uses Java 11.
SDKMANAGER_JAVA_HOME="${SDKMANAGER_JAVA_HOME:-/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home}"
export JAVA_HOME="$SDKMANAGER_JAVA_HOME"
export PATH="$JAVA_HOME/bin:$SDK_DIR/cmdline-tools/latest/bin:$PATH"
export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_HOME="${ANDROID_SDK_HOME:-$ROOT_DIR/.android-sdk-home}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$ROOT_DIR/.android-user-home}"
export HOME="${HOME:-$ROOT_DIR/.home}"

mkdir -p "$SDK_DIR/cmdline-tools" "$TMP_DIR" "$ANDROID_SDK_HOME" "$ANDROID_USER_HOME"

if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
  curl -L --fail "$CMDLINE_TOOLS_URL" -o "$CMDLINE_TOOLS_ZIP"
  rm -rf "$TMP_DIR/cmdline-tools"
  unzip -q "$CMDLINE_TOOLS_ZIP" -d "$TMP_DIR"
  mv "$TMP_DIR/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
fi

# Accept licenses and install the Android packages used by app/build.gradle.
yes | sdkmanager --sdk_root="$SDK_DIR" --licenses >/dev/null || true
sdkmanager --sdk_root="$SDK_DIR" \
  "platform-tools" \
  "platforms;android-30" \
  "build-tools;30.0.3"

cat > "$ROOT_DIR/local.properties" <<LOCAL_PROPERTIES
sdk.dir=$SDK_DIR
LOCAL_PROPERTIES

echo "Android SDK ready at $SDK_DIR"
echo "Run: source scripts/dev-env.sh && ./gradlew :app:assembleDebug"
