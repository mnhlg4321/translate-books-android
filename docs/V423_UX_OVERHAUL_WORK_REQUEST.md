# v4.23 — Yêu cầu làm việc: sửa giao diện và cách làm việc toàn app (U1–U7)

**Trạng thái:** ĐÃ PHẢN BIỆN (2026-10-23). Agent phản biện đối chiếu với mã và tìm ra 8 lỗi; đã sửa tất cả. Đã tính lại tương phản của 5 giao diện mới: accent ≥ 4,76:1 trên nền, muted ≥ 6,29:1 trên card.

**Mốc:**
- `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`; mã sản phẩm như `29a9c76b` (APK dev.22, đang cài trên điện thoại `15e84958`).
- **Phụ thuộc:** làm **sau** `docs/V423_TRANSLATION_RUNTIME_FIX_WORK_REQUEST.md` (F1–F6, dev.23). Hai gói cùng sửa `MainActivity`, nên không chạy song song.

**Quyết định sản phẩm phục vụ:**
- Owner điều khiển lượt dịch mà không phải cuộn trang.
- Tìm lại được mọi công việc cũ.
- Không còn chữ tiếng Anh hay chữ kỹ thuật ở chỗ không cần.
- Thư viện và Cài đặt gọn lại, nhìn vào là biết đang ở đâu và đang lọc gì.

