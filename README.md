# TBL Android TXT v2.6.7 — ReadEra-style safe UI

This version fixes Android 15 edge-to-edge overlap by applying system-bar insets to the root layout.
The old portrait horizontal tab strip has been replaced with a compact top app bar plus a left navigation drawer inspired by reader apps such as ReadEra.

Key UI changes:
- Content no longer draws under the phone status bar/navigation bar.
- Top navigation is a small app bar, not a large header and not a crowded horizontal tab row.
- Main sections are opened from the drawer: Translate, Sample, Settings, Glossaries, Files, Jobs.
- Core translation workflow, checkpoint, API client, glossary/pronoun and foreground service are unchanged.

Build: open this folder in Android Studio, sync Gradle, then Build APK. JDK 17 and Android SDK 35 are required.

---

# Translate Books with LLMs Android TXT v2.6.6

Android TXT-only translation app inspired by Translate Books with LLMs.

## New in v2.6.6 no sticky header

- Removed the sticky/auto-hiding app header from the main layout.
- Root cause of the remaining blink: even with hysteresis, hiding the header still changed the root layout height during a scroll gesture, so Android had to recalculate the ScrollView viewport and the screen could flash once.
- New design: the top area is only the compact tab bar. No header view is shown/hidden while scrolling.
- This reclaims vertical space on portrait phones and avoids the layout reflow that caused the jump.
- Translation workflow, foreground service, checkpoint, API, glossary/pronoun, and storage logic are unchanged.

## New in v2.6.5 stable header scroll

- Fixed the jumpy/unstable scrolling seen after v2.6.4.
- Root cause: the compact header was hidden with `View.GONE` using a very small scroll threshold. That changed the viewport height while Android was still calculating ScrollView movement, so the scroll position could cross the threshold repeatedly and make the UI jump.
- New behavior: header hide/show uses hysteresis. It hides only after a real downward scroll and appears again only when the page is back at the top.
- Activity Log inner scrolling now blocks parent interception more reliably, including the outer SwipeRefreshLayout.
- Translation workflow, foreground service, checkpoint, API, glossary/pronoun, and storage logic are unchanged.

## New in v2.6.4 compact header + scroll log

- Reworked the top app header for portrait phones:
  - smaller card, smaller logo, shorter subtitle, single-line title, compact provider/language pills;
  - the header auto-hides after scrolling down inside the current tab and reappears at the top;
  - tab pages keep a visible right-side scrollbar instead of relying on Android's fading scrollbar.
- Improved the **Activity Log** panel:
  - always-visible vertical scrollbar at the right edge;
  - more reliable inner scrolling inside the log box;
  - inline log text is no longer selectable to avoid touch conflicts; use **Export Log** to save/copy full logs.
- Kept translation workflow, checkpoint, API client, glossary/pronoun, and file logic unchanged.

## New in v2.6.3 tab factory refactor

- Split the large `MainActivity` tab UI into dedicated factories:
  - `TranslatePageFactory`
  - `SettingsPageFactory`
  - `GlossaryPageFactory`
  - `FilesPageFactory`
  - `SamplePageFactory`
  - existing `JobsPageFactory` remains for Jobs/Checkpoints.
- Reduced `MainActivity` from about 2,056 lines to about 1,474 lines.
- Kept translation workflow, foreground service, API client, checkpoint database, glossary/pronoun filtering, and file formats unchanged.
- MainActivity now acts more like the app shell/router, while each tab owns its own UI-building code.

## New in v2.6.2 UI/log/glossary stats

- Renamed the visible app header and launcher label to **Translate Books with LLMs**.
- Redesigned the **Glossaries** list into portrait-safe cards, so `Use`, `Edit`, and `Del` are always visible on a vertical phone screen.
- Redesigned the glossary editor action buttons into stacked rows for small screens.
- Added delete confirmation for glossary deletion.
- Import messages now show the actual filename for glossary, pronoun, instruction, `.env`, and profile imports.
- Multi-file glossary import now reports imported filenames and per-file term counts in toast/log.
- Input estimate now includes words, characters, lines, estimated source tokens, chunks, and cost.
- Activity Log and Jobs Runtime Log now use fixed-height scrollable panels instead of growing endlessly down the page.
- Runtime log storage is capped to the latest tail so long-term usage does not make the internal log file grow forever.

## New in v2.6.1 stability hardening

- Added `AppValidator` to normalize settings and validate API key/model/language/Base URL before Start/Resume/Retry.
- Rejects empty TXT input and likely invalid text before creating a long-running translation job.
- Cost-limit stop now pauses the active checkpoint instead of marking the run as a generic fatal error.
- Translation and refinement failures are persisted per chunk, so **Retry failed chunks** can find and retry them.
- Input cost estimate now runs off the UI thread to reduce freezes when large TXT files are selected.
- Activity log clear now clears the persisted `LogStore`, not only the visible TextView.
- Added safer user-readable API/network errors and `NotificationManager` null guards.
- Enabled cleartext traffic for custom local/OpenAI-compatible HTTP endpoints such as LAN/self-hosted gateways. Prefer HTTPS for public providers.

## New in v2.6.0 stability/job tools

