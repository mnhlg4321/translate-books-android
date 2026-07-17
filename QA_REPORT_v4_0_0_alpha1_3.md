# QA Report v4.0.0-alpha1.3

## Release identity

| Field | Value |
| --- | --- |
| Application ID | `com.ml.tblandroidtxt` |
| Version code | `37` |
| Version name | `4.0.0-alpha1.3` |
| Min SDK | `26` |
| Target / Compile SDK | `35` / `35` |

## Scope

- Scene-aware pronoun selection based on character names, aliases and dialogue evidence in the current chunk.
- Context-only characters and incomplete relationship pairs are excluded.
- Primary/dialogue character rules remain complete; secondary rules retain compact essential guidance.
- Prompt Breakdown displays Source, Instruction, Glossary, Pronoun and Context token estimates.

## Benchmark

Same sample, OpenRouter and `openai/gpt-5.4-mini` profile:

| Metric | Before | After |
| --- | ---: | ---: |
| Prompt tokens | 47,674 | 40,498 |
| Completion tokens | 8,274 | 8,344 |
| Total tokens | 55,948 | 48,842 |
| Observed charged cost | $0.059856 | $0.050123 |

## Verification

- `clean testDebugUnitTest assembleDebug assembleRelease`: PASS.
- Release lint vital analysis/report: PASS.
- Debug APK signature: valid APK Signature Scheme v2, Android Debug certificate.
- Release APK: generated unsigned; no release keystore/signing configuration is present in the project.

## Artifacts

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `app-debug.apk` | 1,930,448 bytes | `23DB7AC6EF585F4E3BF3A4716F1F535F52A901F867F8964AA1253DC035B8F5D7` |
| `app-release-unsigned.apk` | 1,476,867 bytes | `1A0F228B5E2F5BD1BD56E4B984B285106D26C6C08BC531B71400458DA6227596` |

