# Editorial API V1 — N6 execution record (2026-10-07)

## Scope and guard

This record covers N6 package 4A only. The owner-approved decisions D-N6 and D-CP are recorded in `docs/EDITORIAL_API_V1_CP_IMPL1_REVIEW_AND_N6_REQUEST_20261007.md` §6. No provider call, prompt/threshold edit, chunk-pair live run, or pilot-device operation was performed.

The build was made from a clean temporary worktree at source `81e4d5787793d0b93cbde7bc39e7d3f73aff43d7`, while the main worktree's unrelated dirty files were preserved.

## 4A build and archive

| Item | Evidence |
|---|---|
| Production | `4.18-api.6`, version code `243`; `scripts/build-and-save.ps1 -Series 4.18-api -MinimumVersionCode 243 -Offline` |
| Source | `81e4d5787793d0b93cbde7bc39e7d3f73aff43d7` |
| APK SHA-256 | `1158BF2C6A0B193C25524F12853E164D091EFD72435BCAD5112C797ED68DA590` |
| Source ZIP SHA-256 | `C7FFF310173FC171FDB61FE8B2756430F0D3B809E2D38CE7ACFD71A45F494ADD` |
| AndroidTest | event `n6-api-20261007-065128`; `scripts/build-and-save-android-test.ps1 -Offline` |
| AndroidTest APK SHA-256 | `65A2AC5A5BB9745C64926B55F0A372FE273D2E0C09ADE5BCD389931AA137B561` |
| Certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| Archive parity | production and AndroidTest payloads are present in both `artifacts/` and `backup/` |
| Installation | production install guard and AndroidTest install succeeded on `emulator-5554` only |

The production archive is `artifacts/builds/v4.18-api.6/build-20261007-065128/` (mirrored under `backup/builds/`). The AndroidTest archive is `artifacts/test-builds/v4.18-api.6/n6-api-20261007-065128/` (mirrored under `backup/test-builds/`).

## 4A device evidence

The following instrumented classes were run on `emulator-5554` with the archived APKs:

| Surface | Result |
|---|---:|
| `EditorialApiStoreInstrumentedTest` | 4/4 PASS |
| `EditorialApiBienTapFlowInstrumentedTest` | 3/3 PASS |
| `EditorialApiBienTapUiInstrumentedTest` normal UI | 2 executable tests PASS; 3 phase tests are opt-in and were run separately |
| `EditorialApiBienTapUiInstrumentedTest` process-death | `seed` 1/1, force-stop, `verify` 1/1, `cleanup` 1/1 PASS |
| `EditorialPairStoreInstrumentedTest` | 6/6 PASS |
| `EditorialPairUiInstrumentedTest` normal UI | 3 executable tests PASS; 3 phase tests are opt-in and were run separately |
| `EditorialPairUiInstrumentedTest` process-death | `seed` 1/1, force-stop, `verify` 1/1, `cleanup` 1/1 PASS |
| Full instrumented package | 246 tests; 235 PASS and 11 historical failures, matching the accepted baseline |

Full-suite output ended with `Tests run: 246, Failures: 11`. The same 11 historical fixture/schema/pilot-baseline failures were already present in the 4A baseline; no new Editorial API whole/pair or process-death failure appeared. The complete raw logs are retained outside Git at `D:\P5E-private\n6-4a-*`.

The instrumented run used no live-account argument and no provider test opt-in. Provider calls and spend for 4A: **0 / USD 0**. The pilot serial was not queried or installed.

## Gate

4A is **PASS**: the required whole and pair API classes, both process-death sequences, and the full suite completed within the historical failure allowance. Proceeding to 4B is allowed. 4C remains gated on the 4B adjudication and the presence of owner-selected inputs in `D:\P5E-private\n6-inputs\`.

## Next package

Perform 4B offline: adjudicate all 24 N5 runs using only private book evidence, choose the default mode under plan §6, and publish the line-ID-only report. Do not dispatch a provider call until that package is pushed and the 4C input gate is checked.
