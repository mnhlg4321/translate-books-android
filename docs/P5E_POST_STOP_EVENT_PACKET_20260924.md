# P5E post-STOP event packet — CLOSED HISTORICAL

> HISTORICAL CLOSED EVENT — 2026-09-24: this packet describes the event that stopped at `PRODUCTION_PACKAGE / UNSUPPORTED_LAYOUT`. It was used and closed; it is not an unused proposal. Do not execute, reuse, rename or delete its command/evidence. The current local adapter packet is `P5E_SIGNATURE_LAYOUT_ADAPTER_EVENT_PACKET_20260924.md`.
> The source-derived adapter is now locally qualified by synthetic preflight `189/189` and command `28/28`; this historical packet is retained only for provenance. Account has no result; A4.3/RAW/P5 exit/P6 remain closed.


Date: 2026-09-24, Asia/Saigon  
Branch: `feature/v4.18-p5e-runner-repair-20260917`  
HEAD baseline before this repair group: `8e6b16b650905fabf88b5023b4ec043741c39e42`
Implementation commit: `d4c9a74f` (`fix(p5e): repair post-stop host guards and QA`)
Working tree: dirty only because protected owner/IDE/worktree material remains; the hashes below bind the committed P5E repair bytes.

## Decision boundary

`CLOSED_EVENT_HISTORICAL / ACCOUNT_CHECK_NOT_EXECUTED / A4.3_NOT_ISSUED / RAW_NOT_RUN / P5_EXIT_NOT_CLAIMED / P6_NOT_READY`.

This packet is a proposal for one possible account-only event. It is not owner
approval, A4.3 authorization, RAW authorization, release authorization or P6
readiness. The closed event remains terminal and must not be reused:

`D:\P5E-private\p5e-account-check-device-event-20260924-loader-contract-fix-01`

The directory below is the historical event directory that was used and closed;
it must not be reused:

`D:\P5E-private\p5e-account-check-device-event-20260924-post-stop-proposed-01`

Do not invoke this historical command. Any future event requires the separate
current packet and a new explicit owner decision.

## Local repair and QA disposition

The local finding was repaired in both host scripts: path-chain validation now
walks every existing ancestor to the root, including when the leaf is allowed
to be missing. A parent reparse point remains a typed STOP. Metadata parsing is
now package-section scoped and reports only enum/bool/count fields in receipts;
raw dumpsys/stdout/stderr is not retained in evidence. A valid stdout result is
rejected when the tool also emits stderr.

The final offline evidence is synthetic only and was executed with Windows
PowerShell `5.1.26100.9168`:

| Scope | Result | Evidence SHA-256 |
| --- | ---: | --- |
| loader regression (historical qualified input) | `27/27` | `933A7780700F3562106992BFF3F801B841AB09227C4A2664D192685541914E21` |
| preflight/parser post-STOP suite | `133/133` | `431E8D60120604279CA651592B4A7BB4D553C10CBBC810E698373F415F84D15C` |
| command/orchestration post-STOP suite | `26/26` | `4DC62B28D79CBD970FCE79FC006423E0688EF2264A07B1AB9A31803C02B452F3` |
| path guard, both source functions with sandbox junctions | `12/12` | `7AAC21254FA13BAD3D760E099EB483E4AB29E947CB9474735F1F715DE3680080` |

The post-STOP suites report `adbInvocations=0`, `providerCalls=0`,
`dbWrites=0` and `rawDispatches=0`. The path suite mutated only its temporary
sandbox; project filesystem mutations were `0`. The old `71/71` and `22/22`
JSON files remain historical evidence and are not overwritten or reinterpreted.

## Exact source pins

| Input | SHA-256 |
| --- | --- |
| `scripts/p5e-account-check-device-command.ps1` | `C09D66D8C576EF8EAD9C8EE86E87F7CB1A22B591A8BF01D207FCA8228190B4C0` |
| `scripts/p5e-account-check-device-preflight.ps1` | `A82FAE2232C9F5C579CBC2D012B03EB55CCFEB567DAA399A89DEBC661BD235D5` |
| `scripts/p5e-load-expected-digest.ps1` | `1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D` |
| `scripts/p5e-account-check.ps1` | `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65` |
| `scripts/p5e-raw-live-supervisor.ps1` | `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34` |
| `app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EAccountCheckOnlyInstrumentedTest.java` | `2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C` |
| account test APK reference, historical pull-back only | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |
| account test certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |

The command self-checks the dependency pins before evidence initialization and
before any child that could contact a device. The owner command also checks the
command hash externally; it must not accept a QA JSON file as a substitute for
the source hash.

## Runtime contract, if separately approved

- Serial is exactly `15e84958`.
- Production package is `com.ml.tblandroidtxt`, expected code `207`, APK hash
  `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD`, and
  certificate hash `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155`.
- Test package is `com.ml.tblandroidtxt.test`; it uses the separate account APK
  pin above, never the RAW/A4 `57EC…` artifact.
- The preflight allows at most one seven-call read-only chain: get-state,
  production/test package metadata, package paths and pulls in source order.
  It does not install, uninstall, clear data, downgrade or mutate a package.
- If preflight passes, the command may create one attempt marker and launch at
  most one exact account-only instrumentation child. Account child timeout is
  `135000 ms` at the host wrapper; its pinned instrumentation supervisor is
  `120000 ms`. Preflight child timeout is `360000 ms`, each device read is at
  most `30000 ms`, and each pull is at most `60000 ms`; capture and cleanup
  bounds are recorded in the receipts.
- The expected fingerprint is checked only for presence/shape in the owner
  Process environment. The preflight PowerShell child and its ADB/apksigner
  descendants scrub that variable. The account runner child intentionally
  inherits it for its approved process-only transport. The raw key never
  traverses the host.
- `MATCH` means only the source-defined route predicate and digest equality;
  it is not provider account validity, billing proof, A4.3 issuance, P5 exit or
  P6 readiness. `MISMATCH` is terminal and is not a diagnosis that the key is
  wrong. Timeout, missing/duplicate result, terminal failure, nonzero exit or
  redaction failure is `NOT_PROVEN` and is not retried.

## Owner response and output boundary

If the owner accepts the packet, respond with the exact one-event decision and
then run the command in the same PowerShell window that holds the canonical
Process value, using the command in
`docs/P5E_ACCOUNT_CHECK_DEVICE_OWNER_COMMAND_20260924.txt`. Return only the
typed command outcome and the redacted command receipt path. Do not return a
key, endpoint, digest, raw dumpsys, instrumentation stream, logcat, screenshot
or environment dump.

After any outcome, A4.3, RAW, readback, P5 exit and P6 remain separate gates.
