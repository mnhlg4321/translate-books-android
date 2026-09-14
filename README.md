# Translate Books

> Current P5E audit (2026-09-14): A4.2 evidence is verified, but RAW dispatch and P6 remain not ready. See docs/P5E_AUDIT_20260914.md and docs/P5E_NEXT_WORK_REQUEST.md for the concrete host issues and next work request.


Translate Books is an Android app for translating long TXT books with OpenAI-compatible language-model APIs. It is designed for resumable, glossary-aware translation rather than one-shot chat: the app prepares stable chunks, injects only relevant terminology and pronoun rules, validates responses, persists progress, and writes partial output safely.

The project targets Android 8.0 and later (`minSdk 26`) and currently builds against Android SDK 35.

## Project status

| Track | Version | Status |
|---|---|---|
| Released baseline | `v4.15` / code 62 | Tagged, regression-tested, and immutably archived |
| Frozen later development | `v4.16-dev.104` / code168 | RSC/Relation-Speaker and Editorial activation track preserved as historical; no longer the active next action |
| Recovery source baseline | `v4.16-dev.51` / code113 | Verified chapter-translation baseline at commit `a9409ffa`; not reused as the next build number |
| Verified development baseline | `v4.17-dev.1` / code169 | Glossary4/Pronoun7 recovery and device QA complete at `921af92`; public release gates remain separate |
| Active development | `v4.18` | Integrate `V5-SAFE.4.1.3-FULL` through the existing data-only Editorial Pack v1 platform, with future compatible pack import and no automatic project rebind |

See [EDITORIAL_RECOVERY_V4_18.md](EDITORIAL_RECOVERY_V4_18.md) for the single active scope and fixed implementation order. The pack/runtime boundary is frozen in [docs/EDITORIAL_PACK_V1_4_1_3_INTEGRATION.md](docs/EDITORIAL_PACK_V1_4_1_3_INTEGRATION.md), and file/test ownership is mapped in [docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md](docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md). See [BUILD_STATE.md](BUILD_STATE.md) and [WORKSPACE_SNAPSHOT.md](WORKSPACE_SNAPSHOT.md) for current facts and the exact next action. V4.17 remains the verified product baseline; later v4.16 RSC/Editorial/IPC activation documents are historical reference only.

## Highlights

- Translate one or many TXT files through OpenRouter, OpenAI, DeepSeek, or a custom OpenAI-compatible chat-completions endpoint.
- Split source text deterministically while preserving complete source coverage and protected text regions.
- Resume persisted jobs and retry failed chunks without restarting a completed book.
- Filter glossary and character/pronoun rules for the current chunk instead of sending the entire library on every request.
- Import, activate, rename, replace, and delete reusable glossary and pronoun profiles.
- Load custom YAML translation instructions with separate validation of the `translation` and optional `refinement` blocks.
- Preview the exact prompt plan, including source, instruction, glossary, pronoun and context token estimates.
- Track provider-reported or estimated token usage, cached tokens and cost without storing book text in telemetry rows.
- Reject empty, truncated, repeated, refused, echoed, or structurally broken model responses before accepting output.
- Save partial output after accepted chunks and recover from process or app restarts through durable checkpoints.
- Work with Android Storage Access Framework permissions rather than broad filesystem access.
- Use phone bottom navigation and tablet navigation rail layouts for Translate, Jobs, Library and Settings.

## Translation flow

```text
TXT source
  → deterministic chunks
  → relevant glossary/pronoun locks
  → exact prompt plan and cost estimate
  → provider request
  → response validation and local quality checks
  → durable checkpoint
  → partial/final TXT output
```

Each job keeps an immutable settings snapshot. Changes to the active glossary, pronoun profile, instruction file, model, or chunk settings apply to new jobs rather than silently changing a running job.

The v4.17 recovery targets the existing chapter-by-chapter workflow: the user selects one RAW chapter and its matching chapter Glossary and Pronoun files. Automatic `CHxxx` checking and whole-volume profile dispatch are intentionally deferred.

## Editorial v4.18 direction

V4.18 is planned to add a separate three-pass Editorial workflow based on `V5-SAFE.4.1.3-FULL`. It will not replace or reinterpret the v4.17 Translation profile workflow.

The Editorial platform remains data-driven:

```text
editorial-pack.json + Project + Prompt + Workflow
  → byte/hash and compatibility validation
  → immutable side-by-side storage
  → explicit project/run binding
  → deterministic source preflight
  → L1 REPORT_L1
  → L2 VI_L2 + CHANGE_MAP_L2
  → L3 FINAL_QA + QA_RECEIPT
```

The current CODE169 app already contains the manifest parser, ZIP safety boundary, immutable pack storage and compatibility evaluator, but its trusted engine profile supports only pack integrity and Editorial execution remains disabled. V4.18 first characterizes and reuses that platform, then implements only the 4.1.3 behaviors that tests prove are missing.

Future Editorial versions can be imported without rebuilding the APK when they stay within the installed manifest, three-pass contract, schemas and required capabilities. A pack that requires a new parser, phase, execution protocol or other unknown required capability is retained as incompatible and reports `ENGINE_UPGRADE_REQUIRED`; it is never silently downgraded, auto-activated or applied to an existing run.

