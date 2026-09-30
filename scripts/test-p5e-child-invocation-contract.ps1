[CmdletBinding()]
param(
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$contractPath = Join-Path $PSScriptRoot 'p5e-child-invocation-contract.ps1'
if (-not (Test-Path -LiteralPath $contractPath -PathType Leaf)) {
    throw 'P5E_CHILD_INVOCATION_CONTRACT_MISSING_STOP'
}
. $contractPath -LibraryOnly

function Write-P5ESyntheticUtf8 {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Text)
    $encoding = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($Path, $Text, $encoding)
}

function Get-P5ESafeInvocationObservation {
    param([Parameter(Mandatory = $true)]$Result)
    return [ordered]@{
        TypedCode = [string]$Result.TypedCode
        OuterExitCode = [int]$Result.OuterExitCode
        ProcessCreated = [string]$Result.ProcessCreated
        ProcessId = [string]$Result.ProcessId
        ChildExited = [string]$Result.ChildExited
        ChildExitCode = $Result.ChildExitCode
        TimedOut = [bool]$Result.TimedOut
        CaptureBounded = [bool]$Result.CaptureBounded
        CaptureErrorClass = [string]$Result.CaptureErrorClass
        ContainmentStatus = [string]$Result.ContainmentStatus
        ContainmentVerified = [bool]$Result.ContainmentVerified
        StdoutDrainStatus = [string]$Result.StdoutDrainStatus
        StderrDrainStatus = [string]$Result.StderrDrainStatus
        StdoutByteLength = [long]$Result.StdoutByteLength
        StderrByteLength = [long]$Result.StderrByteLength
        StdoutTruncated = [bool]$Result.StdoutTruncated
        StderrTruncated = [bool]$Result.StderrTruncated
        OutputTooLarge = [bool]$Result.OutputTooLarge
        RedactionViolation = [bool]$Result.RedactionViolation
        RetryCount = [int]$Result.RetryCount
        RedispatchCount = [int]$Result.RedispatchCount
    }
}

function Test-P5ESyntheticProcessGone {
    param([Parameter(Mandatory = $true)]$Result)
    if ([string]$Result.ProcessCreated -ne 'TRUE') { return $true }
    if ([bool]$Result.ContainmentVerified -and [string]$Result.ChildExited -eq 'TRUE') { return $true }
    if ([string]$Result.ProcessId -eq 'PID_UNKNOWN') { return $false }
    $process = Get-Process -Id ([int]$Result.ProcessId) -ErrorAction SilentlyContinue
    return ($null -eq $process)
}

$caseRecords = New-Object 'System.Collections.Generic.List[object]'
$failureCount = 0
function Add-P5ESyntheticCase {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [Parameter(Mandatory = $true)]$Observation,
        [string]$Assertion = ''
    )
    $safeObservation = if ($Observation -is [System.Collections.IDictionary]) { $Observation } else { Get-P5ESafeInvocationObservation -Result $Observation }
    [void]$script:caseRecords.Add([pscustomobject]@{
        Name = $Name
        Expected = $Expected
        Passed = $Passed
        Assertion = $Assertion
        Observation = $safeObservation
    })
    if (-not $Passed) { $script:failureCount++ }
}

