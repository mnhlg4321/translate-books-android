# Translation Profile Recovery v4.17

Status: `ACTIVE / D5_RELEASE`

This document is the single product and execution authority for the v4.17 translation-profile recovery. It replaces every active next action from the later RSC/Relation-Speaker, Editorial activation, IPC, CP6 and live-canary tracks. Those tracks and their evidence remain historical and are not deleted or reinterpreted.

## 1. Locked decision

- Restore the proven chapter-by-chapter translation workflow from `v4.16-dev.51` / code113.
- Source baseline: commit `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7`.
- Baseline APK: `TranslateBooks-v4.16-dev.51-code113.apk`, 2,786,334 bytes, SHA-256 `C271F9D8BE757C476300E85E2F7E99742CAF25BF26B119C6218953E93FE24376`.
- Freeze the later RSC/Relation-Speaker and Editorial activation track at the preserved code168 state. The latest preserved production-development APK remains `v4.16-dev.104` / code168, 3,331,375 bytes, SHA-256 `E235BB3640039E48DD1E1C70264A25158ED4B6E9146860A6730644790E585F35`.
- New development uses one branch only: `feature/v4.17-translation-profile-compatibility`.
- Every later work session resumes this branch and this plan. It must not create a new plan, branch or checklist unless the owner changes the release scope.
- The first new APK must use Android `versionCode` greater than 168. Code113 is the source/product baseline, not the number of the new build.

## 2. Authority order

1. `AGENTS.md` — repository safety and work-session rules.
2. `TRANSLATION_PROFILE_RECOVERY_V4_17.md` — product scope, order and acceptance criteria.
3. `BUILD_STATE.md` — exact current artifact/build facts.
4. `WORKSPACE_SNAPSHOT.md` — current phase and exact next action.
5. `release_checklists/v4.17-translation-profile-compatibility.md` — evidence for the single release lifecycle.
6. `README.md` — user/developer overview.
7. All RSC/Editorial/IPC/CP6 documents — immutable historical context only.

Historical documents cannot authorize work or block this track. If they conflict with this document, this document controls the v4.17 product scope.

## 3. Product workflow

The supported workflow is deliberately chapter-by-chapter:

```text
select one RAW chapter
  -> select the matching chapter glossary
  -> select the matching chapter pronoun profile
  -> preview the compact prompt context
  -> translate the chapter
  -> validate and save output
```

The user is responsible for selecting the matching chapter files. Automatic `CHxxx` validation and whole-volume profile dispatch are not required for v4.17.

## 4. Glossary runtime contract

Accepted CSV shapes:

```csv
source,target,category
source,target,category,note
source,target,category,note,priority
```

Runtime fields:

| Field | Parse/store | Runtime/model use |
|---|---|---|
| `source` | yes | match and terminology lock |
| `target` | yes | translated surface lock |
| `category` | yes | compact category label |
| `note` | yes | include only for a matched row when non-empty |
| `priority` | no | user-review metadata only; tolerate and ignore |

The app must never rank, filter or increase prompt cost from `priority`. It must not inject unmatched glossary notes.

## 5. Pronoun runtime contract

The new format is:

```csv
from,speaker,target,self,call,scope,note
```

| Field | Required behavior |
|---|---|
| `from` | source cue used to locate a rule |
| `speaker` | actual speaker identity |
| `target` | actual addressee/target identity |
| `self` | Vietnamese self-reference |
| `call` | Vietnamese address term |
| `scope` | constrain the rule to its paragraph/chunk range without semantic discovery |
| `note` | retain; inject only when needed for a matched rule |

The header must not become a rule. `target` must not be interpreted as pronoun text, and `self`/`call` must reach the compiled prompt. Scope support is a bounded range-selection feature, not a new RSC engine.

Legacy compatibility remains required:

```csv
from,to,pronoun
```

Legacy and seven-column formats use explicit, separate mappings. No generic schema registry, manifest or profile approval workflow is introduced.

## 6. Fixed scope

### In scope

- Characterize code113 behavior before modification.
- Parse, store, reload, match and compile Glossary runtime fields `source,target,category,note`.
- Safely ignore Glossary `priority`.
- Parse, store, reload, scope, match and compile all seven Pronoun fields.
- Preserve legacy three-column glossary/pronoun behavior where it does not conflict with the locked contracts.
- Update prompt preview and token estimation only as required by the new compact context.
- Test with real Mercedes chapter fixtures.
- Build, QA and archive one numbered v4.17 development artifact after regressions pass.

### Explicitly out of scope

