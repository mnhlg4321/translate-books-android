[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$LauncherPath,
    [Parameter(Mandatory = $true)][string]$ExpectedContractSha256,
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-P5EStaticUtf8 {
    param([Parameter(Mandatory = $true)][string]$Path,[Parameter(Mandatory = $true)][string]$Text)
    $parent = Split-Path -Parent ([IO.Path]::GetFullPath($Path))
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) { [void](New-Item -ItemType Directory -Path $parent) }
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($Path), $Text, (New-Object System.Text.UTF8Encoding($false)))
}

$launcherFullPath = [IO.Path]::GetFullPath($LauncherPath)
if (-not (Test-Path -LiteralPath $launcherFullPath -PathType Leaf)) { throw 'P5E_LAUNCHER_STATIC_MISSING_STOP' }
$launcherItem = Get-Item -LiteralPath $launcherFullPath -Force
if (($launcherItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'P5E_LAUNCHER_STATIC_REPARSE_STOP' }
$launcherText = Get-Content -Raw -LiteralPath $launcherFullPath
$launcherHash = (Get-FileHash -LiteralPath $launcherFullPath -Algorithm SHA256).Hash.ToUpperInvariant()
$contractPath = Join-Path ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))) 'scripts\p5e-child-invocation-contract.ps1'
$contractHash = (Get-FileHash -LiteralPath $contractPath -Algorithm SHA256).Hash.ToUpperInvariant()
if ($contractHash -cne $ExpectedContractSha256.ToUpperInvariant()) { throw 'P5E_LAUNCHER_STATIC_CONTRACT_HASH_MISMATCH_STOP' }

$tokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile($launcherFullPath, [ref]$tokens, [ref]$parseErrors) | Out-Null
$checks = New-Object 'System.Collections.Generic.List[object]'
$failureCount = 0
function Add-P5EStaticCheck {
    param([Parameter(Mandatory = $true)][string]$Name,[Parameter(Mandatory = $true)][bool]$Passed,[Parameter(Mandatory = $true)][string]$Evidence)
    [void]$script:checks.Add([pscustomobject]@{ Name = $Name; Passed = $Passed; Evidence = $Evidence })
    if (-not $Passed) { $script:failureCount++ }
}

Add-P5EStaticCheck -Name 'powershell-5.1-parse' -Passed ($parseErrors.Count -eq 0) -Evidence ('errors=' + [string]$parseErrors.Count)
Add-P5EStaticCheck -Name 'exact-contract-hash' -Passed ($contractHash -cne '' -and $contractHash -cne ('0' * 64)) -Evidence $contractHash
Add-P5EStaticCheck -Name 'approval-prompt-visible' -Passed ($launcherText.Contains("Read-Host -Prompt 'Type APPROVE_ONE_FRESH_EVENT exactly'")) -Evidence 'visible approval prompt literal present'
Add-P5EStaticCheck -Name 'key-prompt-hidden' -Passed ($launcherText.Contains("Read-Host -Prompt 'Enter full OpenRouter API key (hidden)' -AsSecureString")) -Evidence 'secure key prompt literal present'
Add-P5EStaticCheck -Name 'tracked-invocation-source-used' -Passed ($launcherText.Contains('Invoke-P5EChildInvocation') -and $launcherText.Contains('p5e-child-invocation-contract.ps1')) -Evidence 'ProcessStartInfo contract call present'
Add-P5EStaticCheck -Name 'expiry-checked-before-child' -Passed ($launcherText.Contains('Test-P5EReceiptExpiryContract') -and $launcherText.IndexOf('Test-P5EReceiptExpiryContract', [StringComparison]::Ordinal) -lt $launcherText.IndexOf('Invoke-P5EChildInvocation', [StringComparison]::Ordinal)) -Evidence 'expiry gate precedes child invocation'
Add-P5EStaticCheck -Name 'raw-command-copy' -Passed ($launcherText.Contains('Write-P5ECreateNewBytes -Path $commandCopyPath -Bytes $commandBytes')) -Evidence 'command bytes copied without text round-trip'
Add-P5EStaticCheck -Name 'single-use-create-new' -Passed ($launcherText.Contains('[IO.FileMode]::CreateNew') -and $launcherText.Contains('RESERVED_CONSUMED_FAIL_CLOSED')) -Evidence 'CreateNew marker and monotonic consumed state present'
Add-P5EStaticCheck -Name 'outer-nonzero-propagation' -Passed ($launcherText.Contains('$childResult.OuterExitCode -ne 0') -and $launcherText.Contains('exit 1')) -Evidence 'exception/child nonzero reaches launcher nonzero exit'
Add-P5EStaticCheck -Name 'bindings-cleared-finally' -Passed ($launcherText.Contains('finally') -and $launcherText.Contains('Clear-P5EProcessBindings')) -Evidence 'Process bindings clear in finally'
Add-P5EStaticCheck -Name 'safe-counter-classification' -Passed ($launcherText.Contains('NOT_EVALUATED_BY_OUTER_LAUNCHER') -and -not $launcherText.Contains('providerCalls = 0') -and -not $launcherText.Contains('databaseWrites = 0') -and -not $launcherText.Contains('rawDispatches = 0')) -Evidence 'outer metadata does not claim unobserved live outcomes'
Add-P5EStaticCheck -Name 'no-native-operator-wrapper' -Passed (-not $launcherText.Contains('& $powershellPath') -and -not $launcherText.Contains('ReadToEndAsync') -and -not $launcherText.Contains('Start-Process')) -Evidence 'no stderr-promoting operator or unbounded read or uncontrolled fallback'
Add-P5EStaticCheck -Name 'old-authority-not-reused' -Passed (-not $launcherText.Contains('P5E_A43_FINAL_OWNER_SINGLE_USE_LAUNCHER_20260929_MANUAL') -and -not $launcherText.Contains('0BDAD3992453ADABCBA0EEF5EBA716F5206112F0911E0031F0AE4772956AB5CF')) -Evidence 'old launcher/receipt identifiers absent'

$result = if ($failureCount -eq 0) { 'PASS' } else { 'FAIL' }
$report = [ordered]@{}
$report['schema'] = 'p5e.a43.child-safe-launcher-static-qa.v1'
$report['result'] = $result
$report['failureCount'] = $failureCount
$report['launcherPath'] = $launcherFullPath
$report['launcherSha256'] = $launcherHash
$report['contractPath'] = 'scripts/p5e-child-invocation-contract.ps1'
$report['contractSha256'] = $contractHash
$report['executed'] = $false
$report['liveBoundariesCalled'] = $false
$report['checks'] = @($checks.ToArray())
$json = $report | ConvertTo-Json -Depth 8
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) { Write-P5EStaticUtf8 -Path $OutputPath -Text $json }
Write-Output $json
if ($failureCount -ne 0) { exit 1 }
exit 0
