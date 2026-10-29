# QA_REPORT — v4.24.0 (phát hành chung điện thoại + PC)

Ngày: 2026-10-30. $0, không gọi nhà cung cấp, không có văn bản sách trong báo cáo.

## 1. Bản phát hành

| Thành phần | Giá trị |
|---|---|
| Commit phát hành | `e176d23f70ac51493982b28b46d315551d7d9379` (nhánh dựng `feature/v4.24.0`, ngắn hạn, như v4.21.0) |
| APK | `TranslateBooks-v4.24.0-code313.apk`, **versionName 4.24.0, versionCode 313** |
| SHA-256 APK | 4ABB5FF113F2446394E6F3300D355AEF13C29C586884976D27F152D268E2C3EF |
| Gói PC (zip) | 78829A33B723385A8DD76A2A2575730A2B8A184A7C0D271A50EF06CC69467417 |
| MSI PC | CF31D304B58C4AA35DE35F04B9290B151729EF1DA07579D5AA54B197D8BE559D (57301752 byte), ProductVersion **4.24.200** |

Sự kiện dựng đã bỏ (không dùng, giữ lại làm vết): APK `build-20261030-063604` (4.24.0/code312) và gói PC `desktop-v4.24.0.1` (msi ProductVersion 4.24.0, thấp hơn RC nên không cài đè được). Cả hai dựng từ commit trước `e176d23f`; `e176d23f` thêm `-ExactReleaseVersion` cho `build-desktop.ps1` để gói PC mang đúng tên 4.24.0 và ProductVersion 4.24.200.

## 2. Mã sản xuất giống hệt RC

`git diff 162b96f2 e176d23f` trên `app/src/main`, `translation-core`, `translation-core-jdbc`, `desktop/src/main`: **0 dòng**. Thay đổi từ RC chỉ là code test, script, tài liệu. Nên bằng chứng RC (instrumented trên emulator, Macrobenchmark/Perfetto, A/B với v4.21.0, ảnh, video) áp dụng cho bản phát hành; APK phát hành chỉ khác RC ở versionName/versionCode.

## 3. Kho sạch trên đúng commit phát hành (`git archive e176d23f`)

| Hạng mục | Kết quả |
|---|---|
| app (JUnit) | 749 test, 5 skip, 0 fail |
| translation-core | 61 test, 0 fail |
| jdbc | 3 test, 0 fail |
| desktop | 70 test, 0 fail |
| golden | 14/14 hai lần liên tiếp (không dùng `-Dgolden.update`) |
| lint | 0 lỗi |
| compile androidTest | xanh |
| Diff Editorial/editorial-engine so với nền | 0 dòng |
| Diff fixture golden so với RC | 0 dòng |

Nhật ký: `D:\P5E-builds\qa-rc1\reg-rel.out`, `reg-rel-gradle.log` (ngoài Git).

## 4. Cài MSI 4.24.0 đè lên RC trên PC

- Trước: Windows Installer báo "Translate Books 4.24.101" (RC).
- Cài: `msiexec /i ... /qn`, mã thoát **0**. Sau: "Translate Books 4.24.200".
- Dữ liệu `%LOCALAPPDATA%\TranslateBooks` (27 tệp trừ logs và khóa): danh sách đường dẫn, kích thước và SHA-256 trước và sau **giống hệt**.
- Khởi động từ bản đã cài: cửa sổ "Translate Books" còn sống và phản hồi sau 15 s.

## 5. Bằng chứng giữ từ RC (cùng mã sản xuất)

`artifacts/qa/v4.24.0-rc1/`: Macrobenchmark `coldStartup` (RC trung vị 3157.7 / 3389.3 / 3408.1 ms; nền v4.23 3373.2 ms), A/B cùng trạng thái với v4.21.0 (6 vòng xen kẽ, RC không chậm hơn), trace Perfetto 5 lần của RC, instrumented 314 test (251 đạt, 37 bỏ qua, 26 lỗi trong đó 22 giống nền v4.23 và 4 không chạy trên nền), ảnh điện thoại và PC, video điện thoại.

## 6. NOT_MEASURED

- Hiệu năng trên điện thoại thật (RC đã cài trên máy của chủ, chưa đo số liệu).
- APK phát hành 4.24.0/code313 chưa cài lên điện thoại thật (cần chủ gõ "duyệt cài" riêng); chưa chạy instrumented trên chính APK này (mã sản xuất giống RC).
- Nâng cấp schema v1–v29 trên PC; hộp chọn tệp hệ thống trên PC.
- Ảnh chụp cửa sổ PC của bản 4.24.0 đã cài: không lưu (ảnh toàn màn hình có cửa sổ khác); chỉ có kết quả khởi động bằng số.
- Chữ ký: MSI không ký số; APK debuggable.
- MSI phát hành cài đè RC: đã làm (mục 4). Cài đè bản phát hành lên bản phát hành sau (4.24.1): chưa có.
