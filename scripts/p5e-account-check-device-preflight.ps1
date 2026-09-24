[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedScriptSha256,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ReferenceTestApkPath,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTestApkSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTestCertificateSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTargetApkSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTargetCertificateSha256,

    [ValidateNotNullOrEmpty()]
    [string]$Serial = '15e84958',

    [ValidateRange(1, [int]::MaxValue)]
    [int]$ExpectedTargetVersionCode = 207,

    [ValidatePattern('^[0-9a-fA-F]{8,128}$')]
    [string]$ExpectedTargetSignatureToken = 'abebea4b',

    [ValidatePattern('^[0-9a-fA-F]{8,128}$')]
    [string]$ExpectedTestSignatureToken = 'abebea4b',

    [string]$AndroidSdkPath = '',
    [string]$AdbPath = '',
    [string]$ApkSignerPath = '',
    [string]$EvidenceDirectory = '',

    [ValidateRange(1024, 4194304)]
    [int]$MaxCaptureBytes = 262144,

    [ValidateRange(100, 120000)]
    [int]$CleanupTimeoutMilliseconds = 5000,

    [ValidateRange(100, 120000)]
    [int]$CaptureDrainTimeoutMilliseconds = 5000,

    [ValidateRange(100, 30000)]
    [int]$DeviceReadTimeoutMilliseconds = 30000,

    [ValidateRange(100, 60000)]
    [int]$PullTimeoutMilliseconds = 60000
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$expectedProcessValueName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$targetPackage = 'com.ml.tblandroidtxt'
$testPackage = 'com.ml.tblandroidtxt.test'
$script:Stage = 'INITIALIZE'
$script:AttemptedCommandCount = 0L
$script:SucceededCommandCount = 0L
$script:DeviceReadAttempts = 0L
$script:LastProcessRun = $null
$script:ObservedTimeout = $false
$script:ObservedCaptureFailure = $false
$script:ObservedCleanupFailure = $false
$script:EvidenceDirectoryResolved = ''
$script:EvidenceReady = $false
$script:ReceiptPath = ''
$script:Stopwatch = [Diagnostics.Stopwatch]::StartNew()

function New-P5EPackageMetadataState {
    return [pscustomobject]@{
        packagePresent = $false
        reason = 'NOT_EVALUATED'
        layoutFamily = 'NOT_EVALUATED'
        versionCandidateCount = 0
        signatureCandidateCount = 0
        pastSignatureCandidateCount = 0
        wrapperIdentityCandidateCount = 0
        signatureSchemeCandidateCount = 0
        versionFieldCount = 0
        signatureFieldCount = 0
        versionCode = $null
        signatureToken = ''
    }
}

$script:TargetPackageMetadata = New-P5EPackageMetadataState
$script:TestPackageMetadata = New-P5EPackageMetadataState

function Get-P5ETypedErrorCode {
    param([Parameter(Mandatory = $true)][System.Management.Automation.ErrorRecord]$ErrorRecord)

    $message = [string]$ErrorRecord.Exception.Message
    if ($message -match '^(?<code>[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*)') {
        return $Matches['code']
    }
    return 'ACCOUNT_CHECK_PREFLIGHT_INTERNAL_STOP'
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
                throw 'ACCOUNT_CHECK_PREFLIGHT_REPARSE_PATH_STOP'
            }
        } elseif ($isLeaf -and -not $AllowMissingLeaf) {
            throw 'ACCOUNT_CHECK_PREFLIGHT_PATH_MISSING_STOP'
        }

        $parent = [IO.Path]::GetDirectoryName($current)
        if ([string]::IsNullOrWhiteSpace($parent) -or
                [StringComparer]::OrdinalIgnoreCase.Equals($parent, $current)) {
            return $fullPath
        }
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

    $literalPath = [IO.Path]::GetFullPath($Path)
    Assert-P5EPathChainNoReparse -Path $literalPath | Out-Null
    if (-not (Test-Path -LiteralPath $literalPath -PathType Leaf)) {
        throw ($ErrorPrefix + '_MISSING_STOP')
    }
    $item = Get-Item -LiteralPath $literalPath -Force -ErrorAction Stop
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw ($ErrorPrefix + '_NOT_REGULAR_STOP')
    }
    $resolvedPath = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literalPath -ErrorAction Stop).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literalPath, $resolvedPath)) {
        throw ($ErrorPrefix + '_PATH_SWAP_STOP')
    }
    $actualHash = (Get-FileHash -LiteralPath $literalPath -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
    if ($actualHash -cne $ExpectedHash.ToUpperInvariant()) {
        throw ($ErrorPrefix + '_HASH_MISMATCH_STOP')
    }
    return $literalPath
}

function Assert-P5EAvailableRegularFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ErrorCode
    )

    $fullPath = [IO.Path]::GetFullPath($Path)
    Assert-P5EPathChainNoReparse -Path $fullPath | Out-Null
    if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
        throw $ErrorCode
    }
    $item = Get-Item -LiteralPath $fullPath -Force -ErrorAction Stop
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw $ErrorCode
    }
    return $fullPath
}

function Resolve-P5EAndroidSdkPath {
    param([string]$RequestedPath)

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        if (-not (Test-Path -LiteralPath $RequestedPath -PathType Container)) {
            throw 'ACCOUNT_CHECK_PREFLIGHT_ANDROID_SDK_MISSING_STOP'
        }
        Assert-P5EPathChainNoReparse -Path $RequestedPath | Out-Null
        return (Resolve-Path -LiteralPath $RequestedPath -ErrorAction Stop).Path
    }
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
            'C:\Users\ADMIN\AppData\Local\Android\Sdk')) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and
                (Test-Path -LiteralPath $candidate -PathType Container)) {
            Assert-P5EPathChainNoReparse -Path $candidate | Out-Null
            return (Resolve-Path -LiteralPath $candidate -ErrorAction Stop).Path
        }
    }
    throw 'ACCOUNT_CHECK_PREFLIGHT_ANDROID_SDK_MISSING_STOP'
}

