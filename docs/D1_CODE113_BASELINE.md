# D1 Code113 Baseline — Canonical v4.17

Ngày kiểm kê D1: 2026-08-28 (+07:00)
Reconciliation D3: 2026-08-29 (+07:00)

## Authority

- Worktree duy nhất: `C:\Users\ADMIN\Documents\App Translate Books-translation-profile`
- Branch duy nhất: `feature/v4.17-translation-profile-compatibility`
- Canonical source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7` (`v4.16-dev.51` / code113)
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`
- Frozen later artifact `v4.16-dev.104` / code168 is context only and is not the D1 source baseline.
- No production source changed in the D1 reconciliation itself; the later D3 implementation is recorded separately below.
- `f888c62` and `0fd2d464` from the old v4.16 D1 track were used only as read-only comparison references; they are not canonical v4.17 commits.

The current canonical branch was directly checked out at the approved code113 source and then received only D0 documentation. Git-object comparison against the old `382e22d` source confirms that 10 of the 12 previously listed production files are identical. The two non-identical files were reopened directly on this branch:

- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java` — current code113 selection, settings snapshot, preview and dispatch path.
- `app/src/main/java/com/ml/tblandroidtxt/TranslationEngine.java` — current code113 input/config, prompt request, validation, retry and metric path.

## Flow map — D1 baseline, D2 carry-forward and D3 runtime

| Stage | Current responsibility on canonical code113 |
|---|---|
| CSV/TXT import | `MainActivity.readSelectedTextFiles` reads selected SAF files through `FileUtil.readText`; the v4.17 workflow selects one RAW chapter and matching profile files manually. |
| Import planning | `LibraryImportPlanner.glossaries` calls `GlossaryStore.parseTerms`; `LibraryImportPlanner.pronouns` validates text and stores the original text in a `PronounStore.Profile`. |
| Parser | `PromptContextBuilder.validate` calls the private Glossary and Pronoun text/JSON parsers. Glossary CSV uses the existing quoted-comma parser and maps the four runtime columns; Pronoun CSV now selects exact legacy-three/P3-seven format, skips its exact header and rejects unsupported/multiline CSV rows without legacy fallback. |
| Data model/store | D1 baseline: `GlossaryStore.Term` had only `source`, `target`, `category`. D2 result: it has exactly `source`, `target`, `category`, `note`. D3 result: transient `PronounRule` keeps `from`, `speaker`, `target`, `self`, `call`, `scope`, `note` plus compatibility fields; `PronounStore.Profile` remains identity/URI/name plus raw `text`. |
| Persistence | D2 JSON contains Glossary `source`, `target`, `category`, `note` and omits `priority`; legacy JSON without `note` loads empty. D3 does not add a Pronoun schema: raw P3 text remains the persisted profile/job value and is reparsed at runtime. |
| Active profile | `MainActivity` selects a `GlossaryStore.Glossary` or `PronounStore.Profile`. Glossary selection renders prompt text; Pronoun selection copies raw profile text. |
| Settings snapshot | `AppSettings`, `SettingsStore` and `SettingsStore.toJson/fromJson` retain `glossaryText` and `pronounText` strings, IDs and names. The snapshot is not a parsed object graph. |
| Chunk matching | `PromptContextBuilder.match` parses the snapshot strings, applies P3 scope overlap and `from` cue matching, ranks legacy rules, dedupes eligible locks and applies default limits 80/40. P3 scope `*`/empty remains cue-gated; explicit ranges fail closed when chunk range is unknown. |
| Prompt compiler | `PromptPlan.translation` and the full `PromptBuilder` path pass chunk paragraph ranges and bounded context to the same compiler; only matched locks enter the system prompt. P3 renders the semantic `speaker → target: self/call | note` line and does not render scope. |
| Prompt preview | `PromptPreviewDialog` chunks the selected input and calls the same range/context-aware `PromptContextBuilder.preview` path as runtime; the displayed matched block therefore follows the compiler's P3 scope/cue result. |
| Token estimator | `CostEstimator.estimatePreparedChunks` builds `PromptPlan` objects and counts only compiled eligible locks. The current `PreparationCoordinator` UI path still uses `estimatePreparedChunksLightweight`, which is intentionally a source-only approximation and does not parse/build prompt locks. |
| Translation | `MainActivity.startTranslationNow` sends `SettingsStore.toJson(s)` and the prepared-batch ID to `TranslatorService`. `TranslatorService` loads/normalizes settings and delegates input preparation and provider attempts to `TranslationEngine`. |
| Resume/retry | `TranslationRepository` persists prepared batches, job settings JSON, chunk rows and attempt state. On prepared reload, resume, retry and context-overflow replacement, `TranslatorService` reconstructs in-memory paragraph ranges from ordered main content; all paths reload `job.settingsJson` and do not snapshot the current UI again. Delivery-unknown responses are not automatically resent. |

## Glossary — actual five-column schema (D1 observation before D2)

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

The JSON parser had a separate compatibility path (`PromptContextBuilder.java:242-269`) that could read a numeric `priority` into transient `TermEntry.priority` and the matcher sorted by it. That was a D1 baseline observation; D2 removes that runtime use and follows the v4.17 authority by ignoring priority.

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

### Required D1 conclusion (baseline bug, not the D2 contract)

With the actual five-column schema:

- note does not become category;
- category is still the third column;
- note in the fourth column is temporarily read as aliases;
- note is lost when `TermEntry` is adapted to `GlossaryStore.Term`;
- priority in the fifth column is dropped by the CSV text parser and has no store/prompt field.

## D2 reconciliation — Glossary4 result

The D1 observation above is intentionally preserved as the pre-change characterization of canonical code113. D2 now projects the same real five-column fixture as follows:

- `PromptContextBuilder.TermEntry` has a separate `note` field; `cols[0]`, `cols[1]`, `cols[2]` and `cols[3]` map to source, target, category and note. `cols[4]` and later columns are not assigned.
- A four-column file defaults column four to `note`. Only the explicit header `source,target,category,aliases` selects the compatibility `aliases` field. The standard `note` and `note,priority` headers never route note into aliases.
- UTF-8 BOM is removed by the existing field cleaner; quoted commas and doubled escaped quotes survive. Multiline CSV fields are rejected with `multiline CSV fields are not supported`; no partial row is injected.
- `GlossaryStore.Term` now has exactly `source`, `target`, `category` and `note`. The three-field constructor remains equivalent to an empty note; the four-field constructor normalizes a null note to `""`. No `priority` or aliases field exists on the runtime Term.
- `GlossaryStore.parseTerms` preserves the four runtime fields. `toJson/fromJson` writes/reads the note key, accepts legacy JSON without it as an empty note, and never writes priority.
- `GlossaryStore.toPromptText` renders a non-empty note once after the first ` | ` delimiter, normalizing CR/LF to spaces. `PromptContextBuilder` parses internal `Source, Target, Category` compact text by taking everything after the first delimiter as note, so a note containing `|` round-trips deterministically.
- Matching and dedupe remain source/target/explicit-alias based. Note is not a match cue, global flag, pronoun cue or ranking input. The lowercased `source + NUL + target` key and default limit 80 remain unchanged; the Pronoun limit remains 40.
- `PromptPlan`, prompt preview and full `CostEstimator` already consume the `PromptContextBuilder` compiler output, so a matched note is counted once and an unmatched note or priority adds no prompt tokens. The source-only prepared-batch lightweight estimate remains intentionally source-only and is not a replacement for the exact plan estimator.

Current D2 implementation references: `GlossaryStore.java:24-39,114-140,206-234`; `PromptContextBuilder.java:34-49,276-390,637-645,884-902`.

## Pronoun — actual P3 schema (D1 observation before D3)

The D1 fixture uses:

```csv
from,speaker,target,self,call,scope,note
私,Mercedes,Basil,ta,ngươi,CH004:p052-p153,Fixture P3 thật của Mercedes
```

### D1 pre-D3 parser behavior

On the canonical source baseline `a9409ff` before D3, `PromptContextBuilder.parsePronounRulesDetailed` (`PromptContextBuilder.java:324-384` in that baseline) had two relevant branches:

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

The D1 `PronounRule` model contained only `text`, `key`, `sourceName`, `targetName`, `details` and `lineNumber` (`PromptContextBuilder.java:50-57` in the baseline). It had no `self`, `call`, `scope` or `note` fields. The four tail values were not parsed into a discarded side object; they were lost at the parser projection. The raw seven-column text remained in `PronounStore.Profile.text`.

The legacy input remains compatible:

```csv
from,to,pronoun
Alice,Bob,chị/em
```

Its header is skipped and the data row becomes `Alice → Bob: chị/em`. The compiler matches this legacy rule against a chunk containing Alice and Bob.

### Matching, dedupe and limit

- Explicit Pronoun profiles are selected from raw text. `PronounStore.Profile.count()` reparses the raw text and counts `explicitPronouns` (`PronounStore.java:17-20`).
- D1 rules were ranked using source/target cue occurrences, dialogue-window participation, CJK aliases from `details`, and rule context (`PromptContextBuilder.java:439-472` in the baseline). Under the P3 bug, Mercedes was treated as the target cue and Basil as details; the actual P3 semantics were not available.
- Rendered rule lines are deduped case-insensitively (`PromptContextBuilder.java:179-191`).
- Global/mandatory/always rules are eligible without a chunk cue.
- At most `AppSettings.pronounInjectLimit` rules are injected; the default is 40 (`PromptContextBuilder.java:128-132,179-191`, `AppSettings.java:52`, `AppValidator.java:33`).
- The top eligible rules may be rendered as full lines and later rules compacted; this is separate from the 40-rule hard injection cap.

### Active profile and save/reload

`PronounStore` saves raw `Profile.text` plus metadata in SharedPreferences JSON (`PronounStore.java:22-36`) and reloads that same raw text. `MainActivity.applyActivePronoun` copies the raw text to `AppSettings.pronounText` (`MainActivity.java:1123-1131`). `SettingsStore` persists that raw string and `SettingsStore.toJson/fromJson` carries it into the start intent (`SettingsStore.java:11-52,63-106,114-160`). No parsed `PronounRule` object is snapshotted.

## D3 reconciliation — Pronoun7 result

The D1 P3 diagnostic was a controlled characterization of the canonical code113 bug, not a desired contract. D3 now keeps the raw-profile architecture and changes only the parser/model/compiler and the in-memory paragraph-range plumbing needed to consume the real P3 schema:

```text
from,speaker,target,self,call,scope,note
私,Mercedes,Basil,ta,ngươi,CH004:p052-p153,Fixture P3 thật của Mercedes
```

- Exact P3 headers are skipped. With an exact header, rows must have seven columns; malformed P3 rows do not fall back to legacy three-column parsing. Headerless rows use exactly three columns as legacy or exactly seven as P3; unsupported Pronoun-shaped headers are rejected instead of triggering column-count inference.
- P3 maps `cols[0..6]` to `from,speaker,target,self,call,scope,note`. Compatibility fields remain explicit: `sourceName=from`, `targetName=target`, and `details` is empty; P3 matching/compiler uses the semantic fields, not the legacy projection.
- UTF-8 BOM, CRLF/LF, Unicode, empty `self`/`call`/`note`, quoted commas, escaped quotes and quoted legacy pronouns are handled by the existing line parser. Multiline CSV fields are rejected with a malformed-row warning and no partial P3 rule.
- Required P3 fields are `from`, `speaker` and `target`; empty `self`, `call` and `note` are valid. Invalid scopes (`p000`, reversed ranges, invalid prefixes/text) remain visible as malformed diagnostics but are fail-closed and never injected. Empty/`*` is chapter-wide but still requires the `from` cue.
- Paragraphs are non-empty blocks separated by blank lines, numbered from 1 after CRLF/CR normalization. `Chunker.chunkText` annotates each chunk with inclusive `paragraphStart/paragraphEnd`; `Chunker.assignParagraphRanges` re-annotates ordered persisted/retry lists without changing offsets, source, hashes or stable IDs. No database column was added.
- A P3 rule is eligible only when its valid scope overlaps the chunk range and `from` occurs in the main chunk or bounded context. Speaker, target and note alone are not cues. Semantic P3 dedupe is `speaker,target,self,call,note` after scope filtering; scope is deliberately excluded from the dedupe payload. The Pronoun injection limit remains 40.
- P3 compiles compactly as `speaker → target: self/call | note`; blank sides use `(omit)`, both blank sides use `omit pronouns`, and note is emitted once only when the eligible rule has a non-empty note. Scope, CSV headers, raw CSV and unmatched notes are not sent to the model.
- `PromptPlan.translation`, the full optimization path, prompt preview and exact `CostEstimator` use the same range/context-aware compiler. `TranslatorService` re-annotates fresh prepared chunks, prepared reloads, resume/retry lists and context-overflow replacement lists. Job settings continue to snapshot the exact raw Pronoun text; active-profile changes after job creation cannot replace it.

The D3 implementation references on the post-D3 working tree are `PromptContextBuilder.java:52-73,393-625,659-767`, `Chunk.java:4-12`, `Chunker.java:12-176`, `PromptPlan.java:31-76`, `PromptBuilder.java:9-105`, `PromptPreviewDialog.java:58-66`, `TranslationRepository.java:184-188` and `TranslatorService.java:347-350,418-458,582-601,663-673`. These are current implementation references; the D1 bug references above remain explicitly anchored to the pre-D3 `a9409ff` source.

## Direct revalidation of MainActivity and TranslationEngine

These two files differ from the old code168-track comparison and were read directly on the canonical branch.

### MainActivity.java

- `onCreate` loads `SettingsStore`, performs legacy Pronoun migration, then copies the selected profile's raw text/metadata back into settings (`MainActivity.java:203-221`).
- Glossary import validates/merges parsed `GlossaryStore.Term` objects and selects the first imported Glossary; Pronoun import validates but stores the original raw text (`MainActivity.java:1028-1084`).
- `collectSettings` prefers the selected Glossary's rendered prompt text and the selected Pronoun's raw profile text (`MainActivity.java:1649-1705`).
- Glossary selection persists the selected ID and rendered compact prompt text with the four runtime fields; Pronoun selection persists raw profile text (`MainActivity.java:2067-2087`, `1123-1131`).
- Profile JSON import is an `AppSettings` snapshot import and does not rebuild Glossary/Pronoun store objects (`MainActivity.java:1332-1342`).
- Prompt preview uses the first input and current settings; `PromptPreviewDialog` now receives chunks already annotated by `Chunker.chunkText` and uses the same scoped compiler as runtime (`MainActivity.java:2276-2285`, `PromptPreviewDialog.java:20-66`).
- Start saves settings and sends the JSON snapshot plus prepared-batch ID to `TranslatorService` (`MainActivity.java:1411-1452`).
- Resume/retry buttons send service actions; they do not attach fresh settings (`MainActivity.java:1513-1568`).

### TranslationEngine.java

- `readInputUris` accepts the current JSON string form, a StringArrayList fallback and a single URI fallback (`TranslationEngine.java:62-80`).
- `loadExternalConfig` reads env/YAML and optional Glossary/Pronoun URIs as text, then normalizes the settings (`TranslationEngine.java:83-110`).
- `prepareInput` reads the RAW chapter, validates it, chunks it (including in-memory paragraph ranges) and records input/settings hashes (`TranslationEngine.java:113-130`, `Chunker.java:12-40`).
- `translateWithRetry` builds `PromptPlan.forTranslation`, records a full-preset baseline plan, calls the injected/default provider, validates strict output, records metrics and applies retry policy (`TranslationEngine.java:133-210`).
- Refinement repeats the same prompt/validation/retry structure (`TranslationEngine.java:226-288`).
- The canonical code113 engine uses the direct `ProviderClient` path; this D1 report makes no RSC/Editorial activation claim.

## Prompt, preview, estimator and persistence details

- `PromptContextBuilder.validate` parses both raw settings strings; `match` creates the compact `ContextBlock` and reports counts/chars.
- `PromptPlan.translation` combines `contextBefore + mainContent`, passes the chunk paragraph range and inserts matched Glossary/Pronoun locks into the system prompt (`PromptPlan.java:31-61`). The full preset now passes the same range/context to its legacy-shaped prompt path (`PromptPlan.java:64-76`, `PromptBuilder.java:84-99`).
- `PromptPreviewDialog` calls `PromptContextBuilder.preview` with the same main chunk, bounded context and paragraph range used by runtime (`PromptPreviewDialog.java:58-66`), so a context-only P3 cue is not lost in the matching view.
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

The Pronoun diagnostic was converted from D1 characterization into a D3 regression proof. Its old output is preserved above as the pre-D3 code113 observation and is not a desired contract. The Glossary diagnostic remains the D2 regression proof of the corrected four-field behavior.

### Pronoun P3

```text
d3.pronoun7.expected=true
raw.header.column.count=7
raw.data.column.count=7
header.rule=SKIPPED
data.rule=Mercedes → Basil: ta/ngươi | Fixture P3 thật của Mercedes
data.projected.from=私
data.projected.speaker=Mercedes
data.projected.target=Basil
data.self=ta
data.call=ngươi
data.scope=CH004:p052-p153
data.note=Fixture P3 thật của Mercedes
legacy3.rule=Alice → Bob: chị/em
legacy3.match.count=1
default.pronoun.limit.observed=40
```

### Glossary five-column

```text
d2.glossary4.expected=true
raw.row.column.count=5
entry.source=クロ
entry.target=Kuro
entry.category=character
entry.note=Tên Mercedes đặt cho nhân vật này
entry.aliases= (aliases require explicit header)
entry.priority=IGNORED (fifth CSV column ignored)
store.term=source,target,category,note
prompt.line=クロ => Kuro [character] | Tên Mercedes đặt cho nhân vật này
store.adapter.note=PRESERVED
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

