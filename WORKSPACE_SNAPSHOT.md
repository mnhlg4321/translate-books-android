# Workspace Snapshot

- Snapshot updated: `2026-08-04` (G2-C0A pure-JVM model/parser/canonicalizer/validator implementation and regression completed; review stop before G2-C0B; V5 executable core remains retired; SAFE4 foundation/code86 remains canonical)
- Current version: `4.15`/code62 remains the released product with corrected annotated tag `v4.15`; `4.16-dev.29`/code91 is the latest archived G2-B2B-ZIP build. Editorial execution remains intentionally blocked.
- Current branch: `feature/v4.16`; the pre-existing user-owned `.idea/gradle.xml` change was temporarily stashed to create the branch, then restored unchanged and remains excluded from product/release commits.
- Current commit: `489d24e` (verified implementation/documentation baseline immediately before this snapshot update; G2-C0A source and tests are the current working-tree change, with the immutable model staged for its first implementation commit)
- Current build: `artifacts/builds/v4.16-dev.29/build-20260804-073059/TranslateBooks-v4.16-dev.29-code91.apk`, SHA-256 `959A25630941550E3D59CB2FBE75C1E3C6D33DCA2A109553C248CDA653E4DE90`; matching five-file payload is under `backup/builds/v4.16-dev.29/build-20260804-073059/`. Source ZIP SHA-256 is `A55B5AEDDD945F5247FC1A6177C80654C615DF664B34485F106B92C83B948BAD`; these code91 hashes and device evidence remain unchanged. G2-C0A source is newer than code91; no APK was built because this slice is pure JVM. Focused C0A tests pass 14/14; full `:editorial-engine:test` passes 33/33; `:app:testDebugUnitTest` passes 161/161; Android instrumentation source compilation and `git diff --check` pass. The released v4.15/code62 archive remains unchanged.

## G2-B1 handoff

- Completed: additive SQLite v14 pack registry, immutable private storage, TOCTOU-safe headless import service, recovery, fail-closed read-only registry, migration/security/recovery tests, checklist and archive evidence.
- Pending: user review of G2-C0 plan only. G2-B2A and G2-B2B-ZIP are complete. Certification, Golden Replay, project binding and execution remain blocked.
- Known limitations: the code91 walkthrough used a disposable synthetic ZIP and is not full release QA or certification evidence; one blocked QA registry row remains on the device because no manual DB mutation/cleanup bypass was used. The APK has no trusted profile and compatibility remains fail-closed.
- Canonical/candidate boundary: code86 SAFE4 hashes are unchanged; `DBE214...` and `3B2FCC...` remain outside the registry and cannot activate or replace SAFE4.

## G2-B2A handoff

- Completed: read-only Editorial Packs section, grouped deterministic list, detail dialog, exact fail-closed wording, persisted metadata/integrity display, presenter/mapper tests and no-row-mutation instrumentation source.
- Pending: user review only. G2-B2B-ZIP is complete. Certification, activation, binding and execution remain blocked.
- Connected/visual UI QA remains pending because `adb` is unavailable.

## G2-B2B-ZIP handoff

