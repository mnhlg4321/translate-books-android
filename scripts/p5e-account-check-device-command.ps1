[CmdletBinding()]
param(
    [string]$EvidenceDirectory = 'D:\P5E-private\p5e-account-check-device-event-20260924-loader-contract-fix-01',
    [string]$AndroidSdkPath = '',
    [string]$AdbPath = '',
    [string]$ApkSignerPath = '',
    [string]$PowerShellPath = '',

    [ValidateRange(4096, 4194304)]
    [int]$MaxCaptureBytes = 262144,

    [ValidateRange(100, 120000)]
    [int]$ChildCleanupTimeoutMilliseconds = 5000,

    [ValidateRange(100, 120000)]
    [int]$ChildCaptureDrainTimeoutMilliseconds = 5000
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$accountEnvironmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$targetPackage = 'com.ml.tblandroidtxt'
$testPackage = 'com.ml.tblandroidtxt.test'
$serial = '15e84958'
$targetVersionCode = 207
$targetSignatureToken = 'abebea4b'
$testSignatureToken = 'abebea4b'
$targetApkSha256 = '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD'
$targetCertificateSha256 = '47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155'
$testApkSha256 = '058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8'
$testCertificateSha256 = '47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155'
$runnerSha256 = '96E6B3B449D00B75989D3AD4E9403EA9510E504FBE90A53D6825E72E09B71E65'
$helperSha256 = '5B621B339F6234415AC7B72C0816F2CA5F657DFCAB8F01C4D6BBC10F84172E34'
$loaderSha256 = '1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D'
$preflightSha256 = 'A82FAE2232C9F5C579CBC2D012B03EB55CCFEB567DAA399A89DEBC661BD235D5'
$accountSourceSha256 = '2F4BF9AD27CF5DF93D89456767423271907598EA209A0AD6E4C27599BC20063C'
$testApkPath = 'D:\P5E-private\p5e-account-check-install-replacement-20260917-113601720-80675f1e5c4746da97f0a884ee319472\installed-test-package.apk'
$runnerClassMethod = 'com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest#ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch'
$runnerComponent = 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'
$script:Stage = 'INITIALIZE'
$script:EvidenceReady = $false
$script:CommandReceiptPath = ''
$script:RunnerChildLaunchCount = 0
$script:LastChildRun = $null
$script:Stopwatch = [Diagnostics.Stopwatch]::StartNew()
$script:CommandPath = [IO.Path]::GetFullPath($MyInvocation.MyCommand.Path)

function Get-P5ETypedErrorCode {
    param([Parameter(Mandatory = $true)][System.Management.Automation.ErrorRecord]$ErrorRecord)
    $message = [string]$ErrorRecord.Exception.Message
    if ($message -match '^(?<code>[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*)') { return $Matches['code'] }
    return 'ACCOUNT_CHECK_DEVICE_COMMAND_INTERNAL_STOP'
}

function Assert-P5EPathChainNoReparse {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [switch]$AllowMissingLeaf
    )

    $fullPath = [IO.Path]::GetFullPath($Path)
    $current = $fullPath
    $isLeaf = $true
    while ($true) {
        if (Test-Path -LiteralPath $current) {
            $item = Get-Item -LiteralPath $current -Force -ErrorAction Stop
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw 'ACCOUNT_CHECK_DEVICE_COMMAND_REPARSE_PATH_STOP'
            }
        } elseif ($isLeaf -and -not $AllowMissingLeaf) {
            throw 'ACCOUNT_CHECK_DEVICE_COMMAND_PATH_MISSING_STOP'
        }
        $parent = [IO.Path]::GetDirectoryName($current)
        if ([string]::IsNullOrWhiteSpace($parent) -or
                [StringComparer]::OrdinalIgnoreCase.Equals($parent, $current)) { return $fullPath }
        $current = $parent
        $isLeaf = $false
    }
}

function Assert-P5ERegularFileHash {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedHash,
        [Parameter(Mandatory = $true)][string]$ErrorPrefix
    )

    $fullPath = [IO.Path]::GetFullPath($Path)
    Assert-P5EPathChainNoReparse -Path $fullPath | Out-Null
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) { throw ($ErrorPrefix + '_MISSING_STOP') }
    $item = Get-Item -LiteralPath $fullPath -Force -ErrorAction Stop
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw ($ErrorPrefix + '_NOT_REGULAR_STOP')
    }
    $resolved = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $fullPath -ErrorAction Stop).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($fullPath, $resolved)) {
        throw ($ErrorPrefix + '_PATH_SWAP_STOP')
    }
    $actual = (Get-FileHash -LiteralPath $fullPath -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
    if ($actual -cne $ExpectedHash.ToUpperInvariant()) { throw ($ErrorPrefix + '_HASH_MISMATCH_STOP') }
    return $fullPath
}

