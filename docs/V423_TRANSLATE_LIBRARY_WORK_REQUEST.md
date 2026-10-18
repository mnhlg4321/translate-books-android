# v4.23 — Yêu cầu làm việc: Library RAW, chọn hàng loạt, dịch nhiều chương theo bộ chương

**Owner yêu cầu 2026-10-18:** ba tính năng sau phục vụ luồng **Dịch**, và **không động vào Biên tập**.

| # | Owner muốn |
|---|---|
| Y1 | Library có tab RAW để nhập hàng loạt RAW, giống Glossary/Pronoun |
| Y2 | Chọn nhiều mục trong Library (như màn "Select items" của ChatGPT) để xóa hàng loạt hoặc làm thao tác khác |
| Y3 | Tạo sẵn hàng loạt tổ hợp RAW–Glossary–Pronoun theo chương, rồi chọn ở màn Dịch. Không phải chọn lại RAW, Glossary, Pronoun cho từng chương |

**Quyết định sản phẩm phục vụ:** owner dịch nhiều chương liền một lượt; mỗi chương dùng đúng Glossary/Pronoun riêng của nó (ví dụ `025_RAW_…txt` + `025_Glossary_…csv` + `025_Pronoun_…csv`).

**Tài liệu nền** (đọc trước khi làm): `docs/V423_LIBRARY_BATCH_PROPOSAL.md`. Đề xuất này đã qua agent phản biện đối chiếu với mã; file:dòng ở mục 2–3 của đề xuất là bằng chứng. Phần S0 (an toàn dữ liệu thư viện, Xuất/Nhập thư viện) **đã có** trên `feature/v4.22` từ `f33cdb3e` và được dùng lại ở đây.

## 1. Nhánh, nơi làm, quyền

- **Nhánh mới `feature/v4.23`**, tạo từ commit của `feature/v4.22` chứa tài liệu này (gọi là `BASE`; ghi hash vào checklist bước 04), worktree mới `D:\P5E-builds\wt-v4.23`. Tạo bằng `git worktree add -b feature/v4.23 D:\P5E-builds\wt-v4.23 BASE`.
  - Chép `local.properties` từ worktree v4.22.
  - Tạo junction `artifacts\builds`, `artifacts\test-builds`, `backup` tới `D:\App Translate Books\…`, như v4.22 F1.
  - **Không** làm trong `D:\App Translate Books`. **Không** đổi `feature/v4.22`.
- Mở checklist `release_checklists/v4.23.md` từ `release_checklists/TEMPLATE.md` một lần. Bước 01–04 ghi bằng chứng ngay.
- Commit theo `type(scope): summary`. Hook yêu cầu `WORKSPACE_SNAPSHOT.md` cập nhật trong 30 phút / 10 commit. Snapshot và `BUILD_STATE.md` trên nhánh v4.23 ghi trạng thái của v4.23.
- **Không gọi provider.** Mọi kiểm tra dùng provider giả, USD 0.
- Test thiết bị chỉ trên `emulator-5554`. Điện thoại `15e84958` chỉ được đọc. **Cài lên điện thoại cần owner duyệt riêng cho từng lần.**
- Push `feature/v4.23` sau khi quét bí mật / chữ sách. Không tag, không merge cho tới khi owner duyệt phát hành.
- Quan hệ với 4.22: 4.22 (Biên tập) đang chờ owner thử trên điện thoại (F5/F6 của `docs/V422_DEVICE_TRIAL_RELEASE_WORK_REQUEST.md`). Nhánh v4.23 chứa mã 4.22 chưa phát hành. Nếu 4.23 phát hành trước 4.22, ghi chú phát hành 4.23 phải nêu cả phần 4.22; owner quyết thứ tự phát hành ở bước phát hành.

## 2. Ranh giới "không động vào Biên tập"

**Không sửa** các tệp sau:
- module `editorial-engine/`;
- mọi tệp `app/src/main/java/com/ml/tblandroidtxt/Editorial*.java`, `EditorialPair*`, `EditorialApi*`;
- `SettingsPageFactory` phần "Biên tập", `JobsMainPageFactory` phần thẻ Biên tập;
- test `Editorial*`.

