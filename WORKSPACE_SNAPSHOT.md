# Workspace Snapshot

- Updated: 2026-09-12 01:07 (+07:00).
- Current status: P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED / P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED / P5E_9B_A3_1_TECHNICAL_PASS / P5E_9B_A3_1R_DOCUMENTATION_AND_EVIDENCE_PASS / P5E_9B_ROUTE_DIAGNOSTIC_A3_ARTIFACT_SUPERSEDED_NOT_INSTALLED / P5E_9B_ROUTE_DIAGNOSTIC_A3R_ARTIFACT_BUILT_NOT_INSTALLED / P5E_9B_A3_2_DEVICE_DIAGNOSTIC_APPROVAL_REQUIRED / P5E_9B_A4_REMEDIATION_NOT_SELECTED / P5E_WORKFLOW_PRETAG_FAIL_CLOSED / RAW_AUTHORIZATION_REQUIRED / NO_AUTHORIZATION_CREATED / NO_LIVE_CALL_PERFORMED / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / P6_NOT_READY. Historical completed statuses are retained below and are not current readiness claims.
- Current version/build: frozen candidate `4.17-p5e.11` / code207, event `build-20260911-201725`, build source snapshot `995d3b6c9678e93905b3802cf22eee0b091b1bb3`, production fix commit `ff6821a5de6f825c55e35f8570dfbf074b4e64b5`, APK SHA-256 `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`, source ZIP SHA-256 `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348`, and certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; artifact/backup payloads are byte-identical. The candidate was installed exactly once through the approved guard and read back successfully. The device is `15e84958`, package `com.ml.tblandroidtxt`, v4.17-p5e.11/code207, signature token `abebea4b`, schema v24, DB SHA-256 `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`, data `RECONSTRUCTED_ONLY`. The A2 test artifact remains historical device evidence; A3.1 added a new private diagnostic artifact that is not installed.
- Current commit baseline: actual resumed HEAD before A3.1R is `33cc68cf8195abf311d86604894226613303fc26`; implementation baseline is `a0009f04139431f0bee38d049f9b32e2b6b04c41`; A3.1 diagnostic test commit is `89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`; A3.1R test-only commit is `9b59ce39b326d5e81e62861b150a618a5e80cddc`; documentation baseline before A3.1R is `fe265e7b579810942370ab4a1a203ca8070f001b`. This snapshot records the implementation/test baseline before its separate documentation/result commit. Test-only correction `f2695c862a9b860e08fd01f932377ec5576d6ad1`, production fix `ff6821a5de6f825c55e35f8570dfbf074b4e64b5` and prior test coverage `995d3b6c9678e93905b3802cf22eee0b091b1bb3` are ancestors. Active authority, canonical pack/profile and final schemas are unchanged. The current data remains `RECONSTRUCTED_ONLY`; code189/code191 and code206 QF results are historical evidence, code204 is pre-correction evidence and code205 is intermediate dirty-source evidence.
- Current phase: the official fresh binding evaluation remains `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`; stale historical P5D evaluation `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` is not interchangeable. A2 ended fail-closed because persisted settings did not satisfy the fresh route predicate; no individual setting was exposed. Its evidence SHA-256 is `42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`, and the approval is not reusable. A3.1R completed host-only authority synchronization, PreTag fail-closed verification and the diagnostic result-channel correction from Logcat-only to instrumentation status. The old A3 artifact is superseded/not installed; the A3R artifact is built/not installed. No ADB, provider/API call, authorization/attempt/reconciliation or current-DB mutation occurred.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.

## Current active state — P5E.9B-A3.1R complete, A3.2 approval required

