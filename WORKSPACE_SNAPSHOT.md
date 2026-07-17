# Workspace Snapshot

- Snapshot updated: `2026-07-17 20:22:06 +07:00`
- Current version: `4.8` (`versionCode 47`, development)
- Current branch: `fix/v4.8-import-navigation`
- Current commit: `21fc148c561dac395e85d19cd48fef6c378da3ab` (tagged v4.8 release commit; checklist tag evidence follows)
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
- Normalized both glossary import paths so imported terms are stored as a library profile, activated immediately, reflected in Settings/prompt state, and returned to the glossary list.
- Added focused glossary parsing/merge/prompt unit coverage; `GlossaryImportV48Test` passes (2 tests).
- Made pronoun profile persistence and active selection a single synchronous transaction, and return to the profile list immediately after import/replacement.
- Added deterministic Back navigation: editors return to lists, Library subsections return to Files, Library root returns to Translate, and Settings walks through General before Translate.
- Confirmed by static audit that the four primary pages use ordinary `ScrollView` containers and have no swipe-refresh/touch gesture interceptor.
- Added instrumentation for glossary reload, pronoun cold-store reload, Library/Settings Back, and four-tab vertical gestures with preference snapshot/restore.
- Installed the current v4.8 debug APK on OnePlus CPH2691 / Android 15 through the connected instrumentation run.
- Preserved the development APK, SHA-256, QA report, release documents, and test reports under `artifacts/releases/v4.8/dev-e4b9dfd-20260717-201229/` and a non-overwriting backup mirror.
- Added an opt-in two-request real API harness that asserts both glossary and pronoun locks are present in each provider prompt without logging credentials.
- Ran the opt-in harness directly against the retained OnePlus app data: exactly 2 real API requests completed, and both prompts contained the expected glossary and pronoun locks.
- Added `QA_REPORT_v4_8.md`, v4.8 release notes, and the v4.8 changelog entry; checklist steps 1-9 now have evidence.
- Created annotated local tag `v4.8` after the PreTag gate passed.

## Pending tasks

- Retry pushing all separate commits after GitHub connectivity/authentication is restored.
- Run PreBackup, archive immutable artifacts/backups, update BUILD_STATE and snapshot, then merge according to workflow.
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
- Glossary import unit regression: 2 passed, 0 failed; device reload instrumentation passed.
- Pronoun/glossary focused JVM regression: 4 passed, 0 failed; cold-store instrumentation passed.
- Back/Settings navigation policy regression: 7 passed, 0 failed; device Back/gesture instrumentation passed.
- Full v4.8 JVM regression: 99 passed, 0 failed, 0 skipped.
- Full Android instrumentation on OnePlus CPH2691 / Android 15: 11 passed, 0 failed, 0 skipped, including all 4 new import/navigation/scroll cases.
- With the opt-in API harness included, normal instrumentation reports 11 passed, 0 failed, 1 intentionally skipped.
- Explicit real API run: PASS; 2 requests completed and both provider prompts were verified to contain glossary and pronoun locks.

## Next step

Commit the corrected API QA harness, retry branch push, then prepare and execute the v4.8 tag/backup/artifact gates. Push remains pending because earlier GitHub attempts did not return; `origin` is the approved `manhluongyd/translate-books-android` destination.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
