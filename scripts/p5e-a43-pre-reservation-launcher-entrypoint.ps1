[CmdletBinding()]
param(
    [switch]$LibraryOnly,
    [switch]$Execute,
    [string]$RepoRoot = '',
    [string]$PrivateRoot = '',
    [string]$AndroidSdkPath = '',
    [string]$LocalPropertiesPath = '',
    [string]$AdbPath = '',
    [string]$JavaPath = '',
    [string]$ApkSignerJarPath = '',
    [string]$BuildToolsVersion = '35.0.0',
    [string]$PowershellPath = '',
    [string]$LauncherPath = '',
    [string]$ReservationRoot = '',
    [string]$AuditPath = '',
    [string]$DecisionId = '',
    [string]$PacketIdentifier = 'P5E-A43-DECISION-ATOMICITY-ENV-20260928-01',
    [string]$Serial = '15e84958',
    [string]$Scope = 'RAW/GLOSSARY',
    [string]$ExpectedCandidateSha256 = '',
    [string]$CommandSourcePath = '',
    [string[]]$ChildArgumentList = @(),
    [string]$ChildWorkingDirectory = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Dot-sourced libraries bind their own switches in this scope. Preserve the
# entrypoint intent before loading them so LibraryOnly cannot mask execution.
$script:P5EA43EntrypointLibraryOnly = [bool]$LibraryOnly
$script:P5EA43EntrypointExecute = [bool]$Execute
$script:P5EA43EntrypointArguments = @{}
foreach ($entryArgument in @('LibraryOnly','Execute','RepoRoot','PrivateRoot',
        'AndroidSdkPath','LocalPropertiesPath','AdbPath','JavaPath','ApkSignerJarPath',
        'BuildToolsVersion','PowershellPath','LauncherPath','ReservationRoot',
        'AuditPath','DecisionId','PacketIdentifier','Serial','Scope',
        'ExpectedCandidateSha256','CommandSourcePath','ChildArgumentList','ChildWorkingDirectory')) {
    $script:P5EA43EntrypointArguments[$entryArgument] = Get-Variable -Name $entryArgument -ValueOnly
}

# This is the executable integration source.  It is safe to dot-source with
# -LibraryOnly for offline QA.  Direct execution requires an external
# candidate hash and explicit -Execute; no self-hash is embedded here.
$script:P5EA43IntegrationVersion = 'p5e.a43.pre-reservation.integration.v1'
$script:P5EA43EndpointEnvironmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$script:P5EA43ReceiptEnvironmentNames = @(
    'P5E_OWNER_DECISION_RECEIPT_PATH',
    'P5E_OWNER_DECISION_RECEIPT_SHA256',
    'P5E_A43_COMMAND_SHA256',
    'P5E_A43_RESERVATION_RECEIPT_SHA256',
    'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
)

$script:P5EA43IntegrationExpectedHashes = [ordered]@{
    Parent = '9183619AD9C3BA5E5C0AF2701968F1F2121894472A8FD136C0DC6B54898F9A8D'
    ChildContract = 'B181801B07E04DC29F373FE092DF7A4F8FE5FF3CBFA0CF3725CA211D81C7A2DB'
    Toolchain = 'C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8'
    RuntimeGuards = '5F78F59FCFE5FD7BB904C8D0B4815CA7C714E03999DB97CDCA2737E5F4C3DC3E'
    ExpectedDigest = '1D6C1DEA14E70001F81DB841668969A51DB15417436E742BA018837BFA587A9D'
    Manifest = '3635E810484973BF2CBC55D2B54C47F884630C3F399E3F7C7DF65D5F4C84F162'
    Command = '7F2B0FA423C2016358F891DA9193DFCEBB3C022480D48FD3E9CA82BCBD38ADEC'
    Helper = '9963A027F8CD91E4252D5C7FE40CA10B5E8D40F6147B5896E6F97D1A9FE0892E'
    Exporter = '813F6ED0ABD110EBF32550990940E975971988FBCF806DC26464EE311021CF99'
    Bridge = '4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111'
}

function Get-P5EA43IntegrationRepoRoot {
    param([string]$Value = '')
    if (-not [string]::IsNullOrWhiteSpace($Value)) { return [IO.Path]::GetFullPath($Value) }
    return [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
}

function Get-P5EA43IntegrationPinTable {
    param([string]$Root = '')
    $base = Get-P5EA43IntegrationRepoRoot -Value $Root
    return [ordered]@{
        Parent = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-a43-pre-reservation-launcher.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Parent }
        ChildContract = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-child-invocation-contract.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.ChildContract }
        Toolchain = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-raw-toolchain.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Toolchain }
        RuntimeGuards = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-a43-runtime-guards.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.RuntimeGuards }
        ExpectedDigest = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-load-expected-digest.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.ExpectedDigest }
        Manifest = [pscustomobject][ordered]@{ Path = Join-Path $base 'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Manifest }
        Command = [pscustomobject][ordered]@{ Path = Join-Path $base 'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Command }
        Helper = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-raw-live-supervisor.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Helper }
        Exporter = [pscustomobject][ordered]@{ Path = Join-Path $base 'scripts\p5e-db-binary-export.ps1'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Exporter }
        Bridge = [pscustomobject][ordered]@{ Path = Join-Path $base 'docs\P5E_SQLITE_BRIDGE.py'; Sha256 = $script:P5EA43IntegrationExpectedHashes.Bridge }
    }
}

