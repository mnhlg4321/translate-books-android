# QA Report — v4.15

## Status

`EXACT BUILD PASS; RELEASE QA PENDING` for release candidate `4.15` /
versionCode `62`.

All three v4.15 correction groups are implemented and covered: Settings persistence,
Glossary/Pronoun scroll preservation, and independent multi-profile file import.
The exact APK is built, archived, checksum-verified, and installed. Final
visual/manual QA, performance evidence, release tag, and immutable tag archive
remain pending.

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
- Full connected instrumentation on `4.15-dev.3`/code61: `16` total,
  `15` passed, `1` skipped, `0` failures, `0` errors.
- Skipped case: the explicitly opt-in paid real-API prompt test.
- Provider requests and API billing during this regression: none.

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

## Known limitations and remaining release work

- Android Lint retains 53 warnings and no errors.
- The paid real-API instrumentation case remains intentionally opt-in and was skipped.
- Full connected instrumentation and focused visual/manual QA must now be run
  against the exact `4.15`/code62 APK.
- Release performance evidence, tag gate, immutable tag archive, final artifact
  verification, and merge to `main` remain pending.
