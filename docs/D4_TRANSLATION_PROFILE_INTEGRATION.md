# D4 Translation Profile Integration Evidence

## Authority

- Worktree: `C:\Users\ADMIN\Documents\App Translate Books-translation-profile`
- Branch: `feature/v4.17-translation-profile-compatibility`
- D4 starting HEAD: `3994ae964aa2787b798c9a5896e95a67d3ea7fb4`
- D4 implementation commit: `11c726f` (`fix(prompt): integrate scoped locks through refinement`)
- Canonical source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7`
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`
- D3 locked baseline before D4: `111 tests, 0 failures, 0 errors, 0 skipped`
- No APK, provider/API request, device/emulator, RSC, Editorial or IPC workflow was used.

The implementation commit contains only the five justified prompt/refinement production changes, the focused integration test, six hermetic fixture files, and the freshness-required snapshot update. `GlossaryStore.java`, `PronounStore.java`, `MainActivity.java`, database schema and activation code are unchanged.

## Hermetic Mercedes fixtures

The six files below were copied byte-for-byte from the named read-only source paths. Tests load only the classpath copies.

| Chapter | Fixture | Repository path | Source path | SHA-256 |
|---|---|---|---|---|
| CH001 | RAW | `app/src/test/resources/fixtures/v417/d4/mercedes-vol4/ch001/raw.txt` | `D:\Ebooks\MERCEDES\VOL 4\3. RAW\001_RAW_1_4.txt` | `FE4CE02301E9EE5FD457A7A744C982B23EE3C95100CFDC5CBDF908B3979F40AF` |
| CH001 | Glossary | `app/src/test/resources/fixtures/v417/d4/mercedes-vol4/ch001/glossary.csv` | `D:\Ebooks\MERCEDES\VOL 4\1. GLOSSARY\VOL4_GLOSSARY_FINAL_RELEASE_MERCEDES_R1\chapter_projections\001_CHAPTER_GLOSSARY_FINAL_MERCEDES_VOL4.csv` | `459FBF1037BC52FA756843BFF7364759D0460388A38F51D16662D8751ACC5E8C` |
| CH001 | Pronoun | `app/src/test/resources/fixtures/v417/d4/mercedes-vol4/ch001/pronoun.csv` | `D:\Ebooks\MERCEDES\VOL 4\2. PRONOUN\PRONOUN_MERCEDES_VOL4_V2_P3_RELEASE\CH001_PRONOUN.csv` | `1F71CDCBC8760022E171FB8303E58A312D1913EE157CF783350E62100D9829FB` |
| CH004 | RAW | `app/src/test/resources/fixtures/v417/d4/mercedes-vol4/ch004/raw.txt` | `D:\Ebooks\MERCEDES\VOL 4\3. RAW\004_RAW_1_4.txt` | `36FB90029B19360590344A6147E8EE53131BBAAF336196D7E7DC1D35B4BA3A4A` |
| CH004 | Glossary | `app/src/test/resources/fixtures/v417/d4/mercedes-vol4/ch004/glossary.csv` | `D:\Ebooks\MERCEDES\VOL 4\1. GLOSSARY\VOL4_GLOSSARY_FINAL_RELEASE_MERCEDES_R1\chapter_projections\004_CHAPTER_GLOSSARY_FINAL_MERCEDES_VOL4.csv` | `5583CFCBDE99E3F3AC32191F9306D5377BD37A008D5EC7B8F47741B04E6DA332` |
| CH004 | Pronoun | `app/src/test/resources/fixtures/v417/d4/mercedes-vol4/ch004/pronoun.csv` | `D:\Ebooks\MERCEDES\VOL 4\2. PRONOUN\PRONOUN_MERCEDES_VOL4_V2_P3_RELEASE\CH004_PRONOUN.csv` | `B4A6A41CC31ABCDD48C246031368C68F31B5CF2517F9E23B7132B642C64F4648` |

