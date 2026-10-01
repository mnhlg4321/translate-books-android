[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^v\d+\.\d+(?:\.\d+)?(?:-[A-Za-z0-9.-]+)?$')]
    [string]$TargetProductionVersion,

    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 2147483647)]
    [int]$TargetProductionVersionCode,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$TargetProductionApkSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$TargetProductionSourceZipSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$TargetProductionCertificateSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9-]{2,80}$')]
    [string]$EventId,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$ExpectedSourceCommit,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$JavaHome,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$AndroidSdkPath,

    [string]$TestPackage = 'com.ml.tblandroidtxt.test',

    [string]$TargetPackage = 'com.ml.tblandroidtxt',

    [string]$Runner = 'androidx.test.runner.AndroidJUnitRunner',

    [string]$Notes = 'Host-only AndroidTest artifact; not installed and not device-verified.'
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

function Invoke-GitText {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $safeArguments = @('-c', "safe.directory=$repositoryRoot") + $Arguments
    $result = @(& git @safeArguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($Arguments -join ' ')`n$($result -join "`n")"
    }
    return ($result -join "`n").Trim()
}

function Get-HighestBuildToolsDirectory {
    param([Parameter(Mandatory = $true)][string]$SdkPath)

    $root = Join-Path $SdkPath 'build-tools'
    $directories = @(Get-ChildItem -LiteralPath $root -Directory | Sort-Object Name -Descending)
    foreach ($directory in $directories) {
        if ((Test-Path -LiteralPath (Join-Path $directory.FullName 'apksigner.bat') -PathType Leaf) -and
                (Test-Path -LiteralPath (Join-Path $directory.FullName 'aapt2.exe') -PathType Leaf)) {
            return $directory.FullName
        }
    }
    throw "No Android build-tools directory with apksigner.bat and aapt2.exe exists under $root."
}

function Get-FileSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

function Assert-EqualIgnoreCase {
    param(
        [Parameter(Mandatory = $true)][string]$Actual,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if (-not $Actual.Equals($Expected, [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Label mismatch. Expected '$Expected', actual '$Actual'."
    }
}

$scriptRoot = (Resolve-Path -LiteralPath $PSScriptRoot).Path
$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $scriptRoot '..')).Path
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
$javaPreflight = Join-Path $scriptRoot 'verify-java-toolchain.ps1'
$buildToolsDirectory = Get-HighestBuildToolsDirectory -SdkPath $AndroidSdkPath
$aapt2 = Join-Path $buildToolsDirectory 'aapt2.exe'
$apksigner = Join-Path $buildToolsDirectory 'apksigner.bat'

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}
if (-not (Test-Path -LiteralPath $javaPreflight -PathType Leaf)) {
    throw "JDK preflight script is missing: $javaPreflight"
}
if (-not (Test-Path -LiteralPath (Join-Path $AndroidSdkPath 'platforms\android-35\android.jar') -PathType Leaf)) {
    throw "Pinned Android SDK platform android-35 is missing under $AndroidSdkPath."
}

$actualHead = Invoke-GitText @('rev-parse', 'HEAD')
Assert-EqualIgnoreCase -Actual $actualHead -Expected $ExpectedSourceCommit -Label 'source commit'
$branch = Invoke-GitText @('branch', '--show-current')
if ([string]::IsNullOrWhiteSpace($branch) -or $branch -eq 'main') {
    throw "Test-only archive requires a non-main branch; actual branch '$branch'."
}
$statusLines = @(Invoke-GitText @('status', '--porcelain=v1', '--untracked-files=all'))
if ($statusLines.Count -gt 0 -and -not [string]::IsNullOrWhiteSpace(($statusLines -join ''))) {
    throw "Test-only archive requires a clean worktree:`n$($statusLines -join "`n")"
}

$toolchainJson = @(& $javaPreflight -AsJson -JavaHome $JavaHome -RequiredMajor 21 2>&1)
if ($LASTEXITCODE -ne 0 -or $toolchainJson.Count -eq 0) {
    throw "JDK preflight failed before AndroidTest archive build:`n$($toolchainJson -join "`n")"
}
try {
    $toolchain = ($toolchainJson -join "`n") | ConvertFrom-Json
}
catch {
    throw 'JDK preflight returned invalid JSON metadata.'
}
if ([int]$toolchain.jdkMajor -lt 17) {
    throw "JDK 17 or newer is required; actual major is $($toolchain.jdkMajor)."
}

