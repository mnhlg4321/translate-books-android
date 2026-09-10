# Workspace Snapshot

- Updated: 2026-09-10 (+07:00).
- Current status: P5E_0_DOCUMENTATION_CLEANUP_PASS / DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED / P5D_DEADLINE_BODY_READ_HARDENING_PASS / NO_LATE_COMMIT / NO_AUTOMATIC_REDISPATCH / CODE191_EXTERNAL_STATE_RECONCILED / COMPACT_RAW_WIRE_CONTRACT_LOCAL_PASS / STRUCTURED_OUTPUT_REQUEST_LOCAL_PASS / OUTPUT_SIZE_WITHIN_BUDGET / RAW_REPLAY_PROTECTION_PASS / PILOT_DATA_PRESERVED / P5E_LIVE_PREP_PENDING / RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / RECONCILE_AUTHORIZATION_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Current version/build: validation artifact 4.17-dev.28 / code196 remains installed on device `15e84958` via `adb install -r`, with package data preserved; APK SHA-256 `85345086FBD76FA78133EE54741CA7631EBA91EB4761401080EC10BA1D35042A`, focused test APK SHA-256 `292F30A50302423E695571BB28E95514504F06F174361762919C35FE6D1704DE`. P5E implementation is staged but not yet built; code197 is the next validation artifact. Code191 is reconciled as `EXTERNAL_CONFIRMED_CANCELLED`; metadata does not prove `$0` billing. No new provider call has been made. DB schema remains v24 and the exact VOL5 binding/source data remain intact. Output cap remains `4,096`.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: ba42d65271c0a51722b917440d3b51e1cf6a7eec, the implementation baseline immediately before this snapshot commit; not self-referential. The P5E implementation/docs are staged in this workspace and will be validated in code197.
- Active authority: V5-SAFE.4.1.3-FULL. Authority bytes, canonical ZIP SHA, Java-control ZIP SHA and profile v2 hash are unchanged.

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
- The controlled RAW diagnostic preflight was rerun without provider access: fake recovery/binding class `13/13 PASS`, live recovery inspection `1/1 PASS`, engine expiry/budget/authorization boundary `16/16 PASS`; provider calls `0`. Exact identities and the pre-dispatch snapshot are in `docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`.
- The user-approved RAW diagnostic authorization was consumed once. The single OpenRouter call returned HTTP `200`/complete transport but `finish=length` at `2,048` output tokens; local validation returned `RETRY_OUTPUT_TRUNCATED`, persisted lifecycle reached `RESPONSE_BODY_COMPLETE`, and no partial report/receipt was committed. See `docs/P5D_RAW_DIAGNOSTIC_ATTEMPT_REPORT.md`.
- Test-first output-budget evidence reproduced the silent `4,096 → 2,048` clamp. The exact-cap HTTP body test and invalid-cap fail-closed test now pass after the minimal alignment change. A focused recovery-history test proved v22's single immutable reconciliation row could not retain a later truncated decision; additive v23 append-only history is implemented and awaits device migration/readback verification. No provider call was made for this change.
- Output-budget alignment is now verified: authorization/coordinator/adapter/request propagate the exact `4,096` cap, and v23 append-only reconciliation history preserves the earlier cancelled decision and later truncated decision without replacing the primary row.
- Code191 focused validation is current evidence: VOL5 v23 recovery readback `1/1`, fake E2E `14/14`, schema/migration `41/41`, importer/P1/P2 `23/23`, P3B/P4 `5/5`; host engine `181/181`, app all unit variants `657/657`, external qualification `306/306`; provider calls in alignment/preflight `0`. Full code189 instrumentation `130/130` remains historical; the delay harness was not rerun.
- The exact user-approved RAW acceptance authorization `P5D-VOL5-RAW-ACCEPTANCE-20260909-01` was consumed once. The live test appended the recovery decision for the prior truncated generation while preserving the original immutable cancelled decision; this was test-only gate wiring before dispatch.
- The final focused test APK was rebuilt from `ae6d9e2`, installed with `adb install -r`, and used for redacted readback. The production package remained code191 and the validation database was not reset or uninstalled.
- The approved RAW acceptance dispatched once. The host runner did not reach a terminal assertion by the five-minute authorization deadline; readback showed `CLAIMED` before cleanup, a consumed acceptance receipt, lifecycle `RESPONSE_HEADERS_RECEIVED`/HTTP 200 with generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8`, request bytes `85,068`, and zero report/receipt bytes. The attempt was then closed through the existing attempt-store owner as `RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT`; no second provider call was made and external state remains unknown.
- P5D deadline/body-read hardening is committed and verified: the OpenAI-compatible client has a scoped monotonic deadline, bounded one-pass response-body read with redacted progress bytes, and RAW-only call ownership; the attempt store has additive response-byte persistence and stale-claim recovery. Code196 device tests cover immediate/delayed transport, the short stalled-body case, the 300-second stalled-body case, process-restart stale-claim recovery and no redispatch.
- Code192 device evidence showed the bounded stalled-body test reached a client `SocketInputStream` read after the server had accepted the request; the stack and timing exposed that `EditorialP5CExactBindingExecution`'s counting wrapper dropped `beginAttempt()`. Code193 then showed the deadline was enforced but the Android timeout exception was mapped to `FAILED_UNKNOWN`; the provider now maps an exception observed after its monotonic deadline to the existing typed timeout, while still giving explicit cancellation precedence. Code196 reruns pass, including the five-minute local stalled-body case with one server request and app-owned terminal recovery. The invalid-hex fixture remains corrected without weakening hash validation.
- P5E.1 reconciled generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` from authenticated OpenRouter metadata as `EXTERNAL_CONFIRMED_CANCELLED`: provider OpenAI, input `23,674`, aggregate output `0`, reasoning `0`, generation duration `9,642 ms`, upstream usage `0.0047348`, no completion timestamp/body and no unambiguous billing flag. No `$0` conclusion or authorization was derived from the metadata.
- P5E.2 measured the current full response shape at `49,665` bytes (`23,814` bytes duplicated source plus ledger/gates/identities/evidence/syntax); the four-byte heuristic is `12,417` and cannot justify a `4,096` semantic cap. The compact wire worst case is `2,785` bytes under an explicit `3,584` byte local ceiling, leaving headroom under the `4,096` token cap. Exact tokenization was unavailable; byte results are the acceptance evidence.
- P5E.3-P5E.6 add a separate bounded `safe4.raw.discovery.wire.v1` DTO/parser, app-owned RAW before/after materialization, strict JSON Schema output, minimal reasoning, hard item/ID/ref limits, empty RAW changes and exact attempt/envelope replay binding. Final `safe4.full.report-l1.v1` and receipt schemas are unchanged.
- P5E.7 host engine boundary tests are `192/192 PASS`; app unit tests and Android test compilation passed after the compact-contract changes. No live provider call has been made for P5E.