- Completed: headless `importZip(InputStream)` boundary, progress/result mapper, one-shot SAF bridge, ZIP import coordinator and Editorial-tab import action. No certification, activation, project binding, execution or folder import is present.
- Evidence: commits `e124b5a`, `8901c8a`, `22c080b`, `fd4f807`, `3efb620`, `f62ea5b`, `ced89c5`, `a831e94`; engine 19/19; app JVM 161/161; instrumentation source compilation pass; lint 0 errors/54 warnings; archive-first `4.16-dev.29`/code91 event `build-20260804-073059` APK SHA `959A25630941550E3D59CB2FBE75C1E3C6D33DCA2A109553C248CDA653E4DE90`; source ZIP SHA `A55B5AEDDD945F5247FC1A6177C80654C615DF664B34485F106B92C83B948BAD`; artifact/backup parity pass. Code91 device QA passed SAF ZIP selection, immutable storage, fail-closed blocking and restart persistence for a synthetic non-SAFE4 fixture; no certification/execution evidence.
- Runtime profile is deliberately empty until a trusted contract registry exists; imports cannot be guessed compatible and therefore remain fail-closed/blocked in the current app. Canonical SAFE4 and external candidate boundary is unchanged.
- Handoff: `EDITORIAL_ACCOUNT_TRANSFER_HANDOFF.md` is the self-contained transfer document; `EDITORIAL_PACK_PLATFORM_G2_C0_PLAN.md` is the approved review-stop plan; `EDITORIAL_PACK_PLATFORM_G2_C0A_CHECKLIST.md` records the C0A evidence. C0A is complete and stopped before C0B/C0C.

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
- Updated `BUILD_STATE.md` to make v4.15/code62 the current accepted local release and record the exact build, corrected tag, installed-device state, regression, archive hashes, mirror parity, and known limitations.
- Passed final export verification on a clean tracked tree: tag ancestry/metadata, 22-file parity, all 21 manifest entries, APK hash, fresh 357-entry tag ZIP equality, current release documents, and installed v4.15/code62 identity all passed.
- Removed the temporary fresh-tag verification ZIP and restored the user's `.idea/gradle.xml` change.
- Recorded the first Complete-gate rejection: all 14 steps were checked, but the gate requires literal workflow status `COMPLETE`; corrected that transitional status for the retry.
- Passed `verify-release-workflow.ps1 -Gate Complete -ExpectedVersion 4.15` after the required status correction; all 14 checklist steps are complete.
- Merged `feature/v4.15` into `main` with `--no-ff` as release merge `81b8344`; corrected tag target `292b24e` is an ancestor of merged `main`.
- Pushed `main` and corrected annotated tag `v4.15` to the private GitHub repository without force.
- Verified remote `main` at `81b8344`, annotated tag object `cb474d2d...89e7`, and peeled tag target `292b24e...432c`.
- Created `feature/v4.16` from clean `main` after preserving only the user-owned IDE change in a named stash.
- Added `EDITORIAL_WORKFLOW_V5_PLAN.md`: reviewed V5 context/state/gate requirements, UI inheritance, single and batch import UX, filename metadata suggestions, project-level glossary/pronoun defaults, asset snapshots, data model, context allow-list, risks, and phased plan.
- Created `release_checklists/v4.16.md`; P0.1 implementation/build evidence is recorded while UI and persistence work remain pending.
- Added P0.1 editorial workflow contracts: strict V5 chapter state transitions, L1/L2/L3 context allow-lists, release-gate predicate, versioned L1/L2/L3 evidence schemas, and structural L1 report validation.
- Added six focused offline tests in `EditorialWorkflowV5Test`; passed 6/6. Archive-first `4.16-dev.1`/code63 build passed all 116 JVM tests and lint with 53 warnings/0 errors; artifact/backup files and hashes match.
- Completed P0.2: L2/L3 output validators, Canonical RAW Map, asset snapshot ambiguity/staleness checks, L2/L3 JSON fixtures, and long-RAW/voice/truncation tests. Focused suite passed 9/9; archive-first `4.16-dev.3`/code65 passed 119 JVM tests and lint with 53 warnings/0 errors; immutable artifact/backup parity passed.
- Completed P1 persistence: additive SQLite v10→v11 migration and repository records for Editorial Project, Chapter, asset snapshot, run, scene, gate and evidence. All 120 JVM tests and the physical-device repository test pass; archive-first `4.16-dev.4`/code66 passed lint with 53 warnings/0 errors and artifact/backup parity.
- Completed P1 import UI: isolated Biên tập tab, project creation, one-picker multi-TXT selection, deterministic RAW–DRAFT mapping preview, optional shared glossary/pronoun and active-profile defaults, and confirmed immutable asset snapshots. Archive-first `4.16-dev.5`/code67 passed 123 JVM tests and lint with 53 warnings/0 errors; artifact/backup parity passed.
- Completed P2 L1: isolated context builder and on-demand audit runner with RAW Map-before-DRAFT order, output validator, scene/gate/evidence persistence and REPORT_L1 rendering. Archive-first `4.16-dev.6`/code68 passed full JVM tests and lint; long chapters are deliberately blocked until scene segmentation is implemented.
- Completed segmented L1 for structurally mapped long chapters: deterministic RAW segmentation, evidence-based RAW–DRAFT marker mapping, per-scene contract/call/checkpoint, aggregate validator and REPORT_L1. Build `4.16-dev.7`/code69 passed 133 JVM tests and lint with 53 warnings/0 errors; artifact parity passed. Ambiguous/misaligned mappings remain deliberately blocked.
- Completed REPORT_L1 UX and checkpoint recovery: closed chapters can view/export TXT; failed segmented runs show failure evidence and retry only non-closed scenes on the same run. Build `4.16-dev.8`/code70 passed 133 JVM tests and lint with 53 warnings/0 errors; device execution of the new retry instrumentation remains pending.
- Completed model-assisted mapping fallback for long L1 chapters without matching markers. The model may select only deterministic anchors; validator rejects skipped, overlapping, reordered, or incomplete DRAFT coverage and retains mapping/token evidence.
- Completed initial L2 RAW-first runner and UI. The first isolated call sees only RAW/Glossary/Pronoun and must close `L2_RAW_LEDGER`; only then does a fresh edit context open DRAFT and validated REPORT_L1. VI_L2, Global Change Register, five gates, context manifest, hashes and per-phase usage are persisted. Build `4.16-dev.10`/code72 passed 138 JVM tests, lint 53 warnings/0 errors and artifact/backup parity; focused device instrumentation passed 4/4 on code71 before the final context-size guard.
- Completed segmented L2, per-RAW/per-edit scene checkpoints, aggregate VI_L2 validation, failed-scene-only retry and line-oriented DRAFT→VI_L2 diff/export UI. Device regression exposed and fixed marker-only scene segmentation. Accepted `4.16-dev.12`/code74 passed 142 JVM tests, lint 0 errors/53 warnings, focused device Editorial tests 5/5 and artifact/backup parity.

