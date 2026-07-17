# TBLAndroidTxt v2.9.0 UI Polish QA Report

## Scope

This release starts from `v2.8.5-glossary-pronoun-engine` and performs a safe UI polish pass. It does **not** change the translation core, API client, chunking, glossary/pronoun matching, checkpoint schema, SAF file layer, or job manager data model.

The goal is to recover the more visual/direct feeling that made v2.6.7 attractive while avoiding the v2.6.7 regression risk: replacing the stable direct tab shell with a DrawerLayout-style navigation shell.

## Version

| Field | Value |
|---|---|
| versionCode | 27 |
| versionName | 2.9.0-ui-polish |

## Main changes

| Area | Change | Risk level |
|---|---|---|
| Top bar | Added `IDLE` / `RUNNING` status chip visible from any tab | Low |
| Navigation | Kept direct horizontal tabs, but reordered to Translate → Jobs → Files → Glossary → Settings → Sample | Low |
| Translate page | Added Workflow dashboard showing TXT/output/glossary readiness | Low |
| Translate page | Compacted input card and action area | Low |
| Actions | Grouped Start/Pause/Resume/Cancel/Prompt/Retry into one clear action card | Low |
| State safety | No DrawerLayout, no new navigation framework, no lifecycle rewrite | Low |

## Files changed

| File | Change |
|---|---|
| `app/build.gradle` | version bump to 2.9.0 / code 27 |
| `MainActivity.java` | compact top title, status chip, safer tab labels/order, status chip refresh on action state changes |
| `TranslatePageFactory.java` | workflow dashboard, compact input card, card-wrapped action grid |
| `CHANGELOG.md` | v2.9.0 notes |
| `QA_REPORT_v2_9_0.md` | this report |

## Static QA performed in sandbox

| Check | Result |
|---|---|
| Source zip unpacked | PASS |
| Version bump present | PASS |
| Java brace/string/comment balance on all Java files | PASS |
| No active DrawerLayout/GravityCompat usage in source code | PASS |
| No `androidx.drawerlayout` dependency in Gradle | PASS |
| Translation/service/core files intentionally not rewritten | PASS |
| Job Manager classes retained | PASS |
| SAF/FileUtil retained | PASS |
| Glossary/pronoun engine retained | PASS |

## Build status

Not built in the sandbox.

Reason: this environment does not include Android SDK, Android Gradle Plugin runtime, Gradle CLI, or a project `gradlew` wrapper. Build must be verified in Android Studio.

## Android Studio QA checklist

### Build

- Sync Gradle.
- Build debug APK.
- Confirm app version shows `2.9.0-ui-polish` if version is surfaced by Android Studio/app info.

### UI smoke test

- Open app on Android 13/14/15 if available.
- Confirm content does not overlap phone status/navigation bars.
- Confirm top chip shows `IDLE` before translation.
- Switch tabs: Translate, Jobs, Files, Glossary, Settings, Sample.
- Confirm tab switching does not clear settings unexpectedly.
- Confirm refresh button rebuilds current page without starting a new job.

### Translate screen

- Confirm Workflow dashboard is visible.
- With no TXT selected: TXT step should show not selected/optional state.
- Select one TXT: dashboard should show 1 file after page refresh/rebuild.
- Select multiple TXT: dashboard should show file count.
- Select output file/folder: dashboard should reflect output ready after refresh/rebuild.
- Select glossary: dashboard should show active glossary term count.
- Confirm Start/Pause/Resume/Cancel buttons are in one compact action card.
- Confirm Prompt preview and Retry failed chunks remain available.

### Regression test from v2.6.7 issue

- Run a short translation.
- Activity Log must update.
- Last Translation Preview must update.
- Switch away to another app and return.
- Top chip should show running while the service is active.
- Progress/log/preview should restore from RuntimeStateStore.

### Existing 2.8.5/2.8.0/2.7.5 features

- Glossary/pronoun health check still works.
- Prompt preview still shows matched glossary/pronoun rules.
- SAF output folder permission check still works.
- Jobs page still supports Resume / Retry / Details / Prompt / Export / Delete per job.
- Export log still includes runtime and API debug trace.

## Known limitations

- UI dashboard values are page-build snapshots; after selecting files, Android activity result handlers should update labels immediately, but the dashboard card refreshes on page rebuild/refresh. This avoids adding fragile live bindings in the UI polish step.
- This is still a Java programmatic UI. A future 3.0/3.1 design pass could move to XML or Compose, but only after release candidate QA.

## Recommendation before v3.0 RC

After v2.9.0 passes Android Studio/device QA, proceed to v3.0.0 release candidate with feature freeze. The v3.0 pass should focus on compile/build fixes, device matrix QA, and any regressions found during real APK testing—not new UI redesign.
