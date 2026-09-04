# Workspace Snapshot

- Updated: 2026-09-04 (+07:00).
- Current status: P4_IN_PROGRESS / PACK_READY_FOR_CERTIFICATION / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE.
- Current version/build: device 15e84958 temporarily has validation 4.17-dev.3 / code171 for P4 entry instrumentation; baseline code169 will be restored before closure. The validation artifact remains retained in artifacts/builds and backup/builds, not treated as a release.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: 270759e5589b2e9101c3e1a5a6b84cff12ec2fd3, the exact implementation/test baseline immediately before this snapshot update; not self-referential.
- Active authority: V5-SAFE.4.1.3-FULL. Authority bytes and canonical/control ZIP hashes are unchanged; see BUILD_STATE.md and docs/P3B_VALIDATION_REPORT.md.

## Completed tasks

- P0/P1 baseline, characterization, four-entry fixture and gap matrix completed.
- P2A canonical reference pack frozen; P2B import acceptance passed.
- P3A GAP-012 importer hardening passed with bounded one-pass drain and EOCD validation retained.
- P3B deterministic contract types, source preflight P01-P09, full-bundle/phase projection, explicit Pronoun status, typed stop/recovery, PRESERVE_DRAFT, receipt/ledger/diff/QA/release validators and G1-G24 replay completed.
- Trusted profile v2 generated and bundled with exact 11 implemented capabilities and no explicitly missing capability. Machine contract, evidence fingerprints, canonical profile hash and source commit are pinned.
- Focused importer/P1/P2/P3B/runtime-wiring device tests passed; full instrumentation passed with one approved real-API skip; device restored to code169.
- Host engine 161/161, app unit 210/210 and external static 306/306 passed. Provider/API calls: 0.
- P4 characterization tests were added and intentionally reproduce two gaps: the v17 project revision/scope canonical objects do not retain the P4 binding/source facts, and the v18 source-entry schema has no source reference, encoding or schema-status columns. The characterization test commit is staged but not yet committed because the snapshot hook required this update.

## Pending tasks

- P4 binding/resume and persistence contract implementation, acceptance tests, and documentation remain in progress. P5 controlled real L1 pilot, P6 L2/L3, and P7 release regression/build gates remain separate future phases.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No provider/API, real chapter, model benchmark, execution or certification evidence exists.
- Profile compatibility is deterministic and fail-closed but does not make 4.1.3 runnable.
- Project/run binding, immutable run-input persistence and execution state transitions are not implemented in P3B.
- P4 characterization currently proves that imported-pack selection is rejected by the legacy project owner and that v18 cannot retain the full source identity tuple without an additive persistence owner/schema change.
- Bootstrap profile v1 remains loadable and non-executable.

## Regression status

- Host baseline before P4 tests: engine 161/161 PASS; app unit 210/210 PASS; profile asset verification PASS. New P4 characterization engine class: 2/2 intentionally FAIL as entry-gate evidence; no production fix has been applied.
- Device baseline before P4 tests: importer 13/13, P1 7/7, P2 3/3, P3B trusted profile 1/1 and runtime wiring 5/5 PASS. The focused connected task was blocked before test execution because Gradle attempted to install generated debug code48 over installed validation code171; this is an installer/version-downgrade condition, not a test result.
- Full instrumentation baseline: 104 total, 103 PASS, 1 approved real-API skip, 0 failures before P4 changes. No P4 full-suite run has been claimed.
- External qualification: 306 PASS / 0 FAIL. Real API remains skipped by opt-in; provider/API calls 0.
- git diff --check and production-change guard pass at closure.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its
user-owned changes and checkout were not reset, staged or modified.

## Exact next step

Implement only the P4 binding owner/persistence extension justified by the
failing characterization tests, then add explicit-selection, exact-readback,
restart-resume, stale-chain and side-by-side acceptance tests. Do not infer
execution, certification, auto-activation or auto-rebind from DATA_COMPATIBLE.

This is current-only state; Git history preserves prior snapshots.
