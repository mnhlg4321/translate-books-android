[CmdletBinding()]
param(
    [string]$ResultPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$preflightPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'p5e-account-check-device-preflight.ps1'))
if ([string]::IsNullOrWhiteSpace($ResultPath)) {
    $ResultPath = Join-Path $repoRoot 'docs\P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA_20260924_POST_STOP.json'
}
$ResultPath = [IO.Path]::GetFullPath($ResultPath)

$assertionCount = 0
$cases = [ordered]@{}
$failures = New-Object System.Collections.Generic.List[string]

function Assert-P5ETrue {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$ErrorCode
    )
    $script:assertionCount++
    if (-not $Condition) { throw $ErrorCode }
}

function Get-P5EFileHash {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256 -ErrorAction Stop).Hash.ToUpperInvariant()
}

function Invoke-P5EPreflightChild {
    param(
        [Parameter(Mandatory = $true)][string]$EvidencePath,
        [string]$Mode = 'pass',
        [int]$DeviceTimeout = 30000,
        [int]$PullTimeout = 60000,
        [int]$CaptureBytes = 262144,
        [string]$ExpectedScriptHash = '',
        [string]$ExpectedTargetHash = '',
        [string]$ExpectedTestHash = ''
    )

    $scriptHash = if ([string]::IsNullOrWhiteSpace($ExpectedScriptHash)) {
        Get-P5EFileHash -Path $preflightPath
    } else { $ExpectedScriptHash }
    $arguments = @(
        '-NoLogo', '-NoProfile', '-NonInteractive', '-File', $preflightPath,
        '-ExpectedScriptSha256', $scriptHash,
        '-ReferenceTestApkPath', $script:referenceApk,
        '-ExpectedTestApkSha256', $(if ($ExpectedTestHash) { $ExpectedTestHash } else { $script:testHash }),
        '-ExpectedTestCertificateSha256', $script:expectedCertificate,
        '-ExpectedTargetApkSha256', $(if ($ExpectedTargetHash) { $ExpectedTargetHash } else { $script:targetHash }),
        '-ExpectedTargetCertificateSha256', $script:expectedCertificate,
        '-AdbPath', $script:fakeAdb,
        '-ApkSignerPath', $script:fakeApkSigner,
        '-EvidenceDirectory', $EvidencePath,
        '-DeviceReadTimeoutMilliseconds', [string]$DeviceTimeout,
        '-PullTimeoutMilliseconds', [string]$PullTimeout,
        '-MaxCaptureBytes', [string]$CaptureBytes
    )
    $previousMode = [Environment]::GetEnvironmentVariable('P5E_FAKE_MODE', 'Process')
    [Environment]::SetEnvironmentVariable('P5E_FAKE_MODE', $Mode, 'Process')
    try {
        $output = @(& powershell.exe @arguments 2>&1)
        return [pscustomobject]@{
            Output = @($output | ForEach-Object { [string]$_ })
            ExitCode = [int]$LASTEXITCODE
            EvidencePath = $EvidencePath
        }
    } finally {
        [Environment]::SetEnvironmentVariable('P5E_FAKE_MODE', $previousMode, 'Process')
    }
}

function Get-P5EReceipt {
    param([Parameter(Mandatory = $true)][string]$EvidencePath)
    $path = Join-Path $EvidencePath 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json'
    Assert-P5ETrue -Condition (Test-Path -LiteralPath $path -PathType Leaf) -ErrorCode 'RECEIPT_MISSING'
    return Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
}

function Get-P5EFunctionTextFromSource {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $tokens = $null
    $parseErrors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseInput($Source, [ref]$tokens, [ref]$parseErrors)
    Assert-P5ETrue -Condition ($parseErrors.Count -eq 0) -ErrorCode ('SOURCE_PARSE_FAILED_' + $Name)
    $functionAst = $ast.Find({
            param($node)
            $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $Name
        }, $true) | Select-Object -First 1
    Assert-P5ETrue -Condition ($null -ne $functionAst) -ErrorCode ('SOURCE_FUNCTION_MISSING_' + $Name)
    return $functionAst.Extent.Text
}

