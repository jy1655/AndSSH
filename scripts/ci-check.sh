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
elf_max_load_align() {
  local path="$1"
  if command -v readelf >/dev/null 2>&1; then
    readelf -l "$path" \
      | awk 'BEGIN{max=0} /^ *LOAD/{s=tolower($NF); sub(/^0x/,"",s); v=0; \
        for(i=1;i<=length(s);i++){v=v*16+index("0123456789abcdef",substr(s,i,1))-1} \
        if(v>max)max=v} END{print max}'
    return
  fi

  if ! command -v python3 >/dev/null 2>&1; then
    echo "ERROR: Neither readelf nor python3 is available for ELF alignment checks." >&2
    exit 1
  fi

  python3 - "$path" <<'PY'
import struct
import sys

path = sys.argv[1]
with open(path, "rb") as fh:
    data = fh.read()

if data[:4] != b"\x7fELF":
    raise SystemExit("0")

ei_class = data[4]
ei_data = data[5]
endian = "<" if ei_data == 1 else ">"
max_align = 0

if ei_class == 1:
    e_phoff = struct.unpack_from(endian + "I", data, 28)[0]
    e_phentsize = struct.unpack_from(endian + "H", data, 42)[0]
    e_phnum = struct.unpack_from(endian + "H", data, 44)[0]
    for index in range(e_phnum):
        offset = e_phoff + index * e_phentsize
        p_type, _, _, _, _, _, _, p_align = struct.unpack_from(endian + "IIIIIIII", data, offset)
        if p_type == 1 and p_align > max_align:
            max_align = p_align
elif ei_class == 2:
    e_phoff = struct.unpack_from(endian + "Q", data, 32)[0]
    e_phentsize = struct.unpack_from(endian + "H", data, 54)[0]
    e_phnum = struct.unpack_from(endian + "H", data, 56)[0]
    for index in range(e_phnum):
        offset = e_phoff + index * e_phentsize
        p_type, _, _, _, _, _, _, p_align = struct.unpack_from(endian + "IIQQQQQQ", data, offset)
        if p_type == 1 and p_align > max_align:
            max_align = p_align

print(max_align)
PY
}

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

  # 16KB alignment required for Google Play's memory-mapped execution on 16KB-page devices.
  MAX_ALIGN=$(elf_max_load_align "$TMP")

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
