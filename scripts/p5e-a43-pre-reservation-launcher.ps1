[CmdletBinding()]
param(
    [switch]$LibraryOnly
)

# This file is the tracked parent-launcher template for the A4.3
# pre-reservation boundary.  It is deliberately a library-only source file:
# a future candidate must wire live dependencies explicitly, while offline QA
# supplies stubs for every side effect.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:P5EA43PreReservationContractVersion = 'p5e.a43.pre-reservation.v1'
$script:P5EA43PreReservationStages = @(
    'ENTRY',
    'REPO_PREFLIGHT',
    'DEPENDENCY_PREFLIGHT',
    'TOOLCHAIN_PREFLIGHT',
    'APPROVAL',
    'LAUNCHER_RESERVATION',
    'OWNER_ROOT',
    'RECEIPT',
    'RECEIPT_VALIDATION',
    'KEY',
    'CHILD_LAUNCH',
    'CHILD_RESULT',
    'DIAGNOSTIC',
    'CLEANUP'
)

$script:P5EA43PathLabels = @(
    'P5E_REPO_ROOT',
    'P5E_PRIVATE_ROOT',
    'P5E_POWERSHELL',
    'P5E_LAUNCHER',
    'P5E_A43_AUDIT_ROOT',
    'P5E_A43_LAUNCHER_RESERVATION_ROOT',
    'P5E_PINNED_FILE',
    'P5E_TOOLCHAIN_SDK',
    'P5E_TOOLCHAIN_ADB',
    'P5E_TOOLCHAIN_JAVA',
    'P5E_TOOLCHAIN_APKSIGNER'
)
$script:P5EA43PathSuffixes = @(
    '_MISSING_STOP',
    '_NOT_REGULAR_STOP',
    '_NOT_DIRECTORY_STOP',
    '_REPARSE_STOP',
    '_PATH_SWAP_STOP',
    '_ANCESTOR_MISSING_STOP',
    '_ANCESTOR_REPARSE_STOP'
)

# This is an exact allowlist.  Do not replace it with a prefix/regex test:
# typed codes are part of the parent contract and unknown exceptions must not
# be able to smuggle their message into evidence.
$script:P5EA43KnownCodes = @(
    'UNKNOWN_STOP',
    'EVIDENCE_INCOMPLETE',
    'P5E_PRE_RESERVATION_DEPENDENCY_MISSING_STOP',
    'P5E_PRE_RESERVATION_CONTRACT_INVALID_STOP',
    'P5E_PRE_RESERVATION_RESULT_INVALID_STOP',
    'P5E_PRE_RESERVATION_APPROVAL_RESULT_INVALID_STOP',
    'P5E_PRE_RESERVATION_TOOLCHAIN_RESULT_INVALID_STOP',
    'P5E_RAW_TOOLCHAIN_SDK_CONFIGURATION_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_SDK_CONFIGURATION_AMBIGUOUS_STOP',
    'P5E_RAW_TOOLCHAIN_SDK_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_SDK_REPARSE_STOP',
    'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_VERSION_REQUIRED_STOP',
    'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_VERSION_INVALID_STOP',
    'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_REPARSE_STOP',
    'P5E_RAW_TOOLCHAIN_LOCAL_PROPERTIES_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_LOCAL_PROPERTIES_REPARSE_STOP',
    'P5E_RAW_TOOLCHAIN_ADB_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_ADB_REPARSE_STOP',
    'P5E_RAW_TOOLCHAIN_ADB_PATH_NOT_PINNED_STOP',
    'P5E_RAW_TOOLCHAIN_ADB_EXTENSION_STOP',
    'P5E_RAW_TOOLCHAIN_JAVA_PATH_REQUIRED_STOP',
    'P5E_RAW_TOOLCHAIN_JAVA_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_JAVA_REPARSE_STOP',
    'P5E_RAW_TOOLCHAIN_JAVA_NAME_STOP',
    'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_MISSING_STOP',
    'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_REPARSE_STOP',
    'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_PATH_NOT_PINNED_STOP',
    'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_NAME_STOP',
    'P5E_RAW_TOOLCHAIN_SHAPE_MISSING_STOP',
    'P5E_TOOLCHAIN_VALUE_MISSING_STOP',
    'P5E_TOOLCHAIN_VALUE_NOT_REGULAR_STOP',
    'P5E_TOOLCHAIN_VALUE_NOT_DIRECTORY_STOP',
    'P5E_TOOLCHAIN_VALUE_REPARSE_STOP',
    'P5E_TOOLCHAIN_VALUE_PATH_SWAP_STOP',
    'P5E_TOOLCHAIN_VALUE_ANCESTOR_MISSING_STOP',
    'P5E_TOOLCHAIN_VALUE_ANCESTOR_REPARSE_STOP',
    'P5E_PINNED_FILE_HASH_MISMATCH_STOP',
    'P5E_A43_AUDIT_PATH_INVALID_STOP',
    'P5E_A43_AUDIT_ALREADY_EXISTS_STOP',
    'P5E_A43_AUDIT_TOO_LARGE_STOP',
    'P5E_A43_RESERVATION_ROOT_CREATE_FAILED_STOP',
    'P5E_A43_LAUNCHER_RESERVATION_ALREADY_EXISTS_STOP',
    'P5E_A43_LAUNCHER_RESERVATION_CREATE_FAILED_STOP',
    'P5E_A43_LAUNCHER_RESERVATION_RESULT_INVALID_STOP',
    'P5E_LAUNCHER_HASH_MISMATCH_STOP',
    'P5E_OWNER_ROOT_CREATE_FAILED_STOP',
    'P5E_OWNER_ROOT_ALREADY_EXISTS_STOP',
    'P5E_OWNER_ROOT_RESULT_INVALID_STOP',
    'P5E_OWNER_RECEIPT_CREATE_FAILED_STOP',
    'P5E_OWNER_RECEIPT_RESULT_INVALID_STOP',
    'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP',
    'P5E_OWNER_APPROVAL_INPUT_EOF_STOP',
    'P5E_OWNER_APPROVAL_INPUT_CANCELLED_STOP',
    'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP',
    'P5E_OWNER_KEY_INPUT_CANCELLED_STOP',
    'P5E_OWNER_RECEIPT_INVALID_STOP',
    'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP',
    'RECEIPT_TIME_RANGE_INVALID',
    'RECEIPT_ISSUED_IN_FUTURE',
    'RECEIPT_EXPIRED_STOP',
    'P5E_CHILD_RESULT_SHAPE_INVALID_STOP',
    'PROCESS_START_FAILED',
    'PROCESS_START_EXCEPTION',
    'EXTERNAL_PROCESS_STATE_UNKNOWN',
    'OUTPUT_TOO_LARGE',
    'PROCESS_TIMEOUT',
    'CAPTURE_FAILED',
    'CAPTURE_NOT_BOUNDED',
    'CHILD_EXIT_UNKNOWN',
    'CHILD_EXIT_ZERO',
    'CHILD_EXIT_NONZERO',
    'EXECUTABLE_PATH_NOT_ABSOLUTE',
    'WORKING_DIRECTORY_INVALID',
    'WORKING_DIRECTORY_REPARSE_STOP',
    'EXECUTABLE_REPARSE_STOP',
    'P5E_A43_CHILD_INVOCATION_TYPED_STOP',
    'P5E_DIAGNOSTIC_WRITE_FAILED_STOP'
)
foreach ($label in $script:P5EA43PathLabels) {
    foreach ($suffix in $script:P5EA43PathSuffixes) {
        $script:P5EA43KnownCodes += ($label + $suffix)
    }
}

