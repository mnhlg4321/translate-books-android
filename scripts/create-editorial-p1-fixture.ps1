$ErrorActionPreference = 'Stop'

# P1-only generator. It reads the external authority and writes only test
# resources; nothing under app/src/main or app/src/main/assets is touched.
$repoRoot = Split-Path -Parent $PSScriptRoot
$authorityRoot = 'D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE'
$chatgptRoot = Join-Path $authorityRoot 'CHATGPT'
$androidAssetRoot = Join-Path $repoRoot 'app\src\androidTest\assets\editorial-p1'
$engineResourceRoot = Join-Path $repoRoot 'editorial-engine\src\test\resources\editorial-p1'
$zipPath = Join-Path $androidAssetRoot 'v5-safe-4.1.3-full.zip'
$manifestPath = Join-Path $engineResourceRoot 'editorial-pack.json'

if (-not (Test-Path -LiteralPath $authorityRoot -PathType Container)) {
    throw "Editorial authority directory is missing: $authorityRoot"
}
foreach ($target in @($zipPath, $manifestPath)) {
    if (Test-Path -LiteralPath $target) {
        throw "Refusing to overwrite existing P1 fixture output: $target"
    }
}

function New-Map([hashtable] $values) {
    $map = [ordered]@{}
    foreach ($key in $values.Keys) { $map[$key] = $values[$key] }
    return $map
}

function Escape-JsonString([string] $value) {
    $builder = [System.Text.StringBuilder]::new()
    [void] $builder.Append('"')
    foreach ($character in $value.ToCharArray()) {
        switch ([int][char]$character) {
            0x22 { [void] $builder.Append([char]0x5c); [void] $builder.Append([char]0x22) }
            0x5c { [void] $builder.Append([char]0x5c); [void] $builder.Append([char]0x5c) }
            0x08 { [void] $builder.Append([char]0x5c); [void] $builder.Append('b') }
            0x0c { [void] $builder.Append([char]0x5c); [void] $builder.Append('f') }
            0x0a { [void] $builder.Append([char]0x5c); [void] $builder.Append('n') }
            0x0d { [void] $builder.Append([char]0x5c); [void] $builder.Append('r') }
            0x09 { [void] $builder.Append([char]0x5c); [void] $builder.Append('t') }
            { $_ -lt 0x20 } { [void] $builder.Append([char]0x5c); [void] $builder.Append(('u{0:x4}' -f [int][char]$character)) }
            default { [void] $builder.Append($character) }
        }
    }
    [void] $builder.Append('"')
    return $builder.ToString()
}

function Write-CanonicalJson($value) {
    if ($null -eq $value) { return 'null' }
    if ($value -is [string]) { return (Escape-JsonString $value) }
    if ($value -is [bool]) { return ($(if ($value) { 'true' } else { 'false' })) }
    if ($value -is [System.Collections.IDictionary]) {
        $keys = [string[]] @($value.Keys | ForEach-Object { [string]$_ })
        [System.Array]::Sort($keys, [System.StringComparer]::Ordinal)
        $parts = [System.Collections.Generic.List[string]]::new()
        foreach ($key in $keys) {
            $parts.Add((Escape-JsonString $key) + ':' + (Write-CanonicalJson $value[$key]))
        }
        return '{' + [string]::Join(',', $parts) + '}'
    }
    if ($value -is [System.Collections.IEnumerable] -and $value -isnot [string]) {
        $parts = [System.Collections.Generic.List[string]]::new()
        foreach ($item in $value) { $parts.Add((Write-CanonicalJson $item)) }
        return '[' + [string]::Join(',', $parts) + ']'
    }
    if ($value -is [System.IFormattable]) {
        return $value.ToString($null, [System.Globalization.CultureInfo]::InvariantCulture)
    }
    throw "Unsupported JSON value type: $($value.GetType().FullName)"
}

function Read-AuthorityBytes([string] $name) {
    $path = Join-Path $chatgptRoot $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Authority file is missing: $path" }
    $bytes = [System.IO.File]::ReadAllBytes($path)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xef -and $bytes[1] -eq 0xbb -and $bytes[2] -eq 0xbf) {
        throw "Authority file unexpectedly contains a UTF-8 BOM: $path"
    }
    return $bytes
}

