# P6 R0 — issue table (2026-10-02)

Evidence base: RAW/DRAFT/GLOSSARY/PRONOUN of chapter 001 (hashes in `docs/P6_R0_FIXTURE_MANIFEST.json`), the pilot FINAL `a7d5f99e…` (pilot VI_L2 = FINAL), the owner's manual FINAL `ebb091d6…` (independent reference, never a runtime input), the pinned PRONOUN profile, and the code at `4417d130`. Anchors are line numbers; quotes are single terms only (≤15 words per cell). Line numbers: RAW and DRAFT are numbered by `split('\n')` of the file; the DRAFT has 194 non-empty lines against 192 in the RAW because two RAW paragraphs are split in the DRAFT (I-005) and the manual FINAL (192) re-merges them; physical line numbers also drift because of blank lines, so every cross-file reference below is given per file and never assumed equal. Cross-check done by script (`scripts/p6/build_fixtures.py` reads the same anchors) rather than a sub-agent: the comparison is deterministic. Classification: `CONFIRMED_DEFECT` (RAW/profile proves it), `PROFILE_VIOLATION`, `PREFERENCE`, `UNCERTAIN`.

## A. Content issues in chapter 001

| ID | Source anchors | Class | Expected-meaning invariant | How it is scored | Covered by (fixture / later test) | Pilot FINAL |
|---|---|---|---|---|---|---|
| I-001 | RAW 237, DRAFT 237 (`今回` left in the line) | CONFIRMED_DEFECT, minor (untranslated token) | The temporal expression is rendered in Vietnamese ("this time"); no CJK character remains in a sentence that has a Vietnamese rendering | Machine: line has 0 CJK. Human: temporal sense kept | `fx-a01` target I-001; analogue `fx-a03` (other token), `fx-a11` | fixed |
| I-002 | RAW 105/147/175/215 ↔ DRAFT 107/148/176/216 (`踏破`), contrast in RAW 175 (`踏破するも攻略ならず`) and RAW 139–141 | CONFIRMED_DEFECT, major — one finding, four occurrences | `踏破` means reaching/traversing the dungeon to its deepest level; it must not be rendered as conquering (`攻略`). In RAW 175 the two achievements stay distinct, and RAW 141 (someone else conquered that dungeon) stays consistent with RAW 147 | Machine: the DRAFT's conquer-verb is absent from the four `踏破` lines and the two verbs differ in line 176. Human rubric: the contrast survives, no claim that the character conquered | `fx-a01` target I-002 (all four occurrences, counted as one finding with 4 occurrences) | not fixed (still the conquer-verb in 4 lines) |
| I-003 | RAW 319 ↔ DRAFT 321 (`嬢ちゃん`), PRONOUN row 3 (speaker `@MALE_1` → Mercedes, call "cô bé", scope p159–p160) | PROFILE_VIOLATION, minor | The vocative for `嬢ちゃん` in that utterance is "cô bé", as the profile pins for that scope | Machine: line contains "cô bé" and not the other vocative. Human: nothing else changed in the utterance | `fx-a01` target I-003; analogue `fx-a10` (a different profile row: `お前`/`bác`) | not fixed |
| I-004 | RAW 123 ↔ DRAFT 125 (`特権階級`) | PREFERENCE (UNCERTAIN: see U1) | Optional: the word 階級 ("class") is expressed. A condensed rendering is tolerated | None mandatory. Edit and preserve are both acceptable; never counted as a miss | `fx-a01` listed as uncertain; not a recall target | unchanged |
| I-005 | RAW 87 → DRAFT 87+89; RAW 305 → DRAFT 305+307 (one RAW paragraph split into two DRAFT paragraphs) | PREFERENCE / structure (UNCERTAIN: see U2) | No content change; it is a one-to-many RAW→DRAFT mapping the inventory must represent | Not a defect target. Mapping fixture for R1: inventory and anchors must survive 1→2 | `fx-a01` (mapping), R1 negative fixtures | unchanged |

Pilot FINAL against the owner's manual FINAL: the manual differs from the DRAFT at seven content lines (107, 125, 148, 176, 216, 237, 321) plus the two merges; the pilot fixed only 237. Not every manual edit is a required correction (I-004, I-005 are not).

## B. Tool defects found by the audit and re-checked on the code

