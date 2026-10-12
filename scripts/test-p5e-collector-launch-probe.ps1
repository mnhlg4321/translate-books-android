[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$module=New-Module -ScriptBlock { param($p) . $p -LibraryOnly; Export-ModuleMember -Function Invoke-P5EReadOnlyProcess } -ArgumentList (Join-Path $PSScriptRoot 'p5e-raw-live-supervisor.ps1')
Import-Module $module -DisableNameChecking
try {
 $missing=Invoke-P5EReadOnlyProcess -FilePath 'p5e-nonexistent-synthetic-launch-probe.exe' -ArgumentList @() -TimeoutMilliseconds 2000
 $ok=Invoke-P5EReadOnlyProcess -FilePath 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe' -ArgumentList @('-NoProfile','-NonInteractive','-Command','exit 0') -TimeoutMilliseconds 5000
 if($missing.LaunchCount -ne 0 -or $missing.Outcome -ne 'FAILED_BEFORE_LAUNCH' -or $ok.LaunchCount -ne 1 -or $ok.ExitCode -ne 0){throw 'SYNTHETIC_LAUNCH_CONTRACT_FAIL'}
 $r=[ordered]@{status='PASS_DIAGNOSTIC_NOT_REPAIR';scope='SYNTHETIC_EXECUTABLE_ONLY';missingExecutableOutcome=$missing.Outcome;missingExecutableLaunchCount=$missing.LaunchCount;missingExecutableErrorClass=$missing.LaunchErrorClass;absoluteExecutableOutcome=$ok.Outcome;absoluteExecutableLaunchCount=$ok.LaunchCount;realAdbLaunches=0;deviceActions=0;providerCalls=0;historicalRootCauseProven=$false}
 [IO.File]::WriteAllText((Join-Path $root 'docs/P5E_COLLECTOR_LAUNCH_PROBE_20260925.json'),($r|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
 Write-Output 'SYNTHETIC_LAUNCH_DIAGNOSTIC_PASS'
} finally { Remove-Module -ModuleInfo $module -Force }