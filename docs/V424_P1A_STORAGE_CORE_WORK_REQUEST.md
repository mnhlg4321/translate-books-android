# v4.24 P1a — Đưa lớp lưu trữ vào `translation-core` (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** phương án A cho bản PC (owner duyệt 2026-10-27). Sau P0b, phần dựng prompt đã nằm trong lõi chung. Phần **lưu job, chunk, tiến độ, Glossary, Pronoun và Tập** vẫn gắn chặt vào Android (`SQLiteDatabase`, `Cursor`, `ContentValues`, `SharedPreferences`), nên app PC chưa dùng lại được. P1a đưa phần này vào lõi qua hai "cổng": cổng DB và cổng lưu khóa–giá trị. App PC (P2) chỉ cần viết phần nối các cổng đó với SQLite trên Windows.

**Không đổi hành vi và dữ liệu trên điện thoại:**
- cùng tệp DB `tbl_android_txt.db`;
- cùng phiên bản DB (`VER = 30`);
- cùng câu SQL;
- cùng khóa `SharedPreferences`.

**Không sửa một byte nào trong tệp `Editorial*`.**

**Nơi làm:** nhánh `feature/v4.24`, worktree `D:\P5E-builds\wt-v4.24`, từ `2c5c7d9f` trở đi. Không tag, push hay merge vào nhánh khác.

## 1. Hiện trạng (đo trên `2c5c7d9f`)

**`TranslationRepository`** (909 dòng):
- `extends SQLiteOpenHelper`;
- có 79 `ContentValues`, 41 `Cursor`, 67 `SQLiteDatabase`;
- được tham chiếu bởi 61 tệp, trong đó **29 tệp Editorial**. Các tệp này dùng:
  - `new TranslationRepository(context)` và `(context, databaseName)` (trong `androidTest`);
  - các kiểu lồng `TranslationRepository.JobSummary` / `ChunkRow` / `Job`;
  - các hàm `listChapters`, `chapterAssets`, `projectReference*`, `getJob*`, `close`;
  - `editorialWritableDatabase()` / `editorialReadableDatabase()`.

**Phần API SQLite đang dùng** (repo, `ChapterSetStore`, `LibraryRawStore`):
- `rawQuery` 47, `execSQL` 42, `update` 29, `delete` 14;
- `insertOrThrow` 11, `insertWithOnConflict` 4, `insert` 3, `query` 1;
- giao dịch `begin` / `setTransactionSuccessful` / `end` 11;
- `setForeignKeyConstraintsEnabled` 1;
- Cursor: `getString`, `getInt`, `getLong`, `getDouble`, `isNull`, `getColumnCount`, `moveToNext`, `moveToFirst`, `close`.

**Các store dùng `SharedPreferences`:**
- `GlossaryStore` (305 dòng) và `PronounStore` (68 dòng), mỗi lớp có 4 tệp Editorial tham chiếu;
- `WorkspaceSetModel`: chỉ dùng cho 2 hàm "mở gần đây".

**13 trong 16 tệp còn bị chặn** chỉ cần các kiểu lồng hoặc hàm thuần của các lớp trên:
- `JobSummary`, `JobProgress`, `ChunkRow`;
- `Glossary`, `Term`, `Profile`;
- `ChapterSetStore.Row` / `SetData`, `WorkspaceSetModel.Entry`;
- `parseTerms`, `mergeTerms`, `toPromptText`, `fromJson`, `toJson`.

**3 tệp ở lại app, lý do đúng:**
- `DrawerNavigationModel` và `NavigationPageTitles` dùng hằng `EditorialApiPresenter.TAB` (điều hướng của điện thoại);
- `TranslateTestSeam` dùng `TranslationEngine`, thuộc P1b.

## 2. Mẫu thiết kế: "lớp lõi + lớp con Android cùng tên cũ"

Java cho lớp con thừa hưởng cả **kiểu lồng** và **hàm static** của lớp cha. Vì vậy:
- Lõi có `TranslationRepositoryCore`, chứa toàn bộ truy vấn và các kiểu lồng `JobSummary`, `ChunkRow`, …
- App giữ lớp tên cũ `TranslationRepository extends TranslationRepositoryCore`.
- Mọi chỗ trong app và Editorial đang viết `TranslationRepository.JobSummary`, `new TranslationRepository(context)` hay `repo.getJob(...)` vẫn biên dịch **không cần sửa**.
- Chỉ các tệp được chuyển sang lõi mới đổi tên tham chiếu, ví dụ `TranslationRepository.JobSummary` → `TranslationRepositoryCore.JobSummary`.

