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

$parameterSets = [regex]::Matches($helper, "ParameterSetName\s*=\s*'([^']+)'") |
    ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
$helperHashGuard = $command -match '(?is)ExpectedHelperSha256|Get-FileHash[^\r\n]*\$helperPath'
$liveCollectorMode = $parameterSets -contains 'CollectReadback'

$helperArtifactShape = $helper -match "'packHash', 'profileHash', 'chapterKey'"
$reportProductionShape = $serializer -match 'report\.put\("canonicalPackHash"' -and
    $serializer -match 'report\.put\("canonicalProfileHash"' -and
    $serializer -match 'report\.put\("bundleIdentity"'
$receiptProductionShape = $serializer -match 'receipt\.put\("manifestRef"' -and
    $serializer -match 'receipt\.put\("packRef"' -and
    $serializer -match 'receipt\.put\("profileRef"' -and
    $serializer -match 'receipt\.put\("bindingRef"'
$artifactContractMismatch = $helperArtifactShape -and $reportProductionShape -and $receiptProductionShape

$liveStart = $liveTest.IndexOf('public void authorizedFreshRawRunsOnlyWhenExplicitlyOptedIn()')
$liveEnd = $liveTest.IndexOf('private static void assertCommittedRawResult', $liveStart)
$liveMethod = if ($liveStart -ge 0 -and $liveEnd -gt $liveStart) {
    $liveTest.Substring($liveStart, $liveEnd - $liveStart)
} else { '' }
$postDispatchStatusEmitter = $liveMethod -match 'sendStatus\s*\('
$dispatchCallPresent = $liveMethod -match '\.dispatchRaw\s*\('

$result = [ordered]@{
    schemaVersion = 'p5e.readiness-reaudit.v1'
    scope = 'STATIC_SOURCE_AND_PACKET_ONLY_NO_DEVICE_NO_PROVIDER_NO_CREDENTIAL_ENVIRONMENT_READ'
    branch = (& git -C $repoRoot branch --show-current).Trim()
    head = (& git -C $repoRoot rev-parse HEAD).Trim()
    files = [ordered]@{
        command = [ordered]@{ path = $commandPath; sha256 = Get-Sha256 $commandPath }
        helper = [ordered]@{ path = $helperPath; sha256 = Get-Sha256 $helperPath }
        liveTest = [ordered]@{ path = $liveTestPath; sha256 = Get-Sha256 $liveTestPath }
        serializer = [ordered]@{ path = $serializerPath; sha256 = Get-Sha256 $serializerPath }
    }
    findings = @(
        [ordered]@{
            id = 'H1_HELPER_RUNTIME_HASH_UNBOUND'
            findingConfirmed = (-not $helperHashGuard)
            commandExecutesHelper = ($command -match '&\s*\$helperPath\s+-Dispatch')
            commandChecksExpectedHelperSha256BeforeExecution = [bool]$helperHashGuard
        },
        [ordered]@{
            id = 'H2_NO_EXECUTABLE_LIVE_READBACK_COLLECTOR'
            findingConfirmed = (-not $liveCollectorMode)
            helperParameterSets = @($parameterSets)
            collectReadbackParameterSetPresent = [bool]$liveCollectorMode
        },
        [ordered]@{
            id = 'H3_SYNTHETIC_ARTIFACT_CONTRACT_DIFFERS_FROM_PRODUCTION_SERIALIZER'
            findingConfirmed = [bool]$artifactContractMismatch
            helperUsesReducedPackHashProfileHashShape = [bool]$helperArtifactShape
            productionReportUsesCanonicalAndBundleFields = [bool]$reportProductionShape
            productionReceiptUsesReferenceFields = [bool]$receiptProductionShape
        },
        [ordered]@{
            id = 'H4_LIVE_METHOD_EMITS_NO_POST_DISPATCH_READBACK'
            findingConfirmed = ($dispatchCallPresent -and -not $postDispatchStatusEmitter)
            dispatchCallPresent = [bool]$dispatchCallPresent
            postDispatchStatusEmitterPresent = [bool]$postDispatchStatusEmitter
        }
    )
    allFourBlockersConfirmed = [bool]((-not $helperHashGuard) -and (-not $liveCollectorMode) -and
        $artifactContractMismatch -and $dispatchCallPresent -and (-not $postDispatchStatusEmitter))
    deviceActions = 0
    providerCalls = 0
    credentialReads = 0
    readyForOwnerDecision = $false
    readyForDispatch = $false
    p6Ready = $false
}

$result | ConvertTo-Json -Depth 8
