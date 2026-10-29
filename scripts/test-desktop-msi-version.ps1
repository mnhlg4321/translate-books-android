$ErrorActionPreference = 'Stop'
# Offline test of scripts/desktop-msi-version.ps1: the installer version never goes down from one package to the next.
. (Join-Path $PSScriptRoot 'desktop-msi-version.ps1')
$failures = New-Object System.Collections.Generic.List[string]

function Expect-Version {
    param([string]$Display, [string]$Expected)
    $actual = Get-DesktopMsiVersion -Version $Display
    if ($actual -ne $Expected) { $failures.Add("$Display : expected $Expected, got $actual") }
}

Expect-Version '4.24-pc-dev.9' '4.24.9'
Expect-Version '4.24-pc-dev.11' '4.24.11'
Expect-Version '4.24.0-rc.1' '4.24.101'
Expect-Version '4.24.0-rc.2' '4.24.102'
Expect-Version '4.24.0' '4.24.200'
Expect-Version '4.24.1' '4.24.201'

# the order in which packages are installed on the owner's PC: every ProductVersion must be higher than the one before
$order = '4.24-pc-dev.9', '4.24-pc-dev.10', '4.24-pc-dev.11', '4.24.0-rc.1', '4.24.0-rc.2', '4.24.0-rc.3', '4.24.0', '4.24.1'
$previous = $null
foreach ($display in $order) {
    $current = [version](Get-DesktopMsiVersion -Version $display)
    if ($previous -and $current -le $previous) { $failures.Add("$display ($current) is not higher than the package before it ($previous)") }
    $previous = $current
}

# a number that would cross into the next range is refused instead of silently going backwards
foreach ($bad in '4.24-pc-dev.100', '4.24.0-rc.100') {
    $refused = $false
    try { Get-DesktopMsiVersion -Version $bad | Out-Null } catch { $refused = $true }
    if (-not $refused) { $failures.Add("$bad should be refused") }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    exit 1
}
'test-desktop-msi-version: all ordering cases passed'
