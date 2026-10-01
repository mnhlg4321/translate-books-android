# Editorial Recovery v4.18 — hoàn thiện bản biên tập cuối

Cập nhật 2026-10-01 theo yêu cầu owner. Đây là canonical plan duy nhất, viết lại tại chỗ; không mở release, branch hoặc checklist mới. Mục điều hành dưới đây thay thế mọi next action cũ. Bản trước viết lại được giữ nguyên byte tại `docs/EDITORIAL_RECOVERY_V4_18_HISTORY_20260930.md` (SHA-256 `2537C72FB5F619172E46BC3D1ADA2F0F494BA134093AE4A47822DF8A7872216D`); chỉ đọc lại lịch sử khi cần bằng chứng cụ thể.

## 1. Kết quả phải giao

Người dùng chọn pack và nguồn của một chương, chạy đủ L1–L3, nhận bản văn đã biên tập hoàn chỉnh, lưu được, mở lại được và xuất TXT UTF-8 được. Bản cuối chỉ chứa văn bản biên tập, không lẫn report, JSON hoặc lời giải thích. Không giao Translation-only thay cho Editorial.

REPORT_L1, VI_L2, CHANGE_MAP_L2 và QA_RECEIPT giữ nội bộ khi cần cho thực thi/kiểm chứng/phục hồi; không bắt người dùng xuất, nhập lại hoặc nhận từng file trung gian. Bỏ bàn giao riêng không thay đổi authority payload/schema đang pin và không cho phép xóa evidence.

Mốc đầu tiên: một chương thật đi trọn L1–L3 và mở lại/export đúng bản cuối. Nghiệm thu sản phẩm: ít nhất ba chương đại diện có final hợp lệ theo `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`. CONTENT_BLOCKED đúng chỉ chứng minh khả năng dừng, không tính thành chương đã biên tập xong. Đây là tiêu chí sản phẩm được làm rõ theo ưu tiên owner; không viết lại kết quả lịch sử.

Ba chương chạy tuần tự, mỗi chương có chain/source identity riêng; không nối chúng thành một chain và không triển khai batch cả sách. Pair Context nếu được dùng chỉ là nguồn tùy chọn theo contract, không tự truyền kết luận/canon từ chương trước.

## 2. Điểm xuất phát đã xác minh

| Hạng mục | Sự thật hiện tại | Giới hạn kết luận |
|---|---|---|
| Workspace | `D:\App Translate Books`, `feature/v4.18-p5e-runner-repair-20260917`, HEAD `4828a831a74d9c5384125a25ab71c2c661147f73` | Giữ continuation này, không quay về branch theo banner lịch sử |
| Nguồn | Staged/working tree khác nhau; entrypoint có sửa chưa stage | Không commit index cũ như candidate đã test |
| Build phát triển | `4.18-dev.1`/code208, source snapshot `240cdc814a324bca36543a8091af6bf6cdd07831`; APK SHA-256 `DB3FE9056A3477FA18D4F9F44A18E1FCCA0F7195F95DD99C91F1BD60AA6FE465` đã đối chiếu | Chưa cài; không chứng minh L1–L3 |
| Pilot | Last-known production `4.17-p5e.11`/code207; A4.3 pins giữ nguyên | Không thay bằng code208 mà kế thừa acceptance cũ |
| P0–P4 | Có bằng chứng lịch sử hoàn tất; tái sử dụng importer/binding/preflight | Không khởi động lại P0–P4 |
| P5 | Owner-window thoát trước prompt; không có audit/terminal evidence mới | `EVIDENCE_INCOMPLETE`; không suy provider/DB bằng 0 |
| P6/P7 | Chưa hoàn tất | Enum/schema không chứng minh luồng sản phẩm đã chạy |

