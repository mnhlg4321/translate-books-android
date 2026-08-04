[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+(?:\.\d+)?(?:-[A-Za-z0-9-]+)?$')]
    [string]$Series = '4.14-dev',

    [ValidatePattern('^\d+\.\d+(?:\.\d+)?$')]
    [string]$ExactReleaseVersion,

    [ValidateNotNullOrEmpty()]
    [string]$Notes = 'Versioned development build.',

    [string]$JavaHome,

    [switch]$Install
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$preflightScript = Join-Path $PSScriptRoot 'verify-java-toolchain.ps1'
if (-not (Test-Path -LiteralPath $preflightScript -PathType Leaf)) {
    throw 'JDK preflight script is missing: scripts/verify-java-toolchain.ps1'
}
$preflightOutput = @(
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
        & $preflightScript -AsJson -JavaHome $JavaHome
    }
    else {
        & $preflightScript -AsJson
    }
)
if ($LASTEXITCODE -ne 0 -or $preflightOutput.Count -eq 0) {
    throw 'JDK preflight failed before archive build.'
}
try {
    $toolchain = ($preflightOutput -join "`n") | ConvertFrom-Json
}
catch {
    throw 'JDK preflight returned invalid metadata.'
}

$effectiveJavaHome = if (-not [string]::IsNullOrWhiteSpace($JavaHome)) { $JavaHome } else { $env:JAVA_HOME }
$effectiveJavaHome = (Resolve-Path -LiteralPath $effectiveJavaHome).Path
$env:JAVA_HOME = $effectiveJavaHome
$env:Path = "$(Join-Path $effectiveJavaHome 'bin');$env:Path"

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

    try { $devices = @(& $AdbPath devices 2>$null) } catch { return 0 }
    $deviceCount = @($devices | Where-Object { $_ -match '\sdevice$' }).Count
    if ($deviceCount -eq 0) {
        return 0
    }

    try { $packageDump = @(& $AdbPath shell dumpsys package $PackageName 2>$null) } catch { return 0 }
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

$repositoryRoot = (& git -c "safe.directory=$((Resolve-Path (Join-Path $PSScriptRoot '..')).Path)" rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repositoryRoot)) {
    throw 'Run this script inside the project Git repository.'
}
$repositoryRoot = (Resolve-Path -LiteralPath $repositoryRoot).Path

$branch = (& git -c "safe.directory=$repositoryRoot" branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branch)) {
    throw 'Cannot determine the current Git branch.'
}
if ($branch -eq 'main') {
    throw 'Versioned builds are forbidden on main. Switch to a feature/vX.Y branch first.'
}
if (-not [string]::IsNullOrWhiteSpace($ExactReleaseVersion)) {
    if ($PSBoundParameters.ContainsKey('Series')) {
        throw 'Use either -Series for numbered development builds or -ExactReleaseVersion for a release build, not both.'
    }

    $expectedReleaseBranch = "feature/v$ExactReleaseVersion"
    if ($branch -ne $expectedReleaseBranch) {
        throw "Exact release v$ExactReleaseVersion must be built from $expectedReleaseBranch; current branch is $branch."
    }

    $releaseChanges = @(& git -c "safe.directory=$repositoryRoot" status --porcelain)
    if ($LASTEXITCODE -ne 0) {
        throw 'Cannot verify the release working tree.'
    }
    if ($releaseChanges.Count -gt 0) {
        throw 'Exact release builds require a clean working tree with all source and release metadata committed.'
    }

    & git -c "safe.directory=$repositoryRoot" rev-parse --verify --quiet "refs/tags/v$ExactReleaseVersion" | Out-Null
    if ($LASTEXITCODE -eq 0) {
        throw "Release tag v$ExactReleaseVersion already exists; refusing to create another build for an immutable release."
    }
}

$untrackedFiles = @(& git -c "safe.directory=$repositoryRoot" status --porcelain --untracked-files=all | Where-Object { $_ -match '^\?\?' })
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