The D2 acceptance test was intentionally run before the production patch:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryFourFieldRuntimeTest --console=plain
```

Result before production change: `8 tests completed, 8 failed`; this was the expected D2 red result for the missing note field/constructor/adapter/compiler behavior. A test-only constructor invocation issue was repaired with reflection before this red result; no production file was changed for that red run.

After the production patch, the focused D2 command completed with `9/9 PASS`:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryFourFieldRuntimeTest --console=plain
```

The final combined A+B+C+D command (same JDK/SDK setup) completed with `94 tests, 0 failures, 0 errors, 0 skipped`:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryImportV48Test --tests com.ml.tblandroidtxt.PronounProfileTest --tests com.ml.tblandroidtxt.PromptContextQaTest --tests com.ml.tblandroidtxt.LibraryImportPlannerTest --tests com.ml.tblandroidtxt.TranslationConfigStateTest --tests com.ml.tblandroidtxt.CostOptimizationTest --tests com.ml.tblandroidtxt.ModelFlowV42IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV43Test --tests com.ml.tblandroidtxt.ReliabilityV43IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV44Test --tests com.ml.tblandroidtxt.CoreRecovery45Test --tests com.ml.tblandroidtxt.RuntimeStateSnapshotTest --tests com.ml.tblandroidtxt.Hotfix441Test --tests com.ml.tblandroidtxt.Patch312Test --tests com.ml.tblandroidtxt.V46DashboardTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --tests com.ml.tblandroidtxt.Code113GlossaryProjectionDiagnosticFixtureTest --tests com.ml.tblandroidtxt.GlossaryFourFieldRuntimeTest --console=plain
```

Not run by explicit scope: `assembleDebug`, `scripts/build-and-save.ps1`, Android instrumentation, emulator/AVD, real provider/API, RSC, Editorial and IPC tests. No Android/device/API evidence is claimed.

The D3 focused red command was run before the production patch, with the same JDK/SDK setup:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.PronounSevenFieldRuntimeTest --console=plain
```

