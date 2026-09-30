[CmdletBinding()]
param(
    [Parameter(ParameterSetName = 'Library')]
    [switch]$LibraryOnly,

    [Parameter(ParameterSetName = 'SelfTest')]
    [switch]$SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# This file is the reviewable outer-launch contract.  It deliberately loads
# the already pinned process launcher/capture implementation instead of using
# PowerShell's native invocation operator.  The latter promotes child stderr
# into a NativeCommandError and can hide a known child exit code.
$script:P5EChildInvocationSupervisorPath = [IO.Path]::GetFullPath(
    (Join-Path $PSScriptRoot 'p5e-raw-live-supervisor.ps1'))
$script:P5EChildInvocationSupervisorSha256 =
    '998a5e45f13f61a5f2b53b46e1f6cf57d97ef1c3a6690fc0854530f57a53c7bf'
if (-not (Test-Path -LiteralPath $script:P5EChildInvocationSupervisorPath -PathType Leaf)) {
    throw 'P5E_CHILD_INVOCATION_SUPERVISOR_MISSING_STOP'
}
$childSupervisorItem = Get-Item -LiteralPath $script:P5EChildInvocationSupervisorPath -Force
if (($childSupervisorItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
    throw 'P5E_CHILD_INVOCATION_SUPERVISOR_REPARSE_STOP'
}
$childSupervisorHash = (Get-FileHash -LiteralPath $script:P5EChildInvocationSupervisorPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($childSupervisorHash -cne $script:P5EChildInvocationSupervisorSha256) {
    throw 'P5E_CHILD_INVOCATION_SUPERVISOR_HASH_MISMATCH_STOP'
}
$contractLibraryFlag = [bool]$LibraryOnly
$contractSelfTestFlag = [bool]$SelfTest
. $script:P5EChildInvocationSupervisorPath -LibraryOnly
$LibraryOnly = $contractLibraryFlag
$SelfTest = $contractSelfTestFlag

$script:P5EChildInvocationStdoutCap = 4194304L
$script:P5EChildInvocationStderrCap = 1048576L

function New-P5EChildInvocationResult {
    param(
        [Parameter(Mandatory = $true)][string]$TypedCode,
        [Parameter(Mandatory = $true)][int]$OuterExitCode,
        [string]$LaunchReason = '',
        [string]$LaunchErrorClass = '',
        [AllowNull()][Nullable[int]]$LaunchNativeErrorCode = $null
    )
    return [pscustomobject][ordered]@{
        ContractVersion = 'p5e.a43.child-invocation.v1'
        TypedCode = $TypedCode
        OuterExitCode = $OuterExitCode
        Attempted = $true
        ProcessCreated = 'FALSE'
        ProcessId = 'PID_UNKNOWN'
        ChildExited = 'UNKNOWN'
        ChildExitCode = $null
        LaunchReason = $LaunchReason
        LaunchErrorClass = $LaunchErrorClass
        LaunchNativeErrorCode = $LaunchNativeErrorCode
        TimedOut = $false
        CaptureBounded = $false
        CaptureErrorClass = ''
        ContainmentStatus = 'NOT_STARTED'
        ContainmentVerified = $false
        StdoutDrainStatus = 'NOT_STARTED'
        StderrDrainStatus = 'NOT_STARTED'
        StdoutByteLength = 0L
        StderrByteLength = 0L
        StdoutTruncated = $false
        StderrTruncated = $false
        OutputTooLarge = $false
        RedactionViolation = $false
        EnvironmentPolicy = 'EXPLICIT_CLEAR_LIST_PLUS_ALLOWLISTED_OVERRIDES'
        RetryCount = 0
        RedispatchCount = 0
    }
}

function Set-P5EChildEnvironment {
    param(
        [Parameter(Mandatory = $true)][System.Diagnostics.ProcessStartInfo]$StartInfo,
        [AllowEmptyCollection()][string[]]$ClearNames = @(),
        [AllowEmptyCollection()][hashtable]$Overrides = @{}
    )
    foreach ($name in @($ClearNames | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })) {
        [void]$StartInfo.EnvironmentVariables.Remove([string]$name)
    }
    foreach ($name in @($Overrides.Keys)) {
        if ([string]::IsNullOrWhiteSpace([string]$name)) {
            throw 'P5E_CHILD_INVOCATION_ENVIRONMENT_NAME_INVALID_STOP'
        }
        $value = [string]$Overrides[$name]
        if ($null -eq $value) { $value = '' }
        $StartInfo.EnvironmentVariables[[string]$name] = $value
    }
}

function Get-P5EChildProcessExitObservation {
    param(
        [Parameter(Mandatory = $true)][System.Diagnostics.Process]$Process,
        [AllowNull()][object]$Capture
    )
    $childExited = 'UNKNOWN'
    $childExitCode = $null
    try {
        if ($null -ne $Capture -and $null -ne $Capture.ExitCode) {
            $childExited = 'TRUE'
            $childExitCode = [int]$Capture.ExitCode
        } elseif ($Process.HasExited) {
            $childExited = 'TRUE'
            $childExitCode = [int]$Process.ExitCode
        } else {
            $childExited = 'FALSE'
        }
    } catch {
        $childExited = 'UNKNOWN'
        $childExitCode = $null
    }
    return [pscustomobject]@{
        ChildExited = $childExited
        ChildExitCode = $childExitCode
    }
}

function Get-P5EChildTerminalCode {
    param(
        [Parameter(Mandatory = $true)][bool]$ProcessCreated,
        [Parameter(Mandatory = $true)][AllowNull()][Nullable[int]]$ChildExitCode,
        [Parameter(Mandatory = $true)][bool]$TimedOut,
        [Parameter(Mandatory = $true)][bool]$OutputTooLarge,
        [Parameter(Mandatory = $true)][bool]$CaptureBounded,
        [Parameter(Mandatory = $true)][bool]$ContainmentVerified,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$CaptureErrorClass
    )
    if (-not $ProcessCreated) { return 'PROCESS_START_FAILED' }
    if (-not $ContainmentVerified) { return 'EXTERNAL_PROCESS_STATE_UNKNOWN' }
    if ($OutputTooLarge) { return 'OUTPUT_TOO_LARGE' }
    if ($TimedOut) { return 'PROCESS_TIMEOUT' }
    if (-not [string]::IsNullOrWhiteSpace($CaptureErrorClass)) { return 'CAPTURE_FAILED' }
    if (-not $CaptureBounded) { return 'CAPTURE_NOT_BOUNDED' }
    if ($null -eq $ChildExitCode) { return 'CHILD_EXIT_UNKNOWN' }
    if ($ChildExitCode -eq 0) { return 'CHILD_EXIT_ZERO' }
    return 'CHILD_EXIT_NONZERO'
}

function Invoke-P5EChildInvocation {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [ValidateRange(1, 600000)][long]$TimeoutMilliseconds = 5000,
        [ValidateRange(1024, 67108864)][long]$StdoutCaptureBytes = $script:P5EChildInvocationStdoutCap,
        [ValidateRange(1024, 67108864)][long]$StderrCaptureBytes = $script:P5EChildInvocationStderrCap,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @(),
        [AllowEmptyCollection()][string[]]$ClearInheritedEnvironmentVariableNames = @(),
        [AllowEmptyCollection()][hashtable]$EnvironmentOverrides = @{}
    )
    $result = New-P5EChildInvocationResult -TypedCode 'PRECHECK_NOT_RUN' -OuterExitCode 2
    $process = $null
    $launch = $null
    $textCapture = $null
    $processCreated = $false
    $processId = 'PID_UNKNOWN'
    $exitObservation = [pscustomobject]@{ ChildExited = 'UNKNOWN'; ChildExitCode = $null }
    $launchErrorClass = ''
    $launchNativeErrorCode = $null
    $launchReason = ''

    try {
        if (-not [IO.Path]::IsPathRooted($FilePath)) {
            $result.TypedCode = 'EXECUTABLE_PATH_NOT_ABSOLUTE'
            $result.LaunchReason = 'ABSOLUTE_PATH_REQUIRED'
            return [pscustomobject]$result
        }
        $executablePath = [IO.Path]::GetFullPath($FilePath)
        $workingDirectoryPath = [IO.Path]::GetFullPath($WorkingDirectory)
        if (-not (Test-Path -LiteralPath $workingDirectoryPath -PathType Container)) {
            $result.TypedCode = 'WORKING_DIRECTORY_INVALID'
            $result.LaunchReason = 'WORKING_DIRECTORY_MISSING'
            return [pscustomobject]$result
        }
        $workingDirectoryItem = Get-Item -LiteralPath $workingDirectoryPath -Force
        if (($workingDirectoryItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            $result.TypedCode = 'WORKING_DIRECTORY_REPARSE_STOP'
            $result.LaunchReason = 'WORKING_DIRECTORY_REPARSE'
            return [pscustomobject]$result
        }

        # A missing executable is retained as a typed start failure so the
        # fixture distinguishes process-created=false from child exit-nonzero.
        if (-not (Test-Path -LiteralPath $executablePath -PathType Leaf)) {
            $result.TypedCode = 'PROCESS_START_FAILED'
            $result.LaunchReason = 'PATH_OR_FILE_NOT_FOUND'
            return [pscustomobject]$result
        }
        $executableItem = Get-Item -LiteralPath $executablePath -Force
        if (($executableItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            $result.TypedCode = 'EXECUTABLE_REPARSE_STOP'
            $result.LaunchReason = 'EXECUTABLE_REPARSE'
            return [pscustomobject]$result
        }

        $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
        $startInfo.FileName = $executablePath
        $startInfo.WorkingDirectory = $workingDirectoryPath
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardInput = $false
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true
        Set-P5EChildEnvironment -StartInfo $startInfo -ClearNames $ClearInheritedEnvironmentVariableNames -Overrides $EnvironmentOverrides
        Set-P5EProcessStartInfoArguments -StartInfo $startInfo -ArgumentList $ArgumentList

        try {
            $launch = [P5EProcessLauncher]::Start($startInfo)
            $process = $launch.Process
            $processCreated = $true
            try { $processId = [string]$process.Id } catch { $processId = 'PID_UNKNOWN' }
            $textCapture = [P5EProcessTextCapture]::Capture(
                $launch,
                [int]$StdoutCaptureBytes,
                [int]$StderrCaptureBytes,
                [int]$TimeoutMilliseconds,
                $null)
            $exitObservation = Get-P5EChildProcessExitObservation -Process $process -Capture $textCapture
        } catch {
            if (-not $processCreated) {
                $launchDiagnostics = Get-P5ELaunchDiagnostics -ErrorRecord $_
                $launchErrorClass = [string]$launchDiagnostics.ErrorClass
                $launchNativeErrorCode = $launchDiagnostics.NativeErrorCode
                $launchReason = [string]$launchDiagnostics.Reason
            } else {
                $launchErrorClass = $_.Exception.GetType().Name
                $launchReason = 'POST_START_INVOCATION_EXCEPTION'
            }
        }
    } finally {
        if ($null -ne $launch) { [void]$launch.Dispose() }
        elseif ($null -ne $process) { [void]$process.Dispose() }
    }

    if (-not $processCreated) {
        $result.TypedCode = if ([string]::IsNullOrWhiteSpace($launchErrorClass)) { 'PROCESS_START_FAILED' } else { 'PROCESS_START_EXCEPTION' }
        $result.OuterExitCode = 2
        $result.LaunchReason = $launchReason
        $result.LaunchErrorClass = $launchErrorClass
        $result.LaunchNativeErrorCode = $launchNativeErrorCode
        return [pscustomobject]$result
    }

    $captureErrorClass = if ($null -eq $textCapture) { 'CAPTURE_NOT_CREATED' } else { [string]$textCapture.CaptureErrorClass }
    $captureBounded = [bool]($null -ne $textCapture -and $textCapture.CaptureBounded)
    $containmentVerified = [bool]($null -ne $textCapture -and $textCapture.ContainmentVerified)
    $outputTooLarge = [bool]($null -ne $textCapture -and $textCapture.OutputTooLarge)
    $timedOut = [bool]($null -ne $textCapture -and $textCapture.TimedOut)
    $redactionViolation = $false
    if ($null -ne $textCapture) {
        # These calls inspect captured bytes only in memory.  The returned
        # text is intentionally discarded; no raw stdout/stderr enters the
        # contract result, exception, receipt, or QA JSON.
        $safeOut = Protect-P5ECaptureText -Text ([string]$textCapture.Stdout) -SensitiveValues $SensitiveValues
        $safeErr = Protect-P5ECaptureText -Text ([string]$textCapture.Stderr) -SensitiveValues $SensitiveValues
        $redactionViolation = [bool]($safeOut.Violation -or $safeErr.Violation)
        $safeOut = $null
        $safeErr = $null
    }
    $typedCode = Get-P5EChildTerminalCode -ProcessCreated $processCreated `
        -ChildExitCode $exitObservation.ChildExitCode -TimedOut $timedOut `
        -OutputTooLarge $outputTooLarge -CaptureBounded $captureBounded `
        -ContainmentVerified $containmentVerified -CaptureErrorClass $captureErrorClass
    $outerExitCode = if ($typedCode -eq 'CHILD_EXIT_ZERO') { 0 } else { 1 }
    $result.TypedCode = $typedCode
    $result.OuterExitCode = $outerExitCode
    $result.ProcessCreated = 'TRUE'
    $result.ProcessId = $processId
    $result.ChildExited = [string]$exitObservation.ChildExited
    $result.ChildExitCode = $exitObservation.ChildExitCode
    $result.TimedOut = $timedOut
    $result.CaptureBounded = $captureBounded
    $result.CaptureErrorClass = $captureErrorClass
    $result.ContainmentStatus = if ($null -eq $textCapture) { 'NOT_STARTED' } else { [string]$textCapture.ContainmentStatus }
    $result.ContainmentVerified = $containmentVerified
    $result.StdoutDrainStatus = if ($null -eq $textCapture) { 'NOT_STARTED' } else { [string]$textCapture.StdoutDrainStatus }
    $result.StderrDrainStatus = if ($null -eq $textCapture) { 'NOT_STARTED' } else { [string]$textCapture.StderrDrainStatus }
    if ($null -ne $textCapture) {
        $result.StdoutByteLength = [long]$textCapture.StdoutByteLength
        $result.StderrByteLength = [long]$textCapture.StderrByteLength
        $result.StdoutTruncated = [bool]$textCapture.StdoutTruncated
        $result.StderrTruncated = [bool]$textCapture.StderrTruncated
    }
    $result.OutputTooLarge = $outputTooLarge
    $result.RedactionViolation = $redactionViolation
    return [pscustomobject]$result
}

function Invoke-P5EPhaseChildInvocation {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$Phase,
        [Parameter(Mandatory = $true)][string]$ExpectedEnvironmentName,
        [AllowEmptyString()][string]$ExpectedEnvironmentValue = '',
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [ValidateRange(1, 600000)][long]$TimeoutMilliseconds = 5000,
        [ValidateRange(1024, 67108864)][long]$StdoutCaptureBytes = $script:P5EChildInvocationStdoutCap,
        [ValidateRange(1024, 67108864)][long]$StderrCaptureBytes = $script:P5EChildInvocationStderrCap,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @()
    )
    $allowedPhases = @('PrepareEvent', 'Before', 'Dispatch', 'After', 'Verify')
    if ($allowedPhases -notcontains $Phase) { throw 'P5E_CHILD_PHASE_UNKNOWN_STOP' }
    if ([string]::IsNullOrWhiteSpace($ExpectedEnvironmentName)) { throw 'P5E_EXPECTED_ENVIRONMENT_NAME_INVALID_STOP' }
    $clearNames = @($ExpectedEnvironmentName)
    $overrides = @{}
    if ($Phase -eq 'Dispatch') {
        if ([string]::IsNullOrWhiteSpace($ExpectedEnvironmentValue)) {
            throw 'P5E_EXPECTED_ENVIRONMENT_MISSING_STOP'
        }
        $overrides[$ExpectedEnvironmentName] = $ExpectedEnvironmentValue
    }
    return Invoke-P5EChildInvocation -FilePath $FilePath -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory -TimeoutMilliseconds $TimeoutMilliseconds `
        -StdoutCaptureBytes $StdoutCaptureBytes -StderrCaptureBytes $StderrCaptureBytes `
        -SensitiveValues $SensitiveValues -ClearInheritedEnvironmentVariableNames $clearNames `
        -EnvironmentOverrides $overrides
}

function Test-P5EReceiptExpiryContract {
    param(
        [Parameter(Mandatory = $true)][DateTimeOffset]$IssuedAtUtc,
        [Parameter(Mandatory = $true)][DateTimeOffset]$ExpiresAtUtc,
        [Parameter(Mandatory = $true)][DateTimeOffset]$NowUtc
    )
    if ($ExpiresAtUtc -le $IssuedAtUtc) {
        return [pscustomobject]@{ LaunchAllowed = $false; TypedCode = 'RECEIPT_TIME_RANGE_INVALID' }
    }
    if ($IssuedAtUtc -gt $NowUtc) {
        return [pscustomobject]@{ LaunchAllowed = $false; TypedCode = 'RECEIPT_ISSUED_IN_FUTURE' }
    }
    if ($ExpiresAtUtc -le $NowUtc) {
        return [pscustomobject]@{ LaunchAllowed = $false; TypedCode = 'RECEIPT_EXPIRED_STOP' }
    }
    return [pscustomobject]@{ LaunchAllowed = $true; TypedCode = 'RECEIPT_CURRENT' }
}

function Test-P5EChildInvocationResultShape {
    param([Parameter(Mandatory = $true)]$Result)
    $required = @(
        'TypedCode', 'OuterExitCode', 'ProcessCreated', 'ProcessId',
        'ChildExited', 'ChildExitCode', 'CaptureBounded', 'TimedOut',
        'StdoutDrainStatus', 'StderrDrainStatus', 'RetryCount', 'RedispatchCount')
    foreach ($name in $required) {
        if ($null -eq $Result.PSObject.Properties[$name]) { return $false }
    }
    if ([int]$Result.RetryCount -ne 0 -or [int]$Result.RedispatchCount -ne 0) { return $false }
    if ($Result.TypedCode -eq 'CHILD_EXIT_ZERO' -and [int]$Result.OuterExitCode -ne 0) { return $false }
    if ($Result.TypedCode -ne 'CHILD_EXIT_ZERO' -and [int]$Result.OuterExitCode -eq 0) { return $false }
    if ([string]$Result.ProcessCreated -eq 'TRUE' -and [string]$Result.ProcessId -eq 'PID_UNKNOWN') {
        # A real Process object normally exposes Id; UNKNOWN remains a safe
        # terminal observation if the host denies the property.
        return $true
    }
    return $true
}

if (-not $LibraryOnly -and -not $SelfTest) {
    throw 'P5E_CHILD_INVOCATION_LIBRARY_ONLY_STOP'
}

if ($SelfTest) {
    $shape = Test-P5EChildInvocationResultShape -Result (New-P5EChildInvocationResult -TypedCode 'SELFTEST' -OuterExitCode 1)
    if (-not $shape) { throw 'P5E_CHILD_INVOCATION_SELFTEST_SHAPE_FAILED' }
    [pscustomobject]@{
        ContractVersion = 'p5e.a43.child-invocation.v1'
        SupervisorSha256 = $script:P5EChildInvocationSupervisorSha256
        StdoutCaptureCap = $script:P5EChildInvocationStdoutCap
        StderrCaptureCap = $script:P5EChildInvocationStderrCap
        ResultShape = 'PASS'
    } | ConvertTo-Json -Depth 4 -Compress
}
