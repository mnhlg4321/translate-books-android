[CmdletBinding()]
param(
    [string]$Series = '4.24-pc-dev',

    [ValidateNotNullOrEmpty()]
    [string]$Notes = 'Desktop development build.',

    [string]$JavaHome,

    [switch]$Offline,

    # Also build an unsigned per-user .msi with jpackage (needs a full JDK 21 as -JavaHome and WiX 3.x in -WixHome).
    [switch]$Msi,
    [string]$WixHome,

    # Dirty working trees are refused so the retained source ZIP is exactly the built commit.
    [switch]$AllowDirty
)

# Builds the Windows desktop app, tries jlink first, falls back to a full copy of the JDK folder, and keeps the same
# immutable package in artifacts/builds/desktop-v<series>.<N>/<event> and backup/builds/desktop-v<series>.<N>/<event>.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repo = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JavaHome)) { $JavaHome = 'C:\Program Files\Android\Android Studio\jbr' }
if (-not (Test-Path (Join-Path $JavaHome 'bin\java.exe'))) { throw "No java.exe under $JavaHome" }
$env:JAVA_HOME = $JavaHome

Push-Location $repo
try {
    $status = (& git status --porcelain) -join "`n"
    if (-not $AllowDirty -and -not [string]::IsNullOrWhiteSpace($status)) { throw 'Working tree is not clean; commit first or pass -AllowDirty.' }
    $commit = (& git rev-parse HEAD).Trim()
    $branch = (& git rev-parse --abbrev-ref HEAD).Trim()

    # next number: highest N already kept in either archive + 1
    $seriesDirName = "desktop-v$Series"
    $highest = 0
    foreach ($root in @('artifacts\builds', 'backup\builds')) {
        $base = Join-Path $repo $root
        if (-not (Test-Path $base)) { continue }
        Get-ChildItem -Path $base -Directory -Filter "$seriesDirName.*" | ForEach-Object {
            if ($_.Name -match ('^' + [regex]::Escape($seriesDirName) + '\.(\d+)$')) { $highest = [Math]::Max($highest, [int]$Matches[1]) }
        }
    }
    $number = $highest + 1
    $version = "$Series.$number"
    $eventId = 'build-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
    $artifactDir = Join-Path $repo "artifacts\builds\desktop-v$version\$eventId"
    $backupDir = Join-Path $repo "backup\builds\desktop-v$version\$eventId"
    if ((Test-Path $artifactDir) -or (Test-Path $backupDir)) { throw "Refusing to reuse $eventId" }

    # 1. build the jars
    $gradle = Join-Path $repo 'gradlew.bat'
    $args = @(':desktop:installDist')
    if ($Offline) { $args = @('--offline') + $args }
    & $gradle @args
    if ($LASTEXITCODE -ne 0) { throw "gradle :desktop:installDist failed ($LASTEXITCODE)" }
    $installRoot = Join-Path $repo 'desktop\build\install\TranslateBooks'
    if (-not (Test-Path (Join-Path $installRoot 'lib'))) { throw 'installDist produced no lib folder' }

    # 2. stage the package
    $packageName = "TranslateBooks-PC-$version"
    $stageRoot = Join-Path ([IO.Path]::GetTempPath()) ("tbl-desktop-" + [guid]::NewGuid().ToString('N'))
    $stage = Join-Path $stageRoot $packageName
    New-Item -ItemType Directory -Force -Path $stage | Out-Null
    Copy-Item -Recurse -Path (Join-Path $installRoot 'lib') -Destination (Join-Path $stage 'lib')

    # 3. runtime: jlink first (needs a jmods folder), otherwise a copy of the whole JDK folder
    $runtimeKind = 'jlink'
    $jlinkNote = ''
    $runtimeDir = Join-Path $stage 'runtime'
    $jlink = Join-Path $JavaHome 'bin\jlink.exe'
    $jlinkOk = $false
    if ((Test-Path $jlink) -and (Test-Path (Join-Path $JavaHome 'jmods'))) {
        # modules the jars really need (jdeps over every jar), plus the ones loaded by reflection or service lookup
        $jdeps = Join-Path $JavaHome 'bin\jdeps.exe'
        $libJars = @(Get-ChildItem -Path (Join-Path $stage 'lib') -Filter '*.jar' | ForEach-Object { $_.FullName })
        # sqlite-jdbc is a named module that requires slf4j (absent): jdeps cannot resolve it as a target; its needs (java.sql, java.logging) are in the fixed list below
        $targets = @($libJars | Where-Object { $_ -notmatch 'sqlite-jdbc' })
        $jdepsOut = @(& $jdeps --multi-release 21 --ignore-missing-deps --print-module-deps -cp (($libJars) -join ';') @targets 2>$null)
        # jdeps prints split-package warnings on stdout (JavaFX ships a plain and a -win jar); the module list is the last line
        $found = ($jdepsOut | Where-Object { $_ -match '^[a-z][a-z0-9_.,]*$' } | Select-Object -Last 1)
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($found)) { throw "jdeps failed ($LASTEXITCODE)" }
        $moduleSet = [System.Collections.Generic.SortedSet[string]]::new()
        foreach ($m in ($found -split ',')) { if ($m.Trim()) { [void]$moduleSet.Add($m.Trim()) } }
        foreach ($m in @('java.base', 'java.desktop', 'java.logging', 'java.naming', 'java.sql', 'java.xml', 'jdk.unsupported', 'jdk.crypto.ec')) { [void]$moduleSet.Add($m) }
        $modules = ($moduleSet -join ',')
        $jlinkNote = "modules: $modules"
        & $jlink --add-modules $modules --output $runtimeDir --strip-debug --no-header-files --no-man-pages
        $jlinkOk = ($LASTEXITCODE -eq 0) -and (Test-Path (Join-Path $runtimeDir 'bin\java.exe'))
        if (-not $jlinkOk) { $jlinkNote = "jlink exited with $LASTEXITCODE" }
        if ($jlinkOk) {
            # the window must start from the trimmed runtime: run the QA screenshot mode once with a throw-away data folder
            $smoke = Join-Path $stageRoot 'smoke'
            New-Item -ItemType Directory -Force -Path $smoke | Out-Null
            $env:TBL_DATA_DIR = Join-Path $smoke 'data'
            $env:TBL_SCREENSHOT_DIR = Join-Path $smoke 'shots'
            $proc = Start-Process -FilePath (Join-Path $runtimeDir 'bin\java.exe') -ArgumentList @('-cp', "`"$(Join-Path $stage 'lib')\*`"", 'com.ml.tblandroidtxt.desktop.DesktopLauncher') -Wait -PassThru -NoNewWindow
            Remove-Item Env:\TBL_DATA_DIR, Env:\TBL_SCREENSHOT_DIR -ErrorAction SilentlyContinue
            $shots = @(Get-ChildItem -Path (Join-Path $smoke 'shots') -Filter '*.png' -ErrorAction SilentlyContinue)
            if ($proc.ExitCode -ne 0 -or $shots.Count -lt 1) { throw "The window did not start from the jlink runtime (exit $($proc.ExitCode), $($shots.Count) screenshots)" }
            $jlinkNote += "; window started from this runtime ($($shots.Count) page screenshots)"
            Remove-Item -Recurse -Force $smoke
        }
    } else {
        $jlinkNote = 'jlink cannot build a runtime: this JDK folder has no jmods directory'
    }
    if (-not $jlinkOk) {
        if (Test-Path $runtimeDir) { Remove-Item -Recurse -Force $runtimeDir }
        $runtimeKind = 'copy-of-jdk-folder'
        Copy-Item -Recurse -Path $JavaHome -Destination $runtimeDir
    }

    $launcher = @(
        '@echo off',
        'rem Translate Books for Windows. Data: %LOCALAPPDATA%\TranslateBooks (or TBL_DATA_DIR).',
        'set "HERE=%~dp0"',
        'start "" "%HERE%runtime\bin\javaw.exe" -cp "%HERE%lib\*" com.ml.tblandroidtxt.desktop.DesktopLauncher %*'
    )
    Set-Content -Path (Join-Path $stage 'TranslateBooks.cmd') -Value $launcher -Encoding ASCII
    $readme = @"
# Translate Books for Windows $version

Run ``TranslateBooks.cmd``. No Java installation is needed: the folder ``runtime`` is the Java used.

- Runtime: $runtimeKind. $jlinkNote
- Data folder: ``%LOCALAPPDATA%\TranslateBooks`` (database, prefs, logs, catalog, encrypted API key). ``TBL_DATA_DIR`` changes it.
- The API key is stored encrypted with Windows DPAPI for the current user only; it never goes to prefs, logs or the database.
- The app makes no network call when it opens. Only the "Cập nhật danh sách model" button and translation runs use the network.
- JavaFX unpacks its native DLLs into ``%USERPROFILE%\.openjfx\cache`` on first start and prints an "unnamed module" warning; both are expected.
- Log timestamps follow the Windows locale (same formatter as the phone).
- The database schema is the phone's version 30 (``translation-core-jdbc`` resource). If the phone schema changes, regenerate that file; a database at another version is refused.
- SmartScreen: the installer and the app are NOT code-signed, so Windows shows an "unknown publisher" warning; choose "More info" then "Run anyway".
- The .msi installs per user (no administrator rights) into ``%LOCALAPPDATA%\Programs\TranslateBooks``; uninstalling never deletes ``%LOCALAPPDATA%\TranslateBooks``.
- Building needs the Android SDK (the shared core compiles against android.jar for org.json).

Built from commit $commit on $branch ($eventId).
"@
    Set-Content -Path (Join-Path $stage 'README.md') -Value $readme -Encoding UTF8

    # 4. zip, retain in both archives
    $zipPath = Join-Path $stageRoot "$packageName.zip"
    Compress-Archive -Path $stage -DestinationPath $zipPath -CompressionLevel Optimal
    $sourceZip = Join-Path $stageRoot "project_source_$eventId.zip"
    & git archive --format=zip --output=$sourceZip $commit
    if ($LASTEXITCODE -ne 0) { throw 'git archive failed' }
    $hash = (Get-FileHash $zipPath -Algorithm SHA256).Hash
    $msiPath = $null
    $msiName = ''
    if ($Msi) {
        if ($runtimeKind -ne 'jlink') { throw 'The .msi needs the jlink runtime: pass a full JDK 21 as -JavaHome.' }
        $jpackage = Join-Path $JavaHome 'bin\jpackage.exe'
        if (-not (Test-Path $jpackage)) { throw "No jpackage under $JavaHome" }
        if ([string]::IsNullOrWhiteSpace($WixHome) -or -not (Test-Path (Join-Path $WixHome 'candle.exe'))) { throw 'WiX 3.x candle.exe not found: pass -WixHome.' }
        $env:PATH = "$WixHome;$env:PATH"
        $msiOut = Join-Path $stageRoot 'msi'
        New-Item -ItemType Directory -Force -Path $msiOut | Out-Null
        $numeric = ($version -replace '[^0-9.]', '.') -replace '\.+', '.'
        $parts = @($numeric.Trim('.').Split('.') | Select-Object -First 3)
        while ($parts.Count -lt 3) { $parts += '0' }
        $appVersion = $parts -join '.'
        # Fixed upgrade GUID: a newer .msi replaces an older one. The install folder differs from the data folder on purpose
        # (data lives in %LOCALAPPDATA%\TranslateBooks and an uninstall never touches it).
        & $jpackage --type msi --name 'Translate Books' --app-version $appVersion --vendor 'Translate Books' `
            --input (Join-Path $stage 'lib') --main-jar desktop.jar --main-class com.ml.tblandroidtxt.desktop.DesktopLauncher `
            --runtime-image $runtimeDir --dest $msiOut --win-menu --win-menu-group 'Translate Books' --win-shortcut `
            --win-per-user-install --install-dir 'Programs\TranslateBooks' --win-upgrade-uuid '3f2c7a1e-5b8d-4c6e-9a41-7d2e8b0c5f13' `
            --description 'Translate Books for Windows (unsigned development build)'
        if ($LASTEXITCODE -ne 0) { throw "jpackage failed ($LASTEXITCODE)" }
        $built = Get-ChildItem -Path $msiOut -Filter '*.msi' | Select-Object -First 1
        $msiName = "TranslateBooks-PC-$version.msi"
        $msiPath = Join-Path $stageRoot $msiName
        Move-Item -Path $built.FullName -Destination $msiPath
    }
    $info = [ordered]@{
        schemaVersion = 1; product = 'TranslateBooks-PC'; version = $version; eventId = $eventId; created = (Get-Date).ToString('o')
        branch = $branch; commit = $commit; runtime = $runtimeKind; runtimeNote = $jlinkNote; javaHome = $JavaHome
        notes = $Notes; packageZip = "$packageName.zip"; packageSha256 = $hash; packageBytes = (Get-Item $zipPath).Length
        msi = $msiName; msiSha256 = $(if ($msiPath) { (Get-FileHash $msiPath -Algorithm SHA256).Hash } else { '' }); msiBytes = $(if ($msiPath) { (Get-Item $msiPath).Length } else { 0 })
    }
    $infoPath = Join-Path $stageRoot 'BUILD_INFO.json'
    $info | ConvertTo-Json | Set-Content -Path $infoPath -Encoding UTF8
    $sums = Join-Path $stageRoot 'SHA256SUMS.txt'
    $sumFiles = @($zipPath, $sourceZip, $infoPath, (Join-Path $stage 'README.md'))
    if ($msiPath) { $sumFiles += $msiPath }
    $lines = foreach ($f in $sumFiles) { '{0}  {1}' -f (Get-FileHash $f -Algorithm SHA256).Hash, (Split-Path $f -Leaf) }
    Set-Content -Path $sums -Value $lines -Encoding ASCII

    foreach ($dest in @($artifactDir, $backupDir)) {
        New-Item -ItemType Directory -Force -Path $dest | Out-Null
        Copy-Item -Path $zipPath, $sourceZip, $infoPath, $sums -Destination $dest
        Copy-Item -Path (Join-Path $stage 'README.md') -Destination $dest
        if ($msiPath) { Copy-Item -Path $msiPath -Destination $dest }
    }
    Remove-Item -Recurse -Force $stageRoot

    Write-Host "Version:          $version"
    Write-Host "Runtime:          $runtimeKind ($jlinkNote)"
    Write-Host "Artifact archive: $artifactDir"
    Write-Host "Backup archive:   $backupDir"
    Write-Host "SHA-256:          $hash"
    if ($msiPath) { Write-Host "MSI SHA-256:      $((Get-FileHash $msiPath -Algorithm SHA256).Hash) ($((Get-Item $msiPath).Length) bytes)" }
}
finally {
    Pop-Location
}