Result before production change: `13 tests completed, 12 failed`; this was the expected D3 red result for the absent P3 fields, scope/range overloads and paragraph metadata. It was recorded as `FAILED_REPAIRING`/expected red and was not committed. A later focused test-harness assertion failure (`17 tests completed, 1 failed`) was repaired in the same branch; it was caused by the test constructing context as `contextAfter` instead of `contextBefore`, not by a production failure.

After the D3 production patch, the focused command was:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.PronounSevenFieldRuntimeTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --console=plain
```

Result: `17/17 PASS` for `PronounSevenFieldRuntimeTest` and `1/1 PASS` for `Code113P3DiagnosticFixtureTest` (`18 tests, 0 failures, 0 errors, 0 skipped`). The test class covers parser/model, scope, paragraph mapping, matching, compiler, settings snapshot, raw profile persistence, preview/estimator and resume/retry plumbing evidence; no Android/provider execution is implied.

The final D1/D2 baseline plus D3 command was rerun after the production patch and includes the D3 class:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'; $env:JAVA_HOME = $taskJavaHome; $env:Path = "$taskJavaHome\bin;$env:Path"; $env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'; $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME; .\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryImportV48Test --tests com.ml.tblandroidtxt.PronounProfileTest --tests com.ml.tblandroidtxt.PromptContextQaTest --tests com.ml.tblandroidtxt.LibraryImportPlannerTest --tests com.ml.tblandroidtxt.TranslationConfigStateTest --tests com.ml.tblandroidtxt.CostOptimizationTest --tests com.ml.tblandroidtxt.ModelFlowV42IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV43Test --tests com.ml.tblandroidtxt.ReliabilityV43IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV44Test --tests com.ml.tblandroidtxt.CoreRecovery45Test --tests com.ml.tblandroidtxt.RuntimeStateSnapshotTest --tests com.ml.tblandroidtxt.Hotfix441Test --tests com.ml.tblandroidtxt.Patch312Test --tests com.ml.tblandroidtxt.V46DashboardTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --tests com.ml.tblandroidtxt.Code113GlossaryProjectionDiagnosticFixtureTest --tests com.ml.tblandroidtxt.GlossaryFourFieldRuntimeTest --tests com.ml.tblandroidtxt.PronounSevenFieldRuntimeTest --console=plain
```

