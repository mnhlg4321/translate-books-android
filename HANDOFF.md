# Handoff — Editorial v4.18

Ngày bàn giao: 2026-10-01 (+07:00).

## Trạng thái mới nhất — ưu tiên đọc trước

**P6 R0–R7 (tiến độ, cập nhật mỗi nhóm).** R0–R4 xong, mỗi nhóm có commit và bằng chứng. R4 trên emulator-5554 với APK lưu trữ 4.18-p6.5/code218 (production EFBADCAA…2712, AndroidTest 114AD1D3…52FB): coordinator 9/9 gồm chuỗi ledger (protected span, INSERT_AFTER, final-read VI_L2 và FINAL, receipt VERIFIED, export từ chối receipt giả, resume không gọi lại). Pilot không bị đụng. Next: R5 rồi dừng hỏi Q1–Q5.

**Cập nhật 2026-10-02 (owner chấp nhận chất lượng chương 001).** W6 đạt; chương 001 quality-accepted (hai khoảng trống tiêu chí L1-khung và đếm candidate chưa đóng chính thức). 1/3 chương đại diện. Ứng viên hai chương còn lại (số liệu chỉ-đọc từ `D:\Ebooks\MERCEDES\VOL 5`): `docs/P6_G6_CHAPTER_CANDIDATES_20261002.md` — đề xuất 007 (dày thoại/xưng hô) và 010 (DRAFT dài nhất). Mỗi chương mới cần project/binding riêng + L1 RAW/RECONCILE + chuỗi L2/L3 (ước tính ~USD 0.05). Next action: owner chọn chương và cấp quyền (plan mục 10).

**Cập nhật 2026-10-02 (P6 W4–W6, owner đã ☑ D1–D4).** Pilot `15e84958` chạy `4.18-p6.2`/215 (APK `51BA2A2B…9703`; AndroidTest vẫn `26CB0563…`). Một chuỗi L2/L3 cho chương 001 từ UI: `L2_EDIT` `e1a4c638…` và `L3_FINAL` `867a23f8…` COMMITTED, FINAL `a7d5f99e…` (26,466 B) = VI_L2, chỉ dòng 237 khác DRAFT (xóa một từ Nhật sót), release numbers 0, 4 call USD 0.04257, mở lại và xuất TXT (`Download/editorial_001_final.txt`) có hash = FINAL. Chưa phải `FINAL_OUTPUT_ACCEPTED`: owner chưa đọc, L1 là bộ khung, đếm candidate L2 (49) ≠ L3 (257). Sửa UX trong repo nhưng chưa cài: `describeRunning`, trình xem bản cuối. Evidence: `docs/P6_W4_W6_CHAPTER_001_EVIDENCE_20261002.md`. Next action: owner đọc mẫu và chốt hai chương còn lại (plan mục 10).

**Cập nhật 2026-10-02 (P6 W1–W3).** Theo `docs/P6_G5_G6_WORK_REQUEST_20261002.md`: D1–D4 (mục 3) trong bản `f9b1bec0` **chưa được đánh dấu**, nên làm W1–W3 và giữ W4–W6. W1 `a8c01b79` (L2_RAW_DISCOVERY hướng d, đếm resolution do app, trần theo cuộc gọi), W2 `bc10aca1` (cổng chạy một cửa + hộp thoại cấp phép), `7078a1d3` (test seed chỉ-emulator), `4bc1aa27` (sửa chữ "execution blocked" ở thẻ P4). Build `4.18-p6.2`/215 `51BA2A2B…9703` + AndroidTest `41ED827E…6500` lưu hai nơi; emulator: 7+8+5+19 test OK và UI smoke đạt (`docs/P6_W3_EMULATOR_QA_20261002.md`). Pilot vẫn `4.18-p5e.5`/213, không cài gì, không gọi provider. Next action: owner đánh dấu D1–D4 (plan mục 10).

**Cập nhật 2026-10-02 (G5 offline).** Commit `a6da1a35`: `inspect` đọc tiến độ L1→L2→L3→FINAL chỉ từ hàng bền (CLAIMED/RECOVERY_REQUIRED = quyết định phục hồi của owner, không tự retry), thẻ chương P4 hiển thị tiến độ, xem bản cuối và Xuất TXT qua SAF có đọc lại hash; chạy L2/L3 còn khóa. App unit 298/298, lint PASS, androidTest compile PASS; UI và 4 test instrumented mới chưa chạy trên máy. `L2_RAW_DISCOVERY` chưa nối và REPORT_L1 thật không có đếm candidate theo ledger (chỉ `populationTotal=1`) — cần owner chọn hướng (plan mục 10). Next action: nút chạy L2/L3 (offline).

