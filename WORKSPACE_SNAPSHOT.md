# Workspace Snapshot

- Updated: 2026-10-02 (+07:00): work request docs/P6_R0_R7_WORK_REQUEST_20261002.md — step 0 (baseline docs, 4417d130) and R0 (issue table + fixtures) done; R1–R5 follow offline; no provider, device or pilot action in this session.
- Current version: active release v4.18; pilot 15e84958 still runs production 4.18-p6.2/code215 (51BA2A2B…9703), AndroidTest 26CB0563… (unchanged); the R0–R7 repair work is offline source only until R6.
- Current branch: feature/v4.18-p5e-runner-repair-20260917; same continuation, no new branch/release/checklist.
- Current commit: 4417d130 — implementation baseline immediately before this snapshot update.
- Current build: 4.18-p6.2/code215, build-20261002-082255 (source 4bc1aa27), archived in artifacts/ and backup/; source is ahead of it (UX fixes and the R-group work are not built).
- Current phase: P5_EXIT_PASS_UNDER_EXCEPTION_B; P6 repair R0 closed, R1 (contract v2) next; chapter 001 chain on the pilot is LEGACY_CONTRACT_V1.
- Completed tasks: step 0 baseline (engine 255/255, app unit 312/312 measured); R0: docs/P6_R0_ISSUE_TABLE.md (I-001..I-005, F1..F10), 14 private fixtures + docs/P6_R0_FIXTURE_MANIFEST.json (holdout locked), scripts/p6/build_fixtures.py and verify_fixtures.py (0 errors).
- Pending tasks: R1 contract v2 (inventory, ledger, identity revision, sizing); R2 L1 + persistence; R3 L2 + reconstruction + protected spans; R4 L3 + final-read; R5 evaluation harness and the execution table with questions Q1–Q5; R6/R7 need owner authority.
- Known bugs/gaps: L1 ledger/persistence/protected spans/final-read/identity-revision defects F1–F8 are confirmed in code and not yet fixed; pilot FINAL still contains I-002 (4 occurrences) and I-003; no semantic evaluation exists yet (only structural tests); holdout sites were chosen by rule with the text visible to the author (no prompt exists to tune).
- Regression status: editorial-engine 255/255, app unit 312/312 (measured at step 0); fixture verification 14 fixtures 0 errors; no new runtime code yet.
- Workspace/data: owner .idea and old P5E docs edits plus untracked evidence preserved, not staged; pilot DB C1F40D14… holds RAW, RECONCILE, L2_EDIT, L3_FINAL for chapter 001 (legacy contract v1, do not edit or restore); private fixtures in D:\P5E-private\p6-fixtures (not in Git).
- Evidence pointers: docs/P6_R0_ISSUE_TABLE.md; docs/P6_R0_FIXTURE_MANIFEST.json; docs/P6_CHAPTER_001_INDEPENDENT_AUDIT_20261002.md; EDITORIAL_RECOVERY_V4_18.md section 11.
- Current Next action: R1 — contract v2: RAW line-level inventory with stable unit ids, Error Ledger/candidate/finding/proof/protected-span schemas, metric definitions, contractRevision bound into the L1/L2/L3 attempt identity (v1 reports readable as LEGACY_CONTRACT_V1), sizing and chunk rules frozen by tests; close when negative fixtures from the R0 table round-trip.
