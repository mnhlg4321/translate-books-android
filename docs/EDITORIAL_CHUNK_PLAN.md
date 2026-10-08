# Biên tập theo chunk — kế hoạch sống (từ 2026-10-09)

Owner (chat 2026-10-09) yêu cầu: tiếp quản việc dở; xác định chính xác nguyên nhân lỗi; loại bỏ ngay các báo cáo/receipt không cần thiết; thiết kế lại Biên tập theo chunk, một lần gọi cho mỗi chunk như luồng Dịch, mỗi chunk gửi cặp RAW–DRAFT khớp nhau và glossary/pronoun ứng viên của chính chunk đó; không test nhiều chương một lúc gây tốn tiền.

Đây là **tài liệu duy nhất** cho hướng chunk. Mỗi lượt làm việc cập nhật tại chỗ (mục 6 trạng thái, mục 7 nhật ký ngắn); không tạo thêm file review/request/execution riêng.

## 1. Nguyên nhân (đo trên 5 chương dev 004–008, so với 28 bản FINAL của owner)

| Cách chạy | Chỗ owner sửa mà app làm đúng/gần đúng | Chi phí dev | Ghi chú |
|---|---:|---:|---|
| luna, 1 lượt toàn chương (E-luna-b) | 6.0% | USD 0.045 | sửa thừa 45 dòng |
| luna, pack v5 3 lượt + báo cáo (V5-luna) | 13.8% | USD 0.168 | 1/5 chương bị chặn ở QA receipt (`CONTENT_UNACCOUNTED_CHANGE`) |
| Sol, 1 lượt toàn chương (E-strong) | 50.3% | USD 0.443 | sửa thừa 165 dòng |

Kết luận đã chứng minh bằng số:

1. **Model nhỏ xử lý cả chương trong một lần thì bỏ sót gần hết lỗi.** Cùng prompt, đổi luna → Sol tăng từ 6% lên 50%. Luật prompt không phải đòn bẩy (Q1: đổi luật trên luna không tăng).
2. **Báo cáo/ledger/receipt của pack v5 làm tăng chi phí ~4 lần và tạo điểm chặn**, chỉ đổi lấy +8 điểm phần trăm. Ba lần chạy v5 gặp ba loại chặn do thủ tục (schema glossary, danh tính file, receipt QA), không lần nào do nội dung.
3. Giả thuyết cần kiểm (chưa chứng minh): **chunk nhỏ như luồng Dịch giúp luna tập trung**, gần mức Sol với chi phí của luna.

## 2. Loại bỏ ngay

| Thứ | Quyết định |
|---|---|
| Báo cáo/ledger/receipt do model viết (REPORT_L1, CHANGE_MAP, QA_RECEIPT, Stop Receipt, NOTES) | Bỏ khỏi luồng sản phẩm. Model chỉ trả bản sửa của chunk (`<EDITED>`), hoặc `<WRONG_PAIR>` khi hai bản rõ ràng khác chương |
| Đường V5 3 lượt | Đóng băng: giữ mã và evidence, không chạy, không có trong UI |
| Chế độ Kỹ (lượt Kiểm + Kiểm lại) | Ẩn khỏi UI; N5 cho thấy không hơn Nhanh mà tốn hơn |
| Receipt/biên bản cho người dùng | Không có. App chỉ giữ bản ghi nội bộ tối thiểu: trạng thái từng chunk, hash nguồn, chi phí |
| Tài liệu quy trình | Chỉ tài liệu này cho hướng chunk; không thêm file review/request/execution riêng mỗi bước |

## 3. Thiết kế

Luồng giống Dịch:

```text
Chọn RAW + DRAFT (+ Glossary, Pronoun)
 → cắt chunk: dòng không rỗng i của RAW ↔ dòng i của DRAFT; gom dòng tới ~N ký tự RAW (theo cài đặt chunk như luồng Dịch)
 → mỗi chunk một lần gọi: Quality Core (không có kênh ghi chú) + glossary/pronoun ứng viên của chính chunk + ngữ cảnh RAW/DRAFT trước–sau (chỉ đọc) + RAW chunk + DRAFT chunk
 → model trả <EDITED>bản sửa của đúng DRAFT chunk</EDITED>
 → app kiểm cấu trúc chunk (mất/gấp chữ, ký hiệu, rò meta), ghép lại theo thứ tự, lưu, xuất TXT
```

Thành phần đã có và tái dùng:

- `EditorialPairRunService`, `PairMaps.fromJobRows`, `PairPromptBuilder` (CP-IMPL-2: không NOTES), `ReferenceProjector` (glossary/pronoun theo phạm vi chunk), `StructuralGate`, `ChunkMerge`, store v27 — đã có test và chạy trên emulator ở CP-IMPL-1.
- Nguồn **job Dịch**: mỗi dòng của job là một cặp chunk (đã có).
- Nguồn **hai file** (mới, `EditorialPairSource.fromAlignedFiles`, commit cùng tài liệu này): khi số dòng không rỗng bằng nhau thì ghép theo dòng và gom theo ngân sách ký tự; các dòng ghép lại đúng nguyên file. Số dòng lệch → `LINE_COUNT_MISMATCH`, không tự ghép.

### 3.1 Căn dòng RAW ↔ DRAFT trước khi cắt chunk (đo 2026-10-09)

Đo trên 28 chương của owner (chỉ đếm, không đưa văn bản vào Git):

- 13/28 chương lệch số dòng ±1–4. Lệch luôn **cục bộ**, 1–4 điểm mỗi chương: DRAFT tách một dòng RAW thành 2 (thường "câu thoại + câu kể" hoặc câu dài), gộp 2 dòng RAW bị ngắt giữa câu, hoặc thiếu/thêm một dòng.
- **Số dòng bằng nhau không bảo đảm khớp 1–1:** chương 016 và 027 có một chỗ tách và một chỗ gộp bù trừ nhau (016: RAW 112 bị DRAFT tách, RAW 189–190 bị DRAFT gộp) nên RAW và DRAFT lệch một dòng suốt 77 dòng (016) và 228 dòng (027). Ghép 1–1 thuần (`fromAlignedFiles`, `1f360c19`) sẽ ghép sai ở hai chương này.

Phương án (prototype: `scripts/chunk/align_lines_reference.py`):

1. **Căn dòng tự động cho mọi chương**, kiểu Gale–Church: quy hoạch động với các bước 1–1, 1–2, 2–1 (và 1–0, 0–1 phạt nặng). Chi phí mỗi cặp gồm:
   - tỷ lệ độ dài so với tỷ lệ VI/JP của cả chương;
   - ký hiệu đầu/cuối dòng (「」『』【】〝〟◇◆…);
   - **neo glossary**: tên/thuật ngữ nguồn ở dòng RAW phải có dạng đích ở dòng DRAFT; cùng với chữ số.
2. **Chỉ cắt chunk ở ranh giới an toàn:** giữa hai cặp 1–1 liền nhau, khi đã đủ ngân sách ký tự. Nhóm tách/gộp luôn nằm trọn trong một chunk. Trong chunk, model được phép tách/gộp dòng theo RAW, vì app ghép theo chunk chứ không theo dòng.
3. **Ngữ cảnh chồng lấn** (đã có): mỗi chunk kèm RAW/DRAFT trước–sau chỉ để đọc, nên nếu cắt lệch một dòng thì model vẫn thấy câu nguồn.
4. **Kiểm từng chunk trước khi gửi**, không tốn API: tỷ lệ độ dài, số câu thoại 「」, neo glossary/số phải khớp. Chunk nghi ngờ → gộp với chunk kề; vẫn nghi → báo người dùng và chạy chương đó như một chunk toàn chương.

Kết quả prototype:

- 8 chương lệch số dòng có đáp án dựng từ FINAL: **42/42 điểm cắt đúng**, mọi điểm lệch tìm đúng chỗ.
- 016/027: tìm ra đúng cặp tách/gộp bù trừ (đã đọc tay xác nhận).
- 4 chương lệch không có đáp án (010, 015, 019, 023): điểm lệch tìm được hợp lý khi đọc mẫu (vd 023 dòng 142–145: DRAFT tách "thoại + kể").
- Mức khớp neo glossary trên các cặp 1–1 có neo: 79–100% theo chương.
- Giới hạn: đáp án dựng từ FINAL chỉ đúng ở chương mà FINAL theo cấu trúc RAW; với 016/027, FINAL theo cấu trúc DRAFT nên phải kiểm tay.


Ước tính: chương 007 (3 823 ký tự RAW) cắt 900 ký tự → 5 chunk; với luna ≈ USD 0.002/chunk → **≈ USD 0.01/chương**.

### 3.2 Bộ WN (85 chương, `D:\Ebooks\JAKUAKU MONSTER WN`) — các trường hợp mới (đo 2026-10-09)

| Trường hợp | Đo được | Cách xử lý (offline, trong app) |
|---|---|---|
| Ghép file theo số dễ sai | RAW `0043-042　獅子身中の怪異.txt`: 4 chữ số = thứ tự file, 3 chữ số = số chương trong truyện; DRAFT/FINAL/glossary/pronoun đánh số theo **thứ tự file** (`043_獅子身中の怪異_translated.txt`). Ghép theo số chương lệch 1–2 từ chương 038 (do 2 file 設定資料集 xen giữa) | Thư viện nguồn ghép theo **tiêu đề** trong tên file trước, rồi tới thứ tự file; không dùng số chương trong truyện. Người dùng xác nhận cặp (D-N1) |
| Tên file không theo mẫu | 設定資料集【第一章/第二章】, `EX1`, hậu tố `_v2`, `(1)`, thiếu `VOL1` | Như trên; file lẻ cho người dùng tự ghép |
| Encoding | RAW 85/85 là UTF-16 LE có BOM, xuống dòng lẫn CRLF/LF | `FileUtil.readText` đã đọc được (UTF-16 BOM, UTF-8, Windows-31J). Chỉ đường V5 (đã đóng băng) đòi UTF-8 chặt |
| Khối đầu chương và lời tác giả | Đầu file có tiêu đề, dòng "Floor…", `=====`; 80/85 RAW kết thúc bằng lời tác giả (xin bookmark/đánh giá), DRAFT dịch cả phần này | Giữ nguyên như nội dung thường; căn dòng xử lý được |
| Số dòng bằng nhau | 55/85 | Vẫn chạy căn dòng (bài học 016/027) |
| Chuỗi gộp dòng liên tiếp | 043, 069: DRAFT gộp hàng loạt cặp dòng RAW ngắn, kèm dòng bị bỏ | Quy tắc cắt có vùng đệm (dưới) |
| DRAFT thiếu câu | 9/85 chương có dòng RAW không có trong DRAFT (bước 1–0) | Dòng thiếu luôn nằm trọn trong một chunk, model dịch bù; app gắn cờ chunk đó |
| DRAFT không áp glossary | 034, 071: nhiều dòng RAW có thuật ngữ mà DRAFT dùng dạng khác (vd `renjou` vắng 9 lần) | Tín hiệu neo glossary vừa dùng để căn dòng, vừa thành danh sách "điểm cần sửa" của chunk |
| Kana sót trong DRAFT | 79/85 chương | Phát hiện tất định (đã có), đưa vào chunk tương ứng |
| Chương rất dài | 059 (設定資料集) 1 222 dòng ngắn dạng bảng/danh sách | Gom theo ký tự: 34 chunk; DP 1 222×1 213 vẫn nhẹ trên điện thoại |

