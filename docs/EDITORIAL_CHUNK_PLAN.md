# Biên tập theo chunk — kế hoạch sống (từ 2026-10-09)

Owner (chat 2026-10-09) yêu cầu: tiếp quản việc dở; xác định chính xác nguyên nhân lỗi; loại bỏ ngay các báo cáo/receipt không cần thiết; thiết kế lại Biên tập theo chunk, một lần gọi cho mỗi chunk như luồng Dịch, mỗi chunk gửi cặp RAW–DRAFT khớp nhau và glossary/pronoun ứng viên của chính chunk đó; không test nhiều chương một lúc gây tốn tiền.

Đây là **tài liệu duy nhất** cho hướng chunk. Mỗi lượt làm việc cập nhật tại chỗ (mục 6 trạng thái, mục 7 nhật ký ngắn); không tạo thêm file review/request/execution riêng.

## 1. Nguyên nhân (đo trên 5 chương dev 004–008, so với 28 bản FINAL của owner)

| Cách chạy | Chỗ owner sửa mà app làm đúng/gần đúng | Chi phí dev | Ghi chú |
|---|---:|---:|---|
| luna, 1 lượt toàn chương (E-luna-b) | 6.0% | USD 0.045 | sửa thừa 45 dòng |
| luna, pack v5 3 lượt + báo cáo (V5-luna) | 13.8% | USD 0.168 | 1/5 chương bị chặn ở QA receipt (`CONTENT_UNACCOUNTED_CHANGE`) |
| Sol, 1 lượt toàn chương (E-strong) | 50.3% | USD 0.443 | sửa thừa 165 dòng |

Kết luận đã hiệu chỉnh sau review 2026-10-09:

1. Các cấu hình trên có số điểm khác nhau; chưa cô lập ảnh hưởng của model, prompt và bộ chấm. Không suy ra model nhỏ hay context toàn chương là nguyên nhân duy nhất; không suy ra sửa prompt vô ích. Similarity với FINAL là tín hiệu, không phải phép chấm lỗi xưng hô hay nghĩa.
2. Luồng V5 ba lượt tốn khoảng bốn lần E trong tập này và từng dừng vì schema, danh tính, Change Map. Chưa đo phần chi phí riêng của báo cáo, nên không gán toàn bộ chênh lệch cho receipt. Nhật ký chi tiêu do app ghi không tốn token provider.
3. C1 đã chạy thật theo chunk: 007 lần 3 đủ 7/7, nhưng ca có rule xưng hô vẫn sửa sai và có 27 dòng khác ở vùng owner không sửa. Chunk giải quyết đơn vị xử lý, chưa chứng minh giải quyết chất lượng. Các dòng khác này cần phân xử, không mặc định đều là lỗi nghĩa.

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
 → căn RAW–DRAFT bằng CS-1, kiểm OK/WARN/BLOCK rồi cắt ở biên an toàn theo Settings → Performance (không ghép i↔i chỉ vì bằng số dòng)
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

**Ghi chú lịch sử trước C1.1, đã được thay thế:** mục 3.1–3.2 là tính toán tất định; C1.1–C1.4 hiện đã đưa CS-1 vào app (xem §6–7). Tại thời điểm prototype, trong app mới có: luồng chunk theo dòng của job Dịch (CP-IMPL-2) và ghép 1–1 thuần `fromAlignedFiles`. Bộ căn dòng, quy tắc cắt có vùng đệm, ghép file theo tiêu đề/thứ tự **mới ở dạng prototype Python** trên máy tính (`scripts/chunk/align_lines_reference.py`). Đưa vào app là việc C1.1.

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

### 5.4 Hướng chốt để build hoàn thiện: sửa theo điểm app gắn cờ (CP-IMPL-6) — 2026-10-09

Owner yêu cầu "chốt hạ hướng build hoàn thiện". Ưu tiên của owner: bắt lỗi xưng hô theo hàng pronoun, rồi thuật ngữ glossary; sửa văn phong tùy tiện là lỗi.

**Bằng chứng (chương 007, luna medium):**

| Cách giao việc cho model | Kết quả |
|---|---|
| Viết lại cả chunk, luật rộng (C1.5 lần 3) | Sửa trúng 4/18; **27 dòng owner không sửa bị đổi**; độ giống FINAL 0.9216 < DRAFT 0.9882 |
| Viết lại cả chunk, luật hẹp (chunk 005, CP-IMPL-4) | Trả nguyên DRAFT; **bỏ sót cả 2 lỗi xưng hô** mục tiêu |
| App tự phát hiện (khối xưng hô CP-IMPL-5, offline, 0 USD) | Chỉ ra 69% dòng xưng hô owner sửa trên 8 chương (94% ở 006/011/014/017) |

Model nhỏ khi được giao "trả lại cả đoạn đã sửa" dao động giữa sửa linh tinh và không sửa gì. Còn việc tìm chỗ sai theo luật (xưng hô, thuật ngữ, kana sót, câu thiếu) thì app làm tất định, rẻ và đã đo được. Hai lần thử thêm luật vào prompt không đổi được hành vi; theo quy tắc "hai lần cùng họ lỗi thì đổi thiết kế", **dừng các phép thử viết lại cả chunk**.

**Quyết định:**

1. App tìm **điểm cần sửa** trong từng chunk (tất định, offline):
   - xưng hô lệch hàng pronoun (từ khối CP-IMPL-5);
   - thuật ngữ: dòng RAW có nguồn glossary mà dòng DRAFT tương ứng thiếu dạng đích;
   - kana/Hán sót trong DRAFT;
   - câu RAW không có trong DRAFT (bước 1–0 của bộ căn dòng).
2. Model chỉ nhận **danh sách điểm đánh số** (kèm câu RAW, câu DRAFT và luật áp dụng, đoạn chunk làm ngữ cảnh chỉ đọc). Với mỗi điểm, model trả **một dòng**: `[n] <câu đã sửa>`, hoặc `[n] =` nếu câu đúng.
3. App chỉ thay **đúng những câu được gắn cờ**. Câu khác **không thể** bị đổi, nên hết "sửa linh tinh" ngay từ cấu trúc, không phải nhờ prompt.
4. Chunk không có điểm nào → **không gọi API** (rẻ hơn).
5. Viết lại cả chunk (sửa văn phong) bị gỡ khỏi luồng người dùng. Mã giữ lại, không phát triển tiếp. Chỉ xem xét lại sau P7, nếu owner muốn, với model mạnh hơn.

**Lộ trình chốt:**

| Bước | Nội dung | Live |
|---|---|---|
| C6 | CP-IMPL-6 offline: điểm sửa, prompt mục tiêu, parser khoan dung, áp dụng có kiểm, giao diện "Đã sửa N chỗ"; đo độ phủ trên 8 chương | 0 USD |
| C6-live | Chương **006** (nhiều hàng pronoun, dev): một lần chạy cả chương | cần D-C6 |
| C7 | Owner đọc 3 chương do app sửa theo điểm, dùng app như người dùng thật | cần duyệt |
| P7 | Bàn giao theo checklist v4.18 | — |

#### Yêu cầu làm việc C6 (cho phiên Codex)

Branch giữ nguyên. Không pilot, không V5, không U1 đầy đủ. Mọi thứ trong C6.1–C6.4 offline, 0 USD. Không tạo file báo cáo mới; ghi kết quả vào mục 7.

**C6.1 — Engine (`editorial/api/fix/`, thuần JVM):**

