# P5C — Authorization Record

Ngày ghi nhận: `2026-09-07` (+07:00)

## Quyết định

```text
LIVE_AUTHORIZATION_INCOMPLETE
P5_DRY_RUN_ONLY
EXECUTION_DISABLED
NOT_CERTIFIED
NOT_RUNNABLE
```

Người dùng chưa cung cấp block `P5 PILOT AUTHORIZATION` đầy đủ cho hai phase
`L1_RAW_DISCOVERY` và `L1_RECONCILE`. Vì vậy không có live provider/API call,
không có request thật, và không có dữ liệu chapter thật rời thiết bị.

## Trạng thái trường bắt buộc

| Nhóm | Trạng thái |
|---|---|
| Pilot ID và hai authorization ID | Chưa cung cấp |
| Project/binding/run/evaluation identity | Chưa chọn một binding persisted cụ thể để live pilot |
| Canonical pack/profile hash cho live authorization | Chưa được ủy quyền trong block |
| Chapter và bốn source reference | Chưa cung cấp |
| Provider/model/endpoint-account fingerprint | Chưa cung cấp |
| Consent dữ liệu rời thiết bị | Chưa cung cấp |
| Call, token, cost, execution-time budget | Chưa cung cấp |
| Request/response retention và redaction | Chưa cung cấp |
| Cancellation/stop authority, issue/expiry | Chưa cung cấp |
| L2/L3, certification, general runnable | Được yêu cầu là `NO` trong mẫu; không được mở |

## Call accounting

- Live provider/API calls: `0`.
- Fake provider calls trong acceptance test: `2` primary semantic calls, một
  cho RAW và một cho RECONCILE. Đây là test-local evidence, không phải live
  authorization hay live pilot.
- API key, secret, full chapter, request body và response body không được ghi
  vào Git hoặc report.

Không được suy diễn quyền live từ fake authorization, `DATA_COMPATIBLE`,
`PILOT_SETUP_READY` hoặc kết quả static qualification.

## Điều kiện mở tiếp

Chỉ sau khi block đầy đủ được cung cấp và preflight cuối được xác nhận riêng
mới có thể xem xét live call. Nếu thiếu bất kỳ identity, consent hoặc budget
bắt buộc nào, kết quả phải tiếp tục là `LIVE_AUTHORIZATION_INCOMPLETE` với
provider call count bằng `0`.
