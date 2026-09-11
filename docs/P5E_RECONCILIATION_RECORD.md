# P5E — Đối soát generation, quyết định dữ liệu và local acceptance gate

Ngày ghi nhận: `2026-09-11` (+07:00)
Phạm vi: metadata OpenRouter được đọc qua Activity/Logs đã xác thực; không mở
I/O logging và không lưu prompt, response body, source text hoặc secret.

## Current active decision — P5E.9B-A3.1 host-only

The latest A2 result is fail-closed, not a readiness pass:
`P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED` /
`P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED`. The redacted A2 evidence
SHA-256 is `42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`.
The A2 single-run approval is consumed and is not reusable. The exact setting
that differed was intentionally not read or logged.

The current task is P5E.9B-A3.1 and is host-only. It may update active status
documentation and add a test-only diagnostic that emits only route booleans;
it may not use ADB, inspect device settings, mutate the current DB, create or
consume authorization/attempt/reconciliation, call a provider, retry, repair
or RECONCILE. P5E.9, A2 and the P5 exit gate remain incomplete. Any A3.2
device action requires a new owner approval and cannot be inferred from A2.

Current implementation baseline is
`a0009f04139431f0bee38d049f9b32e2b6b04c41`; documentation HEAD before this
group is `6a35b2de1ebbb4dcdb6e47cde8d0a1d060781d5e`. Code189/code191 and the
earlier QF results remain historical evidence; code191 remains
`EXTERNAL_CONFIRMED_CANCELLED`, without a `$0` conclusion. The current data
remains `RECONSTRUCTED_ONLY` and the historical preservation failures remain
unchanged.

## Baseline tại đầu nhóm LQ/QF

| Hạng mục | Giá trị |
|---|---|
| Branch | `feature/v4.18` |
| HEAD trước nhóm LQ | `ada38ccbc1034318045cc744598227a1fa8cbcee` |
| HEAD trước documentation snapshot của nhóm QF2 | `6683636e897942ff54f058bd03d22fa5f7a7da04` (production LQ fix `ff6821a`; test-only correction and prior documentation are in this history) |
| HEAD trước nhóm fresh-pilot evidence | `cd683503c79e03e4a215596855458c11200104ab` (historical group start) |
| Production implementation baseline hiện tại | installed code206 source `049e72b5769f8b3fdcb6f50646d1f0ead3043940`; LQ fix commit `ff6821a` |
| Production source commit trong APK code199 | `03b97a30885393c1cc8a3297d5dff9672dcba57e` |
| Production source commit trong pre-patch APK code201 | `a4b4a8f9d215578d5bfae329b1b4608927d0476c` |
| Production source commit trong candidate APK code202 | `4140651d860e4ee11ce7e074970761666c575594` |
| Test-source commit của corrected QF APK (historical) | `34a4ec2832d71a488a2531a0e69a85261e9c9b9b` |
| Candidate validation artifact | `v4.17-p5e.11 / versionCode 207`, `build-20260911-201725`; installed once under separate DV approval and read back successfully |
| Candidate production APK SHA-256 | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` |
| Candidate source ZIP SHA-256 | `B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` |
| Current installed validation package | `v4.17-p5e.11 / versionCode 207`, candidate code207; device readback PASS; code206 là pre-install baseline |
| Last-known original pilot predecessor | `code196 / schema v24`; historical and unavailable, not current data |
| Pre-upgrade code199 production APK SHA-256 | `870CB31186649CE3EF71DA5A58A47DA7877143912DB0BE5BA1D8A4AFB5D3BE09` |
| Pre-install code202 production APK SHA-256 | `8A1E0A2F5031B63B1DE83BEE0AEA639A074F8515E6BCB6430A8D5DB844768CD0` (historical device baseline) |
| Current installed code206 production APK SHA-256 | `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080`; source commit `049e72b5769f8b3fdcb6f50646d1f0ead3043940`; certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Superseded candidate-aligned test APK | `3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2`, test-source commit `f90c0372c019c0d3970efb298b64b6a4addcfd4f`; direct boundary run `4/5`, query defect; not reused |
| Corrected QF test APK SHA-256 (historical) | `68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A`, test-source commit `34a4ec2832d71a488a2531a0e69a85261e9c9b9b`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; separately approved and installed only as `com.ml.tblandroidtxt.test` |
| Superseded LQ candidate-aligned test APK | `9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2`, source snapshot `995d3b6c9678e93905b3802cf22eee0b091b1bb3`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; outside Git, not installed, not approved |
| New QF2 candidate-aligned test APK | `50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587`, test-source commit `f2695c862a9b860e08fd01f932377ec5576d6ad1`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`; outside Git, approved for DV and installed once as the test package only |
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
`2e5c80cc6815935688b68cbe0fa3e9aab6e81520a3464e5115374ad5b7182520` và run `7d804fa125728561c32ac4fa52df44d80f84d7cf8e9207bc69594f155ae072f0`; đây chỉ là bằng chứng hàm dẫn xuất
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
| Test APK attribution | Candidate-aligned APK SHA-256 `63D3093CF68700A563CA979A9D15C3652FD8AB1DE60B219BDB35AE19442F76BC`, test-source commit `424278e44c042b882d1888f45d5c4b5b944e0dca`, certificate SHA-256 `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155`, direct instrumentation `4/4`; pre-candidate `501653AC313DF297BA95C26CA1B80753DED174B01BD754A9422204F98A0C1456` remains historical |
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
Its observed `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` row and
zero-count result remain valid read-only evidence; the
`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` value was a stale
runner expectation, not a reason to edit the DB.

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
| Run declaration evaluation | same exact `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` |

The production `EditorialPackSelectionPolicy.resolve()` semantics select this
trusted evaluation for the canonical pack, and rehydrating the binding row
recomputes the requested binding identity. `f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` is not the
evaluation frozen into this fresh binding; no claim is made about whether that
ID exists elsewhere in history.

Gate: `FRESH_BINDING_EVALUATION_PROVENANCE_CHARACTERIZED` / `NO_DB_MUTATION`.

### E2–E5 — Test-first correction and isolated fixture