function Test-P5EA43KnownTypedCode {
    param([AllowNull()][string]$Code)
    if ([string]::IsNullOrWhiteSpace($Code)) { return $false }
    return ($script:P5EA43KnownCodes -contains $Code)
}

function Get-P5EA43BoundedText {
    param(
        [AllowNull()][object]$Value,
        [int]$MaximumLength = 160
    )
    if ($null -eq $Value) { return '' }
    $text = ([string]$Value) -replace '[\r\n\t]', ' '
    if ($text.Length -gt $MaximumLength) {
        return $text.Substring(0, $MaximumLength)
    }
    return $text
}

function Get-P5EA43SafeExceptionClass {
    param([AllowNull()][object]$ErrorRecord)
    $name = 'Exception'
    try {
        if ($null -ne $ErrorRecord -and $null -ne $ErrorRecord.Exception) {
            $name = [string]$ErrorRecord.Exception.GetType().Name
        }
    } catch { $name = 'Exception' }
    if ([string]::IsNullOrWhiteSpace($name)) { $name = 'Exception' }
    $name = $name -replace '[^A-Za-z0-9_.]', '_'
    return Get-P5EA43BoundedText -Value $name -MaximumLength 96
}

function Stop-P5EA43Typed {
    param(
        [Parameter(Mandatory = $true)][string]$Stage,
        [Parameter(Mandatory = $true)][string]$Code
    )
    $exception = New-Object System.InvalidOperationException 'P5E_TYPED_STOP'
    [void]$exception.Data.Add('P5ECode', $Code)
    [void]$exception.Data.Add('P5EStage', $Stage)
    throw $exception
}

function Get-P5EA43TypedCodeFromError {
    param([Parameter(Mandatory = $true)]$ErrorRecord)
    $candidate = ''
    try {
        if ($null -ne $ErrorRecord.Exception -and $ErrorRecord.Exception.Data.Contains('P5ECode')) {
            $candidate = [string]$ErrorRecord.Exception.Data['P5ECode']
        }
    } catch { $candidate = '' }
    if (Test-P5EA43KnownTypedCode -Code $candidate) { return $candidate }

    # Existing pinned helpers throw their exact typed code as the exception
    # message.  Compare only exact allowlisted values; never use a prefix.
    $message = ''
    try { $message = [string]$ErrorRecord.Exception.Message } catch { $message = '' }
    if (Test-P5EA43KnownTypedCode -Code $message) { return $message }
    return 'UNKNOWN_STOP'
}

