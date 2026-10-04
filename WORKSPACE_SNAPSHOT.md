# Workspace Snapshot

- Updated: 2026-10-05 (+07:00): W5/W3 verified (engine 405/405, app 341/341). W4 live: fx-a03 RAW passed a 4th time; RECONCILE refused L1_DRAFT_ANCHOR_UNUSED_FIELD (all 3 findings filled draft.after). G1 11 calls, USD 0.0936/1.00. Seven of seven live stops have been format/bookkeeping, none content. Owner asked for a decisive fix: docs/P6_R6_DECISIVE_FIX_PLAN_20261005.md (Z1 classify every rejection rule SEMANTIC vs BOOKKEEPING and normalize all bookkeeping at once incl. L2/L3; Z2 mutation tests from real responses + strict schemas for L2/L3; Z3 a rejection is a measurement, not a group stop).
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: 349717d8 — implementation baseline immediately before this snapshot update; confirm actual HEAD on resume.
- Current build: `4.18-p6.22`/code235, event `build-20261004-205658`, source `e8a78edc`; production APK SHA-256 `C0F2CEB4049D2C7F7E3ED0FE9B05826E7B5BCC506685658669E2DFEB5379A89F`, AndroidTest `5A11B6277F8CB4DE2103697D258F85A5C3BADFCF34857B563A197A19F1B25DBD` (event `p6-w3-e8a78edc-20261005-01`). Installed on `emulator-5554` only and archived in both payload roots.
- Current phase: P6 R6 W4 stopped at fx-a03 on a new error code; waiting for the coordinator/owner decision on unused anchor fields.
- Completed tasks: W1, W2, W5, replay-all PASS, W3 build/emulator checks, W4 fx-a03 attempt (2 live calls, STRUCTURAL_VALID 0/1).
- Pending tasks: Decision/fix for unused DRAFT anchor fields (`after` on LINES, `start`/`end` on MISSING), then continue G1 from fx-a03 under D-G1c; remaining fixtures and repeat rounds not started; G2 out of scope.
- Known bugs: Live fx-a03 RECONCILE `L1_DRAFT_ANCHOR_UNUSED_FIELD:findings.0.draft.after`: all 3 findings set after=line on LINES anchors. Response `D:\P5E-private\p6-runs\2a2fe112-cae7-4414-b828-adc204e50d50\results\fx-a03\responses\002-L1_RECONCILE.json`, SHA-256 `F8245DFA7FF694C39CD576058E423CDE520D345E63E951C44D6E0EF46B2EC89B`.
- Regression status: Offline suites as at e8a78edc (engine 404/404, app unit PASS, Python 33/33). G1 ledger: 11 settled calls / 22 entries, USD 0.09358090 / 1.00, USD 0.90641910 remaining, 0 pending UNKNOWN (hash chain verified); no G2 or pilot access.
- Next action: owner decides D-Z3; Codex does Z1–Z4 offline then Z5 (full G1, 8 fixtures + repeats) per docs/P6_R6_DECISIVE_FIX_PLAN_20261005.md.
