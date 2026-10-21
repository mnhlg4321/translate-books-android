# v4.23 — Yêu cầu làm việc: đưa toàn bộ điều hướng vào ngăn ☰ (phương án A), sửa lỗi Công việc mất job tệp lẻ

Trạng thái: ĐÃ PHẢN BIỆN (2026-10-21; agent phản biện đọc mã tại `6f9fefe3` và tìm ra 11 lỗi, đã sửa tất cả).

Mốc:
- `6f9fefe3` trên `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`;
- điện thoại `15e84958` đang chạy `4.23-dev.16` / code 294.

Thay thế phần điều hướng của `docs/V423_WORKSPACE_NAV_PROPOSAL.md` bản 2.1 và W2 của `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md`.

**Quyết định của owner (2026-10-21), không bàn lại:**
- Đưa cả 5 đích (Dịch, Công việc, Biên tập, Thư viện, Cài đặt) vào ngăn ☰, **bỏ thanh dưới** để có thêm chỗ cho nội dung.
- Ngăn chỉ giữ **3 tập gần đây**. Bấm vào một tập thì mở **Công việc đã lọc theo tập đó**.
- **Bỏ trang Volume và trang "Tất cả tập".**
- Gộp sửa lỗi "Công việc mất job tệp lẻ".

**Quyết định sản phẩm phục vụ:** thêm khoảng 8% chiều cao cho nội dung trên điện thoại, bớt trang con, và tab Công việc lại hiện đủ mọi job.

**Bài học v2.6.7 phải giữ:** lần trước giấu các đích vào ngăn, owner thấy "chức năng như bị mất". Lần này owner chủ động chọn, nhưng phải có các bù trừ ở mục 3.3:
- tiêu đề luôn ghi trang đang mở;
- chạm chip trạng thái thì mở Công việc trong 1 chạm;
- ngăn hiện rõ đích đang chọn và trạng thái Công việc.

## 1. Bắt đầu

1. Đọc `AGENTS.md`, `WORKSPACE_SNAPSHOT.md`, tài liệu này, và mục 3 của `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md` (quy tắc điện thoại; vẫn áp dụng nguyên văn).
2. Xác nhận `HEAD` chứa tài liệu này.
3. `artifacts/qa/v4.23-dev14-20261021/media/` chưa commit: để nguyên.
4. Build, test, commit, snapshot làm như lần trước:
   - `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'`, `gradlew --offline`;
   - build APK chỉ qua `scripts/build-and-save.ps1 -Series 4.23-dev`;
   - hồi quy bằng `git archive` sạch;
   - commit dạng `type(scope): summary`.

## 2. Ranh giới

- Không sửa `editorial-engine/` hay tệp `Editorial*`. Riêng `EditorialApiPresenter.TAB_TITLE` vẫn được **đọc** để làm nhãn.
- Không gọi provider. Trên điện thoại **không bấm** Retry, Dịch / Dịch tiếp, Tiếp tục tập, Resume, hay xác nhận Start.
- Không đổi schema. Không merge, tag hay push.
- Không đưa nội dung sách, API key hay dữ liệu cá nhân vào Git.
- Không làm Refined Minimalism (`V423_REFINED_UI_DESIGN.md`) trong đợt này. Dùng helper và màu hiện có.

## 3. Việc làm

### 3.1 Sửa lỗi Công việc (làm trước, có test)

**Lỗi:**
- `JobsMainPageFactory:34` lấy job qua `filteredWorkspaceJobsSnapshot()`.
- Khi lọc "Tất cả", hàm này trả `cachedWorkspace.jobs` (`MainActivity:1300–1303`), tức **chỉ job thuộc lượt chạy tập**.
- Vì vậy job dịch tệp lẻ biến mất khỏi "Recent jobs", và `JobPicker.current` (`:44`) bỏ sót job tệp lẻ đang chạy hoặc dở.
- Đã xác nhận trên điện thoại dev.16: "Tất cả" chỉ còn 9 thẻ lượt tập.

