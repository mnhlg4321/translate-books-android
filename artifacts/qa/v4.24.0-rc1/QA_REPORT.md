# QA_REPORT — v4.24.0-rc.1 (ứng viên phát hành chung điện thoại + PC)

Ngày: 2026-10-29. Không có lệnh gọi nhà cung cấp nào trong bộ kiểm này ($0). Dữ liệu chỉ là QA_; không có văn bản sách trong ảnh, video, log.

## 1. Phiên bản được kiểm

| Thành phần | Giá trị |
|---|---|
| Commit mã RC | `162b96f2` (các commit sau chỉ là test, script, tài liệu; `git diff 162b96f2 -- app/src/main` rỗng) |
| APK điện thoại | `TranslateBooks-v4.24.0-rc.1-code311.apk`, versionName 4.24.0-rc.1, versionCode 311 (> 310) |
| SHA-256 APK | B923583036BB823DB9EBFE6D149D979FE3ECFB550D94A4D4FAA9564A2186B5A8 |
| ZIP nguồn APK | F7C5873E4B9CDA59BCB37BBAE5CE0D857DFEB0F9FCC55C68C44AC5F0F9C5B2D9 |
| Gói PC (zip) | 6D9AAAD4CC452F2B1D56492B72C7991C6D2E02355320A114B6CAE474AD0DFC6B |
| MSI PC | E9C50F4698053D9DCF8F12DAABC70EC248E4782924EAE71B65B42D71703AFFD8 (57301752 byte) |
| Ánh xạ phiên bản MSI | tên hiển thị `rc.1` → ProductVersion **4.24.101** (dev.N → 4.24.N, rc.N → 4.24.(100+N), bản chính 4.24.P → 4.24.(200+P)); bản 4.24.0 cuối phải > 4.24.101 (ví dụ 4.24.200) |

## 2. Kho sạch trên commit RC

| Hạng mục | Kết quả |
|---|---|
| app (JUnit) | 749 test, 5 skip, 0 fail |
| translation-core | 61 test, 0 fail |
| jdbc | 3 test, 0 fail |
| desktop | 69 test, 0 fail |
| golden | 14/14, chạy hai lần |
| lint | 0 lỗi |
| compile androidTest | xanh (APK test dựng được) |
| `app:dependencies` | giống P0b; không có sqlite/xerial hay org.json trong classpath chạy |
| Diff Editorial / editorial-engine | 0 dòng |
| Diff golden fixture | 0 dòng |
| Diff `app/src/main` so với nền | 48 dòng (chuyển JobStatusText + sửa neo popup thư viện) |

Nguồn: `logs/clean-archive-summary.txt`.

## 3. Instrumented đầy đủ (chỉ emulator-5554, provider giả, $0)

Chạy trên APK RC (code 311) với APK test `test-rc1d` (commit nguồn bbcb9faf, APK RC không dựng lại). Tổng 314 test: **251 đạt, 37 bỏ qua, 26 lỗi, 0 chưa xong**.

Đã sửa trong đợt này (chỉ code test, 22 lỗi `NoSuchFieldException cachedJson` do chuyển core + 1 NPE ở `V423PreviewInstrumentedTest`): `StoreCacheFields.find(...)` và `V423PreviewInstrumentedTest` theo cấu trúc mới.

### 3a. Lỗi có sẵn từ v4.23 (22 test, cũng lỗi y hệt trên bản dựng nền v4.23 `4.23-base.1`)
Nguyên nhân là kỳ vọng cũ của test (ví dụ chuỗi "Glossaries: 10 luật · Pronoun: 4 luật" không tồn tại trong sản phẩm v4.23, assert schema 24 so với 30, v.v.), không do v4.24.

- EditorialForeground 1
- EditorialP5CExactBindingFakeE2E 1
- EditorialP5EFreshPilot 4
- EditorialP5EFreshRawBoundary 2
- EditorialP5EFreshRawLineage 1
- V415SettingsPersistence 1
- V423Appearance 3
- V423ChapterRun 2
- V423Polish 2
- V423Resume 3
- V423RunReadability 2

### 3b. Chưa đối chiếu với nền (4 test)
`EditorialP5DVol5RawPilot` (4 lỗi) cần tệp dữ liệu riêng của ứng dụng không có trên emulator; Editorial đã đóng băng nên không chạy trên nền. Trạng thái: **NOT_MEASURED trên nền**, lỗi ở RC được ghi nhận như có sẵn do thiếu dữ liệu, chưa chứng minh bằng chạy nền.

Nguồn: `logs/instrumented-final.txt`, `logs/instrumented-final-summary.json`, `logs/baseline-summary.json`.

## 4. Hiệu năng thật trên emulator-5554 (không phải điện thoại thật)

Macrobenchmark `StartupMacrobenchmark#coldStartup` trên biến thể benchmark dựng từ nguồn RC (bbcb9faf), 5 vòng mỗi lần, trace Perfetto lưu trong `performance/macrobenchmark/`.

| Lần chạy | timeToInitialDisplay trung vị | nhỏ nhất |
|---|---|---|
| RC lần 1 (đầu ra `macrobenchmark-instrument.txt`) | ≈ 3389.3 ms | 2814.8 ms |
| RC lần 2 (`macrobenchmark-coldstartup-rerun.txt`) | 3408.1 ms | — |
| RC lần 3, có trace Perfetto kèm (`macrobenchmark-coldstartup-rc-final.txt`) | **3157.7 ms** | 3053.4 ms |
| Nền v4.23 (`baseline-v423/`) | 3373.2 ms | 2853.1 ms |

