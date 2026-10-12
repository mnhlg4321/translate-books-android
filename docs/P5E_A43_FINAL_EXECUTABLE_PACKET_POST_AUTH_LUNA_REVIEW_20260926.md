# P5E A4.3 final executable packet — post-authorization readiness review

Review date: `2026-09-26`

Reviewer: `Luna (independent offline audit)`

Scope: final executable packet, exact disk hashes, owner-approval boundary,
post-authorization sequence, canonical state consistency, and recurrence
controls for the previously observed fail-closed stops.

Execution boundary: read-only review. This review did not run the command,
ADB, device, provider, credential, database write, build, install, RAW or P6.

## Verdict

`PASS / 0 BLOCKER / 0 HIGH / 1 MEDIUM EXPLAINED / 2 LOW NON-BLOCKING / OWNER_DECISION_RECEIVED_NOT_CONSUMED`

The owner sent the exact approval sentence in the current session. The
decision is valid for one fresh event on serial `15e84958` under the five
runtime hashes below, and it is recorded as received but not consumed. No
event is open, A4.3 has not been issued, RAW has not run, P5 exit is not
claimed and P6 is not ready.

The 1 MEDIUM recorded by the earlier exact-byte review is a branch/HEAD
documentation discrepancy, not a hash mismatch. The packet was frozen against
parent `2b34266eb60e3439864c80aae5d30ae141cfe8aa`; the current branch remains
`feature/v4.18-p5e-runner-repair-20260917`, and current HEAD is
`cbe820deea077ffd3ef920959bfbccb396f2455a`, whose parent is that freeze
baseline and whose commit contains the final packet documents. This is safe to
carry into owner review because the runtime pins were independently rehashed
from disk and remain unchanged. The stale sentence in the earlier review that
reported current HEAD as `2b34266` must not be used as a current-state claim.

## Exact-pin audit

The following final provenance references were independently recomputed from
the current disk and all matched:

| Input | SHA-256 | Result |
|---|---|---|
| Final approval manifest | `23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053` | PASS |
| Final executable command | `C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3` | PASS |
| Runtime helper | `17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E` | PASS |
| Binary exporter | `D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06` | PASS |
| Host SQLite bridge | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` | PASS |
| Toolchain resolver | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` | PASS |
| `local.properties` | `71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2` | PASS |
| Final packet QA script | `BACC1F7676DF394822BAAAD135826CA1B38CD55EC222368EE661D7E50AE46BDE` | PASS |
| Final packet QA report | `F967E73CB8FC6939E6ED8C9D13584150A65AF013F8D1BF0B43EC21B0852A76BF` | PASS |
| Earlier exact-byte Luna review | `05B11BB5C174D4DB24C9E5E7472396AFFCEC8AFB83A7D75B9F34E2E3601C24D7` | PASS |

Both production APK artifact sides hash to
`2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`; both
AndroidTest artifact sides hash to
`058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8`.
The QA report remains synthetic-only `14/14 PASS`, with binding `262/262`,
regression `175/175`, DB host-readback `56/56`, and all live counters zero.

The hash audit independently addresses and controls the two low findings without changing packet bytes:

- The helper's historical default manifest values are not authoritative for
  this packet; the command supplies the new values and checks the helper,
  exporter and bridge pins before access.
- The QA script's command-hash assertion is a weak self-reference, but the
  command hash above was independently computed from disk and matches the
  manifest, owner request and provenance.

Neither LOW requires a packet rewrite or blocks owner review.

## Authorized decision and provenance

The authorized follow-on files are internally consistent with the final
packet:

- Work request
  `docs/P5E_A43_AUTHORIZED_SINGLE_EVENT_NEXT_WORK_REQUEST_20260926.md` —
  SHA-256 `6CE3B8EB6036C481FE4C7972DC279D34B7ED0784B6754EE820B9DFD39CC38301`.
- Decision provenance
  `docs/P5E_A43_AUTHORIZED_SINGLE_EVENT_PROVENANCE_20260926.json` — SHA-256
  `6B32A3CD02522DF0F9389A43EBE0BA08A776CDCCB90F4F97AA20ABFB60B2D3A5`.

The provenance records `received=true`, `consumed=false`, one fresh A4.3
event, serial `15e84958`, and the exact same manifest, command, helper,
exporter and bridge hashes audited above. The owner approval is therefore a
received decision, not a template or a second approval request. The
owner-controlled expected account digest remains process-only; this review did
not read or store it.

## Owner decision validity

The sentence under `Copyable owner approval sentence` in
`docs/P5E_A43_FINAL_EXECUTABLE_OWNER_AUTHORIZATION_REQUEST_20260926.md` was
sent verbatim by the owner in the current session. It includes the serial,
manifest, command, helper, exporter and bridge hashes, one-event scope,
allowlisted RAW/GLOSSARY boundary and the no-retry/no-fallback/no-redispatch
boundary. It is now a valid received decision and must be consumed at most
once by the authorized work request.

The command and manifest hashes transitively bind the toolchain, local
properties, artifacts, certificate and allowlist. The owner does not need to
send those secret or environment-local values to the agent. The expected
account digest remains process-only and owner-controlled. The older
`P5E_A43_OWNER_DECISION_SINGLE_EVENT_*` files reference the consumed previous
packet and are historical evidence; they are not a second approval path.

## Required sequence for the authorized event

