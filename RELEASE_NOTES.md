# Translate Books 4.8 Release Notes

Release date: 2026-07-17  
Git tag: `v4.8`

## Summary

Version 4.8 focuses on reliable glossary/pronoun import and navigation while preserving the existing translation pipeline, dashboard, SAF, session isolation, and recovery behavior.

## Highlights

- Imported glossary terms are stored as a library profile, activated immediately, and reflected in Settings and prompt state.
- Imported pronoun profiles are saved and activated atomically, shown immediately, and retained across cold-start.
- Back navigation now walks through Library and Settings predictably without closing the Activity unexpectedly.
- Four-tab scrolling remains ordinary vertical scrolling with no gesture-triggered refresh.
- Added focused JVM/device coverage plus a 2-request real API prompt verification.

## Verification

- Final Android build completed successfully.
- 99 JVM unit tests passed.
- 11 required Android instrumentation tests passed on OnePlus CPH2691 running Android 15.
- 2 real provider requests passed with glossary and pronoun locks asserted in both prompts.
- Android Lint completed with 0 errors and 50 warnings.
- Device checks covered primary-tab gestures, Jobs layout, glossary import, pronoun import/activation, Back navigation, and dashboard states.

## Artifact

- File: `artifacts/releases/v4.8/tag-v4.8/TranslateBooks-v4.8-debug.apk`
- SHA-256: `ABED4892CEFA9F3363F85ED8CF57F4D06C388447EE8F37617683FBB6DF891FB3`
- Note: APK files are intentionally ignored by Git and must be distributed separately from the source repository.

## Known limitations

- Warm tab-switch P50 is 20 ms, above the 16 ms target. Other recorded performance targets passed.
- Performance validation used Perfetto/frame/PSS evidence, not a dedicated Macrobenchmark module.
- The final navigation-only patch was not followed by a repeat of the complete 80-swipe campaign; relevant automated and targeted device checks passed.

See `BUILD_STATE.md` and `QA_REPORT_v4_8.md` for the complete recorded state and evidence.
