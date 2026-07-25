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
.\scripts\build-and-save.ps1 -Install
```

Archiving always finishes before installation begins. If installation fails, the APK and its records remain safely stored.

## Custom series and notes

```powershell
.\scripts\build-and-save.ps1 -Series 4.15-dev -Notes "Output picker regression fix" -Install
```

Use a new series when starting the next development line. Never rename or overwrite an existing build directory.

## Exact release build

After the release metadata is committed and the release checklist is ready for final regression, build the exact public version from its matching clean feature branch:

```powershell
.\scripts\build-and-save.ps1 -ExactReleaseVersion 4.14 -Notes "Translate Books 4.14 release build." -Install
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
