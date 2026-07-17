# TBLAndroidTxt v2.6.7 → v2.6.8 QA Report

## Scope

Uploaded files inspected:

- `TBLAndroidTxt_source_v2_6_7_readera_safe_ui.zip`
- `TBLAndroidTxt_source_v2_6_6_no_sticky_header.zip`

The uploaded source is an Android Java project, not a Windows desktop project. The “Windows-specific” section below is therefore based on static search for desktop/Windows dependencies inside the submitted code.

## Current project identification

- Technology: native Android app, Java, Android View UI built programmatically.
- Not used: Python, C#, Electron, Flutter, React Native, Qt, web app, Compose, XML layouts.
- Gradle: Android Gradle Plugin 8.7.3, Java 17, compileSdk 35, targetSdk 35, minSdk 26.
- Module: single module `:app`.
- Package: `com.ml.tblandroidtxt`.
- Entry point: `MainActivity` declared in `AndroidManifest.xml`.
- Long-running work: `TranslatorService` foreground service.
- Storage: SAF document/tree URIs, SharedPreferences, SQLiteOpenHelper.
- Network: OkHttp 4.12.0 against OpenAI-compatible `/v1/chat/completions` endpoints.

## Directory/module structure

- `settings.gradle`, root `build.gradle`, `gradle.properties`: Gradle setup.
- `app/build.gradle`: Android plugin/config/dependencies.
- `app/src/main/AndroidManifest.xml`: permissions, `MainActivity`, `TranslatorService`.
- `MainActivity.java`: app shell, navigation, activity-result handling, settings/file/glossary actions, service control.
- `TranslatePageFactory.java`, `SettingsPageFactory.java`, `GlossaryPageFactory.java`, `FilesPageFactory.java`, `JobsPageFactory.java`, `SamplePageFactory.java`: tab/page UI builders.
- `TranslatorService.java`: foreground translation workflow, pause/resume/cancel, retry, checkpoint resume.
- `OpenAICompatibleClient.java`: OkHttp client and active request cancellation.
- `PromptBuilder.java`, `PromptContextBuilder.java`: prompt generation and glossary/pronoun injection.
- `Chunker.java`, `Chunk.java`: chunk creation.
- `SettingsStore.java`, `AppSettings.java`, `EnvParser.java`, `YamlInstructionParser.java`: settings/config/profile parsing.
- `GlossaryStore.java`: local glossary management.
- `TranslationRepository.java`: SQLite jobs/chunks/checkpoints.
- `LogStore.java`: runtime log file.
- `FileUtil.java`: SAF file read/write, output creation, text stats.
- `ModelCatalog.java`, `CostEstimator.java`: model metadata/estimation.

## Main runtime flow

1. `MainActivity.onCreate()` loads persisted settings and URIs.
2. Activity builds the page shell and opens the Translate tab.
3. User selects input TXT(s), output file/folder, `.env`, YAML instructions, glossary, pronoun file, and provider/model settings.
4. `startTranslation()` validates settings and starts `TranslatorService`.
5. Service loads external config, reads TXT, chunks source, creates SQLite job/chunk rows, builds prompt per chunk, calls API, saves chunk result, writes partial output, broadcasts progress.
6. Pause/resume/cancel commands are sent back to the service.
7. Jobs tab can resume incomplete checkpoints or retry failed chunks.

## Dependencies

- Android SDK / platform APIs.
- AndroidX SwipeRefreshLayout 1.1.0.
- OkHttp 4.12.0.
- org.json bundled with Android.
- OpenAI-compatible HTTP API/provider such as OpenRouter/OpenAI/DeepSeek/self-hosted compatible endpoint.

## Windows-specific dependency check

No Windows-specific runtime dependency was found in the submitted Android source:

- No hard-coded `C:\` / drive-letter path.
- No Registry access.
- No COM-port/driver logic.
- No shell command / PowerShell / `.exe` / `ProcessBuilder` usage.
- No Windows API / desktop UI / clipboard-specific code.

The app is already Android-native. The remaining portability concerns are Android-specific: SAF URI permissions, foreground-service survival, notification permission, API key storage, checkpoint persistence, and model/provider reliability.

## QA findings

| Severity | Area | File / location | Problem | Repro / symptom | Fix in v2.6.8 |
|---|---|---|---|---|---|
| Critical | UI regression | `MainActivity.java` v2.6.7 | v2.6.7 changed the shell from stable direct tabs to DrawerLayout. Core logic stayed almost unchanged, so the likely regression source was navigation/state/UI shell, not translation core. | Old functions feel missing; users may not reach Settings/Files/Jobs in the same direct flow; perceived translation/log regressions. | Reverted to v2.6.6 direct tab shell and removed DrawerLayout dependency. |
| Critical | Background log/progress | `TranslatorService.broadcast()` + `MainActivity.receiver` | Service only broadcasted progress. `MainActivity` wrote logs only while foreground/registered. If app went background, service logs/progress/preview were lost from UI state. | Activity Log / Last Translation Preview appears missing after switching apps/reopening. | Added `RuntimeStateStore`; service now persists progress, metrics, preview, and logs. Activity restores state on Translate tab build. |
| High | Last Translation Preview visibility | `TranslatePageFactory.buildTrackingCard()` | Tracking card was hidden until a live progress broadcast. If broadcast was missed or no job ran, preview area looked removed. | “Last Translation Preview” not visible. | Tracking card is visible by default and restored from runtime state. |
| High | Model response handling | `PromptBuilder.extractTranslationStrict()` | Strict wrapper-tag requirement can fail every chunk if a provider/model returns plain translation without `<TRANSLATION>` tags. | Chunk repeatedly fails with missing tag error even when the model output is usable. | Added safe plain-text fallback while still rejecting prompt echoes/refusals/meta output. |
| Medium | Android 15 safe area | v2.6.6 shell | v2.6.6 avoided sticky header jump but did not reserve status/nav bar insets. | Content can overlap phone status icons on targetSdk 35 / Android 15. | Kept v2.6.6 shell but added system-bar inset padding. |
| Medium | Duplicate/fragile service logs | `LogStore` usage | If both service and activity persist same broadcast logs, duplicates can occur. | Repeated runtime lines in Activity Log. | Service persists service logs; Activity receiver displays service logs UI-only. |
| Medium | API key security | `SettingsStore.java` | API key saved in normal SharedPreferences. | Device backup/root/adb debug risk. | Not fixed; should migrate to Android Keystore/EncryptedSharedPreferences. |
| Medium | Build reproducibility | project root | No Gradle wrapper included. | CLI build unavailable without installed Gradle/Android SDK. | Not fixed; recommend adding wrapper. |
| Medium | Process-death continuation | `TranslatorService` | Foreground service + checkpoint exists, but no WorkManager/process-death continuation. | If Android kills process, job resumes manually, not seamlessly. | Not fixed; current checkpoint remains valid. |
| Low | MainActivity size | `MainActivity.java` | Still large and owns many shared actions. | Higher regression risk when UI is edited. | Not fully fixed; avoid large refactor in hotfix. |

## v2.6.8 files changed

- `app/build.gradle`
  - versionCode 21, versionName `2.6.8-safe-core-ui`.
  - removed `androidx.drawerlayout:drawerlayout` dependency.
- `MainActivity.java`
  - restored stable direct tab shell.
  - added Android system-bar inset padding.
  - added UI-only service log appending.
  - added runtime state restoration.
- `TranslatePageFactory.java`
  - `Last Translation Preview` card visible by default.
  - restores latest runtime state after page build.
- `TranslatorService.java`
  - persists runtime state and service log from each progress broadcast.
- `RuntimeStateStore.java`
  - new SharedPreferences-backed progress/preview/metric snapshot store.
- `PromptBuilder.java`
  - safer extraction fallback for plain translation without wrapper tags.
- `CHANGELOG.md`, `REVIEW_REPORT.md`
  - updated notes.

## Build/run verification

Could not run Gradle build inside the sandbox because `gradle` and Android SDK/Gradle wrapper are not available.

Static checks performed:

- ZIP extraction succeeded for both uploaded sources.
- `diff -qr` showed v2.6.7 changed only docs, `app/build.gradle`, and `MainActivity.java` compared with v2.6.6.
- Verified no remaining `DrawerLayout`/`GravityCompat` references in source or Gradle dependency after v2.6.8 patch.
- Performed a simple Java source sanity check for unbalanced braces/newlines in strings: pass.
- Verified modified files are limited and do not rewrite translation workflow broadly.

## Android port / architecture assessment

Because this is already Android-native, the best direction is not a port but controlled stabilization:

- Keep native Java/Kotlin Android.
- Do not move to Flutter/React Native/WebView now; it would increase output drift and file/foreground-service risk.
- Keep core logic in Java classes and gradually extract workflow away from Activity/Service.
- Keep SAF for import/export.
- Keep foreground service + notification + SQLite checkpoint for long translation.
- Later migrate secrets to Android Keystore and long-running job orchestration to WorkManager only if it can preserve foreground behavior.

Recommended packages for the next refactor:

- `ui.translate`, `ui.settings`, `ui.glossary`, `ui.files`, `ui.jobs`
- `workflow` / `runner`
- `data.settings`, `data.jobs`, `data.logs`, `data.glossary`
- `files`
- `llm`
- `prompt`
- `model`

## Android QA checklist for Android Studio/device

1. Clean install v2.6.8.
2. Confirm top content does not overlap status bar on Android 15.
3. Open each tab: Translate, Sample, Settings, Glossaries, Files, Jobs.
4. Import `.env`; confirm API key/model/base URL saved.
5. Select one TXT and output TXT; start translation.
6. Confirm progress, Activity Log, and Last Translation Preview update.
7. Switch to another app for 1–2 chunks, return; confirm log/preview/progress are still visible.
8. Cancel during an active API call; confirm request stops and buttons recover.
9. Enable partial output; confirm output file is updated after each completed chunk.
10. Kill/reopen app; confirm Jobs tab can resume checkpoint.
11. Batch multiple TXT files into output folder; confirm output names and folder scan.
12. Test model that omits `<TRANSLATION>` tags; confirm safe plain translation is accepted, but prompt echoes/refusals are rejected.

## Build plan

### MVP hotfix

- Revert risky shell to stable tabs.
- Keep Android 15 insets.
- Restore log/preview runtime state.
- Make extraction less brittle.
- Pass: app builds, all tabs reachable, 1-file translation works, preview/log persists after background.

### Equivalent-to-Windows / existing Android feature parity

- Keep checkpoint, retry failed chunks, batch folder output, glossary/pronoun filters, YAML/.env import/export.
- Add Gradle wrapper and basic tests.
- Pass: same input/settings/glossary produces same prompt/output format as current stable core.

### Android UI/UX polish

- Rework UI per page without touching service/core.
- Add compact top status chips instead of drawer.
- Add better job details and prompt preview per selected chunk.
- Pass: no jump, no overlap, no hidden critical actions on phone portrait.

### Release stability

- Encrypted API key storage.
- Better process-death recovery.
- Automated unit tests for chunking, settings parsing, prompt injection, output extraction.
- Pass: background, cancel, retry, resume, batch output, and export log verified on real Android devices.