| File | Trách nhiệm | Test bắt buộc |
|---|---|---|
| `FixPoint` | 1 điểm: id, loại (`ADDRESS`, `GLOSSARY`, `KANA`, `MISSING`), chỉ số dòng DRAFT (hoặc vị trí chèn), câu DRAFT nguyên văn, câu RAW tương ứng, luật (hàng pronoun self/call/scope hoặc glossary nguồn→đích) | bất biến; không có điểm không có luật |
| `FixPointFinder` | Từ `ChunkPlan` + cặp đã căn + glossary + pronoun: dùng lại `AddressChecklist` cho `ADDRESS` (ở mức dòng, không chỉ đoạn), `Anchors` cho `GLOSSARY`, bộ phát hiện kana đã có cho `KANA`, bước 1–0 cho `MISSING`. Một dòng nhiều lỗi → một điểm gộp các luật | mỗi loại; hàng đối xứng không nhân đôi điểm; dòng lời kể không thành điểm `ADDRESS`; dòng đã đúng dạng không thành điểm |
| `TargetedFixPrompt` | System ngắn: chỉ sửa câu được liệt kê; mỗi mục đúng một dòng `[n] …` hoặc `[n] =`; xưng hô chỉ đổi theo hàng khi RAW cho thấy người nói/người nghe đúng là speaker/target của hàng, ngược lại `=`; không thêm gì khác. User: RAW + DRAFT của chunk (chỉ đọc), rồi danh sách điểm | không chứa FINAL; không chứa yêu cầu báo cáo; mỗi điểm có đủ câu RAW, câu DRAFT, luật |
| `TargetedFixParser` | Khoan dung: nhận dòng `^\[(\d+)\]\s*(.*)$`; `=`, rỗng hoặc trùng nguyên văn → giữ; id lạ bỏ qua; id trùng lấy lần đầu; thiếu id → giữ; có thể có chữ thừa ngoài các dòng `[n]` → bỏ qua và đếm. **Không bao giờ dừng chạy vì định dạng** | các dạng lệch thường gặp: có `</EDIT>`, có markdown, có đánh số `1.`/`(1)`, có dấu ngoặc kép bao câu |
| `TargetedFixApplier` | Thay đúng dòng của điểm trong phạm vi chunk; `MISSING` chèn sau dòng neo; kiểm từng dòng mới: không thêm kana/Hán (trừ nguồn glossary), không thẻ/meta, độ dài 0.5–2.0 lần dòng cũ (trừ `MISSING`). Vi phạm → giữ dòng cũ và ghi `REJECTED` | dòng ngoài điểm không bao giờ đổi (so từng byte); các trường hợp bị từ chối |

Tăng contract lên `CP-IMPL-6`; lượt chạy cũ không được tiếp tục bằng contract mới (giữ luật CP-IMPL-2).

**C6.2 — App:**

- `EditorialPairRunService`: chunk 0 điểm → `ACCEPTED_NO_CHANGE`, không gọi API. Chunk có điểm → 1 lần gọi, parse, áp dụng, lưu kết quả từng điểm (`FIXED` / `KEPT` / `REJECTED` / `NO_ANSWER`). Giữ toàn bộ luật sổ chi tiêu, UNKNOWN và dừng ở lỗi đầu tiên (lỗi gọi API, không phải lỗi định dạng).
- Màn xác nhận thêm: "Tìm thấy N điểm cần kiểm: X xưng hô, Y thuật ngữ, Z kana sót, W câu thiếu — sẽ gửi K/M đoạn."
- Màn kết quả: "Đã sửa A chỗ, giữ nguyên B chỗ, từ chối C chỗ". Danh sách điểm có trước/sau và luật; Xem bản cuối; So sánh; Xuất TXT.
- Ẩn đường viết lại cả chunk khỏi UI. Chuỗi mới qua `EditorialApiUserStringsTest`.

**C6.3 — Đo offline trên 8 chương có sẵn (004–008, 011, 014, 017), chỉ in số đếm:**

- Số điểm theo loại; số chunk cần gọi / tổng chunk; ước tính USD mỗi chương với luna.
- Độ phủ: % dòng owner sửa có đụng xưng hô / thuật ngữ / kana nằm trong điểm.
- Nhiễu: % điểm mà FINAL không đổi dòng đó.
- Phải ≥ 69% với xưng hô như CP-IMPL-5; ghi số cho thuật ngữ, kana, câu thiếu.

**C6.4 — Offline + build:**

- Test host từ archive sạch: engine, app, `scripts/p6`, `scripts/chunk`.
- Fake provider trên 006 thật: trả `=` cho mọi điểm → `final.txt` == DRAFT từng byte. Trả câu sửa giả cho một điểm → chỉ đúng dòng đó đổi.
- Build qua wrapper (code > bản cao nhất đã archive), cài chỉ `emulator-5554`, test thiết bị tập trung như C1.4.

**C6.5 — Live chương 006 (D-C6 đã duyệt, owner chat 2026-10-09):**

- luna medium, sổ mới `C6-<date>`, trần USD 0.03.
- Kiểm trước: phán định chương OK; ước tính ≤ trần.
- Một lần chạy cả chương. Chấm theo **chỉ số xưng hô**, phân xử theo RAW và luật (xem "Thước đo chất lượng" bên dưới); số so với FINAL chỉ ghi để tham khảo:
  - với các dòng owner sửa có đụng xưng hô và nằm trong điểm: sửa đúng / bỏ sót / sửa sai;
  - với các điểm mà FINAL không đổi: số dòng app đổi (sửa sai);
  - tương tự cho thuật ngữ, kana, câu thiếu;
  - dòng ngoài điểm bị đổi: phải = 0.
