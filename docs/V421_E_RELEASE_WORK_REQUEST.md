# v4.21 — Yêu cầu làm việc: bước E, phát hành 4.21.0 (P2 dấu câu theo RAW)

Canonical plan: `PRODUCT_DIRECTION_PLAN.md` (mục 5, khối v4.21). Checklist: `release_checklists/v4.21.md`. Nhánh duy nhất: `feature/v4.21`, worktree `D:\P5E-builds\wt-v4.21`. Không làm trong `D:\App Translate Books` (thay đổi dở của owner; không switch/merge/commit ở đó).

## 1. Mục đích

Quyết định đã có: **phát hành 4.21.0**. Nội dung: trong luồng Biên tập, dấu câu của DRAFT theo RAW cho `〝〟 「」 『』`, `：` và `……` (quy ước owner 2026-10-14), do app khôi phục tất định, không gọi model; hợp đồng `CP-IMPL-19`. Bằng chứng đã có: đo bằng mã app trên 8 chương — 47 dòng khôi phục, 40 khớp FINAL, 7 là chỗ FINAL giữ dạng DRAFT, 0 lệch khác; archive sạch `4e9d4cd0`: engine 727 (1 skip), app 475 (4 skip), 0 lỗi, lint 0 lỗi. Bước E chỉ chứng minh bản RC chạy, lưu, mở lại, xuất đúng trên thiết bị và đóng gói phát hành.

## 2. Quyền (owner duyệt 2026-10-14)

| Mục | Giá trị |
|---|---|
| Lượt trả phí | **không có**; mọi kiểm tra dùng provider giả, USD 0 |
| Thiết bị | chỉ `emulator-5554` (AVD `tbl-code113-dqa-api35`); không đụng điện thoại `15e84958`; không wipe/clear/uninstall |
| Xuất bản | push `feature/v4.21`; **tag `v4.21.0` và merge `main` không cần hỏi lại** khi mọi điều kiện ở mục 4 đạt |

## 3. Các bước

**E1 — Hồi quy trên archive sạch** của commit sẽ build (sau E2): `:editorial-engine:test`, `:app:test` (debug/release/benchmark), `:app:lintDebug`, `:app:compileDebugAndroidTestJavaWithJavac`, `scripts/p6` và `scripts/chunk` (Python). Ghi PASS / SKIP / FAIL theo từng method; SKIP không tính là PASS.

**E2 — Ghi chú phát hành.** `RELEASE_NOTES.md` và `CHANGELOG.md` mục 4.21.0: dấu câu theo RAW (ba nhóm dấu, chỉ dấu đổi, chữ và số không đổi, không gọi model, chunk chỉ cần khôi phục dấu không tốn lời gọi), `CP-IMPL-19` (lượt Biên tập cũ phải tạo lượt mới; kết quả cũ vẫn xem/xuất được), giới hạn (chỉ chỗ ghép được 1–1; ngoặc lồng, ngoặc bị bỏ, `《・　・　・》` và dòng RAW tách nhiều dòng DRAFT vẫn sửa tay; chỉ có tác dụng khi chương chạy qua Biên tập). Commit `docs(release): ...`.

**E3 — Build RC.** `scripts/build-and-save.ps1 -ExactReleaseVersion 4.21.0` chỉ chạy trên nhánh tên `feature/v4.21.0`: dùng worktree tạm với nhánh cục bộ `feature/v4.21.0` trỏ đúng commit E2 (không push, gỡ sau khi xong). versionCode > 270. Rồi `scripts/build-and-save-android-test.ps1` ghim vào APK sản xuất, source ZIP và cert SHA `47F31389…EF3C155`. Event phải nằm ở `D:\App Translate Books\artifacts\builds|test-builds\v4.21.0\…` và `backup\…`, so hash; không ghi đè event có sẵn.

