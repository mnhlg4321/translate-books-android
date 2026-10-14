# v4.22 Q1 — Yêu cầu làm việc: thiết kế phát hiện lỗi xưng hô theo người nói (offline)

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (mục 5, khối v4.22, Q1). Nhánh: `feature/v4.22` (chỉ tài liệu và mã thử nghiệm tắt mặc định; không ảnh hưởng hành vi 4.22.0). Owner duyệt bắt đầu ở dạng thiết kế + test offline 2026-10-14; lượt đo trả phí xin riêng.

## 1. Vấn đề (đã chứng minh, lặp lại lần thứ năm)

App không biết dòng thoại nào do ai nói với ai, nên:

- Chương 013 Kumo tập 7 (`D:\P5E-private\v422-q0\finder-points.txt`): finder tạo 19 điểm; 15 điểm xưng hô giả (lời Potimas bị so với hàng Hyuban và ngược lại), 2 glossary giả; lỗi thật dòng 151 (Ariel → Shiro dùng "tôi/cô", hàng ghi `chị`) có điểm nhưng model giữ; lỗi thật dòng 346 (Potimas → Ariel dùng "lão", từ của hàng Ariel → Potimas) không có điểm. Bản cuối chỉ đổi 1 dòng, không cần thiết.
- JAKUAKU bước B: 50/122 lỗi tự xưng là lấy `self` của nhân vật khác; finder không phát hiện vì từ sai thuộc một hàng khác.
- Các hướng vá bằng luật chữ đã đo và loại (cổng cue trong dòng RAW, dò ngôi ba, mở rộng từ vựng, thay thế tất định).

## 2. Hướng thiết kế cần đánh giá

Tách việc theo nguyên tắc "model làm nội dung, app làm ghi sổ":

1. **Model ghi người nói/người nghe** cho mỗi dòng thoại của một đoạn, chỉ bằng tham chiếu đơn giản: chọn từ danh sách nhân vật lấy từ file pronoun (cột `speaker`/`target`), hoặc `?` khi không chắc. Không viết lại câu.
2. **App đối chiếu tất định**: với dòng có người nói X và người nghe Y, lấy hàng pronoun X → Y (đúng phạm vi); từ tự xưng/gọi trong dòng trái với `self`/`call` của hàng đó thì thành điểm, kèm đúng một hàng. Dòng có `?` không tạo điểm.
3. **Sửa**: model chỉ được hỏi trên những điểm đó (đã biết hàng), hoặc app thay thế tất định khi chỉ có một cách đổi; câu hỏi mở cho đánh giá.

## 3. Việc cần làm (offline, USD 0)

1. **Tài liệu thiết kế** (mục mới trong file này): định dạng câu trả lời ghi người nói (dòng `[n] X > Y` hoặc tương tự, đơn giản nhất cho parser), cách app dựng danh sách nhân vật, xử lý lời kể (`@NARRATOR`), nhóm (`@GROUP`), người nghe nhiều, phạm vi `scope`, và chỗ gắn vào luồng hiện tại (`FixPointFinder`, `TargetedFixPrompt`, `AddressEditGuard`, `EditorialPairRunService`) — bước mới chạy trước hay thay bước tìm điểm ADDRESS. So sánh chi phí: thêm một lời gọi mỗi đoạn hay gộp vào lời gọi sửa.
2. **Ước lượng giá trị bằng nhãn tham chiếu**: với chương 013 Kumo và chương 014 JAKUAKU, lập nhãn người nói/người nghe cho các dòng thoại (đọc RAW và dòng kề; ghi rõ là nhãn tham chiếu do phiên làm việc lập, không phải đo model của app; lưu ngoài Git). Chạy bộ đối chiếu tất định với nhãn đó: số điểm thật / giả so với finder hiện tại; số lỗi đã biết bắt được (013: dòng 151 và 346; 014: các ca bước B).
3. **Mã thử nghiệm tắt mặc định** (nếu cần cho bước 2): parser câu trả lời người nói, bộ đối chiếu tất định, test tổng hợp (nhân vật giả). Không nối vào luồng chạy của app, không đổi prompt hiện hành.
4. **Đề xuất lượt đo trả phí**: giả thuyết (model ghi đúng người nói ≥ một ngưỡng; số điểm giả giảm; lỗi thật được bắt), chương (013 Kumo và một chương JAKUAKU chưa dùng để chỉnh), số lời gọi tối đa, trần USD, cách chấm (so nhãn model với nhãn tham chiếu; owner đọc các điểm mới). Dừng chờ owner duyệt.

## 4. Tiêu chí

- Tài liệu có quyết định thiết kế rõ, các phương án đã cân nhắc và lý do chọn; số liệu bước 2 cho thấy hướng này loại được phần lớn điểm giả và bắt được các lỗi thật đã biết, hoặc chỉ ra vì sao không.
- Nếu số liệu bước 2 không cho thấy cải thiện rõ: dừng, báo owner, không đề xuất lượt trả phí.

## 5. Không làm

Không gọi provider, không đổi hành vi app 4.22.0, không đưa FINAL của owner vào prompt, không thêm bộ kiểm/cổng đo ngoài phần cần cho bước 2.
