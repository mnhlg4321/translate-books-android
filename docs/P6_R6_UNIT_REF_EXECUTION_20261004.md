# P6 R6 wire v3 execution — 2026-10-04

Scope: docs/P6_R6_UNIT_REF_WORK_REQUEST_20261003.md at e268770c, including approved D-G1b. Continue the existing v4.18 branch/checklist; no pilot access and no G2.

## V1 — offline implementation

Wire labels for L1 RAW/RECONCILE, L2 RAW discovery/edit occurrence references, L3 RAW re-audit/reconcile are v3. The current app coordinator uses L1_LEDGER_V3. The model receives RAW as L<physical line>|text and line references in candidate/report views. EditorialUnitReference resolves them against the unchanged app inventory before coverage, quote, finding, occurrence and probe validation. Durable REPORT_L1, change-map evidence and QA probe records retain full u:<line>:<hash> ids.

Malformed syntax returns L1_UNIT_REF_INVALID; out-of-bounds/overflow returns L1_UNIT_UNKNOWN; blank, Unicode blank or image-marker lines return L1_UNIT_LINE_NOT_A_UNIT. Coverage remains a closed partition over actual units, including across excluded physical lines. Candidate id, note, count and quote limits remain enforced. Shared strict L1 schemas pin ^L[1-9][0-9]*$. V2 report bodies remain readable; v2 predecessors cannot answer v3 chains. V2 and legacy identity hashes are frozen by tests; the v3 revision changes attempt/request identity.

Both build wrappers now accept -Offline, forwarding --offline to Gradle. Existing cached Gradle/JDK dependencies are used. The initial cache selection failed before compilation; subsequent gates use C:/Users/ADMIN/.gradle. No provider was called.

Evidence: engine 351/351, app JVM 341/341; lintDebug PASS; compileDebugAndroidTestJavaWithJavac PASS. Command: gradlew.bat --offline :editorial-engine:test :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestJavaWithJavac. Log: D:/P5E-private/unit-ref-v1-final-tests.log. Python tests 33/33, log D:/P5E-private/unit-ref-python-tests.log. New tests cover syntax/range/exclusions, partition checks, report round-trip, v2 rejection/identity, model-facing views, shared schema and L3 candidate/probe resolution. Existing engine chain/coordinator fakes use v3 wire.

## V2 — pending

Wrapper build/archive/install and emulator preflight/coordinator/fake CHAIN 14/14/negative gate will run from committed tracked source. Existing AVD tbl-code113-dqa-api35 has been restarted without wiping data; emulator-5554 still has code228, matching the prior APK hash AB0AAA1EA0A6DE8C79678A5A9D8F82413338935EA8491F03CC25C908CD38E3F0.

## V3 — pending

Owner D-G1b authorizes G1-20261003-ecbf8c55, starting fx-a03, within USD 1.00 minus USD 0.01296985 already spent. No retry/repair; UNKNOWN or L1 refusal stops the group; stop before G2. The current chat lacks the expected owner fingerprint value; prior private preflight files contain redacted results only. Requested the fingerprint or its private file path while V1–V2 continue; no API key requested/read.

Next action: complete V2 on emulator-5554.
