# Workspace Snapshot

- Updated: 2026-10-09 (+07:00): owner duyệt D-C6 (live chương 006, luna medium, trần USD 0.03 sau C6.1–C6.4) và xác nhận FINAL chỉ là tham chiếu; cổng C6 đổi sang phân xử theo RAW/luật + owner đọc (`docs/EDITORIAL_CHUNK_PLAN.md` mục 5.4).
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: 2e5762d3 — baseline ngay trước commit này (confirm actual HEAD on resume).
- Current build: `4.18-c1.7`/code `254`, event `build-20261009-193025`, source `ff819dde12eb548e83ab0de43d200bf16d9b795d`, APK SHA-256 51BEC1455D9E2827AC8ED10336DC77D1AC703947423297450EBF2B8C65A77D40; AndroidTest archive `c1-chunk-20261009g`; mirrored in `artifacts/` and `backup/`; installed only on `emulator-5554`.
- Current phase: Biên tập theo chunk — C6 (sửa theo điểm gắn cờ) offline; live 006 chờ D-C6; U1 hoãn; 0/3 chương đạt; P7 chưa bắt đầu.
- Completed tasks: C1.1–C1.4 alignment/source/UI/runner and C1.5 live 007 remain as previously recorded. Added offline scorer and synthetic controls under `scripts/chunk/`; 9/9 Python tests pass. Private RAW adjudication of two repeated address occurrences in 007/chunk 005 found 2 wrong fixes; one clean direct vocative stayed unchanged. Synthetic completeness scorer distinguishes restored/remaining/new omissions and boundary leakage. FINAL similarity is not an input to the address score.
- Pending tasks: Codex: C6.1 engine FixPoint/Finder/Prompt/Parser/Applier; C6.2 app (chunk 0 điểm không gọi API, màn xác nhận/kết quả theo điểm); C6.3 đo độ phủ 8 chương; C6.4 offline + build; C6.5 live 006 sau D-C6. Sau đó C7 owner đọc 3 chương, P7.
- Known bugs/limits: CP-IMPL-4 model adherence NOT_MEASURED; completeness of the real 007 response NOT_MEASURED; other 27 changed lines remain UNADJUDICATED. The real annotation is a selected historical CP-IMPL-3 case, not a full-chapter benchmark.
- Regression status: working tree (not clean archive): engine 651 (1 skip), app 465 (3 opt-in skip), scripts/chunk 24, scripts/p6 93, androidTest compile PASS, git diff --check PASS. Real-007 prompts: chunks 1-4, 6, 7 byte-identical to CP-IMPL-4, chunk 5 differs by one section. No new build/device/provider.
- Spend: old C1 ledger remains 17 calls / USD 0.04044430 settled, pending 0, remaining USD 0.00955570; this offline work added 0 provider calls and USD 0.
- Next action: Codex thực hiện C6.1–C6.5 theo mục 5.4 của `docs/EDITORIAL_CHUNK_PLAN.md` (D-C6 đã duyệt).