Last candidate: `scripts/p5e-a43-pre-reservation-launcher-entrypoint.ps1`, SHA-256 `D8E4D0FCB346491E4E9A9EF45C0890339BA67A54F47B2569026F26DF96704F0D` (re-pin 2026-10-01 cho pin hash file DB; thay `68DF8061…` là candidate của lần owner-window thất bại gần nhất). Last visible attempt: DecisionId `p5e-a43-owner-4abe5015e63643c0b5bf31b1c9576c3a`; audit dự kiến dưới `D:\P5E-private\.p5e-a43-audit` không được quan sát. Giữ nguyên sự kiện, không thử lại.

Bằng chứng: `docs/P5E_CONSOLIDATED_FAILURES_20260930.md`, `docs/P5E_A43_ENTRY_BOUNDARY_REPAIR_PROVENANCE_20260930.json`, `docs/P5E_A43_PARENT_INTEGRATION_FINAL_PROVENANCE_20260930.json`, BUILD_INFO trong `artifacts/builds/v4.18-dev.1/build-20260930-185915/`. Các test PASS dẫn lại là lịch sử, không phải test mới trong lượt lập kế hoạch.

## 3. Quy trình vận hành cần có

```text
Chọn pack + RAW/DRAFT/GLOSSARY/PRONOUN
 -> snapshot bất biến, kiểm chế độ nguồn và preflight
 -> L1 RAW discovery -> L1 reconcile -> lưu REPORT_L1/receipt
 -> L2 raw-first -> edit -> atomic VI_L2 + CHANGE_MAP_L2
 -> L3 RAW/VI_L2 blind QA -> reconcile -> hai lượt phản biện
 -> app tính actual diff, coverage/no-regression và điều kiện xuất
 -> atomic FINAL_QA + QA_RECEIPT
 -> xem bản cuối -> mở lại -> xuất TXT -> đọc lại file xuất
```

L1–L3 là ba lượt nghiệp vụ, không đồng nghĩa ba API calls. RAW/reconcile và blind QA có ranh giới nhìn thấy nguồn riêng; không gộp thành một prompt lớn. Lập call/token/time/cost theo phase trước pilot; ngân sách RAW A4.3 không bao phủ cả chuỗi.

App quyết định identity, thứ tự, hash, state, diff, commit và quyền export. Model phân tích/sửa văn; model tự ghi PASS không mở gate. Thiếu/sai nguồn -> INPUT_REQUIRED; lỗi định dạng -> REPAIR_REQUIRED; transport/truncation -> RETRY_REQUIRED; thiếu chắc chắn ngữ nghĩa -> PRESERVE_DRAFT khi contract cho phép; chỉ xung đột nội dung có chứng cứ mới CONTENT_BLOCKED. Không biến lỗi kỹ thuật thành lỗi nội dung.

Một chương/một writer. Restart đọc trạng thái đã commit; không tự dispatch lại request có external state UNKNOWN. Đổi nguồn/pack invalid chain tương ứng, không tự rebind. Chỉ output hoàn chỉnh đã xác minh mới thành predecessor.

## 4. Khoảng cách triển khai đã đối chiếu mã

| Thành phần | Mã đã đọc | Việc còn thiếu/chưa chứng minh |
|---|---|---|
| Host launch | `scripts/test-p5e-a43-entry-boundary.ps1`, `scripts/test-p5e-a43-pre-reservation-integration.ps1` | Entry test ẩn/noninteractive; integration thay input readers. Cần đúng process/argument/console boundary và lỗi trước diagnostic |
| L1 | `EditorialP5PilotRequest.Phase`, `EditorialP5PilotExecution`, `EditorialP5CExactBindingExecution`, `EditorialP5CAttemptStore` | Có RAW/RECONCILE/commit; live L1 persisted acceptance chưa đạt |
| L2/L3 | `EditorialSafe4Workflow`, `EditorialSafe4Contract`, `EditorialPhaseContextProjector`, `EditorialReceiptValidator` | Có state/visibility/validation; chưa chứng minh coordinator/provider/atomic persistence đầy đủ. Không chỉ đổi tên phase trong L1 engine |
| Final/export | `EditorialReleaseValidator`, `EditorialTextDiff`, `EditorialReleaseDestinationTest` | Artifact validator, diff hiển thị và test tên ZIP không chứng minh semantic quality hoặc export final thật |
| UI | `EditorialPageFactory.build/projectCard/chapterCard` | Đang khóa execution; cần nối chạy/tiến độ/phục hồi/xem và xuất final trong P6 |

