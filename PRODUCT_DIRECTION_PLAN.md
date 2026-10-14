# Translate Books — kế hoạch hướng đi sản phẩm

Owner duyệt 2026-10-13. Đây là plan điều hành duy nhất cho đến khi một bản sửa đi tới bước C; khi đó mới mở nhánh/checklist release theo `DEVELOPMENT_WORKFLOW.md`.

## 1. Mục tiêu và thước đo

- Mục tiêu: giảm số chỗ owner phải tự sửa trên mỗi chương app tạo ra, không thêm sửa sai.
- Thước đo duy nhất: trên chương thật, owner đọc đầu ra của app và đếm lỗi app sửa đúng, lỗi còn sót, sửa sai — theo loại (xưng hô, thuật ngữ, nghĩa, văn phong). FINAL của owner là tham chiếu, không phải luật.
- Không phải thước đo: số test, số điểm bộ kiểm, cổng QA, số commit, số tài liệu.

## 2. Luật chống việc vô ích

Một việc chỉ được làm khi trả lời được cả ba câu: (1) phục vụ quyết định sản phẩm nào (chọn vấn đề, phát hành hay bỏ một thay đổi); (2) kết quả nào làm quyết định đó khác đi; (3) đây có phải cách rẻ nhất để có bằng chứng đó.

Cấm: gắn lại nhãn bộ kiểm; dựng cổng/giàn đo mới; tài liệu không kèm thay đổi trạng thái thật; chạy lại test khi mã không đổi; lượt trả phí khi chưa có giả thuyết và tiêu chí phát hành/bỏ.

## 3. Các bước

| Bước | Làm gì | Chi phí | Đầu ra | Quyết định owner |
|---|---|---|---|---|
| A. Kiểm kê vấn đề (1 phiên) | Phân loại các chỗ owner đã sửa DRAFT → FINAL trên 8 chương có sẵn; kiểm DRAFT có do app dịch với cùng file pronoun hay không | USD 0, offline | Một bảng số đếm theo loại; 5 ví dụ thật cho mỗi loại lớn nhất, ở trang đọc riêng ngoài Git | Chọn 1 vấn đề |
| B. Truy vết | 3–5 ví dụ của vấn đề đã chọn: prompt dịch có luật không → DRAFT → Biên tập có gắn cờ không → prompt/response đã lưu → guard → nội dung lưu | USD 0 | Khâu phát sinh lỗi, bản sửa nhỏ nhất, cách kiểm | Duyệt bản sửa |
| C. Sửa | Nhánh mới từ `main`; test đơn vị từ chính các ví dụ (tên giả); replay miễn phí response đã lưu | USD 0 | Commit, test xanh | — |
| D. Một lượt nghiệm thu | Luồng app bình thường trên emulator (APK thật, store SQLite thật, không cổng dừng theo bộ kiểm); 1 chương có vấn đề + 1 chương chưa từng dùng | Trần riêng owner duyệt | Trang đọc RAW / DRAFT / bản app (FINAL tham chiếu) | Phát hành hay bỏ |
| E. Phát hành | Gom các bản sửa đã qua D vào một release theo 14 bước | Theo workflow | Release mới | Tag/merge |

Dừng và đóng việc, không mở thêm đo, khi: A cho thấy vấn đề lớn nhất nằm ngoài phần app sửa được; B không tìm ra khâu gây lỗi; D không làm giảm số chỗ owner phải sửa. Nếu B chứng minh lỗi nên chặn ở khâu dịch, sửa khâu dịch và ngừng đầu tư vào Biên tập cho vấn đề đó. Đổi kiến trúc (trả toàn văn, V5) chỉ được xét khi B chứng minh kiến trúc hiện tại không sửa được vấn đề đã chọn.

## 4. Baseline

