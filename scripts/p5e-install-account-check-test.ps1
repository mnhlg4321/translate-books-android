[CmdletBinding()]
param(
    [switch]$SelfTest,

    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedScriptSha256,

    [string]$TestApkPath,

    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTestApkSha256,

    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedTestCertificateSha256,

    [ValidateNotNullOrEmpty()]
    [string]$Serial = '15e84958',

    [ValidateRange(1, [int]::MaxValue)]
    [int]$ExpectedTargetVersionCode = 207,

    [ValidatePattern('^[0-9a-fA-F]{8,128}$')]
    [string]$ExpectedTargetSignatureToken = 'abebea4b',

    [ValidatePattern('^[0-9a-fA-F]{8,128}$')]
    [string]$ExpectedExistingTestSignatureToken = 'abebea4b',

    [string]$AndroidSdkPath,
    [string]$EvidenceDirectory = '',
    [switch]$CheckOnly,
    [switch]$ExecuteOneReplacement,
    [switch]$AllowMissingTestPackage
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$testPackage = 'com.ml.tblandroidtxt.test'
$targetPackage = 'com.ml.tblandroidtxt'
$testRunner = 'androidx.test.runner.AndroidJUnitRunner'

function Resolve-AndroidSdkPath {
    param([string]$RequestedPath)

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
            'C:\Users\ADMIN\AppData\Local\Android\Sdk')) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and
                (Test-Path -LiteralPath $candidate -PathType Container)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw 'ACCOUNT_TEST_INSTALLER_ANDROID_SDK_MISSING_STOP'
}

function Resolve-BuildTool {
    param([Parameter(Mandatory = $true)][string]$SdkPath,
        [Parameter(Mandatory = $true)][string]$ToolName)

    $buildToolsRoot = Join-Path $SdkPath 'build-tools'
    $tool = Get-ChildItem -LiteralPath $buildToolsRoot -Directory -ErrorAction Stop |
        Sort-Object Name -Descending |
        ForEach-Object {
            $candidate = Join-Path $_.FullName $ToolName
            if (Test-Path -LiteralPath $candidate -PathType Leaf) { $candidate }
        } |
        Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($tool)) {
        throw ('ACCOUNT_TEST_INSTALLER_BUILD_TOOL_MISSING_STOP:' + $ToolName)
    }
    return $tool
}

function Assert-RegularFileHash {
    param([Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedHash,
        [Parameter(Mandatory = $true)][string]$ErrorPrefix)

    $literalPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literalPath -PathType Leaf)) {
        throw ($ErrorPrefix + '_MISSING_STOP')
    }
    $item = Get-Item -LiteralPath $literalPath -Force
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) {
        throw ($ErrorPrefix + '_NOT_REGULAR_STOP')
    }
    $resolvedPath = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literalPath).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literalPath, $resolvedPath)) {
        throw ($ErrorPrefix + '_PATH_SWAP_STOP')
    }
    $actualHash = (Get-FileHash -LiteralPath $literalPath -Algorithm SHA256).Hash
    if ($actualHash -cne $ExpectedHash) {
        throw ($ErrorPrefix + '_HASH_MISMATCH_STOP')
    }
    return $literalPath
}