| ID | Where (file:line at `4417d130`) | Class | Invariant to hold | Scoring / test that closes it | Group |
|---|---|---|---|---|---|
| F1 | `EditorialP5PilotExecution.java:869,899` — `findingCount` declared and passed, never assigned | CONFIRMED_DEFECT | `findingCount` equals the number of validated unique findings of the ledger; 0 only when the ledger is empty | JVM: fixtures with 0, 1, many findings and many occurrences of one finding | R2 |
| F2 | `EditorialP5PilotExecution.java:436,755` — `reportBytes` stores counts, gates and refs, not entries | CONFIRMED_DEFECT | serialize → DB → restart → `committedL1` returns an equivalent ledger (entries, anchors, proofs) | instrumented on the emulator; a fixture with more than four findings | R2 |
| F3 | `EditorialP5RawWireContract.java:31` (`MAX_FINDINGS = 4`, 3,584-byte response), one entry per `population:<chapter>` (`EditorialP5CExactBindingExecution.java:124,216,288,363`) | CONFIRMED_DEFECT | The wire carries an Error Ledger (anchors, expected meaning, evidence, disposition) of unbounded count within a measured cap, chunked when needed, never silently cut | negative fixtures that need more than four findings and more than 300 candidates | R1/R2 |
| F4 | `EditorialChapterFinalCoordinator.java:68,108,115` — protected set is `Set.of()` | CONFIRMED_DEFECT | Protected spans come from REPORT_L1, reach L2/L3 and are remapped DRAFT → VI_L2 → FINAL; a regression is detected | test through the coordinator entry point, not a hand-fed set | R3 |
| F5 | `EditorialL3Execution.java:529` — `finalReadOrder` is a fixed list | CONFIRMED_DEFECT | A final-read is an operation bound to the hash of the built text; a receipt with the marker but no operation is rejected | validator test + a QA edit that triggers a re-read of the built FINAL | R4 |
| F6 | `EditorialP5PilotRequest.java:14` — identity domain `…L1_ATTEMPT_IDENTITY_V1` carries no contract revision | CONFIRMED_DEFECT | A changed wire or report schema cannot return an old COMMITTED artifact as `ALREADY_COMMITTED` | JVM: same inputs, different `contractRevision` ⇒ different identity; v1 reports read as `LEGACY_CONTRACT_V1`, not eligible as v2 predecessor | R1 |
| F7 | `EditorialP5EFreshRawLiveRunner.java:165` — a used binding is refused (`ALREADY_USED`) | CONFIRMED_DEFECT of the plan, not of the code | Re-running chapter 001 under the new contract needs a new binding/run declaration (or a revision-aware lineage policy) | owner decision Q2 in R6; no code change before it | R6 |
| F8 | `QA_RECEIPT.json` of the pilot: 4 probes (2 coverage, 2 regression), all `NO_DEFECT`, mostly unanchored | CONFIRMED_DEFECT (weak evidence) | A probe states what was checked (RAW and VI_L2 anchors), the counter-evidence, a conclusion and an action; a generic `NO_DEFECT` is not accepted | validator rejects unanchored probes; L3-only fixtures with a defect planted in VI_L2 | R4 |
| F9 | `CHANGE_MAP_L2.rawDiscovery` (49 candidates) vs `QA_RECEIPT.ledgers` (257) | design decision | Coverage is judged on the app's RAW inventory and anchor mapping with resolutions, not on equal raw counts | R1 inventory + coverage checks both directions | R1 |
| F10 | consequence of F4 | not independent | `protectedSpanRegressions = 0` proves nothing until F4 is closed | closed with F4 | R3 |

The earlier statements that L3 recorded "8 probes" and that `findingCount = 0` showed L1 detected nothing were wrong (4 probes; the counter is unassigned). What can be said: L1 delivered no auditable ledger.

## C. Fixture map (private files, manifest in the repo)

| Fixture | Kind | Targets | Purpose |
|---|---|---|---|
| `fx-a01` | real | I-001, I-002 (×4), I-003; I-004/I-005 uncertain | reproduction of the chapter as it was translated |
| `fx-a02` | clean | none | control: the owner-corrected text used as DRAFT; nothing should be changed in meaning |
| `fx-a03`–`fx-a10` | seeded | one each: untranslated token, role swap, missing sentence, extra sentence, number, negation, glossary term, address/profile | single-error detection per class, L1-only/L2-only/L3-only derivations |
| `fx-a11` | seeded | six independent errors | more than four findings; ledger must not lose entries |
| `fx-a12` | ambiguous | none (three valid paraphrases; romaji monster names kept by design) | no sense-changing edit, no forced removal of untranslated names |
| `fx-h01`, `fx-h02` | holdout | three rule-seeded errors each on chapters 003 and 005; natural defects unknown | locked before any new model output (lock hash in the manifest) |

Class coverage of the seeded set: untranslated token, wrong role, missing sentence, extra sentence, number, negation, glossary term, address/profile. The ninth class from plan 11.5, protected-span loss, is not a text mutation: it is defined by a protected set plus a QA edit and is built by the R3/R5 harness from these bases.

## D. Questions that need knowledge outside the sources (asked once, at the end of R5)

- U1 — `特権階級` (I-004): is dropping 階級 ("class") an error to be fixed, or an acceptable condensation? Default used: preference, never scored.
- U2 — paragraph split (I-005): is splitting one RAW paragraph into two DRAFT paragraphs a defect? Default used: not a defect, but the mapping must be preserved.
- U3 — `踏破` wording: the sources fix the meaning (reach the deepest level, not conquer) but not the Vietnamese phrase. Default used: any phrase that keeps the two achievements distinct; the owner's manual phrase is one acceptable example.

R0 closes when every CONFIRMED item above has an invariant and a scoring rule (done) and the holdout is chosen and locked (done: `lockSha256` in the manifest; sites chosen by rule from chapters not proposed for R7).