function Get-P5EA43IntegrationSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
}

function Assert-P5EA43IntegrationLeaf {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Label
    )
    $literal = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literal -PathType Leaf)) { throw ($Label + '_MISSING_STOP') }
    $item = Get-Item -LiteralPath $literal -Force -ErrorAction Stop
    if ($item.PSIsContainer) { throw ($Label + '_NOT_REGULAR_STOP') }
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw ($Label + '_REPARSE_STOP') }
    $resolved = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literal -ErrorAction Stop).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literal, $resolved)) { throw ($Label + '_PATH_SWAP_STOP') }
    return $literal
}

function Assert-P5EA43IntegrationPinnedFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedSha256,
        [Parameter(Mandatory = $true)][string]$Label
    )
    [void](Assert-P5EA43IntegrationLeaf -Path $Path -Label $Label)
    if ($ExpectedSha256 -notmatch '^[0-9A-Fa-f]{64}$') { throw ($Label + '_EXPECTED_HASH_INVALID_STOP') }
    $actual = Get-P5EA43IntegrationSha256 -Path $Path
    if ($actual -cne $ExpectedSha256.ToUpperInvariant()) { throw ($Label + '_HASH_MISMATCH_STOP') }
    return $actual
}

function Assert-P5EA43IntegrationPinnedInputs {
    param([Parameter(Mandatory = $true)][System.Collections.IDictionary]$Pins)
    foreach ($name in @('Parent', 'ChildContract', 'Toolchain', 'RuntimeGuards', 'ExpectedDigest', 'Manifest', 'Command', 'Helper', 'Exporter', 'Bridge')) {
        $pin = $Pins[$name]
        if ($null -eq $pin) { throw 'P5E_A43_INTEGRATION_PIN_TABLE_INVALID_STOP' }
        [void](Assert-P5EA43IntegrationPinnedFile -Path ([string]$pin.Path) -ExpectedSha256 ([string]$pin.Sha256) -Label ('P5E_INTEGRATION_' + $name.ToUpperInvariant()))
    }
    return $true
}

function Clear-P5EA43IntegrationEnvironment {
    foreach ($name in $script:P5EA43ReceiptEnvironmentNames) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
}

function Get-P5EA43SecureStringText {
    param([Parameter(Mandatory = $true)][Security.SecureString]$SecureString)
    $bstr = [IntPtr]::Zero
    try {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
    }
}

