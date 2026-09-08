# Build State

- Status: P5C_LIVE_AUTHORIZED_ATTEMPT / P5C_PILOT_STOPPED_WITH_PROVIDER_ERROR / STOP_PROVIDER_TIMEOUT / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Active authority: V5-SAFE.4.1.3-FULL; canonical plan `EDITORIAL_RECOVERY_V4_18.md`.
- Workspace: D:\App Translate Books\App Translate Books-translation-profile.
- Branch: feature/v4.18.
- Current commit baseline: 68db245 test(editorial): size P5C pilot budget for locked authorities, the implementation/evidence baseline immediately before the recovery-inspection test and live-stop documentation; not self-referential.
- Last validation package used: 4.17-dev.16 / Android versionCode 184, archived and used on device `15e84958`; focused/full validation passed. The device was then restored to the immutable 4.17-dev.1 / code169 baseline by uninstall/reinstall, which removed validation-package data.
- Current phase: P5C exact-binding fake E2E and persistent real-binding setup are complete. One explicitly authorized OpenRouter RAW request was dispatched for the selected binding and stopped with `RETRY_PROVIDER_CALL_FAILED` after 179728 ms; no response arrived, RECONCILE was not called, and no report/receipt was committed. Editorial execution/certification remain disabled.

## Source and authority identity

