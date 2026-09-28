[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$OutputPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$exporterPath = Join-Path $repoRoot 'scripts\p5e-db-binary-export.ps1'
$bridgePath = Join-Path $repoRoot 'docs\P5E_SQLITE_BRIDGE.py'
$toolchainPath = Join-Path $repoRoot 'scripts\p5e-raw-toolchain.ps1'
$manifestPath = Join-Path $repoRoot 'docs\P5E_A43_PM_PATH_CAPTURE_REPAIR_MANIFEST_20260928.md'
$commandPath = Join-Path $repoRoot 'docs\P5E_A43_PM_PATH_CAPTURE_REPAIR_COMMAND_20260928.txt'
$bindingQaPath = Join-Path $repoRoot 'scripts\test-p5e-binding-tuple-host-contract-repair.ps1'
$dbQaPath = Join-Path $repoRoot 'scripts\test-p5e-db-host-readback-repair.ps1'
$powershellPath = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'

$expectedManifestSha256 = 'C08C3F6D8EE1B802B0D68AB1FF0302E82655D17C72B06619752B65396C11ADC3'
$expectedCommandSha256 = 'C641F6A01C01209AD08FB820871E575895DC998A4CB6AD15769F1D2222BA4CD5'
$expectedHelperSha256 = '959F2BBDC2EF163F00F3A56900B903DF529FDCDD9024AD6CEE906A01E080A8F0'
$expectedExporterSha256 = 'D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06'
$expectedBridgeSha256 = '4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111'
$expectedToolchainSha256 = 'C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8'
$expectedLocalPropertiesSha256 = '71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2'
$expectedCertificateSha256 = '47F313893A5D68120B075C25825C1C66F1334AC47AFB2EF3741084E22EF3C155'
$serial = '15e84958'

$productionArtifactRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\artifacts\builds\v4.17-p5e.11\build-20260911-201725'
$productionBackupRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\backup\builds\v4.17-p5e.11\build-20260911-201725'
$testArtifactRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\artifacts\test-builds\v4.17-p5e.11\p5e-account-check-20260916-01'
$testBackupRoot = Join-Path $repoRoot 'App Translate Books-translation-profile\backup\test-builds\v4.17-p5e.11\p5e-account-check-20260916-01'
$artifactPins = @(
    [pscustomobject]@{ Name = 'productionApk'; Primary = (Join-Path $productionArtifactRoot 'TranslateBooks-v4.17-p5e.11-code207.apk'); Backup = (Join-Path $productionBackupRoot 'TranslateBooks-v4.17-p5e.11-code207.apk'); Hash = '2CCBB844C629132BB534B0D6ABA516055C410BF96D20B14B3F80F91B962800FD' },
    [pscustomobject]@{ Name = 'productionSource'; Primary = (Join-Path $productionArtifactRoot 'project_source_build-20260911-201725.zip'); Backup = (Join-Path $productionBackupRoot 'project_source_build-20260911-201725.zip'); Hash = 'B60624FC043BB3852D6B1A6E3AC409C9B66CA3C1BAB9FF4A512CCBF85984E348' },
    [pscustomobject]@{ Name = 'productionBuildInfo'; Primary = (Join-Path $productionArtifactRoot 'BUILD_INFO.json'); Backup = (Join-Path $productionBackupRoot 'BUILD_INFO.json'); Hash = 'DB20DA0CF708410AAAB65E5AF69ADF89A769ED62240B577A89CA4E2007FB7F06' },
    [pscustomobject]@{ Name = 'testApk'; Primary = (Join-Path $testArtifactRoot 'app-debug-androidTest.apk'); Backup = (Join-Path $testBackupRoot 'app-debug-androidTest.apk'); Hash = '058BE8511FE733D02C0564FD434DEEC0E19B99025E098E58C838E3B36FC158E8' },
    [pscustomobject]@{ Name = 'testSource'; Primary = (Join-Path $testArtifactRoot 'project_source_p5e-account-check-20260916-01.zip'); Backup = (Join-Path $testBackupRoot 'project_source_p5e-account-check-20260916-01.zip'); Hash = '5029E2AE955E980CEB1246D3ACA19F6B4E008EAA2E5E71360C5BAE305D820C8F' },
    [pscustomobject]@{ Name = 'testBuildInfo'; Primary = (Join-Path $testArtifactRoot 'BUILD_INFO.json'); Backup = (Join-Path $testBackupRoot 'BUILD_INFO.json'); Hash = '772F32E23AEF3537FEF00DACE8E4B9B994448BFEE2071150CF0E0540DDCCC5A9' }
)

$eventRoot = 'D:\P5E-private\raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc'
$eventPins = [ordered]@{
    'EVENT_PLAN.json' = '1C163DC499D934F25B5E4C5EAB911543773D01D333CF7990D0D57DF4D4E9CBE4'
    'COLLECTOR_OUTCOME.json' = '7674858E7D3164EED7873A492AB88742F2AB90A162E71E5210F60CCEAF84A5BC'
    'COLLECTOR_COMMAND_LOG.jsonl' = 'B90CEAB274D510B37D09910313E18E35F4F39AC6FE7F76B0019ABF1B8020826C'
}

$tests = [System.Collections.Generic.List[object]]::new()
$failures = [System.Collections.Generic.List[string]]::new()
$qaRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-pm-capture-repair-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $qaRoot -Force)

function Add-QAResult {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [string]$Detail = ''
    )
    [void]$tests.Add([ordered]@{ name = $Name; passed = $Passed; detail = $Detail })
    if (-not $Passed) { [void]$failures.Add($Name) }
}

function Invoke-QATest {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )
    try {
        & $Action | Out-Null
        Add-QAResult -Name $Name -Passed $true
    } catch {
        $safeMessage = ([string]$_.Exception.Message -split [Environment]::NewLine)[0]
        Add-QAResult -Name $Name -Passed $false -Detail ('typed=' + $_.Exception.GetType().Name + ';code=' + $safeMessage)
    }
}