1. Recheck the recorded approval against all five runtime hashes, serial
   `15e84958`, one-event scope, one-call cap and fail-closed boundary. The
   decision is already received; do not ask for it again.
2. Recheck the branch, current HEAD relationship and dirty-worktree policy.
   Do not reset, clean, stage, rebuild, install, or update a hash to match a
   changed file.
3. Rehash every command and manifest dependency, both artifact/backup sides,
   and the certificate identity. Any change is terminal
   `OWNER_PACKET_BYTES_CHANGED`; close the pending request and prepare a new
   packet instead of retrying under the old decision.
4. In the owner's exact Windows PowerShell 5.1 process, load only the public
   command hash and the owner-controlled expected account digest. Accept only
   the loader PASS signal; never echo, persist, screenshot or log the digest.
5. Invoke the exact command once. It must create one fresh event directory and
   reject every consumed event, old command, old manifest and old decision.
6. Let PrepareEvent verify all pins and the dependency/hash order before any
   account, device, provider or database access. A pin, path, reparse,
   package, certificate, version, toolchain or event-identity failure is a
   typed terminal stop with no retry.
7. Let the Before collector complete only through the pinned binary export and
   host SQLite bridge. Preserve WAL/SHM exit-`1` as valid `ABSENT`; treat
   timeout, launch failure, malformed output, unstable snapshot, hash mismatch
   or any other nonzero result as a typed stop. Never call live Android
   `sqlite3`, checkpoint, delete, restore or write to the device.
8. If Before fails, record the redacted typed result and zero downstream
   counters, then close the event. Do not read native stderr/provider payload,
   diagnose by extra ADB, or redispatch.
9. Only after Before passes, perform the memory-only account and route
   comparison. Missing expected value, route mismatch or account mismatch
   stops before authorization/provider access. Do not substitute a new key or
   retry in the same event.
10. Only after all gates pass, create one fresh authorization and make at most
    one primary RAW/GLOSSARY call. DRAFT and PRONOUN remain hidden; no schema
    repair, fallback, RECONCILE, cleanup or restore is allowed.
11. Always run the pinned After and Verify stages once in the command's
    failure-safe path. Classify missing or inconsistent readback as
    `NOT_PROVEN`/`UNKNOWN`; never infer acceptance from an exit code or a
    provider response alone, and never redispatch.
12. Hash only redacted allowlisted receipts, update the canonical plan,
    `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, checklist and proposal once,
    and leave exactly one Next action matching the terminal outcome. RAW
    acceptance requires committed result, exact report/receipt evidence and
    verifier PASS; it does not itself open P6.

## Anti-loop and run-once matrix

| Stage | Maximum for this decision | Terminal-failure behavior | Forbidden continuation |
|---|---:|---|---|
| Freeze/hash/path checks | 1 pre-event pass | `AUTHORIZED_PACKET_PIN_MISMATCH` or typed guard stop before ADB | no hash substitution, repair or second decision |
| Event directory / PrepareEvent | 1 fresh directory, 1 invocation | close the new event with redacted typed stop | no old-event reuse, rename or second directory |
| Before read-only collection | 1 bounded collector sequence | close event; downstream counters remain zero | no manual ADB, native stderr/provider payload read or retry |
| Account and route comparison | 1 memory-only comparison | terminal stop before authorization/provider on missing, mismatch or unknown | no key replacement, account rerun or same-event retry |
| Fresh authorization | at most 1 | no authorization if any precondition fails | no second authorization |
| RAW/GLOSSARY dispatch | at most 1 primary call | retain attempt/lifecycle/recovery evidence and classify unknown when needed | no network retry, schema repair, fallback or redispatch |
| After collection | 1, always via `finally` | preserve terminal result and run verifier once | no cleanup, restore, checkpoint or device write |
| VerifyOutcome | 1 | `NOT_PROVEN`/`UNKNOWN` when evidence is incomplete | no acceptance inference or redispatch |
| State/provenance update | 1 terminal update | record exact counters and one Next action | no second event under this approval |

## Final state consistency

The top state of `EDITORIAL_RECOVERY_V4_18.md`, `BUILD_STATE.md`,
`WORKSPACE_SNAPSHOT.md`, the v4.18 checklist and
`docs/P5E_RAW_AUTHORIZATION_PROPOSAL.md` agrees on the current state:

`OWNER_DECISION_RECEIVED / SINGLE_EVENT_AUTHORIZED_NOT_YET_CONSUMED / NOT_DISPATCHED / A4_3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.

They point to
`docs/P5E_A43_AUTHORIZED_SINGLE_EVENT_NEXT_WORK_REQUEST_20260926.md` as the
sole current action and keep the 2026-09-25 and earlier 2026-09-26 events
closed/consumed. Historical sections contain older next actions, but they are
explicitly marked superseded and do not override the top state. The stale
previous-packet owner decision work request remains in the tree for evidence
preservation and must not be selected as a live instruction. The frozen owner
request retains its original `OWNER_DECISION_PENDING` label because changing
that packet byte would invalidate its hash; the authorized provenance and top
state carry the subsequent `RECEIVED_NOT_CONSUMED` transition.

## Final conclusion

The owner decision is received but remains unconsumed. The MEDIUM is explained
by the valid parent-to-packet commit relationship and the LOW findings are
independently controlled. No live execution occurred during this review. The
only next action is to execute exactly one fresh event under the authorized
work request and frozen command, then retain the terminal result; do not ask
for approval again, retry, redispatch or open P6.
