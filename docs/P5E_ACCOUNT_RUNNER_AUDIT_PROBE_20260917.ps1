param([string]$OutputPath = (Join-Path $PSScriptRoot 'P5E_ACCOUNT_RUNNER_AUDIT_RESULT_20260917.json'))
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$path = Join-Path $repo 'scripts\p5e-account-check.ps1'
$source = [IO.File]::ReadAllText($path)
# Execute only the actual outcome-classification slice. Never invoke the runner,
# helper, process launch, environment read, ADB or credential path.
$start = $source.IndexOf('$stdout = [string]$run.stdout', [StringComparison]::Ordinal)
$end = $source.IndexOf('$outcome = [ordered]@{', $start, [StringComparison]::Ordinal)
if ($start -lt 0 -or $end -le $start) { throw 'SOURCE_CLASSIFIER_SLICE_MISSING' }
$classify = [scriptblock]::Create($source.Substring($start, $end - $start) + "`n" + '[pscustomobject]@{ outcome=$typedOutcome; result=$result }')
$cases = @(
    @{name='valid_control'; text="INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nOK (1 test)`nINSTRUMENTATION_CODE: -1"; accept=$true},
    @{name='match_then_failure'; text="INSTRUMENTATION_STATUS: p5e.account.result=MATCH`nFAILURES!!!`nTests run: 1, Failures: 1`nINSTRUMENTATION_CODE: 0"; accept=$false},
    @{name='truncated_after_match'; text='INSTRUMENTATION_STATUS: p5e.account.result=MATCH'; accept=$false},
    @{name='wrong_test_identity'; text="INSTRUMENTATION_STATUS: class=other.Test`nINSTRUMENTATION_STATUS: test=otherMethod`nINSTRUMENTATION_STATUS: p5e.account.result=MATCH`nOK (1 test)`nINSTRUMENTATION_CODE: -1"; accept=$false},
    @{name='token_suffix'; text="INSTRUMENTATION_STATUS: p5e.account.result=MATCH_EXTRA`nOK (1 test)`nINSTRUMENTATION_CODE: -1"; accept=$false},
    @{name='duplicate_control'; text="p5e.account.result=MATCH`np5e.account.result=MATCH"; accept=$false},
    @{name='missing_control'; text='INSTRUMENTATION_FAILED: no test'; accept=$false}
)
$results = foreach ($case in $cases) {
    $run = [pscustomobject]@{stdout=$case.text; redactionPass=$true; launchCount=1; timedOut=$false; exitCode=0}
    $observed = & $classify
    $accepted = $observed.outcome -eq 'ACCOUNT_CHECK_COMPLETED'
    [ordered]@{name=$case.name; expectedAccepted=$case.accept; actualAccepted=$accepted; conforms=($accepted -eq $case.accept); outcome=$observed.outcome}
}
$runnerLiteral = [regex]::Match($source, '(?m)^\$testRunner = ''([^'']+)''').Groups[1].Value
$report = [ordered]@{
    baseline='c222b13cafbb0c58a868072b44ae6df17905d089'
    sourceSha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    scope='SOURCE_CLASSIFIER_SLICE_ONLY_SYNTHETIC_NO_PROCESS_LAUNCH_NO_ENVIRONMENT_READ'
    instrumentationComponent=[ordered]@{actual=$runnerLiteral; expected='com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner'; matches=($runnerLiteral -ceq 'com.ml.tblandroidtxt.test/androidx.test.runner.AndroidJUnitRunner')}
    classifierCases=@($results)
    classifierUnexpectedAcceptances=@($results | Where-Object { $_.actualAccepted -and -not $_.expectedAccepted }).Count
    expectedPassedAsInstrumentationArgument=$source.Contains("'-e', 'p5e_expected_endpoint_account_fingerprint', " + '$expected')
    decision='ACCOUNT_RUNNER_REPAIR_REQUIRED / EXPECTED_SOURCE_PENDING / A4.3_NOT_ISSUED / RAW_NOT_RUN / P6_NOT_READY'
    deviceActions=0
    providerCalls=0
    credentialReads=0
}
[IO.File]::WriteAllText($OutputPath, ($report | ConvertTo-Json -Depth 8) + "`n", [Text.UTF8Encoding]::new($false))
$report | ConvertTo-Json -Depth 8
if ($report.classifierUnexpectedAcceptances -gt 0 -or -not $report.instrumentationComponent.matches) { exit 2 }
