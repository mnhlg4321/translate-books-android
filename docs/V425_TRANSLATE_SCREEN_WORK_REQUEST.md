# v4.25: màn Dịch rõ ràng hơn, chọn RAW từ Thư viện, gọi lại khi còn chữ Nhật (gói việc)

- Quyết định sản phẩm được phục vụ: có giữ trong v4.25 nhóm sửa giúp người dùng hiểu model/cấu hình đang chọn, dùng RAW từ Thư viện cho một chương, và tránh lặp lại lỗi dịch khi response giữ nguyên chữ Nhật hay không. Bằng chứng trong gói này là replay offline, regression của app và kiểm luồng bằng provider giả; chất lượng Haiku sau sửa vẫn `NOT_MEASURED`.
- Nền: `main` và nhánh đã có `feature/v4.25` đều ở commit `8551f78e`. HEAD là hậu duệ của tag annotated `v4.24.1`, tag trỏ tới `01ab5313`. Dùng lại worktree `D:\P5E-builds\wt-v4.25` và checklist `release_checklists/v4.25.md`; không tạo nhánh/worktree mới.
- Owner yêu cầu ngày 2026-11-01, kèm 2 ảnh màn Dịch trên điện thoại `15e84958` (4.24.1-dev.5/318).
- Người điều phối đã đọc log và DB của điện thoại, bản sao để ngoài Git ở `D:\P5E-private\v424-phone-logs\`.

## 1. Owner hỏi gì, nguyên nhân đã chứng minh

| # | Owner thấy | Nguyên nhân (bằng chứng) | Việc làm |
|---|---|---|---|
| Q1 | Chọn gpt-5.6-luna thì dịch được, đổi sang Claude Haiku 5.5 thì lỗi luôn. App có khóa chỉ dùng luna không? | **Không khóa.** Job 354, chương 001: Haiku được gọi thật 3 lần, provider trả lời bình thường (`finish=stop`, $0,008985). Cả 3 lần Haiku đều giữ nguyên tiếng gầm katakana `「ルオオオ…！」` ở 3 dòng (RAW dòng 3, 55, 83), nên `ResponseValidator` từ chối với lỗi `RETAINED_SOURCE_LINE`. Luna ở job 335 đã phiên âm đúng thành "Ruôô…". Lần gọi lại gửi **y hệt** request cũ (cùng `requestHash` logic), nên lỗi lặp lại và job dừng ở chunk 1 (6 chunk còn PENDING). Mã chỉ cố định model ở Biên tập (`EditorialP5EFreshRawRoutingPolicy`, `V5ChatEditorialApiProvider`), không ở luồng dịch | Mục 2.1 |
| Q2 | Màn Dịch không còn hiện model đang chọn | Commit `b8c9dabb` (v4.23 phase B) đặt `translateMeta.setVisibility(View.GONE)` (`TranslatePageFactory.java:138`). Tiêu đề chỉ còn `providerMetaLine` = "Japanese → Vietnamese • openrouter" (`MainActivity.java:1283`), không có model | Mục 2.2 |
| Q3 | Thẻ "Cấu hình bản dịch" chỉ còn "Glossary 51 · Pronoun 28 · YAML ✓ · Sửa", không biết tệp nào đang được chọn | `TranslationConfigState.compactSummary()` (lõi, dòng 97–101) chỉ in số mục. Tên tệp chỉ hiện khi bấm "Sửa". Ngoài ra chữ "· Sửa" bị in thừa ngay cạnh nút "Sửa" | Mục 2.3 |
| Q4 | Thư viện đã có RAW, nhưng dịch 1 chương vẫn phải "Chọn tệp" từ thiết bị | Chế độ "1 chương (TXT)" chỉ có `chooseInput()` qua SAF (`MainActivity.java:1913`). Engine đã đọc được RAW thư viện (`tblraw://<id>`, `RawUri`, `LibraryRawStore.uri()`; chế độ Tập đang dùng đường này) | Mục 2.4 |

Thêm 1 lỗi người điều phối tìm thấy trong log (owner chưa hỏi):
- **L1.** `FileUtil.writeTextVerified` (`FileUtil.java:180`) chép output cũ ra `files/output-recovery-<ms>.txt` mỗi lần ghi, và không bao giờ xóa. Điện thoại hiện có 1.349 tệp, 55 MB, từ 2026-09 tới nay. Xem mục 2.5.

## 2. Việc cần làm (chỉ app điện thoại và lõi; không đụng Editorial)

