# Kế hoạch thử A/B prompt v14 với v16

**Trạng thái:** HẾT HIỆU LỰC (2026-10-23). Điện thoại đang dùng `18.lightnovel_general_qc_translation_only_v18_DRAFT.yaml`. Đây là bản v14 cộng phần văn phong và cách đọc `(omit)`, tốt hơn phần văn phong của v16. So v14 với v16 không còn phục vụ quyết định nào. Kế hoạch này không có bước Refine.

Mọi việc với prompt từ nay lấy v18 làm gốc. Những gì v18 còn thiếu so với các lỗi đo được trên điện thoại:
- cách đọc `@NARRATOR`, nhóm `@…`, target `*`;
- ruby `漢字《かな》`;
- kana kéo dài, thán từ, tiêu đề như あとがき;
- không bỏ nguyên câu tiếng Nhật;
- tên không có trong glossary;
- trích dẫn 「」 nằm giữa câu kể.

Nếu owner muốn, các ý này được thêm vào v18 thành một bản nhỏ, không đụng phần văn phong.

**Quyết định của owner (2026-10-23):**
- Giữ nguyên v18.
- Sau tập thật tiếp theo được dịch bằng v18, đo offline (không gọi provider) trên bản sao DB điện thoại, rồi mới quyết định thêm quy tắc.
- Đo 5 chỉ số, chỉ in số đếm và so với mốc v14 (1.254 chunk):
  1. chunk còn kana/kanji ngoài lock; tách riêng `ー`/`ッ`/`ぁ`/thán từ với dòng chưa dịch;
  2. chunk còn ruby `《かな》`;
  3. số dòng mà trích dẫn 「」 giữa câu kể bị đổi sang “ ”, trên 1.000 dòng (mốc v14: 8,8);
  4. tên không có trong glossary bị để nguyên chữ Nhật;
  5. số lượt bị F2 từ chối vì `RETAINED_SOURCE_LINE`.
- Chỉ thêm quy tắc cho chỉ số nào vẫn còn đáng kể. Bản mới phải giữ nguyên toàn bộ v18 (kiểm bằng script).

**Đo lần 1 (2026-10-23, Kumo tập 13 chương 1, bản dịch v18 lúc 2026-10-22 23:47, 528 dòng có chữ, chỉ số đếm):**
- dòng chưa dịch: 0;
- dòng còn kana: 16, gồm 14 dòng có 1 biệt danh nhân vật không có trong Glossary bị dịch nửa chữ (sửa bằng 1 dòng Glossary, không cần quy tắc prompt) và 2 dòng còn dấu `ー` cuối câu;
- ruby: 0 lọt / 70 trong RAW;
- trích dẫn 「」 giữa câu kể: 1 bị đổi sang “ ”, 1 giữ nguyên (2 trường hợp, quá ít để kết luận);
- 91 mục Glossary có trong RAW: 0 mục còn chữ Nhật;
- lượt F2 từ chối: không đo được, bản dịch có trước dev.25.
- Kết luận: chưa đủ dữ liệu để thêm quy tắc; đợi cả tập.

**Quyết định sản phẩm phục vụ:** có đổi prompt dịch mặc định từ v14 sang v16 hay không.

## 1. Hai bản prompt

| | v14 (đang dùng) | v16 (nháp) |
|---|---|---|
| Tệp | `D:\Ebooks\1. Prompt cac the loai\14.lightnovel_general_qc_translation_only_v14.yaml` | `D:\Ebooks\1. Prompt cac the loai\16.lightnovel_general_qc_translation_only_v16_DRAFT.yaml` |
| Độ dài | 6.336 ký tự (~2.200 token) | 10.758 ký tự (~3.800 token, +70%) |
| Chi phí thêm | — | khoảng +9% mỗi lượt gọi (khoảng +1.600 token đầu vào) |

