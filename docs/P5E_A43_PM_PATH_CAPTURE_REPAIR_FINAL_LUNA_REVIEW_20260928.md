# P5E A4.3 PM-path/capture repair — independent final Luna review

Review date: `2026-09-28`.

Review scope: the committed offline repair packet at
`a30a4d1653c3a5a1e0d39fac4b07ae000d588edf`, its exact runtime dependencies,
the canonical state documents, and the prior closed event boundary. This
review performed no ADB, device, provider, credential, database-write,
build/install, live-event, RAW, retry or redispatch action. The frozen manifest
and command were not edited.

## Verdict

`OFFLINE_REPAIR_BEHAVIOR_PASS / LIVE_PACKET_NOT_READY / 0 BLOCKER / 1 HIGH / 2 MEDIUM / 2 LOW`.

The PM-path classifier and the separation of helper process exit from stream
drain state are supported by the recorded `36/36` synthetic QA, binding
`262/262`, regression `175/175`, and DB host-readback `56/56`. The exact packet
and artifact hashes independently match the advertised values. This closes
the original classification and exit-code-masking defects offline.

It does not yet make the packet safe to authorize for another live event. The
outer Windows PowerShell 5.1 timeout path has an unclosed process-tree risk,
and the exact runtime bundle is not reconstructible from the recorded commit.
Those issues should be closed in one offline package before another owner
decision is requested.

## Exact bytes independently checked

