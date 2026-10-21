# Translate Books — Đề xuất cách làm việc theo "Tập" với ngăn trái ☰

**Trạng thái:** bản 2.1, ĐÃ PHẢN BIỆN (2026-10-21; hai lượt phản biện đối chiếu với mã, đã sửa theo cả hai). Lịch sử các bản:
- Bản 1 (`51192dbb`): thêm tab "Tập".
- Bản 2 (`fb3938c5`): ngăn trái kiểu ChatGPT, mở bằng nút; giữ 5 tab; không thêm tab Tập; Cài đặt ở đáy ngăn.
- Bản 2.1 (bản này): giữ hướng của bản 2, sửa theo **dữ liệu dùng thật trên điện thoại của owner**.

**Nguồn:**
- Câu hỏi của owner (2026-10-21).
- Video owner quay app ChatGPT Android (22 giây). Chỉ dùng để tham khảo bố cục, không lưu vào Git vì có thông tin cá nhân.
- Bản sao lưu điện thoại 15e84958 ngày 2026-10-20 (`v4.23-dev11-code286`), nằm ngoài Git. **Chỉ đếm số**; không đọc nội dung sách, không đọc API key. Bản giải nén tạm đã bị xóa.
- `QA_REPORT_v2_6_8.md`, `CHANGELOG.md`.
- Skill `ui-ux-pro-max`.

Đi kèm `docs/V423_REFINED_UI_DESIGN.md`, nói về phong cách hiển thị. Việc cần làm cho W0–W2 nằm trong `docs/V423_WORKSPACE_NAV_WORK_REQUEST.md`.

## 1. Owner thật sự dùng app thế nào (số liệu điện thoại, 2026-10-20)

| Số liệu | Giá trị | Ý nghĩa với thiết kế |
|---|---|---|
| Tổ hợp dịch đã tạo, 18–20/10 | 7, **6 đã bị xóa**; RAW cũng bị xóa (14 RAW hiện có đều tạo ngày 20/10) | Tập là **đồ dùng một lần**: tạo → chạy → xóa. Nếu ngăn đọc tập còn sống thì gần như trống. |
| Lượt chạy tổ hợp (`translate_sessions/*.json`) | 9 lượt, 3–31 chương mỗi lượt; 6 tên tập; vài tập chạy 2 lượt | Lượt chạy là **lịch sử bền**: không có mã nào xóa nó. Mỗi lượt lưu tên tập, `setId` và ảnh chụp từng chương (tên RAW, số chương, tên Glossary/Pronoun). |
| Job từ 18/10 | 160, khoảng 147 nằm trong lượt tổ hợp | Từ khi có tổ hợp, owner gần như chỉ dịch theo tập. |
| Toàn bộ job | 307; trước 18/10 dịch tệp lẻ theo lô 1–3 tệp | "Dịch tệp lẻ" vẫn phải giữ nguyên. |
| Chunk phải thử lại | 87; 36 chunk còn chờ, 7 lỗi có thể retry | Retry và tab Công việc dùng **hằng ngày**. |
| RAW dịch lại | 9 RAW dịch 2 lần, 1 RAW dịch 3 lần | "Đã dịch" theo hash RAW có thể gán nhầm. |
| Biên tập | 1 tổ hợp (nguồn Tệp), 2 lượt | Biên tập **ít dùng**, và đang tạm dừng ("k động vào biên tập"). Điền sẵn Biên tập lùi lại. |
| Glossary / Pronoun | 13 / 13, mỗi chương một tệp | Đúng mô hình mỗi chương một bộ tệp. |

**Kết luận:**
- Bản 2 đúng hướng: owner làm theo tập và thích ngăn kiểu ChatGPT.
- Nhưng bản 2 dựng trên **dữ liệu sai**. Nó đọc tập còn sống (`ChapterSetStore`) và RAW còn sống (`LibraryRawStore`), mà owner xóa cả hai sau khi chạy.
- Bản 2.1 lấy **lượt chạy đã lưu** làm nguồn chính, giống cách ChatGPT lấy cuộc trò chuyện làm danh sách.

## 2. Hiện trạng — cái gì ổn, cái gì vướng (đã kiểm theo mã)

