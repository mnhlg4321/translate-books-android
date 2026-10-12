[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedScriptSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedHelperSha256,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$TestApkPath,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTestApkSha256,

    [string]$Serial = '15e84958',
    [string]$AdbPath = 'adb',
    [string]$EvidenceDirectory = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptPath = [IO.Path]::GetFullPath($MyInvocation.MyCommand.Path)
$helperPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'p5e-raw-live-supervisor.ps1'))
$accountCheckClass = 'com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest#ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch'
$testRunner = 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'
$accountEnvironmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$timeoutMilliseconds = 120000L

function Assert-RegularFileHash {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedHash,
        [Parameter(Mandatory = $true)][string]$ErrorPrefix
    )
    $literalPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literalPath -PathType Leaf)) {
        throw ($ErrorPrefix + '_MISSING_STOP')
    }
    $item = Get-Item -LiteralPath $literalPath -Force
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw ($ErrorPrefix + '_NOT_REGULAR_STOP')
    }
    $resolvedPath = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literalPath).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literalPath, $resolvedPath)) {
        throw ($ErrorPrefix + '_PATH_SWAP_STOP')
    }
    $actual = (Get-FileHash -LiteralPath $literalPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -cne $ExpectedHash.ToLowerInvariant()) {
        throw ($ErrorPrefix + '_HASH_MISMATCH_STOP')
    }
    return $actual
}

function Write-AccountCheckJson {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value
    )
    [IO.File]::WriteAllText(
        $Path,
        ($Value | ConvertTo-Json -Depth 20 -Compress),
        [Text.UTF8Encoding]::new($false))
}

# These pins are checked before this module reads the process-only expected
# value and before any ADB Process is created.
Assert-RegularFileHash -Path $scriptPath -ExpectedHash $ExpectedScriptSha256 -ErrorPrefix 'ACCOUNT_CHECK_SCRIPT' | Out-Null
Assert-RegularFileHash -Path $helperPath -ExpectedHash $ExpectedHelperSha256 -ErrorPrefix 'ACCOUNT_CHECK_HELPER' | Out-Null
Assert-RegularFileHash -Path $TestApkPath -ExpectedHash $ExpectedTestApkSha256 -ErrorPrefix 'ACCOUNT_CHECK_TEST_APK' | Out-Null

# Import only the hash-checked helper library.  The private module prevents
# helper parameter variables from overwriting this runner's parameters, and
# -LibraryOnly cannot dispatch RAW, provider or device work.
$helperModule = New-Module -Name ('P5EAccountCheckHelper_' + [Guid]::NewGuid().ToString('N')) -ScriptBlock {
    param([Parameter(Mandatory = $true)][string]$HelperFile)
    . $HelperFile -LibraryOnly
    Export-ModuleMember -Function @(
        'Get-P5EAccountFingerprint',
        'Get-P5EInstrumentationComponent',
        'Assert-P5EInstrumentationComponent',
        'New-P5EAccountCheckRemoteCommandTokens',
        'Test-P5EAccountCheckCommandArguments',
        'New-P5EAdbArgumentList',
        'Set-P5EProcessStartInfoArguments',
        'Protect-P5ECaptureText',
        'Invoke-P5EProcessSupervisor',
        'Parse-P5EAccountCheckInstrumentationOutput'
    )
} -ArgumentList $helperPath
Import-Module $helperModule -Force | Out-Null

if ($Serial -cne '15e84958') { throw 'ACCOUNT_CHECK_SERIAL_MISMATCH_STOP' }
[void](Assert-P5EInstrumentationComponent -Component $testRunner)
if (-not (Get-Command $AdbPath -ErrorAction SilentlyContinue)) { throw 'ACCOUNT_CHECK_ADB_MISSING_STOP' }

# The only expected-value read is from the owner PowerShell Process scope.
# It is never put in host argv, child environment, a file or a transcript.
$expected = Get-P5EAccountFingerprint
if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
    $EvidenceDirectory = Join-Path 'D:\P5E-private' ('account-check-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff') + '-' + [Guid]::NewGuid().ToString('N'))
}
$EvidenceDirectory = [IO.Path]::GetFullPath($EvidenceDirectory)
New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
if (@(Get-ChildItem -LiteralPath $EvidenceDirectory -Force).Count -ne 0) { throw 'ACCOUNT_CHECK_EVIDENCE_DIRECTORY_NOT_EMPTY_STOP' }

# A remote sh child reads one exact UTF-8 line from adb stdin, then supplies
# that in-memory shell variable to am instrument.  The value is absent from
# every ADB argv token and is removed from the ADB child Process environment.
$remoteTokens = New-P5EAccountCheckRemoteCommandTokens -ClassMethod $accountCheckClass -Component $testRunner
$adbArguments = New-P5EAdbArgumentList -Serial $Serial -RemoteCommandTokens $remoteTokens
$commandContract = Test-P5EAccountCheckCommandArguments -AdbArguments $adbArguments `
    -ClassMethod $accountCheckClass -SensitiveValue $expected
if (-not $commandContract.Passed) {
    throw ('ACCOUNT_CHECK_COMMAND_CONTRACT_STOP:' + ($commandContract.Errors -join ','))
}
$run = Invoke-P5EProcessSupervisor -FilePath $AdbPath -ArgumentList $adbArguments `
    -TimeoutMilliseconds $timeoutMilliseconds -EvidenceDirectory $EvidenceDirectory `
    -SensitiveValues @($expected) -StandardInputText ($expected + "`n") `
    -ClearInheritedEnvironmentVariableNames @($accountEnvironmentName)

