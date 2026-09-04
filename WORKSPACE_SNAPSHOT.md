# Workspace Snapshot

- Updated: 2026-09-04 (+07:00).
- Current status: P4_COMPLETE / BINDING_RESUME_VERIFIED / PILOT_SETUP_READY / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE.
- Current version/build: device 15e84958 is restored to immutable 4.17-dev.1 / code169. Latest retained P4 validation artifact is 4.17-dev.6 / code174.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: 76348b38174cdc6e25ce4ce19000b75984d44f75, the exact implementation/test baseline immediately before this documentation snapshot; not self-referential.
- Active authority: V5-SAFE.4.1.3-FULL. Authority bytes, canonical ZIP SHA, Java-control ZIP SHA and profile v2 hash are unchanged.

## Completed tasks

- P0/P1 baseline, characterization, four-entry fixture and gap matrix completed.
- P2A canonical reference pack frozen; P2B import acceptance passed.
- P3A GAP-012 importer hardening passed with bounded one-pass drain and EOCD validation retained.
- P3B deterministic contract/profile and trusted compatibility evidence completed: P01-P09 `9/9`, G1-G24 `24/24`, exact 11 capability evidence, profile v2 and execution lock.
- P4 characterization proved the legacy project owner and v18 persistence boundary. P4 added the minimal project-scoped selection/binding owner, additive v19 immutable binding tables, exact source identity, atomic persistence, restart/process-death resume, stale-chain checks and side-by-side 4.1.3/4.1.4 acceptance.
- P4 UI remains setup-only: management is read-only, selection is explicit within new project flow, no global active/latest pack and no Run/Start/Activate path.

## Validation evidence

- Host engine `163/163 PASS`; app unit `210/210 PASS`; external qualification `306 PASS / 0 FAIL`.
- Device focused: importer `13/13`, P1 `7/7`, P2 `3/3`, P3B trusted profile `1/1`, runtime wiring `5/5`, P4 binding `4/4`, process-death proof `1/1 + 1/1`, UI recreation `3/3`.
- Full device instrumentation: `112 total = 111 PASS + 1 approved real-API skip`, `0` failures. The P3B baseline was 104; P4 added eight tests.
- Provider/API calls: `0`; real API remained skipped by explicit opt-in.
- Canonical ZIP `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987`, Java control `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5`, and profile resource `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` re-hash correctly.
- Latest validation APK code174 SHA-256: `97DDFD5901AD7957C3B64780C9037A1BE4B6EB3824B2B4C548B0367523163834`; it was archived in both artifacts and backup and is not a release build.
- `git diff --check`: PASS; scoped production-change guard: PASS with no out-of-scope production/build-metadata path.

## Pending tasks

- P5 controlled real L1 pilot, P6 L2/L3 and P7 release regression/build gates remain separate future phases.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No provider/API, real chapter, model execution or certification evidence exists.
- P4 creates pilot setup metadata only; `DATA_COMPATIBLE` and selectable status do not mean runnable or certified.
- Initial setup UI collects explicit source text for binding metadata; it does not certify source bytes or open execution.
- Bootstrap profile v1 remains loadable and non-executable.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## Exact next step

Hand off only to a separately authorized P5 controlled L1 pilot. Do not open execution, certification, auto-activation or auto-rebind from P4.

This is current-only state; Git history preserves prior snapshots.
