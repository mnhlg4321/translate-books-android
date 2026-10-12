# Yêu cầu làm việc — M4 RECONCILE chương 001 (v4.18)

Ngày lập: 2026-10-01, cập nhật 2026-10-02. Người giao: owner. Dùng nguyên văn tài liệu này làm chỉ dẫn cho một phiên làm việc mới.
Repository: `D:\App Translate Books`, branch `feature/v4.18-p5e-runner-repair-20260917`, mốc bắt đầu = commit chứa bản cập nhật 2026-10-02 của tài liệu này (sau `ba58cefc`).

## Trạng thái khi giao (2026-10-02)

- Owner đã duyệt phạm vi mục 0 (2026-10-01). Chưa bước nào thực hiện xong: chưa build, chưa cài, chưa gọi provider; DB pilot vẫn `9fa69f6b…`.
- Lần thử bước 1 trước đó dừng vì máy chưa cắm; thư mục `D:\P5E-private\m4-m0-20261001-235745967` chỉ có `ABORTED_NO_DEVICE.txt` và một file 0 byte (không phải DB export). Giữ nguyên, không dùng làm evidence.
- Owner đã cắm máy: lúc giao `adb devices -l` thấy `15e84958 device` (CPH2691). Phiên mới vẫn phải kiểm lại.
- Code offline đã có trên branch (sẽ vào APK M4): ngoài gói M4 còn có L2/L3 engine, adapter L2/L3, coordinator + export (`be105e16`, `ba8b4bbb`). Mốc kiểm thử: engine 244/244, app unit 289/289, lint PASS, androidTest compile PASS, M4 host self-test 74/74.
- Endpoint account fingerprint (bước 5) do owner đưa vào phiên mới; thiếu thì làm bước 1–4 rồi hỏi đúng giá trị này trước bước 5.

## 0. Quyền được cấp trong yêu cầu này

Owner cấp **đúng một** phạm vi sau, dùng một lần, không gia hạn ngầm:

1. Build một production APK và một AndroidTest APK mới từ HEAD của branch trên, chỉ bằng `scripts/build-and-save.ps1` và `scripts/build-and-save-android-test.ps1`; lưu hai nơi theo policy trước khi cài.
2. Cài hai APK đó lên đúng thiết bị serial `15e84958` (thay code211 / test `CCAAE0AD…`), giữ dữ liệu app.
3. Cho phép DB pilot migrate v24 → v25 (chỉ thêm bảng `editorial_phase_artifacts` và một index) khi app mở DB lần đầu, **sau khi** đã có bản sao DB trước cài (bước 2).
4. **Một** lần gọi provider pha `L1_RECONCILE` cho chương `001`, predecessor RAW event 7 `7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e`, qua `scripts/p5e-m4-reconcile-event.ps1`. Trần: 1 primary call, 0 schema repair, 0 network retry, input 100,000 token, output 4,096 token, tổng 104,096 token, **USD 0.05**, 120 s; route OpenRouter `openai/gpt-5.6-luna` như event 7.
5. Đọc chỉ-đọc: package/APK/DB export, `logcat -d -s P5E_RAW:V`, restart app (force-stop rồi đọc lại) cho M5/M6.

Không được: gọi RAW lại; retry/repair bất kỳ call nào; gọi lần thứ hai dù kết quả STOP/UNKNOWN; khôi phục zero-state; xóa/sửa evidence cũ (đặc biệt `D:\P5E-private\raw-live-a43-preauth-f8a0ee32…`); đọc, in, ghi hay chuyển API key (key nằm trong cài đặt app trên máy; phiên chỉ được đối chiếu fingerprint); cài trên thiết bị khác; mở launcher A4.3/owner-window cũ; tạo branch/release/checklist mới; stage toàn workspace.

Quyết định đã có (không hỏi lại): phương án **B** — RAW event 7 là predecessor duy nhất cho M4, verdict formal của event 7 giữ `RAW_NOT_ACCEPTED` (`docs/P5E_EVENT7_SUPPLEMENTARY_VERIFICATION_20261001.md` mục 5).

## 1. Đọc trước khi làm (theo thứ tự)

1. `AGENTS.md`, `GIT_WORKFLOW.md`, `DEVELOPMENT_WORKFLOW.md` (phần build/archive).
2. `EDITORIAL_RECOVERY_V4_18.md` — mục 9a và "G2 chi tiết — M4", mục 10.
3. `WORKSPACE_SNAPSHOT.md`, `HANDOFF.md` (khối cập nhật 2026-10-01 đầu file).
4. `docs/P5E_EVENT7_SUPPLEMENTARY_VERIFICATION_20261001.md`.
5. Mã M4: `app/src/main/java/com/ml/tblandroidtxt/EditorialP5EFreshRawLiveRunner.java` (`dispatchReconcile`, `inspectReconcileLineage`), `EditorialP5CExactBindingExecution.java` (`executeReconcile`), `app/src/androidTest/.../EditorialP5EReconcileLiveInstrumentedTest.java`, `scripts/p5e-m4-reconcile-event.ps1`.