function Resolve-P5EBuildTool {
    param(
        [Parameter(Mandatory = $true)][string]$SdkPath,
        [Parameter(Mandatory = $true)][string]$ToolName
    )

    $buildToolsRoot = Join-Path $SdkPath 'build-tools'
    if (-not (Test-Path -LiteralPath $buildToolsRoot -PathType Container)) {
        throw ('ACCOUNT_CHECK_PREFLIGHT_BUILD_TOOL_MISSING_STOP:' + $ToolName)
    }
    Assert-P5EPathChainNoReparse -Path $buildToolsRoot | Out-Null
    $tool = Get-ChildItem -LiteralPath $buildToolsRoot -Directory -ErrorAction Stop |
        Sort-Object Name -Descending |
        ForEach-Object {
            if (($_.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { return }
            $candidate = Join-Path $_.FullName $ToolName
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { $candidate }
        } |
        Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace([string]$tool)) {
        throw ('ACCOUNT_CHECK_PREFLIGHT_BUILD_TOOL_MISSING_STOP:' + $ToolName)
    }
    return Assert-P5EAvailableRegularFile -Path ([string]$tool) `
        -ErrorCode ('ACCOUNT_CHECK_PREFLIGHT_BUILD_TOOL_INVALID_STOP:' + $ToolName)
}

function ConvertTo-P5ESingleQuotedLiteral {
    param([Parameter(Mandatory = $true)][string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

function ConvertTo-P5EWindowsProcessArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)

    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $builder = [Text.StringBuilder]::new()
    [void]$builder.Append('"')
    $backslashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') {
            $backslashes++
            continue
        }
        if ($character -eq '"') {
            [void]$builder.Append(('\' * (2 * $backslashes + 1)))
            [void]$builder.Append('"')
            $backslashes = 0
            continue
        }
        if ($backslashes -gt 0) {
            [void]$builder.Append(('\' * $backslashes))
            $backslashes = 0
        }
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

    $argumentListProperty = $StartInfo.PSObject.Properties['ArgumentList']
    if ($null -ne $argumentListProperty) {
        foreach ($argument in $ArgumentList) { [void]$StartInfo.ArgumentList.Add([string]$argument) }
        return
    }
    $StartInfo.Arguments = [string]::Join(' ', @($ArgumentList | ForEach-Object {
        ConvertTo-P5EWindowsProcessArgument -Value ([string]$_)
    }))
}

function New-P5EStreamCaptureState {
    param(
        [Parameter(Mandatory = $true)][IO.Stream]$Stream,
        [Parameter(Mandatory = $true)][int]$MaxBytes
    )

    return [pscustomobject]@{
        Stream = $Stream
        Buffer = New-Object byte[] 8192
        Task = $null
        Complete = $false
        ErrorClass = ''
        TotalBytes = [long]0
        StoredBytes = [long]0
        MaxBytes = [int]$MaxBytes
        Overflow = $false
        Data = [IO.MemoryStream]::new()
    }
}

function Start-P5EStreamRead {
    param([Parameter(Mandatory = $true)]$State)

    if ($State.Complete -or $null -ne $State.Task) { return }
    try {
        $State.Task = $State.Stream.ReadAsync($State.Buffer, 0, $State.Buffer.Length)
    } catch {
        $State.ErrorClass = $_.Exception.GetType().Name
        $State.Complete = $true
    }
}

function Update-P5EStreamCapture {
    param([Parameter(Mandatory = $true)]$State)

    if ($State.Complete -or $null -eq $State.Task -or -not $State.Task.IsCompleted) {
        return
    }
    try {
        # GetResult is only called after IsCompleted; there is no unbounded wait.
        $count = [int]$State.Task.GetAwaiter().GetResult()
        $State.Task = $null
        if ($count -le 0) {
            $State.Complete = $true
            return
        }
        $State.TotalBytes += $count
        $remaining = [long]$State.MaxBytes - $State.StoredBytes
        if ($remaining -gt 0) {
            $toStore = [int][Math]::Min([long]$count, $remaining)
            $State.Data.Write($State.Buffer, 0, $toStore)
            $State.StoredBytes += $toStore
            if ($toStore -lt $count) { $State.Overflow = $true }
        } else {
            $State.Overflow = $true
        }
        Start-P5EStreamRead -State $State
    } catch {
        $State.Task = $null
        $State.ErrorClass = $_.Exception.GetType().Name
        $State.Complete = $true
    }
}

function Get-P5EStreamText {
    param([Parameter(Mandatory = $true)]$State)

    if (-not $State.Complete -or -not [string]::IsNullOrWhiteSpace([string]$State.ErrorClass)) {
        return ''
    }
    try {
        return [Text.UTF8Encoding]::new($false, $true).GetString($State.Data.ToArray())
    } catch {
        $State.ErrorClass = 'UTF8_DECODE_FAILED'
        return ''
    }
}

function Stop-P5EProcessTree {
    param(
        [Parameter(Mandatory = $true)][Diagnostics.Process]$Process,
        [Parameter(Mandatory = $true)][int]$TimeoutMilliseconds
    )

    $taskkill = Join-Path $env:SystemRoot 'System32\taskkill.exe'
    if (Test-Path -LiteralPath $taskkill -PathType Leaf) {
        $killInfo = [Diagnostics.ProcessStartInfo]::new()
        $killInfo.FileName = $taskkill
        $killInfo.UseShellExecute = $false
        $killInfo.CreateNoWindow = $true
        $killInfo.Arguments = '/PID ' + [string]$Process.Id + ' /T /F'
        $killer = [Diagnostics.Process]::new()
        $killer.StartInfo = $killInfo
        try {
            if (-not $killer.Start()) { return 'TREE_KILL_FAILED' }
            if (-not $killer.WaitForExit($TimeoutMilliseconds)) { return 'TREE_KILL_TIMEOUT' }
            if ($killer.ExitCode -ne 0) { return 'TREE_KILL_FAILED' }
        } catch {
            return 'TREE_KILL_FAILED'
        } finally {
            $killer.Dispose()
        }
        try {
            if (-not $Process.HasExited -and $Process.WaitForExit($TimeoutMilliseconds)) { }
            if ($Process.HasExited) { return 'TREE_KILLED_CONFIRMED' }
        } catch { }
        return 'TREE_KILL_NOT_CONFIRMED'
    }

    # Process.Kill() is a parent-only fallback on Windows PowerShell/.NET
    # Framework. It is deliberately reported as uncertain and is never a pass.
    try {
        if (-not $Process.HasExited) { $Process.Kill() }
        if ($Process.WaitForExit($TimeoutMilliseconds) -and $Process.HasExited) {
            return 'PARENT_ONLY_UNCERTAIN'
        }
    } catch { }
    return 'PARENT_ONLY_UNCERTAIN'
}

function Invoke-P5EBoundedProcess {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][int]$TimeoutMilliseconds,
        [Parameter(Mandatory = $true)][int]$MaxBytes,
        [Parameter(Mandatory = $true)][int]$CleanupMilliseconds,
        [Parameter(Mandatory = $true)][int]$DrainMilliseconds,
        [AllowEmptyCollection()][string[]]$ClearInheritedEnvironmentVariableNames = @()
    )

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($name in @($ClearInheritedEnvironmentVariableNames |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        [void]$startInfo.EnvironmentVariables.Remove([string]$name)
    }
    Set-P5EProcessStartInfoArguments -StartInfo $startInfo -ArgumentList $ArgumentList

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $stdout = $null
    $stderr = $null
    $launchCount = 0
    $exitCode = $null
    $timedOut = $false
    $cleanupStatus = 'NOT_REQUIRED'
    $processTreeStatus = 'NOT_REQUIRED'
    $captureDrainTimedOut = $false
    $launchErrorClass = ''
    $startedAt = [Diagnostics.Stopwatch]::GetTimestamp()
    try {
        if (-not $process.Start()) { throw 'PROCESS_START_RETURNED_FALSE' }
        $launchCount = 1
        $stdout = New-P5EStreamCaptureState -Stream $process.StandardOutput.BaseStream -MaxBytes $MaxBytes
        $stderr = New-P5EStreamCaptureState -Stream $process.StandardError.BaseStream -MaxBytes $MaxBytes
        Start-P5EStreamRead -State $stdout
        Start-P5EStreamRead -State $stderr
        $drainDeadline = $null
        while ($true) {
            Update-P5EStreamCapture -State $stdout
            Update-P5EStreamCapture -State $stderr
            $elapsed = [int64](([Diagnostics.Stopwatch]::GetTimestamp() - $startedAt) * 1000 / [Diagnostics.Stopwatch]::Frequency)
            $hasExited = $false
            try { $hasExited = $process.HasExited } catch { $hasExited = $false }

            if (-not $hasExited -and -not $timedOut -and $elapsed -ge $TimeoutMilliseconds) {
                $timedOut = $true
                $processTreeStatus = Stop-P5EProcessTree -Process $process -TimeoutMilliseconds $CleanupMilliseconds
                if ($processTreeStatus -notin @('TREE_KILLED_CONFIRMED')) {
                    $cleanupStatus = 'CLEANUP_UNCERTAIN'
                } else {
                    $cleanupStatus = 'TREE_CLEANUP_CONFIRMED'
                }
                $drainDeadline = $elapsed + $DrainMilliseconds
            }

            if ($hasExited -and $null -eq $drainDeadline) {
                $drainDeadline = $elapsed + $DrainMilliseconds
            }
            if (($stdout.Complete -and $stderr.Complete) -and $hasExited) { break }
            if ($null -ne $drainDeadline -and $elapsed -ge [int64]$drainDeadline) {
                $captureDrainTimedOut = $true
                break
            }
            Start-Sleep -Milliseconds 10
            if ($timedOut -and -not $hasExited -and
                    $elapsed -ge [int64]($TimeoutMilliseconds + $CleanupMilliseconds)) {
                $captureDrainTimedOut = $true
                break
            }
        }
        try {
            if ($process.HasExited) { $exitCode = [int]$process.ExitCode }
        } catch { $exitCode = $null }
    } catch {
        if ($launchCount -eq 0) { $launchErrorClass = $_.Exception.GetType().Name }
        else { $cleanupStatus = 'PROCESS_SUPERVISOR_EXCEPTION' }
    } finally {
        if ($null -ne $stdout) { Update-P5EStreamCapture -State $stdout }
        if ($null -ne $stderr) { Update-P5EStreamCapture -State $stderr }
        $process.Dispose()
    }

    $stdoutComplete = $false
    $stderrComplete = $false
    $stdoutText = ''
    $stderrText = ''
    $stdoutError = ''
    $stderrError = ''
    $stdoutBytes = [long]0
    $stderrBytes = [long]0
    $stdoutOverflow = $false
    $stderrOverflow = $false
    if ($null -ne $stdout) {
        $stdoutComplete = [bool]$stdout.Complete
        $stdoutText = Get-P5EStreamText -State $stdout
        $stdoutError = [string]$stdout.ErrorClass
        $stdoutBytes = [long]$stdout.TotalBytes
        $stdoutOverflow = [bool]$stdout.Overflow
        $stdout.Data.Dispose()
    }
    if ($null -ne $stderr) {
        $stderrComplete = [bool]$stderr.Complete
        $stderrText = Get-P5EStreamText -State $stderr
        $stderrError = [string]$stderr.ErrorClass
        $stderrBytes = [long]$stderr.TotalBytes
        $stderrOverflow = [bool]$stderr.Overflow
        $stderr.Data.Dispose()
    }
    $captureBounded = [bool]($stdoutComplete -and $stderrComplete -and
        [string]::IsNullOrWhiteSpace($stdoutError) -and [string]::IsNullOrWhiteSpace($stderrError) -and
        -not $captureDrainTimedOut)
    $captureOverflow = [bool]($stdoutOverflow -or $stderrOverflow)
    $cleanupConfirmed = $cleanupStatus -in @('NOT_REQUIRED', 'TREE_CLEANUP_CONFIRMED')
    $outcome = if ($launchCount -eq 0) { 'FAILED_BEFORE_LAUNCH' }
        elseif ($timedOut) { 'TIMEOUT' }
        elseif (-not $captureBounded) { 'CAPTURE_NOT_BOUNDED' }
        elseif ($captureOverflow) { 'CAPTURE_OVERFLOW' }
        elseif ($null -eq $exitCode) { 'PROCESS_EXIT_UNKNOWN' }
        elseif ($exitCode -eq 0) { 'PROCESS_EXITED_ZERO' }
        else { 'PROCESS_EXITED_NONZERO' }
    return [pscustomobject]@{
        Outcome = $outcome
        LaunchCount = $launchCount
        ExitCode = $exitCode
        TimedOut = $timedOut
        CaptureBounded = $captureBounded
        CaptureOverflow = $captureOverflow
        CaptureDrainTimedOut = $captureDrainTimedOut
        CleanupStatus = $cleanupStatus
        CleanupConfirmed = $cleanupConfirmed
        ProcessTreeStatus = $processTreeStatus
        LaunchErrorClass = $launchErrorClass
        StdoutErrorClass = $stdoutError
        StderrErrorClass = $stderrError
        StdoutBytes = $stdoutBytes
        StderrBytes = $stderrBytes
        StdoutText = $stdoutText
        StderrText = $stderrText
        ElapsedMilliseconds = [int64](([Diagnostics.Stopwatch]::GetTimestamp() - $startedAt) * 1000 / [Diagnostics.Stopwatch]::Frequency)
    }
}

function Invoke-P5EIsolatedTool {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$ErrorCode,
        [ValidateRange(1, 120000)][int]$TimeoutMilliseconds = 30000,
        [switch]$Detailed
    )

    $commandParts = @('&', (ConvertTo-P5ESingleQuotedLiteral -Value $FilePath))
    foreach ($argument in $Arguments) {
        $commandParts += (ConvertTo-P5ESingleQuotedLiteral -Value $argument)
    }
    $commandParts += '; $nativeExit = [int]$LASTEXITCODE'
    $commandParts += '; if ($nativeExit -ne 0) { exit $nativeExit }'
    $encodedCommand = [Convert]::ToBase64String(
        [Text.Encoding]::Unicode.GetBytes(($commandParts -join ' ')))
    $shellPath = @(
        (Join-Path $PSHOME 'powershell.exe'),
        (Join-Path $PSHOME 'pwsh.exe'),
        'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
    ) | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace([string]$shellPath)) {
        throw 'ACCOUNT_CHECK_PREFLIGHT_HOST_POWERSHELL_MISSING_STOP'
    }

    $script:AttemptedCommandCount++
    $run = Invoke-P5EBoundedProcess -FilePath ([string]$shellPath) `
        -ArgumentList @('-NoLogo', '-NoProfile', '-NonInteractive', '-EncodedCommand', $encodedCommand) `
        -TimeoutMilliseconds $TimeoutMilliseconds -MaxBytes $MaxCaptureBytes `
        -CleanupMilliseconds $CleanupTimeoutMilliseconds -DrainMilliseconds $CaptureDrainTimeoutMilliseconds `
        -ClearInheritedEnvironmentVariableNames @($expectedProcessValueName)
    $script:LastProcessRun = $run
    if ($run.TimedOut) { $script:ObservedTimeout = $true }
    if (-not $run.CaptureBounded -or $run.CaptureOverflow) { $script:ObservedCaptureFailure = $true }
    if (-not $run.CleanupConfirmed) { $script:ObservedCleanupFailure = $true }
    if ($run.Outcome -ne 'PROCESS_EXITED_ZERO') {
        if ($run.Outcome -eq 'TIMEOUT') { throw ($ErrorCode + ':TIMEOUT_STOP') }
        if ($run.Outcome -eq 'CAPTURE_OVERFLOW') { throw ($ErrorCode + ':CAPTURE_OVERFLOW_STOP') }
        if ($run.Outcome -eq 'CAPTURE_NOT_BOUNDED') { throw ($ErrorCode + ':CAPTURE_NOT_BOUNDED_STOP') }
        if ($run.Outcome -eq 'FAILED_BEFORE_LAUNCH') { throw ($ErrorCode + ':LAUNCH_STOP') }
        throw ($ErrorCode + ':NONZERO_STOP')
    }
    $script:SucceededCommandCount++
    $stdout = [string]$run.StdoutText
    $stderr = [string]$run.StderrText
    if ($Detailed) {
        return [pscustomobject]@{
            Stdout = $stdout
            Stderr = $stderr
            Run = $run
        }
    }
    if ([string]::IsNullOrEmpty($stderr)) { return $stdout }
    if ([string]::IsNullOrEmpty($stdout)) { return $stderr }
    return $stdout + "`n" + $stderr
}

function Invoke-P5EDeviceRead {
    param(
        [Parameter(Mandatory = $true)][string]$DeviceAdbPath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$ErrorCode,
        [ValidateRange(1, 120000)][int]$TimeoutMilliseconds = 30000,
        [switch]$Detailed
    )

    if (-not $PSBoundParameters.ContainsKey('TimeoutMilliseconds')) {
        $TimeoutMilliseconds = $script:DeviceReadTimeoutMilliseconds
    }
    $script:DeviceReadAttempts++
    return Invoke-P5EIsolatedTool -FilePath $DeviceAdbPath -Arguments $Arguments `
        -ErrorCode $ErrorCode -TimeoutMilliseconds $TimeoutMilliseconds -Detailed:$Detailed
}

function Get-P5ESignatureListParse {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Raw,
        [switch]$Past
    )

    $result = [pscustomobject]@{
        empty = [string]::IsNullOrWhiteSpace($Raw)
        valid = $true
        candidateCount = 0
        firstToken = ''
    }
    if ($result.empty) { return $result }

    $entries = @($Raw.Split(',') | ForEach-Object { $_.Trim() })
    foreach ($entry in $entries) {
        $pattern = if ($Past) {
            '^(?<token>[0-9a-fA-F]{8,128})\s+flags\s*:\s*[0-9a-fA-F]+$'
        } else {
            '^(?<token>[0-9a-fA-F]{8,128})$'
        }
        $match = [regex]::Match($entry, $pattern)
        if (-not $match.Success) {
            $result.valid = $false
            continue
        }
        $result.candidateCount++
        if ([string]::IsNullOrEmpty([string]$result.firstToken)) {
            $result.firstToken = $match.Groups['token'].Value.ToLowerInvariant()
        }
    }
    return $result
}

function Get-P5EPackageMetadataParse {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Text,
        [Parameter(Mandatory = $true)][string]$PackageName
    )

    $state = New-P5EPackageMetadataState
    if ([string]::IsNullOrEmpty($Text)) {
        $state.reason = 'EMPTY_OUTPUT'
        return $state
    }
    if ($Text -match '(?im)Unable to find package') {
        $state.reason = 'PACKAGE_ABSENT'
        return $state
    }

    $packageHeaderPattern = '(?im)^\s*Package\s*\[\s*' + [regex]::Escape($PackageName) + '\s*\](?:\s*\([^\r\n]*\))?\s*:'
    $packageHeaders = [regex]::Matches($Text, $packageHeaderPattern)
    $state.packagePresent = $packageHeaders.Count -gt 0
    if ($packageHeaders.Count -eq 0) {
        $state.reason = 'PACKAGE_ABSENT'
        return $state
    }
    if ($packageHeaders.Count -ne 1) {
        $state.reason = 'PACKAGE_SECTION_AMBIGUOUS'
        return $state
    }

    $header = $packageHeaders[0]
    $sectionStart = [int]$header.Index
    $tailStart = $sectionStart + [int]$header.Length
    $tail = if ($tailStart -lt $Text.Length) { $Text.Substring($tailStart) } else { '' }
    $nextPackageHeader = [regex]::Match($tail, '(?im)^\s*Package\s*\[')
    $sectionLength = if ($nextPackageHeader.Success) {
        [int]$header.Length + [int]$nextPackageHeader.Index
    } else {
        $Text.Length - $sectionStart
    }
    $section = $Text.Substring($sectionStart, $sectionLength)

    $versionFields = [regex]::Matches($section, '(?im)^\s*versionCode\s*=\s*(?<raw>\S+)')
    $versionCandidates = [regex]::Matches($section, '(?im)^\s*versionCode\s*=\s*(?<value>\d+)\b')
    $state.versionFieldCount = $versionFields.Count
    $state.versionCandidateCount = $versionCandidates.Count
    if ($versionFields.Count -eq 0) {
        $state.reason = if ([regex]::IsMatch($section, '(?im)^\s*versionCode\s*:')) {
            'UNSUPPORTED_LAYOUT'
        } else {
            'VERSION_MISSING'
        }
        return $state
    }
    if ($versionFields.Count -ne 1 -or $versionCandidates.Count -ne 1) {
        $state.reason = if ($versionFields.Count -gt 1 -or $versionCandidates.Count -gt 1) {
            'VERSION_AMBIGUOUS'
        } else {
            'VERSION_INVALID'
        }
        return $state
    }

    $legacySignatureFields = [regex]::Matches($section, '(?im)^\s*signatures\s*:\s*\[(?<raw>[^\]\r\n]*)\]\s*$')
    $aospWrapperMarker = [regex]::Matches($section, '(?im)^\s*signatures\s*=\s*PackageSignatures\s*\{')
    $aospWrapperPattern = '(?im)^\s*signatures\s*=\s*PackageSignatures\s*\{\s*(?<identity>[0-9a-fA-F]+)\s+version\s*:\s*(?<scheme>\d+)\s*,\s*signatures\s*:\s*\[(?<current>[^\]\r\n]*)\]\s*,\s*past\s+signatures\s*:\s*\[(?<past>[^\]\r\n]*)\]\s*\}\s*$'
    $aospWrappers = [regex]::Matches($section, $aospWrapperPattern)

    if ($aospWrapperMarker.Count -gt 0 -and $legacySignatureFields.Count -gt 0) {
        $state.layoutFamily = 'CONFLICTING_SIGNATURE_FORMATS'
        $state.signatureFieldCount = $aospWrapperMarker.Count + $legacySignatureFields.Count
        $state.reason = 'UNSUPPORTED_LAYOUT'
        return $state
    }

    if ($aospWrapperMarker.Count -gt 0) {
        $state.layoutFamily = 'AOSP_PACKAGE_SIGNATURES_WRAPPER'
        $state.signatureFieldCount = $aospWrapperMarker.Count
        if ($aospWrappers.Count -ne 1 -or $aospWrapperMarker.Count -ne 1) {
            $state.wrapperIdentityCandidateCount = $aospWrappers.Count
            $state.signatureSchemeCandidateCount = $aospWrappers.Count
            foreach ($wrapperCandidate in $aospWrappers) {
                $currentCandidate = Get-P5ESignatureListParse -Raw ([string]$wrapperCandidate.Groups['current'].Value)
                $state.signatureCandidateCount += $currentCandidate.candidateCount
                $pastCandidate = Get-P5ESignatureListParse -Raw ([string]$wrapperCandidate.Groups['past'].Value) -Past
                $state.pastSignatureCandidateCount += $pastCandidate.candidateCount
            }
            $state.reason = if ($aospWrappers.Count -gt 1 -or $aospWrapperMarker.Count -gt 1) {
                'SIGNATURE_AMBIGUOUS'
            } else {
                'UNSUPPORTED_LAYOUT'
            }
            return $state
        }

        $wrapper = $aospWrappers[0]
        $state.wrapperIdentityCandidateCount = 1
        $state.signatureSchemeCandidateCount = 1
        $current = Get-P5ESignatureListParse -Raw ([string]$wrapper.Groups['current'].Value)
        $past = Get-P5ESignatureListParse -Raw ([string]$wrapper.Groups['past'].Value) -Past
        $state.signatureCandidateCount = $current.candidateCount
        $state.pastSignatureCandidateCount = $past.candidateCount
        if ($current.empty) {
            $state.reason = 'SIGNATURE_MISSING'
            return $state
        }
        if (-not $current.valid -or -not $past.valid) {
            $state.reason = 'SIGNATURE_INVALID'
            return $state
        }
        if ($current.candidateCount -ne 1) {
            $state.reason = 'SIGNATURE_AMBIGUOUS'
            return $state
        }
        $state.reason = 'PASS'
        $state.versionCode = [int]$versionCandidates[0].Groups['value'].Value
        $state.signatureToken = $current.firstToken
        return $state
    }

    $state.signatureFieldCount = $legacySignatureFields.Count
    if ($legacySignatureFields.Count -eq 0) {
        $state.reason = if ([regex]::IsMatch($section, '(?im)^\s*signatures\s*(?::|=)')) {
            'UNSUPPORTED_LAYOUT'
        } else {
            'SIGNATURE_MISSING'
        }
        return $state
    }
    $state.layoutFamily = 'LEGACY_STANDALONE'
    if ($legacySignatureFields.Count -ne 1) {
        foreach ($legacyCandidate in $legacySignatureFields) {
            $candidateList = Get-P5ESignatureListParse -Raw ([string]$legacyCandidate.Groups['raw'].Value)
            $state.signatureCandidateCount += $candidateList.candidateCount
        }
        $state.reason = 'SIGNATURE_AMBIGUOUS'
        return $state
    }

    $legacy = Get-P5ESignatureListParse -Raw ([string]$legacySignatureFields[0].Groups['raw'].Value)
    $state.signatureCandidateCount = $legacy.candidateCount
    if ($legacy.empty) {
        $state.reason = 'SIGNATURE_MISSING'
        return $state
    }
    if (-not $legacy.valid) {
        $rawSignature = [string]$legacySignatureFields[0].Groups['raw'].Value
        $state.reason = if ($rawSignature -match '(?i)Signature|PackageSignatures|\{') {
            'UNSUPPORTED_LAYOUT'
        } else {
            'SIGNATURE_INVALID'
        }
        return $state
    }
    if ($legacy.candidateCount -ne 1) {
        $state.reason = 'SIGNATURE_AMBIGUOUS'
        return $state
    }

    $state.reason = 'PASS'
    $state.versionCode = [int]$versionCandidates[0].Groups['value'].Value
    $state.signatureToken = $legacy.firstToken
    return $state
}

function Get-P5EPackageMetadataStopCode {
    param(
        [Parameter(Mandatory = $true)][string]$Reason,
        [Parameter(Mandatory = $true)][string]$PackageName
    )
    return ('ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_' + $Reason + '_STOP:' + $PackageName)
}

function Get-P5EPackageState {
    param(
        [Parameter(Mandatory = $true)][string]$DeviceAdbPath,
        [Parameter(Mandatory = $true)][string]$DeviceSerial,
        [Parameter(Mandatory = $true)][string]$PackageName
    )

    $capture = Invoke-P5EDeviceRead -DeviceAdbPath $DeviceAdbPath `
        -Arguments @('-s', $DeviceSerial, 'shell', 'dumpsys', 'package', $PackageName) `
        -ErrorCode ('ACCOUNT_CHECK_PREFLIGHT_PACKAGE_READ_FAILED_STOP:' + $PackageName) -Detailed
    $metadata = Get-P5EPackageMetadataParse -Text ([string]$capture.Stdout) -PackageName $PackageName
    if ($PackageName -ceq $targetPackage) {
        $script:TargetPackageMetadata = $metadata
    } elseif ($PackageName -ceq $testPackage) {
        $script:TestPackageMetadata = $metadata
    }
    if (-not [string]::IsNullOrEmpty([string]$capture.Stderr)) {
        $metadata.reason = 'TOOL_STDERR'
        throw (Get-P5EPackageMetadataStopCode -Reason 'TOOL_STDERR' -PackageName $PackageName)
    }
    if ([string]$metadata.reason -ne 'PASS') {
        throw (Get-P5EPackageMetadataStopCode -Reason ([string]$metadata.reason) -PackageName $PackageName)
    }
    return [pscustomobject]@{
        package = $PackageName
        versionCode = [int]$metadata.versionCode
        signatureToken = [string]$metadata.signatureToken
    }
}

function Assert-P5EApkCertificate {
    param(
        [Parameter(Mandatory = $true)][string]$ToolPath,
        [Parameter(Mandatory = $true)][string]$ApkPath,
        [Parameter(Mandatory = $true)][string]$ExpectedCertificate,
        [Parameter(Mandatory = $true)][string]$ErrorPrefix
    )

    $signing = Invoke-P5EIsolatedTool -FilePath $ToolPath -Arguments @('verify', '--print-certs', $ApkPath) `
        -ErrorCode ($ErrorPrefix + '_READ_FAILED_STOP') -TimeoutMilliseconds 30000
    $certificateMatches = [regex]::Matches($signing, '(?im)certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
    $certificateMatch = if ($certificateMatches.Count -eq 1) { $certificateMatches[0] } else { $null }
    if ($null -eq $certificateMatch -or -not $certificateMatch.Success -or
            $certificateMatch.Groups[1].Value.ToUpperInvariant() -cne $ExpectedCertificate.ToUpperInvariant()) {
        throw ($ErrorPrefix + '_MISMATCH_STOP')
    }
}

function Get-P5EInstalledBaseApkPath {
    param(
        [Parameter(Mandatory = $true)][string]$DeviceAdbPath,
        [Parameter(Mandatory = $true)][string]$DeviceSerial,
        [Parameter(Mandatory = $true)][string]$PackageName
    )

    $text = Invoke-P5EDeviceRead -DeviceAdbPath $DeviceAdbPath `
        -Arguments @('-s', $DeviceSerial, 'shell', 'pm', 'path', $PackageName) `
        -ErrorCode ('ACCOUNT_CHECK_PREFLIGHT_' + $PackageName.Replace('.', '_') + '_APK_PATH_READ_FAILED_STOP') `
        -TimeoutMilliseconds 30000
    $lines = @($text -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($lines.Count -ne 1 -or $lines[0] -notmatch '^package:(?<path>/data/app/[^\s:]+/base\.apk)$') {
        throw ('ACCOUNT_CHECK_PREFLIGHT_' + $PackageName.Replace('.', '_') + '_APK_PATH_INVALID_STOP')
    }
    $path = $Matches['path']
    if ($path -match '\.\.|//|[\r\n]') { throw 'ACCOUNT_CHECK_PREFLIGHT_APK_PATH_INVALID_STOP' }
    return $path
}

function Write-P5EPreflightJson {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value
    )

    if (Test-Path -LiteralPath $Path) { throw 'ACCOUNT_CHECK_PREFLIGHT_RECEIPT_ALREADY_EXISTS_STOP' }
    [IO.File]::WriteAllText(
        $Path,
        ($Value | ConvertTo-Json -Depth 12 -Compress),
        [Text.UTF8Encoding]::new($false))
    $script:ReceiptPath = $Path
}

function Get-P5EReceiptBase {
    param(
        [Parameter(Mandatory = $true)][string]$Status,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$TypedError,
        [string]$ScriptHash = '',
        [int]$TargetVersionCode = 0,
        [string]$TargetSignatureToken = '',
        [string]$TestSignatureToken = ''
    )

    $last = $script:LastProcessRun
    return [ordered]@{
        schemaVersion = 'p5e.account-check.device-preflight.result.v3'
        status = $Status
        typedError = $TypedError
        stage = $script:Stage
        attemptCount = 1
        serial = $Serial
        targetPackage = $targetPackage
        testPackage = $testPackage
        targetVersionCode = $TargetVersionCode
        targetSignatureToken = $TargetSignatureToken
        testSignatureToken = $TestSignatureToken
        targetMetadataPackagePresent = [bool]$script:TargetPackageMetadata.packagePresent
        targetMetadataReason = [string]$script:TargetPackageMetadata.reason
        targetMetadataLayoutFamily = [string]$script:TargetPackageMetadata.layoutFamily
        targetMetadataVersionCandidateCount = [int]$script:TargetPackageMetadata.versionCandidateCount
        targetMetadataSignatureCandidateCount = [int]$script:TargetPackageMetadata.signatureCandidateCount
        targetMetadataPastSignatureCandidateCount = [int]$script:TargetPackageMetadata.pastSignatureCandidateCount
        targetMetadataWrapperIdentityCandidateCount = [int]$script:TargetPackageMetadata.wrapperIdentityCandidateCount
        targetMetadataSignatureSchemeCandidateCount = [int]$script:TargetPackageMetadata.signatureSchemeCandidateCount
        targetMetadataVersionFieldCount = [int]$script:TargetPackageMetadata.versionFieldCount
        targetMetadataSignatureFieldCount = [int]$script:TargetPackageMetadata.signatureFieldCount
        testMetadataPackagePresent = [bool]$script:TestPackageMetadata.packagePresent
        testMetadataReason = [string]$script:TestPackageMetadata.reason
        testMetadataLayoutFamily = [string]$script:TestPackageMetadata.layoutFamily
        testMetadataVersionCandidateCount = [int]$script:TestPackageMetadata.versionCandidateCount
        testMetadataSignatureCandidateCount = [int]$script:TestPackageMetadata.signatureCandidateCount
        testMetadataPastSignatureCandidateCount = [int]$script:TestPackageMetadata.pastSignatureCandidateCount
        testMetadataWrapperIdentityCandidateCount = [int]$script:TestPackageMetadata.wrapperIdentityCandidateCount
        testMetadataSignatureSchemeCandidateCount = [int]$script:TestPackageMetadata.signatureSchemeCandidateCount
        testMetadataVersionFieldCount = [int]$script:TestPackageMetadata.versionFieldCount
        testMetadataSignatureFieldCount = [int]$script:TestPackageMetadata.signatureFieldCount
        targetApkSha256 = $ExpectedTargetApkSha256.ToUpperInvariant()
        targetCertificateSha256 = $ExpectedTargetCertificateSha256.ToUpperInvariant()
        testApkSha256 = $ExpectedTestApkSha256.ToUpperInvariant()
        testCertificateSha256 = $ExpectedTestCertificateSha256.ToUpperInvariant()
        scriptSha256 = $ScriptHash
        evidenceDirectory = $script:EvidenceDirectoryResolved
        attemptedCommandCount = [long]$script:AttemptedCommandCount
        succeededCommandCount = [long]$script:SucceededCommandCount
        deviceReadAttempts = [long]$script:DeviceReadAttempts
        installAttempts = 0
        productionPackageOperations = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
        elapsedMilliseconds = [long]$script:Stopwatch.ElapsedMilliseconds
        timeoutObserved = [bool]$script:ObservedTimeout
        captureFailureObserved = [bool]$script:ObservedCaptureFailure
        cleanupFailureObserved = [bool]$script:ObservedCleanupFailure
        lastProcess = if ($null -eq $last) { $null } else {
            [ordered]@{
                outcome = $last.Outcome
                launchCount = $last.LaunchCount
                exitCode = $last.ExitCode
                timedOut = $last.TimedOut
                captureBounded = $last.CaptureBounded
                captureOverflow = $last.CaptureOverflow
                captureDrainTimedOut = $last.CaptureDrainTimedOut
                cleanupStatus = $last.CleanupStatus
                cleanupConfirmed = $last.CleanupConfirmed
                processTreeStatus = $last.ProcessTreeStatus
                stdoutBytes = $last.StdoutBytes
                stderrBytes = $last.StderrBytes
                elapsedMilliseconds = $last.ElapsedMilliseconds
            }
        }
    }
}

function Initialize-P5EEvidenceDirectory {
    if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
        $EvidenceDirectory = Join-Path 'D:\P5E-private' ('account-check-device-preflight-' +
            [DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff') + '-' + [Guid]::NewGuid().ToString('N'))
    }
    $script:EvidenceDirectoryResolved = [IO.Path]::GetFullPath($EvidenceDirectory)
    Assert-P5EPathChainNoReparse -Path $script:EvidenceDirectoryResolved -AllowMissingLeaf | Out-Null
    if (Test-Path -LiteralPath $script:EvidenceDirectoryResolved) {
        $item = Get-Item -LiteralPath $script:EvidenceDirectoryResolved -Force -ErrorAction Stop
        if (-not $item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
            throw 'ACCOUNT_CHECK_PREFLIGHT_EVIDENCE_DIRECTORY_INVALID_STOP'
        }
        if (@(Get-ChildItem -LiteralPath $script:EvidenceDirectoryResolved -Force -ErrorAction Stop).Count -ne 0) {
            throw 'ACCOUNT_CHECK_PREFLIGHT_EVIDENCE_DIRECTORY_NOT_EMPTY_STOP'
        }
    } else {
        New-Item -ItemType Directory -Path $script:EvidenceDirectoryResolved -ErrorAction Stop | Out-Null
    }
    $script:EvidenceReady = $true
}

try {
    Initialize-P5EEvidenceDirectory
    $script:Stage = 'STATIC_GATES'
    if ($Serial -cne '15e84958') { throw 'ACCOUNT_CHECK_PREFLIGHT_SERIAL_MISMATCH_STOP' }

    $scriptPath = [IO.Path]::GetFullPath($MyInvocation.MyCommand.Path)
    $scriptHashPath = Assert-P5ERegularFileHash -Path $scriptPath -ExpectedHash $ExpectedScriptSha256 `
        -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_SCRIPT'
    $scriptHash = (Get-FileHash -LiteralPath $scriptHashPath -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
    $referenceApk = Assert-P5ERegularFileHash -Path $ReferenceTestApkPath -ExpectedHash $ExpectedTestApkSha256 `
        -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_REFERENCE_APK'

    $sdkPath = $null
    if ([string]::IsNullOrWhiteSpace($AdbPath) -or [string]::IsNullOrWhiteSpace($ApkSignerPath)) {
        $sdkPath = Resolve-P5EAndroidSdkPath -RequestedPath $AndroidSdkPath
    }
    if ([string]::IsNullOrWhiteSpace($AdbPath)) { $AdbPath = Join-Path $sdkPath 'platform-tools\adb.exe' }
    if ([string]::IsNullOrWhiteSpace($ApkSignerPath)) { $ApkSignerPath = Resolve-P5EBuildTool -SdkPath $sdkPath -ToolName 'apksigner.bat' }
    $AdbPath = Assert-P5EAvailableRegularFile -Path $AdbPath -ErrorCode 'ACCOUNT_CHECK_PREFLIGHT_ADB_MISSING_STOP'
    $ApkSignerPath = Assert-P5EAvailableRegularFile -Path $ApkSignerPath -ErrorCode 'ACCOUNT_CHECK_PREFLIGHT_APKSIGNER_MISSING_STOP'

    $script:Stage = 'REFERENCE_CERTIFICATE'
    Assert-P5EApkCertificate -ToolPath $ApkSignerPath -ApkPath $referenceApk `
        -ExpectedCertificate $ExpectedTestCertificateSha256 -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_REFERENCE_CERTIFICATE'

    $script:Stage = 'DEVICE_GET_STATE'
    $deviceState = Invoke-P5EDeviceRead -DeviceAdbPath $AdbPath `
        -Arguments @('-s', $Serial, 'get-state') `
        -ErrorCode 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP'
    if ($deviceState.Trim() -cne 'device') { throw 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP' }

    $script:Stage = 'PRODUCTION_PACKAGE'
    $targetState = Get-P5EPackageState -DeviceAdbPath $AdbPath -DeviceSerial $Serial -PackageName $targetPackage
    if ($targetState.versionCode -ne $ExpectedTargetVersionCode -or
            $targetState.signatureToken -cne $ExpectedTargetSignatureToken.ToLowerInvariant()) {
        throw 'ACCOUNT_CHECK_PREFLIGHT_TARGET_PRODUCTION_PIN_MISMATCH_STOP'
    }

    $script:Stage = 'TEST_PACKAGE'
    $testState = Get-P5EPackageState -DeviceAdbPath $AdbPath -DeviceSerial $Serial -PackageName $testPackage
    if ($testState.signatureToken -cne $ExpectedTestSignatureToken.ToLowerInvariant()) {
        throw 'ACCOUNT_CHECK_PREFLIGHT_TEST_PACKAGE_SIGNATURE_MISMATCH_STOP'
    }

    $script:Stage = 'PRODUCTION_APK_PATH'
    $remoteTargetApk = Get-P5EInstalledBaseApkPath -DeviceAdbPath $AdbPath -DeviceSerial $Serial -PackageName $targetPackage
    $installedTargetApk = Join-Path $script:EvidenceDirectoryResolved 'installed-production.apk'
    $script:Stage = 'PRODUCTION_APK_PULL'
    Invoke-P5EDeviceRead -DeviceAdbPath $AdbPath -Arguments @('-s', $Serial, 'pull', $remoteTargetApk, $installedTargetApk) `
        -ErrorCode 'ACCOUNT_CHECK_PREFLIGHT_PRODUCTION_APK_PULL_FAILED_STOP' -TimeoutMilliseconds $PullTimeoutMilliseconds | Out-Null
    Assert-P5ERegularFileHash -Path $installedTargetApk -ExpectedHash $ExpectedTargetApkSha256 `
        -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_PRODUCTION_APK' | Out-Null
    Assert-P5EApkCertificate -ToolPath $ApkSignerPath -ApkPath $installedTargetApk `
        -ExpectedCertificate $ExpectedTargetCertificateSha256 -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_PRODUCTION_CERTIFICATE'

    $script:Stage = 'TEST_APK_PATH'
    $remoteTestApk = Get-P5EInstalledBaseApkPath -DeviceAdbPath $AdbPath -DeviceSerial $Serial -PackageName $testPackage
    $installedTestApk = Join-Path $script:EvidenceDirectoryResolved 'installed-test-package.apk'
    $script:Stage = 'TEST_APK_PULL'
    Invoke-P5EDeviceRead -DeviceAdbPath $AdbPath -Arguments @('-s', $Serial, 'pull', $remoteTestApk, $installedTestApk) `
        -ErrorCode 'ACCOUNT_CHECK_PREFLIGHT_TEST_APK_PULL_FAILED_STOP' -TimeoutMilliseconds $PullTimeoutMilliseconds | Out-Null
    Assert-P5ERegularFileHash -Path $installedTestApk -ExpectedHash $ExpectedTestApkSha256 `
        -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_TEST_APK' | Out-Null
    Assert-P5EApkCertificate -ToolPath $ApkSignerPath -ApkPath $installedTestApk `
        -ExpectedCertificate $ExpectedTestCertificateSha256 -ErrorPrefix 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_TEST_CERTIFICATE'

    $script:Stage = 'RECEIPT_PASS'
    try {
        $result = Get-P5EReceiptBase -Status 'PASS' -TypedError '' -ScriptHash $scriptHash `
            -TargetVersionCode $targetState.versionCode -TargetSignatureToken $targetState.signatureToken `
            -TestSignatureToken $testState.signatureToken
    } catch {
        throw 'ACCOUNT_CHECK_PREFLIGHT_RECEIPT_BUILD_STOP'
    }
    try {
        Write-P5EPreflightJson -Path (Join-Path $script:EvidenceDirectoryResolved 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json') -Value $result
    } catch {
        throw 'ACCOUNT_CHECK_PREFLIGHT_RECEIPT_WRITE_STOP'
    }
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT=PASS'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_READS=' + $script:DeviceReadAttempts)
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_INSTALL_ATTEMPTS=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_PRODUCTION_OPERATIONS=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_PROVIDER_CALLS=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_DB_WRITES=0'
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_RAW_DISPATCHES=0'
    exit 0
} catch {
    $typedError = Get-P5ETypedErrorCode -ErrorRecord $_
    if ($script:EvidenceReady) {
        try {
            $script:Stage = if ($script:Stage -eq 'RECEIPT_PASS') { 'RECEIPT_FAILURE' } else { $script:Stage }
            $failure = Get-P5EReceiptBase -Status 'STOP' -TypedError $typedError
            Write-P5EPreflightJson -Path (Join-Path $script:EvidenceDirectoryResolved 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json') -Value $failure
        } catch { }
    }
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT=STOP'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_ERROR=' + $typedError)
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_READS=' + $script:DeviceReadAttempts)
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_COMMANDS=' + $script:AttemptedCommandCount)
    exit 2
}
