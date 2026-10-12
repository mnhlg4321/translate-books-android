# G2-C1B1-C1 — Explicit Lineage Runtime Context/Service Seam

## Review-stop result

```text
G2-C1B1C1_SERVICE_SEAM: PASS
AUTHORITATIVE_IDENTITY_SCHEMA: REQUIRED
LINEAGE_RUNTIME_CALLER_WIRING: NOT_STARTED
LINEAGE_CAPABILITY_PROMOTION: NOT_STARTED
SAFE4_EXECUTION_READINESS: BLOCKED
```

C1 implements only a pure-JVM request/context/result/service seam. It prepares
an immutable `EditorialLineageRecord` for a later append phase; a ready result
does not mean that a row exists. There is no importer, project/run caller,
SQLite, DAO, migration, app composition, profile/catalog update, certification,
activation, binding or execution change.

## 1. Branch, HEAD and baseline

- Start branch: `feature/v4.16-g2-c1b1c0`.
- Start HEAD: `b66b73cf1470002e44fc19c5efcb926067fb8fa5`.
- C1 branch: `feature/v4.16-g2-c1b1c1`.
- Implementation end HEAD: `1c5ba96` (`feat(lineage): add pure-jvm runtime service seam`).
- The final documentation/checklist commit is recorded as concrete evidence in checklist step 09; the implementation baseline remains `1c5ba96`.
- SQLite remains v16.
- Latest accepted APK remains `4.16-dev.39`, code101; no APK was built or installed.
- Production evidence catalog still confirms only `pack.integrity.sha256.v1`.
- `EditorialSafe4Pack.executionEnabled()` remains `false`.
- Canonical profile hash remains `2d4e2f76dc5defcfb98cfd36cec49b0e5454cb3462db93a6b1586f7784eb91b6`.
- Machine fingerprint remains `6410f374ce175cbc6fc32484f5cd9635888b9297b01d882923af06ccd4ac1e4c`.
- `.idea/compiler.xml`, `.idea/gradle.xml` and `.idea/misc.xml` are user-owned; they were not edited, staged, stashed or committed.

## 2. Implemented pure-JVM contract

### Caller request and selection

`EditorialLineageCreationRequest` contains:

- `EditorialLineageCallerSelection`;
- optional `EditorialLineageCallerAssertions`, whose fields are explicitly
  suffix-marked `Assertion` and never authoritative.

`EditorialLineageCallerSelection` contains only caller-owned choices:

| Field | Meaning |
|---|---|
| `projectSelector` | Caller selection resolved to a stable project identity by the resolver |
| `inputScopeSelector` | Caller selection resolved to a stable chapter/input-scope identity |
| `runEvaluationSelector` | Caller selection for an existing/closed run context |
| `requestedNodeKind` | Explicit `ROOT` or `CHILD`; null is rejected by the service |
| `selectedParentRecordIdentity` | Required selection for CHILD; the only parent fact caller may select |

The request has no authoritative canonical pack hash, trusted profile fields,
machine fingerprint, contract/schema facts, compatibility outcome, capability
evidence, canonical manifest, manifest fingerprint or parent fingerprint. The
parent fingerprint is always read from the resolver's stored parent record.

Optional assertions cover pack/profile/machine/contract/schema,
compatibility-evidence and input-manifest fingerprints only to detect a race.
The resolver still loads every authoritative value independently; an assertion
mismatch returns `CALLER_ASSERTION_MISMATCH` and no record.

### Authoritative context

`EditorialLineageAuthoritativeContext` is immutable and contains:

- canonical pack hash;
- `EditorialLineageTrustContext` with trusted profile ID/version, canonical
  profile hash and machine contract fingerprint;
- contract and schema identity;
- compatibility evidence identity and
  `EditorialLineageCompatibilityStatus`;
- stable project identity and input-scope identity;
- run/evaluation identity and `EditorialLineageRunContextState`;
- complete input manifest and required input roles;
- the read-only `EditorialLineageValidationContext`; and
- resolver-provided parent candidates for the selected parent identity.

Collections are defensively copied and exposed as unmodifiable values. The
validation context is a read-only injected port; the service never mutates or
persists it.

### Resolver and service

```java
@FunctionalInterface
interface EditorialLineageContextResolver {
    EditorialLineageAuthoritativeContext resolve(
        EditorialLineageCallerSelection selection);
}

final class EditorialLineageRuntimeService {
    EditorialLineageCreationResult validateAndPrepare(
        EditorialLineageCreationRequest request);
}
```

The service performs no database or filesystem operation. Its only injected
dependencies are the resolver port and the existing pure-JVM
`EditorialLineageValidator`.

## 3. Authoritative provenance contract

