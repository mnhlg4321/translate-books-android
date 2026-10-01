# Lean host script for ONE M4 RECONCILE event (L1_RECONCILE on top of the committed event-7 RAW attempt).
#
# Reuses the RAW supervisor library through dot-sourcing (-LibraryOnly) instead of extending it:
#   Resolve-P5ERawToolchain, Get-P5EPackageReadback, Get-P5EDeviceFileHashState,
#   Invoke-P5EDatabaseSnapshotExport, Assert-P5EDatabaseSnapshotStable, Invoke-P5EHostSqliteBridge,
#   Initialize-P5ECollectorCommandLog, New-P5EAdbArgumentList, ConvertFrom-P5EPosixCommandLine,
#   Invoke-P5EProcessSupervisor (the one am-instrument launch), Protect-P5ECaptureText (via the supervisor),
#   Get-P5ESha256, Write-P5EUtf8NoBom, ConvertTo-P5EJson, ConvertTo-P5ESqlLiteral, Get-P5EProperty.
#
# Live mode performs exactly one `am instrument -w -r` launch and never retries or relaunches.
# -SelfTest uses no adb, no device, no provider: host SQLite fixtures plus stubbed device functions.
#
# Instrumentation argument names are read from
#   app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EReconcileLiveInstrumentedTest.java
# (present when this script was written). Both the live path and the self-test compare the argument
# key set sent by this script with the keys that test file requires; a mismatch is a typed stop
# (live: before launch). Values that the task did not give as parameters and that are therefore
# defaulted here (reconcile them if the test changes):
#   p5e_expected_device_signature_token = 'abebea4b'  (RAW constant; -ExpectedDeviceSignatureToken overrides)
#   p5e_expected_schema_version         = 25          (-ExpectedSchemaVersion overrides)
#   p5e_expected_route_fingerprint      = RAW route fingerprint constant from the library
#   p5e_maximum_* limits                = RAW limits (1/0/0, 100000, 4096, 104096, 0.05, 120000)
#   p5e_cancellation_stop_authority     = the RECONCILE_ONLY string in the live test
# Optional toolchain parameters (-AndroidSdkPath, -LocalPropertiesPath, -AdbPath, -JavaPath,
# -ApkSignerJarPath, -BuildToolsVersion) feed Resolve-P5ERawToolchain exactly as the RAW collector does.
[CmdletBinding()]
param(
    [string]$Serial = '',
    [string]$EvidenceRoot = '',
    [string]$ExpectedProductionApkSha256 = '',
    [string]$ExpectedProductionVersion = '',
    [long]$ExpectedProductionVersionCode = 0,
    [string]$ExpectedTestApkSha256 = '',
    [string]$ExpectedCertificateSha256 = '',
    [string]$ExpectedDbSha256 = '',
    [string]$ExpectedEndpointAccountFingerprint = '',
    [long]$ExpectedProjectRowId = 0,
    [string]$AuthorizationId = '',
    [string]$OwnerApprovalManifestSha256 = '',
    [string]$TestSourceCommit = '',
    [int]$ExpectedSchemaVersion = 25,
    [string]$ExpectedDeviceSignatureToken = 'abebea4b',
    [string]$AndroidSdkPath = '',
    [string]$LocalPropertiesPath = '',
    [string]$AdbPath = '',
    [string]$JavaPath = '',
    [string]$ApkSignerJarPath = '',
    [string]$BuildToolsVersion = '',
    [switch]$SelfTest,
    [switch]$LibraryOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Dot-sourcing the supervisor executes its param block in this scope and would overwrite same-named
# variables (Serial, EvidenceDirectory, AdbPath, ...). Capture every parameter first and restore after.
$script:M4ParameterNames = @('Serial', 'EvidenceRoot', 'ExpectedProductionApkSha256', 'ExpectedProductionVersion',
    'ExpectedProductionVersionCode', 'ExpectedTestApkSha256', 'ExpectedCertificateSha256', 'ExpectedDbSha256',
    'ExpectedEndpointAccountFingerprint', 'ExpectedProjectRowId', 'AuthorizationId', 'OwnerApprovalManifestSha256',
    'TestSourceCommit', 'ExpectedSchemaVersion', 'ExpectedDeviceSignatureToken', 'AndroidSdkPath',
    'LocalPropertiesPath', 'AdbPath', 'JavaPath', 'ApkSignerJarPath', 'BuildToolsVersion', 'SelfTest', 'LibraryOnly')
$script:M4P = @{}
foreach ($m4Name in $script:M4ParameterNames) { $script:M4P[$m4Name] = Get-Variable -Name $m4Name -ValueOnly }
$script:M4ScriptPath = $PSCommandPath
$script:M4ScriptRoot = $PSScriptRoot

. (Join-Path $script:M4ScriptRoot 'p5e-raw-live-supervisor.ps1') -LibraryOnly

foreach ($m4Name in $script:M4ParameterNames) { Set-Variable -Name $m4Name -Value $script:M4P[$m4Name] }
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ----------------------------------------------------------------------------------------------
# Constants (identical to EditorialP5EFreshRawLiveRunner.java; the self-test compares them to the source)
# ----------------------------------------------------------------------------------------------
$script:M4RawPredecessor = '7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e'
$script:M4ClassName = 'com.ml.tblandroidtxt.EditorialP5EReconcileLiveInstrumentedTest'
$script:M4ClassMethod = $script:M4ClassName + '#authorizedReconcileRunsOnlyWhenExplicitlyOptedIn'
$script:M4Runner = 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'
$script:M4Selector = 'p5e-fresh-mercedes-vol5-20260911-01'
$script:M4ChapterKey = '001'
$script:M4Binding = '845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf'
$script:M4Run = '8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc'
$script:M4Evaluation = '3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1'
$script:M4PackHash = '497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d'
$script:M4ProfileHash = 'beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21'
$script:M4Phase = 'L1_RECONCILE'
$script:M4RawPhase = 'L1_RAW_DISCOVERY'
$script:M4StopAuthority = 'OWNER_CONTROLLED|RECONCILE_ONLY|NO_SCHEMA_REPAIR|NO_AUTOMATIC_RETRY|NO_RESPONSE_HEALING|NO_FALLBACK|PRESERVE_DURABLE_RECOVERY_STATE'
$script:M4OutcomeSchema = 'p5e.m4.reconcile-outcome.v1'
$script:M4LiveTestSourceRelativePath = 'app\src\androidTest\java\com\ml\tblandroidtxt\EditorialP5EReconcileLiveInstrumentedTest.java'
$script:M4AcceptedSchemaVersions = @(24L, 25L)
$script:M4Classifications = @('RECONCILE_COMMITTED', 'RECONCILE_TYPED_STOP', 'RECONCILE_NOT_DISPATCHED', 'EVIDENCE_INCOMPLETE')

# ----------------------------------------------------------------------------------------------
# Pure helpers
# ----------------------------------------------------------------------------------------------
function Get-M4Sha256OfText {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Text)
    $algorithm = [Security.Cryptography.SHA256]::Create()
    try { return (-join ($algorithm.ComputeHash([Text.UTF8Encoding]::new($false).GetBytes($Text)) | ForEach-Object { $_.ToString('x2') })) }
    finally { $algorithm.Dispose() }
}

function Test-M4Sha256 {
    param([AllowEmptyString()][string]$Value)
    return ($null -ne $Value) -and ($Value -match '^[0-9a-fA-F]{64}$')
}

function Get-M4RepoRoot {
    return [IO.Path]::GetFullPath((Join-Path $script:M4ScriptRoot '..'))
}

function Assert-M4LiveParameters {
    param([Parameter(Mandatory = $true)]$P)
    $missing = [System.Collections.Generic.List[string]]::new()
    foreach ($name in @('Serial', 'EvidenceRoot', 'ExpectedProductionApkSha256', 'ExpectedProductionVersion',
            'ExpectedTestApkSha256', 'ExpectedCertificateSha256', 'ExpectedDbSha256',
            'ExpectedEndpointAccountFingerprint', 'AuthorizationId', 'OwnerApprovalManifestSha256',
            'TestSourceCommit', 'BuildToolsVersion')) {
        if ([string]::IsNullOrWhiteSpace([string]$P[$name])) { [void]$missing.Add($name) }
    }
    if ([long]$P['ExpectedProductionVersionCode'] -le 0L) { [void]$missing.Add('ExpectedProductionVersionCode') }
    if ([long]$P['ExpectedProjectRowId'] -le 0L) { [void]$missing.Add('ExpectedProjectRowId') }
    if ($missing.Count -gt 0) { throw ('M4_PARAMETER_MISSING_STOP:' + ($missing -join ',')) }
    foreach ($name in @('ExpectedProductionApkSha256', 'ExpectedTestApkSha256', 'ExpectedCertificateSha256',
            'ExpectedDbSha256', 'ExpectedEndpointAccountFingerprint', 'OwnerApprovalManifestSha256')) {
        if (-not (Test-M4Sha256 -Value ([string]$P[$name]))) { throw ('M4_PARAMETER_NOT_SHA256_STOP:' + $name) }
    }
    if ([string]$P['TestSourceCommit'] -notmatch '^[0-9a-fA-F]{40}$') { throw 'M4_PARAMETER_TEST_SOURCE_COMMIT_INVALID_STOP' }
    if ([string]$P['ExpectedDeviceSignatureToken'] -notmatch '^[0-9a-fA-F]{8}$') { throw 'M4_PARAMETER_DEVICE_SIGNATURE_TOKEN_INVALID_STOP' }
    if ([string]$P['Serial'] -cne $script:P5ESerial) { throw 'M4_SERIAL_NOT_PINNED_STOP' }
    if (-not [IO.Path]::IsPathRooted([string]$P['EvidenceRoot']) -or
            [string]$P['EvidenceRoot'] -notmatch '^[A-Za-z]:[\\/]') { throw 'M4_EVIDENCE_ROOT_NOT_ABSOLUTE_STOP' }
}

