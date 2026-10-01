[CmdletBinding()]
param(
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($OutputPath)) { $OutputPath = Join-Path $repoRoot 'docs\P5E_PRODUCTION_VERSION_CONTRACT_QA_20260925.json' }
$helperPath = Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1'
$buildInfoPath = Join-Path $repoRoot 'artifacts\builds\v4.18-p5e.2\build-20261001-105237\BUILD_INFO.json'
$buildInfo = Get-Content -Raw -LiteralPath $buildInfoPath | ConvertFrom-Json
$apkPath = Join-Path (Split-Path -Parent $buildInfoPath) ([string]$buildInfo.apk)
if (-not (Test-Path -LiteralPath $apkPath -PathType Leaf)) { throw 'IMMUTABLE_PRODUCTION_APK_NOT_FOUND' }
$expectedVersionName = [string]$buildInfo.versionName
$expectedVersionCode = [long]$buildInfo.versionCode
$expectedApkSha256 = ([string]$buildInfo.apkSha256).ToLowerInvariant()

$results = [System.Collections.Generic.List[object]]::new()
function Assert-QA {
    param([Parameter(Mandatory = $true)][string]$Name, [Parameter(Mandatory = $true)][bool]$Condition)
    [void]$script:results.Add([ordered]@{ name = $Name; passed = $Condition })
    if (-not $Condition) { throw ('QA_ASSERTION_FAILED:' + $Name) }
}
function Invoke-ReadbackCase {
    param(
        [Parameter(Mandatory = $true)]$Module,
        [Parameter(Mandatory = $true)][string]$EvidenceDirectory,
        [Parameter(Mandatory = $true)][long]$VersionCode,
        [Parameter(Mandatory = $true)][string]$VersionName,
        [Parameter(Mandatory = $true)][string]$ExpectedVersion,
        [Parameter(Mandatory = $true)][long]$ExpectedCode
    )
    & $Module {
        param($code, $name, $expectedVersion, $expectedCode, $directory, $apkHash)
        $script:syntheticVersionCode = $code
        $script:syntheticVersionName = $name
        Get-P5EPackageReadback -AdbPath 'synthetic-adb.exe' -SerialValue 'synthetic-serial' `
            -EvidenceDirectory $directory -PackageName 'com.ml.tblandroidtxt' -ExpectedApkSha256 $apkHash `
            -LocalFileName 'installed-production-before.apk' -Name 'production-before' `
            -ApkSignerJavaPath 'synthetic-java.exe' -ApkSignerJarPath 'synthetic-apksigner.jar' `
            -ExpectedVersionCode $expectedCode -ExpectedVersion $expectedVersion
    } $VersionCode $VersionName $ExpectedVersion $ExpectedCode $EvidenceDirectory $expectedApkSha256
}

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-production-version-contract-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $tempRoot -Force)
$module = $null
try {
    $module = New-Module -Name ('P5EProductionVersionQA_' + [Guid]::NewGuid().ToString('N')) -ScriptBlock {
        param([string]$HelperFile, [string]$SourceApk, [string]$ExpectedHash)
        . $HelperFile -LibraryOnly
        $script:P5ESerial = 'synthetic-serial'
        $script:syntheticVersionCode = 207L
        $script:syntheticVersionName = '4.17-p5e.11'
        function Invoke-P5EAdbShellReadOnly {
            param([string]$AdbPath, [string]$SerialValue, [string]$Operation, [string[]]$RemoteTokens, [long]$TimeoutMilliseconds)
            if ($Operation -like 'pm-path-*') {
                $stdout = 'package:/data/app/com.ml.tblandroidtxt/base.apk'
                return [pscustomobject]@{
                    Stdout = $stdout
                    Stderr = ''
                    ExitCode = 0
                    TimedOut = $false
                    LaunchCount = 1L
                    CaptureBounded = $true
                    OutputTooLarge = $false
                    RedactionViolation = $false
                    ByteLength = [long][Text.Encoding]::UTF8.GetByteCount($stdout)
                    HostSha256 = ''
                    LaunchErrorClass = ''
                    LaunchNativeErrorCode = $null
                    LaunchReason = ''
                    TimeoutMilliseconds = $TimeoutMilliseconds
                    Outcome = 'EXITED'
                }
            }
            if ($Operation -like 'package-dump-*') {
                $stdout = ('versionName=' + $script:syntheticVersionName + "`nversionCode=" + [string]$script:syntheticVersionCode)
                return [pscustomobject]@{
                    Stdout = $stdout
                    Stderr = ''
                    ExitCode = 0
                    TimedOut = $false
                    LaunchCount = 1L
                    CaptureBounded = $true
                    OutputTooLarge = $false
                    RedactionViolation = $false
                    ByteLength = [long][Text.Encoding]::UTF8.GetByteCount($stdout)
                    HostSha256 = ''
                    LaunchErrorClass = ''
                    LaunchNativeErrorCode = $null
                    LaunchReason = ''
                    TimeoutMilliseconds = $TimeoutMilliseconds
                    Outcome = 'EXITED'
                }
            }
            throw ('UNEXPECTED_SYNTHETIC_ADB_OPERATION:' + $Operation)
        }
        function Get-P5EApkSignerDigest {
            param([string]$JavaPath, [string]$ApkSignerJarPath, [string]$ApkPath, [string]$Name, [string]$EvidenceDirectory)
            return $script:P5ECertificateSha256
        }
        Export-ModuleMember -Function Get-P5EPackageReadback
    } -ArgumentList $helperPath, $apkPath, $expectedApkSha256
    Import-Module $module -Force | Out-Null
    $evidence = Join-Path $tempRoot 'evidence'
    [void](New-Item -ItemType Directory -Path $evidence -Force)
    Copy-Item -LiteralPath $apkPath -Destination (Join-Path $evidence 'installed-production-before.apk')

    $configuredVersion = & $module { $script:P5EProductionVersion }
    Assert-QA 'helper-expected-matches-independent-build-info' ($configuredVersion -ceq $expectedVersionName)
    $green = Invoke-ReadbackCase -Module $module -EvidenceDirectory $evidence -VersionCode $expectedVersionCode `
        -VersionName $expectedVersionName -ExpectedVersion $expectedVersionName -ExpectedCode $expectedVersionCode
    Assert-QA 'build-info-derived-version-accepted' ([string]$green.version -ceq $expectedVersionName -and [long]$green.versionCode -eq $expectedVersionCode)

    $oldExpected = 'v' + $expectedVersionName
    $oldRejected = $false
    try { Invoke-ReadbackCase -Module $module -EvidenceDirectory $evidence -VersionCode $expectedVersionCode -VersionName $expectedVersionName -ExpectedVersion $oldExpected -ExpectedCode $expectedVersionCode | Out-Null } catch { $oldRejected = $_.Exception.Message -like '*P5E_COLLECTOR_PACKAGE_VERSION_MISMATCH*' }
    Assert-QA 'old-v-prefixed-expected-rejected' $oldRejected

    $wrongNameRejected = $false
    try { Invoke-ReadbackCase -Module $module -EvidenceDirectory $evidence -VersionCode $expectedVersionCode -VersionName '4.17-p5e.10' -ExpectedVersion $expectedVersionName -ExpectedCode $expectedVersionCode | Out-Null } catch { $wrongNameRejected = $_.Exception.Message -like '*P5E_COLLECTOR_PACKAGE_VERSION_MISMATCH*' }
    Assert-QA 'wrong-version-name-rejected' $wrongNameRejected

    $wrongCodeRejected = $false
    try { Invoke-ReadbackCase -Module $module -EvidenceDirectory $evidence -VersionCode ($expectedVersionCode - 1) -VersionName $expectedVersionName -ExpectedVersion $expectedVersionName -ExpectedCode $expectedVersionCode | Out-Null } catch { $wrongCodeRejected = $_.Exception.Message -like '*P5E_COLLECTOR_PACKAGE_VERSION_CODE_MISMATCH*' }
    Assert-QA 'wrong-version-code-rejected' $wrongCodeRejected

    $report = [ordered]@{
        schemaVersion = 1
        status = 'PASS'
        scope = 'OFFLINE_LIBRARY_ONLY_PACKAGE_READBACK_VERSION_CONTRACT'
        buildInfoPath = $buildInfoPath
        expectedVersionName = $expectedVersionName
        expectedVersionCode = $expectedVersionCode
        expectedApkSha256 = $expectedApkSha256
        cases = @($results)
        adbLaunches = 0
        deviceActions = 0
        providerCalls = 0
        rawReads = 0
    }
    $out = [IO.Path]::GetFullPath($OutputPath)
    [IO.Directory]::CreateDirectory((Split-Path -Parent $out)) | Out-Null
    [IO.File]::WriteAllText($out, ($report | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
    Write-Output 'P5E_PRODUCTION_VERSION_CONTRACT_PASS'
} finally {
    if ($null -ne $module) { Remove-Module -ModuleInfo $module -Force }
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
