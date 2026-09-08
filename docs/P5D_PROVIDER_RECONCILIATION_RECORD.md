# P5D — Provider reconciliation record

Audit completed: `2026-09-08` (+07:00), after manual authentication to the
OpenRouter read-only Logs/Activity UI.
Audit scope: the bounded window around the authorized P5C call on `2026-09-08` (+07:00).  
Provider/model filter: `openrouter` / `openai/gpt-5.6-luna`.

## Classification

```text
EXTERNAL_CONFIRMED_CANCELLED
RETRY_ELIGIBLE: YES_AFTER_NEW_EXACT_PHASE_AUTHORIZATION
PROVIDER_CALLS_IN_P5D.0-P5D.4: 0
```

The authenticated OpenRouter Logs row matches the single P5C RAW dispatch by
the provider/model, application, displayed time and the app-owned request
size. Its generation details explicitly report `Finish reason: cancelled`,
provider HTTP `200`, streaming `true`, and no I/O logging. The provider-side
generation therefore has a confirmed cancellation state. The external record
also contains usage/cost metadata, while the app did not receive or persist a
usable response body or usage receipt. This is not a successful L1 result and
is not classified as `EXTERNAL_COMPLETED_BILLED_OUTPUT_UNAVAILABLE` because
the provider explicitly reported cancellation.

## Redacted evidence

| Field | Value |
|---|---|
| Original app evidence | Persisted P5C row selected by `p5c-real-mercedes-vol4-001`; status `RECOVERY_REQUIRED` |
| Binding identity | `2578552b14ae8269a72759a5c76b172ca44f63b41462648ba7f63b6af067e38f` |
| Run declaration identity | `460c9d575d09a47bb1d4b777120ff50af6879d8263342abc25aee8bee30d2852` |
| Chapter key | `001` |
| App-observed result | One RAW dispatch; no response/usage receipt; no automatic retry; no RECONCILE |
| App-observed duration | `179728 ms` |
| OpenRouter displayed time | `Sep 8, 09:29 PM` (+07:00 display; minute precision) |
| OpenRouter provider/model/app | `OpenAI` / `GPT-5.6 Luna` / `Translate Books with LLMs` |
| OpenRouter input/output | `17,808` / `84` tokens |
| OpenRouter cost | `$0.00366` displayed provider usage |
| OpenRouter finish/transport | `cancelled`; streaming `true`; provider HTTP `200` |
| OpenRouter latency | routing `258 ms`; provider `675 ms`; generation `9.8 s`; total `10.8 s` |
| Generation ID | `gen-1788877749-P8b2hBo1TWuduENuKbQ3` |
| Request ID | Not copied from UI; no need to expose it for this classification |
| Account/key fingerprint | UI key label was redacted; full key/secret not recorded |
| Input/output logging | Not enabled; no content retrieval attempted |
| Decision authority | P5D recovery policy; provider-confirmed cancellation |
| Retry decision | Eligible only with a new exact-phase, single-use RAW authorization; old authorization is not reusable |

The OpenRouter row is redacted metadata only. No prompt, chapter text,
request body, response body, API key or session secret was opened, copied or
stored. The displayed cost is provider-side evidence for this generation and
must not be used to claim that the app received a valid L1 response.

## Required next decision

The external state is now reconciled as a provider-confirmed cancellation.
P5D.5 may prepare a new RAW recovery authorization, but it must include the
new attempt identity and explicit acknowledgement that a second semantic
attempt may repeat work and incur new billing. The old P5C authorization must
never be reused. No RECONCILE authorization is issued by this record.

Until a new authorization is separately approved, the durable app attempt
remains `RECOVERY_REQUIRED`, execution remains disabled, and no new provider
call is permitted.
