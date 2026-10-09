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

### 3.1 Căn dòng RAW ↔ DRAFT trước khi cắt chunk (đo 2026-10-09)

Đo trên 28 chương của owner (chỉ đếm, không đưa văn bản vào Git):

- 13/28 chương lệch số dòng ±1–4. Lệch luôn **cục bộ**, 1–4 điểm mỗi chương: DRAFT tách một dòng RAW thành 2 (thường "câu thoại + câu kể" hoặc câu dài), gộp 2 dòng RAW bị ngắt giữa câu, hoặc thiếu/thêm một dòng.
- **Số dòng bằng nhau không bảo đảm khớp 1–1:** chương 016 và 027 có một chỗ tách và một chỗ gộp bù trừ nhau (016: RAW 112 bị DRAFT tách, RAW 189–190 bị DRAFT gộp) nên RAW và DRAFT lệch một dòng suốt 77 dòng (016) và 228 dòng (027). Ghép 1–1 thuần (`fromAlignedFiles`, `1f360c19`) sẽ ghép sai ở hai chương này.

Phương án (prototype: `scripts/chunk/align_lines_reference.py`):

1. **Căn dòng tự động cho mọi chương**, kiểu Gale–Church: quy hoạch động với các bước 1–1, 1–2, 2–1 (và 1–0, 0–1 phạt nặng). Chi phí mỗi cặp gồm:
   - tỷ lệ độ dài so với tỷ lệ VI/JP của cả chương;
   - ký hiệu đầu/cuối dòng (「」『』【】〝〟◇◆…);
   - **neo glossary**: tên/thuật ngữ nguồn ở dòng RAW phải có dạng đích ở dòng DRAFT; cùng với chữ số.
2. **Chỉ cắt chunk ở ranh giới an toàn:** giữa hai cặp 1–1 liền nhau, khi đã đủ ngân sách ký tự. Nhóm tách/gộp luôn nằm trọn trong một chunk. Trong chunk, model được phép tách/gộp dòng theo RAW, vì app ghép theo chunk chứ không theo dòng.
3. **Ngữ cảnh chồng lấn** (đã có): mỗi chunk kèm RAW/DRAFT trước–sau chỉ để đọc, nên nếu cắt lệch một dòng thì model vẫn thấy câu nguồn.
4. **Kiểm từng chunk trước khi gửi**, không tốn API: tỷ lệ độ dài, số câu thoại 「」, neo glossary/số phải khớp. Chunk nghi ngờ → gộp với chunk kề; vẫn nghi → báo người dùng và chạy chương đó như một chunk toàn chương.

Kết quả prototype:

- 8 chương lệch số dòng có đáp án dựng từ FINAL: **42/42 điểm cắt đúng**, mọi điểm lệch tìm đúng chỗ.
- 016/027: tìm ra đúng cặp tách/gộp bù trừ (đã đọc tay xác nhận).
- 4 chương lệch không có đáp án (010, 015, 019, 023): điểm lệch tìm được hợp lý khi đọc mẫu (vd 023 dòng 142–145: DRAFT tách "thoại + kể").
- Mức khớp neo glossary trên các cặp 1–1 có neo: 79–100% theo chương.
- Giới hạn: đáp án dựng từ FINAL chỉ đúng ở chương mà FINAL theo cấu trúc RAW; với 016/027, FINAL theo cấu trúc DRAFT nên phải kiểm tay.


Ước tính: chương 007 (3 823 ký tự RAW) cắt 900 ký tự → 5 chunk; với luna ≈ USD 0.002/chunk → **≈ USD 0.01/chương**.

### 3.2 Bộ WN (85 chương, `D:\Ebooks\JAKUAKU MONSTER WN`) — các trường hợp mới (đo 2026-10-09)

| Trường hợp | Đo được | Cách xử lý (offline, trong app) |
|---|---|---|
| Ghép file theo số dễ sai | RAW `0043-042　獅子身中の怪異.txt`: 4 chữ số = thứ tự file, 3 chữ số = số chương trong truyện; DRAFT/FINAL/glossary/pronoun đánh số theo **thứ tự file** (`043_獅子身中の怪異_translated.txt`). Ghép theo số chương lệch 1–2 từ chương 038 (do 2 file 設定資料集 xen giữa) | Thư viện nguồn ghép theo **tiêu đề** trong tên file trước, rồi tới thứ tự file; không dùng số chương trong truyện. Người dùng xác nhận cặp (D-N1) |
| Tên file không theo mẫu | 設定資料集【第一章/第二章】, `EX1`, hậu tố `_v2`, `(1)`, thiếu `VOL1` | Như trên; file lẻ cho người dùng tự ghép |
| Encoding | RAW 85/85 là UTF-16 LE có BOM, xuống dòng lẫn CRLF/LF | `FileUtil.readText` đã đọc được (UTF-16 BOM, UTF-8, Windows-31J). Chỉ đường V5 (đã đóng băng) đòi UTF-8 chặt |
| Khối đầu chương và lời tác giả | Đầu file có tiêu đề, dòng "Floor…", `=====`; 80/85 RAW kết thúc bằng lời tác giả (xin bookmark/đánh giá), DRAFT dịch cả phần này | Giữ nguyên như nội dung thường; căn dòng xử lý được |
| Số dòng bằng nhau | 55/85 | Vẫn chạy căn dòng (bài học 016/027) |
| Chuỗi gộp dòng liên tiếp | 043, 069: DRAFT gộp hàng loạt cặp dòng RAW ngắn, kèm dòng bị bỏ | Quy tắc cắt có vùng đệm (dưới) |
| DRAFT thiếu câu | 9/85 chương có dòng RAW không có trong DRAFT (bước 1–0) | Dòng thiếu luôn nằm trọn trong một chunk, model dịch bù; app gắn cờ chunk đó |
| DRAFT không áp glossary | 034, 071: nhiều dòng RAW có thuật ngữ mà DRAFT dùng dạng khác (vd `renjou` vắng 9 lần) | Tín hiệu neo glossary vừa dùng để căn dòng, vừa thành danh sách "điểm cần sửa" của chunk |
| Kana sót trong DRAFT | 79/85 chương | Phát hiện tất định (đã có), đưa vào chunk tương ứng |
| Chương rất dài | 059 (設定資料集) 1 222 dòng ngắn dạng bảng/danh sách | Gom theo ký tự: 34 chunk; DP 1 222×1 213 vẫn nhẹ trên điện thoại |

