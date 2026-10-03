#!/usr/bin/env python3
"""Verify the durable P6 group spend hash chain and guard the next fixture."""
import decimal
import hashlib
import json
import os

GENESIS = "0" * 64


def sha(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False)


def inspect(path, group_id, maximum_usd, allow_empty=False):
    maximum = decimal.Decimal(str(maximum_usd))
    if maximum <= 0:
        raise ValueError("P6_SPEND_GROUP_CAP_INVALID")
    if not os.path.isfile(path):
        if not allow_empty:
            raise ValueError("P6_SPEND_LEDGER_MISSING")
        return {"groupId": group_id, "capUsd": str(maximum), "entries": 0, "calls": 0,
                "pending": 0, "settledUsd": "0", "exposureUsd": "0", "lastEntryHash": GENESIS}

    prior = GENESIS
    calls = {}
    settled = decimal.Decimal(0)
    cap_text = None
    entries = 0
    with open(path, encoding="utf-8") as handle:
        for line in handle:
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
            amount = decimal.Decimal(str(row.get("amountUsd")))
            if row.get("kind") == "RESERVE":
                if call_id in calls:
                    raise ValueError("P6_SPEND_LEDGER_DUPLICATE_RESERVATION")
                calls[call_id] = {"phase": phase, "reserved": amount, "settled": None}
            elif row.get("kind") == "SETTLE":
                call = calls.get(call_id)
                if call is None or call["settled"] is not None or call["phase"] != phase:
                    raise ValueError("P6_SPEND_LEDGER_SETTLEMENT_INVALID")
                if amount > call["reserved"]:
                    raise ValueError("P6_SPEND_LEDGER_CALL_OVERRUN")
                call["settled"] = amount
                settled += amount
            else:
                raise ValueError("P6_SPEND_LEDGER_EVENT_INVALID")
            prior = digest

    pending = [call for call in calls.values() if call["settled"] is None]
    exposure = sum((call["reserved"] if call["settled"] is None else call["settled"]
                    for call in calls.values()), decimal.Decimal(0))
    if exposure > maximum:
        raise ValueError("P6_SPEND_LEDGER_GROUP_CAP_OVERRUN")
    return {"groupId": group_id, "capUsd": str(maximum), "entries": entries, "calls": len(calls),
            "pending": len(pending), "settledUsd": str(settled), "exposureUsd": str(exposure),
            "lastEntryHash": prior}


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