Java app ở `app/src/main/java/com/ml/tblandroidtxt/`; engine ở `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/`. Đây là kết luận từ các điểm nối đã đọc, không phải audit mọi file. Trước thêm lớp mới, tìm call sites và tái dùng phần đáp ứng contract. Ownership chi tiết ở `docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md`.

## 5. Trình tự, đầu ra và điều kiện chuyển bước

Giữ P5 -> P6 -> P7 và checklist `release_checklists/v4.18-editorial-v5-safe-4-1-3.md`. Các hàng là nhóm việc trong phase hiện có, không phải release/phase mới. Chỉ một nhóm triển khai active. Đọc mã/thiết kế P6 trong lượt lập kế hoạch không mở P6 execution.

| Nhóm | Công việc | Đầu ra bắt buộc để chuyển tiếp | Không đạt thì làm gì |
|---|---|---|---|
| P5: cửa vào | Tái hiện cách mở process/arguments/console bằng child giả, không key/provider/device; bắt lỗi ngoài cùng trước audit | Prompt thử và terminal result quan sát được; lỗi có nguyên nhân/exit; process PS5.1 sạch, paths có khoảng trắng | Áp giới hạn mục 6; không sửa DB/parser khi chưa có failure ở đó |
| P5: chốt input trước live | Đối chiếu pilot/source inventory có sẵn, đề xuất ba category với exact chapter/source identity; lập bảng call/cap/budget cho phần sắp chạy | Chapter/nguồn và phạm vi chi phí đã được phép trước event tương ứng; chỉ hỏi phần còn thiếu | Tiếp tục local checks độc lập, không đoán quyền/budget hoặc dispatch |
| P5: RAW | Sau offline đạt và đủ quyền: một event mới đúng pins/budget/allowlist; Before/After/readback/verifier | Accepted RAW predecessor đúng identity, payload đầy đủ, cost/state rõ; P5E.9 có evidence | UNKNOWN: dừng dispatch; không retry; chỉ reconcile external state trong phạm vi được phép |
| P5: L1 | Phạm vi RECONCILE riêng khi còn thiếu; kiểm/lưu REPORT_L1/receipt và mở lại | P5.4/P5 exit có kết quả thật; không false block chưa giải thích | Sửa case cụ thể trong nhóm; không mở lại launcher/account không liên quan |
| P6: L2 | Nối projection/provider/validator/store; actual DRAFT→VI_L2 diff gắn IDs; atomic text/map; fake tests trước live | L2 đúng predecessor/coverage, reopen đúng; crash không để nửa kết quả hợp lệ | Giữ L1 hợp lệ; rollback commit dở; typed recovery |
| P6: L3 | Blind RAW-first, reconcile, hai lượt phản biện; actual VI_L2→FINAL; atomic final/receipt | Final hợp lệ lưu/mở lại; quality gates đúng | Sửa phase hỏng; không rerun cả chuỗi hoặc export partial |
| P6: UI/export | UI dùng cùng coordinator với tests; tiến độ/recovery; xem/export final | Một chương thật đi hết L1–L3, restart đúng, TXT đọc lại khớp final và không lẫn report | Sửa app path; test-only/CLI không thay thế |
| P6: ba chương | Chương ngắn, thoại/xưng hô dày, chương dài gần giới hạn; negative conflict/no-edit riêng | Ba final hợp lệ; negative conflict dừng đúng không tính vào ba final | Conflict thật ghi rõ và bổ sung chương hợp lệ, không hạ tiêu chí để tăng PASS |
| P7: bàn giao | Targeted + full regression bắt buộc; wrapper build, hai archive; device QA import/restart/recovery/stale input/export/Translation | APK, final samples và checklist evidence; tag/release chỉ sau gates | Sửa local trong phase, không mở release track mới |