**Quy tắc cắt chunk có vùng đệm:** chỉ cắt khi đủ ngân sách ký tự **và** có ít nhất 2 cặp 1–1 liền nhau ở mỗi bên điểm cắt. Kết quả trên 18 chương có đáp án dựng từ FINAL (8 LN + 10 WN): **133/133 điểm cắt đúng**. Không có vùng đệm: 135/136, sai 1 ở WN 043 giữa chuỗi gộp. Thực thi tham chiếu: `cut_points()` trong `scripts/chunk/align_lines_reference.py`.

**Trường hợp vẫn chưa tự xử lý được (phải báo người dùng, không đoán):**
- DRAFT dịch từ bản RAW khác (LN và WN khác nhau cả câu chữ) → mức khớp neo glossary và tỷ lệ độ dài sẽ rớt trên diện rộng. App dừng trước provider với "RAW và DRAFT có vẻ không cùng bản"; không tự ghép.
- DRAFT đảo thứ tự cả đoạn: quy hoạch động chỉ xử lý được lệch cục bộ. Mức khớp neo rớt theo vùng → gộp vùng đó thành một chunk lớn; quá lớn thì báo.

### 3.3 Trạng thái thực thi

Toàn bộ mục 3.1–3.2 là tính toán tất định, không gọi API, chạy được trên điện thoại. Tuy nhiên **trong app hiện mới có**: luồng chunk theo dòng của job Dịch (CP-IMPL-2) và ghép 1–1 thuần `fromAlignedFiles`. Bộ căn dòng, quy tắc cắt có vùng đệm, ghép file theo tiêu đề/thứ tự **mới ở dạng prototype Python** trên máy tính (`scripts/chunk/align_lines_reference.py`). Đưa vào app là việc C1.1.

### 3.4 Thiết kế hoàn chỉnh khâu tách chunk (CS-1)

Mục tiêu: từ một cặp RAW + DRAFT (+ Glossary, Pronoun), app tự tạo danh sách chunk khớp nhau, **hoàn toàn offline** (không gọi API, chạy trên điện thoại), và **dừng trước khi tốn tiền** nếu RAW và DRAFT không cùng chương hoặc không cùng bản. Mục 3.1–3.2 là số đo làm căn cứ. Thực thi tham chiếu: `scripts/chunk/align_lines_reference.py` (`decode`, `align`, `cut_points`, `verdict`).

#### Luồng

```text
S0 Đọc file ─ S1 Ghép cặp file ─ S2 Tách dòng ─ S3 Căn dòng ─ S4 Phán định chương ─┬─ BLOCK → dừng, báo người dùng
                                                                                   ├─ WARN  → hỏi người dùng
                                                                                   └─ OK
   → S5 Cắt chunk ─ S6 Kiểm từng chunk ─ S7 Đóng gói chunk ─ S8 Lưu kế hoạch chunk ─ (gọi API từng chunk) ─ S9 Ghép lại
```

#### S0 — Đọc file

- Dùng `FileUtil.readText`: BOM UTF-8, UTF-16 LE/BE; không BOM thì UTF-8 chặt, rồi Windows-31J.
- Đổi CRLF/CR → LF để xử lý, nhưng giữ bản gốc để xuất và tính hash.
- Không đọc được → "Không đọc được file <tên>. Hãy lưu lại dưới dạng TXT (UTF-8 hoặc UTF-16)."

#### S1 — Ghép cặp file (thư viện nguồn)

- Rút **tiêu đề** từ tên file: bỏ tiền tố số (`0043-042　`, `043_`), hậu tố (`_translated`, `_v2`, `(1)`, `_FINAL_QA_…`, `_JAKUAKU_MONSTER_VOL1`), phần mở rộng; chuẩn hóa NFKC và khoảng trắng.
- Ghép RAW ↔ DRAFT theo tiêu đề trùng. Không có tiêu đề thì theo **thứ tự file**, không theo số chương trong truyện (bộ WN: `0043-042` là file thứ 43, chương 42).
- Glossary/Pronoun theo cùng số thứ tự file với DRAFT.
- Luôn hiện cặp đã ghép trên màn xác nhận để người dùng đổi; tên file chỉ để gợi ý (D-N1).

#### S2 — Tách dòng

- Đơn vị là **dòng không rỗng**. Mỗi đơn vị giữ nguyên văn gồm cả các dòng trống theo sau, để ghép lại đúng từng byte.
- Dòng đầu chương (tiêu đề, "Floor…", `=====`) và lời tác giả cuối chương là đơn vị bình thường.

#### S3 — Căn dòng

- Quy hoạch động (kiểu Gale–Church) với các bước 1–1, 1–2, 2–1 (phạt 3), 1–0, 0–1 (phạt 6).
- Chi phí mỗi cặp:
  - lệch tỷ lệ độ dài so với tỷ lệ VI/JP của chính chương: |log|;
  - ký hiệu đầu dòng khác nhau (`「『【（〝《◇◆＊─`): +1.5; ký hiệu cuối dòng khác nhau: +1.0;
  - **neo**: thuật ngữ glossary (nguồn ở RAW ↔ đích ở DRAFT) và chữ số, quy toàn khổ về nửa khổ. Khác nhau làm tăng chi phí, trùng nhau làm giảm.