- Xuất bản app + trang đọc vào `D:\P5E-private\chunk-outputs\006\`. Dừng, báo cáo trong mục 7.

**Thước đo chất lượng (owner, chat 2026-10-09):** bản FINAL của owner **không phải luật tuyệt đối**; bản test chỉ cần đạt chuẩn chất lượng đặt ra, rồi owner đọc lại. Vì vậy:

- Phán xử từng điểm **theo RAW và luật** (hàng pronoun, glossary, đủ câu), không theo việc có trùng FINAL hay không. Dùng bộ chấm phân xử theo RAW đã có (`0e0ebd8c`). Mỗi điểm nhận một nhãn:
  - `ĐÚNG`: sửa đúng luật, hoặc giữ đúng khi luật không áp dụng;
  - `SÓT`: luật áp dụng mà vẫn giữ;
  - `SAI`: đổi sai luật hoặc sai nghĩa;
  - `CHƯA CHẮC`: RAW không đủ căn cứ.
- So với FINAL chỉ là **thông tin tham khảo** trong báo cáo, không dùng làm cổng.
- Trang đọc cho owner: RAW / DRAFT / app, kèm luật của từng điểm; cột FINAL ghi rõ "tham chiếu".

**PASS C6:**

- Offline đạt mọi mục C6.4.
- Live: 0 dòng ngoài điểm bị đổi, 0 chữ Nhật mới, không mất câu.
- Trong các điểm có luật áp dụng rõ: `ĐÚNG` ≥ 60%; `SAI` ≤ 10% tổng số điểm.
- **Owner đọc trang đọc và chấp nhận** — đây là điều kiện quyết định.
 Nếu không đạt về xưng hô: thử lại **cùng chương** với reasoning `high` (một biến) trước khi xét model khác; cần owner duyệt.

**Dừng và hỏi owner nếu:** phán định 006 không OK, ước tính vượt trần, UNKNOWN cost, lỗi hạ tầng, hoặc cần đổi ngưỡng phát hiện.

## 6. Trạng thái

- D-C6 đã duyệt (owner, chat 2026-10-09): live chỉ chương 006, luna reasoning medium, sổ mới trần USD 0.03, sau khi C6.1–C6.4 offline đạt. Owner: FINAL là tham chiếu, không phải luật tuyệt đối; cổng là chuẩn chất lượng phân xử theo RAW + owner đọc.
**Hiện hành — CP-IMPL-5 (khối ADDRESS CHECK) đã chuẩn bị offline, 2026-10-09:** app chỉ ra đoạn thoại nằm trong phạm vi hàng pronoun mà DRAFT dùng từ xưng hô khác `self`/`call` của hàng; chỉ chunk 005 của 007 đổi (một khối), 6 chunk còn lại trùng từng byte; test engine 651, app 465 PASS. Chưa build/cài/gọi provider, USD 0; hành vi model NOT_MEASURED. APK vẫn `4.18-c1.7` (CP-IMPL-4). 0/3 chương được chấp nhận; P7 chưa bắt đầu. **Next action duy nhất: owner duyệt hoặc từ chối lần live thứ hai trên cùng chunk 005 với CP-IMPL-5 (đề xuất ở cuối §6.2: đúng 1 call, không retry, sổ mới `C1-SINGLE5B-20261009` trần USD 0.01); nếu duyệt thì build `4.18-c1.8` từ commit gói này, cài chỉ `emulator-5554`, chạy thử fake với selector rồi gọi 1 lần. Chưa có quyền chi nào.**

| Claim cần kiểm | Evidence đã đọc độc lập | Kết luận |
|---|---|---|
| Đã chạy theo chunk | Run `f0d387b5-52bb-4eb0-98d0-f7717b828ef1`, metadata mode CHUNK, 7 chunks; chunks.json mỗi chunk 1 call, finish stop | Đã chứng minh; không còn là phép thử toàn chương Q2 |
| Rule được gửi nhưng không tuân thủ | prompts/005-EDIT.txt có row call và phạm vi P064–P075; RAW xác định lượt thoại; responses/005-EDIT.json dòng 8 dùng dạng xưng hô khác rule | Một ca lỗi tuân thủ rule đã xác minh; không phải lỗi filter bỏ row |
| 007 chỉ có một lỗi xưng hô trong 18 dòng | Báo cáo phân loại thô; chưa có annotation từng occurrence đã phân xử | Chưa chứng minh toàn bộ; không dùng 4/18 làm điểm xưng hô |
| Văn phong gây 27 lỗi mới | Core cũ cho sửa phrasing/register; scorer đếm 27 dòng owner không sửa mà app đổi | Có quyền sửa rộng; quan hệ nhân quả và số lỗi nghĩa mới chưa đo |
| Ba lần chạy hết USD 0.0404443 | verify_spend_ledger.py: 17 calls, 34 entries, pending 0, settled/exposure 0.04044430 | Ledger hash-chain PASS; không có call mới trong review |

Evidence riêng: `D:/P5E-private/chunk-runs/C1/live/007-f0d387b5-52bb-4eb0-98d0-f7717b828ef1/results/fx-a01/`. Hash final đã tính lại: `59f4a4a083c76d287cd6b278086bb24958ef70af263dd53cd360175401d0ff43`. Source/build metadata trùng `57978e83`; BUILD_INFO ghi branch build tạm `tmp/api-n4-20261005`, không phải branch workspace. Không kiểm thiết bị trong review, chỉ xác minh evidence bản đã chạy. Không chép văn bản sách vào Git.

Test patch CP-IMPL-4 từ archive sạch `a14272ab` + đúng 4 file overlay: API engine **221 PASS, 1 test corpus opt-in SKIP**; app pair **47 PASS**. Evidence/hashes/log: `D:/P5E-private/pronoun-priority-20261009-d9e29f6c/`. Kiểm prompt, scope, parser, gate, ghép, lưu/đọc bằng host test; không chứng minh model sẽ tuân thủ. Không APK/device/provider/commit/push. SEMANTIC_EVAL toàn tập vẫn NOT_MEASURED; 0/3 chương được chấp nhận, P7 chưa bắt đầu.

### 6.1 Bộ đối chứng xưng hô/completeness offline

- **Nguyên nhân cần xử lý:** core có mục tiêu sửa rộng; rule có mặt nhưng chưa được tuân thủ; bộ chấm tổng không đo đúng mục tiêu owner. Không sửa căn chunk hay nới structural gate khi chưa có lỗi tương ứng.
- **ENGINE ownership:** QualityCore/PairPromptBuilder/ReferenceProjector và test cùng tên. Giữ CP-IMPL-4 content-only, một EDIT/chunk, không L1/L2/L3, không NOTES/Change Map. Không suy speaker từ substring tên; chỉ scope/cue là tất định. Nếu cần danh sách ứng viên, dùng projection đã có, không lặp cả lời thoại thành checklist dài hoặc đưa expected answer vào prompt.
- **QA ownership:** scripts/chunk/ và test offline liên quan; annotation sách ở vùng riêng ngoài Git. Lập tối thiểu 10 tình huống: sai self, sai call, nhiều lần gọi trong cùng lượt, đảo chiều hai người, đổi người nói, vượt scope qua biên chunk, tên xuất hiện trong lời kể, mơ hồ/xung đột cần giữ, thiếu câu, ký tự hỏng/kana. Có bản sạch đối chứng. Giữ ca 007 đã thấy làm regression; chọn ví dụ 006 chỉ sau khi phân xử RAW, không dùng đếm từ để tự gán nhãn.
- **Đầu ra:** mỗi occurrence ghi source/range/hash, speaker/target nếu xác định được, scope, loại lỗi, các dạng sửa chấp nhận được, lý do ngắn và trạng thái UNADJUDICATED nếu chưa chắc. FINAL owner chỉ là reference ngoài runtime. Giữ nguyên scorer và điểm lịch sử; thêm số correct/missed/wrong cho address, số omissions restored/remaining/new cho completeness, số sửa ngoài mục tiêu đã phân xử. Không có mẫu phù hợp thì NOT_MEASURED, không ghi 0%.
- **Test/PASS:** mapping/scope/cue/conflict và unchanged round-trip PASS offline; scorer mới phân biệt sửa đúng, bỏ sót, sửa sai, xóa câu chứa lỗi (không được tính correct), no-op và negative control. Annotation chưa phân xử không vào mẫu số. Fake provider chỉ chứng minh kỹ thuật, không đạt semantic gate. App tiếp tục xuất TXT; host tự giữ diff/hash/cost, không bắt model tạo receipt.
- **APP ownership:** chỉ tích hợp metadata ứng viên nếu thực sự cần sau QA; không auto-replace tên/xưng hô, không thêm mandatory user approval mỗi câu. Cấu hình thiếu glossary/pronoun vẫn hợp lệ.
- **COORDINATOR:** review test từ source cô lập, bảo vệ old runs/holdout và pin đúng contract. OWNER chỉ cần phân xử ca RAW mơ hồ, không phải duyệt lại quyền offline đã có.
- **Điểm dừng/phụ thuộc:** chưa có annotation đủ tin thì chưa tuyên bố prompt mới tốt hơn; không live hàng loạt. Chỉ sau offline PASS mới đề xuất **1 chunk đã có lỗi xưng hô xác minh**, 1 paid call, không retry sau dispatch, trần đề xuất **USD 0.01** với giá/estimate thực phải nằm trong trần. Đây chưa phải quyền chi; lý do live duy nhất là kiểm model có sửa đúng ca mà offline không chứng minh được. Không chạy cả 006, không đụng holdout để chỉnh prompt; gặp FORMAT/UNKNOWN hoặc lỗi nghĩa mới thì dừng và replay offline.

#### Kết quả offline 2026-10-09

- Bộ chấm mới ở `scripts/chunk/adjudicated_quality.py`; ma trận tổng hợp QA ở `scripts/chunk/test_adjudicated_quality.py`. **9/9 test PASS.** Các fixture chỉ dùng dữ liệu tổng hợp có RAW anchor/hash; không chứa sách, FINAL hay expected answer trong runtime. Có kiểm hash neo, scope, speaker/target, repeated call, đảo chiều, đổi speaker, ngoài scope ở biên chunk, tên trong lời kể (không phải address), ambiguity/conflict (giữ DRAFT), clean/no-op/negative, xóa câu chứa lỗi, omission và reference-only leak. Thử thêm `final_similarity` vào input không làm thay đổi điểm.
- Đầu ra synthetic (đây là phép kiểm bộ chấm, không phải điểm model): address có 7 lỗi adjudicated — sửa đúng **4**, bỏ sót **2**, sửa sai **1**; 3 occurrence sạch giữ nguyên; 1 occurrence UNADJUDICATED bị loại. Completeness có 5 unit RAW-required — khôi phục **1**, còn thiếu **1**, lỗi thiếu mới **2**, giữ nguyên **1**; thêm 1 reference-only leak, 1 unsupported addition và bỏ 1 unit chưa adjudicated. Kiểm ngoài mục tiêu có 3 defect — sửa đúng **1**, bỏ sót **1**, sửa sai **1**; đồng thời phát hiện 1 lỗi mới và 1 thay đổi không có RAW support; clean control giữ nguyên. Các lỗi tổng hợp cố ý được cài để kiểm loại metric, không phải PASS chất lượng.
- Phân xử response thật chỉ dùng RAW + hàng PRONOUN trong prompt, không dùng FINAL similarity: chapter 007, chunk 005, phạm vi P064–P075. Sidecar riêng `D:\P5E-private\chunk-quality-20261009\adjudication-007-chunk5-cp3.json`; RAW SHA-256 `cb750dab75a11518ed610ebdb19ef0df8270e9c98653aabb7a8bb0b805393231`; response SHA-256 `78f06780f37dc3c021e1d4785adbe0f43e63cfb488ac6c76f24b92824e5a0f95`. Trong 2 lần gọi lại thuộc cùng một lượt thoại, output lịch sử CP-IMPL-3 sửa sai cả **2/2**; **0** sửa đúng, **0** bỏ sót vì cả hai đều bị đổi; direct vocative sạch **1/1** được giữ. Đây là phân loại đúng một lỗi đã xác nhận, không đại diện toàn chương và không đo mức tuân thủ CP-IMPL-4. Completeness của response thật **NOT_MEASURED** vì chưa có annotation RAW theo unit; các 27 dòng đổi ngoài phạm vi trước đây vẫn UNADJUDICATED. Không có REPORT/semantic acceptance nào được tạo.
- Regression từ archive sạch HEAD `a14272ab` + overlay chính xác 6 file (4 file CP-IMPL-4, scorer và tests): `:editorial-engine:test` **644 tests, 0 failures, 0 errors, 1 opt-in skip**; `:app:testDebugUnitTest` **457 tests, 0 failures, 0 errors, 1 opt-in skip**; Python **9/9**; `git diff --check` PASS. App pair-service regression xác nhận luồng vẫn một `EDIT` cho mỗi chunk. Archive, overlay manifest/hash và log ở `D:\P5E-private\chunk-quality-20261009\`; lần chạy đầu chỉ thiếu biến môi trường SDK, lần chạy lại với SDK cục bộ đặt tạm trong process đạt. Không build APK, không emulator/device, không provider, không phát sinh chi phí.

#### Một đề xuất live — chưa được cấp quyền

Chỉ đề xuất **chapter 007 / chunk 005 / RAW P064–P075**, vì đây là chunk có hai lần gọi xưng hô sai đã được RAW phân xử. Giả thuyết đo: CP-IMPL-4 sửa đủ cả hai occurrence sang dạng gọi được quy định, giữ direct vocative sạch và không làm mất/thêm nội dung. Gửi đúng **một** `EDIT`, không retry; chấm riêng correct/missed/wrong/new cho address, omission restored/remaining/new và các collateral change theo RAW. Dừng ngay nếu FORMAT, UNKNOWN, lỗi nghĩa mới, estimate không rõ hoặc vượt trần.

Chi phí cùng chunk đã quyết toán ở lần CP-IMPL-3 là **USD 0.00310338** (mốc lịch sử, không phải báo giá chắc chắn cho prompt CP-IMPL-4). Đề xuất tạo sổ mới với **trần đúng một call USD 0.01**; dừng trước dispatch nếu preflight/estimate hiện hành không xác nhận được giá nằm trong trần. Số dư C1 cũ `USD 0.00955570` thấp hơn trần này và không được tái dùng làm quyền chi. Đề xuất này không phải cấp phép chạy; CP-IMPL-4 chưa build/cài và chưa được kiểm trên thiết bị.

Các bullet dưới đây giữ số đo C1 trước review, không còn là next action hiện hành:

- D-C1 đã duyệt (owner, chat 2026-10-09): live chỉ chương 007, luna reasoning medium, trần USD 0.05 — **đã dùng ba lần** (lần 2 và 3 do owner duyệt riêng): lần 1 và 2 dừng ở đoạn 5 (`ENVELOPE/FORMAT`, thẻ đóng `</EDIT>`); **lần 3 chạy đủ 7/7 đoạn**, không chunk nào bị chặn; sổ `C1-20261009` USD 0.0404443 / 17 call, còn USD 0.0095557. Không có quyền chạy thêm.
- C1.1–C1.4 xong và có evidence ở mục 7 (engine CS-1, nguồn hai file, UI gọn, runner `CHUNK`, sửa hẹp thẻ đóng `57978e83`, build `4.18-c1.6`/253 trên `emulator-5554`). Điều kiện PASS C1 đạt về con số (không chặn, 0 chữ Nhật thêm, 4/18 > 3/18) nhưng chênh một dòng, sửa thừa 27 dòng và giống FINAL 0.9216 < DRAFT 0.9882: **chờ owner đọc trang đọc** `D:\P5E-private\chunk-outputs\007\c1-007-third-review.html`.
- 0/3 chương được owner chấp nhận; P7 chưa bắt đầu; U1 tạm hoãn.
- CP-IMPL-2 (chunk không ghi chú, dừng ở chunk lỗi đầu tiên) đã commit `90e9a6f7`; CP-IMPL-3 (`4acc50fd`) nêu đúng thẻ đóng và ghi mã cổng từng chunk; `57978e83` đọc `</EDIT>` cuối câu như thẻ đóng kèm cảnh báo.
- Chi tiêu: ledger Q2 USD 1.17703995 (trần D-Q2b USD 10.00) + ledger C1 USD 0.0404443 (trần USD 0.05).

### 6.2 Review độc lập commit 0e0ebd8c — chuẩn bị một chunk trước live

- Baseline thực: `0e0ebd8cae5699402606c3db1a303e7c2748216c`, branch hiện tại; local ahead upstream 1 commit theo ref đã lưu (không fetch/push). Sáu hash overlay khớp manifest; source test tương đương HEAD (chuẩn hóa LF/CRLF). XML xác nhận engine 644 tổng/1 skip, app 457 tổng/1 skip, 0 failures/errors. Không lặp Gradle vì không đổi Java/app. Diff commit không sửa căn/cắt/ghép CS-1, store hay runner.
- Annotation SHA `88803ad09f3f6ee04d78c4847144695f8c2e5feed73e5c7036da97e92d17a95c`; prompt và response hash khớp file gốc. Phân loại hai occurrence sai + một vocative sạch có căn cứ; đây là một tình huống thoại được đo, không phải hai mẫu độc lập hay điểm toàn chương. Completeness vẫn NOT_MEASURED.
- **Lỗi bộ chấm đã sửa nhỏ, chưa commit:** input trống/toàn UNADJUDICATED từng trả SCORED/NO_ADJUDICATED_ISSUES; nay NOT_MEASURED. Đối chứng chỉ có đoạn sạch hoặc reference leak nay MEASURED dù mẫu số lỗi cần sửa bằng 0. Kiểu JSON sai của anchor/state/role trả AnnotationError; candidate không đổi không được đồng thời đổi trạng thái lỗi để nhận điểm sửa đúng. Không đổi cách chấm similarity hay ngưỡng app.
- Tái hiện trên archive sạch `0e0ebd8c` của scripts/chunk + test mới: 13 test trước sửa FAIL; sau overlay scorer **13/13 PASS**. Replay annotation thật vẫn 0 correct/0 missed/2 wrong, 1 clean retained, completeness NOT_MEASURED. Evidence/log/source ZIP ở `D:/P5E-private/chunk-qa-review-0e0ebd8c/`; dữ liệu sách không vào Git. Patch chỉ QA offline, không cần build APK để kiểm.
- **Chưa đủ điều kiện dispatch ngay:** `EditorialApiV1FixtureRunnerInstrumentedTest.runChunkFixture` hiện prepare toàn bộ `load.source` rồi `service.execute` mọi pair (khoảng dòng 395–400); không có selector chunk 005. Cap USD 0.01 không thay cho giới hạn call và không đảm bảo gọi đúng chunk. Không chạy lại chunk 1–4 để tới chunk 5; không cắt RAW thành file mới rồi làm P064 thành P001.

**Việc tiếp theo duy nhất — chuẩn bị canary offline (chưa live):**

| Ownership | Đầu ra và kiểm chứng bắt buộc |
|---|---|
| APP/runner | Thêm đường thử chọn một pair bằng stable ID/ordinal kèm map/source hashes; giữ toàn bộ snapshot để lấy đúng RAW/DRAFT range, context, glossary, pronoun và số đoạn toàn chương. Hard max dispatch=1 ngay ở provider wrapper, không retry kể cả lỗi/UNKNOWN; sai selection/hash từ chối trước call. Không đổi hành vi luồng sản phẩm nhiều chunk. |
| QA | Fake-provider ghi nhận đúng một request cho chunk 005, không request chunk khác; so range/context/reference với baseline, cho phép khác duy nhất core CP-IMPL-4. Có test selector sai, scope partial, UNKNOWN, response lỗi và lần gọi thứ hai bị từ chối. Output ghi single-chunk experiment, không FINAL toàn chương. |
| QA nội dung | Giữ hai occurrence lỗi và một vocative sạch làm target/negative control; bổ sung danh sách nội dung RAW-required của **toàn chunk 005** để kiểm mất/thêm nội dung và phân xử mọi diff mới. Không đưa annotation/FINAL vào runtime prompt. Không cần phân xử cả 27 dòng toàn chương để chạy phép thử này. |
| COORDINATOR | Kiểm trên source sạch, ghi đúng prompt/model/route/reasoning, source/output hashes, output cap và reservation worst-case. Chỉ sau offline PASS mới lập build qua wrapper và đề xuất cài emulator cụ thể; không tự cài hay gọi provider từ gói review. |

Tiêu chí phép thử sau này: 2/2 occurrence sửa đúng, vocative sạch giữ, không mất/thêm đơn vị nội dung hoặc sinh lỗi xưng hô/ký tự mới trong chunk; chỉ structural PASS không đủ. Nếu chưa phân xử đủ content thì semantic NOT_MEASURED. Giả thuyết live duy nhất: CP-IMPL-4 có cải thiện ca tuân thủ pronoun đã thất bại hay không; offline không chứng minh được hành vi model. Dự kiến model/medium như C1 để không đổi hai biến cùng lúc, **1 call, trần mới USD 0.01**, giá/estimate thực phải vừa trần. Đây là đề xuất chưa được cấp quyền, không dùng số dư C1 cũ; không mở rộng sang cả chương hay holdout.

#### Kết quả chuẩn bị offline phép thử chunk 005 (2026-10-09)

- **Đã làm (APP/runner):** `EditorialSingleChunkProbe` — bộ chọn `Selector` bắt buộc đủ 7 trường (ordinal, pair id, map hash, đoạn đầu/cuối, hash RAW range, hash DRAFT range); `resolve` từ chối trước mọi reservation/call với mã `SELECTOR_*` (thiếu trường, không phải run chunk, đổi contract, lệch map hash, ordinal không duy nhất, lệch pair id, thiếu DRAFT, lệch khoảng đoạn, lệch hash RAW/DRAFT); `OneCallProvider` chỉ cho đúng một request đi tới provider, các request sau bị từ chối không chạm provider. `EditorialPairRunService.executeOnly` gửi đúng pair được chọn, **không retry** (một lần thử), từ chối pair không còn ở trạng thái chưa gửi; `execute` của luồng sản phẩm không đổi (vẫn gửi mọi chunk, thử lại một lần lỗi rõ ràng). Runner `runChunkFixture` nhận `p6_only_*`, ghi `mode=CHUNK_SINGLE`, `single-chunk.json`, `chunk-candidate.txt`, `chunk-response.txt`; không phải FINAL toàn chương. Script riêng ngoài Git `run_c1_chunk.ps1` có thêm `-SelectorFile` (đã kiểm cú pháp, chưa chạy).
- **Test host (không provider):** `EditorialSingleChunkProbeTest` 6 test: chỉ chunk được chọn tới provider và 6 chunk còn lại giữ `IMPORTED`; request đơn **bằng từng ký tự** request chunk đó của run đầy đủ (system gồm core, glossary, hàng pronoun một phần `[áp dụng đoạn …]`, nhãn đoạn, hợp đồng đầu ra; user gồm RAW/DRAFT và context reference-only; `maxOutputTokens` giống); lỗi HTTP, unknown outcome và câu trả lời không thẻ đều chỉ có 1 request và gọi lại bị từ chối; wrapper từ chối mọi request thứ hai; 9 selector sai + thiếu + run toàn chương + pair không thuộc run đều bị từ chối với 0 request, 0 reservation; luồng đầy đủ vẫn gửi mọi chunk và retry lỗi rõ ràng một lần.
- **Test trên dữ liệu 007 thật (opt-in `EditorialSingleChunk007OfflineTest`, 0 USD):** kế hoạch cắt trên host **trùng từng byte** `chunk-plan.json` do thiết bị cắt ở run `f0d387b5…` (7 chunk, phán định OK). Chunk 005 = RAW P061–P079 (19 đoạn), hàng pronoun `吉岡さん` chỉ áp dụng P064–P075 nên là hàng một phần. Với fake provider qua `OneCallProvider`: đúng 1 request, 1 reservation, 6 chunk còn lại chưa gửi. So với prompt thực của lần chạy CP-IMPL-3: phần user giống từng ký tự; từ `# GLOSSARY` đến hết hợp đồng đầu ra giống từng ký tự; chỉ khác phần core (CP-IMPL-4) và đúng một câu bố cục trong CHUNK CONTRACT. Giá trị chọn nằm ở `D:\P5E-private\chunk-quality-20261009\single-chunk-005\selector-chunk5.json` (map hash `c66df054…0006`, pair id `c40bc6ba…`, hash RAW range `b00402f9…`, DRAFT range `2bf9e6b8…`, ước tính vào 2 948 token, trần ra 4 096).
- **Đối chứng completeness toàn chunk:** `scripts/chunk/chunk_completeness.py` + `test_chunk_completeness.py` (10 test tổng hợp, không chứa văn bản sách). RAW-required = 19 đoạn của khối "RAW (this part)", mỗi đoạn khớp một dòng DRAFT; dòng giống DRAFT được xét theo đồng nhất, dòng đổi/mơ hồ/thêm **phải có nhãn người phân xử**, nếu không kết quả là `PARTIAL` chứ không được tính là giữ nguyên; dòng bị xóa là thiếu cơ học. Áp dụng cho chunk 005 (nhãn do reviewer đọc RAW và bản trả về, ngoài Git ở `D:\P5E-private\chunk-quality-20261009\completeness-chunk5-controls.json`): bản DRAFT nguyên (no-op) giữ 19/19; **bản CP-IMPL-3 lịch sử giữ 19/19, 0 thiếu mới** (13 dòng đổi chữ, đều còn đủ nội dung — nên lỗi của lần đó là xưng hô và sửa ngoài ý, không phải mất nội dung); đối chứng âm: xóa đoạn P073 khi chưa có nhãn → `PARTIAL`, có nhãn → 1 thiếu mới; thêm một dòng khi chưa nhãn → `PARTIAL`, có nhãn → 1 thêm không có căn cứ. Nhãn này do reviewer là Claude gán, owner có thể phân xử lại.
- **Regression (cây làm việc, chưa phải archive sạch):** `:editorial-engine:test` 644 test, 1 skip, 0 lỗi; `:app:testDebugUnitTest` 464 test, 2 skip (opt-in), 0 lỗi (+7 test mới); `:app:compileDebugAndroidTestJavaWithJavac` PASS; Python `scripts/chunk` 24/24 và `scripts/p6` 93/93; `git diff --check` PASS. Gói này cũng đưa vào commit các sửa nhỏ scorer (chưa commit) của review §6.2 để control dùng đúng ngữ nghĩa NOT_MEASURED/PARTIAL. **Không build APK, không cài thiết bị, không gọi provider, USD 0.** CP-IMPL-4 vẫn chưa build; mọi kết luận về hành vi model còn NOT_MEASURED.

