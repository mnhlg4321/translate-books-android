# v4.20 E1–E9 — RC 4.20.0, owner 017 reading pending

Date: 2026-10-13. Authority: docs/V420_E_RELEASE_WORK_REQUEST.md section2. Start feature/v4.20 at 08df3a78248d4a8badd263a9f1734aa2b83004e7, clean. Only emulator-5554 / tbl-code113-dqa-api35 used; no phone, clear, wipe, uninstall, tag or merge. Shipped app remains v4.18.0. Book text, prompts, responses and owner adjudications remain outside Git.

## E1 — exact RC-source clean archive

RC source e989653bf645c46e5e6a155b2e3f5049c1c40702 (E2 release notes only; production identical to08df3a78). Clean git archive at D:/P5E-builds/verify-v420-e-e989653b. JDK21 JetBrains21.0.10; Gradle9.3.0, offline. Commands:

```
gradlew.bat :editorial-engine:test :app:test :app:lintDebug :app:compileDebugAndroidTestJavaWithJavac --offline --console=plain
python -m unittest discover -s scripts/p6 -p test_*.py
python -m unittest discover -s scripts/chunk -p test_*.py
```

| Group | PASS methods | SKIP | FAIL |
|---|---:|---:|---:|
| engine |714|1|0|
| app debug |470|3|0|
| app release |470|3|0|
| app benchmark |470|3|0|
| scripts/p6 |106|0|0|
| scripts/chunk |24|0|0|

AndroidTest compile PASS; lint0 errors/57 warnings. Raw host XML/binary evidence retained privately at D:/P5E-private/v420-e-host-evidence with private backup; only method counts/hashes public. Integration-test stdout contains pre-existing book fixture text, so raw host reports were removed from the unpublished QA commit before any push. Initial environment selected Java8; preflight/Gradle failed before running tests. Resolved by explicitly selecting installed JDK21, without source change. The preliminary08df3a78 archive and exact e989653b archive both passed; the latter is the authoritative E1 result.

## E2/E3 — release metadata and immutable RC/test payloads

E2 commit e989653b: C1, C2', CP-IMPL-17 migration (new runs required; old results view/export retained), unflagged self-address/translation/narration limits.

Production wrapper: scripts/build-and-save.ps1 -ExactReleaseVersion4.20.0 -MinimumVersionCode269 -JavaHome <installed JDK21> -DeviceSerial emulator-5554 -Offline. Throw-away local feature/v4.20.0 at e989653b, never pushed. Event build-20261013-165726, version4.20.0/code269. APK SHA256 7008057CCC10F33A889FCF58C1A8C9DFFCCCA50A27F564C7A575D7E8594858DA; source ZIP833F0D7C88C710FFCA6EAABEF7262D716E31B720BFB689D400999C3431F72096; cert47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155.

Initial AndroidTest event v420-e-test-20261013-1658 at e989653b, APK3C61FA67B236B98FE0A738966DB085A8B02A11859DA72BA03A73E686F7DBB2E8. Repaired fixture commit74706f8dce8abb312b6c41acf9a34ee955698a94: only AndroidTest data/expected reason changed. No production/build-input diff from RC. Repaired test event v420-e-test-repair-20261013-1704, APKCC7EFF9FDD165B24C81C2DD6806E9D8103FD33A920CA696BC18BFD5DF1ABA644, source ZIP495728078C2B6C61D6F9EE2D7A21A74BEEF0CBE125607DD15DC494BE3B456E7A. Both test builds use scripts/build-and-save-android-test.ps1, pin production APK/source ZIP/cert, never rebuild production.

Every production/test payload was byte-checked in D:/App Translate Books/artifacts/builds|test-builds/v4.20.0/<event> and backup/builds|test-builds before install, with worktree mirrors. Temporary worktrees are build infrastructure, not another release track.

## E4 — RC device QA, fake provider

Validated install/readback4.20.0/code269, signature tokenabebea4b. Fixture glossary residue check0; owner data not cleared. Same frozen19/chunk6/product6 groups as4.18 RC, plus6 restart phases. scripts/qa/instrumentation_summary.py parses each method, not the closing OK count:

| Group | PASS | SKIP | FAIL |
|---|---:|---:|---:|
| frozen allowlist |116|1|0|
| chunk |21|6|0|
| product/Translation |15|0|0|
| restart seed/verify/cleanup |6|0|0|
| effective total |158|7|0|

Frozen SKIP is seedMainDatabaseForEmulatorUiSmoke (opt-in, not selected). Six chunk phase SKIPs run separately with explicit seed/verify/cleanup, all6 PASS; shell force-stops between seed and verify. Reopen providers assert if called. SQLite store/reopen/export exercised; this does not claim clicking Android's system picker (tests use controller callbacks/file URI).

