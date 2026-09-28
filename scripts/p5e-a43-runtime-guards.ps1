[CmdletBinding()]
param(
    [Parameter(ParameterSetName = 'Library')]
    [switch]$LibraryOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# This file is a library only.  It contains the pre-event authority and
# reservation contract used by the review-only A4.3 command.  It never creates
# an owner receipt and it never starts a process.
$script:P5EA43OwnerReceiptSchema = 'p5e.a43.owner-decision-receipt.v1'
$script:P5EA43OwnerBindingSchema = 'p5e.a43.owner-decision-binding.v1'
$script:P5EA43DecisionEnvironmentNames = @(
    'P5E_OWNER_DECISION_RECEIPT_PATH',
    'P5E_OWNER_DECISION_RECEIPT_SHA256',
    'P5E_A43_COMMAND_SHA256'
)

function Get-P5EA43CanonicalPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    return [IO.Path]::GetFullPath((Resolve-Path -LiteralPath ([IO.Path]::GetFullPath($Path)) -ErrorAction Stop).Path)
}

function Assert-P5EA43NoReparsePath {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][ValidateSet('Leaf', 'Container')][string]$Kind,
        [Parameter(Mandatory = $true)][string]$Label
    )
    $literal = [IO.Path]::GetFullPath($Path)
    $exists = if ($Kind -eq 'Leaf') { Test-Path -LiteralPath $literal -PathType Leaf } else { Test-Path -LiteralPath $literal -PathType Container }
    if (-not $exists) { throw ($Label + '_MISSING_STOP') }
    $item = Get-Item -LiteralPath $literal -Force
    if ($Kind -eq 'Leaf' -and $item.PSIsContainer) { throw ($Label + '_NOT_REGULAR_STOP') }
    if ($Kind -eq 'Container' -and -not $item.PSIsContainer) { throw ($Label + '_NOT_CONTAINER_STOP') }
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw ($Label + '_REPARSE_STOP') }
    $resolved = Get-P5EA43CanonicalPath -Path $literal
    if (-not [StringComparer]::OrdinalIgnoreCase.Equals($literal, $resolved)) { throw ($Label + '_PATH_SWAP_STOP') }
    $cursor = [IO.Directory]::GetParent($literal)
    while ($null -ne $cursor) {
        if (-not (Test-Path -LiteralPath $cursor.FullName -PathType Container)) { throw ($Label + '_ANCESTOR_MISSING_STOP') }
        $cursorItem = Get-Item -LiteralPath $cursor.FullName -Force
        if (($cursorItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { throw ($Label + '_ANCESTOR_REPARSE_STOP') }
        $parent = $cursor.Parent
        if ($null -eq $parent -or $parent.FullName -ceq $cursor.FullName) { break }
        $cursor = $parent
    }
    return $resolved
}

function Assert-P5EA43Sha256 {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value, [Parameter(Mandatory = $true)][string]$Label)
    if ($Value -notmatch '^[0-9a-fA-F]{64}$') { throw ($Label + '_INVALID_STOP') }
    return $Value.ToLowerInvariant()
}

function Assert-P5EA43ExpectedProcessValue {
    # Presence and shape are the only observable result.  The value is never
    # returned, logged, hashed, persisted, or placed in a child argv.
    $value = [Environment]::GetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'Process')
    if ([string]::IsNullOrWhiteSpace($value) -or $value -cnotmatch '^[0-9a-f]{64}$') {
        throw 'OWNER_ENDPOINT_ACCOUNT_FINGERPRINT_MISSING_OR_INVALID_STOP'
    }
    return $true
}

function Test-P5EA43ReceiptPropertySet {
    param([Parameter(Mandatory = $true)]$Receipt)
    $required = @(
        'schemaVersion', 'decisionId', 'packetIdentifier', 'serial',
        'manifestSha256', 'commandSha256', 'helperSha256', 'exporterSha256', 'bridgeSha256',
        'oneEventLimit', 'oneProviderCallLimit', 'scope',
        'noRetry', 'noFallback', 'noRedispatch', 'issuedAt', 'expiresAt')
    $names = @($Receipt.PSObject.Properties.Name)
    foreach ($name in $required) {
        if ($names -notcontains $name) { throw ('P5E_OWNER_RECEIPT_' + $name.ToUpperInvariant() + '_MISSING_STOP') }
    }
    foreach ($name in $names) {
        if ($required -notcontains $name) { throw 'P5E_OWNER_RECEIPT_UNEXPECTED_FIELD_STOP' }
    }
}

