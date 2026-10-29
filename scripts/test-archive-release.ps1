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

function Get-GitOutput {
    param([string[]]$GitArgs)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $text = (& git -C $work @GitArgs 2>$null | Out-String).Trim() } finally { $ErrorActionPreference = $previous }
    if ($LASTEXITCODE -ne 0) { throw "git $($GitArgs -join ' ') failed" }
    $text
}

function Invoke-Archive {
    param([string]$EventId, [string]$Checklist, [string]$Ver = $version, [string]$Ref = 'HEAD', [string[]]$Extra = @())
    $args = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $work 'scripts\archive-release.ps1'),
        '-Version', $Ver, '-EventId', $EventId, '-GitRef', $Ref,
        '-ApkPath', (Join-Path $work 'in\app.apk'), '-QaReportPath', (Join-Path $work 'in\QA.md'),
        '-ChangelogPath', (Join-Path $work 'in\CHANGELOG.md'), '-BuildStatePath', (Join-Path $work 'in\BUILD_STATE.md'),
        '-ReleaseNotesPath', (Join-Path $work 'in\RELEASE_NOTES.md'), '-PerfettoPaths', (Join-Path $work 'in\p.trace'),
        '-MacrobenchmarkPaths', (Join-Path $work 'in\m.json'), '-ScreenshotPaths', (Join-Path $work 'in\s.png'),
        '-VideoPaths', (Join-Path $work 'in\v.mp4'), '-ChecklistPath', $Checklist) + $Extra
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
    New-Item -ItemType Directory -Force (Join-Path $work 'app\src\main'), (Join-Path $work 'docs') | Out-Null
    [IO.File]::WriteAllText((Join-Path $work 'app\src\main\Main.kt'), 'production v1', (New-Object Text.UTF8Encoding $false))
    [IO.File]::WriteAllText((Join-Path $work 'in\extra.txt'), 'supplemental evidence', (New-Object Text.UTF8Encoding $false))
    Run-Git @('add', 'scripts', 'app')
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

    # desktop package: stored under pc/ in both archives, checked against the commit and its own hashes; every case above ran without it
    $headForPc = Get-GitOutput @('rev-parse', 'HEAD')
    function New-PcPackage {
        param([string]$Name, [string]$Commit, [switch]$BadHash, [switch]$NoMsi)
        $dir = Join-Path $work "in\$Name"
        New-Item -ItemType Directory -Force $dir | Out-Null
        $zip = Join-Path $dir 'TranslateBooks-PC-9.9.9.zip'
        $msi = Join-Path $dir 'TranslateBooks-PC-9.9.9.msi'
        [IO.File]::WriteAllText($zip, "synthetic pc zip $Name", (New-Object Text.UTF8Encoding $false))
        if (-not $NoMsi) { [IO.File]::WriteAllText($msi, "synthetic pc msi $Name", (New-Object Text.UTF8Encoding $false)) }
        $zipHash = (Get-FileHash $zip -Algorithm SHA256).Hash
        $msiHash = if ($NoMsi) { 'A' * 64 } else { (Get-FileHash $msi -Algorithm SHA256).Hash }
        if ($BadHash) { $msiHash = 'B' * 64 }
        $info = [ordered]@{ version = '9.9.9-pc'; commit = $Commit; packageSha256 = $zipHash; msiSha256 = $msiHash }
        [IO.File]::WriteAllText((Join-Path $dir 'BUILD_INFO.json'), ($info | ConvertTo-Json), (New-Object Text.UTF8Encoding $false))
        [IO.File]::WriteAllText((Join-Path $dir 'SHA256SUMS.txt'), "$zipHash  TranslateBooks-PC-9.9.9.zip`n$msiHash  TranslateBooks-PC-9.9.9.msi`n", (New-Object Text.UTF8Encoding $false))
        $dir
    }
    $pcGood = New-PcPackage 'pcgood' $headForPc
    $pcEvent = 'build-20261012-171500'
    Expect-Archived 'PC package with the archived commit' (Invoke-Archive $pcEvent $upTo9 -Extra @('-PcPackageDirectory', $pcGood)) $pcEvent
    foreach ($root in 'artifacts\releases', 'backup') {
        $pcDir = Join-Path $work "$root\v$version\$pcEvent\pc"
        foreach ($f in 'TranslateBooks-PC-9.9.9.zip', 'TranslateBooks-PC-9.9.9.msi', 'BUILD_INFO.json', 'SHA256SUMS.txt') {
            if (-not (Test-Path (Join-Path $pcDir $f))) { $failures.Add("PC package: $f missing under pc/ in $root") }
        }
    }
    $pcArtifact = Join-Path $work "artifacts\releases\v$version\$pcEvent"
    if ((Get-Content -Raw (Join-Path $pcArtifact 'ARCHIVE_MANIFEST.md')) -notmatch [regex]::Escape($headForPc)) { $failures.Add('PC package: manifest does not name the PC commit') }
    if ((Get-Content -Raw (Join-Path $pcArtifact 'SHA256SUMS.txt')) -notmatch 'pc/TranslateBooks-PC-9.9.9.msi') { $failures.Add('PC package: msi not covered by the checksum manifest') }
    $pcOther = New-PcPackage 'pcother' ('0' * 40)
    Expect-Refused 'PC package built from another commit' (Invoke-Archive 'build-20261012-171600' $upTo9 -Extra @('-PcPackageDirectory', $pcOther)) 'PC package was built from'
    $pcBad = New-PcPackage 'pcbad' $headForPc -BadHash
    Expect-Refused 'PC msi hash differs from BUILD_INFO' (Invoke-Archive 'build-20261012-171700' $upTo9 -Extra @('-PcPackageDirectory', $pcBad)) 'msi SHA-256 does not match'
    $pcNoMsi = New-PcPackage 'pcnomsi' $headForPc -NoMsi
    Expect-Refused 'PC package without an msi' (Invoke-Archive 'build-20261012-171800' $upTo9 -Extra @('-PcPackageDirectory', $pcNoMsi)) 'exactly one TranslateBooks-PC'
    Expect-Refused 'missing PC package folder' (Invoke-Archive 'build-20261012-171900' $upTo9 -Extra @('-PcPackageDirectory', (Join-Path $work 'in\absent-pc'))) 'Missing PC package folder'
    foreach ($left in 'build-20261012-171600', 'build-20261012-171700', 'build-20261012-171800', 'build-20261012-171900') {
        foreach ($root in 'artifacts\releases', 'backup') {
            if (Test-Path (Join-Path $work "$root\v$version\$left")) { $failures.Add("refused PC event $left left a payload in $root") }
        }
    }

    # reused APK: production commit is c1, the archived QA source is a later commit that only touches docs
    $production = Get-GitOutput @('rev-parse', 'HEAD')
    $productionShort = $production.Substring(0, 12)
    [IO.File]::WriteAllText((Join-Path $work 'docs\note.txt'), 'qa note', (New-Object Text.UTF8Encoding $false))
    Run-Git @('add', 'docs')
    Run-Git @('commit', '-q', '-m', 'docs only')
    $qaSource = Get-GitOutput @('rev-parse', 'HEAD')
    $reuse = @('-ProductionGitRef', $production, '-SupplementalPaths', (Join-Path $work 'in\extra.txt'))

    $reuseEvent = 'build-20261012-170000'
    Expect-Archived 'reused APK, docs-only QA commit' (Invoke-Archive $reuseEvent $upTo9 -Extra $reuse) $reuseEvent
    $artifactDir = Join-Path $work "artifacts\releases\v$version\$reuseEvent"
    $backupDir = Join-Path $work "backup\v$version\$reuseEvent"
    $prodZip = Join-Path $artifactDir "supplemental\production_apk_source_$productionShort.zip"
    $qaZip = Join-Path $artifactDir 'project_source_v9.9.9.zip'
    $refZip = Join-Path $work 'ref-production.zip'
    & git -C $work archive --format=zip "--output=$refZip" $production
    if (-not (Test-Path $prodZip)) { $failures.Add('reused APK: production source ZIP missing') }
    elseif ((Get-FileHash $prodZip).Hash -ne (Get-FileHash $refZip).Hash) { $failures.Add('reused APK: production ZIP is not the exact archive of the production commit') }
    & git -C $work archive --format=zip "--output=$refZip.qa" $qaSource
    if (-not (Test-Path $qaZip)) { $failures.Add('reused APK: QA source ZIP missing') }
    elseif ((Get-FileHash $qaZip).Hash -ne (Get-FileHash "$refZip.qa").Hash) { $failures.Add('reused APK: QA ZIP is not the exact archive of the QA commit') }
    if ((Get-FileHash $prodZip).Hash -eq (Get-FileHash $qaZip).Hash) { $failures.Add('reused APK: production and QA ZIP are identical, the two identities are not separate') }
    if (-not (Test-Path (Join-Path $artifactDir 'supplemental\extra.txt'))) { $failures.Add('reused APK: supplemental file missing') }
    $manifestText = Get-Content -Raw (Join-Path $artifactDir 'ARCHIVE_MANIFEST.md')
    if ($manifestText -notmatch [regex]::Escape("Production APK source commit: ``$production``")) { $failures.Add('reused APK: manifest does not name the production commit') }
    if ($manifestText -notmatch [regex]::Escape("Git commit: ``$qaSource``")) { $failures.Add('reused APK: manifest does not name the QA commit') }
    if ((Get-Content -Raw (Join-Path $artifactDir 'SHA256SUMS.txt')) -ne (Get-Content -Raw (Join-Path $backupDir 'SHA256SUMS.txt'))) { $failures.Add('reused APK: artifact and backup checksums differ') }
    if ((Get-Content -Raw (Join-Path $artifactDir 'SHA256SUMS.txt')) -notmatch 'supplemental/production_apk_source_') { $failures.Add('reused APK: production ZIP not covered by the checksum manifest') }

    # a production commit that is not an ancestor, an unknown ref, and a supplemental name that collides are refused with no payload
    Expect-Refused 'production commit newer than the archived ref' (Invoke-Archive 'build-20261012-170100' $upTo9 -Ref 'HEAD~1' -Extra @('-ProductionGitRef', 'HEAD')) 'is not an ancestor'
    Expect-Refused 'unknown production ref' (Invoke-Archive 'build-20261012-170200' $upTo9 -Extra @('-ProductionGitRef', 'no-such-ref')) 'does not resolve'
    $colliding = Join-Path $work "in\production_apk_source_$productionShort.zip"
    [IO.File]::WriteAllText($colliding, 'not a zip', (New-Object Text.UTF8Encoding $false))
    Expect-Refused 'supplemental colliding with the production ZIP' (Invoke-Archive 'build-20261012-170300' $upTo9 -Extra @('-ProductionGitRef', $production, '-SupplementalPaths', $colliding)) 'Duplicate archive filename'
    Expect-Refused 'missing supplemental file' (Invoke-Archive 'build-20261012-170400' $upTo9 -Extra @('-SupplementalPaths', (Join-Path $work 'in\absent.txt'))) 'Missing required supplemental'

    # production inputs changed after the production commit: the APK cannot be reused
    [IO.File]::WriteAllText((Join-Path $work 'app\src\main\Main.kt'), 'production v2', (New-Object Text.UTF8Encoding $false))
    Run-Git @('add', 'app')
    Run-Git @('commit', '-q', '-m', 'production change')
    Expect-Refused 'production inputs differ' (Invoke-Archive 'build-20261012-170500' $upTo9 -Extra @('-ProductionGitRef', $production)) 'Production inputs differ'
    foreach ($left in 'build-20261012-170100', 'build-20261012-170200', 'build-20261012-170300', 'build-20261012-170400', 'build-20261012-170500') {
        foreach ($root in 'artifacts\releases', 'backup') {
            if (Test-Path (Join-Path $work "$root\v$version\$left")) { $failures.Add("refused event $left left a payload in $root") }
        }
    }
    if (Test-Path (Join-Path $work '.archive-staging')) {
        if (@(Get-ChildItem (Join-Path $work '.archive-staging')).Count -gt 0) { $failures.Add('archive staging was not cleaned') }
    }
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
