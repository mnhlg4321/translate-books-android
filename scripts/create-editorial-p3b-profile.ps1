[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-f]{40,64}$')]
    [string] $SourceCommit,
    [string] $CreatedAt = '2026-09-03T00:00:00+07:00',
    [string] $OutputPath = 'editorial-engine/src/main/resources/editorial/engine-profile/v2/profile.json',
    [string] $ReceiptPath = 'docs/P3B_PROFILE_CHECKSUM_RECEIPT.json'
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $repoRoot

$commitObject = & git cat-file -e "$SourceCommit^{commit}" 2>$null
if ($LASTEXITCODE -ne 0) { throw "Source commit does not exist: $SourceCommit" }

$outputFullPath = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputPath))
$receiptFullPath = [IO.Path]::GetFullPath((Join-Path $repoRoot $ReceiptPath))
$appMain = [IO.Path]::GetFullPath((Join-Path $repoRoot 'app/src/main'))
if ($outputFullPath.StartsWith($appMain, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'P3B profile output may not be written under app/src/main.'
}
if (Test-Path -LiteralPath $outputFullPath) { throw "Refusing to overwrite existing profile: $outputFullPath" }
if (Test-Path -LiteralPath $receiptFullPath) { throw "Refusing to overwrite existing receipt: $receiptFullPath" }

$javaHomePath = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$env:JAVA_HOME = $javaHomePath
$javaExe = Join-Path $javaHomePath 'bin/java.exe'
$javacExe = Join-Path $javaHomePath 'bin/javac.exe'
if (!(Test-Path -LiteralPath $javaExe) -or !(Test-Path -LiteralPath $javacExe)) {
    throw "JDK 17 is required at JAVA_HOME or the Android Studio bundled JBR: $javaHomePath"
}

& .\gradlew.bat :editorial-engine:classes --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'editorial-engine classes failed' }

$classes = Join-Path $repoRoot 'editorial-engine/build/classes/java/main'
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ('editorial-p3b-generator-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempRoot | Out-Null
try {
    & $javacExe -encoding UTF-8 -cp $classes -d $tempRoot (Join-Path $PSScriptRoot 'EditorialSafe4ProfileGenerator.java')
    if ($LASTEXITCODE -ne 0) { throw 'P3B generator bridge compilation failed' }
    $profileJson = & $javaExe -cp "$tempRoot;$classes" EditorialSafe4ProfileGenerator $SourceCommit $CreatedAt
    if ($LASTEXITCODE -ne 0) { throw 'P3B profile generation failed' }
    if ([string]::IsNullOrWhiteSpace($profileJson) -or $profileJson -match "`r|`n") {
        throw 'Generated profile must be one canonical JSON line without a newline suffix.'
    }

    $outputDirectory = Split-Path -Parent $outputFullPath
    if (!(Test-Path -LiteralPath $outputDirectory)) { New-Item -ItemType Directory -Path $outputDirectory | Out-Null }
    $profileBytes = [Text.Encoding]::UTF8.GetBytes($profileJson)
    [IO.File]::WriteAllBytes($outputFullPath, $profileBytes)
    $profileSha = [BitConverter]::ToString(([Security.Cryptography.SHA256]::Create().ComputeHash($profileBytes))).Replace('-', '').ToLowerInvariant()
    $profile = $profileJson | ConvertFrom-Json

    $receiptDirectory = Split-Path -Parent $receiptFullPath
    if (!(Test-Path -LiteralPath $receiptDirectory)) { New-Item -ItemType Directory -Path $receiptDirectory | Out-Null }
    $receipt = [ordered]@{
        artifact = 'editorial-engine trusted profile'
        profileId = $profile.engineProfileId
        profileVersion = $profile.engineProfileVersion
        sourceCommit = $SourceCommit
        createdAt = $CreatedAt
        resourcePath = $OutputPath.Replace('\', '/')
        resourceSha256 = $profileSha
        canonicalProfileHash = $profile.canonicalProfileHash
        machineContractFingerprint = $profile.machineContractFingerprint
        implementedCapabilities = @($profile.implementedCapabilities)
        explicitlyMissingCapabilities = @($profile.explicitlyMissingCapabilities)
    }
    $receiptJson = $receipt | ConvertTo-Json -Depth 8 -Compress
    [IO.File]::WriteAllBytes($receiptFullPath, [Text.Encoding]::UTF8.GetBytes($receiptJson))
    Write-Output ("PROFILE_ID=" + $profile.engineProfileId)
    Write-Output ("PROFILE_VERSION=" + $profile.engineProfileVersion)
    Write-Output ("PROFILE_SHA256=" + $profileSha)
    Write-Output ("CANONICAL_PROFILE_HASH=" + $profile.canonicalProfileHash)
    Write-Output ("MACHINE_CONTRACT_FINGERPRINT=" + $profile.machineContractFingerprint)
}
finally {
    if (Test-Path -LiteralPath $tempRoot) { Remove-Item -LiteralPath $tempRoot -Recurse -Force }
}
