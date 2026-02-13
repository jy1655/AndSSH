# MVP Checklist

## Protocol

- [ ] SSH handshake + auth (public key first)
- [ ] `pty-req` + `shell`
- [ ] `window-change` on resize
- [ ] Non-blocking read/write pipeline

## Security

- [ ] Strict host key verification (first-use accept + mismatch reject)
- [ ] Encrypted private key storage (Keystore-backed AEAD)
- [ ] Clipboard sensitive flag + optional auto-clear

## Runtime

- [ ] Foreground service notification behavior
- [ ] Keepalive and reconnect strategy
- [ ] Doze/background behavior documented in UI settings

## UX

- [ ] Terminal compatibility (`xterm-256color`)
- [ ] Ctrl/Alt/Esc/Tab key row
- [ ] Session tabs
- [ ] SFTP upload/download