The new host test first failed against the old runner constant
(`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` expected,
`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` actual) with one failure and zero provider calls. The minimal production
patch in implementation commit `049e72b5769f8b3fdcb6f50646d1f0ead3043940` pins
the official `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` record; no SQL, migration, pack, profile or authority was
changed. Candidate-boundary tests now use the same exact ID and include the
historical-`f319036d-4d2d-4f47-9cb5-00a9d047dada:compatibility:v1` authorization mismatch assertion; these AndroidTest paths
compile but remain pending candidate-aligned device execution. The intended
typed result is `P5E_FRESH_RAW_AUTHORIZATION_MISMATCH` before provider setup.

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
Code204 remains pre-correction evidence and was never installed. The following
device facts were recorded before the later code206 approval and are retained
as the pre-install baseline.

The pre-install device was read-only as serial `15e84958`, package
`com.ml.tblandroidtxt`, v4.17-p5e.6/code202, signature token `abebea4b`, schema
v24, process absent. Its DB SHA-256 matched the private reconstructed snapshot
(`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`); the
private snapshot manifest remains `6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28`.
No device install, instrumentation, authorization, attempt, RECONCILE or
provider call occurred before the separate owner approval recorded below.

### E8–E11 — Historical pre-approval stop boundary

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

## Historical P5E.9A — owner-approved code206 device run before QF

The owner approval `P5E_9A_CODE206_DEVICE_ZERO_CALL_OWNER_APPROVAL` was then
recorded with the exact device, package, code206 APK/certificate/source pins,
fresh selector/binding/run, and official evaluation
`3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`. Its scope excluded
provider/API calls, authorization creation/consumption, live attempts, retry,
repair, RECONCILE and destructive package/database operations.

### E8–E10 — Snapshot, guarded install and readback

The pinned private snapshot was reused because the current device DB hash still
matched `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391` and
the snapshot manifest hash remained
`6D3949C37E5C6D78FACB5FB7058CB4CBAAE615F54CF07F8F8462D1E39DD75A28`.
The snapshot is `RECONSTRUCTED_ONLY` with data-level restore status
`PASS_DATA_LEVEL_ONLY`; it is not code196 recovery. The exact
`scripts/install-validated.ps1 -CheckOnly` passed package/serial/version/APK
hash/certificate/device-token checks. The same guarded script then performed
exactly one code202 → code206 upgrade and returned install verification PASS.
No fallback, uninstall, clear, reset or downgrade was used.

Post-install readback passed:

```text
device=15e84958
package=com.ml.tblandroidtxt
version=v4.17-p5e.10
versionCode=206
signatureToken=abebea4b
schema=v24
freshSelector=p5e-fresh-mercedes-vol5-20260911-01
freshBinding=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
freshRun=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
freshEvaluation=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
dbSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
```

The superseded test APK was installed directly as
`com.ml.tblandroidtxt.test`; its SHA-256 was
`3838028BC2CE19CBB99B004041383CD5056B6470DA4496A467DBFD75083230B2`, source
commit `f90c0372c019c0d3970efb298b64b6a4addcfd4f`, and certificate matched the
production pin. It is not reused after the `4/5` run below.

### E11 — Candidate-aligned zero-call instrumentation (blocked)

The class `EditorialP5EFreshRawBoundaryInstrumentedTest` was invoked directly
with `adb shell am instrument`, not through `connected*AndroidTest`. It ran five
tests: four passed. The fifth stopped before any provider execution because the
test queried `binding_identity` on `editorial_p5d_reconciliation`; schema v24
defines that table by `attempt_identity`, so the query failed with:

```text
android.database.sqlite.SQLiteException:
no such column: binding_identity
```

The failure is in the candidate-aligned QA predicate at
`EditorialP5EFreshRawBoundaryInstrumentedTest.java:171-172`; it is not a DB
corruption or evaluation-provenance mismatch. The preflight log recorded
request hash `c5920dd842ea92f21d4045c72306a04d59313ac20190c951749fa1b64457c1c2`,
request/context bytes `88604/80317`, and
`providerCalls=0`, `attempts=0`, `authorizations=0`, `reconciliation=0`.
Post-test readback still showed code206, the exact fresh tuple, unchanged DB
SHA-256 and no app/test process left running. The four passing tests covered
strict route rendering, missing/wrong authorization rejection and read-only
fresh baseline; the historical-runner test itself is not accepted as PASS
because it halted on this query error.

No authorization receipt, live attempt, reconciliation row, provider/API call,
partial predecessor, report or receipt was created. The class was not retried.

Historical decision at that time:

```text
P5E_9A_EVALUATION_PROVENANCE_RESOLVED
P5E_9A_CODE206_GUARDED_INSTALL_PASS
P5E_9A_DEVICE_ZERO_CALL_QA_BLOCKED_TEST_QUERY
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
PILOT_DATA_PRESERVATION_FAILED
```

The test-only correction is now committed separately at
`34a4ec2832d71a488a2531a0e69a85261e9c9b9b`. It replaces the faulty predicate
with the required join through `attempt_identity`; AndroidTest compilation
passed and the new test APK was built outside Git:

```text
package=com.ml.tblandroidtxt.test
testSourceCommit=34a4ec2832d71a488a2531a0e69a85261e9c9b9b
testApkSha256=68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
path=D:\P5E-private\fresh-raw-boundary-20260911-qf-test-apk\app-debug-androidTest.apk
```

At that historical stop point the new artifact was not installed and had not
been rerun. The later separate owner approval and QF device rerun are recorded
in the current section below. No RAW authorization may be created from this
historical evidence.

## P5E.9A-QF — current test-query correction and zero-call device result

The owner approved the exact corrected test APK in a separate
`P5E_9A_QF_TEST_APK_OWNER_APPROVAL` block. Only
`com.ml.tblandroidtxt.test` was replaced on device `15e84958`; the production
package was not reinstalled or changed. The installed test package read back
the approved APK hash and certificate:

```text
testPackage=com.ml.tblandroidtxt.test
testApkPath=D:\P5E-private\fresh-raw-boundary-20260911-qf-test-apk\app-debug-androidTest.apk
testApkSha256=68DC191C1F30191AB17407EBDB85B940DD13C7B929CC2333A33F20ECCB415A1A
testSourceCommit=34a4ec2832d71a488a2531a0e69a85261e9c9b9b
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
productionVersionCode=206
productionApkSha256=F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080
deviceSignatureToken=abebea4b
schema=v24
```

The required direct instrumentation order passed without skip:

```text
historicalP5dRunnerRejectsFreshSelectorBeforeProvider: 1/1 PASS
EditorialP5EFreshRawBoundaryInstrumentedTest: 5/5 PASS
```