function Get-P5EA43ErrorStage {
    param(
        [Parameter(Mandatory = $true)]$ErrorRecord,
        [Parameter(Mandatory = $true)][string]$FallbackStage
    )
    $candidate = ''
    try {
        if ($null -ne $ErrorRecord.Exception -and $ErrorRecord.Exception.Data.Contains('P5EStage')) {
            $candidate = [string]$ErrorRecord.Exception.Data['P5EStage']
        }
    } catch { $candidate = '' }
    if ($script:P5EA43PreReservationStages -contains $candidate) { return $candidate }
    if ($script:P5EA43PreReservationStages -contains $FallbackStage) { return $FallbackStage }
    return 'ENTRY'
}

function Get-P5EA43Sha256Upper {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

function Get-P5EA43TextSha256Upper {
    param([Parameter(Mandatory = $true)][string]$Text)
    $sha = $null
    $bytes = $null
    try {
        $sha = [Security.Cryptography.SHA256]::Create()
        $bytes = [Text.UTF8Encoding]::new($false).GetBytes($Text)
        return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToUpperInvariant()
    } finally {
        if ($null -ne $sha) { $sha.Dispose() }
        if ($null -ne $bytes) { [Array]::Clear($bytes, 0, $bytes.Length) }
    }
}

function Assert-P5EA43NoReparsePath {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][ValidateSet('Leaf', 'Container')][string]$Kind,
        [Parameter(Mandatory = $true)][string]$Label,
        [string]$Stage = 'DEPENDENCY_PREFLIGHT'
    )
    $literal = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literal)) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_MISSING_STOP')
    }
    if ($Kind -eq 'Leaf' -and -not (Test-Path -LiteralPath $literal -PathType Leaf)) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_NOT_REGULAR_STOP')
    }
    if ($Kind -eq 'Container' -and -not (Test-Path -LiteralPath $literal -PathType Container)) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_NOT_DIRECTORY_STOP')
    }
    $item = Get-Item -LiteralPath $literal -Force -ErrorAction Stop
    if ($Kind -eq 'Leaf' -and $item.PSIsContainer) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_NOT_REGULAR_STOP')
    }
    if ($Kind -eq 'Container' -and -not $item.PSIsContainer) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_NOT_DIRECTORY_STOP')
    }
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_REPARSE_STOP')
    }
    $cursor = [IO.Directory]::GetParent($literal)
    while ($null -ne $cursor) {
        if (-not (Test-Path -LiteralPath $cursor.FullName -PathType Container)) {
            Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_ANCESTOR_MISSING_STOP')
        }
        $cursorItem = Get-Item -LiteralPath $cursor.FullName -Force -ErrorAction Stop
        if (($cursorItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_ANCESTOR_REPARSE_STOP')
        }
        $parent = $cursor.Parent
        if ($null -eq $parent -or $parent.FullName -ceq $cursor.FullName) { break }
        $cursor = $parent
    }
    $resolved = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literal -ErrorAction Stop).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literal, $resolved)) {
        Stop-P5EA43Typed -Stage $Stage -Code ($Label + '_PATH_SWAP_STOP')
    }
    return $literal
}

function Assert-P5EA43PinnedFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedSha256,
        [string]$Stage = 'DEPENDENCY_PREFLIGHT'
    )
    [void](Assert-P5EA43NoReparsePath -Path $Path -Kind Leaf -Label 'P5E_PINNED_FILE' -Stage $Stage)
    $actual = Get-P5EA43Sha256Upper -Path $Path
    if ($actual -cne $ExpectedSha256.ToUpperInvariant()) {
        Stop-P5EA43Typed -Stage $Stage -Code 'P5E_PINNED_FILE_HASH_MISMATCH_STOP'
    }
    return $actual
}

function Assert-P5EA43ToolchainPaths {
    param([Parameter(Mandatory = $true)]$Toolchain)
    $required = @('sdkPath', 'adbPath', 'javaPath', 'apksignerJarPath')
    foreach ($name in $required) {
        $hasValue = if ($Toolchain -is [System.Collections.IDictionary]) {
            $Toolchain.Contains($name) -and $null -ne $Toolchain[$name]
        } else {
            $null -ne $Toolchain.PSObject.Properties[$name]
        }
        $value = if ($Toolchain -is [System.Collections.IDictionary]) { $Toolchain[$name] } else { $Toolchain.$name }
        if (-not $hasValue -or [string]::IsNullOrWhiteSpace([string]$value)) {
            Stop-P5EA43Typed -Stage 'TOOLCHAIN_PREFLIGHT' -Code 'P5E_RAW_TOOLCHAIN_SHAPE_MISSING_STOP'
        }
    }

    # The SDK is a directory.  The three executable/tool artifacts are files.
    # Keeping these labels separate prevents the old sdkPath-as-Leaf defect.
    $sdkValue = if ($Toolchain -is [System.Collections.IDictionary]) { $Toolchain['sdkPath'] } else { $Toolchain.sdkPath }
    $adbValue = if ($Toolchain -is [System.Collections.IDictionary]) { $Toolchain['adbPath'] } else { $Toolchain.adbPath }
    $javaValue = if ($Toolchain -is [System.Collections.IDictionary]) { $Toolchain['javaPath'] } else { $Toolchain.javaPath }
    $apksignerValue = if ($Toolchain -is [System.Collections.IDictionary]) { $Toolchain['apksignerJarPath'] } else { $Toolchain.apksignerJarPath }
    [void](Assert-P5EA43NoReparsePath -Path ([string]$sdkValue) -Kind Container -Label 'P5E_TOOLCHAIN_SDK' -Stage 'TOOLCHAIN_PREFLIGHT')
    [void](Assert-P5EA43NoReparsePath -Path ([string]$adbValue) -Kind Leaf -Label 'P5E_TOOLCHAIN_ADB' -Stage 'TOOLCHAIN_PREFLIGHT')
    [void](Assert-P5EA43NoReparsePath -Path ([string]$javaValue) -Kind Leaf -Label 'P5E_TOOLCHAIN_JAVA' -Stage 'TOOLCHAIN_PREFLIGHT')
    [void](Assert-P5EA43NoReparsePath -Path ([string]$apksignerValue) -Kind Leaf -Label 'P5E_TOOLCHAIN_APKSIGNER' -Stage 'TOOLCHAIN_PREFLIGHT')
    return $Toolchain
}