- Chạy cho **mọi** chương, kể cả chương có số dòng bằng nhau (LN 016/027 lệch bù trừ).
- Hiệu năng: giới hạn dải chéo rộng |R−D| + 50; chương 1 222 dòng vẫn chạy dưới 1 giây trên điện thoại.

#### S4 — Phán định chương (trước khi gọi API)

| Tín hiệu | Cách tính | OK | WARN | BLOCK |
|---|---|---|---|---|
| Khớp tên (chỉ khi có glossary và ≥ 15 dòng có thuật ngữ) | tỷ lệ cặp 1–1 có thuật ngữ glossary trùng | ≥ 0.6 | 0.35–0.6 | < 0.35 |
| Ký hiệu đầu/cuối dòng | tỷ lệ cặp 1–1 có cùng ký hiệu đầu/cuối | ≥ 0.97 | 0.95–0.97 | < 0.95 |
| Dòng không ghép được | tỷ lệ bước 1–0, 0–1 | ≤ 1.5% | 1.5–5% | > 5% |
| Tỷ lệ độ dài | DRAFT/RAW so với tỷ lệ chuẩn của bộ truyện (mặc định 2.54 cho Nhật→Việt; app cập nhật từ các cặp người dùng đã chấp nhận) | lệch ≤ 20% | 20–35% | > 35% |

Đo trên 113 cặp đúng, 55 cặp lệch chương, 11 cặp khác bản (LN RAW với WN DRAFT):

| Nhóm | Có glossary | Không glossary |
|---|---|---|
| Lệch chương | **55/55 BLOCK** | **55/55 BLOCK** |
| Cặp đúng | 108 OK, 5 WARN, **0 BLOCK** | 110 OK, 3 WARN, **0 BLOCK** |
| Khác bản | 8 BLOCK, 3 WARN | 8 BLOCK, 2 WARN, 1 OK (nội dung gần trùng) |

Các WARN trên cặp đúng đều có lý do thật: chương rất ngắn (LN 028), chương bảng/danh sách 設定資料集 (WN 038, 059), DRAFT dùng sai glossary (WN 034, 069).

**Thông báo cho người dùng:** chỉ dùng tiếng Việt thường, kèm 2 dòng đầu của mỗi file.

- **BLOCK — không gọi API:**
  - "RAW và DRAFT có vẻ không cùng chương." Kèm một hoặc hai lý do theo dữ liệu, ví dụ: "chỉ 8% tên riêng trong RAW có mặt ở dòng DRAFT tương ứng"; "DRAFT dài gấp 4 lần mức thường"; "212 dòng không ghép được".
  - Nút: **Chọn lại DRAFT** · **Chọn lại RAW** · **Hủy**. Không có nút "vẫn chạy".
- **WARN — chờ người dùng:**
  - "RAW và DRAFT khớp chưa chắc chắn" kèm lý do, ví dụ: "34% tên riêng không khớp — có thể DRAFT chưa theo glossary"; "7 câu RAW không có trong DRAFT".
  - Nút: **Vẫn biên tập** · **Chọn lại** · **Hủy**.
- **OK:** dòng tóm tắt trên màn xác nhận, ví dụ "Chia 12 đoạn. DRAFT tách/gộp dòng ở 2 chỗ; 1 câu RAW chưa có trong DRAFT."

#### S5 — Cắt chunk

- Ngân sách: lấy cài đặt chunk của luồng Dịch (chế độ ký tự: `maxCharsPerChunk`; chế độ token: số token tính như ký tự RAW), mặc định 900 ký tự RAW.
- Chỉ cắt khi đã đủ ngân sách **và** có ít nhất 2 cặp 1–1 liền nhau ở mỗi bên điểm cắt. Kết quả: **133/133 điểm cắt đúng** trên 18 chương có đáp án; không có vùng đệm thì sai 1.
- Nhóm tách/gộp (1–2, 2–1) và dòng thiếu (1–0) luôn nằm trọn trong một chunk.
- Một dòng RAW dài hơn ngân sách là một chunk riêng.
- Không tìm được điểm cắt an toàn trong 3× ngân sách → cắt ở cặp 1–1 gần nhất với vùng đệm 1 và gắn cờ chunk.

#### S6 — Kiểm từng chunk (offline)

- Tỷ lệ độ dài chunk lệch không quá 35% so với tỷ lệ của chương.
- Số dòng thoại (bắt đầu bằng `「`/`『`) chênh không quá 1.
- Khớp tên trong chunk (nếu chunk có ≥ 5 dòng có thuật ngữ) ≥ 0.35.
- Không đạt → gộp với chunk kề rồi kiểm lại. Vẫn không đạt → đánh dấu "đoạn chưa chắc" và hiện cho người dùng. Nếu > 20% số chunk không đạt → nâng phán định chương thành WARN.

#### S7 — Đóng gói từng chunk (đầu vào của lần gọi API)

- **RAW chunk và DRAFT chunk** (phần được sửa).
- **Ngữ cảnh chỉ đọc:** 2 dòng RAW và DRAFT trước và sau chunk.
- **Glossary ứng viên:** các mục có nguồn xuất hiện trong RAW chunk.
- **Pronoun ứng viên:** các hàng có `from`/`speaker`/`target` trong chunk, giữ phạm vi; đã có trong `ReferenceProjector`.
- **Điểm app phát hiện trong chunk** (chỉ gợi ý, không kèm đáp án): câu RAW chưa có trong DRAFT (bước 1–0), kana sót, thuật ngữ glossary chưa đúng ở dòng nào.
- Model chỉ trả `<EDITED>` của đúng DRAFT chunk; được tách/gộp dòng theo RAW trong chunk.

#### S8 — Lưu kế hoạch chunk

- Lưu cùng lượt chạy: hash RAW/DRAFT/glossary/pronoun, phiên bản thuật toán `CS-1`, các cặp đã căn (nén), điểm cắt, phán định và lý do.
- Mở lại thì không tính lại. Nguồn đổi hash → tính lại và báo "Nguồn đã thay đổi, kế hoạch chia đoạn được làm lại".