function New-M4EventDirectory {
    param([Parameter(Mandatory = $true)][string]$Root)
    if ($Root -notmatch '^[A-Za-z]:[\\/]') { throw 'M4_EVIDENCE_ROOT_NOT_ABSOLUTE_STOP' }
    $full = [IO.Path]::GetFullPath($Root)
    if (-not (Test-Path -LiteralPath $full -PathType Container)) { throw 'M4_EVIDENCE_ROOT_MISSING_STOP' }
    $rootItem = Get-Item -LiteralPath $full -Force
    if (($rootItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw 'M4_EVIDENCE_ROOT_REPARSE_STOP' }
    $name = 'reconcile-m4-' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + '-' + [Guid]::NewGuid().ToString('N')
    $path = Join-Path $full $name
    if (Test-Path -LiteralPath $path) { throw 'M4_EVENT_DIRECTORY_EXISTS_STOP' }
    [void](New-Item -ItemType Directory -Path $path -ErrorAction Stop)
    return (Get-P5ECanonicalPath -Path $path)
}

function Write-M4Json {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)]$Value)
    if (Test-Path -LiteralPath $Path) { throw 'M4_EVIDENCE_FILE_EXISTS_STOP' }
    Write-P5EUtf8NoBom -Path $Path -Text (ConvertTo-P5EJson -Value $Value)
}

function Write-M4Manifest {
    param([Parameter(Mandatory = $true)][string]$EventDirectory)
    $manifestPath = Join-Path $EventDirectory 'M4_MANIFEST.sha256'
    $base = (Get-P5ECanonicalPath -Path $EventDirectory).TrimEnd('\') + '\'
    $lines = [System.Collections.Generic.List[string]]::new()
    $files = @(Get-ChildItem -LiteralPath $EventDirectory -Recurse -File -Force | Sort-Object FullName)
    foreach ($file in $files) {
        if ($file.FullName -ceq $manifestPath) { continue }
        $relative = $file.FullName.Substring($base.Length).Replace('\', '/')
        [void]$lines.Add((Get-P5ESha256 -Path $file.FullName) + '  ' + $relative)
    }
    Write-P5EUtf8NoBom -Path $manifestPath -Text (($lines -join "`n") + "`n")
    return [pscustomobject]@{ Path = $manifestPath; FileCount = $lines.Count; Sha256 = (Get-P5ESha256 -Path $manifestPath) }
}

# ----------------------------------------------------------------------------------------------
# Database readback (query, parser, before-state check, classification)
# ----------------------------------------------------------------------------------------------
function New-M4ReadbackQuery {
    $bindingLiteral = ConvertTo-P5ESqlLiteral $script:M4Binding
    $reportText = 'CAST(report_bytes AS TEXT)'
    $query = @"
SELECT 'SCHEMA' || char(9) || (SELECT user_version FROM pragma_user_version);
SELECT 'ATTEMPT' || char(9) || attempt_identity || char(9) || phase || char(9) || status || char(9) ||
 chapter_key || char(9) || binding_identity || char(9) || predecessor_identity || char(9) ||
 recovery_reason_code || char(9) || COALESCE(length(report_bytes), 'NULL') || char(9) ||
 COALESCE(length(receipt_bytes), 'NULL') || char(9) ||
 CASE WHEN report_bytes IS NULL THEN 'NULL' WHEN json_valid($reportText) THEN COALESCE(CAST(json_extract($reportText, '`$.phase') AS TEXT), 'MISSING') ELSE 'INVALID_JSON' END || char(9) ||
 CASE WHEN report_bytes IS NULL THEN 'NULL' WHEN json_valid($reportText) THEN COALESCE(CAST(json_extract($reportText, '`$.predecessorIdentity') AS TEXT), 'MISSING') ELSE 'INVALID_JSON' END
 FROM editorial_p5c_attempts WHERE binding_identity=$bindingLiteral ORDER BY created_at, attempt_identity;
SELECT 'COUNTS' || char(9) || (SELECT COUNT(*) FROM editorial_p5c_attempts) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_authorization_receipts) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_authorization_receipts WHERE exact_phase='L1_RECONCILE') || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_network_lifecycle) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation_history);
SELECT 'INTEGRITY_BEGIN';
PRAGMA integrity_check;
SELECT 'INTEGRITY_END';
"@
    return [regex]::Replace($query, '\s+', ' ').Trim()
}

function ConvertFrom-M4ReadbackOutput {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Output)
    $schema = $null
    $counts = $null
    $attempts = [System.Collections.Generic.List[object]]::new()
    $integrity = [System.Collections.Generic.List[string]]::new()
    $inIntegrity = $false
    foreach ($line in @($Output -split '\r?\n' | Where-Object { $_ -ne '' })) {
        if ($line -ceq 'INTEGRITY_BEGIN') { $inIntegrity = $true; continue }
        if ($line -ceq 'INTEGRITY_END') { $inIntegrity = $false; continue }
        if ($inIntegrity) { [void]$integrity.Add($line); continue }
        $c = $line.Split([char]9)
        switch ($c[0]) {
            'SCHEMA' {
                if ($c.Count -ne 2 -or $null -ne $schema) { throw 'M4_READBACK_SCHEMA_ROW_INVALID' }
                $schema = ConvertTo-P5ECollectorLong -Value $c[1] -Name 'schema'
            }
            'ATTEMPT' {
                if ($c.Count -ne 12) { throw 'M4_READBACK_ATTEMPT_ROW_INVALID' }
                $reportLength = $null
                $receiptLength = $null
                if ($c[8] -cne 'NULL') { $reportLength = ConvertTo-P5ECollectorLong -Value $c[8] -Name 'report-length' }
                if ($c[9] -cne 'NULL') { $receiptLength = ConvertTo-P5ECollectorLong -Value $c[9] -Name 'receipt-length' }
                [void]$attempts.Add([pscustomobject]@{
                    AttemptIdentity = $c[1]; Phase = $c[2]; Status = $c[3]; ChapterKey = $c[4]
                    BindingIdentity = $c[5]; PredecessorIdentity = $c[6]; RecoveryReasonCode = $c[7]
                    ReportByteLength = $reportLength; ReceiptByteLength = $receiptLength
                    ReportPhase = $c[10]; ReportPredecessorIdentity = $c[11]
                })
            }
            'COUNTS' {
                if ($c.Count -ne 7 -or $null -ne $counts) { throw 'M4_READBACK_COUNTS_ROW_INVALID' }
                $counts = @($c[1..6] | ForEach-Object { ConvertTo-P5ECollectorLong -Value $_ -Name 'count' })
            }
            default { throw ('M4_READBACK_UNEXPECTED_OUTPUT:' + $c[0]) }
        }
    }
    if ($null -eq $schema -or $null -eq $counts) { throw 'M4_READBACK_ROW_MISSING' }
    return [pscustomobject]@{
        Schema = [long]$schema
        Attempts = $attempts.ToArray()
        AttemptsTotal = [long]$counts[0]
        AuthorizationReceiptsTotal = [long]$counts[1]
        AuthorizationReceiptsReconcile = [long]$counts[2]
        LifecycleTotal = [long]$counts[3]
        ReconciliationTotal = [long]$counts[4]
        ReconciliationHistoryTotal = [long]$counts[5]
        IntegrityValues = $integrity.ToArray()
    }
}

function Test-M4BeforeState {
    # Returns typed error codes; empty means the exact event-7 state the RECONCILE may start from.
    param([Parameter(Mandatory = $true)]$Readback)
    $errors = [System.Collections.Generic.List[string]]::new()
    if ($Readback.Schema -notin $script:M4AcceptedSchemaVersions) { [void]$errors.Add('M4_BEFORE_SCHEMA_VERSION_UNEXPECTED') }
    if (@($Readback.IntegrityValues).Count -ne 1 -or @($Readback.IntegrityValues)[0] -cne 'ok') { [void]$errors.Add('M4_BEFORE_INTEGRITY_CHECK_NOT_OK') }
    $attempts = @($Readback.Attempts)
    if ($attempts.Count -ne 1) { [void]$errors.Add('M4_BEFORE_ATTEMPT_ROW_COUNT_NOT_ONE') }
    $reconcile = @($attempts | Where-Object { $_.Phase -ceq $script:M4Phase })
    if ($reconcile.Count -ne 0) { [void]$errors.Add('M4_BEFORE_RECONCILE_ROW_PRESENT') }
    if ($Readback.AuthorizationReceiptsReconcile -ne 0L) { [void]$errors.Add('M4_BEFORE_RECONCILE_AUTHORIZATION_PRESENT') }
    if ($attempts.Count -eq 1) {
        $raw = $attempts[0]
        if ($raw.AttemptIdentity -cne $script:M4RawPredecessor) { [void]$errors.Add('M4_BEFORE_RAW_PREDECESSOR_IDENTITY_MISMATCH') }
        if ($raw.Phase -cne $script:M4RawPhase) { [void]$errors.Add('M4_BEFORE_RAW_PHASE_MISMATCH') }
        if ($raw.Status -cne 'COMMITTED') { [void]$errors.Add('M4_BEFORE_RAW_NOT_COMMITTED') }
        if ($raw.ChapterKey -cne $script:M4ChapterKey) { [void]$errors.Add('M4_BEFORE_RAW_CHAPTER_MISMATCH') }
        if ($null -eq $raw.ReportByteLength -or $null -eq $raw.ReceiptByteLength -or
                $raw.ReportByteLength -le 0L -or $raw.ReceiptByteLength -le 0L) { [void]$errors.Add('M4_BEFORE_RAW_ARTIFACT_PAIR_INCOMPLETE') }
    }
    return $errors.ToArray()
}

