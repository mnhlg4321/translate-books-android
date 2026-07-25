# QA Report — v4.14 development builds

## Status

`PASS WITH KNOWN LIMITATIONS` for the `v4.14-dev.3` runtime-preview/rule-usage development scope.

This report does not declare a v4.14 product release. No release tag was created.

## Build under test

- Version name: `4.14-dev.3`
- Version code: `52`
- Event: `build-20260725-093258`
- Source commit: `e7a04d78749f0a474132c14f8d792a431d1a63e2`
- APK: `TranslateBooks-v4.14-dev.3-code52.apk`
- APK SHA-256: `3C732596E3552A092C5B0BE656BC690619B8A36C613DE3CED6B10EEE4B3462DF`

## Automated regression

Command:

```powershell
.\scripts\build-and-save.ps1 -Notes "Restore last accepted translation preview and exact per-chunk glossary/pronoun prompt usage." -Install
```

Results:

- JVM unit tests: 106 passed, 0 failed, 0 errors, 0 skipped.
- Android Lint: completed, 0 errors, 54 warnings.
- Debug APK assembly: passed.
- Direct `assembleDebug` without the versioned-build property: rejected with the required archive-first instruction.

## Archive verification

- Artifact: `artifacts/builds/v4.14-dev.3/build-20260725-093258/`
- Backup: `backup/builds/v4.14-dev.3/build-20260725-093258/`
- Artifact/backup file-name, size, and SHA-256 parity: passed.
- `SHA256SUMS.txt` verification: passed for every listed file.
- APK manifest: package `com.ml.tblandroidtxt`, versionName `4.14-dev.3`, versionCode `52`.
- Source archive SHA-256: `260CC6FCF0ABFAD02923FEB5C04201DEBF6818523F44E756282D804BDC444A9C`.
- Per-build `README.md` and `BUILD_INFO.json`: present and consistent with the APK.
- Archive-before-install rule: passed; installation returned `Success` only after both immutable copies were verified.

## Runtime preview and rule-usage device QA

- Exact device package metadata: `4.14-dev.3` / versionCode `52` on OnePlus CPH2691 / Android 15.
- Used a device-local runtime-state fixture only; no provider request, API key use, translation job, or billing occurred.
- Verified the visible values `2/3` current chunk, `8 rules` Glossary, `7 rules` Pronoun, and `Translating • chunk 2/3 • exact prompt usage`.
- Verified `LAST TRANSLATION PREVIEW`, `Accepted chunk 1/3`, and the accepted Vietnamese preview inside the bounded scrollable frame.
- UI hierarchy found every expected label/value; visual inspection confirmed no overlap with the fixed bottom navigation.
- Evidence screenshot: `artifacts/qa/v4.14-dev.3/runtime-preview-rule-usage.png`, SHA-256 `A7093B2F28F7E580A886E5D544F655C3AB3DA43ADCB00E0A8EB7FCC771A065F1`.
- Restored the pre-QA `runtime_state.xml` byte-for-byte; both source and restored SHA-256 were `7418ECF4BD11A4756787445F974532BADED26DE88665E6024BE04E891A3A7AC8`.

## Known limitations

- The full connected Android instrumentation suite was not rerun; focused JVM coverage, main/androidTest compilation during lint, and controlled on-device UI/state QA passed.
- No paid live translation was started for this UI restoration; exact count sourcing is covered by `PromptPlan` unit tests and the service uses the same structured fields at runtime.
- Lint retains 54 warnings and no errors.
- Release-only Perfetto, Macrobenchmark, screenshot, video, tag, and immutable release payload requirements remain pending and are not satisfied by this development build.
