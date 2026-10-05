# Editorial API V1 — kế hoạch đổi hướng (2026-10-05)

Trạng thái: ĐỀ XUẤT, chờ owner duyệt các quyết định ở mục 9. Tài liệu này thay hướng thực thi của P6 trong `EDITORIAL_RECOVERY_V4_18.md`; không mở branch, release hay checklist mới (vẫn `feature/v4.18-p5e-runner-repair-20260917`, checklist `release_checklists/v4.18-editorial-v5-safe-4-1-3.md`). Viết sau khi coordinator (Claude) đọc lại toàn bộ phiên điều phối, phiên đánh giá kiến trúc của Codex (`chatgpt.com/s/cx_6ac3bd82…`, chỉ phân tích, không sửa mã) và đối chiếu mã tại HEAD `aa565098`.

## 1. Vì sao đổi hướng

Mục tiêu sản phẩm không đổi: người dùng chọn RAW + DRAFT (+ Glossary/Pronoun nếu có), bấm Biên tập, nhận bản cuối đúng RAW, lưu–mở lại–xuất TXT; ba chương đại diện đạt rồi sang P7.

Sau P5–P6, sản phẩm vẫn chưa có một chương FINAL nào. Bằng chứng:

| Sự thật | Nguồn | Hệ quả |
|---|---|---|
| Kiến trúc hiện tại cần 8 call/chương (L1 RAW+RECONCILE, L2 RAW+EDIT+FINAL-READ, L3 REAUDIT+RECONCILE+FINAL-READ) | `EditorialChapterFinalCoordinator`, `EDITORIAL_RECOVERY_V4_18.md` §3 | Bản 4.1.3 gốc chỉ yêu cầu 3 lượt trong cùng một chat; 8 call là lựa chọn triển khai của app, không phải luật gốc |
| 7 lần dừng live liên tiếp đều do định dạng/ghi sổ (unit ref, hash, anchor, speakerRecords, draft.after…), không do nội dung | `docs/P6_R6_DECISIVE_FIX_PLAN_20261005.md` §1 | Phần lớn công sức đi vào hợp đồng ghi sổ, không vào chất lượng văn |
| Sau khi sửa, L1 qua cấu trúc 8/8 nhưng chỉ phát hiện 10/25 lỗi gieo | `docs/P6_R6_G1_SEMANTIC_ADJUDICATION_20261005.md` dòng 50 | Hồ sơ ledger đầy đủ không làm model tìm lỗi tốt hơn |
| Mỗi call L1 tốn ~52k token input cho chương 191 unit | `docs/P6_R6_Z5_G1_RESULT_20261005.md` | Pack 4.1.3 (32 KB) + luật wire chiếm phần lớn input |
| Sửa lỗi (repair) chưa từng được đo; G2 dừng vì mất predecessor trên emulator | `docs/P6_R6_G2_PREFLIGHT_20261005.md` | Kiến trúc nhiều bước phụ thuộc trạng thái trung gian dễ vỡ |
| Luồng Dịch đã có prompt refinement "RAW + DRAFT + glossary/pronoun → trả văn bản đã sửa trong thẻ" chạy ổn định | `PromptPlan.refinement` (`app/.../PromptPlan.java:96`) | Đầu ra dạng thẻ văn bản đã được chứng minh trên app thật |
| UI Editorial hiện là bảng chẩn đoán dev: bắt chọn pack, dán nội dung vào ô chữ, ghép RAW/DRAFT theo số trong tên file, import Glossary/Pronoun riêng từng project, hiện sha256/binding/"cần cấp phép" | `EditorialPageFactory.java:120-245`, `EditorialImportPlanner.java:149`, `MainActivity.java:977` | Người dùng không dùng được như luồng Dịch; Library Glossary/Pronoun không được dùng lại |

Kết luận: giữ **tiêu chuẩn chất lượng** của v5/4.1.3, bỏ **thủ tục ghi sổ** mà model phải làm, đưa nó sang app hoặc bỏ hẳn; dựng UI người dùng thật.

