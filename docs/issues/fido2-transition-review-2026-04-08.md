# Security Key SSH 재도입 Review Notes (2026-04-08)

이 문서는 `docs/issues/fido2-transition-design.md` 에 대해 2026-04-08에 수행한
`/autoplan` 검토 결과를 design 문서와 분리해 보관하는 review note다.

## Verdict

- Approved as-is on 2026-04-08
- Framing: U2F->FIDO2 migration이 아니라, current `main` 기준의 FIDO2-first 재도입 검토
- Scope: CEO review + Eng review completed, Design/DX review skipped

## Key Conclusions

1. 이 기능은 active subsystem migration이 아니라 feature reintroduction이다.
2. legacy `securityKey*` 데이터는 silent drop이 아니라 explicit outcome으로 다뤄야 한다.
3. continuity claim은 detection, user-visible outcome, backup/import regression coverage,
   real-device proof가 들어오기 전에는 복구하면 안 된다.
4. rollout, device matrix, observability는 implementation 이후가 아니라 design 단계에서
   먼저 고정해야 한다.

## Follow-up That Landed In This Change Set

- security-key docs re-baselined to current `main`
- legacy compatibility policy added to the design doc
- rollout gate / device matrix / observability minimums added to the design doc
- repository and backup import paths now preserve legacy security-key presence as
  `hasUnsupportedSecurityKeyAuth`
- connection list / import UX now surfaces explicit unsupported-state messaging
- side-by-side `deviceTest` build + connected-test workflow added for real-device testing

## Remaining Non-Review Work

- actual FIDO2 registration/signing reintroduction
- OpenSSH `sk-ecdsa` signing path implementation
- broader device matrix execution beyond the current compatibility test

## Related Files

- Design doc: `docs/issues/fido2-transition-design.md`
- Device test script: `scripts/device-connected-test.sh`
- Smoke install script: `scripts/device-smoke.sh`
