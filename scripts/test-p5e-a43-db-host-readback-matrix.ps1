[CmdletBinding()]
param(
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $repoRoot 'docs\P5E_A43_DB_HOST_READBACK_MATRIX_20260928.json'
}

$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
. $helperPath -LibraryOnly

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-db-matrix-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $tempRoot -Force)
$tests = [ordered]@{}
$counters = [ordered]@{
    adb = 0
    device = 0
    provider = 0
    credential = 0
    databaseWrites = 0
    build = 0
    install = 0
    raw = 0
    redispatch = 0
}

function Add-Test {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [string]$Detail = ''
    )
    if ($tests.Contains($Name)) { throw 'DUPLICATE_TEST_NAME' }
    $safeDetail = if ([string]::IsNullOrWhiteSpace($Detail)) { '' } else { ($Detail -split '\r?\n')[0] }
    if ($safeDetail -match '(?i)(secret|credential|fingerprint|password|authorization|raw|stderr|stdout|sql)') {
        $safeDetail = 'TYPED_OFFLINE_RESULT'
    }
    $tests[$Name] = [ordered]@{ passed = $Passed; detail = $safeDetail }
}

function Assert-True {
    param([Parameter(Mandatory = $true)][bool]$Condition,[Parameter(Mandatory = $true)][string]$Message)
    if (-not $Condition) { throw $Message }
}

function Invoke-ExpectedStop {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Action,
        [Parameter(Mandatory = $true)][string]$ExpectedCode
    )
    $caught = $false
    $actual = ''
    try { & $Action | Out-Null } catch { $caught = $true; $actual = [string]$_.Exception.Message }
    if (-not $caught -or $actual -notmatch [regex]::Escape($ExpectedCode)) {
        throw ('EXPECTED_TYPED_STOP_MISMATCH:' + $ExpectedCode)
    }
}

function Run-Test {
    param([Parameter(Mandatory = $true)][string]$Name,[Parameter(Mandatory = $true)][scriptblock]$Action)
    try { & $Action; Add-Test -Name $Name -Passed $true } catch { Add-Test -Name $Name -Passed $false -Detail ([string]$_.Exception.Message) }
}

function New-Query {
    param([Parameter(Mandatory = $true)][string]$Name,[Parameter(Mandatory = $true)][AllowEmptyString()][string]$Text)
    $path = Join-Path $tempRoot ($Name + '.sql')
    Write-P5EUtf8NoBom -Path $path -Text $Text
    return $path
}

