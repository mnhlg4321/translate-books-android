# P5C — Authorization Record

Ngày ghi nhận: `2026-09-08` (+07:00)

## Decision and scope

```text
P5C_LIVE_AUTHORIZED_ATTEMPT
STOP_PROVIDER_TIMEOUT
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

The user explicitly authorized data egress and a real OpenRouter call, selected
`openai/gpt-5.6-luna`, and approved a total cap of USD 0.10 and five minutes.
The runner used two exact-phase, single-use in-memory authorizations. The
provider request was sent once for `L1_RAW_DISCOVERY`; no RECONCILE call was
made after the RAW provider state became uncertain.

## Authorized facts

| Field | Redacted recorded value |
|---|---|
| Pilot selector | `p5c-real-mercedes-vol4-001` |
| Chapter | `001` |
| Binding identity | `2578552b14ae8269a72759a5c76b172ca44f63b41462648ba7f63b6af067e38f` |
| Run declaration identity | `460c9d575d09a47bb1d4b777120ff50af6879d8263342abc25aee8bee30d2852` |
| Compatibility evaluation | `b117671c-8f07-44b1-b4cc-5b212a573401:compatibility:v1` |
| Canonical pack hash | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` |
| Canonical profile hash | `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Provider / model | `openrouter` / `openai/gpt-5.6-luna` |
| Endpoint | `https://openrouter.ai/api/v1/chat/completions` |
| Endpoint/account fingerprint | `2cd5d48d21f3d99ad396614a8bee162e62a23360ca0a8a081187b26d046b34de` |
| Data egress | `YES` |
| Request-body storage | `NO` |
| Full-response storage | `NO` |
| Evidence redaction | `HASH_ONLY` |
| Primary calls per phase | `1` |
| Schema repairs per phase | `1` maximum |
| Automatic network retries | `0` |
| Input/output/total cap per phase | `100000 / 2048 / 100000` |
| Cost cap | `$0.05` per phase; `$0.10` pilot total |
| Execution cap | `300000 ms` |
| L2/L3, certification, general runnable | `NO` |

Authorization IDs were generated ephemerally by the test runner and were not
persisted or logged because they contain no durable authority; the durable
attempt is identified by its app-owned request/binding facts. The API key was
never written to Git, report, log or receipt.

## Outcome

The RAW authorization was consumed by one provider request. The provider did
not return a response before the observed 179,728 ms call duration, so the app
returned `RETRY_PROVIDER_CALL_FAILED`. This is a legitimate provider stop;
the app did not automatically retry or call RECONCILE. Any external billing
state remains unknown until the provider account is checked.

The durable row is `RECOVERY_REQUIRED` with no response identity, report bytes
or receipt bytes. A new live call requires a separate explicit authorization
after resolving the unknown external-call state.

## Call accounting

- Live provider requests dispatched: `1`.
- Live semantic retries: `0`.
- Live schema repairs: `0`.
- Live RECONCILE calls: `0`.
- Fake provider calls in acceptance tests: `2`.
- API key, source text, request body and response body: not recorded.
