# Build State

## Current release build

- versionName: `4.14`
- versionCode: `57`
- event: `build-20260725-111133`
- branch: `feature/v4.14`
- source commit: `82d5e4a677bc147c083fdcc348401606fd1e9412`
- APK: `artifacts/builds/v4.14/build-20260725-111133/TranslateBooks-v4.14-code57.apk`
- immutable local mirror: `backup/builds/v4.14/build-20260725-111133/`
- APK SHA-256: `D478AC135ED3C736F7FC850FAA06CA4F6F5CB6D0634BB09B15BD59C9B2DAE5D1`
- source ZIP SHA-256: `8EBB557679EA526592D8A9B5D5D0594B46CA7BD298C793454AC0CD4382D9FAA7`
- regression: approved-logo guard and 106 JVM tests passed; Android Lint completed with 54 warnings and 0 errors; connected instrumentation passed 11 cases with 1 opt-in paid real-API case skipped. The accepted five-iteration Macrobenchmark rerun passed with 307.78/322.81/337.52 ms min/median/max and retained real JSON plus five Perfetto traces.
- device state: exact `4.14`/code57 is installed clean on OnePlus CPH2691 / Android 15. Controlled missing-output-grant QA showed the exact blocker and disabled Start with a valid prepared input; all QA-created state was removed afterward.
- release state: exact release build accepted, annotated tag `v4.14` created at `8a3b281`, and immutable `tag-v4.14` artifact/backup verified. Merge to `main` and remote push remain pending.

## Release identity

- versionName: `4.14`
- versionCode: `57`
- build event: `build-20260725-111133`
- Gradle: `9.3.0` wrapper; Android Gradle Plugin `8.7.3`
- Android Studio: `2026.1.1` (`AI-261.23567.138.2611.15646644`)
- compileSdk: `35`
- targetSdk: `35`
- implementation source commit: `82d5e4a677bc147c083fdcc348401606fd1e9412`
- release-document commit: `8a3b281619fc7dc48da6b1e36fd19071a90a8223` (annotated tag `v4.14`)
- branch: `feature/v4.14` (local release complete; merge and remote synchronization pending)

## Existing artifacts

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

`PASS WITH KNOWN LIMITATIONS` for v4.14:

- Archive-first build, approved-logo guard, APK metadata, installation, and checksum verification: passed.
- JVM unit tests: 106 passed, 0 failed, 0 errors, 0 skipped.
- Android Lint: 0 errors, 54 warnings.
- Connected instrumentation: 11 passed, 1 opt-in paid real-API case skipped, 0 failures/errors on OnePlus CPH2691 / Android 15.
- Controlled output-permission-loss QA: passed with a prepared TXT, exact recovery message, Start disabled, and no provider request.
- Physical-device Macrobenchmark: 5/5 cold starts passed; 307.78/322.81/337.52 ms minimum/median/maximum with five retained Perfetto traces.
- Immutable tag archive: both 20-file copies and all 19 manifest entries passed; the source ZIP exactly matches a fresh `git archive` of `v4.14`.

The previous v4.8 release evidence remains `PASS WITH KNOWN LIMITATIONS`:

- Build, 99 JVM tests, 11 required offline/device instrumentation cases, and two opt-in real requests passed.
- Glossary/pronoun persistence, cold-store reload, Back navigation, and four-tab gestures passed.

The last released v4.7 evidence remains `PASS WITH KNOWN LIMITATIONS`:

- Build: successful (`clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`; 73 tasks).
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Lint: 0 errors, 50 warnings.
- Core translation and recovery regression coverage: passed within the scope recorded in `QA_REPORT_v4_7.md`.

The v4.8 tag and `main` branch are published to the private GitHub repository. Remote verification resolves `main` to `c1e807b3bad1b64f079d0a802c3b0a2e7621c0c2`, annotated tag object `e5e6bb8aec6c713edf87354b8b76d21e004ea726`, and tag target `21fc148c561dac395e85d19cd48fef6c378da3ab`.

## Known issues

- Android Lint reports 54 warnings and 0 errors.
- OnePlus Android 15 denies shell `screenrecord`; the retained AVI is a documented 24-frame sequence of timed actual-device screenshots, not a continuous MediaProjection capture.
- AndroidX reports `run-from-apk` compilation mode for the startup benchmark.
- The opt-in paid real-API instrumentation case was intentionally skipped; release QA made no provider request and incurred no billing.
- v4.14 APK/source/evidence archives remain intentionally outside tracked Git under ignored `artifacts/releases/` and `backup/`; a clean clone contains release documents but not these binary payloads.
