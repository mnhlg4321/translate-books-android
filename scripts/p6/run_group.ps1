[CmdletBinding()]
param(
    [ValidateSet('emulator-5554')]
    [string]$Serial = 'emulator-5554',
    [ValidatePattern('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')]
    [string]$RunId = ([guid]::NewGuid().ToString('D').ToLowerInvariant()),
    [ValidateSet('L1_ONLY', 'L2_ONLY', 'L3_ONLY', 'CHAIN')]
    [string]$Mode = 'CHAIN',
    [string]$FixturesRoot = 'D:\P5E-private\p6-fixtures'
)

$ErrorActionPreference = 'Stop'
$RunId = $RunId.ToLowerInvariant()
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$ManifestPath = Join-Path $RepoRoot 'docs\P6_R0_FIXTURE_MANIFEST.json'
$ExpectedRoot = [IO.Path]::GetFullPath('D:\P5E-private\p6-fixtures').TrimEnd('\')
$FixturesRoot = [IO.Path]::GetFullPath($FixturesRoot).TrimEnd('\')
if ($FixturesRoot -ne $ExpectedRoot -or $FixturesRoot.Contains('6.FINAL')) {
    throw 'Fixture root must be the private P6 fixture directory, outside 6.FINAL.'
}
$PrivateParent = Split-Path -Parent $FixturesRoot
$RunRoot = Join-Path (Join-Path $PrivateParent 'p6-runs') $RunId
if (Test-Path -LiteralPath $RunRoot) { throw 'Run directory already exists; use a new RunId.' }

$DeviceInputRoot = "/sdcard/Android/data/com.ml.tblandroidtxt/files/p6-fixtures/$RunId"
$DeviceOutputRoot = "/sdcard/Android/data/com.ml.tblandroidtxt/files/p6-fixture-results/$RunId"
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

& adb -s $Serial shell mkdir -p $DeviceInputRoot
if ($LASTEXITCODE -ne 0) { throw 'Could not prepare emulator input storage.' }
$TransferRoot = Join-Path $RunRoot 'to-device'
$FixtureIds = Get-Content (Join-Path $RunRoot 'fixture-ids.json') -Raw | ConvertFrom-Json
$Failed = [System.Collections.Generic.List[string]]::new()
foreach ($FixtureId in $FixtureIds) {
    $RemoteFixture = "$DeviceInputRoot/$FixtureId"
    & adb -s $Serial shell mkdir -p $RemoteFixture
    if ($LASTEXITCODE -ne 0) { throw "Could not prepare input path for $FixtureId." }
    foreach ($Name in @('RAW.txt', 'DRAFT.txt', 'GLOSSARY.csv', 'PRONOUN.csv')) {
        & adb -s $Serial push (Join-Path (Join-Path $TransferRoot $FixtureId) $Name) "$RemoteFixture/$Name" | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Could not push a source file for $FixtureId." }
    }
    & adb -s $Serial push (Join-Path $TransferRoot "$FixtureId.runtime.json") "$DeviceInputRoot/$FixtureId.runtime.json" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not push the sanitized runtime manifest for $FixtureId." }
}
$Permissions = & adb -s $Serial shell chmod -R a+rX $DeviceInputRoot 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not grant the app read access to the pushed fixture payloads: $($Permissions -join ' ')" }

$Instrumentation = 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'
foreach ($FixtureId in $FixtureIds) {
    $LogPath = Join-Path (Join-Path $RunRoot 'logs') "$FixtureId-instrumentation.txt"
    $Output = & adb -s $Serial shell am instrument -w `
        -e p6_fixture_run YES -e p6_run_id $RunId -e p6_fixture_id $FixtureId -e p6_mode $Mode `
        -e class com.ml.tblandroidtxt.EditorialP6FixtureRunnerInstrumentedTest#runFixture $Instrumentation 2>&1
    $CommandExit = $LASTEXITCODE
    [IO.File]::WriteAllText($LogPath, ($Output -join [Environment]::NewLine), [Text.UTF8Encoding]::new($false))
    $CombinedOutput = $Output -join "`n"
    if (($CommandExit -ne 0) -or $CombinedOutput.Contains('FAILURES!!!') -or $CombinedOutput.Contains('INSTRUMENTATION_FAILED')) {
        $Failed.Add($FixtureId)
    }
}

$LocalResults = Join-Path $RunRoot 'results'
New-Item -ItemType Directory -Path $LocalResults | Out-Null
& adb -s $Serial pull $DeviceOutputRoot $LocalResults | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Could not pull the private offline fixture outputs.' }
& py -3 (Join-Path $RepoRoot 'scripts\p6\verify_fixture_run.py') `
    --fixtures-root $FixturesRoot --manifest $ManifestPath --run-dir $RunRoot --mode $Mode
if ($LASTEXITCODE -ne 0) { throw 'Offline fixture outputs, spend ledger, leak probes, or scorer validation failed.' }
if ($Failed.Count -gt 0) { throw ('Instrumented fixture failures: ' + ($Failed -join ', ')) }

Write-Output "Completed $Mode for $($FixtureIds.Count) offline fixtures. Private evidence: $RunRoot"
