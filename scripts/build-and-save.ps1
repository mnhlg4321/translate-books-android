[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+(?:\.\d+)?(?:-[A-Za-z0-9-]+)?$')]
    [string]$Series = '4.14-dev',

    [ValidateNotNullOrEmpty()]
    [string]$Notes = 'Versioned development build.',

    [switch]$Install
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Write-Utf8File {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][AllowEmptyString()][string[]]$Lines
    )

    [IO.File]::WriteAllLines($Path, $Lines, [Text.UTF8Encoding]::new($false))
}

function Get-AndroidSdkPath {
    param([string]$RepositoryRoot)

    $localProperties = Join-Path $RepositoryRoot 'local.properties'
    if (-not (Test-Path -LiteralPath $localProperties -PathType Leaf)) {
        return $null
    }

    $sdkLine = Select-String -LiteralPath $localProperties -Pattern '^sdk\.dir=' | Select-Object -First 1
    if (-not $sdkLine) {
        return $null
    }

    return $sdkLine.Line.Substring(8).Replace('\:', ':').Replace('\\', '\')
}

function Get-HighestInstalledVersionCode {
    param(
        [string]$AdbPath,
        [string]$PackageName
    )

    if ([string]::IsNullOrWhiteSpace($AdbPath) -or -not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) {
        return 0
    }

    $devices = @(& $AdbPath devices 2>$null)
    $deviceCount = @($devices | Where-Object { $_ -match '\sdevice$' }).Count
    if ($deviceCount -eq 0) {
        return 0
    }

    $packageDump = @(& $AdbPath shell dumpsys package $PackageName 2>$null)
    $versionMatch = [regex]::Match(($packageDump -join "`n"), 'versionCode=(\d+)')
    if (-not $versionMatch.Success) {
        return 0
    }

    return [int]$versionMatch.Groups[1].Value
}

function Get-ArchivedBuildInfo {
    param([string[]]$Roots)

    $items = @()
    foreach ($root in $Roots) {
        if (-not (Test-Path -LiteralPath $root -PathType Container)) {
            continue
        }

        foreach ($file in Get-ChildItem -LiteralPath $root -Filter 'BUILD_INFO.json' -File -Recurse) {
            try {
                $items += Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
            }
            catch {
                throw "Invalid build metadata: $($file.FullName)"
            }
        }
    }
    return @($items)
}

$repositoryRoot = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repositoryRoot)) {
    throw 'Run this script inside the project Git repository.'
}
$repositoryRoot = (Resolve-Path -LiteralPath $repositoryRoot).Path

$branch = (& git branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branch)) {
    throw 'Cannot determine the current Git branch.'
}
if ($branch -eq 'main') {
    throw 'Versioned builds are forbidden on main. Switch to a feature/vX.Y branch first.'
}

