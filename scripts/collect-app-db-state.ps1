[CmdletBinding()]
param(
    [string]$Serial,
    [string]$PackageName = 'com.ml.tblandroidtxt',
    [string]$DatabaseName = 'tbl_android_txt.db',
    [string]$AdbPath,
    [string]$OutputPath,
    # dot-sourced by the offline test: define the functions and do nothing else
    [switch]$DefinitionsOnly
)

# Read-only collector for the state of the app's database files (main, journal, WAL, SHM) on a debuggable build.
# Every file gets exactly one typed state; stderr text is never read as a hash:
#   PRESENT  existence confirmed and sha256sum exited 0 with a 64-hex digest
#   ABSENT   existence test said the file is not there (the probe prints PROBE_ABSENT)
#   ERROR    anything else (adb or run-as failure, sha256sum error text, malformed digest, empty output)
# An ERROR is "unknown", not "absent" and not "unchanged".

$script:ProbeAbsentMarker = 'PROBE_ABSENT'
$script:ProbeExitMarker = 'PROBE_EXIT='

function New-FileProbeCommand {
    param([Parameter(Mandatory)][string]$RelativePath)
    if ($RelativePath -notmatch '^[A-Za-z0-9._/-]+$') { throw "Unsafe probe path: $RelativePath" }
    # one shell program: existence first, then the digest and its own exit code
    "if [ -e $RelativePath ]; then sha256sum $RelativePath; echo ${script:ProbeExitMarker}`$?; else echo ${script:ProbeAbsentMarker}; fi"
}

function ConvertTo-FileProbe {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][int]$AdbExitCode,
        [AllowEmptyString()][AllowNull()][string]$Output
    )
    $text = if ($null -eq $Output) { '' } else { $Output }
    $lines = @($text -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' })
    $result = [ordered]@{ name = $Name; state = 'ERROR'; sha256 = $null; detail = '' }
    if ($AdbExitCode -ne 0) {
        $result.detail = "adb exit code $AdbExitCode"
        return [pscustomobject]$result
    }
    if ($lines.Count -eq 0) {
        $result.detail = 'empty probe output'
        return [pscustomobject]$result
    }
    if (@($lines | Where-Object { $_ -eq $script:ProbeAbsentMarker }).Count -eq 1 -and $lines.Count -eq 1) {
        $result.state = 'ABSENT'
        $result.detail = 'existence test: not present'
        return [pscustomobject]$result
    }
    $digestLines = @($lines | Where-Object { $_ -match '^[0-9a-fA-F]{64}\s+\S+$' })
    $exitLines = @($lines | Where-Object { $_ -like "$($script:ProbeExitMarker)*" })
    if ($digestLines.Count -eq 1 -and $exitLines.Count -eq 1 -and $exitLines[0] -eq "$($script:ProbeExitMarker)0" -and $lines.Count -eq 2) {
        $result.state = 'PRESENT'
        $result.sha256 = ($digestLines[0] -split '\s+')[0].ToLowerInvariant()
        $result.detail = 'sha256sum exit 0'
        return [pscustomobject]$result
    }
    $result.detail = 'unrecognised probe output: ' + ($lines -join ' | ')
    [pscustomobject]$result
}

function New-DbStateReport {
    param([Parameter(Mandatory)][object[]]$Probes)
    $errors = @($Probes | Where-Object { $_.state -eq 'ERROR' })
    [pscustomobject]@{
        schema = 1
        overall = if ($errors.Count -gt 0) { 'ERROR' } else { 'COMPLETE' }
        files = $Probes
    }
}

function Get-AppFileProbe {
    param([string]$Adb, [string]$DeviceSerial, [string]$Package, [string]$RelativePath)
    $command = New-FileProbeCommand -RelativePath $RelativePath
    $output = & $Adb -s $DeviceSerial shell run-as $Package sh -c "'$command'" 2>&1 | Out-String
    $adbExit = $LASTEXITCODE
    ConvertTo-FileProbe -Name ([IO.Path]::GetFileName($RelativePath)) -AdbExitCode $adbExit -Output $output
}

if ($DefinitionsOnly) { return }

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Serial)) { throw '-Serial is required; automatic device selection is disabled.' }
if ([string]::IsNullOrWhiteSpace($AdbPath)) {
    $sdk = $env:ANDROID_HOME
    if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_SDK_ROOT }
    if ([string]::IsNullOrWhiteSpace($sdk)) { throw 'Pass -AdbPath or set ANDROID_HOME.' }
    $AdbPath = Join-Path $sdk 'platform-tools\adb.exe'
}
$probes = foreach ($suffix in '', '-journal', '-wal', '-shm') {
    Get-AppFileProbe -Adb $AdbPath -DeviceSerial $Serial -Package $PackageName -RelativePath ("databases/$DatabaseName$suffix")
}
$report = New-DbStateReport -Probes @($probes)
$json = $report | ConvertTo-Json -Depth 5
if ($OutputPath) { [IO.File]::WriteAllText($OutputPath, $json + "`n", (New-Object Text.UTF8Encoding $false)) }
$json
if ($report.overall -ne 'COMPLETE') { exit 1 }
