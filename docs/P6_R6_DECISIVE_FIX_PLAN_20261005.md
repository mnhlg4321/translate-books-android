# Kế hoạch sửa dứt điểm vòng "live → một lỗi hình thức mới → vá → live" (P6 R6, v4.18)

Ngày lập: 2026-10-05. Người lập: Claude (quản lý dự án). Yêu cầu owner: "Lại bắt đầu làm việc mãi mà không có kết quả, hãy sửa dứt điểm."
Baseline: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `349717d8` (đồng bộ origin). Kiểm `git archive` sạch: engine 405/405, app 341/341.

## 1. Chẩn đoán vì sao chưa có kết quả

| # | Lượt live | Phase dừng | Mã | Bản chất |
|---|---|---|---|---|
| 1 | G1#1 | L1 RAW | `REPAIR_L1_LEDGER_INVALID` (mất chi tiết) | thiếu chẩn đoán |
| 2 | G1#2 | L1 RAW | `L1_UNIT_UNKNOWN` | model chép hash id |
| 3 | V3 | RECONCILE | `L1_TEXT_REQUIRED` | prompt bảo "để rỗng", parser bắt buộc |
| 4 | S5 | L1 RAW | `L1_UNIT_UNKNOWN:coverage.0.from` | schema minLength 3 do app |
| 5 | T3 | RECONCILE | `L1_RAW_QUOTE_NOT_IN_ANCHOR` | furigana — khoảng trống app |
| 6 | U6 | RECONCILE | `L1_DRAFT_QUOTE_NOT_IN_ANCHOR` | model chép số dòng RAW vào neo DRAFT |
| 7 | W4 | RECONCILE | `L1_DRAFT_ANCHOR_UNUSED_FIELD:findings.0.draft.after` | model điền `after` vào trường không dùng của `LINES` (cả 3 finding) |

Tiến bộ thật có: RAW qua 4 lần liên tiếp; mỗi lỗi đã gỡ không tái phát; chi phí thấp (G1 11 call, USD 0.0936 / 1.00). Nhưng **7/7 lần dừng là lỗi hình thức/ghi sổ, 0 lần là lỗi nội dung**. Nguyên nhân gốc chung:

1. **Validator phân xử mọi thứ bằng "từ chối cả response"**, kể cả các trường app tự suy ra được hoặc không ai dùng (`draft.start/end/after` đã chỉ là gợi ý từ W1; `speakerRecords` không được dùng).
2. **Mỗi lượt live chỉ lộ được lỗi đầu tiên của một response**, nên sửa từng cái một. L1 có ~63 mã từ chối; L2/L3/receipt ~205 mã và **chưa chạy live lần nào** — cùng họ lỗi sẽ lặp lại ở đó.
3. **Quy tắc dừng cả nhóm khi một fixture bị từ chối** biến mỗi lỗi hình thức thành "không có kết quả gì".

## 2. Hướng sửa dứt điểm (3 thay đổi cùng lúc, không vá từng mã)

### Z1 — Phân loại toàn bộ luật từ chối một lần (L1 + L2 + L3 + final-read + receipt)

Lập bảng **mọi** mã từ chối với một trong hai nhãn:

- **SEMANTIC (giữ từ chối):** nội dung model khẳng định mà app không kiểm được hoặc kiểm thấy sai — trích dẫn không có trong nguồn, finding không neo RAW, coverage hở/chồng, tham chiếu candidate/finding không tồn tại, id thực thể trùng, dialogue change thiếu speaker proof (đã có cơ chế hoàn nguyên), output bị cắt.
- **BOOKKEEPING (chuẩn hóa, không từ chối):** giá trị app tự suy ra hoặc không dùng — trường không dùng trong union (`after` của `LINES`, `start/end` của `MISSING`), số dòng gợi ý lệch/ngoài phạm vi (kẹp, đã làm một phần), tham chiếu trùng (đã làm), bản ghi phụ có tham chiếu sai (`speakerRecords` đã làm; xét `protectedSpans`), khoảng trắng/NFC, thứ tự phần tử, trường MAY rỗng.

Quy tắc phân loại: *nếu bỏ/sửa giá trị đó không thay đổi bất kỳ khẳng định nội dung nào đã được kiểm chứng bằng nguồn, thì là BOOKKEEPING.* Mọi chuẩn hóa ghi vào `normalizations` kèm đường dẫn; không bịa, không đoán nội dung.

Đầu ra: `docs/P6_R6_VALIDATION_RULE_CLASSIFICATION.md` (bảng mã → phase → path → nhãn → hành động) + mã sửa cho **toàn bộ** hàng BOOKKEEPING trong một lần.

### Z2 — Kiểm thử đột biến từ response thật (offline, thay vì chờ live lộ lỗi)

Từ các response thật đã lưu (RAW ×4, RECONCILE ×4) và wire tổng hợp cho L2/L3, sinh biến thể theo các lệch đã thấy và sẽ thấy: điền trường không dùng, số dòng lệch ±1/±2/ngoài phạm vi, chép số dòng RAW cho DRAFT, trường MAY rỗng/bỏ, tham chiếu trùng, bản ghi phụ trỏ dòng trống, khoảng trắng/NFC/furigana, thứ tự đảo. Kỳ vọng mỗi biến thể: **PASS kèm normalization** hoặc **từ chối SEMANTIC** — không bao giờ từ chối BOOKKEEPING. Chạy cho cả 8 phase bằng parser production. L2/L3 được "cứng hóa" trước khi chạy live lần đầu.

