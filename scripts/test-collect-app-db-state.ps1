$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'collect-app-db-state.ps1') -DefinitionsOnly

$failures = New-Object System.Collections.Generic.List[string]
function Assert-Equal($actual, $expected, [string]$label) {
    if ($actual -ne $expected) { $failures.Add("$label : expected [$expected] got [$actual]") }
}

$digest = ('ab12' * 16)
$mainOut = "$digest  databases/tbl_android_txt.db`r`nPROBE_EXIT=0`r`n"

# PRESENT: a real digest with its own exit 0
$p = ConvertTo-FileProbe -Name 'tbl_android_txt.db' -AdbExitCode 0 -Output $mainOut
Assert-Equal $p.state 'PRESENT' 'present state'
Assert-Equal $p.sha256 $digest 'present digest'
# upper-case digest is normalised to lower case
$p = ConvertTo-FileProbe -Name 'x' -AdbExitCode 0 -Output ($mainOut.ToUpperInvariant().Replace('PROBE_EXIT', 'PROBE_EXIT'))
Assert-Equal $p.state 'PRESENT' 'upper-case digest accepted'
Assert-Equal $p.sha256 $digest 'upper-case digest lowered'

# ABSENT: confirmed by the existence test, and only by it
$p = ConvertTo-FileProbe -Name 'tbl_android_txt.db-wal' -AdbExitCode 0 -Output "PROBE_ABSENT`r`n"
Assert-Equal $p.state 'ABSENT' 'absent state'
Assert-Equal $p.sha256 $null 'absent has no digest'

# ERROR cases: none of them may become PRESENT or ABSENT
$errorCases = [ordered]@{
    'sha256sum error text (the historical WAL/SHM value)' = @(0, 'sha256sum:')
    'sha256sum failure after the existence test' = @(0, "sha256sum: databases/x: Permission denied`r`nPROBE_EXIT=1")
    'digest present but exit code not zero' = @(0, "$digest  databases/x`r`nPROBE_EXIT=1")
    'digest without the exit marker' = @(0, "$digest  databases/x")
    'run-as refused' = @(1, 'run-as: package not debuggable: com.ml.tblandroidtxt')
    'run-as text with exit 0' = @(0, 'run-as: unknown package: com.example.nothing')
    'empty output' = @(0, '')
    'null output' = @(0, $null)
    'adb exit code without output' = @(255, '')
    'short digest' = @(0, "abcd  databases/x`r`nPROBE_EXIT=0")
    'absent marker mixed with a digest' = @(0, "PROBE_ABSENT`r`n$digest  databases/x`r`nPROBE_EXIT=0")
    'absent marker twice' = @(0, "PROBE_ABSENT`r`nPROBE_ABSENT")
}
foreach ($name in $errorCases.Keys) {
    $c = $errorCases[$name]
    $p = ConvertTo-FileProbe -Name 'f' -AdbExitCode $c[0] -Output $c[1]
    Assert-Equal $p.state 'ERROR' "error case: $name"
    Assert-Equal $p.sha256 $null "error case has no digest: $name"
}

# the probe program is one existence-guarded command and refuses unsafe paths
$cmd = New-FileProbeCommand -RelativePath 'databases/tbl_android_txt.db-wal'
if ($cmd -notmatch '^if \[ -e databases/tbl_android_txt\.db-wal \]; then sha256sum databases/tbl_android_txt\.db-wal; echo PROBE_EXIT=\$\?; else echo PROBE_ABSENT; fi$') { $failures.Add("probe command shape: $cmd") }
$threw = $false
try { New-FileProbeCommand -RelativePath "databases/x; rm -rf /" | Out-Null } catch { $threw = $true }
Assert-Equal $threw $true 'unsafe path refused'

# a report is COMPLETE only when no file is in ERROR; ABSENT is a confirmed state, not a failure
$ok = New-DbStateReport -Probes @(
    (ConvertTo-FileProbe -Name 'a' -AdbExitCode 0 -Output $mainOut),
    (ConvertTo-FileProbe -Name 'b' -AdbExitCode 0 -Output 'PROBE_ABSENT'))
Assert-Equal $ok.overall 'COMPLETE' 'report with present and absent'
$bad = New-DbStateReport -Probes @(
    (ConvertTo-FileProbe -Name 'a' -AdbExitCode 0 -Output $mainOut),
    (ConvertTo-FileProbe -Name 'b' -AdbExitCode 0 -Output 'sha256sum:'))
Assert-Equal $bad.overall 'ERROR' 'report with an error'

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}
"test-collect-app-db-state: all cases passed (present, absent, $($errorCases.Count) error cases, command shape, report)"