- Completed isolated L3 core: fresh RAW–VI_L2 context without REPORT_L1, persisted/validated independent ledger checkpoint, delayed report-review context, FINAL_QA/Change Set/five gates, cross-scene voice audit, final read-through and RELEASE_READY UI. Accepted `4.16-dev.13`/code75 passed 144 JVM tests, lint 0 errors/53 warnings, focused device Editorial tests 6/6 and artifact/backup parity.

- Completed segmented L3: inherits accepted L2 scene checkpoints, validates matching anchors, checkpoints independent comparison per scene, closes a separate chapter-level voice audit before REPORT_L1 unlock, checkpoints final review per scene, aggregates FINAL_QA/Change Set/gates, and retries only invalid checkpoints. Accepted code77 passed 145 JVM tests, lint 0 errors/53 warnings, focused device Editorial tests 7/7 and artifact/backup parity.
- Completed Editorial release: revalidates the latest closed L3, FINAL_QA contract and persisted gates; exports a ZIP with final text, redacted hash/length/status evidence and checksums; records manifest/bundle hash and changes state atomically only after successful SAF write. Accepted code78 passed 145 JVM tests, lint 0 errors/53 warnings, and the complete Editorial device suite 9/9.
- Completed the code78 manual Release walkthrough and desktop ZIP inspection; retained eight matching evidence files in artifact/backup, removed all disposable device data, and defined the full v4.16 RC matrix in `QA_SCOPE_v4_16.md`.
- Implemented responsive two-line five-tab navigation and per-project default Release folders with persisted SAF permission, direct ZIP creation, unique `.zip` naming and an explicit fallback-picker destination/cancel warning; code79 build/device verification is pending.
- Accepted code79 after 147/147 JVM tests, lint 0 errors/53 warnings, artifact/backup parity and 10/10 focused Editorial device tests; exact code79 APK was reinstalled after the test runner cleanup.
- Added project-owned Editorial Glossary/Pronoun persistence and UI, removed the Translation Settings fallback from Editorial batch import, and added editable Series/Volume identity with collision validation. The expanded JVM suite passes; archive-first build/device migration regression is pending.
- Replaced the ambiguous all-in-one Editorial batch picker with independent RAW, DRAFT, Glossary, and Pronoun controls. RAW/DRAFT roles are explicit and accept filenames without role tokens; Glossary/Pronoun reuse Translate's wildcard picker and content validation. Code81 archive-first build passed 149 JVM tests and lint, both immutable payloads were created, and exact code81 was installed; hands-on picker UX verification remains pending.
- Corrected explicit RAW/DRAFT pairing to prefer the leading chapter number, so `005_RAW_...txt` pairs with a selected DRAFT named `005_RAW_... (Vietnamese).txt`. Selected RAW/DRAFT filenames are now visible on the project card. Added additive database v12→v13 project-reference profiles: multi-file Glossary/Pronoun import, per-project ACTIVE selection, profile listing/deletion, and migration of each existing project reference to an active profile. Code82 archive-first build passed 150 JVM tests/lint, installed over code81 and cold-launched successfully; hands-on profile/picker UX verification is pending.

