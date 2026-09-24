[CmdletBinding()]
param(
    [string]$ResultPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$commandPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'p5e-account-check-device-command.ps1'))
$fixedTestApk = 'D:\P5E-private\p5e-account-check-install-replacement-20260917-113601720-80675f1e5c4746da97f0a884ee319472\installed-test-package.apk'
$fixedProductionApk = 'D:\P5E-private\a4-2-exact-preflight-device-20260914-185308591\production-after.apk'
$expectedTestHash = '058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8'
$expectedProductionHash = '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD'
$expectedCertificate = '47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155'
$syntheticExpected = 'E' * 64
if ([string]::IsNullOrWhiteSpace($ResultPath)) {
    $ResultPath = Join-Path $repoRoot 'docs\P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA_20260924_POST_STOP.json'
}
$ResultPath = [IO.Path]::GetFullPath($ResultPath)

$assertions = 0
$cases = [ordered]@{}

function Assert-QA {
    param([Parameter(Mandatory = $true)][bool]$Condition, [Parameter(Mandatory = $true)][string]$Code)
    $script:assertions++
    if (-not $Condition) { throw $Code }
}

function Get-HashQA { param([Parameter(Mandatory = $true)][string]$Path)
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
}

function Invoke-CommandChild {
    param(
        [Parameter(Mandatory = $true)][string]$Evidence,
        [string]$Mode = 'pass'
    )
    $oldMode = [Environment]::GetEnvironmentVariable('P5E_COMMAND_FAKE_MODE', 'Process')
    [Environment]::SetEnvironmentVariable('P5E_COMMAND_FAKE_MODE', $Mode, 'Process')
    try {
        $args = @(
            '-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $commandPath,
            '-EvidenceDirectory', $Evidence,
            '-AdbPath', $script:fakeAdb,
            '-ApkSignerPath', $script:fakeSigner,
            '-PowerShellPath', 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
        )
        $output = @(& powershell.exe @args 2>&1)
        return [pscustomobject]@{ Output = @($output | ForEach-Object { [string]$_ }); ExitCode = [int]$LASTEXITCODE; Evidence = $Evidence }
    } finally {
        [Environment]::SetEnvironmentVariable('P5E_COMMAND_FAKE_MODE', $oldMode, 'Process')
    }
}

function Invoke-CommandDefaultEvidenceChild {
    $oldMode = [Environment]::GetEnvironmentVariable('P5E_COMMAND_FAKE_MODE', 'Process')
    [Environment]::SetEnvironmentVariable('P5E_COMMAND_FAKE_MODE', 'pass', 'Process')
    try {
        $args = @(
            '-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $commandPath,
            '-AdbPath', $script:fakeAdb,
            '-ApkSignerPath', $script:fakeSigner,
            '-PowerShellPath', 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
        )
        $output = @(& powershell.exe @args 2>&1)
        return [pscustomobject]@{ Output = @($output | ForEach-Object { [string]$_ }); ExitCode = [int]$LASTEXITCODE }
    } finally {
        [Environment]::SetEnvironmentVariable('P5E_COMMAND_FAKE_MODE', $oldMode, 'Process')
    }
}

function Get-ReceiptQA {
    param([Parameter(Mandatory = $true)][string]$Evidence, [string]$Name = 'ACCOUNT_CHECK_DEVICE_COMMAND_RESULT.json')
    $path = Join-Path $Evidence $Name
    Assert-QA -Condition (Test-Path -LiteralPath $path -PathType Leaf) -Code ('MISSING_' + $Name)
    Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
}