**Sửa:**
- **"Tất cả":** `workspaceJobsSnapshot()` gọi hàm Java thuần mới `JobListMerge.all(cachedJobSummaries, ws == null ? empty : ws.jobs)`. Hàm này:
  - gộp hai danh sách (150 job mới nhất mọi loại, cùng job của các lượt tập);
  - bỏ trùng theo `id`: giữ bản có `updatedAt` lớn hơn; bằng nhau thì giữ bản của `cachedWorkspace`;
  - xếp `updatedAt` giảm dần, rồi `id` giảm dần.
- **Lọc theo tập:** giữ như hiện nay (`jobsForSet`).
- **"Current job":** `JobsMainPageFactory` gọi `JobPicker.current` trên danh sách "Tất cả" đã gộp, **không** theo bộ lọc. Job đang chạy hay đang dở phải luôn hiện, dù đang lọc tập nào.
- **Chỉ `JobsMainPageFactory:34` đọc danh sách đã lọc.** Thẻ tổng, `EditorialJobCard`, việc làm mới chunk lỗi và thẻ job dở ở màn Dịch không đọc danh sách này, nên không phải sửa.
- "Recent jobs" vẫn giới hạn 30 nhóm (`JobsMainPageFactory:61`); giữ nguyên giới hạn này.

**Test** (Java thuần):
- "Tất cả" có job tệp lẻ và job tập, không trùng;
- lọc tập chỉ còn job của tập;
- `current` vẫn là job tệp lẻ đang chạy khi đang lọc một tập.

### 3.2 Bỏ thanh dưới và thanh dọc, đưa đích vào ngăn

1. **`buildUi` (`MainActivity:652–692`):**
   - bỏ `buildPrimaryNavigation` ở **cả hai** bố cục: thanh dưới 58 dp ở màn hẹp, và thanh dọc 112 dp ở màn rộng ≥ 720 dp, tức điện thoại xoay ngang;
   - nội dung chiếm toàn bộ phần còn lại;
   - xóa `tabBar`, `tabButton`, `primaryIcon` và phần tô sáng tab trong `refreshTabs`. Giữ `refreshTabs` vì nó còn cập nhật tiêu đề (3.3);
   - giữ `primaryActive` cho ngăn;
   - **giữ `isWideLayout()`**, vì Cài đặt hai cột (`SettingsPageFactory:19–20`) và `AppearancePage:170` dùng nó. Chỉ bỏ nhánh thanh dọc trong `buildUi`.
2. **Nội dung ngăn**, theo thứ tự:
   1. **Đầu ngăn:** logo cùng "Translate Books".
   2. **4 đích**, bấm thì `navigateTo(...)`:
      - Dịch → `Translate`;
      - Công việc → `Jobs`;
      - Biên tập → `Editorial`, nhãn đọc từ `EditorialApiPresenter.TAB_TITLE`;
      - Thư viện → `Files`.

      **Cách hiển thị:**
      - mỗi đích là một hàng cao ≥ 48 dp, có biểu tượng và nhãn tiếng Việt;
      - đích đang mở được tô sáng và có `setSelected(true)` cho TalkBack. Hàng Thư viện dùng `primaryActive("Library")` (đã bỏ AllSets/Volume), để sáng cả khi ở RAW, Glossaries, Pronouns, ChapterSets, Sample.

      **Trạng thái Công việc**, dùng chung cho hàng Công việc và chấm trên ☰ (3.3):
      - "đang chạy" khi `TranslatorService.isActive()`. Hàng Công việc ghi "đang chạy · N%". Hàng này có TextView riêng, cập nhật trong `updateDrawerProgressFromIntent` (`:1521`);
      - "lỗi" khi có job trong `JobPicker.unfinished(danh sách "Tất cả")` có `failedChunks > 0`. Hàng Công việc ghi "N job lỗi";
      - còn lại thì không có chữ phụ;
      - tính lại khi `refreshJobCacheAsync` xong, và mỗi khi nhận tiến độ.
   3. **"Gần đây":** tối đa **3** tập **tính cả tập đang chạy**, lấy từ `WorkspaceSetModel.recent(snapshot.sets, 3)`.
      - Tập đang chạy đứng đầu, có dòng tiến độ.
      - Mỗi hàng ghi tên, nhãn "(đã xóa)" nếu có, và "xong/tổng".
      - Bấm vào thì gọi `navigateTo("Jobs", entry.id)`. Trong `performNavigation`, khi target là `Jobs` và setId không rỗng:
        - gán `jobsSetFilterId = setId`;
        - gọi `invalidatePage("Jobs")`, để trang không giữ bộ lọc cũ;
        - gọi `WorkspaceSetModel.recordOpened(this, setId)`, để thứ tự "Gần đây" còn theo lần mở.

        Các việc này chạy **sau** hộp hỏi nháp tập, nên nếu owner bấm "Tiếp tục sửa" thì bộ lọc không đổi.
      - Không có tập thì một dòng chữ "Chưa có tập."
   4. **Đáy ngăn:** hàng "Cài đặt" (`Settings`).
