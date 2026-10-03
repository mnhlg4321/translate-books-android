# Yêu cầu làm việc cho Codex — P6 R6/R7: từ contract ledger v2 đã kiểm offline tới bản cuối đo được (v4.18)

Ngày lập: 2026-10-03. Người giao: owner. Người lập: Claude (tiếp nhận kết quả R0–R5, kiểm chứng độc lập, phản biện).
Repo `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, mốc `f11581a3` (hoặc commit chứa tài liệu này). Không tạo branch/release/checklist mới.

Đọc trước, theo thứ tự: `AGENTS.md`, `EDITORIAL_RECOVERY_V4_18.md` mục 10–11, `docs/P6_R0_ISSUE_TABLE.md`, `docs/P6_R0_FIXTURE_MANIFEST.json`, `docs/P6_R5_EVALUATION_PROTOCOL.md`, `docs/P6_R5_EXECUTION_BUDGET_TABLE.md`, `WORKSPACE_SNAPSHOT.md`, `HANDOFF.md`, `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`. Công thức build/emulator của máy này ở mục 7 dưới đây.

## 1. Quyết định owner đã có (2026-10-02, "duyệt tất cả theo hướng khuyến nghị")

| Mã | Nội dung đã duyệt | Giới hạn |
|---|---|---|
| Q1 | Bảng R6 trong `docs/P6_R5_EXECUTION_BUDGET_TABLE.md` §2: G1 ≤ USD 1.00, G2 ≤ 1.00, G3 ≤ 0.75, G4 ≤ 0.50; tổng ≤ **3.25**; thứ tự **G1 → G2 → G4 → G3**; model `openai/gpt-5.6-luna`, route đã khóa, reasoning `minimal`; 0 retry, 0 repair call; UNKNOWN không chạy lại | Dự phòng 0.53 **không** nằm trong quyền: dùng phải hỏi riêng. Bảng phải được tính lại nếu giá kiểm ở P4 khác |
| Q2 | Chạy lại chương 001 trên **binding/run declaration mới trong project pilot hiện có**; chuỗi cũ `7a5e3428→7483b211→e1a4c638→867a23f8` giữ nguyên làm lịch sử `LEGACY_CONTRACT_V1` | Không sửa/xóa hàng cũ |
| Q3 | Cài bản R6 lên pilot `15e84958` **sau** khi emulator đạt regression và **sau** sao lưu DB pilot | Chỉ cài bản đã archive bằng wrapper; `install-validated.ps1` |
| Q4 | R7 = chương **007** và **010**; trần R7 ≤ USD 0.75 (16 call) | Chỉ bắt đầu R7 khi G4 đạt `SEMANTIC_EVAL` PASS hoặc owner chấp nhận kết quả G4 |
| Q5 | U1 bỏ 階級 trong `特権階級` là tùy chọn, không chấm; U2 tách đoạn RAW→2 đoạn DRAFT không phải lỗi nhưng phải giữ ánh xạ; U3 mọi cụm tiếng Việt giữ hai thành tựu `踏破`≠`攻略` đều đạt | Đã đóng băng; không đổi sau khi có lượt chạy |

## 2. Hiện trạng đã kiểm độc lập (2026-10-03)

- HEAD `f11581a3`, đồng bộ origin; working tree chỉ còn file `.idea`/docs P5E cũ/untracked evidence của owner (không stage).
- Từ `git archive` sạch: engine **340/340**, app unit **324/324**, lint PASS, androidTest compile PASS; bộ chấm `scripts/p6/test_score_run.py` **16/16** (cần fixture ngoài Git ở `D:\P5E-private\p6-fixtures`, đang có trên máy).
- Cỡ chương theo đúng luật inventory (`EditorialRawInventory.java:73-100`; dòng rỗng, U+3000, BOM, marker ảnh bị loại): 001 = **191** unit (tài liệu R5 ghi 192 — sai lệch 1 do dòng `[IMAGE: …]`), 007 = 206, 010 = 238, lớn nhất cả tập là 014 = 322; không chương nào gần 600 (`EditorialL1LedgerRun.java:21`). Input ước tính (gate `ceil(bytes/2)`): 007 ≈ 55.7k, 010 ≈ 59.6k token so với trần 100k. 016/017 không có DRAFT.
- Trần trong contract: output 8,192 / 16,384 / 4,096 token theo call; wire 65,536 B (final-read 16,384 B); mỗi call L1 tối đa 400 candidate, 120 dải coverage, 48 finding, 200 speaker record, 100 protected span (`EditorialL1Ledger.java:30-40`).

## 3. Phản biện — các điểm phải sửa trước khi chi tiền live

| # | Vấn đề (có chứng cứ) | Hệ quả | Hành động |
|---|---|---|---|
| C1 **chặn** | Không có đường sản phẩm để chạy **L1** cho binding mới. `EditorialP5EFreshRawLiveRunner` gắn cứng `SELECTOR`/`BINDING_IDENTITY` cũ (dòng 41–58) và binding đó đã `ALREADY_USED`; `executeRaw/executeReconcile` chỉ được gọi từ runner này; UI (`EditorialChapterFinalPanel` → `runToFinal`) chỉ chạy L2/L3 khi L1 đã commit | G4 (Q2) và R7 không chạy được; người dùng thật cũng không chạy được L1 | P1: thêm L1 (RAW + RECONCILE ledger v2) vào **cùng** hành động chạy trên thẻ chương; hộp thoại cấp phép hiển thị đủ 8 call và trần từng call; coordinator có `runFromL1` idempotent theo từng stage |
| C2 **chặn** | Bề mặt chạy fixture G1–G3 và cách cấp key chưa chốt (R5 §3.1 để "spike") | Không chạy được G1–G3; nguy cơ key phải đi qua shell của agent | P2: runner **instrumented trên emulator**, opt-in; owner tự nhập key vào Settings của app trên emulator; runner đọc `SettingsStore`, chỉ đối chiếu endpoint account fingerprint do owner đưa (như M4). Không chạy runner bằng JVM vì key sẽ phải nằm trong môi trường của agent |
| C3 | Nhóm L2-only dựng predecessor L1 "từ nhãn" (`P6_R5_EXECUTION_BUDGET_TABLE.md` §2) | Nếu ledger dựng sẵn chứa câu tiếng Việt đã sửa hoặc chuỗi `mustContain`, đó là lộ đáp án và L2-only đo sai | Ledger dựng sẵn chỉ chứa RAW anchor, DRAFT anchor, loại lỗi, quan sát trung tính; cấm chuỗi `mustContain` và câu đã sửa; thêm test chạy oracle-leak guard trên **chính bytes REPORT_L1 dựng sẵn** và trên prompt L2 |
| C4 | Trần nhóm do "runner cộng dồn" nhưng chưa có cơ chế bền | Restart/crash giữa nhóm làm mất tổng đã tiêu → vượt trần | Sổ chi tiêu theo nhóm lưu bền (một file JSON append-only trong thư mục evidence, hash mỗi dòng); **kiểm trước** mỗi call: tổng đã tiêu + worst-case call sắp gọi ≤ trần nhóm, nếu không thì dừng nhóm |
| C5 | Giá 0.25/1.20 USD/1M suy từ 2 call | Giá có thể đã đổi | P4: đọc giá chính thức OpenRouter cho `openai/gpt-5.6-luna` ngay trước call đầu, ghi nguồn + thời điểm; nếu worst-case mới vượt trần đã duyệt thì dừng hỏi owner |
| C6 | Final-read dùng 0 vòng sửa | Một defect nhỏ ở final-read làm chương không có FINAL (`CONTENT_L3_FINAL_READ_DEFECTS`) | Giữ 0 vòng trong R6 để đo; ghi tần suất. Nếu chặn G4, đề xuất bật **1 vòng** (contract cho phép) như một thay đổi code có test + xin duyệt lại ngân sách; không tự bật |
| C7 | Trần 48 finding/call, 400 candidate, 120 dải | Chương nhiều lỗi có thể chạm trần | Vượt trần phải dừng có kiểu (đã có); báo cáo số đo thực tế mỗi lượt để quyết chunking sau, không chunk trước khi có số |
| C8 | Tài liệu R5 ghi 001 = 192 unit | Sai lệch nhỏ, gây nhầm khi chấm coverage | Sửa thành 191 kèm lý do |
| C9 | 22 test instrumented lịch sử fail (schema v24, seed lịch sử) | Che regression thật khi chạy cả bộ | Trước P7: lập danh sách, cập nhật kỳ vọng v25 hoặc `@Ignore` có lý do trỏ tới evidence lịch sử; báo cáo regression luôn ghi "X/Y, trong đó 22 lịch sử" |
| C10 | Mỗi fixture chạy 1–3 lượt | Không đủ để kết luận thống kê | Báo cáo là chỉ báo; holdout chạy đúng một lần; không chọn lượt đẹp |
| C11 | Tạo binding mới trên pilot là ghi DB pilot | Ảnh hưởng dữ liệu thật | Sao lưu DB trước cài và trước tạo binding; dùng `EditorialP4BindingTransactionService.createSetup` (đường sản phẩm) với **asset chương 001 đã có trong DB**, selector mới; không đổi hàng cũ |
| C12 | Chấm người (`human_scores.json`) cần owner đọc | Kết quả kẹt `PENDING_HUMAN` | Mỗi lượt cần chấm người: chuẩn bị gói đọc gọn (dòng RAW, DRAFT, kết quả, ≤40 dòng đổi) để owner chấm 0/1/2 |

## 4. Gói việc theo thứ tự

Quy ước: "offline" = không provider, không pilot; emulator `emulator-5554` được dùng tự do với APK đã archive.

| Gói | Việc | Đầu ra / kiểm thử | PASS | Dừng / đổi cách |
|---|---|---|---|---|
| **P0** baseline | Xác minh branch/HEAD/status; đọc tài liệu mục đầu; không rerun suite nếu bytes không đổi (mốc ở §2) | Ghi HEAD + status vào báo cáo | — | Lệch lớn so với §2: dừng, báo |
| **P1** offline — C1 | L1 trong hành động chạy sản phẩm: `runFromL1(projectId, selector, chapterKey, budgets, providers)` gọi `executeRaw` → `executeReconcile` (ledger v2) → L2 → L3, resume từng stage, không gửi lại stage UNKNOWN; budgets 8 call theo bảng; hộp thoại cấp phép liệt kê 8 call/trần; thẻ chương hiển thị trạng thái L1. Giữ nguyên runner cũ (lịch sử) | JVM: thứ tự call, resume sau crash ở từng stage, double-tap không tạo 2 lượt, trần sai → không chạy, UNKNOWN → không chạy lại. Instrumented trên emulator: chuỗi 8 call với fake provider từ L1 tới FINAL + reopen | engine/app unit, lint, androidTest compile; instrumented emulator đạt | Hai vòng cùng chữ ký lỗi → xem lại thiết kế, ghi lý do |
| **P2** offline — C2–C4 | Runner fixture instrumented opt-in (`p6_fixture_run=YES`): đọc fixture đã `adb push` vào thư mục tạm của emulator; mỗi fixture một DB tạm + `createSetup`; chế độ `L1_ONLY`/`L2_ONLY`/`L3_ONLY`/`CHAIN`; ghi `final.txt` + `structural.json` đúng §2 protocol; script host `scripts/p6/run_group.ps1` push/run/pull, sổ chi tiêu bền, dừng nhóm theo trần; predecessor L2/L3-only dựng bằng mã production + fake provider, ledger không chứa đáp án (C3) | Dry-run toàn bộ 14 fixture với **fake provider** trên emulator → bộ chấm chạy được trên output (chỉ cấu trúc); test leak guard trên REPORT_L1 dựng sẵn và prompt; test sổ chi tiêu (crash giữa nhóm không làm mất tổng) | Dry-run 14/14 sinh run dir hợp lệ; leak guard xanh; không call provider | Như trên |
| **P3** offline | Build bằng wrapper trong worktree tạm (§7), archive hai nơi; regression emulator (lớp mới + lớp R1–R4); sửa C8; lập danh sách C9 | Hash APK/test APK, payload hai nơi, kết quả instrumented theo lớp | Đạt, ngoài 22 lịch sử đã ghi | Lỗi mã: sửa, build lại |
| **P4** owner + kiểm giá | Owner nhập key vào app trên emulator và đưa endpoint account fingerprint; Codex đọc giá chính thức (C5), tính lại bảng; kiểm lại hash fixture/nhãn/holdout lock và chạy leak guard ngay trước call đầu | Biên bản giá (URL, thời điểm, số), bảng đã cập nhật | Worst-case mới ≤ trần Q1 | Vượt trần: dừng, hỏi owner |
| **P5** live G1 (≤ 1.00) | 24 call L1-only theo bảng §2; mỗi call log redacted (token, USD, finish); chấm `score_run.py`; gói đọc cho owner nếu cần | Báo cáo G1: `STRUCTURAL_VALID` và `SEMANTIC_EVAL` tách riêng, TP/FN/FP theo lớp lỗi | Theo ngưỡng đóng băng | UNKNOWN/vượt trần: dừng nhóm; lỗi prompt/contract: sửa offline, **không** chạy lại trong nhóm này khi chưa có quyền |
| **P6** live G2 (≤ 1.00) | L2-only + L3-only + đối chứng (24 call) | Như G1 | Như G1 | Như G1 |
| **P7** pilot + G4 (≤ 0.50) | M0 chỉ-đọc (`scripts/p5e-m4-m0-observation.ps1`), sao lưu DB, cài bản P3 bằng `install-validated.ps1`, đọc lại hash; tạo binding mới cho 001 (C11) qua opt-in instrumented gọi `createSetup` với asset sẵn có, selector `p6-v2-mercedes-vol5-001-<yyyymmdd>`; chuỗi 8 call từ **UI** (owner bấm chạy sau khi đọc hộp thoại) hoặc runner opt-in gọi đúng coordinator; sau chạy: export DB chỉ-đọc, `logcat -d -s P5E_RAW:V`; owner xem/mở lại/xuất TXT qua SAF; kéo file, so SHA-256 | Evidence `D:\P5E-private\p6-g4-001-<UTC>\`; bảng chấm `fx-a01` | `L3_FINAL_COMMITTED` + `SEMANTIC_EVAL` PASS (I-001, I-003, `踏破` 4/4, collateral ≤ 3, chấm người) | Như G1; FINAL-read defect → C6 |
| **P8** live G3 holdout (≤ 0.75) | `fx-h01`, `fx-h02` chuỗi 8 call, **một lần**, không chỉnh prompt sau đó | Báo cáo holdout | Ngưỡng đóng băng | Không chạy lại holdout đã dùng |
| **P9** R7 (≤ 0.75) | Chỉ khi điều kiện Q4 đạt: kiểm (chỉ đọc) chương 007/010 đã có asset trong project pilot chưa; nếu chưa, owner nhập qua UI; binding mới mỗi chương; chuỗi 8 call mỗi chương; xem/mở lại/xuất | Bảng nghiệm thu theo `EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` mục 4–5 cho 3 chương | 3 chương `FINAL_OUTPUT_ACCEPTED` (owner xác nhận chất lượng) | Chương bị CONTENT_BLOCKED đúng contract là bằng chứng âm, không thay chương đạt |
| **P10** | Cập nhật checklist v4.18 (chỉ bằng evidence), `EDITORIAL_RECOVERY_V4_18.md` mục 10 (đúng một next action), `HANDOFF.md`, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md` | — | — | — |