Áp dụng cho `TranslationRepository`, `GlossaryStore`, `PronounStore`, `ChapterSetStore`, `WorkspaceSetModel`.

## 3. Các cổng (trong lõi)

### 3.1 `SqlDb` — cổng DB

Đặt trong lõi, đúng bằng phần API đang dùng, không thêm:

```java
interface SqlDb {
  DbCursor rawQuery(String sql, String[] args);
  DbCursor query(...);                    // đúng chữ ký của 1 chỗ đang dùng
  void execSQL(String sql); void execSQL(String sql, Object[] args);   // chỉ nếu đang dùng
  long insert(String table, DbValues v); long insertOrThrow(String table, DbValues v);
  long insertWithOnConflict(String table, DbValues v, int conflict);   // hằng số giống SQLiteDatabase.CONFLICT_*
  int update(String table, DbValues v, String where, String[] args);
  int delete(String table, String where, String[] args);
  void beginTransaction(); void setTransactionSuccessful(); void endTransaction();
  void setForeignKeyConstraintsEnabled(boolean on);
}
interface DbCursor extends AutoCloseable { boolean moveToNext(); boolean moveToFirst(); String getString(int i); int getInt(int i); long getLong(int i); double getDouble(int i); boolean isNull(int i); int getColumnCount(); void close(); }
final class DbValues { put(String, String|Integer|Long|Double|Boolean|byte[]|null) ... }
```

**Hành vi phải khớp Android:**
- `DbValues` giữ đúng kiểu Java của từng giá trị, kể cả `null` (`putNull`).
- Tham số `String[] args` của `rawQuery`, `update` và `delete` được bind **dạng chuỗi**, như Android.
- Giao dịch lồng nhau hoạt động như Android: chỉ giao dịch ngoài cùng mới commit.
- `insertOrThrow` ném lỗi khi vi phạm ràng buộc. `insert` trả `-1`.

**Hai hiện thực:**
- **`AndroidSqlDb`** (app): bọc `SQLiteDatabase`, chuyển `DbValues` thành `ContentValues`.
- **`JdbcSqlDb`** (chỉ trong test của lõi): dùng `org.xerial:sqlite-jdbc`, khai báo `testImplementation`.
  - Thư viện này chưa có trong cache Gradle. **Được phép tải một lần** từ Maven Central (không phải provider, không tốn tiền), rồi các lần sau chạy `--offline`.
  - Ghi phiên bản và SHA-256 của jar vào báo cáo.
  - Đây cũng là driver mà app PC sẽ dùng ở P2.

**Khác biệt giữa sqlite-jdbc và SQLite Android mà `JdbcSqlDb` phải xử lý:**
- Bật `PRAGMA foreign_keys=ON` ngay khi mở kết nối.
- Tự giữ bộ đếm giao dịch lồng và cờ "đã hỏng", vì JDBC không có. `endTransaction` mà chưa gọi `setTransactionSuccessful` thì rollback.
- `insertWithOnConflict` ánh xạ sang `INSERT OR REPLACE` / `INSERT OR IGNORE`, … (đang dùng `CONFLICT_REPLACE` ở `TranslationRepository.java:254`).
- `insert` bắt lỗi rồi trả `-1`.
- `Boolean` lưu thành 0/1.
- `getLong` hay `getInt` của cột NULL trả 0.
- `user_version` do test tự đặt.

Lỗi ràng buộc ném ra từ JDBC là `SQLException`, còn Android ném `SQLiteException`. Vì vậy:
- `AndroidSqlDb` để `SQLiteException` đi qua nguyên vẹn, không bọc lại (`androidTest` có chỗ bắt `SQLiteConstraintException`);
- code lõi không bắt ngoại lệ SQLite theo tên lớp.

### 3.2 `KeyValueStore` — cổng lưu khóa–giá trị

- Cổng chỉ gồm đúng các hàm `SharedPreferences` đang dùng:
  - `getString`, `getBoolean`, `getAll`;
  - `putString`, `putBoolean`, `putLong`, `remove`;
  - `commit()` trả `boolean` (`GlossaryStore.java:105` dùng giá trị này), và `apply()` (`WorkspaceSetModel` dùng).
