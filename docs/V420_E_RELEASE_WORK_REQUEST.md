# v4.20 — Yêu cầu làm việc: bước E, phát hành 4.20.0 (C1 + C2')

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (mục 5). Checklist: `release_checklists/v4.20.md` (bước 05–09 ở đây; 10–14 sau quyết định tag của owner). Nhánh duy nhất: `feature/v4.20`, worktree `D:\P5E-builds\wt-v4.20`. Không làm trong `D:\App Translate Books` (163 thay đổi dở của owner).

## 1. Mục đích và quyết định sản phẩm

Quyết định cần có: **tag và merge 4.20.0 hay không.** Nội dung phát hành: C1 (Biên tập bắt model ghi số hàng pronoun; app chỉ nhận dạng của đúng hàng) và C2' (lỗi định dạng của một điểm chỉ giữ dòng đó, từ hai điểm mới chặn chunk). Bằng chứng đã có: 014, 8 dòng tự xưng sửa đúng / 0 sửa sai theo owner, 3 dòng chunk 13 qua replay owner xác nhận đúng. Bước E thêm: bản RC chạy được và lưu/mở lại/xuất đúng trên thiết bị (provider giả), hiệu năng thật, và **một chương trọn vẹn 017 chạy thật trên đúng bản RC** để biết kết quả C1 có giữ trên chương khác không.

## 2. Quyền (owner duyệt 2026-10-13)

| Mục | Giá trị |
|---|---|
| Lượt thật | đúng một lượt chương **017** (`D:\P5E-private\q2-inputs\017\`), `openai/gpt-5.6-luna` medium, tối đa **10** lời gọi, **không retry**, sổ mới `V420-E-017-<yyyymmdd>-<8 ký tự đầu runId>`, trần **USD 0,08** (giữ chỗ ước tính USD 0,061907, 10 chunk có điểm, 80 điểm) |
| Thiết bị | chỉ `emulator-5554` (AVD `tbl-code113-dqa-api35`); không đụng điện thoại `15e84958`; không wipe/clear/uninstall |
| Xuất bản | push `feature/v4.20` sau khi quét; **không tag, không merge** trước khi owner đọc trang 017 và duyệt |

## 3. Baseline đã kiểm

`feature/v4.20` tại commit chứa tài liệu này (mã sản xuất như `7dff39ef`, hợp đồng `CP-IMPL-17`). Archive sạch `7dff39ef`: engine 715 (1 skip), app debug 473 (3 skip), 0 lỗi; lint 0 lỗi / 57 cảnh báo; androidTest compile OK. App phát hành hiện tại 4.18.0 / code 266; emulator đang có `4.20-d.1` / code 268.

## 4. File được và không được sửa

- Được: `RELEASE_NOTES.md`, `CHANGELOG.md` (mục 4.20.0); tài liệu trạng thái (`PRODUCT_DIRECTION_PLAN.md` mục 5, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md`, `release_checklists/v4.20.md`); báo cáo QA trong `artifacts/qa/v4.20-e-*/`; bản sao script riêng ngoài Git.
- Được sửa test chỉ khi test hỏng vì môi trường/dữ liệu thử (`FAILED_REPAIRING`), ghi nguyên nhân; không sửa để che lỗi sản phẩm.
- Không sửa: `app/src/main`, `editorial-engine/src/main` sau khi build RC (nếu buộc phải sửa thì build RC lại và làm lại E3–E7); file RAW/DRAFT/glossary/pronoun của owner; các script riêng cũ.

## 5. Các bước

**E1 — Hồi quy trên archive sạch** của commit sẽ build RC: `:editorial-engine:test`, `:app:test` (debug/release/benchmark), `:app:lintDebug`, `:app:compileDebugAndroidTestJavaWithJavac`, `scripts/p6` và `scripts/chunk` (Python). Ghi số theo PASS / SKIP / FAIL từng method, không gộp SKIP vào PASS.

**E2 — Ghi chú phát hành.** `RELEASE_NOTES.md` và `CHANGELOG.md` mục 4.20.0: C1, C2', hợp đồng `CP-IMPL-17` (lượt Biên tập cũ không chạy tiếp, phải tạo lượt mới; kết quả cũ vẫn xem/xuất được), giới hạn đã biết (lỗi tự xưng mà finder không gắn cờ, lỗi phát sinh ở khâu dịch chưa xử lý, lời kể chương 004). Commit riêng `docs(release): ...`.

**E3 — Build RC.** `scripts/build-and-save.ps1 -ExactReleaseVersion 4.20.0` chỉ chạy trên nhánh tên `feature/v4.20.0`: dùng worktree tạm với nhánh cục bộ `feature/v4.20.0` trỏ đúng commit E2 (không push, gỡ sau khi xong), như cách đã làm cho 4.18.0. versionCode > 268. Rồi `scripts/build-and-save-android-test.ps1` ghim vào APK sản xuất, source ZIP và cert SHA `47F31389…EF3C155`. Event phải nằm ở `D:\App Translate Books\artifacts\builds|test-builds\v4.20.0\…` và `backup\…`, so hash; không ghi đè event có sẵn.

