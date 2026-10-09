# Workspace Snapshot

- Updated: 2026-10-10 (+07:00): coordinator review C6.5 — dừng do trần đầu ra của lời gọi sửa theo điểm quá thấp (sàn 128, lệch mục 5.1); đã sửa sàn 4096 + test (chưa build). Sổ C6-20261010 pending là fail-closed có chủ đích; cần đóng sổ có kiểm. Live lại 006 cần D-C6b (trần USD 0.06).
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: 792030a5 — baseline ngay trước commit sửa này (confirm actual HEAD on resume).
- Current build: `4.18-c6.3`/code `257` from source `1cc8e346540201541f639ffe0be0c70a342f5512`, APK SHA-256 245763A89A4AD7E70491C9A4D60D0020ECBF23A66323E4F75A8955076FEC477F; test APK `c6-pair-final-20261010`; installed only on `emulator-5554`.
- Current phase: C6 (CP-IMPL-6 targeted fix points). C6.1–C6.4 offline PASS; C6.5 live 006 stopped at the first call (output reserve too small for luna medium reasoning). Owner decision required.
- Completed tasks: C6.1 engine (`44dcfffe`), C6.2 app (`0d581055`), C6.3 measurement (`11cd0140`), C6.4 host fake (`9bdf2fed`), and AndroidTest fixture corrections (`9155f1d6`, `1cc8e346`) are pushed. Fake 006 no-op round-trip and isolated target-line change pass; the unflagged chunk makes no call. Engine/app host regressions and both Python suites pass. Emulator pair/API classes and process-death sequences pass; detailed evidence is in plan §7.
- Pending tasks: owner decision on C6 (a: output reserve 2048 + cap USD 0.05 with build 4.18-c6.4 and a new live approval; b: reasoning low; c: stop C6). Reconcile ledger C6-20261010 (pending 0.002066 vs provider cost 0.0024366) by hand. C7 owner reading and P7 wait for C6.
- Known bugs/limits: ADDRESS coverage is 57.3%, below required 69%. Conservative fallback estimate for 006 is USD 0.555370, above C6 cap USD 0.03; this is not a confirmed luna quote. Model quality NOT_MEASURED.
- Regression status: no code change since 4db92c35; host measurement 006 (estimate USD 0.023975, verdict OK) and fake replay (byte-exact, 14/14 calls) re-run PASS; device plan run matches host plan; live 1 call, provider cost USD 0.0024366, ledger pending 0.002066.
- Spend: C6 provider calls 0 / USD 0; all existing ledgers unchanged.
- Next action: Owner quyết định D-C6b; Codex làm C6.6 (đóng sổ có kiểm, build 4.18-c6.4 với sàn 4096, kiểm offline/thiết bị) rồi live lại 006 nếu D-C6b được duyệt.
