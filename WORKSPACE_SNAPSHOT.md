# Workspace Snapshot

- Snapshot updated: `2026-07-17 19:07:02 +07:00`
- Current version: `4.7` (`versionCode 46`)
- Current branch: `main` after the immutable archive workflow merge
- Current commit: implementation baseline `3d925a1`; the snapshot and no-ff merge commits follow this baseline
- Current build: v4.7 debug release archive preserved in `artifacts/releases/v4.7/tag-v4.7` and `backup/v4.7/tag-v4.7`

## Completed tasks

- Initialized the project as a Git repository on `main` and captured the v4.7 source baseline.
- Created and retained annotated tag `v4.7`.
- Added `BUILD_STATE.md`, `RELEASE_NOTES.md`, and normalized `CHANGELOG.md`.
- Established the `main -> feature/vX.Y -> small commits -> regression -> APK -> tag -> merge --no-ff` workflow.
- Enabled repository-local commit template and hooks that block direct commits to `main`, empty commits, invalid branch names, and unclear commit messages.
- Added the resumable `WORKSPACE_SNAPSHOT.md` handoff format and root `AGENTS.md` startup instructions.
- Enforced snapshot freshness before commits: update is required after approximately 30 minutes or before the 10th commit unless the snapshot is already staged.
- Regression-tested that a fresh snapshot is accepted by the commit guard.
- Integrated the completed Workspace Snapshot bootstrap into `main` with a no-ff merge; no application release was created.
- Added an immutable release archiver that requires APK, checksums, QA, release documents, Perfetto, Macrobenchmark evidence/status, screenshots, video, and an exact-ref source ZIP.
- Archived tag `v4.7` in both durable locations without using `build/` as storage.
- Created `project_source_v4.7.zip` from tag `v4.7` at commit `d63005793cf464064d08ed9625d36fb0b9b8c9d8`.
- Verified all 16 archived files, opened the 299-entry source ZIP, matched artifact/backup checksum manifests, and confirmed overwrite protection.
- Recorded the honest v4.7 Macrobenchmark status as `NOT RUN`; Perfetto/frame/PSS evidence remains available.

## Pending tasks

- No application feature group is currently assigned.
- Select the next application version/functional group before creating another feature branch.

## Known bugs

- Warm tab-switch P50 is `20 ms`, above the `16 ms` target.
- Android Lint has 50 warnings and 0 errors in the recorded v4.7 build.
- The full 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.

## Regression status

- v4.7 recorded regression: `PASS WITH KNOWN LIMITATIONS`.
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Workspace Snapshot enforcement: implemented; fresh-snapshot guard regression passed.
- Immutable archive regression: passed checksum, ZIP integrity, artifact/backup parity, and no-overwrite checks.
- Android regression has not been rerun because the current group changes repository workflow only.

## Next step

Read the next requested functional scope, confirm `main` is clean, select its `feature/vX.Y` branch, and update this snapshot when that group completes or a freshness threshold is reached.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