The class verified the exact fresh selector, chapter, binding, run and
official evaluation `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1`.
The corrected reconciliation assertion uses the schema-valid relationship
`reconciliation.attempt_identity -> attempts.attempt_identity ->
attempts.binding_identity`. The route/preflight and missing/wrong
authorization tests remained local-only; no provider dispatch occurred.

Post-run readback:

```text
productionVersion=v4.17-p5e.10
productionVersionCode=206
productionApkSha256=F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080
productionSignatureToken=abebea4b
dbSha256Before=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbSha256After=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
providerCalls=0
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportOrReceipt=0
partialCommit=false
automaticRedispatch=false
```

Logcat recorded the preflight marker with `providerCalls=0`; no authorization,
attempt, reconciliation, report, receipt, retry, repair or RECONCILE was
created. `HISTORICAL_CODE196_PRESERVATION_FAILED` and
`PILOT_DATA_PRESERVATION_FAILED` remain unchanged. This closes only the
zero-call local RAW boundary; it is not a live RAW acceptance and does not
authorize P5E.9B.

Current QF exit state (historical before LQ):

```text
TEST_ONLY_QUERY_CORRECTION_PASS
ANDROID_TEST_COMPILE_PASS
PINNED_TEST_APK_APPROVED
SINGLE_FAILED_METHOD_RERUN: 1/1_PASS
P5E_FRESH_RAW_BOUNDARY_CLASS: 5/5_PASS
PRODUCTION_CODE206_UNCHANGED
DATABASE_HASH_UNCHANGED
PROVIDER_CALLS: 0
AUTHORIZATIONS: 0
ATTEMPTS: 0
RECONCILIATIONS: 0
P5E_9A_FRESH_RAW_BOUNDARY_LOCAL_PASS
FRESH_RAW_EXACT_PREFLIGHT_READY
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
```

## P5E.9A-LQ — production lineage-query hardening (pre-DV stop, historical)

This section supersedes the QF readiness claim for the purpose of the next
candidate. It does not install an APK, open the device DB, create an
authorization/attempt/reconciliation row, call a provider, retry, repair or
RECONCILE. The prior QF device result remains historical zero-call evidence.

### LQ baseline and RED characterization

| Item | Pinned/observed fact |
|---|---|
| Branch | `feature/v4.18` |
| HEAD before LQ | `ada38ccbc1034318045cc744598227a1fa8cbcee` |
| HEAD before this documentation snapshot | `995d3b6c9678e93905b3802cf22eee0b091b1bb3` |
| Installed production baseline | code206 / APK `F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080` / certificate `47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155` |
| Device/package | `15e84958` / `com.ml.tblandroidtxt` / signature token `abebea4b` |
| Current DB | schema v24 / SHA-256 `3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391` / `RECONSTRUCTED_ONLY` |
| Fresh tuple | selector `p5e-fresh-mercedes-vol5-20260911-01`, chapter `001`, binding `845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf`, run `8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc`, evaluation `3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1` |
| Current counts | attempts/auth/reconciliation/history/lifecycle/report/receipt = `0` in the prior read-only QF evidence; no LQ DB action was performed |
| Historical QF evidence | failed method `1/1`, class `5/5`, provider calls `0`; retained as historical code206 evidence and superseded for readiness |

The historical QF run showed that schema v24 has `attempt_identity` and no
`binding_identity` on `editorial_p5d_reconciliation`; executing the old
production predicate reproduced:

```text
android.database.sqlite.SQLiteException: no such column: binding_identity
```

The failure was before provider construction and that historical run recorded
no provider call. The new disposable-v24 AndroidTest encodes the same PRAGMA
and legacy-predicate characterization and compiles, but it has not been
executed because the new candidate is not yet approved for device installation.
This was a query defect, not current-DB corruption, evaluation drift or a
reason to change schema.

### Minimal production correction

Production commit `ff6821a` (`fix(p5e): harden fresh raw lineage query`) adds a
single package-local read-only `inspectLineage` helper and routes `dispatchRaw`
through it. The helper runs every read before aggregating status:

```text
editorial_p5c_attempts.binding_identity
editorial_p5d_authorization_receipts.binding_identity
editorial_p5d_reconciliation.attempt_identity
  -> editorial_p5c_attempts.attempt_identity
editorial_p5d_reconciliation_history.attempt_identity
  -> editorial_p5c_attempts.attempt_identity
editorial_p5d_network_lifecycle.attempt_identity
  -> editorial_p5c_attempts.attempt_identity
```

Any query/schema/runtime read failure returns typed
`P5E_FRESH_RAW_LINEAGE_CHECK_FAILED`; no SQLite exception escapes the live
runner, no unused-lineage fallback is used, and no provider is constructed.
`UNUSED`/`ALREADY_USED` is computed only after all five reads complete. The QF
test now calls this same helper, so the production and QF predicates cannot
silently drift.

### Isolated regression and host qualification

The new AndroidTest class uses only disposable v24 databases. It covers empty
lineage, each of attempts/authorization/primary reconciliation/history/lifecycle,
unrelated binding, missing table/column typed failure, the historical RED
predicate, canonical chapter `001` versus `chapter001`, and null/wrong
authorization with no mutation. No current pilot DB is used as a mutation
fixture. No schema, migration, pack/profile/authority, compact wire, final
report/receipt, cap or route change was made.

```text
productionFixCommit=ff6821a
testCoverageCommit=995d3b6c9678e93905b3802cf22eee0b091b1bb3
engine=200/200 PASS
appDebug=232/232 PASS
appRelease=232/232 PASS
appBenchmark=232/232 PASS
lintDebug=PASS
androidTestCompile=PASS
gitDiffCheck=PASS
secretScan=no matches
providerCalls=0
currentDbMutation=none
```

The candidate created after the fix is archived but not installed:

```text
version=v4.17-p5e.11
versionCode=207
event=build-20260911-201725
buildSourceSnapshot=995d3b6c9678e93905b3802cf22eee0b091b1bb3
productionFixCommit=ff6821a
apkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
sourceZipSha256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
artifactBackupByteEqual=true
artifact=D:\App Translate Books\App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725
backup=D:\App Translate Books\App Translate Books-translation-profile\backup\builds\v4.17-p5e.11\build-20260911-201725
```

The candidate-aligned test artifact is outside Git and not installed:

```text
testPackage=com.ml.tblandroidtxt.test
testSourceSnapshot=995d3b6c9678e93905b3802cf22eee0b091b1bb3
testApkSha256=9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk
```

### Current decision and stop boundary

