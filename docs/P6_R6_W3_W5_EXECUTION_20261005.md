# P6 R6 — W5, replay-all and W3 execution (2026-10-05)

Scope: `docs/P6_R6_WIRE_SIMPLIFICATION_PROPOSAL_20261004.md` §13 (HEAD `1d05117b`, coordinator decision W5). No provider
call in this document's steps; the live W4 continuation is recorded separately.

## W5 — speaker records with an unusable reference are dropped and counted (commit `e8a78edc`)

- `EditorialL1Ledger.parseReconcile`: a `speakerRecords[]` row whose `unitId` fails with `L1_UNIT_REF_INVALID`,
  `L1_UNIT_UNKNOWN` or `L1_UNIT_LINE_NOT_A_UNIT` (malformed, beyond the text, blank/excluded line, non-string, empty)
  is dropped from the artifact; the app never guesses another line. Every other defect of a record (unknown key,
  missing key, blank speaker/basis, over-long text), and findings, coverage, candidates, resolutions and protectedSpans,
  stay strict.
- REPORT_L1 `normalizations` carries `speakerRecordsDropped` (count) and `speakerRecordsDroppedPaths` (safe
  `speakerRecords.N.unitId` paths); `parseBody` restores them. `contractRevision` is `L1_LEDGER_V9`.
- `EditorialFieldSpec`: the `speakerRecords` array rule now reads "MAY be empty; an optional side note nothing depends
  on, so a record whose unitId is not a unit line is dropped, never repaired" (prompt and schema description).
- Replay tool: every case reports `normalizations`; a manifest case may bind `expectedSpeakerRecordsDropped`.
- Tests are synthetic (`EditorialL1SpeakerRecordDropTest` 8 tests + a replay-tool manifest test). Engine 404/404, app unit
  PASS, androidTest compile PASS, Python 33/33.

## Replay-all (offline)

Manifest `D:\P5E-private\p6-w5-replay-all-manifest-20261005.json` (SHA-256
`8685AC6AA49D8D4F54E5710B4611F4F7858FBF734BCD9BD982958E06A62FC577`), result
`D:\P5E-private\p6-w5-replay-all-20261005.json` (SHA-256
`582319DA24E307D7133F0188DDBB6E20838E4FD8D63E6366EE9F5DE34FA2421E`), conclusion **PASS**:

| Case | Expected | Actual |
|---|---|---|
| old RAW (`7b6ac5b8…`) | EXPECTED_REJECT `L1_UNIT_UNKNOWN:coverage.0.from` | as expected |
| current RAW (`3559de99…/001`) | PASS | PASS |
| U6 RECONCILE (`3559de99…/002`, SHA-256 `18c5ddb3…ea126e3`) | PASS with `expectedSpeakerRecordsDropped=2` | **PASS**, `speakerRecordsDropped=2`: `speakerRecords.6.unitId`, `speakerRecords.7.unitId`; both findings and every later invariant completed |

## W3 — wrapper build and emulator checks (0 provider calls)

- Source `e8a78edc07c48cfa3874671945620c74e675de8b`, clean temporary worktree `D:\P5E-builds\wt-p6-w3-20261005` (removed
  afterwards, junctions removed first).
- Production: `4.18-p6.22` / code 235, event `build-20261004-205658`, APK SHA-256
  `C0F2CEB4049D2C7F7E3ED0FE9B05826E7B5BCC506685658669E2DFEB5379A89F`, source ZIP SHA-256
  `C5390A4E34ABB080165B3B608CD6F96BFBBB54F0B79452BB53D426BBA091F71D`; 5-file payload in both `artifacts/builds` and
  `backup/builds` (APK hash identical in both). The wrapper ran app unit tests and lint.
- AndroidTest: event `p6-w3-e8a78edc-20261005-01`, APK SHA-256
  `5A11B6277F8CB4DE2103697D258F85A5C3BADFCF34857B563A197A19F1B25DBD`; 8-file payload in both `artifacts/test-builds` and
  `backup/test-builds`.
- Both installed on `emulator-5554` only (`adb install -r -d`); the installed base APK was pulled and its hash equals the
  archive. The G1 group ledger on the device was untouched (6,732 bytes, SHA-256
  `cd617c2fe252e006050ee8d760705fbbdc058187d10507d599e0905aac0a9997`; verifier: 9 calls, USD 0.07381520 settled, USD
  0.92618480 remaining, 0 pending).
- Instrumented: preflight class 5/5 (log `D:\P5E-private\w3-preflight-coordinator.log`), coordinator class 12/12.
- Fake CHAIN `d0369bc8-e1da-4309-a395-dc358dfd285a`: `STRUCTURAL_VALID 14/14`, 0 actual provider calls, 1,680 ledger
  entries, 0 pending (semantic FAIL 12 / PASS 2 is the no-edit fake control). Negative gate
  `addbab63-6411-4a7d-afc3-6a99e15520da` (`fx-a03`): `STRUCTURAL_VALID 0/1`, 0 actual calls, 2 entries, 0 pending.
  Logs `D:\P5E-private\w3-fake-chain.log`, `w3-negative.log`.
