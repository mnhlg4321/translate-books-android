# P5E account-runner repair worklog — 2026-09-17

> Historical local work-package record. Đây **không phải** release checklist,
> không tạo release lifecycle mới và không phải authority cho device work.

## Scope đã hoàn tất

- Work-package branch/commit: `feature/v4.18-p5e-runner-repair-20260917` /
  `70fe4b1b9820997bd345da2c3cdd689a63b9378e`.
- Snapshot sau repair: `c6173e317a733e13080f041fdc152fe5fe08f4e8`.
- Runner dùng full instrumentation component, host stdin transport, child
  environment clearing, parser exact terminal/identity/result và bounded
  timeout capture.
- Helper future-only được sửa component/capture; Java account test và installer
  không đổi; APK không build hoặc reinstall.
- Offline QA: `37/37 PASS`, năm fake local process, `ADB/device/provider/DB/RAW`
  đều bằng `0`, retry bằng `0`.

## Evidence

| Item | SHA-256 |
| --- | --- |
| Account runner | `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65` |
| RAW helper, future-only | `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34` |
| QA result | `D8940D498AB9DABBBFED4A0A31013448622E266D30ACBC2AEFF7C9E96FEF82D7` |
| Account test source | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` |

## Failure log retained

- Parser/command transport defects were found by source-only audit and repaired
  in the same local work package.
- The missing independent expected provenance was not a repair failure. It
  leaves only the device branch stopped.
- Historical CheckOnly/replacement remains evidence of one test-package
  replacement, not an account result and not a RAW authorization.

The current next action is metadata-only provenance review in
`docs/P5E_NEXT_WORK_REQUEST_20260917.md`. The canonical release checklist is
`release_checklists/v4.18-editorial-v5-safe-4-1-3.md`.
