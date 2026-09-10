# P5E — Đối soát generation, quyết định dữ liệu và local acceptance gate

Ngày ghi nhận: `2026-09-11` (+07:00)
Phạm vi: metadata OpenRouter được đọc qua Activity/Logs đã xác thực; không mở
I/O logging và không lưu prompt, response body, source text hoặc secret.

## Current baseline tại đầu báo cáo

| Hạng mục | Giá trị |
|---|---|
| Branch | `feature/v4.18` |
| HEAD trước nhóm fresh-pilot evidence | `cd683503c79e03e4a215596855458c11200104ab` |
| Production implementation baseline trong candidate | `4140651d860e4ee11ce7e074970761666c575594` |
| Production source commit trong APK code199 | `03b97a30885393c1cc8a3297d5dff9672dcba57e` |
| Production source commit trong pre-patch APK code201 | `a4b4a8f9d215578d5bfae329b1b4608927d0476c` |
| Production source commit trong candidate APK code202 | `4140651d860e4ee11ce7e074970761666c575594` |
| Test-source commit của clean test APK | `914820d3c91ae8df5cc6b2769df7b2d036585f7a` |
| Candidate archived validation artifact | `v4.17-p5e.6 / versionCode 202`, `build-20260911-034554` |
| Candidate production APK SHA-256 | `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0` |
| Candidate source ZIP SHA-256 | `D91FE78F99DE6D04CE0DA09C54A76BF030BA40408CF74142B8FC8FF243471A5C` |
| Current installed validation package | `v4.17-p5e.6 / versionCode 202`, installed once after G1 |
| Last-known original pilot predecessor | `code196 / schema v24`; historical and unavailable, not current data |
| Pre-upgrade code199 production APK SHA-256 | `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09` |
| Current code202 production APK SHA-256 | `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0` |
| Test APK SHA-256 | `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456` (pre-candidate clean APK; candidate-aligned test APK pending) |
| Package / test package | `com.ml.tblandroidtxt` / `com.ml.tblandroidtxt.test` |
| Candidate/installed APK certificate SHA-256 | `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
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
RETRY_ELIGIBLE: BLOCKED_CURRENTLY; only after data gate and a new exact-phase single-use authorization
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
history với evidence reference này; primary reconciliation cũ không bị sửa. DB
đó hiện không còn trên device, nên quyết định được giữ ở đây như historical
evidence và không được coi là một durable row hiện tại để cấp quyền retry. Không
có authorization mới được cấp từ quyết định này.

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

Code201 (`v4.17-p5e.5`) là artifact pre-patch, không được dùng làm candidate
sau khi bổ sung hash/certificate pin. Candidate code202 (`v4.17-p5e.6`) được
build bằng `scripts/build-and-save.ps1`, lưu đối xứng ở
`artifacts/builds/v4.17-p5e.6/build-20260911-034554` và
`backup/builds/v4.17-p5e.6/build-20260911-034554`, có APK SHA-256
`8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`, source
ZIP SHA-256 `D91FE78F99DE6D04CE0DA09C54A76BF030BA40408CF74142B8FC8FF243471A5C`
và production source commit `4140651d860e4ee11ce7e074970761666c575594`.
Candidate này đã được cài đúng một lần sau owner approval và G1 bằng guarded
`adb install -r`; package/hash/certificate/signature và G2 data readback đã pass.
Clean pre-candidate test APK hash
`501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456` thuộc
test-source commit `914820d3c91ae8df5cc6b2769df7b2d036585f7a`, package
`com.ml.tblandroidtxt.test`, cùng debug certificate; versionCode test APK là
`N/A` theo manifest instrumentation. Nó không chứng minh candidate-aligned
test source. Current production package is candidate code202; device/data
readback claims above apply to that package, while fresh-pilot QA remains
pending.

Do gate lịch sử `PILOT_DATA_PRESERVED` không đạt, P5E không tạo authorization
mới và không dispatch provider. G1 backup/restore của reconstructed data và G2
candidate/data readback đã đạt; fresh evaluation/run/binding và fake E2E vẫn
chưa chạy. `P5E_LIVE_PREP_BLOCKED_PILOT_DATA_PRESERVATION` vẫn là blocker live;
không ghi `P5E_LIVE_RAW_ACCEPTANCE_PASS`.

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
- Không ghi `RAW_LIVE_RETRY_READY` khi data-preservation gate còn fail; việc đối
  soát generation và local contract pass không tự cấp quyền retry.
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
`connected*AndroidTest` ở `app/build.gradle`. Candidate code202 check-only và
một lần install sau owner approval/G1 đã pass với package/version/APK SHA-256/
certificate/signature token đúng; wrong hash, wrong certificate và thiếu
certificate pin ở build script bị chặn fail-closed. Các check code199 và code196
downgrade trước đó chỉ là negative evidence historical. Không có fallback hay
lần cài thứ hai. Chi tiết và quy trình tách assemble/install/instrumentation ở
`docs/P5E_INSTALL_AND_DATA_PRESERVATION_RUNBOOK.md`.

## Data decision

Chỉ các root `artifacts`/`backup` đã biết được tìm; không có trusted backup của
DB code196. `D:\Ebooks\New folder\metadata.db` không phải app DB. Sau owner
approval, một snapshot WAL-aware của DB reconstructed code199 đã được tạo ngoài
Git và restore thử cô lập đạt; snapshot này không phục hồi lịch sử code196. Do
đó giữ nguyên:

```text
PILOT_DATA_PRESERVATION_FAILED
RECONSTRUCTED_ONLY
FRESH_PILOT_OWNER_APPROVED
BACKUP_RESTORE_G1_PASS
CANDIDATE_UPGRADE_G2_PASS
FRESH_PILOT_SETUP_PENDING
```

Fresh pilot phải dùng evaluation/run/binding/attempt identity mới, source hash
exact nếu bytes không đổi, authorization mới chỉ ở vòng live sau và backup
SQLite nhất quán có WAL-aware manifest/hash/restore test. Owner approval local-only
đã được ghi nhận; G1/G2 đã thực hiện. P5E hiện vẫn chưa khởi tạo fresh identity,
chưa cấp receipt và không làm cho authorization cũ khả dụng.

## Fresh pilot rebaseline và readiness — owner-approved local work

Candidate code202 đã được cài một lần sau approval và G1 bằng guard exact; package,
certificate, DB/source/pack readback đạt G2. Candidate-aligned test APK và fresh
binding fake E2E vẫn chưa hoàn tất.

| Baseline | Identity/evidence |
|---|---|
| Candidate | `v4.17-p5e.6 / code202`, source commit `4140651d860e4ee11ce7e074970761666c575594`, APK SHA-256 `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Pre-upgrade installed | `v4.17-p5e.3 / code199`, APK SHA-256 `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09`, schema `v24`, data `RECONSTRUCTED_ONLY` |
| Installed after G2 | `v4.17-p5e.6 / code202`, APK SHA-256 `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, schema `v24`, data `RECONSTRUCTED_ONLY`; current candidate device-verified |
| Test APK attribution | Pre-candidate clean APK SHA-256 `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456`, test-source commit `914820d3c91ae8df5cc6b2769df7b2d036585f7a`; candidate-aligned test APK pending |
| Historical | code196/schema v24 original pilot data lost; code189/code191 and consumed authorizations remain historical |

### Owner approval đã được ghi nhận — local-only

```text
FRESH_PILOT_OWNER_APPROVAL_REQUEST
HISTORICAL_CODE196_PRESERVATION_FAILED_ACCEPTED
ALLOW_NEW_PILOT_WITHOUT_HISTORICAL_RECOVERY
TARGET_DEVICE=15e84958
TARGET_PACKAGE=com.ml.tblandroidtxt
CANDIDATE_APK=artifacts/builds/v4.17-p5e.6/build-20260911-034554/TranslateBooks-v4.17-p5e.6-code202.apk
CANDIDATE_APK_SHA256=8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0
CANDIDATE_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
ALLOW_UPGRADE_THROUGH_GUARD_ONLY_AFTER_BACKUP_RESTORE_PASS
KEEP_CURRENT_RECONSTRUCTED_DATA_WITHOUT_DELETE_OR_OVERWRITE
NO_PROVIDER_NO_REPAIR_NO_RETRY_NO_RECONCILE
```

Approval được owner gửi và quan sát lúc `2026-09-11T06:00:59+07:00`. Snapshot
private ngoài Git tại `D:\P5E-private\fresh-pilot-20260911-060059` có manifest
SHA-256 `1C495F95B0572458B8405B599A7D11CC80F6770316080A1772EF379A17AC7C64`;
`RESTORE_ISOLATED_PASS` và G1 đã đạt. Candidate code202 sau đó được cài đúng
một lần bằng guard, và package/DB/source/pack readback đạt G2. Bước tiếp theo duy
nhất là tạo fresh evaluation/run/binding bằng selector mới, rồi fake QA; không
copy attempt/receipt/authorization cũ.

Các nhãn sau chỉ là nhãn đề xuất sau khi đủ gate, không phải trạng thái hiện tại:

```text
FRESH_PILOT_LOCAL_VERIFIED
RAW_AUTHORIZATION_REQUIRED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
```

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
| Candidate artifact pin | `scripts/install-validated.ps1`, `scripts/build-and-save.ps1` | code201 red parameter test; code202 check-only match; wrong hash/certificate rejection; missing build certificate pin rejection | Exact APK SHA-256 and certificate are fail-closed before install |

Scope RAW discovery được giữ hẹp: một request chỉ xử lý population tối đa bốn
item theo wire contract. Không dùng cap nhỏ để âm thầm bỏ item; population lớn
dừng typed và cần binding/contract được owner đo và phê duyệt riêng.

Nguồn tham chiếu capability được kiểm tra trước P5E: [GPT-5.6 Luna model page](https://openrouter.ai/openai/gpt-5.6-luna-20260709),
[OpenRouter Structured Outputs](https://openrouter.ai/docs/guides/features/structured-outputs),
và [OpenRouter generation metadata API](https://openrouter.ai/docs/api/api-reference/generations/get-generation).