## 2. Những gì giữ, bỏ, chuyển

### 2.1 Giữ nguyên làm "Quality Core" (luật model phải theo)

1. RAW quyết định nội dung, thứ tự, tác nhân/người nhận, speaker/listener, POV, số liệu, phủ định, phạm vi, độ chắc chắn.
2. Đủ: không thêm, không thiếu, không đảo câu/đoạn; giữ kể, thoại, nội tâm, bảng, số, ngắt cảnh.
3. Glossary khóa tên riêng/thuật ngữ khi không trái RAW; áp theo ngữ cảnh, không tìm–thay máy móc.
4. Pronoun là tham khảo có scope (from/speaker/target/self/call/scope); không suy "私 = tôi" chung chung; xưng hô giữa một cặp nhất quán trong cùng cảnh/phase, không đổi khi không có căn cứ RAW.
5. Sửa tối thiểu: giữ phần đúng; không viết lại để khác; không kịch hóa, không đổi mức độ.
6. Không chắc thì giữ DRAFT và ghi chú ngắn.
7. Giữ ký hiệu/dấu thoại/khung (「」『』◇◇◇ ＊＊＊ ── …… 【】…), xóa rác kỹ thuật (metadata, Markdown lỗi, U+FFFD, zero-width).
8. Đầu ra chỉ chứa bản văn; ghi chú nằm ngoài thẻ văn bản.

### 2.2 Bỏ khỏi nhiệm vụ của model

| Luật 4.1.3 | Xử lý | Ai thay thế / bù bằng gì |
|---|---|---|
| Manifest, tên file, hash, revision, trạng thái nguồn, Stop Receipt | Bỏ | App snapshot nội dung + SHA-256, kiểm nguồn rỗng/không đọc được trước khi gọi API |
| REPORT_L1 là predecessor bắt buộc; VI_L2/CHANGE_MAP_L2/QA_RECEIPT | Bỏ khỏi luồng mới | App lưu candidate, diff, kết quả kiểm, chi phí trong DB |
| Title/Glossary, Semantic Risk, Relation Candidate ledgers cho mọi occurrence; equations "Candidate = Mapped + Dismissed" | Bỏ | Model chỉ báo lỗi có bằng chứng; app kiểm heuristic glossary (mục 4.3); bộ chấm độc lập đo recall |
| Pair Record đầy đủ Observed/Approved, ROLE_LOCKED proof từng field | Bỏ thủ tục; giữ nguyên tắc ở Quality Core 4 | Lượt Kiểm có loại lỗi `PRONOUN`/`SPEAKER` |
| Speaker Proof cho mọi edit thoại | Chỉ cần khi lượt Kiểm báo đổi speaker/listener/xưng hô | — |
| Error/Change ID trước mỗi edit; Declared Population ⊇ Actual Changes | Bỏ | App tính diff thật sau khi nhận văn bản, gắn ID |
| Dựng lại toàn bộ ledger và đọc lại từ đầu sau mỗi change set, lặp tới ổn định | Bỏ | Một lượt Kiểm trên bản hoàn chỉnh + tối đa 1 vòng kiểm lại |
| Ghép RAW–DRAFT theo tên file/chapter key | Bỏ chặn | Người dùng xác nhận cặp trên màn hình; tên chỉ dùng gợi ý |
| Bắt buộc có Glossary | Bỏ | Cho phép thiếu Glossary và/hoặc Pronoun, cảnh báo rõ |

Chấp nhận đánh đổi: mất khả năng kiểm toán "chứng minh coverage tuyệt đối" bằng sổ do model khai. Không tuyên bố tính năng này nữa; thay bằng phép đo độc lập trên fixture (mục 6).

## 3. Kiến trúc mới: hợp đồng `EDITORIAL_API_V1`