The installed device remains code206 and the current reconstructed DB remains
untouched. The new candidate has not been owner-approved for installation, so
device zero-call rerun and valid-authorization local-path proof are not yet
reached. The current LQ state is:

```text
P5E_9A_LQ_HOST_FIX_BUILD_PASS
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_REQUIRED
P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS: NOT_REACHED
NEW_CANDIDATE_OWNER_APPROVAL_REQUIRED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

The single next step is a separate owner approval block pinning the exact
code207 production APK and new test APK, then a later guarded install/device
zero-call rerun. It is not permission to create P5E.9B authorization.

## P5E.9A-LQ-QF2 — code207 test-artifact alignment (pre-DV host-only stop, historical)

QF2 was limited to the AndroidTest artifact. The required starting baseline was
branch `feature/v4.18`, HEAD
`6c546b6f3fba3203fa3e127f502a76ff7e3550d6`, production fix ancestor
`ff6821a5de6f825c55e35f8570dfbf074b4e64b5` and prior test coverage ancestor
`995d3b6c9678e93905b3802cf22eee0b091b1bb3`. The worktree was clean. The
production package, schema, source, pack, profile, authority and fresh
identity were not changed.

### QF2 test-only correction

The characterization found the single stale AndroidTest expectation
`EXPECTED_CANDIDATE_VERSION_CODE = 206L` while the frozen production candidate
is code207. The minimal patch changed it to `207L` and renamed
`schemaFailureIsTypedBeforeProviderAndDoesNotMutate` to
`schemaFailureReturnsCheckFailedAndDoesNotMutate`. No production source,
schema/migration, build/version metadata, canonical fixture, route, compact
wire contract or output cap changed. No `206L` pin remains in the app source,
test source or AndroidTest source search scope.

```text
testOnlyCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
androidTestCompile=PASS
connectedAndroidTest=NOT_RUN
productionApkRebuilt=false
deviceOperationCount=0
adbInstrumentation=NOT_RUN
providerCalls=0
currentDbOpenedOrMutated=false
authorizationCreated=0
attemptCreated=0
reconciliationCreated=0
gitDiffCheck=PASS
secretScan=no matches
```

The new test artifact was copied outside Git into a new private directory:

```text
testPackage=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
targetCandidateVersion=v4.17-p5e.11
targetCandidateVersionCode=207
testRunner=androidx.test.runner.AndroidJUnitRunner
testSourceCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkBytes=1313798
testApkLastWrite=2026-09-11 20:42:59 +07:00
testArtifactCaptured=2026-09-11 20:43:14 +07:00
testApkVersionCodeMetadata=not_present_in_androidTest_manifest
deviceInstalled=false
ownerApproved=false
```

The previous LQ test artifact is explicitly superseded and was not installed:

```text
supersededTestApkPath=D:\P5E-private\fresh-raw-lineage-lq-20260911-2018-test-apk\app-debug-androidTest.apk
supersededTestApkSha256=9DE2A9F167960A2DA0D5D523A270F35459773F601D0CB84872432B9B576227B2
supersededTestSourceCommit=995d3b6c9678e93905b3802cf22eee0b091b1bb3
supersededStatus=SUPERSEDED_NOT_APPROVED_NOT_INSTALLED
```

### Frozen production candidate revalidation

The code207 production candidate was hashed again without rebuilding. Artifact
and backup APKs and source ZIPs remain byte-identical:

```text
productionVersion=v4.17-p5e.11
productionVersionCode=207
productionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
productionSourceZipSha256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
productionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
artifactApkPath=D:\App Translate Books\App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725\TranslateBooks-v4.17-p5e.11-code207.apk
backupApkPath=D:\App Translate Books\App Translate Books-translation-profile\backup\builds\v4.17-p5e.11\build-20260911-201725\TranslateBooks-v4.17-p5e.11-code207.apk
artifactBackupByteEqual=true
productionRebuild=false
```

The device remains recorded at the prior owner-approved code206 baseline with
schema v24, signature token `abebea4b` and DB SHA-256
`3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391`. QF2 did
not read, install, replace, uninstall or instrument the device, so this is a
pinned pre-existing device fact rather than new device evidence. The current
data classification remains `RECONSTRUCTED_ONLY`; historical
`HISTORICAL_CODE196_PRESERVATION_FAILED` and
`PILOT_DATA_PRESERVATION_FAILED` are unchanged.

### New test-artifact approval request

This is an approval request, not an approval and not a RAW authorization:

```text
P5E_9A_LQ_QF2_TEST_APK_OWNER_APPROVAL_REQUEST
TARGET_DEVICE=15e84958
TARGET_PRODUCTION_PACKAGE=com.ml.tblandroidtxt
INSTALLED_VERSION_CODE=206
INSTALLED_PRODUCTION_APK_SHA256=F561800EBCC436CC921F591B2CE7C9171E8E0C430F1B291C83986980C7E98080
INSTALLED_PRODUCTION_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
INSTALLED_DEVICE_SIGNATURE_TOKEN=abebea4b
INSTALLED_SCHEMA_VERSION=24
CURRENT_DB_SHA256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
CANDIDATE_VERSION=v4.17-p5e.11
CANDIDATE_VERSION_CODE=207
CANDIDATE_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
CANDIDATE_SOURCE_ZIP_SHA256=B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348
CANDIDATE_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
FRESH_SELECTOR=p5e-fresh-mercedes-vol5-20260911-01
FRESH_CHAPTER_KEY=001
FRESH_BINDING=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
FRESH_RUN_DECLARATION=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
FRESH_EVALUATION=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
TEST_PACKAGE=com.ml.tblandroidtxt.test
TEST_APK_PATH=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
TEST_APK_SHA256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
TEST_SOURCE_COMMIT=f2695c862a9b860e08fd01f932377ec5576d6ad1
TEST_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
TEST_RUNNER=androidx.test.runner.AndroidJUnitRunner
ALLOW_TEST_PACKAGE_REPLACEMENT_ONLY=true
ALLOW_DIRECT_AM_INSTRUMENT_ONLY=true
ALLOW_SINGLE_METHOD_THEN_CLASS_ONLY=true
FORBID_PROVIDER_API=true
FORBID_AUTHORIZATION_ATTEMPT_RECONCILIATION=true
FORBID_RETRY_REPAIR=true
FORBID_PRODUCTION_INSTALL_UNINSTALL_CLEAR_RESET_DOWNGRADE=true
FORBID_CONNECTED_ANDROID_TEST=true
FORBID_CURRENT_DB_MUTATION=true
```

### QF2 exit state

```text
P5E_9A_LQ_TEST_VERSION_PIN_CORRECTED
CODE207_PRODUCTION_CANDIDATE_UNCHANGED
CODE207_ALIGNED_TEST_APK_BUILT
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_REQUIRED
NEW_TEST_ARTIFACT_OWNER_APPROVAL_REQUIRED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