FAILED_REPAIRING: first EditorialPairAddressKeepSqlite failed with ADDRESS_ROW_NOT_CITED vs obsolete ADDRESS_FORM_NOT_IN_APPLICABLE_ROW. Synthetic responses lacked R1. Repair aligns them with already-passing host test, expects ADDRESS_FORM_NOT_IN_CITED_ROW, keeps all candidate/export/reopen assertions. Original failed log retained, repaired log separately named; final effective summary excludes the failed attempt but does not erase it. First test-wrapper attempts refused untracked QA files/detached HEAD; successful repaired test build used a clean temporary unpushed local branch. No acceptance reduction or production change.

## E5 — new actual emulator performance

Benchmark app and Macrobenchmark APK built from e989653b via :app:assembleBenchmark :macrobenchmark:assembleBenchmark, retained with source ZIP in performance/build and backup before install. Only emulator5554, same cert/code269; RC restored afterwards using validated installer. One initial command failed from PowerShell argument parsing, corrected with an argument array; successful build evidence is the retained APK/source.

- Cold startup5 iterations: min978.4639 / median1015.4787 / max1120.1162 ms.
- Editorial tab5 iterations: frame CPU P50 15.33755 / P90 19.9708 / P95 20.4615 / P99 20.85406 ms; frame overrun P50 4.048686 ms.
- Synthetic1222 units/53 chunks: best-of-three planning458 ms, verdictOK; performance assertion under1000 ms PASS.
- Macrobenchmark2 methods PASS, planning1 method PASS. Ten new iteration traces plus v420-e-plan.pftrace retained, benchmarkData.json and raw instrument logs retained.
- These are emulator numbers; older4.18 median727.5667 ms/P50 10.08515 ms are lower, not reused here and not a controlled causality comparison. No new acceptance threshold introduced.

## E6 — RC media

EditorialR1MediaWalkthroughInstrumentedTest r1_media=run PASS: six screenshots visually inspected, walkthrough.mp4 retained (50-second screenrecord). Synthetic only, fake provider. Original main database and journal temporarily set aside without clearing; restored SHA256 hashes match. An initial date-quoting error occurred before the test; originals restored in finally, corrected private script then produced all6 stages. Media demonstrates controller flow/export readback, not system-picker interaction or model quality.

## E7/E8 — sole real017 and owner reading

Private runner copy run_v420_e_chunk.ps1, only017, V420-E group, noRetry YES, maxCalls10, capUSD0.08. Original scripts/owner files unchanged. Independent expected fingerprint read privately from existing holdout source, never printed; live runner must pass MATCH before constructing/dispatching provider. Pricing and reservations accepted by live service, no unknown-cost/empty/length/infra/refusal/overrun.

- Offline017-c1b11647-0f34-4bf1-96a8-01ff16a84c1a:11 chunks,10 fake/0 real, final byte-equal DRAFT SHA b92c8bcd3148e76717e69814e7cde8aa077b797be7bfe33498d2b7e938cb0922. Fake metadata USD0.040 is simulated accounting, not real spend.
- Sole live017-3aaa5f6b-8dff-43ad-a7dd-35d7612debd3 on RC4.20.0/code269/e989653b/CP-IMPL-17, openai/gpt-5.6-luna medium:11/11 chunks ACCEPTED, FINAL_ELIGIBLE, structural.valid=true.10 calls/0retry/0refused;56885 input/8044 output; all10 finish stop.
- Ledger V420-E-017-20261013-3aaa5f6b:10 RESERVE/10 SETTLE,20 entries, hash chain verified, pending0/overrun0, USD0.02387255. Unused cap grants no second run.
- Private E8 D:/P5E-private/chunk-runs/V420-E/READING_017.html:17 rows/selects/notes, union13 actually changed +8 B lines/12 reference substitutions (overlap4);9 changed outside B;3 cậu↔name lines in separate group. Preliminary B3 reference-word matches/4 unchanged/1 other (one partial match); C2 format-KEPT0; NOT_REACHED0. These are NOT correct/missed/wrong adjudications. Owner decision pending.
- Rendering initially refused misalignment:017 has two nonblank separator-only units omitted by014's letter-only helper. Private copy now uses app LineUnits nonblank units, maps B's letter-only indices explicitly, asserts point/DRAFT alignment and bead totals; no runtime/source/owner-file edits. Output encoding corrected locally. Raw26 files/prompts/responses/final/ledger remain private, hashes only in SUMMARY.
- Live runner is InMemoryPairRunStore; device fake QA separately proves SQLite/reopen/export, not persistence of this real017 run.

## E9 — handoff before tag/merge

Checklist05–09 prepared with concrete evidence; PreTag gate and immutable build-event archive verified in the final handoff. Archive GitRef is exact RC e989653b; repaired test-source ZIP is supplemental. Release tag/merge and semantic acceptance remain owner-only after reading017. No production changes after observing017, no v4.19 work, no additional run.

Next action: owner reads READING_017.html and decides tag/merge4.20.0.
