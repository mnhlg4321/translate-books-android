# Editorial API V1 — review Q2 và yêu cầu Q2.5 (2026-10-08)

Coordinator: Claude, tiếp nhận sau khi phiên Codex hết quota. Baseline: HEAD `00101271` (đã push); APK `4.18-q2.1`/code 245 từ `45976adb`, chỉ `emulator-5554`. Việc dở được tiếp nhận: bản sửa bộ chấm ký hiệu chưa commit trong working tree.

## 1. Kiểm chứng

| Claim | Evidence coordinator tự kiểm | Kết luận |
|---|---|---|
| Q2.4 dev: 10 logical / 20 physical call, USD 0.8375159, 0 UNKNOWN | Theo ledger báo cáo; khớp số turn capture riêng (5 chương × 3 turn V5 + 5 E) | Chấp nhận theo evidence |
| E-strong (Sol, 1 lượt): improved 84/167, sửa thừa 165, 2 dòng thêm chữ Nhật | Đọc mẫu dòng ở file riêng | Đúng; xem mục 2 |
| V5-strong: 5/5 `V5_FINAL_MISSING`, "model không trả `<FINAL>`" | Đọc 3 response của chương 004: lượt 1 dừng `INPUT_GLOSSARY_SCHEMA_INVALID` (pack đòi 5 cột `source,target,category,note,priority`, app gửi bảng 4 cột đã lọc); lượt 2/3 dừng `INPUT_PREDECESSOR_MISSING` dây chuyền | **Không phải lỗi model — lỗi đầu vào của app** |
| Test Python | Toàn bộ `scripts/p6`: 76/77 PASS tại HEAD — `test_a_live_run_needs_one_actual_call_per_request` FAIL vì Q2.2 đổi câu báo lỗi; báo cáo Q2.2 chỉ chạy 14 test verifier | Đã sửa (mục 4); 77/77 PASS |
| Việc dở: bộ chấm ký hiệu | Bản cũ dùng biến `i` còn sót sau vòng lặp nên ghép dòng RAW sai; test mới FAIL trước sửa, PASS sau sửa | Hoàn tất và commit |

## 2. Đánh giá chất lượng

**Bước tiến thật:** chỉ đổi model từ luna sang GPT-5.6 Sol (model owner dùng), tỷ lệ chỗ owner sửa được app làm khớp hoặc gần hơn tăng từ ~7% (luna, chấm lại ở Q2.1: C1 15/217) lên **50% (84/167)**, trong đó 72 dòng gần như trùng FINAL. Năng lực model là đòn bẩy chính — luật prompt không phải.

**Chưa đạt:** Sol sửa thừa 165 dòng owner giữ nguyên và độ giống FINAL cả chương giảm (−0.037). Đọc mẫu 007 và 004: phần lớn là đổi cách diễn đạt khi câu đã đúng (vd "nắp tủ" → "cánh tủ"); một số là lỗi thật — hạ chữ hoa nhãn trạng thái (`【Cấp độ】` → `【cấp độ】`), đổi xưng hô "cậu" → "cô", đổi chú thích `《sinh lực》` → `《Hit Point》`, 2 dòng sinh thêm chữ Nhật.

**V5 (quy trình thật của owner): NOT_MEASURED.** Cả 5 chương dừng ở bước kiểm nguồn vì app không đưa file của owner nguyên dạng. Bằng chứng mạnh rằng pack 4.1.3 FULL chính là quy trình owner dùng: pack quy định đầu ra `[ID]_FINAL_QA_[SERIES].txt`, trùng tên 28 file FINAL (`001_FINAL_QA_JAKUAKU_MONSTER_VOL1.txt`).

Kết luận: **chưa có chương nào đạt; 0/3.** Nhưng lần đầu có hướng đo được là đúng (model của owner).

## 3. Vì sao phiên Q2 dừng, và phiên trước có tốt hơn không

