# QA Report — v4.15

## Status

`FULL REGRESSION PASS; RELEASE QA PENDING` for development build `4.15-dev.3` /
versionCode `61`.

All three v4.15 correction groups are implemented and covered: Settings persistence,
Glossary/Pronoun scroll preservation, and independent multi-profile file import.
Exact-release build, final visual/manual QA, release tag, and immutable tag archive
remain pending.

## Build under test

- Version name: `4.15-dev.3`
- Version code: `61`
- Event: `build-20260729-065132`
- Branch: `feature/v4.15`
- Source commit: `cda8eff10309f787d15d52d8c552790b323741db`
- APK: `artifacts/builds/v4.15-dev.3/build-20260729-065132/TranslateBooks-v4.15-dev.3-code61.apk`
- APK SHA-256: `5D412996ECACE6553E5A30AC70A4D6D0EAB82E5C87D2DB619E0BFBD32A5A1EF7`
- Source ZIP SHA-256: `9C7D79789F51558FCB8EB320533F0F1D221A7E257AE12043097CD309768B08EF`

The mandatory archive-first build retained matching five-file payloads under
`artifacts/builds/v4.15-dev.3/build-20260729-065132/` and
`backup/builds/v4.15-dev.3/build-20260729-065132/` before installation.
All four entries in the build checksum manifest verify.

## Full automated regression

- Approved-logo guard: passed.
- JVM tests: `110` passed, `0` failed, `0` errors, `0` skipped.
- Android Lint: `0` errors, `53` warnings.
- Connected device: OnePlus CPH2691, Android 15 / API 35.
- Connected instrumentation XML: `16` total, `15` passed, `1` skipped,
  `0` failures, `0` errors.
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
- Exact `4.15` release APK and versionCode have not yet been created.
- Final device visual/manual QA, release performance evidence, tag gate, immutable
  tag archive, final artifact verification, and merge to `main` remain pending.