**Quy tắc cắt chunk có vùng đệm:** chỉ cắt khi đủ ngân sách ký tự **và** có ít nhất 2 cặp 1–1 liền nhau ở mỗi bên điểm cắt. Kết quả trên 18 chương có đáp án dựng từ FINAL (8 LN + 10 WN): **133/133 điểm cắt đúng**. Không có vùng đệm: 135/136, sai 1 ở WN 043 giữa chuỗi gộp. Thực thi tham chiếu: `cut_points()` trong `scripts/chunk/align_lines_reference.py`.

**Trường hợp vẫn chưa tự xử lý được (phải báo người dùng, không đoán):**
- DRAFT dịch từ bản RAW khác (LN và WN khác nhau cả câu chữ) → mức khớp neo glossary và tỷ lệ độ dài sẽ rớt trên diện rộng. App dừng trước provider với "RAW và DRAFT có vẻ không cùng bản"; không tự ghép.
- DRAFT đảo thứ tự cả đoạn: quy hoạch động chỉ xử lý được lệch cục bộ. Mức khớp neo rớt theo vùng → gộp vùng đó thành một chunk lớn; quá lớn thì báo.

### 3.3 Trạng thái thực thi

Toàn bộ mục 3.1–3.2 là tính toán tất định, không gọi API, chạy được trên điện thoại. Tuy nhiên **trong app hiện mới có**: luồng chunk theo dòng của job Dịch (CP-IMPL-2) và ghép 1–1 thuần `fromAlignedFiles`. Bộ căn dòng, quy tắc cắt có vùng đệm, ghép file theo tiêu đề/thứ tự **mới ở dạng prototype Python** trên máy tính (`scripts/chunk/align_lines_reference.py`). Đưa vào app là việc C1.1.

## 4. Quy tắc tiết kiệm khi test

1. Mọi thay đổi kiểm offline trước (test host, fake provider, replay response đã lưu). Không gọi API để phát hiện lỗi định dạng/đầu vào.
2. Mỗi giả thuyết **chỉ chạy live 1 chương**. Chỉ khi kết quả chương đó tốt và owner đồng ý mới chạy thêm **1 chương holdout**. Không chạy ma trận nhiều chương/nhiều nhánh một lần.
3. Mỗi lần live có trần tiền cụ thể do owner duyệt; dừng ngay ở lỗi đầu tiên (đã có trong CP-IMPL-2: dừng tại chunk lỗi đầu tiên).
4. Chương test: dev **007** (ngắn, thẳng dòng, đã có số liệu của mọi nhánh: luna 1 lượt 3/18, v5 8/18, Sol 13/18); holdout **011** (thẳng dòng, chưa dùng để chỉnh).

## 5. Việc tiếp theo (C1) — cho phiên Codex

Ownership: APP (`EditorialPairSourceLoader`, `EditorialApiUiController`, `EditorialApiPageFactory`, `EditorialPairRunService` nếu cần), RUNNER (`EditorialApiV1FixtureRunnerInstrumentedTest`, `scripts/p6/run_group.ps1`), không đụng engine ngoài sửa lỗi có test.

