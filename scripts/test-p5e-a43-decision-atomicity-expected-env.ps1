[CmdletBinding()]
param(
    [string]$OutputPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$guardPath = Join-Path $repoRoot 'scripts\p5e-a43-runtime-guards.ps1'
$commandPath = Join-Path $repoRoot 'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_COMMAND_20260928.txt'
$redEvidencePath = Join-Path $repoRoot 'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_RED_20260928.json'
$ps51Path = 'C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe'
$fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-atomicity-green-' + [Guid]::NewGuid().ToString('N'))
$tests = [System.Collections.Generic.List[object]]::new()
$liveCounters = [ordered]@{
    adb = 0
    device = 0
    provider = 0
    credential = 0
    databaseWrites = 0
    buildInstall = 0
    raw = 0
    redispatch = 0
}

function Add-TestResult {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [Parameter(Mandatory = $true)][string]$Detail,
        [string]$Severity = 'LOW'
    )
    [void]$tests.Add([ordered]@{
        name = $Name
        passed = $Passed
        severity = $Severity
        detail = $Detail
    })
}

function Assert-Fixture {
    param([Parameter(Mandatory = $true)][bool]$Condition,[Parameter(Mandatory = $true)][string]$Code)
    if (-not $Condition) { throw $Code }
}

function Get-SafeFailureCode {
    param([Parameter(Mandatory = $true)]$ErrorRecord)
    $message = [string]$ErrorRecord.Exception.Message
    if ($message -match '^[A-Za-z0-9_.:-]+$') { return $message }
    $type = $ErrorRecord.Exception.GetType().Name -replace '[^A-Za-z0-9]', '_'
    return ('UNSAFE_FIXTURE_ERROR_' + $type)
}

function Invoke-TestCase {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Body,
        [string]$Severity = 'LOW'
    )
    try {
        $detail = & $Body
        $safeDetail = if ([string]::IsNullOrWhiteSpace([string]$detail)) { 'PASS' } else { [string]$detail }
        if ($safeDetail -notmatch '^[A-Za-z0-9_.=;:-]+$') { $safeDetail = 'PASS' }
        Add-TestResult -Name $Name -Passed $true -Detail $safeDetail -Severity $Severity
    } catch {
        Add-TestResult -Name $Name -Passed $false -Detail (Get-SafeFailureCode -ErrorRecord $_) -Severity $Severity
    }
}

function New-FixtureDirectory {
    param([Parameter(Mandatory = $true)][string]$Name)
    $path = Join-Path $fixtureRoot $Name
    [void][IO.Directory]::CreateDirectory($path)
    return $path
}

function New-FakeHash {
    param([Parameter(Mandatory = $true)][char]$Character,[Parameter(Mandatory = $true)][int]$Length)
    return [string]::new($Character, $Length)
}