- A2 is historical fail-closed evidence, not a route or exact-preflight pass. `P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED` and `P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED` remain the active safety boundary.
- P5E.9, A2 and the P5 exit gate remain incomplete. A3.1 has a technical host-only result and A3.1R now has the documentation/evidence-channel result; A3.2 remains a separate device approval gate.
- Implementation baseline: `a0009f04139431f0bee38d049f9b32e2b6b04c41`. Test-only diagnostic commit: `89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`. A3.1R test-only commit: `9b59ce39b326d5e81e62861b150a618a5e80cddc`. Documentation baseline before A3.1R: `fe265e7b579810942370ab4a1a203ca8070f001b`.
- A2 evidence SHA-256: `42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`. A3.1R device operations, provider calls and current-DB reads/mutations were `0`.
- A3 artifact: `D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk`, SHA-256 `64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, package `com.ml.tblandroidtxt.test`, target `com.ml.tblandroidtxt`, runner `androidx.test.runner.AndroidJUnitRunner`, size `1326212` bytes; retained as `SUPERSEDED_NOT_INSTALLED`. A3R artifact: `D:\P5E-private\fresh-raw-route-diagnostic-a3r-20260912-005638832-test-apk\app-debug-androidTest.apk`, SHA-256 `F19051D849139CB66C4005342AF45DEE62F2A1C7C44D7F0316EC810F8F4DBD1E`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, package `com.ml.tblandroidtxt.test`, target `com.ml.tblandroidtxt`, runner `androidx.test.runner.AndroidJUnitRunner`, size `1326403` bytes; built, not installed and not approved for A3.2.

## P5E.9A-EVAL historical freeze evidence

| Item | Current fact |
|---|---|
| Installed baseline | `15e84958` / `com.ml.tblandroidtxt` / `v4.17-p5e.10` / code206 / signature token `abebea4b` / schema v24 |
| Candidate archive | `v4.17-p5e.10` / code206 / source `049e72b5769f8b3fdcb6f50646d1f0ead3043940` / APK `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080` |
| Candidate certificate | `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Superseded test APK | `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2`, source `f90c0372c019c0d3970efb298b64b6a4addcfd4f`, kept outside Git; `4/5` boundary run exposed the query defect |
| Corrected test APK | `68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A`, source `34a4ec2832d71a488a2531a0e69a85261e9c9b9b`, kept outside Git; installed only as `com.ml.tblandroidtxt.test` after owner approval and read back with matching hash/signature |
| Fresh binding | selector/binding/run/chapter matched the requested values |
| Evaluation freeze | PASS: official setup/binding and corrected runner both use `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`; `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` remains historical only |
| Current DB mutation | none; attempts, authorization receipts, reconciliation, history, lifecycle and report/receipt counts remain zero |
| Snapshot/restore | data-level PASS, private reconstructed snapshot; current app restore/readback not re-run |
| Provider/auth | provider calls 0; no authorization created/consumed; no RECONCILE |

The earlier exact evaluation mismatch was a stale candidate expectation, not a
current-DB defect. The official binding row was already correct and the
code206 candidate was later installed under its separate owner approval. The
new LQ production fix is archived as code207 but is not device-verified; the
previous QF device result remains historical zero-call evidence. A new owner
approval is required before any code207 upgrade or candidate-aligned device
test.

## P5E.9A-LQ historical pre-DV freeze