```text
Chọn tổ hợp (RAW, DRAFT, Glossary?, Pronoun?, cài đặt) -> màn xác nhận
 -> app snapshot + kiểm nguồn (rỗng/encoding/độ dài)
 -> [E] Biên tập: Quality Core + Glossary/Pronoun + RAW + DRAFT -> <EDITED>văn bản</EDITED> + <NOTES>
 -> app guards (mục 4.3)
 -> [C] Kiểm (chế độ "Kỹ"): Quality Core-check + nguồn + RAW + EDITED + danh sách đoạn đã đổi -> JSON nhỏ {verdict, issues[]}
 -> app áp các sửa đơn đoạn có trích dẫn khớp đúng một lần
 -> [C2] Kiểm lại một lần nếu có áp sửa
 -> FINAL_OK hoặc FINAL_CÓ_GHI_CHÚ -> lưu -> xem / so sánh / xuất TXT
```

- Chế độ **Nhanh** = chỉ E (1 call). Chế độ **Kỹ** = E + C (+ C2) ≤ 3 call (+ tối đa 1 retry kỹ thuật cho mỗi bước). Chế độ mặc định chọn theo kết quả đo A/B (mục 6).
- E dùng **đầu ra thẻ văn bản** giống luồng Dịch, không JSON: ít lỗi định dạng, không có escape/neo dòng. `<NOTES>` là các dòng `đoạn trích VI | loại | lý do ngắn`, tùy chọn; lỗi parse NOTES không làm hỏng EDITED.
- C dùng JSON strict nhỏ (strict json_schema, OpenRouter): `verdict ∈ {PASS, ISSUES, WRONG_PAIR}`; mỗi issue có `edited_quote` (≤120 ký tự, trích nguyên văn EDITED), `raw_quote` (≤120), `kind ∈ {MEANING, OMISSION, ADDITION, NUMBER, NEGATION, SPEAKER, PRONOUN, GLOSSARY, REGRESSION, TECHNICAL}`, `fix` (văn bản thay cho đúng `edited_quote`, có thể rỗng nếu chỉ báo). Không có unit ref, hash, ID, đếm.
- App định vị `edited_quote` bằng khớp chuỗi đã chuẩn hóa khoảng trắng; không khớp hoặc khớp nhiều lần → issue giữ ở trạng thái "cần xem", đếm số lượng, **không** làm hỏng lượt (bài học P6: lỗi ghi sổ là số đo, không phải điểm dừng).
- C nhận RAW trước, rồi EDITED; DRAFT gốc chỉ xuất hiện dưới dạng danh sách đoạn đã đổi (trước → sau) để bắt hồi quy. Không đưa NOTES của E vào C (giữ độc lập).
- Sai cặp nguồn: C (hoặc E qua thẻ `<WRONG_PAIR>bằng chứng</WRONG_PAIR>`) báo kèm trích dẫn; app dừng và yêu cầu người dùng chọn lại. Khác biệt cục bộ (thiếu câu, sai số) là lỗi cần sửa, không phải sai cặp.

### 3.1 Glossary/Pronoun đưa vào prompt

- Glossary: các mục có `source` xuất hiện trong RAW chương; không áp giới hạn số mục của luồng Dịch (`glossaryInjectLimit` không dùng cho Editorial). Nếu không mục nào khớp thì gửi không có phần glossary.
- Pronoun: toàn bộ hàng của bundle đã chọn có `from`/`speaker`/`target` xuất hiện trong RAW; định dạng 7 cột và 3 cột legacy như v4.17. Không fallback pronoun từ glossary.
- Thiếu nguồn: prompt ghi rõ "Không có glossary/pronoun; giữ cách gọi đang nhất quán trong DRAFT trừ khi trái RAW". UI hiện cảnh báo trước khi chạy.
- Ghi lại số mục đã gửi vào kết quả chạy (để chẩn đoán), không cần model nhắc lại.

### 3.2 Độ dài và chunking

- Chương vừa ngân sách context (RAW + DRAFT + nguồn ≤ ~40k token ước tính) chạy nguyên chương.
- Chương dài hơn: phiên bản đầu dừng với thông báo rõ "chương quá dài, chưa hỗ trợ". Chunking (ý tưởng của owner: RAW chunk + DRAFT chunk + glossary/pronoun của chunk) là bước N7, **chỉ** làm sau khi E/C đã đạt trên chương nguyên, vì cần căn RAW↔DRAFT theo cảnh (không cắt hai bản độc lập theo số thứ tự) và kiểm biên chunk. Không triển khai đồng thời để còn biết thay đổi nào có ích.

