# Translate Books 4.17 Development Artifact

Status: `D5_LOCAL_BUILD_QA_COMPLETE / V4.17_DEV_ARTIFACT_READY`

This recovery build continues the code113 translation workflow on the historical
recovery branch `feature/v4.17-translation-profile-compatibility`. It is a numbered
development artifact, not a public `v4.17` release or tag.

## Scope

- Restore chapter-by-chapter translation from the code113 source baseline.
- Glossary runtime uses `source,target,category,note`; a fifth `priority` column is
  tolerated for file review and ignored at runtime.
- Pronoun runtime uses
  `from,speaker,target,self,call,scope,note`, while legacy three-column input remains
  supported.
- Matched-only compact prompt locks preserve Glossary notes and scoped Pronoun
  rules through translation, refinement, settings snapshots, resume and retry.
- RSC, Editorial and IPC workflows are not activated by this recovery.

## Known limitations

- Multiline quoted CSV fields remain unsupported by the existing line-based parser.
- Malformed Pronoun P3 scopes fail closed.

## Local artifact

- Version: `4.17-dev.1` / Android versionCode `169`.
- Build event: `build-20260829-185818`.
- Build commit: `b0ab983bd4f9b017819af2bb0043bc57caad807a`.
- APK: `TranslateBooks-v4.17-dev.1-code169.apk`, 2,784,446 bytes, SHA-256
  `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1`.
- Source ZIP: `project_source_build-20260829-185818.zip`, SHA-256
  `AA3A75CF4CFC4C527352F037D3AABEC9BBF24517138F58DB974794CEB67EAA6F`.
- Matching immutable payloads:
  `artifacts/builds/v4.17-dev.1/build-20260829-185818/` and
  `backup/builds/v4.17-dev.1/build-20260829-185818/`.
- Local QA: 205 JVM tests passed; Lint 0 errors/53 warnings; package and SDK
  manifest metadata verified statically.
- Device QA: not run because no device was connected; no provider/API request was
  sent.

This is a development artifact, not a public release or tag. Public tag/archive,
device QA, benchmark, Perfetto, screenshot and video evidence remain pending.

---

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

The source history and corrected annotated `v4.15` tag are published to the
private GitHub repository. The exact APK and immutable release evidence remain
outside Git by design in matching `artifacts/releases/v4.15/tag-v4.15/` and
`backup/v4.15/tag-v4.15/` payloads and must be distributed separately.
