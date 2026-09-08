# P5C — Cost, token and latency report

Ngày ghi nhận: `2026-09-08` (+07:00)

## Live pilot

```text
Live provider requests dispatched: 1
RAW primary calls: 1
RAW schema-repair calls: 0
RAW automatic network retries: 0
RECONCILE calls: 0 (not run after RAW uncertainty)
Input/output/total tokens: not reported (no provider response)
Estimated/actual cost: actual billing unknown; do not infer zero
Observed provider-boundary latency: 179728 ms
Typed result: RETRY_PROVIDER_CALL_FAILED
```

The live request was authorized and dispatched, but no response or usage
receipt was returned. The app therefore recorded a typed provider stop and
durable recovery state without committing a report or receipt. The provider
account must be checked before any future retry.

The authorized ceiling was 100000 input tokens and 2048 output tokens per
phase, with one primary call and one schema-only repair maximum per phase,
zero automatic network retries, and a USD 0.10 total pilot cap. Using the
OpenRouter public price for `openai/gpt-5.6-luna` ([model pricing page](https://openrouter.ai/openai/gpt-5.6-luna-20260709), USD 0.20/M input and USD 1.20/M output), the two-phase token ceiling is approximately USD 0.0449152;
this is only a preflight ceiling, not a statement about the unknown billing
for the timed-out request.

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

The live authorization used the same bounded call policy: one primary semantic
call plus one schema-repair call per phase, with zero automatic network
retries. No automatic retry or second semantic call was made after the
provider stop.
