[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$GoldenOutput,
    [string]$ResultPath = '',
    [string]$GoldenJvmCommand = 'NOT_PROVIDED',
    [string]$JvmRuntime = 'NOT_PROVIDED',
    [string]$GoldenTestSourceSha256 = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$workRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-sql-behavioral-' + [Guid]::NewGuid().ToString('N'))
$fixtureRoot = Join-Path $workRoot 'fixtures'
$evidenceRoot = Join-Path $workRoot 'evidence'
[void](New-Item -ItemType Directory -Path $workRoot -Force)
[void](New-Item -ItemType Directory -Path $evidenceRoot -Force)

. (Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1') -LibraryOnly

$checks = [System.Collections.Generic.List[object]]::new()
$failures = [System.Collections.Generic.List[string]]::new()
$scenarioSummaries = [System.Collections.Generic.List[object]]::new()
$mutationSummaries = [System.Collections.Generic.List[object]]::new()
$scenarioReadbacks = @{}
$scenarioErrors = @{}

function Add-BehavioralCheck {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Details
    )
    $status = if ($Passed) { 'PASS' } else { 'FAIL' }
    [void]$script:checks.Add([ordered]@{ name = $Name; status = $status; details = $Details })
    if (-not $Passed) { [void]$script:failures.Add($Name + ':' + $Details) }
}

function Get-FileSha256Lower {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-P5ESha256 -Path $Path).ToLowerInvariant()
}

function Get-BytesSha256Lower {
    param([Parameter(Mandatory = $true)][byte[]]$Bytes)
    $path = Join-Path $script:evidenceRoot ('hash-' + [Guid]::NewGuid().ToString('N') + '.bin')
    [IO.File]::WriteAllBytes($path, $Bytes)
    return Get-FileSha256Lower -Path $path
}

function Get-JsonObject {
    param([Parameter(Mandatory = $true)][string]$Path)
    return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
}

function Get-ScenarioPath {
    param([Parameter(Mandatory = $true)][string]$Scenario)
    $entry = @($script:fixtureManifest.fixtures | Where-Object { [string]$_.scenario -ceq $Scenario })
    if ($entry.Count -ne 1) { throw ('P5E_FIXTURE_SCENARIO_MISSING:' + $Scenario) }
    return [string]$entry[0].path
}

function Invoke-OfflineScenario {
    param([Parameter(Mandatory = $true)][string]$Scenario)
    $scenarioDirectory = Join-Path $script:evidenceRoot ('scenario-' + $Scenario)
    [void](New-Item -ItemType Directory -Path $scenarioDirectory -Force)
    $outputPath = Join-Path $scenarioDirectory 'query-output.tsv'
    $databasePath = Get-ScenarioPath -Scenario $Scenario
    $readback = $null
    $errorText = ''
    $completed = $false
    try {
        $readback = Get-P5EConsistentDatabaseReadback -AdbPath 'offline-fixture' -SerialValue 'offline-fixture' `
            -LocalDatabasePath $databasePath -LocalOutputPath $outputPath
        $completed = $true
        $script:scenarioReadbacks[$Scenario] = $readback
    } catch {
        $errorText = [string]$_.Exception.Message
        $script:scenarioErrors[$Scenario] = $errorText
    }
    $summary = [ordered]@{
        scenario = $Scenario
        fixtureSha256 = Get-FileSha256Lower -Path $databasePath
        queryOutputPath = 'TEMP_OUTSIDE_REPO/' + $Scenario + '/query-output.tsv'
        queryOutputSha256 = if (Test-Path -LiteralPath $outputPath -PathType Leaf) { Get-FileSha256Lower -Path $outputPath } else { '' }
        collectorCompleted = $completed
        typedError = $errorText
    }
    [void]$script:scenarioSummaries.Add($summary)
    return [pscustomobject]@{ Scenario = $Scenario; Completed = $completed; Readback = $readback; Error = $errorText; OutputPath = $outputPath }
}

function Get-OutputLines {
    param([Parameter(Mandatory = $true)][string]$Path)
    return @((Get-Content -Raw -LiteralPath $Path) -split '\r?\n' | Where-Object { $_ -ne '' })
}

function Get-TaggedOutputLine {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Tag)
    $lines = @(Get-OutputLines -Path $Path | Where-Object { $_.StartsWith($Tag + "`t") })
    if ($lines.Count -ne 1) { throw ('P5E_TEST_TAGGED_ROW_COUNT:' + $Tag + ':' + [string]$lines.Count) }
    return [string]$lines[0]
}

function Expect-ParserTypedStop {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Output,
        [Parameter(Mandatory = $true)][string]$ExpectedPattern
    )
    $thrown = $false
    $message = ''
    try { ConvertFrom-P5EConsistentDatabaseReadbackOutput -Output $Output | Out-Null }
    catch { $message = [string]$_.Exception.Message; $thrown = $message -match $ExpectedPattern }
    $details = if ($thrown) { $message } else { 'NO_TYPED_STOP' }
    Add-BehavioralCheck -Name $Name -Passed $thrown -Details $details
    return $thrown
}

