# Workspace Snapshot

- Updated: 2026-10-09 (+07:00): owner duyệt D-Q2c (canary V5-luna 007 lại sau khi sửa danh tính nguồn; E-luna-b dev 004–008 độc lập), ghi mục 5 của request Q2.6.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: ce37cfba — baseline ngay trước commit ghi quyết định này (confirm actual HEAD on resume).
- Current build: `4.18-q2.2`/code `246`, source `abca34cd`, installed only on `emulator-5554`; production and AndroidTest wrapper archives are mirrored in `artifacts/` and `backup/`.
- Current phase: EDITORIAL_API_V1 — Q2.6 (danh tính nguồn V5 + canary lại, E-luna-b dev); U1 tạm hoãn; 0/3 chương đạt; P7 chưa bắt đầu.
- Completed tasks: Q2.5.1–2.5.3 implementation/build/test; Q2.5.4 V5-luna canary preflight and one live turn, stopped at `V5_STOP_INPUT_ARTIFACT_MISSING`.
- Pending tasks: Owner: D-Q2c; Codex: Q2.6.1 role + ID/SERIES + HOST SOURCE MANIFEST + tên gốc + bảng điều kiện pack; Q2.6.2 build; Q2.6.3 canary V5-luna 007, E-luna-b dev 004–008, rồi dev/holdout theo cổng tối thiểu 4.1.3. Known bug: V5 input dùng tên file chung, thiếu Source Manifest/ID/SERIES.
- Known bugs/limits: V5-luna turn 1 stopped with `V5_STOP_INPUT_ARTIFACT_MISSING` despite source-pack preflight PASS and four source blocks in the request; semantic quality remains NOT_MEASURED. No dev/holdout was run. The unrelated broad instrumentation attempt hit historical P4 `expected 27, actual 28`; focused API tests passed.
- Regression status: engine 572/572 PASS; app benchmark/debug/release each 439/439 PASS; `scripts/p6` 80/80 PASS; wrapper unit/lint/build PASS; focused emulator API tests 14/14 PASS; source preflight 8/8 PASS; account check MATCH with 0 calls. Canary: 1 call, USD 0.0080449, 0 UNKNOWN, ledger USD 0.8455608 / stored cap USD 6.00, 11 settled calls, 0 pending.
- Next action: Codex thực hiện Q2.6.1–Q2.6.3 trong `docs/EDITORIAL_API_V1_Q25_REVIEW_AND_Q26_REQUEST_20261009.md` (D-Q2c đã duyệt, mục 5).