function Assert-CommandStop {
    param([Parameter(Mandatory = $true)]$Run, [Parameter(Mandatory = $true)][string]$Outcome)
    Assert-QA -Condition ($Run.ExitCode -ne 0 -and ($Run.Output -join '|') -match 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND=STOP') -Code 'COMMAND_STOP_SIGNAL_MISSING'
    $receipt = Get-ReceiptQA -Evidence $Run.Evidence
    Assert-QA -Condition ($receipt.status -eq 'STOP' -and $receipt.typedOutcome -eq $Outcome) -Code ('COMMAND_STOP_WRONG_OUTCOME_' + [string]$receipt.typedOutcome)
    return $receipt
}

$root = Join-Path ([IO.Path]::GetTempPath()) ('p5e-account-command-qa-' + [Guid]::NewGuid().ToString('N'))
$envNames = @('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'P5E_COMMAND_FAKE_MODE', 'P5E_COMMAND_FAKE_LOG', 'P5E_COMMAND_TARGET_APK', 'P5E_COMMAND_TEST_APK', 'P5E_COMMAND_CERTIFICATE')
$oldExpected = [Environment]::GetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'Process')
$hadExpected = $null -ne $oldExpected
try {
    Assert-QA -Condition (Test-Path -LiteralPath $fixedTestApk -PathType Leaf) -Code 'FIXED_TEST_APK_MISSING'
    Assert-QA -Condition ((Get-HashQA $fixedTestApk) -eq $expectedTestHash) -Code 'FIXED_TEST_APK_HASH_DRIFT'
    Assert-QA -Condition (Test-Path -LiteralPath $fixedProductionApk -PathType Leaf) -Code 'FIXED_PRODUCTION_APK_MISSING'
    Assert-QA -Condition ((Get-HashQA $fixedProductionApk) -eq $expectedProductionHash) -Code 'FIXED_PRODUCTION_APK_HASH_DRIFT'
    New-Item -ItemType Directory -Path $root -ErrorAction Stop | Out-Null
    $script:fakeLog = Join-Path $root 'fake-adb.log'
    $script:fakeAdb = Join-Path $root 'fake-adb.cmd'
    $script:fakeSigner = Join-Path $root 'fake-apksigner.cmd'
    @'
@echo off
setlocal EnableExtensions
if defined P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT exit /b 97
if /i "%1"=="-s" (
  shift
  shift
)
if /i "%1"=="get-state" if not "%P5E_COMMAND_FAKE_LOG%"=="" echo get-state>>"%P5E_COMMAND_FAKE_LOG%"
if /i "%1"=="shell" if not "%P5E_COMMAND_FAKE_LOG%"=="" echo shell>>"%P5E_COMMAND_FAKE_LOG%"
if /i "%1"=="pull" if not "%P5E_COMMAND_FAKE_LOG%"=="" echo pull>>"%P5E_COMMAND_FAKE_LOG%"
if /i "%P5E_COMMAND_FAKE_MODE%"=="offline" if /i "%1"=="get-state" goto OFFLINE
if /i "%1"=="get-state" goto GETSTATE
if /i "%1"=="pull" goto PULL
if /i "%1"=="shell" if /i "%2"=="'sh'" goto INSTRUMENT
if /i "%1"=="shell" if /i "%2"=="dumpsys" goto DUMPSYS
if /i "%1"=="shell" if /i "%2"=="pm" goto PMPATH
exit /b 99
:OFFLINE
echo offline
exit /b 0
:GETSTATE
echo device
exit /b 0
:DUMPSYS
if /i "%4"=="com.ml.tblandroidtxt" goto DUMPSYS_TARGET
if /i "%4"=="com.ml.tblandroidtxt.test" goto DUMPSYS_TEST
exit /b 99
:DUMPSYS_TARGET
echo Package [com.ml.tblandroidtxt] (fake):
echo versionCode=207 minSdk=23
echo signatures:[abebea4b]
exit /b 0
:DUMPSYS_TEST
echo Package [com.ml.tblandroidtxt.test] (fake):
echo versionCode=1 minSdk=23
echo signatures:[abebea4b]
exit /b 0
:PMPATH
if /i not "%3"=="path" exit /b 99
if /i "%4"=="com.ml.tblandroidtxt" goto PMPATH_TARGET
if /i "%4"=="com.ml.tblandroidtxt.test" goto PMPATH_TEST
exit /b 99
:PMPATH_TARGET
echo package:/data/app/fake-production/base.apk
exit /b 0
:PMPATH_TEST
echo package:/data/app/fake-test/base.apk
exit /b 0
:PULL
if /i "%2"=="/data/app/fake-production/base.apk" copy /y "%P5E_COMMAND_TARGET_APK%" "%3" >nul
if /i "%2"=="/data/app/fake-production/base.apk" exit /b 0
if /i "%2"=="/data/app/fake-test/base.apk" copy /y "%P5E_COMMAND_TEST_APK%" "%3" >nul
if /i "%2"=="/data/app/fake-test/base.apk" exit /b 0
exit /b 99
:INSTRUMENT
echo INSTRUMENTATION_STATUS: class=com.ml.tblandroidtxt.EditorialP5EAccountCheckOnlyInstrumentedTest
echo INSTRUMENTATION_STATUS: test=ownerApprovedAccountCheckOnlyReturnsMatchOrMismatch
echo INSTRUMENTATION_STATUS: p5e.account.result=MATCH
echo INSTRUMENTATION_STATUS_CODE: 0
echo OK (1 test)
echo INSTRUMENTATION_CODE: -1
exit /b 0
'@ | Set-Content -LiteralPath $script:fakeAdb -NoNewline -Encoding ascii
    @'
@echo off
setlocal EnableExtensions
if defined P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT exit /b 97
echo Signer #1 certificate SHA-256 digest: %P5E_COMMAND_CERTIFICATE%
exit /b 0
'@ | Set-Content -LiteralPath $script:fakeSigner -NoNewline -Encoding ascii

    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $null, 'Process') }
    [Environment]::SetEnvironmentVariable('P5E_COMMAND_FAKE_LOG', $script:fakeLog, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_COMMAND_TARGET_APK', $fixedProductionApk, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_COMMAND_TEST_APK', $fixedTestApk, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_COMMAND_CERTIFICATE', $expectedCertificate, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $syntheticExpected, 'Process')

    $passEvidence = Join-Path $root 'pass-event'
    $pass = Invoke-CommandChild -Evidence $passEvidence -Mode 'pass'
    Assert-QA -Condition ($pass.ExitCode -eq 0 -and ($pass.Output -join '|') -match 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND=PASS') -Code 'COMMAND_PASS_MISSING'
    $passReceipt = Get-ReceiptQA -Evidence $passEvidence
    Assert-QA -Condition ($passReceipt.status -eq 'PASS' -and $passReceipt.typedOutcome -eq 'ACCOUNT_CHECK_COMPLETED_MATCH' -and
            $passReceipt.accountResult -eq 'MATCH' -and $passReceipt.accountLaunchCount -eq 1 -and
            $passReceipt.runnerProcessLaunchCount -eq 1 -and $passReceipt.preflightStatus -eq 'PASS') -Code 'COMMAND_PASS_RECEIPT_INVALID'
    Assert-QA -Condition ($passReceipt.providerCalls -eq 0 -and $passReceipt.dbWrites -eq 0 -and $passReceipt.rawDispatches -eq 0 -and
            $passReceipt.installAttempts -eq 0 -and (Test-Path -LiteralPath (Join-Path $passEvidence 'ACCOUNT_CHECK_ATTEMPT.marker') -PathType Leaf)) `
        -Code 'COMMAND_SCOPE_OR_MARKER_INVALID'
    $logAfterPass = @(Get-Content -LiteralPath $script:fakeLog)
    Assert-QA -Condition ($logAfterPass.Count -eq 8 -and ($logAfterPass[0] -match 'get-state') -and ($logAfterPass[7] -match 'shell')) -Code 'COMMAND_CALL_ORDER_INVALID'
    Assert-QA -Condition (($logAfterPass -join "`n") -notmatch $syntheticExpected) -Code 'EXPECTED_VALUE_IN_FAKE_ARG_LOG'
    Assert-QA -Condition ((Get-Content -LiteralPath (Join-Path $passEvidence 'ACCOUNT_CHECK_DEVICE_COMMAND_RESULT.json') -Raw) -notmatch $syntheticExpected) -Code 'EXPECTED_VALUE_IN_COMMAND_RECEIPT'
    $cases.pass_preflight_then_one_runner = 'PASS'

    $rerun = Invoke-CommandChild -Evidence $passEvidence -Mode 'pass'
    Assert-QA -Condition ($rerun.ExitCode -ne 0 -and ($rerun.Output -join '|') -match 'ACCOUNT_CHECK_DEVICE_COMMAND_EVIDENCE_NOT_EMPTY_STOP') -Code 'RERUN_NOT_BLOCKED'
    Assert-QA -Condition (@(Get-Content -LiteralPath $script:fakeLog).Count -eq $logAfterPass.Count) -Code 'RERUN_LAUNCHED_CHILD'
    $cases.repeated_command_blocked_before_live_work = 'PASS'

    $offlineEvidence = Join-Path $root 'offline-event'
    $offline = Invoke-CommandChild -Evidence $offlineEvidence -Mode 'offline'
    $offlineReceipt = Assert-CommandStop -Run $offline -Outcome 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP'
    Assert-QA -Condition ($offlineReceipt.runnerProcessLaunchCount -eq 0 -and $offlineReceipt.accountResult -eq 'NOT_PROVEN') -Code 'PREFLIGHT_STOP_LAUNCHED_RUNNER'
    Assert-QA -Condition (-not (Test-Path -LiteralPath (Join-Path $offlineEvidence 'ACCOUNT_CHECK_ATTEMPT.marker'))) -Code 'PREFLIGHT_STOP_CREATED_ACCOUNT_MARKER'
    $cases.preflight_stop_never_launches_runner = 'PASS'

    $defaultEvidence = 'D:\P5E-private\p5e-account-check-device-event-20260924-loader-contract-fix-01'
    $defaultEvidenceExists = Test-Path -LiteralPath $defaultEvidence -PathType Container
    $defaultEvidenceCount = @(Get-ChildItem -LiteralPath $defaultEvidence -Force).Count
    Assert-QA -Condition ($defaultEvidenceExists -and $defaultEvidenceCount -gt 0) -Code 'CLOSED_DEFAULT_EVENT_NOT_PRESENT'
    $beforeDefaultLogCount = @(Get-Content -LiteralPath $script:fakeLog).Count
    $defaultRun = Invoke-CommandDefaultEvidenceChild
    Assert-QA -Condition ($defaultRun.ExitCode -ne 0 -and ($defaultRun.Output -join '|') -match 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND=STOP' -and
            ($defaultRun.Output -join '|') -match 'ACCOUNT_CHECK_DEVICE_COMMAND_EVIDENCE_NOT_EMPTY_STOP') -Code 'DEFAULT_CLOSED_EVENT_NOT_STOPPED'
    Assert-QA -Condition (@(Get-Content -LiteralPath $script:fakeLog).Count -eq $beforeDefaultLogCount) -Code 'DEFAULT_CLOSED_EVENT_LAUNCHED_CHILD'
    $cases.default_closed_event_blocked_before_live_work = 'PASS'

    $source = Get-Content -LiteralPath $commandPath -Raw
    Assert-QA -Condition ($source -notmatch '(?i)ReadToEndAsync|Task\.WaitAll|WaitForExit\(\s*\)') -Code 'COMMAND_UNBOUNDED_OPERATION_PRESENT'
    Assert-QA -Condition ($source -match 'CreateNew' -and $source -match 'ClearInheritedEnvironmentVariableNames' -and
            $source -match 'ExpectedTargetApkSha256' -and $source -match 'preflightReceipt\.deviceReadAttempts') -Code 'COMMAND_GATES_OR_MARKER_MISSING'
    Assert-QA -Condition ($source -match '\$runnerArgs' -and $source -notmatch '\$runnerArgs[\s\S]{0,800}expectedProbe') -Code 'EXPECTED_VALUE_IN_RUNNER_ARGUMENT_CONSTRUCTION'
    Assert-QA -Condition (($source -match 'runnerComponent') -and ($source -match 'androidx\.test\.runner\.AndroidJUnitRunner')) -Code 'FULL_RUNNER_IDENTITY_NOT_PINNED'
    Assert-QA -Condition ($source -match '\$isLeaf' -and $source -match 'targetMetadataReason' -and
            $source -match "EvidenceDirectory = 'D:\\P5E-private\\p5e-account-check-device-event-20260924-loader-contract-fix-01'") -Code 'POST_STOP_GATES_NOT_PINNED'
    $cases.static_orchestration_guards = 'PASS'

    $result = [ordered]@{
        schemaVersion = 'p5e.account-check.device-command.qa.v1'
        status = 'PASS'
        totalAssertions = $assertions
        passedAssertions = $assertions
        failures = @()
        source = [ordered]@{
            commandSha256 = Get-HashQA -Path $commandPath
            preflightSha256 = Get-HashQA -Path (Join-Path $PSScriptRoot 'p5e-account-check-device-preflight.ps1')
            qaScriptSha256 = Get-HashQA -Path $MyInvocation.MyCommand.Path
        }
        host = [ordered]@{
            expectedHost = 'Windows PowerShell 5.1'
            actualHost = (& powershell.exe -NoLogo -NoProfile -NonInteractive -Command '$PSVersionTable.PSVersion.ToString()').Trim()
            actualHostProcess = $PSVersionTable.PSVersion.ToString()
        }
        cases = $cases
        syntheticOnly = $true
        adbInvocations = 0
        deviceActions = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
    }
    $parent = Split-Path -Parent $ResultPath
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) { New-Item -ItemType Directory -Path $parent -ErrorAction Stop | Out-Null }
    [IO.File]::WriteAllText($ResultPath, ($result | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA=PASS'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA_ASSERTIONS=' + $assertions + '/' + $assertions)
} catch {
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA=FAIL'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_COMMAND_QA_ERROR=' + [string]$_.Exception.Message)
    exit 1
} finally {
    if ($hadExpected) { [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $oldExpected, 'Process') }
    else { [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $null, 'Process') }
    foreach ($name in $envNames | Where-Object { $_ -ne 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT' }) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    if (Test-Path -LiteralPath $root) { Remove-Item -LiteralPath $root -Recurse -Force }
}