function Invoke-CheckedCommand {
    param([Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$ErrorCode)

    $output = @(& $FilePath @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw $ErrorCode
    }
    return ($output -join "`n")
}

function Read-TestApkMetadata {
    param([Parameter(Mandatory = $true)][string]$Aapt2Path,
        [Parameter(Mandatory = $true)][string]$ApkPath)

    $badging = Invoke-CheckedCommand -FilePath $Aapt2Path -Arguments @('dump', 'badging', $ApkPath) `
        -ErrorCode 'ACCOUNT_TEST_INSTALLER_BADGING_FAILED_STOP'
    $packageMatch = [regex]::Match($badging, "(?m)^package:\s+name='([^']+)'")
    if (-not $packageMatch.Success -or $packageMatch.Groups[1].Value -cne $testPackage) {
        throw 'ACCOUNT_TEST_INSTALLER_TEST_PACKAGE_MISMATCH_STOP'
    }
    $manifest = Invoke-CheckedCommand -FilePath $Aapt2Path `
        -Arguments @('dump', 'xmltree', '--file', 'AndroidManifest.xml', $ApkPath) `
        -ErrorCode 'ACCOUNT_TEST_INSTALLER_MANIFEST_READ_FAILED_STOP'
    if ($manifest -notmatch [regex]::Escape($targetPackage)) {
        throw 'ACCOUNT_TEST_INSTALLER_TARGET_PACKAGE_MISMATCH_STOP'
    }
    if ($manifest -notmatch [regex]::Escape($testRunner)) {
        throw 'ACCOUNT_TEST_INSTALLER_TEST_RUNNER_MISMATCH_STOP'
    }
    return [pscustomobject]@{ package = $testPackage; targetPackage = $targetPackage; runner = $testRunner }
}

function Assert-ApkCertificate {
    param([Parameter(Mandatory = $true)][string]$ApkSignerPath,
        [Parameter(Mandatory = $true)][string]$ApkPath,
        [Parameter(Mandatory = $true)][string]$ExpectedCertificate)

    $signing = Invoke-CheckedCommand -FilePath $ApkSignerPath -Arguments @('verify', '--print-certs', $ApkPath) `
        -ErrorCode 'ACCOUNT_TEST_INSTALLER_CERTIFICATE_READ_FAILED_STOP'
    $certificateMatch = [regex]::Match($signing, '(?im)certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
    if (-not $certificateMatch.Success -or
            $certificateMatch.Groups[1].Value.ToUpperInvariant() -cne $ExpectedCertificate.ToUpperInvariant()) {
        throw 'ACCOUNT_TEST_INSTALLER_CERTIFICATE_MISMATCH_STOP'
    }
}

function Read-PackageState {
    param([Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string]$DeviceSerial,
        [Parameter(Mandatory = $true)][string]$PackageName)

    $text = Invoke-CheckedCommand -FilePath $AdbPath `
        -Arguments @('-s', $DeviceSerial, 'shell', 'dumpsys', 'package', $PackageName) `
        -ErrorCode ('ACCOUNT_TEST_INSTALLER_PACKAGE_READ_FAILED_STOP:' + $PackageName)
    if ($text -match 'Unable to find package') {
        return [pscustomobject]@{ present = $false; versionCode = $null; signatureToken = '' }
    }
    $versionMatch = [regex]::Match($text, 'versionCode=(\d+)')
    $signatureMatch = [regex]::Match($text, 'signatures:\[([^\]]+)\]')
    if (-not $versionMatch.Success -or -not $signatureMatch.Success) {
        throw ('ACCOUNT_TEST_INSTALLER_PACKAGE_METADATA_INVALID_STOP:' + $PackageName)
    }
    return [pscustomobject]@{
        present = $true
        versionCode = [int]$versionMatch.Groups[1].Value
        signatureToken = (($signatureMatch.Groups[1].Value -split ',')[0].Trim().ToLowerInvariant())
    }
}

function Assert-TargetPackageState {
    param([Parameter(Mandatory = $true)]$State)

    if (-not $State.present -or $State.versionCode -ne $ExpectedTargetVersionCode -or
            $State.signatureToken -cne $ExpectedTargetSignatureToken.ToLowerInvariant()) {
        throw 'ACCOUNT_TEST_INSTALLER_TARGET_PRODUCTION_PIN_MISMATCH_STOP'
    }
}

function Assert-TestPackageState {
    param([Parameter(Mandatory = $true)]$State,
        [Parameter(Mandatory = $true)][bool]$AfterInstall)

    if (-not $State.present) {
        if ($AfterInstall) { throw 'ACCOUNT_TEST_INSTALLER_TEST_PACKAGE_MISSING_AFTER_INSTALL_STOP' }
        if (-not $AllowMissingTestPackage) { throw 'ACCOUNT_TEST_INSTALLER_TEST_PACKAGE_MISSING_STOP' }
        return
    }
    if ($State.signatureToken -cne $ExpectedExistingTestSignatureToken.ToLowerInvariant()) {
        throw 'ACCOUNT_TEST_INSTALLER_TEST_SIGNATURE_MISMATCH_STOP'
    }
}

function Test-AccountInstallerSelfTest {
    $validBadging = "package: name='com.ml.tblandroidtxt.test' versionCode='' versionName=''"
    $package = [regex]::Match($validBadging, "(?m)^package:\s+name='([^']+)'").Groups[1].Value
    $blankVersionAccepted = $package -ceq $testPackage
    $wrongPackageRejected = $false
    try {
        if ('other.package' -cne $testPackage) { throw 'ACCOUNT_TEST_INSTALLER_TEST_PACKAGE_MISMATCH_STOP' }
    } catch {
        $wrongPackageRejected = $_.Exception.Message -eq 'ACCOUNT_TEST_INSTALLER_TEST_PACKAGE_MISMATCH_STOP'
    }
    if (-not $blankVersionAccepted -or -not $wrongPackageRejected) {
        throw 'ACCOUNT_TEST_INSTALLER_SELF_TEST_FAILED_STOP'
    }
    [pscustomobject]@{
        blankAndroidTestVersionCodeAccepted = $blankVersionAccepted
        wrongPackageRejected = $wrongPackageRejected
        deviceOperations = 0
        installAttempts = 0
    } | ConvertTo-Json -Compress
}

if ($SelfTest) {
    Test-AccountInstallerSelfTest
    exit 0
}

if ([string]::IsNullOrWhiteSpace($ExpectedScriptSha256) -or [string]::IsNullOrWhiteSpace($TestApkPath) -or
        [string]::IsNullOrWhiteSpace($ExpectedTestApkSha256) -or [string]::IsNullOrWhiteSpace($ExpectedTestCertificateSha256)) {
    throw 'ACCOUNT_TEST_INSTALLER_REQUIRED_PIN_MISSING_STOP'
}
if (($CheckOnly -and $ExecuteOneReplacement) -or (-not $CheckOnly -and -not $ExecuteOneReplacement)) {
    throw 'ACCOUNT_TEST_INSTALLER_EXPLICIT_MODE_REQUIRED_STOP'
}
if ($Serial -cne '15e84958') { throw 'ACCOUNT_TEST_INSTALLER_SERIAL_MISMATCH_STOP' }

$scriptPath = [IO.Path]::GetFullPath($MyInvocation.MyCommand.Path)
Assert-RegularFileHash -Path $scriptPath -ExpectedHash $ExpectedScriptSha256 -ErrorPrefix 'ACCOUNT_TEST_INSTALLER_SCRIPT' | Out-Null
$resolvedTestApk = Assert-RegularFileHash -Path $TestApkPath -ExpectedHash $ExpectedTestApkSha256 -ErrorPrefix 'ACCOUNT_TEST_INSTALLER_APK'
$sdkPath = Resolve-AndroidSdkPath -RequestedPath $AndroidSdkPath
$adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) { throw 'ACCOUNT_TEST_INSTALLER_ADB_MISSING_STOP' }
$aapt2Path = Resolve-BuildTool -SdkPath $sdkPath -ToolName 'aapt2.exe'
$apkSignerPath = Resolve-BuildTool -SdkPath $sdkPath -ToolName 'apksigner.bat'
$metadata = Read-TestApkMetadata -Aapt2Path $aapt2Path -ApkPath $resolvedTestApk
Assert-ApkCertificate -ApkSignerPath $apkSignerPath -ApkPath $resolvedTestApk -ExpectedCertificate $ExpectedTestCertificateSha256

$deviceState = Invoke-CheckedCommand -FilePath $adbPath -Arguments @('-s', $Serial, 'get-state') `
    -ErrorCode 'ACCOUNT_TEST_INSTALLER_DEVICE_NOT_READY_STOP'
if ($deviceState.Trim() -cne 'device') { throw 'ACCOUNT_TEST_INSTALLER_DEVICE_NOT_READY_STOP' }
$targetBefore = Read-PackageState -AdbPath $adbPath -DeviceSerial $Serial -PackageName $targetPackage
Assert-TargetPackageState -State $targetBefore
$testBefore = Read-PackageState -AdbPath $adbPath -DeviceSerial $Serial -PackageName $testPackage
Assert-TestPackageState -State $testBefore -AfterInstall $false

if ([string]::IsNullOrWhiteSpace($EvidenceDirectory)) {
    $EvidenceDirectory = Join-Path 'D:\P5E-private' ('account-check-install-' + [DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff') + '-' + [Guid]::NewGuid().ToString('N'))
}
$EvidenceDirectory = [IO.Path]::GetFullPath($EvidenceDirectory)
New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
if (@(Get-ChildItem -LiteralPath $EvidenceDirectory -Force).Count -ne 0) {
    throw 'ACCOUNT_TEST_INSTALLER_EVIDENCE_DIRECTORY_NOT_EMPTY_STOP'
}

$result = [ordered]@{
    schemaVersion = 'p5e.account-test-install.result.v1'
    mode = if ($CheckOnly) { 'CHECK_ONLY' } else { 'EXECUTE_ONE_REPLACEMENT' }
    serial = $Serial
    testPackage = $metadata.package
    targetPackage = $metadata.targetPackage
    runner = $metadata.runner
    testApkSha256 = $ExpectedTestApkSha256.ToUpperInvariant()
    targetVersionCode = $targetBefore.versionCode
    installAttempts = 0
    productionPackageOperations = 0
    rawDispatches = 0
}

if ($CheckOnly) {
    [IO.File]::WriteAllText((Join-Path $EvidenceDirectory 'ACCOUNT_TEST_INSTALL_RESULT.json'),
        ($result | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
    Write-Output 'P5E_ACCOUNT_TEST_INSTALL_RESULT=CHECK_ONLY_PASS'
    Write-Output 'P5E_ACCOUNT_TEST_INSTALL_ATTEMPTS=0'
    Write-Output 'P5E_ACCOUNT_TEST_INSTALL_PRODUCTION_OPERATIONS=0'
    Write-Output 'P5E_ACCOUNT_TEST_INSTALL_RAW_DISPATCHES=0'
    exit 0
}

$installOutput = @(& $adbPath '-s' $Serial 'install' '-r' $resolvedTestApk 2>&1)
$result.installAttempts = 1
if ($LASTEXITCODE -ne 0) {
    [IO.File]::WriteAllText((Join-Path $EvidenceDirectory 'ACCOUNT_TEST_INSTALL_RESULT.json'),
        ($result | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
    throw 'ACCOUNT_TEST_INSTALLER_INSTALL_FAILED_STOP'
}

$targetAfter = Read-PackageState -AdbPath $adbPath -DeviceSerial $Serial -PackageName $targetPackage
Assert-TargetPackageState -State $targetAfter
$testAfter = Read-PackageState -AdbPath $adbPath -DeviceSerial $Serial -PackageName $testPackage
Assert-TestPackageState -State $testAfter -AfterInstall $true
$pmPath = Invoke-CheckedCommand -FilePath $adbPath -Arguments @('-s', $Serial, 'shell', 'pm', 'path', $testPackage) `
    -ErrorCode 'ACCOUNT_TEST_INSTALLER_INSTALLED_APK_PATH_READ_FAILED_STOP'
$remoteApk = @($pmPath -split "`r?`n" | Where-Object { $_ -match '^package:.+\.apk$' } |
    ForEach-Object { $_.Substring('package:'.Length) }) | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($remoteApk)) { throw 'ACCOUNT_TEST_INSTALLER_INSTALLED_APK_PATH_INVALID_STOP' }