function Get-M4Classification {
    param(
        [Parameter(Mandatory = $true)]$Readback,
        [Parameter(Mandatory = $true)][long]$LaunchCount
    )
    $reasons = [System.Collections.Generic.List[string]]::new()
    $attempts = @($Readback.Attempts)
    $recoveryReason = ''
    $reconcile = @($attempts | Where-Object { $_.Phase -ceq $script:M4Phase })
    $rawRows = @($attempts | Where-Object { $_.AttemptIdentity -ceq $script:M4RawPredecessor })
    $result = 'EVIDENCE_INCOMPLETE'
    if (@($Readback.IntegrityValues).Count -ne 1 -or @($Readback.IntegrityValues)[0] -cne 'ok') { [void]$reasons.Add('AFTER_INTEGRITY_CHECK_NOT_OK') }
    if ($Readback.Schema -notin $script:M4AcceptedSchemaVersions) { [void]$reasons.Add('AFTER_SCHEMA_VERSION_UNEXPECTED') }
    if ($LaunchCount -gt 1L) { [void]$reasons.Add('LAUNCH_COUNT_ABOVE_ONE') }
    if ($LaunchCount -lt 0L) { [void]$reasons.Add('LAUNCH_STATE_UNKNOWN') }
    if ($rawRows.Count -ne 1 -or $rawRows[0].Status -cne 'COMMITTED' -or $rawRows[0].Phase -cne $script:M4RawPhase) { [void]$reasons.Add('RAW_PREDECESSOR_ROW_CHANGED_OR_MISSING') }
    if ($attempts.Count -ne (1 + $reconcile.Count)) { [void]$reasons.Add('UNEXPECTED_ATTEMPT_ROWS_FOR_BINDING') }
    if ($reconcile.Count -gt 1) { [void]$reasons.Add('MULTIPLE_RECONCILE_ROWS') }
    if ($reconcile.Count -eq 0) {
        if ($LaunchCount -eq 0L) { [void]$reasons.Add('LAUNCH_NOT_PERFORMED') }
        if ($reasons.Count -eq 0 -or ($reasons.Count -eq 1 -and $reasons[0] -ceq 'LAUNCH_NOT_PERFORMED')) { $result = 'RECONCILE_NOT_DISPATCHED' }
    } elseif ($reconcile.Count -eq 1) {
        $row = $reconcile[0]
        if ($LaunchCount -eq 0L) { [void]$reasons.Add('ROW_PRESENT_WITHOUT_LAUNCH') }
        if ($row.PredecessorIdentity -cne $script:M4RawPredecessor) { [void]$reasons.Add('RECONCILE_ROW_PREDECESSOR_NOT_RAW') }
        if ($row.BindingIdentity -cne $script:M4Binding -or $row.ChapterKey -cne $script:M4ChapterKey) { [void]$reasons.Add('RECONCILE_ROW_BINDING_OR_CHAPTER_MISMATCH') }
        if ($row.Status -ceq 'COMMITTED') {
            if ($null -eq $row.ReportByteLength -or $null -eq $row.ReceiptByteLength -or
                    $row.ReportByteLength -le 0L -or $row.ReceiptByteLength -le 0L) { [void]$reasons.Add('RECONCILE_BLOB_PAIR_INCOMPLETE') }
            if ($row.ReportPhase -cne $script:M4Phase) { [void]$reasons.Add('RECONCILE_REPORT_PHASE_MISMATCH') }
            if ($row.ReportPredecessorIdentity -cne $script:M4RawPredecessor) { [void]$reasons.Add('RECONCILE_REPORT_PREDECESSOR_MISMATCH') }
            if ($reasons.Count -eq 0) { $result = 'RECONCILE_COMMITTED' }
        } elseif ($row.Status -ceq 'RECOVERY_REQUIRED') {
            $recoveryReason = [string]$row.RecoveryReasonCode
            if ($reasons.Count -eq 0) { $result = 'RECONCILE_TYPED_STOP' }
        } else {
            [void]$reasons.Add('RECONCILE_STATE_REMAINS_UNKNOWN_' + [string]$row.Status)
        }
    }
    return [pscustomobject]@{
        Classification = $result
        Reasons = $reasons.ToArray()
        RecoveryReasonCode = $recoveryReason
        ReconcileRowCount = [long]$reconcile.Count
    }
}

