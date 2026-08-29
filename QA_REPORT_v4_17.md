# Translate Books v4.17 QA Report

Status: `D5_LOCAL_BUILD_QA_COMPLETE / V4.17_DEV_ARTIFACT_READY`

## Authority

- Branch: `feature/v4.17-translation-profile-compatibility`
- Source baseline: `a9409ffacfcbb05374e3f07b8ae80b22f60a95b7`
- D0 documentation commit: `a54b82a7c6060b88a9a85f0a59567dd81028b0e2`
- Expected development series: `4.17-dev.N`
- Requested minimum Android versionCode: `169`
- Pre-build commit: `b0ab983bd4f9b017819af2bb0043bc57caad807a`

## Pre-build scope

The D5 artifact will be created only through `scripts/build-and-save.ps1`. The
recovery implementation covers Glossary4, Pronoun7, matched-only compact prompt
locks, scoped refinement, settings snapshot/resume/retry and hermetic Mercedes
CH001/CH004 prompt/estimator evidence from D4.

The following are intentionally not run or activated in this local phase:

- device/manual QA, Android instrumentation, emulator/AVD or benchmark;
- real provider/API requests;
- RSC, Editorial or IPC workflows;
- public release/tag archive or release-complete gate.

## Local-build result

The numbered development build completed through `scripts/build-and-save.ps1`.
This is a local development artifact only; it is not a public release or tag.

## Pre-build regression evidence

Environment:

- JDK: `C:\Program Files\Android\Android Studio\jbr` (JDK 21)
- `ANDROID_HOME` / `ANDROID_SDK_ROOT`:
  `C:\Users\ADMIN\AppData\Local\Android\Sdk`

Focused integration command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.ml.tblandroidtxt.TranslationProfileIntegrationTest --rerun-tasks --console=plain
```

Result: `11 tests, 0 failures, 0 errors, 0 skipped` (PASS).

Full JVM command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain
```

Result: `205 tests, 0 failures, 0 errors, 0 skipped` (PASS).

Test XML: `app/build/test-results/testDebugUnitTest/` (40 XML files; the focused
class report is `TEST-com.ml.tblandroidtxt.TranslationProfileIntegrationTest.xml`).

Pre-build script checks: PowerShell AST parse PASS; invalid
`-MinimumVersionCode 0` rejection PASS; `git diff --check` PASS; application-source
diff count `0`.

## Build command and metadata

Environment was set in the same PowerShell shell:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_HOME = $taskJavaHome
$env:Path = "$taskJavaHome\bin;$env:Path"
$env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

Exact build command:

```powershell
.\scripts\build-and-save.ps1 -Series 4.17-dev -MinimumVersionCode 169 -JavaHome 'C:\Program Files\Android\Android Studio\jbr' -Notes 'v4.17 translation-profile recovery: Glossary4, Pronoun7, scoped refinement and Mercedes integration.'
```

Result: PASS. Gradle reported `BUILD SUCCESSFUL in 21s`; shell elapsed time was
approximately 24.6 seconds. The script ran `clean`, `testDebugUnitTest`,
`lintDebug` and `assembleDebug`, then archived before any optional installation.

| Field | Value |
|---|---|
| versionName | `4.17-dev.1` |
| versionCode | `169` |
| package | `com.ml.tblandroidtxt` |
| build event | `build-20260829-185818` |
| build commit | `b0ab983bd4f9b017819af2bb0043bc57caad807a` |
| branch | `feature/v4.17-translation-profile-compatibility` |
| requested minimum | `169` |
| default versionCode | `48` |
| highest archived code observed | `0` |
| installed code observed | `0` (no connected device) |
| highest observed code | `48` |
| selected code | `169` |
| Java/JDK | JBR `21.0.10` |
| Gradle / AGP | `9.3.0` / `8.7.3` |
| compileSdk / targetSdk | `35` / `35` |

`BUILD_INFO.json` and `README.md` are in both payloads and record the requested
minimum, selected version, highest archived code and installed code observed.

## Artifact and backup evidence

Artifact:

`artifacts/builds/v4.17-dev.1/build-20260829-185818/`

Backup:

`backup/builds/v4.17-dev.1/build-20260829-185818/`

| Payload | Size | SHA-256 |
|---|---:|---|
| `TranslateBooks-v4.17-dev.1-code169.apk` | 2,784,446 bytes | `3C3AAEF1A7D47F39A7B5A5FF8AEDF77347D255AF728B908180AA142CA2B276D1` |
| `project_source_build-20260829-185818.zip` | 80,542,911 bytes | `AA3A75CF4CFC4C527352F037D3AABEC9BBF24517138F58DB974794CEB67EAA6F` |

