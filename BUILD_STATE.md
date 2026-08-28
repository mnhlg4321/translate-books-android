# Build State

- Status: `D1_BASELINE_COMPLETE / D2_GLOSSARY4_READY`.
- Active authority: `TRANSLATION_PROFILE_RECOVERY_V4_17.md`.
- Current version: `v4.17-translation-profile-compatibility`; no v4.17 APK exists.
- Active branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113); D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- Current commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`, the canonical D0 implementation/documentation baseline immediately before this D1 state commit; not self-referential.
- Current build: recovery source artifact `v4.16-dev.51` / code113 is 2,786,334 bytes / SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no APK was built or installed.
- Completed: D1 reconciliation on exact code113; direct revalidation of `MainActivity.java` and `TranslationEngine.java`; real five-column Glossary and seven-column Pronoun diagnostic fixtures; legacy three-column proof; focused JVM evidence; application-source diff remains 0.
- Pending: D2 Glossary4, then D3 Pronoun7, D4 integration and D5 release in order.
- Known bugs: Glossary CSV note is transiently read as aliases and lost in the `GlossaryStore.Term` adapter; CSV priority is ignored; real P3 parser maps only `from,speaker,target` and loses `self,call,scope,note`; the real P3 header becomes a rule.
- Regression status: final focused A+B+C+D run is `85 tests, 0 failures, 0 errors, 0 skipped`; no Android instrumentation, device, emulator, API, RSC or Editorial test was run; no production source changed.
- Evidence: `docs/D1_CODE113_BASELINE.md`; `app/src/test/java/com/ml/tblandroidtxt/Code113P3DiagnosticFixtureTest.java`; `app/src/test/java/com/ml/tblandroidtxt/Code113GlossaryProjectionDiagnosticFixtureTest.java`; canonical v4.17 checklist.
- Exact next action: begin D2 Glossary4 by writing focused failing/acceptance tests before modifying production.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` remains untouched; no old D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset here.

This file is current-only; Git history preserves earlier state.
