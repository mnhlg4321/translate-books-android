# Workspace Snapshot

- Updated: 2026-09-07 (+07:00).
- Current status: P5C_FAKE_E2E_PASS / P5_DRY_RUN_ONLY / LIVE_AUTHORIZATION_REQUIRED / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE (P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY).
- Current version/build: latest retained P5C validation artifact is 4.17-dev.13 / code181. Device `15e84958` was restored after validation to immutable 4.17-dev.1 / code169 by uninstall/reinstall; validation-package data was removed.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: 61ba7602005522a8c93f2b4335fe6bea0214a367, the exact P5C implementation/test baseline immediately before this evidence snapshot; not self-referential.
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

## Validation evidence

- Host engine `178/178 PASS` (baseline `163`, P5 `+15`); app unit `211/211 PASS` per variant and aggregate `633/633 PASS`; external qualification `306 PASS / 0 FAIL`.
- Device focused P1/P2/P3B/P4/P5C: `22/22 PASS`; full device instrumentation: `117/117 PASS`, `0` failures. Five tests were added by P5C; real API remained opt-in/skipped.
- Provider/API calls: live `0`; fake acceptance calls `2`.
- Canonical ZIP `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`, Java control `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`, and profile resource `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` re-hash correctly.
- Latest validation APK code181 SHA-256: `807D2E0C28BF3F486845562FFEE05B039FBA6D09AFDA566618C6C892979CD0F2`; it was archived in both artifacts and backup and used for the `117/117` instrumentation run. It is not a release build. Device baseline restore was verified with code169 APK SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`.
- `git diff --check`: PASS; scoped production-change guard: PASS with no out-of-scope production/build-metadata path.

## Pending tasks

- Controlled real L1 pilot, P6 L2/L3 and P7 release regression/build gates remain separate future phases. P5C fake E2E is complete; live authorization is still absent.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No live provider/API, real chapter, model execution or certification evidence exists. Fake-provider execution evidence now includes the app-bound attempt owner but remains non-live.
- P4 creates pilot setup metadata only; `DATA_COMPATIBLE` and selectable status do not mean runnable or certified.
- Initial setup UI collects explicit source text for binding metadata; it does not certify source bytes or open execution.
- Bootstrap profile v1 remains loadable and non-executable.
- P5 live authorization block is incomplete; provider/API access remains forbidden and only fake/dry-run work is allowed. Live cancellation/process-death recovery, real provider metrics and real L1 report/receipt commit are not evidenced.
- Validation package data was removed during the required uninstall/reinstall restore because `pm clear` was rejected; the device is now back on code169.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## Exact next step

2026-09-07 source preparation update: user delegated setup using `D:\Ebooks\MERCEDES\VOL 4`, approved egress and caps of USD 0.10 / 5 minutes. Chapter 001 candidate hashes are recorded in `docs/P5C_REAL_SOURCE_PREPARATION.md`. Glossary/pronoun BOM conflicts with the current preflight; device has no configured provider or persisted pilot binding after restore. Resolve these and the 256-output-token boundary before concrete preflight/final live confirmation. Provider calls remain 0; no new regression PASS is claimed.

The validation device is restored to code169. Obtain the complete two-phase P5
authorization block before any separately approved live call. Do not open execution, certification,
auto-activation or auto-rebind from fake evidence.

This is current-only state; Git history preserves prior snapshots.
