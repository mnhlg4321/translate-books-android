# Build State

- Status: P5E_0_DOCUMENTATION_CLEANUP_PASS / DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED / P5D_DEADLINE_BODY_READ_HARDENING_PASS / NO_LATE_COMMIT / NO_AUTOMATIC_REDISPATCH / CODE191_EXTERNAL_STATE_RECONCILED / COMPACT_RAW_WIRE_CONTRACT_LOCAL_PASS / STRUCTURED_OUTPUT_REQUEST_LOCAL_PASS / OUTPUT_SIZE_WITHIN_BUDGET / RAW_REPLAY_PROTECTION_PASS / PILOT_DATA_PRESERVATION_GATE_FAILED_ORIGINAL_CODE196_DB_LOST_RECONSTRUCTED_ONLY / P5E_LIVE_PREP_BLOCKED_PILOT_DATA_PRESERVATION / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / RECONCILE_AUTHORIZATION_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Active authority: V5-SAFE.4.1.3-FULL; canonical plan `EDITORIAL_RECOVERY_V4_18.md`.
- Workspace: D:\App Translate Books\App Translate Books-translation-profile.
- Branch: feature/v4.18.
- Current commit baseline: d86bb12ed52a0a8f475c8654b9a282a5ceae104e, the implementation/test/documentation baseline immediately before this snapshot commit; not self-referential. P5E implementation and immutable-envelope fix are committed; documentation records the failed original-data preservation gate and the focused fixture correction.
- Last validation package used: production APK 4.17-p5e.3 / Android versionCode 199, archived in artifacts and backup with SHA-256 `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09` and installed on device `15e84958` with `adb install -r` after the pilot DB had been reconstructed. The original code196 package data was not preserved. The artifact source ZIP SHA-256 is `824B7B59994F6E2E1573F0305081E60A7ADB10771C085B9C86EC32C14DC09A7C` at source commit `03b97a30885393c1cc8a3297d5dff9672dcba57e`; clean focused test APK SHA-256 is `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456` at test-source commit `914820d3c91ae8df5cc6b2769df7b2d036585f7a`. No authority or canonical pack/profile resource changed.
- Current phase: P5E.1 code191 metadata reconciliation is `EXTERNAL_CONFIRMED_CANCELLED`; it is not a `$0` billing conclusion and has no completion timestamp/body. P5E.2-P5E.7 compact-wire, structured-request, size-budget and replay gates pass locally; no new provider call or authorization has been made after the preservation incident. Schema v24 and VOL5 source data are present only in the reconstructed validation DB. Because `PILOT_DATA_PRESERVED` failed, no P5E live authorization is prepared; RECONCILE remains `RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED`, unauthorized; Editorial execution/certification remain disabled.

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
- P5E local contract gate: code191 metadata is reconciled as `EXTERNAL_CONFIRMED_CANCELLED`; the full legacy response shape is `49,665` bytes and the compact wire worst case is `2,785` bytes under a `3,584` byte ceiling, with the effective output cap retained at `4,096`. A separate `safe4.raw.discovery.wire.v1` schema, strict parser, app-owned RAW materializer, empty-change guard, reasoning accounting and replay binding are implemented. Engine boundary tests are `192/192 PASS`; app unit/build checks and Android test compilation pass. No new provider call has been made.
- P5E focused regression on code199 with the clean test APK passed manually via `adb shell am instrument` (no connected-test installer): raw-only fake boundary, raw-then-reconcile compatibility replay, compact-wire recovery evolution, immutable recovery-decision/history checks, reconstructed v24/VOL5 readback and persisted VOL5 setup (`7/7`). The raw-only fixture now asserts exact RAW before/after and `declaredChanges=[]`; no provider call was made.
- P5E device preservation gate: the original code196 package data was lost when a connected instrumentation installer handled a version-downgrade attempt; no explicit uninstall/reset command was issued, but the package disappeared and no local DB backup was available. The v24/VOL5 data on device was rehydrated from the canonical pack and user-provided source files. Code197 → code198 and code198 → code199 `adb install -r`/reopen checks are `RECONSTRUCTED_ONLY`; they cannot be reported as code196 pilot-data preservation. An isolated no-provider identity probe reproduced the old evaluation-derived binding/run, but did not restore the missing attempt or reconciliation rows. `PILOT_DATA_PRESERVATION_GATE_FAILED`; no new authorization or provider dispatch followed.

