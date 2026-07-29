# Changelog

All notable changes to Translate Books are recorded in this file. The repository was normalized to Git on 2026-07-17 without feature changes or rollback. The v4.7 source baseline is commit `da8c6d9a9001296c3f3c17817ea3d6372fd59ae3` on branch `main`.

## [4.15] - 2026-07-29

- Preserve the exact Glossary and Pronoun list position when activating another
  profile while retaining intentional top navigation for editors.
- Allow list-level Glossary and Pronoun imports to select multiple files and
  create one independent profile per valid file.
- De-duplicate selected URIs, activate the first valid import, and report invalid
  siblings without discarding successful imports.
- Keep Pronoun replacement single-select and distinguish intentional Glossary
  editor combination with the `Merge files` label.
- Protect persisted Provider, Prompt, and Performance settings from default-value
  overwrites during lazy hydration, refresh/recreation, lifecycle save, and app
  reopening.
- Add focused planner and physical-device instrumentation coverage for Settings
  persistence, exact scroll restoration, picker flags, URI collection, and
  independent profile persistence.
- Verify exact `4.15`/code62 with 110 JVM tests, lint at 0 errors/53 warnings,
  connected instrumentation at 15 passed/1 opt-in skip, and five retained
  physical-device cold-start traces.

## [4.14] - 2026-07-25

- Preserve every successful APK as a numbered, checksummed artifact with an exact source ZIP and a second immutable local backup before optional installation.
- Make glossary imports adopt the source filename automatically, matching the established Pronoun profile workflow.
- Restore the latest accepted translation preview plus exact per-chunk Glossary and Pronoun rule-use counts.
- Install the approved bright cool logo permanently as launcher, round launcher, and in-app header identity, guarded by its exact SHA-256 during every build.
- Add an exact release build mode and a physical-device AndroidX Macrobenchmark path for retained release performance evidence.
- Keep the physical-device benchmark runner active on OnePlus firmware without measuring the first-run notification dialog; retain five real startup traces.
- Verify missing output-folder permission with a prepared local TXT, an exact recovery message, and Start disabled before dispatch.

## [4.14-dev.4] - 2026-07-25

- Make the approved bright cool logo the permanent launcher, round launcher, and in-app header identity.
- Store the approved design in the project and require its exact SHA-256 in every Android `preBuild`.
- Stop builds when the logo file, manifest references, or in-app header reference is missing or changed.
- Verify the packaged resource, installed launcher icon, in-app header, archive parity, and preserved user state on the connected Android device.

## [4.14-dev.3] - 2026-07-25

- Restore the bounded, scrollable preview of the latest accepted translation chunk on the Translate dashboard.
- Show current chunk and exact Glossary/Pronoun rule counts from the actual translation or refinement `PromptPlan`.
- Persist preview identity and rule-usage state across normal UI recreation while distinguishing unknown counts from a valid zero.
- Add prompt-count, runtime-state, dashboard-label, and controlled Android UI evidence.

## [4.8] - 2026-07-17

- Persist and activate glossary imports immediately, including legacy import flow.
- Persist and activate pronoun imports atomically and retain the active profile across cold-start.
- Keep Back navigation inside Library and Settings, with deterministic return paths.
- Verify four-tab scrolling without gesture refresh and preserve translation core/dashboard/SAF/session isolation/recovery.
- Add focused glossary, pronoun, Back, persistence, gesture, and real API prompt tests.
- Bump version to `4.8` / versionCode `47`.

## [4.7] - 2026-07-17

- Remove gesture pull-to-refresh and its touch interception from the application shell; refresh is button-only.
- Replace the nested runtime-log scroller in Jobs with a lightweight Current job / Recent jobs page.
- Move runtime logs, prompt dumps, benchmark controls and chunk inspection to Settings > Logging > Developer tools; build them only when opened.
- Lazily create collapsed Settings section content, preserve one vertical scroll owner per primary tab, and keep the bottom navigation fixed.
- Standardize compact estimated/actual token and cost labels while preserving the v4.6 PreparedBatch estimator and runtime state.
- Keep Android Back inside Glossary/Pronoun library flows and refresh only the affected cached page after glossary import or pronoun activation.
- Bump version to `4.7` / versionCode `46`.

# v4.6

