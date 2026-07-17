# TBLAndroidTxt v2.8.0 File SAF Stable - QA Report

## Build metadata

| Item | Value |
|---|---|
| Base | v2.7.5-job-manager |
| versionCode | `25` |
| versionName | `2.8.0-file-saf-stable` |
| Main focus | Android file/SAF stability |

## What changed

| File | Change |
|---|---|
| `app/build.gradle` | Bumped version to 2.8.0 |
| `FileUtil.java` | Reworked file layer: SAF permission helpers, safer text decoding, sanitized/unique output filenames, clearer file access exceptions |
| `FilePermissionStore.java` | New small inspector for persisted SAF grants shown in Files summary |
| `MainActivity.java` | Validates selected input/output/config URI permissions before Start; shows better file access errors; estimate shows detected encoding |
| `FilesPageFactory.java` | Shows permission badge and output folder permission warnings |
| `AppSettings.java` | Output filename pattern is sanitized before document creation |
| `ApiErrorParser.java` | File permission/access errors are reported directly instead of being treated like API errors |
| `JobStore.java`, `LogStore.java` | Export bundle version updated to v2.8.0 |
| `JobsPageFactory.java` | Dashboard text updated; job manager behavior remains v2.7.5-compatible |

## Static QA performed

| Check | Result |
|---|---|
| Source zip expanded | Pass |
| Version bump | Pass |
| SAF helper methods present | Pass |
| No DrawerLayout reintroduced | Pass |
| Java brace/string sanity check | Pass |
| Output filename sanitization | Pass |
| Encoding fallback code present | Pass |
| Job Manager actions retained | Pass |

## Not verified in sandbox

APK build/run is still not verified in this sandbox because Android SDK/Gradle wrapper are not available here.

## Android Studio QA checklist

1. Sync Gradle and build APK.
2. Select one TXT from Downloads; check estimate shows encoding.
3. Select multiple TXT and output folder; Start should pass only when output folder permission is persisted.
4. Restart app, open Files tab, confirm output folder shows `permission saved`.
5. Remove/revoke file permission or move/delete folder; Start should stop with a clear file permission issue instead of crashing.
6. Test UTF-8 BOM TXT.
7. Test Japanese Shift-JIS/CP932 TXT if available.
8. Test file names containing Japanese/Vietnamese characters and invalid filename chars like `/ : * ?` via output pattern.
9. Confirm batch output creates unique names instead of overwriting an existing file with the same display name.
10. Confirm v2.7.5 Jobs actions still work: Resume selected job, Retry selected job, Details, Prompt, Export, Delete.

## Acceptance criteria

- No crash when Android loses a selected URI permission.
- App asks user to choose file/folder again when permission is missing.
- Batch translation requires a valid output folder.
- Single-file translation works with either output TXT or output folder.
- Output names are valid `.txt` names and safe for Unicode filenames.
- Existing v2.7.5 job manager remains functional.
