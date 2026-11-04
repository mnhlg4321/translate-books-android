# v4.25 PC UI round 1 and 2: before/after screenshots (2026-11-04)

Synthetic QA data only (`QaSeed`); no book text, no API key, no provider call.

- `before/`: clean `git archive` of `6cd0faa4` (branch HEAD before the round 1 commits), built with `:desktop:installDist`, run with `TBL_DATA_DIR`, `TBL_SCREENSHOT_DIR`, `TBL_QA_SEED_DIR` (fresh throw-away folders).
- `after/`: clean `git archive` of `e3207f4d` (final commit of this round), same method, fresh folders.
- 67 images per set, same numbering. Each image is the scene root at the harness's default size (about 1280 x 720).

Commits in this round: `5183ce33` (round 1 display fixes), `487b81cc` (round 2 Translate layout and pricing move), `a98691df` (pronoun column sizing, found on review of the after images), `e3207f4d` (API key full-width line, found on review of the after images).

Inspected images:

- `001-sang-trang-translate.png` (before/after): before, the three ComboBox values in "Cấu hình bản dịch" are solid accent blue and the RAW picker row also includes "Thư mục (nhiều chương)"; after, combo values are plain on the field colour, "Nguồn & cấu hình" sits left and the "Lượt dịch" card right, with Start visible at 1280 px.
- `014-toi-trang-translate.png` (after, dark default): same two-column layout in the dark palette; Start is the accent button with dark text.
- `008-sang-trang-settings.png` and `021-toi-trang-settings.png` (after): status line reads "Đã lưu tự động khi sửa (trừ API key: bấm Lưu key)"; "Lưu kết nối" is gone; the API key field now spans the card with its prompt "API key (không hiển thị lại)" fully visible.
- `011-sang-pronoun-bang-quy-tac.png` (before/after): before, `from` took about 880 px and the other six columns were squeezed to "…"; after, all seven columns (from, speaker, target, self, call, scope, note) are readable at 1280 px.
- `002-sang-trang-volume.png` (after): the "Dịch · Cả tập" page is unchanged apart from the accent highlight in the chapter list (list selection keeps the accent, as intended); the in-table combos are not accent-filled.
