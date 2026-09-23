[CmdletBinding()]
param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\docs\P5E_EXPECTED_VALUE_LOADER_QA_20260923.json')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$loaderPath = Join-Path $repoRoot 'scripts\p5e-load-expected-digest.ps1'
$results = [System.Collections.Generic.List[object]]::new()
$testEnvironmentName = 'P5E_EXPECTED_LOADER_QA_' + [Guid]::NewGuid().ToString('N')
$module = $null
$failure = $null

function Assert-P5ELoaderQa {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Condition
    )
    [void]$script:results.Add([ordered]@{ name = $Name; passed = $Condition })
    if (-not $Condition) { throw ('P5E_EXPECTED_LOADER_QA_ASSERTION_FAILED:' + $Name) }
}

function New-P5EFakeSecureString {
    param([Parameter(Mandatory = $true)][string]$Text)

    $secure = [Security.SecureString]::new()
    foreach ($character in $Text.ToCharArray()) { $secure.AppendChar($character) }
    $secure.MakeReadOnly()
    return $secure
}

function Get-P5EFakeFingerprint {
    param(
        [Parameter(Mandatory = $true)][string]$Endpoint,
        [Parameter(Mandatory = $true)][string]$Key
    )

    $secure = New-P5EFakeSecureString -Text $Key
    try {
        return Get-P5EEndpointAccountFingerprintFromSecureKey -Endpoint $Endpoint -ApiKey $secure
    } finally {
        $secure.Dispose()
    }
}

function New-P5ELoaderLibraryModule {
    param([Parameter(Mandatory = $true)][string]$Path)

    $newModule = New-Module -Name ('P5EExpectedLoaderQA_' + [Guid]::NewGuid().ToString('N')) -ScriptBlock {
        param([Parameter(Mandatory = $true)][string]$LoaderFile)
        . $LoaderFile -LibraryOnly
        Export-ModuleMember -Function @(
            'Normalize-P5EEndpointForFingerprint',
            'Get-P5EEndpointAccountFingerprintFromSecureKey',
            'Set-P5EProcessExpectedFingerprint',
            'Clear-P5EProcessExpectedFingerprint'
        )
    } -ArgumentList $Path
    Import-Module $newModule -Force | Out-Null
    return $newModule
}

