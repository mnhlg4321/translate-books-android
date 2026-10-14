# Workspace Snapshot

- Updated: 2026-10-14 (+07:00): v4.22 opened from owner feedback on 4.21.0; work requests written.
- Current version: v4.22.0 in development; shipped 4.21.0 / code 271 (tag `v4.21.0`), installed on the owner phone `15e84958`.
- Current branch: `feature/v4.22` in worktree `D:\P5E-builds\wt-v4.22` (from `main` `7c44f3a2`).
- Current commit: `0b05e934` is the implementation baseline immediately before the U3/snapshot commit (not a self-reference); actual HEAD verified.
- Current build: none for v4.22. Shipped 4.21.0 / code 271 in `artifacts/releases/v4.21.0/tag-v4.21.0/`.
- Current phase: step05, U1/U2 clean-archive PASS; U3 implementation awaiting clean-archive regression, then P/G/dev QA. Q1 isolated offline work in progress.
- Completed tasks: phone install of 4.21.0 with verified backup; Q0 diagnosis of Kumo vol 7 ch 013 (private data in `D:\P5E-private\v422-q0\`); owner decisions for 4.22 and Q1.
- Pending tasks: `docs/V422_WORK_REQUEST.md` steps 1-6 (U1, U2, U3, P, G, dev build and emulator QA, then owner tries the dev build) and step 7 (release after owner approval); `docs/V422_Q1_SPEAKER_DESIGN_REQUEST.md` (offline design, reference labels, proposal for a paid measurement).
- Known bugs/limits: Editorial does not know who speaks each line (false ADDRESS points, missed wrong-speaker forms); glossary false points (single-character sources, pronoun-row cues, pronoun replaced by a name); results screen and setup hard to read; Editorial runs on a screen thread without notification. Release builds are not debuggable, so `run-as` backups no longer work on the phone.
- Regression status: U1 archive9390dc24 PASS engine/app debug-release-benchmark/lint/AndroidTestcompile; U2 initial compile failure in test Files.readString retained, repaired0b05e934 archive PASS (Files.readAllBytes). U3 pending; no APK built/provider calls.
- Semantic progress: Q0 on Kumo ch 013: 2 real pronoun errors in 220 lines, 0 fixed by the app.
- Spend: USD 0 for v4.22.
- Next action: finish U3 clean regression then P/G/dev build and emulator QA; finish isolated Q1 offline evidence, with no provider/phone/tag/merge.
