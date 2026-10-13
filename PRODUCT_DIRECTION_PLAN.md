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

- App phát hành: v4.18.0 / code 266, source `82626ac8`, tag `v4.18.0` trên `9d02976e`, `main` `998da341`.
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
| E | chưa bắt đầu | Chờ quyết định owner sau trang S7; không tag/merge |

Dữ liệu bước A/B (có văn bản sách) ở `D:\P5E-private\product-plan-A\`, ngoài Git.

**Next action:** owner đọc `D:/P5E-private/chunk-runs/V420-D/READING_014.html` và quyết định phát hành hay bỏ C1 theo mục7; dừng sau S9, không lượt trả phí tiếp theo.

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