The two CSV files per chapter retain the UTF-8 BOM (`EF BB BF`); RAW files have the source bytes unchanged. Parsing produced:

| Chapter | Paragraphs | Glossary rows | P3 rows | Malformed Glossary/P3 rows |
|---|---:|---:|---:|---:|
| CH001 | 53 | 15 | 1 | 0 / 0 |
| CH004 | 154 | 36 | 6 | 0 / 0 |

Headers were skipped, not projected as data. `priority` was tolerated in Glossary input and is absent from the runtime `Term`; Japanese and Vietnamese text survived parsing.

## Selected integrated locks

`TranslationProfileIntegrationTest` found the real chunks from the classpath RAW files with the production `Chunker`.

CH001 `行くぞ` is in chunk index `3` (zero-based), paragraph range `p41-p53`. The plan selected eight Glossary rows and one P3 row in the same translation prompt. The selected Glossary lines were:

```text
クロ => Kuro [character] | Tên Mercedes đặt cho Schwarz Wolf Fang đi theo.
ジークリンデ王女 => Công chúa Sieglinde [title] | Danh xưng thường dùng của Sieglinde.
ダンジョン => dungeon [world_concept] | Không gian mê cung do Dungeon Master sở hữu và quản lý.
ベンケイ => Benkei [character] | Tên Mercedes đặt cho Ashura Ogre đi theo.
メルセデス => Mercedes [character] | Nhân vật chính; tên ngắn.
天然のダンジョン => dungeon tự nhiên [world_concept] | Dungeon hình thành tự nhiên, đối lập loại nhân tạo.
準ダンジョン => dungeon thứ cấp [world_concept] | Loại cấu trúc gần với dungeon nhưng không hoàn toàn tương đương dungeon chuẩn.
魔物 => ma vật [monster] | Tên chung cho ma vật trong thế giới.
```

The exact P3 lock occurred once:

```text
Mercedes → @GROUP_1: omit pronouns | Mệnh lệnh cho Benkei/Kuro; giọng ngắn, dứt khoát, ưu tiên lược đại từ.
```

The source cue `行くぞ` was used only for matching. Empty `self`/`call` compiled to `omit pronouns`; no neutral self/call was invented.

CH004 `私` is in chunk index `3`, paragraph range `p49-p67`. The translation plan selected the scoped `p052-p153` rule and two other applicable chapter-wide/context rules. The exact selected scoped lock occurred once:

```text
Mercedes → Basil: ta/ngươi | Giọng lạnh, áp đảo; chất vấn trực diện dùng ta–ngươi.
```

The selected Glossary lines were:

```text
メルセデス => Mercedes [character] | Nhân vật chính; tên ngắn.
メルセデス・グリューネヴァルト => Mercedes Grünewald [character] | Tên đầy đủ theo gia tộc cha.
真の王 => Vua Chân Chính [title] | Danh xưng chính trị chỉ người được xem là vị vua đích thực.
```

`from=私` remained a cue only; it did not become `私 → Mercedes: Basil`. The scope matcher accepted inclusive overlap at p052/p153, rejected p051/p154, rejected malformed reversed scopes, and required the cue for `*` rules.

## Prompt, preview and metadata parity

The balanced translation plan, full translation plan, full `PromptBuilder` output and `PromptContextBuilder.preview` used the same range-aware match result. Each selected lock line occurred once in the system prompt. The lock blocks contain semantic compact lines only:

- no CSV header or raw CSV row;
- no `priority`, source path, filename or parser warning;
- no `scope`, `CH004:` or P3 `from` metadata field;
- no unmatched note or unmatched row;
- no full profile dump.

Adding 250 unmatched Glossary and 250 unmatched P3 rows left matched counts, Glossary/Pronoun tokens and total input tokens unchanged. Matching remained cue-based; a note-only chunk did not inject its row. Dedupe remained the existing lower-case `source + NUL + target` key for Glossary. Limits remained Glossary `80` and Pronoun `40`.

