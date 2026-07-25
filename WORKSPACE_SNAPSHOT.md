# Workspace Snapshot

- Snapshot updated: `2026-07-25` (`v4.14` root README refreshed after approved-logo implementation)
- Current version: `4.14-dev.3` (`versionCode 52`, untagged development build)
- Current branch: `feature/v4.14`
- Current commit: `f8649b5` (README implementation baseline immediately before this snapshot commit)
- Current build: `artifacts/builds/v4.14-dev.3/build-20260725-093258/TranslateBooks-v4.14-dev.3-code52.apk`, SHA-256 `3C732596E3552A092C5B0BE656BC690619B8A36C613DE3CED6B10EEE4B3462DF`; all five payload files match the immutable mirror under `backup/builds/`. Installed and verified on device; it is an untagged development build, not a release archive.

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
- Added mandatory `scripts/build-and-save.ps1`; direct debug APK assembly is blocked unless invoked through the archive-first workflow.
- Added automatic build numbering based on prior archives, the source default, and the connected device versionCode; the first accepted build is `4.14-dev.1`/code50.
- Created and verified immutable matching payloads for event `build-20260725-084757` in both `artifacts/builds/` and `backup/builds/`, including APK, per-build README, BUILD_INFO, SHA-256 manifest, and exact source ZIP.
- Added list-level `Import glossary` parity with Pronoun, exact single-file filename adoption, deterministic multi-file naming, custom-name preservation, and staged parsing before mutating the glossary.
- Added four naming-policy tests; focused `GlossaryImportV48Test` passes 6 tests with 0 failures/skips.
- Committed the Glossary filename-parity implementation as `b9e17cc328e594f317e99a1aae406bd9cef0171f`.
- Built and archived `4.14-dev.2`/code51 under immutable event `build-20260725-090328`; both durable copies and every manifest hash match.
- Passed the full 103-test JVM suite and Android Lint with 54 warnings and 0 errors.
- Installed `4.14-dev.2`/code51 on the connected OnePlus 13R.
- Verified on device that editor import adopts the exact CSV filename, activates the one-term glossary, and preserves the name/selection after a cold restart; restored the user's prior active glossary and removed all QA data.
- Restored the Translate dashboard's scrollable last accepted translation preview with a separate completed-chunk identity.
- Added current chunk plus exact Glossary/Pronoun rule-use metrics sourced from the actual translation/refinement `PromptPlan`, including zero-versus-unknown presentation and persisted runtime restoration.
- Added focused prompt-count, runtime-state, dashboard-label, and Android fixture coverage; the focused JVM group passed.
- Built and archived `4.14-dev.3`/code52 under event `build-20260725-093258`; 106 JVM tests passed, lint reported 54 warnings/0 errors, and every artifact/backup payload hash matched.
- Installed and visually verified the current chunk, exact rule counts, exact-prompt association, and accepted-chunk preview on OnePlus CPH2691 / Android 15 without a provider call; restored the original runtime state byte-for-byte.
- Promoted the approved bright cool logo to a permanent Android resource, launcher/round icon, and in-app top badge; retained both design concepts under `artwork/logo-concepts/`.
- Added a `preBuild` guard that requires the approved logo SHA-256 and all manifest/header references; the guard and 106 JVM tests passed.
- Replaced the obsolete v2.6.x root README diary with a current project landing page covering product capabilities, configuration formats, mandatory archive-first builds, verification, privacy, repository structure, and development workflow.

## Pending tasks

- Run the mandatory archive-first build/install and device launcher/header QA for `v4.14-dev.4`.
- Merge and push the completed v4.14 branch through the required release workflow before expecting GitHub's default-branch landing page to render the new README.
- P0 only: add a controlled device proof for the output-permission-revoked UX (the device shell does not expose a non-destructive SAF URI-grant revocation command).
- Release-only tag/archive evidence and the pre-existing controlled output-permission-loss device proof remain pending.

## Known bugs

- P0 root causes fixed: the picker stripped `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`; external small config files were used at runtime; stale prepared-plan callbacks could keep Start disabled; and the output tree URI was decoded twice between service preparation and materialization, corrupting the persisted-grant identity.
- Controlled output-permission-loss UX still needs a device-side revoke test; the preflight and UI implementation prevent dispatch when `validateTreeWritable` fails.
- Warm tab-switch P50 is `20 ms`, above the `16 ms` target.
- Android Lint has 50 warnings and 0 errors in the recorded v4.7 build.
- The full 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.
- APK and source ZIP archives are deliberately ignored by Git, so they are not present in a clean clone; durable copies remain in the local `artifacts/releases/v4.8/` and `backup/v4.8/` stores.
- Android Lint reports 54 warnings and 0 errors for the accepted v4.14 development build.

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
- v4.14 archive workflow: PASS. PowerShell parser and `git diff --check` passed; direct `assembleDebug` was blocked; 99 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is 4.14-dev.1/code50; artifact/backup parity, checksum manifest, per-build README/JSON, and 323-entry source ZIP passed.
- Glossary filename-parity focused regression: 6 passed, 0 failed, 0 skipped.
- v4.14-dev.2 full regression/build/archive/install: PASS. 103 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is 4.14-dev.2/code51; artifact/backup parity and all manifest hashes passed.
- Glossary filename-parity device QA: PASS. Exact imported filename, one-term activation, and cold-restart persistence verified; prior user state restored after cleanup.
- Runtime preview/rule-usage focused regression: PASS. Prompt counts are taken from the exact injected plan; snapshot round-trip distinguishes unknown from zero; compact dashboard and preview labels passed.
- v4.14-dev.3 full regression/build/archive/install: PASS. 106 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is 4.14-dev.3/code52; artifact/backup parity and all manifest hashes passed.
- Runtime preview/rule-usage device QA: PASS. Exact fixture values and accepted preview rendered without navigation overlap; original user runtime state was restored byte-for-byte after QA.
- Approved-logo focused regression: PASS. `verifyApprovedLogo` ran through `preBuild`, exact resource/source hashes matched, manifest/header references passed, and 106 JVM tests passed.
- README static verification: PASS. `git diff --check` passed, all six relative documentation links resolve, the obsolete v2.6.7 heading and direct Android Studio APK instructions are absent, and only `README.md` was included in commit `f8649b5`.

## Next step

Build/archive/install `v4.14-dev.4` from the clean logo baseline, then verify the installed launcher icon, in-app header logo, package metadata, and preserved user state. Keep v4.14 untagged until release-only evidence and the remaining checklist gates are satisfied.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
