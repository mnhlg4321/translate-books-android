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
| Workspace | `D:\App Translate Books`, `feature/v4.18-p5e-runner-repair-20260917`, HEAD `3057919b` (2026-10-01, local = origin) | Giữ continuation này, không quay về branch theo banner lịch sử |
| Nguồn | Staged/working tree khác nhau; entrypoint có sửa chưa stage | Không commit index cũ như candidate đã test |
| Build phát triển | `4.18-dev.1`/code208, source snapshot `240cdc814a324bca36543a8091af6bf6cdd07831`; APK SHA-256 `DB3FE9056A3477FA18D4F9F44A18E1FCCA0F7195F95DD99C91F1BD60AA6FE465` đã đối chiếu | Chưa cài; không chứng minh L1–L3 |
| Pilot | Last-known production `4.17-p5e.11`/code207; A4.3 pins giữ nguyên | Không thay bằng code208 mà kế thừa acceptance cũ |
| P0–P4 | Có bằng chứng lịch sử hoàn tất; tái sử dụng importer/binding/preflight | Không khởi động lại P0–P4 |
| P5 | Event 7: RAW (`L1_RAW_DISCOVERY`) commit atomic trên máy, DB `9fa69f6b…` giữ nguyên; verifier formal `RAW_NOT_ACCEPTED` chỉ vì 2 trường mô tả do lỗi collector (xác minh bổ sung: `docs/P5E_EVENT7_SUPPLEMENTARY_VERIFICATION_20261001.md`) | Chưa có RECONCILE/REPORT_L1 thật; P5.4/P5 exit chưa đạt; dùng làm predecessor cần quyết định owner (phương án B) |
| P6/P7 | Chưa hoàn tất | Enum/schema không chứng minh luồng sản phẩm đã chạy |

Last candidate: `scripts/p5e-a43-pre-reservation-launcher-entrypoint.ps1`, SHA-256 `29029B4620887A0752F52A57FB3CAC54C451FCA9C4518EB513C33DD8CAE082B5` (pin lại 2026-10-01 sau khi APK test được build lại; thay `D8E4D0FC…` của event đầu và thay `68DF8061…` là candidate của lần owner-window thất bại gần nhất). Last visible attempt: DecisionId `p5e-a43-owner-4abe5015e63643c0b5bf31b1c9576c3a`; audit dự kiến dưới `D:\P5E-private\.p5e-a43-audit` không được quan sát. Giữ nguyên sự kiện, không thử lại.

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

## 9a. Nhóm việc hiện hành sau event 7 (2026-10-01)

Thay thế các hàng P5/P6 tương ứng ở mục 5 về mức chi tiết; thứ tự P5 → P6 → P7 giữ nguyên. Một nhóm triển khai active; nhóm offline độc lập được làm song song khi không phụ thuộc quyết định live.

| Nhóm | Gap cụ thể | Thay đổi | Phụ thuộc | Đầu ra / kiểm thử | PASS | Dừng / đổi cách |
|---|---|---|---|---|---|---|
| G1 Bảo toàn + xác minh event 7 | Verdict formal `RAW_NOT_ACCEPTED` vì 2 trường mô tả; artifact là RAW-phase, không phải REPORT_L1 sau RECONCILE | Không sửa evidence; tái lập verdict bằng verifier gốc trên bản sao; xác minh 2 trường từ bytes APK đã pin | Không | `docs/P5E_EVENT7_SUPPLEMENTARY_VERIFICATION_20261001.md` + `docs/evidence/p5e-event7-supplementary-20261001/` | **Đạt 2026-10-01**: repro trùng byte, 24 file gốc không đổi | Owner chọn B (khuyến nghị) hoặc C; A không được dùng để đổi verdict |
| G2 Đóng điều kiện P5 (M4–M7) | RECONCILE bắt buộc theo source. Trước sửa: output RECONCILE bắt model chép identity hex + toàn văn (cùng loại lỗi event 6); không có entry chạy RECONCILE trên RAW đã commit; route fresh chỉ cho RAW | Offline (đã làm 2026-10-01): compact wire cho cả hai pha L1, app materialize DRAFT cho RECONCILE và kiểm `RECONCILE_DRAFT_MATERIALIZATION_MISMATCH`; `executeReconcile` đọc RAW đã commit, không dispatch RAW lại. Còn lại: mở route RECONCILE (một dòng guard), runner RECONCILE, build/pin APK, readback | Quyết định owner G1 (B/C); quyền M4 riêng (1 call, trần USD/token); build APK mới | JVM tests engine/app; androidTest compile; sau quyền: một event M4, readback REPORT_L1 phase `L1_RECONCILE` + receipt, restart/reopen (M6) | M4–M7 có evidence; P5.4/P5 exit ghi checklist | UNKNOWN: không retry. Không mở rộng supervisor RAW 5.600 dòng nếu thay đổi vượt phần readback phase-specific: khi đó chạy M4 qua coordinator app (cùng engine/store) và readback DB chỉ-đọc |
| G3 L2 coordinator/persistence (active offline) | Không có thực thi L2; bảng attempts có `CHECK(phase IN ('L1_RAW_DISCOVERY','L1_RECONCILE'))` nên không chứa được L2 (bằng chứng cần migration) | (1) **Đã làm**: `EditorialChangeMapReconstructor` — app dựng DRAFT→VI_L2 từ change rows gắn line/anchor, Error ID, Speaker Proof (thiếu → hoàn nguyên), protected line → hoàn nguyên, anchor sai → REPAIR_REQUIRED, diff thật đối chiếu chéo, CHANGE_MAP canonical. (2) Wire L2 compact: model chỉ trả change rows/preserved/disposition, không trả toàn văn. (3) `L2` execution trong engine: gate predecessor (REPORT_L1 phase RECONCILE, cùng binding/bundle/DRAFT hash) → INPUT_REQUIRED; L2_RAW_DISCOVERY đối chiếu candidate counts; L2_EDIT → reconstruct → commit. (4) Store: migration một bảng artifact theo phase, VI_L2 + CHANGE_MAP_L2 trong **một hàng** (atomic theo cấu trúc) | REPORT_L1 thật chỉ cần cho live; offline dùng fake | Reconstructor 13/13; tiếp: fake-provider tests valid/no-edit/preserve, predecessor sai, thiếu Error ID, DRAFT đổi, thoại thiếu proof, failpoint trước/sau commit + reopen | L2 đúng predecessor/coverage, reopen đúng, crash không để nửa kết quả | Hai vòng cùng failure signature → đổi cách; không đổi schema ngoài bảng mới |
| G4 L3/final QA + atomic persistence | Chưa có thực thi L3; release numbers chưa tính từ dữ liệu thật | Tái dùng reconstructor kind `L3_VI_L2_TO_FINAL`; blind raw-first (RAW+GLOSSARY+VI_L2), reconcile, hai lượt phản biện; app tính 5 release numbers; FINAL_QA + QA_RECEIPT cùng hàng | G3 | Fake tests: rò nguồn ẩn bị từ chối, thoại không proof, regression protected, model PASS không bằng chứng | Final hợp lệ lưu/mở lại; gates đúng | Không rerun cả chuỗi; sửa phase hỏng |
| G5 UI/recovery/save/reopen/export | `EditorialPageFactory` khóa execution | Nối cùng coordinator; tiến độ, typed stop, recovery; xem final; export TXT UTF-8 + đọc lại hash | G3, G4 | Instrumented: một chương đi hết, process death, export lỗi/hủy giữ final | Một chương thật L1–L3, TXT khớp FINAL | Sửa app path; CLI/test-only không thay |
| G6 Ba final + regression/device QA | Chưa có | Ba chain riêng (ngắn, dày thoại, dài); full regression, wrapper build, hai archive, device QA | G5; owner chốt chương + ngân sách chuỗi | Bảng evidence theo mục 5 | `PRODUCT_FINAL_OUTPUT_ACCEPTED` | Conflict thật: thêm chương hợp lệ, không hạ tiêu chí |

