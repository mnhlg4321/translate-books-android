# v4.23 — bản dev `4.23-dev.13` (code 291): hết giật, tổ hợp không dừng vì một chương, bảng tiến trình, giao diện sáng

Ngày 2026-10-20, nhánh `feature/v4.23`. Emulator-5554, provider giả, USD 0. Điện thoại `15e84958` vẫn chạy `4.23-dev.11` (không đụng).

## Bản dựng
- `4.23-dev.13` / code 291, event `build-20261020-233104`, APK SHA-256 `64B9824A9E160F9933C3E162FA79713EA6E27F54B4925D2C86327894CB314499`, chứng chỉ `47F31389…EF3C155`, source ZIP `57D4294E…E1740`.
- APK: `D:\App Translate Books\artifacts\builds\v4.23-dev.13\build-20261020-233104\TranslateBooks-v4.23-dev.13-code291.apk`.
- dev.13 khác dev.12 (`CD1FE143…CF589`) đúng một sửa giao diện: tên ô palette trong Appearance dùng màu chữ của chính palette (ô "Light" trước đó chữ xám nhạt trên nền trắng). Hồi quy archive sạch chạy ở `d4d0ad54` (dev.12); từ đó chỉ thêm test và sửa này.

## Hồi quy archive sạch (`d4d0ad54`)
Engine 742 / 1 SKIP; app debug, release, benchmark 647 mỗi loại / 5 SKIP; 0 FAIL; lint 0 lỗi / 61 cảnh báo; AndroidTest compile PASS. Không sửa `editorial-engine/` hay lớp/test `Editorial*`.

## QA thiết bị (dev.13)
227 phát hiện / **211 PASS / 16 SKIP / 0 FAIL** / 0 INCOMPLETE (dev.11: 216/200/16/0). Nhóm mới: `V423PerfInstrumentedTest`, `V423AppearanceInstrumentedTest` (4), thêm 6 test trong `V423ChapterRunInstrumentedTest`. Bản dev.12 cũng 211/16/0.

## Đo giật (J1), cùng bộ dữ liệu tổng hợp 19 RAW / 19 Glossary / 18 Pronoun, tổ hợp 14 chương, có SAF grant
| | tick → khung hình kế tiếp | yêu cầu SAF ghi thật |
|---|---|---|
| dev.11 | p50 83 ms, p95 117 ms | không đếm được |
| dev.12 | p50 13–15 ms, p95 16–17 ms (3 lần × 24 tick) | 0 |
Nguyên nhân thêm tìm được khi đo: mỗi lần tra danh mục model ghi một dòng log ra đĩa trên luồng UI (và cắt lại tệp log khi quá 512 KB). Đã chuyển ghi log sang luồng nền và chỉ log khi kết quả tra đổi.

## Phát lại 58 response bị từ chối vì ngoặc (chỉ đếm)
31 được nhận (15 nhờ sửa ngoặc lẫn kiểu, 11 vì RAW cũng lệch `(`, 5 cân bằng), 19 báo đúng là "bị cắt", 8 vẫn từ chối, 0 ca thiếu dòng bị nhận nhầm.

## Ảnh (`artifacts/qa/v4.23-dev12-20261020/media`, `artifacts/qa/v4.23-dev13-20261020`)
Dark/Light/Paper, 130% + gọn + màu nhấn + bo tròn, bảng tiến trình tổ hợp, hộp thoại sáng/tối, Biên tập ở palette sáng (đọc được, không sửa mã), trang Appearance. Đã mở từng ảnh; lỗi tìm ra: nút "Tệp TXT" chữ tối trên nền nhấn (đã sửa), nhãn "Chương" chạm thanh ở 130% (đã sửa), tên ô palette (đã sửa ở dev.13). Ảnh dev.12 chụp trước khi sửa tên ô palette.

## Giới hạn
- Chưa cài lên điện thoại; mọi hành vi trên máy thật là NOT_MEASURED.
- Quay lại tab Dịch vẫn đọc DB trên luồng UI (kế hoạch dịch cho lựa chọn không đổi thì được dùng lại).
- Mức chạm tối thiểu 44 dp theo yêu cầu; skill ui-ux-pro-max khuyên 48 dp cho Android.
- Các bản `4.23-wip.1–3` chỉ để thử, nằm trong kho lưu trữ.

## Phiếu thử cho owner (K1–K8)
| # | Owner làm | Đạt khi | Kết quả |
|---|---|---|---|
| K1 | Tick/bỏ tick nhiều chương, tick trong Library, sửa vài ô Settings | không giật, không đơ | NOT_MEASURED |
| K2 | Chạy một tổ hợp ngắn | ô không nhảy kích thước; thấy chương i/n, token, chi phí tổ hợp | NOT_MEASURED |
| K3 | Một chương lỗi trong tổ hợp | chương sau vẫn chạy; "Tiếp tục tổ hợp" chạy nốt | NOT_MEASURED |
| K4 | Library > Files | "Resume last checkpoint" không cắt chữ | emulator PASS (ảnh) |
| K5 | Appearance > Light / Paper | toàn app sáng, hộp thoại sáng | emulator PASS (ảnh) |
| K6 | Bật "Theo hệ thống", đổi chế độ tối | tự đổi sáng/tối | emulator PASS (test), máy thật NOT_MEASURED |
| K7 | Đổi màu nhấn, 130%, Gọn, bo góc | xem trước đổi ngay; không nút nào cắt chữ; Khôi phục trả về cũ | emulator PASS (test), máy thật NOT_MEASURED |
| K8 | Chạy tổ hợp có chương dài | không còn lỗi ngoặc do bị cắt; bị cắt thì tự chia/tăng giới hạn | emulator PASS (provider giả), máy thật NOT_MEASURED |
