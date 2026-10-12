# P5D — VOL5 RAW Diagnostic Attempt Report

Ngày chạy: `2026-09-09` (+07:00), validation package `4.17-dev.21 / code189`

## Kết quả cuối

```text
RAW_DIAGNOSTIC_ATTEMPT_COMPLETE
RAW_DIAGNOSTIC_STOPPED_RETRY_OUTPUT_TRUNCATED
RAW_PREDECESSOR_NOT_COMMITTED
RECONCILE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

Không ghi `LIVE_RAW_VALIDATED_AND_COMMITTED`: response transport đã hoàn tất
nhưng output bị cắt tại giới hạn `2048` tokens, nên local validation fail-closed.
Không gọi repair, không resend và không gọi RECONCILE.

## Exact scope và authorization

| Field | Redacted value |
|---|---|
| Selector / chapter | `p5d-raw-mercedes-vol5-001` / `001` |
| Binding identity | `2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` |
| Run declaration | `7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0` |
| Compatibility evaluation | `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` |
| Canonical pack/profile | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` / `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Authorization ID hash | `a28d70c9f1e9b33160daa6d1614abae98f5a921621fbea49f92279f06bcdc00d` |
| Exact phase | `L1_RAW_DISCOVERY` |
| Provider / model | `openrouter` / `openai/gpt-5.6-luna` |
| Primary / repair / network retry | `1 / 0 / 0` |
| Token caps | `100000 / 2048 / 100000` |
| Cost/time cap | `$0.10` / `300000 ms` |
| Egress / retention | `YES` / request `NO`, full response `NO`, lifecycle `HASH_ONLY` |
| Duplicate-risk acknowledgement | `YES` |
| RECONCILE/L2/L3/certification | `NO / NO / NO / NO` |

Reconciliation được ghi trước dispatch qua `EditorialP5CAttemptStore`, gắn
attempt cũ, classification `EXTERNAL_CONFIRMED_CANCELLED`, evidence
`docs/P5D_VOL5_RAW_PROVIDER_RECONCILIATION.md` và authorization hash mới.
Authorization receipt được consume một lần và không được dùng lại.

## Local execution evidence

| Metric | App result |
|---|---:|
| Provider calls | `1` |
| Primary semantic calls | `1` |
| Schema repairs | `0` |
| Automatic network retries | `0` |
| Input tokens | `20,327` |
| Output tokens | `2,048` |
| Total tokens | `22,375` |
| Actual reported cost | `0.0075392` |
| Finish reason | `length` |
| Truncated | `true` |
| Schema validation | `false` |
| Receipt validation | `false` |
| Local latency | `20,590 ms` |

Durable readback sau execution:

| Field | Value |
|---|---|
| Attempt | `157e3517b4b98535392db95f0c93285f08a0ac6b07341a82508ffc36aea9a5f0` |
| Status | `RECOVERY_REQUIRED` |
| Recovery reason | `RETRY_OUTPUT_TRUNCATED` |
| Response identity | absent |
| REPORT_L1 / receipt | `0` / `0` bytes |
| Lifecycle stage | `RESPONSE_BODY_COMPLETE` |
| Request bytes | `85,068` |
| HTTP/content type | `200` / `application/json` |
| Lifecycle elapsed | `20,452 ms` |
| Generation ID in lifecycle | `gen-1788959113-A9RinufLgb63vTqkAEwE` |
| Cancellation source | empty/unknown; no local cancellation recorded |

`RESPONSE_BODY_COMPLETE` chỉ chứng minh transport body đã đọc hết; nó không
đủ để coi output là hợp lệ. App không commit partial output.

## Provider metadata reconciliation

OpenRouter Logs hiển thị đúng một generation mới khớp model/app/thời điểm:

| Field | Provider metadata |
|---|---|
| Generation ID | `gen-1788959113-A9RinufLgb63vTqkAEwE` |
| Request ID | `req-1788959113-sRWHWKCcGAHmWqJxDqWz` |
| Provider response ID | `resp_0f12eb892139409f016aa1598a01ac87d1a3454c3c4d46dedf` |
| Created | `2026-09-09T13:05:13.571Z` |
| Model/provider | `openai/gpt-5.6-luna` / `OpenAI` |
| Provider status | HTTP `200`, streaming `true` |
| Finish/native reason | `length` / `max_output_tokens` |
| Displayed usage | `20,327` prompt / `2,048` completion tokens |
| Displayed cost | `$0.00754` |
| Routing/provider/total | `245 ms` / `494 ms` / `20.3 s` |
| I/O logging | disabled |

Chỉ lưu metadata redacted ở đây; không lưu prompt, source text, request body,
response body, API key hoặc session secret. Provider metadata xác nhận đây là
output bị giới hạn, không phải cancellation của attempt mới. Nguyên nhân actor
của generation cũ vẫn `UNKNOWN`.

## Source and state preservation

Sau chạy, bốn source identity vẫn khớp binding:

```text
RAW     23814  A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE
DRAFT   26462  64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5
GLOSSARY 3249  4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314
PRONOUN  452  4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686
```

PRONOUN transport file vẫn là `455` bytes có BOM; normalization của runner
chỉ bỏ BOM thành `452` bytes, không trim hoặc thay nội dung. Không uninstall,
reset hoặc dọn package; pilot database và recovery evidence được giữ lại.

## Decision

| Gate | Result |
|---|---|
| SAFETY_STOP_PASS | PASS — exact binding, auth, recovery gate, no auto-retry |
| TRANSPORT_PASS | PASS — one HTTP 200 response body, lifecycle persisted/read back |
| RAW_ACCEPTANCE_PASS | FAIL — `RETRY_OUTPUT_TRUNCATED`, no REPORT/receipt |
| Historical cancellation cause | `UNKNOWN` |
| Further RAW call | not authorized; do not auto-retry |
| RECONCILE | not authorized and no valid RAW predecessor |

P5D diagnostic attempt đã hoàn tất theo authorization. Bước tiếp theo, nếu
muốn thử lại hoặc cho phép schema repair, phải là một quyết định/quyền mới;
không dùng lại authorization hash này và không tự động chuyển sang RECONCILE.