Xác minh: branch, HEAD, `git status` (thay đổi `.idea` và docs P5E cũ của owner phải giữ nguyên, không stage), remote khớp. Không rerun suite rộng khi bytes chưa đổi; mốc hiện tại: engine 244/244, app 276/276, lint PASS, androidTest compile PASS, M4 host self-test 74/74.

## 2. Trình tự thực hiện

Mỗi bước ghi kết quả (lệnh, exit code, hash, đường dẫn evidence). Một bước FAIL → làm theo cột "Nếu không đạt"; không nhảy bước.

| # | Việc | Đạt khi | Nếu không đạt |
|---|---|---|---|
| 1 | **M0 chỉ-đọc trước mọi thay đổi** (`powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\p5e-m4-m0-observation.ps1 -Label m4-m0`; kiểm lineage trên bản DB sao bằng SQLite host chỉ-đọc, ví dụ `py -3` + `sqlite3` với URI `mode=ro`): thiết bị `15e84958` online; production `4.18-p5e.3`/211, APK `E9CF282C…`; test `CCAAE0AD…`; cert `47f31389…c155`; DB `tbl_android_txt.db` SHA-256 `9fa69f6b909c16b61ed2925c0c1509d1b60c910aa3cc4e8057166184e8db7c98`; WAL/SHM vắng; lineage: 1 attempt RAW `COMMITTED` = `7a5e3428…`, 1 authorization receipt, 1 lifecycle, 0 reconciliation, 0 attempt `L1_RECONCILE` | Mọi giá trị khớp | Dừng toàn bộ, không build/cài; báo owner giá trị lệch |
| 2 | **Sao lưu DB trước cài**: export nhị phân chỉ-đọc (cùng cơ chế `p5e-db-binary-export.ps1`/thư viện supervisor) vào thư mục mới `D:\P5E-private\m4-preinstall-<UTC>`; ghi SHA-256 | Hash bản sao = hash M0 | Không cài; sửa cục bộ cách export (tối đa 2 vòng), không đổi thiết bị |
| 3 | **Build**: `scripts/build-and-save.ps1` (versionCode > 211, versionName mới tự gán) và `scripts/build-and-save-android-test.ps1` cho đúng production đó; payload ở `artifacts/` và `backup/` | Hai payload đủ APK/README/BUILD_INFO/SHA-256/source ZIP, parity hai nơi; unit tests trong build đạt | Lỗi build/test là `FAILED_REPAIRING`: sửa trong phase, build lại; không cài APK chưa archive |
| 4 | **Cài**: `scripts/install-validated.ps1` check-only rồi cài production; script cài test package check-only rồi cài (lỗi stderr sau khi cài là lỗi đã biết — xác minh bằng readback độc lập). **Không mở app** sau khi cài | Readback: version/code/APK hash/cert khớp build vừa archive; test APK hash khớp; DB hash vẫn `9fa69f6b…` (chưa migrate) | Hash lệch: dừng, không chạy event; báo owner. Cài thất bại giữa chừng: không thử thiết bị khác; đọc trạng thái, báo owner |
| 5 | **Chuẩn bị tham số event**: authorization id mới (ví dụ `P5E-M4-RECONCILE-001-20261001-01`) + SHA-256; viết approval manifest (trích mục 0 của tài liệu này + hash APK/DB thực đo) vào thư mục private, lấy SHA-256; endpoint account fingerprint do owner cung cấp từ cùng nguồn đã dùng cho event 7 (input `ENDPOINT_ACCOUNT_FINGERPRINT` của cửa sổ owner; evidence event 7 đã REDACTED giá trị này) — phiên không tự tính từ key, thiếu thì hỏi owner đúng giá trị này; project row id `2` (readback event 7, xác nhận lại); expected schema version = **25** (migrate xảy ra bên trong test, sau khi hash DB v24 đã được đối chiếu); expected DB hash = hash đo ở bước 4 | Tập tham số đầy đủ, khớp tên trong `EditorialP5EReconcileLiveInstrumentedTest` (script tự đối chiếu trước launch) | Thiếu giá trị: dừng trước launch; không đoán |
| 6 | **M0 lần cuối** ngay trước event (giống bước 1 nhưng với APK mới) | Khớp; DB vẫn v24 `9fa69f6b…` | Lệch (ví dụ app đã bị mở → DB đã v25): đo lại hash mới, cập nhật tham số DB hash, ghi rõ nguyên nhân; nếu lineage đổi → dừng |
| 7 | **Event M4 — một lần duy nhất**: `scripts/p5e-m4-reconcile-event.ps1 -Serial 15e84958 -EvidenceRoot D:\P5E-private ...` | Script `launchCount=1`, có `M4_OUTCOME.json` + manifest | Typed stop **trước** launch (`BEFORE_STOP_NO_LAUNCH`): được sửa lỗi cục bộ của script/tham số rồi chạy lại phần Before, tối đa 2 vòng cùng chữ ký lỗi. **Sau** khi đã launch: tuyệt đối không chạy lại |
| 8 | **Đọc kết quả**: `M4_OUTCOME.json`; `adb logcat -d -s P5E_RAW:V` chỉ-đọc (chỉ dòng chat/dispatch, không nội dung model) | Phân loại một trong: `RECONCILE_COMMITTED`, `RECONCILE_TYPED_STOP`, `RECONCILE_NOT_DISPATCHED`, `EVIDENCE_INCOMPLETE` | UNKNOWN/thiếu evidence: ghi `EVIDENCE_INCOMPLETE`, không suy ra chi phí = 0, không gọi lại; đối chiếu metadata nhà cung cấp cần quyền riêng |
| 9 | **Nếu COMMITTED — M5 kiểm artifact**: readback attempt `L1_RECONCILE` `COMMITTED`, `predecessor_identity` = `7a5e3428…`, report `artifactType=REPORT_L1`, `phase=L1_RECONCILE`, `schemaVersion=safe4.full.report-l1.v1`, receipt có cùng phase/predecessor, cả hai blob có mặt (commit nguyên tử), integrity ok, chỉ thêm đúng các dòng lineage của RECONCILE + bảng v25 rỗng | Tất cả đúng | Sai/thiếu: ghi rõ mốc thiếu, không chạy lại provider |
| 10 | **M6 restart/reopen**: `am force-stop com.ml.tblandroidtxt`, export DB chỉ-đọc lần nữa, so hash report/receipt của attempt RECONCILE với bước 9 | Hash giống hệt | Sửa đường đọc lại cục bộ nếu là lỗi host; không gọi provider |
| 11 | **M7 ghi nhận**: cập nhật checklist P5.4/P5D.7/P5 exit (chỉ tick khi đủ evidence M4–M6), bảng chain: manifest → pack hash → binding/run → RAW `7a5e3428…` (ngoại lệ B) → REPORT_L1 → receipt → reopen | Evidence cụ thể cho mỗi mốc | Ghi đúng mốc thiếu, không làm tròn |

