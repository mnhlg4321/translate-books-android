[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+(\.\d+)?$')]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$EventId,

    [Parameter(Mandatory = $true)]
    [string]$GitRef,

    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [Parameter(Mandatory = $true)]
    [string]$QaReportPath,

    [Parameter(Mandatory = $true)]
    [string]$ChangelogPath,

    [Parameter(Mandatory = $true)]
    [string]$BuildStatePath,

    [Parameter(Mandatory = $true)]
    [string]$ReleaseNotesPath,

    [Parameter(Mandatory = $true)]
    [string[]]$PerfettoPaths,

    [Parameter(Mandatory = $true)]
    [string[]]$MacrobenchmarkPaths,

    [Parameter(Mandatory = $true)]
    [string[]]$ScreenshotPaths,

    [Parameter(Mandatory = $true)]
    [string[]]$VideoPaths,

    [string]$ChecklistPath,

    # Optional: the commit the APK was really built from, when GitRef is a later QA-only source. The helper then proves that
    # commit is an ancestor of GitRef and that the production inputs are identical, and stores its own exact source ZIP
    # under supplemental/ beside the ZIP of GitRef.
    [string]$ProductionGitRef,

    # Optional extra evidence (QA test APK identity, review patches, ...), stored flat under supplemental/.
    [string[]]$SupplementalPaths = @(),

    # Optional: the folder of one desktop package built by scripts/build-desktop.ps1 -Msi (TranslateBooks-PC-*.zip, TranslateBooks-PC-*.msi,
    # BUILD_INFO.json, SHA256SUMS.txt). It is stored under pc/ in both archives. BUILD_INFO.json must name the archived commit (or the
    # production commit) and its hashes must match the files; without this parameter nothing changes.
    [string]$PcPackageDirectory
)

$ErrorActionPreference = 'Stop'

function Resolve-RequiredFile {
    param([string]$Path, [string]$Label)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Missing required $Label file: $Path"
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

function Copy-RequiredFile {
    param([string]$Source, [string]$DestinationDirectory)

    if (-not (Test-Path -LiteralPath $DestinationDirectory -PathType Container)) {
        New-Item -ItemType Directory -Path $DestinationDirectory | Out-Null
    }

    $destination = Join-Path $DestinationDirectory ([IO.Path]::GetFileName($Source))
    if (Test-Path -LiteralPath $destination) {
        throw "Duplicate archive filename: $destination"
    }

    Copy-Item -LiteralPath $Source -Destination $destination
}

$repositoryRoot = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repositoryRoot)) {
    throw 'Run this script inside the project Git repository.'
}
$repositoryRoot = (Resolve-Path -LiteralPath $repositoryRoot).Path

if ([version]$Version -ge [version]'4.8') {
    if ([string]::IsNullOrWhiteSpace($ChecklistPath)) {
        throw 'ChecklistPath is required for v4.8 and later.'
    }

    $workflowVerifier = Join-Path $repositoryRoot 'scripts\verify-release-workflow.ps1'
    # Exactly two event forms exist for v4.8 and later; anything else is refused before any gate or copy:
    #   tag-v<Version>         archived after the tag, so steps 01-10 must be complete (PreBackup)
    #   build-YYYYMMDD-HHMMSS  a release candidate archived before the tag, so steps 01-09 are enough (PreTag); it can never be
    #                          satisfied by ticking the Tag step early
    $buildEventTime = [datetime]::MinValue
    $workflowGate = if ($EventId -ceq "tag-v$Version") {
        'PreBackup'
    }
    elseif ($EventId -cmatch '^build-\d{8}-\d{6}$' -and
        [datetime]::TryParseExact($EventId.Substring(6), 'yyyyMMdd-HHmmss', [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::None, [ref]$buildEventTime)) {
        'PreTag'
    }
    else {
        throw "EventId '$EventId' is not allowed for v$Version. Use 'build-YYYYMMDD-HHMMSS' (release candidate) or 'tag-v$Version' (after the tag)."
    }
    & $workflowVerifier -ChecklistPath $ChecklistPath -Gate $workflowGate -ExpectedVersion $Version
}

$resolvedCommit = (& git rev-parse "$GitRef^{}").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolvedCommit)) {
    throw "Git ref does not resolve: $GitRef"
}

# Everything that decides the production APK. If any of it differs between the two commits the APK cannot be reused.
$productionInputPaths = @('app/src/main', 'editorial-engine/src/main', 'app/build.gradle', 'build.gradle', 'settings.gradle', 'gradle', 'macrobenchmark')
$productionCommit = $null
if (-not [string]::IsNullOrWhiteSpace($ProductionGitRef)) {
    $productionResolved = & git rev-parse --verify "$ProductionGitRef^{commit}"
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace([string]$productionResolved)) {
        throw "Production git ref does not resolve: $ProductionGitRef"
    }
    $productionCommit = ([string]$productionResolved).Trim()
    & git merge-base --is-ancestor $productionCommit $resolvedCommit
    if ($LASTEXITCODE -ne 0) {
        throw "Production commit $productionCommit is not an ancestor of $resolvedCommit"
    }
    & git diff --quiet $productionCommit $resolvedCommit -- @productionInputPaths
    if ($LASTEXITCODE -eq 1) {
        throw "Production inputs differ between $productionCommit and $resolvedCommit; the APK cannot be reused, build it from the new source"
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Could not compare production inputs between $productionCommit and $resolvedCommit"
    }
}