#### S9 — Ghép lại

- Nối các chunk đã sửa theo thứ tự; chunk lỗi giữ nguyên DRAFT và được đánh dấu.
- Ghép tất cả DRAFT chunk gốc phải ra đúng file DRAFT (kiểm từng byte trước khi gửi bất kỳ chunk nào).

#### Ngoài phạm vi CS-1 (báo, không đoán)

- DRAFT đảo thứ tự cả đoạn: vùng khớp tên thấp được gộp thành một chunk; nếu quá 3× ngân sách thì WARN kèm phạm vi dòng.
- Một file chứa nhiều chương, hoặc chương bị cắt giữa hai file: phán định sẽ báo WARN/BLOCK (tỷ lệ độ dài, dòng không ghép được); người dùng tự tách file.

#### Kiểm thử và nghiệm thu (offline, 0 USD)

- **Unit test:** đọc mọi encoding; rút tiêu đề từ các mẫu tên LN/WN; mỗi kiểu bước (1–1, 1–2, 2–1, 1–0, 0–1, lệch bù trừ); cắt không bao giờ rơi vào nhóm; ghép lại đúng từng byte; mỗi ngưỡng phán định có ca hai bên ngưỡng; thông báo đúng chữ.
- **Đo trên kho của owner** (lệnh chỉ in số đếm), phải tái tạo:
  - 133/133 điểm cắt đúng;
  - 55/55 cặp lệch chương BLOCK;
  - 0 cặp đúng BLOCK;
  - khác bản không có quá 1 OK;
  - ghép S1 đúng 28/28 LN và 85/85 WN.
- **Hiệu năng:** chương 1 222 dòng < 1 giây trên `emulator-5554`.

## 4. Quy tắc tiết kiệm khi test

1. Mọi thay đổi kiểm offline trước (test host, fake provider, replay response đã lưu). Không gọi API để phát hiện lỗi định dạng/đầu vào.
2. Mỗi giả thuyết **chỉ chạy live 1 chương**. Chỉ khi kết quả chương đó tốt và owner đồng ý mới chạy thêm **1 chương holdout**. Không chạy ma trận nhiều chương/nhiều nhánh một lần.
3. Mỗi lần live có trần tiền cụ thể do owner duyệt; dừng ngay ở lỗi đầu tiên (đã có trong CP-IMPL-2: dừng tại chunk lỗi đầu tiên).
4. Chương test: dev **007** (ngắn, thẳng dòng, đã có số liệu của mọi nhánh: luna 1 lượt 3/18, v5 8/18, Sol 13/18); holdout **011** (thẳng dòng, chưa dùng để chỉnh).

## 5. Lộ trình và yêu cầu làm việc chi tiết

### 5.1 Cài đặt dùng chung với luồng Dịch (Settings → Performance)

Khâu tách chunk của Biên tập **không có cài đặt riêng về kích thước**: nó đọc đúng các giá trị Performance mà luồng Dịch đang dùng, qua cùng các hàm.

| Cài đặt Performance | Luồng Dịch | Biên tập theo chunk |
|---|---|---|
| CHUNK MODE (`token`/`char`) | `Chunker.measure` | Đo **RAW** của chunk bằng chính `Chunker.measure(raw, mode)`: CJK/kana 1 token mỗi ký tự, ASCII theo từ, ký hiệu 0.5 |
| MAX TOKENS (450) / MAX CHARS (0 → 3 500) | `effectiveHardLimit()` | Giới hạn cứng = `Chunker.adaptiveLimit(s, s.effectiveHardLimit())` (đã tính preset economy ×2 / balanced ×1.5 và context của model) |
| SOFT LIMIT (0.8) | `effectiveSoftLimit()` | Giới hạn mềm = `adaptiveLimit(s, s.effectiveSoftLimit())`. Chỉ bắt đầu tìm điểm cắt an toàn khi chunk ≥ giới hạn mềm; phải cắt trước giới hạn cứng nếu có điểm an toàn |
| CONTEXT (400) + Context overlap | ngữ cảnh trước–sau | Luôn bật với Biên tập (cần để đọc câu kề). Lấy khoảng `contextChars` ký tự nhưng tròn theo **dòng nguyên**, tối thiểu 2 dòng mỗi bên |
| MAX OUTPUT (4 096) | trần token đầu ra | Ước tính token của DRAFT chunk × 1.3 phải ≤ MAX OUTPUT; vượt thì cắt chunk nhỏ hơn (vẫn theo luật điểm cắt an toàn) |
| GLOSSARY LIMIT (80) / PRONOUN LIMIT (40) | số mục mỗi chunk | Cùng giới hạn, áp **sau** khi lọc theo chunk; nếu phải bỏ bớt mục thì ghi số mục bị bỏ vào bản ghi chunk |
| TIMEOUT, RETRY, Retry empty/truncation/validation | gọi API | Dùng chung. Một chunk đã gửi mà không rõ kết quả thì không tự gửi lại (giữ quy tắc UNKNOWN) |
| TEMPERATURE | | Dùng chung |
| COST LIMIT USD + Stop on cost limit, Stop if pricing unknown | | Dùng chung: dừng trước chunk kế tiếp khi chạm trần; cộng thêm trần mỗi chương của tổ hợp Biên tập |
| Model, provider (tab Provider) | | Dùng chung; tổ hợp Biên tập có thể chọn model khác |

Khác biệt duy nhất so với Dịch: Dịch cắt RAW theo ký tự rồi mới dịch, còn Biên tập phải cắt **cả RAW và DRAFT tại cùng một ranh giới đã căn** (mục 3.4 S3, S5). Vì vậy điểm cắt chỉ rơi giữa hai dòng đã căn chắc, không giữa câu.

### 5.2 Lộ trình