| Item | Current fact |
|---|---|
| RED characterization | Historical QF reproduced the schema-v24 error; the new disposable-v24 test encodes PRAGMA/legacy-predicate coverage and compiles, but has not run on device |
| Production fix | `ff6821a5de6f825c55e35f8570dfbf074b4e64b5`, one read-only `inspectLineage` helper; no schema/migration change |
| Test coverage | `995d3b6c9678e93905b3802cf22eee0b091b1bb3`, isolated empty/used/unrelated/schema/chapter/auth cases; QF calls the same helper |
| Host QA | Engine `200/200`, app debug/release/benchmark `232/232` each, lint debug PASS, AndroidTest compile PASS, diff/secret checks PASS |
| Candidate archive | `v4.17-p5e.11` / code207 / `build-20260911-201725`; production APK SHA `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`; certificate SHA `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Source archive | `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348`; artifact/backup byte-identical |
| Superseded test APK | `D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk`; SHA `9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2`; not installed, not approved |
| New test APK | `D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk`; SHA `50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587`; source `f2695c862a9b860e08fd01f932377ec5576d6ad1`; not installed, not approved |
| Device/current DB | Still code206 / schema v24 / DB SHA `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`; no LQ device or DB mutation |
| Provider/authorization | `0` / `0`; no live call, attempt, reconciliation or RECONCILE |

The previous QF direct result (`1/1` then `5/5`) remains historical evidence
against code206 and its approved test artifact. It does not certify code207 or
restore `FRESH_RAW_EXACT_PREFLIGHT_READY` after the production defect was
found. The current next gate is a separate owner approval pinning code207 and
the new test APK; only then may snapshot/restore, guarded install and direct
zero-call instrumentation be considered.

## Completed tasks

- P0/P1 baseline, characterization, four-entry fixture and gap matrix completed.
- P2A canonical reference pack frozen; P2B import acceptance passed.
- P3A GAP-012 importer hardening passed with bounded one-pass drain and EOCD validation retained.
- P3B deterministic contract/profile and trusted compatibility evidence completed: P01-P09 `9/9`, G1-G24 `24/24`, exact 11 capability evidence, profile v2 and execution lock.
- P4 characterization proved the legacy project owner and v18 persistence boundary. P4 added the minimal project-scoped selection/binding owner, additive v19 immutable binding tables, exact source identity, atomic persistence, restart/process-death resume, stale-chain checks and side-by-side 4.1.3/4.1.4 acceptance.
- P4 UI remains setup-only: management is read-only, selection is explicit within new project flow, no global active/latest pack and no Run/Start/Activate path.
- P4 post-closure source-mode correction is committed: new bindings use `NORMAL_FOUR_SOURCE`/`ALTERNATE_EXPLICIT`; the old P4 producer vocabulary was rejected by a new failing-then-passing device test.
- P5 dry-run boundary is implemented in the engine with an injected fake provider: exact authorization/binding gate, preflight-before-provider, phase projection, typed response recovery, one schema-only repair, local receipt/diff validation, bounded usage and atomic attempt-store contract. `EditorialP5PilotExecutionBoundaryTest` is `15/15 PASS`.
- P5C app-bound exact fake E2E is complete: additive v20 durable attempt owner, exact persisted binding selection, RAW→RECONCILE predecessor readback, redacted report/receipt atomic commit and DB-reopen idempotency. Focused P1/P2/P3B/P4/P5C is `22/22 PASS`.
- Canonical 4.1.3 was imported through the production pack picker on code183 and displayed `DATA_COMPATIBLE`; the test-only device setup passed and staged the four user-supplied chapter files outside Git for exact app-owned source identity creation. The later authorized live attempt used this persisted binding.
- The bounded OpenRouter adapter received exact app-owned binding/run/manifest/bundle/predecessor context and constructed the phase-projected RAW request; the provider returned no response before the typed stop. No RECONCILE request was made.
- Code184 validation passed focused `23/23` and full `118/118` instrumentation; the full suite's real API test remains skipped by opt-in. The new live runner is also opt-in and requires an explicit `p5c_live=YES` invocation after final user confirmation.
- P5D.0 documentation gate is complete: the P4 starting commit is corrected, code184 is historical and code186 is current/latest, code181/code177/code176 remain historical, and PASS/SKIP/FAIL wording is separated. No provider call is permitted in P5D.0-P5D.4.
- P5D.3/P5D.4 are complete locally: additive v21 lifecycle/auth/reconciliation owners, typed provider failures, non-reclaimable recovery gate, hashed single-use authorization receipts and redacted timing/generation metadata pass focused tests; no provider call was made.
- P5D.1/P5D.2 are complete by bounded authenticated read-only OpenRouter metadata for the historical VOL4 and current VOL5 attempts: both matching generations are `EXTERNAL_CONFIRMED_CANCELLED`; a new exact-phase RAW authorization is required before any retry. See `docs/P5D_PROVIDER_RECONCILIATION_RECORD.md` and `docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md`.
- A new user authorization was received and consumed for one independent VOL5/chapter001 RAW attempt: OpenRouter `openai/gpt-5.6-luna`, egress YES, one primary plus one schema repair maximum, zero network retries, USD 0.10 total cap and five-minute window; OpenRouter later confirmed cancellation. RECONCILE remains explicitly unauthorized. The source folder is outside Git.
- A production-owned RAW-only entry point and focused fake regression are committed in `1994b3c`; they return after durable RAW readback and never construct a RECONCILE request. The VOL5 setup/live test is committed in `e34084d`.
- VOL5 setup instrumentation passed `1/1`: selector `p5d-raw-mercedes-vol5-001`, exact pack/profile identity and app-computed source hashes were read back. The earlier missing-key invocation is preserved as `LIVE_AUTHORIZATION_INCOMPLETE` with providerCalls `0`. The subsequent explicit RAW invocation dispatched one request and persisted `RECOVERY_REQUIRED`; OpenRouter generation `gen-1788910936-DHfTNOyDlU3f3PJOAvqb` is confirmed `cancelled`, with zero report/receipt bytes and no RECONCILE. See `docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md`.
- P5D code189 local HTTP closure passed immediate, 11-second delayed-with-legacy-cancel and 11-second delayed-without-legacy-cancel (`1/1` each); the full fake E2E class passed `13/13`, schema/migration device classes `41/41`, and full instrumentation passed `130 tests / 0 failures`. Adapter lifecycle metadata survived DB reopen on isolated v22 databases. Detailed evidence is in `docs/P5D_LOCAL_HTTP_HARNESS_REPORT.md`.
- P5E.9A implementation group is locally staged: the historical P5D runner rejects the fresh selector with a typed guard; the fresh runner separates preflight-only from future RAW dispatch; the fresh OpenRouter route pins JSON Schema/strict/minimal reasoning, `require_parameters=true`, no fallback and an explicit upstream; the evaluation provenance pin is corrected to the official fresh record; host route/parser/provenance tests pass and AndroidTest compilation passes. No provider call, authorization or current-DB mutation occurred.
- The exact-binding preflight patch reconstructs the expected fresh binding, source identities/bytes, immutable pack authority, bundle identity and request-envelope hash without calling `resumeProject` or any provider. Production correction commit `049e72b5769f8b3fdcb6f50646d1f0ead3043940` remains the installed code206 source; QF test-only correction commit `34a4ec2832d71a488a2531a0e69a85261e9c9b9b` replaced the faulty reconciliation predicate and is historical. LQ production fix `ff6821a5de6f825c55e35f8570dfbf074b4e64b5` now owns the lineage query; candidate code207 is archived and unchanged, while QF2 built a new test artifact after correcting the version pin. The historical test APK `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2` is superseded.
- The controlled RAW diagnostic preflight was rerun without provider access: fake recovery/binding class `13/13 PASS`, live recovery inspection `1/1 PASS`, engine expiry/budget/authorization boundary `16/16 PASS`; provider calls `0`. Exact identities and the pre-dispatch snapshot are in `docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`.
- The user-approved RAW diagnostic authorization was consumed once. The single OpenRouter call returned HTTP `200`/complete transport but `finish=length` at `2,048` output tokens; local validation returned `RETRY_OUTPUT_TRUNCATED`, persisted lifecycle reached `RESPONSE_BODY_COMPLETE`, and no partial report/receipt was committed. See `docs/P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md`.
- Test-first output-budget evidence reproduced the silent `4,096 → 2,048` clamp. The exact-cap HTTP body test and invalid-cap fail-closed test now pass after the minimal alignment change. A focused recovery-history test proved v22's single immutable reconciliation row could not retain a later truncated decision; additive v23 append-only history is implemented and awaits device migration/readback verification. No provider call was made for this change.
- Output-budget alignment is now verified: authorization/coordinator/adapter/request propagate the exact `4,096` cap, and v23 append-only reconciliation history preserves the earlier cancelled decision and later truncated decision without replacing the primary row.
- Code191 focused validation is current evidence: VOL5 v23 recovery readback `1/1`, fake E2E `14/14`, schema/migration `41/41`, importer/P1/P2 `23/23`, P3B/P4 `5/5`; host engine `181/181`, app all unit variants `657/657`, external qualification `306/306`; provider calls in alignment/preflight `0`. Full code189 instrumentation `130/130` remains historical; the delay harness was not rerun.
- The exact user-approved RAW acceptance authorization `P5D-VOL5-RAW-ACCEPTANCE-20260909-01` was consumed once. The live test appended the recovery decision for the prior truncated generation while preserving the original immutable cancelled decision; this was test-only gate wiring before dispatch.
- The final focused test APK was rebuilt from `ae6d9e2`, installed with `adb install -r`, and used for redacted readback. The production package remained code191 and the validation database was not reset or uninstalled.
- The approved RAW acceptance dispatched once. The host runner did not reach a terminal assertion by the five-minute authorization deadline; readback showed `CLAIMED` before cleanup, a consumed acceptance receipt, lifecycle `RESPONSE_HEADERS_RECEIVED`/HTTP 200 with generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8`, request bytes `85,068`, and zero report/receipt bytes. The attempt was then closed through the existing attempt-store owner as `RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT`; no second provider call was made and external state remains unknown.
- P5D deadline/body-read hardening is committed and verified: the OpenAI-compatible client has a scoped monotonic deadline, bounded one-pass response-body read with redacted progress bytes, and RAW-only call ownership; the attempt store has additive response-byte persistence and stale-claim recovery. Historical code196 device tests cover immediate/delayed transport, the short stalled-body case, the 300-second stalled-body case and stale-claim recovery without redispatch; the current documented claim is DB reopen, not two-process restart.
- Code192 device evidence showed the bounded stalled-body test reached a client `SocketInputStream` read after the server had accepted the request; the stack and timing exposed that `EditorialP5CExactBindingExecution`'s counting wrapper dropped `beginAttempt()`. Code193 then showed the deadline was enforced but the Android timeout exception was mapped to `FAILED_UNKNOWN`; the provider now maps an exception observed after its monotonic deadline to the existing typed timeout, while still giving explicit cancellation precedence. Code196 reruns pass, including the five-minute local stalled-body case with one server request and app-owned terminal recovery. The invalid-hex fixture remains corrected without weakening hash validation.
- P5E.1 reconciled generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` from authenticated OpenRouter metadata as `EXTERNAL_CONFIRMED_CANCELLED`: provider OpenAI, input `23,674`, aggregate output `0`, reasoning `0`, generation duration `9,642 ms`, upstream usage `0.0047348`, no completion timestamp/body and no unambiguous billing flag. No `$0` conclusion or authorization was derived from the metadata.
- P5E.2 measured the current full response shape at `49,665` bytes (`23,814` bytes duplicated source plus ledger/gates/identities/evidence/syntax); the four-byte heuristic is `12,417` and cannot justify a `4,096` semantic cap. The compact wire worst case is `2,785` bytes under an explicit `3,584` byte local ceiling, leaving headroom under the `4,096` token cap. Exact tokenization was unavailable; byte results are the acceptance evidence.
- P5E.3-P5E.6 add a separate bounded `safe4.raw.discovery.wire.v1` DTO/parser, app-owned RAW before/after materialization, strict JSON Schema output, minimal reasoning, hard item/ID/ref limits, empty RAW changes and exact attempt/envelope replay binding. Final `safe4.full.report-l1.v1` and receipt schemas are unchanged.
- P5E.7 host engine boundary tests are `200/200 PASS`; app unit variants are `228/228` each (`684/684` aggregate), Android test compilation passed, and the clean code199 test APK passed the focused manual device matrix (`7/7`) covering raw-only/no-RECONCILE, compact recovery evolution, recovery history, exact RAW materialization, reconstructed v24/VOL5 readback and setup. Candidate-aligned AndroidTest APK SHA-256 `63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC`, test-source commit `424278e44c042b882d1888f45d5c4b5b944e0dca`, ran `EditorialP5EFreshPilotInstrumentedTest` directly via `adb shell am instrument` and passed `4/4`; fake predecessor rows were isolated. No live provider call has been made for P5E.
- P5E compact QA correction is test-first and local-only: engine `200/200 PASS`, app debug/release/benchmark `228/228` each (`684/684` aggregate), AndroidTest compilation pass. New checks cover duplicate finding/evidence/preserved IDs, orphan evidence, hard findings/preserved/disposition bounds, unsafe escaping, population overflow before provider, STOP without commit, local coverage and RAW UTF-8/BOM round-trip. The provider request still uses strict JSON Schema, `stream=false`, `require_parameters=true`, minimal reasoning and no response-healing.
- P5E installer guard is exact and fail-closed: candidate code202 `-CheckOnly` passes package/version/APK SHA-256/certificate/device-token pins; wrong APK hash, wrong certificate and missing build certificate pin fail closed. After G1, one guarded code199 → code202 install passed and G2 package/data readback passed. Earlier code199/device checks and historical code196 downgrade rejection remain historical negative evidence. `:app:connectedDebugAndroidTest` fails during Gradle configuration before any installer runs. No fallback, second install, reset, clear, uninstall or downgrade was performed in this group.
- P5E candidate-install QA found and reproduced a missing payload pin: the old guard rejected the new `ExpectedApkSha256` parameter. The minimal patch now requires exact APK SHA-256 and certificate SHA-256, passes the code202 check-only match, rejects wrong hash/certificate and rejects `build-and-save.ps1 -Install` without certificate pin. Candidate code202 then passed one guarded install and G2 data readback; it is device-verified for package/artifact/data preservation, not for a live provider acceptance.
- Candidate code202 was built through `scripts/build-and-save.ps1` with app unit/lint/archive success, versionCode `202`, source commit `4140651d860e4ee11ce7e074970761666c575594`, APK SHA-256 `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`, source ZIP SHA-256 `D91FE78F99DE6D04CE0DA09C54A76BF030BA40408CF74142B8FC8FF243471A5C`, and identical artifact/backup payloads. After owner approval and `BACKUP_RESTORE_G1_PASS`, one guarded install passed; post-upgrade package/DB/source/pack readback passed.
- Fresh-pilot owner approval was observed at `2026-09-11T06:00:59+07:00` for local-only work, exact candidate pins and no provider/authorization/retry/repair/RECONCILE. The pre-upgrade private snapshot is outside Git at `D:\P5E-private\fresh-pilot-20260911-060059`, manifest SHA-256 `1C495F95B0572458B8405B599A7D11CC80F6770316080A1772EF379A17AC7C64`; classification is `RECONSTRUCTED_ONLY`, not code196 recovery. SQLite snapshot/isolated restore passed (`user_version=24`, `integrity_check=ok`, foreign-key violations `0`, source/pack hashes equal). The candidate was installed once through `scripts/install-validated.ps1`; device readback is code202 with the same reconstructed DB/source/pack invariants.
- Prior fresh-pilot G3/G4 evidence is retained in the private snapshot `D:\P5E-private\fresh-pilot-20260911-060059\fresh-pilot-snapshot`, manifest SHA-256 `2BEA88D4B562DFFA0CEAE401E1BFA0ED50B353CCD014AA9840F15CE1FA7F35EF`; it is historical relative to the current exact-freeze result. The new snapshot `D:\P5E-private\fresh-raw-boundary-20260911-1810` has manifest SHA-256 `6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28` and records the observed evaluation mismatch. It is classified `RECONSTRUCTED_ONLY`, not code196 recovery.

## Validation evidence

- Host engine XML: `200/200 PASS`, `0` failures, `0` errors, `0` skipped; app debug/release/benchmark unit XML is `232/232 PASS` per variant (`696/696` aggregate), lint debug and AndroidTest compilation passed. The prior code206 device QF result remains historical `1/1` plus `5/5`; code207 device readback and the approved direct zero-call run now pass, with the lineage class `13/13` and the aligned QF class `5/5`. External qualification remains `306 PASS / 0 FAIL` from the prior hardening baseline.
- Code189 full device instrumentation: `130` tests, `0` failures; real provider paths remained opt-in/skipped. Code186 `124` tests remains historical evidence.
- Provider/API calls: code189/code191 and their provider generations are historical evidence only. P5E.1 used authenticated read-only metadata for code191 generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` and recorded `EXTERNAL_CONFIRMED_CANCELLED`; no new provider call or authorization was created during local contract work. Prior known costs remain recorded separately and are not collapsed into `$0`.
- Canonical ZIP `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`, Java control `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`, and profile resource `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` re-hash correctly.
- Pre-upgrade installed validation APK code199 SHA-256: `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09`; source ZIP SHA-256 `824B7B59994F6E2E1573F0305081E60A7ADB10771C085B9C86EC32C14DC09A7C`. Code202 was the pre-install device baseline with APK SHA-256 `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`; code206 was the prior installed candidate with APK SHA-256 `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080`, source commit `049e72b5769f8b3fdcb6f50646d1f0ead3043940`, and certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`. Code207 is now the installed candidate with APK SHA-256 `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` and the same certificate. The superseded candidate-aligned test APK was `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2` from `f90c0372c019c0d3970efb298b64b6a4addcfd4f` and stopped at `4/5`; the approved replacement is `50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587` from `f2695c862a9b860e08fd01f932377ec5576d6ad1`, package `com.ml.tblandroidtxt.test`, same certificate, and was replaced once as the test package only. Code189/code191/code196 remain historical; reconstructed data is not code196 history. Code201 is the pre-patch script-managed archive artifact and was not installed on the current device; no code201 device-verification claim is made.

## Pending tasks

- Historical code196 preservation remains failed: the original pilot DB disappeared during the connected-test installer incident and no trusted backup was found. Code197 → code198 and code198 → code199 readback covers reconstructed data only. This historical failure is not relabeled as recovered; the owner-approved fresh path is independent.
- P5E.9A-EVAL host provenance, the approved guarded code206 install, and post-install tuple/data readback are complete. The historical QF corrected only the test predicate through the required attempt-identity JOIN and passed `1/1` plus `5/5`; its readiness claim was superseded by the LQ production query defect. LQ production fix `ff6821a5de6f825c55e35f8570dfbf074b4e64b5` and isolated coverage commit `995d3b6c9678e93905b3802cf22eee0b091b1bb3` pass host QA. QF2 corrected the test version pin `206L` to `207L`, and the approved DV run installed code207 once, replaced the aligned test package once, and passed the focused zero-call tests. The current DB still has the official `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`; no SQL correction was made. A live RAW authorization is not prepared or issued; provider calls, attempts, repair, retry and RECONCILE remain zero.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No successful P5E app-validated live provider response, real REPORT_L1/receipt, model result or certification evidence exists yet. The original VOL5 attempt/reconciliation rows are unavailable in the reconstructed DB; code191 remains external `EXTERNAL_CONFIRMED_CANCELLED` evidence only, while billing remains unresolved and must not be recorded as `$0`.
- P4 creates pilot setup metadata only; `DATA_COMPATIBLE` and selectable status do not mean runnable or certified.
- Initial setup UI collects explicit source text for binding metadata; it does not certify source bytes or open execution.
- Bootstrap profile v1 remains loadable and non-executable.
- The consumed historical VOL4/VOL5 authorizations cannot be reused. Code191 metadata has the external classification `EXTERNAL_CONFIRMED_CANCELLED`; it has no completion timestamp/body and does not establish whether billing occurred. OpenRouter I/O logging remains disabled. No new P5E authorization was created after the preservation incident. The reconstructed DB has v24/source setup but not the historical VOL5 attempt row or reconciliation history.
- The current fresh selector/binding/run/chapter is present with the official frozen evaluation `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` and zero attempt/auth/reconciliation counts. The old `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` value was a stale runner expectation and is retained only as historical P5D evidence. The LQ production helper now uses the schema-v24 attempt joins and fails closed on query/schema errors; code207 device verification and the zero-call helper run are complete.
- Current validation device state: candidate code207 is installed once with the owner-approved `scripts/install-validated.ps1` guard after WAL-aware snapshot, isolated restore and CheckOnly passed. The code196 package data preservation claim is withdrawn; current data remains `RECONSTRUCTED_ONLY`; no further uninstall/reset/database cleanup is permitted. The aligned test package was replaced once and direct focused zero-call instrumentation passed. Use the explicit serial/version/hash/certificate/signature guard for any future approved install.
- The current VOL5 PRONOUN transport file includes a UTF-8 BOM, but semantic bytes after the existing app-owned removal match the immutable binding. No source rewrite or silent rebind is needed.
- The local delayed harness initially hit a device freezer interruption at `DELAY_STARTED`; bounded cleanup and a test-only foreground keepalive resolved the harness run, but the historical provider cancellation actor remains unknown.
- Code196 focused device evidence is not a P5E live acceptance pass, but its local deadline/body-read gates remain historical evidence: the existing 17 methods, short stalled-body case (`2.069s`) and five-minute stalled-body case (`301.501s`) passed with one server request and no host force-stop. The prior `PROCESS_RESTART_RECOVERY_VERIFIED` label is superseded by `DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED`; the current test evidence does not prove two independent app process invocations. The isolated historical-identity probe reproduced the old binding/run from the old evaluation ID, but did not restore the lost DB rows.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## P5E.9A-LQ-DV — historical device verification

The owner-approved local/device scope is complete. Code207 was installed once
from the frozen production artifact after WAL-aware snapshot, isolated restore
and guarded CheckOnly passed. The test package was replaced once with the
approved candidate-aligned APK. No production reinstall, connected test,
provider/API call, authorization, attempt, reconciliation, retry, repair or
RECONCILE occurred.

```text
device=15e84958
productionPackage=com.ml.tblandroidtxt
installedProductionVersion=v4.17-p5e.11
installedProductionVersionCode=207
installedProductionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
installedProductionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
deviceSignatureToken=abebea4b
schemaVersion=24
dataClassification=RECONSTRUCTED_ONLY
productionUpgradeCount=1
testPackageReplacementCount=1
```

Evidence is outside Git:

```text
snapshotRoot=D:\P5E-private\p5e-9a-lq-dv-snapshot-20260911-210256
snapshotManifestSha256=BBE47271716785B1ED89F888748428C9A0437A47FF61804942FAF202BBE49C76
snapshotDatabaseSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
snapshotRestoreRoot=D:\P5E-private\p5e-9a-lq-dv-restore-20260911-210256
snapshotRestoreStatus=PASS_DATA_LEVEL_ONLY
postInstallReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postinstall-20260911-210550
postRunReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postrun-verified2-20260911-211156
```

Read-only post-run SQLite verification passed `integrity_check=ok`, schema v24,
foreign-key violations `0`, execution disabled and `NOT_CERTIFIED`. The fresh
project has one canonical chapter `001`; the global database has two rows with
that chapter key because another reconstructed project is present. The fresh
tuple and source identity set are unchanged, and all current P5 lineage/report
counts remain zero.

```text
dbBeforeSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbAfterSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
databaseHashUnchanged=true
freshSelector=p5e-fresh-mercedes-vol5-20260911-01
freshChapterKey=001
freshBinding=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
freshRunDeclaration=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
freshEvaluation=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
freshPackSha256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
freshProfileSha256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
freshProjectChapterRows=1
globalChapterKey001Rows=2
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportBytesNonEmpty=0
receiptBytesNonEmpty=0
partialCommit=false
automaticRedispatch=false
providerCalls=0
```

The approved test artifact is outside Git at
`D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk`:

```text
testPackage=com.ml.tblandroidtxt.test
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testSourceCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
testRunner=androidx.test.runner.AndroidJUnitRunner
isolatedLineageClass=13/13 PASS
schemaFailureMethod=1/1 PASS
chapterSemanticsMethod=1/1 PASS
qfSingleMethod=1/1 PASS
qfBoundaryClass=5/5 PASS
allTestsNoSkip=true
```

The current conclusion is:

```text
P5E_9A_LQ_TEST_VERSION_PIN_CORRECTED
CODE207_PRODUCTION_CANDIDATE_UNCHANGED
CODE207_DEVICE_VERIFIED
P5E_9A_QF_ZERO_CALL_BOUNDARY_PASS
P5E_9A_LQ_PRODUCTION_LINEAGE_QUERY_FIX_PASS
P5E_9A_LQ_DEVICE_ZERO_CALL_PASS
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_PASS
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

