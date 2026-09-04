# P4 — Immutable Binding Tuple

Ngày chốt: `2026-09-04` (+07:00)

P4 lưu setup metadata cho một project/run cụ thể. Tuple này không phải
certification receipt và không mở Editorial execution.

## Identity tuple

| Nhóm | Trường được pin |
|---|---|
| Pack | `packId`, `packVersion`, `canonicalPackHash`, `manifestFingerprint` |
| Trusted profile | `trustedProfileId`, `trustedProfileVersion`, `canonicalProfileHash`, `machineContractFingerprint` |
| Compatibility | `compatibilityEvaluationId`, `compatibilityOutcome`, `evaluationContextFingerprint`, `evaluatedAt` trong evaluation row |
| Contract | `contractVersion`, `schemaVersion`, `phaseGraphFingerprint`, `contextAllowListFingerprint` |
| Project/scope/run | `projectRevisionIdentity`, `inputScopeSnapshotIdentity`, `runDeclarationIdentity`, `runAttemptOrdinal`, `runKind`, `phaseIdentity` |
| Source decisions | `sourceMode`, `glossaryStatus`, `pronounStatus`, `pairContextStatus`, `explicitUserDecisionProvenance` |
| Input identity | per source: `role`, `sourceReference`, app-computed `byteLength`, app-computed `sha256`, `encoding`, `schemaStatus`, `ordinal` |
| Safety state | `executionAllowed=false`, `certificationState=NOT_CERTIFIED` |

`EditorialP4Binding` canonicalizes the complete projection and derives the
binding identity/fingerprint from that projection. The service computes source
length/hash from the supplied bytes; it does not trust a UI/model-provided
hash.

## Selection and persistence invariants

- Selection resolves the exact user-selected `packId + packVersion`. There is
  no global `activePack`, `currentEditorialPack`, `latestPack` or fallback.
- A selectable candidate must have a valid immutable storage marker, valid
  integrity, trusted-profile `DATA_COMPATIBLE`, matching profile hash and
  machine fingerprint, and no missing required capability.
- The legacy `editorial_projects.workflow_version/workflow_hash` columns remain
  legacy/read-only markers. The P4 binding table is authoritative for an
  imported pack setup.
- Project row, project revision, input scope snapshot, run declaration and P4
  binding are appended in one SQLite transaction. A failed final append leaves
  no partial rows and does not consume an attempt ordinal.
- The binding and its source child rows are protected by immutable triggers and
  restrictive foreign keys. Retry with the same explicit selector and facts is
  `ALREADY_EXISTS`; a selector collision with different facts is rejected.

## Resume and stale-chain policy

Resume reads the exact persisted selector/project binding and rechecks source
bytes, immutable pack storage, manifest/profile/evaluation facts and the
revision/scope/declaration chain. It never queries the latest pack and never
rewrites a stale record.

Any source identity drift, missing/tampered pack storage, profile/evaluation
drift, missing identity row or declaration-chain mismatch returns a visible
stale result. The record remains inspectable; recovery requires restoring the
exact facts or creating a new project/run chain.

Activity recreation has no in-memory pack binding state. The UI recreation test
asserts the management surface remains read-only, while the binding acceptance
tests close/reopen the database and the two-invocation device proof resumes the
same tuple after a host `am force-stop`.

## Schema decision

The P4 characterization test demonstrated that v18 identity stores could not
retain source reference, encoding, schema status and the complete pack/profile/
evaluation tuple. An additive schema v19 owner was therefore introduced:

- `editorial_p4_bindings`
- `editorial_p4_binding_inputs`

No legacy rows are backfilled, no existing contract is changed, and no
execution/certification state is persisted as enabled.
