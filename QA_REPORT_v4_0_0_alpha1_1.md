# QA Report v4.0.0-alpha1.1

## Root causes

1. Category clicks called `refreshCurrentPage()`, a data refresh operation rather than a navigation operation.
2. The phone horizontal category strip was reconstructed at scroll position zero, making the active category appear to jump away.
3. Palette selection called Activity `recreate()`, causing a visible restart.
4. Provider/model/preset actions depended on a non-focusable EditText receiving the click instead of making the entire setting row actionable.

## Fix

Settings now uses a dedicated category switch path with no scan, refresh toast or runtime log append. Phone uses a stable category picker. Picker rows are fully clickable. Appearance reapplies the shell in memory and preserves the current destination/category.
