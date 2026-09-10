[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$Serial,

    [ValidateNotNullOrEmpty()]
    [string]$PackageName = 'com.ml.tblandroidtxt',

    [Parameter(Mandatory = $true)]
    [ValidateRange(1, [int]::MaxValue)]
    [int]$ExpectedVersionCode,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedApkSha256,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedApkCertificateSha256,

    [string]$ExpectedDeviceSignatureToken,

    [string]$AndroidSdkPath,

    [switch]$AllowMissingPackage,

    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Resolve-AndroidSdkPath {
    param([string]$RequestedPath)

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }

    $environmentPath = if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $env:ANDROID_HOME
    } else {
        $env:ANDROID_SDK_ROOT
    }
    if (-not [string]::IsNullOrWhiteSpace($environmentPath)) {
        return (Resolve-Path -LiteralPath $environmentPath).Path
    }

    $repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
    $localProperties = Join-Path $repositoryRoot 'local.properties'
    if (Test-Path -LiteralPath $localProperties -PathType Leaf) {
        $sdkLine = Select-String -LiteralPath $localProperties -Pattern '^sdk\.dir=' |
            Select-Object -First 1
        if ($sdkLine) {
            $localPath = $sdkLine.Line.Substring(8).Replace('\:', ':').Replace('\\', '\')
            return (Resolve-Path -LiteralPath $localPath).Path
        }
    }
    throw 'Android SDK path is unavailable. Set ANDROID_HOME/ANDROID_SDK_ROOT or pass -AndroidSdkPath.'
}

function Resolve-BuildTool {
    param(
        [string]$SdkPath,
        [string]$ToolName
    )

    $buildToolsRoot = Join-Path $SdkPath 'build-tools'
    if (-not (Test-Path -LiteralPath $buildToolsRoot -PathType Container)) {
        throw "Android build-tools directory is missing: $buildToolsRoot"
    }
    $tool = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
        Sort-Object Name -Descending |
        ForEach-Object {
            $candidate = Join-Path $_.FullName $ToolName
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { Get-Item -LiteralPath $candidate }
        } |
        Select-Object -First 1
    if (-not $tool) {
        throw "Android build tool is missing: $ToolName"
    }
    return $tool.FullName
}

function Invoke-CheckedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$FailureMessage
    )

    $output = @(& $FilePath @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "$FailureMessage`n$($output -join "`n")"
    }
    return $output
}

function Read-PackageState {
    param(
        [string]$AdbPath,
        [string]$DeviceSerial,
        [string]$TargetPackage
    )

    $output = Invoke-CheckedCommand $AdbPath @('-s', $DeviceSerial, 'shell', 'dumpsys', 'package', $TargetPackage) `
        "Unable to read package state for $TargetPackage on $DeviceSerial."
    $text = $output -join "`n"
    if ($text -match 'Unable to find package') {
        return [pscustomobject]@{
            Present = $false
            VersionCode = 0
            SignatureToken = ''
            Raw = $text
        }
    }

    $versionMatch = [regex]::Match($text, 'versionCode=(\d+)')
    if (-not $versionMatch.Success) {
        throw "Package state did not contain a versionCode for $TargetPackage on $DeviceSerial."
    }
    $signatureMatch = [regex]::Match($text, 'signatures:\[([^\]]+)\]')
    if (-not $signatureMatch.Success -or [string]::IsNullOrWhiteSpace($signatureMatch.Groups[1].Value)) {
        throw "Package state did not contain a signing signature token for $TargetPackage on $DeviceSerial."
    }
    $signatureToken = ($signatureMatch.Groups[1].Value -split ',')[0].Trim().ToLowerInvariant()
    return [pscustomobject]@{
        Present = $true
        VersionCode = [int]$versionMatch.Groups[1].Value
        SignatureToken = $signatureToken
        Raw = $text
    }
}

function Assert-PackageState {
    param(
        [pscustomobject]$State,
        [string]$Stage,
        [int]$MinimumVersionCode,
        [string]$ExpectedToken,
        [bool]$WasPreviouslyMissing
    )

    if (-not $State.Present) {
        throw "INSTALL_GUARD_PACKAGE_MISSING_AFTER_INSTALL: package state unavailable after $Stage."
    }
    if ($State.VersionCode -ne $MinimumVersionCode) {
        throw "INSTALL_GUARD_VERSION_MISMATCH: expected versionCode $MinimumVersionCode after $Stage, found $($State.VersionCode)."
    }
    if (-not $WasPreviouslyMissing) {
        if ([string]::IsNullOrWhiteSpace($ExpectedToken)) {
            throw 'INSTALL_GUARD_DEVICE_SIGNATURE_TOKEN_REQUIRED: provide the exact preflight token from dumpsys package.'
        }
        if ($State.SignatureToken -ne $ExpectedToken.ToLowerInvariant()) {
            throw "INSTALL_GUARD_SIGNATURE_MISMATCH: expected device token $ExpectedToken after $Stage, found $($State.SignatureToken)."
        }
    }
}

