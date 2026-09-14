# P5E — tái kiểm tra readiness và provenance, 2026-09-15

**Chưa được chuyển P6; chưa đủ điều kiện dispatch A4.3.** F2 có implementation sửa quoting. F3 có verifier và các fixture cũ đạt, nhưng audit mới chứng minh bốn trường hợp sai vẫn được verifier nhận là RAW_ACCEPTED. Vì vậy “chỉ còn chờ owner” không còn là kết luận đầy đủ. Phần việc local cần làm tiếp được ghi trong `P5E_NEXT_WORK_REQUEST.md`; đây là tiếp tục P5E, không tạo phase/release mới.

## Provenance của cuộc kiểm tra

| Dữ kiện | Nguồn trực tiếp | Kết quả/giới hạn |
|---|---|---|
| HEAD được user bàn giao | `git rev-parse HEAD` | `9beafef8e59825714e47cdfe594dc287b6c5a0ce`; clean lúc bắt đầu |
| Branch | `git status --short --branch` | `feature/v4.18-p5e-audit-20260914`; tiếp tục cùng phạm vi, không tạo release/checklist khác |
| Host implementation | Git log | `c2c79a19842f551fc752a53328024aab8ddb529d` |
| Audit trước | Git log | `0f52d36e516560bb33d294303c70fa1753cb64f9` |
| A4.3 ban đầu | Git log | `f8fe433ef454772a1b55dea496a2c4bfd679766f`; proposal preparation `c1e3ec6eec62e38a1e2f4fdb0d0f151d5efdcfae` |
| Test source/archive | Source pin và diff app/editorial-engine | `d51b7f3c16bdc482513b9904db07b97daed592d1`; Android source không đổi tới HEAD bàn giao |
| Manifest | Hash tính lại từ file | `DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501` |
| Command hiện hành | Hash tính lại từ file | `1D9A67693C4C4F64AF182300CDEB963A9FD9D44361EF77E91371F88C92EDA15E` |
| Helper hiện hành | Hash tính lại từ file | `BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799` |
| Báo cáo host trước | Hash tính lại từ file | `C54E55446629ADC161882C51333B25BD21633277C6A4B1020D5265E93DA11E3C` |
| Mutation probe mới | `P5E_PROVENANCE_REVIEW_PROBE.ps1` và `P5E_PROVENANCE_REVIEW_RESULT.json` | Chỉ synthetic; không chạy Dispatch, VerifyOutcome entry point, SelfTest, process supervisor, ADB hoặc provider |
| Quyền trong tin nhắn user hiện tại | Yêu cầu audit/plan/provenance | Không phải quyết định cho phép đọc credential hoặc gửi sách |

Đã kiểm kê/đọc máy 158 MD/TXT tracked, 27.796 dòng tại baseline. Đọc sâu các tài liệu điều khiển hiện hành, bộ P5E, thay đổi từ audit trước, command/helper, selected live method và nguồn liên quan đến expiry/identity. Không tuyên bố semantic review từng dòng mọi tài liệu lịch sử hoặc toàn bộ Android regression. Những chứng cứ lịch sử A4.2, code196, IPC/CP6 vẫn giữ cách phân loại trong audit trước; không đọc lại toàn bộ raw lịch sử khi không có thay đổi liên quan.

## Đã đóng gì, còn gì

| Mục | Trạng thái đúng |
|---|---|
| F2 pipe quoting | Sửa ở host; giữ bằng chứng RED→GREEN cũ. Không đưa lỗi pipe cũ trở lại thành blocker khi chưa có regression mới |
| F3 consumer và fixtures cũ | Đã có; PASS cũ vẫn đúng trong phạm vi fixture đã chạy |
| F3 khả năng chứng minh dữ liệu thật | Chưa hoàn chỉnh: producer/readback provenance chưa được chỉ ra; verifier bỏ lọt các quan hệ thời gian/identity/event dưới đây |
| F1 actual fingerprint computation | Có trong test pin; settings→normalize endpoint→SHA-256→compare, trước authorization |
| F1 expected fingerprint provenance | Chưa có từ owner; phép tính actual không tự chứng minh đúng tài khoản được owner chọn |
| P5/P5E exit, P6 | Chưa đạt. Không có RAW/L1 acceptance mới |

### R1 — chấp nhận readback sai thời gian, identity và event

Probe dùng đúng helper hash BEEF…6799, nạp AST declarations và gọi verifier như hàm trên dữ liệu giả. Không chạy các top-level dispatch/default branches. Kết quả:

| Case | Mong đợi | Thực tế |
|---|---|---|
| valid_control | accept | RAW_ACCEPTED, đúng |
| missing_receipt_control | reject | RAW_NOT_ACCEPTED, đúng |
| observed_before_run: observedAtMillis=0 | reject | RAW_ACCEPTED, sai |
| consumed_after_expiry: consumed=issued+180001, validity=180000 | reject | RAW_ACCEPTED, sai |
| artifact_manifest_mismatch: receipt manifest khác report | reject | RAW_ACCEPTED, sai |
| wrong_evidence_directory: metadata thuộc event khác | reject | RAW_ACCEPTED, sai |

Không gọi đây là “6/6 PASS”: chỉ hai control có kết quả đúng; bốn mutation phát hiện false acceptance. P6 vẫn false trong mọi case, nhưng điều đó không làm RAW acceptance sai trở thành chấp nhận được.

Nguồn lỗi: `Test-P5EReadback` chỉ kiểm observedAt>=0; consumed>=issued mà không kiểm consumed<expires; manifestFingerprint chỉ kiểm 64 hex mà không ràng buộc report/receipt với identity đúng; metadata.evidenceDirectory thuộc required shape nhưng không đối chiếu với event/file đã mở. Engine authorization có `nowMillis >= expiresAtMillis` là invalid, nên expiry boundary đã có căn cứ nguồn, không phải gate mới tự đặt.

Đây là lỗi verifier host trên dữ liệu giả, **không phải bằng chứng production đã commit dữ liệu sai**, cũng không chứng minh từng readback hợp lệ phải hoàn tất trước auth expiry. Post-readback có thể muộn hơn expiry; claim/consume phải thuộc cửa sổ được phép. Phải phân biệt hai thời điểm khi sửa.

### R2 — có schema nhận readback, chưa có producer được chứng minh

Search `p5e.raw.readback.v1`, `PostReadbackPath`, `post-readback.json` trong scripts/docs/app cho thấy consumer, template comment và generator **fixture** trong helper. Chưa tìm được collector thực từ package/DB/report/receipt đến schema này. Đây là kết luận giới hạn trong phạm vi đã search, không khẳng định không tồn tại công cụ ngoài repo.

Một JSON ghi `atomicClaim=true`, `validationPassed=true`, `allowedDiff=true` chỉ là khai báo nếu không truy ngược được nguồn. Dữ liệu sau transaction cũng không tự chứng minh mọi chi tiết atomicity trong quá khứ. Cần tách: quan sát before/after của event thật; validator chạy trên bytes thật; invariant transaction được chứng minh bằng source/test liên quan. Không bắt owner tự viết JSON giống fixture và không copy booleans từ fixture vào evidence thật.

Producer là dependency thực trước live: nếu chỉ thiết kế sau khi live đã chạy, sẽ tái diễn A3.2 thiếu post-check và A4 emitter/parser gap. Ưu tiên host-only collector từ readback được phép hoặc đường read-only sẵn có; chỉ đề nghị AndroidTest mới khi chứng minh không có đường tương thích với pin hiện tại.

### R3 — tuyên bố không log fingerprint quá mạnh

Test pin dùng `assertEquals(expectedEndpointAccountFingerprint, endpointAccountFingerprint)` ở dòng 297. Khi mismatch, assertion String có thể đưa expected/actual vào failure text. Host redactor chỉ nhận api-key/bearer/endpoint/URL patterns; probe assertion giả với hai fingerprint vẫn giữ nguyên cả chuỗi và `redactionViolation=false`.

Không phát hiện credential thật, không chứng minh đã có leak thật. Fingerprint cũng không phải API key. Tuy vậy, nếu packet yêu cầu fingerprint không vào logs thì regex hiện tại chưa bảo đảm điều đó ở failure path. Sửa host capture để loại giá trị fingerprint ở assertion bằng input fake và bật fail-closed; không đọc credential để tạo test. Giữ phân biệt fingerprint ở metadata/receipt nào được policy cho phép và fingerprint không được phép log, tránh vừa cấm toàn bộ vừa yêu cầu lưu cùng field trong readback.

### R4 — provenance của expected cần một thao tác thực tế

Expected là hash endpoint chuẩn hóa + newline + đúng credential; không phải account ID trên dashboard. Owner phải xác nhận mapping giữa credential đó và account/project/billing được phép. Fingerprint chứng minh chuỗi endpoint/key khớp expected, không tự xác thực legal owner hay trạng thái billing.

Có hai đường: (A) owner đã có giá trị/trusted attestation do quy trình có nguồn tạo ra; hoặc (B) chưa có, cần owner cho phép một enrollment/verification tối thiểu được mô tả chính xác. Đường B chưa được giải quyết bằng lời “hãy cung cấp provenance”; không được giả expected=actual rồi tự phê duyệt. Không cần gửi API key vào chat. Mẫu provenance và giới hạn quyền nằm trong request kế tiếp.

## Vì sao vẫn lặp

