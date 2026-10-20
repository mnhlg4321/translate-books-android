# v4.23 — Yêu cầu làm việc: hết giật lag, tổ hợp không dừng vì lỗi một chương, bảng tiến trình theo tổ hợp, nút bị cắt chữ, giao diện sáng

Trạng thái: ĐÃ PHẢN BIỆN (agent phản biện đọc mã tại `63f83840`, 2026-10-20; khẳng định chính đã kiểm lại).

Nhánh và thiết bị:
- Nhánh `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`, mốc `63f83840`.
- Điện thoại `15e84958` đang chạy `4.23-dev.11` / code 286, cài lúc 2026-10-19 22:40 (theo `dumpsys`). Snapshot của nhánh vẫn ghi "chưa cài" và cần sửa khi làm.
- Ranh giới "không động vào Biên tập" như `docs/V423_TRANSLATE_LIBRARY_WORK_REQUEST.md` mục 2. Riêng J5: token màu dùng chung sẽ đổi giao diện Biên tập theo, nhưng không sửa tệp `Editorial*`.

**Quyết định sản phẩm phục vụ:**
- Dịch cả tổ hợp mà không phải trông chừng hay thiết lập lại.
- App phản hồi ngay khi thao tác.
- Theo dõi tiến độ và chi phí của tổ hợp trên một màn hình điện thoại.
- Có giao diện sáng.

## 1. Nguyên nhân đã chứng minh (file:dòng tại `63f83840`, thư mục `app/src/main/java/com/ml/tblandroidtxt/`)

### J1 — Giật/đơ khi tick chương, chọn trong Library, sửa Settings

**Nguyên nhân chính.** `updateActionButtons` gọi `FileUtil.validateTreeWritable` trên luồng UI (`MainActivity:2204`, rồi lần nữa qua `startReadinessError:2237`). Hàm này **tạo, ghi rồi xóa một tệp thử** trong thư mục output qua SAF (`FileUtil:394–414`).
- Mỗi lần kế hoạch dịch báo tiến độ, `renderPreparedBatch` lại gọi `updateActionButtons` (`PreparationCoordinator:132,207` → `MainActivity:3061`).
- Vì vậy tick 1 chương trong tổ hợp 14 chương gây khoảng 16 lần tạo/xóa tệp thử, cộng khoảng 16 lần mở DB (`hasFailedChunks`), tất cả trên luồng UI.

**Phần cộng thêm:**
- `collectSettings` chạy 2–3 lần mỗi lần tick. Mỗi lần gọi 2 truy vấn `displayName` qua ContentResolver và `toPromptText` (`:2302,2307,2916,2230,3055`; `TranslateSourceFactory:316`).
- `freshBindings` → `ChapterBinding.of` chạy `toPromptText` và 2 lần SHA-256 cho **mỗi** chương được tick (`ChapterBinding:31–39`).
- `render()` dựng lại cả danh sách chương và truy vấn `listSets` (`TranslateSourceFactory:199`).
- Mỗi lần quay về tab Dịch lại chạy `sourceUi.refresh()` → kế hoạch dịch lại từ đầu (`MainActivity:626`).
- Library:
  - tick một thẻ là dựng lại cả trang (`LibraryBulkUi.refresh` → `refreshPagePreservingScroll`);
  - mỗi thẻ Pronoun phân tích toàn văn 2 lần (`PronounStore.Profile.count:22` + `validate`).
- Settings:
  - mỗi lần sửa một ô thì lưu prefs 2 lần (`:714`, `:764`). Dùng `apply()` nhưng ghi lại cả XML, gồm cả `glossaryText`/`pronounText` lớn;
  - sau đó chạy `updateInputEstimate` → toàn bộ chuỗi ở trên.
- Lúc đang dịch, mỗi broadcast làm:
  - gọi `updateActionButtons` 2 lần (`:219,222`);
  - gán lại `logView` 12.000 ký tự (`:3311`);
  - các ô cao theo nội dung phải đo lại bố cục.