| Vấn đề | Nguyên nhân | Ai chịu |
|---|---|---|
| V5 fail 5/5 ở bước kiểm nguồn | `V5ChatEditorialApiProvider.turnTexts` ghép prompt của đường E (Quality Core + glossary 4 cột đã lọc + "APP DETECTIONS") vào lượt 1 thay vì đính kèm nguyên văn 4 file của owner; Project Instruction gửi hai lần (system và lượt 1) | Codex (triển khai) |
| Tốn 5 lần cho cùng một lỗi | Không chạy thử 1 chương trước; không dừng khi chương đầu đã ra cùng mã dừng. Mỗi lần ~USD 0.08, tổng ~USD 0.39 cho một lỗi phát hiện được offline (pack ghi rõ "5 cột") | Codex + coordinator (request không yêu cầu canary/kiểm tiền điều kiện pack) |
| E-strong bị loại dù recall cao gấp ~7 lần | Luật chọn coordinator viết ("sửa thừa không vượt E-luna = 37") không phù hợp: model mạnh tất yếu sửa nhiều hơn; luật này chặn bước tiến lớn nhất | Coordinator |
| Bỏ sót 1 test FAIL | Chỉ chạy tập con test | Codex |

So với phiên Q1: **Q2 tốt hơn rõ** — tìm ra đòn bẩy chính (model), chuẩn hóa theo dòng không rỗng áp được 15/28 chương an toàn (0 dòng xa FINAL), bỏ được thiên hướng "sửa tối thiểu". Q1 đo trên luna nên không thể thấy điều này, và bộ chấm Q1 còn thổi phồng kết quả. Q2 dừng ở chỗ hợp lý (không chạy holdout khi chưa có cấu hình hợp lệ), chỉ lãng phí ~USD 0.39.

## 4. Sửa trong lượt tiếp nhận (coordinator)

- `scripts/p6/score_vs_final.py`: ký hiệu so với đúng dòng RAW của từng dòng DRAFT (việc dở của Codex, đã kiểm test FAIL trước/PASS sau).
- `scripts/p6/verify_fixture_run.py:341`: khôi phục câu báo lỗi "expected a live provider run with one actual call per API request" (hành vi không đổi).
- Python `scripts/p6` 77/77 PASS. Không đổi mã Java, không build, 0 provider call.
- Trang đọc riêng cho owner: `D:\P5E-private\q2-review\q2-sol-review.html` — mọi dòng Sol hoặc owner sửa ở dev 004–008, cột RAW / DRAFT / Sol / FINAL, viền màu theo loại (cả hai sửa / chỉ Sol / chỉ owner).

## 5. Yêu cầu làm việc Q2.5

Branch giữ nguyên. Không đụng pilot, chunk-pair, U1 (tạm hoãn). FINAL owner chỉ dùng để chấm.

### Q2.5.1 — V5 mô phỏng đúng ChatGPT Project (offline)