Both payloads contain exactly five files: the APK, `README.md`, `BUILD_INFO.json`,
`SHA256SUMS.txt` and the exact tracked-source ZIP. Recursive relative-path and
SHA-256 comparison passed for all `5/5` files. `SHA256SUMS.txt` verification passed
for all four listed files. The source ZIP `sourceSnapshotRef` is the exact build
commit above. No existing event was overwritten or reused.

## APK manifest and embedded build metadata

The SDK `apkanalyzer` output was:

```text
application-id: com.ml.tblandroidtxt
version-name: 4.17-dev.1
version-code: 169
min-sdk: 26
target-sdk: 35
```

`aapt2 dump badging` independently reported package
`com.ml.tblandroidtxt`, versionCode `169`, versionName `4.17-dev.1`,
`minSdkVersion:'26'`, `targetSdkVersion:'35'` and compileSdk `35`.

`apkanalyzer dex code --class com.ml.tblandroidtxt.BuildConfig` confirmed the APK
contains:

```text
BUILD_EVENT_ID = "build-20260829-185818"
BUILD_GIT_COMMIT = "b0ab983bd4f9b017819af2bb0043bc57caad807a"
BUILD_TIMESTAMP = "2026-08-29 18:58:18 +07:00"
VERSION_NAME = "4.17-dev.1"
VERSION_CODE = 169
```

## Mercedes fixture and prompt QA

The hermetic D4 fixtures remain byte-stable and are not read from `D:` during
tests.

| Chapter | RAW SHA-256 | Glossary SHA-256 | Pronoun SHA-256 | Parsed counts |
|---|---|---|---|---|
| CH001 | `FE4CE02301E9EE5FD457A7A744C982B23EE3C95100CFDC5CBDF908B3979F40AF` | `459FBF1037BC52FA756843BFF7364759D0460388A38F51D16662D8751ACC5E8C` | `1F71CDCBC8760022E171FB8303E58A312D1913EE157CF783350E62100D9829FB` | 53 paragraphs / 15 Glossary / 1 P3 |
| CH004 | `36FB90029B19360590344A6147E8EE53131BBAAF336196D7E7DC1D35B4BA3A4A` | `5583CFCBDE99E3F3AC32191F9306D5377BD37A008D5EC7B8F47741B04E6DA332` | `B4A6A41CC31ABCDD48C246031368C68F31B5CF2517F9E23B7132B642C64F4648` | 154 paragraphs / 36 Glossary / 6 P3 |

Headers were skipped, no malformed Glossary/P3 rows were observed, Japanese and
Vietnamese text survived parsing, and Glossary `priority` was tolerated but did
not enter the runtime `Term`.

For CH001, the chunk containing `p050` / `行くぞ` was chunk index `3`, range
`p41-p53`. It selected eight matched Glossary rows and one P3 row in the same
translation plan. The exact P3 semantic lock was:

```text
Mercedes → @GROUP_1: omit pronouns | Mệnh lệnh cho Benkei/Kuro; giọng ngắn, dứt khoát, ưu tiên lược đại từ.
```

The empty P3 `self`/`call` values compiled to `omit pronouns`; no neutral fallback
was invented. The lock occurred once.

For CH004, the chunk containing `p052` / `私` was chunk index `3`, range `p49-p67`.
It selected three matched Glossary rows and three P3 rows, including exactly once:

```text
Mercedes → Basil: ta/ngươi | Giọng lạnh, áp đảo; chất vấn trực diện dùng ta–ngươi.
```

The rule used `from=私` only as a cue; it did not render `私 → Mercedes: Basil`.
The `p052-p153` scope overlapped the chunk and did not leak outside its range;
malformed scopes failed closed and `*` still required its cue.

Preview, balanced/full translation plans, and estimator selected the same matched
rows and emitted each lock once. Lock blocks contained no CSV header/raw row,
`priority`, scope/chapter metadata, source path, filename, parser warning, full
profile or unmatched note. Adding unmatched rows did not change matched counts or
token estimates. D4 also proved note injection is matched-only and occurs once.

## Settings, resume and refinement

`SettingsStore.toJson/fromJson` preserved both raw profile texts, optimization
preset, `refineAfter`, Glossary/Pronoun limits, settings hash, lock counts and cost
breakdown. Reconstructed chunks preserved index, offsets, context, paragraph ranges,
prompt locks and lock counts without a database change.

