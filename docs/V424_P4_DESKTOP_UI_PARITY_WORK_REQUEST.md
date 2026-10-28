# v4.24 P4 — App PC đủ các màn như app điện thoại (gói việc)

- Nhánh: `feature/v4.24`, worktree `D:\P5E-builds\wt-v4.24`, gốc `7db8c492`.
- Quyết định sản phẩm: owner thử `4.24-pc-dev.6` và thấy app PC "thiếu nhiều". P3 làm xong phần dịch (một chương, cả tập, Dừng/Tiếp tục, Lịch sử, `.msi`), nhưng chưa có gói nào yêu cầu PC có đủ các màn mà người dùng có trên điện thoại. P4 lấp khoảng trống đó để owner dịch trên PC mà không phải mở điện thoại.
- Mốc đo: giao diện điện thoại ở `app/src/main/java/com/ml/tblandroidtxt/*PageFactory.java`, `AppearancePage.java`, `PromptPreviewDialog.java`, `LibraryBulkUi.java`. Mỗi mục dưới đây ghi rõ tệp điện thoại cần đối chiếu. PC làm **đúng hành vi**, không cần giống từng pixel.

## 0. Nguyên tắc

1. **Chỉ sửa `desktop/`.** Không đổi hành vi app điện thoại, nên **không build và không cài APK** trong P4.
   - Ngoại lệ: một hàm thuần (không có `android.*`) mà PC cần, nằm trong `app/`. Được chuyển sang `translation-core` theo mẫu S5: hàm đi sang lõi, hàm cũ còn một dòng gọi sang.
   - Mọi lần chuyển như vậy gom vào **một commit riêng**, chạy lại toàn bộ test app và test vàng. Báo cáo liệt kê từng hàm đã chuyển.
   - Không cài APK; bản điện thoại kế tiếp sẽ mang thay đổi này.
2. **Dùng lại lõi, không viết lại logic.** Lõi đã có:
   - `GlossaryStoreCore`, `PronounStoreCore`, `ChapterSetStoreCore`, `ChapterSetMatcher`, `ChapterSetVolumeModel`;
   - `LibraryBulk`, `LibraryDeletePlan`, `LibraryRawOwnershipModel`, `LibraryImportPlanner`;
   - `PromptBuilder`, `PromptContextBuilder`, `AcceptedTranslationPreview`, `JobHistoryPageModel`, `SettingsCategoryModel`, `SettingsRecommendationModel`, `UiAppearanceCore`, `RunPanelFormatter`, `TranslationDashboardFormatter`.

   Nếu thấy PC phải viết lại một quy tắc đã có ở điện thoại (ví dụ cách đếm, cách khớp chương, cách xóa), dừng lại và áp dụng mục 0.1.
3. **Không gọi provider, không tốn tiền.** Mọi kiểm tra chạy với dữ liệu `QA_` và client giả.
   - Nút "Kiểm tra API" và "Chẩn đoán model" được làm, nhưng không bấm thật trong P4.
   - Mỗi nút gửi request phải có hộp xác nhận ghi rõ "sẽ gọi provider".
4. **Không đụng Biên tập.** Không thêm màn, nút hay mục cài đặt Biên tập. `git diff 7db8c492..HEAD -- '*Editorial*' editorial-engine` phải rỗng.
5. **Không đưa Công cụ phát triển lên PC.** Không làm Dry A/B, Blind review, Benchmark hay "Apply current config" của `JobsPageFactory`.
6. **Dữ liệu dùng chung với điện thoại.**
   - Mọi thay đổi Glossary/Pronoun/Tập lưu bằng đúng store lõi, cùng định dạng JSON.
   - "Xuất gói thư viện" từ PC rồi nhập vào điện thoại (và ngược lại) phải giữ nguyên số mục.
   - Test PC kiểm việc này bằng vòng xuất → nhập trên JVM.
7. **Từ ngữ:** chunk, prompt, token, model, provider, log, cache, Glossary, Pronoun, RAW/DRAFT/FINAL, API key, Base URL, endpoint giữ tiếng Anh; nhãn hành động và từ thường bằng tiếng Việt, giống điện thoại.

## 1. Điều hướng

Điện thoại tham chiếu: `DrawerNavigationModel`, `NavigationPageTitles`.

- Thanh bên trái có các mục giống điện thoại:
  - Dịch
  - Công việc
  - Thư viện, gồm các mục con RAW · Glossary · Pronoun · Tập
  - Cài đặt, gồm mục con Giao diện
