# Editorial Pack Platform — G2-C0A Evidence Checklist

Status: `PASS / PURE JVM ONLY / REVIEW STOP BEFORE G2-C0B`

Date: `2026-08-04`

Approved scope: immutable trusted engine contract profile model, strict parser,
canonicalization, machine-contract fingerprint, canonical profile hash,
validator and pure-JVM tests in `:editorial-engine`.

Explicitly out of scope: bundled production profile, registry, selector,
runtime wiring, compatibility-result changes, SQLite, importer, UI, APK build,
certification, activation, project binding, L1/L2/L3, Golden Replay and
`EditorialSafe4Pack.executionEnabled()`.

## Baseline evidence

- Branch: `feature/v4.16`.
- Baseline HEAD before G2-C0A: `d5dfe19126e5cf32aa62ff77aacd83a3b2f0299b`.
- Latest accepted APK remains `4.16-dev.29`/code91, event
  `build-20260804-073059`; no APK build is planned for this pure-JVM group.
- The only pre-existing worktree change is user-owned `.idea/gradle.xml`; it
  must remain unstaged.
- SQLite remains v14. The runtime still supplies no trusted contract
  descriptor, packs remain `STORED_BLOCKED`, and
  `EditorialSafe4Pack.executionEnabled()` remains false.

## Evidence gates

- [x] Immutable model/schema added without Android or persistence dependencies.
- [x] Existing G2-A `EditorialEngineProfile` audited and conversion boundary is
  documented/tested; no contradictory runtime source of truth is introduced.
- [x] Strict parser rejects malformed UTF-8/BOM/duplicate/unknown/missing/type
  errors and applies size limits.
- [x] Canonicalizer is deterministic across field order, whitespace, locale,
  timezone and platform line endings.
- [x] Machine-contract fingerprint is recomputed from semantic machine fields.
- [x] Canonical profile hash omits only `canonicalProfileHash`.
- [x] Validator enforces capability evidence-catalog separation and fail-closed
  profile rules.
- [x] Focused G2-C0A tests pass: 14/14, 0 failures/errors/skips.
- [x] Full `:editorial-engine:test` passes: 33/33, 0 failures/errors/skips.
- [x] Full `:app:testDebugUnitTest` passes: 161/161, 0 failures/errors/skips.
- [x] Android instrumentation source compilation passes:
  `:app:compileDebugAndroidTestJavaWithJavac`.
- [x] `git diff --check` passes.
- [x] No profile is bundled, no runtime wiring is changed, and no APK is built.

## Implementation evidence

- Model/catalog: `EditorialEngineContractProfile` and
  `EditorialEngineContractCapabilityEvidenceCatalog`.
- Parser/canonicalization: `EditorialEngineContractProfileParser` and
  `EditorialEngineContractProfileCanonicalizer`; strict UTF-8, no BOM,
  duplicate/unknown/missing/type rejection, bounded input and deterministic
  set-like array ordering.
- Validation: `EditorialEngineContractProfileValidator` and immutable
  `EditorialEngineContractProfileValidationResult`; only the production
  `pack.integrity.sha256.v1` capability is confirmed by the code-owned
  evidence boundary. The nine SAFE4 capabilities remain explicitly missing.
- Tests: `EditorialEngineContractProfileTest` covers 30 required positive and
  negative cases through 14 focused test methods, including the test-only
  namespace catalog boundary. The fixture does not claim any SAFE4 capability.
- Regression command used the unchanged cached Gradle 9.4.1 executable because
  the repository wrapper remains Gradle 9.3.0 while the installed Android
  Gradle Plugin requires 9.4.1. No wrapper change and no APK build occurred.
- Safety evidence remains unchanged: canonical SAFE4 Project Instruction
  `C57100C45F16FC5A27E56AE17ABE919BE89DA55082A66D060DB504746ED8B717`, Prompt
  `0B4C02573F46A91528A63262D3E52C655A5C7E31F2ABBBFB01759D38E94F8E81` and
  Workflow `7A434ADE77239DB33AF5A677456D0310AC11DC8391565FC4888236536FF96B5E`
  were not changed. Candidate `DBE214...` and `3B2FCC...` were not read,
  imported or activated; the existing blocked QA row remains unchanged.
- Commits: `0efde60` model, `2f0a157` parser/canonicalization, `31b40d2`
  validator/result, `02af527` tests. Snapshot evidence is being finalized in
  the documentation commit.

## Review stop

After all evidence above is recorded, stop for user review. Do not start
G2-C0B or G2-C0C without a separate approval.
