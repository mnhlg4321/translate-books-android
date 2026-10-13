# v4.20 — Yêu cầu làm việc: bước D, một lượt nghiệm thu C1 trên chương 014

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (mục 5–7). Checklist: `release_checklists/v4.20.md`. Nhánh duy nhất: `feature/v4.20`, worktree `D:\P5E-builds\wt-v4.20`.

## 1. Mục đích và quyết định sản phẩm

Quyết định cần có: **phát hành hay bỏ C1** (Biên tập bắt model ghi số hàng pronoun `[id] R<n> <dòng>`, app chỉ nhận dạng xưng hô của đúng hàng đó). Câu hỏi: trên một chương chưa từng dùng để chỉnh Biên tập, C1 có sửa được lỗi tự xưng lấy nhầm hàng của nhân vật khác mà không sinh sửa sai mới không. Không trả lời được offline vì là hành vi model; response của các lần chạy cũ không có số hàng nên không replay được.

Đây không phải việc đo bộ kiểm. Không thêm cổng, nhãn, chỉ mục hay bộ chấm mới.

## 2. Quyền (owner duyệt 2026-10-13)

| Mục | Giá trị |
|---|---|
| Chương | 014 (`D:\P5E-private\q2-inputs\014\`), đúng một lượt |
| Model | `openai/gpt-5.6-luna`, reasoning `medium` |
| Lời gọi | tối đa 10 (đo offline: 16 chunk, 10 chunk có điểm, 68 điểm), **không retry**, không chạy lại, không tiếp tục lượt cũ |
| Sổ chi | mới, id `V420-D-014-<yyyymmdd>-<8 ký tự đầu runId>`, trần nhóm và trần chương **USD 0,08** (giữ chỗ ước tính USD 0,060664) |
| Thiết bị | chỉ `emulator-5554` (AVD `tbl-code113-dqa-api35`); không đụng điện thoại `15e84958`, không xóa dữ liệu, không reset |
| Xuất bản | được push `feature/v4.20` sau khi quét; không merge `main`, không tag |

Phần trần chưa dùng không cấp quyền cho lượt khác.

## 3. Baseline đã kiểm

- Commit C1 `739e5c49` (từ `main` `be2056d8`). Archive sạch (zip SHA-256 `2C3ABA55…AAF4`): engine 714 (1 skip), app debug 472 (3 skip), 0 lỗi; `:app:lintDebug` 0 lỗi / 57 cảnh báo; androidTest compile OK.
- App phát hành vẫn 4.18.0 / code 266. Build phát triển gần nhất trên emulator (last-known): 4.19-x5.1 / code 267 (v4.19 đóng băng). Code mới phải > 267.

## 4. File được và không được sửa

- Được sửa: `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialApiV1FixtureRunnerInstrumentedTest.java` (chỉ chế độ không retry ở mục 5, S1); tài liệu trạng thái (`PRODUCT_DIRECTION_PLAN.md` mục 5/7, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, `release_checklists/v4.20.md`); bản sao script riêng ngoài Git.
- Không sửa: `app/src/main`, `editorial-engine/src/main` (prompt, parser, guard, finder giữ nguyên `739e5c49`), khâu dịch, file pronoun/glossary/RAW/DRAFT của owner, `D:\P5E-private\run_c1_chunk.ps1` gốc. Không port cổng X5, bộ kiểm v4.19 hay `optionalDa` sang nhánh này.

## 5. Các bước

**S0 — Resume.** Trong `D:\P5E-builds\wt-v4.20`: `git status` sạch, HEAD là commit chứa tài liệu này, `origin/feature/v4.20` cùng HEAD. Không làm trong `D:\App Translate Books`.

**S1 — Chế độ không retry cho runner (androidTest).** `runChunkFixture` hiện gọi `service.execute(run.id, null)`: luồng sản phẩm thử lại một lần lỗi rõ ràng. Thêm tham số opt-in `p6_no_retry=YES`: duyệt các pair theo thứ tự, mỗi pair gọi `service.executeOnly(run.id, pairId, null)` đúng một lần (maxAttempts = 1), dừng ở pair đầu tiên không `ACCEPTED`/`NO_FIX_POINTS` như service; và một provider bọc đếm lời gọi, từ chối lời gọi thứ `p6_max_calls + 1` (mặc định không giới hạn khi không đặt). Ghi vào `run-metadata.json`: `noRetry`, `maxCalls`, `dispatched`, `refused`. Không thêm kiểm tra nội dung nào. Test: androidTest compile; chạy giả (S4) chứng minh 10 lời gọi giả, 0 bị từ chối. Commit riêng `test(editorial): ...`.

**S2 — Build và lưu trữ.** Theo `AGENTS.md` và recipe: `scripts/build-and-save.ps1` từ worktree sạch của commit S1, series `4.20-d`, `-MinimumVersionCode 268`; rồi `scripts/build-and-save-android-test.ps1` với SHA production/source-zip lấy từ `BUILD_INFO.json` vừa tạo và cert SHA `47F31389…EF3C155`. **Event phải nằm ở `D:\App Translate Books\artifacts\builds|test-builds\…` và `backup\builds|test-builds\…`** (junction hoặc chép rồi so hash), không chỉ trong worktree — lỗi lưu trữ của X5 không được lặp lại. Ghi APK SHA, source ZIP SHA, event id.

**S3 — Cài.** Chỉ `adb -s emulator-5554`; khởi động AVD không wipe; cài production rồi test APK; đọc lại `versionName`/`versionCode` và signer; khớp hash vừa build.

**S4 — Chạy giả (USD 0).** Bản sao script riêng `D:\P5E-private\run_v420_d_chunk.ps1` (chép từ `run_c1_chunk.ps1`, đổi khóa: chỉ chương `014`, trần ≤ 0,08, id nhóm bắt đầu `V420-D-`, thêm `-e p6_no_retry YES -e p6_max_calls 10`). Chạy `-Offline`: `final.txt` phải trùng DRAFT từng byte, số lời gọi giả = số chunk có điểm (10), 0 lời gọi thật, 0 bị từ chối. Không đạt thì sửa trong S1–S3 rồi chạy giả lại; chưa được chạy thật.

**S5 — Một lượt thật.** Preflight tài khoản/endpoint phải `MATCH` (fingerprint đọc từ nguồn riêng đã dùng ở X5; không in, không ghi vào Git). Giá cấu hình phải biết (giữ chỗ 0,25/M vào, 1,20/M ra). Một lần chạy, sổ mới, trần 0,08. Dừng ngay, không retry, không chạy lại khi: preflight không MATCH; giá UNKNOWN; giữ chỗ vượt trần; finish `length` hoặc response rỗng; lỗi hạ tầng; lời gọi thứ 11; chunk không đạt (theo service).

**S6 — Bằng chứng chạy.** Kéo về `D:\P5E-private\chunk-runs\V420-D\live\014-<runId>\`: prompts, responses, `chunks.json`, `final.txt`, `run-metadata.json`, `structural.json`, spend ledger. Kiểm ledger: chuỗi hash, RESERVE/SETTLE khớp số lời gọi, pending 0, overrun 0. Ghi SHA-256 các file vào báo cáo tổng hợp trong Git (không văn bản sách).

**S7 — Trang đọc cho owner** (`D:\P5E-private\chunk-runs\V420-D\READING_014.html`, ngoài Git). Một dòng cho: mọi dòng app đổi, và mọi dòng có lỗi tự xưng của bước B (`D:\P5E-private\product-plan-A\stepB-self-cases.json`, chương 014) dù app có đổi hay không. Cột: RAW, DRAFT, bản app, FINAL (ghi rõ "tham chiếu, không phải luật"), hàng được trích (`R<n>`: speaker → listener, self/call) với dòng xưng hô đã đổi, trạng thái và lý do của guard (`ADDRESS_*`), ô phân xử của owner (ĐÚNG / SÓT / SAI / CHƯA CHẮC). Nhóm riêng các dòng đổi `cậu` ↔ tên.

**S8 — Đếm sơ bộ (không phải kết luận chất lượng).** Với các lỗi tự xưng bước B của 014: số dòng app đổi đúng từ mà FINAL dùng / không đổi / đổi khác. Số dòng app đổi ngoài nhóm đó. Số câu trả lời bị guard giữ theo từng lý do (`ADDRESS_ROW_NOT_CITED`, `ADDRESS_FORM_NOT_IN_CITED_ROW`, `ADDRESS_CITED_ROW_EXCLUDED`, `ADDRESS_CITED_ROW_NOT_ADDRESSED`, …). Số đổi `cậu` ↔ tên. Chunk chưa tới ghi `NOT_REACHED`.

**S9 — Ghi trạng thái và xuất bản.** Báo cáo tổng hợp `artifacts/qa/v4.20-d-014-<date>/QA_REPORT.md` + `SUMMARY.json` (chỉ số, hash; không văn bản sách), mirror sang `backup/qa/...`. Cập nhật plan mục 5 (D), `BUILD_STATE.md`, snapshot, checklist bước 05. Commit, quét dải sẽ đẩy (không secret, không văn bản sách, không fingerprint), push `feature/v4.20`. Dừng chờ owner đọc trang S7.

## 6. Tiêu chí của công việc này

- Hoàn thành khi: S1–S4 đạt; đúng một lượt thật đã chạy hết hoặc dừng theo luật S5; sổ chi hợp lệ, ≤ USD 0,08, ≤ 10 lời gọi, 0 retry; trang đọc S7 và số đếm S8 có đủ; bằng chứng S6 có hash.
- Không được tự kết luận C1 đạt hay không: quyết định phát hành/bỏ là của owner sau khi đọc S7, theo tiêu chí mục 7 của plan (bỏ nếu số lỗi tự xưng sửa đúng không lớn hơn số sửa sai mới).
- Lỗi build/test cục bộ là `FAILED_REPAIRING`: sửa trong phạm vi S1–S3, chạy lại, không đổi mã sản xuất.

## 7. Không làm

Không chạy chương khác, không chạy lần hai, không đổi prompt/finder/guard sau khi thấy kết quả, không dùng phần trần còn lại, không đụng điện thoại, không merge/tag, không viết thêm tài liệu ngoài các file ở mục 4.
