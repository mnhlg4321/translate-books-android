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

## V3 — ready, no call yet

Owner D-G1b authorizes G1-20261003-ecbf8c55, starting fx-a03, within USD 1.00 minus USD 0.01296985 already spent. No retry/repair; UNKNOWN or L1 refusal stops the group; stop before G2. Owner resupplied expected fingerprint in chat; value is not recorded in Git or private logs and no API key was requested/read. Emulator ledger rechecked after reinstall: 2 settled calls/4 entries, USD 0.01296985 exposure, remaining USD 0.98703015, pending 0. Before copy `D:/P5E-private/unit-ref-g1-before.jsonl`.

Commit/push: V1 d1cc1452 pushed to the existing origin continuation branch. No tag or release certification.

Next action: match the zero-call fingerprint preflight and execute approved V3 G1 from fx-a03.
