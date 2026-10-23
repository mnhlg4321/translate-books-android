# v4.23 — Sửa nhỏ B1–B3 (gói việc cho Codex)

**Quyết định sản phẩm được phục vụ:** chốt giao diện v4.23 đủ gọn để phát hành. Ba lỗi nhỏ còn lại sau khi điều phối kiểm dev.25 (`docs/V423_UX_OVERHAUL_WORK_REQUEST.md` mục 8c).

**Nhánh và nơi làm:** `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`. Không đổi nhánh, không merge, không tag, không push.

**Không được đụng:**
- `editorial-engine/` và mọi tệp `Editorial*`;
- prompt và instruction (v18 giữ nguyên);
- logic dịch, chi phí, provider, DB schema.

Không gọi provider. Không thêm gate, manifest hay khung đo mới.

## 1. B1 — Thẻ "Glossary" xuống dòng ở cỡ chữ 130%

**Hiện tại:** `MainActivity.buildLibraryDestination` (dòng 1030–1040).
- Dải thẻ có 4 nút RAW / Glossary / Pronoun / Tập, mỗi nút `weight 1`, cộng nút ⋮ rộng 48 dp.
- `button()` đặt cỡ chữ `13 * fontScale`; sau đó dòng này lại gọi `setTextSize(12)`, nên bỏ qua cỡ chữ của app. `WrapButton` cho phép xuống dòng.
- Ở 360 dp và cỡ chữ 130%, "Glossary" bị bẻ thành "Glossar / y".

**Sửa:**
- Nhãn thẻ luôn một dòng: gọi `setSingleLine(true)` **sau** `button()` (vì `WrapButton` đặt `setSingleLine(false)`), rồi `setEllipsize(null)` để không có dấu "…".
- Cỡ chữ theo cùng thang của app như các nút khác, không gán cứng `12`.
- Nếu ở thang lớn nhất app cho phép, kể cả khi cỡ chữ hệ thống là 130%, bốn nhãn không vừa:
  - giảm đệm ngang bằng `setPadding` tường minh, ví dụ `dp(4)`. `tint()` dùng `GradientDrawable` không có đệm, nên đệm hiện tại đến từ kiểu Button mặc định;
  - nếu vẫn không vừa, đặt dải thẻ trong `HorizontalScrollView` có cạnh mờ (`setHorizontalFadingEdgeEnabled`) để thấy là cuộn được;
  - trong vùng cuộn, thẻ dùng bề rộng `WRAP_CONTENT` và tối thiểu `dp(48)`, không dùng weight, vì con có weight bị co lại trong `HorizontalScrollView`. Vùng cuộn lấy weight 1, cạnh nút ⋮ rộng cố định;
  - nút ⋮ nằm ngoài vùng cuộn, luôn thấy;
  - giữ chiều cao dải 56 dp và thẻ 48 dp như hiện tại.
- Thẻ đang chọn vẫn phân biệt bằng nền và viền như hiện tại; mỗi thẻ cao ít nhất 48 dp.

**Kiểm:**
- Cỡ chữ thực tế = 13 sp × thang app (0,9 / 1,0 / 1,15 / 1,3 — `AppearanceSettings.FONT_PERCENTS`) × cỡ chữ hệ thống.
- Kiểm hai mức: app 1,3 với hệ thống 1,0, và app 1,3 với hệ thống 1,3.
- `Paint.measureText` không chạy được trong test JVM thuần. Tách một hàm thuần ước lượng bề rộng nhãn và quyết định "vừa / cần cuộn" để test, hoặc dùng test `androidTest` (chỉ cần biên dịch được).
- Nếu chọn cách cuộn ngang, test phải khẳng định cách đó được bật ở thang này.

## 2. B2 — Tiêu đề lặp trong Thư viện

**Hiện tại:** `sectionCard(icon, title)` vẽ chữ `icon` (20 sp, màu nhấn) rồi `title`. Nhiều trang truyền cùng một chữ cho cả hai, nên tiêu đề hiện hai lần, ví dụ "RAW  RAW". Thanh trên cùng lại ghi "Thư viện · RAW", và thẻ RAW đang được chọn, nên cùng một chữ xuất hiện tới bốn lần.

