# G2-R2 Global Regression and B1 Requalification

## Review-stop result

- `G2_R0_ROOT_CAUSE_AUDIT: PASS`
- `G2_R1_MINIMAL_FIX: PASS`
- `G2_R2_GLOBAL_CONNECTED_SUITE: PASS`
- `B1_FOCUSED_COORDINATOR: PASS`
- `G2-C1B1C2B1_COORDINATOR: PASS`
- `PRODUCTION_RUN_CONTEXT_CLOSED_EVENT: ABSENT`
- `PRODUCTION_CALLER_OWNER: UNRESOLVED`
- `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`
- `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`
- `SAFE4_EXECUTION_READINESS: BLOCKED`
- `REAL_DATA_CONTINUITY: NOT_CLAIMED`

No production caller, closure event, production identity row, lineage row, binding row, schema migration, profile/catalog change or capability promotion was added.

## R2 archive and provenance

Archive-first `scripts/build-and-save.ps1 -Series 4.16-dev -JavaHome "C:\\Program Files\\Android\\Android Studio\\jbr"` ran from R1 commit `8e0893438e51da2abe28dde447ae6450bb891901`:

- version: `4.16-dev.48`, code110;
- event: `build-20260806-093920`;
- APK SHA-256: `B7E07C945602CF65572702DDF19579961CBA0070024DB9C5FFF89585C825F2C3`;
- source ZIP SHA-256: `E5E927A5C7A32501F11555BB3A74A633A76DE4218A58DF8F2C2E9B5551759B23`;
- artifact: `artifacts/builds/v4.16-dev.48/build-20260806-093920/`;
- backup: `backup/builds/v4.16-dev.48/build-20260806-093920/`;
- BUILD_INFO records Gradle 9.3.0, AGP 8.7.3, JBR/JDK 21.0.10, Java source/target 17, compile/target SDK 35 and Git provenance;
- all five payload hashes match artifact and backup; the archived SHA256 manifest matches the payloads.

The exact archived APK was installed by `adb install -r`; no `pm clear` was used. UTP installed and removed its test package during the connected run; the exact archive and current instrumentation APK were reinstalled for the final direct B1 check.

## Regression matrix

### Wrapper and static gates

- JDK/toolchain preflight: PASS, JBR/OpenJDK `21.0.10`, JDK major 21.
- `:editorial-engine:test`: 105/105, 0 failures/errors/skips.
- `:app:testDebugUnitTest`: 164/164, 0 failures/errors/skips.
- Combined JVM: 269/269, 0 failures/errors/skips.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS.
- `:app:lintDebug`: 0 errors, 53 warnings.
- `git diff --check`: PASS.

### Connected suite

The UTP connected suite was run twice with version properties matching the archive on OnePlus CPH2691 / Android 15. Both runs started 82 tests, finished without assertion failures or errors, and reported the one approved opt-in real-API skip. The final XML reports:

- `EXPECTED_CONNECTED_TESTS: 82`
- `RECEIVED_CONNECTED_TESTS: 82`
- `CONNECTED_FAILURES: 0`
- `CONNECTED_ERRORS: 0`
- `CONNECTED_SKIPS: 1` (approved `V48RealApiPromptInstrumentedTest` assumption)
- runner completion: `BUILD SUCCESSFUL`, no runner abort.

The final XML contains and passes:

- `EditorialPackImportServiceInstrumentedTest`: 13/13;
- `EditorialPackManagementPageInstrumentedTest`: 3/3;
- `EditorialRunContextCoordinatorInstrumentedTest`: 7/7;
- the two previously failing ZIP cases with their corrected semantic codes;
- `detailRebuildAndActivityRecreationDoNotExposeMutationControls`.

The UTP console's intermediate `83/82` display counts the approved skip in its progress line; the authoritative XML summary is 82 tests with 1 skipped, 0 failures and 0 errors.

### B1 requalification

After UTP cleanup, the exact code110 app archive and instrumentation APK were reinstalled by upgrade. Direct `EditorialRunContextCoordinatorInstrumentedTest` returned `OK (7 tests)`. This confirms the R1 importer fix did not alter the B1 coordinator boundary, Option-B recovery behavior, production resolver fail-closed path, or focused B1 fixture behavior.

## Safety and boundary checks

- SQLite source remains v17; no migration v18 was created.
- `EditorialSafe4Pack.executionEnabled()` remains false.
- Production resolver remains absent/fail-closed; no production `RUN_CONTEXT_CLOSED` event exists.
- No MainActivity, startup, ZIP importer caller, project preparation or UI caller was connected to B1.
- No production `DATA_COMPATIBLE`, closed-run, lineage or binding row was created by R0/R1/R2.
- Existing device data and prior session/test cache are present; no continuity claim is made. All importer/B1 positive fixtures used isolated/random databases.
- `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` remain untouched by this work, unstaged and uncommitted.

## Rollback and review stop

R2 changes are documentation/state/handoff only after the R1 source commit. The importer fix can be reverted as the R1 source group if a later review rejects it; no schema or production-data rollback is needed. The R2 archive is immutable evidence and must not be overwritten.

Exactly one next step is permitted: review/approve this R0→R1→R2 regression handoff. The next implementation phase, if separately approved, is not started here. No C2-B2 production lifecycle, retention UX, capability promotion, certification, activation, project binding or execution may begin automatically.