function Ensure-P5EA43Directory {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$FailureCode,
        [string]$Stage = 'LAUNCHER_RESERVATION'
    )
    $literal = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literal)) {
        try { [IO.Directory]::CreateDirectory($literal) | Out-Null }
        catch { Stop-P5EA43Typed -Stage 'LAUNCHER_RESERVATION' -Code $FailureCode }
    }
    return Assert-P5EA43NoReparsePath -Path $literal -Kind Container -Label $Label -Stage $Stage
}

function Write-P5EA43CreateNewUtf8 {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Text
    )
    $stream = $null
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes($Text)
    try {
        $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Flush($true)
    } finally {
        if ($null -ne $stream) { $stream.Dispose() }
        [Array]::Clear($bytes, 0, $bytes.Length)
    }
}

function Get-P5EA43LauncherBinding {
    param(
        [Parameter(Mandatory = $true)][string]$LauncherPath,
        [Parameter(Mandatory = $true)][string]$ReservationRoot
    )
    [void](Assert-P5EA43NoReparsePath -Path $LauncherPath -Kind Leaf -Label 'P5E_LAUNCHER' -Stage 'LAUNCHER_RESERVATION')
    $root = Ensure-P5EA43Directory -Path $ReservationRoot -Label 'P5E_A43_LAUNCHER_RESERVATION_ROOT' -FailureCode 'P5E_A43_RESERVATION_ROOT_CREATE_FAILED_STOP' -Stage 'LAUNCHER_RESERVATION'
    $launcherHash = Get-P5EA43Sha256Upper -Path $LauncherPath
    $rootHash = Get-P5EA43TextSha256Upper -Text ([IO.Path]::GetFullPath($root))
    return [pscustomobject][ordered]@{
        LauncherSha256 = $launcherHash
        ReservationRoot = $root
        ReservationRootSha256 = $rootHash
        ReservationPath = Join-Path $root ($launcherHash.ToLowerInvariant() + '.reservation')
    }
}

function New-P5EA43ReservationMarker {
    param([Parameter(Mandatory = $true)]$Context)
    $record = [ordered]@{
        schemaVersion = 'p5e.a43.pre-reservation.v1'
        launcherSha256 = [string]$Context.LauncherSha256.ToLowerInvariant()
        reservationRootSha256 = [string]$Context.ReservationRootSha256
        reservedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
        state = 'RESERVED_CONSUMED_FAIL_CLOSED'
    }
    try {
        Write-P5EA43CreateNewUtf8 -Path $Context.ReservationPath -Text ($record | ConvertTo-Json -Compress)
    } catch [IO.IOException] {
        Stop-P5EA43Typed -Stage 'LAUNCHER_RESERVATION' -Code 'P5E_A43_LAUNCHER_RESERVATION_ALREADY_EXISTS_STOP'
    } catch {
        Stop-P5EA43Typed -Stage 'LAUNCHER_RESERVATION' -Code 'P5E_A43_LAUNCHER_RESERVATION_CREATE_FAILED_STOP'
    }
    return [pscustomobject][ordered]@{ Created = $true; Path = $Context.ReservationPath }
}

function Write-P5EA43DiagnosticCreateNew {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Record
    )
    $text = $Record | ConvertTo-Json -Depth 12 -Compress
    if ($text.Length -gt 65536) {
        Stop-P5EA43Typed -Stage 'DIAGNOSTIC' -Code 'P5E_A43_AUDIT_TOO_LARGE_STOP'
    }
    try {
        Write-P5EA43CreateNewUtf8 -Path $Path -Text $text
    } catch [IO.IOException] {
        Stop-P5EA43Typed -Stage 'DIAGNOSTIC' -Code 'P5E_A43_AUDIT_ALREADY_EXISTS_STOP'
    } catch {
        Stop-P5EA43Typed -Stage 'DIAGNOSTIC' -Code 'P5E_DIAGNOSTIC_WRITE_FAILED_STOP'
    }
    return $true
}

