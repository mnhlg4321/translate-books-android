# v4.23 — Yêu cầu làm việc UX: tạo tổ hợp từ 3 ô RAW / Glossary / Pronoun, bảng chọn tệp rõ ràng, không lẹm tên tệp

Nhánh duy nhất: `feature/v4.23`, worktree `D:\P5E-builds\wt-v4.23`. Ranh giới "không động vào Biên tập" giữ nguyên như `docs/V423_TRANSLATE_LIBRARY_WORK_REQUEST.md` mục 2. Yêu cầu này thay thế cách tạo bộ chương của L3 và chỉnh giao diện chọn của L4; lõi dịch của L4 (ảnh chụp theo chương, lỗi riêng chương, `selectionKey`) giữ nguyên.

**Quyết định sản phẩm phục vụ:** owner tự tạo được tổ hợp RAW–Glossary–Pronoun mà luôn biết mình đã chọn tệp nào, đọc được đủ tên tệp, rồi dùng tổ hợp đó để dịch nhiều chương.

## 1. Owner phản hồi trên điện thoại (2026-10-18, `4.23-dev.2` / code 277)

| # | Owner thấy | Nguyên nhân trong mã (đã đọc tại `dcde3320`) |
|---|---|---|
| P1 | Tên tệp dài bị lẹm, mất chữ (ô "Pronoun: 001_PRONOUN_ATE_VOL5_PARTIAL…" bị cắt nửa dòng) | Tên tệp nằm trong `Button` có chiều cao cố định:<br>• `ChapterSetPageFactory.pickerButton` ~250–264, gắn với `a.dp(42)` ở dòng 233–234;<br>• nút chọn bộ ở màn Dịch `TranslateSourceFactory:195`, `a.dp(46)`.<br>Chữ xuống dòng thứ hai thì bị cắt. Cùng họ lỗi: mọi chỗ đặt tên tệp vào nút cao cố định |
| P2 | Bấm chọn Glossary/Pronoun thì cửa sổ biến mất ngay, không biết đã chọn đúng chưa | `chooseProfile` (~271–287) dùng `AlertDialog.setItems`:<br>• bấm là đóng;<br>• không đánh dấu mục đang chọn;<br>• "★" không giải thích;<br>• không có ô tìm;<br>• không có nút xác nhận.<br>Cùng kiểu: `TranslateSourceFactory.pickSet` ~256, `previewChapter` ~280 |
| P3 | Muốn: "Tạo tổ hợp mới" → bên dưới có 3 ô RAW, Glossary, Pronoun để chọn. Không phải chọn RAW trước rồi sinh tổ hợp | L3 chỉ có một lối vào: tab RAW → Chọn → "Tạo bộ chương" (`ChapterSetPageFactory.newDraft`, bảng sinh từ RAW đã tick) |
| P4 | Bảng chọn tệp nên giống màn "Select items" (ảnh trắng): danh sách có vòng tròn chọn bên phải, tên rõ, thanh hành động dưới | Chưa có thành phần bảng chọn dùng chung. `LibraryBulkUi.selectableCard` có ô tick nhưng chỉ dùng trong danh sách Library |
| P5 | Nói chung: thiếu/mất thông tin, không biết đã chọn cái nào | Hệ quả của P1–P4. Thêm: sau khi chọn không có xác nhận; bảng ghép chỉ ghi "XANH — Đủ", không nhắc lại rõ tệp nào được ghép với tệp nào |

Đã kiểm: dữ liệu lưu (`ChapterSetStore`: chương, RAW, Glossary id, Pronoun id) đủ cho cách tạo mới. **Không cần đổi schema.**

## 2. Thiết kế

### 2.1 Bảng chọn tệp dùng chung `FilePickerSheet` (P2, P4)

Một `Dialog` chiếm gần trọn màn hình (khung bo tròn, theo theme tối của app), dựng bằng code như các trang khác. Dùng cho mọi chỗ chọn RAW / Glossary / Pronoun / tổ hợp trong app.

