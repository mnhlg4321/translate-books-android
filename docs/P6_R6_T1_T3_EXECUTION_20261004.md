# P6 R6 T1–T3 execution — 2026-10-04

Scope: `docs/P6_R6_WIRE_SIMPLIFICATION_PROPOSAL_20261004.md` §10 (HEAD `75ae33b3`, owner approved D-G1c with the standing replay-proven continuation rule). T1–T2 offline, then T3: continue G1 on the same group ledger starting at `fx-a03`; stop on a new error code, UNKNOWN cost or cap, report `CODE:path` and the response path, and do not start G2. Emulator `emulator-5554` only; the pilot was not accessed. No key or fingerprint value is recorded here.

## T1 — golden wires through the generated schema (offline, PASS)

`EditorialGoldenWireSchemaTest` (8 tests, commit `011b1883`): golden L1 RAW, L1 RECONCILE (every optional field empty, CONTINUE with empty `reasonCode`, STOP, PRESERVE_DRAFT) and final-read wires are checked by a small schema checker (type, required, additionalProperties, min/max length and items, pattern, enum) against the schema the provider actually sends, and by the production parser, with unit references `L1`, `L9`, `L10`, `L99`, `L100`, `L400`; a reference past the inventory passes the schema and is refused by the parser with `L1_UNIT_UNKNOWN:candidates.0.unitId`; every string field of all eight FieldSpec phases must have a satisfiable leaf schema that admits its shortest legal value and refuses `L0`, `L`, `l1`, `L01`, a full `u:…` id and an overlong value. Mutation check: reverting `MIN_UNIT_REFERENCE_LENGTH` to 3 makes 6 of the 8 tests fail. Engine 371/371, app unit 341/341, androidTest compile PASS, Python 33/33 (scorer 16, ledger 3, verifier 9, prompt inputs 5).

## T2 — rebuild, emulator checks, replay of the old response (PASS, 0 calls)

- Source `8e4b9fff` (contains `7f3c25ba`; the only later change is the host script `run_group.ps1`), clean temporary worktree `D:\P5E-builds\wt-p6-t2-20261004` (removed afterwards).
- Production wrapper (`-Offline`): `4.18-p6.18` / code 231, event `build-20261004-093804`, APK SHA-256 `C200014D315189A19AF4DDCC2EA5E3F866815CA408A1E6C5B9829B7C1650F513`, source ZIP SHA-256 `0CC00D38073003287DD65DF4CC7131031182F6682DCEBD9A1A28861DE292CC02`; payload (5 files) identical in `artifacts/builds` and `backup/builds`.
- AndroidTest wrapper: event `p6-t2-8e4b9fff-20261004-18`, APK SHA-256 `9611FF4A49204F5D7138FEC2DAC30CEE86F4D64495EFBD72891B34CD4889D313` (same bytes as the earlier `p6-s4-neg2` APK: no androidTest source changed), same source ZIP; payload (8 files) identical in `artifacts/test-builds` and `backup/test-builds`. Both installed on `emulator-5554` with `adb install -r -d` and read back by hash; the G1 group ledger on the device (3,742 bytes) survived.
- Emulator: preflight class 5/5 (including the deliberately wrong fingerprint, 0 calls), coordinator class 12/12 (ledger chain, protection, restart, interrupted claims, one skipped main-DB seed opt-in); fake CHAIN run `5b0c7e0e-2a74-4c83-9b6f-0c8d6d1c0a13` `STRUCTURAL_VALID 14/14`, 0 actual provider calls, 1,680 summed ledger entries, 0 pending (semantic FAIL 12 / PASS 2 is the no-edit fake control); negative gate run `5b0c7e0e-2a74-4c83-9b6f-0c8d6d1c0a14` (`fx-a03`, fake invalid L1) `STRUCTURAL_VALID 0/1` with stops `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`, `phase=L1_RAW_DISCOVERY`, `L1_COVERAGE_GAP:coverage`, 0 actual calls.
- Replay of the old `fx-a03` response (`7b6ac5b8-…/001-L1_RAW_DISCOVERY.json`) on the new code still reports `L1_UNIT_UNKNOWN:coverage.0.from`: that response (`L1173` in a 383-line RAW) really was invalid. Logs: `D:\P5E-private\t2-*.log`, `t2-fx-a03-replay.txt`.
- Script fixes made on the way (`run_group.ps1`): an offline-only `-NegativeGate` switch, and unwrapping of `fixture-ids.json` (Windows PowerShell 5.1 wraps a JSON array in one object, so a default all-fixture run failed the guard). Invoke `run_group.ps1` through `powershell -NoProfile -Command "& …"` with the output redirected at process level: a `*>` redirect inside the same PowerShell makes `adb push` progress lines terminating errors, and `-File` passes `a,b` as one string.

## T3 — G1 continued on ledger `G1-20261003-ecbf8c55`: stopped at fx-a03 on a new code

