# Biên tập theo chunk — kế hoạch sống (từ 2026-10-09)

Owner (chat 2026-10-09) yêu cầu: tiếp quản việc dở; xác định chính xác nguyên nhân lỗi; loại bỏ ngay các báo cáo/receipt không cần thiết; thiết kế lại Biên tập theo chunk, một lần gọi cho mỗi chunk như luồng Dịch, mỗi chunk gửi cặp RAW–DRAFT khớp nhau và glossary/pronoun ứng viên của chính chunk đó; không test nhiều chương một lúc gây tốn tiền.

Đây là **tài liệu duy nhất** cho hướng chunk. Mỗi lượt làm việc cập nhật tại chỗ (mục 6 trạng thái, mục 7 nhật ký ngắn); không tạo thêm file review/request/execution riêng.

## 1. Nguyên nhân (đo trên 5 chương dev 004–008, so với 28 bản FINAL của owner)

| Cách chạy | Chỗ owner sửa mà app làm đúng/gần đúng | Chi phí dev | Ghi chú |
|---|---:|---:|---|
| luna, 1 lượt toàn chương (E-luna-b) | 6.0% | USD 0.045 | sửa thừa 45 dòng |
| luna, pack v5 3 lượt + báo cáo (V5-luna) | 13.8% | USD 0.168 | 1/5 chương bị chặn ở QA receipt (`CONTENT_UNACCOUNTED_CHANGE`) |
| Sol, 1 lượt toàn chương (E-strong) | 50.3% | USD 0.443 | sửa thừa 165 dòng |

Kết luận đã chứng minh bằng số:

1. **Model nhỏ xử lý cả chương trong một lần thì bỏ sót gần hết lỗi.** Cùng prompt, đổi luna → Sol tăng từ 6% lên 50%. Luật prompt không phải đòn bẩy (Q1: đổi luật trên luna không tăng).
2. **Báo cáo/ledger/receipt của pack v5 làm tăng chi phí ~4 lần và tạo điểm chặn**, chỉ đổi lấy +8 điểm phần trăm. Ba lần chạy v5 gặp ba loại chặn do thủ tục (schema glossary, danh tính file, receipt QA), không lần nào do nội dung.
3. Giả thuyết cần kiểm (chưa chứng minh): **chunk nhỏ như luồng Dịch giúp luna tập trung**, gần mức Sol với chi phí của luna.

## 2. Loại bỏ ngay

| Thứ | Quyết định |
|---|---|
| Báo cáo/ledger/receipt do model viết (REPORT_L1, CHANGE_MAP, QA_RECEIPT, Stop Receipt, NOTES) | Bỏ khỏi luồng sản phẩm. Model chỉ trả bản sửa của chunk (`<EDITED>`), hoặc `<WRONG_PAIR>` khi hai bản rõ ràng khác chương |
| Đường V5 3 lượt | Đóng băng: giữ mã và evidence, không chạy, không có trong UI |
| Chế độ Kỹ (lượt Kiểm + Kiểm lại) | Ẩn khỏi UI; N5 cho thấy không hơn Nhanh mà tốn hơn |
| Receipt/biên bản cho người dùng | Không có. App chỉ giữ bản ghi nội bộ tối thiểu: trạng thái từng chunk, hash nguồn, chi phí |
| Tài liệu quy trình | Chỉ tài liệu này cho hướng chunk; không thêm file review/request/execution riêng mỗi bước |

## 3. Thiết kế

Luồng giống Dịch:

```text
Chọn RAW + DRAFT (+ Glossary, Pronoun)
 → cắt chunk: dòng không rỗng i của RAW ↔ dòng i của DRAFT; gom dòng tới ~N ký tự RAW (theo cài đặt chunk như luồng Dịch)
 → mỗi chunk một lần gọi: Quality Core (không có kênh ghi chú) + glossary/pronoun ứng viên của chính chunk + ngữ cảnh RAW/DRAFT trước–sau (chỉ đọc) + RAW chunk + DRAFT chunk
 → model trả <EDITED>bản sửa của đúng DRAFT chunk</EDITED>
 → app kiểm cấu trúc chunk (mất/gấp chữ, ký hiệu, rò meta), ghép lại theo thứ tự, lưu, xuất TXT
```

Thành phần đã có và tái dùng:

- `EditorialPairRunService`, `PairMaps.fromJobRows`, `PairPromptBuilder` (CP-IMPL-2: không NOTES), `ReferenceProjector` (glossary/pronoun theo phạm vi chunk), `StructuralGate`, `ChunkMerge`, store v27 — đã có test và chạy trên emulator ở CP-IMPL-1.
- Nguồn **job Dịch**: mỗi dòng của job là một cặp chunk (đã có).
- Nguồn **hai file** (mới, `EditorialPairSource.fromAlignedFiles`, commit cùng tài liệu này): khi số dòng không rỗng bằng nhau thì ghép theo dòng và gom theo ngân sách ký tự; các dòng ghép lại đúng nguyên file. Số dòng lệch → `LINE_COUNT_MISMATCH`, không tự ghép.

