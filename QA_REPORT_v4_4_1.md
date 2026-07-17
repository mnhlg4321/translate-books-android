# Translate Books 4.4.1 Hotfix QA Report

Status: implementation complete; release validation incomplete because a real OpenRouter credential was not available for the required paid run.

## Release artifact

- versionName: `4.4.1`
- versionCode: `43`
- APK: `app/build/outputs/apk/debug/TranslateBooks-v4.4.1-debug.apk`
- SHA-256: `88F5346097A899431BAA8C00F5261F646996AB3E9FD1421A99ABFAD3837746C6`
- Device package verification: versionCode 43, versionName 4.4.1, minSdk 26, targetSdk 35.

## 4.4 reproduction and crash evidence

Device: OnePlus CPH2691, Android 15.

The unmodified 4.4 build reproduced the blocking runtime failure but did not emit a Java FATAL EXCEPTION. Fresh Logcat contained no crash stack. Dropbox contained only an unrelated old v4.1 scrollbar NPE, so it is not presented as 4.4 evidence.

Observed 4.4 sequence:

- The 102,078-character Japanese input took about 36 seconds to show 42 chunks.
- Start blocked the UI for another roughly 35 seconds by synchronously repeating estimation.
- The foreground service then prepared the same input again for more than 60 seconds.
- Cancel did not interrupt the running chunk scan.
- No `chunk_request` event occurred and no paid API request was sent before the app was force-stopped for safety.
- The process remained alive; therefore there is no truthful “full crash stack trace” to return for this reproduction.

Conclusion: an exact Java-crash root cause was not established. The confirmed Start failure was execution starvation caused by synchronous repeated preparation, plus an unguarded service-dispatch/service-start path that could terminate on device-specific Android restrictions.

## Root cause: slow and hot estimation

- `MainActivity.updateInputEstimate()` created uncancelled threads and reread, normalized, chunked, and rebuilt prompts for the full file.
- catalog callbacks and UI rebuilds retriggered estimation.
- Start synchronously called the full estimator again.
- `TranslatorService` called `TranslationEngine.prepareInput()` again after Start.
- `Chunker.advanceToMeasure()` tokenized successively larger substrings for each code point, producing quadratic work and allocations.
- glossary/pronoun parsing was repeated for each prompt/chunk.

## Translation-base audit result

- Start now requires the exact prepared batch and verifies its construction/estimate keys.
- File/config/output checks run in an asynchronous preflight; pricing is optional.
- Prepared chunks are persisted in schema 9 and copied atomically into a uniquely identified job.
- Service startup is guarded, promoted before database/engine initialization, and handles Android 15 timeout by checkpointing.
- Provider calls have a deterministic test seam and durable translation/refinement attempt phases.
- The request record exists before network work. Send start, body sent, headers, response content/ID/usage/hashes, and validation state are persisted.
- Death after send without a durable response becomes `DELIVERY_UNKNOWN` / `NEEDS_REVIEW`; automatic resume will not resend it.
- Explicit “Retry anyway” confirmation is required for delivery-unknown chunks.
- Persisted responses interrupted before commit return to `RESPONSE_RECEIVED` for local revalidation without another call.
- Accepted content and the final attempt state commit in one transaction. Rejected candidates retain prior accepted output.
- Folder output files are not created until contiguous accepted output is non-empty.
- Ordered assembly and integrity audit remain database-authoritative.

## Performance measurements

| Build/input | Preparation result | Time | Thermal/CPU result |
|---|---:|---:|---|
| 4.4, 102,078 Japanese chars | 42 chunks | ~36 s estimate; ~35 s repeated Start estimate; service still preparing after 60 s | 34.3→34.4°C during estimate, later 35.0°C; sustained CPU |
| 4.4.1, 133 kB Japanese TXT | 63 chunks / 132,369 tokens | 341 ms coordinator timing | 35.1→35.1°C; zero process CPU ticks over the next 10 s |
| final 4.4.1, larger Japanese TXT | 141 chunks / 297,638 tokens | 872 ms coordinator timing | 35.2→35.2°C; 0.0% sampled CPU, 3 process CPU ticks over 10 s |