$versionPolicy = 'numbered-series'
if (-not [string]::IsNullOrWhiteSpace($ExactReleaseVersion)) {
    $versionName = $ExactReleaseVersion
    $versionPolicy = 'exact-release'
}
else {
    $escapedSeries = [regex]::Escape($Series)
    $highestSequence = 0
    foreach ($build in $archivedBuilds) {
        $match = [regex]::Match([string]$build.versionName, "^$escapedSeries\.(\d+)$")
        if ($match.Success) {
            $highestSequence = [Math]::Max($highestSequence, [int]$match.Groups[1].Value)
        }
    }
    $nextSequence = $highestSequence + 1
    $versionName = "$Series.$nextSequence"
}

$androidSdk = Get-AndroidSdkPath $repositoryRoot
$adb = if ($androidSdk) { Join-Path $androidSdk 'platform-tools\adb.exe' } else { $null }
$installedVersionCode = Get-HighestInstalledVersionCode $adb 'com.ml.tblandroidtxt'

$nextVersionCode = ([Math]::Max(
    [Math]::Max($defaultVersionCode, $highestArchivedCode),
    $installedVersionCode
)) + 1
$eventId = 'build-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$timestamp = [DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz')
$commit = (& git -c "safe.directory=$repositoryRoot" rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) {
    throw 'Cannot resolve HEAD.'
}

$artifactDestination = Join-Path $artifactBuildRoot (Join-Path "v$versionName" $eventId)
$backupDestination = Join-Path $backupBuildRoot (Join-Path "v$versionName" $eventId)
if ((Test-Path -LiteralPath $artifactDestination) -or (Test-Path -LiteralPath $backupDestination)) {
    throw "Build event already exists; refusing to overwrite: $eventId"
}

$snapshotOutput = & git -c "safe.directory=$repositoryRoot" stash create "versioned-build-$eventId"
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

$wrapperPropertiesPath = Join-Path $repositoryRoot 'gradle\wrapper\gradle-wrapper.properties'
$wrapperProperties = Get-Content -LiteralPath $wrapperPropertiesPath -Raw
$distributionUrlMatch = [regex]::Match($wrapperProperties, '(?m)^distributionUrl=(.+)$')
$distributionShaMatch = [regex]::Match($wrapperProperties, '(?m)^distributionSha256Sum=([0-9a-fA-F]{64})$')
if (-not $distributionUrlMatch.Success -or -not $distributionShaMatch.Success) {
    throw 'Gradle wrapper provenance is incomplete: distribution URL and SHA-256 are required.'
}
$gradleDistributionUrl = $distributionUrlMatch.Groups[1].Value.Trim().Replace('\:', ':')
$gradleDistributionSha256 = $distributionShaMatch.Groups[1].Value.Trim().ToLowerInvariant()
$gradleUrlVersionMatch = [regex]::Match($gradleDistributionUrl, '/gradle-([0-9]+\.[0-9]+\.[0-9]+)-bin\.zip$')
if (-not $gradleUrlVersionMatch.Success) {
    throw 'Gradle wrapper distribution URL is not a pinned binary distribution URL.'
}
$gradleVersionFromUrl = $gradleUrlVersionMatch.Groups[1].Value

$rootBuildText = Get-Content -LiteralPath (Join-Path $repositoryRoot 'build.gradle') -Raw
$agpMatches = [regex]::Matches($rootBuildText, "com\.android\.(?:application|test)' version '([^']+)'")
if ($agpMatches.Count -lt 2) {
    throw 'Cannot read both Android Gradle Plugin versions from build.gradle.'
}
$agpVersions = @($agpMatches | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique)
if ($agpVersions.Count -ne 1) {
    throw 'Android Gradle Plugin versions are not consistent across the root plugins block.'
}
$agpVersion = $agpVersions[0]

$javaCompatibilityFiles = @('app\build.gradle', 'editorial-engine\build.gradle', 'macrobenchmark\build.gradle')
$sourceCompatibilities = @()
$targetCompatibilities = @()
foreach ($relativePath in $javaCompatibilityFiles) {
    $moduleText = Get-Content -LiteralPath (Join-Path $repositoryRoot $relativePath) -Raw
    $sourceMatch = [regex]::Match($moduleText, 'sourceCompatibility\s*=?\s*JavaVersion\.VERSION_(\d+)')
    $targetMatch = [regex]::Match($moduleText, 'targetCompatibility\s*=?\s*JavaVersion\.VERSION_(\d+)')
    if (-not $sourceMatch.Success -or -not $targetMatch.Success) {
        throw "Java source/target compatibility is missing from $relativePath."
    }
    $sourceCompatibilities += $sourceMatch.Groups[1].Value
    $targetCompatibilities += $targetMatch.Groups[1].Value
}
$javaSourceCompatibility = @($sourceCompatibilities | Select-Object -Unique)
$javaTargetCompatibility = @($targetCompatibilities | Select-Object -Unique)
if ($javaSourceCompatibility.Count -ne 1 -or $javaTargetCompatibility.Count -ne 1 -or $javaSourceCompatibility[0] -ne '17' -or $javaTargetCompatibility[0] -ne '17') {
    throw 'Java source/target compatibility must remain consistently pinned to 17.'
}

