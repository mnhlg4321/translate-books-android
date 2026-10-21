# Translate Books — Đề xuất cách làm việc theo "Tập" với ngăn trái ☰

**Trạng thái:** bản 2, ĐÃ PHẢN BIỆN (2026-10-21; agent phản biện đối chiếu với mã `feature/v4.23`, các sửa đã đưa vào bên dưới). Bản 1 (`51192dbb`) đề xuất thêm tab "Tập"; owner chọn hướng ngăn trái kiểu ChatGPT:
- mở bằng nút;
- giữ 5 tab hiện có;
- không thêm tab Tập;
- Cài đặt có lối vào ở đáy ngăn.

**Nguồn:**
- Câu hỏi của owner (2026-10-21) và video owner quay app ChatGPT Android (22 giây; chỉ dùng để tham khảo bố cục, không lưu vào Git vì có thông tin cá nhân).
- `QA_REPORT_v2_6_8.md`, `CHANGELOG.md`.
- Skill `ui-ux-pro-max`.

Đi kèm `docs/V423_REFINED_UI_DESIGN.md`, nói về phong cách hiển thị.

## 1. Owner làm việc thế nào

Owner làm theo **tập sách** (ATE_JP_VOL6, KUMO_VOL10…), mỗi tập 10–30 chương; mỗi chương có RAW, Glossary và Pronoun riêng. Vòng việc lặp lại:

1. Nhập tệp và ghép chương.
2. Dịch hàng loạt.
3. Xử lý chương lỗi.
4. Biên tập từng chương (DRAFT → FINAL).
5. Xuất.

## 2. Hiện trạng — cái gì ổn, cái gì vướng (đã kiểm theo mã)

| Việc | Hiện tại | Nhận xét |
|---|---|---|
| Dịch tiếp chương chưa dịch | **~3 chạm**: màn Dịch nhớ tổ hợp đã chọn (`TranslateSourceFactory:70,76`) và tự tick chương chưa dịch (`:147–149`). Từ Jobs có "Tiếp tục tổ hợp" (`JobsMainPageFactory:153`), 2 chạm | ổn |
| Retry chương lỗi | **2–3 chạm** trên thẻ lượt ở Jobs. Lượt đã ghi tên tổ hợp, nhưng chưa **lọc** được theo tổ hợp | thiếu bộ lọc |
| Sửa Glossary chương 5 | Library → Glossaries → tìm → Edit, ~4 chạm và phải rời màn đang làm | vướng vừa |
| **Biên tập chương 5** | Biên tập → + Tổ hợp mới → chọn job (danh sách **mọi** job, không lọc) → chọn Glossary → chọn Pronoun → Lưu → Chạy: **8–10 chạm**, chọn lại đúng những tệp đã ghép ở Library | **vướng lớn nhất** |
| Nhìn cả tập một chỗ | Không có. Trạng thái một chương rải ở Jobs, Biên tập, Library | thiếu |
| Đổi sang tập khác | Library → Tổ hợp → tìm, hoặc màn Dịch → chọn tổ hợp | không có lối nhanh |

**Gốc vấn đề:** hai khái niệm cùng tên "Tổ hợp" nhưng không nối với nhau.
- Library > Tổ hợp (`ChapterSetStore`) là bộ chương để dịch.
- Biên tập > Tổ hợp (`EditorialApiCombo`) là một chương để biên tập.

## 3. Học gì từ ChatGPT, và bài học v2.6.7

**Video ChatGPT Android cho thấy:**
- Màn chính là việc đang làm (khung chat).
- Nút ☰ ở góc trái mở **ngăn chiếm ~80% bề ngang**; màn cũ vẫn thấy mờ ở phần còn lại. Bấm ra ngoài hoặc Back thì đóng.
- Nội dung ngăn:
  - vài lối tắt cố định (Images, Library, Projects…);
  - **Đã ghim** (các project);
  - **Gần đây** (các cuộc trò chuyện) kèm "See all…".
- Đáy ngăn có nút hành động chính cố định ("Chat") và ảnh đại diện; ảnh đại diện mở **Cài đặt**.
- Danh sách Projects đầy đủ là trang riêng, có ô tìm kiếm, chip lọc, biểu tượng ghim và ngày.
- Cài đặt có **ô tìm kiếm** và các hàng nhóm, mỗi hàng ghi giá trị hiện tại bên phải (Accent color · Blue).

