# v4.19 X5 — một lượt 006, dừng đúng cổng hồi quy

2026-10-13, owner “duyệt quyền” cho gói 006: một lượt luna medium, tối đa 14 calls, không retry, cap USD 0.10, sổ mới. **Measurement quality = FAIL / STOPPED**, không claim FINAL/semantic PASS. Không chạy lại.

## Build và preflight

- Production wrapper `4.19-x5.1`/267, source `548594e7e9b01ca3f0d3bb7dc396ef54de789b49`; APK SHA-256 `841CFC6BED8CCFB72711510F8E9296F2BA94FE4DB70C555F45CB07A56C36D488`, source ZIP `50AD4936D1B4E7D13DF38A6714332D45199639C8F97D44A4C434DCA200B1C1BD`, event `build-20261013-082743`, artifact/backup byte-mirror PASS.
- Test wrapper source `5dbf320dcb757caa6d59ac6688374f7d12219a5d`, event `x5-test-20261013-082900`, APK `E81F5200CFD6F86DEFA0DC021D163001AF769D7CB126087A11A28AEDD9F5038E`, source ZIP `1021D14B5BDEFD1E10EC81FE591C8FA8C5DCC4607819A72B3B6AC0598A260A1A`, both mirrors PASS. Only AndroidTest readAllBytes compatibility fix differs from production source; app/engine main identical.
- Production installed/read back only emulator-5554, baseline 266 → 267, same signer abebea4b/certificate 47F31389…EF3C155. AVD resumed without wipe. No clear/uninstall/reset/pilot.
- Host engine 718 total/1 skip; app 478 total/8 skip each debug/release/benchmark (includes 5 new X5 gate tests), 0 failures. Store device QA 6+6+1 PASS.
- Test-only X5 runner uses executeOnly (maxAttempts=1) for each chunk, hard limit 14 calls; pins suite SHA and 006/model/reasoning/caps/15-chunk scope. Checks clean allowed hashes and S4 old wrong fixes after each received chunk, before next dispatch. Production finder/guard/prompt unchanged since X4.
- Fake stored-response replay: 14 fake calls / 0 real provider calls, FINAL_ELIGIBLE, empty X5 stop. Failed initial fake hash argument was uppercase while computed hash is lowercase; fresh fake run with lowercase hash PASS. No live call from that failure.
- Public OpenRouter catalog base input 0.20/M, output 1.20/M; runner reserves conservatively at 0.25/M input and 1.20/M output, package estimate 0.087118 < cap 0.10. Live account/key preflight MATCH; credentials/fingerprint remain private.

## Live result

Run `f8a6ecf7-cee0-430e-a95c-98d69db0eb84`; ledger `V419-X5-006-20261013-f8a6ecf7`. Only chunk 1 dispatched, **1 call**, no retry, finish `stop`; input 6,381 / output 1,136. Exact settled **USD 0.0029583**, initial reserve 0.00646745; 1 RESERVE + 1 SETTLE, hash chain valid, pending 0, overrun 0. Unused cap does not authorize another run.

Stopped at **`X5_CLEAN_REGRESSION:1:7`** before chunk 2; chapter state INCOMPLETE. JUnit OK confirms evidence collection, not model quality PASS. Of chunk 1's seven points: one S5 uncertain point unchanged; five clean outputs unchanged, one strict clean output changed. Later known self/call errors and old wrong-pass cases were not reached and remain NOT_MEASURED for this live run.

At 1:7, model removed the `chị` title before Ōgi and changed third-person `Chị ấy` to `Cô ấy`; guard accepted FIXED/REPLACED. RAW identifies Ai as speaker, referring to Ōgi in third person. This is outside D-A second-person cậu/name choice. Do not reclassify this strict clean record as optional to make the run PASS. Candidate rows cover other speakers; existing finder/guard role-attribution limitations are relevant, but one response does not prove that the prompt change alone caused the regression. No production repair or additional paid run performed after this stop.

## Evidence and limitations

Book text/prompts/responses/chunk gate and reading page remain in `D:/P5E-private/v419-pronoun/X5-20261013/`; `READING_006_X5_STOP.html` shows RAW/DRAFT/candidate. Raw device evidence under the live run's `results/fx-a01/` (9 files). Git receives this aggregate report and hash-only summary; metadata/chunks/ledger/output hashes are in `SUMMARY.json`. QA mirror is retained in backup. No source text or credential copied into Git.

Local FAILED_REPAIRING resolved before live: AndroidTest Files.readString unsupported by Android compile SDK, repaired to readAllBytes/new String; wrapper rebuilt from test-only repair, production inputs unchanged. Provider calls were 0 throughout repair.

**Next action:** review the private 1:7 case and scope an offline repair against strict clean controls; the one-run authority is consumed, no retry/new paid call authorized.
