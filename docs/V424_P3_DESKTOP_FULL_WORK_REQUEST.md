# v4.24 P3 — App PC đầy đủ cho việc dịch (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** phương án A cho bản PC (owner duyệt 2026-10-27).
- P2 đã chứng minh app PC dịch thật được trên lõi chung: 2 chunk, $0,005693, đồng hồ không tính thời gian tạm dừng.
- P3 đưa app PC lên ngang bản điện thoại ở phần **Dịch**:
  - mang cài đặt và thư viện từ điện thoại sang;
  - dịch cả tập theo chương;
  - lịch sử job;
  - giao diện màu;
  - bộ cài.
- Biên tập vẫn ngoài phạm vi (đang đóng băng).

**Nơi làm:** nhánh `feature/v4.24`, worktree `D:\P5E-builds\wt-v4.24`, từ `5f07d9f3`. Không tag, push hay merge vào nhánh khác. **Không sửa tệp `Editorial*`**: chốt `git diff 5f07d9f3..HEAD -- '*Editorial*'` phải rỗng.

## 1. Mang dữ liệu từ điện thoại sang PC (và ngược lại)

Điện thoại đã có hai chức năng xuất:
- **hồ sơ cài đặt** (`SettingsStore.toExportJson`, `MainActivity.java:2680`), nhập lại bằng `fromImportedProfile` (`:2721`);
- **gói thư viện** `LibraryArchive` (đã ở lõi): tệp ZIP chứa Glossary, Pronoun và RAW, có manifest và mã toàn vẹn.

**Việc cần làm:**
1. Chuyển phần thuần của `toExportJson` và `fromImportedProfile` sang `SettingsJson` (lõi), theo mẫu S5. Hàm cũ trong `SettingsStore` chỉ còn một dòng gọi sang. **Không chuyển `toEnv`**, vì nó ghi `API_KEY=`.
2. **Cài đặt (PC) → "Nhập hồ sơ cài đặt từ điện thoại…"**:
   - chọn tệp JSON đã xuất từ điện thoại;
   - xem trước bảng "PC hiện tại / hồ sơ" **chỉ với các mục khác nhau**;
   - "Áp dụng" có "Hoàn tác", theo mẫu B3 của v4.23.

   Hồ sơ **không chứa API key**. Key trên PC giữ nguyên.
3. **Cài đặt (PC) → "Xuất hồ sơ cài đặt…"**: cùng định dạng, để mang ngược sang điện thoại.
4. **Thư viện (PC) → "Nhập gói thư viện…"**:
   - đọc ZIP bằng `LibraryArchive.read` + `plan`;
   - hiện số mục thêm / trùng / xung đột trước khi áp dụng;
   - Glossary và Pronoun vào `JsonFileKeyValueStore`; RAW vào thư viện RAW của PC (mục 2.1).
5. **Thư viện (PC) → "Xuất gói thư viện…"**: tạo ZIP đúng định dạng của điện thoại.

**Test:**
- xuất từ PC rồi nhập lại → giống nhau;
- một gói tự viết, dựng bằng `LibraryArchive.build`, được nhập đúng;
- một gói hỏng mã toàn vẹn bị từ chối.

**Thử thật (không tốn tiền):** owner xuất hồ sơ cài đặt và gói thư viện trên điện thoại rồi chép sang PC. App PC nhập được; số Glossary, Pronoun và RAW trên PC bằng số trên điện thoại. **Chỉ ghi số đếm**, không chép nội dung sách vào bằng chứng.

## 2. Dịch cả tập theo chương

### 2.1 Thư viện RAW trên PC

App PC lưu bản sao RAW trong thư mục dữ liệu, như `LibraryRawStore` của điện thoại: tên, số chương lấy từ đầu tên tệp, số ký tự, `input_hash`, encoding.
- Bảng `library_raw` đã có trong lược đồ v30.
- Đọc tệp qua `TextDecoding`.
- Phần thuần của `LibraryRawStore` (nếu có) tách sang lõi theo mẫu S5. Phần gắn `FileUtil`/`Context` ở lại app.

### 2.2 Đưa vòng "hàng đợi chương" vào lõi (đổi mã điện thoại)

Vòng chạy theo chương nằm trong `TranslatorService.runPreparedJobs`, dòng 339–390. Nó gồm:
- `ChapterRunOutcome`;
- gán Glossary/Pronoun cho từng chương (`binding.applyTo(s)`);
- `continuesQueue` / `shouldStopQueue`;
- chuyển sang chương kế khi một chương lỗi;
- dòng tổng kết.

