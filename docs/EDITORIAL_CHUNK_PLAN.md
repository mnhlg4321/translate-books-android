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

## 5. Việc tiếp theo (C1) — cho phiên Codex

Ownership: APP (`EditorialPairSourceLoader`, `EditorialApiUiController`, `EditorialApiPageFactory`, `EditorialPairRunService` nếu cần), RUNNER (`EditorialApiV1FixtureRunnerInstrumentedTest`, `scripts/p6/run_group.ps1`), không đụng engine ngoài sửa lỗi có test.

1. **Khâu tách chunk CS-1 (mục 3.4) — engine thuần JVM + app:** làm đủ S0–S9 theo mục 3.4, đúng ngưỡng và thông báo đã ghi; nghiệm thu bằng các số ở cuối mục 3.4. Chi tiết cũ giữ để tra: port `scripts/chunk/align_lines_reference.py` thành `LineAligner` (bước 1–1/1–2/2–1/1–0/0–1, chi phí độ dài + ký hiệu đầu/cuối + neo glossary + số) và `ChunkCutter` (cắt khi đủ ngân sách ký tự RAW — lấy từ cài đặt chunk của luồng Dịch, mặc định 900 — và có ≥ 2 cặp 1–1 liền nhau ở mỗi bên; kiểm từng chunk như mục 3.1.4; báo "không cùng bản" khi mức khớp neo/tỷ lệ độ dài rớt diện rộng). Ghép file trong thư viện nguồn theo tiêu đề rồi thứ tự file (mục 3.2); đọc file qua `FileUtil.readText`. Đo offline thêm trên 85 chương WN: tái tạo 133/133 điểm cắt đúng và các số ở mục 3.2. Thay `fromAlignedFiles` 1–1 thuần bằng kết quả căn dòng, cho cả chương bằng số dòng. Test: ca tổng hợp cho từng kiểu lệch (tách, gộp, thiếu, thêm, bù trừ tách+gộp), cắt không bao giờ rơi trong nhóm tách/gộp, các dòng ghép lại đúng nguyên file. Đo offline trên 28 chương (chỉ đếm): tái tạo đúng các kết quả mục 3.1, ghi số vào mục 7. Nối vào combo nguồn FILES; chunk nghi ngờ → gộp; vẫn nghi → một chunk toàn chương kèm thông báo.
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
- 2026-10-09 — coordinator: đo lệch dòng trên 28 chương, prototype căn dòng Gale–Church + neo glossary (42/42 điểm cắt đúng trên 8 chương có đáp án; phát hiện lệch bù trừ ở 016/027 mà ghép 1–1 thuần bỏ sót); cập nhật C1.1 thay ghép 1–1 thuần bằng căn dòng.
- 2026-10-09 — coordinator: khảo sát bộ WN 85 chương. Phát hiện ghép file theo số chương sai (thứ tự file ≠ số chương), RAW UTF-16, chuỗi gộp dòng, DRAFT thiếu câu, DRAFT không áp glossary. Thêm quy tắc cắt có vùng đệm 2 cặp: 133/133 điểm cắt đúng trên 18 chương có đáp án. Ghi rõ: căn dòng mới là prototype Python, chưa có trong app.
- 2026-10-09 — coordinator: chốt thiết kế khâu tách chunk CS-1 (mục 3.4). Hiệu chỉnh luật phán định trên 113 cặp đúng, 55 cặp lệch chương, 11 cặp khác bản: lệch chương 55/55 BLOCK, cặp đúng 0 BLOCK. Thêm `verdict()` vào bộ căn mẫu.