### 2.1 Gọi lại có sửa khi còn chữ Nhật (Q1)
- Khi một lần thử bị từ chối vì `RETAINED_SOURCE_LINE`, lần gọi lại (vẫn trong giới hạn số lần hiện có) phải gửi kèm **một ghi chú sửa ngắn do app tạo**: liệt kê số dòng và đoạn nguồn còn sót (cắt ngắn), và yêu cầu dịch hoặc phiên âm những dòng đó (kể cả tượng thanh/tiếng gầm katakana). Không đổi instruction YAML, không đổi prompt v18.
- Ghi chú đặt ở phần user của request. Test golden 14/14 phải giữ nguyên, vì lần thử đầu không đổi.
- Rà cả họ lỗi: các lỗi xác định được và sửa cục bộ được (`UNCLOSED_BRACKET_OR_DIALOGUE`…) có nên kèm ghi chú tương tự không? Đề xuất, nhưng chỉ làm `RETAINED_SOURCE_LINE` trong gói này.
- Log/OBS ghi `repairHint=RETAINED_SOURCE_LINE lines=N` (không chứa văn bản).
- Tái hiện offline: replay 3 response bị từ chối đã lưu của job 354 (lấy từ bản sao DB) qua `ResponseValidator`, phải ra đúng `RETAINED_SOURCE_LINE` dòng 3/55/83. Test request của lần thử 2 có ghi chú, lần thử 1 thì không.
- Không gọi provider trong gói này. Việc Haiku có qua được hay không là `NOT_MEASURED`; muốn đo thì owner cấp quyền chi riêng.

### 2.2 Hiện model trên màn Dịch (Q2)
- Tiêu đề màn Dịch: "Japanese → Vietnamese • <model ngắn>", ví dụ "gpt-5.6-luna", "claude-haiku-5.5". Provider đưa vào phần chi tiết. Khi chưa chọn model thì ghi "chưa chọn model".
- Hộp xác nhận Bắt đầu và thẻ job hiện model đã dùng.
- Kiểm `NavigationPageTitles` và tiêu đề các trang khác. Chỉ đổi màn Dịch, trừ khi thấy cùng một lỗi ở chỗ khác.

### 2.3 Tên Glossary/Pronoun trong thẻ cấu hình (Q3)
- Dòng tóm tắt hiện **tên tệp kèm số mục**:
  - "Glossary: 001_GLOSSARY_KUMO_VOL13 (51) · Pronoun: 001_PRONOUN_KUMO_VOL13 (28) · YAML: <tên> ✓";
  - tên dài thì cắt giữa, giữ phần số chương;
  - "Không dùng" thì ghi rõ.
- Bỏ chữ "· Sửa" thừa.
- Ở chế độ Tập, vẫn ghi "theo từng chương của tập".
- `TranslationConfigState` nằm trong lõi nên PC dùng chung. Kiểm `desktop/` có dùng `compactSummary` không; nếu có thì PC phải có test đi kèm.

### 2.4 Chọn RAW từ Thư viện cho chế độ 1 chương (Q4)
- Chế độ "1 chương (TXT)" có hai nút:
  - "Từ Thư viện": `FilePickerSheet` liệt kê RAW thư viện, sắp theo số chương, có ô tìm;
  - "Từ thiết bị": `chooseInput()` như cũ.
- Chọn RAW thư viện thì input là `tblraw://<id>`, và tên hiển thị lấy từ thư viện.
- Ước tính, chuẩn bị (`PreparationCoordinator`), Bắt đầu, Tiếp tục và Lịch sử phải chạy đúng với `tblraw://` ở chế độ 1 chương, như ở chế độ Tập.
- Nếu có Glossary/Pronoun trùng số chương mà chưa chọn: **gợi ý** (`ChapterSetMatcher`), không tự đổi lựa chọn Active.
- Nhớ lựa chọn qua `UiSessionStore` khi mở lại app.
- RAW đã xóa khỏi thư viện thì báo rõ, không crash.

### 2.5 Giới hạn bản sao output (L1)
- Giữ tối đa 3 bản gần nhất cho mỗi output **và** tối đa 30 ngày. Xóa phần thừa sau mỗi lần ghi thành công và khi app khởi động.
- Không xóa bản đang cần để khôi phục cho lần ghi đang làm dở.
- Có test cho đường xóa. Lần đầu chạy trên điện thoại sẽ dọn 1.349 tệp cũ; ghi số tệp đã xóa vào log, không ghi tên chương.

## 3. Kiểm, build, ràng buộc

- Bộ kiểm sạch trên commit cuối (`git archive`): app, core, jdbc, desktop, test vàng 14/14 (không `-Dgolden.update`), lint 0, compile androidTest. Diff Editorial/`editorial-engine` rỗng.
- Instrumented chỉ trên emulator-5554, provider giả:
  - Q4: chọn RAW thư viện, ước tính, chạy, xong; mở lại app vẫn còn lựa chọn;
  - Q2/Q3: ảnh màn Dịch;
  - 2.1: provider giả trả lần 1 còn katakana, lần 2 sạch → job xong, và request lần 2 có ghi chú sửa;
  - 2.5: tạo 10 bản sao giả → còn 3.
- Ảnh trước/sau ở 361 dp, sáng/tối, cỡ chữ 100/130.
- APK bằng `build-and-save.ps1` (đặt JAVA_HOME trong lệnh).
- Cài lên điện thoại thật chỉ khi owner gõ "duyệt cài", và phải đủ ba bước: sao lưu run-as, đếm số trước/sau, `install-validated.ps1` (`-CheckOnly` trước).
- Không gọi provider. Không tag, merge hay push nếu owner chưa duyệt phát hành.
- Báo cáo:
  - nguyên nhân và file:line đã sửa cho từng mục;
  - số test;
  - đường dẫn ảnh;
  - APK (version/code/SHA-256);
  - các việc `NOT_MEASURED`: Haiku sau khi sửa, và hiệu năng trên điện thoại thật.
