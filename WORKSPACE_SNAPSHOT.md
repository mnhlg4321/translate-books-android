# Workspace Snapshot

- Updated: 2026-10-09 (+07:00): CP-IMPL-5 (khối ADDRESS CHECK) chuẩn bị offline xong; chỉ chunk 005 của 007 đổi; chưa build/cài/gọi provider.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `12062ce0` is the baseline immediately before this evidence commit (confirm actual HEAD on resume).
- Current build: `4.18-c1.7`/code `254`, event `build-20261009-193025`, source `ff819dde12eb548e83ab0de43d200bf16d9b795d`, APK SHA-256 51BEC1455D9E2827AC8ED10336DC77D1AC703947423297450EBF2B8C65A77D40; AndroidTest archive `c1-chunk-20261009g`; mirrored in `artifacts/` and `backup/`; installed only on `emulator-5554`.
- Current phase: C1.6 offline measurement reviewed, small scorer fixes tested; CP-IMPL-4 not built/installed, model adherence NOT_MEASURED; 0/3 accepted, P7 not started.
- Completed tasks: C1.1–C1.4 alignment/source/UI/runner and C1.5 live 007 remain as previously recorded. Added offline scorer and synthetic controls under `scripts/chunk/`; 9/9 Python tests pass. Private RAW adjudication of two repeated address occurrences in 007/chunk 005 found 2 wrong fixes; one clean direct vocative stayed unchanged. Synthetic completeness scorer distinguishes restored/remaining/new omissions and boundary leakage. FINAL similarity is not an input to the address score.
- Pending tasks: owner decision on the second one-call live run of 007 chunk 005 with CP-IMPL-5; if approved, build 4.18-c1.8, install on emulator-5554, device fake check, one live call. C2 holdout 011, C3 owner reading, P7 follow.
- Known bugs/limits: CP-IMPL-4 model adherence NOT_MEASURED; completeness of the real 007 response NOT_MEASURED; other 27 changed lines remain UNADJUDICATED. The real annotation is a selected historical CP-IMPL-3 case, not a full-chapter benchmark.
- Regression status: working tree (not clean archive): engine 651 (1 skip), app 465 (3 opt-in skip), scripts/chunk 24, scripts/p6 93, androidTest compile PASS, git diff --check PASS. Real-007 prompts: chunks 1-4, 6, 7 byte-identical to CP-IMPL-4, chunk 5 differs by one section. No new build/device/provider.
- Spend: old C1 ledger remains 17 calls / USD 0.04044430 settled, pending 0, remaining USD 0.00955570; this offline work added 0 provider calls and USD 0.
- Next action: owner duyệt hoặc từ chối lần live thứ hai trên cùng chunk 005 với CP-IMPL-5 (đề xuất ở cuối §6.2: đúng 1 call, không retry, sổ mới `C1-SINGLE5B-20261009` trần USD 0.01); nếu duyệt thì build `4.18-c1.8` từ commit gói này, cài chỉ `emulator-5554`, chạy thử fake với selector rồi gọi 1 lần. Chưa có quyền chi nào.
