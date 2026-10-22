# QA report — v4.23-dev.22 / R3a + R2

- Date: 2026-10-22 (+07:00)
- Device: `15e84958` (real OnePlus CPH2691; no emulator)
- Source implementation commit: `29a9c76beb46c3356089eec77aae638e743f41cb`
- Final worktree HEAD: `488cb3756b82787d1f33b333db851927855af68b`
- Version: `4.23-dev.22`
- versionCode: `300`
- APK SHA-256: `7ACF507F3436BE1AB306B9DBC5325A491CCBD55763098232892498F80B3D2406`
- Build event: `build-20261022-203705`
- Build artifact: `D:\P5E-builds\wt-v4.23\artifacts\builds\v4.23-dev.22\build-20261022-203705\TranslateBooks-v4.23-dev.22-code300.apk`
- Durable build backup: `D:\P5E-builds\wt-v4.23\backup\builds\v4.23-dev.22\build-20261022-203705\`
- Phone backup: `D:\App Translate Books\App Translate Books-device-backups\v4.23-dev.22-code300\20261022-213700\`
- Phone backup `base.apk` SHA-256 (pre-install dev.20/code298): `46C608AC8E90DBCE25409AB9EA5F21CF57B7C5A5025810E50C0354210BFA6991`
- Phone backup `private-data.tar.gz` SHA-256: `60A403F37B3E71DB52CE4BD9DEC3C85367B36D651A56E9A4D14EC08D0E96BA1E`; verified listing count: 1,380 files.
- Provider/API calls: 0. No translation action, retry, resume, Start, or provider action was invoked.

## Regression

- Clean Git archive: `D:\P5E-builds\v423-r3a-r2-regression-20261022-01\`
- `testDebugUnitTest lintDebug`: PASS; 677 tests, 0 failures/errors, 5 skipped; 58 lint issues reported without build failure.
- APK artifact/backup mirror: PASS; five payload files matched.

## D1–D6

| Item | Result | Evidence |
|---|---|---|
| D1 | PASS | `Japanese → Vietnamese` visible at app font 100%; `D1_target_100.png`. |
| D2 | PASS | App font 130% kept target-language identity and readable/wrapped controls; `D2_target_130.png`. Old 90% was restored immediately after. |
| D3 | PASS | Glossary/Pronoun labels and actions were visible; button bounds were at least 48dp on the device; `D3_glossary_labels.png`, `D3_pronoun_labels.png`. |
| D4 | PASS | `Chi tiết kỹ thuật` was collapsed by default; open state exposed `Log hoạt động`, `Xóa log`, `Xuất log`; buttons were reachable after one scroll. Clear/Export were not activated to avoid mutating owner logs. Evidence: `D4_log_collapsed.png`, `D4_log_open_scrolled.png`. |
| D5 | PASS | Only `QA_V423_Glossary` was created: `Dùng` → `Sửa` → `Lưu` → two-step delete confirmation → `VẪN XÓA`; final UI contained no QA item and active Glossary returned to `—`. Evidence: `D5_glossary_delete_confirm.png`. |
| D6 | PASS* | TalkBack was enabled through Android Accessibility UI, bound, and app labels remained exposed; then it was turned off. Evidence: `D6_talkback_app.png`. |

`*` D6 functional state was restored: `accessibility_enabled=0`, no bound/enabled service, `touch_exploration_enabled=0`, and TalkBack notification permission returned to `granted=false`. Android's UI leaves `enabled_accessibility_services` as an empty value instead of the original absent/`null` key; ADB was denied permission to delete that Secure Settings key. No accessibility service remains enabled.

## Appearance restoration

- Before: app `fontPercent=90`, `mode=fixed`, `palette=paper`, `paletteLight=paper`; system night mode `no`.
- Tested: app font 100% and 130%; system-following appearance with night mode `yes`; evidence `appearance_dark_probe.png`.
- After: app `fontPercent=90`, `mode=fixed`, `palette=paper`, `paletteLight=paper`, comfortable density, medium corners; system night mode `no`.

## Evidence handling

All listed screenshots were captured with `adb shell screencap` and `adb pull`, have PNG prefix `89 50 4E 47`, and have distinct SHA-256 values. The screenshot files remain local QA evidence and are intentionally not committed.
