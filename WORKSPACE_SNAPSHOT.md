# Workspace Snapshot

- Updated: 2026-10-05 (+07:00): Z3 done offline: a live group now treats a typed refusal of the production engine as a measurement (run_group.ps1 default; -StopOnFirstRefusal restores the old rule). `scripts/p6/group_policy.py` decides after each fixture: continue on VALID or REFUSED; stop only on infrastructure/UNKNOWN cost, the group cap, or 3 consecutive refusals with the same code. The instrumented runner records a typed refusal (structural.json measuredRefusal) instead of failing, and verify_fixture_run.py accepts measured refusals (`--measure-refusals`). Python 49/49, engine 420, app unit PASS, androidTest compile PASS. No provider call.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: 1cfb6e5e — implementation baseline immediately before this snapshot update; confirm actual HEAD on resume.
- Current build: no APK build in this package; Z4 rebuilds through the wrapper after Z2/Z3.
- Current phase: P6 R6 Z3 complete offline; Z4 (wrapper build + emulator checks) pending, then Z5 live G1.
- Completed tasks: W1, W2, W5, Z1, Z2, Z3 (policy + runner + verifier + tests).
- Pending tasks: Z4 wrapper build and emulator checks (preflight, coordinator, fake CHAIN 14/14, negative gate, 0 calls); Z5 full G1 (8 fixtures + 2 repeat rounds) within USD 0.90641910, scored with score_run.py, stop before G2.
- Known bugs: none known in the Z1/Z2 scope. The saved W4 RECONCILE response had a RAW unit number off by two (L321 vs the quote on 323): RAW anchors are now derived from rawQuote like the DRAFT ones.
- Regression status: engine all PASS, app unit PASS, androidTest compile PASS, Python 33/33 at e8a78edc. G1 ledger unchanged: 11 settled calls, USD 0.09358090 / 1.00, USD 0.90641910 remaining, 0 pending.
- Next action: Z4: build production and AndroidTest APKs through the wrappers from a clean worktree at the Z3 HEAD, install on emulator-5554 only, run the offline checks.