function Get-P5EA43IntegrationNowMilliseconds {
    param([hashtable]$Seams = @{})
    if ($Seams.ContainsKey('NowProvider') -and $null -ne $Seams.NowProvider) {
        return [long](& $Seams.NowProvider)
    }
    return [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
}

function Convert-P5EA43ReceiptErrorToParentCode {
    param([Parameter(Mandatory = $true)]$ErrorRecord)
    $message = ''
    try { $message = [string]$ErrorRecord.Exception.Message } catch { $message = '' }
    $map = @{
        'P5E_OWNER_RECEIPT_EXPIRED_STOP' = 'RECEIPT_EXPIRED_STOP'
        'P5E_OWNER_RECEIPT_FUTURE_ISSUED_STOP' = 'RECEIPT_ISSUED_IN_FUTURE'
        'P5E_OWNER_RECEIPT_TIME_SHAPE_STOP' = 'P5E_OWNER_RECEIPT_INVALID_STOP'
        'P5E_OWNER_RECEIPT_HASH_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_SERIAL_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_SCOPE_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_MANIFESTSHA256_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_COMMANDSHA256_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_HELPERSHA256_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_EXPORTERSHA256_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
        'P5E_OWNER_RECEIPT_BRIDGESHA256_MISMATCH_STOP' = 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP'
    }
    if ($map.ContainsKey($message)) { return [string]$map[$message] }
    return 'P5E_OWNER_RECEIPT_INVALID_STOP'
}

function Get-P5EA43IntegrationExpectedPins {
    return [hashtable]@{
        manifestSha256 = $script:P5EA43IntegrationExpectedHashes.Manifest.ToLowerInvariant()
        commandSha256 = $script:P5EA43IntegrationExpectedHashes.Command.ToLowerInvariant()
        helperSha256 = $script:P5EA43IntegrationExpectedHashes.Helper.ToLowerInvariant()
        exporterSha256 = $script:P5EA43IntegrationExpectedHashes.Exporter.ToLowerInvariant()
        bridgeSha256 = $script:P5EA43IntegrationExpectedHashes.Bridge.ToLowerInvariant()
    }
}

function New-P5EA43ExecutableContract {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][hashtable]$Configuration,
        [Parameter(Mandatory = $true)][string]$CandidateSha256,
        [System.Collections.IDictionary]$PinTable = $(Get-P5EA43IntegrationPinTable)
    )
    if ($CandidateSha256 -notmatch '^[0-9A-Fa-f]{64}$') { throw 'P5E_A43_CANDIDATE_HASH_REQUIRED_STOP' }
    Assert-P5EA43IntegrationPinnedInputs -Pins $PinTable | Out-Null
    foreach ($name in @('RepoRoot', 'PrivateRoot', 'PowershellPath', 'LauncherPath', 'ReservationRoot', 'AuditPath', 'AndroidSdkPath', 'JavaPath', 'DecisionId', 'PacketIdentifier', 'Serial', 'Scope', 'CommandSourcePath', 'CommandSourceSha256', 'ChildWorkingDirectory')) {
        if (-not $Configuration.ContainsKey($name) -or [string]::IsNullOrWhiteSpace([string]$Configuration[$name])) {
            throw 'P5E_A43_INTEGRATION_CONTRACT_MISSING_STOP'
        }
    }
    [void](Assert-P5EA43IntegrationLeaf -Path ([string]$Configuration.LauncherPath) -Label 'P5E_A43_CANDIDATE')
    $actualCandidate = Get-P5EA43IntegrationSha256 -Path ([string]$Configuration.LauncherPath)
    if ($actualCandidate -cne $CandidateSha256.ToUpperInvariant()) { throw 'P5E_A43_CANDIDATE_HASH_MISMATCH_STOP' }
    if ($Configuration.CommandSourceSha256 -notmatch '^[0-9A-Fa-f]{64}$') { throw 'P5E_A43_COMMAND_COPY_HASH_INVALID_STOP' }
    [void](Assert-P5EA43IntegrationPinnedFile -Path ([string]$Configuration.CommandSourcePath) -ExpectedSha256 ([string]$Configuration.CommandSourceSha256) -Label 'P5E_A43_COMMAND_SOURCE')
    if (-not (Test-Path -LiteralPath ([string]$Configuration.RepoRoot) -PathType Container)) { throw 'P5E_REPO_ROOT_MISSING_STOP' }
    if (-not (Test-Path -LiteralPath ([string]$Configuration.PrivateRoot) -PathType Container)) {
        throw 'P5E_PRIVATE_ROOT_MISSING_STOP'
    }
    $auditRoot = Split-Path -Parent ([IO.Path]::GetFullPath([string]$Configuration.AuditPath))
    if (-not (Test-Path -LiteralPath $auditRoot -PathType Container)) {
        [void][IO.Directory]::CreateDirectory($auditRoot)
    }
    [void](Assert-P5EA43NoReparsePath -Path $auditRoot -Kind Container -Label 'P5E_A43_AUDIT_ROOT' -Stage 'ENTRY')
    if (Test-Path -LiteralPath ([string]$Configuration.AuditPath)) { throw 'P5E_A43_AUDIT_ALREADY_EXISTS_STOP' }
    $childArgs = @()
    if ($Configuration.ContainsKey('ChildArgumentList') -and $null -ne $Configuration.ChildArgumentList) {
        $childArgs = @($Configuration.ChildArgumentList | ForEach-Object { [string]$_ })
    }
    return [hashtable]@{
        RepoRoot = [IO.Path]::GetFullPath([string]$Configuration.RepoRoot)
        PrivateRoot = [IO.Path]::GetFullPath([string]$Configuration.PrivateRoot)
        PowershellPath = [IO.Path]::GetFullPath([string]$Configuration.PowershellPath)
        LauncherPath = [IO.Path]::GetFullPath([string]$Configuration.LauncherPath)
        ReservationRoot = [IO.Path]::GetFullPath([string]$Configuration.ReservationRoot)
        AuditPath = [IO.Path]::GetFullPath([string]$Configuration.AuditPath)
        SdkPath = [IO.Path]::GetFullPath([string]$Configuration.AndroidSdkPath)
        LocalPropertiesPath = [string]$Configuration.LocalPropertiesPath
        AdbPath = [string]$Configuration.AdbPath
        JavaPath = [IO.Path]::GetFullPath([string]$Configuration.JavaPath)
        ApkSignerJarPath = [string]$Configuration.ApkSignerJarPath
        BuildToolsVersion = [string]$Configuration.BuildToolsVersion
        ApprovalLiteral = 'APPROVE_ONE_FRESH_EVENT'
        DecisionId = [string]$Configuration.DecisionId
        PacketIdentifier = [string]$Configuration.PacketIdentifier
        Serial = [string]$Configuration.Serial
        Scope = [string]$Configuration.Scope
        ExpectedPins = Get-P5EA43IntegrationExpectedPins
        CommandSourcePath = [IO.Path]::GetFullPath([string]$Configuration.CommandSourcePath)
        CommandSourceSha256 = [string]$Configuration.CommandSourceSha256.ToUpperInvariant()
        ChildArgumentList = $childArgs
        ChildWorkingDirectory = [IO.Path]::GetFullPath([string]$Configuration.ChildWorkingDirectory)
        ExpectedEnvironmentName = $script:P5EA43EndpointEnvironmentName
        ChildTimeoutMilliseconds = if ($Configuration.ContainsKey('ChildTimeoutMilliseconds')) { [long]$Configuration.ChildTimeoutMilliseconds } else { 300000L }
        ReceiptValidityMilliseconds = if ($Configuration.ContainsKey('ReceiptValidityMilliseconds')) { [long]$Configuration.ReceiptValidityMilliseconds } else { 1800000L }
        ExpectedLauncherSha256 = $CandidateSha256.ToUpperInvariant()
        PinnedFiles = @(
            [pscustomobject][ordered]@{ Path = $PinTable.Parent.Path; Sha256 = $PinTable.Parent.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.ChildContract.Path; Sha256 = $PinTable.ChildContract.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.Toolchain.Path; Sha256 = $PinTable.Toolchain.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.RuntimeGuards.Path; Sha256 = $PinTable.RuntimeGuards.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.ExpectedDigest.Path; Sha256 = $PinTable.ExpectedDigest.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.Manifest.Path; Sha256 = $PinTable.Manifest.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.Command.Path; Sha256 = $PinTable.Command.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.Helper.Path; Sha256 = $PinTable.Helper.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.Exporter.Path; Sha256 = $PinTable.Exporter.Sha256 }
            [pscustomobject][ordered]@{ Path = $PinTable.Bridge.Path; Sha256 = $PinTable.Bridge.Sha256 }
        )
    }
}

