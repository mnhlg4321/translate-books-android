# P5D — Provider reconciliation record

Audit attempted: `2026-09-08 23:05:26 +07:00`  
Audit scope: the bounded window around the authorized P5C call on `2026-09-08` (+07:00).  
Provider/model filter: `openrouter` / `openai/gpt-5.6-luna`.

## Classification

```text
EXTERNAL_STATE_REMAINS_UNKNOWN
RETRY_ELIGIBLE: NO
PROVIDER_CALLS_IN_P5D.0-P5D.3: 0
```

The OpenRouter Activity page redirected to sign-in in the available browser
session. No authenticated Activity data, generation metadata, request ID,
token usage, cost, finish state or billing state was available for inspection.
The audit therefore does not claim that the request was absent and does not
infer a zero-dollar charge.

## Redacted evidence

| Field | Value |
|---|---|
| Original app evidence | Persisted P5C row selected by `p5c-real-mercedes-vol4-001`; status `RECOVERY_REQUIRED` |
| Binding identity | `2578552b14ae8269a72759a5c76b172ca44f63b41462648ba7f63b6af067e38f` |
| Run declaration identity | `460c9d575d09a47bb1d4b777120ff50af6879d8263342abc25aee8bee30d2852` |
| Chapter key | `001` |
| App-observed result | One RAW dispatch; no response/usage receipt; no automatic retry; no RECONCILE |
| App-observed duration | `179728 ms` |
| Account/key fingerprint | Not available to the unauthenticated Activity audit |
| Generation/request ID | Not available |
| Input/output logging | Not enabled; no content retrieval attempted |
| Decision authority | P5D recovery policy; external state unresolved |
| Retry decision | Do not retry; a new authorization cannot bypass reconciliation |

## Required next evidence

The user must manually authenticate the already-open OpenRouter Activity page,
then inspect only the bounded date/time window using the provider/model/account
filters. The audit may retain only redacted metadata: generation/request ID,
timestamps, provider/model, finish or cancellation state, token counts, cost,
latency and permitted upstream ID. Prompt, chapter, request body, response body,
API key and session secret must not be exported.

Until that read-only audit is available, the durable app attempt remains
`RECOVERY_REQUIRED` / external state unknown. P5D.3 hardening and P5D.4 tests
may proceed locally, but no new provider call is permitted.