#### Đề xuất live (chưa được cấp quyền)

Một phép thử: chương 007, chunk 005, RAW P061–P079, model `openai/gpt-5.6-luna` mức medium như C1 (không đổi hai biến cùng lúc), contract CP-IMPL-4. Giả thuyết duy nhất: core mới có làm model sửa đủ cả hai chỗ gọi `Yoshioka` ở P064 (trước đây bị đổi thành "cô"), giữ đúng lời gọi sạch ở P075 và không mất/thêm nội dung hay xưng hô ngoài phạm vi (P065, P076) hay không.

Điều kiện chạy (chỉ sau khi owner duyệt): (1) build `4.18-c1.7` qua `scripts/build-and-save.ps1` từ commit của gói này, AndroidTest kèm theo, cài chỉ `emulator-5554`; (2) chạy trước trên thiết bị với fake provider và `-SelectorFile` để chứng minh selector chạy thật (0 USD); (3) gọi live đúng **1 request**, **không retry**, ledger mới `C1-SINGLE5-20261009`, trần **USD 0.01**. Giữ giá: lượng giữ chỗ xấu nhất = 2 948 × USD 0.0000002 + 4 096 × USD 0.0000012 = **USD 0.0055048** (trong trần); lượng đã quyết toán của chunk này ở CP-IMPL-3 là USD 0.00310338. Dừng ngay (không gọi lại) nếu selector bị từ chối, estimate không rõ hoặc vượt trần, FORMAT, UNKNOWN, lỗi nghĩa mới. Sau đó chấm: address (đúng/sót/sai), completeness bằng `chunk_completeness.py` với nhãn phân xử, và mọi diff còn lại; 007 không được dùng để tuyên bố điểm toàn chương, không chạm holdout, không chạy chương khác. Số dư ledger C1 cũ USD 0.0095557 không phải quyền chi.

