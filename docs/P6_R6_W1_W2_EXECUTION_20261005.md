# P6 R6 — W1 and W2 execution (2026-10-05)

Scope: `docs/P6_R6_WIRE_SIMPLIFICATION_PROPOSAL_20261004.md` §12, W1 and W2. No provider call, no APK build, no
emulator or pilot access. W3 and W4 were **not** started because the W2 acceptance rule stops here.

## W1 — DRAFT/VI anchors derived from the quote (commit `21499cc0`)

- `EditorialL1Ledger.deriveDraftAnchor`: the model must still send `draftQuote`; the line carrying it is the anchor.
  - One line contains the quote: that line (a hint that already contains it is kept as hinted).
  - Several lines: the one coinciding with or nearest to the hint within 3 lines; a tie or nothing inside the window is
    `L1_DRAFT_QUOTE_AMBIGUOUS:findings.N.draftQuote`.
  - No line contains the quote (including a quote that is empty after NFC/trim/ruby removal): unchanged
    `L1_DRAFT_QUOTE_NOT_IN_ANCHOR`.
  - A multi-line hint keeps its length (`end - start`) from the quote line, clamped to the last line.
  - `start`/`end` outside the draft are clamped instead of rejected; type errors and the `MISSING` rules
    (`after` range, empty `draftQuote`) are unchanged. RAW anchors are unchanged. The app never creates or replaces quotes.
- L3 probes use the same derivation for `viStart/viEnd` from `viQuote` (`L3_PROBE_VI_QUOTE_AMBIGUOUS:<id>` is new).
- REPORT_L1 `normalizations` now also carries `draftAnchorDerivedFromQuote` (count) and `maxDraftAnchorDeviation`
  (lines); `parseBody` restores both. `EditorialContractRevision.CURRENT_LEDGER` is `L1_LEDGER_V8`.
- Tests are synthetic only (the real RAW fragment in `EditorialL1AnchorRegressionTest` was replaced). Engine 396/396,
  app unit and androidTest compile PASS.

## W2 — replay-all

Manifest `D:\P5E-private\p6-w2-replay-all-manifest-20261005.json` (SHA-256
`0E0FFC5EDF0559A71C38D91A26DB110A399B634B07C043FD4906BBF22E0095BD`), result
`D:\P5E-private\p6-w2-replay-all-20261005.json` (SHA-256 `D24B98EB26D1BFF621B4A63602E225FB97E52DC327F67573831E3424A92377BD`),
expecting the U6 RECONCILE response to PASS:

| Case | Expected | Actual |
|---|---|---|
| old RAW `7b6ac5b8…/001` | EXPECTED_REJECT `L1_UNIT_UNKNOWN:coverage.0.from` | as expected |
| current RAW `3559de99…/001` | PASS | PASS |
| U6 RECONCILE `…/002-L1_RECONCILE.json` (SHA-256 `18c5ddb3…ea126e3`) | PASS | **FAIL** — `L1_UNIT_LINE_NOT_A_UNIT:speakerRecords.6.unitId` |

Both findings now pass (`completed: findings.0, findings.1`): the DRAFT 99 vs 101 refusal is gone. The response is
still not valid, because `speakerRecords[6]` (`L126`) and `speakerRecords[7]` (`L140`) name **blank RAW lines**
(`L127` is the next real line and has no record; `L141` already has its own record, so the model emitted one record
per dialogue but mis-numbered two of them). Production stays fail-fast, so only the first is reported.

Exploration on a scratch copy (not evidence; it duplicates the `L141` record): moving just those two `unitId` values to the next non-blank line makes the
whole RECONCILE response PASS (protectedSpans, resolutions and post-finding invariants included). So the complete list of
remaining errors in the saved U6 response is exactly the two `speakerRecords.*.unitId` references above; nothing else is
hidden behind them.

Because this is a different family (speaker-record unit references on non-unit lines), not the DRAFT-anchor family W1
removes, the W2 rule applies: stop and report. The hash-bound expectation of the genuine rejection is recorded in
`D:\P5E-private\p6-w2b-replay-all-manifest-20261005.json` (SHA-256
`E5E67CECE28347938FF680648AEE62FC5AF553B1DC455CB9E770B956AD6109B5`; result
`…w2b-replay-all-20261005.json`, SHA-256 `365C2F88F9832F2E34B8556705E0B886E5C6DF2B6B521A43FE99F11E497E9A92`,
conclusion PASS with the rejection expected). That expectation documents the current behaviour; it does not make the
response valid and is not a reason to continue G1.

## Not done / decision needed

W3 (wrapper build, emulator) and W4 (live G1 from fx-a03) were not run. G1 ledger is unchanged: 9 settled calls,
USD 0.07381520 / 1.00. Proposed options for the owner (W5):

1. App derives/validates `speakerRecords[].unitId` the way W1 did for DRAFT: a record whose `L<n>` is a blank/excluded
   line is rejected with a precise path (current behaviour), or snapped to the nearest following unit whose text carries
   the record's quoted evidence — needs a quote field, which speaker records do not have today, so snapping would be a
   guess and is **not** recommended.
2. Treat `speakerRecords` as optional app-owned metadata (the engine derives speaker/listener downstream), removing the
   model-maintained references altogether.
3. Keep rejecting and accept the cost of one more live repetition (a third sibling of the same bookkeeping family).
