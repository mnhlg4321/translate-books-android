# Offline audit only. Loads declarations, never executes helper dispatch/self-test.
param([string]$OutputRoot)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$helper = Join-Path $repo 'scripts/p5e-raw-live-supervisor.ps1'
$expected = 'BEEFBB7733EED660B1F59435922D0594FBA1CBDED01B6C0B00482E786E456799'
if ((Get-FileHash -LiteralPath $helper).Hash -ne $expected) { throw 'BASELINE_CHANGED' }
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $repo ('evidence/p5e-provenance-review-' + [Guid]::NewGuid().ToString('N'))
}
if (Test-Path -LiteralPath $OutputRoot) { throw 'OUTPUT_MUST_BE_NEW' }
[void](New-Item -ItemType Directory -Path $OutputRoot)
$tokens=$null; $parseErrors=$null
$ast=[System.Management.Automation.Language.Parser]::ParseFile($helper,[ref]$tokens,[ref]$parseErrors)
if ($parseErrors.Count) { throw 'PARSE_FAILED' }
foreach ($statement in $ast.EndBlock.Statements) {
    if ($statement -is [System.Management.Automation.Language.AssignmentStatementAst]) {
        . ([scriptblock]::Create($statement.Extent.Text))
    } elseif ($statement -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
        $statement.Name -notin @('Invoke-P5EDispatch','Invoke-P5EVerifyOutcome','Invoke-P5ESelfTest','Invoke-P5EProcessSupervisor','Get-P5EAccountFingerprint')) {
        . ([scriptblock]::Create($statement.Extent.Text))
    }
}
$issued=1700000000000L
$fakeAccount='a'*64
$results=@()
foreach ($case in @('valid_control','missing_receipt_control','observed_before_run','consumed_after_expiry','artifact_manifest_mismatch','wrong_evidence_directory')) {
    $dir=Join-Path $OutputRoot $case
    [void](New-Item -ItemType Directory -Path $dir)
    $stdout=Join-Path $dir 'stdout.txt'; $stderr=Join-Path $dir 'stderr.txt'
    $metadataPath=Join-Path $dir 'metadata.json'; $readbackPath=Join-Path $dir 'readback.json'
    Write-P5EUtf8NoBom $stdout (New-P5EInstrumentationSuccessText)
    Write-P5EUtf8NoBom $stderr ''
    $run=[pscustomobject]@{ExitCode=0;LaunchCount=1;DispatchCount=1;TimedOut=$false;RedactionViolation=$false}
    $metadata=New-P5EHostMetadata -Run $run -IssuedAtMillis $issued -ExpiresAtMillis ($issued+180000) -EvidenceDirectory $dir
    $readback=New-P5EValidReadbackFixture -AccountFingerprint $fakeAccount -IssuedAtMillis $issued
    switch ($case) {
        'missing_receipt_control' { $readback.attempt.receiptByteLength=0 }
        'observed_before_run' { $readback.observedAtMillis=0 }
        'consumed_after_expiry' { $readback.authorizationReceipt.consumedAtMillis=$issued+180001 }
        'artifact_manifest_mismatch' { $readback.artifacts.receipt.manifestFingerprint='f'*64 }
        'wrong_evidence_directory' { $metadata.evidenceDirectory='SYNTHETIC_DIFFERENT_EVENT' }
    }
    Write-P5EUtf8NoBom $metadataPath (ConvertTo-P5EJson $metadata)
    Write-P5EUtf8NoBom $readbackPath (ConvertTo-P5EJson $readback)
    $result=Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath $stdout -InstrumentationStderrPath $stderr -MetadataPath $metadataPath -PostReadbackPath $readbackPath -ExpectedAccountFingerprint $fakeAccount
    $results += [pscustomobject]@{case=$case;expectedAccepted=($case -eq 'valid_control');actualAccepted=$result.accepted;decision=$result.decision;errors=$result.errors;p6Ready=$result.p6Ready}
}
$capture=Protect-P5ECaptureText -Text ('java.lang.AssertionError: expected:<' + ('a'*64) + '> but was:<' + ('b'*64) + '>')
$report=[ordered]@{baselineHelperSha256=$expected;scope='SYNTHETIC_ONLY_NO_DEVICE_NO_PROVIDER_NO_ENVIRONMENT_CREDENTIAL_READ';cases=$results;fingerprintAssertionProbe=@{redactionViolation=$capture.Violation;syntheticFingerprintStillPresent=$capture.Text.Contains('b'*64)};deviceActions=0;providerCalls=0}
Write-P5EUtf8NoBom (Join-Path $OutputRoot 'RESULT.json') (ConvertTo-P5EJson $report)
$report | ConvertTo-Json -Depth 8