`P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS` is not claimed because this
approval was strictly zero-call and did not dispatch a valid authorization.
`FRESH_RAW_EXACT_PREFLIGHT_READY` is not used to replace that missing evidence.
At the time of this older snapshot the next step was an exact authorization
block; that state was superseded by the A2 fail-closed route result and the
host-only A3.1 diagnostic group below. No authorization may be created,
consumed or dispatched in the current state.

This was current-only state at the time of the earlier snapshot; Git history
preserves it as historical evidence.

## P5E.9B-A1 host-only snapshot

This snapshot records the completed host-only harness group. The current
implementation baseline immediately before this documentation update is
a0009f04139431f0bee38d049f9b32e2b6b04c41; the documentation commit is a
separate commit after that baseline. The branch is feature/v4.18 and the
working tree was clean before the documentation update.

~~~
Current version: v4.17-p5e.11 / code207 frozen production candidate
Current branch: feature/v4.18
Current commit: a0009f04139431f0bee38d049f9b32e2b6b04c41 (implementation baseline before snapshot commit)
Current build: A1 test APK 697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A, not installed
Production APK: 2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD, unchanged and not rebuilt
Device operations in A1: 0
Provider calls in A1: 0
Authorization created/consumed in A1: 0/0
~~~

Completed tasks: added the test-only
EditorialP5EFreshRawLiveInstrumentedTest with separate preflight and live
opt-ins; reused production preflightOnly, fresh routing policy and existing
fixture; required complete live authorization facts before runtime authorization
construction; compiled AndroidTest; built and offline-verified the private test
APK; revalidated the mirrored code207 APK/source-ZIP hashes; ran diff and
secret guards; prepared the unissued authorization template and A2 approval
block.