**Vì sao ChatGPT dùng ngăn được còn v2.6.7 thì hỏng:**
- ChatGPT chỉ có một đích chính (chat), nên ngăn chỉ chứa **danh sách việc**.
- v2.6.7 thì giấu cả 5 đích chính vào ngăn, nên owner thấy "chức năng như bị mất" và bản này phải hoàn nguyên (`QA_REPORT_v2_6_8.md:76`; dòng cấm `MainActivity:639`).

**Nguyên tắc rút ra:**
- 5 tab giữ nguyên ở thanh dưới.
- Ngăn chỉ chứa **các tập**, cộng một lối vào Cài đặt.
- Ngăn **mở bằng nút ☰, không vuốt mép**, vì trên Android 10+ mép trái là cử chỉ Back của hệ thống.
- Ngăn là lớp phủ tự làm (FrameLayout và một panel), **không dùng `DrawerLayout`** (giữ đúng dòng cấm ở `MainActivity:639`).

## 4. Đề xuất

### 4.1 Ngăn trái ☰ — danh sách tập

```
☰ (thay logo ở góc trái thanh trên, mọi tab đều có)
┌───────────────────────────────┐░░░░
│ 🔍 Tìm tập                     │░░░░  ← màn đang mở, làm mờ
│ ĐANG CHẠY                      │░░░░
│  ATE_JP_VOL6  ▮▮▮▯▯ 4/14 · $0.12│░░░░
│ ĐÃ GHIM (W4)                   │░░░░
│  📌 KUMO_VOL10  ✓ 18/18 · BT 6  │░░░░
│ GẦN ĐÂY                        │░░░░
│  RAW_11       × 2 lỗi · 16/31  │░░░░
│  JAKUAKU_VOL1 — chưa dịch       │░░░░
│  Tất cả tập…                   │░░░░
│ ─────────────────────────────  │░░░░
│ [ + Tập mới ]        ⚙ Cài đặt  │░░░░
└───────────────────────────────┘░░░░
```

- **Mở và đóng:**
  - mở bằng nút ☰ ở góc trái thanh trên; nút này thay cho logo hiện tại (`buildTopBar`);
  - đóng bằng chạm vùng mờ, Back, hoặc chọn một mục.
  - Không có cử chỉ vuốt.
- **Thanh dưới giữ nguyên 5 tab:** Dịch · Công việc · Biên tập · Thư viện · Cài đặt. Không thêm, không bỏ tab nào. Hàng "⚙ Cài đặt" ở đáy ngăn chỉ là lối tắt tới cùng trang Cài đặt.
- **Các nhóm:**
  - **Đang chạy:** tập có lượt dịch đang chạy, kèm thanh tiến độ và chi phí. Tính được từ dữ liệu có sẵn: `setIdOfJob` dùng `RunGroups.sessionOf` trên `cachedJobSummaries`/`cachedSessions` (`MainActivity:439–444`); `RunGroups.Run` đã có tiến độ và mức dùng.
  - **Gần đây:** tối đa 5 tập. Xếp theo mốc mới nhất trong ba mốc:
    - `updatedAt` của tập (`ChapterSetStore:30–31`; đây là lúc sửa tập, không phải lúc hoạt động);
    - thời điểm lượt dịch cuối;
    - lần mở tập cuối, lưu thêm ở SharedPreferences (**không đổi schema**).
  - **Đã ghim** (W4, tùy chọn): ghim hoặc bỏ ghim từ ⋮ trên màn tập, lưu ở SharedPreferences. Làm sau, vì "Đang chạy" và "Gần đây" đã đủ cho nhu cầu chính.
  - **Tất cả tập…:** mở trang danh sách đầy đủ.
- **Đáy ngăn:**
  - **"+ Tập mới"** mở luồng tạo Tổ hợp dịch hiện có;
  - **"⚙ Cài đặt"**.
- "Tập" là tên hiển thị của Tổ hợp dịch (`ChapterSetStore`), kèm phụ đề "(bộ chương)" trong giai đoạn chuyển. **Không đổi dữ liệu.** Chữ "Tổ hợp" bên Biên tập giữ nguyên.
- **Hiệu năng:**
  - nội dung ngăn tính ngoài luồng UI, có khung chờ;
  - mở ngăn thì hiện ngay bản đã lưu tạm, rồi làm mới sau.

### 4.2 Trang "Tất cả tập"

- Kiểu trang Projects của ChatGPT:
  - ô tìm;
  - mỗi hàng có tên, tóm tắt trạng thái và ngày hoạt động.
