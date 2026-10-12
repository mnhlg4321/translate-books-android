# Editorial API V1 — thiết kế UX U1 (thư viện nguồn, màn chọn, cài đặt) và yêu cầu làm việc (2026-10-07)

Coordinator: Claude. Owner yêu cầu (chat 2026-10-07): thiết kế chi tiết và bổ sung UI/UX cùng các cài đặt; màn chọn RAW/DRAFT/Glossary/Pronoun theo kiểu danh sách file trong ảnh owner gửi (cuộn dọc, mỗi dòng icon + tên 2 dòng + ngày + nút tròn chọn bên phải, thanh lọc dạng chip, thanh thao tác nổi ở đáy); lưu được RAW (hiện chưa có); các màn khác thân thiện hơn; tiếp tục công việc hiện tại.

Baseline: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `e3b9d7bd` (đã push). APK `4.18-api.6`/code 243 từ `81e4d578`, chỉ cài `emulator-5554`.

## 1. Review N6 (kết quả phiên trước)

| Claim | Evidence coordinator tự kiểm | Kết luận |
|---|---|---|
| 4A: instrumented Editorial API (whole + pair, process-death) PASS; full suite 246, 11 lỗi lịch sử | Báo cáo + log ngoài Git; không chạy lại (không có thay đổi mã mới) | Chấp nhận theo evidence, chưa tự chạy lại |
| 4B: phân xử 24 run N5, mặc định Nhanh | Đối chiếu 24 hash trong `EDITORIAL_API_V1_N5_ADJUDICATION_20261007.md` với file riêng: 24/24 khớp `final.txt` (cột ghi "response" thực chất là hash bản cuối). Hash trùng giữa các fixture khác nhau là do các fixture cùng gốc một chương: sửa đúng lỗi gieo thì bản cuối giống nhau | Đã chứng minh tính toàn vẹn; quyết định Nhanh đúng quy tắc plan §6 |
| 4C: 3 chương, 3 call, USD 0.01967015, lưu/force-stop/mở lại/xuất | Đếm trên file riêng (không in văn bản): số dòng bản cuối = DRAFT (163/104/115), tỷ lệ chữ 0.999–1.000, không rò thẻ/ghi chú, đổi 5/4/4 dòng | STRUCTURAL_VALID: đã chứng minh. App save/reopen/export: theo evidence phiên trước. SEMANTIC_EVAL: **NOT_MEASURED** — chờ owner đọc |

Nhận xét sản phẩm: chế độ Nhanh rất bảo thủ (4–5 dòng đổi mỗi chương). Điều này an toàn với việc giữ phần đúng, nhưng cũng khớp với N5: model bỏ sót lớp câu thiếu và xưng hô. Owner đọc 3 chương là phép đo quyết định.

Để đọc nhanh, coordinator đã tạo trang riêng tư `D:\P5E-private\n6-review\n6-review.html` (ngoài Git): mỗi chương chỉ các dòng đã đổi, cột RAW cùng vị trí, DRAFT và bản cuối với chữ bị bỏ/thêm được tô màu.

## 2. Vấn đề UX hiện tại (đối chiếu mã)

| Vấn đề | Mã | Ảnh hưởng |
|---|---|---|
| RAW/DRAFT không có thư viện: mỗi tổ hợp phải mở trình chọn file hệ thống; file đã chọn không được lưu để dùng lại | `EditorialApiUiController.pickFile` (`:173`), `setFile` (`:206`) chỉ ghi URI vào một combo | Làm 85 chương = 170 lần mở trình chọn file |
| Chọn Glossary/Pronoun/bản dịch gần đây/job là `AlertDialog.setItems` một cột chữ trơn | `chooseGlossary` (`:250`), `pickRecent` (`:184`), `pickJob` (`:212`) | Không cuộn tốt với danh sách dài, không có ngày/kích thước/loại, không tìm/lọc |
| Không có gợi ý ghép theo bộ chương dù tên file của owner theo mẫu `NNN_…` | — | Phải chọn tay 4 lần cho mỗi chương |
| Không có cài đặt Biên tập toàn cục | `SettingsPageFactory` không có mục Biên tập | Mỗi tổ hợp phải đặt lại chế độ/model/trần; không có thư mục/tên file xuất mặc định |
| Màn kết quả: Xem/So sánh/Cần xem nằm dọc, Xuất TXT phải chọn nơi lưu mỗi lần | `EditorialApiPageFactory` (`:273–305`) | Đọc và xuất nhiều chương tốn thao tác |

