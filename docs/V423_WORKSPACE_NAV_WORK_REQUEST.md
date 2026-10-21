# v4.23 — Yêu cầu làm việc: ngăn trái ☰ theo "Tập" (W0–W2), thử trực tiếp trên điện thoại

Trạng thái: ĐÃ PHẢN BIỆN (2026-10-21; agent phản biện đọc mã tại `fb3938c5` và tìm ra 14 lỗi, đã sửa tất cả). Thiết kế: `docs/V423_WORKSPACE_NAV_PROPOSAL.md` bản 2.1. Đọc tài liệu đó trước; tài liệu này chỉ nói làm gì, theo thứ tự nào, và dừng ở đâu.

**Người làm:** Codex (`gpt-5.6-luna`, suy luận max, tầng Fast), chạy trong tab terminal để owner trả lời trực tiếp.

**Nhánh:**
- `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`.
- Không đổi nhánh, không merge, không tag.
- Không làm gì trong `D:\App Translate Books`; thư mục đó giữ nguyên nhánh v4.18 cùng các thay đổi chưa commit của owner.
  - **Ngoại lệ duy nhất:** được ghi vào thư mục chưa theo dõi `D:\App Translate Books\App Translate Books-device-backups\`, cùng các junction `artifacts\builds`, `artifacts\test-builds`, `backup` mà script build dùng.
  - Không chạy lệnh git nào ở đó.

**Quyết định sản phẩm phục vụ:**
- Owner chuyển qua lại giữa các tập trong 2 chạm.
- Owner thấy cả một tập trên một màn, kể cả tập đã xóa sau khi chạy.
- Owner lọc tab Công việc theo tập để retry (việc hằng ngày).

## 1. Bắt đầu

1. Đọc `AGENTS.md`, `WORKSPACE_SNAPSHOT.md`, bản đề xuất 2.1 và tài liệu này.
2. Xác nhận `git status` và `HEAD`.
   - HEAD là commit chứa tài liệu này, hoặc mới hơn.
   - Thư mục `artifacts/qa/v4.23-dev14-20261021/media/` chưa commit, do phiên trước để lại (QA trên emulator). **Để nguyên**, không commit, không xóa.
3. Cách build, test và commit theo memory `build-and-test-recipe` và các yêu cầu v4.23 trước:
   - `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'`, `gradlew --offline`;
   - build APK chỉ qua `scripts/build-and-save.ps1 -Series 4.23-dev`;
   - hồi quy bằng `git archive` sạch;
   - commit dạng `type(scope): summary`; cập nhật `WORKSPACE_SNAPSHOT.md` theo hook pre-commit.

## 2. Ranh giới

- **Không sửa** `editorial-engine/` hay bất kỳ tệp `Editorial*`.
- **Không gọi provider** (không chạy dịch thật, không gọi API trả tiền) khi chưa có quyền chi tiền riêng của owner.
- **Không đổi schema** cơ sở dữ liệu. Thêm khóa SharedPreferences thì được.
- Không làm W3/W4 (trang chương, điền sẵn Biên tập, Ghim, chip lọc, ô tìm Cài đặt, Retry cả tập, Lưu trữ tập).
- Không thêm tab, không bỏ tab. Không dùng `DrawerLayout`, không có cử chỉ vuốt mép.
- Không đưa nội dung sách, API key hay dữ liệu cá nhân vào Git, log commit hay ảnh được commit.

## 3. Thiết bị: điện thoại thật, không dùng emulator