**Mốc P5 sau cửa vào (một RAW event không phải quyền cho cả chuỗi).** Mỗi mốc có evidence PASS riêng; không mốc nào suy ra từ mốc trước.

| Mốc | Đầu ra | Evidence PASS | FAIL thì làm gì |
|---|---|---|---|
| M0 quan sát chỉ-đọc trước event | Serial, APK production/test, certificate, DB sha so với pins của helper | Mọi hash khớp pins (hash DB còn được hard-code trong APK test: đổi pin DB phải đổi cả APK test); số dòng lineage bằng không | Lệch: không mở event, không provider. Chỉ re-pin offline khi drift được giải thích bằng hash đã đo, rồi trình owner lại |
| M1 mở event + Before | Outer log, audit theo DecisionId, reservation, receipt, Before snapshot | `BEFORE_STATE_CAPTURED` | Typed stop: event đã dùng, phân loại bằng outer log + audit, không retry. Nếu là lỗi cửa vào, dùng phương án B (mục 6), không vòng launcher thứ ba |
| M2 một RAW call | Request/response identity, cost, terminal state | After + verifier: response đầy đủ, `finish` hoàn tất, cost trong trần, identity đúng | Truncated/lỗi/timeout: typed result, không retry/repair. UNKNOWN: sang M2b |
| M2b đối chiếu trạng thái ngoài (chỉ khi UNKNOWN) | Phân loại generation, cost | Đọc metadata nhà cung cấp, không gọi model, trong phạm vi owner cho phép riêng | Vẫn không rõ: dừng, owner quyết định; không suy ra `$0` |
| M3 RAW predecessor | Predecessor bền vững | Readback đúng identity/hash/payload | Partial: không được nhận làm predecessor |
| M4 RECONCILE (L1, phase riêng) | Kết quả reconcile | Quyền, trần call/USD riêng; validator đạt; không thừa kế quyền RAW | Theo typed contract (INPUT/REPAIR/RETRY_REQUIRED); không retry mù |
| M5 REPORT_L1 + receipt atomic | Cả hai cùng commit | Readback đúng schema/identity/predecessor/hash; failpoint trước/sau commit | Partial: không nhận cái nào; sửa store cục bộ, không gọi provider lại |
| M6 restart/reopen | Đọc lại REPORT_L1, receipt, predecessor | Cùng hash sau restart | Sửa app path cục bộ |
| M7 P5 exit | `P5_EXIT_PASS / P6_READY` ghi vào checklist P5E.8/P5E.9/P5.4/exit và bảng chapter manifest → pack hash → chain/run → RAW → REPORT_L1 → receipt → reopen | Mọi mốc trên có evidence cụ thể; gate P5D lịch sử phân loại historical/superseded hoặc còn bắt buộc | Ghi đúng mốc thiếu; không chạy provider chỉ để đóng track lịch sử |

Build phục vụ kiểm chứng Android được phép ở milestone cần artifact mới theo policy, không phải đợi P7. Không build cho host/docs thuần. APK mới cần pins/evidence mới; archive trước cài; không dùng connected installer bị cấm trên pilot. Full regression cuối gồm engine/app, lint, Translation Glossary4/Pronoun7 và các checks release/performance bắt buộc trong workflow; không dùng báo cáo thay benchmark thật.

Kiểm thử atomic tối thiểu cho cả L2 và L3: failpoint trước write, sau write thành phần thứ nhất nhưng trước transaction commit, và sau commit trước UI acknowledgement; restart/readback chứng minh hoặc cả cặp tồn tại với đúng hash/predecessor hoặc không cặp nào được nhận hợp lệ. Thêm double-submit/concurrent writer, stale source và UNKNOWN dispatch không gây call lặp. Đây là một transaction cho cặp, không phải hai commit rồi vá consistency.