- Editorial improvement steps 2-3: chapter cards now show RAW, DRAFT, Glossary and Pronoun immutable snapshot metadata (name, size and hash); the import preview is a scrollable card layout with READY/BLOCKED state, four input rows, active-reference validation and warnings. The preview remains read-only.
- Editorial improvement step 1 contract: `EditorialImportPlanner.planBundle(...)` groups by canonical numeric chapter key, detects RAW/DRAFT/Glossary/Pronoun from filename plus supported format, accepts CSV references, exposes `READY`/`NEEDS_REVIEW`, and keeps unclear files in `unassigned` so they cannot be saved automatically. Commit `36f42e7`.
- Editorial improvement step 2: RAW/DRAFT plans explicitly bind project ACTIVE references as `Inherited project default`; four-file bundles bind Glossary/Pronoun as `Chapter override`; chapter-numbered reference imports remain pending per chapter without changing ACTIVE; chapter-less reference imports require confirmation before project-default persistence. Preview renders source/profile/file ownership before save. Commit `4aca556`.
- Built and archived `4.16-dev.21`/code83 from commit `5264501` under event `build-20260801-133230`; both five-file payloads match, APK/source ZIP hashes are recorded in `BUILD_STATE.md`, 153 JVM tests passed and lint has 53 warnings/0 errors.
- Built and installed `4.16-dev.22`/code84 from commit `b6936fb` under event `build-20260801-133953`; both five-file payloads match, APK SHA-256 is `CDE2085E...892A51`, source ZIP SHA-256 is `4FECD825...CF835E2`, and the device reports exact package identity with a successful cold MainActivity launch.
- Added `EDITORIAL_HANDOFF_V4_16.md` and linked it from `README.md`; the handoff preserves the investigation, four-role/default-vs-override contract, revision-safe editing/stale-run roadmap, transfer checklist, build commands, artifact paths, and safety constraints. Commit `6d82c41`.
- Retired the incompatible V5 Editorial execution core: removed its schema/validator, three runners, context/model contracts, unsafe evidence mutation/latest lookup APIs, release bundle/folder flow, run/retry/release UI and obsolete behavior tests. No old execution entry point remains in current source.
- Bundled the exact V5-SAFE.4 three-file source pack with immutable per-file hashes and a mandatory `preBuild` integrity guard. Added a version/pack capability gate that remains disabled until lineage, exhaustive ledgers, evidence-derived gates, conditional Pronoun/Pair handling, SAFE4 release artifacts and Golden Replay G1–G10 are evidenced.
- Converted Editorial storage/UI to fail closed: new projects require the exact SAFE4 identity, new chapters are `SAFE4_BLOCKED`, legacy projects/chapters are read-only, and RAW/DRAFT/Glossary plus optional `PRONOUN_STATUS=NONE` can be prepared without enabling model execution.
- Added `EDITORIAL_SAFE4_MIGRATION.md` as the current source of truth and marked the older V5 plan/handoff historical. SAFE4 foundation regression passed 144/144 JVM tests and Android instrumentation-source compilation; the three bundled assets match the external source byte-for-byte.
- Built and retained `4.16-dev.23`/code85, then rejected it during post-build verification because its source ZIP normalized the three SAFE4 text files to CRLF. The APK pack guard passed, but the archived source was not byte-reproducible. Added a binary `.gitattributes` rule; code85 remains immutable audit evidence and cannot be accepted.
- Built and accepted `4.16-dev.24`/code86 from `ab8e78a` under event `build-20260803-165452`. Artifact/backup parity and manifests pass; 144 JVM tests and lint 0 errors/53 warnings pass; source ZIP reproduces the external SAFE4 sizes/hashes exactly. Installation was not requested.
- Completed Phase 1 source audit and saved `EDITORIAL_PACK_PLATFORM_PLAN.md` as the proposed contract-first, multi-pack, fail-closed architecture. G2-A now adds only the isolated JVM manifest/integrity/compatibility/read-only registry slice; no retired V5 execution component was restored.
- Rechecked the external SAFE4 source folder during Editorial Pack research. Project Instruction and Workflow still match code86, but the external Prompt is now 5,040 bytes/SHA-256 `DBE214D842D98FD76AFD2E700747FCC2CF3D5B6038134B38EA8F220FED3BF273`, while code86/source ZIP retains 5,008 bytes/SHA-256 `0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81`. The added L2 `PRONOUN_STATUS: LEGACY_REJECTED` line may change phase policy, so the external bytes are BLOCKED pending a distinct version, canonical manifest and contract review; no old build evidence applies to them.
- Final G2-A recheck found the external Prompt path changed again to 5,072 bytes/SHA-256 `3B2FCC0A8B684B851F8F6180D41DBE8C75E00CDB5BED217C3FB7A50E94437B04`. The earlier `DBE214...` candidate identity is not assumed to describe these current bytes; both remain UNIDENTIFIED/BLOCKED and outside the official registry.