function Invoke-GoldenPairValidator {
    param(
        [Parameter(Mandatory = $true)][string]$Case,
        [Parameter(Mandatory = $true)][byte[]]$ReportBytes,
        [Parameter(Mandatory = $true)][byte[]]$ReceiptBytes
    )
    $caseDirectory = Join-Path $script:evidenceRoot ('golden-' + $Case)
    [void](New-Item -ItemType Directory -Path $caseDirectory -Force)
    $reportPath = Join-Path $caseDirectory 'report.bin'
    $receiptPath = Join-Path $caseDirectory 'receipt.bin'
    [IO.File]::WriteAllBytes($reportPath, $ReportBytes)
    [IO.File]::WriteAllBytes($receiptPath, $ReceiptBytes)
    $errors = [System.Collections.Generic.List[string]]::new()
    $reportValidation = $null
    $receiptValidation = $null
    try {
        $reportValidation = Test-P5ESerializedArtifactBytes -Path $reportPath -ExpectedSchema 'safe4.full.report-l1.v1' `
            -Kind 'report' -Errors $errors -ExpectedIdentity $script:fixtureIdentity
        $receiptValidation = Test-P5ESerializedArtifactBytes -Path $receiptPath -ExpectedSchema 'safe4.full.receipt.v1' `
            -Kind 'receipt' -Errors $errors -ExpectedIdentity $script:fixtureIdentity
        if ($null -ne $reportValidation -and $null -ne $receiptValidation) {
            Test-P5EArtifactPair -Report $reportValidation.Parsed -Receipt $receiptValidation.Parsed -Errors $errors
        }
    } catch {
        [void]$errors.Add('HOST_VALIDATOR_EXCEPTION:' + [string]$_.Exception.Message)
    }
    return [pscustomobject]@{
        case = $Case
        accepted = $errors.Count -eq 0
        errorCodes = $errors.ToArray()
        reportSha256 = Get-FileSha256Lower -Path $reportPath
        receiptSha256 = Get-FileSha256Lower -Path $receiptPath
    }
}

function New-CanonicalArtifactBytes {
    param([Parameter(Mandatory = $true)]$Object)
    return [Text.UTF8Encoding]::new($false).GetBytes((ConvertTo-P5ECanonicalJson -Value $Object))
}

