# P5 — Controlled L1 pilot dry-run validation

## Decision

P5 live execution was not authorized and was not attempted.  The required
`P5 PILOT AUTHORIZATION` block was not supplied, so this phase is recorded as
dry-run/fake-provider engineering evidence only.

```text
P5_DRY_RUN_ONLY
LIVE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_RUNNABLE
```

This report does not claim `P5_L1_PILOT_COMPLETE`, `L1_EXECUTION_VERIFIED`,
`REPORT_L1_VALIDATED`, or `L1_RECEIPT_VALIDATED` for a real chapter.

## Entry gate

The requested P5 starting commit was `8d676c336d8011ab1534d1d5528cb52171c11bbf`.
The P5 entry review found a concrete P4 producer defect that the request
required fixing before production work: new P4 setup wrote `NORMAL` while the
contract requires `NORMAL_FOUR_SOURCE` (and `ALTERNATE_EXPLICIT`).  The defect
was reproduced by a red device assertion, fixed and committed in:

```text
364faa42e7ed6bb08b75dda7fdc7335b8f931df7  fix(editorial): align P4 source mode vocabulary
16073c6285c5b31b13f25929ad7a774b5044d009  docs(editorial): record P4 source mode correction
```

The actual clean P5 implementation baseline after that mandatory correction is
`16073c6285c5b31b13f25929ad7a774b5044d009`; subsequent P5 commits are listed
below.  Existing legacy rows were not rewritten or silently rebound.

Entry identity checks:

| Item | Evidence |
|---|---|
| Branch | `feature/v4.18` |
| Workspace | `D:\App Translate Books\App Translate Books-translation-profile` |
| Canonical ZIP | `23,638` bytes; `B9C65DBEB9D4C4ED46B67D5EC28FF6252CC2BDC4B63BC902904612987EC58987` |
| Java control ZIP | `23,418` bytes; `44F99423292ADA15680220165AF50430532D847E155F93C1B15D9F173D4609A5` |
| Profile v2 resource | `1B2DB011D59F3E2EF4349AEB0DAA9C54A19B7EFD1E2CA6886BC29B56E4690D62` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Machine contract fingerprint | `a167e08d8400094ef06e219495d1e34f28081fed2473a62b88c8f9fc5fa9d2a3` |
| Authority hashes | project `1727AE...A26AD`; prompt `D25757...CD754F`; workflow `5DB6B4...05730` |
| Provider/API calls | `0` |

The three authority hashes above are the full SHA-256 values already frozen in
`BUILD_STATE.md` and the P2 receipt; no authority byte was read into Git or the
public report.

## Implementation boundary

The dry-run boundary is deliberately injected and provider-agnostic:

- `EditorialP5PilotAuthorization` represents one exact binding/chapter/provider
  authorization, with bounded calls, tokens, cost, time, expiry and consent.
- `EditorialP5PilotRequest` carries the persisted P4 binding, manifest,
  authority bytes, source bytes and app-owned anchors/population.
- `EditorialP5PilotExecution` performs identity checks, P3B source preflight,
  full-bundle validation, phase projection, bounded call policy, local output
  validation and atomic attempt-store handoff.
- `EditorialP5PilotProvider` is only an injected interface.  No production
  HTTP/provider adapter was added or invoked.
- `EditorialP5L1Output` and `EditorialP5PilotResult` keep report/receipt
  disposition separate from chapter state and ignore model-declared PASS for
  local decision making.

The implementation is engine-local.  No database migration, UI, project
rebind, global active pack, provider wiring, execution activation, or
certification state was added.

## Fake-provider contract evidence

`EditorialP5PilotExecutionBoundaryTest` now has `15/15 PASS`.

