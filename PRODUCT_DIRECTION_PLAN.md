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

**Trạng thái điều hành mới nhất:** xem mục10 (v4.23, 2026-10-21); các next action v4.22 bên dưới là lịch sử.

### v4.22 — Biên tập dùng được (owner dùng 4.21.0 trên điện thoại, 2026-10-14)

- Phản hồi owner: màn tạo lượt rối, Glossary/Pronoun mặc định "Không dùng" (muốn 4 file mặc định + ô tick 2 file); kết quả là "một mớ chữ"; cài đặt Biên tập thiếu so với luồng dịch; tiến trình không dùng lại hệ Jobs của luồng dịch; bản cuối gần như không đổi.
- Q0 (offline, chương 013 Kumo tập 7, file owner gửi): DRAFT tốt (2 lỗi xưng hô thật trên 220 dòng: Ariel → Shiro "tôi/cô" thay vì `chị`; Potimas → Ariel "lão"); finder tạo 19 điểm, 15 điểm xưng hô giả (không biết người nói), 2 glossary giả (`白` trong `白い悪魔`; `白いの` đã có hàng pronoun); model giữ đúng các điểm giả; với lỗi Ariel → Shiro model **đề xuất đúng nhưng app chặn** (`ADDRESS_FORM_NOT_IN_CITED_ROW`: cột `call` của hàng chỉ có `Shiro-chan`, ghi chú mới có "gọi em"), đề xuất bị bỏ và màn hình ghi "Giữ nguyên" (đính chính 2026-10-15; câu cũ "model sót/giữ" là sai); lỗi "lão" không có điểm; bản cuối đổi đúng 1 dòng, không cần thiết ("Hắn" → "Potimas"). Đính chính: điểm "ngươi" trong ảnh owner là lời Potimas, giữ nguyên là đúng.
- Quyết định owner: duyệt 4.22 = U1 (4 file mặc định, ô tick 2 file, giữ "Job Dịch") + U2 (kết quả dễ đọc) + U3 (mục Biên tập trong Settings) + P (Biên tập chạy như job, dịch vụ nền, tab Jobs) + G (glossary nhận nhầm), không lượt trả phí; duyệt Q1 (phát hiện xưng hô theo người nói) ở dạng thiết kế + test offline, lượt trả phí xin riêng.
- Yêu cầu: `docs/V422_WORK_REQUEST.md`, `docs/V422_Q1_SPEAKER_DESIGN_REQUEST.md`.
- Trạng thái 2026-10-14 (chờ owner bấm thử bản dev): U1, U2, U3, P, G đã làm; bản dev `4.22-dev.2`/code 273 (APK trong `artifacts/builds/v4.22-dev.2/build-20261014-164611/`), QA emulator-5554 provider giả 164 PASS / 10 SKIP / 0 FAIL, 11 ảnh; chưa cài điện thoại, chưa phát hành. G: điểm GLOSSARY 21 → 18 trên 9 chương, bỏ đúng 3 điểm giả của Kumo 013, 0 điểm owner từng sửa bị mất. Ảnh chụp lộ một lỗi thật của P (thẻ Biên tập không hiện ở tab Jobs), đã sửa và làm lại QA. Q1: số liệu offline không đủ (bắt 1/2 lỗi thật Kumo dù nhãn tham chiếu), không đề xuất lượt đo trả phí. Báo cáo: `artifacts/qa/v4.22-dev-20261014/QA_REPORT.md`. **Next action:** owner bấm thử bản dev (ảnh + APK), rồi quyết định cài điện thoại và phát hành 4.22.0.
- **R — đề xuất bị chặn (owner 2026-10-15: "lỗi cực kỳ lớn, sửa dứt điểm").** Nguyên nhân: prompt đưa cả ghi chú của hàng cho model, guard chỉ tin cột `self`/`call` và khi chặn thì bỏ luôn câu model trả. Không cho guard tin ghi chú: 22/44 ghi chú có từ xưng hô ngoài cột là câu phủ định ("không tự động thành tao/mày"). Sửa (`CP-IMPL-21`): (1) mọi đề xuất bị chặn bởi khóa nội dung/độ dài/dấu được giữ (`proposed`, hàng đã dẫn), màn kết quả có mục "Đề xuất bị chặn — chờ bạn quyết" với so sánh, lý do tiếng Việt và nút **Áp dụng đề xuất** (ghi vào kết quả đoạn, ghép lại bản cuối, nhật ký `PROPOSAL_APPLIED`, không đổi schema); câu sai định dạng (nhiều dòng, thẻ, chữ Nhật mới) không bao giờ được đưa ra; (2) ô `self`/`call` cho nhiều dạng cách nhau bằng `/` (`Shiro-chan/em`): finder và checklist nhận từng dạng (ô "chị Erika" vẫn là một dạng, để chữ "chị" đứng riêng còn tạo điểm sửa họ → tên gọi; hồi quy bản sạch đã bắt lần thử tách từng chữ làm mất điểm này), guard kiểm từng chữ như trước; dữ liệu 9 chương hiện không có `/` nên điểm không đổi. Phát lại offline câu trả lời thật của model (lượt 4.21.0) cho dòng 151: hàng cũ → bị chặn, đề xuất giữ lại, Áp dụng → đúng 1 dòng đổi thành câu đúng; hàng `Shiro-chan/em` → app tự áp dụng. Lỗi 346 (thiếu hàng Potimas → Ariel) vẫn chỉ sửa được bằng dữ liệu hoặc Q1. Kèm sửa nhỏ: `build-and-save.ps1` dừng khi bản build đánh số không thấy kho build (code 274 đã lặp tên `4.22-dev.1` vì `highestArchivedVersionCode = 0`).


