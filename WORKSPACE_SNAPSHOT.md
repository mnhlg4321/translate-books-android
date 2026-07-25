# Workspace Snapshot

- Snapshot updated: `2026-07-25` (first versioned-build attempt failed safely before Gradle; null handling fixed, retry pending)
- Current version: `4.13` (`versionCode 48`, untagged development hotfix)
- Current branch: `feature/v4.14`
- Current commit: `60af8ae6aa4b2f53d019f416a991d44b207af547` (implementation baseline before this snapshot update; actual branch has uncommitted v4.14 workflow work)
- Current build: `app/build/outputs/apk/debug/TranslateBooks-v4.13-p0-dev-debug.apk`, current local SHA-256 `0887FDD67E8F0D9E8B8CCC4EEE4A73EA023E1BFE720428B18EEF490BD3C49C8A`; the historical device-verified build recorded a different SHA-256, so this local APK must not be treated as that evidence or as a release archive.

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
- Created local annotated tag `v4.8` and immutable release archive/backup, including source ZIP and required evidence.
- Added an opt-in two-request real API harness that asserts both glossary and pronoun locks are present in each provider prompt without logging credentials.
- Ran the opt-in harness directly against the retained OnePlus app data: exactly 2 real API requests completed, and both prompts contained the expected glossary and pronoun locks.
- Added `QA_REPORT_v4_8.md`, v4.8 release notes, and the v4.8 changelog entry; checklist steps 1-9 now have evidence.
- Created annotated local tag `v4.8` after the PreTag gate passed.
- Verified remote ancestry with no divergence (`0` remote-only / `12` local-only commits), then pushed `main` and annotated tag `v4.8` without force.
- Verified GitHub refs: `main` at `c1e807b3bad1b64f079d0a802c3b0a2e7621c0c2`; tag object `e5e6bb8aec6c713edf87354b8b76d21e004ea726`; tag target `21fc148c561dac395e85d19cd48fef6c378da3ab`.
- Clean-cloned tag `v4.8`, confirmed versionName `4.8`, versionCode `47`, all release/snapshot documents, and no sensitive path or credential-pattern matches across 31 reachable commits.

## Pending tasks

- P0 only: add a controlled device proof for the output-permission-revoked UX (the device shell does not expose a non-destructive SAF URI-grant revocation command).
- Commit the v4.14 numbered-build workflow, run its full unit/lint/build path, verify the first dual archive and per-build README/checksums/source ZIP, then optionally install only from the archived APK.

## Known bugs

- P0 root causes fixed: the picker stripped `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`; external small config files were used at runtime; stale prepared-plan callbacks could keep Start disabled; and the output tree URI was decoded twice between service preparation and materialization, corrupting the persisted-grant identity.
- Controlled output-permission-loss UX still needs a device-side revoke test; the preflight and UI implementation prevent dispatch when `validateTreeWritable` fails.
- Warm tab-switch P50 is `20 ms`, above the `16 ms` target.
- Android Lint has 50 warnings and 0 errors in the recorded v4.7 build.
- The full 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.
- APK and source ZIP archives are deliberately ignored by Git, so they are not present in a clean clone; durable copies remain in the local `artifacts/releases/v4.8/` and `backup/v4.8/` stores.
- The new v4.14 build/archive workflow has passed syntax and direct-build-block checks but has not yet completed its first full versioned build. Its first run stopped safely before Gradle because clean Git state returned a null snapshot ref; the fix is implemented and pending retry.

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
- v4.13 P0 device regression: PASS for fresh SAF output selection (persisted read/write plus write/delete probe), YAML/glossary/pronoun valid state, real Start/preflight/service/request/response/partial output/completed sequence, and cold restart followed by a second real output. JVM unit tests: 99 passed. Connected instrumentation on OnePlus 13R: 12 passed, 0 failed, 1 intentionally skipped real-API test.
- v4.14 workflow checks so far: PowerShell parser PASS; `git diff --check` PASS; direct `assembleDebug` correctly blocked. First versioned-build attempt created no APK/archive and exposed a clean-Git null-handling bug; the fix is pending commit and full retry.

## Next step

Commit the clean-Git null-handling fix on `feature/v4.14`, then rerun `scripts/build-and-save.ps1` to create and verify the first `4.14-dev.1` artifact/backup pair. Do not tag it as a release until all release criteria are satisfied.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
