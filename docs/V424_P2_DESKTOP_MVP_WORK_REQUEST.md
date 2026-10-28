# v4.24 P2 — App PC đầu tiên (JavaFX) trên lõi chung (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** phương án A cho bản PC (owner duyệt 2026-10-27). Sau P0a–P1b, lõi `translation-core` đã chứa prompt, chunk, validator, chi phí, lưu trữ và vòng chạy. P2 làm **app Windows đầu tiên** dùng đúng lõi đó: một chương RAW cùng Glossary/Pronoun được dịch trên PC bằng **cùng prompt, cùng cách chia chunk, cùng trần chi phí, cùng dừng/tiếp tục** như điện thoại.

**Phạm vi:** chỉ phần Dịch. Không có Biên tập, không dịch cả tập (để P3), không làm bộ cài `.msi` (để P3).

**Không đổi app điện thoại.** Ngoại lệ duy nhất: tách 3 hàm thuần sang lõi theo mẫu S5 ở mục 3 (hàm cũ ở lại, chỉ gọi sang lõi). Test vàng 12/12 phải giữ.

**Nơi làm:** nhánh `feature/v4.24`, worktree `D:\P5E-builds\wt-v4.24`, từ `70d21022`. Không tag, push hay merge vào nhánh khác.

## 1. Ràng buộc môi trường (đã kiểm trên máy)

- **JDK:** dùng `C:\Program Files\Android\Android Studio\jbr` (Java 21). Không có `jpackage`, **không có thư mục `jmods`**, máy không có WiX.
  - Vì thiếu `jmods`, `jlink` gần như chắc chắn không tạo được runtime. Bước đầu tiên: thử `jlink --add-modules java.base` vào thư mục tạm, ghi kết quả.
  - Nếu lỗi (dự kiến), gói P2 **kèm bản sao nguyên thư mục `jbr`** làm runtime, cộng thư viện và `TranslateBooks.cmd`.
  - Bộ cài `.exe`/`.msi` và runtime gọn để P3, khi owner duyệt cài một JDK có `jmods`/`jpackage` và WiX.
- **JavaFX** chưa có trong cache Gradle. **Được tải một lần** từ Maven Central: `org.openjfx:javafx-base`, `-graphics`, `-controls` bản 21 LTS mới nhất, classifier `win`. Khai báo trực tiếp, không dùng plugin `org.openjfx.javafxplugin`. Ghi phiên bản và SHA-256 vào báo cáo.
- **Chạy JavaFX trên classpath** (không dùng module): lớp `main` là một launcher **không** kế thừa `Application`, rồi gọi `Application.launch(...)`.
- **JNA 5.6.0** (`jna`, `jna-platform`) đã có trong cache. Dùng cho DPAPI qua `Crypt32Util`.
- **`sqlite-jdbc 3.46.1.3`** đã có (từ P1a). Chuyển từ `testImplementation` của lõi sang `implementation` của module PC.
- **`org.json:json:20250517`:** module PC khai báo `implementation`. Lõi vẫn biên dịch theo `android.jar` (chốt `put(String,float)` của P0b giữ nguyên), còn khi chạy trên PC thì dùng thư viện này.
- **Lõi cần Android SDK để build.** Máy này có SDK. Ghi rõ điều đó trong README của module.

## 2. Module mới `desktop`

```
desktop/
  build.gradle      application, Java 21 toolchain không bắt buộc; mainClass = com.ml.tblandroidtxt.desktop.DesktopLauncher
  src/main/java/com/ml/tblandroidtxt/desktop/...   (package riêng cho phần giao diện PC)
  src/main/java/com/ml/tblandroidtxt/...            (chỉ các lớp hiện thực cổng cần truy cập package-private của lõi)
  src/test/java/...
```

- `settings.gradle`: thêm `include ':desktop'`.
- **Phụ thuộc:**
  - `implementation project(':translation-core')`;
  - JavaFX 3 gói `win`;
  - `sqlite-jdbc`, `org.json`;
  - `jna`, `jna-platform` 5.6.0;
  - `junit` cho test.
