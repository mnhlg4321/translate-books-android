# Build State

## Current G2-R2 regression review stop

- Review branch: `feature/v4.16-g2-c1b1c2b1-regression`; R1 implementation commit `8e0893438e51da2abe28dde447ae6450bb891901`; R2 documentation/handoff is the final group on this branch.
- R2 archive-first build: `4.16-dev.48`/code110, event `build-20260806-093920`, APK SHA-256 `B7E07C945602CF65572702DDF19579961CBA0070024DB9C5FFF89585C825F2C3`, source ZIP SHA-256 `E5E927A5C7A32501F11555BB3A74A633A76DE4218A58DF8F2C2E9B5551759B23`; artifact/backup five-file parity and manifest hashes PASS.
- R2 regression: UTP connected suite completed twice, each XML `82` tests with `0` failures, `0` errors and `1` approved real-API skip. Import `13/13`, management `3/3`, B1 `7/7` are present and pass. Post-commit engine `105/105`, app JVM `164/164`, Android-test compilation, lint `0` errors/`53` warnings, toolchain preflight and `git diff --check` pass.
- B1 requalification: `G2-C1B1C2B1_COORDINATOR: PASS`; implementation boundary is unchanged. Production closure event remains absent, caller owner unresolved, runtime wiring/capability promotion not started, and SAFE4 execution remains blocked.
- Device boundary: exact code110 was installed with `adb install -r`, no `pm clear`; isolated fixtures used random databases; existing device data/cache are not claimed as real-data continuity. `REAL_DATA_CONTINUITY: NOT_CLAIMED`.
- Exact next step: review/approve the R0→R1→R2 handoff. Do not start C2-B2, production lifecycle, retention UX, capability promotion, certification, activation, project binding or execution.

## Current G2-R1 regression review

- Review branch: `feature/v4.16-g2-c1b1c2b1-regression`; implementation baseline before the R1 evidence/implementation commit is `93ac1f46f0fd637239287968fb821f091929c598`.
- R1 source fix is importer-only: entry paths are rejected before entry-byte processing; bounded same-stream ZIP metadata distinguishes valid empty archives from missing local/central/end records and maps Android pre-entry path exceptions to `INVALID_ENTRY_PATH`. SQLite remains v17; no schema/profile/capability/caller/lifecycle change.
- R1 archive-first build: `4.16-dev.47`/code109, event `build-20260806-093433`, APK SHA-256 `A16F1D4B9D2DAFCF16E9FC038878BD4C58D8D3F46A2666BAF4C5091E4C0D0567`, source ZIP SHA-256 `1C809D7FFF64E660E1C0626404772C3AEBF2384A5DC81AC7731FA46FEB4AA518`; artifact/backup five-file parity PASS.
- R1 verification: engine `105/105`, app JVM `164/164`, 0 failures/errors/skips; Android-test compilation PASS; lint `0` errors/`53` warnings; `git diff --check` PASS; device import `13/13`, management `3/3`, B1 focused `7/7` PASS.
- Device boundary: exact code109 installed with `adb install -r`, no `pm clear`; importer/B1 focused fixtures used isolated random databases. Existing device data means `REAL_DATA_CONTINUITY: NOT_CLAIMED`.
- R1 status: PASS. Exact next step is a fresh post-commit R2 archive-first global connected regression and B1 requalification. If global suite is incomplete or any unapproved failure remains, stop with B1 BLOCKED.

## Current G2-R0 regression review

- Review branch: `feature/v4.16-g2-c1b1c2b1-regression`; implementation baseline before the R0 documentation commit: `0834ec8f1b5ce8557ea0adfe6a1f45005d13e591`.
- SQLite remains v17. The exact prior archive remains `4.16-dev.45`/code107, APK SHA-256 `F0A8971AA9E8A4D08CF08A63857BC37F17BD6DEFE264CE4F2813DAAF8A1211A9`; no new APK was built in R0.
- R0 evidence: isolated traversal `1/1` reproduces `INVALID_ENTRY_PATH` versus `TRUNCATED_STREAM`; isolated truncation `1/1` reproduces `TRUNCATED_STREAM` versus `ENTRY_COUNT_LIMIT`; import class `11` tests has only those 2 failures; management class is `3/3`; import+management is `14` tests with only those 2 failures; two full direct runners each complete `80` tests with `2` failures, `0` errors and `1` approved real-API skip.
- Root causes: both ZIP mismatches are importer ordering/structural-classification defects. The historical management empty-body abort is classified `RUNNER_DEVICE_ENVIRONMENT` after controlled isolation and is not a production UI defect. R0 PASS; a minimal importer-only R1 fix is identified.
- Boundaries unchanged: no B1 coordinator change, no schema/profile/catalog/importer caller or production lifecycle wiring, no production row, no capability promotion/certification/activation/execution; `executionEnabled()` remains false. Protected `.idea/*` remains user-owned and unstaged.
- Exact next step: commit R0 evidence, then implement/test only the proven ZIP importer fix in G2-R1. Stop if the fix needs schema/profile/lifecycle/security expansion.

## Current release build

