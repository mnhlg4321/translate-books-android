# Translate Books

Translate Books is an Android app for translating long TXT books with OpenAI-compatible language-model APIs. It is designed for resumable, glossary-aware translation rather than one-shot chat: the app prepares stable chunks, injects only relevant terminology and pronoun rules, validates responses, persists progress, and writes partial output safely.

The project targets Android 8.0 and later (`minSdk 26`) and currently builds against Android SDK 35.

## Project status

| Track | Version | Status |
|---|---|---|
| Released baseline | `v4.15` / code 62 | Tagged, regression-tested, and immutably archived |
| Next development | `v4.16-dev.22` / code 84 | Editorial mapping work in progress; QA and release gates open |

See [BUILD_STATE.md](BUILD_STATE.md) for the exact current build, checksums, regression results and known limitations. See [WORKSPACE_SNAPSHOT.md](WORKSPACE_SNAPSHOT.md) for active work and the next development step. For Editorial, start with [EDITORIAL_SAFE4_MIGRATION.md](EDITORIAL_SAFE4_MIGRATION.md); the older V5 plan/handoff are historical only.

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

## Configuration files

### Instruction YAML

The minimal supported format is:

```yaml
translation: |-
  Translate the source accurately and return only the requested output.
```

An optional `refinement` block can be present when the additional refinement pass is enabled. A selected instruction file with neither recognized block is rejected before translation starts.

### Glossary

CSV and compatible text mappings can define names, terminology, aliases, categories and character notes:

```csv
source,target,category
フラム,Flum,character
魂の茨,Gai Hồn,skill
魔王,Ma Vương,title
```

Only matching, global, or otherwise mandatory entries are injected into a chunk prompt, subject to the configured glossary limit.

### Pronoun profile

Relationship rules can be imported separately:

```csv
from,to,pronoun
Flum,Milkit,chị/em
Milkit,Flum,em/chị
```

Equivalent TXT rules such as `Flum → Milkit: chị/em` are also supported. An active pronoun profile takes priority over pronoun notes embedded in glossary rows.

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
| `BUILD_STATE.md` | Current build and released baseline |
| `WORKSPACE_SNAPSHOT.md` | Current branch handoff and next action |
| `CHANGELOG.md` | Version history |
| `GIT_WORKFLOW.md` | Required branch and commit workflow |

## Development workflow

Development follows:

```text
main
  → feature/vX.Y
  → small focused commits
  → regression
  → numbered retained build
  → QA and release evidence
  → annotated tag when releasing
  → merge --no-ff to main
```

Read [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md) and [GIT_WORKFLOW.md](GIT_WORKFLOW.md) before modifying the project. Do not commit directly to `main`, overwrite release tags, or treat `app/build/` as durable artifact storage.

## Version history

Current and historical changes are maintained in [CHANGELOG.md](CHANGELOG.md). The former README contained a long v2.6.x development diary; those old implementation notes are no longer used as the project landing page.
