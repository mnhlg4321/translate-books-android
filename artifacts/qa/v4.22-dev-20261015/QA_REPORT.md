# v4.22 dev label-fix verification — 2026-10-15

Product decision served: provide the owner a usable v4.22 development APK with the corrected changed-line label. This is not a release.

- Source: `e8083883ee11e9f3c31e59d15a916854f67fffec` on `feature/v4.22`.
- Production APK: `4.22-dev.1` / code 274, event `build-20261015-090335`, SHA-256 `4E0B6AACF21A295458DFA459059F0FE5D628358A295C375B0AADC0B3A3ABD7D9`.
- Test APK: `v422-dev1-test-20261015-090335`, SHA-256 `AFA78D0AAF72AFF145090756ED91B0754F6F3B8FB2DA11FEE48EF2E136C4FF35`; it is pinned to the production APK, source ZIP and signing certificate.
- Both payloads were mirrored byte-for-byte to `D:\App Translate Books\artifacts` and `D:\App Translate Books\backup` before installation.
- Clean archive regression: 2,207 tests, 16 SKIP, 0 FAIL; lint and AndroidTest compilation PASS. The SKIPs are opt-in tests and are not counted as passes.
- `emulator-5554` only: validated install/readback of code 274, then `EditorialV422MediaWalkthroughInstrumentedTest` with synthetic data and a fake provider: 1 PASS / 0 FAIL, 11 screenshots. It confirms the corrected visible label `Đoạn 1 · Dòng 1`.
- The walkthrough restored the emulator database and journal hashes exactly. Provider calls: 0; USD 0. No phone operation, schema change, tag or merge.

The raw walkthrough output and the eleven images are retained under `media/`; host regression output is private at `D:\P5E-private\v422-host-evidence\label-fix-regression.log`.
