# v4.23 — Yêu cầu làm việc: resume CRLF, job dở / Current job, Jobs theo tổ hợp có token-chi phí, đồng bộ Library, gọn màn Dịch, cuộn khung lồng

Trạng thái: ĐÃ PHẢN BIỆN (agent phản biện đọc mã tại `846b7b11`, 2026-10-19; các khẳng định chính đã kiểm lại).

- Nhánh: `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`.
- Mốc: `846b7b11`; điện thoại đang chạy `4.23-dev.8` / code 283.
- Ranh giới "không động vào Biên tập" như `docs/V423_TRANSLATE_LIBRARY_WORK_REQUEST.md` mục 2. Được **đọc** mã Editorial để kiểm ảnh hưởng, không sửa.

**Quyết định sản phẩm phục vụ:**
- Owner dịch nhiều chương theo tổ hợp mà không bị kẹt: resume được, không có thẻ cũ bám mãi.
- Owner xem được kết quả theo tổ hợp, có token và chi phí.
- Library và màn Dịch gọn, nhất quán.

## 1. Vấn đề và nguyên nhân (file:dòng tại `846b7b11`, đường dẫn trong `app/src/main/java/com/ml/tblandroidtxt/`)

| # | Owner / review thấy | Nguyên nhân đã chứng minh | Còn chưa biết |
|---|---|---|---|
| A | (review) Resume job tạo từ "Tệp TXT" bị chặn khi tệp có xuống dòng Windows (CRLF) | • Kế hoạch dịch lưu hash của văn bản đã chuẩn hóa: `sha256(normalizeSource(text))` (`PreparationCoordinator:139,144`).<br>• Resume lại băm văn bản thô: `TranslatorService:404` (`continueExistingJob`), `:513–515` (`resumeJobById`).<br>• Kết quả: job bị đánh `INPUT_CONFLICT`, rồi bị loại khỏi `getIncompleteJobs` (`TranslationRepository:669`).<br>• Lỗi có từ `v4.21.0`. BOM không gây lỗi: `FileUtil:134,148–157` bỏ BOM ở mọi lần đọc. RAW nội bộ không bị ảnh hưởng. Đường retry (`:587–618`) không kiểm hash | Đã có job nào của owner bị `INPUT_CONFLICT` oan chưa |
| B | Thẻ "Job đang dở" bám mãi, không bỏ được | • Thẻ (`MainActivity:248–278`) không có thao tác bỏ.<br>• Job dở cũ vẫn được chọn kể cả khi owner đã dịch lại cùng tệp bằng job mới.<br>• Tự phục hồi lúc khởi động (`TranslationRepository.recoverInterruptedChunks:175`, chạy mỗi lần service khởi động `TranslatorService:215`) đặt **mọi** job có chunk `DELIVERY_UNKNOWN` thành `NEEDS_REVIEW` và đẩy `updated_at` lên, kể cả job đã hủy. Nếu chỉ thêm trạng thái "bỏ" thì job sẽ bị kéo lại | Job `020_RAW…` của owner có job mới cùng hash đã xong hay chưa |
| C | Tab Jobs "Current job" hiện job xong (`0023-023 苦い後味.txt`), không xóa được | • `JobsMainPageFactory.current()` (`:53–57`) chỉ loại `done`/`cancelled` viết thường. Job xong hiện ghi `COMPLETED`/`COMPLETED_WITH_WARNINGS` (`OutputIntegrityAudit:29`), nên gần như job vừa cập nhật nào cũng thành "current".<br>• Số liệu trên thẻ luôn đọc `RuntimeStateStore` (`:67–76`) chứ không đọc từ job.<br>• Danh sách job chỉ làm mới khi bấm Refresh: khi nhận broadcast done/error, `MainActivity:231–233` không gọi `refreshJobCacheAsync`, nên dữ liệu hiện ra cũ.<br>• Nút xóa job chỉ có trong công cụ nhà phát triển (`JobsPageFactory.confirmDeleteJob`) | — |
| D | Recent jobs cần thống kê theo tổ hợp; thiếu tổng token và chi phí | • `recentRow` chỉ in tên, trạng thái, chunk, thời gian.<br>• Token/chi phí đầy đủ nằm ở `chunk_metrics` (`usage_cost`, `actual_input_tokens`, `output_tokens`, `cached_input_tokens`, `cost_status`). `getMetricsSummary` (`:299–305`) đã cộng theo job. `chunk_attempts.actual_cost` chỉ có khi provider tự báo giá.<br>• `deleteJob` xóa luôn `chunk_metrics` (`:599`).<br>• Gom theo `start_session_id` **không đủ tin cậy**: `continueExistingJob` giữ session cũ; job cũ không có id; `setName` chỉ có ở lượt chạy tổ hợp (`ChapterSession`) | — |
| E | RAW có "19 RAW", Glossaries/Pronouns không đếm; thông báo nạp/xóa/hoàn tác không giống nhau | • Đếm: chỉ `RawPageFactory` có.<br>• Xóa từng thẻ:<br>&nbsp;&nbsp;– RAW qua `bulkUi.deleteIds`, có hoàn tác (`RawPageFactory:107`);<br>&nbsp;&nbsp;– Glossary "Del" dùng hộp thoại + toast (`GlossaryPageFactory:142–149`);<br>&nbsp;&nbsp;– Pronoun xóa **không có phản hồi** (`PronounPageFactory:41`).<br>• Nhập Glossary/Pronoun báo qua `showResult` (thẻ ở màn Dịch, gắn "✅" cả khi lỗi, `MainActivity:3206`).<br>• Hoàn tác hàng loạt: `LibraryBulkUi`, cửa sổ 10 giây (`:22`) | — |
| F | Chế độ tổ hợp vẫn hiện thẻ Glossaries/Pronoun, rối; "Không sử dụng" hiện hai lần | • Thẻ luôn được dựng.<br>• Ở chế độ tổ hợp, Glossary/Pronoun của chương thay hẳn bản chung (`ChapterBinding:44–55`); `startReadinessError` trả về trước bước kiểm khóa (`MainActivity:2160`). Chỉ có các `setText` đã kiểm null (`:2747–2748`) đọc các view này, nên ẩn là an toàn.<br>• Đổi chế độ không dựng lại trang, nên phải ẩn trong `TranslateSourceFactory.render()` (`:182`).<br>• Chữ lặp: `TranslationConfigState.displayText` (`:29–31`) dùng "Không sử dụng" làm tên, rồi `statusLabel()` thêm "Không sử dụng" một lần nữa | — |
| G | Khung preview và Activity Log không vuốt được trên điện thoại; lăn chuột thì được | • `ScrollView` con có `setNestedScrollingEnabled(false)` (`TranslatePageFactory:378,436`), nên `ScrollView` của trang chặn thao tác vuốt.<br>• Lăn chuột gửi `ACTION_SCROLL` thẳng tới view dưới con trỏ, nên vẫn cuộn được.<br>• Cùng lỗi ở "Runtime log tail" trong công cụ nhà phát triển (`JobsPageFactory:106`) | Hành vi trên OPPO / Android 15 sau khi sửa |
| H | (review) Thẻ job "không rõ đã gửi" vẫn để "Tiếp tục dịch" là nút chính | Nút **đã bị tắt** (`MainActivity:268`). Chỉ cần sửa nhãn và độ nổi bật | — |
| I | (review) Emulator còn dữ liệu thử sau QA | Test resume tạo job/input và không dọn hết | — |

