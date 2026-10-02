# P6 W4–W6 — chapter 001: first L2/L3 chain on the pilot (2026-10-02)

Authority: owner marked D1 (option d), D2 (chapter 001), D3 (recommended per-phase caps) and D4 (install on the pilot) in section 3 of `docs/P6_G5_G6_WORK_REQUEST_20261002.md` (record: commit `10e3cd60`). One chain, one authorization, no retry. Raw evidence stays in `D:\P5E-private\` (not in Git); no model text is reproduced here.

## W4 — pilot install (`15e84958`)

| Step | Result |
|---|---|
| M0 before | production `4.18-p5e.5`/213 `88854E47…`, test `26CB0563…`, DB `06C4C48E…` (18,972,672 B, v25, integrity ok), L1 RAW `7a5e3428…` + RECONCILE `7483b211…` COMMITTED, no WAL/SHM — `p6-w4-m0-20261002-013814902` |
| DB copy before install | `p6-w4-preinstall-20261002T013830Z`, SHA-256 `06C4C48E…` = M0 |
| Install | `scripts/install-validated.ps1` check-only PASS, then install of `4.18-p6.2`/215 (APK `51BA2A2B38730FC4498C92E1B687882BBDF8BBB5BD34D49F90BED63B8BCA9703`, archived build `build-20261002-082255`); AndroidTest package left at `26CB0563…` (the UI path does not use it) |
| Readback | APK `51BA2A2B…`, cert `47f31389…c155`, DB still `06C4C48E…` — `p6-w4-postinstall-20261002-013842409` |
| UI | Editorial tab renders, 0 `FATAL EXCEPTION`; project "MERCEDES FRESH PILOT" chapter 001 shows "L1 ✓ • L2 chưa chạy" with the run button; same after `am force-stop` and reopen |
| DB after opening the app | `D4B8CBA9…`: only the legacy `jobs` table differs (start-up housekeeping); editorial rows, RAW report `6c1c183b…`, receipt `f1255272…` unchanged, `editorial_phase_artifacts` 0 rows — hence M0 was re-measured before W5 |

## W5 — one live chain (UI path, run button + authorization dialog)

| Item | Value |
|---|---|
| Start | 2026-10-02 01:40:56 UTC (dialog printed exactly the D3 numbers; route OpenRouter `openai/gpt-5.6-luna`) |
| M0 before | DB `D4B8CBA9F21A5D2EA26EC4CB625E50CC17F0C62E87619226A8899F21F5596C2B`, 0 phase rows — `p6-w5-m0-20261002-014031203` |
| Rows | `L2_EDIT` COMMITTED attempt `e1a4c638…`, predecessor `7483b211…` (the committed RECONCILE); `L3_FINAL` COMMITTED attempt `867a23f8…`, predecessor `e1a4c638…`; both blobs re-hash correctly; terminal state about 90 s after the tap |
| Calls (app runtime log, provider-reported) | discovery in 21,005 / out 959 / USD 0.00640190; edit 29,580 / 1,258 / 0.00890445; blind re-audit 26,729 / 5,354 / 0.0131069; reconcile 39,705 / 3,529 / 0.0141609. **4 calls, 0 repair, 0 retry, total in 117,019 / out 11,100, USD 0.04257415** (chain cap 0.30; every call under its own cap) |
| VI_L2 | 26,466 B, SHA-256 `a7d5f99e9624a06fc0d0e10ef95ef8e92efbe7152ed63babe3915a99f7dbb30c` |
| CHANGE_MAP_L2 | 4,643 B, `4552bf11…`; 1 change (C001, line 237, CLOSED, not dialogue), 0 reverted, 0 preserved, unaccounted 0, protected-span regressions 0; `rawDiscovery`: 49 candidates (UNIT 7, TG 20, SR 14, RC 8), all PROCESSED |
| FINAL | 26,466 B, identical to VI_L2 (L3 made no change); QA receipt 2,837 B `1c44e9ff…` links the report, VI_L2, change map and FINAL hashes (all verified equal) |
| Release numbers (computed by the app) | unprocessed raw units 0, unprocessed candidates 0, proven unresolved conflicts 0, unaccounted changed anchors 0, protected-span regressions 0, preserved 0; `Stop Receipt = NONE` |
| L3 records | blind re-audit enumerated 257 candidates (UNIT 188, TG 9, SR 30, RC 20) all PROCESSED; 2 coverage + 2 regression probes, all `NO_DEFECT`; 0 QA changes |
| After | DB `E6B999D1…` (post-chain), final M0 `C1F40D14…` after the viewer/export; integrity ok; `P5E_RAW` log empty (those two phases do not use that tag); 0 `FATAL EXCEPTION` — `p6-chain-001-20261002T014041Z`, `p6-w6-final-m0-20261002-014813128` |

Result: `L3_FINAL_COMMITTED` with all release numbers 0.

## W6 — view, reopen, export

- After `am force-stop` and reopen the card shows "L1 ✓ • L2 ✓ • L3 ✓"; "Xem bản cuối" shows `sha256 a7d5f99e9624…`, 26,466 byte.
- "Xuất TXT" through the system picker (device storage → `Download`, not the owner's source folder) wrote `editorial_001_final.txt` on the phone; the app read it back; the file pulled to the host is 26,466 B with SHA-256 `A7D5F99E9624A06FC0D0E10EF95EF8E92EFBE7152ED63BABE3915A99F7DBB30C` = the stored FINAL.
- Machine check: the DRAFT had exactly one line containing Japanese characters (line 237, a leftover word); L2 replaced it, and the FINAL has 0 such lines. FINAL differs from the DRAFT in that one line only (private diff `DRAFT_to_FINAL.diff.txt`).

## Against `docs/EDITORIAL_FINAL_OUTPUT_ACCEPTANCE.md` section 4 (chapter 001)

| Criterion | Status |
|---|---|
| Manifest/identity/chain revision | met: binding `845976b3…`, run `8466b95d…`, pack `497786e1…`, chain RAW `7a5e3428…` → REPORT_L1 `7483b211…` → L2 `e1a4c638…` → L3 `867a23f8…` |
| L1 closes raw units/TG/SR/RC, preserved and proof counts | **not met as written**: REPORT_L1 is a frame (`populationTotal=1`); the ledgers exist only in the L2 discovery (49) and the L3 re-audit (257) |
| L2 final-read, actual diff, Change Map, dialogue proof, no unaccounted change | met for what the app can compute (1 non-dialogue change, 0 unaccounted); there is no separate L2 "final-read" marker |
| L3 re-audit, adversarial records, release numbers 0, receipt → FINAL | met |
| FINAL complete, saved, reopened, exported as UTF-8 with hash/count readback | met |
| Owner quality check against RAW/DRAFT | **pending** |

So chapter 001 is a **candidate** for `FINAL_OUTPUT_ACCEPTED`, not accepted: the owner has not read it, the L1 criterion is not met as written, and the FINAL differs from the DRAFT in one line out of 386, so any weakness of the draft passes through unchanged.

## Observations

1. The candidate counts do not reproduce across the passes (49 in L2 discovery vs 257 in the L3 re-audit; UNIT 7 vs 188). Each pass is internally consistent and app-counted, but nothing compares them, so "reproduce the candidate counts" is not demonstrated.
2. UX defects found on the pilot (fixed in the repository, not installed): while the chain runs the card showed the stored CLAIMED row as "dừng: RETRY_L2_CALL_STATE_UNKNOWN"; the "Đóng" button of the FINAL viewer was pushed off screen by a long chapter (Back closes it).
3. The app does not persist per-call tokens or cost in the artifact rows; the figures above come from its runtime log and are provider-reported.