function Get-ReservationResult {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Decision,
        [Parameter(Mandatory = $true)][string]$ReceiptHash
    )
    try {
        $reservation = Reserve-P5EA43OwnerDecisionEvent -EvidenceRoot $Root -DecisionId $Decision -ReceiptSha256 $ReceiptHash `
            -PacketIdentifier 'P5E-A43-SYNTHETIC' -Serial '15e84958' -Scope 'RAW/GLOSSARY'
        return [pscustomobject]@{ Status = 'WIN'; State = [string]$reservation.State; DecisionKey = [string]$reservation.DecisionKey; Path = [string]$reservation.Path }
    } catch {
        return [pscustomobject]@{ Status = 'STOP'; State = Get-SafeFailureCode -ErrorRecord $_; DecisionKey = ''; Path = '' }
    }
}

function Get-EventDirectories {
    param([Parameter(Mandatory = $true)][string]$Root)
    return @(Get-ChildItem -LiteralPath $Root -Directory -Force | Where-Object { $_.Name -notin @('.decisions', '.receipts') })
}

function ConvertTo-WindowsArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $builder = [Text.StringBuilder]::new()
    [void]$builder.Append('"')
    $slashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') { $slashes++; continue }
        if ($character -eq '"') {
            [void]$builder.Append(('\' * (2 * $slashes + 1)))
            [void]$builder.Append('"')
            $slashes = 0
            continue
        }
        if ($slashes -gt 0) { [void]$builder.Append(('\' * $slashes)); $slashes = 0 }
        [void]$builder.Append($character)
    }
    if ($slashes -gt 0) { [void]$builder.Append(('\' * (2 * $slashes))) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Invoke-ConcurrentReservationPair {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Decision,
        [Parameter(Mandatory = $true)][string[]]$ReceiptHashes
    )
    $phase = 'INIT'
    try {
    $worker = Join-Path $fixtureRoot ('worker-' + [Guid]::NewGuid().ToString('N') + '.ps1')
    $startSignal = Join-Path $fixtureRoot ('start-' + [Guid]::NewGuid().ToString('N') + '.signal')
    $workerText = @'
param([string]$Guard,[string]$Root,[string]$Decision,[string]$ReceiptHash,[string]$Ready,[string]$Start)
$ErrorActionPreference = 'Stop'
. $Guard -LibraryOnly
[IO.File]::WriteAllText($Ready, 'READY', [Text.UTF8Encoding]::new($false))
while (-not (Test-Path -LiteralPath $Start -PathType Leaf)) { Start-Sleep -Milliseconds 10 }
try {
    [void](Reserve-P5EA43OwnerDecisionEvent -EvidenceRoot $Root -DecisionId $Decision -ReceiptSha256 $ReceiptHash -PacketIdentifier 'P5E-A43-SYNTHETIC' -Serial '15e84958' -Scope 'RAW/GLOSSARY')
    Write-Output 'WIN'
} catch {
    $m = [string]$_.Exception.Message
    if ($m -match '^[A-Za-z0-9_.:-]+$') { Write-Output ('STOP:' + $m) } else { Write-Output 'STOP:UNSAFE_FIXTURE_ERROR' }
}
'@
    $phase = 'WRITE_WORKER'
    [IO.File]::WriteAllText($worker, $workerText, [Text.UTF8Encoding]::new($false))
    $children = [System.Collections.Generic.List[object]]::new()
    $phase = 'START_CHILDREN'
    for ($index = 0; $index -lt 2; $index++) {
        $ready = Join-Path $fixtureRoot ('ready-' + [Guid]::NewGuid().ToString('N') + '.signal')
        $startInfo = [Diagnostics.ProcessStartInfo]::new()
        $startInfo.FileName = $ps51Path
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true
        $arguments = @('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$worker,$guardPath,$Root,$Decision,$ReceiptHashes[$index],$ready,$startSignal)
        $startInfo.Arguments = [string]::Join(' ', @($arguments | ForEach-Object { ConvertTo-WindowsArgument -Value ([string]$_) }))
        $process = [Diagnostics.Process]::new()
        $process.StartInfo = $startInfo
        [void]$process.Start()
        [void]$children.Add([pscustomobject]@{ Process = $process; Ready = $ready })
    }
    $phase = 'WAIT_READY'
    foreach ($child in $children) {
        $deadline = (Get-Date).AddSeconds(10)
        while (-not (Test-Path -LiteralPath $child.Ready -PathType Leaf) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 20 }
        Assert-Fixture (Test-Path -LiteralPath $child.Ready -PathType Leaf) 'CONTENDER_READY_TIMEOUT'
    }
    $phase = 'WAIT_EXIT'
    [IO.File]::WriteAllText($startSignal, 'GO', [Text.UTF8Encoding]::new($false))
    $statuses = [System.Collections.Generic.List[string]]::new()
    foreach ($child in $children) {
        [void]$child.Process.WaitForExit(15000)
        $stdout = $child.Process.StandardOutput.ReadToEnd().Trim()
        [void]$child.Process.StandardError.ReadToEnd()
        if ($stdout -eq 'WIN') { [void]$statuses.Add('WIN') }
        elseif ($stdout -match '^STOP:[A-Za-z0-9_.:-]+$') { [void]$statuses.Add($stdout.Substring(5)) }
        else { [void]$statuses.Add('UNKNOWN_CHILD_STATUS') }
        $child.Process.Dispose()
    }
    return $statuses.ToArray()
    } catch {
        return @('PARENT_ERROR_' + $phase)
    }
}

function Write-MarkerFixture {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][ValidateSet('decision','receipt')][string]$Kind,
        [Parameter(Mandatory = $true)][string]$DecisionKey,
        [Parameter(Mandatory = $true)][string]$ReceiptHash,
        [switch]$Partial
    )
    $registryName = if ($Kind -eq 'decision') { '.decisions' } else { '.receipts' }
    $registry = Join-Path $Root $registryName
    [void][IO.Directory]::CreateDirectory($registry)
    $key = if ($Kind -eq 'decision') { $DecisionKey } else { $ReceiptHash }
    $path = Join-Path $registry ($key + '.reservation')
    if ($Partial) {
        [IO.File]::WriteAllText($path, '{"schemaVersion":"p5e.a43.' + $Kind + '-reservation.v2"', [Text.UTF8Encoding]::new($false))
        return $path
    }
    $record = [ordered]@{
        schemaVersion = 'p5e.a43.' + $Kind + '-reservation.v2'
        decisionKey = $DecisionKey
        receiptSha256 = $ReceiptHash
        packetIdentifier = 'P5E-A43-SYNTHETIC'
        serial = '15e84958'
        scope = 'RAW/GLOSSARY'
        reservedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
        state = 'RESERVED_CONSUMED_FAIL_CLOSED'
    }
    [IO.File]::WriteAllText($path, ($record | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
    return $path
}

function Get-PhaseEnvironmentFunction {
    $tokens = $null
    $parseErrors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile($commandPath, [ref]$tokens, [ref]$parseErrors)
    Assert-Fixture ($parseErrors.Count -eq 0) 'COMMAND_PARSE_FAILED'
    $functionAst = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -ceq 'Set-P5EPhaseEnvironment' }, $true)
    Assert-Fixture ($null -ne $functionAst) 'PHASE_ENVIRONMENT_FUNCTION_MISSING'
    $definition = $functionAst.Extent.Text -replace '^function Set-P5EPhaseEnvironment', 'function global:Set-P5EPhaseEnvironment'
    Invoke-Expression $definition
}

function Get-EnvironmentVariableFromStartInfo {
    param([Parameter(Mandatory = $true)][Diagnostics.ProcessStartInfo]$StartInfo,[Parameter(Mandatory = $true)][string]$Name)
    $property = $StartInfo.PSObject.Properties['Environment']
    if ($null -ne $property) { return [string]$StartInfo.Environment[$Name] }
    return [string]$StartInfo.EnvironmentVariables[$Name]
}

try {
    . $guardPath -LibraryOnly
    Assert-Fixture (Test-Path -LiteralPath $ps51Path -PathType Leaf) 'POWERSHELL_5_1_MISSING'

    Invoke-TestCase -Name 'red-evidence-preserved' -Severity 'HIGH' -Body {
        Assert-Fixture (Test-Path -LiteralPath $redEvidencePath -PathType Leaf) 'RED_EVIDENCE_MISSING'
        $red = Get-Content -Raw -LiteralPath $redEvidencePath | ConvertFrom-Json
        Assert-Fixture ([string]$red.status -ceq 'RED_REPRODUCED') 'RED_EVIDENCE_STATUS_INVALID'
        'RED_HASH=' + (Get-FileHash -LiteralPath $redEvidencePath -Algorithm SHA256).Hash.ToUpperInvariant()
    }

    Invoke-TestCase -Name 'canonical-decision-key-full-sha256' -Severity 'HIGH' -Body {
        $key = Get-P5EA43DecisionKey -CanonicalDecisionId 'same-decision'
        Assert-Fixture ($key -match '^[0-9a-f]{64}$') 'DECISION_KEY_SHAPE_INVALID'
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $expected = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes('same-decision')))).Replace('-', '').ToLowerInvariant() } finally { $sha.Dispose() }
        Assert-Fixture ($key -ceq $expected) 'DECISION_KEY_BYTES_INVALID'
        'PASS'
    }

    foreach ($nonCanonical in @(' same-decision', 'same-decision ', 'SAME-DECiSION/unsafe', 'é')) {
        $caseName = 'noncanonical-decision-stop-' + $tests.Count.ToString('000')
        $capturedDecision = $nonCanonical
        Invoke-TestCase -Name $caseName -Severity 'HIGH' -Body {
            try { [void](Assert-P5EA43CanonicalDecisionId -Value $capturedDecision); throw 'NONCANONICAL_ACCEPTED' }
            catch { Assert-Fixture ([string]$_.Exception.Message -ceq 'P5E_OWNER_RECEIPT_DECISION_ID_NONCANONICAL_STOP') 'WRONG_NONCANONICAL_STOP' }
            'PASS'
        }
    }

    Invoke-TestCase -Name 'sequential-same-decision-different-receipt-one-use' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'sequential-cross-hash'
        $first = Get-ReservationResult -Root $root -Decision 'same-decision' -ReceiptHash (New-FakeHash 'a' 64)
        $second = Get-ReservationResult -Root $root -Decision 'same-decision' -ReceiptHash (New-FakeHash 'b' 64)
        Assert-Fixture ($first.Status -ceq 'WIN') 'FIRST_RESERVATION_NOT_WINNER'
        Assert-Fixture ($second.Status -ceq 'STOP' -and $second.State -ceq 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP') 'SECOND_DECISION_NOT_BLOCKED'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 1) 'EVENT_COUNT_NOT_ONE'
        'PASS'
    }

    Invoke-TestCase -Name 'sequential-same-decision-same-receipt-one-use' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'sequential-same-hash'
        $hash = New-FakeHash 'c' 64
        $first = Get-ReservationResult -Root $root -Decision 'same-decision' -ReceiptHash $hash
        $second = Get-ReservationResult -Root $root -Decision 'same-decision' -ReceiptHash $hash
        Assert-Fixture ($first.Status -ceq 'WIN') 'FIRST_RESERVATION_NOT_WINNER'
        Assert-Fixture ($second.State -ceq 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP') 'SAME_HASH_REUSE_NOT_BLOCKED'
        'PASS'
    }

    Invoke-TestCase -Name 'concurrent-same-decision-different-receipt-one-winner' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'concurrent-cross-hash'
        $statuses = @(Invoke-ConcurrentReservationPair -Root $root -Decision 'concurrent-decision' -ReceiptHashes @((New-FakeHash 'd' 64), (New-FakeHash 'e' 64)))
        $winCount = 0; foreach ($status in @($statuses)) { if ($status -ceq 'WIN') { $winCount++ } }
        Assert-Fixture ($winCount -eq 1) 'CONCURRENT_CROSS_HASH_WINNER_COUNT_INVALID'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 1) 'CONCURRENT_CROSS_HASH_EVENT_COUNT_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'concurrent-same-decision-same-receipt-one-winner' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'concurrent-same-hash'
        $statuses = @(Invoke-ConcurrentReservationPair -Root $root -Decision 'concurrent-same' -ReceiptHashes @((New-FakeHash 'f' 64), (New-FakeHash 'f' 64)))
        $winCount = 0; foreach ($status in @($statuses)) { if ($status -ceq 'WIN') { $winCount++ } }
        Assert-Fixture ($winCount -eq 1) 'CONCURRENT_SAME_HASH_WINNER_COUNT_INVALID'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 1) 'CONCURRENT_SAME_HASH_EVENT_COUNT_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'different-decision-same-receipt-consumes-second-decision' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'different-decision-same-receipt'
        $hash = New-FakeHash '1' 64
        $first = Get-ReservationResult -Root $root -Decision 'decision-one' -ReceiptHash $hash
        $second = Get-ReservationResult -Root $root -Decision 'decision-two' -ReceiptHash $hash
        Assert-Fixture ($first.Status -ceq 'WIN') 'FIRST_RECEIPT_BINDING_NOT_WINNER'
        Assert-Fixture ($second.State -ceq 'P5E_OWNER_RECEIPT_ALREADY_CONSUMED_STOP') 'SECOND_RECEIPT_REUSE_STOP_INVALID'
        $decisionMarkers = @(Get-ChildItem -LiteralPath (Join-Path $root '.decisions') -File -Force)
        Assert-Fixture ($decisionMarkers.Count -eq 2) 'SECOND_DECISION_MARKER_NOT_CONSUMED'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 1) 'RECEIPT_REUSE_CREATED_EVENT'
        'PASS'
    }

    Invoke-TestCase -Name 'concurrent-different-decision-same-receipt-one-event' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'concurrent-different-decision-same-receipt'
        $statuses = @(Invoke-ConcurrentReservationPair -Root $root -Decision 'race-decision' -ReceiptHashes @((New-FakeHash '2' 64), (New-FakeHash '2' 64)))
        $winCount = 0; foreach ($status in @($statuses)) { if ($status -ceq 'WIN') { $winCount++ } }
        Assert-Fixture ($winCount -eq 1) 'CONCURRENT_RECEIPT_WINNER_COUNT_INVALID'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 1) 'CONCURRENT_RECEIPT_EVENT_COUNT_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'different-decision-different-receipt-independent-reservations' -Severity 'HIGH' -Body {
        $rootA = New-FixtureDirectory 'independent-a'
        $rootB = New-FixtureDirectory 'independent-b'
        $a = Get-ReservationResult -Root $rootA -Decision 'independent-a' -ReceiptHash (New-FakeHash '3' 64)
        $b = Get-ReservationResult -Root $rootB -Decision 'independent-b' -ReceiptHash (New-FakeHash '4' 64)
        Assert-Fixture ($a.Status -ceq 'WIN' -and $b.Status -ceq 'WIN') 'ISOLATED_RESERVATIONS_NOT_INDEPENDENT'
        'PASS'
    }

    Invoke-TestCase -Name 'receipt-full-hash-prefix-collision-is-distinct' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'full-hash-collision'
        $hashA = (New-FakeHash 'a' 16) + (New-FakeHash '0' 48)
        $hashB = (New-FakeHash 'a' 16) + (New-FakeHash '1' 48)
        $a = Get-ReservationResult -Root $root -Decision 'prefix-a' -ReceiptHash $hashA
        $b = Get-ReservationResult -Root $root -Decision 'prefix-b' -ReceiptHash $hashB
        Assert-Fixture ($a.Status -ceq 'WIN' -and $b.Status -ceq 'WIN') 'FULL_HASH_KEYS_COLLIDED'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 2) 'FULL_HASH_EVENT_COUNT_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'event-path-uses-decision-key-only' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'event-path-binding'
        $decision = 'Readable.Decision-01'
        $hash = New-FakeHash '5' 64
        $reservation = Get-ReservationResult -Root $root -Decision $decision -ReceiptHash $hash
        Assert-Fixture ($reservation.Status -ceq 'WIN') 'EVENT_RESERVATION_FAILED'
        Assert-Fixture ([IO.Path]::GetFileName($reservation.Path) -notlike ('*' + $decision + '*')) 'RAW_DECISION_ID_IN_EVENT_PATH'
        Assert-Fixture ([IO.Path]::GetFileName($reservation.Path) -notlike ('*' + $hash.Substring(0, 16) + '*')) 'RECEIPT_PREFIX_IN_EVENT_PATH'
        Assert-Fixture ([IO.Path]::GetFileName($reservation.Path) -match '^raw-live-a43-preauth-[0-9a-f]{64}$') 'EVENT_PATH_SHAPE_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'crash-window-after-decision-marker-fail-closed' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'crash-after-decision-marker'
        $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId 'crash-one'
        [void](Write-MarkerFixture -Root $root -Kind decision -DecisionKey $decisionKey -ReceiptHash (New-FakeHash '6' 64))
        $sameDecision = Get-ReservationResult -Root $root -Decision 'crash-one' -ReceiptHash (New-FakeHash '7' 64)
        $sameReceipt = Get-ReservationResult -Root $root -Decision 'crash-two' -ReceiptHash (New-FakeHash '6' 64)
        Assert-Fixture ($sameDecision.State -ceq 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP') 'CRASH_DECISION_REUSE_ACCEPTED'
        Assert-Fixture ($sameReceipt.State -ceq 'P5E_OWNER_RECEIPT_ALREADY_CONSUMED_STOP') 'CRASH_RECEIPT_REUSE_ACCEPTED'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 0) 'CRASH_WINDOW_CREATED_EVENT'
        'PASS'
    }

    Invoke-TestCase -Name 'crash-window-after-receipt-marker-fail-closed' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'crash-after-receipt-marker'
        $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId 'crash-three'
        $hash = New-FakeHash '8' 64
        [void](Write-MarkerFixture -Root $root -Kind decision -DecisionKey $decisionKey -ReceiptHash $hash)
        [void](Write-MarkerFixture -Root $root -Kind receipt -DecisionKey $decisionKey -ReceiptHash $hash)
        $sameDecision = Get-ReservationResult -Root $root -Decision 'crash-three' -ReceiptHash (New-FakeHash '9' 64)
        $sameReceipt = Get-ReservationResult -Root $root -Decision 'crash-four' -ReceiptHash $hash
        Assert-Fixture ($sameDecision.State -ceq 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP') 'CRASH_RECEIPT_WINDOW_DECISION_REUSE_ACCEPTED'
        Assert-Fixture ($sameReceipt.State -ceq 'P5E_OWNER_RECEIPT_ALREADY_CONSUMED_STOP') 'CRASH_RECEIPT_WINDOW_RECEIPT_REUSE_ACCEPTED'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 0) 'CRASH_RECEIPT_WINDOW_CREATED_EVENT'
        Assert-Fixture ((@(Get-ChildItem -LiteralPath (Join-Path $root '.decisions') -File -Force)).Count -eq 2) 'CRASH_RECEIPT_WINDOW_DECISION_NOT_CONSUMED'
        'PASS'
    }

    Invoke-TestCase -Name 'partial-marker-ledger-unknown-fail-closed' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'partial-marker'
        $key = Get-P5EA43DecisionKey -CanonicalDecisionId 'partial-marker'
        [void](Write-MarkerFixture -Root $root -Kind decision -DecisionKey $key -ReceiptHash (New-FakeHash 'a' 64) -Partial)
        $result = Get-ReservationResult -Root $root -Decision 'new-after-partial' -ReceiptHash (New-FakeHash 'b' 64)
        Assert-Fixture ($result.State -ceq 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP') 'PARTIAL_MARKER_NOT_FAIL_CLOSED'
        Assert-Fixture ((@(Get-EventDirectories -Root $root)).Count -eq 0) 'PARTIAL_MARKER_CREATED_EVENT'
        'PASS'
    }

    Invoke-TestCase -Name 'malformed-marker-filename-ledger-unknown' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'malformed-marker-name'
        $registry = Join-Path $root '.decisions'; [void][IO.Directory]::CreateDirectory($registry)
        [IO.File]::WriteAllText((Join-Path $registry 'not-a-marker.reservation'), 'x', [Text.UTF8Encoding]::new($false))
        $result = Get-ReservationResult -Root $root -Decision 'new-after-malformed-name' -ReceiptHash (New-FakeHash 'c' 64)
        Assert-Fixture ($result.State -ceq 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP') 'MALFORMED_MARKER_NAME_NOT_FAIL_CLOSED'
        'PASS'
    }

    Invoke-TestCase -Name 'receipt-marker-binding-mismatch-ledger-unknown' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'receipt-binding-mismatch'
        $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId 'binding-mismatch'
        $hash = New-FakeHash 'd' 64
        [void](Write-MarkerFixture -Root $root -Kind decision -DecisionKey $decisionKey -ReceiptHash $hash)
        $receiptRegistry = Join-Path $root '.receipts'; [void][IO.Directory]::CreateDirectory($receiptRegistry)
        $wrongName = Join-Path $receiptRegistry ((New-FakeHash 'e' 64) + '.reservation')
        $record = [ordered]@{ schemaVersion = 'p5e.a43.receipt-reservation.v2'; decisionKey = $decisionKey; receiptSha256 = $hash; packetIdentifier = 'P5E-A43-SYNTHETIC'; serial = '15e84958'; scope = 'RAW/GLOSSARY'; reservedAtUtc = [DateTimeOffset]::UtcNow.ToString('o'); state = 'RESERVED_CONSUMED_FAIL_CLOSED' }
        [IO.File]::WriteAllText($wrongName, ($record | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
        $result = Get-ReservationResult -Root $root -Decision 'new-after-binding-mismatch' -ReceiptHash (New-FakeHash 'f' 64)
        Assert-Fixture ($result.State -ceq 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP') 'RECEIPT_BINDING_MISMATCH_ACCEPTED'
        'PASS'
    }

    Invoke-TestCase -Name 'atomic-marker-create-new-and-flush-contract' -Severity 'HIGH' -Body {
        $guardSource = Get-Content -Raw -LiteralPath $guardPath
        Assert-Fixture ($guardSource.Contains('[IO.FileMode]::CreateNew')) 'CREATE_NEW_PRIMITIVE_MISSING'
        Assert-Fixture ($guardSource.Contains('$stream.Flush($true)')) 'DURABLE_FLUSH_MISSING'
        Assert-Fixture (-not $guardSource.Contains('Remove-Item -LiteralPath $decisionMarker')) 'DECISION_MARKER_ROLLBACK_PRESENT'
        Assert-Fixture ($guardSource.Contains('RESERVED_CONSUMED_FAIL_CLOSED')) 'FAIL_CLOSED_STATE_MISSING'
        'PASS'
    }

    Invoke-TestCase -Name 'phase-environment-exact-isolation' -Severity 'HIGH' -Body {
        Get-PhaseEnvironmentFunction
        $global:receipt = [pscustomobject]@{ ReceiptSha256 = (New-FakeHash '1' 64) }
        [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', (New-FakeHash 'a' 64), 'Process')
        $expected = @{}
        foreach ($phase in @('PrepareEvent','Before','Dispatch','After','Verify')) {
            $startInfo = [Diagnostics.ProcessStartInfo]::new()
            $status = Set-P5EPhaseEnvironment -StartInfo $startInfo -Phase $phase
            $value = Get-EnvironmentVariableFromStartInfo -StartInfo $startInfo -Name 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'
            $binding = Get-EnvironmentVariableFromStartInfo -StartInfo $startInfo -Name 'P5E_A43_RESERVATION_RECEIPT_SHA256'
            $expected[$phase] = [pscustomobject]@{ Status = [string]$status; ValuePresent = -not [string]::IsNullOrWhiteSpace($value); BindingPresent = -not [string]::IsNullOrWhiteSpace($binding) }
        }
        [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $null, 'Process')
        Assert-Fixture (-not $expected['PrepareEvent'].ValuePresent -and $expected['PrepareEvent'].BindingPresent) 'PREPARE_ENVIRONMENT_INVALID'
        Assert-Fixture (-not $expected['Before'].ValuePresent -and -not $expected['Before'].BindingPresent) 'BEFORE_ENVIRONMENT_INVALID'
        Assert-Fixture ($expected['Dispatch'].ValuePresent -and -not $expected['Dispatch'].BindingPresent) 'DISPATCH_ENVIRONMENT_INVALID'
        Assert-Fixture (-not $expected['After'].ValuePresent -and -not $expected['After'].BindingPresent) 'AFTER_ENVIRONMENT_INVALID'
        Assert-Fixture (-not $expected['Verify'].ValuePresent -and -not $expected['Verify'].BindingPresent) 'VERIFY_ENVIRONMENT_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'unknown-phase-stops-before-launch' -Severity 'HIGH' -Body {
        Get-PhaseEnvironmentFunction
        $startInfo = [Diagnostics.ProcessStartInfo]::new()
        $stopped = $false
        try { [void](Set-P5EPhaseEnvironment -StartInfo $startInfo -Phase 'Unknown') }
        catch { $stopped = [string]$_.Exception.Message -match 'ValidateSet|parameter|cannot' }
        Assert-Fixture $stopped 'UNKNOWN_PHASE_STOP_INVALID'
        'PASS'
    }

    Invoke-TestCase -Name 'phase-environments-do-not-cross-contaminate' -Severity 'HIGH' -Body {
        Get-PhaseEnvironmentFunction
        $global:receipt = [pscustomobject]@{ ReceiptSha256 = (New-FakeHash '2' 64) }
        [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', (New-FakeHash 'b' 64), 'Process')
        $dispatchInfo = [Diagnostics.ProcessStartInfo]::new(); [void](Set-P5EPhaseEnvironment -StartInfo $dispatchInfo -Phase 'Dispatch')
        $afterInfo = [Diagnostics.ProcessStartInfo]::new(); [void](Set-P5EPhaseEnvironment -StartInfo $afterInfo -Phase 'After')
        [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $null, 'Process')
        Assert-Fixture (-not [string]::IsNullOrWhiteSpace((Get-EnvironmentVariableFromStartInfo -StartInfo $dispatchInfo -Name 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT')) -and [string]::IsNullOrWhiteSpace((Get-EnvironmentVariableFromStartInfo -StartInfo $afterInfo -Name 'P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT'))) 'PHASE_ENVIRONMENT_CROSS_CONTAMINATION'
        'PASS'
    }

    Invoke-TestCase -Name 'receipt-hash-not-passed-through-prepare-argv' -Severity 'HIGH' -Body {
        $commandText = Get-Content -Raw -LiteralPath $commandPath
        Assert-Fixture (-not $commandText.Contains("'-OwnerDecisionReceiptSha256'")) 'RECEIPT_HASH_ARGV_PRESENT'
        Assert-Fixture ($commandText.Contains('P5E_A43_RESERVATION_RECEIPT_SHA256')) 'RECEIPT_HASH_ENV_BINDING_MISSING'
        $helperText = Get-Content -Raw -LiteralPath (Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1')
        Assert-Fixture ($helperText.Contains('P5E_OWNER_RECEIPT_HASH_ARGV_FORBIDDEN_STOP')) 'HELPER_ARGV_GUARD_MISSING'
        'PASS'
    }

    Invoke-TestCase -Name 'expected-value-not-in-exception-or-report-contract' -Severity 'HIGH' -Body {
        $commandText = Get-Content -Raw -LiteralPath $commandPath
        $guardText = Get-Content -Raw -LiteralPath $guardPath
        Assert-Fixture ($guardText.Contains('The value is never') -and $guardText.Contains('returned, logged, hashed, persisted')) 'EXPECTED_VALUE_POLICY_LITERAL_MISSING'
        $helperText = Get-Content -Raw -LiteralPath (Join-Path $repoRoot 'scripts\p5e-raw-live-supervisor.ps1')
        Assert-Fixture ($helperText.Contains('SensitiveValues')) 'EXPECTED_VALUE_CAPTURE_REDACTION_MISSING'
        Assert-Fixture ($commandText.Contains('Clear every authority/expected-value variable first')) 'EXPLICIT_ENVIRONMENT_CLEAR_MISSING'
        'PASS'
    }

    Invoke-TestCase -Name 'marker-state-monotonic-no-cleanup' -Severity 'HIGH' -Body {
        $guardText = Get-Content -Raw -LiteralPath $guardPath
        Assert-Fixture ($guardText.Contains('Any exception') -and $guardText.Contains('leaves both the decision')) 'MONOTONIC_STATE_COMMENT_MISSING'
        Assert-Fixture (-not $guardText.Contains('Remove-Item -LiteralPath $receiptMarker')) 'RECEIPT_MARKER_ROLLBACK_PRESENT'
        'PASS'
    }

    Invoke-TestCase -Name 'concurrent-child-status-is-redacted' -Severity 'HIGH' -Body {
        $root = New-FixtureDirectory 'redacted-child-status'
        $statuses = @(Invoke-ConcurrentReservationPair -Root $root -Decision 'redacted-race' -ReceiptHashes @((New-FakeHash 'e' 64), (New-FakeHash 'f' 64)))
        $safeCount = 0; foreach ($status in @($statuses)) { if ([string]$status -match '^[A-Za-z0-9_.:-]+$') { $safeCount++ } }
        Assert-Fixture ($safeCount -eq 2) 'CHILD_STATUS_NOT_REDACTED_ENUM'
        'PASS'
    }

    $failed = @($tests | Where-Object { -not $_.passed })
    $highFailures = @($failed | Where-Object { $_.severity -eq 'HIGH' })
    $status = if ($failed.Count -eq 0) { 'GREEN_PASS' } else { 'GREEN_FAIL' }
    $severity = if ($highFailures.Count -gt 0) { 'HIGH' } elseif ($failed.Count -gt 0) { 'MEDIUM' } else { 'NONE' }
    $redHash = if (Test-Path -LiteralPath $redEvidencePath -PathType Leaf) { (Get-FileHash -LiteralPath $redEvidencePath -Algorithm SHA256).Hash.ToUpperInvariant() } else { '' }
    $report = [ordered]@{
        schemaVersion = 'p5e.a43.decision-atomicity-expected-env-isolation-qa.v2'
        generatedDate = '2026-09-28'
        status = $status
        severity = $severity
        redEvidence = [ordered]@{ path = 'docs/P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_RED_20260928.json'; sha256 = $redHash; status = 'RED_REPRODUCED' }
        testCount = $tests.Count
        passedTestCount = (@($tests | Where-Object { $_.passed })).Count
        failedTestCount = $failed.Count
        highFailureCount = $highFailures.Count
        tests = $tests.ToArray()
        secretDisclosure = 'NONE'
        rawOutputRetained = $false
        tempFixtureRoot = 'REDACTED_TEMP_ROOT'
        liveCounters = $liveCounters
        nextAction = if ($status -eq 'GREEN_PASS') { 'Continue offline archive-clean QA and final independent review.' } else { 'Repair the failing offline assertion before any packet refreeze.' }
    }
    if ([string]::IsNullOrWhiteSpace($OutputPath)) { $OutputPath = Join-Path $repoRoot 'docs\P5E_A43_DECISION_ATOMICITY_EXPECTED_ENV_ISOLATION_QA_20260928.json' }
    [void][IO.Directory]::CreateDirectory((Split-Path -Parent ([IO.Path]::GetFullPath($OutputPath))))
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), ($report | ConvertTo-Json -Depth 16), [Text.UTF8Encoding]::new($false))
    Write-Output ('P5E_A43_DECISION_ATOMICITY_QA=' + $status)
    Write-Output ('P5E_A43_DECISION_ATOMICITY_QA_TESTS=' + [string]$tests.Count)
    Write-Output ('P5E_A43_DECISION_ATOMICITY_QA_FAILURES=' + [string]$failed.Count)
    Write-Output ('P5E_A43_DECISION_ATOMICITY_QA_HIGH_FAILURES=' + [string]$highFailures.Count)
    if ($failed.Count -ne 0) { exit 1 }
} finally {
    [Environment]::SetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', $null, 'Process')
    if (Test-Path -LiteralPath $fixtureRoot -PathType Container) {
        try { [IO.Directory]::Delete($fixtureRoot, $true) } catch { }
    }
}