| Việc | Hiện tại | Nhận xét |
|---|---|---|
| Dịch tiếp chương chưa dịch | **~3 chạm**: màn Dịch nhớ tổ hợp đã chọn (`TranslateSourceFactory:70,76`) và tự tick chương chưa dịch (`:147–149`). Từ Jobs có "Tiếp tục tổ hợp" (`JobsMainPageFactory:153`), 2 chạm | ổn |
| Retry chương lỗi | **2–3 chạm** trên thẻ lượt ở Jobs. Lượt đã ghi tên tổ hợp, nhưng chưa **lọc** được theo tổ hợp | thiếu bộ lọc; việc hằng ngày |
| Đổi sang tập khác | Library → Tổ hợp → tìm, hoặc màn Dịch → chọn tổ hợp | không có lối nhanh |
| Nhìn cả tập một chỗ | Không có. Sau khi xóa tập thì chỉ còn thẻ lượt ở Jobs | thiếu |
| Sửa Glossary chương 5 | Library → Glossaries → tìm → Edit, ~4 chạm | vướng vừa |
| Biên tập chương 5 | 8–10 chạm | vướng, nhưng ít dùng |

**Tên gọi:**
- Library > Tổ hợp (`ChapterSetStore`) là bộ chương để dịch.
- Biên tập > Tổ hợp (`EditorialApiCombo`) là một chương để biên tập.

Hai thứ cùng tên mà không nối với nhau.

## 3. Học gì từ ChatGPT, và bài học v2.6.7

- **ChatGPT Android:**
  - ☰ mở ngăn chiếm ~80% bề ngang, màn cũ mờ phía sau; chạm ra ngoài hoặc Back thì đóng;
  - ngăn có danh sách việc (Đã ghim, Gần đây, "See all…");
  - đáy ngăn có nút chính và lối vào Cài đặt;
  - trang Projects có ô tìm và chip lọc;
  - Cài đặt có ô tìm.
- **ChatGPT chỉ có một đích chính (chat)**, nên ngăn chỉ chứa danh sách việc.
- **v2.6.7 giấu cả 5 đích chính vào ngăn**, nên owner thấy "chức năng như bị mất" và phải hoàn nguyên (`QA_REPORT_v2_6_8.md:76`; dòng cấm `MainActivity:639`).
- **Nguyên tắc:**
  - 5 tab giữ nguyên;
  - ngăn chỉ chứa tập, cộng một lối vào Cài đặt;
  - mở bằng nút ☰, **không vuốt mép**, vì mép trái là cử chỉ Back của Android 10+;
  - ngăn là lớp phủ tự làm, **không dùng `DrawerLayout`**.

## 4. Đề xuất bản 2.1

### 4.1 "Tập" là gì trong app

Một **tập** là một khóa `setId`, gom từ hai nguồn:
- **Tập còn sống:** hàng trong `chapter_sets`.
- **Tập đã xóa:** có trong lượt chạy (`ChapterSession`: `setId`, `setName`, `createdAt`, ảnh chụp chương) nhưng không còn trong `chapter_sets`.

Một tập đã xóa vẫn hiện trong ngăn và trang Tất cả tập, kèm nhãn "đã xóa".
- Màn tập dựng từ **ảnh chụp chương của lượt mới nhất** và các job của tập đó.
- Không cần RAW còn tồn tại.
- Nút chính đổi thành "Xem trong Công việc", vì không dịch tiếp được khi RAW đã bị xóa.

**Tên gọi:** đổi "Tổ hợp" thành "**Tập**" ở mọi chỗ thuộc phía dịch (Dịch, Công việc, Thư viện, ngăn) **trong cùng một bước (W1)**.
- Chỉ đổi nhãn, không đổi dữ liệu.
- Bên Biên tập giữ "Tổ hợp". Biên tập đang tạm dừng, không sửa mã Biên tập.

### 4.2 Ngăn trái ☰

```
☰ (thay logo ở góc trái thanh trên, mọi tab đều có)
┌───────────────────────────────┐░░░░
│ 🔍 Tìm tập                     │░░░░  ← màn đang mở, làm mờ
│ Đang chạy                      │░░░░
│  ATE_JP_VOL6  ▮▮▮▯▯ 4/14 · $0.12│░░░░
│ Gần đây                        │░░░░
│  KUMO_VOL10   ✓ 18/18          │░░░░
│  RAW_11       × 2 lỗi · 16/31  │░░░░
│  VOL5 (đã xóa) ✓ 31/31          │░░░░
│  Tất cả tập…                   │░░░░
│ ─────────────────────────────  │░░░░
│ [ + Tập mới ]        ⚙ Cài đặt  │░░░░
└───────────────────────────────┘░░░░
```

- **Mở và đóng:**
  - mở bằng nút ☰ 48 dp thay cho logo (`buildTopBar`, `:745–750`); nút có content description;
  - đóng bằng chạm vùng mờ, Back, hoặc chọn một mục;
  - không có cử chỉ vuốt.