### v4.21 — P2 dấu câu theo RAW (2026-10-14)

| Bước | Trạng thái | Kết quả |
|---|---|---|
| B | xong offline | 28 chương: mô phỏng "theo RAW" khớp FINAL 114 dòng, khác FINAL 59 dòng (owner lúc đổi lúc không: `〝〟` đổi 50 / giữ `“ ”` 22; `：` đổi 29 / giữ `:` 19; `……` 17/0). App không tự đổi ngoặc; prompt dịch không yêu cầu giữ ngoặc gốc |
| Quyết định owner | 2026-10-14 | **Luôn theo RAW** cho `〝〟 「」 『』`, `：` và `……` |
| C | xong offline (`feature/v4.21`) | `RawPunctuation` tất định, không model: cặp `“ ”`/`" "` lấy ngoặc RAW theo thứ tự khi số cặp bằng nhau; `:`→`：`, `...`→`……` khi số lần bằng RAW; chữ và số không bao giờ đổi; ngoặc lồng hoặc số lượng lệch thì giữ nguyên. Finder ghi dòng cần khôi phục (dòng DRAFT ghép một nhóm RAW); applier khôi phục trên câu trả lời đã được nhận và trên dòng không có sửa; chunk không có điểm cho model thì app khôi phục **không gọi model**. Model vẫn không được tự đổi ngoặc. Hợp đồng `CP-IMPL-19`. Đo bằng mã app trên 8 chương: 47 dòng khôi phục, 40 khớp FINAL, 7 là chỗ FINAL giữ dạng DRAFT, 0 lệch khác. Trang đọc `D:\P5E-private\v421-p2\READING_P2_8CH.html` |
| D | không cần lượt trả phí | Thay đổi tất định, đã đo trên mã thật |
| E | E1-E9 COMPLETE; tag/main/nhánh đã xuất bản và xác nhận | RC4.21.0/code271/sourcecb288c9a/CP-IMPL-19; regression/device PASS tách SKIP. E5initialFAIL thiếu SurfaceFlinger frame data giữ nguyên, targeted/full rerunPASS trên đúng APK lưu, không sửa mã/tiêu chí;planning432ms,11trace nghiệm thu/22trace tổng. E6media1PASS/6ảnh/video, DB+journal hashkhớp,RC271 restored. PreTag/PreBackup/CompletePASS; build-20261014-091247/tag-v4.21.0 mỗi52checksum/fourmirrors,tagQA54936850/actualAPKsourcecb288c9a. Main--no-ff251a4c03/tree bằng feature306028b4, initialatomicnonforcepush/ls-remote xác nhận;6commit/113blobscan0secret/fingerprint/booktext. Metadata xác nhận này không đổi production/tag; refs cuối kiểm tại handoff. USD0 |


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


