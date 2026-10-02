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
### G2 chi tiết — M4 RECONCILE chương 001 (nhóm active sau quyết định B)

Quyết định owner 2026-10-01: **B** — RAW event 7 (attempt `7a5e3428…`) là predecessor duy nhất cho M4; verdict formal giữ `RAW_NOT_ACCEPTED`. Quyết định này không cấp quyền cài APK, migrate DB hay gọi provider.

| Bước | Việc | Trạng thái | PASS / dừng |
|---|---|---|---|
| M4.a App | Compact wire RECONCILE + `executeReconcile` (`7763085e`); adapter fresh chỉ cho RECONCILE (`withFreshReconcileLifecyclePersistence`), `dispatchReconcile` yêu cầu lineage đúng trạng thái event 7 (`inspectReconcileLineage` = READY), `dispatchRaw` không đổi byte | **Xong** `4a603695` (app 268/268) | JVM tests + androidTest compile |
| M4.b Runner thiết bị | `EditorialP5EReconcileLiveInstrumentedTest`, opt-in `p5e_reconcile_live=YES`; pin APK/DB nhận qua argument và đối chiếu thiết bị; 1 call, 0 repair/retry, ≤USD 0.05, ≤120 s | **Xong** `1f8e7334` (compile) | Compile; không chạy nếu chưa có quyền |
| M4.c Host | Script gọn dùng thư viện supervisor (`-LibraryOnly`: xuất DB Before/After chỉ-đọc, đọc lại SQLite trên host, hash APK), một lần `am instrument`, ghi outcome. **Không** mở rộng supervisor RAW | **Xong** `a64ae460` (self-test 74/74, chưa chạy thiết bị) | Self-test bằng DB fixture; không chạm thiết bị khi test |
| M4.d Build | `build-and-save.ps1` production + test APK mới. Bản build từ HEAD có schema v25: mở app sẽ migrate DB pilot v24→v25 (thêm một bảng). Bản sao lưu trước: ảnh `database-snapshot-after` của event 7 (`9fa69f6b…`) + bản copy chỉ-đọc ngay trước cài | Cần quyền thiết bị | Hash APK/DB sau cài đọc lại khớp; migration chỉ thêm `editorial_phase_artifacts` |
| M4.e Event | Một RECONCILE call | Cần quyền provider riêng | Readback: attempt `L1_RECONCILE` COMMITTED, report `phase=L1_RECONCILE`, predecessor `7a5e3428…`, receipt atomic, lineage +1; UNKNOWN → không retry |
| M5–M7 | Restart/reopen đọc lại REPORT_L1 + receipt cùng hash; ghi P5.4/P5 exit | Sau M4.e | Theo bảng mốc mục 5 |

Phạm vi quyền sẽ xin một lần khi M4.a–c xong: cài production + test APK mới trên serial `15e84958` (kèm migrate v25 có sao lưu), một RECONCILE call trần USD 0.05 / 104,096 token / 120 s, đọc logcat `P5E_RAW` chỉ-đọc. Ước tính chi phí theo event 7: ~USD 0.003–0.006.

Tiến độ offline G3/G4 (2026-10-01): adapter OpenRouter cho wire L2 `63b288fe` (app 276/276); L3 re-audit mù + reconcile + 5 release numbers do app tính, FINAL + QA_RECEIPT một hàng `e8b21d26` (engine 244/244). Còn thiếu: L2_RAW_DISCOVERY đối chiếu candidate counts, adapter OpenRouter cho L3, coordinator app nối L1→L2→L3 với store (G5), UI/export.

## 10. Next action duy nhất

Owner đã duyệt phạm vi M4 (2026-10-01, `docs/P5E_M4_RECONCILE_WORK_REQUEST_20261001.md`). Bước 1 (M0) bị chặn ngoài: máy `15e84958` chưa kết nối ADB (2026-10-02). Next action: owner cắm máy và cho phép USB debugging, rồi tiếp tục yêu cầu từ bước 1. Offline đã thêm: adapter L3 `be105e16`, coordinator + export `ba8b4bbb`.

Lịch sử next action trước (giữ để tra cứu): event 7 commit RAW + REPORT_L1-phase-RAW + receipt (verifier formal `RAW_NOT_ACCEPTED` do collector); collector đã sửa ở `3057919b`. Offline entry-boundary đã đóng 2/2 vòng; nguyên nhân owner-window lịch sử vẫn UNRESOLVED.