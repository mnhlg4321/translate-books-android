# QA Report — v4.1

## Scope

- Settings pull-to-refresh gesture conflict.
- Expandable Settings header/body overlap.
- Small-screen Settings layout, field sizing, fixed bottom navigation and upgrade preservation.
- Regression build and unit suite for existing translation/configuration behavior.

## Root causes reproduced before the change

1. The global `SwipeRefreshLayout` asked the page-shell `LinearLayout` whether it could scroll upward. The real scrolling child was a descendant `ScrollView`, so the callback could return false while Settings was away from the top and pull-to-refresh intercepted the gesture.
2. Mobile Settings rendered the category selector as a separate elevated `Button` immediately above a separately scrolling detail surface. Their shared boundary had no structural parent section, so the button state/elevation painted over the first edge of expanded content and visually hid its border.
3. API 27+ re-declared `AppTheme` without the no-action-bar parent, which allowed a system action bar to consume vertical space on affected devices.

## Implementation verification

- Settings uses one `NestedScrollView` with `fillViewport=true`; bottom navigation remains its sibling outside the scroll viewport.
- Entering Settings cancels an active spinner and disables the global refresh container. Refresh remains enabled only for workspaces with a refresh operation.
- Every mobile category uses the same zero-elevation vertical accordion: header, one divider, then padded body.
- Accordion bodies toggle in place, so repeated expansion does not rebuild the page or accumulate margins.
- Text inputs are single-line, do not nested-scroll, and have a 48 dp minimum height. Header arrows have a 48 dp touch target.
- Paired fields stack below 360 dp or at font scale 1.30 and above.

## Tests performed

- `testDebugUnitTest`: passed, including refresh policy and responsive field policy tests.
- `assembleDebug`: passed.
- Installed over v4.0.0-alpha1.4 on a 1264 × 2780 physical Android 15 device: saved language, provider, YAML, pronoun profile and performance values remained present.
- Visually scrolled Settings top-to-bottom and back using cards, empty areas and Performance fields.
- Expanded Performance in the middle of the page: the complete body border and first `CHUNK MODE / SOFT LIMIT` row rendered below the header.
- Repeated accordion and rapid Translate/Jobs/Library/Settings navigation: no crash and no stale refresh UI.
- Confirmed bottom navigation stayed fixed while Settings content moved behind its bounded viewport.
- Device shell was not allowed to change global font/display settings; large-font behavior is covered by the responsive policy test and wrap-content layout inspection, not a device screenshot.
- Both bundled dark palettes remain readable. The application does not currently expose a light theme.

## Artifacts

- Before: `build/qa/settings-general-before.png`, `build/qa/settings-performance-before.png`
- After: `build/qa/v41-settings.png`, `build/qa/v41-performance.png`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

Final package metadata: versionName `4.1`, versionCode `39`.
