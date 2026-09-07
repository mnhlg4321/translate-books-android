# P5C — Cost, token and latency report

Ngày ghi nhận: `2026-09-07` (+07:00)

## Live pilot

```text
Live calls: 0
Input/output/total tokens: N/A
Estimated/actual cost: N/A
Provider latency: N/A
```

No live authorization or provider response exists, so no live metric is
fabricated.

## Fake acceptance metrics

The fake provider returns deterministic usage metadata solely to exercise the
bounded metrics and durable readback path:

| Phase | Primary calls | Repair calls | Network retries | Input tokens | Output tokens | Total tokens | Reported cost | Truncated |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| `L1_RAW_DISCOVERY` | 1 | 0 | 0 | 40 | 20 | 60 | 0 | No |
| `L1_RECONCILE` | 1 | 0 | 0 | 40 | 20 | 60 | 0 | No |
| Fake aggregate | 2 | 0 | 0 | 80 | 40 | 120 | 0 | No |

For both phases `providerCallsBeforePreflight=0`, schema and receipt validation
passed, `PRESERVE_DRAFT=0`, and the fake response was locally validated. The
fake values are not a forecast or approval for a live budget. Latency is
environment-dependent and is retained only in isolated metrics JSON, not
reported as live performance evidence.

## Budget policy retained

The fake authorization uses bounded values in the test fixture and permits one
primary semantic call plus one schema-repair call per phase, with zero
automatic network retries. The real authorization block must provide its own
limits before any external call.
