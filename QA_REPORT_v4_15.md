# QA Report — v4.15

## Status

`LOCAL RELEASE WORKFLOW COMPLETE; MERGE/PUBLICATION PENDING` for release candidate `4.15` /
versionCode `62`.

All three v4.15 correction groups are implemented and covered: Settings persistence,
Glossary/Pronoun scroll preservation, and independent multi-profile file import.
The exact APK is built, archived, checksum-verified, installed, and accepted by
full connected instrumentation, physical-device visual QA, and Macrobenchmark.
The first local annotated tag was rejected before archive because it contained
stale v4.14 release metadata. With explicit approval, only that unpublished local
tag was deleted. Correct v4.15 release notes and changelog were committed, the
PreTag gate passed again, and a fresh annotated tag now targets the corrected
metadata commit. PreBackup passed and immutable event `tag-v4.15` was created
and verified in both durable stores.

## Build under test

- Version name: `4.15`
- Version code: `62`
- Event: `build-20260729-070042`
- Branch: `feature/v4.15`
- Source commit: `5253f003b0f7898b1755e98783ec91b01790e1d4`
- APK: `artifacts/builds/v4.15/build-20260729-070042/TranslateBooks-v4.15-code62.apk`
- APK size: `2,517,555` bytes
- APK SHA-256: `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`
- Source ZIP SHA-256: `FEFEDF516FD85B3166C6BB36249C22C94909D4428844EAC5E8E811F06015F5EE`

The mandatory archive-first build retained matching five-file payloads under
`artifacts/builds/v4.15/build-20260729-070042/` and
`backup/builds/v4.15/build-20260729-070042/` before installation. All five
mirror files match by SHA-256 and all four entries in the build checksum
manifest verify. Device package metadata reports versionName `4.15`,
versionCode `62`, minSdk `26`, and targetSdk `35`.

## Full automated regression

- Approved-logo guard: passed.
- JVM tests: `110` passed, `0` failed, `0` errors, `0` skipped.
- Android Lint: `0` errors, `53` warnings.
- Exact build workflow repeated the logo guard, all 110 JVM tests, and lint
  successfully before archiving and installing code62.
- Connected device: OnePlus CPH2691, Android 15 / API 35.
- Full connected instrumentation on exact `4.15`/code62: `16` total,
  `15` passed, `1` skipped, `0` failures, `0` errors.
- Skipped case: the explicitly opt-in paid real-API prompt test.
- Provider requests and API billing during this regression: none.
- Exact result XML:
  `artifacts/releases/v4.15/qa-code62-20260729-072300/instrumentation/TEST-CPH2691 - 15-_app-.xml`.

Instrumentation coverage by class:

- `Hotfix441InstrumentedTest`: 4 cases.
- `V415SettingsPersistenceInstrumentedTest`: 1 case.
- `V415LibraryScrollInstrumentedTest`: 1 case.
- `V415MultiProfileImportInstrumentedTest`: 2 cases.
- `V46DashboardInstrumentedTest`: 3 cases.
- `V48ImportNavigationInstrumentedTest`: 4 cases.
- `V48RealApiPromptInstrumentedTest`: 1 case, intentionally skipped.

## v4.15 focused evidence

### Settings persistence

- Cold launch and lazy Provider/Prompt/Performance section construction preserve
  exact custom settings.
- Refresh/recreation does not overwrite stored values with default controls.
- The complete Settings preference snapshot is restored exactly after testing.

### Glossary and Pronoun scroll

- Selecting a different profile from either long list preserves the exact vertical
  scroll position.
- The selected profile ID changes correctly.
- Editor navigation retains its intentional top-of-page behavior.

### Multi-profile import

- List-level picker intent enables Android multi-select.
- Duplicate returned URIs are de-duplicated while preserving selection order.
- Two Glossary files persist as two profiles with separate IDs and one term each.
- Two Pronoun files persist as two profiles with separate IDs and text.
- The first valid imported profile becomes active.
- Invalid siblings are reported independently without discarding valid files.
- Glossary, Pronoun, Settings, and runtime-log state are restored exactly after QA.
- Pronoun replacement remains single-select; Glossary editor merging is explicitly
  labeled `Merge files`.

## Exact-APK release QA

- Evidence event: `qa-code62-20260729-072300`.
- Artifact and backup each retain the same 21 files; relative paths, lengths, and
  SHA-256 hashes match with 0 differences.
- Five actual-device screenshots and five UI hierarchy dumps cover Translate,
  Settings, the Provider/Prompt/Performance section list, Glossaries, and Pronouns.
- The Settings section list visibly retains Provider, Prompt, and Performance
  after navigation; exact persistence across hydration/recreation is asserted by
  `V415SettingsPersistenceInstrumentedTest`.
- Glossary and Pronoun screens visibly describe multi-select and one profile per
  file; independent persistence and exact scroll restoration are asserted by the
  focused code62 instrumentation.
- OnePlus Android 15 denied shell `screenrecord`, consistent with v4.14. The
  retained MJPEG AVI is explicitly documented as 25 actual-device screenshot
  frames at 2 fps and 632x1390, not a continuous MediaProjection recording.
- After performance QA, the archived exact APK was reinstalled. Device metadata
  again reports versionName `4.15`, versionCode `62`, minSdk `26`, targetSdk `35`,
  and launcher activity `.MainActivity`.

## Physical-device performance

- `:macrobenchmark:connectedBenchmarkAndroidTest` passed 1/1 benchmark case with
  0 failures/skips on OnePlus CPH2691 / Android 15.
- Five cold starts completed using AndroidX `run-from-apk` compilation mode.
- Time to initial display: `313.89 ms` minimum, `330.23 ms` median,
  `335.04 ms` maximum.
- All five real Perfetto traces, AndroidX benchmark JSON, text summary, and result
  XML are retained in both durable QA stores.

## Immutable tag archive

- Corrected annotated tag object:
  `cb474d2dd22763f67e24b4c0f57a27b7ae4689e7`.
- Tag target: `292b24e2ac7dec7b9635b8d0e72f76745ddf432c`.
- Artifact: `artifacts/releases/v4.15/tag-v4.15/`.
- Backup: `backup/v4.15/tag-v4.15/`.
- Both stores contain 22 matching files with 0 relative-path, length, or SHA-256
  differences.
- All 21 `SHA256SUMS.txt` payload entries pass.
- Archived APK SHA-256:
  `7FDF60C934E75F4ACF47D77E14E248FBAC21F2EF5DDAD5E086AE586729163969`.
- `project_source_v4.15.zip` SHA-256:
  `0F14121D69314C36CF6F92E80084881D32AA8E6BA35ACB165203D52161B2F433`.
- The 357-entry source ZIP exactly matches a fresh `git archive` of tag `v4.15`.

## Known limitations and remaining release work

- Android Lint retains 53 warnings and no errors.
- The paid real-API instrumentation case remains intentionally opt-in and was skipped.
- OnePlus Android 15 denies shell `screenrecord`; the retained AVI limitation is
  documented above and does not replace functional instrumentation.
- Corrected annotated tag `v4.15` contains v4.15 release notes and changelog.
- The v4.15 Complete workflow gate passed. Merge to `main` and remote publication
  remain pending.
