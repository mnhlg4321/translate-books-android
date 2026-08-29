# Build State

- Status: `D5_PRE_BUILD_READY / D5_BUILD_PENDING`.
- Active authority: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`.
- Current version: `4.17-dev.N` pending the numbered local build; no v4.17 APK exists.
- Active branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113).
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- D1 documentation/state commit: `af2135831f847568b2bad170324e2474684aa0ee`.
- D2 implementation commit: `353b6410c1c359c5fca0ebccaabfdcbb2e83a037` (`feat(glossary): preserve matched runtime notes`).
- D2 state commit: `ff31b4039940ef828b85720314efe00eac723b3c`.
- D3 implementation/tests commit: `11e6f54c4d164ad1b7e163b852b264714bbecb75` (`feat(pronoun): support scoped seven-field profiles`).
- D4 implementation/tests commit: `11c726fd94598664979de1768cb04ed7b574dd10` (`fix(prompt): integrate scoped locks through refinement`).
- Current commit: `080d9d3611a1d13b7be6826d55c789ab8e54c3b6`, the clean D4 state commit immediately before this current-only pre-build state; this field is not self-referential.
- Current build: recovery source artifact `v4.16-dev.51` / code113 remains 2,786,334 bytes / SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no D3 APK was built, installed or archived.
- Completed: D1 exact-code113 reconciliation; D2 Glossary4; D3 exact P3 seven-field parser/model, header/BOM/CSV validation, scope parsing and fail-closed matching, deterministic paragraph-range annotation, compact prompt/preview/estimator parity, raw settings/profile persistence and resume/retry/context-overflow range restoration; D4 hermetic Mercedes CH001/CH004 integration, range-aware refinement and exact cost evidence.
- D4 production files changed: `app/src/main/java/com/ml/tblandroidtxt/PromptPlan.java`, `PromptBuilder.java`, `CostEstimator.java`, `TranslationEngine.java` and `TranslatorService.java`. No `GlossaryStore.java`, `PronounStore.java`, `MainActivity.java`, database schema, RSC, Editorial, IPC, provider or build/version source changed.
- Pending: commit the D5 pre-build metadata/script changes, then run the one authorized `4.17-dev.N` build and static QA. No v4.17 APK was built.
- Known bugs/limitations: multiline CSV fields remain explicitly rejected by the existing line-based parser; malformed P3 scopes are reported and fail closed; legacy String-only refinement APIs intentionally have unknown paragraph range and are not used by the runtime Chunk path. No D4 integration defect remains open.
- Regression status: D5 focused `11/11 PASS`; D5 full JVM `205 tests / 0 failures / 0 errors / 0 skipped`; PowerShell AST and invalid `MinimumVersionCode` rejection PASS.
- Exact test evidence: JDK21 `C:\Program Files\Android\Android Studio\jbr` with in-shell `ANDROID_HOME`/`ANDROID_SDK_ROOT`; exact commands, red repair record and metrics are in `docs/D4_TRANSLATION_PROFILE_INTEGRATION.md`.
- Not run: `scripts/build-and-save.ps1`, APK build, lint, `assembleDebug`, Android instrumentation, emulator/AVD, device, real provider/API, targeted RSC, Editorial and IPC suites. Unit-test regression was run directly; the build script remains the only authorized APK path.
- Exact next action: commit the pre-build changes on this branch, verify a clean tree, then run exactly once through `scripts/build-and-save.ps1 -Series 4.17-dev -MinimumVersionCode 169`.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This file is current-only; Git history preserves prior state.
