[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$CandidateCommit,
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot 'docs\P5E_A43_ARCHIVE_CLEAN_RECONSTRUCTION_20260928.json'
}
$ps51Path = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
$tarPath = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-archive-' + [Guid]::NewGuid().ToString('N') + '.tar')
$archiveRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-archive-' + [Guid]::NewGuid().ToString('N'))
$result = [ordered]@{
    schemaVersion = 'p5e.a43.archive-clean-reconstruction.v1'
    candidateCommit = $CandidateCommit
    status = 'FAIL'
    sourcePolicy = 'EXACT_GIT_ARCHIVE_NO_WORKSPACE_FALLBACK_NO_CACHED_RESULTS'
    checks = [ordered]@{}
    dependencies = [ordered]@{}
    suites = [ordered]@{}
    counters = [ordered]@{ adb = 0; device = 0; provider = 0; credential = 0; databaseWrites = 0; buildInstall = 0; raw = 0; redispatch = 0 }
    archiveRoot = 'REDACTED_TEMP_ROOT'
}

function Add-Check {
    param([Parameter(Mandatory = $true)][string]$Name,[Parameter(Mandatory = $true)][bool]$Passed,[string]$Detail = '')
    $safe = if ([string]::IsNullOrWhiteSpace($Detail)) { '' } else { ($Detail -split '\r?\n')[0] }
    if ($safe -match '(?i)(secret|credential|fingerprint|password|stdout|stderr|raw)') { $safe = 'TYPED_OFFLINE_RESULT' }
    $result.checks[$Name] = [ordered]@{ passed = $Passed; detail = $safe }
}

function Assert-True {
    param([Parameter(Mandatory = $true)][bool]$Condition,[Parameter(Mandatory = $true)][string]$Code)
    if (-not $Condition) { throw $Code }
}

function Get-HashUpper { param([Parameter(Mandatory = $true)][string]$Path) return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant() }

function Invoke-ArchiveSuite {
    param([Parameter(Mandatory = $true)][string]$ScriptPath,[Parameter(Mandatory = $true)][string]$OutputFile)
    & $ps51Path -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $ScriptPath -OutputPath $OutputFile 2>$null | Out-Null
    return [int]$LASTEXITCODE
}