**Được phép:**
- **đọc** bảng `editorial_combos` (cột `glossary_id`, `pronoun_id`) qua một truy vấn mới nằm ngoài lớp Editorial, chỉ để cảnh báo khi xóa Glossary/Pronoun đang được tổ hợp Biên tập dùng;
- sửa `GlossaryStore`, `PronounStore`, `LibraryArchive`, `MainActivity` (phần Library/Dịch), `TranslatorService`, `PreparationCoordinator`, `FileUtil`, `TranslationRepository` (bảng mới), các `*PageFactory` của Library và Dịch.

**Hợp đồng Biên tập `CP-IMPL-21` giữ nguyên.** Kiểm tra cuối mỗi giai đoạn: `git diff BASE -- editorial-engine 'app/src/main/java/com/ml/tblandroidtxt/Editorial*'` phải rỗng.

## 3. Thứ tự làm và tiêu chí từng giai đoạn

Mỗi giai đoạn kết thúc bằng:
- test host xanh;
- commit;
- cập nhật snapshot;
- kiểm ranh giới ở mục 2.

Build dev và QA emulator chạy sau L2 và sau L4.

### L1 — Tab RAW trong Library (Y1)

**Lưu trữ** (schema `VER` 29 → 30, migration mới trong `TranslationRepository.onUpgrade`, có test migration theo mẫu `EditorialPairV29MigrationSpecTest`):
- Bảng `library_raw`: `id` TEXT PK, `name`, `original_name`, `chapter_no` INTEGER (NULL nếu không tách được), `chars` INTEGER, `input_hash` TEXT (hash **đã chuẩn hóa**, đúng cách `PreparationCoordinator` tính cho `jobs.input_hash`), `encoding`, `created_at`, `updated_at`.
- Nội dung lưu thành tệp `filesDir/library_raw/<id>.txt`, ghi tệp tạm rồi đổi tên. Không lưu RAW trong SharedPreferences.
- Loại thư mục `library_raw` khỏi Auto Backup, vì giới hạn 25 MB. Hiện `allowBackup="true"` không có quy tắc nào.
- Bổ sung "Xuất thư viện" (`LibraryArchive`) để gồm RAW:
  - nâng `formatVersion` 2, mục `raws[]` + tệp `raw/<nnn>_<tên>.txt`;
  - nhập vẫn chỉ thêm, giữ id, bỏ trùng theo `input_hash`;
  - bản xuất định dạng 1 vẫn nhập được.

**Đọc RAW nội bộ:**
- `FileUtil` nhận một URI nội bộ dạng `tblraw://<id>`.
- Các hàm `readText`, `displayName` (trả `original_name`), `validateReadable` (không đòi quyền URI lưu bền) và mọi chỗ mở input khác phải qua `FileUtil`. Kiểm bằng `grep openInputStream`; các chỗ hiện có là `FileUtil:102,117,200,325`.
- Resume job đọc lại `jobs.input_uri` và so hash (`TranslatorService:441`), nên phải hoạt động với `tblraw://`.

**Giao diện:**
- Thanh tab Library thêm `RAW` (thứ tự Files, RAW, Glossaries, Pronouns, Sample). Thanh đã cuộn ngang được.
- Nút "+ Nhập RAW" chọn nhiều TXT (`ACTION_OPEN_DOCUMENT` + `EXTRA_ALLOW_MULTIPLE`):
  - đọc → chép vào app → bỏ qua tệp trùng `input_hash` và tệp rỗng hoặc không phải văn bản (dùng `AppValidator.validateSourceText`);
  - báo "Đã thêm n RAW, bỏ qua m (trùng/lỗi)".
- Thẻ RAW:
  - tên tệp gốc;
  - "Chương 25" (hoặc "Không có số chương"), số ký tự;
  - nút Xem (20 dòng đầu), Đổi tên, Thay tệp, Xóa.
- Sắp xếp theo số chương dạng số (9 < 13 < 100); mục không có số xếp cuối. Có ô tìm theo tên.

**Tách số chương** (hàm thuần, test bảng):
- số nguyên **đầu tên tệp**: `025_RAW_…` → 25, `25_…` → 25, `025-…` → 25;
- không lấy số trong `VOL08`, `Ex2`, `第3章` ở giữa tên;
- không có số đầu tên → NULL.