- **Thanh dưới giữ nguyên 5 tab.** "⚙ Cài đặt" ở đáy ngăn chỉ là lối tắt.
- **Đang chạy:**
  - tập có job đang chạy hoặc đang chờ;
  - xác định bằng `TranslatorService.isActive()` cộng `EXTRA_ACTIVE_JOB_ID` → `setIdOfJob`.
- **Đọc job của một tập:**
  - không dùng `cachedJobSummaries`, vì danh sách này chỉ giữ 150 job mới nhất (`MainActivity:1163`) mà owner có 307;
  - dùng một truy vấn chỉ đọc, ngoài luồng UI: `start_session_id IN (các lượt của tập)` cộng `id IN (resumedJobs)`.
- **Gần đây:**
  - tối đa 6 tập, xếp theo mốc mới nhất trong ba mốc: lượt chạy cuối (`Run.lastUpdated`), `updatedAt` của tập còn sống, lần mở tập cuối (SharedPreferences);
  - mỗi hàng một dòng tóm tắt: số chương xong / tổng, số lỗi, nhãn "đã xóa" nếu có.
- **Tất cả tập…:** trang riêng có ô tìm theo tên. Chip lọc để sau.
- **Cập nhật khi đang chạy:**
  - ngăn chỉ dựng lại danh sách **khi mở**;
  - khi đang mở, chỉ cập nhật chữ tiến độ của hàng "Đang chạy", trong `receiver.onReceive` (`:287`) từ `EXTRA_PROGRESS`. Không dựng lại view, và không dựa vào `runtimeTimerTick` (nhịp này chỉ chạy ở tab Dịch, `:3574–3576`). Đây là bài học J1: p95 từng là 117 ms vì làm việc nặng trên luồng UI;
  - đóng ngăn thì dừng cập nhật.

### 4.3 Màn một tập

```
☰  ATE_JP_VOL6                        ⋮
   14 chương · 10 xong · 1 lỗi · 3 chưa dịch
   [ Dịch tiếp ]                        ← nút chính theo trạng thái
──────────────────────────────────────────
Ch.1  001_ATE_JP_VOL6   ✓ Xong · 4 chunk       ›
Ch.4  004_ATE_JP_VOL6   × Lỗi: bị cắt  [Retry] ›
Ch.11 011_ATE_JP_VOL6   — Chưa dịch            ›
```

- **Vị trí:** trang "Volume", con của tab Thư viện (tab Thư viện sáng). Back về nơi đã mở ngăn.
- **Danh sách chương:**
  - tập còn sống: các hàng `chapter_set_rows`;
  - tập đã xóa: hợp các chương của mọi lượt có cùng `setId`, khóa `rawId` (dự phòng `chapterNo`). Không chỉ lấy lượt mới nhất, vì lượt "tiếp tục" chỉ lưu các chương được tick (`TranslateSourceFactory:263–276`).
- **Trạng thái chương lấy từ job**, không từ hash RAW. Gắn job vào chương:
  1. mở ảnh chụp của lượt `job.startSessionId`, lấy `chapters[job.preparedOrdinal].rawId`. Job được tiếp tục giữ lượt và vị trí của lượt đã tạo ra nó (`TranslatorService:390–399,461`);
  2. nếu không có thì dùng `RawUri.idOf(job.inputUri)`;
  3. vẫn không gắn được thì bỏ qua.
  - Job mới nhất là job có `id` lớn nhất. Không có job thì "Chưa dịch".
  - Như vậy một RAW dịch lại ở tập khác **không** làm tập này báo "Xong".
  - Không cần `completedHashes`/`unfinishedJobs`.
- **Nút chính:**
  - tập còn sống và còn chương chưa dịch → "Dịch tiếp" (`continueChapterSet`), **không kèm số**, vì `continueChapterSet` tick theo hash RAW nên số có thể khác. Không còn RAW dùng được thì ẩn nút;
  - đang chạy → "Xem tiến trình";
  - có chương lỗi → "Xem N chương lỗi" (Công việc, lọc theo tập);
  - tập đã xóa → "Xem trong Công việc".
- **Hàng chương lỗi** có [Retry], dùng lại hành động retry một job đang có (`ACTION_RETRY_JOB_FAILED`, `:2430`). Retry cả tập một lần **không** làm trong v4.23.
- **⋮ gồm:** Đổi tên, Sửa tập (luồng hiện có), Xem trong Công việc. Chỉ có với tập còn sống.
- **Không có cột Biên tập** trong v4.23 (Biên tập tạm dừng). Vì vậy không cần truy vấn bảng Biên tập.

