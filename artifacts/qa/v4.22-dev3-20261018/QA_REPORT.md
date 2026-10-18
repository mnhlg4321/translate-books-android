# v4.22 — bản dev `4.22-dev.3` (code 275): R + S0, QA trên emulator-5554, provider giả

Ngày 2026-10-18, nhánh `feature/v4.22`.
- Yêu cầu: `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md` F1–F4 và `docs/V422_S0_LIBRARY_SAFETY_WORK_REQUEST.md`.
- Không gọi provider (USD 0). Không đổi schema DB, không tag/merge.
- Điện thoại `15e84958` chỉ được đọc (`dumpsys`): vẫn là `4.22-dev.2` / code 273. **Chưa cài bản này lên điện thoại**; việc đó chờ owner duyệt riêng (F5).
- Đây không phải bản phát hành.

## 1. Bản dev và nguồn

| Mục | Giá trị |
|---|---|
| Bản dev | `4.22-dev.3` / code 275, event `build-20261018-161433`, source `4032ff3971686e3227ab31c12d1d7267e5e0502a` |
| APK | `D:\App Translate Books\artifacts\builds\v4.22-dev.3\build-20261018-161433\TranslateBooks-v4.22-dev.3-code275.apk`<br>SHA-256 `CC7F669076058D8AAA4843FDADE9D1F3C1021B4C793D67DC42DEEE8B7DC195CB`, chứng chỉ `47F31389…EF3C155`<br>Mirror `backup\builds\…` giống từng byte (5 file) |
| Source ZIP | `209E1985…99AF2AF`, trùng ZIP `git archive` dùng cho hồi quy F2 |
| APK test 1 | `v422-dev3-test-20261018`, `C8185337…AD276C0`, từ `4032ff39`. Dùng cho QA thiết bị ở mục 4 |
| APK test 2 | `v422-dev3-test2-20261018`, `3FDDEB11…7579E1`, từ `2317fb5b`<br>Chỉ khác ở mã androidTest: thêm điểm dừng chụp ảnh cho S0. Dùng cho ảnh ở mục 5<br>Cả hai APK test đều ghim đúng APK sản xuất, source ZIP và chứng chỉ |
| Cài đặt | `scripts/install-validated.ps1 -Serial emulator-5554`: chạy `-CheckOnly` (đạt) rồi mới cài<br>Đọc lại code 275, signer `abebea4b`. Trước đó emulator có code 274 |

Khi build, archive của worktree đã nối (junction) tới archive của owner (F1). Nhờ vậy bản này lấy được tên duy nhất `4.22-dev.3`.

## 2. Thay đổi so với bản trên điện thoại (`4.22-dev.2`)

- Sửa nhãn "Dòng n" (`2ee39476`).
- **R** (`db71bef5`, `CP-IMPL-21`):
  - Đề xuất của model bị chặn giờ được giữ lại, kèm nút "Áp dụng đề xuất".
  - Ô pronoun `self`/`call` nhận nhiều dạng, cách nhau bằng `/`.
  - Lượt Biên tập tạo trước `CP-IMPL-21` phải tạo lượt mới.
- **S0** (`f33cdb3e`):
  - Glossary được ghi đồng bộ.
  - Kho Glossary/Pronoun đọc lỗi thì được giữ nguyên và khóa ghi, kèm thẻ đỏ "Xuất dữ liệu gốc".
  - Nhập hàng loạt không đổi profile Active.
  - Thêm "Xuất thư viện" / "Nhập thư viện" ở Library > Files.
  - "Export profile" không còn ghi API key.
  - Thẻ glossary hiện số mục theo loại thay cho `— → —`.

## 3. Hồi quy trên archive sạch (F2)

`git archive` của `4032ff39` (ZIP `209E1985…`), chạy `--offline --no-daemon`. Log ở `D:\P5E-private\v422-host-evidence\s0-r-regression-4032ff39.log`.

| Engine | App debug / release / benchmark | Lint | AndroidTest compile |
|---|---|---|---|
| 742 test, 1 SKIP, 0 FAIL | 516 / 516 / 516, mỗi biến thể 5 SKIP, 0 FAIL | 0 lỗi / 58 cảnh báo | PASS |

Test host mới cho S0: `LibraryStoreGuardTest` 10, `LibraryArchiveTest` 9, `ProfileExportKeyTest` 3, `GlossaryCardMetaTest` 2. SKIP là các test opt-in, không tính là PASS.

## 4. QA thiết bị (emulator-5554, provider giả, APK test 1)

Mỗi lớp chạy riêng bằng `am instrument -w -r -s emulator-5554`. Tổng hợp bằng `scripts/qa/instrumentation_summary.py`, kết quả ở `device/method-summary.json`; log thô ở `device/raw/`.

| Nhóm | Phát hiện | PASS | SKIP | FAIL |
|---|---:|---:|---:|---:|
| frozen (19 lớp) | 117 | 116 | 1 | 0 |
| chunk (6 lớp) | 27 | 21 | 6 | 0 |
| product / Translation (6 lớp, có `V415MultiProfileImport` đã sửa theo S0.3) | 15 | 15 | 0 | 0 |
| restart: seed → force-stop → verify (→ cleanup) cho api, pair, foreground, **S0**, **R** | 13 | 13 | 0 | 0 |
| v4.22: `EditorialForeground` 6, **`LibraryS0` 3**, **`EditorialRefusedProposal` 4** (các pha restart SKIP khi chạy cả lớp) | 13 | 6 | 7 | 0 |
| **Tổng** | **185** | **171** | **14** | **0** (0 INCOMPLETE) |