- Restore the compact Prepared summary from the cached prepared batch: chunk count, estimated total tokens, and estimated cost only.
- Restore the essential Running/Completed dashboard: progress, elapsed, estimated remaining, failed, fallback, tokens, and cost.
- Distinguish provider-backed actual usage from estimates, and clear prior actual values immediately when a fresh Start session is dispatched.
- Persist estimated totals and provider-usage completeness in the existing version-gated runtime snapshot for rotation/force-stop recovery.
- Keep preparation and estimation on the existing worker; do not re-read source, parse glossary/pronoun data, scan folders, or query SQLite during render/tab switching.
- Remove settings serialization from ordinary tab switches, retain attached page views, and avoid periodic dashboard redraws between progress events.
- Add JVM dashboard presentation tests and Android 15 fake-provider multi-chunk fixtures. Bump version to `4.6` / versionCode `45`.

# v4.5

- Restore SAF correctness: require and verify complete persisted grants, probe document read/write access at Start, and validate output folders with a temporary create/delete operation.
- Make every Start a clean dispatch session so an older failed or completed prepared job cannot be resumed silently.
- Add the durable `IDLE → VALIDATING → READY → RUNNING → PAUSED/COMPLETED/FAILED` job state model, version-gated runtime snapshots, and full initialization failure logging.
- Move prepared-plan lookup, file identity work, job history loading, and output-folder scans away from tab-switch UI work; cache pages and parsed glossary/pronoun data.
- Reduce the Translate screen to source selection, exact chunk readiness, Start, core progress, cost/tokens, and result preview.
- Validate on a OnePlus CPH2691 / Android 15 with a real 11-chunk OpenRouter translation, process-state recovery tests, repeated tab switching, cold relaunch, and device reboot.
- Bump version to `4.5` / versionCode `44`.

# v4.4.1

- Replace redraw-triggered estimation with one cancellable, durable prepared chunk plan reused verbatim by Start and the translation service.
- Make production chunk scanning linear and interruptible; cache normalized source, hashes, chunks, prompt estimates, and preparsed glossary/pronoun rules.
- Separate pricing refresh from chunk/token readiness, with a 20-second request timeout, cached fallback, unavailable state, and retry action.
- Reduce the normal translation estimate to exact chunks, conservative total tokens, and estimated cost; hide inactive execution controls.
- Add schema 8→9 prepared-plan persistence, atomic prepared-plan job creation, and a unique prepared input dispatch identity.
- Add durable request phases and response persistence, delivery-unknown recovery, duplicate-billing review gates, and accepted-output transaction safety.
- Defer folder output creation until accepted non-empty content exists and preserve prior accepted output when retry candidates are rejected.
- Guard Android foreground-service startup/initialization, checkpoint Android 15 data-sync timeout, and keep service `onCreate` lightweight.
- Add deterministic provider injection plus JVM/device regression coverage. Bump version to `4.4.1` / versionCode `43`.

# v4.1

- Fixed Settings pull-to-refresh gesture conflict.
- Fixed expandable Settings header overlapping the first row of controls.
- Use one fill-viewport nested scrolling container while keeping bottom navigation fixed.
- Keep Settings accordions stable during repeated expansion and adapt paired fields for small screens and large fonts.
- Preserve all existing translation, provider, library, job, glossary, pronoun, instruction YAML, cost tracking and export behavior. Bump version to `4.1` / versionCode 39.

# v4.0.0-alpha1.4

- Replace sampled/inflated prompt estimation with the sum of the actual `PromptPlan` generated for every chunk.
- Estimate a cold-start batch cache: the first request is uncached and later requests reuse only their exact shared system-prefix blocks.
- Apply the bundled cached-input price for `openai/gpt-5.4-mini` on OpenRouter and never invent cache savings when cached pricing is unknown.
- Show both `Expected (cached)` and conservative `Cold/no-cache` cost ranges, plus prompt input and expected cached token counts.
- Include previous-translation history in the visible Context breakdown.
- Keep cost-limit and benchmark budget gates conservative by checking the no-cache upper bound.
- On the same sample/profile, the estimator changes from `$0.088–$0.116` to expected cached `$0.064–$0.081`; cold/no-cache is `$0.087–$0.104`.
- Add exact-plan, cache-discount and unknown-cache-price regression tests. Bump version to `4.0.0-alpha1.4` / versionCode 38.

# v4.0.0-alpha1.3

