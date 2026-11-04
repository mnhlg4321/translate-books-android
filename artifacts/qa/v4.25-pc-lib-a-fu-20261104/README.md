# v4.25 PC Library lib-a follow-up — evidence, 2026-11-04

- Code commit under test: `ff50e86a` (fix(pc): Tập tick boxes, Xóa/Vẫn xóa dialogs with Cancel default, one Dùng button).
- Clean `git archive` of it: `D:\P5E-builds\verify-v4.25-fu-ff50e86a`. Offline tests from that archive: `clean-archive-tests.log` (GRADLE_EXIT=0).
- Screenshots: `after/` (80 images) from the clean-archive install (`desktop/build/install/TranslateBooks`), QaSeed into throw-away `D:\P5E-builds\qa-fu\` (no owner data).
- Totals from the XML results: desktop 83 / 0 failed / 0 skipped; translation-core 67 / 0 failed / 1 skipped; translation-core-jdbc 3 / 0 failed; app 766 / 0 failed / 5 skipped.

## Images inspected (light and dark where both exist)

- `017-sang-thu-vien-tap-chon-1-trong-2.png` (light) and `038-toi-thu-vien-tap-chon-1-trong-2.png` (dark): the Tập list shows a tick box on each row now. "1 đã chọn" with the first set ticked (QA_tap_mot_chuong); the second set is not ticked. The status colours of the rows are kept (green). Bar "Xóa (1)" is red (`.danger`) on both palettes and readable.
- `020-hop-thoai-xoa-tap-lan-1.png` (first dialog, Tập): title "Xóa 2 tập?", buttons "Xóa" and "Hủy"; "Hủy" is the highlighted default (blue). Checkbox "Xóa cả 2 RAW chỉ thuộc các tập này".
- `021-hop-thoai-xoa-lan-2-van-xoa.png` (second confirmation, opened directly from the QA hook): buttons "Vẫn xóa" and "Hủy"; "Hủy" is the default.
- `018-hop-thoai-xoa-glossary-lan-1.png` (first dialog, Glossary): buttons "Xóa" and "Hủy"; "Hủy" default.
- `036-toi-thu-vien-glossary-chon-2.png` (dark): two ticked, red "Xóa (2)", "Xuất (2)…" in the bar; "Dùng cho bản dịch" disabled (two ticked).
- `055-sang-thanh-hoan-tac-sau-xoa-glossary.png` (light): the undo bar after a QA delete of the two Glossaries: "Hoàn tác" button and "còn 10 giây để Hoàn tác". KNOWN DEFECT: the line of text above the bar repeats the message ("Đã xóa 2 Glossary. Đã xóa 2 Glossary · còn 10 giây để Hoàn tác"), because the page writes the same note twice. Not fixed in this round; cosmetic.

Earlier selection shots (014–016, 034–037, dark and light) show the tick boxes and the bars for RAW, Glossary, Pronoun.

## Not captured or not inspected
- Dark images of the Glossary, Pronoun and Tập first dialogs (only light was captured for dialogs, as in the previous round).
- The undo bar on a RAW or Tập delete (only Glossary was captured).
- Only one Tập is ticked of two (the coordinator asked for 1 of 2, this matches).
