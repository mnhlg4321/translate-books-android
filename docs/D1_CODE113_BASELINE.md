# D1 Code113 Baseline — Canonical v4.17

Ngày kiểm kê: 2026-08-28 (+07:00)

## Authority

- Worktree duy nhất: `C:\Users\ADMIN\Documents\App Translate Books-translation-profile`
- Branch duy nhất: `feature/v4.17-translation-profile-compatibility`
- Canonical source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113)
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`
- Frozen later artifact `v4.16-dev.104` / code168 is context only and is not the D1 source baseline.
- No production source changed in this D1 reconciliation.
- `f888c62` and `0fd2d464` from the old v4.16 D1 track were used only as read-only comparison references; they are not canonical v4.17 commits.

The current canonical branch was directly checked out at the approved code113 source and then received only D0 documentation. Git-object comparison against the old `382e22d` source confirms that 10 of the 12 previously listed production files are identical. The two non-identical files were reopened directly on this branch:

- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java` — current code113 selection, settings snapshot, preview and dispatch path.
- `app/src/main/java/com/ml/tblandroidtxt/TranslationEngine.java` — current code113 input/config, prompt request, validation, retry and metric path.

## Flow map

| Stage | Current responsibility on canonical code113 |
|---|---|
| CSV/TXT import | `MainActivity.readSelectedTextFiles` reads selected SAF files through `FileUtil.readText`; the v4.17 workflow selects one RAW chapter and matching profile files manually. |
| Import planning | `LibraryImportPlanner.glossaries` calls `GlossaryStore.parseTerms`; `LibraryImportPlanner.pronouns` validates text and stores the original text in a `PronounStore.Profile`. |
| Parser | `PromptContextBuilder.validate` calls the private Glossary and Pronoun text/JSON parsers. CSV is split with quoted-comma support; legacy header heuristics run before row projection. |
| Data model/store | `GlossaryStore.Term` has only `source`, `target`, `category`; `PronounStore.Profile` has identity/URI/name plus raw `text`. Parsed `TermEntry`/`PronounRule` are transient. |
| Persistence | Glossaries are serialized as SharedPreferences JSON containing only the three `Term` fields. Pronoun profiles are serialized as SharedPreferences JSON containing raw text; there is no parsed-rule persistence. |
| Active profile | `MainActivity` selects a `GlossaryStore.Glossary` or `PronounStore.Profile`. Glossary selection renders prompt text; Pronoun selection copies raw profile text. |
| Settings snapshot | `AppSettings`, `SettingsStore` and `SettingsStore.toJson/fromJson` retain `glossaryText` and `pronounText` strings, IDs and names. The snapshot is not a parsed object graph. |
| Chunk matching | `PromptContextBuilder.match` parses the snapshot strings, ranks relevant terms/rules, dedupes rendered locks and applies default limits 80/40. |
| Prompt compiler | `PromptPlan.translation` builds rule context, calls `buildWithRuleContext`, and places matched locks in the system prompt. The full optimization preset retains a legacy `PromptBuilder` path. |
| Prompt preview | `PromptPreviewDialog` reads the first input and calls `PromptContextBuilder.preview` for the selected chunk. Its matching view uses the main chunk; actual translation also has surrounding context and history. |
| Token estimator | `CostEstimator.estimatePreparedChunks` builds `PromptPlan` objects and breaks down lock tokens. The current `PreparationCoordinator` UI path uses `estimatePreparedChunksLightweight`, which is source-only and does not parse/build prompt locks. |
| Translation | `MainActivity.startTranslationNow` sends `SettingsStore.toJson(s)` and the prepared-batch ID to `TranslatorService`. `TranslatorService` loads/normalizes settings and delegates input preparation and provider attempts to `TranslationEngine`. |
| Resume/retry | `TranslationRepository` persists prepared batches, job settings JSON, chunk rows and attempt state. `TranslatorService.resumeJobById` and retry paths reload `job.settingsJson`; they do not snapshot the current UI again. Delivery-unknown responses are not automatically resent. |

## Glossary — actual five-column schema

The D1 fixture uses the real schema:

```csv
source,target,category,note,priority
クロ,Kuro,character,Tên Mercedes đặt cho nhân vật này,high
```

### Parser projection

The text path in `PromptContextBuilder.parseTermsDetailed` (`PromptContextBuilder.java:275-298`) behaves as follows:

1. The header is skipped by the heuristic at line 280 because it contains `source` and `target`.
2. `cols[0]` becomes `TermEntry.source` (`クロ`).
3. `cols[1]` becomes `TermEntry.target` (`Kuro`).
4. `cols[2]` becomes `TermEntry.category` (`character`).
5. `cols[3]` becomes the transient `TermEntry.aliases` field (`Tên Mercedes đặt cho nhân vật này`). This is the current temporary interpretation of the real `note` column.
6. `cols[4]` is not assigned by the CSV text path. `TermEntry.priority` therefore remains its default `0` for this row; the raw value `high` is ignored.

This is not the old mistaken `source,target,note` interpretation. With the five-column schema, note does not become category: category remains column 3, note is temporarily read as aliases.

The JSON parser has a separate compatibility path (`PromptContextBuilder.java:242-269`) that can read a numeric `priority` into transient `TermEntry.priority` and the matcher sorts by it. That path does not make the fifth CSV column supported, and the store adapter still drops the transient value. D2 must follow the v4.17 authority and ignore priority at runtime.

### Adapter, persistence and prompt

`GlossaryStore.parseTerms` (`GlossaryStore.java:121-130`) creates a new `GlossaryStore.Term` with only `source`, `target` and `category`. Consequently:

- source is retained;
- target is retained;
- category is retained;
- `TermEntry.aliases`/the source note is lost at the adapter;
- priority has no destination and is lost;
- `GlossaryStore.Term` has no `note`, `aliases` or `priority` field (`GlossaryStore.java:24-31`).

`GlossaryStore.toPromptText` (`GlossaryStore.java:107-118`) emits only:

```text
クロ => Kuro [character]
```

The note and `high` are absent. Glossary JSON persistence (`GlossaryStore.java:197-221`) round-trips only the same three fields, so save/reload cannot restore the note or priority.

### Dedupe, matching and limit

- Store-level dedupe key: `lower(source) + "\\u0000" + lower(target)` (`GlossaryStore.java:155-165`). Category, note/aliases and priority are excluded.
- Compiler-level dedupe uses the same normalized source/target pair (`PromptContextBuilder.java:163-177`).
- Matching normalizes NFKC, lowercases and trims; it checks source, target and transient aliases, with Latin boundary checks and CJK substring matching (`PromptContextBuilder.java:578-605`).
- Global/mandatory/always categories bypass ordinary occurrence matching.
- Terms are ranked, then at most `AppSettings.glossaryInjectLimit` are injected. The default is 80 (`PromptContextBuilder.java:128-177`, `AppSettings.java:51`, `AppValidator.java:32`).
- The editing UI separately shows at most 80 preview rows (`MainActivity.java:2100-2113`).

### Required D1 conclusion

With the actual five-column schema:

- note does not become category;
- category is still the third column;
- note in the fourth column is temporarily read as aliases;
- note is lost when `TermEntry` is adapted to `GlossaryStore.Term`;
- priority in the fifth column is dropped by the CSV text parser and has no store/prompt field.

## Pronoun — actual P3 schema

The D1 fixture uses:

```csv
from,speaker,target,self,call,scope,note
私,Mercedes,Basil,ta,ngươi,CH004:p052-p153,Fixture P3 thật của Mercedes
```

### Current parser behavior

`PromptContextBuilder.parsePronounRulesDetailed` (`PromptContextBuilder.java:324-384`) has two relevant branches:

- Legacy header skip (`line 367`) requires `from`, `to`, and one of `pronoun`, `xưng` or `pair`. The real P3 header has `from` but has no `to` token, so it is not recognized as a legacy header.
- For every non-arrow CSV row with at least three columns (`lines 369-374`), the parser assigns only `cols[0]`, `cols[1]` and `cols[2]` to `sourceName`, `targetName` and `details`, then formats `source → target: details`.

Therefore the real P3 header becomes:

```text
from → speaker: target
```