| Bước | Nội dung | Live | Điều kiện qua |
|---|---|---|---|
| C1 | Khâu tách chunk CS-1 trong app + nguồn hai file + UI gọn + runner `CHUNK` + live chương 007 | 1 chương (D-C1 đã duyệt) | Mục 5.3 PASS |
| C2 | Nếu 007 tốt: holdout 1 chương (011), không chỉnh gì giữa 007 và 011 | 1 chương (cần duyệt) | Tỷ lệ sửa trúng và không làm hỏng giữ được trên 011 |
| C3 | Owner đọc 3 chương do app biên tập theo chunk (dùng UI như người dùng thật) | 3 chương (cần duyệt) | Owner chấp nhận 3/3 |
| P7 | Bàn giao theo checklist v4.18 | — | Sau C3 |

U1 (thư viện nguồn đầy đủ, màn chọn kiểu danh sách file) vẫn hoãn; C1 chỉ làm phần thư viện tối thiểu cần cho ghép file (S1).

### 5.3 Yêu cầu làm việc C1 (cho phiên Codex)

**Quyền:** D-C1 đã duyệt (owner, chat 2026-10-09): live **chỉ chương 007**, `openai/gpt-5.6-luna` reasoning medium, ledger mới `C1-<date>` trần **USD 0.05**, chỉ `emulator-5554`. Không pilot, không V5, không chương nào khác.

**Nguyên tắc:** mọi bước tách chunk chạy offline trong app; không gọi API để phát hiện lỗi đầu vào. Model chỉ trả `<EDITED>`. Không tạo file báo cáo mới: ghi kết quả vào mục 7 của tài liệu này. Bản FINAL của owner chỉ dùng để chấm.

#### C1.1 — Engine: khâu tách chunk CS-1 (thuần JVM, `editorial-engine/.../api/chunk/`)

| File mới | Trách nhiệm | Test bắt buộc |
|---|---|---|
| `LineUnits` | S2: tách dòng không rỗng + dòng trống theo sau; ghép lại đúng từng byte; CRLF→LF có bản đồ ngược | ghép lại == input cho LF, CRLF, lẫn lộn, BOM đã bỏ, dòng chỉ có `　` |
| `Anchors` | neo glossary (nguồn RAW ↔ đích DRAFT, không phân biệt hoa/thường) + chữ số (toàn khổ → nửa khổ) | thuật ngữ trùng/không trùng; số Hán không bị coi là số |
| `LineAligner` | S3: DP 1–1/1–2/2–1/1–0/0–1, chi phí như `align()` tham chiếu, dải chéo `|R−D|+50` | từng kiểu bước; lệch bù trừ (tách + gộp, số dòng bằng nhau); chuỗi gộp liên tiếp; RAW/DRAFT rỗng |
| `ChapterVerdict` | S4: OK/WARN/BLOCK + lý do có số, ngưỡng như bảng mục 3.4 | mỗi ngưỡng có ca hai bên; "khớp tên" chỉ tính khi có glossary và ≥ 15 dòng |
| `ChunkCutter` | S5: giới hạn mềm/cứng truyền vào (app lấy từ Performance), đo bằng hàm đo truyền vào (app truyền `Chunker::measure`), vùng đệm 2 cặp 1–1, nhóm không bị cắt, dòng dài = chunk riêng, không có điểm an toàn trong 3× cứng → cắt vùng đệm 1 + cờ, kiểm MAX OUTPUT | cắt không bao giờ rơi vào nhóm; ghép các chunk == file; đổi chế độ token/char thay đổi số chunk như Dịch |
| `ChunkChecks` | S6: tỷ lệ độ dài ±35%, số dòng thoại ±1, khớp tên ≥ 0.35; gộp với chunk kề; > 20% chunk không đạt → WARN | ca đạt, ca gộp, ca nâng WARN |
| `ChunkPlan` | S8: kết quả bất biến (hash nguồn, `CS-1`, cặp đã căn nén, điểm cắt, phán định, cờ) + mã hóa/giải mã JSON | khứ hồi JSON; hash đổi → không dùng lại |

Chuyển `PairMaps` sang dựng từ `ChunkPlan` (mỗi chunk một cặp), giữ `fromJobRows` cho nguồn job Dịch. Bỏ `EditorialPairSource.fromAlignedFiles` (1–1 thuần) hoặc để nó gọi `ChunkPlan`.

**Đo offline trên kho của owner** — script mới `scripts/chunk/measure_cs1.py` gọi engine qua một main JVM nhỏ (hoặc test JVM tắt mặc định, bật bằng biến môi trường trỏ tới `D:\Ebooks`), **chỉ in số đếm**. Phải tái tạo:

- 133/133 điểm cắt đúng trên 18 chương có đáp án (cùng định nghĩa đáp án như `cut_rule.py`).
- 55/55 cặp lệch chương → BLOCK, có và không có glossary.
- 0/113 cặp đúng → BLOCK.
- Cặp khác bản: không quá 1/11 OK.
- Với cài đặt Performance mặc định (token 450, soft 0.8, preset hiện tại): số chunk mỗi chương; chương 059 WN (1 222 dòng) chạy < 1 giây trên emulator.

#### C1.2 — App: nguồn hai file + màn xác nhận

