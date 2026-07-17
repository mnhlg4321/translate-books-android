# TBLAndroidTxt v2.6.9 Debug Stable - QA Report

## Scope

This build is an incremental debug/stability release based on v2.6.8-safe-core-ui. It intentionally avoids a large UI rewrite. The goal is to make translation failures diagnosable without breaking the stable core flow restored in v2.6.8.

## Confirmed source state

- Project type: Android native Java app.
- Main package: `com.ml.tblandroidtxt`.
- UI: programmatic Android views in Java, no XML layout/Compose.
- Networking: OkHttp OpenAI-compatible chat completions client.
- Storage: SharedPreferences + internal files + SQLite checkpoint repository + Android SAF URI file I/O.
- Core touched in v2.6.9: API error parsing, debug trace, prompt preview/export log only.

## Changes made

| Area | File(s) | Change |
|---|---|---|
| Version | `app/build.gradle` | versionCode 22, versionName `2.6.9-debug-stable` |
| API errors | `ApiErrorParser.java`, `OpenAICompatibleClient.java`, `AppValidator.java` | Parse HTTP/API/network/model response errors into readable Vietnamese messages |
| Debug trace | `DebugTraceStore.java`, `TranslatorService.java` | Save recent prompt/raw response/extracted translation/error per chunk |
| Export log | `LogStore.java`, `RuntimeStateStore.java`, `MainActivity.java` | Export runtime state + log + API debug trace as one text file |
| Prompt preview | `PromptPreviewDialog.java`, `JobsPageFactory.java` | Show matched glossary/pronoun rules before final prompt |
| Log clear | `MainActivity.java` | Clear runtime log and API trace together |

## QA checklist for Android Studio/device

| Test | Expected result |
|---|---|
| Sync Gradle | Project sync succeeds with Android SDK 35 and JDK 17 |
| Open app | Stable tab UI appears, no status-bar overlap |
| Quick test API with wrong key | Shows readable 401/key error, log records readable error |
| Quick test API with wrong model | Shows readable 403/404/422 style error depending on provider |
| Prompt preview after selecting TXT | Dialog shows MATCHED GLOSSARY / PRONOUN RULES + SYSTEM + USER |
| Start translation | Service runs foreground job, progress/log/preview update |
| Provider returns missing tags but plain translation | Safe fallback still accepts valid translation |
| Provider returns prompt echo/meta/refusal | Chunk fails with clear message instead of silently writing bad output |
| Export log after a failed chunk | Export contains runtime state, runtime log, API debug trace |
| Clear log | Runtime log and API debug trace are cleared; checkpoint/job remains |
| Cancel while API request is active | OkHttp active call is cancelled; app reports cancelled/stopping |

## Static verification performed in sandbox

- Source files patched successfully.
- Version bump verified.
- New classes present: `ApiErrorParser`, `DebugTraceStore`.
- References verified by grep: debug trace is called from `TranslatorService`; export bundle is called from `MainActivity`; prompt preview includes matched glossary/pronoun section.

## Build status

Not built in this sandbox because the environment has Java/Javac only, but does not include Android SDK, Gradle CLI, or a Gradle wrapper. Build must be verified in Android Studio.

## Risk notes

- Debug trace stores prompt and model output locally inside app private storage. It does not store the API key. Use Clear log to remove it.
- Large prompts/responses are capped by the rolling trace file limit, but exported logs can still contain novel text. Treat exported logs as private.
- This release improves diagnosis; deeper job manager and SAF hardening are still planned for v2.7.x/v2.8.x.