#### CP-IMPL-5 — khối ADDRESS CHECK (offline, 2026-10-09)

Owner duyệt thiết kế offline sau khi lần live CP-IMPL-4 trả bản giống DRAFT và bỏ sót cả hai chỗ gọi `cậu`.

- **Thiết kế (`AddressChecklist`, engine, thuần):** với mỗi hàng pronoun có `call` trong request của một chunk, app liệt kê: (1) các đoạn RAW nằm trong phạm vi hàng và có lời thoại (「 hoặc 『); (2) trong số đó, đoạn nào mà đoạn DRAFT cùng vị trí dùng từ xưng hô (danh sách cố định: cậu, bạn, anh, chị, em, cô, ông, bà, mày, ngươi, ngài, chú, bác, thầy, tớ, tao, ta, tui, tôi) **khác `self`/`call` của hàng**, kèm số lần. Chỉ nhãn đoạn và từ; không chép câu nào, không nói dạng đúng ngoài các trường của chính hàng, không suy người nói/người nghe (việc đó vẫn do model đọc RAW). Ghép đoạn theo vị trí: số đoạn RAW và DRAFT phải bằng nhau, hoặc nếu DRAFT dính hai đoạn không có dòng trống thì ghép theo dòng khi mọi đoạn RAW là một dòng; ngoài hai trường hợp đó không sinh khối (đóng an toàn). Khối chỉ xuất hiện khi có ít nhất một từ cần xem, nằm sau các hàng pronoun và trước OUTPUT CONTRACT, kèm một câu hướng dẫn: chỉ dùng `call`/`self` của hàng khi RAW cho thấy người nói và người nghe đúng là speaker/target của hàng, nếu RAW chỉ người khác hoặc không rõ thì giữ từ đó. Chỉ `buildPair`; request toàn chương không đổi. Contract đổi sang **CP-IMPL-5** (run cũ không tự tiếp tục).
- **Test:** `AddressChecklistTest` 7 test (chỉ đoạn có lời thoại trong phạm vi và có từ khác dạng của hàng; lời kể, đoạn đã dùng đúng dạng và đoạn ngoài phạm vi không được liệt kê; không chứa câu sách; hàng không có `call` hoặc `call` trùng từ DRAFT đã dùng thì không có khối; số đoạn không khớp thì không khối; DRAFT dính hai đoạn vẫn ghép theo dòng; toàn chương không đổi; tách từ NFC theo từ nguyên vẹn).
- **Chunk 005 của 007 (selector và dữ liệu thật):** kế hoạch cắt, map hash, khoảng đoạn P061–P079, hash RAW/DRAFT range và phần user message **không đổi** so với CP-IMPL-4; so 7 request của một lần chạy fake toàn chương giữa CP-IMPL-4 và CP-IMPL-5: chunk 1–4, 6, 7 **trùng từng byte**, chỉ chunk 005 khác, và khác đúng một khối: `Row 吉岡さん (Kuragane Erika -> Yoshioka; self "tôi", call "Yoshioka"): quoted RAW paragraphs in scope P064, P068, P075; DRAFT address words other than the row's self/call: P064 "cậu" x2` cùng câu hướng dẫn. P075 (lời gọi sạch), P065 và P076 (lời kể) không bị liệt kê. Ước tính đầu vào 3 153 token (trước 2 948); giữ chỗ xấu nhất = 3 153 × USD 0.0000002 + 4 096 × USD 0.0000012 = **USD 0.0055458**.
- **Kiểm trên 8 chương có sẵn (004–008, 011, 014, 017; chỉ số thô, 0 USD, `D:\P5E-private\chunk-quality-20261009\address-check-corpus.json`):** khối xuất hiện ở 63/85 chunk; tổng 344 đoạn được liệt kê, trong đó FINAL của owner có đổi dòng ở 112 đoạn (33%; nền chung của dòng bị owner đổi thấp hơn nhiều, nhưng một dòng bị đổi có thể vì lý do khác nên đây không phải độ chính xác xưng hô); trong 140 dòng owner đổi có đụng từ xưng hô, 96 (69%) nằm trong đoạn được liệt kê, riêng 006/011/014/017 là 84 trên 89 (94%), còn 004 chỉ 4/38 (hàng đối xứng hai chiều, nhiều dòng đổi không liên quan xưng hô). 008 gần như chỉ là nhiễu (1/25, không có dòng xưng hô nào bị đổi). Hai hàng đối xứng (Erika→Rea và Rea→Erika) cùng liệt kê chung một từ; model phải tự phân biệt theo RAW. Đây là đo độ phủ và nhiễu của khối, không đo hành vi model.
- **Regression (cây làm việc, không phải archive sạch):** `:editorial-engine:test` 651 test, 1 skip, 0 lỗi; `:app:testDebugUnitTest` 465 test, 3 skip (opt-in), 0 lỗi; androidTest biên dịch PASS; Python `scripts/chunk` 24/24, `scripts/p6` 93/93; `git diff --check` PASS. **Không build APK, không cài, không gọi provider, USD 0.** APK vẫn `4.18-c1.7`/254 (CP-IMPL-4); hành vi model với CP-IMPL-5 NOT_MEASURED.

