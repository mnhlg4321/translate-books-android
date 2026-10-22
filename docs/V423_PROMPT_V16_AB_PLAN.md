# Kế hoạch thử A/B prompt v14 với v16

**Trạng thái:** ĐÃ PHẢN BIỆN (2026-10-22). Agent phản biện tìm 12 lỗi; đã sửa cả v16 và kế hoạch. Chưa chạy.

**Quyết định sản phẩm phục vụ:** có đổi prompt dịch mặc định từ v14 sang v16 hay không.

## 1. Hai bản prompt

| | v14 (đang dùng) | v16 (nháp) |
|---|---|---|
| Tệp | `D:\Ebooks\1. Prompt cac the loai\14.lightnovel_general_qc_translation_only_v14.yaml` | `D:\Ebooks\1. Prompt cac the loai\16.lightnovel_general_qc_translation_only_v16_DRAFT.yaml` |
| Độ dài | 6.336 ký tự (~2.200 token) | 8.134 ký tự (~2.850 token, +28%) |
| Chi phí thêm | — | khoảng +4% mỗi lượt gọi |

- Chi phí thêm tính từ dữ liệu điện thoại: đầu vào ~$0,24 / 1M token, trung vị một lượt gọi $0,0041.
- Các tệp prompt nằm ngoài Git, trong thư mục prompt của owner.

**Điểm mới của v16 và nguồn:**

| Điểm mới | Nguồn |
|---|---|
| Danh sách mẫu câu Nhật hay bị bê nguyên, kèm cách viết Việt và giới hạn (không thêm từ láy, hình ảnh mà raw không có) | Zhang et al. 2024; Li et al. ACL 2025: lỗi lớn nhất của LLM dịch văn học là dịch sát chữ, từ vựng nghèo. VnExpress 2025: câu văn không thuần Việt là lỗi phổ biến (Vũ Thế Khôi, Bùi Trần Ca Dao) |
| Giữ nhịp, dấu câu, nét lạ của tác giả; giọng nhân vật khác nhau | Trần Quốc Tân (Marginalia): AI "cào bằng" giọng, mất nhịp. Nguyễn Xuân Minh (Nhã Nam): phải hình dung giọng điệu nhân vật |
| Quy tắc hậu tố: mặc định bỏ, chuyển vào xưng hô | J-Novel Club, nhân viên trên diễn đàn. **Cần owner duyệt, xem mục 2** |
| Tượng thanh → từ tượng thanh Việt; tượng hình → động từ/từ láy | Kubo 1997; Hayase 1978; Sell & Pasfield-Neofitou 2016 |
| Lược chủ ngữ có kiểm soát, không lặp tên | Jay Rubin, *Making Sense of Japanese* |
| Chỗ cho thẻ văn phong từng bộ | Dương Tường: hiểu phong cách tác giả trước khi dịch |
| Đọc đúng lock `(omit)` (ô bỏ trống = không quy định), `@NARRATOR`; ruby; không để sót kana; giữ đúng loại ngoặc | Định dạng lock thật của app (`PromptContextBuilder.formatP3Rule`); lỗi đo trên 1.254 chunk điện thoại |

**Giữ nguyên từ v14:** thứ tự ưu tiên nghĩa trước văn phong, đủ dòng, đầu ra một cặp `<TRANSLATION>`.

**Đã bỏ sau phản biện:** bước "nghĩ trước khi viết". Bước này làm tăng token reasoning trong khi lỗi cắt cụt F1 chưa sửa, và Wu et al. 2025 cho thấy việc chia bước không phải nguồn lợi ích chính.

**Lưu ý về v15:** v15 cũng đọc sai `(omit)` thành "không dùng đại từ". Không dùng v15.

**Giới hạn của nguồn:** không tìm được dịch giả Nhật–Việt hay NXB light novel Việt Nam nào công bố quy tắc riêng về hậu tố và xưng hô.

## 2. Hai việc owner chốt trước khi chạy

1. **Hậu tố:** v14 ghi "không tự thêm hoặc xóa honorific trái quy ước"; v16 đổi thành mặc định bỏ.
   - Đây là đổi quy ước, không chỉ đổi câu chữ.
   - Khuyến nghị: duyệt "mặc định bỏ", vì đây là điều owner đã tự làm 227 lần khi sửa DRAFT JAKUAKU MONSTER thành FINAL.
   - Nếu owner không duyệt, sửa v16 lại theo v14 trước khi thử. Khi đó chỉ số "hậu tố còn lại" không được dùng để chấm.
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
| C1 | Học đường, nhiều thoại | ≥ 10 hậu tố trong raw; nhân vật thô lỗ | một chương JAKUAKU MONSTER vol 1 không thuộc 006/008/011 |
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
- số hậu tố còn lại (chỉ dùng nếu owner duyệt "mặc định bỏ");
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
- **v16 dài hơn 28%:** có thể làm model kém chú ý tới quy tắc cũ. Phần đo tự động có mục số dòng và ngoặc để bắt điều này.
- **Dữ liệu:** không đưa văn bản sách vào Git. Ví dụ trong v16 là câu tự viết, không lấy từ sách hay từ FINAL.
