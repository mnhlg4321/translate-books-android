# G2-C1B1-C2B2A — Production Lifecycle Dependency Plan

Status: `CREATED / REVIEW STOP` because G2-C1B1-D0 is blocked.

This is the sole permitted dependent output after the D0 blocker. It is a
plan only. It does not implement a run lifecycle, event store, SQLite v18,
caller wiring, profile change, production row, or capability promotion.

## Decisions

### Is an active caller required for promotion?

**Yes for this capability evidence package, but not as UI activation.**

`lineage.exact-parent.v1` is not only a pure canonicalizer. Its production
meaning includes a closed run, trusted evaluation/profile context, frozen input
manifest, and exact ROOT/CHILD parent semantics. The C1A promotion rule
requires production implementation and app/integration evidence where those
facts are composed. A production-equivalent authoritative closure source and
owner are therefore required before this capability can be eligible.

MainActivity, startup, ZIP importer, page rebuild, project preparation, and
model-request completion are not acceptable callers. The future caller must
be a single run-lifecycle owner at the actual `RUN_CONTEXT_CLOSED` transition.
The lineage coordinator remains the application orchestration boundary after
that event; it does not invent the event.

### Is a durable event store needed?

**Yes.** The event must survive process death and preserve the exact semantic
intent that locks ROOT versus CHILD and the exact parent selector. The current
v17 `editorial_closed_run_contexts` row stores closed-run facts but does not
persist the closure-event identity/payload, node kind, or exact parent selector
needed to replay that event safely before lineage binding. An in-memory event
or a reconstructed event from chapter state would permit parent drift and is
not authoritative.

### Is SQLite v18 needed?

**A separately approved additive schema/version phase is needed under the
SQLite-authoritative-store assumption; v18 is the current proposal, not an
implementation.** v17 must remain unchanged. The next phase must first audit
whether the existing v17 closed-run identity can be joined to a new immutable
closure-event projection without semantic duplication. If it cannot, v18 must
add an append-only event table/indexes/triggers with zero backfill and
`ON DELETE RESTRICT`. No v18 migration is authorized in this plan.

An alternative durable store would require a new reviewed ownership and
retention contract; it cannot be silently substituted for SQLite.

### Run lifecycle now or L1 runner?

Build the authoritative run lifecycle/event source first, in a separately
approved production-lifecycle phase before caller wiring. Do not defer it to
the L1 runner. L1 consumes a closed, lineage-bound context; it cannot be the
owner of the closure event without creating a circular dependency. The
lifecycle phase may be implemented while the current production profile still
fails closed, but it must not create production `DATA_COMPATIBLE` or closed-run
rows until a separate executable-profile gate is approved.

## Non-circular phase order

```text
C2B2A-1  audit/approve run lifecycle owner and event contract
    -> C2B2A-2  add the separately approved durable event projection (v18 only if required)
    -> C2B2A-3  implement immutable closure emission and exact readback/recovery
    -> D0 re-audit  production-positive lineage evidence
    -> D1 promotion review, only if every C1A condition passes
    -> later caller wiring/retention phase at RUN_CONTEXT_CLOSED
    -> L1 runner consumes the already-bound context
```

This ordering does not promote the capability automatically. It also does not
change the current profile, create a production compatible evaluation, or
enable execution.

## Required lifecycle contract for the future phase

- One authoritative owner emits exactly one immutable `RUN_CONTEXT_CLOSED`
  event after the input manifest is frozen and trusted evaluation facts are
  available.
- The event contains a stable identity and canonical fingerprint over project,
  scope, evaluation selector, run kind, phase, frozen-manifest attestation,
  explicit ROOT/CHILD, and exact parent selector for CHILD.
- Callers provide only the event selector and minimal user/workflow intent;
  they cannot provide hashes, profile facts, evaluation outcome, manifest
  fingerprint, lineage identity, parent fingerprint, timestamp, or attempt
  ordinal.
- Event append is immutable and exact-idempotent. Same identity/different
  canonical bytes is a terminal collision. Retry reads the same event and
  cannot select latest, infer ROOT, or choose another parent.
- The future caller invokes C2-B1 Option B only after event readback. A closed
  context without an exact binding remains derived `CLOSED_UNBOUND`; no state
  update or v18 status column is used for that projection.
- Delete/retention preflight runs before mutable project/chapter/run deletion;
  authoritative rows are never cascade-deleted or detached, and UI receives a
  stable domain result instead of a raw SQLite exception.

## Acceptance and rollback boundary

The lifecycle phase is not complete until it proves restart/readback,
duplicate invocation, process death after close, ROOT/CHILD parent stability,
zero/multiple parent rejection, event collision, trusted-profile mismatch, and
zero production rows when the current profile is non-executable. Its rollback
is an implementation/state rollback before any profile/catalog promotion; it
must not rewrite v17 history or delete authoritative evidence.

The exact next action is review of this dependency plan. No implementation is
started by C2B2A, and D1/C1B2-A remain gate-blocked.

```text
PRODUCTION_RUN_CONTEXT_CLOSED_EVENT: ABSENT
PRODUCTION_CALLER_OWNER: UNRESOLVED
SQLITE_V18: PROPOSED_ONLY / NOT_IMPLEMENTED
PRODUCTION_CLOSED_RUN_CREATION: BLOCKED
LINEAGE_PROMOTION: BLOCKED_PENDING_D0_DEPENDENCY
SAFE4_EXECUTION_READINESS: BLOCKED
```
