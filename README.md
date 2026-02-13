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
- Terminal abstraction (`TerminalEngine`) with simple in-memory implementation
- Placeholder key repository interface for Keystore+AEAD implementation

## Current scope

This is still MVP scaffolding, but SSH connection now uses `sshj`.

## Next implementation order

1. Replace `SimpleTerminalEngine` with `libvterm`-based engine
2. Implement `KeyRepository` with Android Keystore-backed AEAD encryption
3. Improve SFTP UX (progress indicator, overwrite prompts, permission error guidance)

## Important paths

- `app/src/main/java/com/opencode/sshterminal/session/SessionStateMachine.kt`
- `app/src/main/java/com/opencode/sshterminal/ssh/SshClient.kt`
- `app/src/main/java/com/opencode/sshterminal/terminal/TerminalEngine.kt`
- `app/src/main/java/com/opencode/sshterminal/service/SshForegroundService.kt`
