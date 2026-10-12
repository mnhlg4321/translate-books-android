# P6 R6 G1 resume execution — 2026-10-03

Phạm vi: `docs/P6_R6_G1_RESUME_WORK_REQUEST_20261003.md`, owner đã duyệt D-G1 ở mục 6. Không ghi API key hoặc fingerprint vào Git; chỉ dùng emulator `emulator-5554`, không truy cập pilot `15e84958`.

## W1 — build, cài và kiểm offline

- Source build: commit `a178ff971590f3a4e15e28555e8b21793f312ae8` trong worktree tạm `D:\P5E-builds\wt-p6-g1-resume-20261003`.
- Production wrapper đạt: `4.18-p6.15`/code `228`, event `build-20261003-200206`; APK SHA-256 `AB0AAA1EA0A6DE8C79678A5A9D8F82413338935EA8491F03CC25C908CD38E3F0`; source ZIP SHA-256 `EB4BBCAC955791AFBD161B70B98EBA71DE087ABD663D02A0D951142A6F0DA76A`. Payload parity đạt tại `artifacts/builds/v4.18-p6.15/build-20261003-200206` và `backup/builds/v4.18-p6.15/build-20261003-200206`.
- AndroidTest wrapper đạt: event `p6-r6r7-a178ff97-20261003-15`; APK SHA-256 `7ECB59586D61D1AFF788869C4D284DFC85FF9BC8082690F99E568F2F2E16B78F`; source ZIP trùng production. Payload parity đạt tại `artifacts/test-builds/v4.18-p6.15/p6-r6r7-a178ff97-20261003-15` và `backup/test-builds/v4.18-p6.15/p6-r6r7-a178ff97-20261003-15`.
- Production APK được cài/đọc lại trên `emulator-5554` bằng wrapper; AndroidTest APK archived được cài bằng serial tường minh. Không có thao tác pilot. Lượt host đầu tiên thiếu `adb` trong `PATH` và dừng trước thiết bị; chạy lại với SDK `platform-tools` đã đạt, không phát sinh call.
- Preflight suite đạt `5/5`, coordinator fake chain đạt `1/1`. W1 fake CHAIN run `b4b1f4f8-1cf7-4f58-9cd1-6a8bd7ac9d8e` đạt `STRUCTURAL_VALID 14/14`, `actualProviderCalls=0`, `pending UNKNOWN=0`; scorer offline ghi `SEMANTIC_EVAL FAIL 12 / PASS 2`, chỉ là fake/no-edit control.

## W2 — typed stop offline

- Commit `a178ff97` thêm cờ test-only `p6_fake_invalid_l1=YES`, chỉ cho fake `L1_ONLY` và bị từ chối khi live; fake RAW trả coverage hở. Verifier có cờ riêng `--expect-invalid-l1` để đọc negative artifact, không nới kiểm tra live.
- Run `8d7b2a1c-7cbe-4c21-9a87-3e58bf0e6d42`, fixture `fx-a03`: `structural.valid=false`, `stage=L1`, `reasonCode=P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`, `stops` gồm `phase=L1_RAW_DISCOVERY` và `L1_COVERAGE_GAP`. Có `1` fake call, `actualProviderCalls=0`, ledger `2` dòng `RESERVE/SETTLE`, cost `USD 0`, pending `0`.
- `verify_fixture_run.py --expect-invalid-l1` và `score_run.py` cùng đọc được kết quả: `STRUCTURAL_VALID=0/1`, `SEMANTIC_EVAL=FAIL` do fixture vẫn là DRAFT. Đây là kiểm đường dừng, không phải đánh giá chất lượng dịch.

## W3 — G1 live resume và dừng nhóm

- Group giữ nguyên: `G1-20261003-ecbf8c55`, cap `USD 1.00`; run mới `f7f71d89-9cc2-47fb-a8c7-4fd9e6d9bc2e`, bắt đầu `fx-a03`. Ledger trước lượt có `USD 0.0081852` từ attempt cũ; precheck còn đủ trần.
- `fx-a03` gửi đúng `1` call `L1_RAW_DISCOVERY`. Response đã biết cost: `inputTokens=22172`, `outputTokens=1764`, `finish=stop`, `USD 0.00478465`, `unknownCostCalls=0`. Runner ghi artifact trước khi host dừng nhóm.
- Mã dừng đầy đủ: `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID`; chi tiết engine `L1_UNIT_UNKNOWN`; phase `L1_RAW_DISCOVERY`. Vì bị từ chối ở L1, không dispatch L1 reconcile, không chạy fixture tiếp theo, không retry và không repair call.
- Ledger G1 sau lượt: `4` dòng, `2` call, `USD 0.01296985` settled/exposure, còn `USD 0.98703015`, pending UNKNOWN `0`. Scorer cho artifact live partial: `STRUCTURAL_VALID=0/1`; `SEMANTIC_EVAL=FAIL` (`KNOWN_DEFECT_NOT_FIXED:T-S1`) vì final vẫn là DRAFT, không phải phán quyết chất lượng model.
- G2 chưa bắt đầu. Evidence riêng: `D:\P5E-private\p6-runs\f7f71d89-9cc2-47fb-a8c7-4fd9e6d9bc2e\`; W1: `D:\P5E-private\p6-runs\b4b1f4f8-1cf7-4f58-9cd1-6a8bd7ac9d8e\`; W2: `D:\P5E-private\p6-runs\8d7b2a1c-7cbe-4c21-9a87-3e58bf0e6d42\`.

## Kết luận và bước tiếp theo

W1–W2 đạt; W3 đã chứng minh runner giữ được mã từ chối cụ thể và dừng cả nhóm đúng quy tắc sau một call thật. Bước duy nhất tiếp theo là chẩn đoán offline `L1_UNIT_UNKNOWN`, sau đó xin owner quyết định trước mọi live call tiếp theo; không bắt đầu G2.