Kiểm compatible-next tái dùng fixture tạo 4.1.4 ở `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP4BindingInstrumentedTest.java`; tại lần nghiệm thu ghi APK hash, hai ZIP/canonical pack hashes, test method/result, hai binding readback sau restart. Dùng cùng APK trong cả hai lần import; không tạo build chỉ để đổi pack. Không đoán hash của fixture chưa được tạo trong run đó; chọn test invocation qua cơ chế thiết bị được phép, không chạy cả lớp trên dữ liệu pilot.

Bảng evidence nghiệm thu đặt trong checklist/report hiện có: category/chapter, source manifest hash, pack hash, chain/run identity, APK version/hash và source ref, FINAL hash, receipt hash, reopen/export result, đường dẫn evidence. Cả ba chapter phải map tới candidate release cuối; kết quả trên APK trước là lịch sử cho tới khi đã chứng minh tương đương hoặc kiểm lại. Lưu bảng và samples được phép trong payload QA/archive cùng build/release tương ứng; dữ liệu riêng tư chỉ giữ ở nơi được phép và ghi đường dẫn/hash, không tự đưa vào archive chia sẻ.

## 6. Giới hạn chống vòng lặp

1. Cửa vào A4.3: tối đa 60 phút active diagnosis hoặc hai vòng patch–targeted test, điều kiện nào đến trước. Lập kế hoạch hôm nay không tính là vòng sửa. Không tự gia hạn bằng đổi tên package/model/agent.
2. Mỗi vòng ghi ngắn trong báo cáo hiện có: giả thuyết, case tái hiện, thay đổi, bằng chứng mới, kết luận. PASS lại với bytes/input không đổi không phải tiến độ; không rerun suite rộng khi không có thay đổi/rủi ro mới.
3. Hết giới hạn A: dừng phương pháp launcher cũ, giữ FAILED_REPAIRING nếu lỗi local. Chuẩn bị B cụ thể: môi trường Android thử riêng, dữ liệu thay thế rõ, cùng production code path/pack/model contract, Before/After và final acceptance; liệt kê khác biệt phải kiểm lại trên app mục tiêu. Chỉ xin quyết định thay môi trường/quyền khi phương án review được; không tự cài, xóa DB hay gọi provider.
4. B tái dùng engine/store/coordinator, không thành nền tảng thứ hai hoặc CLI tạo final thay app. Thiếu tài nguyên B thì nêu đúng tài nguyên và phần local còn làm được; không tạo thêm bộ giấy tờ.
5. Với L1/L2/L3/UI: hai vòng liên tiếp cùng failure signature mà không thu hẹp nguyên nhân -> dừng cách sửa đó; một review nhỏ kiểm lại contract và đưa phương pháp khác. Review không tự reset bộ đếm. Chỉ tiếp tục có bằng chứng mới hoặc phương pháp thay đổi cụ thể; lỗi local không giả thành BLOCKED_EXTERNAL.
6. Theo dõi số vòng, active time, phase và last durable output; không đưa token budget giả khi không đo được. Provider có trần riêng đã cho phép; kiểm input/output capacity trước call, không thử mù khi biết không đủ.
7. Luna chỉ nhận việc nhỏ (một contract/test/doc); mỗi nhiệm vụ một kết quả. Không review lặp cùng bytes; chỉ xác nhận finding cụ thể sau sửa.
8. Một plan, một checklist, một snapshot hiện tại. Cập nhật theo nhóm xong; không sinh proposal/provenance/review/next-request cho mỗi lỗi local. Giữ immutable evidence cần thiết và lịch sử.