**Xóa RAW:**
- RAW đang được dùng bởi job dịch chưa xong (`jobs.input_uri = tblraw://<id>` và trạng thái khác done) → chặn và nêu tên job.
- RAW đang nằm trong bộ chương (L3) → cảnh báo và cần xác nhận lần hai.

**Test:**
- host: tách số chương, chỉ mục và tệp ghi nguyên tử, bỏ trùng, `FileUtil` với `tblraw://`, migration 29→30, `LibraryArchive` định dạng 2 khứ hồi và nhập bản định dạng 1;
- thiết bị: nhập 5 RAW tổng hợp (tệp trong cache, như `V415MultiProfileImportInstrumentedTest`), sắp xếp, xóa, dịch 1 RAW nội bộ bằng provider giả rồi resume sau force-stop.

### L2 — Chọn hàng loạt trong Library (Y2)

**Thành phần dùng chung** (ví dụ `LibrarySelectionController` + `LibrarySelectionBar`) cho các danh sách RAW, Glossaries, Pronouns (sau thêm Bộ chương):
- vào chế độ chọn: nút "Chọn" ở đầu danh sách, hoặc nhấn giữ một thẻ;
- trong chế độ chọn: ô tròn trên mỗi thẻ; bấm thẻ thì đổi trạng thái chọn; thanh trên "n đã chọn · Chọn tất cả · Hủy"; thanh dưới là các hành động;
- nút Back thoát chế độ chọn trước, không rời trang.

**Hành động:**
- **Xóa**:
  - hộp xác nhận nêu số mục;
  - liệt kê riêng mục đang Active, mục đang được bộ chương dùng, mục đang được tổ hợp Biên tập dùng (chỉ đọc `editorial_combos`) và RAW có job dịch dở;
  - mục có job dở bị bỏ khỏi lần xóa; các mục còn lại cần xác nhận lần hai;
  - qua `ensureConfigMutable()`; **ghi một lần** cho mỗi store bằng `commit()`, tôn trọng khóa `CORRUPT` của S0.
- **Xuất**: ZIP các mục đã chọn, dùng lại `LibraryArchive` (cùng định dạng, chỉ gồm mục đã chọn).
- **Dùng** (khi chọn đúng 1 Glossary hoặc 1 Pronoun): đặt làm Active.
- **Tạo bộ chương** (chỉ ở RAW, có từ L3).

**Không xóa hẳn ngay:** sau khi xóa hiện "Đã xóa n mục · Hoàn tác" trong 10 giây. Hoàn tác ghi lại đúng danh sách cũ (giữ id). Hết 10 giây mới xóa tệp RAW.

**Test:**
- host: controller (chọn, chọn tất cả, hủy), kế hoạch xóa với các loại tham chiếu, hoàn tác;
- thiết bị: chọn 3 glossary → xóa → hoàn tác → còn đủ; xóa glossary đang Active → Active bị bỏ chọn và có cảnh báo.

**Sau L2:**
- build dev `4.23-dev.1` (`scripts/build-and-save.ps1 -Series 4.23-dev -MinimumVersionCode 276`; code phải lớn hơn mọi code đã dùng, hiện là 275);
- QA emulator đủ nhóm như `artifacts/qa/v4.22-dev3-20261018/QA_REPORT.md` (frozen, chunk, product, restart) + test mới;
- ảnh tab RAW, chế độ chọn, hộp xóa, hoàn tác (xem từng ảnh);
- dừng, gửi owner đường dẫn APK + ảnh.

### L3 — Bộ chương (Y3, phần dữ liệu và giao diện Library)

**Lưu trữ** (cùng migration `VER` 30 nếu L1 chưa phát hành bản nào, nếu không thì 31):
- `chapter_sets` (`id`, `name`, `created_at`, `updated_at`);
- `chapter_set_rows` (`set_id`, `ordinal`, `chapter_no`, `raw_id`, `glossary_id`, `pronoun_id`; Glossary/Pronoun được phép rỗng = không dùng).

