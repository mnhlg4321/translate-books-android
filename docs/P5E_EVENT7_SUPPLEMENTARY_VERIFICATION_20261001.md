# Event 7 — xác minh bổ sung độc lập (2026-10-01)

Loại tài liệu: bằng chứng **bổ sung**, không thay thế verdict gốc. Verdict formal của event 7 vẫn là `RAW_NOT_ACCEPTED` (`OUTCOME_VERIFICATION.json` gốc, SHA-256 `a0d479513f15b0ef0d04d0fc3b6e237569c7b8a175c40226a76ec6919d52bd63`). Không file nào trong thư mục evidence gốc bị sửa; không gọi provider, không chạm thiết bị, không đổi DB.

- Event: `D:\P5E-private\raw-live-a43-preauth-f8a0ee32a8ef14b1950134742bf53ce004588131981ff19b0fb5ee3b825bd77e` (candidate `FA3F5F11…`, DecisionId `p5e-a43-owner-30c83137…`).
- Workspace: branch `feature/v4.18-p5e-runner-repair-20260917`, HEAD `3057919b` khi kiểm.
- Bảo toàn: 24 file gốc được băm trước và sau mọi bước; hai danh sách trùng nhau (`docs/evidence/p5e-event7-supplementary-20261001/event7-original-evidence-sha256.txt`).

## 1. Kết quả kiểm

| # | Câu hỏi | Phương pháp | Kết quả |
|---|---|---|---|
| S1 | Verdict gốc có tái lập được không? | Verifier của chính commit `dd41e858` (helper `9963a027…`, đúng hash pin trong provenance event) chạy trên **bản sao** evidence. Vì verifier pin đường dẫn tuyệt đối, phiên chạy ánh xạ đường dẫn gốc sang bản sao/cây `git archive` (shadow `Get-P5ECanonicalPath`, `Test-Path`, `Get-Item`, `Get-Content`, `Get-FileHash`, hai lời gọi `ReadAllBytes`); logic verifier không đổi | Output trùng byte với bản gốc (SHA-256 `a0d47951…bd63`): `RAW_NOT_ACCEPTED`, đúng 2 lỗi `VALUE_MISMATCH:test.targetPackage`, `VALUE_MISMATCH:test.runner`. Script: `docs/evidence/p5e-event7-supplementary-20261001/repro-original-verifier-remapped.ps1` |
| S2 | `errorCount=2` có nghĩa mọi kiểm tra khác đạt? | Đọc mã `scripts/p5e-raw-live-supervisor.ps1` bản `dd41e858` | Có. `Add-P5EError`/`Test-P5EEqual`/`Test-P5EObjectShape` chỉ append, không dừng sớm; event 7 có `COMMITTED_READBACK_CAPTURED` nên toàn bộ `Test-P5EReadback` (production, test, database, tuple, lineage, attempt, metrics, authorization, lifecycle, artifacts, integrity) và `Test-P5EReadbackProvenance` đã chạy. Guard bỏ qua chỉ áp khi input thiếu, mà input thiếu tự báo lỗi; không guard nào áp cho event 7 |
| S3 | Giá trị thật của hai trường lỗi | `aapt2 dump xmltree` (build-tools 37.0.0) trên `installed-test-before.apk`/`-after.apk` lưu trong event | Cả hai APK SHA-256 `CCAAE0AD…E24E66` = pin. Manifest: package `com.ml.tblandroidtxt.test`, `instrumentation android:name=androidx.test.runner.AndroidJUnitRunner`, `android:targetPackage=com.ml.tblandroidtxt`. `EVENT_PLAN.json` cũng ghi runner/targetPackage đúng. Chỉ `collector-input.json`/`post-readback.json` có chuỗi rỗng |
| S4 | Hai trường có tham gia identity/integrity? | Đối chiếu contract | Không. Identity test package được ràng bởi `package`, `apkSha256`, `certificateSha256`, `sourceCommit` — đều khớp pin. `targetPackage`/`runner` là thuộc tính của manifest nằm trong chính bytes APK đã pin, nên bị xác định hoàn toàn bởi `apkSha256`. Không ảnh hưởng phase, receipt, DB preservation, lineage hay cost |
| S5 | Chữ ký | `apksigner verify --print-certs` | Test và production cùng V2 signer `47f31389…c155` = pin certificate |

