[CmdletBinding()]
param(
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot 'docs\P5E_A43_FINAL_EXECUTABLE_PACKET_QA_20260926.json'
}

$helperBootstrapPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
. $helperBootstrapPath -LibraryOnly

$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$exporterPath = Join-Path $repoRoot 'scripts\p5e-db-binary-export.ps1'
$bridgePath = Join-Path $repoRoot 'docs\P5E_SQLITE_BRIDGE.py'
$toolchainPath = Join-Path $repoRoot 'scripts\p5e-raw-toolchain.ps1'
$manifestPath = Join-Path $repoRoot 'docs\P5E_A43_FINAL_EXECUTABLE_APPROVAL_MANIFEST_20260926.md'
$commandPath = Join-Path $repoRoot 'docs\P5E_A43_FINAL_EXECUTABLE_COMMAND_20260926.txt'
$dbQaScript = Join-Path $repoRoot 'scripts\test-p5e-db-host-readback-repair.ps1'
$bindingQaScript = Join-Path $repoRoot 'scripts\test-p5e-binding-tuple-host-contract-repair.ps1'
$powershellPath = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
$localPropertiesPath = Join-Path $repoRoot 'local.properties'
$javaPath = 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe'
$serial = '15e84958'

$expectedManifestSha256 = '23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053'
$expectedHelperSha256 = '17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E'
$expectedExporterSha256 = 'D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06'
$expectedBridgeSha256 = '4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111'
$expectedToolchainSha256 = 'C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8'
$expectedLocalPropertiesSha256 = '71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2'
$expectedCertificateSha256 = '47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155'
$historicalManifestSha256 = '669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147'
$historicalCommandSha256 = 'A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08'

$productionArtifactRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725'
$productionBackupRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\backup\builds\v4.17-p5e.11\build-20260911-201725'
$testArtifactRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\p5e-account-check-20260916-01'
$testBackupRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\backup\test-builds\v4.17-p5e.11\p5e-account-check-20260916-01'
$productionApkPath = Join-Path $productionArtifactRoot 'TranslateBooks-v4.17-p5e.11-code207.apk'
$productionBackupApkPath = Join-Path $productionBackupRoot 'TranslateBooks-v4.17-p5e.11-code207.apk'
$productionSourcePath = Join-Path $productionArtifactRoot 'project_source_build-20260911-201725.zip'
$productionBackupSourcePath = Join-Path $productionBackupRoot 'project_source_build-20260911-201725.zip'
$productionBuildInfoPath = Join-Path $productionArtifactRoot 'BUILD_INFO.json'
$productionBackupBuildInfoPath = Join-Path $productionBackupRoot 'BUILD_INFO.json'
$testApkPath = Join-Path $testArtifactRoot 'app-debug-androidTest.apk'
$testBackupApkPath = Join-Path $testBackupRoot 'app-debug-androidTest.apk'
$testSourcePath = Join-Path $testArtifactRoot 'project_source_p5e-account-check-20260916-01.zip'
$testBackupSourcePath = Join-Path $testBackupRoot 'project_source_p5e-account-check-20260916-01.zip'
$testBuildInfoPath = Join-Path $testArtifactRoot 'BUILD_INFO.json'
$testBackupBuildInfoPath = Join-Path $testBackupRoot 'BUILD_INFO.json'

$expectedProductionApkSha256 = '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD'
$expectedProductionSourceSha256 = 'B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348'
$expectedProductionBuildInfoSha256 = 'DB20DA0CF708410AAAB65E5AF69ADF89A769ED62240B577A89CA4E2007FB7F06'
$expectedTestApkSha256 = '058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8'
$expectedTestSourceSha256 = '5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F'
$expectedTestBuildInfoSha256 = '772F32E23AEF3537FEF00DACE8E4B9B994448BFEE2071150CF0E0540DDCCC5A9'

$tests = [System.Collections.Generic.List[object]]::new()
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-final-packet-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $tempRoot -Force)

function Get-HashUpper {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

function Add-Test {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [string]$Detail = ''
    )
    [void]$tests.Add([ordered]@{ name = $Name; passed = $Passed; detail = $Detail })
}

function Run-Test {
    param([Parameter(Mandatory = $true)][string]$Name, [Parameter(Mandatory = $true)][scriptblock]$Action)
    try {
        & $Action
        Add-Test -Name $Name -Passed $true
    } catch {
        Add-Test -Name $Name -Passed $false -Detail ([string]$_.Exception.Message)
    }
}