## Configuration files

### Instruction YAML

The minimal supported format is:

```yaml
translation: |-
  Translate the source accurately and return only the requested output.
```

An optional `refinement` block can be present when the additional refinement pass is enabled. A selected instruction file with neither recognized block is rejected before translation starts.

### Glossary

CSV and compatible text mappings define names, terminology, categories and optional matched-row notes:

```csv
source,target,category,note,priority
フラム,Flum,character,Tên nhân vật chính,high
魂の茨,Gai Hồn,skill,,medium
魔王,Ma Vương,title,Danh hiệu,high
```

The runtime consumes only `source,target,category,note`. A fifth `priority` column is tolerated but remains user-review metadata: the app does not store, rank, filter or send it to the model. Only matched rows are injected; a non-empty note is included only with its matched row. Legacy three-column and four-column files remain supported.

### Pronoun profile

The v4.17 Pronoun format is:

```csv
from,speaker,target,self,call,scope,note
私,Mercedes,Basil,ta,ngươi,CH004:p052-p153,Quan hệ áp dụng trong phạm vi này
```

The app must preserve and use all seven fields, compile the actual `speaker → target` relation and Vietnamese `self/call`, and constrain rules by `scope` without semantic speaker discovery. The legacy `from,to,pronoun` CSV and equivalent TXT rules such as `Flum → Milkit: chị/em` remain supported through a separate mapping. An active Pronoun profile takes priority over Pronoun notes embedded in glossary rows.

### Environment settings

The app can import an environment-style configuration containing provider, endpoint, model, language, chunk, retry and cost settings. Never commit real API keys. The tracked sample configuration uses placeholders only.

## Build requirements

- Android Studio with Android SDK Platform 35
- JDK 17
- PowerShell
- A connected Android device only when installation or device tests are required

## Mandatory numbered builds

Every development APK must be built through the archive-first script:

```powershell
.\scripts\build-and-save.ps1
```

To archive the build first and then install it on a connected device:

```powershell
.\scripts\build-and-save.ps1 -Install
```

Direct `assembleDebug` and Android Studio **Build APK(s)** are intentionally blocked. The script assigns an increasing version, runs the required checks, preserves the APK, writes build metadata and checksums, and creates an exact tracked-source ZIP before optional installation.

Every successful development build creates matching immutable payloads under:

```text
artifacts/builds/v<version>/<event>/
backup/builds/v<version>/<event>/
```

Generated APKs and durable local archives are intentionally excluded from Git. See [BUILDING.md](BUILDING.md) for the full build and retention contract.

## Verification and reliability

The repository includes JVM and Android instrumentation coverage for:

- deterministic chunk coverage and context handling;
- prompt construction, glossary selection and pronoun matching;
- response parsing, truncation detection and retry classification;
- checkpoint persistence, delivery-unknown protection and output recovery;
- cost estimation, cached-token accounting and benchmark readiness;
- glossary/pronoun import, activation and cold-start persistence;
- navigation, scrolling, runtime preview and dashboard state.

Release and development evidence is recorded in versioned QA reports, [BUILD_STATE.md](BUILD_STATE.md), and [CHANGELOG.md](CHANGELOG.md). A passing historical build is not reused as evidence for a new build.

## Privacy and API behavior

- Source text is sent only to the provider endpoint selected by the user when a translation or paid benchmark request is explicitly started.
- API credentials remain in local app settings and must not be committed to the repository.
- Privacy-safe telemetry stores counts, hashes, status, timing and usage information rather than book or prompt text.
- Provider processing and billing may already have occurred if a network interruption happens after a request body was sent. The app records an unknown-delivery state instead of automatically resending and risking duplicate billing.

## Repository guide

| Path | Purpose |
|---|---|
| `app/src/main/` | Android application code and resources |
| `app/src/test/` | JVM regression tests |
| `app/src/androidTest/` | Device and instrumentation tests |
| `scripts/` | Build, workflow, archive and verification tools |
| `sample_configs/` | Safe example configuration files |
| `release_checklists/` | Evidence-backed release workflow checklists |
| `TRANSLATION_PROFILE_RECOVERY_V4_17.md` | Single active product scope, phase order and acceptance contract |
| `BUILD_STATE.md` | Current build and released baseline |
| `WORKSPACE_SNAPSHOT.md` | Current branch handoff and next action |
| `CHANGELOG.md` | Version history |
| `GIT_WORKFLOW.md` | Required branch and commit workflow |

## Development workflow

Development follows:

```text
approved baseline (`main` normally; exact historical commit only when the canonical plan authorizes recovery)
  → one feature/vX.Y branch for the whole release
  → small focused commits
  → regression
  → numbered retained build
  → QA and release evidence
  → annotated tag when releasing
  → merge --no-ff to main
```

Read [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md) and [GIT_WORKFLOW.md](GIT_WORKFLOW.md) before modifying the project. A later work session resumes the same release branch/checklist instead of creating a new one. Do not commit directly to `main`, overwrite release tags, or treat `app/build/` as durable artifact storage.

## Version history

Current and historical changes are maintained in [CHANGELOG.md](CHANGELOG.md). The former README contained a long v2.6.x development diary; those old implementation notes are no longer used as the project landing page.
