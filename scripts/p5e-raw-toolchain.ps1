[CmdletBinding()]
param(
    [switch]$LibraryOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:P5ERawToolchainContractVersion = 'p5e.raw.toolchain.v1'

function Get-P5ERawToolchainTypedStop {
    param([Parameter(Mandatory = $true)][string]$Code)
    throw $Code
}

function Test-P5ERawReparseAncestor {
    param([Parameter(Mandatory = $true)][string]$Path)
    $current = Get-Item -LiteralPath $Path -Force -ErrorAction Stop
    while ($null -ne $current) {
        if (($current.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            return $true
        }
        $parent = if ($current -is [IO.DirectoryInfo]) { $current.Parent } else { $current.Directory }
        if ($null -eq $parent) { break }
        if ($parent.FullName -ceq $current.FullName) { break }
        $current = $parent
    }
    return $false
}

function Get-P5ERawCanonicalRegularFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$MissingCode,
        [Parameter(Mandatory = $true)][string]$ReparseCode
    )
    $literal = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literal -PathType Leaf)) {
        Get-P5ERawToolchainTypedStop -Code $MissingCode
    }
    $item = Get-Item -LiteralPath $literal -Force -ErrorAction Stop
    if ($item.PSIsContainer -or (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) -or
            (Test-P5ERawReparseAncestor -Path $literal)) {
        Get-P5ERawToolchainTypedStop -Code $ReparseCode
    }
    $resolved = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literal -ErrorAction Stop).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literal, $resolved)) {
        Get-P5ERawToolchainTypedStop -Code $ReparseCode
    }
    return [pscustomobject]@{
        Path = $literal
        Sha256 = (Get-FileHash -LiteralPath $literal -Algorithm SHA256).Hash.ToLowerInvariant()
        Length = [long]$item.Length
    }
}

function Get-P5ERawCanonicalDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$MissingCode,
        [Parameter(Mandatory = $true)][string]$ReparseCode
    )
    $literal = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $literal -PathType Container)) {
        Get-P5ERawToolchainTypedStop -Code $MissingCode
    }
    $item = Get-Item -LiteralPath $literal -Force -ErrorAction Stop
    if (-not $item.PSIsContainer -or (Test-P5ERawReparseAncestor -Path $literal)) {
        Get-P5ERawToolchainTypedStop -Code $ReparseCode
    }
    $resolved = [IO.Path]::GetFullPath((Resolve-Path -LiteralPath $literal -ErrorAction Stop).Path)
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literal, $resolved)) {
        Get-P5ERawToolchainTypedStop -Code $ReparseCode
    }
    return $literal.TrimEnd('\')
}

function ConvertFrom-P5EJavaPropertiesPath {
    param([Parameter(Mandatory = $true)][string]$Value)
    # local.properties stores Windows drive paths with Java-properties escapes.
    return (($Value -replace '\\:', ':') -replace '\\\\', '\')
}

function Resolve-P5ERawSdkPath {
    param(
        [string]$AndroidSdkPath = '',
        [string]$LocalPropertiesPath = ''
    )
    if (-not [string]::IsNullOrWhiteSpace($AndroidSdkPath)) {
        return Get-P5ERawCanonicalDirectory -Path $AndroidSdkPath `
            -MissingCode 'P5E_RAW_TOOLCHAIN_SDK_MISSING_STOP' `
            -ReparseCode 'P5E_RAW_TOOLCHAIN_SDK_REPARSE_STOP'
    }
    if ([string]::IsNullOrWhiteSpace($LocalPropertiesPath)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_SDK_CONFIGURATION_MISSING_STOP'
    }
    $properties = Get-P5ERawCanonicalRegularFile -Path $LocalPropertiesPath `
        -MissingCode 'P5E_RAW_TOOLCHAIN_LOCAL_PROPERTIES_MISSING_STOP' `
        -ReparseCode 'P5E_RAW_TOOLCHAIN_LOCAL_PROPERTIES_REPARSE_STOP'
    $matches = @(Get-Content -LiteralPath $properties.Path -ErrorAction Stop |
        Where-Object { $_ -match '^\s*sdk\.dir\s*=' })
    if ($matches.Count -eq 0) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_SDK_CONFIGURATION_MISSING_STOP'
    }
    if ($matches.Count -ne 1) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_SDK_CONFIGURATION_AMBIGUOUS_STOP'
    }
    $value = [regex]::Replace([string]$matches[0], '^\s*sdk\.dir\s*=\s*', '')
    if ([string]::IsNullOrWhiteSpace($value)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_SDK_CONFIGURATION_MISSING_STOP'
    }
    $decoded = ConvertFrom-P5EJavaPropertiesPath -Value $value
    return Get-P5ERawCanonicalDirectory -Path $decoded `
        -MissingCode 'P5E_RAW_TOOLCHAIN_SDK_MISSING_STOP' `
        -ReparseCode 'P5E_RAW_TOOLCHAIN_SDK_REPARSE_STOP'
}

