# Editorial Pack Platform — G2-C0A Evidence Checklist

Status: `IN_PROGRESS / PURE JVM ONLY / STOP BEFORE G2-C0B`

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

- [ ] Immutable model/schema added without Android or persistence dependencies.
- [ ] Existing G2-A `EditorialEngineProfile` audited and conversion boundary is
  documented/tested; no contradictory runtime source of truth is introduced.
- [ ] Strict parser rejects malformed UTF-8/BOM/duplicate/unknown/missing/type
  errors and applies size limits.
- [ ] Canonicalizer is deterministic across field order, whitespace, locale,
  timezone and platform line endings.
- [ ] Machine-contract fingerprint is recomputed from semantic machine fields.
- [ ] Canonical profile hash omits only `canonicalProfileHash`.
- [ ] Validator enforces capability evidence-catalog separation and fail-closed
  profile rules.
- [ ] Focused G2-C0A tests pass.
- [ ] Full `:editorial-engine:test` passes.
- [ ] Full `:app:testDebugUnitTest` passes.
- [ ] Android instrumentation source compilation passes.
- [ ] `git diff --check` passes.
- [ ] No profile is bundled, no runtime wiring is changed, and no APK is built.

## Review stop

After all evidence above is recorded, stop for user review. Do not start
G2-C0B or G2-C0C without a separate approval.