## Pending tasks

- Resolve, in a later approved pack-review phase, whether the external `DBE214...` Prompt intentionally changes L2 Pronoun policy. Preserve code86's `0B4C...` Prompt as a separate immutable historical identity; do not reuse the same pack version/hash.
- G2-A implementation is complete in commits `83a9428`, `616dc46`, `eb15481` and `9d401b2`; its legacy `EditorialEngineProfile` remains an evaluator-facts object and is not a second trusted-profile source of truth. G2-C0A adds only the immutable trusted-profile data contract, strict parser/canonicalizer, fingerprint/hash calculations and fail-closed validator in `:editorial-engine`. Focused C0A tests pass 14/14 and full engine/app regression remains green. No app connection, SQLite change, model call, runtime wiring, profile bundle, L1/L2/L3 or APK build was made.
- G2-B2B-ZIP is complete for this approval scope. Await user review before any folder import, certification, activation, project binding or execution work.
- Implement typed exhaustive ledgers and machine-derived gates; do not accept model-provided PASS/CLOSED as evidence.
- Implement Pronoun `AVAILABLE/NONE/LEGACY_REJECTED`, scoped Pair Context, exact per-phase context allow-lists and checkpoint lineage.
- Implement versioned L1/L2/L3 SAFE4 runners, SAFE4 receipt/release artifacts, and retained Golden Replay G1–G10 evidence before enabling execution.
- Run device QA only on a new archive-first SAFE4 build. Keep the full v4.16 QA/tag/backup/export gates open.
- Review the completed `EDITORIAL_PACK_PLATFORM_G2_C0A` implementation and approve any later G2-C0B work separately. Do not start G2-C0B/G2-C0C, profile bundling, runtime selection, certification, activation, binding or execution in this handoff.

## Known bugs

- The current external SAFE4 folder no longer matches the exact code86/source-ZIP Prompt bytes, changed during this session, and has no canonical Editorial Pack manifest. Its observed current hash `3B2FCC...` and earlier `DBE214...` identity must not be classified as DATA_COMPATIBLE or certified under the old SAFE4 identity until separately versioned and reviewed.
- SAFE4 model execution and Editorial release remain intentionally blocked; G2-A is not connected to the Android execution path and does not certify any pack. This is a safety blocker, not a completed feature.
- The installed/archived code84 APK still contains the retired V5 engine. It must not be used to create or certify SAFE4 output.
- Code91 device QA proved only that a synthetic ZIP can be stored immutably and remains fail-closed; the device retains one blocked QA registry row and has no trusted engine profile.
- G2-C0A does not bundle or register a production profile; compatibility is not rewired and packs remain `STORED_BLOCKED`. The source is newer than the accepted code91 APK because no APK build was authorized for this pure-JVM slice.
- Code85 is rejected even though its APK pack guard passed: the retained source ZIP changed SAFE4 line endings and therefore failed exact-source reproducibility. Both payload copies are intentionally retained, not overwritten.
- Legacy V5 database rows are preserved but do not yet have SAFE4 manifest/parent lineage; they remain read-only and cannot be promoted into a SAFE4 chain.