## 10. Trạng thái điều hành hiện tại — 2026-10-23, v4.23

- Quyết định sản phẩm đang phục vụ: giảm retry vô ích và chi phí runtime, từ chối chunk còn dòng nguồn tiếng Nhật rõ ràng, làm đồng hồ phiên bền qua pause/stop, và ghi route provider để chẩn đoán offline; giữ nguyên schema, Editorial, định tuyến và request body dùng chung.
- Đã hoàn tất đúng thứ tự tài liệu: F4 `9958b757`, F1 + F6 `5d1e98c5`, F5 `9dbf89f5`, F2 `ae2a825f`, F3 `bc7120e6`; live mục 6 sau đó chạy đúng 2 lượt được owner cấp quyền, không có lượt provider ngoài phạm vi.
- Candidate **4.23-dev.23/code301**, event `build-20261023-185610`, source `bc7120e669a8f3ac543757b6a73cb7fc517088f5`, APK SHA-256 `44B80CEA36604D062001294DCEB9095B238D787DFAF95BC8AA4FCF2976B3D569`; mirror tại `artifacts/builds/v4.23-dev.23/build-20261023-185610/` và `backup/builds/v4.23-dev.23/build-20261023-185610/`.
- Archive sạch `D:/P5E-builds/v423-clean-regression-20261023-02/source/` đã PASS unit test debug/benchmark/release, lint debug/benchmark/release và compile AndroidTest. Phone `15e84958` đã preflight đúng SHA/certificate/token, cài `install -r` thành công, mở được app, và không có FATAL log trong lượt khởi động. F4 post-install đếm `jobs=0`, `config_revisions=0`, `prepared_batches=0`; SharedPreferences `apiKey` không rỗng nhưng không in/chụp giá trị. F2 replay trên DB copy ngoài Git: `1,262` chunk đã nhận; sẽ từ chối `15` ở 40% và `20` ở 30%; false positive phone chưa có nhãn adjudication, fixture tổng hợp quan sát `0`.
- Phone evidence: pre-install backup `D:/App Translate Books/App Translate Books-device-backups/v4.23-dev.23-code301/20261023-192028/`; post-install backup `D:/App Translate Books/App Translate Books-device-backups/v4.23-dev.23-code301/20261023-192106-post-install/`. Portrait Dịch, Công việc, Thư viện và job cũ `7/7 chunks` hiện đủ; PNG screenshots pass magic `89 50 4E 47`. Landscape không đo được vì thiết bị từ chối shell settings write và vẫn ở rotation 0. F3 prefix/route test PASS.
- Live mục 6 trên `15e84958`, cùng một RAW QA trong Library (`005_KUMO_JP_VOL13.txt`), kế hoạch 4 chunk với độ dài `2081/2095/2085/463` ký tự, output chỉ vào `Download/QA_live_20261023`. Run 1/F1-F3: job `333`, medium, 4 lượt gọi, `$0.01237385`, cả 4 lần đầu `stop`. Run 2/F6: job `334`, high, 6 lượt gọi, `$0.06326357`, hoàn tất 4/4 chunk; hai lần `length` không do reasoning ở retry đầu, sau đó hạ high → medium và thành công; không chia nhỏ. Tổng `$0.07563742` / `$0.20`. Response provider quan sát được chỉ `OpenAI`; cache `0.0000%` run 1, `29.1126%` run 2, `17.7073%` gộp; chưa ghim provider. `STRUCTURAL_VALID` 8/8 chunk, số dòng khớp và còn dòng Nhật `0`; `SEMANTIC_EVAL=NOT_MEASURED`. Thiết lập đã khôi phục (`medium`, cost `0`, stop `false`, chunk `1700`). Sao lưu live trước: `D:/App Translate Books/App Translate Books-device-backups/v4.23-dev.23-code301/20261023-193615/`; sau: `D:/App Translate Books/App Translate Books-device-backups/v4.23-dev.23-code301/20261023-200857-post-live/`. F4 sau live còn `0` dòng `apiKey` trong `jobs/config_revisions/prepared_batches` (`334/334/249` hàng).
- **Next action:** dừng tại đây; không gọi provider, cài APK, merge, tag hoặc push nếu chưa có yêu cầu/quyền mới của owner.