try {
    if (-not (Test-Path -LiteralPath $loaderPath -PathType Leaf)) {
        throw 'P5E_EXPECTED_LOADER_MISSING_STOP'
    }
    $module = New-P5ELoaderLibraryModule -Path $loaderPath
    $loaderSource = [IO.File]::ReadAllText($loaderPath)

    Assert-P5ELoaderQa 'library-only-import-does-not-prompt-or-load' $true
    Assert-P5ELoaderQa 'fixed-current-p5e-endpoint' ($loaderSource.Contains("`$script:P5ECurrentEndpoint = 'https://openrouter.ai/api/v1/chat/completions'"))
    Assert-P5ELoaderQa 'secure-prompt-required' ($loaderSource.Contains('Read-Host -Prompt ''Paste original OpenRouter API key (hidden)'' -AsSecureString'))
    Assert-P5ELoaderQa 'process-scope-only-write' ($loaderSource.Contains("SetEnvironmentVariable(`$EnvironmentName, `$Fingerprint, 'Process')"))
    Assert-P5ELoaderQa 'no-user-or-machine-environment-access' ((-not $loaderSource.Contains("'User'")) -and (-not $loaderSource.Contains("'Machine'")))
    Assert-P5ELoaderQa 'no-child-or-network-launch' ((-not $loaderSource.Contains('Start-Process')) -and (-not $loaderSource.Contains('Invoke-WebRequest')) -and (-not $loaderSource.Contains('Invoke-RestMethod')))
    Assert-P5ELoaderQa 'no-clipboard-or-transcript-write' ((-not $loaderSource.Contains('Set-Clipboard')) -and (-not $loaderSource.Contains('Start-Transcript')) -and (-not $loaderSource.Contains('Tee-Object')) -and (-not $loaderSource.Contains('Out-File')))
    Assert-P5ELoaderQa 'typed-output-does-not-contain-fingerprint-variable' (-not [regex]::IsMatch($loaderSource, '(?im)^\s*Write-(Output|Host|Information|Verbose|Error)\b[^\r\n]*\$fingerprint'))
    Assert-P5ELoaderQa 'no-api-key-command-line-parameter' ((-not $loaderSource.Contains('$ApiKey,')) -and (-not $loaderSource.Contains('[string]$ApiKey')))

    $standardEndpoint = 'https://openrouter.ai/api/v1/chat/completions'
    $fakeKey = 'fake-key-001'
    $knownVectors = @(
        [ordered]@{ name = 'v1-appends-chat-completions'; endpoint = 'https://openrouter.ai/api/v1'; key = $fakeKey; expected = 'ec835bcb518f4a409ceb35ad48c757c2f503110faee648900aead962622b76a9' },
        [ordered]@{ name = 'trimmed-v1-slash-matches-current-endpoint'; endpoint = ' https://openrouter.ai/api/v1/ '; key = $fakeKey; expected = 'ec835bcb518f4a409ceb35ad48c757c2f503110faee648900aead962622b76a9' },
        [ordered]@{ name = 'current-endpoint-is-stable'; endpoint = $standardEndpoint; key = $fakeKey; expected = 'ec835bcb518f4a409ceb35ad48c757c2f503110faee648900aead962622b76a9' },
        [ordered]@{ name = 'only-one-trailing-slash-is-removed'; endpoint = 'https://openrouter.ai/api/v1//'; key = $fakeKey; expected = '36b4445e322dd14e2db7a23fe22bf804baad59a11b2d5b57a74001d8780ef7db' },
        [ordered]@{ name = 'key-trailing-space-is-not-trimmed'; endpoint = $standardEndpoint; key = ($fakeKey + ' '); expected = '9b529715d7e2cc2e03bd566d08c597202bc486631fae9fa7f0cc5253e5fac2d0' }
    )
    foreach ($vector in $knownVectors) {
        $actual = Get-P5EFakeFingerprint -Endpoint ([string]$vector.endpoint) -Key ([string]$vector.key)
        Assert-P5ELoaderQa ('known-vector-' + [string]$vector.name) ($actual -ceq [string]$vector.expected)
    }

    Assert-P5ELoaderQa 'java-trim-removes-ascii-space-only' ((Normalize-P5EEndpointForFingerprint -Raw ' https://openrouter.ai/api/v1 ') -ceq $standardEndpoint)
    $nbsp = [string][char]0x00A0
    $nbspEndpoint = $nbsp + 'https://openrouter.ai/api/v1' + $nbsp
    Assert-P5ELoaderQa 'java-trim-preserves-nonbreaking-space' ((Normalize-P5EEndpointForFingerprint -Raw $nbspEndpoint) -ceq $nbspEndpoint)
    Assert-P5ELoaderQa 'api-generate-is-not-rewritten' ((Normalize-P5EEndpointForFingerprint -Raw 'https://example.invalid/api/generate') -ceq 'https://example.invalid/api/generate')

    $fingerprintForProcessTest = 'b' * 64
    Set-P5EProcessExpectedFingerprint -Fingerprint $fingerprintForProcessTest -EnvironmentName $testEnvironmentName
    $loaded = [Environment]::GetEnvironmentVariable($testEnvironmentName, 'Process')
    Assert-P5ELoaderQa 'process-write-is-lowercase-sha256-shape' ($loaded -ceq $fingerprintForProcessTest -and $loaded -match '^[0-9a-f]{64}$')
    Clear-P5EProcessExpectedFingerprint -EnvironmentName $testEnvironmentName
    Assert-P5ELoaderQa 'process-clear-removes-test-value' ($null -eq [Environment]::GetEnvironmentVariable($testEnvironmentName, 'Process'))
} catch {
    $failure = 'P5E_EXPECTED_LOADER_QA_FAILED'
} finally {
    $testEnvironmentPath = 'Env:' + $testEnvironmentName
    if (Test-Path -LiteralPath $testEnvironmentPath) {
        Remove-Item -LiteralPath $testEnvironmentPath -Force
    }
    if ($null -ne $module) {
        Remove-Module -ModuleInfo $module -Force -ErrorAction SilentlyContinue
    }

    $outputFile = [IO.Path]::GetFullPath($OutputPath)
    $outputDirectory = Split-Path -Parent $outputFile
    if (-not (Test-Path -LiteralPath $outputDirectory -PathType Container)) {
        [void](New-Item -ItemType Directory -Path $outputDirectory -Force)
    }
    $passed = ($null -eq $failure)
    $report = [ordered]@{
        schemaVersion = 1
        scope = 'offline fake-input loader contract only'
        status = if ($passed) { 'PASS' } else { 'FAIL' }
        assertionsTotal = $results.Count
        assertionsPassed = @($results | Where-Object { $_.passed }).Count
        assertions = @($results)
        loaderSha256 = (Get-FileHash -LiteralPath $loaderPath -Algorithm SHA256).Hash.ToUpperInvariant()
        qaScriptSha256 = (Get-FileHash -LiteralPath $MyInvocation.MyCommand.Path -Algorithm SHA256).Hash.ToUpperInvariant()
        credentialInputs = 0
        expectedValueLoads = 0
        adbInvocations = 0
        deviceActions = 0
        providerCalls = 0
        databaseActions = 0
        rawDispatches = 0
        note = 'All digest vectors use synthetic values only; the interactive loader was not invoked.'
    }
    [IO.File]::WriteAllText($outputFile, ($report | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
}

if ($null -ne $failure) { throw $failure }
Write-Output 'P5E_EXPECTED_LOADER_QA=PASS'