1. **Căn dòng + cắt chunk (engine, thuần JVM):** port `scripts/chunk/align_lines_reference.py` thành `LineAligner` (bước 1–1/1–2/2–1/1–0/0–1, chi phí độ dài + ký hiệu đầu/cuối + neo glossary + số) và `ChunkCutter` (cắt khi đủ ngân sách ký tự RAW — lấy từ cài đặt chunk của luồng Dịch, mặc định 900 — và có ≥ 2 cặp 1–1 liền nhau ở mỗi bên; kiểm từng chunk như mục 3.1.4; báo "không cùng bản" khi mức khớp neo/tỷ lệ độ dài rớt diện rộng). Ghép file trong thư viện nguồn theo tiêu đề rồi thứ tự file (mục 3.2); đọc file qua `FileUtil.readText`. Đo offline thêm trên 85 chương WN: tái tạo 133/133 điểm cắt đúng và các số ở mục 3.2. Thay `fromAlignedFiles` 1–1 thuần bằng kết quả căn dòng, cho cả chương bằng số dòng. Test: ca tổng hợp cho từng kiểu lệch (tách, gộp, thiếu, thêm, bù trừ tách+gộp), cắt không bao giờ rơi trong nhóm tách/gộp, các dòng ghép lại đúng nguyên file. Đo offline trên 28 chương (chỉ đếm): tái tạo đúng các kết quả mục 3.1, ghi số vào mục 7. Nối vào combo nguồn FILES; chunk nghi ngờ → gộp; vẫn nghi → một chunk toàn chương kèm thông báo.
2. **UI:** bỏ nút chọn chế độ Kỹ và đường V5 khỏi luồng người dùng; không còn ô ghi chú/“Cần xem” từ NOTES (chỉ cảnh báo cấu trúc của app). Giữ chuỗi tiếng Việt, test quét chuỗi.
3. **Runner chế độ `CHUNK`:** đọc RAW/DRAFT/Glossary/Pronoun của 1 chương, chạy qua `EditorialPairRunService` thật, ghi `final.txt` + chi phí; dừng ở chunk lỗi đầu tiên.
4. **Offline:** test host đầy đủ (engine, app, `scripts/p6`), replay fake trên 007 thật (DRAFT trả nguyên → `final.txt` == DRAFT từng byte), build qua wrapper (code > 247), test chunk/Editorial tập trung trên `emulator-5554`.
5. **Live (khi có D-C1):** 1 chương **007**, luna reasoning medium. Chấm bằng `score_vs_final.py`; so với 3/18 · 8/18 · 13/18 đã có. Xuất bản app + trang đọc (RAW/DRAFT/app/FINAL) vào `D:\P5E-private\chunk-outputs\`. Dừng, báo cáo trong mục 7 tài liệu này.

PASS offline: test xanh từ archive sạch; replay no-op trả đúng byte; 0 provider call. PASS live: không chunk nào bị chặn cấu trúc, 0 dòng sinh chữ Nhật, và tỷ lệ sửa trúng 007 cao hơn rõ mức luna toàn chương (3/18). Mức "đạt" cuối cùng vẫn là owner đọc.

## 6. Trạng thái

- 0/3 chương được owner chấp nhận; P7 chưa bắt đầu; U1 tạm hoãn.
- CP-IMPL-2 (chunk không ghi chú, dừng ở chunk lỗi đầu tiên) đã commit `90e9a6f7`.
- `fromAlignedFiles` + 5 test: commit cùng tài liệu này.
- Chi tiêu tới nay (ledger Q2): USD 1.17703995 trong trần D-Q2b USD 10.00. D-Q2b/D-Q2c không cấp quyền cho live chunk.

## 7. Nhật ký

- 2026-10-09 — coordinator tiếp quản sau khi Codex hết quota: hoàn tất và commit CP-IMPL-2 (engine 583, app 0 lỗi, `scripts/p6` 93); chốt nguyên nhân bằng số đo mục 1; thêm nguồn hai file thẳng dòng; viết kế hoạch này.
- 2026-10-09 — coordinator: đo lệch dòng trên 28 chương, prototype căn dòng Gale–Church + neo glossary (42/42 điểm cắt đúng trên 8 chương có đáp án; phát hiện lệch bù trừ ở 016/027 mà ghép 1–1 thuần bỏ sót); cập nhật C1.1 thay ghép 1–1 thuần bằng căn dòng.
- 2026-10-09 — coordinator: khảo sát bộ WN 85 chương. Phát hiện ghép file theo số chương sai (thứ tự file ≠ số chương), RAW UTF-16, chuỗi gộp dòng, DRAFT thiếu câu, DRAFT không áp glossary. Thêm quy tắc cắt có vùng đệm 2 cặp: 133/133 điểm cắt đúng trên 18 chương có đáp án. Ghi rõ: căn dòng mới là prototype Python, chưa có trong app.
