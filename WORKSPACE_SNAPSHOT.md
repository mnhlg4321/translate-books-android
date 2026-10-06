# Workspace Snapshot

- Updated: 2026-10-06 (+07:00): Editorial API V1 N1–N4 offline complete. N1–N3 from Claude remain pushed; N4 verified on the same clean source. Engine 484/484, app 386/386, Python 71/71, AndroidTest compile PASS; UI 3/3, force-stop/reopen 2/2, store/migration 4/4 on emulator-5554. Fixture runner dry-run is structural 1/1 with fake provider and 0 actual calls. No provider call or pilot access.
- Current version: active v4.18.
- Current branch: `feature/v4.18-p5e-runner-repair-20260917`; continue the existing branch and release checklist.
- Current commit: 18571c01 — implementation baseline immediately before this snapshot update; this snapshot is committed as the N4 documentation package.
- Current build: 4.18-api.3/code240, source 18571c0138436a699bdcc67851b2afb42b60d60b; APK SHA-256 9F66F0C9D69F19728DD7EBB77138B946CDAF6672081F468A7B52638E5E4957C8, AndroidTest 20CBCF3C69AE4E81C26B31FB73BB0C6B94FDC1282AB2F5AE38F6444D812A9381, source ZIP 971373AF209C5FF137EA3F52BCB6008C734F06A012C80E5F1EC01EEFA2227A14; payloads are mirrored in both archive roots and the packages are verified on emulator-5554 only.
- Current phase: EDITORIAL_API_V1 N1–N4 complete; stop before N5.
- Completed tasks: D-N1..D-N3 recorded; N1 engine, N2 app/store/provider/service/export/runner, N3 Biên tập UI; N4 wrapper archives, clean-source regression, emulator fake-provider checks, screenshots and fixture dry-run.
- Pending tasks: owner D-N4 decision for N5; no provider, pilot or new ledger run is authorized by this package.
- Known bugs: temporal oracle-probe bypass was repaired and its 54/54 Python regression remains recorded. Semantic findings include 2 confirmed additional defects, 3 false positives, 1 preference and 1 unresolved relation; L1-only repair is NOT_MEASURED. G1 model/route/source metadata are absent from run metadata. The approved G2 repeat-1 predecessor state is unavailable on the emulator; base substitution would alter the inherited-finding measurement.
- Regression status: engine 484/484, app 386/386, Python 71/71, AndroidTest compile PASS; UI 3/3 plus 2/2 after force-stop, store/migration 4/4, runner structural 1/1 with 0 actual provider calls. G1/G2 historical ledgers and evidence are unchanged.
- Next action: owner decides D-N4 scope and budget before N5; no live dispatch from this offline package.
