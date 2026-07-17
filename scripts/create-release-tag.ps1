[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+(\.\d+)?$')]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [string]$ChecklistPath,

    [string]$Message
)

$ErrorActionPreference = 'Stop'

$verifier = Join-Path $PSScriptRoot 'verify-release-workflow.ps1'
& $verifier -ChecklistPath $ChecklistPath -Gate PreTag -ExpectedVersion $Version

$workingChanges = @(& git status --porcelain)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to read Git status.'
}
if ($workingChanges.Count -gt 0) {
    throw 'Working tree must be clean before creating a release tag.'
}

$tagName = "v$Version"
& git rev-parse --verify --quiet "refs/tags/$tagName" | Out-Null
if ($LASTEXITCODE -eq 0) {
    throw "Release tag already exists; refusing to overwrite: $tagName"
}

if ([string]::IsNullOrWhiteSpace($Message)) {
    $Message = "Translate Books $tagName"
}

& git tag -a $tagName -m $Message
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create release tag: $tagName"
}

Write-Output "Created annotated release tag $tagName at $(& git rev-parse HEAD)"
