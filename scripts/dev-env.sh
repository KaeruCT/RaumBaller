#!/usr/bin/env bash
# Source this file before running Gradle commands in this sandboxed/local dev setup:
#   source scripts/dev-env.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ROOT_DIR/.android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"

# Keep Android/Gradle state inside the repo. This avoids writes to ~/.android and ~/.gradle
# in restricted sandboxes and makes the setup self-contained.
export ANDROID_SDK_HOME="${ANDROID_SDK_HOME:-$ROOT_DIR/.android-sdk-home}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$ROOT_DIR/.android-user-home}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-user-home}"

export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
