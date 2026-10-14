param(
    [string]$EvidenceRoot = 'D:\P5E-private\v422-q1',
    [string]$Baseline = '9113523a40f808a9be4bdbc7d0e078ab337d1b95',
    [string]$Jdk = 'C:\Program Files\Android\Android Studio\jbr'
)
$ErrorActionPreference = 'Stop'
$q1Repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$q1Root = [IO.Path]::GetFullPath($EvidenceRoot)
if (-not ($q1Root.Equals('D:\P5E-private\v422-q1',[StringComparison]::OrdinalIgnoreCase) -or $q1Root.StartsWith('D:\P5E-private\v422-q1\', [StringComparison]::OrdinalIgnoreCase))) { throw 'Q1 private evidence root required' }
$q1Event = Join-Path $q1Root ('offline-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $q1Event | Out-Null
$q1Zip = Join-Path $q1Event 'baseline-engine-source.zip'
git -C $q1Repo archive --format=zip --output=$q1Zip $Baseline editorial-engine/src/main
if ($LASTEXITCODE -ne 0) { throw 'baseline archive failed' }
Expand-Archive -LiteralPath $q1Zip -DestinationPath (Join-Path $q1Event 'source')
$q1Classes = Join-Path $q1Event 'classes'
New-Item -ItemType Directory -Path $q1Classes | Out-Null
$q1Sources = @(Get-ChildItem (Join-Path $q1Event 'source\editorial-engine\src\main') -Filter *.java -Recurse | ForEach-Object FullName)
$q1Sources += @(Get-ChildItem $PSScriptRoot -Filter *.java | ForEach-Object FullName)
Copy-Item -LiteralPath $PSScriptRoot -Destination (Join-Path $q1Event 'sidecar-source') -Recurse
& (Join-Path $Jdk 'bin\javac.exe') -encoding UTF-8 --release 17 -d $q1Classes @q1Sources 2>&1 | Tee-Object -FilePath (Join-Path $q1Event 'compile.log')
if ($LASTEXITCODE -ne 0) { throw 'Q1 compile failed' }
& (Join-Path $Jdk 'bin\java.exe') -cp $q1Classes q1.Q1SpeakerSyntheticTest 2>&1 | Tee-Object -FilePath (Join-Path $q1Event 'synthetic.log')
if ($LASTEXITCODE -ne 0) { throw 'Q1 synthetic failed' }
if (Test-Path (Join-Path $q1Root 'reference-Kumo013.tsv')) {
    & (Join-Path $Jdk 'bin\java.exe') -cp $q1Classes q1.Q1SpeakerMeasurement $q1Root $q1Event 2>&1 | Tee-Object -FilePath (Join-Path $q1Event 'measurement.log')
    if ($LASTEXITCODE -ne 0) { throw 'Q1 measurement failed' }
}
Get-ChildItem $q1Root -File -Filter '*.tsv' | Copy-Item -Destination $q1Event
Get-ChildItem $q1Event -File -Recurse | ForEach-Object {
    (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash + '  ' + $_.FullName.Substring($q1Event.Length).TrimStart([char]92)
} | Set-Content -LiteralPath (Join-Path $q1Event 'SHA256SUMS.txt') -Encoding utf8
Write-Output "Q1 evidence: $q1Event"
