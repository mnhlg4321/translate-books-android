# Editorial API V1 — review Q1 và yêu cầu Q2 (2026-10-08)

Coordinator: Claude. Baseline kiểm: HEAD `4ee3518c` (đã push). APK `4.18-q1.1`/code 244 từ `41edafa4`, chỉ trên `emulator-5554`. Owner yêu cầu đánh giá: bản biên tập đã đạt chất lượng chưa, và các việc đã làm đã đạt chưa.

## 1. Kết luận ngắn

- **Bản biên tập: CHƯA ĐẠT.** Chấm đúng cách, mọi cấu hình chỉ đưa 5–8% số dòng owner sửa lại gần bản FINAL hơn, đồng thời sửa thêm nhiều dòng owner giữ nguyên; độ giống FINAL cả chương tăng ở 1–3/8 chương.
- **Các việc Q1: đạt một phần.** Kỹ thuật chạy đúng (test sạch, ngân sách, không rò FINAL, guard chặn chữ Nhật mới), nhưng bộ chấm thổi phồng kết quả, bước chuẩn hóa không sửa được chương thật nào, và giả thuyết "model mạnh hơn" chưa được kiểm (C2 là model rẻ hơn).

## 2. Kiểm chứng

| Claim | Evidence coordinator tự kiểm | Kết luận |
|---|---|---|
| Test xanh tại `4ee3518c` | `git archive` sạch: engine 561/0, app 431/0, androidTest compile PASS, Python 75 OK | Đã chứng minh |
| FINAL không vào prompt | Quét 33 prompt Q1.5: 2 "trùng" ở chương 022 là một nửa của dòng DRAFT mà owner tách đôi, không phải nội dung FINAL | Đã chứng minh |
| 33 call, USD 0.2623, 0 UNKNOWN | Theo ledger báo cáo; không kiểm lại hash chain | Chấp nhận theo evidence |
| Fix recall 0.41–0.43 (dev), 0.38 (holdout) | **Sai do bộ chấm**: `score_vs_final.py` tính dòng là "đã sửa" khi bản app giống FINAL ≥ 0.97 dù DRAFT vốn đã ≥ 0.97 và app không đổi. Chính DRAFT để nguyên đạt 0.382 ("baseline 28 chương") | Bác bỏ con số; đã sửa bộ chấm (mục 3) |
| Q1.2 chuẩn hóa PASS | 1/28 chương đủ điều kiện căn dòng "mạnh" (yêu cầu trùng cả dòng trống), **0 sửa** trên chương thật; trong khi DRAFT lệch ký hiệu so với RAW 1626 lần và 15/28 chương có số dòng không rỗng bằng RAW | PASS về an toàn, **không đạt mục đích** |
| Quality Core v2 bỏ thiên hướng sửa tối thiểu | Câu mở đầu prompt C1 vẫn ghi "changing as little as possible" | Chưa làm đủ |
| C2 = model mạnh hơn | C2 = `qwen/qwen3-235b-a22b-2507`, rẻ hơn luna (USD 0.002–0.010/chương) | Giả thuyết năng lực model **NOT_MEASURED** |
| U1 (UI) | Không có commit U1 nào | NOT_STARTED |

## 3. Chấm lại bằng chỉ số đúng (coordinator, chỉ đếm)

Định nghĩa: căn dòng DRAFT↔FINAL và DRAFT↔app riêng; với dòng owner sửa, "gần hơn" = độ giống FINAL tăng > 0.005, "khớp" = trùng nguyên văn FINAL; "xa hơn" = giảm > 0.005; "sửa thừa" = dòng owner giữ nguyên mà app đổi.

| Cấu hình | Chương | Dòng owner sửa | Khớp | Gần hơn | Xa hơn | Sửa thừa | Chương có độ giống tăng |
|---|---:|---:|---:|---:|---:|---:|---:|
| C0 dev (luna, V1.1) | 8 | 217 | 15 (6.9%) | 16 (7.4%) | 10 | 32 | 3/8 |
| C1 dev (luna, V1.2) | 8 | 217 | 9 (4.1%) | 17 (7.8%) | 11 | 52 | 2/8 |
| C2 dev (qwen, V1.2) | 8 | 217 | 7 (3.2%) | 10 (4.6%) | 18 | 38 | 1/8 |
| C0 holdout | 6 | 219 | 4 (1.8%) | 11 (5.0%) | 20 | 62 | 2/6 |
| C1 holdout | 6 | 219 | 9 (4.1%) | 13 (5.9%) | 7 | 13 | 1/6 |

