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

## Q1.4 — offline replay, manifest, build and emulator (pending)

The saved N6 guard replay, A/B manifest, wrapper build and emulator-only regression will be recorded here. No provider call is allowed before this gate passes.

## Q1.5 — live Q1 (pending Q1.4 PASS)

Ledger: new `Q1-<date>`, cap USD 2.00; C0 current V1.1 luna, C1 V1.2 luna, C2 one Q1.4 candidate with worst-case estimate ≤ USD 0.15/chapter. Dev runs 001–008 (C0 skips 001–003 because N6 already exists), then fixed holdout 010/013/016/019/022/025 with selected best and C0. Prompt, law and thresholds stay byte-identical between dev and holdout. Stop on budget/reservation overflow, UNKNOWN cost, infrastructure error, or a required scope change. Three holdout outputs will be copied only to `D:\P5E-private\q1-outputs\`; owner FINAL is used solely for offline scoring and owner review.