Test mới:

- **`EditorialRefusedProposalInstrumentedTest` (F3)**, chạy trên SQLite thật và màn kết quả thật:
  - Mục "Đề xuất bị chặn — chờ bạn quyết" hiện câu đề xuất.
  - Bấm "Áp dụng đề xuất" thì đúng một dòng của bản xuất đổi, nút biến mất, trạng thái điểm là `OWNER_APPLIED` và nhật ký có `PROPOSAL_APPLIED id=1`.
  - Áp dụng lần hai bị từ chối.
  - Ô `mình/em` cho câu đó qua thẳng (`FIXED`), không còn là đề xuất bị chặn.
  - Sau force-stop, bản xuất vẫn giữ dòng đã áp dụng.
- **`LibraryS0InstrumentedTest` (S0.7)**:
  - Nhập 3 glossary + 2 pronoun: Active không đổi.
  - Xuất ZIP; thẻ hiện "4 glossary, 2 pronoun".
  - Xóa 1 glossary rồi nhập lại ZIP: glossary trở lại với **id cũ**, 5 mục báo "đã có", Active không đổi.
  - Ghi chuỗi hỏng vào kho: store báo `CORRUPT`, ghi bị chặn, chuỗi gốc được chép sang khóa `glossaries_json_corrupt_*`, trang Glossaries hiện thẻ đỏ.
  - Sau force-stop ngay khi vừa nhập, glossary vẫn còn.
  - Test tự khôi phục prefs `glossary_store`, `pronoun_store`, `settings`, `library_backup` và log runtime, rồi so khớp.

Dữ liệu app trên emulator (hash ở `device/app-data-hashes-before.txt` và `-after.txt`):
- `tbl_android_txt.db` và `runtime_state.xml` đổi. Đây là điều đã gặp ở các lần QA trước: các lớp test ghi rồi xóa combo/run/job.
- `glossary_store.xml` khác hash nhưng chỉ còn đúng 3 glossary có sẵn, không có mục thử nào. Lý do: pha restart S0 xóa mục thử bằng `GlossaryStore.delete`, nên danh sách được ghi lại. Không có bản trước để so nội dung từng byte (**UNKNOWN** ở mức byte).
- Có thêm `library_backup.xml` rỗng.

## 5. Ảnh chụp (16 ảnh trong `media/`, dữ liệu tổng hợp, đã xem từng ảnh)

- `01`–`11`: walkthrough 4.22 như dev.2. Ảnh 09/10 hiện nhãn đúng `Đoạn 1 · Dòng 1`.
- `12-refused-proposal-waiting`: bản nháp, đề xuất của model, lý do bị chặn (hàng R1, self=mình), gợi ý thêm dạng bằng `/`, nút "Áp dụng đề xuất".
- `13-refused-proposal-applied`: "Dòng đã sửa" có dòng Rina; điểm 1 ghi "Bạn đã áp dụng đề xuất của model"; tóm tắt còn "0 bị chặn".
- `14-library-backup-card`: thẻ "Sao lưu thư viện" với hai nút và dòng "Lần xuất gần nhất".
- `15-library-import-preview`: hộp xem trước "Sẽ thêm 1 glossary… Bỏ qua 5 đã có…".
- `16-library-locked-card`: thẻ đỏ "Dữ liệu Glossary đọc lỗi" và nút "Xuất dữ liệu gốc".

Lần quay đầu, ảnh 12–14 chụp phần đầu trang nên chưa thấy mục cần chụp. Script đã thêm bước cuộn rồi quay lại; ảnh trong thư mục là lần quay thứ hai. Log instrumentation của mỗi lần quay cũng nằm trong `media/`.

## 6. Giới hạn và quan sát cho owner

- Chỉ chạy trên emulator với provider giả. Những mục sau **NOT_MEASURED** cho tới khi owner thử (F6): chất lượng sửa, bộ chọn tệp hệ thống (test dùng URI tệp trực tiếp), thông báo foreground trên điện thoại.
- Thẻ "Sao lưu thư viện" nằm cuối tab Files, phải cuộn xuống mới thấy.
- Kết quả xuất/nhập thư viện hiện bằng toast. Thẻ kết quả chung của app nằm ở tab Dịch, nên đứng ở Library sẽ không thấy thẻ đó.
- Ảnh 12: khối "Dòng đã sửa" gộp các dòng đổi liền nhau thành một khối "Trước/Sau". Hành vi này có từ trước, không đổi.
- Báo cáo job có thể in nguyên `settingsJson` (có thể chứa API key) khi không phân tích được. Để lại cho 4.23.

## 7. Bước tiếp theo

F5: cài lên điện thoại `15e84958` **chỉ khi owner duyệt riêng**. Trước khi cài, sao lưu `run-as` từ dev.2 (bản debuggable) như lần 2026-10-14. Sau đó owner thử theo phiếu F6 (T1–T13).