**Cập nhật 2026-10-02 (M4 lần 2, owner duyệt).** `RECONCILE_COMMITTED`: build `4.18-p5e.5`/213 (APK `88854E47…5793`, commit `b3f6e3de`) cài trên `15e84958`; một call `L1_RECONCILE` (26,940 token vào, 616 ra, USD 0.00747405, 6.5 s) commit attempt `7483b211…` với REPORT_L1 `b33baf33…` + receipt `11754968…` nguyên tử trên RAW `7a5e3428…`; DB sau event `06C4C48E…`, force-stop rồi export lại trùng byte. Checklist P5.4, P5D.7 và P5 exit đã tick kèm caveat (ngoại lệ B; chưa qua UI; chưa đánh giá ngữ nghĩa). Chi tiết: `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` mục Event M4 lần 2. Next action: G5 offline (canonical plan mục 10); live L2/L3 chờ owner chốt ba chương và ngân sách.

**Cập nhật 2026-10-02 (M4 lần 1).** Đã thực hiện work request M4 đến bước 8: M0 chỉ-đọc, sao lưu DB, build `4.18-p5e.4`/212 + AndroidTest, cài trên `15e84958`, M0 cuối, một event. Kết quả `RECONCILE_NOT_DISPATCHED` (`launchCount=1`): app dừng trước provider với `P5_TOKEN_BUDGET_EXCEEDED`, `providerCalls=0`; DB không thêm attempt/receipt/lifecycle, chỉ migrate v25 (bảng `editorial_phase_artifacts` rỗng + 1 index). Nguyên nhân: gate L1 so 107,231 byte (RAW+GLOSSARY+DRAFT+PRONOUN+authority) với trần 100,000 token; RAW chỉ thấy 80,317 byte nên lọt. Đã sửa offline ở `e2e1d3c5` (so `ceil(byte/2)` với trần token; engine 247/247, app 289/289, lint PASS) — APK đang cài chưa chứa bản sửa. Chi tiết và hash: `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` mục Event M4. P5.4/P5 exit chưa đạt. Next action: owner quyết định cấp quyền cho lần thử M4 thứ hai (canonical plan mục 10).

**Cập nhật 2026-10-01 (sau quyết định B).** Owner chọn B: RAW event 7 (`7a5e3428…`) là predecessor duy nhất cho M4; verdict formal giữ nguyên. Gói M4 offline đã xong (`4a603695`, `1f8e7334`, `a64ae460`); G3/G4 offline thêm adapter L2 (`63b288fe`) và L3 boundary (`e8b21d26`). Engine 244/244, app 276/276, lint PASS. Next action: owner duyệt phạm vi quyền M4 (canonical plan mục 10).

**Cập nhật 2026-10-01 (lượt tiếp quản, sau event 7).** (1) Xác minh bổ sung event 7 (`docs/P5E_EVENT7_SUPPLEMENTARY_VERIFICATION_20261001.md`, commit `2e4b7c2d`): verifier gốc `dd41e858` chạy lại trên bản sao cho output trùng byte; mọi kiểm tra khác đạt; 2 trường lỗi có giá trị đúng trong bytes APK đã pin; 24 file evidence gốc không đổi. Verdict formal vẫn `RAW_NOT_ACCEPTED`. (2) Artifact đã commit là pha `L1_RAW_DISCOVERY` — RAW predecessor, **chưa phải REPORT_L1 sau RECONCILE**; P5.4 chưa đạt, M4 RECONCILE bắt buộc. (3) Offline: RECONCILE chuyển sang compact wire + `executeReconcile` trên RAW đã commit (commit `7763085e`; engine 220/220, app 258/258); route fresh A4.3 vẫn chặn RECONCILE. (4) G3 L2 offline: `EditorialChangeMapReconstructor`, `EditorialL2Execution`, store v25 `EditorialPhaseArtifactStore` (commit `f7a95482`, `b4bf2f15`; engine 232/232, app 264/264, lint PASS). APK code211 đã cài **không** chứa các thay đổi này; schema v25 sẽ migrate DB pilot khi cài APK mới — build M4 từ `7763085e` (schema v24) hoặc sao lưu DB trước khi cài.
Lần chạy owner thứ sáu (candidate `32C011C5…`, DecisionId `p5e-a43-owner-12374ba7…`, event `raw-live-a43-preauth-6a3f0841…`) lại nhận HTTP 200 và log `P5E_RAW` đã chỉ ra nguyên nhân: parser của app từ chối output với `finding.evidenceRefs_contains_invalid_token` (content chỉ 1,764 B, finish=stop, 20,931 prompt / 665 completion / 128 reasoning token, chi phí báo cáo USD 0.0060306, 9.1 s). Giả thuyết giới hạn byte đã bị bác. Prompt RAW nay nêu đúng cú pháp token và luật cấu trúc (commit `32af5ed4`); body request đổi nên cần chu trình build, preflight, re-pin. Decision, event và reservation của `32C011C5…` đã dùng.