Phương án B tối thiểu để trình quyết định: ưu tiên thiết bị Android thử riêng sẵn có; nếu không có, đánh giá emulator như một lựa chọn, không dựng thành gate mới mặc định. Dùng cùng source candidate và production engine/store, bộ nguồn thay thế đã chọn, môi trường không chứa DB người dùng cần bảo toàn. Harness chỉ điều khiển/capture; sản phẩm vẫn chạy trong app. B chỉ đóng các kết luận được kiểm trên môi trường đó; cài/khởi động/lưu–mở lại–export và các khác biệt Android/provider còn liên quan phải được xác minh trên mục tiêu giao trước P7 PASS. Không lựa chọn hay provisioning môi trường trong lượt viết plan.

## 7. Quyền, dữ liệu và chi phí

Lượt này là nghiên cứu/viết lại/phản biện plan, không phải live approval. Không tái dùng consumed decision/event/receipt, đọc secret, tự nhập approval, retry UNKNOWN, bỏ identity hoặc nới DB allowlist. A4.3 vẫn chỉ một RAW/GLOSSARY call theo packet hiện hành (M2); RECONCILE (M4), đối chiếu trạng thái ngoài (M2b) và L2/L3 không thừa kế quyền đó. Quyền cho cả chuỗi (trần call/USD theo phase) chỉ xin sau khi M2 cho số cost/thời gian thật.

Trước pilot đầy đủ, chuẩn bị một bảng scope review được: chương/nguồn/hash, device/app/model route, từng phase/số call tối đa, input/output cap, thời gian, tổng trần USD, timeout/unknown behavior. Có thể xin phạm vi bao trọn chuỗi và chuyển phase xác định để giảm hỏi lặp; chỉ áp dụng sau owner cho phép, không hồi tố A4.3. Giá/capability cần thì kiểm nguồn chính thức tại lúc chọn cấu hình, không đoán.

Chưa chốt tên ba chương và tổng budget chuỗi: cần trước pilot, không ngăn local work. Tái dùng bộ pilot có sẵn nếu đáp ứng tiêu chí/quyền; không hỏi lại dữ liệu đã có. Không migration/đổi schema nếu chưa có failing persistence test. Không marketplace, whole-book batching, auto matching, RSC, provider framework mới hoặc redesign UI tổng thể. UI tối thiểu để chạy/xem/export final là phần bắt buộc P6.

Giữ bản gốc pack V5-SAFE.4 read-only; mục tiêu V5-SAFE.4.1.3-FULL theo ABI hiện có, đúng bốn root entries và ba authority files. Normal mode cần RAW/DRAFT/Glossary 5 cột/Pronoun 7 cột; alternate mode chỉ do người dùng chọn, Pair Context tùy chọn. Tương thích pack mới không tự nâng chain cũ; unknown capability -> ENGINE_UPGRADE_REQUIRED. Không đổi chế độ Translation Glossary4/Pronoun7.

## 8. Phản biện và tối ưu đã chọn

| Phản biện | Quyết định |
|---|---|
| Bỏ A4.3 để làm final ngay? | L1 acceptance còn thiếu; giữ đường chứng minh có giới hạn, nếu harness kẹt thì đổi môi trường có kiểm soát, không bỏ quality/data gates |
| 60 phút bảo đảm sửa xong? | Không, là giới hạn chi phí một phương pháp; hết hạn phải đổi phương pháp, không nhận PASS |
| Bỏ intermediate giảm token? | Giảm export/import/giao file; không mặc định giảm model tokens vì phases vẫn cần dữ liệu. Tái dùng projection/compact wire, không cắt coverage |
| Một call xuất final nhanh hơn? | Không đáp ứng RAW-first/blind QA/predecessor contract; không chọn |
| Nhiều test xanh nghĩa gần xong? | Đo bằng RAW accepted -> L1 persisted -> L2 persisted -> final persisted -> reopen/export -> ba chương -> device QA |
| Receipt đúng nghĩa văn đúng? | Không; cần semantic QA theo authority và đối chiếu mẫu. App chỉ chứng minh các điều tính/kiểm được |
| Dừng conflict là hoàn thiện? | Chỉ chứng minh stop behavior; không thay ba final hợp lệ |
| Bỏ device QA để giao nhanh? | Không chứng minh app thực tế chạy/xuất đúng; host fixtures không thay thế |

