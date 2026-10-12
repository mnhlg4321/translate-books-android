[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$commandPath = Join-Path $PSScriptRoot 'P5E_RAW_AUTHORIZATION_COMMAND.txt'
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$liveTestPath = Join-Path $repoRoot 'app\src\androidTest\java\com\ml\tblandroidtxt\EditorialP5EFreshRawLiveInstrumentedTest.java'
$serializerPath = Join-Path $repoRoot 'editorial-engine\src\main\java\com\ml\tblandroidtxt\editorial\pack\EditorialP5PilotExecution.java'

function Get-Sha256([string]$Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

$command = Get-Content -Raw -LiteralPath $commandPath
$helper = Get-Content -Raw -LiteralPath $helperPath
$liveTest = Get-Content -Raw -LiteralPath $liveTestPath
$serializer = Get-Content -Raw -LiteralPath $serializerPath
$helperSha256 = Get-Sha256 $helperPath
$commandHelperHash = [regex]::Match($command, '\$expectedHelperSha256\s*=\s*''([0-9a-f]{64})''',
    [Text.RegularExpressions.RegexOptions]::IgnoreCase)
$commandHelperHashMatch = $commandHelperHash.Success -and
    $commandHelperHash.Groups[1].Value.ToLowerInvariant() -ceq $helperSha256

$parameterSets = [regex]::Matches($helper, "ParameterSetName\s*=\s*'([^']+)'") |
    ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$helperHashGuard = ($command -match '(?is)Assert-HelperHash') -and
    ($command -match '(?is)-ExpectedHelperSha256') -and
    ($helper -match '(?is)function Assert-P5EHelperRuntimeHash') -and
    $commandHelperHashMatch
$liveCollectorMode = ($parameterSets -contains 'CollectReadback') -and
    ($command -match '(?is)-CollectReadback') -and
    ($helper -match '(?is)function Invoke-P5ELiveReadbackCollector')

$contractPath = Join-Path $repoRoot 'docs\P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json'
$artifactContractPresent = Test-Path -LiteralPath $contractPath -PathType Leaf
$artifactContractHash = if ($artifactContractPresent) { Get-Sha256 $contractPath } else { '' }
$artifactContractPinned = $artifactContractHash -ceq 'ffe70a70e622706fabfa49d5843310ecd5a283b1ca114e32c636ea26b9fae4bf'
$reportProductionShape = ($serializer -match 'report\.put\("canonicalPackHash"') -and
    ($serializer -match 'report\.put\("canonicalProfileHash"') -and
    ($serializer -match 'report\.put\("bundleIdentity"') -and
    ($serializer -match 'report\.put\("evidenceRefs"')
$receiptProductionShape = ($serializer -match 'receipt\.put\("manifestRef"') -and
    ($serializer -match 'receipt\.put\("packRef"') -and
    ($serializer -match 'receipt\.put\("profileRef"') -and
    ($serializer -match 'receipt\.put\("bindingRef"') -and
    ($serializer -match 'receipt\.put\("releaseAttemptCount"')
$separateHostContract = ($helper -match '(?is)Get-P5EArtifactContract') -and
    ($helper -match '(?is)ExpectedSchema .+safe4\.full\.report-l1\.v1') -and
    ($helper -match '(?is)ExpectedSchema .+safe4\.full\.receipt\.v1') -and
    ($helper -match '(?is)function Test-P5EArtifactPair')
$goldenTestPath = Join-Path $repoRoot 'editorial-engine\src\test\java\com\ml\tblandroidtxt\editorial\pack\EditorialP5PilotExecutionBoundaryTest.java'
$goldenSerializerTest = (Test-Path -LiteralPath $goldenTestPath -PathType Leaf) -and
    ((Get-Content -Raw -LiteralPath $goldenTestPath) -match 'committedArtifactsAreGoldenBytesFromSeparateProductionSerializers')
$artifactContractResolved = $artifactContractPresent -and $artifactContractPinned -and $reportProductionShape -and
    $receiptProductionShape -and $separateHostContract -and $goldenSerializerTest

$liveStart = $liveTest.IndexOf('public void authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn()')
$liveEnd = $liveTest.IndexOf('private static void assertCommittedRawResult', $liveStart)
$liveMethod = if ($liveStart -ge 0 -and $liveEnd -gt $liveStart) {
    $liveTest.Substring($liveStart, $liveEnd - $liveStart)
} else { '' }
$postDispatchStatusEmitter = $liveMethod -match 'sendStatus\s*\('
$dispatchCallPresent = $liveMethod -match '\.dispatchRaw\s*\('
$independentPostDispatchCollector = $command -match '(?is)-Dispatch.*-CollectReadback.*-CollectionPhase[\s\S]*After'
$recoveryCollectorPath = $helper -match '(?is)EXTERNAL_CALL_STATE_UNKNOWN' -and
    $helper -match '(?is)RECOVERY_REQUIRED' -and
    $helper -match '(?is)COLLECTOR_TYPED_STOP'

$result = [ordered]@{
    schemaVersion = 'p5e.readiness-reaudit.v1'
    scope = 'STATIC_SOURCE_AND_PACKET_ONLY_NO_DEVICE_NO_PROVIDER_NO_CREDENTIAL_ENVIRONMENT_READ'
    branch = (& git -C $repoRoot branch --show-current).Trim()
    head = (& git -C $repoRoot rev-parse HEAD).Trim()
    files = [ordered]@{
        command = [ordered]@{ path = $commandPath; sha256 = Get-Sha256 $commandPath; expectedHelperHashMatchesCurrentFile = [bool]$commandHelperHashMatch }
        helper = [ordered]@{ path = $helperPath; sha256 = $helperSha256 }
        artifactContract = [ordered]@{ path = $contractPath; sha256 = $artifactContractHash; pinned = [bool]$artifactContractPinned }
        liveTest = [ordered]@{ path = $liveTestPath; sha256 = Get-Sha256 $liveTestPath }
        serializer = [ordered]@{ path = $serializerPath; sha256 = Get-Sha256 $serializerPath }
    }
    findings = @(
        [ordered]@{
            id = 'H1_HELPER_RUNTIME_HASH_UNBOUND'
            findingConfirmed = (-not $helperHashGuard)
            resolvedAssertion = [bool]$helperHashGuard
            commandExecutesHelper = ($command -match '(?is)-File\s+\$helperPath')
            commandChecksExpectedHelperSha256BeforeExecution = [bool]$helperHashGuard
            commandExpectedHelperHashMatchesCurrentFile = [bool]$commandHelperHashMatch
        },
        [ordered]@{
            id = 'H2_NO_EXECUTABLE_LIVE_READBACK_COLLECTOR'
            findingConfirmed = (-not $liveCollectorMode)
            resolvedAssertion = [bool]$liveCollectorMode
            helperParameterSets = @($parameterSets)
            collectReadbackParameterSetPresent = [bool]$liveCollectorMode
        },
        [ordered]@{
            id = 'H3_SYNTHETIC_ARTIFACT_CONTRACT_DIFFERS_FROM_PRODUCTION_SERIALIZER'
            findingConfirmed = (-not $artifactContractResolved)
            resolvedAssertion = [bool]$artifactContractResolved
            productionArtifactContractPresent = [bool]$artifactContractPresent
            productionArtifactContractHashPinned = [bool]$artifactContractPinned
            hostUsesSeparateSourceDerivedContract = [bool]$separateHostContract
            goldenProductionSerializerTestPresent = [bool]$goldenSerializerTest
            productionReportUsesCanonicalAndBundleFields = [bool]$reportProductionShape
            productionReceiptUsesReferenceFields = [bool]$receiptProductionShape
        },
        [ordered]@{
            id = 'H4_LIVE_METHOD_EMITS_NO_POST_DISPATCH_READBACK'
            findingConfirmed = ($dispatchCallPresent -and -not $postDispatchStatusEmitter -and -not $independentPostDispatchCollector)
            resolvedAssertion = [bool]($dispatchCallPresent -and $independentPostDispatchCollector -and $recoveryCollectorPath)
            dispatchCallPresent = [bool]$dispatchCallPresent
            postDispatchStatusEmitterPresent = [bool]$postDispatchStatusEmitter
            independentPostDispatchCollectorPresent = [bool]$independentPostDispatchCollector
            recoveryCollectorPathPresent = [bool]$recoveryCollectorPath
        }
    )
    allFourBlockersConfirmed = [bool]((-not $helperHashGuard) -and (-not $liveCollectorMode) -and
        (-not $artifactContractResolved) -and $dispatchCallPresent -and (-not $postDispatchStatusEmitter))
    allFourBlockersResolved = [bool]($helperHashGuard -and $liveCollectorMode -and
        $artifactContractResolved -and $dispatchCallPresent -and $independentPostDispatchCollector -and $recoveryCollectorPath)
    deviceActions = 0
    providerCalls = 0
    credentialReads = 0
    readyForOwnerDecision = [bool]($helperHashGuard -and $liveCollectorMode -and $artifactContractResolved -and
        $dispatchCallPresent -and $independentPostDispatchCollector -and $recoveryCollectorPath)
    readyForDispatch = $false
    p6Ready = $false
}

$result | ConvertTo-Json -Depth 8
