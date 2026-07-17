# QA Report v3.2.0

## Scope

Production Benchmark and Provider Intelligence. Live A/B requests are initiated only by the user from a stored Job and run inside TranslatorService after a visible cost confirmation.

## Privacy and budget

- No live provider request is executed during build or tests.
- The on-device workflow sends only three selected stored chunks after confirmation.
- Full and Balanced use the same job settings and max-output policy.
- The runner refuses unknown pricing and stops before a request whose worst-case cost could exceed the USD 0.50 hard budget.
- API keys are not included in benchmark result rows or exports.

## Manual quality gate

Open Jobs, select Live A/B, wait for completion, then open Blind review. Compare X/Y for omissions, additions, speaker, subject, names, glossary, pronouns, formatting, truncation and boundary continuity before revealing the mapping.

## Build-environment limitation

The assistant build environment is prohibited from exporting the supplied private book file to OpenRouter. Therefore real provider savings and semantic quality must be measured through the on-device, user-confirmed workflow.
