# P6 R6 offline replay repair — 2026-10-04

Phạm vi: sửa công cụ replay và validator diagnostic trên branch hiện hành, kiểm bằng dữ liệu tổng hợp và replay response đã lưu ngoài Git. Không provider call, không emulator/pilot, không G1/G2.

## Thay đổi

- `EditorialWireReplayTool` xuất report JSON schema 2: phase, response SHA-256, actual/expected status, expectation hash/codes, danh sách `CODE:path`, phần bị bỏ qua do dependency, phần đã kiểm và kết luận từng case/cả lượt.
- Manifest `replay-all` yêu cầu expectation tường minh. `PASS` và `EXPECTED_REJECT` đều hash-bound; `EXPECTED_REJECT` phải khớp chính xác danh sách mã. Cú pháp sáu tham số cũ chỉ là `UNSPECIFIED` diagnostic và được ghi trong report, không gắn ngầm vào fixture nào.
- Exit code 0 chỉ khi mọi expectation đạt. Validation/input/JSON/expectation/tool failure trả exit 2. Production `parseReconcile` vẫn fail-fast.
- `EditorialL1Ledger.diagnoseReconcile` kiểm từng finding độc lập bằng cùng hàm production. Phạm vi diagnostic có giới hạn ở `findings[]`; khi có lỗi, resolutions/speakerRecords/protectedSpans/post-finding invariants được ghi `skipped`. Khi findings đều hợp lệ, production parser kiểm nốt phần còn lại.
- Không đổi matcher để tự reanchor, thay quote, đi tìm toàn chương hoặc tạo proof thay model.

## Regression

Engine `391/391` PASS. Process regression `EditorialWireReplayToolTest` `6/6` PASS: response hợp lệ exit 0; lỗi ngoài kỳ vọng exit 2; EXPECTED_REJECT đúng hash và mã exit 0 nhưng actual vẫn REJECTED; actual PASS khi kỳ vọng reject, hash sai, mã sai, file thiếu và JSON hỏng đều exit 2; output không có khóa/header/model. Hai finding độc lập cùng sai được thu thập theo thứ tự ổn định; production replay chỉ báo lỗi đầu và đánh dấu phần còn lại.

`EditorialL1AnchorRegressionTest` `6/6` PASS với exact/range, dòng lân cận, quote trùng, namespace RAW/DRAFT, NFC/CRLF/trim, furigana, quote rỗng sau chuẩn hóa, MISSING, out-of-range và dạng U6 (anchor DRAFT 99 nhưng quote DRAFT 101).

## Replay U6

- Response: `D:\P5E-private\p6-runs\3559de99-978b-410e-b863-c082062187fa\results\fx-a03\responses\002-L1_RECONCILE.json`
- SHA-256: `18c5ddb34caf6d197b346ad4b86e8b785b6e8bae5cc96bdeea9de12e0ea126e3`
- Manifest/log ngoài Git: `D:\P5E-private\p6-item12-u7-replay-all-manifest-20261004.json`, `D:\P5E-private\p6-item12-u7-replay-all-20261004.json` (log SHA-256 `324ddc7e2779b4e256d250ee8c315fca2c71e4f7ef4c0a9e9e2bf346ce4b69fd`). Replay-all exit `0`: old RAW actual `REJECTED`/expected `EXPECTED_REJECT` với `L1_UNIT_UNKNOWN:coverage.0.from`; current RAW actual `PASS`/expected `PASS`; RECONCILE actual `REJECTED`/expected `EXPECTED_REJECT` với `L1_DRAFT_QUOTE_NOT_IN_ANCHOR:findings.0.draftQuote`. Diagnostic còn thấy finding độc lập `findings.1` hợp lệ về cấu trúc và ghi rõ phần dependent bị skip. Đây không phải REPORT_L1 hợp lệ và không phải semantic PASS.

## Phân loại semantic và gánh bằng chứng

Finding `E182-MEANING` dùng RAW `L183` và DRAFT `182`; cấu trúc neo/quote qua validator, nhưng phần quan hệ gia đình cụ thể hơn không được đoạn RAW được trích dẫn chứng minh đủ. Phân loại: `UNRESOLVED_SUSPECTED_FALSE_POSITIVE`; không thêm mã vào expected list và không tuyên bố chất lượng model.

Prompt thực dùng tách source role RAW/DRAFT và số dòng, nhưng yêu cầu model chép cả quote. App có thể tự lấy văn bản theo anchor chỉ khi đổi contract; khi đó quote không còn là bằng chứng model đã so sánh. Vì vậy gói này chỉ ghi đề xuất contract version mới với invariant liên kết finding, RAW evidence và DRAFT anchor; không triển khai semantic simplification.

## Trạng thái và bước tiếp theo

Gói offline đạt điều kiện nghiệm thu; chi phí provider của gói này là `USD 0` và không có call. G1 ledger vẫn `USD 0.07381520 / 1.00`, còn `USD 0.92618480` theo lần đọc đã ghi trước đó; không tự dùng số này để dispatch. Bước tiếp theo duy nhất là owner xem đề xuất G1 mới gồm giả thuyết, source/prompt/schema revision, fixture, số call tối đa, ngân sách, điều kiện dừng và cách chấm. Không live trong gói này.
