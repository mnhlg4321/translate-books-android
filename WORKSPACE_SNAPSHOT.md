# Workspace Snapshot

- Updated: 2026-09-09 (+07:00).
- Current status: P5D_DOCUMENTATION_BASELINE_CONSISTENT / P5D_EXTERNAL_AUDIT_COMPLETE / P5D_LIFECYCLE_HARDENING_PASS / P5D_REGRESSION_PASS / LOCAL_TRANSPORT_AND_LIFECYCLE_VERIFIED / HISTORICAL_CANCELLATION_CAUSE_UNRESOLVED / RAW_DIAGNOSTIC_RETRY_READY / VOL5_RECOVERY_REQUIRED / EXTERNAL_CONFIRMED_CANCELLED / NEW_RAW_AUTHORIZATION_REQUIRED / P5D_CODE189_PROVIDER_CALLS_0 / RECONCILE_NOT_AUTHORIZED / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_GLOBALLY_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Current version/build: validation artifact 4.17-dev.21 / code189 was archived in artifacts and backup with SHA-256 `D66C3C816E29403508BF997413998683FDCAD6AC2C74F24DA40E9AF31570860C` and installed on device `15e84958`. One historical VOL5 RAW request remains `RECOVERY_REQUIRED` with no response/report/receipt; OpenRouter metadata confirms `cancelled` at displayed cost `$0.00484`. Code189 local transport/recorder validation passed. The current PRONOUN file is 455 bytes/hash `63E79EEB…1A49C` with a UTF-8 BOM; after the existing BOM removal it is 452 bytes/hash `4947FF91…20686`, exactly matching the binding. No retry or RECONCILE request is authorized.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: 81c6c3e2a7cf5c6fc53c210c762ee0ab6bd9c37b, the implementation/documentation baseline immediately before this preflight documentation snapshot commit; not self-referential.
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
- The controlled RAW diagnostic preflight was rerun without provider access: fake recovery/binding class `13/13 PASS`, live recovery inspection `1/1 PASS`, engine expiry/budget/authorization boundary `16/16 PASS`; provider calls `0`. Exact identities and the unissued new authorization draft are in `docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`.

## Validation evidence

- Host engine/app debug unit XML: `396` tests, `0` failures, `0` errors, `0` skipped; external qualification `306 PASS / 0 FAIL`.
- Code189 full device instrumentation: `130` tests, `0` failures; real provider paths remained opt-in/skipped. Code186 `124` tests remains historical evidence.
- Provider/API calls: historical VOL4 had one authorized RAW request; current VOL5 had one authorized RAW request; both matching OpenRouter generations are `cancelled` and neither produced an app-validated response/usage receipt. VOL5 displayed cost was `$0.00484`; fake acceptance calls remain `2`; no automatic retry or RECONCILE call.
- Canonical ZIP `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`, Java control `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`, and profile resource `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` re-hash correctly.
- Latest validation APK code189 SHA-256: `D66C3C816E29403508BF997413998683FDCAD6AC2C74F24DA40E9AF31570860C`; test APK SHA-256 `30EADECFA740326F5A3036584DB2F8613C90633E0DAF19DCF9472C0001EF2165`. Both are validation-only artifacts; code186/code188 remain historical and code169 remains the production baseline. `git diff --check` is required again after this documentation update.

## Pending tasks

- Controlled real L1 pilot remains incomplete after provider-confirmed cancelled VOL4 and VOL5 generations. P5D.1/P5D.2 and P5D.3/P5D.4 pass. VOL5 is now `RECOVERY_REQUIRED`; the consumed RAW authorization cannot be reused, and a new exact-phase authorization with explicit duplicate/billing-risk acknowledgement is required before another RAW dispatch. P5D.7, P6 L2/L3 and P7 release gates remain separate.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No successful app-validated live provider response, real REPORT_L1/receipt, model result or certification evidence exists. The current VOL5 attempt is durably `RECOVERY_REQUIRED` with local reason `RETRY_PROVIDER_CALL_FAILED_UNKNOWN`; OpenRouter confirms the matching generation was `cancelled` with displayed usage cost `$0.00484`, but the app did not receive a usable response.
- P4 creates pilot setup metadata only; `DATA_COMPATIBLE` and selectable status do not mean runnable or certified.
- Initial setup UI collects explicit source text for binding metadata; it does not certify source bytes or open execution.
- Bootstrap profile v1 remains loadable and non-executable.
- The consumed VOL4 and VOL5 authorizations cannot be reused. VOL5 has a separate persisted binding and no RECONCILE authorization exists. OpenRouter I/O logging remains disabled. Device remains on validation code189 with the exact recovery row and setup data; certification remains unproven.
- The current VOL5 PRONOUN transport file includes a UTF-8 BOM, but semantic bytes after the existing app-owned removal match the immutable binding. No source rewrite or silent rebind is needed.
- The local delayed harness initially hit a device freezer interruption at `DELAY_STARTED`; bounded cleanup and a test-only foreground keepalive resolved the harness run, but the historical provider cancellation actor remains unknown.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## Exact next step

2026-09-08 validation update: user delegated setup using `D:\Ebooks\MERCEDES\VOL 4`, approved egress and caps of USD 0.10 / 5 minutes, selected OpenRouter and explicitly confirmed the live call. The persisted binding for chapter `001` passed; RAW dispatched one request and stopped after 179728 ms without response. Recovery inspection `1/1` confirmed no report/receipt bytes, the P5D lifecycle/recovery matrix passed, and the device was restored to code169.

Next action is to await explicit approval of the exact-phase, single-use RAW
diagnostic authorization in `docs/P5D_RAW_DIAGNOSTIC_PREFLIGHT.md`, which
acknowledges the provider-confirmed cancellation and duplicate-work/billing
risk. Do not reuse the consumed authorization, do not call RECONCILE, and do
not enable content logging. Keep the recovery gate closed for dispatch and do
not infer P6 readiness.

This is current-only state; Git history preserves prior snapshots.