## 3. Nguyên tắc thiết kế

1. **Nhập một lần, chọn nhiều lần.** File RAW/DRAFT/Glossary/Pronoun vào thư viện nguồn một lần; mọi tổ hợp chọn từ thư viện.
2. **Theo chương, không theo file.** Thư viện nhóm file theo số chương lấy từ tên; chọn một chương là điền đủ bốn ô. Tên chỉ để **gợi ý**, người dùng luôn đổi được (D-N1: không chặn theo tên).
3. **Một kiểu danh sách cho mọi màn chọn** (kiểu ảnh owner): cùng component, cùng cử chỉ.
4. **Mặc định đúng, nâng cao giấu đi.** Chế độ/model/trần lấy từ Cài đặt Biên tập; trong tổ hợp chỉ hiện khi mở "Nâng cao".
5. **Không thuật ngữ kỹ thuật** trong luồng người dùng (giữ quy tắc test chuỗi hiện có).

## 4. Kiến trúc thông tin

```text
Tab Biên tập
 ├─ [Chương]        danh sách tổ hợp (mặc định)
 ├─ [Thư viện]      thư viện nguồn: RAW · DRAFT · Glossary · Pronoun
 └─ ⚙ (góc phải)    Cài đặt Biên tập
Tổ hợp → Xác nhận → Đang chạy → Kết quả (Bản cuối | So sánh | Cần xem)
Màn chọn nguồn (dùng chung) — mở từ Tổ hợp hoặc Thư viện
```

Hai tab con dùng thanh chip ngang đầu trang (cùng kiểu chip của màn chọn).

## 5. Component dùng chung: `SourcePickerSheet`

Màn toàn trang (không phải dialog), dựng bằng widget có sẵn (`a.card`, `a.text`, `a.chip`, `a.primaryButton`…); danh sách dùng `ListView`/adapter tái dùng view để cuộn mượt với vài trăm file.

```text
┌──────────────────────────────────────────┐
│        Chọn RAW              (  ✕  )      │  ← tiêu đề giữa; khi chọn nhiều: "2 đã chọn"
│ [Tất cả] [RAW] [DRAFT] [Glossary] [Pronoun]│  ← chip lọc, cuộn ngang; chip đang chọn nền xám đậm
│ 🔍 Tìm theo tên hoặc số chương   ⇅ Mới nhất │
│ ── Chương 085 ───────────── Chọn cả bộ ── │  ← nhóm theo số chương (bật/tắt trong ⇅)
│ [▤] 085_【書籍版発売記念】EX1       ( ✓ )  │  ← icon vuông bo góc: ▤ xanh dương = TXT
│     ひとりじめの日                          │     tên tối đa 2 dòng, cắt giữa nếu dài hơn
│     RAW · Hôm qua · 18 KB · 163 dòng       │  ← dòng phụ xám: vai trò · ngày · cỡ · số dòng/mục
│ [▤] 085_…_translated                ( ○ )  │
│     DRAFT · Hôm qua · 24 KB                │
│ [▦] 085_GLOSSARY_JAKUAKU_MONSTER_… ( ○ )  │  ← ▦ xanh lá = CSV
│     Glossary · 42 mục                      │
│ ── Chương 084 ─────────────────────────── │
│  …                                         │
│ ╭────────────╮              ╭───────────╮ │
│ │ 🗑   ⬇ Nhập │              │  ✓ Dùng   │ │  ← thanh nổi: trái = thao tác phụ, phải = nút chính
│ ╰────────────╯              ╰───────────╯ │
└──────────────────────────────────────────┘
```

