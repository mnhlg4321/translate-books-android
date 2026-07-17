# Workspace Snapshot

- Snapshot updated: `2026-07-17 19:54:12 +07:00`
- Current version: `4.8` (`versionCode 47`, development)
- Current branch: `fix/v4.8-import-navigation`
- Current commit: baseline `e2f3b916bd504cff40ba44e94b4c0b274724cd8b`; the v4.8 baseline commit follows it
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
- Defined the fixed 14-step Development Workflow required for every v4.8+ session.
- Added a reusable release checklist that requires concrete Evidence for every completed step.
- Added executable `PreTag`, `PreBackup`, and `Complete` gates; v4.8+ tag/archive operations require a matching checklist version.
- Added a guarded annotated-tag wrapper and integrated the PreBackup gate into the immutable archiver.
- Regression-tested successful gates plus rejection of incomplete checklists, failed Regression, and version mismatch.
- Created pre-cleanup ZIP backup `backup/pre-git/project_source_v4.7_pre-git_20260717-192534.zip` with SHA-256 `2544FF1363F78D7B1A9960C3C234898D35676664716161E2D3BD6F544D16A94C`.
- Hardened `.gitignore`, removed tracked IDE cache/device state, and sanitized the sample API key to `REPLACE_ME`.
- Confirmed no tracked build/cache/local.properties files, no credential-pattern matches, no remote, and preserved immutable tag `v4.7`.
- Audited version history: v4.7/code46 is the last product release; feature/v4.8 through feature/v4.12 were infrastructure-only branches with no app-code changes.
- Started the focused v4.8/code47 import-navigation cycle on `fix/v4.8-import-navigation`.

## Pending tasks

- Fix glossary import persistence, activation, immediate UI refresh, and Back behavior.
- Fix pronoun import persistence, activation, immediate profile display, and cold-start retention.
- Validate Library/Settings Back navigation and four-tab scrolling without gesture refresh.
- Add focused unit/instrumentation coverage, device QA, and a real 2–3 chunk API prompt verification.
- Do not start batch or refinement work in this cycle.

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
- v4.8+ workflow gate regression: passed; negative cases were rejected as designed.
- Repository hygiene regression: passed tracked-file, secret-pattern, and ignore-rule checks.
- Android regression has not been rerun because the current group changes repository workflow only.

## Next step

Inspect the existing glossary/pronoun import and navigation code, then implement the glossary group first without changing translation core, dashboard, SAF, session isolation, or recovery.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
