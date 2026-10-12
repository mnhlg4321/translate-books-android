# Versioned build and local archive

Every successful APK build must be created with:

```powershell
.\scripts\build-and-save.ps1
```

Do not use Android Studio **Build APK(s)** or run `assembleDebug` directly. Those paths are intentionally blocked because they can produce or install an APK without retaining a durable, numbered copy.

## What the command does

The command:

1. reads all previous local build records;
2. checks the version code currently installed on a connected phone, when available;
3. selects the next unused build sequence and a `versionCode` greater than every observed value;
4. runs unit tests, lint, and the debug APK build;
5. copies the APK out of `app/build/` immediately;
6. creates a new README, JSON metadata, SHA-256 manifest, and exact tracked-source ZIP;
7. writes the same immutable payload to both:
   - `artifacts/builds/v<version>/<event>/`
   - `backup/builds/v<version>/<event>/`
8. installs the already-archived APK only when `-Install` is supplied.

No existing event directory is overwritten or reused.

## AndroidTest-only archive

Host-only AndroidTest artifacts use the sibling archive path below. This path
builds only `:app:assembleDebugAndroidTest`; it never builds or installs the
production APK, never selects a device, and retains the same immutable payload
contract (README, `BUILD_INFO.json`, SHA-256 manifests, and tracked-source ZIP)
in both `artifacts/test-builds/` and `backup/test-builds/`.

```powershell
.\scripts\build-and-save-android-test.ps1 `
  -TargetProductionVersion 'v4.17-p5e.11' `
  -TargetProductionVersionCode 207 `
  -TargetProductionApkSha256 '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD' `
  -TargetProductionSourceZipSha256 'B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348' `
  -TargetProductionCertificateSha256 '47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155' `
  -EventId 'a4-1-test-YYYYMMDD-HHMMSS' `
  -ExpectedSourceCommit '<full-clean-HEAD>' `
  -JavaHome 'C:\Program Files\Android\Android Studio\jbr' `
  -AndroidSdkPath 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
```

The event ID must be new and the worktree must be clean. The script performs
JDK/SDK, package, target-package, runner, certificate, source-commit, and
artifact/backup byte checks. It does not use ADB and does not provide an
installation switch.

## Normal development build

```powershell
.\scripts\build-and-save.ps1
```

The default series is `4.14-dev`. Builds are numbered consecutively:

```text
4.14-dev.1
4.14-dev.2
4.14-dev.3
```

Each build also receives an increasing Android `versionCode`.

## Build and then install

```powershell
.\scripts\build-and-save.ps1 `
  -Install `
  -DeviceSerial '15e84958' `
  -ExpectedDeviceSignatureToken 'abebea4b' `
  -ExpectedApkCertificateSha256 '47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155'
```

Archiving always finishes before installation begins. If installation fails, the APK and its records remain safely stored.

## Custom series and notes

```powershell
.\scripts\build-and-save.ps1 -Series 4.15-dev -Notes "Output picker regression fix"
```

Use a new series when starting the next development line. Never rename or overwrite an existing build directory.

## Exact release build

After the release metadata is committed and the release checklist is ready for final regression, build the exact public version from its matching clean feature branch:

```powershell
.\scripts\build-and-save.ps1 -ExactReleaseVersion 4.14 -Notes "Translate Books 4.14 release build."
```

Exact release mode:

- produces `versionName 4.14` rather than `4.14.1`;
- assigns the next unused Android `versionCode`;
- requires the matching `feature/v4.14` branch and a clean working tree;
- rejects an existing `v4.14` tag;
- permits a new pre-tag `4.14` candidate after failed QA, while retaining every prior candidate under a unique event with an increasing versionCode;
- still runs tests, lint, logo verification, archive/backup creation, and optional installation in the normal archive-first order.

Do not combine `-ExactReleaseVersion` with `-Series`.

## Release Macrobenchmark

The project includes a dedicated `:macrobenchmark` test module. It measures five cold starts of the non-debuggable `benchmark` app variant and produces AndroidX Benchmark JSON plus Perfetto traces:

```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest `
  -PversionedBuild=true `
  -PbuildVersionName=4.14 `
  -PbuildVersionCode=54
```

Run this on a physical Android 14 or newer device against the exact release source. Copy the generated JSON and trace files out of `macrobenchmark/build/` immediately; Gradle build directories are temporary and do not satisfy release evidence retention.

## Release archives

Development build archives do not replace the release workflow. Release candidates and tagged releases must still satisfy the 14-step checklist and use `scripts/archive-release.ps1` with the complete QA and evidence payload.
