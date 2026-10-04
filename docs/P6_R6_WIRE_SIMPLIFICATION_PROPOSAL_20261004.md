# Đề xuất — đơn giản hóa giao tiếp model và phá vòng "parser từ chối → thêm luật → gọi lại" (P6 R6, v4.18)

Ngày lập: 2026-10-04. Người lập: Claude (điều phối). Loại: đánh giá + phương án để owner quyết; **chưa** cấp quyền đổi kiến trúc production, gọi provider hay đụng pilot.
Baseline: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `96d3de26` (đồng bộ origin).

## 1. Trạng thái đã kiểm

| Hạng mục | Claim | Evidence | Kết luận |
|---|---|---|---|
| Test | engine 351, app 341, Python 33 | `git archive 96d3de26`, offline Gradle + `scripts/p6/test_*.py` | Đã chứng minh |
| Rò rỉ | không key/fingerprint/văn bản sách | soát `e268770c..96d3de26` | Đã chứng minh |
| Build script | chỉ thêm `-Offline` | diff 2 script: một switch + `--offline` | Không nới chính sách build |
| Checklist | không tick | 4 dòng thêm mục checkpoint + khác biệt whitespace | Đúng |
| Mã đang có / đã test / APK / đã cài | HEAD `96d3de26`; APK `4.18-p6.16`/code229 từ `d1cc1452` trên emulator; pilot vẫn code215 | execution record, BUILD_INFO | Phù hợp: `d9390733`/`96d3de26` chỉ thêm docs |
| Wire v3 sửa lỗi id | RAW được chấp nhận | V3: `L1_RAW_DISCOVERY` accepted (20,932 vào / 1,841 ra, USD 0.00744205) | **Đã chứng minh** — lần đầu một RAW ledger thật qua validator |
| RECONCILE | `L1_TEXT_REQUIRED` | 29,718 vào / 3,656 ra, `finish=stop`, USD 0.01181655 | Đã chứng minh mã; **trường cụ thể chưa biết** |
| Chi phí | G1 USD 0.03222845 / 1.00 | sổ chi tiêu hash-chain | Đúng; còn 0.96777155; 0 UNKNOWN |
| Chất lượng | — | L2/L3 chưa chạy | `SEMANTIC_EVAL`: **NOT_REACHED** |

## 2. Chẩn đoán `L1_TEXT_REQUIRED` — một họ lỗi, không phải một trường

| Phát hiện | Bằng chứng |
|---|---|
| Prompt bảo model dùng chuỗi rỗng khi không áp dụng | `OpenRouterEditorialP5PilotProvider.java:617` |
| Schema L1 chỉ có `maxLength`, không `minLength`/`minItems`/`pattern` cho id | `EditorialL1Ledger.java:550-555`; L2/L3 không gửi schema (chỉ `json_object`) |
| Parser bắt buộc không rỗng nhưng prompt không nói: `disposition.reasonCode` (298, parse **đầu tiên**), `speakerRecords[].speaker/listener/basis` (191–192), `protectedSpans[].reason` (207, prompt không nhắc trường này), `findings[].observation/expectedMeaning/rawQuote` (261, 275–276) | `EditorialL1Ledger.java` |
| Cùng họ ở L2/L3: `changes[].reason` bắt buộc nhưng prompt không nói (`EditorialChangeMapReconstructor.java:176`); độ dài `scope/contrast` ≤120 không nêu | audit Sonnet |
| Chẩn đoán mù: `safeMessage` chỉ cho `[A-Z0-9_:./-]{1,96}` nên mọi đường dẫn camelCase bị nuốt; L1 `keys/object` nhận path rồi bỏ | `EditorialL1Ledger.java:756-759`, `EditorialL2Execution.java:820-823` |
| Response không được giữ → không replay được | chính sách hiện tại của harness |

Thứ tự khả năng (suy luận, chưa chứng minh): (1) `disposition.reasonCode` rỗng khi CONTINUE; (2) `speakerRecords[].listener` rỗng cho lời kể/độc thoại; (3) `protectedSpans[].reason`; (4) `observation`/`expectedMeaning`; (5) `rawQuote` cho finding ADDITION/MISSING.