- Automatic chapter/profile mapping or `CHxxx` validation.
- Whole-volume batch dispatch.
- RSC JSON, Relation-Speaker discovery, Pair/Ledger promotion or semantic speaker inference.
- Editorial project workflow, activation, certification or execution.
- Style approval, owner-decision artifacts, session receipts, state hash chains or mode dispatchers.
- IPC, CP6, AVD orchestration, real-provider canaries or live activation gates.
- Database migration unless a focused persistence test proves the existing profile storage cannot retain the new fields; any such need must first be solved with the smallest backward-compatible representation.
- UI redesign unrelated to selecting, previewing and using the two profiles.

## 7. One ordered implementation sequence

Only one phase may be `IN_PROGRESS`. Complete the sequence in order; do not open parallel release tracks.

### D0_DOCS — authority normalization

Result: `COMPLETE` on `2026-08-28`; cross-document sequence, schema, links, current-only state and zero application-source diff checks passed.

- Create this plan and one release checklist.
- Update repository workflow so one release branch spans all work sessions.
- Replace accumulated BUILD_STATE and WORKSPACE_SNAPSHOT histories with current-only facts; Git remains the history.
- Update README and freeze later tracks as historical.
- Run documentation consistency checks.

Exit: every active document names the same baseline, branch, scope, current phase and next action.

### D1_BASELINE — code113 characterization

Result: `COMPLETE` on `2026-08-28`; canonical source `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7`, no production source change.

- Inventory the exact parser, stores, settings snapshots, prompt builder, preview, estimator and tests.
- Run focused existing tests without product changes.
- Add only missing characterization tests that demonstrate current legacy three-column behavior, the real five-column Glossary projection and the real seven-column P3 misparse.
- Record the smallest production-file change set for D2/D3.

Exit: baseline test evidence exists and no RSC/Editorial code has been activated.

### D2_GLOSSARY4 — four runtime fields

Result: `COMPLETE` on `2026-08-29`; implementation/tests commit `353b6410c1c359c5fca0ebccaabfdcbb2e83a037` (`feat(glossary): preserve matched runtime notes`). Focused D2 `GlossaryFourFieldRuntimeTest` is `9/9 PASS`; the combined A+B+C+D run is `94 tests, 0 failures, 0 errors, 0 skipped`; production diff is exactly `GlossaryStore.java` and `PromptContextBuilder.java`; no APK, device, API, RSC, Editorial or IPC action was performed.

- Extend the glossary model/persistence/compiler with `note`.
- Accept three-, four- and five-column inputs.
- Ignore `priority` exactly as locked.
- Verify import count, matching, prompt preview, save/reload and compact cost behavior.

Exit: focused Glossary tests pass, including Mercedes projection fixtures.

### D3_PRONOUN7 — seven runtime fields

Result: `COMPLETE` on `2026-08-29`; implementation/tests commit `11e6f54c4d164ad1b7e163b852b264714bbecb75` (`feat(pronoun): support scoped seven-field profiles`). The canonical P3 fixture now parses `from,speaker,target,self,call,scope,note`, exact headers are skipped, scope is applied by in-memory paragraph overlap, and legacy three-column parsing remains compatible. Focused D3 is `17/17 PASS`; the P3 diagnostic is `1/1 PASS`; the final canonical JVM run is `111 tests, 0 failures, 0 errors, 0 skipped`. Production source changes are limited to `PromptContextBuilder.java`, `Chunk.java`, `Chunker.java`, `PromptPlan.java`, `PromptBuilder.java`, `PromptPreviewDialog.java`, `TranslationRepository.java` and `TranslatorService.java`; no PronounStore, database schema, MainActivity, TranslationEngine, RSC, Editorial, IPC, provider or build/version source changed. No APK/device/API action was performed.

- Implement separate legacy-three and new-seven-column parsing.
- Preserve all seven fields across import/save/reload.
- Apply `scope` by bounded paragraph/chunk range.
- Compile correct `speaker -> target`, `self` and `call`; conditionally include `note`.
- Verify header/BOM/CSV quoting and prevent the code113 wrong-column behavior.

Exit: focused Pronoun tests pass, including Mercedes P3 fixtures.

### D4_INTEGRATION — real prompt and cost