$untrackedFiles = @(& git status --porcelain --untracked-files=all | Where-Object { $_ -match '^\?\?' })
if ($untrackedFiles.Count -gt 0) {
    throw "Untracked files cannot be reproduced in the source snapshot. Add or ignore them before building:`n$($untrackedFiles -join "`n")"
}

$buildFile = Join-Path $repositoryRoot 'app\build.gradle'
$buildText = Get-Content -LiteralPath $buildFile -Raw
$defaultCodeMatch = [regex]::Match($buildText, "orElse\('(\d+)'\)\.get\(\)\.toInteger\(\)")
if (-not $defaultCodeMatch.Success) {
    throw 'Cannot read the default versionCode from app/build.gradle.'
}
$defaultVersionCode = [int]$defaultCodeMatch.Groups[1].Value

$artifactBuildRoot = Join-Path $repositoryRoot 'artifacts\builds'
$backupBuildRoot = Join-Path $repositoryRoot 'backup\builds'
$archivedBuilds = @(Get-ArchivedBuildInfo @($artifactBuildRoot, $backupBuildRoot))

$highestArchivedCode = 0
if ($archivedBuilds.Count -gt 0) {
    $highestArchivedCode = [int](($archivedBuilds | Measure-Object -Property versionCode -Maximum).Maximum)
}

$escapedSeries = [regex]::Escape($Series)
$highestSequence = 0
foreach ($build in $archivedBuilds) {
    $match = [regex]::Match([string]$build.versionName, "^$escapedSeries\.(\d+)$")
    if ($match.Success) {
        $highestSequence = [Math]::Max($highestSequence, [int]$match.Groups[1].Value)
    }
}

$androidSdk = Get-AndroidSdkPath $repositoryRoot
$adb = if ($androidSdk) { Join-Path $androidSdk 'platform-tools\adb.exe' } else { $null }
$installedVersionCode = Get-HighestInstalledVersionCode $adb 'com.ml.tblandroidtxt'

$nextVersionCode = ([Math]::Max(
    [Math]::Max($defaultVersionCode, $highestArchivedCode),
    $installedVersionCode
)) + 1
$nextSequence = $highestSequence + 1
$versionName = "$Series.$nextSequence"
$eventId = 'build-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$timestamp = [DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz')
$commit = (& git rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) {
    throw 'Cannot resolve HEAD.'
}

$artifactDestination = Join-Path $artifactBuildRoot (Join-Path "v$versionName" $eventId)
$backupDestination = Join-Path $backupBuildRoot (Join-Path "v$versionName" $eventId)
if ((Test-Path -LiteralPath $artifactDestination) -or (Test-Path -LiteralPath $backupDestination)) {
    throw "Build event already exists; refusing to overwrite: $eventId"
}

$snapshotOutput = & git stash create "versioned-build-$eventId"
if ($LASTEXITCODE -ne 0) {
    throw 'Cannot capture the current tracked source state.'
}
$snapshotRef = if ($null -eq $snapshotOutput) { '' } else { ([string]$snapshotOutput).Trim() }
if ([string]::IsNullOrWhiteSpace($snapshotRef)) {
    $snapshotRef = $commit
}

$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $androidStudioJbr = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path -LiteralPath (Join-Path $androidStudioJbr 'bin\java.exe') -PathType Leaf) {
        $env:JAVA_HOME = $androidStudioJbr
    }
}

$gradleArguments = @(
    'clean',
    'testDebugUnitTest',
    'lintDebug',
    'assembleDebug',
    '-PversionedBuild=true',
    "-PbuildVersionName=$versionName",
    "-PbuildVersionCode=$nextVersionCode",
    "-PbuildEventId=$eventId",
    "-PbuildGitCommit=$commit",
    "-PbuildTimestamp=$timestamp"
)

Write-Output "Building $versionName (versionCode $nextVersionCode, event $eventId)"
& $gradleWrapper @gradleArguments
if ($LASTEXITCODE -ne 0) {
    throw 'Build or regression failed. No build archive was created.'
}

$apkPath = Join-Path $repositoryRoot "app\build\outputs\apk\debug\TranslateBooks-v$versionName-debug.apk"
if (-not (Test-Path -LiteralPath $apkPath -PathType Leaf)) {
    throw "Successful Gradle run did not produce the expected APK: $apkPath"
}

$stagingRoot = Join-Path $repositoryRoot ".archive-staging\$([guid]::NewGuid().ToString('N'))"
$payload = Join-Path $stagingRoot 'payload'
$backupStaging = Join-Path $stagingRoot 'backup'

try {
    New-Item -ItemType Directory -Path $payload | Out-Null
    $archivedApkName = "TranslateBooks-v$versionName-code$nextVersionCode.apk"
    $archivedApk = Join-Path $payload $archivedApkName
    Copy-Item -LiteralPath $apkPath -Destination $archivedApk

    $sourceZipName = "project_source_$eventId.zip"
    $sourceZip = Join-Path $payload $sourceZipName
    & git archive --format=zip "--output=$sourceZip" $snapshotRef
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $sourceZip -PathType Leaf)) {
        throw 'Failed to create the exact tracked-source snapshot.'
    }

    $apkHash = (Get-FileHash -LiteralPath $archivedApk -Algorithm SHA256).Hash
    $sourceHash = (Get-FileHash -LiteralPath $sourceZip -Algorithm SHA256).Hash
    $buildInfo = [ordered]@{
        schemaVersion = 1
        versionName = $versionName
        versionCode = $nextVersionCode
        eventId = $eventId
        created = $timestamp
        branch = $branch
        commit = $commit
        sourceSnapshotRef = $snapshotRef
        installedVersionCodeObserved = $installedVersionCode
        apk = $archivedApkName
        apkSha256 = $apkHash
        sourceArchive = $sourceZipName
        sourceArchiveSha256 = $sourceHash
        notes = $Notes
    }
    Write-Utf8File (Join-Path $payload 'BUILD_INFO.json') @(
        ($buildInfo | ConvertTo-Json -Depth 4)
    )

    Write-Utf8File (Join-Path $payload 'README.md') @(
        "# Translate Books $versionName",
        '',
        'This directory is an immutable local development-build record.',
        '',
        "- Version name: ``$versionName``",
        "- Version code: ``$nextVersionCode``",
        "- Build event: ``$eventId``",
        "- Built: ``$timestamp``",
        "- Branch: ``$branch``",
        "- Git commit: ``$commit``",
        "- APK: ``$archivedApkName``",
        "- APK SHA-256: ``$apkHash``",
        "- Source snapshot: ``$sourceZipName``",
        "- Source SHA-256: ``$sourceHash``",
        "- Notes: $Notes",
        '',
        'The APK was archived before any optional device installation. Do not overwrite or reuse this event directory.'
    )

    $checksumLines = Get-ChildItem -LiteralPath $payload -File |
        Where-Object { $_.Name -ne 'SHA256SUMS.txt' } |
        Sort-Object Name |
        ForEach-Object {
            $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
            "$hash  $($_.Name)"
        }
    Write-Utf8File (Join-Path $payload 'SHA256SUMS.txt') @($checksumLines)

    Copy-Item -LiteralPath $payload -Destination $backupStaging -Recurse
    New-Item -ItemType Directory -Path (Split-Path $artifactDestination -Parent) -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path $backupDestination -Parent) -Force | Out-Null
    Move-Item -LiteralPath $payload -Destination $artifactDestination
    Move-Item -LiteralPath $backupStaging -Destination $backupDestination
}
finally {
    if (Test-Path -LiteralPath $stagingRoot) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}

$finalApk = Join-Path $artifactDestination "TranslateBooks-v$versionName-code$nextVersionCode.apk"
if ($Install) {
    if ([string]::IsNullOrWhiteSpace($adb) -or -not (Test-Path -LiteralPath $adb -PathType Leaf)) {
        throw 'The build was archived, but ADB is unavailable, so installation was skipped.'
    }

    & $adb install -r $finalApk
    if ($LASTEXITCODE -ne 0) {
        throw "The build is safely archived, but device installation failed: $finalApk"
    }
}

Write-Output "Version:          $versionName (versionCode $nextVersionCode)"
Write-Output "Artifact archive: $artifactDestination"
Write-Output "Backup archive:   $backupDestination"
Write-Output "README:           $(Join-Path $artifactDestination 'README.md')"
Write-Output "SHA-256:          $((Get-FileHash -LiteralPath $finalApk -Algorithm SHA256).Hash)"
