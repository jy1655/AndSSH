# Security Key E2E (historical reference)

이 문서는 AndSSH의 하드웨어 보안키 SSH 검증에 쓰였던 서버측 참고 절차다.
2026-04-08 기준 현재 `main`에는 보안키 등록/인증 경로가 없으므로, 이 문서를
현재 릴리스 검증 체크리스트로 사용하면 안 된다.

## 1. 현재 상태

- 현재 `ConnectionProfile`에는 보안키 관련 필드가 없다.
- 현재 `ConnectRequest`에는 보안키 인증 payload가 없다.
- 현재 `SSHClient.authenticate()`는 비밀번호 또는 파일 기반 개인키 인증만 지원한다.
- `Enroll Security Key`, `Copy authorized_key`, `-Pandssh.enableFido2Poc=true`는 현재 `main` 기준 유효한 검증 단계가 아니다.

## 2. 이 문서를 지금 어떻게 써야 하나

- 현재 `main` 릴리스 검증에는 사용하지 않는다.
- `./scripts/security-key-e2e.sh`는 향후 보안키 SSH 경로를 다시 도입할 때 쓸 서버측 reference harness로만 보관한다.
- 보안키 기능을 되살리는 브랜치에서는 실제 UI 진입점, 로그 태그, 지원 단말 매트릭스에 맞춰 이 문서를 먼저 갱신한 뒤 테스트를 진행한다.

## 3. Reference server harness

로컬 OpenSSH 테스트 서버 시작:

```bash
./scripts/security-key-e2e.sh start
```

상태/접속 정보 확인:

```bash
./scripts/security-key-e2e.sh status
```

출력에서 확인할 값:

- `candidate host`
- `listen port` (기본 `10022`)
- `username`
- `authorized_keys`

서버 로그 확인:

```bash
./scripts/security-key-e2e.sh show-log
```

정리:

```bash
./scripts/security-key-e2e.sh stop
```

## 4. 향후 재도입 브랜치에서 검증해야 할 것

- 앱이 실제로 보안키 등록과 서명 진입점을 노출하는지
- 사용자 취소, unsupported device, Play Services 오류가 명시적으로 surfaced 되는지
- OpenSSH 서버 로그에서 `sk-ecdsa-sha2-nistp256@openssh.com` 타입 수락이 확인되는지
- 문서에 적힌 UI 흐름, 로그 태그, build flag가 그 브랜치의 실제 코드와 일치하는지
