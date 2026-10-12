[CmdletBinding()]
param(
    [ValidateSet('emulator-5554')]
    [string]$Serial = 'emulator-5554',
    [ValidatePattern('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')]
    [string]$RunId = ([guid]::NewGuid().ToString('D').ToLowerInvariant()),
    [ValidateSet('L1_ONLY', 'L2_ONLY', 'L3_ONLY', 'L1_THEN_L2', 'CHAIN', 'API_V1_QUICK', 'API_V1_THOROUGH', 'V5_CHAT')]
    [string]$Mode = 'CHAIN',
    [string[]]$FixtureIds,
    [string[]]$RetainL1FixtureIds,
    [ValidatePattern('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')]
    [string]$L1SourceRunId,
    [switch]$Live,
    [switch]$NegativeGate,
    # Z3: by default a live group treats a typed refusal of the production engine as a measurement and continues;
    # this switch restores the older rule (stop the group after the first failed fixture).
    [switch]$StopOnFirstRefusal,
    [string]$ExpectedEndpointAccountFingerprint,
    [string]$ModelOverride,
    [ValidateSet('minimal', 'medium', 'high')]
    [string]$ReasoningEffort = 'minimal',
    [ValidatePattern('^[A-Za-z0-9._-]{3,100}$')]
    [string]$GroupId,
    [ValidateRange(0.01, 6.00)]
    [decimal]$GroupMaximumUsd = 0.50,
    # EDITORIAL_API_V1: the cap of one chapter; the run service checks it before every request
    [ValidateRange(0.01, 1.00)]
    [decimal]$ChapterCapUsd = 0.10,
    [string]$FixturesRoot = 'D:\P5E-private\p6-fixtures'
)