$gradlePropertiesPath = Join-Path $repositoryRoot 'gradle\wrapper\gradle-wrapper.properties'
$gradleProperties = Get-Content -LiteralPath $gradlePropertiesPath -Raw
$distributionShaMatch = [regex]::Match($gradleProperties, '(?m)^distributionSha256Sum=([0-9a-fA-F]{64})$')
$distributionUrlMatch = [regex]::Match($gradleProperties, '(?m)^distributionUrl=(.+)$')
if (-not $distributionShaMatch.Success -or -not $distributionUrlMatch.Success) {
    throw 'Gradle wrapper distribution URL/SHA-256 is not pinned.'
}
$gradleDistributionSha256 = $distributionShaMatch.Groups[1].Value.ToUpperInvariant()
$gradleDistributionUrl = $distributionUrlMatch.Groups[1].Value.Trim().Replace('\:', ':')

$appBuildText = Get-Content -LiteralPath (Join-Path $repositoryRoot 'app\build.gradle') -Raw
$compileSdkMatch = [regex]::Match($appBuildText, '(?m)^\s*compileSdk\s+(\d+)')
$targetSdkMatch = [regex]::Match($appBuildText, '(?m)^\s*targetSdk\s+(\d+)')
if (-not $compileSdkMatch.Success -or -not $targetSdkMatch.Success) {
    throw 'compileSdk/targetSdk cannot be read from app/build.gradle.'
}
$compileSdk = [int]$compileSdkMatch.Groups[1].Value
$targetSdk = [int]$targetSdkMatch.Groups[1].Value

