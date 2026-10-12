# P5E A4.3 preauthorization runtime-guard closure — final Luna review

Review date: `2026-09-28`.
Reviewed commit: `6faebf0ad82ec6f0efba3ef42f50fec9e7bf3a74`.
Review mode: exact-byte, offline, read-only. No receipt, event, live command,
ADB, device, provider, credential, database write, build/install, RAW, retry,
fallback, RECONCILE, redispatch or P6 action was performed.

## Verdict

`PASS / 0 BLOCKER / 0 HIGH / 0 MEDIUM / 0 LOW`.

The packet is executable only after a new owner decision is separately
provided. Current state remains:

`FINAL_EXECUTABLE_A43_PACKET_READY / OWNER_DECISION_PENDING / NOT_DISPATCHED / P6_NOT_READY`.

## Exact bytes reviewed

| Role | Path | SHA-256 |
|---|---|---|
| Manifest | `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_MANIFEST_20260928.md` | `B093AB871072D78A9A935914AEC9FF56835606BB344F034AA5EE2F38EA83FF4F` |
| Command | `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_COMMAND_20260928.txt` | `FA63C5D2498AA345C6AC426AD253B466D6BD3206D525BAE4E0E8E6158BA78F80` |
| Helper | `scripts/p5e-raw-live-supervisor.ps1` | `86E386375B771B001B911FB2F538E112F6AD9F709BB933726FEB981D6F875FB6` |
| Exporter | `scripts/p5e-db-binary-export.ps1` | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| Runtime guard | `scripts/p5e-a43-runtime-guards.ps1` | `F332954FB8AA2048EDF18630C5D1EF7B2039D57458C85CD6BB5CB07F59D96DBA` |
| SQLite bridge | `docs/P5E_SQLITE_BRIDGE.py` | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| Toolchain | `scripts/p5e-raw-toolchain.ps1` | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| QA script | `scripts/test-p5e-a43-preauth-runtime-guard-closure.ps1` | `393196B7DC9C7BBA98A319DE1828E3CD6641A091946232C420B63556CF48B9EB` |
| QA report | `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_QA_20260928.json` | `9E461ADF9471B9B836B506079BCD34F9D01805E66FD07E29DD37E776F929F393` |
| Certificate | current packet certificate | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| Serial | current packet serial | `15e84958` |

## Review coverage

- Owner receipt schema, exact packet/serial/scope/hash binding, one-event and
  one-provider limits, no-retry/no-fallback/no-redispatch, canonical path and
  reparse guards, and the expected Process presence/shape gate are enforced
  before reservation, event creation, helper launch or ADB. The expected value
  is never read, printed, persisted, hashed, derived or passed in argv.
- Deterministic reservation uses create-new semantics. The concurrent race and
  rollback fixtures prove that one contender can reserve and the loser stops
  before helper/ADB; a failed directory creation does not leave a reusable
  reservation.
- Native suspended launch assigns the Job Object before resume. Timeout,
  overflow, drain failure and exception paths preserve process exit separately,
  close containment and verify synthetic parent/child/grandchild exit. No
  `Kill(Boolean)`, parent-only fallback or string-built `taskkill` path remains.
- stdout/stderr have independent byte caps and drain statuses. A first drain
  failure remains terminal even if a later bounded wait completes. The wrapper
  checks `CaptureErrorClass`, preserves the helper process exit and emits a
  typed wrapper result instead of converting it to PASS or masking it as the
  only outcome.
- PM-path classification is fail-closed and redacted; mutation cases cover
  truncation, mixed streams, duplicate/wrong paths and sensitive sentinels.
  No raw fixture payload is retained in receipts, QA or exceptions.
- Exporter, toolchain and SQLite bridge exact bytes reconstruct from a Git
  archive at `6faebf0ad82ec6f0efba3ef42f50fec9e7bf3a74`. No old command,
  manifest or decision is a fallback.

## QA evidence

`21/21 PASS`, binding `262/262`, regression `175/175`, DB host-readback
`56/56`, PowerShell 5.1 parse PASS, `git diff --check` PASS, derived severity
`0 BLOCKER / 0 HIGH / 0 MEDIUM / 0 LOW`. Runtime evidence includes suspended
Job Object attach-before-resume, `ALL_DESCENDANTS_EXITED`, drain-failure
process-exit preservation, independent `DRAIN_COMPLETED_AFTER_TIMEOUT`
statuses, and typed `STREAM_CAPTURE_FAILED` wrapper binding. ADB, device,
provider, credential, DB-write, build, install, RAW and redispatch counters are
all `0`.

## Decision boundary

The closed event
`D:\P5E-private\raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc`
and every older event, decision, command and manifest remain closed/consumed
and non-reusable. Luna does not approve a live event. The only next action is
owner review of this exact packet; no owner authorization request is created by
this offline work package and P6 is not ready.