## 2. Thiết kế đã chốt

**S — Một chỗ phân loại trạng thái job** (`JobStatus.classify(String)`, không phân biệt hoa thường).
- Các lớp: `ACTIVE` (RUNNING/running/READY), `CONTINUABLE` (paused/PAUSED/error/RETRYABLE_ERROR/FAILED_INTEGRITY_CHECK chỉ thiếu chunk), `NEEDS_REVIEW`, `CONFLICT` (INPUT_CONFLICT), `FINISHED` (done/COMPLETED/COMPLETED_WITH_WARNINGS), `CANCELLED`, `DISMISSED` (mới).
- Mọi danh sách SQL dùng chung danh sách từ lớp này:
  - `TranslationRepository:375,397,669`;
  - `LibraryRawStore:173,183`;
  - `JobsPageFactory:386,508–517`;
  - `JobsMainPageFactory.current()`;
  - thẻ job dở.
- Test bảng phủ mọi giá trị thô đã thấy.

**A — Hash resume nhất quán.**
- Helper `InputIdentity.matches(storedHash, text)`: đúng khi `storedHash` bằng `sha256(text)` **hoặc** bằng `sha256(normalizeSource(text))`. Dùng ở cả hai đường resume. Log ghi nhánh nào khớp.
- Không đổi cách ghi hash.
- Sửa dữ liệu một lần, chạy nền, có cờ trong prefs:
  - chỉ với job `INPUT_CONFLICT` có `prepared_batch_id<>''` và tệp còn đọc được;
  - nếu hash đã chuẩn hóa khớp thì trả về `paused` và ghi log.
  - Không thể làm sống lại job có tệp đã đổi thật: khớp hash chuẩn hóa nghĩa là nội dung giống hệt, chỉ khác xuống dòng.
- Không đọc tệp trên luồng UI.

