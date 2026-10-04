# Workspace Snapshot

- Updated: 2026-10-04 (+07:00): item 11 U1–U5 PASS and pushed. U6 resumed G1 on the same ledger, then stopped at `fx-a03` on a new RECONCILE code; replay-all recorded the full response code/path.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: `8fb2ceef75c2de363a9cb825853f2c9b1c304fd0` — implementation baseline immediately before this snapshot commit; confirm actual HEAD on resume.
- Current build: `4.18-p6.21`/code234, event `build-20261004-193841`, source `78851930`; APK SHA-256 `ccefceb5513c55fbcd55a8c5fdbff9a877b9451db1acdd2e75d312fbe4fc8f2c`, exact-source ZIP SHA-256 `e2021b1f4cad770970cc97f98c43f6a8811b2fc16d9a35a852d159e8c39fcc23`. AndroidTest event `p6-u5-78851930-20261004-01`, APK SHA-256 `df3e6a0fb7d3b21cccd13e599ae5e74789ab1879495169cef76a8eb5658c2661`. Both payloads matched artifacts/backup and are installed on `emulator-5554` only.
- Current phase: item 11 U1–U5 PASS; U6 stopped at `fx-a03` RECONCILE on `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote` after replay-all.
- Completed tasks: contract revision V7; deterministic duplicate-reference normalization; empty-normalized-quote rejection; replay-all verifies the expected old RAW refusal, current RAW pass and exact RECONCILE pass; source excerpt replaced by synthetic text; U5 wrapper build, archive parity, emulator preflight/coordinator/fake CHAIN/negative gate. Engine 380/380; app 341/341; lint PASS.
- Pending tasks: diagnose the exact `fx-a03` RECONCILE response offline; continue G1 only if a regression and exact-response replay pass under D-G1c. Do not start G2.
- Known bugs: the old RAW response is expected to remain invalid with `L1_UNIT_UNKNOWN:coverage.0.from`; latest live RECONCILE response stops at `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`.
- Regression status: latest G1 ledger readback is 6,732 bytes, SHA-256 `cd617c2fe252e006050ee8d760705fbbdc058187d10507d599e0905aac0a9997`; 9 settled calls / 18 entries, USD `0.07381520` / `1.00`, USD `0.92618480` remaining, 0 pending UNKNOWN. The two U6 calls cost USD `0.01842575`; no G2 or pilot access.
- Next action: diagnose `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote` offline and prove any fix with a regression plus replay of the exact RECONCILE response before resuming G1; stop before G2.