### 4.4 Công việc

- Bộ lọc theo tập (W0), dùng `setId`/`setName` có sẵn trên `RunGroups.Run`.
- Nút "Mở tập" trên thẻ lượt.
- Đây là chỗ owner dùng hằng ngày (retry), nên làm trước.

### 4.5 Để sau (W3–W4, mỗi mục cần duyệt riêng)

- **W3:**
  - trang chương;
  - "Sửa" mở trình sửa Glossary/Pronoun có sẵn rồi quay lại;
  - "Biên tập chương này" chỉ điền sẵn form (`newCombo()` rồi gán trường, owner bấm Lưu). Chỉ làm khi Biên tập mở lại.
- **W4:**
  - Ghim;
  - chip lọc;
  - ô tìm Cài đặt: nhóm Cài đặt chỉ dựng khi mở rộng (`SettingsPageFactory:52–58`), nên cần chỉ mục viết tay;
  - Retry cả tập;
  - bỏ tab Cài đặt khỏi thanh dưới;
  - ngăn luôn mở trên tablet;
  - "Lưu trữ tập" thay cho xóa.
- **Câu hỏi mở cho owner:** vì sao xóa tập sau khi chạy (cho gọn Thư viện, hay vì lý do khác)? Nếu để cho gọn, "Lưu trữ" (ẩn khỏi Thư viện, vẫn có trong ngăn) có thể thay cho xóa.

## 5. Rủi ro khi chuyển sang bản 2.1 và cách sửa từ gốc

| # | Rủi ro | Gốc | Sửa từ gốc | Bước |
|---|---|---|---|---|
| R1 | Ngăn trống vì tập bị xóa | Đọc `chapter_sets` | Khóa tập = `setId` gộp từ tập còn sống và lượt chạy (4.1) | W1 |
| R2 | Màn tập mất trạng thái khi RAW bị xóa | Đọc trạng thái theo hash RAW | Trạng thái từ job của tập, danh sách từ ảnh chụp lượt (4.3) | W2 |
| R3 | Đổi trang làm mất Glossary chưa lưu hoặc nháp tập | `switchTab` (`:814`) không có chốt lưu; Glossary chỉ lưu khi Back (`:880`) | **Một cửa điều hướng** `navigateTo(target)`. Ngăn, màn tập và nút "Mở tập" đều đi qua đây. Hàm này chạy `silentPersistCurrentUi`, lưu Glossary đang sửa, hỏi khi có nháp tập, rồi mới `switchTab` | W1 |
| R4 | Xoay màn hình hoặc đổi giao diện làm mất trang Volume (rơi về trang Sample) | `onSaveInstanceState` lưu tab, Cài đặt và trạng thái chọn nhiều (`:570–578`), nhưng không có trang Volume; tên trang lạ thì về Sample (`:900`) | Một đối tượng `NavState` (trang, khóa tập, nơi xuất phát) lưu vào Bundle và đọc lại trong `buildUi`; thêm "Volume" vào `buildPageForTab`, `primaryActive` (`:812`), `tabLabel` (`:809`). Tập không còn (kể cả trong lượt chạy) thì về Thư viện và báo | W2 |
| R4b | Màn tập cũ báo sai "Chưa dịch" | Danh sách job trong bộ nhớ giới hạn 150 (`:1163`), trang Công việc giới hạn 30 thẻ (`JobsMainPageFactory:58`) | Truy vấn chỉ đọc theo lượt của tập (4.2); bộ lọc áp trước giới hạn 30 | W0/W2 |
| R4c | Thử trên điện thoại vô tình gọi provider | "Dịch tiếp", Retry, Resume tự chạy (`:422–438`, `:2423–2433`) | Trên điện thoại không bấm các nút này; owner tự thử (phiếu N0) | mọi bước |
| R5 | Back không đóng ngăn khi đang ở Biên tập | `editorialApiShared.handleBack()` chạy trước policy (`:874`) | Ngăn mở → đóng ngăn là **dòng đầu của `onBackPressed`**. Volume → `OPEN_ORIGIN` trong `AppBackNavigationPolicy` | W1/W2 |
| R6 | Giật khi ngăn mở lúc đang dịch, hoặc tiến độ đứng yên ngoài tab Dịch | Dựng lại view theo nhịp; nhịp 3 giây chỉ chạy ở tab Dịch | Chỉ đổi chữ tiến độ trên view có sẵn, trong `receiver.onReceive`; dựng danh sách ngoài luồng UI khi mở | W1 |
| R7 | Thanh trên chật ở cỡ chữ 130% (J5) | ☰ + tiêu đề + chip trạng thái + ↻ trên màn 360 dp | Tiêu đề một dòng, cắt "…"; chip trạng thái thu thành chấm màu dưới bề ngang ngưỡng; ☰ và ↻ cố định 48 dp. Kiểm trên điện thoại ở 130% | W1 |
| R8 | Ba tên cho một thứ (Tổ hợp / Tập / bộ chương) | Đổi tên dần | Đổi một lần mọi chỗ phía dịch (4.1), kể cả chuỗi viết dạng `ổ`; giữ `ChapterSetMatcher.defaultName` (là dữ liệu); sửa assert trong test cùng commit | W1 |
| R9 | Panel che inset hoặc lộ padding 10 dp | `appRoot` có padding (`:634`) | `FrameLayout` gốc mới; lớp mờ và panel ngang hàng `appRoot`; panel tự xử lý inset | W1 |
| R10 | TalkBack đọc cả nền khi ngăn mở | Lớp phủ tự làm | `NO_HIDE_DESCENDANTS` cho `appRoot`, focus vào panel, trả lại khi đóng | W1 |
| R11 | Dialog hoặc bàn phím chồng lên ngăn | Cửa sổ riêng | Đóng ngăn trước khi mở dialog; ẩn bàn phím khi mở ngăn | W1 |
| R12 | Dòng cấm `DrawerLayout` (`:639`) bị hiểu sai | Chú thích cũ | Sửa chú thích: ngăn tự làm, chỉ chứa tập, mở bằng nút; nêu bài học v2.6.7 | W1 |
| R13 | Thử trên điện thoại làm hỏng dữ liệu thật | Thử trực tiếp trên máy owner | Sao lưu APK và dữ liệu riêng trước **mỗi** lần cài. Chỉ tạo dữ liệu thử có tiền tố `QA_` và chỉ xóa dữ liệu đó. Không chạy dịch thật nếu chưa có quyền chi tiền | mọi bước |