| Context fact | Authoritative source in the future resolver | C1 behavior |
|---|---|---|
| Canonical pack hash | Integrity-validated `editorial_packs.canonical_pack_hash` and immutable pack manifest | Read from context; caller cannot author it |
| Trusted profile ID/version/hash | Bundled trusted registry plus v15 immutable compatibility evaluation | Must be present and match the validation trust anchor |
| Machine fingerprint | Trusted profile registry/evaluation, not pack declaration or caller | Must be present and match exactly |
| Contract/schema | Evaluated pack/profile compatibility context | Required and non-placeholder |
| Compatibility evidence identity/status | v15 compatibility evaluation identity and outcome | Only `COMPATIBLE` is preparable; `BLOCKED`, `MISMATCHED` and `STALE` fail |
| Project identity | Future immutable project identity/revision schema | C1 accepts only resolver-provided value; current AUTOINCREMENT ID is not enough |
| Input-scope identity | Future immutable chapter/scope identity plus closed source snapshot | C1 accepts only resolver-provided value; current chapter ID is not enough |
| Run/evaluation identity | Future closed immutable run/evaluation record | C1 requires `CLOSED`; it does not close or create the run |
| Input manifest | Future closed snapshot with role, ordinal, SHA-256, byte count and item count | Required roles and entries are checked; C1 does not snapshot inputs |
| Parent identity/fingerprint | Read-only lineage context/DAO adapter in a later phase | Caller selects identity; resolver supplies exact record and fingerprint |
| Duplicate/fork/reparent/cycle facts | Existing `EditorialLineageValidationContext` | Passed to the existing validator without persistence |

No SQLite AUTOINCREMENT ID, pack ID, import ID, random UUID, empty value,
`UNKNOWN`, placeholder or caller-authored trusted fact is used as semantic
identity.

## 4. RUN_CONTEXT_CLOSED gate

`validateAndPrepare` applies the following order:

1. Require a request and caller selection.
2. Call the injected resolver; null returns `LINEAGE_CONTEXT_REQUIRED`, an
   exception returns `RESOLVER_FAILURE`.
3. Require explicit node kind, resolver context, valid pack/contract/schema,
   and a read-only validation context.
4. Require caller project/scope/run selectors and authoritative stable project,
   scope and run identities.
5. Require `runContextState == CLOSED`; an OPEN run is never closed by C1.
6. Require a complete non-placeholder manifest, all required roles, valid
   hashes, non-negative counts and unique role/ordinal pairs.
7. Require a trusted profile/machine context that agrees with the validation
   trust anchor.
8. Require compatibility evidence with status `COMPATIBLE`.
9. Resolve explicit ROOT/CHILD parent semantics; never infer a root from a
   missing parent and never select a parent automatically.
10. Construct the immutable identity and exact parent reference.
11. Call the existing pure-JVM validator.
12. Return either a record-free failure or `READY_TO_APPEND` with one immutable
    record. No append operation exists in this service.

## 5. Root and child preparation

### ROOT

ROOT is accepted only when `requestedNodeKind == ROOT`, parent selection is
absent, resolver parent candidates are empty, the context is complete, the run
is closed, the manifest is complete, compatibility is `COMPATIBLE` and the
validator returns `VALID`. A missing parent is never converted into ROOT.

### CHILD

CHILD is accepted only when `requestedNodeKind == CHILD`, exactly one caller
parent identity is selected, the resolver returns exactly one record for that
identity, and its stored fingerprint is used to build the parent reference.
The existing validator then checks exact pack/profile/machine/contract/schema,
project, scope, manifest and parent-chain context. A child requires a new
run/evaluation identity supplied by the closed authoritative context.

Root-with-parent, child-without-parent, missing/ambiguous/orphan parent,
reparent, duplicate identity, cross-pack/profile/project/scope, manifest
change, fork, cycle and self-parent are all fail-closed.

## 6. Canonical result vocabulary and mapping

`EditorialLineageCreationCode` owns only service-boundary outcomes that do not
duplicate existing validator/DAO meanings:

```text
READY_TO_APPEND
VALIDATION_FAILED
LINEAGE_CONTEXT_REQUIRED
PROJECT_IDENTITY_REQUIRED
INPUT_SCOPE_REQUIRED
RUN_EVALUATION_IDENTITY_REQUIRED
INPUT_MANIFEST_INCOMPLETE
TRUSTED_PROFILE_CONTEXT_MISMATCH
COMPATIBILITY_CONTEXT_MISMATCH
CALLER_ASSERTION_MISMATCH
RESOLVER_FAILURE
```

Record-level results are lossless through
`EditorialLineageCreationResult.validationCodes()` using the existing
`EditorialLineageValidationCode` vocabulary. Therefore C1 does not create a
second enum for `MISSING_PARENT`, `PARENT_MISMATCH`, `ORPHAN_LINEAGE`,
`AMBIGUOUS_PARENT`, `DUPLICATE_LINEAGE`, `REPARENT_ATTEMPT`,
`CROSS_PACK`, `CROSS_PROFILE`, `CROSS_PROJECT`, `CROSS_INPUT_SCOPE`,
`FORK_NOT_ALLOWED`, `CYCLE_DETECTED`, `SELF_PARENT`,
`MANIFEST_MISMATCH` or `LINEAGE_IDENTITY_REVIEW_REQUIRED`.

The app DAO's later `APPENDED`, `ALREADY_EXISTS` and `PERSISTENCE_FAILURE`
outcomes remain persistence meanings. C1's `READY_TO_APPEND` is deliberately
not `APPENDED`; no DAO mapping is called.