Giới hạn đã biết: 13/28 chương của owner có DRAFT lệch số dòng so với RAW (±1–4 dòng). Phiên bản đầu chạy chương đó như **một chunk toàn chương** và báo rõ; bước sau mới làm căn dòng theo neo (ký hiệu khung, thoại, dấu ngắt cảnh) — chỉ khi chunk chứng minh có ích.

Ước tính: chương 007 (3 823 ký tự RAW) cắt 900 ký tự → 5 chunk; với luna ≈ USD 0.002/chunk → **≈ USD 0.01/chương**.

## 4. Quy tắc tiết kiệm khi test

1. Mọi thay đổi kiểm offline trước (test host, fake provider, replay response đã lưu). Không gọi API để phát hiện lỗi định dạng/đầu vào.
2. Mỗi giả thuyết **chỉ chạy live 1 chương**. Chỉ khi kết quả chương đó tốt và owner đồng ý mới chạy thêm **1 chương holdout**. Không chạy ma trận nhiều chương/nhiều nhánh một lần.
3. Mỗi lần live có trần tiền cụ thể do owner duyệt; dừng ngay ở lỗi đầu tiên (đã có trong CP-IMPL-2: dừng tại chunk lỗi đầu tiên).
4. Chương test: dev **007** (ngắn, thẳng dòng, đã có số liệu của mọi nhánh: luna 1 lượt 3/18, v5 8/18, Sol 13/18); holdout **011** (thẳng dòng, chưa dùng để chỉnh).

## 5. Việc tiếp theo (C1) — cho phiên Codex

Ownership: APP (`EditorialPairSourceLoader`, `EditorialApiUiController`, `EditorialApiPageFactory`, `EditorialPairRunService` nếu cần), RUNNER (`EditorialApiV1FixtureRunnerInstrumentedTest`, `scripts/p6/run_group.ps1`), không đụng engine ngoài sửa lỗi có test.

1. **Nối nguồn hai file vào luồng chunk:** combo nguồn FILES → `fromAlignedFiles` với ngân sách lấy từ cài đặt chunk của luồng Dịch (chế độ ký tự: `maxCharsPerChunk`; chế độ token: số token, coi 1 ký tự RAW ≈ 1 token; mặc định 900). `LINE_COUNT_MISMATCH` → chạy một chunk toàn chương và hiện "RAW và DRAFT lệch số dòng — biên tập cả chương một lần".
2. **UI:** bỏ nút chọn chế độ Kỹ và đường V5 khỏi luồng người dùng; không còn ô ghi chú/“Cần xem” từ NOTES (chỉ cảnh báo cấu trúc của app). Giữ chuỗi tiếng Việt, test quét chuỗi.
3. **Runner chế độ `CHUNK`:** đọc RAW/DRAFT/Glossary/Pronoun của 1 chương, chạy qua `EditorialPairRunService` thật, ghi `final.txt` + chi phí; dừng ở chunk lỗi đầu tiên.
4. **Offline:** test host đầy đủ (engine, app, `scripts/p6`), replay fake trên 007 thật (DRAFT trả nguyên → `final.txt` == DRAFT từng byte), build qua wrapper (code > 247), test chunk/Editorial tập trung trên `emulator-5554`.
5. **Live (khi có D-C1):** 1 chương **007**, luna reasoning medium. Chấm bằng `score_vs_final.py`; so với 3/18 · 8/18 · 13/18 đã có. Xuất bản app + trang đọc (RAW/DRAFT/app/FINAL) vào `D:\P5E-private\chunk-outputs\`. Dừng, báo cáo trong mục 7 tài liệu này.

PASS offline: test xanh từ archive sạch; replay no-op trả đúng byte; 0 provider call. PASS live: không chunk nào bị chặn cấu trúc, 0 dòng sinh chữ Nhật, và tỷ lệ sửa trúng 007 cao hơn rõ mức luna toàn chương (3/18). Mức "đạt" cuối cùng vẫn là owner đọc.

## 6. Trạng thái

- 0/3 chương được owner chấp nhận; P7 chưa bắt đầu; U1 tạm hoãn.
- CP-IMPL-2 (chunk không ghi chú, dừng ở chunk lỗi đầu tiên) đã commit `90e9a6f7`.
- `fromAlignedFiles` + 5 test: commit cùng tài liệu này.
- Chi tiêu tới nay (ledger Q2): USD 1.17703995 trong trần D-Q2b USD 10.00. D-Q2b/D-Q2c không cấp quyền cho live chunk.

## 7. Nhật ký

- 2026-10-09 — coordinator tiếp quản sau khi Codex hết quota: hoàn tất và commit CP-IMPL-2 (engine 583, app 0 lỗi, `scripts/p6` 93); chốt nguyên nhân bằng số đo mục 1; thêm nguồn hai file thẳng dòng; viết kế hoạch này.
