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
    [string[]]$VideoPaths
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

$resolvedCommit = (& git rev-parse "$GitRef^{}").Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolvedCommit)) {
    throw "Git ref does not resolve: $GitRef"
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

    $sourceZipName = "project_source_v$Version.zip"
    $sourceZip = Join-Path $artifactStaging $sourceZipName
    & git archive --format=zip "--output=$sourceZip" $GitRef
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $sourceZip -PathType Leaf)) {
        throw "Failed to create source snapshot from $GitRef"
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
    [IO.File]::WriteAllLines((Join-Path $artifactStaging 'ARCHIVE_MANIFEST.md'), $manifest, [Text.UTF8Encoding]::new($false))

    $checksumLines = Get-ChildItem -LiteralPath $artifactStaging -Recurse -File |
        Where-Object { $_.Name -ne 'SHA256SUMS.txt' } |
        Sort-Object FullName |
        ForEach-Object {
            $relativePath = [IO.Path]::GetRelativePath($artifactStaging, $_.FullName).Replace('\', '/')
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
