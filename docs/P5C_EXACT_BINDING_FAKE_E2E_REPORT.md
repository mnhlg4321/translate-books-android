# P5C.2 — Exact-binding fake E2E report

Ngày ghi nhận: `2026-09-07` (+07:00)

## Kết luận

```text
P5C_EXACT_BINDING_FAKE_E2E_PASS
P5_DRY_RUN_ONLY
LIVE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_RUNNABLE
```

Đã chạy một vertical slice fake qua production-bound app path:

```text
persisted P4 binding
  -> L1_RAW_DISCOVERY
  -> durable predecessor/readback
  -> L1_RECONCILE
  -> redacted REPORT_L1 + receipt
  -> SQLite atomic attempt state
  -> close/reopen + idempotent replay
```

Kết quả này không phải live pilot, không chứng nhận pack và không mở
Editorial execution.

## Baseline và identity

| Hạng mục | Giá trị |
|---|---|
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Branch | `feature/v4.18` |
| P5C implementation/test baseline trước report | `61ba7602005522a8c93f2b4335fe6bea0214a367` |
| Canonical ZIP | `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java control ZIP | `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Profile resource | `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Project authority | `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD` |
| Prompt authority | `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F` |
| Workflow authority | `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730` |

## Production owners added for the fake boundary

- `EditorialP5CAttemptStore` là owner SQLite additive cho một attempt exact,
  ghi identity, response identity, redacted report/receipt bytes, metrics và
  recovery disposition; không ghi chapter bytes, request body hoặc provider
  response.
- `EditorialP5CExactBindingExecution` resolve selector bằng persisted P4 DAO,
  xác minh pack immutable storage, profile/evaluation và source hashes trước
  khi gọi injected provider.
- `EditorialMigrationSpec`/`TranslationRepository` thêm schema v20 additive
  cho `editorial_p5c_attempts`; không backfill, không sửa schema cũ.
- Engine `EditorialP5PilotExecution` giữ preflight-before-provider, exact phase,
  bounded call policy, local validation, one schema-only repair và atomic store
  handoff.

Không có HTTP adapter, provider credential wiring, global activation,
certification state, UI Run path hay L2/L3 path được thêm.

## Acceptance matrix

| Case | Evidence | Result |
|---|---|---|
| Durable attempt characterization trước fix | `0b3f947bd4cfe9e91b7a23bd8b04aa55952a801c`; device test đỏ vì thiếu `editorial_p5c_attempts` | Red đúng kỳ vọng |
| Additive durable owner | `77a905454780ba4956d22e69aeb1d34338b5d11d` | Pass |
| Exact binding RAW → RECONCILE | `EditorialP5CExactBindingFakeE2EInstrumentedTest.exactBindingRunsRawThenReconcileAndReloadsIdempotently` | Pass |
| Durable owner characterization | `v20ProvidesDurableAttemptOwnerAfterCharacterization` | Pass |
| Incomplete auth | `incompleteAuthorizationStopsBeforeProvider` | Typed stop; `0` calls; `0` rows |
| Source drift | `sourceDriftStopsBeforeProviderAndLeavesNoAttempt` | `STOP_SOURCE_DRIFT`; `0` calls; `0` rows |
| Store reopen/recovery | `claimedAttemptIsInFlightAfterStoreReopenAndRecoveryIsRetryable` | One row; `IN_FLIGHT`, then controlled recovery |
| Duplicate after DB reopen | Same exact E2E test | `ALREADY_COMMITTED`; `0` second calls |
| Focused P1/P2/P3B/P4/P5C | device `22/22` | Pass |
| Full instrumentation | device `117/117` | Pass; real API remained opt-in/skipped |

The test fixture imports the canonical ZIP and creates a persisted P4 binding.
Because the legacy repository intentionally rejects chapter creation on a
P4-bound project, the test inserts only a synthetic chapter/assets fixture
directly into its isolated test database. This is test setup, not a second
production import or binding path.

## Phase projection and persistence proof

- RAW request exposed exactly `RAW` and `GLOSSARY`.
- RAW request did not expose `DRAFT`, `PRONOUN` or Pair Context.
- RECONCILE request exposed `RAW`, `DRAFT`, `GLOSSARY` and `PRONOUN`.
- The optional Pair Context was absent and did not block.
- Both phases returned locally validated `COMMITTED` results.
- Two durable rows were read back as `COMMITTED`.
- Persisted report and receipt bytes were compared byte-for-byte with the
  committed result.
- Metrics JSON was checked not to contain the fake response body.
- After database close/reopen, exact replay returned `ALREADY_COMMITTED` and
  did not call the provider.
- Every returned app result kept `executionAllowed=false` and
  `certificationState=NOT_CERTIFIED`.

## Validation commands

```powershell
& 'D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\TESTS\test_full_release.ps1'
# PASS=306 FAIL=0 OLD_WORDS=5308 NEW_WORDS=7050 EXACT_RETAINED=223/311

.\gradlew.bat :editorial-engine:test :app:test --no-daemon
# engine 178/178; app 211/211 per debug/release/benchmark variant
# aggregate app :app:test = 633/633; failures=0
```

The device validation build was created with `scripts/build-and-save.ps1` as
`4.17-dev.13/code181`, archived in both `artifacts/builds` and `backup/builds`:

```text
SHA-256 = 807D2E0C28BF3F486845562FFEE05B039FBA6D09AFDA566618C6C892979CD0F2
```

The full device run used `15e84958` and completed `117/117` with zero
failures after the device's notification-permission dialog was dismissed.
That UI precondition is recorded as device setup evidence; it is not a
production change. After validation, the package was uninstalled and the
immutable `4.17-dev.1/code169` APK was reinstalled because `pm clear` is
rejected on this device. Readback verified `versionCode=169` and
`versionName=4.17-dev.1`; this restore removed validation-package data.

## Privacy and secret scan

The repository scan found no high-confidence credential, bearer token, private
key, request body or chapter payload. The only matching text was the existing
UI export placeholder `OPENROUTER_API_KEY=sk-or-v1-...`; it is visibly an
ellipsis placeholder, not a credential. No new secret or provider data was
added by P5C.

## Scope decision

The fake app-bound attempt owner is sufficient evidence for P5C.2. It is not
evidence for a real external call, real chapter semantics, live cancellation,
unknown external-call state, provider cost, or certification. Those remain
authorization-gated follow-up work.
