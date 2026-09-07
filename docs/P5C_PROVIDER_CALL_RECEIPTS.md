# P5C — Provider call receipts (redacted)

Ngày ghi nhận: `2026-09-07` (+07:00)

Không có live provider/API receipt. Bảng dưới chỉ ghi test-local fake calls,
không chứa request body, response body, chapter text, API key hoặc secret.

| Scope | Phase | Provider/model | Call count | Request body stored | Response body stored | Result |
|---|---|---|---:|---|---|---|
| Fake acceptance | `L1_RAW_DISCOVERY` | `FAKE_PROVIDER` / `fake/model` | 1 | No | No | Local schema/receipt validation pass |
| Fake acceptance | `L1_RECONCILE` | `FAKE_PROVIDER` / `fake/model` | 1 | No | No | Local schema/receipt validation pass |
| Live pilot | RAW + RECONCILE | Not authorized | 0 | N/A | N/A | `LIVE_AUTHORIZATION_REQUIRED` |

Fake response identities are test-generated only and are not persisted as
response payloads. The durable store keeps response identity plus redacted
report/receipt/metrics facts; its metrics check explicitly rejects the fake
response text.

```text
External provider/API calls: 0
External data egress: 0
```