- Giữ "Dịch cả tập" như hiện có; đặt dưới Dịch hoặc Thư viện · Tập, chọn chỗ nào hợp lý rồi ghi lý do.
- Tiêu đề trang dùng đúng chuỗi `NavigationPageTitles`, ví dụ "Thư viện · Glossary".
- Mở lại app thì trở về trang dùng lần cuối.
- Phím tắt:
  - `Ctrl+1..5` chuyển trang;
  - `Ctrl+F` vào ô tìm của trang đang mở (nếu trang có ô tìm);
  - `Esc` đóng hộp thoại.

## 2. Thư viện · RAW

Điện thoại tham chiếu: `RawPageFactory`, `LibraryBulkUi`.

- Danh sách RAW có: tên, số ký tự, ô tìm theo tên. Sắp theo số ở đầu tên tệp (`025_…` là chương 25).
- Thao tác trên một RAW:
  - Đổi tên (báo "Tên không hợp lệ" giống điện thoại);
  - Thay tệp;
  - Xóa.
- Chọn nhiều (Ctrl/Shift-click, "Chọn tất cả"), rồi:
  - Xóa: dùng `LibraryDeletePlan` và hỏi xác nhận; RAW đang thuộc Tập thì xác nhận lần hai như điện thoại.
  - Tạo tập từ các RAW đã chọn.
  - Xuất.
- Sau khi xóa có "Hoàn tác" như điện thoại.
- Kéo thả tệp `.txt` từ Explorer vào danh sách để nhập. Kết quả hiện "Đã thêm n · bỏ qua m (trùng/lỗi)".

## 3. Thư viện · Glossary

Điện thoại tham chiếu: `GlossaryPageFactory`.

- Danh sách Glossary có: tên, số mục, đánh dấu "Đang dùng". Nút:
  - "Dùng cho bản dịch";
  - "+ Nhập Glossary" (CSV; được chọn nhiều tệp để "Gộp file" như điện thoại);
  - "+ Tạo Glossary trống";
  - Xóa (hỏi trước; ghi "Không xóa file gốc trong máy").
- Mở một Glossary thì hiện **bảng thuật ngữ** gồm 4 cột `source`, `target`, `category`, `note`.
  - Sửa trực tiếp trong ô.
  - Thêm dòng ("+ Thêm thuật ngữ"), xóa dòng.
  - Lọc theo ô tìm (khớp source hoặc target) và theo Loại.
  - Sửa ngôn ngữ nguồn/đích và tên.
- "Lưu" ghi bằng `GlossaryStoreCore.upsert`.
  - Có thay đổi chưa lưu mà rời trang thì hỏi.
  - Cột `priority` của CSV vẫn bị bỏ qua như runtime hiện tại.
- "Kiểm tra dữ liệu": báo dòng trống, source trùng nhau, target rỗng, giống điện thoại.
- "Xem trước": hiện đúng đoạn Glossary sẽ đưa vào prompt (`toPromptText`).

## 4. Thư viện · Pronoun

Điện thoại tham chiếu: `PronounPageFactory`.

- Danh sách hồ sơ Pronoun có: tên, số quy tắc, số cảnh báo, "Đang dùng". Nút:
  - "Dùng cho bản dịch";
  - "+ Nhập Pronoun";
  - "Thay file";
  - Đổi tên;
  - Xóa (hỏi trước).
- Mở một hồ sơ thì hiện **bảng quy tắc**.
  - Định dạng 7 cột `from,speaker,target,self,call,scope,note` hiện thành 7 cột.
  - Định dạng cũ 3 cột vẫn hiện được và lưu lại vẫn là 3 cột. Không tự đổi định dạng.
  - Sửa trong ô, thêm dòng, xóa dòng, lưu.
- "Kiểm tra dữ liệu": hiện "Hợp lệ" hoặc danh sách cảnh báo theo đúng quy tắc của điện thoại.

## 5. Thư viện · Tập

Điện thoại tham chiếu: `ChapterSetPageFactory`, `ChapterSetMatcher`.

- Danh sách tập có: tên, số chương. Nút "+ Tạo tập mới", Đổi tên, Xóa.
  - Xóa tập có tùy chọn "Xóa cả RAW chỉ thuộc tập này", giống điện thoại.