Reopening the 63-chunk input left the preparation-ready event count unchanged (1→1), proving persisted reattachment rather than another preparation run.

## Physical device/runtime results

- Final APK installed and version metadata verified on the OnePlus/Android 15 device.
- Large real Japanese TXT selection completed with exact chunks, tokens, and price visible; Start remained above the fold.
- CPU settled after preparation and temperature did not increase.
- Process identity remained unchanged after pressing Start with a blocking missing-key error; Logcat contained no FATAL EXCEPTION.
- Deterministic device tests verified ordered two-chunk success, durable accepted attempts/provider response IDs, delivery-unknown pause, and response recovery after simulated death before commit.
- Cached prepared plan survived force-stop/reopen without a new preparation task.

Not validated on the final artifact: real provider request/parse/commit/output, real pause/resume around a paid request, and gesture-versus-three-button navigation comparison.

## Real-provider result

Not run. The deterministic connected-test installation removed the app’s prior saved data, including the credential. No API key was available afterward, and no paid request was attempted. This is the remaining release gate; the hotfix is not claimed fully delivered under the user’s stated acceptance criteria.

## Tests and lint

- `testDebugUnitTest`: 88 tests, 0 failures, 0 errors.
- `connectedDebugAndroidTest`: 3 tests on CPH2691 / Android 15, 0 failures, 0 errors.
- `lintDebug`: passed, 0 errors, 45 existing warnings.
- `assembleDebug`: passed.
- `assembleDebugAndroidTest`: passed.

Coverage added for linear large-Japanese preparation, exact prepared estimates, configuration/estimate key separation, pricing loading/unavailable independence, cancellation, release metadata, ordered fake-provider completion, sent-but-unknown recovery, and received-response recovery.

## Modified files

- `app/build.gradle`
- `app/src/main/java/com/ml/tblandroidtxt/AppBuildInfo.java`
- `app/src/main/java/com/ml/tblandroidtxt/PreparedBatch.java`
- `app/src/main/java/com/ml/tblandroidtxt/PreparationCoordinator.java`
- `app/src/main/java/com/ml/tblandroidtxt/Chunker.java`
- `app/src/main/java/com/ml/tblandroidtxt/CostEstimator.java`
- `app/src/main/java/com/ml/tblandroidtxt/PromptContextBuilder.java`
- `app/src/main/java/com/ml/tblandroidtxt/ModelCatalog.java`
- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslatePageFactory.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslatorService.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslationEngine.java`
- `app/src/main/java/com/ml/tblandroidtxt/OpenAICompatibleClient.java`
- `app/src/main/java/com/ml/tblandroidtxt/TranslationRepository.java`
- `app/src/main/java/com/ml/tblandroidtxt/JobStore.java`
- `app/src/main/java/com/ml/tblandroidtxt/JobsPageFactory.java`
- `app/src/main/java/com/ml/tblandroidtxt/FileUtil.java`
- `app/src/test/java/com/ml/tblandroidtxt/Hotfix441Test.java`
- `app/src/androidTest/java/com/ml/tblandroidtxt/Hotfix441InstrumentedTest.java`
- `CHANGELOG.md`
- `QA_REPORT_v4_4_1.md`

## Known limitations

- No reproducible 4.4 Java FATAL stack exists from the observed device run; the evidence is an ANR-like repeated-work failure before any request.
- The required real OpenRouter capped translation is not complete because no credential is available.
- The device suite covers the highest-risk persistence/recovery paths but not every requested UI/service scenario as a separate instrumentation case.
- Lint has 45 warnings (no errors), including legacy/deprecation and internationalization warnings outside this focused hotfix.
