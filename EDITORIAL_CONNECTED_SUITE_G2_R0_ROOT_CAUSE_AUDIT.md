# G2-R0 Connected Suite Root-Cause Audit

## Review boundary

- Branch: `feature/v4.16-g2-c1b1c2b1-regression`
- Baseline HEAD before this audit: `0834ec8f1b5ce8557ea0adfe6a1f45005d13e591`
- SQLite source version: v17
- Exact available archive: `4.16-dev.45`, code107
- APK SHA-256: `F0A8971AA9E8A4D08CF08A63857BC37F17BD6DEFE264CE4F2813DAAF8A1211A9`
- Device: OnePlus CPH2691, Android 15, serial `15e84958`
- Protected `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` were not changed by this work.
- No source, importer, test expectation, database or profile change was made during R0.

## Reproduction matrix

| Probe | Result | Evidence |
|---|---:|---|
| `EditorialPackImportServiceInstrumentedTest#zipTraversalAndExtraEntriesAreRejectedBeforeRegistryAvailability` | 1 test, 1 assertion failure | Actual `TRUNCATED_STREAM`; no row assertion was reached because traversal assertion failed first. |
| `EditorialPackImportServiceInstrumentedTest#truncatedZipDoesNotCreatePackRow` | 1 test, 1 assertion failure | Actual `ENTRY_COUNT_LIMIT`; the fixture is the three bytes `50 4b 03`. |
| `EditorialPackImportServiceInstrumentedTest` | 11 tests, 2 failures, 0 errors | Only the two ZIP classification mismatches failed. |
| `EditorialPackManagementPageInstrumentedTest#detailRebuildAndActivityRecreationDoNotExposeMutationControls` | 1/1 PASS | Direct runner, fresh app process. |
| `EditorialPackManagementPageInstrumentedTest` | 3/3 PASS | All scenarios close with try-with-resources. |
| Import + management classes, same runner invocation | 14 tests, 2 failures, 0 errors | Both management tests completed; only the two ZIP failures remained. |
| Full direct instrumentation, run 1 | 80 tests, 2 failures, 0 errors, 1 approved real-API skip | Runner returned `INSTRUMENTATION_CODE: -1`; no management failure. |
| Full direct instrumentation, run 2 | 80 tests, 2 failures, 0 errors, 1 approved real-API skip | Same two ZIP failures; no abort, crash or management failure. |

The historical B1 run that reported `38/80` with an empty management failure body was not an assertion result. It was not reproduced by the isolated method, full management class, ordered import+management invocation, or either full direct runner replay. The historical artifact did not retain a crash stack, ANR record, device disconnect, or assertion body. At the requested classification level this is `RUNNER_DEVICE_ENVIRONMENT`; it is not evidence of a management-page production defect.

The Gradle connected task was also run as a precondition probe. It did not execute tests because the stale generated debug APK was code48 and the device held code107; UTP returned `INSTALL_FAILED_VERSION_DOWNGRADE`. This installer failure is separate from the historical suite abort. The exact code107 APK was then restored by `adb install -r` without `pm clear` before direct runner probes.

## ZIP traversal audit

The traversal fixture is structurally complete by construction:

1. `zipWithNames(...)` uses `ZipOutputStream`.
2. It calls `putNextEntry("../editorial-pack.json")`, writes the manifest, closes every entry, and exits try-with-resources, which closes the central directory and end record.
3. No truncating or mutating stream is used for this test.
4. The same fixture helper writes the remaining three root entries. The test's expected semantic result is therefore `INVALID_ENTRY_PATH`, not `TRUNCATED_STREAM`.

The importer defect is visible in `EditorialPackImportService.importZip(...)`:

- It checks directory, symlink and entry-count conditions.
- It reads the entire entry stream before validating the entry name.
- `EditorialPackStorageLayout.normalizeEntryPath(...)` is only reached later through `importPack(entries)`.
- On Android 15, the platform ZIP reader rejects/throws while processing the dangerous traversal fixture before the later normalization step. The catch-all ZIP `IOException` maps that platform signal to `TRUNCATED_STREAM`.

Thus the path-security check is ordered too late. The safe R1 fix is to normalize/reject the entry name immediately after `getNextEntry()` and before reading bytes, preserving the single-pass stream, all size/count/ratio limits, and zero-row behavior. This does not trust a registry row or write any row.

Classification:

`ZIP_TRAVERSAL_FAILURE_CLASS: IMPORTER_DEFECT`

## ZIP truncation audit

The test fixture is exactly `new byte[]{80, 75, 3}`, a three-byte prefix of a ZIP local-file-header signature. It contains neither a complete local header nor a central-directory/end record. It is therefore a malformed/truncated ZIP, not a valid empty archive.

The current importer loop treats the Android 15 behavior as follows:

- `ZipInputStream.getNextEntry()` returns no entry for this short prefix without surfacing an `IOException` to the caller.
- `entries` remains empty.
- The post-loop `entries.isEmpty()` branch returns `ENTRY_COUNT_LIMIT`.
- `importPack(...)` is never called, so no pack or registry row is created.

This explains the observed precedence without indicating an entry-count limit was actually exceeded. A valid empty ZIP must remain distinguishable from a short malformed prefix. The safe R1 fix is a small single-pass structural probe around the same input stream: retain only bounded stream metadata needed to recognize a complete end-of-central-directory record when no entries were observed; map a short/malformed no-entry stream to `TRUNCATED_STREAM`, while a structurally complete empty archive remains `ENTRY_COUNT_LIMIT`. No content, URI, path or row is retained by the probe.

Classification:

`ZIP_TRUNCATION_FAILURE_CLASS: IMPORTER_DEFECT`

## Zero-write and security boundary

Both ZIP failures return from `importZip(...)` before `importPack(...)`. The direct class and ordered-group runs therefore exercised the zero-available-row boundary; the traversal test's row assertion was not reached after the first assertion failure, while the existing malformed-stream code path itself never calls the registry. R1 must add explicit zero-row assertions for each focused ZIP failure path and must not weaken any limit.

## Management/incomplete-suite audit

The management test uses `try (ActivityScenario<MainActivity> ...)` for both activities. No scenario leak, missing cleanup, database dependency, startup/import call, or read-only assertion defect was found in source review. The method passed alone, the class passed 3/3, the ordered import+management run completed all 14 tests, and two full direct runs completed all 80 tests.

Controlled post-run observations:

- `adb get-state`: `device`.
- No `FATAL EXCEPTION`, `am_crash`, `am_anr`, `ANR`, `Force finishing` or process-death marker was emitted by the controlled direct runs.
- The runner returned a complete JUnit summary on both full runs.
- No management assertion failure was reported in either full run.

The historical empty failure body therefore cannot be used as an app assertion. Its low-level subcause (UTP host abort, instrumentation-process interruption, or transient OEM/device scheduling) was not retained, but the only defensible allowed class after controlled isolation is:

`MANAGEMENT_SUITE_ABORT_CLASS: RUNNER_DEVICE_ENVIRONMENT`

No management lifecycle fix is identified or authorized in R1. No retry, timeout increase, ignore, assertion removal, or test-count reduction is allowed.

## Gate result

`G2_R0_ROOT_CAUSE_AUDIT: PASS`

`SAFE_MINIMAL_FIX_IDENTIFIED: YES`

R1 may address only the two proven importer classification/order defects and their focused regression matrix. R1 must not change B1 coordinator code, UI lifecycle, schema, profile, capability, caller wiring, production rows or execution state.

