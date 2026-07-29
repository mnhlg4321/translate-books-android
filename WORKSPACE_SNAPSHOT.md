# Workspace Snapshot

- Snapshot updated: `2026-07-29` (`v4.15` exact release candidate passed QA; corrected annotated tag and immutable archive verified; BUILD_STATE update pending)
- Current version: exact release candidate `4.15`/code62 passed release QA, is tagged by corrected annotated tag `v4.15`, and has a verified immutable tag archive. The released baseline remains `4.14`/code57 until the remaining release-state workflow completes.
- Current branch: `feature/v4.15`; the pre-existing `.idea/gradle.xml` change is preserved and excluded from product commits.
- Current commit: `bd7e6c3` (corrected-tag documentation baseline immediately before this backup-state snapshot commit; actual `HEAD` must be confirmed when resuming)
- Current build: `artifacts/releases/v4.15/tag-v4.15/TranslateBooks-v4.15-code62.apk`, SHA-256 `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`; exact code62 is installed after QA and the 22-file tag payload is mirrored under `backup/v4.15/tag-v4.15/`. The released baseline remains immutable v4.14/code57 until release-state completion.

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
- Built and archived `4.14-dev.4`/code53 from exact source commit `ca51d7e` under immutable event `build-20260725-095315`; all artifact/backup payload hashes match.
- Installed `4.14-dev.4` and verified the approved launcher icon and in-app header logo on OnePlus CPH2691 / Android 15; the user runtime-state hash remained byte-for-byte unchanged.
- Added exact-release build mode so `v4.14` can produce `versionName 4.14` with the next unused versionCode while retaining the mandatory archive-first workflow.
- Exact-release builds now require the matching clean feature branch and reject conflicting series arguments, mismatched branches, existing tags, or duplicate archived release versions.
- Added a dedicated AndroidX Macrobenchmark 1.4.1 test module targeting a non-debuggable, profileable benchmark app variant with ProfileInstaller 1.4.1.
- Added a five-iteration physical-device cold-start benchmark that produces real StartupTiming metrics, AndroidX Benchmark JSON, and Perfetto traces.
- Prepared the v4.14 changelog/release notes and corrected the README release-candidate identity without inventing final hashes or unrun QA results.
- With explicit user approval, deleted the invalid lightweight `v4.14` tag from GitHub and local; verified that neither ref remains before resuming the release build.
- Built and archived exact `4.14`/code54 from commit `0569036` under event `build-20260725-101846`; all artifact/backup payload hashes match.
- Connected Android instrumentation passed 12 tests with 1 opt-in real-API test skipped on OnePlus CPH2691 / Android 15.
- Identified the Macrobenchmark install failure as an unsigned benchmark test APK and added the missing debug signing configuration.
- Built and archived exact `4.14`/code55 from commit `b53d209` under event `build-20260725-102329`; all artifact/backup payload hashes match.
- Signed Macrobenchmark installed successfully and captured two real startup Perfetto traces before the run was stopped after the OnePlus launcher delayed each setup Home action for nearly 10 minutes.
- Removed the redundant Home setup action; `StartupMode.COLD` already force-stops the target before each measured launch.
- Built and archived exact `4.14`/code56 from commit `59b20f1` under event `build-20260725-103832`; all artifact/backup payload hashes match.
- Diagnosed OnePlus Hans freezing the background Macrobenchmark runner even while it held a partial wake lock; standard device-idle whitelisting did not bypass the OEM freezer and was fully reverted.
- Added a benchmark-only foreground host activity, retained a bounded wake lock, and returned the host to the foreground immediately after each measured launch.
- Suppressed the first-run notification request only in the non-product `benchmark` build type so StartupTiming measures `MainActivity`, not the Android permission controller.
- Corrected diagnostic Macrobenchmark passed 5/5 physical-device cold starts with 0 failures/skips and produced AndroidX JSON plus five Perfetto traces. Time to initial display was 293.57 ms minimum, 332.01 ms median, and 441.19 ms maximum.
- Committed the OEM-safe benchmark host and benchmark-only notification-dialog suppression as `82d5e4a`.
- Built, archived, verified, and installed exact `4.14`/code57 from `82d5e4a` under event `build-20260725-111133`; all five build payloads match the backup.
- Passed 106 JVM tests, logo guard, lint (0 errors/54 warnings), and connected instrumentation (11 passed/1 opt-in paid case skipped).
- Accepted code57 Macrobenchmark rerun passed 5/5 physical-device cold starts at 307.78/322.81/337.52 ms min/median/max and retained five real Perfetto traces plus AndroidX JSON in both durable stores.
- Passed controlled output-permission-loss QA with a valid prepared local TXT, exact missing-write-grant blocker, `Choose output folder again`, and Start disabled; no provider request occurred and QA-created app state was removed.
- Retained final Translate/Library screenshots and a 24-frame MJPEG AVI from timed actual device captures; all visual files match their backup copies.
- Passed the PreTag workflow gate and created annotated tag `v4.14` at release-document commit `8a3b281`.
- Passed the PreBackup gate and created immutable event `tag-v4.14` from the annotated tag.
- Verified both 20-file release payloads by relative path, length, and SHA-256; all 19 manifest entries pass.
- Regenerated `project_source_v4.14.zip` directly from tag `v4.14` and confirmed exact SHA-256 equality with the retained source ZIP.
- Merged `feature/v4.14` into `main` with `--no-ff`, pushed `main` and annotated `v4.14`, and verified remote release merge `41cc6e9`, tag object `246de34`, and tag target `8a3b281`.
- Started the v4.15 cycle from clean `main` at `9199733`, created `feature/v4.15`, and restored the user's unrelated `.idea/gradle.xml` change after branch creation.
- Added a Settings hydration guard so programmatic `setText`/`setChecked` calls cannot schedule delayed persistence while stored values are being loaded.
- Hydrated phone Settings sections immediately after lazy construction and stopped section expand/collapse state changes from writing the entire Settings model.
- Added an `onStop` persistence boundary for real user edits while retaining the hydration guard.
- Added `V415SettingsPersistenceInstrumentedTest`, which covers cold launch, lazy Provider/Prompt/Performance expansion, exact custom-value hydration, refresh, and storage preservation.
- Created and installed archive-first `4.15-dev.1`/code59 from Settings P0 commit `e1b7a17` under event `build-20260729-062725`; APK SHA-256 is `18C36F6A...BCC665`.
- Passed the focused Settings P0 instrumentation 1/1 on OnePlus CPH2691 / Android 15.
- Preserved the current vertical position when a user selects a different Glossary or Pronoun from its list, while retaining top-of-page behavior for editor transitions.
- Added `V415LibraryScrollInstrumentedTest`, which builds long fixture libraries, scrolls both lists, selects a different profile, verifies exact `scrollY` and selected IDs, and restores Glossary, Pronoun, and Settings preferences exactly.
- Created and installed archive-first `4.15-dev.2`/code60 from scroll implementation commit `9e6730d` under event `build-20260729-063857`; APK SHA-256 is `828B4535...DCE7`.
- Passed the focused scroll instrumentation 1/1 on OnePlus CPH2691 / Android 15.
- Enabled multi-select on list-level Glossary and Pronoun pickers; every valid file is parsed and persisted as an independent profile, invalid siblings are reported without discarding valid imports, and the first valid profile becomes active.
- Kept Pronoun replacement single-select and cleared stale replacement intent after cancellation/failure. Renamed the Glossary editor action to `Merge files` so its intentional merge behavior is distinct from list import.
- Added four pure planner tests and `V415MultiProfileImportInstrumentedTest`, which verifies picker flags, URI de-duplication, two independent Glossaries, two independent Pronouns, activation, and exact restoration of user preferences/runtime log.
- Created and installed archive-first `4.15-dev.3`/code61 from multi-import commit `cda8eff` under event `build-20260729-065132`; APK SHA-256 is `5D412996...A1EF7`.
- Passed focused multi-profile import instrumentation 2/2 on OnePlus CPH2691 / Android 15.
- Passed the full v4.15 automated regression: approved-logo guard, 110/110 JVM tests, lint with 0 errors/53 warnings, and connected instrumentation with 15 passed plus 1 explicitly opt-in paid case skipped and 0 failures/errors.
- Added `QA_REPORT_v4_15.md` with exact build identity, archive hashes, suite counts, focused v4.15 evidence, and remaining release work.
- Built exact `4.15`/code62 from commit `5253f00` with the mandatory archive-first workflow under event `build-20260729-070042`; repeated 110 passing JVM tests and lint with 0 errors/53 warnings.
- Verified all five exact-build files match their backup mirrors, all four manifest hashes pass, installed the APK, and confirmed device package metadata reports versionName `4.15`/versionCode `62`.
- Passed full connected instrumentation against exact `4.15`/code62: 16 total, 15 passed, 1 explicitly opt-in paid real-API case skipped, and 0 failures/errors.
- Passed exact-code62 physical-device Macrobenchmark with 5/5 cold starts at 313.89/330.23/335.04 ms minimum/median/maximum and retained AndroidX JSON plus five real Perfetto traces.
- Retained five exact-device screenshots, five UI hierarchy dumps, and a clearly documented 25-frame MJPEG AVI covering Settings, Provider/Prompt/Performance, Glossaries, and Pronouns.
- Mirrored the 21-file event `qa-code62-20260729-072300` under both durable QA stores with matching relative paths, lengths, and SHA-256 hashes.
- Reinstalled the exact archived APK after benchmark cleanup and reconfirmed versionName `4.15`, versionCode `62`, minSdk `26`, targetSdk `35`, and `.MainActivity`.
- Passed the PreTag gate and created annotated tag `v4.15`; tag object `7e99cc69eb948a41659021c1114468e775e13b34` targets approved commit `d6d844d97f51ce8637048e1fbbd0f7a9940cee47`.
- Stopped before PreBackup/archive after proving that local tag `v4.15` still contains v4.14 release notes and no v4.15 changelog section; confirmed the tag is absent from `origin` and both `tag-v4.15` archive destinations are absent.
- With explicit approval, deleted only rejected unpublished tag object `7e99cc69eb948a41659021c1114468e775e13b34`; no remote ref or archive payload was removed.
- Prepared correct v4.15 release notes and a v4.15 changelog entry covering all three fixes plus exact build/QA evidence.
- Committed corrected release metadata as `292b24e`, reran PreTag successfully, and created fresh annotated tag object `cb474d2dd22763f67e24b4c0f57a27b7ae4689e7` targeting that commit.
- Verified directly from tag `v4.15` that release notes identify 4.15 and the changelog begins with a 4.15 section.
- Passed PreBackup and created immutable `tag-v4.15` artifact/backup payloads from corrected tag target `292b24e`.
- Verified both 22-file payloads match by relative path, length, and SHA-256; all 21 manifest entries pass.
- Verified archived APK SHA-256 `7FDF60C9...3969` and 357-entry source ZIP SHA-256 `0F14121D...F433`; a fresh `git archive` of `v4.15` matches the retained ZIP exactly.