function Get-M4DatabaseReadback {
    # Device DB read-only export + host SQLite readback. All device access goes through library functions.
    param(
        [Parameter(Mandatory = $true)][string]$AdbExe,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$EventDirectory,
        [Parameter(Mandatory = $true)][ValidateSet('Before', 'After')][string]$Phase
    )
    $phaseLower = $Phase.ToLowerInvariant()
    $filesBefore = Get-P5EDeviceFileHashState -AdbPath $AdbExe -SerialValue $SerialValue
    $export = Invoke-P5EDatabaseSnapshotExport -AdbPath $AdbExe -SerialValue $SerialValue -DeviceFileState $filesBefore `
        -EvidenceDirectory $EventDirectory -CollectionPhase $Phase
    $immutable = (-not [bool]$filesBefore.wal.present -and -not [bool]$filesBefore.shm.present)
    $manifestBytes = [Text.UTF8Encoding]::new($false).GetBytes((ConvertTo-P5ECanonicalJson -Value $export))
    Write-P5EImmutableBytes -Path (Join-Path $EventDirectory ('database-export-manifest-' + $phaseLower + '.json')) -Bytes $manifestBytes -Name ('m4-export-manifest-' + $phaseLower)
    $query = New-M4ReadbackQuery
    $queryPath = Join-Path $EventDirectory ('m4-readback-' + $phaseLower + '.sql')
    Write-P5EImmutableBytes -Path $queryPath -Bytes ([Text.UTF8Encoding]::new($false).GetBytes($query)) -Name ('m4-query-' + $phaseLower)
    $databasePath = Join-Path ([string]$export.directory) 'tbl_android_txt.db'
    $bridge = Invoke-P5EHostSqliteBridge -DatabasePath $databasePath -QueryPath $queryPath -Immutable:$immutable
    $filesAfter = Get-P5EDeviceFileHashState -AdbPath $AdbExe -SerialValue $SerialValue
    [void](Assert-P5EDatabaseSnapshotStable -Before $filesBefore -After $filesAfter)
    $parsed = ConvertFrom-M4ReadbackOutput -Output ([string]$bridge.Stdout)
    return [pscustomobject]@{
        Phase = $Phase
        Readback = $parsed
        DatabaseDeviceSha256 = [string]$filesBefore.database.sha256
        WalPresent = [bool]$filesBefore.wal.present
        ShmPresent = [bool]$filesBefore.shm.present
        ExportedDatabaseSha256 = (Get-P5ESha256 -Path $databasePath)
        QuerySha256 = (Get-P5ESha256 -Path $queryPath)
        BridgeSha256 = [string]$bridge.BridgeSha256
        Immutable = $immutable
    }
}

# ----------------------------------------------------------------------------------------------
# Instrumentation arguments
# ----------------------------------------------------------------------------------------------
function New-M4InstrumentationPairs {
    param([Parameter(Mandatory = $true)]$P, [Parameter(Mandatory = $true)][long]$IssuedAtMillis, [Parameter(Mandatory = $true)][long]$ExpiresAtMillis)
    return [ordered]@{
        p5e_reconcile_live = 'YES'
        p5e_expected_production_apk_sha256 = ([string]$P['ExpectedProductionApkSha256']).ToLowerInvariant()
        p5e_expected_production_version = [string]$P['ExpectedProductionVersion']
        p5e_expected_production_version_code = [string][long]$P['ExpectedProductionVersionCode']
        p5e_expected_certificate_sha256 = ([string]$P['ExpectedCertificateSha256']).ToLowerInvariant()
        p5e_expected_db_sha256 = ([string]$P['ExpectedDbSha256']).ToLowerInvariant()
        p5e_expected_device_signature_token = ([string]$P['ExpectedDeviceSignatureToken']).ToLowerInvariant()
        p5e_test_apk_sha256 = ([string]$P['ExpectedTestApkSha256']).ToLowerInvariant()
        p5e_test_source_commit = ([string]$P['TestSourceCommit']).ToLowerInvariant()
        p5e_authorization_id = [string]$P['AuthorizationId']
        p5e_authorization_id_hash = (Get-M4Sha256OfText -Text ([string]$P['AuthorizationId']))
        p5e_owner_approval_manifest_sha256 = ([string]$P['OwnerApprovalManifestSha256']).ToLowerInvariant()
        p5e_expected_endpoint_account_fingerprint = ([string]$P['ExpectedEndpointAccountFingerprint']).ToLowerInvariant()
        p5e_expected_project_row_id = [string][long]$P['ExpectedProjectRowId']
        p5e_authorization_issued_at_ms = [string]$IssuedAtMillis
        p5e_authorization_expires_at_ms = [string]$ExpiresAtMillis
        p5e_expected_schema_version = [string][int]$P['ExpectedSchemaVersion']
        p5e_expected_phase = $script:M4Phase
        p5e_expected_provider = $script:P5EProvider
        p5e_expected_model = $script:P5EModel
        p5e_expected_upstream_provider = $script:P5EUpstreamProvider
        p5e_expected_route_fingerprint = $script:P5ERouteFingerprint
        p5e_expected_selector = $script:M4Selector
        p5e_expected_chapter_key = $script:M4ChapterKey
        p5e_expected_binding = $script:M4Binding
        p5e_expected_run = $script:M4Run
        p5e_expected_evaluation = $script:M4Evaluation
        p5e_expected_pack_hash = $script:M4PackHash
        p5e_expected_profile_hash = $script:M4ProfileHash
        p5e_expected_raw_predecessor_attempt_identity = $script:M4RawPredecessor
        p5e_maximum_primary_semantic_calls = '1'
        p5e_maximum_schema_repair_calls = '0'
        p5e_maximum_network_retries = '0'
        p5e_maximum_input_tokens = '100000'
        p5e_maximum_output_tokens = '4096'
        p5e_maximum_total_tokens = '104096'
        p5e_maximum_total_cost_usd = '0.05'
        p5e_maximum_execution_time_ms = [string]$script:P5EExecutionDeadlineMilliseconds
        p5e_evidence_redaction_policy = 'HASH_ONLY'
        p5e_cancellation_stop_authority = $script:M4StopAuthority
    }
}

function New-M4RemoteTokens {
    param([Parameter(Mandatory = $true)]$Pairs)
    $tokens = [System.Collections.Generic.List[string]]::new()
    foreach ($token in @('am', 'instrument', '-w', '-r', '-e', 'class', $script:M4ClassMethod)) { [void]$tokens.Add($token) }
    foreach ($pair in $Pairs.GetEnumerator()) {
        [void]$tokens.Add('-e'); [void]$tokens.Add([string]$pair.Key); [void]$tokens.Add([string]$pair.Value)
    }
    [void]$tokens.Add($script:M4Runner)
    return $tokens.ToArray()
}

function Get-M4LiveTestRequiredKeys {
    param([Parameter(Mandatory = $true)][string]$SourcePath)
    if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) { throw 'M4_LIVE_TEST_SOURCE_MISSING_STOP' }
    $source = Get-Content -Raw -LiteralPath $SourcePath
    $keys = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($pattern in @('required\(arguments,\s*"([^"]+)"', 'requireExact\(arguments,\s*"([^"]+)"',
            'requireExactSha256\(arguments,\s*"([^"]+)"', 'requiredInt\(arguments,\s*"([^"]+)"',
            'requiredLong\(arguments,\s*"([^"]+)"', 'requiredDecimal\(arguments,\s*"([^"]+)"',
            'LIVE_OPT_IN\s*=\s*"([^"]+)"')) {
        foreach ($match in [regex]::Matches($source, $pattern)) { [void]$keys.Add($match.Groups[1].Value) }
    }
    return @($keys | Sort-Object)
}

function Test-M4InstrumentationArguments {
    # Typed errors; empty means: argv round-trips through the POSIX parser byte-exactly, no shell operators,
    # no duplicate keys, and the key set equals the keys the live test file requires.
    param([Parameter(Mandatory = $true)][string[]]$AdbArguments, [Parameter(Mandatory = $true)][string[]]$RemoteTokens, [string]$LiveTestSourcePath = '')
    $errors = [System.Collections.Generic.List[string]]::new()
    $parsed = Get-P5ERemoteTokensFromAdbArguments -AdbArguments $AdbArguments
    if ($parsed.Operators.Count -ne 0) { [void]$errors.Add('REMOTE_SHELL_OPERATOR_PRESENT') }
    if ((@($parsed.Tokens) -join [char]0) -cne ($RemoteTokens -join [char]0)) { [void]$errors.Add('REMOTE_TOKENS_NOT_BYTE_EXACT') }
    $tokens = @($parsed.Tokens)
    if ($tokens.Count -lt 8 -or $tokens[0] -cne 'am' -or $tokens[1] -cne 'instrument' -or $tokens[2] -cne '-w' -or
            $tokens[3] -cne '-r' -or $tokens[$tokens.Count - 1] -cne $script:M4Runner -or $tokens[5] -cne 'class' -or
            $tokens[6] -cne $script:M4ClassMethod) { [void]$errors.Add('REMOTE_INSTRUMENT_SHAPE_INVALID') }
    $seen = [System.Collections.Generic.HashSet[string]]::new()
    $index = 7
    while ($index -lt $tokens.Count - 1) {
        if ($tokens[$index] -cne '-e' -or $index + 2 -ge $tokens.Count) { [void]$errors.Add('REMOTE_KEY_VALUE_SHAPE_INVALID'); break }
        if (-not $seen.Add($tokens[$index + 1])) { [void]$errors.Add('DUPLICATE_ARGUMENT:' + $tokens[$index + 1]) }
        $index += 3
    }
    if (-not [string]::IsNullOrWhiteSpace($LiveTestSourcePath)) {
        $required = @(Get-M4LiveTestRequiredKeys -SourcePath $LiveTestSourcePath)
        $sent = @($seen | Sort-Object)
        if (($required -join "`n") -cne ($sent -join "`n")) { [void]$errors.Add('LIVE_TEST_REQUIRED_KEY_SET_MISMATCH') }
    }
    return $errors.ToArray()
}

# ----------------------------------------------------------------------------------------------
# Live event
# ----------------------------------------------------------------------------------------------
function Invoke-M4ReconcileEvent {
    param([Parameter(Mandatory = $true)]$P)
    Assert-M4LiveParameters -P $P
    $serialValue = [string]$P['Serial']
    $toolchain = Resolve-P5ERawToolchain -AndroidSdkPath ([string]$P['AndroidSdkPath']) -LocalPropertiesPath ([string]$P['LocalPropertiesPath']) `
        -AdbPath ([string]$P['AdbPath']) -JavaPath ([string]$P['JavaPath']) -ApkSignerJarPath ([string]$P['ApkSignerJarPath']) `
        -BuildToolsVersion ([string]$P['BuildToolsVersion'])
    $adbExe = [string](Get-P5EProperty $toolchain 'adbPath')
    $javaExe = [string](Get-P5EProperty $toolchain 'javaPath')
    $signerJar = [string](Get-P5EProperty $toolchain 'apksignerJarPath')
    $liveTestSource = Join-Path (Get-M4RepoRoot) $script:M4LiveTestSourceRelativePath
    # The library compares installed certificates with this script-scope constant.
    $script:P5ECertificateSha256 = ([string]$P['ExpectedCertificateSha256']).ToLowerInvariant()
    $account = ([string]$P['ExpectedEndpointAccountFingerprint']).ToLowerInvariant()

    $eventDirectory = New-M4EventDirectory -Root ([string]$P['EvidenceRoot'])
    $outcome = [ordered]@{
        schemaVersion = $script:M4OutcomeSchema
        eventId = Split-Path -Leaf $eventDirectory
        phase = $script:M4Phase
        rawPredecessorAttemptIdentity = $script:M4RawPredecessor
        selector = $script:M4Selector; chapterKey = $script:M4ChapterKey
        bindingIdentity = $script:M4Binding; runDeclarationIdentity = $script:M4Run
        classMethod = $script:M4ClassMethod; runner = $script:M4Runner; serial = $serialValue
        authorizationIdSha256 = (Get-M4Sha256OfText -Text ([string]$P['AuthorizationId']))
        ownerApprovalManifestSha256 = ([string]$P['OwnerApprovalManifestSha256']).ToLowerInvariant()
        testSourceCommit = ([string]$P['TestSourceCommit']).ToLowerInvariant()
        expected = [ordered]@{
            productionApkSha256 = ([string]$P['ExpectedProductionApkSha256']).ToLowerInvariant()
            productionVersion = [string]$P['ExpectedProductionVersion']
            productionVersionCode = [long]$P['ExpectedProductionVersionCode']
            testApkSha256 = ([string]$P['ExpectedTestApkSha256']).ToLowerInvariant()
            certificateSha256 = ([string]$P['ExpectedCertificateSha256']).ToLowerInvariant()
            databaseSha256 = ([string]$P['ExpectedDbSha256']).ToLowerInvariant()
            projectRowId = [long]$P['ExpectedProjectRowId']
            schemaVersion = [int]$P['ExpectedSchemaVersion']
        }
        startedAtUtc = [DateTime]::UtcNow.ToString('o')
        launchCount = 0
        classification = 'EVIDENCE_INCOMPLETE'
        classificationReasons = @()
        stopCode = ''
        recoveryReasonCode = ''
        before = $null
        after = $null
        launch = $null
        packages = $null
        helperHashes = [ordered]@{
            script = (Get-P5ESha256 -Path $script:M4ScriptPath)
            supervisor = (Get-P5ESha256 -Path (Join-Path $script:M4ScriptRoot 'p5e-raw-live-supervisor.ps1'))
            databaseExporter = (Get-P5ESha256 -Path $script:P5EDatabaseExporterPath)
            sqliteBridge = (Get-P5ESha256 -Path $script:P5ESqliteBridgePath)
        }
        readOnlyCommandCount = 0
        retryCount = 0
    }
    $beforePassed = $false
    $run = $null
    try {
        # 1) Before: package/hash/cert compare, DB export + readback. No launch on any mismatch.
        try {
            Initialize-P5ECollectorCommandLog -EvidenceDirectory $eventDirectory -CollectionPhase Before
            $production = Get-P5EPackageReadback -AdbPath $adbExe -SerialValue $serialValue -EvidenceDirectory $eventDirectory `
                -PackageName $script:P5ETargetPackage -ExpectedApkSha256 ([string]$outcome.expected.productionApkSha256) `
                -LocalFileName 'installed-production-before.apk' -Name 'production-before' -ApkSignerJavaPath $javaExe `
                -ApkSignerJarPath $signerJar -ExpectedVersionCode ([int]$P['ExpectedProductionVersionCode']) -ExpectedVersion ([string]$P['ExpectedProductionVersion'])
            $testPackage = Get-P5EPackageReadback -AdbPath $adbExe -SerialValue $serialValue -EvidenceDirectory $eventDirectory `
                -PackageName $script:P5ETestPackage -ExpectedApkSha256 ([string]$outcome.expected.testApkSha256) `
                -LocalFileName 'installed-test-before.apk' -Name 'test-before' -ApkSignerJavaPath $javaExe -ApkSignerJarPath $signerJar
            $outcome.packages = [ordered]@{ productionBefore = $production; testBefore = $testPackage }
            $beforeDb = Get-M4DatabaseReadback -AdbExe $adbExe -SerialValue $serialValue -EventDirectory $eventDirectory -Phase Before
            $outcome.before = [ordered]@{
                databaseDeviceSha256 = $beforeDb.DatabaseDeviceSha256; exportedDatabaseSha256 = $beforeDb.ExportedDatabaseSha256
                walPresent = $beforeDb.WalPresent; shmPresent = $beforeDb.ShmPresent; querySha256 = $beforeDb.QuerySha256
                bridgeSha256 = $beforeDb.BridgeSha256; schemaVersion = $beforeDb.Readback.Schema
                attemptRows = @($beforeDb.Readback.Attempts | ForEach-Object { [ordered]@{ attemptIdentity = $_.AttemptIdentity; phase = $_.Phase; status = $_.Status; predecessorIdentity = $_.PredecessorIdentity } })
                attemptsTotal = $beforeDb.Readback.AttemptsTotal; authorizationReceiptsTotal = $beforeDb.Readback.AuthorizationReceiptsTotal
                lifecycleTotal = $beforeDb.Readback.LifecycleTotal; reconciliationTotal = $beforeDb.Readback.ReconciliationTotal
                reconciliationHistoryTotal = $beforeDb.Readback.ReconciliationHistoryTotal
            }
            if ($beforeDb.DatabaseDeviceSha256 -cne [string]$outcome.expected.databaseSha256 -or
                    $beforeDb.ExportedDatabaseSha256 -cne [string]$outcome.expected.databaseSha256) { throw 'M4_BEFORE_DATABASE_HASH_MISMATCH_STOP' }
            $beforeErrors = @(Test-M4BeforeState -Readback $beforeDb.Readback)
            if ($beforeErrors.Count -gt 0) { throw ('M4_BEFORE_STATE_STOP:' + ($beforeErrors -join ',')) }
            $beforePassed = $true
        } catch {
            $outcome.classification = 'BEFORE_STOP_NO_LAUNCH'
            $outcome.stopCode = [string]$_.Exception.Message
            $outcome.classificationReasons = @($outcome.stopCode)
        }

        # 2) Exactly one launch, never retried.
        if ($beforePassed) {
            $issued = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - $script:P5EAuthorizationClockSkewMarginMilliseconds
            $expires = $issued + $script:P5EAuthorizationValidityMilliseconds
            $pairs = New-M4InstrumentationPairs -P $P -IssuedAtMillis $issued -ExpiresAtMillis $expires
            $remoteTokens = New-M4RemoteTokens -Pairs $pairs
            $adbArguments = New-P5EAdbArgumentList -Serial $serialValue -RemoteCommandTokens $remoteTokens
            $argumentErrors = @(Test-M4InstrumentationArguments -AdbArguments $adbArguments -RemoteTokens $remoteTokens -LiveTestSourcePath $liveTestSource)
            if ($argumentErrors.Count -gt 0) {
                $outcome.classification = 'BEFORE_STOP_NO_LAUNCH'
                $outcome.stopCode = 'M4_ARGUMENT_CONTRACT_STOP:' + ($argumentErrors -join ',')
                $outcome.classificationReasons = @($outcome.stopCode)
            } else {
                $previousClass = $script:P5EClass
                $script:P5EClass = $script:M4ClassName
                try {
                    $run = Invoke-P5EProcessSupervisor -FilePath $adbExe -ArgumentList $adbArguments `
                        -TimeoutMilliseconds $script:P5EHostObservationTimeoutMilliseconds -EvidenceDirectory $eventDirectory `
                        -SensitiveValues @($account) -ClearInheritedEnvironmentVariableNames @($script:P5EAccountEnvironmentName)
                } catch {
                    $run = $null
                    $outcome.launch = [ordered]@{ supervisorException = [string]$_.Exception.GetType().Name }
                } finally { $script:P5EClass = $previousClass }
                if ($null -ne $run) {
                    $outcome.launchCount = [long]$run.LaunchCount
                    $outcome.launch = [ordered]@{
                        outcome = [string]$run.Outcome; exitCode = $run.ExitCode; timedOut = [bool]$run.TimedOut
                        redactionViolation = [bool]$run.RedactionViolation; captureBounded = [bool]$run.CaptureBounded
                        containmentVerified = [bool]$run.ContainmentVerified
                        authorizationIssuedAtMillis = $issued; authorizationExpiresAtMillis = $expires
                        argumentKeys = @($pairs.Keys)
                        stdoutSha256 = (Get-P5ESha256 -Path $run.StdoutPath); stderrSha256 = (Get-P5ESha256 -Path $run.StderrPath)
                    }
                } else { $outcome.launchCount = -1 }
            }
        }

        # 3) After: readback and classification (only when a launch was attempted).
        if ($beforePassed -and $outcome.classification -ne 'BEFORE_STOP_NO_LAUNCH') {
            try {
                Initialize-P5ECollectorCommandLog -EvidenceDirectory $eventDirectory -CollectionPhase After
                try {
                    $productionAfter = Get-P5EPackageReadback -AdbPath $adbExe -SerialValue $serialValue -EvidenceDirectory $eventDirectory `
                        -PackageName $script:P5ETargetPackage -ExpectedApkSha256 ([string]$outcome.expected.productionApkSha256) `
                        -LocalFileName 'installed-production-after.apk' -Name 'production-after' -ApkSignerJavaPath $javaExe `
                        -ApkSignerJarPath $signerJar -ExpectedVersionCode ([int]$P['ExpectedProductionVersionCode']) -ExpectedVersion ([string]$P['ExpectedProductionVersion'])
                    $testAfter = Get-P5EPackageReadback -AdbPath $adbExe -SerialValue $serialValue -EvidenceDirectory $eventDirectory `
                        -PackageName $script:P5ETestPackage -ExpectedApkSha256 ([string]$outcome.expected.testApkSha256) `
                        -LocalFileName 'installed-test-after.apk' -Name 'test-after' -ApkSignerJavaPath $javaExe -ApkSignerJarPath $signerJar
                    $outcome.packages['productionAfter'] = $productionAfter
                    $outcome.packages['testAfter'] = $testAfter
                } catch { $outcome.packages['afterReadbackError'] = [string]$_.Exception.Message }
                $afterDb = Get-M4DatabaseReadback -AdbExe $adbExe -SerialValue $serialValue -EventDirectory $eventDirectory -Phase After
                $classified = Get-M4Classification -Readback $afterDb.Readback -LaunchCount ([long]$outcome.launchCount)
                $outcome.classification = $classified.Classification
                $outcome.classificationReasons = @($classified.Reasons)
                $outcome.recoveryReasonCode = $classified.RecoveryReasonCode
                $outcome.after = [ordered]@{
                    databaseDeviceSha256 = $afterDb.DatabaseDeviceSha256; exportedDatabaseSha256 = $afterDb.ExportedDatabaseSha256
                    walPresent = $afterDb.WalPresent; shmPresent = $afterDb.ShmPresent; querySha256 = $afterDb.QuerySha256
                    bridgeSha256 = $afterDb.BridgeSha256; schemaVersion = $afterDb.Readback.Schema
                    attemptRows = @($afterDb.Readback.Attempts | ForEach-Object { [ordered]@{
                        attemptIdentity = $_.AttemptIdentity; phase = $_.Phase; status = $_.Status; predecessorIdentity = $_.PredecessorIdentity
                        recoveryReasonCode = $_.RecoveryReasonCode; reportByteLength = $_.ReportByteLength; receiptByteLength = $_.ReceiptByteLength
                        reportPhase = $_.ReportPhase; reportPredecessorIdentity = $_.ReportPredecessorIdentity } })
                    reconcileRowCount = $classified.ReconcileRowCount
                    attemptsTotal = $afterDb.Readback.AttemptsTotal; authorizationReceiptsTotal = $afterDb.Readback.AuthorizationReceiptsTotal
                    authorizationReceiptsReconcile = $afterDb.Readback.AuthorizationReceiptsReconcile
                    lifecycleTotal = $afterDb.Readback.LifecycleTotal; reconciliationTotal = $afterDb.Readback.ReconciliationTotal
                    reconciliationHistoryTotal = $afterDb.Readback.ReconciliationHistoryTotal
                }
            } catch {
                $outcome.classification = 'EVIDENCE_INCOMPLETE'
                $outcome.stopCode = [string]$_.Exception.Message
                $outcome.classificationReasons = @('AFTER_READBACK_FAILED:' + $outcome.stopCode)
            }
        }
    } catch {
        $outcome.classification = 'EVIDENCE_INCOMPLETE'
        $outcome.stopCode = [string]$_.Exception.Message
    } finally {
        $outcome.readOnlyCommandCount = [long]$script:P5EReadOnlyCommandCount
        $outcome.finishedAtUtc = [DateTime]::UtcNow.ToString('o')
        $outcome.launchCountMustBeOne = ([long]$outcome.launchCount -eq 1L)
        Write-M4Json -Path (Join-Path $eventDirectory 'M4_OUTCOME.json') -Value $outcome
    }
    $manifest = Write-M4Manifest -EventDirectory $eventDirectory
    return [pscustomobject]@{
        EventDirectory = $eventDirectory
        Classification = [string]$outcome.classification
        LaunchCount = [long]$outcome.launchCount
        StopCode = [string]$outcome.stopCode
        RecoveryReasonCode = [string]$outcome.recoveryReasonCode
        ManifestPath = $manifest.Path
        ManifestSha256 = $manifest.Sha256
        ManifestFileCount = $manifest.FileCount
    }
}

# ----------------------------------------------------------------------------------------------
# Self-test (no adb, no device, no provider)
# ----------------------------------------------------------------------------------------------
function Invoke-M4SelfTest {
    $script:M4Passed = 0
    $script:M4Total = 0
    function Assert-M4 {
        param([Parameter(Mandatory = $true)][bool]$Condition, [Parameter(Mandatory = $true)][string]$Name)
        $script:M4Total++
        if (-not $Condition) { Write-Output ('SELFTEST FAIL ' + $Name); throw ('SELFTEST_ASSERTION_FAILED:' + $Name) }
        $script:M4Passed++
    }

    # Any device/adb/process path must be unreachable. These stubs shadow the library for the whole self-test.
    $script:M4ForbiddenCalls = 0
    function Invoke-P5EAdbReadOnly { $script:M4ForbiddenCalls++; throw 'SELFTEST_ADB_FORBIDDEN' }
    function Invoke-P5EAdbShellReadOnly { $script:M4ForbiddenCalls++; throw 'SELFTEST_ADB_FORBIDDEN' }
    function Invoke-P5EAdbPresenceReadOnly { $script:M4ForbiddenCalls++; throw 'SELFTEST_ADB_FORBIDDEN' }
    function Invoke-P5EDatabaseBinaryExportProcess { $script:M4ForbiddenCalls++; throw 'SELFTEST_ADB_FORBIDDEN' }

    $python = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($null -eq $python) { $python = Get-Command py -ErrorAction SilentlyContinue }
    if ($null -eq $python) {
        Write-Output 'SELFTEST HOST_SQLITE_UNAVAILABLE (py launcher / python sqlite3 not found)'
        return 3
    }
    $tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-m4-selftest-' + [Guid]::NewGuid().ToString('N'))
    [void](New-Item -ItemType Directory -Path $tempRoot)
    try {
        $fixtureScript = Join-Path $tempRoot 'make_fixture.py'
        $fixtureCode = @'
import sqlite3, sys
path, scenario = sys.argv[1], sys.argv[2]
RAW = '7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e'
BIND = '845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf'
RUN = '8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc'
RECON = '9' * 64
OTHER = 'd' * 64
GOOD = ('{"phase":"L1_RECONCILE","predecessorIdentity":"%s"}' % RAW).encode()
BADPRED = ('{"phase":"L1_RECONCILE","predecessorIdentity":"%s"}' % OTHER).encode()
BADPHASE = ('{"phase":"L1_RAW_DISCOVERY","predecessorIdentity":"%s"}' % RAW).encode()
con = sqlite3.connect(path)
con.executescript("""
CREATE TABLE editorial_p5c_attempts(attempt_identity TEXT PRIMARY KEY, request_identity TEXT, binding_identity TEXT,
 run_declaration_identity TEXT, chapter_key TEXT, phase TEXT, predecessor_identity TEXT, request_envelope_hash TEXT,
 provider TEXT, model TEXT, status TEXT, response_identity TEXT DEFAULT '', report_bytes BLOB, receipt_bytes BLOB,
 metrics_json TEXT DEFAULT '', recovery_reason_code TEXT DEFAULT '', created_at INTEGER, updated_at INTEGER);
CREATE TABLE editorial_p5d_authorization_receipts(authorization_id_hash TEXT PRIMARY KEY, exact_phase TEXT, attempt_identity TEXT);
CREATE TABLE editorial_p5d_network_lifecycle(attempt_identity TEXT PRIMARY KEY);
CREATE TABLE editorial_p5d_reconciliation(attempt_identity TEXT PRIMARY KEY);
CREATE TABLE editorial_p5d_reconciliation_history(decision_identity TEXT PRIMARY KEY, attempt_identity TEXT);
""")
def attempt(ident, phase, status, pred, report, receipt, reason='', created=1, chapter='001'):
    con.execute("INSERT INTO editorial_p5c_attempts(attempt_identity,request_identity,binding_identity,run_declaration_identity,"
                "chapter_key,phase,predecessor_identity,request_envelope_hash,provider,model,status,report_bytes,receipt_bytes,"
                "recovery_reason_code,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                (ident, 'c' * 64, BIND, RUN, chapter, phase, pred, 'e' * 64, 'openrouter', 'm', status, report, receipt, reason, created, created))
def raw(status='COMMITTED', blobs=True):
    attempt(RAW, 'L1_RAW_DISCOVERY', status, OTHER, b'\x01\x02\x03' if blobs else None, b'\x04\x05' if blobs else None)
    con.execute("INSERT INTO editorial_p5d_authorization_receipts VALUES(?,?,?)", ('f' * 64, 'L1_RAW_DISCOVERY', RAW))
    con.execute("INSERT INTO editorial_p5d_network_lifecycle VALUES(?)", (RAW,))
version = 24
if scenario == 'event7':
    raw()
elif scenario == 'raw_recovery':
    raw('RECOVERY_REQUIRED', False)
elif scenario == 'committed':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', RAW, GOOD, b'\x09', created=2)
elif scenario == 'typed_stop':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'RECOVERY_REQUIRED', RAW, None, None, reason='P5E_RECONCILE_PROVIDER_STOP', created=2)
elif scenario == 'claimed':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'CLAIMED', RAW, None, None, created=2)
elif scenario == 'wrong_predecessor':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', OTHER, GOOD, b'\x09', created=2)
elif scenario == 'bad_report_predecessor':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', RAW, BADPRED, b'\x09', created=2)
elif scenario == 'bad_report_phase':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', RAW, BADPHASE, b'\x09', created=2)
elif scenario == 'not_json':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', RAW, b'not json', b'\x09', created=2)
elif scenario == 'missing_blob':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', RAW, GOOD, None, created=2)
elif scenario == 'two_reconcile':
    version = 25; raw(); attempt(RECON, 'L1_RECONCILE', 'COMMITTED', RAW, GOOD, b'\x09', created=2); attempt('8' * 64, 'L1_RECONCILE', 'RECOVERY_REQUIRED', RAW, None, None, created=3)
elif scenario == 'raw_missing':
    version = 25
else:
    raise SystemExit('unknown scenario')
con.execute("PRAGMA user_version=%d" % version)
con.commit()
con.close()
'@
        Write-P5EUtf8NoBom -Path $fixtureScript -Text $fixtureCode
        $pythonExe = [string]$python.Source

        function New-M4Fixture {
            param([Parameter(Mandatory = $true)][string]$Scenario)
            $path = Join-Path $tempRoot ('fixture-' + $Scenario + '-' + [Guid]::NewGuid().ToString('N') + '.db')
            $ErrorActionPreference = 'Continue'
            $output = @(& $pythonExe -3 $fixtureScript $path $Scenario 2>&1 | ForEach-Object { [string]$_ })
            if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $path -PathType Leaf)) { throw ('SELFTEST_FIXTURE_BUILD_FAILED:' + $Scenario + ':' + ($output -join ' ')) }
            return $path
        }
        function Read-M4FixtureReadback {
            param([Parameter(Mandatory = $true)][string]$DatabasePath)
            $queryPath = Join-Path $tempRoot ('q-' + [Guid]::NewGuid().ToString('N') + '.sql')
            Write-P5EUtf8NoBom -Path $queryPath -Text (New-M4ReadbackQuery)
            $bridge = Invoke-P5EHostSqliteBridge -DatabasePath $DatabasePath -QueryPath $queryPath -Immutable
            return (ConvertFrom-M4ReadbackOutput -Output ([string]$bridge.Stdout))
        }

        # --- constants vs the Java source -----------------------------------------------------------
        $javaPath = Join-Path (Get-M4RepoRoot) 'app\src\main\java\com\ml\tblandroidtxt\EditorialP5EFreshRawLiveRunner.java'
        $java = Get-Content -Raw -LiteralPath $javaPath
        foreach ($pair in @(
                @('SELECTOR', $script:M4Selector), @('CHAPTER_KEY', $script:M4ChapterKey), @('BINDING_IDENTITY', $script:M4Binding),
                @('RUN_DECLARATION_IDENTITY', $script:M4Run), @('COMPATIBILITY_EVALUATION_ID', $script:M4Evaluation),
                @('CANONICAL_PACK_HASH', $script:M4PackHash), @('CANONICAL_PROFILE_HASH', $script:M4ProfileHash),
                @('RAW_PREDECESSOR_ATTEMPT_IDENTITY', $script:M4RawPredecessor))) {
            $m = [regex]::Match($java, 'public static final String ' + $pair[0] + '\s*=\s*"([^"]+)"')
            Assert-M4 ($m.Success -and $m.Groups[1].Value -ceq $pair[1]) ('java-constant-' + $pair[0])
        }
        Assert-M4 ($script:M4Binding -ceq $script:P5EBindingIdentity -and $script:M4Run -ceq $script:P5ERunDeclarationIdentity -and
            $script:M4Evaluation -ceq $script:P5EEvaluationId -and $script:M4PackHash -ceq $script:P5EPackHash -and
            $script:M4ProfileHash -ceq $script:P5EProfileHash -and $script:M4RawPredecessor -ceq $script:P5EAttemptIdentity) 'library-constants-agree'

        # --- fixtures through the host SQLite bridge ----------------------------------------------
        $fx = @{}
        foreach ($scenario in @('event7', 'raw_recovery', 'committed', 'typed_stop', 'claimed', 'wrong_predecessor',
                'bad_report_predecessor', 'bad_report_phase', 'not_json', 'missing_blob', 'two_reconcile', 'raw_missing')) {
            $fx[$scenario] = Read-M4FixtureReadback -DatabasePath (New-M4Fixture -Scenario $scenario)
        }
        Assert-M4 ($fx['event7'].Schema -eq 24L -and @($fx['event7'].IntegrityValues)[0] -ceq 'ok') 'fixture-readback-event7-integrity-ok'

        # before-state
        Assert-M4 (@(Test-M4BeforeState -Readback $fx['event7']).Count -eq 0) 'before-event7-ready'
        Assert-M4 (@(Test-M4BeforeState -Readback $fx['committed']) -contains 'M4_BEFORE_ATTEMPT_ROW_COUNT_NOT_ONE') 'before-already-reconciled-refused-count'
        Assert-M4 (@(Test-M4BeforeState -Readback $fx['committed']) -contains 'M4_BEFORE_RECONCILE_ROW_PRESENT') 'before-already-reconciled-refused-row'
        Assert-M4 (@(Test-M4BeforeState -Readback $fx['raw_recovery']) -contains 'M4_BEFORE_RAW_NOT_COMMITTED') 'before-raw-recovery-refused'
        Assert-M4 (@(Test-M4BeforeState -Readback $fx['raw_missing']) -contains 'M4_BEFORE_ATTEMPT_ROW_COUNT_NOT_ONE') 'before-no-raw-refused'

        # classification
        $cases = @(
            @('event7', 1, 'RECONCILE_NOT_DISPATCHED'), @('committed', 1, 'RECONCILE_COMMITTED'),
            @('typed_stop', 1, 'RECONCILE_TYPED_STOP'), @('claimed', 1, 'EVIDENCE_INCOMPLETE'),
            @('wrong_predecessor', 1, 'EVIDENCE_INCOMPLETE'), @('bad_report_predecessor', 1, 'EVIDENCE_INCOMPLETE'),
            @('bad_report_phase', 1, 'EVIDENCE_INCOMPLETE'), @('not_json', 1, 'EVIDENCE_INCOMPLETE'),
            @('missing_blob', 1, 'EVIDENCE_INCOMPLETE'), @('two_reconcile', 1, 'EVIDENCE_INCOMPLETE'),
            @('raw_missing', 1, 'EVIDENCE_INCOMPLETE'), @('committed', 2, 'EVIDENCE_INCOMPLETE'),
            @('committed', 0, 'EVIDENCE_INCOMPLETE'), @('committed', -1, 'EVIDENCE_INCOMPLETE'),
            @('event7', 0, 'RECONCILE_NOT_DISPATCHED'), @('event7', -1, 'EVIDENCE_INCOMPLETE'))
        foreach ($case in $cases) {
            $verdict = Get-M4Classification -Readback $fx[$case[0]] -LaunchCount ([long]$case[1])
            Assert-M4 ($verdict.Classification -ceq $case[2]) ('classify-' + $case[0] + '-launch' + $case[1])
        }
        Assert-M4 ((Get-M4Classification -Readback $fx['typed_stop'] -LaunchCount 1).RecoveryReasonCode -ceq 'P5E_RECONCILE_PROVIDER_STOP') 'typed-stop-records-recovery-reason'
        Assert-M4 ((Get-M4Classification -Readback $fx['wrong_predecessor'] -LaunchCount 1).Reasons -contains 'RECONCILE_ROW_PREDECESSOR_NOT_RAW') 'wrong-predecessor-reason'
        $badIntegrity = $fx['committed'].PSObject.Copy()
        $badIntegrity.IntegrityValues = @('*** in database main ***')
        Assert-M4 ((Get-M4Classification -Readback $badIntegrity -LaunchCount 1).Classification -ceq 'EVIDENCE_INCOMPLETE') 'classify-integrity-not-ok'

        # --- instrumentation arguments --------------------------------------------------------------
        $P = @{
            ExpectedProductionApkSha256 = 'a' * 64; ExpectedProductionVersion = '4.18-p5e.4'; ExpectedProductionVersionCode = 212L
            ExpectedTestApkSha256 = 'b' * 64; ExpectedCertificateSha256 = $script:P5ECertificateSha256; ExpectedDbSha256 = 'c' * 64
            ExpectedEndpointAccountFingerprint = 'f1' * 32; ExpectedProjectRowId = 2L; AuthorizationId = 'P5E-M4-RECONCILE-SELFTEST'
            OwnerApprovalManifestSha256 = '1' * 64; TestSourceCommit = '0123456789abcdef0123456789abcdef01234567'
            ExpectedSchemaVersion = 25; ExpectedDeviceSignatureToken = 'abebea4b'
        }
        $pairs = New-M4InstrumentationPairs -P $P -IssuedAtMillis 1000L -ExpiresAtMillis 181000L
        $tokens = New-M4RemoteTokens -Pairs $pairs
        $adbArgs = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens $tokens
        $liveTest = Join-Path (Get-M4RepoRoot) $script:M4LiveTestSourceRelativePath
        Assert-M4 (Test-Path -LiteralPath $liveTest -PathType Leaf) 'live-test-source-present'
        $argErrors = @(Test-M4InstrumentationArguments -AdbArguments $adbArgs -RemoteTokens $tokens -LiveTestSourcePath $liveTest)
        Assert-M4 ($argErrors.Count -eq 0) ('instrument-args-contract:' + ($argErrors -join ','))
        $parsedRemote = Get-P5ERemoteTokensFromAdbArguments -AdbArguments $adbArgs
        $stopIndex = [array]::IndexOf($parsedRemote.Tokens, 'p5e_cancellation_stop_authority')
        Assert-M4 ($parsedRemote.Operators.Count -eq 0 -and $stopIndex -ge 0 -and $parsedRemote.Tokens[$stopIndex + 1] -ceq $script:M4StopAuthority) 'stop-authority-pipes-quoted-byte-exact'
        Assert-M4 ($tokens[0] -ceq 'am' -and $tokens[1] -ceq 'instrument' -and $tokens[2] -ceq '-w' -and $tokens[3] -ceq '-r' -and
            $tokens[$tokens.Count - 1] -ceq $script:M4Runner -and $tokens[6] -ceq $script:M4ClassMethod) 'am-instrument-shape'
        Assert-M4 ($pairs['p5e_authorization_id_hash'] -ceq (Get-M4Sha256OfText -Text 'P5E-M4-RECONCILE-SELFTEST')) 'authorization-hash-matches-id'
        $missingKeyPairs = [ordered]@{}
        foreach ($entry in $pairs.GetEnumerator()) { if ($entry.Key -cne 'p5e_expected_db_sha256') { $missingKeyPairs[$entry.Key] = $entry.Value } }
        $missingTokens = New-M4RemoteTokens -Pairs $missingKeyPairs
        $missingErrors = @(Test-M4InstrumentationArguments -AdbArguments (New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens $missingTokens) -RemoteTokens $missingTokens -LiveTestSourcePath $liveTest)
        Assert-M4 ($missingErrors -contains 'LIVE_TEST_REQUIRED_KEY_SET_MISMATCH') 'instrument-args-missing-key-detected'

        # --- evidence root rules -----------------------------------------------------------------------
        $relativeRefused = $false
        try { New-M4EventDirectory -Root 'relative\path' | Out-Null } catch { $relativeRefused = ([string]$_.Exception.Message -ceq 'M4_EVIDENCE_ROOT_NOT_ABSOLUTE_STOP') }
        Assert-M4 $relativeRefused 'evidence-root-relative-refused'
        $d1 = New-M4EventDirectory -Root $tempRoot
        $d2 = New-M4EventDirectory -Root $tempRoot
        Assert-M4 ($d1 -cne $d2 -and (Split-Path -Leaf $d1) -match '^reconcile-m4-\d{8}T\d{6}Z-[0-9a-f]{32}$') 'event-directory-unique-name'

        # --- end-to-end orchestration with stubbed device functions -----------------------------------
        $script:M4Stub = @{ Current = ''; After = ''; Launches = 0; LastArgs = @(); PackageFailure = $false; Fingerprint = ('f1' * 32) }
        function Resolve-P5ERawToolchain { param([string]$AndroidSdkPath, [string]$LocalPropertiesPath, [string]$AdbPath, [string]$JavaPath, [string]$ApkSignerJarPath, [string]$BuildToolsVersion)
            return [pscustomobject]@{ adbPath = 'SELFTEST-adb.exe'; javaPath = 'SELFTEST-java.exe'; apksignerJarPath = 'SELFTEST-apksigner.jar' } }
        function Get-P5EPackageReadback { param($AdbPath, $SerialValue, $EvidenceDirectory, $PackageName, $ExpectedApkSha256, $LocalFileName, $Name, $ApkSignerJavaPath, $ApkSignerJarPath, $ExpectedVersionCode = -1, $ExpectedVersion = '')
            if ($script:M4Stub.PackageFailure) { throw ('P5E_COLLECTOR_PACKAGE_APK_HASH_MISMATCH:' + $Name) }
            return [ordered]@{ package = $PackageName; apkSha256 = $ExpectedApkSha256; certificateSha256 = $script:P5ECertificateSha256; version = $ExpectedVersion; versionCode = $ExpectedVersionCode } }
        function Get-P5EDeviceFileHashState { param($AdbPath, $SerialValue)
            return [ordered]@{
                database = [ordered]@{ path = 'db'; status = 'PRESENT'; present = $true; sha256 = (Get-P5ESha256 -Path $script:M4Stub.Current) }
                wal = [ordered]@{ path = 'wal'; status = 'ABSENT'; present = $false; sha256 = '' }
                shm = [ordered]@{ path = 'shm'; status = 'ABSENT'; present = $false; sha256 = '' }
                settings = [ordered]@{ path = 'settings'; status = 'PRESENT'; present = $true; sha256 = 'selftest-settings' } } }
        function Invoke-P5EDatabaseSnapshotExport { param($AdbPath, $SerialValue, $DeviceFileState, $EvidenceDirectory, $CollectionPhase, $TimeoutMilliseconds)
            $dir = Assert-P5EDatabaseExportDestination -EvidenceDirectory $EvidenceDirectory -CollectionPhase $CollectionPhase
            Copy-Item -LiteralPath $script:M4Stub.Current -Destination (Join-Path $dir 'tbl_android_txt.db')
            return [ordered]@{ contractVersion = 'selftest'; collectionPhase = $CollectionPhase; directory = $dir; files = @() } }
        function Invoke-P5EProcessSupervisor { param($FilePath, $ArgumentList, $TimeoutMilliseconds, $EvidenceDirectory, $SensitiveValues, $ClearInheritedEnvironmentVariableNames)
            $script:M4Stub.Launches++
            $script:M4Stub.LastArgs = @($ArgumentList)
            $script:M4Stub.Current = $script:M4Stub.After
            $out = Join-Path $EvidenceDirectory 'instrumentation-stdout.txt'; $err = Join-Path $EvidenceDirectory 'instrumentation-stderr.txt'
            Write-P5EUtf8NoBom -Path $out -Text 'INSTRUMENTATION_CODE:-1'; Write-P5EUtf8NoBom -Path $err -Text 'P5E_SAFE_STDERR_EMPTY'
            return [pscustomobject]@{ Outcome = 'PROCESS_EXITED_ZERO'; LaunchCount = 1; DispatchCount = 1; TimedOut = $false; ExitCode = 0
                RedactionViolation = $false; CaptureBounded = $true; ContainmentVerified = $true; StdoutPath = $out; StderrPath = $err } }

        $fakeRoot = Join-Path $tempRoot 'evidence'
        [void](New-Item -ItemType Directory -Path $fakeRoot)
        $eventDb = New-M4Fixture -Scenario 'event7'
        $expectedDbHash = Get-P5ESha256 -Path $eventDb
        $live = @{
            Serial = $script:P5ESerial; EvidenceRoot = $fakeRoot; ExpectedProductionApkSha256 = 'a' * 64
            ExpectedProductionVersion = '4.18-p5e.4'; ExpectedProductionVersionCode = 212L; ExpectedTestApkSha256 = 'b' * 64
            ExpectedCertificateSha256 = $script:P5ECertificateSha256; ExpectedDbSha256 = $expectedDbHash
            ExpectedEndpointAccountFingerprint = $script:M4Stub.Fingerprint; ExpectedProjectRowId = 2L
            AuthorizationId = 'P5E-M4-RECONCILE-SELFTEST'; OwnerApprovalManifestSha256 = '1' * 64
            TestSourceCommit = '0123456789abcdef0123456789abcdef01234567'; ExpectedSchemaVersion = 25
            ExpectedDeviceSignatureToken = 'abebea4b'; AndroidSdkPath = ''; LocalPropertiesPath = ''; AdbPath = ''
            JavaPath = ''; ApkSignerJarPath = ''; BuildToolsVersion = '35.0.0'
        }
        function Invoke-M4Flow {
            param([string]$AfterScenario, [hashtable]$Overrides = @{})
            $script:M4Stub.Launches = 0
            $script:M4Stub.Current = (New-M4Fixture -Scenario 'event7')
            $script:M4Stub.After = (New-M4Fixture -Scenario $AfterScenario)
            $params = $live.Clone()
            foreach ($key in $Overrides.Keys) { $params[$key] = $Overrides[$key] }
            return Invoke-M4ReconcileEvent -P $params
        }
        # fixture file hashes depend only on content, but sqlite headers carry a change counter and are deterministic here
        $live['ExpectedDbSha256'] = Get-P5ESha256 -Path (New-M4Fixture -Scenario 'event7')

        foreach ($flow in @(
                @('committed', 'RECONCILE_COMMITTED'), @('typed_stop', 'RECONCILE_TYPED_STOP'),
                @('event7', 'RECONCILE_NOT_DISPATCHED'), @('wrong_predecessor', 'EVIDENCE_INCOMPLETE'))) {
            $result = Invoke-M4Flow -AfterScenario $flow[0]
            Assert-M4 ($result.Classification -ceq $flow[1]) ('flow-' + $flow[0] + '-classification')
            Assert-M4 ($result.LaunchCount -eq 1 -and $script:M4Stub.Launches -eq 1) ('flow-' + $flow[0] + '-exactly-one-launch')
            $outcomeFile = Join-Path $result.EventDirectory 'M4_OUTCOME.json'
            $outcomeJson = Get-Content -Raw -LiteralPath $outcomeFile | ConvertFrom-Json
            Assert-M4 ($outcomeJson.schemaVersion -ceq 'p5e.m4.reconcile-outcome.v1' -and $outcomeJson.launchCount -eq 1 -and $outcomeJson.retryCount -eq 0) ('flow-' + $flow[0] + '-outcome-file')
            Assert-M4 ((Test-Path -LiteralPath (Join-Path $result.EventDirectory 'M4_MANIFEST.sha256') -PathType Leaf) -and $result.ManifestFileCount -ge 5) ('flow-' + $flow[0] + '-manifest')
            $manifestText = Get-Content -Raw -LiteralPath $result.ManifestPath
            Assert-M4 ($manifestText -match [regex]::Escape('M4_OUTCOME.json')) ('flow-' + $flow[0] + '-manifest-lists-outcome')
            $leak = @(Get-ChildItem -LiteralPath $result.EventDirectory -Recurse -File | Where-Object {
                $_.Extension -in @('.json', '.txt', '.sql', '.sha256', '.jsonl') } | Select-String -SimpleMatch -Pattern $script:M4Stub.Fingerprint -List)
            Assert-M4 ($leak.Count -eq 0) ('flow-' + $flow[0] + '-no-account-fingerprint-in-evidence')
        }
        Assert-M4 ((Invoke-M4Flow -AfterScenario 'typed_stop').RecoveryReasonCode -ceq 'P5E_RECONCILE_PROVIDER_STOP') 'flow-typed-stop-recovery-reason-in-result'
        $sentTokens = @((Get-P5ERemoteTokensFromAdbArguments -AdbArguments (@($script:M4Stub.LastArgs))).Tokens)
        Assert-M4 ($sentTokens[1] -ceq 'instrument' -and $sentTokens -contains 'p5e_reconcile_live' -and $sentTokens -contains $script:M4Stub.Fingerprint) 'flow-launch-argv-reaches-supervisor'

        $before = Invoke-M4Flow -AfterScenario 'committed' -Overrides @{ ExpectedDbSha256 = ('0' * 64) }
        Assert-M4 ($before.Classification -ceq 'BEFORE_STOP_NO_LAUNCH' -and $before.LaunchCount -eq 0 -and $script:M4Stub.Launches -eq 0 -and $before.StopCode -ceq 'M4_BEFORE_DATABASE_HASH_MISMATCH_STOP') 'flow-before-db-hash-mismatch-no-launch'
        $script:M4Stub.PackageFailure = $true
        $pkg = Invoke-M4Flow -AfterScenario 'committed'
        Assert-M4 ($pkg.Classification -ceq 'BEFORE_STOP_NO_LAUNCH' -and $script:M4Stub.Launches -eq 0 -and $pkg.StopCode -match 'PACKAGE_APK_HASH_MISMATCH') 'flow-before-package-mismatch-no-launch'
        $script:M4Stub.PackageFailure = $false
        $script:M4Stub.Current = (New-M4Fixture -Scenario 'committed')
        $script:M4Stub.After = $script:M4Stub.Current
        $usedParams = $live.Clone(); $usedParams['ExpectedDbSha256'] = Get-P5ESha256 -Path $script:M4Stub.Current
        $used = Invoke-M4ReconcileEvent -P $usedParams
        Assert-M4 ($used.Classification -ceq 'BEFORE_STOP_NO_LAUNCH' -and $used.StopCode -match 'M4_BEFORE_RECONCILE_ROW_PRESENT') 'flow-before-already-reconciled-no-launch'
        $script:M4Stub.Current = (New-M4Fixture -Scenario 'event7'); $script:M4Stub.After = (New-M4Fixture -Scenario 'committed')
        $badSerial = $false
        $paramsBad = $live.Clone(); $paramsBad['Serial'] = 'other-device'
        try { Invoke-M4ReconcileEvent -P $paramsBad | Out-Null } catch { $badSerial = ([string]$_.Exception.Message -ceq 'M4_SERIAL_NOT_PINNED_STOP') }
        Assert-M4 $badSerial 'serial-not-pinned-refused'
        Assert-M4 ($script:M4ForbiddenCalls -eq 0) 'no-adb-called-during-selftest'
        $forbidden = $false
        try { Invoke-P5EAdbReadOnly } catch { $forbidden = ([string]$_.Exception.Message -ceq 'SELFTEST_ADB_FORBIDDEN') }
        Assert-M4 $forbidden 'selftest-adb-guard-active'
        Write-Output ('SELFTEST PASS ' + $script:M4Passed + '/' + $script:M4Total)
        return 0
    } finally {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# ----------------------------------------------------------------------------------------------
# Entry
# ----------------------------------------------------------------------------------------------
if ($script:M4P['LibraryOnly']) { return }
if ($script:M4P['SelfTest']) {
    $selfTestExit = 1
    try { $selfTestOutput = @(Invoke-M4SelfTest); $selfTestExit = [int]$selfTestOutput[-1]; if ($selfTestOutput.Count -gt 1) { $selfTestOutput[0..($selfTestOutput.Count - 2)] | ForEach-Object { Write-Output ([string]$_) } } }
    catch { Write-Output ('SELFTEST ERROR ' + [string]$_.Exception.Message); $selfTestExit = 1 }
    exit $selfTestExit
}
$liveExit = 1
try {
    $result = Invoke-M4ReconcileEvent -P $script:M4P
    Write-Output ('M4_EVENT_DIRECTORY=' + $result.EventDirectory)
    Write-Output ('M4_CLASSIFICATION=' + $result.Classification)
    Write-Output ('M4_LAUNCH_COUNT=' + [string]$result.LaunchCount)
    if (-not [string]::IsNullOrEmpty($result.StopCode)) { Write-Output ('M4_STOP_CODE=' + $result.StopCode) }
    if (-not [string]::IsNullOrEmpty($result.RecoveryReasonCode)) { Write-Output ('M4_RECOVERY_REASON_CODE=' + $result.RecoveryReasonCode) }
    Write-Output ('M4_MANIFEST_SHA256=' + $result.ManifestSha256)
    if ($result.Classification -in @('RECONCILE_COMMITTED', 'RECONCILE_TYPED_STOP', 'RECONCILE_NOT_DISPATCHED')) { $liveExit = 0 }
    elseif ($result.Classification -ceq 'BEFORE_STOP_NO_LAUNCH') { $liveExit = 3 }
    else { $liveExit = 2 }
} catch {
    Write-Output ('M4_STOP_CODE=' + [string]$_.Exception.Message)
    $liveExit = 4
}
exit $liveExit
