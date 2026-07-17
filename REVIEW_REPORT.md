# v2.6.8 Safe Core UI Review

This patch treats v2.6.7 as a UI regression risk: the translation core was almost unchanged from v2.6.6, but navigation/state moved to a DrawerLayout shell. v2.6.8 keeps the stable v2.6.6 tab shell, adds Android 15 system-bar insets, persists runtime progress/preview/log state from the foreground service, and adds a safe fallback for model responses that omit `<TRANSLATION>` tags.

Build was not executed in this sandbox because Gradle/Android SDK are unavailable here.

# v2.6.7 UI Review

Root cause of the screenshot issue: with `targetSdk 35`, Android 15 can run the app in edge-to-edge mode, so views may draw behind the status/navigation bars unless the app applies system-bar insets. v2.6.6 removed the large sticky header but did not yet reserve the safe area, so the tab strip overlapped the phone status icons.

Design decision: ReadEra is a good reference for this app only at the navigation/layout level: compact top app bar, drawer for secondary sections, large content area, and no oversized promotional header. It should not be copied as a bookshelf/library UI because TBL is a workflow tool, not a book manager.

Changes made:
- Main root uses `WindowInsets` to add top/bottom safe padding.
- Navigation moved to a left drawer to avoid portrait tab overflow.
- The app bar remains small and stable; it does not auto-hide, so there is no scroll reflow/jump.

Remaining limitation: APK was not built in the sandbox because Android SDK/Gradle wrapper are unavailable here; build in Android Studio.

# Source Review Report — Translate Books with LLMs Android TXT v2.6.6

## 1. Project structure

- Language: Java. No Kotlin sources found.
- UI: Android View system built programmatically in Java. No Jetpack Compose and no XML layout screens found.
- Gradle: Android Gradle Plugin 8.7.3, Java 17, `compileSdk 35`, `targetSdk 35`, `minSdk 26`.
- Modules: one module, `:app`.
- Main package: `com.ml.tblandroidtxt`.
- Main Android components:
  - `MainActivity`: app shell, tabs, settings UI, file pickers, glossary/pronoun/config UI, job actions, prompt preview entry points.
  - `TranslatorService`: foreground translation service, batch execution, pause/resume/cancel, resume checkpoint, retry failed chunks.
- No Fragment, ViewModel, Compose screen, Room, WorkManager, Hilt, or Chaquopy/Python integration found.

## 2. Main flow

1. User opens `MainActivity`.
2. Activity loads persisted settings, selected output URIs, selected glossary/pronoun/instruction metadata, then builds tabs: Translate, Sample, Settings, Glossaries, Files, Jobs.
3. User selects TXT input, output file/folder, provider/API key/model/language/chunking/glossary/pronoun/YAML.
4. Start validates settings and starts `TranslatorService` as a foreground service.
5. Service loads `.env`/YAML/glossary/pronoun if selected, chunks TXT, creates SQLite job/checkpoint rows, builds filtered prompts per chunk, calls an OpenAI-compatible chat endpoint through OkHttp, saves chunk output, writes partial output, and broadcasts progress.
6. User can switch apps while the foreground service runs. If Android kills the process, SQLite checkpoint + partial output are kept and can be resumed from Jobs/Files.
7. User can pause/resume/cancel, resume last checkpoint, retry failed chunks, inspect jobs, preview prompts, clear/export logs, and scan the output folder.

## 3. Current error/build review

No separate Gradle build log was attached with this zip. Static review and a local syntax pass found these categories:

- Gradle/dependency: `android.useAndroidX=true` is present. Dependencies are `androidx.swiperefreshlayout` and OkHttp. No Gradle wrapper is included, so build should be done through Android Studio or an installed Gradle environment.
- AndroidX: SwipeRefreshLayout dependency is compatible with the AndroidX flag.
- Manifest/permissions: `INTERNET`, notification, foreground service, and data-sync foreground-service permissions are declared. `usesCleartextTraffic=true` is enabled for local HTTP endpoints.
- UI: prior Glossaries table layout was not portrait-safe; fixed in v2.6.2 by changing it to vertical cards. Activity log previously expanded the page; fixed with fixed-height scroll panels.
- Storage/file: SAF is used for input/output/import/export. Import toasts now show actual filenames to reduce wrong-file mistakes.
- API/network: OkHttp is used and active calls can be cancelled. User-readable validation remains in `AppValidator`.
- Logic/checkpoint: SQLite checkpoint, retry failed chunks, and partial output are present.
- Python/Chaquopy: not present.

## 4. Core evaluation

Strengths:

- Several core units already exist outside UI: `SettingsStore`, `FileUtil`, `Chunker`, `CostEstimator`, `PromptBuilder`, `PromptContextBuilder`, `OpenAICompatibleClient`, `TranslationRepository`, `LogStore`, `PromptPreviewDialog`, `JobsPageFactory`.
- Glossary/pronoun injection is filtered per chunk, so the whole glossary/pronoun file is not inserted into every API call.
- Foreground service + notification + checkpoint makes translation much safer when leaving the app.
- Cost estimate now runs off the UI thread and includes file statistics.

Weaknesses remaining:

- `MainActivity` has been reduced from ~2,056 lines to ~1,474 lines by moving tab UI builders into factories, but it still owns shared event handlers, Activity Result handling, and service orchestration.
- `TranslatorService` still owns most workflow orchestration; a later refactor should move chunk execution into a `TranslationWorkflow`/`JobRunner` layer.
- API key is still stored in normal SharedPreferences, not EncryptedSharedPreferences/Android Keystore.
- No WorkManager/process-death continuation. The app can run in background through foreground service, but if Android kills the process, it resumes from checkpoint rather than continuing seamlessly.
- No automated unit/instrumentation tests are included.

## 5. v2.6.2 fixes performed

- Header/app label renamed from TBL wording to `Translate Books with LLMs`.
- Glossary list changed from a wide table to portrait-safe cards with visible `Use`, `Edit`, `Del` buttons.
- Glossary editor action layout changed to stacked rows for phone portrait screens.
- Glossary delete now asks for confirmation.
- Glossary/YAML/.env/profile/pronoun import toasts show exact filenames.
- Multi-file glossary import toast/log lists imported filenames and term counts.
- Input estimate now shows word count, character count, line count, approximate tokens, chunks, and cost.
- Activity Log and Jobs Runtime Log are fixed-height scroll areas.
- `LogStore` now caps the internal runtime log file and reads the latest tail.
- Foreground notification title updated.

## 6. v2.6.3 refactor performed

- Extracted Translate tab UI into `TranslatePageFactory`.
- Extracted Settings tab UI into `SettingsPageFactory`.
- Extracted Glossaries tab UI into `GlossaryPageFactory`.
- Extracted Files tab UI into `FilesPageFactory`.
- Extracted Sample tab UI into `SamplePageFactory`.
- Kept `JobsPageFactory` as the existing Jobs tab implementation.
- Kept translation workflow, API client, checkpoint repository, log store, glossary/pronoun parsing, and file processing behavior unchanged.
- `MainActivity` now works mostly as Activity shell/router plus shared event handlers. This lowers risk when editing one tab because tab layouts are no longer interleaved in the same huge class.

Remaining safe refactor candidates:

- `FilePickerController` for input/output/YAML/.env/profile/pronoun import/export.
- `SettingsActionController` for provider/model picker and quick API test.
- `GlossaryActionController` for term import/add/save/select.
- `TranslationWorkflow` or `JobRunner` extracted from `TranslatorService` later.

## 6.2. v2.6.6 UI redesign: no sticky header

The remaining portrait-screen blink was caused by changing the root layout height while the active `ScrollView` was still handling a drag. v2.6.5 reduced repeated toggling, but a single `GONE` transition could still force Android to recalculate the viewport and produce one visible jump.