| Tệp:dòng | Lệnh gọi hiện tại | Sửa |
|---|---|---|
| `RawPageFactory:28` | `sectionCard("RAW", "RAW")` | trang danh sách: bỏ hàng tiêu đề trong thẻ |
| `GlossaryPageFactory:80` | `sectionCard("Glossary", "Glossary")` | trang danh sách: bỏ hàng tiêu đề trong thẻ |
| `PronounPageFactory:34` | `sectionCard("Pronoun","Pronoun")` | trang danh sách: bỏ hàng tiêu đề trong thẻ |
| `ChapterSetPageFactory:143` | `sectionCard("Tập", "Tập")` | trang danh sách: bỏ hàng tiêu đề trong thẻ |
| `GlossaryPageFactory:188` | `sectionCard("Glossary", <tên>)` | trang sửa: giữ tên, bỏ chữ "Glossary" đứng trước |
| `PronounPageFactory:49` | `sectionCard("Pronoun", <tên>)` | trang sửa: giữ tên, bỏ chữ "Pronoun" đứng trước |

**Cách làm:**
- Thêm `LinearLayout plainCard()` trong `MainActivity`: cùng nền, viền, đệm và lề như `sectionCard`, nhưng không có hàng tiêu đề. Dùng cho 4 trang danh sách. Tên trang đã có ở thanh trên cùng và ở thẻ đang chọn.
- `sectionCard` với `icon` rỗng không vẽ ô chữ rỗng và khoảng trống 10 dp. Việc này cũng chữa thụt lề ở các trang Công việc và Dịch đang truyền `""`.
- Không đổi các lệnh gọi khác: `Files`, `Sample`. `JobsPageFactory` có hàm `sectionCard` riêng (`:539`); không sửa hàm đó. Thay emoji toàn app vẫn để gói sau (U7.3).

**Mô tả trang RAW** (`RawPageFactory:29`) rút còn một dòng, ví dụ:
> "Sắp theo số đầu tên tệp: 025_… là chương 25."

**Kiểm:**
- Test thuần cho hàm quyết định có vẽ chữ `icon` hay không: `icon` rỗng hoặc trùng `title` thì không vẽ chữ `icon` và khoảng trống 10 dp, **nhưng vẫn vẽ `title`**. Bỏ hẳn hàng tiêu đề chỉ xảy ra qua `plainCard()`.
- Ảnh chụp 4 trang Thư viện: mỗi trang chỉ còn tiêu đề ở thanh trên cùng.
- Ở các trang danh sách, chế độ chọn nhiều (hàng chọn, `headerRow`) và trạng thái rỗng vẫn hiện đúng.
- Các trang đang truyền `icon` rỗng sẽ đổi lề trái: Công việc (`JobsMainPageFactory:73,85,122`), Lịch sử (`JobsHistoryPageFactory:27`), Dịch (`TranslatePageFactory:176,271`). Chụp thêm Công việc và Dịch để xác nhận tiêu đề thẳng lề với nội dung.

## 3. B3 — "Hoàn tác" bật sẵn khi chưa áp dụng

**Hiện tại:**
- `SettingsPageFactory.recommendationCard` (dòng 107) luôn bật nút "Hoàn tác".
- Bấm khi chưa có gì để hoàn tác thì chỉ hiện toast `"Chưa có thay đổi để hoàn tác"` (`MainActivity.undoRecommendedSettings`).
- Bản lưu để hoàn tác là trường `recommendationUndo`, chỉ tồn tại trong bộ nhớ.

**Sửa:**
- Gọi `a.setButtonEnabled(undo, a.recommendationUndo != null)` (`MainActivity:3251`), không chỉ `setEnabled`. Màu chữ và nền hiện không đổi theo trạng thái, nên phải có alpha 0,45 thì mới nhìn ra nút đã tắt.
- Khi nút tắt:
  - thêm ngay dưới nút dòng `a.text("Hoàn tác dùng được sau khi bấm Áp dụng", 12, a.MUTED, false)` với `setSingleLine(false)`. Chỉ thêm view này khi nút tắt; trang được dựng lại sau mỗi thao tác nên không cần ẩn/hiện;
  - đổi content description thành "Hoàn tác cài đặt khuyến nghị, chưa dùng được: bấm Áp dụng trước". Nút tắt vẫn được TalkBack đọc, kèm trạng thái "đã tắt".