- P0/P1 checkpoint: aef7da1.
- P2/P3A incoming checkpoint: b5589bf5f2b3b60942841950c5877e6f8281f7ff.
- P4 starting checkpoint: 270759e5589b2e9101c3e1a5a6b84cff12ec2fd3.
- Canonical ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip`; 23,638 bytes; SHA-256 `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`.
- Java control ZIP: `app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip`; 23,418 bytes; SHA-256 `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`.
- Project authority: 9,485 bytes; SHA-256 `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD`.
- Prompt authority: 8,852 bytes; SHA-256 `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F`.
- Workflow authority: 34,917 bytes; SHA-256 `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730`.
- Canonical pack hash: `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d`.

## Trusted profile

- Profile: `com.ml.tblandroidtxt.editorial.engine.safe4.full / 2.0.0`.
- Contract: `safe4.full.three-pass.v1`.
- Receipt schema: `safe4.full.receipt.v1`.
- Profile resource SHA-256: `1b2db011d59f3e2ef4349aeb0daa9c54a19b7efd1e2ca6886bc29b56e4690d62`.
- Canonical profile hash: `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21`.
- Machine contract fingerprint: `a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3`.
- Implemented P3B capabilities: exact 11/11 with owner, positive/negative test, fingerprint and source commit.
- Explicitly missing P3B capabilities: none.
- Profile sourceCommit: `3156835d1cc6b723d7932709224bf626dc7a1747`.
- executionEnabled: false; automatic replacement and project rebind: false.

## Completed evidence

- P2A/P2B and P3A GAP-012 complete: canonical/control import, immutable byte readback, idempotent re-import, 4.1.4 side-by-side and security negatives pass.
- P3B complete: P01-P09 `9/9`, G1-G24 `24/24`, typed stop/recovery, PRESERVE_DRAFT, receipt/ledger/diff/QA/release validators and trusted profile acceptance.
- P5 dry-run boundary: `EditorialP5PilotExecutionBoundaryTest` `15/15`; bounded authorization, exact binding checks, preflight-before-provider, phase projection, typed truncation/repair/recovery, local ledger/diff/receipt validation, token budget, idempotency and atomic-store failure are covered with an injected fake provider. No real provider or chapter was used.
- P5C app-bound fake E2E: additive SQLite v20 `editorial_p5c_attempts`, exact persisted P4 selector/binding resolution, RAW predecessor readback, RECONCILE, redacted report/receipt commit, database reopen and idempotent replay are covered by `EditorialP5CExactBindingFakeE2EInstrumentedTest`.
- P4 characterization documented the legacy hard-coded project owner and the v18 tuple/source identity boundary. Additive v19 P4 tables were introduced only after that failing persistence evidence.
- P4 selection/binding: explicit exact pack selection, immutable tuple, app-computed source identities, atomic project/revision/scope/declaration/binding transaction, idempotent retry and collision rejection.
- P4 resume: side-by-side canonical 4.1.3/synthetic 4.1.4, exact DB close/reopen readback, activity recreation UI proof, two-invocation host process-stop proof and stale-chain fail-closed checks.
- P4 post-closure correction: `EditorialP4BindingTransactionService` and the P4 UI now write only the contract vocabulary `NORMAL_FOUR_SOURCE`/`ALTERNATE_EXPLICIT`; the prior `NORMAL`/`ALTERNATE` mismatch was caught by a new canonical-mode assertion and fixed in commit `364faa4`.
- P4 focused device evidence on 15e84958: importer `13/13`, P1 `7/7`, P2 `3/3`, P3B trusted profile `1/1`, runtime wiring `5/5`, P4 binding `4/4`, process-stop preparation/resume `1/1 + 1/1`, UI recreation `3/3`.
- Focused P5C device classes: `22/22 PASS`; this includes P1/P2/P3B/P4/P5C exact-binding fake coverage.
- Full device instrumentation: `117/117 PASS`, `0` failures; real API remained opt-in/skipped. Five tests were added by P5C relative to the previous `112` total. The first run also exposed seven stale v19 assertions; only test expectations were corrected to current additive schema v20 in `744349e` and `61ba760`.
- Host engine suite: `178/178 PASS` (baseline `163`; P5 added `15` tests).
- App unit suite: `211/211 PASS` per debug/release/benchmark variant; aggregate `:app:test` `633/633 PASS`.
- P5C validation APK: `artifacts/builds/v4.17-dev.13/build-20260904-202447/TranslateBooks-v4.17-dev.13-code181.apk`, SHA-256 `807D2E0C28BF3F486845562FFEE05B039FBA6D09AFDA566618C6C892979CD0F2`; matching backup archive.
- External qualification `TESTS/test_full_release.ps1`: `306 PASS / 0 FAIL`.
- Profile and canonical pack/authority verification: PASS. Fake provider calls: `2` in isolated app acceptance. The authorized live boundary dispatched one RAW request and received no response/usage receipt; billing is unknown. Code169 baseline APK SHA-256 is `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1` in both artifact and backup; the device is restored to that baseline. The uninstall/reinstall procedure removed validation-package data because `pm clear` is not accepted on this device.
- `git diff --check`: PASS at closure.

- Device setup preparation: canonical 4.1.3 was imported through the production pack picker on code183 and reported `DATA_COMPATIBLE`; the persistent setup test pinned the normalized app-import bytes and read back the exact P4 tuple for chapter `001`. Source files remain outside Git under the user-provided MERCEDES VOL 4 folder.
- Code184 focused device regression: setup `1/1`, P5C fake E2E `5/5`, P1 `7/7`, P2 `3/3`, P3B `1/1`, P4 binding `4/4`, process-death `2/2`; full instrumentation `118/118 PASS` with the opt-in real API test skipped. Host engine `179/179 PASS`; app unit `214/214 PASS` per debug/release/benchmark variant, aggregate `642/642 PASS`.
- Authorized live boundary: one OpenRouter RAW request, `providerCalls=1`, no automatic retry/repair, no response or usage receipt, typed `RETRY_PROVIDER_CALL_FAILED`, and durable `RECOVERY_REQUIRED`; recovery inspection `1/1` confirmed zero report/receipt bytes. Provider billing/acceptance is unknown and no new live call is authorized by this record.

## Validation artifact

- Latest validation APK: `artifacts/builds/v4.17-dev.13/build-20260904-202447/TranslateBooks-v4.17-dev.13-code181.apk`.
- Backup mirror: `backup/builds/v4.17-dev.13/build-20260904-202447/TranslateBooks-v4.17-dev.13-code181.apk`.
- APK SHA-256: `807D2E0C28BF3F486845562FFEE05B039FBA6D09AFDA566618C6C892979CD0F2`.
- Build event source snapshot: `61ba7602005522a8c93f2b4335fe6bea0214a367`; it contains the P5C fake E2E implementation/test baseline.
- This is a validation APK, not a V4.18 release build.
- Historical P4 correction validation remains immutable and is recorded in
  `docs/P4_VALIDATION_REPORT.md` as code176; it is not the current validation
  artifact.

## Allowed-change guard

P4 production changes are limited to the P4 binding/selection/service/DAO
owners, the additive v19 migration, read-only project projection/setup UI and
the engine P4 value types. P5C production changes are limited to the additive
v20 durable attempt owner, app-bound exact-binding coordinator, the existing
engine pilot boundary and the bounded OpenRouter adapter/request context. No
global activation, certification path, provider framework, UI activation or
legacy workflow rewrite was added. Test changes are AndroidTest/engine-test
plus stale current-schema assertions only.
No authority byte, canonical ZIP, trusted profile resource, database schema
beyond the already-approved additive v20 attempt owner, project binding, build
metadata or legacy workflow was changed during the live attempt. The original
workspace `D:\App Translate Books` was not modified.

## Known limitations

- P4 creates setup metadata only. It does not run L1/L2/L3, call a provider,
  create a model request, certify a pack or open Run/Start.
- The setup UI collects explicit initial source text for the metadata fixture;
  this is not source certification and is not an execution UI.
- `DATA_COMPATIBLE`, selectable and `PILOT_SETUP_READY` do not mean runnable.
- Bootstrap profile v1 remains loadable and non-executable.
- The single authorized RAW request stopped at the provider boundary with no response. External call state and billing remain unknown; do not retry automatically. A future retry requires provider-account inspection, explicit recovery handling and a new authorization.
- P5C proves app-bound fake attempt persistence and idempotent RAW→RECONCILE replay, but does not prove a live response, real token/cost usage, live cancellation/process-death behavior, or a real chapter `REPORT_L1`/receipt commit. RECONCILE and final L1 acceptance remain unproven.
- Device was restored to immutable `4.17-dev.1/code169` after validation by uninstall/reinstall; validation-package data was removed because `pm clear` was rejected. The latest validation artifact remains code184 in artifacts/backup.

## Next step

Inspect the OpenRouter account for the unknown status of the one dispatched
request before considering any retry. If a retry is explicitly authorized,
create a new exact-phase authorization and rerun only after the external-call
state is resolved; never reuse the consumed authorization or automatically
call RECONCILE. Keep `EXECUTION_DISABLED / NOT_CERTIFIED /
NOT_GLOBALLY_RUNNABLE`; do not open L2/L3, auto-activate, auto-rebind or
certify from fake evidence or from this provider stop.

This file is current-only; Git history preserves prior state.
