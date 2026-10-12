[CmdletBinding()]
param(
    [string]$ResultPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$preflightPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'p5e-account-check-device-preflight.ps1'))
$commandPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'p5e-account-check-device-command.ps1'))
if ([string]::IsNullOrWhiteSpace($ResultPath)) {
    $ResultPath = Join-Path $repoRoot 'docs\P5E_ACCOUNT_CHECK_DEVICE_PATH_GUARD_QA_20260924.json'
}
$ResultPath = [IO.Path]::GetFullPath($ResultPath)

$assertions = 0
$cases = [ordered]@{}
$sandboxRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-account-path-guard-qa-' + [Guid]::NewGuid().ToString('N'))

function Assert-P5EPathGuardQA {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$Code
    )
    $script:assertions++
    if (-not $Condition) { throw $Code }
}

function Get-P5EPathGuardFunctionText {
    param([Parameter(Mandatory = $true)][string]$Path)
    $source = Get-Content -LiteralPath $Path -Raw -ErrorAction Stop
    $tokens = $null
    $parseErrors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile($Path, [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count -ne 0) { throw 'PATH_GUARD_SOURCE_PARSE_FAILED' }
    $functionAst = $ast.Find({
        param($node)
        $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
            $node.Name -eq 'Assert-P5EPathChainNoReparse'
    }, $true)
    if ($null -eq $functionAst) { throw 'PATH_GUARD_FUNCTION_MISSING' }
    return $source.Substring($functionAst.Extent.StartOffset, $functionAst.Extent.EndOffset - $functionAst.Extent.StartOffset)
}

function Invoke-P5EPathGuardCase {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [switch]$AllowMissingLeaf
    )
    try {
        $result = if ($AllowMissingLeaf) {
            Assert-P5EPathChainNoReparse -Path $Path -AllowMissingLeaf
        } else {
            Assert-P5EPathChainNoReparse -Path $Path
        }
        return [pscustomobject]@{ accepted = $true; error = ''; result = [string]$result }
    } catch {
        return [pscustomobject]@{ accepted = $false; error = [string]$_.Exception.Message; result = '' }
    }
}

function Get-P5EHash {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
}

