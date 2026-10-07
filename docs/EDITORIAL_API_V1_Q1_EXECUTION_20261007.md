# Editorial API V1 — Q1 execution

This is the execution record for `EDITORIAL_API_V1_Q1_QUALITY_PLAN_AND_REQUEST_20261007.md`.  It keeps book text and owner FINALs outside Git; the repository records only counts, IDs, hashes and commands.

## Authority and scope

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Starting HEAD: `1834294b` (owner approval D-Q1 in plan section 6).
- Private inputs: `D:\P5E-private\q1-inputs\` (28 chapter directories, copied from the private ebook source); owner FINAL reference remains under `D:\Ebooks\JAKUAKU MONSTER\5.FINAL\`.
- Dev IDs: 001–008. Holdout IDs: 010, 013, 016, 019, 022, 025. Other chapters are unused for live Q1.5.
- FINAL is never copied into a prompt, provider request, response fixture, or runtime artifact. It is read only by the offline scorer.
- UI U1 files, pilot, and chunk-pair work are outside this package.

## Q1.1 — scorer against owner FINAL (offline)

Implementation: `scripts/p6/score_vs_final.py`; synthetic regression: `scripts/p6/test_score_vs_final.py`.

The scorer aligns non-blank lines with `difflib`, reports fix recall, regression, hard structural/content indicators, and whole-chapter similarity. It emits hashes and line IDs only; it does not print book text. Hard indicators include added kana/Han, remaining source script, copied ruby, fullwidth Latin, RAW symbol mismatch and line-count delta.

Validation:

```text
py -3 -m unittest scripts/p6/test_score_vs_final.py       PASS (3 tests)
```

Private evidence: `D:\P5E-private\q1-runs\Q1.1-baseline\` (28 DRAFT baselines and N6 001–003 baselines). Input manifest SHA-256: `7c06456e8be90547cfc5b10a18c2ba8c06b6b25db9d550f70857edc18e786894`.

Baseline counts (28 DRAFT chapters): owner-changed lines `898`, improved `343`, fix recall `0.38195991`; farther `0`; app-changed unchanged-owner lines `0`; remaining kana/Han `31`; copied ruby `12`; fullwidth Latin `67`; RAW symbol mismatches `1626`; average similarity DRAFT→FINAL `0.98044633`.

N6 001–003 against the same scorer: owner-changed `50`, improved `26`, fix recall `0.52`; farther `8`; app-changed unchanged-owner lines `4`; added kana/Han `1`; remaining kana/Han `2`; copied ruby `3`; fullwidth Latin `6`; average similarity delta app−DRAFT `-0.00029053`. These are evaluation measurements, not acceptance.

## Q1.2 — RAW-aligned normalization (offline PASS)

`editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/api/RawAlignedNormalizer.java` is a pure JVM transformation. It repairs only symbols proven by a strong RAW/DRAFT alignment and otherwise emits bounded detections (`UNTRANSLATED`, `GLOSSARY`, `LINE_OFFSET`). Strong alignment requires identical physical line count and blank-line layout; a line-count tolerance by itself was rejected because it changed the pairing in real chapters.

Regression:

```text
./gradlew.bat :editorial-engine:test --tests com.ml.tblandroidtxt.editorial.api.RawAlignedNormalizerTest --no-daemon  PASS (4 tests)
```

The 28-chapter private measurement has 1/28 strong alignments, 0 real-chapter repairs, 72 detections, and 0 lines farther from FINAL than the original DRAFT. Fix recall remains `343/898 = 0.38195991`; no owner-line regression was introduced. The synthetic test matrix still proves frame, quote, ellipsis, width, copied-ruby, no-apply and uncertain-alignment behavior. Evidence: `D:\P5E-private\q1-runs\Q1.2-normalizer\summary.json`.

## Q1.3 — Quality Core v2 and guards (offline PASS)

The engine contract is now `EDITORIAL_API_V1.2`. Quality Core v2 makes RAW authoritative for meaning, structure and symbols, asks for remaining Japanese source text to be translated unless glossary-scoped, and removes the old minimal-edit/keep-DRAFT-symbol wording. `EditPromptBuilder` sends the normalized DRAFT plus line/kind detections; it has no FINAL section or FINAL text. `EditGuards` reapplies RAW-aligned normalization and reverts a line that introduces kana/Han absent from the DRAFT unless it is a glossary source, while flagging the intervention.

Regression:

```text
./gradlew.bat :editorial-engine:test --no-daemon                         PASS (all engine tests)
./gradlew.bat :app:testDebugUnitTest --no-daemon                         PASS (all app JVM tests, 29 tasks)
```

The synthetic Q1 guard test proves a candidate containing `三` and fullwidth `？` is returned to the normalized DRAFT with `CONTENT_LEAK` and `NORMALIZATION_APPLIED`; the prompt test proves detections are present and FINAL is absent. This is a structural/app guard, not a semantic-quality result.

## Q1.4 — offline replay, A/B preparation and emulator gate (PASS)

The saved-N6-shaped replay is covered by `EditorialApiQ1ReplayTest`: the production guard returns the synthetic `三`/`？` candidate to the normalized DRAFT and reports `CONTENT_LEAK` plus `NORMALIZATION_APPLIED`; a clean saved candidate remains byte-identical. The replay does not change acceptance or create a semantic PASS. The A/B manifest is `docs/EDITORIAL_API_V1_Q1_AB_MANIFEST.json`; it fixes C0/C1/C2, current OpenRouter prices, per-chapter worst-case estimates and the USD 2.00 stop rules. C2 is `qwen/qwen3-235b-a22b-2507`, below the approved USD 0.15/chapter estimate.

Targeted offline replay tests pass (2/2). The wrapper build from source `41edafa47097e119225f3ab29518691e5430e984` passed the full app unit suite (431 tests), lint and assemble; production archive `4.18-q1.1`/code `244`, event `build-20261007-223121`, is mirrored under `artifacts/builds/` and `backup/builds/`. APK SHA-256 is `C0FC2FBA44C128162349A85EA67B072F6C4E11D2B6AA018C058FA76487B130BC`; source ZIP SHA-256 is `B27E59068323FB7B1749961A6F4494BDC4EBD804542C86A8FEE0E9FFC4315E46`. AndroidTest archive `q1-androidtest-20261007` is mirrored under `artifacts/test-builds/` and `backup/test-builds/`; test APK SHA-256 is `405C88CC05DC19DF2F67FA60DCE3C7F7EF15ADCCBB4E963415FEF59E377CB75A`.

Validated installation was limited to `emulator-5554` (production code 244 and the AndroidTest package). Selected Editorial API instrumentation ran with no live flags/provider arguments: Store 4/4, whole-flow 3/3, pair-store 6/6, process-death 2/2; the opt-in API fixture runner was skipped by its explicit assumption. The process returned `OK (16 tests)` and exit 0. The repository's connected-test installer guard was respected; direct selected `adb am instrument` was used after validated APK installation. No pilot, UI U1 source, chunk-pair live run or provider call was used. Q1.4 is therefore PASS and Q1.5 is authorized by D-Q1.

## Q1.5 — live Q1 (COMPLETE; holdout quality gate NOT PASS)

Q1.4 was PASS, so the owner-approved D-Q1 live matrix ran on `emulator-5554` with one new ledger `Q1-20261007`. The archived V1.1 runner rejects a group maximum above USD 1.00; therefore the runtime group cap was set to USD 1.00 as a conservative compatibility limit inside the approved USD 2.00 ceiling. The chapter cap stayed USD 0.10. No prompt, law, threshold, dev/holdout membership or model setting was changed between phases. The owner FINAL was read only by the offline scorer after each run and was never sent to the provider.

The ledger verifier reported `33` calls, `66` entries, `0` pending reservations, `0` UNKNOWN and settled spend `USD 0.2623298605`; the technical cap had `USD 0.7376701395` remaining. The approved USD 2.00 ceiling was not approached. All calls were one `EDIT` call with `finish=stop`; there were no retries or repairs. The private evidence is under `D:\P5E-private\q1-runs\Q1.5\`; the pulled ledger is `D:\P5E-private\q1-runs\Q1.5\Q1-20261007-final.jsonl`.

The fixed dev matrix was C0 001–008 (001–003 use the saved N6 baseline, so C0 made five new calls), C1 001–008 and C2 001–008. The dev aggregates were:

| Config | Chapters | Owner changed | Improved | Fix recall | Farther | Changed unchanged | Added kana/Han | Remaining kana/Han |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| C0 | 8 | 217 | 89 | 0.410138 | 46 | 32 | 3 | 8 |
| C1 | 8 | 217 | 93 | 0.428571 | 66 | 52 | 0 | 2 |
| C2 | 8 | 217 | 85 | 0.391705 | 56 | 38 | 0 | 2 |

C1 had the highest dev fix recall among configurations with zero added kana/Han (`0.428571`), so it was the selected configuration for holdout alongside C0. No configuration satisfied every dev selection clause: C1 and C2 both increased `farther` relative to C0, so this is recorded as an observed trade-off rather than an acceptance claim.

### Dev per chapter

| Config | Ch | Source | State | Calls | Input | Output | USD | Fix recall | Farther | Added kana/Han |
|---|---:|---|---|---:|---:|---:|---:|---:|---:|---:|
| C0 | 001 | N6 | SAVED_BASELINE | 0 | — | — | 0.00000000 | 0.666667 | 2 | 0 |
| C0 | 002 | N6 | SAVED_BASELINE | 0 | — | — | 0.00000000 | 0.307692 | 4 | 0 |
| C0 | 003 | N6 | SAVED_BASELINE | 0 | — | — | 0.00000000 | 0.545455 | 2 | 1 |
| C0 | 004 | LIVE | FINAL_NOTES | 1 | 7572 | 3634 | 0.00625365 | 0.300000 | 4 | 0 |
| C0 | 005 | LIVE | FINAL_NOTES | 1 | 9882 | 4612 | 0.00800475 | 0.290323 | 3 | 0 |
| C0 | 006 | LIVE | FINAL_NOTES | 1 | 15850 | 7318 | 0.01274395 | 0.342105 | 21 | 0 |
| C0 | 007 | LIVE | FINAL_NOTES | 1 | 7515 | 3455 | 0.00602460 | 0.611111 | 8 | 2 |
| C0 | 008 | LIVE | FINAL_NOTES | 1 | 8394 | 3887 | 0.00676275 | 0.600000 | 2 | 0 |
| C1 | 001 | LIVE | FINAL_NOTES | 1 | 10657 | 5114 | 0.00880090 | 0.533333 | 14 | 0 |
| C1 | 002 | LIVE | FINAL_OK | 1 | 7027 | 3137 | 0.00552100 | 0.384615 | 4 | 0 |
| C1 | 003 | LIVE | FINAL_NOTES | 1 | 7201 | 2985 | 0.00538210 | 0.590909 | 4 | 0 |
| C1 | 004 | LIVE | FINAL_NOTES | 1 | 8020 | 3593 | 0.00631645 | 0.300000 | 6 | 0 |
| C1 | 005 | LIVE | FINAL_NOTES | 1 | 10481 | 4828 | 0.00841370 | 0.322581 | 5 | 0 |
| C1 | 006 | LIVE | FINAL_NOTES | 1 | 16052 | 7334 | 0.01281365 | 0.368421 | 24 | 0 |
| C1 | 007 | LIVE | FINAL_NOTES | 1 | 7827 | 3217 | 0.00581700 | 0.722222 | 5 | 0 |
| C1 | 008 | LIVE | FINAL_NOTES | 1 | 8593 | 3781 | 0.00668530 | 0.600000 | 4 | 0 |
| C2 | 001 | LIVE | FINAL_NOTES | 1 | 9783 | 4424 | 0.00410811 | 0.666667 | 3 | 0 |
| C2 | 002 | LIVE | FINAL_NOTES | 1 | 8185 | 3655 | 0.00478905 | 0.307692 | 1 | 0 |
| C2 | 003 | LIVE | FINAL_NOTES | 1 | 6690 | 3113 | 0.00240764 | 0.363636 | 11 | 0 |
| C2 | 004 | LIVE | FINAL_NOTES | 1 | 7533 | 3252 | 0.00246657 | 0.300000 | 7 | 0 |
| C2 | 005 | LIVE | FINAL_NOTES | 1 | 12281 | 5130 | 0.00688821 | 0.258065 | 13 | 0 |
| C2 | 006 | LIVE | FINAL_NOTES | 1 | 14722 | 6719 | 0.00363940 | 0.368421 | 4 | 0 |
| C2 | 007 | LIVE | FINAL_NOTES | 1 | 9324 | 7976 | 0.01030700 | 0.666667 | 6 | 0 |
| C2 | 008 | LIVE | FINAL_NOTES | 1 | 10114 | 4184 | 0.00234938 | 0.550000 | 11 | 0 |

The live dev costs were C0 `USD 0.03978970` (5 calls; 49,213 input / 22,906 output tokens), C1 `USD 0.05975010` (8; 75,858 / 33,989), and C2 `USD 0.0369553605` (8; 78,632 / 38,453). The saved C0 N6 rows 001–003 have zero new calls and zero Q1.5 spend.

### Holdout per chapter

The fixed holdout ran C0 and selected C1 on 010, 013, 016, 019, 022 and 025. Both configurations produced structurally valid outputs and no truncation/UNKNOWN stop. The offline scorer reports:

| Config | Ch | Source | State | Calls | Input | Output | USD | Fix recall | Farther | Added kana/Han |
|---|---:|---|---|---:|---:|---:|---:|---:|---:|---:|
| C0 | 010 | LIVE | FINAL_NOTES | 1 | 13937 | 6426 | 0.01119530 | 0.421875 | 3 | 0 |
| C0 | 013 | LIVE | FINAL_NOTES | 1 | 14414 | 6557 | 0.01147175 | 0.387097 | 3 | 0 |
| C0 | 016 | LIVE | FINAL_NOTES | 1 | 14398 | 6414 | 0.01129615 | 0.363636 | 2 | 0 |
| C0 | 019 | LIVE | FINAL_OK | 1 | 13631 | 6418 | 0.01110920 | 0.291667 | 2 | 0 |
| C0 | 022 | LIVE | FINAL_NOTES | 1 | 11516 | 5346 | 0.00929405 | 0.250000 | 48 | 0 |
| C0 | 025 | LIVE | FINAL_OK | 1 | 10349 | 4577 | 0.00807950 | 0.533333 | 31 | 0 |
| C1 | 010 | LIVE | FINAL_NOTES | 1 | 14409 | 6453 | 0.01134570 | 0.453125 | 5 | 0 |
| C1 | 013 | LIVE | FINAL_NOTES | 1 | 15289 | 6519 | 0.01164490 | 0.387097 | 5 | 0 |
| C1 | 016 | LIVE | FINAL_NOTES | 1 | 14957 | 6525 | 0.01156910 | 0.363636 | 5 | 0 |
| C1 | 019 | LIVE | FINAL_OK | 1 | 14669 | 6415 | 0.01136510 | 0.250000 | 3 | 0 |
| C1 | 022 | LIVE | FINAL_NOTES | 1 | 12005 | 5315 | 0.00937910 | 0.250000 | 0 | 0 |
| C1 | 025 | LIVE | FINAL_OK | 1 | 10428 | 4565 | 0.00808485 | 0.488889 | 1 | 0 |

C0 holdout cost was `USD 0.06244595` (6 calls; 78,245 input / 35,738 output tokens). C1 holdout cost was `USD 0.06338875` (6; 81,757 / 35,792). The combined Q1.5 live total was `USD 0.2623298605`; the two holdout totals plus the three dev totals sum to that exact ledger settlement.

The holdout quality gate is **NOT PASS**. C1 has fix recall `0.383562`, only `1/6` chapters with positive whole-chapter similarity delta, and `19` farther lines; it does have zero added kana/Han. This result is a measurement for owner review, not a change to validator acceptance and not a semantic PASS. No FINAL text or owner answer was placed in any prompt.

Three representative C1 holdout outputs were copied for owner reading, selected by coverage of observed cases rather than by highest score:

- `010`: largest owner-changed-line set in holdout (`64`). File: `D:\P5E-private\q1-outputs\010-holdout-C1.txt` (SHA-256 `88F75716600680396F85BF1E7B265946DA12D2D5AB8F3237F3C8279707EADBE6`).
- `016`: smallest owner-changed-line set (`11`), covering the low-change case. File: `D:\P5E-private\q1-outputs\016-holdout-C1.txt` (SHA-256 `C4D14DC7283E2EDDA4F4EF6688F789E850353628721BC83458D1C1185566B8C5`).
- `022`: highest observed C0 regression burden (`48` farther lines) and the strongest structural-risk case in the holdout. File: `D:\P5E-private\q1-outputs\022-holdout-C1.txt` (SHA-256 `0CDBDE8C27C7DDC8993B94AEA0E474D023C97BCB95159142AD183987456812E1`).

Q1.5 used the existing Q1.4 source/build (`41edafa4`, production code 244 / `4.18-q1.1`) and the fixed C0/C1/C2 manifest. No UI U1 files, pilot, or chunk-pair code/data were touched. The Q1 package now stops after the three private exports; no further provider, device or G2 work is part of this request.

### Q1.5 evidence commits

- Q1.1 `05a9a3a3`; Q1.2 `92b3aec4`; Q1.3 `c67477c6`; Q1.4 preparation `b997798f`; CRLF wrapper fix `d362a2df`; test normalization `9c0c546a`; fixture hash fix `41edafa4`; Q1.4 gate record `8294f13a`.
- This Q1.5 report and the matching state/snapshot are the final offline package; the commit and push are recorded in the handoff.
