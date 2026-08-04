[CmdletBinding()]
param(
    [string]$JavaHome,

    [ValidateRange(1, 99)]
    [int]$RequiredMajor = 21,

    [switch]$AsJson
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Get-PropertyValue {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $match = [regex]::Match($Text, "(?m)^\s*$([regex]::Escape($Name))\s*=\s*(.+?)\s*$")
    if ($match.Success) {
        return $match.Groups[1].Value.Trim()
    }

    return ''
}

function Get-JavaDetails {
    param(
        [Parameter(Mandatory = $true)][string]$Executable
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = @(& $Executable '-XshowSettings:properties' '-version' 2>&1)
        $exitCode = $LASTEXITCODE
    }
    catch {
        throw 'JDK preflight failed: the selected Java runtime could not be executed.'
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($exitCode -ne 0) {
        throw 'JDK preflight failed: the selected Java runtime could not be executed.'
    }

    $text = ($output | ForEach-Object { [string]$_ }) -join "`n"
    $javaVersion = Get-PropertyValue -Text $text -Name 'java.version'
    if ([string]::IsNullOrWhiteSpace($javaVersion)) {
        $versionMatch = [regex]::Match($text, 'version\s+"([^"]+)"')
        if ($versionMatch.Success) {
            $javaVersion = $versionMatch.Groups[1].Value
        }
    }

    if ([string]::IsNullOrWhiteSpace($javaVersion)) {
        throw 'JDK preflight failed: java.version was not reported by the selected runtime.'
    }

    $versionParts = [regex]::Match($javaVersion, '^(\d+)(?:\.(\d+))?')
    if (-not $versionParts.Success) {
        throw 'JDK preflight failed: the Java version format is not recognized.'
    }

    $jdkMajor = [int]$versionParts.Groups[1].Value
    if ($jdkMajor -eq 1 -and $versionParts.Groups[2].Success) {
        $jdkMajor = [int]$versionParts.Groups[2].Value
    }

    $runtimeVersion = Get-PropertyValue -Text $text -Name 'java.runtime.version'
    $runtimeName = Get-PropertyValue -Text $text -Name 'java.runtime.name'
    $vendor = Get-PropertyValue -Text $text -Name 'java.vendor'

    [pscustomobject]@{
        JavaVersion = $javaVersion
        JavaRuntimeVersion = if ([string]::IsNullOrWhiteSpace($runtimeVersion)) { $javaVersion } else { $runtimeVersion }
        JavaRuntimeName = if ([string]::IsNullOrWhiteSpace($runtimeName)) { 'Unknown runtime' } else { $runtimeName }
        JavaVendor = if ([string]::IsNullOrWhiteSpace($vendor)) { 'Unknown vendor' } else { $vendor }
        JdkMajor = $jdkMajor
    }
}

$requestedJavaHome = if ([string]::IsNullOrWhiteSpace($JavaHome)) { '' } else { $JavaHome.Trim() }
$environmentJavaHome = if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { '' } else { $env:JAVA_HOME.Trim() }
$selectedJavaHome = ''
$javaExecutable = ''
$source = ''

if (-not [string]::IsNullOrWhiteSpace($requestedJavaHome)) {
    $selectedJavaHome = $requestedJavaHome
    $source = 'parameter'
}
elseif (-not [string]::IsNullOrWhiteSpace($environmentJavaHome)) {
    $selectedJavaHome = $environmentJavaHome
    $source = 'JAVA_HOME'
}
else {
    $pathJava = Get-Command java -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pathJava) {
        $pathDetails = Get-JavaDetails -Executable $pathJava.Source
        if ($pathDetails.JdkMajor -ne $RequiredMajor) {
            throw "JDK preflight failed: Java runtime major $($pathDetails.JdkMajor) was found on PATH; required JDK major $RequiredMajor. Set JAVA_HOME or pass -JavaHome to a JDK $RequiredMajor installation."
        }

        throw "JDK preflight failed: JAVA_HOME is not set. A JDK $RequiredMajor was found on PATH, but it will not be selected implicitly. Set JAVA_HOME or pass -JavaHome to the intended JDK."
    }

    throw "JDK preflight failed: JAVA_HOME is not set and no Java runtime was found on PATH. Set JAVA_HOME or pass -JavaHome to a JDK $RequiredMajor installation."
}

if (-not (Test-Path -LiteralPath $selectedJavaHome -PathType Container)) {
    throw 'JDK preflight failed: the selected JAVA_HOME is not a valid JDK directory.'
}

$javaExecutable = Join-Path $selectedJavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable -PathType Leaf)) {
    throw 'JDK preflight failed: the selected JAVA_HOME does not contain bin\java.exe.'
}

$details = Get-JavaDetails -Executable $javaExecutable
if ($details.JdkMajor -ne $RequiredMajor) {
    throw "JDK preflight failed: Java runtime major $($details.JdkMajor) was selected; required JDK major $RequiredMajor. Set JAVA_HOME or pass -JavaHome to a JDK $RequiredMajor installation."
}

$result = [ordered]@{
    javaVersion = $details.JavaVersion
    javaRuntimeVersion = $details.JavaRuntimeVersion
    javaRuntimeName = $details.JavaRuntimeName
    javaVendor = $details.JavaVendor
    jdkMajor = $details.JdkMajor
    jdkMajorPolicy = "JDK major $RequiredMajor required for build runtime"
    javaHomeSource = $source
}

if ($AsJson) {
    Write-Output ($result | ConvertTo-Json -Compress)
}
else {
    Write-Output 'JDK preflight: PASS'
    Write-Output "Java version: $($result.javaVersion)"
    Write-Output "Java runtime: $($result.javaRuntimeName)"
    Write-Output "Java vendor: $($result.javaVendor)"
    Write-Output "JDK major policy: $($result.jdkMajorPolicy)"
    Write-Output "JDK selection source: $($result.javaHomeSource)"
}