## Validation artifact

- Latest validation APK: `artifacts/builds/v4.17-p5e.3/build-20260910-211805/TranslateBooks-v4.17-p5e.3-code199.apk`.
- Backup mirror: `backup/builds/v4.17-p5e.3/build-20260910-211805/TranslateBooks-v4.17-p5e.3-code199.apk`.
- APK SHA-256: `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09`.
- Build event source snapshot: `03b97a30885393c1cc8a3297d5dff9672dcba57e`; code199 is validation-only and was installed with `adb install -r` after data reconstruction. Source ZIP SHA-256: `824B7B59994F6E2E1573F0305081E60A7ADB10771C085B9C86EC32C14DC09A7C`.
- Clean focused test APK SHA-256: `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456`; temporary identity-probe APKs are diagnostic and not evidence of a live acceptance.
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
- Code189/code191 attempts and their authorizations are historical evidence. Code191 generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` is reconciled from authenticated metadata as `EXTERNAL_CONFIRMED_CANCELLED` with input `23,674`, aggregate output/reasoning `0/0`, generation duration `9,642 ms` and upstream usage `0.0047348`; there is no completion timestamp/body or unambiguous billing flag, so `$0` is not recorded.
- The historical diagnostic attempt returned complete HTTP `200` transport but stopped at `max_output_tokens`; the app typed `RETRY_OUTPUT_TRUNCATED` and committed no predecessor/report/receipt. No automatic retry is allowed; a new exact-phase authorization would be required for another call.
- P5C proves app-bound fake attempt persistence and idempotent RAW→RECONCILE replay, but does not prove a live response, real token/cost usage, live cancellation/process-death behavior, or a real chapter `REPORT_L1`/receipt commit. RECONCILE and final L1 acceptance remain unproven.
- Device is on validation code199 after `adb install -r`; its v24/VOL5 DB is reconstructed validation data, not the lost code196 pilot DB. No further uninstall/reset/database cleanup is permitted. The original code196 preservation claim is withdrawn; isolated harnesses remain separate from the current DB.
- The selected acceptance cap is `4,096` requested/effective; `2,048` remains historical diagnostic evidence only. The exact acceptance authorization is consumed and cannot be reused. The acceptance attempt reached response headers but not a complete response within the five-minute cap; no provider usage/cost was available locally, so external billing remains unknown.
- The local harness initially exposed a device freezer interruption at `DELAY_STARTED`; bounded cleanup and the test-only foreground keepalive resolved the local test hang. The code196 five-minute stalled-body run completed under app control without host force-stop. The former `PROCESS_RESTART_RECOVERY_VERIFIED` label is superseded by `DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED`; the current evidence does not prove two independent app process invocations.
- The current PRONOUN transport file includes a UTF-8 BOM, but the app-owned semantic bytes match the immutable binding. Do not silently change the normalization rule or rebind the project.
- The original code196 pilot attempt/reconciliation rows are unavailable after the installer incident. Rehydrating source/pack data and reproducing an old deterministic identity in an isolated DB is not historical-row restoration; `PILOT_DATA_PRESERVED` therefore remains failed.

## Next step

Code189/code191 and their consumed authorizations remain historical and must
not be reused. Code191 now has the external P5E classification
`EXTERNAL_CONFIRMED_CANCELLED`; it is not a `$0` billing conclusion. The
original VOL5 attempt row is not present in the reconstructed DB, so no new
authorization may be prepared. The local compact-wire, structured-output,
size-budget and replay gates pass, but `PILOT_DATA_PRESERVED` fails and the
P5E live gate is blocked. Do not call the provider, increase cap/timeout,
retry automatically or open RECONCILE. Resume only after the owner supplies a
trusted code196 DB backup or explicitly approves a separately designed fresh
pilot-data initialization that does not masquerade as recovery.

This file is current-only; Git history preserves prior state.