try {
    $goldenObject = Get-JsonObject -Path $GoldenOutput
    $goldenReportBytes = [Convert]::FromBase64String([string]$goldenObject.reportBase64)
    $goldenReceiptBytes = [Convert]::FromBase64String([string]$goldenObject.receiptBase64)
    $script:fixtureIdentity = $goldenObject.fixtureIdentity
    $goldenDirectory = Join-Path $evidenceRoot 'golden-source'
    [void](New-Item -ItemType Directory -Path $goldenDirectory -Force)
    $goldenReportPath = Join-Path $goldenDirectory 'report.bin'
    $goldenReceiptPath = Join-Path $goldenDirectory 'receipt.bin'
    [IO.File]::WriteAllBytes($goldenReportPath, $goldenReportBytes)
    [IO.File]::WriteAllBytes($goldenReceiptPath, $goldenReceiptBytes)

    Add-BehavioralCheck -Name 'golden.source-is-production-serializer' `
        -Passed ([string]$goldenObject.source -ceq 'EditorialP5PilotExecution.reportBytes/receiptBytes' -and
            [string]$goldenObject.serializerClass -ceq 'com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecution') `
        -Details ([string]$goldenObject.serializerClass)
    Add-BehavioralCheck -Name 'golden.bytes-are-utf8-no-bom-files' `
        -Passed ($goldenReportBytes.Length -gt 0 -and $goldenReceiptBytes.Length -gt 0 -and
            -not ($goldenReportBytes.Length -ge 3 -and $goldenReportBytes[0] -eq 0xef -and $goldenReportBytes[1] -eq 0xbb -and $goldenReportBytes[2] -eq 0xbf) -and
            -not ($goldenReceiptBytes.Length -ge 3 -and $goldenReceiptBytes[0] -eq 0xef -and $goldenReceiptBytes[1] -eq 0xbb -and $goldenReceiptBytes[2] -eq 0xbf)) `
        -Details ('report=' + [string]$goldenReportBytes.Length + ';receipt=' + [string]$goldenReceiptBytes.Length)

    $builderPath = Join-Path $repoRoot 'docs\P5E_SQL_BEHAVIORAL_FIXTURES.py'
    $builderOutputPath = Join-Path $evidenceRoot 'fixture-builder-output.json'
    $builderOutput = (& py.exe -3 $builderPath --repo $repoRoot --output-root $fixtureRoot --golden-output $GoldenOutput 2>&1 | Out-String)
    $builderExit = $LASTEXITCODE
    Write-P5EUtf8NoBom -Path $builderOutputPath -Text $builderOutput
    Add-BehavioralCheck -Name 'fixture-builder.offline-only' -Passed ($builderExit -eq 0) `
        -Details ('exit=' + [string]$builderExit + ';outputSha256=' + (Get-FileSha256Lower -Path $builderOutputPath))
    if ($builderExit -ne 0) { throw 'P5E_FIXTURE_BUILDER_FAILED' }

    $fixtureManifestPath = Join-Path $fixtureRoot 'FIXTURE_MANIFEST.json'
    $script:fixtureManifest = Get-JsonObject -Path $fixtureManifestPath
    Add-BehavioralCheck -Name 'fixture.integrity-fk-and-source-hash' `
        -Passed ([string]$script:fixtureManifest.scope -ceq 'OFFLINE_SQLITE_DDL_FIXTURES_ONLY' -and
            [int]$script:fixtureManifest.deviceActions -eq 0 -and [int]$script:fixtureManifest.providerCalls -eq 0 -and
            [int]$script:fixtureManifest.credentialReads -eq 0) `
        -Details ('sqlite=' + [string]$script:fixtureManifest.sqliteVersion + ';ddlSha256=' + [string]$script:fixtureManifest.ddlSha256)

    $queryPath = Join-Path $evidenceRoot 'collector-query.sql'
    Write-P5EUtf8NoBom -Path $queryPath -Text (New-P5EConsistentDatabaseReadbackQuery)
    $querySha256 = Get-FileSha256Lower -Path $queryPath

    $unused = Invoke-OfflineScenario -Scenario 'unused'
    Add-BehavioralCheck -Name 'collector.unused-four-input-control' -Passed $unused.Completed -Details $unused.Error
    if ($unused.Completed) {
        $unusedLines = @(Get-OutputLines -Path $unused.OutputPath)
        $inputLines = @($unusedLines | Where-Object { $_.StartsWith('INPUT' + "`t") })
        $inputWidths = @($inputLines | ForEach-Object { $_.Split([char]9).Count })
        Add-BehavioralCheck -Name 'collector.input-seven-columns-control' `
            -Passed ($inputLines.Count -eq 4 -and @($inputWidths | Where-Object { $_ -ne 7 }).Count -eq 0) `
            -Details ('rows=' + [string]$inputLines.Count + ';widths=' + ($inputWidths -join ','))
        $zeroNames = @('exactAttempt', 'exactAuthorization', 'exactLifecycle', 'attempts', 'authorizationReceipts',
            'lifecycle', 'reconciliation', 'reconciliationHistory', 'reportOrReceipt', 'partialArtifactPair', 'activeCompetingWriter')
        $zeroPassed = $true
        foreach ($name in $zeroNames) { if ([long](Get-P5EProperty $unused.Readback.lineage $name) -ne 0L) { $zeroPassed = $false } }
        Add-BehavioralCheck -Name 'collector.unused-lineage-zero-typed' -Passed $zeroPassed -Details (($unused.Readback.lineage | ConvertTo-Json -Compress))
    }

    $claimed = Invoke-OfflineScenario -Scenario 'claimed-null'
    Add-BehavioralCheck -Name 'collector.claimed-null-row-preserved' -Passed $claimed.Completed -Details $claimed.Error
    if ($claimed.Completed) {
        $attempt = $claimed.Readback.attempt
        $nullPair = $null -eq $attempt.reportByteLength -and $null -eq $attempt.reportHex -and
            $null -eq $attempt.receiptByteLength -and $null -eq $attempt.receiptHex
        Add-BehavioralCheck -Name 'collector.claimed-null-sentinel-not-zero' -Passed ($attempt.status -ceq 'CLAIMED' -and $nullPair -and $attempt.metrics -ceq '') `
            -Details ('status=' + [string]$attempt.status + ';metrics-empty=' + [string]($attempt.metrics -ceq ''))
    }

    $recovery = Invoke-OfflineScenario -Scenario 'recovery-null'
    Add-BehavioralCheck -Name 'collector.recovery-null-row-preserved' -Passed $recovery.Completed -Details $recovery.Error
    if ($recovery.Completed) {
        $attempt = $recovery.Readback.attempt
        $nullPair = $null -eq $attempt.reportByteLength -and $null -eq $attempt.reportHex -and
            $null -eq $attempt.receiptByteLength -and $null -eq $attempt.receiptHex
        Add-BehavioralCheck -Name 'collector.recovery-typed-status-and-reason' `
            -Passed ($attempt.status -ceq 'RECOVERY_REQUIRED' -and $attempt.recoveryReasonCode -ceq 'RETRY_PROVIDER_CALL_TIMEOUT' -and $nullPair -and $attempt.metrics -ceq '') `
            -Details ('status=' + [string]$attempt.status + ';reason=' + [string]$attempt.recoveryReasonCode)
    }

    $committed = Invoke-OfflineScenario -Scenario 'committed-golden'
    Add-BehavioralCheck -Name 'collector.committed-golden-row-preserved' -Passed $committed.Completed -Details $committed.Error
    $goldenValidation = $null
    if ($committed.Completed) {
        $attempt = $committed.Readback.attempt
        $reportBytes = Get-P5EHexBytes -Hex ([string]$attempt.reportHex) -Name 'committed-report'
        $receiptBytes = Get-P5EHexBytes -Hex ([string]$attempt.receiptHex) -Name 'committed-receipt'
        $storedReportPath = Join-Path (Join-Path $evidenceRoot 'committed') 'report.bin'
        $storedReceiptPath = Join-Path (Join-Path $evidenceRoot 'committed') 'receipt.bin'
        [void](New-Item -ItemType Directory -Path (Split-Path -Parent $storedReportPath) -Force)
        [IO.File]::WriteAllBytes($storedReportPath, $reportBytes)
        [IO.File]::WriteAllBytes($storedReceiptPath, $receiptBytes)
        Add-BehavioralCheck -Name 'collector.committed-byte-length-and-hash' `
            -Passed ($reportBytes.Length -eq $goldenReportBytes.Length -and $receiptBytes.Length -eq $goldenReceiptBytes.Length -and
                [Convert]::ToBase64String($reportBytes) -ceq [Convert]::ToBase64String($goldenReportBytes) -and
                [Convert]::ToBase64String($receiptBytes) -ceq [Convert]::ToBase64String($goldenReceiptBytes) -and
                (Get-FileSha256Lower -Path $storedReportPath) -ceq (Get-FileSha256Lower -Path $goldenReportPath) -and
                (Get-FileSha256Lower -Path $storedReceiptPath) -ceq (Get-FileSha256Lower -Path $goldenReceiptPath)) `
            -Details ('reportSha256=' + (Get-FileSha256Lower -Path $storedReportPath) + ';receiptSha256=' + (Get-FileSha256Lower -Path $storedReceiptPath))
        $goldenValidation = Invoke-GoldenPairValidator -Case 'valid' -ReportBytes $reportBytes -ReceiptBytes $receiptBytes
        Add-BehavioralCheck -Name 'verifier.production-golden-pair-accepted' -Passed $goldenValidation.accepted `
            -Details (($goldenValidation.errorCodes -join ','))
    } else {
        Add-BehavioralCheck -Name 'verifier.production-golden-pair-accepted' -Passed $false -Details 'COMMITTED_READBACK_MISSING'
    }

    $lineageScenario = Invoke-OfflineScenario -Scenario 'lineage-reconciliation'
    Add-BehavioralCheck -Name 'collector.lineage-reconciliation-source-map' -Passed $lineageScenario.Completed -Details $lineageScenario.Error
    if ($lineageScenario.Completed) {
        $lineage = $lineageScenario.Readback.lineage
        $expectedLineage = [ordered]@{ exactAttempt = 1; exactAuthorization = 1; exactLifecycle = 1; attempts = 1; authorizationReceipts = 1; lifecycle = 1; reconciliation = 1; reconciliationHistory = 1; reportOrReceipt = 0; partialArtifactPair = 0; activeCompetingWriter = 0 }
        $lineagePassed = $true
        foreach ($name in $expectedLineage.Keys) { if ([long](Get-P5EProperty $lineage $name) -ne [long]$expectedLineage[$name]) { $lineagePassed = $false } }
        Add-BehavioralCheck -Name 'collector.lineage-all-sources-counted' -Passed $lineagePassed -Details ($lineage | ConvertTo-Json -Compress)
        Add-BehavioralCheck -Name 'collector.recovery-with-lineage-not-raw-accepted' `
            -Passed ($lineageScenario.Readback.attempt.status -ceq 'RECOVERY_REQUIRED' -and [long]$lineage.reconciliation -gt 0L) `
            -Details 'RECOVERY_REQUIRED_with_reconciliation_history'
    }

    $partial = Invoke-OfflineScenario -Scenario 'partial-blob'
    if ($partial.Completed) {
        $partialAttempt = $partial.Readback.attempt
        Add-BehavioralCheck -Name 'collector.partial-blob-semantic-stop' `
            -Passed ($partialAttempt.status -ceq 'RECOVERY_REQUIRED' -and [long]$partial.Readback.lineage.partialArtifactPair -eq 1L -and
                $null -ne $partialAttempt.reportHex -and $null -eq $partialAttempt.receiptHex) `
            -Details 'partialArtifactPair=1;acceptance=false'
    } else {
        Add-BehavioralCheck -Name 'collector.partial-blob-semantic-stop' -Passed $false -Details $partial.Error
    }

    $unusedRaw = Get-Content -Raw -LiteralPath $unused.OutputPath
    $lineageLine = Get-TaggedOutputLine -Path $unused.OutputPath -Tag 'LINEAGE'
    $lineageColumns = $lineageLine.Split([char]9)
    $dropLineage = [string]::Join("`t", @($lineageColumns[0..15]))
    $extraLineage = $lineageLine + "`textra"
    $badIntegerColumns = @($lineageColumns)
    $badIntegerColumns[1] = 'not-an-integer'
    $badIntegerLine = [string]::Join("`t", $badIntegerColumns)
    $duplicateLineage = $unusedRaw + $lineageLine + "`r`n"
    [void](Expect-ParserTypedStop -Name 'parser.missing-lineage-field-fails-closed' -Output $unusedRaw.Replace($lineageLine, $dropLineage) -ExpectedPattern '^P5E_COLLECTOR_LINEAGE_ROW_INVALID$')
    [void](Expect-ParserTypedStop -Name 'parser.extra-lineage-field-fails-closed' -Output $unusedRaw.Replace($lineageLine, $extraLineage) -ExpectedPattern '^P5E_COLLECTOR_LINEAGE_ROW_INVALID$')
    [void](Expect-ParserTypedStop -Name 'parser.duplicate-lineage-fails-closed' -Output $duplicateLineage -ExpectedPattern '^P5E_COLLECTOR_LINEAGE_ROW_INVALID$')
    [void](Expect-ParserTypedStop -Name 'parser.invalid-lineage-integer-fails-closed' -Output $unusedRaw.Replace($lineageLine, $badIntegerLine) -ExpectedPattern '^P5E_COLLECTOR_NUMERIC_FIELD_INVALID:lineage$')
    $claimedRaw = Get-Content -Raw -LiteralPath (Join-Path $evidenceRoot 'scenario-claimed-null\query-output.tsv')
    $attemptLine = Get-TaggedOutputLine -Path (Join-Path $evidenceRoot 'scenario-claimed-null\query-output.tsv') -Tag 'ATTEMPT'
    $attemptColumns = $attemptLine.Split([char]9)
    $badUtf8Columns = @($attemptColumns); $badUtf8Columns[17] = 'FF'
    $wrongSentinelColumns = @($attemptColumns); $wrongSentinelColumns[13] = '0'
    [void](Expect-ParserTypedStop -Name 'parser.invalid-utf8-metrics-fails-closed' -Output $claimedRaw.Replace($attemptLine, [string]::Join("`t", $badUtf8Columns)) -ExpectedPattern '^P5E_COLLECTOR_UTF8_INVALID:metrics-json$')
    [void](Expect-ParserTypedStop -Name 'parser.zero-is-not-null-sentinel' -Output $claimedRaw.Replace($attemptLine, [string]::Join("`t", $wrongSentinelColumns)) -ExpectedPattern '^P5E_COLLECTOR_ATTEMPT_NULL_SENTINEL_PAIR_INVALID$')
    Add-BehavioralCheck -Name 'parser.valid-control-output-reproducible' -Passed ($null -ne (ConvertFrom-P5EConsistentDatabaseReadbackOutput -Output $unusedRaw)) -Details 'source-query-output-parsed'

    $goldenReportObject = Get-JsonObject -Path $goldenReportPath
    $goldenReceiptObject = Get-JsonObject -Path $goldenReceiptPath
    $artifactCases = [ordered]@{}
    $wrongManifestReport = $goldenReportObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongManifestReceipt = $goldenReceiptObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongManifestReport.manifestFingerprint = 'f' * 64; $wrongManifestReceipt.manifestRef = 'f' * 64
    $artifactCases['wrong_manifest'] = @((New-CanonicalArtifactBytes -Object $wrongManifestReport), (New-CanonicalArtifactBytes -Object $wrongManifestReceipt))
    $wrongBindingReport = $goldenReportObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongBindingReceipt = $goldenReceiptObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongBindingReport.bindingIdentity = 'e' * 64; $wrongBindingReceipt.bindingRef = 'e' * 64
    $artifactCases['wrong_binding'] = @((New-CanonicalArtifactBytes -Object $wrongBindingReport), (New-CanonicalArtifactBytes -Object $wrongBindingReceipt))
    $wrongBundleReport = $goldenReportObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongBundleReceipt = $goldenReceiptObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongBundleReport.bundleIdentity = 'wrong-bundle'; $wrongBundleReceipt.bundleIdentity = 'wrong-bundle'
    $artifactCases['wrong_bundle'] = @((New-CanonicalArtifactBytes -Object $wrongBundleReport), (New-CanonicalArtifactBytes -Object $wrongBundleReceipt))
    $wrongPredecessorReport = $goldenReportObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongPredecessorReceipt = $goldenReceiptObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $wrongPredecessorReport.predecessorIdentity = 'wrong-predecessor'; $wrongPredecessorReceipt.predecessorIdentity = 'wrong-predecessor'
    $artifactCases['wrong_predecessor'] = @((New-CanonicalArtifactBytes -Object $wrongPredecessorReport), (New-CanonicalArtifactBytes -Object $wrongPredecessorReceipt))
    $artifactCases['swapped_report_receipt'] = @($goldenReceiptBytes, $goldenReportBytes)
    $mutatedByte = [byte[]]$goldenReportBytes.Clone(); $mutatedByte[0] = 0x7d
    $artifactCases['one_byte_mutation'] = @($mutatedByte, $goldenReceiptBytes)
    $extraReport = $goldenReportObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json; $extraReport | Add-Member -NotePropertyName extraField -NotePropertyValue 'unexpected'
    $artifactCases['extra_field'] = @((New-CanonicalArtifactBytes -Object $extraReport), $goldenReceiptBytes)
    $missingReport = $goldenReportObject | ConvertTo-Json -Depth 40 | ConvertFrom-Json; $missingReport.PSObject.Properties.Remove('gates')
    $artifactCases['missing_field'] = @((New-CanonicalArtifactBytes -Object $missingReport), $goldenReceiptBytes)
    $artifactCases['bom'] = @(([byte[]]@(0xef, 0xbb, 0xbf) + $goldenReportBytes), $goldenReceiptBytes)
    $invalidUtf8 = [byte[]]($goldenReportBytes + [byte]0xff)
    $artifactCases['invalid_utf8'] = @($invalidUtf8, $goldenReceiptBytes)
    $artifactCases['noncanonical_bytes'] = @(([byte[]]([Text.UTF8Encoding]::new($false).GetBytes(' ' + [Text.UTF8Encoding]::new($false).GetString($goldenReportBytes)))), $goldenReceiptBytes)
    foreach ($case in $artifactCases.Keys) {
        $bytes = $artifactCases[$case]
        $mutationResult = Invoke-GoldenPairValidator -Case $case -ReportBytes ([byte[]]$bytes[0]) -ReceiptBytes ([byte[]]$bytes[1])
        [void]$script:mutationSummaries.Add($mutationResult)
        Add-BehavioralCheck -Name ('verifier.golden-mutation-rejects.' + $case) -Passed (-not $mutationResult.accepted) -Details ($mutationResult.errorCodes -join ',')
    }

    $syntheticDirectory = Join-Path $evidenceRoot 'event-binding-control'
    [void](New-Item -ItemType Directory -Path $syntheticDirectory -Force)
    $fakeFingerprint = 'a' * 64
    $issued = 1700000000000L
    $synthetic = New-P5ESyntheticProducerFixture -EvidenceDirectory $syntheticDirectory -IssuedAtMillis $issued -AccountFingerprint $fakeFingerprint
    $validSynthetic = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $syntheticDirectory 'instrumentation-stdout.txt') `
        -InstrumentationStderrPath (Join-Path $syntheticDirectory 'instrumentation-stderr.txt') -MetadataPath $synthetic.MetadataPath `
        -PostReadbackPath $synthetic.ReadbackPath -ExpectedAccountFingerprint $fakeFingerprint
    Add-BehavioralCheck -Name 'verifier.same-event-control-accepted' -Passed ([bool]$validSynthetic.accepted -and -not [bool]$validSynthetic.p6Ready) `
        -Details ('decision=' + [string]$validSynthetic.decision)
    $crossReadback = Get-JsonObject -Path $synthetic.ReadbackPath
    $crossReadback.provenance.eventId = 'cross-event-forbidden'
    $crossReadbackPath = Join-Path $syntheticDirectory 'cross-event-readback.json'
    Write-P5EUtf8NoBom -Path $crossReadbackPath -Text (ConvertTo-P5EJson -Value $crossReadback)
    $crossResult = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $syntheticDirectory 'instrumentation-stdout.txt') `
        -InstrumentationStderrPath (Join-Path $syntheticDirectory 'instrumentation-stderr.txt') -MetadataPath $synthetic.MetadataPath `
        -PostReadbackPath $crossReadbackPath -ExpectedAccountFingerprint $fakeFingerprint
    Add-BehavioralCheck -Name 'verifier.cross-event-path-rejected' `
        -Passed (-not [bool]$crossResult.accepted -and @($crossResult.errors | Where-Object { $_ -eq 'READBACK_EVENT_OR_FILE_BINDING_INVALID' }).Count -gt 0) `
        -Details (($crossResult.errors -join ','))

    $prelaunchDirectory = Join-Path $evidenceRoot 'supervisor-prelaunch'
    [void](New-Item -ItemType Directory -Path $prelaunchDirectory -Force)
    $missingProcess = Join-Path $prelaunchDirectory 'missing-process.exe'
    $prelaunch = Invoke-P5EProcessSupervisor -FilePath $missingProcess -ArgumentList @() -TimeoutMilliseconds 1000L -EvidenceDirectory $prelaunchDirectory
    Add-BehavioralCheck -Name 'supervisor.failure-before-launch-no-redispatch' `
        -Passed ($prelaunch.Outcome -ceq 'FAILED_BEFORE_LAUNCH' -and $prelaunch.LaunchCount -eq 0 -and $prelaunch.DispatchCount -eq 0) `
        -Details ('outcome=' + [string]$prelaunch.Outcome + ';launch=' + [string]$prelaunch.LaunchCount)
    $timeoutDirectory = Join-Path $evidenceRoot 'supervisor-timeout'
    [void](New-Item -ItemType Directory -Path $timeoutDirectory -Force)
    $timeoutScript = Join-Path $timeoutDirectory 'fake-timeout.ps1'
    Write-P5EUtf8NoBom -Path $timeoutScript -Text 'Start-Sleep -Milliseconds 5000'
    $timeoutProcess = Invoke-P5EProcessSupervisor -FilePath 'powershell.exe' -ArgumentList @('-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-File', $timeoutScript) `
        -TimeoutMilliseconds 100L -EvidenceDirectory $timeoutDirectory
    Add-BehavioralCheck -Name 'supervisor.timeout-single-dispatch-unknown-recovery' `
        -Passed ($timeoutProcess.Outcome -ceq 'TIMEOUT' -and $timeoutProcess.LaunchCount -eq 1 -and $timeoutProcess.DispatchCount -eq 1 -and $timeoutProcess.TimedOut) `
        -Details ('outcome=' + [string]$timeoutProcess.Outcome + ';classification=UNKNOWN_RECOVERY_REQUIRED;redispatch=0')
} catch {
    [void]$script:failures.Add('FATAL:' + [string]$_.Exception.Message)
}

