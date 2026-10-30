# v4.24.1 — Library tab row fix (phone)

Request: the owner sent a screenshot of Thư viện · RAW on phone `15e84958` (4.24.0, 1264 px wide at 560 dpi, about 361 dp, system font 1.0) and asked to research and fix the UI defect. Patch release, phone app only; the PC app uses a JavaFX navigation tree and is not affected. Branch `feature/v4.24.1` from tag `v4.24.0`; checklist `release_checklists/v4.24.1.md`.

## What the owner saw and what it really was

Reproduced on emulator-5554 with `wm size 1264x2780`, `wm density 560` and the 4.24.0 release APK (`artifacts/qa/v4.24.1/library-tab-row/before-*.png`, sheets `sheet-before-*.png`).

| # | Symptom | Cause (confirmed) | Fix |
|---|---|---|---|
| 1 | "Glossary" and "Pronoun" touch the tab border | `LibraryTabLayoutModel.TAB_HORIZONTAL_PADDING_DP = 4` | 8 dp (the widest label still fits four equal tabs at 361 dp, app text 100%) |
| 2 | Four tabs of different widths, packed left, a gap before the menu | tabs were `LayoutParams(-2, 48dp)` in a `HorizontalScrollView` without `fillViewport`; no weight | every tab is as wide as the widest label plus padding and shares the leftover space (weight 1, `setFillViewport(true)`): four equal tabs filling the row; when they do not fit (app text 115/130%) the same equal tabs scroll sideways with a fading edge |
| 3 | Selected tab's border looks blue only on the left and top, the cards look cut | **Not an elevation or `stateListAnimator` shadow (hypothesis refuted: no shadow in the pixels).** The bottom 2 dp of every tab is **clipped**: the nav row was 56 dp high with 4 dp top and 6 dp bottom padding, so the scroll view had 46 dp inside for 48 dp tabs. The menu button is not in a scroll view, so it kept its full border | row height is now `NAV_TOP + 48 + NAV_BOTTOM` from the model (`navHeightDp()`), the scroll view and tabs are 48 dp; the selected tab has a 2 dp blue border on all four sides |
| 4 | Menu ⋮ looks lost: small glyph, different corner radius | `secondaryButton` radius 12 and 13 sp glyph | same 48 dp height, radius 9 and 1 dp border as the tabs, 20 sp glyph, 4 dp gap from the last tab, content description kept |
| 5 | Page scrollbar drawn over the right border of the cards | the system vertical scrollbar of the page `ScrollView` is an overlay inside the view bounds | not changed: it is a thin transient system overlay that fades after use; moving it needs a right margin on every page. Listed for the owner to decide |

Also added: when the row scrolls, the chosen tab is scrolled into view (at 130% text "Tập" was hidden when selected).

Family check: no other `HorizontalScrollView` row uses this pattern (`grep` in `app/src/main`: only `buildLibraryDestination`), and no other nav row combines a fixed 56 dp height with padding. `button()` itself is unchanged.

## Files

- `app/src/main/java/com/ml/tblandroidtxt/MainActivity.java` `buildLibraryDestination` (tab row, menu button, scroll into view).
- `translation-core/src/main/java/com/ml/tblandroidtxt/LibraryTabLayoutModel.java`: padding 8, gap, row height, nav paddings, menu text size, `equalTabWidthDp`, `navHeightDp`, estimate factor 0.50 (measured on the phone: "Glossary" 0.48 sp per character).
- `app/src/test/java/com/ml/tblandroidtxt/LibraryTabLayoutModelTest.java`: padding, row height against the clipping, owner phone fits, large text scrolls, equal tabs at 0.9/1.0/1.15/1.3 app scale × 1.0/1.3 system scale on 320/361/411 dp, monotonic scroll decision, source checks that the builder uses the model.

## Evidence

Screenshots (emulator, 361 dp, app text 100/115/130%, dark and light, all four tabs): `artifacts/qa/v4.24.1/library-tab-row/` (sheets before and after, two full screenshots each, the script). Full set of 24 per side in `D:\P5E-builds\qa-rc1\libshots\{before,after}`.

Constraints kept: phone only; no Editorial change; not installed on the real phone (needs the owner's "duyệt cài" and the three install steps); no provider call; no tag, merge or push.
