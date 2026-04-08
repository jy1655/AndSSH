#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

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

TEST_CLASS=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --class)
      TEST_CLASS="${2:-}"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--class fully.qualified.TestClass]"
      exit 1
      ;;
  esac
done

GRADLE_CMD=(./gradlew --no-daemon :app:connectedDeviceTestAndroidTest)
if [[ -n "$TEST_CLASS" ]]; then
  GRADLE_CMD+=("-Pandroid.testInstrumentationRunnerArguments.class=$TEST_CLASS")
fi

"${GRADLE_CMD[@]}"

echo "Connected deviceTest instrumentation completed on $DEVICE_SERIAL."