- Result: `COMPLETE` on `2026-08-29`; implementation/tests commit `11c726fd94598664979de1768cb04ed7b574dd10` (`fix(prompt): integrate scoped locks through refinement`). Hermetic Mercedes CH001/CH004 fixtures match all six required SHA-256 values; parser counts are 53/15/1 and 154/36/6 for paragraphs/Glossary/P3 rows; focused D4 is `11/11 PASS`, D2/D3/diagnostic+D4 is `39/39 PASS`, preserved D3 baseline is `111/111 PASS`, and final full JVM is `205 tests, 0 failures, 0 errors, 0 skipped`. Range-aware refinement now preserves scoped P3 through translation, refinement, estimator and retry; no APK, provider/API, device, RSC, Editorial or IPC action was performed.

- Verify Glossary4 and Pronoun7 together in the same CH001/CH004 prompt.
- Verify settings snapshot, restart/resume, retry and prompt preview parity.
- Verify compact semantic cost payload and chapter-level dry preparation without external workflow.

Exit: integrated fixtures and relevant full local regressions pass; D5 is now active.

### D5_RELEASE — build and QA

- Result: `D5_LOCAL_BUILD_QA_COMPLETE / V4.17_DEV_ARTIFACT_READY` on `2026-08-29`; the numbered local artifact is `4.17-dev.1` / code169 from `b0ab983bd4f9b017819af2bb0043bc57caad807a`, with matching immutable artifact/backup payloads and static QA recorded in `QA_REPORT_v4_17.md`.
- Full JVM regression is `205 tests, 0 failures, 0 errors, 0 skipped`; focused integration is `11/11 PASS`; Lint is `0 errors/53 warnings`; no device, provider/API, instrumentation, benchmark, RSC, Editorial or IPC action was run.
- Public tag, public release archive, physical-device QA and Complete gate remain pending; this is a development artifact, not `PUBLIC_RELEASE_COMPLETE`.
- Run the full required regression suite.
- Build only through `scripts/build-and-save.ps1`.
- Use a unique version name and Android versionCode greater than 168.
- Inspect the exact prompt and perform chapter translation QA with user-selected matching profiles.
- Preserve immutable artifact/backup payloads and update release evidence.

Exit: local D5 build and QA are complete; checklist steps 10-14 and the public Complete gate remain pending until their real evidence exists. A failed local test/build returns to `FAILED_REPAIRING` in the same branch and phase; it does not create another plan or release.

## 8. Work-session protocol

At the start of every later session:

1. Read `AGENTS.md`.
2. Read this document.
3. Read `BUILD_STATE.md`.
4. Read `WORKSPACE_SNAPSHOT.md`.
5. Verify branch, HEAD and status.
6. Resume exactly `current_phase` and `next_action`.

Do not recreate the branch or checklist on resume. Do not require `SESSION_ID`, `PROJECT_ID`, manifests, hash-chain receipts or owner approval for ordinary local edits/tests/builds already authorized by this release scope.

At handoff, report only:

```text
branch
HEAD
current phase
completed work
test/build evidence
exact next action
```

## 9. Failure and blocker policy

- `IN_PROGRESS`: ordinary implementation is continuing.
- `FAILED_REPAIRING`: a local test, compile, lint or build failed; diagnose, patch and rerun in the same phase.
- `DEFERRED`: explicitly out-of-scope work that does not prevent the current phase.
- `BLOCKED_EXTERNAL`: only a required external resource/authority is unavailable and no safe local alternative exists, or a destructive/out-of-scope product decision is required.
- `COMPLETE`: the declared phase/release criteria have actual evidence.

A historical RSC/IPC blocker cannot become a blocker for this track. A local failure must not create a new branch, version plan, review-stop document or retry authorization package.

## 10. Scope guard for implementation

Expected production files are limited initially to the existing Glossary/Pronoun import, storage, prompt compilation, settings snapshot, preview and estimation path. Tests and fixtures may be added around those files. If a proposed change touches RSC/Editorial activation, SQLite lineage/capability, IPC/AVD scripts or unrelated UI, reject it unless the owner explicitly changes this plan.

## 11. Release acceptance

- All active documents agree on baseline, branch, phase and next action.
- Glossary three/four/five-column imports work; runtime uses four fields and ignores `priority`.
- Pronoun legacy-three and new-seven-column imports work with correct semantics.
- CSV quoting, UTF-8 BOM, Japanese and Vietnamese survive import and persistence.
- Headers never become data rules.
- Matching sends only relevant compact rows/rules to the model.
- Mercedes fixtures prove `self` and `call` reach the prompt and scope prevents cross-range leakage.
- Existing code113 translation/resume behavior remains regressed.
- No RSC/Editorial/IPC activation is introduced.
- A numbered APK with versionCode greater than 168 is built and archived only after tests pass.