**G — Cuộn khung lồng.**
- Bật `setNestedScrollingEnabled(true)` cho các `ScrollView` con: preview, Activity Log, "Runtime log tail". Đây là cơ chế có sẵn từ API 21; trang tự cuộn tiếp khi khung con chạm mép.
- Nếu trên emulator vẫn bị chặn: dùng helper dự phòng `requestDisallowInterceptTouchEvent` có trả quyền ở mép.
- Kiểm nhấn giữ để chọn chữ ở preview vẫn hoạt động.

**B + C + H — Job dở và Current job.**
- Thẻ "Job đang dở":
  - thêm nút **"Bỏ job này"**, có xác nhận ("Job sẽ không hiện ở đây; chunk đã dịch, token và chi phí vẫn giữ trong Jobs"). Nút đặt trạng thái `dismissed`, không xóa dữ liệu.
  - `recoverInterruptedChunks:175` chỉ chạm job thuộc lớp `ACTIVE` / `CONTINUABLE` / `NEEDS_REVIEW`; không chạm `FINISHED` / `CANCELLED` / `DISMISSED`. Có test.
  - **Không tự ghi trạng thái "bị thay thế".** Lúc truy vấn, ẩn job dở khi đã có job mới hơn cùng `input_hash` ở lớp `FINISHED`. Không ghi gì nên đảo lại được. Không dùng chữ `SUPERSEDED`, vì chữ này đã dùng cho chunk bị chia.
  - Khi có nhiều job dở: "và n job dở khác · Xem tất cả" (mở Jobs, lọc "Đang dở").
  - Truy vấn của thẻ chạy nền, không chạy trên luồng UI (`MainActivity:252–253` hiện chạy trên UI).
  - **H:** với job có chunk "không rõ đã gửi", nút chính đổi thành "Xem trong Jobs" và "Tiếp tục dịch" là nút phụ đã tắt kèm lý do.
- "Current job" = job `ACTIVE` (service đang chạy), nếu không có thì job `CONTINUABLE` / `NEEDS_REVIEW` mới nhất, theo cùng quy tắc ẩn như thẻ. Không bao giờ là job đã xong.
  - Số liệu đọc từ job, kể cả token/chi phí từ `getMetricsSummary`. `RuntimeStateStore` chỉ dùng khi chính job đó đang chạy.
  - Không có job như vậy: "Không có job đang chạy hay đang dở."
- Gọi `refreshJobCacheAsync(false)` khi nhận broadcast done/error/cancelled/paused.
- Tab Jobs: nhấn giữ một dòng → "Bỏ" (dismissed) / "Xóa" (xác nhận; cảnh báo mất lịch sử token/chi phí, và tổ hợp Biên tập dùng job này làm DRAFT sẽ không đọc được nữa). Mặc định nên dùng "Bỏ".

**F — Màn Dịch theo chế độ.**
- Trong `TranslateSourceFactory.render()`: ở chế độ Tổ hợp ẩn **hai hàng** Glossaries và Pronoun (không ẩn cả mục, vì Instruction YAML cùng mục). Thay bằng một dòng "Glossary/Pronoun: theo từng chương của tổ hợp (xem danh sách chương ở trên)".
- Đổi chế độ thì ẩn/hiện ngay, không cần dựng lại trang.
- Sửa `TranslationConfigState.displayText`: khi không chọn chỉ hiện "Không sử dụng" một lần.

**E — Library đồng bộ (tối thiểu).**
- Dòng đầu chung cho RAW / Glossaries / Pronouns / Tổ hợp: "N mục" + nút "Chọn". Ô tìm cho Glossaries/Pronouns để lần sau.
- Xóa từng thẻ ở Glossary ("Del") và Pronoun đi qua `bulkUi.deleteIds` như RAW: cùng hộp xác nhận, cùng thanh hoàn tác 10 giây.
- Nhập Glossary / Pronoun / RAW: cùng một thanh thông báo trong trang Library, dùng lại khung `LibraryBulkUi.undoBar`: "Đã thêm n · bỏ qua m (trùng/lỗi) · Xem chi tiết". Không dùng `showResult` cho thao tác Library nữa.
- `showResult` không gắn "✅" vào tiêu đề lỗi.

**D — Jobs theo lượt chạy và tổ hợp, có token/chi phí.**
- **D1 (làm trong đợt này):**
  - mỗi dòng job thêm "token vào/ra (cached) · USD …", lấy từ `chunk_metrics`;
  - đầu tab Jobs có tổng "Hôm nay / 7 ngày": số chương xong, token, chi phí. Tính theo `chunk_metrics.created_at`, lấy `SUM(usage_cost)` ở các dòng `PROVIDER_REPORTED`/`USAGE_DERIVED`; số dòng `UNKNOWN*` ghi riêng ("n lượt chưa rõ giá"). Retry là tiền thật đã tiêu, cộng đúng một lần mỗi lượt gọi.
