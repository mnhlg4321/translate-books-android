# Editorial API V1 — Q2 execution (2026-10-08)

Branch: `feature/v4.18-p5e-runner-repair-20260917`. This report is updated package by package. Private source books, FINAL files, provider responses and ledgers stay under `D:\P5E-private` and are not committed.

## Q2.1 — scorer, normalization and Quality Core

The fixed scorer from `ffe28df4` was rerun offline over Q1.5 (chapters 001–008, configurations C0/C1/C2) and the saved N6 baseline (001–003), plus the earlier Q1 holdout for comparison. It credits a changed owner line only when the app equals FINAL or improves similarity by at least 0.02; an untouched DRAFT is therefore not a fix. The private rerun is `D:\P5E-private\q1-runs\Q1.5\dev-summary.json` and `D:\P5E-private\q2-runs-q1-rescore.txt`.

| Configuration | owner changed | improved | farther | app changed owner-unchanged | added kana/Han | mean similarity delta |
|---|---:|---:|---:|---:|---:|---:|
| C0 dev + N6 | 217 | 37 | 46 | 32 | 3 | -0.00108833 |
| C1 dev + N6 | 217 | 15 | 66 | 52 | 0 | -0.00785508 |
| C2 dev + N6 | 217 | 9 | 56 | 38 | 0 | +0.00121560 |
| C0 prior holdout | 219 | 85 | 89 | 62 | 0 | -0.00120795 |
| C1 prior holdout | 219 | 84 | 19 | 12 | 0 | -0.00141075 |

`RawAlignedNormalizer` now records `PHYSICAL`, `NONBLANK`, or `NONE`. Equal nonblank counts enable deterministic pairing while ignoring blank-line placement. The 28-chapter private measurement (`D:\P5E-private\q2-runs\Q2.1-normalization\score-summary.json`) found 16/28 aligned chapters: 1 physical and 15 nonblank; 12 remained unaligned. The first safety probe applied 110 symbol repairs and produced 59 farther lines. Those repairs were disabled for `NONBLANK`; physical alignment retains the existing repair rules. The final pass has 0 farther lines, 0 app-changed owner-unchanged lines, and 0 added kana/Han. No source, DRAFT or FINAL was edited.

The Quality Core v2 role sentence no longer says “changing as little as possible”; it tells the editor to preserve already-correct lines and change only when RAW supports a clearly better result. The existing rule digest remains the rules-only pin; the role digest is recorded with each run as before.

Offline validation: `:editorial-engine:test` PASS (566 tests, including the nonblank alignment and disabled-repair regression). Q2.1 has no provider calls and no device operation.

## Q2.2

Pending.

## Q2.3

Pending.

## Q2.4

Pending; no live dispatch has occurred in this package yet.