and the data row becomes:

```text
私 → Mercedes: Basil
```

The current `PronounRule` model contains only `text`, `key`, `sourceName`, `targetName`, `details` and `lineNumber` (`PromptContextBuilder.java:50-57`). It has no `self`, `call`, `scope` or `note` fields. The four tail values are not parsed into a discarded side object; they are lost at the parser projection. The raw seven-column text remains in `PronounStore.Profile.text`.

The legacy input remains compatible:

```csv
from,to,pronoun
Alice,Bob,chị/em
```

Its header is skipped and the data row becomes `Alice → Bob: chị/em`. The compiler matches this legacy rule against a chunk containing Alice and Bob.

### Matching, dedupe and limit

- Explicit Pronoun profiles are selected from raw text. `PronounStore.Profile.count()` reparses the raw text and counts `explicitPronouns` (`PronounStore.java:17-20`).
- Rules are ranked using source/target cue occurrences, dialogue-window participation, CJK aliases from `details`, and rule context (`PromptContextBuilder.java:439-472`). Under the P3 bug, Mercedes is treated as the target cue and Basil as details; the actual P3 semantics are not available.
- Rendered rule lines are deduped case-insensitively (`PromptContextBuilder.java:179-191`).
- Global/mandatory/always rules are eligible without a chunk cue.
- At most `AppSettings.pronounInjectLimit` rules are injected; the default is 40 (`PromptContextBuilder.java:128-132,179-191`, `AppSettings.java:52`, `AppValidator.java:33`).
- The top eligible rules may be rendered as full lines and later rules compacted; this is separate from the 40-rule hard injection cap.

### Active profile and save/reload

`PronounStore` saves raw `Profile.text` plus metadata in SharedPreferences JSON (`PronounStore.java:22-36`) and reloads that same raw text. `MainActivity.applyActivePronoun` copies the raw text to `AppSettings.pronounText` (`MainActivity.java:1123-1131`). `SettingsStore` persists that raw string and `SettingsStore.toJson/fromJson` carries it into the start intent (`SettingsStore.java:11-52,63-106,114-160`). No parsed `PronounRule` object is snapshotted.

## Direct revalidation of MainActivity and TranslationEngine

These two files differ from the old code168-track comparison and were read directly on the canonical branch.

### MainActivity.java

- `onCreate` loads `SettingsStore`, performs legacy Pronoun migration, then copies the selected profile's raw text/metadata back into settings (`MainActivity.java:203-221`).
- Glossary import validates/merges parsed `GlossaryStore.Term` objects and selects the first imported Glossary; Pronoun import validates but stores the original raw text (`MainActivity.java:1028-1084`).
- `collectSettings` prefers the selected Glossary's rendered prompt text and the selected Pronoun's raw profile text (`MainActivity.java:1649-1705`).
- Glossary selection persists the selected ID and rendered three-field prompt; Pronoun selection persists raw profile text (`MainActivity.java:2067-2087`, `1123-1131`).
- Profile JSON import is an `AppSettings` snapshot import and does not rebuild Glossary/Pronoun store objects (`MainActivity.java:1332-1342`).
- Prompt preview uses the first input and the current settings (`MainActivity.java:2276-2285`).
- Start saves settings and sends the JSON snapshot plus prepared-batch ID to `TranslatorService` (`MainActivity.java:1411-1452`).
- Resume/retry buttons send service actions; they do not attach fresh settings (`MainActivity.java:1513-1568`).

### TranslationEngine.java

- `readInputUris` accepts the current JSON string form, a StringArrayList fallback and a single URI fallback (`TranslationEngine.java:62-80`).
- `loadExternalConfig` reads env/YAML and optional Glossary/Pronoun URIs as text, then normalizes the settings (`TranslationEngine.java:83-110`).
- `prepareInput` reads the RAW chapter, validates it, chunks it and records input/settings hashes (`TranslationEngine.java:113-130`).
- `translateWithRetry` builds `PromptPlan.forTranslation`, records a full-preset baseline plan, calls the injected/default provider, validates strict output, records metrics and applies retry policy (`TranslationEngine.java:133-210`).
- Refinement repeats the same prompt/validation/retry structure (`TranslationEngine.java:226-288`).
- The canonical code113 engine uses the direct `ProviderClient` path; this D1 report makes no RSC/Editorial activation claim.

