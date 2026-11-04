# Workspace Snapshot

- Updated: 2026-11-04 (+07:00), release 4.25.0 QA complete and PreTag gate passed; tag next.
- Current version: release `4.25.0` / Android versionCode `329` and PC `4.25.0` (msi ProductVersion 4.25.200), built, not tagged yet, not installed. Phone `15e84958` runs `4.25-dev.9`/328; this PC runs `4.25.3`.
- Current branch: `feature/v4.25.0` (release branch from `feature/v4.25` at `e014f8c9`) in `D:\P5E-builds\wt-v4.25`.
- Current commit: `0a05d316` — baseline immediately before this release-documentation commit (test fix for the split fixture; production inputs are those of `e014f8c9`).
- Current build: APK `4.25.0`/329 event `build-20261104-204947` SHA-256 `4B82F0D58B00AF391E9E53879F0717870C28BBA742A1F9475056C63A54FF7B10`; PC event `build-20261104-213322`, zip `B65D2D29…FA52C`, msi `F8B51AD8…FA77E`; both from `e014f8c9`, artifact and backup identical.
- Current phase: release 4.25.0 — steps 01-09 evidenced, PreTag gate exit 0; tag, archive, BUILD_STATE and export remain. Product decision served: ship v4.25 (Translate clarity, run-bar fix, PC UI and Library parity).
- Completed tasks:
  - Split fixture repaired (`0a05d316`); release APK device tests: chapter-run 10/10, V425 classes 3/3.
  - Clean-archive regression green; Macrobenchmark A/B vs v4.24.1 (+31 ms average, NOT_DIAGNOSED); Perfetto trace; phone light/dark screenshots; video; PC release package.
  - QA report `artifacts/qa/v4.25.0/QA_REPORT.md`, CHANGELOG and RELEASE_NOTES for 4.25.0.
- Pending tasks:
  - Annotated tag `v4.25.0`, `archive-release.ps1` build and tag events (with the PC package), BUILD_STATE, Complete gate.
  - Owner approvals still needed for: merge to `main` and push; installing APK 4.25.0 on the phone; installing msi 4.25.0 on the PC.
- Known bugs/limits: cold start +31 ms vs v4.24.1 on the emulator (candidate: startup output-recovery cleanup); reasoning-bound rule kept (Haiku ratio measured 1.09–1.20, other models NOT_MEASURED); real-phone performance NOT_MEASURED; APK debuggable; msi unsigned; Editorial frozen. The owner deleted the 19 PC RAW on purpose (backup kept).
- Regression status: clean archive of `e014f8c9`: app 766/0/5, golden 14/14, core 67/0/1, jdbc 3/0, desktop 83/0, editorial-engine 742/0/1, lint 0 errors; device on release APK: chapter-run 10/10, V425 3/3.
- Next action: create annotated tag `v4.25.0` on the release-documentation commit, then run `archive-release.ps1` (events `build-…` and `tag-v4.25.0`, `-ProductionGitRef e014f8c9`, `-PcPackageDirectory` the PC event) and the PreBackup/Complete gates.
