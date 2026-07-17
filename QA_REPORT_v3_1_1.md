# QA Report v3.1.1

## Scope

This patch validates and hardens the measured-cost pipeline introduced in v3.1.0. It does not change the selected model or add an automatic paid live benchmark.

## Confirmed source-level findings

- The v3.1.0 baseline estimate rebuilt chunks without their original surrounding context. v3.1.1 compares the original locked chunk and context.
- Context before and after were concatenated and trimmed from one side, which could remove all following context. They now receive separate budgets.
- Provider usage fallback was indistinguishable from provider-reported usage. Telemetry now records the usage source and cost status.
- Cached input tokens were parsed but not persisted. They are now included in metrics and exports.
- Context-length errors were classified but retried without a meaningful payload change. They are now non-retryable at the attempt layer.
- Glossary/pronoun matching depended heavily on raw substring order. Matching now supports normalization, boundaries, aliases and explicit global rules.

## Benchmark status

The app includes a no-cost dry A/B action in each Job card. It compares Full and Optimized prompts using the same stored chunks and configured max-output policy.

A paid provider A/B has not been executed in the build environment because no approved benchmark corpus/API run was supplied. Therefore semantic non-inferiority and real billed savings remain a manual release gate.

## Manual live gate

Run the same representative source with Full and Balanced using the same provider, model, temperature, configuration documents and output policy. Review both outputs blind for omissions, additions, terminology, pronouns, speaker, formatting, truncation and boundary duplication. Do not promote Balanced if it loses any critical quality category.