**Đã kiểm, không phải nguyên nhân:**
- `refreshPendingJobCard` đã chạy nền.
- Việc chia chunk lại trong `PreparationCoordinator` chạy nền. Nó tốn CPU nhưng không làm đơ UI.

### J2 — Tổ hợp Vol 6 dừng ở chương 4; Retry xong không chạy tiếp

Bằng chứng từ log điện thoại `files/tbl_runtime.log` (chỉ đọc), 2026-10-20 08:52–08:54:
- Chunk 6/6 của `004_ATE_JP_VOL6.txt` (job 297) bị từ chối 3 lần với `UNCLOSED_BRACKET_OR_DIALOGUE`.
- Kết quả: "Tổ hợp: 3 xong · 1 lỗi · 10 chưa chạy". Owner Retry job 297 → job xong, rồi dừng.

Nguyên nhân:
- Chỉ `ChapterLocalException` được chạy tiếp sang chương sau (`ChapterRunOutcome:26–28`, `TranslatorService:381–386`). Lỗi kiểm định sau lượt retry cuối thì **dừng cả lượt**.
- Không có lối "chạy tiếp phần còn lại của tổ hợp".
- Nhãn "Lỗi khởi tạo:" bị gắn cứng (`TranslatorService:317`).
- Khi một chunk lỗi giữa lượt, app phát `"error"` (`:790`), giao diện liền coi lượt đã dừng (`MainActivity:218,232,237–239`).

Đếm ngoặc trên bản sao DB riêng (`D:\P5E-private\v423-diagnosis-20261020`, ngoài Git; chỉ in số đếm):
- RAW của chunk 6 cân bằng ngoặc. Bản bị từ chối thừa 29 dấu `「`, tức model trả hỏng thật.
- Trong DB có 4 chunk từng bị lỗi này, **3/4 có RAW lệch ngoặc sẵn**. `ResponseValidator.hasUnclosedPairs` (`:32,60`) chỉ xét bản dịch, nên với các chunk đó bản dịch trung thực không bao giờ qua kiểm.
- Cả hai chỗ gọi đều truyền `chunk.mainContent` (không gồm ngữ cảnh: `TranslationEngine:177,252,287`), nên đây là mốc so sánh đúng.

**Lỗi chi phí (phản biện tìm ra):**
- Trần chi phí thực chất áp **theo từng chương**: `enforceCostLimit` dùng `metricCost`, mà biến này bị đặt lại mỗi job (`:908,447`).
- Kiểm retry của engine cũng theo job (`TranslationEngine:144`).
- Hộp xác nhận trước khi chạy lại hiện trần chi phí đặt cạnh ước tính của **cả tổ hợp** (`MainActivity:1980,1988`).

### J3 — Ô tiến trình nhảy kích thước; thiếu số liệu của tổ hợp

- `statBox` cao theo nội dung (`TranslatePageFactory:392–402`).
- Broadcast chỉ mang số của job hiện tại. `resetMetrics` xóa token, chi phí và giờ bắt đầu ở mỗi chương (`TranslatorService:434,440–447`).
- Thẻ tiến trình nằm dưới danh sách 14 chương và mục cấu hình (`TranslatePageFactory:110,161`), nên không thể "một màn hình" nếu không đổi vị trí.

### J4 — Nút bị cắt chữ

`FilesPageFactory:70` ("Resume last checkpoint") dùng `LayoutParams(0, dp(48), 1)`. Cùng kiểu (chiều cao cố định cho nút có chữ) còn ở:
- `FilesPageFactory` 68, 103, 105, 197;
- `GlossaryPageFactory` 71, 73, 75, 128–132, 176–197, 210;
- `PronounPageFactory` 25, 38;
- `RawPageFactory` 31, 58, 101–107;
- `JobsPageFactory` 75–87, 134–136, 150–164, 227–251;
- `LibraryBulkUi` 78, 80, 127–144, 221;
- `TranslateSourceFactory` 83, 85, 227;
- `TranslatePageFactory` 96, 105, 175;
- `SettingsPageFactory` 104, 107;
- `ChapterSetPageFactory` 149;
- `MainActivity` 697 (`dp(38)`), 1266;
- mặc định của `tinyButton` (`:3492`).