## 7. Fail-closed matrix

| Condition | Boundary result | Record returned |
|---|---|---:|
| Resolver returns null | `LINEAGE_CONTEXT_REQUIRED` | No |
| Resolver throws | `RESOLVER_FAILURE` | No |
| Missing project identity/selector | `PROJECT_IDENTITY_REQUIRED` | No |
| Missing scope identity/selector | `INPUT_SCOPE_REQUIRED` | No |
| Missing run identity, selector or CLOSED state | `RUN_EVALUATION_IDENTITY_REQUIRED` | No |
| Required role/hash/ordinal/count missing | `INPUT_MANIFEST_INCOMPLETE` | No |
| Profile or machine trust mismatch/stale | `TRUSTED_PROFILE_CONTEXT_MISMATCH` | No |
| Compatibility blocked/mismatched/stale | `COMPATIBILITY_CONTEXT_MISMATCH` | No |
| Caller assertion differs | `CALLER_ASSERTION_MISMATCH` | No |
| Parent/fork/duplicate/cycle/manifest validation failure | `VALIDATION_FAILED` plus exact existing validation code(s) | No |
| Complete test-only context and validator PASS | `READY_TO_APPEND` | Yes |

Failure results never carry a partial or near-valid record and do not expose
exception text as a machine reason.

## 8. Production profile negative boundary

The current bundled production profile declares only
`pack.integrity.sha256.v1`, has no executable contract and therefore cannot
produce a trusted `COMPATIBLE` authoritative context for this service. The
future production resolver must surface that non-executable state as
`EditorialLineageCompatibilityStatus.BLOCKED`; C1 returns
`COMPATIBILITY_CONTEXT_MISMATCH`.

The positive root/child tests use an explicitly test-only authoritative context
and do not prove that the production profile is executable. No resolver was
added to production composition and no capability support decision changed.

## 9. Stable identity schema decision

```text
AUTHORITATIVE_IDENTITY_SCHEMA: REQUIRED
```

C0's source audit remains decisive: current `editorial_projects.id`,
`editorial_chapters.id` and `editorial_runs.id` are local AUTOINCREMENT keys;
project identity can be updated; the current run table has no production
creation seam; and no safe run-to-lineage binding exists. C1 does not hide this
with fixture values or placeholders.

The next phase must therefore be a separately reviewed:

```text
G2-C1B1-C1.5 — additive authoritative project/scope/closed-run identity
schema review and implementation
```

It must define immutable identity/revision and retention/deletion semantics,
closed input snapshots and a safe additive run-to-lineage binding boundary.
It must not backfill or silently assign identities to old rows. C2 caller
integration is locked until C1.5 is approved and implemented.

## 10. Implementation and regression evidence

### Production classes in C1

- `EditorialLineageCreationCode`
- `EditorialLineageCompatibilityStatus`
- `EditorialLineageCompatibilityEvidence`
- `EditorialLineageRunContextState`
- `EditorialLineageCallerAssertions`
- `EditorialLineageCallerSelection`
- `EditorialLineageCreationRequest`
- `EditorialLineageAuthoritativeContext`
- `EditorialLineageContextResolver`
- `EditorialLineageCreationResult`
- `EditorialLineageRuntimeService`

### Focused pure-JVM tests

`EditorialLineageRuntimeServiceTest` uses only in-memory resolver and
validation-context doubles. The final focused result is `21/21` passed,
0 failed, 0 errors, 0 skipped. It covers deterministic root/child preparation,
caller-field restrictions, closed-run and manifest gates, blocked profile/
compatibility, assertion mismatch, parent resolution, duplicate/reparent/
orphan/ambiguous/cross-context validation, resolver null/throw, defensive
copies and locale determinism.

### Required final commands

| Command | Result |
|---|---|
| `:editorial-engine:clean :editorial-engine:test :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac --no-daemon` | PASS; engine 95/95, app 163/163, Android-test Java compilation PASS |
| Combined JVM total | 258 passed, 0 failed, 0 errors, 0 skipped |
| `:app:lintDebug --no-daemon` | PASS; 0 errors, 53 existing warnings |
| `git diff --check` | PASS |

No SQLite, Android, importer or app-runtime dependency was added to the
`editorial-engine` seam. No APK build-and-save command was run.

## 11. Final protected state and review stop

- No lineage row, run-lineage binding, migration or persistence call was added.
- No project/chapter/run repository or importer call site was changed.
- The production catalog still has only `pack.integrity.sha256.v1`; lineage is
  not promoted or resolver-supported in production.
- The bundled profile and both protected trust anchors are unchanged.
- SQLite remains v16 and old history is not reevaluated.
- Certification, Golden Replay, activation, project binding and execution remain
  locked.
- `EditorialSafe4Pack.executionEnabled()` remains `false`.
- Latest accepted APK remains `4.16-dev.39`/code101.
- The three `.idea/*` files remain untouched, unstaged and uncommitted.
- Review stop: do not start C1.5 or C2 automatically.