The final A1 test artifact is
D:\P5E-private\fresh-raw-live-harness-a1-20260911-233211-test-apk\app-debug-androidTest.apk,
package com.ml.tblandroidtxt.test, target package com.ml.tblandroidtxt,
runner androidx.test.runner.AndroidJUnitRunner, test-source commit
a0009f04139431f0bee38d049f9b32e2b6b04c41, certificate SHA-256
47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155, and
full APK SHA-256
697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A.
The earlier pre-amend artifact and QF2 device artifact remain historical and
were not overwritten.

Pending tasks: obtain owner approval for A2 only, then replace the test package
and run the preflight method under direct instrumentation. The preflight
method is not authorized by this snapshot, the live opt-in is forbidden, and
no exact-preflight-ready or valid-authorization-local-path status is claimed.

Known bugs and historical facts: original code196 pilot preservation remains
failed; reconstructed data remains classified RECONSTRUCTED_ONLY; code191
external reconciliation remains historical and reconciled; no A1 defect was
found in production wiring. The A1 artifact is not device-verified.

Regression status: AndroidTest Java compilation PASS; test APK build PASS;
offline package/target/runner/certificate/hash verification PASS; production
change guard PASS with zero production files; git diff --check PASS; secret
scan PASS. No ADB, connected test, provider call, device preflight or live
method was run.