- Lowered `minSdk` from 35 to 26. The app now targets Android 8.0+ instead of Android 15+ only.
- Replaced `HttpURLConnection` with OkHttp for translation API calls. Cancel now calls `Call.cancel()` on the active request instead of only disconnecting a URL connection.
- Added a dedicated **Jobs / Checkpoints** tab:
  - recent jobs from SQLite,
  - status per job,
  - done / failed / pending chunk counts,
  - progress bar,
  - job details dialog,
  - checkpoint deletion,
  - resume last checkpoint,
  - retry failed chunks.
- Added runtime log persistence through `LogStore`.
- Added **Export log** and **Clear log** actions in the Jobs tab.
- Prompt preview is no longer limited to chunk 1. The user can choose which chunk to preview.
- Prompt preview is also available from the Jobs tab for saved checkpoints.
- Refactored part of the UI out of `MainActivity`:
  - `JobsPageFactory` owns the Jobs/Checkpoint screen.
  - `PromptPreviewDialog` owns input prompt preview.
  - `LogStore` owns runtime log persistence.
- Added Android-version guards for notification permission and broadcast receiver registration so lowering minSdk does not crash on older devices.

## Included from v2.5.1

- Filtered glossary/pronoun context is injected through `PromptBuilder`.
- Strict output guard: translation/refinement require `<TRANSLATION>...</TRANSLATION>` and reject model meta/commentary responses such as “yêu cầu có vẻ kỳ lạ”.
- Pronoun matching uses glossary aliases, so `Flum → Milkit` can match raw chunks containing `フラム` / `ミルキット` when glossary maps those names.
- Cost estimate samples the real filtered prompt instead of counting the full glossary as if it were injected into every chunk.

## Included from v2.5.0

- Language Profile picker: Japanese → Vietnamese Light Novel, English → Vietnamese, Chinese → Vietnamese, Korean → Vietnamese, Vietnamese → English, Custom.
- Backward-compatible glossary: old 3-column CSV still works as source / target / category-note.
- Pronoun file support: import a separate pronoun/xưng hô file for character relationships and speaking pairs.
- Glossary cột 3 pronoun detection: if category/note contains pronoun/xưng hô/`A→B`, the app injects it into `CHARACTER / PRONOUN RULES` instead of treating it only as a category.
- Pronoun priority: if a separate pronoun file is loaded, it has priority over pronoun notes embedded in glossary cột 3.
- Prompt cost control: prompt injection is filtered per chunk; the app does not inject the whole glossary/pronoun file into every request. Limits are configurable with `GLOSSARY_INJECT_LIMIT` and `PRONOUN_INJECT_LIMIT`.
- Database job hash guard: a completed file with the same input content + settings hash is not translated again; output is rebuilt from checkpoint without calling the API.

## Still included from v2.4.2

- Foreground notification cleanup after done/cancel/error.
- Single-job guard in `TranslatorService` to block duplicate Start / Resume checkpoint / Retry failed while one job is active.
- `START_NOT_STICKY` service behavior to avoid restarting old intents.
- UI disables Start/Retry while a job is active.
- Pull-to-refresh redraws UI only and does not create a new job.

## Glossary format

Old 3-column files remain valid:

```csv
source,target,category
フラム,Flum,character
魂の茨,Soul Thorn,skill
魔王,Ma Vương,title
```

If cột 3 contains pronoun/xưng hô, the term is still used as a name lock, and the note is moved into the pronoun prompt block:

```csv
フラム,Flum,character; Flum→Milkit: chị/em; Flum→Ink: tôi/cô
ミルキット,Milkit,character; Milkit→Flum: em/chị
```

## Pronoun file format

CSV/TXT examples:

```csv
from,to,pronoun
Flum,Milkit,chị/em
Milkit,Flum,em/chị
Ink,Flum,tôi/cô
```

or:

```text
Flum → Milkit: chị/em
Milkit → Flum: em/chị
Ink → Flum: tôi/cô
```

If a pronoun file is selected, it has priority over pronoun notes inside glossary cột 3.

## Build

Open this folder in Android Studio, install Android SDK Platform 35, then Build APK(s). This zip does not include a Gradle wrapper, so use Android Studio/installed Gradle from the IDE.

Recommended environment:

```text
Android Studio recent stable
Android Gradle Plugin 8.7.3
Java 17
compileSdk 35
targetSdk 35
minSdk 26
```

## Notes / limits

- This package was statically checked in the sandbox. The sandbox cannot run a full Gradle Android build because it lacks Android SDK/Gradle wrapper. Build in Android Studio to verify APK output. A local `javac` syntax pass was also run; remaining errors were expected missing Android/OkHttp/org.json classes outside Android Studio.
- OkHttp cancel is now implemented, but cancellation still depends on provider/network behavior; if the provider already processed a request, API cost may still be charged server-side.
- MainActivity tab UI is now split into page factories, but picker/import/export actions still remain in MainActivity. A later cleanup can move those into controllers and move service orchestration into a workflow layer.
- Completed-job hash guard applies to v2.5+ jobs. Older v2.4 jobs have no `input_hash/settings_hash`, so they cannot be detected by the hash guard until translated once under v2.5+.
