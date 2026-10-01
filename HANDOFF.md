# Handoff — Editorial v4.18

Ngày bàn giao: 2026-10-01 (+07:00).

## Trạng thái mới nhất — ưu tiên đọc trước

Baseline HEAD `b2f689cb4948bc4e715b6cf85f4396e49afd9525`. Vòng entry-boundary 2/2 đã dùng (≈30/60 phút); bước offline đã đóng, không mở vòng launcher thứ ba. Chi tiết ở mục “Vòng chẩn đoán entry owner-window 2/2” trong `docs/P5E_CONSOLIDATED_FAILURES_20260930.md`.

- Một commit đã chứa entry 68DF8061, parent, loader và mọi test/evidence cần thiết; chạy từ extract `git archive HEAD` trong đường dẫn có khoảng trắng: console QA 6/6, integration 21/21.
- Console QA (hidden console thật, input synthetic) phủ: prompt + literal sai, outer failure, approval→key→synthetic child thành công qua integration fixture, bytes CC01 (exit 0 im lặng), audit hash có sẵn và dạng `-AuditPath` riêng.
- Nguyên nhân lần owner-window gần nhất vẫn UNRESOLVED: không có stderr. `P5E_A43_AUDIT_ALREADY_EXISTS_STOP` đã tái hiện nhưng bản ghi lần đó ghi audit path theo DecisionId nên không chứng minh là nguyên nhân. CC01 silent exit là lỗi của bytes cũ, tách biệt. M0 (2026-10-01): APK/version khớp pin, nội dung DB khớp tuple tươi với lineage bằng 0, nhưng hash file DB `8D084050…` lệch pin `3563F44B…`, nên Before sẽ dừng cứng trước khi gọi provider.
- Giới hạn: không phải cửa sổ owner nhìn thấy; case thành công dùng integration fixture, không qua `-Execute` của entry live. Không có bằng chứng RAW/L1/provider; P5 vẫn chưa PASS, P6 chưa sẵn sàng.
- Next action: owner quyết định cách xử lý pin hash file DB (xem mục 80). Khuyến nghị re-pin sang `8D084050…`.

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

Owner quyết định cách xử lý pin hash file DB. M0 đã chạy xong ngày 2026-10-01: APK/version khớp pin; nội dung DB khớp tuple tươi, lineage bằng 0, integrity ok; nhưng hash file DB là `8D084050…`, khác pin `3563F44B…` và khác `2CC23078…` (09-26), nên Before sẽ dừng `P5E_COLLECTOR_PRELIVE_DATABASE_HASH_MISMATCH` trước khi gọi provider. Khuyến nghị: re-pin sang `8D084050…` (cascade offline helper → manifest → command → entry, test, candidate hash và guide mới), rồi chạy lại M0 chỉ-đọc ngay trước khi duyệt một event. Nếu hash file lệch lần nữa, thay cổng hash file bằng readback nội dung (binding tuple, lineage bằng 0, integrity) là một quyết định riêng của owner.

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