$apk = Resolve-RequiredFile $ApkPath 'APK'
$qaReport = Resolve-RequiredFile $QaReportPath 'QA report'
$changelog = Resolve-RequiredFile $ChangelogPath 'CHANGELOG'
$buildState = Resolve-RequiredFile $BuildStatePath 'BUILD_STATE'
$releaseNotes = Resolve-RequiredFile $ReleaseNotesPath 'RELEASE_NOTES'
$perfetto = @($PerfettoPaths | ForEach-Object { Resolve-RequiredFile $_ 'Perfetto' })
$macrobenchmark = @($MacrobenchmarkPaths | ForEach-Object { Resolve-RequiredFile $_ 'Macrobenchmark' })
$screenshots = @($ScreenshotPaths | ForEach-Object { Resolve-RequiredFile $_ 'screenshot' })
$videos = @($VideoPaths | ForEach-Object { Resolve-RequiredFile $_ 'video' })
$supplemental = @($SupplementalPaths | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { Resolve-RequiredFile $_ 'supplemental' })

$pcFiles = @()
$pcInfoObject = $null
if (-not [string]::IsNullOrWhiteSpace($PcPackageDirectory)) {
    if (-not (Test-Path -LiteralPath $PcPackageDirectory -PathType Container)) { throw "Missing PC package folder: $PcPackageDirectory" }
    $pcFolder = (Resolve-Path -LiteralPath $PcPackageDirectory).Path
    $pcZip = @(Get-ChildItem -LiteralPath $pcFolder -Filter 'TranslateBooks-PC-*.zip' -File)
    $pcMsi = @(Get-ChildItem -LiteralPath $pcFolder -Filter 'TranslateBooks-PC-*.msi' -File)
    if ($pcZip.Count -ne 1) { throw "PC package folder must hold exactly one TranslateBooks-PC-*.zip (found $($pcZip.Count))" }
    if ($pcMsi.Count -ne 1) { throw "PC package folder must hold exactly one TranslateBooks-PC-*.msi (found $($pcMsi.Count))" }
    $pcInfoPath = Resolve-RequiredFile (Join-Path $pcFolder 'BUILD_INFO.json') 'PC BUILD_INFO.json'
    $pcSumsPath = Resolve-RequiredFile (Join-Path $pcFolder 'SHA256SUMS.txt') 'PC SHA256SUMS.txt'
    $pcInfoObject = Get-Content -LiteralPath $pcInfoPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace([string]$pcInfoObject.commit)) { throw 'PC BUILD_INFO.json has no commit' }
    $pcCommit = ([string]$pcInfoObject.commit).Trim()
    if ($pcCommit -ne $resolvedCommit -and (-not $productionCommit -or $pcCommit -ne $productionCommit)) {
        throw "PC package was built from $pcCommit, which is neither the archived commit $resolvedCommit nor the production commit"
    }
    if ((Get-FileHash -LiteralPath $pcZip[0].FullName -Algorithm SHA256).Hash -ne ([string]$pcInfoObject.packageSha256).ToUpperInvariant()) { throw 'PC zip SHA-256 does not match BUILD_INFO.json' }
    if ((Get-FileHash -LiteralPath $pcMsi[0].FullName -Algorithm SHA256).Hash -ne ([string]$pcInfoObject.msiSha256).ToUpperInvariant()) { throw 'PC msi SHA-256 does not match BUILD_INFO.json' }
    $pcFiles = @($pcZip[0].FullName, $pcMsi[0].FullName, $pcInfoPath, $pcSumsPath)
}

if ($perfetto.Count -eq 0 -or $macrobenchmark.Count -eq 0 -or $screenshots.Count -eq 0 -or $videos.Count -eq 0) {
    throw 'Perfetto, Macrobenchmark, screenshots, and video must each contain at least one file.'
}

$artifactDestination = Join-Path $repositoryRoot "artifacts\releases\v$Version\$EventId"
$backupDestination = Join-Path $repositoryRoot "backup\v$Version\$EventId"

if (Test-Path -LiteralPath $artifactDestination) {
    throw "Artifact archive already exists; refusing to overwrite: $artifactDestination"
}
if (Test-Path -LiteralPath $backupDestination) {
    throw "Backup archive already exists; refusing to overwrite: $backupDestination"
}

$stagingRoot = Join-Path $repositoryRoot ".archive-staging\$([guid]::NewGuid().ToString('N'))"
$artifactStaging = Join-Path $stagingRoot 'artifact'
$backupStaging = Join-Path $stagingRoot 'backup'