`priority` was not used for ranking, matching, dedupe or prompt output. A profile containing the same note with the fifth column removed produced identical locks and token counts.

## Snapshot, restart and retry preparation

`SettingsStore.toJson/fromJson` preserved both raw profile texts (including BOM), `optimizationPreset`, `refineAfter`, Glossary/Pronoun limits, and the `HashUtil.settingsHash`. Prompt lock strings/counts and exact estimator breakdown were identical before and after the settings round trip.

Restart simulation rebuilt each `Chunk` from persisted index, offsets, main text and context, then ran `Chunker.assignParagraphRanges`. Paragraph ranges, stable IDs, prompt locks and lock counts were identical before and after reconstruction. No database column was introduced.

## Scoped refinement fix

The focused test first observed the known D4 baseline defect: translation plans received the real `Chunk` range, while the string-only refinement path did not; therefore a scoped P3 present in translation was absent from refinement and from the exact `refineAfter=true` estimate. This was recorded as `FAILED_REPAIRING` and no failing state was committed.

The minimal repair in `11c726f`:

- added `PromptPlan.refinement(Chunk, String, AppSettings)` and `forRefinement(Chunk, String, AppSettings)`;
- added the full-preset `PromptBuilder.refinementPrompt(Chunk, String, AppSettings)` overload;
- changed `CostEstimator` refine-after estimation to use the same `Chunk`;
- added range-aware `TranslationEngine.refineWithRetry` overloads while retaining String compatibility overloads;
- changed both production `TranslatorService` translation/retry refine call sites to pass the real `Chunk`.

The legacy String overload does not invent a paragraph range. The runtime Chunk overload keeps the real chunk through attempts, metrics and retry. For CH004 chunk `3` (`p49-p67`), the selected scoped P3 had `128` Pronoun tokens in translation, `128` in refinement, and `256` in the exact `refineAfter=true` estimator.

## Cost evidence

These rows use the same production chunks with `refineAfter=false` so the profile delta is isolated. Input high equals input low for the selected catalog model approximation. `matched rows` is the total number of locks injected across chapter requests.

| Chapter | Profile | Chunks | Matched Glossary | Matched P3 | Glossary tokens | P3 tokens | Input low | Input high |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| CH001 | no profile | 4 | 0 | 0 | 0 | 0 | 4143 | 4143 |
| CH001 | no note | 4 | 20 | 1 | 306 | 13 | 4486 | 4486 |
| CH001 | full note + priority input | 4 | 20 | 1 | 780 | 49 | 4996 | 4996 |
| CH004 | no profile | 10 | 0 | 0 | 0 | 0 | 10811 | 10811 |
| CH004 | no note | 10 | 74 | 11 | 1030 | 165 | 12085 | 12085 |
| CH004 | full note + priority input | 10 | 74 | 11 | 2541 | 478 | 13906 | 13906 |

Full-note input deltas were `+853` over no profile and `+510` over no note for CH001; `+3095` and `+1821` respectively for CH004. The tests also proved that output/source estimates do not change with profile metadata, priority does not change cost, and unmatched rows do not change cost. Every added token belongs to a matched semantic lock or its matched note; no header/scope/from/priority/raw-CSV overhead was injected and no note was duplicated.

## Whole-chapter dry preparation

The hermetic CH001 preparation covered all 53 paragraphs in source order with four chunks; CH004 covered all 154 paragraphs with ten chunks. Every chunk produced balanced/full translation plans, balanced/full refinement plans, and an exact estimate without an external request. The observed refine-after input estimates were CH001 `11166` low / `11166` high and CH004 `30149` low / `30149` high.

## Test evidence

Environment set in the same PowerShell shell for every Gradle command:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_HOME = $taskJavaHome
$env:Path = "$taskJavaHome\bin;$env:Path"
$env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

Test-first repair record:

