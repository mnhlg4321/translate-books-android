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

## 4B offline adjudication

The complete 24-run adjudication is in `docs/EDITORIAL_API_V1_N5_ADJUDICATION_20261007.md`. Raw book evidence remains private at `D:\P5E-private\n5-adjudication\20261007\adjudication.json` (SHA-256 `B9C10626B123EB3E368AA854F870F4AA8FF0E2975A5556ED7D704417633249F1`). Git contains only response hashes, line IDs and classifications.

The review found one new error: the base Kỹ `fx-a04` response (`7c8a177826b343cbfad72db379f60e491089509eb7e72b273d1f8cc193bc5e7d`) repaired `L71` but omitted `L74–L381` and returned 37/192 lines. It is `NEW_ERROR:OMISSION`; the response remains invalid and is not accepted. Nhanh had no gate-class new error or truncation. The Nhanh base `fx-a11` missing sentence was independently confirmed as an improvement at `L327` even though the mechanical scorer could not align the inserted line.

Both arms scored 19/25 mechanically; Nhanh used 12 calls / USD `0.11166139`, Kỹ 29 calls / USD `0.15397365`. After subtracting the pre-existing `E_L245_MEANING` on `fx-a02`, no new MEANING/OMISSION/NUMBER/NEGATION error was confirmed on the control or ambiguous fixture. Per plan §6, the default is **Nhanh (E)**. This is an offline decision only: acceptance, prompt, thresholds and all stored responses are unchanged; semantic quality outside the seeded matrix remains `NOT_MEASURED`.

## Next package

Proceed to 4C only after checking that owner-selected files actually exist in `D:\P5E-private\n6-inputs\`. If present, use whole flow with Nhanh, the approved model and fresh N6 ledger/caps; otherwise stop and ask the owner. Do not run chunk-pair live or touch pilot.