**Tạo bộ:**
- Tab RAW → chọn nhiều → "Tạo bộ chương".
- App **gợi ý** Glossary/Pronoun có cùng số chương ở đầu tên profile, tách bằng cùng hàm với RAW.
- Màn bảng xác nhận, mỗi dòng một chương:
  - **xanh**: đủ, đúng một ứng viên cho mỗi loại;
  - **vàng**: thiếu Glossary hoặc Pronoun;
  - **đỏ**: nhiều ứng viên, RAW không có số chương, hoặc hai RAW trùng số.
- Mỗi dòng có danh sách chọn Glossary/Pronoun (gồm "Không dùng"). Dòng đỏ phải được sửa trước khi Lưu.
- Đặt tên bộ (mặc định là phần chung của tên tệp RAW) → Lưu.
- **Không ghép âm thầm:** app chỉ gợi ý; owner xác nhận; về sau không tự ghép lại.

**Quản lý:**
- Library thêm tab "Bộ chương": danh sách bộ (tên, số chương, số dòng thiếu); mở bộ để sửa bảng; xóa bộ (không xóa RAW/Glossary/Pronoun).
- Hỗ trợ chọn nhiều của L2.

**Lưu tên tệp gốc và số chương riêng**, để đổi tên profile không làm hỏng việc ghép.

**Test:**
- host: gợi ý ghép với bảng ca (`025`/`25`, `VOL08_013`, `Ex2`, trùng số, không số, nhiều ứng viên), lưu/đọc bộ, xóa profile đang nằm trong bộ (dòng thành "thiếu", không vỡ);
- thiết bị: tạo bộ 3 chương từ RAW + Glossary + Pronoun tổng hợp.

### L4 — Dịch theo bộ chương (Y3, lõi dịch)

**Màn Dịch, bước 1:**
- Chọn nguồn "Tệp TXT" (như hiện nay) hoặc "Bộ chương".
- Khi chọn bộ: danh sách chương có ô tick. Mặc định tick các chương chưa có bản dịch xong; "chưa có" tính bằng `jobs.input_hash` = `library_raw.input_hash` và trạng thái done.
- Mỗi dòng hiện Glossary/Pronoun của chương.
- Ô "Locks" và Prompt preview cho chọn chương để xem đúng bộ của chương đó.

**Ảnh chụp lúc Start** (theo phản biện, thay cho đề xuất nháp):
- Khi bấm Start, ghi tệp `filesDir/translate_sessions/<startSessionId>.json`. Với mỗi `ordinal` gồm: `rawId`, `glossaryId`, `glossaryText`, `glossaryHash`, `pronounId`, `pronounText`, `pronounHash`.
- Intent chỉ mang `startSessionId` và cờ chế độ bộ chương. Không nhét nội dung vào intent (giới hạn kích thước).
- `TranslatorService.runSinglePreparedJob`:
  - áp ảnh chụp của `input.ordinal` lên **bản sao** `AppSettings`;
  - `jobs.settings` của job đó mang đúng Glossary/Pronoun của chương.
  - Resume/retry đã đọc `job.settingsJson` (`TranslatorService:435,523`), nên không cần đổi.
- **Cấm** `SettingsStore.save` với bản sao theo chương: cấu hình chung và profile Active không được đổi sau lượt chạy (`MainActivity:1526`, `TranslatorService:337` đang lưu settings).
- `PreparationCoordinator.selectionKey`: ở chế độ bộ chương phải gồm hash Glossary/Pronoun của từng input. Lý do: hai bộ dùng chung RAW không được dùng chung kế hoạch (`PreparationCoordinator:78,84,181-195`).
- `estimateKey` cũng tính theo bộ.

**Chi phí:**
- Ước tính theo từng chương và tổng, hiện trước khi chạy.
- Trần chi phí giữ **theo từng chương** như hiện nay (`resetMetrics`). Không thêm trần cho cả hàng đợi trong 4.23.

