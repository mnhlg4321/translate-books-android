# Workspace Snapshot

- Updated: 2026-10-02 (+07:00): verified M4 attempt 2 (RECONCILE_COMMITTED, REPORT_L1 b33baf33…) and the G5 slice; wrote docs/P6_G5_G6_WORK_REQUEST_20261002.md (analysis, owner decisions D1–D4, groups W1–W6). Device 15e84958 online with 4.18-p5e.5/213; AVD tbl-code113-dqa-api35 available for instrumented QA.
- Current version: active release v4.18; installed production 4.18-p5e.5/code213 (APK 88854E47…5793), AndroidTest 26CB0563…; neither contains the G5 UI changes (a6da1a35).
- Current branch: feature/v4.18-p5e-runner-repair-20260917; same continuation, no new branch/release/checklist.
- Current commit: d71865f1 — implementation baseline immediately before this snapshot update.
- Current build: 4.18-p5e.5/code213, build-20261002-073259 (source b3f6e3de); no build for G5 yet.
- Current phase: P5_EXIT_PASS_UNDER_EXCEPTION_B (chapter 001, app/DB level); P6 offline: engines, adapters, coordinator, export and now progress/viewer/export UI done; run-action UI and live L2/L3 pending.
- Completed tasks: G1; G2 (M4–M7); G3; G4 core + L3 adapter; coordinator + TXT export; G5 progress/inspect/viewer/export (a6da1a35).
- Pending tasks: run-action UI with user-confirmed per-phase caps (offline); owner decisions (three chapters, per-phase budget, L2_RAW_DISCOVERY approach); G6 three finals; P7 regression, numbered build, archives, device QA.
- Known bugs/gaps: G5 UI and the 4 new instrumented coordinator tests have not run on a device; L2_RAW_DISCOVERY not wired (contract phase skipped; see plan section 10); no live L2/L3 run; REPORT_L1 semantic quality not reviewed; reopen verified at DB level only; event 7 formal verdict stays RAW_NOT_ACCEPTED (exception B); L2/L3 edits are line-level only; REPORT_L1 carries no protected spans; historical owner-window failure UNRESOLVED; code196 loss stays FAIL.
- Regression status: editorial-engine 247/247, app unit 298/298, lint PASS, androidTest compile PASS; no device test run for G5.
- Workspace/data: owner .idea and old P5E docs edits plus untracked evidence preserved, not staged; device DB v25 06C4C48E… holds the committed RAW + RECONCILE; do not restore any DB.
- Evidence pointers: EDITORIAL_RECOVERY_V4_18.md sections 9a and 10; docs/P5E_CONSOLIDATED_FAILURES_20260930.md section Event M4 lần 2; release_checklists/v4.18-editorial-v5-safe-4-1-3.md.
- Current Next action: owner fills section 3 (D1–D4) of docs/P6_G5_G6_WORK_REQUEST_20261002.md and hands it to a new session (W1 L2_RAW_DISCOVERY option d → W2 run-action UI → W3 emulator instrumented QA; W4–W6 on the pilot only once approved).