Do not mark this as `DEVICE_HELPER_PASS`,
`VALID_AUTHORIZATION_LOCAL_PATH_PASS` or
`FRESH_RAW_EXACT_PREFLIGHT_READY`. Stop here. The next action is separate owner
approval for the exact new test artifact, followed by a future device helper
rerun. P5E.9B authorization remains uncreated.

## P5E.9A-LQ-DV — code207 device verification and zero-call boundary

The owner approval `P5E_9A_LQ_DV_OWNER_APPROVAL` was used only for the stated
local/device scope. One guarded production upgrade changed code206 to code207,
and one exact-hash replacement installed the approved test package. No
uninstall, clear, reset, downgrade, fallback, connected AndroidTest,
provider/API call, authorization, attempt, reconciliation, retry, repair or
RECONCILE occurred.

```text
device=15e84958
productionPackage=com.ml.tblandroidtxt
installedProductionVersion=v4.17-p5e.11
installedProductionVersionCode=207
installedProductionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
installedProductionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
deviceSignatureToken=abebea4b
schemaVersion=24
dataClassification=RECONSTRUCTED_ONLY
productionUpgradeCount=1
testPackageReplacementCount=1
connectedAndroidTest=NOT_RUN
testPackage=com.ml.tblandroidtxt.test
testApkPath=D:\P5E-private\fresh-raw-lineage-lq-qf2-20260911-204314-test-apk\app-debug-androidTest.apk
testApkSha256=50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testSourceCommit=f2695c862a9b860e08fd01f932377ec5576d6ad1
testRunner=androidx.test.runner.AndroidJUnitRunner
```

The one approved force-stop was used for snapshot capture only. The
WAL-aware snapshot and isolated restore remain outside Git:

```text
snapshotRoot=D:\P5E-private\p5e-9a-lq-dv-snapshot-20260911-210256
snapshotManifestSha256=BBE47271716785B1ED89F888748428C9A0437A47FF61804942FAF202BBE49C76
snapshotDatabaseSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
snapshotJournalSha256=E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855
snapshotWalPresent=false
snapshotShmPresent=false
snapshotRestoreRoot=D:\P5E-private\p5e-9a-lq-dv-restore-20260911-210256
snapshotRestoreStatus=PASS_DATA_LEVEL_ONLY
postInstallReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postinstall-20260911-210550
postRunReadbackRoot=D:\P5E-private\p5e-9a-lq-dv-postrun-verified2-20260911-211156
```

The isolated restore and post-run SQLite checks passed `integrity_check=ok`,
schema v24 and zero foreign-key violations. The fresh tuple and source,
canonical pack and profile identities were unchanged. The fresh project has
one canonical chapter `001`; two global `001` rows remain because another
reconstructed project is retained.

```text
dbBeforeSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbAfterSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
databaseHashUnchanged=true
freshSelector=p5e-fresh-mercedes-vol5-20260911-01
freshChapterKey=001
freshBinding=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
freshRunDeclaration=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
freshEvaluation=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
freshPackSha256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
freshProfileSha256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
freshProjectChapterRows=1
globalChapterKey001Rows=2
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportBytesNonEmpty=0
receiptBytesNonEmpty=0
partialCommit=false
automaticRedispatch=false
providerCalls=0
```

The direct device order and results were:

```text
schemaV24EmptyFreshLineageIsUnusedAndAllReadsComplete=1/1 PASS
missingTableReturnsTypedCheckFailure=1/1 PASS
canonicalChapterKeyIsAcceptedAndFriendlyAliasIsRejectedBeforeProvider=1/1 PASS
historicalP5dRunnerRejectsFreshSelectorBeforeProvider=1/1 PASS
EditorialP5EFreshRawBoundaryInstrumentedTest=5/5 PASS
EditorialP5EFreshRawLineageInstrumentedTest=13/13 PASS
allTestsNoSkip=true
providerCalls=0
```

Lineage mutation cases used disposable databases. The current pilot DB was
used only for read-only tuple, preflight and count assertions. The QF tests
call the production-owned helper, whose schema-v24 reconciliation, history
and lifecycle reads use the canonical `attempt_identity` joins and map query
or schema failures to typed `P5E_FRESH_RAW_LINEAGE_CHECK_FAILED`. No partial
commit or automatic redispatch occurred.

### Current decision

```text
P5E_9A_LQ_TEST_VERSION_PIN_CORRECTED
CODE207_PRODUCTION_CANDIDATE_UNCHANGED
CODE207_DEVICE_VERIFIED
P5E_9A_QF_ZERO_CALL_BOUNDARY_PASS
P5E_9A_LQ_PRODUCTION_LINEAGE_QUERY_FIX_PASS
P5E_9A_LQ_DEVICE_ZERO_CALL_PASS
P5E_9A_LQ_DEVICE_HELPER_EXECUTION_PASS
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
```

`P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS` is not claimed because the
approved scope forbade valid-authorization dispatch. `FRESH_RAW_EXACT_PREFLIGHT_READY`
is not used to replace that missing evidence. The next step is to prepare an
exact P5E.9B RAW authorization block for separate owner approval; this run did
not create, consume or dispatch one.

## P5E.9B-A1 host-only fresh RAW live harness

This section is the current A1 continuation and does not rewrite the historical
code196, code191, code199, code202, code206 or QF2 evidence above. A1 was
explicitly host-only. It performed no ADB/device operation, no APK install, no
current-DB read or write, no provider/API call, no authorization creation or
consumption, no attempt creation, no preflight execution on device and no
RECONCILE.

### Baseline and change boundary

~~~
branch=feature/v4.18
implementationHeadBeforeDocumentation=a0009f04139431f0bee38d049f9b32e2b6b04c41
productionSourceCandidate=v4.17-p5e.11/code207
productionApkSha256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
productionCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
currentDeviceBaseline=15e84958/code207/schema24
currentDbSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dataClassification=RECONSTRUCTED_ONLY
providerCalls=0
deviceOperations=0
~~~