| Boundary | Evidence |
|---|---|
| Missing authorization, wrong binding and non-L1 phase | Typed fail-closed result; provider call count `0` |
| Source drift, missing RAW and unreadable handle | `STALE_CHAIN` or typed preflight `INPUT_REQUIRED`/`RETRY_REQUIRED`; provider call count `0` |
| Raw-discovery projection | Provider sees only `RAW` and `GLOSSARY`; hidden `DRAFT`, `PRONOUN` and optional Pair Context are absent |
| Reconcile projection | Provider sees only phase-allowed sources; Pair Context remains optional |
| Evidence insufficiency | `PRESERVE_DRAFT` without provider call or partial report |
| Truncated response | `RETRY_REQUIRED / RETRY_OUTPUT_TRUNCATED`; no committed result |
| Schema-only repair | At most one repair call, repair request has no visible source bytes, semantic fingerprint must remain unchanged |
| Model self-certification | Local diff validator rejects a fake declared change even when model declares PASS |
| Token budget | Over-budget response returns `BUDGET_EXCEEDED`; no committed result |
| Expiry/consent | Both fail before provider access |
| Concurrent writer | `RETRY_PROVIDER_CALL_STATE_UNKNOWN`; no second provider call |
| Idempotent retry | Exact committed attempt returns `ALREADY_COMMITTED`; no second provider call |
| Atomic persistence failure | Typed retry; no committed result remains |

The fake success path locally creates redacted `REPORT_L1`/receipt bytes only
after identity, ledger, actual diff, gate and receipt validation pass.  It does
not store full request/response bodies or source text.  `PRESERVE_DRAFT` is
never marked canon or propagation-eligible.

## Required commands and results

All Gradle commands used JDK 21 from Android Studio JBR.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :editorial-engine:test --no-daemon --tests com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecutionBoundaryTest
# 15/15 PASS

.\gradlew.bat :editorial-engine:test --no-daemon
# 178/178 PASS (baseline 163; +15 P5 boundary tests)

$env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest --no-daemon
# 210/210 PASS

& 'D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE\TESTS\test_full_release.ps1'
# PASS=306 FAIL=0 OLD_WORDS=5308 NEW_WORDS=7050 EXACT_RETAINED=223/311
```

Device regression was run on allowed device `15e84958` (`CPH2691`, API 35).
The required archival build script created and retained validation APK
`4.17-dev.9` / code177:

```powershell
.\scripts\build-and-save.ps1 -Series '4.17-dev' -MinimumVersionCode 175 `
  -JavaHome 'C:\Program Files\Android\Android Studio\jbr' `
  -Notes 'P5 dry-run boundary regression validation' -Install
```

| Artifact | Evidence |
|---|---|
| APK | `artifacts/builds/v4.17-dev.9/build-20260904-193721/TranslateBooks-v4.17-dev.9-code177.apk` |
| Backup | matching `backup/builds/v4.17-dev.9/build-20260904-193721` payload |
| APK SHA-256 | `8B4D4714287114013A43C181C232921ABC6D6BA43CFB34F5E87D6CABF34001FA` |
| Source commit in BUILD_INFO | `d0de39cf2ea22117303f299b522048a371372679` |
| Device instrumentation | `112/112` runner result, `0` failures, one approved real-API assumption skip |
| Provider/API | `0` |
| Device after test | restored to `4.17-dev.1` / code169 by uninstall/reinstall |

The first script invocation lacked SDK environment variables and stopped before
build/archive.  It created no artifact; the second invocation supplied the
confirmed SDK and completed successfully.  The device rejected `pm clear`, so
uninstall/reinstall was used for restoration and package validation data was
intentionally lost.  The original workspace was not reset or modified.

## Gaps and handoff

P5 live work remains blocked by missing user authorization, not by an inferred
provider choice.  The following are intentionally not claimed:

- no real project/binding selector or chapter was authorized;
- no RAW/DRAFT/GLOSSARY/PRONOUN paths were supplied;
- no provider, model, endpoint/account configuration or data-egress consent
  was supplied;
- no live token/cost budget was approved;
- no real `REPORT_L1` or receipt was committed;
- no app database attempt store, cancellation/process-death live recovery or
  production provider adapter was exercised.

The next action is to obtain the complete authorization block.  Until then,
keep `EXECUTION_DISABLED / NOT_CERTIFIED / NOT_RUNNABLE`.  After authorization,
run fake-provider end-to-end with the selected persisted P4 binding first,
then request a separate live-call approval if all exact fields and budgets are
valid.  Do not open L2/L3, certify globally, auto-activate or auto-rebind.

## Final checks

- `git diff --check`: PASS before documentation update.
- Canonical ZIP, control ZIP, profile and authority hashes: unchanged.
- No secret, API key, full chapter, full request or full response entered Git.
- No provider/API call was made.
- No production app/database/UI/build metadata change was made by P5.
