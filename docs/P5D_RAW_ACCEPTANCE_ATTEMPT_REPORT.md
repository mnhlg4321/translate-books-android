# P5D — RAW acceptance attempt report

Ngày chạy: `2026-09-09` (+07:00)

## Quyết định

```text
RAW_ACCEPTANCE_ATTEMPT_COMPLETE
RAW_ACCEPTANCE_STOPPED_AT_DEADLINE
RAW_ACCEPTANCE_RECOVERY_CLOSED
EXTERNAL_STATE_REMAINS_UNKNOWN
RAW_PREDECESSOR_NOT_COMMITTED
P5_L1_ACCEPTANCE_INCOMPLETE
RECONCILE_AUTHORIZATION_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_GLOBALLY_RUNNABLE
```

Đây là đúng một lần dispatch RAW được người dùng ủy quyền. Không có
RECONCILE, schema repair, network retry hoặc lần gọi provider thứ hai.

## Identity và authorization

| Field | Value |
|---|---|
| Device | `15e84958` |
| Production package | `4.17-dev.23 / code191` |
| Production APK SHA-256 | `5F3C841F590C6F7AA3E19F625D2B387E55B2D6BFD3C50A93D61F0337E6A2340C` |
| Final focused test APK SHA-256 | `CE5E29CABA15D08D3C6C2A032B961171C11428E28CC41A302792CA6D904CDCC4` |
| Test/source commit | `ae6d9e23d6b5b4709e9fea4c5a42a4adaaacc3a6` |
| Selector / chapter | `p5d-raw-mercedes-vol5-001 / 001` |
| Binding identity | `2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` |
| Run declaration | `7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0` |
| Compatibility evaluation | `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` |
| Pack / profile hash | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` / `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Authorization | `P5D-VOL5-RAW-ACCEPTANCE-20260909-01` |
| Authorization ID hash | `d116a03f995480c19187ea6531dbf4fc4e91690178f13841854297b62cf01693` |
| Provider / model | `OpenRouter / openai/gpt-5.6-luna` |
| Authorization budget | `1 primary / 0 repair / 0 retry / 100000-4096-100000 tokens / $0.10 / 5 min` |

## Source preflight

Binding và source identity được đọc lại từ persistent app state; không dựng lại
từ UI memory. Kết quả khớp binding:

| Role | Normalized bytes | SHA-256 |
|---|---:|---|
| RAW | 23,814 | `A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE` |
| DRAFT | 26,462 | `64ADECD8CECCBB13446EF14C494CA9BB1987117C428C5758E7442270EC7F62B5` |
| GLOSSARY | 3,249 | `4BC3E2DD05542AA5CA6B7E5FCAC43ED53E9AF57060EB69C6FA71E9D0A2EA0314` |
| PRONOUN | 452 | `4947FF9184995BE5F850F2323FBE0A04C67302FB8D5AFB63CF12202B44720686` |

PRONOUN transport vẫn là `455` bytes có BOM, sau đúng quy tắc app-owned bỏ
3-byte UTF-8 BOM là `452` bytes đã pin. Không trim, đổi newline, normalize
Unicode hoặc sửa nguồn.

## Dispatch và lifecycle readback

Instrumentation được chạy bằng method opt-in duy nhất
`authorizedVol5RawRunsOnlyWhenExplicitlyOptedIn` với `p5d_raw_live=YES`.
Authorization được ghi/claim trước dispatch và receipt đọc lại là `CONSUMED`.

Sau hơn giới hạn `300,000 ms`, host runner không nhận được terminal result và
không kết thúc được process theo deadline. Thu thập metadata không chứa body/key
cho thấy target process còn thread `OkHttp` và socket tới `openrouter.ai`; sau đó
runner host được dừng và package được force-stop chỉ để chấm dứt call đã vượt
deadline, không uninstall/reset database.

Readback sau cleanup:

| Field | Value |
|---|---|
| Attempt status trước cleanup | `CLAIMED` |
| Attempt status sau owner recovery closure | `RECOVERY_REQUIRED` |
| Local reason | `RETRY_PROVIDER_CALL_TIMEOUT` |
| Lifecycle last stage | `RESPONSE_HEADERS_RECEIVED` |
| Request body bytes | `85,068` |
| HTTP status / content type | `200 / application/json` |
| Generation ID | `gen-1788967700-RgJDCWrZsNZ4VAAWmlj8` |
| Provider response ID | absent |
| Response body complete | not observed |
| Persisted report / receipt | `0 / 0` bytes |
| Cancellation source | absent/unknown |
| Acceptance authorization | present, `CONSUMED` |
| Recovery history | `2` records; prior decisions preserved |

`elapsedMs=0` trong row lifecycle là giá trị của event headers hiện tại, không
được dùng làm thời lượng thật. Vì app không ghi được event terminal trước khi
process bị dừng, không thể kết luận request đã hoàn tất, bị provider hủy hay đã
bị tính phí chỉ từ local row.

## Kết quả acceptance

- `LIVE_RAW_ACCEPTANCE_PASS`: **FAIL/CHƯA ĐẠT** — không có response body hoàn
  chỉnh và không có local semantic/schema validation result.
- `RAW_PREDECESSOR_COMMITTED`: **NO**.
- `REPORT_L1` và receipt: **NO**, không có partial bytes.
- Provider usage/cost: **UNKNOWN** — lần này không có usage/cost metadata trong
  response body/app; không suy luận thành `$0`. Chi phí đã biết của generation
  truncated trước đó vẫn là `$0.0075392`, không phải chi phí của attempt này.
- External classification: `EXTERNAL_STATE_REMAINS_UNKNOWN`. Generation ID đã
  được nhận ở response headers, nhưng chưa có đối soát external bounded cho body,
  finish reason hoặc billing của attempt này.
- RECONCILE: **không được gọi và chưa được ủy quyền**.
- Execution/certification/general runnable: vẫn **disabled / not certified /
  not globally runnable**.

## Evidence về recovery

Test-only readback/closure chạy pass:

- `readBackVol5AcceptanceAfterDeadlineCleanup`: `1/1 PASS`.
- `closeInterruptedVol5AcceptanceAsTypedRecovery`: `1/1 PASS`.
- Readback sau khi cài final focused test APK: `1/1 PASS`, giữ nguyên status,
  generation ID, `CONSUMED`, và zero report/receipt.

Closure dùng `EditorialP5CAttemptStore.markRecoveryRequired()`; không SQL thủ
công, không overwrite primary reconciliation, không xóa attempt và không tạo
binding mới.

## Blocker và quyết định tiếp theo

`RAW_ACCEPTANCE_INCOMPLETE` là quyết định cuối của lần này. Blocker cụ thể là
đường live không đạt terminal/recovery persistence trong giới hạn 5 phút: app
đã ghi headers/generation nhưng chưa ghi terminal lifecycle/typed result trước
khi runner bị dừng. Vì external state vẫn unknown, không được retry, không được
dùng lại authorization đã consumed và không được mở RECONCILE. Cần một task P5D
hardening riêng cho end-to-end deadline/process-death recovery và một quyết định
authorization mới nếu muốn chạy tiếp.

Không có authority bytes, canonical pack/profile hash, database schema, UI,
provider policy hoặc build metadata production nào bị thay đổi trong lần này.