- App phát hành: v4.21.0/code271, production source `cb288c9a`, hợp đồng `CP-IMPL-19`, annotatedtag `v4.21.0` trên QA source `54936850` (production bằng source đã kiểm). Main tích hợp `--no-ff` và xuất bản refs E9 xác nhận tại handoff; không đổi checkout owner. v4.20.0/code270 giữ nguyên trong archive lịch sử.
- v4.19 (`feature/v4.19` `3098a059`): đóng băng, không phát hành; thay đổi prompt D-A không vào bản nào trừ khi vấn đề được chọn cần đến nó. Các gói X4b, B 008, cổng X5 và gắn lại nhãn đã hủy.
- Sự thật đã kiểm 2026-10-13: luồng dịch đưa hàng pronoun vào prompt như "mandatory locks" (`PromptBuilder.java:103-120`); finder Biên tập bỏ qua hàng thiếu `self` hoặc `call` (`FixPointFinder.java`, vòng hàng pronoun).
- Dữ liệu cho bước A: `D:\P5E-private\q2-inputs\` (RAW/DRAFT/glossary/pronoun), FINAL của owner, dữ kiện từng dòng `D:\P5E-private\v419-pronoun\line-facts.jsonl` (1.447 dòng, 289 dòng owner sửa, 153 có từ xưng hô đổi). Văn bản sách ở ngoài Git.

## 5. Trạng thái

| Bước | Trạng thái | Kết quả |
|---|---|---|
| A | xong 2026-10-13 | 28 chương, 4.956 dòng DRAFT, owner sửa 895 dòng: xưng hô 370 (254 chỉ xưng hô; phần lớn là **tự xưng**: `tôi`→`em` 61, `mình`→`tôi` 43, `tớ`→`mình` 31, `tôi`→`tao` 28), câu chữ nhỏ 233, chỉ dấu câu/ngoặc 186 (ngoặc gốc 〝〟『』「」 bị đổi thành “ ”), tên/thuật ngữ 184, viết lại 67. DRAFT 001–028 tạo 05–08/09, sau bộ pronoun và bản `v4.17-dev.13` có khóa pronoun trong prompt dịch (suy luận mạnh). Owner chọn **P1: tự xưng sai** |
| B | xong 2026-10-13 | 122 lần sửa tự xưng ở 8 chương có dữ kiện từng dòng. **Dữ liệu pronoun đúng** (122/122 từ đúng là `self` của một hàng phủ đoạn). Lỗi phát sinh ở **khâu dịch**: hàng đúng gần như chắc có trong prompt dịch (3–13 hàng/chương, giới hạn 40), model chọn sai: 50 ca lấy `self` của nhân vật khác (014: 31), 26 ca dùng từ không thuộc hàng nào, 8 ca đúng người nói nhưng nhầm người nghe, 38 ca lời kể (chỉ chương 004). Biên tập gắn cờ 70/84 dòng thoại nhưng không sửa vì nhiều hàng có `self` khác nhau và prompt bảo "mơ hồ thì giữ". Gốc chung: không ai xác định người nói/người nghe cho từng dòng. Loại: dò lời kể (mọi chương có hàng người kể, chỉ 004 cần sửa), thay thế tất định (26 ca đều có 2–5 `self` khả dĩ) |
| C | xong offline 2026-10-13 (`feature/v4.20`) | Owner chọn C1. Điểm ADDRESS liệt kê các hàng một lần, đánh số `R1…` kèm ghi chú của owner; luật ADDRESS lặp gom thành một dòng "từ cần kiểm". Model đổi xưng hô phải trả `[id] R<n> <dòng>`; app chỉ nhận dạng của đúng hàng đó, không cho xóa dạng hợp lệ của hàng, từ chối hàng bị RAW loại trừ hoặc trái lời gọi trực tiếp; đổi xưng hô không trích hàng thì giữ DRAFT. Bỏ mặc định "mơ hồ thì giữ" khi RAW cho biết ai nói với ai. Test tổng hợp (Aki/Mori/Ken/Rina) ghim đúng kiểu lỗi bước B. Response đã lưu của các lần chạy cũ không trích hàng nên không dùng để replay đánh giá được. Hợp đồng nâng `CP-IMPL-15` → `CP-IMPL-16` (lượt chuẩn bị bằng hợp đồng cũ không được chạy tiếp). Archive sạch `739e5c49`: engine 714, app 472, lint 0 lỗi, androidTest compile OK |
| D | S0–S9 xong 2026-10-13, chờ owner đọc | Một lượt 014 trên 4.20-d.1/code268 (CP-IMPL-16); S4 fake10/real0, TXT trùng DRAFT. Live7 calls/no retry, USD0.01801660, pending0/overrun0; dừng ở chunk13 vì điểm glossary PROTECTED_SYMBOL_CHANGE, chunk14–16 NOT_REACHED, chương INCOMPLETE. Trang S7 D:/P5E-private/chunk-runs/V420-D/READING_014.html có37 dòng. S8: 21 dòng B (34 lần thay từ): 7 khớp từ FINAL tham chiếu,13 không đổi,1 đổi khác;16 dòng app đổi ngoài nhóm B,1 cậu↔tên. Chưa kết luận chất lượng/phát hành/bỏ |
| C2' | xong offline 2026-10-13 | Owner duyệt lộ trình C2' → E và ngưỡng. Lỗi định dạng của **một** điểm (dấu, chữ Nhật, độ dài, thẻ, nhiều dòng, rỗng) chỉ giữ DRAFT của dòng đó (KEPT, giữ lý do); chunk bị chặn khi **từ 2 điểm** trở lên của cùng câu trả lời lỗi định dạng; lỗi neo dòng vẫn chặn. Hợp đồng `CP-IMPL-17`. Lý do: app chỉ thử lại lỗi trước khi gửi (`EditorialPairRunService` 295–300), một dòng sai dấu đã dừng cả chương 014 ở chunk 13. Replay response thật đã lưu của lượt `7bc2b3e2` (không gọi model): chunk 13 không còn REJECTED (13:3 giữ DRAFT; 13:1, 13:2, 13:5 được áp), chunk 1–12 cho 24 dòng FIXED như lượt thật. Trang đọc 3 dòng mới: `D:\P5E-private\chunk-runs\V420-D\replay-c2-CP-IMPL-17\READING_014_CHUNK13_C2.html`. Owner đọc 2026-10-13: **3/3 dòng đúng** |
| E | E-bis đã đạt mọi điều kiện mục8; Workflow COMPLETE, tagv4.20.0 | RC4.20.0/code270/source261d3b8b/CP-IMPL-18; engine720PASS/1SKIP, app mỗi biến thể470PASS/3SKIP,Python106+24PASS; device158PASS/7SKIP/0FAIL; hiệu năng mới/11trace/6ảnh/video thật. PreTag/PreBackup/CompletePASS, build-20261013-182812 vàtag-v4.20.0 mỗi30checksum/4mirrorkhớp, annotatedtagtạiQA2bc2384f. Owner014accepted và01716/17đúng, headingwrongfixedoffline13e12a65; không lượt trả phí mới, không gắn lại nhãn. Main--no-ff/refpublicationlàbướcđóngcuốiđượcduyệt,xácnhậntạihandoff. Phát hiện mới sang bản sau trừ hồi quy |

Dữ liệu bước A/B (có văn bản sách) ở `D:\P5E-private\product-plan-A\`, ngoài Git.

**Next action:** owner dùng4.21.0 sau E9 xác nhận refs.

### v4.21 — P2 dấu câu theo RAW (2026-10-14)

| Bước | Trạng thái | Kết quả |
|---|---|---|
| B | xong offline | 28 chương: mô phỏng "theo RAW" khớp FINAL 114 dòng, khác FINAL 59 dòng (owner lúc đổi lúc không: `〝〟` đổi 50 / giữ `“ ”` 22; `：` đổi 29 / giữ `:` 19; `……` 17/0). App không tự đổi ngoặc; prompt dịch không yêu cầu giữ ngoặc gốc |
| Quyết định owner | 2026-10-14 | **Luôn theo RAW** cho `〝〟 「」 『』`, `：` và `……` |
| C | xong offline (`feature/v4.21`) | `RawPunctuation` tất định, không model: cặp `“ ”`/`" "` lấy ngoặc RAW theo thứ tự khi số cặp bằng nhau; `:`→`：`, `...`→`……` khi số lần bằng RAW; chữ và số không bao giờ đổi; ngoặc lồng hoặc số lượng lệch thì giữ nguyên. Finder ghi dòng cần khôi phục (dòng DRAFT ghép một nhóm RAW); applier khôi phục trên câu trả lời đã được nhận và trên dòng không có sửa; chunk không có điểm cho model thì app khôi phục **không gọi model**. Model vẫn không được tự đổi ngoặc. Hợp đồng `CP-IMPL-19`. Đo bằng mã app trên 8 chương: 47 dòng khôi phục, 40 khớp FINAL, 7 là chỗ FINAL giữ dạng DRAFT, 0 lệch khác. Trang đọc `D:\P5E-private\v421-p2\READING_P2_8CH.html` |
| D | không cần lượt trả phí | Thay đổi tất định, đã đo trên mã thật |
| E | Workflow COMPLETE, tagv4.21.0; E9 refs xác nhận tại handoff | RC4.21.0/code271/sourcecb288c9a/CP-IMPL-19; engine726PASS/1SKIP,app mỗi variant471PASS/4SKIP,Python106+24PASS;device158PASS/7SKIP/0FAIL,restart6PASS. E5initialFAIL thiếu SurfaceFlinger frame data giữ nguyên, targeted/full rerunPASS trên đúng APK lưu, không sửa mã/tiêu chí;planning432ms,11trace nghiệm thu/22trace tổng. E6media1PASS/6ảnh/video, DB+journal hashkhớp,RC271 restored. PreTag/PreBackup/CompletePASS; build-20261014-091247/tag-v4.21.0 mỗi52checksum/fourmirrors, annotatedtagQA54936850, APKsourcecb288c9a riêng/hashkhớp. Main--no-ff/quét/non-forcepush là bước cuối được duyệt, tree/ref kiểm tại handoff; USD0 |


Review độc lập 2026-10-13 tại `71781fd2`: HEAD sạch trước review và remote khớp; 20/20 hash evidence cùng hash trang đọc khớp; ledger 7 calls/USD0.01801660/pending0 được kiểm lại. Chunk13 điểm3 bị chặn đúng vì response thêm U+300C/U+3002/U+300D vào DRAFT không có các dấu đó; không có bằng chứng lỗi guard để vá. STRUCTURAL_VALID toàn chương chưa đạt, SEMANTIC_EVAL chờ owner; SQLite/reopen/SAF của lượt này NOT_MEASURED vì runner dùng InMemoryPairRunStore. S0–S9 hoàn thành công việc thu evidence, không đồng nghĩa nghiệm thu sản phẩm. Không đổi acceptance, checklist hay evidence cũ.

## 6. Phương án sửa P1 (chờ owner)

- **C1 — Biên tập trích hàng:** với điểm ADDRESS, model trả `[id] R<k> <dòng>` (số hàng nó áp dụng) hoặc `[id] =`; app chỉ nhận thay đổi xưng hô thuộc đúng hàng `R<k>`, từ chối trộn dạng của nhiều hàng; bỏ quy tắc "mơ hồ thì giữ" cho trường hợp RAW xác định được người nói. Model làm phần phán đoán (ai nói với ai), app làm phần kiểm. Kiểm offline: parser/guard với ca tổng hợp; hiệu quả thật cần một lượt D. Rủi ro: model trích sai hàng sẽ thành sửa sai hợp lệ về hình thức. Tiêu chí bỏ: ở D, số lỗi tự xưng sửa đúng không lớn hơn số sửa sai mới.
- **C2 — Khâu dịch trình bày hàng theo người nói:** gom hàng theo nhân vật (một nhân vật xưng gì với từng người nghe) trong prompt dịch. Thay đổi nhỏ, phòng lỗi từ gốc, nhưng ảnh hưởng mọi bản dịch và đo phải dịch lại chương (đắt hơn Biên tập).
- Không làm: dò lời kể, thêm từ vựng, cổng cue, thêm câu nhắc chung vào prompt.

## 7. Gói D — một lượt nghiệm thu C1 trên chương 014 (owner duyệt quyền chi 2026-10-13; yêu cầu chi tiết `docs/V420_D_014_WORK_REQUEST.md`)

- Giả thuyết: khi phải trích hàng, model sửa được lỗi tự xưng lấy nhầm hàng của nhân vật khác (bước B: 014 có 31 ca loại này, 34 lỗi tự xưng) mà không sinh sửa sai mới. Không đo offline được vì là hành vi model; response cũ không theo định dạng mới.
- Phạm vi: chương 014 (chưa từng dùng để chỉnh Biên tập), một lượt, `openai/gpt-5.6-luna` medium, tối đa 10 lời gọi (đo offline: 16 chunk, 10 có điểm, 68 điểm), không retry, sổ chi mới. Giữ chỗ ước tính USD 0,061; đề nghị trần **USD 0,08**.
- Cách chạy: APK sản xuất build bằng wrapper, chỉ cài `emulator-5554`; runner chương dùng `EditorialPairRunService` thật của app với store trong bộ nhớ (việc lưu SQLite đã có test thiết bị riêng, không phải câu hỏi của bước D); mỗi chunk gửi đúng một lần (không retry, tối đa 10 lời gọi) theo quyền đã duyệt; không cổng dừng theo bộ kiểm; trước lượt thật chạy giả (USD 0) để kiểm cơ chế.
- Đầu ra: trang đọc riêng RAW / DRAFT / bản app (FINAL tham chiếu) cho mọi dòng app đổi và mọi dòng có lỗi tự xưng của bước B; số đếm: lỗi tự xưng sửa đúng, còn sót, sửa sai mới, đổi khác. Owner đọc là cổng.
- Tiêu chí: phát hành nếu số lỗi tự xưng sửa đúng lớn hơn rõ rệt số sửa sai mới theo owner đọc; bỏ C1 (hoàn tác) nếu sửa đúng không lớn hơn sửa sai.

- Kết quả thực thi S0–S9 (2026-10-13): báo cáo `artifacts/qa/v4.20-d-014-20261013/QA_REPORT.md` + `SUMMARY.json`, hai bản hash khớp. Production/test wrapper events đã lưu và so byte ở đúng D:/App Translate Books/artifacts/builds|test-builds và backup/builds|test-builds trước cài. Chỉ emulator-5554; không sửa production C1, owner files, script gốc, không cổng/bộ kiểm v4.19. Run014 7bc2b3e2-9795-4299-98f4-5ebfdcca6bfe, sổ V420-D-014-20261013-7bc2b3e2:7 RESERVE/7 SETTLE, USD0.01801660, pending0. Dừng theo service ở chunk13, không retry; phần trần chưa dùng không cấp quyền lượt mới. Trang37 dòng và số S8 chỉ chuẩn bị cho owner đọc, không dùng để tự kết luận C1 đạt hay không.

### Phân xử owner và quyết định giá trị C1 — 2026-10-13

Owner xác nhận trực tiếp trong hội thoại: toàn bộ 37 dòng S7 đúng, đạt kết quả đề ra (bao gồm dòng giữ nguyên). Đây là nguồn phân xử mới; JSON xuất trước đó rỗng không được dùng để phủ nhận xác nhận này. Không sửa nhãn/evidence S8 lịch sử, không đưa phân xử hoặc FINAL vào runtime.

- Đã đối chiếu đủ37 dòng:24 dòng đổi,13 giữ nguyên;21 dòng B/34 lần thay từ, giao với nhóm đổi8 dòng. 20/20 hash evidence khớp SUMMARY tại baseline71781fd2.
- Tự xưng sửa đúng trong nhóm B:8 dòng/10 lần thay từ thực tế, gồm7 dòng khớp từ tham chiếu và1 dòng đổi khác nhưng owner xác nhận đúng. Còn sót/sửa sai mới/chưa chắc trên37 dòng theo owner:0/0/0; không suy rộng tới chunk14–16 chưa chạy. 13 dòng B giữ nguyên chứa24 lần thay từ tham chiếu chưa thực hiện, không tự gọi là24 lỗi còn sót sau phân xử này.
- Ngoài B:16 dòng đổi đúng theo owner, gồm10 dòng ADDRESS (1 dòng cậu↔tên) và6 dòng GLOSSARY; không cộng chúng thành lợi ích sửa tự xưng. Đúng về nội dung không tự chứng minh mọi thay đổi đều cần thiết; lợi ích bổ sung chưa định lượng riêng.
- Khuyến nghị duy nhất: **tiếp tục hướng C1**, vì8 dòng tự xưng sửa đúng so với0 sửa sai mới theo owner. Không phải đủ điều kiện phát hành: chương INCOMPLETE, SQLite/reopen/SAF vẫn NOT_MEASURED. Không có phép đo đối chứng để quy toàn bộ lợi ích riêng cho cơ chế trích hàng.

| chunk/point; dòng DRAFT (1-based) | Ban đầu → thay đổi thực tế | Owner / evidence / kết luận |
|---|---|---|
| 1/6;18 | tự xưng đổi1 lần | ĐÚNG; response001 R7, gate FIXED, output có; lợi ích C1 |
| 2/8;31 | tự xưng đổi2 lần | ĐÚNG; response002 R8, gate FIXED, output có; lợi ích C1 |
| 8/2;132 và8/4;134 | tự xưng đổi1 lần mỗi dòng | ĐÚNG; response003 R3, gate FIXED, output có; lợi ích C1 |
| 8/5;136 và8/6;138 | tự xưng đổi1 lần mỗi dòng | ĐÚNG; response003 R2, gate FIXED, output có; dòng138 khác từ FINAL vẫn đúng |
| 8/9;143 | tự xưng đổi2 lần | ĐÚNG; response003 R2, gate FIXED, output có; lợi ích C1 |
| 9/5;149 | tự xưng đổi1 lần | ĐÚNG; response004 R2, gate FIXED, output có; lợi ích C1 |
| 5/—;95,97;6/—;103;7/—;121,128;10/—;160;11/—;184,189 | B nhưng không có điểm; giữ nguyên8 dòng | ĐÚNG; chunks không có điểm tương ứng, output giữ; không chấm sót theo FINAL |
| 9/8;158 | model giữ nguyên1 dòng | ĐÚNG; response004 =, MODEL_KEEP; không phải lỗi sót theo owner |
| 13/1;214;13/2;216;13/5;222 | đề xuất FIXED nhưng output giữ3 dòng | ĐÚNG với output; chunk STRUCTURE_BLOCKED; không tính đề xuất là sửa thành công |
| 13/4;219 | guard giữ1 dòng | ĐÚNG; ADDRESS_VALID_FORM_CHANGED; output không đổi |
| 1/2;4;1/3;5;2/1;23;2/2;24;2/4;26;2/5;27;2/6;28;2/10;34;2/11;35;8/3;133 | đổi ADDRESS ngoài B10 dòng | ĐÚNG; gate FIXED/output có; dòng133 cậu↔tên tách riêng, không cộng vào tự xưng |
| 1/7;20;11/1;183;11/2;188;12/4;203;12/5;206;12/6;210 | đổi glossary ngoài B6 dòng | ĐÚNG; gate FIXED/output có; lợi ích khác, không quy cho C1 |

Không có ca sai/sót theo phân xử mới cần truy vết thêm. Điều đã chứng minh: các sửa tự xưng được finder chọn, prompt có hàng được trích, model trích R phù hợp với dạng được giữ trong gate, và output của chunk ACCEPTED chứa sửa; chặn chunk13 do glossary thêm dấu là đúng. Chưa chứng minh: chất lượng phần chưa tới, hiệu quả SQLite/SAF, hay mức tăng so với cùng model không dùng C1. Báo cáo S0–S9 và SUMMARY giữ nguyên như evidence lịch sử.
