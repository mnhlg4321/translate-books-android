# Workspace Snapshot

- Snapshot updated: `2026-07-17 18:58:35 +07:00`
- Current version: `4.7` (`versionCode 46`)
- Current branch: `feature/v4.9`
- Current commit: `cfbf742a22301911addff284f3bd073cf423bc1d` (baseline before this snapshot update)
- Current build: existing v4.7 debug APK; SHA-256 `BBA8152197FCE0310236899BA313B7C285E91FA1299D703EEF4BB23FA316EE4F`

## Completed tasks

- Initialized the project as a Git repository on `main` and captured the v4.7 source baseline.
- Created and retained annotated tag `v4.7`.
- Added `BUILD_STATE.md`, `RELEASE_NOTES.md`, and normalized `CHANGELOG.md`.
- Established the `main -> feature/vX.Y -> small commits -> regression -> APK -> tag -> merge --no-ff` workflow.
- Enabled repository-local commit template and hooks that block direct commits to `main`, empty commits, invalid branch names, and unclear commit messages.
- Started Workspace Snapshot setup on `feature/v4.9`.

## Pending tasks

- Add an automated freshness check for the 30-minute and 10-commit snapshot thresholds.
- Regression-test snapshot freshness enforcement.
- Update this snapshot after the enforcement group is complete.
- Merge the snapshot bootstrap into `main` without creating an application release tag.

## Known bugs

- Warm tab-switch P50 is `20 ms`, above the `16 ms` target.
- Android Lint has 50 warnings and 0 errors in the recorded v4.7 build.
- The full 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.

## Regression status

- v4.7 recorded regression: `PASS WITH KNOWN LIMITATIONS`.
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Workspace Snapshot enforcement: not yet implemented at this snapshot point.
- Android regression has not been rerun because the current group changes repository workflow only.

## Next step

Implement and test the snapshot freshness guard, then refresh this file before integrating the completed workflow group into `main`.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.