Zero-call matching preflight before the run: the owner fingerprint already given in this session matched the saved emulator key/route (the run reached the deliberately absent runtime manifest after the comparison; 0 calls). Run `3a9c1d54-7e20-4b6f-9a31-5c8e4f2d7b02`, mode `L1_ONLY`, live, planned base set `fx-a03, fx-a04, fx-a05, fx-a07, fx-a08, fx-a11, fx-a02, fx-a12` with L1 retained for `fx-a04`, `fx-a11`, `fx-a02`.

| Phase | Result | Real provider USD |
|---|---|---:|
| `L1_RAW_DISCOVERY` | **accepted by the production engine**: 32 candidates, 1 coverage range (replay `PASS`) | 0.0067793 |
| `L1_RECONCILE` | refused: **`L1_RAW_QUOTE_NOT_IN_ANCHOR:findings.0.rawQuote`** | 0.01081895 |

2 calls, 51,535 input / 3,929 output tokens, both `finish=stop`, 0 retry, 0 repair, 0 UNKNOWN. G1 ledger after the run: **7 settled calls / 14 entries, USD 0.05538945 / 1.00, USD 0.94461055 remaining, pending 0** (hash chain verified; the previous ledger is a byte prefix). The group stopped after `fx-a03`; `fx-a04` … `fx-a12` and the two repeat rounds were not started. No G2, no pilot.

Response paths (outside Git):
- `D:\P5E-private\p6-runs\3a9c1d54-7e20-4b6f-9a31-5c8e4f2d7b02\results\fx-a03\responses\001-L1_RAW_DISCOVERY.json` (SHA-256 `a3c83a1c7470cd09b5f2bd206530ef9c811a0f7060af41f8df2db2a91a994bf4`)
- `D:\P5E-private\p6-runs\3a9c1d54-7e20-4b6f-9a31-5c8e4f2d7b02\results\fx-a03\responses\002-L1_RECONCILE.json` (SHA-256 `865c956537ef6f75f22e951dbd7fe62c199e61a00425666727bd202aac33b513`)

Offline replay reproduces the refusal: `.\gradlew.bat --offline :editorial-engine:replayP6WireResponse -Pp6ReplayPhase=L1_RECONCILE …` prints `L1_RAW_QUOTE_NOT_IN_ANCHOR:findings.0.rawQuote` (log `D:\P5E-private\t3-fx-a03-replay-reconcile.txt`).

Scorer on the partial output: `STRUCTURAL_VALID 0/1`, `SEMANTIC_EVAL FAIL` (`KNOWN_DEFECT_NOT_FIXED:T-S1`, because L1-only ends with the DRAFT) — not a quality result. Captured prompts contain no label text. `verify_fixture_run.py` was not run on the partial output (its checks need a committed REPORT_L1).

### What the refusal is (diagnosed offline from the stored response, not a model-quality finding)

- The model found exactly the seeded defect: one finding on `L99`, type `UNTRANSLATED`, DRAFT lines 99–99, `draftQuote` present in the DRAFT line, observation and expected meaning correct; 32 candidate resolutions; disposition CONTINUE with an empty `reasonCode` (the S3b rule worked).
- The `rawQuote` (13 characters) is not a substring of RAW line 99 because that RAW line carries ruby furigana markup `…揃《そろ》えても…`; the model quoted the reading-free text `…揃えても`. No RAW line contains the quote. So the validator's exact-substring rule rejected a faithful quote of the visible sentence.
- Same family elsewhere (not yet exercised): L2 `evidenceQuote`, L3 probe `rawQuote`/`viQuote`, final-read `quote` use the same exact-substring comparison; RAW chapters carry `《…》` ruby throughout.

### Proposed fix (needs a decision under the standing rule: this is a new code, so the group stopped)

Compare quotes against the anchored RAW text after the same normalization the proposal already uses for `before` (NFC, trim) **plus removal of ruby markup `《…》` from the anchor text only**, and tell the model in the prompt that RAW shows ruby as `漢字《よみ》` and a quote may omit the `《…》` part. The check stays a substring test against the real source, so it still proves the quote comes from the anchored unit. Regression test: this exact response must replay to `PASS`; add the same normalization test for L2/L3/final-read quote fields; golden wires with ruby. Then continue G1 from `fx-a03` (a new attempt; the failed attempt stays in the ledger) within the remaining USD 0.94461055.

## Next action

Owner/Claude decision on the ruby-aware quote comparison (it changes validation, not just a prompt), then a new G1 continuation from `fx-a03` under the standing rule; nothing further is dispatched before that.

## Item 10 continuation — quote fix, wrapper rebuild, and replay gate (2026-10-04)

The owner-approved D-G1c standing rule was applied: the exact refused response must replay successfully with a regression test before continuing live G1. No provider call was made during this continuation.

### T1 — quote comparison regression (offline, PASS)

