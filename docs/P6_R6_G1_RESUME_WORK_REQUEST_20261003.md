# Yêu cầu làm việc — P6 R6: chẩn đoán lần dừng đầu của G1 và chạy tiếp G1 (v4.18)

Ngày lập: 2026-10-03. Người giao: owner. Người lập: Claude (review `a9d82e40`, chẩn đoán offline, sửa lỗi nhỏ).
Repo `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, mốc = commit chứa tài liệu này. Quy tắc, công thức máy và quyền Q1–Q5: như `docs/P6_R6_R7_CODEX_WORK_REQUEST_20261003.md` và `docs/P6_R6_LIVE_READINESS_WORK_REQUEST_20261003.md`.

## 1. Sự thật đã kiểm

- `a9d82e40` đồng bộ origin; hai commit mới chỉ là tài liệu (`docs/P6_R6_G1_LIVE_PREFLIGHT_20261003.md`, `docs/P6_R6_G1_LIVE_EXECUTION_20261003.md`, BUILD_STATE, snapshot).
- Preflight: sai fingerprint cố ý → `P6_LIVE_FINGERPRINT_MISMATCH`, 0 call; fingerprint owner tạo lại → `MATCH`, 0 call. Mạng emulator thông.
- G1 lượt 1 (`G1-20261003-ecbf8c55`, run `c2f57361-…`): fixture `fx-a03`, **1 call** `L1_RAW_DISCOVERY`. Logcat `P5E_RAW`: `finish=stop contentBytes=7330 prompt=21999 completion=2238 reasoning=96 cost=0.0081852` → response **đầy đủ**, không cắt. App từ chối: `REPAIR_L1_LEDGER_INVALID`. Sổ chi tiêu: 1 call, USD **0.0081852** / trần 1.00, 0 UNKNOWN. Không RECONCILE, không fixture khác.
- `STRUCTURAL_VALID 0/1`, `SEMANTIC_EVAL` không có dữ liệu. **Đây không phải kết quả chất lượng.**

## 2. Chẩn đoán

| Phát hiện | Chứng cứ | Xử lý |
|---|---|---|
| Mã lỗi cụ thể bị mất | Engine ghi `EditorialL1Ledger.safeMessage` (mã có kiểu như `L1_COVERAGE_GAP`, `L1_TEXT_TOO_LONG`, `L1_UNIT_UNKNOWN`) vào `stopReceipt.evidenceRefs` (`EditorialP5PilotExecution.java` ~369–373), nhưng runner ném `IllegalStateException(reasonCode)` và bỏ chi tiết (`EditorialP6FixtureRunnerInstrumentedTest.java` ~252); response không được lưu theo chính sách | **Đã sửa (Claude):** runner bắt dừng có kiểu ở L1/L2/L3 bằng `StageStop`, ghi `structural.json` `valid=false`, `stage`, `providerCalls`, và `stops` = mã dừng + `phase=…` + mã chi tiết an toàn của engine; không còn crash mất `structural.json` |
| Prompt thiếu dữ kiện coverage mà validator bắt buộc | Validator đòi phủ kín, liên tiếp, không chồng 191 unit (`EditorialRawInventory.java:114-136`); prompt không nêu số unit, id đầu/cuối, luật nối dải; luật `candidateId` và độ dài `note` chỉ nêu mơ hồ (cùng loại lỗi event 6) | **Đã sửa (Claude):** `OpenRouterEditorialP5PilotProvider.coverageFacts` thêm vào prompt L1 (RAW và RECONCILE): số unit chính xác, id đầu/cuối, `coverage[0].from`/range cuối, không bỏ/lặp unit, vài dải lớn, regex + tính duy nhất của `candidateId`, sao chép id kể cả 8 hex, `note` ≤ 80 ký tự kể cả khoảng trắng. Test `OpenRouterLedgerCoverageFactsTest` 2/2 |
| Nguyên nhân thật chưa chứng minh | Response không lưu; ba giả thuyết theo thứ tự khả năng: (1) coverage hở/chồng/dừng sớm, (2) `note` > 80 hoặc `candidateId` sai/trùng, (3) chép sai id/hash | Lượt kế sẽ ghi đúng mã nhờ sửa runner. Không nới validator khi chưa có mã thật |

Kiểm sau sửa: app unit **341/341**, lint PASS, androidTest compile PASS (engine không đổi). Bản sửa prompt đổi bytes request L1 → cần build lại APK production + test.

## 3. Quyết định cần owner (một lần)

**D-G1:** cho phép **chạy tiếp G1 trong phần trần còn lại** (USD 1.00 − 0.0081852 ≈ 0.99), bao gồm chạy lại `fx-a03` như một attempt mới (attempt hỏng giữ nguyên trong sổ), với quy tắc dừng mới ở mục 4. Khuyến nghị: **đồng ý** — chi phí mỗi lần thử L1 ≈ USD 0.008, rủi ro nhỏ, và chỉ có dữ liệu thật mới chọn được giữa ba giả thuyết.

## 4. Gói việc cho Codex

| Gói | Việc | PASS | Dừng |
|---|---|---|---|
| **W1** offline | Build production + AndroidTest bằng wrapper từ HEAD (worktree tạm), archive hai nơi, cài lên `emulator-5554`; chạy lại: preflight suite, coordinator fake chain, fixture fake `CHAIN` 14/14 `STRUCTURAL_VALID` | Đạt, 0 call thật | Lỗi mã → sửa trong gói |
| **W2** offline | Kiểm đường dừng có kiểu của runner: thêm tùy chọn chỉ cho test (`p6_fake_invalid_l1=YES`) để fake L1 trả wire hở coverage; chạy một fixture fake → `structural.json` `valid=false`, `stage=L1`, `stops` chứa `L1_COVERAGE_GAP`; scorer/verify đọc được | Đạt, 0 call thật | — |
| **W3** live, chỉ khi D-G1 được duyệt | Chạy lại G1 cùng group ledger (tiếp tục cộng dồn từ USD 0.0081852): `fx-a03` trước. **Quy tắc dừng mới:** một fixture L1 bị từ chối → dừng cả nhóm ngay, báo cáo mã chi tiết (không chạy fixture tiếp theo); nếu `fx-a03` qua L1 (RAW + RECONCILE) thì chạy phần còn lại của G1 theo bảng, mọi luật cũ giữ nguyên (0 retry, UNKNOWN dừng, trần nhóm kiểm trước mỗi fixture) | Báo cáo G1: `STRUCTURAL_VALID`/`SEMANTIC_EVAL` theo fixture, call/token/USD thật, mã dừng nếu có | Từ chối lần nữa → sửa offline theo đúng mã, xin owner trước lượt kế; không đổi trần |

Không làm trong yêu cầu này: G2/G3/G4/R7; nới validator (ví dụ tự cắt `note`) — chỉ đề xuất nếu mã thật cho thấy cần, kèm phân tích rằng không làm yếu tính toàn vẹn.

## 5. Báo cáo cuối

1. Điều mới được chứng minh. 2. Call/chi phí thật so với trần còn lại. 3. Mã từ chối cụ thể nếu có. 4. Commit/push. 5. Đúng một bước tiếp theo.

## 6. Quyết định owner

**D-G1 đã được owner duyệt (2026-10-03, "đồng ý D-g1"):** chạy tiếp G1 trong phần trần còn lại của nhóm (USD 1.00 − 0.0081852 đã tiêu), chạy lại `fx-a03` như attempt mới, dừng cả nhóm ngay khi một fixture bị từ chối ở L1. Các luật khác giữ nguyên: 0 retry, 0 repair call, UNKNOWN dừng, trần nhóm kiểm trước mỗi fixture, G2 chưa được bắt đầu.
