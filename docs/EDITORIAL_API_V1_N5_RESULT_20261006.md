# Editorial API V1 — N5 A/B result (2026-10-06)

Owner approval (chat, 2026-10-06): "duyệt D-N4, chạy N5, giá 1$" plus the endpoint/account fingerprint. The first fingerprint did not match the key saved on the emulator and the runner stopped before any dispatch (0 calls, no ledger); the second matched. Executed exactly as proposed in the N1–N4 execution report: 24 runs, group `N5-20261006` (new ledger, cap USD 1.00), chapter cap USD 0.10, reservation basis USD 0.25 / 1.20 per M tokens, model `openai/gpt-5.6-luna`, route OpenAI, `emulator-5554` only, no pilot, no retry beyond the single technical retry (none was needed). Nothing in code, prompts, fixtures or the scorer was changed during the measurement.

Source: APK `4.18-api.4`/code 241 (SHA-256 `8AABFFA6628256BDECBDE8929E380D62D333EEF772DC5C7DE59FF66B488E87DA`, built from `42b40fb1`). Run metadata records source commit `37889b05` because the runner takes the repository HEAD when launched; `42b40fb1..37889b05` is documentation only.

## Money

41 provider calls (12 Nhanh, 29 Kỹ), **settled USD 0.26563504**, remaining USD 0.73436496, 0 pending/UNKNOWN reservations, 0 cost overruns, all finish reasons `stop`. Ledger pulled to `D:\P5E-private\n5-logs\N5-20261006.final.jsonl` (SHA-256 `DD7D9FC2451F534CA79E552CFA0B430AD9DB10385629B76E5C89C5EA5A8C4C67`), hash chain verified by `verify_spend_ledger.py`. The G1 ledger on the device is byte-identical to before. Estimate was ≈ USD 0.53; actual is about half.

## Runs (private evidence under `D:\P5E-private\p6-runs\<id>`)

| Group | Run id |
|---|---|
| A base (Nhanh, 8 fixtures) | `442ad6c1-404d-44b2-a5cc-303d8756c23a` |
| B base (Kỹ, 8 fixtures) | `820a09ae-db0f-4884-a970-87a94e5eb89f` |
| A rep1 / rep2 (a04, a11) | `42ac94cb-46ab-4620-892b-5b1d731dcd87` / `ea17ed65-4b6e-42cc-b969-3d530d00fdd2` |
| B rep1 / rep2 (a04, a11) | `ec0babff-9eed-419f-922b-630371ea4568` / `c1325bd8-0845-4dfe-badb-a5d26f0e0d54` |

All 24 runs: structural valid, `FINAL_OK` 16 / `FINAL_NOTES` 8, `verify_fixture_run.py` accepted every group. Prompts, responses and final texts stay outside Git (book text).

## Result per arm (scorer targets, 25 per arm)

| | Nhanh (E) | Kỹ (E+C+C2) |
|---|---:|---:|
| Seeded targets fixed (raw scorer count) | **19 / 25** | **19 / 25** (18 / 25 if the degenerate run below is not credited) |
| Calls / USD | 12 / 0.1117 | 29 / 0.1540 |
| Targets missed | MISSING_SENTENCE ×3, ADDRESS_PROFILE ×2 | MISSING_SENTENCE ×4, ADDRESS_PROFILE ×2 |
| Guard flags raised | none | 1 run (below) |
| Scorer machine verdict | 12/12 FAIL | 12/12 FAIL |

Per fixture, fixed/total: a03 1/1, a04 1/1 (all three a04 runs per arm), a05 0/1, a07 1/1, a08 1/1, a11 5/6, 4/6, 4/6 for Nhanh and 4/6, 4/6, 5/6 for Kỹ. The scorer's machine FAIL comes from `COLLATERAL_TOO_HIGH` (10 of 12 runs per arm, 3–20 changed lines outside the targets, 309 in the degenerate run below), `CLEAN_CONTROL_EDITED` on the control and ambiguous fixtures, and the misses above. Human adjudication is `PENDING`. Collateral edits are not necessarily errors: the G1 adjudication already found a real defect in the nominal control `fx-a02`.

## A defect found by the measurement (not fixed here)

Run B base, `fx-a04`: the edit call returned about one fifth of the chapter (37 of 192 lines, ending at a frame symbol) with a well-formed `<EDITED>` and `finish_reason=stop`. The guards fired correctly (`STRUCTURE_WARN` 192 → 37 lines, `REWRITE_WARN` 80 %, symbol and glossary warnings), the check reported one OMISSION without a fix, and the run ended `FINAL_NOTES` **with the shortened text as the final text**. The seeded target counts as "fixed" only because its line was deleted. As designed (plan 4.3: flags do not block) this is the intended behaviour, but it delivers a mostly deleted chapter; it is a product risk, not a measurement artefact. Nhanh never produced such a result in 12 runs, but it has the same exposure (it has no check at all). Suggested repair for a separate package: treat a structure delta above a hard bound as `RETRY_REQUIRED` and keep the draft.

## Decision gate (plan section 6, criteria not lowered)

| Gate | Status |
|---|---|
| Best arm fixes ≥ 15/25 | Both arms 19/25 raw — numerically met |
| 0 new MEANING/OMISSION/NUMBER/NEGATION errors on clean fixtures | **NOT_MEASURED** (needs independent adjudication of the changed lines on `fx-a02`/`fx-a12` and the collateral lines). Kỹ already shows one OMISSION-class failure (above) |
| 0 truncation | No `length` finish; but one content-loss event under `stop` (Kỹ) |
| B beats A by ≥ 4 targets or clearly fewer new errors | **No**: 19 vs 19 (18 vs 19 excluding the degenerate run); Kỹ costs ~38 % more and had the only structural failure |

Mechanical reading: the "new errors" gate cannot be declared met, so the default-mode choice is **not final**. If adjudication clears Nhanh, the gate's own rule picks **Nhanh** as default (equal recall, cheaper, no structural failure) and Kỹ stays an option. Neither arm shows the missing-sentence and address-profile recall the plan hopes for (those two classes account for all misses).

## Not proven / not done

- Independent adjudication of new errors and of the 8 + 4 collateral-edit runs: not done (book text, needs a reader). Three accepted FINAL chapters: **0/3**; N6 not started and not authorized.
- Semantic quality beyond the scorer's seeded targets is NOT_MEASURED; seeded targets are single-line synthetic defects on one chapter.
- Results come from one chapter family (fx-a01 derivatives); no holdout (`fx-h01/h02`) was run.
