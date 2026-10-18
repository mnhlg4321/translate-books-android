# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): dev.3 device test at font 1.3 showed 3-line cap clips 90+ character names; cap removed in slots and raised in the sheet; rebuilding as the next numbered dev build.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `dcde3320` is the baseline before this snapshot commit (docs only in this commit); dev.2 APK built from `79fbc979`. Confirm actual HEAD when resuming.
- Current build: `4.23-dev.2` / code 277 (event `build-20261018-175553`, APK SHA-256 `D2BEE90BE88432411C98FB877EC15558A455EEB6924734993F1BCFED43914D9B`); on emulator-5554 and on the owner phone `15e84958` (dumpsys 2026-10-18 21:00: 4.23-dev.2 / 277).
- Current phase: UX work request docs/V423_UX_COMBO_PICKER_WORK_REQUEST.md: U1-U3 code done; next regression, 4.23-dev.3 build, device QA, 8 screenshots, owner report.
- Completed tasks: steps 01-04; L1, L2, L3, L4; dev.1 and dev.2 builds, clean-archive regressions, device QA and reports (`artifacts/qa/v4.23-dev1-20261018`, `v4.23-dev2-20261018`).
- Pending tasks: U1 FilePickerModel/FilePickerSheet/SelectedFilesSlot; U2 new combo screen and RAW shortcut; U3 Translate source and the clipping/setItems family sweep; clean-archive regression; `4.23-dev.3` (code >= 278) and emulator QA with 8 screenshots; owner install approval; trial D1-D2, E1-E5, D4-D7; release decision (v4.22 and v4.23 order).
- Known bugs/limits: as v4.22; chapter-set UX defects P1-P5 in the UX work request (file names clipped in fixed-height buttons, `AlertDialog.setItems` pickers close without feedback, combo only from RAW tab); byte-level state of glossary/pronoun prefs after the phone upgrade UNKNOWN; a job report prints raw settings JSON when it cannot parse it.
- Regression status: clean archive `79fbc979`: engine 742 / 1 SKIP, app debug, release, benchmark 547 each / 5 SKIP, 0 FAIL, lint 0 errors / 64 warnings, AndroidTest compile PASS; device 196 / 180 PASS / 16 SKIP / 0 FAIL. Editorial boundary diff empty; five Editorial-named tests changed by one number.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone 15e84958: owner approved the install in chat 2026-10-18; backup before install in D:/App Translate Books/App Translate Books-device-backups/v4.22-dev2-code273/20261018-204430 (DB sha256 1ee29b9f...44fd52 matches phone and archive); 4.23-dev.2 / code 277 installed with install-validated.ps1, first launch without crash.
- Next action: build the next 4.23-dev (code 279), clean-archive regression, device QA, 8 screenshots, owner question.