- Select pronoun rules from characters, aliases and dialogue evidence actually present in each chunk instead of filling the 40-rule cap.
- Exclude context-only characters and relationship rules whose counterpart is absent from the scene.
- Preserve full rules for primary/dialogue characters while compacting secondary-character rules to essential gender, narration, self-pronoun and address guidance.
- Add a visible Prompt Breakdown for Source, Instruction, Glossary, Pronoun and Context token estimates.
- Benchmark the same sample/OpenRouter/GPT-5.4-mini pipeline: prompt tokens decreased from 47,674 to 40,498 while completion tokens remained effectively stable.
- Add pronoun relevance, compaction and prompt-breakdown regression tests. Bump version to `4.0.0-alpha1.3` / versionCode 37.

# v4.0.0-alpha1.2

- Replace the single-file Pronoun workflow with a persistent multi-profile Pronoun library aligned with Glossaries.
- Add Import, Use, Active, Edit, Rename, Replace file and Delete actions for each pronoun profile.
- Show the active profile and recognized rule count in the Pronouns list, Settings Prompt group and Translate configuration snapshot.
- Importing a new pronoun file creates and activates a profile; replacing a file updates the selected profile rather than creating an accidental duplicate.
- Migrate the legacy `pronounText/pronounUri/pronounName` selection into the profile library once, preserving existing users' configuration.
- Store `selectedPronounId/selectedPronounName` in editable settings and immutable job snapshots.
- Keep active profile content as the source of truth supplied to PromptBuilder and job settings; profile changes remain blocked while TranslatorService is active.
- Add profile rule-count and settings-copy regression tests. Bump version to `4.0.0-alpha1.2` / versionCode 36.

# v4.0.0-alpha1.1

- Fix Settings category changes incorrectly calling the full page refresh workflow, which scanned data, appended logs, showed refresh feedback and made the UI feel like it reloaded itself.
- Keep Settings category state stable across Activity recreation and avoid rebuilding when the selected category is tapped again.
- Replace the phone horizontal category strip, which jumped back to its first item after every rebuild, with one stable category selector dialog.
- Make the full Provider, Model and Quality preset rows clickable instead of relying on a non-focusable EditText receiving the touch.
- Apply appearance palettes by rebuilding only the in-memory UI shell, without restarting the Activity; active service jobs remain untouched.
- Add Settings navigation regression tests. Bump version to `4.0.0-alpha1.1` / versionCode 35.

# v4.0.0-alpha1

- Begin the product UX redesign with an adaptive productivity-workspace architecture; translation engine, checkpoints, telemetry and service ownership remain unchanged.
- Replace the horizontally scrolling six-tab shell with four primary destinations: Translate, Jobs, Library and Settings.
- Add a compact Library secondary navigator for Files, Glossaries, Pronouns and Sample without hiding those existing features.
- Use bottom navigation on phones and a navigation rail on screens at least 720dp wide.
- Redesign Settings as category/detail navigation: General, Translation, Provider, Prompt, Performance, Appearance, Logging and About.
- Separate everyday translation choices, provider credentials, technical performance tuning and local privacy/log controls.
- Replace the free-text quality preset control with a constrained picker.
- Add 15 persistent workspace palettes inspired by mature file/productivity tools: Default, Ocean Blue, Seafoam, Mint, Forest, Yellow Gold, Orange, Rose, Brick Red, Violet, Iris Pastel, Blue Gray, Gray Dark, Coffee and Camouflage.
- Keep success/running green, warning/paused amber and error/destructive red semantic and independent from the selected decorative workspace palette.
- Add responsive two-pane Settings on tablets and compact horizontal category navigation on phones.
- Add palette fallback/readability tests. Bump version to `4.0.0-alpha1` / versionCode 34.

# v3.2.0

- Add service-owned paid Full/Balanced A/B benchmark sessions for three stable samples (first, middle, last stored chunks). Switching tabs or recreating the Activity does not own or duplicate benchmark requests.
- Require an explicit cost confirmation and enforce a USD 0.50 worst-case/actual budget guard before each provider request.
- Use identical stored chunks, model, provider, temperature, configuration snapshot and max-output policy for both pipelines; alternate execution order to reduce provider-order bias.
- Store benchmark history, blind X/Y labels, token usage, cached tokens, output tokens, duration, cost, finish reason, structural quality warnings and local outputs in SQLite.
- Label every benchmark result as provider-reported or estimated usage/cost; fallback estimates are never presented as provider actuals.
- Add blind review and a separate mapping reveal in Jobs. Job exports include the latest benchmark for manual QA.
- Add provider capability registry for usage details, cached-token reporting, prompt caching, `Retry-After` and batch support without sending unsupported request fields.
- Extend centralized model metadata with cached-input pricing and pricing source. Cached input is not charged again at the normal input rate when a dedicated cached rate is known.
- Show provider capabilities, pricing source and actual/estimated tokenizer calibration in Job Details.
- Add database migration v6 and cleanup benchmark rows when deleting a job.
- Bump version to `3.2.0` / versionCode 33. No automatic model switching, navigation redesign or background cloud upload is included.