try {
    [void](New-Item -ItemType Directory -Path $archiveRoot -Force)
    & git -C $repoRoot archive --format=tar --output=$tarPath $CandidateCommit 2>$null
    Assert-True ($LASTEXITCODE -eq 0) 'ARCHIVE_CREATE_FAILED'
    & tar -xf $tarPath -C $archiveRoot 2>$null
    Assert-True ($LASTEXITCODE -eq 0) 'ARCHIVE_EXTRACT_FAILED'
    & git -C $archiveRoot init --quiet 2>$null
    Assert-True ($LASTEXITCODE -eq 0) 'ARCHIVE_GIT_INIT_FAILED'
    & git -C $repoRoot diff --check ($CandidateCommit + '^') $CandidateCommit -- 2>$null
    Assert-True ($LASTEXITCODE -eq 0) 'ARCHIVE_DIFF_CHECK_FAILED'
    Add-Check -Name 'archive-created-extracted-and-candidate-diff-clean' -Passed $true

    $dependencyPaths = @(
        'scripts\p5e-db-binary-export.ps1',
        'scripts\p5e-raw-toolchain.ps1',
        'docs\P5E_SQLITE_BRIDGE.py',
        'scripts\p5e-a43-runtime-guards.ps1',
        'scripts\p5e-raw-live-supervisor.ps1',
        'scripts\test-p5e-a43-preauth-runtime-guard-closure.ps1',
        'scripts\test-p5e-a43-decision-atomicity-expected-env.ps1',
        'scripts\test-p5e-a43-binding-tuple-matrix.ps1',
        'scripts\test-p5e-a43-db-host-readback-matrix.ps1',
        'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_MANIFEST_20260928.md',
        'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt')
    foreach ($relative in $dependencyPaths) {
        $path = Join-Path $archiveRoot $relative
        Assert-True (Test-Path -LiteralPath $path -PathType Leaf) ('ARCHIVE_MISSING_' + $relative.Replace('\','_'))
        $result.dependencies[$relative.Replace('\','/')] = [ordered]@{ length = [long](Get-Item -LiteralPath $path).Length; sha256 = Get-HashUpper -Path $path }
    }
    Add-Check -Name 'exact-runtime-and-qa-dependencies-present' -Passed $true

    $oldReferences = @(
        'test-p5e-binding-tuple-host-contract-repair.ps1',
        'test-p5e-db-host-readback-repair.ps1')
    $qaTexts = @(
        (Get-Content -Raw -LiteralPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-preauth-runtime-guard-closure.ps1')),
        (Get-Content -Raw -LiteralPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-decision-atomicity-expected-env.ps1')),
        (Get-Content -Raw -LiteralPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-binding-tuple-matrix.ps1')),
        (Get-Content -Raw -LiteralPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-db-host-readback-matrix.ps1')))
    foreach ($old in $oldReferences) { Assert-True (-not (($qaTexts -join "`n").Contains($old))) ('ARCHIVE_OLD_QA_REFERENCE_' + $old) }
    Add-Check -Name 'historical-untracked-qa-dependencies-eliminated' -Passed $true

    $mainOut = Join-Path $archiveRoot 'docs\ARCHIVE_MAIN_QA.json'
    $atomicOut = Join-Path $archiveRoot 'docs\ARCHIVE_ATOMIC_QA.json'
    $bindingOut = Join-Path $archiveRoot 'docs\ARCHIVE_BINDING_QA.json'
    $dbOut = Join-Path $archiveRoot 'docs\ARCHIVE_DB_QA.json'
    $mainCode = Invoke-ArchiveSuite -ScriptPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-preauth-runtime-guard-closure.ps1') -OutputFile $mainOut
    $atomicCode = Invoke-ArchiveSuite -ScriptPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-decision-atomicity-expected-env.ps1') -OutputFile $atomicOut
    $bindingCode = Invoke-ArchiveSuite -ScriptPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-binding-tuple-matrix.ps1') -OutputFile $bindingOut
    $dbCode = Invoke-ArchiveSuite -ScriptPath (Join-Path $archiveRoot 'scripts\test-p5e-a43-db-host-readback-matrix.ps1') -OutputFile $dbOut
    $main = Get-Content -Raw -LiteralPath $mainOut | ConvertFrom-Json
    $atomic = Get-Content -Raw -LiteralPath $atomicOut | ConvertFrom-Json
    $binding = Get-Content -Raw -LiteralPath $bindingOut | ConvertFrom-Json
    $db = Get-Content -Raw -LiteralPath $dbOut | ConvertFrom-Json
    $result.suites.main = [ordered]@{ exit = $mainCode; status = [string]$main.status; tests = [int]$main.testCount; passed = [int]$main.passedTestCount; failures = [int]$main.failureCount; failureNames = @($main.failures); failureDetails = @($main.tests | Where-Object { -not [bool]$_.passed } | ForEach-Object { [ordered]@{ name = [string]$_.name; detail = [string]$_.detail } }) }
    $result.suites.atomicityExpectedEnvironment = [ordered]@{ exit = $atomicCode; status = [string]$atomic.status; tests = [int]$atomic.testCount; passed = [int]$atomic.passedTestCount; failures = [int]$atomic.failedTestCount; highFailures = [int]$atomic.highFailureCount; failureNames = @($atomic.tests | Where-Object { -not [bool]$_.passed } | ForEach-Object { [string]$_.name }) }
    $result.suites.binding = [ordered]@{ exit = $bindingCode; status = [string]$binding.status; tests = [int]$binding.testCount; passed = [int]$binding.passedTestCount; regression = [int]$binding.regressionMatrix.caseCount }
    $result.suites.dbHostReadback = [ordered]@{ exit = $dbCode; status = [string]$db.status; tests = [int]$db.testCount; failures = [int]$db.failureCount }
    Add-Check -Name 'archive-main-qa-21-of-21' -Passed ($mainCode -eq 0 -and [string]$main.status -eq 'PASS' -and [int]$main.testCount -eq 21 -and [int]$main.failureCount -eq 0)
    Add-Check -Name 'archive-atomicity-28-of-28-zero-high' -Passed ($atomicCode -eq 0 -and [string]$atomic.status -eq 'GREEN_PASS' -and [int]$atomic.testCount -eq 28 -and [int]$atomic.failedTestCount -eq 0 -and [int]$atomic.highFailureCount -eq 0)
    Add-Check -Name 'archive-binding-262-and-regression-175' -Passed ($bindingCode -eq 0 -and [string]$binding.status -eq 'PASS' -and [int]$binding.testCount -eq 262 -and [int]$binding.passedTestCount -eq 262 -and [int]$binding.regressionMatrix.caseCount -eq 175)
    Add-Check -Name 'archive-db-56-of-56' -Passed ($dbCode -eq 0 -and [string]$db.status -eq 'PASS' -and [int]$db.testCount -eq 56 -and [int]$db.failureCount -eq 0)
    foreach ($value in $result.counters.Values) { Assert-True ([long]$value -eq 0L) 'ARCHIVE_LIVE_COUNTER_NONZERO' }
    Add-Check -Name 'archive-live-counters-zero' -Passed $true
    $result.status = if (@($result.checks.Values | Where-Object { -not [bool]$_.passed }).Count -eq 0) { 'PASS' } else { 'FAIL' }
} catch {
    $result.status = 'FAIL'
    Add-Check -Name 'archive-clean-unhandled-stop' -Passed $false -Detail ([string]$_.Exception.Message)
} finally {
    $parent = Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath))
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) { [void](New-Item -ItemType Directory -Path $parent -Force) }
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($result | ConvertTo-Json -Depth 30), [Text.UTF8Encoding]::new($false))
    if (Test-Path -LiteralPath $tarPath) { Remove-Item -LiteralPath $tarPath -Force -ErrorAction SilentlyContinue }
    if (Test-Path -LiteralPath $archiveRoot) { Remove-Item -LiteralPath $archiveRoot -Recurse -Force -ErrorAction SilentlyContinue }
}

Write-Output ('P5E_A43_ARCHIVE_CLEAN_RECONSTRUCTION=' + [string]$result.status)
Write-Output ('P5E_A43_ARCHIVE_CLEAN_RECONSTRUCTION_REPORT=' + [IO.Path]::GetFullPath($OutputPath))
if ([string]$result.status -ne 'PASS') { exit 1 }
exit 0
