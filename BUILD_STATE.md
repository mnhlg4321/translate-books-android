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

- versionName: `4.16-dev.24`
- versionCode: `86`
- event: `build-20260803-165452`
- branch: `feature/v4.16`
- source commit: `ab8e78abeeb0fa6264ac8a7b0dea2e436cd39d90`
- APK: `artifacts/builds/v4.16-dev.24/build-20260803-165452/TranslateBooks-v4.16-dev.24-code86.apk`
- immutable local mirror: `backup/builds/v4.16-dev.24/build-20260803-165452/`
- APK SHA-256: `6C3EEF7350B3D1C24BE9D65B0E906D2042CE281556235E783F8DBE4962F37875`
- source ZIP SHA-256: `9CCE4C99955E700FAA5EB6E2AF7153A51211A2130AF7A4E939F97CBB24A0D9F1`
- regression: `scripts/build-and-save.ps1 -Series 4.16-dev` completed successfully; 144 JVM tests passed with 0 failures/errors/skips and Android Lint completed with 53 warnings/0 errors. Both five-file payloads and all four manifest entries match. The source ZIP reproduces the exact three required SAFE4 file sizes and SHA-256 values.
- device state: installation was not requested for code86. No ADB device was available during the rejected code85 optional-install attempt. The previously installed code84 contains the retired V5 engine and must not be used for SAFE4 QA.
- scope: SAFE4 pack integrity, project binding, legacy read-only handling and blocked input preparation are implemented. Exact lineage/schema, evidence-derived gates, SAFE4 L1–L3, release receipts, Golden Replay, manual device QA and all v4.16 RC gates remain open. This is not a completed v4.16 release.

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
- The accepted SAFE4 foundation build `4.16-dev.24`/code86 is archived but not installed or manually device-tested. The device's prior code84 contains the retired V5 engine and must not be reused as SAFE4 execution, QA, or release evidence.
- SAFE4 execution/release is intentionally blocked until exact lineage, exhaustive ledgers, evidence-derived gates, conditional Pronoun/Pair handling, the new release contract and Golden Replay G1–G10 are complete.
- Rejected development candidate `4.16-dev.23`/code85: Gradle and the APK used the exact SAFE4 bytes, but post-build inspection found Git source-archive EOL conversion changed the three `.txt` hashes. Both immutable payloads remain retained for audit; code85 is not an accepted reproducible build and must not be installed or used as evidence.