# v3.1.2

- Add an explicit live-benchmark readiness gate for source, credential, model, pricing and user budget before any paid Full/Balanced pair can run.
- Add one bounded context-length recovery attempt with a materially changed payload: optional previous/surrounding context is removed and output reserve is reduced while mandatory Instruction YAML and glossary/pronoun locks remain.
- Fix settings normalization that previously converted an explicit `contextChars=0` back to 400, which prevented no-context recovery from taking effect.
- Keep context-length retries disabled after that safe fallback, preventing repeated identical paid requests.
- Add pronoun speaker/listener conflict diagnostics instead of silently accepting multiple incompatible rules.
- Add regression tests for safe fallback preservation, benchmark readiness blockers and pronoun conflicts.
- Bump version to `3.1.2` / versionCode 32. No live provider request was executed because the workspace contains neither a benchmark source file nor an API key.

# v3.1.1

- Add a deterministic dry-run A/B benchmark on the exact same stored chunk list and max-output policy; it never calls the provider and clearly labels language quality as unverified.
- Correct the v3.1.0 synthetic baseline so it uses the original chunk, previous translation and surrounding context instead of reconstructing an incomplete chunk.
- Separate provider-reported usage, fallback estimates, unknown usage and unknown pricing; unknown values are no longer treated as actual zero cost.
- Store cached input tokens, logical request IDs and cost/usage status in privacy-safe local telemetry with a database migration.
- Add provider attempt IDs, typed HTTP failures, `Retry-After` handling and explicit retry logs. Context-length failures are not retried with an unchanged payload.
- Split context-before and context-after budgets so long preceding context cannot silently remove all following context.
- Improve glossary selection with NFKC/case normalization, word boundaries, aliases, global/mandatory rules and priority before injection limits.
- Improve pronoun selection by using previous source context, explicit global rules and boundary-aware character matching.
- Add language-independent quality-risk checks for empty/truncated output, prompt echo, lost paired symbols and repeated chunk boundaries.
- Add regression tests for fair A/B setup, aliases, global locks, near-name collisions, previous-context pronouns, two-sided context, retry policy and structural quality checks.
- Bump version to `3.1.1` / versionCode 31. No new model, translation workflow or large UI redesign is included.

# v3.1.0

- Add privacy-safe local telemetry per job, chunk, phase and attempt, including raw, system, instruction, glossary, pronoun, context, output and retry tokens.
- Add expandable token/cost metrics to Job Details and job report exports; no book text is added to telemetry rows.
- Introduce a shared `PromptPlan` so requests, estimates and telemetry measure the same prompt components.
- Remove repeated output instructions in the optimized prompt while retaining mandatory instructions, relevant glossary/pronoun locks, source and output contract.
- Add quality, balanced (default), economy and full-baseline presets; preserve mandatory rules and user-controlled refinement.
- Adapt source chunk targets and max output tokens to the selected safety preset and actual source budget.
- Classify retry failures and stop retrying non-recoverable authentication/invalid requests.
- Parse provider-reported cached-token usage when available; providers without usage details remain compatible.
- Bump version to `3.1.0` / versionCode 30. Model selection and pricing metadata are unchanged.

# v3.0.1

- Bump version to `3.0.1` / versionCode 29.
- Show Glossaries, Pronoun, and Instruction YAML consistently on the Translate screen.
- Derive the three config summaries from the same `AppSettings` or active-job settings snapshot used by translation prompts.
- Treat Instruction YAML as optional when not selected; block Start when a selected file is unreadable or has no valid `translation`/`refinement` instruction.
- Add View, Change, and Clear actions for all three translation-support configs.
- Prevent changing translation-support configs while a foreground translation job is active.
- Restore tab, input selection, progress, current chunk, translated/failed counts, log, preview, tokens, cost, and pause/stop state after tab or Activity recreation.
- Keep translation work owned by `TranslatorService`; rebuilding a tab never starts or resumes a job.
- Add regression tests for three-config state, active-job snapshot precedence, runtime snapshot round-trip, and duplicate-start prevention.
- No translation-engine, model-pricing, cost-estimation, or workflow redesign is included in this patch.