function Assert-P5EAvailableRegularFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ErrorCode
    )
    $fullPath = [IO.Path]::GetFullPath($Path)
    Assert-P5EPathChainNoReparse -Path $fullPath | Out-Null
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) { throw $ErrorCode }
    $item = Get-Item -LiteralPath $fullPath -Force -ErrorAction Stop
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) { throw $ErrorCode }
    return $fullPath
}

function Resolve-P5EAndroidTools {
    param(
        [string]$RequestedSdkPath,
        [string]$RequestedAdbPath,
        [string]$RequestedApkSignerPath
    )

    $sdkPath = $null
    if ([string]::IsNullOrWhiteSpace($RequestedAdbPath) -or [string]::IsNullOrWhiteSpace($RequestedApkSignerPath)) {
        if (-not [string]::IsNullOrWhiteSpace($RequestedSdkPath)) {
            $sdkPath = [IO.Path]::GetFullPath($RequestedSdkPath)
        } else {
            $sdkPath = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
                'C:\Users\ADMIN\AppData\Local\Android\Sdk') |
                Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path -LiteralPath $_ -PathType Container) } |
                Select-Object -First 1
        }
        if ([string]::IsNullOrWhiteSpace([string]$sdkPath)) { throw 'ACCOUNT_CHECK_DEVICE_COMMAND_ANDROID_SDK_MISSING_STOP' }
        Assert-P5EPathChainNoReparse -Path $sdkPath | Out-Null
        $sdkPath = (Resolve-Path -LiteralPath $sdkPath -ErrorAction Stop).Path
    }
    $adb = if ([string]::IsNullOrWhiteSpace($RequestedAdbPath)) {
        Join-Path $sdkPath 'platform-tools\adb.exe'
    } else { $RequestedAdbPath }
    $signer = if ([string]::IsNullOrWhiteSpace($RequestedApkSignerPath)) {
        $root = Join-Path $sdkPath 'build-tools'
        if (-not (Test-Path -LiteralPath $root -PathType Container)) { throw 'ACCOUNT_CHECK_DEVICE_COMMAND_BUILD_TOOLS_MISSING_STOP' }
        $candidate = Get-ChildItem -LiteralPath $root -Directory -ErrorAction Stop |
            Sort-Object Name -Descending |
            ForEach-Object {
                if (($_.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) {
                    $path = Join-Path $_.FullName 'apksigner.bat'
                    if (Test-Path -LiteralPath $path -PathType Leaf) { $path }
                }
            } | Select-Object -First 1
        if ([string]::IsNullOrWhiteSpace([string]$candidate)) { throw 'ACCOUNT_CHECK_DEVICE_COMMAND_APKSIGNER_MISSING_STOP' }
        [string]$candidate
    } else { $RequestedApkSignerPath }
    return [pscustomobject]@{
        SdkPath = if ($null -eq $sdkPath) { '' } else { [string]$sdkPath }
        AdbPath = Assert-P5EAvailableRegularFile -Path ([string]$adb) -ErrorCode 'ACCOUNT_CHECK_DEVICE_COMMAND_ADB_MISSING_STOP'
        ApkSignerPath = Assert-P5EAvailableRegularFile -Path ([string]$signer) -ErrorCode 'ACCOUNT_CHECK_DEVICE_COMMAND_APKSIGNER_MISSING_STOP'
    }
}