**Nguyên tắc dựng v16 (sửa ngày 2026-10-22, sau khi owner phát hiện bản trước làm mất mục văn phong và QC):**
- v16 = **nguyên văn v14** cộng phần chèn thêm. Không bỏ quy tắc nào của v14.
- Đã kiểm bằng script: cả 94 câu quy tắc của v14 đều còn trong v16, độ trùng từ ≥ 90%.
- Chỉ có hai chỗ câu của v14 được mở rộng chứ không bị thay:
  - quy tắc `「」` thêm "kể cả trích dẫn nằm giữa câu kể";
  - mục ký hiệu thêm các loại ngoặc khác, `!?` và `・・・`.

- Chi phí thêm tính từ dữ liệu điện thoại: đầu vào ~$0,24 / 1M token, trung vị một lượt gọi $0,0041.
- Các tệp prompt nằm ngoài Git, trong thư mục prompt của owner.

**Điểm mới của v16 và nguồn:**

| Điểm mới | Nguồn |
|---|---|
| Danh sách mẫu câu Nhật hay bị bê nguyên, kèm cách viết Việt và giới hạn (không thêm từ láy, hình ảnh mà raw không có) | Zhang et al. 2024; Li et al. ACL 2025: lỗi lớn nhất của LLM dịch văn học là dịch sát chữ, từ vựng nghèo. VnExpress 2025: câu văn không thuần Việt là lỗi phổ biến (Vũ Thế Khôi, Bùi Trần Ca Dao) |
| Giữ nhịp, dấu câu, nét lạ của tác giả; giọng nhân vật khác nhau | Trần Quốc Tân (Marginalia): AI "cào bằng" giọng, mất nhịp. Nguyễn Xuân Minh (Nhã Nam): phải hình dung giọng điệu nhân vật |
| Hậu tố: **giữ quy ước của v14** (không tự thêm hoặc xóa), chỉ nói rõ nguồn của quy ước là lock và bản dịch trước | Owner chốt 2026-10-22: không đổi quy ước hậu tố. Đề xuất "mặc định bỏ" (dựa trên J-Novel Club) không được dùng |
| Tượng thanh → từ tượng thanh Việt; tượng hình → động từ/từ láy | Kubo 1997; Hayase 1978; Sell & Pasfield-Neofitou 2016 |
| Lược chủ ngữ có kiểm soát, không lặp tên | Jay Rubin, *Making Sense of Japanese* |
| Chỗ cho thẻ văn phong từng bộ | Dương Tường: hiểu phong cách tác giả trước khi dịch |
| Đọc đúng lock `(omit)` (ô bỏ trống = không quy định), `@NARRATOR`; ruby; không để sót kana; giữ đúng loại ngoặc | Định dạng lock thật của app (`PromptContextBuilder.formatP3Rule`); lỗi đo trên 1.254 chunk điện thoại |

**Giữ nguyên từ v14:** toàn bộ, gồm cả mục "VĂN PHONG TIẾNG VIỆT", "XƯNG HÔ, SPEAKER VÀ GIỌNG NHÂN VẬT", "PHÂN TÍCH NHẬT → VIỆT", "HỢP ĐỒNG ĐẦU RA" và "QC NỘI BỘ TRƯỚC KHI TRẢ". Danh sách QC có thêm 4 ý mới, gắn vào các dòng cũ:
- xưng hô từng cặp đúng lock;
- nhịp của tác giả;
- không còn câu bê nguyên cấu trúc Nhật;
- loại ngoặc, kana kéo dài, ruby.

**Đã bỏ sau phản biện:** bước "nghĩ trước khi viết". Bước này làm tăng token reasoning trong khi lỗi cắt cụt F1 chưa sửa, và Wu et al. 2025 cho thấy việc chia bước không phải nguồn lợi ích chính.

**Lưu ý về v15:** v15 cũng đọc sai `(omit)` thành "không dùng đại từ". Không dùng v15.

**Giới hạn của nguồn:** không tìm được dịch giả Nhật–Việt hay NXB light novel Việt Nam nào công bố quy tắc riêng về hậu tố và xưng hô.

## 2. Quyết định của owner và phần tùy chọn