function Resolve-P5ERawToolchain {
    [CmdletBinding()]
    param(
        [string]$AndroidSdkPath = '',
        [string]$LocalPropertiesPath = '',
        [string]$AdbPath = '',
        [string]$JavaPath = '',
        [string]$ApkSignerJarPath = '',
        [string]$BuildToolsVersion = ''
    )
    if ([string]::IsNullOrWhiteSpace($BuildToolsVersion)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_VERSION_REQUIRED_STOP'
    }
    if ($BuildToolsVersion -notmatch '^\d+\.\d+\.\d+$') {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_VERSION_INVALID_STOP'
    }
    $sdk = Resolve-P5ERawSdkPath -AndroidSdkPath $AndroidSdkPath -LocalPropertiesPath $LocalPropertiesPath
    $expectedAdbPath = [IO.Path]::GetFullPath((Join-Path $sdk 'platform-tools\adb.exe'))
    $adbCandidate = if ([string]::IsNullOrWhiteSpace($AdbPath)) {
        $expectedAdbPath
    } else { $AdbPath }
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetFullPath($adbCandidate), $expectedAdbPath) -and
            -not (Test-Path -LiteralPath $adbCandidate -PathType Leaf)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_ADB_MISSING_STOP'
    }
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetFullPath($adbCandidate), $expectedAdbPath)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_ADB_PATH_NOT_PINNED_STOP'
    }
    $adb = Get-P5ERawCanonicalRegularFile -Path $adbCandidate `
        -MissingCode 'P5E_RAW_TOOLCHAIN_ADB_MISSING_STOP' `
        -ReparseCode 'P5E_RAW_TOOLCHAIN_ADB_REPARSE_STOP'
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetExtension($adb.Path), '.exe')) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_ADB_EXTENSION_STOP'
    }

    $buildTools = Get-P5ERawCanonicalDirectory -Path (Join-Path $sdk ('build-tools\' + $BuildToolsVersion)) `
        -MissingCode 'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_MISSING_STOP' `
        -ReparseCode 'P5E_RAW_TOOLCHAIN_BUILD_TOOLS_REPARSE_STOP'
    $expectedJarPath = [IO.Path]::GetFullPath((Join-Path $buildTools 'lib\apksigner.jar'))
    $jarCandidate = if ([string]::IsNullOrWhiteSpace($ApkSignerJarPath)) {
        $expectedJarPath
    } else { $ApkSignerJarPath }
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetFullPath($jarCandidate), $expectedJarPath) -and
            -not (Test-Path -LiteralPath $jarCandidate -PathType Leaf)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_MISSING_STOP'
    }
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetFullPath($jarCandidate), $expectedJarPath)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_PATH_NOT_PINNED_STOP'
    }
    $jar = Get-P5ERawCanonicalRegularFile -Path $jarCandidate `
        -MissingCode 'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_MISSING_STOP' `
        -ReparseCode 'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_REPARSE_STOP'
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetFileName($jar.Path), 'apksigner.jar')) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_APKSIGNER_JAR_NAME_STOP'
    }
    if ([string]::IsNullOrWhiteSpace($JavaPath)) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_JAVA_PATH_REQUIRED_STOP'
    }
    $java = Get-P5ERawCanonicalRegularFile -Path $JavaPath `
        -MissingCode 'P5E_RAW_TOOLCHAIN_JAVA_MISSING_STOP' `
        -ReparseCode 'P5E_RAW_TOOLCHAIN_JAVA_REPARSE_STOP'
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals([IO.Path]::GetFileName($java.Path), 'java.exe')) {
        Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_JAVA_NAME_STOP'
    }

    return [ordered]@{
        contractVersion = $script:P5ERawToolchainContractVersion
        sdkPath = $sdk
        buildToolsVersion = $BuildToolsVersion
        signerLaunchKind = 'JAVA_JAR'
        adbPath = $adb.Path
        adbSha256 = $adb.Sha256
        javaPath = $java.Path
        javaSha256 = $java.Sha256
        apksignerJarPath = $jar.Path
        apksignerJarSha256 = $jar.Sha256
    }
}

if (-not $LibraryOnly) {
    Get-P5ERawToolchainTypedStop -Code 'P5E_RAW_TOOLCHAIN_LIBRARY_ONLY_REQUIRED_STOP'
}