### J5 — Chưa có giao diện sáng

- Palette là trường token (`MainActivity:473`). `GREEN/RED/AMBER` cố định (`:94–96`) nhưng không phải hằng biên dịch, nên đổi thành token được an toàn; Biên tập đọc `a.GREEN`… sẽ theo.
- Những chỗ sẽ hỏng ở nền sáng:
  - `primaryButton` vẽ chữ `TEXT` trên nền `BLUE` (`:3498`);
  - `FilePickerSheet:49` gắn cứng theme hộp thoại tối;
  - `styles.xml` đặt `windowLightStatusBar=false`, và `configureSystemBars` (`:501`) không bật icon sáng;
  - nhãn `TOKENS` dùng màu token trên nền pastel cố định (`TranslatePageFactory:351–352`).
- Có 63 chỗ `AlertDialog.Builder` (18 trong Biên tập). Chỉ đổi theme của Activity trước `setContentView` (rồi `recreate()`) thì mới đổi được hộp thoại mà không sửa Biên tập.

## 2. Thiết kế đã chốt

### J1 — Phần lợi nhanh (làm đầu tiên)

1. **Đo trước:**
   - bật StrictMode (disk + network, `penaltyLog`) khi `BuildConfig.DEBUG`;
   - thêm `Trace.beginSection` cho `updateActionButtons`, `startReadinessError`, `collectSettings`, `renderPreparedBatch`, `freshBindings`, `render`, `refreshPagePreservingScroll`;
   - test thiết bị đếm số lần gọi kiểm SAF trong một lần tick, và thời gian tick → khung hình kế tiếp. Dùng dữ liệu tổng hợp: 19 RAW, 19 Glossary, 18 Pronoun, tổ hợp 14 chương.
2. **Bỏ kiểm SAF ghi thật khỏi `updateActionButtons` và `startReadinessError`.**
   - Hai chỗ này chỉ kiểm `hasPersistedWrite`: đọc danh sách quyền, không tạo tệp.
   - Kiểm ghi thật chỉ chạy khi chọn thư mục, khi `onResume` (chạy nền) và trong `dispatchTranslation` lúc bấm Start (`:2031`). Service vẫn kiểm lại (`:342,883`).
   - Không dùng bộ đệm thời gian.
3. Khi kế hoạch còn đang chuẩn bị (PREPARING) thì không gọi `updateActionButtons`. Gộp các lần gọi lại thành tối đa 1 lần mỗi khung hình.
4. `hasFailedChunks` chạy nền, lưu kết quả lại, làm mới khi có broadcast done/error.

**Phần còn lại (sau J2):**
- Tick chỉ cập nhật đúng dòng đó và dòng đếm. `applySelection` debounce 400 ms.
- Lưu tạm `ChapterBinding` theo (chương, hash Glossary, hash Pronoun).
- Quay lại tab Dịch không chuẩn bị lại kế hoạch nếu lựa chọn không đổi.
- Library: tick chỉ cập nhật thẻ đó và dòng đếm. Lưu tạm số luật của Pronoun theo hash văn bản.
- Settings: mỗi lần sửa chỉ lưu 1 lần, ở luồng nền. Chỉ gọi `updateInputEstimate` khi trường đó ảnh hưởng ước tính (model, chunk, giá).
- Lúc đang dịch, mỗi broadcast chỉ gọi `updateActionButtons` 1 lần. Log chỉ nối thêm phần mới thay vì gán lại cả 12.000 ký tự.

