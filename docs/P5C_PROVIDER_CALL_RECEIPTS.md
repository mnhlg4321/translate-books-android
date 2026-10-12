# P5C — Provider call receipts (redacted)

Ngày ghi nhận: `2026-09-08` (+07:00)

No request body, response body, chapter text, API key or secret is recorded.

| Scope | Phase | Provider/model | Calls | Request stored | Response stored | Result |
|---|---|---|---:|---|---|---|
| Fake acceptance | `L1_RAW_DISCOVERY` | `FAKE_PROVIDER` / `fake/model` | 1 | No | No | Local validation pass |
| Fake acceptance | `L1_RECONCILE` | `FAKE_PROVIDER` / `fake/model` | 1 | No | No | Local validation pass |
| Authorized live | `L1_RAW_DISCOVERY` | `openrouter` / `openai/gpt-5.6-luna` | 1 | No | No | `RETRY_PROVIDER_CALL_FAILED` |
| Authorized live | `L1_RECONCILE` | `openrouter` / `openai/gpt-5.6-luna` | 0 | N/A | N/A | `NOT_RUN` |

Live response usage was not available. The request timed out/failed at the
provider boundary after approximately 179.7 seconds. Billing state is
unknown and must not be inferred as zero. The attempt was durably marked
`RECOVERY_REQUIRED` with no response identity, report bytes or receipt bytes.

```text
External provider requests dispatched: 1
Automatic retries: 0
External response received: 0
Full request/response retention: disabled
```
