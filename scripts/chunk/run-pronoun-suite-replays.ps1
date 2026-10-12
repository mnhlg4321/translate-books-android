[CmdletBinding()]
param(
    # worktree whose current rules are replayed (defaults to the repository this script lives in)
    [string]$Worktree = '',
    # private directory that holds known-misses.json / clean-controls.json (built by pronoun_miss_diagnosis.py suite)
    [string]$PrivateDir = 'D:\P5E-private\v419-pronoun',
    [string]$InputRoot = 'D:\P5E-private\q2-inputs',
    [string]$RunRoot = 'D:\P5E-private\chunk-runs\C1\live',
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

# Offline, USD 0: replays the model answers stored by the live runs of 006 (C6E), 008 (C7A) and 011 (C7D) through the CURRENT
# finder, parser, applier and guard of $Worktree, then compares every point with the recorded outcome of the suite. A change that
# alters a clean control, or lets a known wrong fix pass again, makes the script exit 1. Book text stays in the private folders.
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrWhiteSpace($Worktree)) { $Worktree = (Resolve-Path (Join-Path $here '..\..')).Path }
$jobs = @(
    @{ Run = '006-C6E'; Chapter = '006'; Responses = "$RunRoot\006-fb219146-9ca9-45c0-8735-5286fcc58946\results\fx-a01\responses" },
    @{ Run = '008-C7A'; Chapter = '008'; Responses = "$RunRoot\008-38b8be8b-1c23-48c7-9896-9672ded7578c\results\fx-a01\responses" },
    @{ Run = '011-C7D'; Chapter = '011'; Responses = "$RunRoot\011-25ce0f9a-0d79-4b5e-8de4-5c2a17c92463\results\fx-a01\responses" }
)
$out = Join-Path $PrivateDir 'replay-current'
New-Item -ItemType Directory -Force $out | Out-Null
$env:JAVA_HOME = $JavaHome
$env:C6_INPUT_ROOT = $InputRoot
Push-Location $Worktree
try {
    $replayArgs = @()
    foreach ($job in $jobs) {
        $tsv = Join-Path $out ("$($job.Run).tsv")
        $env:C6_REPLAY_CHAPTER = $job.Chapter
        $env:C6_REPLAY_RESPONSES = $job.Responses
        $env:C6_REPLAY_OUT = $tsv
        $log = Join-Path $out ("$($job.Run).gradle.log")
        cmd /c ".\gradlew.bat :app:testDebugUnitTest --offline --rerun --tests *EditorialPairC6CorpusMeasurementTest.replayStoredAnswers* -q > `"$log`" 2>&1"
        if ($LASTEXITCODE -ne 0) { throw "replay failed for $($job.Run); see $log" }
        $replayArgs += @('--replay', "$($job.Run)=$tsv")
    }
    $env:PYTHONIOENCODING = 'utf-8'
    $env:PYTHONDONTWRITEBYTECODE = '1'
    & py -3 -B (Join-Path $here 'pronoun_miss_diagnosis.py') verify-replay --known (Join-Path $PrivateDir 'known-misses.json') `
        --clean (Join-Path $PrivateDir 'clean-controls.json') @replayArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
    Remove-Item Env:C6_INPUT_ROOT, Env:C6_REPLAY_CHAPTER, Env:C6_REPLAY_RESPONSES, Env:C6_REPLAY_OUT -ErrorAction SilentlyContinue
}
