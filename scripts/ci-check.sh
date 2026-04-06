#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest

# ---------------------------------------------------------------------------
# Verify that the debug APK contains our locally-built 16KB-aligned libtermux.so
# for every ABI we ship. This catches cases where the AAR's pre-built copy
# sneaks back in or the local build is accidentally excluded.
# ---------------------------------------------------------------------------
command -v readelf >/dev/null 2>&1 || { echo "ERROR: readelf not found. Install binutils." >&2; exit 1; }

APK="app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "ERROR: Debug APK not found at $APK." >&2
  exit 1
fi

TMPDIR_CI=$(mktemp -d)
trap 'rm -rf "$TMPDIR_CI"' EXIT

FOUND_ANY=false
for abi in arm64-v8a armeabi-v7a x86 x86_64; do
  TMP="$TMPDIR_CI/libtermux_${abi}.so"
  if ! unzip -p "$APK" "lib/$abi/libtermux.so" > "$TMP" 2>/dev/null; then
    rm -f "$TMP"
    continue
  fi
  FOUND_ANY=true

  # Verify the LOAD segment alignment is >= 16KB (0x4000).
  # Normalize hex values (prepend 0x if missing) for POSIX-compatible conversion.
  MAX_ALIGN=$(readelf -l "$TMP" \
    | awk '/^ *LOAD/{print $NF}' \
    | while read -r hex; do printf "%d\n" "0x${hex#0x}"; done \
    | sort -rn | head -1)

  if [[ -z "$MAX_ALIGN" || "$MAX_ALIGN" -lt 16384 ]]; then
    echo "ERROR: libtermux.so ($abi) is NOT 16KB page-aligned (align=${MAX_ALIGN:-unknown})." >&2
    exit 1
  fi

  echo "OK: libtermux.so ($abi) page-aligned at ${MAX_ALIGN} bytes."
done

if [[ "$FOUND_ANY" != "true" ]]; then
  echo "ERROR: No libtermux.so found in APK for any ABI." >&2
  exit 1
fi
