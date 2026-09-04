# Workspace Snapshot

- Updated: 2026-09-04 (+07:00).
- Current status: P4_IN_PROGRESS / PACK_READY_FOR_CERTIFICATION / EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE.
- Current version/build: validation 4.17-dev.5 / code173 is the latest retained P4 build; device 15e84958 is temporarily on code173 and will be restored to immutable baseline 4.17-dev.1 / code169 before closure.
- Current branch/workspace: feature/v4.18 / D:\App Translate Books\App Translate Books-translation-profile.
- Current commit baseline: 3d82ca87e614fd5c49aa124bf0bc2c7c183b4564, the exact implementation/test baseline immediately before this snapshot update; not self-referential.
- Active authority: V5-SAFE.4.1.3-FULL. Authority bytes, canonical ZIP SHA, Java-control ZIP SHA and profile v2 hash are unchanged.

## Completed tasks

- P0/P1 baseline, characterization, four-entry fixture and gap matrix completed.
- P2A canonical reference pack frozen; P2B import acceptance passed.
- P3A GAP-012 importer hardening passed with bounded one-pass drain and EOCD validation retained.
- P3B deterministic contract/profile and trusted compatibility evidence completed: P01-P09 `9/9`, G1-G24 `24/24`, exact 11 capability evidence, profile v2, and execution lock.
- P4 characterization proved the legacy repository cannot bind an imported 4.1.3 pack and v18 does not retain the full binding/source tuple. P4 now has additive v19 binding tables, explicit selection policy, project-scoped setup UI, exact binding readback, restart resume, stale-chain detection, side-by-side 4.1.3/4.1.4 and collision rollback evidence.
- P4 focused device acceptance passed `3/3` before the final atomicity test; the final test is compiled and awaits code174 validation rerun.

## Pending tasks

- Run code174 focused/full instrumented validation including the atomic rollback test, then restore the device to code169.
- Complete P4 validation report, gap map, BUILD_STATE, checklist and final acceptance commit.
- P5 controlled real L1 pilot, P6 L2/L3 and P7 release regression/build gates remain separate future phases.
- Release tag, release backup/export and real-chapter certification remain pending by design.

## Known bugs and limitations

- No provider/API, real chapter, model execution or certification evidence exists; provider/API calls remain `0`.
- P4 creates pilot setup metadata only. `DATA_COMPATIBLE` and selectable status do not mean runnable or certified; execution remains disabled and certification remains `NOT_CERTIFIED`.
- Initial P4 setup UI collects explicit setup input text for the four source roles; it does not open execution or certify the source.
- Activity/process recovery is implemented without global active-pack state and is evidenced by exact binding readback after database close/reopen; a separate live activity recreation assertion is not required to mutate persisted state.
- Bootstrap profile v1 remains loadable and non-executable.

## Regression status

- Host engine focused P4 characterization and app compile pass. Prior host suites: engine `161/161`, app unit `210/210`, external qualification `306/306`.
- Device prior to the atomicity addition: importer `13/13`, P1 `7/7`, P2 `3/3`, P3B trusted profile `1/1`, runtime wiring `5/5`, P4 binding `3/3`; full instrumentation `109` total = `108 PASS + 1 approved real-API skip`, `0` failures. The atomicity test adds one instrumented case, so the next full run is expected to be `110` total = `109 PASS + 1 approved skip` if it passes.
- Real API remains skipped by explicit opt-in. No provider/API call was made.
- `git diff --check` passed before the snapshot update; production-change guard remains limited to P4 owners plus tests/docs.

## Protected state

The original workspace D:\App Translate Books remains untouched. Its user-owned changes and checkout were not reset, staged or modified.

## Exact next step

Stage this snapshot with the atomicity test, commit the test evidence, build validation code174 with the required archival script, run focused/full instrumentation, restore code169, and then close P4 without opening execution or certification.

This is current-only state; Git history preserves prior snapshots.
