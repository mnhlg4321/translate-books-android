# Read-only M0 observation for the M4 work request: get-state, pm path, dumpsys, run-as test -e for WAL/SHM,
# exec-out run-as cat of the main DB, pull of both base APKs. No install, no app launch, no writes on the device.
# Output: a new folder under D:\P5E-private named <Label>-<UTC> with M0_OBSERVATION.json and M0_COMMAND_LOG.json.
param([string]$Label = 'm4-m0')
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$serial = '15e84958'
$ts = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmssfff')
$dir = "D:\P5E-private\$Label-$ts"
if (Test-Path $dir) { throw 'exists' }
New-Item -ItemType Directory $dir | Out-Null
$log = New-Object System.Collections.ArrayList
function Run([string]$name, [string[]]$a, [string]$outFile = '') {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = ($a | ForEach-Object { if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ } }) -join ' '
    $psi.UseShellExecute = $false; $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
    $p = [System.Diagnostics.Process]::Start($psi)
    $errTask = $p.StandardError.ReadToEndAsync()
    $ms = New-Object System.IO.MemoryStream
    $p.StandardOutput.BaseStream.CopyTo($ms)
    $p.WaitForExit()
    $bytes = $ms.ToArray()
    if ($outFile) { [IO.File]::WriteAllBytes($outFile, $bytes) }
    [void]$log.Add([ordered]@{ name = $name; args = $psi.Arguments; exit = $p.ExitCode; stdoutBytes = $bytes.Length; stderr = $errTask.Result.Trim() })
    return [pscustomobject]@{ exit = $p.ExitCode; text = [Text.Encoding]::UTF8.GetString($bytes); bytes = $bytes }
}
$state = Run "get-state" @("-s", $serial, "get-state")
if ($state.exit -ne 0 -or $state.text.Trim() -ne "device") {
    Set-Content -Encoding UTF8 (Join-Path $dir "ABORTED_NO_DEVICE.txt") "get-state did not return device; no further command was run."
    throw "P5E_M0_DEVICE_NOT_CONNECTED"
}
$pmProd = Run 'pm-path-prod' @('-s', $serial, 'shell', 'pm', 'path', 'com.ml.tblandroidtxt')
$pmTest = Run 'pm-path-test' @('-s', $serial, 'shell', 'pm', 'path', 'com.ml.tblandroidtxt.test')
$dump = Run 'dumpsys-package-prod' @('-s', $serial, 'shell', 'dumpsys', 'package', 'com.ml.tblandroidtxt')
$wal = Run 'test-e-wal' @('-s', $serial, 'shell', 'run-as', 'com.ml.tblandroidtxt', 'sh', '-c', "'test -e /data/data/com.ml.tblandroidtxt/databases/tbl_android_txt.db-wal'")
$shm = Run 'test-e-shm' @('-s', $serial, 'shell', 'run-as', 'com.ml.tblandroidtxt', 'sh', '-c', "'test -e /data/data/com.ml.tblandroidtxt/databases/tbl_android_txt.db-shm'")
$dbFile = Join-Path $dir 'tbl_android_txt.db'
$db = Run 'exec-out-run-as-cat-main-db' @('-s', $serial, 'exec-out', 'run-as', 'com.ml.tblandroidtxt', 'cat', '/data/data/com.ml.tblandroidtxt/databases/tbl_android_txt.db') $dbFile
$prodPath = ($pmProd.text -split "`n" | Where-Object { $_ -like 'package:*' } | Select-Object -First 1).Trim().Substring(8)
$testPath = ($pmTest.text -split "`n" | Where-Object { $_ -like 'package:*' } | Select-Object -First 1).Trim().Substring(8)
$prodApk = Join-Path $dir 'prod-installed.apk'; $testApk = Join-Path $dir 'test-installed.apk'
[void](Run 'pull-prod' @('-s', $serial, 'pull', $prodPath, $prodApk))
[void](Run 'pull-test' @('-s', $serial, 'pull', $testPath, $testApk))
$vc = if ($dump.text -match 'versionCode=(\d+)') { $Matches[1] } else { '' }
$vn = if ($dump.text -match 'versionName=(\S+)') { $Matches[1] } else { '' }
$obs = [ordered]@{
    schema = 'p5e.m0.observation.v1'; observedUtc = (Get-Date).ToUniversalTime().ToString('o'); serial = $serial
    liveProviderCalls = 0; keyUsed = $false; deviceWrites = 0
    state = $state.text.Trim(); prodVersionCode = $vc; prodVersionName = $vn
    walExit = $wal.exit; shmExit = $shm.exit
    dbExportExit = $db.exit; dbExportBytes = $db.bytes.Length; dbSha256 = (Get-FileHash $dbFile -Algorithm SHA256).Hash
    prodApkSha256 = (Get-FileHash $prodApk -Algorithm SHA256).Hash; testApkSha256 = (Get-FileHash $testApk -Algorithm SHA256).Hash
}
$obs | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $dir 'M0_OBSERVATION.json')
$log | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $dir 'M0_COMMAND_LOG.json')
"DIR=$dir"
$obs | ConvertTo-Json
