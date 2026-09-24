# P5E account-check device repair — post-STOP disposition

Date: 2026-09-24, Asia/Saigon  
Branch: `feature/v4.18-p5e-runner-repair-20260917`  
HEAD baseline: `8e6b16b650905fabf88b5023b4ec043741c39e42`  
Status: `LOCAL_REPAIR_GREEN / OWNER_EVENT_DECISION_PENDING / ACCOUNT_CHECK_NOT_EXECUTED / A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`

## Disposition

The post-STOP local work package is complete. The demonstrated parent-reparse
bug was fixed in both host scripts. Package metadata parsing is now scoped to a
single qualified package section, separates absent/missing/ambiguous/invalid/
unsupported metadata with typed enum/count fields, and rejects valid stdout
when stderr is nonempty. No raw metadata is written to a receipt.

The owner event remains closed at `PRODUCTION_PACKAGE` after two successful
read-only ADB child calls. Its receipts and hashes are preserved and were not
read for secrets, changed, retried, renamed or reused. It did not pull APKs,
launch the account runner, call a provider, write a database, dispatch RAW,
install, uninstall, clear data, downgrade or build.

The exact current source pins, QA bindings, unused proposed evidence directory
and one-time owner command are in:

- `docs/P5E_POST_STOP_EVENT_PACKET_20260924.md`
- `docs/P5E_ACCOUNT_CHECK_DEVICE_OWNER_COMMAND_20260924.txt`

Those files are a proposal only; they do not authorize a device event.

## Final source pins

| Input | SHA-256 |
| --- | --- |
| `scripts/p5e-account-check-device-preflight.ps1` | `A82FAE2232C9F5C579CBC2D012B03EB55CCFEB567DAA399A89DEBC661BD235D5` |
| `scripts/p5e-account-check-device-command.ps1` | `C09D66D8C576EF8EAD9C8EE86E87F7CB1A22B591A8BF01D207FCA8228190B4C0` |
| `scripts/test-p5e-account-check-device-preflight.ps1` | `A84BC662D0942B453E9E887868E0224E03575996BD0B5AE10EE4DED1DF0E4B51` |
| `scripts/test-p5e-account-check-device-command.ps1` | `9EA40F3F3601A62DD68B72299E9F2660FABB7BCA6405E7C430E5C7BBDFA9A61D` |
| `scripts/test-p5e-account-check-device-path-guard.ps1` | `A52886FE154ACC1D1905CE916A813B5F0F03F96ECB1F3D24AF475CEC83395772` |
| `scripts/p5e-load-expected-digest.ps1` | `1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D` |
| `scripts/p5e-account-check.ps1` | `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65` |
| `scripts/p5e-raw-live-supervisor.ps1` | `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34` |
| account AndroidTest source | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` |
| account APK reference, historical pull-back | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |
| account APK certificate | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |

## Offline evidence

All post-STOP suites used synthetic tools or a temporary filesystem sandbox on
Windows PowerShell `5.1.26100.9168`; no real ADB was invoked:

| Evidence | Result | SHA-256 |
| --- | ---: | --- |
| `docs/P5E_EXPECTED_VALUE_LOADER_QA_20260924.json` | `27/27` | `933A7780700F3562106992BFF3F801B841AB09227C4A2664D192685541914E21` |
| `docs/P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA_20260924_POST_STOP.json` | `133/133` | `431E8D60120604279CA651592B4A7BB4D553C10CBBC810E698373F415F84D15C` |
| `docs/P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA_20260924_POST_STOP.json` | `26/26` | `4DC62B28D79CBD970FCE79FC006423E0688EF2264A07B1AB9A31803C02B452F3` |
| `docs/P5E_ACCOUNT_CHECK_DEVICE_PATH_GUARD_QA_20260924.json` | `12/12` | `7AAC21254FA13BAD3D760E099EB483E4AB29E947CB9474735F1F715DE3680080` |

The affected QA reports `adbInvocations=0`, `providerCalls=0`, `dbWrites=0`
and `rawDispatches=0`. The path suite executed both actual source guard
functions against regular files, empty directories, direct junctions and
missing leaves under junctions; only its temporary sandbox was mutated.

The old `71/71` preflight and `22/22` command JSON files remain historical
inputs. They are not overwritten, relabeled as current, or used as device
authorization.

## Owner/event boundary

The old event directory is:

`D:\P5E-private\p5e-account-check-device-event-20260924-loader-contract-fix-01`

Its redacted STOP evidence hash is
`D55BEDD6521FF322BB9E9EAD9AB7FABE2EC2E60D867ED60AB3E02D8FCCAA5976`; the
private preflight and command receipt hashes are
`36793B526B63885215687A5C11074E287F980F3D960B5C9421CF4A354BFE468F` and
`97C33593B9D188BABD6E87C7401BF35FD1D8AA04C9EA3BB2AD3159203388FC4A`.
No raw dumpsys was retained.

The only next action is owner review of the exact proposed packet. If the owner
chooses not to authorize a new event, keep `ACCOUNT_CHECK_NOT_EXECUTED`. If the
owner later authorizes the packet, use its unused evidence directory and its
one-time command in the same owner PowerShell window (with its explicit child
inheritance), then return only typed outcome plus a redacted receipt path. Do
not retry after a STOP, change settings, rotate a key, reinstall, run a manual
ADB diagnostic, call RAW/provider/DB, issue A4.3 or claim P5/P6.
