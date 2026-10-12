[CmdletBinding()]
param(
    [string]$OutputPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $scriptRoot = if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
        $PSScriptRoot
    } else {
        Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    $OutputPath = Join-Path $scriptRoot '..\docs\P5E_ACCOUNT_IDENTITY_LIFECYCLE_QA_20260924.json'
}

# Offline contract tests for the Android instrumentation stream.  The real
# runner emits a start status (code 1), a separate account result status
# (code 0), and a completion status (code 0) before the final summary/code.
# This file deliberately never invokes adb, a device, an APK, or a provider.
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$expectedClassMethod = 'com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest#ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch'
$expectedClass = $expectedClassMethod.Split('#')[0]
$expectedMethod = $expectedClassMethod.Split('#')[1]
$results = [System.Collections.Generic.List[object]]::new()

function Assert-QA {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Condition
    )
    [void]$script:results.Add([ordered]@{ name = $Name; passed = $Condition })
    if (-not $Condition) { throw ('QA_ASSERTION_FAILED:' + $Name) }
}

function New-ParserModule {
    param([Parameter(Mandatory = $true)][string]$Path)
    $module = New-Module -Name ('P5EAccountIdentityLifecycleQA_' + [Guid]::NewGuid().ToString('N')) -ScriptBlock {
        param([Parameter(Mandatory = $true)][string]$HelperFile)
        . $HelperFile -LibraryOnly
        Export-ModuleMember -Function 'Parse-P5EAccountCheckInstrumentationOutput'
    } -ArgumentList $Path
    Import-Module $module -Force | Out-Null
    return $module
}

function New-LifecycleOutput {
    param(
        [ValidateSet('MATCH', 'MISMATCH')][string]$Result = 'MATCH',
        [string]$Class = $expectedClass,
        [string]$Method = $expectedMethod
    )
    @(
        "INSTRUMENTATION_STATUS: class=$Class",
        "INSTRUMENTATION_STATUS: test=$Method",
        'INSTRUMENTATION_STATUS: numtests=1',
        'INSTRUMENTATION_STATUS_CODE: 1',
        "INSTRUMENTATION_STATUS: p5e.account.result=$Result",
        'INSTRUMENTATION_STATUS_CODE: 0',
        "INSTRUMENTATION_STATUS: class=$Class",
        "INSTRUMENTATION_STATUS: test=$Method",
        'INSTRUMENTATION_STATUS: numtests=1',
        'INSTRUMENTATION_STATUS_CODE: 0',
        'INSTRUMENTATION_RESULT: stream=',
        'OK (1 test)',
        'INSTRUMENTATION_CODE: -1'
    ) -join "`n"
}

function Parse-Case {
    param([Parameter(Mandatory = $true)][string]$Text)
    return Parse-P5EAccountCheckInstrumentationOutput -Output $Text -ExpectedClassMethod $expectedClassMethod
}

