$V = 'C:\Users\ADMIN\AppData\Local\Temp\claude\D--App-Translate-Books\53a747e4-f54e-4bc7-bcc7-125df841973c\scratchpad\verifier-repro'
$evName = 'raw-live-a43-preauth-f8a0ee32a8ef14b1950134742bf53ce004588131981ff19b0fb5ee3b825bd77e'
$origEv = 'D:\P5E-private\' + $evName
$copyEv = Join-Path $V ('copy\' + $evName)
$tree = Join-Path $V 'tree'
$origRepo = 'D:\App Translate Books'
function Global:MapP([string]$p) {
    if ($p.StartsWith($origEv, [StringComparison]::OrdinalIgnoreCase)) { return $copyEv + $p.Substring($origEv.Length) }
    if ($p.StartsWith($origRepo + '\', [StringComparison]::OrdinalIgnoreCase)) { return $tree + $p.Substring($origRepo.Length) }
    return $p
}
. (Join-Path $tree 'scripts\p5e-raw-live-supervisor.ps1') -LibraryOnly
$ErrorActionPreference = 'Stop'
function Get-P5ECanonicalPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    $Path = MapP $Path
    if (Microsoft.PowerShell.Management\Test-Path -LiteralPath $Path) { return ([IO.Path]::GetFullPath((Microsoft.PowerShell.Management\Resolve-Path -LiteralPath $Path).Path)).TrimEnd('\') }
    return ([IO.Path]::GetFullPath($Path)).TrimEnd('\')
}
function Test-Path { param([string]$LiteralPath, [string]$PathType) if ($PathType) { Microsoft.PowerShell.Management\Test-Path -LiteralPath (MapP $LiteralPath) -PathType $PathType } else { Microsoft.PowerShell.Management\Test-Path -LiteralPath (MapP $LiteralPath) } }
function Get-Item { param([string]$LiteralPath, [switch]$Force) Microsoft.PowerShell.Management\Get-Item -LiteralPath (MapP $LiteralPath) -Force:$Force }
function Get-Content { param([string]$LiteralPath, [switch]$Raw) Microsoft.PowerShell.Management\Get-Content -LiteralPath (MapP $LiteralPath) -Raw:$Raw }
function Get-FileHash { param([string]$LiteralPath, [string]$Algorithm) Microsoft.PowerShell.Utility\Get-FileHash -LiteralPath (MapP $LiteralPath) -Algorithm $Algorithm }
foreach ($fn in 'Test-P5EProvenanceFile', 'Test-P5ESerializedArtifactBytes') {
    $def = (Microsoft.PowerShell.Core\Get-Command $fn -CommandType Function).Definition
    $new = $def -replace '\[IO\.File\]::ReadAllBytes\((\$\w+)\)', '[IO.File]::ReadAllBytes((MapP $1))'
    if ($new -eq $def) { throw "no patch for $fn" }
    Invoke-Expression ("function $fn { $new }")
}
$r = Invoke-P5EOutcomeVerifier -InstrumentationStdoutPath (Join-Path $origEv 'instrumentation-stdout.txt') `
  -InstrumentationStderrPath (Join-Path $origEv 'instrumentation-stderr.txt') `
  -MetadataPath (Join-Path $origEv 'HOST_RUN_METADATA.json') -PostReadbackPath (Join-Path $origEv 'post-readback.json') `
  -CollectorOutcomePath (Join-Path $origEv 'COLLECTOR_OUTCOME.json')
$text = ConvertTo-P5EJson -Value $r
[IO.File]::WriteAllText((Join-Path $V 'repro-OUTCOME_VERIFICATION.json'), $text, [Text.UTF8Encoding]::new($false))
$text
