# v4.23 — bản dev `4.23-dev.1` (code 276): L1 tab RAW + L2 chọn nhiều, QA trên emulator-5554, provider giả

Ngày 2026-10-18, nhánh `feature/v4.23` (từ `feature/v4.22` `c8408207`).
- Yêu cầu: `docs/V423_TRANSLATE_LIBRARY_WORK_REQUEST.md`, giai đoạn L1 và L2.
- Không gọi provider (USD 0). Test thiết bị chỉ trên `emulator-5554`. Điện thoại `15e84958` không được chạm (lần này không kết nối).
- Không tag, không merge, không push.
- Đây không phải bản phát hành. Chưa có L3 (bộ chương) và L4 (dịch theo bộ chương).

## 1. Bản dev và nguồn

| Mục | Giá trị |
|---|---|
| Bản dev | `4.23-dev.1` / code 276, event `build-20261018-164751`, source `3848ebeb8c31ece4fe6248286f78f500d0b26bfe` |
| APK | `D:\App Translate Books\artifacts\builds\v4.23-dev.1\build-20261018-164751\TranslateBooks-v4.23-dev.1-code276.apk`<br>SHA-256 `22B24F0C6BC8CF32258DF7D9CA70B3AFA796EFD0BE564F41D3EB53114753799E`, chứng chỉ `47F31389…EF3C155`<br>Mirror `backup\builds\…` (cùng nội dung, do script tạo) |
| Source ZIP | `56CA207C…D4D67`, trùng từng byte ZIP `git archive` của `3848ebeb` dùng cho hồi quy (mục 3) |
| APK test 1 | `v423-dev1-test-20261018`, `6E06A1C3…D91DBE`, từ `3848ebeb`. Chạy QA lần đầu; 4 kiểm tra phiên bản DB của hai lớp Biên tập hỏng (mục 4) |
| APK test 2 | `v423-dev1-test2-20261018`, `37552DE3…127928`, từ `c028569b` (chỉ khác `3848ebeb` ở 2 tệp androidTest + snapshot, mã sản xuất giữ nguyên). Dùng cho mọi kết quả còn lại |
| Cài đặt | `scripts/install-validated.ps1 -Serial emulator-5554`: `-CheckOnly` đạt rồi mới cài; đọc lại code 276, signer `abebea4b`. Trước đó emulator có `4.22-dev.3` / 275 |

Script build chạy bằng `cmd /c powershell -File …` vì `*>` trong PowerShell 5.1 biến dòng "Note: … deprecated API" của javac thành lỗi (lần chạy đầu thất bại, chưa tạo thư mục lưu trữ nào).

## 2. Thay đổi so với `4.22-dev.3`

- **L1 — tab RAW** (Library: Files, RAW, Glossaries, Pronouns, Sample):
  - "+ Nhập RAW" chọn nhiều TXT, sao chép vào app (không giữ quyền SAF); bỏ qua tệp trùng nội dung (hash đã chuẩn hóa như `jobs.input_hash`), tệp rỗng hoặc không phải văn bản; báo "Đã thêm n RAW, bỏ qua m (trùng/lỗi)".
  - Thẻ RAW: tên tệp gốc, "Chương N" hoặc "Không có số chương", số ký tự, mã hóa; nút Xem (20 dòng đầu), Đổi tên, Thay tệp, Xóa. Ô tìm theo tên.
  - Sắp theo số chương (9 < 13 < 100), RAW không có số xếp cuối. Số chương lấy từ số nguyên đầu tên tệp; `VOL08_…`, `Ex2`, `第3章` không tính.
  - Lưu: bảng `library_raw` (schema 30, migration `LibraryMigrationSpec`) + tệp `filesDir/library_raw/<id>.txt` ghi qua tệp tạm rồi đổi tên.
  - `FileUtil` đọc `tblraw://<id>` (đọc, tên hiển thị = tên tệp gốc, kích thước, nhận dạng, `validateReadable` không đòi quyền URI). Resume job đọc lại được RAW nội bộ.
  - "Xuất thư viện"/"Nhập thư viện" nâng lên định dạng 2: thêm `raws[]` và `raw/<nnn>_<tên>.txt`, nhập chỉ thêm, bỏ trùng theo hash, định dạng 1 vẫn nhập được.
  - Dọn tệp mồ côi khi mở app.
- **L2 — chọn nhiều** cho RAW, Glossaries, Pronouns:
  - Nút "Chọn" hoặc nhấn giữ một thẻ; ô tick, "n đã chọn · Chọn tất cả · Hủy"; Back thoát chế độ chọn trước.
  - Thanh dưới: Xóa (n), Xuất, Dùng (Glossary/Pronoun, đúng 1 mục), Tạo bộ chương (RAW).
  - Xóa: hộp xác nhận nêu số mục; mục có job dịch dở bị bỏ khỏi lần xóa kèm tên job; mục đang Active, nằm trong bộ chương hoặc tổ hợp Biên tập (chỉ đọc `editorial_combos`) cần xác nhận lần hai; ghi một lần cho mỗi kho, qua `ensureConfigMutable()` và khóa `CORRUPT`.
  - "Đã xóa n mục · Hoàn tác" 10 giây; hoàn tác ghi lại đúng mục với id cũ, đúng vị trí cũ, và chọn lại Active nếu đã bị bỏ chọn. Hết 10 giây mới xóa tệp RAW.
  - Xuất ZIP chỉ gồm mục đã tick.
