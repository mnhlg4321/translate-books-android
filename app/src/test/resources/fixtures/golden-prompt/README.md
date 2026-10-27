# Golden prompt fixtures (v4.24 P0a)

Byte-exact snapshot of what the translation path produces for fixed, invented inputs. Used by
`GoldenPromptSnapshotTest`; later steps that move code into a shared module must reproduce `expected/` unchanged.

- Inputs (all self-written, invented story): `raw_ja.txt`, `glossary.csv`, `pronoun.csv`, `pronoun_legacy3.csv`,
  `instruction.yaml`, `prev_translation.txt` (1500 chars, stands in for `tail(translated, 1500)`), `cases.json`.
- `cases.json` writes every setting field per case; nothing relies on `new AppSettings()` defaults.
- `expected/<case>/`: `chunks.json`, `chunk-<i>.{system,user}.txt`, `chunk-<i>.{plan,request}.json`,
  `estimate.json`, `estimate-lightweight.json`. Floating-point values are written as 6-decimal strings.
- Path under test: `AppValidator.normalize` -> `Chunker.chunkText` -> `PromptPlan.forTranslation` ->
  `OpenAICompatibleClient.buildChatRequestBody(..., null, false, ReasoningEffort.forTranslation(s))` ->
  `CostEstimator.estimatePreparedChunks` / `estimatePreparedChunksLightweight`.
- Rewrite goldens only with `gradlew :app:testDebugUnitTest --tests '*GoldenPromptSnapshotTest' -Dgolden.update=true`,
  then review the diff.

Not covered: `bilingualOutput` (not used by the translation prompt), refinement prompts (only their token cost inside
`refine_on_estimate`), and priced estimates (the test model has no catalog price, so costs are `NaN`).
