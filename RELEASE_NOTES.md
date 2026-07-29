# Translate Books 4.15 Release Notes

Release date: 2026-07-29

Git tag: `v4.15`

## Summary

Version 4.15 fixes three workflow problems reported on the phone UI: selecting a
Glossary or Pronoun no longer sends the list back to the top, list imports can
select several files and create one independent profile per file, and saved
Provider, Prompt, and Performance settings are protected from being overwritten
during lazy screen construction, refresh, reset navigation, or app reopening.

## Highlights

- Preserve the exact Glossary and Pronoun list scroll position when `Use` changes
  the active profile.
- Keep editor navigation's intentional top-of-page behavior separate from
  list-level selection.
- Enable Android multi-select for list-level Glossary and Pronoun imports.
- De-duplicate returned URIs while preserving selection order.
- Persist every valid selected file as a separate profile with its own ID and
  source filename; activate the first valid imported profile.
- Report invalid siblings independently without discarding valid imports.
- Keep Pronoun replacement single-select and label intentional Glossary editor
  combination as `Merge files`.
- Hydrate lazy Settings sections from persisted values before any control can
  write defaults.
- Save Settings safely through normal lifecycle transitions without clearing
  Provider, Prompt, or Performance values.

## Pre-tag verification

- Exact archive-first APK: `4.15`/code62, SHA-256
  `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`.
- Exact source ZIP SHA-256:
  `FEFEDF516FD85B3166C6BB36249C22C94909D4428844EAC5E8E811F06015F5EE`.
- Approved-logo guard and all 110 JVM tests passed.
- Android Lint completed with 0 errors and 53 warnings.
- Exact-code62 connected instrumentation completed with 15 passed, 1 explicitly
  opt-in paid real-API case skipped, and 0 failures/errors.
- Physical-device AndroidX Macrobenchmark passed all five cold starts at
  313.89/330.23/335.04 ms minimum/median/maximum and retained five real Perfetto
  traces.
- Five device screenshots, matching UI hierarchy dumps, and a documented
  25-frame MJPEG device sequence were retained in matching QA artifact/backup
  stores.

Final counts, hashes, artifact locations, benchmark measurements, and known
limitations are recorded in `QA_REPORT_v4_15.md`.

## Known limitations

- Android Lint retains 53 warnings and no errors.
- The paid real-API instrumentation case remains explicitly opt-in and was not
  run during release QA; no provider request or billing occurred.
- OnePlus Android 15 denies shell `screenrecord`. The retained AVI is explicitly
  a timed sequence of actual-device screenshots, not continuous MediaProjection
  capture.
- AndroidX reports `run-from-apk` compilation mode for the startup benchmark.

## Distribution

The exact APK and release evidence remain outside Git by design. After the
annotated tag passes the guarded workflow, the immutable `tag-v4.15` payload must
be created and verified in both `artifacts/releases/v4.15/` and
`backup/v4.15/` before publication.
