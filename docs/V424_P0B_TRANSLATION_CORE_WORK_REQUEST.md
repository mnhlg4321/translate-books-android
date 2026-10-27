# v4.24 P0b — Tách `translation-core` (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** phương án A cho bản PC, owner duyệt ngày 2026-10-27 (`docs/PC_APP_RESEARCH.md`). P0b đưa phần dịch không phụ thuộc Android (prompt, chunk, validator, chi phí, client provider, cài đặt) vào module Java dùng chung. Sau gói này, app PC (P2) có thể gọi đúng mã mà điện thoại đang dùng.

**Bản dịch trên điện thoại không được đổi.** Bằng chứng là test prompt vàng của P0a: prompt, chunk, thân request và ước tính chi phí phải giống từng byte.

**Điều kiện bắt đầu:**
- v4.23 đã chốt (tag), **hoặc** owner nói rõ "làm P0b trước khi chốt v4.23";
- trước khi bắt đầu, merge đầu nhánh `feature/v4.23` vào `feature/v4.24` (`git merge --no-ff`, không rebase). Sau merge, test vàng phải còn PASS.

Gói này không có quyền tag, push hay merge vào nhánh khác.

## 0. Bước 0 — bổ sung test vàng trên mã **chưa tách**

Làm và commit **trước** mọi thay đổi ở mục 1–3, để tệp vàng mới được ghi từ mã gốc.

1. **`glossary.csv`:** thêm một dòng có `target` trống, dùng một từ có trong `raw_ja.txt`. Sửa ghi chú của dòng `霧笛`: từ này có xuất hiện trong RAW. Dòng không xuất hiện là `灯台守`.
2. **Tổ hợp mới `catalog_known`:** giống `balanced_default`, nhưng trước khi chạy cài một catalog test cố định qua `ModelCatalog.installForTests`, có model `openai/gpt-5.6-luna` với context length và giá nhập/xuất tự đặt.
   - Tổ hợp này phủ đường mà điện thoại thật đi qua: `Chunker` tính giới hạn an toàn theo context của model, `CostEstimator` tính giá.
   - **Chọn `contextLength` đủ nhỏ để thật sự đổi kết quả.** Không có model, `Chunker.java:308` dùng mặc định 32000. Test phải khẳng định `chunks.json` hoặc `max_tokens` của `catalog_known` khác `balanced_default`.
   - Cập nhật `cases.json` và `EXPECTED_CASES` cho tổ hợp thứ 12.
   - Riêng tổ hợp này bỏ qua kiểm tra `assertFalse(existsInActiveCatalog)` ở `GoldenPromptSnapshotTest:110`.
   - Test phải trả catalog về như cũ sau khi chạy (đã có cơ chế `savedCatalog`).
3. Ghi lại tệp vàng bằng `-Dgolden.update=true`. Chỉ được đổi:
   - tệp của các tổ hợp dùng `glossary.csv`;
   - thư mục mới `catalog_known/`.

   Liệt kê các tệp đổi trong báo cáo.
4. Hồi quy sạch như mục 5, rồi commit. **Commit này là mốc so sánh cho cả gói.** Ghi hash của nó vào báo cáo.

## 1. Module mới `translation-core`

- `settings.gradle`: thêm `include ':translation-core'`.
- **`translation-core/build.gradle`:**
  - plugin `java-library`, Java 17, mã hóa UTF-8, giống `editorial-engine/build.gradle`;
  - `api project(':editorial-engine')`, vì `EditorialPreferences` dùng kiểu của module đó;
  - `api 'com.squareup.okhttp3:okhttp:4.12.0'`, cùng phiên bản app đang dùng;
  - **`org.json` phải biên dịch theo bản của Android, không theo thư viện `org.json:json`.**
    - Lý do: `OpenAICompatibleClient.java:473` gọi `body.put("temperature", s.temperature)` với `temperature` kiểu `float` (`AppSettings.java:26`).
    - Biên dịch theo `org.json:json:20250517`, javac chọn `put(String,float)`. Android không có hàm này, nên **mọi request dịch trên điện thoại sẽ lỗi `NoSuchMethodError`**. Test JVM không bắt được.
    - Cách làm: lõi khai báo `compileOnly files("<sdk.dir>/platforms/android-35/android.jar")`, với `sdk.dir` đọc từ `local.properties`.
    - `org.json:json:20250517` chỉ đặt ở `testImplementation`, để test JVM chạy được;
  - `testImplementation 'junit:junit:4.13.2'`.