- `.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.TranslationProfileIntegrationTest --rerun-tasks --console=plain` first stopped at test compilation because the new test generator had two missing `append` parentheses: `FAILED_REPAIRING`, no production change.
- The corrected pre-production red command above completed `11 tests, 5 failures, 0 errors, 0 skipped`: four failures were the expected missing range-aware refinement overload; one was a test SHA constant typo. Both were repaired in the same phase; no failing state was committed.

Final focused D4 command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.TranslationProfileIntegrationTest --rerun-tasks --console=plain
```

Result: `11 tests, 0 failures, 0 errors, 0 skipped`.

D2/D3/diagnostics plus D4 command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryFourFieldRuntimeTest --tests com.ml.tblandroidtxt.PronounSevenFieldRuntimeTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --tests com.ml.tblandroidtxt.Code113GlossaryProjectionDiagnosticFixtureTest --tests com.ml.tblandroidtxt.TranslationProfileIntegrationTest --rerun-tasks --console=plain
```

Result: `39 tests, 0 failures, 0 errors, 0 skipped`.

Preserved D3 111-test command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.GlossaryImportV48Test --tests com.ml.tblandroidtxt.PronounProfileTest --tests com.ml.tblandroidtxt.PromptContextQaTest --tests com.ml.tblandroidtxt.LibraryImportPlannerTest --tests com.ml.tblandroidtxt.TranslationConfigStateTest --tests com.ml.tblandroidtxt.CostOptimizationTest --tests com.ml.tblandroidtxt.ModelFlowV42IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV43Test --tests com.ml.tblandroidtxt.ReliabilityV43IntegrationTest --tests com.ml.tblandroidtxt.ReliabilityV44Test --tests com.ml.tblandroidtxt.CoreRecovery45Test --tests com.ml.tblandroidtxt.RuntimeStateSnapshotTest --tests com.ml.tblandroidtxt.Hotfix441Test --tests com.ml.tblandroidtxt.Patch312Test --tests com.ml.tblandroidtxt.V46DashboardTest --tests com.ml.tblandroidtxt.Code113P3DiagnosticFixtureTest --tests com.ml.tblandroidtxt.Code113GlossaryProjectionDiagnosticFixtureTest --tests com.ml.tblandroidtxt.GlossaryFourFieldRuntimeTest --tests com.ml.tblandroidtxt.PronounSevenFieldRuntimeTest --rerun-tasks --console=plain
```

Result: `111 tests, 0 failures, 0 errors, 0 skipped`.

Final full JVM command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain
```

Result: `205 tests, 0 failures, 0 errors, 0 skipped`.

XML reports: `C:\Users\ADMIN\Documents\App Translate Books-translation-profile\app\build\test-results\testDebugUnitTest`.

HTML report: `C:\Users\ADMIN\Documents\App Translate Books-translation-profile\app\build\reports\tests\testDebugUnitTest\index.html`.

## Scope gate

- Production files changed in the D4 implementation commit: `PromptPlan.java`, `PromptBuilder.java`, `CostEstimator.java`, `TranslationEngine.java`, `TranslatorService.java`.
- No production change in `GlossaryStore.java`, `PronounStore.java`, `MainActivity.java`, `Chunker` persistence schema, RSC, Editorial, IPC, database or build/version files.
- `git diff --check`: PASS (fixture CRLF was retained byte-for-byte; local `core.whitespace=cr-at-eol` makes CRLF an end-of-line convention).
- The existing Gradle test lifecycle ran its normal source/resource verification tasks; no targeted RSC, Editorial, IPC, instrumentation, emulator, device, API or provider test was run.
- No `assembleDebug`, `scripts/build-and-save.ps1`, APK build, tag or push was run.

## D4 result

`D4_INTEGRATION_COMPLETE / D5_RELEASE_READY`. The next phase is D5 release regression and QA; only then may the project build exactly once through `scripts/build-and-save.ps1` with Android `versionCode > 168`.
