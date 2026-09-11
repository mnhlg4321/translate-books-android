# P5E — Đối soát generation, quyết định dữ liệu và local acceptance gate

Ngày ghi nhận: `2026-09-11` (+07:00)
Phạm vi: metadata OpenRouter được đọc qua Activity/Logs đã xác thực; không mở
I/O logging và không lưu prompt, response body, source text hoặc secret.

## Current baseline tại đầu báo cáo

| Hạng mục | Giá trị |
|---|---|
| Branch | `feature/v4.18` |
| HEAD hiện tại trước documentation snapshot | `f90c0372c019c0d3970efb298b64b6a4addcfd4f` (test-source alignment commit; implementation correction is `049e72b5769f8b3fdcb6f50646d1f0ead3043940`) |
| HEAD trước nhóm fresh-pilot evidence | `cd683503c79e03e4a215596855458c11200104ab` (historical group start) |
| Production implementation baseline hiện tại | `049e72b5769f8b3fdcb6f50646d1f0ead3043940` |
| Production source commit trong APK code199 | `03b97a30885393c1cc8a3297d5dff9672dcba57e` |
| Production source commit trong pre-patch APK code201 | `a4b4a8f9d215578d5bfae329b1b4608927d0476c` |
| Production source commit trong candidate APK code202 | `4140651d860e4ee11ce7e074970761666c575594` |
| Test-source commit của candidate-aligned test APK | `f90c0372c019c0d3970efb298b64b6a4addcfd4f` |
| Candidate archived validation artifact | `v4.17-p5e.10 / versionCode 206`, `build-20260911-183523`; chưa cài trên device |
| Candidate production APK SHA-256 | `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080` |
| Candidate source ZIP SHA-256 | `5BA76881BA87313C69B3E6221676487A88FDFD9A68D61860521131EABC03E4B8` |
| Current installed validation package | `v4.17-p5e.6 / versionCode 202`, historical installed candidate; code204 chưa cài |
| Last-known original pilot predecessor | `code196 / schema v24`; historical and unavailable, not current data |
| Pre-upgrade code199 production APK SHA-256 | `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09` |
| Current installed code202 production APK SHA-256 | `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0` (historical installed artifact) |
| Candidate-aligned test APK SHA-256 | `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2`, test-source commit `f90c0372c019c0d3970efb298b64b6a4addcfd4f`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; compiled, not installed |
| Pre-candidate clean test APK SHA-256 | `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456`; historical attribution only |
| Package / test package | `com.ml.tblandroidtxt` / `com.ml.tblandroidtxt.test` |
| Candidate/installed APK certificate SHA-256 | `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Device package signature token | `abebea4b` (`dumpsys package`, short token) |
| Current database schema | `v24` |
| Current data state | `RECONSTRUCTED_ONLY`; fresh pilot lineage appended; original code196 rows unavailable |
| Device | `15e84958` |
| Canonical/final schema changes | `NONE` |

Code `189` và code `191` là historical evidence, không phải current baseline.
Code `196`/schema `v24` là last-known original-data path và là historical
predecessor; nó không còn là current installed artifact. Current installed
code199/schema v24 chỉ là validation DB đã dựng lại, nên không được gọi là
preserved history.

## Identity model correction — P5E.9A-EVAL

Compatibility evaluation không phải là một fact bất biến ở cấp canonical pack.
Đó là một trusted-evaluation record bất biến, gắn với một import cụ thể; cùng
canonical pack/profile và cùng ngữ cảnh semantic không làm cho hai evaluation ID
trở thành interchangeable. Exact evaluation ID được official setup chọn và
được freeze trong từng P4 binding.

| Tầng | Identity | Tính chất |
|---|---|---|
| Nội dung pack | Canonical pack hash | Có thể giống nhau khi canonical bytes/data giống nhau |
| Ngữ cảnh đánh giá | Evaluation context fingerprint | Có thể giống giữa các evaluation nếu semantic context giống |
| Bản ghi đánh giá | Evaluation ID | Import-scoped, append-only/bất biến; không interchangeable |

Fresh binding hiện hành sử dụng evaluation chính thức
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`, do import
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987` tạo. Evaluation lịch sử
`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` vẫn được giữ nguyên
trong bằng chứng P5D tại thời điểm đó; tài liệu này không khẳng định nó không
tồn tại ở lịch sử bên ngoài và không dùng nó làm pack fact cho fresh lineage.

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
Pre-candidate clean test APK hash
`501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456` thuộc
test-source commit `914820d3c91ae8df5cc6b2769df7b2d036585f7a`, package
`com.ml.tblandroidtxt.test`, cùng debug certificate; versionCode test APK là
`N/A` theo manifest instrumentation. Nó là historical evidence cho build
trước candidate. Candidate-aligned test APK cuối có SHA-256
`63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC`,
test-source commit `424278e44c042b882d1888f45d5c4b5b944e0dca`, cùng certificate;
manifest/install guard private được lưu ngoài Git. Current production package
is candidate code202; device/data readback claims above apply to that package.
Fresh-pilot QA below is candidate-aligned and complete locally.

Do gate lịch sử `PILOT_DATA_PRESERVED` không đạt, P5E không tự tạo authorization
mới và không dispatch provider. G1 backup/restore của reconstructed data, G2
candidate/data readback, fresh project/run/binding setup và candidate-aligned
fake E2E đã đạt local-only. Compatibility evaluation là trusted-evaluation
record gắn với import cụ thể; fresh setup đã freeze evaluation
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`, không phải một pack
fact có thể thay thế bằng evaluation ID khác. Điều này mở readiness của fresh lineage để xin một
authorization RAW riêng, không phục hồi gate lịch sử và không ghi
`P5E_LIVE_RAW_ACCEPTANCE_PASS`.

