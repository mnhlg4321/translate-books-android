# Workspace Snapshot

- Updated: 2026-10-02 (+07:00): owner checked D1–D4; W4 (pilot install of 4.18-p6.2/215) and W5 (one live L2/L3 chain, chapter 001) done, W6 technical part done (reopen + export hash match). Owner accepted the chapter 001 FINAL quality (1 of 3 representative chapters); two more chapters are pending.
- Current version: active release v4.18; pilot 15e84958 runs production 4.18-p6.2/code215 (51BA2A2B…9703), AndroidTest 26CB0563… (unchanged).
- Current branch: feature/v4.18-p5e-runner-repair-20260917; same continuation, no new branch/release/checklist.
- Current commit: 8609c132 — implementation baseline immediately before this snapshot update (uncommitted UX fix and evidence docs are included in the update).
- Current build: 4.18-p6.2/code215, build-20261002-082255 (source 4bc1aa27), archived in artifacts/ and backup/; source now has two UX fixes after it (describeRunning, viewer layout) that are not built or installed.
- Current phase: P5_EXIT_PASS_UNDER_EXCEPTION_B; P6: chapter 001 FINAL committed and quality-accepted by the owner; G6 needs two more chapters; P7 not started.
- Completed tasks: W1–W6 technical scope; chain evidence (rows, hashes, calls, USD 0.04257), reopen and TXT export verified; UX fixes committed in source.
- Pending tasks: owner picks two more chapters from docs/P6_G6_CHAPTER_CANDIDATES_20261002.md and authorizes their setup, L1 and chain; decide how to treat the L1-frame and candidate-count gaps; rebuild/reinstall for the UX fixes only with new authority; G6, P7.
- Known bugs/gaps: FINAL differs from the DRAFT in 1 of 386 lines (quality equals the draft's); candidate counts L2 49 vs L3 257 are not reconciled; REPORT_L1 has populationTotal=1; per-call tokens/cost are not persisted in the artifact rows (only in the runtime log); installed build still shows the CLAIMED row as an unknown-state stop while running and its FINAL viewer close button can be off screen (Back works); historical owner-window failure UNRESOLVED; code196 loss stays FAIL.
- Regression status: editorial-engine 255/255, app unit 312/312, lint PASS, androidTest compile PASS; emulator instrumented 7+8+5+19 OK on the 215 pair; pilot: one live chain, no instrumented test run.
- Workspace/data: owner .idea and old P5E docs edits plus untracked evidence preserved, not staged; pilot DB C1F40D14… holds RAW, RECONCILE, L2_EDIT, L3_FINAL (do not restore any DB); exported file Download/editorial_001_final.txt on the phone; evidence in D:\P5E-private\p6-* (not in Git).
- Evidence pointers: docs/P6_W4_W6_CHAPTER_001_EVIDENCE_20261002.md; docs/P6_W3_EMULATOR_QA_20261002.md; docs/P6_G5_G6_WORK_REQUEST_20261002.md.
- Current Next action: the owner chooses the two remaining chapters (proposal 007 and 010; 013 if 001 does not count as short) and authorizes per chapter: project/binding setup on the pilot, L1 RAW + RECONCILE, and the L2/L3 chain; plus a decision on the L1-frame and candidate-count gaps.