**Chuyển phần này sang lõi** thành `ChapterQueue.run(tasks, runner, ChapterStep step)`:
- `tasks`: danh sách gồm chương, nhãn, `ChapterBinding` và job cần tiếp tục (nếu có);
- `ChapterStep`: hàm chạy một chương:
  - điện thoại: `runSinglePreparedJob` hoặc `continueExistingJob` như hiện nay, dùng prepared batch;
  - PC: `runner.startNewJob` hoặc `continueExistingJob`.
- Thân vòng chép nguyên, thay theo bảng phép thay của P1b. Chạy script so token.
- Chuẩn bị (`PreparedBatch`, `Uri`, `ChapterSession` trong `getFilesDir`) ở lại service.

**Test JVM:**
- 3 chương, chương 2 lỗi theo kiểu "đi tiếp" → chương 3 vẫn chạy, tổng kết đúng;
- lỗi kiểu "dừng hàng đợi" → dừng;
- hủy giữa chương 2;
- tiếp tục một chương dở.

**Vì đổi mã điện thoại, phải làm đủ:** build `4.24-dev`, sao lưu, ghi số đếm, **dừng chờ owner gõ "duyệt cài" trong phiên**, `install -r`, số đếm sau cài giống hệt, test vàng 12/12.

### 2.3 Màn "Dịch cả tập" trên PC

1. Chọn RAW từ thư viện, **hoặc** chọn một thư mục RAW.
2. Chọn thư mục Glossary và thư mục Pronoun (theo cách owner xếp tệp, ví dụ `2_PRONOUN_KUMO_VOL13\001_….csv`), hoặc chọn từ thư viện.
3. Ghép theo số chương bằng `ChapterSetMatcher` (lõi). Hiện **bảng xem lại**: chương, RAW, Glossary, Pronoun, trạng thái ghép.
   - Người dùng sửa được từng ô.
   - Chương thiếu RAW không chạy. Chương thiếu Glossary/Pronoun hỏi lại: "dùng Glossary/Pronoun đang chọn" hoặc "bỏ qua".
4. Ước tính tổng (số chương, chunk, token, $) và trần chi phí cho cả lượt.
5. **"Bắt đầu" luôn có hộp xác nhận**, ghi số chương, model, effort, ước tính và trần $.
6. Khi chạy: thanh "Chương i/N", tiến độ chunk, Tạm dừng / Tiếp tục / Hủy, tổng kết cuối lượt.

Tên tệp output theo `buildOutputName`, giống điện thoại.

## 3. Lịch sử

Màn **Công việc** trên PC:
- thẻ **Đang làm / Lịch sử**;
- Lịch sử phân trang theo `id DESC` (`JobHistoryPageModel`, lõi);
- gộp các chương của cùng một lượt (`RunGroups`);
- lọc theo trạng thái;
- mở chi tiết job: số chunk xong/lỗi, chi phí, các lượt thử. **Không hiện nội dung chunk**, chỉ số liệu;
- "Mở thư mục output".

## 4. Giao diện màu và cỡ chữ

**5 giao diện sáng giống điện thoại:** Xám nhạt, Bạc hà, Trời nhạt, Oải hương, Hồng đào.
- Lấy đúng mã màu ở bảng U6 (`docs/V423_UX_OVERHAUL_WORK_REQUEST.md` mục 7).
- Đặt tên biến CSS theo đúng tên token ở bảng đó.
- Cỡ chữ 90 / 100 / 115 / 130%.
- Lưu trong prefs của PC.

**Test:**
- mã màu khớp bảng U6;
- độ tương phản chữ ≥ 4,5, viền ≥ 3, `onAccent` ≥ 4,5, `muted` trên `card` ≥ 4,5, như các kiểm tra của U6.

Không làm giao diện tối trong gói này.

## 5. Bộ cài Windows

- Cần một JDK có `jmods` và `jpackage` (ví dụ Eclipse Temurin 21), cùng WiX 3 hoặc 4 để tạo `.msi`.
- **Cài phần mềm vào máy là quyết định của owner.**
  - **Dừng và hỏi owner** trước khi tải hay cài bất cứ thứ gì.
  - Nêu rõ: tên, phiên bản, nguồn tải chính thức, dung lượng.
  - Owner không đồng ý: giữ gói ZIP như P2 và ghi `NOT_DONE`.