- Màn sửa tập:
  - chọn RAW;
  - "Ghép theo chương" tự gợi ý Glossary/Pronoun cùng số chương (`ChapterSetMatcher`);
  - từng dòng chọn được Glossary/Pronoun hoặc "Không dùng";
  - báo các Glossary/Pronoun không khớp chương nào;
  - "Bỏ khỏi tập";
  - "Lưu tập (n chương)".
- Tập lưu ở đây hiện ngay trong "Dịch cả tập". Không có hai nơi lưu tập khác nhau.
- Công việc và Lịch sử lọc được theo Tập ("Lọc Công việc theo Tập", "Bỏ lọc theo Tập").

## 6. Dịch (một chương)

Điện thoại tham chiếu: `TranslatePageFactory`, `PromptPreviewDialog`.

Thêm vào trang Dịch hiện có:
- **Cấu hình bản dịch** (thu gọn được): ngôn ngữ nguồn/đích, Instruction YAML, Glossary/Pronoun đang dùng (bấm để đổi), "Xuất song ngữ", tên tệp đầu ra.
- **Ước tính**: số chunk, token, chi phí ước tính; trong khi chạy có "Còn lại (ước tính)" và "Token cả lượt".
- **Xem trước prompt**: chọn chunk 1..n, hiện system/user prompt và các mục Glossary/Pronoun khớp với chunk đó.
  - Dùng đúng `PromptBuilder.translationPrompt` và `PromptContextBuilder.preview`.
  - Có nút "Sao chép".
  - Test: với đầu vào `QA_`, prompt hiện ra phải giống từng byte bản ghi của test vàng.
- **Kết quả bản dịch**: xem bản dịch ngay trong app.
  - Đọc chunk đã chấp nhận (`AcceptedTranslationPreview`).
  - Có chế độ "Song song RAW | bản dịch" theo chunk; cuộn hai bên cùng nhau.
  - Có "Mở output" và "Mở thư mục".
- **Log hoạt động** (thu gọn được): "Xuất log", "Xóa log" (hỏi trước). Log không chứa văn bản sách và API key; dùng lại bộ lọc hiện có.

## 7. Công việc và Lịch sử

Điện thoại tham chiếu: `JobsMainPageFactory`, `JobsHistoryPageFactory`.

- Nhóm thời gian: "Hôm nay", "7 ngày", "Gần đây".
- Trạng thái: Đang dịch, Tạm dừng, Lỗi, Đã hủy, Đã bỏ, Cần xem lại, Input đã đổi.
- Mỗi job có:
  - token và chi phí;
  - các nút "Tiếp tục", "Dịch lại lỗi", "Bỏ job" (hỏi trước), "Mở kết quả", "Mở chi tiết công việc".
- "Tiếp tục gần nhất" ở đầu trang. Có "Lượt chạy tập" mở được.
- Lịch sử: tìm theo tên tệp, lọc theo Tập, "Tải thêm" (phân trang bằng `JobHistoryPageModel`).
- Chi tiết job: danh sách chunk với trạng thái, số lần thử, token, lỗi gần nhất. Không làm các công cụ phát triển ở mục 0.5.

## 8. Cài đặt

Điện thoại tham chiếu: `SettingsPageFactory`, `SettingsCategoryModel`, `SettingsRecommendationModel`.

Chia nhóm như điện thoại, dùng đúng nhãn tiếng Việt thay cho tên biến (`maxTokensPerChunk` → "Số token/chunk"):
- **Kết nối Provider:**
  - Preset Provider, Base URL, model, API key (đã có);
  - "Kiểm tra API" (có xác nhận, xem 0.3);
  - "Làm mới catalog" (đã có, giữ hộp xác nhận).
- **Mức chất lượng:**
  - Tiết kiệm / Cân bằng / Chất lượng cao, theo `SettingsRecommendationModel`;
  - "Cài đặt khuyến nghị" kèm "Áp dụng" và "Hoàn tác";
  - mức suy luận (tự động / tắt / thấp / vừa / cao), có hộp hỏi "Bật mức Cao?" như điện thoại;
  - Nhiệt độ.
- **Chunk:**
  - chế độ chunk (token hoặc ký tự);
  - Số token/chunk, Số ký tự/chunk;
  - Ngữ cảnh (ký tự), Ngữ cảnh chồng lấn;
  - Số dòng Glossary/chunk.
- **Chi phí:**
  - trần chi phí mỗi lượt, "Bỏ trần chi phí?" (hỏi trước);
  - "Dừng khi chạm trần chi phí";
  - "Dừng khi chưa biết giá".
