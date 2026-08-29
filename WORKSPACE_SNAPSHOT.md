# Workspace Snapshot

- Updated: `2026-08-29` (+07:00).
- Current phase: `D5_RELEASE`.
- Current status: `D5_PRE_BUILD_READY / D5_BUILD_PENDING`.
- Current version: `4.17-dev.N` pending the numbered local build; no v4.17 APK exists.
- Current branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113); D0 commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- Current commit baseline: `080d9d3611a1d13b7be6826d55c789ab8e54c3b6`, the clean D4 state commit immediately before this current-only pre-build snapshot; this line is not self-referential.
- Current build: recovery code113 APK SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`; frozen later code168 artifact remains unchanged; no D3 build, install or replacement.
- Completed: D1 reconciliation on exact code113; D2 Glossary4; D3 Pronoun7 parser/model, exact P3 mapping, scope matching, paragraph-range annotation, compact prompt, preview/estimator parity, raw settings/profile persistence and resume/retry range restoration; D4 Mercedes CH001/CH004 integration, range-aware refinement and exact cost evidence.
- Pending: commit the pre-build metadata/script changes, then run the one authorized numbered D5 development build and static QA. No APK has been built.
- Known bugs/limitations: multiline CSV fields remain explicitly rejected by the existing line-based parser; malformed scoped P3 rows are reported and fail closed. No P3 semantic bug remains open in D3.
- Regression status: D5 focused `11/11 PASS`; D5 full JVM `205 tests / 0 failures / 0 errors / 0 skipped`; PowerShell AST and invalid `MinimumVersionCode` rejection PASS; no APK/device/API/RSC/Editorial/IPC test or runtime action.
- Evidence: `docs/D1_CODE113_BASELINE.md`, `docs/D4_TRANSLATION_PROFILE_INTEGRATION.md`, `QA_REPORT_v4_17.md`, `scripts/build-and-save.ps1`, `app/src/test/java/com/ml/tblandroidtxt/TranslationProfileIntegrationTest.java`, hermetic Mercedes v4.17 fixtures, the D2/D3 focused tests and the v4.17 checklist.
- Exact next action: commit the pre-build changes on this branch, verify a clean tree, then run `scripts/build-and-save.ps1 -Series 4.17-dev -MinimumVersionCode 169`; no direct APK build or install.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This is current-only state; Git history preserves prior snapshots.
