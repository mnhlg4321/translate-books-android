# P5E A4.3 owner-decision review — 2026-09-24

Status: **REVIEW PASS / OWNER PERMISSION STILL PENDING / NOT EXECUTED**.

Reviewed `docs/P5E_A43_OWNER_DECISION_WORK_REQUEST_20260924.md` against the
current command and helper. The pinned bytes are consistent: manifest
`412790E2…5EA3`, command `51A71D47…8AB5`, helper `CB9C0731…901F`, and offline
QA `D1247112…7050`. No new account or fingerprint provenance is required;
the remaining gate is the owner’s permission for the described live event.

The launcher instruction is correct. The command must be copied byte-for-byte
from `.txt` to a private `.ps1` path, preserving encoding and newlines, because
it obtains its self path from `$MyInvocation.MyCommand.Path` and hashes the
launcher bytes. `P5E_A43_COMMAND_SHA256` must be set in the owner PowerShell
process to the full command hash
`51A71D47BBFC91DE19658FEEA120CF419ECD0F2FFD77562D74B83C73F9A98AB5`; it is
separate from the account fingerprint. The command then checks its own hash
before invoking the root helper and the helper repeats its hash/dependency
gates.

The proposed one-run scope and budgets match the pinned implementation:
serial `15e84958`; one fresh event; read-only before/after collection; one
memory-only account comparison inside the live method; fresh authorization;
at most one RAW/GLOSSARY call; no fallback, schema repair, retry, RECONCILE,
cleanup, restore, or redispatch. Limits are primary `1`, repair `0`, retry `0`,
input/output/total `100000/4096/104096`, cost cap USD `0.05`, execution
`120000 ms`, authorization `180000 ms`, and host observation `240000 ms`.

No execution was performed. The next action is solely for the owner to approve
this exact scope and run it once in the prepared PowerShell process; approval
is not implied by this review or by the prior account MATCH.