The test-only commit is
a0009f04139431f0bee38d049f9b32e2b6b04c41. Its exact changed files are:

~~~
app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EFreshRawBoundaryInstrumentedTest.java
app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EFreshRawLiveInstrumentedTest.java
~~~

The first file only exposes existing test fixture data to the new harness. The
second file is the new instrumented harness. Production diff is zero: no
app/src/main file, engine main, schema, migration, manifest, build/version
metadata, canonical pack/profile/authority, routing policy or wire contract
was changed.

### Harness contract

freshRawExactPreflightRunsOnlyWhenExplicitlyOptedIn checks
p5e_fresh_raw_preflight=YES as its first executable operation. Without that
argument it is an approved skip. It performs exact argument and artifact
checks, reads the current binding/source/settings, and calls only the existing
production preflightOnly method. It never constructs an authorization and
never calls dispatchRaw or executeRaw.

authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn checks the distinct
p5e_fresh_raw_live=YES opt-in as its first executable operation. It was not
run in A1. Before any runtime authorization construction it requires the full
authorization ID and hash, owner manifest hash, expected attempt/request/
envelope/body/route/account fingerprints, exact fresh tuple, exact route and
all budget/deadline/stop-policy facts. It recomputes the production preflight,
requires UNUSED lineage with zero counts, then dispatches only through the
fresh RAW runner. There are no live-field defaults, old P5D selector paths,
retry, repair, response healing, fallback or RECONCILE paths.

The harness reuses production EditorialP5EFreshRawLiveRunner.preflightOnly,
EditorialP5EFreshRawLiveRunner.inspectLineage,
EditorialP5EFreshRawRoutingPolicy and the existing test fixture. It does not
duplicate lineage SQL or route policy. Its evidence manifest is redacted and
contains hashes, lengths, identities, visibility, route facts, schema facts,
wire bounds and before/after counts only. It excludes API keys, raw source,
prompt, request body, full response and unredacted credentials.

The egress declaration is explicit: RAW and GLOSSARY are visible; DRAFT and
PRONOUN are hidden; pack authority is required. Pinned source inventory is:

~~~
RAW bytes=23814 sha256=a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be
DRAFT bytes=26462 sha256=64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5
GLOSSARY bytes=3249 sha256=4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314
PRONOUN semanticBytes=452 sha256=4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686
~~~

### Test and artifact evidence

AndroidTest compilation passed with
:app:compileDebugAndroidTestJavaWithJavac --no-daemon. The test APK build
passed with :app:assembleDebugAndroidTest --no-daemon --console=plain; no
production APK was rebuilt and no connected test was invoked. Offline APK
verification passed for package, target package, runner and certificate:

~~~
testApkPath=D:\P5E-private\fresh-raw-live-harness-a1-20260911-233211-test-apk\app-debug-androidTest.apk
testPackage=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
testRunner=androidx.test.runner.AndroidJUnitRunner
testSourceCommit=a0009f04139431f0bee38d049f9b32e2b6b04c41
testApkSha256=697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
testCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
testApkBytes=1323843
testBuildTimestamp=2026-09-11T23:32:11.8261069+07:00
installed=false
~~~

The pre-amend A1 artifact SHA
4DA7EB0A5EB6162B6A2127648B9F26F7CFA21A352A6130B0CA426BBA5F9097FE is
superseded. The QF2 device-QA artifact SHA
50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587 is
historical and is not the live harness artifact.

The frozen production APK and its backup both hash to
2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD.
The production source ZIP and its backup both hash to
B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348.
The mirrored artifact hashes are equal; no code208 or new production build was
created.

### Unissued authorization template

~~~
authorizationId=P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01
authorizationIdSha256=0aa82c5897e3df3ec8a7a1586736dbf184b316c66ec165e95e64e8e4832145eb
phase=L1_RAW_DISCOVERY
provider=openrouter
model=openai/gpt-5.6-luna
upstreamProvider=openai
maximumPrimarySemanticCalls=1
maximumSchemaRepairCalls=0
maximumNetworkRetries=0
maximumInputTokens=100000
maximumOutputTokens=4096
maximumTotalTokens=104096
maximumTotalCostUsd=0.05
maximumExecutionTimeMillis=120000
authorizationValidityWindowMillis=900000
allowChapterToProvider=true
allowFullModelResponseStorage=false
allowRequestBodyStorage=false
evidenceRedactionPolicy=HASH_ONLY
singleUse=true
cancellationStopAuthority=OWNER_CONTROLLED,RAW_ONLY,NO_SCHEMA_REPAIR,NO_AUTOMATIC_RETRY,NO_RECONCILE,NO_RESPONSE_HEALING,NO_FALLBACK,PRESERVE_DURABLE_RECOVERY_STATE
~~~

Route fingerprint:
23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c.
Wire and context bounds are safe4.raw.discovery.wire.v1,
worstCaseWireBytes=2785, maximumWireBytes=3584, outputTokenCap=4096
and contextSizeBytes=80317; byte and token units are not conflated.

OpenRouter capability and pricing were checked at
2026-09-11T23:30:58+07:00 on the official model page. The observed values
were JSON Schema structured-output support, $0.20/M input and $1.20/M
output. The proposed authorization cap remains $0.05; no billing conclusion
is inferred from this reference.

### A2 owner approval request

~~~
P5E_9B_A2_ZERO_CALL_PREFLIGHT_OWNER_APPROVAL_REQUEST
TARGET_DEVICE=15e84958
TARGET_PRODUCTION_PACKAGE=com.ml.tblandroidtxt
INSTALLED_PRODUCTION_VERSION=v4.17-p5e.11
INSTALLED_PRODUCTION_VERSION_CODE=207
INSTALLED_PRODUCTION_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
INSTALLED_PRODUCTION_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
INSTALLED_DEVICE_SIGNATURE_TOKEN=abebea4b
CURRENT_DB_SHA256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
CURRENT_DB_SCHEMA=24
FRESH_SELECTOR=p5e-fresh-mercedes-vol5-20260911-01
FRESH_CHAPTER_KEY=001
FRESH_BINDING=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
FRESH_RUN_DECLARATION=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
FRESH_EVALUATION=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
FRESH_PACK_SHA256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
FRESH_PROFILE_SHA256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
TEST_APK_PATH=D:\P5E-private\fresh-raw-live-harness-a1-20260911-233211-test-apk\app-debug-androidTest.apk
TEST_APK_SHA256=697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
TEST_APK_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
TEST_SOURCE_COMMIT=a0009f04139431f0bee38d049f9b32e2b6b04c41
TEST_PACKAGE=com.ml.tblandroidtxt.test
TEST_RUNNER=androidx.test.runner.AndroidJUnitRunner
ALLOW_REPLACE_TEST_PACKAGE_ONLY=true
ALLOW_DIRECT_AM_INSTRUMENT_ONLY=true
ALLOW_PREFLIGHT_METHOD_ONLY=true
PREFLIGHT_ARGUMENT=p5e_fresh_raw_preflight=YES
FORBID_LIVE_ARGUMENT=p5e_fresh_raw_live=YES
FORBID_AUTHORIZATION_CREATION_OR_CONSUMPTION=true
FORBID_ATTEMPT=true
FORBID_PROVIDER_API=true
FORBID_RETRY_REPAIR_RECONCILE=true
FORBID_CURRENT_DB_MUTATION=true
FORBID_CONNECTED_ANDROID_TEST=true
FORBID_PRODUCTION_PACKAGE_INSTALL=true
~~~

