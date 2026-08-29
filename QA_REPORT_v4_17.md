# Translate Books v4.17 QA Report

Status: `PRE-BUILD`

## Authority

- Branch: `feature/v4.17-translation-profile-compatibility`
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7`
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`
- Expected development series: `4.17-dev.N`
- Requested minimum Android versionCode: `169`
- Pre-build commit: pending (the current clean source before the pre-build metadata commit is `080d9d3611a1d13b7be6826d55c789ab8e54c3b6`)

## Pre-build scope

The D5 artifact will be created only through `scripts/build-and-save.ps1`. The
recovery implementation covers Glossary4, Pronoun7, matched-only compact prompt
locks, scoped refinement, settings snapshot/resume/retry and hermetic Mercedes
CH001/CH004 prompt/estimator evidence from D4.

The following are intentionally not run or activated in this local phase:

- device/manual QA, Android instrumentation, emulator/AVD or benchmark;
- real provider/API requests;
- RSC, Editorial or IPC workflows;
- public release/tag archive or release-complete gate.

## Pending evidence

This report will be updated after the pre-build commit, numbered local build and
static APK QA with exact test totals, lint/assemble results, manifest metadata,
artifact/backup hashes, prompt QA and known limitations.

## Pre-build regression evidence

Environment:

- JDK: `C:\Program Files\Android\Android Studio\jbr` (JDK 21)
- `ANDROID_HOME` / `ANDROID_SDK_ROOT`:
  `C:\Users\ADMIN\AppData\Local\Android\Sdk`

Focused integration command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.TranslationProfileIntegrationTest --rerun-tasks --console=plain
```

Result: `11 tests, 0 failures, 0 errors, 0 skipped` (PASS).

Full JVM command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain
```

Result: `205 tests, 0 failures, 0 errors, 0 skipped` (PASS).

Test XML: `app/build/test-results/testDebugUnitTest/` (40 XML files; the focused
class report is `TEST-com.ml.tblandroidtxt.TranslationProfileIntegrationTest.xml`).

Pre-build script checks: PowerShell AST parse PASS; invalid
`-MinimumVersionCode 0` rejection PASS; `git diff --check` PASS; application-source
diff count `0`. No APK, device, provider/API, RSC, Editorial or IPC action has been
performed at this point.