Chu trình sửa prompt đã xong (commit `dd41e858`): production `4.18-p5e.3`/211 APK `E9CF282C…` và AndroidTest `CCAAE0AD…` đã cài và đọc lại khớp pin; DB đã khôi phục zero-state `8D084050…`; preflight trên máy cho body hash mới `59f9de57…` (identity và route không đổi); pin đã cascade tới candidate `FA3F5F1182F48A7B17524F78F4AEFEC7DCC2DCFCF06E4176B0056B2F5E78DA00`; mọi suite offline PASS (cả từ extract `git archive` sạch) và M0 chỉ-đọc cuối khớp pin. Event thứ bảy chưa chạy.

Event thứ bảy (candidate `FA3F5F11…`, DecisionId `p5e-a43-owner-30c83137…`, event `raw-live-a43-preauth-f8a0ee32…`) đã chạy: log `P5E_RAW` ghi `dispatch status=COMMITTED`, `schemaOk=true receiptOk=true`, một primary call, chi phí báo cáo USD 0.00294841; readback cho thấy attempt `COMMITTED` cùng REPORT_L1 và receipt commit atomic, lineage 1/1/1, DB integrity ok. Verifier formal vẫn báo `RAW_NOT_ACCEPTED` vì lỗi collector phía host (`test.targetPackage` và `test.runner` rỗng; chi tiết trong docs/P5E_CONSOLIDATED_FAILURES_20260930.md). DB trên máy đang giữ kết quả: không khôi phục zero-state. Decision, event và reservation của `FA3F5F11…` đã dùng.

- Event A4.3 `raw-live-a43-preauth-33253efa…` (DecisionId `p5e-a43-owner-1599345c…`) đã dùng và không được dùng lại. Kết quả: Before, một lần launch instrumentation, After; `EXTERNAL_CALL_STATE_UNKNOWN` / `P5E_POST_DISPATCH_DURABLE_STATE_INCOMPLETE`; RAW chưa được chấp nhận.
- Nguyên nhân (đã xác nhận bằng source và source ZIP đã pin): AndroidTest APK `058BE851…` hard-code `EXPECTED_DB_SHA256 = 3563f44b…` và yêu cầu cả tham số launch `p5e_expected_db_sha256` lẫn hash DB thật bằng hằng số đó. Re-pin phía host sang `8D084050…` làm tham số khác hằng số nên test thất bại trước khi đọc key, settings, DB hay mạng. Lỗi của tôi: lúc re-pin chỉ tìm trong scripts/docs, không tìm trong `app/src/androidTest` và APK test.
- Bằng chứng cục bộ: hash DB After bằng Before, lineage toàn 0, chạy khoảng 20 giây, stderr instrumentation rỗng. Chính thức vẫn là UNKNOWN; không suy ra `$0` ở phía nhà cung cấp.
- Nguyên nhân lần owner-window lịch sử trước đó vẫn UNRESOLVED.
- Next action: owner chạy một event với candidate `FA3F5F11…` (xem mục Next action duy nhất).

## Đọc theo thứ tự này

