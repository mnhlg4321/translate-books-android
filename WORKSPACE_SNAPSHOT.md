# Workspace Snapshot

- Updated: 2026-10-19 (+07:00): owner asked for a real-phone test of G1-G6 on 4.23-dev.10: G2-G5 checked, a Hom nay chuong xong count bug found and fixed in code (not built); G1 and G6 need a provider run.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `846b7b11` is the baseline before this snapshot commit (docs only). Confirm actual HEAD when resuming.
- Current build: 4.23-dev.10 / code 285 installed on phone 15e84958; fix for the Hom nay / 7 ngay chapters-done count committed after it, not built.
- Current phase: UX work request complete through QA; stopped for the owner's install approval.
- Completed tasks: steps 01-04; L1, L2, L3, L4; dev.1 and dev.2 builds, clean-archive regressions, device QA and reports (`artifacts/qa/v4.23-dev1-20261018`, `v4.23-dev2-20261018`).
- Pending tasks: U1 FilePickerModel/FilePickerSheet/SelectedFilesSlot; U2 new combo screen and RAW shortcut; U3 Translate source and the clipping/setItems family sweep; clean-archive regression; `4.23-dev.3` (code >= 278) and emulator QA with 8 screenshots; owner install approval; trial D1-D2, E1-E5, D4-D7; release decision (v4.22 and v4.23 order).
- Known bugs/limits: Xem tat ca opens Jobs without a filter; no search box in Glossaries/Pronouns; 66 old test jobs left on the emulator by older test classes; every phone behaviour NOT_MEASURED until the owner tries G1-G6.
- Regression status: clean archive `79fbc979`: engine 742 / 1 SKIP, app debug, release, benchmark 547 each / 5 SKIP, 0 FAIL, lint 0 errors / 64 warnings, AndroidTest compile PASS; device 196 / 180 PASS / 16 SKIP / 0 FAIL. Editorial boundary diff empty; five Editorial-named tests changed by one number.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone 15e84958: owner approved the install in chat 2026-10-18; backup before install in D:/App Translate Books/App Translate Books-device-backups/v4.23-dev2-code277/20261018-213422 (DB sha256 e3820252...107134 matches phone and archive); 4.23-dev.5 / code 280 installed with install-validated.ps1, first launch without crash.
- Next action: owner decides: allow a small provider spend for G1 and G6, and approve building 4.23-dev.11 (totals fix) and installing it.