# v3.0.0-rc1

- Bump version to `3.0.0-rc1` / versionCode 28.
- Feature freeze release candidate: no new translation algorithm, no navigation rewrite, no DrawerLayout comeback.
- Centralized release metadata in `AppBuildInfo.java` so top title, log export, job report, and release copy do not drift between versions.
- Exported debug log filename now includes the current release suffix automatically.
- Job report/debug exports now include `AppBuildInfo.exportVersionLine()` instead of stale hard-coded 2.8.5 labels.
- Jobs screen copy now marks this as a 3.0 release-candidate QA build and confirms the 2.7 core split, 2.8 SAF/file layer, 2.8.5 glossary/pronoun engine, and 2.9 UI polish are preserved.
- Minor cleanup: fixed a stale indentation artifact in `MainActivity` around restored pronoun URI loading.

## Release-candidate scope

This build intentionally avoids another UI redesign. It is intended for Android Studio/device QA before promoting to stable 3.0.0. Remaining work before final stable should be limited to build/runtime fixes discovered during real APK testing.

# v2.9.0-ui-polish

- Bump version to `2.9.0-ui-polish` / versionCode 27.
- Polish UI safely without reintroducing the v2.6.7 DrawerLayout/navigation regression.
- Add compact top status chip (`IDLE` / `RUNNING`) so the user can see translation state from any tab.
- Reorder direct tabs to prioritize daily workflow: Translate, Jobs, Files, Glossary, Settings, Sample.
- Shorten tab labels and keep the horizontal direct tab shell for state safety.
- Add Translate workflow dashboard showing input/output/glossary readiness before Start.
- Compact the input card and wrap Start/Pause/Resume/Cancel/Prompt/Retry actions inside one action card.
- Keep Activity Log and Last Translation Preview visible and scrollable.
- Preserve all v2.8.5 glossary/pronoun engine, v2.8.0 SAF/file handling, v2.7.5 Job Manager, and v2.7.0 core split behavior.

## Notes

- This release is UI polish, not a rewrite. It intentionally keeps the direct tab architecture because v2.6.7 showed that replacing the navigation shell can make translation/log/preview state feel broken.
- Full Android Gradle build still requires Android Studio or an Android SDK environment. The sandbox has no Android SDK, no Gradle CLI, and no `gradlew` wrapper.

---

# v2.8.5-glossary-pronoun-engine

- Bump version to `2.8.5-glossary-pronoun-engine` / versionCode 26.
- Rebuild `PromptContextBuilder` as the glossary/pronoun matching engine:
  - validates glossary/pronoun text before prompt injection;
  - detects malformed rows, duplicate pairs, and same-source/different-target conflicts;
  - keeps explicit Pronoun file priority over pronoun notes embedded in glossary cột 3;
  - keeps glossary cột 3 pronoun notes as character/name locks when explicit Pronoun file exists;
  - previews matched glossary/pronoun rules per chunk with counts and warnings.
- Improve glossary import path by delegating CSV/TXT/JSON parsing to the shared engine.
- Add Glossaries > Health check and editor Health action.
- Add validation summaries when importing glossary/pronoun files.
- Add per-chunk lock-count logging before API calls.
- Fix compile-risk regressions from earlier patch series:
  - clean `PromptContextBuilder` brace structure;
  - remove duplicated `inputUrisJson()` declaration.
- Keep v2.8.0 SAF/file improvements and v2.7.5 Job Manager actions intact; no large UI rewrite.

# v2.8.0-file-saf-stable

- Bump version to `2.8.0-file-saf-stable` / versionCode 25.
- Harden Android Storage Access Framework handling before starting translation.
- Add persisted read/write permission validation for input TXT, output TXT, output folder, instructions, .env, glossary and pronoun files.
- Improve TXT decoding: UTF-8 BOM, UTF-16 BOM, strict UTF-8, Windows-31J/Shift-JIS fallback.
- Sanitize output filenames and create unique names in output folders to reduce accidental overwrite/invalid-name failures.
- Add output folder permission warning in Files tab and SAF permission summary.
- Keep v2.7.5 Job Manager actions intact; no large UI/nav rewrite.

