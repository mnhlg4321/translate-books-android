# v4.25 PC Library lists (lib-a) — evidence, 2026-11-04

- Code commit: `16f2a1ca` (feature/v4.25). Clean `git archive` of it: `D:\P5E-builds\verify-v4.25-lib-a-16f2a1ca`.
- Screenshots: `after/` (78 images) from the clean-archive desktop install, run with `QaSeed` into throw-away folders under `D:\P5E-builds\qa-lib-a\` (no owner data touched). Run log: `D:\P5E-builds\qa-lib-a\launch.log`.
- Tests: `clean-archive-tests.log` (GRADLE_EXIT=0). Totals from the XML results: desktop 80 / 0 failed / 0 skipped; translation-core 67 / 0 failed / 1 skipped; translation-core-jdbc 3 / 0 failed; app 766 / 0 failed / 5 skipped.

## Images inspected
- `014-sang-thu-vien-raw-chon-2.png` (light): tick boxes, "Chọn tất cả", "2 đã chọn", "Hủy", "3 RAW" line, UTF-8 per row, bar "Xóa (2)", "Xuất (2)…", "Tạo tập (2)". Correct.
- `034-toi-thu-vien-raw-chon-2.png` (dark): same layout, readable on the dark palette.
- `015-sang-thu-vien-glossary-chon-2.png` (light): two ticked, bar "Xóa (2)", "Xuất (2)…", "Dùng" greyed out (n ≠ 1). Correct.
- `016-sang-thu-vien-pronoun-chon-2.png` (light): same behaviour for Pronoun.
- `017-sang-thu-vien-tap-chon-2.png` (light): "1 đã chọn" and "Xóa (1)". Only one Tập exists in the QA seed, so two ticks could not be shown.
- `018-hop-thoai-xoa-glossary-lan-1.png`, `019-hop-thoai-xoa-pronoun-lan-1.png`: first delete dialog ("Xóa 2 Glossary?" / "Xóa 2 Pronoun?") with the "Đồng ý / Hủy" buttons. Cancelled by the QA path, nothing removed.

Not inspected in this round: the dark Glossary, Pronoun and Tập selection images (035–037), the Tập delete dialog (020), and the second-confirmation dialog (only the RAW set in `040`/`041` exists from the earlier round). No "before" set was captured for this round, so there is no before/after pair; the old UI is described by the code history (commit before `16f2a1ca`).
