# G2 preflight stop — 2026-10-05

Owner approval for the G2 proposal was received in-session. Before any provider dispatch, the approved L2 predecessor was checked on `emulator-5554` using the live `L1_THEN_L2` runner.

## Approved predecessor and preflight

- Fixture: `fx-a04`.
- Required predecessor: G1 repeat 1, run `5d438ce3-1b6b-4fcd-8434-aa98deaa84a1`.
- Required `REPORT_L1` SHA-256: `ae1172093058cf66cc56f3fb1ce45ea4abf78f38666137902b96919e1fd452ce`.
- Preflight run: `c6d2e8f1-5a37-4c0b-9d28-61ef7342ab90`.
- Private evidence: `D:\P5E-private\p6-runs\c6d2e8f1-5a37-4c0b-9d28-61ef7342ab90\`.

Fixture manifest verification and the prompt-input guard passed. The runner passed the live settings/fingerprint gate and stopped while reopening the retained predecessor with:

`P6_REUSED_L1_STATE_MISSING`

The instrumentation log is `D:\P5E-private\p6-runs\c6d2e8f1-5a37-4c0b-9d28-61ef7342ab90\logs\fx-a04-instrumentation.txt`. No provider request was created, no spend reservation was written, and no G2 group ledger exists. The temporary fixture input was removed by the runner cleanup.

## Why the group stops here

The emulator retains only the base G1 app-private states for `74a53417-249b-419f-8b29-33f0f30fa5f9` (`fx-a02`, `fx-a04`, `fx-a11`). It does not retain the repeat-1 state for `5d438ce3-1b6b-4fcd-8434-aa98deaa84a1`.

The base `fx-a04` report is not an equivalent substitute: its SHA-256 is `380925307f8e9e03b25a64b1a29f9bb9dece374ef3a029ed0895a8732c89e1a5`, with zero findings, while the approved repeat-1 report has one adjudicated T-S2 finding. Substituting it would change the inherited-finding measurement and violate the approved predecessor rule.

This is a prerequisite/state-availability stop, not a provider or model result. Actual provider calls: `0`. G2 spend: `USD 0`. G1 ledger remains unchanged.

## Decision required before dispatch

Keep the approved proposal unchanged and provide/reconstruct the exact repeat-1 app-private predecessor state through a separately reviewed offline mechanism, or approve a concrete scope change that selects the retained base predecessor. Until one of those decisions is made, no L2/L3 provider call is authorized by the current evidence package.