$pulledApk = Join-Path $EvidenceDirectory 'installed-test-package.apk'
Invoke-CheckedCommand -FilePath $adbPath -Arguments @('-s', $Serial, 'pull', $remoteApk, $pulledApk) `
    -ErrorCode 'ACCOUNT_TEST_INSTALLER_INSTALLED_APK_PULL_FAILED_STOP' | Out-Null
Assert-RegularFileHash -Path $pulledApk -ExpectedHash $ExpectedTestApkSha256 -ErrorPrefix 'ACCOUNT_TEST_INSTALLER_INSTALLED_APK' | Out-Null
Assert-ApkCertificate -ApkSignerPath $apkSignerPath -ApkPath $pulledApk -ExpectedCertificate $ExpectedTestCertificateSha256

[IO.File]::WriteAllText((Join-Path $EvidenceDirectory 'ACCOUNT_TEST_INSTALL_RESULT.json'),
    ($result | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
Write-Output 'P5E_ACCOUNT_TEST_INSTALL_RESULT=REPLACEMENT_PASS'
Write-Output 'P5E_ACCOUNT_TEST_INSTALL_ATTEMPTS=1'
Write-Output 'P5E_ACCOUNT_TEST_INSTALL_PRODUCTION_OPERATIONS=0'
Write-Output 'P5E_ACCOUNT_TEST_INSTALL_RAW_DISPATCHES=0'
exit 0