## 6. Lộ trình

| Bước | Nội dung | Rủi ro |
|---|---|---|
| W0 | Công việc: lọc theo tập, nút "Mở tập" (qua `navigateTo`) | thấp |
| W1 | `navigateTo`; ngăn ☰ (Đang chạy, Gần đây, Tất cả tập có ô tìm, + Tập mới, ⚙ Cài đặt); Back đóng ngăn; đổi tên "Tập"; thanh trên ở 130%; sửa chú thích `:639` | vừa |
| W2 | Trang Volume (tập còn sống và đã xóa, trạng thái từ job, nút chính, Retry từng chương); `NavState` qua `recreate()`; Back về nơi xuất phát | vừa |
| W3 | (Khi Biên tập mở lại) trang chương; sửa Glossary/Pronoun bằng trình sửa có sẵn; điền sẵn form Biên tập | vừa |
| W4 | (Tùy chọn) Ghim, chip lọc, ô tìm Cài đặt, Retry cả tập, Lưu trữ tập, bỏ tab Cài đặt, ngăn luôn mở trên tablet | vừa |

## 7. Kiểm thử

- **Unit test (Java thuần):**
  - mô hình tập: gộp tập còn sống và lượt chạy theo `setId`; tập đã xóa vẫn có; một tập không hiện hai lần; Gần đây tối đa 6 theo mốc mới nhất;
  - trạng thái chương từ job: RAW dịch lại ở tập khác không làm tập này "Xong"; job lỗi; chưa có job;
  - `AppBackNavigationPolicy`: Volume → `OPEN_ORIGIN`; Files và Settings giữ hành vi cũ;
  - `NavState` lưu và đọc lại.
- **Trên điện thoại 15e84958** (owner duyệt từng lần cài; sao lưu trước):
  - mở ngăn với dữ liệu thật: có tập đã xóa, có tập đang sống;
  - mở tập, xoay màn hình, đổi giao diện → đúng tập, ngăn đóng;
  - Back khi ngăn mở ở cả 5 tab, kể cả Biên tập;
  - đang sửa Glossary rồi chọn tập trong ngăn → Glossary đã được lưu;
  - cỡ chữ 130%, giao diện sáng và tối;
  - TalkBack đọc ngăn;
  - không bấm Dịch tiếp, Retry, Resume (tốn tiền); owner tự thử;
  - nếu có quyền chi tiền thì thử thêm: mở ngăn trong khi đang dịch, đo trace section, không có khung > 32 ms do ngăn.
- **Trạng thái trống:** không có tập và không có lượt → ngăn hiện "Tạo tập từ RAW" và "Dịch tệp lẻ".