## Prompt, preview, estimator and persistence details

- `PromptContextBuilder.validate` parses both raw settings strings; `match` creates the compact `ContextBlock` and reports counts/chars.
- `PromptPlan.translation` combines `contextBefore + mainContent` for rule context and inserts matched Glossary/Pronoun locks into the system prompt (`PromptPlan.java:38-60`).
- `PromptPreviewDialog` calls `PromptContextBuilder.preview` with the main chunk only (`PromptPreviewDialog.java:20-65`), so preview matching can differ from actual translation when surrounding context contributes a Pronoun cue.
- Full estimation calls `PromptPlan.forTranslation` per chunk and can include refinement estimates (`CostEstimator.java:18-81`). Current prepared-batch UI estimation intentionally calls `CostEstimator.estimatePreparedChunksLightweight` (`PreparationCoordinator.java:103-148`), whose tokenizer state is a source-only approximation and does not reparse locks.
- `TranslationRepository.savePreparedBatch` stores the prepared settings JSON, estimates and exact chunks; job creation stores settings JSON and hashes (`TranslationRepository.java:153-184,276-327`).
- On recovery, interrupted pre-send work is retryable while sent-without-durable-response work becomes delivery-unknown (`TranslationRepository.java:129-151`). Accepted responses are preserved when a later response is rejected; validated chunks are committed separately (`TranslationRepository.java:432-507`).

## TranslatorService, resume and retry

- New jobs use supplied `settings` JSON when present, otherwise `SettingsStore.load`; `loadExternalConfig` is then applied, with a selected Glossary fallback if raw Glossary text is empty (`TranslatorService.java:250-275`).
- Prepared jobs load the exact prepared batch and its settings snapshot before dispatch (`TranslatorService.java:323-352`).
- Resume loads `job.settingsJson`, verifies the current input hash, recovers durable responses, refuses automatic resend for `DELIVERY_UNKNOWN`, skips completed chunks and continues remaining work (`TranslatorService.java:413-494`).
- Retry also loads `job.settingsJson`, targets failed chunks, retains completed output as prior context and commits accepted retry results (`TranslatorService.java:504-612`).
- Translation and refinement calls flow through `TranslationEngine`; chunk errors are classified by `RetryPolicy`, and context-overflow splitting preserves the existing prompt settings/locks (`TranslatorService.java:614-713`).

## Controlled diagnostic results

The committed test is characterization only. Its assertions explicitly label the current projection as an error and must not be treated as the D3 contract.

### Pronoun P3

```text
baseline.code113.expected.error=true
raw.header.column.count=7
raw.data.column.count=7
header.rule=from → speaker: target
data.rule=私 → Mercedes: Basil
data.projected.from=私
data.projected.speaker=Mercedes
data.projected.target=Basil
tail.self.call.scope.note=LOST
legacy3.rule=Alice → Bob: chị/em
legacy3.match.count=1
default.pronoun.limit.observed=40
```

### Glossary five-column

```text
baseline.code113.expected.error=true
raw.row.column.count=5
entry.source=クロ
entry.target=Kuro
entry.category=character
entry.aliases(note)=Tên Mercedes đặt cho nhân vật này
entry.priority=0 (fifth CSV column ignored)
store.term=source,target,category only
prompt.line=クロ => Kuro [character]
store.adapter.note.aliases=LOST
store.adapter.priority=LOST
dedupe.same-source-target.count=1
default.glossary.limit.observed=80
```

## Exact test evidence

All commands used Android Studio JDK21 in the same PowerShell shell:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_HOME = $taskJavaHome
$env:Path = "$taskJavaHome\bin;$env:Path"
$env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

