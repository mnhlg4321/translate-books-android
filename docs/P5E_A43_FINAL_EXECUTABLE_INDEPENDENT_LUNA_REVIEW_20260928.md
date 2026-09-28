# P5E A4.3 final executable packet — independent Luna review (2026-09-28)

## Verdict

`STOP_NOT_READY_FOR_OWNER_AUTHORIZATION / 0_BLOCKER / 1_HIGH / 1_MEDIUM / 0_LOW / NOT_DISPATCHED / P6_NOT_READY`

The packet is internally hash-consistent and the process-tree, bounded-capture, archive-reconstruction, and dependency-durability controls pass this review. It is not ready for owner authorization because the one-use authorization boundary can be bypassed before `EVENT_PLAN.json` exists.

This review was performed offline only. ADB, device, provider, credential, database write, build/install, RAW, redispatch, and live-event counters remain `0`.

## Exact inputs independently verified

| Component | SHA-256 |
|---|---|
| Approval manifest | `B093AB871072D78A9A935914AEC9FF56835606BB344F034AA5EE2F38EA83FF4F` |
| Executable command | `FA63C5D2498AA345C6AC426AD253B466D6BD3206D525BAE4E0E8E6158BA78F80` |
| Live supervisor/helper | `86E386375B771B001B911FB2F538E112F6AD9F709BB933726FEB981D6F875FB6` |
| Binary exporter | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| Runtime guard | `F332954FB8AA2048EDF18630C5D1EF7B2039D57458C85CD6BB5CB07F59D96DBA` |
| SQLite bridge | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| Toolchain resolver | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| Packet QA script | `393196B7DC9C7BBA98A319DE1828E3CD6641A091946232C420B63556CF48B9EB` |
| Packet QA report | `9E461ADF9471B9B836B506079BCD34F9D01805E66FD07E29DD37E776F929F393` |
| Packet provenance | `4F607DFC3180E697AAE889C3A44B7ADCACD41396C203E4939F0E7D9ACC96FA8A` |

The nine packet/dependency files required for reconstruction are present in the Git archive of implementation commit `6faebf0ad82ec6f0efba3ef42f50fec9e7bf3a74`. JSON parsing and `git diff --check` pass.

## Findings

### HIGH — the same decision ID can reserve more than one event when the receipt hash changes

`Reserve-P5EA43OwnerDecisionEvent` derives the reservation path from both `decisionId` and the first 16 characters of the receipt hash. `Test-P5EA43ConsumedDecision` checks the exact current marker and scans event directories, but an event directory without `EVENT_PLAN.json` is skipped. That gap is normal immediately after reservation and can persist after a host interruption before `PrepareEvent` writes the plan.

An offline Windows PowerShell 5.1 reproduction called the frozen runtime guard twice with:

- the same `decisionId`;
- receipt hash `aaaa...aaaa` on the first call;
- receipt hash `bbbb...bbbb` on the second call;
- no `EVENT_PLAN.json` between calls.

Both calls were accepted and created distinct event directories and distinct `.reservation` markers. This violates the packet claim that one owner decision can authorize exactly one event and means the existing `21/21` QA result has a false-pass gap. The existing tests cover same-ID/same-hash reuse and concurrent same-hash contenders, but do not cover same-ID/different-hash reuse or interruption after reservation and before plan creation.

Required offline repair:

1. Make reservation uniqueness atomic on a normalized cryptographic identity of `decisionId`, independent of receipt hash.
2. Keep a separate binding from that decision reservation to the exact receipt hash; reject a different hash for the same decision ID.
3. Also prevent one receipt hash from being reused under a different decision ID.
4. Preserve reservation markers across interruption; absence of `EVENT_PLAN.json` must remain consumed/fail-closed.
5. Add negative and concurrent tests for same-ID/different-hash, different-ID/same-hash, interruption after reservation-before-plan, and cross-hash contenders.

### MEDIUM — expected account digest is inherited by helper phases that do not need it

The command validates the owner receipt and expected Process value before reservation/event/ADB, which passes. However, `Remove-DecisionEnvironment` clears receipt/command variables but deliberately retains `P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT`. The helper processes for `PrepareEvent`, Before collection, After collection, and Verify therefore inherit the digest even though only the account-check/Dispatch phase needs it.

This conflicts with the manifest's phase-isolation claim and expands exposure across child processes. Use phase-specific environment construction: clear the expected digest for PrepareEvent, Before, After, and Verify; expose it only to the single Dispatch/account-check process; clear it again on terminal exit.

## Controls that passed

- Exact packet and dependency pins match the advertised values.
- Receipt and expected-value gates execute before reservation, event creation, helper launch, or ADB.
- The suspended-process launcher attaches the child to a Job Object before resume and terminates the contained process tree on timeout/failure.
- stdout/stderr capture is bounded and redacted at the outer layer.
- Frozen closed events are not selected for reuse by the command path reviewed.
- Exporter, toolchain resolver, SQLite bridge, runtime guard, command, manifest, QA, and provenance are reconstructable from the pinned Git archive.
- No code path reviewed authorizes P6 automatically.

## State consistency

The canonical plan, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, checklist/proposal banners, packet QA, provenance, and earlier Luna review currently state `0 HIGH` and packet readiness. The HIGH reproduction above makes those readiness claims stale. Do not request owner authorization from the current hashes and do not open a live event.

## Single next action

Open one bounded offline repair work package for atomic decision-ID/receipt one-use enforcement and phase-specific expected-value isolation. Add the four missing negative/concurrency cases, rerun the existing full offline QA/regression/DB suites, obtain an independent review, and then repin and synchronize the packet and canonical state. No ADB, device, provider, credential, DB write, build/install, RAW, owner authorization request, or P6 action is permitted in that repair package.