For CH004 chunk `3`, the range-aware runtime path retained the scoped P3 in both
translation and refinement: `128` Pronoun tokens + `128` Pronoun tokens = `256`
Pronoun tokens in the exact `refineAfter=true` estimate. Translation, refinement
retry and resume use the real `Chunk` range; the legacy String overload does not
invent a range.

## Cost evidence

The D4 exact estimator comparison used the same production chunks with
`refineAfter=false`; input high equaled input low for the selected model estimate.
`Matched rows` is the total number of injected locks across the chapter.

| Chapter | Profile | Chunks | Glossary rows | P3 rows | Glossary tokens | P3 tokens | Input low | Input high |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| CH001 | no profile | 4 | 0 | 0 | 0 | 0 | 4143 | 4143 |
| CH001 | no note | 4 | 20 | 1 | 306 | 13 | 4486 | 4486 |
| CH001 | full note + priority input | 4 | 20 | 1 | 780 | 49 | 4996 | 4996 |
| CH004 | no profile | 10 | 0 | 0 | 0 | 0 | 10811 | 10811 |
| CH004 | no note | 10 | 74 | 11 | 1030 | 165 | 12085 | 12085 |
| CH004 | full note + priority input | 10 | 74 | 11 | 2541 | 478 | 13906 | 13906 |

Full-note input deltas were `+853` over no profile and `+510` over no note for
CH001, and `+3095` and `+1821` respectively for CH004. Every increase was a
matched semantic lock or matched note. No metadata, raw CSV, unmatched note,
`from`, scope, header or priority overhead was counted; output/source estimates did
not change from profile metadata.

Whole-chapter dry preparation covered CH001 `53/53` paragraphs in `4` chunks and
CH004 `154/154` paragraphs in `10` chunks. Every chunk produced plans and an exact
estimate without a provider request.

## Lint and unit-test evidence

The build script's integrated Gradle run passed:

- `205 tests, 0 failures, 0 errors, 0 skipped`.
- Android Lint: `0 errors, 53 warnings`; report:
  `app/build/reports/lint-results-debug.xml` and `app/build/reports/lint-results-debug.html`.
- `assembleDebug`: PASS as an internal step of `scripts/build-and-save.ps1` only.

The pre-build focused command passed `11/11`, and the preserved D3 baseline remains
`111/111` with 0 failures/errors/skips. Full JVM XML reports are at
`C:\Users\ADMIN\Documents\App Translate Books-translation-profile\app\build\test-results\testDebugUnitTest`;
the HTML report is at
`C:\Users\ADMIN\Documents\App Translate Books-translation-profile\app\build\reports\tests\testDebugUnitTest\index.html`.

## APK content guard

The APK entry-name guard found no test fixture, Mercedes RAW, `.env`, session,
RSC or IPC artifact entry. Content scanning found no `D:\` path or credential-like
API-key value and no local `.env` file. The baseline DEX retains the sample-template
literal `tbl_android.env` with the placeholder `OPENROUTER_API_KEY=sk-or-v1-...`;
this is not a credential and is recorded rather than misreported as absent.
The standard `AndroidManifest.xml` and `resources.arsc` entries are normal APK
metadata, not workflow artifacts.

The baseline APK still contains the historical Editorial assets:

- `assets/editorial/engine-profile/v1/profile.json`
- `assets/editorial/v5-safe4/PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4.txt`
- `assets/editorial/v5-safe4/PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4.txt`
- `assets/editorial/v5-safe4/WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4.txt`

Compiled baseline classes also contain historical identifier strings such as
`RSC`, `IPC`, `OPENROUTER_API_KEY`, and `sessionId`; this is recorded rather than
misreported as absence of historical code. The D5 source diff contains no
application source change and adds no activation of those tracks.

## Scope and non-actions

Production-source diff after the build is `0`; the pre-build commit changed only
the build script and release/state documentation. No Glossary, Pronoun, MainActivity,
TranslationEngine, database, RSC, Editorial or IPC production change was made in
D5.

`adb devices` returned only `List of devices attached`.

`DEVICE_QA: NOT RUN — no connected device`.

No APK was installed. No Android instrumentation, emulator/AVD, real provider/API,
RSC, Editorial or IPC test/workflow was run. No public tag, public release archive,
benchmark, Perfetto, screenshot or video evidence is claimed.

Known limitations remain: multiline quoted CSV fields are unsupported by the
existing line-based parser; malformed P3 scopes fail closed; historical Editorial
assets remain packaged by the baseline; physical-device/manual QA is pending.