$ErrorActionPreference = 'Stop'
$RunId = $RunId.ToLowerInvariant()
$GroupId = if ([string]::IsNullOrWhiteSpace($GroupId)) { "$Mode-$RunId" } else { $GroupId }
$GroupCapText = $GroupMaximumUsd.ToString('0.00####', [Globalization.CultureInfo]::InvariantCulture)
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Invoke-AdbPush {
    param(
        [Parameter(Mandatory = $true)][string]$LocalPath,
        [Parameter(Mandatory = $true)][string]$RemotePath,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 turns adb's stderr progress line into a terminating error under Stop.
        $ErrorActionPreference = 'Continue'
        & adb -s $Serial push $LocalPath $RemotePath 2>&1 | Out-Null
        $pushExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($pushExitCode -ne 0) { throw $FailureMessage }
}

function Invoke-AdbPull {
    param(
        [Parameter(Mandatory = $true)][string]$RemotePath,
        [Parameter(Mandatory = $true)][string]$LocalPath,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 turns adb's stderr progress line into a terminating error under Stop.
        $ErrorActionPreference = 'Continue'
        & adb -s $Serial pull $RemotePath $LocalPath 2>&1 | Out-Null
        $pullExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($pullExitCode -ne 0) { throw $FailureMessage }
}

$ManifestPath = Join-Path $RepoRoot 'docs\P6_R0_FIXTURE_MANIFEST.json'
$ExpectedRoot = [IO.Path]::GetFullPath('D:\P5E-private\p6-fixtures').TrimEnd('\')
$FixturesRoot = [IO.Path]::GetFullPath($FixturesRoot).TrimEnd('\')
if ($FixturesRoot -ne $ExpectedRoot -or $FixturesRoot.Contains('6.FINAL')) {
    throw 'Fixture root must be the private P6 fixture directory, outside 6.FINAL.'
}
if ($NegativeGate -and ($Live -or $Mode -ne 'L1_ONLY' -or -not $FixtureIds -or $FixtureIds.Count -ne 1)) {
    throw 'The negative gate is an offline L1_ONLY check of exactly one fixture.'
}
if ($Live -and ($ExpectedEndpointAccountFingerprint -notmatch '^[0-9a-fA-F]{64}$')) {
    throw 'Live fixture mode requires the owner-supplied endpoint/account fingerprint.'
}
if ($GroupMaximumUsd -le 0 -or $GroupMaximumUsd -gt 6.00) { throw 'The P6 group cap must be within the approved per-group limit.' }
if ($Live -and $Mode -notin @('L1_ONLY', 'L3_ONLY', 'L1_THEN_L2', 'CHAIN', 'API_V1_QUICK', 'API_V1_THOROUGH', 'V5_CHAT')) {
    throw 'Live fixture mode requires a supported single group mode.'
}
if ($Live -and $Mode -eq 'L2_ONLY') {
    throw 'Live L2 requires the committed, reused G1 REPORT_L1; use L1_THEN_L2 with L1SourceRunId.'
}
if ($Live -and $Mode -eq 'L1_THEN_L2' -and [string]::IsNullOrWhiteSpace($L1SourceRunId)) {
    throw 'Live L1_THEN_L2 requires committed G1 L1 state; it will not rerun L1.'
}
if (-not [string]::IsNullOrWhiteSpace($L1SourceRunId)) {
    $L1SourceRunId = $L1SourceRunId.ToLowerInvariant()
    if ($Mode -ne 'L1_THEN_L2') { throw 'L1SourceRunId is only valid for L1_THEN_L2.' }
}
if ($RetainL1FixtureIds -and $Mode -ne 'L1_ONLY') {
    throw 'L1 state can be retained only by a successful L1_ONLY run.'
}
$PrivateParent = Split-Path -Parent $FixturesRoot
$RunRoot = Join-Path (Join-Path $PrivateParent 'p6-runs') $RunId
if (Test-Path -LiteralPath $RunRoot) { throw 'Run directory already exists; use a new RunId.' }

$DeviceInputRoot = "/data/local/tmp/p6-fixtures/$RunId"
$DeviceAppInputRoot = "files/p6-fixtures/$RunId"
$DeviceOutputRoot = "/sdcard/Android/data/com.ml.tblandroidtxt/files/p6-fixture-results/$RunId"
$DeviceGroupLedger = "/sdcard/Android/data/com.ml.tblandroidtxt/files/p6-spend-ledger-groups/$GroupId.jsonl"
$HostGroupLedger = Join-Path $RunRoot 'group-ledger-current.jsonl'
$GroupLedgerSnapshots = Join-Path $RunRoot 'group-ledger-snapshots'
$WorstCaseByMode = @{
    L1_ONLY = [decimal]::Parse('0.0794912', [Globalization.CultureInfo]::InvariantCulture)
    L2_ONLY = [decimal]::Parse('0.1094064', [Globalization.CultureInfo]::InvariantCulture)
    L3_ONLY = [decimal]::Parse('0.1094064', [Globalization.CultureInfo]::InvariantCulture)
    L1_THEN_L2 = [decimal]::Parse('0.1094064', [Globalization.CultureInfo]::InvariantCulture)
    CHAIN = [decimal]::Parse('0.298304', [Globalization.CultureInfo]::InvariantCulture)
    API_V1_QUICK = $ChapterCapUsd
    API_V1_THOROUGH = $ChapterCapUsd
    V5_CHAT = $ChapterCapUsd
}
$IsApiMode = $Mode -in @('API_V1_QUICK', 'API_V1_THOROUGH', 'V5_CHAT')
$RunnerClass = if ($IsApiMode) { 'EditorialApiV1FixtureRunnerInstrumentedTest' } else { 'EditorialP6FixtureRunnerInstrumentedTest' }
$SourceCommit = (& git -C $RepoRoot rev-parse HEAD).Trim()

function Get-GroupSpendState([decimal]$RequiredUsd, [string]$SnapshotName) {
    $RemoteTest = & adb -s $Serial shell test -f $DeviceGroupLedger 2>$null
    $HasDeviceLedger = $LASTEXITCODE -eq 0
    if ($HasDeviceLedger) {
        Invoke-AdbPull -RemotePath $DeviceGroupLedger -LocalPath $HostGroupLedger -FailureMessage 'Could not pull the durable group ledger to the host.'
    } elseif (Test-Path -LiteralPath $HostGroupLedger) {
        throw 'The previously retained group ledger is missing from the emulator.'
    }
    $Checker = Join-Path $RepoRoot 'scripts\p6\verify_spend_ledger.py'
    $CheckOutput = & py -3 $Checker --ledger $HostGroupLedger --group-id $GroupId `
        --maximum-usd $GroupCapText --required-usd $RequiredUsd.ToString('0.#######', [Globalization.CultureInfo]::InvariantCulture) --allow-empty 2>&1
    if ($LASTEXITCODE -ne 0) { throw ('Group spend precheck stopped: ' + ($CheckOutput -join ' ')) }
    $Snapshot = ($CheckOutput -join "`n") | ConvertFrom-Json
    if (-not [string]::IsNullOrWhiteSpace($SnapshotName)) {
        if (-not (Test-Path -LiteralPath $GroupLedgerSnapshots)) {
            New-Item -ItemType Directory -Path $GroupLedgerSnapshots | Out-Null
        }
        if (Test-Path -LiteralPath $HostGroupLedger) {
            $SnapshotPath = Join-Path $GroupLedgerSnapshots $SnapshotName
            Copy-Item -LiteralPath $HostGroupLedger -Destination $SnapshotPath
        }
    }
    return $Snapshot
}
$state = & adb -s $Serial get-state 2>&1
if ($LASTEXITCODE -ne 0 -or ($state -join '').Trim() -ne 'device') { throw 'The selected emulator is not online.' }
$installed = & adb -s $Serial shell pm path com.ml.tblandroidtxt 2>&1
if ($LASTEXITCODE -ne 0 -or -not ($installed -join "`n").Contains('package:')) {
    throw 'The P6 app is not installed on emulator-5554.'
}
$testInstalled = & adb -s $Serial shell pm path com.ml.tblandroidtxt.test 2>&1
if ($LASTEXITCODE -ne 0 -or -not ($testInstalled -join "`n").Contains('package:')) {
    throw 'The archived P6 instrumented test APK is not installed on emulator-5554.'
}

& py -3 (Join-Path $RepoRoot 'scripts\p6\verify_fixtures.py') $ManifestPath
if ($LASTEXITCODE -ne 0) { throw 'The private fixture set did not pass its frozen hash and leak checks.' }
& py -3 (Join-Path $RepoRoot 'scripts\p6\prepare_fixture_run.py') `
    --fixtures-root $FixturesRoot --manifest $ManifestPath --run-dir $RunRoot --run-id $RunId
if ($LASTEXITCODE -ne 0) { throw 'Could not prepare the label-free fixture payload.' }

$TransferRoot = Join-Path $RunRoot 'to-device'
$AvailableFixtureIds = @((Get-Content (Join-Path $RunRoot 'fixture-ids.json') -Raw | ConvertFrom-Json) | ForEach-Object { $_ })
if (-not $FixtureIds -or $FixtureIds.Count -eq 0) {
    $FixtureIds = $AvailableFixtureIds
} else {
    if (($FixtureIds.Count -ne @($FixtureIds | Select-Object -Unique).Count) -or
            (@($FixtureIds | Where-Object { $_ -notin $AvailableFixtureIds }).Count -gt 0)) {
        throw 'FixtureIds must be unique members of the frozen fixture set.'
    }
}
if ($RetainL1FixtureIds) {
    if ((@($RetainL1FixtureIds | Select-Object -Unique).Count -ne $RetainL1FixtureIds.Count) -or
            (@($RetainL1FixtureIds | Where-Object { $_ -notin $FixtureIds }).Count -gt 0)) {
        throw 'RetainL1FixtureIds must be unique fixtures selected for this L1_ONLY run.'
    }
}
$PromptGuard = Join-Path $RepoRoot 'scripts\p6\verify_prompt_inputs.py'
$PromptGuardArguments = @('--fixtures-root', $FixturesRoot, '--manifest', $ManifestPath,
    '--transfer-root', $TransferRoot, '--fixture-ids') + @($FixtureIds)
& py -3 $PromptGuard @PromptGuardArguments
if ($LASTEXITCODE -ne 0) { throw 'Host prompt-input guard refused the selected fixture sources.' }
$Failed = [System.Collections.Generic.List[string]]::new()
$MeasureRefusals = $Live -and -not $StopOnFirstRefusal
$Executed = [System.Collections.Generic.List[string]]::new()
$DecisionHistory = Join-Path $RunRoot 'group-decisions.jsonl'
$StopReason = ''
$Decisions = [System.Collections.Generic.List[object]]::new()
$LiveArguments = @()
$LiveArguments = @('-e', 'p6_group_id', $GroupId, '-e', 'p6_group_maximum_usd', $GroupCapText)
if ($IsApiMode) {
    $ChapterCapText = $ChapterCapUsd.ToString('0.00####', [Globalization.CultureInfo]::InvariantCulture)
    $LiveArguments += @('-e', 'p6_chapter_cap_usd', $ChapterCapText, '-e', 'p6_source_commit', $SourceCommit)
}
if ($IsApiMode -and -not [string]::IsNullOrWhiteSpace($ModelOverride)) {
    $LiveArguments += @('-e', 'p6_model_override', $ModelOverride)
}
if ($IsApiMode) {
    $LiveArguments += @('-e', 'p6_reasoning_effort', $ReasoningEffort)
}
if ($NegativeGate) { $LiveArguments += @('-e', 'p6_fake_invalid_l1', 'YES') }
if ($MeasureRefusals) { $LiveArguments += @('-e', 'p6_measure_refusals', 'YES') }
if ($Live) {
    $LiveArguments += @('-e', 'p6_fixture_live', 'YES', '-e',
        'p6_expected_endpoint_account_fingerprint', $ExpectedEndpointAccountFingerprint)
}
if (-not [string]::IsNullOrWhiteSpace($L1SourceRunId)) {
    $LiveArguments += @('-e', 'p6_l1_source_run_id', $L1SourceRunId)
}
try {
    & adb -s $Serial shell mkdir -p $DeviceInputRoot
    if ($LASTEXITCODE -ne 0) { throw 'Could not prepare emulator input storage.' }
    foreach ($FixtureId in $FixtureIds) {
        $RemoteFixture = "$DeviceInputRoot/$FixtureId"
        & adb -s $Serial shell mkdir -p $RemoteFixture
        if ($LASTEXITCODE -ne 0) { throw "Could not prepare input path for $FixtureId." }
        & adb -s $Serial shell run-as com.ml.tblandroidtxt mkdir -p "$DeviceAppInputRoot/$FixtureId"
        if ($LASTEXITCODE -ne 0) { throw "Could not prepare app-private input path for $FixtureId." }
        foreach ($Name in @('RAW.txt', 'DRAFT.txt', 'GLOSSARY.csv', 'PRONOUN.csv')) {
            Invoke-AdbPush -LocalPath (Join-Path (Join-Path $TransferRoot $FixtureId) $Name) -RemotePath "$RemoteFixture/$Name" -FailureMessage "Could not push a source file for $FixtureId."
            & adb -s $Serial shell run-as com.ml.tblandroidtxt cp "$RemoteFixture/$Name" "$DeviceAppInputRoot/$FixtureId/$Name"
            if ($LASTEXITCODE -ne 0) { throw "Could not stage a source file for $FixtureId." }
        }
        Invoke-AdbPush -LocalPath (Join-Path $TransferRoot "$FixtureId.runtime.json") -RemotePath "$DeviceInputRoot/$FixtureId.runtime.json" -FailureMessage "Could not push the sanitized runtime manifest for $FixtureId."
        & adb -s $Serial shell run-as com.ml.tblandroidtxt cp "$DeviceInputRoot/$FixtureId.runtime.json" "$DeviceAppInputRoot/$FixtureId.runtime.json"
        if ($LASTEXITCODE -ne 0) { throw "Could not stage the sanitized runtime manifest for $FixtureId." }
    }
    $Instrumentation = 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'
    $FixtureIndex = 0
    foreach ($FixtureId in $FixtureIds) {
        $FixtureIndex++
        $RequiredUsd = $WorstCaseByMode[$Mode]
        if ($MeasureRefusals) {
            # Z3: not enough budget left for a worst-case fixture is the group cap, a clean stop that is reported
            try {
                $BeforeSnapshot = Get-GroupSpendState $RequiredUsd ("{0:D3}-before-{1}.jsonl" -f $FixtureIndex, $FixtureId)
            } catch {
                $StopReason = 'GROUP_CAP_OR_SPEND_PRECHECK: ' + $_.Exception.Message
                break
            }
        } else {
            $BeforeSnapshot = Get-GroupSpendState $RequiredUsd ("{0:D3}-before-{1}.jsonl" -f $FixtureIndex, $FixtureId)
        }
        if ($BeforeSnapshot.pending -ne 0) { throw 'UNKNOWN provider cost blocks the next fixture in this group.' }
        $LogPath = Join-Path (Join-Path $RunRoot 'logs') "$FixtureId-instrumentation.txt"
        $FixtureArguments = @()
        if ($RetainL1FixtureIds -and $FixtureId -in $RetainL1FixtureIds) {
            $FixtureArguments += @('-e', 'p6_keep_l1_state', 'YES')
        }
        $Output = & adb -s $Serial shell am instrument -w `
            -e p6_fixture_run YES -e p6_run_id $RunId -e p6_fixture_id $FixtureId -e p6_mode $Mode @LiveArguments @FixtureArguments `
            -e class "com.ml.tblandroidtxt.$RunnerClass#runFixture" $Instrumentation 2>&1
        $CommandExit = $LASTEXITCODE
        [IO.File]::WriteAllText($LogPath, ($Output -join [Environment]::NewLine), [Text.UTF8Encoding]::new($false))
        $CombinedOutput = $Output -join "`n"
        if (($CommandExit -ne 0) -or $CombinedOutput.Contains('FAILURES!!!') -or $CombinedOutput.Contains('INSTRUMENTATION_FAILED')) {
            $Failed.Add($FixtureId)
        }
        $LocalResults = Join-Path $RunRoot 'results'
        if (-not (Test-Path -LiteralPath $LocalResults)) { New-Item -ItemType Directory -Path $LocalResults | Out-Null }
        try {
            Invoke-AdbPull -RemotePath "$DeviceOutputRoot/$FixtureId" -LocalPath $LocalResults -FailureMessage "Could not pull fixture output for $FixtureId."
        } catch {
            $Failed.Add($FixtureId)
        }
        $AfterSnapshot = Get-GroupSpendState ([decimal]0) ("{0:D3}-after-{1}.jsonl" -f $FixtureIndex, $FixtureId)
        if ($AfterSnapshot.pending -ne 0) { throw 'UNKNOWN provider cost stops the group immediately.' }
        if ($MeasureRefusals) {
            $StructuralPath = Join-Path (Join-Path (Join-Path $RunRoot 'results') $FixtureId) 'structural.json'
            $MetadataPath = Join-Path (Join-Path (Join-Path $RunRoot 'results') $FixtureId) 'run-metadata.json'
            $PolicyArguments = @('--instrumentation-exit', "$CommandExit", '--history', $DecisionHistory, '--fixture', $FixtureId)
            if (Test-Path -LiteralPath $StructuralPath) { $PolicyArguments += @('--structural', $StructuralPath) }
            if (Test-Path -LiteralPath $MetadataPath) { $PolicyArguments += @('--metadata', $MetadataPath) }
            # the next fixture needs a worst-case reservation; the precheck above reports the cap, the policy only sees "yes"
            $PolicyOutput = & py -3 (Join-Path $RepoRoot 'scripts\p6\group_policy.py') @PolicyArguments
            if ($LASTEXITCODE -ne 0) { throw 'The group policy could not judge the fixture.' }
            $Decision = ($PolicyOutput -join "`n") | ConvertFrom-Json
            $Decisions.Add($Decision)
            if ($Decision.outcome -ne 'INFRASTRUCTURE') { $Executed.Add($FixtureId) }
            if ($Decision.action -eq 'STOP') { $StopReason = $Decision.stopReason + ' after ' + $FixtureId; break }
        } elseif ($Failed.Contains($FixtureId)) { throw "Fixture execution failed; group stopped after $FixtureId." }
        else { $Executed.Add($FixtureId) }
    }
} finally {
    & adb -s $Serial shell rm -r $DeviceInputRoot 2>&1 | Out-Null
    & adb -s $Serial shell run-as com.ml.tblandroidtxt rm -r "files/p6-fixtures/$RunId" 2>&1 | Out-Null
}
if ($MeasureRefusals -and $Executed.Count -eq 0) { throw ('No fixture produced a measurement. ' + $StopReason) }
$VerifiedIds = if ($MeasureRefusals) { @($Executed) } else { @($FixtureIds) }
$VerifyArguments = @('--fixtures-root', $FixturesRoot, '--manifest', $ManifestPath,
    '--run-dir', $RunRoot, '--mode', $Mode, '--fixture-ids') + $VerifiedIds
if ($Live) { $VerifyArguments += '--live' }
if ($MeasureRefusals) { $VerifyArguments += '--measure-refusals' }
if ($NegativeGate) { $VerifyArguments += '--expect-invalid-l1' }
& py -3 (Join-Path $RepoRoot 'scripts\p6\verify_fixture_run.py') @VerifyArguments
if ($LASTEXITCODE -ne 0) { throw 'Fixture outputs, spend ledger, leak probes, or scorer validation failed.' }
if ($MeasureRefusals) {
    foreach ($Decision in $Decisions) {
        Write-Output ("fixture {0}: {1} {2} ({3})" -f $Decision.fixture, $Decision.outcome, $Decision.code, $Decision.why)
    }
    if ($StopReason) { throw ("Group stopped by the run rule: $StopReason") }
} elseif ($Failed.Count -gt 0) { throw ('Instrumented fixture failures: ' + ($Failed -join ', ')) }

Write-Output "Completed $Mode for $($Executed.Count) of $($FixtureIds.Count) fixtures. Private evidence: $RunRoot"