```
┌────────────────────────────────────────┐
│ Chọn Glossary                       ✕  │
│ Đã chọn 1 · Gợi ý cho Chương 1         │
│ [🔍 Tìm theo tên…                    ] │
│ [Tất cả] [Gợi ý] [Đã chọn]             │
│────────────────────────────────────────│
│ ⊘  Không dùng                       ○  │
│ 📘 001_GLOSSARY_V.csv               ◉  │  ← nền sáng + viền xanh khi được chọn
│    Gợi ý · cùng chương 1 · 8 mục        │
│ 📘 002_GLOSSARY_V.csv               ○  │
│    Chương 2 · 6 mục                     │
│ 📘 KUMO_VOL08_QA_STATE_CANON_CANDI… ○  │  ← tên xuống tối đa 3 dòng, không cắt giữa
│    DATE_ADD_WIP_R256.csv                │
│────────────────────────────────────────│
│  [ Hủy ]                [ Chọn (1) ]   │
└────────────────────────────────────────┘
```

**Quy tắc:**
- **Bấm một dòng không đóng bảng.** Chỉ đổi trạng thái dòng đó:
  - bảng chọn một (Glossary/Pronoun của một chương, chọn tổ hợp ở màn Dịch): chọn kiểu radio, `◉`/`○`;
  - bảng chọn nhiều (ô RAW / Glossary / Pronoun khi tạo tổ hợp): `✓` trong vòng tròn.
- Dòng đang chọn: vòng tròn đặc màu nhấn, nền dòng sáng hơn, viền màu nhấn, `contentDescription` "đã chọn: <tên>". Mở lại bảng thì các mục đã chọn vẫn được đánh dấu.
- Đầu bảng luôn ghi "Đã chọn n" (bảng chọn một: tên mục đang chọn).
- Bộ lọc nhanh Tất cả / Gợi ý / Đã chọn. Ô tìm lọc theo tên, không phân biệt hoa thường và dấu.
- Sắp xếp: gợi ý trước, rồi theo số chương dạng số, rồi theo tên.
- Thay "★" bằng nhãn chữ "Gợi ý · cùng chương N".
- **Tên tệp không bao giờ bị cắt mất:**
  - `TextView` wrap_content, tối đa 3 dòng. Chỉ khi vượt 3 dòng mới thêm "…" ở cuối;
  - nhấn giữ một dòng thì hiện tên đầy đủ (toast hoặc hộp nhỏ);
  - dòng phụ ghi siêu dữ liệu: RAW "Chương N · x ký tự", Glossary "Chương N · y mục", Pronoun "Chương N · z hàng".
- Bảng chọn nhiều có thêm "Chọn tất cả (đang lọc)" và "Bỏ chọn hết".
- Thanh dưới cố định: "Hủy" (bỏ mọi thay đổi trong bảng) và nút chính "Chọn (n)" hoặc "Xong (n)". Nút chính tắt khi bảng chọn một chưa có mục nào.
- Sau khi bấm nút chính: đóng bảng, ô tương ứng cập nhật ngay, toast "Đã chọn: 001_GLOSSARY_V.csv" hoặc "Đã chọn 12 RAW".
- Back / ✕ = Hủy.
- Lớp thuần `FilePickerModel` (danh sách, lọc, sắp xếp, chọn một/nhiều, bỏ dấu khi tìm) để test trên host; `FilePickerSheet` chỉ dựng view.

### 2.2 Ô đã chọn `SelectedFilesSlot` (P1, P5)

Dùng thay mọi nút "Glossary: … ▾" chứa tên tệp. Bố cục:
- dòng 1: nhãn ("RAW", "Glossary", "Pronoun") + chip trạng thái + nút "Chọn"/"Đổi";
- bên dưới: danh sách tên đã chọn, mỗi tên một dòng, wrap_content (không đặt chiều cao cố định), tối đa 3 dòng mỗi tên;
- hơn 5 tệp thì hiện 5 tệp đầu và "và n tệp nữa ▾" để mở rộng.

Chip trạng thái:
- "Chưa chọn" (vàng);
- "Đã chọn 3" (xanh, có ✓);
- "Không dùng" (xám).