**E4 — Cài và QA thiết bị (provider giả, USD 0).** `scripts/install-validated.ps1`, chỉ `emulator-5554`; đọc lại version/signer. Chạy cùng các nhóm như QA RC 4.18.0 (`D:\App Translate Books\artifacts\qa\v4.18-r1\r1.5-rc-device\QA_REPORT.md`): danh sách đóng băng (frozen allowlist), lớp chunk (`EditorialPairStore`, `EditorialPairUi`, `EditorialPairAddressKeepSqlite`, `EditorialApiStore`, `EditorialApiBienTapFlow`, `EditorialApiBienTapUi`), smoke Translation/sản phẩm, và khởi động lại seed/verify (app bị force-stop giữa hai pha; mở lại không gọi provider). Tổng hợp bằng `scripts/qa/instrumentation_summary.py`. Dọn dữ liệu thử còn sót trước khi chạy (bài học glossary của 4.18).

**E5 — Hiệu năng thật.** Mã sản xuất đã đổi so với 4.18.0 nên không dùng lại số cũ: build benchmark từ cùng source (như R1.3 của 4.18), chạy Macrobenchmark và Perfetto trên `emulator-5554`, giữ nguyên đầu ra. Ghi rõ là số emulator.

**E6 — Ảnh và video** trên chính bản RC bằng `EditorialR1MediaWalkthroughInstrumentedTest` (`r1_media=run`), dữ liệu tổng hợp, không văn bản sách.

**E7 — Một lượt thật chương 017 trên bản RC.** Bản sao script riêng `D:\P5E-private\run_v420_e_chunk.ps1` từ `run_v420_d_chunk.ps1`, đổi khóa: chỉ chương `017`, trần ≤ 0,08, id nhóm bắt đầu `V420-E-`, giữ `-e p6_no_retry YES -e p6_max_calls 10`. Chạy giả trước (`-Offline`): `final.txt` trùng DRAFT từng byte, số lời gọi giả = 10, 0 thật. Rồi đúng một lượt thật; preflight tài khoản phải `MATCH` (fingerprint từ nguồn riêng, không in). Dừng ngay, không retry, không chạy lại khi: preflight không khớp, giá UNKNOWN, giữ chỗ vượt trần, finish `length` hoặc response rỗng, lỗi hạ tầng, lời gọi thứ 11, chunk bị chặn. Lưu prompt, response, `chunks.json`, `final.txt`, metadata, sổ chi vào `D:\P5E-private\chunk-runs\V420-E\live\017-<runId>\`; kiểm sổ (chuỗi hash, RESERVE = SETTLE = số lời gọi, pending 0, overrun 0).

**E8 — Trang đọc 017** (`D:\P5E-private\chunk-runs\V420-E\READING_017.html`, ngoài Git), cùng dạng trang 014: mọi dòng app đổi, và mọi dòng có lỗi tự xưng của bước B ở chương 017 (`D:\P5E-private\product-plan-A\stepB-self-cases.json`, 12 lần thay từ) dù app có đổi hay không. Cột: RAW, DRAFT, bản app, FINAL (ghi rõ chỉ tham chiếu), hàng được trích `R<n>`, trạng thái và lý do guard (kể cả lỗi định dạng được giữ theo C2'), ô phân xử ĐÚNG / SÓT / SAI / CHƯA CHẮC. Nhóm riêng dòng đổi `cậu` ↔ tên. Kèm số đếm sơ bộ (không phải kết luận): dòng B đổi đúng từ FINAL dùng / giữ / đổi khác; dòng đổi ngoài B; số dòng giữ do C2'; chunk NOT_REACHED nếu có.

**E9 — Checklist, gói release, xuất bản.** Điền evidence bước 05–09 của `release_checklists/v4.20.md`; `scripts/verify-release-workflow.ps1 -Gate PreTag` phải PASS. `scripts/archive-release.ps1` với event `build-YYYYMMDD-HHMMSS`, GitRef = commit RC, đủ APK, QA report, CHANGELOG, BUILD_STATE, RELEASE_NOTES, Perfetto, Macrobenchmark, ảnh, video, source ZIP; kiểm hai bản mirror và SHA256SUMS. Báo cáo tổng hợp `artifacts/qa/v4.20-e-<date>/QA_REPORT.md` + `SUMMARY.json` (chỉ số, hash; không văn bản sách, không fingerprint), mirror sang `backup/qa/`. Cập nhật plan mục 5, BUILD_STATE, snapshot. Commit, quét, push `feature/v4.20`. **Dừng chờ owner đọc trang 017 và quyết định tag/merge.**

## 6. Tiêu chí của công việc này

- Hoàn thành khi: E1 không có FAIL; RC build và lưu đủ hai nơi; QA thiết bị E4 không có FAIL chưa giải thích; hiệu năng E5 và media E6 thật; E7 đúng một lượt thật, sổ hợp lệ, ≤ USD 0,08, ≤ 10 lời gọi, 0 retry; trang E8 đủ; PreTag PASS và gói `build-*` kiểm xong.
- Không được tự kết luận phát hành: owner đọc trang 017. Tiêu chí của plan: không phát hành nếu số lỗi tự xưng sửa đúng không lớn hơn số sửa sai mới; một sửa sai mới nghiêm trọng là lý do dừng để chẩn đoán trước khi tag.
- Lỗi cục bộ là `FAILED_REPAIRING`: sửa trong phạm vi mục 4, chạy lại.

## 7. Không làm

Không tag/merge, không chạy chương khác hay lượt thứ hai, không đổi prompt/finder/guard sau khi thấy kết quả 017, không dùng phần trần còn lại, không đụng điện thoại, không thêm bộ kiểm/cổng/chỉ mục mới, không sửa file của owner.