# v2.7.5-job-manager

- Bump version to `2.7.5-job-manager` / versionCode 24.
- Upgrade Jobs screen into a real per-job checkpoint manager.
- Add dashboard counts for running/paused/error/done/cancelled jobs and failed chunks.
- Add per-job actions: Resume selected job, Retry selected job failed chunks, Details, Prompt Preview, Export job report, Delete checkpoint.
- Add service actions `ACTION_RESUME_JOB` and `ACTION_RETRY_JOB_FAILED` using a `jobId` extra.
- Add `JobStore.exportJobBundle(jobId)` to export job metadata, settings snapshot, chunk status list, runtime log tail, and API debug trace tail.
- Keep the stable v2.7.0 tab/top-bar UI; no DrawerLayout or big UI rewrite.

---

# v2.7.0-core-split-ui-safe

- Bump version to `2.7.0-core-split-ui-safe` / versionCode 23.
- Extracted `TranslationEngine` from `TranslatorService` for file/config loading, source validation, chunk preparation, prompt/API calls, retry/backoff, refinement, and partial output assembly.
- Added `JobStore` and `CheckpointStore` facades so UI/service code can move away from direct SQLite details.
- Updated `TranslatorService` to focus on Android lifecycle, foreground notification, pause/cancel state, repository updates, metrics, and broadcasts.
- Fixed a compile-risk duplicate field in `TranslationRepository.JobSummary`.
- Kept the safe v2.6.8/v2.6.9 tab navigation, but borrowed the useful v2.6.7 idea of a compact top app bar with current tab/provider/language context. No `DrawerLayout` was reintroduced.
- Kept debug features from v2.6.9: readable API error parser, debug trace export, prompt preview with matched glossary/pronoun rules.

## Notes

- This is a core-split release, not a full UI redesign. The purpose is to make future UI work safer so a visual change cannot easily break translation again.
- Full Android Gradle build still needs Android Studio or an Android SDK environment. The sandbox does not include Android SDK/Gradle wrapper.

# v2.6.8-safe-core-ui

- Reverted risky DrawerLayout navigation from v2.6.7 back to the stable direct tab bar from v2.6.6.
- Kept Android 15 safe-area padding to avoid drawing under phone status/navigation bars.
- Restored persistent runtime state for progress/Last Translation Preview after app background/reopen.
- Persisted service logs from TranslatorService so Activity Log is not lost when the Activity is stopped.
- Made translation extraction tolerate safe plain-text model responses when wrapper tags are missing, while still rejecting prompt echoes/refusals/meta commentary.
- Removed DrawerLayout dependency.

## 2.6.7-readera-safe-ui

- Fixed Android 15/status-bar overlap by handling system window insets in `MainActivity`.
- Replaced cramped horizontal top tabs with a compact top app bar and left navigation drawer.
- Added `androidx.drawerlayout:drawerlayout:1.2.0`.
- Kept translator core unchanged: API client, service, checkpoint, glossary/pronoun, chunking and retry logic were not modified.

# Changelog

## v2.6.6-no-sticky-header

### Changed files

- `app/build.gradle`
  - Bumped `versionCode` to `19` and `versionName` to `2.6.6-no-sticky-header`.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
  - Removed the sticky/auto-hiding app header from the root layout.
  - Removed the scroll listener that changed the header visibility while the user was dragging the page.
  - Kept only the compact tab bar at the top, so scrolling no longer changes root layout height and no longer produces a one-frame blink/jump.
  - Reduced tab bar vertical padding slightly to reclaim screen space on portrait phones.

### Notes

- This is the safer redesign for the reported header flicker: no `View.GONE`/`View.VISIBLE` is triggered during scroll, so Android does not recalculate the viewport mid-gesture.
- App identity remains in the launcher label, notification title, README, and per-page meta rows.
- No translation workflow, API, checkpoint, storage, glossary/pronoun, or file import/export behavior was changed.
- Full Android Gradle build still needs Android Studio or an Android SDK environment. The sandbox does not include Android SDK.

## v2.6.5-stable-header-scroll

### Changed files

- `app/build.gradle`
  - Bumped `versionCode` to `18` and `versionName` to `2.6.5-stable-header-scroll`.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
  - Fixed the portrait-screen jump/flicker caused by the v2.6.4 header auto-hide logic.
  - Replaced tiny-threshold show/hide toggling with hysteresis: the compact header hides only after a deliberate scroll down and reappears only when the page returns to the top.
  - This prevents Android from repeatedly recalculating the ScrollView height and bouncing the scroll position across the same threshold.
