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

The build/price/token matrix is recorded under `q2_5` in `docs/EDITORIAL_API_V1_Q2_AB_MANIFEST.json`. Next: Q2.5.4 starts with only V5-luna chapter007. If any of its three turns stops, truncates, lacks FINAL, or becomes UNKNOWN, stop all of Q2.5. If the canary is valid, run the frozen dev arms and select holdout only by the section-7 quality gate; re-read the ledger immediately before dispatch.