- Chip lọc **Tất cả · Đang dịch · Có lỗi · Xong** và việc thay hẳn Library > Tổ hợp để W4. Trong W1, Library > Tổ hợp giữ nguyên.

### 4.3 Màn một tập

```
☰  ATE_JP_VOL6                        ⋮
   14 chương · 10 đã dịch · 0 lỗi · BT 2
   [ Dịch 4 chương còn lại ]            ← một nút chính theo ngữ cảnh
──────────────────────────────────────────
Ch.1  001_ATE_JP_VOL6   ✓ Dịch  ✓ BT    ›
Ch.2  002_ATE_JP_VOL6   ✓ Dịch  — BT    ›
Ch.4  004_ATE_JP_VOL6   × Lỗi   Retry   ›
Ch.11 011_ATE_JP_VOL6   — Chưa dịch     ›
```

- **Vị trí:**
  - màn tập là trang "Volume", con của tab **Thư viện**: tab Thư viện sáng, như các trang Glossaries/RAW hiện nay;
  - trang chương là trạng thái bên trong trang Volume (giống `editingGlossary`), không phải một trang riêng;
  - Back về trang/tab đang mở trước khi chọn tập trong ngăn (xem mục 5).
- **Nút chính đổi theo trạng thái tập:**
  - còn chương chưa dịch → "Dịch N chương còn lại" (dùng lại `continueChapterSet`, chuyển sang tab Dịch như hiện nay);
  - đang chạy → "Xem tiến trình" (tab Công việc);
  - có chương lỗi → "Xem N chương lỗi" (mở Công việc, lọc theo tập).
  - Retry cả tập một lần **không có** trong v4.23. `retryJobFailedChunks` chỉ retry một job, còn `continueChapterSet` từ chối khi có job đang chạy và tự chạy lượt kế (`:422–431`). Retry cả tập cần một bộ điều phối mới.
  - Retry từng chương vẫn có ở hàng chương.
- **⋮ gồm:** Đổi tên, Sửa tập (luồng Tổ hợp hiện có), Xem lượt trong Công việc. Ghim thêm ở W4.
- **Trạng thái từng chương** tính từ dữ liệu sẵn có, không đổi schema:
  - RAW có hay không: `LibraryRawStore.find`;
  - đã dịch: `completedHashes`;
  - dở hoặc lỗi: **cần một truy vấn gộp mới**. `unfinishedJobs(c, id)` chạy một truy vấn cho mỗi RAW theo `input_uri` (`LibraryRawStore:180`), nên không gộp được như hiện tại;
  - biên tập: truy vấn nhẹ trên `editorial_pair_runs(combo_id, state)` (xem mục 5).
- **Không có "Đã xuất"**, vì app không ghi lại việc xuất. Chỉ hiện "Có FINAL".
- Trạng thái tính ngoài luồng UI, gộp truy vấn theo lô.
  - Không lặp lại cách của `TranslateSourceFactory.refresh` (`:131–145`): mở cơ sở dữ liệu N lần, rồi quét tuyến tính `GlossaryStore.find`/`PronounStore.find`, tức O(N·M).

### 4.4 Trang chương

- **Đầu trang:** RAW / Glossary / Pronoun dạng hàng.
  - "Sửa" mở **trình sửa Glossary/Pronoun có sẵn** ngay đúng tệp (`editingGlossary`), rồi Back trả về trang chương. Thay cho 4 chạm vòng qua Library.
  - Không làm trình sửa thứ hai.
- **Dịch:** "Đã xong · 3 chunk · $0.012" kèm [Xem] [Dịch lại]; hoặc "Lỗi: bị cắt" kèm [Retry].
- **Biên tập:** "Chưa chạy" kèm **[Biên tập chương này]**. Cách làm:
  - Nếu đã có tổ hợp Biên tập cùng `jobId` (tìm qua `listCombos()`), gọi `openCombo(id)` rồi `switchTab("Editorial")`. `openCombo` chỉ mở **form** tổ hợp và chỉ vẽ lại khi Biên tập đang là tab hiện tại (`EditorialApiUiController:146`).
  - Nếu chưa có:
    1. gọi `editorialApi().newCombo()`;
    2. điền sẵn `combo.sourceKind = SOURCE_JOB`, `jobId`, `glossaryId`, `pronounId` (test hiện có đã gán trường `combo` từ ngoài);
    3. mở tab Biên tập.

    Owner bấm **Lưu**, nên mã mới **không ghi dữ liệu Biên tập** và **không sửa tệp `Editorial*`**.
  - **Job được chọn:** job hoàn tất mới nhất của RAW chương đó. Nếu chương đã dịch nhiều lần, trang chương ghi rõ job nào và cho đổi.
  - Thiếu Glossary hoặc Pronoun thì vẫn mở form, báo rõ ô còn thiếu, không tự chạy.
  - **Số chạm:** từ 8–10 xuống **3** (chương → Biên tập chương này → Lưu/Chạy).
