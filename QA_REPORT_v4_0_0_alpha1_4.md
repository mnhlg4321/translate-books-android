# QA Report v4.0.0-alpha1.4

## Release identity

| Field | Value |
| --- | --- |
| Application ID | `com.ml.tblandroidtxt` |
| Version code | `38` |
| Version name | `4.0.0-alpha1.4` |
| Min SDK | `26` |
| Target / Compile SDK | `35` / `35` |

## Estimator behavior

- Input tokens are summed from the actual `PromptPlan` for every chunk, including representative previous-translation context.
- Expected cache assumes a cold batch: request one is uncached; later requests reuse only exact shared system-prefix blocks.
- Expected cache is disabled when cached-input pricing is unknown.
- UI displays expected cached and cold/no-cache ranges separately.
- Cost-limit and live-benchmark budget gates continue to use the conservative no-cache upper bound.

## Exact sample result

Same sample/profile used by the TBL audit (`openai/gpt-5.4-mini`, OpenRouter, five chunks):

| Metric | Value |
| --- | ---: |
| Expected cached cost | `$0.064–$0.081` |
| Cold/no-cache cost | `$0.087–$0.104` |
| Prompt input estimate | `58,875` |
| Expected cached input | `34,432` |
| Expected uncached input | `24,443` |
| Expected output | `9,604–13,263` |
| Source | `9,148` |
| Instruction | `42,730` |
| Glossary | `1,835` |
| Pronoun | `2,431` |
| Context, including history | `1,822` |

For reference, TBL displays `$0.060–$0.077`; the previous Android estimator displayed `$0.088–$0.116`.

## Verification

- `clean testDebugUnitTest assembleDebug assembleRelease`: PASS.
- 40 unit tests: PASS.
- Release lint vital analysis/report: PASS.
- Debug APK signature: valid APK Signature Scheme v2.
- Release APK: generated unsigned because no release keystore is configured.

## Artifacts

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `app-debug.apk` | 1,931,584 bytes | `C29F7EA421FB1CCCD0C8B7D0B2EEB0EE3E41C566903FB305C8B5803DC4B9D0D5` |
| `app-release-unsigned.apk` | 1,478,439 bytes | `B28A9AC1962FEF5995DC596263FB68C533E94A5F8F39678A86C513CF076EC841` |