Requested owner scope is only test-package replacement and direct preflight
instrumentation. It explicitly forbids the live opt-in, authorization,
attempt, provider, retry, repair, RECONCILE and production-package operation.

### A1 decision

~~~
P5E_9A_LQ_DV_COMPLETE
P5E_9B_A1_LIVE_HARNESS_HOST_PASS
P5E_9B_AUTHORIZATION_TEMPLATE_PREPARED_NOT_ISSUED
P5E_9B_TEST_APK_BUILT_NOT_INSTALLED
P5E_9B_A2_ZERO_CALL_PREFLIGHT_OWNER_APPROVAL_REQUIRED
P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS: NOT_REACHED
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
~~~

## P5E.9B-A2 zero-call preflight result (historical runtime evidence)

The owner-approved A2 scope allowed one test-package replacement and one direct
preflight invocation. It did not allow live dispatch, authorization,
attempt/lifecycle creation, provider/API access, retry, repair, RECONCILE or
production-package installation. The production package was not changed.

### Bounded execution and failure

The old test package was confirmed present with SHA-256
50BC25F1C24E9588F430EE00809E9B6C8E126B5EA975782FA556254840DDA587. The new
private artifact was then installed once into com.ml.tblandroidtxt.test:

~~~
targetTestApk=D:\P5E-private\fresh-raw-live-harness-a1-20260911-233211-test-apk\app-debug-androidTest.apk
targetTestApkSha256=697B2C0E58E206A2E067A5D3E71458963B3940280B245BBF48D21B2B0256085A
targetTestCertificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
targetTestPackage=com.ml.tblandroidtxt.test
targetTestRunner=androidx.test.runner.AndroidJUnitRunner
installOperation=adb install -r once
productionInstallOperations=0
~~~

The exact direct invocation used the fresh preflight class and method with
p5e_fresh_raw_preflight=YES; no live argument was supplied. It produced one
test assertion failure and no pass:

~~~
testsRun=1
failures=1
instrumentationCode=-1
providerCalls=0
failure=AssertionError: current settings must select the fresh RAW route
failureLine=EditorialP5EFreshRawLiveInstrumentedTest.java:149
evidencePath=D:\P5E-private\p5e-9b-a2-preflight-20260912-0008\instrumentation-output.txt
evidenceSha256=42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A
~~~

This is recorded as a route-precondition failure at
EditorialP5EFreshRawRoutingPolicy.matches(SettingsStore.load(target)). The
failure occurred before production preflightOnly and before request/body
construction. It does not establish which individual setting differs. No
settings correction, API-key readback, source/prompt logging, provider call or
second instrumentation run was performed.

### Post-run preservation evidence

Read-only database extraction and local SQLite verification confirmed:

~~~
productionVersionCode=207
productionSignatureToken=abebea4b
dbBeforeSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
dbAfterSha256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
databaseHashUnchanged=true
schemaVersion=24
integrityCheck=ok
foreignKeyViolations=0
attempts=0
authorizationReceipts=0
reconciliation=0
reconciliationHistory=0
lifecycle=0
reportBytesNonEmpty=0
receiptBytesNonEmpty=0
partialCommit=false
automaticRedispatch=false
~~~

The exact fresh binding/read-only rows remain:

~~~
selector=p5e-fresh-mercedes-vol5-20260911-01
chapterKey=001
binding=845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf
runDeclaration=8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc
evaluation=3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1
packSha256=497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d
profileSha256=beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21
~~~

### Current decision

~~~
P5E_9B_A2_ZERO_CALL_PREFLIGHT_STOPPED
P5E_9B_A2_FRESH_RAW_ROUTE_PRECONDITION_FAILED
P5E_9B_A1_LIVE_HARNESS_HOST_PASS
P5E_9B_AUTHORIZATION_TEMPLATE_PREPARED_NOT_ISSUED
P5E_9B_TEST_APK_BUILT_AND_INSTALLED_FOR_A2_ONLY
P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS: NOT_REACHED
FRESH_RAW_EXACT_PREFLIGHT_READY: NOT_REACHED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
~~~

The single-run A2 approval is not reusable. A new owner decision is required
before any further device or settings action. No live authorization has been
created or consumed.

## P5E.9B-A3.1 — current host-only status synchronization and redacted route diagnostic

### Scope and baseline

The A3.1 group started from branch `feature/v4.18`, documentation baseline
HEAD `6a35b2de1ebbb4dcdb6e47cde8d0a1d060781d5e`, implementation baseline
`a0009f04139431f0bee38d049f9b32e2b6b04c41` and a clean worktree. No ADB,
device setting read, current-DB read/write, instrumentation, provider/API call,
authorization, attempt or reconciliation occurred. The installed code207 and
schema-v24 facts remain prior read-only evidence; they were not revalidated in
this host-only group.

The A2 result remains a typed fail-closed route-precondition stop. Its
redacted evidence SHA-256 is
`42BAA89A70392DDA11868C3FF11EED18D602DFBDC878207E349EEB55A07EEB8A`. The A2
single-run approval is consumed and was not reused. No setting value was read
or logged to identify the mismatch.

### PreTag false-green guard

The top-level v4.18 checklist keeps current P5/P5E closure unchecked because
there is no accepted RAW predecessor, REPORT_L1 or receipt. Running
`scripts/verify-release-workflow.ps1 -ChecklistPath release_checklists/v4.18-editorial-v5-safe-4-1-3.md -Gate PreTag -ExpectedVersion 4.18`
failed closed with `Step 09 is not complete for gate PreTag`. No
`P5E_WORKFLOW_GATE_FALSE_GREEN` condition was observed; no tag or release was
attempted.