Ví dụ:
```
RAW        ✓ Đã chọn 3                   [Đổi]
  📄 001_ATE_JP_VOL5.txt  · Chương 1
  📄 002_ATE_JP_VOL5.txt  · Chương 2
  📄 003_ATE_JP_VOL5.txt  · Chương 3
Glossary   ✓ Đã chọn 3                   [Đổi]
  📘 001_GLOSSARY_V.csv … (3 tên)
Pronoun    ⚠ Chưa chọn                   [Chọn]
```

### 2.3 Tạo tổ hợp mới (P3)

**Đổi tên** trong luồng Dịch/Library: "Bộ chương" → **"Tổ hợp"** (owner dùng từ này).
- Mỗi tổ hợp gồm 1 hoặc nhiều chương; mỗi chương là một bộ RAW–Glossary–Pronoun.
- Tab Library "Tổ hợp".
- Màn Dịch có nguồn "Tệp TXT | Tổ hợp".
- Chữ trong Biên tập giữ nguyên (không động vào).

**Lối vào:**
- Library > Tổ hợp > **"+ Tạo tổ hợp mới"**: lối vào chính, nút đặt đầu trang.
- Màn Dịch, nguồn "Tổ hợp" > bảng chọn tổ hợp > dòng đầu "+ Tạo tổ hợp mới".
- Lối cũ tab RAW → Chọn → "Tạo tổ hợp" vẫn giữ làm đường tắt: mở cùng màn tạo mới với ô RAW điền sẵn các RAW đã chọn.

**Màn "Tổ hợp mới"** (một màn cuộn):
1. Tên tổ hợp. Để trống thì app tự đặt theo phần chung của tên RAW và cập nhật khi chọn RAW; owner sửa được.
2. Ba ô `SelectedFilesSlot`, mỗi ô mở `FilePickerSheet` chọn nhiều:
   - **RAW** (bắt buộc);
   - **Glossary** (không bắt buộc);
   - **Pronoun** (không bắt buộc).
3. Mục **"Ghép theo chương"**, tự cập nhật mỗi khi một ô thay đổi:
   - Mỗi RAW đã chọn là một dòng. Dòng ghi **đủ ba tên tệp** (không chỉ "XANH — Đủ"):
     ```
     Chương 1   ✓ Đủ
       📄 RAW      001_ATE_JP_VOL5.txt
       📘 Glossary 001_GLOSSARY_V.csv          [Đổi]
       ↔ Pronoun  001_PRONOUN_ATE_VOL5_PARTIAL.csv  [Đổi]
     ```
   - Quy tắc ghép:
     - nếu ô Glossary/Pronoun có đúng **một** tệp và ô RAW có đúng **một** tệp: ghép thẳng, không cần số chương;
     - còn lại: ghép theo số chương đầu tên (`ChapterNumber`, `ChapterSetMatcher` hiện có), **chỉ trong các tệp owner đã chọn ở ô đó**.
   - Trạng thái dòng giữ nghĩa cũ, nhưng ghi bằng chữ kèm biểu tượng:
     - ✓ "Đủ";
     - ⚠ "Thiếu Pronoun";
     - ✕ "Hai Glossary cùng chương 3 — chọn một".
   - "Đổi" trên một dòng mở bảng chọn một. Các tệp đã chọn ở ô tương ứng xếp đầu; công tắc "Hiện cả thư viện" để chọn tệp ngoài ô; có dòng "Không dùng".
   - Tệp Glossary/Pronoun đã chọn nhưng không ghép được vào chương nào: hiện cảnh báo "2 Glossary không khớp chương nào: 004_GLOSSARY_V.csv, …", kèm nút "Bỏ khỏi ô".
   - Sửa số chương của một RAW: giữ ô số như hiện nay, chiều cao wrap_content.
4. Thanh dưới cố định: "Hủy" và **"Lưu tổ hợp (n chương)"**.
   - Lưu bị chặn khi còn dòng ✕. Khi đó cuộn tới dòng lỗi đầu tiên và tô nhấp nháy, thay cho hộp thoại liệt kê.
   - Lưu xong: toast "Đã lưu tổ hợp "ATE_JP_VOL5" (3 chương)", quay về danh sách và cuộn tới tổ hợp vừa lưu.