Đọc số: cả ba cấu hình gần như không tái tạo được việc owner làm; sửa thừa nhiều hơn sửa trúng. Q1.3 chỉ đổi được phần kỹ thuật (0 chữ Nhật sinh thêm), không đổi chất lượng. Lưu ý: chỉ số này phạt cả cách sửa khác owner mà vẫn đúng, nên nó là cận dưới — nhưng owner đã đọc N6 và kết luận chưa đạt, khớp với số liệu.

Sửa trong lượt review (coordinator): `scripts/p6/score_vs_final.py` chỉ tính "đã sửa" khi bản app trùng FINAL hoặc độ giống tăng ≥ 0.02; thêm test `test_an_untouched_draft_fixes_nothing_even_when_owner_edits_are_tiny` (DRAFT để nguyên → 0). Python scorer 4/4 PASS.

## 4. Chẩn đoán và hướng đi

Đã loại trừ bằng số liệu: định dạng/ghi sổ (V1 trả văn bản, 0 lỗi cấu trúc), chữ Nhật sinh thêm (guard), luật prompt "giữ ký hiệu/sửa tối thiểu" đơn thuần (đổi luật không tăng chất lượng).

Còn lại hai giả thuyết chưa được đo, và đó là khác biệt lớn nhất giữa app và cách owner tạo FINAL:

1. **Năng lực model và suy luận.** App dùng luna với reasoning tối thiểu, một lượt. Owner tạo FINAL bằng Project ChatGPT/Claude với model mạnh.
2. **Quy trình v5 đầy đủ.** Owner chạy pack v5 (Project Instruction + Workflow + Turn Prompt) **3 lượt trong cùng một cuộc chat**: kiểm → sửa → QA. App rút còn một lượt với luật tóm tắt. Bản 8-call trước đây thất bại vì JSON ghi sổ, không phải vì quy trình 3 lượt.

Hướng chọn: **mô phỏng đúng cái đã được chứng minh hiệu quả** (quy trình và model owner dùng), app chỉ điều phối cuộc chat và trích văn bản FINAL; mọi báo cáo của model để dạng chữ tự do, lưu riêng, không parse. Đo trước bằng bộ chấm đã sửa, rồi owner đọc.

Song song, việc tất định miễn phí: sửa bước chuẩn hóa để thực sự áp được (căn theo dòng không rỗng khi số dòng bằng nhau — 15/28 chương), đo trên 28 chương với yêu cầu 0 dòng xa FINAL hơn.

## 5. Yêu cầu làm việc Q2 (cho phiên Codex)

Branch giữ nguyên. Không đụng pilot, chunk-pair, file UI của U1. FINAL owner chỉ dùng cho bộ chấm. Tập chương:

- Dev (đã xem): 004, 005, 006, 007, 008.
- **Holdout mới, khóa từ bây giờ, chưa ai chạy:** 011, 014, 017. Holdout Q1 (010/013/016/019/022/025) đã dùng một lần, không dùng để chọn cấu hình nữa.

### Q2.1 — Bộ chấm và chuẩn hóa (offline)

1. Nhận bản sửa bộ chấm của coordinator; thêm vào báo cáo các cột: khớp, gần hơn, xa hơn, sửa thừa, số chương có độ giống tăng (như mục 3). Chấm lại toàn bộ Q1.5 + N6 bằng bộ chấm mới, lưu ngoài Git, ghi số vào báo cáo.
2. `RawAlignedNormalizer`: thêm chế độ căn "dòng không rỗng bằng nhau" (bỏ yêu cầu trùng dòng trống). Đo trên 28 chương: số chương áp dụng, số sửa theo loại, số dòng gần/xa FINAL. PASS khi xa FINAL = 0; quy tắc nào gây xa FINAL thì tắt quy tắc đó và ghi lại.
3. Bỏ câu "changing as little as possible" khỏi Quality Core v2 (giữ "giữ nguyên dòng đã đúng").