1. **Hậu tố** (owner chốt 2026-10-22): không đổi quy ước. v16 đã được sửa về cách của v14: không tự thêm hoặc xóa honorific trái quy ước. Chỉ số "hậu tố còn lại" vì vậy không dùng để chấm A/B.
2. **Thẻ văn phong từng bộ:** tùy chọn. Nếu dùng, dán vào **cuối** tệp instruction của bộ đó, dạng:
   ```
   THẺ VĂN PHONG TÁC PHẨM
   - Thể loại và độc giả: …
   - Ngôi kể và giọng người kể: …
   - Mức trang trọng chung: …
   - Nhân vật có giọng đặc biệt: …
   - Cách nói cần tránh trong bộ này: …
   ```
   Trong A/B, **không** dùng thẻ, để chỉ một biến thay đổi là prompt.

## 3. Thiết kế thử

**Điều kiện trước:** nên sửa F1 trước (gói `docs/V423_TRANSLATION_RUNTIME_FIX_WORK_REQUEST.md`). Nếu chưa sửa:
- chunk nào bị cắt cụt rồi chạy lại với giới hạn tăng thì loại khỏi phần đọc so sánh;
- ghi số lượt cắt cụt riêng.

**Chỉ một biến thay đổi là prompt.** Giữ nguyên và ghi lại cho từng job:
- model `openai/gpt-5.6-luna`;
- `reasoningEffort` đặt **rõ** là `medium` (không để `auto`);
- `temperature=0.3`, chunk 1.700 token, preset `balanced`;
- cùng tệp Glossary và Pronoun cho từng chương.

**App chỉ giữ một instruction.** Mỗi lần nhập YAML sẽ ghi đè `translationInstructions`. Vì vậy:
- nhập lại đúng tệp ngay trước mỗi lượt;
- kiểm tên tệp trên chip ở màn Dịch;
- sau khi chạy, kiểm `settingsHash` của job khác nhau giữa hai prompt và giống nhau giữa các lượt cùng prompt.

**Chương thử** (chọn xong phải kiểm chương có đủ điều cần thử):

| # | Loại | Phải có trong chương | Gợi ý |
|---|---|---|---|
| C1 | Học đường, nhiều thoại | nhiều cặp xưng hô bạn bè/tiền bối; nhân vật thô lỗ | một chương JAKUAKU MONSTER vol 1 không thuộc 006/008/011 |
| C2 | Fantasy, câu kể dài | `@NARRATOR` có lock; thuật ngữ hệ thống; `男`/`女` lặp | một chương KUMO vol 12 |
| C3 | Tình cảm, đối thoại hai người | lock chỉ khóa một phía (`(omit)`); từ tượng hình | một chương ATE_JP_VOL6 |

**Số lượt:** 3 chương × 2 prompt = 6 lượt, cộng 1 lượt v14 lặp lại cho C1 để đo nhiễu. Tổng 7 lượt.

**Làm mù:**
- Người chạy (Codex, khi owner cấp quyền chi cho đúng kế hoạch này) xuất các bản dịch, đổi tên ngẫu nhiên thành X/Y (và Z cho lượt lặp).
- Bảng ánh xạ `AB_MAPPING.txt` đặt ngoài Git; chỉ mở sau khi owner chấm xong.
- Owner không tự chạy, vì chip tên tệp sẽ cho owner biết bản nào là bản nào.

## 4. Ngân sách

- Ước tính: một chương khoảng 4 chunk, ~$0,018 mỗi lượt (đã tính ~7% lượt gọi lại). 7 lượt ≈ **$0,13**.
- **Trần: $0,30** cho toàn bộ thử nghiệm.
  - `costLimitUsd` của app chỉ được kiểm từ lượt gọi thứ hai của một chunk (`TranslationEngine:178`), nên đây **không** phải trần cứng.
  - Người chạy phải theo dõi chi phí sau mỗi chương (thẻ Token và chi phí ở trang Công việc), và dừng nếu tổng vượt $0,25.
- Không có lượt gọi provider nào ngoài 7 lượt này.

