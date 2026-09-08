# P5C — Live L1 pilot report

Ngày ghi nhận: `2026-09-08` (+07:00)

## Status

```text
P5C_LIVE_AUTHORIZED_ATTEMPT
P5C_PILOT_STOPPED_WITH_PROVIDER_ERROR
STOP_PROVIDER_TIMEOUT
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

## Exact binding

The pilot used the persisted selector `p5c-real-mercedes-vol4-001` and chapter
`001`, not UI memory or a latest-pack lookup. Binding, run declaration,
compatibility evaluation, canonical pack hash, profile hash and source hashes
were re-read and matched before dispatch.

Source identity used by the binding:

| Role | Normalized bytes | SHA-256 |
|---|---:|---|
| RAW | 7,685 | `FE4CE02301E9EE5FD457A7A744C982B23EE3C95100CFDC5CBDF908B3979F40AF` |
| DRAFT | 9,075 | `2B0213214190E0DED1DD1AADE70D7959C23BED294E278E8DFC962BCBC73C042C` |
| GLOSSARY | 1,649 | `7925ED16AE7226677E4DDD37C5E07DC152711CFF6851D177000CC9FE34CDE658` |
| PRONOUN | 172 | `093C6588CE52FBFC76D7C69CCB1FFC1385D587D9C2A08AF33D286FDEABE9DF86` |

## Live result

| Phase | Primary | Repair | Network retry | Result |
|---|---:|---:|---:|---|
| `L1_RAW_DISCOVERY` | 1 | 0 | 0 | `RETRY_PROVIDER_CALL_FAILED` |
| `L1_RECONCILE` | 0 | 0 | 0 | `NOT_RUN` |

The OpenRouter request did leave the device. No response was returned before
the observed 179,728 ms call duration. The app returned a typed retry stop,
marked the attempt `RECOVERY_REQUIRED`, and did not issue a second request.
Provider billing/acceptance is unknown because no response usage receipt was
received; the provider account must be checked before any future retry.

## Durable safety result

- `providerCallsBeforePreflight=0`.
- One RAW provider request; no automatic semantic or network retry.
- No schema repair call.
- RECONCILE was not called because the RAW predecessor was not validated.
- Durable attempt row: `RECOVERY_REQUIRED`.
- Response identity: absent.
- `REPORT_L1` bytes: `0`.
- Receipt bytes: `0`.
- No partial report/receipt, certification, state transition or rebind.
- `executionAllowed=false`, `NOT_CERTIFIED`.

This is not `P5_L1_PILOT_COMPLETE`, `REPORT_L1_VALIDATED` or
`L1_RECEIPT_VALIDATED`. Fake E2E and static/host qualification remain separate
evidence and do not turn this legitimate provider stop into a pass.