**Tiêu chí PASS:**
- 0 lần kiểm SAF ghi thật cho mỗi lần tick, đổi Settings, hay chọn trong Library.
- StrictMode không báo đọc/ghi đĩa trên luồng UI ở các đường này.
- Tick → khung hình kế tiếp: p95 < 50 ms trên emulator, có log trước/sau.

### J2 — Tổ hợp chạy tiếp, có kiểm soát chi phí

1. **Bộ đếm theo lượt:** service giữ `sessionTokens`/`sessionCost`.
   - Đặt về 0 trong `runPreparedJobs`, cộng dồn trong `addUsage` (`:835`).
   - Khi tiếp tục job cũ, nạp sẵn một lần chi phí đã có của job đó.
   - Đưa vào broadcast và `RuntimeStateStore`.
2. **Trần chi phí cho cả lượt:** `enforceCostLimit` so trần với `sessionCost`. Hộp xác nhận trước khi chạy ghi đúng "trần cho cả lượt".
3. **Lỗi kiểm định thành lỗi riêng chương:** `RejectedResponseException` sau lượt retry cuối được coi là lỗi riêng chương, kèm hai chốt chặn:
   - dừng cả lượt nếu **2 chương liên tiếp** lỗi kiểm định;
   - trần chi phí của lượt (mục 2).

   Vẫn dừng cả lượt khi: không rõ đã gửi, lỗi xác thực, hết hạn mức, lỗi mạng.
4. **Thông báo:**
   - lỗi riêng chương phát `"running"` kèm dòng "Chương 4 lỗi, chuyển chương 5", không phát `"error"`;
   - đổi nhãn "Lỗi khởi tạo:" thành "Lỗi:" khi `activeJobId > 0`.
5. **Kiểm ngoặc so với RAW:** với từng cặp ngoặc, chỉ từ chối khi độ lệch của bản dịch khác 0 **và** (ngược dấu với RAW **hoặc** lớn hơn RAW). Bản dịch đóng lại ngoặc dở, hoặc đổi sang “ ”, vẫn qua.
   - Cùng họ lỗi:
     - `REPEATED_RESPONSE_LOOP` (`:33`): bỏ qua khi RAW cũng lặp y như vậy;
     - `LIKELY_TRUNCATED_END` (`:34`): finish `UNKNOWN` mà output còn xa giới hạn thì xử như `STOP`.
   - `JobStore.manualReplace` (`:33`) cũng được hưởng bản sửa này.
6. **"Tiếp tục tổ hợp":** không cần bộ chạy mới.
   - Đọc `ChapterSession`, chọn lại tổ hợp, tick các chương chưa xong, rồi đi đường Start bình thường.
   - `ResumePrompt.chooseJobs` + `continueExistingJob` sẽ tiếp tục job dở (kể cả chunk lỗi).
   - Nút này có ở: thẻ "Job đang dở", thẻ lượt tổ hợp trong Jobs, và hộp hỏi sau khi Retry một job thuộc tổ hợp ("Còn n chương chưa chạy. Chạy tiếp?").

### J4

Mọi nút có chữ dùng `LayoutParams(…, -2, …)` + `setMinHeight(dp(44))`. `tinyButton` mặc định wrap. Kiểm ở font 130%.

### J3 — Bảng tiến trình một màn hình

- **Khi đang chạy:** thẻ tiến trình chuyển lên **đầu** trang Dịch; thẻ nguồn và mục cấu hình thu gọn thành 1 dòng ("Tổ hợp ATE_JP_VOL6 · 14 chương · ▸"). Khi dừng hoặc xong thì trả về như cũ.
- **Bố cục** (rộng 360 dp, mỗi ô ~95 dp; chữ tự co, tối thiểu 10 sp; ô cố định 56 dp):

