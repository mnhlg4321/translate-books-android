# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): 4.23-dev.7 (card fix) built, regression and emulator QA PASS (204 / 188 PASS / 16 SKIP / 0 FAIL); waiting for the owner to approve the phone install.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `e8cd9c37` is the baseline before this snapshot commit (docs only). Confirm actual HEAD when resuming.
- Current build: 4.23-dev.7 / code 282 (event build-20261018-224505, source a5facba8, APK SHA-256 C088BDCED5C5A37338341EE6CF7F3750E6C53037F6056DBCDFB218CB3A8CA59C); emulator-5554 only; phone 15e84958 on 4.23-dev.6 / code 281.
- Current phase: UX work request complete through QA; stopped for the owner's install approval.
- Completed tasks: steps 01-04; L1, L2, L3, L4; dev.1 and dev.2 builds, clean-archive regressions, device QA and reports (`artifacts/qa/v4.23-dev1-20261018`, `v4.23-dev2-20261018`).
- Pending tasks: U1 FilePickerModel/FilePickerSheet/SelectedFilesSlot; U2 new combo screen and RAW shortcut; U3 Translate source and the clipping/setItems family sweep; clean-archive regression; `4.23-dev.3` (code >= 278) and emulator QA with 8 screenshots; owner install approval; trial D1-D2, E1-E5, D4-D7; release decision (v4.22 and v4.23 order).
- Known bugs/limits: as v4.22; resume-after-retry fixed in dev.6 (owner trial F1-F3 pending); D7 not reported (NOT_MEASURED).
- Regression status: clean archive `79fbc979`: engine 742 / 1 SKIP, app debug, release, benchmark 547 each / 5 SKIP, 0 FAIL, lint 0 errors / 64 warnings, AndroidTest compile PASS; device 196 / 180 PASS / 16 SKIP / 0 FAIL. Editorial boundary diff empty; five Editorial-named tests changed by one number.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone 15e84958: owner approved the install in chat 2026-10-18; backup before install in D:/App Translate Books/App Translate Books-device-backups/v4.23-dev2-code277/20261018-213422 (DB sha256 e3820252...107134 matches phone and archive); 4.23-dev.5 / code 280 installed with install-validated.ps1, first launch without crash.
- Next action: ask the owner to approve installing 4.23-dev.7 on phone 15e84958 (the dev.6 app is release-signed: back up what run-as allows first).