#### Đề xuất live lần hai (chưa được cấp quyền)

Cùng một phép thử: chương 007, chunk 005 (RAW P061–P079), `openai/gpt-5.6-luna` medium, đúng 1 call, không retry, selector và bộ chấm như lần trước, nhưng contract CP-IMPL-5. Giả thuyết: khi app chỉ ra đoạn P064 và từ `cậu` x2 thuộc phạm vi hàng, model sửa cả hai sang `Yoshioka`, giữ P075, không đụng P065/P076. Điều kiện: build `4.18-c1.8` qua wrapper từ commit gói này + AndroidTest, cài chỉ `emulator-5554`, chạy thử fake với `-SelectorFile` (0 USD) và so prompt thiết bị với prompt host, rồi gọi live với ledger mới `C1-SINGLE5B-20261009`, trần **USD 0.01** (giữ chỗ xấu nhất USD 0.0055458; lần CP-IMPL-4 chạy thực tế USD 0.00201225). Dừng nếu selector bị từ chối, giá không rõ hoặc vượt trần, FORMAT, UNKNOWN, hay lỗi nghĩa mới. Ghi nhận trước: một lần thử chỉ nói được là khối có giúp chunk này hay không, không phải tỉ lệ; nếu vẫn giữ nguyên DRAFT thì nguyên nhân nằm ở hành vi model/mức suy luận chứ không ở việc thiếu chỉ dẫn vị trí. Số dư ledger cũ không phải quyền chi.

## 7. Nhật ký

