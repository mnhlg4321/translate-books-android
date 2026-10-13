# v4.20 E-bis — rebuilt RC 4.20.0/code270, CP-IMPL-18

Authority: docs/V420_E_RELEASE_WORK_REQUEST.md section8, owner conditional tag/merge approval2026-10-13. Start feature/v4.20 at261d3b8bc28ad2f8f22c4b6ffd326601b4abc8d7, clean. Production unchanged during E-bis. Only emulator-5554 (tbl-code113-dqa-api35, API35); no phone, clear/wipe/uninstall, no paid provider request. Earlier014/017 evidence and reading pages unchanged; no acceptance relabel or rerun.

## Exact checked production source and retention

- Implementation fix13e12a65 is present; production/build source261d3b8b, contractCP-IMPL-18.
- Production scripts/build-and-save.ps1 -ExactReleaseVersion4.20.0 -MinimumVersionCode270 -JavaHome installedJDK21 -DeviceSerial emulator-5554 -Offline, in temporary unpushed localfeature/v4.20.0 at261d3b8b.
- Production eventbuild-20261013-181401; version4.20.0/code270, APK58C1B11FA8DEA27D57BD9830A47007A7757F9F04147A0C3EAAB43F3FD199AFA8; exact tracked-sourceZIPAED3CC03FF574126D5AFC0DDB0B833224C66EDA2E3C562B0FACFDE11632359C9.
- Test scripts/build-and-save-android-test.ps1, eventv420-ebis-test-20261013-1815, exactsource261d3b8b, APKCC7EFF9FDD165B24C81C2DD6806E9D8103FD33A920CA696BC18BFD5DF1ABA644; pinned productionAPK/sourceZIP/cert47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155. Production not rebuilt by the test wrapper.
- Every production/test payload includes APK/README/BUILD_INFO/SHA256SUMS/exactsourceZIP and was byte-verified in D:/App Translate Books/artifacts/builds|test-builds/v4.20.0/<event> and backup/builds|test-builds before install; worktree mirrors retained. Existing code269 events remain immutable/superseded.
- scripts/install-validated.ps1 preflight/install/readback PASS, signer tokenabebea4b; final RC restored after benchmark install. No change to owner workspace branch/checkout/files outside permitted artifact/backup roots.

## E1 — clean git archive of261d3b8b

JDK21.0.10, Gradle9.3.0, offline. Exact archive D:/P5E-builds/verify-v420-ebis-261d3b8b. Commands:

```
gradlew.bat :editorial-engine:test :app:test :app:lintDebug :app:compileDebugAndroidTestJavaWithJavac --offline --console=plain
python -m unittest discover -s scripts/p6 -p test_*.py
python -m unittest discover -s scripts/chunk -p test_*.py
```

| Group | PASS methods | SKIP | FAIL |
|---|---:|---:|---:|
| engine |720|1|0|
| app debug |470|3|0|
| app release |470|3|0|
| app benchmark |470|3|0|
| scripts/p6 |106|0|0|
| scripts/chunk |24|0|0|

Lint0errors/57warnings; AndroidTest compile PASS. Raw host XML/binary/stdout retained privately at D:/P5E-private/v420-ebis-host-evidence because integration-test stdout includes existing book fixtures; only counts/hashes public. New wrapper tests/lint/build also PASS. No local repair or regression failure during E-bis.

## E4 — fresh QA on the new RC, fake provider only

Fixture glossary residue0 before run. Same frozen19/chunk6/product6 classes as previous release QA, plus6 seed/verify/cleanup phases; no reuse of old PASS logs. scripts/qa/instrumentation_summary.py counts method statuses:

| Group | PASS | SKIP | FAIL |
|---|---:|---:|---:|
| frozen allowlist |116|1|0|
| chunk |21|6|0|
| product/Translation |15|0|0|
| process-death restart phases |6|0|0|
| total |158|7|0|

165 discovered,0INCOMPLETE; no failed attempt/rerun. One frozen opt-in seed method not selected; six phase methods skip in normal class runs and PASS separately with explicit phase arguments. App force-stopped between seed and verify, reopen provider fails if called. SQLite persistence/reopen/export PASS with synthetic data. Controller/file-URI export tested; Android system-picker interaction NOT_MEASURED. No live SQLite persistence claim for historical014/017 (their runner used in-memory store).

## E5 — new actual emulator measurements

Same production source261d3b8b. :app:assembleBenchmark :macrobenchmark:assembleBenchmark, version4.20.0-ebis-benchmark/code270, validated signer; APKs and exactsourceZIP copied to performance/build and backup before install. Benchmark APK SHA C01488D2758DB92BDEC85664437630AA3333F4A25E82DF256F6E2FEB73D8494B. Ten new iteration traces retained, plus benchmarkData.json and original instrumentation log. RC270 restored before synthetic planning/media.

- Macrobenchmark2 methods PASS,5 iterations each: cold-start median1087.8085ms; Editorial tab frame CPU P50 12.10205ms.
- RC synthetic1222 units/53chunks, verdictOK: best-of-three planning465ms; under1000ms assertionPASS. New15-second Perfetto v420-ebis-plan.pftrace retained.
- Eleven real traces total, no fabricated/reused performance output. Emulator numbers only; not physical-device measurements, not a controlled attribution comparison with earlier releases.

## E6 — media on RC270

EditorialR1MediaWalkthroughInstrumentedTest r1_media=run PASS. Six screenshots visually inspected; walkthrough.mp4 retained. Synthetic job/glossary/fake provider only. Original main database and journal temporarily set aside without clearing, restored hashes identical (database-preservation.json). Media shows controller flow/export readback, not semantic quality or system-picker interaction.

## Preserved product evidence, spend and limits

- Previous reports: artifacts/qa/v4.20-d-014-20261013 and artifacts/qa/v4.20-e-20261013. Private pages D:/P5E-private/chunk-runs/V420-D/READING_014.html and V420-E/READING_017.html, original hashes checked unchanged.
- Owner014accepted37 reviewed rows and3extra chunk13 replay lines; owner01716correct/1headingwrong, no self-address wrong edit. Heading/glossary error fixed deterministically in13e12a65; prior offline stored-response evidence records unchanged non-heading chunks6/6 for014 and9/9 for017. No new paid semantic evaluation underCP-IMPL-18: the owner-approved section8 deterministic-fix route expressly prohibits a new paid run, not silently inferred.
- E-bis0realcalls/USD0. Existing totalv4.20USD0.04188915 (0140.01801660 +0170.02387255), unchanged. Private owner book text/FINAL/adjudication/fingerprint never copied to runtime or public reports.
- Known limits remain: finder-unflagged self-address, translation-stage pronoun errors,004narration, ADDRESS responses omitting row numbers (discarded,5in017). New discoveries deferred after4.20.0 except regressions, per owner scope.

## Section8 step2 release conditions

E1/E4: no FAIL/unexplained failure; RC/test retained and mirrored before install; E5/E6 real evidence retained; exact production source261d3b8b checked and unchanged. PreTag/build-event archive verification is recorded in the subsequent release closure; tag/merge occurs only after these final checks PASS. QA-only tag source may include these reports, but archive helper must prove all production inputs identical to261d3b8b and retain its exact source ZIP separately via ProductionGitRef.