```
Đang dịch · Tổ hợp ATE_JP_VOL6                     ⏸  ✕
Tổ hợp  ████████░░░░░  Chương 4/14 · 3 xong · 0 lỗi
Chương  █████████░░░░  chunk 5/6 · 004_ATE_JP_VOL6.txt
┌───────────┬───────────┬───────────┐
│ 308k tok  │ $0.2602   │ 26:11     │
│ Tổ hợp    │ Chi phí   │ Đã chạy   │
├───────────┼───────────┼───────────┤
│ ~31:00    │ 0 · 0     │ G10 · P4  │
│ Còn lại   │ Lỗi · fb  │ Luật      │
└───────────┴───────────┴───────────┘
~$0.43 dự kiến cả tổ hợp · chương này 11.6k tok · $0.006
▸ Bản dịch gần nhất (thu gọn)
```

- Lượt một tệp: không có hàng "Tổ hợp". Ô "Tổ hợp" thành "Token".
- Số liệu lấy từ broadcast (bộ đếm theo lượt ở J2). Không truy vấn DB trên luồng UI.
- Bỏ nền pastel cố định, dùng token.

### J5 — Lát A rồi lát B, cùng đợt này (owner 2026-10-20: "đưa hết vào 1")

- **Lát A:**
  - màu trạng thái thành token, có bộ tối và bộ sáng đạt tương phản AA;
  - thêm token `onAccent` cho chữ trên nút chính;
  - thêm 2 palette sáng: **Light** (trắng xám) và **Paper** (kem);
  - icon thanh trạng thái/điều hướng sáng–tối theo palette;
  - chọn theme Activity (`Theme.Material.Light.NoActionBar` hoặc tối) trong `onCreate` trước `setContentView`; đổi palette thì `recreate()` (lưu trạng thái cần giữ qua `onSaveInstanceState`);
  - `FilePickerSheet` lấy theme theo palette;
  - chuyển 15 màu cố định sang token.
- Chụp ảnh cả các màn Biên tập ở palette sáng (không sửa mã Biên tập). Chỗ nào khó đọc thì ghi lại.
- **Lát B** (làm sau khi lát A xong và có ảnh kiểm; vẫn chung đợt và chung bản dev.12):
  - **Theo hệ thống:** tự chọn palette sáng hoặc tối theo chế độ Android. Owner chọn được palette nào dùng cho sáng và cho tối. Khi hệ thống đổi chế độ thì `recreate()`.
  - **Màu nhấn:** 8 màu đặt sẵn và ô nhập mã hex. App tự tính `onAccent` (đen hoặc trắng) theo độ tương phản. Từ chối màu nhấn không đạt tương phản AA với nền của palette đang dùng, và giải thích lý do.
  - **Cỡ chữ app:** 90 / 100 / 115 / 130%, nhân thêm trên cỡ chữ hệ thống. Áp ở các helper `a.text(...)` và nút. Ô tiến trình (J3) vẫn cố định chiều cao và tự co chữ.
  - **Mật độ:** "Thoải mái" / "Gọn". Đổi padding, khoảng cách và chiều cao tối thiểu của nút, nhưng không xuống dưới 44 dp (J4).
  - **Bo góc:** vuông / vừa / tròn, áp ở `a.card(...)` và nút.
  - **Xem trước trực tiếp** trong Settings > Appearance: một thẻ mẫu có nút chính, nút phụ, chip trạng thái và chữ phụ. Có nút "Khôi phục mặc định".
  - Lưu trong prefs `appearance` (khóa mới; không đổi khóa `palette` cũ). Mặc định giữ đúng giao diện hiện tại.
  - Màn Biên tập tự đổi theo vì chỉ dùng token và helper; không sửa mã Biên tập.
- **Kiểm J5 bằng skill `ui-ux-pro-max`:** tương phản (`color-accessible-pairs`, `color-dark-mode`), kích thước vùng chạm, checklist `references/pro-rules.md`.
- **Ảnh cần chụp:**
  - mỗi palette sáng và tối;
  - cỡ chữ 130% kết hợp mật độ "Gọn";
  - một màu nhấn tùy chọn;
  - hộp thoại;
  - màn Biên tập ở palette sáng.