Final result: `111 tests, 0 failures, 0 errors, 0 skipped` (the pre-D3 D1/D2 set remains `94/94 PASS`; D3 adds 17 tests). `git diff --check` also passes. No APK, device, instrumentation, emulator/AVD, real provider/API, RSC, Editorial or IPC command was run.

## D1 result and D2/D3 boundary

D1 canonical result: `COMPLETE`.

D2 Glossary4 result: `COMPLETE` on `2026-08-29`, implementation commit `353b6410c1c359c5fca0ebccaabfdcbb2e83a037` (`feat(glossary): preserve matched runtime notes`). Runtime now retains exactly `source,target,category,note`; it accepts three/four/five-column Glossary input and ignores `priority`. The D1 Glossary diagnostic is a corrected regression proof. The D2 implementation changed only `GlossaryStore.java` and `PromptContextBuilder.java`.

D3 Pronoun7 result: `COMPLETE` on `2026-08-29`, implementation/tests commit `11e6f54c4d164ad1b7e163b852b264714bbecb75` (`feat(pronoun): support scoped seven-field profiles`). The corrected P3 diagnostic is now a regression proof: the exact header is skipped, all seven fields are retained in the transient rule, valid scope is applied by paragraph overlap, the `from` cue is required, and legacy three-column behavior remains intact. D3 changed no `PronounStore`, `MainActivity`, `TranslationEngine`, database, RSC, Editorial, IPC, provider or build/version source.