## 5. Chấm điểm

**A. Đo tự động.** Chạy offline trên bản sao DB sau lượt thử, chỉ in số đếm, theo từng chương và từng bản:
- số dòng còn kana/kanji;
- hậu tố: chỉ kiểm tính nhất quán (cùng một nhân vật không lúc có hậu tố, lúc không trong cùng chương), không chấm theo số lượng;
- số chỗ đổi loại ngoặc so với raw;
- chênh lệch số dòng;
- mẫu dịch sát trên 1.000 dòng: mở câu bằng "Việc", "người đàn ông"/"người phụ nữ" lặp, "một cách", "vừa… vừa", "rồi" thay `ながら`;
- số lượt cắt cụt, lượt gọi lại;
- token vào/ra và chi phí.

**B. Owner đọc mù.**
- Mỗi chương đọc 2 đoạn khoảng 40 dòng: một đoạn nhiều thoại, một đoạn câu kể. Bản X và Y đặt cạnh RAW.
- Với mỗi đoạn, ghi:
  - bản thích hơn: X, Y hoặc hòa;
  - số lỗi theo 4 loại: nghĩa sai/thiếu, xưng hô, văn phong, ký hiệu.
- Với C1, owner đọc thêm bản Z (lượt lặp) theo cùng cách, cũng không biết Z là gì.
- Bản FINAL của owner (nếu có) chỉ để tham khảo khi đọc, không dùng làm đáp án so khớp.

**C. Phân xử.** Chỗ nào hai bản khác nhau về nghĩa thì đối chiếu RAW để quyết định bản nào đúng.

**Gộp kết quả một chương:**
- hai đoạn cùng chọn một bản, hoặc một đoạn chọn và đoạn kia hòa → chương thắng cho bản đó;
- mỗi đoạn chọn một bản khác nhau, hoặc cả hai hòa → chương hòa.

## 6. Tiêu chí quyết định

**Đổi sang v16 khi đủ cả 4 điều kiện:**
1. v16 thắng ít nhất 2 trên 3 chương, và không thua chương nào;
2. lỗi "nghĩa sai/thiếu" của v16 không nhiều hơn v14 (theo owner chấm và phân xử RAW);
3. kana sót và ngoặc sai của v16 thấp hơn hoặc bằng v14;
4. chi phí tăng không quá 10%, và số lượt cắt cụt không tăng.

**Kiểm nhiễu (C1):**
- So hai bản v14 (X hoặc Y với Z), dùng cùng cách chấm: bản thích hơn và số lỗi.
- Nếu owner phân biệt hai bản v14 rõ như phân biệt v14 với v16 (chọn hẳn một bản, số lỗi chênh tương đương), thì kết quả C1 coi là không phân biệt được.

**Nếu kết quả lẫn lộn:**
- giữ v14;
- ghi lại quy tắc nào của v16 có vẻ thắng (ví dụ kana, ngoặc, hậu tố);
- ghép các quy tắc đó vào v14 thành v17, và v17 phải được thử riêng theo cùng kế hoạch trước khi dùng.

## 7. Rủi ro và giới hạn

- **Mẫu nhỏ:** 3 chương, mỗi bên một lượt (cộng một lượt đo nhiễu). Đủ để chọn hướng, không đủ để kết luận thống kê.
- **v16 dài hơn 70%:**
  - có thể làm model kém chú ý tới quy tắc cũ; phần đo tự động có mục số dòng và ngoặc để bắt điều này;
  - chi phí ước tính +9%, sát ngưỡng 10% ở tiêu chí 4. Nếu đo thật vượt 10%, tiêu chí 4 không đạt, dù chất lượng tốt hơn. Khi đó owner quyết riêng là chấp nhận chi phí hay rút gọn phần thêm;
  - nếu sửa được F3 (cache prompt), phần instruction lặp lại sẽ rẻ hơn nhiều.
- **Dữ liệu:** không đưa văn bản sách vào Git. Ví dụ trong v16 là câu tự viết, không lấy từ sách hay từ FINAL.