- versionName: `4.15`
- versionCode: `62`
- event: `build-20260729-070042`
- branch: `feature/v4.15`
- source commit: `5253f003b0f7898b1755e98783ec91b01790e1d4`
- APK: `artifacts/releases/v4.15/tag-v4.15/TranslateBooks-v4.15-code62.apk`
- immutable local mirror: `backup/v4.15/tag-v4.15/`
- APK SHA-256: `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`
- source ZIP SHA-256: `0F14121D69314C36CF6F92E80084881D32AA8E6BA35ACB165203D52161B2F433`
- regression: approved-logo guard and 110 JVM tests passed; Android Lint completed with 53 warnings and 0 errors; exact-code62 connected instrumentation passed 15 cases with 1 explicitly opt-in paid real-API case skipped. Physical-device Macrobenchmark passed 5/5 cold starts at 313.89/330.23/335.04 ms minimum/median/maximum and retained real JSON plus five Perfetto traces.
- device state: exact `4.15`/code62 is installed on OnePlus CPH2691 / Android 15 after QA and benchmark cleanup. Device metadata reports minSdk 26, targetSdk 35, and launcher activity `.MainActivity`.
- release state: released and published. Corrected annotated tag `v4.15` targets release-metadata commit `292b24e`; immutable 22-file `tag-v4.15` artifact/backup payloads, all 21 manifest entries, final export parity, tag-source ZIP, installed APK identity, and release documents are verified; the v4.15 Complete gate passed. Feature history was merged to `main` without squashing at `81b8344`, and GitHub refs for `main`, the annotated tag object, and its peeled target were verified.

## Current development build

- versionName: `4.16-dev.42`
- versionCode: `104`
- event: `build-20260805-190244`
- branch: `feature/v4.16-g2-c1b1c2a`
- source commit: `183f47455093046fe991a9abd95fb2bbed57d951`
- APK: `artifacts/builds/v4.16-dev.42/build-20260805-190244/TranslateBooks-v4.16-dev.42-code104.apk`
- immutable local mirror: `backup/builds/v4.16-dev.42/build-20260805-190244/`
- APK SHA-256: `961D4DDF531EF3703EDFDFB3DF55BCBF26CFC8BF512076B25D63DB55AAC01A7B`
- source ZIP SHA-256: `BE80D9D197AAA7493AAFC4DE9C5D554CEF8DF7C37EC8BF4A632268FFE36E9266`
- toolchain provenance: Wrapper Gradle `9.3.0`, official distribution SHA-256 `0d585f69da091fc5b2beced877feab55a3064d43b8a1d46aeb07996b0915e0e0`, AGP `8.7.3`, JBR `21.0.10`/JetBrains, JDK major policy `21`, Java source/target `17`, compileSdk/targetSdk `35`.
- regression: final Wrapper matrix passed with `:editorial-engine:test` 105/105 and `:app:testDebugUnitTest` 164/164, 0 failures/errors/skips; `:app:compileDebugAndroidTestJavaWithJavac` PASS; lint 0 errors/53 warnings; `git diff --check` PASS. Direct device instrumentation passed isolated C2-A creator/resolver 5/5 on OnePlus CPH2691 / Android 15. Artifact and backup contain identical five-file payloads and all manifest hashes pass.
- device boundary: exact code104 was installed over code103 with `adb install -r`; no `pm clear` was used. The production `databases/` directory was empty at final inspection, so `ISOLATED_C2A_CREATOR_RESOLVER_QA_PASS`; `REAL_DATA_CONTINUITY: NOT_CLAIMED`.
- current C1 implementation baseline: `1c5ba96` on `feature/v4.16-g2-c1b1c1`; C1 adds only pure-JVM lineage request/context/result/service classes and tests, with no persistence or caller wiring. The archived code101 artifact remains unchanged.
- G2-C0B source state: the one truthful bootstrap profile remains at `editorial/engine-profile/v1/profile.json`. Canonical profile hash is `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`; machine-contract fingerprint is `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`; raw resource SHA-256 is `deb0e89a4084a88c137c71ba2aa7a7170f84d9979395ef58866c73529ce601eb`. Only `pack.integrity.sha256.v1` is implemented; all nine SAFE4 capabilities remain missing and the profile has no executable contract.
- G2-C1B1-B persistence state: SQLite source version is v16. The additive `editorial_lineage_records` and `editorial_lineage_input_entries` tables are append-only, validator-gated and have no importer/runtime call site. v15 pack/evaluation rows are preserved without lineage backfill; existing packs remain without lineage evidence until G2-C1B1-C.
- G2-C1B1-C0 state: boundary plan PASS. ZIP import stores pack/compatibility evidence only; project/chapter preparation closes facts only; future `RUN_CONTEXT_CLOSED` is the sole proposed root/child creation event. Stable project, input-scope and run identities plus explicit parent selection remain required. No lineage row, importer/runtime wiring, migration or promotion was added. Next proposed step is exactly G2-C1B1-C1, the explicit runtime context/service seam.
- G2-C1B1-C1 state: pure-JVM service seam PASS. `EditorialLineageRuntimeService.validateAndPrepare` accepts caller selections, resolves immutable authoritative context through an injected port, enforces CLOSED run state, prepares explicit ROOT/CHILD records and preserves existing validator codes without DAO/SQLite access. Focused tests are 21/21; full engine/app JVM regression is 95/95 and 163/163; Android-test Java compilation passed; lint remains 0 errors/53 warnings. `AUTHORITATIVE_IDENTITY_SCHEMA` is REQUIRED; next step is C1.5, not C2.
- device state: archived code101 was installed from `4.16-dev.39` on OnePlus CPH2691 / Android 15 without `pm clear`; isolated v16 migration/append QA passed 20/20. The device had no existing production database at final inspection, so real-data continuity is not claimed. No candidate file was read from `D:` and no manual SQLite mutation was used.
- G2-C0C-B2 state: historical v15 runtime wiring remains unchanged and its non-executable import behavior remains blocked. G2-C1B1-B only raises the database schema to v16 and adds an unused persistence boundary; no production DATA_COMPATIBLE result, ready state, certification, activation, binding or execution was created. `EditorialSafe4Pack.executionEnabled()` remains false.
- scope: G2-B1 storage/import, G2-B2A read-only management and G2-B2B-ZIP runtime ZIP selection are implemented. Folder import, certification, Golden Replay, activation, project binding, model execution, L1–L3 and release receipts remain blocked; this is not a completed v4.16 release.
- handoff: `BUILD_TOOLCHAIN_HARDENING_G2_T0B.md` records the T0B PASS/review stop and `EDITORIAL_PACK_PLATFORM_G2_C0B.md` records the C0B PASS/review stop, trust anchors and regression. C0B bundles the read-only profile/registry only; selector/runtime wiring remains absent, compatibility is unchanged and packs remain `STORED_BLOCKED`. G2-C0C requires separate approval.

