#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RUN_TESTS=1
VARIANT="debug"
APPLICATION_ID="com.opencode.sshterminal"
MAIN_ACTIVITY="com.opencode.sshterminal.app.MainActivity"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-tests)
      RUN_TESTS=0
      shift
      ;;
    --device-test)
      VARIANT="deviceTest"
      APPLICATION_ID="com.opencode.sshterminal.devtest"
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--skip-tests] [--device-test]"
      exit 1
      ;;
  esac
done

if ! command -v adb >/dev/null 2>&1; then
  echo "adb command not found. Add Android platform-tools to PATH."
  exit 1
fi

DEVICE_SERIAL="${ANDROID_SERIAL:-$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')}"
if [[ -z "$DEVICE_SERIAL" ]]; then
  echo "No authorized Android device found."
  adb devices
  exit 1
fi

ADB=(adb -s "$DEVICE_SERIAL")
if [[ "$("${ADB[@]}" get-state 2>/dev/null)" != "device" ]]; then
  echo "Selected device is not ready: $DEVICE_SERIAL"
  exit 1
fi

if [[ "$RUN_TESTS" -eq 1 ]]; then
  ./gradlew --no-daemon :app:testDebugUnitTest
fi

if [[ "$VARIANT" == "deviceTest" ]]; then
  ./gradlew --no-daemon :app:installDeviceTest
else
  ./gradlew --no-daemon :app:installDebug
fi

"${ADB[@]}" shell am start --user current -n "$APPLICATION_ID/$MAIN_ACTIVITY" >/dev/null

echo "Device smoke run complete on $DEVICE_SERIAL: $APPLICATION_ID installed and launched."