## 4. Những thứ app chịu trách nhiệm

### 4.1 Tái sử dụng

| Thành phần có sẵn | Dùng cho |
|---|---|
| OpenRouter transport, settings model/key (`AppSettings`), strict json_schema | E và C |
| `PromptPlan.refinement` cách ghép thẻ, `PromptContextBuilder` (chỉ phần dựng khối luật, không dùng bộ lọc có giới hạn) | Dựng prompt E |
| `GlossaryStore`, `PronounStore` (Library của luồng Dịch), parser 4 cột / 7 cột | Chọn Glossary/Pronoun |
| SAF + `FileUtil.takePersistable/readText`, danh sách "Recent translated files" | Chọn RAW/DRAFT |
| `EditorialTextDiff` | Diff hiển thị, danh sách đoạn đã đổi cho C, tỷ lệ viết lại |
| Fixture runner instrumented, `scripts/p6/*` (manifest, spend ledger hash-chain, response retention ngoài Git, `score_run.py` chấm văn bản cuối) | Đo A/B |
| Wrapper build `scripts/build-and-save*.ps1`, emulator `emulator-5554` | Build/kiểm |

### 4.2 Đóng băng (không xóa, không phát triển tiếp)

Toàn bộ đường SAFE4 8-call (L1/L2/L3 providers, ledger v2/v3, `EditorialChapterFinalCoordinator`, pack import/binding, lineage). Giữ mã và test để không phá regression; ẩn khỏi UI người dùng (chỉ còn trong mục "Công cụ dev — SAFE4 legacy" thu gọn). G2 hủy; ledger G1 đóng ở USD 0.28859445. Xóa mã cũ là việc dọn dẹp sau P7, không làm bây giờ.

### 4.3 App guards sau E (tất định, không gọi model)

| Guard | Hành vi khi vi phạm |
|---|---|
| `finish_reason` ≠ stop, thiếu thẻ, EDITED rỗng | Retry kỹ thuật 1 lần; vẫn lỗi → `RETRY_REQUIRED`, giữ DRAFT |
| Rò rỉ meta (thẻ lạ, Markdown, "Ghi chú:", tiếng Anh giải thích) trong EDITED | Loại phần rò nếu tách được; không thì như trên |
| Số dòng không rỗng của EDITED lệch DRAFT > 10% hoặc > 5 dòng | Cờ `STRUCTURE_WARN` (C phải kiểm), không chặn |
| Tỷ lệ ký tự thay đổi > 35% | Cờ `REWRITE_WARN` |
| Số lượng từng ký hiệu khung (「」『』◇◆＊ ── 【】…) khác DRAFT mà RAW không giải thích | Cờ `SYMBOL_WARN` |
| Mục glossary có source trong RAW nhưng target không xuất hiện trong EDITED | Cờ `GLOSSARY_WARN` (danh sách mục) |

Cờ được đưa vào C như "điểm cần kiểm", hiện cho người dùng ở màn kết quả. Không cờ nào tự động biến thành lỗi nội dung.

### 4.4 Lưu bền

Hai bảng mới, migration chỉ thêm (không sửa bảng cũ):

- `editorial_combos`: id, tên, nguồn RAW (uri, tên hiển thị), nguồn DRAFT, glossary_id (nullable), pronoun_id (nullable), settings_json (chế độ, model override, trần USD/chương), created/updated.
- `editorial_api_runs`: id, combo_id, contract_revision, model, mode, sha256 + nội dung snapshot từng nguồn, state (`RUNNING`, `RETRY_REQUIRED`, `WRONG_PAIR`, `FINAL_OK`, `FINAL_NOTES`, `CANCELLED`), edited_text, final_text, notes, issues_json, guards_json, calls/tokens/USD theo bước, created/updated.