Chuỗi mới vẫn lặp kiểu cũ: viết consumer + tự tạo fixture đạt → gọi chung là host complete → chuyển dependency cho owner → chưa chứng minh producer/event linkage → audit sau phát hiện boundary còn trống. 1.495 dòng helper không phải thước đo completeness. Vấn đề nằm ở bằng chứng end-to-end và tiêu chí kết thúc, không phải cần thêm một lượng tài liệu tùy ý.

Audit trước của chính tôi cũng có phần cần cải thiện: request 50 bước đòi readback/schema-derived nhưng chưa buộc bàn giao producer executable và bảng field→source→transformation→evidence. Điều đó để lại chỗ cho “verifier có fixture” được hiểu là đã hoàn tất F3. Request mới bổ sung acceptance cụ thể cho khoảng trống này, giữ F2 đã đóng và không yêu cầu rerun mọi lịch sử.

Tài liệu còn drift: runbook/reconciliation vẫn có banner audit 14/9 trong khi canonical/state ghi host complete 15/9; snapshot gọi helper là working-tree change dù helper đã commit c2c79a19. Audit này sửa điều hướng/current và baseline, không thay bằng chứng đã pin C54…E3C hoặc tự đánh dấu release 05–09.

## Readiness

- Có thể tiếp tục **sửa local F3/readback provenance trong P5E**, đồng thời chuẩn bị input owner F1. Không cần owner cho phép typo/test/helper repair nằm trong scope host đã giao.
- Chưa đủ để “chỉ chờ owner rồi dispatch”: R1–R3 còn cần đóng bằng evidence. Không mở P mới để sửa chúng.
- Khi local evidence đạt, owner input/permission đúng và current device pins đạt trong cửa sổ đã được cho phép, mới đánh giá one-shot RAW; không cấp quyền live bằng tài liệu này.
- RAW accepted chưa phải P5 exit. RECONCILE/phần L1 còn lại cần predecessor và scope tương ứng; P6 chỉ mở sau P5 exit. Ba chương L1–L3 là exit P6, không đặt thành prerequisite vòng tròn cho việc vào P6.

## QA và phản biện trước xuất

1. Có bằng chứng mới hay chỉ một vòng review cùng input? Có: 4 mutation false accept và assertion-redaction probe; lưu script tái hiện pin chính xác.
2. Có phủ nhận F2/F3 PASS cũ? Không; giới hạn đúng tập fixtures cũ. Không rerun F2 để trì hoãn sửa F3.
3. Có đòi cryptographic attestation platform mới? Không; bảng provenance, hashes/event binding, trusted collector và source-derived checks là đủ. Không thêm service/session-ledger/framework.
4. Có biến owner input thành blocker cho toàn bộ local work? Không; sửa R1–R3 trước, chuẩn bị lựa chọn F1 song song.
5. Có ép no-secret bằng cách bỏ toàn bộ failure evidence? Không; redacted type/status/field/hash allowlist vẫn phải đủ phân loại, fake canaries kiểm success/mismatch/timeout.
6. Có gọi probe thành QA release? Không; RED mới ghi là FAIL cần sửa. Audit/request hoàn tất không có nghĩa host release/RAW/P6 đạt.
7. Có viết nguyên nhân xóa code196 từ suy đoán? Không; giữ historical loss, không khẳng định actor thiếu evidence, không restore để tạo PASS.

Không credential/account check thật, ADB, instrumentation, provider, runtime authorization, DB readback, build/install/tag/merge trong audit này. Các số đếm trong fixture là dữ liệu giả.

### Kiểm tra bản xuất

- Artifact/backup được kiểm lại toàn payload: production 5/5, test 8/8, mismatch=0.
- Bốn hash manifest/command/helper/host-report đúng bảng provenance; không sửa các file được pin này.
- Request có đúng 40 bước đánh số liên tục; template owner giữ NOT_PROVIDED/NOT_APPROVED, không giả quyết định.
- Snapshot có đúng một Next action và baseline 9beafef8, tách test source/host implementation khỏi snapshot commit.
- Checklist release 05–09 vẫn có đủ 5 mục chưa hoàn tất; diff app/editorial-engine/scripts rỗng; git diff --check đạt.
- Probe script/result được lưu trong docs để tái hiện; các input synthetic lưu ngoài repo tại `C:\Users\ADMIN\.codex\visualizations\2026\09\14\01a0a041-c92c-7ba1-ae51-f783e3caa4eb\p5e-provenance-review-f4d8596275ba4f3bb22c6b94996d7d2e`. Đây không phải source dữ liệu thật.
- QA bản xuất đạt các điều kiện tài liệu trên; **QA readiness F3 vẫn RED**, không có tuyên bố release/session phát triển hoàn tất 14 bước.