## Release identity

- versionName: `4.15`
- versionCode: `62`
- build event: `build-20260729-070042`
- Gradle: `9.3.0` wrapper; Android Gradle Plugin `8.7.3`
- Android Studio: `2026.1.1` (`AI-261.23567.138.2611.15646644`)
- compileSdk: `35`
- targetSdk: `35`
- implementation source commit: `5253f003b0f7898b1755e98783ec91b01790e1d4`
- release-document commit: `292b24e2ac7dec7b9635b8d0e72f76745ddf432c` (corrected annotated tag `v4.15`, tag object `cb474d2dd22763f67e24b4c0f57a27b7ae4689e7`)
- branch: `main` (published to `https://github.com/manhluongvd/translate-books-android.git`; corrected annotated tag `v4.15` remains immutable)

## Existing artifacts

- v4.15 release archive: `artifacts/releases/v4.15/tag-v4.15/TranslateBooks-v4.15-code62.apk`
  - Size: `2,517,555` bytes
  - APK SHA-256: `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`
  - Source snapshot: `artifacts/releases/v4.15/tag-v4.15/project_source_v4.15.zip`
  - Source ZIP SHA-256: `0F14121D69314C36CF6F92E80084881D32AA8E6BA35ACB165203D52161B2F433`
  - Immutable mirror: `backup/v4.15/tag-v4.15/`; all 22 files match by relative path, length, and SHA-256.
  - `SHA256SUMS.txt`: all 21 payload entries verified.
  - Source verification: the 357-entry ZIP exactly matches a fresh `git archive` of corrected tag `v4.15`.

- v4.14 release archive: `artifacts/releases/v4.14/tag-v4.14/TranslateBooks-v4.14-code57.apk`
  - Size: `2,515,055` bytes
  - APK SHA-256: `D478AC135ED3C736F7FC850FAA06CA4F6F5CB6D0634BB09B15BD59C9B2DAE5D1`
  - Source snapshot: `artifacts/releases/v4.14/tag-v4.14/project_source_v4.14.zip`
  - Source ZIP SHA-256: `5F2DBA2C1D4F806A838961144807228C22C1846070296FACD6915A3DC2B43E5B`
  - Immutable mirror: `backup/v4.14/tag-v4.14/`; all 20 files match by relative path, length, and SHA-256.
  - `SHA256SUMS.txt`: all 19 payload entries verified.

- v4.8 development APK: `artifacts/releases/v4.8/dev-e4b9dfd-20260717-201229/TranslateBooks-v4.8-debug.apk`
  - Size: `1,626,871` bytes
  - SHA-256: `ABED4892CEFA9F3363F85ED8CF57F4D06C388447EE8F37617683FBB6DF891FB3`
  - Mirrored without overwrite at `backup/v4.8/dev-e4b9dfd-20260717-201229/`.

- v4.8 release archive: `artifacts/releases/v4.8/tag-v4.8/TranslateBooks-v4.8-debug.apk`
  - SHA-256: `ABED4892CEFA9F3363F85ED8CF57F4D06C388447EE8F37617683FBB6DF891FB3`
  - Source snapshot: `artifacts/releases/v4.8/tag-v4.8/project_source_v4.8.zip`
  - Immutable mirror: `backup/v4.8/tag-v4.8/`.

- Release candidate APK: `artifacts/v47/TranslateBooks-v4.7-debug.apk`
  - Size: `1,625,315` bytes
  - SHA-256: `BBA8152197FCE0310236899BA313B7C285E91FA1299D703EEF4BB23FA316EE4F`
  - The APK is present locally but excluded from Git by `*.apk` in `.gitignore`.
- Local build APK: `app/build/outputs/apk/debug/TranslateBooks-v4.7-debug.apk` (excluded from Git).
- Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (excluded from Git).
- Versioned QA evidence: `artifacts/v47/`, including unit-test, instrumentation, lint, screenshots, video, Perfetto traces, performance report, and checksums.

## Regression status

`PASS WITH KNOWN LIMITATIONS` for v4.15:

- Archive-first exact build, approved-logo guard, APK metadata, installation, and checksum verification: passed.
- JVM unit tests: 110 passed, 0 failed, 0 errors, 0 skipped.
- Android Lint: 0 errors, 53 warnings.
- Connected instrumentation: 15 passed, 1 explicitly opt-in paid real-API case skipped, 0 failures/errors on OnePlus CPH2691 / Android 15.
- Focused exact-code62 instrumentation passed Settings persistence, Glossary/Pronoun scroll preservation, and independent multi-profile import.
- Physical-device Macrobenchmark: 5/5 cold starts passed; 313.89/330.23/335.04 ms minimum/median/maximum with five retained Perfetto traces.
- Exact visual evidence: five screenshots, five UI hierarchy dumps, and a documented 25-frame actual-device MJPEG sequence.
- Immutable tag archive: both 22-file copies and all 21 manifest entries passed; the source ZIP exactly matches a fresh `git archive` of corrected tag `v4.15`.

Current development device evidence (2026-08-04):

- Exact archived `4.16-dev.29`/code91 installation passed on OnePlus CPH2691 / Android 15; APK SHA-256 is `959A25630941550E3D59CB2FBE75C1E3C6D33DCA2A109553C248CDA653E4DE90`.
- A disposable non-canonical ZIP selected through SAF was stored with its exact canonical hash, displayed as `STORED_BLOCKED`, and remained present after process restart. The concrete blocker was `UNSUPPORTED_CONTRACT_SCHEMA: No trusted contract descriptor is installed`.
- The device row remains a blocked QA record because no manual database mutation or deletion bypass was used; the pushed ZIP was removed. This evidence does not certify SAFE4 or prove a trusted engine profile.

