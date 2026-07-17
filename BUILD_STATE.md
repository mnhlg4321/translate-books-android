# Build State

## Release identity

- versionName: `4.8` (development; last released tag remains `v4.7`)
- versionCode: `47`
- build date: `2026-07-17 20:12:29 +07:00`
- Gradle: `9.3.0` wrapper; Android Gradle Plugin `8.7.3`
- Android Studio: `2026.1.1` (`AI-261.23567.138.2611.15646644`)
- compileSdk: `35`
- targetSdk: `35`
- commit hash: `e4b9dfde53fdefa9f7a3934dfd6ec604c0e5d565` (application/test baseline before the opt-in API harness)
- branch: `fix/v4.8-import-navigation`

## Existing artifacts

- v4.8 development APK: `artifacts/releases/v4.8/dev-e4b9dfd-20260717-201229/TranslateBooks-v4.8-debug.apk`
  - Size: `1,626,871` bytes
  - SHA-256: `ABED4892CEFA9F3363F85ED8CF57F4D06C388447EE8F37617683FBB6DF891FB3`
  - Mirrored without overwrite at `backup/v4.8/dev-e4b9dfd-20260717-201229/`.

- Release candidate APK: `artifacts/v47/TranslateBooks-v4.7-debug.apk`
  - Size: `1,625,315` bytes
  - SHA-256: `BBA8152197FCE0310236899BA313B7C285E91FA1299D703EEF4BB23FA316EE4F`
  - The APK is present locally but excluded from Git by `*.apk` in `.gitignore`.
- Local build APK: `app/build/outputs/apk/debug/TranslateBooks-v4.7-debug.apk` (excluded from Git).
- Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (excluded from Git).
- Versioned QA evidence: `artifacts/v47/`, including unit-test, instrumentation, lint, screenshots, video, Perfetto traces, performance report, and checksums.

## Regression status

`IN PROGRESS / API VERIFICATION BLOCKED` for v4.8:

- Build: successful (`assembleDebug`).
- JVM unit tests: 99 passed, 0 failed, 0 skipped.
- Android instrumentation: 11 required offline/device cases passed, 0 failed on OnePlus CPH2691 / Android 15.
- Opt-in real API harness: safely skipped in normal regression and blocked when explicitly enabled because the installed app has no configured API key.
- Glossary/pronoun persistence, cold-store reload, Back navigation, and four-tab gestures: passed.

The last released v4.7 evidence remains `PASS WITH KNOWN LIMITATIONS`:

- Build: successful (`clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`; 73 tasks).
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed, 0 failed, 0 skipped on OnePlus CPH2691 / Android 15.
- Lint: 0 errors, 50 warnings.
- Core translation and recovery regression coverage: passed within the scope recorded in `QA_REPORT_v4_7.md`.

No v4.8 tag or release completion is claimed until the two-request real API verification passes.

## Known issues

- Warm tab-switch P50 is `20 ms`, above the `16 ms` target; P95, P99, long-frame, and PSS targets passed.
- Performance evidence uses Perfetto/frame/PSS measurements rather than a dedicated Macrobenchmark module.
- The 80-swipe gesture campaign was not repeated after the final Back/pronoun cache-invalidation patch; the patch did not alter the four primary scroll containers.
- Lint reports 50 warnings and no errors.
- GitHub push attempts to the approved `manhluongyd/translate-books-android` origin currently time out without a response.
- Real API verification requires an API key to be configured again on the installed debug app.
