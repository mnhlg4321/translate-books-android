# QA Report — v4.14 numbered-build archive workflow

## Status

`PASS WITH KNOWN LIMITATIONS` for the development-build archival scope.

This report does not declare a v4.14 product release. No release tag was created.

## Build under test

- Version name: `4.14-dev.1`
- Version code: `50`
- Event: `build-20260725-084757`
- Source commit: `702283870ace921d33d4c69fbab32446b5ab97d4`
- APK: `TranslateBooks-v4.14-dev.1-code50.apk`
- APK SHA-256: `39DE793F31377BAF0AA1AE5464A4A039099CA1C76B4E0127FA89CFE9CAE9B30B`

## Automated regression

Command:

```powershell
.\scripts\build-and-save.ps1 -Notes "First verified build using the mandatory numbered archive workflow."
```

Results:

- JVM unit tests: 99 passed, 0 failed, 0 errors, 0 skipped.
- Android Lint: completed, 0 errors, 54 warnings.
- Debug APK assembly: passed.
- Direct `assembleDebug` without the versioned-build property: rejected with the required archive-first instruction.

## Archive verification

- Artifact: `artifacts/builds/v4.14-dev.1/build-20260725-084757/`
- Backup: `backup/builds/v4.14-dev.1/build-20260725-084757/`
- Artifact/backup file-name, size, and SHA-256 parity: passed.
- `SHA256SUMS.txt` verification: passed for every listed file.
- APK manifest: package `com.ml.tblandroidtxt`, versionName `4.14-dev.1`, versionCode `50`.
- Source archive: 323 entries; contains `scripts/build-and-save.ps1`.
- Source archive SHA-256: `2F0EED5B43A52F69F5633455525B499097AC7F0930354CE2CA71853DC7021F82`.
- Per-build `README.md` and `BUILD_INFO.json`: present and consistent with the APK.
- Archive-before-install rule: passed; no install was requested or attempted.

## Known limitations

- Connected Android instrumentation was not run because this change targets the build/archive workflow rather than application behavior.
- The connected phone intentionally remains on v4.9/versionCode 49.
- Lint retains 54 warnings and no errors.
- Release-only Perfetto, Macrobenchmark, screenshot, video, tag, and immutable release payload requirements remain pending and are not satisfied by this development build.
