$ErrorActionPreference = 'Stop'
# Offline test of scripts/archive-release.ps1 event routing. Everything happens in a throw-away Git repository under %TEMP% with
# tiny files; the real artifacts/ and backup/ trees are never touched.

$realRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$work = Join-Path ([IO.Path]::GetTempPath()) ("archive-release-test-" + [guid]::NewGuid().ToString('N'))
$failures = New-Object System.Collections.Generic.List[string]
$version = '9.9.9'

function Run-Git {
    param([string[]]$GitArgs)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue' # git writes warnings to stderr; only its exit code decides
    try { & git -C $work @GitArgs 2>&1 | Out-Null } finally { $ErrorActionPreference = $previous }
    if ($LASTEXITCODE -ne 0) { throw "git $($GitArgs -join ' ') failed" }
}

function New-Checklist {
    param([int]$CheckedThrough, [string]$ChecklistVersion = "v$version")
    $titles = 'Read BUILD_STATE.md', 'Read WORKSPACE_SNAPSHOT.md', 'Confirm current commit', 'Create new feature branch once', 'Develop', 'Regression', 'Build', 'QA', 'Commit', 'Tag', 'Backup', 'Update BUILD_STATE.md', 'Update WORKSPACE_SNAPSHOT.md', 'Export artifact'
    $lines = @('# Release Development Checklist', '', "- Version: ``$ChecklistVersion``", '- Workflow status: `IN_PROGRESS`', '')
    for ($i = 1; $i -le 14; $i++) {
        $mark = if ($i -le $CheckedThrough) { 'x' } else { ' ' }
        $lines += ('- [{0}] {1:00} {2}' -f $mark, $i, $titles[$i - 1])
        $lines += '  - Evidence: synthetic test evidence'
        $lines += ''
    }
    $path = Join-Path $work ("checklist-$CheckedThrough-$ChecklistVersion.md")
    [IO.File]::WriteAllText($path, ($lines -join "`n"), (New-Object Text.UTF8Encoding $false))
    $path
}

function Invoke-Archive {
    param([string]$EventId, [string]$Checklist, [string]$Ver = $version)
    $args = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $work 'scripts\archive-release.ps1'),
        '-Version', $Ver, '-EventId', $EventId, '-GitRef', 'HEAD',
        '-ApkPath', (Join-Path $work 'in\app.apk'), '-QaReportPath', (Join-Path $work 'in\QA.md'),
        '-ChangelogPath', (Join-Path $work 'in\CHANGELOG.md'), '-BuildStatePath', (Join-Path $work 'in\BUILD_STATE.md'),
        '-ReleaseNotesPath', (Join-Path $work 'in\RELEASE_NOTES.md'), '-PerfettoPaths', (Join-Path $work 'in\p.trace'),
        '-MacrobenchmarkPaths', (Join-Path $work 'in\m.json'), '-ScreenshotPaths', (Join-Path $work 'in\s.png'),
        '-VideoPaths', (Join-Path $work 'in\v.mp4'), '-ChecklistPath', $Checklist)
    Push-Location $work
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue' # the child's refusal text arrives on stderr; its exit code and text are what is asserted
    try {
        $output = & powershell.exe @args 2>&1 | Out-String
        [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $output }
    }
    finally { $ErrorActionPreference = $previous; Pop-Location }
}

function Expect-Refused {
    param([string]$Label, $Result, [string]$Pattern)
    if ($Result.ExitCode -eq 0) { $failures.Add("$Label : expected a refusal, the archive succeeded"); return }
    if ($Result.Output -notmatch $Pattern) { $failures.Add("$Label : refused for another reason: " + $Result.Output.Trim()) }
}

function Expect-Archived {
    param([string]$Label, $Result, [string]$EventId)
    if ($Result.ExitCode -ne 0) { $failures.Add("$Label : expected success, got: " + $Result.Output.Trim()); return }
    foreach ($root in 'artifacts\releases', 'backup') {
        $dir = Join-Path $work "$root\v$version\$EventId"
        if (-not (Test-Path (Join-Path $dir 'project_source_v9.9.9.zip'))) { $failures.Add("$Label : missing payload in $root") }
    }
}

