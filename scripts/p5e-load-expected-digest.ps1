[CmdletBinding(DefaultParameterSetName = 'Load')]
param(
    [Parameter(Mandatory = $true, ParameterSetName = 'Library')]
    [switch]$LibraryOnly,

    [Parameter(Mandatory = $true, ParameterSetName = 'Clear')]
    [switch]$Clear
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# This is the only normalized endpoint accepted by the current P5E account-only
# Android test. The script deliberately accepts no endpoint or API-key argument.
$script:P5ECurrentEndpoint = 'https://openrouter.ai/api/v1/chat/completions'
$script:P5EAccountEnvironmentName = 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'

function ConvertTo-P5EJavaTrim {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) { return $null }
    $start = 0
    $end = $Value.Length
    while ($start -lt $end -and [int][char]$Value[$start] -le 0x20) { $start++ }
    while ($end -gt $start -and [int][char]$Value[$end - 1] -le 0x20) { $end-- }
    return $Value.Substring($start, $end - $start)
}

function Normalize-P5EEndpointForFingerprint {
    param([AllowNull()][string]$Raw)

    # Mirrors AppSettings.normalizeEndpoint exactly for Java String values.
    if ($null -eq $Raw) { return $null }
    $trimmed = ConvertTo-P5EJavaTrim -Value $Raw
    if ($trimmed.Length -eq 0) { return $Raw }

    $url = $trimmed
    if ($url.EndsWith('/', [StringComparison]::Ordinal)) {
        $url = $url.Substring(0, $url.Length - 1)
    }
    if (-not $url.EndsWith('/chat/completions', [StringComparison]::Ordinal)) {
        if ($url.EndsWith('/v1', [StringComparison]::Ordinal)) {
            $url = $url + '/chat/completions'
        } elseif ($url.IndexOf('/api/generate', [StringComparison]::Ordinal) -ge 0) {
            # Retained solely to mirror the production normalizer.
            $url = $url
        }
    }
    return $url
}

function ConvertTo-P5ELowerHex {
    param([Parameter(Mandatory = $true)][byte[]]$Bytes)

    $out = [Text.StringBuilder]::new($Bytes.Length * 2)
    foreach ($byte in $Bytes) {
        [void]$out.Append($byte.ToString('x2', [Globalization.CultureInfo]::InvariantCulture))
    }
    return $out.ToString()
}

function Get-P5EJavaUtf8Bytes {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)

    # Java StandardCharsets.UTF_8 replaces a malformed UTF-16 input unit with
    # ASCII '?'. Configure the .NET encoder explicitly rather than relying on
    # its default U+FFFD replacement behavior.
    $encoding = [Text.Encoding]::GetEncoding(
        65001,
        [Text.EncoderReplacementFallback]::new('?'),
        [Text.DecoderReplacementFallback]::new([string][char]0xFFFD))
    return $encoding.GetBytes($Value)
}

