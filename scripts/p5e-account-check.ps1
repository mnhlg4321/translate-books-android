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
$testRunner = 'androidx.test.runner.AndroidJUnitRunner'
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

function Invoke-AccountCheckProcess {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$OutputDirectory,
        [Parameter(Mandatory = $true)][string]$SensitiveExpected
    )
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $AdbPath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    Set-P5EProcessStartInfoArguments -StartInfo $startInfo -ArgumentList $Arguments

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $launchCount = 0
    $timedOut = $false
    $exitCode = $null
    $stdout = ''
    $stderr = ''
    $launchError = ''
    try {
        if (-not $process.Start()) { throw 'PROCESS_START_RETURNED_FALSE' }
        $launchCount = 1
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit([int]$timeoutMilliseconds)) {
            $timedOut = $true
            try { $process.Kill($true) } catch { try { $process.Kill() } catch { } }
            try { $process.WaitForExit(5000) | Out-Null } catch { }
        } else {
            $process.WaitForExit()
        }
        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()
        if ($process.HasExited) { $exitCode = $process.ExitCode }
    } catch {
        if ($launchCount -eq 0) { $launchError = $_.Exception.GetType().Name }
        else { $stderr = $_.Exception.GetType().Name }
    } finally {
        $process.Dispose()
    }

    $safeStdout = Protect-P5ECaptureText -Text ([string]$stdout) -SensitiveValues @($SensitiveExpected)
    $safeStderr = Protect-P5ECaptureText -Text ([string]$stderr) -SensitiveValues @($SensitiveExpected)
    $digestPattern = '(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])'
    $digestObserved = ([string]$safeStdout.Text -match $digestPattern) -or
        ([string]$safeStderr.Text -match $digestPattern)
    $sensitiveShapeObserved = $safeStdout.Violation -or $safeStderr.Violation -or $digestObserved
    $safeStdoutText = [regex]::Replace([string]$safeStdout.Text, $digestPattern, '[REDACTED_BY_ACCOUNT_CHECK]')
    $safeStderrText = [regex]::Replace([string]$safeStderr.Text, $digestPattern, '[REDACTED_BY_ACCOUNT_CHECK]')
    [IO.File]::WriteAllText((Join-Path $OutputDirectory 'instrumentation-stdout.txt'), $safeStdoutText, [Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $OutputDirectory 'instrumentation-stderr.txt'), $safeStderrText, [Text.UTF8Encoding]::new($false))
    return [pscustomobject]@{
        launchCount = $launchCount
        timedOut = $timedOut
        exitCode = $exitCode
        launchError = $launchError
        stdout = $safeStdoutText
        stderr = $safeStderrText
        redactionPass = -not $sensitiveShapeObserved
    }
}

Assert-RegularFileHash -Path $scriptPath -ExpectedHash $ExpectedScriptSha256 -ErrorPrefix 'ACCOUNT_CHECK_SCRIPT' | Out-Null
Assert-RegularFileHash -Path $helperPath -ExpectedHash $ExpectedHelperSha256 -ErrorPrefix 'ACCOUNT_CHECK_HELPER' | Out-Null
Assert-RegularFileHash -Path $TestApkPath -ExpectedHash $ExpectedTestApkSha256 -ErrorPrefix 'ACCOUNT_CHECK_TEST_APK' | Out-Null

# Import only the hash-checked helper library before reading the process-only
# expected value or creating any adb process.  The private module prevents the
# helper's parameter variables from overwriting this runner's parameters, and
# -LibraryOnly cannot dispatch.
$helperModule = New-Module -Name ('P5EAccountCheckHelper_' + [Guid]::NewGuid().ToString('N')) -ScriptBlock {
    param([Parameter(Mandatory = $true)][string]$HelperFile)
    . $HelperFile -LibraryOnly
    Export-ModuleMember -Function @(
        'Get-P5EAccountFingerprint',
        'New-P5EAdbArgumentList',
        'Set-P5EProcessStartInfoArguments',
        'Protect-P5ECaptureText'
    )
} -ArgumentList $helperPath
Import-Module $helperModule -Force | Out-Null

if ($Serial -cne '15e84958') { throw 'ACCOUNT_CHECK_SERIAL_MISMATCH_STOP' }
if (-not (Get-Command $AdbPath -ErrorAction SilentlyContinue)) { throw 'ACCOUNT_CHECK_ADB_MISSING_STOP' }

$expected = Get-P5EAccountFingerprint
if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
    $EvidenceDirectory = Join-Path 'D:\P5E-private' ('account-check-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff') + '-' + [Guid]::NewGuid().ToString('N'))
}
$EvidenceDirectory = [IO.Path]::GetFullPath($EvidenceDirectory)
New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
if (@(Get-ChildItem -LiteralPath $EvidenceDirectory -Force).Count -ne 0) { throw 'ACCOUNT_CHECK_EVIDENCE_DIRECTORY_NOT_EMPTY_STOP' }

$remoteTokens = @(
    'am', 'instrument', '-w', '-r',
    '-e', 'class', $accountCheckClass,
    '-e', 'p5e_account_check', 'YES',
    '-e', 'p5e_expected_endpoint_account_fingerprint', $expected,
    $testRunner)
$adbArguments = New-P5EAdbArgumentList -Serial $Serial -RemoteCommandTokens $remoteTokens
$run = Invoke-AccountCheckProcess -Arguments $adbArguments -OutputDirectory $EvidenceDirectory -SensitiveExpected $expected

$stdout = [string]$run.stdout
$matches = @([regex]::Matches($stdout, 'p5e\.account\.result=(MATCH|MISMATCH)'))
$result = if ($matches.Count -eq 1) { $matches[0].Groups[1].Value } else { '' }
$typedOutcome = if (-not $run.redactionPass) { 'ACCOUNT_CHECK_REDACTION_FAILURE' }
    elseif ($run.launchCount -eq 0) { 'ACCOUNT_CHECK_FAILED_BEFORE_LAUNCH' }
    elseif ($run.timedOut) { 'ACCOUNT_CHECK_TIMEOUT_NOT_PROVEN' }
    elseif ($null -eq $run.exitCode -or $run.exitCode -ne 0) { 'ACCOUNT_CHECK_NONZERO_NOT_PROVEN' }
    elseif ($result -notin @('MATCH', 'MISMATCH')) { 'ACCOUNT_CHECK_RESULT_MISSING_NOT_PROVEN' }
    else { 'ACCOUNT_CHECK_COMPLETED' }

$outcome = [ordered]@{
    schemaVersion = 'p5e.account-check.result.v1'
    outcome = $typedOutcome
    result = if ($typedOutcome -eq 'ACCOUNT_CHECK_COMPLETED') { $result } else { 'NOT_PROVEN' }
    serial = $Serial
    launchCount = [int]$run.launchCount
    timedOut = [bool]$run.timedOut
    exitCode = $run.exitCode
    redactionPass = [bool]$run.redactionPass
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
if ($typedOutcome -ne 'ACCOUNT_CHECK_COMPLETED') { exit 30 }
exit 0
