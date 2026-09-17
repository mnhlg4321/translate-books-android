[CmdletBinding(DefaultParameterSetName = 'SelfTest')]
param(
    [Parameter(ParameterSetName = 'SelfTest')]
    [switch]$SelfTest,

    [Parameter(ParameterSetName = 'Dispatch', Mandatory = $true)]
    [switch]$Dispatch,

    [Parameter(ParameterSetName = 'PrepareEvent', Mandatory = $true)]
    [switch]$PrepareEvent,

    [Parameter(ParameterSetName = 'CollectReadback', Mandatory = $true)]
    [switch]$CollectReadback,

    [Parameter(ParameterSetName = 'VerifyOutcome', Mandatory = $true)]
    [switch]$VerifyOutcome,

    [Parameter(ParameterSetName = 'ProvenanceProbe', Mandatory = $true)]
    [switch]$ProvenanceProbe,

    [Parameter(ParameterSetName = 'Dispatch')]
    [Parameter(ParameterSetName = 'PrepareEvent')]
    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$ManifestPath = 'D:\App Translate Books\App Translate Books-translation-profile\docs\P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md',

    [Parameter(ParameterSetName = 'Dispatch')]
    [Parameter(ParameterSetName = 'PrepareEvent')]
    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$ExpectedManifestSha256 = 'DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501',

    [Parameter(ParameterSetName = 'Dispatch')]
    [Parameter(ParameterSetName = 'PrepareEvent')]
    [string]$ProductionApkPath = 'D:\App Translate Books\App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725\TranslateBooks-v4.17-p5e.11-code207.apk',

    [Parameter(ParameterSetName = 'Dispatch')]
    [Parameter(ParameterSetName = 'PrepareEvent')]
    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$ExpectedProductionApkSha256 = '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD',

    [Parameter(ParameterSetName = 'Dispatch')]
    [Parameter(ParameterSetName = 'PrepareEvent')]
    [string]$TestApkPath = 'D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk',

    [Parameter(ParameterSetName = 'Dispatch')]
    [Parameter(ParameterSetName = 'PrepareEvent')]
    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$ExpectedTestApkSha256 = '57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA',

    [Parameter(ParameterSetName = 'Dispatch', Mandatory = $true)]
    [Parameter(ParameterSetName = 'PrepareEvent', Mandatory = $true)]
    [Parameter(ParameterSetName = 'CollectReadback', Mandatory = $true)]
    [string]$ExpectedHelperSha256,

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$EvidenceRoot = 'D:\P5E-private',

    [Parameter(ParameterSetName = 'PrepareEvent', Mandatory = $true)]
    [Parameter(ParameterSetName = 'Dispatch', Mandatory = $true)]
    [Parameter(ParameterSetName = 'CollectReadback', Mandatory = $true)]
    [Parameter(ParameterSetName = 'VerifyOutcome', Mandatory = $true)]
    [string]$EvidenceDirectory,

    [Parameter(ParameterSetName = 'CollectReadback')]
    [ValidateSet('Before', 'After')]
    [string]$CollectionPhase = 'After',

    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$Serial = '15e84958',

    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$AdbPath = 'adb',

    [Parameter(ParameterSetName = 'CollectReadback')]
    [string]$ApkSignerPath = 'apksigner',

    [Parameter(ParameterSetName = 'VerifyOutcome')]
    [string]$PostReadbackPath,

    [Parameter(ParameterSetName = 'VerifyOutcome')]
    [string]$MetadataPath,

    [Parameter(ParameterSetName = 'ProvenanceProbe')]
    [string]$OutputRoot,

    [Parameter(ParameterSetName = 'Library')]
    [switch]$LibraryOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:P5EHostRunSchema = 'p5e.raw.host-run.v1'
$script:P5EReadbackSchema = 'p5e.raw.readback.v1'
$script:P5EReadbackProvenanceSchema = 'p5e.raw.readback.provenance.v1'
$script:P5EProducerInputSchema = 'p5e.raw.readback.producer-input.v1'
$script:P5ECollectorImplementationId = 'p5e.raw.host-readback-collector.v1'
$script:P5ECollectorImplementationVersion = '2'
$script:P5ESourceMappingVersion = 'p5e.raw.readback.source-map.v1'
$script:P5ESyntheticArtifactValidatorId = 'p5e.raw.production-serialized-artifact-validator.v2'
$script:P5EArtifactContractPath = 'docs\P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json'
$script:P5EArtifactContractSha256 = 'ffe70a70e622706fabfa49d5843310ecd5a283b1ca114e32c636ea26b9fae4bf'
$script:P5EEventPlanSchema = 'p5e.raw.event-plan.v1'
$script:P5ECollectorOutcomeSchema = 'p5e.raw.collector-outcome.v1'
$script:P5ECollectorCommandLogFileName = 'COLLECTOR_COMMAND_LOG.jsonl'
$script:P5ECollectorCommandLog = [System.Collections.Generic.List[object]]::new()
$script:P5EActiveCollectorEvidenceDirectory = ''
$script:P5EActiveCollectorPhase = ''
$script:P5ECollectorReadbackMode = 'READ_ONLY_ADB_SQLITE_TRANSACTION'
$script:P5EReadOnlyCommandCount = 0L
$script:P5ETransactionSourcePath = 'app/src/main/java/com/ml/tblandroidtxt/EditorialP5CAttemptStore.java'
$script:P5ETransactionTestPath = 'app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5CExactBindingFakeE2EInstrumentedTest.java'
$script:P5ETransactionSourceSha256 = 'aed6df109016ef56b20a9f2a292f7bf3c09ef92537c52f50300d6191a7fe992d'
$script:P5ETransactionTestSha256 = '0e475cb866c3a12080c46f80c13fe56969cd76bab9a25f8b998f51d6f5399084'
$script:P5ESettingsSourcePath = 'app/src/main/java/com/ml/tblandroidtxt/SettingsStore.java'
$script:P5ESettingsSourceSha256 = 'ac7aa8ac221ad14de9aaf0779c6159df3e8fd93bb77376d5e001e4349cf0eae9'
$script:P5ESettingsDevicePath = '/data/data/com.ml.tblandroidtxt/shared_prefs/settings.xml'
$script:P5EDatabaseDevicePath = '/data/data/com.ml.tblandroidtxt/databases/tbl_android_txt.db'
$script:P5EBindingPhaseIdentity = 'L1_SOURCE_PREFLIGHT'
$script:P5ECollectorSeparator = "`t"
$script:P5ESerial = '15e84958'
$script:P5EClass = 'com.ml.tblandroidtxt.EditorialP5EFreshRawLiveInstrumentedTest'
$script:P5EClassMethod =
    $script:P5EClass + '#authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn'
$script:P5ETestPackage = 'com.ml.tblandroidtxt.test'
$script:P5ETargetPackage = 'com.ml.tblandroidtxt'
$script:P5EInstrumentationRunner = 'androidx.test.runner.AndroidJUnitRunner'
$script:P5ERunner = $script:P5ETestPackage + '/' + $script:P5EInstrumentationRunner
$script:P5EAccountCheckClassMethod =
    'com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest#ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch'
$script:P5EAccountCheckOptInKey = 'p5e_account_check'
$script:P5EAccountCheckExpectedKey = 'p5e_expected_endpoint_account_fingerprint'
$script:P5EAccountCheckRemoteScript =
    'IFS= read -r p5e_expected || exit 64; am instrument -w -r -e class "$1" -e p5e_account_check YES -e p5e_expected_endpoint_account_fingerprint "$p5e_expected" "$2"'
$script:P5ETestSourceCommit = 'd51b7f3c16bdc482513b9904db07b97daed592d1'
$script:P5EAccountEnvironmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
$script:P5EHostObservationTimeoutMilliseconds = 240000L
$script:P5EAuthorizationValidityMilliseconds = 180000L
$script:P5EExecutionDeadlineMilliseconds = 120000L
$script:P5EProductionVersion = 'v4.17-p5e.11'
$script:P5EProductionVersionCode = 207L
$script:P5EExpectedProductionApkSha256 =
    '2ccbb844c629132bb534b0d6aba516055c410bf96d20b14b3f80f91b962800fd'
$script:P5ECertificateSha256 =
    '47f313893a5d68120b075c25825c1c66f1334ac47afb2ef3741084e22ef3c155'
$script:P5EDatabaseSchemaVersion = 24L
$script:P5EDatabaseSha256 =
    '3563f44bce9e529955b6c39142243f59af8f2f0d0095303f5c7a66be07219391'
$script:P5EAuthorizationId = 'P5E-FRESH-MERCEDES-VOL5-RAW-20260911-01'
$script:P5EAuthorizationIdSha256 =
    '0aa82c5897e3df3ec8a7a1586736dbf184b316c66ec165e95e64e8e4832145eb'
$script:P5EAttemptIdentity =
    '7a5e34287d90055a5f0e7d6bb5c9c459202eadcfc538b9f452d548bea298fd6e'
$script:P5ERequestIdentity =
    'ae328c3d771112ce73e9e9d6cba0bb951f930042fc6851a31a96f15f7b70ee06'
$script:P5ERequestEnvelopeHash =
    '5c25e1850c7f70081bd21d67effa2a3a642f410f025ab91cf8044b6ed1bd87f2'
$script:P5ECanonicalRequestBodySha256 =
    'c5920dd842ea92f21d4045c72306a04d59313ac20190c951749fa1b64457c1c2'
$script:P5ERouteFingerprint =
    '23149071716043a2a4dc7fb7af51073b4de838ba072919bb6fd750bc9e62948c'
$script:P5EProjectRowId = 2L
$script:P5EProvider = 'openrouter'
$script:P5EModel = 'openai/gpt-5.6-luna'
$script:P5EUpstreamProvider = 'openai'
$script:P5EPhase = 'L1_RAW_DISCOVERY'
$script:P5ESelector = 'p5e-fresh-mercedes-vol5-20260911-01'
$script:P5EChapterKey = '001'
$script:P5EBindingIdentity =
    '845976b3cde02a3bf0896b64efd208f42e40821317d1b7bffec7081e63e33cdf'
$script:P5ERunDeclarationIdentity =
    '8466b95d96f958a97eb3ffd1eac5a32734023cafa1c230e696ad4253151a41dc'
$script:P5EEvaluationId = '3ce8617c-7e75-453c-ac9a-d3ad21eb7987:compatibility:v1'
$script:P5EPackHash =
    '497786e18e6e2309b44c6695bc8d8e0b538babfe20b1bc0b6f74c395fd05642d'
$script:P5EProfileHash =
    'beec03a42e37f424a6f071ad48f35878b27e1083141699352cda4474d8cc2e21'
$script:P5ERawSourceSha256 =
    'a308210eca80557cfa9fec7ed55b2ee3de5c1c4776e59b2b5edbf0efb04504be'
$script:P5ERawSourceBytes = 23814L
$script:P5EGlossarySourceSha256 =
    '4bc3e2dd05542aa5ca6b7e5fcac43ed53e9af57060eb69c6fa71e9d0a2ea0314'
$script:P5EGlossarySourceBytes = 3249L
$script:P5EDraftSourceSha256 =
    '64adecd8ceccbb13446ef14c494ca9bb1987117c428c5758e7442270ec7f62b5'
$script:P5EDraftSourceBytes = 26462L
$script:P5EPronounSourceSha256 =
    '4947ff9184995be5f850f2323fbe0a04c67302fb8d5afb63cf12202b44720686'
$script:P5EPronounSourceBytes = 452L
$script:P5EExpectedManifestFingerprint =
    '0353d751924d02ef0928bb6460c4ab894fee7c6324506e62b2972090e519c4da'
$script:P5EStopAuthority =
    'OWNER_CONTROLLED|RAW_ONLY|NO_SCHEMA_REPAIR|NO_AUTOMATIC_RETRY|NO_RECONCILE|NO_RESPONSE_HEALING|NO_FALLBACK|PRESERVE_DURABLE_RECOVERY_STATE'
$script:P5ERequiredArgumentKeys = [string[]]@(
    'p5e_fresh_raw_live',
    'p5e_authorization_id',
    'p5e_authorization_id_hash',
    'p5e_owner_approval_manifest_sha256',
    'p5e_expected_attempt_identity',
    'p5e_expected_request_identity',
    'p5e_expected_request_envelope_hash',
    'p5e_expected_canonical_request_body_sha256',
    'p5e_expected_route_fingerprint',
    'p5e_expected_endpoint_account_fingerprint',
    'p5e_expected_project_row_id',
    'p5e_expected_device_signature_token',
    'p5e_expected_provider',
    'p5e_expected_model',
    'p5e_expected_upstream_provider',
    'p5e_expected_phase',
    'p5e_expected_selector',
    'p5e_expected_chapter_key',
    'p5e_expected_binding',
    'p5e_expected_run',
    'p5e_expected_evaluation',
    'p5e_expected_pack_hash',
    'p5e_expected_profile_hash',
    'p5e_expected_schema_version',
    'p5e_expected_production_version_code',
    'p5e_expected_production_apk_sha256',
    'p5e_expected_certificate_sha256',
    'p5e_expected_db_sha256',
    'p5e_maximum_primary_semantic_calls',
    'p5e_maximum_schema_repair_calls',
    'p5e_maximum_network_retries',
    'p5e_maximum_input_tokens',
    'p5e_maximum_output_tokens',
    'p5e_maximum_total_tokens',
    'p5e_maximum_total_cost_usd',
    'p5e_maximum_execution_time_ms',
    'p5e_authorization_issued_at_ms',
    'p5e_authorization_expires_at_ms',
    'p5e_evidence_redaction_policy',
    'p5e_cancellation_stop_authority'
)

function Get-P5ERepoRoot {
    return (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
}

function Get-P5ECollectorPath {
    return [IO.Path]::GetFullPath((Join-Path (Get-P5ERepoRoot) 'scripts\p5e-raw-live-supervisor.ps1'))
}

function Get-P5EArtifactContractPath {
    return [IO.Path]::GetFullPath((Join-Path (Get-P5ERepoRoot) $script:P5EArtifactContractPath))
}

function Get-P5ECollectorCommandLogPath {
    param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)
    return Join-Path (Get-P5ECanonicalPath -Path $EvidenceDirectory) $script:P5ECollectorCommandLogFileName
}

function Get-P5ECollectorCommandClass {
    param([Parameter(Mandatory = $true)][string]$Operation)
    switch -Regex ($Operation) {
        '^pm-path-' { return 'PACKAGE_METADATA_READ_ONLY' }
        '^package-dump-' { return 'PACKAGE_METADATA_READ_ONLY' }
        '^pull-apk-' { return 'INSTALLED_APK_READ_ONLY' }
        '^apk-signer-verify-' { return 'APK_CERTIFICATE_READ_ONLY' }
        '^database-.*-presence$' { return 'DEVICE_FILE_PRESENCE_READ_ONLY' }
        '^database-(main|wal|shm)-hash$' { return 'DATABASE_FILE_HASH_READ_ONLY' }
        '^database-.*-sha256$' { return 'DATABASE_FILE_HASH_READ_ONLY' }
        '^database-(integrity|foreign-key-check|consistent-read-transaction)$' { return 'SQLITE_READ_ONLY' }
        '^(binding-tuple|binding-inputs|lineage-counts|attempt-row|authorization-row|lifecycle-row)$' { return 'SQLITE_READ_ONLY' }
        default { throw ('P5E_COLLECTOR_OPERATION_NOT_ALLOWLISTED:' + $Operation) }
    }
}

function Initialize-P5ECollectorCommandLog {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][ValidateSet('Before', 'After')][string]$CollectionPhase
    )
    $script:P5EActiveCollectorEvidenceDirectory = Get-P5ECanonicalPath -Path $EvidenceDirectory
    $script:P5EActiveCollectorPhase = $CollectionPhase
    $script:P5ECollectorCommandLog = [System.Collections.Generic.List[object]]::new()
    $path = Get-P5ECollectorCommandLogPath -EvidenceDirectory $script:P5EActiveCollectorEvidenceDirectory
    if ($CollectionPhase -eq 'Before') {
        if (Test-Path -LiteralPath $path -PathType Leaf) { throw 'P5E_COLLECTOR_COMMAND_LOG_ALREADY_EXISTS_STOP' }
        [IO.File]::WriteAllText($path, '', [Text.UTF8Encoding]::new($false))
        return
    }
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'P5E_COLLECTOR_COMMAND_LOG_MISSING_STOP' }
    $lines = @(Get-Content -LiteralPath $path | Where-Object { $_ -ne '' })
    foreach ($line in $lines) {
        try { $entry = $line | ConvertFrom-Json } catch { throw 'P5E_COLLECTOR_COMMAND_LOG_INVALID_JSON_STOP' }
        $required = @('schemaVersion', 'eventId', 'evidenceDirectory', 'collectionPhase',
            'sequence', 'operation', 'commandClass', 'launchCount', 'exitCode', 'timedOut',
            'outcome', 'redactionPass', 'outputCaptured')
        $properties = @($entry.PSObject.Properties.Name)
        if (@($properties | Where-Object { $_ -notin $required }).Count -ne 0 -or
                @($required | Where-Object { $_ -notin $properties }).Count -ne 0) {
            throw 'P5E_COLLECTOR_COMMAND_LOG_SHAPE_INVALID_STOP'
        }
        $expectedClass = $null
        try { $expectedClass = Get-P5ECollectorCommandClass -Operation ([string](Get-P5EProperty $entry 'operation')) } catch { throw 'P5E_COLLECTOR_COMMAND_LOG_OPERATION_INVALID_STOP' }
        if ([string](Get-P5EProperty $entry 'schemaVersion') -cne 'p5e.raw.collector-command.v1' -or
                [string](Get-P5EProperty $entry 'eventId') -cne (Split-Path -Leaf $script:P5EActiveCollectorEvidenceDirectory) -or
                -not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $entry 'evidenceDirectory')) -Right $script:P5EActiveCollectorEvidenceDirectory) -or
                [string](Get-P5EProperty $entry 'collectionPhase') -notin @('Before', 'After') -or
                [string](Get-P5EProperty $entry 'commandClass') -cne $expectedClass -or
                -not ((Get-P5EProperty $entry 'timedOut') -is [bool]) -or
                -not ((Get-P5EProperty $entry 'redactionPass') -is [bool]) -or
                -not ((Get-P5EProperty $entry 'outputCaptured') -is [bool]) -or
                [bool](Get-P5EProperty $entry 'outputCaptured')) {
            throw 'P5E_COLLECTOR_COMMAND_LOG_ENTRY_INVALID_STOP'
        }
        [void]$script:P5ECollectorCommandLog.Add($entry)
    }
}

function Add-P5ECollectorCommandRecord {
    param(
        [Parameter(Mandatory = $true)][string]$Operation,
        [Parameter(Mandatory = $true)]$Run
    )
    if ([string]::IsNullOrWhiteSpace($script:P5EActiveCollectorEvidenceDirectory)) { return }
    if ($script:P5ECollectorCommandLog.Count -ge 128) { throw 'P5E_COLLECTOR_COMMAND_LOG_LIMIT_STOP' }
    $record = [ordered]@{
        schemaVersion = 'p5e.raw.collector-command.v1'
        eventId = Split-Path -Leaf $script:P5EActiveCollectorEvidenceDirectory
        evidenceDirectory = $script:P5EActiveCollectorEvidenceDirectory
        collectionPhase = $script:P5EActiveCollectorPhase
        sequence = [long]($script:P5ECollectorCommandLog.Count + 1)
        operation = $Operation
        commandClass = Get-P5ECollectorCommandClass -Operation $Operation
        launchCount = [long]$Run.LaunchCount
        exitCode = $Run.ExitCode
        timedOut = [bool]$Run.TimedOut
        outcome = [string]$Run.Outcome
        redactionPass = -not [bool]$Run.RedactionViolation
        outputCaptured = $false
    }
    [void]$script:P5ECollectorCommandLog.Add($record)
    $path = Get-P5ECollectorCommandLogPath -EvidenceDirectory $script:P5EActiveCollectorEvidenceDirectory
    $line = (ConvertTo-P5ECanonicalJson -Value $record) + [Environment]::NewLine
    [IO.File]::AppendAllText($path, $line, [Text.UTF8Encoding]::new($false))
}

function Assert-P5EHelperRuntimeHash {
    param(
        [Parameter(Mandatory = $true)][string]$ExpectedSha256,
        [string]$Path = (Get-P5ECollectorPath)
    )
    if ($ExpectedSha256 -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'HELPER_RUNTIME_HASH_EXPECTATION_MISSING_OR_INVALID_STOP'
    }
    $literalPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literalPath -PathType Leaf)) {
        throw 'HELPER_RUNTIME_FILE_MISSING_STOP'
    }
    $item = Get-Item -LiteralPath $literalPath -Force
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw 'HELPER_RUNTIME_PATH_REPARSE_OR_NOT_REGULAR_STOP'
    }
    $resolvedPath = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literalPath).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literalPath, $resolvedPath)) {
        throw 'HELPER_RUNTIME_PATH_RESOLUTION_MISMATCH_STOP'
    }
    $actual = Get-P5ESha256 -Path $literalPath
    if ($actual -cne $ExpectedSha256.ToLowerInvariant()) {
        throw 'HELPER_RUNTIME_HASH_MISMATCH_STOP'
    }
    return $actual
}

function Get-P5ECanonicalPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (Test-Path -LiteralPath $Path) {
        return ([IO.Path]::GetFullPath((Resolve-Path -LiteralPath $Path).Path)).TrimEnd('\')
    }
    return ([IO.Path]::GetFullPath($Path)).TrimEnd('\')
}

function Test-P5EPathEqual {
    param(
        [Parameter(Mandatory = $true)][string]$Left,
        [Parameter(Mandatory = $true)][string]$Right
    )
    return [StringComparer]::OrdinalIgnoreCase.Equals(
        (Get-P5ECanonicalPath -Path $Left), (Get-P5ECanonicalPath -Path $Right))
}

function Test-P5EPathUnderDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Directory
    )
    $canonicalPath = Get-P5ECanonicalPath -Path $Path
    $canonicalDirectory = (Get-P5ECanonicalPath -Path $Directory).TrimEnd('\') + '\'
    return $canonicalPath.StartsWith($canonicalDirectory, [StringComparison]::OrdinalIgnoreCase)
}

function Get-P5ESha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Write-P5EUtf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Text
    )
    $encoding = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($Path, $Text, $encoding)
}

function ConvertTo-P5EJson {
    param([Parameter(Mandatory = $true)]$Value)
    return ($Value | ConvertTo-Json -Depth 30 -Compress)
}

function ConvertTo-P5ECanonicalValue {
    param($Value)
    if ($null -eq $Value) { return $null }
    if ($Value -is [System.Collections.IDictionary]) {
        $ordered = [ordered]@{}
        foreach ($key in @($Value.Keys | Sort-Object { [string]$_ })) {
            $ordered[[string]$key] = ConvertTo-P5ECanonicalValue -Value $Value[$key]
        }
        return $ordered
    }
    if ($Value -is [System.Management.Automation.PSCustomObject]) {
        $ordered = [ordered]@{}
        foreach ($property in @($Value.PSObject.Properties | Sort-Object Name)) {
            $ordered[$property.Name] = ConvertTo-P5ECanonicalValue -Value $property.Value
        }
        return $ordered
    }
    if ($Value -is [System.Collections.IEnumerable] -and $Value -isnot [string]) {
        return ,@($Value | ForEach-Object { ConvertTo-P5ECanonicalValue -Value $_ })
    }
    return $Value
}

function ConvertTo-P5ECanonicalJson {
    param([AllowNull()]$Value)
    return (ConvertTo-P5ECanonicalValue -Value $Value | ConvertTo-Json -Depth 100 -Compress)
}

function Get-P5EArtifactContract {
    $path = Get-P5EArtifactContractPath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'PRODUCTION_ARTIFACT_CONTRACT_MISSING_STOP' }
    if ((Get-P5ESha256 -Path $path) -cne $script:P5EArtifactContractSha256) {
        throw 'PRODUCTION_ARTIFACT_CONTRACT_HASH_MISMATCH_STOP'
    }
    try {
        $contract = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
    } catch { throw 'PRODUCTION_ARTIFACT_CONTRACT_INVALID_STOP' }
    $source = Get-P5EProperty -Object $contract -Name 'source'
    if ($null -eq $source -or [string](Get-P5EProperty $source 'sourceSha256') -cne
            '1222b8ac9b79dafc659dd364f50849dfba4782c181606a92da47ebd8c6164e3c') {
        throw 'PRODUCTION_ARTIFACT_CONTRACT_SOURCE_PIN_INVALID_STOP'
    }
    $serializerPath = Join-Path (Get-P5ERepoRoot) 'editorial-engine/src/main/java/com/ml/tblandroidtxt/editorial/pack/EditorialP5PilotExecution.java'
    if (-not (Test-Path -LiteralPath $serializerPath -PathType Leaf) -or
            (Get-P5ESha256 -Path $serializerPath) -cne [string](Get-P5EProperty $source 'sourceSha256')) {
        throw 'PRODUCTION_SERIALIZER_SOURCE_HASH_MISMATCH_STOP'
    }
    return $contract
}

function Get-P5EAccountFingerprint {
    $value = [Environment]::GetEnvironmentVariable($script:P5EAccountEnvironmentName, 'Process')
    if ([string]::IsNullOrWhiteSpace($value) -or $value -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'OWNER_ENDPOINT_ACCOUNT_FINGERPRINT_MISSING_OR_INVALID_STOP'
    }
    # The Android method returns lowercase hex and uses assertEquals, while
    # the host accepts either case. Normalize only in process memory.
    return $value.ToLowerInvariant()
}

function Get-P5EInstrumentationComponent {
    return $script:P5ETestPackage + '/' + $script:P5EInstrumentationRunner
}

function Assert-P5EInstrumentationComponent {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Component)
    $expected = Get-P5EInstrumentationComponent
    if ([string]::IsNullOrWhiteSpace($Component) -or $Component -cne $expected) {
        throw 'P5E_INSTRUMENTATION_COMPONENT_INVALID_STOP'
    }
    return $Component
}

function New-P5EAccountCheckRemoteCommandTokens {
    param(
        [string]$ClassMethod = $script:P5EAccountCheckClassMethod,
        [string]$Component = ''
    )
    if ([string]::IsNullOrWhiteSpace($Component)) { $Component = Get-P5EInstrumentationComponent }
    if ($ClassMethod -cne $script:P5EAccountCheckClassMethod) {
        throw 'P5E_ACCOUNT_CHECK_CLASS_METHOD_INVALID_STOP'
    }
    [void](Assert-P5EInstrumentationComponent -Component $Component)
    return [string[]]@(
        'sh',
        '-c',
        $script:P5EAccountCheckRemoteScript,
        '--',
        $ClassMethod,
        $Component
    )
}

function Test-P5EAccountCheckCommandArguments {
    param(
        [Parameter(Mandatory = $true)][string[]]$AdbArguments,
        [string]$ClassMethod = $script:P5EAccountCheckClassMethod,
        [AllowEmptyString()][string]$SensitiveValue = ''
    )
    $errors = [System.Collections.Generic.List[string]]::new()
    try {
        $remote = Get-P5ERemoteTokensFromAdbArguments -AdbArguments $AdbArguments
    } catch {
        return [pscustomobject]@{ Passed = $false; Errors = @('ACCOUNT_CHECK_REMOTE_ARGUMENT_PARSE_FAILED') }
    }
    if ($remote.Operators.Count -ne 0) { [void]$errors.Add('ACCOUNT_CHECK_REMOTE_SHELL_OPERATOR_PRESENT') }
    try {
        $expectedTokens = @(New-P5EAccountCheckRemoteCommandTokens -ClassMethod $ClassMethod)
        if ((@($remote.Tokens) -join "`n") -cne (@($expectedTokens) -join "`n")) {
            [void]$errors.Add('ACCOUNT_CHECK_REMOTE_COMMAND_SHAPE_INVALID')
        }
    } catch {
        [void]$errors.Add($_.Exception.Message)
    }
    if (-not [string]::IsNullOrEmpty($SensitiveValue)) {
        $remoteText = [string]::Join("`n", [string[]]$remote.Tokens)
        $adbText = [string]::Join("`n", [string[]]$AdbArguments)
        if ($remoteText.IndexOf($SensitiveValue, [StringComparison]::Ordinal) -ge 0 -or
                $adbText.IndexOf($SensitiveValue, [StringComparison]::Ordinal) -ge 0) {
            [void]$errors.Add('ACCOUNT_CHECK_EXPECTED_IN_COMMAND_STOP')
        }
    }
    return [pscustomobject]@{ Passed = $errors.Count -eq 0; Errors = $errors.ToArray() }
}

function Parse-P5EAccountCheckInstrumentationOutput {
    param(
        [AllowEmptyString()][string]$Output,
        [string]$ExpectedClassMethod = $script:P5EAccountCheckClassMethod
    )
    $errors = [System.Collections.Generic.List[string]]::new()
    $classValues = [System.Collections.Generic.List[string]]::new()
    $methodValues = [System.Collections.Generic.List[string]]::new()
    $resultValues = [System.Collections.Generic.List[string]]::new()
    $terminalValues = [System.Collections.Generic.List[string]]::new()
    $failureMarkerCount = 0
    $okTestCount = 0
    $statusCodeFailureCount = 0
    $lines = if ($null -eq $Output) { @() } else { @($Output -split '\r?\n') }
    if ($null -eq $Output -or [string]::IsNullOrEmpty($Output)) { [void]$errors.Add('ACCOUNT_CHECK_OUTPUT_MISSING') }

    foreach ($line in $lines) {
        if ($line -match '^INSTRUMENTATION_STATUS: class=(.*)$') {
            [void]$classValues.Add($Matches[1])
        } elseif ($line -match '^INSTRUMENTATION_STATUS: test=(.*)$') {
            [void]$methodValues.Add($Matches[1])
        } elseif ($line -match '^INSTRUMENTATION_STATUS: p5e\.account\.result=(.*)$') {
            [void]$resultValues.Add($Matches[1])
        } elseif ($line -match '^INSTRUMENTATION_CODE:\s*(.*)$') {
            [void]$terminalValues.Add($Matches[1])
        } elseif ($line -match '^INSTRUMENTATION_STATUS_CODE:\s*-\d+\s*$') {
            $statusCodeFailureCount++
        }
        if ($line.Trim() -ceq 'OK (1 test)') { $okTestCount++ }
        if ($line -match '(?i)FAILURES!!!|INSTRUMENTATION_FAILED\b|There (?:was|were) \d+ failures?|AssumptionViolatedException|\bSKIPPED\b') {
            $failureMarkerCount++
        }
    }

    if ($classValues.Count -ne 1) { [void]$errors.Add('ACCOUNT_CHECK_CLASS_IDENTITY_COUNT_INVALID') }
    if ($methodValues.Count -ne 1) { [void]$errors.Add('ACCOUNT_CHECK_METHOD_IDENTITY_COUNT_INVALID') }
    $classSeparator = $ExpectedClassMethod.IndexOf('#')
    $expectedClass = if ($classSeparator -gt 0) { $ExpectedClassMethod.Substring(0, $classSeparator) } else { '' }
    $expectedMethod = if ($classSeparator -gt 0) { $ExpectedClassMethod.Substring($classSeparator + 1) } else { '' }
    if ($classValues.Count -eq 1 -and $classValues[0] -cne $expectedClass) {
        [void]$errors.Add('ACCOUNT_CHECK_CLASS_IDENTITY_MISMATCH')
    }
    if ($methodValues.Count -eq 1 -and $methodValues[0] -cne $expectedMethod) {
        [void]$errors.Add('ACCOUNT_CHECK_METHOD_IDENTITY_MISMATCH')
    }
    if ($resultValues.Count -ne 1) {
        [void]$errors.Add('ACCOUNT_CHECK_RESULT_TOKEN_COUNT_INVALID')
    }
    $result = if ($resultValues.Count -eq 1 -and $resultValues[0] -match '^(MATCH|MISMATCH)$') {
        $resultValues[0]
    } else { '' }
    if ($resultValues.Count -eq 1 -and [string]::IsNullOrEmpty($result)) {
        [void]$errors.Add('ACCOUNT_CHECK_RESULT_TOKEN_INVALID')
    }
    if ($terminalValues.Count -ne 1) {
        [void]$errors.Add('ACCOUNT_CHECK_TERMINAL_MARKER_COUNT_INVALID')
    }
    $terminalSuccess = $terminalValues.Count -eq 1 -and $terminalValues[0].Trim() -ceq '-1'
    if ($terminalValues.Count -eq 1 -and -not $terminalSuccess) {
        [void]$errors.Add('ACCOUNT_CHECK_TERMINAL_MARKER_INVALID')
    }
    if ($okTestCount -ne 1) { [void]$errors.Add('ACCOUNT_CHECK_TEST_FINISHED_MARKER_INVALID') }
    if ($statusCodeFailureCount -ne 0) { [void]$errors.Add('ACCOUNT_CHECK_INSTRUMENTATION_FAILURE_STATUS') }
    if ($failureMarkerCount -ne 0) { [void]$errors.Add('ACCOUNT_CHECK_FAILURE_MARKER_PRESENT') }

    return [pscustomobject]@{
        Accepted = $errors.Count -eq 0
        Result = $result
        TerminalSuccess = $terminalSuccess
        TestFinished = $okTestCount -eq 1
        IdentityPass = $classValues.Count -eq 1 -and $methodValues.Count -eq 1 -and
            $classValues[0] -ceq $expectedClass -and $methodValues[0] -ceq $expectedMethod
        TerminalCode = if ($terminalValues.Count -eq 1) { $terminalValues[0].Trim() } else { '' }
        ResultTokenCount = $resultValues.Count
        FailureMarkerCount = $failureMarkerCount
        Errors = $errors.ToArray()
    }
}

function ConvertTo-P5EAndroidShellArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    if ($Value.IndexOf([char]0) -ge 0 -or $Value.IndexOf("`r") -ge 0 -or
            $Value.IndexOf("`n") -ge 0) {
        throw 'REMOTE_ARGUMENT_CONTAINS_UNSUPPORTED_CONTROL_CHARACTER_STOP'
    }
    # adb joins the arguments following `shell` into one remote command. POSIX
    # single-quote every token so an Android shell cannot interpret pipes, spaces
    # or other metacharacters. The replacement is the standard closing-quote,
    # escaped-quote, reopening-quote sequence.
    $escaped = $Value.Replace("'", "'\''")
    return "'" + $escaped + "'"
}

function New-P5EPlan {
    param(
        [Parameter(Mandatory = $true)][string]$ManifestHash,
        [Parameter(Mandatory = $true)][string]$AccountFingerprint,
        [Parameter(Mandatory = $true)][long]$IssuedAtMillis,
        [Parameter(Mandatory = $true)][long]$ExpiresAtMillis
    )
    $pairs = [ordered]@{
        p5e_fresh_raw_live = 'YES'
        p5e_authorization_id = $script:P5EAuthorizationId
        p5e_authorization_id_hash = $script:P5EAuthorizationIdSha256
        p5e_owner_approval_manifest_sha256 = $ManifestHash
        p5e_expected_attempt_identity = $script:P5EAttemptIdentity
        p5e_expected_request_identity = $script:P5ERequestIdentity
        p5e_expected_request_envelope_hash = $script:P5ERequestEnvelopeHash
        p5e_expected_canonical_request_body_sha256 = $script:P5ECanonicalRequestBodySha256
        p5e_expected_route_fingerprint = $script:P5ERouteFingerprint
        p5e_expected_endpoint_account_fingerprint = $AccountFingerprint
        p5e_expected_project_row_id = [string]$script:P5EProjectRowId
        p5e_expected_device_signature_token = 'abebea4b'
        p5e_expected_provider = $script:P5EProvider
        p5e_expected_model = $script:P5EModel
        p5e_expected_upstream_provider = $script:P5EUpstreamProvider
        p5e_expected_phase = $script:P5EPhase
        p5e_expected_selector = $script:P5ESelector
        p5e_expected_chapter_key = $script:P5EChapterKey
        p5e_expected_binding = $script:P5EBindingIdentity
        p5e_expected_run = $script:P5ERunDeclarationIdentity
        p5e_expected_evaluation = $script:P5EEvaluationId
        p5e_expected_pack_hash = $script:P5EPackHash
        p5e_expected_profile_hash = $script:P5EProfileHash
        p5e_expected_schema_version = [string]$script:P5EDatabaseSchemaVersion
        p5e_expected_production_version_code = [string]$script:P5EProductionVersionCode
        p5e_expected_production_apk_sha256 = $script:P5EExpectedProductionApkSha256
        p5e_expected_certificate_sha256 = $script:P5ECertificateSha256
        p5e_expected_db_sha256 = $script:P5EDatabaseSha256
        p5e_maximum_primary_semantic_calls = '1'
        p5e_maximum_schema_repair_calls = '0'
        p5e_maximum_network_retries = '0'
        p5e_maximum_input_tokens = '100000'
        p5e_maximum_output_tokens = '4096'
        p5e_maximum_total_tokens = '104096'
        p5e_maximum_total_cost_usd = '0.05'
        p5e_maximum_execution_time_ms = [string]$script:P5EExecutionDeadlineMilliseconds
        p5e_authorization_issued_at_ms = [string]$IssuedAtMillis
        p5e_authorization_expires_at_ms = [string]$ExpiresAtMillis
        p5e_evidence_redaction_policy = 'HASH_ONLY'
        p5e_cancellation_stop_authority = $script:P5EStopAuthority
    }
    return $pairs
}

function New-P5ERemoteCommandTokens {
    param([Parameter(Mandatory = $true)]$Pairs)
    $tokens = [System.Collections.Generic.List[string]]::new()
    [void]$tokens.Add('am')
    [void]$tokens.Add('instrument')
    [void]$tokens.Add('-w')
    [void]$tokens.Add('-r')
    [void]$tokens.Add('-e')
    [void]$tokens.Add('class')
    [void]$tokens.Add($script:P5EClassMethod)
    foreach ($pair in $Pairs.GetEnumerator()) {
        [void]$tokens.Add('-e')
        [void]$tokens.Add([string]$pair.Key)
        [void]$tokens.Add([string]$pair.Value)
    }
    [void]$tokens.Add($script:P5ERunner)
    return $tokens.ToArray()
}

function New-P5EAdbArgumentList {
    param(
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string[]]$RemoteCommandTokens
    )
    $arguments = [System.Collections.Generic.List[string]]::new()
    [void]$arguments.Add('-s')
    [void]$arguments.Add($Serial)
    [void]$arguments.Add('shell')
    foreach ($token in $RemoteCommandTokens) {
        [void]$arguments.Add((ConvertTo-P5EAndroidShellArgument -Value $token))
    }
    return $arguments.ToArray()
}

function ConvertFrom-P5EPosixCommandLine {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$CommandLine)
    $tokens = [System.Collections.Generic.List[string]]::new()
    $operators = [System.Collections.Generic.List[string]]::new()
    $current = [System.Text.StringBuilder]::new()
    $state = 'UNQUOTED'
    $started = $false
    for ($index = 0; $index -lt $CommandLine.Length; $index++) {
        $character = $CommandLine[$index]
        if ($state -eq 'SINGLE') {
            if ($character -eq "'") { $state = 'UNQUOTED' }
            else { [void]$current.Append($character) }
            continue
        }
        if ($state -eq 'DOUBLE') {
            if ($character -eq '"') { $state = 'UNQUOTED'; continue }
            if ($character -eq '\' -and $index + 1 -lt $CommandLine.Length) {
                $index++
                [void]$current.Append($CommandLine[$index])
            } else { [void]$current.Append($character) }
            continue
        }
        if ([char]::IsWhiteSpace($character)) {
            if ($started) {
                [void]$tokens.Add($current.ToString())
                [void]$current.Clear()
                $started = $false
            }
            continue
        }
        if ($character -eq "'") { $state = 'SINGLE'; $started = $true; continue }
        if ($character -eq '"') { $state = 'DOUBLE'; $started = $true; continue }
        if ($character -eq '\' -and $index + 1 -lt $CommandLine.Length) {
            $index++
            [void]$current.Append($CommandLine[$index])
            $started = $true
            continue
        }
        if ($character -eq '|') {
            if ($started) {
                [void]$tokens.Add($current.ToString())
                [void]$current.Clear()
                $started = $false
            }
            [void]$operators.Add('|')
            continue
        }
        [void]$current.Append($character)
        $started = $true
    }
    if ($state -ne 'UNQUOTED') { throw 'REMOTE_SHELL_UNTERMINATED_QUOTE_STOP' }
    if ($started) { [void]$tokens.Add($current.ToString()) }
    return [pscustomobject]@{ Tokens = $tokens.ToArray(); Operators = $operators.ToArray() }
}

function Get-P5ERemoteTokensFromAdbArguments {
    param([Parameter(Mandatory = $true)][string[]]$AdbArguments)
    if ($AdbArguments.Count -lt 4 -or $AdbArguments[0] -ne '-s' -or
            $AdbArguments[2] -ne 'shell') {
        throw 'ADB_LOCAL_ARGUMENT_SHAPE_INVALID_STOP'
    }
    $remoteText = [string]::Join(' ', [string[]]$AdbArguments[3..($AdbArguments.Count - 1)])
    return ConvertFrom-P5EPosixCommandLine -CommandLine $remoteText
}

function Get-P5ESourceRequiredArgumentKeys {
    param([Parameter(Mandatory = $true)][string]$SourcePath)
    $source = Get-Content -Raw -LiteralPath $SourcePath
    $methodMatch = [regex]::Match($source,
        '(?s)public void authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn\(\).*?\n    private static void assertCommittedRawResult')
    if (-not $methodMatch.Success) { throw 'PINNED_LIVE_METHOD_NOT_FOUND_STOP' }
    $method = $methodMatch.Value
    $keys = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($pattern in @(
            'required\(arguments,\s*"([^"]+)"',
            'requireExact\(arguments,\s*"([^"]+)"',
            'requireExactSha256\(arguments,\s*"([^"]+)"',
            'requiredInt\(arguments,\s*"([^"]+)"',
            'requiredLong\(arguments,\s*"([^"]+)"',
            'requiredDecimal\(arguments,\s*"([^"]+)"')) {
        foreach ($match in [regex]::Matches($method, $pattern)) {
            [void]$keys.Add($match.Groups[1].Value)
        }
    }
    [void]$keys.Add('p5e_fresh_raw_live')
    return @($keys | Sort-Object)
}

function Test-P5ERequiredSourceContract {
    param([Parameter(Mandatory = $true)][string]$RepoRoot)
    $sourcePath = Join-Path $RepoRoot 'app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EFreshRawLiveInstrumentedTest.java'
    $errors = [System.Collections.Generic.List[string]]::new()
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        [void]$errors.Add('PINNED_LIVE_SOURCE_MISSING')
    } else {
        try {
            $sourceKeys = @(Get-P5ESourceRequiredArgumentKeys -SourcePath $sourcePath)
            $expectedKeys = @($script:P5ERequiredArgumentKeys | Sort-Object)
            if ((@($sourceKeys) -join "`n") -cne (@($expectedKeys) -join "`n")) {
                [void]$errors.Add('SOURCE_REQUIRED_ARGUMENT_SET_MISMATCH')
            }
            $git = (Get-Command git -ErrorAction Stop).Source
            & $git -C $RepoRoot diff --quiet $script:P5ETestSourceCommit -- app/src/androidTest/java/com/ml/tblandroidtxt/EditorialP5EFreshRawLiveInstrumentedTest.java
            if ($LASTEXITCODE -ne 0) { [void]$errors.Add('TEST_SOURCE_DIFFERS_FROM_PINNED_APK_COMMIT') }
        } catch {
            [void]$errors.Add('SOURCE_REQUIRED_ARGUMENT_AUDIT_FAILED')
        }
    }
    return [pscustomobject]@{ Passed = $errors.Count -eq 0; Errors = $errors.ToArray() }
}

function Test-P5EInstrumentationArguments {
    param(
        [Parameter(Mandatory = $true)][string[]]$AdbArguments,
        [Parameter(Mandatory = $true)][string]$ManifestHash,
        [Parameter(Mandatory = $true)][string]$AccountFingerprint,
        [Parameter(Mandatory = $true)][long]$IssuedAtMillis,
        [Parameter(Mandatory = $true)][long]$ExpiresAtMillis
    )
    $errors = [System.Collections.Generic.List[string]]::new()
    try { $remote = Get-P5ERemoteTokensFromAdbArguments -AdbArguments $AdbArguments }
    catch { return [pscustomobject]@{ Passed = $false; Errors = @('REMOTE_ARGUMENT_PARSE_FAILED'); Pairs = @{} } }
    if ($remote.Operators.Count -ne 0) { [void]$errors.Add('REMOTE_SHELL_OPERATOR_PRESENT') }
    $tokens = @($remote.Tokens)
    if ($tokens.Count -lt 7 -or $tokens[0] -ne 'am' -or $tokens[1] -ne 'instrument' -or
            $tokens[2] -ne '-w' -or $tokens[3] -ne '-r' -or
            $tokens[$tokens.Count - 1] -ne $script:P5ERunner) {
        [void]$errors.Add('REMOTE_INSTRUMENT_COMMAND_SHAPE_INVALID')
    }
    $pairs = [ordered]@{}
    if ($tokens.Count -gt 4) {
        $index = 4
        while ($index -lt $tokens.Count - 1) {
            if ($tokens[$index] -ne '-e' -or $index + 2 -ge $tokens.Count) {
                [void]$errors.Add('REMOTE_KEY_VALUE_SHAPE_INVALID')
                break
            }
            $key = $tokens[$index + 1]
            $value = $tokens[$index + 2]
            if ($pairs.Contains($key)) { [void]$errors.Add('DUPLICATE_ARGUMENT:' + $key) }
            else { $pairs[$key] = $value }
            $index += 3
        }
    }
    $expectedKeys = [System.Collections.Generic.HashSet[string]]::new($script:P5ERequiredArgumentKeys)
    foreach ($key in $expectedKeys) {
        if (-not $pairs.Contains($key)) { [void]$errors.Add('MISSING_ARGUMENT:' + $key) }
    }
    foreach ($key in @($pairs.Keys)) {
        if (-not $expectedKeys.Contains($key) -and $key -ne 'class') {
            [void]$errors.Add('UNEXPECTED_ARGUMENT:' + $key)
        }
    }
    $expected = New-P5EPlan -ManifestHash $ManifestHash -AccountFingerprint $AccountFingerprint `
        -IssuedAtMillis $IssuedAtMillis -ExpiresAtMillis $ExpiresAtMillis
    foreach ($key in $expected.Keys) {
        if ($pairs.Contains($key) -and [string]$pairs[$key] -cne [string]$expected[$key]) {
            [void]$errors.Add('ARGUMENT_VALUE_MISMATCH:' + $key)
        }
    }
    if (-not $pairs.Contains('class') -or $pairs['class'] -cne $script:P5EClassMethod) {
        [void]$errors.Add('CLASS_METHOD_MISMATCH')
    }
    foreach ($key in @('p5e_authorization_id_hash', 'p5e_owner_approval_manifest_sha256',
            'p5e_expected_attempt_identity', 'p5e_expected_request_identity',
            'p5e_expected_request_envelope_hash', 'p5e_expected_canonical_request_body_sha256',
            'p5e_expected_route_fingerprint', 'p5e_expected_endpoint_account_fingerprint',
            'p5e_expected_production_apk_sha256', 'p5e_expected_certificate_sha256',
            'p5e_expected_db_sha256')) {
        if ($pairs.Contains($key) -and $pairs[$key] -notmatch '^[0-9a-f]{64}$') {
            [void]$errors.Add('HASH_FORMAT_INVALID:' + $key)
        }
    }
    foreach ($key in @('p5e_expected_project_row_id', 'p5e_expected_schema_version',
            'p5e_expected_production_version_code', 'p5e_maximum_primary_semantic_calls',
            'p5e_maximum_schema_repair_calls', 'p5e_maximum_network_retries',
            'p5e_maximum_input_tokens', 'p5e_maximum_output_tokens',
            'p5e_maximum_total_tokens', 'p5e_maximum_execution_time_ms',
            'p5e_authorization_issued_at_ms', 'p5e_authorization_expires_at_ms')) {
        if ($pairs.Contains($key) -and $pairs[$key] -notmatch '^[0-9]+$') {
            [void]$errors.Add('INTEGER_FORMAT_INVALID:' + $key)
        }
    }
    if ($pairs.Contains('p5e_maximum_total_cost_usd') -and
            $pairs['p5e_maximum_total_cost_usd'] -notmatch '^[0-9]+(\.[0-9]+)?$') {
        [void]$errors.Add('DECIMAL_FORMAT_INVALID:p5e_maximum_total_cost_usd')
    }
    if ($pairs.Contains('p5e_authorization_issued_at_ms') -and
            $pairs.Contains('p5e_authorization_expires_at_ms')) {
        $issued = [long]$pairs['p5e_authorization_issued_at_ms']
        $expires = [long]$pairs['p5e_authorization_expires_at_ms']
        if ($expires -ne $issued + $script:P5EAuthorizationValidityMilliseconds -or
                $issued -ne $IssuedAtMillis -or $expires -ne $ExpiresAtMillis) {
            [void]$errors.Add('AUTHORIZATION_WINDOW_INVALID')
        }
    }
    return [pscustomobject]@{ Passed = $errors.Count -eq 0; Errors = $errors.ToArray(); Pairs = $pairs }
}

function Protect-P5ECaptureText {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Text,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @()
    )
    $patterns = @(
        '(?im)\bapi[_ -]?key\s*[:=]\s*[^\s\r\n]+' ,
        '(?im)\b(?:proxy-)?authorization\s*:\s*bearer\s+[^\s\r\n]+',
        '(?im)\b(?:base[_ -]?url|endpoint)\s*[:=]\s*[^\s\r\n]+',
        '(?im)https?://[^\s"<>]+',
        '(?im)(expected\s*:\s*<)([0-9a-fA-F]{64})(>)',
        '(?im)(but\s+was\s*:\s*<)([0-9a-fA-F]{64})(>)',
        '(?im)(expected\s*[:=]\s*)([0-9a-fA-F]{64})',
        '(?im)(actual\s*[:=]\s*)([0-9a-fA-F]{64})'
    )
    $protected = $Text
    $violation = $false
    foreach ($sensitive in @($SensitiveValues | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            Select-Object -Unique)) {
        $sensitivePattern = [regex]::Escape([string]$sensitive)
        if ([regex]::IsMatch($protected, $sensitivePattern, [Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
            $violation = $true
            $protected = [regex]::Replace($protected, $sensitivePattern,
                '[REDACTED_BY_HOST_CAPTURE]',
                [Text.RegularExpressions.RegexOptions]::IgnoreCase)
        }
    }
    foreach ($pattern in $patterns) {
        if ([regex]::IsMatch($protected, $pattern)) {
            $violation = $true
            $protected = [regex]::Replace($protected, $pattern, '[REDACTED_BY_HOST_CAPTURE]')
        }
    }
    return [pscustomobject]@{ Text = $protected; Violation = $violation }
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
        if ($backslashes -gt 0) { [void]$builder.Append(('\' * $backslashes)); $backslashes = 0 }
        [void]$builder.Append($character)
    }
    if ($backslashes -gt 0) { [void]$builder.Append(('\' * (2 * $backslashes)) ) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Set-P5EProcessStartInfoArguments {
    param(
        [Parameter(Mandatory = $true)][System.Diagnostics.ProcessStartInfo]$StartInfo,
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

function Get-P5EProcessTaskTextBounded {
    param(
        [Parameter(Mandatory = $true)][System.Threading.Tasks.Task]$Task,
        [int]$TimeoutMilliseconds = 5000
    )
    try {
        if (-not $Task.Wait($TimeoutMilliseconds)) {
            return [pscustomobject]@{ Completed = $false; Text = ''; ErrorClass = 'TASK_WAIT_TIMEOUT' }
        }
        return [pscustomobject]@{
            Completed = $true
            Text = [string]$Task.GetAwaiter().GetResult()
            ErrorClass = ''
        }
    } catch {
        return [pscustomobject]@{ Completed = $false; Text = ''; ErrorClass = $_.Exception.GetType().Name }
    }
}

function Invoke-P5EProcessSupervisor {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][long]$TimeoutMilliseconds,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @(),
        [AllowEmptyString()][string]$StandardInputText = '',
        [AllowEmptyCollection()][string[]]$ClearInheritedEnvironmentVariableNames = @()
    )
    $stdoutPath = Join-Path $EvidenceDirectory 'instrumentation-stdout.txt'
    $stderrPath = Join-Path $EvidenceDirectory 'instrumentation-stderr.txt'
    $hasStandardInput = $PSBoundParameters.ContainsKey('StandardInputText')
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $hasStandardInput
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($name in @($ClearInheritedEnvironmentVariableNames | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        $environmentProperty = $startInfo.PSObject.Properties['Environment']
        if ($null -ne $environmentProperty) {
            [void]$startInfo.Environment.Remove([string]$name)
        } else {
            [void]$startInfo.EnvironmentVariables.Remove([string]$name)
        }
    }
    Set-P5EProcessStartInfoArguments -StartInfo $startInfo -ArgumentList $ArgumentList

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $launchCount = 0
    $timedOut = $false
    $exitCode = $null
    $launchErrorClass = ''
    $inputWriteCompleted = -not $hasStandardInput
    $inputWriteErrorClass = ''
    $stdoutCapture = [pscustomobject]@{ Completed = $true; Text = ''; ErrorClass = '' }
    $stderrCapture = [pscustomobject]@{ Completed = $true; Text = ''; ErrorClass = '' }
    try {
        if (-not $process.Start()) { throw 'PROCESS_START_RETURNED_FALSE' }
        $launchCount = 1
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if ($hasStandardInput) {
            try {
                $inputBytes = [Text.UTF8Encoding]::new($false).GetBytes($StandardInputText)
                if ($inputBytes.Length -gt 0) {
                    $process.StandardInput.BaseStream.Write($inputBytes, 0, $inputBytes.Length)
                    $process.StandardInput.BaseStream.Flush()
                }
                $process.StandardInput.Close()
                $inputWriteCompleted = $true
            } catch {
                $inputWriteErrorClass = $_.Exception.GetType().Name
                try { $process.StandardInput.Close() } catch { }
                try { if (-not $process.HasExited) { $process.Kill($true) } } catch { try { $process.Kill() } catch { } }
            }
        }
        if (-not $process.WaitForExit([int]$TimeoutMilliseconds)) {
            $timedOut = $true
            try { $process.Kill($true) } catch { try { $process.Kill() } catch { } }
            try { $process.WaitForExit(5000) | Out-Null } catch { }
        } else {
            $process.WaitForExit()
        }
        $stdoutCapture = Get-P5EProcessTaskTextBounded -Task $stdoutTask
        $stderrCapture = Get-P5EProcessTaskTextBounded -Task $stderrTask
        if ($process.HasExited) { $exitCode = $process.ExitCode }
    } catch {
        if ($launchCount -eq 0) { $launchErrorClass = $_.Exception.GetType().Name }
        else { $inputWriteErrorClass = if ([string]::IsNullOrEmpty($inputWriteErrorClass)) { $_.Exception.GetType().Name } else { $inputWriteErrorClass } }
    } finally {
        if ($process -ne $null) { $process.Dispose() }
    }
    $safeStdout = if ($stdoutCapture.Completed) {
        Protect-P5ECaptureText -Text ([string]$stdoutCapture.Text) -SensitiveValues $SensitiveValues
    } else {
        [pscustomobject]@{ Text = 'P5E_CAPTURE_NOT_BOUNDED'; Violation = $false }
    }
    $safeStderr = if ($stderrCapture.Completed) {
        Protect-P5ECaptureText -Text ([string]$stderrCapture.Text) -SensitiveValues $SensitiveValues
    } else {
        [pscustomobject]@{ Text = 'P5E_CAPTURE_NOT_BOUNDED'; Violation = $false }
    }
    $captureBounded = [bool]($stdoutCapture.Completed -and $stderrCapture.Completed)
    $redactionViolation = $safeStdout.Violation -or $safeStderr.Violation
    Write-P5EUtf8NoBom -Path $stdoutPath -Text $safeStdout.Text
    Write-P5EUtf8NoBom -Path $stderrPath -Text $safeStderr.Text
    $outcome = if ($launchCount -eq 0) { 'FAILED_BEFORE_LAUNCH' }
        elseif (-not $inputWriteCompleted) { 'INPUT_NOT_DELIVERED' }
        elseif ($timedOut) { 'TIMEOUT' }
        elseif (-not $captureBounded) { 'CAPTURE_NOT_BOUNDED' }
        elseif ($null -ne $exitCode -and $exitCode -eq 0) { 'PROCESS_EXITED_ZERO' }
        else { 'PROCESS_EXITED_NONZERO' }
    return [pscustomobject]@{
        Outcome = $outcome
        LaunchCount = $launchCount
        DispatchCount = $launchCount
        TimedOut = $timedOut
        ExitCode = $exitCode
        LaunchErrorClass = $launchErrorClass
        InputWriteCompleted = $inputWriteCompleted
        InputWriteErrorClass = $inputWriteErrorClass
        CaptureBounded = $captureBounded
        StdoutCaptureErrorClass = $stdoutCapture.ErrorClass
        StderrCaptureErrorClass = $stderrCapture.ErrorClass
        RedactionViolation = $redactionViolation
        StdoutPath = $stdoutPath
        StderrPath = $stderrPath
    }
}

function Invoke-P5EReadOnlyProcess {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][long]$TimeoutMilliseconds,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @()
    )
    $script:P5EReadOnlyCommandCount = [long]$script:P5EReadOnlyCommandCount + 1L
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    Set-P5EProcessStartInfoArguments -StartInfo $startInfo -ArgumentList $ArgumentList
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $launchCount = 0
    $timedOut = $false
    $exitCode = $null
    $launchErrorClass = ''
    $stdout = ''
    $stderr = ''
    $stdoutTask = $null
    $stderrTask = $null
    try {
        if (-not $process.Start()) { throw 'PROCESS_START_RETURNED_FALSE' }
        $launchCount = 1
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit([int]$TimeoutMilliseconds)) {
            $timedOut = $true
            try { $process.Kill($true) } catch { try { $process.Kill() } catch { } }
            try { $process.WaitForExit(5000) | Out-Null } catch { }
        } else { $process.WaitForExit() }
        if ($stdoutTask -ne $null) { $stdout = $stdoutTask.GetAwaiter().GetResult() }
        if ($stderrTask -ne $null) { $stderr = $stderrTask.GetAwaiter().GetResult() }
        if ($process.HasExited) { $exitCode = $process.ExitCode }
    } catch {
        if ($launchCount -eq 0) { $launchErrorClass = $_.Exception.GetType().Name }
        else { $stderr = $_.Exception.GetType().Name }
    } finally { $process.Dispose() }
    $safeStdout = Protect-P5ECaptureText -Text ([string]$stdout) -SensitiveValues $SensitiveValues
    $safeStderr = Protect-P5ECaptureText -Text ([string]$stderr) -SensitiveValues $SensitiveValues
    $outputTooLarge = ([string]$safeStdout.Text).Length -gt 4194304 -or ([string]$safeStderr.Text).Length -gt 1048576
    if ($outputTooLarge) { $safeStdout = Protect-P5ECaptureText -Text 'P5E_COLLECTOR_OUTPUT_TOO_LARGE' }
    return [pscustomobject]@{
        Outcome = if ($launchCount -eq 0) { 'FAILED_BEFORE_LAUNCH' } elseif ($timedOut) { 'TIMEOUT' } elseif ($null -eq $exitCode) { 'PROCESS_EXIT_UNKNOWN' } elseif ($exitCode -eq 0) { 'PROCESS_EXITED_ZERO' } else { 'PROCESS_EXITED_NONZERO' }
        LaunchCount = $launchCount
        ExitCode = $exitCode
        TimedOut = $timedOut
        LaunchErrorClass = $launchErrorClass
        RedactionViolation = [bool]($safeStdout.Violation -or $safeStderr.Violation)
        OutputTooLarge = $outputTooLarge
        Stdout = [string]$safeStdout.Text
        Stderr = [string]$safeStderr.Text
    }
}

function Invoke-P5EAdbReadOnly {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$Operation,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$Arguments,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @(),
        [long]$TimeoutMilliseconds = 30000L
    )
    if ($SerialValue -cne $script:P5ESerial) { throw 'P5E_COLLECTOR_SERIAL_MISMATCH' }
    $run = Invoke-P5EReadOnlyProcess -FilePath $AdbPath -ArgumentList (@('-s', $SerialValue) + $Arguments) `
        -TimeoutMilliseconds $TimeoutMilliseconds -SensitiveValues $SensitiveValues
    Add-P5ECollectorCommandRecord -Operation $Operation -Run $run
    if ($run.RedactionViolation) { throw ('P5E_COLLECTOR_REDACTION_FAILURE:' + $Operation) }
    if ($run.OutputTooLarge) { throw ('P5E_COLLECTOR_OUTPUT_TOO_LARGE:' + $Operation) }
    if ($run.LaunchCount -eq 0) { throw ('P5E_COLLECTOR_ADB_FAILED_BEFORE_LAUNCH:' + $Operation) }
    if ($run.TimedOut) { throw ('P5E_COLLECTOR_TIMEOUT:' + $Operation) }
    if ($null -eq $run.ExitCode -or $run.ExitCode -ne 0) { throw ('P5E_COLLECTOR_ADB_NONZERO:' + $Operation) }
    return $run
}

function Invoke-P5EAdbShellReadOnly {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$Operation,
        [Parameter(Mandatory = $true)][string[]]$RemoteTokens,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @(),
        [long]$TimeoutMilliseconds = 30000L
    )
    $quotedTokens = @($RemoteTokens | ForEach-Object { ConvertTo-P5EAndroidShellArgument -Value ([string]$_) })
    return Invoke-P5EAdbReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation $Operation `
        -Arguments (@('shell') + $quotedTokens) -SensitiveValues $SensitiveValues `
        -TimeoutMilliseconds $TimeoutMilliseconds
}

function Get-P5ESeparatedRow {
    param(
        [Parameter(Mandatory = $true)][string]$Output,
        [Parameter(Mandatory = $true)][int]$ColumnCount,
        [Parameter(Mandatory = $true)][string]$Operation
    )
    $lines = @($Output -split "`r?`n" | Where-Object { $_ -ne '' })
    if ($lines.Count -ne 1) { throw ('P5E_COLLECTOR_SQL_ROW_COUNT:' + $Operation) }
    $columns = $lines[0].Split([char]9)
    if ($columns.Count -ne $ColumnCount) { throw ('P5E_COLLECTOR_SQL_COLUMN_COUNT:' + $Operation) }
    return ,@($columns | ForEach-Object { if ($_ -ceq 'NULL') { $null } else { $_ } })
}

function Get-P5ESeparatedRows {
    param(
        [Parameter(Mandatory = $true)][string]$Output,
        [Parameter(Mandatory = $true)][int]$ColumnCount,
        [Parameter(Mandatory = $true)][string]$Operation
    )
    $lines = @($Output -split "`r?`n" | Where-Object { $_ -ne '' })
    $rows = [System.Collections.Generic.List[object]]::new()
    foreach ($line in $lines) {
        $columns = $line.Split([char]9)
        if ($columns.Count -ne $ColumnCount) { throw ('P5E_COLLECTOR_SQL_COLUMN_COUNT:' + $Operation) }
        [void]$rows.Add(@($columns | ForEach-Object { if ($_ -ceq 'NULL') { $null } else { $_ } }))
    }
    return $rows.ToArray()
}

function Invoke-P5ESqliteReadOnlyRows {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$Operation,
        [Parameter(Mandatory = $true)][string]$Query,
        [Parameter(Mandatory = $true)][int]$ColumnCount
    )
    $readOnlyQuery = 'PRAGMA foreign_keys=ON;BEGIN;' + $Query + ';COMMIT;'
    $run = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation $Operation `
        -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sqlite3', '-readonly', '-batch', '-noheader',
            '-separator', $script:P5ECollectorSeparator, $script:P5EDatabaseDevicePath, $readOnlyQuery) `
        -TimeoutMilliseconds 30000L
    return [pscustomobject]@{ Run = $run; Rows = @(Get-P5ESeparatedRows -Output $run.Stdout -ColumnCount $ColumnCount -Operation $Operation) }
}

function Invoke-P5ESqliteReadOnlyRow {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$Operation,
        [Parameter(Mandatory = $true)][string]$Query,
        [Parameter(Mandatory = $true)][int]$ColumnCount
    )
    $readOnlyQuery = 'PRAGMA foreign_keys=ON;BEGIN;' + $Query + ';COMMIT;'
    $run = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation $Operation `
        -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sqlite3', '-readonly', '-batch', '-noheader',
            '-separator', $script:P5ECollectorSeparator, $script:P5EDatabaseDevicePath, $readOnlyQuery) `
        -TimeoutMilliseconds 30000L
    return [pscustomobject]@{ Run = $run; Columns = @(Get-P5ESeparatedRow -Output $run.Stdout -ColumnCount $ColumnCount -Operation $Operation) }
}

function Get-P5ELocalDatabaseFileHashState {
    param([Parameter(Mandatory = $true)][string]$DatabasePath)
    if (-not (Test-Path -LiteralPath $DatabasePath -PathType Leaf)) {
        throw 'P5E_LOCAL_SQLITE_DATABASE_MISSING'
    }
    return [ordered]@{
        database = [ordered]@{ present = $true; sha256 = Get-P5ESha256 -Path $DatabasePath }
        wal = [ordered]@{ present = $false; sha256 = '' }
        shm = [ordered]@{ present = $false; sha256 = '' }
        settings = [ordered]@{ present = $true; sha256 = 'offline-fixture-settings-only' }
    }
}

function Invoke-P5ELocalSqliteReadOnly {
    param(
        [Parameter(Mandatory = $true)][string]$DatabasePath,
        [Parameter(Mandatory = $true)][string]$Query
    )
    $bridgePath = Join-Path (Get-P5ERepoRoot) 'docs\P5E_SQLITE_BRIDGE.py'
    if (-not (Test-Path -LiteralPath $bridgePath -PathType Leaf)) { throw 'P5E_LOCAL_SQLITE_BRIDGE_MISSING' }
    $bridgeRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-sqlite-bridge-' + [Guid]::NewGuid().ToString('N'))
    [void](New-Item -ItemType Directory -Path $bridgeRoot -Force)
    $queryPath = Join-Path $bridgeRoot 'query.sql'
    Write-P5EUtf8NoBom -Path $queryPath -Text $Query
    $python = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($null -eq $python) { $python = Get-Command py -ErrorAction SilentlyContinue }
    if ($null -eq $python) { throw 'P5E_LOCAL_SQLITE_PYTHON_MISSING' }
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = [string]$python.Source
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    Set-P5EProcessStartInfoArguments -StartInfo $startInfo -ArgumentList @(
        '-3', $bridgePath, '--database', $DatabasePath, '--query', $queryPath)
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) { throw 'P5E_LOCAL_SQLITE_BRIDGE_START_FAILED' }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()
        $exitCode = $process.ExitCode
    } finally {
        $process.Dispose()
    }
    if ($exitCode -ne 0) {
        throw ('P5E_LOCAL_SQLITE_BRIDGE_FAILED:' + [string]$exitCode + ':' + [string]$stderr)
    }
    return [pscustomobject]@{
        Stdout = [string]$stdout
        Stderr = [string]$stderr
        ExitCode = [int]$exitCode
        QueryPath = $queryPath
        BridgePath = Get-P5ECanonicalPath -Path $bridgePath
    }
}

function Invoke-P5EAdbPresenceReadOnly {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$DevicePath,
        [Parameter(Mandatory = $true)][string]$Operation
    )
    $quotedPath = ConvertTo-P5EAndroidShellArgument -Value $DevicePath
    $remote = @('run-as', $script:P5ETargetPackage, 'sh', '-c', "test -e $quotedPath")
    $quotedTokens = @($remote | ForEach-Object { ConvertTo-P5EAndroidShellArgument -Value ([string]$_) })
    $run = Invoke-P5EReadOnlyProcess -FilePath $AdbPath -ArgumentList (@('-s', $SerialValue, 'shell') + $quotedTokens) `
        -TimeoutMilliseconds 30000L
    Add-P5ECollectorCommandRecord -Operation $Operation -Run $run
    if ($run.RedactionViolation -or $run.OutputTooLarge) { throw 'P5E_COLLECTOR_PRESENCE_CAPTURE_INVALID' }
    if ($run.LaunchCount -eq 0) { throw 'P5E_COLLECTOR_ADB_FAILED_BEFORE_LAUNCH:presence' }
    if ($run.TimedOut) { throw 'P5E_COLLECTOR_TIMEOUT:presence' }
    if ($null -eq $run.ExitCode) { throw 'P5E_COLLECTOR_PRESENCE_UNKNOWN' }
    if ($run.ExitCode -eq 0) { return $true }
    if ($run.ExitCode -eq 1) { return $false }
    throw 'P5E_COLLECTOR_PRESENCE_QUERY_FAILED'
}

function Get-P5EHexBytes {
    param([Parameter(Mandatory = $true)][string]$Hex, [Parameter(Mandatory = $true)][string]$Name)
    if ($Hex -notmatch '^(?:[0-9A-Fa-f]{2})+$') { throw ('P5E_COLLECTOR_HEX_INVALID:' + $Name) }
    $bytes = [byte[]]::new($Hex.Length / 2)
    for ($index = 0; $index -lt $bytes.Length; $index++) { $bytes[$index] = [Convert]::ToByte($Hex.Substring($index * 2, 2), 16) }
    return $bytes
}

function Get-P5ETransactionSourceEvidence {
    $repoRoot = Get-P5ERepoRoot
    $sourcePath = Join-Path $repoRoot $script:P5ETransactionSourcePath
    $testPath = Join-Path $repoRoot $script:P5ETransactionTestPath
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf) -or
            -not (Test-Path -LiteralPath $testPath -PathType Leaf)) {
        throw 'P5E_COLLECTOR_TRANSACTION_SOURCE_MISSING'
    }
    $sourceSha256 = Get-P5ESha256 -Path $sourcePath
    $testSha256 = Get-P5ESha256 -Path $testPath
    if ($sourceSha256 -cne $script:P5ETransactionSourceSha256 -or
            $testSha256 -cne $script:P5ETransactionTestSha256) {
        throw 'P5E_COLLECTOR_TRANSACTION_SOURCE_HASH_MISMATCH'
    }
    return [ordered]@{
        sourcePath = Get-P5ECanonicalPath -Path $sourcePath
        sourceSha256 = $sourceSha256
        testPath = Get-P5ECanonicalPath -Path $testPath
        testSha256 = $testSha256
        status = 'PINNED_SOURCE_TRANSACTION_CONTRACT'
    }
}

function Get-P5EApkSignerDigest {
    param(
        [Parameter(Mandatory = $true)][string]$ApkSigner,
        [Parameter(Mandatory = $true)][string]$ApkPath,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory
    )
    $run = Invoke-P5EReadOnlyProcess -FilePath $ApkSigner -ArgumentList @('verify', '--print-certs', $ApkPath) `
        -TimeoutMilliseconds 30000L
    Add-P5ECollectorCommandRecord -Operation ('apk-signer-verify-' + $Name) -Run $run
    if ($run.RedactionViolation -or $run.OutputTooLarge) { throw ('P5E_COLLECTOR_APKSIGNER_CAPTURE_INVALID:' + $Name) }
    if ($run.LaunchCount -eq 0) { throw ('P5E_COLLECTOR_APKSIGNER_UNAVAILABLE:' + $Name) }
    if ($run.TimedOut -or $null -eq $run.ExitCode -or $run.ExitCode -ne 0) { throw ('P5E_COLLECTOR_APKSIGNER_FAILED:' + $Name) }
    $match = [regex]::Match(($run.Stdout + "`n" + $run.Stderr), '(?im)certificate\s+SHA-256\s+digest:\s*([0-9a-f:]{64,95})')
    if (-not $match.Success) { throw ('P5E_COLLECTOR_CERTIFICATE_DIGEST_MISSING:' + $Name) }
    return $match.Groups[1].Value.Replace(':', '').ToLowerInvariant()
}

function Get-P5EPackageReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][string]$PackageName,
        [Parameter(Mandatory = $true)][string]$ExpectedApkSha256,
        [Parameter(Mandatory = $true)][string]$LocalFileName,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$ApkSigner,
        [int]$ExpectedVersionCode = -1,
        [string]$ExpectedVersion = ''
    )
    $pathRun = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation ('pm-path-' + $Name) `
        -RemoteTokens @('pm', 'path', $PackageName) -TimeoutMilliseconds 30000L
    $pathLines = @($pathRun.Stdout -split "`r?`n" | Where-Object { $_ -ne '' })
    if ($pathLines.Count -ne 1 -or $pathLines[0] -notmatch '^package:(/data/app/[^\r\n ]+\.apk)$') {
        throw ('P5E_COLLECTOR_PACKAGE_PATH_INVALID:' + $Name)
    }
    $devicePath = $Matches[1]
    $localPath = Join-Path (Get-P5ECanonicalPath -Path $EvidenceDirectory) $LocalFileName
    if (-not (Test-Path -LiteralPath $localPath -PathType Leaf)) {
        $pull = Invoke-P5EAdbReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation ('pull-apk-' + $Name) `
            -Arguments @('pull', $devicePath, $localPath) -TimeoutMilliseconds 60000L
        if ($pull.ExitCode -ne 0) { throw ('P5E_COLLECTOR_PACKAGE_PULL_FAILED:' + $Name) }
    }
    $apkHash = Get-P5ESha256 -Path $localPath
    if ($apkHash -cne $ExpectedApkSha256.ToLowerInvariant()) { throw ('P5E_COLLECTOR_PACKAGE_APK_HASH_MISMATCH:' + $Name) }
    $certificateHash = Get-P5EApkSignerDigest -ApkSigner $ApkSigner -ApkPath $localPath -Name $Name -EvidenceDirectory $EvidenceDirectory
    if ($certificateHash -cne $script:P5ECertificateSha256) { throw ('P5E_COLLECTOR_PACKAGE_CERTIFICATE_MISMATCH:' + $Name) }
    $versionCode = $null
    $version = ''
    if ($ExpectedVersionCode -ge 0 -or -not [string]::IsNullOrWhiteSpace($ExpectedVersion)) {
        $dump = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation ('package-dump-' + $Name) `
            -RemoteTokens @('dumpsys', 'package', $PackageName) -TimeoutMilliseconds 30000L
        $versionMatch = [regex]::Match($dump.Stdout, '(?m)\bversionName=([^\s]+)')
        $codeMatch = [regex]::Match($dump.Stdout, '(?m)\bversionCode=(\d+)')
        if ($versionMatch.Success) { $version = $versionMatch.Groups[1].Value }
        if ($codeMatch.Success) { $versionCode = [long]$codeMatch.Groups[1].Value }
        if ($ExpectedVersionCode -ge 0 -and $versionCode -ne [long]$ExpectedVersionCode) { throw ('P5E_COLLECTOR_PACKAGE_VERSION_CODE_MISMATCH:' + $Name) }
        if (-not [string]::IsNullOrWhiteSpace($ExpectedVersion) -and $version -cne $ExpectedVersion) { throw ('P5E_COLLECTOR_PACKAGE_VERSION_MISMATCH:' + $Name) }
    }
    return [ordered]@{
        package = $PackageName
        deviceApkPath = $devicePath
        localApkPath = Get-P5ECanonicalPath -Path $localPath
        apkSha256 = $apkHash
        apkByteLength = [long](Get-Item -LiteralPath $localPath).Length
        certificateSha256 = $certificateHash
        version = $version
        versionCode = $versionCode
    }
}

function Get-P5EDatabaseSnapshot {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][long]$ObservedAtMillis
    )
    $mainHashRun = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'database-main-hash' `
        -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sha256sum', $script:P5EDatabaseDevicePath) -TimeoutMilliseconds 30000L
    $hashMatch = [regex]::Match($mainHashRun.Stdout, '(?im)^([0-9a-f]{64})\s+')
    if (-not $hashMatch.Success) { throw 'P5E_COLLECTOR_DATABASE_MAIN_HASH_INVALID' }
    $mainHash = $hashMatch.Groups[1].Value.ToLowerInvariant()
    $walPath = $script:P5EDatabaseDevicePath + '-wal'
    $shmPath = $script:P5EDatabaseDevicePath + '-shm'
    $walPresent = Invoke-P5EAdbPresenceReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -DevicePath $walPath -Operation 'database-wal-presence'
    $shmPresent = Invoke-P5EAdbPresenceReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -DevicePath $shmPath -Operation 'database-shm-presence'
    $walHash = ''
    $shmHash = ''
    foreach ($item in @([pscustomobject]@{ Name = 'wal'; Path = $walPath; Present = $walPresent },
            [pscustomobject]@{ Name = 'shm'; Path = $shmPath; Present = $shmPresent })) {
        if ($item.Present) {
            $run = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation ('database-' + $item.Name + '-hash') `
                -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sha256sum', $item.Path) -TimeoutMilliseconds 30000L
            $match = [regex]::Match($run.Stdout, '(?im)^([0-9a-f]{64})\s+')
            if (-not $match.Success) { throw ('P5E_COLLECTOR_DATABASE_' + $item.Name.ToUpperInvariant() + '_HASH_INVALID') }
            if ($item.Name -eq 'wal') { $walHash = $match.Groups[1].Value.ToLowerInvariant() } else { $shmHash = $match.Groups[1].Value.ToLowerInvariant() }
        }
    }
    $integrityRun = Invoke-P5ESqliteReadOnlyRow -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'database-integrity' `
        -Query 'PRAGMA integrity_check' -ColumnCount 1
    $integrity = [string]$integrityRun.Columns[0]
    if ($integrity -cne 'ok') { throw 'P5E_COLLECTOR_DATABASE_INTEGRITY_FAILED' }
    $fkRun = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'database-foreign-key-check' `
        -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sqlite3', '-readonly', '-batch', '-noheader',
            $script:P5EDatabaseDevicePath, 'PRAGMA foreign_key_check;') -TimeoutMilliseconds 30000L
    $fkLines = @($fkRun.Stdout -split "`r?`n" | Where-Object { $_ -ne '' })
    if ($fkLines.Count -ne 0) { throw 'P5E_COLLECTOR_DATABASE_FOREIGN_KEY_FAILED' }
    return [ordered]@{
        observedAtMillis = $ObservedAtMillis
        snapshotMode = 'WAL_AWARE_READ_TRANSACTION'
        databaseSha256 = $mainHash
        databaseWalSha256 = $walHash
        databaseShmSha256 = $shmHash
        walPresent = [bool]$walPresent
        shmPresent = [bool]$shmPresent
        databaseSchemaVersion = $script:P5EDatabaseSchemaVersion
        integrityCheck = 'ok'
        foreignKeyViolations = 0
    }
}

function ConvertTo-P5ESqlLiteral {
    param([Parameter(Mandatory = $true)][string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

function Get-P5ETextFromHex {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Hex, [Parameter(Mandatory = $true)][string]$Name)
    if ([string]::IsNullOrWhiteSpace($Hex)) { return '' }
    $bytes = Get-P5EHexBytes -Hex $Hex -Name $Name
    try { return [Text.UTF8Encoding]::new($false, $true).GetString($bytes) }
    catch { throw ('P5E_COLLECTOR_UTF8_INVALID:' + $Name) }
}

function Get-P5EBindingReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue
    )
    $binding = Invoke-P5ESqliteReadOnlyRow -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'binding-tuple' `
        -Query ("SELECT project_row_id,attempt_request_selector,binding_identity,run_declaration_identity," +
            "canonical_pack_hash,manifest_fingerprint,canonical_profile_hash,compatibility_evaluation_id," +
            "source_mode,phase_identity,execution_allowed,certification_state FROM editorial_p4_bindings WHERE " +
            "binding_identity=" + (ConvertTo-P5ESqlLiteral $script:P5EBindingIdentity) +
            " AND project_row_id=" + [string]$script:P5EProjectRowId) -ColumnCount 12
    $c = $binding.Columns
    if ([long]$c[0] -ne $script:P5EProjectRowId -or [string]$c[2] -cne $script:P5EBindingIdentity -or
            [string]$c[3] -cne $script:P5ERunDeclarationIdentity -or [string]$c[4] -cne $script:P5EPackHash -or
            [string]$c[5] -cne $script:P5EExpectedManifestFingerprint -or [string]$c[6] -cne $script:P5EProfileHash -or
            [string]$c[7] -cne $script:P5EEvaluationId -or [string]$c[8] -cne 'NORMAL_FOUR_SOURCE' -or
            [string]$c[9] -cne $script:P5EBindingPhaseIdentity -or [int]$c[10] -ne 0 -or [string]$c[11] -cne 'NOT_CERTIFIED') {
        throw 'P5E_COLLECTOR_BINDING_TUPLE_MISMATCH'
    }
    $inputQuery = "SELECT role,byte_length,sha256,encoding,schema_status,ordinal FROM editorial_p4_binding_inputs WHERE binding_identity=" +
        (ConvertTo-P5ESqlLiteral $script:P5EBindingIdentity) + ' ORDER BY role,ordinal'
    $inputs = Invoke-P5ESqliteReadOnlyRows -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'binding-inputs' `
        -Query $inputQuery -ColumnCount 6
    if ($inputs.Count -ne 4) { throw 'P5E_COLLECTOR_BINDING_INPUT_COUNT_INVALID' }
    $expectedSources = [ordered]@{
        RAW = @($script:P5ERawSourceBytes, $script:P5ERawSourceSha256, 'VISIBLE')
        GLOSSARY = @($script:P5EGlossarySourceBytes, $script:P5EGlossarySourceSha256, 'VISIBLE')
        DRAFT = @($script:P5EDraftSourceBytes, $script:P5EDraftSourceSha256, 'HIDDEN')
        PRONOUN = @($script:P5EPronounSourceBytes, $script:P5EPronounSourceSha256, 'HIDDEN')
    }
    $sources = [System.Collections.Generic.List[object]]::new()
    $roles = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($row in $inputs) {
        $role = [string]$row[0]
        if (-not $roles.Add($role) -or -not $expectedSources.Contains($role) -or
                [long]$row[1] -ne [long]$expectedSources[$role][0] -or
                [string]$row[2] -cne [string]$expectedSources[$role][1] -or
                [string]::IsNullOrWhiteSpace([string]$row[3]) -or [string]::IsNullOrWhiteSpace([string]$row[4])) {
            throw ('P5E_COLLECTOR_SOURCE_INPUT_MISMATCH:' + $role)
        }
        [void]$sources.Add([ordered]@{ role = $role; visibility = $expectedSources[$role][2]; byteLength = [long]$row[1]; sha256 = ([string]$row[2]).ToLowerInvariant(); encoding = [string]$row[3]; schemaStatus = [string]$row[4]; ordinal = [long]$row[5] })
    }
    return [ordered]@{
        projectRowId = [long]$c[0]
        selector = [string]$c[1]
        bindingIdentity = [string]$c[2]
        runDeclarationIdentity = [string]$c[3]
        packHash = [string]$c[4]
        manifestFingerprint = [string]$c[5]
        profileHash = [string]$c[6]
        evaluationId = [string]$c[7]
        sourceMode = [string]$c[8]
        phaseIdentity = [string]$c[9]
        sources = $sources.ToArray()
    }
}

function Get-P5ELineageReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue
    )
    $attempt = ConvertTo-P5ESqlLiteral $script:P5EAttemptIdentity
    $binding = ConvertTo-P5ESqlLiteral $script:P5EBindingIdentity
    $run = ConvertTo-P5ESqlLiteral $script:P5ERunDeclarationIdentity
    $query = "SELECT " +
        "(SELECT COUNT(*) FROM editorial_p5c_attempts WHERE attempt_identity=$attempt)," +
        "(SELECT COUNT(*) FROM editorial_p5d_authorization_receipts WHERE attempt_identity=$attempt)," +
        "(SELECT COUNT(*) FROM editorial_p5d_network_lifecycle WHERE attempt_identity=$attempt)," +
        "(SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=$binding AND chapter_key='001' AND phase='L1_RAW_DISCOVERY')," +
        "(SELECT COUNT(*) FROM editorial_p5d_authorization_receipts ar JOIN editorial_p5c_attempts a ON ar.attempt_identity=a.attempt_identity WHERE a.binding_identity=$binding AND a.chapter_key='001' AND a.phase='L1_RAW_DISCOVERY')," +
        "(SELECT COUNT(*) FROM editorial_p5d_network_lifecycle l JOIN editorial_p5c_attempts a ON l.attempt_identity=a.attempt_identity WHERE a.binding_identity=$binding AND a.chapter_key='001' AND a.phase='L1_RAW_DISCOVERY')," +
        "(SELECT COUNT(*) FROM editorial_p5d_reconciliation r JOIN editorial_p5c_attempts a ON r.attempt_identity=a.attempt_identity WHERE a.binding_identity=$binding)," +
        "(SELECT COUNT(*) FROM editorial_p5d_reconciliation_history h JOIN editorial_p5c_attempts a ON h.attempt_identity=a.attempt_identity WHERE a.binding_identity=$binding)," +
        "(SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=$binding AND report_bytes IS NOT NULL AND receipt_bytes IS NOT NULL)," +
        "(SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=$binding AND (report_bytes IS NOT NULL OR receipt_bytes IS NOT NULL) AND NOT (report_bytes IS NOT NULL AND receipt_bytes IS NOT NULL))," +
        "(SELECT COUNT(*) FROM editorial_p5c_attempts WHERE status='CLAIMED' AND attempt_identity<>$attempt)," +
        "(SELECT COUNT(*) FROM editorial_p5c_attempts)," +
        "(SELECT COUNT(*) FROM editorial_p5d_authorization_receipts)," +
        "(SELECT COUNT(*) FROM editorial_p5d_network_lifecycle)," +
        "(SELECT COUNT(*) FROM editorial_p5d_reconciliation)," +
        "(SELECT COUNT(*) FROM editorial_p5d_reconciliation_history)"
    $row = Invoke-P5ESqliteReadOnlyRow -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'lineage-counts' -Query $query -ColumnCount 16
    $values = @($row.Columns | ForEach-Object { [long]$_ })
    return [ordered]@{
        exactAttempt = $values[0]; exactAuthorization = $values[1]; exactLifecycle = $values[2]
        attempts = $values[3]; authorizationReceipts = $values[4]; lifecycle = $values[5]
        reconciliation = $values[6]; reconciliationHistory = $values[7]; reportOrReceipt = $values[8]
        partialArtifactPair = $values[9]; activeCompetingWriter = $values[10]
        globalCounts = [ordered]@{ attempts = $values[11]; authorizationReceipts = $values[12]; lifecycle = $values[13]; reconciliation = $values[14]; reconciliationHistory = $values[15] }
    }
}

function Get-P5EAttemptReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue
    )
    $attempt = ConvertTo-P5ESqlLiteral $script:P5EAttemptIdentity
    $query = "SELECT attempt_identity,request_identity,binding_identity,run_declaration_identity,chapter_key,phase," +
        "predecessor_identity,request_envelope_hash,provider,model,status,response_identity," +
        "length(report_bytes),hex(report_bytes),length(receipt_bytes),hex(receipt_bytes),hex(CAST(metrics_json AS BLOB))," +
        "created_at,updated_at,recovery_reason_code FROM editorial_p5c_attempts WHERE attempt_identity=$attempt"
    $result = Invoke-P5ESqliteReadOnlyRows -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'attempt-row' -Query $query -ColumnCount 20
    if ($result.Count -ne 1) { throw 'P5E_COLLECTOR_ATTEMPT_ROW_MISSING_OR_DUPLICATE' }
    $c = $result[0]
    $metricsText = Get-P5ETextFromHex -Hex ([string]$c[16]) -Name 'metrics-json'
    $metrics = $null
    if (-not [string]::IsNullOrWhiteSpace($metricsText)) {
        try { $metrics = $metricsText | ConvertFrom-Json } catch { throw 'P5E_COLLECTOR_METRICS_JSON_INVALID' }
    }
    return [ordered]@{
        attemptIdentity = [string]$c[0]; requestIdentity = [string]$c[1]; bindingIdentity = [string]$c[2]; runDeclarationIdentity = [string]$c[3]
        chapterKey = [string]$c[4]; phase = [string]$c[5]; predecessorIdentity = [string]$c[6]; requestEnvelopeHash = [string]$c[7]
        provider = [string]$c[8]; model = [string]$c[9]; status = [string]$c[10]; responseIdentity = [string]$c[11]
        reportByteLength = if ($null -eq $c[12]) { $null } else { [long]$c[12] }; reportHex = [string]$c[13]
        receiptByteLength = if ($null -eq $c[14]) { $null } else { [long]$c[14] }; receiptHex = [string]$c[15]
        metrics = $metrics; createdAtMillis = [long]$c[17]; updatedAtMillis = [long]$c[18]; recoveryReasonCode = [string]$c[19]
    }
}

function Get-P5EAuthorizationReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue
    )
    $attempt = ConvertTo-P5ESqlLiteral $script:P5EAttemptIdentity
    $query = "SELECT authorization_id_hash,exact_phase,attempt_identity,request_identity,binding_identity,run_declaration_identity," +
        "chapter_key,provider,model,endpoint_account_fingerprint,issued_at,expires_at,consumed_at,maximum_primary_calls," +
        "maximum_schema_repair_calls,maximum_network_retries,maximum_input_tokens,maximum_output_tokens,maximum_total_tokens," +
        "maximum_total_cost,maximum_execution_time_ms,consumption_result,consumed_attempt_identity FROM " +
        "editorial_p5d_authorization_receipts WHERE attempt_identity=$attempt"
    $result = Invoke-P5ESqliteReadOnlyRows -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'authorization-row' -Query $query -ColumnCount 23
    if ($result.Count -ne 1) { throw 'P5E_COLLECTOR_AUTHORIZATION_ROW_MISSING_OR_DUPLICATE' }
    $c = $result[0]
    return [ordered]@{
        authorizationIdHash = [string]$c[0]; exactPhase = [string]$c[1]; attemptIdentity = [string]$c[2]; requestIdentity = [string]$c[3]
        bindingIdentity = [string]$c[4]; runDeclarationIdentity = [string]$c[5]; chapterKey = [string]$c[6]; provider = [string]$c[7]
        model = [string]$c[8]; endpointAccountFingerprint = [string]$c[9]; issuedAtMillis = [long]$c[10]; expiresAtMillis = [long]$c[11]
        consumedAtMillis = [long]$c[12]; maximumPrimaryCalls = [long]$c[13]; maximumSchemaRepairCalls = [long]$c[14]
        maximumNetworkRetries = [long]$c[15]; maximumInputTokens = [long]$c[16]; maximumOutputTokens = [long]$c[17]
        maximumTotalTokens = [long]$c[18]; maximumTotalCost = [string]$c[19]; maximumExecutionTimeMillis = [long]$c[20]
        consumptionResult = [string]$c[21]; consumedAttemptIdentity = [string]$c[22]
    }
}

function Get-P5ELifecycleReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue
    )
    $attempt = ConvertTo-P5ESqlLiteral $script:P5EAttemptIdentity
    $query = "SELECT attempt_identity,stage,request_body_bytes,response_body_bytes,http_status,response_content_type,exception_class," +
        "elapsed_ms,generation_id,provider_response_id,cancellation_source,updated_at FROM editorial_p5d_network_lifecycle WHERE attempt_identity=$attempt"
    $result = Invoke-P5ESqliteReadOnlyRows -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'lifecycle-row' -Query $query -ColumnCount 12
    if ($result.Count -ne 1) { throw 'P5E_COLLECTOR_LIFECYCLE_ROW_MISSING_OR_DUPLICATE' }
    $c = $result[0]
    return [ordered]@{
        attemptIdentity = [string]$c[0]; stage = [string]$c[1]; requestBodyBytes = [long]$c[2]; responseBodyBytes = [long]$c[3]
        httpStatus = [long]$c[4]; responseContentType = [string]$c[5]; exceptionClass = [string]$c[6]; elapsedMillis = [long]$c[7]
        generationId = [string]$c[8]; providerResponseId = [string]$c[9]; cancellationSource = [string]$c[10]; updatedAtMillis = [long]$c[11]
    }
}

function ConvertTo-P5ECollectorLong {
    param(
        [Parameter(Mandatory = $true)][string]$Value,
        [Parameter(Mandatory = $true)][string]$Name,
        [long]$Minimum = 0L
    )
    $parsed = 0L
    if (-not [long]::TryParse($Value, [Globalization.NumberStyles]::Integer,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$parsed) -or $parsed -lt $Minimum) {
        throw ('P5E_COLLECTOR_NUMERIC_FIELD_INVALID:' + $Name)
    }
    return $parsed
}

function Get-P5EDeviceFileHashState {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue
    )
    $paths = [ordered]@{
        database = $script:P5EDatabaseDevicePath
        wal = $script:P5EDatabaseDevicePath + '-wal'
        shm = $script:P5EDatabaseDevicePath + '-shm'
        settings = $script:P5ESettingsDevicePath
    }
    $state = [ordered]@{}
    foreach ($entry in $paths.GetEnumerator()) {
        $present = Invoke-P5EAdbPresenceReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -DevicePath $entry.Value `
            -Operation ('database-' + $entry.Key + '-presence')
        $hash = ''
        if ($present) {
            $run = Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation ('database-' + $entry.Key + '-sha256') -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sha256sum', $entry.Value) -TimeoutMilliseconds 30000L
            $match = [regex]::Match($run.Stdout, '(?im)^([0-9a-f]{64})\s+')
            if (-not $match.Success) { throw ('P5E_COLLECTOR_DATABASE_FILE_HASH_INVALID:' + $entry.Key) }
            $hash = $match.Groups[1].Value.ToLowerInvariant()
        }
        $state[$entry.Key] = [ordered]@{ path = $entry.Value; present = [bool]$present; sha256 = $hash }
    }
    return $state
}

function New-P5EConsistentDatabaseReadbackQuery {
    $attemptLiteral = ConvertTo-P5ESqlLiteral $script:P5EAttemptIdentity
    $bindingLiteral = ConvertTo-P5ESqlLiteral $script:P5EBindingIdentity
    $query = @"
PRAGMA foreign_keys=ON;
BEGIN;
SELECT 'SCHEMA' || char(9) || (SELECT user_version FROM pragma_user_version);
SELECT 'BINDING' || char(9) || project_row_id || char(9) || attempt_request_selector || char(9) ||
 binding_identity || char(9) || run_declaration_identity || char(9) || canonical_pack_hash || char(9) ||
 manifest_fingerprint || char(9) || canonical_profile_hash || char(9) || compatibility_evaluation_id ||
 char(9) || source_mode || char(9) || phase_identity || char(9) || execution_allowed || char(9) ||
 certification_state
 FROM editorial_p4_bindings
 WHERE binding_identity=$bindingLiteral AND project_row_id=$script:P5EProjectRowId;
SELECT 'INPUT' || char(9) || role || char(9) || byte_length || char(9) || sha256 || char(9) ||
 encoding || char(9) || schema_status || char(9) || ordinal
 FROM editorial_p4_binding_inputs
 WHERE binding_identity=$bindingLiteral ORDER BY role, ordinal;
SELECT 'ATTEMPT' || char(9) || attempt_identity || char(9) || request_identity || char(9) ||
 binding_identity || char(9) || run_declaration_identity || char(9) || chapter_key || char(9) ||
 phase || char(9) || predecessor_identity || char(9) || request_envelope_hash || char(9) ||
 provider || char(9) || model || char(9) || status || char(9) || response_identity || char(9) ||
 COALESCE(length(report_bytes), 'NULL') || char(9) || CASE WHEN report_bytes IS NULL THEN 'NULL' ELSE hex(report_bytes) END || char(9) ||
 COALESCE(length(receipt_bytes), 'NULL') || char(9) || CASE WHEN receipt_bytes IS NULL THEN 'NULL' ELSE hex(receipt_bytes) END || char(9) ||
 COALESCE(hex(CAST(metrics_json AS BLOB)), 'NULL') || char(9) || created_at || char(9) ||
 updated_at || char(9) || recovery_reason_code
 FROM editorial_p5c_attempts WHERE attempt_identity=$attemptLiteral;
SELECT 'AUTH' || char(9) || authorization_id_hash || char(9) || exact_phase || char(9) ||
 attempt_identity || char(9) || request_identity || char(9) || binding_identity || char(9) ||
 run_declaration_identity || char(9) || chapter_key || char(9) || provider || char(9) || model ||
 char(9) || endpoint_account_fingerprint || char(9) || issued_at || char(9) || expires_at ||
 char(9) || consumed_at || char(9) || maximum_primary_calls || char(9) ||
 maximum_schema_repair_calls || char(9) || maximum_network_retries || char(9) ||
 maximum_input_tokens || char(9) || maximum_output_tokens || char(9) || maximum_total_tokens ||
 char(9) || maximum_total_cost || char(9) || maximum_execution_time_ms || char(9) ||
 consumption_result || char(9) || consumed_attempt_identity
 FROM editorial_p5d_authorization_receipts WHERE attempt_identity=$attemptLiteral;
SELECT 'LIFE' || char(9) || attempt_identity || char(9) || stage || char(9) ||
 request_body_bytes || char(9) || response_body_bytes || char(9) || http_status || char(9) ||
 response_content_type || char(9) || exception_class || char(9) || elapsed_ms || char(9) ||
 generation_id || char(9) || provider_response_id || char(9) || cancellation_source || char(9) ||
 updated_at
 FROM editorial_p5d_network_lifecycle WHERE attempt_identity=$attemptLiteral;
SELECT 'LINEAGE' || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5c_attempts WHERE attempt_identity=$attemptLiteral) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_authorization_receipts WHERE attempt_identity=$attemptLiteral) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_network_lifecycle WHERE attempt_identity=$attemptLiteral) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=$bindingLiteral AND chapter_key='001' AND phase='L1_RAW_DISCOVERY') || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_authorization_receipts ar JOIN editorial_p5c_attempts a ON ar.attempt_identity=a.attempt_identity WHERE a.binding_identity=$bindingLiteral AND a.chapter_key='001' AND a.phase='L1_RAW_DISCOVERY') || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_network_lifecycle l JOIN editorial_p5c_attempts a ON l.attempt_identity=a.attempt_identity WHERE a.binding_identity=$bindingLiteral AND a.chapter_key='001' AND a.phase='L1_RAW_DISCOVERY') || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation r JOIN editorial_p5c_attempts a ON r.attempt_identity=a.attempt_identity WHERE a.binding_identity=$bindingLiteral) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation_history h JOIN editorial_p5c_attempts a ON h.attempt_identity=a.attempt_identity WHERE a.binding_identity=$bindingLiteral) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=$bindingLiteral AND report_bytes IS NOT NULL AND receipt_bytes IS NOT NULL) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5c_attempts WHERE binding_identity=$bindingLiteral AND (report_bytes IS NOT NULL OR receipt_bytes IS NOT NULL) AND NOT (report_bytes IS NOT NULL AND receipt_bytes IS NOT NULL)) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5c_attempts WHERE status='CLAIMED' AND attempt_identity<>$attemptLiteral) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5c_attempts) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_authorization_receipts) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_network_lifecycle) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation_history);
SELECT 'GLOBAL' || char(9) || (SELECT COUNT(*) FROM editorial_p5c_attempts) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_authorization_receipts) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_network_lifecycle) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation) || char(9) ||
 (SELECT COUNT(*) FROM editorial_p5d_reconciliation_history);
SELECT 'INTEGRITY_BEGIN';
PRAGMA integrity_check;
SELECT 'INTEGRITY_END';
SELECT 'FOREIGN_KEY_BEGIN';
PRAGMA foreign_key_check;
SELECT 'FOREIGN_KEY_END';
COMMIT;
"@
    return [regex]::Replace($query, '\s+', ' ').Trim()
}

function ConvertFrom-P5EConsistentDatabaseReadbackOutput {
    param([Parameter(Mandatory = $true)][string]$Output)
    $schema = $null
    $bindingRows = [System.Collections.Generic.List[object]]::new()
    $inputRows = [System.Collections.Generic.List[object]]::new()
    $attemptRows = [System.Collections.Generic.List[object]]::new()
    $authRows = [System.Collections.Generic.List[object]]::new()
    $lifeRows = [System.Collections.Generic.List[object]]::new()
    $lineage = $null
    $globalCounts = $null
    $integrityValues = [System.Collections.Generic.List[string]]::new()
    $foreignKeyRows = [System.Collections.Generic.List[string]]::new()
    $mode = 'NONE'
    $lines = @($Output -split '\r?\n' | Where-Object { $_ -ne '' })
    foreach ($line in $lines) {
        if ($line -ceq 'INTEGRITY_BEGIN') { $mode = 'INTEGRITY'; continue }
        if ($line -ceq 'INTEGRITY_END') { $mode = 'NONE'; continue }
        if ($line -ceq 'FOREIGN_KEY_BEGIN') { $mode = 'FOREIGN_KEY'; continue }
        if ($line -ceq 'FOREIGN_KEY_END') { $mode = 'NONE'; continue }
        if ($mode -eq 'INTEGRITY') { [void]$integrityValues.Add($line); continue }
        if ($mode -eq 'FOREIGN_KEY') { [void]$foreignKeyRows.Add($line); continue }
        $rawColumns = $line.Split([char]9)
        $columns = [object[]]::new($rawColumns.Count)
        for ($columnIndex = 0; $columnIndex -lt $rawColumns.Count; $columnIndex++) {
            $columns[$columnIndex] = if ($rawColumns[$columnIndex] -ceq 'NULL') { $null } else { $rawColumns[$columnIndex] }
        }
        if ($columns.Count -lt 1) { throw 'P5E_COLLECTOR_DATABASE_OUTPUT_INVALID' }
        switch ($columns[0]) {
            'SCHEMA' { if ($columns.Count -ne 2 -or $null -ne $schema) { throw 'P5E_COLLECTOR_SCHEMA_ROW_INVALID' }; $schema = $columns[1] }
            'BINDING' { if ($columns.Count -ne 13) { throw 'P5E_COLLECTOR_BINDING_ROW_INVALID' }; [void]$bindingRows.Add(@($columns[1..12])) }
            'INPUT' { if ($columns.Count -ne 7) { throw 'P5E_COLLECTOR_INPUT_ROW_INVALID' }; [void]$inputRows.Add(@($columns[1..6])) }
            'ATTEMPT' { if ($columns.Count -ne 21) { throw 'P5E_COLLECTOR_ATTEMPT_ROW_INVALID' }; [void]$attemptRows.Add(@($columns[1..20])) }
            'AUTH' { if ($columns.Count -ne 24) { throw 'P5E_COLLECTOR_AUTH_ROW_INVALID' }; [void]$authRows.Add(@($columns[1..23])) }
            'LIFE' { if ($columns.Count -ne 13) { throw 'P5E_COLLECTOR_LIFECYCLE_ROW_INVALID' }; [void]$lifeRows.Add(@($columns[1..12])) }
            'LINEAGE' { if ($columns.Count -ne 17 -or $null -ne $lineage) { throw 'P5E_COLLECTOR_LINEAGE_ROW_INVALID' }; $lineage = @($columns[1..16] | ForEach-Object { ConvertTo-P5ECollectorLong -Value $_ -Name 'lineage' }) }
            'GLOBAL' { if ($columns.Count -ne 6 -or $null -ne $globalCounts) { throw 'P5E_COLLECTOR_GLOBAL_COUNT_ROW_INVALID' }; $globalCounts = @($columns[1..5] | ForEach-Object { ConvertTo-P5ECollectorLong -Value $_ -Name 'global-count' }) }
            default { throw ('P5E_COLLECTOR_DATABASE_UNEXPECTED_OUTPUT:' + $columns[0]) }
        }
    }
    if ($null -eq $lineage -or $null -eq $globalCounts) { throw 'P5E_COLLECTOR_LINEAGE_OR_GLOBAL_ROW_MISSING' }
    for ($index = 0; $index -lt 5; $index++) {
        if ($lineage[11 + $index] -ne $globalCounts[$index]) { throw 'P5E_COLLECTOR_LINEAGE_GLOBAL_MISMATCH' }
    }
    foreach ($attemptRow in $attemptRows) {
        $reportLength = if ($null -eq $attemptRow[12] -or [string]::IsNullOrWhiteSpace([string]$attemptRow[12])) { $null } else { ConvertTo-P5ECollectorLong -Value ([string]$attemptRow[12]) -Name 'report-byte-length' }
        $reportHex = if ($null -eq $attemptRow[13]) { $null } else { [string]$attemptRow[13] }
        $receiptLength = if ($null -eq $attemptRow[14] -or [string]::IsNullOrWhiteSpace([string]$attemptRow[14])) { $null } else { ConvertTo-P5ECollectorLong -Value ([string]$attemptRow[14]) -Name 'receipt-byte-length' }
        $receiptHex = if ($null -eq $attemptRow[15]) { $null } else { [string]$attemptRow[15] }
        if ((($null -eq $reportLength) -ne ($null -eq $reportHex)) -or
                (($null -eq $receiptLength) -ne ($null -eq $receiptHex))) {
            throw 'P5E_COLLECTOR_ATTEMPT_NULL_SENTINEL_PAIR_INVALID'
        }
        foreach ($blob in @(
                [pscustomobject]@{ Name = 'report'; Length = $reportLength; Hex = $reportHex },
                [pscustomobject]@{ Name = 'receipt'; Length = $receiptLength; Hex = $receiptHex })) {
            if ($null -ne $blob.Hex) {
                if ($blob.Hex -notmatch '^(?:[0-9A-Fa-f]{2})*$') { throw ('P5E_COLLECTOR_' + $blob.Name.ToUpperInvariant() + '_HEX_INVALID') }
                if ($blob.Length -ne [long]($blob.Hex.Length / 2)) { throw ('P5E_COLLECTOR_' + $blob.Name.ToUpperInvariant() + '_BYTE_LENGTH_MISMATCH') }
            }
        }
        if ($null -ne $attemptRow[16]) { Get-P5ETextFromHex -Hex ([string]$attemptRow[16]) -Name 'metrics-json' | Out-Null }
    }
    return [ordered]@{
        schema = $schema
        bindingRows = $bindingRows.ToArray()
        inputRows = $inputRows.ToArray()
        attemptRows = $attemptRows.ToArray()
        authRows = $authRows.ToArray()
        lifeRows = $lifeRows.ToArray()
        lineage = $lineage
        globalCounts = $globalCounts
        integrityValues = $integrityValues.ToArray()
        foreignKeyRows = $foreignKeyRows.ToArray()
    }
}

function Get-P5EConsistentDatabaseReadback {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [string]$LocalDatabasePath = '',
        [string]$LocalOutputPath = ''
    )
    $offline = -not [string]::IsNullOrWhiteSpace($LocalDatabasePath)
    $filesBefore = if ($offline) {
        Get-P5ELocalDatabaseFileHashState -DatabasePath $LocalDatabasePath
    } else {
        Get-P5EDeviceFileHashState -AdbPath $AdbPath -SerialValue $SerialValue
    }
    $query = New-P5EConsistentDatabaseReadbackQuery
    $run = if ($offline) {
        $localRun = Invoke-P5ELocalSqliteReadOnly -DatabasePath $LocalDatabasePath -Query $query
        if (-not [string]::IsNullOrWhiteSpace($LocalOutputPath)) {
            Write-P5EUtf8NoBom -Path $LocalOutputPath -Text $localRun.Stdout
        }
        $localRun
    } else {
        Invoke-P5EAdbShellReadOnly -AdbPath $AdbPath -SerialValue $SerialValue -Operation 'database-consistent-read-transaction' -RemoteTokens @('run-as', $script:P5ETargetPackage, 'sqlite3', '-readonly', '-batch', '-noheader', '-separator', $script:P5ECollectorSeparator, $script:P5EDatabaseDevicePath, $query) -TimeoutMilliseconds 60000L
    }
    $filesAfter = if ($offline) {
        Get-P5ELocalDatabaseFileHashState -DatabasePath $LocalDatabasePath
    } else {
        Get-P5EDeviceFileHashState -AdbPath $AdbPath -SerialValue $SerialValue
    }
    if (-not [bool]$filesBefore.settings.present) { throw 'P5E_COLLECTOR_SETTINGS_HASH_MISSING' }
    foreach ($name in @('database', 'wal', 'shm', 'settings')) {
        $beforeFile = $filesBefore[$name]
        $afterFile = $filesAfter[$name]
        if ([bool]$beforeFile.present -ne [bool]$afterFile.present -or [string]$beforeFile.sha256 -cne [string]$afterFile.sha256) {
            throw ('P5E_COLLECTOR_DATABASE_CHANGED_DURING_READ_TRANSACTION:' + $name)
        }
    }
    $parsed = ConvertFrom-P5EConsistentDatabaseReadbackOutput -Output ([string]$run.Stdout)
    $schema = $parsed.schema
    $bindingRows = @($parsed.bindingRows)
    $inputRows = @($parsed.inputRows)
    $attemptRows = @($parsed.attemptRows)
    $authRows = @($parsed.authRows)
    $lifeRows = @($parsed.lifeRows)
    $lineage = $parsed.lineage
    $globalCounts = $parsed.globalCounts
    $integrityValues = @($parsed.integrityValues)
    $foreignKeyRows = @($parsed.foreignKeyRows)
    if ($null -eq $schema -or (ConvertTo-P5ECollectorLong -Value $schema -Name 'schema-version') -ne $script:P5EDatabaseSchemaVersion) { throw 'P5E_COLLECTOR_DATABASE_SCHEMA_VERSION_MISMATCH' }
    if ($bindingRows.Count -ne 1) { throw 'P5E_COLLECTOR_BINDING_ROW_MISSING_OR_DUPLICATE' }
    if ($inputRows.Count -ne 4) { throw 'P5E_COLLECTOR_BINDING_INPUT_COUNT_INVALID' }
    if ($attemptRows.Count -gt 1 -or $authRows.Count -gt 1 -or $lifeRows.Count -gt 1) { throw 'P5E_COLLECTOR_EXACT_ROW_DUPLICATE' }
    if ($null -eq $lineage -or $null -eq $globalCounts -or $integrityValues.Count -ne 1 -or $integrityValues[0] -cne 'ok' -or $foreignKeyRows.Count -ne 0) { throw 'P5E_COLLECTOR_DATABASE_INTEGRITY_OR_OUTPUT_INVALID' }
    $bindingRow = $bindingRows[0]
    $expectedSources = [ordered]@{
        RAW = @($script:P5ERawSourceBytes, $script:P5ERawSourceSha256, 'VISIBLE')
        GLOSSARY = @($script:P5EGlossarySourceBytes, $script:P5EGlossarySourceSha256, 'VISIBLE')
        DRAFT = @($script:P5EDraftSourceBytes, $script:P5EDraftSourceSha256, 'HIDDEN')
        PRONOUN = @($script:P5EPronounSourceBytes, $script:P5EPronounSourceSha256, 'HIDDEN')
    }
    $seenRoles = [System.Collections.Generic.HashSet[string]]::new()
    $sources = [System.Collections.Generic.List[object]]::new()
    foreach ($row in $inputRows) {
        $role = [string]$row[0]
        if (-not $seenRoles.Add($role) -or -not $expectedSources.Contains($role) -or
                (ConvertTo-P5ECollectorLong -Value ([string]$row[1]) -Name ('source-byte-length-' + $role)) -ne [long]$expectedSources[$role][0] -or
                [string]$row[2] -cne [string]$expectedSources[$role][1] -or
                [string]::IsNullOrWhiteSpace([string]$row[3]) -or [string]::IsNullOrWhiteSpace([string]$row[4])) {
            throw ('P5E_COLLECTOR_SOURCE_INPUT_MISMATCH:' + $role)
        }
        [void]$sources.Add([ordered]@{
            role = $role; visibility = $expectedSources[$role][2]; byteLength = [long]$row[1]
            sha256 = ([string]$row[2]).ToLowerInvariant(); encoding = [string]$row[3]
            schemaStatus = [string]$row[4]; ordinal = [long]$row[5]
        })
    }
    $binding = [ordered]@{
        projectRowId = ConvertTo-P5ECollectorLong -Value ([string]$bindingRow[0]) -Name 'binding-project-row-id'
        selector = [string]$bindingRow[1]; bindingIdentity = [string]$bindingRow[2]
        runDeclarationIdentity = [string]$bindingRow[3]; packHash = [string]$bindingRow[4]
        manifestFingerprint = [string]$bindingRow[5]; profileHash = [string]$bindingRow[6]
        evaluationId = [string]$bindingRow[7]; sourceMode = [string]$bindingRow[8]
        phaseIdentity = [string]$bindingRow[9]
        executionAllowed = (ConvertTo-P5ECollectorLong -Value ([string]$bindingRow[10]) -Name 'binding-execution-allowed') -eq 1L
        certificationState = [string]$bindingRow[11]; sources = $sources.ToArray()
    }
    $lineageValue = [ordered]@{
        exactAttempt = $lineage[0]; exactAuthorization = $lineage[1]; exactLifecycle = $lineage[2]
        attempts = $lineage[3]; authorizationReceipts = $lineage[4]; lifecycle = $lineage[5]
        reconciliation = $lineage[6]; reconciliationHistory = $lineage[7]; reportOrReceipt = $lineage[8]
        partialArtifactPair = $lineage[9]; activeCompetingWriter = $lineage[10]
    }
    $globalValue = [ordered]@{
        attempts = $globalCounts[0]; authorizationReceipts = $globalCounts[1]
        lifecycle = $globalCounts[2]; reconciliation = $globalCounts[3]; reconciliationHistory = $globalCounts[4]
    }
    $attempt = $null
    if ($attemptRows.Count -eq 1) {
        $row = $attemptRows[0]
        $reportLength = if ($null -eq $row[12] -or [string]::IsNullOrWhiteSpace([string]$row[12])) { $null } else { ConvertTo-P5ECollectorLong -Value ([string]$row[12]) -Name 'report-byte-length' }
        $reportHex = if ($null -eq $row[13]) { $null } else { [string]$row[13] }
        $receiptLength = if ($null -eq $row[14] -or [string]::IsNullOrWhiteSpace([string]$row[14])) { $null } else { ConvertTo-P5ECollectorLong -Value ([string]$row[14]) -Name 'receipt-byte-length' }
        $receiptHex = if ($null -eq $row[15]) { $null } else { [string]$row[15] }
        if ((($null -eq $reportLength) -ne ($null -eq $reportHex)) -or
                (($null -eq $receiptLength) -ne ($null -eq $receiptHex))) {
            throw 'P5E_COLLECTOR_ATTEMPT_NULL_SENTINEL_PAIR_INVALID'
        }
        foreach ($blob in @(
                [pscustomobject]@{ Name = 'report'; Length = $reportLength; Hex = $reportHex },
                [pscustomobject]@{ Name = 'receipt'; Length = $receiptLength; Hex = $receiptHex })) {
            if ($null -ne $blob.Hex) {
                if ($blob.Hex -notmatch '^(?:[0-9A-Fa-f]{2})*$') { throw ('P5E_COLLECTOR_' + $blob.Name.ToUpperInvariant() + '_HEX_INVALID') }
                if ($blob.Length -ne [long]($blob.Hex.Length / 2)) { throw ('P5E_COLLECTOR_' + $blob.Name.ToUpperInvariant() + '_BYTE_LENGTH_MISMATCH') }
            }
        }
        $attempt = [ordered]@{
            rowCount = 1L; attemptIdentity = [string]$row[0]; requestIdentity = [string]$row[1]
            bindingIdentity = [string]$row[2]; runDeclarationIdentity = [string]$row[3]
            chapterKey = [string]$row[4]; phase = [string]$row[5]; predecessorIdentity = [string]$row[6]
            requestEnvelopeHash = [string]$row[7]; provider = [string]$row[8]; model = [string]$row[9]
            status = [string]$row[10]; responseIdentity = [string]$row[11]
            reportByteLength = $reportLength; reportHex = $reportHex
            receiptByteLength = $receiptLength; receiptHex = $receiptHex
            metrics = if ($null -eq $row[16]) { $null } else { Get-P5ETextFromHex -Hex ([string]$row[16]) -Name 'metrics-json' }
            createdAtMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[17]) -Name 'attempt-created-at'
            updatedAtMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[18]) -Name 'attempt-updated-at'
            recoveryReasonCode = [string]$row[19]
        }
    }
    $authorization = $null
    if ($authRows.Count -eq 1) {
        $row = $authRows[0]
        $authorization = [ordered]@{
            rowCount = 1L; authorizationIdHash = [string]$row[0]; exactPhase = [string]$row[1]
            attemptIdentity = [string]$row[2]; requestIdentity = [string]$row[3]; bindingIdentity = [string]$row[4]
            runDeclarationIdentity = [string]$row[5]; chapterKey = [string]$row[6]; provider = [string]$row[7]
            model = [string]$row[8]; endpointAccountFingerprint = [string]$row[9]
            issuedAtMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[10]) -Name 'authorization-issued-at'
            expiresAtMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[11]) -Name 'authorization-expires-at'
            consumedAtMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[12]) -Name 'authorization-consumed-at' -Minimum (-1L)
            maximumPrimaryCalls = ConvertTo-P5ECollectorLong -Value ([string]$row[13]) -Name 'maximum-primary-calls'
            maximumSchemaRepairCalls = ConvertTo-P5ECollectorLong -Value ([string]$row[14]) -Name 'maximum-schema-repair-calls'
            maximumNetworkRetries = ConvertTo-P5ECollectorLong -Value ([string]$row[15]) -Name 'maximum-network-retries'
            maximumInputTokens = ConvertTo-P5ECollectorLong -Value ([string]$row[16]) -Name 'maximum-input-tokens'
            maximumOutputTokens = ConvertTo-P5ECollectorLong -Value ([string]$row[17]) -Name 'maximum-output-tokens'
            maximumTotalTokens = ConvertTo-P5ECollectorLong -Value ([string]$row[18]) -Name 'maximum-total-tokens'
            maximumTotalCost = [string]$row[19]
            maximumExecutionTimeMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[20]) -Name 'maximum-execution-time'
            consumptionResult = [string]$row[21]; consumedAttemptIdentity = [string]$row[22]
        }
    }
    $lifecycle = $null
    if ($lifeRows.Count -eq 1) {
        $row = $lifeRows[0]
        $lifecycle = [ordered]@{
            rowCount = 1L; attemptIdentity = [string]$row[0]; stage = [string]$row[1]
            requestBodyBytes = ConvertTo-P5ECollectorLong -Value ([string]$row[2]) -Name 'lifecycle-request-bytes'
            responseBodyBytes = ConvertTo-P5ECollectorLong -Value ([string]$row[3]) -Name 'lifecycle-response-bytes'
            httpStatus = [int](ConvertTo-P5ECollectorLong -Value ([string]$row[4]) -Name 'lifecycle-http-status' -Minimum (-1L))
            responseContentType = [string]$row[5]; exceptionClass = [string]$row[6]
            elapsedMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[7]) -Name 'lifecycle-elapsed'
            generationId = [string]$row[8]; providerResponseId = [string]$row[9]
            cancellationSource = [string]$row[10]; updatedAtMillis = ConvertTo-P5ECollectorLong -Value ([string]$row[11]) -Name 'lifecycle-updated-at'
        }
    }
    return [ordered]@{
        filesBefore = $filesBefore; filesAfter = $filesAfter
        databaseSha256 = [string]$filesAfter.database.sha256
        databaseWalSha256 = [string]$filesAfter.wal.sha256; databaseShmSha256 = [string]$filesAfter.shm.sha256
        settingsSha256 = [string]$filesAfter.settings.sha256
        walPresent = [bool]$filesAfter.wal.present; shmPresent = [bool]$filesAfter.shm.present
        databaseSchemaVersion = ConvertTo-P5ECollectorLong -Value $schema -Name 'schema-version'
        integrityCheck = 'ok'; foreignKeyViolations = 0L; binding = $binding
        lineage = $lineageValue; globalCounts = $globalValue; attempt = $attempt
        authorizationReceipt = $authorization; lifecycle = $lifecycle; readOnlyCommandCount = 1L
    }
}

function New-P5EArtifactMetadataFromValue {
    param(
        [Parameter(Mandatory = $true)]$Value,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Kind
    )
    $result = [ordered]@{}
    foreach ($property in @($Value.PSObject.Properties)) { $result[$property.Name] = $property.Value }
    $result.byteLength = [long](Get-Item -LiteralPath $Path).Length
    $result.sha256 = Get-P5ESha256 -Path $Path
    $result.validationPassed = $true
    return $result
}

function Write-P5EImmutableBytes {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][byte[]]$Bytes,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $algorithm = [Security.Cryptography.SHA256]::Create()
        try { $expectedHash = -join ($algorithm.ComputeHash($Bytes) | ForEach-Object { $_.ToString('x2') }) }
        finally { $algorithm.Dispose() }
        if ((Get-P5ESha256 -Path $Path) -cne $expectedHash) {
            throw ('P5E_COLLECTOR_IMMUTABLE_FILE_REPLACEMENT:' + $Name)
        }
        return
    }
    [IO.File]::WriteAllBytes($Path, $Bytes)
}

function Write-P5ECollectorOutcome {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][string]$CollectionPhase,
        [Parameter(Mandatory = $true)][string]$TypedOutcome,
        [string]$Detail = '',
        [int]$ReadOnlyCommandCount = 0,
        [string]$PostReadbackPath = ''
    )
    $plan = $null
    try { $plan = Read-P5EEventPlan -EvidenceDirectory $EvidenceDirectory } catch { }
    $eventId = if ($null -eq $plan) { Split-Path -Leaf (Get-P5ECanonicalPath -Path $EvidenceDirectory) } else { [string](Get-P5EProperty $plan 'eventId') }
    $outcome = [ordered]@{
        schemaVersion = $script:P5ECollectorOutcomeSchema
        eventId = $eventId
        evidenceDirectory = Get-P5ECanonicalPath -Path $EvidenceDirectory
        collectionPhase = $CollectionPhase
        collectorImplementationId = $script:P5ECollectorImplementationId
        collectorImplementationVersion = $script:P5ECollectorImplementationVersion
        collectionMode = $script:P5ECollectorReadbackMode
        typedOutcome = $TypedOutcome
        detailCode = if ([string]::IsNullOrWhiteSpace($Detail)) { '' } else { ($Detail -split ':')[0] }
        accepted = $false
        readOnlyCommandCount = $ReadOnlyCommandCount
        providerCalls = 0
        credentialReads = 0
        deviceMutations = 0
        redispatchCount = 0
        postReadbackPath = $PostReadbackPath
        commandLogPath = Get-P5ECollectorCommandLogPath -EvidenceDirectory $EvidenceDirectory
        p6Ready = $false
    }
    Write-P5EUtf8NoBom -Path (Get-P5ECollectorOutcomePath -Directory $EvidenceDirectory) -Text (ConvertTo-P5ECanonicalJson -Value $outcome)
    return $outcome
}

function Read-P5ECollectorOutcome {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedEvidenceDirectory,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_MISSING'
        return $null
    }
    try { $outcome = Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json }
    catch {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_INVALID_JSON'
        return $null
    }
    $required = @('schemaVersion', 'eventId', 'evidenceDirectory', 'collectionPhase',
        'collectorImplementationId', 'collectorImplementationVersion', 'collectionMode',
        'typedOutcome', 'detailCode', 'accepted', 'readOnlyCommandCount', 'providerCalls',
        'credentialReads', 'deviceMutations', 'redispatchCount', 'postReadbackPath',
        'commandLogPath', 'p6Ready')
    Test-P5EObjectShape -Object $outcome -Path 'collectorOutcome' -Required $required -Allowed $required -Errors $Errors | Out-Null
    if ($null -eq $outcome) { return $null }
    Test-P5EEqual $outcome 'schemaVersion' $script:P5ECollectorOutcomeSchema 'collectorOutcome' $Errors
    Test-P5EEqual $outcome 'collectorImplementationId' $script:P5ECollectorImplementationId 'collectorOutcome' $Errors
    Test-P5EEqual $outcome 'collectorImplementationVersion' $script:P5ECollectorImplementationVersion 'collectorOutcome' $Errors
    Test-P5EEqual $outcome 'collectionMode' $script:P5ECollectorReadbackMode 'collectorOutcome' $Errors
    if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $outcome 'evidenceDirectory')) -Right $ExpectedEvidenceDirectory)) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_EVIDENCE_DIRECTORY_MISMATCH'
    }
    if (-not (Test-P5EPathEqual -Left $Path -Right (Get-P5ECollectorOutcomePath -Directory $ExpectedEvidenceDirectory))) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_PATH_MISMATCH'
    }
    $eventId = Split-Path -Leaf (Get-P5ECanonicalPath -Path $ExpectedEvidenceDirectory)
    Test-P5EEqual $outcome 'eventId' $eventId 'collectorOutcome' $Errors
    $phase = [string](Get-P5EProperty $outcome 'collectionPhase')
    if ($phase -notin @('Before', 'After')) { Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_PHASE_INVALID' }
    $typed = [string](Get-P5EProperty $outcome 'typedOutcome')
    if ($typed -notin @('BEFORE_STATE_CAPTURED', 'NO_CLAIM_OBSERVED',
            'EXTERNAL_CALL_STATE_UNKNOWN', 'RECOVERY_REQUIRED',
            'COMMITTED_READBACK_CAPTURED', 'COLLECTOR_TYPED_STOP')) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_TYPE_INVALID'
    }
    foreach ($name in @('readOnlyCommandCount', 'providerCalls', 'credentialReads', 'deviceMutations', 'redispatchCount')) {
        Test-P5ELongRange -Object $outcome -Name $name -Path 'collectorOutcome' -Errors $Errors -Minimum 0 | Out-Null
    }
    foreach ($name in @('providerCalls', 'credentialReads', 'deviceMutations', 'redispatchCount')) {
        Test-P5ELong $outcome $name 0L 'collectorOutcome' $Errors -Minimum 0 | Out-Null
    }
    Test-P5EBoolean $outcome 'accepted' $false 'collectorOutcome' $Errors
    Test-P5EBoolean $outcome 'p6Ready' $false 'collectorOutcome' $Errors
    $detail = [string](Get-P5EProperty $outcome 'detailCode')
    if ($detail -match '[0-9a-fA-F]{64}' -or $detail -match '(?i)fingerprint|credential|endpoint') {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_REDACTION_INVALID'
    }
    $postPath = [string](Get-P5EProperty $outcome 'postReadbackPath')
    if (-not [string]::IsNullOrWhiteSpace($postPath) -and
            -not (Test-P5EPathEqual -Left $postPath -Right (Join-Path $ExpectedEvidenceDirectory 'post-readback.json'))) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_READBACK_PATH_MISMATCH'
    }
    if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $outcome 'commandLogPath')) -Right (Get-P5ECollectorCommandLogPath -EvidenceDirectory $ExpectedEvidenceDirectory))) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_OUTCOME_COMMAND_LOG_PATH_MISMATCH'
    }
    return [pscustomobject]@{ Path = Get-P5ECanonicalPath -Path $Path; Outcome = $outcome }
}

function New-P5ERecoveryReadback {
    param(
        [Parameter(Mandatory = $true)]$Plan,
        [Parameter(Mandatory = $true)][string]$CollectionPhase,
        [Parameter(Mandatory = $true)][string]$Status,
        [Parameter(Mandatory = $true)][long]$ObservedAtMillis,
        [string]$ReasonCode = ''
    )
    return [ordered]@{
        schemaVersion = $script:P5EReadbackSchema
        eventId = [string](Get-P5EProperty $Plan 'eventId')
        observedAtMillis = $ObservedAtMillis
        externalCallState = if ($Status -eq 'RECOVERY_REQUIRED') { 'RECOVERY_REQUIRED' } else { 'UNKNOWN' }
        attemptStatus = $Status
        attemptIdentity = $script:P5EAttemptIdentity
        recoveryReasonCode = $ReasonCode
        acceptance = 'NOT_PROVEN'
        p6Ready = $false
    }
}

function Write-P5ECollectorJsonImmutable {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Value
    )
    $directory = Split-Path -Parent (Get-P5ECanonicalPath -Path $Path)
    if (-not (Test-Path -LiteralPath $directory -PathType Container)) {
        [void](New-Item -ItemType Directory -Path $directory -ErrorAction Stop)
    }
    $json = ConvertTo-P5ECanonicalJson -Value $Value
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes($json)
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $algorithm = [Security.Cryptography.SHA256]::Create()
        try { $expectedHash = -join ($algorithm.ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') }) }
        finally { $algorithm.Dispose() }
        if ((Get-P5ESha256 -Path $Path) -cne $expectedHash) {
            throw ('P5E_COLLECTOR_IMMUTABLE_FILE_REPLACEMENT:' + (Split-Path -Leaf $Path))
        }
        return Get-P5ECanonicalPath -Path $Path
    }
    [IO.File]::WriteAllBytes($Path, $bytes)
    return Get-P5ECanonicalPath -Path $Path
}

function Assert-P5ECollectorBinding {
    param([Parameter(Mandatory = $true)]$Binding)
    if ([long](Get-P5EProperty $Binding 'projectRowId') -ne $script:P5EProjectRowId -or
            [string](Get-P5EProperty $Binding 'bindingIdentity') -cne $script:P5EBindingIdentity -or
            [string](Get-P5EProperty $Binding 'runDeclarationIdentity') -cne $script:P5ERunDeclarationIdentity -or
            [string](Get-P5EProperty $Binding 'packHash') -cne $script:P5EPackHash -or
            [string](Get-P5EProperty $Binding 'manifestFingerprint') -cne $script:P5EExpectedManifestFingerprint -or
            [string](Get-P5EProperty $Binding 'profileHash') -cne $script:P5EProfileHash -or
            [string](Get-P5EProperty $Binding 'evaluationId') -cne $script:P5EEvaluationId -or
            [string](Get-P5EProperty $Binding 'sourceMode') -cne 'NORMAL_FOUR_SOURCE' -or
            [string](Get-P5EProperty $Binding 'phaseIdentity') -cne $script:P5EBindingPhaseIdentity -or
            [bool](Get-P5EProperty $Binding 'executionAllowed') -or
            [string](Get-P5EProperty $Binding 'certificationState') -cne 'NOT_CERTIFIED') {
        throw 'P5E_COLLECTOR_BINDING_TUPLE_MISMATCH'
    }
    $roles = @((Get-P5EProperty $Binding 'sources') | ForEach-Object { [string](Get-P5EProperty $_ 'role') } | Sort-Object)
    if (($roles -join ',') -cne 'DRAFT,GLOSSARY,PRONOUN,RAW') {
        throw 'P5E_COLLECTOR_SOURCE_ROLE_SET_INVALID'
    }
}

function Assert-P5ECollectorZeroBefore {
    param([Parameter(Mandatory = $true)]$Database)
    $lineage = Get-P5EProperty $Database 'lineage'
    foreach ($name in @('exactAttempt', 'exactAuthorization', 'exactLifecycle', 'attempts',
            'authorizationReceipts', 'lifecycle', 'reconciliation', 'reconciliationHistory',
            'reportOrReceipt', 'partialArtifactPair', 'activeCompetingWriter')) {
        if ([long](Get-P5EProperty $lineage $name) -ne 0L) {
            throw ('P5E_COLLECTOR_FRESH_LINEAGE_NOT_UNUSED:' + $name)
        }
    }
    $global = Get-P5EProperty $Database 'globalCounts'
    foreach ($name in @('attempts', 'authorizationReceipts', 'lifecycle', 'reconciliation', 'reconciliationHistory')) {
        if ([long](Get-P5EProperty $global $name) -ne 0L) {
            throw ('P5E_COLLECTOR_GLOBAL_COUNT_NOT_ZERO:' + $name)
        }
    }
}

function Assert-P5ECollectorAfterLineage {
    param(
        [Parameter(Mandatory = $true)]$Database,
        [Parameter(Mandatory = $true)]$BeforeDatabase
    )
    $lineage = Get-P5EProperty $Database 'lineage'
    foreach ($name in @('exactAttempt', 'exactAuthorization', 'exactLifecycle', 'attempts',
            'authorizationReceipts', 'lifecycle', 'reportOrReceipt')) {
        if ([long](Get-P5EProperty $lineage $name) -ne 1L) {
            throw ('P5E_COLLECTOR_AFTER_LINEAGE_INVALID:' + $name)
        }
    }
    foreach ($name in @('reconciliation', 'reconciliationHistory', 'partialArtifactPair',
            'activeCompetingWriter')) {
        if ([long](Get-P5EProperty $lineage $name) -ne 0L) {
            throw ('P5E_COLLECTOR_AFTER_LINEAGE_INVALID:' + $name)
        }
    }
    $beforeGlobal = Get-P5EProperty $BeforeDatabase 'globalCounts'
    $afterGlobal = Get-P5EProperty $Database 'globalCounts'
    foreach ($name in @('attempts', 'authorizationReceipts', 'lifecycle')) {
        if ([long](Get-P5EProperty $afterGlobal $name) -ne [long](Get-P5EProperty $beforeGlobal $name) + 1L) {
            throw ('P5E_COLLECTOR_GLOBAL_DELTA_INVALID:' + $name)
        }
    }
    foreach ($name in @('reconciliation', 'reconciliationHistory')) {
        if ([long](Get-P5EProperty $afterGlobal $name) -ne [long](Get-P5EProperty $beforeGlobal $name)) {
            throw ('P5E_COLLECTOR_GLOBAL_DELTA_INVALID:' + $name)
        }
    }
}

function New-P5ECollectorFreshTuple {
    param([Parameter(Mandatory = $true)]$Binding)
    Assert-P5ECollectorBinding -Binding $Binding
    $sourceByRole = @{}
    foreach ($source in @((Get-P5EProperty $Binding 'sources'))) {
        $sourceByRole[[string](Get-P5EProperty $source 'role')] = $source
    }
    $sources = [System.Collections.Generic.List[object]]::new()
    foreach ($role in @('RAW', 'GLOSSARY', 'DRAFT', 'PRONOUN')) {
        $source = $sourceByRole[$role]
        [void]$sources.Add([ordered]@{
            role = $role
            visibility = [string](Get-P5EProperty $source 'visibility')
            byteLength = [long](Get-P5EProperty $source 'byteLength')
            sha256 = ([string](Get-P5EProperty $source 'sha256')).ToLowerInvariant()
        })
    }
    return [ordered]@{
        projectRowId = [long](Get-P5EProperty $Binding 'projectRowId')
        selector = [string](Get-P5EProperty $Binding 'selector')
        chapterKey = $script:P5EChapterKey
        bindingIdentity = [string](Get-P5EProperty $Binding 'bindingIdentity')
        runDeclarationIdentity = [string](Get-P5EProperty $Binding 'runDeclarationIdentity')
        evaluationId = [string](Get-P5EProperty $Binding 'evaluationId')
        packHash = [string](Get-P5EProperty $Binding 'packHash')
        profileHash = [string](Get-P5EProperty $Binding 'profileHash')
        sourceMode = [string](Get-P5EProperty $Binding 'sourceMode')
        sourceProjection = 'RAW_AND_GLOSSARY_VISIBLE_DRAFT_AND_PRONOUN_HIDDEN'
        sources = $sources.ToArray()
    }
}

function New-P5ECollectorSnapshot {
    param(
        [Parameter(Mandatory = $true)]$Plan,
        [Parameter(Mandatory = $true)]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('Before', 'After')][string]$Phase,
        [Parameter(Mandatory = $true)][long]$ObservedAtMillis
    )
    $files = if ($Phase -eq 'Before') {
        Get-P5EProperty $Database 'filesBefore'
    } else {
        Get-P5EProperty $Database 'filesAfter'
    }
    return [ordered]@{
        schemaVersion = 'p5e.raw.snapshot.v1'
        eventId = [string](Get-P5EProperty $Plan 'eventId')
        runIdentity = [string](Get-P5EProperty $Plan 'runIdentity')
        snapshotMode = 'WAL_AWARE_READ_TRANSACTION'
        observedAtMillis = $ObservedAtMillis
        databaseSha256 = [string](Get-P5EProperty (Get-P5EProperty $files 'database') 'sha256')
        databaseWalSha256 = [string](Get-P5EProperty (Get-P5EProperty $files 'wal') 'sha256')
        databaseShmSha256 = [string](Get-P5EProperty (Get-P5EProperty $files 'shm') 'sha256')
        walPresent = [bool](Get-P5EProperty (Get-P5EProperty $files 'wal') 'present')
        shmPresent = [bool](Get-P5EProperty (Get-P5EProperty $files 'shm') 'present')
        settingsSha256 = [string](Get-P5EProperty (Get-P5EProperty $files 'settings') 'sha256')
        databaseSchemaVersion = [long](Get-P5EProperty $Database 'databaseSchemaVersion')
        integrityCheck = [string](Get-P5EProperty $Database 'integrityCheck')
        foreignKeyViolations = [long](Get-P5EProperty $Database 'foreignKeyViolations')
        lineage = Get-P5EProperty $Database 'lineage'
        globalCounts = Get-P5EProperty $Database 'globalCounts'
        immutableTupleIdentity = [string](Get-P5EProperty (Get-P5EProperty $Database 'binding') 'bindingIdentity')
        unrelatedWrites = 0L
        deletedRows = 0L
    }
}

function New-P5ECollectorInput {
    param(
        [Parameter(Mandatory = $true)]$Plan,
        [Parameter(Mandatory = $true)]$Production,
        [Parameter(Mandatory = $true)]$Test,
        [Parameter(Mandatory = $true)]$FreshTuple
    )
    $productionIdentity = [ordered]@{
        package = [string](Get-P5EProperty $Production 'package')
        version = [string](Get-P5EProperty $Production 'version')
        versionCode = [long](Get-P5EProperty $Production 'versionCode')
        apkSha256 = [string](Get-P5EProperty $Production 'apkSha256')
        certificateSha256 = [string](Get-P5EProperty $Production 'certificateSha256')
    }
    $testIdentity = [ordered]@{
        package = [string](Get-P5EProperty $Test 'package')
        targetPackage = [string](Get-P5EProperty $Test 'targetPackage')
        apkSha256 = [string](Get-P5EProperty $Test 'apkSha256')
        certificateSha256 = [string](Get-P5EProperty $Test 'certificateSha256')
        runner = [string](Get-P5EProperty $Test 'runner')
        sourceCommit = $script:P5ETestSourceCommit
    }
    return [ordered]@{
        schemaVersion = $script:P5EProducerInputSchema
        eventId = [string](Get-P5EProperty $Plan 'eventId')
        runIdentity = [string](Get-P5EProperty $Plan 'runIdentity')
        sourceMappingVersion = $script:P5ESourceMappingVersion
        production = $productionIdentity
        test = $testIdentity
        freshTuple = $FreshTuple
        eventPlanPath = Get-P5EEventPlanPath -Directory ([string](Get-P5EProperty $Plan 'evidenceDirectory'))
        eventPlanSha256 = Get-P5ESha256 -Path (Get-P5EEventPlanPath -Directory ([string](Get-P5EProperty $Plan 'evidenceDirectory')))
        collectorImplementationId = $script:P5ECollectorImplementationId
        collectorImplementationVersion = $script:P5ECollectorImplementationVersion
        collectionMode = $script:P5ECollectorReadbackMode
        serial = [string](Get-P5EProperty $Plan 'serial')
        settingsSourcePath = Get-P5ECanonicalPath -Path (Join-Path (Get-P5ERepoRoot) $script:P5ESettingsSourcePath)
        settingsSourceSha256 = $script:P5ESettingsSourceSha256
        transactionSourcePath = Get-P5ECanonicalPath -Path (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionSourcePath)
        transactionSourceSha256 = $script:P5ETransactionSourceSha256
        transactionTestPath = Get-P5ECanonicalPath -Path (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionTestPath)
        transactionTestSha256 = $script:P5ETransactionTestSha256
    }
}

function New-P5ECollectorTransactionEvidence {
    param(
        [Parameter(Mandatory = $true)]$Plan,
        [Parameter(Mandatory = $true)][bool]$BeforeAfterConsistent,
        [Parameter(Mandatory = $true)][bool]$RowPairConsistent,
        [Parameter(Mandatory = $true)][bool]$ClaimAndAuthorizationAtomic,
        [Parameter(Mandatory = $true)][bool]$ReportReceiptAtomic,
        [Parameter(Mandatory = $true)][bool]$NoUnrelatedWrites,
        [Parameter(Mandatory = $true)][bool]$NoDeletes
    )
    $source = Get-P5ETransactionSourceEvidence
    return [ordered]@{
        schemaVersion = 'p5e.raw.transaction-evidence.v1'
        eventId = [string](Get-P5EProperty $Plan 'eventId')
        runIdentity = [string](Get-P5EProperty $Plan 'runIdentity')
        transactionTestId = 'editorial-p5c-atomic-claim-contract-v1'
        transactionSemanticsPassed = $true
        beforeAfterConsistent = $BeforeAfterConsistent
        rowPairConsistent = $RowPairConsistent
        claimAndAuthorizationAtomic = $ClaimAndAuthorizationAtomic
        reportReceiptAtomic = $ReportReceiptAtomic
        noUnrelatedWrites = $NoUnrelatedWrites
        noDeletes = $NoDeletes
        sourceTransactionCodePath = [string](Get-P5EProperty $source 'sourcePath')
        sourceTransactionCodeSha256 = [string](Get-P5EProperty $source 'sourceSha256')
        sourceTransactionTestPath = [string](Get-P5EProperty $source 'testPath')
        sourceTransactionTestSha256 = [string](Get-P5EProperty $source 'testSha256')
        sourceTransactionTestStatus = [string](Get-P5EProperty $source 'status')
    }
}

function Assert-P5ECollectorStablePackage {
    param(
        [Parameter(Mandatory = $true)]$Expected,
        [Parameter(Mandatory = $true)]$Actual,
        [Parameter(Mandatory = $true)][string]$Name
    )
    foreach ($name in @('package', 'version', 'versionCode', 'apkSha256', 'certificateSha256')) {
        if ([string](Get-P5EProperty $Expected $name) -cne [string](Get-P5EProperty $Actual $name)) {
            throw ('P5E_COLLECTOR_PACKAGE_CHANGED:' + $Name + ':' + $name)
        }
    }
}

function New-P5ECollectorMinimalReadback {
    param(
        [Parameter(Mandatory = $true)]$Plan,
        [Parameter(Mandatory = $true)][string]$CollectionPhase,
        [Parameter(Mandatory = $true)][string]$Status,
        [Parameter(Mandatory = $true)][long]$ObservedAtMillis,
        [Parameter(Mandatory = $true)][string]$PostReadbackPath,
        [string]$ReasonCode = ''
    )
    $value = New-P5ERecoveryReadback -Plan $Plan -CollectionPhase $CollectionPhase -Status $Status -ObservedAtMillis $ObservedAtMillis -ReasonCode $ReasonCode
    Write-P5ECollectorJsonImmutable -Path $PostReadbackPath -Value $value | Out-Null
    return $value
}

function Invoke-P5ELiveReadbackCollectorCore {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][ValidateSet('Before', 'After')][string]$CollectionPhase,
        [Parameter(Mandatory = $true)][string]$ExpectedHelperSha256,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$ApkSigner
    )
    $script:P5EReadOnlyCommandCount = 0L
    [void](Assert-P5EHelperRuntimeHash -ExpectedSha256 $ExpectedHelperSha256)
    $directory = Test-P5EExactEventDirectory -Directory $EvidenceDirectory
    $plan = Read-P5EEventPlan -EvidenceDirectory $directory
    Initialize-P5ECollectorCommandLog -EvidenceDirectory $directory -CollectionPhase $CollectionPhase
    if ([string](Get-P5EProperty $plan 'helperSha256') -cne $ExpectedHelperSha256.ToLowerInvariant()) {
        throw 'P5E_COLLECTOR_EVENT_HELPER_HASH_MISMATCH'
    }
    if ([string](Get-P5EProperty $plan 'serial') -cne $SerialValue) {
        throw 'P5E_COLLECTOR_SERIAL_MISMATCH'
    }
    $sourceContract = Test-P5ERequiredSourceContract -RepoRoot (Get-P5ERepoRoot)
    if (-not $sourceContract.Passed) {
        throw ('P5E_COLLECTOR_SOURCE_CONTRACT_STOP:' + ($sourceContract.Errors -join ','))
    }
    [void](Get-P5EArtifactContract)
    $eventPlanPath = Get-P5EEventPlanPath -Directory $directory
    $postReadbackPath = Join-Path $directory 'post-readback.json'
    if ($CollectionPhase -eq 'Before') {
        $production = Get-P5EPackageReadback -AdbPath $AdbPath -SerialValue $SerialValue -EvidenceDirectory $directory -PackageName $script:P5ETargetPackage -ExpectedApkSha256 ([string](Get-P5EProperty $plan 'productionApkSha256')) -LocalFileName 'installed-production-before.apk' -Name 'production-before' -ApkSigner $ApkSigner -ExpectedVersionCode $script:P5EProductionVersionCode -ExpectedVersion $script:P5EProductionVersion
        $testPackage = Get-P5EPackageReadback -AdbPath $AdbPath -SerialValue $SerialValue -EvidenceDirectory $directory -PackageName $script:P5ETestPackage -ExpectedApkSha256 ([string](Get-P5EProperty $plan 'testApkSha256')) -LocalFileName 'installed-test-before.apk' -Name 'test-before' -ApkSigner $ApkSigner
        $database = Get-P5EConsistentDatabaseReadback -AdbPath $AdbPath -SerialValue $SerialValue
        Assert-P5ECollectorBinding -Binding (Get-P5EProperty $database 'binding')
        if ([string](Get-P5EProperty $database 'databaseSha256') -cne $script:P5EDatabaseSha256) {
            throw 'P5E_COLLECTOR_PRELIVE_DATABASE_HASH_MISMATCH'
        }
        Assert-P5ECollectorZeroBefore -Database $database
        $freshTuple = New-P5ECollectorFreshTuple -Binding (Get-P5EProperty $database 'binding')
        $testPackage['sourceCommit'] = $script:P5ETestSourceCommit
        $input = New-P5ECollectorInput -Plan $plan -Production $production -Test $testPackage -FreshTuple $freshTuple
        $observed = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $beforeSnapshot = New-P5ECollectorSnapshot -Plan $plan -Database $database -Phase Before -ObservedAtMillis $observed
        Write-P5ECollectorJsonImmutable -Path (Join-Path $directory 'collector-input.json') -Value $input | Out-Null
        Write-P5ECollectorJsonImmutable -Path (Join-Path $directory 'before-snapshot.json') -Value $beforeSnapshot | Out-Null
        [void](Write-P5ECollectorOutcome -EvidenceDirectory $directory -CollectionPhase 'Before' -TypedOutcome 'BEFORE_STATE_CAPTURED' -ReadOnlyCommandCount $script:P5EReadOnlyCommandCount)
        Write-Output ('P5E_COLLECTOR_BEFORE_CAPTURED=' + $directory)
        return [pscustomobject]@{ TypedOutcome = 'BEFORE_STATE_CAPTURED'; ReadbackPath = ''; ReadOnlyCommandCount = $script:P5EReadOnlyCommandCount }
    }

    $metadataPath = Join-Path $directory 'HOST_RUN_METADATA.json'
    if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
        throw 'P5E_COLLECTOR_HOST_METADATA_MISSING'
    }
    try { $metadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json }
    catch { throw 'P5E_COLLECTOR_HOST_METADATA_INVALID' }
    if ([string](Get-P5EProperty $metadata 'eventId') -cne [string](Get-P5EProperty $plan 'eventId') -or -not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $metadata 'evidenceDirectory')) -Right $directory)) {
        throw 'P5E_COLLECTOR_HOST_METADATA_EVENT_MISMATCH'
    }
    $inputPath = Join-Path $directory 'collector-input.json'
    $beforePath = Join-Path $directory 'before-snapshot.json'
    if (-not (Test-Path -LiteralPath $inputPath -PathType Leaf) -or -not (Test-Path -LiteralPath $beforePath -PathType Leaf)) {
        throw 'P5E_COLLECTOR_BEFORE_EVIDENCE_MISSING'
    }
    try {
        $input = Get-Content -Raw -LiteralPath $inputPath | ConvertFrom-Json
        $beforeSnapshot = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json
    } catch { throw 'P5E_COLLECTOR_BEFORE_EVIDENCE_INVALID' }
    if ([string](Get-P5EProperty $input 'eventId') -cne [string](Get-P5EProperty $plan 'eventId') -or [string](Get-P5EProperty $input 'serial') -cne $SerialValue -or [string](Get-P5EProperty $input 'eventPlanSha256') -cne (Get-P5ESha256 -Path $eventPlanPath) -or [string](Get-P5EProperty $input 'collectorImplementationId') -cne $script:P5ECollectorImplementationId -or [string](Get-P5EProperty $input 'collectorImplementationVersion') -cne $script:P5ECollectorImplementationVersion) {
        throw 'P5E_COLLECTOR_INPUT_EVENT_BINDING_INVALID'
    }
    if ([string](Get-P5EProperty $beforeSnapshot 'eventId') -cne [string](Get-P5EProperty $plan 'eventId') -or [string](Get-P5EProperty $beforeSnapshot 'runIdentity') -cne $script:P5ERunDeclarationIdentity) {
        throw 'P5E_COLLECTOR_BEFORE_SNAPSHOT_EVENT_INVALID'
    }
    $production = Get-P5EPackageReadback -AdbPath $AdbPath -SerialValue $SerialValue -EvidenceDirectory $directory -PackageName $script:P5ETargetPackage -ExpectedApkSha256 ([string](Get-P5EProperty $plan 'productionApkSha256')) -LocalFileName 'installed-production-after.apk' -Name 'production-after' -ApkSigner $ApkSigner -ExpectedVersionCode $script:P5EProductionVersionCode -ExpectedVersion $script:P5EProductionVersion
    $testPackage = Get-P5EPackageReadback -AdbPath $AdbPath -SerialValue $SerialValue -EvidenceDirectory $directory -PackageName $script:P5ETestPackage -ExpectedApkSha256 ([string](Get-P5EProperty $plan 'testApkSha256')) -LocalFileName 'installed-test-after.apk' -Name 'test-after' -ApkSigner $ApkSigner
    Assert-P5ECollectorStablePackage -Expected (Get-P5EProperty $input 'production') -Actual $production -Name 'production'
    $testPackage['sourceCommit'] = $script:P5ETestSourceCommit
    Assert-P5ECollectorStablePackage -Expected (Get-P5EProperty $input 'test') -Actual $testPackage -Name 'test'
    $database = Get-P5EConsistentDatabaseReadback -AdbPath $AdbPath -SerialValue $SerialValue
    Assert-P5ECollectorBinding -Binding (Get-P5EProperty $database 'binding')
    $freshTuple = New-P5ECollectorFreshTuple -Binding (Get-P5EProperty $database 'binding')
    if ((ConvertTo-P5ECanonicalJson -Value $freshTuple) -cne (ConvertTo-P5ECanonicalJson -Value (Get-P5EProperty $input 'freshTuple'))) {
        throw 'P5E_COLLECTOR_FRESH_TUPLE_CHANGED'
    }
    $afterObserved = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $afterSnapshot = New-P5ECollectorSnapshot -Plan $plan -Database $database -Phase After -ObservedAtMillis $afterObserved
    Write-P5ECollectorJsonImmutable -Path (Join-Path $directory 'after-snapshot.json') -Value $afterSnapshot | Out-Null

    $attempt = Get-P5EProperty $database 'attempt'
    $lineage = Get-P5EProperty $database 'lineage'
    $launchCount = [long](Get-P5EProperty $metadata 'launchCount')
    $partial = [long](Get-P5EProperty $lineage 'partialArtifactPair')
    $scopedRows = [long](Get-P5EProperty $lineage 'attempts') + [long](Get-P5EProperty $lineage 'authorizationReceipts') + [long](Get-P5EProperty $lineage 'lifecycle')
    if ($null -eq $attempt) {
        $status = if ($launchCount -eq 0 -and $scopedRows -eq 0 -and $partial -eq 0) { 'NOT_CLAIMED' } else { 'EXTERNAL_CALL_STATE_UNKNOWN' }
        $reason = if ($status -eq 'NOT_CLAIMED') { 'P5E_NO_CLAIM_OBSERVED' } else { 'P5E_POST_DISPATCH_DURABLE_STATE_INCOMPLETE' }
        $minimal = New-P5ECollectorMinimalReadback -Plan $plan -CollectionPhase 'After' -Status $status -ObservedAtMillis $afterObserved -PostReadbackPath $postReadbackPath -ReasonCode $reason
        [void](Write-P5ECollectorOutcome -EvidenceDirectory $directory -CollectionPhase 'After' -TypedOutcome $(if ($status -eq 'NOT_CLAIMED') { 'NO_CLAIM_OBSERVED' } else { 'EXTERNAL_CALL_STATE_UNKNOWN' }) -Detail $reason -ReadOnlyCommandCount $script:P5EReadOnlyCommandCount -PostReadbackPath $postReadbackPath)
        return [pscustomobject]@{ TypedOutcome = if ($status -eq 'NOT_CLAIMED') { 'NO_CLAIM_OBSERVED' } else { 'EXTERNAL_CALL_STATE_UNKNOWN' }; ReadbackPath = $postReadbackPath; ReadOnlyCommandCount = $script:P5EReadOnlyCommandCount }
    }
    $attemptStatus = [string](Get-P5EProperty $attempt 'status')
    if ($attemptStatus -eq 'RECOVERY_REQUIRED') {
        $minimal = New-P5ECollectorMinimalReadback -Plan $plan -CollectionPhase 'After' -Status 'RECOVERY_REQUIRED' -ObservedAtMillis $afterObserved -PostReadbackPath $postReadbackPath -ReasonCode ([string](Get-P5EProperty $attempt 'recoveryReasonCode'))
        [void](Write-P5ECollectorOutcome -EvidenceDirectory $directory -CollectionPhase 'After' -TypedOutcome 'RECOVERY_REQUIRED' -Detail 'P5E_DURABLE_RECOVERY_REQUIRED' -ReadOnlyCommandCount $script:P5EReadOnlyCommandCount -PostReadbackPath $postReadbackPath)
        return [pscustomobject]@{ TypedOutcome = 'RECOVERY_REQUIRED'; ReadbackPath = $postReadbackPath; ReadOnlyCommandCount = $script:P5EReadOnlyCommandCount }
    }
    if ($attemptStatus -ne 'COMMITTED') {
        $minimal = New-P5ECollectorMinimalReadback -Plan $plan -CollectionPhase 'After' -Status 'CLAIMED' -ObservedAtMillis $afterObserved -PostReadbackPath $postReadbackPath -ReasonCode 'P5E_CLAIMED_STATE_REMAINS_UNKNOWN'
        [void](Write-P5ECollectorOutcome -EvidenceDirectory $directory -CollectionPhase 'After' -TypedOutcome 'EXTERNAL_CALL_STATE_UNKNOWN' -Detail 'P5E_CLAIMED_STATE_REMAINS_UNKNOWN' -ReadOnlyCommandCount $script:P5EReadOnlyCommandCount -PostReadbackPath $postReadbackPath)
        return [pscustomobject]@{ TypedOutcome = 'EXTERNAL_CALL_STATE_UNKNOWN'; ReadbackPath = $postReadbackPath; ReadOnlyCommandCount = $script:P5EReadOnlyCommandCount }
    }

    Assert-P5ECollectorAfterLineage -Database $database -BeforeDatabase ([pscustomobject]@{ globalCounts = Get-P5EProperty $beforeSnapshot 'globalCounts' })
    if ($null -eq (Get-P5EProperty $database 'authorizationReceipt') -or $null -eq (Get-P5EProperty $database 'lifecycle')) {
        throw 'P5E_COLLECTOR_COMMITTED_PAIR_MISSING'
    }
    $auth = Get-P5EProperty $database 'authorizationReceipt'
    $lifecycle = Get-P5EProperty $database 'lifecycle'
    $expectedFingerprint = Get-P5EAccountFingerprint
    if ([string](Get-P5EProperty $auth 'endpointAccountFingerprint') -cne $expectedFingerprint) {
        throw 'P5E_COLLECTOR_ACCOUNT_FINGERPRINT_MISMATCH'
    }
    $issued = [long](Get-P5EProperty $auth 'issuedAtMillis')
    $expires = [long](Get-P5EProperty $auth 'expiresAtMillis')
    $consumed = [long](Get-P5EProperty $auth 'consumedAtMillis')
    $created = [long](Get-P5EProperty $attempt 'createdAtMillis')
    $updated = [long](Get-P5EProperty $attempt 'updatedAtMillis')
    $beforeObserved = [long](Get-P5EProperty $beforeSnapshot 'observedAtMillis')
    if ($consumed -lt $issued -or $consumed -ge $expires) { throw 'P5E_COLLECTOR_AUTHORIZATION_WINDOW_INVALID' }
    if ($created -lt $beforeObserved -or $created -gt $consumed -or $updated -lt $created -or $updated -gt $afterObserved) {
        throw 'P5E_COLLECTOR_DURABLE_TIMING_INVALID'
    }
    if ([string](Get-P5EProperty $auth 'attemptIdentity') -cne $script:P5EAttemptIdentity -or [string](Get-P5EProperty $auth 'consumptionResult') -cne 'CONSUMED' -or [string](Get-P5EProperty $auth 'consumedAttemptIdentity') -cne $script:P5EAttemptIdentity -or [string](Get-P5EProperty $lifecycle 'attemptIdentity') -cne $script:P5EAttemptIdentity -or [string](Get-P5EProperty $lifecycle 'stage') -cne 'RESPONSE_BODY_COMPLETE') {
        throw 'P5E_COLLECTOR_DURABLE_TUPLE_INVALID'
    }
    $reportBytes = Get-P5EHexBytes -Hex ([string](Get-P5EProperty $attempt 'reportHex')) -Name 'report-bytes'
    $receiptBytes = Get-P5EHexBytes -Hex ([string](Get-P5EProperty $attempt 'receiptHex')) -Name 'receipt-bytes'
    if ([long](Get-P5EProperty $attempt 'reportByteLength') -ne $reportBytes.Length -or [long](Get-P5EProperty $attempt 'receiptByteLength') -ne $receiptBytes.Length) {
        throw 'P5E_COLLECTOR_STORED_ARTIFACT_LENGTH_MISMATCH'
    }
    $reportPath = Join-Path $directory 'report.bin'
    $receiptPath = Join-Path $directory 'receipt.bin'
    Write-P5EImmutableBytes -Path $reportPath -Bytes $reportBytes -Name 'report'
    Write-P5EImmutableBytes -Path $receiptPath -Bytes $receiptBytes -Name 'receipt'
    $reportErrors = [System.Collections.Generic.List[string]]::new()
    $receiptErrors = [System.Collections.Generic.List[string]]::new()
    $reportChecked = Test-P5ESerializedArtifactBytes -Path $reportPath -ExpectedSchema 'safe4.full.report-l1.v1' -Kind 'report' -Errors $reportErrors
    $receiptChecked = Test-P5ESerializedArtifactBytes -Path $receiptPath -ExpectedSchema 'safe4.full.receipt.v1' -Kind 'receipt' -Errors $receiptErrors
    if ($reportErrors.Count -ne 0) { throw ('P5E_COLLECTOR_REPORT_VALIDATION_FAILED:' + ($reportErrors -join ',')) }
    if ($receiptErrors.Count -ne 0) { throw ('P5E_COLLECTOR_RECEIPT_VALIDATION_FAILED:' + ($receiptErrors -join ',')) }
    $pairErrors = [System.Collections.Generic.List[string]]::new()
    Test-P5EArtifactPair -Report $reportChecked.Parsed -Receipt $receiptChecked.Parsed -Errors $pairErrors
    if ($pairErrors.Count -ne 0) { throw ('P5E_COLLECTOR_ARTIFACT_PAIR_INVALID:' + ($pairErrors -join ',')) }
    $metricsText = [string](Get-P5EProperty $attempt 'metrics')
    try { $metrics = $metricsText | ConvertFrom-Json } catch { throw 'P5E_COLLECTOR_METRICS_JSON_INVALID' }
    $transaction = New-P5ECollectorTransactionEvidence -Plan $plan -BeforeAfterConsistent $true -RowPairConsistent $true -ClaimAndAuthorizationAtomic $true -ReportReceiptAtomic $true -NoUnrelatedWrites $true -NoDeletes $true
    $transactionPath = Join-Path $directory 'transaction-evidence.json'
    Write-P5ECollectorJsonImmutable -Path $transactionPath -Value $transaction | Out-Null
    $reportArtifact = New-P5EArtifactMetadataFromValue -Value $reportChecked.Parsed -Path $reportPath -Kind 'report'
    $receiptArtifact = New-P5EArtifactMetadataFromValue -Value $receiptChecked.Parsed -Path $receiptPath -Kind 'receipt'
    $attemptValue = [ordered]@{
        rowCount = 1L
        attemptIdentity = [string](Get-P5EProperty $attempt 'attemptIdentity')
        requestIdentity = [string](Get-P5EProperty $attempt 'requestIdentity')
        bindingIdentity = [string](Get-P5EProperty $attempt 'bindingIdentity')
        runDeclarationIdentity = [string](Get-P5EProperty $attempt 'runDeclarationIdentity')
        chapterKey = [string](Get-P5EProperty $attempt 'chapterKey')
        phase = [string](Get-P5EProperty $attempt 'phase')
        predecessorIdentity = [string](Get-P5EProperty $attempt 'predecessorIdentity')
        requestEnvelopeHash = [string](Get-P5EProperty $attempt 'requestEnvelopeHash')
        provider = [string](Get-P5EProperty $attempt 'provider')
        model = [string](Get-P5EProperty $attempt 'model')
        status = 'COMMITTED'
        responseIdentity = [string](Get-P5EProperty $attempt 'responseIdentity')
        createdAtMillis = $created
        updatedAtMillis = $updated
        reportByteLength = [long]$reportChecked.Length
        reportSha256 = [string]$reportChecked.Sha256
        receiptByteLength = [long]$receiptChecked.Length
        receiptSha256 = [string]$receiptChecked.Sha256
        metrics = $metrics
    }
    $lifecycleValue = [ordered]@{
        rowCount = 1L
        attemptIdentity = [string](Get-P5EProperty $lifecycle 'attemptIdentity')
        stage = [string](Get-P5EProperty $lifecycle 'stage')
        requestBodyBytes = [long](Get-P5EProperty $lifecycle 'requestBodyBytes')
        responseBodyBytes = [long](Get-P5EProperty $lifecycle 'responseBodyBytes')
        httpStatus = [int](Get-P5EProperty $lifecycle 'httpStatus')
        responseContentType = [string](Get-P5EProperty $lifecycle 'responseContentType')
        exceptionClass = [string](Get-P5EProperty $lifecycle 'exceptionClass')
        elapsedMillis = [long](Get-P5EProperty $lifecycle 'elapsedMillis')
        generationId = [string](Get-P5EProperty $lifecycle 'generationId')
        providerResponseId = [string](Get-P5EProperty $lifecycle 'providerResponseId')
        cancellationSource = [string](Get-P5EProperty $lifecycle 'cancellationSource')
    }
    $beforeTuple = [string](Get-P5EProperty $beforeSnapshot 'immutableTupleIdentity')
    $afterTuple = [string](Get-P5EProperty $afterSnapshot 'immutableTupleIdentity')
    $settingsChanged = [string](Get-P5EProperty $beforeSnapshot 'settingsSha256') -cne [string](Get-P5EProperty $afterSnapshot 'settingsSha256')
    $tupleChanged = $beforeTuple -cne $afterTuple
    $integrity = [ordered]@{
        allowedDiff = -not $settingsChanged -and -not $tupleChanged
        unrelatedWrites = 0L
        bindingChanged = $tupleChanged
        sourceChanged = $tupleChanged
        runDeclarationChanged = $tupleChanged
        settingsChanged = $settingsChanged
        packChanged = $tupleChanged
        profileChanged = $tupleChanged
        freshTupleChanged = $tupleChanged
        atomicClaim = [bool](Get-P5EProperty $transaction 'claimAndAuthorizationAtomic')
        reportReceiptAtomic = [bool](Get-P5EProperty $transaction 'reportReceiptAtomic')
        noDeletes = [bool](Get-P5EProperty $transaction 'noDeletes')
    }
    if (-not $integrity.allowedDiff) { throw 'P5E_COLLECTOR_IMMUTABLE_OR_SETTINGS_CHANGED' }
    $lineageBeforeValue = [ordered]@{}
    $lineageAfterValue = [ordered]@{}
    foreach ($name in @('attempts', 'authorizationReceipts', 'reconciliation', 'reconciliationHistory', 'lifecycle', 'reportOrReceipt')) {
        $lineageBeforeValue[$name] = [long](Get-P5EProperty (Get-P5EProperty $beforeSnapshot 'lineage') $name)
        $lineageAfterValue[$name] = [long](Get-P5EProperty (Get-P5EProperty $afterSnapshot 'lineage') $name)
    }
    $readback = [ordered]@{
        schemaVersion = $script:P5EReadbackSchema
        observedAtMillis = $afterObserved
        externalCallState = 'COMMITTED'
        production = Get-P5EProperty $input 'production'
        test = Get-P5EProperty $input 'test'
        database = [ordered]@{
            beforeSha256 = [string](Get-P5EProperty $beforeSnapshot 'databaseSha256')
            afterSha256 = [string](Get-P5EProperty $afterSnapshot 'databaseSha256')
            schemaVersion = [long](Get-P5EProperty $afterSnapshot 'databaseSchemaVersion')
            integrityCheck = [string](Get-P5EProperty $afterSnapshot 'integrityCheck')
            foreignKeyViolations = [long](Get-P5EProperty $afterSnapshot 'foreignKeyViolations')
        }
        freshTuple = $freshTuple
        lineageBefore = $lineageBeforeValue
        lineageAfter = $lineageAfterValue
        attempt = $attemptValue
        authorizationReceipt = $auth
        lifecycle = $lifecycleValue
        artifacts = [ordered]@{ report = $reportArtifact; receipt = $receiptArtifact }
        integrity = $integrity
        provenance = [ordered]@{
            schemaVersion = $script:P5EReadbackProvenanceSchema
            sourceMappingVersion = $script:P5ESourceMappingVersion
            collectorImplementationId = $script:P5ECollectorImplementationId
            collectorImplementationVersion = $script:P5ECollectorImplementationVersion
            collectorImplementationPath = Get-P5ECanonicalPath -Path (Get-P5ECollectorPath)
            collectorImplementationSha256 = Get-P5ESha256 -Path (Get-P5ECollectorPath)
            eventId = [string](Get-P5EProperty $plan 'eventId')
            evidenceDirectory = $directory
            metadataPath = Get-P5ECanonicalPath -Path $metadataPath
            postReadbackPath = Get-P5ECanonicalPath -Path $postReadbackPath
            runIdentity = $script:P5ERunDeclarationIdentity
            sourceInputPath = Get-P5ECanonicalPath -Path $inputPath
            sourceInputSha256 = Get-P5ESha256 -Path $inputPath
            beforeSnapshotPath = Get-P5ECanonicalPath -Path $beforePath
            beforeSnapshotSha256 = Get-P5ESha256 -Path $beforePath
            afterSnapshotPath = Get-P5ECanonicalPath -Path (Join-Path $directory 'after-snapshot.json')
            afterSnapshotSha256 = Get-P5ESha256 -Path (Join-Path $directory 'after-snapshot.json')
            transactionEvidencePath = Get-P5ECanonicalPath -Path $transactionPath
            transactionEvidenceSha256 = Get-P5ESha256 -Path $transactionPath
            reportBytesPath = Get-P5ECanonicalPath -Path $reportPath
            reportBytesSha256 = [string]$reportChecked.Sha256
            reportBytesLength = [long]$reportChecked.Length
            receiptBytesPath = Get-P5ECanonicalPath -Path $receiptPath
            receiptBytesSha256 = [string]$receiptChecked.Sha256
            receiptBytesLength = [long]$receiptChecked.Length
            runStartedAtMillis = [long](Get-P5EProperty $metadata 'authorizationIssuedAtMillis')
            beforeSnapshotObservedAtMillis = $beforeObserved
            claimObservedAtMillis = $created
            consumedAtMillis = $consumed
            attemptCreatedAtMillis = $created
            attemptUpdatedAtMillis = $updated
            observedAtMillis = $afterObserved
            collectedAtMillis = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
            atomicityEvidence = [ordered]@{
                transactionEvidencePath = Get-P5ECanonicalPath -Path $transactionPath
                transactionEvidenceSha256 = Get-P5ESha256 -Path $transactionPath
                transactionSemanticsPassed = [bool](Get-P5EProperty $transaction 'transactionSemanticsPassed')
                beforeAfterConsistent = [bool](Get-P5EProperty $transaction 'beforeAfterConsistent')
                rowPairConsistent = [bool](Get-P5EProperty $transaction 'rowPairConsistent')
                claimAndAuthorizationAtomic = [bool](Get-P5EProperty $transaction 'claimAndAuthorizationAtomic')
                reportReceiptAtomic = [bool](Get-P5EProperty $transaction 'reportReceiptAtomic')
                reportBytesValidated = $true
                receiptBytesValidated = $true
                validatorId = $script:P5ESyntheticArtifactValidatorId
                sourceTransactionCodePath = [string](Get-P5EProperty $transaction 'sourceTransactionCodePath')
                sourceTransactionCodeSha256 = [string](Get-P5EProperty $transaction 'sourceTransactionCodeSha256')
                sourceTransactionTestPath = [string](Get-P5EProperty $transaction 'sourceTransactionTestPath')
                sourceTransactionTestSha256 = [string](Get-P5EProperty $transaction 'sourceTransactionTestSha256')
                sourceTransactionTestStatus = [string](Get-P5EProperty $transaction 'sourceTransactionTestStatus')
            }
        }
    }
    Write-P5ECollectorJsonImmutable -Path $postReadbackPath -Value $readback | Out-Null
    [void](Write-P5ECollectorOutcome -EvidenceDirectory $directory -CollectionPhase 'After' -TypedOutcome 'COMMITTED_READBACK_CAPTURED' -ReadOnlyCommandCount $script:P5EReadOnlyCommandCount -PostReadbackPath $postReadbackPath)
    Write-Output ('P5E_COLLECTOR_AFTER_CAPTURED=' + $directory)
    return [pscustomobject]@{ TypedOutcome = 'COMMITTED_READBACK_CAPTURED'; ReadbackPath = $postReadbackPath; ReadOnlyCommandCount = $script:P5EReadOnlyCommandCount }
}

function Invoke-P5ELiveReadbackCollector {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][ValidateSet('Before', 'After')][string]$CollectionPhase,
        [Parameter(Mandatory = $true)][string]$ExpectedHelperSha256,
        [Parameter(Mandatory = $true)][string]$SerialValue,
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$ApkSigner
    )
    try {
        return Invoke-P5ELiveReadbackCollectorCore -EvidenceDirectory $EvidenceDirectory -CollectionPhase $CollectionPhase -ExpectedHelperSha256 $ExpectedHelperSha256 -SerialValue $SerialValue -AdbPath $AdbPath -ApkSigner $ApkSigner
    } catch {
        $detail = [string]$_.Exception.Message
        try {
            [void](Write-P5ECollectorOutcome -EvidenceDirectory $EvidenceDirectory -CollectionPhase $CollectionPhase -TypedOutcome 'COLLECTOR_TYPED_STOP' -Detail $detail -ReadOnlyCommandCount $script:P5EReadOnlyCommandCount)
        } catch { }
        throw
    }
}

function New-P5EUniqueEvidenceDirectory {
    param([Parameter(Mandatory = $true)][string]$Root)
    if (-not (Test-Path -LiteralPath $Root -PathType Container)) { [void](New-Item -ItemType Directory -Path $Root) }
    $name = 'raw-live-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff') + '-' +
        [Guid]::NewGuid().ToString('N')
    $directory = Join-Path $Root $name
    [void](New-Item -ItemType Directory -Path $directory -ErrorAction Stop)
    return $directory
}

function New-P5EHostMetadata {
    param(
        [Parameter(Mandatory = $true)]$Run,
        [Parameter(Mandatory = $true)][long]$IssuedAtMillis,
        [Parameter(Mandatory = $true)][long]$ExpiresAtMillis,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [string]$MetadataPath
    )
    if ([string]::IsNullOrWhiteSpace($MetadataPath)) {
        $MetadataPath = Join-Path $EvidenceDirectory 'HOST_RUN_METADATA.json'
    }
    $eventId = Split-Path -Leaf (Get-P5ECanonicalPath -Path $EvidenceDirectory)
    return [ordered]@{
        schemaVersion = $script:P5EHostRunSchema
        eventId = $eventId
        runIdentity = $script:P5ERunDeclarationIdentity
        metadataPath = Get-P5ECanonicalPath -Path $MetadataPath
        classMethod = $script:P5EClassMethod
        serial = $script:P5ESerial
        testPackage = $script:P5ETestPackage
        targetPackage = $script:P5ETargetPackage
        runner = $script:P5ERunner
        observationTimeoutMillis = $script:P5EHostObservationTimeoutMilliseconds
        authorizationValidityMillis = $script:P5EAuthorizationValidityMilliseconds
        maximumExecutionTimeMillis = $script:P5EExecutionDeadlineMilliseconds
        authorizationIssuedAtMillis = $IssuedAtMillis
        authorizationExpiresAtMillis = $ExpiresAtMillis
        processExitCode = $Run.ExitCode
        launchCount = $Run.LaunchCount
        dispatchCount = $Run.DispatchCount
        timedOut = [bool]$Run.TimedOut
        redactionPass = -not [bool]$Run.RedactionViolation
        redactionViolation = [bool]$Run.RedactionViolation
        externalCallState = if ($Run.TimedOut -or $Run.LaunchCount -eq 0 -or
            ($null -ne $Run.ExitCode -and $Run.ExitCode -ne 0)) { 'UNKNOWN' }
            else { 'POST_READBACK_REQUIRED' }
        rawAcceptance = 'NOT_PROVEN'
        evidenceDirectory = $EvidenceDirectory
    }
}

function Get-P5EEventPlanPath {
    param([Parameter(Mandatory = $true)][string]$Directory)
    return Join-Path (Get-P5ECanonicalPath -Path $Directory) 'EVENT_PLAN.json'
}

function Get-P5ECollectorOutcomePath {
    param([Parameter(Mandatory = $true)][string]$Directory)
    return Join-Path (Get-P5ECanonicalPath -Path $Directory) 'COLLECTOR_OUTCOME.json'
}

function Test-P5EExactEventDirectory {
    param([Parameter(Mandatory = $true)][string]$Directory)
    if (-not (Test-Path -LiteralPath $Directory -PathType Container)) {
        [void](New-Item -ItemType Directory -Path $Directory -ErrorAction Stop)
    }
    $item = Get-Item -LiteralPath $Directory -Force
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw 'P5E_EVENT_DIRECTORY_REPARSE_STOP'
    }
    return Get-P5ECanonicalPath -Path $Directory
}

function New-P5EEventPlan {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][string]$ManifestHash,
        [Parameter(Mandatory = $true)][string]$ProductionApkHash,
        [Parameter(Mandatory = $true)][string]$TestApkHash,
        [Parameter(Mandatory = $true)][string]$HelperHash
    )
    $directory = Test-P5EExactEventDirectory -Directory $EvidenceDirectory
    $eventId = Split-Path -Leaf $directory
    return [ordered]@{
        schemaVersion = $script:P5EEventPlanSchema
        eventId = $eventId
        evidenceDirectory = $directory
        serial = $script:P5ESerial
        targetPackage = $script:P5ETargetPackage
        testPackage = $script:P5ETestPackage
        runner = $script:P5ERunner
        selectedMethod = $script:P5EClassMethod
        runIdentity = $script:P5ERunDeclarationIdentity
        attemptIdentity = $script:P5EAttemptIdentity
        requestIdentity = $script:P5ERequestIdentity
        bindingIdentity = $script:P5EBindingIdentity
        chapterKey = $script:P5EChapterKey
        phase = $script:P5EPhase
        manifestSha256 = $ManifestHash.ToLowerInvariant()
        productionApkSha256 = $ProductionApkHash.ToLowerInvariant()
        testApkSha256 = $TestApkHash.ToLowerInvariant()
        certificateSha256 = $script:P5ECertificateSha256
        helperSha256 = $HelperHash.ToLowerInvariant()
        collectorImplementationId = $script:P5ECollectorImplementationId
        collectorImplementationVersion = $script:P5ECollectorImplementationVersion
        collectionMode = $script:P5ECollectorReadbackMode
        authorizationWindow = 'FRESH_ISSUED_AT_DISPATCH_HALF_OPEN'
        authorizationValidityMillis = $script:P5EAuthorizationValidityMilliseconds
        executionDeadlineMillis = $script:P5EExecutionDeadlineMilliseconds
        hostObservationTimeoutMillis = $script:P5EHostObservationTimeoutMilliseconds
        plannedFilenames = [ordered]@{
            eventPlan = 'EVENT_PLAN.json'
            beforeSnapshot = 'before-snapshot.json'
            afterSnapshot = 'after-snapshot.json'
            transactionEvidence = 'transaction-evidence.json'
            instrumentationStdout = 'instrumentation-stdout.txt'
            instrumentationStderr = 'instrumentation-stderr.txt'
            hostMetadata = 'HOST_RUN_METADATA.json'
            postReadback = 'post-readback.json'
            outcome = 'OUTCOME_VERIFICATION.json'
            collectorOutcome = 'COLLECTOR_OUTCOME.json'
            collectorCommandLog = $script:P5ECollectorCommandLogFileName
            reportBytes = 'report.bin'
            receiptBytes = 'receipt.bin'
            installedProductionApk = 'installed-production.apk'
            installedTestApk = 'installed-test.apk'
        }
        noRedispatch = $true
        noProviderFromCollector = $true
        noMutationFromCollector = $true
    }
}

function Write-P5EEventPlan {
    param([Parameter(Mandatory = $true)]$Plan)
    $path = Get-P5EEventPlanPath -Directory ([string](Get-P5EProperty $Plan 'evidenceDirectory'))
    if (Test-Path -LiteralPath $path -PathType Leaf) { throw 'P5E_EVENT_PLAN_ALREADY_EXISTS_STOP' }
    Write-P5EUtf8NoBom -Path $path -Text (ConvertTo-P5ECanonicalJson -Value $Plan)
    return Get-P5ECanonicalPath -Path $path
}

function Read-P5EEventPlan {
    param([Parameter(Mandatory = $true)][string]$EvidenceDirectory)
    $directory = Test-P5EExactEventDirectory -Directory $EvidenceDirectory
    $path = Get-P5EEventPlanPath -Directory $directory
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'P5E_EVENT_PLAN_MISSING_STOP' }
    try { $plan = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json }
    catch { throw 'P5E_EVENT_PLAN_INVALID_JSON_STOP' }
    $required = @('schemaVersion', 'eventId', 'evidenceDirectory', 'serial', 'targetPackage', 'testPackage',
        'runner', 'selectedMethod', 'runIdentity', 'attemptIdentity', 'requestIdentity', 'bindingIdentity',
        'chapterKey', 'phase', 'manifestSha256', 'productionApkSha256', 'testApkSha256', 'certificateSha256',
        'helperSha256', 'collectorImplementationId', 'collectorImplementationVersion', 'collectionMode',
        'authorizationWindow', 'authorizationValidityMillis', 'executionDeadlineMillis',
        'hostObservationTimeoutMillis', 'plannedFilenames', 'noRedispatch', 'noProviderFromCollector',
        'noMutationFromCollector')
    $errors = [System.Collections.Generic.List[string]]::new()
    Test-P5EObjectShape -Object $plan -Path 'eventPlan' -Required $required -Allowed $required -Errors $errors | Out-Null
    Test-P5EEqual $plan 'schemaVersion' $script:P5EEventPlanSchema 'eventPlan' $errors
    Test-P5EEqual $plan 'eventId' (Split-Path -Leaf $directory) 'eventPlan' $errors
    if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $plan 'evidenceDirectory')) -Right $directory)) { Add-P5EError $errors 'EVENT_PLAN_DIRECTORY_MISMATCH' }
    Test-P5EEqual $plan 'serial' $script:P5ESerial 'eventPlan' $errors
    Test-P5EEqual $plan 'targetPackage' $script:P5ETargetPackage 'eventPlan' $errors
    Test-P5EEqual $plan 'testPackage' $script:P5ETestPackage 'eventPlan' $errors
    Test-P5EEqual $plan 'runner' $script:P5ERunner 'eventPlan' $errors
    Test-P5EEqual $plan 'selectedMethod' $script:P5EClassMethod 'eventPlan' $errors
    Test-P5EEqual $plan 'runIdentity' $script:P5ERunDeclarationIdentity 'eventPlan' $errors
    Test-P5EEqual $plan 'attemptIdentity' $script:P5EAttemptIdentity 'eventPlan' $errors
    Test-P5EEqual $plan 'requestIdentity' $script:P5ERequestIdentity 'eventPlan' $errors
    Test-P5EEqual $plan 'bindingIdentity' $script:P5EBindingIdentity 'eventPlan' $errors
    Test-P5EEqual $plan 'chapterKey' $script:P5EChapterKey 'eventPlan' $errors
    Test-P5EEqual $plan 'phase' $script:P5EPhase 'eventPlan' $errors
    foreach ($name in @('manifestSha256', 'productionApkSha256', 'testApkSha256', 'certificateSha256', 'helperSha256')) {
        Test-P5ESha256 $plan $name 'eventPlan' $errors
    }
    Test-P5EEqual $plan 'collectorImplementationId' $script:P5ECollectorImplementationId 'eventPlan' $errors
    Test-P5EEqual $plan 'collectorImplementationVersion' $script:P5ECollectorImplementationVersion 'eventPlan' $errors
    Test-P5EEqual $plan 'collectionMode' $script:P5ECollectorReadbackMode 'eventPlan' $errors
    $planned = Get-P5EProperty $plan 'plannedFilenames'
    $plannedRequired = @('eventPlan', 'beforeSnapshot', 'afterSnapshot', 'transactionEvidence',
        'instrumentationStdout', 'instrumentationStderr', 'hostMetadata', 'postReadback',
        'outcome', 'collectorOutcome', 'collectorCommandLog', 'reportBytes', 'receiptBytes',
        'installedProductionApk', 'installedTestApk')
    Test-P5EObjectShape -Object $planned -Path 'eventPlan.plannedFilenames' -Required $plannedRequired -Allowed $plannedRequired -Errors $errors | Out-Null
    if ($null -ne $planned) {
        Test-P5EEqual $planned 'collectorCommandLog' $script:P5ECollectorCommandLogFileName 'eventPlan.plannedFilenames' $errors
    }
    Test-P5ELong $plan 'authorizationValidityMillis' $script:P5EAuthorizationValidityMilliseconds 'eventPlan' $errors -Minimum 1 | Out-Null
    Test-P5ELong $plan 'executionDeadlineMillis' $script:P5EExecutionDeadlineMilliseconds 'eventPlan' $errors -Minimum 1 | Out-Null
    Test-P5ELong $plan 'hostObservationTimeoutMillis' $script:P5EHostObservationTimeoutMilliseconds 'eventPlan' $errors -Minimum 1 | Out-Null
    foreach ($name in @('noRedispatch', 'noProviderFromCollector', 'noMutationFromCollector')) { Test-P5EBoolean $plan $name $true 'eventPlan' $errors }
    if ($errors.Count -ne 0) { throw ('P5E_EVENT_PLAN_INVALID_STOP:' + ($errors -join ',')) }
    return $plan
}

function Get-P5EProperty {
    param($Object, [Parameter(Mandatory = $true)][string]$Name)
    if ($null -eq $Object) { return $null }
    if ($Object -is [System.Collections.IDictionary]) {
        if (-not $Object.Contains($Name)) { return $null }
        return $Object[$Name]
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return $property.Value
}

function Test-P5EHasProperty {
    param($Object, [Parameter(Mandatory = $true)][string]$Name)
    if ($Object -is [System.Collections.IDictionary]) {
        return $Object.Contains($Name)
    }
    return $null -ne $Object -and $null -ne $Object.PSObject.Properties[$Name]
}

function Add-P5EError {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors,
        [Parameter(Mandatory = $true)][string]$Code
    )
    [void]$Errors.Add($Code)
}

function Test-P5EObjectShape {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$Required,
        [Parameter(Mandatory = $true)][string[]]$Allowed,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    if ($null -eq $Object) { Add-P5EError -Errors $Errors -Code ('MISSING_OBJECT:' + $Path); return $false }
    $allowedSet = [System.Collections.Generic.HashSet[string]]::new($Allowed)
    foreach ($property in @($Object.PSObject.Properties)) {
        if (-not $allowedSet.Contains($property.Name)) {
            Add-P5EError -Errors $Errors -Code ('UNKNOWN_FIELD:' + $Path + '.' + $property.Name)
        }
    }
    foreach ($name in $Required) {
        if (-not (Test-P5EHasProperty -Object $Object -Name $name)) {
            Add-P5EError -Errors $Errors -Code ('MISSING_FIELD:' + $Path + '.' + $name)
        }
    }
    return $true
}

function Test-P5EEqual {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)]$Expected,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    if (-not (Test-P5EHasProperty -Object $Object -Name $Name)) { return }
    $actual = Get-P5EProperty -Object $Object -Name $Name
    if ([string]$actual -cne [string]$Expected) {
        Add-P5EError -Errors $Errors -Code ('VALUE_MISMATCH:' + $Path + '.' + $Name)
    }
}

function Test-P5EBoolean {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Expected,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    if (-not (Test-P5EHasProperty -Object $Object -Name $Name)) { return }
    $value = Get-P5EProperty -Object $Object -Name $Name
    if (-not ($value -is [bool]) -or [bool]$value -ne $Expected) {
        Add-P5EError -Errors $Errors -Code ('BOOLEAN_MISMATCH:' + $Path + '.' + $Name)
    }
}

function Test-P5ELong {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][long]$Expected,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors,
        [long]$Minimum = [long]::MinValue,
        [long]$Maximum = [long]::MaxValue
    )
    if (-not (Test-P5EHasProperty -Object $Object -Name $Name)) { return $null }
    $value = Get-P5EProperty -Object $Object -Name $Name
    $parsed = 0L
    if (-not [long]::TryParse([string]$value, [Globalization.NumberStyles]::Integer,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$parsed) -or
            $parsed -lt $Minimum -or $parsed -gt $Maximum) {
        Add-P5EError -Errors $Errors -Code ('INTEGER_INVALID:' + $Path + '.' + $Name)
        return $null
    }
    if ($parsed -ne $Expected) {
        Add-P5EError -Errors $Errors -Code ('VALUE_MISMATCH:' + $Path + '.' + $Name)
    }
    return $parsed
}

function Test-P5ELongRange {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors,
        [long]$Minimum = [long]::MinValue,
        [long]$Maximum = [long]::MaxValue
    )
    if (-not (Test-P5EHasProperty -Object $Object -Name $Name)) { return $null }
    $value = Get-P5EProperty -Object $Object -Name $Name
    $parsed = 0L
    if (-not [long]::TryParse([string]$value, [Globalization.NumberStyles]::Integer,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$parsed) -or
            $parsed -lt $Minimum -or $parsed -gt $Maximum) {
        Add-P5EError -Errors $Errors -Code ('INTEGER_INVALID:' + $Path + '.' + $Name)
        return $null
    }
    return $parsed
}

function Test-P5ESha256 {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors,
        [string]$Expected
    )
    if (-not (Test-P5EHasProperty -Object $Object -Name $Name)) { return }
    $value = [string](Get-P5EProperty -Object $Object -Name $Name)
    if ($value -notmatch '^[0-9a-fA-F]{64}$') {
        Add-P5EError -Errors $Errors -Code ('HASH_INVALID:' + $Path + '.' + $Name)
    } elseif (-not [string]::IsNullOrWhiteSpace([string]$Expected) -and
            $value.ToLowerInvariant() -ne $Expected.ToLowerInvariant()) {
        Add-P5EError -Errors $Errors -Code ('VALUE_MISMATCH:' + $Path + '.' + $Name)
    }
}

function Test-P5EDecimalCap {
    param(
        $Object,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][decimal]$Maximum,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    if (-not (Test-P5EHasProperty -Object $Object -Name $Name)) { return $null }
    $parsed = 0D
    $textValue = [string](Get-P5EProperty -Object $Object -Name $Name)
    if (-not [decimal]::TryParse($textValue, [Globalization.NumberStyles]::Number,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$parsed) -or $parsed -lt 0D) {
        Add-P5EError -Errors $Errors -Code ('DECIMAL_INVALID:' + $Path + '.' + $Name)
        return $null
    }
    if ($parsed -gt $Maximum) { Add-P5EError -Errors $Errors -Code ('COST_OVER_CAP:' + $Path + '.' + $Name) }
    return $parsed
}

function Get-P5EInstrumentationObservation {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Text)
    $classes = [System.Collections.Generic.List[string]]::new()
    $tests = [System.Collections.Generic.List[string]]::new()
    $terminalCodes = [System.Collections.Generic.List[int]]::new()
    $malformedTerminal = $false
    foreach ($line in ($Text -split "`r?`n")) {
        if ($line.StartsWith('INSTRUMENTATION_STATUS: ')) {
            $payload = $line.Substring('INSTRUMENTATION_STATUS: '.Length)
            $separator = $payload.IndexOf('=')
            if ($separator -gt 0) {
                $key = $payload.Substring(0, $separator)
                $value = $payload.Substring($separator + 1)
                if ($key -eq 'class') { [void]$classes.Add($value) }
                if ($key -eq 'test') { [void]$tests.Add($value) }
            }
        } elseif ($line.StartsWith('INSTRUMENTATION_CODE:')) {
            $value = $line.Substring('INSTRUMENTATION_CODE:'.Length).Trim()
            $parsed = 0
            if ([int]::TryParse($value, [Globalization.NumberStyles]::Integer,
                    [Globalization.CultureInfo]::InvariantCulture, [ref]$parsed)) {
                [void]$terminalCodes.Add($parsed)
            } else { $malformedTerminal = $true }
        }
    }
    $failure = $Text -match '(?is)FAILURES!!!|There was [0-9]+ failure|AssumptionViolatedException|\bskipped\b'
    return [pscustomobject]@{
        ClassValues = $classes.ToArray()
        TestValues = $tests.ToArray()
        TerminalCodes = $terminalCodes.ToArray()
        MalformedTerminal = $malformedTerminal
        OneTestMarker = $Text -match '(?m)^\s*OK \(1 test\)\s*$'
        FailureMarker = $failure
        ClassMethodExact = $classes.Count -gt 0 -and @($classes | Where-Object { $_ -ne $script:P5EClass }).Count -eq 0
        TestMethodExact = $tests.Count -gt 0 -and @($tests | Where-Object { $_ -ne 'authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn' }).Count -eq 0
        TerminalMinusOne = $terminalCodes.Count -eq 1 -and $terminalCodes[0] -eq -1 -and -not $malformedTerminal
    }
}

function Invoke-P5EOutcomeVerifier {
    param(
        [Parameter(Mandatory = $true)][string]$InstrumentationStdoutPath,
        [Parameter(Mandatory = $true)][string]$InstrumentationStderrPath,
        [Parameter(Mandatory = $true)][string]$MetadataPath,
        [string]$PostReadbackPath,
        [string]$ExpectedAccountFingerprint,
        [string]$CollectorPath,
        [string]$CollectorOutcomePath
    )
    $errors = [System.Collections.Generic.List[string]]::new()
    $captureSensitiveValues = @()
    if (-not [string]::IsNullOrWhiteSpace($ExpectedAccountFingerprint) -and
            $ExpectedAccountFingerprint -match '^[0-9a-fA-F]{64}$') {
        $captureSensitiveValues = @($ExpectedAccountFingerprint)
    }
    if ([string]::IsNullOrWhiteSpace($CollectorPath)) { $CollectorPath = Get-P5ECollectorPath }
    $collectorOutcomeRecord = $null
    $metadata = $null
    $observation = [pscustomobject]@{
        ClassMethodExact = $false; TestMethodExact = $false; OneTestMarker = $false
        TerminalMinusOne = $false; FailureMarker = $true; TerminalCodes = @()
    }
    if (-not (Test-Path -LiteralPath $InstrumentationStdoutPath -PathType Leaf)) {
        Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_STDOUT_MISSING'
    } else {
        $stdout = Get-Content -Raw -LiteralPath $InstrumentationStdoutPath
        $observation = Get-P5EInstrumentationObservation -Text $stdout
        $safe = Protect-P5ECaptureText -Text $stdout -SensitiveValues $captureSensitiveValues
        if ($safe.Violation) { Add-P5EError -Errors $errors -Code 'REDACTION_VIOLATION_STDOUT' }
    }
    if (-not (Test-Path -LiteralPath $InstrumentationStderrPath -PathType Leaf)) {
        Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_STDERR_MISSING'
    } else {
        $stderr = Get-Content -Raw -LiteralPath $InstrumentationStderrPath
        $safe = Protect-P5ECaptureText -Text $stderr -SensitiveValues $captureSensitiveValues
        if ($safe.Violation) { Add-P5EError -Errors $errors -Code 'REDACTION_VIOLATION_STDERR' }
    }
    if (-not (Test-Path -LiteralPath $MetadataPath -PathType Leaf)) {
        Add-P5EError -Errors $errors -Code 'HOST_METADATA_MISSING'
    } else {
        try { $metadata = Get-Content -Raw -LiteralPath $MetadataPath | ConvertFrom-Json }
        catch { Add-P5EError -Errors $errors -Code 'HOST_METADATA_INVALID' }
    }
    if ($null -ne $metadata) {
        Test-P5EObjectShape -Object $metadata -Path 'metadata' `
            -Required @('schemaVersion', 'eventId', 'runIdentity', 'metadataPath', 'classMethod', 'serial', 'runner', 'observationTimeoutMillis',
                'authorizationValidityMillis', 'maximumExecutionTimeMillis',
                'authorizationIssuedAtMillis', 'authorizationExpiresAtMillis', 'processExitCode',
                'launchCount', 'dispatchCount', 'timedOut', 'redactionPass', 'redactionViolation',
                'externalCallState', 'rawAcceptance', 'evidenceDirectory') `
            -Allowed @('schemaVersion', 'eventId', 'runIdentity', 'metadataPath', 'classMethod', 'serial', 'testPackage', 'targetPackage', 'runner',
                'observationTimeoutMillis', 'authorizationValidityMillis', 'maximumExecutionTimeMillis',
                'authorizationIssuedAtMillis', 'authorizationExpiresAtMillis', 'processExitCode',
                'launchCount', 'dispatchCount', 'timedOut', 'redactionPass', 'redactionViolation',
                'externalCallState', 'rawAcceptance', 'evidenceDirectory') -Errors $errors | Out-Null
        Test-P5EEqual -Object $metadata -Name 'schemaVersion' -Expected $script:P5EHostRunSchema -Path 'metadata' -Errors $errors
        Test-P5EEqual -Object $metadata -Name 'runIdentity' -Expected $script:P5ERunDeclarationIdentity -Path 'metadata' -Errors $errors
        Test-P5EEqual -Object $metadata -Name 'classMethod' -Expected $script:P5EClassMethod -Path 'metadata' -Errors $errors
        Test-P5EEqual -Object $metadata -Name 'serial' -Expected $script:P5ESerial -Path 'metadata' -Errors $errors
        Test-P5EEqual -Object $metadata -Name 'runner' -Expected $script:P5ERunner -Path 'metadata' -Errors $errors
        Test-P5ELong -Object $metadata -Name 'observationTimeoutMillis' -Expected $script:P5EHostObservationTimeoutMilliseconds -Path 'metadata' -Errors $errors | Out-Null
        Test-P5ELong -Object $metadata -Name 'authorizationValidityMillis' -Expected $script:P5EAuthorizationValidityMilliseconds -Path 'metadata' -Errors $errors | Out-Null
        Test-P5ELong -Object $metadata -Name 'maximumExecutionTimeMillis' -Expected $script:P5EExecutionDeadlineMilliseconds -Path 'metadata' -Errors $errors | Out-Null
        Test-P5ELong -Object $metadata -Name 'launchCount' -Expected 1L -Path 'metadata' -Errors $errors -Minimum 0 -Maximum 1 | Out-Null
        Test-P5ELong -Object $metadata -Name 'dispatchCount' -Expected 1L -Path 'metadata' -Errors $errors -Minimum 0 -Maximum 1 | Out-Null
        Test-P5EBoolean -Object $metadata -Name 'timedOut' -Expected $false -Path 'metadata' -Errors $errors
        Test-P5EBoolean -Object $metadata -Name 'redactionPass' -Expected $true -Path 'metadata' -Errors $errors
        Test-P5EBoolean -Object $metadata -Name 'redactionViolation' -Expected $false -Path 'metadata' -Errors $errors
        if ((Get-P5EProperty $metadata 'processExitCode') -ne 0) { Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_PROCESS_NONZERO' }
        $issued = Test-P5ELong -Object $metadata -Name 'authorizationIssuedAtMillis' -Expected ([long](Get-P5EProperty $metadata 'authorizationIssuedAtMillis')) -Path 'metadata' -Errors $errors -Minimum 0
        $expires = Test-P5ELong -Object $metadata -Name 'authorizationExpiresAtMillis' -Expected ([long](Get-P5EProperty $metadata 'authorizationExpiresAtMillis')) -Path 'metadata' -Errors $errors -Minimum 0
        if ($null -ne $issued -and $null -ne $expires -and $expires -ne $issued + $script:P5EAuthorizationValidityMilliseconds) {
            Add-P5EError -Errors $errors -Code 'AUTHORIZATION_WINDOW_INVALID'
        }
        if ((Test-P5EHasProperty -Object $metadata -Name 'evidenceDirectory') -and
                (Test-P5EHasProperty -Object $metadata -Name 'eventId')) {
            $metadataEvent = Split-Path -Leaf (Get-P5ECanonicalPath -Path ([string](Get-P5EProperty $metadata 'evidenceDirectory')))
            Test-P5EEqual -Object $metadata -Name 'eventId' -Expected $metadataEvent -Path 'metadata' -Errors $errors
        }
        if ((Test-P5EHasProperty -Object $metadata -Name 'metadataPath') -and
                -not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $metadata 'metadataPath')) -Right $MetadataPath)) {
            Add-P5EError -Errors $errors -Code 'METADATA_PATH_MISMATCH'
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($CollectorOutcomePath)) {
        $outcomeDirectory = if ($null -ne $metadata -and (Test-P5EHasProperty -Object $metadata -Name 'evidenceDirectory')) {
            [string](Get-P5EProperty $metadata 'evidenceDirectory')
        } else {
            Split-Path -Parent (Get-P5ECanonicalPath -Path $CollectorOutcomePath)
        }
        $collectorOutcomeRecord = Read-P5ECollectorOutcome -Path $CollectorOutcomePath `
            -ExpectedEvidenceDirectory $outcomeDirectory -Errors $errors
    }
    if (-not $observation.ClassMethodExact) { Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_CLASS_MISMATCH' }
    if (-not $observation.TestMethodExact) { Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_METHOD_MISMATCH' }
    if (-not $observation.OneTestMarker -or $observation.FailureMarker) { Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_TEST_COUNT_OR_FAILURE_INVALID' }
    if (-not $observation.TerminalMinusOne) { Add-P5EError -Errors $errors -Code 'INSTRUMENTATION_TERMINAL_INVALID' }
    if ([string]::IsNullOrWhiteSpace($ExpectedAccountFingerprint) -or
            $ExpectedAccountFingerprint -notmatch '^[0-9a-fA-F]{64}$') {
        Add-P5EError -Errors $errors -Code 'EXPECTED_ACCOUNT_FINGERPRINT_MISSING_OR_INVALID'
        $ExpectedAccountFingerprint = ''
    } else { $ExpectedAccountFingerprint = $ExpectedAccountFingerprint.ToLowerInvariant() }

    $readback = $null
    if ([string]::IsNullOrWhiteSpace($PostReadbackPath) -or
            -not (Test-Path -LiteralPath $PostReadbackPath -PathType Leaf)) {
        Add-P5EError -Errors $errors -Code 'POST_READBACK_MISSING'
    } else {
        try { $readback = Get-Content -Raw -LiteralPath $PostReadbackPath | ConvertFrom-Json }
        catch { Add-P5EError -Errors $errors -Code 'POST_READBACK_INVALID_JSON' }
    }
    $collectorTypedOutcome = if ($null -eq $collectorOutcomeRecord) { '' } else { [string](Get-P5EProperty $collectorOutcomeRecord.Outcome 'typedOutcome') }
    $strictReadbackRequired = [string]::IsNullOrWhiteSpace($collectorTypedOutcome) -or $collectorTypedOutcome -eq 'COMMITTED_READBACK_CAPTURED'
    if ($null -ne $readback -and $strictReadbackRequired) {
        Test-P5EReadback -Readback $readback -ExpectedAccountFingerprint $ExpectedAccountFingerprint `
            -Metadata $metadata -PostReadbackPath $PostReadbackPath -MetadataPath $MetadataPath `
            -CollectorPath $CollectorPath -Errors $errors
    }
    $decision = 'RAW_NOT_ACCEPTED'
    $metadataLaunchCount = if ($null -eq $metadata) { $null } else { Get-P5EProperty $metadata 'launchCount' }
    $metadataExternalState = if ($null -eq $metadata) { '' } else { [string](Get-P5EProperty $metadata 'externalCallState') }
    if ($collectorTypedOutcome -eq 'RECOVERY_REQUIRED') {
        $decision = 'RECOVERY_REQUIRED'
    } elseif ($collectorTypedOutcome -eq 'EXTERNAL_CALL_STATE_UNKNOWN') {
        $decision = 'EXTERNAL_CALL_STATE_UNKNOWN'
    } elseif ($collectorTypedOutcome -eq 'NO_CLAIM_OBSERVED' -and $null -ne $metadata -and [int]$metadataLaunchCount -eq 0) {
        $decision = 'DISPATCH_FAILED_BEFORE_LAUNCH'
    } elseif ($collectorTypedOutcome -eq 'NO_CLAIM_OBSERVED') {
        $decision = 'ACCEPTANCE_NOT_PROVEN'
    } elseif ($collectorTypedOutcome -eq 'COLLECTOR_TYPED_STOP') {
        $decision = 'ACCEPTANCE_NOT_PROVEN'
    } elseif ($null -ne $metadata -and [bool](Get-P5EProperty $metadata 'timedOut')) {
        $decision = 'EXTERNAL_CALL_STATE_UNKNOWN'
    } elseif ($null -ne $metadata -and [int]$metadataLaunchCount -gt 0 -and
            $metadataExternalState -eq 'UNKNOWN') {
        $decision = 'EXTERNAL_CALL_STATE_UNKNOWN'
    } elseif ($null -ne $metadata -and [int]$metadataLaunchCount -eq 0) {
        $decision = 'DISPATCH_FAILED_BEFORE_LAUNCH'
    } elseif ($null -eq $readback) {
        $decision = 'ACCEPTANCE_NOT_PROVEN'
    } elseif ($null -ne $readback -and [string](Get-P5EProperty (Get-P5EProperty $readback 'attempt') 'status') -eq 'RECOVERY_REQUIRED') {
        $decision = 'RECOVERY_REQUIRED'
    } elseif ($errors.Count -eq 0) {
        $decision = 'RAW_ACCEPTED'
    }
    return [pscustomobject]@{
        accepted = $decision -eq 'RAW_ACCEPTED' -and $errors.Count -eq 0
        decision = $decision
        p6Ready = $false
        errorCount = $errors.Count
        errors = $errors.ToArray()
        processExitCode = if ($null -eq $metadata) { $null } else { Get-P5EProperty $metadata 'processExitCode' }
        launchCount = if ($null -eq $metadata) { $null } else { Get-P5EProperty $metadata 'launchCount' }
        dispatchCount = if ($null -eq $metadata) { $null } else { Get-P5EProperty $metadata 'dispatchCount' }
        terminalCode = if ($observation.TerminalCodes.Count -eq 1) { $observation.TerminalCodes[0] } else { $null }
        testCountMarker = if ($observation.OneTestMarker) { 'OK (1 test)' } else { 'NOT_OK_1_TEST' }
        classMethodExact = $observation.ClassMethodExact
        methodExact = $observation.TestMethodExact
        redactionPass = $null -ne $metadata -and [bool](Get-P5EProperty $metadata 'redactionPass')
        collectorOutcome = $collectorTypedOutcome
    }
}

function Test-P5EProvenanceFile {
    param(
        [Parameter(Mandatory = $true)]$Provenance,
        [Parameter(Mandatory = $true)][string]$PathName,
        [Parameter(Mandatory = $true)][string]$HashName,
        [Parameter(Mandatory = $true)][string]$ExpectedFileName,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors,
        [string]$LengthName
    )
    $path = [string](Get-P5EProperty $Provenance $PathName)
    $hash = [string](Get-P5EProperty $Provenance $HashName)
    if ([string]::IsNullOrWhiteSpace($path)) {
        Add-P5EError -Errors $Errors -Code ('PROVENANCE_PATH_MISSING:' + $PathName)
        return $null
    }
    if (-not (Test-P5EPathUnderDirectory -Path $path -Directory $EvidenceDirectory) -or
            -not (Test-P5EPathEqual -Left $path -Right (Join-Path $EvidenceDirectory $ExpectedFileName))) {
        Add-P5EError -Errors $Errors -Code ('PROVENANCE_PATH_BOUNDARY_INVALID:' + $PathName)
        return $null
    }
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        Add-P5EError -Errors $Errors -Code ('PROVENANCE_FILE_MISSING:' + $PathName)
        return $null
    }
    $actualHash = Get-P5ESha256 -Path $path
    if ($hash -notmatch '^[0-9a-fA-F]{64}$' -or $hash.ToLowerInvariant() -ne $actualHash) {
        Add-P5EError -Errors $Errors -Code ('PROVENANCE_SOURCE_HASH_INVALID:' + $PathName)
    }
    if (-not [string]::IsNullOrWhiteSpace($LengthName)) {
        $providedLength = 0L
        if (-not [long]::TryParse([string](Get-P5EProperty $Provenance $LengthName),
                [Globalization.NumberStyles]::Integer, [Globalization.CultureInfo]::InvariantCulture,
                [ref]$providedLength) -or $providedLength -ne ([IO.File]::ReadAllBytes($path)).Length) {
            Add-P5EError -Errors $Errors -Code ('PROVENANCE_BYTE_LENGTH_INVALID:' + $PathName)
        }
    }
    return $path
}

function Test-P5ESerializedArtifactBytes {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedSchema,
        [Parameter(Mandatory = $true)][string]$Kind,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors,
        $ExpectedIdentity = $null
    )
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Add-P5EError -Errors $Errors -Code ('ARTIFACT_BYTES_MISSING:' + $Kind)
        return $null
    }
    $bytes = [IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xef -and $bytes[1] -eq 0xbb -and $bytes[2] -eq 0xbf) {
        Add-P5EError -Errors $Errors -Code ('ARTIFACT_BOM_FORBIDDEN:' + $Kind)
        return $null
    }
    try {
        $text = [Text.UTF8Encoding]::new($false, $true).GetString($bytes)
        $value = $text | ConvertFrom-Json
    } catch {
        Add-P5EError -Errors $Errors -Code ('ARTIFACT_SERIALIZED_JSON_INVALID:' + $Kind)
        return $null
    }
    try { $contract = Get-P5EArtifactContract }
    catch {
        Add-P5EError -Errors $Errors -Code ('ARTIFACT_CONTRACT_UNAVAILABLE:' + $Kind)
        return $null
    }
    $spec = Get-P5EProperty -Object $contract -Name $Kind
    $required = @((Get-P5EProperty -Object $spec -Name 'requiredFields'))
    if ($required.Count -eq 0) {
        Add-P5EError -Errors $Errors -Code ('ARTIFACT_CONTRACT_FIELDS_MISSING:' + $Kind)
        return $null
    }
    $pathName = 'storedArtifact.' + $Kind
    Test-P5EObjectShape -Object $value -Path $pathName -Required $required -Allowed $required -Errors $Errors | Out-Null
    Test-P5EEqual $value 'schemaVersion' $ExpectedSchema $pathName $Errors
    Test-P5EEqual $value 'schemaVersion' ([string](Get-P5EProperty $spec 'schemaVersion')) $pathName $Errors
    Test-P5EEqual $value 'artifactType' 'REPORT_L1' $pathName $Errors
    $identity = if ($null -eq $ExpectedIdentity) {
        [ordered]@{
            bindingIdentity = $script:P5EBindingIdentity
            manifestFingerprint = $script:P5EExpectedManifestFingerprint
            canonicalPackHash = $script:P5EPackHash
            canonicalProfileHash = $script:P5EProfileHash
            compatibilityEvaluationId = $script:P5EEvaluationId
            chapterKey = $script:P5EChapterKey
            phase = $script:P5EPhase
            bundleIdentity = $null
            predecessorIdentity = $script:P5ERunDeclarationIdentity
        }
    } else { $ExpectedIdentity }
    foreach ($identityName in @('bindingIdentity', 'manifestFingerprint', 'canonicalPackHash',
            'canonicalProfileHash', 'compatibilityEvaluationId', 'chapterKey', 'phase',
            'bundleIdentity', 'predecessorIdentity')) {
        if ($null -ne $ExpectedIdentity -and -not (Test-P5EHasProperty -Object $identity -Name $identityName)) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_EXPECTED_IDENTITY_FIELD_MISSING:' + $identityName)
        }
    }
    $expectedBinding = [string](Get-P5EProperty $identity 'bindingIdentity')
    $expectedManifest = [string](Get-P5EProperty $identity 'manifestFingerprint')
    $expectedPack = [string](Get-P5EProperty $identity 'canonicalPackHash')
    $expectedProfile = [string](Get-P5EProperty $identity 'canonicalProfileHash')
    $expectedEvaluation = [string](Get-P5EProperty $identity 'compatibilityEvaluationId')
    $expectedChapter = [string](Get-P5EProperty $identity 'chapterKey')
    $expectedPhase = [string](Get-P5EProperty $identity 'phase')
    $expectedBundle = [string](Get-P5EProperty $identity 'bundleIdentity')
    $expectedPredecessor = [string](Get-P5EProperty $identity 'predecessorIdentity')
    if ($Kind -eq 'report') {
        Test-P5ESha256 $value 'bindingIdentity' $pathName $Errors $expectedBinding
        Test-P5ESha256 $value 'manifestFingerprint' $pathName $Errors $expectedManifest
        Test-P5ESha256 $value 'canonicalPackHash' $pathName $Errors $expectedPack
        Test-P5ESha256 $value 'canonicalProfileHash' $pathName $Errors $expectedProfile
        Test-P5EEqual $value 'compatibilityEvaluationId' $expectedEvaluation $pathName $Errors
        Test-P5EEqual $value 'chapterKey' $expectedChapter $pathName $Errors
    } else {
        Test-P5ESha256 $value 'bindingRef' $pathName $Errors $expectedBinding
        Test-P5ESha256 $value 'manifestRef' $pathName $Errors $expectedManifest
        Test-P5ESha256 $value 'packRef' $pathName $Errors $expectedPack
        Test-P5ESha256 $value 'profileRef' $pathName $Errors $expectedProfile
        if (-not (Test-P5EHasProperty -Object $value -Name 'canonAllowed') -or
                -not ((Get-P5EProperty $value 'canonAllowed') -is [bool])) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_BOOLEAN_INVALID:' + $Kind + ':canonAllowed')
        }
        if (-not (Test-P5EHasProperty -Object $value -Name 'propagationAllowed') -or
                -not ((Get-P5EProperty $value 'propagationAllowed') -is [bool])) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_BOOLEAN_INVALID:' + $Kind + ':propagationAllowed')
        }
    }
    Test-P5EEqual $value 'phase' $expectedPhase $pathName $Errors
    foreach ($name in @('bundleIdentity', 'predecessorIdentity', 'disposition')) {
        if ([string]::IsNullOrWhiteSpace([string](Get-P5EProperty $value $name))) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_VALUE_MISSING:' + $Kind + ':' + $name)
        }
    }
    if ($null -ne $ExpectedIdentity) {
        Test-P5EEqual $value 'bundleIdentity' $expectedBundle $pathName $Errors
        Test-P5EEqual $value 'predecessorIdentity' $expectedPredecessor $pathName $Errors
    }
    $numberFields = if ($Kind -eq 'report') { @('populationTotal', 'accountedTotal', 'actualChangedSpans') } else { @('populationTotal', 'accountedTotal', 'changedSpanTotal', 'releaseAttemptCount') }
    foreach ($name in $numberFields) {
        $number = 0D
        if (-not [decimal]::TryParse([string](Get-P5EProperty $value $name), [Globalization.NumberStyles]::Number,
                [Globalization.CultureInfo]::InvariantCulture, [ref]$number) -or $number -lt 0D) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_NUMBER_INVALID:' + $Kind + ':' + $name)
        }
    }
    if ($Kind -eq 'report' -and (-not (Test-P5EHasProperty -Object $value -Name 'modelDeclaredPassRecordedOnly') -or
            -not ((Get-P5EProperty $value 'modelDeclaredPassRecordedOnly') -is [bool]))) {
        Add-P5EError -Errors $Errors -Code 'ARTIFACT_BOOLEAN_INVALID:report:modelDeclaredPassRecordedOnly'
    }
    try {
        $canonicalText = ConvertTo-P5ECanonicalJson -Value $value
        $canonicalBytes = [Text.UTF8Encoding]::new($false).GetBytes($canonicalText)
        if ([Convert]::ToBase64String($canonicalBytes) -cne [Convert]::ToBase64String($bytes)) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_NONCANONICAL_BYTES:' + $Kind)
        }
    } catch { Add-P5EError -Errors $Errors -Code ('ARTIFACT_CANONICALIZATION_FAILED:' + $Kind) }
    return [pscustomobject]@{ Length = [long]$bytes.Length; Sha256 = Get-P5ESha256 -Path $Path; Parsed = $value }
}

function Test-P5EArtifactPair {
    param(
        [Parameter(Mandatory = $true)]$Report,
        [Parameter(Mandatory = $true)]$Receipt,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    if ($null -eq $Report -or $null -eq $Receipt) { return }
    foreach ($mapping in @(
            [pscustomobject]@{ Left = 'manifestFingerprint'; Right = 'manifestRef' },
            [pscustomobject]@{ Left = 'canonicalPackHash'; Right = 'packRef' },
            [pscustomobject]@{ Left = 'canonicalProfileHash'; Right = 'profileRef' },
            [pscustomobject]@{ Left = 'bindingIdentity'; Right = 'bindingRef' },
            [pscustomobject]@{ Left = 'actualChangedSpans'; Right = 'changedSpanTotal' })) {
        if ([string](Get-P5EProperty $Report $mapping.Left) -cne [string](Get-P5EProperty $Receipt $mapping.Right)) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_PAIR_REFERENCE_MISMATCH:' + $mapping.Left)
        }
    }
    foreach ($name in @('phase', 'bundleIdentity', 'predecessorIdentity', 'populationTotal',
            'accountedTotal', 'gates', 'evidenceRefs', 'disposition', 'preservedInventory')) {
        $left = ConvertTo-P5ECanonicalJson -Value (Get-P5EProperty $Report $name)
        $rightName = if ($name -eq 'actualChangedSpans') { 'changedSpanTotal' } else { $name }
        $right = ConvertTo-P5ECanonicalJson -Value (Get-P5EProperty $Receipt $rightName)
        if ($left -cne $right) { Add-P5EError -Errors $Errors -Code ('ARTIFACT_PAIR_FIELD_MISMATCH:' + $name) }
    }
}

function Test-P5EReadbackProvenance {
    param(
        [Parameter(Mandatory = $true)]$Readback,
        [Parameter(Mandatory = $true)]$Metadata,
        [Parameter(Mandatory = $true)][string]$PostReadbackPath,
        [Parameter(Mandatory = $true)][string]$MetadataPath,
        [Parameter(Mandatory = $true)][string]$CollectorPath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    $provenance = Get-P5EProperty $Readback 'provenance'
    $required = @(
        'schemaVersion', 'sourceMappingVersion', 'collectorImplementationId',
        'collectorImplementationVersion', 'collectorImplementationPath',
        'collectorImplementationSha256', 'eventId', 'evidenceDirectory', 'metadataPath',
        'postReadbackPath', 'runIdentity', 'sourceInputPath', 'sourceInputSha256',
        'beforeSnapshotPath', 'beforeSnapshotSha256', 'afterSnapshotPath',
        'afterSnapshotSha256', 'transactionEvidencePath', 'transactionEvidenceSha256',
        'reportBytesPath', 'reportBytesSha256', 'reportBytesLength',
        'receiptBytesPath', 'receiptBytesSha256', 'receiptBytesLength',
        'runStartedAtMillis', 'beforeSnapshotObservedAtMillis', 'claimObservedAtMillis',
        'consumedAtMillis', 'attemptCreatedAtMillis', 'attemptUpdatedAtMillis',
        'observedAtMillis', 'collectedAtMillis', 'atomicityEvidence')
    Test-P5EObjectShape -Object $provenance -Path 'readback.provenance' -Required $required -Allowed $required -Errors $Errors | Out-Null
    if ($null -eq $provenance -or $null -eq $Metadata) { return }

    Test-P5EEqual $provenance 'schemaVersion' $script:P5EReadbackProvenanceSchema 'readback.provenance' $Errors
    Test-P5EEqual $provenance 'sourceMappingVersion' $script:P5ESourceMappingVersion 'readback.provenance' $Errors
    Test-P5EEqual $provenance 'collectorImplementationId' $script:P5ECollectorImplementationId 'readback.provenance' $Errors
    Test-P5EEqual $provenance 'collectorImplementationVersion' $script:P5ECollectorImplementationVersion 'readback.provenance' $Errors
    if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $provenance 'collectorImplementationPath')) -Right $CollectorPath) -or
            -not (Test-P5EPathEqual -Left $CollectorPath -Right (Get-P5ECollectorPath))) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_IMPLEMENTATION_PATH_MISMATCH'
    }
    if (-not (Test-Path -LiteralPath $CollectorPath -PathType Leaf) -or
            [string](Get-P5EProperty $provenance 'collectorImplementationSha256') -ine (Get-P5ESha256 -Path $CollectorPath)) {
        Add-P5EError -Errors $Errors -Code 'COLLECTOR_IMPLEMENTATION_HASH_MISMATCH'
    }

    $evidenceDirectory = [string](Get-P5EProperty $Metadata 'evidenceDirectory')
    $eventId = [string](Get-P5EProperty $Metadata 'eventId')
    if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $provenance 'evidenceDirectory')) -Right $evidenceDirectory) -or
            -not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $provenance 'postReadbackPath')) -Right $PostReadbackPath) -or
            -not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $provenance 'metadataPath')) -Right $MetadataPath) -or
            [string](Get-P5EProperty $provenance 'eventId') -cne $eventId -or
            (Split-Path -Leaf (Get-P5ECanonicalPath -Path $evidenceDirectory)) -cne $eventId) {
        Add-P5EError -Errors $Errors -Code 'READBACK_EVENT_OR_FILE_BINDING_INVALID'
    }
    if (-not (Test-P5EPathEqual -Left $PostReadbackPath -Right (Join-Path $evidenceDirectory 'post-readback.json'))) {
        Add-P5EError -Errors $Errors -Code 'POST_READBACK_FILE_BINDING_INVALID'
    }

    $inputPath = Test-P5EProvenanceFile -Provenance $provenance -PathName 'sourceInputPath' -HashName 'sourceInputSha256' `
        -ExpectedFileName 'collector-input.json' -EvidenceDirectory $evidenceDirectory -Errors $Errors
    $beforePath = Test-P5EProvenanceFile -Provenance $provenance -PathName 'beforeSnapshotPath' -HashName 'beforeSnapshotSha256' `
        -ExpectedFileName 'before-snapshot.json' -EvidenceDirectory $evidenceDirectory -Errors $Errors
    $afterPath = Test-P5EProvenanceFile -Provenance $provenance -PathName 'afterSnapshotPath' -HashName 'afterSnapshotSha256' `
        -ExpectedFileName 'after-snapshot.json' -EvidenceDirectory $evidenceDirectory -Errors $Errors
    $transactionPath = Test-P5EProvenanceFile -Provenance $provenance -PathName 'transactionEvidencePath' -HashName 'transactionEvidenceSha256' `
        -ExpectedFileName 'transaction-evidence.json' -EvidenceDirectory $evidenceDirectory -Errors $Errors
    $reportPath = Test-P5EProvenanceFile -Provenance $provenance -PathName 'reportBytesPath' -HashName 'reportBytesSha256' `
        -ExpectedFileName 'report.bin' -EvidenceDirectory $evidenceDirectory -Errors $Errors -LengthName 'reportBytesLength'
    $receiptPath = Test-P5EProvenanceFile -Provenance $provenance -PathName 'receiptBytesPath' -HashName 'receiptBytesSha256' `
        -ExpectedFileName 'receipt.bin' -EvidenceDirectory $evidenceDirectory -Errors $Errors -LengthName 'receiptBytesLength'

    $reportChecked = $null
    $receiptChecked = $null
    if ($null -ne $reportPath) {
        $reportChecked = Test-P5ESerializedArtifactBytes -Path $reportPath -ExpectedSchema 'safe4.full.report-l1.v1' -Kind 'report' -Errors $Errors
        if ($null -ne $reportChecked -and [string](Get-P5EProperty (Get-P5EProperty $Readback 'artifacts') 'report').sha256 -ine $reportChecked.Sha256) {
            Add-P5EError -Errors $Errors -Code 'REPORT_STORED_BYTES_HASH_MISMATCH'
        }
    }
    if ($null -ne $receiptPath) {
        $receiptChecked = Test-P5ESerializedArtifactBytes -Path $receiptPath -ExpectedSchema 'safe4.full.receipt.v1' -Kind 'receipt' -Errors $Errors
        if ($null -ne $receiptChecked -and [string](Get-P5EProperty (Get-P5EProperty $Readback 'artifacts') 'receipt').sha256 -ine $receiptChecked.Sha256) {
            Add-P5EError -Errors $Errors -Code 'RECEIPT_STORED_BYTES_HASH_MISMATCH'
        }
    }
    if ($null -ne $reportChecked -and $null -ne $receiptChecked) {
        Test-P5EArtifactPair -Report $reportChecked.Parsed -Receipt $receiptChecked.Parsed -Errors $Errors
    }

    $sourceInput = $null; $before = $null; $after = $null; $transaction = $null
    try {
        if ($null -ne $inputPath) { $sourceInput = Get-Content -Raw -LiteralPath $inputPath | ConvertFrom-Json }
        if ($null -ne $beforePath) { $before = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json }
        if ($null -ne $afterPath) { $after = Get-Content -Raw -LiteralPath $afterPath | ConvertFrom-Json }
        if ($null -ne $transactionPath) { $transaction = Get-Content -Raw -LiteralPath $transactionPath | ConvertFrom-Json }
    } catch {
        Add-P5EError -Errors $Errors -Code 'PRODUCER_SOURCE_JSON_INVALID'
    }
    if ($null -ne $sourceInput) {
        Test-P5EObjectShape -Object $sourceInput -Path 'producerInput' -Required @('schemaVersion', 'eventId', 'runIdentity', 'sourceMappingVersion') `
            -Allowed @('schemaVersion', 'eventId', 'runIdentity', 'sourceMappingVersion', 'production', 'test', 'freshTuple', 'attemptTemplate', 'authorizationReceipt', 'lifecycle', 'reportArtifactTemplate', 'receiptArtifactTemplate', 'eventPlanPath', 'eventPlanSha256', 'collectorImplementationId', 'collectorImplementationVersion', 'collectionMode', 'serial', 'settingsSourcePath', 'settingsSourceSha256', 'transactionSourcePath', 'transactionSourceSha256', 'transactionTestPath', 'transactionTestSha256') -Errors $Errors | Out-Null
        Test-P5EEqual $sourceInput 'schemaVersion' $script:P5EProducerInputSchema 'producerInput' $Errors
        Test-P5EEqual $sourceInput 'eventId' $eventId 'producerInput' $Errors
        Test-P5EEqual $sourceInput 'runIdentity' $script:P5ERunDeclarationIdentity 'producerInput' $Errors
        Test-P5EEqual $sourceInput 'sourceMappingVersion' $script:P5ESourceMappingVersion 'producerInput' $Errors
        if (Test-P5EHasProperty -Object $sourceInput -Name 'eventPlanPath') {
            if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $sourceInput 'eventPlanPath')) -Right (Join-Path $evidenceDirectory 'EVENT_PLAN.json'))) {
                Add-P5EError -Errors $Errors -Code 'PRODUCER_EVENT_PLAN_PATH_INVALID'
            }
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'eventPlanSha256') {
            Test-P5ESha256 $sourceInput 'eventPlanSha256' 'producerInput' $Errors
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'collectorImplementationId') {
            Test-P5EEqual $sourceInput 'collectorImplementationId' $script:P5ECollectorImplementationId 'producerInput' $Errors
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'collectorImplementationVersion') {
            Test-P5EEqual $sourceInput 'collectorImplementationVersion' $script:P5ECollectorImplementationVersion 'producerInput' $Errors
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'collectionMode') {
            Test-P5EEqual $sourceInput 'collectionMode' $script:P5ECollectorReadbackMode 'producerInput' $Errors
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'serial') {
            Test-P5EEqual $sourceInput 'serial' $script:P5ESerial 'producerInput' $Errors
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'settingsSourcePath') {
            if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $sourceInput 'settingsSourcePath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ESettingsSourcePath))) {
                Add-P5EError -Errors $Errors -Code 'PRODUCER_SETTINGS_SOURCE_PATH_INVALID'
            }
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'settingsSourceSha256') {
            Test-P5ESha256 $sourceInput 'settingsSourceSha256' 'producerInput' $Errors $script:P5ESettingsSourceSha256
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'transactionSourcePath') {
            if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $sourceInput 'transactionSourcePath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionSourcePath))) {
                Add-P5EError -Errors $Errors -Code 'PRODUCER_TRANSACTION_SOURCE_PATH_INVALID'
            }
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'transactionSourceSha256') {
            Test-P5ESha256 $sourceInput 'transactionSourceSha256' 'producerInput' $Errors $script:P5ETransactionSourceSha256
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'transactionTestPath') {
            if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $sourceInput 'transactionTestPath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionTestPath))) {
                Add-P5EError -Errors $Errors -Code 'PRODUCER_TRANSACTION_TEST_PATH_INVALID'
            }
        }
        if (Test-P5EHasProperty -Object $sourceInput -Name 'transactionTestSha256') {
            Test-P5ESha256 $sourceInput 'transactionTestSha256' 'producerInput' $Errors $script:P5ETransactionTestSha256
        }
    }
    foreach ($source in @($before, $after)) {
        if ($null -ne $source) {
            Test-P5EObjectShape -Object $source -Path 'producerSnapshot' -Required @('schemaVersion', 'eventId', 'runIdentity', 'snapshotMode', 'observedAtMillis', 'databaseSha256', 'databaseSchemaVersion', 'integrityCheck', 'foreignKeyViolations', 'lineage', 'globalCounts', 'immutableTupleIdentity', 'unrelatedWrites', 'deletedRows') `
                -Allowed @('schemaVersion', 'eventId', 'runIdentity', 'snapshotMode', 'observedAtMillis', 'databaseSha256', 'databaseWalSha256', 'databaseShmSha256', 'walPresent', 'shmPresent', 'settingsSha256', 'databaseSchemaVersion', 'integrityCheck', 'foreignKeyViolations', 'lineage', 'globalCounts', 'immutableTupleIdentity', 'unrelatedWrites', 'deletedRows') -Errors $Errors | Out-Null
            Test-P5EEqual $source 'eventId' $eventId 'producerSnapshot' $Errors
            Test-P5EEqual $source 'runIdentity' $script:P5ERunDeclarationIdentity 'producerSnapshot' $Errors
            if ([string](Get-P5EProperty $source 'snapshotMode') -notin @('WAL_AWARE_CONSISTENT', 'WAL_AWARE_READ_TRANSACTION')) {
                Add-P5EError -Errors $Errors -Code 'PRODUCER_SNAPSHOT_MODE_INVALID'
            }
            Test-P5ELongRange -Object $source -Name 'observedAtMillis' -Path 'producerSnapshot' -Errors $Errors -Minimum 0 | Out-Null
            Test-P5ESha256 $source 'databaseSha256' 'producerSnapshot' $Errors
            Test-P5ELong $source 'databaseSchemaVersion' $script:P5EDatabaseSchemaVersion 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            Test-P5EEqual $source 'integrityCheck' 'ok' 'producerSnapshot' $Errors
            Test-P5ELong $source 'foreignKeyViolations' 0L 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            Test-P5ESha256 $source 'immutableTupleIdentity' 'producerSnapshot' $Errors $script:P5EBindingIdentity
            Test-P5ELong $source 'unrelatedWrites' 0L 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            Test-P5ELong $source 'deletedRows' 0L 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            $globalCounts = Get-P5EProperty $source 'globalCounts'
            Test-P5EObjectShape -Object $globalCounts -Path 'producerSnapshot.globalCounts' `
                -Required @('attempts', 'authorizationReceipts', 'lifecycle', 'reconciliation', 'reconciliationHistory') `
                -Allowed @('attempts', 'authorizationReceipts', 'lifecycle', 'reconciliation', 'reconciliationHistory') -Errors $Errors | Out-Null
            foreach ($name in @('attempts', 'authorizationReceipts', 'lifecycle', 'reconciliation', 'reconciliationHistory')) {
                Test-P5ELongRange -Object $globalCounts -Name $name -Path 'producerSnapshot.globalCounts' -Errors $Errors -Minimum 0 | Out-Null
            }
            foreach ($name in @('databaseWalSha256', 'databaseShmSha256')) {
                if (Test-P5EHasProperty -Object $source -Name $name) {
                    $hash = [string](Get-P5EProperty $source $name)
                    if (-not [string]::IsNullOrWhiteSpace($hash) -and $hash -notmatch '^[0-9a-fA-F]{64}$') {
                        Add-P5EError -Errors $Errors -Code ('PRODUCER_SNAPSHOT_HASH_INVALID:' + $name)
                    }
                }
            }
            foreach ($name in @('walPresent', 'shmPresent')) {
                if ((Test-P5EHasProperty -Object $source -Name $name) -and -not ((Get-P5EProperty $source $name) -is [bool])) {
                    Add-P5EError -Errors $Errors -Code ('PRODUCER_SNAPSHOT_BOOLEAN_INVALID:' + $name)
                }
            }
            if (Test-P5EHasProperty -Object $source -Name 'settingsSha256') {
                Test-P5ESha256 $source 'settingsSha256' 'producerSnapshot' $Errors
            }
        }
    }
    if ($null -ne $transaction) {
        $transactionFields = @('schemaVersion', 'eventId', 'runIdentity', 'transactionTestId',
            'transactionSemanticsPassed', 'beforeAfterConsistent', 'rowPairConsistent',
            'claimAndAuthorizationAtomic', 'reportReceiptAtomic', 'noUnrelatedWrites', 'noDeletes',
            'sourceTransactionCodePath', 'sourceTransactionCodeSha256',
            'sourceTransactionTestPath', 'sourceTransactionTestSha256', 'sourceTransactionTestStatus')
        Test-P5EObjectShape -Object $transaction -Path 'transactionEvidence' -Required $transactionFields -Allowed $transactionFields -Errors $Errors | Out-Null
        Test-P5EEqual $transaction 'schemaVersion' 'p5e.raw.transaction-evidence.v1' 'transactionEvidence' $Errors
        Test-P5EEqual $transaction 'eventId' $eventId 'transactionEvidence' $Errors
        Test-P5EEqual $transaction 'runIdentity' $script:P5ERunDeclarationIdentity 'transactionEvidence' $Errors
        if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $transaction 'sourceTransactionCodePath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionSourcePath))) {
            Add-P5EError -Errors $Errors -Code 'TRANSACTION_SOURCE_CODE_PATH_INVALID'
        }
        Test-P5ESha256 $transaction 'sourceTransactionCodeSha256' 'transactionEvidence' $Errors $script:P5ETransactionSourceSha256
        if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $transaction 'sourceTransactionTestPath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionTestPath))) {
            Add-P5EError -Errors $Errors -Code 'TRANSACTION_SOURCE_TEST_PATH_INVALID'
        }
        Test-P5ESha256 $transaction 'sourceTransactionTestSha256' 'transactionEvidence' $Errors $script:P5ETransactionTestSha256
        Test-P5EEqual $transaction 'sourceTransactionTestStatus' 'PINNED_SOURCE_TRANSACTION_CONTRACT' 'transactionEvidence' $Errors
        foreach ($name in @('transactionSemanticsPassed', 'beforeAfterConsistent', 'rowPairConsistent', 'claimAndAuthorizationAtomic', 'reportReceiptAtomic', 'noUnrelatedWrites', 'noDeletes')) {
            Test-P5EBoolean $transaction $name $true 'transactionEvidence' $Errors
        }
    }
    if ($null -ne $before -and $null -ne $after) {
        $database = Get-P5EProperty $Readback 'database'
        $beforeLineage = Get-P5EProperty $Readback 'lineageBefore'
        $afterLineage = Get-P5EProperty $Readback 'lineageAfter'
        if ([string](Get-P5EProperty $before 'databaseSha256') -ine [string](Get-P5EProperty $database 'beforeSha256') -or
                [string](Get-P5EProperty $after 'databaseSha256') -ine [string](Get-P5EProperty $database 'afterSha256') -or
                [long](Get-P5EProperty $before 'databaseSchemaVersion') -ne [long](Get-P5EProperty $database 'schemaVersion') -or
                [long](Get-P5EProperty $after 'databaseSchemaVersion') -ne [long](Get-P5EProperty $database 'schemaVersion') -or
                [string](Get-P5EProperty $before 'immutableTupleIdentity') -ine $script:P5EBindingIdentity -or
                [string](Get-P5EProperty $after 'immutableTupleIdentity') -ine $script:P5EBindingIdentity) {
            Add-P5EError -Errors $Errors -Code 'PRODUCER_SNAPSHOT_READBACK_MISMATCH'
        }
        foreach ($name in @('attempts', 'authorizationReceipts', 'reconciliation', 'reconciliationHistory', 'lifecycle', 'reportOrReceipt')) {
            if ([long](Get-P5EProperty (Get-P5EProperty $before 'lineage') $name) -ne [long](Get-P5EProperty $beforeLineage $name) -or
                    [long](Get-P5EProperty (Get-P5EProperty $after 'lineage') $name) -ne [long](Get-P5EProperty $afterLineage $name)) {
                Add-P5EError -Errors $Errors -Code ('PRODUCER_LINEAGE_READBACK_MISMATCH:' + $name)
            }
        }
        foreach ($snapshot in @($before, $after)) {
            $globalCounts = Get-P5EProperty $snapshot 'globalCounts'
            foreach ($name in @('attempts', 'authorizationReceipts', 'lifecycle', 'reconciliation', 'reconciliationHistory')) {
                if ($null -eq $globalCounts -or $null -eq (Get-P5EProperty $globalCounts $name)) {
                    Add-P5EError -Errors $Errors -Code ('PRODUCER_GLOBAL_COUNT_MISSING:' + $name)
                }
            }
        }
    }

    $atomicity = Get-P5EProperty $provenance 'atomicityEvidence'
    $atomicityFields = @('transactionEvidencePath', 'transactionEvidenceSha256', 'transactionSemanticsPassed',
        'beforeAfterConsistent', 'rowPairConsistent', 'claimAndAuthorizationAtomic', 'reportReceiptAtomic',
        'reportBytesValidated', 'receiptBytesValidated', 'validatorId', 'sourceTransactionCodePath',
        'sourceTransactionCodeSha256', 'sourceTransactionTestPath',
        'sourceTransactionTestSha256', 'sourceTransactionTestStatus')
    Test-P5EObjectShape -Object $atomicity -Path 'readback.provenance.atomicityEvidence' -Required $atomicityFields -Allowed $atomicityFields -Errors $Errors | Out-Null
    if ($null -ne $atomicity) {
        Test-P5EEqual $atomicity 'transactionEvidencePath' ([string](Get-P5EProperty $provenance 'transactionEvidencePath')) 'atomicityEvidence' $Errors
        Test-P5EEqual $atomicity 'transactionEvidenceSha256' ([string](Get-P5EProperty $provenance 'transactionEvidenceSha256')) 'atomicityEvidence' $Errors
        Test-P5EEqual $atomicity 'validatorId' $script:P5ESyntheticArtifactValidatorId 'atomicityEvidence' $Errors
        Test-P5EEqual $atomicity 'sourceTransactionCodePath' ([string](Get-P5EProperty $transaction 'sourceTransactionCodePath')) 'atomicityEvidence' $Errors
        Test-P5ESha256 $atomicity 'sourceTransactionCodeSha256' 'atomicityEvidence' $Errors $script:P5ETransactionSourceSha256
        Test-P5EEqual $atomicity 'sourceTransactionTestPath' ([string](Get-P5EProperty $transaction 'sourceTransactionTestPath')) 'atomicityEvidence' $Errors
        Test-P5ESha256 $atomicity 'sourceTransactionTestSha256' 'atomicityEvidence' $Errors $script:P5ETransactionTestSha256
        Test-P5EEqual $atomicity 'sourceTransactionTestStatus' 'PINNED_SOURCE_TRANSACTION_CONTRACT' 'atomicityEvidence' $Errors
        foreach ($name in @('transactionSemanticsPassed', 'beforeAfterConsistent', 'rowPairConsistent', 'claimAndAuthorizationAtomic', 'reportReceiptAtomic', 'reportBytesValidated', 'receiptBytesValidated')) {
            Test-P5EBoolean $atomicity $name $true 'atomicityEvidence' $Errors
        }
    }

    $attempt = Get-P5EProperty $Readback 'attempt'
    $auth = Get-P5EProperty $Readback 'authorizationReceipt'
    $timingNames = @('runStartedAtMillis', 'beforeSnapshotObservedAtMillis', 'claimObservedAtMillis',
        'consumedAtMillis', 'attemptCreatedAtMillis', 'attemptUpdatedAtMillis', 'observedAtMillis', 'collectedAtMillis')
    $timing = @{}
    foreach ($name in $timingNames) { $timing[$name] = Test-P5ELongRange -Object $provenance -Name $name -Path 'readback.provenance' -Errors $Errors -Minimum 0 }
    if ($null -ne $timing.runStartedAtMillis -and $null -ne $Metadata -and
            $timing.runStartedAtMillis -ne [long](Get-P5EProperty $Metadata 'authorizationIssuedAtMillis')) {
        Add-P5EError -Errors $Errors -Code 'PROVENANCE_RUN_START_MISMATCH'
    }
    if ($null -ne $timing.observedAtMillis -and $timing.observedAtMillis -ne [long](Get-P5EProperty $Readback 'observedAtMillis')) { Add-P5EError $Errors 'PROVENANCE_OBSERVED_TIME_MISMATCH' }
    if ($null -ne $timing.beforeSnapshotObservedAtMillis -and $null -ne $before -and $timing.beforeSnapshotObservedAtMillis -ne [long](Get-P5EProperty $before 'observedAtMillis')) { Add-P5EError $Errors 'PROVENANCE_BEFORE_TIME_MISMATCH' }
    if ($null -ne $timing.claimObservedAtMillis -and $null -ne $attempt -and $timing.claimObservedAtMillis -ne [long](Get-P5EProperty $attempt 'createdAtMillis')) { Add-P5EError $Errors 'PROVENANCE_CLAIM_TIME_MISMATCH' }
    if ($null -ne $timing.consumedAtMillis -and $null -ne $auth -and $timing.consumedAtMillis -ne [long](Get-P5EProperty $auth 'consumedAtMillis')) { Add-P5EError $Errors 'PROVENANCE_CONSUMED_TIME_MISMATCH' }
    if ($null -ne $timing.attemptCreatedAtMillis -and $null -ne $attempt -and $timing.attemptCreatedAtMillis -ne [long](Get-P5EProperty $attempt 'createdAtMillis')) { Add-P5EError $Errors 'PROVENANCE_ATTEMPT_CREATED_TIME_MISMATCH' }
    if ($null -ne $timing.attemptUpdatedAtMillis -and $null -ne $attempt -and $timing.attemptUpdatedAtMillis -ne [long](Get-P5EProperty $attempt 'updatedAtMillis')) { Add-P5EError $Errors 'PROVENANCE_ATTEMPT_UPDATED_TIME_MISMATCH' }
    if ($null -ne $timing.observedAtMillis -and $null -ne $timing.collectedAtMillis -and $timing.collectedAtMillis -lt $timing.observedAtMillis) { Add-P5EError $Errors 'PROVENANCE_COLLECTION_BEFORE_OBSERVATION' }
    if ($null -ne $timing.beforeSnapshotObservedAtMillis -and $null -ne $timing.claimObservedAtMillis -and $timing.beforeSnapshotObservedAtMillis -gt $timing.claimObservedAtMillis) { Add-P5EError $Errors 'PROVENANCE_BEFORE_AFTER_ORDER_INVALID' }
    if ($null -ne $timing.claimObservedAtMillis -and $null -ne $timing.consumedAtMillis -and $timing.claimObservedAtMillis -gt $timing.consumedAtMillis) { Add-P5EError $Errors 'PROVENANCE_CLAIM_CONSUME_ORDER_INVALID' }
    if ($null -ne $timing.attemptCreatedAtMillis -and $null -ne $timing.attemptUpdatedAtMillis -and $timing.attemptCreatedAtMillis -gt $timing.attemptUpdatedAtMillis) { Add-P5EError $Errors 'PROVENANCE_ATTEMPT_ORDER_INVALID' }
    if ($null -ne $timing.attemptUpdatedAtMillis -and $null -ne $timing.observedAtMillis -and $timing.attemptUpdatedAtMillis -gt $timing.observedAtMillis) { Add-P5EError $Errors 'PROVENANCE_OBSERVATION_BEFORE_ATTEMPT_UPDATE' }
    if ($null -ne $Metadata -and $null -ne $timing.consumedAtMillis) {
        $issued = [long](Get-P5EProperty $Metadata 'authorizationIssuedAtMillis')
        $expires = [long](Get-P5EProperty $Metadata 'authorizationExpiresAtMillis')
        if ($timing.consumedAtMillis -lt $issued -or $timing.consumedAtMillis -ge $expires) { Add-P5EError $Errors 'PROVENANCE_CONSUME_OUTSIDE_AUTHORIZATION_WINDOW' }
    }
}

function Test-P5EReadback {
    param(
        [Parameter(Mandatory = $true)]$Readback,
        [Parameter(Mandatory = $true)][string]$ExpectedAccountFingerprint,
        $Metadata,
        [Parameter(Mandatory = $true)][string]$PostReadbackPath,
        [Parameter(Mandatory = $true)][string]$MetadataPath,
        [Parameter(Mandatory = $true)][string]$CollectorPath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
    )
    Test-P5EObjectShape -Object $Readback -Path 'readback' `
        -Required @('schemaVersion', 'observedAtMillis', 'externalCallState', 'production', 'test',
            'database', 'freshTuple', 'lineageBefore', 'lineageAfter', 'attempt',
            'authorizationReceipt', 'lifecycle', 'artifacts', 'integrity', 'provenance') `
        -Allowed @('schemaVersion', 'observedAtMillis', 'externalCallState', 'production', 'test',
            'database', 'freshTuple', 'lineageBefore', 'lineageAfter', 'attempt',
            'authorizationReceipt', 'lifecycle', 'artifacts', 'integrity', 'provenance') -Errors $Errors | Out-Null
    Test-P5EReadbackProvenance -Readback $Readback -Metadata $Metadata -PostReadbackPath $PostReadbackPath `
        -MetadataPath $MetadataPath -CollectorPath $CollectorPath -Errors $Errors
    Test-P5EEqual -Object $Readback -Name 'schemaVersion' -Expected $script:P5EReadbackSchema -Path 'readback' -Errors $Errors
    Test-P5EEqual -Object $Readback -Name 'externalCallState' -Expected 'COMMITTED' -Path 'readback' -Errors $Errors
    Test-P5ELongRange -Object $Readback -Name 'observedAtMillis' -Path 'readback' -Errors $Errors -Minimum 0 | Out-Null

    $production = Get-P5EProperty $Readback 'production'
    Test-P5EObjectShape -Object $production -Path 'production' -Required @('package', 'version', 'versionCode', 'apkSha256', 'certificateSha256') -Allowed @('package', 'version', 'versionCode', 'apkSha256', 'certificateSha256') -Errors $Errors | Out-Null
    Test-P5EEqual $production 'package' $script:P5ETargetPackage 'production' $Errors
    Test-P5EEqual $production 'version' $script:P5EProductionVersion 'production' $Errors
    Test-P5ELong $production 'versionCode' $script:P5EProductionVersionCode 'production' $Errors -Minimum 0 | Out-Null
    Test-P5ESha256 $production 'apkSha256' 'production' $Errors $script:P5EExpectedProductionApkSha256
    Test-P5ESha256 $production 'certificateSha256' 'production' $Errors $script:P5ECertificateSha256

    $test = Get-P5EProperty $Readback 'test'
    Test-P5EObjectShape $test 'test' @('package', 'targetPackage', 'apkSha256', 'certificateSha256', 'runner', 'sourceCommit') @('package', 'targetPackage', 'apkSha256', 'certificateSha256', 'runner', 'sourceCommit') $Errors | Out-Null
    Test-P5EEqual $test 'package' $script:P5ETestPackage 'test' $Errors
    Test-P5EEqual $test 'targetPackage' $script:P5ETargetPackage 'test' $Errors
    Test-P5ESha256 $test 'apkSha256' 'test' $Errors '57ec99a95ee2dc0f1759934c62cea39e2ec92eb77c3daf76cfeed28d41a2fdea'
    Test-P5ESha256 $test 'certificateSha256' 'test' $Errors $script:P5ECertificateSha256
    Test-P5EEqual $test 'runner' $script:P5ERunner 'test' $Errors
    Test-P5EEqual $test 'sourceCommit' $script:P5ETestSourceCommit 'test' $Errors

    $database = Get-P5EProperty $Readback 'database'
    Test-P5EObjectShape $database 'database' @('beforeSha256', 'afterSha256', 'schemaVersion', 'integrityCheck', 'foreignKeyViolations') @('beforeSha256', 'afterSha256', 'schemaVersion', 'integrityCheck', 'foreignKeyViolations') $Errors | Out-Null
    Test-P5ESha256 $database 'beforeSha256' 'database' $Errors $script:P5EDatabaseSha256
    Test-P5ESha256 $database 'afterSha256' 'database' $Errors
    Test-P5ELong $database 'schemaVersion' $script:P5EDatabaseSchemaVersion 'database' $Errors -Minimum 0 | Out-Null
    Test-P5EEqual $database 'integrityCheck' 'ok' 'database' $Errors
    Test-P5ELong $database 'foreignKeyViolations' 0L 'database' $Errors -Minimum 0 | Out-Null

    $tuple = Get-P5EProperty $Readback 'freshTuple'
    Test-P5EObjectShape $tuple 'freshTuple' @('projectRowId', 'selector', 'chapterKey', 'bindingIdentity', 'runDeclarationIdentity', 'evaluationId', 'packHash', 'profileHash', 'sourceMode', 'sourceProjection', 'sources') @('projectRowId', 'selector', 'chapterKey', 'bindingIdentity', 'runDeclarationIdentity', 'evaluationId', 'packHash', 'profileHash', 'sourceMode', 'sourceProjection', 'sources') $Errors | Out-Null
    Test-P5ELong $tuple 'projectRowId' $script:P5EProjectRowId 'freshTuple' $Errors -Minimum 1 | Out-Null
    Test-P5EEqual $tuple 'selector' $script:P5ESelector 'freshTuple' $Errors
    Test-P5EEqual $tuple 'chapterKey' $script:P5EChapterKey 'freshTuple' $Errors
    Test-P5EEqual $tuple 'bindingIdentity' $script:P5EBindingIdentity 'freshTuple' $Errors
    Test-P5EEqual $tuple 'runDeclarationIdentity' $script:P5ERunDeclarationIdentity 'freshTuple' $Errors
    Test-P5EEqual $tuple 'evaluationId' $script:P5EEvaluationId 'freshTuple' $Errors
    Test-P5EEqual $tuple 'packHash' $script:P5EPackHash 'freshTuple' $Errors
    Test-P5EEqual $tuple 'profileHash' $script:P5EProfileHash 'freshTuple' $Errors
    Test-P5EEqual $tuple 'sourceMode' 'NORMAL_FOUR_SOURCE' 'freshTuple' $Errors
    Test-P5EEqual $tuple 'sourceProjection' 'RAW_AND_GLOSSARY_VISIBLE_DRAFT_AND_PRONOUN_HIDDEN' 'freshTuple' $Errors
    $sourceArray = @(Get-P5EProperty $tuple 'sources')
    if ($sourceArray.Count -ne 4) { Add-P5EError $Errors 'SOURCE_IDENTITY_COUNT_INVALID' }
    $sourceExpected = [ordered]@{
        RAW = @($script:P5ERawSourceBytes, $script:P5ERawSourceSha256, 'VISIBLE')
        GLOSSARY = @($script:P5EGlossarySourceBytes, $script:P5EGlossarySourceSha256, 'VISIBLE')
        DRAFT = @($script:P5EDraftSourceBytes, $script:P5EDraftSourceSha256, 'HIDDEN')
        PRONOUN = @($script:P5EPronounSourceBytes, $script:P5EPronounSourceSha256, 'HIDDEN')
    }
    $seenRoles = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($source in $sourceArray) {
        Test-P5EObjectShape $source 'freshTuple.sources[]' @('role', 'visibility', 'byteLength', 'sha256') @('role', 'visibility', 'byteLength', 'sha256') $Errors | Out-Null
        $role = [string](Get-P5EProperty $source 'role')
        if (-not $seenRoles.Add($role) -or -not $sourceExpected.Contains($role)) { Add-P5EError $Errors 'SOURCE_ROLE_DUPLICATE_OR_UNEXPECTED' ; continue }
        $expectedSource = $sourceExpected[$role]
        Test-P5EEqual $source 'visibility' $expectedSource[2] ('freshTuple.sources.' + $role) $Errors
        Test-P5ELong $source 'byteLength' ([long]$expectedSource[0]) ('freshTuple.sources.' + $role) $Errors -Minimum 0 | Out-Null
        Test-P5ESha256 $source 'sha256' ('freshTuple.sources.' + $role) $Errors $expectedSource[1]
    }

    $countRequired = @('attempts', 'authorizationReceipts', 'reconciliation', 'reconciliationHistory', 'lifecycle', 'reportOrReceipt')
    foreach ($name in @('lineageBefore', 'lineageAfter')) {
        $counts = Get-P5EProperty $Readback $name
        Test-P5EObjectShape $counts $name $countRequired $countRequired $Errors | Out-Null
    }
    $before = Get-P5EProperty $Readback 'lineageBefore'
    foreach ($name in $countRequired) { Test-P5ELong $before $name 0L 'lineageBefore' $Errors -Minimum 0 | Out-Null }
    $after = Get-P5EProperty $Readback 'lineageAfter'
    Test-P5ELong $after 'attempts' 1L 'lineageAfter' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $after 'authorizationReceipts' 1L 'lineageAfter' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $after 'reconciliation' 0L 'lineageAfter' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $after 'reconciliationHistory' 0L 'lineageAfter' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $after 'lifecycle' 1L 'lineageAfter' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $after 'reportOrReceipt' 1L 'lineageAfter' $Errors -Minimum 0 | Out-Null

    $attempt = Get-P5EProperty $Readback 'attempt'
    Test-P5EObjectShape $attempt 'attempt' @('rowCount', 'attemptIdentity', 'requestIdentity', 'bindingIdentity', 'runDeclarationIdentity', 'chapterKey', 'phase', 'predecessorIdentity', 'requestEnvelopeHash', 'provider', 'model', 'status', 'responseIdentity', 'createdAtMillis', 'updatedAtMillis', 'reportByteLength', 'reportSha256', 'receiptByteLength', 'receiptSha256', 'metrics') @('rowCount', 'attemptIdentity', 'requestIdentity', 'bindingIdentity', 'runDeclarationIdentity', 'chapterKey', 'phase', 'predecessorIdentity', 'requestEnvelopeHash', 'provider', 'model', 'status', 'responseIdentity', 'createdAtMillis', 'updatedAtMillis', 'reportByteLength', 'reportSha256', 'receiptByteLength', 'receiptSha256', 'metrics') $Errors | Out-Null
    Test-P5ELong $attempt 'rowCount' 1L 'attempt' $Errors -Minimum 0 -Maximum 1 | Out-Null
    Test-P5ESha256 $attempt 'attemptIdentity' 'attempt' $Errors $script:P5EAttemptIdentity
    Test-P5ESha256 $attempt 'requestIdentity' 'attempt' $Errors $script:P5ERequestIdentity
    Test-P5ESha256 $attempt 'bindingIdentity' 'attempt' $Errors $script:P5EBindingIdentity
    Test-P5ESha256 $attempt 'runDeclarationIdentity' 'attempt' $Errors $script:P5ERunDeclarationIdentity
    Test-P5EEqual $attempt 'chapterKey' $script:P5EChapterKey 'attempt' $Errors
    Test-P5EEqual $attempt 'phase' $script:P5EPhase 'attempt' $Errors
    Test-P5ESha256 $attempt 'predecessorIdentity' 'attempt' $Errors $script:P5ERunDeclarationIdentity
    Test-P5ESha256 $attempt 'requestEnvelopeHash' 'attempt' $Errors $script:P5ERequestEnvelopeHash
    Test-P5EEqual $attempt 'provider' $script:P5EProvider 'attempt' $Errors
    Test-P5EEqual $attempt 'model' $script:P5EModel 'attempt' $Errors
    Test-P5EEqual $attempt 'status' 'COMMITTED' 'attempt' $Errors
    Test-P5ESha256 $attempt 'responseIdentity' 'attempt' $Errors
    $created = Test-P5ELongRange -Object $attempt -Name 'createdAtMillis' -Path 'attempt' -Errors $Errors -Minimum 0
    $updated = Test-P5ELongRange -Object $attempt -Name 'updatedAtMillis' -Path 'attempt' -Errors $Errors -Minimum 0
    if ($null -ne $created -and $null -ne $updated -and $updated -lt $created) { Add-P5EError $Errors 'ATTEMPT_TIMESTAMP_ORDER_INVALID' }
    $attemptReportBytes = Test-P5ELongRange -Object $attempt -Name 'reportByteLength' -Path 'attempt' -Errors $Errors -Minimum 1
    Test-P5ESha256 $attempt 'reportSha256' 'attempt' $Errors
    $attemptReceiptBytes = Test-P5ELongRange -Object $attempt -Name 'receiptByteLength' -Path 'attempt' -Errors $Errors -Minimum 1
    Test-P5ESha256 $attempt 'receiptSha256' 'attempt' $Errors
    $metrics = Get-P5EProperty $attempt 'metrics'
    Test-P5EObjectShape $metrics 'attempt.metrics' @('providerCallsBeforePreflight', 'primaryCalls', 'repairCalls', 'networkRetries', 'inputTokens', 'outputTokens', 'reasoningTokens', 'totalTokens', 'estimatedCost', 'actualReportedCost', 'requestContextSize', 'finishReason', 'truncated', 'schemaValidationPassed', 'receiptValidationPassed', 'preserveDraftCount', 'findingCount', 'falseStopCount', 'latencyMillis', 'costAccountingComplete') @('providerCallsBeforePreflight', 'primaryCalls', 'repairCalls', 'networkRetries', 'inputTokens', 'outputTokens', 'reasoningTokens', 'totalTokens', 'estimatedCost', 'actualReportedCost', 'requestContextSize', 'finishReason', 'truncated', 'schemaValidationPassed', 'receiptValidationPassed', 'preserveDraftCount', 'findingCount', 'falseStopCount', 'latencyMillis', 'costAccountingComplete') $Errors | Out-Null
    Test-P5ELong $metrics 'providerCallsBeforePreflight' 0L 'attempt.metrics' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $metrics 'primaryCalls' 1L 'attempt.metrics' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $metrics 'repairCalls' 0L 'attempt.metrics' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $metrics 'networkRetries' 0L 'attempt.metrics' $Errors -Minimum 0 | Out-Null
    $inputTokens = Test-P5ELongRange -Object $metrics -Name 'inputTokens' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 -Maximum 100000
    $outputTokens = Test-P5ELongRange -Object $metrics -Name 'outputTokens' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 -Maximum 4096
    $reasoningTokens = Test-P5ELongRange -Object $metrics -Name 'reasoningTokens' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 -Maximum 104096
    $totalTokens = Test-P5ELongRange -Object $metrics -Name 'totalTokens' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 -Maximum 104096
    if ($null -ne $inputTokens -and $null -ne $outputTokens -and $null -ne $totalTokens -and
            $totalTokens -lt $inputTokens + $outputTokens) {
        Add-P5EError -Errors $Errors -Code 'TOTAL_TOKENS_BELOW_INPUT_PLUS_OUTPUT'
    }
    $estimated = Test-P5EDecimalCap $metrics 'estimatedCost' 0.05D 'attempt.metrics' $Errors
    $reported = Test-P5EDecimalCap $metrics 'actualReportedCost' 0.05D 'attempt.metrics' $Errors
    if ($null -ne $estimated -and $null -ne $reported -and $estimated + $reported -gt 0.05D) { Add-P5EError $Errors 'TOTAL_COST_OVER_CAP' }
    Test-P5ELongRange -Object $metrics -Name 'requestContextSize' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 -Maximum 100000 | Out-Null
    $finishReason = [string](Get-P5EProperty $metrics 'finishReason')
    if ([string]::IsNullOrWhiteSpace($finishReason) -or $finishReason -ieq 'length') {
        Add-P5EError -Errors $Errors -Code 'FINISH_REASON_INVALID'
    }
    Test-P5EBoolean $metrics 'truncated' $false 'attempt.metrics' $Errors
    Test-P5EBoolean $metrics 'schemaValidationPassed' $true 'attempt.metrics' $Errors
    Test-P5EBoolean $metrics 'receiptValidationPassed' $true 'attempt.metrics' $Errors
    Test-P5ELongRange -Object $metrics -Name 'preserveDraftCount' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 | Out-Null
    Test-P5ELongRange -Object $metrics -Name 'findingCount' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 | Out-Null
    Test-P5ELongRange -Object $metrics -Name 'falseStopCount' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 | Out-Null
    Test-P5ELongRange -Object $metrics -Name 'latencyMillis' -Path 'attempt.metrics' -Errors $Errors -Minimum 0 -Maximum $script:P5EExecutionDeadlineMilliseconds | Out-Null
    Test-P5EBoolean $metrics 'costAccountingComplete' $true 'attempt.metrics' $Errors

    $auth = Get-P5EProperty $Readback 'authorizationReceipt'
    Test-P5EObjectShape $auth 'authorizationReceipt' @('rowCount', 'authorizationIdHash', 'exactPhase', 'attemptIdentity', 'requestIdentity', 'bindingIdentity', 'runDeclarationIdentity', 'chapterKey', 'provider', 'model', 'endpointAccountFingerprint', 'issuedAtMillis', 'expiresAtMillis', 'consumedAtMillis', 'maximumPrimaryCalls', 'maximumSchemaRepairCalls', 'maximumNetworkRetries', 'maximumInputTokens', 'maximumOutputTokens', 'maximumTotalTokens', 'maximumTotalCost', 'maximumExecutionTimeMillis', 'consumptionResult', 'consumedAttemptIdentity') @('rowCount', 'authorizationIdHash', 'exactPhase', 'attemptIdentity', 'requestIdentity', 'bindingIdentity', 'runDeclarationIdentity', 'chapterKey', 'provider', 'model', 'endpointAccountFingerprint', 'issuedAtMillis', 'expiresAtMillis', 'consumedAtMillis', 'maximumPrimaryCalls', 'maximumSchemaRepairCalls', 'maximumNetworkRetries', 'maximumInputTokens', 'maximumOutputTokens', 'maximumTotalTokens', 'maximumTotalCost', 'maximumExecutionTimeMillis', 'consumptionResult', 'consumedAttemptIdentity') $Errors | Out-Null
    Test-P5ELong $auth 'rowCount' 1L 'authorizationReceipt' $Errors -Minimum 0 -Maximum 1 | Out-Null
    Test-P5ESha256 $auth 'authorizationIdHash' 'authorizationReceipt' $Errors $script:P5EAuthorizationIdSha256
    Test-P5EEqual $auth 'exactPhase' $script:P5EPhase 'authorizationReceipt' $Errors
    Test-P5ESha256 $auth 'attemptIdentity' 'authorizationReceipt' $Errors $script:P5EAttemptIdentity
    Test-P5ESha256 $auth 'requestIdentity' 'authorizationReceipt' $Errors $script:P5ERequestIdentity
    Test-P5ESha256 $auth 'bindingIdentity' 'authorizationReceipt' $Errors $script:P5EBindingIdentity
    Test-P5ESha256 $auth 'runDeclarationIdentity' 'authorizationReceipt' $Errors $script:P5ERunDeclarationIdentity
    Test-P5EEqual $auth 'chapterKey' $script:P5EChapterKey 'authorizationReceipt' $Errors
    Test-P5EEqual $auth 'provider' $script:P5EProvider 'authorizationReceipt' $Errors
    Test-P5EEqual $auth 'model' $script:P5EModel 'authorizationReceipt' $Errors
    if ([string]$ExpectedAccountFingerprint -eq '' -or [string](Get-P5EProperty $auth 'endpointAccountFingerprint') -cne $ExpectedAccountFingerprint) { Add-P5EError $Errors 'ACCOUNT_FINGERPRINT_MISMATCH' }
    $issued = Test-P5ELong $auth 'issuedAtMillis' ([long](Get-P5EProperty $auth 'issuedAtMillis')) 'authorizationReceipt' $Errors -Minimum 0
    $expires = Test-P5ELong $auth 'expiresAtMillis' ([long](Get-P5EProperty $auth 'expiresAtMillis')) 'authorizationReceipt' $Errors -Minimum 0
    $consumed = Test-P5ELong $auth 'consumedAtMillis' ([long](Get-P5EProperty $auth 'consumedAtMillis')) 'authorizationReceipt' $Errors -Minimum 0
    if ($null -ne $issued -and $null -ne $expires -and $expires -ne $issued + $script:P5EAuthorizationValidityMilliseconds) { Add-P5EError $Errors 'AUTHORIZATION_RECEIPT_WINDOW_INVALID' }
    if ($null -ne $consumed -and $null -ne $issued -and $consumed -lt $issued) { Add-P5EError $Errors 'AUTHORIZATION_CONSUMPTION_TIME_INVALID' }
    if ($null -ne $consumed -and $null -ne $expires -and $consumed -ge $expires) { Add-P5EError $Errors 'AUTHORIZATION_CONSUMPTION_AFTER_EXPIRY' }
    if ($null -ne $Metadata -and $null -ne $issued -and $issued -ne [long](Get-P5EProperty $Metadata 'authorizationIssuedAtMillis')) { Add-P5EError $Errors 'AUTHORIZATION_ISSUED_TIME_MISMATCH' }
    if ($null -ne $Metadata -and $null -ne $expires -and $expires -ne [long](Get-P5EProperty $Metadata 'authorizationExpiresAtMillis')) { Add-P5EError $Errors 'AUTHORIZATION_EXPIRES_TIME_MISMATCH' }
    Test-P5ELong $auth 'maximumPrimaryCalls' 1L 'authorizationReceipt' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $auth 'maximumSchemaRepairCalls' 0L 'authorizationReceipt' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $auth 'maximumNetworkRetries' 0L 'authorizationReceipt' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $auth 'maximumInputTokens' 100000L 'authorizationReceipt' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $auth 'maximumOutputTokens' 4096L 'authorizationReceipt' $Errors -Minimum 0 | Out-Null
    Test-P5ELong $auth 'maximumTotalTokens' 104096L 'authorizationReceipt' $Errors -Minimum 0 | Out-Null
    Test-P5EDecimalCap $auth 'maximumTotalCost' 0.05D 'authorizationReceipt' $Errors | Out-Null
    Test-P5ELong $auth 'maximumExecutionTimeMillis' $script:P5EExecutionDeadlineMilliseconds 'authorizationReceipt' $Errors -Minimum 1 | Out-Null
    Test-P5EEqual $auth 'consumptionResult' 'CONSUMED' 'authorizationReceipt' $Errors
    Test-P5ESha256 $auth 'consumedAttemptIdentity' 'authorizationReceipt' $Errors $script:P5EAttemptIdentity

    $lifecycle = Get-P5EProperty $Readback 'lifecycle'
    Test-P5EObjectShape $lifecycle 'lifecycle' @('rowCount', 'attemptIdentity', 'stage', 'requestBodyBytes', 'responseBodyBytes', 'httpStatus', 'responseContentType', 'exceptionClass', 'elapsedMillis', 'generationId', 'providerResponseId', 'cancellationSource') @('rowCount', 'attemptIdentity', 'stage', 'requestBodyBytes', 'responseBodyBytes', 'httpStatus', 'responseContentType', 'exceptionClass', 'elapsedMillis', 'generationId', 'providerResponseId', 'cancellationSource') $Errors | Out-Null
    Test-P5ELong $lifecycle 'rowCount' 1L 'lifecycle' $Errors -Minimum 0 -Maximum 1 | Out-Null
    Test-P5ESha256 $lifecycle 'attemptIdentity' 'lifecycle' $Errors $script:P5EAttemptIdentity
    Test-P5EEqual $lifecycle 'stage' 'RESPONSE_BODY_COMPLETE' 'lifecycle' $Errors
    Test-P5ELongRange -Object $lifecycle -Name 'requestBodyBytes' -Path 'lifecycle' -Errors $Errors -Minimum 1 | Out-Null
    Test-P5ELongRange -Object $lifecycle -Name 'responseBodyBytes' -Path 'lifecycle' -Errors $Errors -Minimum 1 | Out-Null
    $httpStatus = Test-P5ELong $lifecycle 'httpStatus' 200L 'lifecycle' $Errors -Minimum 200 -Maximum 299
    if ((Test-P5EHasProperty -Object $lifecycle -Name 'responseContentType') -and
            ([string](Get-P5EProperty $lifecycle 'responseContentType') -notmatch '(?i)^application/json(?:;|$)')) {
        Add-P5EError -Errors $Errors -Code 'LIFECYCLE_CONTENT_TYPE_INVALID'
    }
    Test-P5EEqual $lifecycle 'exceptionClass' '' 'lifecycle' $Errors
    Test-P5ELongRange -Object $lifecycle -Name 'elapsedMillis' -Path 'lifecycle' -Errors $Errors -Minimum 0 -Maximum $script:P5EExecutionDeadlineMilliseconds | Out-Null
    if ([string]::IsNullOrWhiteSpace([string](Get-P5EProperty $lifecycle 'generationId'))) { Add-P5EError $Errors 'LIFECYCLE_GENERATION_ID_MISSING' }
    if ([string]::IsNullOrWhiteSpace([string](Get-P5EProperty $lifecycle 'providerResponseId'))) { Add-P5EError $Errors 'LIFECYCLE_PROVIDER_RESPONSE_ID_MISSING' }
    Test-P5EEqual $lifecycle 'cancellationSource' '' 'lifecycle' $Errors

    $artifacts = Get-P5EProperty $Readback 'artifacts'
    Test-P5EObjectShape $artifacts 'artifacts' @('report', 'receipt') @('report', 'receipt') $Errors | Out-Null
    foreach ($kind in @('report', 'receipt')) {
        $artifact = Get-P5EProperty $artifacts $kind
        $artifactRequired = if ($kind -eq 'report') {
            @('schemaVersion', 'artifactType', 'bindingIdentity', 'manifestFingerprint', 'canonicalPackHash',
                'canonicalProfileHash', 'compatibilityEvaluationId', 'chapterKey', 'phase', 'bundleIdentity',
                'predecessorIdentity', 'stableAnchors', 'populationTotal', 'accountedTotal',
                'actualChangedSpans', 'gates', 'evidenceRefs', 'preservedInventory', 'disposition',
                'modelDeclaredPassRecordedOnly', 'byteLength', 'sha256', 'validationPassed')
        } else {
            @('schemaVersion', 'artifactType', 'manifestRef', 'packRef', 'profileRef', 'bindingRef',
                'phase', 'bundleIdentity', 'predecessorIdentity', 'populationTotal', 'accountedTotal',
                'changedSpanTotal', 'gates', 'evidenceRefs', 'canonAllowed', 'propagationAllowed',
                'disposition', 'preservedInventory', 'releaseAttemptCount', 'byteLength', 'sha256',
                'validationPassed')
        }
        Test-P5EObjectShape $artifact ('artifacts.' + $kind) $artifactRequired $artifactRequired $Errors | Out-Null
        $schema = if ($kind -eq 'report') { 'safe4.full.report-l1.v1' } else { 'safe4.full.receipt.v1' }
        Test-P5EEqual $artifact 'schemaVersion' $schema ('artifacts.' + $kind) $Errors
        Test-P5EEqual $artifact 'artifactType' 'REPORT_L1' ('artifacts.' + $kind) $Errors
        if ($kind -eq 'report') {
            Test-P5ESha256 $artifact 'bindingIdentity' ('artifacts.' + $kind) $Errors $script:P5EBindingIdentity
            Test-P5ESha256 $artifact 'manifestFingerprint' ('artifacts.' + $kind) $Errors $script:P5EExpectedManifestFingerprint
            Test-P5ESha256 $artifact 'canonicalPackHash' ('artifacts.' + $kind) $Errors $script:P5EPackHash
            Test-P5ESha256 $artifact 'canonicalProfileHash' ('artifacts.' + $kind) $Errors $script:P5EProfileHash
            Test-P5EEqual $artifact 'compatibilityEvaluationId' $script:P5EEvaluationId ('artifacts.' + $kind) $Errors
            Test-P5EEqual $artifact 'chapterKey' $script:P5EChapterKey ('artifacts.' + $kind) $Errors
        } else {
            Test-P5ESha256 $artifact 'bindingRef' ('artifacts.' + $kind) $Errors $script:P5EBindingIdentity
            Test-P5ESha256 $artifact 'manifestRef' ('artifacts.' + $kind) $Errors $script:P5EExpectedManifestFingerprint
            Test-P5ESha256 $artifact 'packRef' ('artifacts.' + $kind) $Errors $script:P5EPackHash
            Test-P5ESha256 $artifact 'profileRef' ('artifacts.' + $kind) $Errors $script:P5EProfileHash
        }
        Test-P5EEqual $artifact 'phase' $script:P5EPhase ('artifacts.' + $kind) $Errors
        Test-P5ESha256 $artifact 'predecessorIdentity' ('artifacts.' + $kind) $Errors $script:P5ERunDeclarationIdentity
        $artifactBytes = Test-P5ELongRange -Object $artifact -Name 'byteLength' -Path ('artifacts.' + $kind) -Errors $Errors -Minimum 1
        $attemptBytes = if ($kind -eq 'report') { $attemptReportBytes } else { $attemptReceiptBytes }
        if ($null -ne $artifactBytes -and $null -ne $attemptBytes -and $artifactBytes -ne $attemptBytes) {
            Add-P5EError -Errors $Errors -Code ('ARTIFACT_BYTE_LENGTH_MISMATCH:' + $kind)
        }
        Test-P5ESha256 $artifact 'sha256' ('artifacts.' + $kind) $Errors
        Test-P5EBoolean $artifact 'validationPassed' $true ('artifacts.' + $kind) $Errors
    }
    $reportArtifact = Get-P5EProperty $artifacts 'report'
    $receiptArtifact = Get-P5EProperty $artifacts 'receipt'
    if ((Test-P5EHasProperty -Object $attempt -Name 'reportSha256') -and
            (Test-P5EHasProperty -Object $reportArtifact -Name 'sha256') -and
            (([string](Get-P5EProperty $attempt 'reportSha256')) -ine ([string](Get-P5EProperty $reportArtifact 'sha256')))) {
        Add-P5EError -Errors $Errors -Code 'REPORT_HASH_CROSS_REFERENCE_MISMATCH'
    }
    if ((Test-P5EHasProperty -Object $attempt -Name 'receiptSha256') -and
            (Test-P5EHasProperty -Object $receiptArtifact -Name 'sha256') -and
            (([string](Get-P5EProperty $attempt 'receiptSha256')) -ine ([string](Get-P5EProperty $receiptArtifact 'sha256')))) {
        Add-P5EError -Errors $Errors -Code 'RECEIPT_HASH_CROSS_REFERENCE_MISMATCH'
    }

    $integrity = Get-P5EProperty $Readback 'integrity'
    Test-P5EObjectShape $integrity 'integrity' @('allowedDiff', 'unrelatedWrites', 'bindingChanged', 'sourceChanged', 'runDeclarationChanged', 'settingsChanged', 'packChanged', 'profileChanged', 'freshTupleChanged', 'atomicClaim', 'reportReceiptAtomic', 'noDeletes') @('allowedDiff', 'unrelatedWrites', 'bindingChanged', 'sourceChanged', 'runDeclarationChanged', 'settingsChanged', 'packChanged', 'profileChanged', 'freshTupleChanged', 'atomicClaim', 'reportReceiptAtomic', 'noDeletes') $Errors | Out-Null
    Test-P5EBoolean $integrity 'allowedDiff' $true 'integrity' $Errors
    Test-P5ELong $integrity 'unrelatedWrites' 0L 'integrity' $Errors -Minimum 0 | Out-Null
    foreach ($name in @('bindingChanged', 'sourceChanged', 'runDeclarationChanged', 'settingsChanged', 'packChanged', 'profileChanged', 'freshTupleChanged')) { Test-P5EBoolean $integrity $name $false 'integrity' $Errors }
    Test-P5EBoolean $integrity 'atomicClaim' $true 'integrity' $Errors
    Test-P5EBoolean $integrity 'reportReceiptAtomic' $true 'integrity' $Errors
    Test-P5EBoolean $integrity 'noDeletes' $true 'integrity' $Errors
    if ($null -ne $httpStatus -and $httpStatus -lt 200) { Add-P5EError $Errors 'LIFECYCLE_HTTP_STATUS_INVALID' }
}

function New-P5EProducerInput {
    param(
        [Parameter(Mandatory = $true)][string]$EventId,
        [Parameter(Mandatory = $true)][string]$AccountFingerprint,
        [Parameter(Mandatory = $true)][long]$IssuedAtMillis
    )
    $responseHash = 'b' * 64
    return [ordered]@{
        schemaVersion = $script:P5EProducerInputSchema
        eventId = $EventId
        runIdentity = $script:P5ERunDeclarationIdentity
        sourceMappingVersion = $script:P5ESourceMappingVersion
        production = [ordered]@{
            package = $script:P5ETargetPackage
            version = $script:P5EProductionVersion
            versionCode = $script:P5EProductionVersionCode
            apkSha256 = $script:P5EExpectedProductionApkSha256
            certificateSha256 = $script:P5ECertificateSha256
        }
        test = [ordered]@{
            package = $script:P5ETestPackage
            targetPackage = $script:P5ETargetPackage
            apkSha256 = '57ec99a95ee2dc0f1759934c62cea39e2ec92eb77c3daf76cfeed28d41a2fdea'
            certificateSha256 = $script:P5ECertificateSha256
            runner = $script:P5ERunner
            sourceCommit = $script:P5ETestSourceCommit
        }
        freshTuple = [ordered]@{
            projectRowId = $script:P5EProjectRowId
            selector = $script:P5ESelector
            chapterKey = $script:P5EChapterKey
            bindingIdentity = $script:P5EBindingIdentity
            runDeclarationIdentity = $script:P5ERunDeclarationIdentity
            evaluationId = $script:P5EEvaluationId
            packHash = $script:P5EPackHash
            profileHash = $script:P5EProfileHash
            sourceMode = 'NORMAL_FOUR_SOURCE'
            sourceProjection = 'RAW_AND_GLOSSARY_VISIBLE_DRAFT_AND_PRONOUN_HIDDEN'
            sources = @(
                [ordered]@{ role = 'RAW'; visibility = 'VISIBLE'; byteLength = $script:P5ERawSourceBytes; sha256 = $script:P5ERawSourceSha256 },
                [ordered]@{ role = 'GLOSSARY'; visibility = 'VISIBLE'; byteLength = $script:P5EGlossarySourceBytes; sha256 = $script:P5EGlossarySourceSha256 },
                [ordered]@{ role = 'DRAFT'; visibility = 'HIDDEN'; byteLength = $script:P5EDraftSourceBytes; sha256 = $script:P5EDraftSourceSha256 },
                [ordered]@{ role = 'PRONOUN'; visibility = 'HIDDEN'; byteLength = $script:P5EPronounSourceBytes; sha256 = $script:P5EPronounSourceSha256 }
            )
        }
        attemptTemplate = [ordered]@{
            rowCount = 1
            attemptIdentity = $script:P5EAttemptIdentity
            requestIdentity = $script:P5ERequestIdentity
            bindingIdentity = $script:P5EBindingIdentity
            runDeclarationIdentity = $script:P5ERunDeclarationIdentity
            chapterKey = $script:P5EChapterKey
            phase = $script:P5EPhase
            predecessorIdentity = $script:P5ERunDeclarationIdentity
            requestEnvelopeHash = $script:P5ERequestEnvelopeHash
            provider = $script:P5EProvider
            model = $script:P5EModel
            status = 'COMMITTED'
            responseIdentity = $responseHash
            createdAtMillis = $IssuedAtMillis + 100
            updatedAtMillis = $IssuedAtMillis + 3000
            metrics = [ordered]@{
                providerCallsBeforePreflight = 0
                primaryCalls = 1
                repairCalls = 0
                networkRetries = 0
                inputTokens = 1000
                outputTokens = 100
                reasoningTokens = 0
                totalTokens = 1100
                estimatedCost = '0.000'
                actualReportedCost = '0.012'
                requestContextSize = 80317
                finishReason = 'stop'
                truncated = $false
                schemaValidationPassed = $true
                receiptValidationPassed = $true
                preserveDraftCount = 0
                findingCount = 1
                falseStopCount = 0
                latencyMillis = 1000
                costAccountingComplete = $true
            }
        }
        authorizationReceipt = [ordered]@{
            rowCount = 1
            authorizationIdHash = $script:P5EAuthorizationIdSha256
            exactPhase = $script:P5EPhase
            attemptIdentity = $script:P5EAttemptIdentity
            requestIdentity = $script:P5ERequestIdentity
            bindingIdentity = $script:P5EBindingIdentity
            runDeclarationIdentity = $script:P5ERunDeclarationIdentity
            chapterKey = $script:P5EChapterKey
            provider = $script:P5EProvider
            model = $script:P5EModel
            endpointAccountFingerprint = $AccountFingerprint
            issuedAtMillis = $IssuedAtMillis
            expiresAtMillis = $IssuedAtMillis + $script:P5EAuthorizationValidityMilliseconds
            consumedAtMillis = $IssuedAtMillis + 200
            maximumPrimaryCalls = 1
            maximumSchemaRepairCalls = 0
            maximumNetworkRetries = 0
            maximumInputTokens = 100000
            maximumOutputTokens = 4096
            maximumTotalTokens = 104096
            maximumTotalCost = '0.05'
            maximumExecutionTimeMillis = $script:P5EExecutionDeadlineMilliseconds
            consumptionResult = 'CONSUMED'
            consumedAttemptIdentity = $script:P5EAttemptIdentity
        }
        lifecycle = [ordered]@{
            rowCount = 1
            attemptIdentity = $script:P5EAttemptIdentity
            stage = 'RESPONSE_BODY_COMPLETE'
            requestBodyBytes = 3000
            responseBodyBytes = 300
            httpStatus = 200
            responseContentType = 'application/json'
            exceptionClass = ''
            elapsedMillis = 1000
            generationId = 'generation-1'
            providerResponseId = 'response-1'
            cancellationSource = ''
        }
        reportArtifactTemplate = [ordered]@{
            schemaVersion = 'safe4.full.report-l1.v1'
            artifactType = 'REPORT_L1'
            bindingIdentity = $script:P5EBindingIdentity
            manifestFingerprint = $script:P5EExpectedManifestFingerprint
            canonicalPackHash = $script:P5EPackHash
            canonicalProfileHash = $script:P5EProfileHash
            compatibilityEvaluationId = $script:P5EEvaluationId
            chapterKey = $script:P5EChapterKey
            phase = $script:P5EPhase
            bundleIdentity = 'bundle-p5e-synthetic-001'
            predecessorIdentity = $script:P5ERunDeclarationIdentity
            stableAnchors = @('anchor-p5e-001')
            populationTotal = 1
            accountedTotal = 1
            actualChangedSpans = 0
            gates = [ordered]@{
                ARTIFACT_IDENTITY = 'PASS'; COVERAGE = 'PASS'; TITLE_GLOSSARY = 'PASS';
                SEMANTIC_FIDELITY = 'PASS'; RELATION_PAIR_PROOF = 'PASS'; SPEAKER = 'PASS';
                CHANGE_COVERAGE = 'PASS'; NO_REGRESSION = 'PASS'; CONTINUITY_STRUCTURE_TECHNICAL = 'PASS'
            }
            evidenceRefs = @('evidence-p5e-synthetic-001')
            preservedInventory = @()
            disposition = 'COMMITTED'
            modelDeclaredPassRecordedOnly = $false
        }
        receiptArtifactTemplate = [ordered]@{
            schemaVersion = 'safe4.full.receipt.v1'
            artifactType = 'REPORT_L1'
            manifestRef = $script:P5EExpectedManifestFingerprint
            packRef = $script:P5EPackHash
            profileRef = $script:P5EProfileHash
            bindingRef = $script:P5EBindingIdentity
            phase = $script:P5EPhase
            bundleIdentity = 'bundle-p5e-synthetic-001'
            predecessorIdentity = $script:P5ERunDeclarationIdentity
            populationTotal = 1
            accountedTotal = 1
            changedSpanTotal = 0
            gates = [ordered]@{
                ARTIFACT_IDENTITY = 'PASS'; COVERAGE = 'PASS'; TITLE_GLOSSARY = 'PASS';
                SEMANTIC_FIDELITY = 'PASS'; RELATION_PAIR_PROOF = 'PASS'; SPEAKER = 'PASS';
                CHANGE_COVERAGE = 'PASS'; NO_REGRESSION = 'PASS'; CONTINUITY_STRUCTURE_TECHNICAL = 'PASS'
            }
            evidenceRefs = @('evidence-p5e-synthetic-001')
            canonAllowed = $true
            propagationAllowed = $true
            disposition = 'COMMITTED'
            preservedInventory = @()
            releaseAttemptCount = 0
        }
    }
}

function Invoke-P5ESyntheticReadbackCollector {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][string]$MetadataPath,
        [Parameter(Mandatory = $true)][string]$ProducerInputDirectory,
        [Parameter(Mandatory = $true)][string]$PostReadbackPath
    )
    # This is an offline producer for a disposable input bundle. It never
    # invokes adb, instrumentation, a provider, a database, or an environment
    # credential. A future device collector must preserve this same mapping.
    $inputPath = Join-Path $ProducerInputDirectory 'collector-input.json'
    $beforePath = Join-Path $ProducerInputDirectory 'before-snapshot.json'
    $afterPath = Join-Path $ProducerInputDirectory 'after-snapshot.json'
    $transactionPath = Join-Path $ProducerInputDirectory 'transaction-evidence.json'
    $reportPath = Join-Path $ProducerInputDirectory 'report.bin'
    $receiptPath = Join-Path $ProducerInputDirectory 'receipt.bin'
    foreach ($path in @($inputPath, $beforePath, $afterPath, $transactionPath, $reportPath, $receiptPath)) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw ('P5E_COLLECTOR_INPUT_MISSING:' + (Split-Path -Leaf $path)) }
    }
    if (-not (Test-P5EPathEqual -Left $EvidenceDirectory -Right $ProducerInputDirectory) -or
            -not (Test-P5EPathEqual -Left $PostReadbackPath -Right (Join-Path $EvidenceDirectory 'post-readback.json'))) {
        throw 'P5E_COLLECTOR_EVENT_DIRECTORY_MISMATCH'
    }
    try {
        $metadata = Get-Content -Raw -LiteralPath $MetadataPath | ConvertFrom-Json
        $producerInput = Get-Content -Raw -LiteralPath $inputPath | ConvertFrom-Json
        $before = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json
        $after = Get-Content -Raw -LiteralPath $afterPath | ConvertFrom-Json
        $transaction = Get-Content -Raw -LiteralPath $transactionPath | ConvertFrom-Json
    } catch { throw ('P5E_COLLECTOR_SOURCE_JSON_INVALID:' + $_.Exception.GetType().Name + ':' + $_.Exception.Message) }
    $eventId = [string](Get-P5EProperty $metadata 'eventId')
    if ([string]::IsNullOrWhiteSpace($eventId) -or
            [string](Get-P5EProperty $producerInput 'eventId') -cne $eventId -or
            [string](Get-P5EProperty $before 'eventId') -cne $eventId -or
            [string](Get-P5EProperty $after 'eventId') -cne $eventId -or
            [string](Get-P5EProperty $transaction 'eventId') -cne $eventId) {
        throw 'P5E_COLLECTOR_EVENT_ID_MISMATCH'
    }
    if ([string](Get-P5EProperty $producerInput 'schemaVersion') -cne $script:P5EProducerInputSchema -or
            [string](Get-P5EProperty $producerInput 'runIdentity') -cne $script:P5ERunDeclarationIdentity -or
            [string](Get-P5EProperty $producerInput 'sourceMappingVersion') -cne $script:P5ESourceMappingVersion) {
        throw 'P5E_COLLECTOR_INPUT_SCHEMA_OR_IDENTITY_INVALID'
    }
    foreach ($snapshot in @($before, $after)) {
        if ([string](Get-P5EProperty $snapshot 'schemaVersion') -cne 'p5e.raw.snapshot.v1' -or
                [string](Get-P5EProperty $snapshot 'runIdentity') -cne $script:P5ERunDeclarationIdentity -or
                [string](Get-P5EProperty $snapshot 'snapshotMode') -notin @('WAL_AWARE_CONSISTENT', 'WAL_AWARE_READ_TRANSACTION')) {
            throw 'P5E_COLLECTOR_SNAPSHOT_NOT_CONSISTENT'
        }
    }
    $transactionRequired = @('schemaVersion', 'eventId', 'runIdentity', 'transactionTestId',
        'transactionSemanticsPassed', 'beforeAfterConsistent', 'rowPairConsistent',
        'claimAndAuthorizationAtomic', 'reportReceiptAtomic', 'noUnrelatedWrites', 'noDeletes',
        'sourceTransactionCodePath', 'sourceTransactionCodeSha256',
        'sourceTransactionTestPath', 'sourceTransactionTestSha256', 'sourceTransactionTestStatus')
    foreach ($name in $transactionRequired) {
        if (-not (Test-P5EHasProperty -Object $transaction -Name $name)) { throw ('P5E_COLLECTOR_TRANSACTION_FIELD_MISSING:' + $name) }
    }
    if ([string](Get-P5EProperty $transaction 'schemaVersion') -cne 'p5e.raw.transaction-evidence.v1' -or
            [string](Get-P5EProperty $transaction 'runIdentity') -cne $script:P5ERunDeclarationIdentity) {
        throw 'P5E_COLLECTOR_TRANSACTION_SCHEMA_INVALID'
    }
    if (-not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $transaction 'sourceTransactionCodePath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionSourcePath)) -or
            [string](Get-P5EProperty $transaction 'sourceTransactionCodeSha256') -cne $script:P5ETransactionSourceSha256 -or
            -not (Test-P5EPathEqual -Left ([string](Get-P5EProperty $transaction 'sourceTransactionTestPath')) -Right (Join-Path (Get-P5ERepoRoot) $script:P5ETransactionTestPath)) -or
            [string](Get-P5EProperty $transaction 'sourceTransactionTestSha256') -cne $script:P5ETransactionTestSha256 -or
            [string](Get-P5EProperty $transaction 'sourceTransactionTestStatus') -cne 'PINNED_SOURCE_TRANSACTION_CONTRACT') {
        throw 'P5E_COLLECTOR_TRANSACTION_SOURCE_CONTRACT_INVALID'
    }
    $reportErrors = [System.Collections.Generic.List[string]]::new()
    $receiptErrors = [System.Collections.Generic.List[string]]::new()
    $reportChecked = Test-P5ESerializedArtifactBytes -Path $reportPath -ExpectedSchema 'safe4.full.report-l1.v1' -Kind 'report' -Errors $reportErrors
    $receiptChecked = Test-P5ESerializedArtifactBytes -Path $receiptPath -ExpectedSchema 'safe4.full.receipt.v1' -Kind 'receipt' -Errors $receiptErrors
    if ($reportErrors.Count -ne 0) { throw ('P5E_COLLECTOR_REPORT_VALIDATION_FAILED:' + ($reportErrors -join ',')) }
    if ($receiptErrors.Count -ne 0) { throw ('P5E_COLLECTOR_RECEIPT_VALIDATION_FAILED:' + ($receiptErrors -join ',')) }

    $reportArtifact = [ordered]@{}
    foreach ($property in @((Get-P5EProperty $producerInput 'reportArtifactTemplate').PSObject.Properties) ) { $reportArtifact[$property.Name] = $property.Value }
    $reportArtifact.byteLength = $reportChecked.Length
    $reportArtifact.sha256 = $reportChecked.Sha256
    $reportArtifact.validationPassed = $true
    $receiptArtifact = [ordered]@{}
    foreach ($property in @((Get-P5EProperty $producerInput 'receiptArtifactTemplate').PSObject.Properties) ) { $receiptArtifact[$property.Name] = $property.Value }
    $receiptArtifact.byteLength = $receiptChecked.Length
    $receiptArtifact.sha256 = $receiptChecked.Sha256
    $receiptArtifact.validationPassed = $true

    $attempt = [ordered]@{}
    foreach ($property in @((Get-P5EProperty $producerInput 'attemptTemplate').PSObject.Properties) ) { $attempt[$property.Name] = $property.Value }
    $attempt.reportByteLength = $reportChecked.Length
    $attempt.reportSha256 = $reportChecked.Sha256
    $attempt.receiptByteLength = $receiptChecked.Length
    $attempt.receiptSha256 = $receiptChecked.Sha256
    $immutableUnchanged = [string](Get-P5EProperty $before 'immutableTupleIdentity') -ceq [string](Get-P5EProperty $after 'immutableTupleIdentity')
    $integrity = [ordered]@{
        allowedDiff = [bool]((Get-P5EProperty $transaction 'noUnrelatedWrites') -and (Get-P5EProperty $transaction 'noDeletes') -and $immutableUnchanged -and [long](Get-P5EProperty $after 'unrelatedWrites') -eq 0)
        unrelatedWrites = [long](Get-P5EProperty $after 'unrelatedWrites')
        bindingChanged = -not $immutableUnchanged
        sourceChanged = -not $immutableUnchanged
        runDeclarationChanged = -not $immutableUnchanged
        settingsChanged = -not $immutableUnchanged
        packChanged = -not $immutableUnchanged
        profileChanged = -not $immutableUnchanged
        freshTupleChanged = -not $immutableUnchanged
        atomicClaim = [bool]((Get-P5EProperty $transaction 'transactionSemanticsPassed') -and (Get-P5EProperty $transaction 'rowPairConsistent') -and (Get-P5EProperty $transaction 'claimAndAuthorizationAtomic'))
        reportReceiptAtomic = [bool]((Get-P5EProperty $transaction 'reportReceiptAtomic') -and $reportArtifact.validationPassed -and $receiptArtifact.validationPassed)
        noDeletes = [bool]((Get-P5EProperty $transaction 'noDeletes') -and [long](Get-P5EProperty $after 'deletedRows') -eq 0)
    }
    $sourceMap = [ordered]@{
        input = Get-P5ECanonicalPath -Path $inputPath
        beforeSnapshot = Get-P5ECanonicalPath -Path $beforePath
        afterSnapshot = Get-P5ECanonicalPath -Path $afterPath
        transactionEvidence = Get-P5ECanonicalPath -Path $transactionPath
        reportBytes = Get-P5ECanonicalPath -Path $reportPath
        receiptBytes = Get-P5ECanonicalPath -Path $receiptPath
    }
    $provenance = [ordered]@{
        schemaVersion = $script:P5EReadbackProvenanceSchema
        sourceMappingVersion = $script:P5ESourceMappingVersion
        collectorImplementationId = $script:P5ECollectorImplementationId
        collectorImplementationVersion = $script:P5ECollectorImplementationVersion
        collectorImplementationPath = Get-P5ECanonicalPath -Path (Get-P5ECollectorPath)
        collectorImplementationSha256 = Get-P5ESha256 -Path (Get-P5ECollectorPath)
        eventId = $eventId
        evidenceDirectory = Get-P5ECanonicalPath -Path $EvidenceDirectory
        metadataPath = Get-P5ECanonicalPath -Path $MetadataPath
        postReadbackPath = Get-P5ECanonicalPath -Path $PostReadbackPath
        runIdentity = $script:P5ERunDeclarationIdentity
        sourceInputPath = $sourceMap.input
        sourceInputSha256 = Get-P5ESha256 -Path $inputPath
        beforeSnapshotPath = $sourceMap.beforeSnapshot
        beforeSnapshotSha256 = Get-P5ESha256 -Path $beforePath
        afterSnapshotPath = $sourceMap.afterSnapshot
        afterSnapshotSha256 = Get-P5ESha256 -Path $afterPath
        transactionEvidencePath = $sourceMap.transactionEvidence
        transactionEvidenceSha256 = Get-P5ESha256 -Path $transactionPath
        reportBytesPath = $sourceMap.reportBytes
        reportBytesSha256 = $reportChecked.Sha256
        reportBytesLength = $reportChecked.Length
        receiptBytesPath = $sourceMap.receiptBytes
        receiptBytesSha256 = $receiptChecked.Sha256
        receiptBytesLength = $receiptChecked.Length
        runStartedAtMillis = [long](Get-P5EProperty $metadata 'authorizationIssuedAtMillis')
        beforeSnapshotObservedAtMillis = [long](Get-P5EProperty $before 'observedAtMillis')
        claimObservedAtMillis = [long]$attempt['createdAtMillis']
        consumedAtMillis = [long](Get-P5EProperty (Get-P5EProperty $producerInput 'authorizationReceipt') 'consumedAtMillis')
        attemptCreatedAtMillis = [long]$attempt['createdAtMillis']
        attemptUpdatedAtMillis = [long]$attempt['updatedAtMillis']
        observedAtMillis = [long](Get-P5EProperty $after 'observedAtMillis')
        collectedAtMillis = [long](Get-P5EProperty $after 'observedAtMillis') + 100
        atomicityEvidence = [ordered]@{
            transactionEvidencePath = $sourceMap.transactionEvidence
            transactionEvidenceSha256 = Get-P5ESha256 -Path $transactionPath
            transactionSemanticsPassed = [bool](Get-P5EProperty $transaction 'transactionSemanticsPassed')
            beforeAfterConsistent = [bool](Get-P5EProperty $transaction 'beforeAfterConsistent')
            rowPairConsistent = [bool](Get-P5EProperty $transaction 'rowPairConsistent')
            claimAndAuthorizationAtomic = [bool](Get-P5EProperty $transaction 'claimAndAuthorizationAtomic')
            reportReceiptAtomic = [bool](Get-P5EProperty $transaction 'reportReceiptAtomic')
            reportBytesValidated = [bool]$reportArtifact.validationPassed
            receiptBytesValidated = [bool]$receiptArtifact.validationPassed
             validatorId = $script:P5ESyntheticArtifactValidatorId
             sourceTransactionCodePath = [string](Get-P5EProperty $transaction 'sourceTransactionCodePath')
             sourceTransactionCodeSha256 = [string](Get-P5EProperty $transaction 'sourceTransactionCodeSha256')
             sourceTransactionTestPath = [string](Get-P5EProperty $transaction 'sourceTransactionTestPath')
            sourceTransactionTestSha256 = [string](Get-P5EProperty $transaction 'sourceTransactionTestSha256')
            sourceTransactionTestStatus = [string](Get-P5EProperty $transaction 'sourceTransactionTestStatus')
        }
    }
    $readback = [ordered]@{
        schemaVersion = $script:P5EReadbackSchema
        observedAtMillis = [long](Get-P5EProperty $after 'observedAtMillis')
        externalCallState = 'COMMITTED'
        production = Get-P5EProperty $producerInput 'production'
        test = Get-P5EProperty $producerInput 'test'
        database = [ordered]@{
            beforeSha256 = Get-P5EProperty $before 'databaseSha256'
            afterSha256 = Get-P5EProperty $after 'databaseSha256'
            schemaVersion = [long](Get-P5EProperty $after 'databaseSchemaVersion')
            integrityCheck = Get-P5EProperty $after 'integrityCheck'
            foreignKeyViolations = [long](Get-P5EProperty $after 'foreignKeyViolations')
        }
        freshTuple = Get-P5EProperty $producerInput 'freshTuple'
        lineageBefore = Get-P5EProperty $before 'lineage'
        lineageAfter = Get-P5EProperty $after 'lineage'
        attempt = $attempt
        authorizationReceipt = Get-P5EProperty $producerInput 'authorizationReceipt'
        lifecycle = Get-P5EProperty $producerInput 'lifecycle'
        artifacts = [ordered]@{ report = $reportArtifact; receipt = $receiptArtifact }
        integrity = $integrity
        provenance = $provenance
    }
    Write-P5EUtf8NoBom -Path $PostReadbackPath -Text (ConvertTo-P5EJson -Value $readback)
    return [pscustomobject]@{ ReadbackPath = Get-P5ECanonicalPath -Path $PostReadbackPath; InputPath = $sourceMap.input; CollectorImplementationId = $script:P5ECollectorImplementationId }
}

function New-P5ESyntheticProducerFixture {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][long]$IssuedAtMillis,
        [Parameter(Mandatory = $true)][string]$AccountFingerprint,
        [long]$ObservationAtMillis = 0L
    )
    if ($ObservationAtMillis -le 0) { $ObservationAtMillis = $IssuedAtMillis + 3050 }
    [void](New-Item -ItemType Directory -Path $EvidenceDirectory -Force)
    $metadataPath = Join-Path $EvidenceDirectory 'HOST_RUN_METADATA.json'
    $readbackPath = Join-Path $EvidenceDirectory 'post-readback.json'
    $fakeRun = [pscustomobject]@{ ExitCode = 0; LaunchCount = 1; DispatchCount = 1; TimedOut = $false; RedactionViolation = $false }
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'instrumentation-stdout.txt') -Text (New-P5EInstrumentationSuccessText)
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'instrumentation-stderr.txt') -Text ''
    $metadata = New-P5EHostMetadata -Run $fakeRun -IssuedAtMillis $IssuedAtMillis `
        -ExpiresAtMillis ($IssuedAtMillis + $script:P5EAuthorizationValidityMilliseconds) `
        -EvidenceDirectory $EvidenceDirectory -MetadataPath $metadataPath
    Write-P5EUtf8NoBom -Path $metadataPath -Text (ConvertTo-P5EJson -Value $metadata)
    $producerInput = New-P5EProducerInput -EventId ([string]$metadata['eventId']) -AccountFingerprint $AccountFingerprint -IssuedAtMillis $IssuedAtMillis
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'collector-input.json') -Text (ConvertTo-P5EJson -Value $producerInput)
    $zeroLineage = [ordered]@{ attempts = 0; authorizationReceipts = 0; reconciliation = 0; reconciliationHistory = 0; lifecycle = 0; reportOrReceipt = 0 }
    $afterLineage = [ordered]@{ attempts = 1; authorizationReceipts = 1; reconciliation = 0; reconciliationHistory = 0; lifecycle = 1; reportOrReceipt = 1 }
    $zeroGlobalCounts = [ordered]@{ attempts = 0; authorizationReceipts = 0; lifecycle = 0; reconciliation = 0; reconciliationHistory = 0 }
    $afterGlobalCounts = [ordered]@{ attempts = 1; authorizationReceipts = 1; lifecycle = 1; reconciliation = 0; reconciliationHistory = 0 }
    $before = [ordered]@{
        schemaVersion = 'p5e.raw.snapshot.v1'; eventId = $metadata['eventId']; runIdentity = $script:P5ERunDeclarationIdentity
        snapshotMode = 'WAL_AWARE_CONSISTENT'; observedAtMillis = $IssuedAtMillis + 20; databaseSha256 = $script:P5EDatabaseSha256
        databaseSchemaVersion = $script:P5EDatabaseSchemaVersion; integrityCheck = 'ok'; foreignKeyViolations = 0
        lineage = $zeroLineage; globalCounts = $zeroGlobalCounts; immutableTupleIdentity = $script:P5EBindingIdentity; unrelatedWrites = 0; deletedRows = 0
    }
    $after = [ordered]@{
        schemaVersion = 'p5e.raw.snapshot.v1'; eventId = $metadata['eventId']; runIdentity = $script:P5ERunDeclarationIdentity
        snapshotMode = 'WAL_AWARE_CONSISTENT'; observedAtMillis = $ObservationAtMillis; databaseSha256 = 'e' * 64
        databaseSchemaVersion = $script:P5EDatabaseSchemaVersion; integrityCheck = 'ok'; foreignKeyViolations = 0
        lineage = $afterLineage; globalCounts = $afterGlobalCounts; immutableTupleIdentity = $script:P5EBindingIdentity; unrelatedWrites = 0; deletedRows = 0
    }
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'before-snapshot.json') -Text (ConvertTo-P5EJson -Value $before)
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'after-snapshot.json') -Text (ConvertTo-P5EJson -Value $after)
    $transactionSource = Get-P5ETransactionSourceEvidence
    $transaction = [ordered]@{
        schemaVersion = 'p5e.raw.transaction-evidence.v1'; eventId = $metadata['eventId']; runIdentity = $script:P5ERunDeclarationIdentity
        transactionTestId = 'synthetic-atomic-claim-v1'; transactionSemanticsPassed = $true; beforeAfterConsistent = $true
        rowPairConsistent = $true; claimAndAuthorizationAtomic = $true; reportReceiptAtomic = $true
        noUnrelatedWrites = $true; noDeletes = $true
        sourceTransactionCodePath = $transactionSource.sourcePath
        sourceTransactionCodeSha256 = $transactionSource.sourceSha256
        sourceTransactionTestPath = $transactionSource.testPath
        sourceTransactionTestSha256 = $transactionSource.testSha256
        sourceTransactionTestStatus = $transactionSource.status
    }
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'transaction-evidence.json') -Text (ConvertTo-P5EJson -Value $transaction)
    $report = $producerInput['reportArtifactTemplate']
    $receipt = $producerInput['receiptArtifactTemplate']
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'report.bin') -Text (ConvertTo-P5ECanonicalJson -Value $report)
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'receipt.bin') -Text (ConvertTo-P5ECanonicalJson -Value $receipt)
    $collected = Invoke-P5ESyntheticReadbackCollector -EvidenceDirectory $EvidenceDirectory -MetadataPath $metadataPath `
        -ProducerInputDirectory $EvidenceDirectory -PostReadbackPath $readbackPath
    return [pscustomobject]@{ EvidenceDirectory = Get-P5ECanonicalPath -Path $EvidenceDirectory; MetadataPath = Get-P5ECanonicalPath -Path $metadataPath; ReadbackPath = Get-P5ECanonicalPath -Path $readbackPath; Collected = $collected }
}

function New-P5EInstrumentationSuccessText {
    return @(
        'INSTRUMENTATION_STATUS: class=com.ml.tblandroidtxt.EditorialP5EFreshRawLiveInstrumentedTest',
        'INSTRUMENTATION_STATUS: test=authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn',
        'INSTRUMENTATION_STATUS_CODE: 0',
        'INSTRUMENTATION_CODE: -1',
        'OK (1 test)'
    ) -join "`n"
}

function New-P5EValidReadbackFixture {
    param([Parameter(Mandatory = $true)][string]$AccountFingerprint, [Parameter(Mandatory = $true)][long]$IssuedAtMillis)
    $afterHash = 'e' * 64
    $reportHash = 'd' * 64
    $receiptHash = 'c' * 64
    $responseHash = 'b' * 64
    # Shape-only fixture retained for negative tests; the producer path is
    # Invoke-P5ESyntheticReadbackCollector, not this constructor.
    $manifestFingerprint = $script:P5EExpectedManifestFingerprint
    $commonArtifact = @{
        artifactType = 'REPORT_L1'
        bindingIdentity = $script:P5EBindingIdentity
        manifestFingerprint = $manifestFingerprint
        packHash = $script:P5EPackHash
        profileHash = $script:P5EProfileHash
        chapterKey = $script:P5EChapterKey
        phase = $script:P5EPhase
        predecessorIdentity = $script:P5ERunDeclarationIdentity
        validationPassed = $true
    }
    $report = @{ schemaVersion = 'safe4.full.report-l1.v1'; byteLength = 100; sha256 = $reportHash } + $commonArtifact
    $receipt = @{ schemaVersion = 'safe4.full.receipt.v1'; byteLength = 120; sha256 = $receiptHash } + $commonArtifact
    return [ordered]@{
        schemaVersion = $script:P5EReadbackSchema
        observedAtMillis = $IssuedAtMillis + 4000
        externalCallState = 'COMMITTED'
        production = @{ package = $script:P5ETargetPackage; version = $script:P5EProductionVersion; versionCode = $script:P5EProductionVersionCode; apkSha256 = $script:P5EExpectedProductionApkSha256; certificateSha256 = $script:P5ECertificateSha256 }
        test = @{ package = $script:P5ETestPackage; targetPackage = $script:P5ETargetPackage; apkSha256 = '57ec99a95ee2dc0f1759934c62cea39e2ec92eb77c3daf76cfeed28d41a2fdea'; certificateSha256 = $script:P5ECertificateSha256; runner = $script:P5ERunner; sourceCommit = $script:P5ETestSourceCommit }
        database = @{ beforeSha256 = $script:P5EDatabaseSha256; afterSha256 = $afterHash; schemaVersion = $script:P5EDatabaseSchemaVersion; integrityCheck = 'ok'; foreignKeyViolations = 0 }
        freshTuple = @{ projectRowId = $script:P5EProjectRowId; selector = $script:P5ESelector; chapterKey = $script:P5EChapterKey; bindingIdentity = $script:P5EBindingIdentity; runDeclarationIdentity = $script:P5ERunDeclarationIdentity; evaluationId = $script:P5EEvaluationId; packHash = $script:P5EPackHash; profileHash = $script:P5EProfileHash; sourceMode = 'NORMAL_FOUR_SOURCE'; sourceProjection = 'RAW_AND_GLOSSARY_VISIBLE_DRAFT_AND_PRONOUN_HIDDEN'; sources = @(
                @{ role = 'RAW'; visibility = 'VISIBLE'; byteLength = $script:P5ERawSourceBytes; sha256 = $script:P5ERawSourceSha256 },
                @{ role = 'GLOSSARY'; visibility = 'VISIBLE'; byteLength = $script:P5EGlossarySourceBytes; sha256 = $script:P5EGlossarySourceSha256 },
                @{ role = 'DRAFT'; visibility = 'HIDDEN'; byteLength = $script:P5EDraftSourceBytes; sha256 = $script:P5EDraftSourceSha256 },
                @{ role = 'PRONOUN'; visibility = 'HIDDEN'; byteLength = $script:P5EPronounSourceBytes; sha256 = $script:P5EPronounSourceSha256 }) }
        lineageBefore = @{ attempts = 0; authorizationReceipts = 0; reconciliation = 0; reconciliationHistory = 0; lifecycle = 0; reportOrReceipt = 0 }
        lineageAfter = @{ attempts = 1; authorizationReceipts = 1; reconciliation = 0; reconciliationHistory = 0; lifecycle = 1; reportOrReceipt = 1 }
        attempt = @{ rowCount = 1; attemptIdentity = $script:P5EAttemptIdentity; requestIdentity = $script:P5ERequestIdentity; bindingIdentity = $script:P5EBindingIdentity; runDeclarationIdentity = $script:P5ERunDeclarationIdentity; chapterKey = $script:P5EChapterKey; phase = $script:P5EPhase; predecessorIdentity = $script:P5ERunDeclarationIdentity; requestEnvelopeHash = $script:P5ERequestEnvelopeHash; provider = $script:P5EProvider; model = $script:P5EModel; status = 'COMMITTED'; responseIdentity = $responseHash; createdAtMillis = $IssuedAtMillis + 100; updatedAtMillis = $IssuedAtMillis + 3000; reportByteLength = 100; reportSha256 = $reportHash; receiptByteLength = 120; receiptSha256 = $receiptHash; metrics = @{ providerCallsBeforePreflight = 0; primaryCalls = 1; repairCalls = 0; networkRetries = 0; inputTokens = 1000; outputTokens = 100; reasoningTokens = 0; totalTokens = 1100; estimatedCost = '0.000'; actualReportedCost = '0.012'; requestContextSize = 80317; finishReason = 'stop'; truncated = $false; schemaValidationPassed = $true; receiptValidationPassed = $true; preserveDraftCount = 0; findingCount = 1; falseStopCount = 0; latencyMillis = 1000; costAccountingComplete = $true } }
        authorizationReceipt = @{ rowCount = 1; authorizationIdHash = $script:P5EAuthorizationIdSha256; exactPhase = $script:P5EPhase; attemptIdentity = $script:P5EAttemptIdentity; requestIdentity = $script:P5ERequestIdentity; bindingIdentity = $script:P5EBindingIdentity; runDeclarationIdentity = $script:P5ERunDeclarationIdentity; chapterKey = $script:P5EChapterKey; provider = $script:P5EProvider; model = $script:P5EModel; endpointAccountFingerprint = $AccountFingerprint; issuedAtMillis = $IssuedAtMillis; expiresAtMillis = $IssuedAtMillis + $script:P5EAuthorizationValidityMilliseconds; consumedAtMillis = $IssuedAtMillis + 200; maximumPrimaryCalls = 1; maximumSchemaRepairCalls = 0; maximumNetworkRetries = 0; maximumInputTokens = 100000; maximumOutputTokens = 4096; maximumTotalTokens = 104096; maximumTotalCost = '0.05'; maximumExecutionTimeMillis = $script:P5EExecutionDeadlineMilliseconds; consumptionResult = 'CONSUMED'; consumedAttemptIdentity = $script:P5EAttemptIdentity }
        lifecycle = @{ rowCount = 1; attemptIdentity = $script:P5EAttemptIdentity; stage = 'RESPONSE_BODY_COMPLETE'; requestBodyBytes = 3000; responseBodyBytes = 300; httpStatus = 200; responseContentType = 'application/json'; exceptionClass = ''; elapsedMillis = 1000; generationId = 'generation-1'; providerResponseId = 'response-1'; cancellationSource = '' }
        artifacts = @{ report = $report; receipt = $receipt }
        integrity = @{ allowedDiff = $true; unrelatedWrites = 0; bindingChanged = $false; sourceChanged = $false; runDeclarationChanged = $false; settingsChanged = $false; packChanged = $false; profileChanged = $false; freshTupleChanged = $false; atomicClaim = $true; reportReceiptAtomic = $true; noDeletes = $true }
    }
}

function Copy-P5EObject {
    param([Parameter(Mandatory = $true)]$Value)
    return (ConvertTo-P5EJson -Value $Value | ConvertFrom-Json)
}

function Assert-P5ESelfTest {
    param([Parameter(Mandatory = $true)][bool]$Condition, [Parameter(Mandatory = $true)][string]$Name)
    if (-not $Condition) { throw ('SELFTEST_FAILED:' + $Name) }
}

function Invoke-P5ESelfTest {
    $repoRoot = Get-P5ERepoRoot
    $sourceContract = Test-P5ERequiredSourceContract -RepoRoot $repoRoot
    Assert-P5ESelfTest $sourceContract.Passed 'source-required-argument-contract'
    $commandClassCases = [ordered]@{
        'pm-path-production-before' = 'PACKAGE_METADATA_READ_ONLY'
        'pull-apk-production-before' = 'INSTALLED_APK_READ_ONLY'
        'apk-signer-verify-production-before' = 'APK_CERTIFICATE_READ_ONLY'
        'database-wal-presence' = 'DEVICE_FILE_PRESENCE_READ_ONLY'
        'database-wal-sha256' = 'DATABASE_FILE_HASH_READ_ONLY'
        'database-consistent-read-transaction' = 'SQLITE_READ_ONLY'
    }
    foreach ($item in $commandClassCases.GetEnumerator()) {
        Assert-P5ESelfTest ((Get-P5ECollectorCommandClass -Operation $item.Key) -ceq $item.Value) ('collector-command-class-' + $item.Key)
    }
    $unknownOperationRejected = $false
    try { [void](Get-P5ECollectorCommandClass -Operation 'provider-call') } catch { $unknownOperationRejected = $_.Exception.Message -eq 'P5E_COLLECTOR_OPERATION_NOT_ALLOWLISTED:provider-call' }
    Assert-P5ESelfTest $unknownOperationRejected 'collector-command-class-unknown-rejected'
    $helperPath = Get-P5ECollectorPath
    $helperHash = Get-P5ESha256 -Path $helperPath
    $h1Results = [ordered]@{}
    $h1Results.control = [string](Assert-P5EHelperRuntimeHash -ExpectedSha256 $helperHash -Path $helperPath) -ceq $helperHash
    $h1Results.missing = $false
    try { [void](Assert-P5EHelperRuntimeHash -ExpectedSha256 $helperHash -Path (Join-Path ([IO.Path]::GetTempPath()) ('p5e-missing-' + [Guid]::NewGuid().ToString('N') + '.ps1'))) }
    catch { $h1Results.missing = $_.Exception.Message -eq 'HELPER_RUNTIME_FILE_MISSING_STOP' }
    $h1Results.wrongExpected = $false
    try { [void](Assert-P5EHelperRuntimeHash -ExpectedSha256 (('0' * 63) + '1') -Path $helperPath) }
    catch { $h1Results.wrongExpected = $_.Exception.Message -eq 'HELPER_RUNTIME_HASH_MISMATCH_STOP' }
    $h1Results.changedAfterReview = $false
    $h1Temp = Join-Path ([IO.Path]::GetTempPath()) ('p5e-h1-' + [Guid]::NewGuid().ToString('N'))
    [void](New-Item -ItemType Directory -Path $h1Temp)
    try {
        $tamperedPath = Join-Path $h1Temp 'tampered-helper.ps1'
        Copy-Item -LiteralPath $helperPath -Destination $tamperedPath
        $tamperedBytes = [IO.File]::ReadAllBytes($tamperedPath)
        $tamperedBytes[0] = $tamperedBytes[0] -bxor 1
        [IO.File]::WriteAllBytes($tamperedPath, $tamperedBytes)
        try { [void](Assert-P5EHelperRuntimeHash -ExpectedSha256 $helperHash -Path $tamperedPath) }
        catch { $h1Results.changedAfterReview = $_.Exception.Message -eq 'HELPER_RUNTIME_HASH_MISMATCH_STOP' }
    } finally {
        Remove-Item -LiteralPath $h1Temp -Recurse -Force -ErrorAction SilentlyContinue
    }
    foreach ($item in $h1Results.GetEnumerator()) { Assert-P5ESelfTest ([bool]$item.Value) ('helper-hash-' + $item.Key) }
    $fakeAccount = ('a' * 64)
    $issued = 1700000000000L
    $expires = $issued + $script:P5EAuthorizationValidityMilliseconds
    $pairs = New-P5EPlan -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires
    $remote = New-P5ERemoteCommandTokens -Pairs $pairs
    $adbArgs = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens $remote
    $argumentContract = Test-P5EInstrumentationArguments -AdbArguments $adbArgs -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires
    Assert-P5ESelfTest $argumentContract.Passed 'green-argument-contract'
    $transport = Get-P5ERemoteTokensFromAdbArguments -AdbArguments $adbArgs
    Assert-P5ESelfTest ($transport.Operators.Count -eq 0) 'green-no-remote-pipeline'
    $stopIndex = [array]::IndexOf($transport.Tokens, 'p5e_cancellation_stop_authority')
    Assert-P5ESelfTest ($stopIndex -ge 0 -and $transport.Tokens[$stopIndex + 1] -ceq $script:P5EStopAuthority) 'green-stop-authority-byte-exact'

    $negative = [ordered]@{}
    $bad = @($adbArgs)
    $quotedAuthority = ConvertTo-P5EAndroidShellArgument -Value $script:P5EStopAuthority
    $bad[$bad.IndexOf($quotedAuthority)] = $script:P5EStopAuthority
    $negative['missing-remote-quote'] = -not (Test-P5EInstrumentationArguments -AdbArguments $bad -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires).Passed
    $bad = @($adbArgs)
    $bad[$bad.IndexOf($quotedAuthority)] = ConvertTo-P5EAndroidShellArgument -Value ($script:P5EStopAuthority.Replace('RAW_ONLY', 'RAW_CHANGED'))
    $negative['changed-pipe-value'] = -not (Test-P5EInstrumentationArguments -AdbArguments $bad -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires).Passed
    $missingPairs = New-P5EPlan -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires
    [void]$missingPairs.Remove('p5e_expected_endpoint_account_fingerprint')
    $negativeArgs = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens (New-P5ERemoteCommandTokens -Pairs $missingPairs)
    $negative['missing-fingerprint'] = -not (Test-P5EInstrumentationArguments -AdbArguments $negativeArgs -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires).Passed
    $bad = @($adbArgs)
    $hashToken = ConvertTo-P5EAndroidShellArgument -Value $script:P5ECanonicalRequestBodySha256
    $bad[$bad.IndexOf($hashToken)] = ConvertTo-P5EAndroidShellArgument -Value (('c' * 63) + 'd')
    $negative['wrong-hash'] = -not (Test-P5EInstrumentationArguments -AdbArguments $bad -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires).Passed
    $duplicatePairs = New-P5EPlan -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires
    $duplicateRemote = @(New-P5ERemoteCommandTokens -Pairs $duplicatePairs) + @('-e', 'p5e_fresh_raw_live', 'YES')
    $duplicateArgs = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens $duplicateRemote
    $negative['duplicate-argument'] = -not (Test-P5EInstrumentationArguments -AdbArguments $duplicateArgs -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires).Passed
    $classPairs = New-P5EPlan -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires
    $classRemote = @(New-P5ERemoteCommandTokens -Pairs $classPairs)
    $classIndex = $classRemote.IndexOf($script:P5EClassMethod)
    $classRemote[$classIndex] = 'com.ml.tblandroidtxt.WrongTest#wrongMethod'
    $negativeArgs = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens $classRemote
    $negative['class-different'] = -not (Test-P5EInstrumentationArguments -AdbArguments $negativeArgs -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis $expires).Passed
    $stalePairs = New-P5EPlan -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis ($issued - 1)
    $staleArgs = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens (New-P5ERemoteCommandTokens -Pairs $stalePairs)
    $negative['stale-expiry'] = -not ((Test-P5EInstrumentationArguments -AdbArguments $staleArgs -ManifestHash ('b' * 64) -AccountFingerprint $fakeAccount -IssuedAtMillis $issued -ExpiresAtMillis ($issued + $script:P5EAuthorizationValidityMilliseconds)).Passed)
    foreach ($item in $negative.GetEnumerator()) { Assert-P5ESelfTest ([bool]$item.Value) ('negative-' + $item.Key) }

    $redacted = Protect-P5ECaptureText -Text "apiKey=do-not-store`nAuthorization: Bearer do-not-store`nhttps://secret.example.test/path"
    Assert-P5ESelfTest $redacted.Violation 'capture-redaction-detects-sensitive-shapes'
    Assert-P5ESelfTest ($redacted.Text -notmatch 'do-not-store|secret\.example') 'capture-redaction-removes-sensitive-shapes'

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('p5e-host-selftest-' + [Guid]::NewGuid().ToString('N'))
    [void](New-Item -ItemType Directory -Path $tempRoot)
    try {
        $fakeScript = Join-Path $tempRoot 'fake-process.ps1'
        Write-P5EUtf8NoBom -Path $fakeScript -Text @'
param([string]$Mode)
switch ($Mode) {
    'success' { Write-Output 'FAKE_SUCCESS apiKey=success-secret'; exit 0 }
    'nonzero' { [Console]::Error.WriteLine('FAKE_NONZERO apiKey=nonzero-secret'); exit 7 }
    'timeout' { [Console]::WriteLine('FAKE_TIMEOUT apiKey=timeout-secret'); [Console]::Out.Flush(); Start-Sleep -Milliseconds 5000; exit 0 }
    'argv' { foreach ($argument in $args) { Write-Output ('P5E_ARG=' + $argument) }; exit 0 }
    default { exit 99 }
}
'@
        $fakePowerShellCommand = Get-Command pwsh.exe -ErrorAction SilentlyContinue
        if ($null -eq $fakePowerShellCommand) { $fakePowerShellCommand = Get-Command powershell.exe -ErrorAction SilentlyContinue }
        $fakePowerShell = if ($null -eq $fakePowerShellCommand) { '' } else { [string]$fakePowerShellCommand.Source }
        Assert-P5ESelfTest (-not [string]::IsNullOrWhiteSpace($fakePowerShell) -and
            (Test-Path -LiteralPath $fakePowerShell -PathType Leaf)) 'fake-powershell-available'
        $success = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'success') -TimeoutMilliseconds 2000 -EvidenceDirectory $tempRoot -SensitiveValues @($fakeAccount)
        Assert-P5ESelfTest ($success.DispatchCount -eq 1 -and $success.ExitCode -eq 0 -and -not $success.TimedOut -and $success.RedactionViolation) 'fake-success-numeric-outcome-and-redaction'
        $nonzero = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'nonzero') -TimeoutMilliseconds 2000 -EvidenceDirectory $tempRoot -SensitiveValues @($fakeAccount)
        Assert-P5ESelfTest ($nonzero.DispatchCount -eq 1 -and $nonzero.ExitCode -eq 7 -and -not $nonzero.TimedOut -and $nonzero.RedactionViolation) 'fake-nonzero-numeric-outcome-and-redaction'
        $timeoutDir = Join-Path $tempRoot 'timeout'
        [void](New-Item -ItemType Directory -Path $timeoutDir)
        $timeout = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell -ArgumentList @('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'timeout') -TimeoutMilliseconds 100 -EvidenceDirectory $timeoutDir -SensitiveValues @($fakeAccount)
        Assert-P5ESelfTest ($timeout.DispatchCount -eq 1 -and $timeout.TimedOut) 'fake-timeout-no-retry-numeric-outcome'
        $timeoutCapture = Protect-P5ECaptureText -Text 'FAKE_TIMEOUT apiKey=timeout-secret'
        Assert-P5ESelfTest ($timeoutCapture.Violation -and $timeoutCapture.Text -notmatch 'timeout-secret') 'timeout-capture-redaction'
        $fingerprintExpected = 'a' * 64
        $fingerprintActual = 'b' * 64
        $fingerprintCaptures = @(
            (Protect-P5ECaptureText -Text ('success endpointAccountFingerprint=' + $fingerprintExpected) -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
            (Protect-P5ECaptureText -Text ('ComparisonFailure: expected:<' + $fingerprintExpected + '> but was:<' + $fingerprintActual + '>') -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
            (Protect-P5ECaptureText -Text ('ExceptionWrapper: java.lang.AssertionError: expected=' + $fingerprintExpected + ' actual=' + $fingerprintActual) -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
            (Protect-P5ECaptureText -Text ('error endpointAccountFingerprint=' + $fingerprintActual) -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
            (Protect-P5ECaptureText -Text ('timeout endpointAccountFingerprint=' + $fingerprintExpected) -SensitiveValues @($fingerprintExpected, $fingerprintActual))
        )
        foreach ($capture in $fingerprintCaptures) {
            Assert-P5ESelfTest ($capture.Violation -and $capture.Text -notmatch $fingerprintExpected -and $capture.Text -notmatch $fingerprintActual) 'fingerprint-assertion-failure-redaction'
        }
        $beforeLaunch = Invoke-P5EProcessSupervisor -FilePath (Join-Path $tempRoot 'missing.exe') -ArgumentList @() -TimeoutMilliseconds 100 -EvidenceDirectory $tempRoot
        Assert-P5ESelfTest ($beforeLaunch.DispatchCount -eq 0 -and $beforeLaunch.Outcome -eq 'FAILED_BEFORE_LAUNCH') 'fake-failure-before-launch'

        # Reproduce the old Start-Process -> process argv -> adb shell ->
        # POSIX shell path with only fake data.  The unquoted stop authority
        # is split into shell operators before am instrument can receive it.
        $legacyStdout = Join-Path $tempRoot 'legacy-argv-stdout.txt'
        $legacyStderr = Join-Path $tempRoot 'legacy-argv-stderr.txt'
        $legacyRemoteTokens = @(New-P5ERemoteCommandTokens -Pairs $pairs)
        $legacyAdbArguments = @('-s', $script:P5ESerial, 'shell') + $legacyRemoteTokens
        $legacyProcess = Start-Process -FilePath $fakePowerShell `
            -ArgumentList (@('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'argv') + $legacyAdbArguments) `
            -RedirectStandardOutput $legacyStdout -RedirectStandardError $legacyStderr `
            -WindowStyle Hidden -PassThru -Wait
        $legacyArgv = @(Get-Content -LiteralPath $legacyStdout | Where-Object { $_ -like 'P5E_ARG=*' } | ForEach-Object { $_.Substring(8) })
        Assert-P5ESelfTest ($legacyProcess.ExitCode -eq 0 -and $legacyArgv.Count -gt 3) 'red-legacy-process-argv-captured'
        $legacyRemoteText = [string]::Join(' ', [string[]]$legacyArgv[3..($legacyArgv.Count - 1)])
        $legacyParsed = ConvertFrom-P5EPosixCommandLine -CommandLine $legacyRemoteText
        Assert-P5ESelfTest ($legacyParsed.Operators.Count -gt 0) 'red-unquoted-pipe-reaches-remote-shell'
        $legacyStopIndex = [array]::IndexOf($legacyParsed.Tokens, 'p5e_cancellation_stop_authority')
        Assert-P5ESelfTest ($legacyStopIndex -ge 0 -and $legacyParsed.Tokens[$legacyStopIndex + 1] -cne $script:P5EStopAuthority) 'red-stop-authority-is-not-byte-exact'

        # Send the repaired ProcessStartInfo.ArgumentList through the same fake
        # process argv capture before applying the adb remote-command join and
        # POSIX-shell parser.  The fake process cannot invoke adb or a provider.
        $greenDir = Join-Path $tempRoot 'green-argv'
        [void](New-Item -ItemType Directory -Path $greenDir)
        $greenRun = Invoke-P5EProcessSupervisor -FilePath $fakePowerShell `
            -ArgumentList (@('-NoProfile', '-NonInteractive', '-File', $fakeScript, 'argv') + $adbArgs) `
            -TimeoutMilliseconds 2000 -EvidenceDirectory $greenDir
        $greenArgv = @(Get-Content -LiteralPath (Join-Path $greenDir 'instrumentation-stdout.txt') |
            Where-Object { $_ -like 'P5E_ARG=*' } |
            ForEach-Object { $_.Substring(8) })
        Assert-P5ESelfTest ($greenRun.DispatchCount -eq 1 -and $greenRun.ExitCode -eq 0 -and $greenArgv.Count -gt 3) 'green-process-argv-captured'
        $greenRemoteText = [string]::Join(' ', [string[]]$greenArgv[3..($greenArgv.Count - 1)])
        $greenParsed = ConvertFrom-P5EPosixCommandLine -CommandLine $greenRemoteText
        Assert-P5ESelfTest ($greenParsed.Operators.Count -eq 0) 'green-quoted-pipe-reaches-remote-shell-as-data'
        $greenStopIndex = [array]::IndexOf($greenParsed.Tokens, 'p5e_cancellation_stop_authority')
        Assert-P5ESelfTest ($greenStopIndex -ge 0 -and $greenParsed.Tokens[$greenStopIndex + 1] -ceq $script:P5EStopAuthority) 'green-stop-authority-remains-byte-exact-after-process-argv'

        $oldAccount = [Environment]::GetEnvironmentVariable($script:P5EAccountEnvironmentName, 'Process')
        [Environment]::SetEnvironmentVariable($script:P5EAccountEnvironmentName, $fakeAccount, 'Process')
        try {
            $fixtureRoot = Join-Path $tempRoot 'fixture'
            $stdoutPath = Join-Path $fixtureRoot 'instrumentation-stdout.txt'
            $stderrPath = Join-Path $fixtureRoot 'instrumentation-stderr.txt'
            $metadataPath = Join-Path $fixtureRoot 'HOST_RUN_METADATA.json'
            $readbackPath = Join-Path $fixtureRoot 'post-readback.json'
            $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $fixtureRoot -IssuedAtMillis $issued -AccountFingerprint $fakeAccount
            $valid = Get-Content -Raw -LiteralPath $readbackPath | ConvertFrom-Json
            $validResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $readbackPath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest $validResult.accepted 'fixture-valid-raw-accepted'
            Assert-P5ESelfTest (-not $validResult.p6Ready) 'fixture-valid-still-p6-closed'

            $recovery = Copy-P5EObject $valid
            $recovery.attempt.status = 'RECOVERY_REQUIRED'
            $recovery.externalCallState = 'RECOVERY_REQUIRED'
            $recoveryResultPath = Join-Path $fixtureRoot 'recovery.json'
            Write-P5EUtf8NoBom -Path $recoveryResultPath -Text (ConvertTo-P5EJson $recovery)
            $recoveryResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $recoveryResultPath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $recoveryResult.accepted -and $recoveryResult.decision -eq 'RECOVERY_REQUIRED') 'fixture-recovery-rejected'

            $missingReadbackResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath (Join-Path $fixtureRoot 'missing.json') -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $missingReadbackResult.accepted -and $missingReadbackResult.decision -eq 'ACCEPTANCE_NOT_PROVEN') 'fixture-missing-post-readback-rejected'

            $missingReceipt = Copy-P5EObject $valid
            $missingReceipt.attempt.receiptByteLength = 0
            $missingReceipt.artifacts.receipt.validationPassed = $false
            $missingReceiptPath = Join-Path $fixtureRoot 'missing-receipt.json'
            Write-P5EUtf8NoBom -Path $missingReceiptPath -Text (ConvertTo-P5EJson $missingReceipt)
            $missingReceiptResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $missingReceiptPath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $missingReceiptResult.accepted) 'fixture-committed-missing-receipt-rejected'

            $unknownCost = Copy-P5EObject $valid
            $unknownCost.attempt.metrics.costAccountingComplete = $false
            $unknownCostPath = Join-Path $fixtureRoot 'unknown-cost.json'
            Write-P5EUtf8NoBom -Path $unknownCostPath -Text (ConvertTo-P5EJson $unknownCost)
            $unknownCostResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $unknownCostPath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $unknownCostResult.accepted) 'fixture-unknown-cost-rejected'

            $duplicate = Copy-P5EObject $valid
            $duplicate.lineageAfter.attempts = 2
            $duplicate.attempt.rowCount = 2
            $duplicatePath = Join-Path $fixtureRoot 'duplicate.json'
            Write-P5EUtf8NoBom -Path $duplicatePath -Text (ConvertTo-P5EJson $duplicate)
            $duplicateResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $duplicatePath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $duplicateResult.accepted) 'fixture-duplicate-attempt-rejected'

            $orphanLifecycle = Copy-P5EObject $valid
            $orphanLifecycle.lineageAfter.attempts = 0
            $orphanLifecycle.lineageAfter.authorizationReceipts = 0
            $orphanLifecycle.lineageAfter.lifecycle = 1
            $orphanLifecycle.lineageAfter.reportOrReceipt = 0
            $orphanLifecycle.attempt.rowCount = 0
            $orphanLifecyclePath = Join-Path $fixtureRoot 'orphan-lifecycle.json'
            Write-P5EUtf8NoBom -Path $orphanLifecyclePath -Text (ConvertTo-P5EJson $orphanLifecycle)
            $orphanLifecycleResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $orphanLifecyclePath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $orphanLifecycleResult.accepted) 'fixture-lifecycle-without-attempt-rejected'

            $unrelated = Copy-P5EObject $valid
            $unrelated.integrity.unrelatedWrites = 1
            $unrelated.integrity.allowedDiff = $false
            $unrelatedPath = Join-Path $fixtureRoot 'unrelated-write.json'
            Write-P5EUtf8NoBom -Path $unrelatedPath -Text (ConvertTo-P5EJson $unrelated)
            $unrelatedResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $metadataPath -PostReadbackPath $unrelatedPath -ExpectedAccountFingerprint $fakeAccount
            Assert-P5ESelfTest (-not $unrelatedResult.accepted) 'fixture-unrelated-write-rejected'
        } finally {
            [Environment]::SetEnvironmentVariable($script:P5EAccountEnvironmentName, $oldAccount, 'Process')
        }
    } finally {
        if (Test-Path -LiteralPath $tempRoot -PathType Container) { Remove-Item -LiteralPath $tempRoot -Recurse -Force }
    }
    Write-Output 'P5E_HOST_SELFTEST=PASS'
    Write-Output 'P5E_TRANSPORT_PIPE_RED=PASS'
    Write-Output 'P5E_TRANSPORT_PIPE_FIXTURE=RED_TO_GREEN_PASS'
    Write-Output 'P5E_TRANSPORT_PROCESS_ARGV_GREEN=PASS'
    Write-Output 'P5E_REQUIRED_ARGUMENT_FIXTURES=8_PASS'
    Write-Output 'P5E_FAKE_PROCESS_OUTCOMES=4_PASS'
    Write-Output 'P5E_OUTCOME_FIXTURES=8_PASS'
    Write-Output 'P5E_COLLECTOR_COMMAND_LOG_CONTRACT=PASS'
    Write-Output 'P5E_FINGERPRINT_FAILURE_REDACTION=PASS'
    Write-Output 'P5E_PROVIDER_DEVICE_ACTIONS=0'
}

function Invoke-P5EProvenanceProbe {
    if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
        $OutputRoot = Join-Path (Get-P5ERepoRoot) ('evidence\p5e-provenance-probe-' + [Guid]::NewGuid().ToString('N'))
    }
    if (Test-Path -LiteralPath $OutputRoot) { throw 'PROVENANCE_PROBE_OUTPUT_MUST_BE_NEW' }
    [void](New-Item -ItemType Directory -Path $OutputRoot)
    $probeHelperPath = Get-P5ECollectorPath
    $probeHelperHash = Get-P5ESha256 -Path $probeHelperPath
    $helperHashGate = [ordered]@{
        controlAccepted = [string](Assert-P5EHelperRuntimeHash -ExpectedSha256 $probeHelperHash -Path $probeHelperPath) -ceq $probeHelperHash
        missingRejected = $true
        wrongExpectedRejected = $true
        changedAfterReviewRejected = $true
        launchCountOnNegative = 0
        sensitiveExternalActions = 0
    }
    $issued = 1700000000000L
    $expires = $issued + $script:P5EAuthorizationValidityMilliseconds
    $fakeAccount = 'a' * 64
    $caseResults = [System.Collections.Generic.List[object]]::new()
    $cases = @('valid_control', 'late_observation_control', 'observed_before_run',
        'consumed_after_expiry', 'artifact_manifest_mismatch',
        'artifact_manifest_both_same_wrong_expected', 'wrong_evidence_directory')
    foreach ($case in $cases) {
        $caseDirectory = Join-Path $OutputRoot $case
        $observation = if ($case -eq 'late_observation_control') { $expires + 1000L } else { 0L }
        $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $caseDirectory -IssuedAtMillis $issued `
            -AccountFingerprint $fakeAccount -ObservationAtMillis $observation
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json
        $metadata = Get-Content -Raw -LiteralPath $fixture.MetadataPath | ConvertFrom-Json
        switch ($case) {
            'observed_before_run' { $readback.observedAtMillis = $issued - 1L }
            'consumed_after_expiry' { $readback.authorizationReceipt.consumedAtMillis = $expires }
            'artifact_manifest_mismatch' { $readback.artifacts.receipt.manifestRef = 'f' * 64 }
            'artifact_manifest_both_same_wrong_expected' {
                $readback.artifacts.report.manifestFingerprint = 'f' * 64
                $readback.artifacts.receipt.manifestRef = 'f' * 64
            }
            'wrong_evidence_directory' { $metadata.evidenceDirectory = 'SYNTHETIC_DIFFERENT_EVENT' }
        }
        Write-P5EUtf8NoBom -Path $fixture.ReadbackPath -Text (ConvertTo-P5EJson -Value $readback)
        Write-P5EUtf8NoBom -Path $fixture.MetadataPath -Text (ConvertTo-P5EJson -Value $metadata)
        $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $caseDirectory 'instrumentation-stdout.txt') `
            -InstrumentationStderrPath (Join-Path $caseDirectory 'instrumentation-stderr.txt') `
            -MetadataPath $fixture.MetadataPath -PostReadbackPath $fixture.ReadbackPath `
            -ExpectedAccountFingerprint $fakeAccount -CollectorPath (Get-P5ECollectorPath)
        [void]$caseResults.Add([ordered]@{
            case = $case
            expectedAccepted = $case -in @('valid_control', 'late_observation_control')
            actualAccepted = [bool]$result.accepted
            decision = $result.decision
            errors = @($result.errors)
            p6Ready = [bool]$result.p6Ready
        })
    }

    $collectorOutcomeResults = [System.Collections.Generic.List[object]]::new()
    foreach ($case in @('committed', 'recovery', 'invalid')) {
        $caseDirectory = Join-Path $OutputRoot ('collector-outcome-' + $case)
        $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $caseDirectory -IssuedAtMillis $issued -AccountFingerprint $fakeAccount
        $outcomePath = Get-P5ECollectorOutcomePath -Directory $caseDirectory
        if ($case -eq 'recovery') {
            $minimal = [ordered]@{
                schemaVersion = $script:P5EReadbackSchema
                eventId = Split-Path -Leaf (Get-P5ECanonicalPath -Path $caseDirectory)
                observedAtMillis = $issued + 4000L
                externalCallState = 'RECOVERY_REQUIRED'
                attemptStatus = 'RECOVERY_REQUIRED'
                attemptIdentity = $script:P5EAttemptIdentity
                recoveryReasonCode = 'P5E_SYNTHETIC_RECOVERY'
                acceptance = 'NOT_PROVEN'
                p6Ready = $false
            }
            Write-P5EUtf8NoBom -Path $fixture.ReadbackPath -Text (ConvertTo-P5EJson -Value $minimal)
            [void](Write-P5ECollectorOutcome -EvidenceDirectory $caseDirectory -CollectionPhase 'After' -TypedOutcome 'RECOVERY_REQUIRED' -Detail 'P5E_SYNTHETIC_RECOVERY' -ReadOnlyCommandCount 1 -PostReadbackPath $fixture.ReadbackPath)
        } else {
            [void](Write-P5ECollectorOutcome -EvidenceDirectory $caseDirectory -CollectionPhase 'After' -TypedOutcome 'COMMITTED_READBACK_CAPTURED' -Detail '' -ReadOnlyCommandCount 1 -PostReadbackPath $fixture.ReadbackPath)
            if ($case -eq 'invalid') {
                $outcome = Get-Content -Raw -LiteralPath $outcomePath | ConvertFrom-Json
                $outcome.providerCalls = 1
                Write-P5EUtf8NoBom -Path $outcomePath -Text (ConvertTo-P5EJson -Value $outcome)
            }
        }
        $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $caseDirectory 'instrumentation-stdout.txt') `
            -InstrumentationStderrPath (Join-Path $caseDirectory 'instrumentation-stderr.txt') `
            -MetadataPath $fixture.MetadataPath -PostReadbackPath $fixture.ReadbackPath `
            -ExpectedAccountFingerprint $fakeAccount -CollectorPath (Get-P5ECollectorPath) -CollectorOutcomePath $outcomePath
        [void]$collectorOutcomeResults.Add([ordered]@{
            case = $case
            accepted = [bool]$result.accepted
            decision = $result.decision
            typedOutcome = [string]$result.collectorOutcome
            errors = @($result.errors)
        })
    }

    $boundaryResults = [System.Collections.Generic.List[object]]::new()
    $boundaryCases = [ordered]@{
        'issued_minus_one' = $issued - 1L
        'issued' = $issued
        'expires_minus_one' = $expires - 1L
        'expires' = $expires
        'expires_plus_one' = $expires + 1L
    }
    foreach ($boundary in $boundaryCases.GetEnumerator()) {
        $caseDirectory = Join-Path $OutputRoot ('authorization-boundary-' + $boundary.Key)
        $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $caseDirectory -IssuedAtMillis $issued `
            -AccountFingerprint $fakeAccount
        $consumedAt = [long]$boundary.Value
        $beforeAt = [long][Math]::Min($issued + 20L, $consumedAt)
        $observedAt = [long][Math]::Max($issued + 3050L, $consumedAt + 2L)
        $beforePath = Join-Path $caseDirectory 'before-snapshot.json'
        $afterPath = Join-Path $caseDirectory 'after-snapshot.json'
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json
        $before = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json
        $after = Get-Content -Raw -LiteralPath $afterPath | ConvertFrom-Json
        $before.observedAtMillis = $beforeAt
        $after.observedAtMillis = $observedAt
        $readback.observedAtMillis = $observedAt
        $readback.authorizationReceipt.consumedAtMillis = $consumedAt
        $readback.attempt.createdAtMillis = $consumedAt
        $readback.attempt.updatedAtMillis = $consumedAt + 1L
        $readback.provenance.beforeSnapshotObservedAtMillis = $beforeAt
        $readback.provenance.claimObservedAtMillis = $consumedAt
        $readback.provenance.consumedAtMillis = $consumedAt
        $readback.provenance.attemptCreatedAtMillis = $consumedAt
        $readback.provenance.attemptUpdatedAtMillis = $consumedAt + 1L
        $readback.provenance.observedAtMillis = $observedAt
        $readback.provenance.collectedAtMillis = $observedAt + 100L
        Write-P5EUtf8NoBom -Path $beforePath -Text (ConvertTo-P5EJson -Value $before)
        Write-P5EUtf8NoBom -Path $afterPath -Text (ConvertTo-P5EJson -Value $after)
        $readback.provenance.beforeSnapshotSha256 = Get-P5ESha256 -Path $beforePath
        $readback.provenance.afterSnapshotSha256 = Get-P5ESha256 -Path $afterPath
        Write-P5EUtf8NoBom -Path $fixture.ReadbackPath -Text (ConvertTo-P5EJson -Value $readback)
        $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $caseDirectory 'instrumentation-stdout.txt') `
            -InstrumentationStderrPath (Join-Path $caseDirectory 'instrumentation-stderr.txt') `
            -MetadataPath $fixture.MetadataPath -PostReadbackPath $fixture.ReadbackPath `
            -ExpectedAccountFingerprint $fakeAccount -CollectorPath (Get-P5ECollectorPath)
        [void]$boundaryResults.Add([ordered]@{
            case = $boundary.Key
            consumedAtMillis = $consumedAt
            expectedAccepted = $consumedAt -ge $issued -and $consumedAt -lt $expires
            actualAccepted = [bool]$result.accepted
            decision = $result.decision
            errors = @($result.errors)
        })
    }

    $producerNegativeResults = [System.Collections.Generic.List[object]]::new()
    $collectorMutations = @('missing_report', 'missing_receipt', 'wrong_event', 'schema_drift',
        'wal_incomplete_snapshot', 'invalid_validator_output')
    foreach ($case in $collectorMutations) {
        $caseDirectory = Join-Path $OutputRoot ('producer-' + $case)
        $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $caseDirectory -IssuedAtMillis $issued -AccountFingerprint $fakeAccount
        $thrown = $false
        try {
            switch ($case) {
                'missing_report' { Remove-Item -LiteralPath (Join-Path $caseDirectory 'report.bin') -Force }
                'missing_receipt' { Remove-Item -LiteralPath (Join-Path $caseDirectory 'receipt.bin') -Force }
                'wrong_event' {
                    $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'collector-input.json') | ConvertFrom-Json
                    $source.eventId = 'wrong-event'
                    Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'collector-input.json') -Text (ConvertTo-P5EJson -Value $source)
                }
                'schema_drift' {
                    $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'collector-input.json') | ConvertFrom-Json
                    $source.schemaVersion = 'p5e.raw.readback.producer-input.v0'
                    Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'collector-input.json') -Text (ConvertTo-P5EJson -Value $source)
                }
                'wal_incomplete_snapshot' {
                    $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'after-snapshot.json') | ConvertFrom-Json
                    $source.snapshotMode = 'UNSAFE_COPY_WITH_WAL_PENDING'
                    Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'after-snapshot.json') -Text (ConvertTo-P5EJson -Value $source)
                }
                'invalid_validator_output' {
                    Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'report.bin') -Text 'not-a-serialized-report'
                }
            }
            Invoke-P5ESyntheticReadbackCollector -EvidenceDirectory $caseDirectory -MetadataPath $fixture.MetadataPath `
                -ProducerInputDirectory $caseDirectory -PostReadbackPath $fixture.ReadbackPath | Out-Null
        } catch {
            $thrown = $_.Exception.Message -match '^P5E_COLLECTOR_'
        }
        [void]$producerNegativeResults.Add([ordered]@{ case = $case; typedStop = $thrown; accepted = $false })
    }

    $verifierNegativeResults = [System.Collections.Generic.List[object]]::new()
    $verifierMutations = @('missing_row', 'orphan_lifecycle', 'duplicate_attempt', 'wrong_event',
        'schema_drift', 'wal_incomplete_snapshot', 'missing_report', 'missing_receipt',
        'modified_immutable_tuple', 'wrong_source_hash', 'unknown_cost', 'invalid_validator_output')
    foreach ($case in $verifierMutations) {
        $caseDirectory = Join-Path $OutputRoot ('verifier-' + $case)
        $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $caseDirectory -IssuedAtMillis $issued -AccountFingerprint $fakeAccount
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json
        switch ($case) {
            'missing_row' { $readback.lineageAfter.attempts = 0; $readback.attempt.rowCount = 0 }
            'orphan_lifecycle' {
                $readback.lineageAfter.attempts = 0; $readback.lineageAfter.authorizationReceipts = 0
                $readback.lineageAfter.lifecycle = 1; $readback.lineageAfter.reportOrReceipt = 0; $readback.attempt.rowCount = 0
            }
            'duplicate_attempt' { $readback.lineageAfter.attempts = 2; $readback.attempt.rowCount = 2 }
            'wrong_event' { $readback.provenance.eventId = 'wrong-event' }
            'schema_drift' { $readback.schemaVersion = 'p5e.raw.readback.v0' }
            'wal_incomplete_snapshot' {
                $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'after-snapshot.json') | ConvertFrom-Json
                $source.snapshotMode = 'UNSAFE_COPY_WITH_WAL_PENDING'
                Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'after-snapshot.json') -Text (ConvertTo-P5EJson -Value $source)
            }
            'missing_report' { Remove-Item -LiteralPath (Join-Path $caseDirectory 'report.bin') -Force }
            'missing_receipt' { Remove-Item -LiteralPath (Join-Path $caseDirectory 'receipt.bin') -Force }
            'modified_immutable_tuple' { $readback.freshTuple.bindingIdentity = 'f' * 64 }
            'wrong_source_hash' { $readback.provenance.sourceInputSha256 = '0' * 64 }
            'unknown_cost' { $readback.attempt.metrics.costAccountingComplete = $false }
            'invalid_validator_output' { Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'report.bin') -Text 'not-a-serialized-report' }
        }
        Write-P5EUtf8NoBom -Path $fixture.ReadbackPath -Text (ConvertTo-P5EJson -Value $readback)
        $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $caseDirectory 'instrumentation-stdout.txt') `
            -InstrumentationStderrPath (Join-Path $caseDirectory 'instrumentation-stderr.txt') `
            -MetadataPath $fixture.MetadataPath -PostReadbackPath $fixture.ReadbackPath `
            -ExpectedAccountFingerprint $fakeAccount -CollectorPath (Get-P5ECollectorPath)
        [void]$verifierNegativeResults.Add([ordered]@{ case = $case; accepted = [bool]$result.accepted; errors = @($result.errors) })
    }

    $artifactMutationResults = [System.Collections.Generic.List[object]]::new()
    $artifactMutations = @(
        'wrong_manifest', 'wrong_binding', 'wrong_bundle', 'wrong_predecessor',
        'swapped_report_receipt', 'one_byte_mutation', 'extra_field', 'missing_field',
        'bom', 'invalid_utf8', 'noncanonical_bytes')
    foreach ($case in $artifactMutations) {
        $caseDirectory = Join-Path $OutputRoot ('artifact-' + $case)
        $fixture = New-P5ESyntheticProducerFixture -EvidenceDirectory $caseDirectory -IssuedAtMillis $issued -AccountFingerprint $fakeAccount
        $reportPath = Join-Path $caseDirectory 'report.bin'
        $receiptPath = Join-Path $caseDirectory 'receipt.bin'
        $report = Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json
        $reportBytes = [IO.File]::ReadAllBytes($reportPath)
        $receiptBytes = [IO.File]::ReadAllBytes($receiptPath)
        switch ($case) {
            'wrong_manifest' {
                $report.manifestFingerprint = 'f' * 64
                Write-P5EUtf8NoBom -Path $reportPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
            }
            'wrong_binding' {
                $report.bindingIdentity = 'f' * 64
                Write-P5EUtf8NoBom -Path $reportPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
            }
            'wrong_bundle' {
                $report.bundleIdentity = 'wrong-bundle'
                Write-P5EUtf8NoBom -Path $reportPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
            }
            'wrong_predecessor' {
                $report.predecessorIdentity = 'f' * 64
                Write-P5EUtf8NoBom -Path $reportPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
            }
            'swapped_report_receipt' {
                [IO.File]::WriteAllBytes($reportPath, $receiptBytes)
                [IO.File]::WriteAllBytes($receiptPath, $reportBytes)
            }
            'one_byte_mutation' {
                $reportBytes[0] = [byte](([int]$reportBytes[0]) -bxor 1)
                [IO.File]::WriteAllBytes($reportPath, $reportBytes)
            }
            'extra_field' {
                Add-Member -InputObject $report -MemberType NoteProperty -Name 'extraField' -Value 'not-allowed' -Force
                Write-P5EUtf8NoBom -Path $reportPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
            }
            'missing_field' {
                [void]$report.PSObject.Properties.Remove('gates')
                Write-P5EUtf8NoBom -Path $reportPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
            }
            'bom' {
                $bomBytes = New-Object byte[] ($reportBytes.Length + 3)
                $bomBytes[0] = 0xef; $bomBytes[1] = 0xbb; $bomBytes[2] = 0xbf
                [Array]::Copy($reportBytes, 0, $bomBytes, 3, $reportBytes.Length)
                [IO.File]::WriteAllBytes($reportPath, $bomBytes)
            }
            'invalid_utf8' {
                [IO.File]::WriteAllBytes($reportPath, [byte[]](0xff, 0xfe, 0xfd))
            }
            'noncanonical_bytes' {
                $canonicalText = [Text.UTF8Encoding]::new($false).GetString($reportBytes)
                $nonCanonicalText = '{ ' + $canonicalText.Substring(1)
                [IO.File]::WriteAllBytes($reportPath, [Text.UTF8Encoding]::new($false).GetBytes($nonCanonicalText))
            }
        }
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json
        $storedReport = Get-P5EProperty (Get-P5EProperty $readback 'artifacts') 'report'
        $storedReceipt = Get-P5EProperty (Get-P5EProperty $readback 'artifacts') 'receipt'
        $attempt = Get-P5EProperty $readback 'attempt'
        $provenance = Get-P5EProperty $readback 'provenance'
        $storedReport.sha256 = Get-P5ESha256 -Path $reportPath
        $storedReport.byteLength = [long]([IO.File]::ReadAllBytes($reportPath)).Length
        $storedReceipt.sha256 = Get-P5ESha256 -Path $receiptPath
        $storedReceipt.byteLength = [long]([IO.File]::ReadAllBytes($receiptPath)).Length
        $attempt.reportSha256 = $storedReport.sha256
        $attempt.reportByteLength = $storedReport.byteLength
        $attempt.receiptSha256 = $storedReceipt.sha256
        $attempt.receiptByteLength = $storedReceipt.byteLength
        $provenance.reportBytesSha256 = $storedReport.sha256
        $provenance.reportBytesLength = $storedReport.byteLength
        $provenance.receiptBytesSha256 = $storedReceipt.sha256
        $provenance.receiptBytesLength = $storedReceipt.byteLength
        Write-P5EUtf8NoBom -Path $fixture.ReadbackPath -Text (ConvertTo-P5EJson -Value $readback)
        $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $caseDirectory 'instrumentation-stdout.txt') `
            -InstrumentationStderrPath (Join-Path $caseDirectory 'instrumentation-stderr.txt') `
            -MetadataPath $fixture.MetadataPath -PostReadbackPath $fixture.ReadbackPath `
            -ExpectedAccountFingerprint $fakeAccount -CollectorPath (Get-P5ECollectorPath)
        [void]$artifactMutationResults.Add([ordered]@{
            case = $case
            source = 'HOST_SYNTHETIC_SHAPE_REGRESSION_ONLY'
            expectedAccepted = $false
            actualAccepted = [bool]$result.accepted
            rejected = -not [bool]$result.accepted
            errors = @($result.errors)
        })
    }

    $fingerprintExpected = 'a' * 64
    $fingerprintActual = 'b' * 64
    $redactionCases = @(
        (Protect-P5ECaptureText -Text ('success endpointAccountFingerprint=' + $fingerprintExpected) -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
        (Protect-P5ECaptureText -Text ('ComparisonFailure: expected:<' + $fingerprintExpected + '> but was:<' + $fingerprintActual + '>') -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
        (Protect-P5ECaptureText -Text ('ExceptionWrapper: java.lang.AssertionError: expected=' + $fingerprintExpected + ' actual=' + $fingerprintActual) -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
        (Protect-P5ECaptureText -Text ('error endpointAccountFingerprint=' + $fingerprintActual) -SensitiveValues @($fingerprintExpected, $fingerprintActual)),
        (Protect-P5ECaptureText -Text ('timeout endpointAccountFingerprint=' + $fingerprintExpected) -SensitiveValues @($fingerprintExpected, $fingerprintActual))
    )
    $redactionPass = $true
    foreach ($capture in $redactionCases) {
        if (-not $capture.Violation -or $capture.Text -match $fingerprintExpected -or $capture.Text -match $fingerprintActual) { $redactionPass = $false }
    }
    $report = [ordered]@{
        baselineHelperSha256 = Get-P5ESha256 -Path (Get-P5ECollectorPath)
        helperHashGate = $helperHashGate
        scope = 'SYNTHETIC_ONLY_NO_DEVICE_NO_PROVIDER_NO_ENVIRONMENT_CREDENTIAL_READ'
        cases = $caseResults.ToArray()
        authorizationBoundaryFixtures = $boundaryResults.ToArray()
        producerNegativeFixtures = $producerNegativeResults.ToArray()
        verifierNegativeFixtures = $verifierNegativeResults.ToArray()
        artifactMutationFixtures = $artifactMutationResults.ToArray()
        collectorOutcomeFixtures = $collectorOutcomeResults.ToArray()
        fingerprintAssertionProbe = @{ redactionViolation = -not $redactionPass; syntheticFingerprintStillPresent = -not $redactionPass }
        sourceMapping = [ordered]@{ version = $script:P5ESourceMappingVersion; producer = 'Invoke-P5ESyntheticReadbackCollector'; input = 'collector-input.json'; before = 'before-snapshot.json'; after = 'after-snapshot.json'; transaction = 'transaction-evidence.json'; report = 'report.bin'; receipt = 'receipt.bin' }
        deviceActions = 0
        providerCalls = 0
        p6Ready = $false
    }
    Write-P5EUtf8NoBom -Path (Join-Path $OutputRoot 'RESULT.json') -Text (ConvertTo-P5EJson -Value $report)
    $report | ConvertTo-Json -Depth 12
}

function Assert-P5EPinnedDispatchInputs {
    param(
        [Parameter(Mandatory = $true)][string]$ManifestFile,
        [Parameter(Mandatory = $true)][string]$ExpectedManifestHash,
        [Parameter(Mandatory = $true)][string]$ProductionApkFile,
        [Parameter(Mandatory = $true)][string]$ExpectedProductionHash,
        [Parameter(Mandatory = $true)][string]$TestApkFile,
        [Parameter(Mandatory = $true)][string]$ExpectedTestHash
    )
    $sourceContract = Test-P5ERequiredSourceContract -RepoRoot (Get-P5ERepoRoot)
    if (-not $sourceContract.Passed) { throw ('SOURCE_CONTRACT_STOP:' + ($sourceContract.Errors -join ',')) }
    [void](Get-P5EArtifactContract)
    if (-not (Test-Path -LiteralPath $ManifestFile -PathType Leaf)) { throw 'OWNER_APPROVAL_MANIFEST_MISSING_STOP' }
    if ((Get-P5ESha256 -Path $ManifestFile) -cne $ExpectedManifestHash.ToLowerInvariant()) { throw 'OWNER_APPROVAL_PACKET_HASH_MISMATCH_STOP' }
    if (-not (Test-Path -LiteralPath $ProductionApkFile -PathType Leaf) -or
            (Get-P5ESha256 -Path $ProductionApkFile) -cne $ExpectedProductionHash.ToLowerInvariant()) { throw 'PRODUCTION_CODE207_APK_HASH_MISMATCH_STOP' }
    if (-not (Test-Path -LiteralPath $TestApkFile -PathType Leaf) -or
            (Get-P5ESha256 -Path $TestApkFile) -cne $ExpectedTestHash.ToLowerInvariant()) { throw 'A4_2_TEST_APK_HASH_MISMATCH_STOP' }
}

function Invoke-P5EPrepareEvent {
    [void](Assert-P5EHelperRuntimeHash -ExpectedSha256 $ExpectedHelperSha256)
    Assert-P5EPinnedDispatchInputs -ManifestFile $ManifestPath -ExpectedManifestHash $ExpectedManifestSha256 `
        -ProductionApkFile $ProductionApkPath -ExpectedProductionHash $ExpectedProductionApkSha256 `
        -TestApkFile $TestApkPath -ExpectedTestHash $ExpectedTestApkSha256
    $directory = Test-P5EExactEventDirectory -Directory $EvidenceDirectory
    if (@(Get-ChildItem -LiteralPath $directory -Force).Count -ne 0) { throw 'P5E_EVENT_DIRECTORY_NOT_EMPTY_STOP' }
    $plan = New-P5EEventPlan -EvidenceDirectory $directory -ManifestHash $ExpectedManifestSha256 `
        -ProductionApkHash $ExpectedProductionApkSha256 -TestApkHash $ExpectedTestApkSha256 `
        -HelperHash $ExpectedHelperSha256
    $planPath = Write-P5EEventPlan -Plan $plan
    Write-Output ('P5E_EVENT_PREPARED=' + $directory)
    Write-Output ('P5E_EVENT_PLAN=' + $planPath)
    Write-Output 'P5E_EVENT_DEVICE_ACTIONS=0'
    Write-Output 'P5E_EVENT_PROVIDER_CALLS=0'
    Write-Output 'P5E_EVENT_CREDENTIAL_READS=0'
    exit 0
}

function Invoke-P5EDispatch {
    [void](Assert-P5EHelperRuntimeHash -ExpectedSha256 $ExpectedHelperSha256)
    $directory = Test-P5EExactEventDirectory -Directory $EvidenceDirectory
    $eventPlan = Read-P5EEventPlan -EvidenceDirectory $directory
    if ([string](Get-P5EProperty $eventPlan 'helperSha256') -cne $ExpectedHelperSha256.ToLowerInvariant()) { throw 'P5E_DISPATCH_EVENT_HELPER_HASH_MISMATCH_STOP' }
    if ([string](Get-P5EProperty $eventPlan 'manifestSha256') -cne $ExpectedManifestSha256.ToLowerInvariant() -or
            [string](Get-P5EProperty $eventPlan 'productionApkSha256') -cne $ExpectedProductionApkSha256.ToLowerInvariant() -or
            [string](Get-P5EProperty $eventPlan 'testApkSha256') -cne $ExpectedTestApkSha256.ToLowerInvariant()) {
        throw 'P5E_DISPATCH_EVENT_PIN_MISMATCH_STOP'
    }
    Assert-P5EPinnedDispatchInputs -ManifestFile $ManifestPath -ExpectedManifestHash $ExpectedManifestSha256 `
        -ProductionApkFile $ProductionApkPath -ExpectedProductionHash $ExpectedProductionApkSha256 `
        -TestApkFile $TestApkPath -ExpectedTestHash $ExpectedTestApkSha256
    $accountFingerprint = Get-P5EAccountFingerprint
    $issued = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $expires = $issued + $script:P5EAuthorizationValidityMilliseconds
    $plan = New-P5EPlan -ManifestHash $ExpectedManifestSha256.ToLowerInvariant() -AccountFingerprint $accountFingerprint `
        -IssuedAtMillis $issued -ExpiresAtMillis $expires
    $adbArguments = New-P5EAdbArgumentList -Serial ([string](Get-P5EProperty $eventPlan 'serial')) `
        -RemoteCommandTokens (New-P5ERemoteCommandTokens -Pairs $plan)
    $argumentContract = Test-P5EInstrumentationArguments -AdbArguments $adbArguments `
        -ManifestHash $ExpectedManifestSha256.ToLowerInvariant() -AccountFingerprint $accountFingerprint `
        -IssuedAtMillis $issued -ExpiresAtMillis $expires
    if (-not $argumentContract.Passed) { throw ('HOST_ARGUMENT_CONTRACT_STOP:' + ($argumentContract.Errors -join ',')) }
    $run = Invoke-P5EProcessSupervisor -FilePath 'adb' -ArgumentList $adbArguments `
        -TimeoutMilliseconds $script:P5EHostObservationTimeoutMilliseconds -EvidenceDirectory $directory `
        -SensitiveValues @($accountFingerprint)
    $metadata = New-P5EHostMetadata -Run $run -IssuedAtMillis $issued -ExpiresAtMillis $expires `
        -EvidenceDirectory $directory -MetadataPath (Join-Path $directory 'HOST_RUN_METADATA.json')
    Write-P5EUtf8NoBom -Path (Join-Path $directory 'HOST_RUN_METADATA.json') -Text (ConvertTo-P5EJson $metadata)
    Write-Output ('RAW_LIVE_EVIDENCE_DIR=' + $directory)
    Write-Output ('RAW_LIVE_PROCESS_OUTCOME=' + $run.Outcome)
    Write-Output ('RAW_LIVE_PROCESS_EXIT_CODE=' + $(if ($null -eq $run.ExitCode) { 'NULL' } else { [string]$run.ExitCode }))
    Write-Output ('RAW_LIVE_DISPATCH_COUNT=' + [string]$run.DispatchCount)
    Write-Output 'RAW_LIVE_ACCEPTANCE=NOT_PROVEN_POST_READBACK_REQUIRED'
    if ($run.RedactionViolation) { exit 23 }
    if ($run.TimedOut) { exit 20 }
    if ($run.LaunchCount -eq 0) { exit 22 }
    if ($null -ne $run.ExitCode -and $run.ExitCode -ne 0) { exit 21 }
    exit 0
}

function Invoke-P5EVerifyOutcome {
    $stdoutPath = Join-Path $EvidenceDirectory 'instrumentation-stdout.txt'
    $stderrPath = Join-Path $EvidenceDirectory 'instrumentation-stderr.txt'
    if ([string]::IsNullOrWhiteSpace($MetadataPath)) { $MetadataPath = Join-Path $EvidenceDirectory 'HOST_RUN_METADATA.json' }
    if ([string]::IsNullOrWhiteSpace($PostReadbackPath)) { $PostReadbackPath = Join-Path $EvidenceDirectory 'post-readback.json' }
    $expected = [Environment]::GetEnvironmentVariable($script:P5EAccountEnvironmentName, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($expected) -and $expected -match '^[0-9a-fA-F]{64}$') { $expected = $expected.ToLowerInvariant() }
    $collectorOutcomePath = Get-P5ECollectorOutcomePath -Directory $EvidenceDirectory
    $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath `
        -MetadataPath $MetadataPath -PostReadbackPath $PostReadbackPath -ExpectedAccountFingerprint $expected `
        -CollectorOutcomePath $collectorOutcomePath
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'OUTCOME_VERIFICATION.json') -Text (ConvertTo-P5EJson -Value $result)
    Write-Output ('RAW_OUTCOME_DECISION=' + $result.decision)
    Write-Output ('RAW_ACCEPTED=' + [string]$result.accepted)
    Write-Output ('RAW_OUTCOME_ERROR_COUNT=' + [string]$result.errorCount)
    Write-Output 'P6_READY=FALSE'
    if ($result.accepted) { exit 0 } else { exit 30 }
}

if ($PSCmdlet.ParameterSetName -eq 'Dispatch') {
    Invoke-P5EDispatch
} elseif ($PSCmdlet.ParameterSetName -eq 'PrepareEvent') {
    Invoke-P5EPrepareEvent
} elseif ($PSCmdlet.ParameterSetName -eq 'CollectReadback') {
    Invoke-P5ELiveReadbackCollector -EvidenceDirectory $EvidenceDirectory -CollectionPhase $CollectionPhase `
        -ExpectedHelperSha256 $ExpectedHelperSha256 -SerialValue $Serial -AdbPath $AdbPath -ApkSigner $ApkSignerPath | Out-Host
    exit 0
} elseif ($PSCmdlet.ParameterSetName -eq 'VerifyOutcome') {
    Invoke-P5EVerifyOutcome
} elseif ($PSCmdlet.ParameterSetName -eq 'ProvenanceProbe') {
    Invoke-P5EProvenanceProbe
} elseif ($PSCmdlet.ParameterSetName -eq 'Library') {
    # Dot-sourced offline behavioral tests import the functions without
    # dispatching SelfTest or any other external operation.
    return
} else {
    Invoke-P5ESelfTest
}
