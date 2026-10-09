# Workspace Snapshot

- Updated: 2026-10-10 (+07:00): coordinator review C6.3/C6.4 — ước tính 006 với giá luna USD 0.023975 (dưới trần 0.03; USD 0.555 là giá dự phòng ghi cứng trong test đo, đã sửa); độ phủ xưng hô 57.3% đổi mẫu số so với CP-IMPL-5 và chỉ là tham chiếu FINAL. C6.5 đủ điều kiện theo D-C6.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: 60d5564b — baseline ngay trước commit review này (confirm actual HEAD on resume).
- Current build: `4.18-c6.3` / code `257`, event `build-20261010-054638`, source `1cc8e346540201541f639ffe0be0c70a342f5512`, APK SHA-256 `245763A89A4AD7E70491C9A4D60D0020ECBF23A66323E4F75A8955076FEC477F`, source ZIP SHA-256 `D59CCE9E1572A058151EFA34000E7BCE862F7A3F7418D2D476B070C2438B37D4`; immutable payload mirrored in `artifacts/` and `backup/`, installed only on `emulator-5554` after guarded preflight.
- Current phase: C6.4 offline/build/device complete. C6.3 ADDRESS coverage is below gate, so C6.5 is not eligible. U1 remains deferred; no pilot or V5 work.
- Completed tasks: C6.1 engine (`44dcfffe`), C6.2 app (`0d581055`), C6.3 measurement (`11cd0140`), C6.4 host fake (`9bdf2fed`), and AndroidTest fixture corrections (`9155f1d6`, `1cc8e346`) are pushed. Fake 006 no-op round-trip and isolated target-line change pass; the unflagged chunk makes no call. Engine/app host regressions and both Python suites pass. Emulator pair/API classes and process-death sequences pass; detailed evidence is in plan §7.
- Pending tasks: owner review whether to continue offline work on address coverage and obtain a verified under-cap luna estimate before any later live request.
- Known bugs/limits: ADDRESS coverage is 57.3%, below required 69%. Conservative fallback estimate for 006 is USD 0.555370, above C6 cap USD 0.03; this is not a confirmed luna quote. Model quality NOT_MEASURED.
- Regression status: clean-source engine 665 tests (0 failures/errors, 1 opt-in skip), app 468 (0 failures/errors, 3 opt-in skips), `scripts/p6` 93/93 and `scripts/chunk` 24/24 PASS. Emulator: Pair Store 6/6, Pair UI 6/6, API Store 6/6, API flow 3/3, API UI 5/5; pair and API process-death sequences each passed seed/force-stop/verify/cleanup.
- Spend: C6 provider calls 0 / USD 0; all existing ledgers unchanged.
- Next action: Codex thực hiện C6.5: live chương 006 theo D-C6 (luna medium, sổ mới trần USD 0.03), chấm ĐÚNG/SÓT/SAI/CHƯA CHẮC theo RAW, xuất trang đọc.
