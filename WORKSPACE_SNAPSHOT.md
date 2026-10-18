# Workspace Snapshot

- Updated: 2026-10-18 (+07:00): owner approved L3+L4; L4 implemented (service per-chapter settings from a per-Start snapshot, chapter-local error policy, Translate source switch, plan key with hashes); host 547 PASS; building 4.23-dev.2.
- Current version: v4.23.0 in development (not released); contains unreleased v4.22 code; shipped 4.21.0 / code 271 (tag `v4.21.0`).
- Current branch: `feature/v4.23` in worktree `D:\P5E-builds\wt-v4.23` (BASE `c8408207` on `feature/v4.22`).
- Current commit: `c028569b` (test fix on top of the dev.1 source `3848ebeb`) is the implementation baseline immediately before this snapshot commit; the dev.1 APK is built from `3848ebeb`. Confirm actual HEAD when resuming.
- Current build: `4.23-dev.1` / code 276 (event `build-20261018-164751`, source `3848ebeb`, APK SHA-256 `22B24F0C6BC8CF32258DF7D9CA70B3AFA796EFD0BE564F41D3EB53114753799E`), emulator-5554 only; test APK event `v423-dev1-test-20261018`.
- Current phase: step 05 develop: L3 and L4 code complete on host; building 4.23-dev.2, then regression, device QA and screenshots.
- Completed tasks: steps 01-04; L1 RAW tab; L2 multi-select; dev.1 build, clean-archive regression, device QA and QA report (`artifacts/qa/v4.23-dev1-20261018/QA_REPORT.md`).
- Pending tasks: owner approval to continue; L3 chapter sets (matcher, confirmation table, list; code written, device test written, not built); L4 per-chapter translation (binding, session snapshot, plan key, error policy, Translate screen); `4.23-dev.2` + QA + owner report; L5 owner trial D1-D7.
- Known bugs/limits: as v4.22 (`feature/v4.22` snapshot); a job report prints raw settings JSON when it cannot parse it.
- Regression status: clean archive `3848ebeb`: engine 742 / 1 SKIP, app debug, release, benchmark 533 each / 5 SKIP, 0 FAIL, lint 0 errors / 60 warnings, AndroidTest compile PASS; device 192 discovered / 176 PASS / 16 SKIP / 0 FAIL. Editorial boundary diff empty; five Editorial-named tests changed by one number (VER 29 -> 30).
- Semantic progress: unchanged.
- Spend: USD 0; no provider call. Phone `15e84958` not touched.
- Next action: build 4.23-dev.2 (code 277) and its test APK, run the clean-archive regression and device QA including V423ChapterSet/V423ChapterRun tests, take screenshots, write the dev.2 QA report and stop for the owner.