function New-P5EA43DiagnosticRecord {
    param(
        [Parameter(Mandatory = $true)]$Context,
        [Parameter(Mandatory = $true)][string]$Stage,
        [Parameter(Mandatory = $true)][string]$TypedCode,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$ExceptionClass,
        [Parameter(Mandatory = $true)][int]$OuterExitCode,
        [Parameter(Mandatory = $true)][string]$DiagnosticStatus,
        [Parameter(Mandatory = $true)][string]$SecondaryStatus
    )
    return [ordered]@{
        schemaVersion = $script:P5EA43PreReservationContractVersion
        stage = Get-P5EA43BoundedText -Value $Stage -MaximumLength 48
        operation = Get-P5EA43BoundedText -Value ([string]$Context.Operation) -MaximumLength 80
        typedCode = Get-P5EA43BoundedText -Value $TypedCode -MaximumLength 96
        exceptionClass = Get-P5EA43BoundedText -Value $ExceptionClass -MaximumLength 96
        launcherSha256 = Get-P5EA43BoundedText -Value ([string]$Context.LauncherSha256) -MaximumLength 64
        reservationRoot = Get-P5EA43BoundedText -Value ([string]$Context.ReservationRoot) -MaximumLength 260
        reservationRootSha256 = Get-P5EA43BoundedText -Value ([string]$Context.ReservationRootSha256) -MaximumLength 64
        reservationPath = Get-P5EA43BoundedText -Value ([string]$Context.ReservationPath) -MaximumLength 300
        reservationState = Get-P5EA43BoundedText -Value ([string]$Context.ReservationState) -MaximumLength 48
        reservationCreated = [bool]$Context.ReservationCreated
        ownerRootState = Get-P5EA43BoundedText -Value ([string]$Context.OwnerRootState) -MaximumLength 48
        ownerRootCreated = [bool]$Context.OwnerRootCreated
        receiptState = Get-P5EA43BoundedText -Value ([string]$Context.ReceiptState) -MaximumLength 48
        receiptCreated = [bool]$Context.ReceiptCreated
        keyPromptCount = [int]$Context.KeyPromptCount
        childInvocationState = Get-P5EA43BoundedText -Value ([string]$Context.ChildInvocationState) -MaximumLength 48
        childAttempts = [int]$Context.ChildAttempts
        childExitCode = $Context.ChildExitCode
        outerExitCode = [int]$OuterExitCode
        nonzeroExit = ([int]$OuterExitCode -ne 0)
        diagnosticStatus = Get-P5EA43BoundedText -Value $DiagnosticStatus -MaximumLength 48
        secondaryStatus = Get-P5EA43BoundedText -Value $SecondaryStatus -MaximumLength 240
        liveBoundary = 'NOT_EVALUATED'
        providerCalls = 'NOT_EVALUATED'
        databaseWrites = 'NOT_EVALUATED'
        rawDispatches = 'NOT_EVALUATED'
        redispatches = 'NOT_EVALUATED'
    }
}

function Test-P5EA43DependencyTable {
    param([Parameter(Mandatory = $true)][hashtable]$Dependencies)
    foreach ($name in @(
            'ResolveToolchain',
            'ReadApproval',
            'ReserveLauncher',
            'CreateOwnerRoot',
            'CreateReceipt',
            'ValidateReceipt',
            'ReadKey',
            'InvokeChild',
            'ClearKey',
            'WriteDiagnostic')) {
        if (-not $Dependencies.ContainsKey($name) -or $null -eq $Dependencies[$name] -or
                $Dependencies[$name] -isnot [scriptblock]) {
            return $false
        }
    }
    return $true
}

