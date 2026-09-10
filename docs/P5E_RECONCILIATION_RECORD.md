# P5E.1 — Đối soát external generation code191

Ngày ghi nhận: `2026-09-10` (+07:00)
Phạm vi: metadata OpenRouter được đọc qua Activity/Logs đã xác thực; không mở
I/O logging và không lưu prompt, response body, source text hoặc secret.

## Current baseline tại đầu báo cáo

| Hạng mục | Giá trị |
|---|---|
| Branch | `feature/v4.18` |
| Baseline commit trước P5E | `ba42d65271c0a51722b917440d3b51e1cf6a7eec` |
| Current validation package | `v4.17-p5e.3 / versionCode 199` |
| Current build event | `build-20260910-211805` |
| Production APK SHA-256 | `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09` |
| Current database schema | `v24` |
| Device | `15e84958` |
| Canonical/final schema changes | `NONE` |

Code `189` và code `191` là historical evidence, không phải current baseline.
Báo cáo này đưa code `196`/schema `v24` lên current baseline trước khi đánh
giá contract mới.

## Generation được đối soát

```text
generation_id: gen-1788967700-RgJDCWrZsNZ4VAAWmlj8
request_id:    req-1788967700-Aldd9q7iqakFxAs1TMP4
model:         openai/gpt-5.6-luna-20260709
provider:      OpenAI
app_id:        4252031
origin:        https://local.tbl.android/
created_at:    2026-09-09T15:28:20.653Z
```

| Trường | Metadata đọc được |
|---|---|
| Finish/cancel | `cancelled=true`; UI detail hiển thị `finish reason: cancelled`; raw metadata để `finish_reason=null` |
| Input tokens | `23,674` (`tokens_prompt` và `native_tokens_prompt`) |
| Output tokens | `0` (`tokens_completion` và `native_tokens_completion`) |
| Reasoning tokens | `0` (`native_tokens_reasoning`) |
| Total tokens | `23,674` theo prompt + completion; `usage=0` là account usage field, không thay thế token counts |
| Provider/upstream | `OpenAI`; provider response `status=200`, latency `753 ms` |
| Generation duration | `generation_time=9,642 ms`; UI total khoảng `10.7 s`, routing `259 ms`, first token/provider latency `753 ms` |
| Body/terminal state | `streamed=true`, provider response status `200`; không có response body trong record, I/O logging disabled |
| Completion time | Không có `completed_at`/terminal timestamp trong metadata; chỉ có `created_at` |
| Cost fields | UI `$0.00`; raw `usage=0`, `usage_upstream=0.0047348`; không có billing flag đủ để kết luận |

## Decision

```text
EXTERNAL_CONFIRMED_CANCELLED
RETRY_ELIGIBLE: YES, only after a new exact-phase single-use authorization
NEW_AUTHORIZATION_BEFORE_THIS_DECISION: NO
RECONCILE_AUTHORIZATION: NOT_ISSUED
```

`cancelled=true` và trạng thái cancelled hiển thị trong UI là bằng chứng đủ để
loại `EXTERNAL_STATE_REMAINS_UNKNOWN`. Tuy nhiên, trạng thái cancelled không
chứng minh generation không phát sinh charge. Vì vậy P5E không ghi `$0`: account
usage field là `0`, nhưng upstream usage là `0.0047348` và billing flag không
được cung cấp. Đây là `BILLING_STATUS_NOT_UNAMBIGUOUS`; mọi retry phải ghi nhận
duplicate work/billing risk.

Trong DB pilot trước sự cố cài đặt, quyết định P5E được append vào reconciliation
history với evidence reference này và hash của authorization mới; primary
reconciliation cũ không bị sửa. DB đó hiện không còn trên device, nên quyết định
được giữ ở đây như historical evidence và không được coi là một durable row hiện
tại để cấp quyền retry.

## P5E.8 — Trạng thái bảo toàn dữ liệu trên device

Trong lần chạy `connectedDebugAndroidTest` dùng để kiểm tra validation, Gradle
installer đã xử lý một lần cài sai version và package `com.ml.tblandroidtxt`
biến mất khỏi device. Không có lệnh uninstall/reset/clear chủ động nào được
phát hành, nhưng package-data code196 đã mất và không tìm thấy bản sao DB cục bộ.
Vì vậy claim bảo toàn DB code196 là **FAIL**, không được đổi tên thành PASS.

DB v24 hiện tại được rehydrate từ canonical pack và source VOL5 app-owned để
tiếp tục kiểm tra contract; đây là `RECONSTRUCTED_ONLY`, không phải readback của
DB code196. Cặp cài `adb install -r` code197 → code198 và test
`p5eCode198ReconstructedDbSchemaAndVol5DataReadback` chỉ chứng minh schema/source
ở DB đã dựng lại còn nguyên qua reopen/upgrade trong phạm vi đó. Probe cô lập,
không provider và không chạm pilot DB, dùng evaluation ID lịch sử
`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` đã tái tạo đúng binding
`2e5c80…82520` và run `7d804f…072f0`; đây chỉ là bằng chứng hàm dẫn xuất
identity, không khôi phục attempt/reconciliation row đã mất.

Validation artifact code199 được cài tiếp bằng `adb install -r`; test APK sạch có
SHA-256 `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456`.
Đây chỉ là kiểm chứng code path trên DB dựng lại, không thay đổi kết luận bảo
tồn dữ liệu lịch sử.

Do gate `PILOT_DATA_PRESERVED` không đạt, P5E không tạo authorization mới và
không dispatch provider. `P5E_LIVE_PREP_BLOCKED_PILOT_DATA_PRESERVATION` là
trạng thái hiện tại; không ghi `P5E_LIVE_RAW_ACCEPTANCE_PASS`.

## Phân biệt deadline

`generation_time=9,642 ms` là duration do provider ghi cho generation. Nó không
phải app-owned provider-call deadline. App-owned deadline của lần P5E mới là
`300,000 ms` và được dùng bởi client cho network call/body read. Nếu có cleanup
sau terminal timeout, `terminal_cleanup_elapsed_ms` phải ghi riêng; cleanup
không được cộng vào provider-call duration và không được biến thành late commit.

## Claims đã sửa

- `PROCESS_RESTART_RECOVERY_VERIFIED` không còn được dùng như current claim.
  Bằng chứng code196 chỉ chứng minh DB reopen/stale-claim path trong cùng test
  invocation; tên evidence đúng là `DB_REOPEN_STALE_CLAIM_RECOVERY_VERIFIED`.
  Chỉ một cặp invocation/process thật mới đủ để phục hồi claim process-restart.
- `RECONCILE` hiện ở trạng thái `RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED`.
- Không ghi `RAW_LIVE_RETRY_READY` trước khi generation này được đối soát và
  preflight contract mới pass.
- Code189/code191 và các authorization đã consumed chỉ được dẫn như lịch sử;
  không được reuse.

Nguồn tham chiếu capability được kiểm tra trước P5E: [GPT-5.6 Luna model page](https://openrouter.ai/openai/gpt-5.6-luna-20260709),
[OpenRouter Structured Outputs](https://openrouter.ai/docs/guides/features/structured-outputs),
và [OpenRouter generation metadata API](https://openrouter.ai/docs/api/api-reference/generations/get-generation).
