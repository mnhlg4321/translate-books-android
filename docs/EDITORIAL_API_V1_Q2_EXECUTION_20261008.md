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

Pending; dispatch begins with the new `Q2-20261008` ledger and dev chapters 004–008 on the two frozen arms. No live dispatch has occurred in this package yet.

