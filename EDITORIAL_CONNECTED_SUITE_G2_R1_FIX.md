# G2-R1 Minimal Regression Fix

## Gate and scope

- R0 evidence commit: `93ac1f46f0fd637239287968fb821f091929c598`.
- R0 result: PASS; both ZIP failures were proven importer defects; safe minimal fix identified.
- R1 implementation is limited to `EditorialPackImportService` and its focused instrumentation fixture matrix.
- No B1 coordinator, schema, profile/catalog, UI lifecycle, importer caller, production lifecycle, capability or execution boundary changed.

## Root causes addressed

### Traversal/absolute/alternate separator paths

The old ZIP path check occurred only after the complete ZIP stream had been read and converted into `EditorialPackImportEntry` objects. Android 15 can throw while processing the dangerous local header before that later check, so the generic ZIP `IOException` was incorrectly returned as `TRUNCATED_STREAM`.

The fix now normalizes and rejects every entry name immediately after `getNextEntry()` and before reading entry bytes. If Android throws before returning the entry, the bounded probe inspects only the local-header metadata already observed and maps an invalid local path to `INVALID_ENTRY_PATH`. No content, URI/path or database row is logged or persisted by the probe.

### Truncated local/central/end structures

The old implementation treated Android's null entry result for the three-byte prefix `50 4b 03` as an empty ZIP and returned `ENTRY_COUNT_LIMIT`. The fix keeps bounded tail metadata on the same single-pass stream and requires a complete end-of-central-directory record before accepting the ZIP stream. A valid empty archive remains `ENTRY_COUNT_LIMIT`; a short local header or missing central/end record is `TRUNCATED_STREAM`.

The existing entry-count, file-size, total-size, compression-ratio, symlink and registry-availability rules are unchanged. `importPack(...)` is still reached only after the ZIP stream passes these checks, so all failure paths remain zero-write at the available-pack boundary.

## Implementation files

- `app/src/main/java/com/ml/tblandroidtxt/EditorialPackImportService.java`
  - early canonical path validation;
  - bounded `ZipStructureProbe` for end-record and local-header classification;
  - no new table, column, migration, caller or production event.
- `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialPackImportServiceInstrumentedTest.java`
  - path variants: `../`, backslash traversal, absolute `/`, and drive-qualified path;
  - valid empty ZIP versus truncated central-directory structure;
  - existing traversal, extra-entry, truncated-prefix, and zero-row assertions.

The management-page test was not changed because isolated method/class/group runs and both full direct runner replays passed it without reproducing the historical empty-body abort.

## R1 verification

### Focused device tests

Exact archived app APK `4.16-dev.47`/code109 was installed with `adb install -r`; no `pm clear` was used. A separately packaged instrumentation APK from the current source was installed.

| Scope | Result |
|---|---:|
| `EditorialPackImportServiceInstrumentedTest` | 13/13 PASS |
| `EditorialPackManagementPageInstrumentedTest` | 3/3 PASS |
| `EditorialRunContextCoordinatorInstrumentedTest` | 7/7 PASS |
| Fatal/ANR/process-death markers in focused runs | none observed; `adb get-state=device` |

The device database and cache contain pre-existing session/test data from earlier work. R1 used isolated random databases for importer and B1 fixtures and does not claim real-data continuity.

### Wrapper/toolchain regression

- JDK preflight: PASS, JetBrains JBR/OpenJDK `21.0.10`, JDK major 21 policy.
- Gradle Wrapper: `9.3.0`; AGP: `8.7.3`; compile/target SDK: 35; Java source/target: 17.
- `:editorial-engine:test`: 105/105, 0 failures/errors/skips.
- `:app:testDebugUnitTest`: 164/164, 0 failures/errors/skips.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS.
- `:app:lintDebug`: 0 errors, 53 warnings.
- `git diff --check`: PASS.

### Archive provenance

Archive-first `scripts/build-and-save.ps1 -Series 4.16-dev -JavaHome "C:\\Program Files\\Android\\Android Studio\\jbr"` passed:

- version: `4.16-dev.47`, code109;
- event: `build-20260806-093433`;
- APK SHA-256: `A16F1D4B9D2DAFCF16E9FC038878BD4C58D8D3F46A2666BAF4C5091E4C0D0567`;
- source ZIP SHA-256: `1C809D7FFF64E660E1C0626404772C3AEBF2384A5DC81AC7731FA46FEB4AA518`;
- artifact: `artifacts/builds/v4.16-dev.47/build-20260806-093433/`;
- backup: `backup/builds/v4.16-dev.47/build-20260806-093433/`;
- all five archived payload hashes matched artifact/backup parity.

## Gate result

`G2_R1_MINIMAL_FIX: PASS`

R2 may proceed to the required archive/device/global regression. R2 must still stop with B1 `BLOCKED` if the full connected suite does not complete with the expected count and no unapproved failures/errors.