- Code78 bottom-navigation clipping has an implemented responsive two-line fix in code79; visual verification is pending because the physical device is secured at the lock screen.
- Code80 device acceptance is blocked only by the Android device being disconnected; build, 148 JVM tests, lint and artifact parity passed.
- The OnePlus last-used-folder ambiguity has a code79 per-project Release folder/direct-write path plus clearer fallback warning; physical SAF selection/write/permission-loss verification is pending.

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
- Project default/chapter override flow is connected to the current picker/preview/save path; revision-safe editing of already-created snapshots is still not implemented.
- The portable handoff is tracked, but it does not replace the required source implementation, device QA, or v4.16 release gates.

## Regression status

- Editorial Pack Platform G2-A: PASS / REVIEW STOP. The isolated `:editorial-engine` module passes 19/19 JVM tests from clean state; the existing app passes 144/144 JVM tests. No SQLite, UI, model, execution, APK or release evidence was produced for G2-A.
- V5-SAFE.4 retirement/foundation regression: PASS for source safety and compilation. `:app:testDebugUnitTest` passed 144/144 with 0 failures/errors/skips; the mandatory pack guard verified all three external-source hashes; `:app:compileDebugAndroidTestJavaWithJavac` succeeded; `git diff --check` passed. SAFE4 execution, release, device QA and Golden Replay are deliberately not claimed.
- V5-SAFE.4 archive-first build verification: PASS on accepted code86. Both five-file stores and both checksum manifests match; APK/source ZIP hashes are `6C3EEF73...F37875`/`9CCE4C99...A0D9F1`; the ZIP's three SAFE4 files match external byte sizes and SHA-256 values. Installation/manual device QA remain open.

- v4.16 Editorial project-default/override regression: PASS. `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest` passed 160/160 JVM tests with 0 failures/errors/skips; targeted planner tests passed 14/14; `git diff --check` passed. Tests cover inherited defaults, chapter overrides, missing defaults and the 005/006/007 chapter-local bundle. Source/test evidence is committed through `5589af0`.
- v4.16 Editorial improvement steps 1-3 regression/build: PASS. `scripts/build-and-save.ps1 -Series 4.16-dev` archived code83 with 153 JVM tests passed, 0 failures/errors/skips, lint 0 errors/53 warnings, and matching five-file artifact/backup payloads. Device/manual UX QA remains pending.
- v4.16-dev.22 archive/install verification: PASS. `scripts/build-and-save.ps1 -Series 4.16-dev -Install` produced matching five-file artifact/backup payloads; manifest hashes pass; OnePlus CPH2691 / Android 15 reports versionName `4.16-dev.22`, versionCode `84`, minSdk `26`, targetSdk `35`; cold MainActivity launch returned `Status: ok`.
- v4.16 segmented L3 regression: PASS on accepted code77. 145 JVM tests passed; lint reports 0 errors/53 warnings; focused device Editorial suite passed 7/7. Controlled final scene-002 failure proved retry made exactly one additional model call while reusing independent scenes, chapter voice audit and closed final scene-001. Artifact/backup parity passed.
- v4.16 Editorial release regression: PASS on accepted code78. 145 JVM tests passed; lint reports 0 errors/53 warnings; complete physical-device Editorial suite passed 9/9 on OnePlus CPH2691 / Android 15. Tests prove sensitive source/config strings are absent from redacted evidence, checksums match, an open gate blocks release, and successful recording reaches `RELEASED`. Artifact/backup parity passed.
- v4.16 code78 manual Release QA: PARTIAL PASS. Confirmation, picker abort/restart safety, successful save, `RELEASED` UI, actual ZIP opening, exact three-entry contract, UTF-8 output, checksum verification and redaction inspection passed. Disposable device data was removed and evidence mirrored. Full RC scope remains pending; bottom-nav clipping and picker destination/cancel UX are recorded.
- v4.16 code79 navigation/destination regression: AUTOMATED PASS / VISUAL PENDING. 147 JVM tests and 10/10 Editorial device tests passed; lint and artifact parity passed. Visual navigation and real SAF folder walkthrough could not proceed while the phone was secured; no lock bypass was attempted.
- Editorial reference isolation/edit-identity JVM regression: PASS. `testDebugUnitTest` completed successfully after the additive v11→v12 migration and UI/data-flow changes; device migration/instrumentation remains pending.
- v4.16 transfer handoff documentation: PASS. `EDITORIAL_HANDOFF_V4_16.md` records the problem analysis, implemented contract, remaining revision-safe/stale-run design, exact resource map, transfer checklist, and archive-first continuation commands; `git diff --check` passed before commit `6d82c41`.
- G2-C0 audit/device evidence: PASS WITH LIMITATIONS. Source audit found only `pack.integrity.sha256.v1` implemented and all nine SAFE4 execution capabilities missing. Archived code91 installed successfully; synthetic SAF ZIP import stored the exact fixture immutably, returned `UNSUPPORTED_CONTRACT_SCHEMA`/`STORED_BLOCKED`, and persisted after process restart. No profile was bundled, no database/schema was changed, and no certification or execution was performed.
- G2-C0A trusted profile model/validation regression: PASS / REVIEW STOP. Focused C0A suite passed 14/14; full `:editorial-engine:test` passed 33/33; `:app:testDebugUnitTest` passed 161/161; Android instrumentation source compilation passed; `git diff --check` passed. Cached Gradle 9.4.1 was invoked directly because the unchanged repository wrapper is 9.3.0 while the installed Android Gradle Plugin requires 9.4.1; no APK was built. C0A validates declared capability evidence against the production catalog, which confirms only `pack.integrity.sha256.v1`; the nine SAFE4 capabilities remain missing.