- **Gọi lại và thời gian chờ:** Số lần thử tối đa, Thời gian chờ (giây), Độ trễ gọi lại, Độ trễ tối đa, "Gọi lại khi phản hồi rỗng".
- **Đầu ra:** Đầu ra song ngữ, Lưu đầu ra từng phần, Tên tệp đầu ra.
- **Instruction và profile:** Instruction YAML, ngôn ngữ nguồn/đích.
- **Log và chẩn đoán:** "Xóa log runtime" (hỏi trước), "Chẩn đoán model" (có xác nhận nếu gọi provider).
- **Chuyển dữ liệu với điện thoại:** giữ nguyên như hiện có.
- "Đã lưu tự động khi sửa", giống điện thoại. Hoặc giữ nút Lưu nhưng báo rõ khi có thay đổi chưa lưu; chọn một cách rồi ghi lý do.
- Không đưa lên PC: các mục ghi "chưa có tác dụng" trên điện thoại; mục Biên tập; "Làm mượt sau khi dịch" (refinement, owner không dùng Refine).

## 9. Giao diện

Điện thoại tham chiếu: `AppearancePage`, `UiAppearanceCore`.

- Chế độ: Theo hệ thống, Sáng, Tối.
  - Thêm bảng màu tối nếu `UiAppearanceCore` có. Nếu lõi chưa có bảng tối thì ghi `NOT_DONE` cho Tối, không tự bịa màu.
- Cỡ chữ, Mật độ (Gọn / Vừa / Thoải mái), Bo góc (Vuông / Vừa / Tròn), "Xem trước", "Khôi phục mặc định".
- **Sửa lỗi đã biết:** chữ trong hộp thoại xác nhận rất to.
  - Mọi `Alert`/`Dialog` dùng chung stylesheet và cỡ chữ của cửa sổ chính.
  - Ảnh chụp từng loại hộp thoại phải nằm trong báo cáo.

## 10. Hoàn thiện chung

- Mọi thao tác xóa hoặc ghi đè đều hỏi trước.
- Mọi lỗi hiện ngay cạnh chỗ thao tác, không chỉ ghi vào log.
- Việc dài (nhập nhiều tệp, quét thư viện) chạy ngoài luồng giao diện và có chỉ báo đang làm.
- Cửa sổ nhớ kích thước và vị trí. Bố cục vẫn dùng được ở 1280×720 và 1920×1080, không bị cắt chữ.
- Bàn phím: Tab đi qua mọi ô theo thứ tự hợp lý, Enter bấm nút chính của hộp thoại, viền focus luôn nhìn thấy.

## 11. Kiểm

Chạy trên bản sạch từ `git archive HEAD`, cùng lệnh như P3:
- `testDebugUnitTest :translation-core:test :translation-core-jdbc:test :desktop:test compileDebugAndroidTestJavaWithJavac lintDebug`.
- Test vàng 14/14 không đổi. Lint 0.

Test mới cho `desktop/`, tối thiểu:

| Mã | Kiểm cái gì |
|---|---|
| T1 | Sửa Glossary: thêm, sửa, xóa dòng, lưu, mở lại thì giống. Vòng xuất gói thư viện → nhập giữ nguyên số mục. |
| T2 | Pronoun 7 cột và 3 cột: mở, sửa một ô, lưu, định dạng giữ nguyên. |
| T3 | Tập: ghép theo chương với bộ `QA_`. Gợi ý giống `ChapterSetMatcher` trên cùng đầu vào. Lưu xong thì "Dịch cả tập" thấy tập. |
| T4 | Xem trước prompt chunk 1 của đầu vào test vàng giống từng byte tệp vàng. |
| T5 | Xóa nhiều RAW: RAW thuộc tập phải qua xác nhận lần hai; Hoàn tác khôi phục đủ. |
| T6 | Cài đặt: đổi từng nhóm, lưu, mở lại thì giống. Hồ sơ xuất ra vẫn không có API key, output hay Biên tập. |
| T7 | Lịch sử: lọc theo Tập và tìm theo tên trả đúng job trên DB `QA_`. |
| T8 | Không có nút nào gọi provider mà không qua hộp xác nhận. Test bấm thử bằng client giả và đếm số request: phải là 0 trước khi xác nhận. |

