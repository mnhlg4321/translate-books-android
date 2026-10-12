# P6 §7 live preflight result — 2026-10-03

## Outcome

The emulator network is available, and the deliberately wrong-fingerprint preflight passed its zero-call check. The correct owner-supplied fingerprint did **not** match the endpoint/key pair currently saved in the emulator Settings. Stop before G1.

## Evidence

- Device scope: `emulator-5554` only. The pilot `15e84958` was not queried or changed.
- Network: emulator boot completed; Wi-Fi on, mobile data on, airplane mode off. One ping each to `8.8.8.8` and `openrouter.ai` succeeded with no packet loss. No network setting needed changing.
- Wrong fingerprint: `EditorialP6FixtureLivePreflightInstrumentedTest#liveModeRejectsTheDeliberatelyWrongEmulatorFingerprintBeforeAnyDispatch` passed and returned `P6_LIVE_FINGERPRINT_MISMATCH`; provider calls `0`.
- Owner fingerprint: the live fixture runner returned `P6_LIVE_FINGERPRINT_MISMATCH`; provider calls `0`. The guard reached fingerprint comparison, which means the pinned route predicate and non-empty-key check passed. The supplied fingerprint does not match the endpoint/key currently stored by the emulator app.
- The test output was reduced to fixed result codes and call counts before saving. No API key or endpoint/account fingerprint value is included in Git or this report. Codex did not read or print the API key.
- Private redacted evidence is under `D:\P5E-private\p6-live-checks\20261003\`.

## Stop state

- G1 was not started. Provider calls and spend for this §7 attempt: `0`, USD `0.00`.
- `score_run.py` was not run because there are no G1 fixture outputs. `STRUCTURAL_VALID` and `SEMANTIC_EVAL` are therefore not measured for G1.
- G2 was not started.

## Required next action

On the emulator, verify that Settings contains the intended OpenRouter key, or provide the endpoint/account fingerprint that matches the key currently saved there. Do not send the API key in chat. After that owner action, rerun the matching zero-call preflight; start G1 only if it returns `MATCH` with zero provider calls.