function Test-P5EA43ResultHasProperty {
    param(
        [AllowNull()][object]$Result,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if ($null -eq $Result) { return $false }
    if ($Result -is [System.Collections.IDictionary]) { return $Result.Contains($Name) }
    return ($null -ne $Result.PSObject.Properties[$Name])
}

function Invoke-P5EA43PreReservation {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][hashtable]$Contract,
        [Parameter(Mandatory = $true)][hashtable]$Dependencies
    )

    $context = [ordered]@{
        ContractVersion = $script:P5EA43PreReservationContractVersion
        Operation = 'A43_PRE_RESERVATION'
        Stage = 'ENTRY'
        LauncherSha256 = ''
        ReservationRoot = ''
        ReservationRootSha256 = ''
        ReservationPath = ''
        ReservationState = 'NOT_ATTEMPTED'
        ReservationCreated = $false
        OwnerRootState = 'NOT_ATTEMPTED'
        OwnerRootCreated = $false
        OwnerRootPath = ''
        ReceiptState = 'NOT_ATTEMPTED'
        ReceiptCreated = $false
        ReceiptPath = ''
        ReceiptSha256 = ''
        KeyPromptCount = 0
        ChildInvocationState = 'NOT_ATTEMPTED'
        ChildAttempts = 0
        ChildExitCode = $null
    }
    $primaryTypedCode = 'UNKNOWN_STOP'
    $primaryStage = 'ENTRY'
    $primaryExceptionClass = ''
    $outerExitCode = 1
    $diagnosticStatus = 'NOT_WRITTEN'
    $diagnosticWritten = $false
    $secondary = New-Object System.Collections.ArrayList
    $keyHeld = $false

    try {
        $context.Stage = 'ENTRY'
        if (-not (Test-P5EA43DependencyTable -Dependencies $Dependencies)) {
            Stop-P5EA43Typed -Stage 'ENTRY' -Code 'P5E_PRE_RESERVATION_DEPENDENCY_MISSING_STOP'
        }
        foreach ($required in @('RepoRoot', 'PrivateRoot', 'PowershellPath', 'LauncherPath', 'ReservationRoot', 'AuditPath')) {
            if (-not $Contract.ContainsKey($required) -or [string]::IsNullOrWhiteSpace([string]$Contract[$required])) {
                Stop-P5EA43Typed -Stage 'ENTRY' -Code 'P5E_PRE_RESERVATION_CONTRACT_INVALID_STOP'
            }
        }

        $context.Stage = 'REPO_PREFLIGHT'
        [void](Assert-P5EA43NoReparsePath -Path ([string]$Contract.RepoRoot) -Kind Container -Label 'P5E_REPO_ROOT')
        [void](Assert-P5EA43NoReparsePath -Path ([string]$Contract.PrivateRoot) -Kind Container -Label 'P5E_PRIVATE_ROOT')
        [void](Assert-P5EA43NoReparsePath -Path ([string]$Contract.PowershellPath) -Kind Leaf -Label 'P5E_POWERSHELL')

        $context.Stage = 'DEPENDENCY_PREFLIGHT'
        if ($Contract.ContainsKey('PinnedFiles') -and $null -ne $Contract.PinnedFiles) {
            foreach ($pin in @($Contract.PinnedFiles)) {
                if ($null -eq $pin -or $null -eq $pin.PSObject.Properties['Path'] -or
                        $null -eq $pin.PSObject.Properties['Sha256']) {
                    Stop-P5EA43Typed -Stage 'DEPENDENCY_PREFLIGHT' -Code 'P5E_PRE_RESERVATION_CONTRACT_INVALID_STOP'
                }
                [void](Assert-P5EA43PinnedFile -Path ([string]$pin.Path) -ExpectedSha256 ([string]$pin.Sha256))
            }
        }

        $context.Stage = 'TOOLCHAIN_PREFLIGHT'
        $toolchain = & $Dependencies.ResolveToolchain $Contract
        if ($null -eq $toolchain) {
            Stop-P5EA43Typed -Stage 'TOOLCHAIN_PREFLIGHT' -Code 'P5E_PRE_RESERVATION_TOOLCHAIN_RESULT_INVALID_STOP'
        }
        [void](Assert-P5EA43ToolchainPaths -Toolchain $toolchain)

        $context.Stage = 'APPROVAL'
        $approvalResult = & $Dependencies.ReadApproval $Contract
        if ($null -eq $approvalResult) {
            Stop-P5EA43Typed -Stage 'APPROVAL' -Code 'P5E_OWNER_APPROVAL_INPUT_EOF_STOP'
        }
        $approvalState = 'VALUE'
        $approvalValue = ''
        if ($approvalResult -is [string]) {
            $approvalValue = [string]$approvalResult
        } elseif (Test-P5EA43ResultHasProperty -Result $approvalResult -Name 'State') {
            $approvalState = [string]$approvalResult.State
            if (Test-P5EA43ResultHasProperty -Result $approvalResult -Name 'Value') {
                $approvalValue = [string]$approvalResult.Value
            }
        } else {
            Stop-P5EA43Typed -Stage 'APPROVAL' -Code 'P5E_PRE_RESERVATION_APPROVAL_RESULT_INVALID_STOP'
        }
        if ($approvalState -eq 'EOF') {
            Stop-P5EA43Typed -Stage 'APPROVAL' -Code 'P5E_OWNER_APPROVAL_INPUT_EOF_STOP'
        }
        if ($approvalState -eq 'CANCELLED') {
            Stop-P5EA43Typed -Stage 'APPROVAL' -Code 'P5E_OWNER_APPROVAL_INPUT_CANCELLED_STOP'
        }
        $expectedApproval = if ($Contract.ContainsKey('ApprovalLiteral')) { [string]$Contract.ApprovalLiteral } else { 'APPROVE_ONE_FRESH_EVENT' }
        if ($approvalValue -cne $expectedApproval) {
            Stop-P5EA43Typed -Stage 'APPROVAL' -Code 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP'
        }

        $context.Stage = 'LAUNCHER_RESERVATION'
        $binding = Get-P5EA43LauncherBinding -LauncherPath ([string]$Contract.LauncherPath) -ReservationRoot ([string]$Contract.ReservationRoot)
        $context.LauncherSha256 = [string]$binding.LauncherSha256
        $context.ReservationRoot = [string]$binding.ReservationRoot
        $context.ReservationRootSha256 = [string]$binding.ReservationRootSha256
        $context.ReservationPath = [string]$binding.ReservationPath
        if ($Contract.ContainsKey('ExpectedLauncherSha256') -and -not [string]::IsNullOrWhiteSpace([string]$Contract.ExpectedLauncherSha256) -and
                $context.LauncherSha256 -cne ([string]$Contract.ExpectedLauncherSha256).ToUpperInvariant()) {
            Stop-P5EA43Typed -Stage 'LAUNCHER_RESERVATION' -Code 'P5E_LAUNCHER_HASH_MISMATCH_STOP'
        }
        $context.ReservationState = 'ATTEMPTED'
        $reservationResult = & $Dependencies.ReserveLauncher $context
        if (-not (Test-P5EA43ResultHasProperty -Result $reservationResult -Name 'Created') -or
                -not [bool]$reservationResult.Created) {
            Stop-P5EA43Typed -Stage 'LAUNCHER_RESERVATION' -Code 'P5E_A43_LAUNCHER_RESERVATION_RESULT_INVALID_STOP'
        }
        $context.ReservationCreated = $true
        $context.ReservationState = 'CREATED'

        $context.Stage = 'OWNER_ROOT'
        $ownerResult = & $Dependencies.CreateOwnerRoot $context
        if (-not (Test-P5EA43ResultHasProperty -Result $ownerResult -Name 'Created') -or
                -not [bool]$ownerResult.Created) {
            Stop-P5EA43Typed -Stage 'OWNER_ROOT' -Code 'P5E_OWNER_ROOT_RESULT_INVALID_STOP'
        }
        $context.OwnerRootCreated = $true
        $context.OwnerRootState = 'CREATED'
        if (Test-P5EA43ResultHasProperty -Result $ownerResult -Name 'Path') {
            $context.OwnerRootPath = Get-P5EA43BoundedText -Value ([string]$ownerResult.Path) -MaximumLength 300
        }

        $context.Stage = 'RECEIPT'
        $receiptResult = & $Dependencies.CreateReceipt $context
        if (-not (Test-P5EA43ResultHasProperty -Result $receiptResult -Name 'Created') -or
                -not [bool]$receiptResult.Created) {
            Stop-P5EA43Typed -Stage 'RECEIPT' -Code 'P5E_OWNER_RECEIPT_RESULT_INVALID_STOP'
        }
        $context.ReceiptCreated = $true
        $context.ReceiptState = 'CREATED'
        if (Test-P5EA43ResultHasProperty -Result $receiptResult -Name 'Path') {
            $context.ReceiptPath = Get-P5EA43BoundedText -Value ([string]$receiptResult.Path) -MaximumLength 300
        }
        if (Test-P5EA43ResultHasProperty -Result $receiptResult -Name 'Sha256') {
            $context.ReceiptSha256 = Get-P5EA43BoundedText -Value ([string]$receiptResult.Sha256) -MaximumLength 64
        }

        $context.Stage = 'RECEIPT_VALIDATION'
        $receiptGate = & $Dependencies.ValidateReceipt $context
        if (-not (Test-P5EA43ResultHasProperty -Result $receiptGate -Name 'Valid') -or -not [bool]$receiptGate.Valid) {
            $gateCode = if (Test-P5EA43ResultHasProperty -Result $receiptGate -Name 'TypedCode') { [string]$receiptGate.TypedCode } else { 'P5E_OWNER_RECEIPT_INVALID_STOP' }
            if (-not (Test-P5EA43KnownTypedCode -Code $gateCode)) { $gateCode = 'P5E_OWNER_RECEIPT_INVALID_STOP' }
            Stop-P5EA43Typed -Stage 'RECEIPT_VALIDATION' -Code $gateCode
        }

        $context.Stage = 'KEY'
        $context.KeyPromptCount = 1
        $keyResult = & $Dependencies.ReadKey $context
        if ($null -eq $keyResult) {
            Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP'
        }
        if (Test-P5EA43ResultHasProperty -Result $keyResult -Name 'State') {
            $keyState = [string]$keyResult.State
            if ($keyState -eq 'EOF' -or $keyState -eq 'CANCELLED') {
                Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_CANCELLED_STOP'
            }
        }
        $keyHeld = $true

        $context.Stage = 'CHILD_LAUNCH'
        $context.ChildAttempts = 1
        $context.ChildInvocationState = 'START_ATTEMPTED'
        $childResult = & $Dependencies.InvokeChild $context
        if (-not (Test-P5EA43ResultHasProperty -Result $childResult -Name 'TypedCode') -or
                -not (Test-P5EA43ResultHasProperty -Result $childResult -Name 'OuterExitCode')) {
            Stop-P5EA43Typed -Stage 'CHILD_RESULT' -Code 'P5E_CHILD_RESULT_SHAPE_INVALID_STOP'
        }
        $childCode = [string]$childResult.TypedCode
        if (-not (Test-P5EA43KnownTypedCode -Code $childCode)) { $childCode = 'UNKNOWN_STOP' }
        $outerExitCode = 1
        try { $outerExitCode = [int]$childResult.OuterExitCode } catch { $outerExitCode = 1 }
        if ($outerExitCode -eq 0 -and $childCode -ne 'CHILD_EXIT_ZERO') {
            Stop-P5EA43Typed -Stage 'CHILD_RESULT' -Code 'P5E_CHILD_RESULT_SHAPE_INVALID_STOP'
        }
        if ($outerExitCode -ne 0 -and $childCode -eq 'CHILD_EXIT_ZERO') {
            Stop-P5EA43Typed -Stage 'CHILD_RESULT' -Code 'P5E_CHILD_RESULT_SHAPE_INVALID_STOP'
        }
        $context.ChildInvocationState = 'COMPLETED'
        if ((Test-P5EA43ResultHasProperty -Result $childResult -Name 'ChildExitCode') -and $null -ne $childResult.ChildExitCode) {
            try { $context.ChildExitCode = [int]$childResult.ChildExitCode } catch { $context.ChildExitCode = $null }
        }
        $primaryTypedCode = $childCode
        $primaryStage = 'CHILD_RESULT'
        $primaryExceptionClass = ''
        if ($outerExitCode -eq 0) {
            $primaryTypedCode = 'CHILD_EXIT_ZERO'
        }
    } catch {
        $primaryTypedCode = Get-P5EA43TypedCodeFromError -ErrorRecord $_
        $primaryStage = Get-P5EA43ErrorStage -ErrorRecord $_ -FallbackStage ([string]$context.Stage)
        $primaryExceptionClass = Get-P5EA43SafeExceptionClass -ErrorRecord $_
        $outerExitCode = 1
        if ($context.Stage -eq 'CHILD_LAUNCH' -and $context.ChildAttempts -gt 0 -and
                $context.ChildInvocationState -eq 'START_ATTEMPTED') {
            $context.ChildInvocationState = 'UNKNOWN'
        }
    } finally {
        $context.Stage = 'CLEANUP'
        if ($keyHeld) {
            try { [void](& $Dependencies.ClearKey $context) }
            catch {
                [void]$secondary.Add(('CLEANUP_FAILED:' + (Get-P5EA43SafeExceptionClass -ErrorRecord $_)))
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($primaryTypedCode)) { $primaryTypedCode = 'UNKNOWN_STOP' }
    if ($primaryTypedCode -eq 'UNKNOWN_STOP' -and [string]::IsNullOrWhiteSpace($primaryExceptionClass)) {
        $primaryExceptionClass = 'Exception'
    }
    $secondaryStatus = if ($secondary.Count -eq 0) { 'NONE' } else { ($secondary -join ';') }
    $context.Stage = 'DIAGNOSTIC'
    $diagnosticRecord = New-P5EA43DiagnosticRecord -Context $context -Stage $primaryStage `
        -TypedCode $primaryTypedCode -ExceptionClass $primaryExceptionClass `
        -OuterExitCode $outerExitCode -DiagnosticStatus 'PENDING' -SecondaryStatus $secondaryStatus
    try {
        $writeResult = & $Dependencies.WriteDiagnostic ([string]$Contract.AuditPath) $diagnosticRecord
        if ($writeResult -is [bool] -and -not [bool]$writeResult) {
            throw (New-Object System.InvalidOperationException 'P5E_DIAGNOSTIC_WRITE_FAILED')
        }
        $diagnosticWritten = $true
        $diagnosticStatus = 'WRITTEN'
    } catch {
        $diagnosticStatus = 'EVIDENCE_INCOMPLETE'
        [void]$secondary.Add(('DIAGNOSTIC_WRITE_FAILED:' + (Get-P5EA43SafeExceptionClass -ErrorRecord $_)))
        $secondaryStatus = ($secondary -join ';')
        if ($outerExitCode -eq 0) {
            $primaryTypedCode = 'EVIDENCE_INCOMPLETE'
            $primaryStage = 'DIAGNOSTIC'
            $primaryExceptionClass = Get-P5EA43SafeExceptionClass -ErrorRecord $_
            $outerExitCode = 1
        }
    }

    return [pscustomobject][ordered]@{
        ContractVersion = $script:P5EA43PreReservationContractVersion
        Status = if ($outerExitCode -eq 0) { 'CHILD_COMPLETED_ZERO' } else { 'STOPPED' }
        Stage = $primaryStage
        TypedCode = $primaryTypedCode
        ExceptionClass = $primaryExceptionClass
        OuterExitCode = [int]$outerExitCode
        NonzeroExit = ([int]$outerExitCode -ne 0)
        LauncherSha256 = [string]$context.LauncherSha256
        ReservationRoot = [string]$context.ReservationRoot
        ReservationRootSha256 = [string]$context.ReservationRootSha256
        ReservationPath = [string]$context.ReservationPath
        ReservationState = [string]$context.ReservationState
        ReservationCreated = [bool]$context.ReservationCreated
        OwnerRootState = [string]$context.OwnerRootState
        OwnerRootCreated = [bool]$context.OwnerRootCreated
        ReceiptState = [string]$context.ReceiptState
        ReceiptCreated = [bool]$context.ReceiptCreated
        KeyPromptCount = [int]$context.KeyPromptCount
        ChildInvocationState = [string]$context.ChildInvocationState
        ChildAttempts = [int]$context.ChildAttempts
        ChildExitCode = $context.ChildExitCode
        AuditPath = [string]$Contract.AuditPath
        DiagnosticWritten = [bool]$diagnosticWritten
        DiagnosticStatus = $diagnosticStatus
        SecondaryStatus = $secondaryStatus
        LiveBoundary = 'NOT_EVALUATED'
        ProviderCalls = 'NOT_EVALUATED'
        DatabaseWrites = 'NOT_EVALUATED'
        RawDispatches = 'NOT_EVALUATED'
        Redispatches = 'NOT_EVALUATED'
        DiagnosticRecord = $diagnosticRecord
    }
}

if (-not $LibraryOnly) {
    throw 'P5E_A43_PRE_RESERVATION_LIBRARY_ONLY_STOP'
}