### Test-only diagnostic

Test-only commit:
`89eef75a4a62e5674d02b7e48eaaff012d9a7ae0`.

`EditorialP5EFreshRawRouteDiagnosticInstrumentedTest` has a separate opt-in
method `persistedRawRouteDiagnosticRunsOnlyWhenExplicitlyOptedIn`. Its first
executable statement checks
`p5e_fresh_raw_route_diagnostic=YES`. Only after that check does it call
`SettingsStore.load` once. It computes and emits only
`providerMatch`, `modelMatch`, `endpointMatch` and `routeMatch`; the latter is
the conjunction of the three component booleans and is checked against the
production-owned route predicate. It emits no actual provider/model/endpoint,
API key, source text, prompt, request body or response body. It does not call
`SettingsStore.save`, open the DB, construct a provider/client/request or call
`preflightOnly`.

The same AndroidTest-only source contains synthetic in-memory cases for the
exact route, each individual mismatch, provider/model case behavior, endpoint
trailing-slash and surrounding-whitespace normalization, conjunction
invariance and output allowlisting. They were compiled, not run through
instrumentation in A3.1.

### Build and artifact evidence

`:app:compileDebugAndroidTestJavaWithJavac --no-daemon` and
`:app:assembleDebugAndroidTest --no-daemon --console=plain` both returned
`BUILD SUCCESSFUL`. No production APK was rebuilt. The new private test
artifact is:

~~~
path=D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk
package=com.ml.tblandroidtxt.test
targetPackage=com.ml.tblandroidtxt
runner=androidx.test.runner.AndroidJUnitRunner
sourceCommit=89eef75a4a62e5674d02b7e48eaaff012d9a7ae0
sha256=64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A
certificateSha256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
bytes=1326212
installed=false
~~~

The frozen production artifact was not rebuilt and remains
`2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` in both
artifact and backup. The production source ZIP remains
`B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348` in both
locations.

### A3.2 approval request

The following is an approval request, not an approval and not a RAW
authorization. It must not be inferred from A2.

~~~
P5E_9B_A3_2_ROUTE_DIAGNOSTIC_OWNER_APPROVAL_REQUEST
APPROVAL_STATUS=REQUIRED
APPROVAL_SCOPE_SINGLE_RUN=true
TARGET_DEVICE=15e84958
TARGET_PRODUCTION_PACKAGE=com.ml.tblandroidtxt
INSTALLED_PRODUCTION_VERSION=v4.17-p5e.11
INSTALLED_PRODUCTION_VERSION_CODE=207
INSTALLED_PRODUCTION_APK_SHA256=2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD
INSTALLED_PRODUCTION_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
INSTALLED_DEVICE_SIGNATURE_TOKEN=abebea4b
CURRENT_DB_SHA256=3563F44BCE9E529955B6C39142243F59AF8F2F0D0095303F5C7A66BE07219391
CURRENT_DB_SCHEMA=24
TEST_APK_PATH=D:\P5E-private\fresh-raw-route-diagnostic-a3-20260912-002937-test-apk\app-debug-androidTest.apk
TEST_APK_SHA256=64A9976F43F04397DF0E593F21E7AE154CDED1ED6749294F3B1E5F9D2A77757A
TEST_APK_CERTIFICATE_SHA256=47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155
TEST_SOURCE_COMMIT=89eef75a4a62e5674d02b7e48eaaff012d9a7ae0
TEST_PACKAGE=com.ml.tblandroidtxt.test
TEST_RUNNER=androidx.test.runner.AndroidJUnitRunner
DIAGNOSTIC_CLASS=com.ml.tblandroidtxt.EditorialP5EFreshRawRouteDiagnosticInstrumentedTest
DIAGNOSTIC_METHOD=persistedRawRouteDiagnosticRunsOnlyWhenExplicitlyOptedIn
DIAGNOSTIC_ARGUMENT_NAME=p5e_fresh_raw_route_diagnostic
DIAGNOSTIC_ARGUMENT_VALUE=YES
LIVE_ARGUMENT=ABSENT
ALLOW_REPLACE_EXISTING_TEST_PACKAGE_WITH_ADB_INSTALL_R_ONCE=true
ALLOW_ONE_DIAGNOSTIC_METHOD=true
ALLOW_SINGLE_INVOCATION=true
ALLOW_POST_RUN_READ_ONLY_VERIFICATION=true
PROVIDER_CALL_BUDGET=0
PRODUCTION_PACKAGE_OPERATIONS=0
FORBID_FULL_CLASS=true
FORBID_RERUN=true
FORBID_LIVE_ARGUMENT=true
FORBID_AUTHORIZATION_CREATION_OR_CONSUMPTION=true
FORBID_ATTEMPT_OR_RECONCILIATION=true
FORBID_PROVIDER_API=true
FORBID_CURRENT_DB_MUTATION=true
FORBID_UNINSTALL_CLEAR_RESET_DOWNGRADE=true
FORBID_CONNECTED_ANDROID_TEST=true
FORBID_A2_APPROVAL_REUSE=true
~~~

### Current status after A3.1

~~~
P5E_9B_A3_1_HOST_ONLY_PASS
P5E_9B_ROUTE_DIAGNOSTIC_ARTIFACT_BUILT_NOT_INSTALLED
P5E_9B_A3_2_DEVICE_DIAGNOSTIC_APPROVAL_REQUIRED
P5E_9B_A4_REMEDIATION_NOT_SELECTED
RAW_AUTHORIZATION_REQUIRED
NO_AUTHORIZATION_CREATED
NO_LIVE_CALL_PERFORMED
RECONCILE_BLOCKED_RAW_PREDECESSOR_REQUIRED
EXECUTION_DISABLED
NOT_CERTIFIED
P6_NOT_READY
HISTORICAL_CODE196_PRESERVATION_FAILED
PILOT_DATA_PRESERVATION_FAILED
~~~

`P5E_9A_VALID_AUTHORIZATION_LOCAL_PATH_PASS` and
`FRESH_RAW_EXACT_PREFLIGHT_READY` remain unclaimed. The next and only step is
the separate A3.2 owner decision for the redacted diagnostic. No provider,
authorization, attempt, reconciliation or P6 action is authorized.
