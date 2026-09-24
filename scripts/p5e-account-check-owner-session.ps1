[CmdletBinding()]
param(
    [string]$RepoRoot = 'D:\App Translate Books',
    [string]$EventEvidence = 'D:\P5E-private\p5e-account-check-device-event-20260924-identity-lifecycle-01'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Set-Location -LiteralPath $RepoRoot

$commandFile = Join-Path $RepoRoot 'scripts\p5e-account-check-device-command.ps1'
$loaderFile = Join-Path $RepoRoot 'scripts\p5e-load-expected-digest.ps1'
$expectedCommandHash = '9B64078D5A2A6EA2C3A9618C4B1F8CDBB01D3CCF6C3120A2D7D4AFFF3F5E6331'
$expectedLoaderHash = '1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D'
$accountEnvironmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$hostPowerShell = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'

function Assert-OwnerSessionHash {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
    if ($actual -cne $Expected) {
        throw ($Name + '_HASH_MISMATCH_STOP')
    }
}

Assert-OwnerSessionHash -Path $commandFile -Expected $expectedCommandHash -Name 'OWNER_COMMAND'
Assert-OwnerSessionHash -Path $loaderFile -Expected $expectedLoaderHash -Name 'OWNER_LOADER'
if (-not (Test-Path -LiteralPath $hostPowerShell -PathType Leaf)) {
    throw 'OWNER_POWERSHELL_MISSING_STOP'
}
if (Test-Path -LiteralPath $EventEvidence) {
    throw 'OWNER_EVENT_PATH_ALREADY_EXISTS_STOP'
}

Write-Host 'Run the pinned loader in this PowerShell. Enter the key only at the hidden prompt.'
& $loaderFile

$expectedProbe = [Environment]::GetEnvironmentVariable($accountEnvironmentName, 'Process')
if ([string]::IsNullOrWhiteSpace($expectedProbe)) {
    throw 'EXPECTED_LOAD_OWNER_REPORTED_PROCESS_MISSING_STOP'
}
if ($expectedProbe -notmatch '^[0-9a-fA-F]{64}$') {
    throw 'EXPECTED_LOAD_OWNER_REPORTED_PROCESS_SHAPE_STOP'
}
$expectedProbe = $null

if (Test-Path -LiteralPath $EventEvidence) {
    throw 'OWNER_EVENT_PATH_ALREADY_EXISTS_STOP'
}

Write-Host 'Expected value loaded in this Process. Starting the approved command once.'
& $hostPowerShell -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $commandFile -EvidenceDirectory $EventEvidence
$eventExitCode = $LASTEXITCODE
Write-Host "P5E_ACCOUNT_CHECK_DEVICE_COMMAND_EXIT_CODE=$eventExitCode"
exit $eventExitCode
