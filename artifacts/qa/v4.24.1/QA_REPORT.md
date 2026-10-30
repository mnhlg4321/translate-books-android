# QA_REPORT — v4.24.1 (vá giao diện hàng thẻ Thư viện, chỉ điện thoại)

Ngày: 2026-10-30. $0, không gọi nhà cung cấp, không có văn bản sách trong báo cáo.

## 1. Bản phát hành

| Thành phần | Giá trị |
|---|---|
| Nhánh / commit dựng | `feature/v4.24.1`, commit `7e8c5475` (mã sửa ở `8ff63926`) |
| APK | `TranslateBooks-v4.24.1-code319.apk`, versionName 4.24.1, **versionCode 319** (> 318) |
| SHA-256 APK | 655576E98D440C754E1BC8CF0A1F611DADDE97F64C0820887217A3713397D00C |
| Dựng bằng | `scripts/build-and-save.ps1 -ExactReleaseVersion 4.24.1`, sự kiện `build-20261030-073455` |
| Gói PC | không thuộc bản này (PC dùng cây điều hướng JavaFX); gói PC vẫn là 4.24.0 |

## 2. Thay đổi và nguyên nhân

Chi tiết trong `docs/V4241_LIBRARY_TAB_ROW_FIX.md`. Nguyên nhân lỗi đã xác nhận trên emulator ở 361 dp: hàng cao 56 dp có đệm 4+6 dp nên chỉ còn 46 dp cho thẻ cao 48 dp trong `HorizontalScrollView`, làm đáy mọi thẻ bị cắt (không phải bóng đổ). Sửa: hàng cao đúng 4+48+6 dp, bốn thẻ bằng nhau lấp đầy hàng (cuộn ngang khi chữ lớn), đệm nhãn 8 dp (cũ 4 dp), viền thẻ chọn 2 dp đủ bốn cạnh, nút ⋮ cùng chiều cao/bo góc/viền với thẻ và ký tự 20 sp, thẻ đang chọn tự cuộn vào tầm nhìn.

Mã đổi so với `v4.24.0`: chỉ `MainActivity.java` (hàm `buildLibraryDestination`), `LibraryTabLayoutModel.java` và `LibraryTabLayoutModelTest.java`. Diff `editorial-engine` và fixture golden so với `v4.24.0` là 0 dòng.

## 3. Kho sạch trên đúng commit dựng (`git archive 7e8c5475`)

| Hạng mục | Kết quả |
|---|---|
| app (JUnit) | 753 test, 5 skip, 0 fail (749 cũ + 4 mới của `LibraryTabLayoutModelTest`) |
| translation-core | 61 test, 0 fail |
| jdbc | 3 test, 0 fail |
| desktop | 70 test, 0 fail |
| golden | 14/14 hai lần liên tiếp (không dùng `-Dgolden.update`) |
| lint | 0 lỗi |
| compile androidTest | xanh |

Nhật ký: `D:\P5E-builds\qa-rc1\reg-4241r.out` (ngoài Git).

## 4. Ảnh chụp trước/sau (emulator, 361 dp, 1264 px, 560 dpi)

`artifacts/qa/v4.24.1/library-tab-row/`: `sheet-before-default.png`, `sheet-before-light.png`, `sheet-after-default.png`, `sheet-after-light.png` (chữ app 100/115/130%, nền tối và sáng, cả 4 thẻ), hai ảnh đầy đủ mỗi bên và script `libshots.py`. Ảnh chụp trên bản dev 4.24.1-dev.4; mã giống bản phát hành. Bộ đủ 24 ảnh mỗi bên ở `D:\P5E-builds\qa-rc1\libshots`.

## 5. Cài thử trên điện thoại của chủ

Bản dev `4.24.1-dev.5`/code318 (commit `8ff63926`, cùng mã) đã cài lên điện thoại `15e84958` với sao lưu trước/sau trùng SHA-256, số đếm trùng, buffer crash 0 của app; chủ xem Thư viện và báo "đã ổn, phát hành". Bản phát hành 4.24.1/code319 **chưa** cài lên điện thoại (cần chủ gõ "duyệt cài" riêng).

## 6. Bằng chứng giữ từ 4.24.0 (không đo lại)

Hiệu năng (Macrobenchmark `coldStartup`, trace Perfetto của RC, A/B với v4.21.0), instrumented 314 test trên emulator và video điện thoại là của `v4.24.0` (`artifacts/qa/v4.24.0-rc1/`). Thay đổi 4.24.1 chỉ ở hàng thẻ Thư viện nên không đo lại; payload ghi rõ các tệp này thuộc 4.24.0.

## 7. NOT_MEASURED

- Hiệu năng khởi động của chính 4.24.1 (dùng số của 4.24.0) và instrumented trên APK 4.24.1.
- Hình dạng hàng thẻ trên điện thoại thật bằng ảnh chụp (chủ đã xem bằng mắt; không có ảnh lưu).
- Thanh cuộn hệ thống vẽ đè viền phải của thẻ ở một số trang: không đổi.
- APK phát hành 4.24.1/code319 trên điện thoại thật; APK vẫn debuggable.