- Không sửa nút "Áp dụng" trong gói này, dù nó cũng thiếu alpha.
- Sau "Áp dụng", trang được dựng lại (`invalidatePage("Settings")` rồi `switchTab`), nút bật và dòng chữ phụ biến mất. Sau "Hoàn tác", nút tắt trở lại.
- Chỉ hai hàm `applyRecommendedSettings` và `undoRecommendedSettings` đổi `recommendationUndo`, và cả hai đã gọi `invalidatePage("Settings")`. Nếu thêm đường nào khác đổi trường này, đường đó cũng phải gọi `invalidatePage("Settings")`.
- Không lưu `recommendationUndo` xuống đĩa. Mất bản lưu khi app bị đóng là chấp nhận được; ghi điều này vào báo cáo.

**Kiểm:** test thuần, hoặc tách một hàm trạng thái, cho ba bước "chưa áp dụng → đã áp dụng → đã hoàn tác". Nút lần lượt tắt → bật → tắt.

## 4. Build và thử trên điện thoại

1. **Hồi quy** trên bản `git archive` sạch của commit cuối:
   ```
   JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
   gradlew --offline -q testDebugUnitTest compileDebugAndroidTestJavaWithJavac lintDebug
   ```
   Toàn bộ test `app` qua (mốc dev.25: 722 test, 5 SKIP, 0 FAIL, cộng các test mới); lint không thêm lỗi.
2. **Build** bằng `scripts/build-and-save.ps1 -Series 4.23-dev` để ra `4.23-dev.26`. Ghi lại bản, `versionCode` và SHA-256.
3. **Sao lưu điện thoại `15e84958` trước khi cài:**
   ```
   cmd /c "adb -s 15e84958 exec-out run-as com.ml.tblandroidtxt tar czf - . > private-data.tar.gz"
   ```
   - Không dùng `>` của PowerShell.
   - Đặt bản sao lưu ở `D:\App Translate Books\App Translate Books-device-backups\`.
   - Không giải nén bản sao lưu vào thư mục bằng chứng.
4. **Dừng, chờ owner gõ "duyệt cài".** Sau đó cài bằng `install-validated.ps1`:
   - `-Serial 15e84958`, `-ExpectedVersionCode`, `-ExpectedApkSha256`;
   - `-ExpectedApkCertificateSha256 47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`;
   - `-ExpectedDeviceSignatureToken abebea4b`.

   Chỉ `install -r`, không bao giờ gỡ cài đặt.
5. **Thử trên máy.** Không bấm nút tốn tiền hay nút xóa. Chỉ dùng dữ liệu `QA_`.

   **B1:**
   - đặt cỡ chữ app 130%, chụp dải thẻ Thư viện;
   - nếu chỉnh được cỡ chữ hệ thống mà không đụng cài đặt bảo mật, chụp thêm một ảnh ở cỡ chữ hệ thống lớn rồi trả về như cũ.

   **B2:** chụp 4 trang RAW / Glossary / Pronoun / Tập, và 1 trang sửa Glossary của một mục `QA_`.

   **B3:**
   1. chụp "Hoàn tác" đang tắt;
   2. bấm "Áp dụng", chụp nút đã bật;
   3. bấm "Hoàn tác", chụp nút tắt trở lại.

   Sau đó đối chiếu các giá trị không bí mật trong `settings.xml` trước và sau. **Chỉ in các khóa không bí mật đã lọc. Không chép nguyên tệp**: tệp có API key.

   **Cuối buổi thử:** trả cỡ chữ app về như trước khi thử.

**Ảnh:**
- chụp bằng `adb shell screencap -p /sdcard/x.png` rồi `adb pull`;
- kiểm tra 4 byte đầu là `89 50 4E 47`;
- mỗi ảnh một trạng thái;
- `MANIFEST` ghi ảnh nào cho mục nào;
- lưu ở `artifacts/qa/v4.23-polish-b1b3-<ngày>/`.

## 5. Báo cáo

- Các commit.
- Bản, `versionCode` và SHA-256 APK; đường dẫn bản sao lưu.
- Kết quả hồi quy: số test, SKIP, FAIL; số lỗi lint.
- PASS / FAIL / NOT_MEASURED cho B1, B2, B3, mỗi mục kèm đường dẫn ảnh.
- Xác nhận:
  - không sửa tệp `Editorial*`;
  - không có văn bản sách, API key hay dữ liệu cá nhân trong Git, artifact hay log.
