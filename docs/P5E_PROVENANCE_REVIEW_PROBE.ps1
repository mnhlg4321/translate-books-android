# Offline provenance audit only. The helper's explicit probe parameter creates
# disposable synthetic inputs and never invokes adb, instrumentation, a
# provider, a database, or an environment credential.
param([string]$OutputRoot)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$helper = Join-Path $repo 'scripts\p5e-raw-live-supervisor.ps1'
$arguments = @('-NoProfile', '-NonInteractive', '-File', $helper, '-ProvenanceProbe')
if (-not [string]::IsNullOrWhiteSpace($OutputRoot)) { $arguments += @('-OutputRoot', $OutputRoot) }
& (Join-Path $PSHOME 'pwsh.exe') @arguments
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