The previous v4.8 release evidence remains `PASS WITH KNOWN LIMITATIONS`:

- Build, 99 JVM tests, 11 required offline/device instrumentation cases, and two opt-in real requests passed.
- Glossary/pronoun persistence, cold-store reload, Back navigation, and four-tab gestures passed.

The last released v4.7 evidence remains `PASS WITH KNOWN LIMITATIONS`:

- Build: successful (`clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`; 73 tasks).
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Lint: 0 errors, 50 warnings.
- Core translation and recovery regression coverage: passed within the scope recorded in `QA_REPORT_v4_7.md`.

The v4.15 release was published to the private GitHub repository. Initial remote
verification resolved release merge `main` to
`81b8344ba38be7b3851086aca250a0cce2869e37`, annotated tag object
`cb474d2dd22763f67e24b4c0f57a27b7ae4689e7`, and immutable tag target
`292b24e2ac7dec7b9635b8d0e72f76745ddf432c`. Final release-state
documentation follows on `main` without moving the tag.

The previous v4.14 release remains published and immutable.

## Known issues

- Android Lint reports 53 warnings and 0 errors.
- OnePlus Android 15 denies shell `screenrecord`; the retained AVI is a documented 25-frame sequence of timed actual-device screenshots, not a continuous MediaProjection capture.
- AndroidX reports `run-from-apk` compilation mode for the startup benchmark.
- The opt-in paid real-API instrumentation case was intentionally skipped; release QA made no provider request and incurred no billing.
- The first local `v4.15` tag was rejected before archive because it contained stale v4.14 release metadata. With explicit approval it was deleted while still unpublished; corrected metadata was committed and the accepted annotated tag was recreated before backup.
- v4.15 APK/source/evidence archives remain intentionally outside tracked Git under ignored `artifacts/releases/` and `backup/`; a clean clone contains release documents but not these binary payloads.
- The accepted SAFE4 foundation build `4.16-dev.24`/code86 remains archived and is not the code91 device-QA identity. SAFE4 QA must use the exact approved build and canonical assets; no candidate bytes are permitted.
- SAFE4 execution/release is intentionally blocked until exact lineage, exhaustive ledgers, evidence-derived gates, conditional Pronoun/Pair handling, the new release contract and Golden Replay G1–G10 are complete.
- Rejected development candidate `4.16-dev.23`/code85: Gradle and the APK used the exact SAFE4 bytes, but post-build inspection found Git source-archive EOL conversion changed the three `.txt` hashes. Both immutable payloads remain retained for audit; code85 is not an accepted reproducible build and must not be installed or used as evidence.

## G2-C0C evidence (2026-08-05)

- Decision: `G2-C0C BLOCKED`; C0C-A pure-JVM adapter/resolver/result seam is `PASS`, but C0C-B production importer/persistence wiring was not started because SQLite v14 lacks trusted profile ID/version/hash, adapter-set hash, capability fingerprint and evaluation-context identity. The approved plan requires a separate additive immutable migration; none was created or run.
- Baseline/end: branch `feature/v4.16`; start HEAD `3597e9d556e7e0edaef51267d990129cdb2cacc5`; C0C-A implementation baseline `91b9bbed92d4180dd6a038a2812818664d6a411a`. The documentation commit is intentionally not used as an implementation baseline.
- C0C-A files: immutable `EditorialCompatibilityEvaluationResult`, stable `EditorialCompatibilityReasonCode`, `EditorialEngineProfileAdapter`, `EditorialEngineProfileResolver` and `EditorialEngineProfileResolverTest`. No app/runtime importer, UI, database/migration, SAFE4 or candidate file changed.
- Focused C0C-A tests: 6/6 pass, 0 failures/errors/skips. The bundled production profile remains no-executable-contract (null contract bounds, empty schema/phase/context descriptors); no integrity-only `DATA_COMPATIBLE` result was fabricated.
- Wrapper/JDK regression: Gradle 9.3.0/JBR 21; engine 56/56, app JVM 161/161, instrumentation Java compilation PASS, lint 0 errors/53 warnings, `git diff --check` PASS; JDK preflight 4/4. No direct cached Gradle replacement was used.
- Build/device: no APK build or installation was performed after C0C-A because C0C-B is blocked. Latest accepted code92 remains APK SHA-256 `07BC98B22019832AFD37D0307E691957FBDC47D1C0E929949535D69AFB472801`, source ZIP SHA-256 `BD6BCDC832DC5C3D9AFFA3E1C597A12D090F560F6B8D6319D0D65F7FB22C1126`; artifact/backup parity remains unchanged. Device QA is pending/not run.
- Safety/worktree: `EditorialSafe4Pack.executionEnabled()` remains false; canonical SAFE4 hashes, nine missing capabilities, SQLite v14 history, importer/SAF flow, QA blocked row and code92 artifact remain unchanged. Final worktree is limited to the three protected user-owned `.idea/*` changes, unstaged; no push, merge or tag.

## G2-C0C-B2 runtime wiring evidence (2026-08-05)

