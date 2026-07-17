[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ChecklistPath,

    [Parameter(Mandatory = $true)]
    [ValidateSet('PreTag', 'PreBackup', 'Complete')]
    [string]$Gate,

    [string]$ExpectedVersion
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $ChecklistPath -PathType Leaf)) {
    throw "Release checklist not found: $ChecklistPath"
}

$content = Get-Content -LiteralPath $ChecklistPath -Raw
$versionMatch = [regex]::Match($content, '(?m)^- Version:\s*`v(\d+\.\d+(?:\.\d+)?)`\s*$')
if (-not $versionMatch.Success) {
    throw 'Checklist must contain a concrete version such as: - Version: `v4.8.0`'
}

$versionText = $versionMatch.Groups[1].Value
$version = [version]$versionText
if ($version -lt [version]'4.8') {
    throw "The fixed Development Workflow applies to v4.8 and later; found v$version"
}
if (-not [string]::IsNullOrWhiteSpace($ExpectedVersion) -and $versionText -ne $ExpectedVersion) {
    throw "Checklist version v$versionText does not match expected version v$ExpectedVersion"
}

$requiredSteps = switch ($Gate) {
    'PreTag' { 9 }
    'PreBackup' { 10 }
    'Complete' { 14 }
}

$stepPattern = '(?m)^- \[([ xX])\] (\d{2}) (.+)$'
$steps = [regex]::Matches($content, $stepPattern)
if ($steps.Count -ne 14) {
    throw "Checklist must contain exactly 14 workflow steps; found $($steps.Count)"
}

for ($index = 0; $index -lt 14; $index++) {
    $expectedNumber = $index + 1
    $actualNumber = [int]$steps[$index].Groups[2].Value
    if ($actualNumber -ne $expectedNumber) {
        throw "Workflow step order is invalid at position $expectedNumber"
    }

    if ($expectedNumber -gt $requiredSteps) {
        continue
    }

    if ([string]::IsNullOrWhiteSpace($steps[$index].Groups[1].Value)) {
        throw "Step $($steps[$index].Groups[2].Value) is not complete for gate $Gate"
    }

    $blockStart = $steps[$index].Index + $steps[$index].Length
    $blockEnd = if ($index + 1 -lt $steps.Count) { $steps[$index + 1].Index } else { $content.Length }
    $block = $content.Substring($blockStart, $blockEnd - $blockStart)
    $evidenceMatch = [regex]::Match($block, '(?m)^\s+- Evidence:\s*(.+?)\s*$')
    if (-not $evidenceMatch.Success) {
        throw "Step $($steps[$index].Groups[2].Value) has no Evidence line"
    }

    $evidence = $evidenceMatch.Groups[1].Value.Trim()
    if ([string]::IsNullOrWhiteSpace($evidence) -or $evidence -eq 'TODO') {
        throw "Step $($steps[$index].Groups[2].Value) has no concrete Evidence"
    }
}

if ($Gate -eq 'Complete') {
    if ($content -notmatch '(?m)^- Workflow status:\s*`COMPLETE`\s*$') {
        throw 'Complete gate requires: - Workflow status: `COMPLETE`'
    }
}

Write-Output "Release workflow gate passed: $Gate for v$versionText"