P1–P3 làm ngay, không cần thêm quyền. P4 cần owner thao tác. P5–P9 chạy trong quyền §1, theo thứ tự, mỗi gói chỉ bắt đầu khi gói trước có báo cáo.

## 5. Quy tắc

- Không gọi lại RAW/RECONCILE/L2/L3 của chuỗi cũ; không sửa blob/receipt cũ; không khôi phục DB pilot; không `connectedAndroidTest` trên pilot; luôn `adb -s <serial>` (máy owner và emulator cùng cắm).
- Không đọc, in, ghi, chuyển API key; fingerprint do owner đưa, không lưu vào Git.
- Fixture văn bản sách và run dir ở `D:\P5E-private\…`, ngoài Git; bản FINAL owner sửa tay không bao giờ vào prompt; bộ chấm từ chối đường dẫn `6.FINAL`.
- Mọi APK qua wrapper, archive hai nơi; không build cho thay đổi docs thuần; không rerun suite khi bytes không đổi.
- Báo cáo luôn tách `STRUCTURAL_VALID` và `SEMANTIC_EVAL`; không kết luận chất lượng từ COMMITTED, metric 0 hay fake test.
- Lỗi build/test là `FAILED_REPAIRING`, sửa trong gói; hai vòng cùng chữ ký lỗi thì đổi cách và ghi lý do.
- Commit theo gói, message `type(scope): summary`, không BOM, có dòng `Co-Authored-By` theo quy ước repo; hook yêu cầu cập nhật `WORKSPACE_SNAPSHOT.md` trong commit; không stage `.idea`/docs P5E cũ.

