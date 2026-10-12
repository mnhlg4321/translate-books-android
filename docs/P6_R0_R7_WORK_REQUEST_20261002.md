# Yêu cầu làm việc — P6 R0→R7: sửa ledger, lưu bền, coverage và kiểm ngữ nghĩa L1–L3

Ngày lập: 2026-10-02. Người giao: owner. Dùng nguyên văn cho một phiên làm việc mới (Claude điều phối, sub-agent/Luna làm việc nhỏ).
Repo `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD lúc lập `547c8a4d` cộng các file tài liệu chưa commit nêu ở bước 0. Không tạo branch/release/checklist mới.

Căn cứ chính, đọc theo thứ tự: `EDITORIAL_RECOVERY_V4_18.md` mục 10–11 (kế hoạch R0→R7, chưa commit), `docs/P6_LEDGER_QA_CLAUDE_HANDOFF_20261002.md` (chưa commit), `docs/P6_CHAPTER_001_INDEPENDENT_AUDIT_20261002.md` (chưa commit), `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`, `WORKSPACE_SNAPSHOT.md`, `BUILD_STATE.md`. Mục 11 là kế hoạch; tài liệu này bổ sung phản biện, quyết định kỹ thuật đã chốt và thứ tự thực hiện cụ thể. Khi hai nơi khác nhau, mục 11 thắng về mục tiêu, tài liệu này thắng về cách làm.

## 1. Nhận định đã kiểm lại trên mã (2026-10-02)

| # | Nhận định trong mục 11/audit | Kiểm trên mã | Kết luận |
|---|---|---|---|
| F1 | `findingCount` không được gán | `EditorialP5PilotExecution` khai báo ở dòng ~869 và truyền vào metrics, không có chỗ tăng | Đúng — số 0 không đo được gì |
| F2 | REPORT_L1 lưu bền bỏ ledger entries | `reportBytes` chỉ ghi counts, gates, evidenceRefs, preservedInventory; không có `ledger.entries` | Đúng — L2 không có gì để tiêu thụ |
| F3 | Wire L1 là population acknowledgement, không phải Error Ledger | `MAX_FINDINGS=4`, một entry mỗi `population:<chapter>`, không anchor/giải thích | Đúng |
| F4 | Protected set rỗng ở runtime | `EditorialChapterFinalCoordinator` dòng ~68 `Set.of()` | Đúng — `protectedSpanRegressions=0` không chứng minh gì |
| F5 | `finalReadOrder` là marker tĩnh | `EditorialL3Execution` ghi `List.of(...)` cố định; không có call đọc lại FINAL đã dựng | Đúng |
| F6 | **Bổ sung — chưa được nêu cụ thể:** attempt identity L1 không gắn revision contract | `EditorialP5PilotRequest.ATTEMPT_IDENTITY_DOMAIN = "EDITORIAL_P5_L1_ATTEMPT_IDENTITY_V1"`; identity = binding/run/pack/profile/eval/chapter/phase/predecessor/bundle | Nếu đổi wire/report mà giữ domain, RAW mới cho chương 001 trên cùng binding sẽ trùng identity với event 7 → `ALREADY_COMMITTED` trả về artifact cũ. Phải đưa contract revision vào identity (R1) |
| F7 | **Bổ sung:** runner live RAW chỉ chạy trên binding chưa dùng | `dispatchRaw` → `inspectLineage` → `ALREADY_USED` nếu binding đã có attempt | Chạy lại chương 001 theo contract mới cần **binding/run declaration mới** (hoặc chính sách lineage theo revision). Đây là quyết định của owner ở R6 |

## 2. Phản biện kế hoạch và quyết định kỹ thuật đã chốt

| Điểm | Rủi ro nếu làm nguyên văn | Quyết định cho phiên thực hiện |
|---|---|---|
| Phạm vi R0–R7 rất rộng | Lại thành vòng tài liệu/gate dài, không ra bản cuối | Làm theo **lát dọc**: R0 tối thiểu → R1 contract v2 tối thiểu → R2 → R3 → R4 → R5 harness. Mỗi R có timebox và đầu ra chạy được; không mở rộng sang bước sau khi bước trước chưa có test đỏ→xanh |
| Inventory RAW cấp câu | Tách câu tiếng Nhật dễ sai, payload lớn | **v2 = inventory cấp dòng/đoạn**: mỗi dòng RAW không rỗng là một unit, ID ổn định = `u:<số dòng>:<8 hex đầu sha256 nội dung>`; quy tắc loại trừ (dòng rỗng, marker ảnh) ghi trong code và test. Chỉ chia nhỏ hơn khi R0 chứng minh cần |
| Báo cáo từng unit | 257 dòng × entry làm output phình | Model trả **coverage theo dải** (`u:a–u:b PROCESSED`) và **liệt kê riêng** candidate/finding có anchor; app kiểm dải phủ kín, không chồng, không mồ côi. Finding vẫn liệt kê đầy đủ, không trần 4 |
| "Tăng trần" | Cắt im lặng khi vượt | Trần mới theo sizing đo được; vượt trần → chunk theo dải unit với chồng lấn, hoặc typed `INPUT_REQUIRED/REPAIR_REQUIRED`; không bao giờ commit report thiếu chunk |
| Reconstructor split/merge/insert/delete | Viết lại lớn, dễ vỡ anchor | Thêm thao tác `INSERT_AFTER`, `DELETE`, `MERGE_WITH_NEXT` có anchor + hash dòng; ánh xạ dòng DRAFT→VI_L2→FINAL do app tính; mọi thao tác vẫn qua diff đối chiếu chéo. Không hỗ trợ sửa giữa dòng theo offset trong v2 |
| Final-read | Marker tĩnh hoặc gọi thêm vô hạn | Final-read = một call có trần, nhận **đúng bytes đã dựng** + hash; response phải echo hash; sửa sau đó làm lần đọc mất hiệu lực; tối đa 1 vòng sửa-sau-đọc rồi typed incomplete |
| Fixture lấy từ sách thật | Đưa văn bản có bản quyền lên GitHub | Fixture chứa trích đoạn RAW/DRAFT/manual FINAL thật **để ngoài Git** (`D:\P5E-private\p6-fixtures\`), repo chỉ giữ manifest (đường dẫn tương đối, SHA-256, nhãn). Fixture tổng hợp tự viết thì để trong repo |
| Rò đáp án | Manual FINAL hoặc nhãn lọt prompt | Test bắt buộc: dựng prompt cho từng fixture rồi kiểm không chứa bất kỳ dòng nào chỉ có trong manual FINAL hay chuỗi nhãn; builder fixture từ chối đường dẫn `6.FINAL` ở runtime |
| Semantic eval cần provider | Fake test bị dùng như bằng chứng chất lượng | Tách hai trạng thái trong mọi báo cáo: `STRUCTURAL_VALID` (fake/offline) và `SEMANTIC_EVAL` (provider thật, theo bộ chấm đã đóng băng). Không câu nào gộp hai thứ |
| Chuỗi cũ trên pilot | Sửa blob cũ hoặc tái dùng COMMITTED | Chuỗi `7a5e3428→7483b211→e1a4c638→867a23f8` giữ nguyên làm lịch sử; report cũ đọc được, gắn nhãn `LEGACY_CONTRACT_V1` |

## 3. Bước thực hiện

### Bước 0 — baseline (bắt buộc trước mọi sửa)
1. Xác minh branch/HEAD/status; so với HEAD lúc lập. Không reset/stash; không stage `.idea`, docs P5E cũ, untracked evidence.
2. Đọc bốn tài liệu chưa commit; nếu không mâu thuẫn rõ với mã, commit chúng thành **một** commit docs "baseline kế hoạch R0–R7" (chỉ các file: `EDITORIAL_RECOVERY_V4_18.md`, `WORKSPACE_SNAPSHOT.md`, hai file `docs/P6_*_20261002.md`, và tài liệu này), push.
3. Chạy một lần mốc test hiện tại (`:editorial-engine:test`, `:app:testDebugUnitTest`) để có baseline thật cho phiên.

### R0 — bảng lỗi và fixture (offline)
- Đầu ra: `docs/P6_R0_ISSUE_TABLE.md` (trong repo, không chép đoạn văn dài: chỉ anchor dòng, ≤15 từ trích dẫn mỗi ô) với cột: ID, nguồn (RAW/DRAFT/pilot/manual dòng), phân loại (`CONFIRMED_DEFECT` / `PROFILE_VIOLATION` / `PREFERENCE` / `UNCERTAIN`), invariant nghĩa kỳ vọng, cách chấm, test sẽ phủ. Tối thiểu phủ: dòng 237 `今回`; nhóm `踏破/攻略` (RAW 105, 147, 175, 215 ↔ pilot 107, 148, 176, 216); `嬢ちゃん` dòng 321; `特権階級` dòng 125 (phân xử riêng); hai chỗ gộp đoạn của manual; F1–F7.
- Fixture: `D:\P5E-private\p6-fixtures\` + `docs/P6_R0_FIXTURE_MANIFEST.json` (hash, nhãn, loại: real/seeded/clean/ambiguous/holdout). Seeded mutation mỗi fixture một lỗi mục tiêu, phân loại theo mục 11.5.
- Luna: đối chiếu anchor RAW/DRAFT/pilot/manual cho từng dòng bảng (một agent, chỉ đọc). Claude phân xử loại lỗi. Owner chỉ quyết các ô `UNCERTAIN` cần tri thức ngoài nguồn — gom thành một danh sách hỏi một lần.
- Đóng R0 khi mọi lỗi `CONFIRMED` có invariant + cách chấm; holdout được chọn và **khóa** (hash) trước khi nhìn output model mới.

### R1 — contract v2 (offline)
- Schema mới có version: inventory (§2), candidate, finding (Error ID, loại, mức độ, RAW anchor(s), DRAFT anchor/vị trí chèn, quan sát ngắn, expected meaning, evidence refs, disposition, evidence limit), TG/SR/RC/Pair/Speaker record tối thiểu, protected span (nguồn, scope, lý do), metric definitions (`uniqueFindingCount`, `occurrenceCount`, unresolved/preserved, applied/reverted).
- Bind `contractRevision` vào attempt identity L1 (F6), L2, L3. Report v1 đọc được, gắn `LEGACY_CONTRACT_V1`, không đủ điều kiện làm predecessor v2.
- Sizing: đo chương 001 (ngắn) và một chương dài từ nguồn pilot (chỉ đọc) → trần per-call; quy tắc chunk + overlap đóng băng bằng test (Unicode, CRLF, BOM, cuối file).
- Đóng khi negative fixtures biểu diễn được toàn bộ bảng R0 và round-trip schema không mất trường.

### R2 — L1 và lưu bền (offline)
- Provider/parser/report/store mang ledger đầy đủ; `findingCount` và metric mới tính từ dữ liệu đã validate; RAW giữ raw-first (chỉ RAW+GLOSSARY).
- Test bắt buộc: serialize → DB (emulator/instrumented) → restart → `committedL1` → L2 nhận đủ entry; fixture 0/1/nhiều finding, nhiều occurrence một finding, >4 finding, chunked.

### R3 — L2 và reconstruction (offline)
- L2 phải xử lý từng L1 finding: `FIXED` (Change ID + diff), `REJECTED` (RAW evidence), `PRESERVED` (span + evidence limit), `UNRESOLVED` (typed stop); finding mới do L2 thêm có ID riêng. Known-defect bị preserve = semantic fail ở bộ chấm, dù hợp lệ cấu trúc.
- Protected spans từ REPORT_L1 v2 đi vào coordinator thật (bỏ `Set.of()`), remap qua DRAFT→VI_L2→FINAL.
- Reconstructor thêm `INSERT_AFTER`/`DELETE`/`MERGE_WITH_NEXT` (§2). Final-read VI_L2 theo §2.
- Test từ entrypoint coordinator, không chỉ unit truyền tay.

### R4 — L3 và release (offline)
- Probe/QA finding có anchor RAW + VI_L2, phạm vi đã kiểm, đối chứng, kết luận, action; bỏ `NO_DEFECT` chung chung không anchor.
- Final-read FINAL thật (§2) thay marker tĩnh; receipt ghi hash đã đọc; validator từ chối receipt có marker mà không có operation tương ứng.
- Test L3-only bằng harness dựng predecessor nhất quán (không sửa DB pilot, không bypass production).

### R5 — harness đánh giá (offline, chuẩn bị live)
- Bộ chấm tự động cho invariant có thể kiểm máy + phiếu chấm người cho phần nghĩa; đóng băng nhãn và ngưỡng (mục 11.5) trước chạy.
- Lập **bảng execution** (xem §4) với số call thật theo thiết kế R1–R4. Không gọi provider.

### R6 — build và live có phạm vi (cần quyền)
- `scripts/build-and-save.ps1` + test build, archive hai nơi; regression đủ; emulator instrumented QA.
- Chạy semantic eval và chuỗi chương 001 theo contract mới **chỉ** trong quyền owner cấp theo bảng §4.

### R7 — tái nghiệm thu (cần quyền + chọn chương)
- Chương 001 + hai category còn lại; UI save/reopen/export; checklist chỉ cập nhật bằng evidence thật.

## 4. Bảng quyền/ngân sách sẽ xin (điền số thật ở R5, ước tính ban đầu để owner hình dung)

| Hạng mục | Call ước tính | Trần đề xuất |
|---|---|---|
| L1-only fixtures (lỗi đơn + tổng hợp >4) | ~8–12 (×3 lượt cho subset trọng yếu) | ≤ USD 1.00 |
| L2-only, L3-only, clean/ambiguous controls | ~10–16 | ≤ USD 1.00 |
| Holdout | ~6–10 | ≤ USD 0.75 |
| Chuỗi chương 001 contract v2 (RAW, RECONCILE, L2 discovery, L2 edit, final-read VI_L2, L3 ×2, final-read FINAL; chunk nếu cần) | ~8–12 | ≤ USD 0.50 |
| **Tổng R6** | | **≤ USD 3.25**, 0 retry tự động, UNKNOWN không chạy lại |

Mốc giá tham chiếu đo thật: RAW event 7 USD 0.00295 (21k token vào); RECONCILE M4 USD 0.00747 (27k vào). Output ledger lớn hơn sẽ đắt hơn; bảng ở R5 phải tính worst-case theo trần token từng call.

Quyết định owner sẽ cần (gom một lần ở cuối R5): (Q1) duyệt bảng §4; (Q2) cách chạy lại chương 001: **binding/run declaration mới trên pilot** (khuyến nghị, giữ chuỗi cũ) hay project mới; (Q3) cài build R6 lên pilot `15e84958` (sao lưu DB trước); (Q4) chọn hai chương còn lại cho R7 từ danh sách ứng viên đã có ở commit `547c8a4d`; (Q5) các ô `UNCERTAIN` của R0.

## 5. Quy tắc điều phối

- Claude giữ contract, tích hợp, phân xử nghĩa khó, cập nhật trạng thái. Sub-agent nhận một việc nhỏ có file ownership, đầu vào, test rõ; không hai writer một file; sub-agent không đổi schema/route/budget, không chứng nhận chất lượng.
- Lỗi build/test: `FAILED_REPAIRING`, sửa trong nhóm. Hai vòng cùng chữ ký lỗi → xem lại giả thuyết, đổi cách, ghi một dòng lý do.
- Không provider/device ngoài quyền; emulator `tbl-code113-dqa-api35` dùng tự do cho instrumented test với APK đã archive; không `connectedAndroidTest` trên pilot.
- Mỗi nhóm R xong: commit theo nhóm (có `Co-Authored-By`), push, cập nhật mục 10 plan (đúng một next action), `HANDOFF.md`, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`. Không tuyên bố "đã sửa hết" dựa trên fake test, số COMMITTED hay metric 0.

## 6. Báo cáo cuối phiên

1. Nhóm R đã đóng và bằng chứng (test đỏ→xanh, file/commit). 2. Lỗi trong bảng R0 đã được phủ bằng test nào; lỗi nào còn mở. 3. Gate đạt/chưa (`STRUCTURAL_VALID` tách khỏi `SEMANTIC_EVAL`). 4. Commit/push. 5. Đúng một bước tiếp theo hoặc danh sách Q1–Q5 kèm số liệu nếu đã tới R5.