- `app/src/main/java/com/ml/tblandroidtxt/TranslatePageFactory.java`
  - Made the Activity Log inner scroll more stable by disabling nested scrolling and asking every parent container, including SwipeRefreshLayout, not to intercept touch while dragging inside the log panel.

### Notes

- No translation workflow, API, checkpoint, storage, glossary/pronoun, or file import/export behavior was changed.
- This is a UI stability patch for the jumpy scroll/header behavior reported from the screen recording.
- Full Android Gradle build still needs Android Studio or an Android SDK environment. The sandbox does not include Android SDK.


## v2.6.4-compact-header-scroll-log

### Changed files

- `app/build.gradle`
  - Bumped `versionCode` to `17` and `versionName` to `2.6.4-compact-header-scroll-log`.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
  - Rebuilt the top header as a compact single-line mobile header.
  - Shortened provider/language pills in the header to avoid the title wrapping into a huge card on portrait phones.
  - Added auto-hide behavior: when the current tab content is scrolled down, the big app header is hidden and reappears when scrolled back to the top.
  - Slightly reduced outer padding and tab button height to reclaim vertical space.
  - Page-level scroll views now keep a visible right-side scrollbar, closer to website-style scrolling.
- `app/src/main/java/com/ml/tblandroidtxt/TranslatePageFactory.java`
  - Activity Log inner panel now keeps its own always-visible vertical scrollbar.
  - Inner log scroll now asks the parent page not to intercept touch events while the user scrolls inside the log box.
  - Log text selectability was disabled in the inline panel to make touch scrolling reliable; Export Log remains available for copying/saving full logs.
  - Added a short hint explaining that the log box itself can be scrolled.

### Notes

- No translation workflow, API, glossary/pronoun, checkpoint, storage, or service behavior was changed.
- This patch addresses the two reported portrait-screen problems: oversized sticky header and Activity Log lacking an obvious scrollbar.
- Full Android Gradle build still needs Android Studio or an Android SDK environment. The sandbox does not include Android SDK.

## v2.6.3-tab-factory-refactor

### Changed files

- `app/build.gradle`
  - Bumped `versionCode` to `16` and `versionName` to `2.6.3-tab-factory-refactor`.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
  - Reduced the file from about 2,056 lines to about 1,474 lines.
  - Kept `MainActivity` as the app shell: lifecycle, tab switching, file picker results, service control, settings persistence, and shared helper methods.
  - Replaced large tab UI builders with small delegators.
- `app/src/main/java/com/ml/tblandroidtxt/TranslatePageFactory.java`
  - New factory for the Translate tab, tracking card, and scrollable Activity Log panel.
- `app/src/main/java/com/ml/tblandroidtxt/SettingsPageFactory.java`
  - New factory for Provider, Translation Options, Chunk/Runtime, and Custom Instructions settings UI.
- `app/src/main/java/com/ml/tblandroidtxt/GlossaryPageFactory.java`
  - New factory for glossary list, glossary cards, delete confirmation, and glossary editor UI.
- `app/src/main/java/com/ml/tblandroidtxt/FilesPageFactory.java`
  - New factory for Files tab, output folder scan card, and recent translated files list.
- `app/src/main/java/com/ml/tblandroidtxt/SamplePageFactory.java`
  - New factory for Sample Config UI.

### Notes

- This refactor does not change translation logic, checkpoint logic, API client behavior, glossary/pronoun filtering, or file format behavior.
- The goal is maintainability: future UI edits can be made per tab instead of editing one 2k-line `MainActivity`.
- `MainActivity` is still not tiny because it still owns shared event handlers and Activity Result handling; the next safe step is extracting picker/import/export controllers.
- Full Android Gradle build still needs Android Studio or an Android SDK environment. The sandbox does not include Android SDK.

## v2.6.2-ui-log-glossary-stats

### Changed files

- `app/build.gradle`
  - Bumped `versionCode` to `15` and `versionName` to `2.6.2-ui-log-glossary-stats`.
