# Translate Books v4.7 QA Report

## Release identity

- Version: `4.7` (`versionCode 46`)
- Device: OnePlus CPH2691, Android 15
- APK: `artifacts/v47/TranslateBooks-v4.7-debug.apk`
- SHA-256: `BBA8152197FCE0310236899BA313B7C285E91FA1299D703EEF4BB23FA316EE4F`

## Build and automated verification

- Final clean command: `clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`
- Result: BUILD SUCCESSFUL; 73 tasks executed.
- JVM unit tests: 93 passed, 0 failed, 0 skipped.
- Android instrumentation: 7 passed on CPH2691 / Android 15, 0 failed, 0 skipped.
- Lint: 0 errors, 50 warnings.
- A final targeted instrumentation run after the screenshot-fixture-only adjustment also passed 7/7.

## Required device checks

- Gesture: 20 upward/downward swipes were exercised in each of Translate, Jobs, Library, and Settings. Swipes began on cards, text, job rows, settings sections, and file-list content. No gesture-triggered refresh, refresh toast, refresh indicator, unintended tab change, or trapped child scroller was observed.
- Jobs: Runtime log and developer/QA panels are absent from the main page. Current job and Recent jobs remain; diagnostics are under Settings and lazy-created.
- Glossary import: imported `glossary-import-v47.csv` through the Android document picker on the real device. Two terms were parsed, persisted, and shown after returning to the glossary list.
- Pronoun import/activation: imported `pronoun-import-v47.txt` through the Android document picker. The new profile persisted as the selected profile and the UI showed `Active pronoun: pronoun-import-v47.txt • 2 rules` after cold activity recreation.
- Back: the original failure was reproduced first (system Back from the glossary editor closed the app). After the fix, system Back from both Glossary and Pronoun editors returned to their lists while MainActivity remained resumed.
- Dashboard: Prepared, Running, and Completed were verified with the fake-provider fixture. Cost formatting is three decimals (`$0.171`, `$0.010`, `$0.030`).

## Performance

The final 100 warm-switch run after changing cached hidden pages to `GONE` measured:

- P50: 20 ms
- P90: 26 ms
- P95: 27 ms
- P99: 31 ms
- Frames over 50 ms: 0
- Frames over 100 ms: 0
- Maximum: 46 ms
- PSS: 63,698 KB to 64,343 KB (+645 KB)

Perfetto traces are stored as `artifacts/v47/v47_after.perfetto-trace` and `artifacts/v47/v47_after_gone.perfetto-trace`. The P50 target of 16 ms was not met; P95, P99, long-frame, and PSS targets were met.

## Core regression scope

The automated fixture suite covers fresh Start dispatch, ordered multi-chunk processing, delivery-unknown protection, response recovery before commit, provider usage, and the Prepared/Running/Completed dashboards. Existing v4.5/v4.6 core translation, SAF validation, session identity, verified output writing, and runtime recovery code were not rewritten.

## Evidence outside build/

- `prepared.png`, `running.png`, `completed.png`, `jobs.png`
- `four_tabs_gesture.mp4`
- `v47_after.perfetto-trace`, `v47_after_gone.perfetto-trace`
- final unit, instrumentation, and lint reports

## Explicit limitations

- Warm-switch P50 remains 20 ms, above the requested 16 ms target.
- Performance was captured with Perfetto and frame/PSS measurements, not a dedicated Macrobenchmark module.
- The 80-swipe gesture campaign was not repeated after the final Back/pronoun cache invalidation patch; that patch only changes same-page Glossary/Pronoun navigation and does not alter the four primary scroll containers.