## Pending tasks

- Update `BUILD_STATE.md` for the v4.15 release, then continue final snapshot/export verification.

## Known bugs

- Rejected code54 candidate: Android removed the target package during cleanup after rejecting the unsigned Macrobenchmark APK. No benchmark measurement was produced.
- Rejected code55 candidate: the signed test captured two traces, but the OEM launcher stability wait made the five-iteration run impractical; the redundant Home step is removed for the next candidate.
- Rejected code56 candidate: its archived source removed the Home step but OnePlus Hans still froze the background test runner after trace capture. The correction was committed and rebuilt as accepted code57 release evidence.
- First code57 Macrobenchmark evidence set: completed but rejected because one OEM-affected first iteration measured 109,774 ms; retained for audit. The accepted rerun has all five iterations between 307.78 and 337.52 ms.
- P0 root causes fixed: the picker stripped `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`; external small config files were used at runtime; stale prepared-plan callbacks could keep Start disabled; and the output tree URI was decoded twice between service preparation and materialization, corrupting the persisted-grant identity.
- Controlled output-permission-loss UX passed on the physical device with a prepared local TXT, an invalid persisted output grant, the exact recovery message, and Start disabled before dispatch.
- Warm tab-switch P50 is `20 ms`, above the `16 ms` target.
- Android Lint has 50 warnings and 0 errors in the recorded v4.7 build.
- The full 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.
- APK and source ZIP archives are deliberately ignored by Git, so they are not present in a clean clone; durable copies remain in the local `artifacts/releases/v4.8/` and `backup/v4.8/` stores.
- Android Lint reports 54 warnings and 0 errors for the accepted v4.14 development build.
- v4.15 Settings P0 root cause was lazy phone-section construction followed by persistence of default-valued controls before hydration. The correction passed compile/JVM checks and the strengthened physical-device instrumentation, including exact preference snapshot equality after QA restoration.
- An accidental but valid immutable `4.14-dev.5`/code58 build is retained under event `build-20260729-062629` because the first build invocation omitted the explicit v4.15 series. It was superseded, not overwritten, by `4.15-dev.1`/code59.
- v4.15 library scroll root cause was same-tab cache invalidation rebuilding the `ScrollView` at position zero after selection. The correction restores the bounded prior position only for list-level Use actions; editor navigation behavior is unchanged.
- v4.15 multi-file import root cause was that list-level Glossary/Pronoun pickers did not set `EXTRA_ALLOW_MULTIPLE`, and their result handlers assumed one URI. The corrected handlers enumerate unique selected URIs and persist one profile per valid file.
- OnePlus Android 15 denies shell `screenrecord`; exact-code62 visual evidence therefore uses an explicitly documented MJPEG sequence of actual-device screenshots, not continuous MediaProjection capture.
- The first local tag was rejected for backup because it contained stale v4.14 release metadata; it was absent from `origin` and was deleted with approval before any immutable archive existed.

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
- v4.14-dev.4 full regression/build/archive/install: PASS. Approved-logo guard and 106 JVM tests passed; lint completed with 54 warnings and 0 errors; APK metadata is v4.14-dev.4/code53; all artifact/backup hashes match.
- Permanent-logo device QA: PASS. The installed launcher and in-app header display the approved logo, packaged resource and manifest references match, and the original user runtime state was preserved byte-for-byte.
- Exact-release tooling regression: PASS. PowerShell parser returned 0 errors; mutual exclusion, branch matching, and clean-tree guards each rejected the controlled invalid invocation with the expected reason.
- Macrobenchmark build regression: PASS. `compileBenchmarkJavaWithJavac`, `:app:assembleBenchmark`, and `:macrobenchmark:assembleBenchmark` completed successfully; the approved-logo guard also ran for the target variant.
- v4.14/code54 regression: PARTIAL PASS. Approved-logo guard, 106 JVM tests, lint (54 warnings/0 errors), artifact parity, and connected instrumentation (12 passed/1 opt-in skipped) passed; Macrobenchmark failed before measurement with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.
- v4.14/code55 regression: PARTIAL PASS. Approved-logo guard, 106 JVM tests, lint (54 warnings/0 errors), artifact parity, connected instrumentation (12 passed/1 opt-in skipped), signed benchmark installation, and two real traces passed; the five-iteration Macrobenchmark was stopped after the OEM launcher added nearly 10 minutes per setup.
- v4.14/code56 regression: PARTIAL PASS. Approved-logo guard, 106 JVM tests, lint (54 warnings/0 errors), artifact parity, and connected instrumentation (12 passed/1 opt-in skipped) passed. The archived source remained vulnerable to the OnePlus background freezer, so code56 is rejected.
- Corrected working-tree Macrobenchmark diagnostic: PASS. OnePlus CPH2691 / Android 15 completed 5/5 cold starts of `com.ml.tblandroidtxt.MainActivity`, 0 failed/skipped; median time to initial display was 332.01 ms and all five real Perfetto traces plus AndroidX JSON were retained in ignored durable artifact/backup diagnostic stores.
- v4.14/code57 final pre-tag regression: PASS. Approved-logo guard, 106 JVM tests, lint (0 errors/54 warnings), five-file build parity, connected instrumentation (11 passed/1 opt-in skip), controlled output-permission-loss QA, final visuals, and the accepted 5/5 Macrobenchmark rerun all passed.
- v4.15 Settings P0 focused compile/JVM regression: PASS. `:app:compileDebugAndroidTestJavaWithJavac testDebugUnitTest` completed with the approved-logo guard; 106 JVM tests passed with 0 failures/errors/skips.
- v4.15 Settings P0 strengthened device regression: PASS. `V415SettingsPersistenceInstrumentedTest` passed 1/1 on OnePlus CPH2691 / Android 15 with the exact `4.15-dev.1`/code59 build identity and exact preference snapshot equality after restore.
- v4.15-dev.1 archive verification: PASS. Both durable stores contain the same five files; all mirror SHA-256 values match, all four manifest entries verify, lint reports 0 errors/54 warnings, and the APK/source ZIP hashes are `18C36F6A...BCC665`/`D047836E...356`.
- v4.15 Glossary/Pronoun scroll regression: PASS. `V415LibraryScrollInstrumentedTest` passed 1/1 on OnePlus CPH2691 / Android 15 with exact `4.15-dev.2`/code60 identity; exact scroll positions and selected IDs passed for both lists, and all preference snapshots matched after restoration.
- v4.15-dev.2 archive verification: PASS. Both durable stores contain the same five files; every mirror hash matches, all four manifest entries verify, 106 JVM tests pass, lint reports 0 errors/54 warnings, and the APK/source ZIP hashes are `828B4535...DCE7`/`96838283...9C40`.
- v4.15 multi-profile import regression: PASS. Four focused planner tests pass, and `V415MultiProfileImportInstrumentedTest` passes 2/2 with exact `4.15-dev.3`/code61 identity on OnePlus CPH2691 / Android 15.
- v4.15-dev.3 archive verification: PASS. Both durable stores contain the same five files; every mirror hash matches, all four manifest entries verify, 110 JVM tests pass, lint reports 0 errors/53 warnings, and the APK/source ZIP hashes are `5D412996...A1EF7`/`9C7D7978...8EF`.
- v4.15 full automated regression: PASS. Logo guard and 110 JVM tests passed; lint reports 0 errors/53 warnings; full connected instrumentation on OnePlus CPH2691 / Android 15 records 16 total, 15 passed, 1 opt-in paid case skipped, and 0 failures/errors.
- v4.15/code62 exact build verification: PASS. Archive-first build repeated 110 JVM tests and lint, both five-file durable payloads match, all four manifest entries verify, installation succeeded, and device metadata matches `4.15`/code62.
- v4.15/code62 exact release QA: PASS. Connected instrumentation records 16 total/15 passed/1 opt-in paid skip/0 failures or errors; Macrobenchmark passed 5/5 cold starts at 313.89/330.23/335.04 ms min/median/max with five real traces; the 21-file visual, UI, instrumentation, and performance evidence event matches its backup mirror exactly.
- v4.15 PreTag/tag verification: PASS. The PreTag gate accepted completed checklist steps 1-9; `v4.15` is an annotated tag object targeting `d6d844d`.
- v4.15 tag repair: PASS. Corrected release metadata is committed and the fresh annotated tag targets it.
- v4.15 immutable tag archive: PASS. Both 22-file stores match, all 21 manifest entries pass, and the retained source ZIP exactly matches a fresh archive of corrected tag `v4.15`.

## Next step

Update `BUILD_STATE.md` with the exact v4.15 build, corrected tag, archive, regression, device, and known-limitation state. Keep `.idea/gradle.xml` outside all product commits.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
