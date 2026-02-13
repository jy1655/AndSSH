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
- GitHub Actions CI: `assembleDebug` + `testDebugUnitTest`
- Terminal abstraction (`TerminalEngine`) with simple in-memory implementation
- Placeholder key repository interface for Keystore+AEAD implementation

## Current scope

This is still MVP scaffolding, but SSH connection now uses `sshj`.

## Build & test (CLI)

This repository now includes Gradle Wrapper (`gradlew`).

1. Prepare environment variables (example paths used in this project):

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

3. Run unit test task:

```bash
./gradlew testDebugUnitTest
```

Output APK path:

- `app/build/outputs/apk/debug/app-debug.apk`

## Next implementation order

1. Replace `SimpleTerminalEngine` with `libvterm`-based engine
2. Implement `KeyRepository` with Android Keystore-backed AEAD encryption
3. Replace plain text fields with dedicated file pickers and progress percentage UI for large transfers

## Important paths

- `app/src/main/java/com/opencode/sshterminal/session/SessionStateMachine.kt`
- `app/src/main/java/com/opencode/sshterminal/ssh/SshClient.kt`
- `app/src/main/java/com/opencode/sshterminal/terminal/TerminalEngine.kt`
- `app/src/main/java/com/opencode/sshterminal/service/SshForegroundService.kt`