Hành vi:

- **Chế độ chọn một** (mở từ một ô của tổ hợp): chip lọc đặt sẵn đúng vai trò; chạm một dòng là chọn (nút tròn ✓) và nút "Dùng" sáng; chạm "Dùng" hoặc chạm đúp dòng là xong. Mục đang dùng của tổ hợp hiện ✓ khi mở.
- **Chế độ chọn nhiều** (mở từ tab Thư viện, nhấn giữ một dòng): tiêu đề thành "N đã chọn"; thanh nổi đổi thành `Xóa khỏi thư viện` · `Đổi vai trò` · `Tạo tổ hợp`.
- **Chọn cả bộ** ở đầu nhóm chương: điền RAW + DRAFT + Glossary + Pronoun của nhóm vào tổ hợp đang sửa (hoặc tạo tổ hợp mới nếu mở từ Thư viện). Ô nào trong nhóm không có file thì để trống, Glossary/Pronoun trống = "Không dùng" kèm cảnh báo như hiện tại.
- **Nhập** (nút ⬇): mở trình chọn file hệ thống, cho chọn nhiều (`EXTRA_ALLOW_MULTIPLE`), giữ quyền đọc lâu dài, rồi đưa vào thư viện (mục 6). Nhập trùng (cùng SHA-256) không tạo bản sao, chỉ cập nhật ngày dùng.
- Trống: "Chưa có file nào. Bấm ⬇ Nhập để thêm RAW, DRAFT, Glossary, Pronoun." Lọc ra không còn gì: "Không có file khớp bộ lọc."
- File mất quyền đọc hoặc đã bị xóa ở máy: dòng mờ, dòng phụ "Không mở được — nhập lại"; không chọn được.
- Sắp xếp (⇅): Mới nhất · Tên · Số chương; bật/tắt "Nhóm theo chương" (mặc định bật).
- Nguồn "Từ job Dịch" là một chip lọc thêm khi có job: RAW = input của job, DRAFT = output của job (thay `pickRecent`).
- Khả dụng: mỗi dòng có contentDescription "Tên, vai trò, ngày, đã chọn/chưa chọn"; vùng chạm ≥ 48dp; chữ theo cỡ chữ hệ thống; màu theo theme sáng/tối hiện có.

## 6. Thư viện nguồn (lưu RAW/DRAFT)

Bảng mới `editorial_sources` (migration v28, chỉ thêm):

| Cột | Ý nghĩa |
|---|---|
| `id` | khóa |
| `role` | `RAW`, `DRAFT`, `GLOSSARY`, `PRONOUN` |
| `uri`, `display_name` | URI có quyền đọc lâu dài, tên hiển thị |
| `size_bytes`, `line_count`, `item_count` | để hiện dòng phụ (item_count cho CSV) |
| `sha256` | phát hiện nhập trùng và nguồn đã đổi |
| `chapter_key` | số chương suy từ tên (vd `085`), rỗng nếu không suy được |
| `series_hint` | phần tên chung (vd `JAKUAKU_MONSTER`), chỉ để nhóm/tìm |
| `imported_at`, `last_used_at`, `missing` | |

