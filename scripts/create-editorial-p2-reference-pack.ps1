param(
    [string] $CanonicalZipPath,
    [string] $ControlZipPath,
    [string] $ManifestPath,
    [string] $ReceiptPath
)

$ErrorActionPreference = 'Stop'
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$authorityRoot = 'D:\Ebooks\1. Prompt cac the loai\4.BIÊN TẬP\BIEN_TAP_V5_SAFE_4_1_3_FULL_RELEASE'
$chatgptRoot = Join-Path $authorityRoot 'CHATGPT'
$templatePath = Join-Path $repoRoot 'editorial-engine\src\test\resources\editorial-p1\editorial-pack.json'
$javaWriterPath = Join-Path $PSScriptRoot 'EditorialP2JavaZipWriter.java'
$fixedArchiveTimestamp = '2026-09-03T00:00:00+07:00'
$fixedArchiveTimestampMillis = 1757862000000L
$expectedEntryNames = @('editorial-pack.json', 'project.txt', 'prompt.txt', 'workflow.txt')

function Resolve-RepoPath([string] $path) {
    if ([string]::IsNullOrWhiteSpace($path)) { return $null }
    if ([System.IO.Path]::IsPathRooted($path)) { return [System.IO.Path]::GetFullPath($path) }
    return [System.IO.Path]::GetFullPath((Join-Path $repoRoot $path))
}

if ([string]::IsNullOrWhiteSpace($CanonicalZipPath)) { $CanonicalZipPath = 'app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-canonical.zip' }
if ([string]::IsNullOrWhiteSpace($ControlZipPath)) { $ControlZipPath = 'app/src/androidTest/assets/editorial-p2/v5-safe-4.1.3-full-java-control.zip' }
if ([string]::IsNullOrWhiteSpace($ManifestPath)) { $ManifestPath = 'editorial-engine/src/test/resources/editorial-p2/editorial-pack.json' }
if ([string]::IsNullOrWhiteSpace($ReceiptPath)) { $ReceiptPath = 'docs/P2_REFERENCE_PACK_CHECKSUM_RECEIPT.json' }

$CanonicalZipPath = Resolve-RepoPath $CanonicalZipPath
$ControlZipPath = Resolve-RepoPath $ControlZipPath
$ManifestPath = Resolve-RepoPath $ManifestPath
$ReceiptPath = Resolve-RepoPath $ReceiptPath

function Read-Bytes([string] $path) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Required file is missing: $path" }
    return [System.IO.File]::ReadAllBytes($path)
}

