# Yêu cầu làm việc cho Codex — P6 R6: đóng các khoảng trống trước khi gọi provider (v4.18)

Ngày lập: 2026-10-03. Người giao: owner. Người lập: Claude (review công việc P0–P4 của Codex, kiểm chứng độc lập, phản biện).
Repo `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, mốc `fbde77f6` (đã push; trước đó 23 commit chưa được push). Không tạo branch/release/checklist mới.

Đọc trước: `docs/P6_R6_R7_CODEX_WORK_REQUEST_20261003.md` (quyền Q1–Q5, quy tắc, công thức máy ở §7), `docs/P6_R6_R7_OFFLINE_EXECUTION_20261003.md`, `docs/P6_R5_EXECUTION_BUDGET_TABLE.md`, `docs/P6_R5_EVALUATION_PROTOCOL.md`. Quyền Q1–Q5 không đổi. Tài liệu này **thay** thứ tự P5→P9 của yêu cầu trước bằng gói L0–L9 dưới đây cho tới khi G1 được phép bắt đầu.

## 1. Kết quả review P0–P4 (đã kiểm độc lập)

| Hạng mục | Kết quả |
|---|---|
| Test từ `git archive` sạch của `fbde77f6` | engine 340/340, app unit 331/331, lint PASS, androidTest compile PASS, `test_score_run.py` 16/16, `test_verify_fixture_run.py` 2/2 |
| Rò rỉ trong 23 commit mới | Không có key, fingerprint hay văn bản sách; mọi 64-hex là hash APK/ZIP/fixture có nhãn |
| Giá P4 | Đúng nguồn chính thức; worst-case dùng mức cache-write 0.25/M là thận trọng hợp lý; tổng R6 2.724 dưới trần 3.25 |
| Build/emulator | code224 archive hai nơi; 14/14 fixture `STRUCTURAL_VALID` với fake provider; regression an toàn 78/78 |

| Kế hoạch | Đánh giá | Bằng chứng |
|---|---|---|
| C1 L1 trong sản phẩm | **PARTIAL** | Nút chạy và hộp thoại 8 call đạt (`EditorialChapterFinalPanel` ~100–210, `EditorialChainBudgets.describe`). **Lỗi:** panel gọi `runFromL1` cả khi `RUN_STAGE_WITH_AUTHORIZATION` (dòng 102–103, 204) → chương có L1 legacy (chương 001 binding cũ trên pilot) sẽ phát sinh RAW ledger-v2 mới trên binding cũ, trái Q2. Chưa có test ngắt L1 giữa chừng để chứng minh không dispatch lại (test UNKNOWN hiện chỉ cho L3). Mã chết: `withFreshLedgerL1LifecyclePersistence`, nhánh `L1_LEDGER` trong `freshPhaseAllows` |
| C2 runner fixture gọi thật | **NOT MET — chặn G1–G3** | Runner chỉ dựng `FakeL1/FakeL2/FakeL3` (`EditorialP6FixtureRunnerInstrumentedTest` ~120–122, 545–593), `providerKind=FAKE_OFFLINE`; không có cổng live, không đọc `SettingsStore`, không đối chiếu fingerprint owner đưa |
| C3 chống lộ đáp án | **PARTIAL** | An toàn vì predecessor rỗng, nhưng: (a) L2-only với ledger rỗng không đo được "L2 xử lý đủ finding"; (b) leak guard là script host sau khi chạy, chỉ kiểm prompt `L2_EDIT` |
| C4 trần ngân sách | **PARTIAL** | Sổ chi tiêu bền, đặt chỗ worst-case trước call, chặn khi cost UNKNOWN: tốt. **Lỗi:** `groupId = runId|fixtureId|mode` nên trần G1/G2/G3 (1.00/1.00/0.75) **không** được áp xuyên các fixture; `run_group.ps1` không dừng theo trần nhóm |
| C6 | MET (0 vòng sửa sau final-read, giữ để đo) | — |
| C8 | MET (191 unit) | — |
| C9 | OPEN | Không có danh sách 22 lỗi lịch sử; mới sửa 13 assertion v24 |
| Khác | Owner nói key đã lưu trong app Settings nhưng chưa rõ **thiết bị nào** (emulator cho G1–G3, pilot cho G4) | Phải xác nhận trước G1 |

## 2. Phản biện và quyết định kỹ thuật

1. **"P4 xong, sẵn sàng P5" là chưa đúng**: không có đường gọi provider cho fixture. Làm L1 dưới đây trước mọi chi tiêu.
2. **Trần nhóm phải ở cấp nhóm**, không ở cấp fixture: dùng một sổ chi tiêu cho cả nhóm (`groupId = G1|<groupRunId>`), dùng chung giữa các fixture, cộng dồn; host kiểm trước mỗi fixture rằng phần còn lại ≥ worst-case của fixture đó.
3. **G2 L2-only đổi thiết kế (không tốn thêm call, trong Q1):** thay vì predecessor L1 rỗng, chạy `fx-a04`, `fx-a11`, `fx-a02` ở chế độ `L1_THEN_L2` trong cùng DB tạm: L1 thật (đã tính trong G1) rồi L2 thật (3 call/fixture, đúng số call L2-only cũ). Như vậy L2 nhận ledger thật của model, không có nhãn, và đo được việc xử lý từng finding. Ba lượt L1 tương ứng của G1 được tái dùng (không chạy lại L1). L3-only giữ nguyên (VI_L2 = DRAFT có lỗi gieo, L1 rỗng là đúng vì L3 phải tự tìm).
4. **Leak guard phải chạy trước dispatch**, trên **mọi** prompt (L1 RAW/RECONCILE, L2 discovery/edit/final-read, L3 reaudit/reconcile/final-read): host kiểm template + nguồn đẩy lên trước khi chạy; thiết bị ghi lại bytes prompt đã gửi để host kiểm lại sau.
5. **Fingerprint**: runner live (emulator, G1–G3) đọc key từ `SettingsStore` trên thiết bị, tính fingerprint như M4, so với giá trị owner đưa qua instrumentation argument; lệch → dừng trước call, 0 call. UI sản phẩm (G4, owner bấm) không cần fingerprint vì owner là người dùng; nếu Codex chạy G4 bằng runner opt-in thì dùng cùng kiểm tra.
6. **Không dùng `RUN_STAGE_WITH_AUTHORIZATION` để vào L1**: nếu binding đã có L1 commit (legacy hay v2) thì chỉ chạy tiếp L2/L3 theo contract tương ứng; L1 legacy → hiển thị "chuỗi legacy, tạo binding mới để chạy contract v2", không có nút chạy L1.
7. **Branch/tài liệu**: giữ branch hiện tại (AGENTS.md cấm mở track mới). Banner "Active continuation note (2026-09-14)" trong `AGENTS.md` đã cũ (nói về audit branch v4.17/p5e-audit) → đề xuất owner duyệt sửa banner trỏ tới `EDITORIAL_RECOVERY_V4_18.md` mục 10 và branch hiện tại; Codex chỉ sửa khi owner đồng ý (AGENTS.md là file hướng dẫn của owner). Tên branch `p5e-runner-repair` không còn phản ánh phạm vi nhưng đổi tên lúc này phá liên kết evidence; xử lý ở bước merge/release theo `GIT_WORKFLOW.md`.

## 3. Gói việc

| Gói | Việc | Kiểm thử / đầu ra | PASS | Dừng |
|---|---|---|---|---|
| **L0** | Xác minh branch/HEAD/status = `fbde77f6` hoặc mới hơn; push nếu ahead | — | — | Lệch không giải thích được |
| **L1** C2 | Runner fixture: cổng live riêng `p6_fixture_live=YES` + argument `p6_expected_endpoint_account_fingerprint`; khi live dùng đúng adapter production (`OpenRouterEditorialP6L1Provider`, L2/L3 provider) bọc `EditorialP6Budgeted*Provider`; đọc `SettingsStore`, kiểm `EditorialP5EFreshRawRoutingPolicy.matches`, so fingerprint; không log/ghi key; `structural.json` ghi `providerKind=LIVE`, call/token/USD thật từ provider | JVM/instrumented: thiếu cổng → fake; live thiếu fingerprint/route sai/fingerprint lệch → dừng trước dispatch, **0 call** (chạy được trên emulator mà không tốn tiền vì dừng trước call) | Mọi nhánh từ chối trả 0 call | Hai vòng cùng lỗi → đổi cách |
| **L2** C4 | Sổ chi tiêu cấp nhóm: `groupId` theo nhóm, file chung giữa các fixture trên emulator (không xóa ở teardown), trần nhóm truyền vào runner; `run_group.ps1` kiểm trước mỗi fixture (còn lại ≥ worst-case fixture) và dừng nhóm; sổ được kéo về host sau mỗi fixture (bản sao hash-chain) | Test: fixture thứ k vượt trần → không dispatch; crash giữa fixture → tổng giữ nguyên; cost UNKNOWN → nhóm dừng | Trần G1/G2/G3 áp xuyên fixture | — |
| **L3** C1 bug | Panel: chỉ `L1_REQUIRED` mới gọi `runFromL1`; L1 legacy → thông báo tạo binding mới, không nút L1; xóa mã chết | JVM + instrumented: chương có L1 legacy → 0 call L1; chương có L1 v2 → chỉ L2/L3 | Đạt | — |
| **L4** C1 UNKNOWN | Test instrumented: ngắt sau claim RAW (và sau claim RECONCILE) bằng provider ném lỗi/giả process death → lần chạy sau dừng `RECOVERY_REQUIRED`/UNKNOWN, **0** dispatch lại | Instrumented trên emulator | Đạt | — |
| **L5** G2 thiết kế | Chế độ `L1_THEN_L2` trong runner; bảng §2 của `docs/P6_R5_EXECUTION_BUDGET_TABLE.md` cập nhật mô tả G1/G2 (tổng call và worst-case không đổi: G1 24, G2 24) | Dry-run fake: 3 fixture `L1_THEN_L2` sinh REPORT_L1 + VI_L2 trong cùng DB | Đạt | Nếu số call đổi → dừng, hỏi owner |
| **L6** C3 | Ghi bytes prompt mọi phase vào run dir (trên thiết bị, ngoài Git); `verify_fixture_run.py` kiểm leak trên mọi prompt; thêm **pre-dispatch** check trên host (template + nguồn đẩy lên) trong `run_group.ps1`; test Python cho cả hai | `test_verify_fixture_run.py` + test mới | Đạt | — |
| **L7** C9 | Chạy toàn bộ androidTest trên emulator (trừ lớp live/provider và lớp seed DB chính), lập danh sách lỗi hiện tại theo lớp/phương thức, phân loại (lịch sử pilot-only / cần sửa); sửa hoặc `@Ignore` có lý do | Bảng C9 trong execution record | Danh sách đầy đủ, không còn lỗi chưa phân loại | — |
| **L8** | Build wrapper (production + test), archive hai nơi; regression emulator; dry-run fake 14 fixture lại; chạy **live-mode zero-call check** (fingerprint cố ý sai) trên emulator | Hash, kết quả | Đạt; 0 call thật | — |
| **L9** owner | Xác nhận: key nằm trong app trên **emulator** (G1–G3) và trên **pilot** (G4); fingerprint đã đưa áp cho cả hai (cùng key) | Câu trả lời owner | — | Thiếu → không bắt đầu G1 |

Sau L0–L9: tiếp tục G1 → G2 → G4 → G3 → R7 đúng như `docs/P6_R6_R7_CODEX_WORK_REQUEST_20261003.md` §4 (P5–P9), với hai thay đổi đã nêu (sổ chi tiêu cấp nhóm, G2 dùng `L1_THEN_L2`). Mỗi nhóm live có báo cáo riêng trước khi sang nhóm sau.

## 4. Quy tắc (giữ nguyên yêu cầu trước) và việc commit

- Không provider/device ngoài quyền; luôn `adb -s <serial>`; không cài pilot trước G4; không đụng chuỗi legacy.
- Commit theo gói, **push sau mỗi gói** (lần trước 23 commit nằm local); hook yêu cầu cập nhật `WORKSPACE_SNAPSHOT.md`.
- Báo cáo tách `STRUCTURAL_VALID` / `SEMANTIC_EVAL`; điểm `SEMANTIC_EVAL` của fake run (FAIL 12/PASS 2) chỉ chứng minh bộ chấm phát hiện lỗi chưa sửa, không đo model.

## 5. Báo cáo cuối

1. Gói đã đóng + bằng chứng. 2. Call/chi phí thật (phải 0 cho tới khi G1 bắt đầu). 3. Test đã chạy, giới hạn. 4. Commit/push. 5. Đúng một bước tiếp theo hoặc câu hỏi L9.

## 6. Trả lời L9 của owner (2026-10-03)

Owner xác nhận: API key đã lưu trong Settings của app trên **cả emulator và máy pilot `15e84958`**, và là **cùng một key** với endpoint account fingerprint owner đã đưa. Câu trả lời này đóng L9 về mặt thông tin; việc kiểm thật vẫn do runner làm trên từng thiết bị (fingerprint tính từ `SettingsStore` phải khớp giá trị owner đưa, lệch thì dừng trước call, 0 call). Codex không đọc, in hay ghi key; fingerprint không vào Git. G1 được bắt đầu ngay khi L0–L8 đạt.

## 7. Review L0–L8 và gỡ chặn L8 (Claude, 2026-10-03)

**Kiểm độc lập trên `3ee406c7`:** từ `git archive` sạch: engine 340/340, app unit 339/339, lint và androidTest compile PASS; Python `test_score_run` 16, `test_spend_ledger` 3, `test_verify_fixture_run` 9, `test_verify_prompt_inputs` 5 (33/33). Soát `495b30bb..HEAD`: không key, không fingerprint, không văn bản sách. Đối chiếu mã: L1 live mode MET (cổng `p6_fixture_live=YES`, key chỉ qua `SettingsStore`, thứ tự: định dạng fingerprint → route → key có mặt → so fingerprint; provider chỉ dựng sau khi preflight qua), L2 trần cấp nhóm MET (`p6_group_id`, file ledger theo nhóm, `run_group.ps1` kiểm trước mỗi fixture), L3 MET (`EditorialChapterRunActionPolicy`), L4 MET (hai test ngắt RAW/RECONCILE, instrumented), L5 MET, L6 PARTIAL chấp nhận được: trước dispatch kiểm nguồn + template (prompt là hàm xác định của hai thứ này), sau chạy kiểm bytes prompt mọi phase.

**Nguyên nhân L8 `P6_LIVE_ROUTE_SETTINGS_MISMATCH`:** không phải lỗi mã hay OpenRouter. Trong emulator, **Wi-Fi bị tắt và `mobile_data=0`** (`ping 8.8.8.8` → "Network is unreachable"), nên app không làm mới được catalog và không chọn được `openai/gpt-5.6-luna`. Claude đã bật lại (`svc wifi enable`, `svc data enable` trên `emulator-5554`); sau đó `ping 8.8.8.8` và `ping openrouter.ai` đều thông. Không đụng Settings, key hay máy pilot.

**Việc owner làm trên emulator (không đổi key):** mở app → Settings: Provider **OpenRouter**; Base URL để **mặc định** (`https://openrouter.ai/api/v1/chat/completions`); bấm **Refresh Pricing** rồi chọn `openai/gpt-5.6-luna`, hoặc dùng nút **Custom model** nhập đúng `openai/gpt-5.6-luna`; lưu. Cổng route chỉ kiểm ba trường này (`EditorialP5EFreshRawRoutingPolicy.matches`).

**Việc Codex làm tiếp:** (1) kiểm lại mạng emulator còn thông (nếu emulator khởi động lại mà mạng tắt, bật lại bằng `svc wifi enable`/`svc data enable`, ghi vào log); (2) chạy lại preflight sai-fingerprint cố ý → phải ra `P6_LIVE_FINGERPRINT_MISMATCH`, 0 call; (3) chạy preflight với fingerprint owner đưa → phải qua, vẫn 0 call (không dispatch); (4) chạy **G1** đúng bảng (24 call, trần nhóm USD 1.00, 0 retry, UNKNOWN dừng), chấm bằng `score_run.py`, báo cáo `STRUCTURAL_VALID`/`SEMANTIC_EVAL`/chi phí thật, **dừng trước G2**. Nếu route vẫn lệch sau khi owner lưu: dừng, ghi giá trị provider/model/endpoint (không ghi key) để owner sửa.
