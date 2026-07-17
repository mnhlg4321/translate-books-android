# TBLAndroidTxt v2.7.5 Job Manager - QA Report

## Scope

Base: `v2.7.0-core-split-ui-safe`.

Goal: finish the next planned step after core split: make the Jobs/Checkpoint screen actionable per job, without redesigning the translation UI or changing the prompt/output logic.

## Build metadata

| Item | Value |
|---|---|
| versionCode | `24` |
| versionName | `2.7.5-job-manager` |
| Main focus | Job Manager / checkpoint control |
| UI strategy | Keep stable tab shell; no DrawerLayout comeback |

## Files changed

| File | Change |
|---|---|
| `app/build.gradle` | Bumped version to 2.7.5 |
| `JobsPageFactory.java` | Rebuilt Jobs screen: dashboard, per-job Resume/Retry/Details/Prompt/Export/Delete |
| `MainActivity.java` | Added selected-job actions and job report export flow |
| `TranslatorService.java` | Added `ACTION_RESUME_JOB` and `ACTION_RETRY_JOB_FAILED` with `jobId` extra |
| `TranslationRepository.java` | Added richer `JobSummary` fields and `getJobSummary(jobId)` |
| `JobStore.java` | Added job facade helpers and `exportJobBundle(jobId)` |
| `LogStore.java` | Export bundle now identifies v2.7.5 |
| `TranslationEngine.java`, `CheckpointStore.java` | Comment/version wording cleanup only |

## What v2.7.5 improves

| Area | Before | After |
|---|---|---|
| Resume | Only resume latest incomplete job | Resume latest, or resume a selected job by ID |
| Retry | Only retry latest job with failed chunks | Retry failed chunks for a selected job by ID |
| Job screen | List + details + prompt preview | Dashboard + per-job action buttons + richer metadata |
| Export | Global runtime/API log only | Global log + per-job report export |
| Delete | Delete by ID | Blocks deleting active running job, confirms status/file |
| Debug | Runtime log tail | Runtime log tail + job report includes chunk states/settings/log trace |

## Per-job report contents

`JobStore.exportJobBundle(jobId)` writes:

- job id, file name, status;
- language pair;
- created/updated time;
- input/output URI;
- chunk counts: total/done/failed/pending;
- settings snapshot: provider, base URL, model, language, chunk size, refine/bilingual/partial flags;
- chunk list with status, source preview, output preview, and error message;
- runtime log tail;
- API debug trace tail.

## Static QA performed

| Check | Result |
|---|---|
| Zip extracted cleanly | PASS |
| Version bump present | PASS |
| New service actions defined | PASS |
| MainActivity calls new actions | PASS |
| JobsPageFactory constructor matches MainActivity | PASS |
| Modified Java brace/string balance | PASS |
| No reintroduced DrawerLayout dependency | PASS |
| Existing prompt/API/core flow preserved | PASS |

## Build status

APK build was **not verified in this sandbox** because the environment does not include:

- Android SDK;
- Android Gradle Plugin runtime;
- Gradle wrapper `./gradlew`.

This package is a source patch and must be opened/built in Android Studio.

## Android Studio QA checklist

1. Sync Gradle.
2. Build APK and confirm versionName `2.7.5-job-manager`.
3. Start a small translation and confirm a job appears in Jobs.
4. Pause/cancel a job, then use **Resume** on that exact job card.
5. Force one chunk error, then use **Retry** on that exact job card.
6. Open **Details** and confirm chunk status/errors are readable.
7. Open **Prompt** and preview at least chunk 1 and a later chunk.
8. Use **Export** on a job card and confirm report file contains job metadata + chunk list.
9. Try deleting a non-running job and confirm it disappears after refresh.
10. Confirm deleting a currently running job is blocked.
11. Confirm global **Export all logs** still works.
12. Confirm Translate tab still shows progress/log/last preview during a real run.

## Known limitations / deferred to v2.8.0

| Limitation | Planned version |
|---|---|
| SAF/file permission hardening is still not complete | v2.8.0 |
| Job list is still SQLite-backed without Room/DAO tests | later |
| Per-job API trace is not perfectly isolated; report includes global trace tail | later |
| No full APK build in sandbox | Android Studio required |

## Acceptance criteria for v2.7.5

Accept this build if:

- selected-job Resume works;
- selected-job Retry failed chunks works;
- job report export works;
- global translation flow from v2.7.0 still works;
- Last Translation Preview and Activity Log still appear during translation;
- no UI regression like v2.6.7 DrawerLayout/state loss appears.
