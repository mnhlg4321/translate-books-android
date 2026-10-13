# v4.20 D — 014, S0–S9, 2026-10-13

Product decision served: owner decides whether to ship or drop C1 (model cites the pronoun row for ADDRESS edits). Work request `docs/V420_D_014_WORK_REQUEST.md` section 2: one 014 run, luna medium, at most 10 calls, no retry, new ledger, group/chapter cap USD 0.08, emulator-5554 only. **Owner quality decision is PENDING the S7 reading page.** Preliminary word/reference counts below are not semantic adjudication.

## S0–S4

- Resume: clean `feature/v4.20`, HEAD and origin `f7e39015f77fe7eb734925cff4d4ff4619069f53`, worktree `D:/P5E-builds/wt-v4.20`.
- S1 `9bcbabf4`: opt-in p6_no_retry=YES executes every pair once via executeOnly; stops at first non-ACCEPTED pair (no-point pairs are ACCEPTED by service). Call wrapper refuses calls above p6_max_calls, default unlimited without it; metadata noRetry/maxCalls/dispatched/refused. `abffa518` makes only the opt-in fake provider reply with numbered keep answers `[id] =`. AndroidTest compile PASS. No content gate, suite, index, relabel or v4.19 optionalDa. No production diff from starting f7e39015; inherited C1 contract remains CP-IMPL-16.
- Production wrapper `4.20-d.1`/code268 from `9bcbabf419bc85083fb3fa4ac971341d1cd4bef9`; event `build-20261013-095132`; APK SHA-256 `BB603C680AC5EE17CA8874AE078FCEC7BA91559382118CC358F248B51465184E`; source ZIP `C7EC11C46530ABBBD0C5CFBAF36B026EDB83442C25337ADB8F4CECAF7A7F92B9`.
- Test wrapper source `abffa518dd335ea58337ef3e11e1f3af9b9c89bc`, event `v420-d-test-20261013-095400`; APK `3C61FA67B236B98FE0A738966DB085A8B02A11859DA72BA03A73E686F7DBB2E8`; source ZIP `284D058AEF21F450CB1FA6587E337FF82AEFE60B6552F059348DEF1F4DCA6689`. The later commit differs only in test-only fake response formatting; production sources identical.
- Both production/test complete payloads copied BEFORE installation and byte-verified in `D:/App Translate Books/artifacts/builds|test-builds/v4.20-d.1/<event>` and matching `backup/builds|test-builds`; original worktree mirrors retained too. Existing event directories not reused/overwritten.
- S3: only emulator-5554, code267 → 268/name4.20-d.1, same signer abebea4b and certificate 47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155; production/test install -r and readback. No phone/clear/uninstall/reset/wipe.
- Wrapper host debug tests 472 total, 3 SKIP, 0 failures; lint 0 errors/57 warnings. Unchanged C1 engine was not redundantly retested (the canonical clean-archive evidence remains 714/1 skip). Test wrapper compile/assemble PASS.
- S4 run 6f74eee3-bc0c-4df9-85d4-27500b3be9e2: 16 chunks, 10 fake calls, 0 real, 0 refused, noRetry=true/maxCalls=10; FINAL_ELIGIBLE, final TXT exactly DRAFT bytes, both SHA `6546cec1b4be245b9f518be42b7a413e86c2817c4196c58f624a582ef08fb115`.
- Private runner copied from the original into `D:/P5E-private/run_v420_d_chunk.ps1`; locked to 014, <=0.08, V420-D ledger ids, opt-in no retry/max10. Source `run_c1_chunk.ps1` SHA stays `9C42650452A66970CB0E305D1FBF9A89F1F369EEEFFFD71DAE026C9B3A6028A0`. A private-copy quoting error was repaired before the fake/live runs; no provider dispatch from that failure.

## S5–S6

Exactly one live run `7bc2b3e2-9795-4299-98f4-5ebfdcca6bfe`; ledger `V420-D-014-20261013-7bc2b3e2`. Endpoint/account preflight passed using the private device settings source used at X5, no credential/fingerprint logged in Git. Known configured reservation prices 0.25/M input, 1.20/M output; current public catalog base is no higher. No retry/resume/second run.

7 real calls, 0 refused, noRetry=true, maxCalls=10; 36,973 input/7,312 output; all finish reasons stop. Exact settled **USD 0.01801660** under 0.08. Ledger hash chain VALID, 7 RESERVE/7 SETTLE, **pending0/overrun0**, matching dispatched count.

Reached chunks1–13 (12 ACCEPTED, including no-point chunks); chunk13 STRUCTURE_BLOCKED due to glossary point13:3 `PROTECTED_SYMBOL_CHANGE`, with `TARGETED_LINE_REJECTED`. Runner stopped before14. Chunks14–16 **NOT_REACHED**. Chapter INCOMPLETE; exported TXT is provisional service output, not an accepted complete FINAL. C1 production was not edited after seeing the response. Unused cap grants no new run.

20 evidence files (prompts7/responses7 + chunk plan/chunks/output/metadata/structural/ledger) retained in `D:/P5E-private/chunk-runs/V420-D/live/014-7bc2b3e2-9795-4299-98f4-5ebfdcca6bfe/results/fx-a01/`; every file SHA-256 is in SUMMARY.json. Device originals also retained; no evidence deletion.

## S7–S8: owner reading and preliminary counts

Reading page `D:/P5E-private/chunk-runs/V420-D/READING_014.html`: **37 rows**, union of all24 app-changed lines and21 distinct step-B self-error lines (34 word occurrences,8 lines intersect). Columns RAW/DRAFT/app/FINAL explicitly reference only, actual cited R<n> row from each point's captured prompt/response, chunk state/guard reason,37 empty owner adjudication controls and notes. One cậu↔name changed line is grouped separately. Blocked-chunk proposals are distinguished from the actual provisional TXT; NOT_REACHED shown for remaining chunks. RAW spans use recorded CS-1 beads, app↔DRAFT preserve273 units; FINAL unequal replace blocks are explicitly marked, not guessed as exact 1:1.

| S8 count | Value |
|---|---|
| Step-B self-error occurrences / distinct lines | 34 / 21 |
| Lines whose app edits match all step-B reference word substitutions | 7 |
| Step-B lines unchanged | 13 |
| Step-B lines changed differently | 1 |
| App changed lines outside step-B | 16 |
| cậu↔name changed lines | 1 |
| Guard-kept ADDRESS_SCOPE_EXCEEDED | 1 |
| Guard-kept ADDRESS_CITED_ROW_EXCLUDED | 3 |
| Guard-kept ADDRESS_PLURAL_SELF | 1 |
| Guard-kept ADDRESS_VALID_FORM_CHANGED | 1 |
| ADDRESS_ROW_NOT_CITED / ADDRESS_FORM_NOT_IN_CITED_ROW / ADDRESS_CITED_ROW_NOT_ADDRESSED | 0 / 0 / 0 |
| NOT_REACHED chunks | 14,15,16 |

The7/13/1 classification compares step-B old→new word substitutions with app token edits, not full-line equality and not correct/incorrect labels. Partial reference word matches are classified changedOther (none here). The34 cases are word occurrences on21 lines, not34 independent lines. All step-B lines lie in reached chunks, including the blocked chunk; no step-B line lies in14–16. Owner must judge fixes/misses/new wrong edits from S7; no ship/drop conclusion made.

## S9

Report and hash-only SUMMARY mirrored to backup; branch publication is authorized after secret/book-text/fingerprint scan. The existing checklist release steps remain open. No merge/tag/release, no new document/suite/gate, no change to owner input files or the original script. Stop here for owner reading S7.
