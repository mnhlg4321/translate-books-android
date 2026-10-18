# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): 4.23-dev.5 (UX request U1-U3) built, regression and emulator QA PASS (197 / 181 PASS / 16 SKIP / 0 FAIL); waiting for the owner to approve the phone install.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `dcde3320` is the baseline before this snapshot commit (docs only in this commit); dev.2 APK built from `79fbc979`. Confirm actual HEAD when resuming.
- Current build: 4.23-dev.5 / code 280 (event build-20261018-212435, source f887b2b3, APK SHA-256 56E300A454460E131032E2EA20199F055777B43ADB7359A13A31E0083DB7E5EB); emulator-5554 only; phone 15e84958 still 4.23-dev.2 / code 277.
- Current phase: UX work request complete through QA; stopped for the owner's install approval.
- Completed tasks: steps 01-04; L1, L2, L3, L4; dev.1 and dev.2 builds, clean-archive regressions, device QA and reports (`artifacts/qa/v4.23-dev1-20261018`, `v4.23-dev2-20261018`).
- Pending tasks: U1 FilePickerModel/FilePickerSheet/SelectedFilesSlot; U2 new combo screen and RAW shortcut; U3 Translate source and the clipping/setItems family sweep; clean-archive regression; `4.23-dev.3` (code >= 278) and emulator QA with 8 screenshots; owner install approval; trial D1-D2, E1-E5, D4-D7; release decision (v4.22 and v4.23 order).
- Known bugs/limits: as v4.22; chapter-set UX defects P1-P5 in the UX work request (file names clipped in fixed-height buttons, `AlertDialog.setItems` pickers close without feedback, combo only from RAW tab); byte-level state of glossary/pronoun prefs after the phone upgrade UNKNOWN; a job report prints raw settings JSON when it cannot parse it.
- Regression status: clean archive `79fbc979`: engine 742 / 1 SKIP, app debug, release, benchmark 547 each / 5 SKIP, 0 FAIL, lint 0 errors / 64 warnings, AndroidTest compile PASS; device 196 / 180 PASS / 16 SKIP / 0 FAIL. Editorial boundary diff empty; five Editorial-named tests changed by one number.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone 15e84958: owner approved the install in chat 2026-10-18; backup before install in D:/App Translate Books/App Translate Books-device-backups/v4.23-dev2-code277/20261018-213422 (DB sha256 e3820252...107134 matches phone and archive); 4.23-dev.5 / code 280 installed with install-validated.ps1, first launch without crash.
- Next action: owner tries E1-E5 on the phone and reports; D1-D7 items not yet tried stay NOT_MEASURED.