## Validation evidence

- Host engine XML: `192/192 PASS`, `0` failures, `0` errors, `0` skipped; app unit/build checks and Android test compilation passed; current installed device evidence is code196 and external qualification remains `306 PASS / 0 FAIL` from the prior hardening baseline.
- Code189 full device instrumentation: `130` tests, `0` failures; real provider paths remained opt-in/skipped. Code186 `124` tests remains historical evidence.
- Provider/API calls: code189/code191 and their provider generations are historical evidence only. P5E.1 used authenticated read-only metadata for code191 generation `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` and recorded `EXTERNAL_CONFIRMED_CANCELLED`; no new provider call or authorization was created during local contract work. Prior known costs remain recorded separately and are not collapsed into `$0`.
- Canonical ZIP `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`, Java control `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`, and profile resource `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` re-hash correctly.
- Latest validation APK code196 SHA-256: `85345086FBD76FA78133EE54741CA7631EBA91EB4761401080EC10BA1D35042A`; current focused test APK SHA-256 `292F30A50302423E695571BB28E95514504F06F174361762919C35FE6D1704DE`. All are validation-only artifacts; code191 and earlier remain historical and code169 remains the production baseline. `git diff --check` is required again after this documentation update.

## Pending tasks

- P5E live RAW acceptance is still pending. The new code197 artifact must be built/installed with `adb install -r`, pilot data must be read back, then a preflight must record the reconciled code191 decision and exact compact wire limits before one new authorization and exactly one RAW acceptance. No automatic retry, schema repair, cap increase or RECONCILE authorization is permitted.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No successful P5E app-validated live provider response, real REPORT_L1/receipt, model result or certification evidence exists yet. The current durable VOL5 row remains the old `RECOVERY_REQUIRED / RETRY_PROVIDER_CALL_TIMEOUT` predecessor until the new P5E authorization runs; code191 is now externally classified `EXTERNAL_CONFIRMED_CANCELLED`, while billing remains unresolved and must not be recorded as `$0`.
- P4 creates pilot setup metadata only; `DATA_COMPATIBLE` and selectable status do not mean runnable or certified.
- Initial setup UI collects explicit source text for binding metadata; it does not certify source bytes or open execution.
- Bootstrap profile v1 remains loadable and non-executable.
- The consumed historical VOL4/VOL5 authorizations cannot be reused. The code191 acceptance predecessor has an append-only P5E reconciliation decision classified `EXTERNAL_CONFIRMED_CANCELLED`; its metadata has no completion timestamp/body and does not establish whether billing occurred. OpenRouter I/O logging remains disabled. A new P5E authorization is not reusable and is not created until the code197 preflight gates pass. VOL5 has a separate persisted binding and no RECONCILE authorization exists.
- Current validation device state: code196 is installed with `adb install -r`; the VOL5 recovery row, binding and source setup remain intact, with no uninstall/reset.
- The current VOL5 PRONOUN transport file includes a UTF-8 BOM, but semantic bytes after the existing app-owned removal match the immutable binding. No source rewrite or silent rebind is needed.
- The local delayed harness initially hit a device freezer interruption at `DELAY_STARTED`; bounded cleanup and a test-only foreground keepalive resolved the harness run, but the historical provider cancellation actor remains unknown.
- Code196 focused device evidence is not a P5E live acceptance pass, but local deadline/body-read gates pass: the existing 17 methods, short stalled-body case (`2.069s`) and five-minute stalled-body case (`301.501s`) pass with one server request and no host force-stop. The prior `PROCESS_RESTART_RECOVERY_VERIFIED` label is superseded by `DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED`; the current test evidence does not prove two independent app process invocations.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## Exact next step

2026-09-10 update: P5E implementation is staged on the exact `ba42d652...`
baseline. Code191 has an authenticated metadata decision of
`EXTERNAL_CONFIRMED_CANCELLED`; code189/code191 remain historical evidence.
The current full-shape characterization is `49,665` bytes and compact-wire
worst case is `2,785` bytes under a `3,584` byte local ceiling. Host engine is
`192/192`; app unit/build checks and Android test compilation pass. The exact
VOL5 binding, normalized source identities and schema v24 data remain intact.
Next action: commit this P5E baseline, build code197 with
`scripts/build-and-save.ps1`, install with `adb install -r`, run focused
data-preservation/regression checks, write the preflight, then perform one
new authorized RAW acceptance. Stop before RECONCILE. Until the real RAW
predecessor is committed and read back, keep
`RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED / RECONCILE_AUTHORIZATION_REQUIRED /
EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE`.

This is current-only state; Git history preserves prior snapshots.
