# Workspace Snapshot

- Updated: 2026-10-09 (+07:00): phép thử live single-chunk 005 (CP-IMPL-4, 1 call, USD 0.00201225) xong: bản trả về giống DRAFT, bỏ sót 2/2 chỗ gọi `cậu`, completeness 19/19; giả thuyết chưa xác nhận. Dừng chờ owner.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `ff819dde` is the baseline immediately before this evidence commit (confirm actual HEAD on resume).
- Current build: `4.18-c1.7`/code `254`, event `build-20261009-193025`, source `ff819dde12eb548e83ab0de43d200bf16d9b795d`, APK SHA-256 51BEC1455D9E2827AC8ED10336DC77D1AC703947423297450EBF2B8C65A77D40; AndroidTest archive `c1-chunk-20261009g`; mirrored in `artifacts/` and `backup/`; installed only on `emulator-5554`.
- Current phase: C1.6 offline measurement reviewed, small scorer fixes tested; CP-IMPL-4 not built/installed, model adherence NOT_MEASURED; 0/3 accepted, P7 not started.
- Completed tasks: C1.1–C1.4 alignment/source/UI/runner and C1.5 live 007 remain as previously recorded. Added offline scorer and synthetic controls under `scripts/chunk/`; 9/9 Python tests pass. Private RAW adjudication of two repeated address occurrences in 007/chunk 005 found 2 wrong fixes; one clean direct vocative stayed unchanged. Synthetic completeness scorer distinguishes restored/remaining/new omissions and boundary leakage. FINAL similarity is not an input to the address score.
- Pending tasks: owner decision after the chunk 005 result (offline deterministic address-candidate block, or another direction). C2 holdout 011, C3 owner reading, P7 follow.
- Known bugs/limits: CP-IMPL-4 model adherence NOT_MEASURED; completeness of the real 007 response NOT_MEASURED; other 27 changed lines remain UNADJUDICATED. The real annotation is a selected historical CP-IMPL-3 case, not a full-chapter benchmark.
- Regression status: host working tree before build: engine 644 (1 skip), app 464 (2 opt-in skip), scripts/chunk 24, scripts/p6 93; emulator focused tests 8 groups PASS on 4.18-c1.7; device fake single-chunk run and host prompt byte-identical; live 1 call, ledger C1-SINGLE5-20261009 verified (1 call, USD 0.00201225, 0 pending).
- Spend: old C1 ledger remains 17 calls / USD 0.04044430 settled, pending 0, remaining USD 0.00955570; this offline work added 0 provider calls and USD 0.
- Next action: owner quyết định hướng sau kết quả chunk 005: cho phép thiết kế offline (không live) một khối ứng viên xưng hô tất định đưa vào request — liệt kê, trong lượt thoại thuộc phạm vi hàng pronoun, từng chỗ RAW/DRAFT mà hàng đó chi phối — rồi kiểm lại bằng cùng selector và bộ đối chứng; hoặc đổi hướng. Không chạy live thêm trước quyết định; sổ `C1-SINGLE5-20261009` còn USD 0.00798775 nhưng không phải quyền chi.