- **Seam test** `TranslateTestSeam` (chỉ test, mặc định null): thay provider và thư mục output của dịch vụ để chạy dịch đầy đủ bằng provider giả. Không đụng Biên tập.

Giới hạn đã biết của bản này:
- Nút "Tạo bộ chương" ở thanh dưới của tab RAW **có hiện** nhưng chỉ báo "Bộ chương sẽ có ở bước sau" (L3 chưa có). Ảnh 18 có nút này.
- Hộp xóa hiện tiêu đề theo số mục đã tick ("Xóa 3 mục?") dù thân hộp nói chỉ xóa 2 (ảnh 19). Sẽ sửa ở bản sau.

## 3. Hồi quy trên archive sạch

`git archive` của `3848ebeb` (ZIP `56CA207C…`, trùng source ZIP của bản dev) giải nén vào `D:\P5E-builds\arch-v423-3848ebeb`, chạy `--offline --no-daemon`. Log `device/clean-archive-regression-3848ebeb.log`.

| Engine | App debug / release / benchmark | Lint | AndroidTest compile |
|---|---|---|---|
| 742 test, 1 SKIP, 0 FAIL | 533 / 533 / 533, mỗi biến thể 5 SKIP, 0 FAIL | 0 lỗi / 60 cảnh báo (4.22: 58) | PASS |

Test host mới (17): `LibraryRawTest` 10 (số chương, địa chỉ `tblraw://`, ghi tệp nguyên tử và dọn, kế hoạch nhập, migration 30, ZIP định dạng 2 khứ hồi / định dạng 1 / sửa nội dung / thiếu tệp / bỏ trùng), `LibrarySelectionTest` 7 (chọn, chọn tất cả, hủy, kế hoạch xóa với mọi loại tham chiếu, xóa và hoàn tác theo thứ tự, Back). Ba kiểm tra phiên bản trong `EditorialApiV26MigrationSpecTest`, `EditorialPairV27MigrationSpecTest`, `EditorialPairV29MigrationSpecTest` đổi `VER = 29` thành `VER = 30` (chỉ con số); `LibraryArchiveTest` đổi "phiên bản mới hơn" từ 2 thành `FORMAT_VERSION + 1`.

`git diff c8408207 -- editorial-engine 'app/src/main/java/com/ml/tblandroidtxt/Editorial*'` rỗng.

## 4. QA thiết bị (emulator-5554, provider giả)

Mỗi lớp chạy riêng bằng `am instrument -w -r`, tổng hợp bằng `scripts/qa/instrumentation_summary.py` (`device/method-summary.json`, log thô `device/raw/`). Các lớp restart chạy theo từng phương thức pha (seed → force-stop → verify → cleanup).

| Nhóm | Phát hiện | PASS | SKIP | FAIL |
|---|---:|---:|---:|---:|
| frozen (19 lớp) | 117 | 116 | 1 | 0 |
| chunk (6 lớp) | 27 | 21 | 6 | 0 |
| product / Translation (6 lớp) | 15 | 15 | 0 | 0 |
| v4.22: `EditorialForeground`, `LibraryS0`, `EditorialRefusedProposal` | 13 | 6 | 7 | 0 |
| **mới v4.23**: `V423LibraryInstrumentedTest` 2, `V423TranslateRaw` (đủ) 1 | 5 | 3 | 2 | 0 |
| restart: api, pair, foreground, S0, R, **RAW** (seed → force-stop → verify, cleanup nếu có) | 15 | 15 | 0 | 0 |
| **Tổng** | **192** | **176** | **16** | **0** (0 INCOMPLETE) |

SKIP là các test opt-in hoặc pha restart không được chọn, không tính là PASS.

Lần chạy đầu có 9 FAIL, đã sửa trong mã test, không phải lỗi sản phẩm (log `device/first-run-failed/`):
- `EditorialApiStoreInstrumentedTest` (3) và `EditorialPairStoreInstrumentedTest` (1) đòi `db.getVersion() == 29`; schema giờ là 30. Đổi 4 con số 29 thành 30 (`c028569b`), build lại APK test 2.
- 5 FAIL ở `restart-pair-*` do script chạy cả lớp `EditorialPairUiInstrumentedTest` với đối số pha; các test thường của lớp cần fixture riêng. Chạy theo từng phương thức pha như các lần QA trước thì PASS.