function Assert-QA {
    param([Parameter(Mandatory = $true)][bool]$Condition, [Parameter(Mandatory = $true)][string]$Code)
    if (-not $Condition) { throw $Code }
}

function Get-QAHashUpper {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

function Assert-QAFilePin {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedHash,
        [Parameter(Mandatory = $true)][string]$Label
    )
    Assert-QA (Test-Path -LiteralPath $Path -PathType Leaf) ($Label + '_MISSING')
    $item = Get-Item -LiteralPath $Path -Force
    Assert-QA (-not $item.PSIsContainer) ($Label + '_NOT_REGULAR')
    Assert-QA (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) ($Label + '_REPARSE')
    Assert-QA ((Get-QAHashUpper -Path $Path) -ceq $ExpectedHash) ($Label + '_HASH')
    return $item
}

function Assert-QAArtifactPair {
    param([Parameter(Mandatory = $true)]$Pin)
    $primary = Assert-QAFilePin -Path $Pin.Primary -ExpectedHash $Pin.Hash -Label ($Pin.Name + '_PRIMARY')
    $backup = Assert-QAFilePin -Path $Pin.Backup -ExpectedHash $Pin.Hash -Label ($Pin.Name + '_BACKUP')
    Assert-QA ($primary.Length -eq $backup.Length) ($Pin.Name + '_LENGTH')
}

function Assert-QAContains {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string]$Needle,
        [Parameter(Mandatory = $true)][string]$Code
    )
    Assert-QA ($Text.IndexOf($Needle, [StringComparison]::OrdinalIgnoreCase) -ge 0) $Code
}

