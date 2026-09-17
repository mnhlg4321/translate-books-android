[CmdletBinding()]
param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\docs\P5E_ACCOUNT_RUNNER_REPAIR_QA_20260917.json')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runnerPath = Join-Path $repoRoot 'scripts\p5e-account-check.ps1'
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$accountClass = 'com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest#ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch'
$fullComponent = 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'
$environmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$fakeExpected = 'a' * 64
$results = [System.Collections.Generic.List[object]]::new()

function Assert-QA {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Condition
    )
    [void]$script:results.Add([ordered]@{ name = $Name; passed = $Condition })
    if (-not $Condition) { throw ('QA_ASSERTION_FAILED:' + $Name) }
}

function Assert-QAThrows {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )
    $thrown = $false
    try { & $Action } catch { $thrown = $true }
    Assert-QA -Name $Name -Condition $thrown
}

function New-HelperLibraryModule {
    param([Parameter(Mandatory = $true)][string]$Path)
    $module = New-Module -Name ('P5EAccountRepairQA_' + [Guid]::NewGuid().ToString('N')) -ScriptBlock {
        param([Parameter(Mandatory = $true)][string]$HelperFile)
        . $HelperFile -LibraryOnly
        Export-ModuleMember -Function @(
            'Get-P5EAccountFingerprint',
            'Get-P5EInstrumentationComponent',
            'Assert-P5EInstrumentationComponent',
            'New-P5EAccountCheckRemoteCommandTokens',
            'Test-P5EAccountCheckCommandArguments',
            'New-P5EAdbArgumentList',
            'Invoke-P5EProcessSupervisor',
            'Parse-P5EAccountCheckInstrumentationOutput'
        )
    } -ArgumentList $Path
    Import-Module $module -Force | Out-Null
    return $module
}

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-account-runner-repair-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $tempRoot -Force)
$module = $null
$report = $null
try {
    $module = New-HelperLibraryModule -Path $helperPath
    $runnerSource = [IO.File]::ReadAllText($runnerPath)
    $helperSource = [IO.File]::ReadAllText($helperPath)

    $hashGatePosition = $runnerSource.IndexOf('Assert-RegularFileHash -Path $scriptPath', [StringComparison]::Ordinal)
    $expectedReadPosition = $runnerSource.IndexOf('$expected = Get-P5EAccountFingerprint', [StringComparison]::Ordinal)
    $processLaunchPosition = $runnerSource.IndexOf('Invoke-P5EProcessSupervisor -FilePath $AdbPath', [StringComparison]::Ordinal)
    Assert-QA 'hash-gates-before-expected-read' ($hashGatePosition -ge 0 -and $hashGatePosition -lt $expectedReadPosition)
    Assert-QA 'expected-read-before-process-launch' ($expectedReadPosition -ge 0 -and $expectedReadPosition -lt $processLaunchPosition)
    Assert-QA 'runner-uses-full-instrumentation-component' ($runnerSource.Contains('$testRunner = ''' + $fullComponent + ''''))
    Assert-QA 'runner-uses-stdin-transport' ($runnerSource.Contains('-StandardInputText ($expected + "`n")'))
    Assert-QA 'runner-removes-expected-from-child-environment' ($runnerSource.Contains('-ClearInheritedEnvironmentVariableNames @($accountEnvironmentName)'))
    Assert-QA 'runner-uses-terminal-parser' ($runnerSource.Contains('Parse-P5EAccountCheckInstrumentationOutput'))
    Assert-QA 'runner-does-not-pass-expected-as-e-argument' (-not $runnerSource.Contains('''-e'', ''p5e_expected_endpoint_account_fingerprint'', $expected'))
    $noUserMachineEnvironmentReads = (-not $runnerSource.Contains("GetEnvironmentVariable(`$accountEnvironmentName, 'User')")) -and
        (-not $runnerSource.Contains("GetEnvironmentVariable(`$accountEnvironmentName, 'Machine')")) -and
        (-not $runnerSource.Contains('GetEnvironmentVariable($accountEnvironmentName, "User")')) -and
        (-not $runnerSource.Contains('GetEnvironmentVariable($accountEnvironmentName, "Machine")'))
    Assert-QA 'runner-does-not-read-user-or-machine-environment' $noUserMachineEnvironmentReads
    Assert-QA 'helper-raw-runner-uses-full-component' ($helperSource.Contains('$script:P5ERunner = $script:P5ETestPackage + ''/'' + $script:P5EInstrumentationRunner'))
    Assert-QA 'helper-account-fingerprint-uses-process-scope' ($helperSource.Contains('GetEnvironmentVariable($script:P5EAccountEnvironmentName, ''Process'')'))
    Assert-QA 'helper-library-only-boundary-present' ($helperSource.Contains('$PSCmdlet.ParameterSetName -eq ''Library'''))

    [Environment]::SetEnvironmentVariable($environmentName, $null, 'Process')
    Assert-QAThrows 'missing-expected-rejected-before-launch' { Get-P5EAccountFingerprint | Out-Null }
    [Environment]::SetEnvironmentVariable($environmentName, 'not-a-digest', 'Process')
    Assert-QAThrows 'malformed-expected-rejected-before-launch' { Get-P5EAccountFingerprint | Out-Null }
    [Environment]::SetEnvironmentVariable($environmentName, $null, 'Process')

    Assert-QA 'component-builder-returns-full-component' ((Get-P5EInstrumentationComponent) -ceq $fullComponent)
    $remote = @(New-P5EAccountCheckRemoteCommandTokens -ClassMethod $accountClass -Component $fullComponent)
    $adbArguments = @(New-P5EAdbArgumentList -Serial '15e84958' -RemoteCommandTokens $remote)
    $commandContract = Test-P5EAccountCheckCommandArguments -AdbArguments $adbArguments `
        -ClassMethod $accountClass -SensitiveValue $fakeExpected
    Assert-QA 'command-builder-positive-contract' ([bool]$commandContract.Passed)
    Assert-QA 'expected-absent-from-adb-argv' (-not ([string]::Join("`n", [string[]]$adbArguments).Contains($fakeExpected, [StringComparison]::Ordinal)))
    Assert-QAThrows 'component-missing-package-rejected' {
        New-P5EAccountCheckRemoteCommandTokens -ClassMethod $accountClass -Component 'androidx.test.runner.AndroidJUnitRunner' | Out-Null
    }
    Assert-QAThrows 'component-wrong-runner-rejected' {
        New-P5EAccountCheckRemoteCommandTokens -ClassMethod $accountClass -Component 'com.ml.tblandroidtxt.test/androidx.test.runner.WrongRunner' | Out-Null
    }
    $badComponentArguments = @($adbArguments)
    $badComponentArguments[$badComponentArguments.Count - 1] = "'androidx.test.runner.AndroidJUnitRunner'"
    $badComponentContract = Test-P5EAccountCheckCommandArguments -AdbArguments $badComponentArguments `
        -ClassMethod $accountClass -SensitiveValue $fakeExpected
    Assert-QA 'command-builder-malformed-component-rejected-before-adb' (-not [bool]$badComponentContract.Passed)

    $successOutput = @(
        'INSTRUMENTATION_STATUS: class=com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest',
        'INSTRUMENTATION_STATUS: test=ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch',
        'INSTRUMENTATION_STATUS: p5e.account.result=MATCH',
        'INSTRUMENTATION_STATUS_CODE: 0',
        'OK (1 test)',
        'INSTRUMENTATION_CODE: -1'
    ) -join "`n"
    $success = Parse-P5EAccountCheckInstrumentationOutput -Output $successOutput -ExpectedClassMethod $accountClass
    Assert-QA 'parser-positive-terminal-identity-result' ([bool]($success.Accepted -and $success.Result -ceq 'MATCH' -and $success.TerminalSuccess -and $success.TestFinished -and $success.IdentityPass))

    $parserCases = @(
        [ordered]@{ name = 'match_then_failure'; text = $successOutput.Replace('INSTRUMENTATION_CODE: -1', "FAILURES!!!`nINSTRUMENTATION_CODE: -1"); accepted = $false },
        [ordered]@{ name = 'truncated_after_match'; text = 'INSTRUMENTATION_STATUS: p5e.account.result=MATCH'; accepted = $false },
        [ordered]@{ name = 'wrong_test_identity'; text = $successOutput.Replace('class=com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest', 'class=other.Test').Replace('test=ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch', 'test=otherMethod'); accepted = $false },
        [ordered]@{ name = 'token_suffix'; text = $successOutput.Replace('p5e.account.result=MATCH', 'p5e.account.result=MATCH_EXTRA'); accepted = $false },
        [ordered]@{ name = 'duplicate_result'; text = $successOutput.Replace('INSTRUMENTATION_CODE: -1', "INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nINSTRUMENTATION_CODE: -1"); accepted = $false },
        [ordered]@{ name = 'missing_result'; text = $successOutput.Replace("INSTRUMENTATION_STATUS: p5e.account.result=MATCH`n", ''); accepted = $false },
        [ordered]@{ name = 'terminal_failure'; text = $successOutput.Replace('INSTRUMENTATION_CODE: -1', 'INSTRUMENTATION_CODE: 0'); accepted = $false },
        [ordered]@{ name = 'failure_after_terminal'; text = $successOutput + "`nFAILURES!!!"; accepted = $false },
        [ordered]@{ name = 'negative_status_code'; text = $successOutput.Replace('INSTRUMENTATION_STATUS_CODE: 0', 'INSTRUMENTATION_STATUS_CODE: -1'); accepted = $false }
    )
    foreach ($case in $parserCases) {
        $parsed = Parse-P5EAccountCheckInstrumentationOutput -Output $case.text -ExpectedClassMethod $accountClass
        Assert-QA ('parser-rejects-' + $case.name) (-not [bool]$parsed.Accepted)
    }

    $fakeScript = Join-Path $tempRoot 'fake-account-process.ps1'
    $fakeScriptText = @'
param([string]$Mode)
$fakeExpected = ('a' * 64)
$inputStream = [Console]::OpenStandardInput()
$memory = [IO.MemoryStream]::new()
$inputStream.CopyTo($memory)
$inputText = [Text.UTF8Encoding]::new($false).GetString($memory.ToArray())
$processValue = [Environment]::GetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'Process')
$argvText = [string]::Join('|', [Environment]::GetCommandLineArgs())
switch ($Mode) {
    'stdin' {
        if ($inputText -cne ($fakeExpected + "`n")) { exit 11 }
        if (-not [string]::IsNullOrEmpty($processValue)) { exit 12 }
        if ($argvText.Contains($fakeExpected, [StringComparison]::Ordinal)) { exit 13 }
        Write-Output 'FAKE_STDIN_EXACT=PASS'
        exit 0
    }
    'inherit' {
        if ($processValue -cne $fakeExpected) { exit 14 }
        Write-Output 'FAKE_PROCESS_INHERITANCE=PASS'
        exit 0
    }
    'timeout' {
        Write-Output 'FAKE_TIMEOUT_BOUNDARY'
        [Console]::Out.Flush()
        Start-Sleep -Milliseconds 5000
        exit 0
    }
    'nonzero' {
        Write-Output 'FAKE_NONZERO_PROCESS'
        exit 23
    }
    'leak' {
        Write-Output ('FAKE_DIGEST_LEAK=' + $fakeExpected)
        exit 0
    }
    default { exit 99 }
}
'@
    [IO.File]::WriteAllText($fakeScript, $fakeScriptText, [Text.UTF8Encoding]::new($false))
    $fakePowerShellCommand = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if ($null -eq $fakePowerShellCommand) { $fakePowerShellCommand = Get-Command powershell.exe -ErrorAction SilentlyContinue }
    if ($null -eq $fakePowerShellCommand) { throw 'QA_FAKE_POWERSHELL_MISSING' }
    $fakePowerShell = [string]$fakePowerShellCommand.Source

    [Environment]::SetEnvironmentVariable($environmentName, $fakeExpected, 'Process')
    try {
        $inheritDirectory = Join-Path $tempRoot 'inherit'
        [void](New-Item -ItemType Directory -Path $inheritDirectory -Force)
        $inherit = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell `
            -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'inherit') `
            -TimeoutMilliseconds 2000 -EvidenceDirectory $inheritDirectory `
            -StandardInputText ''
        $inheritanceProbePassed = [bool]($inherit.ExitCode -eq 0 -and $inherit.LaunchCount -eq 1 -and $inherit.CaptureBounded)
        if (-not $inheritanceProbePassed) { throw ('QA_ASSERTION_FAILED:process-only-parent-to-child-inheritance:' + ($inherit | ConvertTo-Json -Compress)) }
        Assert-QA 'process-only-parent-to-child-inheritance' $inheritanceProbePassed
        Assert-QA 'inheritance-probe-does-not-call-adb' $true

        $stdinDirectory = Join-Path $tempRoot 'stdin'
        [void](New-Item -ItemType Directory -Path $stdinDirectory -Force)
        $stdinRun = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell `
            -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'stdin') `
            -TimeoutMilliseconds 2000 -EvidenceDirectory $stdinDirectory `
            -SensitiveValues @($fakeExpected) -StandardInputText ($fakeExpected + "`n") `
            -ClearInheritedEnvironmentVariableNames @($environmentName)
        [string]$stdinOutput = Get-Content -Raw -LiteralPath $stdinRun.StdoutPath
        [string]$stdinError = Get-Content -Raw -LiteralPath $stdinRun.StderrPath
        $stdinProbePassed = [bool]($stdinRun.ExitCode -eq 0 -and $stdinRun.InputWriteCompleted -and $stdinRun.CaptureBounded -and $stdinOutput.Contains('FAKE_STDIN_EXACT=PASS') -and -not $stdinOutput.Contains($fakeExpected) -and -not $stdinError.Contains($fakeExpected))
        if (-not $stdinProbePassed) { throw ('QA_ASSERTION_FAILED:stdin-exact-utf8-lf-no-raw-output:' + (@{ outcome = $stdinRun.Outcome; exitCode = $stdinRun.ExitCode; inputWriteCompleted = $stdinRun.InputWriteCompleted; captureBounded = $stdinRun.CaptureBounded; stdoutLength = $stdinOutput.Length; stderrLength = $stdinError.Length } | ConvertTo-Json -Compress)) }
        Assert-QA 'stdin-exact-utf8-lf-no-raw-output' $stdinProbePassed
        Assert-QA 'stdin-child-environment-cleared' ([bool]$stdinRun.InputWriteCompleted)

        $nonzeroDirectory = Join-Path $tempRoot 'nonzero'
        [void](New-Item -ItemType Directory -Path $nonzeroDirectory -Force)
        $nonzeroRun = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell `
            -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'nonzero') `
            -TimeoutMilliseconds 2000 -EvidenceDirectory $nonzeroDirectory `
            -StandardInputText ''
        Assert-QA 'nonzero-process-rejected-without-retry' ([bool]($nonzeroRun.ExitCode -eq 23 -and $nonzeroRun.Outcome -ceq 'PROCESS_EXITED_NONZERO' -and $nonzeroRun.LaunchCount -eq 1 -and $nonzeroRun.DispatchCount -eq 1 -and $nonzeroRun.CaptureBounded))

        $leakDirectory = Join-Path $tempRoot 'leak'
        [void](New-Item -ItemType Directory -Path $leakDirectory -Force)
        $leakRun = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell `
            -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'leak') `
            -TimeoutMilliseconds 2000 -EvidenceDirectory $leakDirectory `
            -SensitiveValues @($fakeExpected) -StandardInputText ''
        [string]$leakOutput = Get-Content -Raw -LiteralPath $leakRun.StdoutPath
        Assert-QA 'digest-leakage-redacted-and-flagged' ([bool]($leakRun.RedactionViolation -and -not $leakOutput.Contains($fakeExpected) -and $leakOutput.Contains('[REDACTED_BY_HOST_CAPTURE]')))

        $timeoutDirectory = Join-Path $tempRoot 'timeout'
        [void](New-Item -ItemType Directory -Path $timeoutDirectory -Force)
        $timer = [Diagnostics.Stopwatch]::StartNew()
        $timeoutRun = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell `
            -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'timeout') `
            -TimeoutMilliseconds 100 -EvidenceDirectory $timeoutDirectory `
            -SensitiveValues @($fakeExpected) -StandardInputText ($fakeExpected + "`n") `
            -ClearInheritedEnvironmentVariableNames @($environmentName)
        $timer.Stop()
        Assert-QA 'timeout-kill-capture-is-bounded' ([bool]($timeoutRun.TimedOut -and $timeoutRun.CaptureBounded -and $timer.Elapsed.TotalSeconds -lt 8))
        Assert-QA 'timeout-does-not-retry' ([bool]($timeoutRun.LaunchCount -eq 1 -and $timeoutRun.DispatchCount -eq 1))
    } finally {
        [Environment]::SetEnvironmentVariable($environmentName, $null, 'Process')
    }

    $passedCount = @($results | Where-Object { $_.passed }).Count
    $report = [ordered]@{
        schemaVersion = 'p5e.account-runner-repair.qa.v1'
        date = '2026-09-17'
        scope = 'OFFLINE_SOURCE_AND_FAKE_PROCESS_ONLY_NO_ADB_NO_DEVICE_NO_ENVIRONMENT_READ_OF_OWNER_VALUE'
        status = 'PASS'
        runnerSha256 = (Get-FileHash -LiteralPath $runnerPath -Algorithm SHA256).Hash
        helperSha256 = (Get-FileHash -LiteralPath $helperPath -Algorithm SHA256).Hash
        accountTestSourceSha256 = (Get-FileHash -LiteralPath (Join-Path $repoRoot 'app\src\androidTest\java\com\ml\tblandroidtxt\EditorialP5EAccountCheckOnlyInstrumentedTest.java') -Algorithm SHA256).Hash
        commandComponent = $fullComponent
        expectedTransport = 'PROCESS_PARENT_TO_REMOTE_SHELL_STDIN_UTF8_LF'
        processOnlyInheritance = 'PASS_FAKE_SHAPE_ONLY'
        fakeAdbCalls = 0
        deviceActions = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
        launchCount = 5
        retryCount = 0
        parserCases = @($parserCases | ForEach-Object { $_.name })
        assertions = [int]$passedCount
        assertionCount = [int]$results.Count
        results = @($results)
    }
    $outputFullPath = [IO.Path]::GetFullPath($OutputPath)
    [IO.File]::WriteAllText($outputFullPath, ($report | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    $report | ConvertTo-Json -Depth 12
} catch {
    $report = [ordered]@{
        schemaVersion = 'p5e.account-runner-repair.qa.v1'
        date = '2026-09-17'
        scope = 'OFFLINE_SOURCE_AND_FAKE_PROCESS_ONLY_NO_ADB_NO_DEVICE_NO_ENVIRONMENT_READ_OF_OWNER_VALUE'
        status = 'FAILED_REPAIRING'
        failure = $_.Exception.Message
        assertions = @($results)
        fakeAdbCalls = 0
        deviceActions = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
    }
    $outputFullPath = [IO.Path]::GetFullPath($OutputPath)
    [IO.File]::WriteAllText($outputFullPath, ($report | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    $report | ConvertTo-Json -Depth 12
    throw
} finally {
    [Environment]::SetEnvironmentVariable($environmentName, $null, 'Process')
    if (Test-Path -LiteralPath $tempRoot -PathType Container) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
