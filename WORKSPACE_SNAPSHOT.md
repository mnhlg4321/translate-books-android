# Workspace Snapshot

- Updated: 2026-09-08 (+07:00).
- Current status: P5D_DOCUMENTATION_BASELINE_CONSISTENT / P5C_LIVE_AUTHORIZED_ATTEMPT / P5C_PILOT_STOPPED_WITH_PROVIDER_ERROR / STOP_PROVIDER_TIMEOUT / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Current version/build: validation artifact 4.17-dev.16 / code184 was archived and passed focused/full instrumentation on device `15e84958`; the device is now restored to 4.17-dev.1 / code169. One authorized OpenRouter RAW request stopped with no response; no report/receipt was committed.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: e5733cf docs(editorial): record authorized P5C provider stop, the exact starting commit for P5D documentation hygiene; not self-referential.
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
- P5D.0 documentation gate is complete in the working tree: the P4 starting commit is corrected, code184 is current/latest, code181/code177/code176 are labeled historical, and PASS/SKIP/FAIL wording is separated. No provider call is permitted in P5D.0-P5D.3.

## Validation evidence

- Host engine `178/178 PASS` (baseline `163`, P5 `+15`); app unit `211/211 PASS` per variant and aggregate `633/633 PASS`; external qualification `306 PASS / 0 FAIL`.
- Device focused P1/P2/P3B/P4/P5C: `22/22 PASS`; full device instrumentation: `117/117 PASS`, `0` failures. Five tests were added by P5C; real API remained opt-in/skipped.
- Provider/API calls: one authorized live RAW request dispatched, no response/usage receipt; fake acceptance calls `2`; no automatic retry or RECONCILE call.
- Canonical ZIP `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`, Java control `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`, and profile resource `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` re-hash correctly.
- Latest validation APK code184 SHA-256: `AA69A6EDD8A7B11D8488FC431B71097515FE6BD738710C970F6E4A8F54150797`; archived in both artifacts and backup and used for focused/full validation. It is not a release build. Code181 remains the previous full `117/117` instrumentation artifact; code184 is the current full `118/118` result. The device was restored by uninstall/reinstall to code169 APK SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`; validation-package data was removed.
- `git diff --check`: PASS; scoped production-change guard: PASS with no out-of-scope production/build-metadata path.

## Pending tasks

- Controlled real L1 pilot remains incomplete after a legitimate provider stop; P5D.1-P5D.4 provider-state reconciliation/hardening are next. P6 L2/L3 and P7 release gates remain separate; a retry needs provider-account inspection and new explicit authorization.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No successful live provider response, real REPORT_L1/receipt, model result or certification evidence exists. One live request was dispatched and stopped with `RETRY_PROVIDER_CALL_FAILED`; billing and external acceptance remain unknown.
- P4 creates pilot setup metadata only; `DATA_COMPATIBLE` and selectable status do not mean runnable or certified.
- Initial setup UI collects explicit source text for binding metadata; it does not certify source bytes or open execution.
- Bootstrap profile v1 remains loadable and non-executable.
- The consumed live authorization cannot be reused. Live cancellation/process-death recovery, response usage, billing, RECONCILE and final report/receipt commit remain unproven; no automatic retry is allowed while external call state is unknown. P5D.1-P5D.3 must not call the provider.
- The prior validation package data was removed during the required uninstall/reinstall restore because `pm clear` was rejected. Device is back on code169; code184 remains archived for evidence.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## Exact next step

2026-09-08 validation update: user delegated setup using `D:\Ebooks\MERCEDES\VOL 4`, approved egress and caps of USD 0.10 / 5 minutes, selected OpenRouter and explicitly confirmed the live call. The persisted binding for chapter `001` passed; RAW dispatched one request and stopped after 179728 ms without response. Recovery inspection `1/1` confirmed no report/receipt bytes, and the device was restored to code169.

Next action is the bounded read-only OpenRouter Activity audit for the old
request, without enabling content logging or making a provider call. Record a
redacted `EXTERNAL_*` classification, then complete P5D.3/P5D.4. Only after
those gates may a new exact-phase RAW authorization be considered; do not
reuse authorization, call RECONCILE automatically or infer P6 readiness.

This is current-only state; Git history preserves prior snapshots.