Đây là lần thứ ba liên tiếp lỗi hình thức (event 6: cú pháp token; G1#1–2: chép id có hash; G1#3: trường rỗng). Theo quy tắc hai vòng, phải sửa **thiết kế giao tiếp**, không chỉ vá trường đầu tiên.

## 3. Model đang phải làm bao nhiêu việc ghi sổ

| Việc model đang làm | Có cần model làm không | Đề xuất |
|---|---|---|
| Chép số dòng làm tham chiếu (`L<n>`, `D<n>`) | Có (rẻ, ổn định) | Giữ |
| Chép **nguyên dòng DRAFT** vào `changes[].before` ở L2_EDIT/L3 | Không — app biết dòng | Bỏ; app tự lấy `before` theo số dòng (cùng loại với bài học hash id) |
| Trích dẫn ngắn `rawQuote/draftQuote/viQuote` phải là chuỗi con | Có (là bằng chứng) | Giữ; app chuẩn hóa NFC + cắt khoảng trắng hai đầu trước khi so |
| Điền mọi khóa kể cả không áp dụng | Một phần | Danh sách MUST/MAY rõ ràng; trường không áp dụng cho phép rỗng thật sự (parser chấp nhận) hoặc giá trị chuẩn (`UNKNOWN`, `OK`) |
| Coverage phân hoạch theo dải | Có (owner cấm suy coverage từ lời khai) | Giữ |
| `reasonCode` theo charset riêng | Không cần với CONTINUE | Cho phép rỗng khi CONTINUE/PRESERVE_DRAFT; bắt buộc khi STOP |
| Hash, identity, đếm, diff, liên kết artifact | Không | Đã do app làm — giữ |

## 4. So sánh hình thức output L2/L3

| Tiêu chí | A. JSON change rows (hiện tại) | B. Khối thay thế dạng văn bản có thẻ + neo số dòng (đề xuất) | C. Trả toàn văn (như refine của luồng dịch) |
|---|---|---|---|
| Ví dụ | `{"line":12,"before":"…","after":"…","errorId":…}` | `<EDIT line="12" finding="E3">câu mới</EDIT>`, `<INSERT after="40" finding="E5">…</INSERT>`, `<DELETE line="7" finding="E2"/>` + danh sách giải quyết finding dạng thẻ | `<TEXT>…toàn chương…</TEXT>` |
| Mất/thừa câu | Không thể ngoài op khai báo | Không thể ngoài op khai báo | **Có thể** (luồng dịch chỉ cảnh báo, không chặn — `ChunkQa` ngưỡng 1/3–3×) |
| Sửa ngoài phạm vi | App chặn (diff = rows) | App chặn | Phải tự căn hàng + ánh xạ diff→finding; model hay chuẩn hóa dấu câu hàng loạt → nhiều diff không có finding |
| Protected spans | Thực thi chính xác | Thực thi chính xác | Chỉ kiểm sau; phải hoàn nguyên từng dòng |
| Truy vết finding→thay đổi | Trực tiếp | Trực tiếp | Gián tiếp, dễ mơ hồ |
| Token ra (chương ~26–34 KB) | Nhỏ (theo số sửa) | Nhỏ, ít hơn A (không chép `before`, không escape JSON) | ~10–16k/call; chương 010 sát trần 16,384 → rủi ro cắt |
| Chi phí ước tính L2_EDIT | ~0.01–0.02 | ~0.01 | ~0.02–0.03 (×2 nếu L3 cũng trả toàn văn) |
| Độ ổn định hình thức | Thấp (JSON lồng, escape ngoặc kép tiếng Việt/thoại, chép nguyên dòng) | Cao hơn (văn bản thuần trong thẻ như luồng dịch đã chạy cả sách) | Cao về hình thức, thấp về kiểm soát |
| Tái sử dụng luồng dịch | Không | `PromptBuilder.stripThinking/stripCodeFence`, trích thẻ, `ResponseValidator` (finish, cap, refusal, echo, loop), `RetryPolicy` phân loại, lưu `response_content` trước khi kiểm | Như B + prompt refine |

**Khuyến nghị:** giữ **change rows** nhưng chuyển L2_EDIT/L3_RECONCILE sang **B** (văn bản có thẻ, neo số dòng, app tự lấy `before`), giữ phần "giải quyết finding" dạng thẻ đơn giản. Không chọn C làm mặc định vì không chặn được mất câu/sửa lan và có rủi ro cắt ở chương dài. Không đổi trước khi có số đo: chạy thử A/B/C có kiểm soát (gói S6) sau khi L1 thông.

L1 vẫn là JSON có schema strict (đã chạy được RAW); chỉ sửa tính nhất quán, không đổi hình thức.

## 5. Gói việc đề xuất (để owner duyệt triển khai)

| Gói | Nguyên nhân xử lý | File chính | Đầu ra / test | PASS | Dừng |
|---|---|---|---|---|---|
| **S1** chẩn đoán có đường dẫn | Lỗi mù | `EditorialL1Ledger` (`str/id/enumOf/intOf/stringList/object/keys/bad`, vòng lặp có chỉ số), `EditorialUnitReference`, `EditorialL3Ledger`, `EditorialL2Execution` (`keys/object/text/bool/line/safeMessage`), `EditorialL2Findings`, `EditorialFinalRead` | Ngoại lệ có kiểu `WireViolation(code, path)` với path chỉ từ hằng + chỉ số (vd `findings.2.expectedMeaning`); `safeMessage` trả `CODE:path`; không bao giờ đưa tên khóa lạ của model vào path | JVM: mỗi luật bắt buộc có test trả đúng `CODE:path`; khóa lạ → không lộ tên | — |
| **S2** giữ response để replay | Không replay được | provider adapters L1/L2/L3 + runner fixture | Ghi **bytes nội dung response** (không header, không key) vào thư mục riêng trước khi validate: fixture → `files/p6-fixture-results/<run>/<fixture>/responses/NNN-PHASE.json` rồi kéo về `D:\P5E-private`; pilot/sản phẩm: **tắt mặc định** (cần owner duyệt riêng). Công cụ replay JVM: đọc file response + inventory/fixture, chạy đúng parser production, in `CODE:path` | Replay lại response giả lập tái hiện đúng mã; không ghi key/header | — |
| **S3** một nguồn sự thật cho trường | Prompt–schema–parser lệch | `EditorialL1Ledger.jsonSchema`, prompt L1 (`:617`, `:557-567`), prompt L2/L3 | Bảng `FieldSpec` (path, MUST/MAY/CONDITIONAL, min/max, pattern) trong engine; schema L1 sinh từ bảng (thêm `minLength`, `minItems`, `pattern` id); prompt liệt kê MUST/MAY từ bảng; test nhất quán: mọi trường parser bắt không rỗng phải là MUST trong bảng, có trong prompt và có `minLength:1` ở schema | Test nhất quán xanh cho cả 8 phase | Không nới anchor/id/coverage |
| **S3b** quy tắc rỗng hợp lý | Trường không áp dụng | `EditorialL1Ledger` | `disposition.reasonCode` cho phép rỗng khi CONTINUE/PRESERVE_DRAFT; `speakerRecords[].listener` cho phép `UNKNOWN`/rỗng; giữ `reason` của protectedSpan bắt buộc và nói rõ trong prompt | JVM | — |
| **S4** bỏ chép nguyên dòng ở L2/L3 | Cùng họ "chép" | `EditorialChangeMapReconstructor`, wire L2_EDIT/L3_RECONCILE, adapters | `before` không còn bắt buộc; app lấy theo số dòng; nếu model gửi `before` thì chỉ kiểm là chuỗi con (sau NFC/trim), lệch → cảnh báo, không từ chối | JVM + fake chain 14/14 | — |
| **S5** live tiếp G1 | Có chẩn đoán đầy đủ | — | Chạy tiếp G1 (L1-only) trong phần trần còn lại, `fx-a03` trước, cùng quy tắc dừng nhóm | Mỗi từ chối có `CODE:path` + response replay được | Từ chối: sửa offline bằng replay, không gọi lại ngay |
| **S6** thử A/B/C cho L2 (sau khi có REPORT_L1 thật) | Chọn hình thức có số đo | runner fixture | Cùng 3 fixture (`fx-a04`, `fx-a11`, `fx-a02`) và cùng REPORT_L1 thật từ G1; 3 biến thể × 3 fixture × 2 lượt = 18 call L2_EDIT; đo: tỷ lệ qua cấu trúc, lỗi gieo sửa đúng (scorer đóng băng), collateral, mất/thừa câu, token/USD | Bảng so sánh; quyết định giữ B hay đổi | Ngân sách riêng ≤ USD 0.40 (cần owner duyệt) |

S1–S4 là offline, có thể hoàn nguyên (một nhánh commit theo gói). S3/S4 đổi wire → tăng `contractRevision` (attempt v3 không bị tái dùng). Không đổi branch/release/checklist.

## 6. Phản biện

- *"Chỉ cần vá `reasonCode`"*: có thể đúng cho lần này nhưng còn ≥5 trường cùng họ; không có path thì lần sau lại mù.
- *"Trả toàn văn sẽ hết lỗi hình thức"*: đổi lỗi hình thức lấy lỗi khó thấy (mất câu, sửa lan); luồng dịch hiện không có kiểm tra bản sửa so với bản nháp.
- *"Lưu response là rủi ro dữ liệu"*: response là văn bản sinh từ sách, cùng loại với fixture đã nằm trong `D:\P5E-private`; không có key/header. Với pilot vẫn tắt mặc định.
- *"Bỏ JSON schema L1"*: không — schema strict đã giúp RAW qua; chỉ cần nó khớp parser.
- *Rủi ro `require_parameters`*: thêm `minLength/minItems` vào schema có thể làm router loại endpoint không hỗ trợ; S3 phải kiểm bằng một preflight 0-call không gửi (dựng body) và nêu rủi ro trước live.

## 7. Chi phí dự kiến

S1–S4: 0 USD (offline). S5: phần còn lại của G1 (≤ USD 0.968, đã duyệt). S6: ≤ USD 0.40, cần duyệt riêng.

## 8. Một bước tiếp theo

Owner duyệt **S1–S4** (offline, thay đổi wire và parser trong production, có thể hoàn nguyên) và chính sách S2 (giữ response cho fixture run ngoài Git; pilot tắt). Sau đó giao Codex S1–S4 rồi S5.