- **Kết quả:** "Có FINAL" kèm [Xem] [Xuất].

### 4.5 Công việc, Cài đặt, màn rộng

- **Công việc:** thêm bộ lọc theo tập, và nút "Mở tập" trên mỗi lượt.
- **Cài đặt:** học từ video, thêm **ô tìm cài đặt** (W4, bước riêng).
  - Rủi ro không thấp: nội dung mỗi nhóm chỉ được dựng khi mở rộng (`SettingsPageFactory:52–58`), và màn rộng dùng cấu trúc khác (`:18–26`).
  - Vì vậy cần một chỉ mục nhãn → nhóm viết tay, cộng logic cuộn tới mục cho cả hai bố cục.
  - Phong cách hàng nhóm theo `V423_REFINED_UI_DESIGN.md`.
- **Màn rộng (≥ 720 dp):**
  - ngăn vẫn mở bằng ☰ dưới dạng lớp phủ, rộng tối đa 360 dp;
  - thanh dọc 112 dp giữ nguyên;
  - chế độ ngăn luôn mở ở màn rộng để sau, chỉ làm khi owner thấy cần.

## 5. Ràng buộc và rủi ro

- **Ngăn tự làm, không `DrawerLayout`:**
  - **Bố cục:**
    - `buildUi` hiện đặt `appRoot` trực tiếp làm content view (`MainActivity:629–653`);
    - bọc nó trong một `FrameLayout` gốc mới, thêm lớp mờ và panel ngăn **ngang hàng** với `appRoot`, ngoài padding 10 dp của nó (`:634`);
    - inset vẫn tới `appRoot` vì listener không tiêu thụ inset; panel cần tự xử lý inset trên/dưới.
  - **Chuyển trang từ ngăn** phải qua cùng các chốt như khi đổi tab:
    - `silentPersistCurrentUi`;
    - nháp ChapterSets đang mở;
    - Glossary chưa lưu (hiện chỉ lưu khi Back, `:880`).
  - **Trợ năng:**
    - khi ngăn mở, đặt `NO_HIDE_DESCENDANTS` cho `appRoot` và chuyển focus vào panel;
    - nút ☰ cần content description, vì mô tả "Translate Books logo" (`:747`) mất theo logo.
  - **Cửa sổ khác:** `AlertDialog`, toast và bàn phím là cửa sổ riêng, không xung đột. Đóng ngăn trước khi mở dialog.
  - **Dòng cấm ở `:639`:** sửa lời chú thích, ghi rõ ngăn tự làm (chỉ chứa danh sách tập, mở bằng nút) được phép, và vì sao.
- **Giữ trạng thái qua `recreate()`:**
  - Xoay màn hình (manifest không khai báo `configChanges`) hoặc đổi giao diện (`commitAppearance` → `recreate()`, `:674`) đều dựng lại Activity.
  - `onSaveInstanceState` hiện chỉ lưu `currentTab` và trạng thái Cài đặt (`:570–578`). Tên trang lạ thì `buildPageForTab` rơi về trang Sample (`:~899`).
  - Cần:
    - lưu `volumeId`, `chapterId` (nếu đang mở chương) và `volumeOrigin` vào Bundle;
    - thêm "Volume" vào `buildPageForTab`, `primaryActive` (`:812`) và `tabLabel` (`:808`);
    - khi dựng lại thì ngăn luôn đóng.
- **Back** (`onBackPressed` và `AppBackNavigationPolicy`):
  - ngăn đang mở → đóng ngăn. Đây là **dòng đầu tiên của `onBackPressed`**, đứng trước `editorialApiShared.handleBack()` (`:874`) và các kiểm tra chọn nhiều / nháp ChapterSets (`:875–876`);
  - chương → màn tập (trạng thái trong trang Volume);
  - màn tập → nơi xuất phát: thêm trường `volumeOrigin` và một luật `"Volume" → OPEN_ORIGIN` trong `AppBackNavigationPolicy.resolve`;
  - Files bấm Back về Dịch; Settings bấm Back về nhóm General trước, rồi về Dịch (`AppBackNavigationPolicy:24–26`). Giữ nguyên;
  - manifest chưa bật predictive back, cần lo khi lên targetSdk 36.
