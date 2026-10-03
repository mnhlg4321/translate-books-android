# Yêu cầu làm việc — P6 R6: tham chiếu unit theo số dòng (wire v3) rồi chạy tiếp G1 (v4.18)

Ngày lập: 2026-10-03. Người giao: owner. Người lập: Claude (review `a178ff97`/`a14e1661`, chẩn đoán offline).
Repo `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, mốc = commit chứa tài liệu này. Quy tắc, công thức máy, quyền Q1–Q5: như `docs/P6_R6_R7_CODEX_WORK_REQUEST_20261003.md`, `docs/P6_R6_LIVE_READINESS_WORK_REQUEST_20261003.md`, `docs/P6_R6_G1_RESUME_WORK_REQUEST_20261003.md`.

## 1. Sự thật đã kiểm

- `a14e1661` đồng bộ origin. Commit mới chỉ đổi androidTest runner (tùy chọn test-only `p6_fake_invalid_l1`, không có trong `src/main`), `verify_fixture_run.py` và tài liệu; không key/fingerprint/văn bản sách. Test Python: score 16, spend 3, verify 9, prompt-inputs 5 — đạt. Mã production không đổi so với lần kiểm 341/341.
- W1–W2 đạt (code228 trên emulator; fake CHAIN 14/14 `STRUCTURAL_VALID`; negative gate trả `L1_COVERAGE_GAP` có kiểu).
- W3: `fx-a03`, 1 call `L1_RAW_DISCOVERY`, `finish=stop`, 22,172 vào / 1,764 ra, USD 0.00478465; từ chối `REPAIR_L1_LEDGER_INVALID` → **`L1_UNIT_UNKNOWN`** (`structural.json` của run `f7f71d89-…`). Sổ G1 cộng dồn **USD 0.01296985 / 1.00**, 0 UNKNOWN. Đúng quy tắc dừng nhóm.
- **Phía app nhất quán:** Claude dựng lại inventory độc lập từ `RAW.txt` đã đẩy lên thiết bị (bỏ BOM, bỏ dòng trống/U+3000/marker ảnh, `u:<dòng>:<8 hex sha256>`) → 191 id, **trùng 100%** với 191 id trong prompt đã gửi. Vậy model đã trả một `unitId` không tồn tại: chép sai hash/số dòng hoặc tự tạo id.

## 2. Phán đoán và quyết định kỹ thuật

Hai lượt thật liên tiếp bị từ chối trong cùng họ lỗi "giữ sổ id/coverage" (lượt 1 mất mã, lượt 2 `L1_UNIT_UNKNOWN`; lượt 2 đã có prompt nêu rõ luật chép id). Theo quy tắc chống vòng lặp, **đổi cách làm** thay vì thêm chữ vào prompt: bắt model chép 8 ký tự hex cho hàng chục tham chiếu là yêu cầu dễ vỡ, không thêm tính toàn vẹn (hash do app tính từ bytes RAW đã ghim).

**Wire v3 — tham chiếu unit bằng số dòng:**
- Prompt hiển thị RAW dạng `L<dòng>|<text>` chỉ cho các dòng là unit (không hiện hash). Model tham chiếu `L<dòng>` (chuỗi), ở `unitId` của candidate, `from`/`to` của coverage, và mọi anchor RAW trong finding/probe của các wire dùng chung grammar.
- App phân giải `L<dòng>` → unit trong inventory (kèm hash) một cách xác định. Dòng ngoài phạm vi → `L1_UNIT_UNKNOWN`; dòng không phải unit (trống/marker ảnh) → `L1_UNIT_LINE_NOT_A_UNIT`; sai cú pháp → `L1_UNIT_REF_INVALID`. Coverage vẫn phải là phân hoạch kín các dòng unit (kiểm như cũ, trên id đã phân giải).
- REPORT_L1 và các artifact lưu bền vẫn ghi **id đầy đủ** `u:<dòng>:<hash>` (do app chuyển đổi) → L2/L3, readback và bộ chấm không đổi nghĩa.
- Áp dụng cho mọi wire dùng grammar RAW-pass/anchor: L1 RAW, L1 RECONCILE, L2_RAW_DISCOVERY, L3_RAW_FIRST_REAUDIT, và anchor RAW trong probe L3 nếu có. Tăng nhãn wire lên v3 và `contractRevision` (gắn vào attempt identity) để không tái dùng attempt v2. Không có dữ liệu v2 sản phẩm cần giữ ngoài evidence fixture.
- Không nới các kiểm tra khác (coverage, độ dài note, id candidate).

**Phương án dự phòng (chưa làm):** nếu sau v3 vẫn bị từ chối vì coverage, cân nhắc để model chỉ khai `coverageComplete` + danh sách dải PRESERVED, app suy coverage; quyết định chỉ khi có mã từ chối thật.

## 3. Gói việc cho Codex

| Gói | Việc | Kiểm thử | PASS | Dừng |
|---|---|---|---|---|
| **V1** offline | Hiện thực wire v3 trong engine (`EditorialL1Ledger`, `EditorialL1LedgerRun`, chỗ dùng chung ở L2/L3), provider (`OpenRouterEditorialP5PilotProvider.renderUnits` → `L<dòng>|`, `coverageFacts` nêu dòng đầu/cuối và số unit; adapter L2/L3), JSON schema (pattern `^L[1-9][0-9]*$` cho tham chiếu), contract revision + identity | JVM: tham chiếu hợp lệ phân giải đúng id đầy đủ; ngoài phạm vi / dòng trống / marker ảnh / sai cú pháp → đúng mã; coverage partition trên dòng; REPORT_L1 round-trip chứa id đầy đủ; prompt không còn chuỗi `u:<n>:<8hex>`; golden identity v2 cũ vẫn tính như trước | engine + app unit, lint, androidTest compile | Hai vòng cùng lỗi → đổi cách, ghi lý do |
| **V2** offline | Build wrapper, archive hai nơi, cài emulator; preflight, coordinator fake chain, fixture fake CHAIN 14/14, negative gate W2 | Như cũ | Đạt, 0 call thật | — |
| **V3** live (cần D-G1b) | Tiếp tục **cùng group ledger G1** (đã tiêu USD 0.01296985): `fx-a03` trước, giữ quy tắc dừng nhóm khi một fixture bị từ chối ở L1; nếu qua thì chạy phần còn lại của G1 theo bảng | Báo cáo theo fixture: `STRUCTURAL_VALID`/`SEMANTIC_EVAL`, call/token/USD, mã dừng | Theo ngưỡng đóng băng | Từ chối → dừng, báo mã, không chạy tiếp |

## 4. Quyết định cần owner

**D-G1b:** sau khi V1–V2 đạt, cho phép chạy tiếp G1 trên wire v3 trong phần trần G1 còn lại (≈ USD 0.987), cùng quy tắc dừng nhóm khi bị từ chối ở L1. Khuyến nghị: **đồng ý** — thay đổi bỏ được nguyên nhân dễ vỡ nhất mà không giảm tính toàn vẹn; mỗi lần thử ≈ USD 0.005–0.008.

## 5. Báo cáo cuối

1. Điều mới được chứng minh. 2. Call/chi phí thật so với trần. 3. Mã từ chối nếu có. 4. Commit/push. 5. Đúng một bước tiếp theo.

## 6. Quyết định owner

**D-G1b đã được owner duyệt (2026-10-03, "đồng ý D-G1b"):** sau khi V1–V2 đạt, chạy tiếp G1 trên wire v3 trong phần trần G1 còn lại (USD 1.00 − 0.01296985 đã tiêu), bắt đầu `fx-a03`, dừng cả nhóm ngay khi một fixture bị từ chối ở L1. Giữ nguyên: 0 retry, 0 repair call, UNKNOWN dừng, trần nhóm kiểm trước mỗi fixture, chưa bắt đầu G2.