- Decision: `G2-C0C-B2_IMPLEMENTATION: PASS`; `SAFE4_EXECUTION_READINESS: BLOCKED`. Runtime wiring is limited to new, valid, non-duplicate imports. No certification, activation, project binding, execution, reevaluation or production `DATA_COMPATIBLE` result was created.
- Baseline/end: branch `feature/v4.16`; baseline HEAD `922247b0ab78b924ef1c71f26e7d5f0a8d11534f`; implementation baseline before documentation is `6e148264e46279dbcf5340a6ee4b4a78a108fc07`. The three user-owned `.idea/*` files remained untouched, unstaged and uncommitted.
- Runtime boundary: `BundledEditorialEngineContractProfileRegistry` is the only trusted profile source. The resolver is injected into the import service after integrity validation and before compatibility state persistence; profile facts are not read from pack bytes, ZIP/SAF, SQLite, external filesystem, network or `MainActivity`.
- Eligibility: production profile `com.ml.tblandroidtxt.editorial.engine.bootstrap`/`1.0.0` remains non-executable and declares only `pack.integrity.sha256.v1`. Its canonical hash is `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`; machine fingerprint is `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`. Production compatibility therefore resolves to `ENGINE_UPGRADE_REQUIRED` with `PROFILE_NON_EXECUTABLE` and deterministic missing-capability details, persisted as `STORED_BLOCKED`.
- Persistence: SQLite v15 append-only evidence records pack hash, trusted profile identity/hash, machine fingerprint, evaluator contract version, canonical evaluation context/fingerprint, outcome, stable reason, blocker/missing-capability details, timestamp and evaluation identity. v14 pack/import/compatibility rows are not backfilled. Duplicate hash/import checks precede resolver evaluation; transaction failure rolls back database rows and retains immutable payload for recovery.
- Regression: Wrapper Gradle `9.3.0`/JBR `21.0.10`; `:editorial-engine:test` 60/60; `:app:testDebugUnitTest` 162/162; instrumentation compilation PASS; lint 0 errors/53 warnings; `git diff --check` PASS; JDK preflight 4/4. Connected targeted B2 instrumentation passed 5/5 on OnePlus CPH2691 / Android 15.
- Archive: `4.16-dev.36`/code98, event `build-20260805-095511`, APK SHA-256 `9E147135DEA5D37EDFF5220D5EC8C44CE2686438DE3831BC5EC564E7BC752EC8`, source ZIP SHA-256 `B8B518270092D03ECC4D479DC93AB2C55EFF027A395600582EECAA5797799F22`. Artifact/backup parity passed for the five-file payload; `BUILD_INFO.json` and README contain complete T0B Gradle/AGP/JDK/SDK/Git/hash provenance.
- Device evidence: final code98 was installed as an upgrade without clearing app data. The 5/5 targeted suite covered production non-executable provenance, duplicate no-re-evaluation, v15 rollback, restart readback and test-only unsupported contract. No candidate was read from `D:` and no manual database edit was made. Initial code94 duplicate failure was corrected before code98 and is retained in the B2 handoff evidence.
- Next step: review stop. The next proposed work is a separately approved capability/executable-contract phase; do not proceed directly to certification or activation.

## G2-C1B1-B SQLite v16 persistence evidence (2026-08-05)

- Decision: `G2-C1B1B_PERSISTENCE: PASS`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Baseline/end: B1B started from `feature/v4.16-g2-c1b1a` at `3f748ca78380d9bc6daa9f6367020874fc5b1ae6`; C1A snapshot correction is `3fcf039be0185b22567e75f34a970bf75ba5ea6a`; B1B implementation baseline is `1d6fbae` on `feature/v4.16-g2-c1b1b`. Protected `.idea/*` files remain untouched and unstaged.
- Schema: `TranslationRepository` source version is v16. `EditorialMigrationSpec.from15To16()` adds only `editorial_lineage_records`, `editorial_lineage_input_entries`, three lookup indexes and four immutable update/delete triggers. v15 tables/rows are not altered, rebuilt, backfilled or reevaluated. Downgrade is rejected.
- Implementation: append/read-only `EditorialLineageDao`, immutable row mapper, SQLite validation-context adapter and pure-JVM validator boundary. Parent must exist and match exactly; duplicate, reparent, orphan, mismatch, cross-context, partial transaction and mutation cases fail closed. No importer/runtime call site exists.
- Regression: engine `74/74`, app JVM `163/163`, 0 failures/errors/skips; Android-test Java compilation PASS; lint 0 errors/53 warnings; `git diff --check` PASS. Device direct runner passed `EditorialLineagePersistenceInstrumentedTest` 11/11 and `EditorialPackCompatibilityEvaluationInstrumentedTest` 9/9 on OnePlus CPH2691 / Android 15.
- Archive: `4.16-dev.39`/code101, event `build-20260805-124714`, APK SHA-256 `BF5C7EB41A26242C171B04977182A0A1378BF1F8269B34E77A98D0CC13E61F30`, source ZIP SHA-256 `7EE2512AA71E39E4C12BD17697C981B6B11A12F7C4B34EDF8C95C47C1C762E8C`; artifact/backup five-file parity passed and BUILD_INFO source commit is `312cf01a6b566bf46dc6c84a01b4e203b798500e`.
- Device boundary: isolated migration/append QA passed 20/20. No production database existed at final `run-as` inspection, so real-data continuity is not claimed. No `pm clear`, manual SQLite mutation or candidate input was used.
- Profile/catalog/execution: only `pack.integrity.sha256.v1` remains in the production catalog; canonical profile hash `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6` and machine fingerprint `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c` are unchanged; `executionEnabled()` remains false.
- Handoff: `EDITORIAL_PACK_PLATFORM_G2_C1B1B_LINEAGE_SQLITE_V16.md`; checklist `release_checklists/v4.16-g2-c1b1b.md`. Exact next step is `G2-C1B1-C importer/runtime lineage wiring`; do not start it without separate approval.

## G2-C1B1-C1.5-A authoritative identity schema plan (2026-08-05)

