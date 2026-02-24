#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RUN_TESTS=1
if [[ "${1:-}" == "--skip-tests" ]]; then
  RUN_TESTS=0
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "adb command not found. Add Android platform-tools to PATH."
  exit 1
fi

adb wait-for-device >/dev/null
if [[ "$(adb get-state 2>/dev/null)" != "device" ]]; then
  echo "No authorized Android device found (adb get-state != device)."
  exit 1
fi

if [[ "$RUN_TESTS" -eq 1 ]]; then
  ./gradlew --no-daemon :app:testDebugUnitTest
fi

./gradlew --no-daemon :app:installDebug
adb shell am start --user current -n com.opencode.sshterminal/.app.MainActivity >/dev/null

echo "Device smoke run complete: app installed and launched."