$safeStdoutText = Get-Content -Raw -LiteralPath $run.StdoutPath
$safeStderrText = Get-Content -Raw -LiteralPath $run.StderrPath
$digestPattern = '(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])'
$digestObserved = ($safeStdoutText -match $digestPattern) -or ($safeStderrText -match $digestPattern)
$redactionPass = -not [bool]($run.RedactionViolation -or $digestObserved)
if ($digestObserved) {
    $safeStdoutText = [regex]::Replace($safeStdoutText, $digestPattern, '[REDACTED_BY_ACCOUNT_CHECK]')
    $safeStderrText = [regex]::Replace($safeStderrText, $digestPattern, '[REDACTED_BY_ACCOUNT_CHECK]')
    [IO.File]::WriteAllText((Join-Path $EvidenceDirectory 'instrumentation-stdout.txt'), $safeStdoutText, [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $EvidenceDirectory 'instrumentation-stderr.txt'), $safeStderrText, [Text.UTF8Encoding]::new($false))
}
$parsed = Parse-P5EAccountCheckInstrumentationOutput -Output $safeStdoutText -ExpectedClassMethod $accountCheckClass

$typedOutcome = if (-not $redactionPass) { 'ACCOUNT_CHECK_REDACTION_FAILURE' }
    elseif (-not $run.CaptureBounded) { 'ACCOUNT_CHECK_TIMEOUT_BOUNDARY_NOT_PROVEN' }
    elseif (-not $run.InputWriteCompleted) { 'ACCOUNT_CHECK_INPUT_CHANNEL_NOT_PROVEN' }
    elseif ($run.LaunchCount -eq 0) { 'ACCOUNT_CHECK_FAILED_BEFORE_LAUNCH' }
    elseif ($run.TimedOut) { 'ACCOUNT_CHECK_TIMEOUT_NOT_PROVEN' }
    elseif ($null -eq $run.ExitCode -or $run.ExitCode -ne 0) { 'ACCOUNT_CHECK_NONZERO_NOT_PROVEN' }
    elseif (-not $parsed.Accepted) { 'ACCOUNT_CHECK_RESULT_NOT_PROVEN' }
    elseif ($parsed.Result -ceq 'MATCH') { 'ACCOUNT_CHECK_COMPLETED_MATCH' }
    else { 'ACCOUNT_CHECK_COMPLETED_MISMATCH' }

$outcome = [ordered]@{
    schemaVersion = 'p5e.account-check.result.v2'
    outcome = $typedOutcome
    result = if ($typedOutcome -in @('ACCOUNT_CHECK_COMPLETED_MATCH', 'ACCOUNT_CHECK_COMPLETED_MISMATCH')) { $parsed.Result } else { 'NOT_PROVEN' }
    serial = $Serial
    launchCount = [int]$run.LaunchCount
    timedOut = [bool]$run.TimedOut
    exitCode = $run.ExitCode
    processOutcome = $run.Outcome
    inputChannel = 'PROCESS_PARENT_TO_REMOTE_SHELL_STDIN_UTF8_LF'
    inputWriteCompleted = [bool]$run.InputWriteCompleted
    expectedInAdbArgv = $false
    expectedInAdbChildEnvironment = $false
    captureBounded = [bool]$run.CaptureBounded
    terminalSuccess = [bool]$parsed.TerminalSuccess
    testFinished = [bool]$parsed.TestFinished
    testIdentityPass = [bool]$parsed.IdentityPass
    resultTokenCount = [int]$parsed.ResultTokenCount
    failureMarkerCount = [int]$parsed.FailureMarkerCount
    parserErrors = @($parsed.Errors)
    redactionPass = [bool]$redactionPass
    scriptSha256 = (Get-FileHash -LiteralPath $scriptPath -Algorithm SHA256).Hash.ToLowerInvariant()
    helperSha256 = (Get-FileHash -LiteralPath $helperPath -Algorithm SHA256).Hash.ToLowerInvariant()
    testApkSha256 = (Get-FileHash -LiteralPath $TestApkPath -Algorithm SHA256).Hash.ToLowerInvariant()
    providerCalls = 0
    dbWrites = 0
    rawDispatches = 0
}
Write-AccountCheckJson -Path (Join-Path $EvidenceDirectory 'ACCOUNT_CHECK_RESULT.json') -Value $outcome
Write-Output ('P5E_ACCOUNT_CHECK_RESULT=' + [string]$outcome.result)
Write-Output ('P5E_ACCOUNT_CHECK_OUTCOME=' + $typedOutcome)
Write-Output 'P5E_ACCOUNT_CHECK_PROVIDER_CALLS=0'
Write-Output 'P5E_ACCOUNT_CHECK_DB_WRITES=0'
Write-Output 'P5E_ACCOUNT_CHECK_RAW_DISPATCHES=0'
if ($typedOutcome -notin @('ACCOUNT_CHECK_COMPLETED_MATCH', 'ACCOUNT_CHECK_COMPLETED_MISMATCH')) { exit 30 }
exit 0
