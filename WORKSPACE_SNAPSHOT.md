# Workspace Snapshot

- Updated: `2026-08-29` (+07:00).
- Current phase: `D5_RELEASE`.
- Current status: `D5_LOCAL_BUILD_QA_COMPLETE / V4.17_DEV_ARTIFACT_READY`.
- Current version: `4.17-dev.1` / Android versionCode `169`.
- Current branch: `feature/v4.17-translation-profile-compatibility`.
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113); D0 commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`.
- Current commit baseline: `b0ab983bd4f9b017819af2bb0043bc57caad807a`, the pre-build source commit immediately before this current-only final QA/state snapshot; this line is not self-referential.
- Current build: `4.17-dev.1` / code169, APK SHA-256 `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`, event `build-20260829-185818`; artifact `artifacts/builds/v4.17-dev.1/build-20260829-185818/`; backup `backup/builds/v4.17-dev.1/build-20260829-185818/`; source ZIP SHA-256 `AA3A75CF4CFC4C527352F037D3AABEC9BBF24517138F58DB974794CEB67EAA6F`; artifact and backup parity `5/5 PASS`.
- Completed: D1 reconciliation on exact code113; D2 Glossary4; D3 Pronoun7 parser/model, exact P3 mapping, scope matching, paragraph-range annotation, compact prompt, preview/estimator parity, raw settings/profile persistence and resume/retry range restoration; D4 Mercedes CH001/CH004 integration, range-aware refinement and exact cost evidence; D5 numbered local build, Lint/static APK QA and immutable artifact verification.
- Pending: physical-device/manual QA on the archived development APK and any separately authorized public tag/archive gate; public release is not claimed.
- Known bugs/limitations: multiline CSV fields remain explicitly rejected by the existing line-based parser; malformed scoped P3 rows are reported and fail closed; historical Editorial assets remain packaged; device QA is pending.
- Regression status: focused `11/11 PASS`; full JVM `205 tests / 0 failures / 0 errors / 0 skipped`; Lint `0 errors/53 warnings`; artifact/checksum parity PASS; no device/API/RSC/Editorial/IPC test or runtime action.
- Evidence: `docs/D1_CODE113_BASELINE.md`, `docs/D4_TRANSLATION_PROFILE_INTEGRATION.md`, `QA_REPORT_v4_17.md`, `scripts/build-and-save.ps1`, `app/src/test/java/com/ml/tblandroidtxt/TranslationProfileIntegrationTest.java`, hermetic Mercedes v4.17 fixtures, the D2/D3 focused tests and the v4.17 checklist.
- Exact next action: perform manual QA on the archived `4.17-dev.1` APK when a real device is connected, without rebuilding; keep public tag/archive and Complete gates pending.
- Protected state: the original workspace `C:\Users\ADMIN\Documents\App Translate Books` and its pre-existing dirty/untracked state remain untouched; no old v4.16 D1 commit, artifact, evidence, raw or RSC/Editorial state was staged or reset.

This is current-only state; Git history preserves prior snapshots.