## 9. Tài liệu và định nghĩa hoàn thành

- Scope/order: file này. Authority nguyên trạng: `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE`.
- ABI/hashes authority: `docs/EDITORIAL_PACK_V1_4_1_3_INTEGRATION.md`; không sửa pack chỉ để che lỗi triển khai.
- Acceptance: `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` (Luna soạn, primary review).
- Ownership/gap map: `docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md`; phần hiện hành đứng trước P0 history.
- Evidence: checklist v4.18 hiện có, không tick vì plan đã viết. State: BUILD_STATE và WORKSPACE_SNAPSHOT.

Sản phẩm hoàn thành khi ba final hợp lệ lưu/mở lại/xuất đúng, quality/recovery/identity gates có evidence, cùng APK nhập 4.1.3 và compatible-next mà không rebuild, unknown capability bị từ chối đúng, Translation regression đạt, numbered build được lưu hai nơi và device/release QA đạt. Lịch sử mất dữ liệu code196 giữ FAIL; không đòi tái tạo bằng chứng không còn để hoàn tất scope mới, cũng không nhận đã phục hồi.

Kế hoạch hoàn thành khi mọi mốc có output/exit/failure action, phân biệt code với live evidence, có đường tới final trong app và giới hạn vòng sửa, không nới live scope ngầm. Một lượt Luna phản biện đủ; sửa findings cụ thể, không mở audit tổng quát tiếp.

Review 2026-10-01: Luna soạn acceptance và phản biện giới hạn năm điểm. Đã làm rõ ba chain riêng, dependency chapter/budget trước live, fixture/evidence compatible-next, failpoints atomic, và liên kết ba final với artifact cuối. Primary đối chiếu source authority/L1 engine/UI/validators và kiểm tài liệu; không nhận bất kỳ runtime/test/build PASS mới nào từ lượt này.

## 10. Next action duy nhất

Offline entry-boundary đã đóng (2/2 vòng, ≈30/60 phút). M0 chỉ-đọc ngày 2026-10-01 cho thấy APK/version khớp pin, nội dung DB khớp tuple tươi với lineage bằng 0, hash file DB `8D084050…` lệch pin cũ. Owner đã đồng ý re-pin: helper, child contract, manifest, command và entry được cập nhật theo thứ tự; candidate mới `D8E4D0FCB346491E4E9A9EF45C0890339BA67A54F47B2569026F26DF96704F0D` (commit `68f0e8da`); test liên quan PASS và kiểm lại từ extract `git archive` độc lập. Nguyên nhân lần owner-window lịch sử vẫn UNRESOLVED.


Cửa vào launcher đã chạy trọn vẹn trong cửa sổ owner thật (2026-10-01 09:25); event A4.3 đó đã dùng và kết thúc `EXTERNAL_CALL_STATE_UNKNOWN`, RAW chưa chấp nhận. Nguyên nhân đã xác nhận: APK test `058BE851…` hard-code hash DB cũ `3563f44b…`, nên re-pin phía host sang `8D084050…` làm test thất bại trước khi đọc key/DB/mạng (DB After = Before, lineage 0; chính thức vẫn UNKNOWN, không suy ra `$0`). Next action: owner chọn hướng sửa — khuyến nghị lấy kỳ vọng hash DB từ tham số launch do helper pin, build lại APK test bằng `scripts/build-and-save-android-test.ps1` (offline), re-pin theo chuỗi helper → manifest → command → entry, rồi trình hash mới để duyệt riêng việc thay test package và một event mới. Không mở vòng launcher thứ ba.