- **Thư mục dữ liệu:** `%LOCALAPPDATA%\TranslateBooks\` gồm `db\tbl_android_txt.db`, `prefs\*.json`, `logs\`, `catalog\`, `secrets\`.
  - Không bao giờ nằm trong repo.
  - Cho phép đổi thư mục bằng biến môi trường `TBL_DATA_DIR`, chỉ dùng cho test.

## 3. Bổ sung lõi (phần cần thiết của P1a-2, mẫu S5)

Chỉ tách 3 nhóm hàm thuần. Hàm cũ ở app giữ nguyên chữ ký và chỉ còn một dòng gọi sang:

1. **`TextDecoding`** (lõi): `decodeText(byte[])` và phần đọc luồng có nhận BOM (UTF-8 / UTF-16LE / UTF-16BE), lấy từ `FileUtil` (dòng 129–160).
   - PC phải đọc RAW **đúng như điện thoại**.
   - Test: cùng một bộ byte (tự viết, có và không có BOM, UTF-16) cho kết quả giống nhau qua lõi và qua bản cũ.
2. **`SettingsJson`** (lõi): `toJson`, `fromJson`, `stripPersistedSecrets`, `toPersistedJson`, `toExportJson`, `fromImportedProfile`, `toEnv`, lấy từ `SettingsStore`.
   - Chỉ chuyển những hàm không dùng `Context`/`SharedPreferences`.
   - Hàm nào thực ra có dùng thì để lại và ghi vào báo cáo.
3. **`LogStore.formatLine`** → `LogFormat.formatLine` (lõi).

Test vàng 12/12 và toàn bộ test app phải giữ. `git diff 70d21022..HEAD -- '*Editorial*'` rỗng.

## 4. Hiện thực các cổng trên PC

| Cổng (lõi) | Lớp PC | Ghi chú |
|---|---|---|
| `SqlDb` | `JdbcSqlDb` | Chuyển từ `translation-core/src/test` sang `desktop/src/main`; test của lõi dùng lại qua `testImplementation project(':desktop')` hoặc chép. Giữ đúng các khác biệt đã xử lý ở P1a (foreign keys, giao dịch lồng, …) |
| Lược đồ DB | `DesktopSchema` | DB mới được tạo bằng cách chạy `tbl_android_txt_v30.sql`, lấy từ DB thật của điện thoại ở P1a (đặt làm resource), rồi `PRAGMA user_version=30`. DB có sẵn với `user_version` khác 30 thì **từ chối mở** và báo rõ. **Rủi ro, ghi vào README:** khi lược đồ điện thoại đổi, phải tạo lại tệp này. Chuyển lược đồ hẳn vào lõi để sau |
| `KeyValueStore` | `JsonFileKeyValueStore` | Mỗi tên prefs một tệp `prefs\<tên>.json`. Ghi atomic (tệp tạm + `ATOMIC_MOVE`). `commit()` trả `true`/`false` thật |
| `DocumentIO` | `PathDocumentIO` | `ref` dạng `file:///…` (`Path.toUri().toString()`). Đọc qua `TextDecoding`. Ghi UTF-8 không BOM, có kiểm lại sau khi ghi như `writeTextVerified`. `resolveOutput(jobId, ref)`: thư mục output người dùng chọn + tên tệp theo `OutputNames.sanitizeOutputName` |
| `SecretsResolver` | `DpapiSecrets` | API key mã hóa bằng DPAPI (`Crypt32Util.cryptProtectData`, phạm vi user hiện tại), lưu `secrets\api_key.bin`. Không bao giờ ghi key vào prefs, log, DB hay `toPersistedJson` |
| `RunListener` | `FxRunListener` | Chuyển mọi cập nhật sang luồng giao diện bằng `Platform.runLater` |
| `RunStatePersister` | `JsonRunStatePersister` | Ghi trạng thái gần nhất vào `prefs\runtime_state.json`, để mở lại app vẫn thấy job dở |
| `RuntimeLog` / `TraceSink` / `ObservabilityLog.Sink` | `FileRuntimeLog` | `logs\tbl_runtime.log`, định dạng qua `LogFormat.formatLine`. Không ghi key, không ghi văn bản sách vào log (giống điện thoại) |
| `ModelCatalog.CacheStore` | `FileModelCatalogCache` (đã có ở lõi) | `catalog\openrouter_model_catalog_v42.json` |
| `Clock` / `Sleeper` | bản hệ thống | |

Mỗi lớp PC có test JVM riêng: ghi rồi đọc lại; ghi atomic khi bị ngắt giữa chừng; DPAPI mã hóa rồi giải mã (bỏ qua test này nếu không chạy trên Windows); `DesktopSchema` tạo DB mới có 142 đối tượng và `user_version=30`.