function Add-P5EA43IntegrationCall {
    param([Parameter(Mandatory = $true)][hashtable]$State,[Parameter(Mandatory = $true)][string]$Name)
    if (-not $State.ContainsKey($Name)) { $State[$Name] = 0 }
    $State[$Name] = [int]$State[$Name] + 1
}

function New-P5EA43ExecutableDependencies {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][hashtable]$Contract,
        [hashtable]$Seams = @{},
        [hashtable]$State = $(New-Object hashtable)
    )
    foreach ($name in @('ResolveToolchain', 'ReadApproval', 'ReserveLauncher', 'CreateOwnerRoot', 'CreateReceipt', 'ValidateReceipt', 'ReadKey', 'InvokeChild', 'ClearKey', 'WriteDiagnostic')) {
        if (-not $State.ContainsKey($name)) { $State[$name] = 0 }
    }
    $resolverInvoker = if ($Seams.ContainsKey('ResolveToolchainInvoker') -and $null -ne $Seams.ResolveToolchainInvoker) {
        $Seams.ResolveToolchainInvoker
    } else {
        {
            param($contract)
            $sdk = if ([string]::IsNullOrWhiteSpace([string]$contract.SdkPath)) { '' } else { [string]$contract.SdkPath }
            $local = if ($contract.ContainsKey('LocalPropertiesPath')) { [string]$contract.LocalPropertiesPath } else { '' }
            $adb = if ($contract.ContainsKey('AdbPath')) { [string]$contract.AdbPath } else { '' }
            $java = [string]$contract.JavaPath
            $jar = if ($contract.ContainsKey('ApkSignerJarPath')) { [string]$contract.ApkSignerJarPath } else { '' }
            return Resolve-P5ERawToolchain -AndroidSdkPath $sdk -LocalPropertiesPath $local -AdbPath $adb -JavaPath $java -ApkSignerJarPath $jar -BuildToolsVersion ([string]$contract.BuildToolsVersion)
        }.GetNewClosure()
    }
    $approvalReader = if ($Seams.ContainsKey('ApprovalReader') -and $null -ne $Seams.ApprovalReader) { $Seams.ApprovalReader } else {
        { param($prompt) return (Read-Host -Prompt $prompt -AsSecureString) }.GetNewClosure()
    }
    $keyReader = if ($Seams.ContainsKey('KeyReader') -and $null -ne $Seams.KeyReader) { $Seams.KeyReader } else {
        { param($prompt) return (Read-Host -Prompt $prompt -AsSecureString) }.GetNewClosure()
    }
    $expectedEnvironmentName = [string]$Contract.ExpectedEnvironmentName
    $fingerprintLoader = if ($Seams.ContainsKey('FingerprintLoader') -and $null -ne $Seams.FingerprintLoader) { $Seams.FingerprintLoader } else {
        { param($secureKey) Invoke-P5EExpectedFingerprintLoadFromSecureKey -ApiKey $secureKey -EnvironmentName $expectedEnvironmentName }.GetNewClosure()
    }
    $nowProvider = if ($Seams.ContainsKey('NowProvider') -and $null -ne $Seams.NowProvider) { $Seams.NowProvider } else { { [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() }.GetNewClosure() }
    $childOverride = if ($Seams.ContainsKey('ChildInvocationOverride') -and $null -ne $Seams.ChildInvocationOverride) { $Seams.ChildInvocationOverride } else { $null }
    $afterKeyHook = if ($Seams.ContainsKey('AfterKeyHook') -and $null -ne $Seams.AfterKeyHook) { $Seams.AfterKeyHook } else { $null }
    $cleanupShouldThrow = [bool]($Seams.ContainsKey('CleanupShouldThrow') -and $Seams.CleanupShouldThrow)
    $diagnosticWriterOverride = if ($Seams.ContainsKey('DiagnosticWriter') -and $null -ne $Seams.DiagnosticWriter) { $Seams.DiagnosticWriter } else { $null }
    $state.SecureKey = $null
    $state.ReceiptPath = ''
    $state.ReceiptSha256 = ''
    $state.CommandCopyPath = ''
    $state.ReceiptRevalidationCalls = 0
    $state.ChildContractCalls = 0
    $state.ReadKeySelfCleared = 0
    $state.EnvironmentWasSet = $false
    $state.EnvironmentClearAfterRun = $false

    $dependencies = @{}
    $dependencies.ResolveToolchain = {
        param($parentContract)
        Add-P5EA43IntegrationCall -State $State -Name 'ResolveToolchain'
        $resolved = @(& $resolverInvoker $parentContract)
        if ($resolved.Count -ne 1 -or $null -eq $resolved[0]) { throw 'P5E_PRE_RESERVATION_TOOLCHAIN_RESULT_INVALID_STOP' }
        $toolchain = $resolved[0]
        $State.ResolvedSdkPath = if ($toolchain -is [System.Collections.IDictionary]) { [string]$toolchain['sdkPath'] } else { [string]$toolchain.sdkPath }
        $State.ResolvedAdbPath = if ($toolchain -is [System.Collections.IDictionary]) { [string]$toolchain['adbPath'] } else { [string]$toolchain.adbPath }
        $State.ResolvedJavaPath = if ($toolchain -is [System.Collections.IDictionary]) { [string]$toolchain['javaPath'] } else { [string]$toolchain.javaPath }
        $State.ResolvedApkSignerJarPath = if ($toolchain -is [System.Collections.IDictionary]) { [string]$toolchain['apksignerJarPath'] } else { [string]$toolchain.apksignerJarPath }
        return $toolchain
    }.GetNewClosure()
    $dependencies.ReadApproval = {
        param($parentContract)
        Add-P5EA43IntegrationCall -State $State -Name 'ReadApproval'
        $raw = & $approvalReader 'Type APPROVE_ONE_FRESH_EVENT exactly (input hidden)'
        if ($null -eq $raw) { return [pscustomobject][ordered]@{ State = 'EOF' } }
        if ($raw -is [System.Collections.IDictionary] -and $raw.Contains('State')) { return $raw }
        if ($null -ne $raw.PSObject.Properties['State'] -and $raw.PSObject.Properties['State'].Value -ne $null) { return $raw }
        $text = $null
        try {
            if ($raw -is [Security.SecureString]) { $text = Get-P5EA43SecureStringText -SecureString $raw } else { $text = [string]$raw }
            return [pscustomobject][ordered]@{ State = 'VALUE'; Value = $text }
        } finally { $text = $null }
    }.GetNewClosure()
    $dependencies.ReserveLauncher = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'ReserveLauncher'
        return New-P5EA43ReservationMarker -Context $context
    }.GetNewClosure()
    $dependencies.CreateOwnerRoot = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'CreateOwnerRoot'
        $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId ([string]$Contract.DecisionId)
        $ownerPath = Join-Path ([string]$Contract.PrivateRoot) ('p5e-a43-owner-' + $decisionKey)
        if (Test-Path -LiteralPath $ownerPath) { Stop-P5EA43Typed -Stage 'OWNER_ROOT' -Code 'P5E_OWNER_ROOT_ALREADY_EXISTS_STOP' }
        try { [void][IO.Directory]::CreateDirectory($ownerPath) } catch { Stop-P5EA43Typed -Stage 'OWNER_ROOT' -Code 'P5E_OWNER_ROOT_CREATE_FAILED_STOP' }
        [void](Assert-P5EA43NoReparsePath -Path $ownerPath -Kind Container -Label 'P5E_OWNER_ROOT' -Stage 'OWNER_ROOT')
        $State.OwnerRootPath = $ownerPath
        return [pscustomobject][ordered]@{ Created = $true; Path = $ownerPath }
    }.GetNewClosure()
    $dependencies.CreateReceipt = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'CreateReceipt'
        $issuedAt = [long](& $nowProvider)
        $expiresAt = $issuedAt + [long]$Contract.ReceiptValidityMilliseconds
        $record = [ordered]@{
            schemaVersion = 'p5e.a43.owner-decision-receipt.v1'
            decisionId = [string]$Contract.DecisionId
            packetIdentifier = [string]$Contract.PacketIdentifier
            serial = [string]$Contract.Serial
            manifestSha256 = [string]$Contract.ExpectedPins.manifestSha256
            commandSha256 = [string]$Contract.ExpectedPins.commandSha256
            helperSha256 = [string]$Contract.ExpectedPins.helperSha256
            exporterSha256 = [string]$Contract.ExpectedPins.exporterSha256
            bridgeSha256 = [string]$Contract.ExpectedPins.bridgeSha256
            oneEventLimit = 1
            oneProviderCallLimit = 1
            scope = [string]$Contract.Scope
            noRetry = $true
            noFallback = $true
            noRedispatch = $true
            issuedAt = $issuedAt
            expiresAt = $expiresAt
        }
        $path = Join-Path ([string]$context.OwnerRootPath) 'OWNER_DECISION_RECEIPT.json'
        Write-P5EA43CreateNewUtf8 -Path $path -Text ($record | ConvertTo-Json -Compress)
        $hash = Get-P5EA43Sha256Upper -Path $path
        $State.ReceiptPath = $path
        $State.ReceiptSha256 = $hash
        return [pscustomobject][ordered]@{ Created = $true; Path = $path; Sha256 = $hash }
    }.GetNewClosure()
    $dependencies.ValidateReceipt = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'ValidateReceipt'
        try {
            $validated = Read-P5EA43OwnerDecisionReceipt -ReceiptPath ([string]$State.ReceiptPath) -ExpectedReceiptSha256 ([string]$State.ReceiptSha256) -ExpectedPins $Contract.ExpectedPins -ExpectedSerial ([string]$Contract.Serial) -ExpectedScope ([string]$Contract.Scope) -NowUnixMilliseconds ([long](& $nowProvider))
            return [pscustomobject][ordered]@{ Valid = $true; TypedCode = 'RECEIPT_CURRENT'; Receipt = $validated }
        } catch {
            return [pscustomobject][ordered]@{ Valid = $false; TypedCode = Convert-P5EA43ReceiptErrorToParentCode -ErrorRecord $_ }
        }
    }.GetNewClosure()
    $dependencies.ReadKey = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'ReadKey'
        $secureKey = $null
        $succeeded = $false
        try {
            $raw = & $keyReader 'Enter full OpenRouter API key (hidden)'
            if ($null -eq $raw) { Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP' }
            if ($raw -is [System.Collections.IDictionary] -and $raw.Contains('State')) {
                $keyState = [string]$raw['State']
                if ($keyState -eq 'EOF' -or $keyState -eq 'CANCELLED') { Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_CANCELLED_STOP' }
                if ($raw.Contains('SecureString')) { $secureKey = $raw['SecureString'] }
            } elseif ($null -ne $raw.PSObject.Properties['State'] -and [string]$raw.State -ne 'VALUE') {
                Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_CANCELLED_STOP'
            } else { $secureKey = $raw }
            if ($secureKey -isnot [Security.SecureString]) { Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP' }
            $State.SecureKey = $secureKey
            & $fingerprintLoader $secureKey
            [void](Assert-P5EA43ExpectedProcessValue)
            $State.EnvironmentWasSet = $true
            if ($null -ne $afterKeyHook) { & $afterKeyHook ([string]$State.ReceiptPath) }
            $State.ReceiptRevalidationCalls = [int]$State.ReceiptRevalidationCalls + 1
            try {
                [void](Read-P5EA43OwnerDecisionReceipt -ReceiptPath ([string]$State.ReceiptPath) -ExpectedReceiptSha256 ([string]$State.ReceiptSha256) -ExpectedPins $Contract.ExpectedPins -ExpectedSerial ([string]$Contract.Serial) -ExpectedScope ([string]$Contract.Scope) -NowUnixMilliseconds ([long](& $nowProvider)))
            } catch {
                Stop-P5EA43Typed -Stage 'KEY' -Code (Convert-P5EA43ReceiptErrorToParentCode -ErrorRecord $_)
            }
            [Environment]::SetEnvironmentVariable('P5E_OWNER_DECISION_RECEIPT_PATH', [IO.Path]::GetFullPath([string]$State.ReceiptPath), 'Process')
            [Environment]::SetEnvironmentVariable('P5E_OWNER_DECISION_RECEIPT_SHA256', ([string]$State.ReceiptSha256).ToLowerInvariant(), 'Process')
            [Environment]::SetEnvironmentVariable('P5E_A43_COMMAND_SHA256', ([string]$Contract.ExpectedPins.commandSha256).ToLowerInvariant(), 'Process')
            [Environment]::SetEnvironmentVariable('P5E_A43_RESERVATION_RECEIPT_SHA256', ([string]$State.ReceiptSha256).ToLowerInvariant(), 'Process')
            $succeeded = $true
            return [pscustomobject][ordered]@{ State = 'VALUE' }
        } catch {
            $typed = $false
            try { $typed = $null -ne $_.Exception.Data -and $_.Exception.Data.Contains('P5ECode') } catch { $typed = $false }
            try { Clear-P5EA43IntegrationEnvironment } catch { }
            if ($null -ne $secureKey) { try { $secureKey.Dispose() } catch { } }
            $State.SecureKey = $null
            $State.ReadKeySelfCleared = [int]$State.ReadKeySelfCleared + 1
            if ($typed) { throw }
            Stop-P5EA43Typed -Stage 'KEY' -Code 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP'
        } finally {
            if (-not $succeeded -and $null -ne $secureKey) { $secureKey = $null }
        }
    }.GetNewClosure()
    $dependencies.InvokeChild = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'InvokeChild'
        $copyPath = Join-Path ([string]$context.OwnerRootPath) ('P5E_A43_COMMAND_' + ([string]$Contract.DecisionId) + '.ps1')
        $bytes = [IO.File]::ReadAllBytes([string]$Contract.CommandSourcePath)
        try {
            $stream = $null
            try {
                $stream = [IO.File]::Open($copyPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
                $stream.Write($bytes, 0, $bytes.Length)
                $stream.Flush($true)
            } finally { if ($null -ne $stream) { $stream.Dispose() } }
        } finally { [Array]::Clear($bytes, 0, $bytes.Length) }
        [void](Assert-P5EA43PinnedFile -Path $copyPath -ExpectedSha256 ([string]$Contract.CommandSourceSha256) -Stage 'CHILD_LAUNCH')
        $State.CommandCopyPath = $copyPath
        $fingerprint = [Environment]::GetEnvironmentVariable($expectedEnvironmentName, 'Process')
        if ([string]::IsNullOrWhiteSpace($fingerprint) -or $fingerprint -cnotmatch '^[0-9a-f]{64}$') { Stop-P5EA43Typed -Stage 'CHILD_LAUNCH' -Code 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP' }
        $childArgs = @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $copyPath) + @($Contract.ChildArgumentList)
        $State.ChildContractCalls = [int]$State.ChildContractCalls + 1
        if ($null -ne $childOverride) { return (& $childOverride $context $Contract $copyPath $fingerprint $childArgs) }
        return Invoke-P5EPhaseChildInvocation -Phase 'Dispatch' -ExpectedEnvironmentName $expectedEnvironmentName -ExpectedEnvironmentValue $fingerprint -FilePath ([string]$Contract.PowershellPath) -ArgumentList $childArgs -WorkingDirectory ([string]$Contract.ChildWorkingDirectory) -TimeoutMilliseconds ([long]$Contract.ChildTimeoutMilliseconds) -SensitiveValues @($fingerprint)
    }.GetNewClosure()
    $dependencies.ClearKey = {
        param($context)
        Add-P5EA43IntegrationCall -State $State -Name 'ClearKey'
        Clear-P5EA43IntegrationEnvironment
        if ($null -ne $State.SecureKey) { try { $State.SecureKey.Dispose() } catch { } }
        $State.SecureKey = $null
        $State.EnvironmentClearAfterRun = $true
        if ($cleanupShouldThrow) { throw (New-Object System.InvalidOperationException 'P5E_SYNTHETIC_CLEANUP_FAILURE') }
        return $true
    }.GetNewClosure()
    $dependencies.WriteDiagnostic = {
        param($path, $record)
        Add-P5EA43IntegrationCall -State $State -Name 'WriteDiagnostic'
        if ($null -ne $diagnosticWriterOverride) { return (& $diagnosticWriterOverride $path $record) }
        return Write-P5EA43DiagnosticCreateNew -Path $path -Record $record
    }.GetNewClosure()
    return $dependencies
}

function Invoke-P5EA43ExecutableLauncher {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][hashtable]$Contract,
        [hashtable]$Seams = @{},
        [hashtable]$State = $(New-Object hashtable)
    )
    $dependencies = New-P5EA43ExecutableDependencies -Contract $Contract -Seams $Seams -State $State
    return Invoke-P5EA43PreReservation -Contract $Contract -Dependencies $dependencies
}