- **`app/build.gradle`:** thêm `implementation project(':translation-core')`. Giữ nguyên các dependency khác.
- **Package giữ nguyên `com.ml.tblandroidtxt`.** Lý do:
  - không phải sửa `import` ở hàng trăm tệp;
  - test trong app vẫn gọi được thành phần package-private;
  - Android và JVM test chạy trên classpath thường, không dùng JPMS.

  Không thêm `module-info.java`.

## 2. Bốn chỗ phải tách trước khi di chuyển

Đo bằng phân tích tham chiếu bắc cầu trên `feature/v4.24` tại `396117eb`: hiện chỉ 40 tệp không chạm lớp gắn Android. Lý do là gần như mọi tệp dịch đều dùng `AppSettings`, mà `AppSettings` lại bị bốn chỗ dưới đây chặn. Tách xong bốn chỗ này thì chuyển được 71 tệp (mục 3).

| # | Chỗ chặn | Cách tách | Nơi gọi phải sửa |
|---|---|---|---|
| S1 | `ObservabilityLog` giữ `Context` và ghi qua `LogStore` | Lõi giữ `formatEvent` và `event(...)`. Thêm interface `ObservabilityLog.Sink { void append(String line); }` và `static void install(Sink)`; không có sink thì `event` không làm gì, giống hiện nay khi chưa có `Context`. App thêm `AndroidObservabilitySink` (ghi qua `LogStore.append(context, line)`, dùng `getApplicationContext()`) | Đổi `ObservabilityLog.initialize(context)` thành `ObservabilityLog.install(new AndroidObservabilitySink(context))` ở `MainActivity:584` và `TranslatorService:144`. Bỏ lời gọi bên trong `ModelCatalog.initialize`, vì nơi gọi đã cài sink trước. Thêm `app/src/androidTest/.../EditorialApiV1FixtureRunnerInstrumentedTest.java:322`: **tệp test Editorial, chỉ sửa khi owner đã đồng ý (mục 4)** |
| S2 | `ModelCatalog` dùng `Context` để đọc/ghi cache catalog | Lõi giữ:
- dữ liệu, tra cứu, trạng thái, listener;
- phần tải từ mạng;
- việc dựng/đọc JSON cache (`retrieved_at`, `response`, `parseOpenRouterResponse`);
- sự kiện lỗi `catalog_cache`;
- `installForTests`.

Thêm interface `ModelCatalog.CacheStore`:
- `String load()`: nguyên nội dung tệp, `null` nếu chưa có tệp;
- `void save(String rootJson)`: ghi đè, UTF-8.

`initialize(CacheStore)` giữ kiểm `null` và chỉ nạp cache một lần; `refreshAsync(CacheStore)` thay bản nhận `Context`.

Lõi thêm `FileModelCatalogCache` (Java thuần, nhận một `File`) kèm test ghi rồi đọc lại. App thêm `AndroidModelCatalogCache`: chỉ trỏ tới `files/openrouter_model_catalog_v42.json` qua `getApplicationContext()` | `MainActivity:591` (`initialize`), `MainActivity:3568` và `:4220` (`refreshAsync`), `TranslatorService:260` (`initialize`) |
| S3 | `ApiErrorParser` kiểm `FileUtil.FileAccessException` | Lõi thêm lớp `FileAccessFailure extends Exception`, cùng hai constructor. Tên khác để không trùng tên ngắn với lớp lồng. Lớp lồng `FileUtil.FileAccessException` đổi thành `extends FileAccessFailure`, giữ tên để mọi nơi gọi không phải sửa. `ApiErrorParser` kiểm `instanceof FileAccessFailure` | Không có |
| S4 | `AppSettings` có trường `EditorialPreferences` | **Di chuyển nguyên** `EditorialPreferences.java` sang lõi bằng `git mv`, **không sửa một byte**. `git diff -M` phải báo rename 100%. Đây là tệp Editorial duy nhất được đụng, và chỉ đổi chỗ | Không có |

**Hành vi phải giữ nguyên:**
- cùng tên tệp cache catalog;
- cùng nội dung dòng log OBS;
- cùng thời điểm tải catalog: lúc mở app và lúc dịch vụ dịch khởi động.

Trước và sau khi tách, chạy các test hiện có của `ModelCatalog` và `ObservabilityLog`, và test `formatEvent`.

## 3. Danh sách di chuyển