$gradleVersionLines = @(& $gradleWrapper '--version' '--no-daemon' 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "Gradle wrapper version check failed:`n$($gradleVersionLines -join "`n")"
}
$gradleVersionMatch = [regex]::Match(($gradleVersionLines -join "`n"), '(?m)^Gradle\s+([0-9]+\.[0-9]+\.[0-9]+)')
if (-not $gradleVersionMatch.Success) {
    throw 'Gradle wrapper did not report a parseable version.'
}
$gradleVersion = $gradleVersionMatch.Groups[1].Value

$timestamp = [DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz')
$eventRoot = Join-Path $repositoryRoot ".archive-staging\android-test-$EventId-$([guid]::NewGuid().ToString('N'))"
$payload = Join-Path $eventRoot 'payload'
$artifactDestination = Join-Path $repositoryRoot "artifacts\test-builds\$TargetProductionVersion\$EventId"
$backupDestination = Join-Path $repositoryRoot "backup\test-builds\$TargetProductionVersion\$EventId"
if ((Test-Path -LiteralPath $artifactDestination) -or (Test-Path -LiteralPath $backupDestination)) {
    throw "Archive event already exists; refusing to overwrite: $EventId"
}

New-Item -ItemType Directory -Path $payload -Force | Out-Null
$previousJavaHome = $env:JAVA_HOME
$previousAndroidHome = $env:ANDROID_HOME
$previousAndroidSdkRoot = $env:ANDROID_SDK_ROOT
$env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path
$env:ANDROID_HOME = (Resolve-Path -LiteralPath $AndroidSdkPath).Path
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

try {
    $gradleTask = ':app:assembleDebugAndroidTest'
    if ($gradleTask -eq ':app:assembleDebug' -or $gradleTask -match 'connected') {
        throw "Refusing non-test or connected Gradle task: $gradleTask"
    }
    $gradleCommand = @($gradleTask, '--rerun-tasks', '--no-daemon', '--console=plain')
    # Windows PowerShell 5.1 turns native stderr (e.g. javac Notes) into terminating errors under Stop; capture it as output instead.
    $gradleErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $assembleOutput = @(& $gradleWrapper @gradleCommand 2>&1) } finally { $ErrorActionPreference = $gradleErrorActionPreference }
    $assembleExitCode = $LASTEXITCODE
    Write-Utf8File (Join-Path $payload 'assemble-output.txt') @($assembleOutput)
    if ($assembleExitCode -ne 0) {
        throw "AndroidTest-only Gradle task failed with exit code $assembleExitCode."
    }

    $builtApk = Join-Path $repositoryRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    if (-not (Test-Path -LiteralPath $builtApk -PathType Leaf)) {
        throw "AndroidTest-only Gradle task did not produce $builtApk"
    }
    $apkHash = Get-FileSha256 -Path $builtApk
    $apkBytes = (Get-Item -LiteralPath $builtApk).Length

    $aaptOutput = @(& $aapt2 'dump' 'badging' $builtApk 2>&1)
    $aaptExitCode = $LASTEXITCODE
    if ($aaptExitCode -ne 0) {
        throw "aapt2 could not inspect the AndroidTest APK:`n$($aaptOutput -join "`n")"
    }
    $packageMatch = [regex]::Match(($aaptOutput -join "`n"), "package: name='([^']+)'")
    if (-not $packageMatch.Success) {
        throw 'AndroidTest APK package name was not present in aapt2 badging output.'
    }
    Assert-EqualIgnoreCase -Actual $packageMatch.Groups[1].Value -Expected $TestPackage -Label 'test package'

    $manifestToolOutput = @(& $aapt2 'dump' 'xmltree' '--file' 'AndroidManifest.xml' $builtApk 2>&1)
    $manifestExitCode = $LASTEXITCODE
    if ($manifestExitCode -ne 0) {
        throw "aapt2 could not inspect AndroidManifest.xml:`n$($manifestToolOutput -join "`n")"
    }
    $manifestText = $manifestToolOutput -join "`n"
    if ($manifestText -notmatch [regex]::Escape($TargetPackage)) {
        throw "AndroidTest manifest does not contain target package '$TargetPackage'."
    }
    if ($manifestText -notmatch [regex]::Escape($Runner)) {
        throw "AndroidTest manifest does not contain runner '$Runner'."
    }
    if ($manifestText -notmatch 'android:debuggable|debuggable') {
        throw 'AndroidTest manifest did not expose a debuggable flag for inspection.'
    }

    $apksignerOutput = @(& $apksigner 'verify' '--print-certs' $builtApk 2>&1)
    $apksignerExitCode = $LASTEXITCODE
    if ($apksignerExitCode -ne 0) {
        throw "apksigner verification failed:`n$($apksignerOutput -join "`n")"
    }
    $certificateMatch = [regex]::Match(
        ($apksignerOutput -join "`n"),
        '(?im)certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
    if (-not $certificateMatch.Success) {
        throw 'apksigner did not report a certificate SHA-256 digest.'
    }
    $certificateHash = $certificateMatch.Groups[1].Value.ToUpperInvariant()
    Assert-EqualIgnoreCase -Actual $certificateHash -Expected $TargetProductionCertificateSha256 -Label 'test certificate'

    $sourceZipName = "project_source_$EventId.zip"
    $sourceZip = Join-Path $payload $sourceZipName
    $archiveResult = @(& git -c "safe.directory=$repositoryRoot" archive --format=zip "--output=$sourceZip" $actualHead 2>&1)
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $sourceZip -PathType Leaf)) {
        throw "Failed to create exact tracked-source ZIP:`n$($archiveResult -join "`n")"
    }
    $sourceZipHash = Get-FileSha256 -Path $sourceZip

    $apkName = 'app-debug-androidTest.apk'
    Copy-Item -LiteralPath $builtApk -Destination (Join-Path $payload $apkName)
    $buildInfo = [ordered]@{
        schemaVersion = 1
        artifactKind = 'androidTest-only'
        eventId = $EventId
        buildTimestamp = $timestamp
        branch = $branch
        sourceCommit = $actualHead
        parentCommit = Invoke-GitText @('rev-parse', 'HEAD^')
        javaHome = $env:JAVA_HOME
        javaVersion = $toolchain.javaVersion
        javaRuntimeVersion = $toolchain.javaRuntimeVersion
        jdkMajor = [int]$toolchain.jdkMajor
        jdkMajorPolicy = $toolchain.jdkMajorPolicy
        androidSdkPath = $env:ANDROID_SDK_ROOT
        androidSdkPlatform = "android-$compileSdk"
        compileSdk = $compileSdk
        targetSdk = $targetSdk
        gradleVersion = $gradleVersion
        gradleDistributionUrl = $gradleDistributionUrl
        gradleDistributionSha256 = $gradleDistributionSha256
        gradleTask = ($gradleCommand -join ' ')
        testApk = $apkName
        testApkBytes = [int64]$apkBytes
        testApkSha256 = $apkHash
        testPackage = $TestPackage
        targetPackage = $TargetPackage
        runner = $Runner
        certificateSha256 = $certificateHash
        targetProductionVersion = $TargetProductionVersion
        targetProductionVersionCode = $TargetProductionVersionCode
        targetProductionApkSha256 = $TargetProductionApkSha256.ToUpperInvariant()
        targetProductionSourceZipSha256 = $TargetProductionSourceZipSha256.ToUpperInvariant()
        targetProductionCertificateSha256 = $TargetProductionCertificateSha256.ToUpperInvariant()
        sourceArchive = $sourceZipName
        sourceArchiveSha256 = $sourceZipHash
        installed = $false
        deviceOperations = 0
        providerCalls = 0
        authorizationCreated = 0
        attemptCreated = 0
        reconciliationCreated = 0
        productionApkRebuilt = $false
        productionDiffCount = 0
        notes = $Notes
    }
    Write-Utf8File (Join-Path $payload 'BUILD_INFO.json') @(
        ($buildInfo | ConvertTo-Json -Depth 5)
    )

    Write-Utf8File (Join-Path $payload 'README.md') @(
        '# Translate Books AndroidTest-only archive',
        '',
        'This immutable payload was created by the test-only archive path. It is not installed and is not device-verified.',
        "- Event: ``$EventId``",
        "- Build timestamp: ``$timestamp``",
        "- Branch/source commit: ``$branch`` / ``$actualHead``",
        "- Test APK: ``$apkName`` ($apkBytes bytes)",
        "- Test APK SHA-256: ``$apkHash``",
        "- Test package/target/runner: ``$TestPackage`` / ``$TargetPackage`` / ``$Runner``",
        "- Certificate SHA-256: ``$certificateHash``",
        "- Source ZIP: ``$sourceZipName``",
        "- Source ZIP SHA-256: ``$sourceZipHash``",
        "- Target production version/code: ``$TargetProductionVersion`` / ``$TargetProductionVersionCode``",
        "- Target production APK SHA-256: ``$TargetProductionApkSha256``",
        "- Target production source ZIP SHA-256: ``$TargetProductionSourceZipSha256``",
        "- Production APK rebuilt: ``false``",
        "- Installed: ``false``",
        "- Device operations/provider calls: ``0`` / ``0``",
        "- Notes: $Notes",
        '',
        'The previous A4 status-mapping artifact DC0E6790C1D82F3C7D3102711C929F8DC0CA2D380314E4EB77F1EEA46C41AC2B is superseded and must not be installed.'
    )

    $inspectionLines = @(
        'artifactKind=androidTest-only',
        "package=$TestPackage",
        "targetPackage=$TargetPackage",
        "runner=$Runner",
        'debuggable=true',
        "certificateSha256=$certificateHash",
        "testApkBytes=$apkBytes",
        "testApkSha256=$apkHash",
        "sourceCommit=$actualHead",
        "productionApkRebuilt=false",
        'installed=false',
        'deviceOperations=0',
        'providerCalls=0',
        'authorizationCreated=0',
        'attemptCreated=0',
        'reconciliationCreated=0'
    )
    Write-Utf8File (Join-Path $payload 'artifact-inspection.txt') $inspectionLines

    $checksumLines = @(Get-ChildItem -LiteralPath $payload -File |
        Where-Object { $_.Name -notin @('SHA256SUMS.txt', 'SHA256SUMS.sha256') } |
        Sort-Object Name |
        ForEach-Object {
            "$(Get-FileSha256 -Path $_.FullName)  $($_.Name)"
        })
    Write-Utf8File (Join-Path $payload 'SHA256SUMS.txt') $checksumLines
    Write-Utf8File (Join-Path $payload 'SHA256SUMS.sha256') @(
        "$(Get-FileSha256 -Path (Join-Path $payload 'SHA256SUMS.txt'))  SHA256SUMS.txt"
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $artifactDestination) -Force | Out-Null
    New-Item -ItemType Directory -Path (Split-Path -Parent $backupDestination) -Force | Out-Null
    Copy-Item -LiteralPath $payload -Destination $artifactDestination -Recurse
    Copy-Item -LiteralPath $payload -Destination $backupDestination -Recurse

    $artifactFiles = @(Get-ChildItem -LiteralPath $artifactDestination -File | Sort-Object Name)
    $backupFiles = @(Get-ChildItem -LiteralPath $backupDestination -File | Sort-Object Name)
    if (($artifactFiles.Name -join '|') -ne ($backupFiles.Name -join '|')) {
        throw 'Artifact and backup payload file lists differ.'
    }
    foreach ($artifactFile in $artifactFiles) {
        $backupFile = Join-Path $backupDestination $artifactFile.Name
        Assert-EqualIgnoreCase -Actual (Get-FileSha256 -Path $artifactFile.FullName) `
            -Expected (Get-FileSha256 -Path $backupFile) -Label "artifact/backup $($artifactFile.Name)"
    }
    Write-Output "artifactDestination=$artifactDestination"
    Write-Output "backupDestination=$backupDestination"
    Write-Output "testApkSha256=$apkHash"
    Write-Output "testApkBytes=$apkBytes"
    Write-Output "certificateSha256=$certificateHash"
    Write-Output "sourceArchiveSha256=$sourceZipHash"
    Write-Output 'productionApkRebuilt=false'
    Write-Output 'installed=false'
    Write-Output 'deviceOperations=0'
    Write-Output 'providerCalls=0'
}
finally {
    if ($null -eq $previousJavaHome) { Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue } else { $env:JAVA_HOME = $previousJavaHome }
    if ($null -eq $previousAndroidHome) { Remove-Item Env:ANDROID_HOME -ErrorAction SilentlyContinue } else { $env:ANDROID_HOME = $previousAndroidHome }
    if ($null -eq $previousAndroidSdkRoot) { Remove-Item Env:ANDROID_SDK_ROOT -ErrorAction SilentlyContinue } else { $env:ANDROID_SDK_ROOT = $previousAndroidSdkRoot }
    if (Test-Path -LiteralPath $eventRoot) {
        Remove-Item -LiteralPath $eventRoot -Recurse -Force
    }
}
