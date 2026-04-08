<!-- /autoplan restore point: /Users/jy/.gstack/projects/jy1655-AndSSH/main-autoplan-restore-20260408-102308.md -->
# Issue: Security Key SSH 재도입 설계 (FIDO2-first)

## 배경

- 현재 `main`에는 U2F/FIDO2 등록 코드와 보안키 SSH 인증 경로가 없다.
- 저장소에는 제거된 U2F/FIDO2 구현을 기준으로 작성된 문서와 이슈 흔적이 일부 남아 있다.
- 향후 이 기능이 돌아온다면, active subsystem migration이 아니라 current `main` 기준의 FIDO2-first 재도입으로 봐야 한다.
- OpenSSH `sk-ecdsa-sha2-nistp256@openssh.com` 호환성은 여전히 핵심 프로토콜 목표다.

## 목표

- AndSSH가 보안키 SSH 인증을 다시 제공할지 product decision을 분명히 한다.
- 재도입한다면 current `main`에 맞는 FIDO2-first 설계를 정의한다.
- legacy `securityKey*` 데이터와 SSH 인증 경로의 호환 전략을 명시한다.

## 범위

- 포함:
  - current `main` 기준 data model, persistence, backup/import, request model, SSH signing boundary 재설계
  - Android FIDO2 등록(credential create) 및 서명(assertion) 후보 API 분석
  - legacy `securityKey*` 저장 데이터의 detect / migrate / reject 전략
  - OpenSSH `sk-ecdsa` 인증에 필요한 데이터 추출/변환 경로 정의
  - rollout gate, device matrix, observability 요구사항 정의
- 제외:
  - deprecated U2F enrollment를 shipped path로 되살리는 일
  - 즉시 코드 구현/릴리스
  - 지원하지 않는 구형 단말에 대한 별도 우회 구현

## 현재 `main` 상태 (2026-04-08)

- `ConnectionProfile`에는 보안키 관련 필드가 없다.
- `ConnectRequest`에는 보안키 인증 payload가 없다.
- `SSHClient.authenticate()`는 비밀번호 또는 파일 기반 개인키만 처리한다.
- historical `securityKey*` 데이터는 current runtime contract의 일부가 아니며, continuity를 주장하려면 별도의 compatibility work가 필요하다.
- `docs/security-key-e2e.md` 등 일부 문서는 historical reference로 취급해야 한다.

## 설계 결정이 필요한 항목

1. 등록 데이터 모델:
   - current `ConnectionProfile` 확장 vs auth 전용 구조 분리
2. legacy compatibility 처리:
   - historical `securityKey*` 필드를 migrate할지, reject할지, explicit unsupported로 남길지
3. 서명 데이터 파싱:
   - FIDO2 assertion(authData/signature)을 OpenSSH `sk-ecdsa` 포맷으로 어떻게 매핑할지
4. 호환성 정책:
   - public docs, backups, stored profiles에서 continuity claim을 어디까지 허용할지
   - 재도입 시 feature flag와 rollout gate를 어떻게 둘지
5. 장애 대응:
   - 단말별 FIDO2 API 미지원/오류를 어떻게 surfaced/logged 할지

## 작업 항목

1. README, security-key docs, regression checklist를 current `main` 기준으로 re-baseline
2. legacy `securityKey*` profile / backup compatibility 정책 정의
3. FIDO2-first auth data model과 OpenSSH `sk-ecdsa` signing boundary 정의
4. rollout flag, unsupported-device handling, observability 요구사항 설계
5. 실기기 매트릭스(안드로이드 버전/벤더/보안키 종류) 호환성 점검 계획 수립

## 완료 조건

- 설계 문서가 current `main`과 모순되지 않는다.
- 데이터 모델/호환성/롤백 전략이 명시된다.
- public docs가 unsupported behavior를 약속하지 않는다.
- 재도입 전에 필요한 device matrix / rollout gate / observability 요구사항이 문서화된다.

## 리스크

