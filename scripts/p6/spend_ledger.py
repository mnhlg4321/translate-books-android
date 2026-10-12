#!/usr/bin/env python3
"""Verify the durable P6 group spend hash chain, close an overrun reservation append-only, and guard the next fixture."""
import decimal
import hashlib
import json
import os

GENESIS = "0" * 64
# A call charged above its reservation keeps the reservation pending (UNKNOWN) on the device. The only way to close it is an
# appended SETTLE_OVERRUN record that carries the provider-reported cost, never an edit of an existing line.
OVERRUN_KIND = "SETTLE_OVERRUN"
OVERRUN_REASON = "COST_ABOVE_RESERVATION"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)


def _money(value, code):
    try:
        return decimal.Decimal(str(value))
    except (decimal.InvalidOperation, ValueError, TypeError):
        raise ValueError(code)


def _verify(lines, group_id, maximum_usd):
    """Walks the whole chain from its lines. Returns (summary, calls, cap_text, prior_hash, entry_count); raises ValueError on any broken rule."""
    maximum = decimal.Decimal(str(maximum_usd))
    if maximum <= 0:
        raise ValueError("P6_SPEND_GROUP_CAP_INVALID")
    prior = GENESIS
    calls = {}
    settled = decimal.Decimal(0)
    cap_text = None
    entries = 0
    overrun_closures = 0
    for line in lines:
        if not line.strip():
            raise ValueError("P6_SPEND_LEDGER_EMPTY_ENTRY")
        row = json.loads(line, parse_float=decimal.Decimal)
        digest = row.pop("entryHash", None)
        if digest != sha(canonical(row).encode("utf-8")) or row.get("previousHash") != prior:
            raise ValueError("P6_SPEND_LEDGER_HASH_CHAIN_INVALID")
        entries += 1
        if row.get("sequence") != entries:
            raise ValueError("P6_SPEND_LEDGER_SEQUENCE_INVALID")
        if row.get("groupId") != group_id:
            raise ValueError("P6_SPEND_LEDGER_GROUP_MISMATCH")
        event_cap = row.get("groupMaximumUsd")
        if cap_text is None:
            cap_text = event_cap
        elif event_cap != cap_text:
            raise ValueError("P6_SPEND_LEDGER_CAP_CHANGED")
        if decimal.Decimal(str(event_cap)) != maximum:
            raise ValueError("P6_SPEND_LEDGER_CAP_CHANGED")
        call_id = row.get("callId")
        phase = row.get("phase")
        amount = _money(row.get("amountUsd"), "P6_SPEND_LEDGER_AMOUNT_INVALID")
        kind = row.get("kind")
        if kind == "RESERVE":
            if call_id in calls:
                raise ValueError("P6_SPEND_LEDGER_DUPLICATE_RESERVATION")
            calls[call_id] = {"phase": phase, "reserved": amount, "settled": None}
        elif kind == "SETTLE":
            call = calls.get(call_id)
            if call is None or call["settled"] is not None or call["phase"] != phase:
                raise ValueError("P6_SPEND_LEDGER_SETTLEMENT_INVALID")
            if amount > call["reserved"]:
                raise ValueError("P6_SPEND_LEDGER_CALL_OVERRUN")
            call["settled"] = amount
            settled += amount
        elif kind == OVERRUN_KIND:
            call = calls.get(call_id)
            if call is None or call["settled"] is not None or call["phase"] != phase:
                raise ValueError("P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID")
            provider = _money(row.get("providerCostUsd"), "P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID")
            if row.get("reason") != OVERRUN_REASON or amount != provider:
                raise ValueError("P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID")
            if _money(row.get("reservedUsd"), "P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID") != call["reserved"]:
                raise ValueError("P6_SPEND_LEDGER_OVERRUN_CLOSE_INVALID")
            if amount <= call["reserved"]:
                raise ValueError("P6_SPEND_LEDGER_OVERRUN_CLOSE_NOT_OVERRUN")
            call["settled"] = amount
            settled += amount
            overrun_closures += 1
        else:
            raise ValueError("P6_SPEND_LEDGER_EVENT_INVALID")
        prior = digest

    pending = [call for call in calls.values() if call["settled"] is None]
    exposure = sum((call["reserved"] if call["settled"] is None else call["settled"]
                    for call in calls.values()), decimal.Decimal(0))
    if exposure > maximum:
        raise ValueError("P6_SPEND_LEDGER_GROUP_CAP_OVERRUN")
    summary = {"groupId": group_id, "capUsd": str(maximum), "entries": entries, "calls": len(calls),
               "pending": len(pending), "settledUsd": str(settled), "exposureUsd": str(exposure),
               "overrunClosures": overrun_closures, "lastEntryHash": prior}
    return summary, calls, cap_text, prior, entries


