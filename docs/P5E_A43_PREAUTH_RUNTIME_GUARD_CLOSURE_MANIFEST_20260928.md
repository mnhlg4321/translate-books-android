# P5E A4.3 — preauthorization runtime-guard closure manifest

Document status: `OFFLINE_RUNTIME_GUARD_CLOSURE_PASS / OWNER_REVIEW_PENDING / NOT_AUTHORIZED / NOT_DISPATCHED / P6_NOT_READY`.

Work package: `P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE`.
Packet identifier: `P5E-A43-PREAUTH-RUNTIME-GUARD-CLOSURE-20260928-01`.

This is a new owner-review manifest. It is not an authorization, does not
create a receipt, does not reserve an event, and has not been executed. The
only next action after this offline packet is owner review; no live event is
opened by this work package.

## Boundary and immutable history

- Canonical plan: `EDITORIAL_RECOVERY_V4_18.md`.
- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Implementation baseline before this closure: `a30a4d1653c3a5a1e0d39fac4b07ae000d588edf`.
- Current work began from its direct child `d1c15a07d1ddbed2b105b7954920d72bc7002f76`.
- Closed event remains immutable and non-reusable:
  `raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc`.
- All earlier decisions, events, manifests and commands remain closed/consumed.
  The historical A4.3 command and manifest are never a fallback or input to
  this packet.
- No ADB, device, provider, credential, database write, build, install, RAW,
  retry, fallback, RECONCILE, cleanup, restore, redispatch or P6 action is
  permitted in this offline work package.

## Fixed A4.3 scope

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

## Exact packet pins

| Component | Path | SHA-256 |
|---|---|---|
| Approval manifest | `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_MANIFEST_20260928.md` | pinned by the command, not embedded in this self-describing file |
| Review-only command | `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_COMMAND_20260928.txt` | `PROCESS_SELF_HASH_REQUIRED` |
| Live helper | `scripts/p5e-raw-live-supervisor.ps1` | `86E386375B771B001B911FB2F538E112F6AD9F709BB933726FEB981D6F875FB6` |
| Job/capture exporter | `scripts/p5e-db-binary-export.ps1` | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| Runtime guard library | `scripts/p5e-a43-runtime-guards.ps1` | `F332954FB8AA2048EDF18630C5D1EF7B2039D57458C85CD6BB5CB07F59D96DBA` |
| SQLite bridge | `docs/P5E_SQLITE_BRIDGE.py` | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| Toolchain resolver | `scripts/p5e-raw-toolchain.ps1` | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| local.properties | `local.properties` | `71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2` |

The command pins the final manifest hash, all runtime bytes above, both
artifact/backup pairs, certificate and serial. The command does not embed a
receipt, account value, credential, endpoint or expected digest.

## Machine-enforced owner decision

The future owner receipt is a separate owner-controlled regular file outside
the repository. The command accepts only its canonical path and expected
SHA-256 through Process variables:

- `P5E_OWNER_DECISION_RECEIPT_PATH`
- `P5E_OWNER_DECISION_RECEIPT_SHA256`

The receipt schema is `p5e.a43.owner-decision-receipt.v1` and contains only
the decision id, packet id, serial, exact packet hashes, one-event and
one-provider-call limits, `RAW/GLOSSARY` scope, `noRetry`, `noFallback`,
`noRedispatch`, `issuedAt` and `expiresAt`. The validator rejects missing,
malformed, stale, future-issued, reused, mismatched, reparse or over-broad
receipts before event reservation or helper launch. It never repairs or
creates a receipt.

Before reservation the command also checks the Process-only expected account
value for presence and exact lower-case 64-hex shape. It never reads,
prints, stores, hashes, derives or passes that value through argv during the
pre-event gate. The value is still isolated from child processes until the
existing account-check contract needs it.

The deterministic reservation directory is created once with create-new
semantics from `decisionId` and receipt hash. A successful reservation is
consumed even if PrepareEvent or Before later stops. A race cannot create a
second directory, and a pre-reservation failure does not consume the
decision. `EVENT_PLAN.json` binds the decision id, receipt hash, packet id,
serial, scope and all one-shot/no-retry limits.

## Process containment and bounded capture

`scripts/p5e-db-binary-export.ps1` now exposes a Windows Job Object
containment contract with `KILL_ON_JOB_CLOSE`, attached immediately after
process start. Timeout, output overflow, capture drain failure and exception
paths terminate the Job Object, wait boundedly and verify that the parent,
child and grandchild synthetic processes have exited. Assignment or
verification failure is the terminal enum
`EXTERNAL_PROCESS_STATE_UNKNOWN`; no parent-only fallback is a pass.

The implementation does not use `Process.Kill(Boolean)` or a constructed
`taskkill` command. On Windows PowerShell 5.1/.NET Framework 4.8 the QA
records that the unsupported overload is absent and exercises Job Object
assignment instead.

Outer helper capture uses independent constants:

- stdout cap: `4194304` bytes;
- stderr cap: `1048576` bytes;
- bounded drain wait: `5000` ms.

The result keeps `processExitCode`, `containmentStatus`,
`containmentVerified`, `stdoutDrainStatus`, `stderrDrainStatus`, byte counts,
`timedOut`, `outputTooLarge` and `captureBounded` separate. It emits only
allowlisted enums, counts and booleans. It never writes or prints raw helper
stdout/stderr prefixes when a cap is exceeded, and a wrapper code such as
`125` cannot replace a known helper process exit or typed helper outcome.

## Dependency provenance

The previously untracked exporter and toolchain, and the modified SQLite
bridge, are preserved as exact tracked dependency bytes in the packet commit.
The dependency provenance records each path, length and SHA-256, plus the
implementation baseline and the commit that contains the exact bytes. No
`.idea`, owner worktree, artifact directory or unrelated file is part of the
dependency commit. A clean checkout/archive extraction check is offline and
must reproduce the three dependency hashes and parse the PowerShell/Python
sources.

## Offline QA acceptance

The closure QA must pass the four RED-to-GREEN fixtures (process-tree gap,
missing owner gate, missing expected-value gate, and uncapped outer capture),
receipt negative matrix, simultaneous reservation race, Job Object process
tree matrix, capture cap matrix, PM-path classifier matrix, helper self-test,
captured-export binding `262/262`, regression `175/175`, DB host-readback
`56/56`, dependency/hash order, command/manifest/helper binding,
path/reparse guards, expected-value isolation, secret scan, timeout/no-retry,
consumed-event rejection, PowerShell 5.1 parse and `git diff --check`.

Severity is computed from assertion failures and policy mapping. PASS requires
`0 BLOCKER / 0 HIGH`; any MEDIUM on decision binding, process containment or
capture cap is a stop and cannot produce an owner authorization request.
This packet contains no owner authorization request and no live result.

## State and next action

- Offline guard closure: PASS only after the exact QA and Luna reports pass.
- Owner decision: NOT AUTHORIZED; no receipt is present in the repository.
- Event: NOT DISPATCHED; closed events remain closed/consumed.
- P5 exit: NOT CLAIMED.
- P6: NOT READY.
- Next action: owner reviews this exact guard-closed packet; no dispatch.