try {
    New-Item -ItemType Directory -Path $artifactStaging | Out-Null

    Copy-RequiredFile $apk $artifactStaging
    Copy-RequiredFile $qaReport $artifactStaging
    Copy-RequiredFile $changelog $artifactStaging
    Copy-RequiredFile $buildState $artifactStaging
    Copy-RequiredFile $releaseNotes $artifactStaging

    foreach ($path in $perfetto) {
        Copy-RequiredFile $path (Join-Path $artifactStaging 'perfetto')
    }
    foreach ($path in $macrobenchmark) {
        Copy-RequiredFile $path (Join-Path $artifactStaging 'macrobenchmark')
    }
    foreach ($path in $screenshots) {
        Copy-RequiredFile $path (Join-Path $artifactStaging 'screenshots')
    }
    foreach ($path in $videos) {
        Copy-RequiredFile $path (Join-Path $artifactStaging 'video')
    }

    foreach ($path in $supplemental) {
        Copy-RequiredFile $path (Join-Path $artifactStaging 'supplemental')
    }

    foreach ($path in $pcFiles) {
        Copy-RequiredFile $path (Join-Path $artifactStaging 'pc')
    }

    $sourceZipName = "project_source_v$Version.zip"
    $sourceZip = Join-Path $artifactStaging $sourceZipName
    & git archive --format=zip "--output=$sourceZip" $GitRef
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $sourceZip -PathType Leaf)) {
        throw "Failed to create source snapshot from $GitRef"
    }

    $productionZipName = $null
    if ($productionCommit) {
        $productionZipName = "production_apk_source_$($productionCommit.Substring(0, 12)).zip"
        $productionZipDirectory = Join-Path $artifactStaging 'supplemental'
        if (-not (Test-Path -LiteralPath $productionZipDirectory -PathType Container)) {
            New-Item -ItemType Directory -Path $productionZipDirectory | Out-Null
        }
        $productionZip = Join-Path $productionZipDirectory $productionZipName
        if (Test-Path -LiteralPath $productionZip) {
            throw "Duplicate archive filename: $productionZip"
        }
        & git archive --format=zip "--output=$productionZip" $productionCommit
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $productionZip -PathType Leaf)) {
            throw "Failed to create production source snapshot from $productionCommit"
        }
    }

    $manifest = @(
        '# Immutable Release Archive',
        '',
        "- Version: ``$Version``",
        "- Event: ``$EventId``",
        "- Git ref: ``$GitRef``",
        "- Git commit: ``$resolvedCommit``",
        "- Created: ``$([DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz'))``",
        '- Overwrite policy: `FORBIDDEN`'
    )
    if ($productionCommit) {
        $manifest += @(
            '',
            "- Source snapshot of this event (``$sourceZipName``): final QA source ``$resolvedCommit``; it is NOT the commit the APK was built from.",
            "- Production APK source commit: ``$productionCommit`` (``supplemental/$productionZipName``, generated by this script).",
            "- Production inputs identical between the two commits: ``$($productionInputPaths -join ' ')``.",
            '- The APK was reused, not rebuilt.'
        )
    }
    if ($pcInfoObject) {
        $manifest += @(
            '',
            "- PC package (``pc/``): version ``$($pcInfoObject.version)``, commit ``$($pcInfoObject.commit)``.",
            "- PC zip SHA-256: ``$($pcInfoObject.packageSha256)``; PC msi SHA-256: ``$($pcInfoObject.msiSha256)``."
        )
    }
    [IO.File]::WriteAllLines((Join-Path $artifactStaging 'ARCHIVE_MANIFEST.md'), $manifest, [Text.UTF8Encoding]::new($false))

    $checksumLines = Get-ChildItem -LiteralPath $artifactStaging -Recurse -File |
        Where-Object { $_.Name -ne 'SHA256SUMS.txt' } |
        Sort-Object FullName |
        ForEach-Object {
            $relativePath = $_.FullName.Substring($artifactStaging.Length).TrimStart([char[]]'\/').Replace('\', '/')
            $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
            "$hash  $relativePath"
        }
    [IO.File]::WriteAllLines((Join-Path $artifactStaging 'SHA256SUMS.txt'), $checksumLines, [Text.UTF8Encoding]::new($false))

    Copy-Item -LiteralPath $artifactStaging -Destination $backupStaging -Recurse

    New-Item -ItemType Directory -Path (Split-Path $artifactDestination -Parent) -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path $backupDestination -Parent) -Force | Out-Null
    Move-Item -LiteralPath $artifactStaging -Destination $artifactDestination
    Move-Item -LiteralPath $backupStaging -Destination $backupDestination

    Write-Output "Artifact archive: $artifactDestination"
    Write-Output "Backup archive:   $backupDestination"
    Write-Output "Source snapshot:  $sourceZipName"
    Write-Output "Git commit:       $resolvedCommit"
}
finally {
    $stagingParent = Join-Path $repositoryRoot '.archive-staging'
    if ((Test-Path -LiteralPath $stagingRoot) -and $stagingRoot.StartsWith($stagingParent, [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}