function Invoke-BridgeFixture {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [switch]$Immutable,
        [long]$TimeoutMilliseconds = 60000L,
        [long]$MaximumOutputBytes = 4194304L
    )
    $name = 'q-' + [Guid]::NewGuid().ToString('N')
    $queryPath = New-Query -Name $name -Text $Text
    return Invoke-P5EHostSqliteBridge -DatabasePath $script:fixtureDb -QueryPath $queryPath -Immutable:$Immutable `
        -TimeoutMilliseconds $TimeoutMilliseconds -MaximumOutputBytes $MaximumOutputBytes
}

function Get-PythonPath {
    $command = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($null -eq $command) { $command = Get-Command py -ErrorAction SilentlyContinue }
    if ($null -eq $command) { throw 'P5E_HOST_SQLITE_PYTHON_MISSING' }
    return [string]$command.Source
}

try {
    $pythonPath = Get-PythonPath
    $fixtureDb = Join-Path $tempRoot 'fixture root # %.db'
    $fixtureScript = Join-Path $tempRoot 'make-fixture.py'
    Write-P5EUtf8NoBom -Path $fixtureScript -Text @'
import sqlite3
import sys
db = sys.argv[1]
con = sqlite3.connect(db)
con.execute("PRAGMA journal_mode=DELETE")
con.execute("PRAGMA foreign_keys=ON")
con.execute("CREATE TABLE probe(id INTEGER PRIMARY KEY, label TEXT, payload BLOB)")
con.execute("CREATE TABLE child(id INTEGER PRIMARY KEY, probe_id INTEGER REFERENCES probe(id))")
con.execute("INSERT INTO probe(id, label, payload) VALUES (1, 'alpha;quoted', X'00FF')")
con.execute("INSERT INTO probe(id, label, payload) VALUES (2, NULL, X'1020')")
con.execute("INSERT INTO child(id, probe_id) VALUES (1, 1)")
con.execute("PRAGMA user_version=24")
con.commit()
con.close()
'@
    $createRun = Invoke-P5EReadOnlyProcess -FilePath $pythonPath -ArgumentList @('-3', $fixtureScript, $fixtureDb) `
        -TimeoutMilliseconds 60000L -ClearInheritedEnvironmentVariableNames @($script:P5EAccountEnvironmentName)
    Assert-True ($createRun.LaunchCount -eq 1 -and $createRun.ExitCode -eq 0 -and $createRun.CaptureBounded) 'FIXTURE_CREATE_FAILED'
    Assert-True (Test-Path -LiteralPath $fixtureDb -PathType Leaf) 'FIXTURE_DB_MISSING'

    Run-Test 'fixture-created-under-bounded-temp-root' {
        Assert-True (Test-P5EPathUnderDirectory -Path $fixtureDb -Directory $tempRoot) 'FIXTURE_OUTSIDE_TEMP_ROOT'
        Assert-True ((Get-Item -LiteralPath $fixtureDb).Length -gt 0) 'FIXTURE_EMPTY'
    }
    Run-Test 'read-select-count' { $r = Invoke-BridgeFixture -Text 'SELECT count(*) FROM probe;' -Immutable; Assert-True ($r.ExitCode -eq 0 -and $r.Stdout.Trim() -eq '2') 'COUNT_READ_FAILED' }
    Run-Test 'read-integrity-check' { $r = Invoke-BridgeFixture -Text 'PRAGMA integrity_check;' -Immutable; Assert-True ($r.Stdout.Trim() -eq 'ok') 'INTEGRITY_READ_FAILED' }
    Run-Test 'read-foreign-key-check' { $r = Invoke-BridgeFixture -Text 'PRAGMA foreign_key_check;' -Immutable; Assert-True ($r.ExitCode -eq 0) 'FOREIGN_KEY_READ_FAILED' }
    Run-Test 'read-user-version' { $r = Invoke-BridgeFixture -Text 'PRAGMA user_version;' -Immutable; Assert-True ($r.Stdout.Trim() -eq '24') 'USER_VERSION_READ_FAILED' }
    Run-Test 'read-null-rendering' { $r = Invoke-BridgeFixture -Text 'SELECT label FROM probe WHERE id=2;' -Immutable; Assert-True ($r.Stdout.Trim() -eq 'NULL') 'NULL_RENDER_FAILED' }
    Run-Test 'read-blob-rendering' { $r = Invoke-BridgeFixture -Text 'SELECT payload FROM probe WHERE id=2;' -Immutable; Assert-True ($r.Stdout.Trim() -eq '1020') 'BLOB_RENDER_FAILED' }
    Run-Test 'read-quoted-semicolon' { $r = Invoke-BridgeFixture -Text "SELECT label FROM probe WHERE id=1;" -Immutable; Assert-True ($r.Stdout.Trim() -eq 'alpha;quoted') 'QUOTED_VALUE_READ_FAILED' }
    Run-Test 'read-where-zero-empty-result' { $r = Invoke-BridgeFixture -Text 'SELECT id FROM probe WHERE id=99;' -Immutable; Assert-True ($r.ExitCode -eq 0 -and [string]::IsNullOrWhiteSpace($r.Stdout)) 'EMPTY_RESULT_FAILED' }
    Run-Test 'read-multi-statement-bounded' { $r = Invoke-BridgeFixture -Text "PRAGMA integrity_check;`nSELECT count(*) FROM child;" -Immutable; Assert-True ($r.Stdout -match 'ok' -and $r.Stdout -match '1') 'MULTI_STATEMENT_READ_FAILED' }
    Run-Test 'read-immutable-flag-recorded' { $r = Invoke-BridgeFixture -Text 'SELECT 1;' -Immutable; Assert-True ([bool]$r.Immutable -and $r.ExitCode -eq 0) 'IMMUTABLE_FLAG_FAILED' }
    Run-Test 'read-space-hash-percent-path' { $r = Invoke-BridgeFixture -Text 'SELECT 1;' -Immutable; Assert-True ($r.QueryPath -match 'q-' -and $r.BridgeSha256 -match '^[0-9a-f]{64}$') 'PATH_ENCODING_READ_FAILED' }
    Run-Test 'read-capture-bounded' { $r = Invoke-BridgeFixture -Text 'SELECT 1;' -Immutable; Assert-True ($r.Run.CaptureBounded -and -not $r.Run.RedactionViolation) 'CAPTURE_NOT_BOUNDED' }
    Run-Test 'read-two-column-shape' { $r = Invoke-BridgeFixture -Text 'SELECT id, label FROM probe WHERE id=1;' -Immutable; Assert-True ($r.Stdout -match '1\talpha;quoted') 'TWO_COLUMN_READ_FAILED' }
    Run-Test 'read-aggregate' { $r = Invoke-BridgeFixture -Text 'SELECT sum(id) FROM probe;' -Immutable; Assert-True ($r.Stdout.Trim() -eq '3') 'AGGREGATE_READ_FAILED' }
    Run-Test 'read-pragma-query-only-on' { $r = Invoke-BridgeFixture -Text 'PRAGMA query_only=ON; SELECT 1;' -Immutable; Assert-True ($r.ExitCode -eq 0 -and $r.Stdout -match '1') 'QUERY_ONLY_ON_FAILED' }
    Run-Test 'read-pragma-foreign-keys-on' { $r = Invoke-BridgeFixture -Text 'PRAGMA foreign_keys=ON; SELECT 1;' -Immutable; Assert-True ($r.ExitCode -eq 0 -and $r.Stdout -match '1') 'FOREIGN_KEYS_ON_FAILED' }

    $writeCases = [ordered]@{
        'write-create-rejected' = 'CREATE TABLE bad(x);'
        'write-insert-rejected' = "INSERT INTO probe(id) VALUES (99);"
        'write-update-rejected' = "UPDATE probe SET label='changed' WHERE id=1;"
        'write-delete-rejected' = 'DELETE FROM probe WHERE id=1;'
        'write-replace-rejected' = "REPLACE INTO probe(id) VALUES (99);"
        'write-alter-rejected' = 'ALTER TABLE probe ADD COLUMN extra TEXT;'
        'write-drop-rejected' = 'DROP TABLE probe;'
        'write-vacuum-rejected' = 'VACUUM;'
        'write-reindex-rejected' = 'REINDEX;'
        'write-attach-rejected' = "ATTACH DATABASE 'other.db' AS other;"
        'write-detach-rejected' = 'DETACH DATABASE other;'
        'write-begin-rejected' = 'BEGIN;'
        'write-commit-rejected' = 'COMMIT;'
        'write-rollback-rejected' = 'ROLLBACK;'
        'write-savepoint-rejected' = 'SAVEPOINT p;'
        'write-release-rejected' = 'RELEASE p;'
        'write-query-only-off-rejected' = 'PRAGMA query_only=OFF;'
        'write-foreign-keys-off-rejected' = 'PRAGMA foreign_keys=OFF;'
        'write-pragma-assignment-rejected' = 'PRAGMA user_version=99;'
        'write-with-insert-rejected' = 'WITH x AS (SELECT 1) INSERT INTO probe(id) SELECT * FROM x;'
    }
    foreach ($entry in $writeCases.GetEnumerator()) {
        $name = [string]$entry.Key; $sql = [string]$entry.Value
        Run-Test $name { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-BridgeFixture -Text $sql -Immutable } }
    }

    Run-Test 'invalid-select-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-BridgeFixture -Text 'SELECT missing_column FROM probe;' -Immutable } }
    Run-Test 'empty-query-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-BridgeFixture -Text '   ' -Immutable } }
    Run-Test 'nonselect-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-BridgeFixture -Text 'EXPLAIN SELECT 1;' -Immutable } }
    Run-Test 'malformed-quote-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-BridgeFixture -Text "SELECT 'unterminated;" -Immutable } }
    Run-Test 'output-limit-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-BridgeFixture -Text "SELECT printf('%.*c', 128, 'x');" -Immutable -MaximumOutputBytes 8L } }
    Run-Test 'timeout-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_TIMEOUT' -Action { Invoke-BridgeFixture -Text 'WITH RECURSIVE c(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM c WHERE x<100000000) SELECT sum(x) FROM c;' -Immutable -TimeoutMilliseconds 1L } }
    Run-Test 'python-path-missing-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_PYTHON_MISSING' -Action { Invoke-P5EHostSqliteBridge -DatabasePath $fixtureDb -QueryPath (New-Query 'missing-python-query' 'SELECT 1;') -Immutable -PythonPath (Join-Path $tempRoot 'missing-python.exe') } }
    Run-Test 'database-missing-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_INPUT_MISSING' -Action { Invoke-P5EHostSqliteBridge -DatabasePath (Join-Path $tempRoot 'missing.db') -QueryPath (New-Query 'missing-db-query' 'SELECT 1;') -Immutable } }
    Run-Test 'query-missing-rejected' { Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_INPUT_MISSING' -Action { Invoke-P5EHostSqliteBridge -DatabasePath $fixtureDb -QueryPath (Join-Path $tempRoot 'missing.sql') -Immutable } }
    Run-Test 'corrupt-database-rejected' { $corrupt = Join-Path $tempRoot 'corrupt.db'; [IO.File]::WriteAllBytes($corrupt, [byte[]](1,2,3,4)); Invoke-ExpectedStop -ExpectedCode 'P5E_HOST_SQLITE_OPEN_OR_QUERY_FAILED' -Action { Invoke-P5EHostSqliteBridge -DatabasePath $corrupt -QueryPath (New-Query 'corrupt-query' 'SELECT 1;') -Immutable } }
    Run-Test 'database-remains-unchanged' { $before = Get-P5ESha256 -Path $fixtureDb; [void](Invoke-BridgeFixture -Text 'SELECT count(*) FROM probe;' -Immutable); $after = Get-P5ESha256 -Path $fixtureDb; Assert-True ($before -ceq $after) 'DATABASE_CHANGED_BY_READ' }
    Run-Test 'no-database-write-counter' { Assert-True ($counters.databaseWrites -eq 0) 'DATABASE_WRITE_COUNTER_NONZERO' }
    Run-Test 'no-wal-or-shm-created' { Assert-True (-not (Test-Path -LiteralPath ($fixtureDb + '-wal')) -and -not (Test-Path -LiteralPath ($fixtureDb + '-shm'))) 'WAL_SHM_CREATED' }
    Run-Test 'fixture-regular-file-no-reparse' { $item = Get-Item -LiteralPath $fixtureDb -Force; Assert-True (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'FIXTURE_DB_REPARSE' }
    Run-Test 'query-regular-file-no-reparse' { $q = New-Query 'guard-query' 'SELECT 1;'; $item = Get-Item -LiteralPath $q -Force; Assert-True (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -eq 0) 'QUERY_REPARSE' }
    Run-Test 'query-path-under-temp-root' { $q = New-Query 'root-query' 'SELECT 1;'; Assert-True (Test-P5EPathUnderDirectory -Path $q -Directory $tempRoot) 'QUERY_OUTSIDE_TEMP_ROOT' }
    Run-Test 'bridge-python-source-parses' {
        $compileScript = Join-Path $tempRoot 'parse-bridge.py'
        Write-P5EUtf8NoBom -Path $compileScript -Text "import ast,sys; ast.parse(open(sys.argv[1], encoding='utf-8').read())"
        $run = Invoke-P5EReadOnlyProcess -FilePath $pythonPath -ArgumentList @('-3', $compileScript, (Join-Path $repoRoot 'docs\P5E_SQLITE_BRIDGE.py')) -TimeoutMilliseconds 60000L -ClearInheritedEnvironmentVariableNames @($script:P5EAccountEnvironmentName)
        Assert-True ($run.ExitCode -eq 0 -and $run.CaptureBounded) 'BRIDGE_PARSE_FAILED'
    }
    Run-Test 'report-secret-sentinel-absent' {
        $value = $tests | ConvertTo-Json -Depth 10 -Compress
        Assert-True ($value -notmatch '(?i)(endpoint_account_fingerprint|credential|password|secret)') 'SECRET_SENTINEL_IN_REPORT'
    }
    Run-Test 'live-counters-all-zero' { foreach ($value in $counters.Values) { Assert-True ([long]$value -eq 0L) 'LIVE_COUNTER_NONZERO' } }
} finally {
    $failureCount = @($tests.Values | Where-Object { -not [bool]$_.passed }).Count
    $report = [ordered]@{
        schemaVersion = 'p5e.a43.db-host-readback-matrix.v1'
        generatedDate = '2026-09-28'
        status = if ($failureCount -eq 0 -and $tests.Count -eq 56) { 'PASS' } else { 'FAIL' }
        testCount = $tests.Count
        failureCount = $failureCount
        tests = $tests
        counters = $counters
        fixturePolicy = 'TEMP_ROOT_ONLY_NO_RAW_OUTPUT_NO_DEVICE_PROVIDER_CREDENTIAL_OR_DATABASE_WRITE'
    }
    $parent = Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath))
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) { [void](New-Item -ItemType Directory -Path $parent -Force) }
    Write-P5EUtf8NoBom -Path $OutputPath -Text ($report | ConvertTo-Json -Depth 20)
    if (Test-Path -LiteralPath $tempRoot) { Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue }
}

$exit = if ($failureCount -eq 0 -and $tests.Count -eq 56) { 0 } else { 1 }
Write-Output ('P5E_A43_DB_HOST_READBACK_MATRIX=' + [string]$report.status)
Write-Output ('P5E_A43_DB_HOST_READBACK_TESTS=' + [string]$report.testCount)
Write-Output ('P5E_A43_DB_HOST_READBACK_FAILURES=' + [string]$report.failureCount)
exit $exit