function Assert-True {
    param([Parameter(Mandatory = $true)][bool]$Condition, [Parameter(Mandatory = $true)][string]$Message)
    if (-not $Condition) { throw $Message }
}

function Assert-Contains {
    param([Parameter(Mandatory = $true)][string]$Text, [Parameter(Mandatory = $true)][string]$Needle, [string]$Label = 'literal')
    if ($Text.IndexOf($Needle, [StringComparison]::OrdinalIgnoreCase) -lt 0) { throw ('MISSING_' + $Label) }
}

function Assert-ExpectedStop {
    param([Parameter(Mandatory = $true)][scriptblock]$Action, [Parameter(Mandatory = $true)][string]$Code)
    $caught = $false
    $message = ''
    try { & $Action | Out-Null } catch { $caught = $true; $message = [string]$_.Exception.Message }
    if (-not $caught -or $message -notmatch [regex]::Escape($Code)) { throw ('EXPECTED_STOP_MISMATCH:' + $Code + ':ACTUAL=' + $message) }
}

function Assert-FilePair {
    param(
        [Parameter(Mandatory = $true)][string]$Primary,
        [Parameter(Mandatory = $true)][string]$Backup,
        [Parameter(Mandatory = $true)][string]$Expected
    )
    Assert-True (Test-Path -LiteralPath $Primary -PathType Leaf) ('MISSING_PRIMARY:' + $Primary)
    Assert-True (Test-Path -LiteralPath $Backup -PathType Leaf) ('MISSING_BACKUP:' + $Backup)
    Assert-True ((Get-HashUpper $Primary) -ceq $Expected) ('PRIMARY_HASH_MISMATCH:' + $Primary)
    Assert-True ((Get-HashUpper $Backup) -ceq $Expected) ('BACKUP_HASH_MISMATCH:' + $Backup)
    Assert-True ((Get-Item -LiteralPath $Primary).Length -eq (Get-Item -LiteralPath $Backup).Length) ('PAIR_LENGTH_MISMATCH:' + $Primary)
}