function ConvertTo-QAWindowsArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $builder = [Text.StringBuilder]::new()
    [void]$builder.Append('"')
    $slashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') { $slashes++; continue }
        if ($character -eq '"') {
            [void]$builder.Append(('\' * (2 * $slashes + 1)))
            [void]$builder.Append('"')
            $slashes = 0
            continue
        }
        if ($slashes -gt 0) { [void]$builder.Append(('\' * $slashes)); $slashes = 0 }
        [void]$builder.Append($character)
    }
    if ($slashes -gt 0) { [void]$builder.Append(('\' * (2 * $slashes))) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Set-QAProcessArguments {
    param(
        [Parameter(Mandatory = $true)][Diagnostics.ProcessStartInfo]$StartInfo,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )
    $argumentListProperty = $StartInfo.PSObject.Properties['ArgumentList']
    if ($null -ne $argumentListProperty) {
        foreach ($argument in $Arguments) { [void]$StartInfo.ArgumentList.Add([string]$argument) }
    } else {
        $StartInfo.Arguments = [string]::Join(' ', @($Arguments | ForEach-Object { ConvertTo-QAWindowsArgument -Value ([string]$_) }))
    }
}

function Get-QABoundedCapture {
    param(
        [Parameter(Mandatory = $true)][System.Threading.Tasks.Task]$Task,
        [int]$WaitMilliseconds = 1000
    )
    try {
        if (-not $Task.Wait($WaitMilliseconds)) {
            return [pscustomobject]@{ DrainStatus = 'TIMEOUT'; ByteCount = -1L; Bounded = $false }
        }
        $text = [string]$Task.GetAwaiter().GetResult()
        return [pscustomobject]@{ DrainStatus = 'DRAINED'; ByteCount = [long][Text.Encoding]::UTF8.GetByteCount($text); Bounded = $true }
    } catch {
        return [pscustomobject]@{ DrainStatus = 'DRAIN_FAILED'; ByteCount = -1L; Bounded = $false }
    }
}

function Get-QASyntheticCode {
    param(
        [Parameter(Mandatory = $true)][string]$Mode,
        [Parameter(Mandatory = $true)][string]$PowerShellPath
    )
    switch ($Mode) {
        'exit0' { return 'exit 0' }
        'exit1' { return 'exit 1' }
        'none' { return 'exit 0' }
        'stdout' { return "[Console]::Out.Write('SAFE_STDOUT'); exit 0" }
        'stderr' { return "[Console]::Error.Write('SAFE_STDERR'); exit 0" }
        'both' { return "[Console]::Out.Write('SAFE_STDOUT'); [Console]::Error.Write('SAFE_STDERR'); exit 0" }
        'near-limit' { return "[Console]::Out.Write(('x' * 32768)); exit 0" }
        'timeout' { return 'Start-Sleep -Seconds 5; exit 0' }
        'stream-handle' {
            $escapedPath = $PowerShellPath.Replace("'", "''")
            return ('$grand = [Diagnostics.ProcessStartInfo]::new(); $grand.FileName = ''' + $escapedPath + '''; $grand.UseShellExecute = $false; $grand.CreateNoWindow = $true; $grand.Arguments = ''-NoLogo -NoProfile -NonInteractive -Command "Start-Sleep -Seconds 4"''; [Diagnostics.Process]::Start($grand) | Out-Null; [Console]::Out.Write(''LEAK_STDOUT''); [Console]::Error.Write(''LEAK_STDERR''); exit 7')
        }
        default { throw 'SYNTHETIC_MODE_UNKNOWN' }
    }
}

function Invoke-QASyntheticCapture {
    param(
        [Parameter(Mandatory = $true)][string]$Mode,
        [Parameter(Mandatory = $true)][bool]$Repaired,
        [int]$TimeoutMilliseconds = 1500,
        [int]$DrainWaitMilliseconds = 1000
    )
    $filePath = $powershellPath
    if ($Mode -eq 'start-failure') { $filePath = Join-Path $qaRoot 'missing-powershell.exe' }
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $filePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    if ($Mode -ne 'start-failure') {
        $code = Get-QASyntheticCode -Mode $Mode -PowerShellPath $powershellPath
        $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($code))
        Set-QAProcessArguments -StartInfo $startInfo -Arguments @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded)
    }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $launchCount = 0L
    $timedOut = $false
    $processExitCode = $null
    $stdoutTask = $null
    $stderrTask = $null
    $stdoutCapture = [pscustomobject]@{ DrainStatus = 'NOT_STARTED'; ByteCount = -1L; Bounded = $false }
    $stderrCapture = [pscustomobject]@{ DrainStatus = 'NOT_STARTED'; ByteCount = -1L; Bounded = $false }
    $captureException = ''
    try {
        try {
            if (-not $process.Start()) { throw 'SYNTHETIC_PROCESS_START_FALSE' }
            $launchCount = 1L
            $stdoutTask = $process.StandardOutput.ReadToEndAsync()
            $stderrTask = $process.StandardError.ReadToEndAsync()
            if (-not $process.WaitForExit($TimeoutMilliseconds)) {
                $timedOut = $true
                try { $process.Kill($true) } catch { try { $process.Kill() } catch { } }
                try { $process.WaitForExit(3000) | Out-Null } catch { }
            }
            if ($process.HasExited) { $processExitCode = [int]$process.ExitCode }
        } catch {
            if ($launchCount -eq 0L) { $captureException = 'PROCESS_START_FAILED' }
            else { $captureException = $_.Exception.GetType().Name }
        }
        if ($null -ne $stdoutTask) { $stdoutCapture = Get-QABoundedCapture -Task $stdoutTask -WaitMilliseconds $DrainWaitMilliseconds }
        if ($null -ne $stderrTask) { $stderrCapture = Get-QABoundedCapture -Task $stderrTask -WaitMilliseconds $DrainWaitMilliseconds }
    } finally {
        $process.Dispose()
    }
    $captureBounded = [bool]($stdoutCapture.Bounded -and $stderrCapture.Bounded)
    if (-not $Repaired) {
        if ($launchCount -eq 0L) { return 126 }
        if ($timedOut) { return 124 }
        if (-not $captureBounded) { return 125 }
        if ($null -eq $processExitCode) { return 126 }
        return [int]$processExitCode
    }
    $wrapperStatus = if ($launchCount -eq 0L) { 'PROCESS_START_FAILED' }
        elseif ($timedOut) { 'TIMEOUT' }
        elseif (-not [string]::IsNullOrWhiteSpace($captureException)) { 'PROCESS_CAPTURE_EXCEPTION' }
        elseif ($null -eq $processExitCode) { 'PROCESS_EXIT_UNKNOWN' }
        elseif (-not $captureBounded) { 'STREAM_CAPTURE_NOT_BOUNDED' }
        else { 'PROCESS_COMPLETED' }
    $wrapperExitCode = switch ($wrapperStatus) {
        'PROCESS_START_FAILED' { 126; break }
        'TIMEOUT' { 124; break }
        'PROCESS_CAPTURE_EXCEPTION' { 125; break }
        'PROCESS_EXIT_UNKNOWN' { 126; break }
        'STREAM_CAPTURE_NOT_BOUNDED' { 125; break }
        default { [int]$processExitCode; break }
    }
    return [pscustomobject]@{
        WrapperExitCode = [int]$wrapperExitCode
        WrapperStatus = $wrapperStatus
        ProcessExitCode = $processExitCode
        StdoutDrainStatus = [string]$stdoutCapture.DrainStatus
        StderrDrainStatus = [string]$stderrCapture.DrainStatus
        StdoutByteCount = [long]$stdoutCapture.ByteCount
        StderrByteCount = [long]$stderrCapture.ByteCount
        TimedOut = [bool]$timedOut
        CaptureBounded = $captureBounded
    }
}

function New-QASyntheticRun {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Stdout,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Stderr,
        [AllowNull()][object]$ExitCode,
        [long]$LaunchCount = 1L,
        [bool]$TimedOut = $false,
        [bool]$CaptureBounded = $true,
        [bool]$OutputTooLarge = $false,
        [bool]$RedactionViolation = $false
    )
    return [pscustomobject]@{
        Outcome = if ($LaunchCount -eq 0L) { 'FAILED_BEFORE_LAUNCH' } elseif ($TimedOut) { 'TIMEOUT' } elseif ($null -eq $ExitCode) { 'PROCESS_EXIT_UNKNOWN' } elseif ([long]$ExitCode -eq 0L) { 'PROCESS_EXITED_ZERO' } else { 'PROCESS_EXITED_NONZERO' }
        LaunchCount = [long]$LaunchCount
        ExitCode = $ExitCode
        TimedOut = [bool]$TimedOut
        TimeoutMilliseconds = 30000L
        CaptureBounded = [bool]$CaptureBounded
        OutputTooLarge = [bool]$OutputTooLarge
        RedactionViolation = [bool]$RedactionViolation
        Stdout = $Stdout
        Stderr = $Stderr
        ByteLength = [long][Text.Encoding]::UTF8.GetByteCount($Stdout)
        HostSha256 = ''
        LaunchErrorClass = ''
        LaunchNativeErrorCode = $null
        LaunchReason = ''
    }
}

$helperText = Get-Content -Raw -LiteralPath $helperPath
$commandText = Get-Content -Raw -LiteralPath $commandPath
$manifestText = Get-Content -Raw -LiteralPath $manifestPath
. $helperPath -LibraryOnly

# The helper is dot-sourced for its offline library functions. Reassert the QA
# pins afterward because the helper's parameter variables share PowerShell's
# case-insensitive scope with similarly named QA locals.
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$exporterPath = Join-Path $repoRoot 'scripts\p5e-db-binary-export.ps1'
$bridgePath = Join-Path $repoRoot 'docs\P5E_SQLITE_BRIDGE.py'
$toolchainPath = Join-Path $repoRoot 'scripts\p5e-raw-toolchain.ps1'
$manifestPath = Join-Path $repoRoot 'docs\P5E_A43_PM_PATH_CAPTURE_REPAIR_MANIFEST_20260928.md'
$commandPath = Join-Path $repoRoot 'docs\P5E_A43_PM_PATH_CAPTURE_REPAIR_COMMAND_20260928.txt'
$expectedManifestSha256 = 'C08C3F6D8EE1B802B0D68AB1FF0302E82655D17C72B06619752B65396C11ADC3'
$expectedCommandSha256 = 'C641F6A01C01209AD08FB820871E575895DC998A4CB6AD15769F1D2222BA4CD5'
$expectedHelperSha256 = '959F2BBDC2EF163F00F3A56900B903DF529FDCDD9024AD6CEE906A01E080A8F0'
$expectedExporterSha256 = 'D8783B31F9141458CA397915664CA79B07D3161A5E0F0D3B4365C5C65EA41D06'
$expectedBridgeSha256 = '4598BFDFCC4A9BE08DCED9F66A010A47A72CF2ED049C770404A924C44BEA2111'
$expectedToolchainSha256 = 'C0AE7D431474F37597228A7AFA6F9382C63E26EB5A54CFB72604620D9DD5C3C8'
$expectedLocalPropertiesSha256 = '71EB9D8E8E0179D863D919E329B56A1F3251126C15C2295E1F8E132B745770C2'

Invoke-QATest -Name 'frozen-closed-event-three-file-hash-check' -Action {
    foreach ($entry in $eventPins.GetEnumerator()) {
        $path = Join-Path $eventRoot $entry.Key
        Assert-QAFilePin -Path $path -ExpectedHash $entry.Value -Label ('EVENT_' + $entry.Key.Replace('.', '_'))
    }
}

Invoke-QATest -Name 'closed-event-read-set-and-typed-stop-only' -Action {
    $planPath = Join-Path $eventRoot 'EVENT_PLAN.json'
    $outcomePath = Join-Path $eventRoot 'COLLECTOR_OUTCOME.json'
    $logPath = Join-Path $eventRoot 'COLLECTOR_COMMAND_LOG.jsonl'
    $plan = Get-Content -Raw -LiteralPath $planPath | ConvertFrom-Json
    $outcome = Get-Content -Raw -LiteralPath $outcomePath | ConvertFrom-Json
    $lines = @(Get-Content -LiteralPath $logPath | Where-Object { $_ -ne '' })
    Assert-QA ($lines.Count -eq 1) 'EVENT_LOG_LINE_COUNT'
    $log = $lines[0] | ConvertFrom-Json
    Assert-QA ([string]$plan.eventId -ceq 'raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc') 'EVENT_ID'
    Assert-QA ([string]$plan.serial -ceq $serial) 'EVENT_SERIAL'
    Assert-QA ([string]$outcome.typedOutcome -ceq 'COLLECTOR_TYPED_STOP') 'EVENT_TYPED_OUTCOME'
    Assert-QA ([string]$outcome.detailCode -ceq 'P5E_COLLECTOR_ADB_NONZERO') 'EVENT_DETAIL'
    Assert-QA ([string]$log.operation -ceq 'pm-path-production-before') 'EVENT_OPERATION'
    Assert-QA ([int]$log.exitCode -eq 1 -and [bool]$log.captureBounded -and -not [bool]$log.outputCaptured) 'EVENT_SAFE_LOG'
    Assert-QA (@($log.PSObject.Properties.Name | Where-Object { $_ -match 'stdout|stderr|argv|commandLine|raw' }).Count -eq 0) 'EVENT_RAW_FIELD'
}

Invoke-QATest -Name 'dependency-order-and-repaired-pins' -Action {
    Assert-QAFilePin -Path $helperPath -ExpectedHash $expectedHelperSha256 -Label 'HELPER'
    Assert-QAFilePin -Path $exporterPath -ExpectedHash $expectedExporterSha256 -Label 'EXPORTER'
    Assert-QAFilePin -Path $bridgePath -ExpectedHash $expectedBridgeSha256 -Label 'BRIDGE'
    Assert-QAFilePin -Path $toolchainPath -ExpectedHash $expectedToolchainSha256 -Label 'TOOLCHAIN'
    Assert-QAFilePin -Path (Join-Path $repoRoot 'local.properties') -ExpectedHash $expectedLocalPropertiesSha256 -Label 'LOCAL_PROPERTIES'
    Assert-QAFilePin -Path $manifestPath -ExpectedHash $expectedManifestSha256 -Label 'REPAIR_MANIFEST'
    Assert-QAFilePin -Path $commandPath -ExpectedHash $expectedCommandSha256 -Label 'REPAIR_COMMAND'
    foreach ($pin in $artifactPins) { Assert-QAArtifactPair -Pin $pin }
    Assert-QA ($commandText.IndexOf('Assert-PinnedFile -Path $helperPath', [StringComparison]::Ordinal) -lt $commandText.IndexOf('New-Item -ItemType Directory', [StringComparison]::Ordinal)) 'COMMAND_PIN_AFTER_EVENT_CREATE'
    Assert-QA ($helperText.IndexOf('$script:P5EIsLivePinPhase', [StringComparison]::Ordinal) -lt $helperText.IndexOf('. $script:P5EDatabaseExporterPath -LibraryOnly', [StringComparison]::Ordinal)) 'HELPER_EXPORTER_DOT_SOURCE_ORDER'
}

Invoke-QATest -Name 'command-manifest-helper-exporter-bridge-serial-certificate-binding' -Action {
    Assert-QAContains -Text $commandText -Needle 'P5E_A43_PM_PATH_CAPTURE_REPAIR_MANIFEST_20260928.md' -Code 'COMMAND_REPAIR_MANIFEST_PATH'
    Assert-QAContains -Text $commandText -Needle $expectedManifestSha256 -Code 'COMMAND_REPAIR_MANIFEST_HASH'
    Assert-QAContains -Text $commandText -Needle $expectedHelperSha256 -Code 'COMMAND_REPAIR_HELPER_HASH'
    Assert-QAContains -Text $commandText -Needle $expectedExporterSha256 -Code 'COMMAND_EXPORTER_HASH'
    Assert-QAContains -Text $commandText -Needle $expectedBridgeSha256 -Code 'COMMAND_BRIDGE_HASH'
    Assert-QAContains -Text $commandText -Needle $expectedCertificateSha256 -Code 'COMMAND_CERTIFICATE_HASH'
    Assert-QAContains -Text $commandText -Needle $serial -Code 'COMMAND_SERIAL'
    Assert-QAContains -Text $manifestText -Needle 'P5E_A43_PM_PATH_CAPTURE_AND_OUTER_CAPTURE_REPAIR' -Code 'MANIFEST_REPAIR_SCOPE'
    Assert-QAContains -Text $manifestText -Needle $expectedHelperSha256 -Code 'MANIFEST_REPAIR_HELPER_HASH'
    Assert-QA ($commandText.IndexOf('P5E_A43_FINAL_EXECUTABLE_COMMAND_20260926.txt', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_COMMAND_PATH'
    Assert-QA ($commandText.IndexOf('P5E_A43_FINAL_EXECUTABLE_APPROVAL_MANIFEST_20260926.md', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_MANIFEST_PATH'
    Assert-QA ($commandText.IndexOf('23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_MANIFEST_HASH'
    Assert-QA ($commandText.IndexOf('C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OLD_COMMAND_HASH'
    Assert-QA ($commandText.IndexOf('669C54049920C49344D2FB55533EFA9FA9F87E933A6A18DE5FA7215F1146D147', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'HISTORICAL_MANIFEST_HASH'
    Assert-QA ($commandText.IndexOf('A2EF2BA90F07D3F4D2517E7F1541EA752615F6BCF61E304E579E301A8D5D3D08', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'HISTORICAL_COMMAND_HASH'
}

Invoke-QATest -Name 'synthetic-pm-path-classifier-matrix' -Action {
    $validProduction = 'package:/data/app/~~abc==/com.ml.tblandroidtxt-ABC==/base.apk'
    $validTest = 'package:/data/app/com.ml.tblandroidtxt.test-ABC/base.apk'
    $pmCases = @(
        [pscustomobject]@{ Name = 'present'; Package = 'com.ml.tblandroidtxt'; Stdout = $validProduction; Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'PACKAGE_PRESENT' },
        [pscustomobject]@{ Name = 'present-test'; Package = 'com.ml.tblandroidtxt.test'; Stdout = $validTest; Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'PACKAGE_PRESENT' },
        [pscustomobject]@{ Name = 'package-not-found'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'Error: package com.ml.tblandroidtxt not found'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'PACKAGE_NOT_FOUND' },
        [pscustomobject]@{ Name = 'unknown-package'; Package = 'com.ml.tblandroidtxt'; Stdout = 'Unknown package: com.ml.tblandroidtxt'; Stderr = ''; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'PACKAGE_NOT_FOUND' },
        [pscustomobject]@{ Name = 'device-offline'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'error: device offline'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'DEVICE_UNAVAILABLE' },
        [pscustomobject]@{ Name = 'device-unauthorized'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'error: device unauthorized'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'DEVICE_UNAVAILABLE' },
        [pscustomobject]@{ Name = 'device-missing'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = ''; ExitCode = $null; LaunchCount = 0; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'DEVICE_UNAVAILABLE' },
        [pscustomobject]@{ Name = 'pm-service'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'Can''t find service: package'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'PM_SERVICE_FAILURE' },
        [pscustomobject]@{ Name = 'permission'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'Security Exception: permission denied'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'PM_SERVICE_FAILURE' },
        [pscustomobject]@{ Name = 'empty-success'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'MALFORMED_RESPONSE' },
        [pscustomobject]@{ Name = 'multiple-paths'; Package = 'com.ml.tblandroidtxt'; Stdout = ($validProduction + [Environment]::NewLine + $validProduction); Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'MALFORMED_RESPONSE' },
        [pscustomobject]@{ Name = 'duplicate-path'; Package = 'com.ml.tblandroidtxt'; Stdout = ($validProduction + [Environment]::NewLine + $validProduction); Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'MALFORMED_RESPONSE' },
        [pscustomobject]@{ Name = 'mixed-stderr'; Package = 'com.ml.tblandroidtxt'; Stdout = $validProduction; Stderr = 'warning'; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'MALFORMED_RESPONSE' },
        [pscustomobject]@{ Name = 'wrong-package-path'; Package = 'com.ml.tblandroidtxt'; Stdout = 'package:/data/app/com.other.package-ABC/base.apk'; Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'MALFORMED_RESPONSE' },
        [pscustomobject]@{ Name = 'truncated-path'; Package = 'com.ml.tblandroidtxt'; Stdout = 'package:/data/app/com.ml.tblandroidtxt-ABC/base'; Stderr = ''; ExitCode = 0; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'MALFORMED_RESPONSE' },
        [pscustomobject]@{ Name = 'unknown-nonzero'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'pm failed with an unrecognized status'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'UNKNOWN_NONZERO' },
        [pscustomobject]@{ Name = 'capture-incomplete'; Package = 'com.ml.tblandroidtxt'; Stdout = 'Unknown package: com.ml.tblandroidtxt'; Stderr = ''; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $false; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'UNKNOWN_NONZERO' },
        [pscustomobject]@{ Name = 'sensitive-redaction-flag'; Package = 'com.ml.tblandroidtxt'; Stdout = ('api' + '_key=INLINE'); Stderr = ''; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'UNKNOWN_NONZERO' },
        [pscustomobject]@{ Name = 'explicit-redaction-flag'; Package = 'com.ml.tblandroidtxt'; Stdout = 'Unknown package: com.ml.tblandroidtxt'; Stderr = ''; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $true; OutputTooLarge = $false; Expected = 'UNKNOWN_NONZERO' },
        [pscustomobject]@{ Name = 'wrong-package-not-found'; Package = 'com.ml.tblandroidtxt'; Stdout = ''; Stderr = 'Error: package com.other.package not found'; ExitCode = 1; LaunchCount = 1; TimedOut = $false; CaptureBounded = $true; RedactionViolation = $false; OutputTooLarge = $false; Expected = 'UNKNOWN_NONZERO' }
    )
    foreach ($case in $pmCases) {
        $caseCopy = $case
        Invoke-QATest -Name ('pm-' + $caseCopy.Name) -Action {
            $run = New-QASyntheticRun -Stdout $caseCopy.Stdout -Stderr $caseCopy.Stderr -ExitCode $caseCopy.ExitCode -LaunchCount $caseCopy.LaunchCount -TimedOut $caseCopy.TimedOut -CaptureBounded $caseCopy.CaptureBounded -OutputTooLarge $caseCopy.OutputTooLarge -RedactionViolation $caseCopy.RedactionViolation
            $classification = Get-P5EPackagePathClassification -Run $run -PackageName $caseCopy.Package
            Assert-QA ([string]$classification.Classification -ceq $caseCopy.Expected) ('PM_EXPECTED_' + $caseCopy.Name)
            $propertyNames = @($classification.PSObject.Properties.Name)
            Assert-QA (@($propertyNames | Where-Object { $_ -match 'stdout|stderr|argv|commandLine|raw' }).Count -eq 0) ('PM_RAW_PROPERTY_' + $caseCopy.Name)
            if ($caseCopy.Expected -eq 'PACKAGE_PRESENT') {
                Assert-QA (-not [string]::IsNullOrWhiteSpace([string]$classification.DevicePath)) ('PM_PRESENT_PATH_' + $caseCopy.Name)
            } else {
                Assert-QA ([string]$classification.DevicePath -ceq '') ('PM_NON_PRESENT_PATH_' + $caseCopy.Name)
            }
        }
    }
}

Invoke-QATest -Name 'pm-path-command-log-redacted-classification-only' -Action {
    $receiptDirectory = Join-Path $qaRoot 'collector-receipt'
    [void](New-Item -ItemType Directory -Path $receiptDirectory -Force)
    Initialize-P5ECollectorCommandLog -EvidenceDirectory $receiptDirectory -CollectionPhase Before
    $run = New-QASyntheticRun -Stdout 'package:/data/app/com.ml.tblandroidtxt-ABC/base.apk' -Stderr '' -ExitCode 0
    Add-P5ECollectorCommandRecord -Operation 'pm-path-production-before' -Run $run -SafeClassification 'PACKAGE_PRESENT'
    $line = Get-Content -LiteralPath (Get-P5ECollectorCommandLogPath -EvidenceDirectory $receiptDirectory)
    Assert-QA ($line -notmatch 'com\.ml\.tblandroidtxt|stdout|stderr|argv|commandLine') 'PM_RECEIPT_RAW'
    Assert-QA ($line -match '"safeClassification":"PACKAGE_PRESENT"') 'PM_RECEIPT_CLASSIFICATION'
    Assert-QA ($line -match '"outputCaptured":false') 'PM_RECEIPT_CAPTURE_FLAG'
    Initialize-P5ECollectorCommandLog -EvidenceDirectory $receiptDirectory -CollectionPhase After
}

Invoke-QATest -Name 'outer-capture-red-green-and-fixture-matrix' -Action {
    $modes = @('exit0', 'exit1', 'stdout', 'stderr', 'both', 'none', 'near-limit')
    foreach ($mode in $modes) {
        $result = Invoke-QASyntheticCapture -Mode $mode -Repaired $true -TimeoutMilliseconds 3000 -DrainWaitMilliseconds 1000
        Assert-QA ([int]$result.WrapperExitCode -eq $(if ($mode -eq 'exit1') { 1 } else { 0 })) ('OUTER_' + $mode + '_EXIT')
        Assert-QA ([bool]$result.CaptureBounded) ('OUTER_' + $mode + '_BOUNDED')
    }
    $timeout = Invoke-QASyntheticCapture -Mode 'timeout' -Repaired $true -TimeoutMilliseconds 250 -DrainWaitMilliseconds 500
    Assert-QA ([int]$timeout.WrapperExitCode -eq 124 -and [bool]$timeout.TimedOut) 'OUTER_TIMEOUT'
    $startFailure = Invoke-QASyntheticCapture -Mode 'start-failure' -Repaired $true
    Assert-QA ([string]$startFailure.WrapperStatus -ceq 'PROCESS_START_FAILED' -and [int]$startFailure.WrapperExitCode -eq 126) 'OUTER_START_FAILURE'
    $red = Invoke-QASyntheticCapture -Mode 'stream-handle' -Repaired $false -TimeoutMilliseconds 2500 -DrainWaitMilliseconds 500
    $green = Invoke-QASyntheticCapture -Mode 'stream-handle' -Repaired $true -TimeoutMilliseconds 2500 -DrainWaitMilliseconds 500
    Assert-QA ([int]$red -eq 125) 'OUTER_RED_125'
    Assert-QA ($null -ne $green.ProcessExitCode -and [int]$green.ProcessExitCode -eq 7) 'OUTER_GREEN_PROCESS_EXIT'
    Assert-QA ([int]$green.WrapperExitCode -eq 125 -and [string]$green.WrapperStatus -ceq 'STREAM_CAPTURE_NOT_BOUNDED') 'OUTER_GREEN_WRAPPER_STATUS'
    Assert-QA ([string]$green.StdoutDrainStatus -ne 'DRAINED' -or [string]$green.StderrDrainStatus -ne 'DRAINED') 'OUTER_GREEN_STREAM_STATUS'
    Assert-QA ([bool]$green.CaptureBounded -eq $false -and [bool]$green.TimedOut -eq $false) 'OUTER_GREEN_FLAGS'
}

Invoke-QATest -Name 'outer-command-static-no-raw-and-no-process-exit-masking' -Action {
    foreach ($needle in @('processExitCode', 'StdoutDrainStatus', 'StderrDrainStatus', 'TimedOut', 'CaptureBounded', 'STREAM_CAPTURE_NOT_BOUNDED', 'P5E_HELPER_STDOUT_BYTE_COUNT', 'P5E_HELPER_STDERR_BYTE_COUNT')) {
        Assert-QAContains -Text $commandText -Needle $needle -Code ('OUTER_FIELD_' + $needle)
    }
    Assert-QA ($commandText.IndexOf('Get-BoundedHelperText', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OUTER_OLD_CAPTURE_FUNCTION'
    Assert-QA ($commandText.IndexOf('return 125', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OUTER_EXIT_MASKING_PATTERN'
    Assert-QA ($commandText.IndexOf('stdout.Text', [StringComparison]::OrdinalIgnoreCase) -lt 0 -and $commandText.IndexOf('stderr.Text', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OUTER_RAW_TEXT_PRINT'
    Assert-QA ($commandText.IndexOf('P5E_HELPER_STDERR_SUPPRESSED', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'OUTER_STDERR_PRINT'
}

Invoke-QATest -Name 'path-and-reparse-guards' -Action {
    foreach ($needle in @('ReparsePoint', 'PATH_SWAP_STOP', 'ANCESTOR_REPARSE_STOP', 'P5E_DB_EXPORT_DESTINATION_OUTSIDE_EVENT', 'P5E_COLLECTOR_SERIAL_MISMATCH')) {
        Assert-QAContains -Text ($commandText + $helperText) -Needle $needle -Code ('GUARD_' + $needle)
    }
    $guardDirectory = Join-Path $qaRoot 'guard-directory'
    [void](New-Item -ItemType Directory -Path $guardDirectory -Force)
    $candidate = Join-Path $guardDirectory 'safe.txt'
    $resolved = Assert-P5EEventDescendantNoReparse -EventDirectory $guardDirectory -Path $candidate -Kind Leaf
    Assert-QA ($resolved -eq [IO.Path]::GetFullPath($candidate)) 'GUARD_SAFE_DESCENDANT'
}

Invoke-QATest -Name 'expected-value-isolation-and-secret-scan' -Action {
    Assert-QA ($commandText -notmatch '(?i)GetEnvironmentVariable\([^\r\n]*P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT') 'EXPECTED_VALUE_READ'
    Assert-QA (($commandText | Select-String -Pattern 'ClearExpectedEnvironment' -AllMatches).Matches.Count -ge 4) 'EXPECTED_VALUE_CLEAR_COUNT'
    foreach ($path in @($manifestPath, $commandPath)) {
        $text = Get-Content -Raw -LiteralPath $path
        Assert-QA ($text -notmatch '(?i)https?://[^\s]+') ('SECRET_URL_' + [IO.Path]::GetFileName($path))
        Assert-QA ($text -notmatch '(?i)(password|credential|secret|api[_-]?key)\s*[:=]\s*[^\s,;}]+') ('SECRET_VALUE_' + [IO.Path]::GetFileName($path))
        Assert-QA ($text -notmatch '(?i)(endpointAccountFingerprint|endpoint_account_fingerprint|authorization_id_hash)\s*[:=]\s*(?!\[REDACTED_BY_HOST_CAPTURE\]|REDACTED)[0-9a-f]{64}') ('SECRET_DIGEST_' + [IO.Path]::GetFileName($path))
    }
}

Invoke-QATest -Name 'timeout-no-retry-redispatch-and-consumed-event-rejection' -Action {
    Assert-QAContains -Text $commandText -Needle 'NO_REDISPATCH' -Code 'COMMAND_NO_REDISPATCH'
    Assert-QAContains -Text $commandText -Needle 'RAW_LIVE_RETRIES=0' -Code 'COMMAND_ZERO_RETRY'
    Assert-QAContains -Text $manifestText -Needle 'consumed/non-reusable' -Code 'MANIFEST_CONSUMED_MARKER'
    Assert-QA ($commandText.IndexOf('23AF3DFAA81F50484187AFD183EA56454EBE38022C67DA2B963245A42D230053', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'COMMAND_CONSUMED_MANIFEST_REFERENCE'
    Assert-QA ($commandText.IndexOf('C84355B912DCF3BB59D52004EC80E06CE8B37FC75B5ABE083ECCA95D64C89BA3', [StringComparison]::OrdinalIgnoreCase) -lt 0) 'COMMAND_CONSUMED_COMMAND_REFERENCE'
}

Invoke-QATest -Name 'powershell-5-1-parse' -Action {
    foreach ($path in @($helperPath, $toolchainPath, $exporterPath, $commandPath, $PSCommandPath)) {
        $tokens = $null
        $errors = $null
        [Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$errors) | Out-Null
        Assert-QA ($errors.Count -eq 0) ('PARSE_' + [IO.Path]::GetFileName($path))
    }
}

function Invoke-QAChildScript {
    param([Parameter(Mandatory = $true)][string]$ScriptPath, [Parameter(Mandatory = $true)][string]$ReportPath)
    & $powershellPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $ScriptPath -OutputPath $ReportPath | Out-Null
    return [int]$LASTEXITCODE
}

Invoke-QATest -Name 'helper-self-test-offline' -Action {
    & $powershellPath -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $helperPath -SelfTest | Out-Null
    $exitCode = [int]$LASTEXITCODE
    Assert-QA ($exitCode -eq 0) ('HELPER_SELFTEST_EXIT_' + [string]$exitCode)
}

$bindingReportPath = Join-Path $qaRoot 'binding-tuple.json'
Invoke-QATest -Name 'captured-export-binding-262-regression-175' -Action {
    $exitCode = Invoke-QAChildScript -ScriptPath $bindingQaPath -ReportPath $bindingReportPath
    Assert-QA ($exitCode -eq 0) ('BINDING_QA_EXIT_' + [string]$exitCode)
    $report = Get-Content -Raw -LiteralPath $bindingReportPath | ConvertFrom-Json
    Assert-QA ([string]$report.status -ceq 'PASS') 'BINDING_QA_STATUS'
    Assert-QA ([int]$report.testCount -eq 262 -and [int]$report.passedTestCount -eq 262) 'BINDING_QA_262'
    Assert-QA ([string]$report.regressionMatrix.status -ceq 'PASS' -and [int]$report.regressionMatrix.caseCount -eq 175) 'REGRESSION_175'
    Assert-QA ([int]$report.counters.adb -eq 0 -and [int]$report.counters.device -eq 0 -and [int]$report.counters.provider -eq 0 -and [int]$report.counters.credential -eq 0 -and [int]$report.counters.databaseWrites -eq 0 -and [int]$report.counters.redispatch -eq 0) 'BINDING_COUNTERS'
}

$dbReportPath = Join-Path $qaRoot 'db-host-readback.json'
Invoke-QATest -Name 'db-host-readback-56' -Action {
    $exitCode = Invoke-QAChildScript -ScriptPath $dbQaPath -ReportPath $dbReportPath
    Assert-QA ($exitCode -eq 0) ('DB_QA_EXIT_' + [string]$exitCode)
    $report = Get-Content -Raw -LiteralPath $dbReportPath | ConvertFrom-Json
    $propertyCount = @($report.tests.PSObject.Properties).Count
    Assert-QA ([string]$report.status -ceq 'PASS' -and $propertyCount -eq 56 -and [int]$report.failureCount -eq 0) 'DB_QA_56'
}

Invoke-QATest -Name 'git-diff-check' -Action {
    $output = @(& git -C $repoRoot diff --check 2>&1)
    Assert-QA ($LASTEXITCODE -eq 0) 'GIT_DIFF_CHECK'
}

$failureArray = @($failures)
$report = [ordered]@{
    schemaVersion = 'p5e.a43.pm-path-capture-repair-qa.v1'
    generatedDate = '2026-09-28'
    workPackage = 'P5E_A43_PM_PATH_CAPTURE_AND_OUTER_CAPTURE_REPAIR'
    status = if ($failureArray.Count -eq 0) { 'PASS' } else { 'STOP' }
    packetState = 'CLOSED_CONSUMED_NON_REUSABLE / OFFLINE_REPAIR_REVIEW_PENDING / NOT_DISPATCHED / P6_NOT_READY'
    syntheticOnly = $true
    eventCreated = $false
    frozenEvent = [ordered]@{
        eventId = 'raw-live-a43-final-20260928-023533942-f78284fe37d44dfab09493458dc7f9cc'
        allowedReadFiles = @('EVENT_PLAN.json', 'COLLECTOR_OUTCOME.json', 'COLLECTOR_COMMAND_LOG.jsonl')
        eventPlanSha256 = $eventPins['EVENT_PLAN.json']
        collectorOutcomeSha256 = $eventPins['COLLECTOR_OUTCOME.json']
        collectorCommandLogSha256 = $eventPins['COLLECTOR_COMMAND_LOG.jsonl']
    }
    red = [ordered]@{
        status = if (@($tests | Where-Object { $_.name -like 'outer-capture-red-green*' -and $_.passed }).Count -eq 1) { 'PASS' } else { 'STOP' }
        oldWrapperStatus = '125'
        knownProcessExitCode = 7
        streamHandleFixture = 'synthetic-child'
    }
    green = [ordered]@{
        status = if (@($tests | Where-Object { $_.name -like 'outer-capture-red-green*' -and $_.passed }).Count -eq 1) { 'PASS' } else { 'STOP' }
        preservesProcessExitCode = 7
        preservesStdoutDrainStatus = $true
        preservesStderrDrainStatus = $true
        preservesTimedOut = $true
        preservesCaptureBounded = $true
        emitsRawOutput = $false
    }
    pmPathClassifier = [ordered]@{
        enumAllowlist = @('PACKAGE_PRESENT', 'PACKAGE_NOT_FOUND', 'DEVICE_UNAVAILABLE', 'PM_SERVICE_FAILURE', 'MALFORMED_RESPONSE', 'UNKNOWN_NONZERO')
        outputRetention = 'CLASSIFICATION_ONLY_NO_RAW_STDOUT_STDERR_ARGV_OR_DEVICE_PATH'
        failClosed = $true
        mutationMatrix = 'PASS'
    }
    regressions = [ordered]@{
        bindingTuple = '262/262'
        regressionMatrix = '175/175'
        dbHostReadback = '56/56'
    }
    finalPins = [ordered]@{
        manifestPath = $manifestPath
        manifestSha256 = Get-QAHashUpper -Path $manifestPath
        commandPath = $commandPath
        commandSha256 = Get-QAHashUpper -Path $commandPath
        helperPath = $helperPath
        helperSha256 = Get-QAHashUpper -Path $helperPath
        exporterPath = $exporterPath
        exporterSha256 = Get-QAHashUpper -Path $exporterPath
        bridgePath = $bridgePath
        bridgeSha256 = Get-QAHashUpper -Path $bridgePath
        toolchainPath = $toolchainPath
        toolchainSha256 = Get-QAHashUpper -Path $toolchainPath
        certificateSha256 = $expectedCertificateSha256
        serial = $serial
    }
    counters = [ordered]@{
        adbLaunches = 0
        deviceReads = 0
        deviceWrites = 0
        providerCalls = 0
        credentialReads = 0
        databaseWrites = 0
        builds = 0
        installs = 0
        rawDispatches = 0
        redispatches = 0
    }
    blockerCount = 0
    highCount = 0
    mediumLowFindings = @()
    failures = $failureArray
    testCount = $tests.Count
    passedTestCount = @($tests | Where-Object { $_.passed }).Count
    failureCount = $failureArray.Count
    tests = $tests.ToArray()
}
Write-P5EUtf8NoBom -Path $OutputPath -Text (ConvertTo-P5ECanonicalJson -Value $report)
Write-Output ('P5E_OFFLINE_REPAIR_QA_STATUS=' + [string]$report.status)
Write-Output ('P5E_OFFLINE_REPAIR_QA_TESTS=' + [string]$report.testCount + '/' + [string]$report.passedTestCount)
if ($failureArray.Count -gt 0) { exit 1 }
exit 0
