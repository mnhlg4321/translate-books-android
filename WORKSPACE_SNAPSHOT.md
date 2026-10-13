# Workspace Snapshot

- Updated: 2026-10-13 (+07:00): X3 xong offline — áp D-A vào bộ kiểm (7 điểm sang bản sạch; 29 lỗi đã biết / 129 bản sạch; replay 158/158 không đổi), test chỉ ra prompt CP-IMPL-14/15 trái D-A, đề xuất chỉ sửa prompt, gói đo B 008 soạn sẵn chờ quyền chi.
- Current version: v4.19.0 in development; shipped baseline v4.18.0 (tag `v4.18.0` on `9d02976e`), not changed.
- Current branch: `feature/v4.19` in worktree `D:\P5E-builds\wt-v4.19` (cut from `main` `998da341`); pushed to `origin/feature/v4.19`.
- Current commit: `132479dd` là baseline ngay trước commit X3 này (không tự tham chiếu); xác nhận HEAD khi resume.
- Current build: none for v4.19. Last production build 4.18.0 / code 266 from `82626ac8`, APK SHA-256 `5865F23A1EE15602C5C38B967E34AED22E63728078C7A792F92E8BECBD6AE91B`; events `build-20261012-153252`, `build-20261012-161228`, `tag-v4.18.0` in `artifacts/releases/v4.18.0/` and `backup/v4.18.0/`.
- Current phase: X3 xong (relabel D-A, test mâu thuẫn, đề xuất); chờ owner chọn X4 sửa prompt và/hoặc quyền chi cho gói đo B 008. Không mã sản xuất nào đổi.
- Completed tasks: X0 docs; X1 bộ kiểm (nay 29 lỗi đã biết + 129 bản sạch sau D-A); X2 chẩn đoán; review 2026-10-13; D-A; X3 relabel + replay + test mâu thuẫn + đề xuất + gói B soạn sẵn.
- Pending tasks: quyết định owner cho X4 (sửa prompt D-A) và gói đo B 008; X5/X6 chưa bắt đầu.
- Known bugs/limits: 006 keeps 13 and 008 keeps 7 pronoun misses measured under CP-IMPL-9/11 (13 of the 14 FINAL-unchanged points have `cậu` in the DRAFT); 011 keeps 2:2, 12:2, 10:6; finder covers 18/37 address lines in 014 (11 lost to row conflicts) and 1/40 in 004 (narrator `mình`); the `mình` lexicon gap and row conflicts are one family (no line-to-row attribution); S2 for 004, 005, 007, 014, 017 NOT_MEASURED. Owner's workspace `D:\App Translate Books` keeps 163 uncommitted entries, untouched.
- Regression status: X3 trên cây làm việc: Python `scripts/chunk` 53/53; engine full (xem commit); replay bộ kiểm 158/158 không đổi, exit 0. `app/src/main`, `editorial-engine/src/main` không đổi so với `998da341`.
- Semantic progress: unchanged 3/3 owner-accepted chapters (006, 008, 011) with recorded limits; no new model measurement.
- Spend: 0 provider calls / USD 0; no spend authority exists for v4.19.
- Next action: Owner chọn: duyệt sửa prompt D-A (X4 offline) và/hoặc cấp quyền chi gói đo B 008 (mục 9.4 báo cáo chẩn đoán), hoặc quyết mở rộng D-A cho self `tôi` trước; không provider trước khi có quyền chi.