3. **Bỏ khỏi ngăn:** ô "Tìm tập", nút "Tất cả tập…", "+ Tập mới", và các nút trạng thái trống "Tạo tập từ RAW", "Dịch tệp lẻ". Tạo tập vẫn làm ở Thư viện > Tập như trước.
4. **Mọi lần chọn trong ngăn** đều đi qua `navigateTo`, để giữ các chốt lưu Glossary và nháp tập.
5. **Giữ nguyên** từ dev.16: lớp mờ, Back đóng ngăn trước tiên, inset, `NO_HIDE_DESCENDANTS`, đóng ngăn khi `recreate()`. Độ rộng ngăn là `min(360 dp, 80%)`, ở cả chế độ ngang.

### 3.3 Bù trừ cho việc không còn thanh tab

1. **Tiêu đề thanh trên** ghi **tên trang đang mở**, thay cho chữ "Translate Books". `topTitle` hiện chỉ được đặt một lần (`:796`):
   - thêm hàm mới `pageTitle()` trả về Dịch, Công việc, Biên tập, Thư viện, Cài đặt; trang con Thư viện trả "Thư viện · Glossary", "Thư viện · Tập"…;
   - không dùng `tabLabel` làm tiêu đề, vì nó trả tên tiếng Anh. **Giữ nguyên chữ ký `String tabLabel(String name)`**, vì `EditorialApiUserStringsTest:78` kiểm đúng chuỗi này;
   - gọi `topTitle.setText(pageTitle())` trong `refreshTabs()`;
   - phụ đề bỏ tiền tố `tabLabel(...) •`, chỉ còn `compactProviderMeta()`.
2. **Chip trạng thái** (`topStatusChip`, `:805–811`; cập nhật ở `refreshTopStatusChip` `:1170–1176`). Hiện chip không có xử lý chạm và chỉ rộng 32–34 dp. Cần:
   - `setClickable(true)`, `setFocusable(true)`;
   - vùng chạm ≥ 48 dp;
   - chạm vào thì `navigateTo("Jobs")`;
   - content description đặt trong `refreshTopStatusChip`: "Trạng thái: … Mở Công việc".
3. **Nút ☰** có chấm nhỏ màu lỗi khi có job lỗi, màu nhấn khi đang chạy. Content description ghi rõ trạng thái ("Mở menu · có job lỗi").

### 3.4 Bỏ trang Volume và "Tất cả tập"

- **Xóa:**
  - `VolumePageFactory.java`;
  - `buildAllSetsPage`;
  - các nhánh `"Volume"`/`"AllSets"` trong `buildPageForTab`, `tabLabel`, `primaryActive`, `refreshJobCacheAsync` (`:1286–1289`);
  - `navigateTo("Volume"…)`, `workspaceSetButton` nếu chỉ còn trang đó dùng;
  - luật `OPEN_ORIGIN` trong `AppBackNavigationPolicy` cùng test của nó;
  - nhánh `OPEN_ORIGIN` trong `onBackPressed` (`:982–988`);
  - `drawerOriginTab`, cùng tham số origin của `navigateTo` (chỉ OPEN_ORIGIN/Volume dùng);
  - phần tìm kiếm của ngăn (`drawerSearch`, `drawerSearchText`, `matchesWorkspace` nếu không còn dùng).