## 6. Báo cáo cuối mỗi gói

1. Điều mới được chứng minh (hash, số liệu). 2. Call và chi phí thật so với trần (theo sổ chi tiêu). 3. Lỗi đã sửa, test đã chạy, giới hạn. 4. `STRUCTURAL_VALID` / `SEMANTIC_EVAL` theo fixture. 5. Commit/push. 6. Đúng một bước tiếp theo hoặc một quyết định cụ thể cần owner.

## 7. Công thức máy này (đã kiểm trong các phiên trước)

- Bash không có `python`; dùng `py` trong PowerShell. Gradle: đặt `JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'` trong cùng lệnh PowerShell; chạy `cmd /c ".\gradlew.bat <tasks> -q 2>&1"` từ repo; cộng số test từ `build/test-results/**/*.xml`.
- APK chỉ qua `scripts/build-and-save.ps1` và `scripts/build-and-save-android-test.ps1` từ worktree sạch tạm dưới `D:\P5E-builds\wt-*` (`git worktree add -b tmp/... HEAD`, chép `local.properties`, junction `artifacts\builds`, `artifacts\test-builds`, `backup` về thư mục thật; gỡ junction bằng `[IO.Directory]::Delete(path,$false)` trước `git worktree remove`). Script test APK cần SHA APK/source-zip production từ `BUILD_INFO.json` đã archive và cert `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- Emulator: `emulator.exe -avd tbl-code113-dqa-api35` (serial `emulator-5554`); chạy một lớp: `am instrument -w -e class <Class>[#method] com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner`.
