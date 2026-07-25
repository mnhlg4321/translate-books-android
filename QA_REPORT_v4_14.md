# QA Report — v4.14 numbered-build archive workflow

## Status

`PASS WITH KNOWN LIMITATIONS` for the development-build archival scope.

This report does not declare a v4.14 product release. No release tag was created.

## Build under test

- Version name: `4.14-dev.2`
- Version code: `51`
- Event: `build-20260725-090328`
- Source commit: `b9e17cc328e594f317e99a1aae406bd9cef0171f`
- APK: `TranslateBooks-v4.14-dev.2-code51.apk`
- APK SHA-256: `BF034D2601F44A5739C53D8B9868A142FCF2FDC724006E0C029B631B83963C91`

## Automated regression

Command:

```powershell
.\scripts\build-and-save.ps1 -Notes "Glossary imports adopt source filenames while preserving custom names." -Install
```

Results:

- JVM unit tests: 103 passed, 0 failed, 0 errors, 0 skipped.
- Android Lint: completed, 0 errors, 54 warnings.
- Debug APK assembly: passed.
- Direct `assembleDebug` without the versioned-build property: rejected with the required archive-first instruction.

## Archive verification

- Artifact: `artifacts/builds/v4.14-dev.2/build-20260725-090328/`
- Backup: `backup/builds/v4.14-dev.2/build-20260725-090328/`
- Artifact/backup file-name, size, and SHA-256 parity: passed.
- `SHA256SUMS.txt` verification: passed for every listed file.
- APK manifest: package `com.ml.tblandroidtxt`, versionName `4.14-dev.2`, versionCode `51`.
- Source archive SHA-256: `D3CB891EEF77A31E03E8329D33A9AA14B5D8D2602C4B3D040C8811BDE673A628`.
- Per-build `README.md` and `BUILD_INFO.json`: present and consistent with the APK.
- Archive-before-install rule: passed; installation returned `Success` only after both immutable copies were verified.

## Glossary filename device QA

- Exact reported device package version: `4.14-dev.2` / versionCode `51`.
- In the existing `New glossary` editor, imported `codex_glossary_autoname_test.csv`.
- The editor import produced one valid term and changed the profile name exactly to `codex_glossary_autoname_test.csv`.
- The imported profile became active and the list reported `Active glossary: codex_glossary_autoname_test.csv • 1 terms`.
- After a force-stop and cold launch, both the exact profile name and active selection persisted.
- Restored the previously active `mer vol1 024` profile and removed the QA profile and device fixture.

## Known limitations

- Connected Android instrumentation was not run because this change targets the build/archive workflow rather than application behavior.
- Lint retains 54 warnings and no errors.
- Release-only Perfetto, Macrobenchmark, screenshot, video, tag, and immutable release payload requirements remain pending and are not satisfied by this development build.