Nếu kết quả là `RECONCILE_TYPED_STOP`: ghi reason code, phân loại (INPUT/REPAIR/RETRY/CONTENT), đọc log để tìm nguyên nhân như event 6, sửa offline nếu là lỗi app/prompt; **không** chạy event mới khi chưa có quyền mới.

## 3. Đầu ra bắt buộc

- Thư mục evidence M4 dưới `D:\P5E-private\` (không đưa vào Git): `M4_OUTCOME.json`, manifest SHA-256, export Before/After, stdout/stderr đã lọc.
- Hai payload build trong `artifacts/` và `backup/` (đúng policy, không ghi đè).
- Trong repo: một mục "Event M4" ngắn trong `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` (hoặc báo cáo hiện có tương ứng) với hash, chi phí báo cáo, phân loại; cập nhật `EDITORIAL_RECOVERY_V4_18.md` (bảng M4, mục 10 — đúng một next action), `HANDOFF.md`, `BUILD_STATE.md`, `WORKSPACE_SNAPSHOT.md` (thay giá trị hiện tại, không append lịch sử), checklist v4.18.
- Commit theo nhóm (build/pin nếu có sửa mã; docs/evidence riêng), message có dòng `Co-Authored-By`; push lên `origin` cùng branch. Không đưa key, nội dung model, DB hay dữ liệu riêng tư vào Git.

## 4. Báo cáo cuối phiên (ngắn)

1. Điều mới được chứng minh (M4/M5/M6 đạt hay không, với hash).
2. Chi phí và số call thực tế (phải ≤ 1 call, ≤ USD 0.05).
3. Lỗi đã sửa, test đã chạy và giới hạn.
4. Gate đạt/chưa đạt (P5.4, P5 exit, P6 ready).
5. Commit/push.
6. Đúng một bước tiếp theo hoặc một quyết định cụ thể cần owner.

## 5. Nếu M4 xong sớm hoặc bị chặn ngoài tầm — việc offline được phép tiếp

Chỉ local, không provider/thiết bị: adapter OpenRouter cho hai call L3 (`EditorialL3Execution`, các phase `L3_RAW_FIRST_REAUDIT`/`L3_RECONCILE`, block `L3_REAUDIT_CANDIDATES`); pha `L2_RAW_DISCOVERY` đối chiếu candidate counts với REPORT_L1; coordinator app nối REPORT_L1 → `EditorialL2Execution` → `EditorialL3Execution` với `EditorialPhaseArtifactStore` (G5). Mỗi việc có test JVM/fake provider; giới hạn hai vòng cùng chữ ký lỗi rồi đổi cách.
