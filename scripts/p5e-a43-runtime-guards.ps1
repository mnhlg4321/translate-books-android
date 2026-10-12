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
    'P5E_A43_COMMAND_SHA256',
    'P5E_A43_RESERVATION_RECEIPT_SHA256'
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

function Assert-P5EA43CanonicalDecisionId {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    # The receipt schema is ASCII and case-sensitive.  No trim, case fold or
    # Unicode normalization is permitted; the exact accepted bytes are the
    # canonical representation used for the decision key.
    if ($Value -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$') {
        throw 'P5E_OWNER_RECEIPT_DECISION_ID_NONCANONICAL_STOP'
    }
    $roundTrip = [Text.Encoding]::UTF8.GetString([Text.Encoding]::UTF8.GetBytes($Value))
    if ($roundTrip -cne $Value) { throw 'P5E_OWNER_RECEIPT_DECISION_ID_NONCANONICAL_STOP' }
    return $Value
}

function Get-P5EA43DecisionKey {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$CanonicalDecisionId)
    $canonical = Assert-P5EA43CanonicalDecisionId -Value $CanonicalDecisionId
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($canonical)))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
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
    [void](Assert-P5EA43CanonicalDecisionId -Value ([string]$receipt.decisionId))
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
        DecisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId ([string]$receipt.decisionId)
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

function Ensure-P5EA43RegistryDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceRoot,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Label
    )
    $root = Assert-P5EA43NoReparsePath -Path $EvidenceRoot -Kind Container -Label 'P5E_EVIDENCE_ROOT'
    $path = Join-Path $root $Name
    if (-not (Test-Path -LiteralPath $path)) {
        [void](Assert-P5EA43NoReparsePath -Path $root -Kind Container -Label ($Label + '_PARENT'))
        try { [IO.Directory]::CreateDirectory($path) | Out-Null } catch { throw ($Label + '_CREATE_FAILED_STOP') }
    }
    return (Assert-P5EA43NoReparsePath -Path $path -Kind Container -Label $Label)
}