### J6 — `UNCLOSED_BRACKET_OR_DIALOGUE` xảy ra nhiều dù RAW đủ ngoặc (owner 2026-10-20)

**Đo trên bản sao DB của điện thoại** (ngoài Git, chỉ đếm, không in chữ sách; script `bracket_survey.py`, `bracket_rejects.py`, `bracket_lines.py`, `bracket_ctx.py`, `bracket_tokens.py`, `coverage.py` trong scratchpad của phiên chính):

- **Cách chia chunk không phải nguyên nhân.**
  - Có 307 job; 306 job có toàn văn RAW cân bằng 「」『』.
  - Trong 1.231 chunk chỉ 1 chunk lệch, và chunk đó không thuộc job cân bằng nào.
  - Cả **58/58** lượt bị từ chối vì ngoặc đều rơi vào chunk RAW cân bằng.
  - Chỉ 1,6% chunk có thoại kéo dài nhiều dòng; 0 chunk như vậy trong nhóm lỗi.
- **Lỗi nằm ở bản dịch của model** (`openai/gpt-5.6-luna`, `maxOutputTokens = 4096`, `maxTokensPerChunk` 1650–1800):
  - **19/58 bị cắt vì hết token:** `finish_reason = length`; 13 lượt có output ≥ 4000 token. Model suy luận cũng tiêu token đầu ra (log thấy `reasoning` 145–544). Bản bị cắt dừng giữa câu thoại nên báo lỗi ngoặc.
  - Nhóm thiếu đúng một `」` (20 lượt): **17/20 có dấu mở bị bỏ dở nằm ở 3 dòng cuối**, và bản dịch **ngắn hơn RAW 75–92 dòng** ở phần lớn các ca. Đây là bản bị cắt hoặc dừng sớm, không phải lỗi ngoặc.
  - **7 lượt mở bằng `「` nhưng đóng bằng `”`**: lẫn kiểu ngoặc, sửa được theo luật cố định; khoảng 13 lượt lệch lớn khác cùng dạng.
  - 11 lượt thừa `(` ASCII (RAW không có); 2 lượt lệch `『』`; 1 lượt thừa `[`.
- **Thiệt hại:**
  - 31 chunk bị dính; 26 chunk qua ở lượt retry sau, 5 chunk vẫn lỗi.
  - Khoảng **0,20 USD** đã trả cho các lượt bị từ chối.
  - Retry gửi lại **đúng cùng request** (cùng `requestHash`, cache vào), nên với lỗi cắt do hết token thì gần như chắc chắn lại bị cắt.
- **Bản đã được nhận vẫn ổn:** 1.169/1.178 chunk có ≥ 95% số dòng của RAW; 0 chunk dưới 80%. Bộ kiểm hiện đang chặn được bản cụt.

**Sửa:**
1. **Báo đúng nguyên nhân.** `ResponseValidator` xếp `FINISH_REASON_LENGTH` / `OUTPUT_TOKEN_LIMIT_REACHED` / `LIKELY_TRUNCATED_END` **trước** lỗi ngoặc, và thông báo ghi "Bản dịch bị cắt (hết token đầu ra)".
   - Thêm kiểm **thiếu dòng**: bản dịch có < 80% số dòng không trống của RAW thì báo `OUTPUT_MISSING_LINES`.
   - Sự kiện `chunk_attempt_failed` ghi loại ngoặc, độ lệch, `finish`, `output_tokens`, tỉ lệ dòng (không ghi chữ).
2. **Bị cắt thì không gửi lại y hệt.** Lần retry sau chọn một trong hai:
   - (a) tăng giới hạn đầu ra lên mức model cho phép (mặc định đề xuất 8192 cho model suy luận; chỉ trả tiền cho token thực dùng);
   - (b) nếu đã ở mức tối đa thì chia chunk làm đôi bằng `Chunker.splitForContextOverflow` (đã có cho lỗi vượt context).

   Ghi lý do vào log.