- **Giữ:**
  - `WorkspaceSetModel`, `ChapterSetWorkspaceStore`;
  - `ChapterSetVolumeModel`, vì ngăn dùng nó để tính "xong/tổng" qua `applyVolumeStats`; giữ cả test của nó;
  - bộ lọc tập ở Công việc;
  - truy vấn `getJobSummariesForSessions`.
- **`NavState`:**
  - xóa `originTab` và `setId`, giữ `page`;
  - `jobsSetFilterId` vẫn lưu qua Bundle như `:584`, `:596`;
  - sửa `NavStateTest:12`, vì test đang kiểm `originTab`.
- **Trạng thái cũ:**
  - chuẩn hóa ngay trong `NavState.from` bằng danh sách trang hợp lệ: Translate, Jobs, Editorial, Settings, Files, RAW, Glossaries, Pronouns, ChapterSets, Sample;
  - trang lạ, kể cả "Volume" và "AllSets" từ dev.16, trở thành `Translate`;
  - không sửa ở `buildPageForTab`: sửa ở đó là quá muộn, vì `currentTab` vẫn mang giá trị lạ nên tiêu đề và tô sáng sẽ sai;
  - thêm test trong `NavStateTest`.

### 3.5 Back

Giữ hành vi hiện có:
1. ngăn mở → đóng ngăn;
2. Biên tập tự xử lý Back;
3. thoát chọn nhiều;
4. đóng nháp tập;
5. trang con Thư viện → Thư viện;
6. Files → Dịch;
7. Cài đặt → nhóm General → Dịch.

Thêm một luật: **Công việc, Biên tập, Thư viện (Files) → Dịch**. Chỉ khi ở Dịch thì Back mới thoát app. Lý do: không còn thanh tab để thấy mình đang ở đâu, nên Back phải đưa về màn chính trước khi thoát.

Cập nhật `AppBackNavigationPolicyTest`:
- `:34` đang kiểm Jobs → `SYSTEM`; đổi thành `OPEN_TRANSLATE`;
- thêm ca Editorial → `OPEN_TRANSLATE` và Translate → `SYSTEM`.

Luật mới không đụng `editorialApiShared.handleBack()` hay việc thoát chọn nhiều, vì hai bước đó chạy trước policy.

### 3.6 Chú thích và dọn dẹp

- Thay chú thích trong `buildUi`. Nội dung: v2.6.7 giấu các đích vào ngăn khiến owner thấy mất chức năng; v4.23 làm lại theo quyết định owner 2026-10-21; các bù trừ là tiêu đề trang, chip mở Công việc, chấm trạng thái trên ☰; không dùng `DrawerLayout`, không vuốt mép.
- Sửa các test cũ còn nhắc thanh tab.
- Lệnh sau phải trả về **0 dòng**:

  `rg -n '"Volume"|"AllSets"|VolumePageFactory|buildAllSetsPage|renderAllSetsPage|allSets|OPEN_ORIGIN|tabBar|tabButton|primaryIcon|originTab|drawerOriginTab|drawerSearch' app/src`

  `ChapterSetVolumeModel`, `applyVolumeStats` và chữ "Volume" bên Biên tập là hợp lệ, không bị lệnh này bắt.
- Biên dịch được cả `androidTest`.

## 4. Thử trên điện thoại

Thử trên máy thật `15e84958`, không dùng emulator. Mọi quy tắc ở mục 3 của `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md` áp dụng nguyên văn: sao lưu trước, hỏi owner "duyệt cài", chỉ `install -r`, chỉ dữ liệu `QA_`, khôi phục mọi cài đặt máy.