Ước lượng chi phí L2/L3 trước live: lấy số đo thật event 7 (RAW: 21,143 prompt / 535 completion token, USD 0.00295, 8.2 s) làm mốc; L2_EDIT thêm DRAFT + REPORT_L1 (~2× prompt) và output là danh sách change rows (không phải toàn văn). Bảng call/cap/USD theo phase lập khi G3 có wire cố định.
Tiến độ G5 offline (2026-10-02, `a6da1a35`): `EditorialChapterFinalCoordinator.inspect` đọc tiến độ L1→L2→L3→FINAL chỉ từ hàng bền (không claim/dispatch/repair); `EditorialChapterProgress` (JVM) phân loại stage, typed stop và next action — hàng `CLAIMED` sau process death hoặc `RECOVERY_REQUIRED` là quyết định phục hồi của owner, không bao giờ tự retry; thẻ chương P4 có tiến độ, xem bản cuối và Xuất TXT qua SAF (ghi đúng byte FINAL, đọc lại SHA-256/độ dài, lỗi/hủy không đụng FINAL đã lưu); chạy L2/L3 vẫn khóa chờ cấp phép riêng. App unit 298/298, lint PASS, androidTest compile PASS (4 test instrumented mới chưa chạy trên máy; UI chưa kiểm trên máy). **Phát hiện:** `L2_RAW_DISCOVERY` (đọc RAW độc lập, tái lập candidate counts) chưa được nối: REPORT_L1 thật chỉ mang `populationTotal=1`/`accountedTotal=1` (một population app-owned, không có đếm UNIT/TG/SR/RC), nên đối chiếu count hiện không có sức phân biệt; cần quyết định (xem mục 10).

Tiến độ P6 W1–W3 (2026-10-02, phiên theo `docs/P6_G5_G6_WORK_REQUEST_20261002.md`): cột "Đã chốt" D1–D4 trong bản commit `f9b1bec0` vẫn **trống** (chỉ có khuyến nghị) nên chỉ làm W1–W3 (không cần quyền), giữ W4–W6. W1 (`a8c01b79`, theo hướng (d) khuyến nghị): `L2_RAW_DISCOVERY` là cuộc gọi mù RAW + GLOSSARY lập candidate UNIT/TG/SR/RC; `L2_EDIT` nhận danh sách như block `L2_RAW_CANDIDATES` do app sở hữu và phải giải quyết từng candidate; app đếm (thiếu/UNPROCESSED → REPAIR_REQUIRED, CONFLICT → CONTENT_BLOCKED), ghi `rawDiscovery` vào CHANGE_MAP_L2; trần riêng từng cuộc gọi qua `EditorialChainBudgets`. W2 (`bc10aca1`): `EditorialChapterRunService` là cửa duy nhất tới chuỗi live (xác nhận trần + trần hợp lệ + preflight route/key trước khi claim + khóa một-writer-một-chương + trạng thái bền chưa từng chạy); thẻ chương có hộp thoại cấp phép in đúng trần. W3: cặp `4.18-p6.2`/215 (APK `51BA2A2B…9703`) + AndroidTest `41ED827E…6500` build bằng hai script wrapper, lưu hai nơi; trên emulator `emulator-5554`: FinalCoordinator 7, PhaseArtifactStore 8, ReconcileLineage 5, ExactBindingFakeE2E 19 đều OK; UI smoke thẻ L1, hộp thoại, từ chối khi chưa có key (0 hàng artifact), thẻ FINAL, xuất SAF SHA-256 = FINAL. Chi tiết `docs/P6_W3_EMULATOR_QA_20261002.md`. Chưa có lượt provider thật nào; pilot vẫn 213.

Tiến độ P6 W4–W6 (2026-10-02, owner đã ☑ D1–D4, commit `10e3cd60`): đã cài `4.18-p6.2`/215 lên pilot (sao lưu DB, M0, readback khớp) và chạy **một** chuỗi L2/L3 cho chương 001 từ UI: `L2_EDIT` + `L3_FINAL` COMMITTED (VI_L2 = FINAL `a7d5f99e…`, 26,466 B), release numbers = 0, 4 call, 0 repair/retry, USD 0.04257 (trần 0.30); mở lại và xuất TXT trên máy có hash = FINAL. Ứng viên `FINAL_OUTPUT_ACCEPTED` cho chương 001, chưa nghiệm thu: owner chưa đọc chất lượng, tiêu chí "L1 đóng đủ ledger" chưa đạt như văn bản (REPORT_L1 là bộ khung), và các đếm candidate không tái lập giữa L2 (49) và L3 (257). Chi tiết `docs/P6_W4_W6_CHAPTER_001_EVIDENCE_20261002.md`.

Owner chấp nhận chất lượng FINAL chương 001 (2026-10-02): W6 đạt. Chương 001 ghi nhận là *quality-accepted* (không đóng chính thức `FINAL_OUTPUT_ACCEPTED` vì hai khoảng trống tiêu chí: L1 là bộ khung, đếm candidate L2 49 ≠ L3 257 — owner mới chấp nhận chất lượng). 1/3 chương đại diện. Ứng viên hai chương còn lại: `docs/P6_G6_CHAPTER_CANDIDATES_20261002.md`.

### G2 chi tiết — M4 RECONCILE chương 001 (nhóm active sau quyết định B)

Quyết định owner 2026-10-01: **B** — RAW event 7 (attempt `7a5e3428…`) là predecessor duy nhất cho M4; verdict formal giữ `RAW_NOT_ACCEPTED`. Quyết định này không cấp quyền cài APK, migrate DB hay gọi provider.

