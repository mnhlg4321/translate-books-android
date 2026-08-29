# Build State

- Status: `D5_DEVICE_QA_COMPLETE / V4.17_DEV_ARTIFACT_DEVICE_VERIFIED`.
- Active authority: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`.
- Current phase: `D5_RELEASE` (device QA complete for the development artifact; public release remains pending).
- Current version: `4.17-dev.1` / Android versionCode `169`.
- Active branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113).
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- D1 documentation/state commit: `af2135831f847568b2bad170324e2474684aa0ee`.
- D2 implementation commit: `353b6410c1c359c5fca0ebccaabfdcbb2e83a037` (`feat(glossary): preserve matched runtime notes`).
- D2 state commit: `ff31b4039940ef828b85720314efe00eac723b3c`.
- D3 implementation/tests commit: `11e6f54c4d164ad1b7e163b852b264714bbecb75` (`feat(pronoun): support scoped seven-field profiles`).
- D4 implementation/tests commit: `11c726fd94598664979de1768cb04ed7b574dd10` (`fix(prompt): integrate scoped locks through refinement`).
- D5 pre-build/build commit: `b0ab983bd4f9b017819af2bb0043bc57caad807a` (`build(release): prepare v4.17 development artifact`).
- Current commit: `c01de1d72da62dccfc910239eacb882d5eb78128`, the clean source/state commit immediately before this current-only device-QA/state update; this field is not self-referential.
- Current build: `4.17-dev.1` / code169, APK `TranslateBooks-v4.17-dev.1-code169.apk`, 2,784,446 bytes / SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`; event `build-20260829-185818`; artifact `artifacts/builds/v4.17-dev.1/build-20260829-185818/`; backup `backup/builds/v4.17-dev.1/build-20260829-185818/`; source ZIP `project_source_build-20260829-185818.zip` SHA-256 `AA3A75CF4CFC4C527352F037D3AABEC9BBF24517138F58DB974794CEB67EAA6F`; matching payloads are retained.
- Completed: D1 exact-code113 reconciliation; D2 Glossary4; D3 exact P3 seven-field parser/model, header/BOM/CSV validation, scope parsing and fail-closed matching, deterministic paragraph-range annotation, compact prompt/preview/estimator parity, raw settings/profile persistence and resume/retry/context-overflow range restoration; D4 hermetic Mercedes CH001/CH004 integration, range-aware refinement and exact cost evidence; D5 numbered local build, static APK QA, immutable artifact parity and manual clean-install QA on OnePlus CPH2691.
- D4 production files changed: `app/src/main/java/com/ml/tblandroidtxt/PromptPlan.java`, `PromptBuilder.java`, `CostEstimator.java`, `TranslationEngine.java` and `TranslatorService.java`. No `GlossaryStore.java`, `PronounStore.java`, `MainActivity.java`, database schema, RSC, Editorial, IPC, provider or build/version source changed.
- Pending: separately authorized public tag/archive and public release gate. No public release is claimed.
- Known bugs/limitations: multiline CSV fields remain explicitly rejected by the existing line-based parser; malformed P3 scopes are reported and fail closed; legacy String-only refinement APIs intentionally have unknown paragraph range and are not used by the runtime Chunk path; historical Editorial assets remain packaged; public release evidence remains pending.
- Regression status: focused `11/11 PASS`; full JVM `205 tests / 0 failures / 0 errors / 0 skipped`; D3 baseline `111/111 PASS`; Lint `0 errors/53 warnings`; assemble PASS inside the archive-first script; artifact parity and checksum verification PASS; device clean-install/first-launch/CH001/CH004/restart QA PASS.
- Exact test evidence: JDK21 `C:\Program Files\Android\Android Studio\jbr` with in-shell `ANDROID_HOME`/`ANDROID_SDK_ROOT`; commands, XML/report paths, prompt QA, static APK QA and build metadata are in `QA_REPORT_v4_17.md`; device evidence is in `artifacts/device-qa/v4.17-dev.1/device-20260829-194713/`.
- Not run: Android instrumentation, emulator/AVD, real provider/API, targeted RSC, Editorial and IPC suites, public tag/archive, benchmark, Perfetto, Macrobenchmark and release screenshot/video evidence. No rebuild was performed in device QA; the APK was the exact archived payload from `scripts/build-and-save.ps1`.
- Exact next action: make a separate public-release decision and, if authorized, run the remaining tag/archive/benchmark/media gates; do not treat this development device QA as `PUBLIC_RELEASE_COMPLETE`.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This file is current-only; Git history preserves prior state.