**E4 — QA thiết bị (provider giả).** `scripts/install-validated.ps1`, chỉ `emulator-5554`; đọc lại version/signer. Cùng các nhóm như RC 4.20.0 (`artifacts/qa/v4.20-ebis-20261013/QA_REPORT.md` trên nhánh `feature/v4.20`): danh sách đóng băng, lớp chunk (`EditorialPairStore`, `EditorialPairUi`, `EditorialPairAddressKeepSqlite`, `EditorialApiStore`, `EditorialApiBienTapFlow`, `EditorialApiBienTapUi`), smoke Translation/sản phẩm, khởi động lại seed/verify (force-stop giữa hai pha, mở lại không gọi provider). Tổng hợp bằng `scripts/qa/instrumentation_summary.py`. Dọn dữ liệu thử còn sót trước khi chạy. Nếu một test thiết bị hỏng vì câu trả lời giả hoặc dữ liệu thử chưa theo hợp đồng mới: `FAILED_REPAIRING`, sửa test (không sửa mã sản xuất), ghi nguyên nhân, giữ log hỏng.

**E5 — Hiệu năng thật.** Mã sản xuất đã đổi so với 4.20.0: build benchmark từ cùng source, chạy Macrobenchmark và Perfetto trên `emulator-5554`, giữ nguyên đầu ra, ghi rõ là số emulator; không dùng lại số cũ.

**E6 — Ảnh và video** trên bản RC bằng `EditorialR1MediaWalkthroughInstrumentedTest` (`r1_media=run`), dữ liệu tổng hợp, không văn bản sách. Nếu tạm dời DB chính của emulator thì khôi phục và so hash như lần 4.20.0.

**E7 — Checklist và gói RC.** Điền evidence bước 05–09 của `release_checklists/v4.21.md`; `scripts/verify-release-workflow.ps1 -Gate PreTag` phải PASS. `scripts/archive-release.ps1` event `build-YYYYMMDD-HHMMSS`, GitRef = commit RC, đủ APK, QA report, CHANGELOG, BUILD_STATE, RELEASE_NOTES, Perfetto, Macrobenchmark, ảnh, video, source ZIP; kiểm hai bản mirror và SHA256SUMS. Báo cáo `artifacts/qa/v4.21-e-<date>/QA_REPORT.md` + `SUMMARY.json` (chỉ số, hash; không văn bản sách), mirror sang `backup/qa/`.

**E8 — Tag và merge** (chỉ khi mục 4 đạt): tag annotated `v4.21.0` trên commit ghi trong event `build-*`; điền bước 10; PreBackup PASS; `scripts/archive-release.ps1` event `tag-v4.21.0` (GitRef = tag, `-ProductionGitRef` = commit build RC); kiểm mirror; điền bước 11–14; đặt `Workflow status: COMPLETE`; Complete gate PASS; merge `--no-ff` vào `main` trong worktree tạm (tiêu đề `Merge feature/v4.21 ...` theo hook); kiểm tree `main` = tree nhánh; gỡ worktree tạm.

**E9 — Xuất bản.** Quét dải sẽ đẩy (không secret, không văn bản sách, không fingerprint); push `feature/v4.21`, tag `v4.21.0`, `main` (fast-forward từ origin, không force); xác nhận bằng `git ls-remote`. Cập nhật plan mục 5 (E xong), BUILD_STATE, snapshot (next action: owner dùng 4.21.0).

## 4. Điều kiện để tag/merge

Tất cả phải đạt, thiếu một thì dừng ở E7 và báo owner:

- E1 và E4 không có FAIL chưa giải thích (FAILED_REPAIRING đã sửa và chạy lại đạt thì được);
- RC và APK test lưu đủ hai nơi, hash khớp;
- E5 và E6 có đầu ra thật;
- PreTag PASS, gói `build-*` kiểm xong;
- mã sản xuất của RC đúng bằng commit đã chạy E1.

## 5. Không làm

Không gọi provider, không chạy chương thật, không sửa `app/src/main` hay `editorial-engine/src/main` sau khi build RC (nếu buộc phải sửa thì build RC lại và làm lại E1–E7), không đụng điện thoại, không sửa file của owner, không thêm bộ kiểm/cổng mới.