- **Khi owner đã đồng ý và đã cài:**
  - `build-desktop.ps1` tạo runtime gọn bằng `jlink`, với module lấy từ `jdeps` trên mọi jar;
  - dùng `jpackage --type msi`, gồm: tên "Translate Books", lối tắt trong Start Menu, cài theo user (không cần quyền admin), nâng cấp đè bản cũ (`--win-upgrade-uuid` cố định);
  - **gỡ cài đặt không được xóa** `%LOCALAPPDATA%\TranslateBooks\`;
  - gói `.msi` lưu theo chính sách artifact kép như P2.
- **Bộ cài chưa được ký số**, nên Windows SmartScreen sẽ cảnh báo. Ghi rõ trong README.

## 6. Dọn phần còn lại của P2

- `FileUtil.decodeText` trên điện thoại gọi sang `TextDecoding` (lõi), thay cho bản chép. Đổi mã điện thoại, đi chung lần cài ở mục 2.2.
- Hàm `TranslationEngine.updatePartialOutput(TranslationRepository, …)` không có nơi gọi (ghi nhận từ P1b): xóa, nếu grep xác nhận không còn nơi gọi, kể cả trong `androidTest`.

## 6b. Bổ sung sau phản biện (mục này thắng nếu khác các mục trên)

1. **Truy cập.** `LibraryArchive`, `ChapterBinding`, `ChapterSession`, `ChapterSetMatcher`, `RunGroups` và `ChapterNumber` là package-private. Giao diện ở `…desktop` chỉ gọi chúng qua **cầu nối trong `DesktopRuntime`** (package lõi). Không đổi mức truy cập, không đổi hành vi.
2. **Hồ sơ cài đặt dùng chung cho hai bên.**
   - Cả nhập và xuất **bỏ qua** `editorial`, `instructionUri`, `envUri`, `outputUri`, `outputTreeUri` và các trường `*Name` đi kèm URI. Bảng khác biệt không hiện các trường này.
   - Khi xuất từ PC, chỉ ghi các khóa dùng chung, và liệt kê chúng trong README.
   - **Điện thoại** vẫn dùng `SettingsStore.save(imported)` cho toàn bộ đối tượng. Vì vậy hàm nhập ở lõi phải **giữ nguyên giá trị hiện tại** cho mọi khóa bị bỏ qua hoặc vắng mặt. Test: nhập một hồ sơ xuất từ PC vào cài đặt điện thoại thì `editorial` và `outputTreeUri` không đổi.
   - `EditorialPreferences` nằm sẵn ở lõi. Không sửa lớp đó.
3. **Bước áp dụng gói thư viện** hiện chỉ có ở app (`MainActivity.applyLibraryImport`, `:2066–2092`). Viết bước áp dụng cho PC: ghi Glossary/Pronoun, ghi RAW, báo lỗi ghi. Không tách hàm của app.
4. **Thư viện RAW trên PC:**
   - Không tách `LibraryRawStore` (toàn bộ dùng `Context`/SQLite). Viết `DesktopRawStore` trên `SqlDb` + `RawFileStore`; `RawImportPlanner`, `RawUri`, `ChapterNumber` đã ở lõi.
   - `PathDocumentIO` phải đọc được tham chiếu `tblraw://<id>` của RAW trong thư viện. Test: job tạo từ RAW thư viện chạy được.
5. **`ChapterQueue`:**
   - Vòng nằm ở `TranslatorService.java:354–390`. Nhánh `outcome==null` (không phải chế độ chương) ở lại service.
   - `QueueStoppedException` đưa vào lõi, mức `public`, để khối `catch` ở `:310` vẫn bắt đúng.
   - `repo.touchJob`, `deferredOutput`, `finishCancelled`, `runSinglePreparedJob`, `parseResumeJobs` ở lại service, truyền vào qua callback.
6. **Lượt chương và lịch sử trên PC:**
   - `RunGroups` chỉ gộp được từ tệp phiên `translate_sessions/*.json` (`ChapterSession`). App PC phải **ghi `ChapterSession`** vào thư mục dữ liệu khi chạy cả tập, giống điện thoại.
   - Chi tiết "các lượt thử" đọc bảng `chunk_attempts` qua một hàm truy vấn đặt tên rõ trong `TranslationRepositoryCore`.
7. **Ghép theo số chương:** `ChapterNumber.parse` chỉ lấy chữ số ở đầu tên tệp.
   - Một tệp chung như `GLOSSARY_WORKING.csv` không có số chương → ô Glossary để trống, đánh dấu vàng.
   - Bảng xem lại cho phép chọn **một Glossary dùng cho mọi chương**.
   - Nếu chọn thư mục có `chapters/` bên trong, ghi rõ có quét thư mục con hay không, và quét thế nào.
8. **Giao diện màu:**
   - Điện thoại đã có đúng mã màu trong `UiAppearance.java:67–76`. Tên trong code là `panel`, `card`, `muted`, `border`, khác tên ở bảng U6.
   - Tách `Palette` và danh sách 5 bảng màu sang lõi; `UiAppearance` ở app chỉ còn `load`/`save`. PC đọc từ lõi, **không chép mã màu**.
   - Test so mã với bảng U6 và tính tương phản bằng `ColorMath`.
   - Đây là thay đổi mã điện thoại, nên đi chung lần cài ở mục 2.2.