1. `EDITORIAL_RECOVERY_V4_18.md` — canonical plan hiện hành và next action duy nhất.
2. `BUILD_STATE.md` — build/runtime facts hiện tại; phần “Current planning handoff” ở đầu có quyền ưu tiên.
3. `WORKSPACE_SNAPSHOT.md` — trạng thái gọn và đúng một next action.
4. `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` — tiêu chí nghiệm thu final L1–L3.
5. `docs/EDITORIAL_V4_18_IMPLEMENTATION_MAP.md` — map code/gap/test hiện hành.
6. `release_checklists/v4.18-editorial-v5-safe-4-1-3.md` — gate/evidence của release đang dùng; không tạo checklist mới.
7. `GIT_WORKFLOW.md`, rồi `DEVELOPMENT_WORKFLOW.md` trước khi sửa/commit/build.

Bản canonical plan trước lần viết lại được giữ nguyên byte tại `docs/EDITORIAL_RECOVERY_V4_18_HISTORY_20260930.md`, SHA-256 `2537C72FB5F619172E46BC3D1ADA2F0F494BA134093AE4A47822DF8A7872216D`. Chỉ đọc khi cần tra một bằng chứng lịch sử cụ thể; không dùng next action trong đó.

## Trạng thái repository và build

- Workspace: `D:\App Translate Books`.
- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Baseline HEAD trước commit docs đồng bộ: `b2f689cb4948bc4e715b6cf85f4396e49afd9525`.
- Remote push: `origin = https://github.com/mnhlg4321/translate-books-android.git`.
- Working tree không sạch. Có thay đổi owner `.idea`, các docs P5E cũ chưa commit và nhiều evidence untracked. Nhóm entry A4.3 (entrypoint 68DF, parent, loader, test, evidence) đã commit tại `b2f689cb`; không còn bytes CC01 trong index. Không reset/clean/stash hàng loạt; không commit toàn bộ working tree; stage đúng nhóm file.
- Last-known installed production: `4.17-p5e.11` / code207. A4.3 pins hiện tại thuộc code207.
- Development export: `4.18-dev.1` / code208, event `build-20260930-185915`, source snapshot `240cdc814a324bca36543a8091af6bf6cdd07831`, APK SHA-256 `DB3FE9056A3477FA18D4F9F44A18E1FCCA0F7195F95DD99C91F1BD60AA6FE465`. Artifact/backup/delivery parity đã PASS; APK chưa cài và không chứng minh P5/P6.
- Current phase: `P5 incomplete / OWNER_WINDOW_PRE_PROMPT_EXIT / EVIDENCE_INCOMPLETE / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.
- Lượt lập plan/handoff không chạy test, build, device hoặc provider và không tạo live authorization mới.

## Kết quả sản phẩm và quy trình v4.18

Đích giao là bản biên tập cuối sau L1–L3, lưu được, mở lại được và xuất TXT UTF-8 đúng nội dung. Người dùng không cần nhận riêng REPORT_L1, VI_L2, CHANGE_MAP_L2 hoặc QA_RECEIPT; app vẫn phải giữ dữ liệu trung gian cần cho predecessor, kiểm chứng và recovery.

Chuỗi bắt buộc:

```text
pack + RAW/DRAFT/GLOSSARY/PRONOUN
 -> immutable snapshot + preflight
 -> L1 RAW discovery -> L1 reconcile -> atomic REPORT_L1/receipt
 -> L2 raw-first/edit -> atomic VI_L2/CHANGE_MAP_L2
 -> L3 blind RAW-first/reconcile/two adversarial passes
 -> actual diff + release gates -> atomic FINAL_QA/QA_RECEIPT
 -> save -> process restart/reopen -> TXT export/readback
