# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): 4.23-dev.2 (L3 chapter sets + L4 translate by chapter set) built, clean-archive regression and emulator QA PASS (196 discovered / 180 PASS / 16 SKIP / 0 FAIL), 8 screenshots inspected; waiting for the owner.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `8e1141c4` is the baseline before this snapshot commit; dev.2 APK built from `79fbc979`, test APK `v423-dev2-test5-20261018` from `106a2159` (androidTest-only difference). Confirm actual HEAD when resuming.
- Current build: `4.23-dev.2` / code 277 (event `build-20261018-175553`, APK SHA-256 `D2BEE90BE88432411C98FB877EC15558A455EEB6924734993F1BCFED43914D9B`), emulator-5554 only.
- Current phase: step 05 develop complete for L1-L4; stopped for the owner's approval to install on the phone and run trial D1-D7.
- Completed tasks: steps 01-04; L1, L2, L3, L4; dev.1 and dev.2 builds, clean-archive regressions, device QA and reports (`artifacts/qa/v4.23-dev1-20261018`, `v4.23-dev2-20261018`).
- Pending tasks: owner approval for phone install of 4.23-dev.2 (separate approval); owner trial D1-D7; then release decision (v4.22 and v4.23 order).
- Known bugs/limits: as v4.22 (`feature/v4.22` snapshot); a job report prints raw settings JSON when it cannot parse it.
- Regression status: clean archive `79fbc979`: engine 742 / 1 SKIP, app debug, release, benchmark 547 each / 5 SKIP, 0 FAIL, lint 0 errors / 64 warnings, AndroidTest compile PASS; device 196 / 180 PASS / 16 SKIP / 0 FAIL. Editorial boundary diff empty; five Editorial-named tests changed by one number.
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone 15e84958: owner approved the install in chat 2026-10-18; backup before install in D:/App Translate Books/App Translate Books-device-backups/v4.22-dev2-code273/20261018-204430 (DB sha256 1ee29b9f...44fd52 matches phone and archive); 4.23-dev.2 / code 277 installed with install-validated.ps1, first launch without crash.
- Next action: owner runs trial D1-D7 on the phone and reports; byte-level check of glossary/pronoun prefs after the upgrade is UNKNOWN.