**Sửa tổ hợp đã lưu:** mở cùng màn này.
- Ba ô điền sẵn các tệp đang dùng trong tổ hợp.
- Bảng ghép giữ đúng lựa chọn owner đã lưu: không tự ghép lại; chỉ gợi ý cho dòng mới hoặc khi owner đổi ô.

**Danh sách tổ hợp:** mỗi thẻ ghi
- tên;
- số chương và trạng thái tổng ("3 chương · ✓ 3 đủ" / "⚠ 1 thiếu Pronoun");
- dòng tóm tắt chương "Ch.1–3".

Bấm thẻ là mở. Chọn nhiều để xóa dùng lại L2.

### 2.4 Màn Dịch, nguồn "Tổ hợp" (P1, P2, P5)

- "Chọn tổ hợp" dùng `FilePickerSheet` chọn một. Dòng phụ ghi số chương và trạng thái.
- Sau khi chọn: một `SelectedFilesSlot` ghi tên tổ hợp, nút "Đổi" và "Sửa tổ hợp".
- Danh sách chương có ô tick:
  - mỗi dòng ghi đủ RAW / Glossary / Pronoun (wrap, không cắt), "✓ đã dịch xong" và ước tính như hiện nay;
  - đầu danh sách có "Chọn tất cả / Bỏ chọn / Chỉ chương chưa dịch";
  - dòng đang tick có nền và viền màu nhấn như bảng chọn.
- "Prompt preview (chọn chương)" dùng `FilePickerSheet` chọn một thay cho `setItems`.

### 2.5 Rà cả họ lỗi (bắt buộc trong phạm vi Library + Dịch)

- Tìm mọi `Button` / `secondaryButton` / `tinyButton` mang tên tệp hoặc tên profile và có chiều cao cố định `a.dp(4x)` trong `ChapterSetPageFactory`, `TranslateSourceFactory`, `RawPageFactory`, `LibraryBulkUi`, `GlossaryPageFactory`, `PronounPageFactory`, `TranslatePageFactory`. Đổi sang `SelectedFilesSlot` hoặc `TextView` wrap_content.
- Tìm mọi `AlertDialog.setItems` dùng để chọn tệp/profile/tổ hợp trong các tệp đó. Đổi sang `FilePickerSheet`.
- Hộp xác nhận chỉ có chữ (xóa, hoàn tác, xác nhận lần hai) giữ `AlertDialog`.
- Kiểm cỡ chữ hệ thống 130%: không mục nào bị cắt.
- Không sửa `Editorial*`.

## 3. Test

**Host:**
- `FilePickerModel`:
  - lọc và tìm, bỏ dấu tiếng Việt và chữ Nhật giữ nguyên;
  - sắp xếp gợi ý → số chương → tên;
  - chọn một/nhiều;
  - Hủy trả lại lựa chọn cũ;
  - "Chọn tất cả (đang lọc)".
- Ghép từ ba ô (`ChapterSetMatcher` mở rộng hoặc lớp mới `ComboDraft`):
  - 1 RAW + 1 Glossary + 1 Pronoun không có số chương → một dòng ✓;
  - N–N–N cùng số → N dòng ✓;
  - thiếu Pronoun → ⚠;
  - hai Glossary cùng số → ✕;
  - Glossary thừa → cảnh báo "không khớp";
  - chỉ ghép trong tệp đã chọn ở ô;
  - sửa tổ hợp đã lưu không ghép lại.