- Historical context from the dev.21/UI track follows; it does not supersede the runtime-fix next action above.

- Quyết định owner mới nhất: thiết kế lại bảng đang dịch để bỏ viết tắt khó hiểu, thời gian có s, đủ tên Glossaries/Pronoun, preview dùng diện tích hữu ích và trạng thái trên cùng có chữ. Tiếp tục J3/J4/R0b/R3; skill UI/UX bản Claude + Luna audit/review readonly, coordinator kiểm lại.
- Phân xử: v4.18 trong tên tin nhắn/AGENTS là lịch sử; làm tiếp feature/v4.23 tại D:/P5E-builds/wt-v4.23. Giữ checkout v4.18/media owner, không nhánh/release/checklist mới. Dev.20 đã cài theo bằng chứng54ef672d, nên next action cũ “duyệt cài dev.20” hết hiệu lực. A2 tĩnh/A5 cấu trúc không đồng nghĩa job đang dịch/provider PASS.
- Baseline54ef672d; phone gần nhất dev.20/code298/source98a2e15e, retained installed APK SHA46C608AC…BFA6991 khớp. Lượt này không đụng phone/pilot. Mã hiện tại trước commit tài liệu deb5846a, production không đổi sau4849de19.
- Đã đóng offline: token/giây/chi phí/lỗi/luật có nhãn đầy đủ; fb bị bỏ vì service không tăng counter; status luôn có chữ và mở Công việc; stat phẳng wrap theo cỡ chữ; preview gộp heading/source, ngắn tự co/dài cuộn160–480dp. Giữ các sửa reset-preview/restore muộn/callback lifecycle của dev.20, không đổi dịch/Editorial/schema/prompt/model.
- Candidate **dev.21/code299**, production4849de19, eventbuild-20261021-223903; APK SHA2421F66B…877806, source ZIP BCB49F63…53428FC. Hai mirror/manifest/source ZIP đúng Git archive PASS. App668PASS/5SKIP mỗi biến thể, lint0Error/58Warning; core device19/19 và matrix đọc2ca×5cấu hình10/10 PASS trên AVD riêng5580. Không cộng lần lặp thành test độc lập. Compile/capture/invocation failures và bản sửa giữ trong report.
- Bảng claim/evidence/nguyên nhân/giới hạn: artifacts/qa/v4.23-readability-20261022/QA_REPORT.md. Thiết kế đã triển khai và audit các màn còn lại ở docs/V423_REFINED_UI_DESIGN.md mục9; R0a–R5 toàn app chưa hoàn tất. Wrapper test phải gọi từ đúng root; artifact test3 sai provenance bị loại, test4 từ archive sạch được dùng.
- Giới hạn: phone UI mới, TalkBack, A2 đang chạy/A4 thật và SEMANTIC_EVAL event owner NOT_MEASURED; exact prompt/schema/model/response/finish reason/token/cost/ledger event UNKNOWN. Preview vẫn giới hạn phần đuôi1200ký tự. Ba FINAL006/008/011 owner accepted và P7 current chunk scope giữ nguyên lịch sử, không gắn nhãn lại. Provider0/USD0; không push/merge/tag.
- **Nghiệm thu dev.21** (2026-10-22, owner/phiên review): màn theo dõi dịch PASS có điều kiện, chỉ trong phạm vi offline/UI. Còn mở:
  - cặp ngôn ngữ bị cắt ở màn hẹp;
  - nhãn tiếng Anh còn trộn;
  - R0a–R5 chưa xong;
  - TalkBack và kiểm trên điện thoại: NOT_MEASURED.
- **Điều phối kiểm lại:**
  - ảnh trùng SHA là bộ `screenshots-final` cũ; bộ bàn giao `screenshots-clean` hợp lệ;
  - đã tự sửa hai lỗi nhỏ có nguyên nhân chứng minh: thứ tự dòng phụ (ngôn ngữ trước provider, tối đa 2 dòng), và cột nhãn tiến độ chưa tính cỡ chữ hệ thống;
  - test 674 = 669 PASS / 5 SKIP / 0 FAIL, lint 0 Error, androidTest biên dịch được. Chạy trên bản sao HEAD cộng bản sửa; chưa build, chưa xem trên máy;
  - owner chốt 2026-10-22: thuật ngữ chuyên môn (chunk, prompt, token, model, provider, log, Glossary, Pronoun, RAW/DRAFT/FINAL) giữ tiếng Anh, nhãn thao tác Việt hóa; cho phép bật TalkBack ngắn trên điện thoại cho D6;
  - gói tiếp theo: `docs/V423_REFINED_UI_DESIGN.md` mục 10 (R3a + R2 + dev.22 trên điện thoại).
