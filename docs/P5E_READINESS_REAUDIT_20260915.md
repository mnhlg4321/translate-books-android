# P5E readiness re-audit — 2026-09-15

Status: `P5E_LOCAL_REPAIR_REQUIRED / OWNER_PACKET_NOT_READY / A4.3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY`

## Kết luận quyết định

Chưa đủ điều kiện chuyển P6 và cũng chưa đủ điều kiện đưa packet A4.3 hiện hành
cho owner duyệt. Có đủ evidence để mở **một work package local tiếp theo trong
P5E**. Work package đó được viết tại `docs/P5E_NEXT_WORK_REQUEST.md`; nó không
phải authorization live.

F2 transport vẫn PASS. Các gate timing/event/path và redaction thêm ở F3 là cải
thiện có giá trị, nhưng nhãn `F3_PROVENANCE_REPAIR_GREEN` phải rút lại. Producer
synthetic và verifier cùng dùng một artifact contract do helper tự dựng, không
khớp bytes do production serializer tạo. Repo cũng chưa có executable live
collector hoặc post-dispatch emitter để tạo bộ input mà verifier yêu cầu. Cuối
cùng, command được hash nhưng không kiểm hash helper ngay trước khi thực thi.

## Provenance cuộc kiểm tra

| Dữ kiện | Nguồn | Kết quả |
|---|---|---|
| Resume branch/HEAD | `git branch --show-current`; `git rev-parse HEAD` | `feature/v4.18-p5e-audit-20260914`; `8c24b7d2a0d4ddc39ff07cfd2220ce14843258d8`; clean trước re-audit |
| Canonical authority | `EDITORIAL_RECOVERY_V4_18.md` | P5/P5E chưa hoàn tất; RAW predecessor chưa có; P6 khóa |
| Release process | `DEVELOPMENT_WORKFLOW.md`; checklist v4.18 | checklist 05–09 còn `[ ]`; không build/tag/archive/merge |
| A4.3 manifest | file + `Get-FileHash` | `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501`; không sửa trong re-audit |
| Command | file + `Get-FileHash` | `1D9A67693C4C4F64AF182300CDEB963A9FD9D44361EF77E91371F88C92EDA15E` |
| Helper | file + `Get-FileHash` | `6FABAE1F53973942561EDA52002F052F775570A931EAB65188103E7DEE9955A2` |
| Frozen APK pins | snapshot/build state/artifact manifests | production code207 `2CCBB8…800FD`; AndroidTest `57EC99…FDEA`; không rebuild/install |
| Dynamic host regression | `scripts/p5e-raw-live-supervisor.ps1 -SelfTest` | PASS: F2 transport, 8 required-argument fixtures, 4 process outcomes, 8 outcome fixtures, redaction; device/provider `0` |
| Prior F3 probe rerun | `docs/P5E_PROVENANCE_REVIEW_PROBE.ps1` | synthetic controls/mutations vẫn cho expected result; chỉ chứng minh internal synthetic contract |
| Re-audit static probe | `docs/P5E_READINESS_REAUDIT_PROBE.ps1`; result JSON | H1–H4 đều được xác nhận; device/provider/credential reads `0` |

Re-audit không đọc credential, biến fingerprint, settings content, source text,
prompt hay model response. Không gọi ADB, instrumentation, provider, runtime
authorization, database live, build hoặc install.

## Bốn blocker mới được chứng minh

### H1 — command không bind helper ở thời điểm chạy

`P5E_RAW_AUTHORIZATION_COMMAND.txt` gọi `& $helperPath -Dispatch` và pin manifest,
production APK, test APK. Nó không chứa expected helper SHA-256 và không chạy
`Get-FileHash` cho helper trước lời gọi. Proposal có ghi helper hash để con người
đọc, nhưng đó không phải fail-closed runtime gate. Nếu helper đổi sau review mà
đường dẫn giữ nguyên, command vẫn gọi file mới.

Ảnh hưởng: owner không thể duyệt một executable chain bất biến chỉ từ ba hash
đang có. Sửa local bắt buộc là pin helper hash trong command, kiểm đúng bytes
trước mọi environment/account/device operation, và có negative fixture cho
helper thay một byte.

### H2 — không có executable live readback collector

Helper chỉ có bốn parameter set: `SelfTest`, `Dispatch`, `VerifyOutcome`,
`ProvenanceProbe`. `Invoke-P5ESyntheticReadbackCollector` chỉ đọc sáu file giả
được tạo sẵn. `VerifyOutcome` chỉ consume `post-readback.json`; nó không tạo file
này. Command để lại một comment yêu cầu người chạy tự thay đường dẫn readback.

Ảnh hưởng: sau một live call, workflow hiện không có bước executable đã review
để lấy package/DB/attempt/authorization/lifecycle/artifact evidence. Đây chính
là lớp lỗi từng xảy ra ở A3.2: action đã diễn ra nhưng post-check đầy đủ không
được giữ, khiến acceptance không thể chứng minh và no-redispatch chặn thử lại.

### H3 — artifact fixture không khớp production serializer

`Test-P5ESerializedArtifactBytes` chỉ cho chín field và yêu cầu cả report/receipt
có `bindingIdentity`, `manifestFingerprint`, `packHash`, `profileHash`,
`chapterKey`, `phase`, `predecessorIdentity`. Synthetic producer ghi đúng shape
này nên producer→verifier GREEN.

Production `EditorialP5PilotExecution` tạo hai shape khác nhau:

| Artifact | Identity fields production |
|---|---|
| REPORT_L1 | `bindingIdentity`, `manifestFingerprint`, `canonicalPackHash`, `canonicalProfileHash`, `compatibilityEvaluationId`, `chapterKey`, `phase`, `bundleIdentity`, `predecessorIdentity`, cùng ledger/gate/disposition fields |
| Receipt | `manifestRef`, `packRef`, `profileRef`, `bindingRef`, `phase`, `bundleIdentity`, `predecessorIdentity`, cùng totals/gates/disposition/release fields |

Vì verifier dùng strict allowed-field list của fixture, bytes production hợp lệ
sẽ bị từ chối; ngược lại fixture GREEN không chứng minh serializer thật. Tuyên
bố trước đây “actual serialized bytes validated” là sai phạm vi và được thay
bằng `SYNTHETIC_CONTRACT_ONLY / F3_NOT_CLOSED`.

### H4 — live method không phát post-dispatch evidence

Selected method gọi `dispatchRaw` rồi chỉ assert kết quả in-memory. Trong method
live không có `sendStatus` sau dispatch, không ghi event-bound readback file và
không export persisted report/receipt bytes. `sendStatus` hiện có thuộc preflight
method lịch sử. Exit 0 hoặc `OK (1 test)` vì thế vẫn không đủ cho durable RAW
acceptance.

Ảnh hưởng: collector host không có nguồn chuẩn để phân biệt `COMMITTED`,
`RECOVERY_REQUIRED`, timeout/unknown, partial lifecycle hoặc missing cost. Một
test-only emitter/read-only acquisition boundary cần được thiết kế, build và
repin trước khi owner review; nếu process chết, phải còn đường read persisted
state mà không redispatch.

## Nguyên nhân vòng lặp overthinking

Vòng lặp không đến từ việc thiếu thêm checklist. Nó đến từ bốn lỗi kiểm soát:

1. **Đóng gate trên fixture tự tham chiếu.** Producer và verifier dùng cùng
   schema do host tự đặt nên mutation tests PASS nhưng không chạm contract
   production.
2. **Đẩy dependency local sang owner.** Tài liệu gọi owner provenance là “bước
   duy nhất” dù executable collector và helper binding vẫn có thể sửa offline.
3. **Tách dispatch khỏi observability.** One-shot command được chuẩn bị trước,
   còn cách lấy post-state để sau; khi timeout/mất device, evidence không còn.
4. **Lặp trạng thái ở nhiều file.** Banner/snapshot/checklist/proposal giữ nhãn
   GREEN cũ sau khi evidence mới phủ định, nên lượt sau đọc được nhiều “sự thật
   hiện tại” khác nhau và lại audit từ đầu.

Biện pháp dừng vòng lặp là một dependency chain duy nhất:

```text
production serializer contract
  -> test-only/live readback emitter + recovery collector
  -> independent golden/negative fixtures
  -> verifier
  -> hash-bound command/helper packet
  -> owner provenance + permission
  -> one dispatch
  -> durable acceptance
  -> đánh giá P5 exit/P6
```

Mỗi node chỉ đóng khi evidence của node trước tồn tại. Không thêm phase, session
ledger hay framework mới.

## Đánh giá gate

| Gate | Kết quả | Lý do |
|---|---|---|
| F2 transport | PASS giữ nguyên | self-test rerun PASS; không có evidence regression |
| F3 timing/event/path/redaction | PARTIAL PASS | mutation cũ bị chặn, redaction PASS |
| F3 artifact contract | FAIL | synthetic shape khác serializer production |
| F3 live collection | FAIL | không có executable collector/post emitter |
| Command immutability | FAIL | helper không được runtime hash-check |
| F1 trusted account provenance | PENDING OWNER | expected/account mapping và permission chưa có |
| A4.3 owner packet | NOT READY | còn H1–H4 local trước khi xin quyết định |
| A4.3 dispatch | NOT AUTHORIZED / NOT RUN | không có quyết định owner; packet cũng chưa ready |
| RAW predecessor | NOT PROVEN | zero live run |
| P5 exit | INCOMPLETE | RAW rồi RECONCILE/L1 acceptance chưa hoàn tất |
| P6 entry | NOT READY | phụ thuộc P5 exit |

## Phản biện trước khi xuất

- “Helper hash đã ghi trong proposal” không đủ: executable command không kiểm
  hash đó.
- “Synthetic producer là producer cụ thể” đúng ở unit scope, nhưng không có
  device source và dùng artifact contract khác production.
- “Có thể viết collector sau live” không chấp nhận vì no-redispatch; thiếu
  post-check sau call sẽ tái diễn A3.2.
- “Instrumentation PASS thì commit đã bền” không đúng: live method chỉ assert
  in-memory và có thể kết thúc ở recovery/unknown.
- “Owner fingerprint là blocker duy nhất” không đúng: H1–H4 không cần credential
  thật để thiết kế, triển khai và test.
- “Cần build lại production” chưa có căn cứ: ưu tiên test-only emitter/host
  collector; production source, route, model, budget, pack/profile giữ nguyên.
- “Có thể dùng actual mismatch làm expected” bị cấm vì tạo trust vòng tròn.
- “Unknown cost/provider state là zero” bị cấm; unknown phải giữ unknown và
  không redispatch.

## Quyết định bàn giao

Tiếp tục đúng P5E bằng work request mới. Hoàn tất local H1–H4, test độc lập với
serializer thật, tạo/repin AndroidTest artifact nếu test-only source phải đổi,
rồi mới freeze packet và xin owner cung cấp F1 + quyền account/readback/egress.
Không chạy A4.3 hoặc chuyển P6 từ trạng thái hiện tại.