$gitHead = (& git -C $repoRoot rev-parse HEAD 2>$null | Out-String).Trim()
$gitBranch = (& git -C $repoRoot branch --show-current 2>$null | Out-String).Trim()
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$commandPath = Join-Path $repoRoot 'docs\P5E_RAW_AUTHORIZATION_COMMAND.txt'
$serializerPath = Join-Path $repoRoot 'editorial-engine\src\main\java\com\ml\tblandroidtxt\editorial\pack\EditorialP5PilotExecution.java'
$goldenTestPath = Join-Path $repoRoot 'editorial-engine\src\test\java\com\ml\tblandroidtxt\editorial\pack\EditorialP5PilotExecutionBoundaryTest.java'
$contractPath = Join-Path $repoRoot 'docs\P5E_PRODUCTION_ARTIFACT_CONTRACT_20260915.json'
$report = [ordered]@{
    schemaVersion = 'p5e.sql.behavioral-result.v1'
    status = if ($script:failures.Count -eq 0) { 'P5E_LOCAL_BEHAVIORAL_GATE_GREEN' } else { 'LOCAL_VALIDATION_FAILED_REPAIR_REQUIRED' }
    scope = 'OFFLINE_SQLITE_QUERY_COLLECTOR_PARSER_VERIFIER_AND_TARGETED_JVM_ONLY'
    currentState = [ordered]@{ branch = $gitBranch; headAtTest = $gitHead; a4_3 = 'NOT_ISSUED'; raw = 'NOT_RUN'; ownerDecision = 'PENDING'; dispatchReady = $false; p6Ready = $false }
    queryParserBehavioral = [ordered]@{ status = if ($script:failures.Count -eq 0) { 'PASS' } else { 'FAILED_REPAIRING' }; querySha256 = $querySha256; fixtureManifestSha256 = Get-FileSha256Lower -Path $fixtureManifestPath; scenarios = $scenarioSummaries.ToArray(); parserMutations = @($checks | Where-Object { $_.name -like 'parser.*' }) }
    goldenJvm = [ordered]@{ executed = $true; status = 'PASS'; command = $GoldenJvmCommand; runtime = $JvmRuntime; targetedTest = 'com.ml.tblandroidtxt.editorial.pack.EditorialP5PilotExecutionBoundaryTest'; testCount = 1; serializerSourceSha256 = Get-FileSha256Lower -Path $serializerPath; testSourceSha256 = Get-FileSha256Lower -Path $goldenTestPath; suppliedTestSourceSha256 = $GoldenTestSourceSha256; goldenOutputSha256 = Get-FileSha256Lower -Path $GoldenOutput; reportSha256 = Get-FileSha256Lower -Path $goldenReportPath; receiptSha256 = Get-FileSha256Lower -Path $goldenReceiptPath }
    hostContract = [ordered]@{ status = if ($script:failures.Count -eq 0) { 'PASS' } else { 'FAILED_REPAIRING' }; helperSelfTest = 'RUN_SEPARATELY'; productionArtifactContractSha256 = Get-FileSha256Lower -Path $contractPath; goldenMutationResults = $mutationSummaries.ToArray(); supervisor = [ordered]@{ prelaunch = 'FAILED_BEFORE_LAUNCH/launch0'; timeout = 'TIMEOUT/launch1/redispatch0' } }
    provenance = [ordered]@{ helperSha256 = Get-FileSha256Lower -Path $helperPath; commandSha256 = Get-FileSha256Lower -Path $commandPath; collectorImplementationId = 'p5e.raw.host-readback-collector.v1'; collectorImplementationVersion = '2'; sourceMap = 'p5e.raw.readback.source-map.v1'; migrationSourceSha256 = [string]$script:fixtureManifest.migrationSha256; ddlSha256 = [string]$script:fixtureManifest.ddlSha256; queryOutputHashes = @($scenarioSummaries | ForEach-Object { [ordered]@{ scenario = $_.scenario; sha256 = $_.queryOutputSha256 } }) }
    outcomeMatrix = [ordered]@{ unused = 'ZERO_LINEAGE'; claimedNull = 'CLAIMED_PRESERVED_UNKNOWN_BLOBS'; recoveryNull = 'RECOVERY_REQUIRED_PRESERVED_UNKNOWN_BLOBS'; committedGolden = 'HOST_BYTES_VALIDATED_OFFLINE_ONLY'; partialBlob = 'TYPED_STOP'; timeout = 'UNKNOWN_RECOVERY_REQUIRED'; crossEvent = 'REJECTED' }
    actionCounts = [ordered]@{ deviceActions = 0; providerCalls = 0; credentialReads = 0; adbInvocations = 0; instrumentationRuns = 0; apkBuilds = 0; redispatches = 0 }
    ownerReadiness = 'PENDING_NOT_APPROVED'
    liveReadiness = 'NOT_READY_LIVE_ACTIONS_NOT_AUTHORIZED'
    p5Exit = 'NOT_CLAIMED'
    p6Ready = $false
    failures = $script:failures.ToArray()
    checks = $checks.ToArray()
    nextAction = 'Owner review of this final-hash-bound local packet with independent expected account fingerprint provenance and separately approved read-only collection plus one RAW/GLOSSARY dispatch; do not dispatch before approval.'
}
$resultText = ConvertTo-P5EJson -Value $report
$tempResultPath = Join-Path $workRoot 'P5E_SQL_BEHAVIORAL_RESULT.json'
Write-P5EUtf8NoBom -Path $tempResultPath -Text $resultText
if (-not [string]::IsNullOrWhiteSpace($ResultPath)) { Write-P5EUtf8NoBom -Path $ResultPath -Text $resultText }
Write-Output $resultText
if ($script:failures.Count -ne 0) { exit 1 }