try {
    New-Item -ItemType Directory -Path $sandboxRoot -ErrorAction Stop | Out-Null
    $normalRoot = Join-Path $sandboxRoot 'normal'
    $junctionTarget = Join-Path $sandboxRoot 'junction-target'
    $junctionParent = Join-Path $sandboxRoot 'parent-junction'
    $directJunction = Join-Path $sandboxRoot 'direct-junction'
    New-Item -ItemType Directory -Path $normalRoot, $junctionTarget -ErrorAction Stop | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $junctionTarget 'empty') -ErrorAction Stop | Out-Null
    Set-Content -LiteralPath (Join-Path $normalRoot 'regular.apk') -Value 'fixture' -Encoding ascii
    Set-Content -LiteralPath (Join-Path $junctionTarget 'regular.apk') -Value 'fixture' -Encoding ascii
    New-Item -ItemType Junction -Path $junctionParent -Target $junctionTarget -ErrorAction Stop | Out-Null
    New-Item -ItemType Junction -Path $directJunction -Target $junctionTarget -ErrorAction Stop | Out-Null

    $sourceEntries = @(
        [pscustomobject]@{ name = 'preflight'; path = $preflightPath; reparseError = 'ACCOUNT_CHECK_PREFLIGHT_REPARSE_PATH_STOP' },
        [pscustomobject]@{ name = 'command'; path = $commandPath; reparseError = 'ACCOUNT_CHECK_DEVICE_COMMAND_REPARSE_PATH_STOP' }
    )
    foreach ($entry in $sourceEntries) {
        if (Test-Path -LiteralPath 'Function:\Assert-P5EPathChainNoReparse') {
            Remove-Item -LiteralPath 'Function:\Assert-P5EPathChainNoReparse' -Force
        }
        Invoke-Expression (Get-P5EPathGuardFunctionText -Path $entry.path)

        $normalFile = Invoke-P5EPathGuardCase -Path (Join-Path $normalRoot 'regular.apk')
        Assert-P5EPathGuardQA -Condition ($normalFile.accepted -and $normalFile.error -eq '') -Code ($entry.name + '_NORMAL_CONTROL_REJECTED')

        $junctionFile = Invoke-P5EPathGuardCase -Path (Join-Path $junctionParent 'regular.apk')
        Assert-P5EPathGuardQA -Condition (-not $junctionFile.accepted -and $junctionFile.error -eq $entry.reparseError) -Code ($entry.name + '_PARENT_REPARSE_FILE_NOT_REJECTED')

        $junctionEmpty = Invoke-P5EPathGuardCase -Path (Join-Path $junctionParent 'empty')
        Assert-P5EPathGuardQA -Condition (-not $junctionEmpty.accepted -and $junctionEmpty.error -eq $entry.reparseError) -Code ($entry.name + '_PARENT_REPARSE_DIRECTORY_NOT_REJECTED')

        $direct = Invoke-P5EPathGuardCase -Path $directJunction
        Assert-P5EPathGuardQA -Condition (-not $direct.accepted -and $direct.error -eq $entry.reparseError) -Code ($entry.name + '_DIRECT_REPARSE_NOT_REJECTED')

        $missingUnderJunction = Invoke-P5EPathGuardCase -Path (Join-Path $junctionParent 'missing.apk') -AllowMissingLeaf
        Assert-P5EPathGuardQA -Condition (-not $missingUnderJunction.accepted -and $missingUnderJunction.error -eq $entry.reparseError) -Code ($entry.name + '_MISSING_LEAF_PARENT_REPARSE_NOT_REJECTED')

        $missingNormal = Invoke-P5EPathGuardCase -Path (Join-Path $normalRoot 'missing.apk') -AllowMissingLeaf
        Assert-P5EPathGuardQA -Condition ($missingNormal.accepted -and $missingNormal.error -eq '') -Code ($entry.name + '_ALLOWED_MISSING_NORMAL_LEAF_REJECTED')
        $cases[$entry.name + '_ancestor_walk'] = 'PASS'
    }

    $result = [ordered]@{
        schemaVersion = 'p5e.account-check.device-path-guard.qa.v1'
        status = 'PASS'
        totalAssertions = $assertions
        passedAssertions = $assertions
        failures = @()
        source = [ordered]@{
            preflightSha256 = Get-P5EHash -Path $preflightPath
            commandSha256 = Get-P5EHash -Path $commandPath
            qaScriptSha256 = Get-P5EHash -Path $MyInvocation.MyCommand.Path
        }
        host = [ordered]@{
            expectedHost = 'Windows PowerShell 5.1'
            actualHost = (& powershell.exe -NoLogo -NoProfile -NonInteractive -Command '$PSVersionTable.PSVersion.ToString()').Trim()
            actualHostProcess = $PSVersionTable.PSVersion.ToString()
        }
        cases = $cases
        syntheticOnly = $true
        sourceFunctionExecuted = $true
        projectFilesystemMutations = 0
        sandboxFilesystemMutations = $true
        adbInvocations = 0
        expectedReads = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
    }
    $parent = Split-Path -Parent $ResultPath
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
        New-Item -ItemType Directory -Path $parent -ErrorAction Stop | Out-Null
    }
    [IO.File]::WriteAllText($ResultPath, ($result | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PATH_GUARD_QA=PASS'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PATH_GUARD_QA_ASSERTIONS=' + $assertions + '/' + $assertions)
} catch {
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PATH_GUARD_QA=FAIL'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PATH_GUARD_QA_ERROR=' + [string]$_.Exception.Message)
    exit 1
} finally {
    if (Test-Path -LiteralPath $sandboxRoot) {
        Remove-Item -LiteralPath $sandboxRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
