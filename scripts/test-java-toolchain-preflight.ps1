[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$preflightScript = Join-Path $PSScriptRoot 'verify-java-toolchain.ps1'
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:Path
$testTemp = Join-Path ([IO.Path]::GetTempPath()) "translate-books-jdk-preflight-$([guid]::NewGuid().ToString('N'))"
$passed = 0

function Assert-True {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$Message
    )

    if (-not $Condition) {
        throw "Preflight test failed: $Message"
    }
}

function Invoke-ExpectedFailure {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Action,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Pattern
    )

    $failed = $false
    $message = ''
    try {
        & $Action
    }
    catch {
        $failed = $true
        $message = $_.Exception.Message
    }

    Assert-True $failed "$Name did not fail."
    Assert-True ($message -match $Pattern) "$Name returned an unexpected error message."
    return $message
}

try {
    New-Item -ItemType Directory -Path $testTemp | Out-Null

    $jdk21Candidates = @(
        $env:JAVA_HOME,
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'),
        (Join-Path ${env:ProgramFiles(x86)} 'Android\Android Studio\jbr')
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
    $jdk21 = $jdk21Candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe') -PathType Leaf } | Select-Object -First 1
    Assert-True (-not [string]::IsNullOrWhiteSpace($jdk21)) 'a JDK 21 installation is required for the positive test.'

    $env:JAVA_HOME = ''
    $env:Path = $originalPath
    $positive = (& $preflightScript -JavaHome $jdk21 -AsJson | ConvertFrom-Json)
    Assert-True ($positive.jdkMajor -eq 21) 'JDK 21 was not accepted.'
    Assert-True ($positive.javaHomeSource -eq 'parameter') 'the explicit JDK parameter was not recorded.'
    $passed++
    Write-Output 'PASS: JDK 21 accepted through -JavaHome.'

    $env:JAVA_HOME = ''
    $env:Path = $originalPath
    $java8Message = Invoke-ExpectedFailure -Name 'Java 8 rejection' -Pattern 'major 8' -Action {
        & $preflightScript -AsJson | Out-Null
    }
    Assert-True ($java8Message -notmatch '[A-Za-z]:\\Users\\') 'Java 8 error exposed a personal user path.'
    $passed++
    Write-Output 'PASS: Java 8 rejected with a clear major-version error.'

    $env:JAVA_HOME = ''
    $env:Path = $testTemp
    $missingMessage = Invoke-ExpectedFailure -Name 'missing JAVA_HOME' -Pattern 'JAVA_HOME|JavaHome' -Action {
        & $preflightScript -AsJson | Out-Null
    }
    Assert-True ($missingMessage -notmatch [regex]::Escape($testTemp)) 'missing-JAVA_HOME error exposed a temporary path.'
    $passed++
    Write-Output 'PASS: missing JAVA_HOME rejected without selecting a runtime implicitly.'

    $env:JAVA_HOME = ''
    $env:Path = $originalPath
    $invalidHome = Join-Path $testTemp 'not-a-jdk'
    $invalidMessage = Invoke-ExpectedFailure -Name 'invalid Java path' -Pattern 'valid JDK|bin\\java\.exe' -Action {
        & $preflightScript -JavaHome $invalidHome -AsJson | Out-Null
    }
    Assert-True ($invalidMessage -notmatch [regex]::Escape($invalidHome)) 'invalid-path error exposed the supplied path.'
    $passed++
    Write-Output 'PASS: invalid JDK path rejected without exposing the path.'

    Write-Output "Preflight tests passed: $passed/4"
}
finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:Path = $originalPath
    if (Test-Path -LiteralPath $testTemp) {
        Remove-Item -LiteralPath $testTemp -Recurse -Force
    }
}
