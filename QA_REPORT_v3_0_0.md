# TBLAndroidTxt v3.0.0-rc1 Release Candidate QA Report

## Release identity

| Field | Value |
|---|---|
| Base version | v2.9.0-ui-polish |
| New versionCode | 28 |
| New versionName | `3.0.0-rc1` |
| Scope | Release candidate / feature freeze |

## What changed in this RC

| Area | Change | Risk |
|---|---|---|
| Versioning | Bumped Gradle version to `3.0.0-rc1` / code 28 | Low |
| Release metadata | Added `AppBuildInfo.java` as a single source for app title/version/export labels | Low |
| Debug export | Debug log export filename now uses current release suffix automatically | Low |
| Log bundle | Log export reports `AppBuildInfo.exportVersionLine()` instead of stale 2.8.5 text | Low |
| Job report | Per-job export reports current app version through `AppBuildInfo` | Low |
| Jobs UI | Copy changed to mark the build as v3.0.0 RC and feature-frozen | Low |
| UI shell | Kept the safe tab/top-bar shell from 2.9.0; no DrawerLayout/nav rewrite | Low |
| Core | No translation prompt/chunker/API/checkpoint algorithm rewrite | Low |

## Preserved from previous milestones

| Milestone | Preserved capability |
|---|---|
| 2.6.9 | API error parser, debug trace, prompt preview, export log |
| 2.7.0 | Service/core split through `TranslationEngine` |
| 2.7.5 | Job Manager: resume/retry/details/prompt/export/delete per job |
| 2.8.0 | SAF permission validation, safer output writing, encoding fallback |
| 2.8.5 | Glossary/pronoun validation, conflict checker, matched-rule preview |
| 2.9.0 | Safer visual polish: top chip, workflow dashboard, compact action cards |

## Static checks performed in this environment

| Check | Result |
|---|---|
| Source zip extracted cleanly | PASS |
| Version bump in `app/build.gradle` | PASS |
| Added `AppBuildInfo.java` | PASS |
| No `DrawerLayout`, `GravityCompat`, or `androidx.drawerlayout` active dependency | PASS |
| Core Java syntax compile with lightweight stubs for `org.json`, `okhttp3`, and Android-bound `FileUtil` | PASS for non-Android core subset |
| Java brace/string/comment sanity on modified files | PASS |
| Stale hard-coded `2.8.5-glossary-pronoun-engine` removed from active app source | PASS |
| Zip integrity after repack | PASS |

## Not verified in this sandbox

This environment does not include Android SDK, Android Gradle Plugin runtime, or a Gradle wrapper. Therefore the following remain **chưa kiểm chứng** here:

- full Android Gradle build;
- APK installation;
- real foreground service behavior;
- Storage Access Framework behavior on device;
- notification permission behavior on Android 13+;
- real API call behavior;
- background/kill/resume behavior on physical Android devices.

## Required Android Studio QA before promoting to stable 3.0.0

### Build/install

1. Sync Gradle.
2. Build APK.
3. Install on Android 12, 13, 14, and 15 if possible.
4. Confirm app info shows `3.0.0-rc1`.

### API/debug

1. Test API with a valid key and model.
2. Test API with a wrong key; error must be readable.
3. Test API with a wrong model; error must be readable.
4. Export full debug log and confirm the header says `3.0.0-rc1 / versionCode 28`.

### Translation core

1. Translate one short TXT without glossary/pronoun.
2. Translate one TXT with glossary CSV 3 columns.
3. Translate one TXT with pronoun CSV.
4. Confirm Activity Log and Last Translation Preview remain visible.
5. Confirm output is not empty when API succeeds.

### Jobs/checkpoints

1. Start a multi-chunk job.
2. Pause/cancel mid-job.
3. Resume selected job from Jobs screen.
4. Retry only failed chunks.
5. Open Details and Prompt for a selected job.
6. Export job report and confirm version header is `3.0.0-rc1 / versionCode 28`.
7. Delete a non-running job and confirm output file is not deleted.

### File/SAF

1. Pick input TXT from Downloads.
2. Pick input TXT from another file provider if available.
3. Pick output folder and restart app; permission should persist.
4. Test UTF-8 BOM file.
5. Test Japanese/Unicode filename.
6. Test output name collision; app should not overwrite unexpectedly.
7. Revoke/remove folder permission and confirm the app reports a file permission issue instead of crashing.

### Background/service

1. Start a translation, switch to another app, return after 1-5 minutes.
2. Confirm progress/log/preview restore.
3. Test Cancel while API request is running.
4. Confirm no duplicate job starts while service is active.

### UI regression

1. Confirm no content overlaps status/nav bars.
2. Confirm top status chip changes IDLE/RUNNING.
3. Confirm tab switching does not clear selected files/settings unexpectedly.
4. Confirm log and preview scroll/readability are acceptable on a small phone screen.

## Release decision

Promote this RC to stable `3.0.0` only after the Android Studio/device QA above passes. If a build/runtime bug appears, patch as `3.0.0-rc2` without adding new features.
