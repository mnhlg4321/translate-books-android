# P6 W3 — emulator QA (2026-10-02)

Scope: `docs/P6_G5_G6_WORK_REQUEST_20261002.md` group W3. Emulator only (`emulator-5554`, AVD `tbl-code113-dqa-api35`, android-35 x86_64); the pilot `15e84958` was only read (`dumpsys`, still `4.18-p5e.5`/213). No `connectedAndroidTest`, no provider call, no network.

## Candidate pair (archived in `artifacts/` and `backup/`, parity PASS)

| Item | Value |
|---|---|
| Source | commit `4bc1aa27ee80bf03ebd1d20aa359f3043406f628` (clean worktree, branch removed afterwards) |
| Production | `4.18-p6.2` / versionCode 215, event `build-20261002-082255`, APK SHA-256 `51BA2A2B38730FC4498C92E1B687882BBDF8BBB5BD34D49F90BED63B8BCA9703`, source ZIP `FB4FC309…3D61`, BUILD_INFO `CEEF43F6…7182`; unit 311/311, lint PASS, cert `47f31389…c155` |
| AndroidTest | event `p6-w3-prod215-20261002-01`, APK SHA-256 `41ED827EB2B9BF308906F98DEA619D2F86758679BB6A1F937E5F5E212E4F6500` (byte-identical to the earlier `p6-w3-prod214-20261002-02` test APK: the test sources did not change), BUILD_INFO `1B4E2CCC…BF5F` |
| Scripts | `scripts/build-and-save.ps1 -Series 4.18-p6 -MinimumVersionCode 215`, `scripts/build-and-save-android-test.ps1` (both through a clean temporary worktree because they refuse untracked files) |

Earlier candidate `4.18-p6.1`/214 (commit `bc10aca1`, and test events `…-01`/`…-02`) is superseded and kept immutable; it was installed on the emulator only.

## Instrumented classes (`am instrument -w -r -e class … com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner`, emulator only)

| Class | Result on the 215 pair |
|---|---|
| `EditorialChapterFinalCoordinatorInstrumentedTest` | OK (7 tests; includes the opt-in seed test, which skips without `p6_seed_emulator_ui`) |
| `EditorialPhaseArtifactStoreInstrumentedTest` | OK (8 tests) |
| `EditorialP5EReconcileLineageInstrumentedTest` | OK (5 tests) |
| `EditorialP5CExactBindingFakeE2EInstrumentedTest` | OK (19 tests) |

The first full pass on the 214 pair gave 6 + 8 + 5 + 19 and the second 7 + 8 + 5 + 19; raw outputs are in `D:\P5E-private\p6-w3-emulator-20261002T010247Z` and `D:\P5E-private\p6-w3-emulator-final-20261002T012412Z` (not in Git). The coordinator class exercises the four-call chain (blind discovery → edit → blind re-audit → reconcile) on a real SQLite database with fake providers, progress per stage after reopening the repository, an interrupted L3 (`AssertionError` escaping like a process death → `IN_FLIGHT_UNKNOWN`, resume dispatches 0 calls), a failed provider call (`RECOVERY_REQUIRED` with `RETRY_L2_PROVIDER_CALL_FAILED`) and failed exports leaving the stored FINAL untouched.

## UI smoke (uiautomator + screenshots, emulator only)

1. Fresh install (`pm clear`): the Editorial tab renders, 0 `FATAL EXCEPTION`.
2. Seed `L1` (opt-in test method, refuses any database with P4 bindings): the chapter card shows "Editorial L1 ✓ • L2 chưa chạy. Chạy cần cấp phép riêng" and the button "Chạy L2 → L3 (cần cấp phép)"; the dialog states what leaves the device and prints the D3 numbers (per-phase output and USD caps, input ≤ 200000 byte, 180 s, 0 repair, 0 retry, chain USD 0.30). Allowing the run with no key configured was refused before any claim: `editorial_phase_artifacts` stayed at 0 rows and the card returned to "L2 chưa chạy". After `am force-stop` and reopen the card shows the same state.
3. Seed `FINAL` (fake chain): the card shows "L1 ✓ • L2 ✓ • L3 ✓", "Xem bản cuối" shows the text and `sha256 ae3667bcfd35…, 18 byte`; "Xuất TXT" through the system picker wrote `editorial_<chapter>_final.txt`; pulled back, 18 bytes, SHA-256 `AE3667BCFD35E6AC34C040F4D75924633F0A73654F5AD29C9D1A3E751008C5E8` = the stored FINAL.

## Findings

- Fixed (`4bc1aa27`): on a P4-bound chapter the legacy lines "execution blocked: SAFE 4 engine is locked …" and "Execution đang khóa" contradicted the working panel.
- Emulator artifact, not a product bug: the AVD held a database from another lineage (`user_version` 25 but no `editorial_p4_bindings`/`p5c`/`p5d` tables and `editorial_ledger_*` tables the current code never creates), so no migration ran and the Editorial tab crashed with `no such table: editorial_p4_bindings`. App data was cleared on the emulator only; a copy of the file is in the private evidence folder.
- Not covered: the real-provider path (key, route, network) and the pilot database; the OpenRouter adapters were only exercised by JVM tests that never reach a transport.