- **Điện thoại:** `15e84958` (OnePlus CPH2691, Android 15), đang chạy `4.23-dev.13` / code 291. Owner yêu cầu thử trực tiếp trên máy này, không qua emulator.
- **Mỗi lần cài là một lần hỏi owner** (`AGENTS.md`). Trước mỗi lần cài:
  1. Chỉ sao lưu khi không có job đang chạy. Sao lưu ngoài Git vào `D:\App Translate Books\App Translate Books-device-backups\v<bản đang cài>-code<mã>\<YYYYMMDD-HHMMSS>\`:
     - `base.apk`: lấy đường dẫn bằng `adb -s 15e84958 shell pm path com.ml.tblandroidtxt`, rồi `adb -s 15e84958 pull`;
     - `private-data.tar.gz`: chạy qua `cmd /c`, vì toán tử `>` của PowerShell 5.1 làm hỏng dữ liệu nhị phân:

       `cmd /c "adb -s 15e84958 exec-out run-as com.ml.tblandroidtxt tar czf - . > private-data.tar.gz"`

       (bản dev là bản debug nên `run-as` dùng được);
     - `dumpsys.txt`;
     - ghi kích thước và SHA-256 của từng tệp.
  2. Kiểm tra tệp sao lưu mở được (`tar -tzf`, đếm số tệp). **Không in nội dung.**
  3. Báo owner bản, mã, SHA-256 APK và đường dẫn sao lưu. **Hỏi và chờ owner trả lời "duyệt cài"** trong terminal này.
- **Cài:** dùng `scripts/install-validated.ps1` với:
  - `-ApkPath <APK trong artifacts\builds>`;
  - `-Serial 15e84958`;
  - `-ExpectedVersionCode <mã mới>`;
  - `-ExpectedApkSha256 <SHA-256>`;
  - `-ExpectedApkCertificateSha256 47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`;
  - `-ExpectedDeviceSignatureToken abebea4b`.

  Cách chạy:
  - chạy `-CheckOnly` trước;
  - script cài đè (`install -r`) và từ chối hạ cấp;
  - **không bao giờ** gỡ cài đặt và không dùng `-AllowMissingPackage`, vì gỡ sẽ mất dữ liệu owner;
  - nếu token chữ ký khác `abebea4b` thì dừng và báo owner.
- **Không bấm nút nào có thể gọi provider trên điện thoại:** Retry, Dịch / Dịch tiếp, Tiếp tục tập, Resume, hay xác nhận Start. Những nút này tự chạy và tốn tiền. Chỉ kiểm rằng nút hiện đúng và đúng trạng thái.
- **Xoay màn hình:** ở bề ngang ≥ 720 dp app dùng thanh dọc bên trái thay thanh dưới (`MainActivity:643–647`). Kiểm cả dọc và ngang; ngăn phải phủ lên cả thanh dọc.
- **Dữ liệu thật trên máy là của owner:**
  - chỉ đọc và xem;
  - chỉ được tạo dữ liệu thử có tên bắt đầu bằng `QA_`, và chỉ xóa đúng dữ liệu đó;
  - không xóa, sửa, đổi tên tập, RAW, Glossary, Pronoun hay job của owner.
- **Điều khiển máy:** `adb shell input`, `uiautomator dump`, `screencap`.
  - Đổi cỡ chữ (`settings put system font_scale`), chế độ tối (`cmd uimode night`) hay TalkBack: **ghi giá trị cũ trước** và **khôi phục ngay sau khi thử**.
  - Không mở màn có nội dung sách khi chụp ảnh.
- **Ảnh và bằng chứng:** đặt trong `artifacts/qa/v4.23-nav-<YYYYMMDD>/`.
  - Chỉ commit ảnh không chứa nội dung sách. Tên tệp và tên tập là chấp nhận được.
  - Ảnh nào có nội dung sách thì giữ ngoài Git.
- Không có cách thử "đang chạy" mà không gọi provider. Mục đó ghi `NOT_MEASURED` trong báo cáo; owner tự thử trong phiếu K.

## 4. Việc làm, theo thứ tự

Mỗi bước: viết test trước (Java thuần ở `app/src/test`), làm, chạy test, commit.

### W0 — Công việc lọc theo tập
- **Bộ lọc "Tập"** trên tab Công việc:
  - các mục: "Tất cả", rồi từng tập lấy từ các lượt chạy (`setId`/`setName`), gồm cả tập đã xóa;
  - tên tập đã xóa có nhãn "(đã xóa)".
- **Đọc job của tập** (lớp mới, dùng lại cho W2):
  - **không** lọc trên `cachedJobSummaries`, vì danh sách này chỉ giữ 150 job mới nhất (`MainActivity:1163`) mà owner có 307;
  - dùng một truy vấn **chỉ đọc**, chạy ngoài luồng UI: job có `start_session_id IN (các lượt của setId)`, cộng job có `id IN (resumedJobs của các lượt đó)`;
  - không đổi schema.
- **Thứ tự:** áp bộ lọc **trước** giới hạn 30 thẻ của trang Công việc (`JobsMainPageFactory:58`).
- **Thẻ lượt:**
  - có nút "Mở tập", chỉ hoạt động sau W2 (trước đó thì ẩn);
  - với tập đã xóa thì ẩn nút "Tiếp tục tổ hợp" (`JobsMainPageFactory:152`), vì không còn RAW để tiếp tục.
- Lưu lựa chọn lọc qua `recreate()`.

### W1 — Một cửa điều hướng, ngăn ☰, đổi tên
1. **`navigateTo(target)`** trong `MainActivity`. Mọi chuyển trang mới (ngăn, "Mở tập", màn tập) phải đi qua hàm này. Hàm làm theo thứ tự:
   1. `silentPersistCurrentUi()`;
   2. nếu `editingGlossary`: `saveEditingGlossary()`, gán null, rồi `invalidatePage("Glossaries")`;
   3. nếu `editingPronoun`: chỉ gán null. Trình xem Pronoun chỉ đọc, Back cũng chỉ đóng nó (`:883`, `PronounPageFactory:38`);
   4. nếu nháp tập `chapterSetDraft.dirty`: hiện hộp hỏi (hộp này bất đồng bộ, `:1188–1192`). Chỉ khi owner chọn "Bỏ" mới chạy tiếp, qua callback;
   5. `switchTab(target)`.

   Đường đổi tab cũ giữ nguyên hành vi.
2. **Mô hình tập** (lớp mới, Java thuần, có test):
   - gộp `chapter_sets` còn sống với các lượt chạy (`ChapterSession`) theo `setId`;
   - tập đã xóa vẫn có, kèm cờ `deleted`;
   - nhóm "Đang chạy" và "Gần đây" (tối đa 6, theo mốc mới nhất: lượt cuối, `updatedAt`, lần mở cuối lưu ở SharedPreferences);
   - một tập không xuất hiện hai lần.
3. **Ngăn:**
   - **Dựng:**
     - `FrameLayout` gốc mới bọc `appRoot`;
     - lớp mờ và panel ngang hàng `appRoot`; panel rộng 80%, tối đa 360 dp; tự xử lý inset;
     - nút ☰ 48 dp thay logo, có content description; logo đặt ở đầu ngăn.
   - **Nội dung:** Tìm tập; Đang chạy; Gần đây; "Tất cả tập…"; đáy ngăn có "+ Tập mới" (luồng tạo tập hiện có) và "⚙ Cài đặt".
   - **Cách dựng danh sách:**
     - khi mở thì dựng ngoài luồng UI;
     - khi đang mở, cập nhật chữ tiến độ trên view có sẵn **trong `receiver.onReceive` (`:287`)**, từ `EXTRA_PROGRESS`. Không dựa vào `runtimeTimerTick`, vì nhịp đó chỉ chạy ở tab Dịch (`:3574–3576`);
     - đóng ngăn thì dừng cập nhật.
   - **"Đang chạy"** xác định bằng `TranslatorService.isActive()` cộng `EXTRA_ACTIVE_JOB_ID` → `setIdOfJob`.
   - **Tổng "N/M"** của một tập dùng danh sách chương gộp của W2, không dùng `Run.chapterCount`.
   - **Đóng:** dòng đầu của `onBackPressed`, trước `editorialApiShared.handleBack()`. Chạm vùng mờ, hoặc chọn mục, cũng đóng.
   - **Trợ năng:** `NO_HIDE_DESCENDANTS` cho `appRoot` khi mở, focus vào panel, trả lại khi đóng.
   - **Cửa sổ khác:** ẩn bàn phím khi mở; đóng ngăn trước khi mở dialog.
   - **Khi dựng lại Activity:** ngăn luôn đóng.
4. **Trang "Tất cả tập"** có ô tìm theo tên. Chọn một tập thì mở màn tập (W2). Trước W2 thì mở Thư viện > Tổ hợp như hiện nay.
5. **Đổi tên "Tổ hợp" thành "Tập"** ở mọi nhãn phía dịch: Dịch, Công việc, Thư viện, ngăn.
   - **Tìm đủ chuỗi:** dùng `rg 'ổ hợp|u1ed5 h'`. Có tệp viết chữ này dạng `Tổ hợp`: `MainActivity:3559`, `TranslatePageFactory:351`, `RunPanelFormatter`.
   - **Không đổi:**
     - `ChapterSetMatcher.defaultName` (`:124`), vì giá trị này được lưu làm dữ liệu;
     - `LibraryDeletePlan:65`, vì là chữ bên Biên tập;
     - mọi chữ bên Biên tập;
     - khóa và tên lớp.
   - **Sửa test** trong cùng commit:
     - `app/src/test`: `ChapterRunTest:140,143`, `ChapterSetMatcherTest:131`, `V423RunPanelTest:73,75`, `V423ValidationTest:199`;
     - `androidTest`: các test nhắc chữ này. Không chạy được trên emulator, nhưng phải biên dịch được.
6. **Thanh trên ở cỡ chữ 130%:**
   - tiêu đề một dòng, cắt "…";
   - chip trạng thái thu thành chấm màu, có content description, khi bề ngang không đủ;
   - ☰ và ↻ cố định 48 dp.
7. **Sửa chú thích `MainActivity:639`:** ngăn tự làm, chỉ chứa tập, mở bằng nút, không vuốt; nêu bài học v2.6.7.

### W2 — Màn một tập
1. Trang "Volume", con của Thư viện (tab Thư viện sáng).
2. **Danh sách chương:**
   - tập còn sống: `chapter_set_rows`;
   - tập đã xóa: **hợp các chương của mọi lượt có cùng `setId`**, khóa `rawId` (dự phòng `chapterNo`), xếp theo `chapterNo`. Không chỉ lấy lượt mới nhất, vì lượt "tiếp tục" chỉ lưu các chương được tick (`TranslateSourceFactory:263–276`, `MainActivity:2254`).
3. **Gắn job vào chương** (lớp Java thuần, có test). Job lấy từ truy vấn của W0.
   1. Mở ảnh chụp của lượt `job.startSessionId`, lấy `chapters[job.preparedOrdinal].rawId`. Job được tiếp tục vẫn giữ `start_session_id` và `prepared_input_ordinal` của lượt đã tạo ra nó (`TranslatorService:390–399,461`).
   2. Nếu không có, dùng `RawUri.idOf(job.inputUri)`.
   3. Nếu vẫn không gắn được thì không tính vào chương nào.

   Job mới nhất của chương là job có `id` lớn nhất. Không dùng `completedHashes`/`unfinishedJobs`.

   **Test bắt buộc:**
   - RAW dịch lại ở tập khác không làm tập này "Xong";
   - job tiếp tục từ tập khác hoặc từ chế độ tệp lẻ;
   - job lỗi;
   - chưa có job;
   - tập đã xóa có hai lượt, lượt sau chỉ gồm vài chương.
4. **Nút chính** theo đề xuất 4.3, với các điểm sau:
   - nút ghi "**Dịch tiếp**", **không kèm số**: `continueChapterSet` tick theo hash RAW (`TranslateSourceFactory:276`), nên số đếm theo job có thể khác số được tick;
   - tập còn sống nhưng không còn RAW dùng được thì ẩn nút.
   - Hàng chương lỗi có [Retry], dùng lại `ACTION_RETRY_JOB_FAILED`. Trên điện thoại chỉ kiểm nút hiện đúng, **không bấm**.
5. **`NavState`** (trang, khóa tập, nơi xuất phát):
   - lưu và đọc qua Bundle, cạnh trạng thái đang lưu: tab, Cài đặt, chọn nhiều (`:570–578`);
   - thêm "Volume" vào `buildPageForTab` (trang lạ hiện rơi về Sample, `:900`), `primaryActive` (`:812`), `tabLabel` (`:809`);
   - tập không còn ở đâu nữa thì về Thư viện và báo.
6. **Back:** luật Volume → `OPEN_ORIGIN` trong `AppBackNavigationPolicy`, có test. Files và Settings giữ hành vi cũ.
7. Bật nút "Mở tập" của W0.

## 5. Kiểm thử và điểm dừng

**Test host:** toàn bộ test mới và cũ của `app` qua; lint không thêm lỗi mới.

**Điểm dừng 1 — sau W0 + W1:**
1. hồi quy archive sạch;
2. build `4.23-dev.14` (code > 291);
3. sao lưu điện thoại;
4. **hỏi owner duyệt cài**;
5. cài, rồi thử trên điện thoại:
   - mở và đóng ngăn ở cả 5 tab (☰, chạm vùng mờ, Back), kể cả tab Biên tập;
   - ngăn hiện tập đã xóa của owner (nhãn "đã xóa");
   - đang sửa một Glossary `QA_` rồi chọn mục trong ngăn → Glossary đã được lưu;
   - lọc Công việc theo tập;
   - giao diện sáng và tối, cỡ chữ 130% (khôi phục sau);
   - TalkBack đọc ngăn (khôi phục sau);
   - xoay màn hình khi ngăn mở → app không crash, ngăn đóng; ở chế độ ngang, ngăn phủ lên thanh dọc;
   - logcat không có FATAL.
6. Báo cáo kèm ảnh. **Không cần dừng chờ owner** trừ khi có lỗi; làm tiếp W2.

**Điểm dừng 2 — sau W2:**
1. hồi quy archive sạch;
2. build `4.23-dev.15`;
3. sao lưu điện thoại;
4. **hỏi owner duyệt cài**;
5. cài, rồi thử:
   - mở một tập còn sống và một tập đã xóa của owner;
   - trạng thái chương khớp với tab Công việc, kể cả tập cũ có job nằm ngoài 150 job mới nhất;
   - xoay màn hình và đổi giao diện khi đang ở màn tập → quay lại đúng tập;
   - Back về đúng nơi đã mở ngăn;
   - "Mở tập" từ Công việc.
6. **Dừng** và báo owner, kèm phiếu thử dưới đây.

**Báo cáo mỗi điểm dừng:**
- commit;
- bản, mã và SHA-256;
- đường dẫn sao lưu;
- kết quả từng mục: `PASS`, `FAIL` hoặc `NOT_MEASURED`;
- ảnh;
- lỗi còn lại.

Cập nhật `WORKSPACE_SNAPSHOT.md` theo đúng chính sách (một Next action).

**Phiếu thử cho owner sau dev.15:**

| # | Owner làm | Đạt khi |
|---|---|---|
| N0 | (Owner tự bấm, vì tốn tiền) "Dịch tiếp" trên màn tập | tick đúng các chương chưa dịch và bắt đầu chạy |
| N1 | Bấm ☰ ở bất kỳ tab nào, chọn một tập | mở đúng tập trong 2 chạm; Back về chỗ cũ |
| N2 | Mở một tập đã xóa | vẫn thấy danh sách chương và trạng thái; nút "Xem trong Công việc" |
| N3 | Chạy một tập (chi phí do owner quyết), mở ngăn khi đang chạy | tiến độ trong ngăn chạy theo, không giật |
| N4 | Một chương lỗi → mở tập → Retry ở hàng chương | retry đúng chương |
| N5 | Công việc → lọc theo tập | chỉ thấy lượt của tập đó |
| N6 | Cỡ chữ 130%, sáng/tối | thanh trên không bị cắt; ngăn đọc rõ |

## 6. Rủi ro

- **Gắn job vào chương** có thể sai với job tiếp tục từ tập khác hoặc từ chế độ tệp lẻ. Mục W2.3 quy định cách gắn và các test bắt buộc.
- **Đổi tên** có thể chạm chuỗi so sánh trong mã. Chỉ đổi chuỗi hiển thị, chạy toàn bộ test.
- **Thử trên điện thoại thật** có thể ảnh hưởng dữ liệu owner. Đã có sao lưu trước mỗi lần cài, và quy tắc `QA_` ở mục 3.