function Read-P5EA43OwnerDecisionReceipt {
    param(
        [Parameter(Mandatory = $true)][string]$ReceiptPath,
        [Parameter(Mandatory = $true)][string]$ExpectedReceiptSha256,
        [Parameter(Mandatory = $true)][hashtable]$ExpectedPins,
        [Parameter(Mandatory = $true)][string]$ExpectedSerial,
        [Parameter(Mandatory = $true)][string]$ExpectedScope,
        [long]$NowUnixMilliseconds = 0L
    )
    $expectedHash = Assert-P5EA43Sha256 -Value $ExpectedReceiptSha256 -Label 'P5E_OWNER_RECEIPT_HASH'
    $canonical = Assert-P5EA43NoReparsePath -Path $ReceiptPath -Kind Leaf -Label 'P5E_OWNER_RECEIPT'
    $actualHash = (Get-FileHash -LiteralPath $canonical -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -cne $expectedHash) { throw 'P5E_OWNER_RECEIPT_HASH_MISMATCH_STOP' }
    try { $receipt = Get-Content -Raw -LiteralPath $canonical | ConvertFrom-Json } catch { throw 'P5E_OWNER_RECEIPT_MALFORMED_JSON_STOP' }
    if ($null -eq $receipt -or $receipt -is [array]) { throw 'P5E_OWNER_RECEIPT_MALFORMED_JSON_STOP' }
    Test-P5EA43ReceiptPropertySet -Receipt $receipt
    if ([string]$receipt.schemaVersion -cne $script:P5EA43OwnerReceiptSchema) { throw 'P5E_OWNER_RECEIPT_SCHEMA_STOP' }
    if ([string]$receipt.decisionId -notmatch '^[A-Za-z0-9._-]{1,96}$') { throw 'P5E_OWNER_RECEIPT_DECISION_ID_STOP' }
    if ([string]::IsNullOrWhiteSpace([string]$receipt.packetIdentifier)) { throw 'P5E_OWNER_RECEIPT_PACKET_ID_STOP' }
    if ([string]$receipt.serial -cne $ExpectedSerial) { throw 'P5E_OWNER_RECEIPT_SERIAL_MISMATCH_STOP' }
    if ([string]$receipt.scope -cne $ExpectedScope) { throw 'P5E_OWNER_RECEIPT_SCOPE_MISMATCH_STOP' }
    foreach ($name in @('manifestSha256', 'commandSha256', 'helperSha256', 'exporterSha256', 'bridgeSha256')) {
        $actual = Assert-P5EA43Sha256 -Value ([string]$receipt.$name) -Label ('P5E_OWNER_RECEIPT_' + $name.ToUpperInvariant())
        if (-not $ExpectedPins.ContainsKey($name) -or $actual -cne ([string]$ExpectedPins[$name]).ToLowerInvariant()) {
            throw ('P5E_OWNER_RECEIPT_' + $name.ToUpperInvariant() + '_MISMATCH_STOP')
        }
    }
    if ([long]$receipt.oneEventLimit -ne 1L) { throw 'P5E_OWNER_RECEIPT_EVENT_LIMIT_STOP' }
    if ([long]$receipt.oneProviderCallLimit -ne 1L) { throw 'P5E_OWNER_RECEIPT_PROVIDER_LIMIT_STOP' }
    foreach ($name in @('noRetry', 'noFallback', 'noRedispatch')) {
        if ($receipt.$name -isnot [bool] -or -not [bool]$receipt.$name) { throw ('P5E_OWNER_RECEIPT_' + $name.ToUpperInvariant() + '_STOP') }
    }
    if ($NowUnixMilliseconds -le 0L) { $NowUnixMilliseconds = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() }
    $issued = 0L; $expires = 0L
    if (-not [long]::TryParse([string]$receipt.issuedAt, [Globalization.NumberStyles]::Integer, [Globalization.CultureInfo]::InvariantCulture, [ref]$issued) -or
        -not [long]::TryParse([string]$receipt.expiresAt, [Globalization.NumberStyles]::Integer, [Globalization.CultureInfo]::InvariantCulture, [ref]$expires)) {
        throw 'P5E_OWNER_RECEIPT_TIME_SHAPE_STOP'
    }
    if ($issued -gt $NowUnixMilliseconds) { throw 'P5E_OWNER_RECEIPT_FUTURE_ISSUED_STOP' }
    if ($expires -le $NowUnixMilliseconds -or $expires -le $issued) { throw 'P5E_OWNER_RECEIPT_EXPIRED_STOP' }
    return [pscustomobject]@{
        Path = $canonical
        ReceiptSha256 = $actualHash
        DecisionId = [string]$receipt.decisionId
        PacketIdentifier = [string]$receipt.packetIdentifier
        Serial = [string]$receipt.serial
        Scope = [string]$receipt.scope
        IssuedAt = $issued
        ExpiresAt = $expires
        OneEventLimit = 1L
        OneProviderCallLimit = 1L
        NoRetry = $true
        NoFallback = $true
        NoRedispatch = $true
    }
}

