# Workspace Snapshot

- Updated: 2026-11-04 (+07:00), release 4.25.0 attempt stopped at QA (FAILED_REPAIRING).
- Current version: release candidate `4.25.0` / Android versionCode `329` (built, not tagged, not installed). Phone `15e84958` still runs `4.25-dev.9`/328; this PC runs `4.25.3` (package `4.25-pc-dev.3`).
- Current branch: `feature/v4.25.0` (release branch, created from `feature/v4.25` at `e014f8c9`) in `D:\P5E-builds\wt-v4.25`. `feature/v4.25` stays at `e014f8c9`.
- Current commit: `e014f8c9f72c5eb16cb4daa9f5e1e67bb8f36b6c` — implementation baseline immediately before this QA/snapshot commit (it adds the single delete notice on the PC Library pages).
- Current build: release APK `4.25.0`/329, event `build-20261104-204947`, APK SHA-256 `4B82F0D58B00AF391E9E53879F0717870C28BBA742A1F9475056C63A54FF7B10`, source ZIP `D81344A539742694072A9D91F0AE859074BED2C79443A65587E65E63B597B4A9`, commit `e014f8c9`; AndroidTest event `device-v4250-release-20261104`. PC release package not built.
- Current phase: release 4.25.0 QA, FAILED_REPAIRING. Product decision served: whether v4.25 ships; blocked on the truncation path of `reasoningEffort=auto` (see Known bugs).
- Completed tasks:
  - Duplicated delete notice fixed (`e014f8c9`).
  - Clean-archive regression of `e014f8c9`: app 766/0/5, translation-core 67/0/1, jdbc 3/0, desktop 83/0, editorial-engine 742/0/1, golden 14/14, androidTest compile, lint 0 errors; Editorial diff vs `v4.24.1` empty.
  - Release APK and pinned AndroidTest APK built; emulator: three V425 device tests pass, `V423ChapterRunInstrumentedTest` 9/10.
- Pending tasks:
  - Owner decision and repair of the failing split test (below), then rerun QA, Macrobenchmark/Perfetto A/B vs v4.24.1, screenshots (light/dark), video, PC release package, QA report, PreTag, tag, archive, BUILD_STATE, Complete gate.
  - Merge/push and any install still need separate owner approval.
- Known bugs/limits: `aChunkCutAtEveryLimitIsSplitInHalvesAndTheChapterCompletes` fails on 4.25.0 and on dev.9 (3/3 + control); chapter 1 ends with `FINISH_REASON_LENGTH` at 1154/1154 without a raised limit or split. Inferred cause: `isReasoningBound` counts a short visible answer as reasoning-bound and, with `reasoningEffort=auto` and no lower effort, the attempt loop stops. Why the class passed on 2026-11-03 is NOT_DIAGNOSED. 19 PC RAW deleted in the owner's app session at 19:21 (backup kept; owner not yet asked to restore).
- Regression status: clean archive of `e014f8c9` green (counts above); device: 3/3 V425 tests pass, chapter-run class 9/10.
- Next action: owner decides how a short visible cut under `reasoningEffort=auto` is handled (raise the limit and split for non-reasoning models, or keep ending the chunk); then repair, rerun the chapter-run class and continue the release plan.
