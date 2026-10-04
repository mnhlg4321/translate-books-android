# P6 R6 wire simplification U1–U6 execution — 2026-10-04

Scope: execute §11 of `P6_R6_WIRE_SIMPLIFICATION_PROPOSAL_20261004.md` after the owner-approved duplicate-reference decision. U1–U5 are offline; U6 may continue the existing G1 only after the exact saved `fx-a03` RECONCILE response passes replay. Stop before G2. No API key value is recorded; the pilot device is out of scope.

## U1–U4 — offline packages (PASS, pushed)

- **U1** (`77dd573e`, snapshot `24b2cba1`): contract revision V7; deterministically remove duplicate list references and references repeated between `rawUnits` and `occurrenceUnits`, preserving first-seen order and recording counts in artifact metadata. Duplicate entity definitions remain invalid. Engine 377/377; app 341/341. The exact `fx-a03` RECONCILE replay passed.
- **U2** (`468b70b4`, snapshot `17107948`): reject quotes that become empty after NFC, trim, and ruby-reading removal; engine 379/379; app 341/341.
- **U3** (`dd3e680d`, snapshot `e27a3075`): add offline replay-all for the older RAW, the accepted RAW, and the exact RECONCILE response. Results: expected `L1_UNIT_UNKNOWN:coverage.0.from` for the old RAW; current RAW PASS (32 candidates, one coverage range); RECONCILE PASS (1 finding, 32 resolutions, 1 duplicate reference removed); `REPLAY_ALL PASS`.
- **U4** (`be87734b`, snapshot `78851930`): replace the brief source excerpt in the report and test with synthetic furigana text. Repository search found no remaining matching excerpt.

The four packages and their snapshot commits are present on `origin/feature/v4.18-p5e-runner-repair-20260917`.

## U5 — clean wrapper build and emulator checks (PASS, offline)

Built from clean source `78851930091804317cac02d6af51c8d424628fc6` in temporary branch `codex/p6-u5-20261004` using `scripts/build-and-save.ps1 -Offline`. Production is `4.18-p6.21` / code 234, event `build-20261004-193841`; APK SHA-256 `ccefceb5513c55fbcd55a8c5fdbff9a877b9451db1acdd2e75d312fbe4fc8f2c`; exact-source ZIP SHA-256 `e2021b1f4cad770970cc97f98c43f6a8811b2fc16d9a35a852d159e8c39fcc23`. Five-file payloads in `artifacts/builds` and `backup/builds` match. The wrapper verified the signing certificate and installed/read back code 234 on `emulator-5554`.

The AndroidTest wrapper event `p6-u5-78851930-20261004-01` produced APK SHA-256 `df3e6a0fb7d3b21cccd13e599ae5e74789ab1879495169cef76a8eb5658c2661`, using the same source ZIP. Eight-file artifact and backup payloads match. The test APK was installed on `emulator-5554`. App unit tests were 341/341; engine tests were 380/380; wrapper lint passed. Emulator preflight passed 5/5, including deliberate wrong-fingerprint rejection with zero provider calls. Coordinator tests passed 12/12; the opt-in test that seeds the main database was skipped.

- Fake CHAIN run `4bb83094-7692-477a-9a5e-e0cef1fbcb46`, group `U5-FAKE-20261004-194212`: `STRUCTURAL_VALID 14/14`, fake semantic control FAIL 12 / PASS 2, 1,680 ledger entries, 0 pending UNKNOWN, 0 actual provider calls.
- Negative gate run `9b5904fc-bb49-4dbc-b7a7-6185c3c8ab72`, `fx-a03`: expected `STRUCTURAL_VALID 0/1`, stops `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`, `phase=L1_RAW_DISCOVERY`, `L1_COVERAGE_GAP:coverage`; USD 0, 0 actual provider calls, 0 pending UNKNOWN.

Private build and emulator logs are under `D:\P5E-private\p6-item11-u5-20261004\`; fake-run evidence is under `D:\P5E-private\p6-runs\4bb83094-7692-477a-9a5e-e0cef1fbcb46\` and `D:\P5E-private\p6-runs\9b5904fc-bb49-4dbc-b7a7-6185c3c8ab72\`.

## D-G1c replay gate

On the current production parser, the exact saved `fx-a03` RECONCILE response replayed as `PASS L1_RECONCILE findings=1 resolutions=32`. Response SHA-256 is `865c956537ef6f75f22e951dbd7fe62c199e61a00425666727bd202aac33b513`; replay log: `D:\P5E-private\p6-item11-u5-20261004\fx-a03-reconcile-exact-replay.log`.

Before U6, the same G1 device ledger was byte-identical at 5,237 bytes, SHA-256 `d4826b2bca52494441b9d056323fb5d14e98cd15d9df651ac77027f82f33d14e`: 7 settled calls / 14 entries, USD `0.05538945` spent of `1.00`, USD `0.94461055` remaining, 0 pending UNKNOWN. U5 added no G1 call. The exact-response D-G1c gate was met before resuming the group.

## U6 — G1 base resumed, group stopped at fx-a03

Run `3559de99-978b-410e-b863-c082062187fa` resumed the existing group `G1-20261003-ecbf8c55` at `fx-a03`, using the approved eight-fixture base order and retaining L1 only for `fx-a04`, `fx-a11`, and `fx-a02`. The production parser accepted RAW (38 candidates, one coverage range), then rejected RECONCILE. The runner stopped the entire group at `fx-a03`; it did not start another fixture or repeat round.

- Full stop details: `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`; `phase=L1_RECONCILE`; `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`.
- Actual calls: 2; `L1_RAW_DISCOVERY` cost USD `0.00680570`, `L1_RECONCILE` cost USD `0.01162005`, total USD `0.01842575`; 51,605 input / 4,604 output tokens; both finish reasons `stop`; 0 retry, 0 repair, 0 UNKNOWN.
- Exact saved responses: RAW `D:\P5E-private\p6-runs\3559de99-978b-410e-b863-c082062187fa\results\fx-a03\responses\001-L1_RAW_DISCOVERY.json` (SHA-256 `652045f9f28666f4e94296e93bf4403d37acb4819cd3a44ad5840b8238bafe8a`); RECONCILE `D:\P5E-private\p6-runs\3559de99-978b-410e-b863-c082062187fa\results\fx-a03\responses\002-L1_RECONCILE.json` (SHA-256 `18c5ddb34caf6d197b346ad4b86e8b785b6e8bae5cc96bdeea9de12e0ea126e3`).
- Replay-all output: expected old RAW code `L1_UNIT_UNKNOWN:coverage.0.from`; new RAW PASS (38 candidates, one coverage range); one unexpected code: **`L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`** for the RECONCILE response above; `REPLAY_ALL FAIL`. Full log: `D:\P5E-private\p6-item11-u6-20261004\replay-all.log`.
- G1 device ledger now matches the host snapshot at 6,732 bytes, SHA-256 `cd617c2fe252e006050ee8d760705fbbdc058187d10507d599e0905aac0a9997`: 9 settled calls / 18 entries, USD `0.07381520` spent of `1.00`, USD `0.92618480` remaining, 0 pending UNKNOWN. The ledger verifier passed, including capacity for another base fixture. No fixture after `fx-a03` was dispatched; G2 and the pilot remain untouched.

Complete unexpected `CODE:path` list:

`L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote` — `D:\P5E-private\p6-runs\3559de99-978b-410e-b863-c082062187fa\results\fx-a03\responses\002-L1_RECONCILE.json`

No further provider call was made. Resume G1 only after offline diagnosis, a regression test, and replay of this exact RECONCILE response to PASS under D-G1c; stop before G2.