function Test-P5EA43ConsumedDecision {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceRoot,
        [Parameter(Mandatory = $true)][string]$DecisionId,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256,
        [Parameter(Mandatory = $true)][string]$ReservationName
    )
    $root = Assert-P5EA43NoReparsePath -Path $EvidenceRoot -Kind Container -Label 'P5E_EVIDENCE_ROOT'
    try { $children = @(Get-ChildItem -LiteralPath $root -Directory -Force -ErrorAction Stop) } catch { throw 'P5E_OWNER_DECISION_SCAN_FAILED_STOP' }
    $reservationMarker = Join-Path $root ($ReservationName + '.reservation')
    if (Test-Path -LiteralPath $reservationMarker) { throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP' }
    foreach ($child in $children) {
        try { $childPath = Assert-P5EA43NoReparsePath -Path $child.FullName -Kind Container -Label 'P5E_EVENT_DIRECTORY' } catch { throw 'P5E_OWNER_DECISION_SCAN_FAILED_STOP' }
        if ($child.Name -ceq $ReservationName) { throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP' }
        $planPath = Join-Path $childPath 'EVENT_PLAN.json'
        if (-not (Test-Path -LiteralPath $planPath)) { continue }
        try { [void](Assert-P5EA43NoReparsePath -Path $planPath -Kind Leaf -Label 'P5E_EVENT_PLAN_SCAN'); $plan = Get-Content -Raw -LiteralPath $planPath | ConvertFrom-Json } catch { throw 'P5E_OWNER_DECISION_SCAN_FAILED_STOP' }
        $hasDecision = $null -ne $plan.PSObject.Properties['ownerDecisionId'] -or $null -ne $plan.PSObject.Properties['ownerDecisionReceiptSha256']
        if ($hasDecision) {
            if ($null -eq $plan.PSObject.Properties['ownerDecisionId'] -or $null -eq $plan.PSObject.Properties['ownerDecisionReceiptSha256']) { throw 'P5E_OWNER_DECISION_SCAN_FAILED_STOP' }
            if ([string]$plan.ownerDecisionId -ceq $DecisionId -or [string]$plan.ownerDecisionReceiptSha256 -ieq $ReceiptSha256) {
                throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP'
            }
        }
    }
    return $false
}

function Reserve-P5EA43OwnerDecisionEvent {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceRoot,
        [Parameter(Mandatory = $true)][string]$DecisionId,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256
    )
    $root = Assert-P5EA43NoReparsePath -Path $EvidenceRoot -Kind Container -Label 'P5E_EVIDENCE_ROOT'
    $receiptHash = Assert-P5EA43Sha256 -Value $ReceiptSha256 -Label 'P5E_OWNER_RECEIPT_HASH'
    if ($DecisionId -notmatch '^[A-Za-z0-9._-]{1,96}$') { throw 'P5E_OWNER_RECEIPT_DECISION_ID_STOP' }
    $name = 'raw-live-a43-preauth-' + $DecisionId + '-' + $receiptHash.Substring(0, 16)
    Test-P5EA43ConsumedDecision -EvidenceRoot $root -DecisionId $DecisionId -ReceiptSha256 $receiptHash -ReservationName $name | Out-Null
    $path = Join-Path $root $name
    $reservationMarker = Join-Path $root ($name + '.reservation')
    $marker = $null
    $markerCreated = $false
    try {
        # The marker is the atomic decision reservation. FileMode.CreateNew
        # closes the scan/create race. It becomes consumed only together with
        # successful event-directory creation; a failed pre-directory attempt
        # removes only its own marker and remains pre-event/unconsumed.
        $marker = [IO.File]::Open($reservationMarker, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $bytes = [Text.UTF8Encoding]::new($false).GetBytes('p5e.a43.reservation.v1')
        $marker.Write($bytes, 0, $bytes.Length)
        $marker.Flush($true)
        $marker.Dispose(); $marker = $null
        $markerCreated = $true
        [void](New-Item -ItemType Directory -Path $path -ErrorAction Stop)
    } catch {
        if ($null -ne $marker) { $marker.Dispose() }
        if ($markerCreated -and -not (Test-Path -LiteralPath $path -PathType Container) -and (Test-Path -LiteralPath $reservationMarker -PathType Leaf)) {
            try { Remove-Item -LiteralPath $reservationMarker -Force -ErrorAction Stop } catch { throw 'P5E_OWNER_DECISION_RESERVATION_STATE_UNKNOWN_STOP' }
            throw 'P5E_OWNER_DECISION_RESERVATION_FAILED_STOP'
        }
        throw 'P5E_OWNER_DECISION_ALREADY_RESERVED_STOP'
    }
    [void](Assert-P5EA43NoReparsePath -Path $path -Kind Container -Label 'P5E_RESERVED_EVENT')
    return [pscustomobject]@{ Name = $name; Path = (Get-P5EA43CanonicalPath -Path $path); DecisionId = $DecisionId; ReceiptSha256 = $receiptHash; Reserved = $true }
}

function Assert-P5EA43OwnerPlanBinding {
    param(
        [Parameter(Mandatory = $true)]$Plan,
        [Parameter(Mandatory = $true)][string]$DecisionId,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256,
        [Parameter(Mandatory = $true)][string]$PacketIdentifier,
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$Scope
    )
    foreach ($name in @('ownerDecisionBindingVersion', 'ownerDecisionId', 'ownerDecisionReceiptSha256', 'ownerDecisionPacketIdentifier', 'ownerDecisionSerial', 'ownerDecisionScope')) {
        if ($null -eq $Plan.PSObject.Properties[$name]) { throw 'P5E_OWNER_DECISION_PLAN_BINDING_MISSING_STOP' }
    }
    if ([string]$Plan.ownerDecisionBindingVersion -cne $script:P5EA43OwnerBindingSchema -or
        [string]$Plan.ownerDecisionId -cne $DecisionId -or
        [string]$Plan.ownerDecisionReceiptSha256 -cne $ReceiptSha256.ToLowerInvariant() -or
        [string]$Plan.ownerDecisionPacketIdentifier -cne $PacketIdentifier -or
        [string]$Plan.ownerDecisionSerial -cne $Serial -or
        [string]$Plan.ownerDecisionScope -cne $Scope) {
        throw 'P5E_OWNER_DECISION_PLAN_BINDING_MISMATCH_STOP'
    }
    if ([long]$Plan.ownerDecisionOneEventLimit -ne 1L -or [long]$Plan.ownerDecisionOneProviderCallLimit -ne 1L -or
        $Plan.ownerDecisionNoRetry -isnot [bool] -or -not [bool]$Plan.ownerDecisionNoRetry -or
        $Plan.ownerDecisionNoFallback -isnot [bool] -or -not [bool]$Plan.ownerDecisionNoFallback -or
        $Plan.ownerDecisionNoRedispatch -isnot [bool] -or -not [bool]$Plan.ownerDecisionNoRedispatch) {
        throw 'P5E_OWNER_DECISION_PLAN_LIMIT_BINDING_STOP'
    }
    return $true
}

if (-not $LibraryOnly) { return }