- **`AndroidPrefsStore`** (app): bọc `SharedPreferences`.
- **`MemoryKeyValueStore`** (lõi): dùng cho test.
- Thay `TextUtils.isEmpty`/`equals` bằng hàm Java thuần cùng nghĩa, trong lõi.

## 4. Việc chuyển, theo thứ tự (mỗi bước một commit, hồi quy xanh sau mỗi bước)

**B0 — Mốc.** Ghi lại các mốc trên `2c5c7d9f`, chạy JVM, không cần điện thoại:
- tập hợp **mọi chuỗi literal** trong `TranslationRepository`, `GlossaryStore`, `PronounStore`, `ChapterSetStore`, `WorkspaceSetModel`;
- danh sách hàm `public`/package-private của các lớp đó, kèm chữ ký.

Hai mốc này dùng cho chốt chặn ở mục 6.

**B1 — Cổng và hai hiện thực.** Thêm test cho `JdbcSqlDb` và `MemoryKeyValueStore`: kiểu giá trị, bind chuỗi, giao dịch lồng, `insertOrThrow` và xung đột.

**B2 — Lược đồ DB ở lại app; tách helper.**
- **Lược đồ không chuyển sang lõi trong P1a.** Lý do: `onCreate`/`onUpgrade` gọi 5 tệp Editorial (`EditorialMigrationSpec`, `EditorialApiMigrationSpec`, `EditorialApiV28MigrationSpec`, `EditorialPairMigrationSpec`, `EditorialPairV29MigrationSpec`, `TranslationRepository.java:137-165`). Lõi không được tham chiếu tới chúng. Lược đồ cho app PC để P2 quyết định.
- **Chuyển nguyên sang lớp mới `TranslationDbHelper extends SQLiteOpenHelper` (app):**
  - `onCreate`, `onUpgrade`, `onDowngrade`, `onConfigure`, các hàm `create*Table`;
  - các hàm chỉ chạy lúc nâng cấp: `backfillReliabilityMetadata`, `recoverInterruptedChunks`.

  Code chép nguyên. Hằng `DB` và `VER = 30` khai báo trong lớp `TranslationRepository` ở app, giữ mức truy cập như cũ (`androidTest` dùng `TranslationRepository.VER`).
- **Fixture lược đồ cho test JVM:**
  - lấy `databases/tbl_android_txt.db` từ bản sao lưu trước khi cài dev.1, đưa vào thư mục tạm **ngoài Git**;
  - đọc **chỉ** `sqlite_master` (cột `type`, `name`, `sql`) và `user_version`, ghi thành `translation-core/src/test/resources/schema/tbl_android_txt_v30.sql`;
  - **xóa thư mục tạm ngay**;
  - không đọc dòng dữ liệu nào. Tệp này chỉ có DDL, không có văn bản sách.
- **Chỉ test những gì có mốc thật.** Không có lược đồ v1–v29 nào để dựng lại, nên **không** test nâng cấp qua từng phiên bản và **không** bịa lược đồ cũ. Ghi "không đo" cho phần này trong báo cáo.

**B3 — Repository.**
- Chuyển các hàm truy vấn và kiểu lồng sang `TranslationRepositoryCore(SqlDb db)`. Thay kiểu theo đúng bảng sau, không đổi gì khác:
  - `ContentValues` → `DbValues`;
  - `Cursor` → `DbCursor`;
  - `SQLiteDatabase` → `SqlDb`;
  - `getReadableDatabase()` / `getWritableDatabase()` → `db()`.
- **Cấu trúc:** `TranslationRepository` ở app `extends TranslationRepositoryCore`, và **giữ một trường** `TranslationDbHelper helper`. Lớp này không còn là `SQLiteOpenHelper`. Đã grep: không có `instanceof SQLiteOpenHelper`, không có lớp nào kế thừa nó, không gọi `getDatabaseName`.
  - Constructor dựng helper trước, rồi truyền `new AndroidSqlDb(helper)` lên `super`, ví dụ `TranslationRepository(Context c, String name){ this(new TranslationDbHelper(c, name)); }` cộng một constructor private nhận helper.
