# Workspace Snapshot

- Updated: 2026-10-09 (+07:00): coordinator đo lệch dòng RAW/DRAFT trên 28 chương và prototype căn dòng (scripts/chunk/align_lines_reference.py): 42/42 điểm cắt đúng trên 8 chương có đáp án; ghép 1–1 thuần sai ở 016/027 (lệch bù trừ). Cập nhật mục 3.1 và C1.1 của `docs/EDITORIAL_CHUNK_PLAN.md`.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: 1f360c19 — baseline ngay trước commit này (confirm actual HEAD on resume).
- Current build: `4.18-q2.3`/code `247`, event `build-20261009-034756`, source `7cc4108b`, APK SHA-256 D21C64E5A08879E88B850C60E45515812A8902ABB30DFB1449F292434D20B4DE; AndroidTest archive `q26-editorial-api-20261009` (SHA-256 58ED4E1843F27E0C768053E22E432BCA8702056325CA9F451E741ABA2BC26D89); mirrored in `artifacts/` and `backup/`; installed only on `emulator-5554`.
- Current phase: Biên tập theo chunk (C1): nối nguồn hai file + runner CHUNK + UI bỏ Kỹ/V5; live 1 chương 007 sau D-C1; U1 tạm hoãn; 0/3 chương đạt; P7 chưa bắt đầu.
- Completed tasks: Q2.6.1 (role-based V5 preflight, ID/SERIES, HOST SOURCE MANIFEST, original names, pack-condition table), Q2.6.2 (build 4.18-q2.3, focused emulator tests), Q2.6.3 (canary, dev matrix, gate, holdout, exports).
- Pending tasks: Owner: D-C1 (live 1 chương 007 chunk-luna, trần USD 0.05); Codex: C1.1 nối nguồn hai file, C1.2 UI bỏ Kỹ/V5, C1.3 runner CHUNK, C1.4 offline + build + emulator, C1.5 live 007 khi có D-C1.
- Known bugs/limits: no arm reaches the 4.1.3 automatic minimum; V5-luna 004 stopped in turn 3 (CONTENT_UNACCOUNTED_CHANGE, DRAFT kept); one run per arm and chapter, no repeats; semantic quality NOT_MEASURED. Earlier unrelated limits unchanged (historical P4 instrumented failures).
- Regression status: engine 582/582 PASS; app benchmark/debug/release each 443/443 PASS; scripts/p6 90/90 PASS; AndroidTest compile PASS; focused emulator API tests PASS (6/6, 3/3, 2/2, process death 3/3). Q2.6 provider calls 29, USD 0.33147915; ledger Q2-20261008 USD 1.17703995 settled, 0 pending/UNKNOWN.
- Review completed 2026-10-09: ledger and archive/output hashes verified. Added scorer diagnostics only (working tree), 8 scorer + 7 gate tests PASS from clean HEAD archive plus overlay; all 13 Q2.6 replays preserve every existing metric. 017 report corrected: two replacements and one insertion, not a no-change answer. No build/device/provider/commit/push. Evidence: D:/P5E-private/q26-review-clean-d94d0350/scorer-diagnostics-review.json.
- Next action: Codex thực hiện C1 (mục 5) trong `docs/EDITORIAL_CHUNK_PLAN.md`.
