# Build State

- Status: `D3_PRONOUN7_COMPLETE / D4_INTEGRATION_READY`.
- Active authority: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`.
- Current version: `v4.17-translation-profile-compatibility`; no v4.17 APK exists.
- Active branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113).
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- D1 documentation/state commit: `af2135831f847568b2bad170324e2474684aa0ee`.
- D2 implementation commit: `353b6410c1c359c5fca0ebccaabfdcbb2e83a037` (`feat(glossary): preserve matched runtime notes`).
- D2 state commit: `ff31b4039940ef828b85720314efe00eac723b3c`.
- D3 implementation/tests commit: `11e6f54c4d164ad1b7e163b852b264714bbecb75` (`feat(pronoun): support scoped seven-field profiles`).
- Current commit: `11e6f54c4d164ad1b7e163b852b264714bbecb75`, the D3 implementation baseline immediately before this current-only state/docs commit; this field is not self-referential.
- Current build: recovery source artifact `v4.16-dev.51` / code113 remains 2,786,334 bytes / SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no D3 APK was built, installed or archived.
- Completed: D1 exact-code113 reconciliation; D2 Glossary4; D3 exact P3 seven-field parser/model, header/BOM/CSV validation, scope parsing and fail-closed matching, deterministic paragraph-range annotation, compact prompt/preview/estimator parity, raw settings/profile persistence and resume/retry/context-overflow range restoration.
- D3 production files changed: `app/src/main/java/com/ml/tblandroidtxt/PromptContextBuilder.java`, `Chunk.java`, `Chunker.java`, `PromptPlan.java`, `PromptBuilder.java`, `PromptPreviewDialog.java`, `TranslationRepository.java` and `TranslatorService.java`. No `PronounStore.java`, `MainActivity.java`, `TranslationEngine.java`, database schema, RSC, Editorial, IPC, provider or build/version source changed.
- Pending: `D4_INTEGRATION` → `D5_RELEASE`, strictly in order. No D4 implementation has started.
- Known bugs/limitations: multiline CSV fields remain explicitly rejected by the existing line-based parser; malformed P3 scopes are reported and fail closed. No P3 semantic defect remains open from D3.
- Regression status: D3 focused `17/17 PASS`; P3 diagnostic `1/1 PASS`; D2 focused `9/9 PASS`; A suite `34/34 PASS`; B suite `49/49 PASS`; final combined canonical JVM run `111 tests / 0 failures / 0 errors / 0 skipped`.
- Exact test evidence: JDK21 `C:\Program Files\Android\Android Studio\jbr` with in-shell `ANDROID_HOME`/`ANDROID_SDK_ROOT`; exact commands and the expected pre-production red result are recorded in `docs/D1_CODE113_BASELINE.md`.
- Not run: `assembleDebug`, `scripts/build-and-save.ps1`, APK build, Android instrumentation, emulator/AVD, device, real provider/API, RSC, Editorial and IPC tests.
- Exact next action: write focused D4 Glossary+Pronoun integration tests before changing production.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This file is current-only; Git history preserves prior state.
