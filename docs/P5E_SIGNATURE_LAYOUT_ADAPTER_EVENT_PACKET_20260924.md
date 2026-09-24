# P5E account-only event packet — signature-layout adapter completion

Status: `PACKET_READY_FOR_OWNER_DECISION / NOT_EXECUTED`.

This packet is a proposal only. It does not authorize a device operation. The
event that stopped at `PRODUCTION_PACKAGE` remains closed and must not be
reused, renamed or deleted.

## Exact local pins

| Item | Value |
| --- | --- |
| Branch | `feature/v4.18-p5e-runner-repair-20260917` |
| Implementation baseline before this package | `bf97cd410ea63fe7f6d978b58324584249995e60` |
| Adapter implementation commit | `3d8ab021a8acea3f769e0a2d1695286f4618f29a` (`fix(p5e): add source-grounded signature layout adapter`) |
| Serial | `15e84958` |
| Production package/code | `com.ml.tblandroidtxt` / `207` |
| Test package | `com.ml.tblandroidtxt.test` |
| Production APK SHA-256 | `2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD` |
| Account test APK SHA-256 | `058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8` |
| APK certificate SHA-256 | `47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155` |
| Loader | `scripts/p5e-load-expected-digest.ps1` / `1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D` |
| Runner | `scripts/p5e-account-check.ps1` / `96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65` |
| Helper | `scripts/p5e-raw-live-supervisor.ps1` / `5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34` |
| Preflight | `scripts/p5e-account-check-device-preflight.ps1` / `09A33DC74820A1AA18B3EE47AA96862DC0AFA03067D5B46B433B74E4EB1AFA22` |
| Command | `scripts/p5e-account-check-device-command.ps1` / `B79A6C7AD05DE30249DEBAA6C3E1E4FFBD0326754CFD3F14AFC0B69E265F1298` |
| Preflight QA | `docs/P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA_20260924_LAYOUT.json` / `86A13ADBB27AABAC3D409DCBE65B47FDCDB8DF00CCD10DE992DB4B14ECC67E50` / `189/189` |
| Command QA | `docs/P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA_20260924_LAYOUT.json` / `EB8A9BBA81F1F08419810B52C9BFBDACE4AE2669E0C52C1AA5B963A100597895` / `28/28` |

## New evidence directory

`D:\P5E-private\p5e-account-check-device-event-20260924-signature-layout-adapter-01`

State: `UNUSED_NOT_CREATED`. It is not the closed event directory and must
remain uncreated until the owner makes a separate decision.

## Proposed one-time owner command

Run only after the owner explicitly approves this exact packet, from the same
Windows PowerShell process that contains the qualified Process-only expected
value. The command itself checks presence/shape without printing the value.

```powershell
Set-Location -LiteralPath 'D:\App Translate Books'
$eventEvidence = 'D:\P5E-private\p5e-account-check-device-event-20260924-signature-layout-adapter-01'
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File '.\scripts\p5e-account-check-device-command.ps1' -EvidenceDirectory $eventEvidence
$eventExitCode = $LASTEXITCODE
Write-Host "P5E_ACCOUNT_CHECK_DEVICE_COMMAND_EXIT_CODE=$eventExitCode"
```

Do not use the script default because it points to the closed historical
directory. Do not pass the expected value as an argument, file, clipboard
content or transcript. Do not run the loader in a child PowerShell and expect
the parent to inherit it; if the value is absent when the packet is approved,
reload it in this same owner window with the qualified loader before the one
command.

## Boundaries and typed outcomes

- At most one seven-call read-only preflight, then at most one account-only
  runner; any STOP closes the event and is not retried.
- No install, uninstall, clear-data, downgrade, build, provider call, DB write,
  RAW dispatch, manual ADB diagnostic or reconnect loop.
- `MATCH` means only the qualified route/digest equality predicate. It does
  not issue A4.3, authorize RAW, claim P5 exit or make P6 ready.
- `MISMATCH` stops for owner provenance/mapping review; it is not proof that a
  key is wrong.
- Timeout, missing/duplicate result, nonzero exit, terminal failure or missing
  receipt is `NOT_PROVEN`; do not retry.

After a decision, return only the typed outcome and receipt path. Do not send
key, endpoint, digest, raw dumps, logcat or command transcript.