Mở lại: đọc run đã lưu, không gọi lại API. Nguồn trên đĩa đổi sau snapshot → hiện "nguồn đã thay đổi, chạy lại?" thay vì tự chạy. Xuất TXT: chỉ `final_text`, UTF-8, đọc lại file sau ghi để xác nhận.

## 5. UI người dùng

1. Tab **Biên tập**: danh sách tổ hợp đã lưu (tên, trạng thái chạy cuối, nút Chạy/Mở). Nút "+ Tổ hợp mới".
2. Màn **Tổ hợp**: RAW (chọn file hoặc từ bản dịch gần đây), DRAFT (như trên), Glossary (danh sách Library hoặc "Không dùng"), Pronoun (như trên), Chế độ (Nhanh / Kỹ), Model (mặc định theo Cài đặt), Trần chi phí/chương (mặc định USD 0.10). Lưu với tên, ví dụ `raw001+draft001+glossary001+pronoun001`. Có nút "Nhân bản" để đổi một thành phần.
3. **Màn xác nhận** trước khi gọi API: tên hai file, 3 dòng đầu mỗi file, số ký tự, glossary/pronoun đang dùng (hoặc cảnh báo thiếu), ước tính chi phí, nút Đổi / Chạy. Cảnh báo khi tỷ lệ độ dài DRAFT/RAW bất thường (ngoài 0.5–4), không chặn.
4. **Tiến độ**: bước hiện tại (Biên tập / Kiểm / Kiểm lại), chi phí đã dùng; hủy được.
5. **Kết quả**: Xem bản cuối, So sánh với DRAFT (diff), danh sách "Cần xem" (issues chưa áp, cờ guard) với trích RAW/VI, Xuất TXT, Chạy lại. Chi tiết kỹ thuật (hash, model, token) trong mục thu gọn.
6. Không hiện "pack", "binding", "SAFE4_BLOCKED", "cấp phép" trong luồng người dùng.

Các cài đặt thêm có thể làm sau P7 (ghi nhận, không làm bây giờ): hướng dẫn biên tập riêng theo series, danh sách ký hiệu riêng, xử lý honorific, chạy hàng loạt nhiều chương, chunking.

## 6. Đo và quyết định

Phép đo A/B trên cùng bộ fixture G1 (manifest `8c3868ba…`, 8 fixture: seeded `fx-a03/a04/a05/a07/a08/a11`, clean `fx-a02`, ambiguous `fx-a12`) + 2 lượt lặp `fx-a11`, `fx-a04`, cùng model `openai/gpt-5.6-luna`, cùng nguồn:

- Nhánh A = Nhanh (E). Nhánh B = Kỹ (E + C + C2).
- Chỉ số mỗi fixture: target gieo đã **sửa đúng** trong văn bản cuối (`score_run.py`), lỗi mới do biên tập gây ra (phân xử độc lập theo cách của `P6_R6_G1_SEMANTIC_ADJUDICATION`), tỷ lệ viết lại trên fixture sạch, cờ guard, call/token/USD/thời gian.
- Ghi model/route/prompt revision/source commit vào metadata của mỗi run (thiếu ở G1).
- Quy tắc chạy Z3 giữ nguyên: một lần bị từ chối là số đo, chạy tiếp; dừng nhóm chỉ khi UNKNOWN, chạm trần, lỗi hạ tầng, hoặc 3 fixture liên tiếp cùng mã.

Cổng quyết định (đề xuất, owner có thể chỉnh):

| Điều kiện | Kết luận |
|---|---|
| Nhánh tốt nhất sửa đúng ≥ 15/25 target, 0 lỗi mới loại MEANING/OMISSION/NUMBER/NEGATION trên fixture sạch, 0 truncation | Chọn nhánh đó làm mặc định → chạy 3 chương thật |
| B hơn A ≥ 4 target hoặc ít lỗi mới hơn rõ | Mặc định Kỹ; ngược lại mặc định Nhanh (rẻ hơn), giữ Kỹ làm tùy chọn |
| Cả hai < 15/25 | Không chạy chương thật; phân tích lỗi bị bỏ sót theo loại, sửa Quality Core/C prompt một lần, đo lại một lần (ngân sách riêng) |

