# P5E A4.3 offline PM-path/capture repair — Luna review

Review scope: exact final bytes of the offline repair packet only. This review
did not edit the worktree and performed no ADB, device, provider, credential,
database-write, build, install, event, RAW or redispatch action.

## Verdict

`CONDITIONAL PASS / 0 BLOCKER / 0 HIGH / OWNER_REVIEW_PENDING / NOT_DISPATCHED`.

The synthetic QA evidence supports `36/36`, binding `262/262`, regression
`175/175`, DB host-readback `56/56`, `eventCreated=false`, and all live-action
counters equal to zero. This is an offline review result only; it is not an
authorization and does not permit a new event.

## Exact bytes reviewed

| File | Byte length | SHA-256 |
|---|---:|---|
| `scripts/p5e-raw-live-supervisor.ps1` | 405223 | `959F2BBDC2EF163F00F3A56900B903DF529FDCDD9024AD6CEE906A01E080A8F0` |
| `scripts/test-p5e-production-version-contract.ps1` | 8503 | `1A6E0B08715494130A362633243CAB51969535E6EDE807C175FC3E67576FA406` |
| `scripts/test-p5e-a43-pm-path-capture-repair.ps1` | 42575 | `F7E99CF9F52EC733625D5D0B312C966792B40AF63425026D368A771A816246CB` |
| `docs/P5E_A43_PM_PATH_CAPTURE_REPAIR_MANIFEST_20260928.md` | 11008 | `C08C3F6D8EE1B802B0D68AB1FF0302E82655D17C72B06619752B65396C11ADC3` |
| `docs/P5E_A43_PM_PATH_CAPTURE_REPAIR_COMMAND_20260928.txt` | 22127 | `C641F6A01C01209AD08FB820871E575895DC998A4CB6AD15769F1D2222BA4CD5` |
| `docs/P5E_A43_PM_PATH_CAPTURE_REPAIR_QA_20260928.json` | 5231 | `92C3539069A8F285250F7F18AC154C2E85643A229BA449813C1D830099D0CEE7` |

The PM classifier is fail-closed: package-not-found requires an exact
package-specific message, device/PM failures take precedence, and unknown
nonzero cases remain `UNKNOWN_NONZERO`. The command log retains only the safe
classification and bounded metadata, not raw stdout, stderr, argv or device
path.

## Non-blocking findings

- `MEDIUM`: descendant cleanup is not fully guaranteed when the wrapper falls
  back from recursive process termination to parent-only termination; the
  synthetic fixture preserves the known process exit and reports bounded
  capture status, but does not prove every descendant handle is gone.
- `MEDIUM`: the review-only command enforces its self-hash but does not itself
  machine-enforce a new owner-decision token before a future live event. The
  manifest and command boundary still state that the packet is not an
  authorization and must not be dispatched without a new owner decision.
- `LOW`: outer asynchronous capture has no independent byte cap; helper output
  is not persisted or printed, but can exist transiently in memory.
- `LOW`: QA severity fields are report metadata rather than an independent
  severity-analysis engine; the exact Luna review records the findings here.

These findings do not change the required result because there are no blocker
or high findings, but they must remain visible to the owner.

## Boundary and next action

Closed event
`raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc`
remains immutable and was not reused. Its three allowed evidence hashes are
`EVENT_PLAN.json=1C163DC499D934F25B5E4C5EAB911543773D01D333CF7990D0D57DF4D4E9CBE4`,
`COLLECTOR_OUTCOME.json=7674858E7D3164EED7873A492AB88742F2AB90A162E71E5210F60CCEAF84A5BC`,
and `COLLECTOR_COMMAND_LOG.jsonl=B90CEAB274D510B37D09910313E18E35F4F39AC6FE7F76B0019ABF1B8020826C`.

Next action: owner reviews this repaired offline packet. Do not run the
command, open a new event, reuse the consumed decision, retry/redispatch,
claim P5 exit, or open P6.
