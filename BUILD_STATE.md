# Build State

- Status: P5D_DOCUMENTATION_BASELINE_CONSISTENT / P5D_EXTERNAL_AUDIT_COMPLETE / P5D_LIFECYCLE_HARDENING_PASS / P5D_REGRESSION_PASS / P5D_DEADLINE_BODY_READ_HARDENING_PASS / END_TO_END_DEADLINE_VERIFIED / STALLED_BODY_RECOVERY_VERIFIED / PROCESS_RESTART_RECOVERY_VERIFIED / NO_LATE_COMMIT / NO_AUTOMATIC_REDISPATCH / PILOT_DATA_PRESERVED / RAW_RETRY_READY_FOR_NEW_AUTHORIZATION / TRANSPORT_AND_LIFECYCLE_LIVE_VERIFIED / RAW_OUTPUT_TRUNCATION_CONFIRMED / RAW_PREDECESSOR_REQUIRED / OUTPUT_BUDGET_ALIGNMENT_PASS / RECOVERY_HISTORY_PRESERVED / RAW_ACCEPTANCE_AUTHORIZATION_APPROVED / RAW_ACCEPTANCE_ATTEMPTED / RAW_ACCEPTANCE_STOPPED_AT_DEADLINE / RAW_ACCEPTANCE_RECOVERY_CLOSED / EXTERNAL_STATE_REMAINS_UNKNOWN / RAW_ACCEPTANCE_INCOMPLETE / P6_NOT_READY / HISTORICAL_CANCELLATION_CAUSE_UNRESOLVED / RAW_DIAGNOSTIC_ATTEMPT_COMPLETE / RAW_DIAGNOSTIC_STOPPED_RETRY_OUTPUT_TRUNCATED / RAW_PREDECESSOR_NOT_COMMITTED / VOL5_RECOVERY_REQUIRED / EXTERNAL_CONFIRMED_CANCELLED / P5D_PREFLIGHT_PROVIDER_CALLS_0 / P5D_CURRENT_HARDENING_PROVIDER_CALLS_0 / P5D_LIVE_PROVIDER_CALLS_1 / P5D_RAW_ACCEPTANCE_PROVIDER_CALLS_1 / NEW_RAW_AUTHORIZATION_REQUIRED / RECONCILE_AUTHORIZATION_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Active authority: V5-SAFE.4.1.3-FULL; canonical plan `EDITORIAL_RECOVERY_V4_18.md`.
- Workspace: D:\App Translate Books\App Translate Books-translation-profile.
- Branch: feature/v4.18.
- Current commit baseline: f01f65e7dce070aed2398bacecdb70ed4e24e8f2, the implementation/test baseline immediately before this documentation snapshot commit; not self-referential.
- Last validation package used: production APK 4.17-dev.28 / Android versionCode 196, archived in artifacts and backup with SHA-256 `85345086FBD76FA78133EE54741CA7631EBA91EB4761401080EC10BA1D35042A` and installed on device `15e84958` with `adb install -r`, preserving package data. The final focused test APK has SHA-256 `292F30A50302423E695571BB28E95514504F06F174361762919C35FE6D1704DE`; no build metadata, authority or canonical pack/profile resource changed.
- Current phase: the authorized VOL5 attempts remain fail-closed: the earlier diagnostic stopped at `RETRY_OUTPUT_TRUNCATED`, and the later acceptance attempt is `RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT` with external state unknown, no RAW predecessor, report or receipt. P5D deadline/body-read hardening now passes locally: code196 `executeRaw()` stops a stalled body at the 300-second attempt deadline, persists typed recovery and survives readback; the server observed one request and no host force-stop was used. The 455-byte PRONOUN transport file contains a UTF-8 BOM; after the existing app-owned removal it is exactly the pinned 452-byte/hash `4947FF91…20686`. RECONCILE remains unauthorized and Editorial execution/certification remain disabled.

## Source and authority identity

