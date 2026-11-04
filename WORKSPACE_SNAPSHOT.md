# Workspace Snapshot

- Updated: 2026-11-04 (+07:00), release 4.25.0 tagged and archived locally.
- Current version: release `4.25.0` / versionCode `329` (phone) and PC `4.25.0` (msi 4.25.200); tag `v4.25.0`. Not installed: phone `15e84958` runs `4.25-dev.9`/328, this PC runs `4.25.3`.
- Current branch: `feature/v4.25.0` in `D:\P5E-builds\wt-v4.25` (release branch from `feature/v4.25` at `e014f8c9`).
- Current commit: `cde799f0` — the tag commit, baseline immediately before this closing documentation commit.
- Current build: APK `4B82F0D5…7B10` (event `build-20261104-204947`); PC msi `F8B51AD8…FA77E`, zip `B65D2D29…FA52C` (event `build-20261104-213322`); both from `e014f8c9`.
- Current phase: release 4.25.0 complete locally (14/14 steps; Complete gate). Product decision served: ship v4.25.
- Completed tasks:
  - Tag `v4.25.0` (object `eb1a0236`) on `cde799f0`; archive events `build-20261104-213906` and `tag-v4.25.0` in artifacts and backup (29 files each, identical, with `pc/`).
  - BUILD_STATE updated; QA report, CHANGELOG, RELEASE_NOTES committed.
- Pending tasks:
  - Owner: merge to `main` and push (`main`, `feature/v4.25`, `feature/v4.25.0`, tag); install APK 4.25.0 on the phone; install msi 4.25.0 on the PC. `feature/v4.25` still points at `e014f8c9` and needs a fast-forward to the release branch.
- Known bugs/limits: emulator cold start +31 ms vs v4.24.1 (NOT_DIAGNOSED); reasoning-bound rule kept; real-phone performance NOT_MEASURED; APK debuggable; msi unsigned; Editorial frozen.
- Regression status: clean archive of `e014f8c9` green (app 766/0/5, golden 14/14, core 67/0/1, jdbc 3, desktop 83, editorial-engine 742/0/1, lint 0 errors); release APK on emulator: chapter-run 10/10, V425 3/3.
- Next action: owner decides whether to merge `feature/v4.25.0` into `main` and push it with the tag; installs of APK 4.25.0 and msi 4.25.0 are separate approvals.