- 2026-10-09 — QA/coordinator: hoàn tất §6.1 offline. Thêm scorer RAW-adjudicated + synthetic controls (9/9), phân xử riêng 007 chunk 005 (2 wrong fixes, direct vocative sạch được giữ; response CP-IMPL-3, completeness thật NOT_MEASURED), chạy từ archive sạch HEAD + overlay 6 file: engine 644/1 opt-in skip, app 457/1 opt-in skip. Ghi đúng một đề xuất live ở §6.1, chưa cấp quyền. Commit gói offline này; không push/build/device/provider; 0 call/USD 0 và giữ toàn bộ dirty work ngoài scope.
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
- 2026-10-09 — C1.5 live chương 007 (D-C1), chạy 1 lần, **dừng ở đoạn 5/7; PASS C1 chưa đạt**. Kiểm trước: phán định OK, 7 chunk, ước tính worst-case 7 × ≈USD 0.0054 (đoạn đầu ra tối thiểu 4 096 token) ≈ USD 0.038 ≤ 0.05; tài khoản khớp; sổ mới `C1-20261009` trần USD 0.05. Chạy: `openai/gpt-5.6-luna` medium, build `4.18-c1.4`/251 nguồn `56399f1d`, `emulator-5554`, run `0d6a6acb-d807-4151-9b87-ece80bd45c9d`, Performance mặc định (token, cứng 675, mềm 540, MAX OUTPUT 4096, CONTEXT 400). Kết quả: đoạn 1–4 `ACCEPTED`, **đoạn 5 `STRUCTURE_BLOCKED`** (bản trả về đóng bằng `</EDIT>` thay vì `</EDITED>`; chẩn đoán ban đầu "chép câu RAW" đã được đính chính ở mục chạy lại bên dưới), app dừng đúng luật: đoạn 6–7 không gửi, không gửi lại đoạn nào. 5 lượt gọi, 14 415 vào / 6 458 ra token, **USD 0.0113526**, 0 UNKNOWN, 0 vượt giữ chỗ, 0 pending; sổ: 5 call, còn USD 0.0386474 (SHA-256 `f3550491…99ac`). Kế hoạch 007: 104 cặp 1–1, không tách/gộp/thiếu dòng. Chấm bản tạm (đoạn 1–4 đã sửa, phần còn lại giữ DRAFT) với `score_vs_final.py`: owner sửa 18 dòng, **app sửa trúng/gần hơn 3/18, gần đúng 3** (mốc luna toàn chương 3 · v5 8 · Sol 13), 16 dòng xa FINAL hơn, 13 dòng owner không sửa mà app đổi, 0 dòng sinh chữ Nhật trong bản tạm (đoạn chặn bị loại), độ giống FINAL cả chương 0.9696 so với DRAFT 0.9882; chỉ 19 trên 104 dòng bị đổi, toàn bộ nằm ở đoạn 1–4 (dòng 1–49). Vì chỉ sửa được 4/7 đoạn, 3/18 không so được trực tiếp với các mốc toàn chương. **Chưa đạt:** có một chunk bị chặn cấu trúc và chưa vượt 3/18. Hạn chế nêu rõ: runner lúc đó chưa ghi mã cổng cấu trúc của đoạn bị chặn (chỉ ghi trạng thái); đã sửa ở `4acc50fd`. Không chạy lại (D-C1 chỉ cho một lần); không chạy chương nào khác. Bản app (tạm), trang đọc RAW/DRAFT/app/FINAL: `D:\P5E-private\chunk-outputs\007\` (`007_APP_chunk-luna_PARTIAL_JAKUAKU_MONSTER_VOL1.txt` SHA-256 `26d77ae9…c655`, `c1-007-review.html` SHA-256 `a0749e6c…b5d`). **Next action duy nhất:** owner xem trang đọc và quyết định có cho thêm một lần chạy 007 sau khi sửa phần đang nghi (câu lệnh cho model khi dòng RAW bị chép nguyên văn; ghi mã cổng) hay đổi hướng; không chạy live thêm trước quyết định đó.
- 2026-10-09 — C1.5 live 007 chạy lại (owner duyệt "duyệt chạy lại"), **vẫn dừng ở đoạn 5/7; PASS C1 chưa đạt**. Sửa trước khi chạy: commit `4acc50fd` (CP-IMPL-3): hợp đồng đầu ra nêu đúng thẻ đóng `</EDITED>` và cấm chép câu RAW vào bản trả về; runner ghi mã cổng cấu trúc cho từng chunk. Build `4.18-c1.5`/252 nguồn `4acc50fd`, event `build-20261009-181748`, APK SHA-256 `5836DF2B…D4D1`, mirror `artifacts/` + `backup/`, chỉ cài `emulator-5554`; test thiết bị tập trung PASS. Chạy: run `ca731da4-aedc-4bf7-b50b-f47d2b15ed97`, cùng cấu hình (luna medium, Performance mặc định, sổ `C1-20261009` trần USD 0.05). Kết quả: đoạn 1–4 `ACCEPTED` (cổng PASS), **đoạn 5 `STRUCTURE_BLOCKED`, mã `ENVELOPE/FORMAT` ("not a usable EDITED answer")**: câu trả lời có `<EDITED>` nhưng đóng bằng `</EDIT>` thay vì `</EDITED>`; đoạn 6–7 không gửi, không gửi lại đoạn nào. 5 lượt gọi, 14 645 vào / 9 444 ra token, **USD 0.01499330**; sổ sau lần này: 10 call, **USD 0.0263459**, 0 pending, 0 UNKNOWN (`verify_spend_ledger.py` OK, hash cuối `5381680b…0336`), còn USD 0.0236541. **Đính chính chẩn đoán lần 1:** mục lần 1 nói đoạn 5 bị chặn vì bản trả về chép câu Nhật và chưa có mã cổng. Phát lại cổng ngoại tuyến (USD 0) trên câu trả lời đã lưu của lần 1 cho thấy mã chặn cũng là `ENVELOPE/FORMAT` với thẻ đóng `</EDIT>`; việc chép câu RAW không phải mã chặn. Lần 2 (hợp đồng mới) câu trả lời không còn mở đầu bằng câu RAW nhưng vẫn đóng sai thẻ cùng kiểu. **Phát lại ngoại tuyến (USD 0, `D:\P5E-private\chunk-runs\C1\gate-replay-closing-tag.txt`):** nếu thẻ đóng là `</EDITED>`, cổng cho cả hai câu trả lời đoạn 5 là WARN, không BLOCK (lần 1: `LAYOUT/BLANK_RUN_CHANGED`, 1212→1157 chữ; lần 2: `BOUNDARY/BOUNDARY_WS_TRIMMED`, 1212→1212 chữ; cả hai 19→19 dòng). Nội dung đoạn 5 dùng được; chỉ thẻ đóng sai làm chạy dừng cả hai lần. Chấm bản tạm (đoạn 1–4 đã sửa, đoạn 5–7 giữ DRAFT) bằng `score_vs_final.py`/`min_gate_413.py`: owner sửa 18 dòng, **app sửa trúng/gần hơn 4/18 (22.2%), gần đúng 3** (mốc luna toàn chương 3 · v5 8 · Sol 13), 14 dòng xa FINAL hơn (4 trong số dòng owner đã sửa), 10 dòng owner không sửa mà app đổi, 0 dòng thêm chữ Nhật, độ giống FINAL cả chương 0.9747 so với DRAFT 0.9882; 13 vùng đổi so với DRAFT, đều ở đoạn 1–4. Số dòng còn chữ Nhật 5 bằng DRAFT (đoạn 5–7 chưa sửa), FINAL 3. Cổng tối thiểu 4.1.3: chưa đạt (c2, c3, c4 sai) và chưa so được trực tiếp vì mới sửa 4/7 đoạn. Không chạy thêm (lần duyệt này đã dùng hết); không chạy chương nào khác. Bản app (tạm) và trang đọc: `D:\P5E-private\chunk-outputs\007\007_APP_chunk-luna_RERUN_PARTIAL_JAKUAKU_MONSTER_VOL1.txt` SHA-256 `1565aa85…22f8`, `c1-007-rerun-review.html` SHA-256 `1996e4ae…bc1f`. **Next action duy nhất:** owner xem `D:\P5E-private\chunk-outputs\007\c1-007-rerun-review.html` và quyết định có cho một lần chạy 007 thứ ba sau khi sửa hẹp (bộ phân tích chấp nhận thẻ đóng sai kiểu `</EDIT>` ở cuối câu trả lời, vẫn ghi cảnh báo; ước tính ≈ USD 0.015, sổ còn USD 0.0236541) hay đổi hướng; không chạy live thêm trước quyết định đó.
- 2026-10-09 — C1.5 live 007 lần 3 (owner duyệt "duyệt" sau đề xuất sửa hẹp), **chạy đủ 7/7 đoạn, không đoạn nào bị chặn**. Sửa hẹp: commit `57978e83`: trong chạy chunk, đúng một `</EDIT>` ở cuối câu trả lời (không có `</EDITED>`, không có thẻ mở lồng, sau nó chỉ còn khoảng trắng) được đọc như thẻ đóng; cổng luôn thêm cảnh báo `CLOSE_TAG_REPAIRED` (không bao giờ PASS). Chỉ `EditorialPairRunService` bật chế độ này; luồng toàn chương và bộ phân tích mặc định không đổi. Test host trên cây làm việc: engine 644 (1 off), app 457 (1 off), mới: 2 test bộ phân tích, 1 test cổng, 1 test dịch vụ (chunk đóng sai thẻ thành WARN_REVIEW, chạy tiếp, bản ghép đủ kèm cảnh báo). Không chạy lại archive sạch cho commit này. Build `4.18-c1.6`/253 nguồn `57978e83`, event `build-20261009-183051`, APK SHA-256 `10E44370…88D7`, mirror `artifacts/` + `backup/`; AndroidTest `c1-chunk-20261009f`; chỉ cài `emulator-5554`; test thiết bị tập trung 8 nhóm PASS (pair store 6, pair UI 6, API store 6, flow 3, UI 5, process death 3), sổ Q2 không đổi; fake replay 007: 7 chunk, `final.txt` == DRAFT từng byte. Chạy: run `f0d387b5-52bb-4eb0-98d0-f7717b828ef1`, cùng cấu hình (luna medium, Performance mặc định, sổ `C1-20261009`). Kết quả: `FINAL_ELIGIBLE`, 7 lượt gọi (mỗi chunk 1 lần, 7 × finish `stop`), 19 227 vào / 10 548 ra token, **USD 0.01409840**. Cổng: đoạn 1, 2, 4, 6, 7 PASS; đoạn 3 và 5 `WARN_REVIEW` chỉ với `BOUNDARY_WS_TRIMMED` (khoảng trắng ở mép); **không có `CLOSE_TAG_REPAIRED`**: cả 7 câu trả lời đều đóng đúng `</EDITED>`, nên phần sửa hẹp không được dùng trong lần chạy này (lần 3 không tái hiện lỗi đóng sai thẻ ở đoạn 5; lỗi này gặp 2/2 lần trước). Sổ: 17 call, **USD 0.0404443** (trần 0.05), 0 pending, 0 UNKNOWN (`verify_spend_ledger.py` OK, hash cuối `42184ab9…b6e2`), còn USD 0.0095557. Chấm toàn chương (7/7 đoạn) bằng `score_vs_final.py`/`min_gate_413.py`: owner sửa 18 dòng, **app sửa trúng/gần hơn 4/18 (22.2%), gần đúng 3** (mốc luna toàn chương 3 · v5 8 · Sol 13); **27 dòng owner không sửa mà app đổi**, 34 dòng xa FINAL hơn (7 trong số dòng owner đã sửa), 0 dòng thêm chữ Nhật, chữ Nhật còn lại 4 dòng (DRAFT 5, FINAL 3), độ giống FINAL cả chương **0.9216 so với DRAFT 0.9882** (thấp hơn DRAFT). Cổng tối thiểu 4.1.3: chưa đạt (c2, c3, c4 sai). **Đối chiếu PASS C1 (mục 5.3):** không chunk nào bị chặn cấu trúc ✓; 0 dòng sinh chữ Nhật ✓; sửa trúng 4/18 > 3/18 ✓ về con số nhưng chênh một dòng nên "rõ hơn" không chắc; chất lượng (sửa thừa nhiều, giống FINAL kém DRAFT) do owner đọc quyết định. Semantic NOT_MEASURED; 0/3 chương được chấp nhận; P7 chưa bắt đầu. Không chạy thêm; không chạy chương nào khác. Bản app và trang đọc: `D:\P5E-private\chunk-outputs\007\007_APP_chunk-luna_THIRD_JAKUAKU_MONSTER_VOL1.txt` SHA-256 `59f4a4a0…ff43`, `c1-007-third-review.html` SHA-256 `327f41a5…`. **Next action duy nhất:** owner xem `D:\P5E-private\chunk-outputs\007\c1-007-third-review.html` và quyết định hướng tiếp theo (sổ C1 chỉ còn USD 0.0095557, không đủ cho một lần chạy 007 nữa; mọi lần chạy live thêm cần trần mới): bản 7/7 đoạn đã đủ chương nhưng sửa thừa nhiều (27 dòng owner không sửa) và giống FINAL kém DRAFT, nên cần owner đọc để quyết định có chấp nhận hướng chunk, siết lệnh để bớt sửa thừa, hay đổi hướng; không chạy live thêm trước quyết định đó.
- 2026-10-09 — Phép thử live single-chunk (owner duyệt "duyệt, build và chạy thử"): chương 007, chunk 005 (RAW P061–P079), CP-IMPL-4, `openai/gpt-5.6-luna` medium, **1 call**. Build `4.18-c1.7`/254 nguồn `ff819dde`, event `build-20261009-193025`, APK SHA-256 `51BEC145…7D40`, AndroidTest `c1-chunk-20261009g`, mirror `artifacts/` + `backup/`; emulator phải khởi động lại, cài chỉ `emulator-5554`. Test thiết bị tập trung PASS (pair store 6, pair UI 6, API store 6, flow 3, UI 5, process death 3; sổ Q2 không đổi). Chạy fake trên thiết bị với `-SelectorFile`: 1 request, 6 chunk còn lại chưa gửi, prompt trên thiết bị **trùng từng byte** prompt dựng trên host (SHA-256 `13843db5…66cb`). Live: run `b7f48241-d2a1-491b-8374-b36132a9a673`, sổ mới `C1-SINGLE5-20261009` trần USD 0.01; 1 call, finish `stop`, 3 216 vào / 1 007 ra token, **USD 0.00201225**, 0 pending/UNKNOWN (`verify_spend_ledger.py` OK, còn USD 0.00798775 — không phải quyền chi); không retry, chunk khác không gửi. Cổng: `WARN` mã `CLOSE_TAG_REPAIRED` — câu trả lời lại kết thúc bằng `</EDIT>` và phần sửa hẹp `57978e83` đã dùng thật lần đầu (lần chạy này sẽ bị chặn nếu không có nó); trạng thái chunk `WARN_REVIEW`, chạy `INCOMPLETE` (single-chunk, không phải FINAL). **Kết quả: bản trả về chunk 005 giống DRAFT từng dòng (19/19 dòng, 0 dòng đổi).** Xưng hô (annotation đã phân xử `adjudication-007-chunk5-cp4.json`): 2 chỗ lỗi `cậu` ở P064 **bỏ sót cả 2** (0 sửa đúng, 0 sửa sai), lời gọi sạch `Yoshioka` ở P075 giữ nguyên. Completeness (`chunk_completeness.py`): 19/19 đơn vị RAW giữ, 0 thiếu mới, 0 dòng đổi chưa phân xử; sửa ngoài mục tiêu 0. **Giả thuyết CP-IMPL-4 làm model sửa đúng hai chỗ gọi: không được xác nhận trong một lần thử.** Core mới hết sửa văn phong thừa (27 dòng trước → 0 dòng đổi ở chunk này) nhưng cũng không bắt được lỗi xưng hô mục tiêu. Nguyên nhân chưa biết; các giả thuyết chưa kiểm: hàng pronoun có `from=吉岡さん` còn RAW dùng `あなた` trong lượt thoại; quy tắc "mơ hồ thì giữ DRAFT" bị áp dụng quá thận trọng; mức suy luận medium. Một lần thử không đủ kết luận về tỉ lệ; không dùng làm điểm toàn chương. Evidence riêng: `D:\P5E-private\chunk-runs\C1\live\007-b7f48241-d2a1-491b-8374-b36132a9a673\`, điểm `D:\P5E-private\chunk-quality-20261009\score-cp4-single-chunk5.json`. **Next action duy nhất:** owner quyết định hướng sau kết quả chunk 005: cho phép thiết kế offline (không live) một khối ứng viên xưng hô tất định đưa vào request — liệt kê, trong lượt thoại thuộc phạm vi hàng pronoun, từng chỗ RAW/DRAFT mà hàng đó chi phối — rồi kiểm lại bằng cùng selector và bộ đối chứng; hoặc đổi hướng. Không chạy live thêm trước quyết định; sổ `C1-SINGLE5-20261009` còn USD 0.00798775 nhưng không phải quyền chi.
- 2026-10-09 — coordinator: kiểm archive sạch `e3f13f87` (engine 651, app 465, p6 93, chunk 24, androidTest compile PASS) và push 4 commit. Owner yêu cầu chốt hướng build hoàn thiện → chốt **sửa theo điểm app gắn cờ (CP-IMPL-6)**, mục 5.4: app tìm điểm xưng hô/thuật ngữ/kana/câu thiếu, model chỉ trả câu sửa cho từng điểm, app chỉ thay đúng các câu đó. Dừng các phép thử viết lại cả chunk; không chạy lần live thứ hai trên chunk 005.
- 2026-10-09 — owner duyệt D-C6 và chỉ rõ bản FINAL không phải luật tuyệt đối. Coordinator đổi cổng C6: phân xử từng điểm theo RAW/luật (ĐÚNG/SÓT/SAI/CHƯA CHẮC), so với FINAL chỉ để tham khảo, owner đọc là điều kiện quyết định. Trang đọc lần chạy thứ ba của 007 (27 dòng sửa thêm) có sẵn tại `D:P5E-privateunk-outputsq-007-third-review.html` để owner tự đánh giá.

#### C6.3 — đo offline 8 chương (2026-10-09)

- C6.1 engine đã được push ở `44dcfffe`; C6.2 app ở `0d581055`. Regression xác nhận chunk không có fix point nhận `NO_FIX_POINTS` và không phát request; chunk có điểm gọi một request và lưu kết quả từng điểm. Cập nhật detector: với RAW đã căn có lời thoại trong scope, không đòi DRAFT giữ dấu ngoặc thoại Nhật mới xét từ xưng hô; test mới bao quát trường hợp DRAFT bỏ dấu ngoặc, còn RAW kể chuyện và hàng ngoài scope vẫn không tạo điểm.
- Test đo private-corpus opt-in `EditorialPairC6CorpusMeasurementTest` chỉ in số, đọc 004–008/011/014/017 từ `D:\P5E-private\q2-inputs` và FINAL tham chiếu ngoài repo. Kết quả: **85 chunks / 64 chunk có điểm / 346 điểm** (305 ADDRESS, 25 GLOSSARY, 16 KANA, 0 MISSING). ADDRESS có 171 dòng owner sửa tham chiếu, 98 nằm trong điểm: **57.3%, dưới ngưỡng 69%**. GLOSSARY 16/18 (88.9%); KANA 10/10 (100%); điểm FINAL không đổi lần lượt 207/305, 9/25, 6/16 — chỉ là số tham khảo. Chapter 006 riêng: 15 chunks, 14 cần gọi, 74 điểm (65/5/4), 67,069 token vào và 8,801 token dự trữ đầu ra.
- Dự toán bảo thủ theo fallback giá của ứng dụng (không phải báo giá xác nhận cho luna) là **USD 0.555370 cho 006**, cao hơn trần C6 USD 0.03. Vì vậy **C6.3 chưa đạt** và C6.5 chưa đủ điều kiện; chưa dispatch provider, chi phí USD 0. Đo offline không dùng FINAL để gán nhãn semantic. C6.4 fake/build/emulator là bằng chứng kỹ thuật độc lập sẽ được ghi sau; không được xem là vượt cổng độ phủ/chi phí.