$script:P5EA43IntegrationRepoRoot = Get-P5EA43IntegrationRepoRoot -Value $RepoRoot
$script:P5EA43IntegrationPins = Get-P5EA43IntegrationPinTable -Root $script:P5EA43IntegrationRepoRoot
Assert-P5EA43IntegrationPinnedInputs -Pins $script:P5EA43IntegrationPins | Out-Null
# Keep the library functions in the entrypoint's script scope.  Dot-sourcing
# them from a helper function would make the functions disappear when that
# helper returns, which would break a clean PowerShell process.
. $script:P5EA43IntegrationPins.RuntimeGuards.Path -LibraryOnly
. $script:P5EA43IntegrationPins.ExpectedDigest.Path -LibraryOnly
. $script:P5EA43IntegrationPins.Toolchain.Path -LibraryOnly
. $script:P5EA43IntegrationPins.ChildContract.Path -LibraryOnly
. $script:P5EA43IntegrationPins.Parent.Path -LibraryOnly

# The child library also dot-sources the supervisor, whose parameters include
# SDK/Java/serial paths. Restore every entry argument, not just the mode flags.
foreach ($entryArgument in $script:P5EA43EntrypointArguments.Keys) {
    Set-Variable -Name $entryArgument -Value $script:P5EA43EntrypointArguments[$entryArgument] -Scope Script
}