def _read_lines(path):
    with open(path, encoding="utf-8") as handle:
        return handle.readlines()


def inspect(path, group_id, maximum_usd, allow_empty=False):
    if not os.path.isfile(path):
        if not allow_empty:
            raise ValueError("P6_SPEND_LEDGER_MISSING")
        maximum = decimal.Decimal(str(maximum_usd))
        if maximum <= 0:
            raise ValueError("P6_SPEND_GROUP_CAP_INVALID")
        return {"groupId": group_id, "capUsd": str(maximum), "entries": 0, "calls": 0,
                "pending": 0, "settledUsd": "0", "exposureUsd": "0", "overrunClosures": 0,
                "lastEntryHash": GENESIS}
    summary, _calls, _cap, _prior, _entries = _verify(_read_lines(path), group_id, maximum_usd)
    return summary


def close_overrun(path, group_id, maximum_usd, call_id, provider_cost_usd, evidence=""):
    """Appends one SETTLE_OVERRUN record that closes a pending reservation at the provider-reported cost.

    The whole chain, with the new record, is verified in memory before anything is written; a refused closing leaves the file
    byte-for-byte as it was. No existing line is ever edited.
    """
    lines = _read_lines(path)
    if lines and not lines[-1].endswith("\n"):
        raise ValueError("P6_SPEND_LEDGER_NOT_LINE_TERMINATED")
    _summary, calls, cap_text, prior, entries = _verify(lines, group_id, maximum_usd)
    call = calls.get(call_id)
    if call is None:
        raise ValueError("P6_SPEND_OVERRUN_CALL_UNKNOWN")
    if call["settled"] is not None:
        raise ValueError("P6_SPEND_OVERRUN_ALREADY_SETTLED")
    provider = _money(provider_cost_usd, "P6_SPEND_OVERRUN_COST_INVALID")
    if provider <= call["reserved"]:
        raise ValueError("P6_SPEND_OVERRUN_NOT_ABOVE_RESERVATION")
    row = {"amountUsd": str(provider), "callId": call_id, "evidence": str(evidence), "groupId": group_id,
           "groupMaximumUsd": cap_text, "kind": OVERRUN_KIND, "phase": call["phase"], "previousHash": prior,
           "providerCostUsd": str(provider), "reason": OVERRUN_REASON, "reservedUsd": str(call["reserved"]),
           "sequence": entries + 1}
    row["entryHash"] = sha(canonical({k: v for k, v in row.items() if k != "entryHash"}).encode("utf-8"))
    new_line = canonical(row) + "\n"
    _verify(lines + [new_line], group_id, maximum_usd)  # refuses here, before any byte is written
    with open(path, "a", encoding="utf-8", newline="\n") as handle:
        handle.write(new_line)
    return inspect(path, group_id, maximum_usd)


def preflight(snapshot, required_usd):
    required = decimal.Decimal(str(required_usd))
    if required < 0:
        raise ValueError("P6_FIXTURE_WORST_CASE_INVALID")
    if snapshot["pending"]:
        raise ValueError("P6_SPEND_UNKNOWN_PENDING")
    exposure = decimal.Decimal(snapshot["exposureUsd"])
    cap = decimal.Decimal(snapshot["capUsd"])
    if exposure + required > cap:
        raise ValueError("P6_SPEND_GROUP_CAP_WOULD_BE_EXCEEDED")
    return {"remainingUsd": str(cap - exposure), "requiredUsd": str(required)}
