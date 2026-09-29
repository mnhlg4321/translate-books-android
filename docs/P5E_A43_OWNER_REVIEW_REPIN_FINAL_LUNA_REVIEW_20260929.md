# P5E A4.3 owner-review repin — final Luna review

Review date: `2026-09-29`
Reviewer: `Luna`
Reviewed HEAD: `1f38c4e30baafd98001d2102d49aebabe2fb81de`
Branch: `feature/v4.18-p5e-runner-repair-20260917`
Runtime candidate: `a68d6ceb4e727bc67d1672cde3a284246d62e09e`
Mode: exact-byte, offline, read-only. No ADB, device, provider, credential,
database write, build/install, RAW, retry, fallback, RECONCILE, redispatch,
owner receipt or P6 action was performed.

## Verdict

`PASS / 0 BLOCKER / 0 HIGH / 0 MEDIUM / 0 LOW / OWNER_REVIEW_ONLY / NOT_DISPATCHED / P6_NOT_READY`

The two MEDIUM findings from the pre-sync review are closed. The current
authority set uses the repinned manifest and command, the archive and
provenance identify the exact runtime candidate, and the snapshot's only
remaining action is this final review. HEAD `1f38c4e3` changes only the
snapshot after canonical synchronization; it does not alter the reviewed
runtime packet.

## Exact pins

| File | Length | SHA-256 |
| --- | ---: | --- |
| `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md` | 8201 | `9F0928876963EBE00187A384008D10569C0667C18C54588C20A9BE0E9D30E200` |
| `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt` | 24220 | `A179433558798687452E061AEAE8C0D20E4D8C96AA2F6C9ADC05B0A573B656B1` |
| `scripts/p5e-raw-live-supervisor.ps1` | 417314 | `4DAD6E30928DD0D78396BDDFA57AFA1053E75481524D2EA05659227869C57444` |
| `scripts/p5e-a43-runtime-guards.ps1` | 23180 | `C31217CDBD40F22DB9A74AFB529B1ECC33F725F485ECD462992073235F59ADD8` |
| `scripts/p5e-db-binary-export.ps1` | 52379 | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| `docs/P5E_SQLITE_BRIDGE.py` | 5958 | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| `scripts/p5e-raw-toolchain.ps1` | 9002 | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |

The command's manifest pin matches the manifest hash. The helper and all
runtime dependency hashes match the same values in the canonical plan,
`BUILD_STATE.md`, proposal, checklist, owner-review provenance and clean
archive report. Historical files still retain old hashes as immutable prior
evidence; none is referenced as a current or fallback input.

## Findings and closure

### BLOCKER / HIGH / MEDIUM / LOW

No finding remains.

1. The prior canonical-pin finding is closed: current pins in
   `BUILD_STATE.md:5`, `EDITORIAL_RECOVERY_V4_18.md:5`,
   `docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md:5` and the release checklist all
   use manifest `9F092887...` and command `A1794335...`. The new provenance
   and archive report carry the same exact bytes.
2. The prior stale-next-action/provenance finding is closed: the archive
   report records candidate `a68d6ceb` with `PASS`; provenance records the
   same candidate and one owner-review next action; and
   `WORKSPACE_SNAPSHOT.md:11` and `:18` now require only this final review.
   No document asks for another repin, archive reconstruction or repair.

The snapshot line describing the `bf008fc7` baseline is consistent with the
repository snapshot policy: it identifies the implementation/evidence
baseline immediately before the final-review snapshot commit. It is not a
request to recommit or rerun the canonical synchronization.

## Evidence and historical failure coverage

The exact-candidate archive report records `PASS` with no workspace fallback
or cached results: main `21/21`, atomicity/expected environment `28/28` with
zero HIGH, binding `262/262`, regression `175/175`, DB host readback `56/56`,
and all live counters zero. The current provenance records the same reports,
candidate and counters.

The next work request reduces the loop to one owner choice and forbids a new
offline repair without new RED evidence. Its failure table and QA gates cover
the historical expected-value, route/layout, identity, toolchain, version,
WAL/SHM, missing-device-SQLite, binding-tuple, capture/PM-path, decision/receipt
race/crash and phase-isolation failures. It explicitly keeps `STOP_NO_EVENT`
and `APPROVE_ONE_FRESH_EVENT` separate and preserves no-retry behavior.

The manifest names both immutable closed events, and the helper rejects both
closed event directories before export. Consumed decisions and closed events
remain non-reusable. No RAW acceptance, P5 exit or P6 predecessor is claimed.

## Readiness

The packet is ready for owner review only:

`OWNER_REVIEW_ONLY / NOT_AUTHORIZED / NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.

Offline PASS does not authorize a receipt, event, command execution, provider
call, DB write or P6 transition.

## Exact next action

Record this final review's path/hash in the synchronized provenance and
canonical state, then owner reviews the exact repinned packet and chooses
`STOP_NO_EVENT` or separately approves exactly one fresh A4.3 event. Until
that owner decision, do not create or consume authorization, open an event,
execute the command, use ADB/device/provider/credential/database writes,
build/install, dispatch RAW, retry/redispatch, claim P5 exit or open P6.