- P0/P1 checkpoint: aef7da1.
- P2/P3A incoming checkpoint: b5589bf5f2b3b60942841950c5877e6f8281f7ff.
- P4 starting checkpoint: 270759e5589b2e9101c1e3a5a6b84cff12ec2fd3.
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
- Previous P5C validation APK: `artifacts/builds/v4.17-dev.13/build-20260904-202447/TranslateBooks-v4.17-dev.13-code181.apk`, SHA-256 `807D2E0C28BF3F486845562FFEE05B039FBA6D09AFDA566618C6C892979CD0F2`; retained as historical evidence with matching backup archive.
- External qualification `TESTS/test_full_release.ps1`: `306 PASS / 0 FAIL`.
- Profile and canonical pack/authority verification: PASS. Fake provider calls: `2` in isolated app acceptance. The authorized live boundary dispatched one RAW request; the app received no usable response/usage receipt, while authenticated OpenRouter metadata records one matching generation as `cancelled` with displayed usage cost `$0.00366`. Code169 baseline APK SHA-256 is `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1` in both artifact and backup; the device is restored to that baseline. The uninstall/reinstall procedure removed validation-package data because `pm clear` is not accepted on this device.
- `git diff --check`: PASS at closure.

- VOL5 RAW gate evidence: `docs/P5D_VOL5_RAW_AUTHORIZATION_BLOCKED.md`; the earlier missing-key invocation remains historical and made `providerCalls=0`. After the key was saved, setup instrumentation again passed `1/1`; one exact RAW request for selector `p5d-raw-mercedes-vol5-001` was dispatched. Durable readback is `RECOVERY_REQUIRED`, with no response identity, report bytes or receipt bytes; OpenRouter confirms generation `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` as `cancelled`, 23,674/90 tokens and displayed cost `$0.00484`. See `docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md`; no RECONCILE call occurred.