Smallest production set identified for D2/D3:

- D2 Glossary4: implemented in `PromptContextBuilder.java` and `GlossaryStore.java`.
- D3 Pronoun7: implemented in `PromptContextBuilder.java`, `Chunk.java`, `Chunker.java` and `PromptPlan.java`. Focused call-path evidence required bounded updates in `PromptBuilder.java` (full preset), `PromptPreviewDialog.java`, `TranslationRepository.java` and `TranslatorService.java`; raw profile/settings persistence remained sufficient, so `PronounStore.java`, `MainActivity.java`, `TranslationEngine.java` and the database schema were not changed.

The canonical phase after this completed D3 handoff is `D4_INTEGRATION`; the exact next action is to write focused D4 Glossary+Pronoun integration tests before changing production. This report does not claim D4 implementation.

## Scope and diff guard

- D1 reconciliation production source diff: `0` files under `app/src/main`.
- D2 production source diff: exactly `2` files under `app/src/main`: `GlossaryStore.java` and `PromptContextBuilder.java`.
- D3 production source diff: exactly `8` files under `app/src/main`: `PromptContextBuilder.java`, `Chunk.java`, `Chunker.java`, `PromptPlan.java`, `PromptBuilder.java`, `PromptPreviewDialog.java`, `TranslationRepository.java` and `TranslatorService.java`; the extra four are justified by focused full-preset/preview/reload/resume/retry range evidence.
- D3 test/report changes are limited to the focused test, corrected existing P3 diagnostic, existing code113 fixtures and the current v4.17 authority documents.
- D3 `git diff --check` passes; after the implementation commit and state/docs commit the canonical worktree is clean and the current uncommitted production-source diff count is `0`.
- No workspace v4.16 file was changed, staged or reset.