function ConvertTo-P5EWindowsProcessArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $builder = [Text.StringBuilder]::new()
    [void]$builder.Append('"')
    $backslashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') { $backslashes++; continue }
        if ($character -eq '"') {
            [void]$builder.Append(('\' * (2 * $backslashes + 1)))
            [void]$builder.Append('"')
            $backslashes = 0
            continue
        }
        if ($backslashes -gt 0) { [void]$builder.Append(('\' * $backslashes)); $backslashes = 0 }
        [void]$builder.Append($character)
    }
    if ($backslashes -gt 0) { [void]$builder.Append(('\' * (2 * $backslashes))) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Set-P5EProcessStartInfoArguments {
    param(
        [Parameter(Mandatory = $true)][Diagnostics.ProcessStartInfo]$StartInfo,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList
    )
    if ($null -ne $StartInfo.PSObject.Properties['ArgumentList']) {
        foreach ($argument in $ArgumentList) { [void]$StartInfo.ArgumentList.Add([string]$argument) }
    } else {
        $StartInfo.Arguments = [string]::Join(' ', @($ArgumentList | ForEach-Object {
            ConvertTo-P5EWindowsProcessArgument -Value ([string]$_)
        }))
    }
}

function New-P5EStreamCapture {
    param([Parameter(Mandatory = $true)][IO.Stream]$Stream, [Parameter(Mandatory = $true)][int]$MaxBytes)
    return [pscustomobject]@{
        Stream = $Stream
        Buffer = New-Object byte[] 8192
        Task = $null
        Complete = $false
        ErrorClass = ''
        TotalBytes = [long]0
        StoredBytes = [long]0
        MaxBytes = $MaxBytes
        Overflow = $false
        Data = [IO.MemoryStream]::new()
    }
}

function Start-P5EStreamRead { param([Parameter(Mandatory = $true)]$State)
    if ($State.Complete -or $null -ne $State.Task) { return }
    try { $State.Task = $State.Stream.ReadAsync($State.Buffer, 0, $State.Buffer.Length) }
    catch { $State.ErrorClass = $_.Exception.GetType().Name; $State.Complete = $true }
}

function Update-P5EStreamCapture { param([Parameter(Mandatory = $true)]$State)
    if ($State.Complete -or $null -eq $State.Task -or -not $State.Task.IsCompleted) { return }
    try {
        # GetResult is called only after IsCompleted; it cannot introduce an unbounded wait.
        $count = [int]$State.Task.GetAwaiter().GetResult()
        $State.Task = $null
        if ($count -le 0) { $State.Complete = $true; return }
        $State.TotalBytes += $count
        $remaining = [long]$State.MaxBytes - $State.StoredBytes
        if ($remaining -gt 0) {
            $toStore = [int][Math]::Min([long]$count, $remaining)
            $State.Data.Write($State.Buffer, 0, $toStore)
            $State.StoredBytes += $toStore
            if ($toStore -lt $count) { $State.Overflow = $true }
        } else { $State.Overflow = $true }
        Start-P5EStreamRead -State $State
    } catch {
        $State.Task = $null
        $State.ErrorClass = $_.Exception.GetType().Name
        $State.Complete = $true
    }
}

function Stop-P5EProcessTree {
    param([Parameter(Mandatory = $true)][Diagnostics.Process]$Process, [Parameter(Mandatory = $true)][int]$TimeoutMilliseconds)
    $taskkill = Join-Path $env:SystemRoot 'System32\taskkill.exe'
    if (Test-Path -LiteralPath $taskkill -PathType Leaf) {
        $info = [Diagnostics.ProcessStartInfo]::new()
        $info.FileName = $taskkill
        $info.Arguments = '/PID ' + [string]$Process.Id + ' /T /F'
        $info.UseShellExecute = $false
        $info.CreateNoWindow = $true
        $killer = [Diagnostics.Process]::new(); $killer.StartInfo = $info
        try {
            if (-not $killer.Start() -or -not $killer.WaitForExit($TimeoutMilliseconds) -or $killer.ExitCode -ne 0) { return 'TREE_KILL_FAILED' }
        } catch { return 'TREE_KILL_FAILED' } finally { $killer.Dispose() }
        try {
            if (-not $Process.HasExited) { [void]$Process.WaitForExit($TimeoutMilliseconds) }
            if ($Process.HasExited) { return 'TREE_KILLED_CONFIRMED' }
        } catch { }
        return 'TREE_KILL_NOT_CONFIRMED'
    }
    try {
        if (-not $Process.HasExited) { $Process.Kill() }
        if ($Process.WaitForExit($TimeoutMilliseconds) -and $Process.HasExited) { return 'PARENT_ONLY_UNCERTAIN' }
    } catch { }
    return 'PARENT_ONLY_UNCERTAIN'
}

function Invoke-P5EChildBounded {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][int]$TimeoutMilliseconds,
        [AllowEmptyCollection()][string[]]$ClearInheritedEnvironmentVariableNames = @()
    )

    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $FilePath
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($name in @($ClearInheritedEnvironmentVariableNames | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        [void]$info.EnvironmentVariables.Remove([string]$name)
    }
    Set-P5EProcessStartInfoArguments -StartInfo $info -ArgumentList $ArgumentList
    $process = [Diagnostics.Process]::new(); $process.StartInfo = $info
    $stdout = $null; $stderr = $null; $launchCount = 0; $exitCode = $null
    $timedOut = $false; $captureDrainTimedOut = $false; $cleanupStatus = 'NOT_REQUIRED'; $treeStatus = 'NOT_REQUIRED'
    $startedAt = [Diagnostics.Stopwatch]::GetTimestamp()
    try {
        if (-not $process.Start()) { throw 'ACCOUNT_CHECK_DEVICE_COMMAND_CHILD_START_STOP' }
        $launchCount = 1
        $stdout = New-P5EStreamCapture -Stream $process.StandardOutput.BaseStream -MaxBytes $MaxCaptureBytes
        $stderr = New-P5EStreamCapture -Stream $process.StandardError.BaseStream -MaxBytes $MaxCaptureBytes
        Start-P5EStreamRead -State $stdout; Start-P5EStreamRead -State $stderr
        $drainDeadline = $null
        while ($true) {
            Update-P5EStreamCapture -State $stdout; Update-P5EStreamCapture -State $stderr
            $elapsed = [int64](([Diagnostics.Stopwatch]::GetTimestamp() - $startedAt) * 1000 / [Diagnostics.Stopwatch]::Frequency)
            $exited = $false; try { $exited = $process.HasExited } catch { }
            if (-not $exited -and -not $timedOut -and $elapsed -ge $TimeoutMilliseconds) {
                $timedOut = $true
                $treeStatus = Stop-P5EProcessTree -Process $process -TimeoutMilliseconds $ChildCleanupTimeoutMilliseconds
                $cleanupStatus = if ($treeStatus -eq 'TREE_KILLED_CONFIRMED') { 'TREE_CLEANUP_CONFIRMED' } else { 'CLEANUP_UNCERTAIN' }
                $drainDeadline = $elapsed + $ChildCaptureDrainTimeoutMilliseconds
            }
            if ($exited -and $null -eq $drainDeadline) { $drainDeadline = $elapsed + $ChildCaptureDrainTimeoutMilliseconds }
            if ($exited -and $stdout.Complete -and $stderr.Complete) { break }
            if ($null -ne $drainDeadline -and $elapsed -ge [int64]$drainDeadline) { $captureDrainTimedOut = $true; break }
            Start-Sleep -Milliseconds 10
        }
        try { if ($process.HasExited) { $exitCode = [int]$process.ExitCode } } catch { }
    } catch { $cleanupStatus = 'PROCESS_SUPERVISOR_EXCEPTION' }
    finally {
        if ($null -ne $stdout) { Update-P5EStreamCapture -State $stdout }
        if ($null -ne $stderr) { Update-P5EStreamCapture -State $stderr }
        $process.Dispose()
    }
    $stdoutText = ''; $stderrText = ''; $stdoutError = ''; $stderrError = ''; $stdoutBytes = 0L; $stderrBytes = 0L; $stdoutOverflow = $false; $stderrOverflow = $false
    $stdoutComplete = $false; $stderrComplete = $false
    if ($null -ne $stdout) {
        $stdoutComplete = [bool]$stdout.Complete; $stdoutError = [string]$stdout.ErrorClass; $stdoutBytes = [long]$stdout.TotalBytes; $stdoutOverflow = [bool]$stdout.Overflow
        if ($stdoutComplete -and [string]::IsNullOrWhiteSpace($stdoutError)) { try { $stdoutText = [Text.UTF8Encoding]::new($false, $true).GetString($stdout.Data.ToArray()) } catch { $stdoutError = 'UTF8_DECODE_FAILED' } }
        $stdout.Data.Dispose()
    }
    if ($null -ne $stderr) {
        $stderrComplete = [bool]$stderr.Complete; $stderrError = [string]$stderr.ErrorClass; $stderrBytes = [long]$stderr.TotalBytes; $stderrOverflow = [bool]$stderr.Overflow
        if ($stderrComplete -and [string]::IsNullOrWhiteSpace($stderrError)) { try { $stderrText = [Text.UTF8Encoding]::new($false, $true).GetString($stderr.Data.ToArray()) } catch { $stderrError = 'UTF8_DECODE_FAILED' } }
        $stderr.Data.Dispose()
    }
    $captureBounded = [bool]($stdoutComplete -and $stderrComplete -and [string]::IsNullOrWhiteSpace($stdoutError) -and [string]::IsNullOrWhiteSpace($stderrError) -and -not $captureDrainTimedOut)
    $captureOverflow = [bool]($stdoutOverflow -or $stderrOverflow)
    $outcome = if ($launchCount -eq 0) { 'FAILED_BEFORE_LAUNCH' } elseif ($timedOut) { 'TIMEOUT' } elseif (-not $captureBounded) { 'CAPTURE_NOT_BOUNDED' } elseif ($captureOverflow) { 'CAPTURE_OVERFLOW' } elseif ($null -eq $exitCode) { 'PROCESS_EXIT_UNKNOWN' } elseif ($exitCode -eq 0) { 'PROCESS_EXITED_ZERO' } else { 'PROCESS_EXITED_NONZERO' }
    return [pscustomobject]@{
        Outcome = $outcome
        LaunchCount = $launchCount
        ExitCode = $exitCode
        TimedOut = $timedOut
        CaptureBounded = $captureBounded
        CaptureOverflow = $captureOverflow
        CaptureDrainTimedOut = $captureDrainTimedOut
        CleanupStatus = $cleanupStatus
        CleanupConfirmed = $cleanupStatus -in @('NOT_REQUIRED', 'TREE_CLEANUP_CONFIRMED')
        ProcessTreeStatus = $treeStatus
        StdoutText = $stdoutText
        StderrText = $stderrText
        StdoutBytes = $stdoutBytes
        StderrBytes = $stderrBytes
        ElapsedMilliseconds = [int64](([Diagnostics.Stopwatch]::GetTimestamp() - $startedAt) * 1000 / [Diagnostics.Stopwatch]::Frequency)
    }
}

function New-P5EEmptyDirectory {
    param([Parameter(Mandatory = $true)][string]$Path)
    $full = [IO.Path]::GetFullPath($Path)
    Assert-P5EPathChainNoReparse -Path $full -AllowMissingLeaf | Out-Null
    if (Test-Path -LiteralPath $full) {
        $item = Get-Item -LiteralPath $full -Force -ErrorAction Stop
        if (-not $item.PSIsContainer -or @((Get-ChildItem -LiteralPath $full -Force -ErrorAction Stop)).Count -ne 0) {
            throw 'ACCOUNT_CHECK_DEVICE_COMMAND_EVIDENCE_NOT_EMPTY_STOP'
        }
    } else { New-Item -ItemType Directory -Path $full -ErrorAction Stop | Out-Null }
    return $full
}

function New-P5EAttemptMarker {
    param([Parameter(Mandatory = $true)][string]$Path)
    Assert-P5EPathChainNoReparse -Path $Path -AllowMissingLeaf | Out-Null
    $stream = $null
    try {
        $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $bytes = [Text.UTF8Encoding]::new($false).GetBytes('P5E_ACCOUNT_CHECK_ATTEMPT=1`n')
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Flush()
    } catch [IO.IOException] { throw 'ACCOUNT_CHECK_DEVICE_COMMAND_ATTEMPT_ALREADY_EXISTS_STOP' }
    finally { if ($null -ne $stream) { $stream.Dispose() } }
}

function Get-P5EJson {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$MissingCode)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw $MissingCode }
    try { return Get-Content -LiteralPath $Path -Raw -ErrorAction Stop | ConvertFrom-Json -ErrorAction Stop }
    catch { throw ($MissingCode + '_INVALID_STOP') }
}

function Write-P5ECommandReceipt {
    param([Parameter(Mandatory = $true)]$Value)
    $path = Join-Path ([IO.Path]::GetFullPath($EvidenceDirectory)) 'ACCOUNT_CHECK_DEVICE_COMMAND_RESULT.json'
    if (Test-Path -LiteralPath $path) { throw 'ACCOUNT_CHECK_DEVICE_COMMAND_RECEIPT_ALREADY_EXISTS_STOP' }
    [IO.File]::WriteAllText($path, ($Value | ConvertTo-Json -Depth 12 -Compress), [Text.UTF8Encoding]::new($false))
    $script:CommandReceiptPath = $path
}

function Get-P5ECommandReceiptBase {
    param(
        [Parameter(Mandatory = $true)][string]$Status,
        [Parameter(Mandatory = $true)][string]$TypedOutcome,
        [string]$PreflightStatus = '',
        [string]$PreflightTypedError = '',
        [string]$AccountResult = 'NOT_PROVEN',
        [int]$AccountLaunchCount = -1,
        [string]$PreflightReceiptSha256 = '',
        [string]$RunnerReceiptSha256 = ''
    )
    return [ordered]@{
        schemaVersion = 'p5e.account-check.device-command.result.v1'
        status = $Status
        typedOutcome = $TypedOutcome
        serial = $serial
        targetPackage = $targetPackage
        testPackage = $testPackage
        targetVersionCode = $targetVersionCode
        targetSignatureToken = $targetSignatureToken
        testSignatureToken = $testSignatureToken
        targetApkSha256 = $targetApkSha256
        targetCertificateSha256 = $targetCertificateSha256
        testApkSha256 = $testApkSha256
        testCertificateSha256 = $testCertificateSha256
        runnerSha256 = $runnerSha256
        helperSha256 = $helperSha256
        loaderSha256 = $loaderSha256
        preflightSha256 = $preflightSha256
        accountSourceSha256 = $accountSourceSha256
        commandSha256 = (Get-FileHash -LiteralPath $script:CommandPath -Algorithm SHA256).Hash.ToUpperInvariant()
        evidenceDirectory = [IO.Path]::GetFullPath($EvidenceDirectory)
        preflightEvidenceDirectory = Join-Path ([IO.Path]::GetFullPath($EvidenceDirectory)) 'preflight'
        runnerEvidenceDirectory = Join-Path ([IO.Path]::GetFullPath($EvidenceDirectory)) 'account-runner'
        preflightStatus = $PreflightStatus
        preflightTypedError = $PreflightTypedError
        preflightReceiptSha256 = $PreflightReceiptSha256
        accountResult = $AccountResult
        accountLaunchCount = if ($AccountLaunchCount -lt 0) { $null } else { $AccountLaunchCount }
        runnerProcessLaunchCount = [int]$script:RunnerChildLaunchCount
        runnerReceiptSha256 = $RunnerReceiptSha256
        runnerProcessOutcome = if ($script:RunnerChildLaunchCount -eq 0 -or $null -eq $script:LastChildRun) { '' } else { $script:LastChildRun.Outcome }
        runnerProcessExitCode = if ($script:RunnerChildLaunchCount -eq 0 -or $null -eq $script:LastChildRun) { $null } else { $script:LastChildRun.ExitCode }
        runnerCaptureBounded = if ($script:RunnerChildLaunchCount -eq 0 -or $null -eq $script:LastChildRun) { $false } else { [bool]$script:LastChildRun.CaptureBounded }
        runnerTimedOut = if ($script:RunnerChildLaunchCount -eq 0 -or $null -eq $script:LastChildRun) { $false } else { [bool]$script:LastChildRun.TimedOut }
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
        installAttempts = 0
        elapsedMilliseconds = [long]$script:Stopwatch.ElapsedMilliseconds
        attemptMarker = Join-Path ([IO.Path]::GetFullPath($EvidenceDirectory)) 'ACCOUNT_CHECK_ATTEMPT.marker'
    }
}

try {
    $script:Stage = 'STATIC_PINS'
    $repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $runnerPath = Assert-P5ERegularFileHash -Path (Join-Path $PSScriptRoot 'p5e-account-check.ps1') -ExpectedHash $runnerSha256 -ErrorPrefix 'ACCOUNT_CHECK_RUNNER'
    $helperPath = Assert-P5ERegularFileHash -Path (Join-Path $PSScriptRoot 'p5e-raw-live-supervisor.ps1') -ExpectedHash $helperSha256 -ErrorPrefix 'ACCOUNT_CHECK_HELPER'
    $loaderPath = Assert-P5ERegularFileHash -Path (Join-Path $PSScriptRoot 'p5e-load-expected-digest.ps1') -ExpectedHash $loaderSha256 -ErrorPrefix 'ACCOUNT_CHECK_LOADER'
    $preflightPath = Assert-P5ERegularFileHash -Path (Join-Path $PSScriptRoot 'p5e-account-check-device-preflight.ps1') -ExpectedHash $preflightSha256 -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT'
    $accountSourcePath = Assert-P5ERegularFileHash -Path (Join-Path $repoRoot 'app\src\androidTest\java\com\ml\tblandroidtxt\EditorialP5EAccountCheckOnlyInstrumentedTest.java') -ExpectedHash $accountSourceSha256 -ErrorPrefix 'ACCOUNT_CHECK_ANDROID_SOURCE'
    $testApkPath = Assert-P5ERegularFileHash -Path $testApkPath -ExpectedHash $testApkSha256 -ErrorPrefix 'ACCOUNT_CHECK_TEST_APK'
    $runnerSource = Get-Content -LiteralPath $runnerPath -Raw -ErrorAction Stop
    if ($runnerSource.IndexOf($runnerClassMethod, [StringComparison]::Ordinal) -lt 0 -or
            $runnerSource.IndexOf($runnerComponent, [StringComparison]::Ordinal) -lt 0) {
        throw 'ACCOUNT_CHECK_DEVICE_COMMAND_RUNNER_IDENTITY_STOP'
    }
    $hostPowerShell = if ([string]::IsNullOrWhiteSpace($PowerShellPath)) {
        @('C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe', (Join-Path $PSHOME 'powershell.exe'), (Join-Path $PSHOME 'pwsh.exe')) |
            Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    } else { $PowerShellPath }
    $hostPowerShell = Assert-P5EAvailableRegularFile -Path ([string]$hostPowerShell) -ErrorCode 'ACCOUNT_CHECK_DEVICE_COMMAND_POWERSHELL_MISSING_STOP'
    $tools = Resolve-P5EAndroidTools -RequestedSdkPath $AndroidSdkPath -RequestedAdbPath $AdbPath -RequestedApkSignerPath $ApkSignerPath

    $script:Stage = 'EXPECTED_PROCESS_SHAPE'
    $expectedProbe = [Environment]::GetEnvironmentVariable($accountEnvironmentName, 'Process')
    if ([string]::IsNullOrWhiteSpace($expectedProbe)) { throw 'EXPECTED_LOAD_OWNER_REPORTED_PROCESS_MISSING_STOP' }
    if ($expectedProbe -notmatch '^[0-9a-fA-F]{64}$') { throw 'EXPECTED_LOAD_OWNER_REPORTED_PROCESS_SHAPE_STOP' }
    $expectedProbe = $null

    $script:Stage = 'EVIDENCE_INIT'
    $EvidenceDirectory = New-P5EEmptyDirectory -Path $EvidenceDirectory
    $preflightEvidence = New-P5EEmptyDirectory -Path (Join-Path $EvidenceDirectory 'preflight')
    $runnerEvidence = New-P5EEmptyDirectory -Path (Join-Path $EvidenceDirectory 'account-runner')
    $script:EvidenceReady = $true

    $script:Stage = 'PREFLIGHT_CHILD'
    $preflightArgs = @(
        '-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $preflightPath,
        '-ExpectedScriptSha256', $preflightSha256,
        '-ReferenceTestApkPath', $testApkPath,
        '-ExpectedTestApkSha256', $testApkSha256,
        '-ExpectedTestCertificateSha256', $testCertificateSha256,
        '-ExpectedTargetApkSha256', $targetApkSha256,
        '-ExpectedTargetCertificateSha256', $targetCertificateSha256,
        '-Serial', $serial,
        '-ExpectedTargetVersionCode', [string]$targetVersionCode,
        '-ExpectedTargetSignatureToken', $targetSignatureToken,
        '-ExpectedTestSignatureToken', $testSignatureToken,
        '-AdbPath', $tools.AdbPath,
        '-ApkSignerPath', $tools.ApkSignerPath,
        '-EvidenceDirectory', $preflightEvidence
    )
    $preflightRun = Invoke-P5EChildBounded -FilePath $hostPowerShell -ArgumentList $preflightArgs `
        -TimeoutMilliseconds 360000 -ClearInheritedEnvironmentVariableNames @($accountEnvironmentName)
    $script:LastChildRun = $preflightRun
    $preflightReceiptPath = Join-Path $preflightEvidence 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json'
    $preflightReceipt = $null
    if (Test-Path -LiteralPath $preflightReceiptPath -PathType Leaf) { $preflightReceipt = Get-P5EJson -Path $preflightReceiptPath -MissingCode 'ACCOUNT_CHECK_PREFLIGHT_RECEIPT' }
    if ($preflightRun.Outcome -ne 'PROCESS_EXITED_ZERO' -or $null -eq $preflightReceipt -or $preflightReceipt.status -ne 'PASS') {
        $typed = if ($null -ne $preflightReceipt -and -not [string]::IsNullOrWhiteSpace([string]$preflightReceipt.typedError)) {
            [string]$preflightReceipt.typedError
        } elseif ($preflightRun.TimedOut) { 'ACCOUNT_CHECK_PREFLIGHT_TIMEOUT_STOP' }
        elseif (-not $preflightRun.CaptureBounded -or $preflightRun.CaptureOverflow) { 'ACCOUNT_CHECK_PREFLIGHT_CAPTURE_STOP' }
        else { 'ACCOUNT_CHECK_PREFLIGHT_STOP' }
        $receipt = Get-P5ECommandReceiptBase -Status 'STOP' -TypedOutcome $typed `
            -PreflightStatus $(if ($null -eq $preflightReceipt) { 'NOT_PROVEN' } else { [string]$preflightReceipt.status }) `
            -PreflightTypedError $(if ($null -eq $preflightReceipt) { $typed } else { [string]$preflightReceipt.typedError })
        Write-P5ECommandReceipt -Value $receipt
        Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND=STOP'
        Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND_OUTCOME=' + $typed)
        exit 2
    }
    if ([string]$preflightReceipt.serial -cne $serial -or [int]$preflightReceipt.deviceReadAttempts -ne 7 -or
            [int]$preflightReceipt.attemptCount -ne 1 -or [string]$preflightReceipt.scriptSha256 -cne $preflightSha256 -or
            [string]$preflightReceipt.targetApkSha256 -cne $targetApkSha256 -or
            [string]$preflightReceipt.targetCertificateSha256 -cne $targetCertificateSha256 -or
            [string]$preflightReceipt.testApkSha256 -cne $testApkSha256 -or
            [string]$preflightReceipt.testCertificateSha256 -cne $testCertificateSha256 -or
            [int]$preflightReceipt.targetVersionCode -ne $targetVersionCode -or
             [string]$preflightReceipt.targetSignatureToken -cne $targetSignatureToken -or
             [string]$preflightReceipt.testSignatureToken -cne $testSignatureToken -or
             -not [bool]$preflightReceipt.targetMetadataPackagePresent -or
             [string]$preflightReceipt.targetMetadataReason -cne 'PASS' -or
             [int]$preflightReceipt.targetMetadataVersionCandidateCount -ne 1 -or
             [int]$preflightReceipt.targetMetadataSignatureCandidateCount -ne 1 -or
             -not [bool]$preflightReceipt.testMetadataPackagePresent -or
             [string]$preflightReceipt.testMetadataReason -cne 'PASS' -or
             [int]$preflightReceipt.testMetadataVersionCandidateCount -ne 1 -or
             [int]$preflightReceipt.testMetadataSignatureCandidateCount -ne 1 -or
             [int]$preflightReceipt.installAttempts -ne 0 -or [int]$preflightReceipt.providerCalls -ne 0 -or
            [int]$preflightReceipt.dbWrites -ne 0 -or [int]$preflightReceipt.rawDispatches -ne 0 -or
            [int]$preflightReceipt.productionPackageOperations -ne 0) {
        throw 'ACCOUNT_CHECK_PREFLIGHT_RECEIPT_PIN_STOP'
    }
    $preflightReceiptSha = (Get-FileHash -LiteralPath $preflightReceiptPath -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()

    $script:Stage = 'ACCOUNT_ATTEMPT_MARKER'
    New-P5EAttemptMarker -Path (Join-Path $EvidenceDirectory 'ACCOUNT_CHECK_ATTEMPT.marker')

    $script:Stage = 'ACCOUNT_RUNNER_CHILD'
    $runnerArgs = @(
        '-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $runnerPath,
        '-ExpectedScriptSha256', $runnerSha256,
        '-ExpectedHelperSha256', $helperSha256,
        '-TestApkPath', $testApkPath,
        '-ExpectedTestApkSha256', $testApkSha256,
        '-Serial', $serial,
        '-AdbPath', $tools.AdbPath,
        '-EvidenceDirectory', $runnerEvidence
    )
    $script:RunnerChildLaunchCount = 1
    $runnerRun = Invoke-P5EChildBounded -FilePath $hostPowerShell -ArgumentList $runnerArgs -TimeoutMilliseconds 135000
    $script:LastChildRun = $runnerRun
    $runnerReceiptPath = Join-Path $runnerEvidence 'ACCOUNT_CHECK_RESULT.json'
    $runnerReceipt = $null
    if (Test-Path -LiteralPath $runnerReceiptPath -PathType Leaf) { $runnerReceipt = Get-P5EJson -Path $runnerReceiptPath -MissingCode 'ACCOUNT_CHECK_RESULT' }
    $runnerReceiptSha = if ($null -eq $runnerReceipt) { '' } else { (Get-FileHash -LiteralPath $runnerReceiptPath -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant() }
    $accountResult = if ($null -eq $runnerReceipt) { 'NOT_PROVEN' } else { [string]$runnerReceipt.result }
    $accountLaunchCount = -1
    if ($null -ne $runnerReceipt -and $runnerReceipt.PSObject.Properties['launchCount']) { $accountLaunchCount = [int]$runnerReceipt.launchCount }
    $typedOutcome = if ($null -eq $runnerReceipt -or $runnerRun.Outcome -ne 'PROCESS_EXITED_ZERO') { 'ACCOUNT_CHECK_NOT_PROVEN_STOP' }
        elseif ([string]$runnerReceipt.outcome -notin @('ACCOUNT_CHECK_COMPLETED_MATCH', 'ACCOUNT_CHECK_COMPLETED_MISMATCH')) { 'ACCOUNT_CHECK_NOT_PROVEN_STOP' }
        elseif ([string]$runnerReceipt.serial -cne $serial -or [int]$runnerReceipt.launchCount -ne 1 -or
                [bool]$runnerReceipt.expectedInAdbArgv -or [bool]$runnerReceipt.expectedInAdbChildEnvironment -or
                -not [bool]$runnerReceipt.captureBounded -or -not [bool]$runnerReceipt.inputWriteCompleted -or
                -not [bool]$runnerReceipt.terminalSuccess -or -not [bool]$runnerReceipt.testFinished -or
                -not [bool]$runnerReceipt.testIdentityPass -or [int]$runnerReceipt.resultTokenCount -ne 1 -or
                [int]$runnerReceipt.failureMarkerCount -ne 0 -or -not [bool]$runnerReceipt.redactionPass -or
                [string]$runnerReceipt.scriptSha256 -ine $runnerSha256 -or
                [string]$runnerReceipt.helperSha256 -ine $helperSha256 -or
                [string]$runnerReceipt.testApkSha256 -ine $testApkSha256) { 'ACCOUNT_CHECK_NOT_PROVEN_STOP' }
        else { [string]$runnerReceipt.outcome }
    $status = if ($typedOutcome -in @('ACCOUNT_CHECK_COMPLETED_MATCH', 'ACCOUNT_CHECK_COMPLETED_MISMATCH')) { 'PASS' } else { 'STOP' }
    $receipt = Get-P5ECommandReceiptBase -Status $status -TypedOutcome $typedOutcome `
        -PreflightStatus 'PASS' -PreflightTypedError '' -AccountResult $accountResult `
        -AccountLaunchCount $accountLaunchCount -PreflightReceiptSha256 $preflightReceiptSha -RunnerReceiptSha256 $runnerReceiptSha
    Write-P5ECommandReceipt -Value $receipt
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND=' + $status)
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND_OUTCOME=' + $typedOutcome)
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND_ACCOUNT_RESULT=' + $accountResult)
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_PROVIDER_CALLS=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_DB_WRITES=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_RAW_DISPATCHES=0'
    if ($status -eq 'PASS') { exit 0 } else { exit 3 }
} catch {
    $typedError = Get-P5ETypedErrorCode -ErrorRecord $_
    if ($script:EvidenceReady -and [string]::IsNullOrWhiteSpace($script:CommandReceiptPath)) {
        try {
            $receipt = Get-P5ECommandReceiptBase -Status 'STOP' -TypedOutcome $typedError
            Write-P5ECommandReceipt -Value $receipt
        } catch { }
    }
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND=STOP'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND_OUTCOME=' + $typedError)
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_PROVIDER_CALLS=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_DB_WRITES=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_RAW_DISPATCHES=0'
    exit 2
}
