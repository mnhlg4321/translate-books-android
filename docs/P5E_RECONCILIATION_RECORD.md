# P5E — Đối soát generation, quyết định dữ liệu và local acceptance gate

Ngày ghi nhận: `2026-09-11` (+07:00)
Phạm vi: metadata OpenRouter được đọc qua Activity/Logs đã xác thực; không mở
I/O logging và không lưu prompt, response body, source text hoặc secret.

## Current baseline tại đầu báo cáo

| Hạng mục | Giá trị |
|---|---|
| Branch | `feature/v4.18` |
| HEAD khi chốt baseline/documentation | `ca9cae929e27a5899f590c29e887459b0e2456ab` |
| Production source commit trong APK code199 | `03b97a30885393c1cc8a3297d5dff9672dcba57e` |
| Test-source commit của clean test APK | `914820d3c91ae8df5cc6b2769df7b2d036585f7a` |
| Current installed validation package | `v4.17-p5e.3 / versionCode 199` |
| Current build event | `build-20260910-211805` |
| Production APK SHA-256 | `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09` |
| Test APK SHA-256 | `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456` |
| Package / test package | `com.ml.tblandroidtxt` / `com.ml.tblandroidtxt.test` |
| APK certificate SHA-256 | `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Device package signature token | `abebea4b` (`dumpsys package`, short token) |
| Current database schema | `v24` |
| Current data state | `RECONSTRUCTED_ONLY`; original code196 rows unavailable |
| Device | `15e84958` |
| Canonical/final schema changes | `NONE` |

Code `189` và code `191` là historical evidence, không phải current baseline.
Code `196`/schema `v24` là last-known original-data path và là historical
predecessor; nó không còn là current installed artifact. Current installed
code199/schema v24 chỉ là validation DB đã dựng lại, nên không được gọi là
preserved history.

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

## Installer investigation và guard result

Evidence daemon `daemon-39212.out.log` ghi đúng lower-version attempt code48 lên
code197 và `INSTALL_FAILED_VERSION_DOWNGRADE`; không có evidence đủ để quy cho
một cleanup actor cụ thể. `daemon-39532.out.log` ghi các test sau đó thiếu
VOL5 DB/fixture/attempt. Repo cũ không có uninstall/clear/fallback trong
`scripts/build-and-save.ps1`, nhưng đường `connectedDebugAndroidTest` của AGP
có thể tự cài APK stale; đây là guard gap đã biết.

P5E bổ sung `scripts/install-validated.ps1` và chặn task
`connected*AndroidTest` ở `app/build.gradle`. Check-only trên device hiện tại
đã pass với package/version/signature đúng; APK code196 bị chặn downgrade và
signature token sai bị chặn. Không cài lại device trong vòng này. Chi tiết và
quy trình tách assemble/install/instrumentation ở
`docs/P5E_INSTALL_AND_DATA_PRESERVATION_RUNBOOK.md`.

## Data decision

Chỉ các root `artifacts`/`backup` đã biết được tìm; không có DB/recovery backup
đáng tin cậy và không có isolated restore test. `D:\Ebooks\New folder\metadata.db`
không phải app DB. Do đó giữ nguyên:

```text
PILOT_DATA_PRESERVATION_FAILED
RECONSTRUCTED_ONLY
FRESH_PILOT_PROPOSAL_ONLY
```

Fresh pilot, nếu owner phê duyệt ở bước sau, phải dùng evaluation/run/binding/
attempt identity mới, source hash exact nếu bytes không đổi, authorization mới
và backup SQLite nhất quán có WAL-aware manifest/hash/restore test. P5E này
không khởi tạo fresh pilot, không cấp receipt và không làm cho authorization cũ
khả dụng.

## Authorization ledger — historical, consumed, never reuse

| Record | Trạng thái | Ghi chú |
|---|---|---|
| P5C VOL4 `p5c-real-mercedes-vol4-001` | Consumed; two ephemeral in-memory auth records, IDs không được persist | Một RAW dispatch; no RECONCILE; xem `docs/P5C_AUTHORIZATION_RECORD.md` |
| `P5D-VOL5-RAW-DIAGNOSTIC-20260909-01` | Consumed once | One primary, zero repair/network retry; `finish=length`/2048; no report/receipt |
| `P5D-VOL5-RAW-ACCEPTANCE-20260909-01` | Consumed once | One primary; deadline/no terminal app result; generation code191; no report/receipt |

Không có authorization P5E mới sau preservation incident. Các record trên chỉ
là historical evidence và không được dùng lại dù code191 đã được phân loại
`EXTERNAL_CONFIRMED_CANCELLED`.

## Compact contract local QA decision

| Requirement | Code | Test/evidence | Kết luận |
|---|---|---|---|
| Full source không được lặp trong wire | `EditorialP5RawWireContract`, `EditorialP5RawWireResponse` | Full shape `49,665` bytes; duplicated source `47,628`; `currentFullShapeWithDuplicatedRawTextExceedsByteBudget` | Legacy shape không chấp nhận được |
| Hard counts/IDs/refs/closed schema | `EditorialP5RawWireContract.validate`, strict parser | Findings/evidence/preserved/disposition limit tests; unknown-field, unsafe-token và >ceiling tests | Fail-closed; worst case `2,785 <= 3,584 < 4,096` |
| Evidence inventory/coverage | Wire validator + local `EditorialLedgerValidator` | Duplicate/orphan evidence, missing population coverage tests | Pass local; external evidence existence vẫn phải do app-owned inventory cung cấp |
| STOP semantics | `EditorialP5PilotExecution` | `rawStopDispositionIsTypedAndNeverCommits` | Typed stop, no predecessor/report/receipt commit |
| Exact RAW materialization | `EditorialP5RawWireResponse.materialize` | No-source compact test, BOM byte round-trip, empty changes | App-owned before=after exact bytes; edits rejected |
| Population không bị silent drop | `EditorialP5PilotExecution` | `rawPopulationBeyondCompactWireLimitStopsBeforeProviderCall` | >4 dừng trước provider; không tự partition/retry |
| Binding/replay/PASS | Existing engine/parser validators | attempt/envelope/replay, wrong binding/phase, model PASS tests | Local pass; no model-owned identity/authority |

Scope RAW discovery được giữ hẹp: một request chỉ xử lý population tối đa bốn
item theo wire contract. Không dùng cap nhỏ để âm thầm bỏ item; population lớn
dừng typed và cần binding/contract được owner đo và phê duyệt riêng.

Nguồn tham chiếu capability được kiểm tra trước P5E: [GPT-5.6 Luna model page](https://openrouter.ai/openai/gpt-5.6-luna-20260709),
[OpenRouter Structured Outputs](https://openrouter.ai/docs/guides/features/structured-outputs),
và [OpenRouter generation metadata API](https://openrouter.ai/docs/api/api-reference/generations/get-generation).