| Bước | Việc | Trạng thái | PASS / dừng |
|---|---|---|---|
| M4.a App | Compact wire RECONCILE + `executeReconcile` (`7763085e`); adapter fresh chỉ cho RECONCILE (`withFreshReconcileLifecyclePersistence`), `dispatchReconcile` yêu cầu lineage đúng trạng thái event 7 (`inspectReconcileLineage` = READY), `dispatchRaw` không đổi byte | **Xong** `4a603695` (app 268/268) | JVM tests + androidTest compile |
| M4.b Runner thiết bị | `EditorialP5EReconcileLiveInstrumentedTest`, opt-in `p5e_reconcile_live=YES`; pin APK/DB nhận qua argument và đối chiếu thiết bị; 1 call, 0 repair/retry, ≤USD 0.05, ≤120 s | **Xong** `1f8e7334` (compile) | Compile; không chạy nếu chưa có quyền |
| M4.c Host | Script gọn dùng thư viện supervisor (`-LibraryOnly`: xuất DB Before/After chỉ-đọc, đọc lại SQLite trên host, hash APK), một lần `am instrument`, ghi outcome. **Không** mở rộng supervisor RAW | **Xong** `a64ae460` (self-test 74/74, chưa chạy thiết bị) | Self-test bằng DB fixture; không chạm thiết bị khi test |
| M4.d Build | `build-and-save.ps1` production + test APK mới. Bản build từ HEAD có schema v25: mở app sẽ migrate DB pilot v24→v25 (thêm một bảng). Bản sao lưu trước: ảnh `database-snapshot-after` của event 7 (`9fa69f6b…`) + bản copy chỉ-đọc ngay trước cài | **Xong 2026-10-02**: `4.18-p5e.4`/212 APK `8B6A4683…` + test `26CB0563…` build/lưu hai nơi, cài trên `15e84958`, đọc lại khớp; DB sao lưu trước cài `9FA69F6B…`; migration v24→v25 xảy ra khi app mở DB trong event, chỉ thêm `editorial_phase_artifacts` + `idx_editorial_phase_artifacts_chain` (đã đối chiếu) | Hash APK/DB sau cài đọc lại khớp; migration chỉ thêm `editorial_phase_artifacts` |
| M4.e Event | Một RECONCILE call | **Lần 1 (2026-10-02) không dispatch** (`P5_TOKEN_BUDGET_EXCEEDED`, `providerCalls=0`, sửa `e2e1d3c5`). **Lần 2 (2026-10-02) `RECONCILE_COMMITTED`**: attempt `7483b211…`, REPORT_L1 `b33baf33…`, receipt `11754968…`, 1 call, USD 0.00747405; chi tiết `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` mục Event M4 lần 2 | Readback: attempt `L1_RECONCILE` COMMITTED, report `phase=L1_RECONCILE`, predecessor `7a5e3428…`, receipt atomic, lineage +1; UNKNOWN → không retry |
| M5–M7 | Restart/reopen đọc lại REPORT_L1 + receipt cùng hash; ghi P5.4/P5 exit | **Xong 2026-10-02 ở mức app/DB**: M5 đạt (hàng atomic, đúng schema/phase/predecessor, lineage +1); M6 đạt (force-stop, DB export trùng byte `06C4C48E…`; chưa qua UI); M7 ghi vào checklist | Theo bảng mốc mục 5 |

Phạm vi quyền sẽ xin một lần khi M4.a–c xong: cài production + test APK mới trên serial `15e84958` (kèm migrate v25 có sao lưu), một RECONCILE call trần USD 0.05 / 104,096 token / 120 s, đọc logcat `P5E_RAW` chỉ-đọc. Ước tính chi phí theo event 7: ~USD 0.003–0.006.

Tiến độ offline G3/G4 (2026-10-01): adapter OpenRouter cho wire L2 `63b288fe` (app 276/276); L3 re-audit mù + reconcile + 5 release numbers do app tính, FINAL + QA_RECEIPT một hàng `e8b21d26` (engine 244/244). Còn thiếu: L2_RAW_DISCOVERY đối chiếu candidate counts, adapter OpenRouter cho L3, coordinator app nối L1→L2→L3 với store (G5), UI/export.

## 10. Next action duy nhất

**Hiện hành (2026-10-08, Q2.5.4 stopped):** Q2.5.1–2.5.3 đã hoàn tất; build `4.18-q2.2`/code246 từ `abca34cd` qua wrapper, cài emulator-5554. Q2.5.4 canary chương007, V5-luna/medium: tiền kiểm pack PASS (4 file, Project Instruction 1 lần), account MATCH; lượt đầu trả `V5_STOP_INPUT_ARTIFACT_MISSING`, app `RETRY_REQUIRED`, 1 call, 18,145 input / 2,924 output tokens, USD 0.0080449, 0 UNKNOWN. Dừng theo quy tắc; không chạy dev, E-luna-b hoặc holdout, không xuất holdout. Ledger 11 calls, USD 0.8455608 / stored cap USD 6.00, pending 0. Semantic quality NOT_MEASURED. Engine 572/572, app mỗi variant 439/439, Python 80/80, focused emulator API 14/14 PASS. **Next action duy nhất:** owner xem response riêng tại `D:\P5E-private\q2-runs\Q2.5\V5-luna-007-73771fb2-cb0f-4937-9e4f-1dbdacd0064c\results\fx-a01\v5-chat\01-response.txt` và quyết định có giao một gói riêng để khảo sát cách gửi input artifact; ma trận dev/holdout hiện dừng.

**Trước đó — Hiện hành (2026-10-08, review Q1):** test sạch `4ee3518c` PASS (engine 561, app 431, Python 75); prompt không chứa FINAL. Bộ chấm Q1 thổi phồng recall (DRAFT để nguyên đạt 0.382) — coordinator đã sửa `score_vs_final.py` + test. Chấm đúng: mọi cấu hình chỉ đưa 5–8% dòng owner sửa lại gần FINAL hơn, sửa thừa nhiều hơn sửa trúng; chuẩn hóa Q1.2 không áp được chương thật nào (1/28 đủ điều kiện); C2 là model rẻ hơn nên giả thuyết model mạnh chưa đo. Bản biên tập CHƯA ĐẠT; 0/3 chương. D-Q2 đã duyệt (trần USD 6.00) và U1 tạm hoãn (2026-10-08). Next action duy nhất: Codex làm Q2.1–Q2.3 offline rồi Q2.4 live trong `docs/EDITORIAL_API_V1_Q1_REVIEW_AND_Q2_REQUEST_20261008.md` (đường v5 chat 3 lượt + model mạnh), model mạnh = GPT-5.6 Sol reasoning medium (owner xác nhận là model đã tạo FINAL, mục 8), pack mặc định 4.1.3 FULL CHATGPT.