**Thiết bị** (`emulator-5554`; dữ liệu tổng hợp có tên tệp dài ≥ 90 ký tự, như `KUMO_VOL08_QA_STATE_CANON_CANDIDATE_ADD_WIP_R256_EXTRA_LONG_NAME_FOR_WRAP_TEST.csv`):
- Mở bảng chọn, bấm một dòng: bảng **vẫn mở**, dòng đó có trạng thái đã chọn; bấm "Chọn (1)" thì ô cập nhật đúng tên.
- Bấm Hủy: ô không đổi.
- Không cắt chữ: với mọi `TextView` hiển thị tên tệp trong ô và trong bảng, `getLayout().getEllipsisCount(i) == 0` với tên ≤ 3 dòng, và chiều cao view ≥ chiều cao layout. Chạy thêm ở `font_scale 1.3` (`adb -s emulator-5554 shell settings put system font_scale 1.3`, khôi phục sau).
- Tạo tổ hợp mới từ ba ô (3 RAW, 3 Glossary, 2 Pronoun) → ghép đúng, dòng 3 ⚠ thiếu Pronoun → Lưu → mở màn Dịch → chọn tổ hợp → dịch 1 chương bằng provider giả → prompt có đúng Glossary của chương.
- Đường tắt từ tab RAW mở đúng màn với ô RAW điền sẵn.
- Hồi quy các test L1–L4 hiện có. Cập nhật test dùng nhãn cũ "Bộ chương" bằng mã test, ghi FAILED_REPAIRING nếu có.

**Ảnh** (xem từng ảnh, lưu trong `artifacts/qa/v4.23-dev3-<date>/media/`):
1. danh sách Tổ hợp;
2. màn Tổ hợp mới khi chưa chọn;
3. bảng chọn RAW nhiều mục có tên dài;
4. bảng chọn Glossary một mục đang chọn;
5. ô đã chọn sau khi bấm "Chọn";
6. bảng ghép có ✓ / ⚠ / ✕;
7. màn Dịch nguồn Tổ hợp;
8. cùng màn ở font 130%.

## 4. Thứ tự, điểm dừng, quyền

1. U1 `FilePickerModel` + `FilePickerSheet` + `SelectedFilesSlot` (+ test host).
2. U2 màn Tổ hợp mới / sửa, đường tắt từ RAW, đổi tên "Tổ hợp".
3. U3 màn Dịch nguồn Tổ hợp và rà cả họ lỗi (2.5).
4. Hồi quy archive sạch (engine, app 3 biến thể, lint, androidTest compile).
5. Build `4.23-dev.3` (`-Series 4.23-dev -MinimumVersionCode 278`).
6. QA emulator đủ nhóm như `artifacts/qa/v4.23-dev2-20261018` + test mới + 8 ảnh.
7. **Dừng.** Gửi owner APK, SHA-256, ảnh và một câu hỏi duyệt cài lên điện thoại. Mỗi lần cài là một lần duyệt mới; sao lưu `run-as` từ `4.23-dev.2` (debuggable) trước khi cài.

**Quyền:**
- Không gọi provider (USD 0). Không chạy test trên điện thoại.
- Không tag/merge.
- Push `feature/v4.23` sau khi quét bí mật / chữ sách.
- Không sửa `editorial-engine/` hay `Editorial*`. Kiểm `git diff dcde3320 -- editorial-engine 'app/src/main/java/com/ml/tblandroidtxt/Editorial*'` rỗng.

## 5. Phiếu thử cho owner sau dev.3 (thay D3 của phiếu cũ, D1–D2 và D4–D7 giữ)

| # | Owner làm | Đạt khi |
|---|---|---|
| E1 | Library > Tổ hợp > + Tạo tổ hợp mới > RAW: chọn 3 chương | bảng không đóng khi bấm; thấy rõ 3 dòng đã chọn; ô RAW ghi đủ 3 tên |
| E2 | Glossary: chọn 3; Pronoun: chọn 3 | bảng ghép ghi đủ tên ba tệp mỗi chương, không lẹm chữ |
| E3 | Đổi Glossary của chương 2 sang "Không dùng" rồi đổi lại | biết rõ đang chọn gì trước khi bấm "Chọn" |
| E4 | Lưu, mở màn Dịch, nguồn Tổ hợp, chọn tổ hợp vừa lưu | danh sách chương ghi đủ RAW/Glossary/Pronoun |
| E5 | Tạo tổ hợp với tên tệp rất dài | không có chữ nào bị cắt mất |

Mục nào owner không thử thì ghi NOT_MEASURED.
