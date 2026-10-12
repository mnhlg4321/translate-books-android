# P6 R5 — evaluation protocol (frozen before any new model output)

Status: offline. No provider was called to produce or test anything in this document. The scorer
(`scripts/p6/score_run.py`), its frozen limits (`scripts/p6/thresholds.json`, SHA-256
`7db3d51495474bf12541bee65c7dee4dcb23fb0e05b0130002deb3bcb2a45597`) and 16 offline tests
(`scripts/p6/test_score_run.py`) exist; the labels and the holdout lock are the R0 ones
(`docs/P6_R0_FIXTURE_MANIFEST.json`, holdout lock `8872e28118242bab…`). Changing a limit after a run exists
invalidates that run.

## 1. Two verdicts, never merged

| Verdict | Meaning | Produced by |
|---|---|---|
| `STRUCTURAL_VALID` | the production engine accepted every committed artifact of the run (ledger parse, coverage, anchors, reconstruction replay, QA receipt validator, identities) | the harness that runs the engine; written to `<fixture>/structural.json` |
| `SEMANTIC_EVAL` | the final text fixes what the frozen labels say it must fix, damages nothing it should not touch, and a person confirmed the meaning where a machine cannot | `score_run.py` plus `human_scores.json` |

Every report carries both blocks side by side and the summary counts them separately. A run can be structurally
valid and semantically failed (the pilot chain was exactly that: `findingCount=0`, receipt valid, `踏破` still
wrong in three of four places). A known defect that is still in the final text is a semantic failure, always.
A structurally invalid run is reported as such and is not "repaired" by a good-looking text.

## 2. Run directory contract (written by the R6 harness, one folder per fixture)

```
final.txt          text the run ends with (FINAL for a chain, VI_L2 for an L2-only run, the DRAFT for an L1-only run)
structural.json    {"valid": bool, "reasonCode": str, "stage": "L1|L2|L3|CHAIN", "providerCalls": int, "stops": [str]}
human_scores.json  optional, see section 4
```

Fixtures, labels and run directories live outside Git (`D:\P5E-private\…`). The scorer refuses any path that goes
through `6.FINAL`, refuses to run when a model input contains a whole corrected sentence of a seeded or holdout
label (oracle-leak guard), and never reads a label from the run directory.

## 3. Machine invariants

`final.txt` is aligned to the fixture DRAFT with a line diff (`difflib`, no junk heuristic), so inserted, removed and
merged lines do not move a verdict. For every label target the state is one of:

| State | Rule |
|---|---|
| `RESTORED_EXACT` | the changed region contains the label's `old` line exactly |
| `FIXED_MATCH` | the region contains every `mustContain` and none of `mustNotContain` |
| `CHANGED_UNVERIFIED` | the line changed but matches neither the defect nor the fix: a person decides |
| `STILL_DEFECTIVE` | the line changed but still contains a `mustNotContain` string |
| `MISSED` | the line is unchanged (the known defect was preserved) |
| `PARTIAL` / `FIXED_MATCH` (rule target `I-002`) | how many of the four `踏破` lines changed (4 of 4 is only the machine part; a person judges the wording) |

Edits outside the targets are *collateral*; on the real chapter the lines of the uncertain items (I-004, I-005) and,
for the ambiguous control, the three labelled paraphrase lines are *ambiguous* and are reported but not graded.

Frozen limits (`thresholds.json`):

| Subject | Machine PASS requires |
|---|---|
| single seeded fixture | its target is `RESTORED_EXACT` or `FIXED_MATCH`; collateral ≤ 2 |
| six-defect fixture (`fx-a11`) | at least 5 of 6 targets fixed (0.83); collateral ≤ 2 |
| holdout (`fx-h01`, `fx-h02`) | judged per fixture at 0.83 on its 3 targets, i.e. all 3 fixed; collateral ≤ 2; evaluated once |
| clean control / ambiguous control | 0 semantic edits outside the labelled paraphrase lines |
| real chapter 001 (`fx-a01`) | I-001 and I-003 fixed, all four `踏破` lines changed (I-002), collateral ≤ 3; I-004/I-005 never scored |

Per-run verdict: `FAIL` if the machine part fails or the human part fails; `PENDING_HUMAN` if the machine part passes and a
person still has to read something; `PASS` otherwise. A person is required when any target is `CHANGED_UNVERIFIED` or
`PARTIAL`, for the real chapter, for any collateral or ambiguous edit, and for every rule target.

## 4. Human rubric

Scale per item: **0** wrong or unchanged, **1** changed but meaning or naturalness off, **2** correct and natural.
Items: every target the machine could not settle, `sampleOk` (all changed lines up to 40, read against the RAW: no new
error, no lost subject/listener/voice, numbers and symbols intact) and `voiceOk` (address terms agree with the PRONOUN
profile and the glossary). Human PASS needs every item ≥ 1 and mean ≥ 1.5. The `踏破` / `攻略` contrast is judged on the
meaning fixed by the sources (reaching the deepest level, not conquering it); any Vietnamese phrase that keeps the two
achievements distinct is acceptable (R0 U3). The record is `human_scores.json`:
`{"items": {"I-002": 2, …}, "sampleOk": 2, "voiceOk": 2}`; a missing item leaves the run `PENDING_HUMAN`.

## 5. What this does and does not establish

Established offline: the scorer separates structure from meaning, catches a preserved known defect, a partial fix of a
repeated defect (the pilot's one-of-four), collateral edits, an edited control, misaligned lines after inserts and
removals, a forged "perfect" structural result, and a leaked corrected sentence. The 16 tests fabricate runs by editing
the fixture DRAFT; they validate the scorer, they say nothing about any model.

Not established: whether the new prompts and contract make the model fix anything. That is measured only by the live
group of R6 under the owner's authority (see `docs/P6_R5_EXECUTION_BUDGET_TABLE.md`), scored by this unchanged
scorer, with the holdout run once. Natural defects that no label names are found only by the human sample; the
scorer cannot prove their absence.