3. **Mức suy luận cho bản dịch.** Thêm tùy chọn `reasoningEffort` cho luồng dịch (đã có tham số trong `OpenAICompatibleClient` nhưng chỉ adapter P5E dùng). Mặc định "low" cho model có suy luận, để giảm token suy luận và nguy cơ bị cắt. Hiện trong Settings > Performance.
4. **Sửa ngoặc theo luật cố định trước khi kiểm.** Nếu bản dịch mở `「` và đóng `”` (số `「` = số `”`, không có `」`) trong khi RAW dùng `「」`, thì đổi các `”` đóng thành `」`. Làm tương tự cho `『』` / `‘’`. Chỉ đổi dấu, không đổi chữ; ghi `QUOTE_STYLE_REPAIRED` vào cảnh báo.
5. **Ngoặc `(` ASCII:** thuộc họ "so với RAW" ở J2-5. RAW không có `(` mà bản dịch thừa `(` chưa đóng thì vẫn từ chối, nhưng thông báo ghi rõ loại ngoặc.
6. **Mặc định khuyến nghị cho owner:** `maxOutputTokens` 8192 với gpt-5.6-luna. Hiện trong Settings dưới dạng gợi ý, không tự đổi cài đặt của owner.

**Test:**
- Host:
  - bản `finish = length` → lần sau dùng giới hạn lớn hơn hoặc chia đôi, không gửi request giống hệt;
  - sửa `「…”` → `「…」`;
  - bản thiếu > 20% dòng → `OUTPUT_MISSING_LINES`;
  - thứ tự thông báo lỗi.
- Thiết bị (provider giả):
  - lượt trả `length` rồi tự chia đôi và hoàn tất;
  - lượt trả ngoặc lẫn kiểu được nhận kèm cảnh báo.
- Offline replay: chạy validator mới trên 58 response đã lưu trong bản sao DB (chỉ đếm). Kỳ vọng:
  - 7+ ca lẫn kiểu được nhận sau sửa;
  - các ca cụt báo "bị cắt";
  - không ca nào hiện bị từ chối lại thành được nhận nếu thiếu dòng.

### Công cụ thiết kế cho J3, J4, J5

Skill `ui-ux-pro-max` (MIT, nguồn `nextlevelbuilder/ui-ux-pro-max-skill`, commit `56ea9cf1`) đã được cài cho tài khoản tại `C:\Users\ADMIN\.claude\skills\ui-ux-pro-max` (owner duyệt 2026-10-20).
- Script đã được soát: chỉ dùng thư viện chuẩn, không gọi mạng, chỉ ghi tệp khi có `--persist`.
- Skill chỉ nạp ở phiên mở mới.
- Dùng để soát:
  - độ tương phản palette tối và sáng (`color-accessible-pairs`);
  - vùng chạm tối thiểu 44 dp;
  - bố cục không nhảy, bảng tiến trình;
  - checklist trong `references/pro-rules.md`.

Lệnh gọi trên máy này:

```
py -3 "C:\Users\ADMIN\.claude\skills\ui-ux-pro-max\scripts\search.py" "<truy vấn>" --domain ux
```

Lưu ý:
- App dùng Android View viết bằng Java; skill không có stack này. Chỉ dùng phần `--domain ux`, `color`, `web` (hướng dẫn app gốc). Không dùng `--stack jetpack-compose` làm mã mẫu.
- Không dùng `--persist` trong repo.

## 3. Thứ tự, test, điểm dừng

**Thứ tự:** J1 lợi nhanh → J2 + J6 (cùng chạm `ResponseValidator` và đường retry) → J1 phần còn lại → J4 → J3 → J5 lát A → J5 lát B.

