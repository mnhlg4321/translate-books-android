# Workspace Snapshot

- Updated: `2026-08-29` (+07:00).
- Current phase: `D3_PRONOUN7`.
- Current status: `D2_GLOSSARY4_COMPLETE / D3_PRONOUN7_READY`.
- Current version: `v4.17-translation-profile-compatibility`; no v4.17 APK exists.
- Current branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113); D0 commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- Current commit baseline: `353b641`, the D2 implementation commit immediately before this current-only state/docs update; final state/docs HEAD is reported at handoff and this line is not self-referential.
- Current build: recovery code113 APK SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no D2 build, install or replacement.
- Completed: D1 reconciliation on exact code113; direct MainActivity/TranslationEngine revalidation; real five-column Glossary and seven-column Pronoun diagnostics; D2 Glossary4 with `source,target,category,note`, priority ignored, persistence and compact prompt round-trip, matched-only preview/compiler/estimator behavior.
- Pending: D3 Pronoun7 → D4 integration → D5 release, strictly in order. D3 production work has not started.
- Known bugs/limitations: preserved Pronoun P3 baseline misprojection (`from → speaker: target` for the header, `私 → Mercedes: Basil` for the data row, tail `self/call/scope/note` lost); existing line-based Glossary parser rejects multiline CSV fields explicitly. No new D2 production bug is open.
- Regression status: final combined canonical JVM run `94 tests / 0 failures / 0 errors / 0 skipped`; D2 focused `9/9 PASS`; no APK/device/API/RSC/Editorial/IPC action.
- Evidence: `docs/D1_CODE113_BASELINE.md`, `app/src/test/java/com/ml/tblandroidtxt/GlossaryFourFieldRuntimeTest.java`, the corrected `Code113GlossaryProjectionDiagnosticFixtureTest`, the unchanged P3 diagnostic/fixtures, and the v4.17 checklist.
- Exact next action: write focused failing/acceptance tests for D3 Pronoun7 before changing production.
- Protected state: original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This is current-only state; Git history preserves prior snapshots.