### Q2.2 — Đường "v5 chat" (offline)

1. Provider mới `V5_CHAT`: gửi một cuộc hội thoại 3 lượt qua OpenRouter, mỗi lượt là một request có toàn bộ lịch sử trước đó (bật prompt caching nếu model hỗ trợ).
   - System = Project Instruction của pack owner dùng.
   - Lượt 1 = Turn Prompt L1 + Workflow + RAW/DRAFT/Glossary/Pronoun dạng văn bản đính kèm.
   - Lượt 2 = Turn Prompt L2; lượt 3 = Turn Prompt L3.
   - App thêm vào cuối lượt 3 một câu duy nhất: "Sau toàn bộ báo cáo, đặt toàn văn bản cuối giữa `<FINAL>` và `</FINAL>`."
2. Không parse báo cáo; lưu nguyên văn ngoài Git. Chỉ trích `<FINAL>`, áp guard hiện có (mất/gấp chữ, chữ Nhật mới, meta) và chuẩn hóa Q2.1.
3. Pack: dùng đúng bản owner xác nhận (mục 6); ghi SHA-256 từng file vào metadata run.
4. Test: lịch sử hội thoại đúng thứ tự; thiếu `<FINAL>` → `RETRY_REQUIRED` giữ DRAFT; finish `length` ở bất kỳ lượt nào → dừng, không gửi lượt sau; chi phí cộng dồn đúng; UNKNOWN không gửi lại.
5. Runner: chế độ `V5_CHAT` và `E` với model override; ước tính worst-case theo token thật của dev.

### Q2.3 — Build, emulator, manifest (offline)

Build qua wrapper (code > 244), cài chỉ `emulator-5554`, instrumented Editorial API như Q1.4. Manifest A/B: `E-luna` (C1 hiện tại, làm mốc), `E-strong` (Quality Core v2 sửa, model mạnh, reasoning cao), `V5-strong` (v5 chat, model mạnh). Model mạnh = model owner dùng cho FINAL (mục 6); nếu không có trên OpenRouter thì model tương đương gần nhất, ghi giá. Ước tính USD/chương và tổng theo token thật.

**Dừng sau Q2.3** nếu chưa có D-Q2; có D-Q2 thì tiếp Q2.4.

### Q2.4 — Live (chỉ khi owner duyệt D-Q2)

