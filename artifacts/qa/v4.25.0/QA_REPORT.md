# QA report — Translate Books 4.25.0

- Date: 2026-11-04 (+07:00). Release branch `feature/v4.25.0`, created from `feature/v4.25` at `e014f8c9f72c5eb16cb4daa9f5e1e67bb8f36b6c`.
- Production commit (phone APK and PC package): `e014f8c9`. Later commits on the release branch change only tests, docs and evidence.
- Owner authority: "Phát hành theo plan" and "Sửa test, phát hành" (chat, 2026-11-04). Merge to `main`, push and any install need a separate approval.
- No provider call, no phone or PC install in this QA. Device work on `emulator-5554` only; the owner phone was not connected.

## 1. Builds

| Item | Version | Hash (SHA-256) | Location |
|---|---|---|---|
| Phone APK | `4.25.0` / versionCode `329` | `4B82F0D58B00AF391E9E53879F0717870C28BBA742A1F9475056C63A54FF7B10` | `artifacts/builds/v4.25.0/build-20261104-204947/` and `backup/builds/v4.25.0/build-20261104-204947/` |
| Phone source ZIP | — | `D81344A539742694072A9D91F0AE859074BED2C79443A65587E65E63B597B4A9` | same folders |
| AndroidTest APK (pinned to the release APK) | event `device-v4250-release-20261104-b`, source `0a05d316` (test-only change after `e014f8c9`) | — | `artifacts/test-builds/v4.25.0/device-v4250-release-20261104-b/` |
| PC zip | `4.25.0` | `B65D2D294B6A5B6AFE6A2DBDC5E93E777316181AACF525D3C1F63C8C4CFFA52C` | `artifacts/builds/desktop-v4.25.0/build-20261104-213322/` and the backup mirror (every file identical) |
| PC msi | `4.25.0` (ProductVersion `4.25.200`) | `F8B51AD8D573620B79E89D56A462DD6A2EF3353FD26AF85068950BBA420FA77E` (57,330,424 bytes) | same folders; built from a clean temporary worktree at `e014f8c9`, jlink runtime, window smoke start (63 page screenshots) |

## 2. Regression (clean `git archive` of `e014f8c9`)

- `:app:testDebugUnitTest` 766 (5 skipped, 0 failed); `GoldenPromptSnapshotTest` 14/14.
- `:translation-core:test` 67 (1 skipped, 0 failed); `:translation-core-jdbc:test` 3/3; `:desktop:test` 83/83; `:editorial-engine:test` 742 (1 skipped, 0 failed).
- `:app:compileDebugAndroidTestJavaWithJavac` passed; `:app:lintDebug` 0 errors, 67 warnings.
- `git diff v4.24.1 e014f8c9 -- '*Editorial*' editorial-engine` is empty.

## 3. Device tests (emulator-5554, release APK 4.25.0/329)

| Class | Result |
|---|---|
| `V423ChapterRunInstrumentedTest` (10 tests) | `OK (10 tests)`, 38 s (`device-b/`) |
| `V425RetainedSourceRepairInstrumentedTest` | `OK (1 test)` |
| `V425SingleRawLibraryInstrumentedTest` | `OK (1 test)` |
| `V425OutputRecoveryRetentionInstrumentedTest` | `OK (1 test)` |

- First run (`device/`, test APK from `e014f8c9`): `aChunkCutAtEveryLimitIsSplitInHalvesAndTheChapterCompletes` failed (3/3 alone, and on `4.25-dev.9` as control). Cause: the fixture answered its cut with a few short lines, which the F6/F6b rule classes as reasoning-bound, so the chunk ended without a raised limit or split. The product rule was kept after measuring the owner's phone data (Claude Haiku 5.5: provider tokens / app estimate 1.09–1.20). Test fix `0a05d316`: both cut fixtures use the shared full-length `cutAnswer`, and the split test asserts two cuts with a raised limit before the split. This test failed already in v4.24.0-rc1 QA.

## 4. Performance (emulator-5554, not a real phone)

Macrobenchmark `StartupMacrobenchmark#coldStartup`, benchmark variants built from clean `v4.24.1` and `e014f8c9`, the same macrobenchmark APK (`15A9E5A4…`, identical to the one used for 4.24.0), fresh install before each run, alternating, 3 rounds × 5 iterations, `androidx.benchmark.suppressErrors EMULATOR,LOW-BATTERY`. Script and raw output: `performance-ab/`.

| Round | v4.24.1 median (ms) | 4.25.0 median (ms) | Difference |
|---|---|---|---|
| 1 | 643.2 | 653.9 | +10.7 |
| 2 | 752.1 | 781.1 | +29.0 |
| 3 | 735.3 | 787.2 | +51.9 |

- Finding: 4.25.0 is slower in all three rounds, about +31 ms (≈ 4.5 %) on average. The difference is smaller than the spread between rounds (643 → 752 ms for the same build) but always in the same direction. Candidate cause, NOT_DIAGNOSED: the output-recovery cleanup added to startup in v4.25 (`f66367a8`). Not a release blocker; to be checked on the real phone.
- Perfetto: one trace of 4.25.0 round 3 (`performance/StartupMacrobenchmark_coldStartup_iter001_2026-11-04-14-32-16.perfetto-trace`, 5.4 MB) plus `benchmarkData.json`; all 30 traces (about 170 MB) stay outside Git in `D:\P5E-builds\qa-v4250-traces\`.

## 5. Screenshots and video

- Phone (emulator, release APK, synthetic QA data): `screenshots-phone/light-01-translate.png`, `light-02-translate-scrolled.png`, `default-01-translate.png`, `default-02-translate-scrolled.png`. Dark default palette: selected buttons now black text on the light-blue accent (was the light workspace text). The Translate card shows leftover test-fixture state ("3 tệp TXT đã chọn / Không chuẩn bị được chunk"), not a release defect. The emulator appearance file was restored afterwards.
- Phone video: `phone-emulator-smoke.mp4` (22 s, 540×960).
- PC: before/after images of the UI rounds (`artifacts/qa/v4.25-pc-ui-r12-20261104/`), Library multi-select (`artifacts/qa/v4.25-pc-lib-a-20261104/`, `artifacts/qa/v4.25-pc-lib-a-fu-20261104/`); the coordinator inspected the Translate two-column page, dark Settings, Pronoun columns, Tập tick boxes light/dark, the "Vẫn xóa" dialog with Cancel default and the undo bar.

## 6. PC install history in this release cycle

- `4.25-pc-dev.1` (4.25.1), `4.25-pc-dev.2` (4.25.2) and `4.25-pc-dev.3` (4.25.3) were installed on this PC with owner approval, each with a data backup and byte-identical data afterwards. The release msi 4.25.0 (4.25.200) is not installed.

## 7. NOT_MEASURED

- Real-phone startup and translation behaviour of 4.25.0; the phone runs `4.25-dev.9`/328.
- A real translation on the PC (needs spend authority).
- Token ratio of models other than Claude Haiku 5.5 and gpt-5.6-luna for the reasoning-bound rule.
- Why the chapter-run class passed 10/10 on 2026-11-03 with the old split fixture.
- Release msi installed over 4.25.3.