Next step: owner approval for the exact A2 zero-call preflight block in
BUILD_STATE.md; after approval, run only the preflight method and preserve
the same zero-call/current-DB boundary.

## P5E.9B-A2 result snapshot (historical runtime evidence)

The A2 owner approval was used for one bounded device run. This is a runtime
result only; no production source changed. The implementation baseline remains
a0009f04139431f0bee38d049f9b32e2b6b04c41 and the documentation HEAD before
this update was 392c1b0a2e65175a693bebc1825047999ee0c874.

~~~
Current version: v4.17-p5e.11 / code207
Current branch: feature/v4.18
Current commit: a0009f04139431f0bee38d049f9b32e2b6b04c41 (implementation baseline)
Current build: code207 production unchanged; A1 harness test APK installed for A2 only
Device: 15e84958
Device test package: com.ml.tblandroidtxt.test / 697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
Database: schema24 / 3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391 unchanged
Production package operations: 0
Test package replacement operations: 1
Instrumentation invocations: 1
Provider calls: 0
~~~

Completed: read-only baseline gates passed; the existing QF2 test package hash
was confirmed before replacement; the final A1 test APK was installed once in
the test package; the single preflight method was invoked with the preflight
opt-in only; the failure was captured; and post-run database/package/lineage
readback confirmed zero mutation and zero lineage rows.