function Get-P5EEndpointAccountFingerprintFromSecureKey {
    param(
        [Parameter(Mandatory = $true)][AllowEmptyString()][string]$Endpoint,
        [Parameter(Mandatory = $true)][Security.SecureString]$ApiKey
    )

    $bstr = [IntPtr]::Zero
    $payloadBytes = $null
    $digestBytes = $null
    $sha256 = $null
    try {
        $normalizedEndpoint = Normalize-P5EEndpointForFingerprint -Raw $Endpoint
        if ([string]::IsNullOrEmpty($normalizedEndpoint)) {
            throw 'P5E_EXPECTED_ENDPOINT_MISSING_STOP'
        }
        if ($ApiKey.Length -eq 0) {
            throw 'P5E_EXPECTED_KEY_MISSING_STOP'
        }

        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($ApiKey)
        $keyText = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        if ([string]::IsNullOrEmpty($keyText) -or (ConvertTo-P5EJavaTrim -Value $keyText).Length -eq 0) {
            throw 'P5E_EXPECTED_KEY_MISSING_STOP'
        }

        # Java source: SHA-256(UTF-8(normalizeEndpoint(baseUrl) + LF + apiKey)).
        # $keyText is intentionally not trimmed.
        $payloadBytes = Get-P5EJavaUtf8Bytes -Value ($normalizedEndpoint + "`n" + $keyText)
        $sha256 = [Security.Cryptography.SHA256]::Create()
        $digestBytes = $sha256.ComputeHash($payloadBytes)
        return (ConvertTo-P5ELowerHex -Bytes $digestBytes)
    } finally {
        if ($null -ne $sha256) { $sha256.Dispose() }
        if ($null -ne $payloadBytes) { [Array]::Clear($payloadBytes, 0, $payloadBytes.Length) }
        if ($null -ne $digestBytes) { [Array]::Clear($digestBytes, 0, $digestBytes.Length) }
        if ($bstr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
    }
}

function Set-P5EProcessExpectedFingerprint {
    param(
        [Parameter(Mandatory = $true)][ValidatePattern('^[0-9a-f]{64}$')][string]$Fingerprint,
        [ValidateNotNullOrEmpty()][string]$EnvironmentName = $script:P5EAccountEnvironmentName
    )

    [Environment]::SetEnvironmentVariable($EnvironmentName, $Fingerprint, 'Process')
}

function Clear-P5EProcessExpectedFingerprint {
    param(
        [ValidateNotNullOrEmpty()][string]$EnvironmentName = $script:P5EAccountEnvironmentName
    )

    $environmentPath = 'Env:' + $EnvironmentName
    if (Test-Path -LiteralPath $environmentPath) {
        Remove-Item -LiteralPath $environmentPath -Force
    }
}

function Invoke-P5EExpectedFingerprintLoadFromSecureKey {
    param(
        [Parameter(Mandatory = $true)][Security.SecureString]$ApiKey,
        [ValidateNotNullOrEmpty()][string]$EnvironmentName = $script:P5EAccountEnvironmentName
    )

    $fingerprint = $null
    try {
        # A new attempt never inherits a stale expected value.
        Clear-P5EProcessExpectedFingerprint -EnvironmentName $EnvironmentName
        $fingerprint = Get-P5EEndpointAccountFingerprintFromSecureKey `
            -Endpoint $script:P5ECurrentEndpoint -ApiKey $ApiKey
        Set-P5EProcessExpectedFingerprint -Fingerprint $fingerprint -EnvironmentName $EnvironmentName
    } catch {
        # A failed attempt must not leave either an old or partially loaded value.
        try { Clear-P5EProcessExpectedFingerprint -EnvironmentName $EnvironmentName } catch { }
        throw 'P5E_EXPECTED_VALUE_PROCESS_LOAD_FAILED'
    } finally {
        $fingerprint = $null
    }
}

function Invoke-P5EExpectedFingerprintLoad {
    $secureKey = $null
    try {
        $secureKey = Read-Host -Prompt 'Enter original OpenRouter API key (hidden)' -AsSecureString
        Invoke-P5EExpectedFingerprintLoadFromSecureKey -ApiKey $secureKey
    } catch {
        try { Clear-P5EProcessExpectedFingerprint } catch { }
        throw 'P5E_EXPECTED_VALUE_PROCESS_LOAD_FAILED'
    } finally {
        if ($null -ne $secureKey) { $secureKey.Dispose() }
    }
    Write-Output 'P5E_EXPECTED_VALUE_PROCESS_LOAD=PASS'
}

if ($PSCmdlet.ParameterSetName -eq 'Library') { return }
if ($PSCmdlet.ParameterSetName -eq 'Clear') {
    Clear-P5EProcessExpectedFingerprint
    Write-Output 'P5E_EXPECTED_VALUE_PROCESS_CLEAR=PASS'
    return
}

Invoke-P5EExpectedFingerprintLoad
