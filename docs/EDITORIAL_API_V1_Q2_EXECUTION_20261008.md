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

Implemented the offline `V5_CHAT` path. The provider sends exactly three OpenAI-compatible requests with one shared history: project instruction as system, L1 source turn, L2 user turn after the L1 assistant answer, and L3 after the L2 assistant answer. The final instruction is appended once to L3. Raw turn request/response captures are written by the runner outside Git; only `<FINAL>` from turn 3 is wrapped as the production `<EDITED>` answer. Reports are never parsed.

`length` at L1/L2/L3 stops the chain immediately with a typed `V5_TRUNCATED_L<n>` error. Missing/empty delimiters produce `V5_FINAL_MISSING`; V5 errors are terminal and do not trigger the generic edit retry. A dispatched transport failure is `V5_TURN_<n>_UNKNOWN` and is not resent. Usage and cost are summed across all physical turns. The ledger reserves an aggregate three-turn worst case for V5 while the stored API run retains one logical EDIT step and records physical calls separately.

Bundled pack: `v5-safe4-full-chatgpt/`; SHA-256: Project `1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD`, Prompt `D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F`, Workflow `5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730`. Runner accepts `V5_CHAT`, raises the group ceiling to USD 6.00 and chapter ceiling to USD 1.00, and passes model/reasoning overrides. The group-cap policy has a JVM regression at the approved USD 6.00 boundary.

Offline validation: `:editorial-engine:test` PASS (566 tests); `:app:test` PASS (77 test classes/tasks); AndroidTest Java compile PASS; Python fixture-verifier tests 14/14 PASS. No provider call, device operation or APK build occurred for Q2.2.

## Q2.3 — wrapper build, emulator gate and A/B manifest

The clean wrapper checkout was pinned to source `45976adbc6ecfb46210cb94d31e4c78861f337e1`, including the binary `.gitattributes` rule for the three FULL CHATGPT pack files. The production wrapper build passed offline with version `4.18-q2.1` / code `245`, event `build-20261008-064927`, APK SHA-256 `0EC548E725E72FA7099096E38D69E6E0C5C65E680EFCE876727BD7A17E0F01CC`, and source ZIP SHA-256 `0EDED56114F15FE9EFCFE98795E5587C62A9D1F8511046C4A37C32427C40BA60`. The AndroidTest wrapper archive passed with event `q2-androidtest-20261008`, test APK SHA-256 `1D9ADBE9013E01631D39A02E6147F4021AE8FC5EFD59501C4E4AB253D5B47B5C`, and source ZIP SHA-256 `CF8D1FE2B3E7E56DBE439269A5B8FE606740418A608033B1AA4B0E78657D7725`. Both payloads are mirrored in `artifacts/` and `backup/`; the wrapper installed only production and test packages on `emulator-5554`.

