#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TEST_CLASS="com.opencode.sshterminal.e2e.RealSshConnectionDeviceTest"
WORKSPACE_PATH="$ROOT_DIR"
SSH_USERNAME="${USER:-$(id -un)}"
SSH_PORT=""
TEMP_DIR=""
SSHD_PID=""
SSHD_LOG=""
DEVICE_SERIAL=""
SSHD_BIN=""
PROBE_TOKEN="ANDSSH_DEVICE_SSH_E2E_$(date +%s)"

usage() {
  cat <<'EOF'
Usage: ./scripts/device-ssh-e2e.sh [--workspace /abs/path] [--user username] [--port port]

Starts a temporary localhost sshd on the host, maps it into the connected Android device via
adb reverse, and runs the real-device SSH E2E instrumentation test against the current workspace.
EOF
}

shell_quote() {
  printf "'%s'" "${1//\'/\'\"\'\"\'}"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1"
    exit 1
  fi
}

pick_free_port() {
  python3 - <<'PY'
import socket
s = socket.socket()
s.bind(("127.0.0.1", 0))
print(s.getsockname()[1])
s.close()
PY
}

cleanup() {
  local exit_code=$?
  set +e
  if [[ -n "${DEVICE_SERIAL:-}" && -n "${SSH_PORT:-}" ]]; then
    adb -s "$DEVICE_SERIAL" reverse --remove "tcp:$SSH_PORT" >/dev/null 2>&1
  fi
  if [[ -n "$SSHD_PID" ]]; then
    kill "$SSHD_PID" >/dev/null 2>&1
    wait "$SSHD_PID" >/dev/null 2>&1
  fi
  if [[ $exit_code -ne 0 && -n "$SSHD_LOG" && -f "$SSHD_LOG" ]]; then
    echo
    echo "Temporary sshd log:"
    cat "$SSHD_LOG"
  fi
  if [[ -n "$TEMP_DIR" && -d "$TEMP_DIR" ]]; then
    rm -rf "$TEMP_DIR"
  fi
  exit $exit_code
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --workspace)
      WORKSPACE_PATH="$(cd "${2:?missing workspace path}" && pwd)"
      shift 2
      ;;
    --user)
      SSH_USERNAME="${2:?missing username}"
      shift 2
      ;;
    --port)
      SSH_PORT="${2:?missing port}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

require_command adb
require_command ssh
require_command ssh-keygen
require_command sshd
require_command python3
SSHD_BIN="$(command -v sshd)"

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

if [[ -z "$SSH_PORT" ]]; then
  SSH_PORT="$(pick_free_port)"
fi

TEMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/andssh-ssh-e2e.XXXXXX")"
SSHD_LOG="$TEMP_DIR/sshd.log"
trap cleanup EXIT

ssh-keygen -q -t ed25519 -N '' -f "$TEMP_DIR/client_key" >/dev/null
ssh-keygen -q -t ed25519 -N '' -f "$TEMP_DIR/ssh_host_ed25519_key" >/dev/null
chmod 600 "$TEMP_DIR/client_key" "$TEMP_DIR/ssh_host_ed25519_key"
cp "$TEMP_DIR/client_key.pub" "$TEMP_DIR/authorized_keys"
chmod 600 "$TEMP_DIR/authorized_keys"

cat >"$TEMP_DIR/sshd_config" <<EOF
Port $SSH_PORT
ListenAddress 127.0.0.1
HostKey $TEMP_DIR/ssh_host_ed25519_key
AuthorizedKeysFile $TEMP_DIR/authorized_keys
PidFile $TEMP_DIR/sshd.pid
PubkeyAuthentication yes
PasswordAuthentication no
KbdInteractiveAuthentication no
ChallengeResponseAuthentication no
UsePAM no
PermitRootLogin no
AllowUsers $SSH_USERNAME
StrictModes no
PrintMotd no
LogLevel VERBOSE
Subsystem sftp internal-sftp
EOF

"$SSHD_BIN" -D -f "$TEMP_DIR/sshd_config" -E "$SSHD_LOG" &
SSHD_PID=$!

python3 - "$SSH_PORT" <<'PY'
import socket
import sys
import time

port = int(sys.argv[1])
deadline = time.time() + 10
while time.time() < deadline:
    sock = socket.socket()
    sock.settimeout(0.2)
    try:
        sock.connect(("127.0.0.1", port))
    except OSError:
        time.sleep(0.2)
    else:
        sock.close()
        sys.exit(0)
    finally:
        sock.close()

print(f"Timed out waiting for localhost sshd on port {port}", file=sys.stderr)
sys.exit(1)
PY

REMOTE_WORKSPACE="$(shell_quote "$WORKSPACE_PATH")"
LOCAL_PROBE_OUTPUT="$(
  ssh \
    -i "$TEMP_DIR/client_key" \
    -p "$SSH_PORT" \
    -o BatchMode=yes \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o LogLevel=ERROR \
    "$SSH_USERNAME@127.0.0.1" \
    "cd $REMOTE_WORKSPACE && pwd"
)"

if [[ "$LOCAL_PROBE_OUTPUT" != "$WORKSPACE_PATH" ]]; then
  echo "Local SSH probe returned unexpected workspace path."
  echo "Expected: $WORKSPACE_PATH"
  echo "Actual:   $LOCAL_PROBE_OUTPUT"
  exit 1
fi

"${ADB[@]}" reverse "tcp:$SSH_PORT" "tcp:$SSH_PORT" >/dev/null

PRIVATE_KEY_BASE64="$(base64 < "$TEMP_DIR/client_key" | tr -d '\n')"

./gradlew \
  --no-daemon \
  :app:connectedDeviceTestAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.class=$TEST_CLASS" \
  "-Pandroid.testInstrumentationRunnerArguments.andsshTestSshHost=127.0.0.1" \
  "-Pandroid.testInstrumentationRunnerArguments.andsshTestSshPort=$SSH_PORT" \
  "-Pandroid.testInstrumentationRunnerArguments.andsshTestSshUsername=$SSH_USERNAME" \
  "-Pandroid.testInstrumentationRunnerArguments.andsshTestPrivateKeyBase64=$PRIVATE_KEY_BASE64" \
  "-Pandroid.testInstrumentationRunnerArguments.andsshTestWorkspacePath=$WORKSPACE_PATH" \
  "-Pandroid.testInstrumentationRunnerArguments.andsshTestProbeToken=$PROBE_TOKEN"

echo "Real-device SSH E2E completed on $DEVICE_SERIAL against $WORKSPACE_PATH."