Thêm: L2/L3 hiện chỉ dùng `json_object`; sinh **strict json_schema** từ `EditorialFieldSpec` cho các phase này (đã có golden-wire test chống lỗi kiểu minLength), để decoder chặn lệch ngay khi sinh.

### Z3 — Đổi quy tắc chạy G1: từ chối SEMANTIC là một kết quả đo, không phải lý do dừng nhóm

- Fixture bị từ chối **SEMANTIC** → ghi `STRUCTURAL_VALID=false`, `SEMANTIC_EVAL=FAIL(reason)`, **chạy tiếp fixture kế**.
- Fixture bị từ chối **BOOKKEEPING** (Z1 sót) → ghi nhận, chạy tiếp fixture kế; sau nhóm sửa offline bằng replay + Z2.
- **Dừng nhóm** chỉ khi: UNKNOWN, chạm trần nhóm, lỗi hạ tầng (mạng/route/fingerprint), hoặc **3 fixture liên tiếp** bị từ chối cùng một mã (dấu hiệu lỗi hệ thống).
- Kết quả: một lượt G1 luôn cho ra **bảng 8 fixture** — đó là "kết quả" mà các lượt trước chưa có.

## 3. Gói việc cho Codex (một lượt, theo thứ tự)

| Gói | Việc | PASS | Dừng |
|---|---|---|---|
| **Z1** | Bảng phân loại toàn bộ mã; hiện thực mọi hàng BOOKKEEPING (kể cả `draft.after/start/end` không dùng — lỗi W4); tăng `contractRevision` | Bảng đủ 100% mã của các parser; JVM xanh | — |
| **Z2** | Bộ đột biến từ 8 response thật + wire L2/L3 tổng hợp; strict schema cho L2/L3 từ FieldSpec | 0 biến thể bị từ chối vì BOOKKEEPING; replay-all cả 4 RECONCILE thật: lỗi W4 PASS, các response cũ hoặc PASS hoặc chỉ còn lỗi SEMANTIC đã biết | Phát hiện mã mới không phân loại được → thêm vào bảng, không bỏ qua |
| **Z3** | Sửa `scripts/p6/run_group.ps1` + runner theo quy tắc chạy mới; test quy tắc dừng (UNKNOWN, trần, 3 lỗi cùng mã, hạ tầng) | Test xanh | — |
| **Z4** | Build wrapper, archive, emulator: preflight, fake CHAIN 14/14, negative gate | Đạt, 0 call | — |
| **Z5** live | **G1 đủ 8 fixture + 2 lượt lặp** theo bảng R5 trong phần trần còn lại (USD 0.9064) với quy tắc Z3; chấm `score_run.py` | **Bảng G1 hoàn chỉnh**: theo fixture `STRUCTURAL_VALID`, `SEMANTIC_EVAL`, finding đúng/sai so với lỗi gieo, call/token/USD | Dừng theo Z3 |

**Cổng quyết định sau Z5 (để không lặp vô hạn):** nếu ≥ 6/8 fixture qua cấu trúc L1 → sang G2 (L2/L3). Nếu < 6/8 và lỗi chủ yếu ở hình thức RECONCILE → đổi kiến trúc RECONCILE sang đầu ra văn bản có thẻ như luồng dịch (đánh giá A/B trên cùng fixture, ngân sách riêng) thay vì tiếp tục vá JSON.

## 4. Chi phí

Z1–Z4: USD 0. Z5: tối đa phần còn lại của G1 (USD 0.9064, đã duyệt); ước tính thực ~USD 0.25 cho 24 call (~USD 0.01/call đo được).

## 5. Quyết định cần owner

**D-Z3:** thay quy tắc dừng của G1 bằng Z3 (từ chối là kết quả đo; chỉ dừng nhóm khi UNKNOWN/trần/hạ tầng/3 lỗi cùng mã liên tiếp). Khuyến nghị **đồng ý** — đây là thay đổi then chốt để một lượt live cho ra kết quả thay vì dừng ở lỗi đầu tiên. Z1–Z4 offline, không cần quyền thêm.

## 6. Quyết định owner

**D-Z3 đã được owner duyệt (2026-10-05, "đồng ý"):** quy tắc chạy G1 đổi theo Z3 — fixture bị từ chối (SEMANTIC hoặc BOOKKEEPING sót) được ghi là kết quả đo và chạy tiếp fixture kế; chỉ dừng nhóm khi UNKNOWN, chạm trần nhóm (phần còn lại USD 0.9064), lỗi hạ tầng (mạng/route/fingerprint), hoặc 3 fixture liên tiếp bị từ chối cùng một mã. Z1–Z4 làm offline trước; Z5 chạy đủ G1 (8 fixture + 2 lượt lặp) rồi dừng trước G2. 0 retry tự động, 0 repair call, không đụng pilot.
