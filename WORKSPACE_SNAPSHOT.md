# Workspace Snapshot

- Updated: 2026-10-09 (+07:00): chuẩn bị offline phép thử single-chunk 005 xong (selector, khóa một call, đối chứng completeness); chưa build/cài/gọi provider.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `0e0ebd8c` is the baseline immediately before this evidence commit (confirm actual HEAD on resume).
- Current build: unchanged `4.18-c1.6`/253, source `57978e836a4cf13f2308fd8e6541a3732a0b66fa`, event `build-20261009-183051`; this work produced no APK and made no emulator/device change.
- Current phase: C1.6 offline measurement reviewed, small scorer fixes tested; CP-IMPL-4 not built/installed, model adherence NOT_MEASURED; 0/3 accepted, P7 not started.
- Completed tasks: C1.1–C1.4 alignment/source/UI/runner and C1.5 live 007 remain as previously recorded. Added offline scorer and synthetic controls under `scripts/chunk/`; 9/9 Python tests pass. Private RAW adjudication of two repeated address occurrences in 007/chunk 005 found 2 wrong fixes; one clean direct vocative stayed unchanged. Synthetic completeness scorer distinguishes restored/remaining/new omissions and boundary leakage. FINAL similarity is not an input to the address score.
- Pending tasks: owner decision on the one-call live proposal for 007 chunk 005 (plan 6.2); if approved, build 4.18-c1.7, install on emulator-5554, device fake-provider check with the selector, then one live call. C2 holdout 011, C3 owner reading, P7 follow.
- Known bugs/limits: CP-IMPL-4 model adherence NOT_MEASURED; completeness of the real 007 response NOT_MEASURED; other 27 changed lines remain UNADJUDICATED. The real annotation is a selected historical CP-IMPL-3 case, not a full-chapter benchmark.
- Regression status: working tree (not clean archive): engine 644 (1 skip), app 464 (2 opt-in skip), scripts/chunk 24, scripts/p6 93, androidTest compile PASS, git diff --check PASS. Single-chunk host tests and real-007 offline test PASS (plan equals device plan byte for byte). No new build/device/provider.
- Spend: old C1 ledger remains 17 calls / USD 0.04044430 settled, pending 0, remaining USD 0.00955570; this offline work added 0 provider calls and USD 0.
- Next action: owner duyệt hoặc từ chối đề xuất live ở cuối §6.2 (chương 007, chunk 005, P061–P079, đúng 1 call, sổ mới `C1-SINGLE5-20261009` trần USD 0.01); nếu duyệt thì mới build `4.18-c1.7` từ commit gói này, cài chỉ `emulator-5554`, chạy thử single-chunk bằng fake provider trên thiết bị, rồi mới gọi 1 lần. Chưa có quyền chi nào.
