# P5E A4.3 — decision atomicity and expected-environment isolation manifest

Document status: `OFFLINE_PACKET_REVIEWABLE / NOT_AUTHORIZED / NOT_DISPATCHED / P6_NOT_READY`.

Work package: `P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_REPAIR`.
Packet identifier: `P5E-A43-DECISION-ATOMICITY-ENV-20260928-01`.

This is a new offline packet. It is not an owner authorization, does not
create a receipt, does not reserve an event, and is not dispatched. The only
permitted next action is review of this exact packet; no live event or P6
action is authorized by this document.

## Boundary and immutable history

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Implementation baseline supplied for this repair: `0730110a261edbab8f07f7db9e5831f567976fd3`.
- Actual worktree continuation was a later child; no reset or clean was used.
- Runtime/dependency archive baseline: `6faebf0ad82ec6f0efba3ef42f50fec9e7bf3a74`.
- Closed event remains immutable and non-reusable:
  `raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc`.
- The old command, old manifest, old decision and all earlier closed events
  are not fallback inputs. Their bytes are not reused by this packet.
- No ADB, device, provider, credential, endpoint/account operation, database
  write, build, install, RAW, GLOSSARY, retry, fallback, RECONCILE, restore,
  cleanup, redispatch or P6 action is in this work package.

## Fixed A4.3 scope and artifact pins

- Serial: `15e84958`.
- Production APK SHA-256:
  `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`.
- Production source ZIP SHA-256:
  `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348`.
- Production BUILD_INFO SHA-256:
  `DB20DA0CF708410AAAB65E5AF69ADF89A769ED62240B577A89CA4E2007FB7F06`.
- AndroidTest APK SHA-256:
  `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`.
- AndroidTest source ZIP SHA-256:
  `5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F`.
- AndroidTest BUILD_INFO SHA-256:
  `772F32E23AEF3537FEF00DACE8E4B9B994448BFEE2071150CF0E0540DDCCC5A9`.
- Certificate SHA-256:
  `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- Package/version/code: `com.ml.tblandroidtxt` / `4.17-p5e.11` / `207`.
- Test package/runner: `com.ml.tblandroidtxt.test` /
  `androidx.test.runner.AndroidJUnitRunner`.

## Exact runtime pins

| Component | Path | Length | SHA-256 |
|---|---|---:|---|
| Review-only command | `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt` | `PROCESS_SELF_HASH_REQUIRED` | `PROCESS_SELF_HASH_REQUIRED` |
| Approval manifest | `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md` | `PROCESS_PINNED_BY_COMMAND` | `PROCESS_PINNED_BY_COMMAND` |
| Live helper | `scripts/p5e-raw-live-supervisor.ps1` | 417661 | `3FE3B764C52DAF1520FE61901BC0861AFA229E4B3C4E775EAE878567106C0D16` |
| Runtime guard | `scripts/p5e-a43-runtime-guards.ps1` | 23180 | `C31217CDBD40F22DB9A74AFB529B1ECC33F725F485ECD462992073235F59ADD8` |
| Job/capture exporter | `scripts/p5e-db-binary-export.ps1` | 52379 | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| SQLite bridge | `docs/P5E_SQLITE_BRIDGE.py` | 5958 | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| Toolchain resolver | `scripts/p5e-raw-toolchain.ps1` | 9002 | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| `local.properties` | `local.properties` | 368 | `71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2` |

The command pins the exact manifest hash, helper, exporter, runtime guard,
bridge, toolchain, local properties, current APK/source/certificate pairs and
serial. It accepts no receipt or account value through argv.

## Atomic decision/receipt transaction

The receipt validator runs before reservation and requires the exact canonical
decision ID, packet, serial, scope, hashes, one-event/one-provider-call
limits, no-retry/no-fallback/no-redispatch flags and valid time window.

`decisionKey` is the full SHA-256 of the UTF-8 canonical decision ID. The
decision marker is `.decisions/<decisionKey>.reservation`; the receipt marker
is `.receipts/<fullReceiptSha256>.reservation`. Both use `CreateNew`, bounded
records and `Flush($true)`. A registry-wide scan rejects malformed, partial,
unreadable, reparse or foreign markers with
`P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP`.

The monotonic state is:

`NONE -> DECISION_CONSUMED -> RECEIPT_CONSUMED -> EVENT_RESERVED -> PLAN_BOUND`.

There is no rollback. A decision marker is consumed even if a later receipt
binding, event-directory creation or plan write fails. This intentional
fail-closed tradeoff is recorded and tested; no repair path makes a consumed
decision reusable. The event directory identity is derived from `decisionKey`,
never from a receipt-hash prefix. A same-decision race has one winner even
when receipt hashes differ. A same-receipt race has one receipt binding; a
losing different decision remains consumed and stops before helper/ADB.

## Expected-value environment isolation

The expected account digest is checked only for Process presence and exact
lower-case 64-hex shape before reservation. Its value is never printed,
persisted, hashed, derived, put in an exception or passed through argv.

Each helper receives an explicit phase environment. `PrepareEvent`, `Before`,
`After` and `Verify` are `EXPECTED_ABSENT`; only `Dispatch` is
`EXPECTED_PRESENT`. Receipt path/hash, command hash and expected digest are
cleared from non-authority child environments. Fixtures expose only boolean
presence and validation class. Sequential and concurrent launches are checked
for cross-contamination and all terminal paths dispose the environment/capture
state.

## Process and capture controls retained

The existing Windows Job Object containment and independent stdout/stderr
byte caps remain in force. The packet does not use `Process.Kill(Boolean)` as
tree-kill evidence, does not use input-built `taskkill`, and does not infer
success when descendants or pipe handles remain unverified. Process exit,
containment, drain status, timeout, overflow and bounded-capture status remain
separate fields. No raw helper output is retained in packet evidence.

## Offline QA and provenance

The RED evidence is preserved at
`docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_RED_20260928.json`.
The required GREEN suites are:

- atomic decision/receipt/crash/environment matrix: `28/28`, zero HIGH;
- binding tuple matrix: `262/262`;
- regression matrix: `175/175`;
- DB host-readback matrix: `56/56`;
- legacy runtime guard closure assertions: `21/21`;
- helper self-test, process-tree/capture, PM-path, path/reparse, secret scan,
  PowerShell 5.1 parse and `git diff --check`.

The two historical untracked QA scripts are eliminated from runtime
references. Their needed assertions are carried by the tracked package suites
`scripts/test-p5e-a43-binding-tuple-matrix.ps1` and
`scripts/test-p5e-a43-db-host-readback-matrix.ps1`. Archive-clean QA must run
from the exact candidate commit without workspace fallback or cached results.
The helper source-contract audit uses the historical commit when that object is
available and otherwise verifies the pinned AndroidTest source SHA-256
`B3F974A185F8B2590424471F3A1317EC436CFBEF3D92244EFAE1674F935AC04A` inside
the clean archive; it never treats a missing history object as a pass.

All live counters for this work package are zero: ADB, device, provider,
credential, database write, build, install, RAW and redispatch. P6 remains not
ready. Closed events and consumed decisions remain closed/consumed.

## Current state

- Offline packet: `REVIEWABLE` after final QA and independent review only.
- Owner decision: `NOT_AUTHORIZED`.
- Live dispatch: `NOT_DISPATCHED`.
- P5 exit: `NOT_CLAIMED`.
- P6: `NOT_READY`.
- Next action: complete archive-clean QA and independent exact-byte review;
  do not request or execute owner authorization in this repair package.
