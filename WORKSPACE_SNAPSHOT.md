# Workspace Snapshot

- Updated: 2026-10-12 (+07:00), owner accepted chapter 006: PASS C6 reached, 1/3 chapters accepted; C7 proposal priced offline; nothing run.
- Current version: active v4.18; release/checklist unchanged.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`.
- Current commit: `bbed91ad` is the implementation baseline immediately before this documentation snapshot commit (code at `9a72100fd6a8f826de14ac3387398b01d123fac5`); confirm actual `HEAD` when resuming.
- Current build: `4.18-c6.6`/code `260` from `74c712ca2eee94f2ae14165f8a7e06f2c8176348` (APK SHA-256 CE0AD974ED19F045B7898F886FF4C1A1D571D0BB66B2F39DF410928873F8CF6A; test APK `c6-8-test-20261012`), installed only on `emulator-5554`. CP-IMPL-10 is not built or installed.
- Current phase: C6 closed with owner acceptance; C7 (two more chapters for the owner to read) awaiting a decision. 1/3 accepted, P7 not started.
- Completed tasks: C6E whole-chapter run of 006 under CP-IMPL-9 (14/14 calls, USD 0.0318669, ledger `C6E-20261012` pending 0); scored ĐÚNG 52, SÓT 15, SAI 1, CHƯA CHẮC 6; gates met and the owner accepted it. CP-IMPL-10 (`9a72100f`) fixes 7:3 and 14:5 offline; clean archive suites passed (engine 694, app host 472, androidTest compile, scripts/p6 106, scripts/chunk 24). Offline measurement of the 8 corpus chapters (0 USD) priced the C7 proposal: 008 then 011, expected total about USD 0.03, cap USD 0.06 per chapter.
- Pending tasks: owner decision on C7; build `4.18-c6.7` from CP-IMPL-10 before any further live.
- Known bugs/limits: accepted evidence is CP-IMPL-9, CP-IMPL-10 unmeasured live; large model variance (5:2 missed every run); 15 misses on 006 are the model's; pure `tớ`→`mình` variant of 4:5 not blocked; finder makes no point from self-only or call-only rows; device log has no `provider_usage`; `V5ChatEditorialApiProvider` and host fixtures still price luna input at 0.20/M; host input estimate is about 9% below the real count.
- Regression status: all suites above PASS from clean archives; evidence under D:/P5E-private/c6-guard-20261012/ and D:/P5E-private/chunk-runs/C1/live/006-fb219146-9ca9-45c0-8735-5286fcc58946/.
- Spend: 0 in this step; C6E USD 0.0318669, C6D USD 0.00325825, C6C USD 0.00281905, C6B USD 0.02788237; all ledgers pending 0.
- Next action: owner duyệt, sửa hoặc từ chối đề xuất C7 trong khối này: build `4.18-c6.7` từ CP-IMPL-10 (đã đạt archive sạch, chưa chạy live), cài chỉ `emulator-5554`, chạy hai chương 008 rồi 011 (mỗi chương một lần chạy cả chương, luna medium, không retry, mỗi chương một sổ mới `C7A-<ngày>`/`C7B-<ngày>`, trần USD 0.06 mỗi chương), chấm riêng từng chương và dừng sau mỗi chương để owner đọc. Chưa build, cài hay chi trước quyết định đó.