- OpenSSH `sk-ecdsa` 요구 포맷과 Android FIDO2 응답 포맷 간 차이
- historical security-key data가 silently dropped 되며 product trust 문제가 생길 수 있음
- 단말별 Play Services 버전 편차에 따른 동작 불일치

## 결정된 legacy compatibility 정책 (2026-04-08)

1. Detection boundary
   - historical 보안키 데이터는 current model decode 이전의 raw JSON 단계에서 감지한다.
   - 최소 marker field는 `securityKeyApplication`, `securityKeyHandleBase64`, `securityKeyPublicKeyBase64`, `securityKeyFlags`로 본다.
   - 이 감지는 local profile restore와 encrypted backup import 모두에 동일하게 적용한다.

2. Current-release truth policy
   - current `main`은 legacy security-key continuity를 약속하지 않는다.
   - README, 체크리스트, 릴리스 문구에서 "기존 보안키 연결이 계속 동작한다"는 표현을 금지한다.

3. User-visible behavior when legacy data is found
   - future compatibility patch가 들어오기 전까지는 legacy security-key-only profile을 normal password/private-key profile처럼 보이게 두지 않는다.
   - explicit outcome만 허용한다: `migrateable`, `unsupported`, `import rejected`.
   - silent downgrade, silent drop, implicit fallback은 금지한다.

4. Migration posture
   - 보안키 auth state를 password/private-key shape로 자동 변환하지 않는다.
   - migrator가 생기기 전에는 raw legacy payload를 dedicated compatibility structure로 보존하거나, import/read를 명시적으로 거부한다.
   - partial import로 profile만 살리고 auth material만 버리는 동작은 금지한다.

5. Release gate for any continuity claim
   - detection
   - explicit user-visible outcome
   - backup/import regression coverage
   - one real-device E2E proof
   위 네 가지가 동시에 들어오기 전에는 continuity claim을 복구하지 않는다.

## 결정된 rollout gate / device matrix (2026-04-08)

### Gate 0. Design complete

- docs가 current `main` truth와 일치한다.
- legacy compatibility 정책이 명시된다.
- auth boundary, rollout, observability 요구사항이 문서화된다.

### Gate 1. Internal build only

- feature flag default `off`
- legacy raw JSON detection unit test 추가
- FIDO2 assertion -> OpenSSH `sk-ecdsa` signature vector test 추가
- unsupported device / cancelled prompt / malformed payload error path 정의

### Gate 2. Manual device proof

- 최소 1개 실기기에서 happy path 성공
- 최소 1개 실패 경로(unsupported device 또는 user cancel)에서 explicit diagnostic 확보
- server-side acceptance는 `./scripts/security-key-e2e.sh` harness 또는 동등한 OpenSSH 검증으로 확인

### Gate 3. Pre-public matrix

- Android API lower bucket: `minSdk 26` 계열에서 1종 이상
- Android API current bucket: API 33-35 계열에서 1종 이상
- OEM / Play Services 조합 2종 이상
- 실제 지원하려는 security key transport 2종 이상을 검증하거나, 1종만 지원한다면 문서에 명시
- historical profile / backup import가 explicit outcome을 내는지 검증

### Rollback posture

- feature flag off로 즉시 차단 가능해야 한다.
- stored profile data는 rollback 중에도 손실 없이 유지되어야 한다.
- docs / release notes / support guidance를 같은 change set에서 되돌릴 수 있어야 한다.

## Observability minimums

- log stages:
  - `legacy_profile_detected`
  - `fido_register_start`
  - `fido_register_result`
  - `fido_sign_start`
  - `fido_sign_result`
  - `ssh_publickey_result`
- failure context:
  - device model
  - Android API level
  - Google Play Services version
  - RP ID or application mapping choice
  - user-cancel vs unsupported-device vs parse-failure 구분
- exported diagnostics에는 위 분류를 담되, raw secret material은 포함하지 않는다.

## Review History

- Review summary and approval notes were split into
  `docs/issues/fido2-transition-review-2026-04-08.md`.
- This file now stays design-focused: problem framing, policy, rollout gate, and observability.