- Decision: `G2-C1B1C15A_SCHEMA_PLAN: PASS`; `SQLITE_V17_IMPLEMENTATION: NOT_STARTED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Baseline/end: branch `feature/v4.16-g2-c1b1c15a`; implementation baseline before documentation is `63d77ac267671f88ee2c9ebc31ebc8e575ec74cd`; handoff/checklist commit is `f8d0662270436a0a39b7f2c9a48e0ef4655090c7`. The protected `.idea/*` files remain untouched and unstaged.
- Schema decision: future v16→v17 migration is additive only and proposes immutable `editorial_project_revisions`, `editorial_input_scope_snapshots`, `editorial_input_scope_snapshot_entries`, `editorial_closed_run_contexts` and `editorial_run_lineage_bindings`. AUTOINCREMENT IDs remain local provenance only; no v17 table uses one as a semantic identity.
- Boundary: current project/chapter/asset/reference/run rows are mutable or incomplete; v15 compatibility evaluation is authoritative compatibility evidence but not a run identity; v16 lineage is persistence evidence but has no authoritative project/scope/closed-run source. Existing rows remain `IDENTITY_UNATTESTED`; no backfill, migration, DAO, runtime caller, lineage row or importer change occurred.
- Identity policy: separate domain-separated SHA-256 projections exclude timestamps, UI text, local paths and row IDs; project revision, scope snapshot, closed run and run-lineage binding are append-only with `ON DELETE RESTRICT` and immutable triggers. `RUN_CONTEXT_CLOSED` remains the only future lineage creation boundary.
- Verification: required read-only Wrapper regression passed with `:editorial-engine:test` 95/95 and `:app:testDebugUnitTest` 163/163; failures/errors/skips `0/0/0`; combined 258/258. `git diff --check` passed before the final documentation commit. No APK build/install or device QA was run.
- Handoff: `EDITORIAL_PACK_PLATFORM_G2_C1B1C15A_IDENTITY_SCHEMA_PLAN.md`; checklist `release_checklists/v4.16-g2-c1b1c15a`. Exact next step is `G2-C1B1-C1.5-B` schema/DAO implementation after the listed identity and retention decisions are approved; do not start C2 caller wiring.

## G2-C1B1-C1.5-B SQLite v17 authoritative identity store (2026-08-05)

- Status: `G2-C1B1C15B_SQLITE_V17: PASS`; `AUTHORITATIVE_IDENTITY_STORE: IMPLEMENTED`; `AUTHORITATIVE_IDENTITY_CREATOR: NOT_STARTED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Branch/HEAD: implementation started from `feature/v4.16-g2-c1b1c15a` at `721c78291a25ed15d09165f7f1a49de435368b24`; implementation/build source HEAD is `4cd50ba34b1a64749290c51c90fe3a6fc65e91ed` on `feature/v4.16-g2-c1b1c15b`. Final documentation/state commit is separate from the archived build source.
- SQLite: `TranslationRepository.VER=17`. `EditorialMigrationSpec.from16To17()` is additive/transactional and creates exactly five tables: `editorial_project_revisions`, `editorial_input_scope_snapshots`, `editorial_input_scope_snapshot_entries`, `editorial_closed_run_contexts`, and `editorial_run_lineage_bindings`, with lookup indexes, `ON DELETE RESTRICT` foreign keys and before-update/before-delete rejection triggers. No v16 table is altered/rebuilt and migration performs zero backfill.
- Canonical identities: the four deterministic families use exact domains `EDITORIAL_PROJECT_REVISION_IDENTITY_V1`, `EDITORIAL_INPUT_SCOPE_IDENTITY_V1`, `EDITORIAL_CLOSED_RUN_IDENTITY_V1`, and `EDITORIAL_RUN_LINEAGE_BINDING_IDENTITY_V1`; separate fingerprint domains are used. Strict UTF-8, NFC scope keys, ASCII outer trim, preserved case/punctuation/leading numeric meaning, sorted roles/entries and exclusion of timestamps/row IDs/URI/path/UI state are tested. Project identity requires an explicit stable semantic key and versioned project-definition contract; current row IDs, pack/import IDs and random UUIDs are not used.
- DAO/transaction: `EditorialProjectRevisionDao`, `EditorialInputScopeSnapshotDao`, `EditorialClosedRunContextDao` and `EditorialRunLineageBindingDao` expose append/read/list only. Same canonical bytes return `ALREADY_EXISTS`; identity/content mismatch returns `DUPLICATE_IMMUTABLE_RECORD`. Snapshot entries are atomic. Closed-run attempt ordinals are DAO-allocated inside serialized transactions using scope/run-kind/phase/trusted-context allocation keys. Only trusted `DATA_COMPATIBLE` evaluation evidence can close a run; blocked, legacy, stale/mismatched, invalid, upgrade-required and adapter-required contexts fail closed. `EditorialLineageAndBindingTransactionService` uses one SQLite transaction for v16 lineage append/readback and v17 binding; binding failure rolls back a new lineage and preserves an existing lineage.
- Regression: focused Gate A `10/10`; `:editorial-engine:test` `105/105`; `:app:testDebugUnitTest` `164/164`; failures/errors/skips `0/0/0`; `:app:compileDebugAndroidTestJavaWithJavac` PASS; `:app:lintDebug` PASS with 0 errors/53 warnings; `git diff --check` PASS.
- Archive: `4.16-dev.41`/code103, event `build-20260805-183201`, source commit `4cd50ba34b1a64749290c51c90fe3a6fc65e91ed`; APK `artifacts/builds/v4.16-dev.41/build-20260805-183201/TranslateBooks-v4.16-dev.41-code103.apk`; immutable mirror `backup/builds/v4.16-dev.41/build-20260805-183201/`; APK SHA-256 `F9935E2CC4BE5B863A14DA194ED7073E76853A186E1877F5FAC6EE548285252`; source ZIP SHA-256 `183E6B4DF79EA8F03CCA9536868EDDF92623088D55EE40CD53C4535E55C0D11B`.
- Device QA: exact code103 was installed over code101 with `adb install -r`, no `pm clear`. Direct instrumentation on OnePlus CPH2691 / Android 15 passed `EditorialV17IdentityStoreInstrumentedTest` `8/8`, `EditorialLineagePersistenceInstrumentedTest` `11/11`, and `EditorialPackCompatibilityEvaluationInstrumentedTest` `9/9`; total `28/28`. The production app `databases/` directory was empty at inspection, so `ISOLATED_V17_IDENTITY_AND_BINDING_QA_PASS`; `REAL_DATA_CONTINUITY: NOT_CLAIMED`. No manual SQLite mutation and no candidate file from `D:` were used.
- Safety boundary: profile/catalog unchanged; production profile remains non-executable; `EditorialSafe4Pack.executionEnabled()` remains `false`; no importer, production project/run caller, creator, certification, activation, project binding, capability promotion or execution was added.
- Handoff/checklist: `EDITORIAL_PACK_PLATFORM_G2_C1B1C15B_SQLITE_V17.md`; `release_checklists/v4.16-g2-c1b1c15b.md`. Exactly one next step: review this implementation and approve any separate future C2 caller phase; do not start it automatically.

## G2-C1B1-C2-A authoritative identity creator/resolver (2026-08-05)

- Status: `G2-C1B1C2A_CREATOR_RESOLVER: PASS`; `AUTHORITATIVE_IDENTITY_CREATOR: IMPLEMENTED`; `AUTHORITATIVE_CONTEXT_RESOLVER: IMPLEMENTED`; `PRODUCTION_CLOSED_RUN_CREATION: BLOCKED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Baseline/build: started from `5e17c8703c7656cde85e21cbc37db241d44a830d` on `feature/v4.16-g2-c1b1c15b`; implementation/build source is `183f47455093046fe991a9abd95fb2bbed57d951` on `feature/v4.16-g2-c1b1c2a`. Archive is `4.16-dev.42`/code104, event `build-20260805-190244`; APK SHA-256 `961D4DDF531EF3703EDFDFB3DF55BCBF26CFC8BF512076B25D63DB55AAC01A7B`; source ZIP SHA-256 `BE80D9D197AAA7493AAFC4DE9C5D554CEF8DF7C37EC8BF4A632268FFE36E9266`; five-file artifact/backup parity PASS.
- Creator/resolver: explicit request-only project/scope/closure creator reuses v17 canonical models and DAO append-only vocabulary; it re-reads pack/evaluation/scope facts and lets the DAO allocate attempt ordinal. SQLite resolver reconstructs C1 context solely from exact v17/v15/lineage stored facts, with no latest-row fallback. Read-only retention preflight reports project/run authoritative provenance references; deletion UX wiring remains deferred.
- Production boundary: bundled resolver requires an executable exact trusted profile; current profile is non-executable, so production closure is rejected without a row. Positive closure/context evidence is isolated test-only fixture data and does not alter profile/catalog/capability state.
- Verification: Wrapper/JBR engine `105/105`, app `164/164`, failures/errors/skips `0/0/0`; Android-test compilation PASS; lint 0 errors/53 warnings; `git diff --check` PASS. Exact code104 upgrade (no `pm clear`) passed isolated creator/resolver device QA 5/5 on OnePlus CPH2691 / Android 15. Production database was empty: `REAL_DATA_CONTINUITY: NOT_CLAIMED`.
- Safety: no importer/startup/UI/production caller, C1 runtime invocation, production lineage/binding, migration, certification, activation, project binding, execution, capability promotion, or profile/catalog change. `executionEnabled()` remains false; protected `.idea/*` remains untouched/unstaged.
- Handoff/checklist: `EDITORIAL_PACK_PLATFORM_G2_C1B1C2A_AUTHORITATIVE_CREATOR_RESOLVER.md`; `release_checklists/v4.16-g2-c1b1c2a.md`. Exactly one next step: review C2-A and separately approve C2-B production caller wiring at `RUN_CONTEXT_CLOSED`; do not start it automatically.

## G2-C1B1-C2-B0 production caller boundary plan (2026-08-06)

- Status: `G2-C1B1C2B0_BOUNDARY_PLAN: PASS`; `PRODUCTION_RUN_CONTEXT_CLOSED_EVENT: ABSENT`; `PRODUCTION_CALLER_OWNER: UNRESOLVED`; `ATOMICITY_CONTRACT: DECIDED`; `RECOVERY_CONTRACT: DECIDED`; `ROOT_CHILD_SELECTION_CONTRACT: DECIDED`; `C2B1_IMPLEMENTATION: NOT_STARTED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Baseline/end: started from `feature/v4.16-g2-c1b1c2a` at `ddec1acebec448f7f206142cd011dfa633a91816`; current branch is `feature/v4.16-g2-c1b1c2b0`. No production source or database implementation changed; the documentation/state commit is separate from the implementation baseline. The three protected `.idea/*` files remain untouched, unstaged and uncommitted.
- Audit result: source evidence shows `MainActivity`/`EditorialRepository` create mutable projects/chapters/assets; `editorial_runs` has DDL and deletion references but no production INSERT/creator/state-transition call site; `L1_CLOSED`/`L2_CLOSED` are chapter workflow states, not run closure. Translation job resume/retry is not authoritative editorial-run recovery. No production caller owns `sourceRunRowId`, `runKind`, `phaseIdentity`, or frozen manifest attestation.
- Contract: exactly one future caller is proposed, `EditorialRunContextCoordinator`, at an explicit authoritative `RUN_CONTEXT_CLOSED` event. The event is currently absent, so caller wiring remains unresolved/not started. ROOT must be explicit with no parent; CHILD must carry one exact parent identity; no latest/fallback/reparent behavior is allowed.
- Atomicity/recovery: Option B is decided — close immutable context independently, then pure C1 validation, then existing one-connection lineage+binding transaction. `CLOSED_UNBOUND` is a derived read state, not schema v18. Retry is deterministic, readback-authoritative, no new ordinal, no new parent, and no binding replacement. Retention preflight remains read-only; delete UX wiring is deferred to C2-B2 or a separate phase.
- Compatibility blocker: production profile still declares only `pack.integrity.sha256.v1`; `EditorialSafe4Pack.executionEnabled()` remains `false`. No production `DATA_COMPATIBLE`, closed-run, lineage, or binding row may be created; test fixtures are not production evidence.
- Verification: Wrapper read-only regression `:editorial-engine:test` `105/105` and `:app:testDebugUnitTest` `164/164`, failures/errors/skips `0/0/0`; `git diff --check` PASS. No APK build/install, instrumentation, lint, migration, raw SQLite inspection, or database-row creation was performed for B0. Code104 artifact/backup parity remains the accepted baseline. No candidate was read from `D:`.
- Handoff: `EDITORIAL_PACK_PLATFORM_G2_C1B1C2B0_PRODUCTION_CALLER_BOUNDARY_PLAN.md`; checklist `release_checklists/v4.16-g2-c1b1c2b0.md`. Exactly one next step: review/approve the C2-B1 coordinator plan; do not start C2-B1, C2-B2, capability promotion, certification, activation, or execution automatically.

## G2-C1B1-C2-B1 lineage coordinator and recovery (2026-08-06)

- Status: `G2-C1B1C2B1_COORDINATOR: BLOCKED` for review-stop reporting because the unrelated full connected suite retained three non-B1 failures; the B1 focused coordinator class itself is `7/7 PASS`. `OPTION_B_ORCHESTRATION: IMPLEMENTED`; `CLOSED_UNBOUND_RECOVERY: IMPLEMENTED`; `ROOT_CHILD_PARENT_STABILITY: PROVEN` for the injected immutable event fixture; `PRODUCTION_CLOSURE_EVENT_RESOLVER: ABSENT_FAIL_CLOSED`; `PRODUCTION_RUN_CONTEXT_CLOSED_EVENT: ABSENT`; `PRODUCTION_CALLER_OWNER: UNRESOLVED`; `LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED`; `LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED`; `SAFE4_EXECUTION_READINESS: BLOCKED`.
- Branch/baseline: started from `feature/v4.16-g2-c1b1c2b0` at `616a0184a2df2462c3adca316f59cbd70e7d88b9`; implementation was completed on `feature/v4.16-g2-c1b1c2b1`. Protected `.idea/compiler.xml`, `.idea/gradle.xml`, and `.idea/misc.xml` remain user-owned, untouched, unstaged and uncommitted.
- Implementation: app-side immutable `EditorialRunContextCommand`, closure-event model/resolution port, production unavailable resolver, injected clock, coordinator result/status vocabulary and `EditorialRunContextCoordinator`. The coordinator resolves exactly one canonical event, closes v17 context through C2-A, reconstructs C1 selection, supports Option B `CLOSED_UNBOUND`, uses the existing one-connection lineage+binding transaction, and exact-readbacks immutable lineage/binding. No production composition call site was added.
- Event/recovery contract: `EDITORIAL_CLOSURE_EVENT_IDENTITY_V1` and `EDITORIAL_CLOSURE_EVENT_FINGERPRINT_V1` include explicit ROOT/CHILD and exact parent. Replays read exact binding/lineage before C1 duplicate rejection; same event returns `ALREADY_BOUND`; existing unbound lineage is rebound only after authoritative context readback; conflict/mismatch never rebinds. Status inspection is read-only and never chooses latest or writes.
- Verification: Wrapper final JVM regression `:editorial-engine:test` `105/105` and `:app:testDebugUnitTest` `164/164`, combined `269/269`, failures/errors/skips `0/0/0`; Android instrumentation source compilation PASS; lint PASS with 0 errors/53 warnings; `git diff --check` PASS. Direct exact device run on OnePlus CPH2691 / Android 15 passed B1 coordinator `7/7` on the archived code107 APK.
- Archive/device: final archive-first build is `4.16-dev.45`/code107, event `build-20260806-090226`, APK SHA-256 `F0A8971AA9E8A4D08CF08A63857BC37F17BD6DEFE264CE4F2813DAAF8A1211A9`, source ZIP and artifact/backup parity recorded by `scripts/build-and-save.ps1`. Exact APK was installed with `adb install -r`; no `pm clear` was used. Production database continuity is not claimed: `REAL_DATA_CONTINUITY: NOT_CLAIMED`.
- Known verification blocker: full connected suite was attempted on the exact archive build but stopped with three unrelated failures: two existing ZIP expectation mismatches (`INVALID_ENTRY_PATH`/`TRUNCATED_STREAM`, `TRUNCATED_STREAM`/`ENTRY_COUNT_LIMIT`) and one `EditorialPackManagementPageInstrumentedTest` failure. One explicitly skipped real-API test remains approved. These were not changed by B1 and prevent a global PASS claim.
- Safety: no migration v18, importer/startup/UI/project-preparation caller, production closure event, production closed-run/lineage/binding row, capability promotion, certification, activation, project binding or execution was added. `EditorialSafe4Pack.executionEnabled()` remains `false`; retention preflight remains read-only and delete UX wiring is deferred.
- Handoff/checklist: `EDITORIAL_PACK_PLATFORM_G2_C1B1C2B1_LINEAGE_COORDINATOR.md`; `release_checklists/v4.16-g2-c1b1c2b1.md`. Exactly one next step: review this B1 handoff and resolve/approve the unrelated connected-suite blocker before any separately approved C2-B2 production-lifecycle work.