Giới hạn: S1 là chạy ở chế độ thư viện với lớp ánh xạ đường dẫn, không phải CLI nguyên trạng (CLI trên bản sao cho 16 lỗi, 14 lỗi thêm đều là path/pin do đổi vị trí). S3/S5 dùng build-tools 37.0.0, khác bản 35.0.0 collector dùng. Không có kiểm nào chạy trên thiết bị.

## 2. "L1 persisted" thực chất là gì

`report.bin` (SHA-256 `6c1c183b…0351`) và `receipt.bin` (`f1255272…88e9`) có `artifactType=REPORT_L1` nhưng `phase=L1_RAW_DISCOVERY`, `predecessorIdentity=8466b95d…` (run declaration), `disposition=CONTINUE`. Theo `EditorialP5CExactBindingExecution.execute` và contract `PHASES` (`L1_RAW_DISCOVERY → L1_RECONCILE`), đây là **RAW predecessor** đã commit atomic (M2/M3/M5-cho-pha-RAW ở mức app/DB), **chưa phải REPORT_L1 sau RECONCILE**. P5.4 (REPORT_L1 thật) và M4 chưa đạt; RECONCILE là bắt buộc theo source, không phải tùy chọn.

## 3. Phân loại bất đồng

| Lớp | Kết luận |
|---|---|
| Sản phẩm/runtime | Không lỗi: dispatch `COMMITTED`, schema/receipt hợp lệ, 1 primary call, 0 repair/retry, DB integrity ok, không ghi ngoài allowlist |
| Collector/verifier | Lỗi collector: `Get-P5EPackageReadback` không trả `targetPackage`/`runner`; đã sửa ở `3057919b` (helper `A4FFEDA9…`) cho event sau, chưa chạy live. Verifier không sai: nó báo đúng dữ liệu collector ghi |
| Evidence thiếu | Không thiếu cho RAW; thiếu RECONCILE/REPORT_L1 (chưa chạy) |
| Quyền/tài nguyên | RECONCILE cần quyền live riêng (M4) |
| Tài liệu lệch | Snapshot/HANDOFF gọi kết quả là "REPORT_L1 + receipt" — đúng tên artifact nhưng dễ hiểu nhầm là L1 hoàn tất; đã ghi rõ ở plan |

## 4. Phương án

- **A (bảo toàn + xác minh độc lập)**: đã làm phần có thể làm đúng contract (mục 1). A **không** biến verdict formal thành PASS: verifier gốc pin helper `9963A027…`, nên chạy helper mới trên evidence cũ sẽ là giả kết quả chính thức. Không làm.
- **B (ngoại lệ phạm vi hẹp) — khuyến nghị**: owner chấp nhận RAW đã commit của event 7 làm predecessor **chỉ cho M4 RECONCILE của chương 001**, với điều kiện ghi kèm: verdict formal giữ `RAW_NOT_ACCEPTED`; tiêu chí không đạt duy nhất là 2 trường mô tả (S3/S4); bằng chứng S1–S5. Rủi ro: thấp — hai trường bị xác định bởi APK hash đã khớp. Ảnh hưởng phase sau: checklist P5E.9 ghi "accepted by narrow owner exception", không ghi `RAW_ACCEPTED`; mọi event sau dùng collector đã sửa và phải đạt formal. Không reset DB, không chi thêm.
- **C (event mới)**: khôi phục zero-state (xóa kết quả commit) và chạy lại RAW với candidate `67423A0C…`. Chi phí ~USD 0.003 + một chu trình thiết bị; lợi ích duy nhất là verdict formal PASS. Không khuyến nghị trừ khi owner không chấp nhận B.

Đây là chuẩn bị quyết định, không phải phê duyệt ngoại lệ, reset DB hay lượt provider mới.