The preflight did not reach production preflightOnly because
SettingsStore.load(target) did not satisfy
EditorialP5EFreshRawRoutingPolicy.matches(settings). The exact mismatch field
was not inferred or logged. No exact preflight manifest exists for this run,
and exact-preflight-ready remains NOT_REACHED.

Regression status: the device method result is 0/1 PASS with one
route-precondition failure. Post-run SQLite integrity and zero-count checks
passed. No class rerun, live method, connected test, retry, repair, settings
mutation or RECONCILE occurred.

Pending blocker: route settings are not aligned with the fresh RAW policy.
The single next step is a new owner decision for an explicit read-only
investigation/remediation scope; the consumed single-run A2 approval is not
reusable.

## P5E.9B-A3.1 historical host-only result

The active A3.1 group completed without ADB, device settings access, current
DB access, instrumentation, provider/API call, authorization, attempt or
reconciliation. The A2 result remains fail-closed and historical; its evidence
SHA-256 is `42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`.

The PreTag verifier failed closed because current pilot closure remains
unchecked: `Step 09 is not complete for gate PreTag`. No false-green PreTag,
tag or release action was observed.

The test-only diagnostic source is committed at
`89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`. The opt-in method checks
`p5e_fresh_raw_route_diagnostic=YES` first, loads settings once, and emits
only the four boolean fields `providerMatch`, `modelMatch`, `endpointMatch`
and `routeMatch`. It does not log raw settings, API keys, source, prompt,
request or response content and does not open the database or call
`preflightOnly`. Synthetic offline cases were compiled but not run on a
device.

The private artifact is:

~~~
path=D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk
package=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
runner=androidx.test.runner.AndroidJUnitRunner
sourceCommit=89eef75a4a62e5674d02b7e48eaaff012d9a7ae0
sha256=64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
bytes=1326212
installed=false
~~~

The next action is separate A3.2 owner approval for exactly one test-package
replacement and one diagnostic-method invocation, with the live flag absent,
provider budget zero, production-package operations zero, no rerun/full class
and no A2 approval reuse.
