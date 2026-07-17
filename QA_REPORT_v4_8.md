# v4.8 QA Report

- Version: `4.8` / versionCode `47`
- Branch: `fix/v4.8-import-navigation`
- Device: OnePlus CPH2691, Android 15
- Build APK SHA-256: `ABED4892CEFA9F3363F85ED8CF57F4D06C388447EE8F37617683FBB6DF891FB3`

## Regression

- JVM: 99 passed, 0 failed, 0 skipped.
- Android instrumentation: 11 required tests passed, 0 failed.
- Glossary import: profile persisted, activated, reflected immediately in UI/prompt state, and survived store reload.
- Pronoun import: profile persisted, activated, displayed immediately, and survived cold-store reload.
- Back/navigation: Glossary editor/list, Pronoun editor/list, Library root, and Settings categories passed.
- Scroll: Translate, Jobs, Library, and Settings vertical gestures passed without refresh or tab changes.
- Core translation, dashboard, SAF, session isolation, and recovery regression remained green.

## Real API verification

- Exactly 2 real provider requests were sent from the retained OnePlus app configuration.
- Both captured provider prompts contained glossary locks and pronoun locks before dispatch.
- Both requests returned valid translation responses.
- No credential value is recorded in this report.

## Known limitations

- Warm tab-switch P50 remains approximately 20 ms versus the 16 ms target.
- Performance evidence is Perfetto/frame/PSS based; no dedicated Macrobenchmark module exists.
- Android 15 shell `screenrecord` was denied by the device, so the archived video is retained as a clearly named v4.7 visual reference; the current v4.8 screenshot and Perfetto trace are fresh.
- GitHub push is currently blocked by `Repository not found` for the configured private-repository URL.