```

Nghiệm thu sản phẩm cần ba chương final thành công, chạy tuần tự với ba chain riêng: chương ngắn, chương dày thoại/xưng hô, chương dài gần giới hạn. `CONTENT_BLOCKED` đúng contract là negative evidence, không thay một trong ba final. Chi tiết ở `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md`.

## Gate đã PASS và chưa PASS

PASS/được giữ làm evidence lịch sử:

- Release workflow steps 01–04: đọc state/snapshot, xác nhận baseline và tạo branch một lần.
- P0–P4: Pack Manifest v1/reference pack, runtime contract/typed stops, binding/resume, side-by-side 4.1.3/4.1.4, process-death characterization.
- Các gate local/fake L1 được checklist đánh dấu: P5.0–P5.3 và P5C fake RAW→RECONCILE.
- Host/offline repairs: parent/pre-reservation `26/26`, child regression `27/27`, static launcher `16/16`, parent integration `21/21` với `candidateExecuted=false`, binding tuple `262/262`, regression packet `175/175`, DB host readback `56/56`, toolchain `14/14`, package-version collector `5/5`.
- Code208 development build: app unit `249/249`, lint và archive/delivery parity PASS. Đây chỉ là build evidence.

Chưa PASS/không được suy ra:

- P5E.8 current-candidate/live preservation evidence và P5E.9/A4.3 accepted RAW.
- P5.4 real L1 atomic REPORT_L1/receipt, P5 exit; P5D.7 separate RECONCILE và P5D exit còn unchecked.
- P6 executable L2/L3 coordinator, atomic persistence, final quality, UI save/reopen/export và ba successful final chapters.
- P7 current-source full regression, candidate device QA và release gates.
- Release checklist steps 05–14, PreTag/PreBackup/Complete, tag/merge/release archive.
- Historical original code196 preservation vẫn FAIL và không được viết lại thành PASS.

Offline/component PASS không thay live/product acceptance. Enum/schema/validator tồn tại không chứng minh UI flow đã chạy. Thiếu audit/counter không có nghĩa provider/DB action bằng 0; dùng `UNKNOWN/EVIDENCE_INCOMPLETE`.

## Next action duy nhất

Owner quyết định đóng event 7: B (khuyến nghị) — chấp nhận RAW đã commit làm predecessor chỉ cho M4 RECONCILE chương 001 theo ngoại lệ hẹp, verdict formal giữ nguyên; hoặc C — khôi phục zero-state và chạy RAW mới (~USD 0.003, mất kết quả đã commit). Trong lúc chờ: tiếp G3 offline (adapter OpenRouter cho wire L2, L2_RAW_DISCOVERY đối chiếu candidate counts) theo `EDITORIAL_RECOVERY_V4_18.md` mục 9a. Không provider, không thiết bị, không khôi phục DB.

Bước chẩn đoán offline launcher đã đóng ở 2/2 vòng (≈30/60 phút): không mở vòng thứ ba. Nếu cửa sổ owner vẫn lỗi dù đã có outer log, dùng phương án B ở canonical plan §6: trình môi trường Android thử riêng/dữ liệu thay thế/cùng production path và phần nào còn phải kiểm lại trên target; không tự cài, xóa DB, gọi provider hoặc thay scope.

Sau khi offline gate đạt, chưa được tự chạy live. Trước event tương ứng phải chốt exact chapter/source identity, phase/call caps, token/output/time/USD budget và quyền hiện hành. Consumed event/decision/receipt không được dùng lại; dispatch UNKNOWN không được retry.

## Lệnh kiểm tra và build

Thiết lập JDK/SDK trong PowerShell khi chạy Gradle:

```powershell
$taskJavaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_HOME = $taskJavaHome
$env:Path = "$taskJavaHome\bin;$env:Path"
$env:ANDROID_HOME = 'C:\Users\ADMIN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

Targeted baseline cho entry boundary, chỉ chạy khi reproducer/patch mới cần nó. Các fixture cũ không tự chứng minh interactive owner window:

```powershell
.\scripts\test-p5e-a43-entry-boundary.ps1
.\scripts\test-p5e-a43-pre-reservation-integration.ps1
```

Targeted/full JVM checks khi thay đổi Android/engine liên quan:

```powershell
.\gradlew.bat :editorial-engine:test --no-daemon --console=plain
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:compileDebugAndroidTestJavaWithJavac :app:lintDebug --no-daemon --console=plain
```

Build APK duy nhất được phép:

```powershell
.\scripts\build-and-save.ps1
```

Không chạy `assembleDebug` trực tiếp hoặc Android Studio Build APK(s). Chỉ dùng `build-and-save.ps1 -Install` khi đã có đúng device/signature authorization và runbook; connected test installer bị cấm trên P5E pilot. Next action hiện tại không cần APK build.

Release gates, chỉ chạy ở đúng phase sau khi evidence tương ứng hoàn tất:

```powershell
.\scripts\verify-release-workflow.ps1 -ChecklistPath .\release_checklists\v4.18-editorial-v5-safe-4-1-3.md -Gate PreTag -ExpectedVersion 4.18
.\scripts\verify-release-workflow.ps1 -ChecklistPath .\release_checklists\v4.18-editorial-v5-safe-4-1-3.md -Gate PreBackup -ExpectedVersion 4.18
.\scripts\verify-release-workflow.ps1 -ChecklistPath .\release_checklists\v4.18-editorial-v5-safe-4-1-3.md -Gate Complete -ExpectedVersion 4.18
```