The on-device gate used the installed code `245` and no provider arguments: `EditorialApiStoreInstrumentedTest` 4/4, `EditorialApiBienTapFlowInstrumentedTest` 3/3, the normal Editorial API UI path 3/3 with its two phase-assumption cases intentionally skipped, and the explicit process-death seed → force-stop → verify → cleanup sequence 3/3. Engine/app/full build checks and AndroidTest compilation were already part of the wrapper gate. No provider call, pilot action, U1 file, or chunk-pair test was run. Logs are retained privately under `D:\P5E-private\q2-runs\Q2.3-emulator\`.

`docs/EDITORIAL_API_V1_Q2_AB_MANIFEST.json` records the exact build, contract `EDITORIAL_API_V1.3`, FULL CHATGPT pack hashes, model IDs, reasoning setting and current OpenRouter prices. GPT-5.6 Sol is pinned as `openai/gpt-5.6-sol` with reasoning `medium`, matching the owner's FINAL process. The public price basis captured on 2026-10-08 is USD 2.00/M input, USD 10.00/M output, USD 0.20/M cache-read and USD 2.50/M cache-write; the estimate assumes no cache discount. The provider's actual reservation calculation gives every planned chapter below the USD 1.00 chapter ceiling. Running both dev arms and then V5-strong on all three holdouts has a worst-case reservation of USD 5.553072, below the USD 6.00 group ceiling.

Q2.3: PASS. Q2.4 is authorized by D-Q2 and the measured reservation is within both caps; no live Q2.4 call has been sent yet.

## Q2.4

The new ledger `Q2-20261008` ran the complete dev matrix on `emulator-5554`: five E-strong logical calls and five V5-strong logical calls. E-strong used one physical call per chapter; V5-strong used exactly three physical turns per chapter. The ledger has 10 settled logical reservations (20 ledger entries), representing 20 physical provider calls, with 0 pending reservations, 0 UNKNOWN and 0 overrun entries; settled spend is `USD 0.8375159`, leaving `USD 5.1624841` of the USD 6.00 cap. No retry was sent after a V5 stop.

The fixed scorer ran offline against the private FINAL files; the detailed private report is `D:\P5E-private\q2-runs\Q2.4\dev-score-summary.json`. `E-luna`'s comparable Q1 baseline for 004–008 had 37 app-changed owner-unchanged lines and zero added kana/Han. The dev results are:

| Arm | Ch | State / structural | Owner changed | Improved | Near-exact | Farther | App changed unchanged | Added kana/Han | Δ similarity | USD | physical calls |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| E-strong | 004 | FINAL_NOTES / valid | 60 | 33 | 30 | 39 | 19 | 0 | -0.03185791 | 0.0721335 | 1 |
| E-strong | 005 | FINAL_NOTES / valid | 31 | 13 | 9 | 30 | 26 | 0 | -0.03209830 | 0.0872760 | 1 |
| E-strong | 006 | FINAL_NOTES / valid | 38 | 12 | 9 | 70 | 57 | 0 | -0.02142038 | 0.1339335 | 1 |
| E-strong | 007 | FINAL_NOTES / valid | 18 | 13 | 12 | 28 | 25 | 0 | -0.02428989 | 0.0740010 | 1 |
| E-strong | 008 | FINAL_NOTES / valid | 20 | 13 | 12 | 45 | 38 | 2 | -0.07437167 | 0.0759960 | 1 |
| V5-strong | 004 | RETRY_REQUIRED / invalid (`V5_FINAL_MISSING`) | 60 | 0 | 0 | 0 | 0 | 0 | 0.00000000 | 0.0751931 | 3 |
| V5-strong | 005 | RETRY_REQUIRED / invalid (`V5_FINAL_MISSING`) | 31 | 0 | 0 | 0 | 0 | 0 | 0.00000000 | 0.0777438 | 3 |
| V5-strong | 006 | RETRY_REQUIRED / invalid (`V5_FINAL_MISSING`) | 38 | 0 | 0 | 0 | 0 | 0 | 0.00000000 | 0.1007846 | 3 |
| V5-strong | 007 | RETRY_REQUIRED / invalid (`V5_FINAL_MISSING`) | 18 | 0 | 0 | 0 | 0 | 0 | 0.00000000 | 0.0675287 | 3 |
| V5-strong | 008 | RETRY_REQUIRED / invalid (`V5_FINAL_MISSING`) | 20 | 0 | 0 | 0 | 0 | 0 | 0.00000000 | 0.0729257 | 3 |

E-strong aggregates to owner-changed `167`, improved `84`, near-exact `72`, farther `212`, app-changed owner-unchanged `165`, added kana/Han `2`, and mean similarity delta `-0.03680763`. V5-strong has no structurally valid candidate: every third turn returned a stop receipt without the required `<FINAL>` delimiters, so the production run correctly kept DRAFT and recorded `V5_FINAL_MISSING`. These are measurements, not a reason to relax extraction or acceptance.

No arm satisfies the frozen selection rule: E-strong exceeds the E-luna unchanged-owner baseline and has added kana/Han; V5-strong is invalid in all five chapters. Therefore holdout 011/014/017 was not dispatched, and no files were written to `D:\P5E-private\q2-outputs\`. This is a deliberate stop before holdout, with no prompt, law, threshold or membership change and no U1, pilot or chunk-pair action.

## Q2.5 — Luna-only follow-up (owner section 7)

The owner revised the decision for this package: only `openai/gpt-5.6-luna`, reasoning `medium`; total Q2 ceiling USD 10.00. Keep the existing `Q2-20261008` ledger and its stricter stored USD 6.00 cap. Before any Q2.5 dispatch, read the current settled/pending ledger from `emulator-5554`; the last available report is USD 0.8375159 settled, 10 logical reservations, 0 pending, 0 UNKNOWN. Owner FINAL remains scoring-only.

### Q2.5.1 — original V5 input pack (offline; commit `c69eb0f8`, pushed)

Implemented the V5 full-chat source contract. Each request now carries the exact original `RAW.txt`, `DRAFT.txt`, `GLOSSARY.csv`, and `PRONOUN.csv` names and text; V5 preflight requires exactly those four files, strict valid text, the pack's five-column glossary header, and the seven-column or supported legacy pronoun format. It verifies RAW/DRAFT match the original attachments, and executes before run persistence/provider dispatch. Original source attachments persist with V5 run state and survive rehydration. Prompt assembly sends Project Instruction only as the system message once, then the first-chat prompt section, Workflow, and each original file in a named block; it does not add the E Quality Core, filtered glossary, or app detections. V5 stops immediately on `stop_class`, maps `length` to a typed truncation code, reserves at least 32,768 output tokens per turn, and extracts only `<FINAL>` or the named FINAL_QA output file. Migration v28 preserves existing API runs while adding V5 contract data.

Private-source pack preflight passed for the eight required live chapters (007; 004–008; 011, 014, 017), reporting only file roles, sizes, and hashes outside Git. Synthetic regressions cover attachment ordering/pack headers, bad or missing files, exact one-time system instruction, FINAL_QA extraction, first-turn stop, max output token floor, and SQLite migration/restart. No private book text, response, ledger, account value, or source hash is included in Git.

Validation: `:editorial-engine:test` 572/572 PASS; app benchmark/debug/release unit test tasks each 439/439 PASS; `scripts/p6` 80/80 PASS; source pack preflight 8/8 PASS; `:app:compileDebugAndroidTestJavaWithJavac` PASS. Emulator/device validation remains pending. Q2.5 calls: 0; build/device actions for Q2.5: 0. Commit `c69eb0f8` pushed to the existing branch.

### Q2.5.2 — reduce unnecessary rewrites and preserve status-label capitalization (offline; commit `abca34cd`, pushed)

Quality Core now asks the model to leave already-correct phrasing alone, repair only an identifiable issue supported by RAW/glossary/pronouns, preserve DRAFT/glossary term capitalization, and keep DRAFT forms of address absent contrary evidence. A deterministic guard restores case-only changes inside `【…】` labels only when the corresponding labels remain on the same aligned line; it does not restore semantic changes, unmatched labels, or moved lines. Contract revision increased from `EDITORIAL_API_V1.3` to `EDITORIAL_API_V1.4`; acceptance thresholds are unchanged.

Full forced regressions against the edited source: engine 572/572 PASS; app benchmark/debug/release unit test tasks each 439/439 PASS; all `scripts/p6` tests 80/80 PASS; AndroidTest Java compile PASS. No provider call, build, or device operation occurred for Q2.5.2. Commit `abca34cd` is pushed.

### Q2.5.3 — wrapper build, emulator checks, and cost estimate (offline)

Source `abca34cd00bdbcf8989cd2370bacc2862806dd29`, contract `EDITORIAL_API_V1.4`. The production wrapper built `4.18-q2.2`/code246, event `build-20261008-181534`, APK SHA-256 `C3D32924F91EE3C1B2F8A460D5B8AF76D8AC0D7FB7962E2A39CB7450F6DF31E4`, source ZIP SHA-256 `FBC2B3A2F16A8B8C4D9B35EBF955DA09035115475843B5B6D619D4142F55BFB2`; wrapper unit/lint gates passed and installed only on `emulator-5554`. The AndroidTest wrapper archive event `q25-editorial-api-20261008` has test APK SHA-256 `FDF1A5E930231AB61D7CD7269F304A41682C19BC4A8AC10661F980F7F74EA882` and source ZIP SHA-256 `858A48603991EBD08536E8891480339E7EC53A5FCE25A4493B66BB44F2F8FC4D`; its archive is mirrored in `artifacts/` and `backup/`. The test APK manifest uses `versionCode=0`, so after checking its exact hash, package and signer it was installed only on `emulator-5554`; the app stayed at code246.

Focused on-device Editorial API tests passed: store/migration 6/6, whole-flow 3/3, UI form/stored result 2/2, process-death seed → force-stop → verify → cleanup 3/3 (14/14 total). All used fake providers; no fixture live argument, Q2 provider call, pair test, pilot operation, or source-book content was used. Logs are private at `D:\P5E-private\q2-runs\Q2.5\q2.5.3-emulator-20261008\`.

One first attempt to invoke the instrumentation filter passed a PowerShell automatic-argument name incorrectly and started the broad 248-test APK suite. It hit the unrelated historical P4 assertion `expected 27, actual 28`; the run was force-stopped as it entered test 63 (`EditorialP5CExactBindingFakeE2EInstrumentedTest`). The log contains no `EditorialPair` class before the stop. No live instrumentation argument was supplied, the Q2 ledger hash remained unchanged, and the focused commands above then passed. This broad attempt is a recorded harness mistake, not a Q2.5 quality or provider result.

Offline V5 pack preflight passed for all eight requested chapters. Luna standard pricing was checked at OpenRouter: USD 0.20/M input, 1.20/M output, 0.02/M cache read, 0.25/M cache write. Cost reservation used the app's character/3 token proxy on the actual four source files and complete pack prompts, the full 32,768 output-token ceiling on all three V5 turns, and the higher USD 0.25/M input rate without cache-read discounts. Upper-bound Q2.5 estimate is USD 1.35386495 if V5-luna is selected for holdout (dev both arms + V5 holdout); USD 0.93618235 if E-luna-b is selected for holdout. Highest V5 chapter reserve is USD 0.16307480, below its USD 1.50 cap; E's highest estimated chapter is below USD 0.10. Current ledger was read before and after emulator tests: 10 settled calls, USD 0.8375159, pending 0, unchanged last-entry hash, USD 5.1624841 left under its stricter existing USD 6.00 cap. Combined with the maximum Q2.5 estimate, total is USD 2.19138085, below both this ledger cap and D-Q2b's USD 10.00 approval. No Q2.5 provider call has been sent.

The build/price/token matrix is recorded under `q2_5` in `docs/EDITORIAL_API_V1_Q2_AB_MANIFEST.json`.

### Q2.5.4 — V5-luna canary stopped at the first turn

The zero-call account check returned MATCH. The V5 input-pack preflight for 007 passed: exactly four source-file blocks (RAW, DRAFT, GLOSSARY, PRONOUN) and one Project Instruction were present in the recorded first request. Immediately before dispatch, the unchanged `Q2-20261008` ledger had 10 settled calls, USD 0.8375159 settled, 0 pending/UNKNOWN, and USD 5.1624841 remaining under its existing USD 6.00 cap. The chapter reserve estimate was USD 0.16307480; the app chapter cap remained USD 1.50.

Canary run `73771fb2-cb0f-4937-9e4f-1dbdacd0064c` used `V5-luna`, `openai/gpt-5.6-luna`, medium reasoning, source/build `abca34cd`, and contract `EDITORIAL_API_V1.4`. Turn 1 returned finish reason `stop` and the explicit model stop code `V5_STOP_INPUT_ARTIFACT_MISSING`; the app state is `RETRY_REQUIRED`, structural validity is false, and the companion status is `P6_API_V1_RETRY_REQUIRED`. This is the first request's private response: `D:\P5E-private\q2-runs\Q2.5\V5-luna-007-73771fb2-cb0f-4937-9e4f-1dbdacd0064c\results\fx-a01\v5-chat\01-response.txt` (SHA-256 `DB1784E57347D857610F26AF6725203D4B6DE374CDB936B88DC1AF74652E0036`). The response body itself remains outside Git. The app retained the original DRAFT as output; it did not create a valid FINAL.

Actual cost: one provider call, 18,145 input tokens, 2,924 output tokens, USD 0.0080449; finish reason `stop`, 0 UNKNOWN cost calls, 0 overruns, and no retry. The settled ledger now has 11 calls / 22 entries, USD 0.8455608 settled, 0 pending/UNKNOWN, and USD 5.1544392 left under its stricter stored USD 6.00 cap. The account check and source-pack preflight made zero calls.

Per the stop rule, Q2.5.4 stopped here. No second/third V5 turn, no E-luna-b or remaining dev chapter, no holdout, and no holdout TXT/reading page were produced. Quality remains NOT_MEASURED; the canary stop does not demonstrate translation quality. No prompt, law, threshold, or acceptance rule was changed. U1, pilot, and chunk-pair were untouched.

The offline implementation and build/test packages remain pushed. This canary stop is the end of the authorized Q2.5.4 run; do not resume the dev or holdout matrix from this run.


## Q2.6 — V5 source identity, original file names and the pack-condition table (owner D-Q2c)

Model `openai/gpt-5.6-luna`, reasoning `medium`, contract `EDITORIAL_API_V1.4`, branch unchanged. D-Q2c was approved in chat ("duyệt D-Q2c"; recorded in section 5 of `docs/EDITORIAL_API_V1_Q25_REVIEW_AND_Q26_REQUEST_20261009.md`, commit `4d5633ea`). U1, pilot and chunk-pair are untouched. Owner FINAL files are used only for scoring.

### Q2.6.1 — what changed (offline)

The first V5 canary stopped with `INPUT_ARTIFACT_MISSING` because the request sent placeholder names (`RAW.txt`, ...) and no chain identity, while the pack derives the chapter `ID` and `SERIES` from the owner's file names and names its outputs `[ID]_FINAL_QA_[SERIES].txt`. This is the second failure of one family ("V5 input is not what ChatGPT receives"), so the whole family was reviewed against the pack text, not just this symptom.

- `EditInputs.OriginalSourceFile` is now `(role, name, content)`; `role` is RAW, DRAFT, GLOSSARY or PRONOUN and is separate from the name.
- `V5SourcePackPreflight` judges by role (exactly one file per role, any order). The name only has to be the owner's real file name: non-blank, no path or control characters, and not one of the four placeholders (`V5_SOURCE_NAME_INVALID`). Schema checks are unchanged: strict UTF-8, glossary header with five columns, pronoun with seven columns or the supported legacy form.
- `V5SourceIdentity(chainId, series)` is new and is part of `EditInputs` and `ApiPrompt`. Missing either part stops before any provider call with `V5_IDENTITY_MISSING`; a malformed value is `V5_IDENTITY_INVALID`. The file-based runner derives it from the four original names (`NNN_<series words>`, removing each file's role words such as `RAW`, `DRAFT`, `translated`, `chapter`, `glossary`, `PRONOUN`); names that do not follow the pattern give `V5_IDENTITY_MISSING`, names that disagree give `V5_IDENTITY_MISMATCH`. A library (U1, later) passes the identity explicitly.
- `V5HostSourceManifest` builds the HOST SOURCE MANIFEST that now opens turn 1: `ID`, `SERIES`, `VERSION=V5-SAFE.4.1.3-FULL`, `EXECUTION_MODE`, the counting rules, and one line per file in role order with role, original name, bytes, characters (code points), lines, non-blank lines, first and last non-blank anchors (at most 40 code points, escaped), SHA-256 of the exact bytes (host-computed) and `bytes_readable=yes`. After the manifest come the unchanged first-chat prompt section, the Workflow, and the four `=== FILE: <original name> ===` blocks in role order. Project Instruction is still the system message exactly once.
- The stored V5 snapshot now keeps role, original name and identity (`{"chainId","series","files":[...]}`); the older bare array (name and content only) is still read, its roles are recovered from the placeholder names and its identity is empty, so such a run is refused as V5 input instead of being sent.
- The provider returns typed `V5_*` source codes for these checks (no request is sent); the run service raises the same codes in `prepare`, before the run is stored.
- Runner (`EditorialApiV1FixtureRunnerInstrumentedTest`) reads a `roles` map from the runtime manifest, derives and checks the identity, and records `v5Id`, `v5Series`, `v5Version`, per-file role/name/bytes/chars/lines/SHA-256 and the SHA-256 of the whole manifest block (the anchors are book text and are not written to metadata).
- Private input folder `D:\P5E-private\q2-inputs\<chapter>\` holds the owner's original files under their real names, copied byte for byte from `D:\Ebooks\JAKUAKU MONSTER\` and checked by SHA-256 against the Q1-locked `q1-inputs` bytes (all 32 files equal; nothing under `D:\Ebooks` or `q1-inputs` was changed). `roles.json` maps role to name; `q2-inputs-manifest.json` records name, bytes and SHA-256. The E arm keeps its Q1 payload and placeholder names. Runner script: `D:\P5E-private\run_q26_api.ps1` (outside Git).
- `scripts/p6/v5_pack_preflight.py` is an independent Python implementation of the same checks and counts; it checks roles, original names, identity and schemas for all eight live chapters (004, 005, 006, 007, 008, 011, 014, 017: PASS, pronoun 7 columns, glossary 5 columns).

### Pack-condition table (checked before any live call)

Source of conditions: Workflow "L1 SOURCE PREFLIGHT" (lines 28-37), the artefact and manifest contract (lines 9-26), golden replay G13-G24 (lines 296-308), the reason-code vocabulary (lines 334-364), and the three-turn prompt. "Provided" means the app supplies what the condition needs and a test covers it.

| # | Condition in the pack | What the app provides | Test | Status |
|---|---|---|---|---|
| 1 | Normal L1 needs RAW, DRAFT, GLOSSARY and PRONOUN; inventory the exact files (L1 steps 1-2; G21; `INPUT_*_FILE_MISSING`) | Four roles, each exactly once, judged by role; unknown, duplicate or missing role stops with `V5_SOURCE_FILE_SET_INVALID` in the run service, the provider and the runner | `V5SourcePackPreflightTest.refusesMissingDuplicateEmptyOrUnknownRoleFiles`, `filesAreJudgedByRoleNotByNameOrOrder`; `EditorialApiRunServiceTest.v5RequiresAndPersistsTheExactValidatedFourSourceAttachments` | Provided |
| 2 | Byte access is confirmed per source; a handle or name is not proof (L1 step 3; G22; `RETRY_SOURCE_BYTES_UNAVAILABLE`) | The file text is inline in the request, read by the host with strict UTF-8; every manifest line carries host-counted `bytes` and `bytes_readable=yes` | `V5HostSourceManifestTest.entriesAreInRoleOrderWithBytesCharsLinesAndHashesOfTheExactContent`; `V5ChatEditorialApiProviderTest.turnOneOpensWithTheHostManifestThenThePromptAndTheFourBlocksWithOriginalNames` | Provided (inline text, not binary attachments) |
| 3 | Parse and encoding checked (L1 step 4; `INPUT_SOURCE_FORMAT_INVALID`) | Strict UTF-8 without replacement, BOM and CRLF kept; blank or invalid text stops with `V5_SOURCE_EMPTY_OR_ENCODING_INVALID` / `V5_SOURCE_UTF8_INVALID` before sending | `strictUtf8DecoderRejectsMalformedBytes`; Python `test_rejects_missing_empty_bad_utf8_and_wrong_headers` | Provided (the host stops first, so the model's own code is never needed) |
| 4 | Glossary exactly five columns `source,target,category,note,priority` (L1 step 4; G23; `INPUT_GLOSSARY_SCHEMA_INVALID`) | Header and every row checked on the owner's original file, not a filtered table | `enforcesFiveColumnGlossaryAndSevenColumnOrLegacyPronoun`; Python glossary cases | Provided |
| 5 | Pronoun exactly seven columns, FINAL PRONOUN-CSV/3.1; legacy or generic is not authoritative (L1 step 4; `INPUT_PRONOUN_SCHEMA_INVALID` / `INPUT_PRONOUN_NOT_AUTHORITATIVE`) | Seven-column header and rows are checked. The host also accepts the legacy three-column form (kept from Q2.5.1: "schema as it exists"); the pack would call such a file non-authoritative, so for a legacy file the model may stop | same test; all eight live chapters have seven columns, checked by the Python preflight | Provided for the eight chapters; documented gap for legacy files (not applicable here) |
| 6 | Status inference: valid glossary and exactly one valid pronoun file imply `AVAILABLE` (G13, G15, G16; lines 17-19) | The host declares no status and supplies exactly one pronoun file, so the pack's own inference applies; the host never invents `NONE` | prompt contents in `turnOneOpensWith...` (no status line, one file per role) | Provided by design |
| 7 | Source Manifest: ID, series, version, file names, character and line counts, first and last anchors, SHA-256 if the host provides it; the model does not hash (line 15; L1 step 5) | HOST SOURCE MANIFEST opens turn 1 with all of these, hashes computed by the host over the exact bytes | `V5HostSourceManifestTest` (counts, anchors, escaping, redacted form, empty and single-line files); provider test compares the sent block with the host rendering and recomputes each SHA-256 | Provided in turn 1; turns 2 and 3 rely on the shared history and do not repeat the block |
| 8 | Identity mismatch is a typed stop (`INPUT_IDENTITY_MISMATCH`) and the chain is pinned to ID and series (lines 15, 338) | The runner derives the identity from all four names and requires agreement; a mismatch, a missing part or a malformed part stops before the provider | `identityIsReadFromTheOwnersFileNamesWhateverTheirPattern`, `identityMissingOrDisagreeingNamesStopBeforeAProviderIsInvolved`, `theChainNeedsBothIdAndSeriesBeforeAnythingIsSent`, `aMissingIdOrSeriesStopsBeforeAnyProviderCall`, `placeholderNamesAreRefusedBeforeAnyProviderCall`; Python identity test | Provided |
| 9 | Outputs are named `[ID]_REPORT_L1_[SERIES].txt`, `[ID]_VI_L2_[SERIES].txt`, `[ID]_FINAL_QA_[SERIES].txt` | ID and SERIES are now in the manifest; `V5FinalExtractor` takes the `FINAL_QA` named block or `<FINAL>` and nothing else | `V5FinalExtractorTest` (existing) | Provided; whether the model names its files as the pack expects is visible only live |
| 10 | Artefact gate: no predecessor artefact, no next turn (`INPUT_PREDECESSOR_MISSING`, `INPUT_ARTIFACT_MISSING`; lines 15, 195, 221) | One shared message history: the model's own turn-1 and turn-2 answers precede the next user turn, verbatim | `sendsThreeTurnsWithThePriorAssistantAnswersAndExtractsFinal` | **Provided by construction, not provable offline.** In ChatGPT the reports may be file attachments; whether the model accepts its own earlier answer as the artefact is observable only in the canary. If turn 2 stops with `INPUT_PREDECESSOR_MISSING` the canary reports that code and V5 stops |
| 11 | Chain pins the Project and Workflow revision; no hot-swap (G14; `INPUT_CHAIN_REVISION_MISMATCH`) | One Project Instruction (system, once per request), one Workflow, one version string; SHA-256 of the three pack files is recorded per run | `sendsThreeTurns...` (project exactly once per request); runner metadata `v5PackSha256` | Provided |
| 12 | Any stop receipt ends the chain; truncation is `RETRY_OUTPUT_TRUNCATED` (G20; stop-receipt contract) | `stop_class:` in an answer stops at that turn with `V5_STOP_<reason_code>`; `length` stops with `V5_TRUNCATED_L<n>`; no later turn is sent | `stopReceiptOnTheFirstTurnStopsBeforeAnyLaterProviderCall`, `lengthOnTheFirstTurnStopsWithoutSendingTheSecond` | Provided |
| 13 | One chat for all three turns; no new chat between turns (prompt) | One history for L1, L2, L3 | provider test | Provided |
| 14 | Pair Context is optional (G18; line 41) | Not supplied | n/a | Provided (omitted by design) |
| 15 | Room for an exhaustive report plus the full chapter | At least 32,768 output tokens per turn | `MIN_OUTPUT_TOKENS` assertion in the provider test | Provided |
| 16 | QA_RECEIPT: final hash and count "if the host or tool supports it" (line 267) | Not provided: the host has no final-read hash tool | n/a | Not provided; optional in the pack |

No condition is missing that would make a live run pointless. Row 10 is the one real uncertainty and it can only be settled by the canary; row 5 does not apply to the eight chapters; row 16 is optional.

### Q2.6.1 validation

Forced full run on the working tree: `:editorial-engine:test` 582/582; `:app:testBenchmarkUnitTest`, `:app:testDebugUnitTest`, `:app:testReleaseUnitTest` 443/443 each; `:app:compileDebugAndroidTestJavaWithJavac` PASS; all of `scripts/p6` 83/83. No provider call, no device operation, no build for Q2.6.1.

Manifest lines for chapter 007 as the app will print them (names, counts and SHA-256 from the independent Python cross-check; anchors and book text omitted; the Java values written by the runner are compared with these after the canary):

```text
ID=007
SERIES=JAKUAKU_MONSTER_VOL1
VERSION=V5-SAFE.4.1.3-FULL
FILE role=RAW name="007_RAW_JAKUAKU_MONSTER_VOL1.txt" bytes=11660 chars=4030 lines=207 nonblank_lines=104 sha256=cb750dab75a11518ed610ebdb19ef0df8270e9c98653aabb7a8bb0b805393231 bytes_readable=yes
FILE role=DRAFT name="007_JAKUAKU_MONSTER_VOL1_DRAFT.txt" bytes=13242 chars=9758 lines=206 nonblank_lines=104 sha256=151b53d170ec32fe60cf67a7959bfcec16986efa2937e8751b2ecd0fa33a67fe bytes_readable=yes
FILE role=GLOSSARY name="007_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv" bytes=2778 chars=2058 lines=36 nonblank_lines=36 sha256=f0299a55b334f45bdc1ab42edd97e3a757546699181a83ae60059a326b1caa81 bytes_readable=yes
FILE role=PRONOUN name="007_PRONOUN_JAKUAKU_MONSTER_VOL1.csv" bytes=348 chars=285 lines=3 nonblank_lines=3 sha256=08b9afcb40772e456e29c3d90712ff8de3ad87403d1415fb6585f650516526cf bytes_readable=yes
```

### Q2.6.2 — wrapper build and focused emulator tests (offline)

Source commit `7cc4108b0eb41715c824f0fa65c55baec2269ec7` (Q2.6.1, pushed), contract `EDITORIAL_API_V1.4`. Built in the clean throwaway worktree through `scripts/build-and-save.ps1` (series `4.18-q2`, minimum version code 247, offline): `4.18-q2.3` / code `247`, event `build-20261009-034756`, APK SHA-256 `D21C64E5A08879E88B850C60E45515812A8902ABB30DFB1449F292434D20B4DE`, source ZIP SHA-256 `276F2769775229FC0AAB648654B37074FE9FA0F3C12DBD4E524015B65E422AB8` (contains `V5HostSourceManifest.java`, `V5SourceIdentity.java` and their tests). Wrapper unit and lint gates passed. The payload (APK, `BUILD_INFO.json`, README, `SHA256SUMS.txt`, source ZIP) is byte-identical in `artifacts/builds/v4.18-q2.3/` and `backup/builds/v4.18-q2.3/`. The previous build `4.18-q2.2` / code 246 stays archived.

AndroidTest archive (not a production artifact), event `q26-editorial-api-20261009`: test APK SHA-256 `58ED4E1843F27E0C768053E22E432BCA8702056325CA9F451E741ABA2BC26D89`, signer `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`, payload mirrored in `artifacts/test-builds/` and `backup/test-builds/`.

Both packages were installed on `emulator-5554` only (production by `adb install -r`, which keeps the app data and the external-storage spend ledger; test package with `-t`). The installed app reports `versionName=4.18-q2.3`, `versionCode=247`. The owner's phone was not touched.

Focused on-device Editorial API tests (explicit classes only, no broad suite; all with fake providers, no live argument, logs at `D:\P5E-private\q2-runs\Q2.6\q2.6.2-emulator\`):

| Class / phase | Result |
|---|---|
| `EditorialApiStoreInstrumentedTest` | 6/6 PASS |
| `EditorialApiBienTapFlowInstrumentedTest` | 3/3 PASS |
| `EditorialApiBienTapUiInstrumentedTest` (normal path; the three phase-gated cases are skipped by their own assumption) | 2/2 PASS |
| process death: seed, `am force-stop`, verify, cleanup (`bientap_phase`) | 3/3 PASS |

The `Q2-20261008` ledger was pulled before the install and after the tests: SHA-256 `28D87C2D2E48E01F37148C7B392521BC95D79F43506865B87EE36861C6156AE0` both times (11 settled calls, USD 0.8455608, 0 pending, stored cap USD 6.00), so the tests did not touch it.

Offline V5 rehearsal on the emulator (fake provider, group `Q26-OFFLINE-REHEARSAL-1`, 0 provider calls, USD 0): the runner read the roles map, staged the four original files, derived `ID=007` / `SERIES=JAKUAKU_MONSTER_VOL1`, passed the preflight and wrote the manifest facts into `run-metadata.json`. Every per-file value written by the Java runner (role, name, bytes, characters, lines, non-blank lines, SHA-256) equals the independent Python cross-check for all four files of chapter 007 (table in Q2.6.1 above), which also confirms that the files on the device are byte-identical to the owner's originals. The whole-block SHA-256 of the rendered manifest for chapter 007 is `f6b11a5380cfd493aa47bd43df5b6933229082f90a3981306d3a52389ab4bc51`.

Runner scripts (outside Git, `D:\P5E-private`): `run_q26_api.ps1` (V5 reads `q2-inputs` with original names and the roles map; `-Offline` is the rehearsal) and `run_q26_emulator_tests.ps1`, SHA-256 `A2B85B45D802E273F8EF0C278CE4D675F2EC764109320FFBE73EC2E54D2795D7` and `9B9B6F2F810E20C7244185C41E5C8BE2CE7E7A18AE313C0B15B2E9879387DDC2` at the time of Q2.6.2.

### Q2.6.3 — live runs under D-Q2c

Installed app `4.18-q2.3` / code 247 (source `7cc4108b`, contract `EDITORIAL_API_V1.4`), `emulator-5554` only, model `openai/gpt-5.6-luna`, reasoning `medium`, route OpenAI, group ledger `Q2-20261008` (stored cap USD 6.00; D-Q2b ceiling USD 10.00), V5 chapter cap USD 1.50, E chapter cap USD 0.10. Before every run the runner checked the endpoint/account fingerprint (MATCH, 0 calls) and verified the ledger (`verify_spend_ledger.py`). Owner FINAL files were used only by the scorer. Nothing in code, prompts, thresholds or acceptance rules changed between the canary, dev and holdout; U1, pilot and chunk-pair were not touched. Ledger before Q2.6.3: 11 settled calls, USD 0.8455608, 0 pending.

**Canary, V5-luna chapter 007 (run `24bf28b7-b6fc-426e-9f07-7ab07b33691c`) — passed.** The request had one HOST SOURCE MANIFEST, then the unchanged prompt and Workflow, then four `=== FILE: <original name> ===` blocks (`007_RAW_JAKUAKU_MONSTER_VOL1.txt`, `007_JAKUAKU_MONSTER_VOL1_DRAFT.txt`, `007_JAKUAKU_MONSTER_VOL1_chapter_glossary.csv`, `007_PRONOUN_JAKUAKU_MONSTER_VOL1.csv`), one Project Instruction in each of the three requests, and none of the E-path text (no "APP DETECTIONS", no "QUALITY STANDARD"). Turn-1 request capture SHA-256 `efdc1001938d641754dc90ffc1afe6755184bdc6a1a372fb2e38dc933fbfd144`; SHA-256 of the manifest block `f6b11a5380cfd493aa47bd43df5b6933229082f90a3981306d3a52389ab4bc51` (identical to the offline rehearsal). The per-file values the runner wrote (role, name, bytes, characters, lines, non-blank lines, SHA-256) equal the Python cross-check for all four files. All three turns finished with `finish_reason=stop` and no `stop_class`; the model named its outputs with the chain identity (`007_REPORT_L1_…`, `007_VI_L2_…`, `007_QA_RECEIPT_…` for series `JAKUAKU_MONSTER_VOL1`), the second and third turns accepted the earlier answers in the history (so row 10 of the table above held), and the FINAL was extracted: app state `FINAL_NOTES`, structurally valid, 3 physical calls, 78,927 input and 19,513 output tokens, USD 0.03241211. The stop that ended the Q2.5 canary (`INPUT_ARTIFACT_MISSING`) did not recur.

**Dev matrix.** E-luna-b (independent of the canary) on 004–008 and V5-luna on 004, 005, 006, 008 (007 is the canary). One run per arm and chapter, no repeats, no retry after dispatch. Columns follow the scorer; "Farther" is counted only on lines the owner changed, as the gate defines it; the gate columns are the automatic conditions 1–4 of the minimum gate (`scripts/p6/min_gate_413.py`, new in this package, with its own unit tests), and the owner's reading (condition 5) is not computed.

| Arm | Ch | State | Owner changed | Improved | Near-exact | Farther (owner-changed lines) | App changed owner-unchanged | Added kana/Han | Delta similarity | Gate 1-4 | Failed checks | calls | in tok | out tok | USD |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---|---|---:|---:|---:|---:|
| E-luna-b | 004 | FINAL_NOTES | 60 | 2 | 0 | 3 | 5 | 0 | -0.00130236 | FAIL | c2_japanese_lines_not_above_final, c3_similarity_not_below_draft, c4_improved_at_least_25pct | 1 | 8104 | 4130 | 0.00698185 |
| E-luna-b | 005 | FINAL_NOTES | 31 | 0 | 0 | 1 | 7 | 0 | -0.00497477 | FAIL | c3_similarity_not_below_draft, c4_improved_at_least_25pct | 1 | 10565 | 5544 | 0.0092939 |
| E-luna-b | 006 | FINAL_NOTES | 38 | 5 | 5 | 9 | 16 | 0 | -0.00133459 | FAIL | c2_symbol_mismatch_not_above_draft, c3_farther_on_owner_changed_at_most_10pct, c3_similarity_not_below_draft, c4_improved_at_least_25pct | 1 | 16136 | 7653 | 0.01321745 |
| E-luna-b | 007 | FINAL_NOTES | 18 | 3 | 3 | 2 | 10 | 0 | -0.00431065 | FAIL | c1_line_count_vs_raw, c3_farther_on_owner_changed_at_most_10pct, c3_similarity_not_below_draft, c4_improved_at_least_25pct | 1 | 7911 | 4693 | 0.0076092 |
| E-luna-b | 008 | FINAL_NOTES | 20 | 0 | 0 | 0 | 7 | 0 | -0.01068225 | FAIL | c2_symbol_mismatch_not_above_draft, c3_similarity_not_below_draft, c4_improved_at_least_25pct | 1 | 8677 | 4501 | 0.0075703 |
| V5-luna | 004 | RETRY_REQUIRED / invalid (V5_STOP_CONTENT_UNACCOUNTED_CHANGE) | 60 | 0 | 0 | 0 | 0 | 0 | +0.00000000 | FAIL | c1_valid_not_truncated, c2_japanese_lines_not_above_final, c4_improved_at_least_25pct | 3 | 71413 | 13868 | 0.02420742 |
| V5-luna | 005 | FINAL_NOTES | 31 | 1 | 0 | 0 | 4 | 0 | -0.00300171 | FAIL | c3_similarity_not_below_draft, c4_improved_at_least_25pct | 3 | 82164 | 22022 | 0.03536322 |
| V5-luna | 006 | FINAL_NOTES | 38 | 7 | 5 | 7 | 12 | 0 | +0.00060666 | FAIL | c3_farther_on_owner_changed_at_most_10pct, c4_improved_at_least_25pct | 3 | 101020 | 25334 | 0.04150276 |
| V5-luna | 007 | FINAL_NOTES | 18 | 8 | 8 | 2 | 5 | 0 | -0.00324978 | FAIL | c1_line_count_vs_raw, c3_farther_on_owner_changed_at_most_10pct, c3_similarity_not_below_draft | 3 | 78927 | 19513 | 0.03241211 |
| V5-luna | 008 | FINAL_NOTES | 20 | 7 | 8 | 0 | 7 | 0 | -0.01190111 | FAIL | c3_similarity_not_below_draft | 3 | 79795 | 21652 | 0.03458618 |


Dev totals per arm (167 owner-changed lines each):

| Arm | Chapters passing 1–4 | Improved | Near-exact | Farther (owner-changed) | App changed owner-unchanged | Added kana/Han | Physical calls | Input / output tokens | USD |
|---|---:|---:|---:|---:|---:|---:|---:|---|---:|
| E-luna-b | 0/5 | 10 (6.0 %) | 8 | 15 | 45 | 0 | 5 | 51,393 / 26,521 | 0.0446727 |
| V5-luna | 0/5 | 23 (13.8 %) | 21 | 9 | 28 | 0 | 15 | 413,319 / 102,389 | 0.16807169 |

V5-luna 004 stopped in turn 3 with the model's own typed stop `V5_STOP_CONTENT_UNACCOUNTED_CHANGE` (it found sentence-final `。` marks removed in dialogue without a change ID in the Change Map and refused to release); the app kept the DRAFT and recorded `RETRY_REQUIRED`. It is scored as invalid, was not resent, and did not repeat on the next chapters, so the V5 series continued. No run produced a truncation, a `length` finish, an UNKNOWN cost or an overrun.

**Gate and choice.** No chapter of either arm satisfies conditions 1–4 (0/5 and 0/5), mostly on condition 4 (improved lines below 25 % of the owner-changed lines), then on condition 3 (similarity to FINAL below the DRAFT's, or more than 10 % of the owner-changed lines moved farther) and, in single cases, on condition 1 or 2 (line count against RAW; Japanese lines above FINAL; symbol mismatches above the DRAFT's). By the frozen rule the arm with the most passing chapters is chosen and a tie goes to the higher improved count: both have 0, V5-luna has 23 against 10, so **V5-luna** was chosen. Because fewer than 3 of 5 dev chapters pass, the result is recorded as **not reaching the 4.1.3 minimum** and the holdout was run only to give the owner something to read, as the request specified.

**Holdout 011, 014, 017 — V5-luna, no change from dev.**

| Arm | Ch | State | Owner changed | Improved | Near-exact | Farther (owner-changed lines) | App changed owner-unchanged | Added kana/Han | Delta similarity | Gate 1-4 | Failed checks | calls | in tok | out tok | USD |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---|---|---:|---:|---:|---:|
| V5-luna | 011 | FINAL_NOTES | 30 | 2 | 2 | 1 | 2 | 0 | +0.00018658 | FAIL | c4_improved_at_least_25pct | 3 | 98694 | 25276 | 0.04127555 |
| V5-luna | 014 | FINAL_NOTES | 52 | 14 | 12 | 9 | 6 | 0 | +0.00084328 | FAIL | c3_farther_on_owner_changed_at_most_10pct | 3 | 106661 | 26054 | 0.04303549 |
| V5-luna | 017 | FINAL_OK | 40 | 0 | 0 | 0 | 0 | 0 | +0.00127845 | FAIL | c4_improved_at_least_25pct | 3 | 86512 | 20937 | 0.03442372 |


Holdout total: 122 owner-changed lines, improved 16 (13.1 %), near-exact 14, farther on owner-changed lines 10, app changed owner-unchanged 8, added kana/Han 0, 9 physical calls, 291,867 input and 72,267 output tokens, USD 0.11873476. 0/3 chapters pass conditions 1–4 (011 and 017 on the 25 % improvement condition, 014 on the 10 % farther condition). Chapter 017 returned `FINAL_OK` with the text essentially equal to the DRAFT (0 improved, 0 changed lines), which is a valid no-change answer, not a fix.

**Spend and ledger.** Final ledger (pulled after the last run, `verify_spend_ledger.py` PASS): 24 settled logical calls, 48 entries, USD 1.17703995 settled, 0 pending, 0 UNKNOWN, 0 overruns, USD 4.82296005 left under the stored USD 6.00 cap (D-Q2b ceiling USD 10.00). Q2.6 spent USD 0.33147915 on 13 logical calls = 29 physical provider calls (5 E, 24 V5); canary USD 0.03241211, E-luna-b dev USD 0.0446727, V5-luna dev (without the canary) USD 0.13565958, holdout USD 0.11873476. Ledger file SHA-256 `86087576bcc1114e12cab76235e884674c330b5c8466ee1e35bfc67d1009efc2`; last entry hash `bd8d265059532ce6a2ed17eaa364161051efe3a371ce2d0a39127bd11da43401`. No stop condition of the approval was triggered (pack-table condition left open: none; UNKNOWN: 0; infrastructure error: 0; estimate above cap: none).

**What this does and does not show.** STRUCTURAL: all V5 runs that completed produced a valid, untruncated candidate; the three-turn chain, the host manifest and the original names work end to end. SEMANTIC: NOT_MEASURED; the automatic numbers are a filter, and the owner's reading decides. PERSISTENCE/EXPORT: the app texts below were copied byte for byte from the run outputs (SHA-256 equals the recorded `finalSha256`). V5-luna fixes more owner-changed lines than E-luna-b (13.8 % against 6.0 % on dev, 13.1 % on holdout) and changes fewer lines the owner left alone (28 against 45 on dev), at about 3.8 times the dev cost; neither reaches the 25 % improvement floor of the minimum gate. 0/3 accepted chapters still stands; this package does not change it, P7 is not started, and no default mode is chosen by this result.

**Owner materials (private, outside Git), `D:\P5E-private\q2-outputs\`:**

| File | SHA-256 |
|---|---|
| `011_APP_V5-luna_JAKUAKU_MONSTER_VOL1.txt` | `de4935b0f215d727e0db062f67f2758a3dcbdd674c497d1993a9197ee21b52b5` |
| `014_APP_V5-luna_JAKUAKU_MONSTER_VOL1.txt` | `4e0166c30849e1e6d350e12470f876dc9829f9465470b04e4e31b575bf085d6f` |
| `017_APP_V5-luna_JAKUAKU_MONSTER_VOL1.txt` | `9090734fcdb0fef4658d0f73cb2b9794032eb2cc66ad350836b427e05e7809ce` |
| `q26-holdout-V5-luna-review.html` (RAW / DRAFT / app / FINAL, lines either side changed) | `885acbe1edebd2c1f134bec4ceb8173e3966c7aee78adcf4ca6d0bfd8d7c4d56` |
| `q26-dev-V5-luna-review.html` (dev 004–008, for comparison) | `34fc14ca7cf7e26d087e9c654a178a2bd49c34dc4fb0325a89ab24f78dabcc96` |
| `q26-dev-E-luna-b-review.html` (dev 004–008, for comparison) | `f57dc1b833d5a5d1a17a5bfd8eed73e39b5ad8f9340cfd6318eb0121ccbcb485` |

Raw run folders, request/response captures, per-run `score.json` and `gate.json`, the summaries `dev-gate-summary.json` and `holdout-gate-summary.json`, and the ledger copies are under `D:\P5E-private\q2-runs\Q2.6\`. Private scripts: `run_q26_api.ps1`, `run_q26_dev.ps1`, `run_q26_holdout.ps1`, `score_q26.py`, `make_q26_review_page.py`. Q2.6 is complete; the run stops here for the owner's reading.

**Q2.6 validation (forced, final tree `HEAD` plus the gate tool):** `:editorial-engine:test` 582/582; `:app:testBenchmarkUnitTest`, `:app:testDebugUnitTest`, `:app:testReleaseUnitTest` 443/443 each; `:app:compileDebugAndroidTestJavaWithJavac` PASS; all of `scripts/p6` 90/90 (83 before plus 7 for `min_gate_413.py`); focused emulator API tests 6/6, 3/3, 2/2 and process death 3/3 on build `4.18-q2.3`; provider calls in Q2.6: 29, all within D-Q2c.
