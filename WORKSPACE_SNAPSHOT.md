# Workspace Snapshot

- Updated: `2026-08-29` (+07:00).
- Current phase: `D4_INTEGRATION`.
- Current status: `D3_PRONOUN7_COMPLETE / D4_INTEGRATION_READY`.
- Current version: `v4.17-translation-profile-compatibility`; no v4.17 APK exists.
- Current branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113); D0 commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- Current commit baseline: `11e6f54c4d164ad1b7e163b852b264714bbecb75`, the D3 implementation/tests commit immediately before this current-only state/docs commit; this line is not self-referential.
- Current build: recovery code113 APK SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no D3 build, install or replacement.
- Completed: D1 reconciliation on exact code113; D2 Glossary4; D3 Pronoun7 parser/model, exact P3 mapping, scope matching, paragraph-range annotation, compact prompt, preview/estimator parity, raw settings/profile persistence and resume/retry range restoration.
- Pending: D4 integration → D5 release, strictly in order. D4 implementation has not started.
- Known bugs/limitations: multiline CSV fields remain explicitly rejected by the existing line-based parser; malformed scoped P3 rows are reported and fail closed. No P3 semantic bug remains open in D3.
- Regression status: final canonical JVM run `111 tests / 0 failures / 0 errors / 0 skipped`; D3 focused `17/17 PASS`; P3 diagnostic `1/1 PASS`; D2 focused `9/9 PASS`; no APK/device/API/RSC/Editorial/IPC action.
- Evidence: `docs/D1_CODE113_BASELINE.md`, `app/src/test/java/com/ml/tblandroidtxt/PronounSevenFieldRuntimeTest.java`, the corrected `Code113P3DiagnosticFixtureTest`, the unchanged code113 fixtures and the v4.17 checklist.
- Exact next action: write focused D4 Glossary+Pronoun integration tests before changing production.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This is current-only state; Git history preserves prior snapshots.
