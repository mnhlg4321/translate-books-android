# Workspace Snapshot

- Updated: 2026-10-08 (+07:00): owner duyệt D-Q2b (trần Q2 USD 10.00) với điều kiện chỉ chạy GPT-5.6 luna; cổng = chất lượng tối thiểu 4.1.3 (mục 7 request Q2.5); luna chạy reasoning medium.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; existing release checklist unchanged.
- Current commit: 398e907c — baseline ngay trước commit ghi quyết định này (confirm actual HEAD on resume).
- Current build: `4.18-q2.1`/code `245`, source `45976adb`, installed only on `emulator-5554`; wrapper archives are mirrored in both artifact roots.
- Current phase: EDITORIAL_API_V1 — Q2.5 (V5 đính kèm file gốc + canary, E-strong giảm sửa thừa); U1 tạm hoãn; 0/3 chương đạt; P7 chưa bắt đầu.
- Completed tasks: Q2.1 scorer/normalizer/Quality Core, Q2.2 V5_CHAT provider and offline regressions, Q2.3 wrapper production/test archives/emulator gate/manifest, Q2.4 dev 004–008 on both frozen arms with offline scoring.
- Pending tasks: Codex: Q2.5.1 V5 input nguyên văn + tiền kiểm pack; Q2.5.2 Quality Core + guard nhãn; Q2.5.3 build; Q2.5.4 canary V5-luna 007 → dev V5-luna + E-luna-b 004–008 → holdout 011/014/017 với arm đạt nhiều chương tối thiểu nhất; owner đọc holdout.
- Known bugs: V5ChatEditorialApiProvider.turnTexts ghép prompt đường E (glossary 4 cột đã lọc, APP DETECTIONS) thay vì file gốc và lặp Project Instruction; E-strong hạ chữ hoa nhãn trạng thái, đổi xưng hô, sinh thêm chữ Nhật ở 2 dòng. Chunk-pair đóng băng offline.
- Regression status: engine 566/566, app unit tasks PASS, AndroidTest compile and targeted emulator Editorial API tests PASS, Python verifier 14/14 PASS; Q2 ledger 10 settled logical reservations (20 entries / physical calls), USD 0.8375159, pending 0, UNKNOWN 0.
- Next action: Codex thực hiện Q2.5 trong `docs/EDITORIAL_API_V1_Q2_REVIEW_AND_Q25_REQUEST_20261008.md` theo mục 7 (luna medium, D-Q2b).