## Fresh-pilot G3/G4 — local verification trên candidate

G3 đã dùng đường setup chính thức để tạo lineage mới, không sao chép attempt,
receipt hoặc authorization cũ:

```text
selector:          p5e-fresh-mercedes-vol5-20260911-01
binding:           845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
run declaration:   8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
old selector:      p5d-raw-mercedes-vol5-001
old binding:       d1f854f3d723d8326dc4e99aa7f750524b4b1c223f20ff8e101557f5c0052a14
schema:             v24
current p5c auth/reconcile: 0 / 0 / 0
```

Fresh source bytes retain the pinned RAW hash
`A308210ECA80557CFA9FEC7ED55B2EE3DE5C1C4776E59B2B5EDBF0EFB04504BE`, the
existing PRONOUN BOM/semantic rule, and the canonical pack/profile hashes.
The fresh snapshot/readback is private at
`D:\P5E-private\fresh-pilot-20260911-060059\fresh-pilot-snapshot`, manifest
SHA-256 `2BEA88D4B562DFFA0CEAE401E1BFA0ED50B353CCD014AA9840F15CE1FA7F35EF`;
it is classified `RECONSTRUCTED_ONLY_FRESH_PILOT`, not code196 recovery.

G4 used candidate-aligned test APK `63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC`
and direct `adb shell am instrument`, not `connectedDebugAndroidTest`:

| Case | Result | Scope |
|---|---|---|
| Fresh setup/readback and old-lineage non-reuse | PASS | Current reconstructed DB; source/pack/binding invariants preserved |
| Compact RAW success, exact materialization, empty changes, replay | PASS | Isolated DB/storage; one fake call, replay zero fake calls |
| Fault/STOP, no partial commit, no repair | PASS | Isolated DB/storage; no report/receipt/reconcile rows |
| Malformed/truncated/oversized/unknown/change/replay parser negatives | PASS | Isolated parser fixture; no current DB attempt |
| Instrumentation total | `4/4`, exit `0` | `EditorialP5EFreshPilotInstrumentedTest`, final r3 |

The fake predecessor is not live evidence. Current DB remains at zero P5C
attempts, P5D authorization receipts and reconciliation rows; provider calls and
new authorizations remain zero.

Local readiness labels are now current for the fresh lineage:

```text
FRESH_PILOT_LOCAL_VERIFIED
RAW_AUTHORIZATION_REQUIRED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
```

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
FRESH_PILOT_LOCAL_VERIFIED
```

Fresh pilot phải dùng project/run/binding/attempt identity mới, giữ exact
trusted evaluation record mà official setup đã freeze trong binding, source hash
exact nếu bytes không đổi, authorization mới chỉ ở vòng live sau và backup
SQLite nhất quán có WAL-aware manifest/hash/restore test. Owner approval local-only
đã được ghi nhận; G1/G2/G3/G4 đã thực hiện. Current DB chưa có attempt, receipt
hoặc reconciliation mới; fake predecessor chỉ tồn tại trong isolated QA và không
làm cho authorization cũ khả dụng.

## Fresh pilot rebaseline và readiness — owner-approved local work

Candidate code202 đã được cài một lần sau approval và G1 bằng guard exact; package,
certificate, DB/source/pack readback đạt G2. Candidate-aligned test APK và fresh
binding fake E2E đã hoàn tất local-only trên binding mới; không có live call.

| Baseline | Identity/evidence |
|---|---|
| Candidate | `v4.17-p5e.6 / code202`, source commit `4140651d860e4ee11ce7e074970761666c575594`, APK SHA-256 `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Pre-upgrade installed | `v4.17-p5e.3 / code199`, APK SHA-256 `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09`, schema `v24`, data `RECONSTRUCTED_ONLY` |
| Installed after G2 | `v4.17-p5e.6 / code202`, APK SHA-256 `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, schema `v24`, data `RECONSTRUCTED_ONLY`; current candidate device-verified |
| Test APK attribution | Candidate-aligned APK SHA-256 `63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC`, test-source commit `424278e44c042b882d1888f45d5c4b5b944e0dca`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, direct instrumentation `4/4`; pre-candidate `501653...` remains historical |
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
một lần bằng guard, và package/DB/source/pack readback đạt G2. Fresh setup và
candidate-aligned fake QA tiếp theo đạt G3/G4; không copy attempt/receipt/
authorization cũ. Snapshot fresh có manifest SHA-256
`2BEA88D4B562DFFA0CEAE401E1BFA0ED50B353CCD014AA9840F15CE1FA7F35EF`.

Sau G1/G2/G3/G4, các nhãn local-only dưới đây là evidence của baseline trước
P5E.9A exact-freeze; chúng không phải current P5E.9A pass, không phải live
acceptance và không phục hồi lịch sử code196:

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

## P5E.9A — Initial fresh RAW boundary freeze (superseded evidence)

Nhóm này dừng ở read-only freeze. Không tạo hoặc tiêu thụ authorization, không
tạo attempt, không gọi provider/API, không RECONCILE, không repair/retry và
không chạy `connected*AndroidTest`.

### Baseline hiện hành

| Hạng mục | Candidate mới | Installed trên device |
|---|---|---|
| Source | `a88673ab113c0877119d078c2fb569fd4f39c55a` | code202 historical source `4140651d860e4ee11ce7e074970761666c575594` |
| APK | `v4.17-p5e.8`, code204, event `build-20260911-180303` | `v4.17-p5e.6`, code202 |
| Production APK SHA-256 | `6BECD0F89ABAD308617CBB864AB38B62BCBDAE00E876BE96D57E3AD3FD8F8C83` | `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0` |
| Source ZIP SHA-256 | `C5DCA76D6DC0970A0F18114ADC772AC8F883D63FA825951182BBD269AED614F5` | historical code202 source ZIP |
| Certificate | `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` | same |
| Test APK | `41350151A4AB6AAA8C25A5B7AE0FE71D7297ECD1C3535233D23F11A5B487B144`, test source `a88673ab113c0877119d078c2fb569fd4f39c55a`; compiled outside Git, not installed | code202 test APK evidence is historical |

Candidate code204 is archived symmetrically in `artifacts/builds` and `backup`
with matching APK bytes. It was not installed: the recorded owner approval
pins code202, and the current exact binding freeze failed before an upgrade was
permitted. Code203 and the older test APK are historical/superseded evidence.

### Read-only device freeze

Device/package facts were read again on serial `15e84958`: package
`com.ml.tblandroidtxt`, installed `v4.17-p5e.6`/code202, certificate token
`abebea4b`, schema v24. The app process was absent; DB journal mode was
`delete`, `tbl_android_txt.db-journal` was 0 bytes, and no `-wal`/`-shm` file
was present. The requested selector, binding, run, chapter, source mode,
source hashes/lengths, pack hash and profile hash matched the expected facts;
the row counts were all zero:

```text
attempts=0
authorization_receipts=0
reconciliation=0
reconciliation_history=0
network_lifecycle=0
report_or_receipt=0
```

The freeze nevertheless failed on one exact identity:

```text
expected compatibility_evaluation_id:
f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1
observed compatibility_evaluation_id:
3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
typed result: P5E_9A_DEVICE_FREEZE_BLOCKED_FRESH_EVALUATION_MISMATCH
```

This is a fail-closed identity mismatch, not permission to repin or repair the
row. No SQL was issued and no current DB row was changed. The official
fresh-pilot setup/binding owner must resolve it before the exact preflight can
be rerun.

### Snapshot and local attribution

A new private snapshot was captured with the app stopped, no WAL/SHM sidecar,
and the zero-byte rollback journal retained. It contains the DB, source files
and immutable pack files, but no secret or book content was added to Git or
this report. The manifest is outside Git at
`D:\P5E-private\fresh-raw-boundary-20260911-1810\SNAPSHOT_MANIFEST.json`,
manifest SHA-256 `6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28`.
The snapshot classification is `RECONSTRUCTED_ONLY`; it is not code196
recovery. An isolated copy opened with `integrity_check=ok`, schema 24,
`journal_mode=delete`, matching source/pack hashes and zero lifecycle rows.
This proves data-level SQLite restore only, not current-app restore after an
APK upgrade.

The source patch was validated locally without a provider: latest host engine
tests are `200/200 PASS`, app debug unit tests are `231/231 PASS`, and
AndroidTest compilation passes. The new candidate test APK was not installed,
so no device instrumentation result is attributed to code204. Prior code202
G3/G4 fake/device evidence remains historical and does not close this exact
P5E.9A freeze.

### Decision and stop state

```text
P5E_9A_FRESH_RAW_BOUNDARY_LOCAL_PASS: NOT_REACHED
P5E_9A_DEVICE_FREEZE_BLOCKED_FRESH_EVALUATION_MISMATCH
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_PROVIDER_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
```

This initial freeze is superseded by the P5E.9A-EVAL source correction below.
Its observed `3ce…` row and zero-count result remain valid read-only evidence;
the `f319…` value was a stale runner expectation, not a reason to edit the DB.

## P5E.9A-EVAL — Evaluation provenance correction and zero-call boundary

This section records the current candidate correction. It does not rerun setup,
does not modify the current DB, does not create authorization or attempt rows,
and does not call a provider. The original code196 preservation failure remains
unchanged.

### E1 — Fresh binding/evaluation characterization (read-only)

| Fact | Observed value |
|---|---|
| Selector | `p5e-fresh-mercedes-vol5-20260911-01` |
| Binding identity | `845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf` |
| Run declaration identity | `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc` |
| Chapter | `001` |
| Compatibility evaluation ID | `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` |
| Evaluation context fingerprint | `3d323f1360bab837700295dc28201dabecaec506bbd6b55be7bd6c0484bf6abe` |
| Canonical pack/profile hashes | `497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d` / `beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21` |
| Binding created timestamp | `20260911060059` |
| Evaluation import ID | `3ce8617c-7e75-453c-ac9a-d3ad21eb7987` |
| Evaluation outcome/time | `DATA_COMPATIBLE` / `1789048497413` |
| Import state/storage | `STORED_READY_FOR_CERTIFICATION` / valid stored-ready state (`storage_moved=1`) |
| Import pack identity | `com.ml.tblandroidtxt.editorial.safe4.full / 4.1.3` |
| Run declaration evaluation | same exact `3ce…:compatibility:v1` |

The production `EditorialPackSelectionPolicy.resolve()` semantics select this
trusted evaluation for the canonical pack, and rehydrating the binding row
recomputes the requested binding identity. `f319…:compatibility:v1` is not the
evaluation frozen into this fresh binding; no claim is made about whether that
ID exists elsewhere in history.

Gate: `FRESH_BINDING_EVALUATION_PROVENANCE_CHARACTERIZED` / `NO_DB_MUTATION`.

### E2–E5 — Test-first correction and isolated fixture

The new host test first failed against the old runner constant (`3ce…` expected,
`f319…` actual) with one failure and zero provider calls. The minimal production
patch in implementation commit `049e72b5769f8b3fdcb6f50646d1f0ead3043940` pins
the official `3ce…` record; no SQL, migration, pack, profile or authority was
changed. Candidate-boundary tests now use the same exact ID, and a historical
`f319…` authorization is rejected with typed
`P5E_FRESH_RAW_AUTHORIZATION_MISMATCH` before provider setup.

The isolated fake fixture now relies on the importer-created
`importId + ":compatibility:v1"` evaluation. It asserts that this ID has the
same import namespace and that its binding identity is distinct from the device
binding. It no longer copies an evaluation ID from the current DB or describes
compatibility as a pack-level immutable fact. Generic isolated contract tests
and exact-device read-only checks are separate evidence classes.

### E6–E7 — Local candidate evidence

| Evidence | Result |
|---|---|
| Editorial engine unit suite | `200/200 PASS` |
| App debug/release/benchmark unit variants | `232/232 PASS` each (`696/696` aggregate) |
| AndroidTest Java compilation | `PASS` |
| Debug lint | `PASS` |
| `git diff --check` / secret scan / canonical-pack-profile-authority guard | `PASS` / no matches / `PASS` |
| Provider calls, current authorization/attempt/reconciliation mutation | `0` / `0` / `0` / `0` |
| Production candidate | `v4.17-p5e.10`, versionCode `206`, event `build-20260911-183523`, source commit `049e72b5769f8b3fdcb6f50646d1f0ead3043940` |
| Production APK / certificate | `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080` / `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Source ZIP | `5BA76881BA87313C69B3E6221676487A88FDFD9A68D61860521131EABC03E4B8` |
| Artifact/backup mirror | byte-identical payloads |
| Candidate-aligned test APK | `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2`, test-source commit `f90c0372c019c0d3970efb298b64b6a4addcfd4f`, certificate `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |

The intermediate code205 archive was built before the correction commit was
clean and is retained only as build evidence; it is not an approval candidate.
Code204 remains pre-correction evidence and was never installed. The code206
candidate has not been installed because the earlier code202 approval does not
cover it.

The current device was rechecked read-only as serial `15e84958`, package
`com.ml.tblandroidtxt`, v4.17-p5e.6/code202, signature token `abebea4b`, schema
v24, process absent. Its DB SHA-256 still matches the private reconstructed
snapshot (`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`);
the private snapshot manifest remains `6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28`.
No device install, instrumentation, authorization, attempt, RECONCILE or
provider call occurred in this correction round.

### E8–E11 — Stop boundary

E8 requires a new owner approval pinned to code206, its full APK/certificate
hashes, source commit, fresh selector/binding/run/evaluation and the local-only
limits. Because that approval has not been issued, no snapshot-before-install
decision, guarded upgrade, candidate device readback or E11 direct
instrumentation was performed. Therefore the device portion of P5E.9A remains
pending even though evaluation provenance and host QA are resolved.

```text
P5E_9A_EVALUATION_PROVENANCE_RESOLVED
P5E_9A_FRESH_RAW_BOUNDARY_LOCAL_PASS: NOT_REACHED
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
OWNER_APPROVAL_FOR_CODE206_REQUIRED
```

The single next step is to obtain that exact owner approval. Until then, code206
must remain uninstalled and no RAW authorization block may be created.