- `app/src/main/res/values/strings.xml`
  - Renamed app label to `Translate Books with LLMs`.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
  - Renamed header from `TBL` to `Translate Books with LLMs`.
  - Rebuilt Glossaries list as vertical cards, so `Use / Edit / Del` remain visible on portrait phones without horizontal scrolling.
  - Rebuilt glossary editor toolbar/buttons into portrait-safe rows.
  - Added delete confirmation for glossary deletion.
  - Glossary/YAML/.env/profile/pronoun import toasts now show the actual imported filename.
  - Multi-glossary import log/toast now lists imported filenames and per-file term counts.
  - Input estimate now shows word count, character count, line count, source-token estimate, chunk count, and cost estimate.
  - Activity Log now uses a fixed-height scrollable log panel with Export Log beside Clear Log.
  - Visible log text is capped to the newest tail to avoid endlessly stretching the Translate tab.
- `app/src/main/java/com/ml/tblandroidtxt/FileUtil.java`
  - Added text statistics helper for words/chars/lines/tokens.
- `app/src/main/java/com/ml/tblandroidtxt/LogStore.java`
  - Runtime log file is capped to a tail window instead of growing forever.
  - `read()` now returns the latest log tail, not the oldest bytes.
- `app/src/main/java/com/ml/tblandroidtxt/JobsPageFactory.java`
  - Runtime log panel in Jobs tab is now fixed-height and scrollable.
- `app/src/main/java/com/ml/tblandroidtxt/TranslatorService.java`
  - Foreground notification title renamed to `Translate Books with LLMs`.
- `app/src/main/java/com/ml/tblandroidtxt/OpenAICompatibleClient.java` / `ModelCatalog.java` / `SettingsStore.java` / `sample_configs/tbl_android.env`
  - User-facing title/comment text renamed away from `TBL Android TXT`.
- `README.md`, `REVIEW_REPORT.md`, `CHANGELOG.md`
  - Updated review, build notes, and changelog for this patch.

### Notes

- No Kotlin/Compose migration was done.
- No WorkManager rewrite was done; background translation still uses the existing foreground service + SQLite checkpoint design.
- Full Android Gradle build still needs Android Studio because this source package has no Gradle wrapper and the sandbox has no Android SDK.

## v2.6.1-stability-hardening

### Changed files

- `app/build.gradle`
  - Bumped `versionCode` to `14` and `versionName` to `2.6.1-stability-hardening`.
- `app/src/main/AndroidManifest.xml`
  - Enabled `android:usesCleartextTraffic="true"` to support local/LAN OpenAI-compatible HTTP endpoints. Keep HTTPS for public providers when possible.
- `app/src/main/java/com/ml/tblandroidtxt/AppValidator.java`
  - New validation/normalization helper for settings, source TXT content, and readable errors.
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
  - Start now validates normalized settings before launching the service.
  - Save settings stores normalized values.
  - Input estimate runs in a background thread instead of blocking the UI thread.
  - Clear Log clears the persisted `LogStore` as well as the visible log view.
- `app/src/main/java/com/ml/tblandroidtxt/TranslatorService.java`
  - Start/Resume/Retry validate normalized settings before API calls.
  - Empty or invalid TXT input is rejected before long-running job execution.
  - Cost limit now pauses the active checkpoint cleanly.
  - Final translation/refinement failures are marked on the affected chunk, so retry failed chunks can find them.
  - Retry failed chunks now persists repeated failures instead of silently losing the failed state.
  - Notification cleanup now guards null `NotificationManager`.
- `README.md`
  - Updated version notes, build notes, and remaining limits.

### Not changed

- No major UI redesign.
- No migration to Kotlin, Compose, Hilt, Room, or WorkManager in this stability pass.
- API keys are still stored in SharedPreferences; consider Android Keystore/EncryptedSharedPreferences in a later security pass.

## v2.6.9-debug-stable

- Bump version to `2.6.9-debug-stable` / versionCode 22.
- Added `ApiErrorParser` to classify common OpenAI-compatible API failures: 401, 403, 404, 413, 422, 429, 5xx, timeout, empty response, malformed/non-compatible JSON.
- Added `DebugTraceStore` for recent per-chunk API debug trace: prompt sent, raw model content, extracted translation, and error summary. API key is not stored in this trace.
- `Export log` now exports a debug bundle: runtime state + runtime log + API debug trace.
- `Clear log` now clears both runtime log and API debug trace.
- Prompt Preview now shows matched glossary/pronoun rules before the full system/user prompt.
- Retry logs now use readable API errors instead of raw exception text.
- Kept v2.6.8 stable tab UI and safe-area behavior; no major UI rewrite in this release.
