#!/usr/bin/env python3
"""Group spend checks stop before a fixture dispatch when cap or UNKNOWN state blocks it."""
import hashlib
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spend_ledger  # noqa: E402


def append(path, previous, sequence, kind, call_id, amount, phase="L1_RAW_DISCOVERY"):
    row = {"amountUsd": amount, "callId": call_id, "groupId": "G1-test",
           "groupMaximumUsd": "1", "kind": kind, "phase": phase,
           "previousHash": previous, "sequence": sequence}
    digest = hashlib.sha256(spend_ledger.canonical(row).encode("utf-8")).hexdigest()
    row["entryHash"] = digest
    with open(path, "a", encoding="utf-8", newline="\n") as handle:
        handle.write(spend_ledger.canonical(row) + "\n")
    return digest


class GroupSpendGuardTest(unittest.TestCase):
    def test_fixture_that_would_exceed_group_cap_is_refused_before_dispatch(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = os.path.join(temporary, "group.jsonl")
            prior = append(path, spend_ledger.GENESIS, 1, "RESERVE", "c1", "0.94")
            append(path, prior, 2, "SETTLE", "c1", "0.94")
            snapshot = spend_ledger.inspect(path, "G1-test", "1")
            with self.assertRaisesRegex(ValueError, "P6_SPEND_GROUP_CAP_WOULD_BE_EXCEEDED"):
                spend_ledger.preflight(snapshot, "0.08")

    def test_unknown_cost_stops_the_group_before_the_next_fixture(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = os.path.join(temporary, "group.jsonl")
            append(path, spend_ledger.GENESIS, 1, "RESERVE", "c1", "0.08")
            snapshot = spend_ledger.inspect(path, "G1-test", "1")
            self.assertEqual(1, snapshot["pending"])
            with self.assertRaisesRegex(ValueError, "P6_SPEND_UNKNOWN_PENDING"):
                spend_ledger.preflight(snapshot, "0.08")

    def test_cumulative_spend_survives_a_host_restart_snapshot(self):
        with tempfile.TemporaryDirectory() as temporary:
            device_copy = os.path.join(temporary, "device-ledger.jsonl")
            host_snapshot = os.path.join(temporary, "after-fixture.jsonl")
            prior = append(device_copy, spend_ledger.GENESIS, 1, "RESERVE", "c1", "0.08")
            append(device_copy, prior, 2, "SETTLE", "c1", "0.07")
            with open(device_copy, "rb") as source, open(host_snapshot, "wb") as target:
                target.write(source.read())
            snapshot = spend_ledger.inspect(host_snapshot, "G1-test", "1")
            self.assertEqual("0.07", snapshot["exposureUsd"])
            self.assertEqual("0.93", spend_ledger.preflight(snapshot, "0.08")["remainingUsd"])


if __name__ == "__main__":
    unittest.main()