Ước tính chi phí (từ giá G1: ~USD 0.13/M input, ~USD 1.25/M output): E ≈ 25k in + 10k out ≈ USD 0.016; C ≈ 30k in + 1.5k out ≈ USD 0.006. A/B 10 run ≈ USD 0.45 thực tế; reservation worst-case cần trần nhóm mới **USD 1.00** (ledger mới, không kế thừa ledger G1/G2).

## 7. Lộ trình

| Bước | Nội dung | Live? | Đầu ra |
|---|---|---|---|
| N0 | Owner duyệt plan + quyết định mục 9; cập nhật `EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` cho API_V1 (giữ tiêu chí chất lượng, thay tiêu chí artifact L1–L3) | Không | Docs |
| N1 | Engine: Quality Core prompt, builder E/C, parser thẻ + JSON C, guards, áp sửa, state machine, test (kể cả đột biến từ response thật) | Không | Mã + test |
| N2 | App: provider E/C qua transport hiện có, bảng mới, lưu/mở lại/xuất, runner fixture cho API_V1, metadata đủ | Không | Mã + test |
| N3 | UI mục 5 + ẩn SAFE4 legacy | Không | Mã + test + ảnh chụp emulator |
| N4 | Build qua wrapper, cài emulator, kiểm offline (fake provider) toàn luồng: tạo tổ hợp → xác nhận → chạy → lưu → mở lại → xuất | Không | APK + bằng chứng |
| N5 | A/B mục 6 | Có (cần duyệt + trần USD 1.00) | Bảng A/B + quyết định mặc định |
| N6 | 3 chương thật đại diện bằng chế độ đã chọn; owner đọc và chấp nhận | Có (cần duyệt) | 3 FINAL đạt → P7 |
| N7 | (Sau P7, tùy chọn) chunking theo cảnh cho chương dài | — | — |

N1–N4 làm liền trong một phiên Codex, push sau mỗi gói. Dừng trước N5.

## 8. Rủi ro và cách chặn

| Rủi ro | Chặn |
|---|---|
| Model viết lại quá tay khi trả toàn văn | Quality Core 5, guard REWRITE_WARN, đo tỷ lệ viết lại trên fixture sạch, cổng mục 6 |
| Model làm rơi câu/đoạn khi chép lại toàn chương | Guard số dòng + C loại OMISSION + finish_reason |
| C và E cùng bỏ sót một lỗi | Đo recall độc lập; không tuyên bố "đã chứng minh đủ" |
| Áp sửa từ C làm hỏng ngữ pháp | Chỉ áp khi trích khớp đúng 1 lần; C2 kiểm lại; còn lỗi → "cần xem", không tự sửa tiếp |
| Lặp lại vòng "vá định dạng" như P6 | Đầu ra E là văn bản; C chỉ có 4 trường; mọi lỗi ghi sổ là số đo, không dừng nhóm |
| Phá regression SAFE4 cũ | Không sửa mã SAFE4; chỉ thêm lớp mới; giữ test cũ PASS |

## 9. Quyết định cần owner

| ID | Quyết định | Khuyến nghị |
|---|---|---|
| D-N1 | Đổi hướng sang `EDITORIAL_API_V1` (E/C), đóng băng đường SAFE4 8-call, hủy G2 | Duyệt |
| D-N2 | Thay tiêu chí nghiệm thu artifact L1–L3 bằng tiêu chí chất lượng mục 6 + owner đọc 3 chương | Duyệt |
| D-N3 | Cho phép biên tập khi thiếu Glossary/Pronoun (cảnh báo, không chặn) | Duyệt |
| D-N4 | Ngân sách N5: nhóm mới trần USD 1.00, model `openai/gpt-5.6-luna` — duyệt trước để Codex chạy liền sau N4, hoặc duyệt sau khi xem bằng chứng N4 | Duyệt sau N4 (một lần duyệt, không hỏi từng run) |