- `bee24f9a2cc1c1fb71ff9b0b57d86665cfd18063` adds a shared matcher for NFC and outer whitespace normalization and ruby-aware RAW anchors across L1, L2, L3 and final-read quote fields; prompts explain RAW `漢字《reading》` markup. Contract revision is `L1_LEDGER_V6`.
- The first exact-response replay passed the ruby quote check but exposed the next independent invariant below. To cover both provider forms and the local fake harness, `3480dda8a7638a072ef05afd277fa1afd358c2d8` also removes closed `《…》` spans from quote excerpts during RAW-anchor comparison; the regression asserts both reading-free and ruby-marked excerpts are accepted, while a quote consisting only of the reading is rejected.
- Offline checks on the follow-up source: engine 375/375, app 341/341, AndroidTest compile PASS; wrapper clean build also ran app unit tests and lint successfully. The PowerShell 5.1 adb transfer repair is included in `run_group.ps1` and was checked with the 5.1 parser.

### T2 — clean-HEAD wrapper build and emulator validation (offline, PASS)

- Clean worktree HEAD `3480dda8a7638a072ef05afd277fa1afd358c2d8`; production built through `scripts/build-and-save.ps1 -Offline`: `4.18-p6.20` / code 233, event `build-20261004-103109`, APK SHA-256 `746AF50FCD1ADCCD246665FD15A6CF162B2BDC8758DB5BF1C89C29E5E81C1E49`, exact-source ZIP SHA-256 `281DF34DAEF12459ED1255F26C89CA25C29D454BBDA4718208B0D33DDB5F275F`. Five-file payloads in `artifacts/builds` and `backup/builds` were byte-identical. Installed and verified on `emulator-5554` only.
- AndroidTest wrapper event `p6-ruby-anchor-3480dda8-20261004-01`; APK SHA-256 `316E03A3F6657B6A84F7BD5DC9939E9F262FEC9E67C8DBDB91E90CEBBC5104C4`, same exact-source ZIP. Eight-file payloads in `artifacts/test-builds` and `backup/test-builds` were byte-identical; APK installed on `emulator-5554`.
- Emulator preflight `5/5` and coordinator `12/12` passed. Fake CHAIN run `8f9a1072-f2ae-488c-b00a-b402f5661014`: `STRUCTURAL_VALID 14/14`, 0 actual provider calls, 1,680 ledger entries, 0 pending UNKNOWN; fake no-edit semantic control was FAIL 12 / PASS 2. The PowerShell 5.1 transfer path completed.
- Negative gate run `8f9a1072-f2ae-488c-b00a-b402f5661015`, only `fx-a03`: `STRUCTURAL_VALID 0/1` as expected, `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`, `phase=L1_RAW_DISCOVERY`, `L1_COVERAGE_GAP:coverage`, 0 actual provider calls, 0 cost.
- The older invalid RAW response still replays to `L1_UNIT_UNKNOWN:coverage.0.from`; response `D:\P5E-private\p6-runs\7b6ac5b8-aea9-44b0-8f92-da72ca0d2e7e\results\fx-a03\responses\001-L1_RAW_DISCOVERY.json`. Final replay log: `D:\P5E-private\p6-item10-qa-20261004\fx-a03-raw-replay-final.log`.

### T3 — stop before another live call on the exact-response replay gate

On the final parser, replaying the exact `fx-a03` RECONCILE response now advances past the furigana-aware quote check and stops at the independent overlap invariant:

`L1_OCCURRENCE_DUPLICATE:findings.0.occurrenceUnits.0`

Response to replay: `D:\P5E-private\p6-runs\3a9c1d54-7e20-4b6f-9a31-5c8e4f2d7b02\results\fx-a03\responses\002-L1_RECONCILE.json` (SHA-256 `865C956537EF6F75F22E951DBD7FE62C199E61A00425666727BD202AAC33B513`). The finding repeats a unit already present in `rawUnits`; the parser currently requires `occurrenceUnits` to identify additional distinct units. Replay log: `D:\P5E-private\p6-item10-qa-20261004\fx-a03-reconcile-replay-final.log`.

This response does not yet replay to PASS, so the standing D-G1c continuation condition is unmet. The group was not restarted and no additional provider call or repair call was sent. The current device ledger was read back as 5,237 bytes, SHA-256 `D4826B2BCA52494441B9D056323FB5D14E98CD15D9DF651AC77027F82F33D14E`, 7 settled calls / 14 entries, USD `0.05538945` of `1.00`, USD `0.94461055` remaining, pending UNKNOWN `0`. This includes the two previously reported calls; this continuation added USD `0`. No G2 or pilot access.

## Next action

Resolve offline whether `occurrenceUnits` must exclude the primary `rawUnits` anchor or whether the parser should accept that overlap; add a regression and replay this exact response to PASS before resuming G1 at `fx-a03`. Stop before G2.