The first A invocation used the required JDK variables but no SDK environment variable and failed before Gradle could resolve tests:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryImportV48Test --tests com.ml.tblandroidtxt.PronounProfileTest --tests com.ml.tblandroidtxt.PromptContextQaTest --tests com.ml.tblandroidtxt.LibraryImportPlannerTest --tests com.ml.tblandroidtxt.TranslationConfigStateTest --tests com.ml.tblandroidtxt.CostOptimizationTest --console=plain
```

Result: `FAILED_REPAIRING` before tests because SDK location was not configured. Read-only diagnosis found `C:\Users\ADMIN\AppData\Local\Android\Sdk`; the reruns set `ANDROID_HOME`/`ANDROID_SDK_ROOT` in-shell only. No repo configuration was written.

The repaired focused commands were:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryImportV48Test --tests com.ml.tblandroidtxt.PronounProfileTest --tests com.ml.tblandroidtxt.PromptContextQaTest --tests com.ml.tblandroidtxt.LibraryImportPlannerTest --tests com.ml.tblandroidtxt.TranslationConfigStateTest --tests com.ml.tblandroidtxt.CostOptimizationTest --console=plain
```

Result: `34/34 PASS`.

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.ModelFlowV42IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV43Test --tests com.ml.tblandroidtxt.ReliabilityV43IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV44Test --tests com.ml.tblandroidtxt.CoreRecovery45Test --tests com.ml.tblandroidtxt.RuntimeStateSnapshotTest --tests com.ml.tblandroidtxt.Hotfix441Test --tests com.ml.tblandroidtxt.Patch312Test --tests com.ml.tblandroidtxt.V46DashboardTest --console=plain
```

Result: `49/49 PASS`.

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --tests com.ml.tblandroidtxt.Code113GlossaryProjectionDiagnosticFixtureTest --console=plain
```

Result: `2/2 PASS`.

The final combined A+B+C+D command (same JDK/SDK setup) completed with `85 tests, 0 failures, 0 errors, 0 skipped`:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryImportV48Test --tests com.ml.tblandroidtxt.PronounProfileTest --tests com.ml.tblandroidtxt.PromptContextQaTest --tests com.ml.tblandroidtxt.LibraryImportPlannerTest --tests com.ml.tblandroidtxt.TranslationConfigStateTest --tests com.ml.tblandroidtxt.CostOptimizationTest --tests com.ml.tblandroidtxt.ModelFlowV42IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV43Test --tests com.ml.tblandroidtxt.ReliabilityV43IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV44Test --tests com.ml.tblandroidtxt.CoreRecovery45Test --tests com.ml.tblandroidtxt.RuntimeStateSnapshotTest --tests com.ml.tblandroidtxt.Hotfix441Test --tests com.ml.tblandroidtxt.Patch312Test --tests com.ml.tblandroidtxt.V46DashboardTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --tests com.ml.tblandroidtxt.Code113GlossaryProjectionDiagnosticFixtureTest --console=plain
```

Not run by explicit scope: `assembleDebug`, `scripts/build-and-save.ps1`, Android instrumentation, emulator/AVD, real provider/API, RSC, Editorial and IPC tests. No Android/device/API evidence is claimed.

## D1 result and D2/D3 boundary

D1 canonical result: `COMPLETE`.

Smallest production set identified for later work, without implementing it here:

- D2 Glossary4: `PromptContextBuilder.java` and `GlossaryStore.java`; add `MainActivity.java` only if the editor UI must expose/edit note. `LibraryImportPlanner` already delegates through the adapter. No priority runtime behavior should be introduced.
- D3 Pronoun7: `PromptContextBuilder.java` is the minimum parser/model/compiler change. Current raw `PronounStore`, `AppSettings`, `SettingsStore`, `MainActivity` and job settings JSON already preserve the original text; additional persistence changes are needed only if D3 changes the snapshot representation from raw text to parsed objects.

The canonical phase may advance to `D2_GLOSSARY4` after this characterization, but this D1 task does not implement D2. The exact next action is to write focused failing/acceptance tests for Glossary4 before changing production.

## Scope and diff guard

- Production source diff for this reconciliation: `0` files under `app/src/main`.
- D1 changes are limited to test fixtures, test-only Java, the canonical report, the existing v4.17 checklist, current-only plan/state/snapshot updates.
- The canonical D1 diff itself passes `git diff --check`.
- No workspace v4.16 file was changed, staged or reset.