try {
    New-Item -ItemType Directory -Force (Join-Path $work 'scripts'), (Join-Path $work 'in') | Out-Null
    Copy-Item (Join-Path $realRoot 'scripts\archive-release.ps1') (Join-Path $work 'scripts')
    Copy-Item (Join-Path $realRoot 'scripts\verify-release-workflow.ps1') (Join-Path $work 'scripts')
    foreach ($f in 'app.apk', 'QA.md', 'CHANGELOG.md', 'BUILD_STATE.md', 'RELEASE_NOTES.md', 'p.trace', 'm.json', 's.png', 'v.mp4') {
        [IO.File]::WriteAllText((Join-Path $work "in\$f"), "synthetic $f", (New-Object Text.UTF8Encoding $false))
    }
    Run-Git @('init', '-q')
    Run-Git @('config', 'user.email', 'test@example.invalid')
    Run-Git @('config', 'user.name', 'archive test')
    Run-Git @('config', 'core.autocrlf', 'false')
    Run-Git @('add', 'scripts')
    Run-Git @('commit', '-q', '-m', 'synthetic')

    $upTo9 = New-Checklist -CheckedThrough 9
    $upTo8 = New-Checklist -CheckedThrough 8
    $upTo10 = New-Checklist -CheckedThrough 10

    # build event: steps 01-09 are enough, 01-08 are not
    Expect-Refused 'build event with step 09 missing' (Invoke-Archive 'build-20261012-153252' $upTo8) 'Step 09 is not complete'
    $first = Invoke-Archive 'build-20261012-153252' $upTo9
    Expect-Archived 'build event with steps 01-09' $first 'build-20261012-153252'
    Expect-Refused 'archive destination already exists' (Invoke-Archive 'build-20261012-153252' $upTo9) 'already exists'

    # tag event: steps 01-10 are required, and the event names this exact version
    Expect-Refused 'tag event with step 10 missing' (Invoke-Archive 'tag-v9.9.9' $upTo9) 'Step 10 is not complete'
    Expect-Archived 'tag event with steps 01-10' (Invoke-Archive 'tag-v9.9.9' $upTo10) 'tag-v9.9.9'
    Expect-Refused 'tag event for another version' (Invoke-Archive 'tag-v9.9.8' $upTo10) 'is not allowed'

    # any other event is refused before a gate or a copy, even with every step ticked
    foreach ($bad in 'qa-20261012-153252', 'dev-1', 'build-20261012', 'build-20261012-15325', 'build-20261012-153252-extra', 'Build-20261012-153252',
        'build-20261301-153252', 'build-20261012-256161', 'tag-9.9.9', 'tag-v9.9.9-rc1', 'TAG-v9.9.9', 'release') {
        Expect-Refused "event '$bad'" (Invoke-Archive $bad $upTo10) 'is not allowed'
        # the file system ignores case, so compare the exact directory names that exist
        if (@(Get-ChildItem (Join-Path $work "artifacts\releases\v$version") -Directory | Where-Object { $_.Name -ceq $bad }).Count -gt 0) { $failures.Add("event '$bad' left a payload") }
    }

    # the checklist must name the same version
    Expect-Refused 'checklist for another version' (Invoke-Archive 'build-20261012-160000' (New-Checklist -CheckedThrough 9 -ChecklistVersion 'v9.9.8')) 'does not match expected version'
    if (Test-Path (Join-Path $work "artifacts\releases\v$version\build-20261012-160000")) { $failures.Add('wrong-version checklist left a payload') }
}
finally {
    if ((Test-Path $work) -and $work.StartsWith([IO.Path]::GetTempPath(), [StringComparison]::OrdinalIgnoreCase)) {
        Get-ChildItem -LiteralPath $work -Recurse -Force | ForEach-Object { if ($_.Attributes -band [IO.FileAttributes]::ReadOnly) { $_.Attributes = 'Normal' } }
        Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}
'test-archive-release: all event routing cases passed'
