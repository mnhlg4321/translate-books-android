# Maps the display version of a desktop package to the three-part ProductVersion the .msi registers. Windows Installer only installs a package
# over another when its ProductVersion is higher, so the mapping is built to keep this order for every package that exists or will exist:
#   development 4.24-pc-dev.N  ->  4.24.N          (N stays below 100)
#   release candidate 4.24.0-rc.N  ->  4.24.(100+N)
#   release 4.24.P  ->  4.24.(200+P)
# so dev.11 (4.24.11) < rc.1 (4.24.101) < rc.2 (4.24.102) < 4.24.0 (4.24.200) < 4.24.1 (4.24.201).
function Get-DesktopMsiVersion {
    param([Parameter(Mandatory = $true)][string]$Version)
    if ($Version -match '^(\d+)\.(\d+)-pc-dev\.(\d+)$') {
        if ([int]$Matches[3] -ge 100) { throw "Development package number $($Matches[3]) would reach the release candidate range (100 and up)" }
        return "$($Matches[1]).$($Matches[2]).$($Matches[3])"
    }
    if ($Version -match '^(\d+)\.(\d+)\.\d+-rc\.(\d+)$') {
        if ([int]$Matches[3] -ge 100) { throw "Release candidate number $($Matches[3]) would reach the release range (200 and up)" }
        return "$($Matches[1]).$($Matches[2]).$(100 + [int]$Matches[3])"
    }
    if ($Version -match '^(\d+)\.(\d+)\.(\d+)$') { return "$($Matches[1]).$($Matches[2]).$(200 + [int]$Matches[3])" }
    $numeric = ($Version -replace '[^0-9.]', '.') -replace '\.+', '.'
    $parts = @($numeric.Trim('.').Split('.') | Select-Object -First 3)
    while ($parts.Count -lt 3) { $parts += '0' }
    return ($parts -join '.')
}
