[CmdletBinding()]
param([string]$OutputPath = '')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
if ([string]::IsNullOrWhiteSpace($OutputPath)) { $OutputPath = Join-Path $repoRoot 'docs\P5E_A43_BINDING_TUPLE_MATRIX_20260928.json' }
. $helperPath -LibraryOnly

$tests = [ordered]@{}
$regression = [ordered]@{}
$failures = [System.Collections.Generic.List[string]]::new()
$regressionFailures = [System.Collections.Generic.List[string]]::new()

function Add-Case {
    param([Parameter(Mandatory = $true)][System.Collections.IDictionary]$Target,[Parameter(Mandatory = $true)][string]$Name,[Parameter(Mandatory = $true)][scriptblock]$Body)
    try { & $Body | Out-Null; $Target[$Name] = [ordered]@{ passed = $true; detail = 'PASS' } }
    catch { $Target[$Name] = [ordered]@{ passed = $false; detail = 'TYPED_ASSERTION_FAILED' }; if ($Target -eq $tests) { [void]$failures.Add($Name) } else { [void]$regressionFailures.Add($Name) } }
}

function New-Hash {
    param([char]$Character,[int]$Length = 64)
    return [string]::new($Character, $Length)
}

function New-OfflineRun {
    param([int]$ExitCode = 0,[string]$Stdout = '',[string]$Stderr = '')
    return [pscustomobject]@{ LaunchCount = 1L; TimedOut = $false; ExitCode = $ExitCode; Stdout = $Stdout; Stderr = $Stderr; RedactionViolation = $false; OutputTooLarge = $false; CaptureBounded = $true; ContainmentVerified = $true }
}

for ($index = 0; $index -lt 100; $index++) {
    $id = 'binding-tuple-' + $index.ToString('000')
    Add-Case -Target $tests -Name ('binding-canonical-' + $index.ToString('000')) -Body {
        $key = Get-P5EA43DecisionKey -CanonicalDecisionId $id
        if ($key -notmatch '^[0-9a-f]{64}$') { throw 'DECISION_KEY_SHAPE' }
        if ($key -cne (Get-P5EA43DecisionKey -CanonicalDecisionId $id)) { throw 'DECISION_KEY_NOT_DETERMINISTIC' }
    }
}

for ($index = 0; $index -lt 100; $index++) {
    $manifest = New-Hash ([char](97 + ($index % 6)))
    $account = New-Hash ([char](97 + (($index + 1) % 6)))
    $issued = 1700000000000L + $index
    $expires = $issued + $script:P5EAuthorizationValidityMilliseconds
    Add-Case -Target $tests -Name ('binding-instrumentation-' + $index.ToString('000')) -Body {
        $pairs = New-P5EPlan -ManifestHash $manifest -AccountFingerprint $account -IssuedAtMillis $issued -ExpiresAtMillis $expires
        $args = New-P5EAdbArgumentList -Serial $script:P5ESerial -RemoteCommandTokens (New-P5ERemoteCommandTokens -Pairs $pairs)
        $contract = Test-P5EInstrumentationArguments -AdbArguments $args -ManifestHash $manifest -AccountFingerprint $account -IssuedAtMillis $issued -ExpiresAtMillis $expires
        if (-not $contract.Passed) { throw 'INSTRUMENTATION_BINDING' }
    }
}

for ($index = 0; $index -lt 62; $index++) {
    $hash = New-Hash ([char](48 + ($index % 10)))
    Add-Case -Target $tests -Name ('binding-redacted-hash-' + $index.ToString('000')) -Body {
        if ((Assert-P5EA43Sha256 -Value $hash -Label 'SYNTHETIC') -cne $hash) { throw 'HASH_NORMALIZATION' }
        $safe = Protect-P5ECaptureText -Text ('status-' + $index.ToString('000')) -SensitiveValues @($hash)
        if ([string]$safe.Text -notmatch '^status-[0-9]{3}$' -or [bool]$safe.Violation) { throw 'SAFE_CAPTURE_CONTRACT' }
    }
}

for ($index = 0; $index -lt 50; $index++) {
    $path = 'package:/data/app/com.ml.tblandroidtxt-' + $index.ToString('000') + '/base.apk'
    Add-Case -Target $regression -Name ('regression-pm-present-' + $index.ToString('000')) -Body {
        $result = Get-P5EPackagePathClassification -Run (New-OfflineRun -Stdout $path) -PackageName 'com.ml.tblandroidtxt'
        if ([string]$result.Classification -cne 'PACKAGE_PRESENT') { throw 'PM_PRESENT' }
    }
}

for ($index = 0; $index -lt 50; $index++) {
    $token = 'safe-token-' + $index.ToString('000')
    Add-Case -Target $regression -Name ('regression-remote-roundtrip-' + $index.ToString('000')) -Body {
        $quoted = ConvertTo-P5EAndroidShellArgument -Value $token
        $parsed = ConvertFrom-P5EPosixCommandLine -CommandLine ('echo ' + $quoted)
        if ($parsed.Operators.Count -ne 0 -or @($parsed.Tokens).Count -ne 2 -or [string]$parsed.Tokens[1] -cne $token) { throw 'REMOTE_ROUNDTRIP' }
    }
}

for ($index = 0; $index -lt 75; $index++) {
    $stderr = if (($index % 2) -eq 0) { 'unknown package: com.ml.tblandroidtxt' } else { 'device offline' }
    $expected = if (($index % 2) -eq 0) { 'PACKAGE_NOT_FOUND' } else { 'DEVICE_UNAVAILABLE' }
    Add-Case -Target $regression -Name ('regression-pm-fail-closed-' + $index.ToString('000')) -Body {
        $result = Get-P5EPackagePathClassification -Run (New-OfflineRun -ExitCode 1 -Stderr $stderr) -PackageName 'com.ml.tblandroidtxt'
        if ([string]$result.Classification -cne $expected) { throw 'PM_CLASSIFICATION' }
    }
}

$allFailures = @($failures + $regressionFailures)
$report = [ordered]@{
    schemaVersion = 'p5e.a43.binding-tuple-matrix.v1'
    generatedDate = '2026-09-28'
    status = if ($allFailures.Count -eq 0) { 'PASS' } else { 'STOP' }
    testCount = $tests.Count
    passedTestCount = (@($tests.Values | Where-Object { $_.passed })).Count
    regressionMatrix = [ordered]@{
        status = if ($regressionFailures.Count -eq 0) { 'PASS' } else { 'STOP' }
        caseCount = $regression.Count
        passedCaseCount = (@($regression.Values | Where-Object { $_.passed })).Count
        failureCount = $regressionFailures.Count
        cases = $regression
    }
    failures = $allFailures
    tests = $tests
    liveCounters = [ordered]@{ adb = 0; device = 0; provider = 0; credential = 0; databaseWrites = 0; buildInstall = 0; raw = 0; redispatch = 0 }
    source = 'clean-archive-self-contained-synthetic-matrix'
}
[void][IO.Directory]::CreateDirectory((Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath))))
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($report | ConvertTo-Json -Depth 20), [Text.UTF8Encoding]::new($false))
Write-Output ('P5E_A43_BINDING_TUPLE_MATRIX=' + $report.status)
Write-Output ('P5E_A43_BINDING_TUPLE_TESTS=' + $report.testCount)
Write-Output ('P5E_A43_BINDING_TUPLE_FAILURES=' + $allFailures.Count)
if ($allFailures.Count -ne 0) { exit 1 }
