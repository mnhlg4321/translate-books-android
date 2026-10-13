# Translate Books — kế hoạch hướng đi sản phẩm

Owner duyệt 2026-10-13. Đây là plan điều hành duy nhất cho đến khi một bản sửa đi tới bước C; khi đó mới mở nhánh/checklist release theo `DEVELOPMENT_WORKFLOW.md`.

## 1. Mục tiêu và thước đo

- Mục tiêu: giảm số chỗ owner phải tự sửa trên mỗi chương app tạo ra, không thêm sửa sai.
- Thước đo duy nhất: trên chương thật, owner đọc đầu ra của app và đếm lỗi app sửa đúng, lỗi còn sót, sửa sai — theo loại (xưng hô, thuật ngữ, nghĩa, văn phong). FINAL của owner là tham chiếu, không phải luật.
- Không phải thước đo: số test, số điểm bộ kiểm, cổng QA, số commit, số tài liệu.

## 2. Luật chống việc vô ích

Một việc chỉ được làm khi trả lời được cả ba câu: (1) phục vụ quyết định sản phẩm nào (chọn vấn đề, phát hành hay bỏ một thay đổi); (2) kết quả nào làm quyết định đó khác đi; (3) đây có phải cách rẻ nhất để có bằng chứng đó.

Cấm: gắn lại nhãn bộ kiểm; dựng cổng/giàn đo mới; tài liệu không kèm thay đổi trạng thái thật; chạy lại test khi mã không đổi; lượt trả phí khi chưa có giả thuyết và tiêu chí phát hành/bỏ.

## 3. Các bước

| Bước | Làm gì | Chi phí | Đầu ra | Quyết định owner |
|---|---|---|---|---|
| A. Kiểm kê vấn đề (1 phiên) | Phân loại các chỗ owner đã sửa DRAFT → FINAL trên 8 chương có sẵn; kiểm DRAFT có do app dịch với cùng file pronoun hay không | USD 0, offline | Một bảng số đếm theo loại; 5 ví dụ thật cho mỗi loại lớn nhất, ở trang đọc riêng ngoài Git | Chọn 1 vấn đề |
| B. Truy vết | 3–5 ví dụ của vấn đề đã chọn: prompt dịch có luật không → DRAFT → Biên tập có gắn cờ không → prompt/response đã lưu → guard → nội dung lưu | USD 0 | Khâu phát sinh lỗi, bản sửa nhỏ nhất, cách kiểm | Duyệt bản sửa |
| C. Sửa | Nhánh mới từ `main`; test đơn vị từ chính các ví dụ (tên giả); replay miễn phí response đã lưu | USD 0 | Commit, test xanh | — |
| D. Một lượt nghiệm thu | Luồng app bình thường trên emulator (APK thật, store SQLite thật, không cổng dừng theo bộ kiểm); 1 chương có vấn đề + 1 chương chưa từng dùng | Trần riêng owner duyệt | Trang đọc RAW / DRAFT / bản app (FINAL tham chiếu) | Phát hành hay bỏ |
| E. Phát hành | Gom các bản sửa đã qua D vào một release theo 14 bước | Theo workflow | Release mới | Tag/merge |

Dừng và đóng việc, không mở thêm đo, khi: A cho thấy vấn đề lớn nhất nằm ngoài phần app sửa được; B không tìm ra khâu gây lỗi; D không làm giảm số chỗ owner phải sửa. Nếu B chứng minh lỗi nên chặn ở khâu dịch, sửa khâu dịch và ngừng đầu tư vào Biên tập cho vấn đề đó. Đổi kiến trúc (trả toàn văn, V5) chỉ được xét khi B chứng minh kiến trúc hiện tại không sửa được vấn đề đã chọn.

## 4. Baseline

- App phát hành: v4.18.0 / code 266, source `82626ac8`, tag `v4.18.0` trên `9d02976e`, `main` `998da341`.
- v4.19 (`feature/v4.19` `3098a059`): đóng băng, không phát hành; thay đổi prompt D-A không vào bản nào trừ khi vấn đề được chọn cần đến nó. Các gói X4b, B 008, cổng X5 và gắn lại nhãn đã hủy.
- Sự thật đã kiểm 2026-10-13: luồng dịch đưa hàng pronoun vào prompt như "mandatory locks" (`PromptBuilder.java:103-120`); finder Biên tập bỏ qua hàng thiếu `self` hoặc `call` (`FixPointFinder.java`, vòng hàng pronoun).
- Dữ liệu cho bước A: `D:\P5E-private\q2-inputs\` (RAW/DRAFT/glossary/pronoun), FINAL của owner, dữ kiện từng dòng `D:\P5E-private\v419-pronoun\line-facts.jsonl` (1.447 dòng, 289 dòng owner sửa, 153 có từ xưng hô đổi). Văn bản sách ở ngoài Git.

## 5. Trạng thái

| Bước | Trạng thái |
|---|---|
| A | đang làm |
| B–E | chưa bắt đầu |

**Next action:** bước A — bảng phân loại chỗ owner sửa trên 8 chương và kiểm nguồn gốc DRAFT; dừng để owner chọn 1 vấn đề.