Test mới:
- **`V423LibraryInstrumentedTest.rawTab_…`**: nhập 5 RAW tổng hợp (+1 tệp trùng, +1 tệp trống bị bỏ qua) qua cùng `Intent` mà bộ chọn trả về; thứ tự 9, 13, 25, 100, không số; `FileUtil` đọc, tên, kích thước, nhận dạng, `validateReadable` qua `tblraw://` (không cần quyền SAF) khớp văn bản đã chuẩn hóa (CRLF → LF) và hash; tab RAW hiện "Chương 25" / "Không có số chương"; chọn 3 mục; job RUNNING giữ lại mục 100 và chỉ bỏ qua mục đó; xóa 2 → còn 3, tệp còn trong 10 giây; hoàn tác → đủ 5 với id cũ và thứ tự cũ; xóa lại và hết hạn → tệp bị xóa. RAW có sẵn của emulator không bị đổi.
- **`V423LibraryInstrumentedTest.glossaryAndPronounSelection_…`**: 4 glossary (1 Active) + 3 pronoun; chọn 3 glossary → xóa → hoàn tác → còn đủ đúng thứ tự; xóa glossary Active → kế hoạch đòi xác nhận lần hai, Active bị bỏ chọn, hoàn tác chọn lại; pronoun xóa/hoàn tác; Dùng đặt Active. Prefs `glossary_store`, `pronoun_store`, `settings` giống hệt trước khi chạy.
- **`V423TranslateRawInstrumentedTest`**: RAW nội bộ đi qua kế hoạch chunk (≥ 3 chunk, `displayName` = tên gốc, hash khớp) và toàn bộ dịch vụ với provider giả: job hoàn tất, mọi chunk được gửi đúng 1 lần, tệp output có chunk đầu và cuối. Pha restart: seed dịch xong chunk 1, chunk 2 chưa gửi, shell force-stop; verify resume job từ cùng RAW nội bộ, **không gửi lại chunk 1** (số lần gọi = số chunk − 1), output giữ chunk 1 và có chunk 2.. sau khi mở lại. Test tự xóa RAW, job và tệp output.

Dữ liệu app trên emulator (`device/app-data-hashes-before.txt` / `-after.txt`): `glossary_store.xml`, `pronoun_store.xml`, `settings.xml` giữ nguyên hash; `tbl_android_txt.db` đổi (các lớp test ghi rồi xóa dữ liệu, như các lần QA trước).

## 5. Ảnh chụp (6 ảnh trong `media/`, dữ liệu tổng hợp, đã xem từng ảnh)

- `17-raw-tab`: tab RAW, nút "+ Nhập RAW", ô tìm, nút "Chọn", "5 RAW", thẻ "9_v423-a.txt — Chương 9" và "013_… — Chương 13" với Xem / Đổi tên / Thay tệp / Xóa.
- `18-raw-selection`: chế độ chọn, "3 đã chọn · Chọn tất cả · Hủy", ô tick, thanh dưới "Xóa (3) · Xuất · Tạo bộ chương".
- `19-raw-delete-dialog`: hộp xóa "Xóa 2 mục khỏi thư viện… còn 10 giây để Hoàn tác", dưới đó "Bỏ qua 1 mục vì còn job dịch dở: 100_v423-d.txt — job #270". Tiêu đề hộp vẫn ghi "Xóa 3 mục?".
- `20-raw-undo-bar`: sau khi xóa, "3 RAW" còn lại và thanh "Đã xóa 2 mục · Hoàn tác".
- `21-glossary-delete-dialog`: chọn 3 glossary (một cái ACTIVE), hộp "Xóa 3 mục?".
- `22-glossary-undo-bar`: còn glossary 3, "Active glossary: —" (Active bị bỏ vì đã xóa), thanh "Đã xóa 3 mục · Hoàn tác".

Ảnh chụp bằng `adb exec-out screencap` tại các điểm dừng của test (`-e v423_media run`); log instrumentation của lượt chụp nằm trong `media/`.

## 6. Giới hạn và quan sát cho owner

- Chỉ chạy trên emulator với provider giả. NOT_MEASURED cho tới khi owner thử: bộ chọn tệp hệ thống (test đưa URI tệp trực tiếp), hiệu năng nhập hàng chục RAW lớn, thông báo foreground trên điện thoại.
- Hộp xác nhận lần hai (mục Active / bộ chương / Biên tập) được kiểm bằng kế hoạch xóa, chưa chụp ảnh riêng.
- Chưa có L3 (tạo bộ chương, bảng xác nhận xanh/vàng/đỏ) và L4 (dịch theo bộ chương); nút "Tạo bộ chương" chưa làm gì.
- 2 cảnh báo lint mới so với 4.22 (60 so với 58), 0 lỗi.
- Báo cáo job vẫn có thể in nguyên `settingsJson` khi không phân tích được (để lại như đã ghi ở 4.22).

## 7. Bước tiếp theo

Chờ owner duyệt để làm L3 và L4 (xong thì build `4.23-dev.2`, QA lại, rồi mới tới phiếu thử D1–D7). Cài lên điện thoại chỉ khi owner duyệt riêng.