Di chuyển bằng `git mv` từ `app/src/main/java/com/ml/tblandroidtxt/` sang `translation-core/src/main/java/com/ml/tblandroidtxt/`. **Chính xác 71 tệp:**

> AcceptedTranslationPreview, ApiErrorParser, ApiHttpException, AppBackNavigationPolicy, AppSettings, AppValidator, AtomicFileCommit, BenchmarkReadiness, BenchmarkReport, BudgetPolicy, ChapterLocalException, ChapterNumber, ChapterRunOutcome, ChapterSetMatcher, Chunk, ChunkMetric, ChunkQa, Chunker, ColorMath, ComboDraft, CostEstimator, CostLimitPolicy, CustomInstructions, DatabaseMigrationSpec, EditorialPreferences, EnvParser, FilePickerModel, HashUtil, InputIdentity, JobStatus, LibraryBulk, LibraryDeletePlan, LibraryMigrationSpec, LibraryRawOwnershipModel, LibrarySelectionController, LibraryTabLayoutModel, ModelCatalog, ObservabilityLog, OpenAICompatibleClient, PromptBuilder, PromptContextBuilder, PromptPair, PromptPlan, ProviderCapabilities, RawFileStore, RawImportPlanner, RawUri, ReasoningEffort, RecommendationUndoModel, ReliabilityScenarioRunner, ResponseValidator, ResumeGuard, RetryPolicy, RootChunkAttemptBudget, RunControlModel, RunCostMeter, RunPanelFormatter, RuntimeStateSnapshot, SectionCardHeaderModel, SelectedWorkModel, SettingsCategoryModel, SettingsNavigationPolicy, SettingsRecommendationModel, TranslationConfigState, TranslationDashboardFormatter, TranslationJobState, TranslationQualityChecks, TranslationUiStatePolicy, UsageTotals, WorkspaceFilterModel, YamlInstructionParser

Thêm vào lõi: `FileAccessFailure` và `FileModelCatalogCache` (mới, S2/S3). Thêm vào app: `AndroidObservabilitySink` và `AndroidModelCatalogCache` (mới, S1/S2).

**Quy tắc khi di chuyển:**
- **68 tệp** (tất cả trừ `ModelCatalog`, `ObservabilityLog`, `ApiErrorParser`) phải giống từng byte: `git diff -M --stat` báo rename 100%. Kiểm bằng `git diff --find-renames=100% --name-status <mốc bước 0>..HEAD` và ghi kết quả vào báo cáo.
- Nếu trình biên dịch báo một tệp trong danh sách vẫn gọi lớp ở lại app: **dừng**, để tệp đó lại app, ghi vào báo cáo. Không sửa nó cho vừa.
- **16 tệp còn bị chặn ở lại app** cho P1a: `BindingCache`, `ChapterBinding`, `ChapterSession`, `ChapterSetVolumeModel`, `DrawerNavigationModel`, `DrawerRecentModel`, `JobHistoryPageModel`, `JobListMerge`, `JobPicker`, `LibraryArchive`, `LibraryImportPlanner`, `NavigationPageTitles`, `OutputIntegrityAudit`, `ResumePrompt`, `RunGroups`, `TranslateTestSeam`. Lý do: chúng còn gọi `TranslationRepository`, các `*Store` hoặc Editorial.
- **Test** ở nguyên `app/src/test/`. Chúng vẫn gọi được lớp đã chuyển vì cùng package. Không chuyển test vàng.
- Thêm vào `translation-core/src/test/` một test khói nhỏ: dựng prompt từ fixture P0a **mà không dùng app**, chứng minh lõi tự đứng được.
  - chép tối thiểu các hàm phụ cần thiết (đọc cài đặt từ `cases.json`, chuẩn hóa JSON);
  - chỉ so `chunks.json` và `chunk-0` (system, user, request) của `balanced_default`;
  - thư mục làm việc khi chạy test là thư mục module, nên đường dẫn là `../app/src/test/resources/fixtures/golden-prompt/`.

## 4. Không được làm

- Không sửa logic, chữ trong prompt, tên trường, ngưỡng hay giá trị mặc định.
- Không sửa nội dung tệp `Editorial*`. **Hai ngoại lệ cần owner đồng ý trước khi bắt đầu**, ghi rõ trong câu giao việc:
  1. di chuyển nguyên `EditorialPreferences.java`, không đổi một byte;
  2. sửa một dòng `ObservabilityLog.initialize(context)` trong tệp test `EditorialApiV1FixtureRunnerInstrumentedTest.java:322`.

  Không có sự đồng ý đó thì dừng ở bước 0 và báo cáo.

  **Owner đã đồng ý cả hai ngoại lệ ngày 2026-10-27:** "Đồng ý 2 ngoại lệ cho P0b: di chuyển nguyên EditorialPreferences.java (không đổi byte nào) và sửa 1 dòng ObservabilityLog.initialize trong EditorialApiV1FixtureRunnerInstrumentedTest.java." Ngoài hai việc này, không có tệp Editorial nào khác được đụng.