- **Vai trò tự đoán** khi nhập (người dùng đổi được): CSV có header glossary (`source,target,…`) → Glossary; CSV header pronoun (7 cột hoặc 3 cột legacy) → Pronoun; TXT có `_translated`, `DRAFT` trong tên → DRAFT; TXT còn lại → RAW. Đoán sai không chặn gì.
- **Glossary/Pronoun**: nhập vào thư viện nguồn đồng thời nạp vào `GlossaryStore`/`PronounStore` như luồng Dịch đang làm (một nguồn sự thật cho nội dung luật; `editorial_sources` chỉ giữ liên kết + metadata). Hồ sơ đã có sẵn trong `GlossaryStore`/`PronounStore` hiện trong picker như dòng thư viện.
- **Tổ hợp** chuyển sang tham chiếu `source_id` (thêm cột `raw_source_id`, `draft_source_id`; giữ cột URI cũ để tương thích ngược, combo cũ được gắn tự động vào thư viện khi mở lần đầu).
- Snapshot khi chạy giữ nguyên như hiện có (nội dung + SHA-256 trong run). Xóa khỏi thư viện **không** xóa file trên máy, không xóa run/kết quả đã lưu.

## 7. Các màn khác

### 7.1 Danh sách chương (tab Biên tập)

```text
Biên tập                         [Chương] [Thư viện]   ⚙
┌───────────────────────────────────────┐
│ 085  EX1 ひとりじめの日                  │
│ ● Xong · cần xem 2 · Hôm qua · $0.009  │  ← chip trạng thái màu: Chưa chạy (xám), Đang chạy (xanh dương),
│ [Xem kết quả]              [Chạy lại]  │     Xong (xanh lá), Cần xem n (cam), Lỗi (đỏ)
└───────────────────────────────────────┘
          ( + Tổ hợp mới )                  ← nút nổi; nhấn giữ: "Tạo từ thư viện theo chương"
```

- Sắp theo số chương; ô tìm theo tên/số chương; menu ⋮ của thẻ: Sửa · Nhân bản · Xóa.
- "Tạo từ thư viện theo chương": mở picker chọn nhiều nhóm chương → tạo một tổ hợp cho mỗi chương (không chạy tự động, không chạy hàng loạt).

### 7.2 Tổ hợp

Bốn **ô nguồn** dạng thẻ, chạm cả thẻ để mở `SourcePickerSheet` đúng vai trò:

```text
RAW       [▤] 085_【書籍版発売記念】EX1…   163 dòng   ›
DRAFT     [▤] 085_…_translated              163 dòng   ›
Glossary  [▦] 085_GLOSSARY_…                42 mục     ›   (Không dùng ⚠ nếu trống)
Pronoun   [▦] 085_PRONOUN_…                 12 mục     ›
Gợi ý: cùng chương 085 → "Điền cả bộ"                  ← hiện khi vừa chọn RAW và thư viện có bộ cùng số
▸ Nâng cao: Chế độ (Nhanh) · Model (theo Cài đặt) · Trần $0.10
[Lưu]                                  [Tiếp tục →]
```

- Tên tổ hợp tự đặt từ RAW (vd `085 EX1 ひとりじめの日`), sửa được.
- Lệch số dòng RAW/DRAFT lớn hiện cảnh báo nhẹ ngay dưới ô DRAFT (không chặn).

### 7.3 Xác nhận

Bảng gọn: hai tên file + 2 dòng đầu mỗi file, số dòng, Glossary/Pronoun (hoặc cảnh báo), chế độ, ước tính chi phí. Nút `Đổi` · `Chạy`. Giữ đủ nội dung hiện có, chỉ đổi bố cục.

### 7.4 Đang chạy

Thanh tiến độ theo bước (Biên tập → Kiểm → Kiểm lại; Nhanh chỉ một bước), thời gian đã chạy, chi phí tạm; `Hủy`; ghi chú "Có thể rời màn này".

### 7.5 Kết quả

Chip con `[Bản cuối] [So sánh] [Cần xem n]`:

- **So sánh**: mặc định "Chỉ dòng đã đổi", mỗi mục: RAW cùng vị trí (chữ nhỏ, xám), DRAFT với phần bị bỏ gạch đỏ, bản cuối với phần thêm nền xanh (giống trang review N6). Công tắc "Toàn văn".
- **Cần xem**: thẻ từng mục (loại, trích RAW/VI, lý do); nút `Đã xem` để ẩn (lưu trạng thái).
- Thanh đáy: `Xuất TXT` (dùng thư mục xuất mặc định nếu đã đặt → ghi ngay, báo tên file; chưa đặt → hỏi nơi lưu như hiện tại) · `Chạy lại`.
- Đánh dấu chấp nhận: `✓ Chấp nhận chương` (lưu cờ owner_accepted + thời điểm vào run; chỉ là ghi nhận của người dùng, không đổi nội dung).

