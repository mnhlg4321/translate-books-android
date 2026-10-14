# v4.22 — bản dev `4.22-dev.2` (code 273): QA trên emulator-5554, provider giả

Ngày 2026-10-14, nhánh `feature/v4.22`. Yêu cầu: `docs/V422_WORK_REQUEST.md` bước 1–6 (U1, U2, U3, P, G, bản dev, QA, ảnh) và `docs/V422_Q1_SPEAKER_DESIGN_REQUEST.md` (Q1 offline). Không gọi provider (USD 0), không cài lên điện thoại `15e84958` (không nối máy trong lượt này), không đổi schema DB, không tag/merge. Đây **không** phải bản phát hành; giao diện cần owner bấm thử và chấp nhận.

## 1. Bản dev và nguồn

| Mục | Giá trị |
|---|---|
| Bản dev (hiện hành) | `4.22-dev.2` / code 273, event `build-20261014-164611`, source `1ba67d1bebd85db475d23ecda6ba2204d200e879` |
| APK | `D:\App Translate Books\artifacts\builds\v4.22-dev.2\build-20261014-164611\TranslateBooks-v4.22-dev.2-code273.apk`, SHA-256 `5BA07CD91021FF8B1CED45877B6910CC70AAF618305DAF47E20FD0F8004F00D6`, chứng chỉ `47F31389…EF3C155`; mirror `backup\builds\v4.22-dev.2\build-20261014-164611\` giống từng byte (5 file) |
| Source ZIP | `EA1B2208…B09AC5` (ZIP của wrapper) |
| APK test | `…\artifacts\test-builds\v4.22-dev.2\v422-dev2-test-20261014\app-debug-androidTest.apk`, SHA-256 `AFA78D0A…C4FF35`, ghim đúng APK sản xuất/source ZIP/chứng chỉ, mirror trong `backup\test-builds\` |
| Cài đặt | `scripts/install-validated.ps1` chỉ `emulator-5554` (`-CheckOnly` rồi cài; đọc lại version 273, signer `abebea4b`). Điện thoại không bị chạm |
| Bản bị thay | `4.22-dev.1` / code 272 (`build-20261014-162415`, source `d8388e88`): giữ nguyên ở hai nơi, **bị thay** vì ảnh chụp của nó lộ lỗi P (mục 4) |

## 2. Các bước đã làm

- **U1–P** (commit `9390dc24`…`56789cac`, phần trước của phiên làm việc): màn tạo lượt 4 file mặc định + ô tick 2 file + cảnh báo thiếu file; kết quả dễ đọc; mục Biên tập trong Settings; dịch vụ foreground + thẻ job.
- **G** (`857a6a1d`): ba quy tắc nhỏ trong `FixPointFinder` (nguồn một chữ Hán nằm trong từ dài hơn; chữ của hàng pronoun nằm trong ngoặc thoại; nhân vật đã được nhắc bằng đại từ ngôi ba và dòng không có tên riêng khác giữa câu). Đo bằng finder thật trên 9 chương (8 JAKUAKU + Kumo 013): **21 → 18 điểm GLOSSARY**; ba điểm bị bỏ đúng là ba điểm giả đã biết của Kumo 013; **0 điểm** mà FINAL của owner từng thêm thuật ngữ bị bỏ (0/18 ở JAKUAKU). Lần thử đầu bỏ 13 điểm kể cả 8 điểm owner đã sửa (quy tắc hàng pronoun áp cả lời kể; quy tắc đại từ không phân biệt người); hai quy tắc được siết lại rồi đo lại; số liệu cuối ở trên. Engine thêm 10 test (3 quy tắc + đối chứng vẫn tạo điểm).
- **Q1** (`d3798e09`): thiết kế offline + sidecar tắt mặc định, không nối vào app. Chạy lại độc lập số liệu của phiên trước: giống từng con số (36 assertion PASS; Kumo 16 → 3 điểm ADDRESS, JAKUAKU 58 → 46; bắt 1/2 lỗi thật Kumo). Kết luận giữ nguyên: **không đề xuất lượt đo trả phí**.

## 3. Hồi quy trên archive sạch (`git archive`)

| Commit | Engine | App debug / release / benchmark | Lint | AndroidTest compile |
|---|---|---|---|---|
| `d3798e09` (U1–P, G, Q1) | 737 test, 1 SKIP, 0 FAIL | 489 / 489 / 489, mỗi biến thể 5 SKIP, 0 FAIL | 0 lỗi / 58 cảnh báo | PASS |
| `1ba67d1b` (thêm sửa Jobs) — **commit của APK dev.2** | 737, 1 SKIP, 0 FAIL | 489 / 489 / 489, 5 SKIP, 0 FAIL | 0 lỗi / 58 cảnh báo | PASS |

ZIP archive `1ba67d1b`: `EA1B2208…B09AC5` (trùng ZIP của wrapper). SKIP là test opt-in cần biến môi trường, không tính là PASS.

## 4. QA thiết bị (emulator-5554, provider giả)

Nhóm giống RC 4.21.0 cộng test mới; tổng hợp bằng `scripts/qa/instrumentation_summary.py` (`device/method-summary.json`, log thô trong `device/raw/`):

| Nhóm | PASS | SKIP | FAIL |
|---|---:|---:|---:|
| frozen (19 lớp) | 116 | 1 | 0 |
| chunk (6 lớp) | 21 | 6 | 0 |
| product / Translation (6 lớp) | 15 | 0 | 0 |
| restart seed → force-stop → verify → cleanup (api, pair, **foreground mới**) | 9 | 0 | 0 |
| foreground mới (`EditorialForeground`: hoàn tất, hủy rồi chạy tiếp chỉ đoạn còn lại, **thẻ trên tab Jobs**) | 3 | 3 (pha restart) | 0 |
| **Tổng** | **164** | **10** | **0** (0 INCOMPLETE) |

**FAILED_REPAIRING, giữ nguyên bằng chứng** (`superseded-dev1/device/failed-attempt-1/`, `…/attempt-2-test2apk/`):
1. Lần chạy đầu trên `4.22-dev.1`: 3 test UI cũ hỏng vì còn dùng nhãn trước 4.22 (`Đổi`, `Bỏ`) và mặc định không-Glossary/Pronoun (`EditorialApiBienTapUi` 1, `EditorialPairUi` 2). Sửa **chỉ mã test**: dùng nút "Chọn Glossary từ thư viện", ô tick 2 file, đặt lại lựa chọn nhớ trong SharedPreferences; sau đó một lỗi nữa của chính helper (ô tick không có click listener nên `performClick` trả false) và phát hiện tôi chạy nhầm cả lớp thay vì từng pha restart; đều sửa trong test/lệnh chạy. Chạy lại đủ nhóm: dev.1 163 PASS / 10 SKIP / 0 FAIL, dev.2 164 PASS / 10 SKIP / 0 FAIL.
2. **Lỗi sản xuất thật do ảnh chụp lộ ra** (không có test nào bắt): thẻ Biên tập không hiện ở tab Jobs trên `4.22-dev.1`, vì thẻ được gắn vào `JobsPageFactory` (trang cũ) còn tab Jobs do `JobsMainPageFactory` dựng. Sửa ở mã sản xuất (`1ba67d1b`, một dòng), thêm test thiết bị `jobsTabShowsTheEditorialRunCard`, build lại `4.22-dev.2`, chạy lại hồi quy sạch, QA thiết bị và ảnh. Log text của trang Jobs trong lần quay cuối có thẻ: tên tổ hợp, "Đang biên tập · Đoạn 0/3 · USD 0", "Hủy sau đoạn hiện tại", "Mở kết quả".

Dữ liệu thử: glossary/pronoun tổng hợp, không có văn bản sách; test tự dọn (store glossary/pronoun/combo/job).

## 5. Ảnh chụp (11, `media/screenshots/`, dữ liệu tổng hợp, đã xem từng ảnh)

`01-setup-four-files-default` · `02-setup-two-files-ticked` · `03-setup-job-source-glossary-only` · `04-warning-missing-pronoun` · `05-setup-four-files-complete` · `06-plan-verdict` · `07-progress-card-on-the-editorial-screen` (đang chạy, provider giả bị giữ ở lời gọi đầu) · `08-jobs-tab-card` · `09-result-summary` · `10-result-changed-lines` · `11-settings-editorial`. Ảnh lần quay đầu (có lỗi thẻ Jobs) giữ ở `superseded-dev1/media/` làm chứng cứ.

## 6. Giới hạn và quan sát

- Chỉ emulator, provider giả; **chất lượng biên tập, chọn file bằng picker hệ thống, thông báo foreground trên màn hình khóa/Android 13+ (quyền thông báo) chưa đo (NOT_MEASURED)**; ảnh chứng minh bố cục và luồng, không chứng minh bản sửa đúng.
- Nhãn "Dòng n" trong "Dòng đã sửa" là số dòng **trong đoạn**, không phải số dòng trong chương; với dữ liệu tổng hợp ảnh 09 hiện "Dòng 2" cho dòng đầu của đoạn. Nguyên nhân chưa xác định (nghi văn bản đoạn có dòng trống đầu; chưa kiểm). Chưa sửa; owner nên xem khi bấm thử.
- G không bỏ điểm khi dòng DRAFT có tên riêng khác giữa câu (chủ ý, để không mất điểm owner từng sửa); đo trên 9 chương, chưa đo ngoài các chương này.
- Release chưa thực hiện: hiệu năng (Macrobenchmark/Perfetto), RC `-ExactReleaseVersion`, PreTag, tag/merge là của bước 7, chờ owner.
