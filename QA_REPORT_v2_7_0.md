# TBLAndroidTxt v2.7.0 Core Split UI Safe - QA Report

## Scope

Base: `v2.6.9-debug-stable`.

Goal: move toward the 3.0 architecture without repeating the v2.6.7 UI regression. This release focuses on separating translation core work from Android UI/service lifecycle, while keeping the stable tab shell and reusing only the visually useful compact top-bar idea from v2.6.7.

## Technology / project summary

| Area | Finding |
|---|---|
| Platform | Native Android app |
| Language | Java |
| UI | Programmatic Android Views, `Activity`, `ScrollView`, `LinearLayout`, `SwipeRefreshLayout` |
| Background | Foreground `Service` (`TranslatorService`) |
| API | OpenAI-compatible chat completions through OkHttp |
| Storage | SharedPreferences-style settings, internal files for runtime/debug logs, SQLite for jobs/chunks |
| Android SDK | compile/target SDK 35, min SDK 26 |
| Not Windows | No registry, COM, Windows driver, `.exe`, PowerShell, or Windows path dependency found |

## v2.7.0 changes

| File | Change |
|---|---|
| `app/build.gradle` | versionCode `23`, versionName `2.7.0-core-split-ui-safe` |
| `TranslationEngine.java` | New core coordinator for input URI parsing, external config loading, input validation, chunking, hash preparation, translation/refinement retry, debug trace, usage callback, and partial output assembly |
| `TranslatorService.java` | Reduced responsibility: now delegates file/config/chunk/prompt/API/refine/output primitives to `TranslationEngine`; keeps Android lifecycle, foreground notification, pause/cancel, metrics, DB updates, and broadcasts |
| `JobStore.java` | New facade to reduce UI dependency on SQLite implementation details |
| `CheckpointStore.java` | New checkpoint facade for future resume/detail UI work |
| `JobsPageFactory.java` | Uses `JobStore` for summaries/chunks/delete where possible |
| `MainActivity.java` | Keeps stable tab navigation but adds compact top bar inspired by v2.6.7; no DrawerLayout reintroduced |
| `TranslationRepository.java` | Fixed duplicate `targetLanguage` field in `JobSummary`, which could cause Java compile failure |
| `CHANGELOG.md` | Added v2.7.0 notes |

## Why v2.6.7 UI was not copied directly

v2.6.7 was more visual because it used a compact top app bar and left drawer. The useful part is the compact status/title bar. The risky part is the DrawerLayout/navigation shell: it changed how pages are reached and how state is refreshed, which likely caused features such as translation preview/log state to feel missing or broken.

v2.7.0 therefore keeps direct tabs for reachability and adds only the safe top-bar pattern:

```text
compact title/status bar
horizontal stable tabs
current page content
```

No `DrawerLayout`, `GravityCompat`, or `androidx.drawerlayout` dependency is added.

## Static QA results

| Check | Result |
|---|---|
| Source patch applied | Pass |
| Version bump | Pass |
| New core classes present | Pass: `TranslationEngine`, `JobStore`, `CheckpointStore` |
| `TranslatorService` delegates to core | Pass: input/config/chunk/translate/refine/partial output now go through `TranslationEngine` |
| Drawer dependency absent | Pass: no Gradle drawer dependency; no active DrawerLayout imports |
| Java brace balance on modified files | Pass for `MainActivity`, `TranslatorService`, `TranslationEngine`, `TranslationRepository`, `JobStore`, `CheckpointStore` |
| Duplicate `targetLanguage` field | Fixed |
| v2.6.9 debug features retained | Pass: `ApiErrorParser`, `DebugTraceStore`, prompt preview/export log retained |

## Build/run status

Build was **not executed in this sandbox** because the environment does not include:

- Android SDK;
- Android Gradle Plugin runtime;
- Gradle wrapper `./gradlew`.

The patch was checked statically. Final APK build must be run in Android Studio.

## Manual QA required in Android Studio / device

### Build QA

1. Open project folder in Android Studio.
2. Sync Gradle.
3. Build APK.
4. Confirm version name shows `2.7.0-core-split-ui-safe`.

### Functional QA

| Case | Expected result |
|---|---|
| Open app on Android 15 | Content does not overlap status/navigation bar |
| Top bar | Shows app title and current tab/provider/language metadata |
| Tab navigation | Translate/Sample/Settings/Glossaries/Files/Jobs still reachable directly |
| Test API | Same behavior as v2.6.9 |
| Prompt Preview | Shows matched glossary/pronoun then prompt |
| Translate short TXT | Runs and updates progress/log/preview |
| Translate with bad key | Error is readable and export log includes trace |
| Cancel while API running | OkHttp active request cancel still triggers |
| Resume checkpoint | Last incomplete job resumes |
| Retry failed chunks | Only failed chunks retry |
| Jobs page details | Can show chunk status and prompt preview |
| Export log | Writes v2.7.0 debug bundle file |

## Remaining risks / not yet complete

| Risk | Status | Next step |
|---|---|---|
| `MainActivity` still large | Improved but not fully solved | v2.7.5 or v2.8 should extract picker/import/export controllers |
| SQLite still custom | Working but manual | Later migrate or wrap more through stores; Room optional after 3.0 |
| File handling still partly in `FileUtil` | Not fully SAF gateway-separated | v2.8.0 should create `AndroidFileGateway`, `TextReader`, `TextWriter`, `EncodingDetector` |
| API key storage | Still settings storage | Later add encrypted storage / keystore path |
| UI only lightly improved | Intentional | Larger UI polish should wait until job/file layers are safer |

## Pass criteria for accepting v2.7.0

Accept this version if:

1. APK builds.
2. Existing translation works at least as well as v2.6.9.
3. Last Translation Preview and Activity Log remain visible after app background/reopen.
4. Prompt Preview and Export Log still work.
5. Jobs page can display recent jobs and chunk details.
6. No regression from adding the compact top bar.

If any translation failure appears, compare exported debug log against v2.6.9 before changing UI again.