**Ảnh chụp:** chạy `Screenshots` cho mọi trang và hộp thoại mới, cả Sáng và Tối (nếu có), ở 1280×720. Lưu ngoài Git trong `D:\P5E-builds\qa-p4\shots\`.
- Chỉ dùng dữ liệu `QA_`.
- Không có văn bản sách thật hoặc API key trong ảnh.

**Build PC:** `scripts/build-desktop.ps1 -Msi` tạo `4.24-pc-dev.N` mới, lưu vào `artifacts/` và `backup/`. Ghi SHA-256 của ZIP và `.msi`. Không ký số.

## 12. Không được làm

- Không build APK, không cài lên điện thoại, không gọi provider, không tốn tiền.
- Không tag, merge hay push.
- Không sửa `editorial-engine/` hay tệp `*Editorial*`.
- Không thêm màn Biên tập, công cụ phát triển, Refine, hay mục "chưa có tác dụng".
- Không đổi schema DB hay định dạng JSON của store.
- Không đổi prompt v18 hay hành vi chia chunk.
- Không đưa văn bản sách, API key hay dữ liệu cá nhân vào Git, log hay ảnh.

## 13. Cách làm và báo cáo

- Làm theo thứ tự 1 → 10. Mỗi mục là một hoặc vài commit, có test kèm.
- Sau mỗi 3 mục, chạy bộ kiểm ở mục 11 rồi cập nhật `WORKSPACE_SNAPSHOT.md`.
- Báo cáo cuối có:
  - bảng 10 mục kèm trạng thái `DONE` / `NOT_DONE` và lý do;
  - danh sách hàm đã chuyển sang lõi (nếu có);
  - số test từng module;
  - kết quả diff Editorial;
  - đường dẫn ảnh chụp;
  - SHA-256 của gói PC;
  - cách owner cài bản mới để thử.

## 14. Bổ sung sau phản biện (mục này thắng nếu khác các mục trên)

Phản biện đã đối chiếu với mã ở `7db8c492`. Đường dẫn dưới đây tính từ worktree; `app/` nghĩa là `app/src/main/java/com/ml/tblandroidtxt/`.

1. **Xem trước prompt (§6, T4).**
   - Dùng `PromptPlan.forTranslation(chunk, i==0 ? "" : previous, AppValidator.normalize(s))`, tức đúng cái app gửi đi. Không dùng `PromptBuilder.translationPrompt`, vì hai hàm này chỉ cho cùng kết quả ở preset `full`.
   - T4 mở rộng `desktop/src/test/.../PromptCrossTest.java`: đọc fixture `golden-prompt` và đưa CSV thô vào `glossaryText/pronounText`.
   - Không sửa `PromptPreviewDialog` của điện thoại. Chênh lệch giữa preview điện thoại và prompt thật ghi là known issue trong báo cáo.
2. **Pronoun (§4).**
   - Màn sửa = bảng chỉ đọc (tách cột để xem) + sửa dạng văn bản thô (TextArea), lưu bằng `PronounStoreCore.upsert`. Không tách rồi ghép cột khi lưu.
   - Bảng sửa từng ô: `NOT_DONE`, chờ một gói có bộ ghi dòng trong lõi.
3. **Kiểm tra dữ liệu (§3, §4).**
   - Nội dung = `PromptContextBuilder.validate(glossary, pronoun).summaryText()` + 3 dòng Priority rule, như `MainActivity.showGlossaryPronounHealth`. Không tự thêm luật "dòng trống" hay "target rỗng".
   - Số cảnh báo Pronoun: thẻ danh sách dùng `conflicts.size()`, màn sửa dùng `malformedPronounRows`.
4. **Glossary (§3).** Bảng sửa từng ô, xóa dòng và lọc Loại là phần thêm của PC; vẫn làm. `Term` giữ nguyên 4 trường, lưu bằng `GlossaryStoreCore.upsert`.
5. **Tập (§5).**
   - Tập trên PC lưu bằng `ChapterSetStoreCore` trong DB desktop (bảng `chapter_sets` có sẵn trong `LibraryMigrationSpec`). Màn sửa dùng `ComboDraft` của lõi.
   - "Dịch cả tập" nạp tập đã lưu và ghi `session.setId`, để "Lọc theo Tập" hoạt động. `VolumePlan` chỉ còn là bảng xác nhận, không lưu gì.
   - Tập không đi qua gói thư viện (`LibraryArchive` không có Tập). Ghi rõ điều này trong màn hình và báo cáo; không đổi định dạng gói.
6. **Xóa và Hoàn tác (§2).**
   - Thêm `removeRows / restore / purgeFiles` vào `DesktopRawStore` (chỉ desktop). Dùng `LibraryBulk.remove/reinsert` và `LibraryDeletePlan`. Tệp chỉ bị xóa thật khi hết 10 giây Hoàn tác, như điện thoại.
   - Tra "đang dùng ở đâu" bằng truy vấn `chapter_sets` viết cho PC. Không chuyển `LibraryUsage`, không đọc `editorial_combos`.
7. **Công việc (§7).**
   - "Hôm nay / 7 ngày" là dòng tổng token/chi phí từ `repo.getUsageTotals(since)`, không phải nhóm job. "Gần đây" là danh sách job gần đây.
   - Thay `DesktopHistory.statusText` bằng đúng nhãn của `JobsMainPageFactory.statusText`, có cả "Xong" và cách gộp `CONTINUABLE` + `error` thành "Lỗi". Nếu hàm đó thuần thì chuyển sang lõi theo §0.1.
8. **Xem bản dịch (§6).**
   - Đọc theo chunk từ `TranslationRepositoryCore.getChunkRows(jobId)` (source/translated). `AcceptedTranslationPreview` chỉ giữ 1.200 ký tự cuối, không dùng cho việc này.
   - Chế độ song song RAW | bản dịch là phần thêm của PC.
9. **Giao diện (§9).**
   - Theo đúng `AppearanceSettings`:
     - chế độ Cố định / Theo hệ thống;
     - chọn palette sáng và palette tối (`UiAppearanceCore` đã có 15 palette tối, nên không áp dụng `NOT_DONE`);
     - 2 mật độ Gọn / Thoải mái;
     - 3 bo góc;
     - 4 cỡ chữ 90/100/115/130.
   - Vẫn sửa lỗi chữ to trong hộp thoại.
10. **Cài đặt (§8): bổ sung các mục còn thiếu.**
    - preset "Đầy đủ" (`full`);
    - Tỷ lệ mềm, Max output tokens, Số quy tắc Pronoun/chunk;
    - "Gọi lại khi có dấu hiệu bị cắt", "Gọi lại khi kiểm tra thất bại";
    - Cảnh báo chi phí, "Không giới hạn" (hỏi trước);
    - Lưu kết nối, Xuất hồ sơ, Xuất toàn bộ log.

    Ghi chú:
    - "Mức chất lượng" là `optimizationPreset`, tách khỏi `SettingsRecommendationModel` (Cài đặt khuyến nghị).
    - "Tên tệp đầu ra" (`outputFilenamePattern`) không có trên Cài đặt của điện thoại; được để ở trang Dịch.
    - Trang Dịch (§6) cũng không có "Làm mượt bản dịch" và công tắc Biên tập.
11. **Trang Dịch và Tệp: bổ sung.**
    - Trang Dịch: "Thư mục (nhiều chương)", "Nơi lưu bản dịch", "Chọn profile ngôn ngữ", "Cập nhật giá model" (có xác nhận nếu gọi mạng), dòng "Đã áp dụng: n mục Glossary · n quy tắc Pronoun", "Mở chi tiết lượt dịch trước".
    - Từ `FilesPageFactory`: quét thư mục output lấy các `.txt` mới nhất (mở được), "Sao lưu / Nhập / Xuất thư viện" (đặt ở Thư viện).
    - `SamplePageFactory` là chỗ trống, không làm.
12. **Điều hướng (§1).**
    - Chép chuỗi tiêu đề sang desktop. Không chuyển `NavigationPageTitles` hay `DrawerNavigationModel` vào lõi, vì chúng kéo theo Biên tập.
    - Tiêu đề trang Giao diện trên PC là "Cài đặt · Giao diện".
13. **Chặn đụng nhầm.**
    - Không chạy `-Dgolden.update`.
    - `git diff 7db8c492..HEAD -- app/src/test/resources/fixtures/golden-prompt` phải rỗng.
    - `git diff 7db8c492..HEAD -- app/src/main` phải rỗng, trừ commit chuyển-lõi đã liệt kê theo §0.1.
    - Không chuyển lớp nào có `import android.*` hoặc tham chiếu `Editorial*`. Không đọc bảng Biên tập.
    - `GlossaryStoreCore.loadAll/saveAll` và các hàm của `ChapterSetStoreCore` là package-private: gọi từ lớp desktop trong package `com.ml.tblandroidtxt` như `DesktopRuntime` đang làm, không gọi từ `...desktop`.