## 5. Giao diện (JavaFX, một cửa sổ)

**Bố cục:**
- cột điều hướng trái cố định, gồm **Dịch**, **Công việc**, **Thư viện**, **Cài đặt**;
- vùng nội dung bên phải;
- thanh trạng thái dưới cùng cho lượt đang chạy (trạng thái, chunk x/y, token, chi phí, đồng hồ).

Một giao diện sáng. Màu lấy từ bộ "Xám nhạt" của U6 (`docs/V423_UX_OVERHAUL_WORK_REQUEST.md` mục 7), viết thành biến CSS. Không dùng emoji làm biểu tượng.

**Thuật ngữ** theo quy tắc của owner: giữ tiếng Anh chunk, prompt, token, model, provider, log, cache, Glossary, Pronoun, RAW/DRAFT/FINAL, API key, Base URL, endpoint. Hành động và từ thường dùng tiếng Việt, giống bản điện thoại.

**Cài đặt:**
- provider (openrouter, openai, deepseek, custom), Base URL, model (chọn từ catalog, có ô tìm), API key (ô ẩn, nút "Lưu key"; hiển thị chỉ `••••` cùng 4 ký tự cuối);
- reasoning, temperature, `maxTokensPerChunk`, `maxOutputTokens`, `contextChars`, preset;
- instruction YAML (chọn tệp; lưu đường dẫn, đọc lại mỗi lượt);
- trần chi phí mỗi lượt.

Lưu qua `SettingsJson.toPersistedJson`, tức không có key.

**Thư viện:**
- nhập nhiều tệp Glossary CSV / Pronoun CSV;
- danh sách, chọn mục đang dùng, xóa có hỏi lại;
- dùng `GlossaryStoreCore` / `PronounStoreCore` qua `JsonFileKeyValueStore`.

RAW chưa lưu vào thư viện ở P2: chọn tệp trực tiếp ở màn Dịch.

**Dịch:**
- chọn tệp RAW (`.txt`) và thư mục output;
- hiện Glossary/Pronoun đang dùng;
- hiện **ước tính** (`CostEstimator`: số chunk, token, $) trước khi chạy.
- **"Bắt đầu" luôn mở hộp xác nhận**, ghi model, effort, ước tính $ và trần $. Không có đường chạy nào bỏ qua bước này.
- Khi đang chạy:
  - Tạm dừng / Tiếp tục / Hủy (Hủy có hỏi lại);
  - xem trước chunk vừa xong;
  - số liệu như điện thoại, lấy từ `RunSnapshot`.
- Xong: nút "Mở thư mục output".

**Công việc:**
- danh sách job từ `getJobSummaries`, gồm trạng thái, số chunk xong/lỗi, chi phí;
- trên job dở có nút **Tiếp tục**; trên job có chunk lỗi có nút **Dịch lại chunk lỗi**. Cả hai đều qua hộp xác nhận tiền.

Mọi thao tác gọi provider đều chạy trên luồng nền. Giao diện không bao giờ bị treo.

## 6. Build và lưu bản