**Bổ sung và sửa:**
- **Bỏ dòng "Xoay màn hình" trong mục 3 cũ** (kiểm ngăn phủ thanh dọc). Đợt này không còn thanh dọc; chế độ ngang được kiểm theo điểm dừng bên dưới.
- **Thêm vào danh sách không bấm:** các nút trên thẻ Công việc (Cancel, Bỏ, Xóa job, Tiếp tục tập).
- **Ảnh chụp màn hình:** PowerShell 5.1 làm hỏng dữ liệu nhị phân qua `>`. Hai ảnh của lần trước (`D:\P5E-builds\phone-qa-v423-dev15-*`, `dev16-*`) đều hỏng. Chụp theo một trong hai cách:
  - `adb -s 15e84958 shell screencap -p /sdcard/qa.png`, rồi `adb -s 15e84958 pull /sdcard/qa.png <đích>`, rồi `adb -s 15e84958 shell rm /sdcard/qa.png`;
  - hoặc `cmd /c "adb -s 15e84958 exec-out screencap -p > <đích>"`.

  Kiểm 4 byte đầu là `89 50 4E 47`.
- **Cỡ chữ 130%:** đổi trong app, tại Cài đặt > **Appearance** > "Cỡ chữ của app" > 130% (`SettingsPageFactory:15`, `AppearancePage:45`). Không dùng `settings put system font_scale`, vì Android 15 từ chối. Đổi cỡ chữ sẽ gọi `recreate()`. Ghi giá trị cũ, khôi phục sau khi thử.
- **Đếm thẻ trong danh sách dài:** cuộn đến hết rồi mới đếm. `uiautomator dump` chỉ thấy phần đang hiện. Lần trước báo "0 thẻ lượt" là sai vì chưa cuộn.

**Điểm dừng:**
1. hồi quy archive sạch;
2. build `4.23-dev.17` (code > 294);
3. sao lưu điện thoại;
4. **hỏi owner duyệt cài**;
5. cài, rồi thử:
   - không còn thanh dưới ở chế độ dọc; không còn thanh dọc ở chế độ ngang;
   - ngăn có 4 đích, Gần đây (≤ 3) và Cài đặt; đích đang mở được tô sáng;
   - chọn từng đích → tiêu đề đổi đúng tên trang;
   - bấm một tập trong Gần đây → Công việc lọc đúng tập (không phải bộ lọc cũ); bấm tập khác → bộ lọc đổi theo;
   - bỏ lọc ("Tất cả") → job tệp lẻ mới nhất nằm trong 30 mục đầu (cuộn hết danh sách để đếm);
   - chạm chip trạng thái → mở Công việc;
   - Back: từ Công việc, Biên tập, Thư viện về Dịch; từ Dịch thì thoát app; ngăn mở thì Back đóng ngăn;
   - xoay ngang: ngăn mở được, không có thanh dọc;
   - sáng/tối; cỡ chữ 130% trong app (khôi phục sau);
   - logcat không có FATAL;
   - ảnh hợp lệ cho: ngăn, Công việc "Tất cả", Công việc đã lọc, chế độ ngang.
6. **Dừng** và báo cáo: commit, bản, mã, SHA-256, đường dẫn sao lưu, PASS/FAIL/NOT_MEASURED từng mục, đường dẫn ảnh. Cập nhật `WORKSPACE_SNAPSHOT.md`.

**Phiếu thử cho owner sau dev.17:**

| # | Owner làm | Đạt khi |
|---|---|---|
| A1 | Dùng app một buổi chỉ với ☰ | đổi trang không bị lạc; tiêu đề luôn cho biết đang ở đâu |
| A2 | Đang dịch (chi phí do owner quyết), chạm chip trạng thái | mở Công việc ngay |
| A3 | ☰ → bấm một tập gần đây | Công việc lọc đúng tập |
| A4 | Dịch một tệp lẻ | job hiện trong Công việc "Tất cả" và ở "Current job" |
| A5 | So với dev.16 | thấy rõ thêm chỗ hiển thị; không thiếu chức năng nào |

## 5. Rủi ro

- **Đổi trang chậm hơn** (2 chạm thay 1). Bù bằng chip mở Công việc (1 chạm) và Back về Dịch. Owner đánh giá ở A1. Nếu không ổn thì quay lại thanh dưới, nhưng đó là quyết định riêng.
- **Back về Dịch** thay vì thoát app thẳng từ Công việc. Đây là hành vi mới; owner kiểm ở A1.
- **Xóa trang Volume** để lại mã chết hoặc test gãy. `rg` ở mục 3.6 và biên dịch `androidTest` phải sạch.