- Không sửa `editorial-engine/`.
- Không sửa prompt hay instruction.
- Không đổi `versionName`/`versionCode` ngoài cách `build-and-save.ps1` tự đánh số.
- Không gọi provider. Không tag, push hay merge vào nhánh khác.
- Không làm phần app PC.

## 5. Kiểm

0. **Ba chốt chặn cho lõi:**
   - Không có `import android` trong `translation-core/src/main`. Chốt này cần vì `android.jar` có mặt khi biên dịch.
   - `javap -c -p` của 71 lớp phải giống nhau giữa bản build app ở mốc bước 0 và bản build lõi, trừ `ModelCatalog`, `ObservabilityLog` và `ApiErrorParser`. Chốt này bắt được lỗi chọn sai hàm `org.json` như ở mục 1.
   - Grep không thấy API Java 11+ mà Android có thể thiếu (ví dụ `InputStream.readAllBytes`, `Stream.toList`) trong các tệp lõi đã sửa hoặc thêm mới.
1. **Hồi quy** trên `git archive` sạch của commit cuối, có `local.properties`:
   ```
   gradlew --offline -q testDebugUnitTest :translation-core:test compileDebugAndroidTestJavaWithJavac lintDebug
   ```
   - Số test app bằng mốc bước 0, 0 FAIL;
   - test vàng 12/12 tổ hợp PASS **không cần cờ cập nhật**;
   - lint 0 lỗi;
   - `--offline` phải tải được `okhttp`, `okio` và `kotlin-stdlib` cho module Java từ cache. Nếu không được, ghi `FAILED_REPAIRING` và sửa cấu hình, không đổi phiên bản thư viện.
2. **Danh sách thư viện APK:** chạy `gradlew :app:dependencies --configuration debugRuntimeClasspath` trước (mốc bước 0) và sau. Chỉ được thêm `project :translation-core`. **Không được có `org.json:json`.** Đính kèm diff.
3. **Lớp trong APK:**
   - build bằng `scripts/build-and-save.ps1 -Series 4.24-dev`;
   - dùng `apkanalyzer dex packages` (hoặc liệt kê dex) xác nhận có `com.ml.tblandroidtxt.PromptPlan`, `AppSettings`, `EditorialPreferences` và `ModelCatalog`;
   - không có lớp nào của `org.json` ngoài lớp của hệ thống Android.
4. **Điện thoại** `15e84958`:
   - sao lưu bằng `cmd /c "adb -s 15e84958 exec-out run-as com.ml.tblandroidtxt tar czf - . > private-data.tar.gz"`, lưu vào `D:\App Translate Books\App Translate Books-device-backups\`;
   - **dừng chờ owner gõ "duyệt cài"**;
   - cài bằng `install-validated.ps1` (chỉ `install -r`), với chữ ký `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` và token `abebea4b`.

   Thử không tốn tiền, chỉ dữ liệu `QA_`:
   - mở app;
   - Cài đặt › model hiển thị danh sách catalog;
   - màn Dịch hiện ước tính chi phí cho một tệp `QA_`;
   - Công việc mở được;
   - log có dòng `OBS event=` mới (chỉ đếm dòng).

   Không bấm nút tốn tiền hay nút xóa. Không chép nguyên `settings.xml`.

## 6. Báo cáo

- Commit mốc bước 0 và các commit sau đó.
- Kết quả rename 100% của 68 tệp; diff nội dung của `ModelCatalog`, `ObservabilityLog`, `ApiErrorParser`, `FileUtil` và hai nơi gọi.
- Tệp nào phải để lại app và vì sao.
- Hồi quy: số test app và core, SKIP, FAIL, lint.
- Diff danh sách thư viện; kết quả kiểm dex.
- Bản build, `versionCode`, SHA-256; đường dẫn bản sao lưu; kết quả thử trên máy.
- Xác nhận:
  - không đổi logic;
  - không đổi nội dung tệp Editorial nào;
  - không có văn bản sách hay API key trong Git;
  - không gọi provider.
