# Translate Books 4.7 Release Notes

Release date: 2026-07-17  
Git tag: `v4.7`

## Summary

Version 4.7 focuses on navigation reliability, a lighter Jobs screen, and smoother tab switching while preserving the existing translation pipeline, prepared-batch estimator, runtime state, and recovery behavior.

## Highlights

- Removed gesture pull-to-refresh and touch interception from the application shell; refresh is now button-only.
- Simplified Jobs to Current job and Recent jobs, with diagnostics moved to Settings > Logging > Developer tools.
- Lazily creates collapsed Settings content and retains cached pages to reduce unnecessary work during tab switches.
- Standardized compact token and cost labels for Prepared, Running, and Completed states.
- Keeps Android Back navigation inside Glossary and Pronoun flows.
- Refreshes only the affected cached page after glossary import or pronoun activation.

## Verification

- Final Android build completed successfully.
- 93 JVM unit tests passed.
- 7 Android instrumentation tests passed on OnePlus CPH2691 running Android 15.
- Android Lint completed with 0 errors and 50 warnings.
- Device checks covered primary-tab gestures, Jobs layout, glossary import, pronoun import/activation, Back navigation, and dashboard states.

## Artifact

- File: `artifacts/v47/TranslateBooks-v4.7-debug.apk`
- SHA-256: `BBA8152197FCE0310236899BA313B7C285E91FA1299D703EEF4BB23FA316EE4F`
- Note: APK files are intentionally ignored by Git and must be distributed separately from the source repository.

## Known limitations

- Warm tab-switch P50 is 20 ms, above the 16 ms target. Other recorded performance targets passed.
- Performance validation used Perfetto/frame/PSS evidence, not a dedicated Macrobenchmark module.
- The final navigation-only patch was not followed by a repeat of the complete 80-swipe campaign; relevant automated and targeted device checks passed.

See `BUILD_STATE.md` and `QA_REPORT_v4_7.md` for the complete recorded state and evidence.