**Khi một chương lỗi** (mặc định đề xuất; owner có thể đổi trước khi làm L4):
- Lỗi riêng của chương thì đánh dấu job lỗi và **chạy tiếp** chương sau. Gồm: kiểm toàn vẹn thất bại (`TranslatorService:684`), chunk không chia nhỏ được (`:669`).
- **Dừng cả hàng đợi** khi: chạm trần chi phí, không rõ lời gọi đã tới provider chưa (`:631`), lỗi xác thực / hạn mức / mạng.
- Cuối lượt hiện tóm tắt "x xong · y lỗi (liệt kê) · z chưa chạy".

**Output:** mỗi chương một tệp trong output folder theo quy ước tên hiện có. Tên lấy từ `original_name` của RAW, không lấy id.

**Thẻ tiến độ/Jobs:** giữ thẻ dịch hiện có; thêm "Chương i/n · tên chương".

**Seam cho test:** luồng dịch đầy đủ chưa có chỗ thay provider ở mức service (hiện chỉ `Hotfix441InstrumentedTest` dùng `OpenAICompatibleClient.ChatResult` giả ở mức engine). Thêm một seam chỉ dùng trong test, cùng kiểu `EditorialApiUiController.providerOverride` nhưng là lớp mới phía Dịch, không đụng Editorial, để chạy service với provider giả.

**Test:**
- host:
  - 3 input với 3 glossary khác nhau: kiểm prompt từng chương và `jobs.settings`;
  - kế hoạch dùng lại giữa hai bộ cùng RAW không lẫn glossary;
  - `SettingsStore` và profile Active không đổi;
  - phân loại lỗi dừng / chạy tiếp;
  - tên output.
- thiết bị (provider giả):
  - dịch bộ 3 chương: mỗi prompt có đúng thuật ngữ của chương;
  - force-stop giữa chương 2, chạy tiếp thì dùng ảnh chụp chương 2;
  - lỗi toàn vẹn ở chương 2 thì chương 3 vẫn chạy;
  - lỗi xác thực thì dừng.

**Sau L4:**
- build `4.23-dev.2`; QA emulator đủ nhóm + test mới;
- ảnh: màn Dịch chọn bộ, bảng ghép, tiến độ chương i/n, tóm tắt cuối;
- dừng, gửi owner, chờ duyệt cài lên điện thoại và phiếu thử.

### L5 — Phiếu thử cho owner (sau khi owner duyệt cài)

| # | Owner làm | Đạt khi |
|---|---|---|
| D1 | Library → RAW → nhập 10–20 RAW của một tập | đủ, đúng số chương, sắp xếp đúng |
| D2 | Chọn nhiều 3 Glossary → Xóa → Hoàn tác | còn đủ |
| D3 | Tạo bộ chương từ các RAW vừa nhập | xanh/vàng/đỏ đúng; sửa được dòng đỏ |
| D4 | Màn Dịch → Bộ chương → tick 3 chương → Start | mỗi chương ra một tệp; Glossary/Pronoun đúng chương (xem Prompt preview từng chương) |
| D5 | Thoát app giữa chương 2, mở lại, chạy tiếp | không dịch lại chương 1; chương 2 tiếp đúng chỗ |
| D6 | Sau lượt chạy | Glossary/Pronoun Active không đổi |
| D7 | Xuất thư viện | ZIP có RAW, Glossary, Pronoun; không có API key |

Chi phí D4/D5 do owner quyết (khóa của owner). Mục owner không thử ghi NOT_MEASURED.

## 4. Không làm

- Không sửa Biên tập (mục 2).
- Không gọi provider từ máy phát triển. Không chạy test trên điện thoại. Không cài khi chưa có duyệt riêng.
- Không trần chi phí cho cả hàng đợi.
- Không tự ghép lại sau khi owner đã lưu bộ.
- Không đổi định dạng Glossary/Pronoun đã lưu.
- Không tag/merge.
- Không thêm bộ kiểm hay cổng đo mới.

## 5. Báo cáo cho owner sau mỗi lần dừng

Mỗi lần dừng (sau L2, sau L4) gồm:
- commit đã làm;
- kết quả hồi quy archive sạch (engine, app 3 biến thể, lint, androidTest compile);
- QA thiết bị PASS/SKIP/FAIL;
- đường dẫn APK + SHA-256 và ảnh;
- giới hạn còn lại;
- **một** câu hỏi duyệt.
