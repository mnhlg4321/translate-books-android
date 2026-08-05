# Build State

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

- versionName: `4.16-dev.30`
- versionCode: `92`
- event: `build-20260804-182006`
- branch: `feature/v4.16`
- source commit: `8fbc8d1ed004c1524e001b7bbd0a2b321584331b`
- APK: `artifacts/builds/v4.16-dev.30/build-20260804-182006/TranslateBooks-v4.16-dev.30-code92.apk`
- immutable local mirror: `backup/builds/v4.16-dev.30/build-20260804-182006/`
- APK SHA-256: `07BC98B22019832AFD37D0307E691957FBDC47D1C0E929949535D69AFB472801`
- source ZIP SHA-256: `BD6BCDC832DC5C3D9AFFA3E1C597A12D090F560F6B8D6319D0D65F7FB22C1126`
- toolchain provenance: Wrapper Gradle `9.3.0`, official distribution SHA-256 `0d585f69da091fc5b2beced877feab55a3064d43b8a1d46aeb07996b0915e0e0`, AGP `8.7.3`, JBR `21.0.10`/JetBrains, JDK major policy `21`, Java source/target `17`, compileSdk/targetSdk `35`.
- regression: G2-T0B required Wrapper matrix passed with `:editorial-engine:test` 33/33, `:app:testDebugUnitTest` 161/161, instrumentation Java compilation PASS, lint 0 errors/53 warnings and `git diff --check` PASS. Preflight tests passed 4/4 for JDK 21, Java 8 rejection, missing JAVA_HOME and invalid path. Archive-first build completed successfully with no installation. Artifact and backup contain identical five-file payloads and all manifest hashes pass. The prior code91 device evidence remains the latest installed-device evidence; code92 was not installed.
- G2-C0B source state (no APK build): commits `fb2bd262dc996d20fbef051414727525410e9e1a`, `6c6fdb2c3ab15e972716d005b1588cee8b51fdec` and `285819c329c922d1256eb27bc13ee5ba75ec5fd1` bundle the one truthful bootstrap profile at `editorial/engine-profile/v1/profile.json` and copy/verify that same source into generated APK assets. Canonical profile hash is `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`; machine-contract fingerprint is `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`; raw resource SHA-256 is `deb0e89a4084a88c137c71ba2aa7a7170f84d9979395ef58866c73529ce601eb`. Only `pack.integrity.sha256.v1` is implemented; all nine SAFE4 capabilities remain missing.
- G2-C0B regression: focused bundled-registry suite passed 17/17; full `:editorial-engine:test` passed 50/50, `:app:testDebugUnitTest` 161/161, instrumentation Java compilation PASS, lint 0 errors/53 warnings and `git diff --check` PASS. No runtime/importer wiring or compatibility state changed.
- device state: archived code91 APK was installed without rebuilding on OnePlus CPH2691 / Android 15 (`adb install -r` returned `Success`); package metadata reports `4.16-dev.29`/code91, minSdk 26 and targetSdk 35. Manual ZIP-import device QA passed the real SAF selection, immutable storage, fail-closed `STORED_BLOCKED` result and process-restart persistence checks. This was synthetic non-SAFE4 QA only; no certification, activation, binding, model call or execution was performed.
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