- **`AndroidSqlDb` giữ helper**, và mỗi lần gọi lại `helper.getWritableDatabase()` / `getReadableDatabase()`. **Không lưu** đối tượng `SQLiteDatabase`, vì sau `close()` helper trả về đối tượng mới. Như vậy lõi, `LibraryRawStore` và các DAO Editorial vẫn dùng chung một kết nối do helper quản lý.
- **Hàm `private` dùng chung không được kế thừa.** `safe`, `args`, `isCompleted`, `hasAcceptedTranslation`, `createJobRows`, `getPreparedChunks`, `deletePreparedBatch`, … đặt ở lõi, mức package-private hoặc `protected`. Hàm còn ở app (prepared batch) gọi chúng qua `db()`, không qua `SQLiteDatabase` thô. Nếu một hàm buộc phải dùng `SQLiteDatabase` (ví dụ `createJobFromPrepared`), nó ở lại app và ghi vào báo cáo.
- `TranslationRepository` ở app (`extends TranslationRepositoryCore`) giữ:
  - hai constructor `(Context)` và `(Context, String databaseName)`;
  - `getReadableDatabase()` / `getWritableDatabase()` / `editorialWritableDatabase()` / `editorialReadableDatabase()` trả `SQLiteDatabase` của helper, cho `LibraryRawStore` và các DAO Editorial;
  - `close()`;
  - **các hàm gắn Android:** `savePreparedBatch`, `getPreparedBatch*`, `deletePreparedBatch` (dùng `PreparedBatch` có `android.net.Uri`), và mọi hàm khác có kiểu Android trong chữ ký. Các hàm này giữ code cũ trên `SQLiteDatabase`.
- **`SettingsStore.toPersistedJson`** mà repo đang gọi: tách phần thuần sang lõi `SettingsJson.toPersistedJson` theo mẫu S5 của P0b. Hàm cũ trong `SettingsStore` chỉ còn một dòng gọi sang, nên nơi gọi cũ không phải sửa.
**Test JVM** (sqlite-jdbc, DB tạo từ fixture lược đồ ở B2):
- tạo job → ghi trạng thái chunk → đọc `getJobSummaries`, `JobProgress`, `getChunkRows`;
- cập nhật cấu hình (`config_revisions`);
- xóa job;
- giao dịch rollback khi lỗi.

So kết quả với giá trị kỳ vọng viết tay.

**B4 — Store dùng `SharedPreferences`.**
- `GlossaryStoreCore` và `PronounStoreCore` (lõi) giữ:
  - kiểu lồng `Glossary`, `Term`, `Profile`;
  - hàm thuần `parseTerms`, `mergeTerms`, `toPromptText`, `fromJson`, `toJson`;
  - phiên bản nhận `KeyValueStore` của mọi hàm đang nhận `Context`.
- Lớp con ở app giữ **đúng chữ ký `Context` cũ**, mỗi hàm một dòng gọi sang, với cùng tên tệp prefs và cùng khóa.
- **Mọi trạng thái static chỉ nằm ở lớp lõi**: `cachedJson`, `cachedGlossaries`, `corruptJson`, `PROMPT_TEXT_CACHE` và cache của Pronoun. Lớp con không giữ bản sao nào. Thêm `resetForTest()` package-private, vì cache khóa theo chuỗi JSON và sẽ rò giữa các test.
- **Hằng `PREF`, `KEY_*`** đặt ở lõi (package-private) để lớp con dùng lại, không khai báo lại. Ghi rõ trong báo cáo chuỗi nào nằm ở đâu.
- **Hàm cùng tên nhận `KeyValueStore`** trong lõi và nhận `Context` trong lớp con: grep để chắc không chỗ nào gọi với `null` (sẽ mơ hồ). Nếu có, đặt tên khác cho bản lõi, ví dụ `loadAllFrom(KeyValueStore)`.
- `ChapterSetStoreCore` (dùng `SqlDb`) và `WorkspaceSetModelCore` (kiểu `Entry` và phần thuần) làm theo cùng cách. Hai hàm "mở gần đây" dùng `Context` ở lại lớp con.
- **Test JVM:** đọc/ghi qua `MemoryKeyValueStore`; JSON trước và sau giống hệt, dùng fixture tự viết.

**B5 — Chuyển 13 tệp.** Dùng `git mv`:

> `BindingCache`, `ChapterBinding`, `ChapterSession`, `ChapterSetVolumeModel`, `DrawerRecentModel`, `JobHistoryPageModel`, `JobListMerge`, `JobPicker`, `LibraryArchive`, `LibraryImportPlanner`, `OutputIntegrityAudit`, `ResumePrompt`, `RunGroups`