- **D2 (làm trong đợt này, sau D1):** gom theo lượt chạy tổ hợp.
  - Ảnh chụp `translate_sessions/<id>.json` ghi thêm id các job được tiếp tục (`resumedJobs`). Không đổi schema.
  - Thẻ lượt tổ hợp gồm tên tổ hợp, thời gian, "x/y chương xong · z lỗi", tổng token, tổng USD, thời lượng.
  - Bấm vào mở danh sách chương: trạng thái, chunk, token, USD, "Mở kết quả".
  - Job cũ không có session: mỗi job một thẻ, ghi "(lượt cũ)".
  - Nếu một chương được tiếp tục trong lượt khác: hiện dưới lượt mới, ghi "tiếp tục từ lượt …".
- Chỉ đọc; không đổi schema.

**I — Dọn dữ liệu thử.** `V423ResumeInstrumentedTest` và test mới tự xóa job/input của mình. QA so hash DB/prefs của emulator trước/sau và báo lệch.

## 3. Thứ tự, kiểm thử, điểm dừng

**Thứ tự:** S → A → G → B+C+H (cùng làm mới cache) → F → E → D1 → D2 → I.

**Test host:**
- `JobStatus`: bảng mọi giá trị thô.
- `InputIdentity`: CRLF, BOM+CRLF, CR; nội dung sửa vẫn conflict.
- Sửa dữ liệu một lần: lặp lại không đổi gì, bỏ qua job có tệp đã đổi.
- `recoverInterruptedChunks` không chạm job dismissed/finished/cancelled.
- Quy tắc ẩn job dở khi có job xong mới hơn cùng hash.
- `current()` không bao giờ trả job đã xong.
- Tổng chi phí với `cost_status` lẫn lộn và có retry.
- `displayText` không lặp chữ.

**Test thiết bị** (`emulator-5554`, provider giả):
- Tệp CRLF: Start → force-stop → Resume, cả hai đường.
- Vuốt trong preview/log: `scrollY` khung tăng; ở mép thì trang cuộn.
- Đổi chế độ Tệp TXT ↔ Tổ hợp thì các hàng ẩn/hiện.
- "Bỏ job" → thẻ biến mất và không quay lại sau khi khởi động lại service.
- Xóa Pronoun có thanh hoàn tác.
- Tab Jobs: tổng token/chi phí; lượt tổ hợp gom đúng.
- Ảnh (xem từng ảnh):
  1. thẻ job dở có "Bỏ job này";
  2. Current job trống / đúng;
  3. màn Dịch chế độ Tổ hợp đã gọn;
  4. Library ba tab có "N mục";
  5. thanh thông báo nhập / xóa;
  6. Jobs với thẻ lượt tổ hợp và tổng token/USD;
  7. preview đang cuộn.

**Điểm dừng:**
- Hồi quy archive sạch.
- Build `4.23-dev.9` (code > 283).
- QA đủ nhóm + test mới.
- **Dừng**, gửi owner APK, SHA-256, ảnh và đúng một câu hỏi duyệt cài. Trước khi cài, sao lưu `run-as` từ `4.23-dev.8`.

**Quyền:** không gọi provider; không chạy test trên điện thoại; không sửa `Editorial*`; không tag/merge; push sau khi quét.

**Phiếu thử cho owner sau dev.9:**

| # | Owner làm | Đạt khi |
|---|---|---|
| G1 | Bấm "Bỏ job này" trên thẻ job dở | thẻ biến mất, không quay lại sau khi mở lại app |
| G2 | Tab Jobs | Current job không hiện job đã xong; thấy tổng token/USD; lượt tổ hợp gom thành một thẻ |
| G3 | Chọn nguồn Tổ hợp ở màn Dịch | không còn thẻ Glossaries/Pronoun |
| G4 | Vuốt trong preview và Activity Log | cuộn được bằng tay |
| G5 | Nhập/xóa/hoàn tác ở RAW, Glossaries, Pronouns | cùng kiểu thông báo; có "N mục" |
| G6 | Resume một tệp TXT tạo trên Windows | chạy tiếp được |

Mục nào owner không thử thì ghi NOT_MEASURED.

## 4. Rủi ro còn lại

- Dừng bằng Stop đặt `cancelled` (`TranslatorService:153`), nên job đó rời khỏi thẻ và câu hỏi resume. Owner có thể hiểu là "mất". Đợt này chỉ ghi chú, không đổi.
- Tệp > 1 MB được giải mã UTF-8 không chặt (`FileUtil:113`). Tệp Shift_JIS lớn có thể ra chữ lỗi nhưng hash vẫn nhất quán. Ngoài phạm vi đợt này.
- Cảm ứng trên OPPO (G) chỉ xác nhận được khi owner thử.