## 8. Cài đặt Biên tập (⚙)

| Cài đặt | Mặc định | Ghi chú |
|---|---|---|
| Chế độ mặc định | Nhanh | theo kết quả N5 |
| Model mặc định | theo Cài đặt chung | |
| Trần chi phí mỗi chương (USD) | 0.10 | |
| Thư mục xuất mặc định | chưa đặt | chọn bằng SAF tree, giữ quyền |
| Mẫu tên file xuất | `{chuong}_{ten}_bientap.txt` | biến: `{chuong}`, `{ten}`, `{ngay}` |
| Khi file xuất đã tồn tại | Tạo tên mới (thêm `-2`, `-3`) | hoặc Ghi đè (hỏi xác nhận) |
| Gợi ý ghép theo số chương | Bật | |
| Nhóm thư viện theo chương | Bật | |
| Hiện chi tiết kỹ thuật ở kết quả | Tắt | model, token, hash |
| Cảnh báo khi thiếu Glossary/Pronoun | Bật | |

Lưu trong `SettingsStore` (khóa tiền tố `editorial.`), đưa vào export/import cài đặt hiện có. Tổ hợp mới lấy giá trị mặc định tại lúc tạo; tổ hợp cũ giữ giá trị đã lưu.

## 9. Ngoài phạm vi U1 (ghi nhận để sau P7)

Chạy hàng loạt nhiều chương; hàng đợi nền; hướng dẫn biên tập riêng theo series; chunk-pair live (D-CP đóng băng); đồng bộ đám mây; sửa tay bản cuối trong app.

## 10. Yêu cầu làm việc U1 (cho phiên Codex)

Đọc tài liệu này, plan `docs/EDITORIAL_API_V1_PLAN_20261005.md` mục 5 và mã `EditorialApiPageFactory`/`EditorialApiUiController`/`EditorialApiPresenter`. Branch giữ nguyên. Không gọi provider, không đụng pilot, không đổi engine/prompt/ngưỡng, không đụng chunk-pair ngoài việc giữ chạy được.

### Gói và ownership

| # | Gói | File chính (mới/sửa) | Test |
|---|---|---|---|
| U1.1 | Thư viện nguồn | `EditorialSourceLibrary` (mới), `SqliteEditorialApiStore`, `EditorialApiMigrationSpec` (v28), `EditorialApiCombo` (source_id) | migration v27→v28 giữ dữ liệu; nhập trùng SHA; đoán vai trò (TXT/CSV glossary/pronoun 7 cột/3 cột); suy `chapter_key` (`085_…`, `001_RAW_…`, `084_エピローグ`, không có số); file mất quyền → `missing`; combo cũ tự gắn vào thư viện |
| U1.2 | `SourcePickerSheet` | `EditorialSourcePickerPage`, presenter thuần JVM | presenter: lọc theo chip, tìm, sắp xếp, nhóm theo chương, chọn một/chọn nhiều, "Chọn cả bộ" với nhóm thiếu ô |
| U1.3 | Danh sách + Tổ hợp + Xác nhận + Đang chạy | `EditorialApiPageFactory`, `EditorialApiUiController` | presenter: chip trạng thái, gợi ý "Điền cả bộ", tên tự đặt, "Tạo từ thư viện theo chương" không dispatch |
| U1.4 | Kết quả | `EditorialApiPageFactory`, `EditorialApiExport` | diff "chỉ dòng đổi" khớp `EditorialTextDiff`; `Đã xem` lưu bền; xuất theo thư mục + mẫu tên + quy tắc trùng tên; cờ chấp nhận lưu bền |
| U1.5 | Cài đặt Biên tập | `SettingsPageFactory`, `SettingsStore`, `AppSettings` | lưu/đọc/export/import; tổ hợp mới nhận mặc định |
| U1.6 | QA thiết bị | androidTest mới cho picker/thư viện/xuất | xem dưới |