$resolvedApk = (Resolve-Path -LiteralPath $ApkPath).Path
$actualApkSha256 = (Get-FileHash -LiteralPath $resolvedApk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualApkSha256 -ne $ExpectedApkSha256.ToLowerInvariant()) {
    throw "INSTALL_GUARD_APK_HASH_MISMATCH: expected $($ExpectedApkSha256.ToLowerInvariant()), found $actualApkSha256."
}
$sdkPath = Resolve-AndroidSdkPath $AndroidSdkPath
$adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
    throw "ADB is missing: $adbPath"
}
$aaptPath = Resolve-BuildTool $sdkPath 'aapt.exe'
$apksignerPath = Resolve-BuildTool $sdkPath 'apksigner.bat'

$badging = Invoke-CheckedCommand $aaptPath @('dump', 'badging', $resolvedApk) `
    "Unable to inspect APK metadata: $resolvedApk"
$badgingText = $badging -join "`n"
$packageMatch = [regex]::Match($badgingText,
    "package:\s+name='([^']+)'\s+versionCode='(\d+)'\s+versionName='([^']*)'")
if (-not $packageMatch.Success) {
    throw "APK metadata did not contain package, versionCode and versionName: $resolvedApk"
}
$apkPackage = $packageMatch.Groups[1].Value
$apkVersionCode = [int]$packageMatch.Groups[2].Value
$apkVersionName = $packageMatch.Groups[3].Value
if ($apkPackage -ne $PackageName) {
    throw "INSTALL_GUARD_PACKAGE_MISMATCH: expected $PackageName, APK contains $apkPackage."
}
if ($apkVersionCode -ne $ExpectedVersionCode) {
    throw "INSTALL_GUARD_APK_VERSION_MISMATCH: expected versionCode $ExpectedVersionCode, APK contains $apkVersionCode."
}

$signing = Invoke-CheckedCommand $apksignerPath @('verify', '--print-certs', $resolvedApk) `
    "Unable to verify APK signature: $resolvedApk"
$signingText = $signing -join "`n"
$certMatch = [regex]::Match($signingText, 'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
if (-not $certMatch.Success) {
    throw "APK signature output did not contain a SHA-256 certificate digest: $resolvedApk"
}
$apkCertificateSha256 = $certMatch.Groups[1].Value.ToLowerInvariant()
if ($apkCertificateSha256 -ne $ExpectedApkCertificateSha256.ToLowerInvariant()) {
    throw "INSTALL_GUARD_APK_CERTIFICATE_MISMATCH: expected $($ExpectedApkCertificateSha256.ToLowerInvariant()), found $apkCertificateSha256."
}

$stateOutput = Invoke-CheckedCommand $adbPath @('-s', $Serial, 'get-state') `
    "ADB device preflight failed for $Serial."
if (-not (($stateOutput -join "`n").Trim() -match '(?m)^device$')) {
    throw "INSTALL_GUARD_DEVICE_NOT_READY: $Serial is not in the device state."
}
$before = Read-PackageState $adbPath $Serial $PackageName
if ($before.Present) {
    if ($before.VersionCode -gt $ExpectedVersionCode) {
        throw "INSTALL_GUARD_VERSION_DOWNGRADE: device versionCode $($before.VersionCode) is newer than APK versionCode $ExpectedVersionCode."
    }
    if ([string]::IsNullOrWhiteSpace($ExpectedDeviceSignatureToken)) {
        throw 'INSTALL_GUARD_DEVICE_SIGNATURE_TOKEN_REQUIRED: existing package requires an exact preflight signature token.'
    }
    if ($before.SignatureToken -ne $ExpectedDeviceSignatureToken.ToLowerInvariant()) {
        throw "INSTALL_GUARD_SIGNATURE_MISMATCH: expected device token $ExpectedDeviceSignatureToken, found $($before.SignatureToken)."
    }
} elseif (-not $AllowMissingPackage) {
    throw 'INSTALL_GUARD_PACKAGE_MISSING: refusing an implicit fresh install; pass -AllowMissingPackage only for an explicitly empty target.'
}

Write-Output "Preflight PASS: package=$apkPackage versionCode=$apkVersionCode versionName=$apkVersionName"
Write-Output "APK SHA-256: $actualApkSha256"
Write-Output "APK certificate SHA-256: $apkCertificateSha256"
if ($before.Present) {
    Write-Output "Device package: present versionCode=$($before.VersionCode) signatureToken=$($before.SignatureToken)"
} else {
    Write-Output 'Device package: missing (explicit fresh-target allowance)'
}

if ($CheckOnly) {
    Write-Output 'Check-only mode: no install attempted.'
    exit 0
}

$installOutput = @(& $adbPath '-s' $Serial 'install' '-r' $resolvedApk 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "INSTALL_GUARD_INSTALL_FAILED: APK archived/preflighted but install failed.`n$($installOutput -join "`n")"
}
$installOutput | ForEach-Object { Write-Output $_ }

$after = Read-PackageState $adbPath $Serial $PackageName
Assert-PackageState $after 'post-install verification' $ExpectedVersionCode `
    $ExpectedDeviceSignatureToken (-not $before.Present)
Write-Output "Install verification PASS: package=$PackageName versionCode=$($after.VersionCode)"
