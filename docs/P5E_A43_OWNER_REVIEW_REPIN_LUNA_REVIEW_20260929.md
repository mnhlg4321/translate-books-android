# P5E A4.3 owner-review repin — Luna offline review

Review date: `2026-09-29`  
Reviewer: `Luna`  
Candidate commit: `a68d6ceb4e727bc67d1672cde3a284246d62e09e`  
Branch: `feature/v4.18-p5e-runner-repair-20260917`  
Mode: exact-byte, offline, read-only. No ADB, device, provider, credential,
database write, build/install, RAW, retry, fallback, RECONCILE, redispatch,
owner receipt or P6 action was performed.

## Verdict

`CONDITIONAL PASS / 0 BLOCKER / 0 HIGH / 2 MEDIUM / 0 LOW / OWNER_REVIEW_ONLY / NOT_DISPATCHED / P6_NOT_READY`

The repinned manifest and command are mutually consistent and the supplied
archive-clean result for `a68d6ceb` is accepted as the reported offline test
result. The candidate is suitable for a human owner review of the exact
packet. It is not ready for owner authorization or P6 because the committed
canonical/provenance documents still identify the superseded packet and the
snapshot still points to work already completed by this candidate.

## Exact packet pins

| File | Length | SHA-256 |
| --- | ---: | --- |
| `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md` | 8201 | `9F0928876963EBE00187A384008D10569C0667C18C54588C20A9BE0E9D30E200` |
| `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt` | 24220 | `A179433558798687452E061AEAE8C0D20E4D8C96AA2F6C9ADC05B0A573B656B1` |
| `scripts/p5e-raw-live-supervisor.ps1` | 417314 | `4DAD6E30928DD0D78396BDDFA57AFA1053E75481524D2EA05659227869C57444` |
| `scripts/p5e-a43-runtime-guards.ps1` | 23180 | `C31217CDBD40F22DB9A74AFB529B1ECC33F725F485ECD462992073235F59ADD8` |
| `scripts/p5e-db-binary-export.ps1` | 52379 | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| `docs/P5E_SQLITE_BRIDGE.py` | 5958 | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| `scripts/p5e-raw-toolchain.ps1` | 9002 | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |

The command pins the current manifest at line 29 and the manifest records the
same owner-review-only state at lines 143–152. No stale hash occurs inside the
manifest/command pair. The helper rejects both closed event names at
`scripts/p5e-raw-live-supervisor.ps1:279-281` and `:1919-1920`.

## Findings

### MEDIUM — canonical current pins are still the superseded packet

`BUILD_STATE.md:5`, `EDITORIAL_RECOVERY_V4_18.md:5` and
`docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md:5` still advertise manifest
`D8F5BECE...` and command `52B961DE...`, while the executable packet in this
candidate is `9F092887...` and `A1794335...`. The same old values remain in
the committed provenance, archive-clean report, prior Luna review and
preauthorization QA report. A reviewer following canonical state therefore
loads a packet that fails the current command's self/manifest hash gate. This
is fail-closed, but it creates the exact stale-authority loop this repin was
intended to remove and must be repaired before any owner decision.

### MEDIUM — candidate identity and snapshot evidence are one commit behind

`docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_PROVENANCE_20260928.json`,
`docs/P5E_A43_ARCHIVE_CLEAN_RECONSTRUCTION_20260928.json` and the prior Luna
review still name candidate `5597a3b20a4a1614c411c71872ee1333196cbe9c` and the
old manifest/command bytes. `WORKSPACE_SNAPSHOT.md:11` and `:18` still say to
commit the repin and then run archive reconstruction/review, although
`a68d6ceb` is already that repin commit and the requested archive-clean run is
reported PASS. The checked-in evidence therefore does not yet provide one
durable, exact-a68 provenance chain. This is a documentation/provenance gap,
not a runtime bypass, but it prevents a clean canonical handoff.

### BLOCKER / HIGH / LOW

No blocker, high or low finding was established. The exact packet's runtime
controls are coherent; the two findings above are the only material defects
found in this candidate.

## Historical failure coverage and closed-event boundary

The reported offline suites cover the previously demonstrated failures:

- decision/receipt one-use, same-decision and same-receipt races, crash-window
  consumption and expected-value phase isolation: `28/28`, zero HIGH;
- runtime guard, process-tree/capture, PM-path classification, path/reparse,
  secret scan and no-retry/no-redispatch controls: `21/21`;
- binding tuple: `262/262`; regression: `175/175`; DB host readback: `56/56`;
- archive-clean reconstruction for the exact `a68d6ceb` candidate: reported
  `PASS`; live counters: ADB/device/provider/credential/database-write/
  build-install/RAW/redispatch all `0`.

The current manifest names both immutable closed events and explicitly forbids
reuse, and the helper rejects both names before database export. Consumed
decisions and closed events remain non-reusable. These controls are clear and
closed; they do not establish RAW acceptance, P5 exit or P6 readiness.

## Readiness

`OWNER_REVIEW_ONLY / NOT_AUTHORIZED / NOT_DISPATCHED / P5_EXIT_NOT_CLAIMED /
P6_NOT_READY` is the only supported state. The packet is reviewable by the
owner after canonical synchronization. It is not a live authorization and it
cannot advance to P6 from offline QA alone.

## Exact next action

Synchronize `BUILD_STATE.md`, `EDITORIAL_RECOVERY_V4_18.md`,
`docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md`, packet provenance, archive-clean
evidence, prior-review references and `WORKSPACE_SNAPSHOT.md` to candidate
`a68d6ceb` and the exact current pins above in one documentation-only repair;
make the snapshot's single next action owner review of this exact packet.
Then the owner may review the synchronized packet. Do not create or consume a
receipt, open an event, execute the command, use ADB/device/provider/credential/
database writes/build/install/RAW/retry/redispatch, claim P5 exit or open P6.