1. `V5ChatEditorialApiProvider`: system = Project Instruction (một lần, không lặp ở lượt 1). Lượt 1 = phần "LƯỢT 1" của Prompt đầu chat + Workflow + **4 file nguồn nguyên văn** của owner, mỗi file trong khối `=== FILE: <tên file gốc> ===` … `=== END FILE ===` (RAW, DRAFT, GLOSSARY CSV 5 cột gốc, PRONOUN CSV gốc). Không đưa Quality Core, bảng glossary đã lọc hay "APP DETECTIONS" vào đường V5. Lượt 2, 3 = phần LƯỢT 2, LƯỢT 3.
2. Nguồn file: runner đọc file gốc (tên + byte) từ thư mục input riêng; app (sau này) lấy từ thư viện nguồn. `EditInputs`/request cần mang thêm danh sách file gốc cho V5; đường E giữ nguyên.
3. Trích bản cuối: nhận khối file `…FINAL_QA…` của lượt 3, hoặc `<FINAL>…</FINAL>` (giữ câu nhắc hiện có). Không có cả hai → `V5_FINAL_MISSING`. Lượt nào trả "stop_class:" → dừng chuỗi ngay, không gửi lượt sau, ghi mã `V5_STOP_<reason_code>`.
4. `max_tokens` mỗi lượt đủ cho báo cáo + toàn văn (≥ 32k, theo giới hạn model); `length` → `V5_TRUNCATED_L<n>` như hiện có.
5. **Tiền kiểm offline theo pack:** script/test kiểm đầu vào trước khi gửi — đủ 4 file, glossary đúng header 5 cột, pronoun đúng 7 cột hoặc 3 cột legacy, UTF-8 đọc được, không rỗng. Sai thì dừng trước provider.
6. Test: lượt 1 chứa đúng 4 khối file với tên gốc và header `source,target,category,note,priority`, không chứa "APP DETECTIONS"/"QUALITY STANDARD"; Project Instruction xuất hiện đúng một lần; trích được khối FINAL_QA; "stop_class" ở lượt 1 → chỉ 1 physical call.

### Q2.5.2 — E-strong giảm sửa thừa (offline)

1. Quality Core: thêm "Không đổi cách diễn đạt của câu đã đúng nghĩa, đúng giọng và tự nhiên; chỉ sửa khi có lỗi chỉ ra được theo RAW, glossary hoặc pronoun. Giữ chữ hoa/thường của nhãn trạng thái và thuật ngữ như DRAFT/glossary. Giữ xưng hô của DRAFT trừ khi RAW hoặc pronoun chứng minh sai."
2. Guard tất định: trong dòng có `【…】`, khôi phục hoa/thường nhãn về DRAFT nếu chỉ khác hoa/thường.
3. Test cho guard; tăng `CONTRACT_REVISION`.

### Q2.5.3 — Build, emulator, manifest (offline)

Build qua wrapper (code > 245), cài chỉ `emulator-5554`, instrumented Editorial API như Q2.3. Ước tính USD theo token thật cho V5 đầy đủ 3 lượt (báo cáo dài) với Sol medium.

### Q2.5.4 — Live

