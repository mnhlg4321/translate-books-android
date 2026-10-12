# P6 R6 wire v3 execution — 2026-10-04

Scope: docs/P6_R6_UNIT_REF_WORK_REQUEST_20261003.md at e268770c, including approved D-G1b. Continue the existing v4.18 branch/checklist; no pilot access and no G2.

## V1 — offline implementation

Wire labels for L1 RAW/RECONCILE, L2 RAW discovery/edit occurrence references, L3 RAW re-audit/reconcile are v3. The current app coordinator uses L1_LEDGER_V3. The model receives RAW as L<physical line>|text and line references in candidate/report views. EditorialUnitReference resolves them against the unchanged app inventory before coverage, quote, finding, occurrence and probe validation. Durable REPORT_L1, change-map evidence and QA probe records retain full u:<line>:<hash> ids.

Malformed syntax returns L1_UNIT_REF_INVALID; out-of-bounds/overflow returns L1_UNIT_UNKNOWN; blank, Unicode blank or image-marker lines return L1_UNIT_LINE_NOT_A_UNIT. Coverage remains a closed partition over actual units, including across excluded physical lines. Candidate id, note, count and quote limits remain enforced. Shared strict L1 schemas pin ^L[1-9][0-9]*$. V2 report bodies remain readable; v2 predecessors cannot answer v3 chains. V2 and legacy identity hashes are frozen by tests; the v3 revision changes attempt/request identity.

Both build wrappers now accept -Offline, forwarding --offline to Gradle. Existing cached Gradle/JDK dependencies are used. The initial cache selection failed before compilation; subsequent gates use C:/Users/ADMIN/.gradle. No provider was called.

Evidence: engine 351/351, app JVM 341/341; lintDebug PASS; compileDebugAndroidTestJavaWithJavac PASS. Command: gradlew.bat --offline :editorial-engine:test :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestJavaWithJavac. Log: D:/P5E-private/unit-ref-v1-final-tests.log. Python tests 33/33, log D:/P5E-private/unit-ref-python-tests.log. New tests cover syntax/range/exclusions, partition checks, report round-trip, v2 rejection/identity, model-facing views, shared schema and L3 candidate/probe resolution. Existing engine chain/coordinator fakes use v3 wire.

## V2 — offline PASS

- Production wrapper: `build-and-save.ps1 -Offline -Series 4.18-p6 -MinimumVersionCode 229 -Install -DeviceSerial emulator-5554` with pinned certificate/signature, source `d1cc14525be9fb444c9773fcb275b80dfdf7bede`, clean temporary worktree `D:/P5E-builds/wt-p6-unit-ref-20261004`. Build `4.18-p6.16`/code229, event `build-20261004-065614`. APK SHA-256 `98AAE03A23DD6D0573E2C5FFBCB46C6B18918D35D6A68B663143B0D5B6673141`; exact tracked-source ZIP SHA-256 `A7F543DBCC394C5295F61CF4FABF5FAE8750D827258D0E30430C3AFFFFEF2170`.
- Production payload parity 5/5 under `artifacts/builds/v4.18-p6.16/build-20261004-065614` and `backup/builds/v4.18-p6.16/build-20261004-065614`. Wrapper archived both before installing. The temporary artifact copy was additionally retained in the main workspace artifact root; no existing payload overwritten. Wrapper log `D:/P5E-private/unit-ref-v2-wrapper.log`.
- AndroidTest wrapper `build-and-save-android-test.ps1 -Offline`: event `p6-unit-ref-d1cc1452-20261004-16`, APK SHA-256 `7A03D658CE7B83C59509791E6315976D943A67661D234B236F265E1777DF7AAF`, same exact source ZIP hash; parity 8/8 in `artifacts/test-builds` and `backup/test-builds`. Test APK installed and pulled back with matching hash. Log `D:/P5E-private/unit-ref-v2-test-wrapper.log`.
- Existing AVD `tbl-code113-dqa-api35` restarted without wipe. Production install/readback PASS on emulator-5554 only. Preflight 5/5 (including deliberately wrong fingerprint: FINGERPRINT_MISMATCH, providerCalls=0), coordinator ledger chain/protection/restart 1/1 PASS; log `D:/P5E-private/unit-ref-v2-preflight-coordinator.log`.
- Fake CHAIN run `cee702b0-f933-482f-91a8-a723f41d2775`: STRUCTURAL_VALID 14/14; 112 fake calls, 0 actual calls, authoritative group ledger 224 entries, USD 0, pending 0. SEMANTIC_EVAL FAIL 12/PASS 2 is the no-edit fake control, not model quality. Verifier's 1680 summed entries count cumulative ledger copies, not additional calls. Evidence under `D:/P5E-private/p6-runs/cee702b0-f933-482f-91a8-a723f41d2775`; log `D:/P5E-private/unit-ref-v2-fake-chain.log`.
- Negative gate run `6db963b6-be5e-4788-837c-f51ab9447433`, fx-a03, fake L1_ONLY: valid=false, stage=L1, P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID, phase=L1_RAW_DISCOVERY, detail L1_COVERAGE_GAP. 1 fake call/0 actual calls/USD 0, 2 ledger entries, pending 0. `verify_fixture_run.py --expect-invalid-l1` and frozen scorer read it successfully (STRUCTURAL_VALID 0/1, semantic FAIL because DRAFT remains). Evidence under its private run directory; log `D:/P5E-private/unit-ref-v2-negative.log`.