function Remove-P5EParsedFunction {
    param([Parameter(Mandatory = $true)][string]$Name)
    Remove-Item -Path ('Function:\' + $Name) -Force -ErrorAction SilentlyContinue
}

function Assert-P5EStopCase {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$ExpectedTypedError,
        [Parameter(Mandatory = $true)]$Run,
        [scriptblock]$ExtraCheck = { param($receipt) }
    )
    Assert-P5ETrue -Condition ($Run.ExitCode -ne 0) -ErrorCode ($Name + '_EXIT_ZERO')
    $receipt = Get-P5EReceipt -EvidencePath $Run.EvidencePath
    Assert-P5ETrue -Condition ($receipt.status -eq 'STOP') -ErrorCode ($Name + '_NOT_STOP')
    Assert-P5ETrue -Condition ($receipt.typedError -eq $ExpectedTypedError) `
        -ErrorCode ($Name + '_WRONG_TYPED_ERROR_' + [string]$receipt.typedError)
    & $ExtraCheck $receipt
    $script:cases[$Name] = 'PASS'
}

$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-account-preflight-qa-' + [Guid]::NewGuid().ToString('N'))
$originalExpected = [Environment]::GetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'Process')
$hadOriginalExpected = $null -ne $originalExpected
$envNames = @(
    'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'P5E_FAKE_LOG', 'P5E_FAKE_MODE',
    'P5E_FAKE_SOURCE_APK', 'P5E_FAKE_TARGET_APK', 'P5E_FAKE_TEST_APK',
    'P5E_FAKE_TEST_SIGNATURE', 'P5E_FAKE_CERTIFICATE')

try {
    New-Item -ItemType Directory -Path $temporaryRoot -ErrorAction Stop | Out-Null
    $script:referenceApk = Join-Path $temporaryRoot 'reference-test.apk'
    $script:testApk = Join-Path $temporaryRoot 'test-installed.apk'
    $script:targetApk = Join-Path $temporaryRoot 'production-installed.apk'
    $wrongApk = Join-Path $temporaryRoot 'wrong.apk'
    [IO.File]::WriteAllBytes($script:referenceApk, [Text.Encoding]::UTF8.GetBytes('P5E_SYNTHETIC_TEST_APK_BYTES'))
    [IO.File]::WriteAllBytes($script:testApk, [Text.Encoding]::UTF8.GetBytes('P5E_SYNTHETIC_TEST_APK_BYTES'))
    [IO.File]::WriteAllBytes($script:targetApk, [Text.Encoding]::UTF8.GetBytes('P5E_SYNTHETIC_PRODUCTION_APK_BYTES'))
    [IO.File]::WriteAllBytes($wrongApk, [Text.Encoding]::UTF8.GetBytes('P5E_SYNTHETIC_WRONG_APK_BYTES'))
    $script:testHash = Get-P5EFileHash -Path $script:testApk
    $script:targetHash = Get-P5EFileHash -Path $script:targetApk
    $script:expectedCertificate = 'A' * 64
    $script:syntheticExpected = 'E' * 64

    $script:fakeLog = Join-Path $temporaryRoot 'fake-adb.log'
    $script:fakeAdb = Join-Path $temporaryRoot 'fake-adb.cmd'
    $script:fakeApkSigner = Join-Path $temporaryRoot 'fake-apksigner.cmd'
    @'
@echo off
setlocal EnableExtensions
if defined P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT exit /b 97
if not "%P5E_FAKE_LOG%"=="" echo %*>>"%P5E_FAKE_LOG%"
if /i "%P5E_FAKE_MODE%"=="timeout" if /i "%3"=="get-state" (
  powershell.exe -NoLogo -NoProfile -NonInteractive -Command "Start-Sleep -Seconds 5"
  exit /b 0
)
if /i "%P5E_FAKE_MODE%"=="large-output" if /i "%3"=="get-state" (
  for /L %%N in (1,1,400) do echo 123456789012345678901234567890123456789012345678901234567890
  exit /b 0
)
if /i "%P5E_FAKE_MODE%"=="tool-nonzero" exit /b 17
if /i "%1"=="-s" (
  shift
  shift
)
if /i "%1"=="get-state" (
  if /i "%P5E_FAKE_MODE%"=="offline" echo unauthorized
  if /i not "%P5E_FAKE_MODE%"=="offline" echo device
  exit /b 0
)
if /i "%1"=="shell" if /i "%2"=="dumpsys" (
  if /i "%4"=="com.ml.tblandroidtxt" (
    if /i "%P5E_FAKE_MODE%"=="metadata-package-absent" (
      echo Unable to find package com.ml.tblandroidtxt
      exit /b 0
    )
    if /i "%P5E_FAKE_MODE%"=="metadata-empty" exit /b 0
    echo Package [com.ml.tblandroidtxt]:
    if /i "%P5E_FAKE_MODE%"=="metadata-package-duplicate" echo Package [com.ml.tblandroidtxt]:
    if /i "%P5E_FAKE_MODE%"=="metadata-version-invalid" echo versionCode=not-a-number
    if /i "%P5E_FAKE_MODE%"=="metadata-version-duplicate" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="wrong-version" echo versionCode=206 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" echo   versionCode = 207 minSdk=23
    if /i not "%P5E_FAKE_MODE%"=="metadata-version-missing" if /i not "%P5E_FAKE_MODE%"=="metadata-version-invalid" if /i not "%P5E_FAKE_MODE%"=="metadata-version-duplicate" if /i not "%P5E_FAKE_MODE%"=="wrong-version" if /i not "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" if /i not "%P5E_FAKE_MODE%"=="aosp-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-current-multiple" if /i not "%P5E_FAKE_MODE%"=="aosp-current-missing" if /i not "%P5E_FAKE_MODE%"=="aosp-identity-pin" if /i not "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-conflicting-format" if /i not "%P5E_FAKE_MODE%"=="aosp-truncated" if /i not "%P5E_FAKE_MODE%"=="aosp-nonhex" if /i not "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="metadata-version-duplicate" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-wrapper" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-current-multiple" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-current-missing" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-identity-pin" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-conflicting-format" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-truncated" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-nonhex" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo versionCode=207 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" echo   signatures: [abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-signature-invalid" echo signatures:[not-hex]
    if /i "%P5E_FAKE_MODE%"=="metadata-signature-duplicate" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-inline-wrapper" echo signatures: [PackageSignatures{signatures:[abebea4b]}]
    if /i "%P5E_FAKE_MODE%"=="aosp-wrapper" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-current-multiple" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b, cafebabe], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-current-missing" echo signatures=PackageSignatures{abc123 version:3, signatures:[], past signatures:[abebea4b flags: 1]}
    if /i "%P5E_FAKE_MODE%"=="aosp-identity-pin" echo signatures=PackageSignatures{abebea4b version:3, signatures:[cafebabe], past signatures:[abebea4b flags: 1]}
    if /i "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" echo signatures=PackageSignatures{def456 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-conflicting-format" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-conflicting-format" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="aosp-truncated" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]
    if /i "%P5E_FAKE_MODE%"=="aosp-nonhex" echo signatures=PackageSignatures{abc123 version:3, signatures:[not-hex], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[cafebabe]}
    if /i not "%P5E_FAKE_MODE%"=="metadata-signature-missing" if /i not "%P5E_FAKE_MODE%"=="metadata-signature-invalid" if /i not "%P5E_FAKE_MODE%"=="metadata-signature-duplicate" if /i not "%P5E_FAKE_MODE%"=="metadata-inline-wrapper" if /i not "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" if /i not "%P5E_FAKE_MODE%"=="aosp-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-current-multiple" if /i not "%P5E_FAKE_MODE%"=="aosp-current-missing" if /i not "%P5E_FAKE_MODE%"=="aosp-identity-pin" if /i not "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-conflicting-format" if /i not "%P5E_FAKE_MODE%"=="aosp-truncated" if /i not "%P5E_FAKE_MODE%"=="aosp-nonhex" if /i not "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-signature-duplicate" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-stderr" echo metadata warning 1>&2
    exit /b 0
  )
  if /i "%4"=="com.ml.tblandroidtxt.test" (
    if /i "%P5E_FAKE_MODE%"=="metadata-package-absent" (
      echo Unable to find package com.ml.tblandroidtxt.test
      exit /b 0
    )
    if /i "%P5E_FAKE_MODE%"=="metadata-empty" exit /b 0
    echo Package [com.ml.tblandroidtxt.test]:
    if /i "%P5E_FAKE_MODE%"=="metadata-package-duplicate" echo Package [com.ml.tblandroidtxt.test]:
    if /i "%P5E_FAKE_MODE%"=="metadata-version-invalid" echo versionCode=not-a-number
    if /i "%P5E_FAKE_MODE%"=="metadata-version-duplicate" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" echo   versionCode = 1 minSdk=23
    if /i not "%P5E_FAKE_MODE%"=="metadata-version-missing" if /i not "%P5E_FAKE_MODE%"=="metadata-version-invalid" if /i not "%P5E_FAKE_MODE%"=="metadata-version-duplicate" if /i not "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" if /i not "%P5E_FAKE_MODE%"=="aosp-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-current-multiple" if /i not "%P5E_FAKE_MODE%"=="aosp-current-missing" if /i not "%P5E_FAKE_MODE%"=="aosp-identity-pin" if /i not "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-conflicting-format" if /i not "%P5E_FAKE_MODE%"=="aosp-truncated" if /i not "%P5E_FAKE_MODE%"=="aosp-nonhex" if /i not "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="metadata-version-duplicate" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-wrapper" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-current-multiple" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-current-missing" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-identity-pin" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-conflicting-format" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-truncated" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-nonhex" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo versionCode=1 minSdk=23
    if /i "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" echo   signatures: [abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-signature-invalid" echo signatures:[not-hex]
    if /i "%P5E_FAKE_MODE%"=="metadata-signature-duplicate" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-inline-wrapper" echo signatures: [PackageSignatures{signatures:[abebea4b]}]
    if /i "%P5E_FAKE_MODE%"=="wrong-test-signature" echo signatures:[deadbeef]
    if /i "%P5E_FAKE_MODE%"=="aosp-wrapper" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-current-multiple" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b, cafebabe], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-current-missing" echo signatures=PackageSignatures{abc123 version:3, signatures:[], past signatures:[abebea4b flags: 1]}
    if /i "%P5E_FAKE_MODE%"=="aosp-identity-pin" echo signatures=PackageSignatures{abebea4b version:3, signatures:[cafebabe], past signatures:[abebea4b flags: 1]}
    if /i "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" echo signatures=PackageSignatures{def456 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-conflicting-format" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-conflicting-format" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="aosp-truncated" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[]
    if /i "%P5E_FAKE_MODE%"=="aosp-nonhex" echo signatures=PackageSignatures{abc123 version:3, signatures:[not-hex], past signatures:[]}
    if /i "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[cafebabe]}
    if /i not "%P5E_FAKE_MODE%"=="metadata-signature-missing" if /i not "%P5E_FAKE_MODE%"=="metadata-signature-invalid" if /i not "%P5E_FAKE_MODE%"=="metadata-signature-duplicate" if /i not "%P5E_FAKE_MODE%"=="metadata-inline-wrapper" if /i not "%P5E_FAKE_MODE%"=="real-dumpsys-spacing" if /i not "%P5E_FAKE_MODE%"=="wrong-test-signature" if /i not "%P5E_FAKE_MODE%"=="aosp-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-current-multiple" if /i not "%P5E_FAKE_MODE%"=="aosp-current-missing" if /i not "%P5E_FAKE_MODE%"=="aosp-identity-pin" if /i not "%P5E_FAKE_MODE%"=="aosp-duplicate-wrapper" if /i not "%P5E_FAKE_MODE%"=="aosp-conflicting-format" if /i not "%P5E_FAKE_MODE%"=="aosp-truncated" if /i not "%P5E_FAKE_MODE%"=="aosp-nonhex" if /i not "%P5E_FAKE_MODE%"=="aosp-past-malformed" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-signature-duplicate" echo signatures:[abebea4b]
    if /i "%P5E_FAKE_MODE%"=="metadata-stderr" echo metadata warning 1>&2
    exit /b 0
  )
)
if /i "%1"=="shell" if /i "%2"=="pm" if /i "%3"=="path" (
  if /i "%4"=="com.ml.tblandroidtxt" (
    if /i "%P5E_FAKE_MODE%"=="path-split" echo package:/data/app/fake-production/base.apk&echo package:/data/app/fake-production/split_config.arm64_v8a.apk
    if /i not "%P5E_FAKE_MODE%"=="path-split" echo package:/data/app/fake-production/base.apk
    exit /b 0
  )
  if /i "%4"=="com.ml.tblandroidtxt.test" (
    echo package:/data/app/fake-test/base.apk
    exit /b 0
  )
)
if /i "%1"=="pull" (
  if /i "%2"=="/data/app/fake-production/base.apk" (
    if /i "%P5E_FAKE_MODE%"=="wrong-production-hash" copy /y "%P5E_FAKE_SOURCE_APK%" "%3" >nul
    if /i not "%P5E_FAKE_MODE%"=="wrong-production-hash" copy /y "%P5E_FAKE_TARGET_APK%" "%3" >nul
    exit /b 0
  )
  if /i "%2"=="/data/app/fake-test/base.apk" (
    if /i "%P5E_FAKE_MODE%"=="wrong-test-hash" copy /y "%P5E_FAKE_SOURCE_APK%" "%3" >nul
    if /i not "%P5E_FAKE_MODE%"=="wrong-test-hash" copy /y "%P5E_FAKE_TEST_APK%" "%3" >nul
    exit /b 0
  )
)
exit /b 99
'@ | Set-Content -LiteralPath $script:fakeAdb -NoNewline -Encoding ascii
    @'
@echo off
setlocal EnableExtensions
if defined P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT exit /b 97
set cert=%P5E_FAKE_CERTIFICATE%
if /i "%P5E_FAKE_MODE%"=="wrong-reference-certificate" set cert=0000000000000000000000000000000000000000000000000000000000000000
if /i "%P5E_FAKE_MODE%"=="wrong-production-certificate" echo %~3|findstr /i "installed-production" >nul && set cert=0000000000000000000000000000000000000000000000000000000000000000
if /i "%P5E_FAKE_MODE%"=="wrong-test-certificate" echo %~3|findstr /i "installed-test" >nul && set cert=0000000000000000000000000000000000000000000000000000000000000000
echo Signer #1 certificate SHA-256 digest: %cert%
exit /b 0
'@ | Set-Content -LiteralPath $script:fakeApkSigner -NoNewline -Encoding ascii

    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $null, 'Process') }
    [Environment]::SetEnvironmentVariable('P5E_FAKE_LOG', $script:fakeLog, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_FAKE_SOURCE_APK', $wrongApk, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_FAKE_TARGET_APK', $script:targetApk, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_FAKE_TEST_APK', $script:testApk, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_FAKE_TEST_SIGNATURE', 'abebea4b', 'Process')
    [Environment]::SetEnvironmentVariable('P5E_FAKE_CERTIFICATE', $script:expectedCertificate, 'Process')
    [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $script:syntheticExpected, 'Process')

    $passEvidence = Join-Path $temporaryRoot 'pass-evidence'
    $pass = Invoke-P5EPreflightChild -EvidencePath $passEvidence -Mode 'pass'
    Assert-P5ETrue -Condition ($pass.ExitCode -eq 0 -and $pass.Output -contains 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT=PASS') `
        -ErrorCode 'PASS_SIGNAL_MISSING'
    $passReceipt = Get-P5EReceipt -EvidencePath $passEvidence
    Assert-P5ETrue -Condition ($passReceipt.status -eq 'PASS' -and $passReceipt.schemaVersion -eq 'p5e.account-check.device-preflight.result.v3') `
        -ErrorCode 'PASS_RECEIPT_SCHEMA_INVALID'
    Assert-P5ETrue -Condition ($passReceipt.deviceReadAttempts -eq 7 -and $passReceipt.attemptedCommandCount -ge 10) `
        -ErrorCode 'PASS_COMMAND_COUNTS_INVALID'
    Assert-P5ETrue -Condition ($passReceipt.targetApkSha256 -eq $script:targetHash -and $passReceipt.testApkSha256 -eq $script:testHash -and
            $passReceipt.targetCertificateSha256 -eq $script:expectedCertificate -and $passReceipt.testCertificateSha256 -eq $script:expectedCertificate) `
        -ErrorCode 'PASS_BOTH_PACKAGE_PINS_INVALID'
    Assert-P5ETrue -Condition ($passReceipt.targetMetadataPackagePresent -and $passReceipt.targetMetadataReason -eq 'PASS' -and
            $passReceipt.targetMetadataVersionCandidateCount -eq 1 -and $passReceipt.targetMetadataSignatureCandidateCount -eq 1 -and
            $passReceipt.testMetadataPackagePresent -and $passReceipt.testMetadataReason -eq 'PASS' -and
            $passReceipt.testMetadataVersionCandidateCount -eq 1 -and $passReceipt.testMetadataSignatureCandidateCount -eq 1) `
        -ErrorCode 'PASS_METADATA_DIAGNOSTICS_INVALID'
    Assert-P5ETrue -Condition ($passReceipt.installAttempts -eq 0 -and $passReceipt.productionPackageOperations -eq 0 -and
            $passReceipt.providerCalls -eq 0 -and $passReceipt.dbWrites -eq 0 -and $passReceipt.rawDispatches -eq 0) `
        -ErrorCode 'PASS_SCOPE_COUNTER_INVALID'
    Assert-P5ETrue -Condition ([Environment]::GetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'Process') -ceq $script:syntheticExpected) `
        -ErrorCode 'PARENT_EXPECTED_VALUE_CHANGED'
    $adbLines = @(Get-Content -LiteralPath $script:fakeLog)
    Assert-P5ETrue -Condition ($adbLines.Count -eq 7 -and $adbLines[0] -match 'get-state' -and
            $adbLines[1] -match 'dumpsys' -and $adbLines[2] -match 'dumpsys' -and
            $adbLines[3] -match 'shell pm path' -and $adbLines[4] -match 'pull' -and
            $adbLines[5] -match 'shell pm path' -and $adbLines[6] -match 'pull') `
        -ErrorCode 'PASS_ADB_ORDER_INVALID'
    $cases.pass_full_identity_read_only = 'PASS'

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-wrapper') -Mode 'aosp-wrapper'
    Assert-P5ETrue -Condition ($run.ExitCode -eq 0 -and $run.Output -contains 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT=PASS') `
        -ErrorCode 'AOSP_WRAPPER_PASS_SIGNAL_MISSING'
    $aospReceipt = Get-P5EReceipt -EvidencePath $run.EvidencePath
    Assert-P5ETrue -Condition ($aospReceipt.status -eq 'PASS' -and
            $aospReceipt.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
            $aospReceipt.testMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
            $aospReceipt.targetMetadataWrapperIdentityCandidateCount -eq 1 -and
            $aospReceipt.testMetadataWrapperIdentityCandidateCount -eq 1 -and
            $aospReceipt.targetMetadataSignatureSchemeCandidateCount -eq 1 -and
            $aospReceipt.testMetadataSignatureSchemeCandidateCount -eq 1 -and
            $aospReceipt.targetMetadataSignatureCandidateCount -eq 1 -and
            $aospReceipt.testMetadataSignatureCandidateCount -eq 1 -and
            $aospReceipt.targetMetadataPastSignatureCandidateCount -eq 0 -and
            $aospReceipt.testMetadataPastSignatureCandidateCount -eq 0 -and
            $aospReceipt.targetSignatureToken -eq 'abebea4b' -and
            $aospReceipt.testSignatureToken -eq 'abebea4b') `
        -ErrorCode 'AOSP_WRAPPER_LAYOUT_NOT_PARSED'
    $aospReceiptText = Get-Content -LiteralPath (Join-Path $run.EvidencePath 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json') -Raw
    Assert-P5ETrue -Condition ($aospReceiptText -notmatch 'PackageSignatures|past signatures|(?m)^\s*versionCode\s*=|(?m)^\s*signatures\s*[:=]') -ErrorCode 'AOSP_RAW_METADATA_LEAKED'
    $cases.aosp_source_derived_current_vs_past_layout_pass = 'PASS'

    $sourceDerivedFixture = @'
Package [com.ml.tblandroidtxt] (source-derived synthetic fixture):
  versionCode=207 minSdk=23
  signatures=PackageSignatures{abc123 version:3, signatures:[abebea4b], past signatures:[cafebabe flags: 1]}
'@
    $baselineSourceRef = 'bf97cd410ea63fe7f6d978b58324584249995e60'
    $oldSource = (& git -C $repoRoot show (('{0}:scripts/p5e-account-check-device-preflight.ps1' -f $baselineSourceRef))) -join "`n"
    Remove-P5EParsedFunction -Name 'Get-P5EPackageMetadataParse'
    Remove-P5EParsedFunction -Name 'New-P5EPackageMetadataState'
    Invoke-Expression (Get-P5EFunctionTextFromSource -Source $oldSource -Name 'New-P5EPackageMetadataState')
    Invoke-Expression (Get-P5EFunctionTextFromSource -Source $oldSource -Name 'Get-P5EPackageMetadataParse')
    $oldParserResult = Get-P5EPackageMetadataParse -Text $sourceDerivedFixture -PackageName 'com.ml.tblandroidtxt'
    Assert-P5ETrue -Condition ($oldParserResult.reason -eq 'UNSUPPORTED_LAYOUT') -ErrorCode 'OLD_PARSER_DID_NOT_REJECT_SOURCE_FIXTURE'
    Remove-P5EParsedFunction -Name 'Get-P5EPackageMetadataParse'
    Remove-P5EParsedFunction -Name 'Get-P5ESignatureListParse'
    Remove-P5EParsedFunction -Name 'New-P5EPackageMetadataState'
    $currentSource = Get-Content -LiteralPath $preflightPath -Raw
    Invoke-Expression (Get-P5EFunctionTextFromSource -Source $currentSource -Name 'New-P5EPackageMetadataState')
    Invoke-Expression (Get-P5EFunctionTextFromSource -Source $currentSource -Name 'Get-P5ESignatureListParse')
    Invoke-Expression (Get-P5EFunctionTextFromSource -Source $currentSource -Name 'Get-P5EPackageMetadataParse')
    $newParserResult = Get-P5EPackageMetadataParse -Text $sourceDerivedFixture -PackageName 'com.ml.tblandroidtxt'
    Assert-P5ETrue -Condition ($newParserResult.reason -eq 'PASS' -and
            $newParserResult.layoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
            $newParserResult.signatureCandidateCount -eq 1 -and
            $newParserResult.pastSignatureCandidateCount -eq 1 -and
            $newParserResult.signatureToken -eq 'abebea4b') -ErrorCode 'NEW_PARSER_DID_NOT_ACCEPT_SOURCE_FIXTURE'
    $cases.source_derived_positive_red_to_green = 'PASS'
    Remove-P5EParsedFunction -Name 'Get-P5EPackageMetadataParse'
    Remove-P5EParsedFunction -Name 'Get-P5ESignatureListParse'
    Remove-P5EParsedFunction -Name 'New-P5EPackageMetadataState'

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'real-dumpsys-spacing') -Mode 'real-dumpsys-spacing'
    Assert-P5ETrue -Condition ($run.ExitCode -eq 0 -and $run.Output -contains 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT=PASS') `
        -ErrorCode 'REAL_DUMPSYS_LAYOUT_PASS_SIGNAL_MISSING'
    $realLayoutReceipt = Get-P5EReceipt -EvidencePath $run.EvidencePath
    Assert-P5ETrue -Condition ($realLayoutReceipt.status -eq 'PASS' -and $realLayoutReceipt.deviceReadAttempts -eq 7 -and
            $realLayoutReceipt.targetVersionCode -eq 207 -and $realLayoutReceipt.targetSignatureToken -eq 'abebea4b' -and
            $realLayoutReceipt.testSignatureToken -eq 'abebea4b' -and
            $realLayoutReceipt.targetMetadataReason -eq 'PASS' -and $realLayoutReceipt.testMetadataReason -eq 'PASS') `
        -ErrorCode 'REAL_DUMPSYS_LAYOUT_NOT_PARSED'
    $cases.real_dumpsys_spacing_layout = 'PASS'

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-package-absent') -Mode 'metadata-package-absent'
    Assert-P5EStopCase -Name 'metadata_package_absent' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_PACKAGE_ABSENT_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition (-not $r.targetMetadataPackagePresent -and $r.targetMetadataReason -eq 'PACKAGE_ABSENT' -and
                $r.targetMetadataVersionCandidateCount -eq 0 -and $r.targetMetadataSignatureCandidateCount -eq 0) -ErrorCode 'PACKAGE_ABSENT_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-empty') -Mode 'metadata-empty'
    Assert-P5EStopCase -Name 'metadata_empty_output' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_EMPTY_OUTPUT_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition (-not $r.targetMetadataPackagePresent -and $r.targetMetadataReason -eq 'EMPTY_OUTPUT') -ErrorCode 'EMPTY_OUTPUT_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-package-duplicate') -Mode 'metadata-package-duplicate'
    Assert-P5EStopCase -Name 'metadata_package_duplicate' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_PACKAGE_SECTION_AMBIGUOUS_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataPackagePresent -and $r.targetMetadataReason -eq 'PACKAGE_SECTION_AMBIGUOUS') -ErrorCode 'PACKAGE_DUPLICATE_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-version-missing') -Mode 'metadata-version-missing'
    Assert-P5EStopCase -Name 'metadata_version_missing' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_VERSION_MISSING_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataPackagePresent -and $r.targetMetadataReason -eq 'VERSION_MISSING' -and
                $r.targetMetadataVersionFieldCount -eq 0) -ErrorCode 'VERSION_MISSING_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-version-duplicate') -Mode 'metadata-version-duplicate'
    Assert-P5EStopCase -Name 'metadata_version_duplicate' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_VERSION_AMBIGUOUS_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataReason -eq 'VERSION_AMBIGUOUS' -and
                $r.targetMetadataVersionCandidateCount -eq 2 -and $r.targetMetadataVersionFieldCount -eq 2) -ErrorCode 'VERSION_DUPLICATE_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-version-invalid') -Mode 'metadata-version-invalid'
    Assert-P5EStopCase -Name 'metadata_version_invalid' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_VERSION_INVALID_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataReason -eq 'VERSION_INVALID' -and
                $r.targetMetadataVersionFieldCount -eq 1 -and $r.targetMetadataVersionCandidateCount -eq 0) -ErrorCode 'VERSION_INVALID_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-signature-missing') -Mode 'metadata-signature-missing'
    Assert-P5EStopCase -Name 'metadata_signature_missing' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_MISSING_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataReason -eq 'SIGNATURE_MISSING' -and
                $r.targetMetadataSignatureFieldCount -eq 0) -ErrorCode 'SIGNATURE_MISSING_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-signature-duplicate') -Mode 'metadata-signature-duplicate'
    Assert-P5EStopCase -Name 'metadata_signature_duplicate' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_AMBIGUOUS_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataReason -eq 'SIGNATURE_AMBIGUOUS' -and
                $r.targetMetadataSignatureCandidateCount -eq 2 -and $r.targetMetadataSignatureFieldCount -eq 2) -ErrorCode 'SIGNATURE_DUPLICATE_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-signature-invalid') -Mode 'metadata-signature-invalid'
    Assert-P5EStopCase -Name 'metadata_signature_invalid' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_INVALID_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataReason -eq 'SIGNATURE_INVALID' -and
                $r.targetMetadataSignatureFieldCount -eq 1 -and $r.targetMetadataSignatureCandidateCount -eq 0) -ErrorCode 'SIGNATURE_INVALID_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-inline-wrapper') -Mode 'metadata-inline-wrapper'
    Assert-P5EStopCase -Name 'metadata_inline_wrapper_unsupported' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_UNSUPPORTED_LAYOUT_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataPackagePresent -and $r.targetMetadataReason -eq 'UNSUPPORTED_LAYOUT') -ErrorCode 'INLINE_WRAPPER_DIAGNOSTICS_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-current-multiple') -Mode 'aosp-current-multiple'
    Assert-P5EStopCase -Name 'aosp_current_multiple_signers' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_AMBIGUOUS_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataSignatureCandidateCount -eq 2 -and $r.targetMetadataPastSignatureCandidateCount -eq 0) -ErrorCode 'AOSP_MULTIPLE_CURRENT_NOT_REJECTED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-current-missing') -Mode 'aosp-current-missing'
    Assert-P5EStopCase -Name 'aosp_current_missing_past_present' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_MISSING_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataSignatureCandidateCount -eq 0 -and $r.targetMetadataPastSignatureCandidateCount -eq 1) -ErrorCode 'AOSP_PAST_SIGNER_SUBSTITUTION' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-identity-pin') -Mode 'aosp-identity-pin'
    Assert-P5EStopCase -Name 'aosp_wrapper_identity_not_signer' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_TARGET_PRODUCTION_PIN_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataWrapperIdentityCandidateCount -eq 1 -and $r.targetSignatureToken -eq '') -ErrorCode 'AOSP_WRAPPER_IDENTITY_USED_AS_SIGNER' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-duplicate-wrapper') -Mode 'aosp-duplicate-wrapper'
    Assert-P5EStopCase -Name 'aosp_duplicate_wrapper' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_AMBIGUOUS_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataSignatureFieldCount -eq 2 -and $r.targetMetadataWrapperIdentityCandidateCount -eq 2) -ErrorCode 'AOSP_DUPLICATE_WRAPPER_NOT_REJECTED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-conflicting-format') -Mode 'aosp-conflicting-format'
    Assert-P5EStopCase -Name 'aosp_legacy_wrapper_conflict' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_UNSUPPORTED_LAYOUT_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'CONFLICTING_SIGNATURE_FORMATS' -and
                $r.targetMetadataSignatureFieldCount -eq 2) -ErrorCode 'AOSP_CONFLICTING_FORMAT_NOT_REJECTED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-truncated') -Mode 'aosp-truncated'
    Assert-P5EStopCase -Name 'aosp_truncated_wrapper' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_UNSUPPORTED_LAYOUT_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataSignatureFieldCount -eq 1 -and $r.targetMetadataWrapperIdentityCandidateCount -eq 0) -ErrorCode 'AOSP_TRUNCATED_WRAPPER_NOT_STOPPED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-nonhex') -Mode 'aosp-nonhex'
    Assert-P5EStopCase -Name 'aosp_current_nonhex' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_INVALID_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataSignatureCandidateCount -eq 0 -and $r.targetMetadataPastSignatureCandidateCount -eq 0) -ErrorCode 'AOSP_NONHEX_NOT_TYPED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'aosp-past-malformed') -Mode 'aosp-past-malformed'
    Assert-P5EStopCase -Name 'aosp_past_malformed' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_SIGNATURE_INVALID_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataLayoutFamily -eq 'AOSP_PACKAGE_SIGNATURES_WRAPPER' -and
                $r.targetMetadataSignatureCandidateCount -eq 1 -and $r.targetMetadataPastSignatureCandidateCount -eq 0) -ErrorCode 'AOSP_PAST_MALFORMED_NOT_TYPED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'metadata-stderr') -Mode 'metadata-stderr'
    Assert-P5EStopCase -Name 'metadata_stdout_valid_stderr_error' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_PACKAGE_METADATA_TOOL_STDERR_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.targetMetadataReason -eq 'TOOL_STDERR' -and
                $r.targetMetadataVersionCandidateCount -eq 1 -and $r.targetMetadataSignatureCandidateCount -eq 1) -ErrorCode 'STDERR_DIAGNOSTICS_INVALID' }
    $stderrReceiptText = Get-Content -LiteralPath (Join-Path $run.EvidencePath 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json') -Raw
    Assert-P5ETrue -Condition ($stderrReceiptText -notmatch 'metadata warning|Package \[|versionCode\s*=|signatures\s*:') -ErrorCode 'STDERR_RAW_OUTPUT_LEAKED'

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'wrong-production-hash') -Mode 'wrong-production-hash'
    Assert-P5EStopCase -Name 'wrong_production_hash' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_PRODUCTION_APK_HASH_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 5) -ErrorCode 'WRONG_PRODUCTION_HASH_READ_COUNT' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'wrong-production-certificate') -Mode 'wrong-production-certificate'
    Assert-P5EStopCase -Name 'wrong_production_certificate' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_PRODUCTION_CERTIFICATE_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 5) -ErrorCode 'WRONG_PRODUCTION_CERT_READ_COUNT' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'wrong-test-hash') -Mode 'wrong-test-hash'
    Assert-P5EStopCase -Name 'wrong_test_hash' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_INSTALLED_TEST_APK_HASH_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 7) -ErrorCode 'WRONG_TEST_HASH_READ_COUNT' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'wrong-version') -Mode 'wrong-version'
    Assert-P5EStopCase -Name 'wrong_production_version' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_TARGET_PRODUCTION_PIN_MISMATCH_STOP' -Run $run

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'wrong-test-signature') -Mode 'wrong-test-signature'
    Assert-P5EStopCase -Name 'wrong_test_signature_pin' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_TEST_PACKAGE_SIGNATURE_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 3 -and
                $r.targetMetadataReason -eq 'PASS' -and $r.testMetadataReason -eq 'PASS' -and
                $r.testMetadataSignatureCandidateCount -eq 1) -ErrorCode 'WRONG_TEST_SIGNATURE_PIN_NOT_PROVEN' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'path-split') -Mode 'path-split'
    Assert-P5EStopCase -Name 'split_apk_path' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_COM_ML_TBLANDROIDTXT_APK_PATH_INVALID_STOP' -Run $run

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'offline') -Mode 'offline'
    Assert-P5EStopCase -Name 'device_offline' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 1 -and $r.lastProcess.outcome -eq 'PROCESS_EXITED_ZERO') -ErrorCode 'OFFLINE_RECEIPT_INVALID' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'tool-nonzero') -Mode 'tool-nonzero'
    Assert-P5EStopCase -Name 'native_nonzero' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 1 -and $r.lastProcess.exitCode -eq 17) -ErrorCode 'NONZERO_EXIT_NOT_PRESERVED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'timeout') -Mode 'timeout' -DeviceTimeout 300 -PullTimeout 500
    Assert-P5EStopCase -Name 'timeout_bounded' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.timeoutObserved -and $r.lastProcess.outcome -eq 'TIMEOUT' -and
                $r.lastProcess.cleanupStatus -ne 'NOT_REQUIRED' -and $r.elapsedMilliseconds -lt 8000) -ErrorCode 'TIMEOUT_NOT_BOUNDED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'large-output') -Mode 'large-output' -CaptureBytes 1024
    Assert-P5EStopCase -Name 'capture_overflow' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_DEVICE_NOT_READY_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.captureFailureObserved -and $r.lastProcess.captureOverflow) -ErrorCode 'CAPTURE_OVERFLOW_NOT_TYPED' }

    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'wrong-reference-certificate') -Mode 'wrong-reference-certificate'
    Assert-P5EStopCase -Name 'reference_certificate_pin' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_REFERENCE_CERTIFICATE_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 0) -ErrorCode 'ADB_LAUNCHED_BEFORE_REFERENCE_CERTIFICATE' }

    $beforePinLogCount = @(Get-Content -LiteralPath $script:fakeLog).Count
    $run = Invoke-P5EPreflightChild -EvidencePath (Join-Path $temporaryRoot 'script-pin') -ExpectedScriptHash ('0' * 64)
    Assert-P5EStopCase -Name 'script_pin' -ExpectedTypedError 'ACCOUNT_CHECK_PREFLIGHT_SCRIPT_HASH_MISMATCH_STOP' -Run $run `
        -ExtraCheck { param($r) Assert-P5ETrue -Condition ($r.deviceReadAttempts -eq 0) -ErrorCode 'TOOL_LAUNCHED_BEFORE_SCRIPT_PIN' }
    Assert-P5ETrue -Condition (@(Get-Content -LiteralPath $script:fakeLog).Count -eq $beforePinLogCount) -ErrorCode 'PIN_DRIFT_SIDE_EFFECT'

    $occupiedEvidence = Join-Path $temporaryRoot 'occupied-evidence'
    New-Item -ItemType Directory -Path $occupiedEvidence -ErrorAction Stop | Out-Null
    Set-Content -LiteralPath (Join-Path $occupiedEvidence 'old.json') -Value '{}' -Encoding ascii
    $run = Invoke-P5EPreflightChild -EvidencePath $occupiedEvidence
    Assert-P5ETrue -Condition ($run.ExitCode -ne 0 -and
            $run.Output -contains 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT=STOP' -and
            ($run.Output -join '|') -match 'ACCOUNT_CHECK_PREFLIGHT_EVIDENCE_DIRECTORY_NOT_EMPTY_STOP') -ErrorCode 'STALE_EVIDENCE_NOT_STOPPED'
    Assert-P5ETrue -Condition (-not (Test-Path -LiteralPath (Join-Path $occupiedEvidence 'ACCOUNT_CHECK_DEVICE_PREFLIGHT_RESULT.json'))) `
        -ErrorCode 'STALE_EVIDENCE_WAS_OVERWRITTEN'
    $cases.stale_evidence_stop_preserves_existing_files = 'PASS'

    $source = Get-Content -LiteralPath $preflightPath -Raw
    Assert-P5ETrue -Condition ($source -notmatch '(?i)ReadToEndAsync|Task\.WaitAll|WaitForExit\(\s*\)') -ErrorCode 'UNBOUNDED_PROCESS_OPERATION_PRESENT'
    Assert-P5ETrue -Condition ($source -match 'EnvironmentVariables\.Remove\(\[string\]\$name\)' -and
            $source -match 'ClearInheritedEnvironmentVariableNames' -and
            $source -notmatch 'GetEnvironmentVariable\(.*P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT') `
        -ErrorCode 'EXPECTED_PROCESS_VALUE_ISOLATION_MISSING'
    Assert-P5ETrue -Condition ($source -match 'ExpectedTargetApkSha256' -and $source -match 'ExpectedTargetCertificateSha256' -and
            $source -match 'TREE_KILLED_CONFIRMED' -and $source -match 'CaptureOverflow' -and
            $source -match 'Get-P5EPackageMetadataParse' -and $source -match 'PACKAGE_SECTION_AMBIGUOUS' -and
            $source -match 'targetMetadataVersionCandidateCount') -ErrorCode 'PRODUCTION_OR_CLEANUP_GATES_MISSING'
    Assert-P5ETrue -Condition ($source -notmatch '(?i)adb\s+install|\buninstall\b|\bpm\s+clear\b|\bam\s+instrument\b|\bprovider\s+call\b|\bsqlite\b') `
        -ErrorCode 'FORBIDDEN_SCOPE_TOKEN_PRESENT'
    $cases.static_source_boundary_and_bounded_ops = 'PASS'

    $result = [ordered]@{
        schemaVersion = 'p5e.account-check.device-preflight.qa.v2'
        status = 'PASS'
        totalAssertions = $assertionCount
        passedAssertions = $assertionCount
        failures = @($failures)
        source = [ordered]@{
            preflightSha256 = Get-P5EFileHash -Path $preflightPath
            qaScriptSha256 = Get-P5EFileHash -Path $MyInvocation.MyCommand.Path
        }
        host = [ordered]@{
            expectedHost = 'Windows PowerShell 5.1'
            actualHost = (& powershell.exe -NoLogo -NoProfile -NonInteractive -Command '$PSVersionTable.PSVersion.ToString()').Trim()
            actualHostProcess = $PSVersionTable.PSVersion.ToString()
        }
        cases = $cases
        syntheticOnly = $true
        adbInvocations = 0
        deviceActions = 0
        providerCalls = 0
        dbWrites = 0
        rawDispatches = 0
    }
    $parentDirectory = Split-Path -Parent $ResultPath
    if (-not (Test-Path -LiteralPath $parentDirectory -PathType Container)) {
        New-Item -ItemType Directory -Path $parentDirectory -ErrorAction Stop | Out-Null
    }
    [IO.File]::WriteAllText($ResultPath, ($result | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA=PASS'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA_ASSERTIONS=' + $assertionCount + '/' + $assertionCount)
} catch {
    $failures.Add([string]$_.Exception.Message)
    Write-Output 'P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA=FAIL'
    Write-Output ('P5E_ACCOUNT_CHECK_DEVICE_PREFLIGHT_QA_ERROR=' + [string]$_.Exception.Message)
    exit 1
} finally {
    if ($hadOriginalExpected) {
        [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $originalExpected, 'Process')
    } else {
        [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $null, 'Process')
    }
    foreach ($name in $envNames | Where-Object { $_ -ne 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT' }) {
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    if (Test-Path -LiteralPath $temporaryRoot) {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
    }
}
