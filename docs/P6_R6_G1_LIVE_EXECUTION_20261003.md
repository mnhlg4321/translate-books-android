# P6 §7 live execution result — 2026-10-03

## Preflight

- Emulator `emulator-5554` only; pilot `15e84958` was not accessed.
- Network recheck passed: Wi-Fi/mobile data on, airplane mode off, and one successful ping each to `8.8.8.8` and `openrouter.ai`.
- Deliberately wrong fingerprint returned `P6_LIVE_FINGERPRINT_MISMATCH`, provider calls `0`.
- After the owner regenerated the fingerprint locally, the live runner passed the matching preflight (`MATCH`) and stopped at an intentionally absent fixture manifest before provider construction; provider calls `0`. The earlier mismatch is recorded in `P6_R6_G1_LIVE_PREFLIGHT_20261003.md` and was resolved by this new fingerprint. No fingerprint value or API key is recorded here.
- Installed production code227 and AndroidTest event #14 matched their archived APK SHA-256 values. Fixture hash/leak validation passed `14/14`; the pre-dispatch prompt-input guard passed.

## G1 stopped on its first fixture

- Group: `G1-20261003-ecbf8c55`; base run: `c2f57361-61ba-47d2-bba5-20110261453a`; approved cap: USD `1.00`.
- `run_group.ps1` started `fx-a03` in `L1_ONLY`. One live call reached phase `L1_RAW_DISCOVERY`; the runner stopped with `P6_L1_PREDECESSOR_FAILED:REPAIR_L1_LEDGER_INVALID` before committing `REPORT_L1`.
- The verified group ledger contains one settled call, two ledger entries, USD `0.0081852` settled/exposed, USD `1.00` cap, and zero pending UNKNOWN reservations. The reservation for that call was USD `0.0348304`.
- No `report-l1.json`, `structural.json`, or `final.txt` was produced for `fx-a03`; L1 reconcile was not dispatched. The remaining G1 fixtures and both planned repeats were not dispatched. No retry or repair call was made.
- `score_run.py` was run on the available partial output: `STRUCTURAL_VALID=0/1` (`STRUCTURAL_RESULT_MISSING`); `SEMANTIC_EVAL=FAIL` solely because `FINAL_TEXT_MISSING`. This is not a translation-quality evaluation. Durable token totals were not produced because the runner exited before writing run metadata; the spend ledger retained the known USD cost.
- Private run/evidence: `D:\P5E-private\p6-runs\c2f57361-61ba-47d2-bba5-20110261453a\` and `D:\P5E-private\p6-live-checks\20261003\`.

## Stop state and next action

G1 is incomplete and stopped after one provider call. No further G1 call is authorized by the current no-retry/no-repair scope; G2 was not started. Diagnose `REPAIR_L1_LEDGER_INVALID` offline, then obtain owner authorization before any live rerun or continuation that would dispatch additional calls.