Mọi thay đổi: chạy `git diff --check` trên nhóm file liên quan. Không rerun suite rộng nếu bytes/risk không đổi.

## File quan trọng

- `scripts/p5e-a43-pre-reservation-launcher-entrypoint.ps1` — current host entry candidate; working/staged bytes phải được đối chiếu trước sửa/commit.
- `scripts/p5e-a43-pre-reservation-launcher.ps1` — parent/pre-reservation logic.
- `scripts/test-p5e-a43-entry-boundary.ps1` — hiện dùng hidden/noninteractive process; đây chính là khoảng trống QA.
- `scripts/test-p5e-a43-pre-reservation-integration.ps1` — hiện thay ApprovalReader/KeyReader và ghi `candidateExecuted=false`.
- `docs/P5E_CONSOLIDATED_FAILURES_20260930.md` — báo cáo lỗi/vòng lặp và nơi ghi hai vòng diagnosis.
- `docs/P5E_A43_ENTRY_BOUNDARY_REPAIR_PROVENANCE_20260930.json` và `docs/P5E_A43_PARENT_INTEGRATION_FINAL_PROVENANCE_20260930.json` — evidence offline gần nhất.
- `editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialP5PilotExecution.java` và `EditorialP5PilotRequest.java` — L1 execution/phases.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialP5CExactBindingExecution.java` và `EditorialP5CAttemptStore.java` — app-bound L1 orchestration/store.
- `EditorialSafe4Workflow`, `EditorialSafe4Contract`, `EditorialPhaseContextProjector`, `EditorialReceiptValidator`, `EditorialDiffValidator` — L2/L3 state/contract/validators; chưa phải complete runtime.
- `app/src/main/java/com/ml/tblandroidtxt/EditorialPageFactory.java` — UI hiện vẫn execution locked.
- `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP4BindingInstrumentedTest.java` — synthetic compatible 4.1.4 fixture; tái dùng, không tạo pack platform mới.

Các plan/acceptance/map hiện đã nằm trong repo. Source authority được pin ở `D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE`; đó là nguồn ngoài repo có hash trong ABI, không phải plan cần chép vào Git.

## Hướng đã thử và kết quả thất bại

- Approval literal được nhập sau khi launcher đã trả về prompt PowerShell: input không còn đi vào process; không có approval/receipt.
- Child invocation từng thành `RemoteException`; fixture cho thấy native stderr có thể bị parent bắt sai, nhưng chưa chứng minh native cause của event thật.
- Pre-reservation attempt từng kết thúc `CHILD_NOT_STARTED / STOP_REASON_UNRESOLVED`; catch che nguyên nhân gốc.
- Latest visible owner-window attempt thoát trước prompt và trước audit/reservation/owner-root; nguyên nhân entry/argument/console vẫn chưa được chứng minh.
- Test entry-boundary cũ chạy hidden/noninteractive; integration thay reader bằng seam. PASS của chúng không phủ đường cửa sổ thật.
- Historical live collectors đã dừng trước dispatch vì `P5E_COLLECTOR_ADB_NONZERO`, package version mismatch hoặc binding tuple mismatch. WAL/SHM exit 1 là `ABSENT` hợp lệ; native reason của consistent-read exit 1 không có evidence đầy đủ.
- Route/account đã từng `MISMATCH`/`NOT_PROVEN`, sau đó route-corrected `MATCH`; MATCH không chứng minh RAW/L1/provider/DB acceptance.
- Wrapper/counters/marker roots/command `.txt` từng tạo false-green hoặc đọc sai evidence; các repair offline đã PASS nhưng live terminal result vẫn thiếu.
- Mỗi typed stop từng sinh thêm packet/plan. Canonical plan hiện cấm packet mới khi bytes/evidence không đổi và cấm dùng số lượng tài liệu/test làm phần trăm sản phẩm.
- Code208 build thành công không giải quyết owner-window và không mở P6.

Không bỏ approval/identity/preservation gates để đi nhanh. Không tự tái tạo nguyên nhân lịch sử đã mất; chỉ sửa failure mới tái hiện được. Đo tiến độ bằng durable outputs: RAW accepted -> L1 persisted -> L2 persisted -> FINAL persisted -> reopen/export -> ba final -> device QA.