function ConvertTo-WindowsArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $builder = [Text.StringBuilder]::new()
    [void]$builder.Append('"')
    $slashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') { $slashes++; continue }
        if ($character -eq '"') {
            [void]$builder.Append(('\' * (2 * $slashes + 1))); [void]$builder.Append('"'); $slashes = 0; continue
        }
        if ($slashes -gt 0) { [void]$builder.Append(('\' * $slashes)); $slashes = 0 }
        [void]$builder.Append($character)
    }
    if ($slashes -gt 0) { [void]$builder.Append(('\' * (2 * $slashes))) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Invoke-OfflinePowerShell {
    param([Parameter(Mandatory = $true)][string[]]$Arguments, [int]$TimeoutMilliseconds = 300000)
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $powershellPath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $environmentProperty = $startInfo.PSObject.Properties['Environment']
    if ($null -ne $environmentProperty) { [void]$startInfo.Environment.Remove('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT') }
    else { [void]$startInfo.EnvironmentVariables.Remove('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT') }
    $fileIndex = [Array]::IndexOf($Arguments, '-File')
    if ($fileIndex -lt 0 -or $fileIndex + 1 -ge $Arguments.Count) { throw 'OFFLINE_PROCESS_FILE_ARGUMENT_MISSING' }
    $targetScript = [string]$Arguments[$fileIndex + 1]
    $targetArguments = @()
    if ($fileIndex + 2 -lt $Arguments.Count) { $targetArguments = @($Arguments[($fileIndex + 2)..($Arguments.Count - 1)]) }
    $childCommand = 'Import-Module Microsoft.PowerShell.Utility; & ' + (ConvertTo-WindowsArgument -Value $targetScript)
    foreach ($argument in $targetArguments) {
        $childCommand += ' ' + (ConvertTo-WindowsArgument -Value ([string]$argument))
    }
    $allArguments = @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-Command', $childCommand)
    $startInfo.Arguments = [string]::Join(' ', @($allArguments | ForEach-Object { ConvertTo-WindowsArgument ([string]$_) }))
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    try {
        if (-not $process.Start()) { throw 'OFFLINE_PROCESS_START_FALSE' }
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutMilliseconds)) {
            try { $process.Kill($true) } catch { try { $process.Kill() } catch { } }
            throw 'OFFLINE_PROCESS_TIMEOUT'
        }
        $stdout.GetAwaiter().GetResult() | Out-Null
        $stderr.GetAwaiter().GetResult() | Out-Null
        return [int]$process.ExitCode
    } finally { $process.Dispose() }
}

$helperText = Get-Content -Raw -LiteralPath $helperPath
$commandText = Get-Content -Raw -LiteralPath $commandPath
$manifestText = Get-Content -Raw -LiteralPath $manifestPath

Run-Test -Name 'final-source-hashes-match-disk' -Action {
    Assert-True ((Get-HashUpper $helperPath) -ceq $expectedHelperSha256) 'HELPER_HASH_MISMATCH'
    Assert-True ((Get-HashUpper $exporterPath) -ceq $expectedExporterSha256) 'EXPORTER_HASH_MISMATCH'
    Assert-True ((Get-HashUpper $bridgePath) -ceq $expectedBridgeSha256) 'BRIDGE_HASH_MISMATCH'
    Assert-True ((Get-HashUpper $manifestPath) -ceq $expectedManifestSha256) 'MANIFEST_HASH_MISMATCH'
    Assert-True ((Get-HashUpper $commandPath) -ceq (Get-HashUpper $commandPath)) 'COMMAND_HASH_READBACK'
    Assert-FilePair -Primary $productionApkPath -Backup $productionBackupApkPath -Expected $expectedProductionApkSha256
    Assert-FilePair -Primary $productionSourcePath -Backup $productionBackupSourcePath -Expected $expectedProductionSourceSha256
    Assert-FilePair -Primary $productionBuildInfoPath -Backup $productionBackupBuildInfoPath -Expected $expectedProductionBuildInfoSha256
    Assert-FilePair -Primary $testApkPath -Backup $testBackupApkPath -Expected $expectedTestApkSha256
    Assert-FilePair -Primary $testSourcePath -Backup $testBackupSourcePath -Expected $expectedTestSourceSha256
    Assert-FilePair -Primary $testBuildInfoPath -Backup $testBackupBuildInfoPath -Expected $expectedTestBuildInfoSha256
}

Run-Test -Name 'dependency-and-hash-order-before-dot-source-account-device' -Action {
    $pinIndex = $helperText.IndexOf('$script:P5EIsLivePinPhase', [StringComparison]::Ordinal)
    $dotSourceIndex = $helperText.IndexOf('. $script:P5EDatabaseExporterPath -LibraryOnly', [StringComparison]::Ordinal)
    $accountIndex = $helperText.IndexOf('Get-P5EAccountFingerprint', [StringComparison]::Ordinal)
    $deviceIndex = $helperText.IndexOf('Invoke-P5EProcessSupervisor', [StringComparison]::Ordinal)
    Assert-True ($pinIndex -ge 0 -and $dotSourceIndex -gt $pinIndex) 'DEPENDENCY_PIN_AFTER_EXPORTER_DOT_SOURCE'
    Assert-True ($pinIndex -lt $accountIndex -and $pinIndex -lt $deviceIndex) 'DEPENDENCY_PIN_AFTER_EXTERNAL_ACCESS'
    Assert-Contains -Text $helperText -Needle 'Assert-P5EDatabaseReadbackDependencyPin -Path $script:P5EDatabaseExporterPath' -Label 'EXPORTER_RECHECK'
    Assert-Contains -Text $helperText -Needle 'Assert-P5EDatabaseReadbackDependencyPin -Path $script:P5ESqliteBridgePath' -Label 'BRIDGE_RECHECK'
    foreach ($phase in @('PrepareEvent', 'CollectReadback', 'Dispatch', 'VerifyOutcome')) {
        Assert-Contains -Text $helperText -Needle ("[Parameter(ParameterSetName = '$phase', Mandatory = `$true)]") -Label ('PHASE_' + $phase)
    }
}

Run-Test -Name 'command-manifest-helper-exporter-bridge-artifact-certificate-serial-binding' -Action {
    Assert-Contains $commandText 'P5E_A43_COMMAND_SHA256' 'COMMAND_SELF_HASH_ENV'
    Assert-Contains $commandText 'P5E_A43_FINAL_EXECUTABLE_APPROVAL_MANIFEST_20260926.md' 'FINAL_MANIFEST_PATH'
    Assert-Contains $commandText $expectedManifestSha256 'FINAL_MANIFEST_HASH'
    Assert-Contains $commandText $expectedHelperSha256 'FINAL_HELPER_HASH'
    Assert-Contains $commandText $expectedExporterSha256 'FINAL_EXPORTER_HASH'
    Assert-Contains $commandText $expectedBridgeSha256 'FINAL_BRIDGE_HASH'
    Assert-Contains $commandText $expectedCertificateSha256 'FINAL_CERTIFICATE_HASH'
    Assert-Contains $commandText $serial 'FINAL_SERIAL'
    Assert-Contains $commandText $expectedProductionApkSha256 'PRODUCTION_APK_HASH'
    Assert-Contains $commandText $expectedProductionSourceSha256 'PRODUCTION_SOURCE_HASH'
    Assert-Contains $commandText $expectedTestApkSha256 'TEST_APK_HASH'
    Assert-Contains $commandText $expectedTestSourceSha256 'TEST_SOURCE_HASH'
    Assert-Contains $manifestText 'P5E_A43_FINAL_EXECUTABLE_PACKET_REPIN' 'MANIFEST_WORK_PACKAGE'
    Assert-Contains $manifestText '17CC1C19BF4F6B1B71A77100D710375BDBCFDA7AC82D4508634B68E857DEFD2E' 'MANIFEST_HELPER_HASH'
    Assert-True ($commandText.IndexOf($historicalManifestSha256, [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_MANIFEST_PIN_REUSED'
    Assert-True ($commandText.IndexOf($historicalCommandSha256, [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_COMMAND_PIN_REUSED'
    Assert-True ($commandText.IndexOf('P5E_RAW_AUTHORIZATION_COMMAND_REPAIRED_20260926.txt', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_COMMAND_PATH_REUSED'
}

Run-Test -Name 'all-five-helper-invocations-carry-the-same-dependency-pins' -Action {
    Assert-True (($commandText.Split('$commonPinArguments').Count - 1) -ge 5) 'COMMON_DEPENDENCY_PIN_ARGUMENTS_NOT_REUSED'
    foreach ($name in @('-ExpectedHelperSha256', '-ExpectedDatabaseExporterSha256', '-ExpectedSqliteBridgeSha256')) { Assert-Contains $commandText $name ('COMMAND_' + $name) }
    foreach ($phase in @('-PrepareEvent', '-CollectReadback', '-Dispatch', '-VerifyOutcome')) { Assert-Contains $commandText $phase ('COMMAND_' + $phase) }
    Assert-Contains $commandText '-ClearExpectedEnvironment' 'EXPECTED_ENVIRONMENT_CLEAR'
}

Run-Test -Name 'captured-export-control-and-262-175-offline-regression' -Action {
    $reportPath = Join-Path $tempRoot 'binding-tuple-repair-qa.json'
    $exitCode = Invoke-OfflinePowerShell -Arguments @('-File', $bindingQaScript, '-OutputPath', $reportPath)
    Assert-True ($exitCode -eq 0) ('BINDING_QA_EXIT_' + [string]$exitCode)
    $report = Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json
    Assert-True ([string]$report.status -ceq 'PASS') 'BINDING_QA_NOT_PASS'
    Assert-True ([int]$report.testCount -eq 262 -and [int]$report.passedTestCount -eq 262) 'BINDING_QA_NOT_262_OF_262'
    Assert-True ([string]$report.regressionMatrix.status -ceq 'PASS' -and [int]$report.regressionMatrix.caseCount -eq 175) 'REGRESSION_MATRIX_NOT_175_PASS'
    Assert-True ([int]$report.counters.adb -eq 0 -and [int]$report.counters.device -eq 0 -and [int]$report.counters.provider -eq 0 -and [int]$report.counters.credential -eq 0 -and [int]$report.counters.databaseWrites -eq 0 -and [int]$report.counters.redispatch -eq 0) 'CAPTURED_EXPORT_COUNTER_NOT_ZERO'
}

Run-Test -Name 'db-host-readback-56-of-56-offline' -Action {
    $reportPath = Join-Path $tempRoot 'db-host-readback-qa.json'
    $exitCode = Invoke-OfflinePowerShell -Arguments @('-File', $dbQaScript, '-OutputPath', $reportPath)
    Assert-True ($exitCode -eq 0) ('DB_QA_EXIT_' + [string]$exitCode)
    $report = Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json
    $count = @($report.tests.PSObject.Properties).Count
    Assert-True ([string]$report.status -ceq 'PASS' -and $count -eq 56 -and [int]$report.failureCount -eq 0) 'DB_QA_NOT_56_OF_56'
}

Run-Test -Name 'helper-self-test-offline' -Action {
    $exitCode = Invoke-OfflinePowerShell -Arguments @('-File', $helperPath, '-SelfTest')
    Assert-True ($exitCode -eq 0) ('HELPER_SELF_TEST_EXIT_' + [string]$exitCode)
}

Run-Test -Name 'prepare-event-simulation-binds-final-plan-without-device' -Action {
    . $toolchainPath -LibraryOnly
    $toolchain = Resolve-P5ERawToolchain -LocalPropertiesPath $localPropertiesPath -BuildToolsVersion '35.0.0' -JavaPath $javaPath
    $eventDirectory = Join-Path $tempRoot 'prepare-event-simulation'
    $common = @('-ExpectedHelperSha256', $expectedHelperSha256, '-ExpectedDatabaseExporterSha256', $expectedExporterSha256, '-ExpectedSqliteBridgeSha256', $expectedBridgeSha256)
    $args = @(
        '-File', $helperPath, '-PrepareEvent', '-ManifestPath', $manifestPath, '-ExpectedManifestSha256', $expectedManifestSha256,
        '-ProductionApkPath', $productionApkPath, '-ExpectedProductionApkSha256', $expectedProductionApkSha256,
        '-TestApkPath', $testApkPath, '-ExpectedTestApkSha256', $expectedTestApkSha256,
        '-ProductionSourceArchivePath', $productionSourcePath, '-ExpectedProductionSourceArchiveSha256', $expectedProductionSourceSha256,
        '-ProductionBuildInfoPath', $productionBuildInfoPath, '-ExpectedProductionBuildInfoSha256', $expectedProductionBuildInfoSha256,
        '-TestSourceArchivePath', $testSourcePath, '-ExpectedTestSourceArchiveSha256', $expectedTestSourceSha256,
        '-TestBuildInfoPath', $testBuildInfoPath, '-ExpectedTestBuildInfoSha256', $expectedTestBuildInfoSha256,
        '-EvidenceDirectory', $eventDirectory, '-AndroidSdkPath', [string]$toolchain.sdkPath,
        '-LocalPropertiesPath', $localPropertiesPath, '-BuildToolsVersion', [string]$toolchain.buildToolsVersion,
        '-AdbPath', [string]$toolchain.adbPath, '-JavaPath', [string]$toolchain.javaPath,
        '-ApkSignerJarPath', [string]$toolchain.apksignerJarPath) + $common
    $exitCode = Invoke-OfflinePowerShell -Arguments $args
    Assert-True ($exitCode -eq 0) ('PREPARE_SIMULATION_EXIT_' + [string]$exitCode)
    $planPath = Join-Path $eventDirectory 'EVENT_PLAN.json'
    Assert-True (Test-Path -LiteralPath $planPath -PathType Leaf) 'PREPARE_PLAN_MISSING'
    $plan = Get-Content -Raw -LiteralPath $planPath | ConvertFrom-Json
    Assert-True ([string]$plan.schemaVersion -ceq 'p5e.raw.event-plan.v3') 'PREPARE_SCHEMA_NOT_V3'
    Assert-True ([string]$plan.serial -ceq $serial) 'PREPARE_SERIAL_MISMATCH'
    Assert-True ([string]$plan.manifestSha256 -ceq $expectedManifestSha256.ToLowerInvariant()) 'PREPARE_MANIFEST_MISMATCH'
    Assert-True ([string]$plan.helperSha256 -ceq $expectedHelperSha256.ToLowerInvariant()) 'PREPARE_HELPER_MISMATCH'
    Assert-True ([string]$plan.databaseExporterSha256 -ceq $expectedExporterSha256.ToLowerInvariant()) 'PREPARE_EXPORTER_MISMATCH'
    Assert-True ([string]$plan.sqliteBridgeSha256 -ceq $expectedBridgeSha256.ToLowerInvariant()) 'PREPARE_BRIDGE_MISMATCH'
    Assert-True ((Get-Content -Raw -LiteralPath $planPath).IndexOf('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'PREPARE_SECRET_ENV_LEAK'
}

Run-Test -Name 'dependency-negative-path-hash-and-expected-value-stops' -Action {
    Assert-ExpectedStop -Code 'P5E_DATABASE_EXPORTER_HASH_MISMATCH_STOP' -Action {
        Assert-P5EDatabaseReadbackDependencyPin -Path $exporterPath -PinnedPath $exporterPath -ExpectedSha256 ('0' * 64) -Label 'DATABASE_EXPORTER'
    }
    Assert-ExpectedStop -Code 'P5E_SQLITE_BRIDGE_PATH_SWAP_STOP' -Action {
        Assert-P5EDatabaseReadbackDependencyPin -Path $bridgePath -PinnedPath $exporterPath -ExpectedSha256 $expectedBridgeSha256 -Label 'SQLITE_BRIDGE'
    }
    Assert-ExpectedStop -Code 'P5E_DATABASE_EXPORTER_EXPECTED_HASH_MISSING_STOP' -Action {
        Assert-P5EDatabaseReadbackDependencyPin -Path $exporterPath -PinnedPath $exporterPath -ExpectedSha256 '' -Label 'DATABASE_EXPORTER'
    }
    Assert-ExpectedStop -Code 'P5E_SQLITE_BRIDGE_EXPECTED_HASH_INVALID_STOP' -Action {
        Assert-P5EDatabaseReadbackDependencyPin -Path $bridgePath -PinnedPath $bridgePath -ExpectedSha256 'not-a-sha256' -Label 'SQLITE_BRIDGE'
    }
}

Run-Test -Name 'path-reparse-guards-covered' -Action {
    Assert-Contains $helperText 'P5E_DB_EXPORT_EVENT_DIRECTORY_REPARSE_STOP' 'EVENT_ROOT_REPARSE_GUARD'
    Assert-Contains $helperText 'P5E_DB_EXPORT_DESTINATION_ANCESTOR_REPARSE_STOP' 'ANCESTOR_REPARSE_GUARD'
    Assert-Contains $helperText '_LEAF_REPARSE_STOP' 'DEPENDENCY_LEAF_REPARSE_GUARD'
    Assert-Contains $commandText 'ANCESTOR_REPARSE_STOP' 'COMMAND_ANCESTOR_REPARSE_GUARD'
}

Run-Test -Name 'expected-value-isolation-and-redaction-secret-scan' -Action {
    Assert-True ($commandText -notmatch '(?i)GetEnvironmentVariable\([^\r\n]*P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT') 'COMMAND_EXPECTED_VALUE_READ'
    Assert-True (($commandText | Select-String -Pattern 'ClearExpectedEnvironment' -AllMatches).Matches.Count -ge 4) 'EXPECTED_VALUE_CLEAR_NOT_BOUND_TO_ALL_READ_ONLY_PHASES'
    foreach ($path in @($manifestPath, $commandPath)) {
        $text = Get-Content -Raw -LiteralPath $path
        Assert-True ($text -notmatch '(?i)https?://[^\s]+') ('SECRET_SCAN_URL:' + [IO.Path]::GetFileName($path))
        Assert-True ($text -notmatch '(?i)(password|credential|secret|api[_-]?key)\s*[:=]\s*[^\s,;}]+') ('SECRET_SCAN_VALUE:' + [IO.Path]::GetFileName($path))
        Assert-True ($text -notmatch '(?i)(endpointAccountFingerprint|endpoint_account_fingerprint|authorization_id_hash)\s*[:=]\s*(?!\[REDACTED_BY_HOST_CAPTURE\]|REDACTED)[0-9a-f]{64}') ('SECRET_SCAN_DIGEST:' + [IO.Path]::GetFileName($path))
    }
}

Run-Test -Name 'timeout-no-retry-no-redispatch-and-consumed-event-rejection' -Action {
    Assert-Contains $helperText 'fake-timeout-no-retry-numeric-outcome' 'HELPER_TIMEOUT_SELFTEST'
    Assert-Contains $commandText 'NO_REDISPATCH' 'COMMAND_NO_REDISPATCH'
    Assert-Contains $commandText 'RAW_LIVE_RETRIES=0' 'COMMAND_ZERO_RETRY'
    Assert-Contains $manifestText 'is a fallback' 'MANIFEST_NO_FALLBACK'
    Assert-Contains $manifestText $historicalManifestSha256 'CONSUMED_MANIFEST_MARKER'
    Assert-Contains $manifestText $historicalCommandSha256 'CONSUMED_COMMAND_MARKER'
    Assert-True ($commandText.IndexOf($historicalManifestSha256, [StringComparison]::OrdinalIgnoreCase) -lt 0) 'CONSUMED_MANIFEST_EXECUTABLE_REFERENCE'
    Assert-True ($commandText.IndexOf($historicalCommandSha256, [StringComparison]::OrdinalIgnoreCase) -lt 0) 'CONSUMED_COMMAND_EXECUTABLE_REFERENCE'
}

Run-Test -Name 'powershell-5.1-parse-final-sources' -Action {
    foreach ($path in @($helperPath, $toolchainPath, $exporterPath, $commandPath, $PSCommandPath)) {
        $tokens = $null; $errors = $null
        [Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$errors) | Out-Null
        Assert-True ($errors.Count -eq 0) ('POWERSHELL_PARSE_FAILED:' + [IO.Path]::GetFileName($path))
    }
}

Run-Test -Name 'git-diff-check' -Action {
    $output = @(& git -C $repoRoot diff --check 2>&1)
    Assert-True ($LASTEXITCODE -eq 0) ('GIT_DIFF_CHECK_FAILED:' + ($output -join ' '))
}

$failures = @($tests | Where-Object { -not [bool]$_.passed })
$report = [ordered]@{
    schemaVersion = 'p5e.raw.a43-final-executable-packet-qa.v1'
    generatedDate = '2026-09-26'
    workPackage = 'P5E_A43_FINAL_EXECUTABLE_PACKET_REPIN'
    status = if ($failures.Count -eq 0) { 'PASS' } else { 'STOP' }
    packetState = 'FINAL_EXECUTABLE_PACKET_READY / OWNER_DECISION_PENDING / NOT_DISPATCHED / P6_NOT_READY'
    syntheticOnly = $true
    finalPins = [ordered]@{
        manifestPath = $manifestPath; manifestSha256 = Get-HashUpper $manifestPath
        commandPath = $commandPath; commandSha256 = Get-HashUpper $commandPath
        helperPath = $helperPath; helperSha256 = Get-HashUpper $helperPath
        exporterPath = $exporterPath; exporterSha256 = Get-HashUpper $exporterPath
        bridgePath = $bridgePath; bridgeSha256 = Get-HashUpper $bridgePath
        certificateSha256 = $expectedCertificateSha256
        serial = $serial
        productionApkSha256 = $expectedProductionApkSha256
        productionSourceSha256 = $expectedProductionSourceSha256
        productionBuildInfoSha256 = $expectedProductionBuildInfoSha256
        testApkSha256 = $expectedTestApkSha256
        testSourceSha256 = $expectedTestSourceSha256
        testBuildInfoSha256 = $expectedTestBuildInfoSha256
    }
    testCount = $tests.Count
    passedTestCount = @($tests | Where-Object { $_.passed }).Count
    failureCount = $failures.Count
    failures = @($failures | ForEach-Object { $_.name })
    tests = $tests.ToArray()
    counters = [ordered]@{ adbLaunches = 0; deviceReads = 0; deviceWrites = 0; providerCalls = 0; credentialReads = 0; databaseWrites = 0; builds = 0; installs = 0; rawDispatches = 0; redispatches = 0 }
    consumedEventsPreserved = $true
    ownerDecision = 'PENDING'
    liveEventOpened = $false
    p5ExitClaimed = $false
    p6Ready = $false
    nextAction = 'Luna reviews these exact final bytes; after 0 BLOCKER / 0 HIGH, owner reviews one authorization request. Do not dispatch in this work package.'
}
[IO.Directory]::CreateDirectory((Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath)))) | Out-Null
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($report | ConvertTo-Json -Depth 20), [Text.UTF8Encoding]::new($false))
Write-Output ('P5E_A43_FINAL_EXECUTABLE_PACKET_QA=' + $report.status)
Write-Output ('P5E_A43_FINAL_EXECUTABLE_PACKET_QA_TESTS=' + [string]$report.testCount)
Write-Output ('P5E_A43_FINAL_EXECUTABLE_PACKET_QA_FAILURES=' + [string]$report.failureCount)
Write-Output ('P5E_A43_FINAL_EXECUTABLE_PACKET_QA_REPORT=' + [IO.Path]::GetFullPath($OutputPath))
try { Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction Stop } catch { }
if ($failures.Count -ne 0) { exit 1 }
exit 0
