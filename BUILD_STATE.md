# Build State

- Status: `D2_GLOSSARY4_COMPLETE / D3_PRONOUN7_READY`.
- Active authority: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`.
- Current version: `v4.17-translation-profile-compatibility`; no v4.17 APK exists.
- Active branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113).
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- D2 implementation commit: `353b641` (`feat(glossary): preserve matched runtime notes`).
- Current commit: `353b641`, the D2 implementation baseline immediately before this current-only state/docs update; the final state/docs commit is reported at handoff and this line is not self-referential.
- Current build: recovery source artifact `v4.16-dev.51` / code113 remains 2,786,334 bytes / SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no APK was built, installed or archived for D2.
- Completed: D1 reconciliation on exact code113; direct MainActivity/TranslationEngine revalidation; real Glossary five-column and Pronoun P3 diagnostics; D2 test-first red run, four-field Glossary runtime, note persistence/compact round-trip, matched-only prompt/cost behavior and priority exclusion.
- Production files changed in D2: exactly `app/src/main/java/com/ml/tblandroidtxt/GlossaryStore.java` and `app/src/main/java/com/ml/tblandroidtxt/PromptContextBuilder.java`.
- Pending: D3 Pronoun7, then D4 integration and D5 release, in order. D3 has not started.
- Known bugs/limitations: the code113 Pronoun P3 parser still maps only `from,speaker,target`, turns the seven-column header into a rule and loses `self,call,scope,note`; this is the preserved D1 baseline bug, not a D2 contract. Glossary multiline CSV fields are explicitly rejected because the existing line parser is not a multiline CSV framework.
- Regression status: focused D2 `9/9 PASS`; A suite `34/34 PASS`; B persistence/retry/chunk suite `49/49 PASS`; P3 plus corrected Glossary diagnostics `2/2 PASS`; final combined A+B+C+D `94 tests, 0 failures, 0 errors, 0 skipped`.
- Exact test evidence: `docs/D1_CODE113_BASELINE.md` records the complete JDK21/Android-SDK-in-shell commands, the expected pre-production D2 red result, final suite totals and diagnostic output.
- Not run: `assembleDebug`, `scripts/build-and-save.ps1`, APK build, Android instrumentation, emulator/AVD, device, real provider/API, RSC, Editorial and IPC tests.
- Exact next action: write focused failing/acceptance tests for D3 Pronoun7 before changing production.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` was not modified, staged, reset, stashed or cleaned; old v4.16 D1 commits remain read-only references.

This file is current-only; Git history preserves prior state.
