# P5E A4.3 decision atomicity / expected-environment isolation — Luna review

Date: 2026-09-28
Work package: `P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_REPAIR`
Candidate commit: `5597a3b20a4a1614c411c71872ee1333196cbe9c`
Packet: `P5E-A43-DECISION-ATOMICITY-ENV-20260928-01`

## Verdict

**PASS — 0 BLOCKER / 0 HIGH / 0 MEDIUM / 0 LOW**

The independent exact-byte review was rerun against the full candidate commit
after the closed-event guard was aligned with both immutable closed events. No
live command, ADB, device, provider, credential, database write, build,
install, RAW dispatch, retry, fallback, RECONCILE, redispatch or P6 action was
performed.

## Exact pins reviewed

| File | Length | SHA-256 |
| --- | ---: | --- |
| `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md` | 8163 | `D8F5BECE61116217E8F9C0108AE08624A16C6736A2E9DE04F4BF73E0CEEFD5DE` |
| `docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt` | 24220 | `52B961DEC64429B786970D47083F94067E739AD9A9A9D5E988B3F8C8198F246D` |
| `scripts/p5e-raw-live-supervisor.ps1` | 417314 | `4DAD6E30928DD0D78396BDDFA57AFA1053E75481524D2EA05659227869C57444` |
| `scripts/p5e-a43-runtime-guards.ps1` | 23180 | `C31217CDBD40F22DB9A74AFB529B1ECC33F725F485ECD462992073235F59ADD8` |
| `scripts/p5e-db-binary-export.ps1` | 52379 | `813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99` |
| `docs/P5E_SQLITE_BRIDGE.py` | 5958 | `4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111` |
| `scripts/p5e-raw-toolchain.ps1` | 9002 | `C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8` |
| `scripts/test-p5e-a43-preauth-runtime-guard-closure.ps1` | 38030 | `0F4614F6404FFC2F77233682E4ECC32C94FAA152E23FD555489DD7E9CD83AD1B` |
| `scripts/test-p5e-a43-decision-atomicity-expected-env.ps1` | 32508 | `BDD0D4D0F401EC050876A1C584762687D58F524D1FF4A6F02266ABE3BEC8F97E` |
| `scripts/test-p5e-a43-binding-tuple-matrix.ps1` | 6332 | `1E843BA4EACC89778C48E12F8FD53CF8E64B3CA892FDF6F72D172E36D372B3DB` |
| `scripts/test-p5e-a43-db-host-readback-matrix.ps1` | 15391 | `61ECE7837EB5F9EA769A37CEAB6272C507F7AD37E6EB1F9BF87262F3754EEF7D` |
| `scripts/test-p5e-a43-archive-clean-reconstruction.ps1` | 9653 | `144D0D96D5ACC4DCC9B010676CF3891514F014CA3C414C78CF2469EF1CE73444` |

The archive-clean report is
`docs/P5E_A43_ARCHIVE_CLEAN_RECONSTRUCTION_20260928.json`, length `9789`,
SHA-256 `7ED941AD44B398AE304E1420AC096A214B671077EED3FF8E5EB409964FA4875B`,
and records the full candidate commit above.

## Independent attack checks

- Decision identity is canonical and keyed by the full SHA-256 of UTF-8
  `decisionId`; same-decision cross-hash sequential and concurrent contenders
  have one winner only.
- Receipt identity is the full exact-byte SHA-256; same receipt across
  different decisions cannot create a second event, and partial marker/crash
  windows remain consumed fail-closed.
- Decision and receipt markers use `CreateNew`, flush before advancing, and
  never roll back. Event identity is derived from the decision key.
- PrepareEvent, Before, After and Verify receive `EXPECTED_ABSENT`; only the
  Dispatch/account phase receives the process-only expected value. No value is
  echoed, hashed, persisted or placed in argv.
- Process-tree containment, bounded stdout/stderr capture, process-exit
  preservation, PM-path fail-closed classification, path/reparse guards,
  secret-sentinel scans and no-retry/no-redispatch controls remain covered.
- Both immutable closed-event names are rejected by the helper, matching the
  packet manifest. The old command, old manifest, old decision and closed
  events have no fallback path.
- Clean Git archive reconstruction does not use the historical untracked QA
  scripts or cached workspace results.

## Evidence counts

| Suite | Result |
| --- | --- |
| RED fixtures | 6 reproduced; 6 corresponding GREEN controls |
| Final main QA | 21/21 PASS; PowerShell 5.1 parse 10 files; diff check PASS |
| Atomicity / expected environment | 28/28 PASS; 0 HIGH |
| Binding tuple | 262/262 PASS |
| Regression | 175/175 PASS |
| DB host readback | 56/56 PASS |
| Archive-clean | PASS from exact Git archive |

All live counters are zero: ADB, device, provider, credential, database
write, build/install, RAW and redispatch. The packet is reviewable only;
owner authorization has not been requested or consumed, no event was opened,
and P6 is not ready.

**Next action:** owner review of this offline packet only.