**Trước đó — Hiện hành (2026-10-07 đêm, owner từ chối 3/3 chương N6):** so với 28 bản FINAL của owner, app gần như không thêm giá trị (owner sửa 9–19% dòng, app sửa ~3% và trùng 9/51; 4/13 sửa của app là lỗi mới, gồm chữ Hán `三` và `？` toàn khổ; sót tiếng Nhật 001 L91). Nguyên nhân chính: Quality Core giữ ký hiệu sai của DRAFT và ép sửa tối thiểu; fixture N5 không đại diện; việc tất định (ký hiệu theo RAW, sót kana, thuật ngữ) đang giao cho model nhỏ; năng lực model chưa đo. Next action duy nhất: Codex làm Q1.1–Q1.4 (offline) trong `docs/EDITORIAL_API_V1_Q1_QUALITY_PLAN_AND_REQUEST_20261007.md`; Q1.5 live đã được owner duyệt (D-Q1, trần USD 2.00) và chạy ngay sau khi Q1.4 PASS. U1 (UI) tiếp tục song song với ownership tách bạch. 0/3 chương đạt; P7 chưa bắt đầu.

**Trước đó — Hiện hành (2026-10-07 tối, review N6 + thiết kế U1):** coordinator xác minh N6: 24/24 hash phân xử N5 khớp `final.txt` riêng; 3 chương N6 sạch cấu trúc (số dòng = DRAFT, chữ 0.999–1.000, đổi 5/4/4 dòng), chất lượng NOT_MEASURED tới khi owner đọc (trang riêng `D:P5E-private
6-review
6-review.html`). Owner yêu cầu thiết kế UX: thư viện nguồn lưu RAW/DRAFT, màn chọn kiểu danh sách file có chip lọc và chọn theo bộ chương, Cài đặt Biên tập. Next action duy nhất: Codex thực hiện U1 theo `docs/EDITORIAL_API_V1_UX_U1_DESIGN_AND_REQUEST_20261007.md` mục 10 (offline + emulator, 0 provider call); song song owner đọc và chấp nhận/không chấp nhận 3 chương N6. P7 chỉ sau 3/3 chương được chấp nhận và U1 PASS.