1. `EditorialPairSourceLoader`: tổ hợp nguồn FILES → đọc bằng `FileUtil.readText` → `ChunkPlan` với giới hạn từ `AppSettings` hiện hành (`Chunker.adaptiveLimit`, `effectiveSoftLimit/HardLimit`, `chunkMode`, `contextChars`, `maxOutputTokens`, glossary/pronoun limit).
2. **S1 ghép file:** hàm thuần `SourceTitle.of(fileName)` rút tiêu đề (bỏ tiền tố số `0043-042　`/`043_`, hậu tố `_translated`/`_v2`/`(1)`/`_FINAL_QA_…`/series, đuôi file; NFKC). Ghép theo tiêu đề rồi thứ tự file; không dùng số chương trong truyện. Test trên tên thật của 28 LN + 85 WN (danh sách tên lưu trong test, không có nội dung sách): ghép đúng 28/28, 85/85.
3. **Màn xác nhận** (trước khi gọi API, chạy nền, hiện vòng chờ "Đang chia đoạn…"):
   - OK: "Chia N đoạn theo cài đặt Performance (token 450 · mềm 0.8). DRAFT tách/gộp dòng ở X chỗ; Y câu RAW chưa có trong DRAFT." + nút **Biên tập**.
   - WARN: "RAW và DRAFT khớp chưa chắc chắn" + tối đa 2 lý do có số + 2 dòng đầu mỗi file; nút **Vẫn biên tập** · **Chọn lại** · **Hủy**.
   - BLOCK: "RAW và DRAFT có vẻ không cùng chương" + tối đa 2 lý do có số + 2 dòng đầu mỗi file; nút **Chọn lại DRAFT** · **Chọn lại RAW** · **Hủy**. **Không có** nút chạy.
   - Câu lý do (mã → chữ): `NAME_MATCH` "chỉ P% tên riêng trong RAW có mặt ở dòng DRAFT tương ứng"; `EDGE_SYMBOLS` "P% dòng có dấu thoại/ký hiệu khác nhau"; `UNPAIRED_LINES` "N dòng không ghép được"; `LENGTH_RATIO` "DRAFT dài/ngắn hơn mức thường P%".
4. Lưu `ChunkPlan` cùng lượt chạy (store v27/v28, migration chỉ thêm cột/bảng). Mở lại không tính lại; nguồn đổi → tính lại và báo.

#### C1.3 — UI gọn

- Bỏ khỏi luồng người dùng: chế độ Kỹ, đường V5, "Cần xem" từ NOTES. Giữ cảnh báo cấu trúc của app và cờ "đoạn chưa chắc".
- Màn kết quả: danh sách đoạn (trạng thái từng đoạn), Xem bản cuối, So sánh với DRAFT, Xuất TXT.
- `EditorialApiUserStringsTest` quét thêm mọi chuỗi mới; không có "pack", "binding", "SAFE4", "cấp phép", tiếng Anh trong chuỗi người dùng.

#### C1.4 — Runner `CHUNK` + offline + build

1. Runner chế độ `CHUNK`: đọc RAW/DRAFT/Glossary/Pronoun **tên gốc** của 1 chương từ `D:\Ebooks\...`, chạy qua `EditorialPairRunService` thật với `AppSettings` mặc định của app, ghi `final.txt`, `chunk-plan.json` (không có văn bản sách: chỉ chỉ số dòng, hash, phán định), chi phí từng chunk. Dừng ở chunk lỗi đầu tiên.
2. Test host đầy đủ từ archive sạch: engine, app, `scripts/p6`, `scripts/chunk`.
3. Fake replay trên 007 thật: model trả nguyên DRAFT chunk → `final.txt` == DRAFT từng byte; số chunk khớp `measure_cs1.py`.
4. Build qua wrapper (code > bản cao nhất đã archive), cài chỉ `emulator-5554`, test Editorial/chunk tập trung (danh sách lớp cụ thể, không chạy bộ instrumented rộng).

#### C1.5 — Live chương 007 (D-C1)