- Historical dev.21 next action (superseded): thực hiện `docs/V423_REFINED_UI_DESIGN.md` mục 10: R3a + R2, build dev.22, sao lưu và chờ owner duyệt cài, rồi thử D1–D6 trên điện thoại 15e84958.

- **2026-10-22, phân tích prompt v14 trên 1.254 chunk thật:**
  - prompt dịch ổn;
  - 4 lỗi phía app có nguyên nhân chứng minh:
    - F1: giới hạn output không chừa chỗ cho reasoning, nên chunk ngắn bị cắt;
    - F2: chunk còn dòng tiếng Nhật vẫn được chấp nhận;
    - F3: cache prompt chỉ trúng ~5%;
    - F4: API key nằm trong snapshot SQLite;
  - gói việc: `docs/V423_TRANSLATION_RUNTIME_FIX_WORK_REQUEST.md`;
  - bản nháp prompt v15 nằm ngoài Git, chưa dùng trên app.

- **2026-10-23: chẩn đoán 8 vấn đề owner nêu và rà UX toàn app (40 ảnh dev.22).**
  - Đã chứng minh:
    - đồng hồ tính lại theo giờ thật (`MainActivity:3915`);
    - reasoning high: 13 lượt gọi / $0,133 / 1 trên 7 chunk, do trần output, vòng chia nhỏ không giới hạn và không có chặn chi phí;
    - nút điều khiển nằm dưới;
    - "Màu nhấn" bị từ chối âm thầm;
    - 182/332 job không xem được;
    - bộ lọc tập ẩn.
  - Gói runtime thêm F5, F6.
  - Gói mới `docs/V423_UX_OVERHAUL_WORK_REQUEST.md` (U1–U7).
  - Không đổi mã, không gọi provider.

- **2026-10-23, điều phối kiểm độc lập dev.23** (nguồn `bc7120e6`, APK SHA `44B80CEA…D569` khớp `BUILD_INFO`):
  - archive sạch: 699 test, 5 SKIP, 0 FAIL; lint 0 Error / 58 Warning; androidTest biên dịch được;
  - không sửa tệp Editorial* hay `OpenAICompatibleClient`; không đổi schema;
  - điện thoại đang chạy code 301;
  - bản sao lưu sau cài: 0 hàng có `apiKey` trong `jobs`, `config_revisions`, `prepared_batches` (332/332/238 hàng);
  - trần chi phí giờ được kiểm trước mọi lượt gọi; Resume/Retry không có ước tính thì dùng trần $0,10;
  - F1/F3/F6 trên provider thật: đã đo trong đúng hai lượt được cấp quyền ở trên; SEMANTIC_EVAL vẫn `NOT_MEASURED`, còn F3 ghim provider chỉ là đề xuất, chưa thay đổi định tuyến.

- **2026-10-23, điều phối kiểm lại lượt live:**
  - job 333 `medium`: 4 lượt gọi / $0,0124; job 334 `high`: 6 lượt gọi / $0,0633; tổng $0,0756 trên mức duyệt $0,20; thiết lập đã khôi phục.
  - F1, F6, STRUCTURAL_VALID: PASS. SEMANTIC_EVAL: NOT_MEASURED (owner đọc `Download/QA_live_20261023`).
  - Việc tiếp theo đã ghi ở mục 6b của gói runtime:
    - F6b: ngưỡng 90% bỏ sót ca 87%, làm tốn thêm một lượt gọi $0,02;
    - F3b: lượt đầu của mọi chunk cache 0% dù cùng provider OpenAI; đề xuất `prompt_cache_key`, cần duyệt live riêng tối đa $0,03.
  - `high` tốn gấp 5,1 lần `medium`; giữ `medium` làm mặc định.
