# P6 R6 W4 — G1 continuation on ledger `G1-20261003-ecbf8c55` (2026-10-05)

Scope: `docs/P6_R6_WIRE_SIMPLIFICATION_PROPOSAL_20261004.md` §13, W4 under D-G1c. Build `4.18-p6.22`/code235 (source
`e8a78edc`) on `emulator-5554` only; the owner-supplied fingerprint was passed as an instrumentation argument and is not
recorded. Mode `L1_ONLY`, live, run `2a2fe112-cae7-4414-b828-adc204e50d50`, planned base set `fx-a03, fx-a04, fx-a05,
fx-a07, fx-a08, fx-a11, fx-a02, fx-a12` with L1 retained for `fx-a04`, `fx-a11`, `fx-a02`. 0 automatic retry, 0 repair
call. **The group stopped after `fx-a03` on a new error code**; `fx-a04` … `fx-a12` and the two repeat rounds were not
started. No G2, no pilot.

## fx-a03

| Item | Result |
|---|---|
| `L1_RAW_DISCOVERY` | accepted by the production engine (replay conclusion PASS earlier for the same shape); response SHA-256 `5FBEA4777BE6A7EACCED6A1A4CE924017703A57DB741CEA59778293234BD1D91` |
| `L1_RECONCILE` | refused: **`L1_DRAFT_ANCHOR_UNUSED_FIELD:findings.0.draft.after`** — response SHA-256 `F8245DFA7FF694C39CD576058E423CDE520D345E63E951C44D6E0EF46B2EC89B` |
| STRUCTURAL_VALID | **0/1** (`P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`, `phase=L1_RECONCILE`) |
| SEMANTIC_EVAL | not evaluable (L1-only ends with the DRAFT; no REPORT_L1 committed) |
| Calls / tokens / USD | 2 calls (both `finish=stop`), 51,992 input / 5,640 output, USD 0.01976570; 0 UNKNOWN, 0 retry, 0 repair |

Response paths (outside Git): `D:\P5E-private\p6-runs\2a2fe112-cae7-4414-b828-adc204e50d50\results\fx-a03\responses\001-L1_RAW_DISCOVERY.json`
and `…\002-L1_RECONCILE.json`. Offline replay reproduces the refusal (`D:\P5E-private\w4-fx-a03-replay.txt`, SHA-256
`f7cfa08a02a9920c6de03875ea7dc4c8e29f9903547cbce0474d7139d6ba125c`).

Progress made by W1/W5: this RECONCILE got past the earlier families (empty fields, quote/ruby, duplicate references, DRAFT
line numbers, speaker records: 19 speaker records and 11 protected spans were parsed without error). The next
model-maintained bookkeeping mismatch surfaced instead.

## What the refusal is (diagnosed offline from the stored response)

All three findings (`E-L99-UNTRANSLATED`, `E-L375-CERTAINTY`, `E-L321-SENSE`, all OPEN, `kind=LINES`) carry the DRAFT
anchor `{kind: LINES, start: N, end: N, after: N}`: the model filled the unused `after` number with the same value as the
line. The strict response schema requires all four keys for both kinds ("unused values are 0"), and the parser rejects a
non-zero `after` on a `LINES` anchor (and a non-zero `start`/`end` on a `MISSING` anchor). The schema/prompt tell the model
only that unused values are 0; the model copied the line into the field that is not used. This is the same family as the
earlier ones (an unused or model-maintained number the app can derive), not a content error: the `start`/`end` values and
quotes themselves were accepted up to that check order.

Proposed fix for the coordinator (needs a decision under D-G1c because it is a new code; nothing was changed): for
`kind=LINES` ignore `after`, and for `kind=MISSING` ignore `start`/`end` (the app derives the anchor from `kind` plus the
quote, as W1 already does), count the ignored values in `normalizations` (e.g. `unusedAnchorFieldsIgnored` with paths) and
keep the `kind` enum, `MISSING` `after` range check and `draftQuote` rules strict; bump `contractRevision`; synthetic
regression plus an exact replay of the response above; then continue G1 from `fx-a03` within the remaining budget.

## Ledger

Group ledger after the run (verifier on `group-ledger-current.jsonl`, 8,228 bytes, SHA-256
`e82b0e8865d0ca3c74953075b5fb231ca2dcbcafa9e3ce2458f6fc22e9dfdf77`): **11 settled calls / 22 entries, USD 0.09358090 /
1.00, USD 0.90641910 remaining, 0 pending**, last entry hash
`302554233d1bfd1d08e65f4062907b4045447784470b18bb7c4b753f32ed8001`. Logs: `D:\P5E-private\w4-base.out.log`,
`w4-base.err.log`.
