# QA Report — v4.14

## Status

`PASS` for release `4.14` / versionCode `57`.

Annotated tag `v4.14` was created at release-document commit `8a3b281619fc7dc48da6b1e36fd19071a90a8223` only after the PreTag gate passed. The immutable tag archive was then created and verified in both durable stores.

## Build under test

- Version name: `4.14`
- Version code: `57`
- Event: `build-20260725-111133`
- Branch: `feature/v4.14`
- Source commit: `82d5e4a677bc147c083fdcc348401606fd1e9412`
- APK: `artifacts/builds/v4.14/build-20260725-111133/TranslateBooks-v4.14-code57.apk`
- APK size: `2,515,055` bytes
- APK SHA-256: `D478AC135ED3C736F7FC850FAA06CA4F6F5CB6D0634BB09B15BD59C9B2DAE5D1`
- Exact source ZIP SHA-256: `8EBB557679EA526592D8A9B5D5D0594B46CA7BD298C793454AC0CD4382D9FAA7`

The mandatory build workflow archived and verified both durable copies before installing the APK.

## Automated regression

- Approved-logo `preBuild` guard: passed.
- JVM tests: `106` passed, `0` failed, `0` errors, `0` skipped across 21 suites.
- Android Lint: `0` errors, `54` warnings.
- APK assembly, archive, checksum verification, and device installation: passed.
- Connected Android instrumentation: `12` total cases, `11` passed and `1` opt-in paid real-API case skipped; `0` failures/errors.
- No provider request, API billing, or translation job was started during final release QA.

## Archive verification

- Artifact: `artifacts/builds/v4.14/build-20260725-111133/`
- Backup: `backup/builds/v4.14/build-20260725-111133/`
- Artifact/backup filename, size, and SHA-256 parity: passed for all five build payload files.
- Manifest verification: all entries in `SHA256SUMS.txt` matched.
- APK metadata: package `com.ml.tblandroidtxt`, versionName `4.14`, versionCode `57`.
- Source archive ref: exact commit `82d5e4a677bc147c083fdcc348401606fd1e9412`.

## Immutable tag archive

- Artifact: `artifacts/releases/v4.14/tag-v4.14/`.
- Backup: `backup/v4.14/tag-v4.14/`.
- Tag object: `246de3416b247379a3e3e749a1a6d3f727a9a685`.
- Tag target: `8a3b281619fc7dc48da6b1e36fd19071a90a8223`.
- Files: `20` in each durable copy; all match by relative path, length, and SHA-256.
- `SHA256SUMS.txt`: all `19` listed payload entries verified.
- Release source ZIP SHA-256: `5F2DBA2C1D4F806A838961144807228C22C1846070296FACD6915A3DC2B43E5B`.
- A fresh `git archive` of `v4.14` produced the same source ZIP SHA-256.
- The archive includes the APK, QA report, CHANGELOG, BUILD_STATE, RELEASE_NOTES, five Perfetto traces, Macrobenchmark JSON/log, three screenshots, UI hierarchy XML, device-navigation AVI, source ZIP, manifest, and checksums.

## Physical-device Macrobenchmark

Device: OnePlus CPH2691, Android 15 / API 35.

The accepted rerun completed five cold starts of `com.ml.tblandroidtxt.MainActivity`:

- Runs: `337.52`, `330.30`, `307.78`, `310.93`, `322.81` ms.
- Minimum: `307.78` ms.
- Median: `322.81` ms.
- Maximum: `337.52` ms.
- Coefficient of variation: `3.91%`.
- Tests: `1` passed, `0` failed, `0` skipped.
- Thermal-throttle sleep: `0` seconds.
- Evidence: AndroidX Benchmark JSON plus five real Perfetto traces under `artifacts/releases/v4.14/qa-code57-20260725-1117/macrobenchmark/`.
- JSON SHA-256: `ED209CF867B1FB80F915C569A4D883344D5F1C6334AA726A1AE03AEC87B02E92`.
- The seven-file evidence set matches its backup copy by filename, length, and SHA-256.

An earlier code57 run completed but contained one `109,774 ms` first-iteration outlier. It is retained under `qa-code57-20260725-1115` for audit and rejected as final performance evidence; it was not silently discarded or reported as representative.

## Controlled output-permission-loss proof

A local TXT fixture was prepared successfully, a dummy non-billable API-key value was used, and the output tree was replaced with a controlled SAF URI that had no persisted write grant.

Observed behavior:

- The exact blocker was `Output folder: chưa có quyền ghi lâu dài. Hãy chọn lại output folder.`
- The visible folder label changed to `Output folder permission lost`.
- The folder action changed to `Choose output folder again`.
- The UI hierarchy recorded the `Start` button with `enabled=false`.
- No provider request or output write occurred.
- Screenshot: `artifacts/releases/v4.14/qa-code57-20260725-1126-output-permission-ready/output-permission-lost-ready-input.png`.
- Screenshot SHA-256: `543AC713CF7385CE094E4857EB1AA555A0642E6C045F7102F1A0E7F34983886C`.
- UI hierarchy SHA-256: `7B53E2F7D6B2ACF1430341E305A703FB3889AD66BC653F335DB0BE780EAB3ACE`.
- Both evidence files match the backup copy by filename, length, and SHA-256.

The fixture and all QA-created app files were removed afterward. Because the OEM denied the standard `pm clear` command, cleanup used `run-as` against the explicit QA-created internal files; exact `4.14` / code57 remains installed in a clean state.

## Visual evidence

- Translate dashboard screenshot: `artifacts/releases/v4.14/qa-code57-20260725-1131-device-visual/screenshots/translate-dashboard.png`.
- Library Files screenshot: `artifacts/releases/v4.14/qa-code57-20260725-1131-device-visual/screenshots/library-files.png`.
- Device-navigation video: `artifacts/releases/v4.14/qa-code57-20260725-1131-device-visual/video/code57-device-navigation-mjpeg.avi`.
- The AVI contains 24 actual device screenshots at 2 fps, 632×1390, with a valid RIFF/AVI length and index.
- Video SHA-256: `096958F7E366E058A57E4FDC4D9A596EE3327EF13E63A04D8A3038FF185658A1`.
- All three visual files match the backup copy by path, length, and SHA-256.

## Known limitations

- Android Lint retains 54 warnings and no errors.
- OnePlus Android 15 denies shell `screenrecord`; the retained AVI is an honest timed sequence of actual device screenshots during four-tab navigation, not a continuous MediaProjection recording.
- AndroidX reports `run-from-apk` compilation mode for the retained startup benchmark.
- The opt-in paid real-API instrumentation case was intentionally skipped; final QA made no provider request and incurred no billing.
- Macrobenchmark installation replaces the target package data. The exact product APK was reinstalled afterward and left clean at `4.14` / code57.