1. **Canary V5:** chương 007 (ngắn nhất dev). Nếu dừng ở bất kỳ lượt nào → dừng cả gói, báo mã dừng, không chạy tiếp.
2. Canary đạt (có bản cuối hợp lệ): V5 trên 004, 005, 006, 008; E-strong-b (Q2.5.2) trên 004–008.
3. Chấm bằng bộ chấm hiện tại. **Luật chọn mới** (thay luật "sửa thừa ≤ E-luna"): improved cao nhất trong các cấu hình có 0 dòng sinh thêm chữ Nhật, 0 truncation, và "xa hơn" trên dòng owner sửa ≤ 10% số dòng owner sửa. Sửa thừa được báo cáo, không dùng làm cổng tự động (owner đánh giá qua trang đọc).
4. Holdout 011, 014, 017 với cấu hình đã chọn; không chỉnh gì giữa dev và holdout. Xuất sang `D:\P5E-private\q2-outputs\` kèm trang đọc dạng `q2-sol-review.html` (RAW/DRAFT/app/FINAL) cho owner. Dừng.

Ngân sách: theo D-Q2b (mục 6). Ledger tiếp tục `Q2-20261008` (đã dùng USD 0.8375159).

### Bằng chứng

Cập nhật `docs/EDITORIAL_API_V1_Q2_EXECUTION_20261008.md` (mục Q2.5): commit, test (chạy **toàn bộ** `scripts/p6`, engine, app), APK, turn 1 đã render (chỉ hash + tên khối file), kết quả canary, bảng dev/holdout, call/token/USD. Cập nhật §10, snapshot, BUILD_STATE. Push sau mỗi gói.

## 6. Cần owner

| ID | Nội dung | Khuyến nghị |
|---|---|---|
| Đọc | Mở `D:\P5E-private\q2-review\q2-sol-review.html`, xem các dòng viền cam (chỉ Sol sửa): phần lớn là diễn đạt khác chấp nhận được, hay làm xấu bản dịch? | Đọc 1 chương (007 hoặc 004) là đủ để định hướng; câu trả lời giúp quyết định có cần Q2.5.2 mạnh tay không |
| D-Q2b | Chạy Q2.5.4 với trần Q2 nâng từ USD 6.00 lên **USD 10.00** (ledger hiện đã dùng USD 0.84). V5 đầy đủ ước tính ~USD 0.5–0.9/chương; dev 5 + E-strong-b 5 + holdout 3 ≈ USD 4–6. Trần chương nâng lên **USD 1.50** cho riêng V5. Canary dừng là dừng cả gói | Duyệt |

## 7. Quyết định đã nhận (2026-10-08)

Owner (chat): "duyệt D-Q2b nhưng chạy trên gpt 5.6 luna thôi, chạy api sol đắt lắm, cơ bản nếu nó đạt chất lượng tối thiểu của 4.1.3 là được".

Mục này thay các chỗ nói về Sol ở mục 5:

- **Model duy nhất cho Q2.5: `openai/gpt-5.6-luna`.** Không chạy Sol trong Q2.5. Reasoning **medium** (trước đây luna chạy `minimal`; medium là biến mới, rẻ, và khớp mức owner dùng). Hai arm:
  - `V5-luna`: đường V5 sửa theo Q2.5.1 (4 file gốc, pack 4.1.3 FULL CHATGPT).
  - `E-luna-b`: đường E với Quality Core + guard Q2.5.2.
  - Mốc so sánh có sẵn, không chạy lại: C1 (luna minimal, Q1) và E-strong (Sol, Q2.4).
- **Ngân sách D-Q2b:** trần Q2 USD 10.00 (đã dùng USD 0.8375159), trần chương V5 USD 1.50, E USD 0.10. Với luna, ước tính thực tế toàn Q2.5 dưới USD 1.
- **Canary giữ nguyên:** `V5-luna` chương 007 trước; dừng ở bất kỳ lượt nào (stop_class, thiếu FINAL, truncation) → dừng cả gói và báo mã.
- **Cổng "chất lượng tối thiểu 4.1.3"** (thay luật chọn ở mục 5, Q2.5.4.3). Một chương đạt mức tối thiểu khi đủ cả:
  1. Cấu trúc: không truncation, không mất/gấp chữ, số dòng không rỗng không lệch RAW nhiều hơn DRAFT.
  2. Không lỗi kỹ thuật mới: 0 dòng sinh thêm chữ Nhật; số dòng còn chữ Nhật ≤ của FINAL owner; số dòng lệch ký hiệu so với RAW ≤ của DRAFT.
  3. Không làm hỏng: số dòng xa FINAL hơn trên các dòng owner sửa ≤ 10% số dòng owner sửa; độ giống cả chương với FINAL **không giảm** so với DRAFT.
  4. Có sửa thật: improved ≥ 25% số dòng owner sửa.
  5. **Owner đọc và chấp nhận** — đây là điều kiện quyết định; 1–4 chỉ là lọc tự động trước khi owner đọc.
  Chọn arm có nhiều chương dev đạt 1–4 nhất (hòa thì improved cao hơn). Không arm nào có ≥ 3/5 chương dev đạt 1–4 → vẫn chạy holdout với arm tốt nhất, nhưng báo rõ "chưa đạt tối thiểu" để owner quyết hướng tiếp theo.
- Holdout 011, 014, 017 với arm đã chọn; xuất bản app + trang đọc (RAW/DRAFT/app/FINAL như `q2-sol-review.html`) vào `D:\P5E-private\q2-outputs\`. Dừng.