1. Kiểm trước: phán định 007 phải OK; ước tính worst-case ≤ USD 0.05 (nếu vượt: dừng, báo số).
2. Chạy 1 lần. Dừng ở chunk lỗi đầu tiên; không gửi lại chunk đã gửi.
3. Chấm `score_vs_final.py` (chỉ số mục 3 của Q1 review: khớp, gần hơn, xa hơn, sửa thừa, độ giống cả chương). So với mốc 007: luna toàn chương 3/18 · v5 8/18 · Sol 13/18.
4. Xuất bản app + trang đọc (RAW/DRAFT/app/FINAL, giống `q2-sol-review.html`) vào `D:\P5E-private\chunk-outputs\007\`.
5. Ghi 5–10 dòng kết quả vào mục 7; cập nhật mục 6, §10 canonical, snapshot, BUILD_STATE. Dừng.

**PASS C1:** offline đạt mọi số ở C1.1; fake replay đúng từng byte; live không chunk nào bị chặn cấu trúc, 0 dòng sinh chữ Nhật, và 007 sửa trúng rõ hơn luna toàn chương (> 3/18). Đạt hay không về chất lượng vẫn do owner đọc.

**Dừng và hỏi owner nếu:** phán định 007 không phải OK; ước tính vượt USD 0.05; UNKNOWN cost; lỗi hạ tầng; cần đổi ngưỡng CS-1 (ngưỡng chỉ được đổi khi số đo offline chứng minh, và phải ghi lý do).

## 6. Trạng thái

- D-C1 đã duyệt (owner, chat 2026-10-09): live chỉ chương 007, luna reasoning medium, trần USD 0.05.
- 0/3 chương được owner chấp nhận; P7 chưa bắt đầu; U1 tạm hoãn.
- CP-IMPL-2 (chunk không ghi chú, dừng ở chunk lỗi đầu tiên) đã commit `90e9a6f7`.
- `fromAlignedFiles` + 5 test: commit cùng tài liệu này.
- Chi tiêu tới nay (ledger Q2): USD 1.17703995 trong trần D-Q2b USD 10.00. D-Q2b/D-Q2c không cấp quyền cho live chunk.

## 7. Nhật ký

- 2026-10-09 — coordinator tiếp quản sau khi Codex hết quota: hoàn tất và commit CP-IMPL-2 (engine 583, app 0 lỗi, `scripts/p6` 93); chốt nguyên nhân bằng số đo mục 1; thêm nguồn hai file thẳng dòng; viết kế hoạch này.
- 2026-10-09 — coordinator: đo lệch dòng trên 28 chương, prototype căn dòng Gale–Church + neo glossary (42/42 điểm cắt đúng trên 8 chương có đáp án; phát hiện lệch bù trừ ở 016/027 mà ghép 1–1 thuần bỏ sót); cập nhật C1.1 thay ghép 1–1 thuần bằng căn dòng.
- 2026-10-09 — coordinator: khảo sát bộ WN 85 chương. Phát hiện ghép file theo số chương sai (thứ tự file ≠ số chương), RAW UTF-16, chuỗi gộp dòng, DRAFT thiếu câu, DRAFT không áp glossary. Thêm quy tắc cắt có vùng đệm 2 cặp: 133/133 điểm cắt đúng trên 18 chương có đáp án. Ghi rõ: căn dòng mới là prototype Python, chưa có trong app.
- 2026-10-09 — coordinator: chốt thiết kế khâu tách chunk CS-1 (mục 3.4). Hiệu chỉnh luật phán định trên 113 cặp đúng, 55 cặp lệch chương, 11 cặp khác bản: lệch chương 55/55 BLOCK, cặp đúng 0 BLOCK. Thêm `verdict()` vào bộ căn mẫu.
- 2026-10-09 — owner duyệt D-C1; coordinator xác nhận khâu tách chunk dùng chung cài đặt Performance của luồng Dịch (`Chunker.measure`, `adaptiveLimit`, soft/hard, context, max output, giới hạn glossary/pronoun) và viết lại mục 5 thành lộ trình + yêu cầu làm việc C1 chi tiết.
- 2026-10-09 — C1.1 xong (Codex): khâu tách chunk CS-1 trong engine, `editorial/api/chunk/` (`LineUnits`, `Anchors`, `LineAligner`, `ChapterVerdict`, `ChunkCutter`, `ChunkChecks`, `ChunkPlan`, `ChunkPlanner`), thuần JVM, 53 test mới (engine 635, 1 test đo kho bị tắt khi không có biến môi trường). `ChunkCutter` nhận giới hạn mềm/cứng, hàm đo RAW, hàm đo DRAFT và MAX OUTPUT từ người gọi (app sẽ truyền `Chunker.measure` và các giá trị Performance ở C1.2). Đo trên kho bằng `scripts/chunk/measure_cs1.py` (chỉ in số đếm; cấu hình đo: mềm 900 ký tự, không cứng, tức đúng luật tham chiếu): **133/133 điểm cắt đúng trên 18 chương có đáp án; lệch chương 55/55 BLOCK (có và không glossary); cặp đúng 0/113 BLOCK (có glossary 108 OK + 5 WARN, không glossary 110 OK + 3 WARN); khác bản 0/11 OK khi có glossary, 1/11 OK khi không**. Đúng bằng các số ở mục 3.4, ngưỡng không đổi. Chương WN 059 (1 222 × 1 213 dòng): căn dòng và phán định 53 ms trên JVM. Chưa làm: đo số chunk với cài đặt Performance mặc định và đo trên emulator (C1.4), nối vào app (C1.2).
- 2026-10-09 — C1.2 làm dở (Codex, hết hạn mức giữa chừng): xong phần engine của C1.2 — `SourceTitle` (S1: tiêu đề từ tên file, ghép RAW↔DRAFT theo tiêu đề rồi theo thứ tự file, glossary/pronoun theo vị trí file; test trên tên thật 28 LN + 85 WN: ghép đúng 28/28 và 85/85), ngữ cảnh chỉ đọc tròn theo dòng nguyên (tối thiểu 2 dòng, `DocManifest.contextBeforeLines/AfterLines`, `PairPromptBuilder.ContextPolicy`), `ChunkPlan.Limits` mang `contextChars`. Engine 641 test (1 bị tắt), 0 lỗi. **Chưa làm:** nối vào app (loader đọc Performance qua `Chunker.measure/adaptiveLimit`, `EditorialPairSource.fromPlan` thay `fromAlignedFiles`, cột `chunk_plan_json` DB v29, màn xác nhận OK/WARN/BLOCK), C1.3 UI gọn, C1.4 runner CHUNK + build + emulator + số chunk với Performance mặc định, C1.5 live 007. Chưa có lượt gọi provider nào, 0 USD, chưa tạo ledger C1.
- 2026-10-09 — C1.2 xong (Codex): nguồn hai file trong app. `EditorialPairSourceLoader.plannerSettings` lấy giới hạn từ Settings → Performance qua đúng các hàm của luồng Dịch (`Chunker.measure` theo chế độ token/char, `Chunker.adaptiveLimit` cho giới hạn cứng và mềm, MAX OUTPUT, CONTEXT); `loadFiles` đọc RAW/DRAFT bằng `FileUtil.readText` rồi chạy `ChunkPlanner`; `EditorialPairSource.fromPlan` thay ghép 1–1 thuần (`fromAlignedFiles` và test cũ đã bỏ). Kế hoạch chunk lưu cùng lượt chạy (cột `chunk_plan_json`, DB v29, migration chỉ thêm cột; mở lại không tính lại; ngữ cảnh chỉ đọc lấy `contextChars` từ kế hoạch, tròn theo dòng). Màn xác nhận có vòng chờ "Đang chia đoạn…", OK = một dòng tóm tắt + nút Biên tập; WARN = lý do có số (tối đa 2) + 2 dòng đầu mỗi file + Vẫn biên tập / Chọn lại / Hủy; BLOCK = lý do + Chọn lại DRAFT / Chọn lại RAW / Hủy, không có nút chạy, `prepare` từ chối bằng `SOURCE_CHAPTER_BLOCKED` trước khi gửi gì. Luồng Hai file luôn đi theo chunk; ô chọn chế độ (Nhanh/Kỹ) đã bỏ khỏi form. Test mới `EditorialPairFilesFlowTest` (Performance điều khiển cắt; hàng = chunk và ghép lại đúng file; replay giả cả chương trả DRAFT đúng từng byte, 1 lượt gọi mỗi chunk; cặp lệch chương bị chặn, 0 request) và `EditorialPairV29MigrationSpecTest`; app 454 test, 0 lỗi; androidTest compile PASS. Còn lại: C1.3 (kết quả theo đoạn, bỏ "Cần xem"/V5 khỏi UI), C1.4, C1.5.
- 2026-10-09 — C1.3 xong (Codex): UI gọn. Ô chọn chế độ Nhanh/Kỹ đã bỏ khỏi form; luồng Hai file và Job đều đi theo chunk (một lượt gọi mỗi đoạn, không NOTES, không bước Kiểm); đường V5 không có trong UI. Từ ngữ người dùng đổi "cặp" thành "đoạn" như luồng Dịch. Màn kết quả: danh sách đoạn và trạng thái từng đoạn, **Xem bản cuối** (hộp thoại chỉ đọc, chọn chép được), **So sánh với DRAFT** (mở/ẩn diff mọi đoạn), Xuất TXT, cờ "đoạn chưa chắc" lấy từ kế hoạch chunk, cảnh báo cấu trúc của app giữ nguyên. Kết quả cũ của luồng một lượt toàn chương vẫn đọc được từ danh sách (không tạo thêm "Cần xem" nào). `EditorialApiUserStringsTest` quét thêm các chuỗi mới (không có "pack", "binding", "SAFE4", "cấp phép"). App 455 test, 0 lỗi; androidTest compile PASS.
- 2026-10-09 — C1.4 (phần mã, Codex): runner chế độ `CHUNK` (`EditorialApiV1FixtureRunnerInstrumentedTest#runChunkFixture`) đọc RAW/DRAFT/Glossary/Pronoun **tên gốc** của một chương, cắt bằng CS-1 với `AppSettings` của app, chạy qua `EditorialPairRunService` thật, mỗi lượt gọi có định danh sổ riêng (`PerCallLedger`); ghi `final.txt`, `chunk-plan.json` (không văn bản sách), `chunks.json` (trạng thái, token, USD từng chunk), `run-metadata.json`, sổ chi tiêu; không gửi lại chunk đã gửi, dừng ở chunk lỗi đầu tiên. Offline dùng provider giả trả nguyên DRAFT chunk. Số chunk với cài đặt Performance mặc định (token 450, mềm 0.8, preset balanced → cứng 675, mềm 540, MAX OUTPUT 4096, CONTEXT 400) đo bằng `scripts/chunk/measure_cs1.py --performance` (test app tắt mặc định, chỉ in số đếm): 113 chương, mỗi chương 3–51 chunk (trung vị 13), **007 = 7 chunk (OK), 011 = 13 chunk (OK), WN 059 (1 222 dòng) = 39 chunk (WARN)**; kế hoạch dài nhất tính 55 ms trên JVM. Thêm test thiết bị `EditorialChunkPlanPerformanceInstrumentedTest` (đo chương dài trên emulator). Test đầy đủ: engine 641 (1 tắt), app 456 ×3 biến thể (1 tắt), `scripts/p6` 93, androidTest compile PASS. Còn lại: build wrapper, cài emulator, fake replay 007 thật, đo 059 trên emulator, test tập trung (C1.4); live 007 (C1.5).
- 2026-10-09 — C1.4 xong (Codex): build, thiết bị và replay. **Build** `4.18-c1.4` / code 251, sự kiện `build-20261009-180414`, nguồn `56399f1d049f100355c8918d1cb858b2e4f11cd3`, APK SHA-256 `A9C3A65A0555582F982A676C1BB479DED795B30D7568DC80824564316CFBB28C`, ZIP nguồn `6262E0F3E79F092F08BE49E208018C1406413B10F9B12BB209010BBE6713DC00`; AndroidTest `c1-chunk-20261009d` SHA-256 `D5A18C406FEA8EC84C63069A26602A6A254173EA9494840434CB6FC61A690B9B`; payload giống hệt trong `artifacts/` và `backup/`; chỉ cài trên `emulator-5554`. **Test host từ archive sạch của `56399f1d`:** engine 641 (1 tắt), app 456 ×3 biến thể (1 tắt), `scripts/p6` 93, androidTest compile PASS, đo kho lại đúng 133/133, 55/55, 0/113, khác bản ≤ 1/11 OK. **Emulator, test tập trung (lớp cụ thể, không chạy bộ rộng):** `EditorialPairStoreInstrumentedTest` 6/6 (lần đầu chạy trên thiết bị, DB v29), `EditorialPairUiInstrumentedTest` 6/6 (3 ca theo pha bỏ qua) + pha chết tiến trình seed→force-stop→verify→cleanup PASS, `EditorialApiStoreInstrumentedTest` 6/6, `EditorialApiBienTapFlowInstrumentedTest` 3/3 (đường toàn chương cũ), `EditorialApiBienTapUiInstrumentedTest` 5 (2 chạy + 3 theo pha) và pha chết tiến trình PASS; sổ Q2 không đổi (SHA-256 `86087576…efc2`). **Lỗi thật do thiết bị bắt được và đã sửa:** `Pattern.UNICODE_CHARACTER_CLASS` không có trên Android (kế hoạch không khởi tạo được) → dùng `Character.isDigit`, số đo kho không đổi; combo mới mặc định nguồn Job nên các test UI cũ không vào được màn xác nhận → mặc định là Hai file. **Replay giả trên 007 thật** (emulator, 0 USD, `run_c1_chunk.ps1 -Offline`): 7 chunk (khớp đo host với Performance mặc định), phán định OK, 7 lượt gọi giả, `final.txt` == DRAFT từng byte (SHA-256 `151b53d1…67fe`, bằng SHA-256 DRAFT 007), kế hoạch chunk 262 ms. **Chương WN 059 (1 222 × 1 213 dòng) trên emulator:** 39 chunk (đúng bằng host), WARN, lập kế hoạch 578 ms (tốt nhất trong 3 lần) < 1 giây. Cài đặt Performance dùng: token, cứng 675, mềm 540 (preset balanced), MAX OUTPUT 4096, CONTEXT 400.