## V3 — executed; G1 stopped at L1 as required

Matching zero-call preflight reached the deliberately absent `fx-a03.runtime.json` sentinel after the fingerprint comparison and before provider construction. This is evidence that the saved route/key pair matched the owner value; the intentional missing-input instrumentation failure is not counted as a passing fixture. Network probe to openrouter.ai passed. No Settings change or pilot access. Official price page rechecked at 2026-10-04 00:03 UTC: [OpenRouter GPT-5.6 Luna](https://openrouter.ai/openai/gpt-5.6-luna), displayed normal input/output USD 0.20/1.20 per million and cache-read 0.02, unchanged from P4; existing conservative reservation rates/caps retained.

Run `b4c0a9c2-118c-4d51-a172-955c6f95d999`, group **G1-20261003-ecbf8c55**, mode L1_ONLY, began with fx-a03. Frozen fixture/hash/prompt-input guard passed immediately before dispatch. Model/route/reasoning stayed pinned (`openai/gpt-5.6-luna`, minimal). The runner wrote its typed result before the host stopped the whole group.

| Phase | Result | Input tokens | Output tokens | Finish | Real provider-reported USD |
|---|---|---:|---:|---|---:|
| L1_RAW_DISCOVERY | Accepted by production engine, proceeded to reconcile | 20,932 | 1,841 | stop | 0.00744205 |
| L1_RECONCILE | REPAIR_L1_LEDGER_INVALID, detail L1_TEXT_REQUIRED | 29,718 | 3,656 | stop | 0.01181655 |
| Current V3 total | 2 calls, 0 retry/repair, 0 UNKNOWN | 50,650 | 5,497 | both complete | **0.01925860** |

Full refusal: `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`; `stage=L1`; `phase=L1_RECONCILE`; engine detail **L1_TEXT_REQUIRED**. It means a required text field was empty/blank. Current safe diagnostics do not retain its field name, and provider response bodies are not retained by this harness, so the particular field is **not established**. Do not infer a coverage defect, truncation, or content conflict. The previous RAW L1_UNIT_UNKNOWN did not recur in this attempt; this is one successful live RAW structural check, not semantic acceptance.

| Fixture scope | Structural result | Semantic/scorer result | Calls in this V3 run |
|---|---|---|---:|
| fx-a03 | valid=false, STRUCTURAL_VALID 0/1; L1 stopped before REPORT_L1 | FAIL: KNOWN_DEFECT_NOT_FIXED:T-S1; final.txt is fallback DRAFT, not model quality evidence | 2 |
| fx-a04, fx-a05, fx-a07, fx-a08, fx-a11, fx-a02, fx-a12 | NOT_STARTED after group stop | NOT_MEASURED | 0 |
| Two planned extra rounds of fx-a11/fx-a04 | NOT_STARTED | NOT_MEASURED | 0 |

G1 cumulative ledger now has **4 settled calls / 8 entries**, **USD 0.03222845 / 1.00**, remaining **USD 0.96777155**, pending **0**. Before = USD 0.01296985; delta = exactly USD 0.01925860. The old ledger file remains an exact byte prefix of the new file; no old event overwritten/reused. There was no L2/L3 dispatch, repair call, retry, later fixture, G2, pilot install or pilot DB operation.

Evidence: `D:/P5E-private/p6-runs/b4c0a9c2-118c-4d51-a172-955c6f95d999/`, including results/fx-a03/structural.json, run-metadata.json, spend-ledger.jsonl, two captured L1 prompts and score-report.json. Redacted provider usage: `D:/P5E-private/unit-ref-v3-provider-usage.log`; group-stop log `D:/P5E-private/unit-ref-v3-g1-base.log`; matching preflight log `D:/P5E-private/unit-ref-v3-match-preflight.log`.

Validation: authoritative `verify_spend_ledger.py` and fixture ledger hash-chain check PASS; both captured L1 phase prompts and host-only oracle checks PASS; frozen `score_run.py` read the partial output (STRUCTURAL_VALID 0/1, semantic FAIL 1). `verify_fixture_run.py --live --expect-invalid-l1` correctly refused that CLI combination: its negative convenience flag is offline-only. It was not weakened; partial live evidence was checked with the existing ledger/prompt routines and frozen scorer directly. Log `D:/P5E-private/unit-ref-v3-verify-partial.log`. No absent REPORT_L1 is claimed valid.

Artifact SHA-256: structural.json `89e499389256ed172c3ebdd472d186b24250e6996d39a29fcc245d496356cad1`; run-metadata.json `d79ed13eba82f856e4f6201cb5d99146e7bf6e029c6dcfa443df6dbbe412562c`; fixture spend-ledger.jsonl `8c4163096c73796e654bab88121c73ba9b6de4c7b6595837677346ab247e92b9`.

Commit/push: V1 `d1cc1452`, V2 `d9390733`, both pushed to the existing continuation branch. This final evidence update is a separate commit. Owner changes remain unstaged. No release/tag or P6/P7 completion claimed.

Next action: diagnose L1_TEXT_REQUIRED offline and add a safe field-name diagnostic before proposing any further live G1 attempt.
