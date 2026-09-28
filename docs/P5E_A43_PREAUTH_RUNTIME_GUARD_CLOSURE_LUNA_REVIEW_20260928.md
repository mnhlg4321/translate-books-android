# P5E A4.3 preauthorization runtime-guard closure — Luna review

Review date: `2026-09-28`.

## Verdict

`WORK_REQUEST_AND_PROVENANCE_PASS / 0 BLOCKER / 0 HIGH / 0 MEDIUM / LIVE_PACKET_STILL_NOT_READY`.

This verdict assesses the next offline work request, not the unfinished runtime
implementation. The underlying packet remains
`0 BLOCKER / 1 HIGH / 2 MEDIUM / 2 LOW`, not authorized and not dispatchable.
No frozen packet byte or live surface was touched.

## Reviewed bytes

- Work request:
  `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_NEXT_WORK_REQUEST_20260928.md`,
  SHA-256 `BC2ECB5C18691E9553558C48FD237E99940BAEE8FF6F9EA0010AD5F89230F716`.
- Provenance:
  `docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_PROVENANCE_20260928.json`,
  SHA-256 `1AC85D7457504431B51E461DEB704E2CD574485B511152081A7D0B74EB4A3913`.
- Source independent review:
  `docs/P5E_A43_PM_PATH_CAPTURE_REPAIR_FINAL_LUNA_REVIEW_20260928.md`,
  SHA-256 `76ABB92C3F29ECDE3A6A62C6263F095CEAB8A5FF96356DDBCC9352FD7BB638E9`.

The provenance JSON parses successfully and pins the exact work-request hash,
manifest, command, helper, exporter, SQLite bridge, toolchain, certificate and
serial. `git diff --check` passes.

## Coverage decision

The work request fully covers:

- the PowerShell 5.1 `Kill(Boolean)` absence and verified descendant
  containment, including child/grandchild pipe-hold fixtures;
- `EXTERNAL_PROCESS_STATE_UNKNOWN` when termination cannot be proven;
- durable reconstruction of exact exporter, toolchain and SQLite bridge bytes;
- owner receipt and zero-disclosure expected-value presence/shape checks before
  event creation, helper or ADB;
- atomic decision-bound reservation/event creation, including a real concurrent
  contender fixture, so scan-then-create cannot admit two events;
- bounded outer stdout/stderr capture with exact-cap, cap+1, encoding, delayed
  close and secret-sentinel tests;
- all named historical failures and unchanged no-retry/no-redispatch/live-zero
  boundaries;
- owner usability: the owner supplies one exact approval sentence after final
  review and is not asked to write JSON or disclose key, endpoint or digest.

The canonical plan, build state, snapshot, checklist and proposal now use the
same current state. Their single next action is the offline runtime-guard
closure; historical next actions remain clearly marked historical or
superseded.

## Single next action

Execute only
`docs/P5E_A43_PREAUTH_RUNTIME_GUARD_CLOSURE_NEXT_WORK_REQUEST_20260928.md`
offline. Do not request owner authorization, execute either review-only
command, create an event, use ADB/device/provider/credential/DB writes,
build/install, dispatch RAW, retry/redispatch, claim P5 exit or open P6.