- v4.16 isolated L3 regression: PASS on code75. 144 JVM tests passed; lint reports 0 errors/53 warnings; focused device Editorial suite passed 6/6. The L3 test proves the first model prompt contains RAW and VI_L2 but no REPORT_L1, then proves report visibility only after independent evidence validates CLOSED. Artifact/backup parity passed.

- v4.16 segmented L2 regression: PASS on accepted code74. 142 JVM tests passed; lint reports 0 errors/53 warnings; focused device Editorial suite passed 5/5, including a controlled scene-002 failure where retry made exactly one additional model call and reused every closed RAW/edit checkpoint. Artifact/backup parity passed.
- v4.16 model-mapping/L2 RAW-first regression: PASS. Archive-first `4.16-dev.10`/code72 ran 138 JVM tests with 0 failures/errors; lint reported 53 warnings/0 errors; focused physical-device Editorial instrumentation passed 4/4 on code71; final code72 added only the safe-context guard and passed the complete JVM/lint build; artifact/backup file hashes match.

- v4.16 planning documentation: PASS. `git diff --check` passed; no product code, APK build, or device regression has been run because implementation has not started.
- v4.16 P0.1 regression: PASS. `EditorialWorkflowV5Test` 6/6; full archive-first build recorded 116 JVM tests, 0 failures/errors, Android Lint 53 warnings/0 errors, and verified five-file artifact/backup parity.
- v4.16 P0 regression: PASS. `EditorialWorkflowV5Test` 9/9; full archive-first build recorded 119 JVM tests, 0 failures/errors, Android Lint 53 warnings/0 errors, and verified five-file artifact/backup parity.
- v4.16 P1 regression: PASS. All 120 JVM tests recorded 0 failures/errors; physical-device `EditorialRepositoryInstrumentedTest` passed 1/1; archive-first build recorded lint 53 warnings/0 errors and verified five-file artifact/backup parity.
- v4.16 P1 import UI regression: PASS. All 123 JVM tests recorded 0 failures/errors; `EditorialImportPlannerTest` exercises complete, missing, and ambiguous filename cases; archive-first build recorded lint 53 warnings/0 errors and verified five-file artifact/backup parity.

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
- v4.15 BUILD_STATE verification: PASS. Current release identity, code62 APK, corrected tag, QA counts, performance metrics, archive hashes, device state, and known limitations are recorded.
- v4.15 final export verification: PASS. Corrected tag, artifact/backup parity, manifest, APK, source ZIP, release documents, and installed device identity all passed.
- v4.15 merge/publication verification: PASS. Initial release merge `main` and corrected annotated tag/peeled target match the verified GitHub refs.

## Next step

Review the completed `EDITORIAL_PACK_PLATFORM_G2_C0A` implementation and its small commits. Stop here until separately approving G2-C0B; do not start G2-C0C or any profile bundling/runtime selection/folder import/certification/activation/binding/execution work; do not re-enable an old V5 path or claim pack certification.

## Resume rule

At the start of every session, read this file before changing code. Confirm the actual branch and `HEAD` with Git; `Current commit` intentionally records the implementation baseline immediately before the snapshot commit, because a committed file cannot contain its own final Git hash.