$module = $null
$report = $null
try {
    $module = New-ParserModule -Path $helperPath

    $matchText = New-LifecycleOutput -Result MATCH
    $mismatchText = New-LifecycleOutput -Result MISMATCH
    $match = Parse-Case -Text $matchText
    $mismatch = Parse-Case -Text $mismatchText

    Assert-QA 'normal-match-full-lifecycle-accepted' ([bool]($match.Accepted -and $match.Result -ceq 'MATCH' -and $match.IdentityPass -and $match.TerminalSuccess -and $match.TestFinished))
    Assert-QA 'normal-mismatch-full-lifecycle-accepted' ([bool]($mismatch.Accepted -and $mismatch.Result -ceq 'MISMATCH' -and $mismatch.IdentityPass -and $mismatch.TerminalSuccess -and $mismatch.TestFinished))

    $cases = [ordered]@{}
    $cases['abbreviated-single-identity-rejected'] = @(
        "INSTRUMENTATION_STATUS: class=$expectedClass",
        "INSTRUMENTATION_STATUS: test=$expectedMethod",
        'INSTRUMENTATION_STATUS: p5e.account.result=MATCH',
        'INSTRUMENTATION_STATUS_CODE: 0',
        'OK (1 test)',
        'INSTRUMENTATION_CODE: -1'
    ) -join "`n"
    $cases['conflicting-class-rejected'] = $matchText.Replace("class=$expectedClass`nINSTRUMENTATION_STATUS: test=$expectedMethod`nINSTRUMENTATION_STATUS: numtests=1`nINSTRUMENTATION_STATUS_CODE: 0", "class=$expectedClass`nINSTRUMENTATION_STATUS: test=$expectedMethod`nINSTRUMENTATION_STATUS: numtests=1`nINSTRUMENTATION_STATUS_CODE: 0`nINSTRUMENTATION_STATUS: class=other.Class")
    $cases['conflicting-method-rejected'] = $matchText.Replace("class=$expectedClass`nINSTRUMENTATION_STATUS: test=$expectedMethod`nINSTRUMENTATION_STATUS: numtests=1`nINSTRUMENTATION_STATUS_CODE: 0", "class=$expectedClass`nINSTRUMENTATION_STATUS: test=$expectedMethod`nINSTRUMENTATION_STATUS: numtests=1`nINSTRUMENTATION_STATUS_CODE: 0`nINSTRUMENTATION_STATUS: test=otherMethod")
    $cases['duplicate-start-identity-rejected'] = $matchText.Replace('INSTRUMENTATION_STATUS_CODE: 1', "INSTRUMENTATION_STATUS_CODE: 1`nINSTRUMENTATION_STATUS: class=$expectedClass`nINSTRUMENTATION_STATUS: test=$expectedMethod")
    $cases['missing-start-class-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS: class=$expectedClass`n", '')
    $cases['missing-start-method-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS: test=$expectedMethod`n", '')
    $cases['missing-result-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS: p5e.account.result=MATCH`n", '')
    $cases['duplicate-result-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS_CODE: 0`nINSTRUMENTATION_STATUS: class=$expectedClass", "INSTRUMENTATION_STATUS_CODE: 0`nINSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_STATUS: class=$expectedClass")
    $cases['result-before-start-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS: class=$expectedClass`n", '').Replace("INSTRUMENTATION_STATUS: p5e.account.result=MATCH`n", "INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_STATUS: class=$expectedClass`n")
    $cases['completion-before-result-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_STATUS_CODE: 0`nINSTRUMENTATION_STATUS: class=$expectedClass", "INSTRUMENTATION_STATUS_CODE: 0`nINSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_STATUS: class=$expectedClass")
    $completionBlock = "INSTRUMENTATION_STATUS: class=$expectedClass`nINSTRUMENTATION_STATUS: test=$expectedMethod`nINSTRUMENTATION_STATUS: numtests=1`nINSTRUMENTATION_STATUS_CODE: 0`n"
    $cases['missing-completion-status-rejected'] = $matchText.Replace($completionBlock, '')
    $cases['wrong-start-status-code-rejected'] = $matchText.Replace('INSTRUMENTATION_STATUS_CODE: 1', 'INSTRUMENTATION_STATUS_CODE: 0')
    $cases['status-failure-rejected'] = $matchText.Replace('INSTRUMENTATION_STATUS_CODE: 1', 'INSTRUMENTATION_STATUS_CODE: -1')
    $cases['missing-terminal-code-rejected'] = $matchText.Replace("`nINSTRUMENTATION_CODE: -1", '')
    $cases['terminal-before-summary-rejected'] = $matchText.Replace("OK (1 test)`nINSTRUMENTATION_CODE: -1", "INSTRUMENTATION_CODE: -1`nOK (1 test)")
    $cases['result-after-terminal-rejected'] = $matchText + "`nINSTRUMENTATION_RESULT: stream=late"
    $cases['nonterminal-code-rejected'] = $matchText.Replace('INSTRUMENTATION_CODE: -1', 'INSTRUMENTATION_CODE: 0')
    $cases['failure-marker-rejected'] = $matchText.Replace('INSTRUMENTATION_CODE: -1', "FAILURES!!!`nINSTRUMENTATION_CODE: -1")
    $cases['truncated-after-result-rejected'] = "INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_STATUS_CODE: 0"
    $cases['result-suffix-rejected'] = $matchText.Replace('p5e.account.result=MATCH', 'p5e.account.result=MATCH_EXTRA')
    $cases['result-conflict-rejected'] = $matchText.Replace("INSTRUMENTATION_STATUS: p5e.account.result=MATCH`n", "INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_STATUS: p5e.account.result=MISMATCH`n")

    foreach ($entry in $cases.GetEnumerator()) {
        $parsed = Parse-Case -Text ([string]$entry.Value)
        Assert-QA ('rejects-' + $entry.Key) (-not [bool]$parsed.Accepted)
    }

    # RED->GREEN proof: the committed pre-repair parser is loaded from Git as
    # one function AST only.  The old implementation must reject the real
    # AndroidX lifecycle because it counted the repeated identity packets as
    # invalid; the current parser must accept the same transcript.
    $oldSourceLines = @(git -C $repoRoot show '55061312f40f367b933d0c2db4866335cd36a8ed:scripts/p5e-raw-live-supervisor.ps1')
    $oldSource = $oldSourceLines -join "`n"
    $parseTokens = $null
    $parseErrors = $null
    $oldAst = [System.Management.Automation.Language.Parser]::ParseInput($oldSource, [ref]$parseTokens, [ref]$parseErrors)
    $oldFunction = @($oldAst.FindAll({ param($node)
            $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
                $node.Name -ceq 'Parse-P5EAccountCheckInstrumentationOutput'
        }, $true))
    Assert-QA 'old-parser-function-found-from-committed-ast' ($oldFunction.Count -eq 1 -and $parseErrors.Count -eq 0)
    $oldFunctionText = $oldFunction[0].Extent.Text.Replace(
        'function Parse-P5EAccountCheckInstrumentationOutput',
        'function Invoke-P5EOldAccountCheckInstrumentationParser')
    . ([scriptblock]::Create($oldFunctionText))
    $oldPositive = Invoke-P5EOldAccountCheckInstrumentationParser -Output $matchText -ExpectedClassMethod $expectedClassMethod
    Assert-QA 'old-parser-red-on-real-lifecycle' (-not [bool]$oldPositive.Accepted)

    # The test itself is deliberately synthetic-only.  These fields are
    # emitted in the report so a future failure cannot be mistaken for a live
    # account/device attempt.
    $passedCount = @($results | Where-Object { $_.passed }).Count
    $report = [ordered]@{
        schemaVersion = 'p5e.account.identity-lifecycle.qa.v1'
        date = '2026-09-24'
        status = 'PASS'
        scope = 'OFFLINE_SYNTHETIC_INSTRUMENTATION_STREAM_ONLY'
        helperSha256 = (Get-FileHash -LiteralPath $helperPath -Algorithm SHA256).Hash
        expectedClassMethod = $expectedClassMethod
        normalLifecycle = 'class/test start status 1; separate result status 0; class/test completion status 0; summary and terminal code -1'
        cases = @($cases.Keys)
        assertions = [int]$passedCount
        assertionCount = [int]$results.Count
        results = @($results)
        adbCalls = 0
        deviceActions = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
        installAttempts = 0
        launchCount = 0
        retryCount = 0
    }
    $outputFullPath = [IO.Path]::GetFullPath($OutputPath)
    [IO.File]::WriteAllText($outputFullPath, ($report | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    $report | ConvertTo-Json -Depth 12
} catch {
    $report = [ordered]@{
        schemaVersion = 'p5e.account.identity-lifecycle.qa.v1'
        date = '2026-09-24'
        status = 'FAILED_REPAIRING'
        scope = 'OFFLINE_SYNTHETIC_INSTRUMENTATION_STREAM_ONLY'
        failure = $_.Exception.Message
        assertions = @($results)
        adbCalls = 0
        deviceActions = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
        installAttempts = 0
        launchCount = 0
        retryCount = 0
    }
    $outputFullPath = [IO.Path]::GetFullPath($OutputPath)
    [IO.File]::WriteAllText($outputFullPath, ($report | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    $report | ConvertTo-Json -Depth 12
    throw
} finally {
    if ($null -ne $module) { Remove-Module -ModuleInfo $module -Force -ErrorAction SilentlyContinue }
}
