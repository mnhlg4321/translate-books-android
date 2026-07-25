# Build State

## Current release candidate build

- versionName: `4.14`
- versionCode: `56`
- event: `build-20260725-103832`
- branch: `feature/v4.14`
- source commit: `59b20f1c6d26309ccb415bbfe5c7c4e2998a7548`
- APK: `artifacts/builds/v4.14/build-20260725-103832/TranslateBooks-v4.14-code56.apk`
- immutable local mirror: `backup/builds/v4.14/build-20260725-103832/`
- APK SHA-256: `C6C0E8B263D22781E62235AC17A9FBFAF4EAD4812E363C5664E824014DF1E39F`
- source ZIP SHA-256: `AA3AE415CF91E05312FBFF5A212331358FD81C3256149C58F6847BDACDEEBEB1`
- regression: approved-logo guard and 106 JVM tests passed; Android Lint completed with 54 warnings and 0 errors; connected instrumentation passed 12 tests with 1 opt-in real-API test skipped. The archived code56 source still allowed OnePlus Hans to freeze the background benchmark runner. A working-tree diagnostic then completed all five correct `MainActivity` cold starts with real JSON/Perfetto evidence after adding a foreground benchmark host and suppressing only the benchmark variant's first-run notification dialog.
- device state: OnePlus CPH2691 / Android 15 completed the corrected five-iteration diagnostic. Time to initial display was 293.57/332.01/441.19 ms min/median/max with 0 skipped and 0 failed tests.
- release state: untagged release candidate; code56 is retained but rejected because the successful Macrobenchmark fix is newer than its archived source commit. A new immutable exact candidate is required.

## Release identity

- versionName: `4.8` (released locally; previous release was `v4.7`)
- versionCode: `47`
- build date: `2026-07-17 20:22:06 +07:00`
- Gradle: `9.3.0` wrapper; Android Gradle Plugin `8.7.3`
- Android Studio: `2026.1.1` (`AI-261.23567.138.2611.15646644`)
- compileSdk: `35`
- targetSdk: `35`
- commit hash: `21fc148c561dac395e85d19cd48fef6c378da3ab` (annotated tag `v4.8`)
- branch: `main` (synchronized to `https://github.com/manhluongvd/translate-books-android.git`; tag `v4.8` remains immutable)

## Existing artifacts

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

`PASS WITH KNOWN LIMITATIONS` for v4.8:

- Build: successful (`assembleDebug`).
- JVM unit tests: 99 passed, 0 failed, 0 skipped.
- Android instrumentation: 11 required offline/device cases passed, 0 failed on OnePlus CPH2691 / Android 15.
- Opt-in real API harness: 2 real requests passed with both lock types asserted in each provider prompt.
- Glossary/pronoun persistence, cold-store reload, Back navigation, and four-tab gestures: passed.

The last released v4.7 evidence remains `PASS WITH KNOWN LIMITATIONS`:

- Build: successful (`clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`; 73 tasks).
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Lint: 0 errors, 50 warnings.
- Core translation and recovery regression coverage: passed within the scope recorded in `QA_REPORT_v4_7.md`.

The v4.8 tag and `main` branch are published to the private GitHub repository. Remote verification resolves `main` to `c1e807b3bad1b64f079d0a802c3b0a2e7621c0c2`, annotated tag object `e5e6bb8aec6c713edf87354b8b76d21e004ea726`, and tag target `21fc148c561dac395e85d19cd48fef6c378da3ab`.

## Known issues

- Warm tab-switch P50 is `20 ms`, above the `16 ms` target; P95, P99, long-frame, and PSS targets passed.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.
- The 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch; the patch did not alter the four primary scroll containers.
- Lint reports 50 warnings and no errors.
- v4.8 APK/source ZIP archives remain intentionally outside the tracked Git tree under ignored `artifacts/releases/` and `backup/`; a clean clone contains the release documents but not these binary archives.
- Android 15 shell screenrecord was denied; the archive records a fresh screenshot/Perfetto trace and clearly labels the retained visual video reference.