9. **Bộ cài:**
   - **JDK 21 `jpackage` chỉ dùng WiX 3.x.** Hỏi owner cài đúng WiX 3.
   - `--win-per-user-install` cài mặc định vào `%LOCALAPPDATA%\TranslateBooks`, **trùng thư mục dữ liệu**. Đặt `--install-dir` khác (ví dụ `%LOCALAPPDATA%\Programs\TranslateBooks`).
   - Kiểm: gỡ cài đặt → dữ liệu còn nguyên; cài bản mới đè bản cũ → dữ liệu còn nguyên.
   - `build-desktop.ps1` **đã có** bước `jlink` với danh sách module cố định (`:66–73`). **Sửa** nó: lấy module từ `jdeps --multi-release 21` trên mọi jar, và thử JavaFX khởi động được từ runtime image.
10. **`TextDecoding`:**
    - `TextDecodingMatchesFileUtilTest` so lõi với `FileUtil.decodeText` bằng reflection. Sau khi `FileUtil` gọi sang lõi, test này chỉ còn so lõi với chính nó. Thay bằng **bộ vector cố định**: byte vào → chữ và encoding ra, gồm UTF-8, có BOM, UTF-16LE/BE, Shift-JIS.
    - Giữ `FileUtil.TextReadResult` public (`MainActivity` dùng 3 chỗ, cùng `PreparationCoordinator`), chuyển từ kết quả của lõi.
11. **Chỉ xóa overload** `TranslationEngine.updatePartialOutput(TranslationRepository, …)`. Hàm cùng tên của `TranslationRunner` vẫn được dùng.
12. **Trần chi phí khi chạy cả tập:** trần áp cho **cả lượt**. Chạm trần giữa hàng đợi thì dừng theo `enforceCostLimit`, ghi chương đang dở là "dừng vì trần", và **không** chuyển sang chương tiếp. Thêm vào test của `ChapterQueue`.
13. **Kiểm thêm:**
    - ảnh hộp xác nhận "Bắt đầu" (không bấm);
    - chốt `git diff 5f07d9f3..HEAD -- '*Editorial*'` rỗng, chạy trên bản archive sạch;
    - test nhập hồ sơ ở mục 2 của phần này.

## 7. Kiểm

1. **Hồi quy** trên `git archive` sạch:
   ```
   gradlew --offline -q testDebugUnitTest :translation-core:test :translation-core-jdbc:test :desktop:test compileDebugAndroidTestJavaWithJavac lintDebug
   ```
   0 FAIL; test vàng 12/12; lint 0 lỗi.
2. **Script so token** cho các hàm chuyển ở mục 2.2 và 6.
3. **Điện thoại:** đúng quy trình ở mục 2.2.
4. **App PC:**
   - chạy với `TBL_DATA_DIR` riêng;
   - chụp màn hình: nhập hồ sơ, nhập gói thư viện, bảng ghép chương, Lịch sử, 5 giao diện màu ở cỡ chữ 100% và 130%;
   - **không bấm Bắt đầu**;
   - ảnh không lộ key, tên người dùng Windows hay văn bản sách. Dùng dữ liệu `QA_` tự viết, ngoại trừ phần đếm của mục 1.
5. **Lượt dịch thật cả tập trên PC:** **chỉ khi owner cấp quyền chi riêng.** Đề xuất ≤ $0,05: 2 chương `QA_` tự viết, mỗi chương 2 chunk, effort `medium`, có 1 lần Dừng/Tiếp tục.
   - Trước khi gọi provider, kiểm hình dạng key: dài 73 ký tự, đúng regex. Không in key.
   - Không có quyền chi thì ghi `NOT_MEASURED`.

## 8. Không được làm

- Không sửa tệp `Editorial*` hay `editorial-engine/`.
- Không đổi logic dịch, prompt hay giá trị mặc định.
- Không tải hay cài JDK/WiX khi owner chưa đồng ý.
- Không gọi provider khi chưa có quyền chi. Không gọi mạng khi mở app.
- Không đưa API key, văn bản sách hay dữ liệu cá nhân vào Git, log, ảnh hay gói.
- Không tag, push hay merge vào nhánh khác.

## 9. Báo cáo

- Commit theo từng mục.
- Kết quả script so token.
- Hồi quy: số test từng module, lint.
- Bản APK `4.24-dev`: SHA-256, bản sao lưu, số đếm trước và sau cài.
- Gói PC (ZIP và `.msi` nếu có): SHA-256.
- Số đếm nhập từ điện thoại.
- Ảnh màn hình.
- Lượt dịch thật: kết quả, hoặc `NOT_MEASURED`.
- Các việc dừng chờ owner.
