# v4.23 — bản dev `4.23-dev.2` (code 277): L3 bộ chương + L4 dịch theo bộ chương (báo cáo rút gọn)

- APK `D:\App Translate Books\artifacts\builds\v4.23-dev.2\build-20261018-175553\TranslateBooks-v4.23-dev.2-code277.apk`, SHA-256 `D2BEE90BE88432411C98FB877EC15558A455EEB6924734993F1BCFED43914D9B`, source `79fbc979`, source ZIP `0169316B…1153`. Cài trên emulator-5554 only. Test APK cuối `v423-dev2-test5-20261018` (`C39680BC…`, commit `106a2159`, chỉ khác ở androidTest).
- Hồi quy archive sạch `79fbc979`: engine 742/1 SKIP; app debug/release/benchmark 547 mỗi biến thể/5 SKIP, 0 FAIL; lint 0 lỗi/64 cảnh báo; AndroidTest compile PASS.
- QA thiết bị (emulator, provider giả, USD 0): 196 phát hiện / 180 PASS / 16 SKIP / 0 FAIL, gồm V423ChapterSet, V423ChapterRun (3 chương mỗi chương một Glossary, Active và settings không đổi; lỗi riêng của chương 2 thì chạy tiếp chương 3; lỗi xác thực dừng hàng đợi), restart RAW.
- Ảnh 23–30 trong `media/` đã xem (bảng xanh/vàng/đỏ, màn Dịch chọn bộ, tóm tắt cuối "2 xong · 1 lỗi · 0 chưa chạy", dừng vì xác thực "1 xong · 1 lỗi · 1 chưa chạy").
- Chưa cài lên điện thoại. Chưa chụp riêng: bảng "xác nhận lần hai" khi xóa mục đang dùng.
- Lần QA nền đầu bị va chạm với lần chạy tay (force-stop chồng nhau) nên được chạy lại tuần tự; kết quả trên là lần sạch.
