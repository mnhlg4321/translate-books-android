# QA Report — v4.14 development builds

## Status

`PASS WITH KNOWN LIMITATIONS` for the `v4.14-dev.4` permanent-logo development scope.

This report does not declare a v4.14 product release. No release tag was created.

## Build under test

- Version name: `4.14-dev.4`
- Version code: `53`
- Event: `build-20260725-095315`
- Source commit: `ca51d7e27066083c79ac03db5ea07ceb8d0af8a8`
- APK: `TranslateBooks-v4.14-dev.4-code53.apk`
- APK SHA-256: `3CA3A64327A9F1C413505151259D540A9A4C58AD67D3D4616B6F896628AF4038`

## Automated regression

Command:

```powershell
.\scripts\build-and-save.ps1 -Notes "Permanently install the approved bright cool logo as launcher, round, and in-app brand icon with a preBuild hash guard." -Install
```

Results:

- `verifyApprovedLogo` passed through `preBuild`.
- JVM unit tests: 106 passed, 0 failed, 0 errors, 0 skipped.
- Android Lint: completed, 0 errors, 54 warnings.
- Debug APK assembly and device installation: passed.
- The approved logo source/resource SHA-256 is `104D17792425810F4E7419BFEA268505DECA6148D04C7AC852D646245CD61797`.

## Archive verification

- Artifact: `artifacts/builds/v4.14-dev.4/build-20260725-095315/`
- Backup: `backup/builds/v4.14-dev.4/build-20260725-095315/`
- Artifact/backup file-name, size, and SHA-256 parity: passed for all five payload files.
- `SHA256SUMS.txt` verification: passed for every listed file.
- APK manifest: package `com.ml.tblandroidtxt`, versionName `4.14-dev.4`, versionCode `53`.
- APK contains `res/drawable-nodpi-v4/translate_books_logo.png` with the exact approved SHA-256.
- Packaged `android:icon` and `android:roundIcon` both resolve to `drawable/translate_books_logo`.
- Source archive SHA-256: `90993F81E07C826ADC7BD6E64D93AC6347977EA999C6A1D675EF43924C2CB77E`.
- Archive-before-install rule: passed; installation ran only after both immutable copies were verified.

## Device logo QA

- Exact device package metadata: `4.14-dev.4` / versionCode `53` on OnePlus CPH2691 / Android 15.
- The launcher displays the approved bright cool icon with the Translate Books label.
- The in-app top bar displays the approved logo; the UI hierarchy exposes content description `Translate Books logo`.
- Visual inspection confirmed the header logo is clear and does not overlap adjacent content.
- Launcher evidence: `artifacts/qa/v4.14-dev.4/launcher-logo.png`, SHA-256 `439E095941E13492BC6C4D7656F625DA52C01F47C63FAE7C404BB51D67121F31`.
- In-app evidence: `artifacts/qa/v4.14-dev.4/in-app-logo.png`, SHA-256 `4ED083E2273DD41690B053CF2F29E08368C529705A1FE0319F8172E6CDABDC69`.
- No provider request, API key use, translation job, or billing occurred.
- User runtime state remained byte-for-byte unchanged with SHA-256 `7418ECF4BD11A4756787445F974532BADED26DE88665E6024BE04E891A3A7AC8`.

## Known limitations

- The full connected Android instrumentation suite was not rerun; focused JVM coverage, lint compilation, and controlled on-device logo/state QA passed.
- Lint retains 54 warnings and no errors.
- Release-only Perfetto, Macrobenchmark, screenshot, video, tag, and immutable release payload requirements remain pending and are not satisfied by this development build.
