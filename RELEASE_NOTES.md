# Translate Books 4.18.0 Release Notes

Status: `RELEASE_CANDIDATE` (not tagged, not merged to `main`)

Branch: `feature/v4.18-p5e-runner-repair-20260917`

## Summary

Version 4.18.0 releases the chunk-based Editorial (Bien tap) flow. The app aligns a
chapter's RAW and DRAFT, cuts the chapter into chunks, shows the plan with a verdict,
and, after confirmation, reviews each chunk and applies fixes only at the points it
flags. The earlier V5 three-pass path stays in the code as a frozen path and is not
part of this release.

## Scope

- Source selection and plan: RAW/DRAFT alignment, chunk plan, OK / WARN / BLOCK
  verdict shown before any provider request.
- Point-based fixes: address, glossary and kana points are flagged; a model proposal
  is accepted only when the deterministic guard passes, otherwise the DRAFT line is
  kept.
- Persistence: runs are stored in SQLite (database version 29) and survive restart and
  process death; the result is exported as TXT.
- Spend control: ledger and per-run cap; a plan must be confirmed first.
- Translation, Glossary, Pronoun and Settings are unchanged by this work.

## Known limitations

- Three chapters were measured and accepted by the owner. Their recorded misses stay:
  006 keeps 13 pronoun misses and the `cau` misses (CP-IMPL-9); 008 keeps 7 `cau`
  misses; 011 keeps 2:2 (correct fix blocked by the third-person rule), 12:2
  (uncertain) and 10:6 (missed). Details: `docs/EDITORIAL_CHUNK_PLAN.md` section 6.
- The call-name prompt (CP-IMPL-14) was measured on chapter 011 only; fixes
  CP-IMPL-10 to 13 and 15 are verified by tests and replays, not by a live run.
- Performance numbers come from the emulator, not a physical device.
- The V5 three-pass gates in section C of the v4.18 checklist are out of this
  release's scope; they stay unchecked and are not waived.

## Verification

- Frozen allowlist: 117/117 on `emulator-5554` after R1.1.
- Offline on a clean archive of `12824cee`: engine 706, app 472 per variant, lint
  0 errors / 57 warnings, `scripts/p6` 106, `scripts/chunk` 24.
- Device QA of the chunk scope: `artifacts/qa/v4.18-p7-device-12824cee/QA_REPORT.md`.
- Macrobenchmark and Perfetto: `artifacts/qa/v4.18-r1/r1.3-performance/`.
- Release-candidate device QA and the release payload: see the R1.5 entry in
  `docs/EDITORIAL_CHUNK_PLAN.md` section 6.
- Screenshots and video use synthetic data only; they contain no book text.

## Local artifact

The release-candidate APK, source ZIP and checksums are recorded in
`artifacts/releases/v4.18.0/<event>/` and `backup/v4.18.0/<event>/` by
`scripts/archive-release.ps1` (R1.5). Tag `v4.18.0` and the merge to `main` need the
owner's decision (D-R2).

---

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