- Device setup preparation: canonical 4.1.3 was imported through the production pack picker on code183 and reported `DATA_COMPATIBLE`; the persistent setup test pinned the normalized app-import bytes and read back the exact P4 tuple for chapter `001`. Source files remain outside Git under the user-provided MERCEDES VOL 4 folder.
- Code184 focused device regression: setup `1/1`, P5C fake E2E `5/5`, P1 `7/7`, P2 `3/3`, P3B `1/1`, P4 binding `4/4`, process-death `2/2`; full instrumentation `118/118 PASS` with the opt-in real API test skipped. Host engine `179/179 PASS`; app unit `214/214 PASS` per debug/release/benchmark variant, aggregate `642/642 PASS`.
- Authorized live boundary: one OpenRouter RAW request, `providerCalls=1`, no automatic retry/repair, no usable app response/usage receipt, typed `RETRY_PROVIDER_CALL_FAILED`, and durable `RECOVERY_REQUIRED`; recovery inspection `1/1` confirmed zero report/receipt bytes. The authenticated provider audit found generation `gen-1788877749-P8b2hBo1TWuduENuKbQ3` with finish `cancelled`, `17,808/84` tokens and displayed cost `$0.00366`; a new retry is eligible only after new authorization and explicit duplicate/billing-risk acknowledgement.
- P5D.1/P5D.2 provider audit: authenticated OpenRouter Logs metadata matched the old app/model/time/request-size tuple and classified the external generation as `EXTERNAL_CONFIRMED_CANCELLED`; I/O logging remained disabled and P5D provider calls remain `0`.
- P5D.3/P5D.4: additive v21 lifecycle, authorization-receipt and reconciliation owners; typed provider-failure taxonomy; non-reclaimable `RECOVERY_REQUIRED` gate; and redacted timing/generation metadata passed focused tests. Code186 device XML reports `124` test methods, `0` failures, `0` errors and `4` approved skips; engine `180/180`; app aggregate `645/645`.
- P5D code189 local harness closure: `EditorialP5CExactBindingFakeE2EInstrumentedTest` `13/13 PASS`; immediate, 11-second delayed-with-legacy-cancel and 11-second delayed-without-legacy-cancel `1/1` each; schema/migration device classes `41/41`; full instrumentation `130 tests, 0 failures`; engine/app debug unit XML `396/396`; external qualification `306/306`; no provider call. Detailed evidence: `docs/P5D_LOCAL_HTTP_HARNESS_REPORT.md`.
- P5D code189 DB readback: schema v22; historical VOL5 attempt remains `RECOVERY_REQUIRED`, prior authorization is consumed, response/report/receipt are absent, lifecycle and local reconciliation rows are absent for the historical attempt. RAW/DRAFT/GLOSSARY bytes match the binding; PRONOUN is 455 bytes/hash `63E79EEB…1A49C` raw with BOM and 452 bytes/hash `4947FF91…20686` after the existing BOM removal, so the semantic source identity matches.
- P5D controlled diagnostic preflight rerun: code189 `EditorialP5CExactBindingFakeE2EInstrumentedTest` `13/13 PASS`, live recovery inspection `1/1 PASS`, and engine `EditorialP5PilotExecutionBoundaryTest` `16/16 PASS`; provider calls `0`, pilot DB unchanged. See `docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`.
- The approved authorization `P5D-VOL5-RAW-DIAGNOSTIC-20260909-01` was consumed once for the exact VOL5/chapter001 binding. One OpenRouter RAW primary call returned HTTP `200` with complete transport but ended at `2,048` output tokens (`finish=length`); app metrics were `20,327/2,048/22,375` tokens, reported cost `$0.0075392`, latency `20,590 ms`, schema/receipt invalid. Durable readback is `RECOVERY_REQUIRED` with lifecycle `RESPONSE_BODY_COMPLETE`, no response identity and zero report/receipt bytes. Full redacted evidence: `docs/P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md`.
- Output-budget alignment and recovery-history hardening are complete without a provider call: the prior `4,096 → 2,048` clamp was reproduced test-first, then removed so authorization/request/HTTP use the exact cap; v23 adds append-only reconciliation history without replacing the immutable primary row. Code191 validation focused device evidence is recorded in `docs/P5D_RAW_ACCEPTANCE_PREFLIGHT.md`.
- Code191 focused validation: VOL5 v23 recovery readback `1/1`, fake E2E `14/14`, schema/migration `41/41`, importer/P1/P2 `23/23`, P3B/P4 `5/5`; host engine `181/181`, app all unit variants `657/657`, external qualification `306/306`; provider calls for alignment/preflight `0`.
- RAW acceptance attempt evidence: authorization `P5D-VOL5-RAW-ACCEPTANCE-20260909-01` was consumed for exactly one dispatch. The final focused test APK (`CE5E29CABA15D08D3C6C2A032B961171C11428E28CC41A302792CA6D904CDCC4`) ran on `15e84958`; the process did not reach terminal result by the five-minute cap. Redacted readback retained lifecycle `RESPONSE_HEADERS_RECEIVED`, HTTP `200`, request bytes `85,068`, generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8`, and zero report/receipt bytes. Owner recovery closure changed `CLAIMED` to `RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT`; provider usage/cost and external completion remain unknown. See `docs/P5D_RAW_ACCEPTANCE_ATTEMPT_REPORT.md`.

- P5D deadline/body-read/recovery closure: production fix `d39bca7dcd16a64d6a97006d71e17f994653c065` gives the RAW adapter a scoped monotonic deadline, removes the legacy `+30s` grace from bounded pilot calls, keeps RAW cancellation separate from legacy global cancellation, bounds one-pass response-body buffering and persists redacted progress bytes. Schema v24 adds only `response_body_bytes`; stale claims recover fail-closed without redispatch. Code196 on device `15e84958` passed the focused class's prior `17/17`, short stalled-body `1/1` (`2.069s`) and five-minute stalled-body `1/1` (`301.501s`) with server request-count `1`; the five-minute run was not host force-stopped. Isolated P1-P5C/migration regression was `92/92`, VOL5 readback `1/1`, engine `183/183`, app unit `222/222` per debug/release/benchmark variant, external qualification `306/306`, and high-confidence secret scan found no credential. This validation made `0` provider/API calls. Details: `docs/P5D_LOCAL_HTTP_HARNESS_REPORT.md` and `docs/P5D_VALIDATION_REPORT.md`.

## Validation artifact

- Latest validation APK: `artifacts/builds/v4.17-dev.28/build-20260910-190649/TranslateBooks-v4.17-dev.28-code196.apk`.
- Backup mirror: `backup/builds/v4.17-dev.28/build-20260910-190649/TranslateBooks-v4.17-dev.28-code196.apk`.
- APK SHA-256: `85345086FBD76FA78133EE54741CA7631EBA91EB4761401080EC10BA1D35042A`.
- Build event source snapshot: `d39bca7dcd16a64d6a97006d71e17f994653c065`; code196 is validation-only and was installed with `adb install -r`.
- Test APK SHA-256 for the current focused validation runner: `292F30A50302423E695571BB28E95514504F06F174361762919C35FE6D1704DE`.
- Code186 remains immutable historical evidence for the prior lifecycle baseline; it is not the current validation artifact.
- Code184 remains immutable historical evidence for the pre-P5D live attempt; it is not the current validation artifact.
- This is a validation APK, not a V4.18 release build.
- Historical P4 correction validation remains immutable and is recorded in
  `docs/P4_VALIDATION_REPORT.md` as code176; it is not the current validation
  artifact.

## Allowed-change guard

P4 production changes are limited to the P4 binding/selection/service/DAO
owners, the additive v19 migration, read-only project projection/setup UI and
the engine P4 value types. P5C production changes are limited to the additive
v20 durable attempt owner, app-bound exact-binding coordinator, the existing
engine pilot boundary and the bounded OpenRouter adapter/request context. P5D
production changes are limited to the additive v21 lifecycle, authorization
receipt, reconciliation and typed provider-failure owners, v22 content-type and
cancellation-source fields, v23 append-only reconciliation history, v24
response-body progress bytes, scoped monotonic deadline/body-read handling and
exact output-budget/response acceptance propagation through existing pilot
owners. No global activation,
certification path, provider framework, UI activation or legacy workflow rewrite
was added. Test changes are AndroidTest/engine-test plus stale current-schema
assertions only.
No authority byte, canonical ZIP, trusted profile resource, build metadata or
legacy workflow was changed. The original workspace `D:\App Translate Books`
was not modified.

## Known limitations

- P4 creates setup metadata only. It does not run L1/L2/L3, call a provider,
  create a model request, certify a pack or open Run/Start.
- The setup UI collects explicit initial source text for the metadata fixture;
  this is not source certification and is not an execution UI.
- `DATA_COMPATIBLE`, selectable and `PILOT_SETUP_READY` do not mean runnable.
- Bootstrap profile v1 remains loadable and non-executable.
- The historical VOL5 RAW request was provider-confirmed `cancelled` at displayed cost `$0.00484`. The subsequent diagnostic attempt returned complete HTTP `200` transport but stopped at `max_output_tokens`; the app typed `RETRY_OUTPUT_TRUNCATED` and committed no predecessor/report/receipt. No automatic retry is allowed; a new exact-phase authorization would be required for another call.
- P5C proves app-bound fake attempt persistence and idempotent RAW→RECONCILE replay, but does not prove a live response, real token/cost usage, live cancellation/process-death behavior, or a real chapter `REPORT_L1`/receipt commit. RECONCILE and final L1 acceptance remain unproven.
- Device remains on validation code196 for inspection; it was upgraded with `adb install -r` and no uninstall, reset or database cleanup was used. The local harness uses isolated test databases and does not overwrite the VOL5 pilot DB.
- The selected acceptance cap is `4,096` requested/effective; `2,048` remains historical diagnostic evidence only. The exact acceptance authorization is consumed and cannot be reused. The acceptance attempt reached response headers but not a complete response within the five-minute cap; no provider usage/cost was available locally, so external billing remains unknown.
- The local harness initially exposed a device freezer interruption at `DELAY_STARTED`; bounded cleanup and the test-only foreground keepalive resolved the local test hang. The code196 five-minute stalled-body run completed under app control without host force-stop. This is not proof of the historical OpenRouter cancellation actor, which remains unknown.
- The current PRONOUN transport file includes a UTF-8 BOM, but the app-owned semantic bytes match the immutable binding. Do not silently change the normalization rule or rebind the project.

## Next step

The approved single-use diagnostic and acceptance authorizations are consumed and
must not be reused. Code196 now verifies the local end-to-end deadline, stalled
body recovery, process-restart stale-claim recovery, no late commit and no
automatic redispatch. The exact persisted acceptance attempt remains
`RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT`, with lifecycle headers and
generation metadata, no complete response/report/receipt, and
`EXTERNAL_STATE_REMAINS_UNKNOWN`. The local gate is
`RAW_RETRY_READY_FOR_NEW_AUTHORIZATION`, not provider authorization: do not
retry, do not infer `$0`, and do not open RECONCILE. A further call requires a
new recovery decision, new exact-phase authorization and a fresh preflight.
Keep `RAW_ACCEPTANCE_INCOMPLETE / P6_NOT_READY / EXECUTION_DISABLED /
NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE`.

This file is current-only; Git history preserves prior state.