function Get-P5EA43ReservationMarkerFiles {
    param([Parameter(Mandatory = $true)][string]$RegistryPath,[Parameter(Mandatory = $true)][string]$Label)
    try { $items = @(Get-ChildItem -LiteralPath $RegistryPath -File -Force -ErrorAction Stop) } catch { throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP' }
    if ($items.Count -gt 4096) { throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP' }
    foreach ($item in $items) {
        try { [void](Assert-P5EA43NoReparsePath -Path $item.FullName -Kind Leaf -Label $Label) } catch { throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP' }
        if ($item.Name -notmatch '^[0-9a-f]{64}\.reservation$') { throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP' }
    }
    return $items
}

function Read-P5EA43ReservationMarker {
    param([Parameter(Mandatory = $true)][string]$Path,[Parameter(Mandatory = $true)][ValidateSet('decision','receipt')][string]$Kind)
    try {
        $text = Get-Content -Raw -LiteralPath (Assert-P5EA43NoReparsePath -Path $Path -Kind Leaf -Label 'P5E_RESERVATION_MARKER')
        if ([Text.Encoding]::UTF8.GetByteCount($text) -gt 8192) { throw 'oversize' }
        $record = $text | ConvertFrom-Json
        if ($null -eq $record -or $record -is [array]) { throw 'shape' }
        $required = @('schemaVersion','decisionKey','receiptSha256','packetIdentifier','serial','scope','reservedAtUtc','state')
        $allowed = [System.Collections.Generic.HashSet[string]]::new()
        foreach ($requiredName in $required) { [void]$allowed.Add($requiredName) }
        foreach ($property in @($record.PSObject.Properties)) { if (-not $allowed.Contains($property.Name)) { throw 'unknown-field' } }
        foreach ($name in $required) { if ($null -eq $record.PSObject.Properties[$name]) { throw 'missing-field' } }
        if ([string]$record.schemaVersion -cne ('p5e.a43.' + $Kind + '-reservation.v2')) { throw 'schema' }
        if ([string]$record.decisionKey -notmatch '^[0-9a-f]{64}$') { throw 'decision-key' }
        if ([string]$record.receiptSha256 -notmatch '^[0-9a-f]{64}$') { throw 'receipt-hash' }
        if ([string]$record.packetIdentifier -notmatch '^[A-Za-z0-9._-]{1,160}$') { throw 'packet' }
        if ([string]$record.serial -notmatch '^[A-Za-z0-9._:-]{1,96}$') { throw 'serial' }
        if ([string]$record.scope -cne 'RAW/GLOSSARY') { throw 'scope' }
        $reservedAt = [DateTimeOffset]::MinValue
        if (-not [DateTimeOffset]::TryParse([string]$record.reservedAtUtc,
                [Globalization.CultureInfo]::InvariantCulture,
                [Globalization.DateTimeStyles]::RoundtripKind,
                [ref]$reservedAt)) { throw 'reserved-at' }
        if ([string]$record.state -cne 'RESERVED_CONSUMED_FAIL_CLOSED') { throw 'state' }
        $fileKey = [IO.Path]::GetFileNameWithoutExtension([IO.Path]::GetFileNameWithoutExtension($Path))
        if ([string]$record.decisionKey -cne $fileKey -and $Kind -eq 'decision') { throw 'decision-file-binding' }
        if ([string]$record.receiptSha256 -cne $fileKey -and $Kind -eq 'receipt') { throw 'receipt-file-binding' }
        return [pscustomobject]@{
            DecisionKey = [string]$record.decisionKey
            ReceiptSha256 = [string]$record.receiptSha256
            PacketIdentifier = [string]$record.packetIdentifier
            Serial = [string]$record.serial
            Scope = [string]$record.scope
            State = [string]$record.state
        }
    } catch { throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP' }
}

function Get-P5EA43ReservationLedger {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceRoot,
        [Parameter(Mandatory = $true)][string]$DecisionKey,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256
    )
    $decisionRoot = Ensure-P5EA43RegistryDirectory -EvidenceRoot $EvidenceRoot -Name '.decisions' -Label 'P5E_DECISION_REGISTRY'
    $receiptRoot = Ensure-P5EA43RegistryDirectory -EvidenceRoot $EvidenceRoot -Name '.receipts' -Label 'P5E_RECEIPT_REGISTRY'
    $decisionItems = @(Get-P5EA43ReservationMarkerFiles -RegistryPath $decisionRoot -Label 'P5E_DECISION_MARKER')
    $receiptItems = @(Get-P5EA43ReservationMarkerFiles -RegistryPath $receiptRoot -Label 'P5E_RECEIPT_MARKER')
    $decisions = [System.Collections.Generic.List[object]]::new()
    $receipts = [System.Collections.Generic.List[object]]::new()
    foreach ($item in $decisionItems) { [void]$decisions.Add((Read-P5EA43ReservationMarker -Path $item.FullName -Kind decision)) }
    foreach ($item in $receiptItems) { [void]$receipts.Add((Read-P5EA43ReservationMarker -Path $item.FullName -Kind receipt)) }
    foreach ($receipt in $receipts) {
        if (@($decisions | Where-Object { $_.DecisionKey -ceq $receipt.DecisionKey }).Count -eq 0) {
            throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP'
        }
    }
    if (@($decisions | Where-Object { $_.DecisionKey -ceq $DecisionKey }).Count -gt 0) {
        throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP'
    }
    # A complete receipt binding is allowed to be observed here so a new
    # decision marker can be durably consumed before the receipt CreateNew
    # fails.  A decision-only marker is the crash window between the two
    # markers and must block reuse before creating another decision marker.
    $decisionForReceipt = @($decisions | Where-Object { $_.ReceiptSha256 -ceq $ReceiptSha256 })
    $receiptForHash = @($receipts | Where-Object { $_.ReceiptSha256 -ceq $ReceiptSha256 })
    if ($decisionForReceipt.Count -gt 0 -and $receiptForHash.Count -eq 0) {
        throw 'P5E_OWNER_RECEIPT_ALREADY_CONSUMED_STOP'
    }
    return [pscustomobject]@{ DecisionRoot = $decisionRoot; ReceiptRoot = $receiptRoot; Decisions = $decisions.ToArray(); Receipts = $receipts.ToArray() }
}

function Write-P5EA43ReservationMarker {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][ValidateSet('decision','receipt')][string]$Kind,
        [Parameter(Mandatory = $true)][string]$DecisionKey,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256,
        [Parameter(Mandatory = $true)][string]$PacketIdentifier,
        [Parameter(Mandatory = $true)][string]$Serial,
        [Parameter(Mandatory = $true)][string]$Scope
    )
    $record = [ordered]@{
        schemaVersion = 'p5e.a43.' + $Kind + '-reservation.v2'
        decisionKey = $DecisionKey
        receiptSha256 = $ReceiptSha256
        packetIdentifier = $PacketIdentifier
        serial = $Serial
        scope = $Scope
        reservedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
        state = 'RESERVED_CONSUMED_FAIL_CLOSED'
    }
    $text = $record | ConvertTo-Json -Compress
    if ([Text.Encoding]::UTF8.GetByteCount($text) -gt 8192) { throw 'P5E_OWNER_DECISION_RESERVATION_RECORD_TOO_LARGE_STOP' }
    $stream = $null
    try {
        $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $bytes = [Text.UTF8Encoding]::new($false).GetBytes($text)
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Flush($true)
        $stream.Dispose(); $stream = $null
    } catch {
        if ($null -ne $stream) { $stream.Dispose() }
        if (Test-Path -LiteralPath $Path -PathType Leaf) {
            try {
                [void](Read-P5EA43ReservationMarker -Path $Path -Kind $Kind)
                if ($Kind -eq 'receipt') { throw 'P5E_OWNER_RECEIPT_ALREADY_CONSUMED_STOP' }
                throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP'
            } catch {
                $message = [string]$_.Exception.Message
                if ($message -in @('P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP', 'P5E_OWNER_RECEIPT_ALREADY_CONSUMED_STOP')) { throw }
                throw 'P5E_OWNER_DECISION_RESERVATION_LEDGER_UNKNOWN_STOP'
            }
        }
        throw 'P5E_OWNER_DECISION_RESERVATION_STATE_UNKNOWN_STOP'
    }
}

function Test-P5EA43ConsumedDecision {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceRoot,
        [Parameter(Mandatory = $true)][string]$DecisionId,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256,
        [Parameter(Mandatory = $true)][string]$ReservationName
    )
    $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId $DecisionId
    $receiptHash = Assert-P5EA43Sha256 -Value $ReceiptSha256 -Label 'P5E_OWNER_RECEIPT_HASH'
    [void](Get-P5EA43ReservationLedger -EvidenceRoot $EvidenceRoot -DecisionKey $decisionKey -ReceiptSha256 $receiptHash)
    $legacyPath = Join-Path (Assert-P5EA43NoReparsePath -Path $EvidenceRoot -Kind Container -Label 'P5E_EVIDENCE_ROOT') $ReservationName
    if (Test-Path -LiteralPath $legacyPath) { throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP' }
    $legacyMarker = $legacyPath + '.reservation'
    if (Test-Path -LiteralPath $legacyMarker) { throw 'P5E_OWNER_DECISION_ALREADY_CONSUMED_STOP' }
    return $false
}

function Reserve-P5EA43OwnerDecisionEvent {
    param(
        [Parameter(Mandatory = $true)][string]$EvidenceRoot,
        [Parameter(Mandatory = $true)][string]$DecisionId,
        [Parameter(Mandatory = $true)][string]$ReceiptSha256,
        [string]$PacketIdentifier = 'P5E-A43-SYNTHETIC',
        [string]$Serial = '15e84958',
        [string]$Scope = 'RAW/GLOSSARY'
    )
    $root = Assert-P5EA43NoReparsePath -Path $EvidenceRoot -Kind Container -Label 'P5E_EVIDENCE_ROOT'
    $canonicalDecisionId = Assert-P5EA43CanonicalDecisionId -Value $DecisionId
    $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId $canonicalDecisionId
    $receiptHash = Assert-P5EA43Sha256 -Value $ReceiptSha256 -Label 'P5E_OWNER_RECEIPT_HASH'
    if ($PacketIdentifier -notmatch '^[A-Za-z0-9._-]{1,160}$') { throw 'P5E_OWNER_DECISION_PACKET_ID_STOP' }
    if ($Serial -notmatch '^[A-Za-z0-9._:-]{1,96}$') { throw 'P5E_OWNER_DECISION_SERIAL_STOP' }
    if ($Scope -cne 'RAW/GLOSSARY') { throw 'P5E_OWNER_DECISION_SCOPE_STOP' }
    $ledger = Get-P5EA43ReservationLedger -EvidenceRoot $root -DecisionKey $decisionKey -ReceiptSha256 $receiptHash
    $decisionMarker = Join-Path $ledger.DecisionRoot ($decisionKey + '.reservation')
    $receiptMarker = Join-Path $ledger.ReceiptRoot ($receiptHash + '.reservation')
    Write-P5EA43ReservationMarker -Path $decisionMarker -Kind decision -DecisionKey $decisionKey -ReceiptSha256 $receiptHash `
        -PacketIdentifier $PacketIdentifier -Serial $Serial -Scope $Scope
    # The decision marker is durable before receipt binding.  Any exception
    # after this point leaves both the decision and any later marker consumed.
    try {
        Write-P5EA43ReservationMarker -Path $receiptMarker -Kind receipt -DecisionKey $decisionKey -ReceiptSha256 $receiptHash `
            -PacketIdentifier $PacketIdentifier -Serial $Serial -Scope $Scope
    } catch { throw }
    $eventName = 'raw-live-a43-preauth-' + $decisionKey
    $eventPath = Join-Path $root $eventName
    if (Test-Path -LiteralPath $eventPath) { throw 'P5E_OWNER_DECISION_EVENT_ALREADY_EXISTS_STOP' }
    [void](Assert-P5EA43NoReparsePath -Path $root -Kind Container -Label 'P5E_EVENT_PARENT')
    try { [IO.Directory]::CreateDirectory($eventPath) | Out-Null } catch { throw 'P5E_OWNER_DECISION_EVENT_CREATE_FAILED_STOP' }
    [void](Assert-P5EA43NoReparsePath -Path $eventPath -Kind Container -Label 'P5E_RESERVED_EVENT')
    return [pscustomobject]@{
        Name = $eventName
        Path = Get-P5EA43CanonicalPath -Path $eventPath
        DecisionId = $canonicalDecisionId
        DecisionKey = $decisionKey
        ReceiptSha256 = $receiptHash
        DecisionMarkerPath = Get-P5EA43CanonicalPath -Path $decisionMarker
        ReceiptMarkerPath = Get-P5EA43CanonicalPath -Path $receiptMarker
        State = 'EVENT_RESERVED'
        Reserved = $true
    }
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
    $required = @(
        'ownerDecisionBindingVersion', 'ownerDecisionId', 'ownerDecisionReceiptSha256',
        'ownerDecisionPacketIdentifier', 'ownerDecisionSerial', 'ownerDecisionScope',
        'ownerDecisionOneEventLimit', 'ownerDecisionOneProviderCallLimit',
        'ownerDecisionNoRetry', 'ownerDecisionNoFallback', 'ownerDecisionNoRedispatch')
    $values = @{}
    foreach ($name in $required) {
        if ($Plan -is [System.Collections.IDictionary]) {
            if (-not $Plan.Contains($name)) { throw 'P5E_OWNER_DECISION_PLAN_BINDING_MISSING_STOP' }
            $values[$name] = $Plan[$name]
        } else {
            $property = $Plan.PSObject.Properties[$name]
            if ($null -eq $property) { throw 'P5E_OWNER_DECISION_PLAN_BINDING_MISSING_STOP' }
            $values[$name] = $property.Value
        }
    }
    if ([string]$values.ownerDecisionBindingVersion -cne $script:P5EA43OwnerBindingSchema -or
        [string]$values.ownerDecisionId -cne $DecisionId -or
        [string]$values.ownerDecisionReceiptSha256 -cne $ReceiptSha256.ToLowerInvariant() -or
        [string]$values.ownerDecisionPacketIdentifier -cne $PacketIdentifier -or
        [string]$values.ownerDecisionSerial -cne $Serial -or
        [string]$values.ownerDecisionScope -cne $Scope) {
        throw 'P5E_OWNER_DECISION_PLAN_BINDING_MISMATCH_STOP'
    }
    if ([long]$values.ownerDecisionOneEventLimit -ne 1L -or [long]$values.ownerDecisionOneProviderCallLimit -ne 1L -or
        $values.ownerDecisionNoRetry -isnot [bool] -or -not [bool]$values.ownerDecisionNoRetry -or
        $values.ownerDecisionNoFallback -isnot [bool] -or -not [bool]$values.ownerDecisionNoFallback -or
        $values.ownerDecisionNoRedispatch -isnot [bool] -or -not [bool]$values.ownerDecisionNoRedispatch) {
        throw 'P5E_OWNER_DECISION_PLAN_LIMIT_BINDING_STOP'
    }
    return $true
}

if (-not $LibraryOnly) { return }