The safer design in v2.6.6 removes the auto-hiding app header entirely from the root screen. The top area now keeps only the compact tab bar. Because no header view changes visibility during scroll, the root layout height stays stable for the whole gesture. This is preferable on phones because it also gives more space to the actual translation controls and Activity Log.

No core translation behavior was changed.

## 7. Recommended architecture next

Keep behavior stable, but split by responsibility:

- UI layer:
  - `TranslatePageFactory`
  - `SettingsPageFactory`
  - `GlossaryPageFactory`
  - `FilesPageFactory`
  - existing `JobsPageFactory`
- Workflow layer:
  - `TranslationWorkflow`
  - `JobRunner`
  - `ChunkProcessor`
  - `RetryFailedChunksUseCase`
  - `ResumeCheckpointUseCase`
- Managers:
  - `FileManager`
  - `ConfigManager`
  - `LogManager`
  - `CostManager`
  - `PromptPreviewManager`
- API layer:
  - `LlmClient` interface
  - `OpenAICompatibleClient` implementation
- Persistence:
  - keep `TranslationRepository` for now; consider Room only if schema grows.

## 8. Build guide

1. Open the project root folder in Android Studio.
2. Use JDK 17.
3. Install Android SDK Platform 35.
4. Sync Gradle.
5. Build > Make Project.
6. Build > Build Bundle(s) / APK(s) > Build APK(s).

## 9. Settings to verify before running

- API key is present and belongs to the selected provider.
- Model ID matches the provider. OpenRouter model IDs normally include provider prefix.
- Base URL points to an OpenAI-compatible `/v1/chat/completions` endpoint.
- For local/self-hosted HTTP endpoint, cleartext traffic is enabled; for public providers, prefer HTTPS.
- Input is valid TXT/UTF-8 and not empty.
- Output TXT or output folder has SAF write permission.
- `MAX_TOKENS_PER_CHUNK`, `MAX_OUTPUT_TOKENS`, timeout, retry attempts, glossary/pronoun inject limits, and cost limit are appropriate.
- Notification permission should be allowed on Android 13+ so the foreground service is visible.

## 10. Limits not solved in this pass

- Full Gradle APK build was not run in this sandbox because there is no Android SDK/Gradle wrapper here.
- `MainActivity` is split by tab UI factories, but file picker/import/export actions still remain in the Activity shell.
- No WorkManager migration.
- API key encryption was not implemented.
- No new automated tests were added.
## 7. v2.6.4 UI compactness fix

- The header was not a performance problem, but it was a usability problem on portrait phones because it was fixed above the tab content and consumed vertical workspace.
- The header has been changed to a compact mobile header and now auto-hides after the tab content is scrolled down. It returns when the tab content is scrolled back to the top.
- The Activity Log panel already had a fixed-height `ScrollView`, but Android scrollbars fade by default and nested scrolling inside a page scroll can make it look like the log cannot be dragged independently. Page-level scrollbars are also forced visible for a more website-like scroll cue. The log panel now forces the scrollbar to stay visible and asks the parent layout not to intercept touch while dragging inside the log panel.
- No core translation behavior was changed in this patch.
## 8. v2.6.5 jumpy-scroll fix

Observed issue from the supplied screen recording:

- The UI could jump while scrolling after the v2.6.4 compact-header patch.
- The most likely root cause was the header auto-hide code in `MainActivity.updateHeaderForContentScroll()`: it toggled the header between `VISIBLE` and `GONE` around a very small threshold.
- Because `GONE` removes the header from layout, the content area height changed mid-scroll. Android then recalculated the child ScrollView position, which could cross the same threshold again and repeat the show/hide cycle.

Fix applied:

- Header auto-hide now uses hysteresis: hide after a larger downward scroll; show only when the page returns to the top.
- Removed the immediate post-scroll update that could re-trigger header visibility changes right after page rebuild.
- Activity Log inner ScrollView now disables nested scrolling and tells all parent containers not to intercept drag events while the user scrolls inside the log panel.

Risk:

- Low. This patch only changes UI scroll handling and does not touch translation, API, storage, checkpoint, or glossary logic.

