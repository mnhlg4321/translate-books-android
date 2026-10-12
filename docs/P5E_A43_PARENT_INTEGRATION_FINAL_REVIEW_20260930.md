# P5E A4.3 parent integration — final offline review — 2026-09-30

## Verdict

`OFFLINE_EXECUTABLE_INTEGRATION_PASS / CANDIDATE_NOT_RUN / NOT_AUTHORIZED / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.

The executable candidate is now tracked in the working tree and is bound to the exact repaired parent, resolver, runtime guards, expected-value loader, child contract and packet pins. It passed the full synthetic PS5.1 integration fixture. This is not an A4.3 live pass and does not create reusable authority.

## Exact evidence

- Candidate: `scripts/p5e-a43-pre-reservation-launcher-entrypoint.ps1`, SHA-256 `CC01C33F056F86F4ACC4E3BD45AAAA2910CCEE9719AC5830EDAE99987A7C8E23`; direct candidate execution was not performed.
- Integration fixture: `scripts/test-p5e-a43-pre-reservation-integration.ps1`, SHA-256 `4353DE8FC2F60B7BCCA9FF5EB13A817F8FB22E1475CE2D614371B0FD38B5C423`.
- Integration report: `evidence/p5e-a43-parent-integration-20260930/P5E_A43_PARENT_INTEGRATION_QA_20260930_03.json`, SHA-256 `DC54F9788BDAA2D5D0FB50BC65B07B69485CA9235107FECD9DB4C63B1DD3E457`, `21/21 PASS`, `candidateExecuted=false`, `liveActions=0`.
- Process-level probe: synthetic success exited `0`; synthetic child failure exited `1`; probe output contained no key, digest or child-output sentinel.
- Parent regression: `26/26 PASS`. Child regression: `27/27 PASS`. PS5.1 parse: PASS. `git diff --check`: PASS.

## Dependency review

| Slot | Real implementation bound | Offline assertion |
|---|---|---|
| ResolveToolchain | `Resolve-P5ERawToolchain` | One spy call; SDK Container; adb/java/apksigner Leaf; output reaches parent. |
| ReadApproval | `Read-Host -AsSecureString` seam | Exact literal; wrong/empty/EOF/cancel stop before reservation/key; no echo. |
| ReserveLauncher | `New-P5EA43ReservationMarker` | CreateNew and collision/no-overwrite. |
| CreateOwnerRoot | `Get-P5EA43DecisionKey` plus no-reparse guard | Unique root and reuse stop. |
| CreateReceipt | Strict `p5e.a43.owner-decision-receipt.v1` serializer | Exact schema, pins, scope, serial, limits and TTL. |
| ValidateReceipt | `Read-P5EA43OwnerDecisionReceipt` | Hash/pin/scope/serial/expiry and post-key revalidation. |
| ReadKey | `Invoke-P5EExpectedFingerprintLoadFromSecureKey` | SecureString-only synthetic input; process cleanup and no secret evidence. |
| InvokeChild | `Invoke-P5EPhaseChildInvocation` | Exact command copy, one child boundary, exit/capture/timeout handling. |
| ClearKey | Environment clear plus `SecureString.Dispose` | All branches; cleanup failure remains secondary. |
| WriteDiagnostic | `Write-P5EA43DiagnosticCreateNew` | Bounded/redacted record; write failure is `EVIDENCE_INCOMPLETE`. |

The integration fixture uses temporary directories with spaces, a synthetic SDK/toolchain, a fake child process and no ADB/provider/database/credential dependency. Receipt and reservation fixtures never enter a private live registry.

## Independent correctness disposition

No remaining blocker, high, medium or low finding affects the executable contract in this offline package. The review specifically covered SDK kind, resolver output transfer, parent catch preservation, approval/EOF, receipt schema and expiry after key, one-use reservation, environment isolation, child exit/capture/timeout, cleanup masking, diagnostic redaction and actual PS5.1 process exit.

The historical first stop remains unresolved by design. The old launcher, receipts, markers and event evidence were not rerun, renamed or reused. Release steps 05–09 remain unchecked.

## Owner boundary

The exact candidate may be presented for one separate future A4.3 decision under the pinned manifest and fresh authorization. This package does not request key entry, approval, live dispatch, retry, fallback, reconciliation, build, install, P5 exit or P6.

Final provenance and delivery manifest: `docs/P5E_A43_PARENT_INTEGRATION_FINAL_PROVENANCE_20260930.json` and `docs/P5E_A43_PARENT_INTEGRATION_DELIVERY_20260930.json`.
