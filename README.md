# OpenCode SSH Terminal (MVP Scaffold)

Android native SSH terminal app scaffold focused on TTY-based remote CLI workflows.

## What is included

- Minimal Android Compose app module
- Foreground service skeleton for long-running SSH sessions
- Session state machine (`IDLE/CONNECTING/CONNECTED/FAILED`)
- SSH abstraction interfaces (`SshClient`, `SshSession`)
- `sshj`-based real SSH adapter with `PTY shell`, `window-change`, and stream read/write loop
- Host key change detection UX (fingerprint dialog with `Reject (Default)`, `Trust Once`, `Update known_hosts`)
- stdin 입력 UI (`Ctrl/Alt/ESC/TAB/ENTER`, 방향키, `^C/^D`, 직접 입력 전송)
- `sshj` SFTP channel adapter (`list`, `upload`, `download`)
- Main 화면에 SFTP 파일 브라우저 섹션(`List`, 디렉터리 `Open`, 파일 `Download`, 경로 기반 `Upload`)
- SFTP UX 개선: 진행 인디케이터, 업/다운로드 덮어쓰기 확인 다이얼로그, 권한/경로 오류 안내 문구
- Android 14+/targetSdk 35 foreground service 권한 정리 (`FOREGROUND_SERVICE_DATA_SYNC`)
- 모바일 화면 입력 UX 수정: 스크롤 + 가시성 있는 입력 필드(`OutlinedTextField`)
- GitHub Actions CI: `assembleDebug` + `testDebugUnitTest`
- Terminal abstraction (`TerminalEngine`) with simple in-memory implementation
- Placeholder key repository interface for Keystore+AEAD implementation

## Current scope

This is still MVP scaffolding, but SSH connection now uses `sshj`.

## Build & test (CLI, WSL 기준)

This repository now includes Gradle Wrapper (`gradlew`).

1. Prepare environment variables:

```bash
export JAVA_HOME=/home/h1655/Dev/android-ssh/.local/jdk/jdk-17.0.14+7
export ANDROID_SDK_ROOT=/home/h1655/Dev/android-ssh/.local/android-sdk
export GRADLE_USER_HOME=/home/h1655/Dev/android-ssh/.gradle-local
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
```

2. Build debug APK:

```bash
./gradlew assembleDebug
```

3. Run unit tests:

```bash
./gradlew --no-daemon testDebugUnitTest
```

Output APK path:

- `app/build/outputs/apk/debug/app-debug.apk`

## Run on Android device

1. (WSL2) Windows PowerShell에서 폰을 WSL로 attach:

```powershell
usbipd bind --busid <BUSID>
usbipd attach --wsl --busid <BUSID>
```

2. WSL에서 ADB 연결 확인 (`device` 상태여야 함):

```bash
adb devices -l
```

3. 디버그 앱 설치:

```bash
./gradlew --no-daemon installDebug
```

4. 앱 실행:

```bash
adb shell am start -n com.opencode.sshterminal/.app.MainActivity
```

If `unauthorized` appears, approve the RSA debugging prompt on the phone.
If `no permissions` appears, add a udev rule for your vendor ID and reload rules.

## Next implementation order

1. Replace `SimpleTerminalEngine` with `libvterm`-based engine
2. Implement `KeyRepository` with Android Keystore-backed AEAD encryption
3. Replace plain text fields with dedicated file pickers and progress percentage UI for large transfers

## Important paths

- `app/src/main/java/com/opencode/sshterminal/session/SessionStateMachine.kt`
- `app/src/main/java/com/opencode/sshterminal/ssh/SshClient.kt`
- `app/src/main/java/com/opencode/sshterminal/terminal/TerminalEngine.kt`
- `app/src/main/java/com/opencode/sshterminal/service/SshForegroundService.kt`
- `app/src/main/java/com/opencode/sshterminal/sftp/SshjSftpAdapter.kt`
- `app/src/test/java/com/opencode/sshterminal/sftp/SshjSftpAdapterTest.kt`