- Thêm `scripts/build-desktop.ps1` theo tinh thần `build-and-save.ps1`:
  - đánh số `4.24-pc-dev.N`;
  - chạy `:desktop:installDist`;
  - tạo runtime bằng `jlink`, gồm các module JDK cần (liệt kê bằng `jdeps`);
  - gói thành thư mục `TranslateBooks-PC-4.24-pc-dev.N\` có `TranslateBooks.cmd`;
  - nén ZIP;
  - lưu **cùng một gói** vào `artifacts/builds/desktop-v4.24-pc-dev.N/<event>` và `backup/builds/desktop-v4.24-pc-dev.N/<event>`, gồm ZIP, README, `BUILD_INFO.json`, `SHA256SUMS`, ZIP mã nguồn đúng commit.

  Không ghi đè gói cũ.
- Chạy được trên máy này **không cần cài Java** (dùng runtime đi kèm).

## 7. Kiểm

1. **Hồi quy** trên `git archive` sạch:
   ```
   gradlew --offline -q testDebugUnitTest :translation-core:test :desktop:test compileDebugAndroidTestJavaWithJavac lintDebug
   ```
   0 FAIL; test vàng 12/12; lint 0 lỗi.
2. **Test prompt chéo:** một test ở `desktop` (package `com.ml.tblandroidtxt`, vì `buildChatRequestBody` là package-private) chạy cùng fixture P0a qua đường PC: `PathDocumentIO` đọc tệp → `AppValidator.normalize` → `Chunker` → `PromptPlan.forTranslation` → `buildChatRequestBody`. So với tệp vàng của `balanced_default`:
   - `chunks.json` và system/user prompt: **giống từng byte**;
   - thân request: **giống sau khi chuẩn hóa JSON**. `org.json` trên PC xếp khóa khác Android;
   - thêm một khẳng định cho giá trị `temperature`.

   Đây là bằng chứng "PC và điện thoại gửi cùng một prompt".
3. **Lượt dịch giả trên PC:** test chạy `TranslationRunner` với provider giả, `JdbcSqlDb` thật và `PathDocumentIO` thật trong thư mục tạm. Kiểm DB và tệp output, kể cả tạm dừng/tiếp tục và tiếp tục job sau khi "tắt app" (mở lại DB).
4. **Mở app thật trên máy:**
   - chạy `TranslateBooks.cmd` với `TBL_DATA_DIR` trỏ tới một thư mục tạm;
   - chụp màn hình 4 trang (Dịch, Công việc, Thư viện, Cài đặt);
   - nhập một Glossary/Pronoun `QA_` tự viết;
   - chọn RAW `QA_` tự viết và xem ước tính.

   **Không bấm Bắt đầu.** Không nhập API key thật: nhập `TEST_KEY_NOT_SECRET` để thử lưu/đọc DPAPI. Ảnh không được chứa key hay văn bản sách.
5. **Lượt dịch thật** (2–3 chunk, tệp `QA_`, effort `medium`): **chỉ khi owner cấp quyền chi riêng**, đề xuất ≤ $0,03, và owner tự nhập API key trên máy. Không có quyền thì ghi `NOT_MEASURED`.

## 7b. Bổ sung sau phản biện (mục này thắng nếu khác các mục trên)

1. **Đưa đường "Bắt đầu" vào lõi (chặn chính).**
   - Lõi chưa có lối vào cho một lượt mới. `runNewJobs`/`runSingleJob` và `TranslationEngine.prepareInput` vẫn ở `TranslatorService` (dòng 243–435). Chúng gồm: chuẩn bị input, `repo.createJob`, đặt job đang chạy, `touchJob`, `translateJob`, `finishJob`, xử lý trần tiền và trạng thái.
   - **Chuyển phần thuần thành `TranslationRunner.startNewJob(...)`**: đọc input qua `DocumentIO`, `prepareInput` sang `TranslationEngineCore`, chép nguyên thân hàm theo bảng thay của P1b.
   - `TranslatorService` gọi sang hàm này. Phần còn lại ở service: đọc Intent, nhiều tệp, SAF, prepared batch.
   - Chạy lại script so token của P1b cho các hàm vừa chuyển. Test vàng 12/12 phải giữ.
   - **Thay đổi này chạy trên điện thoại**, nên **phải** build `4.24-dev`, sao lưu, ghi số đếm, dừng chờ owner gõ "duyệt cài" ngay trong phiên, `install -r`, và số đếm phải giống hệt. Mục 8 "không đụng điện thoại" được thay bằng điểm này.
   - Bảng cổng (mục 4) thêm: `TranslationEngineCore(repo, ProviderClient)` và `runner.attach(repo, engine)`.
2. **Truy cập package-private:** không mở rộng `public`. Các lớp nối cổng đặt trong module `desktop` **nhưng thuộc package `com.ml.tblandroidtxt`**:
   - `DesktopRepository extends TranslationRepositoryCore`, vì constructor là `protected`;
   - lớp dựng runner;
   - test prompt chéo.

   Giao diện ở `…desktop` chỉ gọi các lớp này. Tên tệp prefs phải trùng hằng `PREF` của lõi (`glossary_store`, `pronoun_store`).
3. **`DesktopSchema`:**
   - tách câu theo dấu `-- @@@` như `TestDb.java:23`;
   - **bỏ qua `CREATE TABLE sqlite_sequence`** (tên dành riêng, SQLite từ chối);
   - mở với `foreign_keys=ON`, như `onConfigure` của điện thoại.

   Tiêu chí đạt: tập tên trong `sqlite_master` có `sql IS NOT NULL` bằng tập tên lấy từ tệp lược đồ (trừ `sqlite_sequence`), và `user_version=30`. Không dùng con số "142 đối tượng".
4. **`JdbcSqlDb`** chuyển sang **module nhỏ mới `translation-core-jdbc`**. Module gồm `JdbcSqlDb`, `DesktopSchema` và tệp lược đồ làm resource, chỉ phụ thuộc lõi. Test của lõi và module `desktop` cùng dùng nó qua `testImplementation`/`implementation`. Không để lõi phụ thuộc `desktop`, không chép lớp.
5. **Mục 3 sửa lại:**
   - `SettingsJson` đã có `toJson`, `toPersistedJson`, `stripPersistedSecrets`, `fromJson`. Chỉ thêm `toExportJson`, `fromImportedProfile` và `withCurrentSecrets` bản hai tham số.
   - **Không chuyển `toEnv`**: hàm này ghi `API_KEY=`. App PC không bao giờ gọi nó.
   - `TextDecoding` chuyển cả bước dự phòng từ UTF-8 nghiêm ngặt sang windows-31j/Shift-JIS và kiểu kết quả `TextReadResult`. Test byte phải có ca Shift-JIS. `readLargeText` cần `Context`, nên chỉ phần nhận BOM được tách.
   - `LogFormat.formatLine` giữ `Locale.getDefault()` như cũ. Ghi trong README rằng mốc giờ theo locale Windows.
6. **Không gọi mạng khi khởi động.** `ModelCatalog.initialize` luôn gọi `refreshAsync`, tức tải danh sách model từ `openrouter.ai` (`ModelCatalog.java:152, 161–164`).
   - App PC chỉ nạp cache khi mở.
   - Việc tải lại catalog nằm sau nút "Cập nhật danh sách model", chỉ bật khi người dùng bấm.
   - Mọi test và bước 7.4 chạy offline.
7. **`jlink`:** nếu bước thử ở mục 1 thành công, lấy module bằng `jdeps --multi-release 21` trên **mọi** jar trong classpath. Danh sách dự kiến: `java.desktop`, `jdk.unsupported`, `java.sql`, `java.naming`, `jdk.crypto.ec`, `java.xml`, `java.logging`.
   - Ghi trong README: JavaFX giải nén DLL vào `%USERPROFILE%\.openjfx\cache`, và in cảnh báo "unnamed module".
8. **Chi tiết nhỏ:**
   - tạo thư mục `catalog\` trước khi dùng `FileModelCatalogCache`;
   - test khẳng định `TEST_KEY_NOT_SECRET` không có trong `prefs\*.json`, log hay DB;
   - `FxRunListener.onRunEnded`/`onCancelledEnd`: bật lại các nút, cập nhật danh sách job, **không** đóng cửa sổ;
   - chỉ cho **một lượt chạy một lúc**. Khi đang chạy, khóa "Bắt đầu" / "Tiếp tục" / "Dịch lại". Mở app lần hai thì báo và thoát, dùng khóa tệp trong thư mục dữ liệu;
   - ảnh chụp không được lộ đường dẫn chứa tên người dùng Windows: dùng `TBL_DATA_DIR` và tệp `QA_` trong một thư mục trung tính như `D:\P5E-builds\qa-desktop\`.

## 8. Không được làm

- Không sửa tệp `Editorial*` hay `editorial-engine/`.
- Không đổi logic dịch, prompt hay giá trị mặc định trong lõi, ngoài việc tách 3 nhóm hàm thuần ở mục 3.
- Không đưa API key, văn bản sách hay dữ liệu cá nhân vào Git, log, ảnh hay gói build.
- Không gọi provider khi chưa có quyền chi.
- Không cài phần mềm hệ thống (JDK, WiX). Chỉ tải thư viện Maven nêu ở mục 1.
- Không tag, push hay merge vào nhánh khác.
- Không đụng điện thoại: không có thay đổi chạy trên máy, nên không build hay cài APK.

## 9. Báo cáo

- Các commit.
- Phiên bản và SHA-256 của các thư viện mới tải.
- Danh sách lớp PC và hàm đã tách sang lõi.
- Hồi quy: số test app / lõi / desktop, lint.
- Kết quả test prompt chéo và lượt dịch giả.
- Đường dẫn gói build, SHA-256, kích thước.
- 4 ảnh màn hình.
- Lượt dịch thật: kết quả, hoặc `NOT_MEASURED`.
- Xác nhận:
  - không sửa tệp `Editorial*`;
  - không có key hay văn bản sách trong Git;
  - không gọi provider.