1. Dev 004–008: `E-strong` và `V5-strong` (E-luna đã có từ Q1). Chấm bằng bộ chấm mới.
2. Chọn cấu hình theo: "gần hơn + khớp" cao nhất với "sửa thừa" không vượt E-luna và 0 chữ Nhật sinh thêm.
3. Holdout 011, 014, 017: chạy cấu hình đã chọn. Không chỉnh gì giữa dev và holdout.
4. Cổng đề xuất (owner chỉnh được): holdout "gần hơn + khớp" ≥ 40% số dòng owner sửa; độ giống FINAL tăng ở 3/3 chương; 0 chữ Nhật sinh thêm, 0 truncation. Đạt thì xuất 3 chương sang `D:\P5E-private\q2-outputs\` để owner đọc — **đây là cổng 3 chương FINAL**. Không đạt vẫn xuất 1 chương tốt nhất để owner đánh giá hướng.

### Bằng chứng

`docs/EDITORIAL_API_V1_Q2_EXECUTION_<date>.md`: commit, test, APK, bảng chấm lại Q1, kết quả chuẩn hóa 28 chương, manifest + giá, bảng dev/holdout theo chương (chỉ số mục 3), call/token/USD, ledger. Cập nhật §10, snapshot, BUILD_STATE. Push sau mỗi gói.

## 6. Cần owner

| ID | Nội dung | Khuyến nghị |
|---|---|---|
| Thông tin | Bản FINAL 001–028 được tạo bằng **model nào** (vd GPT-5.x Thinking, Claude Opus/Sonnet) và **pack nào** (đường dẫn bản 4.1.3 FULL trong `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\…`)? Có sửa tay thêm sau khi model xuất không? | Trả lời ngắn; đây là dữ kiện quyết định thiết kế Q2.2/Q2.3 |
| D-Q2 | Cho phép Q2.4: ledger mới `Q2-<date>` trần **USD 6.00**, trần chương **USD 1.00**, dev 5 + holdout 3 chương, model mạnh theo câu trả lời trên, chỉ emulator; dừng và hỏi nếu ước tính vượt trần | Duyệt — chi phí dự kiến USD 2–4, thấp so với giá trị của việc biết model mạnh có đạt chuẩn không |
| U1 | Giữ U1 chờ tới khi Q2 cho thấy hướng chất lượng đạt (UI chỉ có giá trị khi bản biên tập đạt) | Duyệt tạm hoãn U1 |

## 7. Quyết định đã nhận

Owner (chat, 2026-10-08): "duyệt D-Q2 và Tạm hoãn U1".

- **D-Q2:** Q2.4 chạy ngay sau khi Q2.3 PASS, không hỏi lại. Ledger mới `Q2-<date>`, trần **USD 6.00**, trần chương **USD 1.00**; dev 004–008 (`E-strong`, `V5-strong`), holdout 011/014/017 (cấu hình đã chọn); chỉ `emulator-5554`, không pilot, không chunk-pair. Dừng và hỏi nếu ước tính worst-case vượt trần, UNKNOWN cost, lỗi hạ tầng, hoặc cần đổi tập chương. Runner chỉ nhận trần nhóm ≤ USD 1.00 thì nâng giới hạn đó trong Q2.2 (có test), không chia nhỏ ledger để lách.
- **U1 tạm hoãn:** không làm U1 cho tới khi Q2 có chương được owner chấp nhận; thiết kế giữ nguyên.
- **Model và pack (owner chưa trả lời câu hỏi mục 6):** mặc định pack `D:books. Prompt cac the loai.BIÊN TẬPBIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASECHATGPT` (3 file: Project Instruction, Prompt đầu chat 3 lượt, Workflow — bản acceptance đã dẫn chiếu), ghi SHA-256 từng file. Model mạnh mặc định: model OpenAI lớp cao nhất (không phải luna/mini) có trên OpenRouter, reasoning cao, với ước tính ≤ USD 1.00/chương cho đường V5 3 lượt; nếu không có model nào đạt trần chương thì chọn model mạnh nhất trong trần và ghi lý do. Nếu owner trả lời khác trước khi Q2.4 bắt đầu, dùng theo câu trả lời của owner.

## 8. Owner xác nhận model (2026-10-08)

Owner (chat): "final được làm bằng gpt 5.6 sol medium trên project chat của chat gpt".

- Model mạnh của `E-strong` và `V5-strong` là **GPT-5.6 Sol, reasoning medium** (trên OpenRouter dự kiến `openai/gpt-5.6-sol`; Codex xác minh đúng id và giá hiện hành, ghi vào manifest). Không thay bằng model khác. Reasoning đặt `medium` cho cả hai đường để khớp cách owner tạo FINAL.
- Môi trường gốc là ChatGPT Project: Project Instruction là chỉ dẫn của project, file nguồn được đính kèm, 3 lượt trong cùng một chat. Đường `V5_CHAT` mô phỏng: Project Instruction ở system, nội dung file nguồn đưa nguyên văn vào lượt 1, 3 lượt trong cùng lịch sử. Khác biệt không tránh được (ChatGPT có thể truy xuất file theo đoạn, có công cụ/bộ nhớ riêng) phải ghi trong báo cáo.
- Pack: owner chưa nêu phiên bản; giữ mặc định 4.1.3 FULL CHATGPT (mục 7). Nếu owner nêu bản khác trước Q2.4 thì đổi theo.
- Nếu ước tính worst-case của `V5-strong` với GPT-5.6 Sol vượt USD 1.00/chương hoặc tổng vượt USD 6.00: dừng trước Q2.4 và báo con số, không tự đổi model hay cắt tập chương.
