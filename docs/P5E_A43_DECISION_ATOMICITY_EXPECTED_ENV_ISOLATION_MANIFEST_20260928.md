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
- The earlier closed event rejection remains immutable and non-reusable:
  `raw-live-20260925-093707011-cc71e9e18029485c8e2411698c88f586`.
- The old command, old manifest, old decision and all earlier closed events
  are not fallback inputs. Their bytes are not reused by this packet.
- No ADB, device, provider, credential, endpoint/account operation, database
  write, build, install, RAW, GLOSSARY, retry, fallback, RECONCILE, restore,
  cleanup, redispatch or P6 action is in this work package.

## Fixed A4.3 scope and artifact pins

- Serial: `15e84958`.
- Production APK SHA-256:
  `4F3DAF9C77DAD104A57CC54536A237D09B941610600A60A5876BD608C168D41C`.
- Production source ZIP SHA-256:
  `CA39E984CD3AEB0F59582CB7E6BA6EFFDE83792D6C8DE945C216EEA60F81B4C3`.
- Production BUILD_INFO SHA-256:
  `561C5211774BF339928FB919327F2E24CF48FF9F93338D6B514E5D27E5B8EE75`.
- AndroidTest APK SHA-256:
  `8F3329631DA2B35C66412B480CCD0C93F1CF6C52BE7D1B6FD21FBE3FE1C43566`.
- AndroidTest source ZIP SHA-256:
  `49BEDB483F0A6F63ADE266B2AE5515042EDF5A2FC96E1F98F9BC18A5E88A3BCB`.
- AndroidTest BUILD_INFO SHA-256:
  `02E43140C3867137724AC38915370A4CB02C662CDCDD873A66F97743411D9B37`.
- AndroidTest build: source commit `ed54d2b6c5ba75128c18f286d5a85186520744e7`,
  event `p5e-a43-prod209-20261001-01`, built with
  `scripts/build-and-save-android-test.ps1` and archived in
  `artifacts/test-builds` and `backup/test-builds` with matching hashes. It
  replaces the `058BE851…` APK, which compiled in the old DB hash. The
  instrumented test now takes the expected DB hash from the launch argument
  `p5e_expected_db_sha256` (pinned in the helper) and compares it with the real
  DB file hash. The Live test source hash pinned in the helper is
  `2CFC25A182E966CA2A0D003F40D7D53C23AAE07B943FE7B6B688C7D672FB4D4B`.
- Certificate SHA-256:
  `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- Package/version/code: `com.ml.tblandroidtxt` / `4.18-p5e.1` / `209`.
- Test package/runner: `com.ml.tblandroidtxt.test` /
  `androidx.test.runner.AndroidJUnitRunner`.
- Device DB main-file SHA-256 (pre-live Before gate):
  `8D084050974E0681BF05AE46D799DB8741BB2593FFEFE92B2D12B5920FDFE685`,
  18,952,192 bytes, schema 24, observed read-only on 2026-10-01. It replaces the
  earlier pin `3563F44B…` (the DB file hash changed on 2026-09-26 and
  2026-10-01 while the fresh-tuple content and zero-lineage counts did not).
  A mismatch at Before stops the event before any provider call.

## Exact runtime pins

| Component | Path | Length | SHA-256 |
|---|---|---:|---|
| Review-only command | `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt` | `PROCESS_SELF_HASH_REQUIRED` | `PROCESS_SELF_HASH_REQUIRED` |
| Approval manifest | `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md` | `PROCESS_PINNED_BY_COMMAND` | `PROCESS_PINNED_BY_COMMAND` |
| Live helper | `scripts/p5e-raw-live-supervisor.ps1` | 417228 | `EB6BEAA5C418D5351A9516083A62DA9BF9F0E89B7C612DBB1FDF7E6FA0477A96` |
| Runtime guard | `scripts/p5e-a43-runtime-guards.ps1` | 23743 | `5F78F59FCFE5FD7BB904C8D0B4815CA7C714E03999DB97CDCA2737E5F4C3DC3E` |
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
The exact receipt schema is `p5e.a43.owner-decision-receipt.v1`; the live
command checks this literal in the manifest before it reads or consumes any
owner receipt.

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
byte caps remain in force: stdout `4194304` bytes and stderr `1048576` bytes.
The packet does not use `Process.Kill(Boolean)` as
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
`2CFC25A182E966CA2A0D003F40D7D53C23AAE07B943FE7B6B688C7D672FB4D4B` inside
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
- Next action: owner review of this exact offline packet only. Do not create
  or consume authorization, open a live event, dispatch RAW/GLOSSARY or start
  P6 from this repair package.
