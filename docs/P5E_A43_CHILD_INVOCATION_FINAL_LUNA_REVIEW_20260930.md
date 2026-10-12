# P5E A4.3 child-invocation final Luna review — 2026-09-30

## Verdict

`ACCEPT` on the offline launch-safety bar.

Counts: `0 BLOCKER / 0 HIGH / 0 MEDIUM / 2 LOW`.

Review target: branch `feature/v4.18-p5e-runner-repair-20260917`, exact packet
implementation commit `01a425e7`, candidate
`D:\P5E-private\P5E_A43_FINAL_OWNER_SINGLE_USE_LAUNCHER_20260930_CHILD_SAFE.ps1`.

## Exact evidence reviewed

- Contract: `2A1474494E2760D851DC9378AF8252968A65A4779E2E1786434AB70918E6D2EB`.
- Synthetic QA: `EE03540BAB9BDFD754595C5AEBE5276CBED3875104BC78BECEDEE822EE32F328` — `27/27 PASS`.
- Static launcher QA: `ADE91A69C08793E409FE7B37E91B35448B1E41128B8366390F24FB4905A1D7F1` — `16/16 PASS`.
- Candidate launcher: `569F8437A0630D52E8F26D5631AF2D9CBC089947BFD337E7FEC510B831D98A41`.

The review confirmed separate process-start/PID/exit/capture/timeout states,
bounded concurrent stream capture, descendant containment verification,
redacted exceptions, outer nonzero propagation, Dispatch-only expected-value
injection, expiry-before-child, strict receipt schema, CreateNew single-use
reservation and no fallback to consumed authority. The candidate was not run.

## Low findings and disposition

1. The workspace snapshot had stale `26/26` and `13/13` counts. The canonical
   state sync updates these to `27/27` and `16/16`.
2. The candidate suppresses a post-child launcher-metadata write exception.
   This is a post-run evidence-durability limitation, not an authorization,
   PrepareEvent, child-invocation, retry or dispatch bypass. It remains an
   explicit owner-review caveat: missing metadata must be treated as an
   evidence gap and cannot be converted to a live PASS or P5/P6 claim.

No file was edited by Luna. No launcher, receipt, event, ADB, provider,
credential, database, build/install or RAW action was performed.