**Nguồn bằng chứng** (ngoài Git):
- Chẩn đoán 8 vấn đề owner nêu (2026-10-23), có file:dòng, đã kiểm lại.
- Rà UX toàn app bằng skill `ui-ux-pro-max`, gồm `references/pro-rules.md`, trên 40 ảnh điện thoại dev.22 ở `D:\P5E-builds\ux-audit-20261023\`.
- Bản sao dữ liệu điện thoại chỉ đọc: `...\v4.23-dev.22-code300\20261023-180815-readonly-diagnosis\`.

**Giới hạn của bằng chứng:** ảnh trang Công việc và danh mục "Translation" trong Cài đặt bị chụp nhầm sang trang Dịch. Hai chỗ này được rà theo mã, và theo ảnh owner gửi cho trang Công việc. Chụp lại khi làm U3 và U5.

## 0. Ranh giới

- **Không sửa** `editorial-engine/` hay mọi tệp `Editorial*` (kể cả `EditorialSettingsPage`, `EditorialJobCard`). Lỗi giao diện bên Biên tập chỉ được ghi vào mục 9.
- Không gọi provider. Không đổi schema. Không đổi prompt, chunking hay logic dịch.
  - Riêng trần chi phí và giới hạn lượt gọi thuộc F6 của gói runtime; gói này chỉ đổi nhãn và chỗ hiển thị.
- **Thuật ngữ:** theo `docs/V423_REFINED_UI_DESIGN.md` mục 10.2.
  - Giữ tiếng Anh: chunk, prompt, token, model, provider, log, cache, Glossary, Pronoun, RAW, DRAFT, FINAL, API key, Base URL, endpoint.
  - Còn lại, gồm nhãn thao tác và chữ thường ngày: tiếng Việt.
- Mọi nút cao ≥ 48 dp. Không dùng emoji làm biểu tượng ở chỗ mới làm.
- Mọi hành động **tốn tiền** phải qua hộp xác nhận.
- Mọi hành động **xóa** phải hỏi lại.

## 1. Quyết định mặc định (owner có thể đổi trước khi giao)

| # | Câu hỏi | Mặc định đề xuất | Lý do |
|---|---|---|---|
| Q1 | Mục "Màu nhấn" | Bỏ | Cả 8 màu đều bị từ chối âm thầm trên giao diện sáng (tương phản 2,3–3,8:1). Mỗi giao diện đã có màu nhấn đạt chuẩn |
| Q2 | "Export profile" trong Cài đặt | Giữ | Đã bỏ API key khỏi tệp xuất (S0); vẫn hữu ích để sao lưu cấu hình |
| Q3 | Tách job Dịch và Biên tập | Hai thẻ "Dịch / Biên tập" ở trang Công việc | Không phải sửa mã Biên tập; vẫn thấy cả hai loại |
| Q4 | Tập đã xóa trong "Gần đây" | Xếp chung theo thời gian với việc mới | Việc mới tự đẩy tập cũ ra |
| Q5 | Trang "Files" và "Sample" trong Thư viện | Chuyển chọn nơi lưu và "Tiếp tục từ checkpoint" sang màn Dịch; "Sao lưu thư viện" vào menu ⋮ của dải thẻ Thư viện; bỏ cả hai trang. **Trang chủ Thư viện thành `RAW`** (chi tiết ở U4.1) | Files không chứa tài liệu thư viện; Sample là trang dev, có hiện một phần API key |
| Q6 | Số danh mục Cài đặt | Gom 9 danh mục thành 5 | Giảm trùng lặp: model, chi phí và reasoning đang nằm ở nhiều nơi |

## 2. U1 — Thanh điều khiển lượt dịch (owner, mục 3)

**Nguyên nhân:**
- `applyRunLayout` (`MainActivity:189`) chỉ đưa `trackingCard` lên đầu trang.
- Khối nút `actionGrid` (`TranslatePageFactory:131–159`) và `pendingJobCard` vẫn nằm dưới các thẻ khác.

**Trùng lặp và rủi ro:**
- Có 3 nút "Xem prompt": `TranslatePageFactory:83,149` và `TranslateSourceFactory:246`.
- Có 3 nghĩa khác nhau của chữ "Tiếp tục".
- Nút "Tiếp tục" vẫn bật trong lúc đang chạy.
- Ba nút sau chạy tốn tiền mà không hỏi lại: "Dịch lại chunk lỗi" (`MainActivity:2759`), "Tiếp tục dịch" (`:2773`), "Retry chunk lỗi" (`:2790`).
- Nút "Dừng" không hỏi lại.

**Làm:**
- Một `RunControlBar` dùng chung cho **cả hai chế độ** (tổ hợp và chương lẻ), do `MainActivity` sở hữu và `updateActionButtonsImpl` vẽ.
- Đặt ngay dưới dòng tiêu đề của `trackingCard`. Ở bố cục ≥ 720 dp thì nằm trên một hàng.
- Mỗi trạng thái hiện **tối đa** một nút chính, một nút phụ và nút ⋮:

| Trạng thái | Nút chính | Nút phụ | ⋮ |
|---|---|---|---|
| Sẵn sàng | Bắt đầu | — | Xem prompt |
| Bị chặn | Bắt đầu (tắt, lý do hiện ở banner) | — | Xem prompt |
| Đang chuẩn bị | — | Dừng (hỏi lại) | — |
| Đang dừng (`stopping`) | — (chỉ hiện "Đang dừng…") | — | — |
| Đã hủy / cả lượt lỗi | như "Sẵn sàng", hoặc như "Có chunk lỗi" nếu còn chunk lỗi | | |
| Chưa rõ đã gửi (DELIVERY_UNKNOWN / NEEDS_REVIEW) | Xem lại | — | — |
| Đang dịch | Tạm dừng | Dừng (hỏi lại) | Xem prompt |
| Tạm dừng | Tiếp tục | Dừng (hỏi lại) | — |
| Có chunk lỗi / job dở | Dịch lại N chunk lỗi, hoặc Tiếp tục (job dở, tổ hợp) | — | Bỏ job này, Xem trong Công việc, Xem prompt |
| Xong | Mở kết quả | Dịch lại | Xem prompt |

- **Gộp:**
  - Tạm dừng và Tiếp tục thành một nút đổi trạng thái theo `trackedState`.
  - "Tiếp tục dịch" và "Tiếp tục tập" thành một nút "Tiếp tục".
  - Bỏ hai nút "Xem prompt" thừa, chỉ giữ một trong ⋮.
  - `pendingJobCard` chỉ còn dòng tên job làm ngữ cảnh.
- **Hỏi lại:**
  - mọi nút gọi provider đi qua cùng hộp xác nhận "Sẵn sàng dịch" (có chi phí ước tính và trần chi phí của F6);
  - "Dừng" hỏi: "Dừng dịch? Chunk đang gửi vẫn bị tính tiền".
- **Giữ:**
  - khóa không cho chạy hai job một lúc (`mayStartNewJob`);
  - hộp xác nhận trùng phí riêng cho trạng thái "Chưa rõ đã gửi"; không thay nó bằng hộp "Sẵn sàng dịch";
  - content description mà test đang dùng. Nút "Tiếp tục" gộp vẫn mang `pending-job-continue` ở chế độ chương lẻ và `pending-job-continue-set` ở chế độ tập (`MainActivity:402,420`).

**Test:**
- hàm thuần trạng thái → nút cho cả 7 trạng thái, ở **cả hai chế độ**, kết quả phải giống nhau;
- mọi nút tốn tiền đều mở hộp xác nhận;
- Dừng hỏi lại;
- thanh nằm trong nửa trên màn hình 360×800 ở cỡ chữ 130%.

## 3. U2 — Màn Dịch khi chưa chạy (rà UX)

| ID | Vấn đề | Sửa | Ưu tiên |
|---|---|---|---|
| U2.1 | Sau khi xong, thẻ tiến trình vẫn to như đang chạy ("Xong · Chunk 7/7", chip "Hoàn tất"), dù owner đã chọn tệp mới | Khi đổi đầu vào hoặc cấu hình sau một lượt đã xong, thu thẻ thành 1 dòng "Lần trước: Xong 7/7 · $0,028 · Chi tiết"; chip về "Sẵn sàng" | P1 |
| U2.2 | Nơi lưu hiện đường dẫn SAF thô và "(persisted grant; verified at Start)"; thư mục còn là của Vol 12 khi dịch Vol 13 | Chỉ hiện tên thư mục cuối, badge "Đã cấp quyền / Cần cấp lại", cảnh báo khi tên thư mục chứa tập khác với RAW. `MainActivity:3030,2427,2436`; `FileUtil:441` | P1 |
| U2.3 | "Mở output" chỉ hiện toast, không mở gì | Mở bằng `ACTION_VIEW`; không mở được thì đổi nhãn thành "Vị trí tệp" và hiện đường dẫn. `TranslatePageFactory:188` | P1 |
| U2.4 | Ước tính toàn tiếng Anh ("7 chunks ≈ 39.0k estimated tokens / Estimated cost"), chữ đậm giữa trang | "7 chunk · ≈ 39,0k token · Chi phí ước tính ≈ $0,030", căn trái, chữ thường. `MainActivity:3585–3732`, `CostEstimator:221` | P2 |
| U2.5 | Thẻ nơi lưu trộn ngôn ngữ ("Single output TXT", "Output ready / Saved and verified"); hai nút không giải thích | Nút chọn hai phần "1 tệp / Thư mục (nhiều chương)"; nhãn "Đã lưu và kiểm tra"; một nguồn chuỗi chung | P2 |
| U2.6 | "Bỏ chọn" màu đỏ như nút xóa, 3 nút bằng nhau trên mỗi hàng Glossary/Pronoun/YAML | "Bỏ chọn" thành nút chữ hoặc ✕; một nhãn "Glossary" (không "Glossaries"). `TranslatePageFactory:255–266` | P2 |
| U2.7 | Ba thẻ cấu hình luôn mở hết cỡ, nút Bắt đầu nằm sau khoảng 2,3 màn | Khi hợp lệ, gộp thành 1 dòng "Glossary 51 · Pronoun 28 · YAML ✓ · Sửa" | P2 |
| U2.8 | Tên YAML xuống dòng giữa đuôi tệp; câu giải thích nội bộ ("Không chọn sẽ hiển thị…") | Cắt giữa tên (`TruncateAt.MIDDLE`); chỉ hiện "Tùy chọn"; câu "không đổi khi đang chạy" chỉ hiện khi đang chạy | P3 |
| U2.9 | Nút chọn nguồn "Tệp TXT / Tập" khó hiểu, rồi lại có nút "Chọn TXT" lặp chữ | "1 chương (TXT) / Nhiều chương (Tập)", lựa chọn đang bật có dấu ✓ | P3 |
| U2.10 | "Chi tiết kỹ thuật" chiếm một thẻ riêng kèm nút "Mở chi tiết" lặp chữ | Một dòng bấm được "Log và chi tiết kỹ thuật ▸" | P3 |
| U2.11 | Số liệu lặp hai lần ("Dự kiến / Đã dùng" nhỏ dưới ô số to); "Chi phí cả lượt" cho cả chương lẻ; dòng "Chương" hiện ở chương lẻ | Bỏ khối lặp; chương lẻ ghi "Chi phí"; dòng "Chương" chỉ hiện ở chế độ tập. `TranslatePageFactory:340–352` | P3 |
| U2.12 | "Glossaries: 4 luật · Pronoun: 2 luật" khó hiểu; đơn vị lúc "mục" lúc "luật" | "Đã áp dụng: 4 mục Glossary · 2 quy tắc Pronoun"; dùng một đơn vị cho mỗi loại tệp. `RunPanelFormatter:90` | P3 |
| U2.13 | Model và ngôn ngữ ghi hai lần (dòng phụ trên cùng và dòng meta dưới nút) | Giữ một chỗ | P3 |

## 4. U3 — Công việc, "Gần đây", lịch sử (owner, mục 6–8)

| ID | Vấn đề và nguyên nhân | Sửa | Ưu tiên |
|---|---|---|---|
| U3.1 | Job Biên tập luôn ở đầu: `JobsMainPageFactory:32` gọi `EditorialJobCard.addJobs` trước mọi phần khác. Hàm này mở SQLite và dựng View trong cùng một vòng lặp (`EditorialJobCard:18–21`), trên luồng giao diện | Hai thẻ "Dịch / Biên tập · N" (Q3), lưu thẻ đang chọn qua Bundle. Thẻ Biên tập: ngoài luồng giao diện thì chỉ **gọi** `SqliteEditorialApiStore.listCombos` và `SqliteEditorialPairRunStore.latestRun/items` (không sửa các tệp này); trên luồng giao diện thì gọi `EditorialJobCard.build(a, combo, run, items)`. Không gọi `addJobs`. Có chấm khi job Biên tập đang chạy | P1 |
| U3.2 | Bộ lọc tập ẩn: chỉ hiện trong chữ nút "Tập: …"; đặt từ ngăn ☰ rồi giữ mãi. `workspaceFilterLabel` (`:1302`) trả "Tất cả" khi chưa nạp xong, trong khi danh sách vẫn đang lọc | Chip "Tập: X ✕" bỏ lọc một chạm; nhãn không bao giờ nói "Tất cả" khi đang lọc | P1 |
| U3.3 | Không xem được việc cũ: chỉ nạp 150 job (`refreshJobCacheAsync`, `recentSummaries(150)`); trên máy 182/332 job, 177 trong đó là chương lẻ, không bao giờ hiện | Trang "Lịch sử": truy vấn theo trang (keyset theo `id DESC`, 50 dòng một lần, chạy ngoài luồng giao diện; không dùng `updated_at` vì giá trị này đổi mỗi khi job được động tới; không thêm index); ô tìm theo tên tệp, và theo tập qua `start_session_id` ↔ `ChapterSetWorkspaceStore` (có debounce); "Tải thêm"; trạng thái trống "Không có kết quả". Gồm cả tập và chương lẻ | P1 |
| U3.4 | "Gần đây" trong ngăn ☰ chỉ có tập (`renderWorkspaceDrawer`, `:1387`); hai job mới nhất trên máy là chương lẻ nhưng không hiện | Hàm thuần `DrawerRecentModel` trộn tập và chương lẻ theo thời gian, tối đa 3 mục, mục đang chạy đứng đầu. Bấm chương lẻ: mở Công việc tại job đó, **không** đặt bộ lọc tập | P2 |
| U3.5 | Giới hạn 30 nhóm (`JobsMainPageFactory:62`) sẽ bị chạm khi job nhiều lên | Thêm "Xem tất cả" dẫn sang trang Lịch sử | P2 |
| U3.6 | Job cũ hơn mốc 150 không được "Current job" xét tới | `JobPicker.current` xét thêm job dở hoặc đang chạy từ một truy vấn riêng, nhẹ | P2 |

## 5. U4 — Thư viện (rà UX)

| ID | Vấn đề | Sửa | Ưu tiên |
|---|---|---|---|
| U4.1 | Dải thẻ con bị cắt ("San"), không có dấu hiệu cuộn được; nhãn trộn ngôn ngữ (Files, Glossaries, Pronouns, Tập, Sample) | Theo Q5 còn 4 thẻ vừa màn hình: "RAW · Glossary · Pronoun · Tập". Nhãn trên thẻ trùng tiêu đề trang. `MainActivity:993–996`. Xem phần **Trang chủ Thư viện** ngay dưới bảng | P1 |
| U4.2 | Trang Files gần như toàn tiếng Anh, một thông tin lặp 3 lần, và là việc của màn Dịch | Thực hiện Q5; phần còn lại Việt hóa. `FilesPageFactory:58–72,117` | P1 |
| U4.3 | Sample là trang dev, hiện một phần API key | Bỏ (Q5) | P1 |
| U4.4 | Thẻ RAW có 4 nút (Xem/Đổi tên/Thay tệp/Xóa), Xóa đỏ nằm sát nút khác; 19 thẻ dài 3 màn | Bấm thẻ để xem; Đổi tên/Thay tệp/Xóa vào menu ⋮. Hàng gọn: tên và một dòng thông tin. `RawPageFactory:101–109` | P2 |
| U4.5 | Nút "Chọn" (chọn nhiều) đứng một mình | Một hàng "19 RAW" và "Chọn" như trang Glossary. `RawPageFactory:58` | P2 |
| U4.6 | `tinyButton` không có chiều cao tối thiểu (11 sp, đệm 4 dp); các nút khác 40–44 dp | Tối thiểu 48 dp. `MainActivity:4181–4190`, `LibraryBulkUi:112,114,169` | P2 |
| U4.7 | Trạng thái "đang dùng" lặp 4 lần (viền, ✓, chip, nút tắt "Đang dùng") | Giữ viền và một chip; bỏ nút tắt. `GlossaryPageFactory:92,130–144`, `PronounPageFactory:47` | P2 |
| U4.8 | Xóa tập không có lựa chọn xóa luôn RAW của tập (owner thường làm vậy) | Hộp xác nhận hiện số chỗ đang dùng (`LibraryDeletePlan`), có ô "Xóa cả RAW chỉ thuộc tập này" | P2 |
| U4.9 | Số đếm mơ hồ ("18 mục"), mỗi trang ghi một kiểu | "18 Glossary", "18 Pronoun", "19 RAW"; một định dạng dòng thông tin chung | P3 |
| U4.10 | Tên tệp dài xuống dòng giữa chữ | Cắt giữa tên, giữ đuôi tệp | P3 |
| U4.11 | Trạng thái trống của Tập chưa dẫn bước tiếp | "Chưa có tập. Nhập RAW trước", kèm lối tắt "Nhập RAW" | P3 |

**Trang chủ Thư viện khi bỏ Files và Sample.** Hiện `"Files"` là trang chủ Thư viện ở nhiều chỗ: `MainActivity:861` (ngăn ☰), `:974` (Back), `NavigationPageTitles:12`. Xuất thư viện (`:1815`) và `refreshFilesPage` (`:3809`) cũng chuyển sang Files. Còn `buildPage` khi gặp trang lạ thì rơi về Sample (`:989`).
- Trang chủ Thư viện thành `RAW`. Mục "Thư viện" trong ngăn ☰ mở `RAW`; điều này thay quy tắc "Thư viện → Files" của gói ngăn ☰ trước đây.
- **Back:** RAW → Dịch; Glossary, Pronoun, Tập → RAW.
- Bundle cũ còn lưu `Files` hoặc `Sample` thì mở `RAW`. `NavState.isValidPage` bỏ hai trang này. `buildPage` gặp trang lạ thì về Dịch.
- "Sao lưu thư viện" (xuất/nhập) vào menu ⋮ của dải thẻ Thư viện. Sau khi xuất thì ở lại trang đang mở, không chuyển sang Files.
- **Sửa test:** `AppBackNavigationPolicyTest`, `DrawerNavigationModelTest`, `NavigationPageTitlesTest`, `LibrarySelectionTest` và hai androidTest có nhắc `Files`/`Sample`.

**Để sau (gói riêng, effort L):** "Nhập cả tập". Một lần chọn nhận cả RAW, Glossary, Pronoun; app tự phân loại theo tên, xem trước rồi dựng tập.

## 6. U5 — Cài đặt (owner, mục 4 + rà UX)

| ID | Vấn đề | Sửa | Ưu tiên |
|---|---|---|---|
| U5.1 | Mục Prompt còn Pronoun profiles (chỉ là lối tắt), `.env` Import (ghi đè lặng lẽ model, chi phí…), Prompt preview (trùng màn Dịch). `SettingsPageFactory:118` | Chỉ giữ Instruction YAML và Export profile (Q2). Xóa nhánh `REQ_ENV` cùng hàng `.env`; `setLabel(envFileLabel…)` không còn được gọi. **Bỏ luôn ghi đè ẩn:** `envUri` cũ đã lưu (`:574`, `SettingsStore:99`) vẫn được gửi cho service (`:2663`), kiểm ở Start (`:2730`), rồi áp vào bằng `AppSettings.fromEnv` (`TranslationEngine:120–125`), mà owner không thấy và không xóa được. Khi nạp settings thì xóa `envUri`/`envName`; bỏ `putExtra("envUri")` và kiểm tra ở `:2730`. Không sửa `TranslationEngine` | P1 |
| U5.2 | Chưa có "Cài đặt khuyến nghị" | Một hàng "Cài đặt khuyến nghị · N mục khác". Mở ra là bảng "máy bạn / khuyến nghị / lý do", **chỉ các mục khác nhau**; nút "Áp dụng" có "Hoàn tác"; không đụng model, key, Base URL, instruction, Glossary, Pronoun, số tiền | P1 |
| U5.3 | 0 nghĩa là "không giới hạn" ở các ô chi phí và số lần gọi lại trả tiền mà không ghi; ô "Dừng khi vượt chi phí" tách khỏi ô số tiền | Theo quy ước của F6 (gói runtime): 0 = dùng trần mặc định an toàn; muốn bỏ trần phải bật công tắc "Không giới hạn" và xác nhận. Ô số tiền khác 0 tự bật dừng. `maxPaidRetries`/`maxRetryCostUsd` chưa nối vào luồng dịch: ẩn, hoặc ghi "chưa có tác dụng". `SettingsPageFactory:122–126` | P1 |
| U5.4 | Trộn kiểu lưu: Performance tự lưu nhưng vẫn có nút "Save performance settings"; Provider phải bấm lưu; rời trang thì mất | Tự lưu kèm dòng "Đã lưu". Với API key và Base URL: giữ nút Lưu, đánh dấu "chưa lưu", hỏi khi rời trang | P1 |
| U5.5 | Performance là một form 20 ô với tên biến môi trường (`MAX_TOKENS_PER_CHUNK`…), không đơn vị, không giải thích | 3 nhóm "Chunk", "Gọi lại và thời gian chờ", "Chi phí"; phần hiếm dùng vào "Nâng cao"; mỗi ô có đơn vị (giây, ký tự) và một dòng giải thích; báo lỗi ngay ở ô khi sai | P2 |
| U5.6 | Chọn reasoning: nhãn trộn ngôn ngữ, không nói "High tốn hơn" | Nhãn tiếng Việt kèm gợi ý chi phí; chọn High phải xác nhận. `SettingsPageFactory:102` | P2 |
| U5.7 | 9 danh mục trùng nhau (model, chi phí, reasoning nằm nhiều chỗ); `CATEGORIES` ở `SettingsPageFactory:15` | Gom còn 5 (Q6), theo bảng ánh xạ ngay dưới. Mỗi lần chỉ mở một danh mục; danh mục vừa mở cuộn lên đầu | P2 |
| U5.8 | Tiêu đề và mô tả tiếng Anh ("Settings", "Tap a section to expand it", "Runtime safeguards", "Save connection", "Refresh catalog"…) | Việt hóa theo quy tắc thuật ngữ. `SettingsPageFactory:15,33–34,81,88,92,95,111,114,118` | P2 |
| U5.9 | Ô provider, model, key, Base URL bị gạch đỏ chính tả; "ENDPOINT" khác thuật ngữ "Base URL"; preset provider nằm dưới khối chẩn đoán | Tắt gợi ý chính tả (`TYPE_TEXT_FLAG_NO_SUGGESTIONS`); đổi nhãn thành "Base URL"; preset đưa lên đầu thẻ kết nối | P3 |
| U5.10 | Ô API key không có trạng thái, không có nút hiện/ẩn | "Đã lưu key ·····abcd" và nút hiện/ẩn; kết quả "Test API" hiện ngay dưới ô | P3 |

**Ánh xạ danh mục cho U5.7:**

| Danh mục mới | id giữ lại | Lấy từ |
|---|---|---|
| Dịch | `General` | General + Translation + Prompt (phần còn lại sau U5.1) |
| Provider & chi phí | `Provider` | Provider + các ô chi phí và gọi lại của Performance |
| Biên tập | `Biên tập` (không đổi) | giữ nguyên, không sửa mã |
| Giao diện | `Appearance` | Appearance (U6) |
| Nâng cao & thông tin | `Advanced` (mới) | phần còn lại của Performance, Logging, About, Model diagnostics |

- Tên danh mục cũ còn lưu trong Bundle (`Translation`, `Prompt`, `Performance`, `Logging`, `About`) được ánh xạ sang id mới khi khôi phục.
- Back vẫn về `General` (`AppBackNavigationPolicy:26`, `MainActivity:976`).

## 7. U6 — Appearance (owner, mục 5)

- **Bố cục mới:**
  1. **Giao diện:** danh sách nhóm "Sáng" và "Tối", kèm công tắc "Theo hệ thống". Bật công tắc thì chọn một giao diện sáng và một tối.
  2. **Cỡ chữ.**
  3. **Tùy chỉnh thêm** (thu gọn): Mật độ, Bo góc.
  4. **Khôi phục mặc định.** Sửa lỗi chính tả hiện tại "Khôi phục mặc ịnh".
  - Thẻ xem trước thu thành một dải nhỏ.
- **Bỏ "Màu nhấn"** (Q1). Giá trị `accent` đã lưu thì bỏ qua khi đọc.
  - Nguyên nhân "không dùng được": `AppearanceSettings.accentError` (`:82`) đòi tương phản ≥ 4,5:1, mà cả 8 màu đều dưới mức đó trên Paper/Light; lỗi chỉ hiện bằng dòng chữ nhỏ cuối nhóm.
- **Thêm 5 giao diện sáng.** Các giá trị đã tính tương phản; chữ trên nền 14,7–17,6:1, chữ trên màu nhấn 5,8–7,7:1:

| Tên | bg | surface | raised | text | text2 | divider | outline | accent | onAccent | ok | warn | err |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Xám nhạt | #F1F2F4 | #FFFFFF | #E7E9ED | #16191F | #4B5463 | #D5D9E0 | #6F7888 | #2457B8 | #FFFFFF | #14703A | #8A5300 | #C0262D |
| Bạc hà | #E9F5EE | #F7FCF9 | #D9EDE2 | #10231A | #3C5747 | #C4DDCF | #5F8A74 | #0B7358 | #FFFFFF | #14703A | #8A5300 | #B8232B |
| Trời nhạt | #EAF2FA | #F7FAFD | #DCE8F4 | #0F1C2B | #3F5268 | #C5D6E8 | #5F7EA0 | #1A5BB0 | #FFFFFF | #14703A | #8A5300 | #BE2A31 |
| Oải hương | #F0EDF8 | #FAF8FE | #E3DEF2 | #1D1830 | #50476B | #D3CBE8 | #776D9C | #5A3DB0 | #FFFFFF | #14703A | #8A5300 | #B8232F |
| Hồng đào | #FBEFEA | #FFFAF8 | #F4E0D8 | #2B1A15 | #5E443B | #EBD0C5 | #9A7062 | #A63A1E | #FFFFFF | #2D6A1E | #7A5000 | #A52222 |

  Cột `outline` đã làm đậm hơn bản đề xuất ban đầu để đạt WCAG 1.4.11. Đã tính lại: 3,5–4,1:1 trên `bg`, 3,2–3,7:1 trên `raised`. Test vẫn phải khẳng định các giá trị ≥ 3.
- **Map vào `UiAppearance.Palette`:**
  - `bg→bg`, `surface→panel`, `raised→card` và `field`, `text→text`, `text2→muted`;
  - `outline→border` (`divider` không dùng, vì `border` chỉ có một trường);
  - `accent→accent`, `accentAlt = accent`, `onAccent→onAccent`;
  - `ok/warn/err→ok/warn/bad`.
  - Thêm vào nhóm sáng.
- **Test:**
  - tương phản của 5 giao diện mới: chữ ≥ 4,5; viền ≥ 3; onAccent ≥ 4,5;
  - accent ≥ 4,5 trên bg/card/field, vì `accentError` coi accent là chữ;
  - muted ≥ 4,5 trên card;
  - "Theo hệ thống" chọn đúng giao diện theo `uiMode`;
  - đọc prefs cũ có `accent` không lỗi;
  - đổi giao diện vẫn giữ trang đang mở.

## 8. U7 — Chung toàn app

| ID | Vấn đề | Sửa | Ưu tiên |
|---|---|---|---|
| U7.1 | Chip trạng thái "Hoàn tất" hiện ở mọi trang, dễ bị hiểu là nút "Lưu" trong Cài đặt; nút ↻ không có tên cho trình đọc màn hình | Chip chỉ hiện ở màn Dịch và khi có lượt đang chạy; ↻ có content description "Làm mới" | P2 |
| U7.2 | Nhiều nút dưới 48 dp | Sửa trong `tinyButton` và các nút 40/42/44 dp đã liệt kê | P2 |
| U7.3 | Emoji và ký tự làm biểu tượng (📄 ✅ 📁 ⚡ ◇ ▣ ✎ …) | Ở chỗ sửa trong gói này: bỏ biểu tượng hoặc dùng vector. Thay toàn app để gói sau | P3 |

## 8b. Sửa sau giai đoạn A (điều phối kiểm dev.24, 2026-10-23)

**Kiểm độc lập:** nguồn `874a030d`, APK SHA `4E58C487…0C76` khớp; archive sạch 708 test, 5 SKIP, 0 FAIL; lint 0 lỗi; điện thoại chạy code 302. Ảnh: `D:\P5E-builds\ux-audit-dev24\` (ngoài Git).

| ID | Vấn đề thấy trên máy | Nguyên nhân | Sửa | Ưu tiên |
|---|---|---|---|---|
| A1 | Màn Dịch, trạng thái "Sẵn sàng": owner đã chọn `005_KUMO_JP_VOL13.txt`, nhưng nút chính lại là "Dịch lại 1 chunk lỗi". Chunk lỗi đó thuộc một job khác (`020_RAW_…`) | Bảng trạng thái U1 (mục 2) không phân biệt tệp **đang chọn** với **job dở** của tệp khác. Lỗi nằm ở thiết kế, không phải ở phần làm | Nếu tệp hay tập đang chọn khác với job dở: nút chính là "Bắt đầu" cho lựa chọn hiện tại. Job dở chỉ hiện một dòng ngữ cảnh "Job dở: 020_… · 1 chunk lỗi", kèm "Dịch lại lỗi" trong ⋮ (vẫn qua hộp xác nhận). Chỉ khi lựa chọn hiện tại **chính là** job dở thì nút chính mới là "Dịch lại N chunk lỗi" / "Tiếp tục". Test thuần cho cả hai trường hợp, ở cả hai chế độ | P1 |
| A2 | Ngăn ☰ › Gần đây có hai mục `005_KUMO_JP_VOL13.txt` liền nhau | `DrawerRecentModel` liệt kê theo job, không gộp nhiều job của cùng một tệp | Gộp chương lẻ theo tệp đầu vào (`input_hash`, dự phòng tên tệp), lấy job mới nhất. Test: 2 job cùng tệp → 1 mục | P2 |
| A3 | Màn Dịch lúc sẵn sàng vẫn vẽ cả bảng số liệu rỗng (thanh Tập/Chương, "—", "chưa đo") và ô "Bản dịch gần nhất · Chưa có chunk" | Thẻ tiến trình luôn hiện đủ, kể cả khi chưa có lượt chạy | Khi chưa có lượt chạy, hoặc lượt trước đã thu gọn theo U2.1: ẩn bảng số liệu và ô xem trước, chỉ giữ tiêu đề trạng thái và thanh điều khiển | P2 |
| A4 | Trang Công việc: nút "Làm mới" là nút chính màu cam, nổi hơn cả việc thật; bộ lọc tập chiếm một thẻ hai nút | Kiểu nút chưa theo thứ bậc | "Làm mới" thành nút phụ hoặc ↻; bộ lọc thành chip "Tập: Tất cả ▾" (và "Tập: X ✕" khi đang lọc) | P3 |

## 9. Bên Biên tập — chỉ ghi lại, không sửa (mã đang khóa)

- Trang Biên tập mở đầu bằng một đoạn giải thích dài và nhiều chữ chuyên môn ("cặp RAW–DRAFT", "lượt viết báo cáo"). Tiêu đề "Biên tập" lặp hai lần.
- "Công cụ dev — SAFE4 legacy" hiện ra với owner.
- Nút "Xóa" không có kiểu nguy hiểm.
- Ô reasoning là ô gõ tự do, nhận cả "xhigh". Ô model gõ tay, không có bộ chọn (`EditorialSettingsPage:14–15`).
- Nút Lưu và nút xem trước sát nhau.

Các điểm này để dành cho lúc Biên tập được mở lại.

## 10. Thứ tự, build, thử trên điện thoại

**Giai đoạn A:** U1 → U2 (các mục P1, P2) → U3.
1. Hồi quy archive sạch, build `4.23-dev.24`.
2. Sao lưu điện thoại, chờ owner "duyệt cài", chỉ `install -r`.
3. Thử trên máy (không bấm nút tốn tiền):
   - thanh điều khiển ở cả hai chế độ, ở các trạng thái thử được: sẵn sàng, xong, job dở có chunk lỗi;
   - Dừng hỏi lại;
   - thẻ "Dịch / Biên tập";
   - chip bộ lọc và nút bỏ lọc;
   - trang Lịch sử thấy job cũ hơn mốc 150 (đếm, đối chiếu với DB);
   - "Gần đây" có chương lẻ.
4. **Dừng**, báo cáo.

**Giai đoạn B:** A1–A4 (mục 8b) trước → U4 → U5 → U6 → U7, cùng các mục P3 còn lại của U2.
1. Build `4.23-dev.25`, quy trình như trên.
2. Thử trên máy:
   - Thư viện còn 4 thẻ con, không bị cắt ở 360 dp và cỡ chữ 130%;
   - "Cài đặt khuyến nghị": Áp dụng rồi Hoàn tác, đối chiếu giá trị trong `settings.xml` (chỉ in giá trị không bí mật);
   - 5 giao diện mới ở cỡ chữ 100% và 130%;
   - không còn "Màu nhấn";
   - Cài đặt còn 5 danh mục.
3. **Dừng**, báo cáo.

**Mọi giai đoạn:**
- Test Java thuần cho mọi mô hình trạng thái mới: thanh điều khiển, `DrawerRecentModel`, truy vấn lịch sử theo trang, nhãn bộ lọc, bảng khuyến nghị, tương phản giao diện.
- Toàn bộ test `app` qua; lint không thêm lỗi; `androidTest` biên dịch được.
- Ảnh PNG hợp lệ (`89 50 4E 47`), chụp bằng `shell screencap` + `pull`, mỗi ảnh một trạng thái; manifest ghi ảnh nào cho mục nào.
- Không đưa văn bản sách, API key hay dữ liệu cá nhân vào Git.

**Báo cáo:** commit; bản, mã và SHA-256; đường dẫn sao lưu; PASS/FAIL/NOT_MEASURED từng mục U; đường dẫn ảnh; các mục chuyển sang gói sau.