if ($script:P5EA43EntrypointLibraryOnly) { return }
if (-not $script:P5EA43EntrypointExecute) { throw 'P5E_A43_EXECUTION_MODE_REQUIRED_STOP' }
if ([string]::IsNullOrWhiteSpace($PrivateRoot) -or [string]::IsNullOrWhiteSpace($DecisionId) -or [string]::IsNullOrWhiteSpace($ExpectedCandidateSha256)) {
    throw 'P5E_A43_EXECUTION_ARGUMENTS_REQUIRED_STOP'
}
if ([string]::IsNullOrWhiteSpace($PowershellPath)) { $PowershellPath = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe' }
if ([string]::IsNullOrWhiteSpace($LauncherPath)) { $LauncherPath = [IO.Path]::GetFullPath($MyInvocation.MyCommand.Path) }
if ([string]::IsNullOrWhiteSpace($ReservationRoot)) { $ReservationRoot = Join-Path $PrivateRoot '.p5e-a43-pre-reservation-reservations' }
if ([string]::IsNullOrWhiteSpace($AuditPath)) { $AuditPath = Join-Path $PrivateRoot ('.p5e-a43-audit\' + $ExpectedCandidateSha256.ToLowerInvariant() + '.json') }
if ([string]::IsNullOrWhiteSpace($CommandSourcePath)) { $CommandSourcePath = $script:P5EA43IntegrationPins.Command.Path }
if ([string]::IsNullOrWhiteSpace($ChildWorkingDirectory)) { $ChildWorkingDirectory = $script:P5EA43IntegrationRepoRoot }
$config = @{
    RepoRoot = $script:P5EA43IntegrationRepoRoot
    PrivateRoot = $PrivateRoot
    PowershellPath = $PowershellPath
    LauncherPath = $LauncherPath
    ReservationRoot = $ReservationRoot
    AuditPath = $AuditPath
    AndroidSdkPath = $AndroidSdkPath
    LocalPropertiesPath = $LocalPropertiesPath
    AdbPath = $AdbPath
    JavaPath = $JavaPath
    ApkSignerJarPath = $ApkSignerJarPath
    BuildToolsVersion = $BuildToolsVersion
    DecisionId = $DecisionId
    PacketIdentifier = $PacketIdentifier
    Serial = $Serial
    Scope = $Scope
    CommandSourcePath = $CommandSourcePath
    CommandSourceSha256 = $script:P5EA43IntegrationPins.Command.Sha256
    ChildArgumentList = $ChildArgumentList
    ChildWorkingDirectory = $ChildWorkingDirectory
}
$contract = New-P5EA43ExecutableContract -Configuration $config -CandidateSha256 $ExpectedCandidateSha256 -PinTable $script:P5EA43IntegrationPins
$state = New-Object hashtable
$result = Invoke-P5EA43ExecutableLauncher -Contract $contract -State $state
$result | ConvertTo-Json -Depth 16 -Compress
exit ([int]$result.OuterExitCode)