| Input | SHA-256 |
|---|---|
| Repair manifest | `C08C3F6D8EE1B802B0D68AB1FF0302E82655D17C72B06619752B65396C11ADC3` |
| Review-only command | `C641F6A01C01209AD08FB820871E575895DC998A4CB6AD15769F1D2222BA4CD5` |
| Helper | `959F2BBDC2EF163F00F3A56900B903DF529FDCDD9024AD6CEE906A01E080A8F0` |
| Binary exporter | `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06` |
| SQLite bridge | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| Toolchain resolver | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| QA report | `92C3539069A8F285250F7F18AC154C2E85643A229BA449813C1D830099D0CEE7` |
| Repair result | `11B9E5D7FBFE8301537529BFE79767693D853019560D1A2314080052D70104CD` |
| Repair provenance | `0FDA0BFE80F34023B3B03D42AD7582A0F4706D05628D1654884BE5544DDE2BB1` |
| Existing Luna review | `CD5447A8053EB96DB5EC38D3E0952795F61EE4B15E106757E72EA6413EBEF9EA` |
| Production APK artifact/backup pair | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` |
| AndroidTest APK artifact/backup pair | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |

The current branch is
`feature/v4.18-p5e-runner-repair-20260917`; actual `HEAD` is
`a30a4d1653c3a5a1e0d39fac4b07ae000d588edf`. The manifest and snapshot use
`b8326f5b8c9649c741e4c2124efb35a6a1045919` as the implementation baseline
immediately before the packet commit. That baseline/packet distinction is
internally explainable and is not itself a defect.

## Findings

### HIGH — outer timeout does not prove descendant termination on the required host

The review-only command attempts `$process.Kill($true)` and falls back to
`$process.Kill()`. On the required Windows PowerShell 5.1 host, backed by .NET
Framework 4.8, `System.Diagnostics.Process` exposes only `Void Kill()`; the
tree-kill overload is unavailable. The fallback therefore proves termination
of only the immediate helper PowerShell process. It does not prove termination
of a descendant ADB, instrumentation, Java/apksigner, Python, or provider-bound
process.

This matters at the live boundary: an outer timeout could be reported while a
descendant continues after the wrapper has entered After/terminal handling.
That would weaken the one-event/one-dispatch/no-redispatch contract. The helper
dependency already contains a `taskkill /PID ... /T /F` fallback for its inner
capture, but the outer command does not use an equivalent verified contract.

Required closure: implement an explicit process-tree termination result with
the termination mechanism, attempted PID, exit status, parent-exit status and
a bounded post-termination verification. Test both a cooperative child and a
grandchild that keeps a redirected stream open. A failed or unproven tree
termination must become terminal `EXTERNAL_PROCESS_STATE_UNKNOWN`; no After
interpretation may claim the external action has stopped.

### MEDIUM — exact runtime dependency bundle is not durable at the recorded commit

At `HEAD a30a4d16`, `scripts/p5e-db-binary-export.ps1` and
`scripts/p5e-raw-toolchain.ps1` are untracked, while
`docs/P5E_SQLITE_BRIDGE.py` is modified relative to its tracked blob. Their
working-tree bytes do match the packet pins, so the current hash guards are
effective. However, the recorded commit cannot reconstruct the exact runtime
bundle on a clean checkout. This weakens provenance and makes accidental
workspace cleanup another source of packet churn.

Required closure: preserve the exact pinned dependency bytes in Git or in one
immutable, hash-bound source archive, without staging unrelated owner changes.
Record the durable commit/archive identity separately from the pre-packet
implementation baseline. Recompute downstream packet hashes only if any byte
actually changes.

### MEDIUM — authorization and expected-value readiness are not machine-gated before event creation

The command self-hash is enforced, but a future owner-decision token is not.
The command also deliberately does not check the process-only expected account
value before creating the event directory and starting the Before collector.
Consequently, an accidental direct launch or a missing Process value can
consume operator time and create another pre-dispatch event before the missing
authority/input is detected.

Required closure: use a small, hash-bound launcher/precheck that validates a
new decision identifier/digest and checks only the presence and shape of the
expected Process value before event creation or ADB. It must not read, print,
persist, derive or transmit the value. The decision must bind the final
manifest, command, helper, exporter, bridge, toolchain, serial and launcher
hashes. A missing/mismatched gate stops with event count and ADB count `0`.

### LOW — outer capture has no independent byte ceiling

The outer `ReadToEndAsync()` retains the helper streams in memory without its
own byte cap. The helper output is expected to be small and is not printed or
persisted, but a malfunction can still cause avoidable memory growth. Add a
bounded stream reader or reuse the already bounded capture primitive.

### LOW — severity counts are self-reported metadata

The QA report's blocker/high fields do not come from an independent finding
engine. They should be derived from the final review artifact or treated as
descriptive fields only. This does not invalidate the behavioral test results.

## Prior failure coverage

The reviewed classifier covers the historical ambiguity among package absence,
device unavailable, package-manager failure, malformed response and unknown
nonzero output. Exact package identity is required before a path is accepted;
unknown nonzero remains fail-closed. The outer wrapper now records process exit,
timeout, stdout/stderr drain and boundedness independently, so wrapper code
`125` no longer erases the helper exit evidence.

The next repair must preserve all earlier closures: AOSP signature wrapper,
account class/method identity, toolchain resolution, versionName/code gates,
binary DB export without Android `sqlite3`, WAL/SHM semantics, binding tuple,
artifact/certificate parity, secret redaction, no retry and no redispatch.

## Readiness and single next action

- Offline PM-path/capture defect: `PASS`.
- New owner authorization request: `NOT READY` until the HIGH finding and
  durable dependency provenance are closed.
- A4.3 live acceptance: `NOT PROVEN`.
- P5 exit: `NOT CLAIMED`.
- P6: `NOT READY`.

Single next action: complete one offline `A43_OUTER_PROCESS_TREE_AND_AUTHORITY_GATE_CLOSURE`
work package. It must add verified process-tree termination, pre-event
decision/expected-value gates, and durable exact dependency provenance; rerun
the targeted RED/GREEN matrix plus binding `262/262`, regression `175/175`, DB
`56/56`, PowerShell 5.1 parse, secret scan and `git diff --check`; then repin
one final owner-review packet. Do not request owner authorization, execute the
current command, open an event, use ADB/device/provider/credential/DB writes,
retry/redispatch, claim P5 exit or open P6 during that closure.