Kết luận: khởi động nguội của RC nằm trong dao động của nền v4.23 (3158–3408 ms so với 3373 ms) → **không thấy hồi quy so với v4.23** trên emulator. Mốc v4.21 (≈ 861.7 ms) đo ở thời điểm/trạng thái emulator khác, **không dùng làm so sánh hồi quy** (ghi nhận, không kết luận).

- Chạy lần đầu không có tham số `androidx.benchmark.suppressErrors EMULATOR,LOW-BATTERY` thất bại đúng như mong đợi; đầu ra giữ lại ở `macrobenchmark-first-attempt-without-suppress-arg.txt`.
- Trace của các lần chạy 1 và 2 và của nền bị lần chạy sau ghi đè trên thiết bị; chỉ trace của lần 3 được lưu. Hai bộ trace nền/lần 1-2 không có (số trung vị vẫn nằm trong đầu ra văn bản). Trace nền không lưu để giữ kích thước commit.
- `openEditorialTab` thất bại: "The Biên tập tab was not found on the main screen" — kịch bản macrobenchmark cũ sau khi v4.23 đổi điều hướng; **có sẵn từ v4.23**, không sửa trong bản này (không tính năng mới).

### A/B khởi động nguội cùng trạng thái emulator (2026-10-29, $0)

- Biến thể benchmark của tag v4.21.0 (commit 54936850) và của RC (nguồn 162b96f2 trong test worktree bbcb9faf), cùng một macrobenchmark APK (SHA-256 15a9e5a4... giống nhau ở cả hai), cài mới (gỡ rồi cài) trước mỗi lần, chạy xen kẽ v4.21, RC, 3 vòng, mỗi vòng 5 lần `StartupMacrobenchmark#coldStartup` trên emulator-5554. Đầu ra gốc và `ab.sh` ở `artifacts/qa/v4.24.0-rc1/performance-ab/`; trace Perfetto (170 MB) để ngoài Git ở `D:\P5E-builds\qa-rc1b-traces\`.
- Trung vị timeToInitialDisplay (ms): v4.21: 4453.1 / 4803.1 / 3535.3; RC: 3790.7 / 4076.2 / 3603.4. Nhỏ nhất: v4.21 3686.3 / 4038.8 / 3312.7; RC 3129.1 / 3835.6 / 2628.7.
- Kết luận: RC không chậm hơn v4.21 trong cùng trạng thái (RC thấp hơn ở 2/3 vòng, ngang ở vòng 3; dao động giữa các vòng 3535-4803 ms lớn hơn khác biệt giữa hai bản). Con số ≈0,86 s của v4.21 là do trạng thái emulator lúc đó, không phải mã. Không có hồi quy khởi động nguội để chia đôi; câu hỏi đóng. Giới hạn: emulator, không phải điện thoại thật.

## 5. Ảnh chụp và video

- Điện thoại (emulator, APK RC code 311, dữ liệu trống, không có văn bản sách): `screenshots-phone/01-home.png`, `02-tap-mode.png`, `03-menu.png` (menu có logo, thanh Dịch/Công việc/Biên tập/Thư viện), `04-settings-edit.png`.
- Video ngắn điện thoại (25 s, 540x960): `phone-emulator-smoke.mp4`.
- PC: `screenshots-pc/` (67 ảnh chụp theo bộ QA_ trong phiên P4/R0) và `screenshots-pc/installed-rc1-window.png` (cửa sổ từ MSI đã cài).

## 6. PC: cài từ MSI RC đè lên dev.11

- MSI RC cài đè lên dev.11, mã thoát 0, ProductVersion 4.24.101; 29/29 tệp dữ liệu giữ nguyên.
- Khởi động từ bản đã cài: cửa sổ "Translate Books", phản hồi, còn sống sau 15 s; biểu tượng 256/48/32/16 xác nhận từ exe.
- Phát hiện đã xử lý: cài MSI khi ứng dụng đang chạy cho lỗi 1603 (bản cũ đã bị gỡ); README gói PC đã dặn đóng ứng dụng trước, có test chặn lùi quy tắc.
- Kiểm tra bản chính đè lên RC: **thuộc R6**, chưa làm.

## 7. Số liệu R0 giữ lại (chương thật, đã được chủ duyệt chi trần)

Thẻ điện thoại RAW 19 / Glossary 18 / Pronoun 18 / Tập 0; PC sau khi nhập giống hệt. Chương 2 chạy thật trên PC: 3 lần gọi, 23631 token, $0,011979 trong trần $0,03; 28/29 thuật ngữ Glossary được theo; nguồn còn sót 0; kana 0. Lần QA thứ hai $0,005767 trong trần $0,02. Không có chi phí mới trong R3.

## 8. NOT_MEASURED (và lý do)

- Hiệu năng trên điện thoại thật: R4 cần chủ gõ "duyệt cài"; chưa cài.
- So sánh hiệu năng với v4.21 như hồi quy: mốc cũ ở trạng thái khác, không so sánh tương đương.
- 4 test `EditorialP5DVol5RawPilot` trên nền v4.23: Editorial đóng băng, thiếu dữ liệu riêng.
- Trace Perfetto của lần 1-2 và của nền v4.23: bị ghi đè trên emulator, không lưu.
- Cuộn thư viện lớn, hộp chọn tệp hệ thống (system picker) trên PC và nâng cấp schema v1–v29 từ máy thật: chưa chạy tự động.
- Kiểm tra bản 4.24.0 chính đè lên RC (MSI): R6.
- Chữ ký MSI/APK phát hành: MSI không ký (đã ghi nhận từ đầu chương trình).

## 9. Tồn đọng cần quyết định

Không có lỗi mới do bản này gây ra trong bộ kiểm. 26 lỗi instrumented đều thuộc kỳ vọng cũ hoặc thiếu dữ liệu riêng (mục 3). Quyết định phát hành thuộc chủ.