- Chỉ được sửa các dòng đổi tên tham chiếu sang `*Core`. Việc đổi tên là bắt buộc: trong lõi không có lớp tên cũ. Liệt kê từng dòng trong báo cáo.
- Nếu một tệp còn cần thứ gắn Android, để nó lại app và ghi lý do. Áp dụng quy tắc P0b: hàm thuần thì tách sang lõi kèm dòng gọi sang, hàm gắn Android thì để lại.

**Ngoài phạm vi, để P1a-2 sau:** `FileUtil`, `LibraryRawStore`, `SettingsStore` (lưu/đọc cài đặt và API key), `LogStore`, `RuntimeStateStore`, `ChapterSetWorkspaceStore`. Lý do: chúng gắn với `Uri`/SAF và cách lưu key. Bản PC cần thiết kế riêng cho các phần này (DPAPI, `Path`), không phải chuyển máy móc.

## 5. Không được làm

- Không sửa nội dung tệp `Editorial*` (cả `main` lẫn `androidTest`) và `editorial-engine/`.
- Không đổi câu SQL, tên bảng/cột, `VER`, tên tệp DB, tên tệp prefs hay khóa prefs.
- Không đổi logic, prompt hay giá trị mặc định.
- Không gọi provider. Không tag, push hay merge vào nhánh khác. Không làm phần app PC.

## 6. Kiểm

**Chốt chặn:**
- `git diff 2c5c7d9f..HEAD -- '*Editorial*'` **rỗng**.
- Tập chuỗi literal của các lớp ở B0 bằng đúng tập chuỗi của bản tách (lõi + lớp con + `TranslationDbHelper`). Chỉ được thêm chuỗi thông báo lỗi và câu `PRAGMA` của các lớp cổng, liệt kê trong báo cáo.
- Đếm lại sau B3: số chỗ dùng `ContentValues`/`DbValues`, `Cursor`/`DbCursor`, `SQLiteDatabase`/`SqlDb` cộng lại phải khớp với mốc ở mục 1 (79 / 41 / 67). Lệch thì giải thích từng chỗ.
- Lõi không tham chiếu tới lớp `Editorial*` nào, trừ `EditorialPreferences` đã ở lõi từ P0b.
- Mọi hàm ở mốc B0 vẫn gọi được qua tên lớp cũ, cùng chữ ký. Kiểm bằng một test biên dịch trong app gọi từng hàm.
- `translation-core/src/main`: không `import android`; quét `javap` không thấy `put(String,float)` của org.json (giữ chốt của P0b).

**Hồi quy:**
- `git archive` sạch, `gradlew --offline -q testDebugUnitTest :translation-core:test compileDebugAndroidTestJavaWithJavac lintDebug`;
- 0 FAIL; test vàng 12/12; lint 0 lỗi.

**Build:** `4.24-dev` bằng `scripts/build-and-save.ps1`.

**Điện thoại `15e84958`:**
1. Sao lưu bằng `cmd /c "adb ... run-as ... tar czf - . > private-data.tar.gz"`.
2. **Trước khi cài**, chụp ảnh và ghi **số đếm**:
   - số job ở Công việc và Lịch sử;
   - số Glossary, Pronoun và Tập trong Thư viện;
   - tên Glossary/Pronoun đang dùng (chỉ tên `QA_` hoặc chỉ đếm).
3. **Dừng chờ owner "duyệt cài"**. Cài bằng `install-validated.ps1` (chỉ `install -r`).
4. Sau khi cài, các số đếm trên phải **giống hệt**. Thêm:
   - mở một job cũ xem tiến độ;
   - màn Dịch hiện ước tính;
   - log `AndroidRuntime` không có lỗi.

   Không bấm nút tốn tiền hay nút xóa. Không chép `settings.xml`. Không đọc dữ liệu sách.

## 7. Báo cáo

- Các commit theo B0–B5.
- Kết quả từng chốt chặn.
- Fixture lược đồ: số bảng, số chỉ mục, `user_version`. Xác nhận đã xóa thư mục tạm.
- Tệp nào ở lại app và vì sao.
- Hồi quy: số test app và lõi, SKIP, FAIL, lint.
- Bản build, SHA-256, bản sao lưu.
- Số đếm trên điện thoại trước và sau khi cài.
- Phiên bản và SHA-256 của `sqlite-jdbc`.