**Trước đó — Hiện hành (2026-10-07, sau gói N6 4A–4C):** build production `4.18-api.6`/code243 và AndroidTest đã qua wrapper từ source `81e4d578`, archive đồng nhất trong `artifacts/` và `backup/`, cài chỉ trên `emulator-5554`. `EditorialApiStore` 4/4, `EditorialApiBienTapFlow` 3/3, UI whole 2/2, `EditorialPairStore` 6/6, Pair UI 3/3; hai chuỗi process-death `seed → force-stop → verify → cleanup` đều 3/3. Full instrumented package chạy 246 test, 235 PASS và đúng 11 failure lịch sử; không có failure mới trong API whole/pair, pilot untouched. Bằng chứng 4A–4C: `docs/EDITORIAL_API_V1_N6_EXECUTION_20261007.md`, log riêng tại `D:\P5E-private\n6-4a-*`, ledger/output tại `D:\P5E-private\n6-logs\` và `D:\P5E-private\n6-outputs\`. Phát hiện lỗi N5 (bản cụt 37/192 dòng thành FINAL_NOTES) đã được sửa bằng `EditorialApiFlow.sizeBlock` (BLOCK mất/gấp chữ → retry kỹ thuật → RETRY_REQUIRED giữ DRAFT), engine 553, app 431; chunk-pair đóng băng offline theo D-CP. Phân xử 4B đủ 24 run trong `docs/EDITORIAL_API_V1_N5_ADJUDICATION_20261007.md`: chế độ mặc định là **Nhanh (E)** theo plan §6, không đổi acceptance/prompt/ngưỡng. 4C đã chạy đúng 3 chương whole-flow, 3 provider calls, settled USD `0.01967015`, 0 pending/UNKNOWN, combo và kết quả sống qua force-stop/reopen, ba TXT export được hash; chất lượng model vẫn `NOT_MEASURED`. Next action duy nhất: owner review báo cáo N6 4C; dừng trước provider/device/pilot/chunk-pair/G2 tiếp theo.

**Trước đó — Hiện hành (2026-10-07, cập nhật sau N5, CP-OFFLINE-2 và CP-IMPL-1):** hướng là EDITORIAL_API_V1 (D-N1..D-N4 đã duyệt; SAFE4 8-call đóng băng, G2 hủy). N5 đã chạy live 24 run, 41 lượt gọi, settled USD 0.26563504, 0 pending; hai nhánh cùng sửa 19/25 target thô, scorer tự động FAIL do sửa ngoài target, chỉ tiêu lỗi mới vẫn `NOT_MEASURED`, chưa chọn chế độ mặc định, 0/3 chương được chấp nhận. Một run Kỹ (`fx-a04`) trả bản cụt 37/192 dòng và vẫn thành `FINAL_NOTES` (chi tiết `docs/EDITORIAL_API_V1_N5_RESULT_20261006.md`). Gói offline `docs/EDITORIAL_API_V1_CHUNK_PAIR_OFFLINE_PACKAGE_20261006.md` revision `CP-OFFLINE-2` (document-only, chưa có evidence runtime) đã được chỉnh: tách separator biên ghép khỏi layout nội bộ của candidate, ngưỡng SIZE theo bậc kèm đối chứng sửa đoạn hợp lệ và chunk ngắn, đường xử lý WARN (`WARN_REVIEW`), đặc tả scope pronoun trước MAIN/CONTEXT, reservation ledger trước dispatch (trạng thái `E_RESERVED`/`E_SENT`, `RESERVE_FAILED`) và công thức cap `max(1.20 × baseUsd, peakExposureUsd)`; bảng test và state machine đã đồng bộ; lần chỉnh sau đó bỏ BLOCK dựa trên số dòng (chỉ WARN `LINE_DELTA`, BLOCK theo chữ và marker, kèm đối chứng gộp dòng giữ nguyên chữ, khôi phục đoạn thiếu và replay N5) và bảo toàn phạm vi pronoun (effective scope trong prompt và dedupe với `from` trong khóa gộp, xung đột cùng phạm vi giữ nguyên không tự suy ưu tiên, đối chứng và phản ví dụ W/C); dấu so sánh ngưỡng chữ chốt bằng số nguyên, các ngưỡng chỉ là hàng rào nghi ngờ, và `REFLOW_ONLY` không sinh khi candidate nguyên văn. **Triển khai offline `CP-IMPL-1` (2026-10-07, host, local commit, không push):** engine (manifest RAW/DRAFT, PairMap tường minh, projector reference, structural gate số nguyên, prompt/merge/trạng thái) và app (import read-only từ job Dịch, run service reservation-trước-dispatch + journal + phục hồi, store SQLite v27 additive, UI tiếng Việt chọn job/bỏ chọn glossary-pronoun/xem cặp/tiến độ/diff/kết quả/xuất có nhãn) đã có test host xanh (engine 551, app 431). Bất biến W/C được sửa thành C ⊆ W (cue xét trong đơn vị cue của từng arm) và phản ví dụ khóa dedupe thiếu `from` được viết lại cho đúng. androidTest (store SQLite thật, migration v26→v27, job read-only, cửa sổ ngắt, UI, mở lại sau process death) mới được viết và compile, **NOT_RUN**; semantic NOT_MEASURED; 0 lượt gọi provider, USD 0; 0/3 chương được chấp nhận, P7 chưa đạt. Đề xuất đo W/C (N + P_total, reservation, cap, stop rule, thứ tự, đối chứng sạch, lỗi đã biết, holdout) đã chuẩn bị ở §9.4 của gói, chưa được duyệt. **Next action duy nhất:** owner review evidence offline `CP-IMPL-1` và APK phát triển đã lưu trữ; chưa cài thiết bị, chạy provider, N6, push hay phát hành trước quyết định đó. Mục 1–9 mô tả chain L1–L3 là lịch sử cho SAFE4; API_V1 nghiệm thu theo `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` mục 1A.

**Lịch sử next action trước N5 (2026-10-06 sáng, giữ để tra cứu; đã được thay bằng đoạn trên):** Hướng P6 đổi theo D-N1..D-N3 sang EDITORIAL_API_V1; SAFE4 8-call đóng băng, G2 hủy. Review 2026-10-06 tại `5bea4c93` xác minh archive code240/source18571c01, nhưng thu hẹp kết luận N4: kiểm UI hiện chưa chứng minh chọn nguồn và mở lại cùng tổ hợp sau process restart. Hai sửa nhỏ local đã có regression trước FAIL/sau PASS (API engine 63/63, app 44/44): giữ chi phí/call khi hủy sau dispatch và không coi check mất nội dung là FINAL_OK. Chưa commit/build/cài các sửa này. Còn thiếu reference ở C/C2, UNKNOWN/retry và quyết toán chi phí đúng, giữ giá trị form qua refresh, bằng chứng UI/reopen và đồng bộ acceptance/matrix N5. Cập nhật 2026-10-06 (chưa commit): các thiếu sót offline đã được đóng trên host (reference C/C2, UNKNOWN/chi phí, form giữ giá trị, acceptance 1A, ma trận N5 24 run/25 target mỗi nhánh); còn thiếu bằng chứng thiết bị và bản build lưu trữ cho source mới. Chi tiết, ownership và PASS/FAIL ở mục Independent review của `docs/EDITORIAL_API_V1_N1_N4_EXECUTION_20261005.md`. Next action duy nhất: đóng các thiếu sót offline trong gói N1–N4 hiện có, rồi trình lại evidence và D-N4 cụ thể; không chạy N5/provider/pilot trong gói review. Mục 1–9 mô tả chain L1–L3 là lịch sử cho SAFE4; hướng API_V1 theo plan mới và D-N1..D-N3, không dùng artifact lịch sử để nghiệm thu API_V1.

Lịch sử next action trước 2026-10-05 tối (giữ để tra cứu): Z1–Z5 đã hoàn tất; G1 base 8/8 và hai vòng lặp 4/4 qua cấu trúc. Điều đó chưa chứng minh chất lượng phát hiện của L1. Review baseline `006434da` phát hiện và sửa lỗi verifier dùng response tương lai để bỏ qua rò đáp án; 12 output đã lưu vẫn qua phép kiểm sửa. Next action duy nhất: hoàn tất gói chấm nội dung offline và đề xuất G2 cụ thể tại `docs/P6_R6_DECISIVE_FIX_PLAN_20261005.md` mục 7. D-Z3 đã được duyệt; G2 chưa được duyệt/chạy. Không chạy lại G1, gọi provider hay đụng thiết bị trong gói này.

Lịch sử: M4 lần 1 (2026-10-02) dừng trước provider vì gate byte-vs-token (sửa `e2e1d3c5`); lần 2 đạt `RECONCILE_COMMITTED`.

Lịch sử next action trước 2026-10-02 (giữ để tra cứu): M0 bị chặn vì máy chưa kết nối ADB; owner đã cắm máy và M4 chạy như trên. 

Lịch sử next action trước (giữ để tra cứu): event 7 commit RAW + REPORT_L1-phase-RAW + receipt (verifier formal `RAW_NOT_ACCEPTED` do collector); collector đã sửa ở `3057919b`. Offline entry-boundary đã đóng 2/2 vòng; nguyên nhân owner-window lịch sử vẫn UNRESOLVED.

## 11. Kế hoạch sửa và phản biện để Claude điều phối — 2026-10-02

Đây là chi tiết sửa trong P6 của canonical plan hiện có, không mở release/branch/checklist mới. Tài liệu bàn giao hỗ trợ: `docs/P6_LEDGER_QA_CLAUDE_HANDOFF_20261002.md`. Căn cứ: `docs/P6_CHAPTER_001_INDEPENDENT_AUDIT_20261002.md` và tiêu chí `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`. Mục tiêu là đóng mọi lỗi đã xác minh bằng test và evidence, không hứa mô hình ngôn ngữ sẽ không bao giờ bỏ sót lỗi mới.

### 11.1. Baseline và các kết luận phải sửa ngay

- Branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD khi lập kế hoạch `547c8a4d48f11856b29654e3d65749201d847efd`. Phải đọc HEAD/status thật khi tiếp quản; giữ dirty files không thuộc công việc. Build pilot lưu lại: `4.18-p6.2`/215 từ `4bc1aa27`; source có hai UX fix chưa build/cài.
- RAW/DRAFT/GLOSSARY chương 001 khớp binding pilot; PRONOUN khớp khi bỏ BOM. Pilot FINAL `a7d5f99e…` chỉ sửa `今回` ở dòng 237; FINAL thủ công của owner `ebb091d6…` có thêm sáu khác biệt nội dung và hai chỗ gộp đoạn. Không dùng reference thủ công làm đầu vào model hoặc tự coi mọi khác biệt là lỗi.
- L1 không sửa văn là đúng. Lỗi nằm ở việc không có Error Ledger đủ nội dung và việc report lưu bền bỏ các entry. `findingCount=0` không chứng minh model tìm được 0 lỗi: biến hiện không được gán. Không dùng metric này để chấm năng lực cũ.
- Receipt L3 có 4 probe, không phải 8. L3 không sửa dòng nào tự nó không chứng minh thất bại; lỗi nghĩa còn sót trong pilot FINAL mới là đối chứng.
- Phân biệt `踏破`/`攻略` theo ngữ cảnh RAW là lỗi nghĩa cần test; nhóm xưng hô `嬢ちゃん` là kiểm tra tuân thủ profile và speaker/scope; `特権階級` cần phân xử riêng, không tự tính là lỗi bắt buộc.
- Đọc thêm khi lập kế hoạch: `EditorialChapterFinalCoordinator.runToFinal` truyền `protectedLines = Set.of()` vì REPORT_L1 chưa mang protected spans. Vì vậy chỉ số protected regression bằng 0 chưa chứng minh bảo vệ span. `EditorialL3Execution` tạo `finalReadOrder` bằng danh sách cố định, và sau reconstruct QA edits đi tới commit, chưa có bước provider đọc lại FINAL đã dựng. Đưa cả hai vào phạm vi kiểm chứng/sửa, không coi marker tĩnh là bằng chứng đã đọc.

### 11.2. Phân công và thứ tự

Claude là điều phối chính, chịu trách nhiệm thiết kế contract chung, tích hợp, kiểm định kết quả sub-agent và cập nhật trạng thái. Mỗi nhiệm vụ có file ownership, đầu ra và test rõ; không cho nhiều writer sửa cùng file. Luna nhận việc nhỏ: đối chiếu fixture, rà schema/round-trip, rà bảng claim→evidence hoặc phản biện một nhóm test; không giao Luna tự kết luận toàn bộ semantic acceptance. Chỉ song song việc độc lập. Không tạo nhiều release track để chia việc.

Thứ tự: **R0 → R1 → R2 → R3 → R4 → R5 → R6 → R7**. Có thể chuẩn bị harness R5 song song sau khi contract R1 đã chốt. Các mã R chỉ là nhóm sửa trong P6, không phải phase sản phẩm mới. Khi một nhóm test fail: FAILED_REPAIRING, sửa/rerun cùng nhóm; không xin owner giải quyết lỗi code thường lệ. Hai vòng cùng failure signature: điều phối xem lại giả thuyết, không tăng gate hoặc hạ test để đạt PASS.

| Nhóm | Công việc/ownership chính | Điều kiện đóng nhóm |
|---|---|---|
| R0 — sự thật và đối chứng | Claude chốt bảng issue → bằng chứng → nguyên nhân → test; Luna đối chiếu RAW/DRAFT/pilot/manual và occurrence anchors | Phân loại confirmed defect / profile violation / preference / uncertain; mỗi lỗi xác nhận có expected semantic invariant, nguồn và cách chấm; không sửa file nguồn |
| R1 — contract và coverage | Engine contract, projector, parser, provider wire; định nghĩa inventory, Error Ledger, proofs, protected spans, version và budget | Schema/negative fixtures chứng minh biểu diễn được toàn bộ R0; có phương án vượt 4 findings và giới hạn 300 candidates, không cắt im lặng; giữ đúng visibility từng phase |
| R2 — L1 và lưu bền | `EditorialP5RawWireContract`, `EditorialP5RawWireResponse`, `EditorialP5PilotExecution`, provider L1 và store/readback liên quan | RAW discovery giữ đúng vai trò raw-first; RECONCILE xuất ledger đầy đủ; serialize→DB→restart→L2 giữ nguyên entry/proof; metrics tính từ dữ liệu đã validate |
| R3 — L2 và reconstruction | `EditorialL2Execution`, `EditorialChangeMapReconstructor`, provider L2, coordinator | Mọi L1 Error ID có cách xử lý và evidence; sửa đúng chứ không chỉ đủ ID; protected span thực đi vào runtime; final-read áp dụng lên đúng VI_L2 đã dựng |
| R4 — L3 và release | `EditorialL3Execution`, provider L3, receipt/release validators | Bắt được lỗi cài trực tiếp trong VI_L2; probe có anchors và lập luận cụ thể; nếu QA edit thì đọc lại đúng FINAL đã dựng trước commit; generic PASS không đủ |
| R5 — đánh giá độc lập | Harness offline/fake + bộ semantic evaluation được tách khỏi đáp án | Test cấu trúc đạt; fixture/holdout và scoring đóng băng trước chạy model; bảng ngân sách/call mới cụ thể, chưa gọi provider |
| R6 — build và xác minh live có phạm vi | Claude tích hợp, regression, wrapper archive, chuẩn bị rồi thực hiện đúng quyền live được owner cấp | Cùng code/prompt/schema được kiểm qua model thật, lưu evidence đầy đủ; không dùng fake tests để tuyên bố semantic PASS |
| R7 — tái nghiệm thu sản phẩm | Chạy chương 001 và hai category còn lại theo nguồn/quyền đã chốt, UI save/reopen/export; cập nhật checklist hiện có | Đủ ba chapter theo contract sửa, kiểm nội dung độc lập và technical gates; P7/tag/release chỉ sau gates hiện có |

### 11.3. Contract bắt buộc — sửa từ gốc, không chỉ tăng trần

**Tách bốn khái niệm:** raw unit/occurrence là phần nguồn cần kiểm; candidate là nghi vấn cần xét; finding là lỗi có chứng cứ; change là thao tác sửa. Không gộp một population acknowledgement thành một lỗi. Định nghĩa metric cùng schema: uniqueFindingCount, occurrenceCount, unresolved/preserved, applied/revertedChangeCount; số candidate không là số lỗi. Dữ liệu lịch sử thiếu metric phải ghi unavailable/legacy, không suy đoán 0 hoặc hồi tố PASS.

App dựng inventory RAW ổn định từ byte snapshot: đoạn/câu/thoại với offset và ID gắn source hash; quy tắc loại trừ blank/markup được ghi rõ. Scene grouping có thể là phân tích của model nhưng không thay inventory gốc. Mọi span nội dung thuộc inventory, mọi candidate/finding map về nguồn; lỗi nội dung thiếu trong DRAFT dùng RAW anchor và vị trí chèn, không cần giả một dòng DRAFT đang tồn tại. Hỗ trợ một-nhiều/nhiều-một; không ép 49=257. Theo dõi coverage hai chiều, orphan, duplicate và phần bị loại trừ có lý do. Raw coverage đạt không đồng nghĩa đã hiểu đúng nghĩa.

L1 Error Ledger tối thiểu: Error ID duy nhất ổn định trong report; loại/mức độ; RAW anchor(s), DRAFT anchor/range hoặc missing target; quan sát ngắn; expected meaning/ràng buộc; bằng chứng; quan hệ với candidate/occurrence; disposition và evidence-limit khi thiếu chắc chắn. TG/SR/RC/Pair/Speaker records phải giữ được dữ liệu mà pinned workflow yêu cầu, không chỉ status. Sửa lời thoại cần direct listener và căn cứ RAW/ngữ cảnh; protected spans phải có nguồn, scope và lý do. Không bảo vệ cả chương chỉ để làm chỉ số regression đẹp.

Phân biệt source role trong anchor. App xác minh bounds, hash/quote khớp, ID/reference hợp lệ và coverage phép đếm; model/reviewer chịu phần suy luận nghĩa. Một quote đúng không tự chứng minh kết luận đúng. Không yêu cầu model xuất chain-of-thought; chỉ cần lập luận kiểm chứng ngắn và trích đoạn liên quan.

Lưu bền đầy đủ inventory, ledger entries, evidence, preserved/protected spans trong report/receipt hoặc artifact nội bộ được hash-link. Reopen phải phục hồi được nội dung tương đương, không chỉ tổng số. Sửa `findingCount` theo định nghĩa mới và thêm fixture 0/1/nhiều finding + nhiều occurrence cùng một finding. Số byte/token/call/cost, truncation và model/prompt/schema revision phải truy vết được; nếu provider không trả cost thì ghi unknown/estimated đúng nguồn.

**Version/compatibility:** tăng revision cho wire/report/validator thay đổi không tương thích và bind vào identity. Report cũ vẫn đọc để xem lịch sử, không được coi là đủ ledger theo contract mới. Không sửa blob/receipt cũ, không tái dùng COMMITTED cũ cho chain đã đổi semantics. Xác định rõ predecessor nào tương thích; nếu RAW inventory đổi, chạy lại RAW khi được phép. Không mặc định có thể kế thừa event 7. Migration chỉ khi cần, ưu tiên artifact blob hiện có; test upgrade/reopen/rollback trước pilot.

**Giới hạn tải:** lập sizing theo chương ngắn/dày thoại/dài và payload đầy ledger. Ưu tiên một response khi vừa; nếu vượt, chunk theo inventory/scene với context chồng lấn cần thiết, ownership rõ, dedup và tổng hợp không mất mục. Cross-scene speaker/listener phải được kiểm. Freeze quy tắc chunk/offset; test Unicode, CRLF, BOM, cuối file, gộp/tách/chèn/xóa đoạn. Không buộc model nhận biết mọi lỗi bằng cách tăng vô hạn trần. Mỗi call/chunk, tổng phase và chain có budget/token/time/call cap; thiếu chunk hoặc truncation không được commit report đầy đủ; UNKNOWN không tự retry.

### 11.4. Trách nhiệm L2/L3 và gate nội dung

L2 phải xử lý từng L1 finding: sửa với Change ID/diff chứng minh, bác với RAW evidence, preserve có exact span/evidence-limit theo contract, hoặc unresolved với typed stop thích hợp. Finding mới do L2 tự phát hiện phải được thêm và truy vết. Không được ghi PROCESSED rồi làm biến mất lỗi. L1 false positive có thể bị bác; không ép L2 sửa theo một kết luận sai của L1. Known-defect fixture bị preserve thay vì sửa vẫn là semantic test fail, dù preserve đúng cấu trúc.

Reconstructor phải biểu diễn được missing/extra sentence và split/merge thực tế, không chỉ thay một dòng nếu cách đó không thể sửa fixture. Audit khả năng hiện tại trước khi mở rộng. Áp edit bằng exact anchor/hash, phát hiện overlap và remap vị trí qua DRAFT→VI_L2→FINAL; stale line number không được dùng để bảo vệ nhầm câu. Test từ entrypoint coordinator thật, không chỉ truyền protected set bằng tay trong unit test.

L3 blind pass chỉ nhận nguồn được phép (RAW/GLOSSARY/VI_L2 theo contract); không nhận manual FINAL, gold labels hoặc kết luận L1/L2. Reconcile mới mở nguồn trước và PRONOUN theo visibility/status đã pin. Expected relations và QA findings có anchors; adversarial coverage phải thử bác nghĩa/vai/tác nhân/speaker/listener/số/phủ định/thiếu-thừa; regression phải xét mọi actual edit. Không thêm số lượng probe tối thiểu tùy tiện để giả lập độ sâu. Thay `NO_DEFECT` chung chung bằng record phạm vi đã kiểm, đối chứng nguồn, kết luận và action; validator kiểm cấu trúc, reviewer/evaluation kiểm nghĩa.

Đọc lại sau sửa là operation gắn hash của văn bản đã dựng. Nếu L3 sửa, app dựng candidate FINAL rồi thực hiện final-read/verification có ngân sách trước commit; sửa tiếp làm invalid lần đọc cũ và phải kiểm lại trong số vòng đã giới hạn. Hết cap giữ typed incomplete/recovery, không xuất FINAL đạt. Nếu không sửa, có thể tái dùng lần đọc đúng cùng hash nếu đủ coverage. Tương tự cho final-read VI_L2. Marker thứ tự do app tự điền không chứng minh operation đã xảy ra.

Giữ hai kết luận riêng: **structural execution valid** và **semantic evaluation đạt bộ kiểm đã định**. UI không gắn “không còn lỗi” chỉ từ COMMITTED hoặc các số 0. Không biến uncertainty hợp lệ thành CONTENT_BLOCKED; cũng không cho PRESERVE_DRAFT che known unresolved conflict trái contract.

### 11.5. Ma trận test và nghiệm thu đo được

R0 lập bộ đối chứng có RAW/profile proof; manual FINAL là nguồn gợi ý, không phải chuỗi ký tự bắt model chép. Reviewer kiểm các invariant nghĩa độc lập với model sửa. Chốt labels trước chạy; bất đồng phải phân xử và lưu lý do. Luna có thể đối chiếu anchors; Claude chịu kiểm định nghĩa khó, owner quyết định trường hợp còn mơ hồ cần tri thức ngoài nguồn.

| Bộ kiểm | Đầu vào và điều cần chứng minh | Tiêu chí |
|---|---|---|
| Reproduction 001 | RAW/DRAFT thật; lỗi `今回`, nhóm dungeon, profile nếu được R0 xác nhận | Mỗi lỗi xác nhận có L1 finding; L2 sửa đúng; L3 không xác nhận sạch khi còn lỗi |
| L1-only | Một lỗi mỗi fixture, rồi case tổng hợp >4 lỗi độc lập; không đưa manual FINAL | Phát hiện đủ lỗi đã chốt, có evidence; không sửa DRAFT; ledger/readback không mất mục |
| L2-only | Ledger gồm lỗi thật + một false positive có chủ ý | Sửa đúng lỗi thật; bác false positive có proof; mọi Error ID được xử lý, 0 edit vô căn cứ |
| L3-only | VI_L2 có lỗi cài trực tiếp, không qua L2; có trường hợp lỗi mới không có trong REPORT_L1 | Bắt/sửa lỗi cài; không phụ thuộc danh sách L1; no-edit PASS bị từ chối nếu còn lỗi đã biết |
| Clean/ambiguous controls | Bản đúng và bản có nhiều cách diễn đạt hợp lệ; intentional Japanese quote/term hợp lệ | Không sửa sai nghĩa; không ép xóa mọi ký tự Nhật; uncertainty giữ đúng contract |
| Giới hạn và fault injection | >4 findings, >300 candidates, nhiều occurrence, chunk boundary; malformed/missing/duplicate/forged refs; crash trước/sau commit | Đầy đủ hoặc typed incomplete; không silent truncation/partial PASS, không dùng lại stale artifact |
| Protected/final-read | Coordinator với span thật; QA edit gây regression; marker có nhưng không có verification đúng hash | Bắt protected regression, stale read và stale anchor; final-read thực trên output sau sửa |
| Holdout | Đoạn/chương chưa dùng chỉnh prompt, lỗi tự nhiên và seeded riêng; không có đáp án trong runtime | Chấm độc lập theo lớp lỗi; công khai miss/false repair/preserved; không chọn lượt đẹp nhất |

Phân loại seeded tối thiểu: sót từ, sai nghĩa/vai, thiếu/thừa câu, số/phủ định, tên/glossary, speaker/listener/xưng hô, mất protected span. Từng mutation phải vẫn tạo đầu vào hợp lệ và có một lỗi mục tiêu rõ; mutation tạo thêm lỗi ngoài ý muốn phải loại/chỉnh trước chạy. Đáp án, tên fixture gợi đáp án, manual FINAL và expected labels không được vào runtime prompt. App inventory/dedup phải deterministic; không hứa output model deterministic chỉ nhờ seed.

L3-only dùng harness/copy cô lập: dựng predecessor và hash nhất quán cho fixture trước khi chạy, dùng cùng projector/provider/parser/validator/reconstructor của production. Không chèn lỗi vào blob DB đã COMMITTED của pilot, không tắt identity gates để test chạy được, không mở một production bypass. Phải có một test end-to-end qua coordinator/UI thật bên cạnh các phép đo từng phase; fixture harness không thay thế nghiệm thu đường ứng dụng.

**Ngưỡng đề xuất chốt trước chạy:** toàn bộ structural/negative tests PASS; 100% lỗi xác nhận R0 và seeded bắt buộc được phát hiện đúng ở L1/L3 riêng, sửa đúng ở L2/L3; 0 lỗi nghĩa mới/sửa sai trên clean controls; 0 oracle leak; 0 mất ledger/proof sau reopen. Known defects không được đóng bằng preserve. Báo riêng TP/FP/FN, recall/precision theo loại, correct-repair rate, regression rate, counts và mẫu số; N/A khi mẫu số 0. Bộ nhỏ đạt 100% không có nghĩa chất lượng mọi sách đạt 100%.

R5 chốt holdout và ngưỡng trước nhìn kết quả; mặc định yêu cầu không còn lỗi nghiêm trọng đã adjudicate trên holdout, công khai mọi lỗi nhẹ. Không đạt thì sửa và đánh giá bằng holdout mới khi bộ cũ đã dùng để chỉnh. Dự kiến ba lượt độc lập cho subset lỗi trọng yếu để đo biến thiên, lưu mọi lượt và tiêu hao; chỉ chạy trong quyền/ngân sách thực tế, nếu chưa đủ lượt thì ghi thiếu evidence. Fake provider chỉ chứng minh plumbing/contract, không được tính vào semantic recall.

### 11.6. Phản biện bắt buộc trước kết luận

| Đề xuất dễ sai | Phản biện và cách xử lý |
|---|---|
| Chỉ tăng MAX_FINDINGS hoặc viết prompt mạnh hơn | Không sửa việc report bỏ entry, thiếu anchor/proof và gate chỉ đếm status. Phải sửa xuyên suốt provider→parser→report→store→L2/L3 |
| Gieo 10 lỗi rồi chạy cả chuỗi là đủ | L2 có thể sửa hết trước L3. Cần L1-only, L2-only, L3-only và end-to-end riêng |
| L1=0 finding/L3=0 edit chứng minh không làm việc | Metric L1 hiện hỏng; L3 có thể đúng khi bản sạch. Dùng bộ lỗi đã biết và clean controls |
| Bản sửa tay luôn là đáp án; phải khớp toàn văn | Có sửa phong cách/format và cách dịch tương đương. Chấm invariant nghĩa/profile, phân xử từng khác biệt |
| Candidate counts bằng nhau thì coverage đủ | Hai model có thể chia khác nhau hoặc cùng bỏ sót. Kiểm mapping theo raw inventory và proof; không equality số thô |
| App tự tính 0 nên app đã chứng minh ngữ nghĩa | Input trạng thái vẫn do model khai; protected set hiện rỗng. Tách kiểm máy khỏi kiểm nghĩa, test actual runtime |
| Tăng reasoning/đổi model sẽ chữa tận gốc | Có thể giúp semantic sensitivity nhưng không chữa contract hỏng. Giữ baseline route trước; chỉ thử model/reasoning khác như biến đo riêng sau sửa cấu trúc, có ngân sách, không tự đổi pins |
| Fake tests xanh và finalReadOrder đủ nghiệm thu | Fake tests không kiểm năng lực model; marker tĩnh không chứng minh đọc. Cần semantic evaluation thật và operation gắn output hash |
| Chunk nhỏ là giải pháp mặc định | Có thể mất ngữ cảnh speaker/quan hệ xuyên đoạn. Chỉ chunk khi sizing cần, overlap/ownership rõ và có boundary tests |
| USD 0.05/lần đủ cho mọi phép thử | Số cũ chủ yếu là L2/L3 bốn call. Full L1–L3, ledger lớn, chunk và final-read làm đổi chi phí. Tính lại từng call + tổng worst-case; không thừa kế budget cũ |

### 11.7. Bàn giao, quyền và định nghĩa hoàn tất

Phiên này chỉ lập kế hoạch/tài liệu, không triển khai code hay chạy model. Khi tiếp tục, Claude thực hiện công việc offline nằm trong phạm vi sửa đã được owner giao; không hỏi lại cho đọc mã, fixture, implementation và test thường lệ đã được phép. Chuẩn bị xong thay đổi reviewable, tests, build/archive và bảng execution cụ thể trước khi xin phần quyền live còn thiếu. Bảng đó nêu chapter/source hashes, schema/prompt/model revision, số call theo phase/chunk/verification, trần token/USD/time, stop/retry policy, cách giữ DB và event evidence. Không hỏi lại quyền đã được cấp rõ trong session tiếp theo.

Mọi APK dùng `scripts/build-and-save.ps1`, immutable payload hai nơi và exact-source ZIP. Không cài pilot, migrate pilot, tạo binding hay gọi provider theo quyền cũ đã tiêu thụ. Rerun sau schema đổi cần chain mới đúng identity trong project/binding được phép, giữ toàn bộ chain cũ. Không ghi đè file DRAFT/RAW/manual FINAL của owner. Không sửa acceptance để hợp thức hóa report cũ.

Mỗi nhóm bàn giao bằng: file/commit, lỗi gốc, thay đổi hành vi, test mới và kết quả, còn thiếu gì, đúng một next action. Update checklist hiện có chỉ với evidence thật. Khi kết thúc R6/R7, bảng issue→test→artifact phải đóng từng lỗi xác nhận; source/build/pilot cùng revision đã kiểm; chapter 001 chạy lại theo contract mới và đủ ba category được nghiệm thu; UI lưu/mở lại/export đúng text và hash. Trước đó chỉ được tuyên bố phần đã hoàn tất, không “đã sửa dứt điểm toàn bộ” dựa vào unit tests hoặc số phase COMMITTED.
