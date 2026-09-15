[CmdletBinding(DefaultParameterSetName = 'SelfTest')]
param(
    [Parameter(ParameterSetName = 'SelfTest')]
    [switch]$SelfTest,

    [Parameter(ParameterSetName = 'Dispatch', Mandatory = $true)]
    [switch]$Dispatch,

    [Parameter(ParameterSetName = 'VerifyOutcome', Mandatory = $true)]
    [switch]$VerifyOutcome,

    [Parameter(ParameterSetName = 'ProvenanceProbe', Mandatory = $true)]
    [switch]$ProvenanceProbe,

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$ManifestPath = 'D:\App Translate Books\App Translate Books-translation-profile\docs\P5E_RAW_AUTHORIZATION_APPROVAL_MANIFEST.md',

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$ExpectedManifestSha256 = 'DD58BF339FCC0C0C2A25895B5AE614AF31A5A281677B33A0D39F171F9DA24501',

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$ProductionApkPath = 'D:\App Translate Books\App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725\TranslateBooks-v4.17-p5e.11-code207.apk',

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$ExpectedProductionApkSha256 = '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD',

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$TestApkPath = 'D:\App Translate Books\App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\a4-1-test-20260914-065532\app-debug-androidTest.apk',

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$ExpectedTestApkSha256 = '57EC99A95EE2DC0F1759934C62CEA39E2EC92EB77C3DAF76CFEED28D41A2FDEA',

    [Parameter(ParameterSetName = 'Dispatch')]
    [string]$EvidenceRoot = 'D:\P5E-private',

    [Parameter(ParameterSetName = 'VerifyOutcome', Mandatory = $true)]
    [string]$EvidenceDirectory,

    [Parameter(ParameterSetName = 'VerifyOutcome')]
    [string]$PostReadbackPath,

    [Parameter(ParameterSetName = 'VerifyOutcome')]
    [string]$MetadataPath,

    [Parameter(ParameterSetName = 'ProvenanceProbe')]
    [string]$OutputRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:P5EHostRunSchema = 'p5e.raw.host-run.v1'
$script:P5EReadbackSchema = 'p5e.raw.readback.v1'
$script:P5EReadbackProvenanceSchema = 'p5e.raw.readback.provenance.v1'
$script:P5EProducerInputSchema = 'p5e.raw.readback.producer-input.v1'
$script:P5ECollectorImplementationId = 'p5e.raw.host-readback-collector.v1'
$script:P5ECollectorImplementationVersion = '1'
$script:P5ESourceMappingVersion = 'p5e.raw.readback.source-map.v1'
$script:P5ESyntheticArtifactValidatorId = 'p5e.raw.serialized-artifact-validator.v1'
$script:P5ESerial = '15e84958'
$script:P5EClass = 'com.ml.tblandroidtxt.EditorialP5EFreshRawLiveInstrumentedTest'
$script:P5EClassMethod =
    $script:P5EClass + '#authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn'
$script:P5ETestPackage = 'com.ml.tblandroidtxt.test'
$script:P5ETargetPackage = 'com.ml.tblandroidtxt'
$script:P5ERunner = 'androidx.test.runner.AndroidJUnitRunner'
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

function Get-P5EAccountFingerprint {
    $value = [Environment]::GetEnvironmentVariable($script:P5EAccountEnvironmentName, 'Process')
    if ([string]::IsNullOrWhiteSpace($value) -or $value -notmatch '^[0-9a-fA-F]{64}$') {
        throw 'OWNER_ENDPOINT_ACCOUNT_FINGERPRINT_MISSING_OR_INVALID_STOP'
    }
    # The Android method returns lowercase hex and uses assertEquals, while
    # the host accepts either case. Normalize only in process memory.
    return $value.ToLowerInvariant()
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

function Invoke-P5EProcessSupervisor {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][long]$TimeoutMilliseconds,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [AllowEmptyCollection()][string[]]$SensitiveValues = @()
    )
    $stdoutPath = Join-Path $EvidenceDirectory 'instrumentation-stdout.txt'
    $stderrPath = Join-Path $EvidenceDirectory 'instrumentation-stderr.txt'
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($argument in $ArgumentList) { [void]$startInfo.ArgumentList.Add([string]$argument) }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $launchCount = 0
    $timedOut = $false
    $exitCode = $null
    $launchErrorClass = ''
    $stdout = ''
    $stderr = ''
    $redactionViolation = $false
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
        } else {
            $process.WaitForExit()
        }
        if ($stdoutTask -ne $null) { $stdout = $stdoutTask.GetAwaiter().GetResult() }
        if ($stderrTask -ne $null) { $stderr = $stderrTask.GetAwaiter().GetResult() }
        if ($process.HasExited) { $exitCode = $process.ExitCode }
    } catch {
        if ($launchCount -eq 0) { $launchErrorClass = $_.Exception.GetType().Name }
        else { $stderr = $_.Exception.GetType().Name }
    } finally {
        if ($process -ne $null) { $process.Dispose() }
    }
    $safeStdout = Protect-P5ECaptureText -Text ([string]$stdout) -SensitiveValues $SensitiveValues
    $safeStderr = Protect-P5ECaptureText -Text ([string]$stderr) -SensitiveValues $SensitiveValues
    $redactionViolation = $safeStdout.Violation -or $safeStderr.Violation
    Write-P5EUtf8NoBom -Path $stdoutPath -Text $safeStdout.Text
    Write-P5EUtf8NoBom -Path $stderrPath -Text $safeStderr.Text
    $outcome = if ($launchCount -eq 0) { 'FAILED_BEFORE_LAUNCH' }
        elseif ($timedOut) { 'TIMEOUT' }
        elseif ($null -ne $exitCode -and $exitCode -eq 0) { 'PROCESS_EXITED_ZERO' }
        else { 'PROCESS_EXITED_NONZERO' }
    return [pscustomobject]@{
        Outcome = $outcome
        LaunchCount = $launchCount
        DispatchCount = $launchCount
        TimedOut = $timedOut
        ExitCode = $exitCode
        LaunchErrorClass = $launchErrorClass
        RedactionViolation = $redactionViolation
        StdoutPath = $stdoutPath
        StderrPath = $stderrPath
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

function Get-P5EProperty {
    param($Object, [Parameter(Mandatory = $true)][string]$Name)
    if ($null -eq $Object) { return $null }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return $property.Value
}

function Test-P5EHasProperty {
    param($Object, [Parameter(Mandatory = $true)][string]$Name)
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
        [string]$CollectorPath
    )
    $errors = [System.Collections.Generic.List[string]]::new()
    $captureSensitiveValues = @()
    if (-not [string]::IsNullOrWhiteSpace($ExpectedAccountFingerprint) -and
            $ExpectedAccountFingerprint -match '^[0-9a-fA-F]{64}$') {
        $captureSensitiveValues = @($ExpectedAccountFingerprint)
    }
    if ([string]::IsNullOrWhiteSpace($CollectorPath)) { $CollectorPath = Get-P5ECollectorPath }
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
        try { $readback = Get-Content -Raw -LiteralPath $PostReadbackPath | ConvertFrom-Json -Depth 30 }
        catch { Add-P5EError -Errors $errors -Code 'POST_READBACK_INVALID_JSON' }
    }
    if ($null -ne $readback) {
        Test-P5EReadback -Readback $readback -ExpectedAccountFingerprint $ExpectedAccountFingerprint `
            -Metadata $metadata -PostReadbackPath $PostReadbackPath -MetadataPath $MetadataPath `
            -CollectorPath $CollectorPath -Errors $errors
    }
    $decision = 'RAW_NOT_ACCEPTED'
    $metadataLaunchCount = if ($null -eq $metadata) { $null } else { Get-P5EProperty $metadata 'launchCount' }
    $metadataExternalState = if ($null -eq $metadata) { '' } else { [string](Get-P5EProperty $metadata 'externalCallState') }
    if ($null -ne $metadata -and [bool](Get-P5EProperty $metadata 'timedOut')) {
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
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Errors
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
        $value = $text | ConvertFrom-Json -Depth 30
    } catch {
        Add-P5EError -Errors $Errors -Code ('ARTIFACT_SERIALIZED_JSON_INVALID:' + $Kind)
        return $null
    }
    $required = @('schemaVersion', 'artifactType', 'bindingIdentity', 'manifestFingerprint',
        'packHash', 'profileHash', 'chapterKey', 'phase', 'predecessorIdentity')
    Test-P5EObjectShape -Object $value -Path ('storedArtifact.' + $Kind) -Required $required -Allowed $required `
        -Errors $Errors | Out-Null
    Test-P5EEqual $value 'schemaVersion' $ExpectedSchema ('storedArtifact.' + $Kind) $Errors
    Test-P5EEqual $value 'artifactType' 'REPORT_L1' ('storedArtifact.' + $Kind) $Errors
    Test-P5ESha256 $value 'bindingIdentity' ('storedArtifact.' + $Kind) $Errors $script:P5EBindingIdentity
    Test-P5ESha256 $value 'manifestFingerprint' ('storedArtifact.' + $Kind) $Errors $script:P5EExpectedManifestFingerprint
    Test-P5ESha256 $value 'packHash' ('storedArtifact.' + $Kind) $Errors $script:P5EPackHash
    Test-P5ESha256 $value 'profileHash' ('storedArtifact.' + $Kind) $Errors $script:P5EProfileHash
    Test-P5EEqual $value 'chapterKey' $script:P5EChapterKey ('storedArtifact.' + $Kind) $Errors
    Test-P5EEqual $value 'phase' $script:P5EPhase ('storedArtifact.' + $Kind) $Errors
    Test-P5ESha256 $value 'predecessorIdentity' ('storedArtifact.' + $Kind) $Errors $script:P5ERunDeclarationIdentity
    return [pscustomobject]@{ Length = [long]$bytes.Length; Sha256 = Get-P5ESha256 -Path $Path }
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

    if ($null -ne $reportPath) {
        $reportBytes = Test-P5ESerializedArtifactBytes -Path $reportPath -ExpectedSchema 'safe4.full.report-l1.v1' -Kind 'report' -Errors $Errors
        if ($null -ne $reportBytes -and [string](Get-P5EProperty (Get-P5EProperty $Readback 'artifacts') 'report').sha256 -ine $reportBytes.Sha256) {
            Add-P5EError -Errors $Errors -Code 'REPORT_STORED_BYTES_HASH_MISMATCH'
        }
    }
    if ($null -ne $receiptPath) {
        $receiptBytes = Test-P5ESerializedArtifactBytes -Path $receiptPath -ExpectedSchema 'safe4.full.receipt.v1' -Kind 'receipt' -Errors $Errors
        if ($null -ne $receiptBytes -and [string](Get-P5EProperty (Get-P5EProperty $Readback 'artifacts') 'receipt').sha256 -ine $receiptBytes.Sha256) {
            Add-P5EError -Errors $Errors -Code 'RECEIPT_STORED_BYTES_HASH_MISMATCH'
        }
    }

    $sourceInput = $null; $before = $null; $after = $null; $transaction = $null
    try {
        if ($null -ne $inputPath) { $sourceInput = Get-Content -Raw -LiteralPath $inputPath | ConvertFrom-Json -Depth 30 }
        if ($null -ne $beforePath) { $before = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json -Depth 30 }
        if ($null -ne $afterPath) { $after = Get-Content -Raw -LiteralPath $afterPath | ConvertFrom-Json -Depth 30 }
        if ($null -ne $transactionPath) { $transaction = Get-Content -Raw -LiteralPath $transactionPath | ConvertFrom-Json -Depth 30 }
    } catch {
        Add-P5EError -Errors $Errors -Code 'PRODUCER_SOURCE_JSON_INVALID'
    }
    if ($null -ne $sourceInput) {
        Test-P5EObjectShape -Object $sourceInput -Path 'producerInput' -Required @('schemaVersion', 'eventId', 'runIdentity', 'sourceMappingVersion') `
            -Allowed @('schemaVersion', 'eventId', 'runIdentity', 'sourceMappingVersion', 'production', 'test', 'freshTuple', 'attemptTemplate', 'authorizationReceipt', 'lifecycle', 'reportArtifactTemplate', 'receiptArtifactTemplate') -Errors $Errors | Out-Null
        Test-P5EEqual $sourceInput 'schemaVersion' $script:P5EProducerInputSchema 'producerInput' $Errors
        Test-P5EEqual $sourceInput 'eventId' $eventId 'producerInput' $Errors
        Test-P5EEqual $sourceInput 'runIdentity' $script:P5ERunDeclarationIdentity 'producerInput' $Errors
        Test-P5EEqual $sourceInput 'sourceMappingVersion' $script:P5ESourceMappingVersion 'producerInput' $Errors
    }
    foreach ($source in @($before, $after)) {
        if ($null -ne $source) {
            Test-P5EObjectShape -Object $source -Path 'producerSnapshot' -Required @('schemaVersion', 'eventId', 'runIdentity', 'snapshotMode', 'observedAtMillis', 'databaseSha256', 'databaseSchemaVersion', 'integrityCheck', 'foreignKeyViolations', 'lineage', 'immutableTupleIdentity', 'unrelatedWrites', 'deletedRows') `
                -Allowed @('schemaVersion', 'eventId', 'runIdentity', 'snapshotMode', 'observedAtMillis', 'databaseSha256', 'databaseSchemaVersion', 'integrityCheck', 'foreignKeyViolations', 'lineage', 'immutableTupleIdentity', 'unrelatedWrites', 'deletedRows') -Errors $Errors | Out-Null
            Test-P5EEqual $source 'eventId' $eventId 'producerSnapshot' $Errors
            Test-P5EEqual $source 'runIdentity' $script:P5ERunDeclarationIdentity 'producerSnapshot' $Errors
            Test-P5EEqual $source 'snapshotMode' 'WAL_AWARE_CONSISTENT' 'producerSnapshot' $Errors
            Test-P5ELongRange -Object $source -Name 'observedAtMillis' -Path 'producerSnapshot' -Errors $Errors -Minimum 0 | Out-Null
            Test-P5ESha256 $source 'databaseSha256' 'producerSnapshot' $Errors
            Test-P5ELong $source 'databaseSchemaVersion' $script:P5EDatabaseSchemaVersion 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            Test-P5EEqual $source 'integrityCheck' 'ok' 'producerSnapshot' $Errors
            Test-P5ELong $source 'foreignKeyViolations' 0L 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            Test-P5ESha256 $source 'immutableTupleIdentity' 'producerSnapshot' $Errors $script:P5EBindingIdentity
            Test-P5ELong $source 'unrelatedWrites' 0L 'producerSnapshot' $Errors -Minimum 0 | Out-Null
            Test-P5ELong $source 'deletedRows' 0L 'producerSnapshot' $Errors -Minimum 0 | Out-Null
        }
    }
    if ($null -ne $transaction) {
        $transactionFields = @('schemaVersion', 'eventId', 'runIdentity', 'transactionTestId',
            'transactionSemanticsPassed', 'beforeAfterConsistent', 'rowPairConsistent',
            'claimAndAuthorizationAtomic', 'reportReceiptAtomic', 'noUnrelatedWrites', 'noDeletes')
        Test-P5EObjectShape -Object $transaction -Path 'transactionEvidence' -Required $transactionFields -Allowed $transactionFields -Errors $Errors | Out-Null
        Test-P5EEqual $transaction 'schemaVersion' 'p5e.raw.transaction-evidence.v1' 'transactionEvidence' $Errors
        Test-P5EEqual $transaction 'eventId' $eventId 'transactionEvidence' $Errors
        Test-P5EEqual $transaction 'runIdentity' $script:P5ERunDeclarationIdentity 'transactionEvidence' $Errors
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
    }

    $atomicity = Get-P5EProperty $provenance 'atomicityEvidence'
    $atomicityFields = @('transactionEvidencePath', 'transactionEvidenceSha256', 'transactionSemanticsPassed',
        'beforeAfterConsistent', 'rowPairConsistent', 'claimAndAuthorizationAtomic', 'reportReceiptAtomic',
        'reportBytesValidated', 'receiptBytesValidated', 'validatorId')
    Test-P5EObjectShape -Object $atomicity -Path 'readback.provenance.atomicityEvidence' -Required $atomicityFields -Allowed $atomicityFields -Errors $Errors | Out-Null
    if ($null -ne $atomicity) {
        Test-P5EEqual $atomicity 'transactionEvidencePath' ([string](Get-P5EProperty $provenance 'transactionEvidencePath')) 'atomicityEvidence' $Errors
        Test-P5EEqual $atomicity 'transactionEvidenceSha256' ([string](Get-P5EProperty $provenance 'transactionEvidenceSha256')) 'atomicityEvidence' $Errors
        Test-P5EEqual $atomicity 'validatorId' $script:P5ESyntheticArtifactValidatorId 'atomicityEvidence' $Errors
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
        PRONOUN_SEMANTIC = @($script:P5EPronounSourceBytes, $script:P5EPronounSourceSha256, 'HIDDEN')
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
        Test-P5EObjectShape $artifact ('artifacts.' + $kind) @('schemaVersion', 'artifactType', 'bindingIdentity', 'manifestFingerprint', 'packHash', 'profileHash', 'chapterKey', 'phase', 'predecessorIdentity', 'byteLength', 'sha256', 'validationPassed') @('schemaVersion', 'artifactType', 'bindingIdentity', 'manifestFingerprint', 'packHash', 'profileHash', 'chapterKey', 'phase', 'predecessorIdentity', 'byteLength', 'sha256', 'validationPassed') $Errors | Out-Null
        $schema = if ($kind -eq 'report') { 'safe4.full.report-l1.v1' } else { 'safe4.full.receipt.v1' }
        Test-P5EEqual $artifact 'schemaVersion' $schema ('artifacts.' + $kind) $Errors
        Test-P5EEqual $artifact 'artifactType' 'REPORT_L1' ('artifacts.' + $kind) $Errors
        Test-P5ESha256 $artifact 'bindingIdentity' ('artifacts.' + $kind) $Errors $script:P5EBindingIdentity
        Test-P5ESha256 $artifact 'manifestFingerprint' ('artifacts.' + $kind) $Errors $script:P5EExpectedManifestFingerprint
        Test-P5ESha256 $artifact 'packHash' ('artifacts.' + $kind) $Errors $script:P5EPackHash
        Test-P5ESha256 $artifact 'profileHash' ('artifacts.' + $kind) $Errors $script:P5EProfileHash
        Test-P5EEqual $artifact 'chapterKey' $script:P5EChapterKey ('artifacts.' + $kind) $Errors
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
                [ordered]@{ role = 'PRONOUN_SEMANTIC'; visibility = 'HIDDEN'; byteLength = $script:P5EPronounSourceBytes; sha256 = $script:P5EPronounSourceSha256 }
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
            packHash = $script:P5EPackHash
            profileHash = $script:P5EProfileHash
            chapterKey = $script:P5EChapterKey
            phase = $script:P5EPhase
            predecessorIdentity = $script:P5ERunDeclarationIdentity
        }
        receiptArtifactTemplate = [ordered]@{
            schemaVersion = 'safe4.full.receipt.v1'
            artifactType = 'REPORT_L1'
            bindingIdentity = $script:P5EBindingIdentity
            manifestFingerprint = $script:P5EExpectedManifestFingerprint
            packHash = $script:P5EPackHash
            profileHash = $script:P5EProfileHash
            chapterKey = $script:P5EChapterKey
            phase = $script:P5EPhase
            predecessorIdentity = $script:P5ERunDeclarationIdentity
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
        $metadata = Get-Content -Raw -LiteralPath $MetadataPath | ConvertFrom-Json -Depth 30
        $producerInput = Get-Content -Raw -LiteralPath $inputPath | ConvertFrom-Json -Depth 30
        $before = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json -Depth 30
        $after = Get-Content -Raw -LiteralPath $afterPath | ConvertFrom-Json -Depth 30
        $transaction = Get-Content -Raw -LiteralPath $transactionPath | ConvertFrom-Json -Depth 30
    } catch { throw 'P5E_COLLECTOR_SOURCE_JSON_INVALID' }
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
                [string](Get-P5EProperty $snapshot 'snapshotMode') -cne 'WAL_AWARE_CONSISTENT') {
            throw 'P5E_COLLECTOR_SNAPSHOT_NOT_CONSISTENT'
        }
    }
    $transactionRequired = @('schemaVersion', 'eventId', 'runIdentity', 'transactionTestId',
        'transactionSemanticsPassed', 'beforeAfterConsistent', 'rowPairConsistent',
        'claimAndAuthorizationAtomic', 'reportReceiptAtomic', 'noUnrelatedWrites', 'noDeletes')
    foreach ($name in $transactionRequired) {
        if (-not (Test-P5EHasProperty -Object $transaction -Name $name)) { throw ('P5E_COLLECTOR_TRANSACTION_FIELD_MISSING:' + $name) }
    }
    if ([string](Get-P5EProperty $transaction 'schemaVersion') -cne 'p5e.raw.transaction-evidence.v1' -or
            [string](Get-P5EProperty $transaction 'runIdentity') -cne $script:P5ERunDeclarationIdentity) {
        throw 'P5E_COLLECTOR_TRANSACTION_SCHEMA_INVALID'
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
    $before = [ordered]@{
        schemaVersion = 'p5e.raw.snapshot.v1'; eventId = $metadata['eventId']; runIdentity = $script:P5ERunDeclarationIdentity
        snapshotMode = 'WAL_AWARE_CONSISTENT'; observedAtMillis = $IssuedAtMillis + 20; databaseSha256 = $script:P5EDatabaseSha256
        databaseSchemaVersion = $script:P5EDatabaseSchemaVersion; integrityCheck = 'ok'; foreignKeyViolations = 0
        lineage = $zeroLineage; immutableTupleIdentity = $script:P5EBindingIdentity; unrelatedWrites = 0; deletedRows = 0
    }
    $after = [ordered]@{
        schemaVersion = 'p5e.raw.snapshot.v1'; eventId = $metadata['eventId']; runIdentity = $script:P5ERunDeclarationIdentity
        snapshotMode = 'WAL_AWARE_CONSISTENT'; observedAtMillis = $ObservationAtMillis; databaseSha256 = 'e' * 64
        databaseSchemaVersion = $script:P5EDatabaseSchemaVersion; integrityCheck = 'ok'; foreignKeyViolations = 0
        lineage = $afterLineage; immutableTupleIdentity = $script:P5EBindingIdentity; unrelatedWrites = 0; deletedRows = 0
    }
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'before-snapshot.json') -Text (ConvertTo-P5EJson -Value $before)
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'after-snapshot.json') -Text (ConvertTo-P5EJson -Value $after)
    $transaction = [ordered]@{
        schemaVersion = 'p5e.raw.transaction-evidence.v1'; eventId = $metadata['eventId']; runIdentity = $script:P5ERunDeclarationIdentity
        transactionTestId = 'synthetic-atomic-claim-v1'; transactionSemanticsPassed = $true; beforeAfterConsistent = $true
        rowPairConsistent = $true; claimAndAuthorizationAtomic = $true; reportReceiptAtomic = $true
        noUnrelatedWrites = $true; noDeletes = $true
    }
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'transaction-evidence.json') -Text (ConvertTo-P5EJson -Value $transaction)
    $report = $producerInput['reportArtifactTemplate']
    $receipt = $producerInput['receiptArtifactTemplate']
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'report.bin') -Text (ConvertTo-P5EJson -Value $report)
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'receipt.bin') -Text (ConvertTo-P5EJson -Value $receipt)
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
                @{ role = 'PRONOUN_SEMANTIC'; visibility = 'HIDDEN'; byteLength = $script:P5EPronounSourceBytes; sha256 = $script:P5EPronounSourceSha256 }) }
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
    return (ConvertTo-P5EJson -Value $Value | ConvertFrom-Json -Depth 30)
}

function Assert-P5ESelfTest {
    param([Parameter(Mandatory = $true)][bool]$Condition, [Parameter(Mandatory = $true)][string]$Name)
    if (-not $Condition) { throw ('SELFTEST_FAILED:' + $Name) }
}

function Invoke-P5ESelfTest {
    $repoRoot = Get-P5ERepoRoot
    $sourceContract = Test-P5ERequiredSourceContract -RepoRoot $repoRoot
    Assert-P5ESelfTest $sourceContract.Passed 'source-required-argument-contract'
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
        $fakePowerShell = Join-Path $PSHOME 'pwsh.exe'
        Assert-P5ESelfTest (Test-Path -LiteralPath $fakePowerShell -PathType Leaf) 'fake-powershell-available'
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
            $valid = Get-Content -Raw -LiteralPath $readbackPath | ConvertFrom-Json -Depth 30
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
    Write-Output 'P5E_FINGERPRINT_FAILURE_REDACTION=PASS'
    Write-Output 'P5E_PROVIDER_DEVICE_ACTIONS=0'
}

function Invoke-P5EProvenanceProbe {
    if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
        $OutputRoot = Join-Path (Get-P5ERepoRoot) ('evidence\p5e-provenance-probe-' + [Guid]::NewGuid().ToString('N'))
    }
    if (Test-Path -LiteralPath $OutputRoot) { throw 'PROVENANCE_PROBE_OUTPUT_MUST_BE_NEW' }
    [void](New-Item -ItemType Directory -Path $OutputRoot)
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
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json -Depth 30
        $metadata = Get-Content -Raw -LiteralPath $fixture.MetadataPath | ConvertFrom-Json -Depth 30
        switch ($case) {
            'observed_before_run' { $readback.observedAtMillis = $issued - 1L }
            'consumed_after_expiry' { $readback.authorizationReceipt.consumedAtMillis = $expires }
            'artifact_manifest_mismatch' { $readback.artifacts.receipt.manifestFingerprint = 'f' * 64 }
            'artifact_manifest_both_same_wrong_expected' {
                $readback.artifacts.report.manifestFingerprint = 'f' * 64
                $readback.artifacts.receipt.manifestFingerprint = 'f' * 64
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
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json -Depth 30
        $before = Get-Content -Raw -LiteralPath $beforePath | ConvertFrom-Json -Depth 30
        $after = Get-Content -Raw -LiteralPath $afterPath | ConvertFrom-Json -Depth 30
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
                    $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'collector-input.json') | ConvertFrom-Json -Depth 30
                    $source.eventId = 'wrong-event'
                    Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'collector-input.json') -Text (ConvertTo-P5EJson -Value $source)
                }
                'schema_drift' {
                    $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'collector-input.json') | ConvertFrom-Json -Depth 30
                    $source.schemaVersion = 'p5e.raw.readback.producer-input.v0'
                    Write-P5EUtf8NoBom -Path (Join-Path $caseDirectory 'collector-input.json') -Text (ConvertTo-P5EJson -Value $source)
                }
                'wal_incomplete_snapshot' {
                    $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'after-snapshot.json') | ConvertFrom-Json -Depth 30
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
        $readback = Get-Content -Raw -LiteralPath $fixture.ReadbackPath | ConvertFrom-Json -Depth 30
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
                $source = Get-Content -Raw -LiteralPath (Join-Path $caseDirectory 'after-snapshot.json') | ConvertFrom-Json -Depth 30
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
        scope = 'SYNTHETIC_ONLY_NO_DEVICE_NO_PROVIDER_NO_ENVIRONMENT_CREDENTIAL_READ'
        cases = $caseResults.ToArray()
        authorizationBoundaryFixtures = $boundaryResults.ToArray()
        producerNegativeFixtures = $producerNegativeResults.ToArray()
        verifierNegativeFixtures = $verifierNegativeResults.ToArray()
        fingerprintAssertionProbe = @{ redactionViolation = -not $redactionPass; syntheticFingerprintStillPresent = -not $redactionPass }
        sourceMapping = [ordered]@{ version = $script:P5ESourceMappingVersion; producer = 'Invoke-P5ESyntheticReadbackCollector'; input = 'collector-input.json'; before = 'before-snapshot.json'; after = 'after-snapshot.json'; transaction = 'transaction-evidence.json'; report = 'report.bin'; receipt = 'receipt.bin' }
        deviceActions = 0
        providerCalls = 0
        p6Ready = $false
    }
    Write-P5EUtf8NoBom -Path (Join-Path $OutputRoot 'RESULT.json') -Text (ConvertTo-P5EJson -Value $report)
    $report | ConvertTo-Json -Depth 12
}

function Invoke-P5EDispatch {
    $repoRoot = Get-P5ERepoRoot
    $sourceContract = Test-P5ERequiredSourceContract -RepoRoot $repoRoot
    if (-not $sourceContract.Passed) { throw ('SOURCE_CONTRACT_STOP:' + ($sourceContract.Errors -join ',')) }
    if (-not (Test-Path -LiteralPath $ManifestPath -PathType Leaf)) { throw 'OWNER_APPROVAL_MANIFEST_MISSING_STOP' }
    if ((Get-P5ESha256 -Path $ManifestPath) -ne $ExpectedManifestSha256.ToLowerInvariant()) { throw 'OWNER_APPROVAL_PACKET_HASH_MISMATCH_STOP' }
    if (-not (Test-Path -LiteralPath $ProductionApkPath -PathType Leaf) -or
            (Get-P5ESha256 -Path $ProductionApkPath) -ne $ExpectedProductionApkSha256.ToLowerInvariant()) { throw 'PRODUCTION_CODE207_APK_HASH_MISMATCH_STOP' }
    if (-not (Test-Path -LiteralPath $TestApkPath -PathType Leaf) -or
            (Get-P5ESha256 -Path $TestApkPath) -ne $ExpectedTestApkSha256.ToLowerInvariant()) { throw 'A4_2_TEST_APK_HASH_MISMATCH_STOP' }
    $accountFingerprint = Get-P5EAccountFingerprint
    $issued = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $expires = $issued + $script:P5EAuthorizationValidityMilliseconds
    $plan = New-P5EPlan -ManifestHash $ExpectedManifestSha256.ToLowerInvariant() -AccountFingerprint $accountFingerprint -IssuedAtMillis $issued -ExpiresAtMillis $expires
    $adbArguments = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens (New-P5ERemoteCommandTokens -Pairs $plan)
    $argumentContract = Test-P5EInstrumentationArguments -AdbArguments $adbArguments -ManifestHash $ExpectedManifestSha256.ToLowerInvariant() -AccountFingerprint $accountFingerprint -IssuedAtMillis $issued -ExpiresAtMillis $expires
    if (-not $argumentContract.Passed) { throw ('HOST_ARGUMENT_CONTRACT_STOP:' + ($argumentContract.Errors -join ',')) }
    $evidenceDirectory = New-P5EUniqueEvidenceDirectory -Root $EvidenceRoot
    $run = Invoke-P5EProcessSupervisor -FilePath 'adb' -ArgumentList $adbArguments -TimeoutMilliseconds $script:P5EHostObservationTimeoutMilliseconds -EvidenceDirectory $evidenceDirectory -SensitiveValues @($accountFingerprint)
    $metadata = New-P5EHostMetadata -Run $run -IssuedAtMillis $issued -ExpiresAtMillis $expires -EvidenceDirectory $evidenceDirectory
    Write-P5EUtf8NoBom -Path (Join-Path $evidenceDirectory 'HOST_RUN_METADATA.json') -Text (ConvertTo-P5EJson $metadata)
    Write-Output ('RAW_LIVE_EVIDENCE_DIR=' + $evidenceDirectory)
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
    $expected = [Environment]::GetEnvironmentVariable($script:P5EAccountEnvironmentName, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($expected) -and $expected -match '^[0-9a-fA-F]{64}$') { $expected = $expected.ToLowerInvariant() }
    $result = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdoutPath -InstrumentationStderrPath $stderrPath -MetadataPath $MetadataPath -PostReadbackPath $PostReadbackPath -ExpectedAccountFingerprint $expected
    Write-P5EUtf8NoBom -Path (Join-Path $EvidenceDirectory 'OUTCOME_VERIFICATION.json') -Text (ConvertTo-P5EJson -Value $result)
    Write-Output ('RAW_OUTCOME_DECISION=' + $result.decision)
    Write-Output ('RAW_ACCEPTED=' + [string]$result.accepted)
    Write-Output ('RAW_OUTCOME_ERROR_COUNT=' + [string]$result.errorCount)
    Write-Output 'P6_READY=FALSE'
    if ($result.accepted) { exit 0 } else { exit 30 }
}

if ($PSCmdlet.ParameterSetName -eq 'Dispatch') {
    Invoke-P5EDispatch
} elseif ($PSCmdlet.ParameterSetName -eq 'VerifyOutcome') {
    Invoke-P5EVerifyOutcome
} elseif ($PSCmdlet.ParameterSetName -eq 'ProvenanceProbe') {
    Invoke-P5EProvenanceProbe
} else {
    Invoke-P5ESelfTest
}
