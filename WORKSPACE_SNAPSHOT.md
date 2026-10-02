# Workspace Snapshot

- Updated: 2026-10-02 (+07:00): G5 offline slice done — chapter progress (read-only inspect over durable rows), FINAL viewer and verified TXT export on the P4 chapter card; run L2/L3 stays locked. Found that L2_RAW_DISCOVERY is not wired and its candidate-count comparison has no discriminating power with the current REPORT_L1 (populationTotal=1/accountedTotal=1); owner decision needed before live L2/L3.
- Current version: active release v4.18; installed production 4.18-p5e.5/code213 (APK 88854E47…5793), AndroidTest 26CB0563…; neither contains the G5 UI changes (a6da1a35).
- Current branch: feature/v4.18-p5e-runner-repair-20260917; same continuation, no new branch/release/checklist.
- Current commit: a6da1a35 — implementation baseline immediately before this snapshot update.
- Current build: 4.18-p5e.5/code213, build-20261002-073259 (source b3f6e3de); no build for G5 yet.
- Current phase: P5_EXIT_PASS_UNDER_EXCEPTION_B (chapter 001, app/DB level); P6 offline: engines, adapters, coordinator, export and now progress/viewer/export UI done; run-action UI and live L2/L3 pending.
- Completed tasks: G1; G2 (M4–M7); G3; G4 core + L3 adapter; coordinator + TXT export; G5 progress/inspect/viewer/export (a6da1a35).
- Pending tasks: run-action UI with user-confirmed per-phase caps (offline); owner decisions (three chapters, per-phase budget, L2_RAW_DISCOVERY approach); G6 three finals; P7 regression, numbered build, archives, device QA.
- Known bugs/gaps: G5 UI and the 4 new instrumented coordinator tests have not run on a device; L2_RAW_DISCOVERY not wired (contract phase skipped; see plan section 10); no live L2/L3 run; REPORT_L1 semantic quality not reviewed; reopen verified at DB level only; event 7 formal verdict stays RAW_NOT_ACCEPTED (exception B); L2/L3 edits are line-level only; REPORT_L1 carries no protected spans; historical owner-window failure UNRESOLVED; code196 loss stays FAIL.
- Regression status: editorial-engine 247/247, app unit 298/298, lint PASS, androidTest compile PASS; no device test run for G5.
- Workspace/data: owner .idea and old P5E docs edits plus untracked evidence preserved, not staged; device DB v25 06C4C48E… holds the committed RAW + RECONCILE; do not restore any DB.
- Evidence pointers: EDITORIAL_RECOVERY_V4_18.md sections 9a and 10; docs/P5E_CONSOLIDATED_FAILURES_20260930.md section Event M4 lần 2; release_checklists/v4.18-editorial-v5-safe-4-1-3.md.
- Current Next action: offline G5 — run-action UI on the chapter card (authorization dialog with user-confirmed per-phase caps, background run through EditorialChapterFinalCoordinator.runToFinal, progress via inspect) with JVM/fake-provider tests; live L2/L3 waits for the owner's three chapters, per-phase budget and the L2_RAW_DISCOVERY decision.