$appBuildText = Get-Content -LiteralPath (Join-Path $repositoryRoot 'app\build.gradle') -Raw
$compileSdkMatch = [regex]::Match($appBuildText, '(?m)^\s*compileSdk\s+(\d+)')
$targetSdkMatch = [regex]::Match($appBuildText, '(?m)^\s*targetSdk\s+(\d+)')
if (-not $compileSdkMatch.Success -or -not $targetSdkMatch.Success) {
    throw 'compileSdk or targetSdk is missing from app/build.gradle.'
}
$compileSdk = [int]$compileSdkMatch.Groups[1].Value
$targetSdk = [int]$targetSdkMatch.Groups[1].Value

$gradleVersionOutput = @(& $gradleWrapper '--version' '--no-daemon')
if ($LASTEXITCODE -ne 0) {
    throw 'Gradle Wrapper version verification failed before the archive build.'
}
$gradleVersionOutput | ForEach-Object { Write-Output $_ }
$gradleVersionMatch = [regex]::Match(($gradleVersionOutput -join "`n"), '(?m)^Gradle\s+([0-9]+\.[0-9]+\.[0-9]+)')
if (-not $gradleVersionMatch.Success) {
    throw 'Gradle Wrapper did not report a parseable version.'
}
$gradleVersion = $gradleVersionMatch.Groups[1].Value
if ($gradleVersion -ne $gradleVersionFromUrl) {
    throw "Gradle Wrapper version $gradleVersion does not match distribution URL version $gradleVersionFromUrl."
}

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
        versionPolicy = $versionPolicy
        eventId = $eventId
        created = $timestamp
        branch = $branch
        commit = $commit
        gitCommit = $commit
        sourceSnapshotRef = $snapshotRef
        installedVersionCodeObserved = $installedVersionCode
        buildEvent = $eventId
        gradleVersion = $gradleVersion
        gradleDistributionUrl = $gradleDistributionUrl
        gradleDistributionSha256 = $gradleDistributionSha256
        gradleDistributionChecksumSource = 'https://services.gradle.org/distributions/gradle-9.3.0-bin.zip.sha256'
        agpVersion = $agpVersion
        javaVersion = $toolchain.javaVersion
        javaRuntimeVersion = $toolchain.javaRuntimeVersion
        javaRuntimeName = $toolchain.javaRuntimeName
        javaVendor = $toolchain.javaVendor
        jdkMajor = [int]$toolchain.jdkMajor
        jdkMajorPolicy = $toolchain.jdkMajorPolicy
        javaSourceCompatibility = $javaSourceCompatibility[0]
        javaTargetCompatibility = $javaTargetCompatibility[0]
        compileSdk = $compileSdk
        targetSdk = $targetSdk
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
        "- Version policy: ``$versionPolicy``",
        "- Build event: ``$eventId``",
        "- Built: ``$timestamp``",
        "- Branch: ``$branch``",
        "- Git commit: ``$commit``",
        "- Gradle: ``$gradleVersion``",
        "- Gradle distribution: ``$gradleDistributionUrl``",
        "- Gradle distribution SHA-256: ``$gradleDistributionSha256``",
        "- Android Gradle Plugin: ``$agpVersion``",
        "- Java/JDK version: ``$($toolchain.javaVersion)``",
        "- Java runtime: ``$($toolchain.javaRuntimeName)``",
        "- Java vendor: ``$($toolchain.javaVendor)``",
        "- JDK major policy: ``$($toolchain.jdkMajorPolicy)``",
        "- Java source/target compatibility: ``$($javaSourceCompatibility[0])``/``$($javaTargetCompatibility[0])``",
        "- compileSdk/targetSdk: ``$compileSdk``/``$targetSdk``",
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