$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-child-invocation-repair-' + [Guid]::NewGuid().ToString('N') + ' path')
$oldWrapperRoot = Join-Path $fixtureRoot 'red-wrapper'
[void](New-Item -ItemType Directory -Path $fixtureRoot)
[void](New-Item -ItemType Directory -Path $oldWrapperRoot)
$fakeChildPath = Join-Path $fixtureRoot 'fake child.ps1'
$fakeChildText = @'
param([string]$Mode, [string]$MarkerPath)
switch ($Mode) {
    'exit0-stdout' { [Console]::Out.Write('SYNTHETIC_STDOUT_SENTINEL'); [Console]::Out.Flush(); exit 0 }
    'exit0-stderr' { [Console]::Error.Write('SYNTHETIC_STDERR_SENTINEL'); [Console]::Error.Flush(); exit 0 }
    'exit7-stderr' { [Console]::Error.Write('SYNTHETIC_TYPED_STOP'); [Console]::Error.Flush(); exit 7 }
    'exit7-empty' { exit 7 }
    'path-arg' { if ($MarkerPath -ceq 'path-space-flag') { [Console]::Out.Write('ARG_MATCH'); exit 0 }; exit 31 }
    'env-absent' {
        $variables = [Environment]::GetEnvironmentVariables('Process')
        if (-not $variables.ContainsKey('P5E_SYNTHETIC_EXPECTED')) { exit 0 }
        exit 41
    }
    'env-present' {
        $variables = [Environment]::GetEnvironmentVariables('Process')
        if ($variables.ContainsKey('P5E_SYNTHETIC_EXPECTED') -and $variables['P5E_SYNTHETIC_EXPECTED'] -ceq 'fake-secret-sentinel') { exit 0 }
        exit 42
    }
    'large-both' {
        $chunk = 'X' * 8192
        for ($i = 0; $i -lt 9; $i++) { [Console]::Out.Write($chunk); [Console]::Error.Write($chunk) }
        [Console]::Out.Flush(); [Console]::Error.Flush(); exit 0
    }
    'timeout' { Start-Sleep -Seconds 10; exit 0 }
    'tree-timeout' {
        $childPath = $MyInvocation.MyCommand.Path
        $hostPath = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
        $child = Start-Process -FilePath $hostPath -ArgumentList @('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$childPath,'pipe-holder') -WindowStyle Hidden -PassThru
        if (-not [string]::IsNullOrWhiteSpace($MarkerPath)) { [IO.File]::WriteAllText($MarkerPath, [string]$child.Id) }
        Start-Sleep -Seconds 10
        exit 0
    }
    'pipe-holder' { Start-Sleep -Seconds 10; exit 0 }
    'invalid-utf8' { $stream = [Console]::OpenStandardOutput(); $bytes = [byte[]](0xC3, 0x28, 0xFF); $stream.Write($bytes, 0, $bytes.Length); $stream.Flush(); exit 0 }
    'delayed-close' { [Console]::Out.Write('DELAYED'); [Console]::Out.Flush(); Start-Sleep -Milliseconds 250; exit 0 }
    'marker' { if (-not [string]::IsNullOrWhiteSpace($MarkerPath)) { [IO.File]::WriteAllText($MarkerPath, 'started') }; exit 0 }
    default { exit 99 }
}
'@
Write-P5ESyntheticUtf8 -Path $fakeChildPath -Text $fakeChildText
$outerPropagationPath = Join-Path $fixtureRoot 'outer exit propagation.ps1'
$outerPropagationText = @'
param([string]$ContractPath, [string]$HostPath, [string]$FakePath, [string]$Mode)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. $ContractPath -LibraryOnly
$argsForChild = @('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$FakePath,$Mode)
$child = Invoke-P5EChildInvocation -FilePath $HostPath -ArgumentList $argsForChild -WorkingDirectory ([IO.Path]::GetDirectoryName($FakePath)) -TimeoutMilliseconds 3000
exit [int]$child.OuterExitCode
'@
Write-P5ESyntheticUtf8 -Path $outerPropagationPath -Text $outerPropagationText
$expiryGatePath = Join-Path $fixtureRoot 'expiry gate.ps1'
$expiryGateText = @'
param([string]$ContractPath, [string]$HostPath, [string]$FakePath, [string]$MarkerPath)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. $ContractPath -LibraryOnly
$now = [DateTimeOffset]::Parse('2026-09-29T10:00:00Z')
$gate = Test-P5EReceiptExpiryContract -IssuedAtUtc $now.AddMinutes(-31) -ExpiresAtUtc $now.AddMinutes(-1) -NowUtc $now
if (-not $gate.LaunchAllowed) { exit 2 }
$argsForChild = @('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$FakePath,'marker',$MarkerPath)
$child = Invoke-P5EChildInvocation -FilePath $HostPath -ArgumentList $argsForChild -WorkingDirectory ([IO.Path]::GetDirectoryName($FakePath)) -TimeoutMilliseconds 3000
exit [int]$child.OuterExitCode
'@
Write-P5ESyntheticUtf8 -Path $expiryGatePath -Text $expiryGateText

$psCommand = Get-Command 'powershell.exe' -ErrorAction Stop
$psPath = [IO.Path]::GetFullPath([string]$psCommand.Source)
$sensitiveSentinel = 'fake-secret-sentinel'
[Environment]::SetEnvironmentVariable('P5E_SYNTHETIC_EXPECTED', $sensitiveSentinel, 'Process')

try {
    # RED: native invocation with stderr is promoted to an exception and has
    # no typed child/process-created record for the caller.
    $redStdout = Join-Path $oldWrapperRoot 'stdout.txt'
    $redStderr = Join-Path $oldWrapperRoot 'stderr.txt'
    $redCaught = $false
    $redExceptionClass = ''
    $redExit = $null
    try {
        & $psPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $fakeChildPath 'exit7-stderr' 1> $redStdout 2> $redStderr
        $redExit = $LASTEXITCODE
    } catch {
        $redCaught = $true
        $redExceptionClass = $_.Exception.GetType().Name
    }
    Add-P5ESyntheticCase -Name 'RED-native-invocation-stderr-hides-typed-child-outcome' `
        -Expected 'exception-without-structured-child-exit' `
        -Passed ($redCaught -and -not [string]::IsNullOrWhiteSpace($redExceptionClass)) `
        -Observation ([ordered]@{
            ExceptionCaught = $redCaught
            ExceptionClass = $redExceptionClass
            StructuredChildExit = $false
            ProcessCreated = 'UNKNOWN'
            ProcessId = 'PID_UNKNOWN'
            ChildExitCode = $null
            LauncherExitCode = 'UNKNOWN'
        }) -Assertion 'PowerShell native invocation raised an exception instead of returning a typed exit/capture record.'

    $common = @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $fakeChildPath)
    $zeroStdout = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('exit0-stdout')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @($sensitiveSentinel)
    $zeroStdoutJson = $zeroStdout | ConvertTo-Json -Depth 8 -Compress
    Add-P5ESyntheticCase -Name 'GREEN-exit0-stdout' -Expected 'CHILD_EXIT_ZERO/outer-0' `
        -Passed ($zeroStdout.TypedCode -eq 'CHILD_EXIT_ZERO' -and $zeroStdout.ChildExitCode -eq 0 -and
            $zeroStdout.OuterExitCode -eq 0 -and $zeroStdout.ProcessCreated -eq 'TRUE' -and
            $zeroStdout.CaptureBounded -and $zeroStdoutJson -notmatch 'SYNTHETIC_') `
        -Observation $zeroStdout -Assertion 'Known zero exit is preserved and the result contains no raw stdout.'

    $zeroStderr = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('exit0-stderr')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @('SYNTHETIC_STDERR_SENTINEL')
    $zeroStderrJson = $zeroStderr | ConvertTo-Json -Depth 8 -Compress
    Add-P5ESyntheticCase -Name 'GREEN-exit0-stderr' -Expected 'CHILD_EXIT_ZERO/outer-0' `
        -Passed ($zeroStderr.TypedCode -eq 'CHILD_EXIT_ZERO' -and $zeroStderr.ChildExitCode -eq 0 -and
            $zeroStderr.OuterExitCode -eq 0 -and $zeroStderr.StderrByteLength -gt 0 -and
            $zeroStderrJson -notmatch 'SYNTHETIC_STDERR_SENTINEL') `
        -Observation $zeroStderr -Assertion 'Stderr is captured independently; exit zero remains success and raw stderr is absent.'

    $nonzeroStderr = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('exit7-stderr')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @('SYNTHETIC_TYPED_STOP')
    $nonzeroStderrJson = $nonzeroStderr | ConvertTo-Json -Depth 8 -Compress
    Add-P5ESyntheticCase -Name 'GREEN-exit7-stderr' -Expected 'CHILD_EXIT_NONZERO/outer-nonzero' `
        -Passed ($nonzeroStderr.TypedCode -eq 'CHILD_EXIT_NONZERO' -and $nonzeroStderr.ChildExitCode -eq 7 -and
            $nonzeroStderr.OuterExitCode -ne 0 -and $nonzeroStderr.ProcessCreated -eq 'TRUE' -and
            $nonzeroStderrJson -notmatch 'SYNTHETIC_TYPED_STOP') `
        -Observation $nonzeroStderr -Assertion 'Child exit 7 is preserved; stderr does not cause a wrapper exception or false pass.'

    $nonzeroEmpty = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('exit7-empty')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000
    Add-P5ESyntheticCase -Name 'GREEN-exit7-empty-stderr' -Expected 'CHILD_EXIT_NONZERO/outer-nonzero' `
        -Passed ($nonzeroEmpty.TypedCode -eq 'CHILD_EXIT_NONZERO' -and $nonzeroEmpty.ChildExitCode -eq 7 -and
            $nonzeroEmpty.OuterExitCode -ne 0 -and $nonzeroEmpty.StderrByteLength -eq 0) `
        -Observation $nonzeroEmpty -Assertion 'A nonzero child with empty stderr is still a terminal nonzero result.'

    $missingPath = Join-Path $fixtureRoot 'missing executable.exe'
    $startFailure = Invoke-P5EChildInvocation -FilePath $missingPath -ArgumentList @() `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 100
    Add-P5ESyntheticCase -Name 'GREEN-process-start-failure' -Expected 'PROCESS_START_FAILED/process-false/outer-nonzero' `
        -Passed ($startFailure.TypedCode -eq 'PROCESS_START_FAILED' -and $startFailure.ProcessCreated -eq 'FALSE' -and
            $startFailure.ChildExitCode -eq $null -and $startFailure.OuterExitCode -ne 0) `
        -Observation $startFailure -Assertion 'Missing absolute executable stops before process creation without a child exit.'

    $pathSpace = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('path-arg', 'path-space-flag')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000
    Add-P5ESyntheticCase -Name 'GREEN-path-with-spaces' -Expected 'CHILD_EXIT_ZERO/outer-0' `
        -Passed ($pathSpace.TypedCode -eq 'CHILD_EXIT_ZERO' -and $pathSpace.ChildExitCode -eq 0 -and $pathSpace.OuterExitCode -eq 0) `
        -Observation $pathSpace -Assertion 'Absolute executable, working directory, and script path containing spaces are quoted correctly.'

    & $psPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $outerPropagationPath `
        $contractPath $psPath $fakeChildPath 'exit0-stdout'
    $outerZeroExit = [int]$LASTEXITCODE
    & $psPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $outerPropagationPath `
        $contractPath $psPath $fakeChildPath 'exit7-stderr'
    $outerNonzeroExit = [int]$LASTEXITCODE
    Add-P5ESyntheticCase -Name 'GREEN-outer-process-exit-propagation' -Expected 'caller-exit-0-and-caller-exit-nonzero' `
        -Passed ($outerZeroExit -eq 0 -and $outerNonzeroExit -ne 0) `
        -Observation ([ordered]@{
            TypedCode = 'OUTER_EXIT_PROPAGATION'
            ProcessCreated = 'TRUE'
            ProcessId = 'PID_UNKNOWN'
            ChildExitCode = 7
            LauncherExitCode = $outerNonzeroExit
            ZeroCaseOuterExitCode = $outerZeroExit
        }) -Assertion 'A PowerShell 5.1 caller propagates the structured result exit code instead of ending success after child failure.'

    $treePidFile = Join-Path $fixtureRoot 'synthetic-descendant.pid'
    $treeTimeout = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('tree-timeout', $treePidFile)) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 1500
    $treePidText = if (Test-Path -LiteralPath $treePidFile -PathType Leaf) { (Get-Content -Raw -LiteralPath $treePidFile).Trim() } else { '' }
    $treePidValid = $treePidText -match '^[0-9]+$'
    $treeGone = $false
    if ($treePidValid) {
        $treeProcess = Get-Process -Id ([int]$treePidText) -ErrorAction SilentlyContinue
        $treeGone = ($null -eq $treeProcess)
    }
    Add-P5ESyntheticCase -Name 'GREEN-timeout-descendant-and-pipe-holder' -Expected 'parent-and-grandchild-gone/tree-verified' `
        -Passed ($treeTimeout.TypedCode -eq 'PROCESS_TIMEOUT' -and $treeTimeout.ContainmentVerified -and
            $treeTimeout.OuterExitCode -ne 0 -and $treePidValid -and $treeGone) `
        -Observation ([ordered]@{
            TypedCode = [string]$treeTimeout.TypedCode
            ProcessCreated = [string]$treeTimeout.ProcessCreated
            ProcessId = [string]$treeTimeout.ProcessId
            ChildExitCode = $treeTimeout.ChildExitCode
            LauncherExitCode = [int]$treeTimeout.OuterExitCode
            ContainmentVerified = [bool]$treeTimeout.ContainmentVerified
            DescendantPid = $treePidText
            DescendantGone = $treeGone
        }) -Assertion 'Synthetic child creates a descendant that holds the phase open; Job containment verifies both are terminated without redispatch.'

    $timeout = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('timeout')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 150
    $timeoutGone = Test-P5ESyntheticProcessGone -Result $timeout
    Add-P5ESyntheticCase -Name 'GREEN-timeout-no-redispatch' -Expected 'PROCESS_TIMEOUT/outer-nonzero/tree-gone' `
        -Passed ($timeout.TypedCode -eq 'PROCESS_TIMEOUT' -and $timeout.TimedOut -and $timeout.OuterExitCode -ne 0 -and
            $timeout.ContainmentVerified -and $timeout.RedispatchCount -eq 0 -and $timeoutGone) `
        -Observation $timeout -Assertion 'Timeout terminates the contained synthetic process, preserves typed timeout, and never relaunches.'

    $large = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('large-both')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -StdoutCaptureBytes 32768 -StderrCaptureBytes 32768
    Add-P5ESyntheticCase -Name 'GREEN-simultaneous-bounded-large-streams' -Expected 'OUTPUT_TOO_LARGE/outer-nonzero' `
        -Passed ($large.OutputTooLarge -and $large.OuterExitCode -ne 0 -and $large.StdoutByteLength -gt 32768 -and
            $large.StderrByteLength -gt 32768 -and $large.RedispatchCount -eq 0) `
        -Observation $large -Assertion 'Both streams are drained concurrently, capped independently, and overflow is terminal.'

    $invalidUtf8 = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('invalid-utf8')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000
    Add-P5ESyntheticCase -Name 'GREEN-invalid-utf8' -Expected 'CHILD_EXIT_ZERO/outer-0' `
        -Passed ($invalidUtf8.TypedCode -eq 'CHILD_EXIT_ZERO' -and $invalidUtf8.ChildExitCode -eq 0 -and $invalidUtf8.CaptureBounded) `
        -Observation $invalidUtf8 -Assertion 'Invalid UTF-8 is handled as bounded text capture and cannot alter exit propagation.'

    $delayed = Invoke-P5EChildInvocation -FilePath $psPath -ArgumentList ($common + @('delayed-close')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000
    Add-P5ESyntheticCase -Name 'GREEN-delayed-stream-close' -Expected 'CHILD_EXIT_ZERO/outer-0/drained' `
        -Passed ($delayed.TypedCode -eq 'CHILD_EXIT_ZERO' -and $delayed.CaptureBounded -and
            $delayed.StdoutDrainStatus -like 'DRAIN*' -and $delayed.StderrDrainStatus -like 'DRAIN*') `
        -Observation $delayed -Assertion 'Delayed close does not deadlock or convert a known zero exit into an exception.'

    $envAbsent = Invoke-P5EPhaseChildInvocation -Phase 'Before' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
        -ExpectedEnvironmentValue '' -FilePath $psPath -ArgumentList ($common + @('env-absent')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @($sensitiveSentinel)
    Add-P5ESyntheticCase -Name 'GREEN-environment-absent-before' -Expected 'CHILD_EXIT_ZERO/expected-absent' `
        -Passed ($envAbsent.TypedCode -eq 'CHILD_EXIT_ZERO' -and $envAbsent.ChildExitCode -eq 0 -and $envAbsent.RedactionViolation -eq $false) `
        -Observation $envAbsent -Assertion 'Before receives no expected variable and no sentinel is emitted.'

    $envPrepare = Invoke-P5EPhaseChildInvocation -Phase 'PrepareEvent' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
        -ExpectedEnvironmentValue '' -FilePath $psPath -ArgumentList ($common + @('env-absent')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @($sensitiveSentinel)
    Add-P5ESyntheticCase -Name 'GREEN-environment-absent-prepare' -Expected 'CHILD_EXIT_ZERO/expected-absent' `
        -Passed ($envPrepare.TypedCode -eq 'CHILD_EXIT_ZERO' -and $envPrepare.ChildExitCode -eq 0) `
        -Observation $envPrepare -Assertion 'PrepareEvent receives no expected variable.'

    $envPresent = Invoke-P5EPhaseChildInvocation -Phase 'Dispatch' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
        -ExpectedEnvironmentValue $sensitiveSentinel -FilePath $psPath -ArgumentList ($common + @('env-present')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @($sensitiveSentinel)
    $envPresentJson = $envPresent | ConvertTo-Json -Depth 8 -Compress
    Add-P5ESyntheticCase -Name 'GREEN-environment-present-dispatch-only' -Expected 'CHILD_EXIT_ZERO/expected-present/no-secret' `
        -Passed ($envPresent.TypedCode -eq 'CHILD_EXIT_ZERO' -and $envPresent.ChildExitCode -eq 0 -and
            $envPresentJson -notmatch [regex]::Escape($sensitiveSentinel)) `
        -Observation $envPresent -Assertion 'Dispatch receives the process-only synthetic value; result serialization contains no value.'

    $envAfter = Invoke-P5EPhaseChildInvocation -Phase 'After' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
        -ExpectedEnvironmentValue '' -FilePath $psPath -ArgumentList ($common + @('env-absent')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @($sensitiveSentinel)
    Add-P5ESyntheticCase -Name 'GREEN-environment-cleared-after' -Expected 'CHILD_EXIT_ZERO/expected-absent' `
        -Passed ($envAfter.TypedCode -eq 'CHILD_EXIT_ZERO' -and $envAfter.ChildExitCode -eq 0) `
        -Observation $envAfter -Assertion 'The Dispatch environment does not leak into After.'

    $envVerify = Invoke-P5EPhaseChildInvocation -Phase 'Verify' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
        -ExpectedEnvironmentValue '' -FilePath $psPath -ArgumentList ($common + @('env-absent')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000 -SensitiveValues @($sensitiveSentinel)
    Add-P5ESyntheticCase -Name 'GREEN-environment-absent-verify' -Expected 'CHILD_EXIT_ZERO/expected-absent' `
        -Passed ($envVerify.TypedCode -eq 'CHILD_EXIT_ZERO' -and $envVerify.ChildExitCode -eq 0) `
        -Observation $envVerify -Assertion 'Verify receives no expected variable.'

    [Environment]::SetEnvironmentVariable('P5E_SYNTHETIC_EXPECTED', '', 'Process')
    $envEmptyParent = Invoke-P5EPhaseChildInvocation -Phase 'Before' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
        -ExpectedEnvironmentValue '' -FilePath $psPath -ArgumentList ($common + @('env-absent')) `
        -WorkingDirectory $fixtureRoot -TimeoutMilliseconds 3000
    Add-P5ESyntheticCase -Name 'GREEN-empty-inherited-environment-is-removed' -Expected 'CHILD_EXIT_ZERO/name-absent-not-empty-value' `
        -Passed ($envEmptyParent.TypedCode -eq 'CHILD_EXIT_ZERO' -and $envEmptyParent.ChildExitCode -eq 0) `
        -Observation $envEmptyParent -Assertion 'An inherited empty variable is removed by name; IsNullOrEmpty cannot falsely prove absence.'

    $unknownPhaseCaught = $false
    $unknownPhaseClass = ''
    try {
        [void](Invoke-P5EPhaseChildInvocation -Phase 'Unknown' -ExpectedEnvironmentName 'P5E_SYNTHETIC_EXPECTED' `
            -FilePath $psPath -ArgumentList ($common + @('marker')) -WorkingDirectory $fixtureRoot)
    } catch {
        $unknownPhaseCaught = $true
        $unknownPhaseClass = $_.Exception.GetType().Name
    }
    Add-P5ESyntheticCase -Name 'GREEN-unknown-phase-fail-closed' -Expected 'typed-stop-before-child' `
        -Passed ($unknownPhaseCaught -and $unknownPhaseClass -eq 'RuntimeException') `
        -Observation ([ordered]@{ TypedCode = 'P5E_CHILD_PHASE_UNKNOWN_STOP'; ProcessCreated = 'FALSE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = 2 }) `
        -Assertion 'Unknown phase is rejected before constructing a child launch.'

    $expiredMarker = Join-Path $fixtureRoot 'expired-child-started.marker'
    $expiredNow = [DateTimeOffset]::Parse('2026-09-29T10:00:00Z')
    $expired = Test-P5EReceiptExpiryContract -IssuedAtUtc ($expiredNow.AddMinutes(-31)) -ExpiresAtUtc ($expiredNow.AddMinutes(-1)) -NowUtc $expiredNow
    $expiredNoLaunch = -not $expired.LaunchAllowed -and $expired.TypedCode -eq 'RECEIPT_EXPIRED_STOP' -and -not (Test-Path -LiteralPath $expiredMarker)
    Add-P5ESyntheticCase -Name 'GREEN-expired-receipt-no-child' -Expected 'RECEIPT_EXPIRED_STOP/no-child/no-event/outer-nonzero' `
        -Passed $expiredNoLaunch `
        -Observation ([ordered]@{ TypedCode = [string]$expired.TypedCode; ProcessCreated = 'FALSE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = 2; MarkerPresent = (Test-Path -LiteralPath $expiredMarker) }) `
        -Assertion 'Expired synthetic receipt is rejected before invocation; no marker or event is created.'

    $expiredGateMarker = Join-Path $fixtureRoot 'expiry-gate-child.marker'
    & $psPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $expiryGatePath `
        $contractPath $psPath $fakeChildPath $expiredGateMarker
    $expiredGateExit = [int]$LASTEXITCODE
    Add-P5ESyntheticCase -Name 'GREEN-expiry-gate-stops-real-child-path' -Expected 'outer-nonzero/no-child-marker' `
        -Passed ($expiredGateExit -ne 0 -and -not (Test-Path -LiteralPath $expiredGateMarker)) `
        -Observation ([ordered]@{ TypedCode = 'RECEIPT_EXPIRED_STOP'; ProcessCreated = 'FALSE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = $expiredGateExit; MarkerPresent = (Test-Path -LiteralPath $expiredGateMarker) }) `
        -Assertion 'The same tracked gate used immediately before invocation prevents child creation when expiry is stale.'

    $unknownCode = Get-P5EChildTerminalCode -ProcessCreated $true -ChildExitCode $null -TimedOut $false `
        -OutputTooLarge $false -CaptureBounded $false -ContainmentVerified $false -CaptureErrorClass ''
    Add-P5ESyntheticCase -Name 'GREEN-child-status-unknown-is-terminal' -Expected 'EXTERNAL_PROCESS_STATE_UNKNOWN/outer-nonzero' `
        -Passed ($unknownCode -eq 'EXTERNAL_PROCESS_STATE_UNKNOWN') `
        -Observation ([ordered]@{ TypedCode = $unknownCode; ProcessCreated = 'TRUE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = 1 }) `
        -Assertion 'Unknown post-start state is terminal and cannot become success or trigger redispatch.'

    $cleanupChecks = @($zeroStdout, $zeroStderr, $nonzeroStderr, $nonzeroEmpty, $pathSpace, $timeout, $large, $invalidUtf8, $delayed, $treeTimeout, $envAbsent, $envPrepare, $envPresent, $envAfter, $envVerify, $envEmptyParent)
    $cleanupPassed = $true
    foreach ($cleanupResult in $cleanupChecks) { if (-not (Test-P5ESyntheticProcessGone -Result $cleanupResult)) { $cleanupPassed = $false } }
    $cleanupOuterExit = if ($cleanupPassed) { 0 } else { 1 }
    Add-P5ESyntheticCase -Name 'GREEN-cleanup-all-branches' -Expected 'all-created-fixture-processes-gone' `
        -Passed $cleanupPassed `
        -Observation ([ordered]@{ CleanupVerified = $cleanupPassed; ProcessCreated = 'TRUE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = $cleanupOuterExit }) `
        -Assertion 'Every started fixture process is gone after the contract returns.'

    $moduleSelfTest = & $psPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $contractPath -SelfTest
    $moduleSelfTestText = [string]::Join("`n", @($moduleSelfTest))
    Add-P5ESyntheticCase -Name 'GREEN-module-self-test' -Expected 'exit-0/result-shape-pass' `
        -Passed ($LASTEXITCODE -eq 0 -and $moduleSelfTestText -match 'ResultShape.*PASS') `
        -Observation ([ordered]@{ TypedCode = 'MODULE_SELFTEST'; ProcessCreated = 'FALSE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = [int]$LASTEXITCODE }) `
        -Assertion 'The tracked invocation module parses and its result-shape self-test passes under Windows PowerShell.'

    $resultJson = $caseRecords | ConvertTo-Json -Depth 12 -Compress
    $secretLeak = $resultJson -match [regex]::Escape($sensitiveSentinel) -or $resultJson -match 'SYNTHETIC_(STDOUT|STDERR|TYPED_STOP)_SENTINEL'
    $reportScanExit = if ($secretLeak) { 1 } else { 0 }
    Add-P5ESyntheticCase -Name 'GREEN-report-redaction' -Expected 'no-secret-or-raw-sentinel-in-report' `
        -Passed (-not $secretLeak) `
        -Observation ([ordered]@{ TypedCode = 'REPORT_SECRET_SCAN'; ProcessCreated = 'FALSE'; ProcessId = 'PID_UNKNOWN'; ChildExitCode = $null; LauncherExitCode = $reportScanExit; SecretLeak = $secretLeak }) `
        -Assertion 'Synthetic secret and raw output sentinels never enter the bounded QA JSON.'

    $sourceHash = (Get-FileHash -LiteralPath $contractPath -Algorithm SHA256).Hash.ToUpperInvariant()
    $fixtureHash = (Get-FileHash -LiteralPath $fakeChildPath -Algorithm SHA256).Hash.ToUpperInvariant()
    $outerPropagationHash = (Get-FileHash -LiteralPath $outerPropagationPath -Algorithm SHA256).Hash.ToUpperInvariant()
    $expiryGateHash = (Get-FileHash -LiteralPath $expiryGatePath -Algorithm SHA256).Hash.ToUpperInvariant()
    $overallResult = if ($failureCount -eq 0) { 'PASS' } else { 'FAIL' }
    $report = [ordered]@{}
    $report['schema'] = 'p5e.a43.child-invocation-repair-qa.v1'
    $report['generatedAtUtc'] = [DateTime]::UtcNow.ToString('o')
    $report['result'] = $overallResult
    $report['failureCount'] = $failureCount
    $report['sourcePath'] = 'scripts/p5e-child-invocation-contract.ps1'
    $report['sourceSha256'] = $sourceHash
    $report['fixtureName'] = 'fake child.ps1'
    $report['fixtureSha256'] = $fixtureHash
    $report['outerPropagationFixtureSha256'] = $outerPropagationHash
    $report['expiryGateFixtureSha256'] = $expiryGateHash
    $report['supervisorSha256'] = $script:P5EChildInvocationSupervisorSha256
    $report['stdoutCaptureCap'] = $script:P5EChildInvocationStdoutCap
    $report['stderrCaptureCap'] = $script:P5EChildInvocationStderrCap
    $report['red'] = [ordered]@{ oldOperatorStderrExceptionReproduced = $redCaught; exceptionClass = $redExceptionClass }
    $report['counters'] = [ordered]@{
        adb = 0; deviceRead = 0; deviceWrite = 0; provider = 0; credential = 0
        dbWrite = 0; buildInstall = 0; raw = 0; redispatch = 0; retry = 0
    }
    $report['cases'] = @($caseRecords.ToArray())
    $report['rawOutputRetained'] = $false
    $report['secretSentinelRetained'] = $false
    $json = $report | ConvertTo-Json -Depth 14
    if ($json -match [regex]::Escape($sensitiveSentinel) -or $json -match 'SYNTHETIC_(STDOUT|STDERR|TYPED_STOP)_SENTINEL') {
        throw 'P5E_CHILD_INVOCATION_QA_SECRET_SCAN_FAILED'
    }
    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        $outputFull = [IO.Path]::GetFullPath($OutputPath)
        $outputParent = Split-Path -Parent $outputFull
        if (-not (Test-Path -LiteralPath $outputParent -PathType Container)) { [void](New-Item -ItemType Directory -Path $outputParent) }
        Write-P5ESyntheticUtf8 -Path $outputFull -Text $json
    }
    Write-Output $json
    if ($failureCount -ne 0) { exit 1 }
    exit 0
} finally {
    [Environment]::SetEnvironmentVariable('P5E_SYNTHETIC_EXPECTED', $null, 'Process')
    if (Test-Path -LiteralPath $fixtureRoot) { Remove-Item -LiteralPath $fixtureRoot -Recurse -Force -ErrorAction SilentlyContinue }
}
