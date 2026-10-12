[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script = Join-Path $PSScriptRoot 'p5e-m4-reconcile-event.ps1'
if (-not (Test-Path -LiteralPath $script -PathType Leaf)) { throw 'M4_SCRIPT_MISSING' }

# PowerShell 5.1 parser check, then the self-test in a clean child process (no profile).
$tokens = $null
$errors = $null
[void][System.Management.Automation.Language.Parser]::ParseFile($script, [ref]$tokens, [ref]$errors)
if (@($errors).Count -ne 0) { throw ('M4_PARSE_ERRORS:' + (@($errors | ForEach-Object { $_.Message }) -join ' | ')) }

$child = (Get-Command powershell.exe).Source
$output = & $child -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $script -SelfTest 2>&1
$exitCode = $LASTEXITCODE
$text = (@($output) | ForEach-Object { [string]$_ }) -join "`n"
Write-Output $text
if ($exitCode -ne 0) { Write-Output ('M4 SELFTEST CHILD EXIT ' + $exitCode); exit 1 }
if ($text -notmatch 'SELFTEST PASS (\d+)/\1\b') { Write-Output 'M4 SELFTEST PASS LINE MISSING'; exit 1 }
Write-Output 'TEST PASS p5e-m4-reconcile-event'
exit 0