function Sha256([byte[]] $bytes) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { return ([System.BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}

function Assert-BytesEqual([byte[]] $actual, [byte[]] $expected, [string] $label) {
    if ($null -eq $actual -or $null -eq $expected -or $actual.Length -ne $expected.Length) {
        $actualLength = if ($null -eq $actual) { '<null>' } else { $actual.Length }
        $expectedLength = if ($null -eq $expected) { '<null>' } else { $expected.Length }
        $firstDifference = -1
        if ($null -ne $actual -and $null -ne $expected) {
            $limit = [Math]::Min($actual.Length, $expected.Length)
            for ($offset = 0; $offset -lt $limit; $offset++) {
                if ($actual[$offset] -ne $expected[$offset]) { $firstDifference = $offset; break }
            }
            if ($firstDifference -lt 0 -and $actual.Length -ne $expected.Length) { $firstDifference = $limit }
        }
        throw "$label byte length mismatch: actual=$actualLength expected=$expectedLength firstDifference=$firstDifference"
    }
    for ($index = 0; $index -lt $expected.Length; $index++) {
        if ($actual[$index] -ne $expected[$index]) { throw "$label byte mismatch at offset $index" }
    }
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
        $keys = [string[]] @($value.Keys | ForEach-Object { [string] $_ })
        [System.Array]::Sort($keys, [System.StringComparer]::Ordinal)
        $parts = [System.Collections.Generic.List[string]]::new()
        foreach ($key in $keys) { $parts.Add((Escape-JsonString $key) + ':' + (Write-CanonicalJson $value[$key])) }
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

function New-Map([hashtable] $values) {
    $map = [ordered] @{}
    foreach ($key in $values.Keys) { $map[$key] = $values[$key] }
    return $map
}

function Test-ByteSequence([byte[]] $bytes, [byte[]] $needle) {
    if ($needle.Length -eq 0 -or $bytes.Length -lt $needle.Length) { return $false }
    for ($start = 0; $start -le $bytes.Length - $needle.Length; $start++) {
        $matches = $true
        for ($offset = 0; $offset -lt $needle.Length; $offset++) {
            if ($bytes[$start + $offset] -ne $needle[$offset]) { $matches = $false; break }
        }
        if ($matches) { return $true }
    }
    return $false
}

function Read-ZipEntryBytes([System.IO.Compression.ZipArchiveEntry] $entry) {
    $stream = $entry.Open()
    $memory = [System.IO.MemoryStream]::new()
    try {
        $stream.CopyTo($memory)
        return ,$memory.ToArray()
    }
    finally {
        $memory.Dispose()
        $stream.Dispose()
    }
}

function Read-AndValidateZip([string] $path, [hashtable] $expectedPayloads, [string] $label) {
    $archive = [System.IO.Compression.ZipFile]::OpenRead($path)
    try {
        $entries = @($archive.Entries)
        if ($entries.Count -ne $expectedEntryNames.Count) { throw "$label must contain exactly four entries; found $($entries.Count)" }
        $names = @($entries | ForEach-Object { $_.FullName })
        if ([string]::Join('|', [string[]]$names) -ne [string]::Join('|', [string[]]$expectedEntryNames)) {
            throw "$label entry order/names are not canonical: $([string]::Join(', ', [string[]]$names))"
        }
        $payloads = [ordered] @{}
        foreach ($entry in $entries) {
            $name = [string]$entry.FullName
            if ($name.EndsWith('/') -or $name.EndsWith('\') -or $name.Contains('/') -or $name.Contains('\') -or $name.Contains('..') -or [System.IO.Path]::IsPathRooted($name)) {
                throw "$label contains an invalid root entry: $name"
            }
            $payload = Read-ZipEntryBytes $entry
            Assert-BytesEqual $payload $expectedPayloads[$name] "$label/$name"
            $payloads[$name] = $payload
        }
        return $payloads
    }
    finally { $archive.Dispose() }
}

function Ensure-TargetAbsent([string] $path) {
    if (Test-Path -LiteralPath $path) { throw "Refusing to overwrite existing P2 output: $path" }
}

if (-not (Test-Path -LiteralPath $authorityRoot -PathType Container)) { throw "Editorial authority directory is missing: $authorityRoot" }
if (-not (Test-Path -LiteralPath $templatePath -PathType Leaf)) { throw "P1 manifest template is missing: $templatePath" }
if (-not (Test-Path -LiteralPath $javaWriterPath -PathType Leaf)) { throw "Java control writer is missing: $javaWriterPath" }
foreach ($target in @($CanonicalZipPath, $ControlZipPath, $ManifestPath, $ReceiptPath)) { Ensure-TargetAbsent $target }

$authority = @(
    [ordered]@{ role = 'PROJECT_INSTRUCTION'; path = 'project.txt'; source = 'PROJECT_INSTRUCTION_BIEN_TAP_V5_SAFE_4_1_3_FULL.txt'; byteLength = 9485L; sha256 = '1727AE173F2CFD530EB818CAE69E0D3FADC59C35E3B5B6D478704A02091A26AD' },
    [ordered]@{ role = 'TURN_PROMPT'; path = 'prompt.txt'; source = 'PROMPT_DAU_CHAT_3_LUOT_V5_SAFE_4_1_3_FULL.txt'; byteLength = 8852L; sha256 = 'D25757D1A6BDDD5962A3B178B9EF850727573AE0C34867EC8F4B8450C7CD754F' },
    [ordered]@{ role = 'WORKFLOW'; path = 'workflow.txt'; source = 'WORKFLOW_BIEN_TAP_3_LUOT_V5_SAFE_4_1_3_FULL.txt'; byteLength = 34917L; sha256 = '5DB6B4F6509313F106499113537D2880BC6D2FF663859239DAFB285557505730' }
)

$authorityBytes = [ordered] @{}
foreach ($item in $authority) {
    $path = Join-Path $chatgptRoot $item.source
    $bytes = Read-Bytes $path
    if ($bytes.Length -ne $item.byteLength) { throw "Authority byte length drift for $($item.source): $($bytes.Length)" }
    if ((Sha256 $bytes) -ne $item.sha256.ToLowerInvariant()) { throw "Authority SHA-256 drift for $($item.source)" }
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xef -and $bytes[1] -eq 0xbb -and $bytes[2] -eq 0xbf) { throw "Authority BOM is forbidden: $($item.source)" }
    try { [void]([System.Text.UTF8Encoding]::new($false, $true).GetString($bytes)) }
    catch { throw "Authority is not strict UTF-8: $($item.source)" }
    $authorityBytes[$item.path] = $bytes
}

$templateBytes = Read-Bytes $templatePath
if ($templateBytes.Length -ge 3 -and $templateBytes[0] -eq 0xef -and $templateBytes[1] -eq 0xbb -and $templateBytes[2] -eq 0xbf) { throw 'P1 manifest template has a forbidden BOM' }
$manifest = ConvertFrom-Json -InputObject ([System.Text.Encoding]::UTF8.GetString($templateBytes)) -AsHashtable -Depth 100 -DateKind String
$manifestCanonicalBytes = [System.Text.Encoding]::UTF8.GetBytes((Write-CanonicalJson $manifest))
Assert-BytesEqual $templateBytes $manifestCanonicalBytes 'P1 manifest template'

$requiredRootFields = @(
    'manifestFormat', 'manifestVersion', 'packId', 'version', 'displayName', 'contractVersion',
    'schemaVersion', 'minimumEngineVersion', 'compatibilityClass', 'requiredCapabilities',
    'fileRoles', 'canonicalPackHash', 'inputRoles', 'pronounPolicy', 'pairContextPolicy',
    'phaseGraph', 'contextAllowList', 'evidenceSchemas', 'gateDefinitions', 'releaseArtifacts',
    'goldenReplayCases', 'createdAt', 'migrationPolicy')
foreach ($field in $requiredRootFields) { if (-not $manifest.ContainsKey($field)) { throw "P1 manifest is missing required field: $field" } }
$allowedRootFields = @($requiredRootFields + 'adapterId')
$actualRootFields = @($manifest.Keys | ForEach-Object { [string]$_ })
foreach ($field in $actualRootFields) {
    if ($allowedRootFields -notcontains $field) { throw "P1 manifest has an unknown root field: $field" }
}

$fixedExpectations = [ordered]@{
    manifestFormat = 'com.ml.tblandroidtxt.editorial-pack'
    manifestVersion = 1L
    packId = 'com.ml.tblandroidtxt.editorial.safe4.full'
    version = '4.1.3'
    contractVersion = 'safe4.full.three-pass.v1'
    schemaVersion = 'safe4.full.receipt.v1'
    minimumEngineVersion = '4.17.0'
    compatibilityClass = 'DATA_COMPATIBLE'
}
foreach ($key in $fixedExpectations.Keys) {
    if ([string]$manifest[$key] -ne [string]$fixedExpectations[$key]) { throw "P1 manifest fixed field drift: $key" }
}

$rolesByName = @{}
$fileRoles = @($manifest['fileRoles'])
if ($fileRoles.Count -ne 3) { throw "P1 manifest fileRoles count is $($fileRoles.Count), expected 3" }
foreach ($entry in $fileRoles) {
    $role = [string]$entry['role']
    if ($rolesByName.ContainsKey($role)) { throw "Duplicate manifest role: $role" }
    $rolesByName[$role] = $entry
}
foreach ($item in $authority) {
    if (-not $rolesByName.ContainsKey($item.role)) { throw "Manifest is missing authority role: $($item.role)" }
    $entry = $rolesByName[$item.role]
    if ([string]$entry['path'] -ne $item.path -or [string]$entry['mediaType'] -ne 'text/plain' -or [string]$entry['charset'] -ne 'UTF-8' -or [string]$entry['bom'] -ne 'FORBIDDEN') {
        throw "Manifest file metadata drift for $($item.role)"
    }
    if ([long]$entry['byteLength'] -ne $item.byteLength -or [string]$entry['sha256'] -ne $item.sha256.ToLowerInvariant()) {
        throw "Manifest authority length/hash drift for $($item.role)"
    }
}

$withoutHash = [ordered] @{}
foreach ($key in $manifest.Keys) { if ($key -ne 'canonicalPackHash') { $withoutHash[$key] = $manifest[$key] } }
$hashDomain = [System.Text.Encoding]::UTF8.GetBytes("EDITORIAL_PACK_CANONICAL_HASH_V1`n")
$withoutHashBytes = [System.Text.Encoding]::UTF8.GetBytes((Write-CanonicalJson $withoutHash))
$hashPayload = [byte[]]::new($hashDomain.Length + $withoutHashBytes.Length)
[System.Buffer]::BlockCopy($hashDomain, 0, $hashPayload, 0, $hashDomain.Length)
[System.Buffer]::BlockCopy($withoutHashBytes, 0, $hashPayload, $hashDomain.Length, $withoutHashBytes.Length)
$calculatedCanonicalPackHash = Sha256 $hashPayload
if ([string]$manifest['canonicalPackHash'] -ne $calculatedCanonicalPackHash) { throw 'P1 manifest canonicalPackHash does not recalculate' }

$stageRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('.editorial-p2-' + [guid]::NewGuid().ToString('N'))
$stageManifest = Join-Path $stageRoot 'editorial-pack.json'
$stageProject = Join-Path $stageRoot 'project.txt'
$stagePrompt = Join-Path $stageRoot 'prompt.txt'
$stageWorkflow = Join-Path $stageRoot 'workflow.txt'
$stageCanonicalZip = Join-Path $stageRoot 'v5-safe-4.1.3-full-canonical.zip'
$stageControlZip = Join-Path $stageRoot 'v5-safe-4.1.3-full-java-control.zip'
$stageReceipt = Join-Path $stageRoot 'P2_REFERENCE_PACK_CHECKSUM_RECEIPT.json'

try {
    New-Item -ItemType Directory -Path $stageRoot -Force | Out-Null
    [System.IO.File]::WriteAllBytes($stageManifest, $manifestCanonicalBytes)
    [System.IO.File]::WriteAllBytes($stageProject, $authorityBytes['project.txt'])
    [System.IO.File]::WriteAllBytes($stagePrompt, $authorityBytes['prompt.txt'])
    [System.IO.File]::WriteAllBytes($stageWorkflow, $authorityBytes['workflow.txt'])

    $zipFile = [System.IO.File]::Open($stageCanonicalZip, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
    try {
        $archive = [System.IO.Compression.ZipArchive]::new($zipFile, [System.IO.Compression.ZipArchiveMode]::Create, $true)
        try {
            $entries = @(
                [ordered]@{ name = 'editorial-pack.json'; bytes = $manifestCanonicalBytes },
                [ordered]@{ name = 'project.txt'; bytes = $authorityBytes['project.txt'] },
                [ordered]@{ name = 'prompt.txt'; bytes = $authorityBytes['prompt.txt'] },
                [ordered]@{ name = 'workflow.txt'; bytes = $authorityBytes['workflow.txt'] })
            foreach ($item in $entries) {
                $entry = $archive.CreateEntry($item.name, [System.IO.Compression.CompressionLevel]::Optimal)
                $entry.LastWriteTime = [DateTimeOffset]::Parse($fixedArchiveTimestamp)
                $stream = $entry.Open()
                try { $stream.Write($item.bytes, 0, $item.bytes.Length) }
                finally { $stream.Dispose() }
            }
        }
        finally { $archive.Dispose() }
    }
    finally { $zipFile.Dispose() }

    $javaHomeCandidates = @()
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { $javaHomeCandidates += $env:JAVA_HOME }
    $javaHomeCandidates += 'C:\Program Files\Android\Android Studio\jbr'
    $javaHomeCandidates += @((Get-ChildItem -LiteralPath 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName))
    $javaHome = $null
    foreach ($candidate in $javaHomeCandidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe'))) {
            $javaHome = $candidate
            break
        }
    }
    if ($null -eq $javaHome) { throw 'A JDK with javac is required to create the Java data-descriptor control ZIP' }
    $javacPath = Join-Path $javaHome 'bin\javac.exe'
    $javaPath = Join-Path $javaHome 'bin\java.exe'
    $compileRoot = Join-Path $stageRoot 'java-classes'
    New-Item -ItemType Directory -Path $compileRoot -Force | Out-Null
    & $javacPath '-d' $compileRoot $javaWriterPath
    if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }
    & $javaPath '-cp' $compileRoot 'EditorialP2JavaZipWriter' $stageManifest $stageProject $stagePrompt $stageWorkflow $stageControlZip
    if ($LASTEXITCODE -ne 0) { throw "Java control writer failed with exit code $LASTEXITCODE" }

    $expectedPayloads = [ordered]@{
        'editorial-pack.json' = $manifestCanonicalBytes
        'project.txt' = $authorityBytes['project.txt']
        'prompt.txt' = $authorityBytes['prompt.txt']
        'workflow.txt' = $authorityBytes['workflow.txt']
    }
    $canonicalPayloads = Read-AndValidateZip $stageCanonicalZip $expectedPayloads 'canonical ZIP'
    $controlPayloads = Read-AndValidateZip $stageControlZip $expectedPayloads 'Java control ZIP'
    foreach ($name in $expectedEntryNames) { Assert-BytesEqual $controlPayloads[$name] $canonicalPayloads[$name] "control payload/$name" }

    $canonicalZipBytes = Read-Bytes $stageCanonicalZip
    $controlZipBytes = Read-Bytes $stageControlZip
    $dataDescriptorSignature = [byte[]](0x50, 0x4b, 0x07, 0x08)
    $canonicalHasDataDescriptor = Test-ByteSequence $canonicalZipBytes $dataDescriptorSignature
    $controlHasDataDescriptor = Test-ByteSequence $controlZipBytes $dataDescriptorSignature
    if (-not $controlHasDataDescriptor) { throw 'Java control ZIP did not contain a data descriptor as required by the control fixture' }

    $manifestSha256 = Sha256 $manifestCanonicalBytes
    $canonicalZipSha256 = Sha256 $canonicalZipBytes
    $controlZipSha256 = Sha256 $controlZipBytes
    $relative = {
        param([string] $path)
        return ([System.IO.Path]::GetRelativePath($repoRoot, $path)).Replace('\', '/')
    }
    $receipt = [ordered]@{
        receiptFormat = 'com.ml.tblandroidtxt.editorial-p2-reference-receipt'
        receiptVersion = 1L
        sourceBaselineCommit = '921af9256e1b1fe4ab9ac113affa98eec7a1e339'
        authorityRelease = 'V5-SAFE.4.1.3-FULL'
        generator = 'scripts/create-editorial-p2-reference-pack.ps1'
        generatorSupport = 'scripts/EditorialP2JavaZipWriter.java'
        fixedArchiveTimestamp = $fixedArchiveTimestamp
        archiveEntryOrder = $expectedEntryNames
        packageBoundary = [ordered]@{
            exactRootEntryCount = 4L
            exactRootEntries = $expectedEntryNames
            executablePayloadAllowed = $false
            forbidden = @('folder-entry', 'nested-path', 'absolute-path', 'parent-segment', 'alternate-separator', 'symlink', 'APP', 'COMMON', 'TESTS', 'script', 'executable')
        }
        authorityFiles = @($authority | ForEach-Object { [ordered]@{ entry = $_.path; role = $_.role; source = $_.source; byteLength = $_.byteLength; sha256 = $_.sha256.ToLowerInvariant() } })
        canonicalManifest = [ordered]@{ relativePath = (& $relative $ManifestPath); byteLength = [long]$manifestCanonicalBytes.Length; sha256 = $manifestSha256; canonicalPackHash = [string]$manifest['canonicalPackHash'] }
        canonicalIdentity = [ordered]@{
            definition = 'canonical manifest semantics plus exact authority hashes'
            packId = [string]$manifest['packId']
            version = [string]$manifest['version']
            manifestFormat = [string]$manifest['manifestFormat']
            manifestVersion = [long]$manifest['manifestVersion']
            contractVersion = [string]$manifest['contractVersion']
            schemaVersion = [string]$manifest['schemaVersion']
            canonicalPackHash = [string]$manifest['canonicalPackHash']
            manifestSha256 = $manifestSha256
            authoritySha256 = [ordered]@{ project = $authority[0].sha256.ToLowerInvariant(); prompt = $authority[1].sha256.ToLowerInvariant(); workflow = $authority[2].sha256.ToLowerInvariant() }
        }
        transportArtifacts = [ordered]@{
            canonicalZip = [ordered]@{ relativePath = (& $relative $CanonicalZipPath); sha256 = $canonicalZipSha256; usesDataDescriptor = $canonicalHasDataDescriptor }
            javaControlZip = [ordered]@{ relativePath = (& $relative $ControlZipPath); sha256 = $controlZipSha256; usesDataDescriptor = $controlHasDataDescriptor }
            sameCanonicalPayload = $true
            sameCanonicalIdentity = $true
            zipSha256MayDiffer = $true
        }
        compatibilityIdentityDoesNotUse = @('ZIP SHA-256', 'filename', 'displayName', 'transport writer')
        p2Decision = 'GAP-012 belongs to importer transport handling; canonical pack bytes remain unchanged'
    }
    $receiptBytes = [System.Text.Encoding]::UTF8.GetBytes((Write-CanonicalJson $receipt))
    [System.IO.File]::WriteAllBytes($stageReceipt, $receiptBytes)

    foreach ($target in @($CanonicalZipPath, $ControlZipPath, $ManifestPath, $ReceiptPath)) { Ensure-TargetAbsent $target }
    foreach ($target in @($CanonicalZipPath, $ControlZipPath, $ManifestPath, $ReceiptPath)) {
        $parent = [System.IO.Path]::GetDirectoryName($target)
        if (-not [string]::IsNullOrWhiteSpace($parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
    }
    Move-Item -LiteralPath $stageCanonicalZip -Destination $CanonicalZipPath
    Move-Item -LiteralPath $stageControlZip -Destination $ControlZipPath
    Move-Item -LiteralPath $stageManifest -Destination $ManifestPath
    Move-Item -LiteralPath $stageReceipt -Destination $ReceiptPath

    Write-Output "Created canonical manifest: $ManifestPath"
    Write-Output "Created canonical ZIP: $CanonicalZipPath"
    Write-Output "Created Java control ZIP: $ControlZipPath"
    Write-Output "Created checksum receipt: $ReceiptPath"
    Write-Output "Manifest bytes: $($manifestCanonicalBytes.Length)"
    Write-Output "Manifest SHA-256: $manifestSha256"
    Write-Output "Canonical pack hash: $($manifest['canonicalPackHash'])"
    Write-Output "Canonical ZIP SHA-256: $canonicalZipSha256"
    Write-Output "Java control ZIP SHA-256: $controlZipSha256"
    Write-Output "Canonical/control payload identity: $($true)"
}
finally {
    if (Test-Path -LiteralPath $stageRoot) { Remove-Item -LiteralPath $stageRoot -Recurse -Force -ErrorAction SilentlyContinue }
}
