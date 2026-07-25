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

## Release archives

Development build archives do not replace the release workflow. Release candidates and tagged releases must still satisfy the 14-step checklist and use `scripts/archive-release.ps1` with the complete QA and evidence payload.