function Sha256([byte[]] $bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return ([System.BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}

function File-Role([string] $role, [string] $path, [byte[]] $bytes) {
    return (New-Map @{
        role = $role
        path = $path
        mediaType = 'text/plain'
        charset = 'UTF-8'
        bom = 'FORBIDDEN'
        byteLength = [long]$bytes.Length
        sha256 = (Sha256 $bytes)
    })
}

$projectBytes = Read-AuthorityBytes 'PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt'
$promptBytes = Read-AuthorityBytes 'PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt'
$workflowBytes = Read-AuthorityBytes 'WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt'

$requiredCapabilities = @(
    'pack.integrity.sha256.v1',
    'source.preflight.safe4-full.v1',
    'bundle.phase-visibility.safe4-full.v1',
    'status.glossary-pronoun.safe4-full.v1',
    'ledger.exhaustive.safe4-full.v1',
    'preserve.draft.safe4-full.v1',
    'stop.typed.safe4-full.v1',
    'diff.change-coverage.v1',
    'qa.l3-two-adversarial.v1',
    'release.safe4-full.v1',
    'replay.safe4.g1-g24.v1')

$inputRoles = @(
    (New-Map @{ role = 'RAW'; cardinality = 'ONE'; required = $true }),
    (New-Map @{ role = 'DRAFT'; cardinality = 'ONE'; required = $true }),
    (New-Map @{ role = 'GLOSSARY'; cardinality = 'ONE'; required = $true }),
    (New-Map @{ role = 'PRONOUN'; cardinality = 'ONE'; required = $true }))

$pronounPolicy = New-Map @{
    allowedStatuses = @('AVAILABLE', 'NONE', 'LEGACY_REJECTED')
    defaultStatus = 'NONE'
    availableRequiresRole = 'PRONOUN'
    noneForbidsRole = $true
    legacyRejectedForbidsRole = $true
    fallbackLookupAllowed = $false
}

$pairContextPolicy = New-Map @{
    optional = $true
    source = 'PAIR_CONTEXT'
    qaConfirmedRequired = $false
    sameProjectRequired = $true
    samePackHashRequired = $true
    scopeRequired = $false
    boundaryRequired = $false
}

$phases = @(
    'L1_SOURCE_PREFLIGHT', 'L1_RAW_DISCOVERY', 'L1_RECONCILE',
    'L2_RAW_DISCOVERY', 'L2_EDIT', 'L3_RAW_FIRST_REAUDIT', 'L3_RECONCILE')
$edges = @()
for ($index = 0; $index -lt ($phases.Count - 1); $index++) { $edges += ,@($phases[$index], $phases[$index + 1]) }
$phaseGraph = New-Map @{
    profile = 'safe4.full.three-pass.v1'
    initialPhase = $phases[0]
    terminalPhase = $phases[$phases.Count - 1]
    phases = $phases
    edges = $edges
}

$contextAllowList = New-Map @{
    L1_SOURCE_PREFLIGHT = (New-Map @{ required = @('RAW', 'DRAFT', 'GLOSSARY', 'PRONOUN'); allowed = @('RAW', 'DRAFT', 'GLOSSARY', 'PRONOUN') })
    L1_RAW_DISCOVERY = (New-Map @{ required = @('RAW', 'GLOSSARY'); allowed = @('RAW', 'GLOSSARY') })
    L1_RECONCILE = (New-Map @{ required = @('RAW', 'DRAFT', 'GLOSSARY'); allowed = @('RAW', 'DRAFT', 'GLOSSARY', 'PRONOUN', 'PAIR_CONTEXT') })
    L2_RAW_DISCOVERY = (New-Map @{ required = @('RAW', 'GLOSSARY'); allowed = @('RAW', 'GLOSSARY') })
    L2_EDIT = (New-Map @{ required = @('RAW', 'DRAFT', 'GLOSSARY', 'REPORT_L1'); allowed = @('RAW', 'DRAFT', 'GLOSSARY', 'REPORT_L1', 'PRONOUN', 'PAIR_CONTEXT') })
    L3_RAW_FIRST_REAUDIT = (New-Map @{ required = @('RAW', 'GLOSSARY', 'VI_L2'); allowed = @('RAW', 'GLOSSARY', 'VI_L2') })
    L3_RECONCILE = (New-Map @{ required = @('RAW', 'DRAFT', 'GLOSSARY', 'REPORT_L1', 'VI_L2', 'CHANGE_MAP_L2'); allowed = @('RAW', 'DRAFT', 'GLOSSARY', 'REPORT_L1', 'VI_L2', 'CHANGE_MAP_L2', 'PRONOUN', 'PAIR_CONTEXT') })
}

$evidenceSchemas = @(
    (New-Map @{ evidenceType = 'REPORT_L1'; schemaId = 'safe4.full.report-l1.v1' }),
    (New-Map @{ evidenceType = 'VI_L2'; schemaId = 'safe4.full.vi-l2.v1' }),
    (New-Map @{ evidenceType = 'CHANGE_MAP_L2'; schemaId = 'safe4.full.change-map-l2.v1' }),
    (New-Map @{ evidenceType = 'FINAL_QA'; schemaId = 'safe4.full.final-qa.v1' }),
    (New-Map @{ evidenceType = 'QA_RECEIPT'; schemaId = 'safe4.full.receipt.v1' }))

$gateNames = @('ARTIFACT_IDENTITY', 'COVERAGE', 'TITLE_GLOSSARY', 'SEMANTIC_FIDELITY', 'RELATION_PAIR_PROOF', 'SPEAKER', 'CHANGE_COVERAGE', 'NO_REGRESSION', 'CONTINUITY_STRUCTURE_TECHNICAL')
$gateDefinitions = @()
foreach ($gate in $gateNames) {
    $gateDefinitions += ,(New-Map @{
        gateId = $gate
        calculatorId = ('safe4.full.gate.' + $gate.ToLowerInvariant())
        requires = @('QA_RECEIPT')
    })
}

$releaseArtifactRoles = @('REPORT_L1', 'VI_L2', 'CHANGE_MAP_L2', 'FINAL_QA', 'QA_RECEIPT', 'SHA256_MANIFEST', 'PROJECT_SOURCE')
$releaseArtifacts = @()
foreach ($role in $releaseArtifactRoles) {
    $releaseArtifacts += ,(New-Map @{
        role = $role
        required = $true
        nameTemplate = ('{chapter}.' + $role.ToLowerInvariant() + '.json')
        contentContract = ('safe4.full.' + $role.ToLowerInvariant() + '.v1')
        presentWhen = 'RELEASE'
    })
}

$goldenReplayCases = @()
foreach ($number in 1..24) {
    $goldenReplayCases += ,(New-Map @{
        caseId = "G$number"
        fixtureId = "V5-SAFE.4.1.3-FULL/G$number"
        expectedCode = "SAFE4_G${number}_QUALIFIED"
    })
}

$fileRoles = @(
    (File-Role 'PROJECT_INSTRUCTION' 'project.txt' $projectBytes),
    (File-Role 'TURN_PROMPT' 'prompt.txt' $promptBytes),
    (File-Role 'WORKFLOW' 'workflow.txt' $workflowBytes))

$root = New-Map @{
    manifestFormat = 'com.ml.tblandroidtxt.editorial-pack'
    manifestVersion = [long]1
    packId = 'com.ml.tblandroidtxt.editorial.safe4.full'
    version = '4.1.3'
    displayName = 'Biên tập V5-SAFE.4.1.3-FULL'
    contractVersion = 'safe4.full.three-pass.v1'
    schemaVersion = 'safe4.full.receipt.v1'
    minimumEngineVersion = '4.17.0'
    compatibilityClass = 'DATA_COMPATIBLE'
    requiredCapabilities = $requiredCapabilities
    fileRoles = $fileRoles
    canonicalPackHash = ('0' * 64)
    inputRoles = $inputRoles
    pronounPolicy = $pronounPolicy
    pairContextPolicy = $pairContextPolicy
    phaseGraph = $phaseGraph
    contextAllowList = $contextAllowList
    evidenceSchemas = $evidenceSchemas
    gateDefinitions = $gateDefinitions
    releaseArtifacts = (New-Map @{ maximumFiles = [long]7; artifacts = $releaseArtifacts; forbiddenArtifacts = @('EXECUTABLE_CODE', 'REMOTE_SCRIPT') })
    goldenReplayCases = $goldenReplayCases
    createdAt = '2026-09-03T00:00:00+07:00'
    migrationPolicy = (New-Map @{ automaticProjectUpgrade = $false; projectRebindAllowed = $false; existingProjectAction = 'READ_ONLY'; newProjectPolicy = 'EXPLICIT_SELECTION'; copyToDifferentPackRequiresNewProject = $true })
}

$withoutHash = New-Map @{}
foreach ($key in $root.Keys) { if ($key -ne 'canonicalPackHash') { $withoutHash[$key] = $root[$key] } }
$hashDomain = [System.Text.Encoding]::UTF8.GetBytes("EDITORIAL_PACK_CANONICAL_HASH_V1`n")
$hashBytes = [System.Text.Encoding]::UTF8.GetBytes((Write-CanonicalJson $withoutHash))
$hashPayload = [byte[]]::new($hashDomain.Length + $hashBytes.Length)
[System.Buffer]::BlockCopy($hashDomain, 0, $hashPayload, 0, $hashDomain.Length)
[System.Buffer]::BlockCopy($hashBytes, 0, $hashPayload, $hashDomain.Length, $hashBytes.Length)
$root['canonicalPackHash'] = Sha256 $hashPayload
$manifestBytes = [System.Text.Encoding]::UTF8.GetBytes((Write-CanonicalJson $root))

New-Item -ItemType Directory -Path $androidAssetRoot -Force | Out-Null
New-Item -ItemType Directory -Path $engineResourceRoot -Force | Out-Null
[System.IO.File]::WriteAllBytes($manifestPath, $manifestBytes)

$zipFile = [System.IO.File]::Open($zipPath, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
try {
    $archive = [System.IO.Compression.ZipArchive]::new($zipFile, [System.IO.Compression.ZipArchiveMode]::Create, $true)
    try {
        $entries = @(
            @{ name = 'editorial-pack.json'; bytes = $manifestBytes },
            @{ name = 'project.txt'; bytes = $projectBytes },
            @{ name = 'prompt.txt'; bytes = $promptBytes },
            @{ name = 'workflow.txt'; bytes = $workflowBytes })
        foreach ($item in $entries) {
            $entry = $archive.CreateEntry($item.name, [System.IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime = [DateTimeOffset]::Parse('2026-09-03T00:00:00+07:00')
            $stream = $entry.Open()
            try { $stream.Write($item.bytes, 0, $item.bytes.Length) }
            finally { $stream.Dispose() }
        }
    }
    finally { $archive.Dispose() }
}
finally { $zipFile.Dispose() }

$zipEntries = [System.IO.Compression.ZipFile]::OpenRead($zipPath).Entries
try {
    if ($zipEntries.Count -ne 4) { throw "Fixture ZIP entry count is $($zipEntries.Count), expected 4" }
    foreach ($entry in $zipEntries) {
        if ($entry.FullName.Contains('/') -or $entry.FullName.Contains('\\')) { throw "Fixture ZIP contains nested path: $($entry.FullName)" }
    }
}
finally { $zipEntries | ForEach-Object { $_.Archive.Dispose() } }

Write-Output "Created manifest: $manifestPath"
Write-Output "Created test-only ZIP: $zipPath"
Write-Output "Manifest bytes: $($manifestBytes.Length)"
Write-Output "Manifest SHA-256: $(Sha256 $manifestBytes)"
Write-Output "Pack canonical hash: $($root['canonicalPackHash'])"
Write-Output "ZIP SHA-256: $((Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant())"
