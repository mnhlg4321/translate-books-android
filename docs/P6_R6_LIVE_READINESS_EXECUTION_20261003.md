# P6 R6 live-readiness execution record — 2026-10-03

Scope: owner request `docs/P6_R6_LIVE_READINESS_WORK_REQUEST_20261003.md`. Complete L0–L8 offline and, after those gates pass, run G1 only within Q1's approved USD 1.00 cap; stop and report before G2. The owner answer in §6 closes L9 for the emulator and pilot. Do not read, print, store, or transfer the API key. Do not put the fingerprint in Git or output logs.

## L0 — branch and baseline

- Branch: `feature/v4.18-p5e-runner-repair-20260917`.
- HEAD: `495b30bb6b409c0c3d93a06ac1c2da220260b4ab`, which contains the L9 owner answer and is newer than the request's minimum `fbde77f6`.
- The branch was synchronized with origin. The working tree had pre-existing owner edits and untracked evidence/artifacts in `.idea`, P5E documentation/scripts, `raw/`, `evidence/`, `artifacts/`, and owner backup folders. These were not staged, reset, cleaned, or moved.
- L9: §6 records that the same key is saved in app Settings on `emulator-5554` and pilot `15e84958`, and the supplied fingerprint applies to both. This is owner-provided placement evidence; the runner must still validate emulator Settings before dispatch.
- At L0 completion: no provider call, key read, pilot access, device operation, or database change was performed. No call or spend has been incurred in this live-readiness sequence.
- **L0 PASS.**

## L1 — live fixture runner implementation

- Added a separate `p6_fixture_live=YES` instrumentation gate. Without it the runner uses local fakes; a live run reads `SettingsStore` in process memory and checks the qualified route, nonempty key, and owner-supplied endpoint/account fingerprint before fixture setup or provider construction. Refusal results are fixed codes; neither key nor expected/actual fingerprint is written or printed.
- Live calls use the production ledger-v2 RAW and RECONCILE OpenRouter adapters, production L2/L3 adapters, and the existing `EditorialP6Budgeted*Provider` wrappers. The runner requires a group ID and cap (maximum USD 1.00); its hash-chained group ledger is stored under app-private files and is not removed by test teardown.
- A test-only recorder writes exact rendered prompt bytes for every phase to the private run directory before delegate dispatch. Structural and run metadata distinguish LIVE from FAKE_OFFLINE and include provider call count, usage, known USD, finish reasons, and a cumulative spend-ledger copy. The fingerprint itself is excluded.
- Added synthetic preflight tests for missing expected fingerprint, wrong route, missing key, and mismatched/matching fingerprint. Added an opt-in emulator test for a deliberately wrong fingerprint that reports only `FINGERPRINT_MISMATCH` and zero dispatches.
- `:app:compileDebugAndroidTestJavaWithJavac`: PASS. PowerShell parser check for `scripts/p6/run_group.ps1`: PASS. The emulator preflight test will run in L8 after the final archived production/test APKs are built and installed.
- No API key was read by Codex; the test code only reads it in emulator process memory as required. No provider call, pilot access, or spend occurred.

## L2–L8

Pending.

## G1

Not started. Authorized only after L0–L8 pass; maximum total spend USD 1.00; zero retries or repair calls; stop on UNKNOWN or cap exhaustion. Report separately before G2.

## Current next action

Implement L2's group-level spend ledger shared across fixture invocations, with host prechecks and a ledger pull/validation after each fixture.