Một writer cho mỗi file; U1.1 → U1.2 → U1.3/U1.4 → U1.5 → U1.6.

### Kiểm thử và build

1. Host: engine + app unit + Python `scripts/p6` từ archive sạch của commit báo cáo; `EditorialApiUserStringsTest` quét thêm mọi file UI mới (không "pack", "binding", "SAFE4", "cấp phép", không tiếng Anh trong chuỗi người dùng).
2. Build qua `scripts/build-and-save.ps1 -Series '4.18-api' -MinimumVersionCode 244 -Offline` + test APK; archive `artifacts/` + `backup/`; cài chỉ `emulator-5554`.
3. Trên emulator, với file mẫu tự tạo (không dùng văn bản sách) đặt tên theo mẫu của owner (`085_…`, `085_…_translated`, `085_GLOSSARY_…csv`, `085_PRONOUN_…csv`, thêm chương 084 thiếu Pronoun và một file không có số chương):
   - nhập nhiều file một lần → thư viện nhóm đúng 2 chương + 1 file lẻ, vai trò đoán đúng;
   - tạo tổ hợp bằng "Chọn cả bộ" chương 085; chương 084 Pronoun trống có cảnh báo;
   - force-stop, mở lại: thư viện và tổ hợp còn nguyên;
   - chạy bằng fake provider, mở Kết quả: So sánh "chỉ dòng đổi", Cần xem, `Đã xem`, xuất vào thư mục mặc định đúng mẫu tên, xuất lần hai tạo `-2`;
   - xóa một file khỏi thư viện: tổ hợp đang dùng nó báo "Không mở được — chọn lại", run cũ vẫn mở được;
   - full instrumented suite: không thêm lỗi ngoài 11 lỗi lịch sử.
4. Ảnh chụp từng màn (sáng và tối) lưu `D:\P5E-private\u1-screens\` (file mẫu, không có văn bản sách), liệt kê trong báo cáo.

### PASS / FAIL

PASS khi mọi test trên PASS, bước 3 đạt đủ, 0 provider call, không thêm lỗi instrumented. Lỗi test/build/emulator là FAILED_REPAIRING trong cùng gói. Dừng và hỏi owner chỉ khi cần gọi provider, đụng pilot, xóa dữ liệu người dùng thật, hoặc đổi thiết kế ở mục 5–8.

### Bằng chứng

`docs/EDITORIAL_API_V1_U1_EXECUTION_<date>.md`: commit từng gói, số test, APK version/code/SHA-256, đường archive, danh sách ảnh chụp, các lệch so với thiết kế và lý do. Cập nhật §10 canonical, snapshot, BUILD_STATE. Push sau mỗi gói, không force-push, chỉ stage file đã review.

## 11. Thứ tự tới P7

1. **Owner đọc 3 chương N6** (song song với U1, không phụ thuộc mã): dùng `D:\P5E-private\n6-review\n6-review.html` và các file `n6-outputs`. Mỗi chương: Chấp nhận / Không chấp nhận + lỗi thấy được (chỉ cần số dòng và loại lỗi).
2. U1 (tài liệu này).
3. Nếu owner không chấp nhận chương nào vì lỗi model (câu thiếu/xưng hô): gói chất lượng Q1 riêng (sửa Quality Core có đo lại trên fixture + chương đó), không gộp vào U1.
4. P7: full regression, wrapper build release, `archive-release.ps1`, device QA theo checklist v4.18, rồi tag — chỉ khi 3/3 chương được owner chấp nhận và U1 PASS.