**Test host:**
- so ngoặc / lặp / cụt câu với RAW (kể cả ca bản dịch đóng ngoặc dở);
- phân loại lỗi kiểm định thành lỗi riêng chương;
- dừng khi 2 chương liên tiếp lỗi;
- cộng dồn chi phí lượt qua job được tiếp tục;
- trần chi phí theo lượt;
- `ChapterBinding` dùng lại bản đã lưu tạm;
- J6: bị cắt (`length`) thì retry với giới hạn lớn hơn hoặc chia đôi; sửa `「…”`; `OUTPUT_MISSING_LINES`; offline replay 58 response đã lưu;
- J5: tính `onAccent` và từ chối màu nhấn không đạt AA; hệ số cỡ chữ và mật độ (không xuống dưới 44 dp); đọc/ghi prefs `appearance` và giữ mặc định cũ.

**Test thiết bị (provider giả):**
- tổ hợp 3 chương, chương 2 hỏng ngoặc 3 lần → chương 3 vẫn chạy, không có `"error"` giữa lượt, tóm tắt "2 xong · 1 lỗi";
- 2 chương liên tiếp hỏng → dừng;
- "Tiếp tục tổ hợp" sau force-stop;
- đếm lần kiểm SAF mỗi lần tick = 0;
- StrictMode sạch;
- ảnh 360 dp, font 100% và 130%, palette tối và sáng, có cả hộp thoại;
- J5:
  - đổi palette, màu nhấn, cỡ chữ, mật độ, bo góc → `recreate()` giữ nguyên tab và trạng thái đang chọn;
  - "Theo hệ thống" đổi đúng khi đặt emulator sang `night yes`/`no` (`adb -s emulator-5554 shell cmd uimode night yes|no`, khôi phục sau).

**Điểm dừng:**
1. hồi quy archive sạch;
2. build `4.23-dev.12` (code > 286);
3. QA đủ nhóm + test mới + ảnh;
4. **dừng** và hỏi owner duyệt cài lên điện thoại (sao lưu `run-as` từ dev.11 trước khi cài).

**Quyền:** không gọi provider; không chạy test trên điện thoại; không sửa `Editorial*`; không tag/merge; push sau khi quét.

**Phiếu thử cho owner sau dev.12:**

| # | Owner làm | Đạt khi |
|---|---|---|
| K1 | Tick/bỏ tick nhiều chương, tick trong Library, sửa vài ô Settings | không giật, không đơ |
| K2 | Chạy một tổ hợp (ngắn, chi phí do owner quyết) | các ô không nhảy kích thước; thấy chương i/n, token và chi phí của tổ hợp trên một màn hình |
| K3 | Một chương lỗi trong tổ hợp (nếu gặp) | các chương sau vẫn chạy; "Tiếp tục tổ hợp" chạy nốt phần còn lại |
| K4 | Library > Files | "Resume last checkpoint" không bị cắt chữ |
| K5 | Settings > Appearance > Light / Paper | toàn app sáng, đọc rõ, hộp thoại cũng sáng |
| K6 | Appearance: bật "Theo hệ thống", đổi chế độ tối của điện thoại | app tự đổi sáng/tối |
| K7 | Appearance: đổi màu nhấn, cỡ chữ 130%, mật độ "Gọn", bo góc | xem trước đổi ngay; toàn app áp dụng; không nút nào bị cắt chữ; "Khôi phục mặc định" trả về như cũ |
| K8 | Chạy một tổ hợp có chương dài | không còn lỗi ngoặc do bị cắt; nếu bị cắt thì app tự chia đôi hoặc tăng giới hạn, thông báo đúng "bị cắt" |

Mục nào owner không thử thì ghi NOT_MEASURED.

## 4. Rủi ro

- `recreate()` làm mất trạng thái tạm (URI đang chờ…): phải lưu và khôi phục, kèm test.
- Kiểm ngoặc nới lỏng có thể nhận một bản hỏng có độ lệch trùng hợp với RAW. Hiếm, nhưng có thể xảy ra.
- Coi lỗi kiểm định là lỗi riêng chương sẽ tốn tối đa 3 lượt gọi cho mỗi chương lỗi; trần chi phí lượt và chốt dừng khi 2 chương liên tiếp lỗi giới hạn mức này.