- **Nút ☰ thay logo:** logo vẫn hiện ở đầu ngăn, nên nhận diện thương hiệu không mất. Thanh trên vẫn giữ chip trạng thái và nút ↻.
- **Biên tập:** mã mới không ghi dữ liệu Biên tập (owner tự bấm Lưu, mục 4.4) và không sửa tệp `Editorial*`.
- **"Đã dịch" tính theo hash RAW**, không theo tập. Một RAW nằm trong hai tập, hoặc dịch lại với Glossary khác, có thể bị báo "đã dịch" sai. Màn chương ghi rõ job và Glossary đã dùng.
- **Đọc trạng thái Biên tập:**
  - `SqliteEditorialPairRunStore.latestRun` chạy `SELECT *` (`:71`), tức đọc cả `raw_rows_json`, `draft_rows_json` và `merged_text`. Vì vậy chỉ gọi nó cho chương đang mở.
  - Ngăn và màn tập dùng truy vấn nhẹ trong **một lớp mới ngoài mã Biên tập**, chỉ đọc `editorial_pair_runs(combo_id, state)`.
  - Lớp này phụ thuộc schema bảng Biên tập, nên cần test riêng để báo lỗi khi schema đổi.
- **"Gần đây" cần mốc thời gian:**
  - lượt dịch đã có thời điểm;
  - lần mở tập cần lưu thêm vào SharedPreferences (không đổi schema).

## 6. Lộ trình

| Bước | Nội dung | Rủi ro |
|---|---|---|
| W0 | Công việc: lọc theo tập, nút "Mở tập" | thấp |
| W1 | Ngăn ☰ (Đang chạy, Gần đây, Tất cả tập có ô tìm, + Tập mới, ⚙ Cài đặt); Back đóng ngăn; sửa chú thích `:639` | vừa |
| W2 | Trang Volume (trạng thái chương, nút chính theo ngữ cảnh, truy vấn gộp mới); Back về nơi xuất phát; lưu trạng thái qua `recreate()` | vừa |
| W3 | Trang chương: mở trình sửa Glossary/Pronoun có sẵn rồi quay lại; "Biên tập chương này" điền sẵn form, owner bấm Lưu | vừa |
| W4 | (Tùy chọn, mỗi mục cần duyệt riêng sau một kỳ dùng) Ghim; chip lọc và thay Library > Tổ hợp; ô tìm Cài đặt; Retry cả tập; bỏ tab Cài đặt khỏi thanh dưới; ngăn luôn mở ở màn rộng | vừa |

## 7. Kiểm thử

- **Unit test `AppBackNavigationPolicy`:**
  - chương → tập → nơi xuất phát (`OPEN_ORIGIN`);
  - Files và Settings giữ hành vi cũ.
- **Instrumented test (emulator):**
  - ngăn mở → Back đóng ngăn, kể cả khi đang ở tab Biên tập;
  - xoay màn hình và đổi giao diện khi đang ở trang Volume/chương → quay lại đúng tập và chương, ngăn đóng.
- **Test truy vấn trạng thái Biên tập** trên schema `editorial_pair_runs` hiện tại.
- **Mô hình trạng thái chương bằng Java thuần**, có các ca:
  - một RAW thuộc hai tập;
  - thiếu Glossary/Pronoun;
  - job dở;
  - có tổ hợp Biên tập.
- **Mô hình ngăn bằng Java thuần:**
  - xếp nhóm Đang chạy / Gần đây;
  - một tập không hiện hai lần;
  - Gần đây tối đa 5, xếp theo mốc mới nhất trong ba mốc;
  - tập đã bị xóa thì bỏ qua.
- **Điền sẵn Biên tập:**
  - có tổ hợp cùng `jobId` thì mở tổ hợp đó;
  - chưa có thì form mới được điền đủ trường;
  - không có hàng nào được ghi trước khi owner bấm Lưu.
- **Thiết bị (emulator):**
  - mở ngăn với 30 tập và mở tập 30 chương, không có việc dài trên luồng UI (đo bằng trace section);
  - xoay màn hình khi ngăn mở;
  - TalkBack đọc ngăn;
  - cỡ chữ 130%.
- **Trạng thái trống:** chưa có tập nào → ngăn hiện "Tạo tập từ RAW" và "Dịch tệp lẻ".
